(ns anthropic.core
  "Idiomatic Clojure wrapper over the official Anthropic Java SDK
  (`com.anthropic/anthropic-java`).

  Build a request as a Clojure map, get a Clojure map back. The client reads
  `ANTHROPIC_API_KEY` from the environment by default."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [jsonista.core :as json])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.client.okhttp AnthropicOkHttpClient AnthropicOkHttpClient$Builder)
           (com.anthropic.core JsonValue LogLevel RequestOptions)
           (com.anthropic.core.http Headers HttpResponse HttpResponseFor StreamResponse)
           (com.anthropic.helpers MessageAccumulator)
           (java.net Proxy)
           (java.time Duration)
           (com.anthropic.models.beta.files DeletedFile FileListPage FileMetadata
                                            FileUploadParams)
           (com.anthropic.models.models ModelCapabilities ModelInfo ModelListPage ModelListParams)
           (com.anthropic.models.messages.batches BatchCreateParams
                                                  BatchCreateParams$Request
                                                  BatchCreateParams$Request$Params
                                                  BatchCreateParams$Request$Params$Builder
                                                  BatchCreateParams$Request$Params$ServiceTier
                                                  BatchListPage
                                                  DeletedMessageBatch MessageBatch
                                                  MessageBatchIndividualResponse
                                                  MessageBatchRequestCounts
                                                  MessageBatchResult
                                                  MessageBatchSucceededResult)
           (com.anthropic.models.messages Base64ImageSource
                                          Base64ImageSource$MediaType
                                          Base64PdfSource
                                          CacheControlEphemeral
                                          CacheControlEphemeral$Ttl
                                          CitationCharLocation
                                          CitationContentBlockLocation
                                          CitationPageLocation
                                          CitationsConfigParam
                                          CitationsSearchResultLocation
                                          CitationsWebSearchResultLocation
                                          CodeExecutionTool20260521
                                          CodeExecutionTool20260521$AllowedCaller
                                          Container
                                          ContainerUploadBlockParam
                                          ContentBlock ContentBlockParam
                                          DocumentBlockParam DocumentBlockParam$Source
                                          ImageBlockParam ImageBlockParam$Source
                                          JsonOutputFormat JsonOutputFormat$Schema
                                          Message
                                          MessageCountTokensTool
                                          MessageCountTokensParams
                                          MessageCountTokensParams$Builder
                                          MessageCreateParams
                                          MessageCreateParams$Builder
                                          MessageCreateParams$ServiceTier
                                          MessageTokensCount Metadata Model
                                          OutputConfig OutputConfig$Effort
                                          RawContentBlockDelta
                                          RawContentBlockDeltaEvent
                                          RawContentBlockStartEvent
                                          RawContentBlockStartEvent$ContentBlock
                                          RawMessageDeltaEvent
                                          RawMessageStreamEvent
                                          InputJsonDelta
                                          RedactedThinkingBlockParam
                                          SearchResultBlockParam SearchResultBlockParam$Builder
                                          TextBlock TextBlockParam TextCitation
                                          TextDelta
                                          ThinkingBlock ThinkingConfigAdaptive
                                          ThinkingConfigDisabled
                                          ThinkingConfigEnabled
                                          ThinkingConfigParam ThinkingDelta
                                          ThinkingBlockParam
                                          Tool Tool$AllowedCaller Tool$InputSchema ToolBash20250124
                                          ToolBash20250124$AllowedCaller
                                          ToolSearchToolBm25_20251119
                                          ToolSearchToolBm25_20251119$AllowedCaller
                                          ToolSearchToolBm25_20251119$Type
                                          ToolSearchToolRegex20251119
                                          ToolSearchToolRegex20251119$AllowedCaller
                                          ToolSearchToolRegex20251119$Type
                                          ToolChoice
                                          ToolChoiceAny ToolChoiceAuto
                                          ToolChoiceNone ToolChoiceTool
                                          ServerToolUseBlock
                                          ToolResultBlockParam ToolResultBlockParam$Builder
                                          ToolTextEditor20250728
                                          ToolTextEditor20250728$AllowedCaller
                                          ToolUnion ToolUseBlock
                                          ToolUseBlockParam
                                          MemoryTool20250818 MemoryTool20250818$AllowedCaller
                                          PlainTextSource UrlImageSource UrlPdfSource
                                          RefusalStopDetails
                                          CacheCreation OutputTokensDetails
                                          ServerToolUsage
                                          UserLocation
                                          WebSearchTool20260318
                                          WebSearchTool20260318$AllowedCaller
                                          WebFetchTool20260318
                                          WebFetchTool20260318$AllowedCaller
                                          Usage)
           (com.anthropic.errors AnthropicException
                                 AnthropicIoException
                                 AnthropicServiceException
                                 BadRequestException
                                 InternalServerException
                                 NotFoundException
                                 PermissionDeniedException
                                 RateLimitException
                                 UnauthorizedException
                                 UnexpectedStatusCodeException
                                 UnprocessableEntityException)))

(defn client
  "An Anthropic client. With no args, resolves credentials from the environment
  (`ANTHROPIC_API_KEY`). With a map, accepts optional `:api-key`, `:auth-token`,
  `:base-url`, `:timeout-ms`, `:max-retries`, `:webhook-key`, `:log-level`
  (`:off`/`:info`/`:error`/`:debug`), `:response-validation`, `:proxy`,
  `:headers`, and `:query-params`; only supplied keys are set on the SDK builder.
  `:configure` receives the raw builder last for SDK features not wrapped here."
  (^AnthropicClient [] (AnthropicOkHttpClient/fromEnv))
  (^AnthropicClient [{:keys [api-key auth-token base-url timeout-ms max-retries
                             webhook-key log-level response-validation proxy
                             headers query-params configure]
                      :as opts}]
   (let [^AnthropicOkHttpClient$Builder b (AnthropicOkHttpClient/builder)]
     (when api-key (.apiKey b ^String api-key))
     (when auth-token (.authToken b ^String auth-token))
     (when base-url (.baseUrl b ^String base-url))
     (when timeout-ms (.timeout b (Duration/ofMillis (long timeout-ms))))
     (when max-retries (.maxRetries b (int max-retries)))
     (when webhook-key (.webhookKey b ^String webhook-key))
     (when log-level
       (.logLevel b (case (keyword log-level)
                      :off LogLevel/OFF
                      :info LogLevel/INFO
                      :error LogLevel/ERROR
                      :debug LogLevel/DEBUG)))
     (when (contains? opts :response-validation)
       (.responseValidation b (boolean response-validation)))
     (when proxy (.proxy b ^Proxy proxy))
     (doseq [[name value] headers]
       (.putHeader b ^String name ^String value))
     (doseq [[k v] query-params]
       (.putQueryParam b ^String k ^String v))
     (when configure (configure b))
     (.build b))))

(defn- service-error-type [e]
  (condp instance? e
    BadRequestException :bad-request
    UnauthorizedException :unauthorized
    PermissionDeniedException :permission-denied
    NotFoundException :not-found
    UnprocessableEntityException :unprocessable-entity
    RateLimitException :rate-limit
    InternalServerException :internal-server
    UnexpectedStatusCodeException :unexpected-status
    :api-error))

