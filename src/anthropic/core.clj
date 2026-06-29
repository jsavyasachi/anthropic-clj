(ns anthropic.core
  "Idiomatic Clojure wrapper over the official Anthropic Java SDK
  (`com.anthropic/anthropic-java`).

  Build a request as a Clojure map, get a Clojure map back. The client reads
  `ANTHROPIC_API_KEY` from the environment by default."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [jsonista.core :as json])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.client.okhttp AnthropicOkHttpClient)
           (com.anthropic.core JsonValue)
           (com.anthropic.core.http HttpResponse StreamResponse)
           (com.anthropic.models.beta.files DeletedFile FileListPage FileMetadata
                                            FileUploadParams)
           (com.anthropic.models.models ModelInfo ModelListPage)
           (com.anthropic.models.messages.batches BatchCreateParams
                                                  BatchCreateParams$Request
                                                  BatchCreateParams$Request$Params
                                                  BatchCreateParams$Request$Params$Builder
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
                                          CitationsSearchResultLocation
                                          CitationsWebSearchResultLocation
                                          CodeExecutionTool20260521
                                          CodeExecutionTool20260521$AllowedCaller
                                          ContentBlock ContentBlockParam
                                          DocumentBlockParam DocumentBlockParam$Source
                                          ImageBlockParam ImageBlockParam$Source
                                          JsonOutputFormat JsonOutputFormat$Schema
                                          Message
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
                                          TextBlock TextBlockParam TextCitation
                                          TextDelta
                                          ThinkingBlock ThinkingConfigAdaptive
                                          ThinkingConfigDisabled
                                          ThinkingConfigEnabled
                                          ThinkingConfigParam ThinkingDelta
                                          Tool Tool$InputSchema ToolBash20250124
                                          ToolChoice
                                          ToolChoiceAny ToolChoiceAuto
                                          ToolChoiceNone ToolChoiceTool
                                          ServerToolUseBlock
                                          ToolResultBlockParam ToolTextEditor20250728
                                          ToolUnion ToolUseBlock
                                          ToolUseBlockParam
                                          MemoryTool20250818
                                          PlainTextSource UrlImageSource UrlPdfSource
                                          UserLocation
                                          WebSearchTool20260318
                                          WebSearchTool20260318$AllowedCaller
                                          WebFetchTool20260318
                                          WebFetchTool20260318$AllowedCaller
                                          Usage)))

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

(defn- ->custom-tool ^Tool [{:keys [name description input-schema]}]
  (let [required (:required input-schema)
        isb (Tool$InputSchema/builder)
        tb (Tool/builder)]
    (.properties isb (->json (:properties input-schema)))
    (when (seq required) (.required isb ^java.util.List (vec required)))
    (.name tb ^String name)
    (.inputSchema tb (.build isb))
    (when description (.description tb ^String description))
    (.build tb)))

(defn- ->user-location ^UserLocation [{:keys [city region country timezone]}]
  (let [b (UserLocation/builder)]
    (when city (.city b ^String city))
    (when region (.region b ^String region))
    (when country (.country b ^String country))
    (when timezone (.timezone b ^String timezone))
    (.build b)))

