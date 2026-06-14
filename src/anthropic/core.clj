(ns anthropic.core
  "Idiomatic Clojure wrapper over the official Anthropic Java SDK
  (`com.anthropic/anthropic-java`).

  Build a request as a Clojure map, get a Clojure map back. The client reads
  `ANTHROPIC_API_KEY` from the environment by default."
  (:require [clojure.string :as str])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.client.okhttp AnthropicOkHttpClient)
           (com.anthropic.models.messages ContentBlock Message
                                          MessageCreateParams Model
                                          TextBlock ThinkingBlock Usage)))

(defn client
  "An Anthropic client. With no args, resolves credentials from the environment
  (`ANTHROPIC_API_KEY`). Pass `{:api-key \"...\"}` to set the key explicitly."
  (^AnthropicClient [] (AnthropicOkHttpClient/fromEnv))
  (^AnthropicClient [{:keys [api-key]}]
   (-> (AnthropicOkHttpClient/builder)
       (.apiKey ^String api-key)
       (.build))))

(defn- ->params
  "Translate a request map into the SDK's MessageCreateParams."
  ^MessageCreateParams [{:keys [model max-tokens system messages]
                         :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (MessageCreateParams/builder)
            (.model (Model/of model))
            (.maxTokens (long max-tokens)))]
    (when system (.system b ^String system))
    (doseq [{:keys [role content]} messages]
      (case (keyword role)
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content)))
    (.build b)))

(defn- block->map [^ContentBlock b]
  (let [txt (.text b)]
    (cond
      (.isPresent txt) {:type :text :text (.text ^TextBlock (.get txt))}
      :else (let [th (.thinking b)]
              (if (.isPresent th)
                {:type :thinking :thinking (.thinking ^ThinkingBlock (.get th))}
                {:type :other})))))

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
