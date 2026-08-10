(ns anthropic.beta
  "Clojure wrappers over the beta agents-platform APIs of the official
  Anthropic Java SDK: skills, memory stores, agents, sessions, deployments,
  deployment runs, environments, vaults, user profiles, and webhooks.

  These wrap beta endpoints that Anthropic may change. Errors follow
  `anthropic.core`'s contract: API/IO failures are ex-info keyed
  `:anthropic/error` with the SDK exception as cause."
  (:require [anthropic.core]
            [clojure.string :as str]
            [clojure.walk :as walk])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue MultipartField UnwrapWebhookParams)
           (com.anthropic.core.http Headers HttpResponse StreamResponse)
           (com.anthropic.errors AnthropicException)
           (com.anthropic.models.beta.skills SkillCreateParams
                                             SkillCreateResponse
                                             SkillDeleteResponse
                                             SkillListPage
                                             SkillListResponse
                                             SkillRetrieveResponse)
           (com.anthropic.models.beta.skills.versions VersionCreateParams
                                                       VersionCreateResponse
                                                       VersionDeleteParams
                                                       VersionDeleteResponse
                                                       VersionDownloadParams
                                                       VersionListPage
                                                       VersionListResponse
                                                       VersionListParams
                                                       VersionRetrieveParams
                                                       VersionRetrieveResponse)
           (com.anthropic.models.beta.memorystores BetaManagedAgentsMemoryStore
                                                   BetaManagedAgentsDeletedMemoryStore
                                                   MemoryStoreCreateParams
                                                   MemoryStoreCreateParams$Metadata
                                                   MemoryStoreListPage
                                                   MemoryStoreUpdateParams
                                                   MemoryStoreUpdateParams$Metadata)
           (com.anthropic.models.beta.memorystores.memories BetaManagedAgentsDeletedMemory
                                                            BetaManagedAgentsMemory
                                                            BetaManagedAgentsMemoryView
                                                            MemoryCreateParams
                                                            MemoryDeleteParams
                                                            MemoryListPage
                                                            MemoryListParams
                                                            MemoryRetrieveParams
                                                            MemoryUpdateParams)
           (com.anthropic.models.beta.memorystores.memoryversions BetaManagedAgentsMemoryVersion
                                                                  BetaManagedAgentsMemoryVersionOperation
                                                                  MemoryVersionListPage
                                                                  MemoryVersionListParams
                                                                  MemoryVersionRetrieveParams)
           (com.anthropic.models.beta.agents AgentCreateParams
                                             AgentCreateParams$Tool
                                             AgentCreateParams$Metadata
                                             AgentListPage
                                             AgentUpdateParams
                                             AgentUpdateParams$Tool
                                             AgentUpdateParams$Metadata
                                             BetaManagedAgentsAgent
                                             BetaManagedAgentsAgent$Skill
                                             BetaManagedAgentsAgent$Tool
                                             BetaManagedAgentsAdvisor
                                             BetaManagedAgentsAgentReference
                                             BetaManagedAgentsMultiagentSelfParams
                                             BetaManagedAgentsMultiagentSelfParams$Type
                                             BetaManagedAgentsSessionThreadAgent
                                             BetaManagedAgentsAnthropicSkill
                                             BetaManagedAgentsAnthropicSkillParams
                                             BetaManagedAgentsCustomSkill
                                             BetaManagedAgentsCustomSkillParams
                                             BetaManagedAgentsCustomTool
                                             BetaManagedAgentsCustomToolInputSchema
                                             BetaManagedAgentsCustomToolParams
                                             BetaManagedAgentsMcpServerUrlDefinition
                                             BetaManagedAgentsMcpToolset
                                             BetaManagedAgentsMcpToolsetParams
                                             BetaManagedAgentsSkillParams
                                             BetaManagedAgentsUrlMcpServerParams
                                             BetaManagedAgentsModel
                                             BetaManagedAgentsModelConfig
                                             BetaManagedAgentsModelConfig$Effort
                                             BetaManagedAgentsModelConfigParams)
           (com.anthropic.models.beta.sessions BetaManagedAgentsDeletedSession
                                               BetaManagedAgentsAgentParams
                                               BetaManagedAgentsAdvisorParams
                                               BetaManagedAgentsSession
                                               BetaManagedAgentsMultiagent
                                               BetaManagedAgentsMultiagent$Agent
                                               BetaManagedAgentsMultiagentParams
                                               BetaManagedAgentsMultiagentParams$Type
                                               BetaManagedAgentsMultiagentRosterEntryParams
                                               BetaManagedAgentsAdvisorParams$Type
                                               BetaManagedAgentsAgentParams$Type
                                               BetaManagedAgentsSystemContentBlock
                                               BetaManagedAgentsSystemMessageEvent
                                               SessionCreateParams
                                               SessionCreateParams$Builder
                                               SessionCreateParams$InitialEvent
                                               SessionCreateParams$Metadata
                                               SessionListPage
                                               SessionUpdateParams
                                               SessionUpdateParams$Builder
                                               SessionUpdateParams$Metadata)
           (com.anthropic.models.beta.sessions.events BetaManagedAgentsEventParams
                                                       BetaManagedAgentsFileRubricParams
                                                       BetaManagedAgentsSendSessionEvents
                                                       BetaManagedAgentsSessionEvent
                                                       BetaManagedAgentsTextBlock
                                                       BetaManagedAgentsTextRubricParams
                                                       BetaManagedAgentsUserDefineOutcomeEvent
                                                       BetaManagedAgentsUserDefineOutcomeEventParams
                                                       BetaManagedAgentsUserDefineOutcomeEventParams$Rubric
                                                       BetaManagedAgentsUserMessageEvent
                                                       BetaManagedAgentsUserMessageEvent$Content
                                                       BetaManagedAgentsUserMessageEventParams
                                                       BetaManagedAgentsSystemMessageEventParams
                                                       EventListPage
                                                       EventSendParams)
           (com.anthropic.models.beta.sessions.threads BetaManagedAgentsSessionThread
                                                       BetaManagedAgentsSessionThread$Agent
                                                        ThreadArchiveParams
                                                        ThreadListPage
                                                        ThreadListParams
                                                        ThreadRetrieveParams)
           (com.anthropic.models.beta.deployments BetaManagedAgentsDeployment
                                                  BetaManagedAgentsDeploymentInitialEventParams
                                                  DeploymentCreateParams
                                                  DeploymentCreateParams$Builder
                                                  DeploymentCreateParams$Metadata
                                                  DeploymentListPage
                                                  DeploymentRunParams
                                                  DeploymentUpdateParams
                                                  DeploymentUpdateParams$Builder
                                                  DeploymentUpdateParams$Metadata)
           (com.anthropic.models.beta.deploymentruns BetaManagedAgentsDeploymentRun
                                                     DeploymentRunListPage)
           (com.anthropic.models.beta.environments BetaEnvironment
                                                   BetaEnvironmentDeleteResponse
                                                   EnvironmentCreateParams
                                                   EnvironmentCreateParams$Metadata
                                                   EnvironmentListPage
                                                   EnvironmentUpdateParams
                                                   EnvironmentUpdateParams$Metadata)
           (com.anthropic.models.beta.vaults BetaManagedAgentsDeletedVault
                                             BetaManagedAgentsVault
                                             VaultCreateParams
                                             VaultCreateParams$Metadata
                                             VaultListPage
                                             VaultUpdateParams
                                             VaultUpdateParams$Metadata)
           (com.anthropic.models.beta.userprofiles BetaUserProfile
                                                   BetaUserProfileEnrollmentUrl
                                                   UserProfileCreateEnrollmentUrlParams
                                                   UserProfileCreateParams
                                                   UserProfileCreateParams$Metadata
                                                   UserProfileCreateParams$Relationship
                                                   UserProfileListPage
                                                   UserProfileUpdateParams
                                                   UserProfileUpdateParams$Metadata
                                                   UserProfileUpdateParams$Relationship)
           (com.anthropic.models.beta.webhooks BetaWebhookDeploymentArchivedEventData
                                               BetaWebhookDeploymentCreatedEventData
                                               BetaWebhookDeploymentDeletedEventData
                                               BetaWebhookDeploymentPausedEventData
                                               BetaWebhookDeploymentRunFailedEventData
                                               BetaWebhookDeploymentRunStartedEventData
                                               BetaWebhookDeploymentRunSucceededEventData
                                               BetaWebhookDeploymentUnpausedEventData
                                               BetaWebhookDeploymentUpdatedEventData
                                               BetaWebhookEnvironmentArchivedEventData
                                               BetaWebhookEnvironmentCreatedEventData
                                               BetaWebhookEnvironmentDeletedEventData
                                               BetaWebhookEnvironmentUpdatedEventData
                                               BetaWebhookEventData
                                               BetaWebhookMemoryStoreArchivedEventData
                                               BetaWebhookMemoryStoreCreatedEventData
                                               BetaWebhookMemoryStoreDeletedEventData
                                               BetaWebhookSessionCreatedEventData
                                               BetaWebhookSessionBudgetReachedEventData
                                               UnwrapWebhookEvent)
           (com.anthropic.models.beta.models BetaModelInfo
                                             ModelListPage
                                             ModelListParams
                                             ModelRetrieveParams)
           (java.util Optional)))

(set! *warn-on-reflection* true)