(def ^:private server-tool-types
  #{:web-search :web-fetch :code-execution :bash :text-editor :memory})

(defn- ->server-tool
  "Map a server-side tool spec (latest version of each) to a ToolUnion."
  ^ToolUnion [{:keys [type max-uses allowed-domains blocked-domains user-location
                      max-content-tokens max-characters allowed-callers]}]
  (case (keyword type)
    :web-search (ToolUnion/ofWebSearchTool20260318
                 (let [b (WebSearchTool20260318/builder)]
                   (when max-uses (.maxUses b (long max-uses)))
                   (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
                   (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
                   (when user-location (.userLocation b (->user-location user-location)))
                   (doseq [c allowed-callers]
                     (.addAllowedCaller b (WebSearchTool20260318$AllowedCaller/of (name c))))
                   (.build b)))
    :web-fetch (ToolUnion/ofWebFetchTool20260318
                (let [b (WebFetchTool20260318/builder)]
                  (when max-uses (.maxUses b (long max-uses)))
                  (when max-content-tokens (.maxContentTokens b (long max-content-tokens)))
                  (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
                  (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
                  (doseq [c allowed-callers]
                    (.addAllowedCaller b (WebFetchTool20260318$AllowedCaller/of (name c))))
                  (.build b)))
    :code-execution (ToolUnion/ofCodeExecutionTool20260521
                     (let [b (CodeExecutionTool20260521/builder)]
                       (doseq [c allowed-callers]
                         (.addAllowedCaller b (CodeExecutionTool20260521$AllowedCaller/of (name c))))
                       (.build b)))
    :bash (ToolUnion/ofBash20250124 (.build (ToolBash20250124/builder)))
    :text-editor (ToolUnion/ofTextEditor20250728
                  (let [b (ToolTextEditor20250728/builder)]
                    (when max-characters (.maxCharacters b (long max-characters)))
                    (.build b)))
    :memory (ToolUnion/ofMemoryTool20250818 (.build (MemoryTool20250818/builder)))))

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
          (-> (UrlImageSource/builder) (.url ^String url) (.build)))))

(defn- ->document-source ^DocumentBlockParam$Source [{:keys [type data url]}]
  (case (keyword type)
    :base64 (DocumentBlockParam$Source/ofBase64
             (-> (Base64PdfSource/builder) (.data ^String data) (.build)))
    :url (DocumentBlockParam$Source/ofUrl
          (-> (UrlPdfSource/builder) (.url ^String url) (.build)))
    :text (DocumentBlockParam$Source/ofText
           (-> (PlainTextSource/builder) (.data ^String data) (.build)))))

(defn- ->content-block ^ContentBlockParam [{:keys [type cache-control] :as blk}]
  (case (keyword type)
    :text (let [b (-> (TextBlockParam/builder) (.text ^String (:text blk)))]
            (when cache-control (.cacheControl b (->cache-control cache-control)))
            (ContentBlockParam/ofText (.build b)))
    :image (let [b (-> (ImageBlockParam/builder)
                       (.source ^ImageBlockParam$Source (->image-source (:source blk))))]
             (when cache-control (.cacheControl b (->cache-control cache-control)))
             (ContentBlockParam/ofImage (.build b)))
    :document (let [b (-> (DocumentBlockParam/builder)
                          (.source ^DocumentBlockParam$Source (->document-source (:source blk))))]
                (when (:title blk) (.title b ^String (:title blk)))
                (when (:context blk) (.context b ^String (:context blk)))
                (when cache-control (.cacheControl b (->cache-control cache-control)))
                (ContentBlockParam/ofDocument (.build b)))
    :tool-result (let [b (-> (ToolResultBlockParam/builder)
                             (.toolUseId ^String (:tool-use-id blk))
                             (.content ^String (str (:content blk))))]
                   (when cache-control (.cacheControl b (->cache-control cache-control)))
                   (ContentBlockParam/ofToolResult (.build b)))
    :tool-use (let [b (-> (ToolUseBlockParam/builder)
                          (.id ^String (:id blk))
                          (.name ^String (:name blk))
                          (.input (->json (:input blk))))]
                (when cache-control (.cacheControl b (->cache-control cache-control)))
                (ContentBlockParam/ofToolUse (.build b)))))

(defn- ->thinking ^ThinkingConfigParam [{:keys [type budget-tokens]}]
  (case (keyword type)
    :enabled (ThinkingConfigParam/ofEnabled
              (-> (ThinkingConfigEnabled/builder)
                  (.budgetTokens (long budget-tokens)) (.build)))
    :disabled (ThinkingConfigParam/ofDisabled (.build (ThinkingConfigDisabled/builder)))
    :adaptive (ThinkingConfigParam/ofAdaptive (.build (ThinkingConfigAdaptive/builder)))))

(defn- ->tool-choice ^ToolChoice [tc]
  (if (map? tc)
    (ToolChoice/ofTool (-> (ToolChoiceTool/builder) (.name ^String (:name tc)) (.build)))
    (case (keyword tc)
      :auto (ToolChoice/ofAuto (.build (ToolChoiceAuto/builder)))
      :any (ToolChoice/ofAny (.build (ToolChoiceAny/builder)))
      :none (ToolChoice/ofNone (.build (ToolChoiceNone/builder))))))

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
                                response-format effort]
                         :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (MessageCreateParams/builder)
            (.model (Model/of model))
            (.maxTokens (long max-tokens)))]
    (when system (.system b ^String system))
    (when temperature (.temperature b (double temperature)))
    (when top-p (.topP b (double top-p)))
    (when top-k (.topK b (long top-k)))
    (when (seq stop-sequences) (.stopSequences b ^java.util.List (vec stop-sequences)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when thinking (.thinking b (->thinking thinking)))
    (when metadata (.metadata b (->metadata metadata)))
    (when service-tier (.serviceTier b (->service-tier service-tier)))
    (when (or response-format effort)
      (.outputConfig b (->output-config response-format effort)))
    (doseq [t tools] (.addTool b (->tool t)))
    (doseq [m messages] (add-message b m))
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
  ^MessageCountTokensParams [{:keys [model system messages tools thinking tool-choice]
                              :or {model "claude-opus-4-8"}}]
  (let [b (doto (MessageCountTokensParams/builder)
            (.model (Model/of model)))]
    (when system (.system b ^String system))
    (when thinking (.thinking b (->thinking thinking)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    ;; count-tokens uses a distinct tool union; support custom tools only here.
    (doseq [t tools :when (not (server-tool? t))] (.addTool b (->custom-tool t)))
    (doseq [m messages] (add-count-message b m))
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
      (.isPresent th) {:type :thinking :thinking (.thinking ^ThinkingBlock (.get th))}
      (.isPresent stu) (let [x ^ServerToolUseBlock (.get stu)]
                         {:type :server-tool-use
                          :id (.id x)
                          :name (str (.name x))
                          :input (json->clj (._input x))})
      (.isPresent (.webSearchToolResult b)) (block-raw b :web-search-result)
      (.isPresent (.webFetchToolResult b)) (block-raw b :web-fetch-result)
      (.isPresent (.codeExecutionToolResult b)) (block-raw b :code-execution-result)
      (.isPresent (.bashCodeExecutionToolResult b)) (block-raw b :bash-code-execution-result)
      (.isPresent (.textEditorCodeExecutionToolResult b)) (block-raw b :text-editor-code-execution-result)
      (.isPresent (.toolSearchToolResult b)) (block-raw b :tool-search-result)
      (.isPresent (.containerUpload b)) (block-raw b :container-upload)
      (.isPresent (.redactedThinking b)) {:type :redacted-thinking}
      :else {:type :other})))

(defn- usage->map [^Usage u]
  (let [cc (.cacheCreationInputTokens u)
        cr (.cacheReadInputTokens u)]
    (cond-> {:input-tokens (.inputTokens u)
             :output-tokens (.outputTokens u)}
      (.isPresent cc) (assoc :cache-creation-input-tokens (.get cc))
      (.isPresent cr) (assoc :cache-read-input-tokens (.get cr)))))

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

(defn- parse-text
  "Decode the first text block of a response map as JSON (keyword keys), or nil."
  [resp]
  (when-let [t (->> (:content resp) (filter #(= :text (:type %))) first :text)]
    (json/read-value t json-mapper)))

(defn create-message
  "Send a Messages request and return the response as a Clojure map.

  `req` keys: `:model` (string, defaults to \"claude-opus-4-8\"), `:max-tokens`
  (defaults to 1024), `:system` (string), `:messages` (a seq of
  `{:role :user|:assistant :content \"...\"}`), `:tools`, and the optional
  controls `:temperature`, `:top-p`, `:top-k`, `:stop-sequences` (seq of
  strings), `:tool-choice` (`:auto`/`:any`/`:none` or `{:type :tool :name \"x\"}`),
  `:thinking` (`{:type :enabled :budget-tokens N}` / `{:type :adaptive}` /
  `{:type :disabled}`), `:metadata` (`{:user-id \"...\"}`), and `:service-tier`
  (`:auto`/`:standard-only`). For structured output, pass `:response-format` (a
  JSON Schema map) and/or `:effort` (`:low`…`:max`); when `:response-format` is
  set the returned map also carries `:parsed`, the response text decoded as a
  Clojure map. Returns
  `{:id :model :role :stop-reason :content [...] :usage {...}}`."
  [^AnthropicClient client req]
  (let [resp (-> (.messages client)
                 (.create (->params req))
                 (message->map))]
    (cond-> resp
      (:response-format req) (assoc :parsed (parse-text resp)))))

(defn count-tokens
  "Count the input tokens a request would use, without sending it. Takes the same
  `req` map as `create-message` (sampling params and `:max-tokens` are ignored).
  Returns `{:input-tokens n}`."
  [^AnthropicClient client req]
  (let [^MessageTokensCount r (-> (.messages client) (.countTokens (->count-params req)))]
    {:input-tokens (.inputTokens r)}))

(defn- model->map [^ModelInfo m]
  (let [mit (.maxInputTokens m)
        mt (.maxTokens m)]
    (cond-> {:id (.id m)
             :display-name (.displayName m)
             :created-at (str (.createdAt m))}
      (.isPresent mit) (assoc :max-input-tokens (.get mit))
      (.isPresent mt) (assoc :max-tokens (.get mt)))))

(defn list-models
  "List the available models as a seq of maps, newest first. Each map has `:id`,
  `:display-name`, `:created-at` (ISO-8601 string), and `:max-input-tokens` /
  `:max-tokens` when the API reports them. Pages are followed automatically."
  [^AnthropicClient client]
  (let [^ModelListPage p (-> (.models client) (.list))]
    (mapv model->map (.autoPager p))))

(defn get-model
  "Retrieve one model's info by id, as a map shaped like `list-models`' entries."
  [^AnthropicClient client ^String id]
  (model->map (-> (.models client) (.retrieve id))))

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
  "Translate a per-request map into a batch Request.Params. Same keys as
  `->params` except `:service-tier` (not supported per batch request)."
  ^BatchCreateParams$Request$Params
  [{:keys [model max-tokens system messages tools temperature top-p top-k
           stop-sequences tool-choice thinking metadata response-format effort]
    :or {model "claude-opus-4-8" max-tokens 1024}}]
  (let [b (doto (BatchCreateParams$Request$Params/builder)
            (.model (Model/of model))
            (.maxTokens (long max-tokens)))]
    (when system (.system b ^String system))
    (when temperature (.temperature b (double temperature)))
    (when top-p (.topP b (double top-p)))
    (when top-k (.topK b (long top-k)))
    (when (seq stop-sequences) (.stopSequences b ^java.util.List (vec stop-sequences)))
    (when tool-choice (.toolChoice b (->tool-choice tool-choice)))
    (when thinking (.thinking b (->thinking thinking)))
    (when metadata (.metadata b (->metadata metadata)))
    (when (or response-format effort)
      (.outputConfig b (->output-config response-format effort)))
    (doseq [t tools] (.addTool b (->tool t)))
    (doseq [m messages] (add-batch-message b m))
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
  `{:custom-id \"...\" :params <same map as create-message>}` (`:params` ignores
  `:service-tier`). Returns the batch as a map (see `get-batch`)."
  [^AnthropicClient client requests]
  (let [reqs ^java.util.List (mapv ->batch-request requests)
        bp (-> (BatchCreateParams/builder) (.requests reqs) (.build))]
    (batch->map (-> (.messages client) (.batches) (.create bp)))))

(defn get-batch
  "Retrieve a batch by id. Returns `{:id :processing-status :request-counts
  :created-at :expires-at}` plus `:ended-at`/`:results-url` once available."
  [^AnthropicClient client ^String id]
  (batch->map (-> (.messages client) (.batches) (.retrieve id))))

(defn list-batches
  "List all batches (pages followed) as a seq of maps like `get-batch`."
  [^AnthropicClient client]
  (let [^BatchListPage p (-> (.messages client) (.batches) (.list))]
    (mapv batch->map (.autoPager p))))

(defn cancel-batch
  "Request cancellation of a batch; returns the updated batch map."
  [^AnthropicClient client ^String id]
  (batch->map (-> (.messages client) (.batches) (.cancel id))))

(defn delete-batch
  "Delete a batch by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String id]
  (let [^DeletedMessageBatch d (-> (.messages client) (.batches) (.delete id))]
    {:id (.id d) :deleted true}))

(defn batch-results
  "Fetch a completed batch's results as a vector of
  `{:custom-id ... :result {:type :succeeded|:errored|:canceled|:expired ...}}`;
  succeeded results include the parsed `:message`. The results stream is closed
  automatically."
  [^AnthropicClient client ^String id]
  (with-open [^StreamResponse sr (-> (.messages client) (.batches) (.resultsStreaming id))]
    (mapv batch-result->map (iterator-seq (.iterator (.stream sr))))))

(defn- start-block->map [^RawContentBlockStartEvent$ContentBlock cb]
  (let [tu (.toolUse cb)
        th (.thinking cb)
        tx (.text cb)]
    (cond
      (.isPresent tu) (let [x ^ToolUseBlock (.get tu)]
                        {:type :tool-use :id (.id x) :name (.name x)})
      (.isPresent th) {:type :thinking}
      (.isPresent tx) {:type :text}
      :else {:type :other})))

(defn- delta->map [index ^RawContentBlockDelta d]
  (let [t (.text d) ij (.inputJson d) th (.thinking d) sg (.signature d)]
    (cond
      (.isPresent t) {:type :text-delta :index index :text (.text ^TextDelta (.get t))}
      (.isPresent ij) {:type :input-json-delta :index index
                       :partial-json (.partialJson ^InputJsonDelta (.get ij))}
      (.isPresent th) {:type :thinking-delta :index index
                       :thinking (.thinking ^ThinkingDelta (.get th))}
      (.isPresent sg) {:type :signature-delta :index index}
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
      (.isPresent md) (let [sr (.stopReason (.delta ^RawMessageDeltaEvent (.get md)))]
                        {:type :message-delta
                         :stop-reason (when (.isPresent sr) (->keyword (.get sr)))})
      (.isPresent (.messageStart ev)) {:type :message-start}
      (.isPresent (.messageStop ev)) {:type :message-stop}
      :else {:type :other})))

(defn stream
  "Stream a Messages request, invoking `on-event` with a normalized event map for
  every server-sent event as it arrives, and returning the full concatenated
  assistant text when the stream ends. Takes the same `req` map as
  `create-message`. The underlying HTTP stream is closed automatically.

  Event maps are keyed by `:type`: `:message-start`, `:content-block-start`
  (`:index`, `:block`), `:text-delta`/`:thinking-delta`/`:input-json-delta`/
  `:signature-delta` (`:index` plus the payload), `:content-block-stop`
  (`:index`), `:message-delta` (`:stop-reason`), and `:message-stop`. To
  reconstruct a streamed tool call, accumulate `:input-json-delta` `:partial-json`
  per `:index` (the matching `:content-block-start` carries the tool `:id`/`:name`)."
  ^String [^AnthropicClient client req on-event]
  (with-open [^StreamResponse sr (.createStreaming (.messages client) (->params req))]
    (let [sb (StringBuilder.)]
      (doseq [ev (iterator-seq (.iterator (.stream sr)))]
        (let [m (event->map ev)]
          (when (= :text-delta (:type m)) (.append sb ^String (:text m)))
          (when on-event (on-event m))))
      (str sb))))

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
  (file->map (-> (.beta client) (.files) (.upload (->upload-params file)))))

(defn get-file
  "Retrieve a file's metadata by id: `{:id :filename :mime-type :size-bytes
  :created-at}` plus `:downloadable` when reported."
  [^AnthropicClient client ^String id]
  (file->map (-> (.beta client) (.files) (.retrieveMetadata id))))

(defn list-files
  "List uploaded files (pages followed) as a seq of maps like `get-file`."
  [^AnthropicClient client]
  (let [^FileListPage p (-> (.beta client) (.files) (.list))]
    (mapv file->map (.autoPager p))))

(defn delete-file
  "Delete a file by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String id]
  (let [^DeletedFile d (-> (.beta client) (.files) (.delete id))]
    {:id (.id d) :deleted true}))

(defn download-file
  "Download a file's contents by id, returning a byte array. The HTTP response
  is closed automatically."
  ^bytes [^AnthropicClient client ^String id]
  (with-open [^HttpResponse r (-> (.beta client) (.files) (.download id))]
    (.readAllBytes (.body r))))
