(ns anthropic.core
  "Clojure wrapper over the official Anthropic Java SDK
  (`com.anthropic/anthropic-java`).

  Build a request as a Clojure map, get a Clojure map back. The client reads
  `ANTHROPIC_API_KEY` from the environment by default."
  (:require [anthropic.pagination :as pagination]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [jsonista.core :as json]
            [anthropic.stream :as stream-control])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.client.okhttp AnthropicOkHttpClient AnthropicOkHttpClient$Builder)
           (com.anthropic.core JsonValue LogLevel MultipartField RequestOptions)
           (com.anthropic.core.http Headers HttpResponse HttpResponseFor StreamResponse)
           (com.anthropic.helpers MessageAccumulator)
           (java.net Proxy)
           (java.time Duration)
           (com.anthropic.models.files DeletedFile DeletedFile$Type FileMetadata
                                       FileListPage FileListParams FileUploadParams)
           (com.anthropic.models.skills DeletedSkill Skill SkillCreateParams
                                         SkillListPage SkillListParams SkillSource
                                         SkillRetrieveParams)
           (com.anthropic.models.skills.versions DeletedSkillVersion
                                                 SkillVersion VersionCreateParams
                                                 VersionListPage VersionListParams
                                                 VersionRetrieveParams VersionDeleteParams)
           (com.anthropic.models.models ModelCapabilities ModelInfo ModelListPage ModelListParams)
           (com.anthropic.models.messages.batches BatchCreateParams
                                                  BatchCreateParams$Request
                                                  BatchCreateParams$Request$Params
                                                  BatchCreateParams$Request$Params$Builder
                                                  BatchCreateParams$Request$Params$ServiceTier
                                                  BatchListParams
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
                                          ContainerParams
                                          ContainerUploadBlockParam
                                          ContentBlock ContentBlockParam
                                          DocumentBlockParam$Source
                                          FileDocumentSource FileImageSource
                                          ImageBlockParam ImageBlockParam$Source
                                          ImageTransformationsParam
                                          ImageTransformationsParam$OversizedImage
                                          JsonOutputFormat JsonOutputFormat$Schema
                                          StructuredOutputConfig
                                          Message
                                          MessageCountTokensTool
                                          MessageCountTokensParams
                                          MessageCountTokensParams$Builder
                                          MessageCreateParams
                                          MessageCreateParams$Builder
                                          MessageCreateParams$ServiceTier
                                          MessageCreateParamsContainer
                                          MessageTokensCount Metadata Model
                                          OutputConfig OutputConfig$Effort
                                          RedactedThinkingBlock
                                          RawContentBlockDelta
                                          RawContentBlockDeltaEvent
                                          RawContentBlockStartEvent
                                          RawContentBlockStartEvent$ContentBlock
                                          RawMessageDeltaEvent
                                          RawMessageStreamEvent
                                          InputJsonDelta
                                          RedactedThinkingBlockParam
                                          SearchResultBlockParam SearchResultBlockParam$Builder
                                          ServerToolCaller ServerToolCaller20260120
                                          TextBlock TextBlockParam TextCitation
                                          TextDelta
                                          ThinkingBlock ThinkingConfigAdaptive
                                          ThinkingConfigDisabled
                                          ThinkingConfigEnabled
                                          ThinkingConfigParam ThinkingDelta
                                          ThinkingBlockParam
                                          Tool Tool$AllowedCaller Tool$InputExample Tool$InputExample$Builder
                                          Tool$InputSchema Tool$InputSchema$Properties
                                          Tool$InputSchema$Properties$Builder Tool$InputSchema$Builder
                                          ToolBash20250124
                                          ToolBash20250124$AllowedCaller
                                          ToolSearchToolBm25_20251119
                                          ToolSearchToolBm25_20251119$AllowedCaller
                                          ToolSearchToolBm25_20251119$Type
                                          ToolSearchToolRegex20251119
                                          ToolSearchToolRegex20251119$AllowedCaller
                                          ToolSearchToolRegex20251119$Type
                                          ToolChoice
                                          ToolChoiceAny ToolChoiceAny$Builder
                                          ToolChoiceAuto ToolChoiceAuto$Builder
                                          ToolChoiceNone ToolChoiceTool
                                          ToolChoiceTool$Builder
                                          ToolResultBlockParam ToolResultBlockParam$Builder
                                          ToolTextEditor20250728
                                          ToolTextEditor20250728$AllowedCaller
                                          ToolUnion ToolUseBlock ToolUseBlock$Caller
                                          ToolUseBlockParam
                                          MemoryTool20250818 MemoryTool20250818$AllowedCaller
                                          PlainTextSource UrlImageSource UrlPdfSource
                                          RefusalStopDetails
                                          CacheCreation OutputTokensDetails
                                          ServerToolUsage
                                          SkillParams SkillParams$Type
                                          UserLocation
                                          WebSearchTool20260318
                                          WebSearchTool20260318$AllowedCaller
                                          WebSearchTool20260318$ResponseInclusion
                                          WebFetchTool20260318
                                          WebFetchTool20260318$AllowedCaller
                                          WebFetchTool20260318$ResponseInclusion
                                          Usage)
           (com.anthropic.errors AnthropicException
                                 AnthropicIoException
                                 AnthropicRetryableException
                                 AnthropicServiceException
                                 BadRequestException
                                 CredentialResolutionException
                                 InternalServerException
                                 NoCredentialsException
                                 NotFoundException
                                 PermissionDeniedException
                                 RateLimitException
                                 SseException
                                 UnauthorizedException
                                 UnexpectedStatusCodeException
                                 UnprocessableEntityException)))

(declare headers->map json->clj ->keyword)

(def models
  "Keyword aliases for the SDK's named models. Any raw model-id string is still accepted."
  {:claude-opus-5 "claude-opus-5"
   :claude-sonnet-5 "claude-sonnet-5"
   :claude-fable-5 "claude-fable-5"
   :claude-mythos-5 "claude-mythos-5"
   :claude-opus-4-8 "claude-opus-4-8"
   :claude-opus-4-7 "claude-opus-4-7"
   :claude-mythos-preview "claude-mythos-preview"
   :claude-opus-4-6 "claude-opus-4-6"
   :claude-sonnet-4-6 "claude-sonnet-4-6"
   :claude-haiku-4-5 "claude-haiku-4-5"
   :claude-haiku-4-5-20251001 "claude-haiku-4-5-20251001"
   :claude-opus-4-5 "claude-opus-4-5"
   :claude-opus-4-5-20251101 "claude-opus-4-5-20251101"
   :claude-sonnet-4-5 "claude-sonnet-4-5"
   :claude-sonnet-4-5-20250929 "claude-sonnet-4-5-20250929"
   :claude-opus-4-1 "claude-opus-4-1"
   :claude-opus-4-1-20250805 "claude-opus-4-1-20250805"})

(defn- ->model-string [model]
  (if (keyword? model)
    (or (get models model)
        (throw (ex-info "Unknown model keyword"
                        {:anthropic/error :unknown-model
                         :model model
                         :known (sort (keys models))})))
    model))

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

;; These helpers use reflection so bedrock/vertex remain optional dependencies.
(defn- optional-class [class-name alias]
  (try
    (Class/forName class-name)
    (catch ClassNotFoundException e
      (throw (ex-info (str "Optional " (name alias) " dependency is not on the classpath")
                      {:anthropic/error :missing-optional-dependency
                       :alias alias}
                      e)))))

