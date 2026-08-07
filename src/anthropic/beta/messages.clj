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
                                               BetaContextManagementConfig
                                               BetaContextManagementConfig$Edit
                                               BetaClearThinking20251015Edit
                                               BetaClearToolUses20250919Edit
                                               BetaCompact20260112Edit
                                               BetaDiagnosticsParam
                                               BetaImageBlockParam
                                               BetaImageBlockParam$Source
                                               BetaJsonOutputFormat
                                               BetaJsonOutputFormat$Schema
                                               BetaMessage BetaMessageTokensCount
                                               BetaMetadata BetaOutputConfig
                                               BetaOutputConfig$Effort
                                               BetaFallbackParam
                                               BetaFallbackParam$Speed
                                               BetaPlainTextSource
                                               BetaRedactedThinkingBlockParam
                                               BetaRequestDocumentBlock
                                               BetaRequestDocumentBlock$Source
                                               BetaRequestToolAdditionBlock
                                               BetaRequestToolAdditionBlock$Builder
                                               BetaRequestToolRemovalBlock
                                               BetaRequestToolRemovalBlock$Builder
                                               BetaRequestMcpServerToolConfiguration
                                               BetaRequestMcpServerUrlDefinition
                                               BetaTextBlockParam
                                               BetaTokenTaskBudget
                                               BetaThinkingBlockParam
                                               BetaThinkingConfigAdaptive
                                               BetaThinkingConfigDisabled
                                               BetaThinkingConfigEnabled
                                               BetaThinkingConfigParam
                                               BetaTool BetaTool$Builder BetaTool$AllowedCaller BetaTool$InputSchema
                                               BetaTool$InputSchema$Properties
                                               BetaToolUnion
                                               BetaWebSearchTool20260318
                                               BetaWebFetchTool20260318
                                               BetaCodeExecutionTool20260521
                                               BetaToolBash20250124
                                               BetaToolTextEditor20250728
                                               BetaMemoryTool20250818
                                               BetaToolBash20250124$InputExample$Builder
                                               BetaToolTextEditor20250728$InputExample$Builder
                                               BetaMemoryTool20250818$InputExample$Builder
                                               BetaToolSearchToolBm25_20251119$AllowedCaller
                                               BetaToolSearchToolBm25_20251119$Builder
                                               BetaToolSearchToolBm25_20251119$Type
                                               BetaToolSearchToolRegex20251119$AllowedCaller
                                               BetaToolSearchToolRegex20251119$Builder
                                               BetaToolSearchToolRegex20251119$Type
                                               BetaToolSearchToolBm25_20251119
                                               BetaToolSearchToolRegex20251119
                                               BetaToolComputerUse20251124
                                               BetaToolComputerUse20251124$AllowedCaller
                                               BetaToolComputerUse20251124$Builder
                                               BetaToolComputerUse20251124$InputExample$Builder
                                               BetaAdvisorTool20260301
                                               BetaAdvisorTool20260301$AllowedCaller
                                               BetaAdvisorTool20260301$Builder
                                               BetaMcpToolset BetaMcpToolset$Configs
                                               BetaMcpToolDefaultConfig
                                               BetaCitationsConfigParam BetaUserLocation
                                               BetaWebSearchTool20260318$AllowedCaller
                                               BetaWebSearchTool20260318$Builder
                                               BetaWebSearchTool20260318$ResponseInclusion
                                               BetaWebFetchTool20260318$AllowedCaller
                                               BetaWebFetchTool20260318$Builder
                                               BetaWebFetchTool20260318$ResponseInclusion
                                               BetaCodeExecutionTool20260521$AllowedCaller
                                               BetaCodeExecutionTool20260521$Builder
                                               BetaToolBash20250124$AllowedCaller
                                               BetaToolBash20250124$Builder
                                               BetaToolTextEditor20250728$AllowedCaller
                                               BetaToolTextEditor20250728$Builder
                                               BetaMemoryTool20250818$AllowedCaller
                                               BetaMemoryTool20250818$Builder
                                               BetaTool$InputExample$Builder
                                               BetaToolChangeMcpToolReference
                                               BetaToolChangeMcpToolsetReference
                                               BetaToolChoice BetaToolChoiceAny
                                               BetaToolChoiceAuto BetaToolChoiceNone
                                               BetaToolChoiceTool
                                               BetaToolResultBlockParam
                                               BetaToolUseBlockParam
                                               BetaToolUseBlockParam$Input
                                               BetaUrlImageSource BetaUrlPdfSource
                                               MessageCountTokensParams
                                               MessageCountTokensParams$Tool
                                               MessageCountTokensParams$Builder
                                               MessageCreateParams
                                               MessageCreateParams$Builder
                                               MessageCreateParams$ServiceTier
                                               MessageCreateParams$Speed
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