(defn- throw-normalized!
  "Rethrow an SDK exception: service errors and I/O errors become ex-info
  keyed `:anthropic/error` with the original as cause; anything else
  propagates unchanged."
  [^Throwable e]
  (cond
    (instance? AnthropicServiceException e)
    (throw (ex-info (or (.getMessage e) "Anthropic API error")
                    {:anthropic/error :api-error
                     :status (.statusCode ^AnthropicServiceException e)
                     :error-type (service-error-type e)}
                    e))
    (instance? AnthropicIoException e)
    (throw (ex-info (or (.getMessage e) "Anthropic I/O error")
                    {:anthropic/error :io-error}
                    e))
    :else (throw e)))

(defmacro ^:private with-api-errors [& body]
  `(try ~@body
        (catch AnthropicException e# (throw-normalized! e#))))

(defn- ->json ^JsonValue [m]
  (JsonValue/from (walk/stringify-keys m)))

(defn- anthropic-error [code message data]
  (ex-info message (assoc data :anthropic/error code)))

(declare ->cache-control java->clj)

(def ^:private content-wire-types
  {:mid-conversation-system "mid_conv_system"
   :web-search-result "web_search_tool_result"
   :web-fetch-result "web_fetch_tool_result"
   :code-execution-result "code_execution_tool_result"
   :bash-code-execution-result "bash_code_execution_tool_result"
   :text-editor-code-execution-result "text_editor_code_execution_tool_result"
   :tool-search-result "tool_search_tool_result"})

(defn- ->wire-data [x]
  (cond
    (map? x) (into {}
                   (map (fn [[k v]]
                          [(-> (name k)
                               (str/replace #"([a-z0-9])([A-Z])" "$1_$2")
                               (str/replace "-" "_")
                               str/lower-case)
                           (->wire-data v)]))
                   x)
    (sequential? x) (mapv ->wire-data x)
    (keyword? x) (or (content-wire-types x)
                     (str/replace (name x) "-" "_"))
    :else x))

(defn- ->sdk-content-block ^ContentBlockParam [blk]
  (.readValue (JsonValue/access$getJSON_MAPPER$cp)
              (json/write-value-as-string (->wire-data blk))
              ContentBlockParam))

(defn- configure-tool-builder
  [b {:keys [allowed-callers cache-control defer-loading strict]} caller-of]
  (doseq [c allowed-callers]
    (.addAllowedCaller b (caller-of (name c))))
  (when cache-control (.cacheControl b (->cache-control cache-control)))
  (when (some? defer-loading) (.deferLoading b (boolean defer-loading)))
  (when (some? strict) (.strict b (boolean strict)))
  b)

(defn- ->custom-tool ^Tool [{:keys [name description input-schema] :as t}]
  (let [required (:required input-schema)
        isb (Tool$InputSchema/builder)
        tb (Tool/builder)]
    (.properties isb (->json (:properties input-schema)))
    (when (seq required) (.required isb ^java.util.List (vec required)))
    (.name tb ^String name)
    (.inputSchema tb (.build isb))
    (when description (.description tb ^String description))
    (configure-tool-builder tb t #(Tool$AllowedCaller/of %))
    (.build tb)))

(defn- ->user-location ^UserLocation [{:keys [city region country timezone]}]
  (let [b (UserLocation/builder)]
    (when city (.city b ^String city))
    (when region (.region b ^String region))
    (when country (.country b ^String country))
    (when timezone (.timezone b ^String timezone))
    (.build b)))

(def ^:private server-tool-types
  #{:web-search :web-fetch :code-execution :bash :text-editor :memory
    :tool-search})

(defn- ->web-search-tool ^WebSearchTool20260318
  [{:keys [max-uses allowed-domains blocked-domains user-location] :as t}]
  (let [b (WebSearchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (when user-location (.userLocation b (->user-location user-location)))
    (configure-tool-builder b t #(WebSearchTool20260318$AllowedCaller/of %))
    (.build b)))

(defn- ->web-fetch-tool ^WebFetchTool20260318
  [{:keys [max-uses max-content-tokens allowed-domains blocked-domains] :as t}]
  (let [b (WebFetchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when max-content-tokens (.maxContentTokens b (long max-content-tokens)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (configure-tool-builder b t #(WebFetchTool20260318$AllowedCaller/of %))
    (.build b)))

(defn- ->code-execution-tool ^CodeExecutionTool20260521
  [t]
  (let [b (CodeExecutionTool20260521/builder)]
    (configure-tool-builder b t #(CodeExecutionTool20260521$AllowedCaller/of %))
    (.build b)))

(defn- ->bash-tool ^ToolBash20250124 [t]
  (let [b (ToolBash20250124/builder)]
    (configure-tool-builder b t #(ToolBash20250124$AllowedCaller/of %))
    (.build b)))

(defn- ->text-editor-tool ^ToolTextEditor20250728
  [{:keys [max-characters] :as t}]
  (let [b (ToolTextEditor20250728/builder)]
    (when max-characters (.maxCharacters b (long max-characters)))
    (configure-tool-builder b t #(ToolTextEditor20250728$AllowedCaller/of %))
    (.build b)))

(defn- ->memory-tool ^MemoryTool20250818 [t]
  (let [b (MemoryTool20250818/builder)]
    (configure-tool-builder b t #(MemoryTool20250818$AllowedCaller/of %))
    (.build b)))

(defn- ->tool-search-bm25 ^ToolSearchToolBm25_20251119
  [{:keys [allowed-callers cache-control defer-loading strict]}]
  (let [b (ToolSearchToolBm25_20251119/builder)]
    (.type b ToolSearchToolBm25_20251119$Type/TOOL_SEARCH_TOOL_BM25_20251119)
    (doseq [c allowed-callers]
      (.addAllowedCaller b (ToolSearchToolBm25_20251119$AllowedCaller/of (name c))))
    (when cache-control (.cacheControl b ^CacheControlEphemeral (->cache-control cache-control)))
    (when (some? defer-loading) (.deferLoading b (boolean defer-loading)))
    (when (some? strict) (.strict b (boolean strict)))
    (.build b)))

(defn- ->tool-search-regex ^ToolSearchToolRegex20251119
  [{:keys [allowed-callers cache-control defer-loading strict]}]
  (let [b (ToolSearchToolRegex20251119/builder)]
    (.type b ToolSearchToolRegex20251119$Type/TOOL_SEARCH_TOOL_REGEX_20251119)
    (doseq [c allowed-callers]
      (.addAllowedCaller b (ToolSearchToolRegex20251119$AllowedCaller/of (name c))))
    (when cache-control (.cacheControl b ^CacheControlEphemeral (->cache-control cache-control)))
    (when (some? defer-loading) (.deferLoading b (boolean defer-loading)))
    (when (some? strict) (.strict b (boolean strict)))
    (.build b)))

(defn- ->server-tool
  "Map a server-side tool spec (latest version of each) to a ToolUnion."
  ^ToolUnion [{:keys [type] :as t}]
  (case (keyword type)
    :web-search (ToolUnion/ofWebSearchTool20260318 (->web-search-tool t))
    :web-fetch (ToolUnion/ofWebFetchTool20260318 (->web-fetch-tool t))
    :code-execution (ToolUnion/ofCodeExecutionTool20260521 (->code-execution-tool t))
    :bash (ToolUnion/ofBash20250124 (->bash-tool t))
    :text-editor (ToolUnion/ofTextEditor20250728 (->text-editor-tool t))
    :memory (ToolUnion/ofMemoryTool20250818 (->memory-tool t))
    :tool-search (case (keyword (:variant t))
                   :bm25 (ToolUnion/ofSearchToolBm25_20251119 (->tool-search-bm25 t))
                   :regex (ToolUnion/ofSearchToolRegex20251119 (->tool-search-regex t))
                   (throw (anthropic-error :unsupported-tool-search-variant
                                           "Unsupported tool-search variant"
                                           {:variant (:variant t)})))
    (throw (anthropic-error :unsupported-server-tool
                            "Unsupported server tool type"
                            {:type type}))))

(defn- ->count-tool ^MessageCountTokensTool [{:keys [type] :as t}]
  (case (keyword type)
    :web-search (MessageCountTokensTool/ofWebSearchTool20260318 (->web-search-tool t))
    :web-fetch (MessageCountTokensTool/ofWebFetchTool20260318 (->web-fetch-tool t))
    :code-execution (MessageCountTokensTool/ofCodeExecutionTool20260521 (->code-execution-tool t))
    :bash (MessageCountTokensTool/ofToolBash20250124 (->bash-tool t))
    :text-editor (MessageCountTokensTool/ofToolTextEditor20250728 (->text-editor-tool t))
    :memory (MessageCountTokensTool/ofMemoryTool20250818 (->memory-tool t))
    :tool-search (case (keyword (:variant t))
                   :bm25 (MessageCountTokensTool/ofToolSearchToolBm25_20251119 (->tool-search-bm25 t))
                   :regex (MessageCountTokensTool/ofToolSearchToolRegex20251119 (->tool-search-regex t))
                   (throw (anthropic-error :unsupported-tool-search-variant
                                           "Unsupported tool-search variant"
                                           {:variant (:variant t)})))
    (MessageCountTokensTool/ofTool (->custom-tool t))))

(defn- server-tool? [t] (contains? server-tool-types (keyword (:type t))))

(defn- ->tool
  "A tool spec -> ToolUnion. Custom tools are `{:name :description :input-schema}`;
  server tools are `{:type :web-search|:web-fetch|:code-execution|:bash|
  :text-editor|:memory ...}`."
  ^ToolUnion [t]
  (if (server-tool? t)
    (->server-tool t)
    (ToolUnion/ofTool (->custom-tool t))))

(defn- ->cache-control ^CacheControlEphemeral [cc]
  ;; `cc` may be `true`/`:ephemeral` (default 5m) or `{:ttl :5m|:1h}`.
  (let [b (CacheControlEphemeral/builder)]
    (when-let [ttl (and (map? cc) (:ttl cc))]
      (.ttl b (CacheControlEphemeral$Ttl/of (name ttl))))
    (.build b)))

(defn- ->image-source ^ImageBlockParam$Source [{:keys [type media-type data url]}]
  (case (keyword type)
    :base64 (ImageBlockParam$Source/ofBase64
             (-> (Base64ImageSource/builder)
                 (.data ^String data)
                 (.mediaType (Base64ImageSource$MediaType/of media-type))
                 (.build)))
    :url (ImageBlockParam$Source/ofUrl
          (-> (UrlImageSource/builder) (.url ^String url) (.build)))
    (throw (anthropic-error :unsupported-content-source
                            "Unsupported image source type"
                            {:type type}))))

(defn- ->document-source ^DocumentBlockParam$Source [{:keys [type data url]}]
  (case (keyword type)
    :base64 (DocumentBlockParam$Source/ofBase64
             (-> (Base64PdfSource/builder) (.data ^String data) (.build)))
    :url (DocumentBlockParam$Source/ofUrl
          (-> (UrlPdfSource/builder) (.url ^String url) (.build)))
    :text (DocumentBlockParam$Source/ofText
           (-> (PlainTextSource/builder) (.data ^String data) (.build)))
    (throw (anthropic-error :unsupported-content-source
                            "Unsupported document source type"
                            {:type type}))))

(defn- ->citations-config ^CitationsConfigParam [enabled]
  (-> (CitationsConfigParam/builder)
      (.enabled (boolean enabled))
      (.build)))

(defn- ->search-result-text ^TextBlockParam [{:keys [text cache-control]}]
  (let [b (-> (TextBlockParam/builder) (.text ^String text))]
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (.build b)))

(defn- ->system-block ^TextBlockParam [{:keys [text cache-control]}]
  (let [b (-> (TextBlockParam/builder) (.text ^String text))]
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (.build b)))

(defn- ->content-block ^ContentBlockParam [{:keys [type cache-control] :as blk}]
  (case (keyword type)
    :text (if (:citations blk)
            (->sdk-content-block blk)
            (let [b (-> (TextBlockParam/builder) (.text ^String (:text blk)))]
              (when cache-control (.cacheControl b (->cache-control cache-control)))
              (ContentBlockParam/ofText (.build b))))
    :image (let [b (-> (ImageBlockParam/builder)
                       (.source ^ImageBlockParam$Source (->image-source (:source blk))))]
             (when cache-control (.cacheControl b (->cache-control cache-control)))
             (ContentBlockParam/ofImage (.build b)))
    :document (->sdk-content-block
               (cond-> blk
                 (boolean? (:citations blk))
                 (assoc :citations {:enabled (:citations blk)})))
    :search-result (let [b ^SearchResultBlockParam$Builder
                         (-> (SearchResultBlockParam/builder)
                             (.source ^String (:source blk))
                             (.title ^String (:title blk))
                             (.content ^java.util.List (mapv ->search-result-text (:content blk))))]
                     (when (contains? blk :citations)
                       (.citations b (->citations-config (:citations blk))))
                     (when cache-control (.cacheControl b (->cache-control cache-control)))
                     (ContentBlockParam/ofSearchResult (.build b)))
    :thinking (ContentBlockParam/ofThinking
               (-> (ThinkingBlockParam/builder)
                   (.thinking ^String (:thinking blk))
                   (.signature ^String (:signature blk))
                   (.build)))
    :redacted-thinking (ContentBlockParam/ofRedactedThinking
                        (-> (RedactedThinkingBlockParam/builder)
                            (.data ^String (:data blk))
                            (.build)))
    :container-upload (let [b (-> (ContainerUploadBlockParam/builder)
                                  (.fileId ^String (:file-id blk)))]
                        (when cache-control (.cacheControl b (->cache-control cache-control)))
                        (ContentBlockParam/ofContainerUpload (.build b)))
    :tool-result (let [^String content-str (if (string? (:content blk))
                                             (:content blk)
                                             (json/write-value-as-string (:content blk)))
                       b ^ToolResultBlockParam$Builder (ToolResultBlockParam/builder)]
                   (.toolUseId ^ToolResultBlockParam$Builder b ^String (:tool-use-id blk))
                   (.content ^ToolResultBlockParam$Builder b content-str)
                   (when (contains? blk :is-error)
                     (.isError ^ToolResultBlockParam$Builder b (boolean (:is-error blk))))
                   (when cache-control (.cacheControl ^ToolResultBlockParam$Builder b (->cache-control cache-control)))
                   (ContentBlockParam/ofToolResult (.build b)))
    :tool-use (let [b (-> (ToolUseBlockParam/builder)
                          (.id ^String (:id blk))
                          (.name ^String (:name blk))
                          (.input (->json (:input blk))))]
                (when cache-control (.cacheControl b (->cache-control cache-control)))
                (ContentBlockParam/ofToolUse (.build b)))
    (:server-tool-use :web-search-result :web-fetch-result
     :code-execution-result :bash-code-execution-result
     :text-editor-code-execution-result :tool-search-result
     :mid-conversation-system)
    (->sdk-content-block blk)
    (throw (anthropic-error :unsupported-content-block
                            "Unsupported content block type"
                            {:type type}))))

(defn- ->thinking ^ThinkingConfigParam [{:keys [type budget-tokens]}]
  (case (keyword type)
    :enabled (ThinkingConfigParam/ofEnabled
              (-> (ThinkingConfigEnabled/builder)
                  (.budgetTokens (long budget-tokens)) (.build)))
    :disabled (ThinkingConfigParam/ofDisabled (.build (ThinkingConfigDisabled/builder)))
    :adaptive (ThinkingConfigParam/ofAdaptive (.build (ThinkingConfigAdaptive/builder)))
    (throw (anthropic-error :unsupported-thinking-type
                            "Unsupported thinking type"
                            {:type type}))))

(defn- ->tool-choice ^ToolChoice [tc]
  (if (map? tc)
    (ToolChoice/ofTool (-> (ToolChoiceTool/builder) (.name ^String (:name tc)) (.build)))
    (case (keyword tc)
      :auto (ToolChoice/ofAuto (.build (ToolChoiceAuto/builder)))
      :any (ToolChoice/ofAny (.build (ToolChoiceAny/builder)))
      :none (ToolChoice/ofNone (.build (ToolChoiceNone/builder)))
      (throw (anthropic-error :unsupported-tool-choice
                              "Unsupported tool choice"
                              {:tool-choice tc})))))

(defn- ->service-tier ^MessageCreateParams$ServiceTier [t]
  (MessageCreateParams$ServiceTier/of (-> t name (str/replace "-" "_"))))

(defn- ->metadata ^Metadata [{:keys [user-id]}]
  (-> (Metadata/builder) (.userId ^String user-id) (.build)))

(def ^:private json-mapper (json/object-mapper {:decode-key-fn true}))

(defn- ->schema ^JsonOutputFormat$Schema [schema-map]
  ;; The SDK models the JSON Schema as a free-form object, so each top-level
  ;; schema key becomes a JsonValue-typed additional property.
  (let [b (JsonOutputFormat$Schema/builder)]
    (doseq [[k v] schema-map]
      (.putAdditionalProperty b ^String (name k) (JsonValue/from (walk/stringify-keys v))))
    (.build b)))

(defn- ->output-config ^OutputConfig [schema effort]
  (let [b (OutputConfig/builder)]
    (when schema
      (.format b (-> (JsonOutputFormat/builder) (.schema (->schema schema)) (.build))))
    (when effort
      (.effort b (OutputConfig$Effort/of (name effort))))
    (.build b)))

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
  ^MessageCreateParams [{:keys [model max-tokens system messages tools
                                temperature top-p top-k stop-sequences
                                tool-choice thinking metadata service-tier
                                response-format effort container inference-geo
                                user-profile-id cache-control extra-headers
                                extra-query extra-body]
                         :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (MessageCreateParams/builder)
            (.model (Model/of model))
            (.maxTokens (long max-tokens)))]
    (when system
      (if (string? system)
        (.system b ^String system)
        (.systemOfTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when temperature (.temperature b (double temperature)))
    (when top-p (.topP b (double top-p)))
    (when top-k (.topK b (long top-k)))
    (when (seq stop-sequences) (.stopSequences b ^java.util.List (vec stop-sequences)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when thinking (.thinking b (->thinking thinking)))
    (when metadata (.metadata b (->metadata metadata)))
    (when service-tier (.serviceTier b (->service-tier service-tier)))
    (when container (.container b ^String container))
    (when inference-geo (.inferenceGeo b ^String inference-geo))
    (when user-profile-id (.userProfileId b ^String user-profile-id))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when (or response-format effort)
      (.outputConfig b (->output-config response-format effort)))
    (doseq [t tools] (.addTool b (->tool t)))
    (doseq [m messages] (add-message b m))
    (doseq [[k v] extra-headers]
      (.putAdditionalHeader b ^String k ^String v))
    (doseq [[k v] extra-query]
      (.putAdditionalQueryParam b ^String k ^String v))
    (doseq [[k v] extra-body]
      (let [^String property-name (name k)]
        (.putAdditionalBodyProperty b property-name (->json v))))
    (.build b)))

(defn- add-count-message [^MessageCountTokensParams$Builder b {:keys [role content]}]
  (let [r (keyword role)]
    (if (string? content)
      (case r
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content))
      (let [blocks (mapv ->content-block content)]
        (case r
          :user (.addUserMessageOfBlockParams b blocks)
          :assistant (.addAssistantMessageOfBlockParams b blocks))))))

(defn- ->count-params
  "Translate a request map into the SDK's MessageCountTokensParams. Accepts the
  same `:model`/`:system`/`:messages`/`:tools`/`:thinking`/`:tool-choice` keys as
  `->params`; `:max-tokens` and sampling params are ignored (not part of the
  count-tokens request)."
  ^MessageCountTokensParams [{:keys [model system messages tools thinking tool-choice
                                     response-format effort user-profile-id
                                     cache-control extra-headers extra-query
                                     extra-body]
                              :or {model "claude-opus-4-8"}}]
  (let [b (doto (MessageCountTokensParams/builder)
            (.model (Model/of model)))]
    (when system
      (if (string? system)
        (.system b ^String system)
        (.systemOfTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when thinking (.thinking b (->thinking thinking)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when user-profile-id (.userProfileId b ^String user-profile-id))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when (or response-format effort)
      (.outputConfig b (->output-config response-format effort)))
    (doseq [t tools] (.addTool b (->count-tool t)))
    (doseq [m messages] (add-count-message b m))
    (doseq [[k v] extra-headers]
      (.putAdditionalHeader b ^String k ^String v))
    (doseq [[k v] extra-query]
      (.putAdditionalQueryParam b ^String k ^String v))
    (doseq [[k v] extra-body]
      (let [^String property-name (name k)]
        (.putAdditionalBodyProperty b property-name (->json v))))
    (.build b)))

(defn- java->clj [x]
  (cond
    (instance? java.util.Map x) (persistent!
                                 (reduce-kv (fn [acc k v] (assoc! acc (keyword (str k)) (java->clj v)))
                                            (transient {}) (into {} x)))
    (instance? java.util.List x) (mapv java->clj x)
    :else x))

(defn- json->clj [^JsonValue jv]
  (java->clj (.convert jv java.lang.Object)))

(defn- ->keyword [x]
  (-> x str str/lower-case (str/replace "_" "-") keyword))

(defn- citation->map [^TextCitation c]
  (let [cl (.charLocation c)
        pl (.pageLocation c)
        cbl (.contentBlockLocation c)
        wsr (.webSearchResultLocation c)
        sr (.searchResultLocation c)]
    (cond
      (.isPresent cl) (let [x ^CitationCharLocation (.get cl)]
                        (cond-> {:type :char-location :cited-text (.citedText x)
                                 :document-index (.documentIndex x)
                                 :start-char-index (.startCharIndex x)
                                 :end-char-index (.endCharIndex x)}
                          (.isPresent (.documentTitle x)) (assoc :document-title (.get (.documentTitle x)))
                          (.isPresent (.fileId x)) (assoc :file-id (.get (.fileId x)))))
      (.isPresent pl) (let [x ^CitationPageLocation (.get pl)]
                        (cond-> {:type :page-location :cited-text (.citedText x)
                                 :document-index (.documentIndex x)
                                 :start-page-number (.startPageNumber x)
                                 :end-page-number (.endPageNumber x)}
                          (.isPresent (.documentTitle x)) (assoc :document-title (.get (.documentTitle x)))
                          (.isPresent (.fileId x)) (assoc :file-id (.get (.fileId x)))))
      (.isPresent cbl) (let [x ^CitationContentBlockLocation (.get cbl)]
                         (cond-> {:type :content-block-location :cited-text (.citedText x)
                                  :document-index (.documentIndex x)
                                  :start-block-index (.startBlockIndex x)
                                  :end-block-index (.endBlockIndex x)}
                           (.isPresent (.documentTitle x)) (assoc :document-title (.get (.documentTitle x)))
                           (.isPresent (.fileId x)) (assoc :file-id (.get (.fileId x)))))
      (.isPresent wsr) (let [x ^CitationsWebSearchResultLocation (.get wsr)]
                         (cond-> {:type :web-search-result-location :cited-text (.citedText x)
                                  :url (.url x) :encrypted-index (.encryptedIndex x)}
                           (.isPresent (.title x)) (assoc :title (.get (.title x)))))
      (.isPresent sr) (let [x ^CitationsSearchResultLocation (.get sr)]
                        (cond-> {:type :search-result-location :cited-text (.citedText x)
                                 :source (.source x)
                                 :search-result-index (.searchResultIndex x)
                                 :start-block-index (.startBlockIndex x)
                                 :end-block-index (.endBlockIndex x)}
                          (.isPresent (.title x)) (assoc :title (.get (.title x)))))
      :else {:type :other})))

(defn- block-raw
  "Best-effort raw JSON for a content block whose fields we don't map in detail."
  [^ContentBlock b kind]
  (let [j (._json b)]
    (cond-> {:type kind}
      (.isPresent j) (assoc :json (json->clj (.get j))))))

(defn- normalize-content-data [x]
  (cond
    (map? x) (reduce-kv (fn [m k v]
                          (let [k' (-> (name k)
                                       (str/replace #"([a-z0-9])([A-Z])" "$1-$2")
                                       (str/replace "_" "-")
                                       str/lower-case
                                       keyword)]
                            (assoc m k' (if (and (= k' :type) (string? v))
                                          (->keyword v)
                                          (normalize-content-data v)))))
                        {} x)
    (sequential? x) (mapv normalize-content-data x)
    :else x))

(defn- server-block->map [^ContentBlock b type]
  (-> (JsonValue/from (.toParam b))
      json->clj
      normalize-content-data
      (assoc :type type)))

(defn- compact-map [m]
  (into {} (remove (comp nil? val)) m))

(defn- block->map [^ContentBlock b]
  (let [txt (.text b)
        tu (.toolUse b)
        th (.thinking b)
        stu (.serverToolUse b)]
    (cond
      (.isPresent txt) (let [tb ^TextBlock (.get txt)
                             cits (.citations tb)]
                         (cond-> {:type :text :text (.text tb)}
                           (and (.isPresent cits) (seq (.get cits)))
                           (assoc :citations (mapv citation->map (.get cits)))))
      (.isPresent tu) (let [x ^ToolUseBlock (.get tu)]
                        {:type :tool-use
                         :id (.id x)
                         :name (.name x)
                         :input (json->clj (._input x))})
      (.isPresent th) (let [x ^ThinkingBlock (.get th)]
                        {:type :thinking
                         :thinking (.thinking x)
                         :signature (.signature x)})
      (.isPresent stu) (server-block->map b :server-tool-use)
      (.isPresent (.webSearchToolResult b)) (server-block->map b :web-search-result)
      (.isPresent (.webFetchToolResult b)) (server-block->map b :web-fetch-result)
      (.isPresent (.codeExecutionToolResult b)) (server-block->map b :code-execution-result)
      (.isPresent (.bashCodeExecutionToolResult b)) (server-block->map b :bash-code-execution-result)
      (.isPresent (.textEditorCodeExecutionToolResult b)) (server-block->map b :text-editor-code-execution-result)
      (.isPresent (.toolSearchToolResult b)) (server-block->map b :tool-search-result)
      (.isPresent (.containerUpload b)) (block-raw b :container-upload)
      (.isPresent (.redactedThinking b)) {:type :redacted-thinking}
      :else {:type :other})))

(defn- cache-creation->map [^CacheCreation c]
  {:ephemeral-1h-input-tokens (.ephemeral1hInputTokens c)
   :ephemeral-5m-input-tokens (.ephemeral5mInputTokens c)})

(defn- output-tokens-details->map [^OutputTokensDetails d]
  {:thinking-tokens (.thinkingTokens d)})

(defn- server-tool-usage->map [^ServerToolUsage s]
  {:web-fetch-requests (.webFetchRequests s)
   :web-search-requests (.webSearchRequests s)})

(defn- usage->map [^Usage u]
  (let [ccit (.cacheCreationInputTokens u)
        crit (.cacheReadInputTokens u)
        stu (.serverToolUse u)
        st (.serviceTier u)
        cc (.cacheCreation u)
        ig (.inferenceGeo u)
        otd (.outputTokensDetails u)]
    (cond-> {:input-tokens (.inputTokens u)
             :output-tokens (.outputTokens u)}
      (.isPresent ccit) (assoc :cache-creation-input-tokens (.get ccit))
      (.isPresent crit) (assoc :cache-read-input-tokens (.get crit))
      (.isPresent stu) (assoc :server-tool-use (server-tool-usage->map (.get stu)))
      (.isPresent st) (assoc :service-tier (->keyword (.get st)))
      (.isPresent cc) (assoc :cache-creation (cache-creation->map (.get cc)))
      (.isPresent ig) (assoc :inference-geo (.get ig))
      (.isPresent otd) (assoc :output-tokens-details (output-tokens-details->map (.get otd))))))

(defn- container->map [^Container c]
  {:id (.id c)
   :expires-at (str (.expiresAt c))})

(defn- stop-details->map [^RefusalStopDetails sd]
  (let [cat (.category sd)
        exp (.explanation sd)]
    (cond-> {}
      (.isPresent cat) (assoc :category (->keyword (.get cat)))
      (.isPresent exp) (assoc :explanation (.get exp)))))

(defn- message->map [^Message m]
  (let [sr (.stopReason m)
        c (.container m)
        ss (.stopSequence m)
        sd (.stopDetails m)]
    (cond-> {:id (.id m)
             :model (str (.model m))
             :role :assistant ; Messages API responses are always the assistant turn
             :stop-reason (when (.isPresent sr) (->keyword (.get sr)))
             :content (mapv block->map (.content m))
             :usage (usage->map (.usage m))}
      (.isPresent c) (assoc :container (container->map (.get c)))
      (.isPresent ss) (assoc :stop-sequence (.get ss))
      (.isPresent sd) (assoc :stop-details (stop-details->map (.get sd))))))

(defn- parse-text
  "Decode the first text block of a response map as JSON (keyword keys), or nil."
  [resp]
  (when-let [t (->> (:content resp) (filter #(= :text (:type %))) first :text)]
    (json/read-value t json-mapper)))

(defn- strip-tool-fn [tool]
  (dissoc tool :fn))

(defn- strip-tool-fns [params]
  (if (contains? params :tools)
    (update params :tools #(mapv strip-tool-fn %))
    params))

(defn- tool-fns [tools]
  (into {}
        (keep (fn [{:keys [name fn]}]
                (when fn [name fn])))
        tools))

(defn- normalize-messages [messages]
  (cond
    (nil? messages) []
    (string? messages) [{:role :user :content messages}]
    :else (vec messages)))

(defn- assistant-turn [resp]
  {:role :assistant :content (:content resp)})

(defn- tool-result-block [block f]
  (try
    {:type :tool-result
     :tool-use-id (:id block)
     :content (f (:input block))}
    (catch Throwable e
      {:type :tool-result
       :tool-use-id (:id block)
       :content (or (.getMessage e) (str e))
       :is-error true})))

(defn- tool-result-blocks [fns blocks]
  (mapv (fn [{:keys [name] :as block}]
          (let [f (get fns name)]
            (when-not f
              (throw (anthropic-error :no-tool-fn
                                      "Tool call has no matching :fn"
                                      {:name name})))
            (tool-result-block block f)))
        blocks))

(defn- run-tools*
  "Implementation for `run-tools`; `call-fn` accepts a create-message params map
  and returns a response map."
  [call-fn params {:keys [max-iterations on-message]
                   :or {max-iterations 10}}]
  (let [fns (tool-fns (:tools params))]
    (loop [iterations 0
           messages (normalize-messages (:messages params))]
      (when (>= iterations max-iterations)
        (throw (anthropic-error :max-iterations-exceeded
                                "Tool loop exceeded max iterations"
                                {:iterations iterations
                                 :messages messages})))
      (let [req (-> params
                    strip-tool-fns
                    (assoc :messages messages))
            resp (call-fn req)]
        (when on-message (on-message resp))
        (if (= :tool-use (:stop-reason resp))
          (let [blocks (filterv #(= :tool-use (:type %)) (:content resp))
                results (tool-result-blocks fns blocks)]
            (recur (inc iterations)
                   (conj messages
                         (assistant-turn resp)
                         {:role :user :content results})))
          (assoc resp :messages (conj messages (assistant-turn resp))))))))

(defn- ->request-options ^RequestOptions [{:keys [timeout-ms response-validation] :as opts}]
  (if (or (contains? opts :timeout-ms)
          (contains? opts :response-validation))
    (let [b (RequestOptions/builder)]
      (when (contains? opts :timeout-ms)
        (.timeout b (Duration/ofMillis (long timeout-ms))))
      (when (contains? opts :response-validation)
        (.responseValidation b (boolean response-validation)))
      (.build b))
    (RequestOptions/none)))

(defn- headers->map [^Headers headers]
  (into {}
        (map (fn [^String name]
               [(str/lower-case name) (vec (.values headers name))]))
        (.names headers)))

(defn- response-metadata [^HttpResponse r]
  (let [request-id (.requestId r)]
    {:status (.statusCode r)
     :request-id (when (.isPresent request-id) (.get request-id))
     :headers (headers->map (.headers r))}))

(defn create-message
  "Send a Messages request and return the response as a Clojure map.

  `req` keys: `:model` (string, defaults to \"claude-opus-4-8\"), `:max-tokens`
  (defaults to 1024), `:system` (a string or text-block maps supporting
  `:cache-control`; system-block citations are not wrapped), `:messages` (a seq of
  `{:role :user|:assistant :content \"...\"}`), `:tools`, and the optional
  controls `:temperature`, `:top-p`, `:top-k`, `:stop-sequences` (seq of
  strings), `:tool-choice` (`:auto`/`:any`/`:none` or `{:type :tool :name \"x\"}`),
  `:thinking` (`{:type :enabled :budget-tokens N}` / `{:type :adaptive}` /
  `{:type :disabled}`), `:metadata` (`{:user-id \"...\"}`), and `:service-tier`
  (`:auto`/`:standard-only`). Custom tools accept `:cache-control`. The request
  escape hatches `:extra-headers`, `:extra-query`, and `:extra-body` pass
  unwrapped values to the SDK builder. For structured output, pass
  `:response-format` (a
  JSON Schema map) and/or `:effort` (`:low`…`:max`); when `:response-format` is
  set the returned map also carries `:parsed`, the response text decoded as a
  Clojure map. An optional third `opts` map accepts `:timeout-ms`,
  `:response-validation`, and truthy `:include-response`; the latter adds raw
  HTTP `:response` metadata (`:status`, `:request-id`, and lowercase headers).
  Returns
  `{:id :model :role :stop-reason :content [...] :usage {...}}`. See also
  `run-tools` for hand-rolled tool execution over this request shape."
  ([^AnthropicClient client req]
   (create-message client req {}))
  ([^AnthropicClient client req opts]
   (with-api-errors
     (let [params (->params req)
           request-options (->request-options opts)
           [resp response]
           (if (:include-response opts)
             (with-open [^HttpResponseFor r (.create (.withRawResponse (.messages client))
                                                     params request-options)]
               [(message->map (.parse r)) (response-metadata r)])
             [(message->map (.create (.messages client) params request-options)) nil])]
       (cond-> resp
         (:response-format req) (assoc :parsed (parse-text resp))
         response (assoc :response response))))))

(defn run-tools
  "Run a Messages request with local tool functions until the model stops asking
  for tools. Tools may include `:fn`, a function of parsed tool input returning
  the tool result; `:fn` is stripped before each API call. Returns the final
  response map from `create-message` plus `:messages`, the accumulated
  conversation including the final assistant turn.

  `opts`: `:max-iterations` (default 10 create-message calls) and
  `:on-message` (called with each response map)."
  ([^AnthropicClient client params]
   (run-tools client params {}))
  ([^AnthropicClient client params opts]
   (run-tools* (partial create-message client) params opts)))

(defn count-tokens
  "Count the input tokens a request would use, without sending it. Takes the same
  `req` map as `create-message` (sampling params and `:max-tokens` are ignored).
  Shared system blocks, tool cache control, and `:extra-headers`/`:extra-query`/
  `:extra-body` request escape hatches are supported.
  An optional third `opts` map accepts `:timeout-ms`, `:response-validation`, and
  truthy `:include-response`; the latter adds raw HTTP response metadata.
  Returns `{:input-tokens n}`."
  ([^AnthropicClient client req]
   (count-tokens client req {}))
  ([^AnthropicClient client req opts]
   (with-api-errors
     (let [params (->count-params req)
           request-options (->request-options opts)]
       (if (:include-response opts)
         (with-open [^HttpResponseFor r (.countTokens (.withRawResponse (.messages client))
                                                      params request-options)]
           (assoc {:input-tokens (.inputTokens ^MessageTokensCount (.parse r))}
                  :response (response-metadata r)))
         (let [^MessageTokensCount r (.countTokens (.messages client) params request-options)]
           {:input-tokens (.inputTokens r)}))))))

(defn- model->map [^ModelInfo m]
  (let [mit (.maxInputTokens m)
        mt (.maxTokens m)
        caps (.capabilities m)]
    (cond-> {:id (.id m)
             :display-name (.displayName m)
             :created-at (str (.createdAt m))}
      (.isPresent mit) (assoc :max-input-tokens (.get mit))
      (.isPresent mt) (assoc :max-tokens (.get mt))
      (.isPresent caps) (assoc :capabilities
                               (-> (JsonValue/from ^ModelCapabilities (.get caps))
                                   json->clj
                                   normalize-content-data)))))

(defn- ->model-list-params ^ModelListParams
  [{:keys [limit before-id after-id]}]
  (let [b (ModelListParams/builder)]
    (when limit (.limit b (long limit)))
    (when before-id (.beforeId b ^String before-id))
    (when after-id (.afterId b ^String after-id))
    (.build b)))

(defn list-models
  "List the available models as a seq of maps, newest first. Each map has `:id`,
  `:display-name`, `:created-at` (ISO-8601 string), and `:max-input-tokens` /
  `:max-tokens` and `:capabilities` when the API reports them. Pages are followed
  automatically. Optional `opts`: `:limit`, `:before-id`, and `:after-id`."
  ([^AnthropicClient client]
   (list-models client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^ModelListPage p (-> (.models client) (.list (->model-list-params opts)))]
       (mapv model->map (.autoPager p))))))

