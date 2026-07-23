(ns anthropic.beta.messages
  "Idiomatic Clojure wrapper for the beta Messages API."
  (:require [anthropic.core]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [jsonista.core :as json])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue RequestOptions)
           (com.anthropic.core.http Headers HttpResponse HttpResponseFor StreamResponse)
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
                                               MessageCreateParams$ServiceTier
                                               BetaRawMessageStreamEvent)
           (com.anthropic.models.beta.messages.batches BatchCreateParams
                                                       BatchCreateParams$Request
                                                       BatchCreateParams$Request$Params
                                                       BatchCreateParams$Request$Params$Builder
                                                       BatchCreateParams$Request$Params$ServiceTier
                                                       BatchDeleteParams BatchListPage BatchListParams
                                                       BetaDeletedMessageBatch BetaMessageBatch
                                                       BetaMessageBatchIndividualResponse)
           (com.anthropic.services.blocking.beta.messages BatchService)))

(set! *warn-on-reflection* true)

(def ^:private throw-normalized! @#'anthropic.core/throw-normalized!)
(def ^:private json-mapper (json/object-mapper {:decode-key-fn true}))

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

(defn- parse-beta-text
  "Decode the first text block of a beta response map as JSON, or nil."
  [response]
  (when-let [text (->> (:content response)
                       (filter #(= :text (:type %)))
                       first
                       :text)]
    (json/read-value text json-mapper)))

(defn create-beta-message
  "Send a beta Messages request and return a generic Clojure map response."
  ([^AnthropicClient client req] (create-beta-message client req {}))
  ([^AnthropicClient client req opts]
   (with-api-errors
     (let [params (->params req)
           request-options (->request-options opts)
           response (if (:include-response opts)
                      (with-open [^HttpResponseFor raw-response (.create (.withRawResponse (.messages (.beta client))) params request-options)]
                        (assoc (beta-message->map (.parse raw-response))
                               :response (response-metadata raw-response)))
                      (beta-message->map (.create (.messages (.beta client)) params request-options)))]
       (cond-> response
         (:response-format req) (assoc :parsed (parse-beta-text response)))))))

(defn- strip-tool-fns [params]
  (if (contains? params :tools)
    (update params :tools #(mapv (fn [tool] (dissoc tool :fn)) %))
    params))

(defn- beta-tool-fns [tools]
  (into {}
        (keep (fn [{:keys [name fn]}]
                (when fn [name fn])))
        tools))

(defn- beta-tool-result [block f]
  (try
    {:type :tool-result
     :tool-use-id (:id block)
     :content (f (:input block))}
    (catch Throwable e
      {:type :tool-result
       :tool-use-id (:id block)
       :content (or (.getMessage e) (str e))
       :is-error true})))

(defn- run-beta-tools*
  [call-fn params {:keys [max-iterations on-message]
                   :or {max-iterations 10}}]
  (let [fns (beta-tool-fns (:tools params))]
    (loop [iterations 0
           messages (cond
                      (nil? (:messages params)) []
                      (string? (:messages params)) [{:role :user :content (:messages params)}]
                      :else (vec (:messages params)))]
      (when (>= iterations max-iterations)
        (throw (ex-info "Beta tool loop exceeded max iterations"
                        {:anthropic/error :max-iterations-exceeded
                         :iterations iterations
                         :messages messages})))
      (let [response (call-fn (-> params strip-tool-fns (assoc :messages messages)))
            tool-uses (filterv #(= :tool-use (:type %)) (:content response))]
        (when on-message (on-message response))
        (if (or (= :tool-use (:stop-reason response)) (seq tool-uses))
          (let [results (mapv (fn [{:keys [name] :as block}]
                                (if-let [f (get fns name)]
                                  (beta-tool-result block f)
                                  (throw (ex-info "Tool call has no matching :fn"
                                                  {:anthropic/error :no-tool-fn :name name}))))
                              tool-uses)]
            (recur (inc iterations)
                   (conj messages
                         {:role :assistant :content (:content response)}
                         {:role :user :content results})))
          (assoc response :messages (conj messages {:role :assistant :content (:content response)})))))))

(defn run-beta-tools
  "Run a beta Messages request with local tool functions until no tool is requested."
  ([^AnthropicClient client params]
   (run-beta-tools client params {}))
  ([^AnthropicClient client params opts]
   (run-beta-tools* (partial create-beta-message client) params opts)))

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

(defn- ->batch-request-params ^BatchCreateParams$Request$Params [req]
  (let [^MessageCreateParams p (->params req)
        ^BatchCreateParams$Request$Params$Builder b
        (doto (BatchCreateParams$Request$Params/builder)
          (.maxTokens (.maxTokens p)) (.messages (.messages p)) (.model (.model p)))]
    (doseq [[value setter]
            [[(.cacheControl p) #(.cacheControl b ^BetaCacheControlEphemeral %)]
             [(.inferenceGeo p) #(.inferenceGeo b ^String %)]
             [(.mcpServers p) #(.mcpServers b ^java.util.List %)]
             [(.metadata p) #(.metadata b ^BetaMetadata %)]
             [(.outputConfig p) #(.outputConfig b ^BetaOutputConfig %)]
             [(.stopSequences p) #(.stopSequences b ^java.util.List %)]
             [(.temperature p) #(.temperature b (double %))]
             [(.thinking p) #(.thinking b ^BetaThinkingConfigParam %)]
             [(.toolChoice p) #(.toolChoice b ^BetaToolChoice %)]
             [(.tools p) #(.tools b ^java.util.List %)]
             [(.topK p) #(.topK b (long %))] [(.topP p) #(.topP b (double %))]]]
      (when (.isPresent ^java.util.Optional value) (setter (.get ^java.util.Optional value))))
    (when-let [container (:container req)] (.container b ^String container))
    (when-let [system (:system req)]
      (if (string? system) (.system b ^String system)
          (.systemOfBetaTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when-let [tier (:service-tier req)]
      (.serviceTier b (BatchCreateParams$Request$Params$ServiceTier/of
                       (-> tier name (str/replace "-" "_")))))
    (.build b)))

(defn- ->batch-request ^BatchCreateParams$Request [{:keys [custom-id params]}]
  (-> (BatchCreateParams$Request/builder) (.customId ^String custom-id)
      (.params (->batch-request-params params)) (.build)))

(defn- ->batch-create-params ^BatchCreateParams [{:keys [requests]}]
  (-> (BatchCreateParams/builder) (.requests ^java.util.List (mapv ->batch-request requests)) (.build)))

(defn- batch->map [^BetaMessageBatch batch]
  (let [m (json->clj (JsonValue/from batch))]
    (cond-> m (string? (:type m)) (update :type ->keyword)
      (string? (:processing-status m)) (update :processing-status ->keyword))))

(defn- deleted-batch->map [^BetaDeletedMessageBatch batch]
  (let [m (json->clj (JsonValue/from batch))]
    (cond-> m (string? (:type m)) (update :type ->keyword))))

(defn create-beta-batch [^AnthropicClient client req]
  (with-api-errors
    (let [^BatchService batches (-> (.beta client) (.messages) (.batches))]
      (batch->map (.create batches (->batch-create-params req))))))

(defn get-beta-batch [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^BatchService batches (-> (.beta client) (.messages) (.batches))]
      (batch->map (.retrieve batches id)))))

(defn- ->batch-list-params ^BatchListParams [{:keys [after-id before-id limit betas]}]
  (let [b (BatchListParams/builder)]
    (when after-id (.afterId b ^String after-id))
    (when before-id (.beforeId b ^String before-id))
    (when limit (.limit b (long limit)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn list-beta-batches
  ([^AnthropicClient client] (list-beta-batches client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^BatchService batches (-> (.beta client) (.messages) (.batches))
           ^BatchListPage page (.list batches (->batch-list-params opts))]
       (mapv batch->map (.autoPager page))))))

(defn cancel-beta-batch [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^BatchService batches (-> (.beta client) (.messages) (.batches))]
      (batch->map (.cancel batches id)))))

(defn delete-beta-batch [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^BatchService batches (-> (.beta client) (.messages) (.batches))
          params (-> (BatchDeleteParams/builder) (.messageBatchId id) (.build))]
      (deleted-batch->map (.delete batches params)))))

(defn- batch-result->map [^BetaMessageBatchIndividualResponse response]
  (json->clj (JsonValue/from response)))

(defn- reduce-beta-batch-result-stream [^StreamResponse sr f init]
  (with-open [^StreamResponse stream sr]
    (reduce (fn [acc response] (f acc (batch-result->map response))) init
            (iterator-seq (.iterator (.stream stream))))))

(defn reduce-beta-batch-results [^AnthropicClient client ^String id f init]
  (with-api-errors
    (let [^BatchService batches (-> (.beta client) (.messages) (.batches))]
      (reduce-beta-batch-result-stream (.resultsStreaming batches id) f init))))

(defn beta-batch-results [^AnthropicClient client ^String id]
  (reduce-beta-batch-results client id conj []))

(defn- beta-stream-event->map [^BetaRawMessageStreamEvent event]
  (let [m (json->clj (or (some-> (._json event) (.orElse nil))
                         (JsonValue/from event)))]
    (cond-> m (string? (:type m)) (update :type ->keyword))))

(defn- consume-beta-stream ^String [^StreamResponse sr on-event]
  (with-open [^StreamResponse stream sr]
    (let [sb (StringBuilder.)]
      (doseq [event (iterator-seq (.iterator (.stream stream)))]
        (let [m (beta-stream-event->map event)]
          (when-let [text (get-in m [:delta :text])] (.append sb ^String text))
          (when on-event (on-event m))))
      (str sb))))

(defn stream-beta-message ^String [^AnthropicClient client req on-event]
  (with-api-errors
    (consume-beta-stream (.createStreaming (.messages (.beta client)) (->params req)) on-event)))

(defn stream-beta-text ^String [^AnthropicClient client req on-text]
  (stream-beta-message client req
                       (fn [event]
                         (when-let [text (get-in event [:delta :text])]
                           (when on-text (on-text text))))))