(defn- static-call [^Class cls method args]
  (clojure.lang.Reflector/invokeStaticMethod cls method (object-array args)))

(defn- instance-call [target method & args]
  (clojure.lang.Reflector/invokeInstanceMethod target method (object-array args)))

(defn- backend-client ^AnthropicClient [backend]
  (let [b (AnthropicOkHttpClient/builder)]
    (.backend b backend)
    (.build b)))

(defn bedrock-client
  "Build an `AnthropicClient` backed by Amazon Bedrock. Requires the optional
  `:bedrock` deps.edn alias. Options: `:region`, `:api-key`,
  `:aws-credentials`, `:aws-credentials-provider`, and `:configure`, which
  receives the Bedrock backend builder last. With no args, loads backend
  settings from the AWS environment."
  (^AnthropicClient []
   (let [cls (optional-class "com.anthropic.bedrock.backends.BedrockBackend" :bedrock)
         b (static-call cls "builder" [])]
     (instance-call b "fromEnv")
     (backend-client (instance-call b "build"))))
  (^AnthropicClient [{:keys [region api-key aws-credentials
                             aws-credentials-provider configure]}]
   (let [cls (optional-class "com.anthropic.bedrock.backends.BedrockBackend" :bedrock)
         b (static-call cls "builder" [])]
     (when region
       (let [region-class (optional-class "software.amazon.awssdk.regions.Region" :bedrock)]
         (instance-call b "region" (static-call region-class "of" [region]))))
     (when api-key (instance-call b "apiKey" api-key))
     (when aws-credentials (instance-call b "awsCredentials" aws-credentials))
     (when aws-credentials-provider
       (instance-call b "awsCredentialsProvider" aws-credentials-provider))
     (when configure (configure b))
     (backend-client (instance-call b "build")))))

(defn vertex-client
  "Build an `AnthropicClient` backed by Google Vertex AI. Requires the optional
  `:vertex` deps.edn alias. Options: `:region`, `:project`,
  `:google-credentials`, `:access-token`, `:base-url`, and `:configure`, which receives the
  Vertex backend builder last. With no args, loads backend settings and
  application-default credentials from the environment."
  (^AnthropicClient []
   (let [cls (optional-class "com.anthropic.vertex.backends.VertexBackend" :vertex)
         b (static-call cls "builder" [])]
     (instance-call b "fromEnv")
     (backend-client (instance-call b "build"))))
  (^AnthropicClient [{:keys [region project base-url google-credentials access-token configure]}]
   (let [cls (optional-class "com.anthropic.vertex.backends.VertexBackend" :vertex)
         b (static-call cls "builder" [])
         credentials (or google-credentials
                         (when access-token
                           (let [token-class (optional-class
                                              "com.google.auth.oauth2.AccessToken" :vertex)
                                 credentials-class (optional-class
                                                    "com.google.auth.oauth2.GoogleCredentials"
                                                    :vertex)
                                 token (.newInstance
                                        (.getConstructor token-class
                                                         (into-array Class
                                                                     [String java.util.Date]))
                                        (object-array [access-token nil]))]
                             (static-call credentials-class "create" [token]))))]
     (when region (instance-call b "region" region))
     (when project (instance-call b "project" project))
     (when base-url (instance-call b "baseUrl" base-url))
     (when credentials (instance-call b "googleCredentials" credentials))
     (when configure (configure b))
     (backend-client (instance-call b "build")))))

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

(defn- error-classification [e]
  (cond
    (instance? SseException e) :stream-error
    (instance? RateLimitException e) :rate-limit
    (or (instance? BadRequestException e)
        (instance? UnprocessableEntityException e)) :invalid-request
    (or (instance? UnauthorizedException e)
        (instance? PermissionDeniedException e)
        (instance? CredentialResolutionException e)
        (instance? NoCredentialsException e)) :auth
    (or (instance? InternalServerException e)
        (instance? UnexpectedStatusCodeException e)
        (instance? AnthropicRetryableException e)
        (instance? AnthropicIoException e)) :retryable
    :else :api-error))

(defn- retryable-error? [e]
  (contains? #{:stream-error :rate-limit :retryable}
             (error-classification e)))

(defn- request-id-from-headers [headers]
  (some-> (or (get headers "request-id") (get headers "x-request-id")) first))

(defn- throw-normalized!
  "Rethrow an SDK exception: service errors and I/O errors become ex-info
  keyed `:anthropic/error` with the original as cause; anything else
  propagates unchanged."
  [^Throwable e]
  (cond
    (instance? AnthropicServiceException e)
    (let [se ^AnthropicServiceException e
          headers (headers->map (.headers se))
          sdk-type (.errorType se)]
      (throw (ex-info (or (.getMessage e) "Anthropic API error")
                      (cond-> {:anthropic/error :api-error
                               :status (.statusCode se)
                               :error-type (service-error-type e)
                               :classification (error-classification e)
                               :retryable (retryable-error? e)
                               :headers headers
                               :body (json->clj (.body se))}
                        (request-id-from-headers headers)
                        (assoc :request-id (request-id-from-headers headers))
                        (.isPresent sdk-type)
                        (assoc :sdk-error-type
                               (->keyword (.asString ^com.anthropic.models.ErrorType
                                                     (.get sdk-type)))))
                      e)))
    (instance? AnthropicIoException e)
    (throw (ex-info (or (.getMessage e) "Anthropic I/O error")
                    {:anthropic/error :io-error
                     :classification :retryable
                     :retryable true}
                    e))
    (or (instance? AnthropicRetryableException e)
        (instance? CredentialResolutionException e)
        (instance? NoCredentialsException e))
    (throw (ex-info (or (.getMessage e) "Anthropic client error")
                    {:anthropic/error :api-error
                     :classification (error-classification e)
                     :retryable (retryable-error? e)}
                    e))
    :else (throw e)))