(defn get-model
  "Retrieve one model's info by id, as a map shaped like `list-models`' entries."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (model->map (-> (.models client) (.retrieve id)))))

;; ---- Message Batches ------------------------------------------------------

(defn- add-batch-message [^BatchCreateParams$Request$Params$Builder b {:keys [role content]}]
  (let [r (keyword role)]
    (if (string? content)
      (case r
        :user (.addUserMessage b ^String content)
        :assistant (.addAssistantMessage b ^String content))
      (let [blocks (mapv ->content-block content)]
        (case r
          :user (.addUserMessageOfBlockParams b blocks)
          :assistant (.addAssistantMessageOfBlockParams b blocks))))))

(defn- ->batch-req-params
  "Translate a per-request map into batch params through `->params` so both
  request surfaces preserve the same stable fields."
  ^BatchCreateParams$Request$Params
  [req]
  (let [^MessageCreateParams p (->params req)
        b (doto (BatchCreateParams$Request$Params/builder)
            (.maxTokens (.maxTokens p))
            (.messages (.messages p))
            (.model (.model p)))]
    (doseq [[value setter]
            [[(.cacheControl p) #(.cacheControl b %)]
             [(.container p) #(.container b ^String %)]
             [(.inferenceGeo p) #(.inferenceGeo b ^String %)]
             [(.metadata p) #(.metadata b %)]
             [(.outputConfig p) #(.outputConfig b %)]
             [(.stopSequences p) #(.stopSequences b ^java.util.List %)]
             [(.temperature p) #(.temperature b (double %))]
             [(.thinking p) #(.thinking b %)]
             [(.toolChoice p) #(.toolChoice b %)]
             [(.tools p) #(.tools b ^java.util.List %)]
             [(.topK p) #(.topK b (long %))]
             [(.topP p) #(.topP b (double %))]]]
      (when (.isPresent ^java.util.Optional value)
        (setter (.get ^java.util.Optional value))))
    (when-let [system (:system req)]
      (if (string? system)
        (.system b ^String system)
        (.systemOfTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when-let [tier (:service-tier req)]
      (.serviceTier b (BatchCreateParams$Request$Params$ServiceTier/of
                       (-> tier name (str/replace "-" "_")))))
    (.build b)))

(defn- ->batch-request ^BatchCreateParams$Request [{:keys [custom-id params]}]
  (-> (BatchCreateParams$Request/builder)
      (.customId ^String custom-id)
      (.params (->batch-req-params params))
      (.build)))

(defn- counts->map [^MessageBatchRequestCounts c]
  {:processing (.processing c) :succeeded (.succeeded c) :errored (.errored c)
   :canceled (.canceled c) :expired (.expired c)})

(defn- batch->map [^MessageBatch b]
  (let [ended (.endedAt b)
        url (.resultsUrl b)]
    (cond-> {:id (.id b)
             :processing-status (->keyword (.processingStatus b))
             :request-counts (counts->map (.requestCounts b))
             :created-at (str (.createdAt b))
             :expires-at (str (.expiresAt b))}
      (.isPresent ended) (assoc :ended-at (str (.get ended)))
      (.isPresent url) (assoc :results-url (.get url)))))

(defn- batch-result->map [^MessageBatchIndividualResponse r]
  (let [res ^MessageBatchResult (.result r)
        s (.succeeded res)]
    {:custom-id (.customId r)
     :result (cond
               (.isPresent s) {:type :succeeded
                               :message (message->map
                                         (.message ^MessageBatchSucceededResult (.get s)))}
               (.isPresent (.errored res)) {:type :errored}
               (.isPresent (.canceled res)) {:type :canceled}
               (.isPresent (.expired res)) {:type :expired}
               :else {:type :unknown})}))

(defn create-batch
  "Submit a Message Batch. `requests` is a seq of
  `{:custom-id \"...\" :params <same map as create-message>}`. Returns the batch
  as a map (see `get-batch`)."
  [^AnthropicClient client requests]
  (with-api-errors
    (let [reqs ^java.util.List (mapv ->batch-request requests)
          bp (-> (BatchCreateParams/builder) (.requests reqs) (.build))]
      (batch->map (-> (.messages client) (.batches) (.create bp))))))

(defn get-batch
  "Retrieve a batch by id. Returns `{:id :processing-status :request-counts
  :created-at :expires-at}` plus `:ended-at`/`:results-url` once available."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (batch->map (-> (.messages client) (.batches) (.retrieve id)))))

(defn list-batches
  "List all batches (pages followed) as a seq of maps like `get-batch`."
  [^AnthropicClient client]
  (with-api-errors
    (let [^BatchListPage p (-> (.messages client) (.batches) (.list))]
      (mapv batch->map (.autoPager p)))))

(defn cancel-batch
  "Request cancellation of a batch; returns the updated batch map."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (batch->map (-> (.messages client) (.batches) (.cancel id)))))

(defn delete-batch
  "Delete a batch by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^DeletedMessageBatch d (-> (.messages client) (.batches) (.delete id))]
      {:id (.id d) :deleted true})))

(defn- reduce-batch-result-stream
  [^StreamResponse sr f init]
  (with-open [^StreamResponse s sr]
    (reduce (fn [acc r] (f acc (batch-result->map r)))
            init
            (iterator-seq (.iterator (.stream s))))))

(defn reduce-batch-results
  "Fetch a completed batch's results and reduce over result maps without
  retaining the full result set. Calls `(f acc result-map)` for each
  `{:custom-id ... :result {:type :succeeded|:errored|:canceled|:expired ...}}`
  and closes the underlying results stream automatically."
  [^AnthropicClient client ^String id f init]
  (with-api-errors
    (reduce-batch-result-stream
     (-> (.messages client) (.batches) (.resultsStreaming id))
     f
     init)))

(defn batch-results
  "Fetch a completed batch's results as a vector of
  `{:custom-id ... :result {:type :succeeded|:errored|:canceled|:expired ...}}`;
  succeeded results include the parsed `:message`. The results stream is closed
  automatically. Use `reduce-batch-results` to consume large result sets without
  retaining them."
  [^AnthropicClient client ^String id]
  (reduce-batch-results client id conj []))

(defn- start-block->map [^RawContentBlockStartEvent$ContentBlock cb]
  (let [m (-> (JsonValue/from cb) json->clj normalize-content-data)]
    (update m :type {:web-search-tool-result :web-search-result
                     :web-fetch-tool-result :web-fetch-result
                     :code-execution-tool-result :code-execution-result
                     :bash-code-execution-tool-result :bash-code-execution-result
                     :text-editor-code-execution-tool-result :text-editor-code-execution-result
                     :tool-search-tool-result :tool-search-result}
            (:type m))))

(defn- delta->map [index ^RawContentBlockDelta d]
  (let [t (.text d) ij (.inputJson d) cs (.citations d)
        th (.thinking d) sg (.signature d)]
    (cond
      (.isPresent t) {:type :text-delta :index index :text (.text ^TextDelta (.get t))}
      (.isPresent ij) {:type :input-json-delta :index index
                       :partial-json (.partialJson ^InputJsonDelta (.get ij))}
      (.isPresent cs) {:type :citations-delta :index index
                       :citation (-> (JsonValue/from
                                     (.citation ^com.anthropic.models.messages.CitationsDelta
                                                 (.get cs)))
                                     json->clj
                                     normalize-content-data
                                     compact-map)}
      (.isPresent th) {:type :thinking-delta :index index
                       :thinking (.thinking ^ThinkingDelta (.get th))}
      (.isPresent sg) {:type :signature-delta :index index
                       :signature (.signature ^com.anthropic.models.messages.SignatureDelta
                                              (.get sg))}
      :else {:type :delta :index index})))

(defn- event->map
  "Normalize one `RawMessageStreamEvent` into a Clojure map keyed by `:type`."
  [^RawMessageStreamEvent ev]
  (let [cbs (.contentBlockStart ev)
        cbd (.contentBlockDelta ev)
        cbp (.contentBlockStop ev)
        md (.messageDelta ev)]
    (cond
      (.isPresent cbs) (let [e ^RawContentBlockStartEvent (.get cbs)]
                         {:type :content-block-start
                          :index (.index e)
                          :block (start-block->map (.contentBlock e))})
      (.isPresent cbd) (let [e ^RawContentBlockDeltaEvent (.get cbd)]
                         (delta->map (.index e) (.delta e)))
      (.isPresent cbp) {:type :content-block-stop
                        :index (.index ^com.anthropic.models.messages.RawContentBlockStopEvent (.get cbp))}
      (.isPresent md) (let [e ^RawMessageDeltaEvent (.get md)
                            delta (.delta e)
                            sr (.stopReason delta)
                            ss (.stopSequence delta)
                            c (.container delta)
                            sd (.stopDetails delta)]
                        (cond-> {:type :message-delta
                                 :stop-reason (when (.isPresent sr) (->keyword (.get sr)))
                                 :usage (-> (JsonValue/from (.usage e))
                                            json->clj
                                            normalize-content-data
                                            compact-map)}
                          (.isPresent ss) (assoc :stop-sequence (.get ss))
                          (.isPresent c) (assoc :container (container->map (.get c)))
                          (.isPresent sd) (assoc :stop-details (stop-details->map (.get sd)))))
      (.isPresent (.messageStart ev))
      {:type :message-start
       :message (message->map
                 (.message ^com.anthropic.models.messages.RawMessageStartEvent
                           (.get (.messageStart ev))))}
      (.isPresent (.messageStop ev)) {:type :message-stop}
      :else {:type :other})))

(defn stream
  "Stream a Messages request, invoking `on-event` with a normalized event map for
  every server-sent event as it arrives, and returning the full concatenated
  assistant text when the stream ends. Takes the same `req` map as
  `create-message`. The underlying HTTP stream is closed automatically.

  Event maps are keyed by `:type`: `:message-start`, `:content-block-start`
  (`:index`, `:block`), `:text-delta`/`:thinking-delta`/`:input-json-delta`/
  `:signature-delta`/`:citations-delta` (`:index` plus the payload),
  `:content-block-stop` (`:index`), `:message-delta` (usage and stop metadata),
  and `:message-stop`. `:message-start` includes `:message`; block starts include
  the complete initial content block. To
  reconstruct a streamed tool call, accumulate `:input-json-delta` `:partial-json`
  per `:index` (the matching `:content-block-start` carries the tool `:id`/`:name`)."
  ^String [^AnthropicClient client req on-event]
  (with-api-errors
    (with-open [^StreamResponse sr (.createStreaming (.messages client) (->params req))]
      (let [sb (StringBuilder.)]
        (doseq [ev (iterator-seq (.iterator (.stream sr)))]
          (let [m (event->map ev)]
            (when (= :text-delta (:type m)) (.append sb ^String (:text m)))
            (when on-event (on-event m))))
        (str sb)))))

(defn stream-message
  "Stream a Messages request and return the fully reconstructed message map.
  Calls `on-event` with each normalized event map as it arrives. Unlike `stream`,
  the result includes all content blocks, tool inputs, usage, and stop metadata.
  When `req` has `:response-format`, the result also includes `:parsed`. The
  underlying HTTP stream is closed automatically."
  [^AnthropicClient client req on-event]
  (with-api-errors
    (with-open [^StreamResponse sr (.createStreaming (.messages client) (->params req))]
      (let [^MessageAccumulator acc (MessageAccumulator/create)]
        (doseq [^RawMessageStreamEvent ev (iterator-seq (.iterator (.stream sr)))]
          (.accumulate acc ev)
          (when on-event (on-event (event->map ev))))
        (let [resp (message->map (.message acc))]
          (cond-> resp
            (:response-format req) (assoc :parsed (parse-text resp))))))))

(defn stream-text
  "Stream a Messages request, calling `on-text` with each text delta (a string)
  as it arrives, and returning the full concatenated text when the stream ends.
  Takes the same `req` map as `create-message`. A thin convenience over `stream`
  that ignores every non-text event; reach for `stream` when you need thinking or
  tool-use deltas. The underlying HTTP stream is closed automatically."
  ^String [^AnthropicClient client req on-text]
  (stream client req
          (fn [m] (when (and on-text (= :text-delta (:type m))) (on-text (:text m))))))

;; ---- Files (beta) ---------------------------------------------------------

(defn- file->map [^FileMetadata f]
  (cond-> {:id (.id f)
           :filename (.filename f)
           :mime-type (.mimeType f)
           :size-bytes (.sizeBytes f)
           :created-at (str (.createdAt f))}
    (.isPresent (.downloadable f)) (assoc :downloadable (.get (.downloadable f)))))

(defn- ->upload-params ^FileUploadParams [file]
  (let [b (FileUploadParams/builder)]
    (cond
      (bytes? file) (.file b ^bytes file)
      (instance? java.io.File file) (.file b (.toPath ^java.io.File file))
      (instance? java.nio.file.Path file) (.file b ^java.nio.file.Path file)
      (instance? java.io.InputStream file) (.file b ^java.io.InputStream file)
      (string? file) (.file b (.toPath (java.io.File. ^String file)))
      :else (throw (IllegalArgumentException.
                    "upload-file expects a path string, java.io.File, Path, InputStream, or byte[]")))
    (.build b)))

(defn upload-file
  "Upload a file (a path string, `java.io.File`, `java.nio.file.Path`,
  `InputStream`, or byte array) to the Files API. Returns its metadata map
  (see `get-file`). Uses the beta Files API."
  [^AnthropicClient client file]
  (with-api-errors
    (file->map (-> (.beta client) (.files) (.upload (->upload-params file))))))

(defn get-file
  "Retrieve a file's metadata by id: `{:id :filename :mime-type :size-bytes
  :created-at}` plus `:downloadable` when reported."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (file->map (-> (.beta client) (.files) (.retrieveMetadata id)))))

(defn list-files
  "List uploaded files (pages followed) as a seq of maps like `get-file`."
  [^AnthropicClient client]
  (with-api-errors
    (let [^FileListPage p (-> (.beta client) (.files) (.list))]
      (mapv file->map (.autoPager p)))))

(defn delete-file
  "Delete a file by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^DeletedFile d (-> (.beta client) (.files) (.delete id))]
      {:id (.id d) :deleted true})))

(defn download-file
  "Download a file's contents by id, returning a byte array. The HTTP response
  is closed automatically."
  ^bytes [^AnthropicClient client ^String id]
  (with-api-errors
    (with-open [^HttpResponse r (-> (.beta client) (.files) (.download id))]
      (.readAllBytes (.body r)))))