(def ^:private throw-normalized! @#'anthropic.core/throw-normalized!)

(defmacro ^:private with-api-errors [& body]
  `(try ~@body
        (catch AnthropicException e# (throw-normalized! e#))))

(defn- missing-key! [k]
  (throw (ex-info (str "Missing required key " k)
                  {:anthropic/error :missing-key :key k})))

(defn- unopt [^Optional o]
  (when (.isPresent o) (.get o)))

(declare ^:private additional-properties->map)

(defn- monetary-amount->map [^com.anthropic.models.beta.BetaMonetaryAmount a]
  {:amount (.amount a) :currency (keyword (str/lower-case (.asString (.currency a))))})

(defn- ->monetary-amount ^com.anthropic.models.beta.BetaMonetaryAmount
  [{:keys [amount currency]}]
  (-> (com.anthropic.models.beta.BetaMonetaryAmount/builder)
      (.amount ^String amount)
      (.currency (com.anthropic.models.beta.BetaCurrency/of (str/upper-case (name currency))))
      (.build)))

(defn- budget->map [^com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit b]
  {:max-list-cost (monetary-amount->map (.maxListCost b))
   :type (keyword (str/lower-case (.asString (.type b))))})

(defn- ->budget ^com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit
  [{:keys [max-list-cost type]}]
  (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit/builder)
      (.maxListCost (->monetary-amount max-list-cost))
      (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit$Type/of
              (if (keyword? type) (name type) type)))
      (.build)))

(defn- server-tool-usage->map
  [^com.anthropic.models.beta.sessions.BetaManagedAgentsServerToolUsage u]
  (cond-> {}
    (unopt (.webFetchRequests u)) (assoc :web-fetch-requests (unopt (.webFetchRequests u)))
    (unopt (.webSearchRequests u)) (assoc :web-search-requests (unopt (.webSearchRequests u)))))

(defn- cache-creation-usage->map
  [^com.anthropic.models.beta.sessions.BetaManagedAgentsCacheCreationUsage u]
  (cond-> {}
    (unopt (.ephemeral1hInputTokens u)) (assoc :ephemeral-1h-input-tokens (unopt (.ephemeral1hInputTokens u)))
    (unopt (.ephemeral5mInputTokens u)) (assoc :ephemeral-5m-input-tokens (unopt (.ephemeral5mInputTokens u)))))

(defprotocol ^:private UsageFields
  (usage-active-seconds [u])
  (usage-cache-creation [u])
  (usage-cache-read-input-tokens [u])
  (usage-input-tokens [u])
  (usage-list-cost [u])
  (usage-output-tokens [u])
  (usage-server-tool-use [u]))

(extend-protocol UsageFields
  com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage
  (usage-active-seconds [u] (.activeSeconds ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-cache-creation [u] (.cacheCreation ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-cache-read-input-tokens [u] (.cacheReadInputTokens ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-input-tokens [u] (.inputTokens ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-list-cost [u] (.listCost ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-output-tokens [u] (.outputTokens ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  (usage-server-tool-use [u] (.serverToolUse ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage u))
  com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage
  (usage-active-seconds [u] (.activeSeconds ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-cache-creation [u] (.cacheCreation ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-cache-read-input-tokens [u] (.cacheReadInputTokens ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-input-tokens [u] (.inputTokens ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-list-cost [u] (.listCost ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-output-tokens [u] (.outputTokens ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  (usage-server-tool-use [u] (.serverToolUse ^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadUsage u))
  com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot
  (usage-active-seconds [u] (.activeSeconds ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-cache-creation [u] (.cacheCreation ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-cache-read-input-tokens [u] (.cacheReadInputTokens ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-input-tokens [u] (.inputTokens ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-list-cost [u] (.listCost ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-output-tokens [u] (.outputTokens ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u))
  (usage-server-tool-use [u] (.serverToolUse ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot u)))

(defn- usage->map [u]
  (cond-> {}
    (unopt (usage-active-seconds u)) (assoc :active-seconds (unopt (usage-active-seconds u)))
    (unopt (usage-cache-creation u)) (assoc :cache-creation (cache-creation-usage->map (unopt (usage-cache-creation u))))
    (unopt (usage-cache-read-input-tokens u)) (assoc :cache-read-input-tokens (unopt (usage-cache-read-input-tokens u)))
    (unopt (usage-input-tokens u)) (assoc :input-tokens (unopt (usage-input-tokens u)))
    (unopt (usage-list-cost u)) (assoc :list-cost (monetary-amount->map (unopt (usage-list-cost u))))
    (unopt (usage-output-tokens u)) (assoc :output-tokens (unopt (usage-output-tokens u)))
    (unopt (usage-server-tool-use u)) (assoc :server-tool-use (server-tool-usage->map (unopt (usage-server-tool-use u))))))

(defn- json-string [^JsonValue v]
  (.convert v String))

(defn- java->clj [x]
  (cond
    (instance? java.util.Map x) (persistent!
                                 (reduce-kv (fn [acc k v]
                                              (assoc! acc (keyword (str k)) (java->clj v)))
                                            (transient {}) (into {} x)))
    (instance? java.util.List x) (mapv java->clj x)
    :else x))

(defn- json->clj [^JsonValue jv]
  (java->clj (.convert jv java.lang.Object)))

(defn- normalize-model-data [x]
  (cond
    (map? x) (reduce-kv (fn [m k v]
                          (let [k' (-> (name k)
                                       (str/replace #"([a-z0-9])([A-Z])" "$1-$2")
                                       (str/replace "_" "-")
                                       str/lower-case
                                       keyword)]
                            (assoc m k' (normalize-model-data v))))
                        {} x)
    (sequential? x) (mapv normalize-model-data x)
    :else x))

(defn- ->keyword [x]
  (-> x str str/lower-case (str/replace #"[._]" "-") keyword))

(defn- ->offset-date-time ^java.time.OffsetDateTime [x]
  (if (instance? java.time.OffsetDateTime x)
    x
    (java.time.OffsetDateTime/parse ^String x)))

(defn- ->beta-names [betas]
  (mapv #(if (keyword? %) (clojure.core/name %) %) betas))

(defn- ->list-pagination [opts set-limit set-page add-beta]
  (when-let [limit (:limit opts)] (set-limit limit))
  (when-let [page (:page opts)] (set-page page))
  (doseq [beta (->beta-names (:betas opts))] (add-beta beta)))

(defn- ->beta-model-list-params
  ^ModelListParams
  ([opts]
   (let [opts (or opts {})
         b (ModelListParams/builder)]
     (when-let [limit (:limit opts)] (.limit b (long limit)))
     (when-let [before-id (:before-id opts)] (.beforeId b ^String before-id))
     (when-let [after-id (:after-id opts)] (.afterId b ^String after-id))
     (doseq [beta (->beta-names (:betas opts))]
       (.addBeta b ^String beta))
     (.build b))))

(defn- ->beta-model-retrieve-params
  ^ModelRetrieveParams
  ([model-id] (->beta-model-retrieve-params model-id {}))
  ([model-id opts]
   (let [opts (or opts {})
         b (ModelRetrieveParams/builder)]
     (.modelId b ^String model-id)
     (doseq [beta (->beta-names (:betas opts))]
       (.addBeta b ^String beta))
     (.build b))))

(defn- beta-model->map
  ^clojure.lang.IPersistentMap
  [^BetaModelInfo m]
  (let [fallbacks (.allowedFallbackModels m)
        caps (.capabilities m)
        mit (.maxInputTokens m)
        mt (.maxTokens m)]
    (cond-> {:id (.id m)
             :display-name (.displayName m)
             :created-at (str (.createdAt m))
             :type (->keyword (json->clj (._type m)))}
      (.isPresent fallbacks) (assoc :allowed-fallback-models (.get fallbacks))
      (.isPresent mit) (assoc :max-input-tokens (.get mit))
      (.isPresent mt) (assoc :max-tokens (.get mt))
      (.isPresent caps) (assoc :capabilities
                               (normalize-model-data
                                (json->clj (JsonValue/from ^com.anthropic.models.beta.models.BetaModelCapabilities
                                                           (.get caps))))))))

(defn list-beta-models
  "List beta models as maps, newest first. Optional `opts` accepts `:limit`,
  `:before-id`, `:after-id`, and free-form string or keyword `:betas`. Each map
  includes the stable model keys plus `:allowed-fallback-models` and `:type`
  when the API reports them. Pages are followed automatically."
  ([^AnthropicClient client]
   (list-beta-models client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^ModelListParams params (->beta-model-list-params opts)
           ^ModelListPage p (-> (.beta client) (.models) (.list params))]
       (mapv beta-model->map (.autoPager p))))))

(defn get-beta-model
  "Get one beta model's info by id as a map shaped like `list-beta-models`' entries."
  ([^AnthropicClient client ^String model-id]
   (get-beta-model client model-id {}))
  ([^AnthropicClient client ^String model-id opts]
   (with-api-errors
     (let [^ModelRetrieveParams params (->beta-model-retrieve-params model-id opts)]
       (beta-model->map (-> (.beta client) (.models) (.retrieve params)))))))

(defn- ->enum-value [value allowed constructor key]
  (let [value (if (keyword? value) value (->keyword value))]
    (if (contains? allowed value)
      (constructor (name value))
      (throw (ex-info (str "Unknown " (name key) " " value)
                      {:anthropic/error :invalid-enum-value
                       :key key :value value})))))

(defn- ->skill-list-params
  ^com.anthropic.models.beta.skills.SkillListParams
  ([opts]
   (let [opts (or opts {})
         b (com.anthropic.models.beta.skills.SkillListParams/builder)]
     (->list-pagination opts #(.limit b (long %)) #(.page b ^String %)
                        #(.addBeta b ^String %))
     (when-let [source (:source opts)] (.source b ^String source))
     (.build b))))

(defn- ->version-list-params
  ^VersionListParams
  ([skill-id] (->version-list-params skill-id {}))
  ([skill-id opts]
   (let [opts (or opts {})
         b (VersionListParams/builder)]
     (.skillId b ^String skill-id)
     (->list-pagination opts #(.limit b (long %)) #(.page b ^String %)
                        #(.addBeta b ^String %))
     (.build b))))

(defn- ->memory-store-list-params
  ^com.anthropic.models.beta.memorystores.MemoryStoreListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.memorystores.MemoryStoreListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:created-at-gte opts)] (.createdAtGte b (->offset-date-time v)))
    (when-let [v (:created-at-lte opts)] (.createdAtLte b (->offset-date-time v)))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->agent-list-params
  ^com.anthropic.models.beta.agents.AgentListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.agents.AgentListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:created-at-gte opts)] (.createdAtGte b (->offset-date-time v)))
    (when-let [v (:created-at-lte opts)] (.createdAtLte b (->offset-date-time v)))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->session-list-params
  ^com.anthropic.models.beta.sessions.SessionListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.sessions.SessionListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:agent-id opts)] (.agentId b ^String v))
    (when-let [v (:agent-version opts)] (.agentVersion b (int v)))
    (when-let [v (:created-at-gt opts)] (.createdAtGt b (->offset-date-time v)))
    (when-let [v (:created-at-gte opts)] (.createdAtGte b (->offset-date-time v)))
    (when-let [v (:created-at-lt opts)] (.createdAtLt b (->offset-date-time v)))
    (when-let [v (:created-at-lte opts)] (.createdAtLte b (->offset-date-time v)))
    (when-let [v (:deployment-id opts)] (.deploymentId b ^String v))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (when-let [v (:memory-store-id opts)] (.memoryStoreId b ^String v))
    (when-let [v (:order opts)]
      (let [^com.anthropic.models.beta.sessions.SessionListParams$Order value
            (->enum-value v #{:asc :desc}
                          (fn [s#] (com.anthropic.models.beta.sessions.SessionListParams$Order/of s#)) :order)]
        (.order b value)))
    (when-let [values (:statuses opts)]
      (.statuses b ^java.util.List
                 (mapv #(->enum-value % #{:rescheduling :running :idle :terminated}
                                      (fn [s#] (com.anthropic.models.beta.sessions.SessionListParams$Status/of s#)) :statuses)
                       values)))
    (.build b)))

(defn- ->deployment-list-params
  ^com.anthropic.models.beta.deployments.DeploymentListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.deployments.DeploymentListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:agent-id opts)] (.agentId b ^String v))
    (when-let [v (:created-at-gte opts)] (.createdAtGte b (->offset-date-time v)))
    (when-let [v (:created-at-lte opts)] (.createdAtLte b (->offset-date-time v)))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (when-let [v (:status opts)]
      (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentStatus value
            (->enum-value v #{:active :paused}
                          (fn [s#] (com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentStatus/of s#)) :status)]
        (.status b value)))
    (.build b)))

(defn- ->deployment-run-list-params
  ^com.anthropic.models.beta.deploymentruns.DeploymentRunListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.deploymentruns.DeploymentRunListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:created-at-gt opts)] (.createdAtGt b (->offset-date-time v)))
    (when-let [v (:created-at-gte opts)] (.createdAtGte b (->offset-date-time v)))
    (when-let [v (:created-at-lt opts)] (.createdAtLt b (->offset-date-time v)))
    (when-let [v (:created-at-lte opts)] (.createdAtLte b (->offset-date-time v)))
    (when-let [v (:deployment-id opts)] (.deploymentId b ^String v))
    (when (some? (:has-error opts)) (.hasError b (boolean (:has-error opts))))
    (when-let [v (:trigger-type opts)]
      (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerType value
            (->enum-value v #{:schedule :manual}
                          (fn [s#] (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerType/of s#)) :trigger-type)]
        (.triggerType b value)))
    (.build b)))

(defn- ->environment-list-params
  ^com.anthropic.models.beta.environments.EnvironmentListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.environments.EnvironmentListParams/builder)]
    (->list-pagination opts #(.limit b (long %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->vault-list-params
  ^com.anthropic.models.beta.vaults.VaultListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.vaults.VaultListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->tunnel-list-params
  ^com.anthropic.models.beta.tunnels.TunnelListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.tunnels.TunnelListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->certificate-list-params
  ^com.anthropic.models.beta.tunnels.certificates.CertificateListParams
  [tunnel-id opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.tunnels.certificates.CertificateListParams/builder)]
    (.tunnelId b ^String tunnel-id)
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->dream-list-params
  ^com.anthropic.models.beta.dreams.DreamListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.dreams.DreamListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:created-at-gt opts)] (.createdAtGt b (->offset-date-time v)))
    (when-let [v (:created-at-lt opts)] (.createdAtLt b (->offset-date-time v)))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (when-let [values (:statuses opts)]
      (.statuses b ^java.util.List
                 (mapv #(->enum-value % #{:pending :running :completed :failed :cancelled}
                                      (fn [s#] (com.anthropic.models.beta.dreams.BetaDreamStatus/of s#)) :statuses)
                       values)))
    (.build b)))

(defn- ->credential-list-params
  ^com.anthropic.models.beta.vaults.credentials.CredentialListParams
  [vault-id opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.vaults.credentials.CredentialListParams/builder)]
    (.vaultId b ^String vault-id)
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when (some? (:include-archived opts)) (.includeArchived b (boolean (:include-archived opts))))
    (.build b)))

(defn- ->user-profile-list-params
  ^com.anthropic.models.beta.userprofiles.UserProfileListParams
  [opts]
  (let [opts (or opts {})
        b (com.anthropic.models.beta.userprofiles.UserProfileListParams/builder)]
    (->list-pagination opts #(.limit b (int %)) #(.page b ^String %)
                       #(.addBeta b ^String %))
    (when-let [v (:order opts)]
      (let [^com.anthropic.models.beta.userprofiles.UserProfileListParams$Order value
            (->enum-value v #{:asc :desc}
                          (fn [s#] (com.anthropic.models.beta.userprofiles.UserProfileListParams$Order/of s#)) :order)]
        (.order b value)))
    (.build b)))

;; ---- Skills ---------------------------------------------------------------

(defn- ->skill-file ^MultipartField [f]
  (let [^java.io.File file (if (string? f) (java.io.File. ^String f) f)]
    (-> (MultipartField/builder)
        (.value (.toPath file))
        (.filename (.getName file))
        (.build))))

(defn- ->skill-create-params ^SkillCreateParams [{:keys [display-title files]}]
  (when-not (seq files) (missing-key! :files))
  (let [b (SkillCreateParams/builder)]
    (when display-title (.displayTitle b ^String display-title))
    (doseq [f files] (.addFile b (->skill-file f)))
    (.build b)))

(defn- skill-map [id display-title latest-version source created-at updated-at type]
  (cond-> {:id id
           :source (str source)
           :created-at (str created-at)
           :updated-at (str updated-at)
           :type (keyword type)}
    display-title (assoc :display-title display-title)
    latest-version (assoc :latest-version latest-version)))

(defn- skill-create->map [^SkillCreateResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r) (.type r)))

(defn- skill-retrieve->map [^SkillRetrieveResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r) (.type r)))

(defn- skill-list->map [^SkillListResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r) (.type r)))

(defn create-skill
  "Create a skill from `:files` (paths or `java.io.File`s; typically a
  SKILL.md plus resources) with an optional `:display-title`. Returns the
  skill as a map (`:id`, `:display-title`, `:latest-version`, `:source`,
  `:created-at`, `:updated-at`)."
  [^AnthropicClient client req]
  (with-api-errors
    (skill-create->map (-> (.beta client) (.skills) (.create (->skill-create-params req))))))

(defn get-skill
  "Get one skill by id, as a map shaped like `create-skill`'s return."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (skill-retrieve->map (-> (.beta client) (.skills) (.retrieve skill-id)))))

(defn list-skills
  "List skills with optional `:limit`, `:page`, `:source`, and `:betas`."
  ([^AnthropicClient client] (list-skills client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^com.anthropic.models.beta.skills.SkillListParams params (->skill-list-params opts)
           ^SkillListPage p (-> (.beta client) (.skills) (.list params))]
       (mapv skill-list->map (.autoPager p))))))

(defn delete-skill
  "Delete a skill by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (let [^SkillDeleteResponse r (-> (.beta client) (.skills) (.delete skill-id))]
      {:id (.id r) :deleted true :type (keyword (.type r))})))

;; ---- Skill versions -------------------------------------------------------

(defn- ->version-create-params ^VersionCreateParams
  [skill-id {:keys [files]}]
  (when-not (seq files) (missing-key! :files))
  (let [b (VersionCreateParams/builder)]
    (.skillId b ^String skill-id)
    (doseq [f files] (.addFile b (->skill-file f)))
    (.build b)))

(defn- ->version-retrieve-params ^VersionRetrieveParams [skill-id version]
  (let [b (VersionRetrieveParams/builder)]
    (.skillId b ^String skill-id)
    (.version b ^String version)
    (.build b)))

(defn- ->version-delete-params ^VersionDeleteParams [skill-id version]
  (let [b (VersionDeleteParams/builder)]
    (.skillId b ^String skill-id)
    (.version b ^String version)
    (.build b)))

(defn- ->version-download-params ^VersionDownloadParams [skill-id version]
  (let [b (VersionDownloadParams/builder)]
    (.skillId b ^String skill-id)
    (.version b ^String version)
    (.build b)))

(defn- skill-version-map [id skill-id version name description directory created-at type]
  (cond-> {:id id
           :skill-id skill-id
           :version version
           :name name
           :description description
           :directory directory
           :created-at (str created-at)
           :type (keyword type)}
    true identity))

(defn- skill-version->map [r]
  (cond
    (instance? VersionCreateResponse r)
    (let [^VersionCreateResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r) (.type r)))
    (instance? VersionRetrieveResponse r)
    (let [^VersionRetrieveResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r) (.type r)))
    :else
    (let [^VersionListResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r) (.type r)))))

(defn- skill-version-delete->map [^VersionDeleteResponse r]
  {:id (.id r) :deleted true :type (keyword (.type r))})

(defn create-skill-version
  "Create a new skill version for `skill-id` from `:files` (paths or
  `java.io.File`s). Returns the version as a map."
  [^AnthropicClient client ^String skill-id req]
  (with-api-errors
    (skill-version->map (-> (.beta client) (.skills) (.versions)
                            (.create (->version-create-params skill-id req))))))

(defn get-skill-version
  "Get one skill version."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (skill-version->map (-> (.beta client) (.skills) (.versions)
                            (.retrieve (->version-retrieve-params skill-id version))))))

(defn list-skill-versions
  "List skill versions with optional `:limit`, `:page`, and `:betas`."
  ([^AnthropicClient client ^String skill-id]
   (list-skill-versions client skill-id {}))
  ([^AnthropicClient client ^String skill-id opts]
   (with-api-errors
     (let [^VersionListParams params (->version-list-params skill-id opts)
           ^VersionListPage p (-> (.beta client) (.skills) (.versions)
                                  (.list params))]
       (mapv skill-version->map (.autoPager p))))))

(defn delete-skill-version
  "Delete a skill version. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (skill-version-delete->map (-> (.beta client) (.skills) (.versions)
                                   (.delete (->version-delete-params skill-id version))))))

(defn download-skill-version
  "Download a skill version archive. Returns the response body as a byte array."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (let [^HttpResponse r (-> (.beta client) (.skills) (.versions)
                              (.download (->version-download-params skill-id version)))]
      (with-open [body (.body r)]
        (.readAllBytes body)))))

;; ---- Memory stores ---------------------------------------------------------

(defn- ->ms-create-metadata ^MemoryStoreCreateParams$Metadata [m]
  (let [b (MemoryStoreCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->ms-update-metadata ^MemoryStoreUpdateParams$Metadata [m]
  (let [b (MemoryStoreUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->memory-store-create-params ^MemoryStoreCreateParams
  [{:keys [name description metadata]}]
  (when-not name (missing-key! :name))
  (let [b (MemoryStoreCreateParams/builder)]
    (.name b ^String name)
    (when description (.description b ^String description))
    (when metadata (.metadata b (->ms-create-metadata metadata)))
    (.build b)))

(defn- ->memory-store-update-params ^MemoryStoreUpdateParams
  [memory-store-id {:keys [name description metadata]}]
  (let [b (MemoryStoreUpdateParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (when name (.name b ^String name))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->ms-update-metadata metadata)))
    (.build b)))

(defn- memory-store->map [^BetaManagedAgentsMemoryStore r]
  (cond-> {:id (.id r)
           :name (.name r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :type (->keyword (unopt (.asString (._type r))))}
    (unopt (.metadata r))
    (assoc :metadata (additional-properties->map
                      (._additionalProperties ^com.anthropic.models.beta.memorystores.BetaManagedAgentsMemoryStore$Metadata
                                               (unopt (.metadata r)))))
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn create-memory-store
  "Create a memory store: `:name` (required), `:description`, `:metadata`.
  Returns the store as a map (`:id`, `:name`, `:description`, `:created-at`,
  `:updated-at`)."
  [^AnthropicClient client req]
  (with-api-errors
    (memory-store->map (-> (.beta client) (.memoryStores)
                           (.create (->memory-store-create-params req))))))

(defn get-memory-store
  "Get a memory store by id, as a map like `create-memory-store`'s return."
  [^AnthropicClient client ^String memory-store-id]
  (with-api-errors
    (memory-store->map (-> (.beta client) (.memoryStores) (.retrieve memory-store-id)))))

(defn list-memory-stores
  "List memory stores with optional `:created-at-gte`, `:created-at-lte`,
  `:include-archived`, `:limit`, `:page`, and `:betas`."
  ([^AnthropicClient client] (list-memory-stores client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^MemoryStoreListPage p (-> (.beta client) (.memoryStores)
                                      (.list (->memory-store-list-params opts)))]
       (mapv memory-store->map (.autoPager p))))))

(defn update-memory-store
  "Update a memory store's `:name`, `:description`, or `:metadata`. Returns
  the updated store map."
  [^AnthropicClient client ^String memory-store-id changes]
  (with-api-errors
    (memory-store->map (-> (.beta client) (.memoryStores)
                           (.update (->memory-store-update-params memory-store-id changes))))))

(defn archive-memory-store
  "Archive a memory store by id. Returns the archived store map."
  [^AnthropicClient client ^String memory-store-id]
  (with-api-errors
    (memory-store->map (-> (.beta client) (.memoryStores) (.archive memory-store-id)))))

(defn delete-memory-store
  "Delete a memory store by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String memory-store-id]
  (with-api-errors
    (let [^BetaManagedAgentsDeletedMemoryStore d
          (-> (.beta client) (.memoryStores) (.delete memory-store-id))]
      {:id (.id d) :deleted true :type (keyword (.type d))})))

;; ---- Memories -------------------------------------------------------------

(defn- memory-view ^BetaManagedAgentsMemoryView [v]
  (BetaManagedAgentsMemoryView/of (name v)))

(defn- ->memory-create-params ^MemoryCreateParams
  [memory-store-id {:keys [path content view]}]
  (when-not path (missing-key! :path))
  (let [b (MemoryCreateParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.path b ^String path)
    (when content (.content b ^String content))
    (when view (.view b (memory-view view)))
    (.build b)))

(defn- ->memory-retrieve-params ^MemoryRetrieveParams
  [memory-store-id memory-id]
  (let [b (MemoryRetrieveParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryId b ^String memory-id)
    (.build b)))

(defn- ->memory-update-params ^MemoryUpdateParams
  [memory-store-id memory-id {:keys [path content view precondition]}]
  (let [b (MemoryUpdateParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryId b ^String memory-id)
    (when path (.path b ^String path))
    (when content (.content b ^String content))
    (when view (.view b (memory-view view)))
    (when precondition
      (let [pb (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsPrecondition/builder)]
        (when (:type precondition)
          (.type pb (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsPrecondition$Type/of
                     (name (:type precondition)))))
        (when (:content-sha256 precondition) (.contentSha256 pb ^String (:content-sha256 precondition)))
        (.precondition b (.build pb))))
    (.build b)))

(defn- ->memory-list-params ^MemoryListParams
  [memory-store-id {:keys [path-prefix depth limit page view]}]
  (let [b (MemoryListParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (when path-prefix (.pathPrefix b ^String path-prefix))
    (when depth (.depth b (int depth)))
    (when limit (.limit b (int limit)))
    (when page (.page b ^String page))
    (when view (.view b (memory-view view)))
    (.build b)))

(defn- ->memory-delete-params ^MemoryDeleteParams
  [memory-store-id memory-id {:keys [expected-content-sha256]}]
  (let [b (MemoryDeleteParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryId b ^String memory-id)
    (when expected-content-sha256
      (.expectedContentSha256 b ^String expected-content-sha256))
    (.build b)))

(defn- memory->map [^BetaManagedAgentsMemory r]
  (cond-> {:id (.id r)
           :memory-store-id (.memoryStoreId r)
           :memory-version-id (.memoryVersionId r)
           :path (.path r)
           :content-sha256 (.contentSha256 r)
           :content-size-bytes (.contentSizeBytes r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :type (->keyword (.asString (.type r)))}
    (unopt (.content r)) (assoc :content (unopt (.content r)))))

(defn- memory-delete->map [^BetaManagedAgentsDeletedMemory r]
  {:id (.id r) :deleted true :type (->keyword (.asString (.type r)))})

(defn- memory-list-item->map
  [^com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemoryListItem r]
  (cond-> {:type (if (.isMemory r) :memory :memory-prefix)
           :path (.path r)}
    (.isMemory r) (merge (memory->map (.asMemory r)))
    (.isMemoryPrefix r) (assoc :type (->keyword (.asString (.type (.asMemoryPrefix r)))))
    ))

(defn create-memory
  "Create a memory in `memory-store-id`: `:path` (required), `:content`,
  and `:view`. Returns the memory map."
  [^AnthropicClient client ^String memory-store-id req]
  (with-api-errors
    (memory->map (-> (.beta client) (.memoryStores) (.memories)
                     (.create (->memory-create-params memory-store-id req))))))

(defn get-memory
  "Get a memory by id."
  [^AnthropicClient client ^String memory-store-id ^String memory-id]
  (with-api-errors
    (memory->map (-> (.beta client) (.memoryStores) (.memories)
                     (.retrieve (->memory-retrieve-params memory-store-id memory-id))))))

(defn update-memory
  "Update a memory's `:path`, `:content`, or `:view`."
  [^AnthropicClient client ^String memory-store-id ^String memory-id changes]
  (with-api-errors
    (memory->map (-> (.beta client) (.memoryStores) (.memories)
                     (.update (->memory-update-params memory-store-id memory-id changes))))))

(defn list-memories
  "List memories (pages followed) for a memory store."
  ([^AnthropicClient client ^String memory-store-id]
   (list-memories client memory-store-id {}))
  ([^AnthropicClient client ^String memory-store-id opts]
   (with-api-errors
     (let [^MemoryListPage p (-> (.beta client) (.memoryStores) (.memories)
                                 (.list (->memory-list-params memory-store-id opts)))]
       (mapv memory-list-item->map (.autoPager p))))))

(defn delete-memory
  "Delete a memory. `opts` may include `:expected-content-sha256`."
  ([^AnthropicClient client ^String memory-store-id ^String memory-id]
   (delete-memory client memory-store-id memory-id {}))
  ([^AnthropicClient client ^String memory-store-id ^String memory-id opts]
   (with-api-errors
     (memory-delete->map (-> (.beta client) (.memoryStores) (.memories)
                             (.delete (->memory-delete-params memory-store-id memory-id opts)))))))

;; ---- Memory versions ------------------------------------------------------

(defn- ->memory-version-list-params ^MemoryVersionListParams
  [memory-store-id {:keys [memory-id api-key-id created-at-gte created-at-lte session-id limit page view operation]}]
  (let [b (MemoryVersionListParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (when memory-id (.memoryId b ^String memory-id))
    (when api-key-id (.apiKeyId b ^String api-key-id))
    (when created-at-gte (.createdAtGte b (->offset-date-time created-at-gte)))
    (when created-at-lte (.createdAtLte b (->offset-date-time created-at-lte)))
    (when session-id (.sessionId b ^String session-id))
    (when limit (.limit b (int limit)))
    (when page (.page b ^String page))
    (when view (.view b (memory-view view)))
    (when operation (.operation b (BetaManagedAgentsMemoryVersionOperation/of (name operation))))
    (.build b)))

(defn- ->memory-version-retrieve-params ^MemoryVersionRetrieveParams
  [memory-store-id memory-version-id {:keys [view]}]
  (let [b (MemoryVersionRetrieveParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryVersionId b ^String memory-version-id)
    (when view (.view b (memory-view view)))
    (.build b)))

(defn- memory-version->map [^BetaManagedAgentsMemoryVersion r]
  (cond-> {:id (.id r)
           :memory-store-id (.memoryStoreId r)
           :memory-id (.memoryId r)
           :operation (->keyword (.asString (.operation r)))
           :created-at (str (.createdAt r))}
    (unopt (.content r)) (assoc :content (unopt (.content r)))
    (unopt (.path r)) (assoc :path (unopt (.path r)))
    (unopt (.contentSha256 r)) (assoc :content-sha256 (unopt (.contentSha256 r)))
    (unopt (.contentSizeBytes r)) (assoc :content-size-bytes (unopt (.contentSizeBytes r)))
    (unopt (.redactedAt r)) (assoc :redacted-at (str (unopt (.redactedAt r))))))

(defn list-memory-versions
  "List memory versions for a memory store. Optional filters include `:memory-id`,
  `:operation`, `:view`, `:limit`, and `:page`."
  ([^AnthropicClient client ^String memory-store-id]
   (list-memory-versions client memory-store-id {}))
  ([^AnthropicClient client ^String memory-store-id opts]
   (with-api-errors
     (let [^MemoryVersionListPage p (-> (.beta client) (.memoryStores) (.memoryVersions)
                                        (.list (->memory-version-list-params memory-store-id opts)))]
       (mapv memory-version->map (.autoPager p))))))

(defn get-memory-version
  "Get one memory version. `opts` may include `:view`."
  ([^AnthropicClient client ^String memory-store-id ^String memory-version-id]
   (get-memory-version client memory-store-id memory-version-id {}))
  ([^AnthropicClient client ^String memory-store-id ^String memory-version-id opts]
   (with-api-errors
     (memory-version->map (-> (.beta client) (.memoryStores) (.memoryVersions)
                              (.retrieve (->memory-version-retrieve-params
                                          memory-store-id memory-version-id opts)))))))

(defn- ->memory-version-redact-params
  ^com.anthropic.models.beta.memorystores.memoryversions.MemoryVersionRedactParams
  [memory-store-id memory-version-id]
  (let [b (com.anthropic.models.beta.memorystores.memoryversions.MemoryVersionRedactParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryVersionId b ^String memory-version-id)
    (.build b)))

(defn redact-memory-version [^AnthropicClient client ^String memory-store-id ^String memory-version-id]
  (with-api-errors
    (memory-version->map (-> (.beta client) (.memoryStores) (.memoryVersions)
                             (.redact (->memory-version-redact-params memory-store-id memory-version-id))))))

;; ---- Agents ----------------------------------------------------------------

(defn- ->agent-create-metadata ^AgentCreateParams$Metadata [m]
  (let [b (AgentCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->agent-update-metadata ^AgentUpdateParams$Metadata [m]
  (let [b (AgentUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->agent-skill ^BetaManagedAgentsSkillParams [{:keys [type skill-id version]}]
  (when-not skill-id (missing-key! :skill-id))
  (case type
    :anthropic
    (let [b (BetaManagedAgentsAnthropicSkillParams/builder)]
      (.skillId b ^String skill-id)
      (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsAnthropicSkillParams$Type/of "anthropic"))
      (when version (.version b ^String version))
      (BetaManagedAgentsSkillParams/ofAnthropic (.build b)))
    :custom
    (let [b (BetaManagedAgentsCustomSkillParams/builder)]
      (.skillId b ^String skill-id)
      (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsCustomSkillParams$Type/of "custom"))
      (when version (.version b ^String version))
      (BetaManagedAgentsSkillParams/ofCustom (.build b)))
    (throw (ex-info (str "Unknown skill type " type)
                    {:anthropic/error :unknown-skill-type :type type}))))

(defn- ->mcp-server ^BetaManagedAgentsUrlMcpServerParams [{:keys [name url]}]
  (when-not name (missing-key! :name))
  (when-not url (missing-key! :url))
  (let [b (BetaManagedAgentsUrlMcpServerParams/builder)]
    (.name b ^String name)
    (.url b ^String url)
    (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsUrlMcpServerParams$Type/of "url"))
    (.build b)))

(defn- ->custom-tool-input-schema ^BetaManagedAgentsCustomToolInputSchema
  [{:keys [type required] :as schema}]
  (when-not type (missing-key! :input-schema))
  (let [b (BetaManagedAgentsCustomToolInputSchema/builder)
        props (dissoc schema :type :required)]
    (.type b (JsonValue/from type))
    (when required
      (.required b ^java.util.List (mapv name required)))
    (doseq [[k v] (walk/stringify-keys props)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->custom-tool ^BetaManagedAgentsCustomToolParams
  [{:keys [name description input-schema]}]
  (when-not name (missing-key! :name))
  (when-not description (missing-key! :description))
  (when-not input-schema (missing-key! :input-schema))
  (let [b (BetaManagedAgentsCustomToolParams/builder)]
    (.name b ^String name)
    (.description b ^String description)
    (.inputSchema b (->custom-tool-input-schema input-schema))
    (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolParams$Type/of "custom"))
    (.build b)))

(defn- ->mcp-toolset ^BetaManagedAgentsMcpToolsetParams [{:keys [mcp-server-name configs default-config]}]
  (when-not mcp-server-name (missing-key! :mcp-server-name))
  (let [b (BetaManagedAgentsMcpToolsetParams/builder)]
    (.mcpServerName b ^String mcp-server-name)
    (doseq [config configs]
      (.addConfig b (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfigParams/builder)
                        (.name ^String (:name config))
                        (.enabled (boolean (:enabled config)))
                        (.build))))
    (when default-config
      (.defaultConfig b (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfigParams/builder)
                            (.enabled (boolean (:enabled default-config)))
                            (.build))))
    (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams$Type/of "mcp_toolset"))
    (.build b)))

(defn- ->agent-create-tool ^AgentCreateParams$Tool [{:keys [type] :as tool}]
  (case type
    :custom (AgentCreateParams$Tool/ofCustom (->custom-tool tool))
    :mcp-toolset (AgentCreateParams$Tool/ofMcpToolset (->mcp-toolset tool))
    :agent-toolset-20260401
    (AgentCreateParams$Tool/ofAgentToolset20260401
     (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params/builder)
         (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params$Type/of
                 "agent_toolset_20260401"))
         (.build)))
    (throw (ex-info (str "Unknown tool type " type)
                    {:anthropic/error :unknown-tool-type :type type}))))

(defn- ->agent-update-tool ^AgentUpdateParams$Tool [{:keys [type] :as tool}]
  (case type
    :custom (AgentUpdateParams$Tool/ofCustom (->custom-tool tool))
    :mcp-toolset (AgentUpdateParams$Tool/ofMcpToolset (->mcp-toolset tool))
    :agent-toolset-20260401
    (AgentUpdateParams$Tool/ofAgentToolset20260401
     (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params/builder)
         (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401Params$Type/of
                 "agent_toolset_20260401"))
         (.build)))
    (throw (ex-info (str "Unknown tool type " type)
                    {:anthropic/error :unknown-tool-type :type type}))))

(defn- ->agent-roster-entry ^BetaManagedAgentsMultiagentRosterEntryParams [entry]
  (cond
    (string? entry)
    (BetaManagedAgentsMultiagentRosterEntryParams/ofString ^String entry)

    (= :agent (:type entry))
    (let [b (BetaManagedAgentsAgentParams/builder)]
      (.id b ^String (:id entry))
      (when (:version entry) (.version b (int (:version entry))))
      (.type b (BetaManagedAgentsAgentParams$Type/of "agent"))
      (BetaManagedAgentsMultiagentRosterEntryParams/ofAgent (.build b)))

    (= :self (:type entry))
    (BetaManagedAgentsMultiagentRosterEntryParams/ofSelf
     (-> (BetaManagedAgentsMultiagentSelfParams/builder)
         (.type (BetaManagedAgentsMultiagentSelfParams$Type/of "self"))
         (.build)))

    (= :advisor (:type entry))
    (BetaManagedAgentsMultiagentRosterEntryParams/ofAdvisor
     (-> (BetaManagedAgentsAdvisorParams/builder)
         (.model ^String (:model entry))
         (.type (BetaManagedAgentsAdvisorParams$Type/of "advisor"))
         (.build)))

    :else
    (throw (ex-info (str "Unknown multiagent roster entry " entry)
                    {:anthropic/error :unknown-multiagent-roster-entry
                     :entry entry}))))

(defn- ->agent-multiagent ^BetaManagedAgentsMultiagentParams
  [{:keys [type agents]}]
  (let [b (BetaManagedAgentsMultiagentParams/builder)]
    (.type b (BetaManagedAgentsMultiagentParams$Type/of (name type)))
    (doseq [entry agents]
      (.addAgent b (->agent-roster-entry entry)))
    (.build b)))

(defn- ->managed-agent-model-config ^BetaManagedAgentsModelConfigParams [model effort inference-geo speed]
  (let [b (BetaManagedAgentsModelConfigParams/builder)]
    (.id b (BetaManagedAgentsModel/of ^String model))
    (when effort
      (.effort b
               (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfigParams$Effort$BetaManagedAgentsEffortLevel/of
                (name effort))))
    (when inference-geo (.inferenceGeo b ^String inference-geo))
    (when speed
      (.speed b ^com.anthropic.models.beta.agents.BetaManagedAgentsModelConfigParams$Speed
              (->enum-value speed #{:standard :fast}
                            (fn [s#] (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfigParams$Speed/of s#)) :speed)))
    (.build b)))

(defn- ->agent-create-params ^AgentCreateParams
  [{:keys [name model effort inference-geo speed system description metadata skills mcp-servers
           tools multiagent betas]}]
  (when-not name (missing-key! :name))
  (when-not model (missing-key! :model))
  (let [b (AgentCreateParams/builder)]
    (.name b ^String name)
    (if (or effort inference-geo speed)
      (.model b ^BetaManagedAgentsModelConfigParams (->managed-agent-model-config model effort inference-geo speed))
      (.model b ^BetaManagedAgentsModel (BetaManagedAgentsModel/of ^String model)))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-create-metadata metadata)))
    (doseq [skill skills] (.addSkill b (->agent-skill skill)))
    (doseq [server mcp-servers] (.addMcpServer b (->mcp-server server)))
    (doseq [tool tools] (.addTool b (->agent-create-tool tool)))
    (when multiagent (.multiagent b (->agent-multiagent multiagent)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- ->agent-update-params ^AgentUpdateParams
  [agent-id {:keys [version name model effort inference-geo speed system description metadata
                    skills mcp-servers tools multiagent betas]}]
  (let [b (AgentUpdateParams/builder)]
    (.agentId b ^String agent-id)
    (when version (.version b (int version)))
    (when name (.name b ^String name))
    (when model
    (if (or effort inference-geo speed)
        (.model b ^BetaManagedAgentsModelConfigParams (->managed-agent-model-config model effort inference-geo speed))
        (.model b ^BetaManagedAgentsModel (BetaManagedAgentsModel/of ^String model))))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-update-metadata metadata)))
    (doseq [skill skills] (.addSkill b (->agent-skill skill)))
    (doseq [server mcp-servers] (.addMcpServer b (->mcp-server server)))
    (doseq [tool tools] (.addTool b (->agent-update-tool tool)))
    (when multiagent (.multiagent b (->agent-multiagent multiagent)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- agent-skill->map [^BetaManagedAgentsAgent$Skill s]
  (cond
    (.isAnthropic s)
    (let [^BetaManagedAgentsAnthropicSkill skill (.asAnthropic s)]
      (cond-> {:type :anthropic :skill-id (.skillId skill)}
        (.version skill) (assoc :version (.version skill))))
    (.isCustom s)
    (let [^BetaManagedAgentsCustomSkill skill (.asCustom s)]
      (cond-> {:type :custom :skill-id (.skillId skill)}
        (.version skill) (assoc :version (.version skill))))
    :else {:type :unknown}))

(defn- mcp-server->map [^BetaManagedAgentsMcpServerUrlDefinition s]
  {:name (.name s) :url (.url s) :type (->keyword (.asString (.type s)))})

(defn- permission-policy->map [p]
  (cond
    (instance? com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfig$PermissionPolicy p)
    (let [^com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfig$PermissionPolicy p p]
      (cond (.isAlwaysAllow p) {:type :always-allow}
            (.isAlwaysAsk p) {:type :always-ask}
            :else {:type :unknown}))
    (instance? com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig$PermissionPolicy p)
    (let [^com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig$PermissionPolicy p p]
      (cond (.isAlwaysAllow p) {:type :always-allow}
            (.isAlwaysAsk p) {:type :always-ask}
            :else {:type :unknown}))
    (instance? com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig$PermissionPolicy p)
    (let [^com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig$PermissionPolicy p p]
      (cond (.isAlwaysAllow p) {:type :always-allow}
            (.isAlwaysAsk p) {:type :always-ask}
            :else {:type :unknown}))
    (instance? com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig$PermissionPolicy p)
    (let [^com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig$PermissionPolicy p p]
      (cond (.isAlwaysAllow p) {:type :always-allow}
            (.isAlwaysAsk p) {:type :always-ask}
            :else {:type :unknown}))
    :else {:type :unknown}))

(defn- custom-tool->map [^BetaManagedAgentsCustomTool tool]
  {:type :custom :name (.name tool) :description (.description tool)
   :input-schema (let [^BetaManagedAgentsCustomToolInputSchema s (.inputSchema tool)]
                   (cond-> {:type (json->clj (._type s))}
                     (unopt (.properties s))
                     (assoc :properties
                            (additional-properties->map
                             (._additionalProperties
                              ^com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema$Properties
                              (unopt (.properties s)))))
                     (unopt (.required s)) (assoc :required (unopt (.required s)))))} )

(defn- mcp-tool-config->map
  [^com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfig c]
  {:name (.name c) :enabled (.enabled c) :permission-policy (permission-policy->map (.permissionPolicy c))})

(defn- mcp-default-config->map
  [^com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig c]
  {:enabled (.enabled c) :permission-policy (permission-policy->map (.permissionPolicy c))})

(defn- agent-tool-config->map
  [^com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig c]
  {:name (->keyword (.asString (.name c))) :enabled (.enabled c)
   :permission-policy (permission-policy->map (.permissionPolicy c))})

(defn- agent-default-config->map
  [^com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig c]
  {:enabled (.enabled c) :permission-policy (permission-policy->map (.permissionPolicy c))})

(defn- agent-tool-payload->map [t]
  (cond
    (instance? BetaManagedAgentsCustomTool t)
    (custom-tool->map t)
    (instance? BetaManagedAgentsMcpToolset t)
    (let [^BetaManagedAgentsMcpToolset tool t]
      {:type :mcp-toolset :mcp-server-name (.mcpServerName tool)
       :configs (mapv mcp-tool-config->map (.configs tool))
       :default-config (mcp-default-config->map (.defaultConfig tool))})
    (instance? com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401 t)
    (let [^com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401 tool t]
      {:type :agent-toolset-20260401
       :configs (mapv agent-tool-config->map (.configs tool))
       :default-config (agent-default-config->map (.defaultConfig tool))})
    :else {:type :unknown}))

(defn- agent-tool->map [^BetaManagedAgentsAgent$Tool t]
  (cond
    (.isCustom t) (agent-tool-payload->map (.asCustom t))
    (.isMcpToolset t) (agent-tool-payload->map (.asMcpToolset t))
    (.isAgentToolset20260401 t) (agent-tool-payload->map (.asAgentToolset20260401 t))
    :else {:type :unknown}))

(defn- model-effort->keyword [^BetaManagedAgentsModelConfig$Effort effort]
  (cond
    (.isLow effort) :low
    (.isMedium effort) :medium
    (.isHigh effort) :high
    (.isXhigh effort) :xhigh
    (.isMax effort) :max
    :else (let [^JsonValue json (or (unopt (._json effort)) (JsonValue/from nil))]
            (.convert json Object))))

(declare ^:private agent-ref->map)

(defn- multiagent->map [^BetaManagedAgentsMultiagent r]
  {:type (keyword (.asString (.type r)))
   :agents (mapv agent-ref->map (.agents r))})

(defn- agent->map [^BetaManagedAgentsAgent r]
  (cond-> {:id (.id r)
           :name (.name r)
           :model (.id ^BetaManagedAgentsModelConfig (.model r))
           :version (.version r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :type (keyword (.asString (.type r)))
           :metadata (additional-properties->map (._additionalProperties (.metadata r)))}
    (unopt (.effort ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :effort (model-effort->keyword
                    (unopt (.effort ^BetaManagedAgentsModelConfig (.model r)))))
    (unopt (.inferenceGeo ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :inference-geo (unopt (.inferenceGeo ^BetaManagedAgentsModelConfig (.model r))))
    (unopt (.speed ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :speed (let [^com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig$Speed speed
                        (unopt (.speed ^BetaManagedAgentsModelConfig (.model r)))]
                    (->keyword (.asString speed))))
    (unopt (.system r)) (assoc :system (unopt (.system r)))
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (seq (.skills r)) (assoc :skills (mapv agent-skill->map (.skills r)))
    (seq (.mcpServers r)) (assoc :mcp-servers (mapv mcp-server->map (.mcpServers r)))
    (seq (.tools r)) (assoc :tools (mapv agent-tool->map (.tools r)))
    (unopt (.multiagent r)) (assoc :multiagent (multiagent->map (unopt (.multiagent r))))))

(defn create-agent
  "Create a managed agent: `:name` and `:model` (required), `:system`,
  `:description`, `:metadata`, `:skills`, `:mcp-servers`, `:tools`, and
  `:multiagent`, `:betas`, `:inference-geo`, and `:speed` when using model config.
  Returns the agent as a map (`:id`, `:name`, `:model`, `:version`,
  `:system`, `:description`, `:skills`, `:mcp-servers`, `:tools`, `:multiagent`,
  `:created-at`, `:updated-at`). Tool maps include custom input-schema
  `:properties`, and tool config `:permission-policy` values."
  [^AnthropicClient client req]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents) (.create (->agent-create-params req))))))

(defn get-agent
  "Get an agent by id, as a map like `create-agent`'s return, including
  `:multiagent` when present. Tool maps include custom input-schema
  `:properties` and tool config `:permission-policy` values."
  [^AnthropicClient client ^String agent-id]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents) (.retrieve agent-id)))))

(defn list-agents
  "List agents with optional `:created-at-gte`, `:created-at-lte`,
  `:include-archived`, `:limit`, `:page`, and `:betas`."
  ([^AnthropicClient client] (list-agents client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^AgentListPage p (-> (.beta client) (.agents)
                                (.list (->agent-list-params opts)))]
       (mapv agent->map (.autoPager p))))))

(defn update-agent
  "Update an agent. `changes` may include `:version` (the current agent version,
  for optimistic concurrency - see `:version` in `get-agent`'s return) plus
  `:name`, `:model`, `:inference-geo`, `:system`, `:description`, `:metadata`, or
  `:multiagent` and `:betas`. Returns the updated agent map, including
  `:multiagent` when present. Tool maps include custom input-schema
  `:properties` and tool config `:permission-policy` values."
  [^AnthropicClient client ^String agent-id changes]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents)
                    (.update (->agent-update-params agent-id changes))))))

(defn archive-agent
  "Archive an agent by id. Returns the archived agent map, including
  `:multiagent` when present."
  [^AnthropicClient client ^String agent-id]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents) (.archive agent-id)))))

;; ---- Sessions ----------------------------------------------------------------

(defn- ->session-create-metadata ^SessionCreateParams$Metadata [m]
  (let [b (SessionCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->session-update-metadata ^SessionUpdateParams$Metadata [m]
  (let [b (SessionUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(declare ^:private ->session-create-initial-event)
(declare ^:private ->deployment-resource)

(defn- ->session-agent-update
  ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgentUpdate
  [{:keys [mcp-servers tools]}]
  (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgentUpdate/builder)]
    (doseq [server mcp-servers]
      (.addMcpServer b (->mcp-server server)))
    (doseq [tool tools]
      (.addTool b
                (case (:type tool)
                  :custom
                  (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgentUpdate$Tool/ofCustom
                   (->custom-tool tool))
                  :mcp-toolset
                  (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgentUpdate$Tool/ofMcpToolset
                   (->mcp-toolset tool))
                  (throw (ex-info (str "Unknown tool type " (:type tool))
                                  {:anthropic/error :unknown-tool-type :type (:type tool)})))))
    (.build b)))

(defn- ->session-create-params ^SessionCreateParams
  [{:keys [agent title environment-id metadata initial-events budget resources vault-ids betas]}]
  (when-not agent (missing-key! :agent))
  (let [^SessionCreateParams$Builder b (SessionCreateParams/builder)]
    (.agent b ^String agent)
    (when title (.title b ^String title))
    (when environment-id (.environmentId b ^String environment-id))
    (when metadata (.metadata b (->session-create-metadata metadata)))
    (when budget (.budget b (->budget budget)))
    (doseq [resource resources]
      (case (:type resource)
        :file (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams (->deployment-resource resource))
        :github-repository (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams (->deployment-resource resource))
        :memory-store (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam (->deployment-resource resource))
        (->deployment-resource resource)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (doseq [event initial-events]
      (let [^SessionCreateParams$InitialEvent event (->session-create-initial-event event)]
        (.addInitialEvent b event)))
    (.build b)))

(defn- ->session-update-params ^SessionUpdateParams
  [session-id {:keys [title metadata budget agent vault-ids betas]}]
  (let [^SessionUpdateParams$Builder b (SessionUpdateParams/builder)]
    (.sessionId b ^String session-id)
    (when title (.title b ^String title))
    (when metadata (.metadata b (->session-update-metadata metadata)))
    (when budget (.budget b (->budget budget)))
    (when agent (.agent b (->session-agent-update agent)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(declare ^:private agent-ref->map session-resource->map)

(defn- session-agent->map
  [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent r]
  (cond-> {:id (.id r)
           :name (.name r)
           :model (.id ^BetaManagedAgentsModelConfig (.model r))
           :version (.version r)
           :type (keyword (.asString (.type r)))
           :mcp-servers (mapv mcp-server->map (.mcpServers r))
           :skills (mapv (fn [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Skill s]
                           (cond-> {:type (if (.isAnthropic s) :anthropic :custom)
                                    :skill-id (.skillId s)
                                    :version (.version s)}))
                         (.skills r))
           :tools (mapv (fn [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Tool t]
                          (cond
                            (.isCustom t) (agent-tool-payload->map (.asCustom t))
                            (.isMcpToolset t) (agent-tool-payload->map (.asMcpToolset t))
                            (.isAgentToolset20260401 t) (agent-tool-payload->map (.asAgentToolset20260401 t))
                            :else {:type :unknown}))
                        (.tools r))}
    (unopt (.effort ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :effort (model-effort->keyword
                    (unopt (.effort ^BetaManagedAgentsModelConfig (.model r)))))
    (unopt (.inferenceGeo ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :inference-geo (unopt (.inferenceGeo ^BetaManagedAgentsModelConfig (.model r))))
    (unopt (.speed ^BetaManagedAgentsModelConfig (.model r)))
    (assoc :speed (let [^com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig$Speed speed
                        (unopt (.speed ^BetaManagedAgentsModelConfig (.model r)))]
                    (->keyword (.asString speed))))
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.system r)) (assoc :system (unopt (.system r)))
    (unopt (.multiagent r)) (assoc :multiagent
                                   (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionMultiagentCoordinator m
                                         (unopt (.multiagent r))]
                                     {:type (->keyword (.asString (.type m)))
                                      :agents (mapv agent-ref->map (.agents m))}))))

(defn- outcome-evaluation->map [^com.anthropic.models.beta.sessions.BetaManagedAgentsOutcomeEvaluationResource r]
  (cond-> {:type (keyword (.asString (.type r)))
           :description (.description r)
           :iteration (.iteration r)
           :outcome-id (.outcomeId r)
           :result (.result r)}
    (unopt (.completedAt r)) (assoc :completed-at (str (unopt (.completedAt r))))
    (unopt (.explanation r)) (assoc :explanation (unopt (.explanation r)))))

(defn- session->map [^BetaManagedAgentsSession r]
  (cond-> {:id (.id r)
           :agent (session-agent->map (.agent r))
           :status (->keyword (.asString (.status r)))
           :stats (let [s (.stats r)] {:active-seconds (unopt (.activeSeconds s))
                                       :duration-seconds (unopt (.durationSeconds s))})
           :type (keyword (.asString (.type r)))
           :metadata (additional-properties->map (._additionalProperties (.metadata r)))
           :outcome-evaluations (mapv outcome-evaluation->map (.outcomeEvaluations r))
           :resources (mapv session-resource->map (.resources r))
           :vault-ids (vec (.vaultIds r))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.title r)) (assoc :title (unopt (.title r)))
    (.environmentId r) (assoc :environment-id (.environmentId r))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (unopt (.budget r)) (assoc :budget (budget->map (unopt (.budget r))))
    (.usage r) (assoc :usage (usage->map (.usage r)))
    (unopt (.deploymentId r)) (assoc :deployment-id (unopt (.deploymentId r)))))

(defn- advisor->map [^BetaManagedAgentsAdvisor a]
  {:type :advisor :model (.model a)})

(defn- agent-ref->map
  "An agent slot is a union of an agent reference and an advisor. The union
   wrapper is per-parent, so the bare reference types are accepted too."
  [r]
  (cond
    (instance? BetaManagedAgentsMultiagent$Agent r)
    (let [^BetaManagedAgentsMultiagent$Agent u r]
      (if (.isAdvisor u)
        (advisor->map (.asAdvisor u))
        (agent-ref->map (.asAgent u))))

    (instance? BetaManagedAgentsSessionThread$Agent r)
    (let [^BetaManagedAgentsSessionThread$Agent u r]
      (if (.isAdvisor u)
        (advisor->map (.asAdvisor u))
        (agent-ref->map (.asAgent u))))

    (instance? BetaManagedAgentsAdvisor r)
    (advisor->map r)

    (instance? BetaManagedAgentsAgentReference r)
    (let [^BetaManagedAgentsAgentReference r r]
      {:type :agent :id (.id r) :version (.version r)})

    :else
    (let [^BetaManagedAgentsSessionThreadAgent r r]
      {:type :agent :id (.id r) :version (.version r)})))

(defn create-session
  "Create a session for `:agent` (an agent id, required), with optional
  `:title`, `:environment-id`, `:metadata`, and `:budget` shaped as
  `{:max-list-cost {:amount '...' :currency :usd} :type :limit}`. Optional
  `:resources`, `:vault-ids`, `:betas`, and per-session agent overrides
  via `:agent` with `:mcp-servers` and `:tools`. Returns the
  session as a map (`:id`, `:status`, `:title`, `:environment-id`,
  `:created-at`, `:updated-at`, `:budget`, and `:usage`). Resource maps may
  include GitHub `:checkout` and memory-store `:access`, `:description`,
  `:instructions`, and `:name`. Session agent tool maps match agent tool maps,
  including custom input-schema `:properties` and config `:permission-policy`."
  [^AnthropicClient client req]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.create (->session-create-params req))))))

(defn get-session
  "Get a session by id, as a map like `create-session`'s return, including
  resource GitHub `:checkout`, memory-store fields, and complete agent tool maps."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.retrieve session-id)))))

(defn list-sessions
  "List sessions with optional `:agent-id`, `:agent-version`, timestamp
  filters, `:deployment-id`, `:include-archived`, `:limit`, `:memory-store-id`,
  `:order`, `:page`, `:statuses`, and `:betas`. Returned resource maps include
  GitHub `:checkout`, memory-store fields, and complete agent tool maps."
  ([^AnthropicClient client] (list-sessions client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^SessionListPage p (-> (.beta client) (.sessions)
                                  (.list (->session-list-params opts)))]
       (mapv session->map (.autoPager p))))))

(defn update-session
  "Update a session's `:title`, `:metadata`, `:budget`, `:agent`, `:vault-ids`,
  or `:betas`. Budget uses the `{:max-list-cost {:amount '...' :currency :usd}
  :type :limit}` shape. Returns the updated session map with complete resource
  and agent tool maps."
  [^AnthropicClient client ^String session-id changes]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions)
                      (.update (->session-update-params session-id changes))))))

(defn archive-session
  "Archive a session by id. Returns the archived session map with complete
  resource and agent tool maps."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.archive session-id)))))

(defn delete-session
  "Delete a session by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (let [^BetaManagedAgentsDeletedSession d
          (-> (.beta client) (.sessions) (.delete session-id))]
      {:id (.id d) :deleted true :type (keyword (.type d))})))

;; ---- Session events -------------------------------------------------------

(defn- ->user-message-event ^BetaManagedAgentsUserMessageEventParams [{:keys [content]}]
  (when-not content (missing-key! :content))
  (let [b (BetaManagedAgentsUserMessageEventParams/builder)]
    (if (string? content)
      (.addTextContent b ^String content)
      (doseq [block content]
        (if (string? block)
          (.addTextContent b ^String block)
          (.putAdditionalProperty b "content" (JsonValue/from block)))))
    (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams$Type/of "user_message"))
    (.build b)))

(defn- ->system-message-event ^BetaManagedAgentsSystemMessageEventParams [{:keys [content]}]
  (when-not content (missing-key! :content))
  (let [b (BetaManagedAgentsSystemMessageEventParams/builder)]
    (if (string? content)
      (.addTextContent b ^String content)
      (doseq [block content]
        (if (string? block)
          (.addTextContent b ^String block)
          (.putAdditionalProperty b "content" (JsonValue/from block)))))
    (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSystemMessageEventParams$Type/of "system_message"))
    (.build b)))

(defn- ->outcome-rubric ^BetaManagedAgentsUserDefineOutcomeEventParams$Rubric [rubric]
  (when-not rubric (missing-key! :rubric))
  (case (:type rubric)
    :text
    (let [text (:text rubric)]
      (when-not text (missing-key! :text))
      (let [b (BetaManagedAgentsTextRubricParams/builder)]
        (.content b ^String text)
        (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextRubricParams$Type/of "text"))
        (BetaManagedAgentsUserDefineOutcomeEventParams$Rubric/ofText (.build b))))
    :file
    (let [file-id (:file-id rubric)]
      (when-not file-id (missing-key! :file-id))
      (let [b (BetaManagedAgentsFileRubricParams/builder)]
        (.fileId b ^String file-id)
        (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileRubricParams$Type/of "file"))
        (BetaManagedAgentsUserDefineOutcomeEventParams$Rubric/ofFile (.build b))))
    (throw (ex-info (str "Unknown rubric type " (:type rubric))
                    {:anthropic/error :unknown-rubric-type :type (:type rubric)}))))

(defn- ->user-define-outcome-event ^BetaManagedAgentsUserDefineOutcomeEventParams
  [{:keys [description rubric max-iterations]}]
  (when-not description (missing-key! :description))
  (let [b (BetaManagedAgentsUserDefineOutcomeEventParams/builder)]
    (.description b ^String description)
    (.rubric b (->outcome-rubric rubric))
    (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEventParams$Type/of "user_define_outcome"))
    (when max-iterations (.maxIterations b (int max-iterations)))
    (.build b)))

(defn- ->session-event ^BetaManagedAgentsEventParams [{:keys [type] :as event}]
  (case type
    :user-message (BetaManagedAgentsEventParams/ofUserMessage (->user-message-event event))
    :system-message (BetaManagedAgentsEventParams/ofSystemMessage (->system-message-event event))
    :user-define-outcome (BetaManagedAgentsEventParams/ofUserDefineOutcome (->user-define-outcome-event event))
    :user-interrupt
    (BetaManagedAgentsEventParams/ofUserInterrupt
     (let [b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEventParams/builder)]
       (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEventParams$Type/of "user_interrupt"))
       (when (:session-thread-id event) (.sessionThreadId b ^String (:session-thread-id event)))
       (.build b)))
    :user-tool-confirmation
    (BetaManagedAgentsEventParams/ofUserToolConfirmation
     (let [b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams/builder)]
       (.toolUseId b ^String (:tool-use-id event))
       (.result b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams$Result/of
                   (name (:result event))))
       (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEventParams$Type/of "user_tool_confirmation"))
       (when (:deny-message event) (.denyMessage b ^String (:deny-message event)))
       (.build b)))
    :user-custom-tool-result
    (BetaManagedAgentsEventParams/ofUserCustomToolResult
     (let [b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams/builder)]
       (.customToolUseId b ^String (:custom-tool-use-id event))
       (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEventParams$Type/of "user_custom_tool_result"))
       (when (contains? event :is-error) (.isError b (boolean (:is-error event))))
       (doseq [content (:content event)]
         (when (= :text (:type content)) (.addTextContent b ^String (:text content))))
       (.build b)))
    :user-tool-result
    (BetaManagedAgentsEventParams/ofUserToolResult
     (let [b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolResultEventParams/builder)]
       (.toolUseId b ^String (:tool-use-id event))
       (.type b (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolResultEventParams$Type/of "user_tool_result"))
       (when (contains? event :is-error) (.isError b (boolean (:is-error event))))
       (doseq [content (:content event)]
         (when (= :text (:type content)) (.addTextContent b ^String (:text content))))
       (.build b)))
    (throw (ex-info (str "Unknown event type " type)
                    {:anthropic/error :unknown-event-type :type type}))))

(defn- ->session-create-initial-event ^SessionCreateParams$InitialEvent [{:keys [type] :as event}]
  (case type
    :user-message (SessionCreateParams$InitialEvent/ofUserMessage (->user-message-event event))
    :user-define-outcome (SessionCreateParams$InitialEvent/ofUserDefineOutcome
                          (->user-define-outcome-event event))
    (throw (ex-info (str "Unknown initial event type " type)
                    {:anthropic/error :unknown-event-type :type type}))))

(defn- ->deployment-initial-event ^BetaManagedAgentsDeploymentInitialEventParams [event]
  (let [^BetaManagedAgentsEventParams e (->session-event event)]
    (cond
      (.isUserMessage e) (BetaManagedAgentsDeploymentInitialEventParams/ofUserMessage (.asUserMessage e))
      (.isSystemMessage e) (BetaManagedAgentsDeploymentInitialEventParams/ofSystemMessage (.asSystemMessage e))
      (.isUserDefineOutcome e) (BetaManagedAgentsDeploymentInitialEventParams/ofUserDefineOutcome (.asUserDefineOutcome e))
      :else (throw (ex-info "Unsupported deployment initial event"
                            {:anthropic/error :unsupported-event-type})))))

(defn- ->event-send-params ^EventSendParams [session-id events]
  (let [b (EventSendParams/builder)]
    (.sessionId b ^String session-id)
    (doseq [event events] (.addEvent b (->session-event event)))
    (.build b)))

(defn- image-source->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock$Source source]
  (cond
    (.isBase64 source) {:type :base64 :media-type (.mediaType (.asBase64 source)) :data (.data (.asBase64 source))}
    (.isUrl source) {:type :url :url (.url (.asUrl source))}
    :else {:type :file :file-id (.fileId (.asFile source))}))

(defn- document-source->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Source source]
  (cond
    (.isBase64 source) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64DocumentSource s
                             (.asBase64 source)]
                         {:type :base64 :media-type (.mediaType s) :data (.data s)})
    (.isText source) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsPlainTextDocumentSource s
                           (.asText source)]
                       {:type :text :media-type (.asString (.mediaType s)) :data (.data s)})
    (.isUrl source) {:type :url :url (.url (.asUrl source))}
    :else {:type :file :file-id (.fileId (.asFile source))}))

(defn- user-content->map [^BetaManagedAgentsUserMessageEvent$Content c]
  (cond
    (.isText c) (.text ^BetaManagedAgentsTextBlock (.asText c))
    (.isImage c) {:type :image
                  :source (image-source->map (.source (.asImage c)))}
    (.isDocument c) (let [d (.asDocument c)]
                      (cond-> {:type :document
                               :source (document-source->map (.source d))}
                        (unopt (.context d)) (assoc :context (unopt (.context d)))
                        (unopt (.title d)) (assoc :title (unopt (.title d)))))
    (.isRedacted c) {:type :redacted}
    :else {:type :unknown}))

(defn- session-usage-event->map
  [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsageEvent r]
  (cond-> {:type :session-usage
           :id (.id r)
           :processed-at (str (.processedAt r))
           :usage (usage->map (.usage r))}
    (unopt (.budget r)) (assoc :budget (budget->map (unopt (.budget r))))))

(defn- session-event-common->map [^BetaManagedAgentsSessionEvent e type]
  (cond-> {:type type :id (.id e)}
    (unopt (.processedAt e)) (assoc :processed-at (str (unopt (.processedAt e))))
    (unopt (.sessionThreadId e)) (assoc :session-thread-id (unopt (.sessionThreadId e)))
    (unopt (.toolUseId e)) (assoc :tool-use-id (unopt (.toolUseId e)))
    (unopt (.name e)) (assoc :name (unopt (.name e)))
    (unopt (.agentName e)) (assoc :agent-name (unopt (.agentName e)))
    (unopt (.iteration e)) (assoc :iteration (unopt (.iteration e)))
    (unopt (.outcomeId e)) (assoc :outcome-id (unopt (.outcomeId e)))
    (unopt (.isError e)) (assoc :is-error (unopt (.isError e)))))

(defn- event-payload->map [^BetaManagedAgentsSessionEvent event]
  (let [m (json->clj (._json event))]
    (dissoc (walk/postwalk (fn [x]
                             (if (keyword? x)
                               (keyword (str/replace (name x) "_" "-"))
                               x)) m)
            :type :id :processed-at :session-thread-id :tool-use-id :name
            :agent-name :iteration :outcome-id :is-error)))

(defn- session-rubric->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEvent$Rubric rubric]
  (cond
    (.isFile rubric) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileRubric r (.asFile rubric)]
                       {:type :file :file-id (.fileId r)})
    (.isText rubric) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextRubric r (.asText rubric)]
                       {:type :text :text (.content r)})
    :else {:type :unknown}))

(defn- user-message-payload->map
  [^BetaManagedAgentsUserMessageEvent event]
  {:content (mapv user-content->map (.content event))})

(defn- system-message-payload->map
  [^BetaManagedAgentsSystemMessageEvent event]
  {:content (mapv (fn [^BetaManagedAgentsSystemContentBlock block] (.text block))
                  (.content event))})

(defn- user-define-outcome-payload->map
  [^BetaManagedAgentsUserDefineOutcomeEvent event]
  (cond-> {:description (.description event)
           :outcome-id (.outcomeId event)
           :rubric (session-rubric->map (.rubric event))}
    (unopt (.maxIterations event)) (assoc :max-iterations (unopt (.maxIterations event)))))

(defn- user-interrupt-payload->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEvent event]
  {})

(declare ^:private search-result-block->map)

(defn- user-tool-confirmation-payload->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEvent event]
  (cond-> {:result (->keyword (.asString (.result event)))}
    (unopt (.denyMessage event)) (assoc :deny-message (unopt (.denyMessage event)))))

(defn- custom-tool-content->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEvent$Content content]
  (cond
    (.isText content) (cond-> (let [^BetaManagedAgentsTextBlock block (.asText content)]
                                {:type :text :text (.text block)})
                        (unopt (.title content)) (assoc :title (unopt (.title content))))
    (.isImage content) (cond-> (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock block (.asImage content)]
                                 {:type :image :source (image-source->map (.source block))})
                         (unopt (.title content)) (assoc :title (unopt (.title content))))
    (.isDocument content) (cond-> (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock block (.asDocument content)]
                                    (cond-> {:type :document :source (document-source->map (.source block))}
                                      (unopt (.context block)) (assoc :context (unopt (.context block)))
                                      (unopt (.title block)) (assoc :title (unopt (.title block)))))
                              (unopt (.title content)) (assoc :title (unopt (.title content))))
    (.isSearchResult content) (cond-> (search-result-block->map (.asSearchResult content))
                                (unopt (.title content)) (assoc :title (unopt (.title content))))
    :else {:type :unknown}))

(defn- search-result-block->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSearchResultBlock block]
  {:type (->keyword (.asString (.type block)))
   :source (.source block)
   :title (.title block)
   :citations {:enabled (.enabled (.citations block))}
   :content (mapv (fn [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSearchResultContent content]
                    {:type (->keyword (.asString (.type content))) :text (.text content)})
                  (.content block))})

(defn- user-custom-tool-result-payload->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEvent event]
  (cond-> {:custom-tool-use-id (.customToolUseId event)}
    (unopt (.content event)) (assoc :content (mapv custom-tool-content->map (unopt (.content event))))))

(defn- user-tool-result-payload->map
  [^com.anthropic.models.beta.sessions.BetaManagedAgentsUserToolResultEvent event]
  (cond-> {}
    (unopt (.content event)) (assoc :content
                                    (mapv (fn [^com.anthropic.models.beta.sessions.BetaManagedAgentsUserToolResultEvent$Content content]
                                            (cond
                                              (.isText content) (let [^BetaManagedAgentsTextBlock block (.asText content)]
                                                                  (cond-> {:type :text :text (.text block)}
                                                                    (unopt (.title content)) (assoc :title (unopt (.title content)))))
                                              (.isImage content) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock block (.asImage content)]
                                                                   (cond-> {:type :image :source (image-source->map (.source block))}
                                                                     (unopt (.title content)) (assoc :title (unopt (.title content)))))
                                              (.isDocument content) (let [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock block (.asDocument content)]
                                                                      (cond-> {:type :document :source (document-source->map (.source block))}
                                                                        (unopt (.title content)) (assoc :title (unopt (.title content)))))
                                              (.isSearchResult content) (cond-> (search-result-block->map (.asSearchResult content))
                                                                          (unopt (.title content)) (assoc :title (unopt (.title content))))
                                              :else {:type :unknown}))
                                          (unopt (.content event))))))

(defn- session-event->map [^BetaManagedAgentsSessionEvent e]
  (cond
    (.isUserMessage e)
    (let [^BetaManagedAgentsUserMessageEvent r (.asUserMessage e)]
      (merge (session-event-common->map e :user-message)
             (user-message-payload->map r)))
    (.isSystemMessage e)
    (let [^BetaManagedAgentsSystemMessageEvent r (.asSystemMessage e)]
      (merge (session-event-common->map e :system-message)
             (system-message-payload->map r)))
    (.isUserDefineOutcome e)
    (let [^BetaManagedAgentsUserDefineOutcomeEvent r (.asUserDefineOutcome e)]
      (merge (session-event-common->map e :user-define-outcome)
             (user-define-outcome-payload->map r)))
    (.isSessionUsage e)
    (merge (session-event-common->map e :session-usage)
           (session-usage-event->map (.asSessionUsage e)))
    (.isSessionUpdated e)
    (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUpdatedEvent r (.asSessionUpdated e)]
      (merge (session-event-common->map e :session-updated)
             (cond-> {}
        (unopt (.title r)) (assoc :title (unopt (.title r)))
        (unopt (.budget r)) (assoc :budget (budget->map (unopt (.budget r))))
        (unopt (.agent r)) (assoc :agent (session-agent->map (unopt (.agent r))))
        (unopt (.metadata r)) (assoc :metadata (additional-properties->map
                                                (._additionalProperties ^com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUpdatedEvent$Metadata
                                                                         (unopt (.metadata r)))))
        )))
    (.isUserInterrupt e) (merge (session-event-common->map e :user-interrupt)
                                (user-interrupt-payload->map (.asUserInterrupt e)))
    (.isUserToolConfirmation e) (merge (session-event-common->map e :user-tool-confirmation)
                                      (user-tool-confirmation-payload->map (.asUserToolConfirmation e)))
    (.isUserCustomToolResult e) (merge (session-event-common->map e :user-custom-tool-result)
                                      (user-custom-tool-result-payload->map (.asUserCustomToolResult e)))
    (.isAgentCustomToolUse e) (merge (session-event-common->map e :agent-custom-tool-use)
                                     (event-payload->map (.asAgentCustomToolUse e)))
    (.isAgentMessage e) (merge (session-event-common->map e :agent-message)
                               (event-payload->map (.asAgentMessage e)))
    (.isAgentThinking e) (merge (session-event-common->map e :agent-thinking)
                                (event-payload->map (.asAgentThinking e)))
    (.isAgentMcpToolUse e) (merge (session-event-common->map e :agent-mcp-tool-use)
                                  (event-payload->map (.asAgentMcpToolUse e)))
    (.isAgentMcpToolResult e) (merge (session-event-common->map e :agent-mcp-tool-result)
                                    (event-payload->map (.asAgentMcpToolResult e)))
    (.isAgentToolUse e) (merge (session-event-common->map e :agent-tool-use)
                               (event-payload->map (.asAgentToolUse e)))
    (.isAgentToolResult e) (merge (session-event-common->map e :agent-tool-result)
                                  (event-payload->map (.asAgentToolResult e)))
    (.isAgentThreadMessageReceived e) (merge (session-event-common->map e :agent-thread-message-received) (event-payload->map (.asAgentThreadMessageReceived e)))
    (.isAgentThreadMessageSent e) (merge (session-event-common->map e :agent-thread-message-sent) (event-payload->map (.asAgentThreadMessageSent e)))
    (.isAgentThreadContextCompacted e) (merge (session-event-common->map e :agent-thread-context-compacted) (event-payload->map (.asAgentThreadContextCompacted e)))
    (.isSessionError e) (merge (session-event-common->map e :session-error) (event-payload->map (.asSessionError e)))
    (.isSessionStatusRescheduled e) (merge (session-event-common->map e :session-status-rescheduled) (event-payload->map (.asSessionStatusRescheduled e)))
    (.isSessionStatusRunning e) (merge (session-event-common->map e :session-status-running) (event-payload->map (.asSessionStatusRunning e)))
    (.isSessionStatusIdle e) (merge (session-event-common->map e :session-status-idle) (event-payload->map (.asSessionStatusIdle e)))
    (.isSessionStatusTerminated e) (merge (session-event-common->map e :session-status-terminated) (event-payload->map (.asSessionStatusTerminated e)))
    (.isSessionThreadCreated e) (merge (session-event-common->map e :session-thread-created) (event-payload->map (.asSessionThreadCreated e)))
    (.isSpanOutcomeEvaluationStart e) (merge (session-event-common->map e :span-outcome-evaluation-start) (event-payload->map (.asSpanOutcomeEvaluationStart e)))
    (.isSpanOutcomeEvaluationEnd e) (merge (session-event-common->map e :span-outcome-evaluation-end) (event-payload->map (.asSpanOutcomeEvaluationEnd e)))
    (.isSpanModelRequestStart e) (merge (session-event-common->map e :span-model-request-start) (event-payload->map (.asSpanModelRequestStart e)))
    (.isSpanModelRequestEnd e) (merge (session-event-common->map e :span-model-request-end) (event-payload->map (.asSpanModelRequestEnd e)))
    (.isSpanOutcomeEvaluationOngoing e) (merge (session-event-common->map e :span-outcome-evaluation-ongoing) (event-payload->map (.asSpanOutcomeEvaluationOngoing e)))
    (.isSessionDeleted e) (merge (session-event-common->map e :session-deleted) (event-payload->map (.asSessionDeleted e)))
    (.isSessionThreadStatusRunning e) (merge (session-event-common->map e :session-thread-status-running) (event-payload->map (.asSessionThreadStatusRunning e)))
    (.isSessionThreadStatusIdle e) (merge (session-event-common->map e :session-thread-status-idle) (event-payload->map (.asSessionThreadStatusIdle e)))
    (.isSessionThreadStatusTerminated e) (merge (session-event-common->map e :session-thread-status-terminated) (event-payload->map (.asSessionThreadStatusTerminated e)))
    (.isUserToolResult e) (merge (session-event-common->map e :user-tool-result)
                                 (user-tool-result-payload->map (.asUserToolResult e)))
    (.isSessionThreadStatusRescheduled e) (session-event-common->map e :session-thread-status-rescheduled)
    :else {:type :unknown}))

(defn- send-data->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data d]
  (let [common (cond-> {:id (.id d)}
                 (unopt (.processedAt d)) (assoc :processed-at (str (unopt (.processedAt d))))
                 (unopt (.sessionThreadId d)) (assoc :session-thread-id (unopt (.sessionThreadId d)))
                 (unopt (.toolUseId d)) (assoc :tool-use-id (unopt (.toolUseId d)))
                 (unopt (.isError d)) (assoc :is-error (unopt (.isError d))))]
    (cond
      (.isUserMessage d) (merge (assoc common :type :user-message)
                                (user-message-payload->map (.asUserMessage d)))
      (.isSystemMessage d) (merge (assoc common :type :system-message)
                                  (system-message-payload->map (.asSystemMessage d)))
      (.isUserDefineOutcome d) (merge (assoc common :type :user-define-outcome)
                                     (user-define-outcome-payload->map (.asUserDefineOutcome d)))
      (.isUserInterrupt d) (merge (assoc common :type :user-interrupt)
                                  (user-interrupt-payload->map (.asUserInterrupt d)))
      (.isUserToolConfirmation d) (merge (assoc common :type :user-tool-confirmation)
                                         (user-tool-confirmation-payload->map (.asUserToolConfirmation d)))
      (.isUserCustomToolResult d) (merge (assoc common :type :user-custom-tool-result)
                                         (user-custom-tool-result-payload->map (.asUserCustomToolResult d)))
      (.isUserToolResult d) (merge (assoc common :type :user-tool-result)
                                   (user-tool-result-payload->map (.asUserToolResult d)))
      :else (assoc common :type :unknown))))

(defn- send-session-events->map [^BetaManagedAgentsSendSessionEvents r]
  {:data (mapv send-data->map (or (unopt (.data r)) []))})

(defn send-session-events
  "Send a vector of session event maps to `session-id`. Event maps support
  `{:type :user-message :content ...}`, `{:type :system-message :content ...}`,
  and `{:type :user-define-outcome :description ... :rubric ...}`. Returns
  `{:data [...]}` maps with common fields and each event variant's typed payload,
  including user message content, outcome rubric, tool confirmation result, and
  tool result content."
  [^AnthropicClient client ^String session-id events]
  (with-api-errors
    (send-session-events->map (-> (.beta client) (.sessions) (.events)
                                  (.send (->event-send-params session-id events))))))

(defn list-session-events
  "List session events (pages followed) as normalized event maps. Options include
  `:created-at-gt`, `:created-at-gte`, `:created-at-lt`, `:created-at-lte`, `:limit`,
  `:order`, `:page`, `:types`, and `:betas`."
  ([^AnthropicClient client ^String session-id]
   (list-session-events client session-id {}))
  ([^AnthropicClient client ^String session-id opts]
  (with-api-errors
    (let [b (com.anthropic.models.beta.sessions.events.EventListParams/builder)]
      (.sessionId b ^String session-id)
      (when (:created-at-gt opts) (.createdAtGt b (->offset-date-time (:created-at-gt opts))))
      (when (:created-at-gte opts) (.createdAtGte b (->offset-date-time (:created-at-gte opts))))
      (when (:created-at-lt opts) (.createdAtLt b (->offset-date-time (:created-at-lt opts))))
      (when (:created-at-lte opts) (.createdAtLte b (->offset-date-time (:created-at-lte opts))))
      (when (:limit opts) (.limit b (int (:limit opts))))
      (when (:order opts) (.order b (com.anthropic.models.beta.sessions.events.EventListParams$Order/of
                                     (if (keyword? (:order opts)) (name (:order opts)) (:order opts)))))
      (when (:page opts) (.page b ^String (:page opts)))
      (when (:types opts) (.types b ^java.util.List
                                   (mapv #(if (keyword? %) (name %) %) (:types opts))))
      (doseq [beta (->beta-names (:betas opts))] (.addBeta b ^String beta))
      (let [^EventListPage p (-> (.beta client) (.sessions) (.events) (.list (.build b)))]
        (mapv session-event->map (.autoPager p)))))))

;; ---- Event streams --------------------------------------------------------

(defprotocol ^:private StreamEventJson
  (stream-event-json [event]))

(extend-protocol StreamEventJson
  com.anthropic.models.beta.sessions.events.BetaManagedAgentsStreamSessionEvents
  (stream-event-json [event] (unopt (._json event)))
  com.anthropic.models.beta.sessions.threads.BetaManagedAgentsStreamSessionThreadEvents
  (stream-event-json [event] (unopt (._json event))))

(defn- stream-event->map [event]
  (let [m (json->clj (stream-event-json event))]
    (update m :type ->keyword)))

(defn- consume-event-stream [^StreamResponse sr on-event]
  (with-open [^StreamResponse s sr]
    (mapv (fn [event]
            (let [m (stream-event->map event)]
              (when on-event (on-event m))
              m))
          (iterator-seq (.iterator (.stream s))))))

(defn- enum-name [x]
  (str/replace (name x) "-" "."))

(defn- ->event-deltas [event-deltas]
  (mapv #(com.anthropic.models.beta.sessions.BetaManagedAgentsDeltaType/of
          (enum-name %))
        event-deltas))

(defn- ->session-event-stream-params
  ^com.anthropic.models.beta.sessions.events.EventStreamParams
  [session-id {:keys [event-deltas]}]
  (let [b (com.anthropic.models.beta.sessions.events.EventStreamParams/builder)]
    (.sessionId b ^String session-id)
    (when event-deltas (.eventDeltas b ^java.util.List (->event-deltas event-deltas)))
    (.build b)))

(defn- ->thread-event-stream-params
  ^com.anthropic.models.beta.sessions.threads.events.EventStreamParams
  [session-id thread-id {:keys [event-deltas]}]
  (let [b (com.anthropic.models.beta.sessions.threads.events.EventStreamParams/builder)]
    (.sessionId b ^String session-id)
    (.threadId b ^String thread-id)
    (when event-deltas (.eventDeltas b ^java.util.List (->event-deltas event-deltas)))
    (.build b)))

(defn stream-session-events
  "Open an SSE stream of beta session events. Calls `on-event` with a normalized
  event map for each event, returns a vector of all event maps, and closes the
  HTTP stream automatically. Event maps retain raw event fields and use `:type`
  keywords such as `:agent-message`, `:agent-thinking`, `:session-status-running`,
  and `:event-delta`. `:event-deltas` may contain `:agent-message` and
  `:agent-thinking` to narrow the delta stream."
  ([client session-id] (stream-session-events client session-id {} nil))
  ([client session-id opts] (stream-session-events client session-id opts nil))
  ([^AnthropicClient client ^String session-id opts on-event]
   (with-api-errors
     (let [^StreamResponse sr (-> (.beta client) (.sessions) (.events)
                                  (.streamStreaming
                                   (->session-event-stream-params session-id opts)))]
       (consume-event-stream sr on-event)))))

(defn stream-thread-events
  "Open an SSE stream of beta session-thread events. Calls `on-event` with a
  normalized event map for each event, returns a vector of all event maps, and
  closes the HTTP stream automatically. Event maps retain raw event fields and
  use `:type` keywords such as `:agent-message`, `:agent-thinking`,
  `:session-status-running`, and `:event-delta`. `:event-deltas` may contain
  `:agent-message` and `:agent-thinking` to narrow the delta stream."
  ([client session-id thread-id] (stream-thread-events client session-id thread-id {} nil))
  ([client session-id thread-id opts]
   (stream-thread-events client session-id thread-id opts nil))
  ([^AnthropicClient client ^String session-id ^String thread-id opts on-event]
   (with-api-errors
     (let [^StreamResponse sr (-> (.beta client) (.sessions) (.threads) (.events)
                                  (.streamStreaming
                                   (->thread-event-stream-params session-id thread-id opts)))]
       (consume-event-stream sr on-event)))))

;; ---- Session threads ------------------------------------------------------

(defn- ->thread-retrieve-params ^ThreadRetrieveParams [session-id thread-id]
  (let [b (ThreadRetrieveParams/builder)]
    (.sessionId b ^String session-id)
    (.threadId b ^String thread-id)
    (.build b)))

(defn- ->thread-list-params ^ThreadListParams [session-id]
  (let [b (ThreadListParams/builder)]
    (.sessionId b ^String session-id)
    (.build b)))

(defn- ->thread-archive-params ^ThreadArchiveParams [session-id thread-id]
  (let [b (ThreadArchiveParams/builder)]
    (.sessionId b ^String session-id)
    (.threadId b ^String thread-id)
    (.build b)))

(defn- session-thread-stats->map
  [^com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadStats s]
  {:active-seconds (unopt (.activeSeconds s))
   :duration-seconds (unopt (.durationSeconds s))
   :startup-seconds (unopt (.startupSeconds s))})

(defn- session-thread->map [^BetaManagedAgentsSessionThread r]
  (cond-> {:id (.id r)
           :session-id (.sessionId r)
           :agent (agent-ref->map (.agent r))
           :status (->keyword (.asString (.status r)))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.parentThreadId r)) (assoc :parent-thread-id (unopt (.parentThreadId r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (unopt (.usage r)) (assoc :usage (usage->map (unopt (.usage r))))
    (unopt (.stats r)) (assoc :stats (session-thread-stats->map (unopt (.stats r))))
    (.type r) (assoc :type (->keyword (.asString (.type r))))))

(defn get-session-thread
  "Get a session thread by session id and thread id."
  [^AnthropicClient client ^String session-id ^String thread-id]
  (with-api-errors
    (session-thread->map (-> (.beta client) (.sessions) (.threads)
                             (.retrieve (->thread-retrieve-params session-id thread-id))))))

(defn list-session-threads
  "List session threads (pages followed) for a session."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (let [^ThreadListPage p (-> (.beta client) (.sessions) (.threads)
                                (.list (->thread-list-params session-id)))]
      (mapv session-thread->map (.autoPager p)))))

(defn archive-session-thread
  "Archive a session thread by session id and thread id."
  [^AnthropicClient client ^String session-id ^String thread-id]
  (with-api-errors
    (session-thread->map (-> (.beta client) (.sessions) (.threads)
                             (.archive (->thread-archive-params session-id thread-id))))))

;; ---- Thread events --------------------------------------------------------

(defn- ->thread-event-list-params
  ^com.anthropic.models.beta.sessions.threads.events.EventListParams
  [session-id thread-id {:keys [limit page betas]}]
  (let [b (com.anthropic.models.beta.sessions.threads.events.EventListParams/builder)]
    (.sessionId b ^String session-id)
    (.threadId b ^String thread-id)
    (when limit (.limit b (int limit)))
    (when page (.page b ^String page))
    (doseq [beta (->beta-names betas)] (.addBeta b ^String beta))
    (.build b)))

(defn list-thread-events
  "List thread events (pages followed) as normalized event maps."
  [^AnthropicClient client ^String session-id ^String thread-id opts]
  (with-api-errors
    (let [^com.anthropic.models.beta.sessions.threads.events.EventListPage p
          (-> (.beta client) (.sessions) (.threads) (.events)
              (.list (->thread-event-list-params session-id thread-id opts)))]
      (mapv session-event->map (.autoPager p)))))

;; ---- Session resources ----------------------------------------------------

(defn- ->github-checkout [checkout]
  (when checkout
    (case (:type checkout)
      :branch (let [name (:name checkout)]
                (when-not name (missing-key! :name))
                (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsBranchCheckout/builder)]
                  (.name b ^String name)
                  (.type b (com.anthropic.models.beta.sessions.BetaManagedAgentsBranchCheckout$Type/of "branch"))
                  (com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams$Checkout/ofBranch
                   (.build b))))
      :commit (let [sha (:sha checkout)]
                (when-not sha (missing-key! :sha))
                (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsCommitCheckout/builder)]
                  (.sha b ^String sha)
                  (.type b (com.anthropic.models.beta.sessions.BetaManagedAgentsCommitCheckout$Type/of "commit"))
                  (com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams$Checkout/ofCommit
                   (.build b))))
      (throw (ex-info (str "Unknown checkout type " (:type checkout))
                      {:anthropic/error :unknown-checkout-type :type (:type checkout)})))))

(defn- github-checkout->map
  [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource$Checkout checkout]
  (cond
    (.isBranch checkout) (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsBranchCheckout b (.asBranch checkout)]
                           {:type :branch :name (.name b)})
    (.isCommit checkout) (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsCommitCheckout c (.asCommit checkout)]
                           {:type :commit :sha (.sha c)})
    :else (throw (ex-info "Unsupported checkout type" {:anthropic/error :unknown-checkout-type}))))

(defn- memory-access->keyword
  [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource$Access access]
  (->keyword (.asString access)))

(defn- ->session-resource-add-params
  ^com.anthropic.models.beta.sessions.resources.ResourceAddParams
  [session-id {:keys [file-id mount-path]}]
  (when-not file-id (missing-key! :file-id))
  (let [resource (com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams/builder)
        b (com.anthropic.models.beta.sessions.resources.ResourceAddParams/builder)]
    (.fileId resource ^String file-id)
    (.type resource (com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams$Type/of "file"))
    (when mount-path (.mountPath resource ^String mount-path))
    (.sessionId b ^String session-id)
    (.betaManagedAgentsFileResourceParams b (.build resource))
    (.build b)))

(defn- ->session-resource-list-params
  ^com.anthropic.models.beta.sessions.resources.ResourceListParams
  [session-id {:keys [limit page]}]
  (let [b (com.anthropic.models.beta.sessions.resources.ResourceListParams/builder)]
    (.sessionId b ^String session-id)
    (when limit (.limit b (int limit)))
    (when page (.page b ^String page))
    (.build b)))

(defn- ->session-resource-retrieve-params
  ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveParams [session-id resource-id]
  (let [b (com.anthropic.models.beta.sessions.resources.ResourceRetrieveParams/builder)]
    (.sessionId b ^String session-id) (.resourceId b ^String resource-id) (.build b)))

(defn- ->session-resource-update-params
  ^com.anthropic.models.beta.sessions.resources.ResourceUpdateParams [session-id resource-id {:keys [authorization-token]}]
  (when-not authorization-token (missing-key! :authorization-token))
  (let [b (com.anthropic.models.beta.sessions.resources.ResourceUpdateParams/builder)]
    (.sessionId b ^String session-id) (.resourceId b ^String resource-id)
    (.authorizationToken b ^String authorization-token) (.build b)))

(defn- ->session-resource-delete-params
  ^com.anthropic.models.beta.sessions.resources.ResourceDeleteParams [session-id resource-id]
  (let [b (com.anthropic.models.beta.sessions.resources.ResourceDeleteParams/builder)]
    (.sessionId b ^String session-id) (.resourceId b ^String resource-id) (.build b)))

(defprotocol ^:private ResourceUnion
  (resource-id [r])
  (resource-mount-path [r])
  (resource-created-at [r])
  (resource-updated-at [r])
  (resource-is-file [r])
  (resource-is-memory-store [r])
  (resource-as-file [r])
  (resource-is-github [r])
  (resource-as-github [r])
  (resource-as-memory-store [r]))

(extend-protocol ResourceUnion
  com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse
  (resource-id [r] (unopt (.id ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r)))
  (resource-mount-path [r] (unopt (.mountPath ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r)))
  (resource-created-at [r] (unopt (.createdAt ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r)))
  (resource-updated-at [r] (unopt (.updatedAt ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r)))
  (resource-is-file [r] (.isFile ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  (resource-is-memory-store [r] (.isMemoryStore ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  (resource-as-file [r] (.asFile ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  (resource-is-github [r] (.isGitHubRepository ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  (resource-as-github [r] (.asGitHubRepository ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  (resource-as-memory-store [r] (.asMemoryStore ^com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse r))
  com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse
  (resource-id [r] (unopt (.id ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r)))
  (resource-mount-path [r] (unopt (.mountPath ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r)))
  (resource-created-at [r] (unopt (.createdAt ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r)))
  (resource-updated-at [r] (unopt (.updatedAt ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r)))
  (resource-is-file [r] (.isFile ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r))
  (resource-is-memory-store [r] (.isMemoryStore ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r))
  (resource-as-file [r] (.asFile ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r))
  (resource-is-github [r] (.isGitHubRepository ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r))
  (resource-as-github [r] (.asGitHubRepository ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r))
  (resource-as-memory-store [r] (.asMemoryStore ^com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse r)))

(defn- session-github-resource->map
  [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource x id mount-path created-at updated-at]
  (cond-> {:type :github-repository :id (or id (.id x)) :url (.url x)
           :mount-path mount-path}
    (unopt (.checkout x)) (assoc :checkout (github-checkout->map (unopt (.checkout x))))
    created-at (assoc :created-at (str created-at))
    updated-at (assoc :updated-at (str updated-at))))

(defn- session-memory-resource->map
  [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource x id mount-path created-at updated-at]
  (cond-> {:type :memory-store :memory-store-id (.memoryStoreId x)
           :mount-path mount-path}
    id (assoc :id id)
    created-at (assoc :created-at (str created-at))
    updated-at (assoc :updated-at (str updated-at))
    (unopt (.access x)) (assoc :access (memory-access->keyword (unopt (.access x))))
    (unopt (.description x)) (assoc :description (unopt (.description x)))
    (unopt (.instructions x)) (assoc :instructions (unopt (.instructions x)))
    (unopt (.name x)) (assoc :name (unopt (.name x)))))

(defn- session-resource->map [r]
  (cond
    (instance? com.anthropic.models.beta.sessions.resources.BetaManagedAgentsFileResource r)
    (let [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsFileResource x r]
      (cond-> {:type :file :id (.id x) :file-id (.fileId x) :mount-path (.mountPath x)}
        (.createdAt x) (assoc :created-at (str (.createdAt x)))
        (.updatedAt x) (assoc :updated-at (str (.updatedAt x)))))
    (instance? com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource r)
    (let [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource x r]
      (session-github-resource->map x (.id x) (.mountPath x) (.createdAt x) (.updatedAt x)))
    (instance? com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource r)
    (let [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource x r]
      (session-memory-resource->map x nil (unopt (.mountPath x)) nil nil))
    (resource-is-file r)
    (let [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsFileResource x (resource-as-file r)]
      (cond-> {:type :file :id (resource-id r) :file-id (.fileId x)
               :mount-path (resource-mount-path r)}
        (resource-created-at r) (assoc :created-at (str (resource-created-at r)))
        (resource-updated-at r) (assoc :updated-at (str (resource-updated-at r)))))
    (resource-is-github r)
    (session-github-resource->map (resource-as-github r) (resource-id r)
                                  (resource-mount-path r) (resource-created-at r)
                                  (resource-updated-at r))
    (resource-is-memory-store r)
    (session-memory-resource->map (resource-as-memory-store r) (resource-id r)
                                  (resource-mount-path r) (resource-created-at r)
                                  (resource-updated-at r))
    :else
    (throw (ex-info "Unsupported session resource"
                    {:anthropic/error :unknown-resource-type}))))

(defn add-session-resource
  "Add a file resource to a session. Resource responses include GitHub
  `:checkout` and memory-store `:access`, `:description`, `:instructions`,
  and `:name` when returned by the SDK."
  [^AnthropicClient client ^String session-id req]
  (with-api-errors (session-resource->map (-> (.beta client) (.sessions) (.resources)
                                               (.add (->session-resource-add-params session-id req))))))

(defn list-session-resources
  "List session resources, including GitHub `:checkout` and all returned
  memory-store fields."
  [^AnthropicClient client ^String session-id opts]
  (with-api-errors (let [^com.anthropic.models.beta.sessions.resources.ResourceListPage p (-> (.beta client) (.sessions) (.resources) (.list (->session-resource-list-params session-id opts)))]
                     (mapv session-resource->map (.autoPager p)))))
(defn get-session-resource
  "Get a session resource, including GitHub `:checkout` and all returned
  memory-store fields."
  [^AnthropicClient client ^String session-id ^String resource-id]
  (with-api-errors (session-resource->map (-> (.beta client) (.sessions) (.resources) (.retrieve (->session-resource-retrieve-params session-id resource-id))))))
(defn update-session-resource
  "Update a session resource and return its complete mapped representation,
  including GitHub `:checkout` and all returned memory-store fields."
  [^AnthropicClient client ^String session-id ^String resource-id changes]
  (with-api-errors (session-resource->map (-> (.beta client) (.sessions) (.resources) (.update (->session-resource-update-params session-id resource-id changes))))))
(defn delete-session-resource [^AnthropicClient client ^String session-id ^String resource-id]
  (with-api-errors (let [^com.anthropic.models.beta.sessions.resources.BetaManagedAgentsDeleteSessionResource r (-> (.beta client) (.sessions) (.resources) (.delete (->session-resource-delete-params session-id resource-id)))] {:id (.id r) :deleted true})))

;; ---- Deployments -----------------------------------------------------------

(defn- ->deployment-create-metadata ^DeploymentCreateParams$Metadata [m]
  (let [b (DeploymentCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->deployment-update-metadata ^DeploymentUpdateParams$Metadata [m]
  (let [b (DeploymentUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->deployment-resource [resource]
  ;; The SDK's resource params reject a null on their required fields, so guard
  ;; each one here: a missing key is the library's `:missing-key` error rather
  ;; than a null pointer thrown from inside the SDK.
  (case (:type resource)
    :file (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams/builder)]
            (when-not (:file-id resource) (missing-key! :file-id))
            (.type b (com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams$Type/of "file"))
            (.fileId b ^String (:file-id resource))
            (when (:mount-path resource) (.mountPath b ^String (:mount-path resource)))
            (.build b))
    :github-repository (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams/builder)]
                         (when-not (:url resource) (missing-key! :url))
                         (when-not (:authorization-token resource) (missing-key! :authorization-token))
                         (.type b (com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams$Type/of "github_repository"))
                         (.url b ^String (:url resource))
                         (.authorizationToken b ^String (:authorization-token resource))
                         (when (:checkout resource)
                           (.checkout b ^com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams$Checkout
                                       (->github-checkout (:checkout resource))))
                         (when (:mount-path resource) (.mountPath b ^String (:mount-path resource)))
                         (.build b))
    :memory-store (let [b (com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam/builder)]
                    (when-not (:memory-store-id resource) (missing-key! :memory-store-id))
                    (.type b (com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam$Type/of "memory_store"))
                    (.memoryStoreId b ^String (:memory-store-id resource))
                    (when (:access resource)
                      (.access b ^com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam$Access
                               (->enum-value (:access resource) #{:read-write :read-only}
                                             (fn [s#] (com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam$Access/of s#)) :access)))
                    (when (:instructions resource) (.instructions b ^String (:instructions resource)))
                    (.build b))
    (throw (ex-info (str "Unknown deployment resource type " (:type resource))
                    {:anthropic/error :unknown-deployment-resource-type :type (:type resource)}))))

(defn- ->deployment-schedule ^com.anthropic.models.beta.deployments.BetaManagedAgentsScheduleParams
  [{:keys [expression timezone type]}]
  (let [b (com.anthropic.models.beta.deployments.BetaManagedAgentsScheduleParams/builder)]
    (when expression (.expression b ^String expression))
    (when timezone (.timezone b ^String timezone))
    (when type (.type b (com.anthropic.models.beta.deployments.BetaManagedAgentsScheduleParams$Type/of (name type))))
    (.build b)))

(defn- ->deployment-create-params ^DeploymentCreateParams
  [{:keys [name agent environment-id initial-events description metadata vault-ids budget resources schedule betas] :as req}]
  (when-not name (missing-key! :name))
  (when-not agent (missing-key! :agent))
  (when-not environment-id (missing-key! :environment-id))
  (when-not (contains? req :initial-events) (missing-key! :initial-events))
  (let [^DeploymentCreateParams$Builder b (DeploymentCreateParams/builder)]
    (.name b ^String name)
    (.agent b ^String agent)
    (.environmentId b ^String environment-id)
    (.initialEvents b ^java.util.List (mapv ->deployment-initial-event initial-events))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->deployment-create-metadata metadata)))
    (when budget (.budget b (->budget budget)))
    (doseq [resource resources]
      (case (:type resource)
        :file (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams (->deployment-resource resource))
        :github-repository (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams (->deployment-resource resource))
        :memory-store (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam (->deployment-resource resource))
        (->deployment-resource resource)))
    (when schedule (.schedule b (->deployment-schedule schedule)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- ->deployment-update-params ^DeploymentUpdateParams
  [deployment-id {:keys [name agent environment-id initial-events description metadata vault-ids budget resources schedule betas]}]
  (let [^DeploymentUpdateParams$Builder b (DeploymentUpdateParams/builder)]
    (.deploymentId b ^String deployment-id)
    (when name (.name b ^String name))
    (when agent (.agent b ^String agent))
    (when environment-id (.environmentId b ^String environment-id))
    (when initial-events
      (.initialEvents b ^java.util.List (mapv ->deployment-initial-event initial-events)))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->deployment-update-metadata metadata)))
    (when budget (.budget b (->budget budget)))
    (doseq [resource resources]
      (case (:type resource)
        :file (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsFileResourceParams (->deployment-resource resource))
        :github-repository (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsGitHubRepositoryResourceParams (->deployment-resource resource))
        :memory-store (.addResource b ^com.anthropic.models.beta.sessions.BetaManagedAgentsMemoryStoreResourceParam (->deployment-resource resource))
        (->deployment-resource resource)))
    (when schedule (.schedule b (->deployment-schedule schedule)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- ->deployment-run-params ^DeploymentRunParams [deployment-id]
  (let [b (DeploymentRunParams/builder)]
    (.deploymentId b ^String deployment-id)
    (.build b)))

(defn- deployment-resource->map
  [^com.anthropic.models.beta.deployments.BetaManagedAgentsSessionResourceConfig r]
  (cond
    (.isFile r)
    (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsFileResourceConfig x (.asFile r)]
      (cond-> {:type :file :file-id (.fileId x)}
        (unopt (.mountPath r)) (assoc :mount-path (unopt (.mountPath r)))))
    (.isGitHubRepository r)
    (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsGitHubRepositoryResourceConfig x (.asGitHubRepository r)]
      (cond-> {:type :github-repository :url (.url x)}
        (unopt (.checkout x)) (assoc :checkout (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsGitHubRepositoryResourceConfig$Checkout c (unopt (.checkout x))]
                                                 (cond
                                                   (.isBranch c) (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsBranchCheckout b (.asBranch c)]
                                                                   {:type :branch :name (.name b)})
                                                   (.isCommit c) (let [^com.anthropic.models.beta.sessions.BetaManagedAgentsCommitCheckout c (.asCommit c)]
                                                                   {:type :commit :sha (.sha c)})
                                                   :else (throw (ex-info "Unsupported checkout type" {:anthropic/error :unknown-checkout-type})))))
        (unopt (.mountPath r)) (assoc :mount-path (unopt (.mountPath r)))))
    :else
    (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsMemoryStoreResourceConfig x (.asMemoryStore r)]
      (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsMemoryStoreResourceConfig$Access access (unopt (.access x))]
        (cond-> {:type :memory-store :memory-store-id (.memoryStoreId x)}
        access (assoc :access (->keyword (.asString access)))
        (unopt (.instructions x)) (assoc :instructions (unopt (.instructions x)))
        (unopt (.mountPath r)) (assoc :mount-path (unopt (.mountPath r))))))))

(defn- deployment-schedule->map [^com.anthropic.models.beta.deployments.BetaManagedAgentsSchedule r]
  (cond-> {:expression (.expression r)
           :timezone (.timezone r)
           :type (keyword (.asString (.type r)))}
    (unopt (.lastRunAt r)) (assoc :last-run-at (str (unopt (.lastRunAt r))))
    (unopt (.upcomingRunsAt r)) (assoc :upcoming-runs-at (mapv str (unopt (.upcomingRunsAt r))))))

(defn- deployment-content->map [^com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentUserMessageEvent$Content c]
  (cond
    (.isText c) (.text ^com.anthropic.models.beta.sessions.events.BetaManagedAgentsTextBlock (.asText c))
    (.isImage c) {:type :image
                  :source (image-source->map (.source (.asImage c)))}
    (.isDocument c) (let [d (.asDocument c)]
                      (cond-> {:type :document
                               :source (document-source->map (.source d))}
                        (unopt (.context d)) (assoc :context (unopt (.context d)))
                        (unopt (.title d)) (assoc :title (unopt (.title d)))))
    (.isRedacted c) {:type :redacted}
    :else {:type :unknown}))

(defn- deployment-initial-event->map [^BetaManagedAgentsDeploymentInitialEventParams r]
  (cond
    (.isUserMessage r)
    (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentUserMessageEvent e (.asUserMessage r)]
      {:type :user-message :content (mapv deployment-content->map (.content e))})
    (.isSystemMessage r)
    (let [^com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentSystemMessageEvent e (.asSystemMessage r)]
      {:type :system-message
       :content (mapv (fn [^com.anthropic.models.beta.sessions.BetaManagedAgentsSystemContentBlock c] (.text c)) (.content e))}
    (.isUserDefineOutcome r) (let [x (.asUserDefineOutcome r)]
                               (cond-> {:type :user-define-outcome :description (.description x)}
                                 (.isText (.rubric x)) (assoc :rubric {:type :text :text (.content (.asText (.rubric x)))})
                                 (.isFile (.rubric x)) (assoc :rubric {:type :file :file-id (.fileId (.asFile (.rubric x)))})
                                 (unopt (.maxIterations x)) (assoc :max-iterations (unopt (.maxIterations x))))))
    :else (throw (ex-info "Unsupported deployment initial event"
                          {:anthropic/error :unsupported-event-type}))))

(defn- additional-properties->map [m]
  (walk/keywordize-keys
   (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)]) m))))

(defn- deployment-metadata->map
  [^com.anthropic.models.beta.deployments.BetaManagedAgentsDeployment$Metadata m]
  (additional-properties->map (._additionalProperties m)))

(defn- deployment->map [^BetaManagedAgentsDeployment r]
  (cond-> {:id (.id r)
           :agent (agent-ref->map (.agent r))
           :environment-id (.environmentId r)
           :name (.name r)
           :status (->keyword (.asString (.status r)))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :vault-ids (vec (.vaultIds r))
           :resources (mapv deployment-resource->map (.resources r))
           :initial-events (mapv deployment-initial-event->map (.initialEvents r))
           :metadata (deployment-metadata->map (.metadata r))
           :type (keyword (.asString (.type r)))}
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (unopt (.pausedReason r)) (assoc :paused-reason (str (unopt (.pausedReason r))))
    (unopt (.budget r)) (assoc :budget (budget->map (unopt (.budget r))))
    (unopt (.schedule r)) (assoc :schedule (deployment-schedule->map (unopt (.schedule r))))))

(defn create-deployment
  "Create a deployment. Required: `:name`, `:agent`, `:environment-id`, and
  `:initial-events` (event maps). Optional: `:description`,
  `:metadata`, `:vault-ids`, `:resources` (including GitHub `:checkout` and
  memory-store `:access` and `:instructions`), `:schedule`, `:budget`, and `:betas`. Budget uses the
  `{:max-list-cost {:amount '...' :currency :usd} :type :limit}` shape.
  Returns the deployment map."
  [^AnthropicClient client req]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments)
                         (.create (->deployment-create-params req))))))

(defn get-deployment
  "Get a deployment by id."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments) (.retrieve deployment-id)))))

(defn list-deployments
  "List deployments with optional `:agent-id`, timestamps, `:include-archived`,
  `:limit`, `:page`, `:status`, and `:betas`."
  ([^AnthropicClient client] (list-deployments client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^DeploymentListPage p (-> (.beta client) (.deployments)
                                     (.list (->deployment-list-params opts)))]
       (mapv deployment->map (.autoPager p))))))

(defn update-deployment
  "Update a deployment. `changes` may include `:name`, `:agent`,
  `:environment-id`, `:initial-events`, `:description`, `:metadata`,
  `:vault-ids`, `:resources` (including GitHub `:checkout` and memory-store
  `:access` and `:instructions`), `:schedule`, `:budget`, or `:betas`. Returns the updated deployment map."
  [^AnthropicClient client ^String deployment-id changes]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments)
                         (.update (->deployment-update-params deployment-id changes))))))

(defn pause-deployment
  "Pause a deployment by id. Returns the deployment map."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments) (.pause deployment-id)))))

(defn unpause-deployment
  "Unpause a deployment by id. Returns the deployment map."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments) (.unpause deployment-id)))))

(defn archive-deployment
  "Archive a deployment by id. Returns the deployment map."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments) (.archive deployment-id)))))

(defn- deployment-run-error->map
  [e]
  (if-not (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error e)
    (throw (ex-info "Unsupported deployment run error"
                    {:anthropic/error :unknown-deployment-run-error-type}))
    (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error e e]
      (cond
    (.isEnvironmentArchived e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError x (.asEnvironmentArchived e)]
                                  {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isAgentArchived e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsAgentArchivedRunError x (.asAgentArchived e)]
                           {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isEnvironmentNotFound e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentNotFoundRunError x (.asEnvironmentNotFound e)]
                                 {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isVaultNotFound e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultNotFoundRunError x (.asVaultNotFound e)]
                          {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isVaultArchived e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultArchivedRunError x (.asVaultArchived e)]
                           {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isFileNotFound e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsFileNotFoundRunError x (.asFileNotFound e)]
                         {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isMemoryStoreArchived e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMemoryStoreArchivedRunError x (.asMemoryStoreArchived e)]
                                 {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isSkillNotFound e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSkillNotFoundRunError x (.asSkillNotFound e)]
                          {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isSessionResourceNotFound e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionResourceNotFoundRunError x (.asSessionResourceNotFound e)]
                                    {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isWorkspaceArchived e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsWorkspaceArchivedRunError x (.asWorkspaceArchived e)]
                               {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isOrganizationDisabled e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsOrganizationDisabledRunError x (.asOrganizationDisabled e)]
                                  {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isSessionRateLimited e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionRateLimitedRunError x (.asSessionRateLimited e)]
                                {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isSessionCreationRejected e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionCreationRejectedRunError x (.asSessionCreationRejected e)]
                                    {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isUnknown e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsUnknownRunError x (.asUnknown e)]
                     {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isSelfHostedResourcesUnsupported e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSelfHostedResourcesUnsupportedRunError x (.asSelfHostedResourcesUnsupported e)]
                                           {:type (->keyword (.asString (.type x))) :message (.message x)})
    (.isMcpEgressBlocked e) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMcpEgressBlockedRunError x (.asMcpEgressBlocked e)]
                              {:type (->keyword (.asString (.type x))) :message (.message x)})
    :else (throw (ex-info "Unsupported deployment run error"
                          {:anthropic/error :unknown-deployment-run-error-type}))))))

(defn- deployment-run-trigger-context->map
  [context]
  (if-not (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerContext context)
    (throw (ex-info "Unsupported deployment run trigger context"
                    {:anthropic/error :unknown-deployment-run-trigger-context-type}))
    (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerContext context context]
      (cond
    (.isManual context) {:type :manual}
    (.isSchedule context) (let [^com.anthropic.models.beta.deploymentruns.BetaManagedAgentsScheduleTriggerContext schedule
                                (.asSchedule context)]
                            {:type :schedule :scheduled-at (str (.scheduledAt schedule))})
    :else (throw (ex-info "Unsupported deployment run trigger context"
                          {:anthropic/error :unknown-deployment-run-trigger-context-type}))))))

(defn- deployment-run->map [^BetaManagedAgentsDeploymentRun r]
  (cond-> {:id (.id r)
           :agent (agent-ref->map (.agent r))
           :deployment-id (.deploymentId r)
           :created-at (str (.createdAt r))
           :type (->keyword (.asString (.type r)))}
    (unopt (.sessionId r)) (assoc :session-id (unopt (.sessionId r)))
    (unopt (.error r)) (assoc :error (deployment-run-error->map (unopt (.error r))))
    (.triggerContext r) (assoc :trigger-context (deployment-run-trigger-context->map (.triggerContext r)))))

(defn run-deployment
  "Run a deployment manually by id. Returns the deployment run map, with nested
  `:error` and `:trigger-context` plain-data maps when present."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment-run->map (-> (.beta client) (.deployments)
                             (.run (->deployment-run-params deployment-id))))))

;; ---- Deployment runs -------------------------------------------------------

(defn get-deployment-run
  "Get a deployment run by id. Returns a deployment run map, with nested
  `:error` and `:trigger-context` plain-data maps when present."
  [^AnthropicClient client ^String deployment-run-id]
  (with-api-errors
    (deployment-run->map (-> (.beta client) (.deploymentRuns)
                             (.retrieve deployment-run-id)))))

(defn list-deployment-runs
  "List deployment runs with optional timestamps, `:deployment-id`,
  `:has-error`, `:limit`, `:page`, `:trigger-type`, and `:betas`. Returns
  deployment run maps, with nested `:error` and `:trigger-context` plain-data
  maps when present."
  ([^AnthropicClient client] (list-deployment-runs client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^DeploymentRunListPage p (-> (.beta client) (.deploymentRuns)
                                        (.list (->deployment-run-list-params opts)))]
       (mapv deployment-run->map (.autoPager p))))))

;; ---- Environments ----------------------------------------------------------

(defn- ->environment-create-metadata ^EnvironmentCreateParams$Metadata [m]
  (let [b (EnvironmentCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->environment-update-metadata ^EnvironmentUpdateParams$Metadata [m]
  (let [b (EnvironmentUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->environment-cloud-config ^com.anthropic.models.beta.environments.BetaCloudConfigParams
  [{:keys [networking packages]}]
  (let [b (com.anthropic.models.beta.environments.BetaCloudConfigParams/builder)]
    (.type b (JsonValue/from "cloud"))
    (when networking
      (case (:type networking)
          :unrestricted (.networking b ^com.anthropic.models.beta.environments.BetaUnrestrictedNetwork
                                      (.build (com.anthropic.models.beta.environments.BetaUnrestrictedNetwork/builder)))
          :limited (let [n (com.anthropic.models.beta.environments.BetaLimitedNetworkParams/builder)]
                     (doseq [[k setter] [[:allow-mcp-servers #(.allowMcpServers n (boolean %))]
                                         [:allow-package-managers #(.allowPackageManagers n (boolean %))]
                                         [:allowed-hosts #(.allowedHosts n ^java.util.List %)]]]
                       (when (contains? networking k) (setter (get networking k))))
                     (.networking b (com.anthropic.models.beta.environments.BetaCloudConfigParams$Networking/ofLimited (.build n))))
          (throw (ex-info (str "Unknown environment networking type " (:type networking))
                          {:anthropic/error :unknown-environment-networking-type :type (:type networking)}))))
    (when packages
      (let [p (com.anthropic.models.beta.environments.BetaPackagesParams/builder)]
        (doseq [[k setter] [[:apt #(.apt p ^java.util.List %)] [:cargo #(.cargo p ^java.util.List %)]
                             [:gem #(.gem p ^java.util.List %)] [:go #(.go p ^java.util.List %)]
                             [:npm #(.npm p ^java.util.List %)] [:pip #(.pip p ^java.util.List %)]]]
          (when (contains? packages k) (setter (get packages k))))
        (.packages b (.build p))))
    (.build b)))

(defn- ->environment-config [config]
  (case (:type config)
    :cloud (->environment-cloud-config config)
    :self-hosted (let [b (com.anthropic.models.beta.environments.BetaSelfHostedConfigParams/builder)]
                   (.type b (JsonValue/from "self_hosted"))
                   (.build b))
    (throw (ex-info (str "Unknown environment config type " (:type config))
                    {:anthropic/error :unknown-environment-config-type :type (:type config)}))))

(defn- environment-networking->map
  [^com.anthropic.models.beta.environments.BetaCloudConfig$Networking n]
  (cond
    (.isUnrestricted n) {:type :unrestricted}
    (.isLimited n) (let [^com.anthropic.models.beta.environments.BetaLimitedNetwork l (.asLimited n)]
                     (cond-> {:type :limited}
                       (.allowMcpServers l) (assoc :allow-mcp-servers (.allowMcpServers l))
                       (.allowPackageManagers l) (assoc :allow-package-managers (.allowPackageManagers l))
                       (.allowedHosts l) (assoc :allowed-hosts (vec (.allowedHosts l)))))
    :else (throw (ex-info "Unsupported environment networking" {:anthropic/error :unknown-environment-networking-type}))))

(defn- environment-packages->map
  [^com.anthropic.models.beta.environments.BetaPackages p]
  (cond-> {}
    (unopt (.type p)) (assoc :type (keyword (.asString ^com.anthropic.models.beta.environments.BetaPackages$Type (unopt (.type p)))))
    (.apt p) (assoc :apt (vec (.apt p)))
    (.cargo p) (assoc :cargo (vec (.cargo p)))
    (.gem p) (assoc :gem (vec (.gem p)))
    (.go p) (assoc :go (vec (.go p)))
    (.npm p) (assoc :npm (vec (.npm p)))
    (.pip p) (assoc :pip (vec (.pip p)))))

(defn- environment-config->map
  [^com.anthropic.models.beta.environments.BetaEnvironment$Config config]
  (cond
    (.isCloud config) (let [^com.anthropic.models.beta.environments.BetaCloudConfig x (.asCloud config)]
                        (cond-> {:type :cloud}
                          (.networking x) (assoc :networking (environment-networking->map (.networking x)))
                          (.packages x) (assoc :packages (environment-packages->map (.packages x)))))
    (.isSelfHosted config) {:type :self-hosted}
    :else (throw (ex-info "Unsupported environment config"
                          {:anthropic/error :unknown-environment-config-type}))))

(defn- environment-metadata->map
  [^com.anthropic.models.beta.environments.BetaEnvironment$Metadata m]
  (additional-properties->map (._additionalProperties m)))

(defn- ->environment-create-params ^EnvironmentCreateParams
  [{:keys [name description metadata config scope betas]}]
  (when-not name (missing-key! :name))
  (let [b (EnvironmentCreateParams/builder)]
    (.name b ^String name)
    (when description (.description b ^String description))
    (when metadata (.metadata b (->environment-create-metadata metadata)))
    (when config
      (case (:type config)
        :cloud (.config b ^com.anthropic.models.beta.environments.BetaCloudConfigParams (->environment-config config))
        :self-hosted (.config b ^com.anthropic.models.beta.environments.BetaSelfHostedConfigParams (->environment-config config))
        (->environment-config config)))
    (when scope (.scope b (com.anthropic.models.beta.environments.EnvironmentCreateParams$Scope/of (clojure.core/name scope))))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- ->environment-update-params ^EnvironmentUpdateParams
  [environment-id {:keys [name description metadata config scope betas]}]
  (let [b (EnvironmentUpdateParams/builder)]
    (.environmentId b ^String environment-id)
    (when name (.name b ^String name))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->environment-update-metadata metadata)))
    (when config
      (case (:type config)
        :cloud (.config b ^com.anthropic.models.beta.environments.BetaCloudConfigParams (->environment-config config))
        :self-hosted (.config b ^com.anthropic.models.beta.environments.BetaSelfHostedConfigParams (->environment-config config))
        (->environment-config config)))
    (when scope (.scope b (com.anthropic.models.beta.environments.EnvironmentUpdateParams$Scope/of (clojure.core/name scope))))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- environment->map [^BetaEnvironment r]
  (cond-> {:id (.id r)
           :name (.name r)
           :description (.description r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :config (environment-config->map (.config r))
           :metadata (environment-metadata->map (.metadata r))}
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (unopt (.scope r)) (assoc :scope (keyword (.asString ^com.anthropic.models.beta.environments.BetaEnvironment$Scope (unopt (.scope r)))))))

(defn- environment-delete->map [^BetaEnvironmentDeleteResponse r]
  {:id (.id r) :deleted true :type (keyword (.asString (.type r)))})

(defn create-environment
  "Create an environment: `:name` (required), `:description`, `:metadata`,
  `:config`, `:scope`, and `:betas`. Returns the environment map."
  [^AnthropicClient client req]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments)
                          (.create (->environment-create-params req))))))

(defn get-environment
  "Get an environment by id."
  [^AnthropicClient client ^String environment-id]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments) (.retrieve environment-id)))))

(defn list-environments
  "List environments with optional `:include-archived`, `:limit`, `:page`,
  and `:betas`."
  ([^AnthropicClient client] (list-environments client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^EnvironmentListPage p (-> (.beta client) (.environments)
                                      (.list (->environment-list-params opts)))]
       (mapv environment->map (.autoPager p))))))

(defn update-environment
  "Update an environment's `:name`, `:description`, `:metadata`, `:config`, or
  `:scope`, or `:betas`."
  [^AnthropicClient client ^String environment-id changes]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments)
                          (.update (->environment-update-params environment-id changes))))))

(defn archive-environment
  "Archive an environment by id. Returns the environment map."
  [^AnthropicClient client ^String environment-id]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments) (.archive environment-id)))))

(defn delete-environment
  "Delete an environment by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String environment-id]
  (with-api-errors
    (environment-delete->map (-> (.beta client) (.environments) (.delete environment-id)))))

;; ---- Environment work -----------------------------------------------------

(defn- ->environment-work-update-metadata
  ^com.anthropic.models.beta.environments.work.BetaSelfHostedWorkUpdateRequest$Metadata
  [m]
  (let [b (com.anthropic.models.beta.environments.work.BetaSelfHostedWorkUpdateRequest$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->environment-work-retrieve-params
  ^com.anthropic.models.beta.environments.work.WorkRetrieveParams
  [environment-id work-id]
  (let [b (com.anthropic.models.beta.environments.work.WorkRetrieveParams/builder)]
    (.environmentId b ^String environment-id)
    (.workId b ^String work-id)
    (.build b)))

(defn- ->environment-work-update-params
  ^com.anthropic.models.beta.environments.work.WorkUpdateParams
  [environment-id work-id {:keys [metadata]}]
  (when-not metadata (missing-key! :metadata))
  (let [b (com.anthropic.models.beta.environments.work.WorkUpdateParams/builder)
        body (com.anthropic.models.beta.environments.work.BetaSelfHostedWorkUpdateRequest/builder)]
    (.environmentId b ^String environment-id)
    (.workId b ^String work-id)
    (.metadata body (->environment-work-update-metadata metadata))
    (.betaSelfHostedWorkUpdateRequest b (.build body))
    (.build b)))

(defn- ->environment-work-list-params
  ^com.anthropic.models.beta.environments.work.WorkListParams
  [environment-id {:keys [limit page]}]
  (let [b (com.anthropic.models.beta.environments.work.WorkListParams/builder)]
    (.environmentId b ^String environment-id)
    (when limit (.limit b (long limit)))
    (when page (.page b ^String page))
    (.build b)))

(defn- ->environment-work-ack-params
  ^com.anthropic.models.beta.environments.work.WorkAckParams
  [environment-id work-id]
  (let [b (com.anthropic.models.beta.environments.work.WorkAckParams/builder)]
    (.environmentId b ^String environment-id)
    (.workId b ^String work-id)
    (.build b)))

(defn- ->environment-work-heartbeat-params
  ^com.anthropic.models.beta.environments.work.WorkHeartbeatParams
  (^com.anthropic.models.beta.environments.work.WorkHeartbeatParams [environment-id work-id]
   (->environment-work-heartbeat-params environment-id work-id {}))
  (^com.anthropic.models.beta.environments.work.WorkHeartbeatParams
   [environment-id work-id {:keys [desired-ttl-seconds expected-last-heartbeat]}]
  (let [b (com.anthropic.models.beta.environments.work.WorkHeartbeatParams/builder)]
    (.environmentId b ^String environment-id)
    (.workId b ^String work-id)
    (when desired-ttl-seconds (.desiredTtlSeconds b (long desired-ttl-seconds)))
    (when expected-last-heartbeat (.expectedLastHeartbeat b ^String expected-last-heartbeat))
    (.build b))))

(defn- ->environment-work-poll-params
  ^com.anthropic.models.beta.environments.work.WorkPollParams
  (^com.anthropic.models.beta.environments.work.WorkPollParams [environment-id]
   (->environment-work-poll-params environment-id {}))
  (^com.anthropic.models.beta.environments.work.WorkPollParams
   [environment-id {:keys [block-ms reclaim-older-than-ms anthropic-worker-id]}]
  (let [b (com.anthropic.models.beta.environments.work.WorkPollParams/builder)]
    (.environmentId b ^String environment-id)
    (when block-ms (.blockMs b (long block-ms)))
    (when reclaim-older-than-ms (.reclaimOlderThanMs b (long reclaim-older-than-ms)))
    (when anthropic-worker-id (.anthropicWorkerId b ^String anthropic-worker-id))
    (.build b))))

(defn- ->environment-work-stats-params
  ^com.anthropic.models.beta.environments.work.WorkStatsParams
  [environment-id]
  (let [b (com.anthropic.models.beta.environments.work.WorkStatsParams/builder)]
    (.environmentId b ^String environment-id)
    (.build b)))

(defn- ->environment-work-stop-params
  ^com.anthropic.models.beta.environments.work.WorkStopParams
  [environment-id work-id {:keys [force]}]
  (let [b (com.anthropic.models.beta.environments.work.WorkStopParams/builder)
        body (com.anthropic.models.beta.environments.work.BetaSelfHostedWorkStopRequest/builder)]
    (.environmentId b ^String environment-id)
    (.workId b ^String work-id)
    (when (some? force) (.force body (boolean force)))
    (.betaSelfHostedWorkStopRequest b (.build body))
    (.build b)))

(defn- environment-work-metadata->map
  [^com.anthropic.models.beta.environments.work.BetaSelfHostedWork$Metadata m]
  (walk/keywordize-keys
   (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)]))
         (._additionalProperties m))))

(defn- environment-work->map
  [^com.anthropic.models.beta.environments.work.BetaSelfHostedWork r]
  (cond-> {:id (.id r)
           :created-at (.createdAt r)
           :data {:id (.id (.data r))}
           :environment-id (.environmentId r)
           :metadata (environment-work-metadata->map (.metadata r))
           :state (->keyword (.asString (.state r)))}
    (unopt (.acknowledgedAt r)) (assoc :acknowledged-at (unopt (.acknowledgedAt r)))
    (unopt (.latestHeartbeatAt r)) (assoc :latest-heartbeat-at (unopt (.latestHeartbeatAt r)))
    (unopt (.secret r)) (assoc :secret (unopt (.secret r)))
    (unopt (.startedAt r)) (assoc :started-at (unopt (.startedAt r)))
    (unopt (.stopRequestedAt r)) (assoc :stop-requested-at (unopt (.stopRequestedAt r)))
    (unopt (.stoppedAt r)) (assoc :stopped-at (unopt (.stoppedAt r)))))

(defn- environment-work-heartbeat->map
  [^com.anthropic.models.beta.environments.work.BetaSelfHostedWorkHeartbeatResponse r]
  {:last-heartbeat (.lastHeartbeat r)
   :lease-extended (.leaseExtended r)
   :state (->keyword (.asString (.state r)))
   :ttl-seconds (.ttlSeconds r)})

(defn- environment-work-stats->map
  [^com.anthropic.models.beta.environments.work.BetaSelfHostedWorkQueueStats r]
  (cond-> {:depth (.depth r) :pending (.pending r)}
    (unopt (.oldestQueuedAt r)) (assoc :oldest-queued-at (unopt (.oldestQueuedAt r)))
    (unopt (.workersPolling r)) (assoc :workers-polling (unopt (.workersPolling r)))))

(defn- environment-work-optional->map [^Optional r]
  (when (.isPresent r) (environment-work->map (.get r))))

(defn get-environment-work [^AnthropicClient client ^String environment-id ^String work-id]
  (with-api-errors
    (environment-work->map (-> (.beta client) (.environments) (.work)
                             (.retrieve (->environment-work-retrieve-params environment-id work-id))))))

(defn update-environment-work [^AnthropicClient client ^String environment-id ^String work-id changes]
  (with-api-errors
    (environment-work->map (-> (.beta client) (.environments) (.work)
                             (.update (->environment-work-update-params environment-id work-id changes))))))

(defn list-environment-work [^AnthropicClient client ^String environment-id opts]
  (with-api-errors
    (let [^com.anthropic.models.beta.environments.work.WorkListPage p
          (-> (.beta client) (.environments) (.work)
              (.list (->environment-work-list-params environment-id opts)))]
      (mapv environment-work->map (.autoPager p)))))

(defn ack-environment-work [^AnthropicClient client ^String environment-id ^String work-id]
  (with-api-errors
    (environment-work->map (-> (.beta client) (.environments) (.work)
                             (.ack (->environment-work-ack-params environment-id work-id))))))

(defn heartbeat-environment-work
  ([^AnthropicClient client ^String environment-id ^String work-id]
   (heartbeat-environment-work client environment-id work-id {}))
  ([^AnthropicClient client ^String environment-id ^String work-id opts]
   (with-api-errors
     (environment-work-heartbeat->map (-> (.beta client) (.environments) (.work)
                                        (.heartbeat (->environment-work-heartbeat-params environment-id work-id opts)))))))

(defn poll-environment-work
  ([^AnthropicClient client ^String environment-id]
   (poll-environment-work client environment-id {}))
  ([^AnthropicClient client ^String environment-id opts]
   (with-api-errors
     (environment-work-optional->map (-> (.beta client) (.environments) (.work)
                                         (.poll (->environment-work-poll-params environment-id opts)))))))

(defn environment-work-stats [^AnthropicClient client ^String environment-id]
  (with-api-errors
    (environment-work-stats->map (-> (.beta client) (.environments) (.work)
                                     (.stats (->environment-work-stats-params environment-id))))))

(defn stop-environment-work [^AnthropicClient client ^String environment-id ^String work-id opts]
  (with-api-errors
    (environment-work->map (-> (.beta client) (.environments) (.work)
                             (.stop (->environment-work-stop-params environment-id work-id opts))))))

;; ---- Vaults ---------------------------------------------------------------

(defn- ->vault-create-metadata ^VaultCreateParams$Metadata [m]
  (let [b (VaultCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->vault-update-metadata ^VaultUpdateParams$Metadata [m]
  (let [b (VaultUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->vault-create-params ^VaultCreateParams
  [{:keys [display-name metadata betas]}]
  (when-not display-name (missing-key! :display-name))
  (let [b (VaultCreateParams/builder)]
    (.displayName b ^String display-name)
    (when metadata (.metadata b (->vault-create-metadata metadata)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- ->vault-update-params ^VaultUpdateParams
  [vault-id {:keys [display-name metadata betas]}]
  (let [b (VaultUpdateParams/builder)]
    (.vaultId b ^String vault-id)
    (when display-name (.displayName b ^String display-name))
    (when metadata (.metadata b (->vault-update-metadata metadata)))
    (doseq [beta betas]
      (let [^String beta-name (if (keyword? beta) (clojure.core/name beta) beta)]
        (.addBeta b beta-name)))
    (.build b)))

(defn- vault->map [^BetaManagedAgentsVault r]
  (cond-> {:id (.id r)
           :display-name (.displayName r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :metadata (additional-properties->map (._additionalProperties ^com.anthropic.models.beta.vaults.BetaManagedAgentsVault$Metadata (.metadata r)))
           :type (keyword (.asString (.type r)))}
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn- vault-delete->map [^BetaManagedAgentsDeletedVault r]
  {:id (.id r) :deleted true :type (keyword (.asString (.type r)))})

(defn create-vault
  "Create a vault: `:display-name` (required), `:metadata`, and `:betas`. Credentials are
  not wrapped yet. Returns the vault map."
  [^AnthropicClient client req]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults) (.create (->vault-create-params req))))))

(defn get-vault
  "Get a vault by id."
  [^AnthropicClient client ^String vault-id]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults) (.retrieve vault-id)))))

(defn list-vaults
  "List vaults with optional `:include-archived`, `:limit`, `:page`, and
  `:betas`."
  ([^AnthropicClient client] (list-vaults client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^VaultListPage p (-> (.beta client) (.vaults)
                                (.list (->vault-list-params opts)))]
       (mapv vault->map (.autoPager p))))))

(defn update-vault
  "Update a vault's `:display-name`, `:metadata`, or `:betas`."
  [^AnthropicClient client ^String vault-id changes]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults)
                    (.update (->vault-update-params vault-id changes))))))

(defn archive-vault
  "Archive a vault by id. Returns the vault map."
  [^AnthropicClient client ^String vault-id]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults) (.archive vault-id)))))

(defn delete-vault
  "Delete a vault by id. Returns `{:id ... :deleted true :type ...}`."
  [^AnthropicClient client ^String vault-id]
  (with-api-errors
    (vault-delete->map (-> (.beta client) (.vaults) (.delete vault-id)))))

;; ---- Tunnels --------------------------------------------------------------

(defn- ->tunnel-create-params ^com.anthropic.models.beta.tunnels.TunnelCreateParams
  [{:keys [display-name]}]
  (let [b (com.anthropic.models.beta.tunnels.TunnelCreateParams/builder)]
    (when display-name (.displayName b ^String display-name))
    (.build b)))

(defn- tunnel->map [^com.anthropic.models.beta.tunnels.BetaTunnel r]
  (cond-> {:id (.id r) :domain (.domain r) :created-at (str (.createdAt r))
           :type (keyword (json-string (._type r)))}
    (unopt (.displayName r)) (assoc :display-name (unopt (.displayName r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn create-tunnel [^AnthropicClient client req]
  (with-api-errors (tunnel->map (-> (.beta client) (.tunnels)
                                    (.create (->tunnel-create-params req))))))

(defn get-tunnel [^AnthropicClient client ^String tunnel-id]
  (with-api-errors (tunnel->map (-> (.beta client) (.tunnels) (.retrieve tunnel-id)))))

(defn list-tunnels
  "List tunnels with optional `:include-archived`, `:limit`, `:page`, and
  `:betas`."
  ([^AnthropicClient client] (list-tunnels client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^com.anthropic.models.beta.tunnels.TunnelListPage p
           (-> (.beta client) (.tunnels) (.list (->tunnel-list-params opts)))]
       (mapv tunnel->map (.autoPager p))))))

(defn archive-tunnel [^AnthropicClient client ^String tunnel-id]
  (with-api-errors (tunnel->map (-> (.beta client) (.tunnels) (.archive tunnel-id)))))

(defn- ->tunnel-reveal-token-params ^com.anthropic.models.beta.tunnels.TunnelRevealTokenParams
  [tunnel-id]
  (let [b (com.anthropic.models.beta.tunnels.TunnelRevealTokenParams/builder)]
    (.tunnelId b ^String tunnel-id)
    (.build b)))

(defn- ->tunnel-rotate-token-params ^com.anthropic.models.beta.tunnels.TunnelRotateTokenParams
  [tunnel-id {:keys [reason]}]
  (let [b (com.anthropic.models.beta.tunnels.TunnelRotateTokenParams/builder)]
    (.tunnelId b ^String tunnel-id)
    (when reason (.reason b ^String reason))
    (.build b)))

(defn- tunnel-token->map [^com.anthropic.models.beta.tunnels.BetaTunnelToken r]
  {:id (.id r) :tunnel-token (.tunnelToken r)})

(defn reveal-tunnel-token [^AnthropicClient client ^String tunnel-id]
  (with-api-errors (tunnel-token->map (-> (.beta client) (.tunnels)
                                           (.revealToken (->tunnel-reveal-token-params tunnel-id))))))

(defn rotate-tunnel-token
  ([^AnthropicClient client ^String tunnel-id]
   (rotate-tunnel-token client tunnel-id {}))
  ([^AnthropicClient client ^String tunnel-id opts]
   (with-api-errors (tunnel-token->map (-> (.beta client) (.tunnels)
                                            (.rotateToken (->tunnel-rotate-token-params tunnel-id opts)))))))

;; ---- Tunnel certificates --------------------------------------------------

(defn- ->tunnel-certificate-create-params
  ^com.anthropic.models.beta.tunnels.certificates.CertificateCreateParams
  [tunnel-id {:keys [ca-certificate-pem]}]
  (when-not ca-certificate-pem (missing-key! :ca-certificate-pem))
  (let [b (com.anthropic.models.beta.tunnels.certificates.CertificateCreateParams/builder)]
    (.tunnelId b ^String tunnel-id)
    (.caCertificatePem b ^String ca-certificate-pem)
    (.build b)))

(defn- tunnel-certificate->map
  [^com.anthropic.models.beta.tunnels.certificates.BetaTunnelCertificate r]
  (cond-> {:id (.id r) :tunnel-id (.tunnelId r) :fingerprint (.fingerprint r)
           :created-at (str (.createdAt r))}
    (unopt (.expiresAt r)) (assoc :expires-at (str (unopt (.expiresAt r))))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn create-tunnel-certificate [^AnthropicClient client ^String tunnel-id req]
  (with-api-errors
    (tunnel-certificate->map (-> (.beta client) (.tunnels) (.certificates)
                                 (.create (->tunnel-certificate-create-params tunnel-id req))))))

(defn get-tunnel-certificate [^AnthropicClient client ^String tunnel-id ^String certificate-id]
  (with-api-errors
    (tunnel-certificate->map (-> (.beta client) (.tunnels) (.certificates)
                                 (.retrieve certificate-id)))) )

(defn list-tunnel-certificates
  "List tunnel certificates with optional `:include-archived`, `:limit`,
  `:page`, and `:betas`."
  ([^AnthropicClient client ^String tunnel-id]
   (list-tunnel-certificates client tunnel-id {}))
  ([^AnthropicClient client ^String tunnel-id opts]
   (with-api-errors
     (let [^com.anthropic.models.beta.tunnels.certificates.CertificateListPage p
           (-> (.beta client) (.tunnels) (.certificates)
               (.list (->certificate-list-params tunnel-id opts)))]
       (mapv tunnel-certificate->map (.autoPager p))))))

(defn archive-tunnel-certificate [^AnthropicClient client ^String tunnel-id ^String certificate-id]
  (with-api-errors
    (tunnel-certificate->map (-> (.beta client) (.tunnels) (.certificates)
                                 (.archive certificate-id)))))

;; ---- Agent versions -------------------------------------------------------

(defn- ->agent-version-list-params
  ^com.anthropic.models.beta.agents.versions.VersionListParams
  [agent-id {:keys [limit page]}]
  (let [b (com.anthropic.models.beta.agents.versions.VersionListParams/builder)]
    (.agentId b ^String agent-id)
    (when limit (.limit b (int limit)))
    (when page (.page b ^String page))
    (.build b)))

(defn list-agent-versions
  "List an agent's versions (pages followed) as agent maps."
  [^AnthropicClient client ^String agent-id opts]
  (with-api-errors
    (let [^com.anthropic.models.beta.agents.versions.VersionListPage p
          (-> (.beta client) (.agents) (.versions)
              (.list (->agent-version-list-params agent-id opts)))]
      (mapv agent->map (.autoPager p)))))

;; ---- Dreams ---------------------------------------------------------------

(defn- ->dream-create-params ^com.anthropic.models.beta.dreams.DreamCreateParams
  [{:keys [inputs model instructions]}]
  (when-not (contains? #{nil} inputs) (when-not (sequential? inputs) (missing-key! :inputs)))
  (when-not model (missing-key! :model))
  (let [b (com.anthropic.models.beta.dreams.DreamCreateParams/builder)
        ^com.anthropic.models.beta.dreams.DreamCreateParams$Model model*
        (if (string? model)
          (com.anthropic.models.beta.dreams.DreamCreateParams$Model/ofString ^String model)
          model)]
    (.inputs b ^java.util.List (vec inputs))
    (.model b model*)
    (when instructions (.instructions b ^String instructions)) (.build b)))

(defn- dream->map [^com.anthropic.models.beta.dreams.BetaDream r]
  (cond-> {:id (.id r) :status (->keyword (.asString (.status r))) :created-at (str (.createdAt r))
           :inputs (vec (.inputs r)) :outputs (vec (.outputs r))
           :type (->keyword (.asString (.type r)))
           :model {:id (.id (.model r))}
           :usage {:cache-creation-input-tokens (.cacheCreationInputTokens (.usage r))
                   :cache-read-input-tokens (.cacheReadInputTokens (.usage r))
                   :input-tokens (.inputTokens (.usage r))
                   :output-tokens (.outputTokens (.usage r))}}
    (unopt (.instructions r)) (assoc :instructions (unopt (.instructions r)))
    (unopt (.sessionId r)) (assoc :session-id (unopt (.sessionId r)))
    (unopt (.endedAt r)) (assoc :ended-at (str (unopt (.endedAt r))))
    (unopt (.error r)) (assoc :error (let [^com.anthropic.models.beta.dreams.BetaDreamError e (unopt (.error r))]
                                      {:type (->keyword (.type e)) :message (.message e)}))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))
(defn create-dream [^AnthropicClient client req] (with-api-errors (dream->map (-> (.beta client) (.dreams) (.create (->dream-create-params req))))))
(defn get-dream [^AnthropicClient client ^String dream-id] (with-api-errors (dream->map (-> (.beta client) (.dreams) (.retrieve dream-id)))))
(defn list-dreams
  "List dreams with optional timestamp filters, `:include-archived`, `:limit`,
  `:page`, `:statuses`, and `:betas`."
  ([^AnthropicClient client] (list-dreams client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^com.anthropic.models.beta.dreams.DreamListPage p
           (-> (.beta client) (.dreams) (.list (->dream-list-params opts)))]
       (mapv dream->map (.autoPager p))))))
(defn archive-dream [^AnthropicClient client ^String dream-id] (with-api-errors (dream->map (-> (.beta client) (.dreams) (.archive dream-id)))))
(defn cancel-dream [^AnthropicClient client ^String dream-id] (with-api-errors (dream->map (-> (.beta client) (.dreams) (.cancel dream-id)))))

;; ---- Vault credentials ----------------------------------------------------

(defn- credential->map [^com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredential r]
  (cond-> {:id (.id r) :vault-id (.vaultId r) :created-at (str (.createdAt r)) :updated-at (str (.updatedAt r))
           :type (keyword (.asString (.type r)))
           :metadata (additional-properties->map (._additionalProperties (.metadata r)))
           :auth (cond
                   (.isMcpOAuth (.auth r)) {:type :mcp-oauth :mcp-server-url (.mcpServerUrl (.asMcpOAuth (.auth r)))}
                   (.isStaticBearer (.auth r)) {:type :static-bearer :mcp-server-url (.mcpServerUrl (.asStaticBearer (.auth r)))}
                   :else {:type :environment-variable :secret-name (.secretName (.asEnvironmentVariable (.auth r)))})}
    (unopt (.displayName r)) (assoc :display-name (unopt (.displayName r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))
(defn- ->credential-create-params [vault-id {:keys [auth display-name]}]
  (when-not auth (missing-key! :auth))
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialCreateParams/builder)
        ^com.anthropic.models.beta.vaults.credentials.CredentialCreateParams$Auth auth auth]
    (.vaultId b ^String vault-id) (.auth b auth) (when display-name (.displayName b ^String display-name)) (.build b)))
(defn- ->credential-retrieve-params [vault-id credential-id]
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialRetrieveParams/builder)] (.vaultId b ^String vault-id) (.credentialId b ^String credential-id) (.build b)))
(defn- ->credential-archive-params [vault-id credential-id]
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialArchiveParams/builder)] (.vaultId b ^String vault-id) (.credentialId b ^String credential-id) (.build b)))
(defn- ->credential-delete-params [vault-id credential-id]
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialDeleteParams/builder)] (.vaultId b ^String vault-id) (.credentialId b ^String credential-id) (.build b)))
(defn- ->credential-update-params [vault-id credential-id {:keys [display-name]}]
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialUpdateParams/builder)] (.vaultId b ^String vault-id) (.credentialId b ^String credential-id) (when display-name (.displayName b ^String display-name)) (.build b)))
(defn- ->credential-mcp-oauth-validate-params [vault-id credential-id]
  (let [b (com.anthropic.models.beta.vaults.credentials.CredentialMcpOAuthValidateParams/builder)]
    (.vaultId b ^String vault-id) (.credentialId b ^String credential-id) (.build b)))
(defn- refresh-http-response->map
  [^com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsRefreshHttpResponse h]
  {:body (.body h)
   :body-truncated (.bodyTruncated h)
   :content-type (.contentType h)
   :status-code (.statusCode h)})

(defn- credential-validation->map [^com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation r]
  (let [^com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsMcpProbe probe (unopt (.mcpProbe r))
        ^com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsRefreshObject refresh (unopt (.refresh r))]
    (cond-> {:credential-id (.credentialId r) :vault-id (.vaultId r)
             :has-refresh-token (.hasRefreshToken r) :status (keyword (str (.status r)))
             :type (->keyword (.asString (.type r)))
             :validated-at (str (.validatedAt r))}
      probe (assoc :mcp-probe
                   (cond-> {:method (.method probe)}
                     (unopt (.httpResponse probe))
                     (assoc :http-response (refresh-http-response->map
                                            (unopt (.httpResponse probe))))))
      refresh (assoc :refresh
                     (cond-> {:status (keyword (str (.status refresh)))}
                       (unopt (.httpResponse refresh))
                       (assoc :http-response (refresh-http-response->map
                                              (unopt (.httpResponse refresh)))))))))
(defn create-vault-credential [^AnthropicClient client ^String vault-id req] (with-api-errors (credential->map (-> (.beta client) (.vaults) (.credentials) (.create (->credential-create-params vault-id req))))))
(defn get-vault-credential [^AnthropicClient client ^String vault-id ^String credential-id] (with-api-errors (credential->map (-> (.beta client) (.vaults) (.credentials) (.retrieve (->credential-retrieve-params vault-id credential-id))))))
(defn list-vault-credentials
  "List vault credentials with optional `:include-archived`, `:limit`, `:page`,
  and `:betas`."
  ([^AnthropicClient client ^String vault-id]
   (list-vault-credentials client vault-id {}))
  ([^AnthropicClient client ^String vault-id opts]
   (with-api-errors
     (let [^com.anthropic.models.beta.vaults.credentials.CredentialListPage p
           (-> (.beta client) (.vaults) (.credentials)
               (.list (->credential-list-params vault-id opts)))]
       (mapv credential->map (.autoPager p))))))
(defn update-vault-credential [^AnthropicClient client ^String vault-id ^String credential-id changes] (with-api-errors (credential->map (-> (.beta client) (.vaults) (.credentials) (.update (->credential-update-params vault-id credential-id changes))))))
(defn archive-vault-credential [^AnthropicClient client ^String vault-id ^String credential-id] (with-api-errors (credential->map (-> (.beta client) (.vaults) (.credentials) (.archive (->credential-archive-params vault-id credential-id))))))
(defn delete-vault-credential [^AnthropicClient client ^String vault-id ^String credential-id] (with-api-errors (let [r (-> (.beta client) (.vaults) (.credentials) (.delete (->credential-delete-params vault-id credential-id)))] {:id (.id r) :deleted true :type (keyword (.asString (.type r)))})))
(defn mcp-oauth-validate-vault-credential [^AnthropicClient client ^String vault-id ^String credential-id]
  (with-api-errors (credential-validation->map (-> (.beta client) (.vaults) (.credentials)
                                                     (.mcpOAuthValidate (->credential-mcp-oauth-validate-params vault-id credential-id))))))

;; ---- User profiles ---------------------------------------------------------

(defn- ->user-profile-create-metadata ^UserProfileCreateParams$Metadata [m]
  (let [b (UserProfileCreateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->user-profile-update-metadata ^UserProfileUpdateParams$Metadata [m]
  (let [b (UserProfileUpdateParams$Metadata/builder)]
    (doseq [[k v] (walk/stringify-keys m)]
      (.putAdditionalProperty b ^String k (JsonValue/from v)))
    (.build b)))

(defn- ->user-profile-create-params ^UserProfileCreateParams
  [{:keys [name external-id metadata relationship]}]
  (let [b (UserProfileCreateParams/builder)]
    (when name (.name b ^String name))
    (when external-id (.externalId b ^String external-id))
    (when metadata (.metadata b (->user-profile-create-metadata metadata)))
    (when relationship (.relationship b ^UserProfileCreateParams$Relationship
                                      (->enum-value relationship #{:external :resold :internal}
                                                    (fn [s#] (UserProfileCreateParams$Relationship/of s#)) :relationship)))
    (.build b)))

(defn- ->user-profile-update-params ^UserProfileUpdateParams
  [user-profile-id {:keys [name external-id metadata relationship]}]
  (let [b (UserProfileUpdateParams/builder)]
    (.userProfileId b ^String user-profile-id)
    (when name (.name b ^String name))
    (when external-id (.externalId b ^String external-id))
    (when metadata (.metadata b (->user-profile-update-metadata metadata)))
    (when relationship (.relationship b ^UserProfileUpdateParams$Relationship
                                      (->enum-value relationship #{:external :resold :internal}
                                                    (fn [s#] (UserProfileUpdateParams$Relationship/of s#)) :relationship)))
    (.build b)))

(defn- ->user-profile-enrollment-url-params ^UserProfileCreateEnrollmentUrlParams
  [user-profile-id]
  (let [b (UserProfileCreateEnrollmentUrlParams/builder)]
    (.userProfileId b ^String user-profile-id)
    (.build b)))

(defn- user-profile->map [^BetaUserProfile r]
  (cond-> {:id (.id r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :metadata (additional-properties->map (._additionalProperties (.metadata r)))
           :type (keyword (.asString (.type r)))}
    (unopt (.name r)) (assoc :name (unopt (.name r)))
    (unopt (.externalId r)) (assoc :external-id (unopt (.externalId r)))
    (.relationship r) (assoc :relationship (->keyword (.asString (.relationship r))))
    (.trustGrants r) (assoc :trust-grants
                             (additional-properties->map
                              (._additionalProperties ^com.anthropic.models.beta.userprofiles.BetaUserProfile$TrustGrants
                                                       (.trustGrants r))))))

(defn- enrollment-url->map [^BetaUserProfileEnrollmentUrl r]
  {:url (.url r)
   :expires-at (str (.expiresAt r))})

(defn create-user-profile
  "Create a user profile with optional `:name`, `:external-id`, `:metadata`,
  and `:relationship` (`:external`, `:resold`, or `:internal`). Returns the
  profile map, including `:relationship` and `:trust-grants`."
  [^AnthropicClient client req]
  (with-api-errors
    (user-profile->map (-> (.beta client) (.userProfiles)
                           (.create (->user-profile-create-params req))))))

(defn get-user-profile
  "Get a user profile by id."
  [^AnthropicClient client ^String user-profile-id]
  (with-api-errors
    (user-profile->map (-> (.beta client) (.userProfiles) (.retrieve user-profile-id)))))

(defn list-user-profiles
  "List user profiles with optional `:limit`, `:order`, `:page`, and `:betas`."
  ([^AnthropicClient client] (list-user-profiles client {}))
  ([^AnthropicClient client opts]
   (with-api-errors
     (let [^UserProfileListPage p (-> (.beta client) (.userProfiles)
                                      (.list (->user-profile-list-params opts)))]
       (mapv user-profile->map (.autoPager p))))))

(defn update-user-profile
  "Update a user profile's `:name`, `:external-id`, `:metadata`, or
  `:relationship` (`:external`, `:resold`, or `:internal`)."
  [^AnthropicClient client ^String user-profile-id changes]
  (with-api-errors
    (user-profile->map (-> (.beta client) (.userProfiles)
                           (.update (->user-profile-update-params user-profile-id changes))))))

(defn create-enrollment-url
  "Create an enrollment URL for a user profile. Returns `{:url ...
  :expires-at ...}`."
  [^AnthropicClient client ^String user-profile-id]
  (with-api-errors
    (enrollment-url->map (-> (.beta client) (.userProfiles)
                             (.createEnrollmentUrl
                              (->user-profile-enrollment-url-params user-profile-id))))))

;; ---- Webhooks --------------------------------------------------------------

(defn- webhook-common->map [type data-id organization-id workspace-id]
  {:type type
   :data-id data-id
   :organization-id organization-id
   :workspace-id workspace-id})

(defn- webhook-session-created->map [^BetaWebhookSessionCreatedEventData d]
  (webhook-common->map :session-created (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-session-budget-reached->map
  [^BetaWebhookSessionBudgetReachedEventData d]
  (webhook-common->map :session-budget-reached (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-created->map [^BetaWebhookDeploymentCreatedEventData d]
  (webhook-common->map :deployment-created (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-updated->map [^BetaWebhookDeploymentUpdatedEventData d]
  (webhook-common->map :deployment-updated (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-paused->map [^BetaWebhookDeploymentPausedEventData d]
  (webhook-common->map :deployment-paused (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-unpaused->map [^BetaWebhookDeploymentUnpausedEventData d]
  (webhook-common->map :deployment-unpaused (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-archived->map [^BetaWebhookDeploymentArchivedEventData d]
  (webhook-common->map :deployment-archived (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-deleted->map [^BetaWebhookDeploymentDeletedEventData d]
  (webhook-common->map :deployment-deleted (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-run-started->map [^BetaWebhookDeploymentRunStartedEventData d]
  (webhook-common->map :deployment-run-started (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-run-succeeded->map [^BetaWebhookDeploymentRunSucceededEventData d]
  (webhook-common->map :deployment-run-succeeded (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-deployment-run-failed->map [^BetaWebhookDeploymentRunFailedEventData d]
  (webhook-common->map :deployment-run-failed (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-environment-created->map [^BetaWebhookEnvironmentCreatedEventData d]
  (webhook-common->map :environment-created (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-environment-updated->map [^BetaWebhookEnvironmentUpdatedEventData d]
  (webhook-common->map :environment-updated (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-environment-archived->map [^BetaWebhookEnvironmentArchivedEventData d]
  (webhook-common->map :environment-archived (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-environment-deleted->map [^BetaWebhookEnvironmentDeletedEventData d]
  (webhook-common->map :environment-deleted (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-memory-store-created->map [^BetaWebhookMemoryStoreCreatedEventData d]
  (webhook-common->map :memory-store-created (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-memory-store-archived->map [^BetaWebhookMemoryStoreArchivedEventData d]
  (webhook-common->map :memory-store-archived (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-memory-store-deleted->map [^BetaWebhookMemoryStoreDeletedEventData d]
  (webhook-common->map :memory-store-deleted (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-generic->map
  [^BetaWebhookSessionCreatedEventData d type]
  (webhook-common->map type (.id d) (.organizationId d) (.workspaceId d)))

(defn- webhook-data->map [^BetaWebhookEventData d]
  (cond
    (.isSessionCreated d) (webhook-session-created->map (.asSessionCreated d))
    (.isSessionBudgetReached d) (webhook-session-budget-reached->map (.asSessionBudgetReached d))
    (.isDeploymentCreated d) (webhook-deployment-created->map (.asDeploymentCreated d))
    (.isDeploymentUpdated d) (webhook-deployment-updated->map (.asDeploymentUpdated d))
    (.isDeploymentPaused d) (webhook-deployment-paused->map (.asDeploymentPaused d))
    (.isDeploymentUnpaused d) (webhook-deployment-unpaused->map (.asDeploymentUnpaused d))
    (.isDeploymentArchived d) (webhook-deployment-archived->map (.asDeploymentArchived d))
    (.isDeploymentDeleted d) (webhook-deployment-deleted->map (.asDeploymentDeleted d))
    (.isDeploymentRunStarted d) (webhook-deployment-run-started->map (.asDeploymentRunStarted d))
    (.isDeploymentRunSucceeded d) (webhook-deployment-run-succeeded->map (.asDeploymentRunSucceeded d))
    (.isDeploymentRunFailed d) (webhook-deployment-run-failed->map (.asDeploymentRunFailed d))
    (.isEnvironmentCreated d) (webhook-environment-created->map (.asEnvironmentCreated d))
    (.isEnvironmentUpdated d) (webhook-environment-updated->map (.asEnvironmentUpdated d))
    (.isEnvironmentArchived d) (webhook-environment-archived->map (.asEnvironmentArchived d))
    (.isEnvironmentDeleted d) (webhook-environment-deleted->map (.asEnvironmentDeleted d))
    (.isMemoryStoreCreated d) (webhook-memory-store-created->map (.asMemoryStoreCreated d))
    (.isMemoryStoreArchived d) (webhook-memory-store-archived->map (.asMemoryStoreArchived d))
    (.isMemoryStoreDeleted d) (webhook-memory-store-deleted->map (.asMemoryStoreDeleted d))
    (.isSessionPending d) (webhook-generic->map (.asSessionPending d) :session-pending)
    (.isSessionRunning d) (webhook-generic->map (.asSessionRunning d) :session-running)
    (.isSessionIdled d) (webhook-generic->map (.asSessionIdled d) :session-idled)
    (.isSessionRequiresAction d) (webhook-generic->map (.asSessionRequiresAction d) :session-requires-action)
    (.isSessionArchived d) (webhook-generic->map (.asSessionArchived d) :session-archived)
    (.isSessionDeleted d) (webhook-generic->map (.asSessionDeleted d) :session-deleted)
    (.isSessionStatusRescheduled d) (webhook-generic->map (.asSessionStatusRescheduled d) :session-status-rescheduled)
    (.isSessionStatusRunStarted d) (webhook-generic->map (.asSessionStatusRunStarted d) :session-status-run-started)
    (.isSessionStatusIdled d) (webhook-generic->map (.asSessionStatusIdled d) :session-status-idled)
    (.isSessionStatusTerminated d) (webhook-generic->map (.asSessionStatusTerminated d) :session-status-terminated)
    (.isSessionThreadCreated d) (webhook-generic->map (.asSessionThreadCreated d) :session-thread-created)
    (.isSessionThreadIdled d) (webhook-generic->map (.asSessionThreadIdled d) :session-thread-idled)
    (.isSessionThreadTerminated d) (webhook-generic->map (.asSessionThreadTerminated d) :session-thread-terminated)
    (.isSessionOutcomeEvaluationEnded d) (webhook-generic->map (.asSessionOutcomeEvaluationEnded d) :session-outcome-evaluation-ended)
    (.isVaultCreated d) (webhook-generic->map (.asVaultCreated d) :vault-created)
    (.isVaultArchived d) (webhook-generic->map (.asVaultArchived d) :vault-archived)
    (.isVaultDeleted d) (webhook-generic->map (.asVaultDeleted d) :vault-deleted)
    (.isVaultCredentialCreated d) (webhook-generic->map (.asVaultCredentialCreated d) :vault-credential-created)
    (.isVaultCredentialArchived d) (webhook-generic->map (.asVaultCredentialArchived d) :vault-credential-archived)
    (.isVaultCredentialDeleted d) (webhook-generic->map (.asVaultCredentialDeleted d) :vault-credential-deleted)
    (.isVaultCredentialRefreshFailed d) (webhook-generic->map (.asVaultCredentialRefreshFailed d) :vault-credential-refresh-failed)
    (.isSessionUpdated d) (webhook-generic->map (.asSessionUpdated d) :session-updated)
    (.isAgentCreated d) (webhook-generic->map (.asAgentCreated d) :agent-created)
    (.isAgentArchived d) (webhook-generic->map (.asAgentArchived d) :agent-archived)
    (.isAgentDeleted d) (webhook-generic->map (.asAgentDeleted d) :agent-deleted)
    (.isAgentUpdated d) (webhook-generic->map (.asAgentUpdated d) :agent-updated)
    :else {:type :unknown}))

(defn- webhook-event->map [^UnwrapWebhookEvent r]
  (let [data (webhook-data->map (.data r))]
    (cond-> (assoc data
                   :id (.id r)
                   :created-at (str (.createdAt r)))
      (unopt (.sessionThreadId (.data r)))
      (assoc :session-thread-id (unopt (.sessionThreadId (.data r))))
      (unopt (.vaultId (.data r)))
      (assoc :vault-id (unopt (.vaultId (.data r))))
      (not= :unknown (:type data))
      (assoc :event-type (json-string (._type r))))))

(defn- ->headers ^Headers [headers]
  (let [b (Headers/builder)]
    (doseq [[k v] headers]
      (.put b ^String (name k) ^String v))
    (.build b)))

(defn- ->unwrap-webhook-params ^UnwrapWebhookParams
  [payload {:keys [headers secret]}]
  (let [b (UnwrapWebhookParams/builder)]
    (.body b ^String payload)
    (when headers (.headers b (->headers headers)))
    (when secret (.secret b ^String secret))
    (.build b)))

(defn unwrap-webhook
  "Parse a raw webhook payload into a normalized map. With a second arity,
  verifies signatures when `:headers` and `:secret` are supplied."
  ([^AnthropicClient client ^String payload]
   (with-api-errors
     (webhook-event->map (-> (.beta client) (.webhooks) (.unwrap payload)))))
  ([^AnthropicClient client ^String payload opts]
   (with-api-errors
     (webhook-event->map (-> (.beta client) (.webhooks)
                             (.unwrap (->unwrap-webhook-params payload opts)))))))