(defmacro ^:private with-api-errors [& body]
  `(binding [pagination/*error-handler*
             (fn [e#]
               (if (instance? AnthropicException e#)
                 (throw-normalized! e#)
                 (throw e#)))]
     (try ~@body
          (catch AnthropicException e# (throw-normalized! e#)))))

(defn- ->json ^JsonValue [m]
  (JsonValue/from (walk/stringify-keys m)))

(defn- anthropic-error [code message data]
  (ex-info message (assoc data :anthropic/error code)))

(defn- validate-allowed-domains! [t]
  (when (and (contains? t :allowed-domains)
             (empty? (:allowed-domains t)))
    (throw (anthropic-error :empty-allowed-domains
                            "Allowed domains must contain at least one domain"
                            {:allowed-domains []}))))

(defn- missing-key! [k]
  (throw (ex-info (str "Missing required key " k)
                  {:anthropic/error :missing-key :key k})))

(declare ->cache-control java->clj)

(def ^:private content-wire-types
  {:web-search-result "web_search_tool_result"
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

(defn- ->text-citations ^java.util.List [text citations]
  (.get ^java.util.Optional
        (.citations ^TextBlockParam
                    (.get ^java.util.Optional
                          (.text ^ContentBlockParam
                                 (->sdk-content-block
                                  {:type :text :text text :citations citations}))))))

(defn- configure-tool-builder
  [{:keys [allowed-callers cache-control defer-loading strict]}
   {:keys [add-allowed-caller cache-control! defer-loading! strict!]}]
  (doseq [c allowed-callers]
    (add-allowed-caller c))
  (when cache-control (cache-control! (->cache-control cache-control)))
  (when (some? defer-loading) (defer-loading! defer-loading))
  (when (some? strict) (strict! strict)))

(defn- ->custom-input-example ^Tool$InputExample [example]
  (let [b (Tool$InputExample$Builder.)]
    (doseq [[k v] example]
      (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->stable-input-examples [class-name examples]
  (mapv (fn [example]
          (let [^Class builder-class
                (Class/forName (str class-name "$InputExample$Builder"))
                builder (.newInstance builder-class)]
            (doseq [[k v] example]
              (instance-call builder "putAdditionalProperty" ^String (name k) (->json v)))
            (instance-call builder "build")))
        examples))

(defn- ->custom-tool ^Tool [{:keys [name description input-schema] :as t}]
  (let [schema (or input-schema {})
        ^Tool$InputSchema$Properties$Builder properties (Tool$InputSchema$Properties/builder)
        ^Tool$InputSchema$Builder schema-builder (-> (Tool$InputSchema/builder)
                                                     (.type ^JsonValue (->json (or (:type schema) "object"))))
        ^com.anthropic.models.messages.Tool$Builder b (-> (Tool/builder)
                                                          (.name ^String name))]
    (doseq [[k v] (:properties schema)]
      (.putAdditionalProperty properties ^String (clojure.core/name k) (->json v)))
    (when (contains? schema :properties)
      (.properties schema-builder (.build properties)))
    (when (seq (:required schema))
      (.required schema-builder ^java.util.List (vec (:required schema))))
    (.inputSchema b (.build schema-builder))
    (when description (.description b ^String description))
    (when (some? (:eager-input-streaming t))
      (.eagerInputStreaming b (boolean (:eager-input-streaming t))))
    (when (seq (:input-examples t))
      (.inputExamples b ^java.util.List (mapv ->custom-input-example (:input-examples t))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.Tool$Builder b
                                               (Tool$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.Tool$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.Tool$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.Tool$Builder b (boolean %))})
    (.build b)))

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
  [{:keys [max-uses allowed-domains blocked-domains user-location response-inclusion] :as t}]
  (validate-allowed-domains! t)
  (let [b (WebSearchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (when user-location (.userLocation b (->user-location user-location)))
    (when response-inclusion
      (.responseInclusion b (WebSearchTool20260318$ResponseInclusion/of (name response-inclusion))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.WebSearchTool20260318$Builder b
                                               (WebSearchTool20260318$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.WebSearchTool20260318$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.WebSearchTool20260318$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.WebSearchTool20260318$Builder b (boolean %))})
    (.build b)))

(defn- ->citations-config ^CitationsConfigParam [enabled]
  (-> (CitationsConfigParam/builder)
      (.enabled (boolean (if (map? enabled) (:enabled enabled) enabled)))
      (.build)))

(defn- ->web-fetch-tool ^WebFetchTool20260318
  [{:keys [max-uses max-content-tokens allowed-domains blocked-domains use-cache citations response-inclusion] :as t}]
  (validate-allowed-domains! t)
  (let [b (WebFetchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when max-content-tokens (.maxContentTokens b (long max-content-tokens)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (when (some? use-cache) (.useCache b (boolean use-cache)))
    (when citations (.citations b (->citations-config citations)))
    (when response-inclusion
      (.responseInclusion b (WebFetchTool20260318$ResponseInclusion/of (name response-inclusion))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.WebFetchTool20260318$Builder b
                                               (WebFetchTool20260318$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.WebFetchTool20260318$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.WebFetchTool20260318$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.WebFetchTool20260318$Builder b (boolean %))})
    (.build b)))

(defn- ->code-execution-tool ^CodeExecutionTool20260521
  [t]
  (let [b (CodeExecutionTool20260521/builder)]
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.CodeExecutionTool20260521$Builder b
                                               (CodeExecutionTool20260521$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.CodeExecutionTool20260521$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.CodeExecutionTool20260521$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.CodeExecutionTool20260521$Builder b (boolean %))})
    (.build b)))

(defn- ->bash-tool ^ToolBash20250124
  [{:keys [input-examples] :as t}]
  (let [b (ToolBash20250124/builder)]
    (when (seq input-examples)
      (.inputExamples b ^java.util.List
                      (->stable-input-examples
                       "com.anthropic.models.messages.ToolBash20250124"
                       input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.ToolBash20250124$Builder b
                                               (ToolBash20250124$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.ToolBash20250124$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.ToolBash20250124$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.ToolBash20250124$Builder b (boolean %))})
    (.build b)))

(defn- ->text-editor-tool ^ToolTextEditor20250728
  [{:keys [max-characters input-examples] :as t}]
  (let [b (ToolTextEditor20250728/builder)]
    (when max-characters (.maxCharacters b (long max-characters)))
    (when (seq input-examples)
      (.inputExamples b ^java.util.List
                      (->stable-input-examples
                       "com.anthropic.models.messages.ToolTextEditor20250728"
                       input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.ToolTextEditor20250728$Builder b
                                               (ToolTextEditor20250728$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.ToolTextEditor20250728$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.ToolTextEditor20250728$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.ToolTextEditor20250728$Builder b (boolean %))})
    (.build b)))

(defn- ->memory-tool ^MemoryTool20250818
  [{:keys [input-examples] :as t}]
  (let [b (MemoryTool20250818/builder)]
    (when (seq input-examples)
      (.inputExamples b ^java.util.List
                      (->stable-input-examples
                       "com.anthropic.models.messages.MemoryTool20250818"
                       input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^com.anthropic.models.messages.MemoryTool20250818$Builder b
                                               (MemoryTool20250818$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^com.anthropic.models.messages.MemoryTool20250818$Builder b ^CacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^com.anthropic.models.messages.MemoryTool20250818$Builder b (boolean %))
      :strict! #(.strict ^com.anthropic.models.messages.MemoryTool20250818$Builder b (boolean %))})
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

(def ^:private stable-server-tool-variants
  {:bash
   {"20250124" {:builder "com.anthropic.models.messages.ToolBash20250124$Builder"
                :union "ofBash20250124"
                :count "ofToolBash20250124"
                :features #{:common :input-examples}}
    }
   :memory
   {"20250818" {:builder "com.anthropic.models.messages.MemoryTool20250818$Builder"
                :union "ofMemoryTool20250818"
                :count "ofMemoryTool20250818"
                :features #{:common :input-examples}}
    }
   :code-execution
   {"20250522" {:builder "com.anthropic.models.messages.CodeExecutionTool20250522$Builder"
                :union "ofCodeExecutionTool20250522"
                :count "ofCodeExecutionTool20250522"
                :features #{:common}}
    "20250825" {:builder "com.anthropic.models.messages.CodeExecutionTool20250825$Builder"
                :union "ofCodeExecutionTool20250825"
                :count "ofCodeExecutionTool20250825"
                :features #{:common}}
    "20260120" {:builder "com.anthropic.models.messages.CodeExecutionTool20260120$Builder"
                :union "ofCodeExecutionTool20260120"
                :count "ofCodeExecutionTool20260120"
                :features #{:common}}
    "20260521" {:builder "com.anthropic.models.messages.CodeExecutionTool20260521$Builder"
                :union "ofCodeExecutionTool20260521"
                :count "ofCodeExecutionTool20260521"
                :features #{:common}}}
   :text-editor
   {"20250124" {:builder "com.anthropic.models.messages.ToolTextEditor20250124$Builder"
                :union "ofTextEditor20250124"
                :count "ofToolTextEditor20250124"
                :features #{:common :input-examples}}
    "20250429" {:builder "com.anthropic.models.messages.ToolTextEditor20250429$Builder"
                :union "ofTextEditor20250429"
                :count "ofToolTextEditor20250429"
                :features #{:common :input-examples}}
    "20250728" {:builder "com.anthropic.models.messages.ToolTextEditor20250728$Builder"
                :union "ofTextEditor20250728"
                :count "ofToolTextEditor20250728"
                :features #{:common :max-characters :input-examples}}}
   :web-search
   {"20250305" {:builder "com.anthropic.models.messages.WebSearchTool20250305$Builder"
                :union "ofWebSearchTool20250305"
                :count "ofWebSearchTool20250305"
                :features #{:common :max-uses :domains :user-location}}
    "20260209" {:builder "com.anthropic.models.messages.WebSearchTool20260209$Builder"
                :union "ofWebSearchTool20260209"
                :count "ofWebSearchTool20260209"
                :features #{:common :max-uses :domains :user-location}}
    "20260318" {:builder "com.anthropic.models.messages.WebSearchTool20260318$Builder"
                :union "ofWebSearchTool20260318"
                :count "ofWebSearchTool20260318"
                :features #{:common :max-uses :domains :user-location :response-inclusion}}}
   :web-fetch
   {"20250910" {:builder "com.anthropic.models.messages.WebFetchTool20250910$Builder"
                :union "ofWebFetchTool20250910"
                :count "ofWebFetchTool20250910"
                :features #{:common :domains :max-uses :max-content-tokens :citations}}
    "20260209" {:builder "com.anthropic.models.messages.WebFetchTool20260209$Builder"
                :union "ofWebFetchTool20260209"
                :count "ofWebFetchTool20260209"
                :features #{:common :domains :max-uses :max-content-tokens :citations}}
    "20260309" {:builder "com.anthropic.models.messages.WebFetchTool20260309$Builder"
                :union "ofWebFetchTool20260309"
                :count "ofWebFetchTool20260309"
                :features #{:common :domains :max-uses :max-content-tokens :citations :use-cache}}
    "20260318" {:builder "com.anthropic.models.messages.WebFetchTool20260318$Builder"
                :union "ofWebFetchTool20260318"
                :count "ofWebFetchTool20260318"
                :features #{:common :domains :max-uses :max-content-tokens :citations :use-cache :response-inclusion}}}})

(defn- ->version-string [version]
  (if (keyword? version) (name version) version))

(defn- unsupported-server-tool-version [family version]
  (throw (anthropic-error :unsupported-server-tool-version
                          "Unsupported server tool version"
                          {:family family :type family :version version})))

(defn- select-server-tool-version [family versions version]
  (let [selected (or (some-> version ->version-string)
                     (last (sort (keys versions))))]
    (or (get versions selected)
        (unsupported-server-tool-version family version))))

(defn- ->versioned-tool-builder
  [^String builder-class {:keys [features] :as t}]
  (when (contains? features :domains)
    (validate-allowed-domains! t))
  (let [tool-class (Class/forName (subs builder-class 0
                                        (- (count builder-class) 8)))
        b (static-call tool-class "builder" [])
        caller-class (Class/forName (str (subs builder-class 0
                                               (- (count builder-class) 8))
                                         "$AllowedCaller"))]
    (when (contains? features :max-uses)
      (when-let [max-uses (:max-uses t)] (instance-call b "maxUses" (long max-uses))))
    (when (contains? features :max-content-tokens)
      (when-let [max-content-tokens (:max-content-tokens t)]
        (instance-call b "maxContentTokens" (long max-content-tokens))))
    (when (contains? features :domains)
      (when (seq (:allowed-domains t))
        (instance-call b "allowedDomains" ^java.util.List (vec (:allowed-domains t))))
      (when (seq (:blocked-domains t))
        (instance-call b "blockedDomains" ^java.util.List (vec (:blocked-domains t)))))
    (when (contains? features :user-location)
      (when-let [user-location (:user-location t)]
        (instance-call b "userLocation" (->user-location user-location))))
    (when (contains? features :citations)
      (when-let [citations (:citations t)]
        (instance-call b "citations" (->citations-config citations))))
    (when (contains? features :use-cache)
      (when (some? (:use-cache t))
        (instance-call b "useCache" (boolean (:use-cache t)))))
    (when (contains? features :max-characters)
      (when-let [max-characters (:max-characters t)]
        (instance-call b "maxCharacters" (long max-characters))))
    (when (contains? features :input-examples)
      (when (seq (:input-examples t))
        (instance-call b "inputExamples"
                       ^java.util.List
                       (->stable-input-examples
                        (subs builder-class 0 (- (count builder-class) 8))
                        (:input-examples t)))))
    (when (contains? features :response-inclusion)
      (when-let [response-inclusion (:response-inclusion t)]
        (let [tool-class (subs builder-class 0 (- (count builder-class) 8))
              response-class (Class/forName (str tool-class "$ResponseInclusion"))]
          (instance-call b "responseInclusion"
                         (static-call response-class "of" [(name response-inclusion)])))))
    (configure-tool-builder
     t
     {:add-allowed-caller
      #(instance-call b "addAllowedCaller"
                      (static-call caller-class "of" [(name %)]))
      :cache-control! #(instance-call b "cacheControl" %)
      :defer-loading! #(instance-call b "deferLoading" (boolean %))
      :strict! #(instance-call b "strict" (boolean %))})
    (instance-call b "build")))

(defn- ->versioned-server-tool
  ^ToolUnion [family {:keys [version] :as t}]
  (let [entry (select-server-tool-version
               family (get stable-server-tool-variants family) version)]
    (static-call ToolUnion (:union entry)
                 [(->versioned-tool-builder (:builder entry)
                                             (assoc t :features (:features entry)))])))

(defn- ->versioned-count-tool
  ^MessageCountTokensTool [family {:keys [version] :as t}]
  (let [entry (select-server-tool-version
               family (get stable-server-tool-variants family) version)]
    (static-call MessageCountTokensTool (:count entry)
                 [(->versioned-tool-builder (:builder entry)
                                             (assoc t :features (:features entry)))])))

(def ^:private stable-tool-search-versions #{"20251119"})

(defn- validate-tool-search-version [version]
  (when (and version
             (not (contains? stable-tool-search-versions
                             (->version-string version))))
    (unsupported-server-tool-version :tool-search version)))

(defn- ->server-tool
  "Map a server-side tool spec to a dated ToolUnion variant."
  ^ToolUnion [{:keys [type] :as t}]
  (case (keyword type)
    (:web-search :web-fetch :code-execution :bash :text-editor :memory)
    (->versioned-server-tool (keyword type) t)
    :tool-search (do
                   (validate-tool-search-version (:version t))
                   (case (keyword (:variant t))
                   :bm25 (ToolUnion/ofSearchToolBm25_20251119 (->tool-search-bm25 t))
                   :regex (ToolUnion/ofSearchToolRegex20251119 (->tool-search-regex t))
                   (throw (anthropic-error :unsupported-tool-search-variant
                                           "Unsupported tool-search variant"
                                           {:variant (:variant t)}))))
    (throw (anthropic-error :unsupported-server-tool
                            "Unsupported server tool type"
                            {:type type}))))

(defn- ->count-tool ^MessageCountTokensTool [{:keys [type] :as t}]
  (case (keyword type)
    (:web-search :web-fetch :code-execution :bash :text-editor :memory)
    (->versioned-count-tool (keyword type) t)
    :tool-search (do
                   (validate-tool-search-version (:version t))
                   (case (keyword (:variant t))
                   :bm25 (MessageCountTokensTool/ofToolSearchToolBm25_20251119 (->tool-search-bm25 t))
                   :regex (MessageCountTokensTool/ofToolSearchToolRegex20251119 (->tool-search-regex t))
                   (throw (anthropic-error :unsupported-tool-search-variant
                                           "Unsupported tool-search variant"
                                           {:variant (:variant t)}))))
    (MessageCountTokensTool/ofTool (->custom-tool t))))

(defn- server-tool? [t] (contains? server-tool-types (keyword (:type t))))

(defn- ->tool
  "A tool spec -> ToolUnion. Custom tools are `{:name :description :input-schema}`;
  input schemas support optional `:type` (defaulting to `\"object\"`),
  `:properties`, and `:required` keys;
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

(defn- ->image-source ^ImageBlockParam$Source [{:keys [type media-type data url file-id]}]
  (case (keyword type)
    :base64 (ImageBlockParam$Source/ofBase64
             (-> (Base64ImageSource/builder)
                 (.data ^String data)
                 (.mediaType (Base64ImageSource$MediaType/of media-type))
                 (.build)))
    :url (ImageBlockParam$Source/ofUrl
          (-> (UrlImageSource/builder) (.url ^String url) (.build)))
    :file (ImageBlockParam$Source/ofFile
           (-> (FileImageSource/builder) (.fileId ^String file-id) (.build)))
    (throw (anthropic-error :unsupported-content-source
                            "Unsupported image source type"
                            {:type type}))))

(defn- ->document-source ^DocumentBlockParam$Source [{:keys [type data url file-id]}]
  (case (keyword type)
    :base64 (DocumentBlockParam$Source/ofBase64
             (-> (Base64PdfSource/builder) (.data ^String data) (.build)))
    :url (DocumentBlockParam$Source/ofUrl
          (-> (UrlPdfSource/builder) (.url ^String url) (.build)))
    :file (DocumentBlockParam$Source/ofFile
           (-> (FileDocumentSource/builder) (.fileId ^String file-id) (.build)))
    :text (DocumentBlockParam$Source/ofText
           (-> (PlainTextSource/builder) (.data ^String data) (.build)))
    (throw (anthropic-error :unsupported-content-source
                            "Unsupported document source type"
                            {:type type}))))

(defn- ->image-transformations ^ImageTransformationsParam [{:keys [oversized-image]}]
  (let [b (ImageTransformationsParam/builder)]
    (when oversized-image
      (.oversizedImage b
                       (ImageTransformationsParam$OversizedImage/of
                        (name oversized-image))))
    (.build b)))

(defn- ->skill-params ^SkillParams [{:keys [skill-id type version]}]
  (let [b (-> (SkillParams/builder)
              (.skillId ^String skill-id)
              (.type (SkillParams$Type/of (name type))))]
    (when version (.version b ^String version))
    (.build b)))

(defn- ->container-params ^ContainerParams [{:keys [id skills]}]
  (let [b (ContainerParams/builder)]
    (when id (.id b ^String id))
    (when skills (.skills b ^java.util.List (mapv ->skill-params skills)))
    (.build b)))

(defn- ->search-result-text ^TextBlockParam [{:keys [text cache-control citations]}]
  (let [b (-> (TextBlockParam/builder) (.text ^String text))]
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when citations (.citations b (->text-citations text citations)))
    (.build b)))