(defn- add-tool-change
  [^BetaRequestToolAdditionBlock$Builder b {:keys [reference mcp-tool-reference mcp-toolset-reference]}]
  (cond
    reference (.referenceTool b ^String reference)
    mcp-tool-reference
    (.tool b ^BetaToolChangeMcpToolReference
           (-> (BetaToolChangeMcpToolReference/builder)
               (.name ^String (:name mcp-tool-reference))
               (.serverName ^String (:server-name mcp-tool-reference))
               (.build)))
    mcp-toolset-reference (.mcpToolsetReferenceTool b ^String (:server-name mcp-toolset-reference))
    :else (throw (ex-info "Unsupported beta tool change reference"
                          {:anthropic/error :unsupported-tool-change-reference}))))

(defn- remove-tool-change
  [^BetaRequestToolRemovalBlock$Builder b {:keys [reference mcp-tool-reference mcp-toolset-reference]}]
  (cond
    reference (.referenceTool b ^String reference)
    mcp-tool-reference
    (.tool b ^BetaToolChangeMcpToolReference
           (-> (BetaToolChangeMcpToolReference/builder)
               (.name ^String (:name mcp-tool-reference))
               (.serverName ^String (:server-name mcp-tool-reference))
               (.build)))
    mcp-toolset-reference (.mcpToolsetReferenceTool b ^String (:server-name mcp-toolset-reference))
    :else (throw (ex-info "Unsupported beta tool change reference"
                          {:anthropic/error :unsupported-tool-change-reference}))))

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
    :tool-addition (let [^BetaRequestToolAdditionBlock$Builder b
                         (add-tool-change (BetaRequestToolAdditionBlock/builder)
                                          (:tool blk))]
                     (when cache-control (.cacheControl b (->cache-control cache-control)))
                     (BetaContentBlockParam/ofToolAddition (.build b)))
    :tool-removal (let [^BetaRequestToolRemovalBlock$Builder b
                        (remove-tool-change (BetaRequestToolRemovalBlock/builder)
                                            (:tool blk))]
                    (when cache-control (.cacheControl b (->cache-control cache-control)))
                    (BetaContentBlockParam/ofToolRemoval (.build b)))
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
    (case (keyword (:type tc))
      :auto (let [b (BetaToolChoiceAuto/builder)]
              (when (contains? tc :disable-parallel-tool-use)
                (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
              (BetaToolChoice/ofAuto (.build b)))
      :any (let [b (BetaToolChoiceAny/builder)]
             (when (contains? tc :disable-parallel-tool-use)
               (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
             (BetaToolChoice/ofAny (.build b)))
      :none (when (contains? tc :disable-parallel-tool-use)
              (throw (ex-info "Unsupported disable parallel tool use"
                              {:anthropic/error :unsupported-disable-parallel-tool-use})))
      (let [b (BetaToolChoiceTool/builder)]
        (.name b ^String (:name tc))
        (when (contains? tc :disable-parallel-tool-use)
          (.disableParallelToolUse b (boolean (:disable-parallel-tool-use tc))))
        (BetaToolChoice/ofTool (.build b))))
    (case (keyword tc)
      :auto (BetaToolChoice/ofAuto (.build (BetaToolChoiceAuto/builder)))
      :any (BetaToolChoice/ofAny (.build (BetaToolChoiceAny/builder)))
      :none (BetaToolChoice/ofNone (.build (BetaToolChoiceNone/builder)))
      (throw (ex-info "Unsupported tool choice"
                      {:anthropic/error :unsupported-tool-choice :tool-choice tc})))))

(defn- configure-tool-builder
  [{:keys [allowed-callers cache-control defer-loading strict]}
   {:keys [add-allowed-caller cache-control! defer-loading! strict!]}]
  (doseq [c allowed-callers]
    (add-allowed-caller c))
  (when cache-control (cache-control! (->cache-control cache-control)))
  (when (some? defer-loading) (defer-loading! defer-loading))
  (when (some? strict) (strict! strict)))

(defn- ->beta-bash-input-example [example]
  (let [b (BetaToolBash20250124$InputExample$Builder.)]
    (doseq [[k v] example] (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->beta-text-editor-input-example [example]
  (let [b (BetaToolTextEditor20250728$InputExample$Builder.)]
    (doseq [[k v] example] (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->beta-memory-input-example [example]
  (let [b (BetaMemoryTool20250818$InputExample$Builder.)]
    (doseq [[k v] example] (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->beta-computer-use-input-example [example]
  (let [b (BetaToolComputerUse20251124$InputExample$Builder.)]
    (doseq [[k v] example] (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->beta-custom-input-example [example]
  (let [b (BetaTool$InputExample$Builder.)]
    (doseq [[k v] example] (.putAdditionalProperty b ^String (name k) (->json v)))
    (.build b)))

(defn- ->custom-tool ^BetaTool [{:keys [name description input-schema] :as t}]
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
    (when (some? (:eager-input-streaming t))
      (.eagerInputStreaming b (boolean (:eager-input-streaming t))))
    (when (seq (:input-examples t))
      (.inputExamples b ^java.util.List (mapv ->beta-custom-input-example (:input-examples t))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaTool$Builder b
                                               (BetaTool$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaTool$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaTool$Builder b (boolean %))
      :strict! #(.strict ^BetaTool$Builder b (boolean %))})
    (.build b)))

(defn- ->user-location ^BetaUserLocation [{:keys [city region country timezone]}]
  (let [b (BetaUserLocation/builder)]
    (when city (.city b ^String city))
    (when region (.region b ^String region))
    (when country (.country b ^String country))
    (when timezone (.timezone b ^String timezone))
    (.build b)))

(def ^:private server-tool-types
  #{:web-search :web-fetch :code-execution :bash :text-editor :memory
    :tool-search :computer-use :advisor :mcp-toolset})

(defn- ->web-search-tool ^BetaWebSearchTool20260318
  [{:keys [max-uses allowed-domains blocked-domains user-location response-inclusion] :as t}]
  (let [b (BetaWebSearchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (when user-location (.userLocation b (->user-location user-location)))
    (when response-inclusion
      (.responseInclusion b (BetaWebSearchTool20260318$ResponseInclusion/of (name response-inclusion))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaWebSearchTool20260318$Builder b
                                               (BetaWebSearchTool20260318$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaWebSearchTool20260318$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaWebSearchTool20260318$Builder b (boolean %))
      :strict! #(.strict ^BetaWebSearchTool20260318$Builder b (boolean %))})
    (.build b)))

(defn- ->citations ^BetaCitationsConfigParam [citations]
  (let [b (BetaCitationsConfigParam/builder)]
    (.enabled b (boolean (if (map? citations) (:enabled citations) citations)))
    (.build b)))

(defn- ->web-fetch-tool ^BetaWebFetchTool20260318
  [{:keys [max-uses max-content-tokens allowed-domains blocked-domains use-cache citations response-inclusion] :as t}]
  (let [b (BetaWebFetchTool20260318/builder)]
    (when max-uses (.maxUses b (long max-uses)))
    (when max-content-tokens (.maxContentTokens b (long max-content-tokens)))
    (when (seq allowed-domains) (.allowedDomains b ^java.util.List (vec allowed-domains)))
    (when (seq blocked-domains) (.blockedDomains b ^java.util.List (vec blocked-domains)))
    (when (some? use-cache) (.useCache b (boolean use-cache)))
    (when citations (.citations b (->citations citations)))
    (when response-inclusion
      (.responseInclusion b (BetaWebFetchTool20260318$ResponseInclusion/of (name response-inclusion))))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaWebFetchTool20260318$Builder b
                                               (BetaWebFetchTool20260318$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaWebFetchTool20260318$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaWebFetchTool20260318$Builder b (boolean %))
      :strict! #(.strict ^BetaWebFetchTool20260318$Builder b (boolean %))})
    (.build b)))

(defn- ->code-execution-tool ^BetaCodeExecutionTool20260521 [t]
  (let [b (BetaCodeExecutionTool20260521/builder)]
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaCodeExecutionTool20260521$Builder b
                                               (BetaCodeExecutionTool20260521$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaCodeExecutionTool20260521$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaCodeExecutionTool20260521$Builder b (boolean %))
      :strict! #(.strict ^BetaCodeExecutionTool20260521$Builder b (boolean %))})
    (.build b)))

(defn- ->bash-tool ^BetaToolBash20250124 [{:keys [input-examples] :as t}]
  (let [b (BetaToolBash20250124/builder)]
    (when (seq input-examples)
      (.inputExamples b ^java.util.List (mapv ->beta-bash-input-example input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaToolBash20250124$Builder b
                                               (BetaToolBash20250124$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaToolBash20250124$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaToolBash20250124$Builder b (boolean %))
      :strict! #(.strict ^BetaToolBash20250124$Builder b (boolean %))})
    (.build b)))

(defn- ->text-editor-tool ^BetaToolTextEditor20250728
  [{:keys [max-characters input-examples] :as t}]
  (let [b (BetaToolTextEditor20250728/builder)]
    (when max-characters (.maxCharacters b (long max-characters)))
    (when (seq input-examples)
      (.inputExamples b ^java.util.List (mapv ->beta-text-editor-input-example input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaToolTextEditor20250728$Builder b
                                               (BetaToolTextEditor20250728$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaToolTextEditor20250728$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaToolTextEditor20250728$Builder b (boolean %))
      :strict! #(.strict ^BetaToolTextEditor20250728$Builder b (boolean %))})
    (.build b)))

(defn- ->memory-tool ^BetaMemoryTool20250818 [{:keys [input-examples] :as t}]
  (let [b (BetaMemoryTool20250818/builder)]
    (when (seq input-examples)
      (.inputExamples b ^java.util.List (mapv ->beta-memory-input-example input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaMemoryTool20250818$Builder b
                                               (BetaMemoryTool20250818$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaMemoryTool20250818$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaMemoryTool20250818$Builder b (boolean %))
      :strict! #(.strict ^BetaMemoryTool20250818$Builder b (boolean %))})
    (.build b)))

(defn- ->tool-search-bm25 ^BetaToolSearchToolBm25_20251119
  [{:keys [allowed-callers cache-control defer-loading strict]}]
  (let [b (BetaToolSearchToolBm25_20251119/builder)]
    (.type b BetaToolSearchToolBm25_20251119$Type/TOOL_SEARCH_TOOL_BM25_20251119)
    (doseq [c allowed-callers] (.addAllowedCaller b (BetaToolSearchToolBm25_20251119$AllowedCaller/of (clojure.core/name c))))
    (when cache-control (.cacheControl b ^BetaCacheControlEphemeral (->cache-control cache-control)))
    (when (some? defer-loading) (.deferLoading b (boolean defer-loading)))
    (when (some? strict) (.strict b (boolean strict)))
    (.build b)))

(defn- ->tool-search-regex ^BetaToolSearchToolRegex20251119
  [{:keys [allowed-callers cache-control defer-loading strict]}]
  (let [b (BetaToolSearchToolRegex20251119/builder)]
    (.type b BetaToolSearchToolRegex20251119$Type/TOOL_SEARCH_TOOL_REGEX_20251119)
    (doseq [c allowed-callers] (.addAllowedCaller b (BetaToolSearchToolRegex20251119$AllowedCaller/of (clojure.core/name c))))
    (when cache-control (.cacheControl b ^BetaCacheControlEphemeral (->cache-control cache-control)))
    (when (some? defer-loading) (.deferLoading b (boolean defer-loading)))
    (when (some? strict) (.strict b (boolean strict)))
    (.build b)))

(defn- ->computer-use-tool ^BetaToolComputerUse20251124
  [{:keys [display-height-px display-width-px display-number enable-zoom input-examples] :as t}]
  (let [b (BetaToolComputerUse20251124/builder)]
    (when display-height-px (.displayHeightPx b (long display-height-px)))
    (when display-width-px (.displayWidthPx b (long display-width-px)))
    (when display-number (.displayNumber b (long display-number)))
    (when (some? enable-zoom) (.enableZoom b (boolean enable-zoom)))
    (when (seq input-examples)
      (.inputExamples b ^java.util.List (mapv ->beta-computer-use-input-example input-examples)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaToolComputerUse20251124$Builder b
                                               (BetaToolComputerUse20251124$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaToolComputerUse20251124$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaToolComputerUse20251124$Builder b (boolean %))
      :strict! #(.strict ^BetaToolComputerUse20251124$Builder b (boolean %))})
    (.build b)))

(defn- ->advisor-tool ^BetaAdvisorTool20260301
  [{:keys [model max-tokens max-uses caching] :as t}]
  (let [b (BetaAdvisorTool20260301/builder)]
    (when model (.model b ^String model))
    (when max-tokens (.maxTokens b (long max-tokens)))
    (when max-uses (.maxUses b (long max-uses)))
    (when caching (.caching b ^BetaCacheControlEphemeral (->cache-control caching)))
    (configure-tool-builder
     t
     {:add-allowed-caller #(.addAllowedCaller ^BetaAdvisorTool20260301$Builder b
                                               (BetaAdvisorTool20260301$AllowedCaller/of (clojure.core/name %)))
      :cache-control! #(.cacheControl ^BetaAdvisorTool20260301$Builder b ^BetaCacheControlEphemeral %)
      :defer-loading! #(.deferLoading ^BetaAdvisorTool20260301$Builder b (boolean %))
      :strict! #(.strict ^BetaAdvisorTool20260301$Builder b (boolean %))})
    (.build b)))

(defn- ->mcp-toolset ^BetaMcpToolset
  [{:keys [mcp-server-name configs default-config cache-control]}]
  (let [b (BetaMcpToolset/builder)]
    (when mcp-server-name (.mcpServerName b ^String mcp-server-name))
    (when configs
      (let [cb (BetaMcpToolset$Configs/builder)]
        (doseq [[k v] configs] (.putAdditionalProperty cb (name k) (->json v)))
        (.configs b (.build cb))))
    (when default-config
      (let [db (BetaMcpToolDefaultConfig/builder)]
        (when (contains? default-config :defer-loading) (.deferLoading db (boolean (:defer-loading default-config))))
        (when (contains? default-config :enabled) (.enabled db (boolean (:enabled default-config))))
        (.defaultConfig b (.build db))))
    (when cache-control (.cacheControl b (->cache-control cache-control)))
    (.build b)))

(def ^:private dated-tool-variants
  {:web-search {"20250305" ["BetaWebSearchTool20250305" "ofWebSearchTool20250305" "ofBetaWebSearchTool20250305"]
                "20260209" ["BetaWebSearchTool20260209" "ofWebSearchTool20260209" "ofBetaWebSearchTool20260209"]
                "20260318" ["BetaWebSearchTool20260318" "ofWebSearchTool20260318" "ofBetaWebSearchTool20260318"]}
   :web-fetch {"20250910" ["BetaWebFetchTool20250910" "ofWebFetchTool20250910" "ofBetaWebFetchTool20250910"]
               "20260209" ["BetaWebFetchTool20260209" "ofWebFetchTool20260209" "ofBetaWebFetchTool20260209"]
               "20260309" ["BetaWebFetchTool20260309" "ofWebFetchTool20260309" "ofBetaWebFetchTool20260309"]
               "20260318" ["BetaWebFetchTool20260318" "ofWebFetchTool20260318" "ofBetaWebFetchTool20260318"]}
   :code-execution {"20250522" ["BetaCodeExecutionTool20250522" "ofCodeExecutionTool20250522" "ofBetaCodeExecutionTool20250522"]
                    "20250825" ["BetaCodeExecutionTool20250825" "ofCodeExecutionTool20250825" "ofBetaCodeExecutionTool20250825"]
                    "20260120" ["BetaCodeExecutionTool20260120" "ofCodeExecutionTool20260120" "ofBetaCodeExecutionTool20260120"]
                    "20260521" ["BetaCodeExecutionTool20260521" "ofCodeExecutionTool20260521" "ofBetaCodeExecutionTool20260521"]}
   :bash {"20241022" ["BetaToolBash20241022" "ofBash20241022" "ofBetaToolBash20241022"]
          "20250124" ["BetaToolBash20250124" "ofBash20250124" "ofBetaToolBash20250124"]}
   :text-editor {"20241022" ["BetaToolTextEditor20241022" "ofTextEditor20241022" "ofBetaToolTextEditor20241022"]
                 "20250124" ["BetaToolTextEditor20250124" "ofTextEditor20250124" "ofBetaToolTextEditor20250124"]
                 "20250429" ["BetaToolTextEditor20250429" "ofTextEditor20250429" "ofBetaToolTextEditor20250429"]
                 "20250728" ["BetaToolTextEditor20250728" "ofTextEditor20250728" "ofBetaToolTextEditor20250728"]}
   :computer-use {"20241022" ["BetaToolComputerUse20241022" "ofComputerUse20241022" "ofBetaToolComputerUse20241022"]
                  "20250124" ["BetaToolComputerUse20250124" "ofComputerUse20250124" "ofBetaToolComputerUse20250124"]
                  "20251124" ["BetaToolComputerUse20251124" "ofComputerUse20251124" "ofBetaToolComputerUse20251124"]}})

(defn- invoke-method [^Object target method & values]
  (let [^Class target-class (class target)
        compatible? (fn [^Class parameter-class value]
                     (let [value-class (class value)]
                       (or (and value-class (.isAssignableFrom parameter-class value-class))
                           (and (.isPrimitive parameter-class)
                                (= value-class
                                   ({Boolean/TYPE Boolean
                                     Long/TYPE Long
                                     Integer/TYPE Integer
                                     Double/TYPE Double}
                                    parameter-class))))))
        ^java.lang.reflect.Method method-ref
        (first (filter #(and (= method (.getName ^java.lang.reflect.Method %))
                             (= (count values) (alength (.getParameterTypes ^java.lang.reflect.Method %)))
                             (every? true?
                                     (map compatible?
                                          (vec (.getParameterTypes ^java.lang.reflect.Method %))
                                          values)))
                       (.getMethods target-class)))]
    (.invoke method-ref target (object-array values))))

(defn- invoke-static-zero [class-name method]
  (let [^Class target-class (Class/forName (str "com.anthropic.models.beta.messages." class-name))
        ^java.lang.reflect.Method method-ref
        (first (filter #(and (= method (.getName ^java.lang.reflect.Method %))
                             (= 0 (alength (.getParameterTypes ^java.lang.reflect.Method %))))
                       (.getMethods target-class)))]
    (.invoke method-ref nil (object-array 0))))

(defn- new-instance [class-name]
  (let [^Class target-class (Class/forName (str "com.anthropic.models.beta.messages." class-name))]
    (.newInstance target-class)))

(defn- invoke-static [class-name method value]
  (let [^Class target-class (Class/forName (str "com.anthropic.models.beta.messages." class-name))
        ^java.lang.reflect.Method method-ref
        (first (filter #(and (= method (.getName ^java.lang.reflect.Method %))
                             (= 1 (alength (.getParameterTypes ^java.lang.reflect.Method %))))
                       (.getMethods target-class)))]
    (.invoke method-ref nil (object-array [value]))))

(defn- build-dated-tool [class-name t]
  (let [builder (invoke-static-zero class-name "builder")
        input-class (str class-name "$InputExample$Builder")]
    (doseq [c (:allowed-callers t)] (invoke-method builder "addAllowedCaller" (invoke-static (str class-name "$AllowedCaller") "of" (name c))))
    (when (:cache-control t) (invoke-method builder "cacheControl" (->cache-control (:cache-control t))))
    (when (some? (:defer-loading t)) (invoke-method builder "deferLoading" (boolean (:defer-loading t))))
    (when (some? (:strict t)) (invoke-method builder "strict" (boolean (:strict t))))
    (when-let [v (:max-uses t)] (invoke-method builder "maxUses" (long v)))
    (when-let [v (:max-content-tokens t)] (invoke-method builder "maxContentTokens" (long v)))
    (when-let [v (:max-characters t)] (invoke-method builder "maxCharacters" (long v)))
    (when-let [v (:display-height-px t)] (invoke-method builder "displayHeightPx" (long v)))
    (when-let [v (:display-width-px t)] (invoke-method builder "displayWidthPx" (long v)))
    (when-let [v (:display-number t)] (invoke-method builder "displayNumber" (long v)))
    (when (some? (:enable-zoom t)) (invoke-method builder "enableZoom" (boolean (:enable-zoom t))))
    (when (some? (:use-cache t)) (invoke-method builder "useCache" (boolean (:use-cache t))))
    (when-let [v (:model t)] (invoke-method builder "model" ^String v))
    (when-let [v (:max-tokens t)] (invoke-method builder "maxTokens" (long v)))
    (when-let [v (:caching t)] (invoke-method builder "caching" (->cache-control v)))
    (when (:user-location t) (invoke-method builder "userLocation" (->user-location (:user-location t))))
    (when (:citations t) (invoke-method builder "citations" (->citations (:citations t))))
    (when-let [v (:response-inclusion t)]
      (when-let [response-class (try (Class/forName (str "com.anthropic.models.beta.messages." class-name "$ResponseInclusion")) (catch ClassNotFoundException _ nil))]
        (when-let [response-method (first (filter #(= "of" (.getName ^java.lang.reflect.Method %)) (.getMethods ^Class response-class)))]
          (invoke-method builder "responseInclusion"
                         (.invoke ^java.lang.reflect.Method response-method nil (object-array [(name v)]))))))
    (when (seq (:allowed-domains t)) (invoke-method builder "allowedDomains" ^java.util.List (vec (:allowed-domains t))))
    (when (seq (:blocked-domains t)) (invoke-method builder "blockedDomains" ^java.util.List (vec (:blocked-domains t))))
    (when (seq (:input-examples t))
      (invoke-method builder "inputExamples"
                     ^java.util.List
                     (mapv (fn [example]
                             (let [input-builder (new-instance input-class)]
                               (doseq [[k v] example]
                                 (invoke-method input-builder "putAdditionalProperty" (name k) (->json v)))
                               (invoke-method input-builder "build")))
                           (:input-examples t))))
    (invoke-method builder "build")))

(defn- dated-tool [family t]
  (let [variants (get dated-tool-variants family)
        version (if-let [version (:version t)] (if (keyword? version) (name version) (str version))
                      (last (sort (keys variants))))
        [class-name _ _] (get variants version)]
    (if class-name
      (build-dated-tool class-name t)
      (throw (ex-info "Unsupported server tool version"
                      {:anthropic/error :unsupported-server-tool-version
                       :type family :version (:version t)})))))

(defn- validate-tool-version [family t expected]
  (when-let [version (:version t)]
    (let [version (if (keyword? version) (name version) (str version))]
      (when-not (= expected version)
        (throw (ex-info "Unsupported server tool version"
                        {:anthropic/error :unsupported-server-tool-version
                         :type family :version (:version t)})))))
  t)

(defn- ->server-tool ^BetaToolUnion [{:keys [type] :as t}]
  (let [family (keyword type)]
    (case family
      (:web-search :web-fetch :code-execution :bash :text-editor :computer-use)
      (let [version (if-let [version (:version t)] (if (keyword? version) (name version) (str version))
                    (last (sort (keys (get dated-tool-variants family)))))
            [_ constructor _] (get-in dated-tool-variants [family version])]
        (if constructor
          (invoke-static "BetaToolUnion" constructor (dated-tool family t))
          (throw (ex-info "Unsupported server tool version"
                          {:anthropic/error :unsupported-server-tool-version
                           :type family :version (:version t)}))))
      :memory (invoke-static "BetaToolUnion" "ofMemoryTool20250818"
                             (->memory-tool (validate-tool-version family t "20250818")))
      :tool-search (case (keyword (:variant t))
                     :bm25 (BetaToolUnion/ofSearchToolBm25_20251119
                            (->tool-search-bm25 (validate-tool-version family t "20251119")))
                     :regex (BetaToolUnion/ofSearchToolRegex20251119
                             (->tool-search-regex (validate-tool-version family t "20251119"))))
      :advisor (BetaToolUnion/ofAdvisorTool20260301
                (->advisor-tool (validate-tool-version family t "20260301")))
      :mcp-toolset (BetaToolUnion/ofMcpToolset (->mcp-toolset t))
      (throw (ex-info "Unsupported server tool type" {:anthropic/error :unsupported-server-tool :type type})))))

(defn- ->count-tool ^MessageCountTokensParams$Tool [{:keys [type] :as t}]
  (let [family (keyword type)]
    (if (contains? dated-tool-variants family)
      (let [version (if-let [version (:version t)] (if (keyword? version) (name version) (str version))
                    (last (sort (keys (get dated-tool-variants family)))))
            [_ _ constructor] (get-in dated-tool-variants [family version])]
        (if constructor
          (invoke-static "MessageCountTokensParams$Tool" constructor (dated-tool family t))
          (throw (ex-info "Unsupported server tool version"
                          {:anthropic/error :unsupported-server-tool-version
                           :type family :version (:version t)}))))
      (case family
        :memory (invoke-static "MessageCountTokensParams$Tool" "ofBetaMemoryTool20250818"
                               (->memory-tool (validate-tool-version family t "20250818")))
        :tool-search (case (keyword (:variant t))
                        :bm25 (MessageCountTokensParams$Tool/ofBetaToolSearchToolBm25_20251119
                               (->tool-search-bm25 (validate-tool-version family t "20251119")))
                        :regex (MessageCountTokensParams$Tool/ofBetaToolSearchToolRegex20251119
                                (->tool-search-regex (validate-tool-version family t "20251119"))))
        :advisor (MessageCountTokensParams$Tool/ofBetaAdvisorTool20260301
                  (->advisor-tool (validate-tool-version family t "20260301")))
        :mcp-toolset (MessageCountTokensParams$Tool/ofBetaMcpToolset (->mcp-toolset t))
        (MessageCountTokensParams$Tool/ofBeta (->custom-tool t))))))

(defn- server-tool? [t]
  ;; Only a recognized `:type` makes a tool server-side. A tool carrying `:fn` is
  ;; executed locally by `run-beta-tools`, so it is always custom. Any other
  ;; `:type` belongs to the caller and must not steal the custom-tool path.
  (and (nil? (:fn t))
       (contains? server-tool-types (keyword (:type t)))))

(defn- ->tool ^BetaToolUnion [t]
  (if (server-tool? t)
    (->server-tool t)
    (BetaToolUnion/ofBetaTool (->custom-tool t))))

(defn- ->metadata ^BetaMetadata [{:keys [user-id]}]
  (-> (BetaMetadata/builder) (.userId ^String user-id) (.build)))

(defn- ->json-output-format ^BetaJsonOutputFormat [schema]
  (let [sb (BetaJsonOutputFormat$Schema/builder)]
    (doseq [[k v] schema]
      (.putAdditionalProperty sb ^String (name k) (->json v)))
    (-> (BetaJsonOutputFormat/builder) (.schema (.build sb)) (.build))))

(defn- ->output-config ^BetaOutputConfig [schema effort task-budget]
  (let [b (BetaOutputConfig/builder)]
    (when schema (.format b (->json-output-format schema)))
    (when effort (.effort b (BetaOutputConfig$Effort/of (name effort))))
    (when task-budget
      (.taskBudget b
                   (let [tb (BetaTokenTaskBudget/builder)]
                     (.total tb (long (:total task-budget)))
                     (when (:remaining task-budget) (.remaining tb (long (:remaining task-budget))))
                     (.build tb))))
    (.build b)))

(defn- ->context-edit ^BetaContextManagementConfig$Edit
  [{:keys [type clear-tool-inputs instructions keep] :as edit}]
  (case (keyword type)
    :clear-tool-uses-20250919
    (BetaContextManagementConfig$Edit/ofClearToolUses20250919
     (let [b (BetaClearToolUses20250919Edit/builder)]
       (when (contains? edit :clear-tool-inputs)
         (.clearToolInputs b (boolean clear-tool-inputs)))
       (.build b)))
    :clear-thinking-20251015
    (BetaContextManagementConfig$Edit/ofClearThinking20251015
     (let [b (BetaClearThinking20251015Edit/builder)]
       (when keep
         (if (= :all (keyword keep))
           (.keepAll b)))
       (.build b)))
    :compact-20260112
    (BetaContextManagementConfig$Edit/ofCompact20260112
     (let [b (BetaCompact20260112Edit/builder)]
       (when instructions (.instructions b ^String instructions))
       (.build b)))
    (throw (ex-info "Unsupported context management edit"
                    {:anthropic/error :unsupported-context-management-edit :type type}))))

(defn- ->context-management ^BetaContextManagementConfig
  [{:keys [edits]}]
  (let [b (BetaContextManagementConfig/builder)]
    (doseq [edit edits]
      (.addEdit b ^BetaContextManagementConfig$Edit (->context-edit edit)))
    (.build b)))

(defn- ->diagnostics ^BetaDiagnosticsParam
  [{:keys [previous-message-id]}]
  (let [b (BetaDiagnosticsParam/builder)]
    (when previous-message-id (.previousMessageId b ^String previous-message-id))
    (.build b)))

(defn- ->fallback-param ^BetaFallbackParam
  [{:keys [model max-tokens output-config speed thinking]}]
  (let [b (doto (BetaFallbackParam/builder)
            (.model ^String model))]
    (when max-tokens (.maxTokens b (long max-tokens)))
    (when output-config
      (.outputConfig b (->output-config (:schema output-config) (:effort output-config) (:task-budget output-config))))
    (when speed (.speed b (BetaFallbackParam$Speed/of (name speed))))
    (when thinking
      (case (keyword (:type thinking))
        :enabled (.enabledThinking b (long (:budget-tokens thinking)))
        :disabled (.thinking b (.build (BetaThinkingConfigDisabled/builder)))
        :adaptive (.thinking b (.build (BetaThinkingConfigAdaptive/builder)))))
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
           tool-choice thinking metadata service-tier response-format output-format effort container inference-geo
           task-budget
           context-management diagnostics speed
           user-profile-id cache-control betas mcp-servers fallbacks fallback-credit-token
           extra-headers extra-query extra-body]
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
    (when (or response-format effort task-budget) (.outputConfig b (->output-config response-format effort task-budget)))
    (when output-format (.outputFormat b (->json-output-format output-format)))
    (when context-management (.contextManagement b (->context-management context-management)))
    (when diagnostics (.diagnostics b (->diagnostics diagnostics)))
    (when speed
      (.speed b (case (keyword speed)
                  :standard MessageCreateParams$Speed/STANDARD
                  :fast MessageCreateParams$Speed/FAST
                  (throw (ex-info "Unsupported speed"
                                  {:anthropic/error :unsupported-speed :speed speed})))))
    (when fallbacks
      (if (= :default (keyword fallbacks))
        (.fallbacksDefault b)
        (.fallbacksOfFallbackParams b (mapv ->fallback-param fallbacks))))
    (when fallback-credit-token (.fallbackCreditToken b ^String fallback-credit-token))
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
    (doseq [tool tools] (.addTool b (->count-tool tool)))
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
  "Send a beta Messages request and return a generic Clojure map response.

  Request maps support context-management, diagnostics, speed, and tool-choice
  disable-parallel-tool-use options. Tool specs support response-inclusion,
  input-examples, eager-input-streaming, caching, and dated :version options."
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
  [call-fn params {:keys [max-iterations on-message on-turn]
                   :or {max-iterations 10 on-turn (fn [_ params] params)}}]
  (loop [iterations 0
         params params
         messages (cond
                    (nil? (:messages params)) []
                    (string? (:messages params)) [{:role :user :content (:messages params)}]
                    :else (vec (:messages params)))]
    (when (>= iterations max-iterations)
      (throw (ex-info "Beta tool loop exceeded max iterations"
                      {:anthropic/error :max-iterations-exceeded
                       :iterations iterations
                       :messages messages})))
    (let [fns (beta-tool-fns (:tools params))
          response (call-fn (-> params strip-tool-fns (assoc :messages messages)))
          tool-uses (filterv #(= :tool-use (:type %)) (:content response))]
      (when on-message (on-message response))
      (if (or (= :tool-use (:stop-reason response)) (seq tool-uses))
        (let [results (mapv (fn [{:keys [name] :as block}]
                              (if-let [f (get fns name)]
                                (beta-tool-result block f)
                                (throw (ex-info "Tool call has no matching :fn"
                                                {:anthropic/error :no-tool-fn :name name}))))
                            tool-uses)
              next-messages (conj messages
                                  {:role :assistant :content (:content response)}
                                  {:role :user :content results})
              next-params (on-turn response (assoc params :messages next-messages))]
          (recur (inc iterations)
                 next-params
                 (or (:messages next-params) next-messages)))
        (do
          (on-turn response (assoc params :messages
                                   (conj messages {:role :assistant :content (:content response)})))
          (assoc response :messages (conj messages {:role :assistant :content (:content response)})))))))

(defn run-beta-tools
  "Run beta Messages with local tool functions until no tool is requested.

  Options include `:max-iterations`, `:on-message`, and `:on-turn`. `:on-turn`
  receives each assistant response and the current params, and returns params
  for the next iteration, allowing tools and request settings to change. Tool
  specs support response-inclusion, input-examples, eager-input-streaming,
  caching, and dated :version options."
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
