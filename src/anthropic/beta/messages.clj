(ns anthropic.beta.messages
  "Idiomatic Clojure wrapper for the beta Messages API."
  (:require [anthropic.core]
            [clojure.string :as str]
            [clojure.walk :as walk])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue RequestOptions)
           (com.anthropic.core.http Headers HttpResponse HttpResponseFor)
           (com.anthropic.errors AnthropicException)
           (com.anthropic.models.beta.messages BetaBase64ImageSource
                                               BetaBase64ImageSource$MediaType
                                               BetaBase64PdfSource
                                               BetaCacheControlEphemeral
                                               BetaCacheControlEphemeral$Ttl
                                               BetaContentBlockParam
                                               BetaImageBlockParam
                                               BetaImageBlockParam$Source
                                               BetaJsonOutputFormat
                                               BetaJsonOutputFormat$Schema
                                               BetaMessage BetaMessageTokensCount
                                               BetaMetadata BetaOutputConfig
                                               BetaOutputConfig$Effort
                                               BetaPlainTextSource
                                               BetaRedactedThinkingBlockParam
                                               BetaRequestDocumentBlock
                                               BetaRequestDocumentBlock$Source
                                               BetaRequestMcpServerToolConfiguration
                                               BetaRequestMcpServerUrlDefinition
                                               BetaTextBlockParam
                                               BetaThinkingBlockParam
                                               BetaThinkingConfigAdaptive
                                               BetaThinkingConfigDisabled
                                               BetaThinkingConfigEnabled
                                               BetaThinkingConfigParam
                                               BetaTool BetaTool$InputSchema
                                               BetaTool$InputSchema$Properties
                                               BetaToolChoice BetaToolChoiceAny
                                               BetaToolChoiceAuto BetaToolChoiceNone
                                               BetaToolChoiceTool
                                               BetaToolResultBlockParam
                                               BetaToolUseBlockParam
                                               BetaToolUseBlockParam$Input
                                               BetaUrlImageSource BetaUrlPdfSource
                                               MessageCountTokensParams
                                               MessageCountTokensParams$Builder
                                               MessageCreateParams
                                               MessageCreateParams$Builder
                                               MessageCreateParams$ServiceTier)))

(set! *warn-on-reflection* true)

