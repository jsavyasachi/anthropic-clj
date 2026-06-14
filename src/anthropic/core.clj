(ns anthropic.core
  "Idiomatic Clojure wrapper over the official Anthropic Java SDK
  (`com.anthropic/anthropic-java`).

  Build a request as a Clojure map, get a Clojure map back. The client reads
  `ANTHROPIC_API_KEY` from the environment by default."
  (:require [clojure.string :as str]
            [clojure.walk :as walk])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.client.okhttp AnthropicOkHttpClient)
           (com.anthropic.core JsonValue)
           (com.anthropic.core.http StreamResponse)
           (com.anthropic.models.messages ContentBlock ContentBlockParam Message
                                          MessageCreateParams
                                          MessageCreateParams$Builder Model
                                          RawContentBlockDelta
                                          RawContentBlockDeltaEvent
                                          RawMessageStreamEvent
                                          TextBlock TextBlockParam TextDelta
                                          ThinkingBlock Tool Tool$InputSchema
                                          ToolResultBlockParam ToolUseBlock
                                          ToolUseBlockParam Usage)))

(defn client
  "An Anthropic client. With no args, resolves credentials from the environment
  (`ANTHROPIC_API_KEY`). Pass `{:api-key \"...\"}` to set the key explicitly."
  (^AnthropicClient [] (AnthropicOkHttpClient/fromEnv))
  (^AnthropicClient [{:keys [api-key]}]
   (-> (AnthropicOkHttpClient/builder)
       (.apiKey ^String api-key)
       (.build))))

(defn- ->json ^JsonValue [m]
  (JsonValue/from (walk/stringify-keys m)))

(defn- ->tool ^Tool [{:keys [name description input-schema]}]
  (let [required (:required input-schema)
        isb (Tool$InputSchema/builder)
        tb (Tool/builder)]
    (.properties isb (->json (:properties input-schema)))
    (when (seq required) (.required isb ^java.util.List (vec required)))
    (.name tb ^String name)
    (.inputSchema tb (.build isb))
    (when description (.description tb ^String description))
    (.build tb)))

(defn- ->content-block ^ContentBlockParam [{:keys [type] :as blk}]
  (case (keyword type)
    :text (ContentBlockParam/ofText
           (-> (TextBlockParam/builder) (.text ^String (:text blk)) (.build)))
    :tool-result (ContentBlockParam/ofToolResult
                  (-> (ToolResultBlockParam/builder)
                      (.toolUseId ^String (:tool-use-id blk))
                      (.content ^String (str (:content blk)))
                      (.build)))
    :tool-use (ContentBlockParam/ofToolUse
               (-> (ToolUseBlockParam/builder)
                   (.id ^String (:id blk))
                   (.name ^String (:name blk))
                   (.input (->json (:input blk)))
                   (.build)))))

(defn- add-message [^MessageCreateParams$Builder b {:keys [role content]}]
  (let [r (keyword role)]
    (if (string? content)
      (case r
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content))
      (let [blocks (mapv ->content-block content)]
        (case r
          :user (.addUserMessageOfBlockParams b blocks)
          :assistant (.addAssistantMessageOfBlockParams b blocks))))))

(defn- ->params
  "Translate a request map into the SDK's MessageCreateParams."
  ^MessageCreateParams [{:keys [model max-tokens system messages tools]
                         :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (MessageCreateParams/builder)
            (.model (Model/of model))
            (.maxTokens (long max-tokens)))]
    (when system (.system b ^String system))
    (doseq [t tools] (.addTool b (->tool t)))
    (doseq [m messages] (add-message b m))
    (.build b)))

(defn- java->clj [x]
  (cond
    (instance? java.util.Map x) (persistent!
                                 (reduce-kv (fn [acc k v] (assoc! acc (keyword (str k)) (java->clj v)))
                                            (transient {}) (into {} x)))
    (instance? java.util.List x) (mapv java->clj x)
    :else x))

(defn- block->map [^ContentBlock b]
  (let [txt (.text b)
        tu (.toolUse b)
        th (.thinking b)]
    (cond
      (.isPresent txt) {:type :text :text (.text ^TextBlock (.get txt))}
      (.isPresent tu) (let [x ^ToolUseBlock (.get tu)]
                        {:type :tool-use
                         :id (.id x)
                         :name (.name x)
                         :input (java->clj (.convert (._input x) java.lang.Object))})
      (.isPresent th) {:type :thinking :thinking (.thinking ^ThinkingBlock (.get th))}
      :else {:type :other})))

(defn- usage->map [^Usage u]
  {:input-tokens (.inputTokens u)
   :output-tokens (.outputTokens u)})

(defn- ->keyword [x]
  (-> x str str/lower-case (str/replace "_" "-") keyword))

(defn- message->map [^Message m]
  (let [sr (.stopReason m)]
    {:id (.id m)
     :model (str (.model m))
     :role :assistant ; Messages API responses are always the assistant turn
     :stop-reason (when (.isPresent sr) (->keyword (.get sr)))
     :content (mapv block->map (.content m))
     :usage (usage->map (.usage m))}))

(defn create-message
  "Send a Messages request and return the response as a Clojure map.

  `req` keys: `:model` (string, defaults to \"claude-opus-4-8\"), `:max-tokens`
  (defaults to 1024), `:system` (string, optional), and `:messages` (a seq of
  `{:role :user|:assistant :content \"...\"}`). Returns
  `{:id :model :role :stop-reason :content [...] :usage {...}}`."
  [^AnthropicClient client req]
  (-> (.messages client)
      (.create (->params req))
      (message->map)))

(defn- delta-text ^String [^RawMessageStreamEvent ev]
  (let [cbd (.contentBlockDelta ev)]
    (when (.isPresent cbd)
      (let [d (.delta ^RawContentBlockDeltaEvent (.get cbd))
            t (.text ^RawContentBlockDelta d)]
        (when (.isPresent t)
          (.text ^TextDelta (.get t)))))))

(defn stream-text
  "Stream a Messages request, calling `on-text` with each text delta (a string)
  as it arrives, and returning the full concatenated text when the stream ends.
  Takes the same `req` map as `create-message`. The underlying HTTP stream is
  closed automatically."
  ^String [^AnthropicClient client req on-text]
  (with-open [^StreamResponse sr (.createStreaming (.messages client) (->params req))]
    (let [sb (StringBuilder.)]
      (doseq [ev (iterator-seq (.iterator (.stream sr)))]
        (when-let [s (delta-text ev)]
          (.append sb s)
          (when on-text (on-text s))))
      (str sb))))