(defn- ->system-block ^TextBlockParam [{:keys [text cache-control citations]}]
  (let [b (-> (TextBlockParam/builder) (.text ^String text))]
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when citations (.citations b (->text-citations text citations)))
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
             (when-let [transformations (:transformations blk)]
               (.transformations b (->image-transformations transformations)))
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
     :text-editor-code-execution-result :tool-search-result)
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
    (case (keyword (:type tc))
      :auto (let [b ^ToolChoiceAuto$Builder (ToolChoiceAuto/builder)]
              (when (contains? tc :disable-parallel-tool-use)
                (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
              (ToolChoice/ofAuto (.build b)))
      :any (let [b ^ToolChoiceAny$Builder (ToolChoiceAny/builder)]
             (when (contains? tc :disable-parallel-tool-use)
               (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
             (ToolChoice/ofAny (.build b)))
      :none (if (contains? tc :disable-parallel-tool-use)
              ;; The API has no parallel-use control to disable when no tool runs.
              (throw (anthropic-error :unsupported-disable-parallel-tool-use
                                      "Tool choice :none has no parallel tool use to disable"
                                      {:tool-choice tc}))
              (ToolChoice/ofNone (.build (ToolChoiceNone/builder))))
      (let [b ^ToolChoiceTool$Builder (ToolChoiceTool/builder)]
        (.name b ^String (:name tc))
        (when (contains? tc :disable-parallel-tool-use)
          (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
        (ToolChoice/ofTool (.build b))))
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

(def ^:private malli-schema-tags
  #{:map :map-of :vector :sequential :set :tuple :enum :maybe :and :or :not
    :string :int :integer :double :float :number :boolean :keyword :symbol
    :nil :any :re :fn})

(defn- malli-schema? [x]
  (and (vector? x) (contains? malli-schema-tags (first x))))

(defn- optional-resolve [sym]
  (try
    (requiring-resolve sym)
    (catch java.io.FileNotFoundException _ nil)))

(defn- malli-functions []
  (let [validate (optional-resolve 'malli.core/validate)
        explain (optional-resolve 'malli.core/explain)
        transform (optional-resolve 'malli.json-schema/transform)]
    (when-not (and validate explain transform)
      (throw (anthropic-error :missing-optional-dependency
                              "Malli structured output requires the :malli alias"
                              {:dependency 'metosin/malli
                               :alias :malli})))
    {:validate validate :explain explain :transform transform}))

(defn- spec-form [x]
  (if (keyword? x)
    (if-let [spec (s/get-spec x)] (s/form spec) x)
    (if (seq? x) x (s/form (s/spec x)))))

(declare spec->json-schema)

(defn- predicate->json-schema [p]
  (let [same? (fn [f] (or (= p f) (= (str p) (str f))))]
    (cond
      (same? string?) {:type "string"}
      (same? keyword?) {:type "string"}
      (same? symbol?) {:type "string"}
      (same? boolean?) {:type "boolean"}
      (same? integer?) {:type "integer"}
      (same? int?) {:type "integer"}
      (same? number?) {:type "number"}
      (same? double?) {:type "number"}
      (same? float?) {:type "number"}
      (same? nil?) {:type "null"}
      (same? map?) {:type "object"}
      (same? vector?) {:type "array"}
      (same? seq?) {:type "array"}
      (same? any?) {}
      :else (throw (anthropic-error :unsupported-schema
                                    "Unsupported clojure.spec predicate for JSON Schema"
                                    {:schema p})))))

(defn- spec-key-name [k] (name k))

(defn- spec-keys->json-schema [[_ & options]]
  (let [opts (apply hash-map options)
        required (mapv spec-key-name (concat (:req opts) (:req-un opts)))
        optional (concat (:opt opts) (:opt-un opts))
        properties (into {}
                         (map (fn [k]
                                [(spec-key-name k)
                                 (spec->json-schema (or (s/get-spec k)
                                                        (throw (anthropic-error
                                                                :unsupported-schema
                                                                "Spec key has no registered spec"
                                                                {:key k}))))]))
                         (concat (:req opts) (:req-un opts) optional))]
    (cond-> {:type "object" :properties properties :additionalProperties false}
      (seq required) (assoc :required required))))

(defn- spec->json-schema [x]
  (let [form (spec-form x)]
    (cond
      (keyword? form)
      (throw (anthropic-error :unsupported-schema
                              "Spec keyword must resolve to a registered spec"
                              {:schema x}))
      (symbol? form) (predicate->json-schema (some-> (resolve form) deref))
      (seq? form)
      (case (first form)
        clojure.spec.alpha/keys (spec-keys->json-schema form)
        clojure.spec.alpha/and {:allOf (mapv spec->json-schema (rest form))}
        clojure.spec.alpha/or {:anyOf (mapv (fn [[tag schema]]
                                             (assoc (spec->json-schema schema) :title (name tag)))
                                           (partition 2 (rest form)))}
        clojure.spec.alpha/nilable {:anyOf [(spec->json-schema (second form))
                                            {:type "null"}]}
        (if (= 'clojure.spec.alpha/every (first form))
          {:type "array"}
          (predicate->json-schema (some-> (resolve (first form)) deref))))
      :else (predicate->json-schema form))))

(defn- ->structured-schema [schema]
  (if (map? schema)
    {:schema schema :validator nil}
    (if (malli-schema? schema)
      (let [{:keys [validate explain transform]} (malli-functions)]
        {:schema (transform schema)
         :validator (fn [value] (boolean (validate schema value)))
         :explain (fn [value] (explain schema value))})
      {:schema (spec->json-schema schema)
       :validator (fn [value] (s/valid? schema value))
       :explain (fn [value] (s/explain-data schema value))})))

(defn- validate-parsed! [value schema]
  (let [{:keys [validator explain]} (->structured-schema schema)]
    (if (or (nil? validator) (validator value))
      value
      (throw (anthropic-error :response-validation-failed
                              "Structured response failed validation"
                              {:value value
                               :schema schema
                               :validation-errors (when explain (explain value))})))))

(declare parse-text)

(defn- parsed-response [resp req opts]
  (if-let [schema (or (:response-format req)
                      (when-not (instance? Class (:output-type req))
                        (:output-type req)))]
    (let [parsed (parse-text resp)]
      (assoc resp :parsed (if (:response-validation opts)
                            (validate-parsed! parsed schema)
                            parsed)))
    resp))

(defn- ->output-config ^OutputConfig [schema effort output-type]
  (if (instance? Class output-type)
    (let [b (StructuredOutputConfig/builder)]
      (.format b ^Class output-type)
      (when effort (.effort b (OutputConfig$Effort/of (name effort))))
      (.rawOutputConfig (.build b)))
    (let [b (OutputConfig/builder)]
      (when (or schema output-type)
        (.format b (-> (JsonOutputFormat/builder)
                       (.schema (->schema
                                 (:schema (->structured-schema (or output-type schema)))))
                       (.build))))
      (when effort
        (.effort b (OutputConfig$Effort/of (name effort))))
      (.build b))))

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
                                response-format output-type effort container inference-geo
                                user-profile-id cache-control extra-headers
                                extra-query extra-body]
                         :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (MessageCreateParams/builder)
            (.model (Model/of (->model-string model)))
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
    (when container
      (if (string? container)
        (.container b ^String container)
        (.container b ^ContainerParams (->container-params container))))
    (when inference-geo (.inferenceGeo b ^String inference-geo))
    (when user-profile-id (.userProfileId b ^String user-profile-id))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when (or response-format output-type effort)
      (.outputConfig b (->output-config response-format effort output-type)))
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
  count-tokens request). Web-fetch tools accept `:use-cache` and `:citations`."
  ^MessageCountTokensParams [{:keys [model system messages tools thinking tool-choice
                                     response-format output-type effort user-profile-id
                                     cache-control extra-headers extra-query
                                     extra-body]
                              :or {model "claude-opus-4-8"}}]
  (let [b (doto (MessageCountTokensParams/builder)
            (.model (Model/of (->model-string model))))]
    (when system
      (if (string? system)
        (.system b ^String system)
        (.systemOfTextBlockParams b ^java.util.List (mapv ->system-block system))))
    (when thinking (.thinking b (->thinking thinking)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when user-profile-id (.userProfileId b ^String user-profile-id))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (when (or response-format output-type effort)
      (.outputConfig b (->output-config response-format effort output-type)))
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

(defn- caller->map [^ToolUseBlock$Caller caller]
  (cond
    (nil? caller) (throw (anthropic-error :unsupported-caller
                                          "Unsupported tool-use caller"
                                          {}))
    (.isDirect caller) {:type :direct}
    (.isCodeExecution20250825 caller)
    (let [^ServerToolCaller value (.asCodeExecution20250825 caller)]
      {:type :code-execution-20250825
       :tool-id (.toolId value)})
    (.isCodeExecution20260120 caller)
    (let [^ServerToolCaller20260120 value (.asCodeExecution20260120 caller)]
      {:type :code-execution-20260120
       :tool-id (.toolId value)})
    :else (throw (anthropic-error :unsupported-caller
                                  "Unsupported tool-use caller"
                                  {}))))

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
                         :input (json->clj (._input x))
                         :caller (caller->map (.caller x))})
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
      (.isPresent (.redactedThinking b)) (let [x ^RedactedThinkingBlock
                                               (.get (.redactedThinking b))]
                                           {:type :redacted-thinking
                                            :data (.data x)})
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

(defn- container-skill->map [^com.anthropic.models.messages.ContainerSkill s]
  {:skill-id (.skillId s)
   :type (->keyword (.asString (.type s)))
   :version (.version s)})

(defn- container->map [^Container c]
  (let [skills (.skills c)]
    (cond-> {:id (.id c)
             :expires-at (str (.expiresAt c))}
      (and (.isPresent skills) (seq (.get skills)))
      (assoc :skills (mapv container-skill->map (.get skills))))))

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
  `:cache-control` and `:citations`), `:messages` (a seq of
  `{:role :user|:assistant :content \"...\"}`), `:tools`, and the optional
  controls `:temperature`, `:top-p`, `:top-k`, `:stop-sequences` (seq of
  strings), `:tool-choice` (`:auto`/`:any`/`:none` or a map with `:type`/`:name`
  and optional `:disable-parallel-tool-use`),
  `:thinking` (`{:type :enabled :budget-tokens N}` / `{:type :adaptive}` /
  `{:type :disabled}`), `:metadata` (`{:user-id \"...\"}`), and `:service-tier`
  (`:auto`/`:standard-only`). Web-fetch tools accept `:use-cache` and
  `:citations`; latest web-search and web-fetch tools accept `:response-inclusion`;
  bash, text-editor, and memory tools accept `:input-examples`; custom tools accept `:input-schema` with optional `:type`,
  `:properties`, and `:required` keys, plus `:cache-control`,
  `:eager-input-streaming`, and `:input-examples`. The request
  escape hatches `:extra-headers`, `:extra-query`, and `:extra-body` pass
  unwrapped values to the SDK builder. For structured output, pass
  `:response-format` (a
  JSON Schema map), `:output-type` (a Java `Class`, Malli schema, or registered
  clojure.spec keyword/form), and/or `:effort` (`:low`…`:max`); `:output-type`
  uses the Java Class path only for a Class and otherwise converts the schema.
  When `:response-format` or a non-Class `:output-type` is set the returned map
  also carries `:parsed`, the response text decoded as a Clojure map. An
  optional third `opts` map accepts
  `:timeout-ms`, `:response-validation` (also validates Malli/spec decoded
  values), and truthy `:include-response`; the latter adds raw
  HTTP `:response` metadata (`:status`, `:request-id`, and lowercase headers).
  Returns
  `{:id :model :role :stop-reason :content [...] :usage {...}}`; tool-use content
  blocks include a plain-data `:caller` map with a `:type` discriminator, and
  redacted thinking content blocks include `{:type :redacted-thinking :data \"...\"}`.
  See also
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
       (cond-> (parsed-response resp req opts)
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
  Shared system blocks, including `:cache-control` and `:citations`, tool cache control, and `:extra-headers`/`:extra-query`/
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
  [{:keys [limit before-id after-id betas]}]
  (let [b (ModelListParams/builder)]
    (when limit (.limit b (long limit)))
    (when before-id (.beforeId b ^String before-id))
    (when after-id (.afterId b ^String after-id))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn list-models
  "List the available models as a seq of maps, newest first. Each map has `:id`,
  `:display-name`, `:created-at` (ISO-8601 string), and `:max-input-tokens` /
  `:max-tokens` and `:capabilities` when the API reports them. Pages are followed
  automatically. Optional `opts`: `:limit`, `:before-id`, `:after-id`, and free-form
  string or keyword `:betas`."
  ([^AnthropicClient client]
   (list-models client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^ModelListPage p (-> (.models client) (.list (->model-list-params opts)))]
       (mapv model->map (.autoPager p))))))

(defn get-model
  "Get one model's info by id, as a map shaped like `list-models`' entries."
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
  [{:keys [stream] :as req}]
  (let [^MessageCreateParams p (->params req)
        ^BatchCreateParams$Request$Params$Builder b (doto (BatchCreateParams$Request$Params/builder)
            (.maxTokens (.maxTokens p))
            (.messages (.messages p))
            (.model (.model p)))]
    (doseq [[value setter]
            [[(.cacheControl p) #(.cacheControl b ^CacheControlEphemeral %)]
             [(.container p) #(.container b ^MessageCreateParamsContainer %)]
             [(.inferenceGeo p) #(.inferenceGeo b ^String %)]
             [(.metadata p) #(.metadata b ^Metadata %)]
             [(.outputConfig p) #(.outputConfig b ^OutputConfig %)]
             [(.stopSequences p) #(.stopSequences b ^java.util.List %)]
             [(.temperature p) #(.temperature b (double %))]
             [(.thinking p) #(.thinking b ^ThinkingConfigParam %)]
             [(.toolChoice p) #(.toolChoice b ^ToolChoice %)]
             [(.tools p) #(.tools b ^java.util.List %)]
             [(.topK p) #(.topK b (long %))]
             [(.topP p) #(.topP b (double %))]]]
      (when (.isPresent ^java.util.Optional value)
        (setter (.get ^java.util.Optional value))))
    (when (some? stream)
      (.stream b (boolean stream)))
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

(defn- deleted-batch->map [^DeletedMessageBatch d]
  {:id (.id d)
   :deleted true
   :type (->keyword (json->clj (._type d)))})

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
  "Get a batch by id. Returns `{:id :processing-status :request-counts
  :created-at :expires-at}` plus `:ended-at`/`:results-url` once available."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (batch->map (-> (.messages client) (.batches) (.retrieve id)))))

(defn- ->batch-list-params ^BatchListParams
  [{:keys [after-id before-id limit]}]
  (let [b (BatchListParams/builder)]
    (when after-id (.afterId b ^String after-id))
    (when before-id (.beforeId b ^String before-id))
    (when limit (.limit b (long limit)))
    (.build b)))

(defn list-batches
  "List all batches (pages followed) as a seq of maps like `get-batch`.
  Optional opts: `:after-id`, `:before-id`, and `:limit`."
  ([^AnthropicClient client]
   (list-batches client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^BatchListPage p (-> (.messages client) (.batches)
                                (.list (->batch-list-params opts)))]
       (mapv batch->map (.autoPager p))))))

(defn cancel-batch
  "Request cancellation of a batch; returns the updated batch map."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (batch->map (-> (.messages client) (.batches) (.cancel id)))))

(defn delete-batch
  "Delete a batch by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^DeletedMessageBatch d (-> (.messages client) (.batches) (.delete id))]
      (deleted-batch->map d))))

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
  per `:index` (the matching `:content-block-start` carries the tool `:id`, `:name`,
  and `:caller`)."
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
          (parsed-response resp req {}))))))

(defn stream-text
  "Stream a Messages request, calling `on-text` with each text delta (a string)
  as it arrives, and returning the full concatenated text when the stream ends.
  Takes the same `req` map as `create-message`. It wraps `stream` and ignores
  every non-text event. Use `stream` when you need thinking or
  tool-use deltas. The underlying HTTP stream is closed automatically."
  ^String [^AnthropicClient client req on-text]
  (stream client req
          (fn [m] (when (and on-text (= :text-delta (:type m))) (on-text (:text m))))))

(defn stream-handle
  "Start a Messages SSE stream on a worker thread and return a cancellable
  handle. `on-event` receives normalized events; pass `:buffer-size` in `opts`
  to make the handle a bounded pull stream instead."
  ([^AnthropicClient client req on-event]
   (stream-handle client req on-event {}))
  ([^AnthropicClient client req on-event opts]
   (stream-control/start!
    #(.createStreaming (.messages client) (->params req))
    on-event
    (assoc (or opts {}) :map-event event->map))))

(defn stream-queue
  "Start a bounded pull stream of normalized Messages events. Call
  `anthropic.stream/take-stream-event` to consume events and
  `anthropic.stream/cancel-stream!` when abandoning the handle."
  ([^AnthropicClient client req] (stream-queue client req {}))
  ([^AnthropicClient client req opts]
   (stream-handle client req nil
                  (assoc (or opts {}) :buffer-size (or (:buffer-size opts) 64)))))

(def stream-message-handle stream-handle)
(def stream-message-queue stream-queue)

(def cancel-stream! stream-control/cancel-stream!)
(def close-stream! stream-control/close-stream!)
(def take-stream-event stream-control/take-stream-event)
(def await-stream stream-control/await-stream)

;; ---- Files ----------------------------------------------------------------

(defn- file->map [^FileMetadata f]
  (let [downloadable (.downloadable f)
        expires-at (.expiresAt f)]
    (cond-> {:id (.id f)
             :filename (.filename f)
             :mime-type (.mimeType f)
             :size-bytes (.sizeBytes f)
             :created-at (str (.createdAt f))}
      (.isPresent downloadable) (assoc :downloadable (.get downloadable))
      (.isPresent expires-at) (assoc :expires-at (str (.get expires-at))))))

(defn- deleted-file->map [^DeletedFile d]
  (let [t (.type d)]
    (cond-> {:id (.id d) :deleted true}
      (.isPresent t) (assoc :type (->keyword
                                   (.asString ^DeletedFile$Type
                                              (.get t)))))))

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
  (see `get-file`), including `:expires-at` when reported."
  [^AnthropicClient client file]
  (with-api-errors
    (file->map (-> (.files client) (.upload (->upload-params file))))))

(defn get-file
  "Get a file's metadata by id: `{:id :filename :mime-type :size-bytes
  :created-at}` plus `:downloadable` and `:expires-at` when reported."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (file->map (-> (.files client) (.retrieveMetadata id)))))

(defn- ->file-list-params ^FileListParams
  [{:keys [ids page limit]}]
  (let [b (FileListParams/builder)]
    (when ids (.ids b ^java.util.List (mapv str ids)))
    (when page (.page b ^String page))
    (when limit (.limit b (long limit)))
    (.build b)))

(defn list-files
  "List uploaded files (pages followed) as a seq of maps like `get-file`,
  including `:expires-at` when reported. Optional opts: `:ids`, `:page`,
  and `:limit`."
  ([^AnthropicClient client]
   (list-files client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^FileListPage p (-> (.files client)
                               (.list (->file-list-params opts)))]
       (mapv file->map (.autoPager p))))))

(defn delete-file
  "Delete a file by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String id]
  (with-api-errors
    (let [^DeletedFile d (-> (.files client) (.delete id))]
      (deleted-file->map d))))

(defn download-file
  "Download a file's contents by id, returning a byte array. The HTTP response
  is closed automatically."
  ^bytes [^AnthropicClient client ^String id]
  (with-api-errors
    (with-open [^HttpResponse r (-> (.files client) (.download id))]
      (.readAllBytes (.body r)))))

(defn list-models-lazy
  "Lazily list the available models; accepts the same options as `list-models`."
  ([^AnthropicClient client] (list-models-lazy client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^ModelListPage p (-> (.models client) (.list (->model-list-params opts)))]
       (pagination/->lazy-pager model->map (.autoPager p))))))

(defn list-batches-lazy
  "Lazily list message batches; accepts the same options as `list-batches`."
  ([^AnthropicClient client] (list-batches-lazy client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
         (let [^BatchListPage p (-> (.messages client)
                                (.batches)
                                (.list (->batch-list-params opts)))]
       (pagination/->lazy-pager batch->map (.autoPager p))))))

(defn list-files-lazy
  "Lazily list uploaded files; accepts the same options as `list-files`."
  ([^AnthropicClient client] (list-files-lazy client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^FileListPage p (-> (.files client) (.list (->file-list-params opts)))]
       (pagination/->lazy-pager file->map (.autoPager p))))))

;; ---- Skills ----------------------------------------------------------------

(defn- ->skill-file ^MultipartField [f]
  (let [^java.io.File file (if (string? f) (java.io.File. ^String f) f)]
    (-> (MultipartField/builder)
        (.value (java.io.FileInputStream. file))
        (.filename (.getName file))
        (.build))))

(defn- ->skill-create-params ^SkillCreateParams
  [{:keys [display-name files]}]
  (when-not (seq files) (missing-key! :files))
  (let [b (SkillCreateParams/builder)]
    (when display-name (.displayName b ^String display-name))
    (doseq [f files] (.addFile b (->skill-file f)))
    (.build b)))

(defn- ->skill-retrieve-params ^SkillRetrieveParams [skill-id]
  (let [b (SkillRetrieveParams/builder)]
    (.skillId b ^String skill-id)
    (.build b)))

(defn- ->skill-list-params ^SkillListParams
  [{:keys [limit page source]}]
  (let [b (SkillListParams/builder)]
    (when limit (.limit b (long limit)))
    (when page (.page b ^String page))
    (when source (.source b ^String source))
    (.build b)))

(defn- skill->map [^Skill r]
  {:id (.id r)
   :display-name (.displayName r)
   :latest-version-id (.latestVersionId r)
   :source {:type (->keyword (.asString (.type ^SkillSource (.source r))))}
   :created-at (str (.createdAt r))
   :updated-at (str (.updatedAt r))})

(defn- skill-delete->map [^DeletedSkill r]
  {:id (.id r) :deleted true})

(defn create-skill
  "Create a skill from `:files` with an optional `:display-name`."
  [^AnthropicClient client req]
  (with-api-errors
    (skill->map (-> (.skills client) (.create (->skill-create-params req))))))

(defn get-skill
  "Get one skill by id."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (skill->map (-> (.skills client)
                    (.retrieve (->skill-retrieve-params skill-id))))))

(defn list-skills
  "List skills with optional `:limit`, `:page`, and `:source`."
  ([^AnthropicClient client] (list-skills client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^SkillListPage p (-> (.skills client)
                                (.list (->skill-list-params opts)))]
       (mapv skill->map (.autoPager p))))))

(defn delete-skill
  "Delete a skill by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (skill-delete->map (-> (.skills client) (.delete skill-id)))))

(defn- ->skill-version-create-params ^VersionCreateParams
  [skill-id {:keys [files]}]
  (when-not (seq files) (missing-key! :files))
  (let [b (VersionCreateParams/builder)]
    (.skillId b ^String skill-id)
    (doseq [f files] (.addFile b (->skill-file f)))
    (.build b)))

(defn- ->skill-version-retrieve-params ^VersionRetrieveParams
  [skill-id version]
  (let [b (VersionRetrieveParams/builder)]
    (.skillId b ^String skill-id)
    (.version b ^String version)
    (.build b)))

(defn- ->skill-version-list-params ^VersionListParams
  [skill-id {:keys [limit page]}]
  (let [b (VersionListParams/builder)]
    (.skillId b ^String skill-id)
    (when limit (.limit b (long limit)))
    (when page (.page b ^String page))
    (.build b)))

(defn- ->skill-version-delete-params ^VersionDeleteParams
  [skill-id version]
  (let [b (VersionDeleteParams/builder)]
    (.skillId b ^String skill-id)
    (.version b ^String version)
    (.build b)))

(defn- skill-version->map [^SkillVersion r]
  {:id (.id r)
   :skill-id (.skillId r)
   :name (.name r)
   :description (.description r)
   :created-at (str (.createdAt r))})

(defn- skill-version-delete->map [^DeletedSkillVersion r]
  {:id (.id r) :deleted true})

(defn create-skill-version
  "Create a new skill version for `skill-id` from `:files`."
  [^AnthropicClient client ^String skill-id req]
  (with-api-errors
    (skill-version->map (-> (.skills client) (.versions)
                            (.create (->skill-version-create-params skill-id req))))))

(defn get-skill-version
  "Get one skill version."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (skill-version->map (-> (.skills client) (.versions)
                            (.retrieve (->skill-version-retrieve-params skill-id version))))))

(defn list-skill-versions
  "List skill versions with optional `:limit` and `:page`."
  ([^AnthropicClient client ^String skill-id]
   (list-skill-versions client skill-id {}))
  ([^AnthropicClient client ^String skill-id opts]
   (with-api-errors
     (let [^VersionListPage p (-> (.skills client) (.versions)
                                  (.list (->skill-version-list-params skill-id opts)))]
       (mapv skill-version->map (.autoPager p))))))

(defn delete-skill-version
  "Delete a skill version. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (skill-version-delete->map (-> (.skills client) (.versions)
                                   (.delete (->skill-version-delete-params skill-id version))))))

(defn list-skills-lazy
  "Lazily list skills; accepts the same options as `list-skills`."
  ([^AnthropicClient client] (list-skills-lazy client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^SkillListPage p (-> (.skills client)
                                (.list (->skill-list-params opts)))]
       (pagination/->lazy-pager skill->map (.autoPager p))))))

(defn list-skill-versions-lazy
  "Lazily list skill versions; accepts the same options as `list-skill-versions`."
  ([^AnthropicClient client ^String skill-id]
   (list-skill-versions-lazy client skill-id {}))
  ([^AnthropicClient client ^String skill-id opts]
   (with-api-errors
     (let [^VersionListPage p (-> (.skills client) (.versions)
                                  (.list (->skill-version-list-params skill-id opts)))]
       (pagination/->lazy-pager skill-version->map (.autoPager p))))))