(def ^:private throw-normalized! @#'anthropic.core/throw-normalized!)

(defmacro ^:private with-api-errors [& body]
  `(try ~@body
        (catch AnthropicException e# (throw-normalized! e#))))

(defn- ->json ^JsonValue [x]
  (JsonValue/from (walk/stringify-keys x)))

(defn- ->cache-control ^BetaCacheControlEphemeral [cc]
  (let [b (BetaCacheControlEphemeral/builder)]
    (when-let [ttl (and (map? cc) (:ttl cc))]
      (.ttl b (BetaCacheControlEphemeral$Ttl/of (name ttl))))
    (.build b)))

(defn- ->image-source ^BetaImageBlockParam$Source [{:keys [type media-type data url]}]
  (case (keyword type)
    :base64 (BetaImageBlockParam$Source/ofBase64
             (-> (BetaBase64ImageSource/builder)
                 (.data ^String data)
                 (.mediaType (BetaBase64ImageSource$MediaType/of ^String media-type))
                 (.build)))
    :url (BetaImageBlockParam$Source/ofUrl
          (-> (BetaUrlImageSource/builder) (.url ^String url) (.build)))
    (throw (ex-info "Unsupported image source type"
                    {:anthropic/error :unsupported-content-source :type type}))))

(defn- ->document-source ^BetaRequestDocumentBlock$Source [{:keys [type data url]}]
  (case (keyword type)
    :base64 (BetaRequestDocumentBlock$Source/ofBase64
             (-> (BetaBase64PdfSource/builder) (.data ^String data) (.build)))
    :url (BetaRequestDocumentBlock$Source/ofUrl
          (-> (BetaUrlPdfSource/builder) (.url ^String url) (.build)))
    :text (BetaRequestDocumentBlock$Source/ofText
           (-> (BetaPlainTextSource/builder) (.data ^String data) (.build)))
    (throw (ex-info "Unsupported document source type"
                    {:anthropic/error :unsupported-content-source :type type}))))

(defn- ->system-block ^BetaTextBlockParam [{:keys [text cache-control]}]
  (let [b (-> (BetaTextBlockParam/builder) (.text ^String text))]
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (.build b)))

(defn- ->tool-input ^BetaToolUseBlockParam$Input [input]
  (let [b (BetaToolUseBlockParam$Input/builder)]
    (doseq [[k v] input]
      (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->content-block ^BetaContentBlockParam [{:keys [type cache-control] :as blk}]
  (case (keyword type)
    :text (let [b (-> (BetaTextBlockParam/builder) (.text ^String (:text blk)))]
            (when cache-control (.cacheControl b (->cache-control cache-control)))
            (BetaContentBlockParam/ofText (.build b)))
    :image (let [b (-> (BetaImageBlockParam/builder)
                        (.source ^BetaImageBlockParam$Source (->image-source (:source blk))))]
             (when cache-control (.cacheControl b (->cache-control cache-control)))
             (BetaContentBlockParam/ofImage (.build b)))
    :document (let [b (-> (BetaRequestDocumentBlock/builder)
                           (.source ^BetaRequestDocumentBlock$Source (->document-source (:source blk))))]
                (when cache-control (.cacheControl b (->cache-control cache-control)))
                (when-let [title (:title blk)] (.title b ^String title))
                (when-let [context (:context blk)] (.context b ^String context))
                (BetaContentBlockParam/ofDocument (.build b)))
    :thinking (BetaContentBlockParam/ofThinking
               (-> (BetaThinkingBlockParam/builder)
                   (.thinking ^String (:thinking blk))
                   (.signature ^String (:signature blk))
                   (.build)))
    :redacted-thinking (BetaContentBlockParam/ofRedactedThinking
                        (-> (BetaRedactedThinkingBlockParam/builder)
                            (.data ^String (:data blk))
                            (.build)))
    :tool-use (let [b (-> (BetaToolUseBlockParam/builder)
                           (.id ^String (:id blk))
                           (.name ^String (:name blk))
                           (.input ^BetaToolUseBlockParam$Input (->tool-input (:input blk))))]
                (when cache-control (.cacheControl b (->cache-control cache-control)))
                (BetaContentBlockParam/ofToolUse (.build b)))
    :tool-result (let [b (-> (BetaToolResultBlockParam/builder)
                              (.toolUseId ^String (:tool-use-id blk)))]
                   (if (string? (:content blk))
                     (.content b ^String (:content blk))
                     (.contentAsJson b (walk/stringify-keys (:content blk))))
                   (when (contains? blk :is-error) (.isError b (boolean (:is-error blk))))
                   (when cache-control (.cacheControl b (->cache-control cache-control)))
                   (BetaContentBlockParam/ofToolResult (.build b)))
    (throw (ex-info "Unsupported beta content block type"
                    {:anthropic/error :unsupported-content-block :type type}))))

(defn- ->thinking ^BetaThinkingConfigParam [{:keys [type budget-tokens]}]
  (case (keyword type)
    :enabled (BetaThinkingConfigParam/ofEnabled
              (-> (BetaThinkingConfigEnabled/builder)
                  (.budgetTokens (long budget-tokens)) (.build)))
    :disabled (BetaThinkingConfigParam/ofDisabled (.build (BetaThinkingConfigDisabled/builder)))
    :adaptive (BetaThinkingConfigParam/ofAdaptive (.build (BetaThinkingConfigAdaptive/builder)))
    (throw (ex-info "Unsupported thinking type"
                    {:anthropic/error :unsupported-thinking-type :type type}))))

(defn- ->tool-choice ^BetaToolChoice [tc]
  (if (map? tc)
    (BetaToolChoice/ofTool (-> (BetaToolChoiceTool/builder) (.name ^String (:name tc)) (.build)))
    (case (keyword tc)
      :auto (BetaToolChoice/ofAuto (.build (BetaToolChoiceAuto/builder)))
      :any (BetaToolChoice/ofAny (.build (BetaToolChoiceAny/builder)))
      :none (BetaToolChoice/ofNone (.build (BetaToolChoiceNone/builder)))
      (throw (ex-info "Unsupported tool choice"
                      {:anthropic/error :unsupported-tool-choice :tool-choice tc})))))

(defn- ->tool ^BetaTool [{:keys [name description input-schema cache-control]}]
  (let [schema (or input-schema {})
        properties (BetaTool$InputSchema$Properties/builder)
        schema-builder (-> (BetaTool$InputSchema/builder)
                           (.type (->json (or (:type schema) "object"))))
        b (-> (BetaTool/builder)
              (.name ^String name))]
    (doseq [[k v] (:properties schema)]
      (.putAdditionalProperty properties ^String (name k) (->json v)))
    (.properties schema-builder (.build properties))
    (when (seq (:required schema)) (.required schema-builder ^java.util.List (vec (:required schema))))
    (.inputSchema b (.build schema-builder))
    (when description (.description b ^String description))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (.build b)))

(defn- ->metadata ^BetaMetadata [{:keys [user-id]}]
  (-> (BetaMetadata/builder) (.userId ^String user-id) (.build)))

(defn- ->output-config ^BetaOutputConfig [schema effort]
  (let [b (BetaOutputConfig/builder)]
    (when schema
      (let [sb (BetaJsonOutputFormat$Schema/builder)]
        (doseq [[k v] schema]
          (.putAdditionalProperty sb ^String (name k) (->json v)))
        (.format b (-> (BetaJsonOutputFormat/builder) (.schema (.build sb)) (.build)))))
    (when effort (.effort b (BetaOutputConfig$Effort/of (name effort))))
    (.build b)))

(defn- ->mcp-server ^BetaRequestMcpServerUrlDefinition
  [{:keys [name url authorization-token tool-configuration]}]
  (let [b (-> (BetaRequestMcpServerUrlDefinition/builder)
              (.name ^String name)
              (.url ^String url))]
    (when authorization-token (.authorizationToken b ^String authorization-token))
    (when tool-configuration
      (let [tb (BetaRequestMcpServerToolConfiguration/builder)]
        (when-let [allowed-tools (:allowed-tools tool-configuration)]
          (.allowedTools tb ^java.util.List (vec allowed-tools)))
        (when (contains? tool-configuration :enabled)
          (.enabled tb (boolean (:enabled tool-configuration))))
        (.toolConfiguration b (.build tb))))
    (.build b)))

(defn- add-create-message [^MessageCreateParams$Builder b {:keys [role content]}]
  (let [role (keyword role)]
    (if (string? content)
      (case role
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content))
      (let [blocks (mapv ->content-block content)]
        (case role
          :user (.addUserMessageOfBetaContentBlockParams b ^java.util.List blocks)
          :assistant (.addAssistantMessageOfBetaContentBlockParams b ^java.util.List blocks))))))

(defn- add-count-message [^MessageCountTokensParams$Builder b {:keys [role content]}]
  (let [role (keyword role)]
    (if (string? content)
      (case role
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content))
      (let [blocks (mapv ->content-block content)]
        (case role
          :user (.addUserMessageOfBetaContentBlockParams b ^java.util.List blocks)
          :assistant (.addAssistantMessageOfBetaContentBlockParams b ^java.util.List blocks))))))

(defn- ->params ^MessageCreateParams
  [{:keys [model max-tokens system messages tools temperature top-p top-k stop-sequences
           tool-choice thinking metadata service-tier response-format effort container inference-geo
           user-profile-id cache-control betas mcp-servers extra-headers extra-query extra-body]
    :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [^String model-name (if (keyword? model) (name model) model)
        b (doto (MessageCreateParams/builder)
            (.model model-name)
            (.maxTokens (long max-tokens)))]
    (when system
      (if (string? system)
        (.system b ^String system)
        (.systemOfBetaTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when temperature (.temperature b (double temperature)))
    (when top-p (.topP b (double top-p)))
    (when top-k (.topK b (long top-k)))
    (when (seq stop-sequences) (.stopSequences b ^java.util.List (vec stop-sequences)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when thinking (.thinking b (->thinking thinking)))
    (when metadata (.metadata b (->metadata metadata)))
    (when service-tier (.serviceTier b (MessageCreateParams$ServiceTier/of (-> service-tier name (str/replace "-" "_")))))
    (when container (.container b ^String container))
    (when inference-geo (.inferenceGeo b ^String inference-geo))
    (when user-profile-id (.userProfileId b ^String user-profile-id))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when (or response-format effort) (.outputConfig b (->output-config response-format effort)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (name beta) beta)]
        (.addBeta b beta-name)))
    (doseq [tool tools] (.addTool b (->tool tool)))
    (doseq [server mcp-servers] (.addMcpServer b (->mcp-server server)))
    (doseq [message messages] (add-create-message b message))
    (doseq [[k v] extra-headers] (.putAdditionalHeader b ^String (name k) ^String v))
    (doseq [[k v] extra-query] (.putAdditionalQueryParam b ^String (name k) ^String v))
    (doseq [[k v] extra-body] (.putAdditionalBodyProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->count-params ^MessageCountTokensParams
  [{:keys [model system messages tools thinking tool-choice betas]
    :or {model "claude-opus-4-8"}}]
  (let [^String model-name (if (keyword? model) (name model) model)
        b (doto (MessageCountTokensParams/builder)
            (.model model-name))]
    (when system
      (if (string? system)
        (.system b ^String system)
        (.systemOfBetaTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when thinking (.thinking b (->thinking thinking)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (name beta) beta)]
        (.addBeta b beta-name)))
    (doseq [tool tools] (.addTool b (->tool tool)))
    (doseq [message messages] (add-count-message b message))
    (.build b)))

(defn- java->clj [x]
  (cond
    (instance? java.util.Map x) (persistent!
                                 (reduce-kv (fn [acc k v]
                                              (let [value (java->clj v)]
                                                (if (nil? value)
                                                  acc
                                                  (assoc! acc (keyword (str/replace (str k) "_" "-")) value))))
                                            (transient {}) (into {} x)))
    (instance? java.util.List x) (mapv java->clj x)
    :else x))

(defn- json->clj [^JsonValue jv]
  (java->clj (.convert jv java.lang.Object)))

(defn- ->keyword [x]
  (-> x str str/lower-case (str/replace #"[._]" "-") keyword))

(defn- beta-message->map [^BetaMessage message]
  (let [m (json->clj (JsonValue/from message))]
    (cond-> m
      (string? (:type m)) (update :type ->keyword)
      (string? (:role m)) (update :role ->keyword)
      (string? (:stop-reason m)) (update :stop-reason ->keyword))))

(defn- beta-tokens-count->map [^BetaMessageTokensCount result]
  {:input-tokens (.inputTokens result)})

(defn- ->request-options ^RequestOptions [{:keys [timeout-ms response-validation] :as opts}]
  (if (or (contains? opts :timeout-ms) (contains? opts :response-validation))
    (let [b (RequestOptions/builder)]
      (when (contains? opts :timeout-ms)
        (.timeout b (java.time.Duration/ofMillis (long timeout-ms))))
      (when (contains? opts :response-validation)
        (.responseValidation b (boolean response-validation)))
      (.build b))
    (RequestOptions/none)))

(defn- headers->map [^Headers headers]
  (into {} (map (fn [^String name] [(str/lower-case name) (vec (.values headers name))])) (.names headers)))

(defn- response-metadata [^HttpResponse response]
  (let [request-id (.requestId response)]
    {:status (.statusCode response)
     :request-id (when (.isPresent request-id) (.get request-id))
     :headers (headers->map (.headers response))}))

(defn create-beta-message
  "Send a beta Messages request and return a generic Clojure map response."
  ([^AnthropicClient client req] (create-beta-message client req {}))
  ([^AnthropicClient client req opts]
   (with-api-errors
     (let [params (->params req)
           request-options (->request-options opts)]
       (if (:include-response opts)
         (with-open [^HttpResponseFor response (.create (.withRawResponse (.messages (.beta client))) params request-options)]
           (assoc (beta-message->map (.parse response)) :response (response-metadata response)))
         (beta-message->map (.create (.messages (.beta client)) params request-options)))))))

(defn count-beta-tokens
  "Count beta Messages input tokens without creating a message."
  ([^AnthropicClient client req] (count-beta-tokens client req {}))
  ([^AnthropicClient client req opts]
   (with-api-errors
     (let [params (->count-params req)
           request-options (->request-options opts)]
       (if (:include-response opts)
         (with-open [^HttpResponseFor response (.countTokens (.withRawResponse (.messages (.beta client))) params request-options)]
           (assoc (beta-tokens-count->map (.parse response)) :response (response-metadata response)))
         (beta-tokens-count->map (.countTokens (.messages (.beta client)) params request-options)))))))
