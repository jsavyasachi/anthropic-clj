(ns anthropic.beta
  "Clojure wrappers over the beta agents-platform APIs of the official
  Anthropic Java SDK: skills, memory stores, agents, sessions, deployments,
  deployment runs, environments, vaults, user profiles, and webhooks.

  These wrap beta endpoints that Anthropic may change; vault credentials,
  environment work, memory versions, thread events, session event streaming,
  session resources, agent versions, and detailed deployment-run trigger
  context/resources/schedules are not wrapped yet. Errors follow
  `anthropic.core`'s contract: API/IO failures are ex-info keyed
  `:anthropic/error` with the SDK exception as cause."
  (:require [anthropic.core]
            [clojure.walk :as walk])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue MultipartField UnwrapWebhookParams)
           (com.anthropic.core.http Headers HttpResponse)
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
                                             BetaManagedAgentsAgentReference
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
                                             BetaManagedAgentsModelConfig)
           (com.anthropic.models.beta.sessions BetaManagedAgentsDeletedSession
                                               BetaManagedAgentsSession
                                               BetaManagedAgentsSystemContentBlock
                                               BetaManagedAgentsSystemMessageEvent
                                               SessionCreateParams
                                               SessionCreateParams$Metadata
                                               SessionListPage
                                               SessionUpdateParams
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
                                                        ThreadArchiveParams
                                                        ThreadListPage
                                                        ThreadListParams
                                                        ThreadRetrieveParams)
           (com.anthropic.models.beta.deployments BetaManagedAgentsDeployment
                                                  BetaManagedAgentsDeploymentInitialEventParams
                                                  DeploymentCreateParams
                                                  DeploymentCreateParams$Metadata
                                                  DeploymentListPage
                                                  DeploymentRunParams
                                                  DeploymentUpdateParams
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
                                                   UserProfileListPage
                                                   UserProfileUpdateParams
                                                   UserProfileUpdateParams$Metadata)
           (com.anthropic.models.beta.webhooks BetaWebhookDeploymentArchivedEventData
                                               BetaWebhookDeploymentCreatedEventData
                                               BetaWebhookDeploymentDeletedEventData
                                               BetaWebhookDeploymentPausedEventData
                                               BetaWebhookDeploymentRunFailedEventData
                                               BetaWebhookDeploymentRunStartedEventData
                                               BetaWebhookDeploymentRunSucceededEventData
                                               BetaWebhookDeploymentUnpausedEventData
                                               BetaWebhookDeploymentUpdatedEventData
                                               BetaWebhookEventData
                                               BetaWebhookSessionCreatedEventData
                                               UnwrapWebhookEvent)
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

(defn- json-string [^JsonValue v]
  (.convert v String))

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

(defn- skill-map [id display-title latest-version source created-at updated-at]
  (cond-> {:id id
           :source (str source)
           :created-at (str created-at)
           :updated-at (str updated-at)}
    display-title (assoc :display-title display-title)
    latest-version (assoc :latest-version latest-version)))

(defn- skill-create->map [^SkillCreateResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r)))

(defn- skill-retrieve->map [^SkillRetrieveResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r)))

(defn- skill-list->map [^SkillListResponse r]
  (skill-map (.id r) (unopt (.displayTitle r)) (unopt (.latestVersion r))
             (.source r) (.createdAt r) (.updatedAt r)))

(defn create-skill
  "Create a skill from `:files` (paths or `java.io.File`s; typically a
  SKILL.md plus resources) with an optional `:display-title`. Returns the
  skill as a map (`:id`, `:display-title`, `:latest-version`, `:source`,
  `:created-at`, `:updated-at`)."
  [^AnthropicClient client req]
  (with-api-errors
    (skill-create->map (-> (.beta client) (.skills) (.create (->skill-create-params req))))))

(defn get-skill
  "Retrieve one skill by id, as a map shaped like `create-skill`'s return."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (skill-retrieve->map (-> (.beta client) (.skills) (.retrieve skill-id)))))

(defn list-skills
  "List skills (pages followed) as a vector of maps like `get-skill`."
  [^AnthropicClient client]
  (with-api-errors
    (let [^SkillListPage p (-> (.beta client) (.skills) (.list))]
      (mapv skill-list->map (.autoPager p)))))

(defn delete-skill
  "Delete a skill by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (let [^SkillDeleteResponse r (-> (.beta client) (.skills) (.delete skill-id))]
      {:id (.id r) :deleted true})))

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

(defn- ->version-list-params ^VersionListParams [skill-id]
  (let [b (VersionListParams/builder)]
    (.skillId b ^String skill-id)
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

(defn- skill-version-map [id skill-id version name description directory created-at]
  (cond-> {:id id
           :skill-id skill-id
           :version version
           :name name
           :description description
           :directory directory
           :created-at (str created-at)}
    true identity))

(defn- skill-version->map [r]
  (cond
    (instance? VersionCreateResponse r)
    (let [^VersionCreateResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r)))
    (instance? VersionRetrieveResponse r)
    (let [^VersionRetrieveResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r)))
    :else
    (let [^VersionListResponse r r]
      (skill-version-map (.id r) (.skillId r) (.version r) (.name r)
                         (.description r) (.directory r) (.createdAt r)))))

(defn- skill-version-delete->map [^VersionDeleteResponse r]
  {:id (.id r) :deleted true})

(defn create-skill-version
  "Create a new skill version for `skill-id` from `:files` (paths or
  `java.io.File`s). Returns the version as a map."
  [^AnthropicClient client ^String skill-id req]
  (with-api-errors
    (skill-version->map (-> (.beta client) (.skills) (.versions)
                            (.create (->version-create-params skill-id req))))))

(defn get-skill-version
  "Retrieve one skill version."
  [^AnthropicClient client ^String skill-id ^String version]
  (with-api-errors
    (skill-version->map (-> (.beta client) (.skills) (.versions)
                            (.retrieve (->version-retrieve-params skill-id version))))))

(defn list-skill-versions
  "List skill versions (pages followed) as a vector of maps."
  [^AnthropicClient client ^String skill-id]
  (with-api-errors
    (let [^VersionListPage p (-> (.beta client) (.skills) (.versions)
                                 (.list (->version-list-params skill-id)))]
      (mapv skill-version->map (.autoPager p)))))

(defn delete-skill-version
  "Delete a skill version. Returns `{:id ... :deleted true}`."
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
           :updated-at (str (.updatedAt r))}
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
  "Retrieve a memory store by id, as a map like `create-memory-store`'s return."
  [^AnthropicClient client ^String memory-store-id]
  (with-api-errors
    (memory-store->map (-> (.beta client) (.memoryStores) (.retrieve memory-store-id)))))

(defn list-memory-stores
  "List memory stores (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^MemoryStoreListPage p (-> (.beta client) (.memoryStores) (.list))]
      (mapv memory-store->map (.autoPager p)))))

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
  "Delete a memory store by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String memory-store-id]
  (with-api-errors
    (let [^BetaManagedAgentsDeletedMemoryStore d
          (-> (.beta client) (.memoryStores) (.delete memory-store-id))]
      {:id (.id d) :deleted true})))

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
  [memory-store-id memory-id {:keys [path content view]}]
  (let [b (MemoryUpdateParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (.memoryId b ^String memory-id)
    (when path (.path b ^String path))
    (when content (.content b ^String content))
    (when view (.view b (memory-view view)))
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
           :updated-at (str (.updatedAt r))}
    (unopt (.content r)) (assoc :content (unopt (.content r)))))

(defn- memory-delete->map [^BetaManagedAgentsDeletedMemory r]
  {:id (.id r) :deleted true})

(defn create-memory
  "Create a memory in `memory-store-id`: `:path` (required), `:content`,
  and `:view`. Returns the memory map."
  [^AnthropicClient client ^String memory-store-id req]
  (with-api-errors
    (memory->map (-> (.beta client) (.memoryStores) (.memories)
                     (.create (->memory-create-params memory-store-id req))))))

(defn get-memory
  "Retrieve a memory by id."
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
       (mapv memory->map (.autoPager p))))))

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
  [memory-store-id {:keys [memory-id limit page view operation]}]
  (let [b (MemoryVersionListParams/builder)]
    (.memoryStoreId b ^String memory-store-id)
    (when memory-id (.memoryId b ^String memory-id))
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
           :operation (keyword (str (.operation r)))
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
  "Retrieve one memory version. `opts` may include `:view`."
  ([^AnthropicClient client ^String memory-store-id ^String memory-version-id]
   (get-memory-version client memory-store-id memory-version-id {}))
  ([^AnthropicClient client ^String memory-store-id ^String memory-version-id opts]
   (with-api-errors
     (memory-version->map (-> (.beta client) (.memoryStores) (.memoryVersions)
                              (.retrieve (->memory-version-retrieve-params
                                          memory-store-id memory-version-id opts)))))))

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

(defn- ->mcp-toolset ^BetaManagedAgentsMcpToolsetParams [{:keys [mcp-server-name]}]
  (when-not mcp-server-name (missing-key! :mcp-server-name))
  (let [b (BetaManagedAgentsMcpToolsetParams/builder)]
    (.mcpServerName b ^String mcp-server-name)
    (.type b (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetParams$Type/of "mcp_toolset"))
    (.build b)))

(defn- ->agent-create-tool ^AgentCreateParams$Tool [{:keys [type] :as tool}]
  (case type
    :custom (AgentCreateParams$Tool/ofCustom (->custom-tool tool))
    :mcp-toolset (AgentCreateParams$Tool/ofMcpToolset (->mcp-toolset tool))
    (throw (ex-info (str "Unknown tool type " type)
                    {:anthropic/error :unknown-tool-type :type type}))))

(defn- ->agent-update-tool ^AgentUpdateParams$Tool [{:keys [type] :as tool}]
  (case type
    :custom (AgentUpdateParams$Tool/ofCustom (->custom-tool tool))
    :mcp-toolset (AgentUpdateParams$Tool/ofMcpToolset (->mcp-toolset tool))
    (throw (ex-info (str "Unknown tool type " type)
                    {:anthropic/error :unknown-tool-type :type type}))))

(defn- ->agent-create-params ^AgentCreateParams
  [{:keys [name model system description metadata skills mcp-servers tools]}]
  (when-not name (missing-key! :name))
  (when-not model (missing-key! :model))
  (let [b (AgentCreateParams/builder)]
    (.name b ^String name)
    (.model b (BetaManagedAgentsModel/of ^String model))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-create-metadata metadata)))
    (doseq [skill skills] (.addSkill b (->agent-skill skill)))
    (doseq [server mcp-servers] (.addMcpServer b (->mcp-server server)))
    (doseq [tool tools] (.addTool b (->agent-create-tool tool)))
    (.build b)))

(defn- ->agent-update-params ^AgentUpdateParams
  [agent-id {:keys [version name model system description metadata skills mcp-servers tools]}]
  (when-not version (missing-key! :version))
  (let [b (AgentUpdateParams/builder)]
    (.agentId b ^String agent-id)
    (.version b (int version))
    (when name (.name b ^String name))
    (when model (.model b (BetaManagedAgentsModel/of ^String model)))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-update-metadata metadata)))
    (doseq [skill skills] (.addSkill b (->agent-skill skill)))
    (doseq [server mcp-servers] (.addMcpServer b (->mcp-server server)))
    (doseq [tool tools] (.addTool b (->agent-update-tool tool)))
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
  {:name (.name s) :url (.url s)})

(defn- agent-tool->map [^BetaManagedAgentsAgent$Tool t]
  (cond
    (.isCustom t)
    (let [^BetaManagedAgentsCustomTool tool (.asCustom t)]
      {:type :custom :name (.name tool)})
    (.isMcpToolset t)
    (let [^BetaManagedAgentsMcpToolset tool (.asMcpToolset t)]
      {:type :mcp-toolset :mcp-server-name (.mcpServerName tool)})
    :else {:type :unknown}))

(defn- agent->map [^BetaManagedAgentsAgent r]
  (cond-> {:id (.id r)
           :name (.name r)
           :model (.id ^BetaManagedAgentsModelConfig (.model r))
           :version (.version r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.system r)) (assoc :system (unopt (.system r)))
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (seq (.skills r)) (assoc :skills (mapv agent-skill->map (.skills r)))
    (seq (.mcpServers r)) (assoc :mcp-servers (mapv mcp-server->map (.mcpServers r)))
    (seq (.tools r)) (assoc :tools (mapv agent-tool->map (.tools r)))))

(defn create-agent
  "Create a managed agent: `:name` and `:model` (required), `:system`,
  `:description`, `:metadata`, `:skills`, `:mcp-servers`, and `:tools`.
  Returns the agent as a map (`:id`, `:name`, `:model`, `:version`,
  `:system`, `:description`, `:skills`, `:mcp-servers`, `:tools`,
  `:created-at`, `:updated-at`)."
  [^AnthropicClient client req]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents) (.create (->agent-create-params req))))))

(defn get-agent
  "Retrieve an agent by id, as a map like `create-agent`'s return."
  [^AnthropicClient client ^String agent-id]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents) (.retrieve agent-id)))))

(defn list-agents
  "List agents (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^AgentListPage p (-> (.beta client) (.agents) (.list))]
      (mapv agent->map (.autoPager p)))))

(defn update-agent
  "Update an agent. `changes` requires `:version` (the current agent version,
  for optimistic concurrency - see `:version` in `get-agent`'s return) plus
  any of `:name`, `:model`, `:system`, `:description`, `:metadata`. Returns
  the updated agent map."
  [^AnthropicClient client ^String agent-id changes]
  (with-api-errors
    (agent->map (-> (.beta client) (.agents)
                    (.update (->agent-update-params agent-id changes))))))

(defn archive-agent
  "Archive an agent by id. Returns the archived agent map."
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

(defn- ->session-create-params ^SessionCreateParams
  [{:keys [agent title environment-id metadata]}]
  (when-not agent (missing-key! :agent))
  (let [b (SessionCreateParams/builder)]
    (.agent b ^String agent)
    (when title (.title b ^String title))
    (when environment-id (.environmentId b ^String environment-id))
    (when metadata (.metadata b (->session-create-metadata metadata)))
    (.build b)))

(defn- ->session-update-params ^SessionUpdateParams
  [session-id {:keys [title metadata]}]
  (let [b (SessionUpdateParams/builder)]
    (.sessionId b ^String session-id)
    (when title (.title b ^String title))
    (when metadata (.metadata b (->session-update-metadata metadata)))
    (.build b)))

(defn- session->map [^BetaManagedAgentsSession r]
  (cond-> {:id (.id r)
           :status (str (.status r))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.title r)) (assoc :title (unopt (.title r)))
    (unopt (.environmentId r)) (assoc :environment-id (unopt (.environmentId r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn- agent-ref->map [r]
  (if (instance? BetaManagedAgentsAgentReference r)
    (let [^BetaManagedAgentsAgentReference r r]
      {:id (.id r) :version (.version r)})
    (let [^BetaManagedAgentsSessionThreadAgent r r]
      {:id (.id r) :version (.version r)})))

(defn create-session
  "Create a session for `:agent` (an agent id, required), with optional
  `:title`, `:environment-id`, and `:metadata`. Session resources, vault
  ids, and per-session agent overrides are not wrapped yet. Returns the
  session as a map (`:id`, `:status`, `:title`, `:environment-id`,
  `:created-at`, `:updated-at`)."
  [^AnthropicClient client req]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.create (->session-create-params req))))))

(defn get-session
  "Retrieve a session by id, as a map like `create-session`'s return."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.retrieve session-id)))))

(defn list-sessions
  "List sessions (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^SessionListPage p (-> (.beta client) (.sessions) (.list))]
      (mapv session->map (.autoPager p)))))

(defn update-session
  "Update a session's `:title` or `:metadata`. Returns the updated session map."
  [^AnthropicClient client ^String session-id changes]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions)
                      (.update (->session-update-params session-id changes))))))

(defn archive-session
  "Archive a session by id. Returns the archived session map."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (session->map (-> (.beta client) (.sessions) (.archive session-id)))))

(defn delete-session
  "Delete a session by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (let [^BetaManagedAgentsDeletedSession d
          (-> (.beta client) (.sessions) (.delete session-id))]
      {:id (.id d) :deleted true})))

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
    (throw (ex-info (str "Unknown event type " type)
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

(defn- user-content->map [^BetaManagedAgentsUserMessageEvent$Content c]
  (cond
    (.isText c) (.text ^BetaManagedAgentsTextBlock (.asText c))
    :else {:type :unknown}))

(defn- session-event->map [^BetaManagedAgentsSessionEvent e]
  (cond
    (.isUserMessage e)
    (let [^BetaManagedAgentsUserMessageEvent r (.asUserMessage e)]
      (cond-> {:type :user-message
               :id (.id r)
               :content (mapv user-content->map (.content r))}
        (unopt (.processedAt r)) (assoc :processed-at (str (unopt (.processedAt r))))))
    (.isSystemMessage e)
    (let [^BetaManagedAgentsSystemMessageEvent r (.asSystemMessage e)]
      (cond-> {:type :system-message
               :id (.id r)
               :content (mapv (fn [^BetaManagedAgentsSystemContentBlock b] (.text b))
                              (.content r))}
        (unopt (.processedAt r)) (assoc :processed-at (str (unopt (.processedAt r))))))
    (.isUserDefineOutcome e)
    (let [^BetaManagedAgentsUserDefineOutcomeEvent r (.asUserDefineOutcome e)]
      (cond-> {:type :user-define-outcome
               :id (.id r)
               :description (.description r)
               :outcome-id (.outcomeId r)
               :processed-at (str (.processedAt r))}
        (unopt (.maxIterations r)) (assoc :max-iterations (unopt (.maxIterations r)))))
    :else {:type :unknown}))

(defn- send-data->map
  [^com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data d]
  (cond
    (.isUserMessage d)
    {:type :user-message
     :id (.id ^BetaManagedAgentsUserMessageEvent (unopt (.userMessage d)))}
    (.isSystemMessage d)
    {:type :system-message
     :id (.id ^com.anthropic.models.beta.sessions.BetaManagedAgentsSystemMessageEvent
              (unopt (.systemMessage d)))}
    (.isUserDefineOutcome d)
    {:type :user-define-outcome
     :id (.id ^BetaManagedAgentsUserDefineOutcomeEvent (unopt (.userDefineOutcome d)))}
    (.isUserInterrupt d) {:type :user-interrupt}
    (.isUserToolConfirmation d) {:type :user-tool-confirmation}
    (.isUserCustomToolResult d) {:type :user-custom-tool-result}
    (.isUserToolResult d) {:type :user-tool-result}
    :else {:type :unknown}))

(defn- send-session-events->map [^BetaManagedAgentsSendSessionEvents r]
  {:data (mapv send-data->map (or (unopt (.data r)) []))})

(defn send-session-events
  "Send a vector of session event maps to `session-id`. Event maps support
  `{:type :user-message :content ...}`, `{:type :system-message :content ...}`,
  and `{:type :user-define-outcome :description ... :rubric ...}`."
  [^AnthropicClient client ^String session-id events]
  (with-api-errors
    (send-session-events->map (-> (.beta client) (.sessions) (.events)
                                  (.send (->event-send-params session-id events))))))

(defn list-session-events
  "List session events (pages followed) as normalized event maps."
  [^AnthropicClient client ^String session-id]
  (with-api-errors
    (let [^EventListPage p (-> (.beta client) (.sessions) (.events) (.list session-id))]
      (mapv session-event->map (.autoPager p)))))

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

(defn- session-thread->map [^BetaManagedAgentsSessionThread r]
  (cond-> {:id (.id r)
           :session-id (.sessionId r)
           :agent (agent-ref->map (.agent r))
           :status (str (.status r))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.parentThreadId r)) (assoc :parent-thread-id (unopt (.parentThreadId r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn get-session-thread
  "Retrieve a session thread by session id and thread id."
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

(defn- ->deployment-create-params ^DeploymentCreateParams
  [{:keys [name agent environment-id initial-events description metadata vault-ids] :as req}]
  (when-not name (missing-key! :name))
  (when-not agent (missing-key! :agent))
  (when-not environment-id (missing-key! :environment-id))
  (when-not (contains? req :initial-events) (missing-key! :initial-events))
  (let [b (DeploymentCreateParams/builder)]
    (.name b ^String name)
    (.agent b ^String agent)
    (.environmentId b ^String environment-id)
    (.initialEvents b ^java.util.List (mapv ->deployment-initial-event initial-events))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->deployment-create-metadata metadata)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (.build b)))

(defn- ->deployment-update-params ^DeploymentUpdateParams
  [deployment-id {:keys [name agent environment-id initial-events description metadata vault-ids]}]
  (let [b (DeploymentUpdateParams/builder)]
    (.deploymentId b ^String deployment-id)
    (when name (.name b ^String name))
    (when agent (.agent b ^String agent))
    (when environment-id (.environmentId b ^String environment-id))
    (when initial-events
      (.initialEvents b ^java.util.List (mapv ->deployment-initial-event initial-events)))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->deployment-update-metadata metadata)))
    (doseq [^String vault-id vault-ids] (.addVaultId b vault-id))
    (.build b)))

(defn- ->deployment-run-params ^DeploymentRunParams [deployment-id]
  (let [b (DeploymentRunParams/builder)]
    (.deploymentId b ^String deployment-id)
    (.build b)))

(defn- deployment->map [^BetaManagedAgentsDeployment r]
  (cond-> {:id (.id r)
           :agent (agent-ref->map (.agent r))
           :environment-id (.environmentId r)
           :name (.name r)
           :status (str (.status r))
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))
           :vault-ids (vec (.vaultIds r))}
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))
    (unopt (.pausedReason r)) (assoc :paused-reason (str (unopt (.pausedReason r))))))

(defn create-deployment
  "Create a deployment. Required: `:name`, `:agent`, `:environment-id`, and
  `:initial-events` (event maps). Optional: `:description`,
  `:metadata`, `:vault-ids`. Returns the deployment map."
  [^AnthropicClient client req]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments)
                         (.create (->deployment-create-params req))))))

(defn get-deployment
  "Retrieve a deployment by id."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment->map (-> (.beta client) (.deployments) (.retrieve deployment-id)))))

(defn list-deployments
  "List deployments (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^DeploymentListPage p (-> (.beta client) (.deployments) (.list))]
      (mapv deployment->map (.autoPager p)))))

(defn update-deployment
  "Update a deployment. `changes` may include `:name`, `:agent`,
  `:environment-id`, `:initial-events`, `:description`, `:metadata`, or
  `:vault-ids`. Returns the updated deployment map."
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

(defn- deployment-run->map [^BetaManagedAgentsDeploymentRun r]
  (cond-> {:id (.id r)
           :agent (agent-ref->map (.agent r))
           :deployment-id (.deploymentId r)
           :created-at (str (.createdAt r))
           :type (str (.type r))}
    (unopt (.sessionId r)) (assoc :session-id (unopt (.sessionId r)))
    (unopt (.error r)) (assoc :error (str (unopt (.error r))))
    (.triggerContext r) (assoc :trigger-context (str (.triggerContext r)))))

(defn run-deployment
  "Run a deployment manually by id. Returns the deployment run map."
  [^AnthropicClient client ^String deployment-id]
  (with-api-errors
    (deployment-run->map (-> (.beta client) (.deployments)
                             (.run (->deployment-run-params deployment-id))))))

;; ---- Deployment runs -------------------------------------------------------

(defn get-deployment-run
  "Retrieve a deployment run by id."
  [^AnthropicClient client ^String deployment-run-id]
  (with-api-errors
    (deployment-run->map (-> (.beta client) (.deploymentRuns)
                             (.retrieve deployment-run-id)))))

(defn list-deployment-runs
  "List deployment runs (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^DeploymentRunListPage p (-> (.beta client) (.deploymentRuns) (.list))]
      (mapv deployment-run->map (.autoPager p)))))

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

(defn- ->environment-create-params ^EnvironmentCreateParams
  [{:keys [name description metadata]}]
  (when-not name (missing-key! :name))
  (let [b (EnvironmentCreateParams/builder)]
    (.name b ^String name)
    (when description (.description b ^String description))
    (when metadata (.metadata b (->environment-create-metadata metadata)))
    (.build b)))

(defn- ->environment-update-params ^EnvironmentUpdateParams
  [environment-id {:keys [name description metadata]}]
  (let [b (EnvironmentUpdateParams/builder)]
    (.environmentId b ^String environment-id)
    (when name (.name b ^String name))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->environment-update-metadata metadata)))
    (.build b)))

(defn- environment->map [^BetaEnvironment r]
  (cond-> {:id (.id r)
           :name (.name r)
           :description (.description r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn- environment-delete->map [^BetaEnvironmentDeleteResponse r]
  {:id (.id r) :deleted true})

(defn create-environment
  "Create an environment: `:name` (required), `:description`, `:metadata`.
  Work/config/scope details are not wrapped yet. Returns the environment map."
  [^AnthropicClient client req]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments)
                          (.create (->environment-create-params req))))))

(defn get-environment
  "Retrieve an environment by id."
  [^AnthropicClient client ^String environment-id]
  (with-api-errors
    (environment->map (-> (.beta client) (.environments) (.retrieve environment-id)))))

(defn list-environments
  "List environments (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^EnvironmentListPage p (-> (.beta client) (.environments) (.list))]
      (mapv environment->map (.autoPager p)))))

(defn update-environment
  "Update an environment's `:name`, `:description`, or `:metadata`."
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
  [{:keys [display-name metadata]}]
  (when-not display-name (missing-key! :display-name))
  (let [b (VaultCreateParams/builder)]
    (.displayName b ^String display-name)
    (when metadata (.metadata b (->vault-create-metadata metadata)))
    (.build b)))

(defn- ->vault-update-params ^VaultUpdateParams
  [vault-id {:keys [display-name metadata]}]
  (let [b (VaultUpdateParams/builder)]
    (.vaultId b ^String vault-id)
    (when display-name (.displayName b ^String display-name))
    (when metadata (.metadata b (->vault-update-metadata metadata)))
    (.build b)))

(defn- vault->map [^BetaManagedAgentsVault r]
  (cond-> {:id (.id r)
           :display-name (.displayName r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn- vault-delete->map [^BetaManagedAgentsDeletedVault r]
  {:id (.id r) :deleted true})

(defn create-vault
  "Create a vault: `:display-name` (required), `:metadata`. Credentials are
  not wrapped yet. Returns the vault map."
  [^AnthropicClient client req]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults) (.create (->vault-create-params req))))))

(defn get-vault
  "Retrieve a vault by id."
  [^AnthropicClient client ^String vault-id]
  (with-api-errors
    (vault->map (-> (.beta client) (.vaults) (.retrieve vault-id)))))

(defn list-vaults
  "List vaults (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^VaultListPage p (-> (.beta client) (.vaults) (.list))]
      (mapv vault->map (.autoPager p)))))

(defn update-vault
  "Update a vault's `:display-name` or `:metadata`."
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
  "Delete a vault by id. Returns `{:id ... :deleted true}`."
  [^AnthropicClient client ^String vault-id]
  (with-api-errors
    (vault-delete->map (-> (.beta client) (.vaults) (.delete vault-id)))))

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
  [{:keys [name external-id metadata]}]
  (let [b (UserProfileCreateParams/builder)]
    (when name (.name b ^String name))
    (when external-id (.externalId b ^String external-id))
    (when metadata (.metadata b (->user-profile-create-metadata metadata)))
    (.build b)))

(defn- ->user-profile-update-params ^UserProfileUpdateParams
  [user-profile-id {:keys [name external-id metadata]}]
  (let [b (UserProfileUpdateParams/builder)]
    (.userProfileId b ^String user-profile-id)
    (when name (.name b ^String name))
    (when external-id (.externalId b ^String external-id))
    (when metadata (.metadata b (->user-profile-update-metadata metadata)))
    (.build b)))

(defn- ->user-profile-enrollment-url-params ^UserProfileCreateEnrollmentUrlParams
  [user-profile-id]
  (let [b (UserProfileCreateEnrollmentUrlParams/builder)]
    (.userProfileId b ^String user-profile-id)
    (.build b)))

(defn- user-profile->map [^BetaUserProfile r]
  (cond-> {:id (.id r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.name r)) (assoc :name (unopt (.name r)))
    (unopt (.externalId r)) (assoc :external-id (unopt (.externalId r)))
    (.relationship r) (assoc :relationship (str (.relationship r)))
    (.trustGrants r) (assoc :trust-grants (str (.trustGrants r)))))

(defn- enrollment-url->map [^BetaUserProfileEnrollmentUrl r]
  {:url (.url r)
   :expires-at (str (.expiresAt r))})

(defn create-user-profile
  "Create a user profile with optional `:name`, `:external-id`, and
  `:metadata`. Relationship config is not wrapped yet. Returns the profile
  map."
  [^AnthropicClient client req]
  (with-api-errors
    (user-profile->map (-> (.beta client) (.userProfiles)
                           (.create (->user-profile-create-params req))))))

(defn get-user-profile
  "Retrieve a user profile by id."
  [^AnthropicClient client ^String user-profile-id]
  (with-api-errors
    (user-profile->map (-> (.beta client) (.userProfiles) (.retrieve user-profile-id)))))

(defn list-user-profiles
  "List user profiles (pages followed) as a vector of maps."
  [^AnthropicClient client]
  (with-api-errors
    (let [^UserProfileListPage p (-> (.beta client) (.userProfiles) (.list))]
      (mapv user-profile->map (.autoPager p)))))

(defn update-user-profile
  "Update a user profile's `:name`, `:external-id`, or `:metadata`."
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

(defn- webhook-data->map [^BetaWebhookEventData d]
  (cond
    (.isSessionCreated d) (webhook-session-created->map (.asSessionCreated d))
    (.isDeploymentCreated d) (webhook-deployment-created->map (.asDeploymentCreated d))
    (.isDeploymentUpdated d) (webhook-deployment-updated->map (.asDeploymentUpdated d))
    (.isDeploymentPaused d) (webhook-deployment-paused->map (.asDeploymentPaused d))
    (.isDeploymentUnpaused d) (webhook-deployment-unpaused->map (.asDeploymentUnpaused d))
    (.isDeploymentArchived d) (webhook-deployment-archived->map (.asDeploymentArchived d))
    (.isDeploymentDeleted d) (webhook-deployment-deleted->map (.asDeploymentDeleted d))
    (.isDeploymentRunStarted d) (webhook-deployment-run-started->map (.asDeploymentRunStarted d))
    (.isDeploymentRunSucceeded d) (webhook-deployment-run-succeeded->map (.asDeploymentRunSucceeded d))
    (.isDeploymentRunFailed d) (webhook-deployment-run-failed->map (.asDeploymentRunFailed d))
    :else {:type :unknown}))

(defn- webhook-event->map [^UnwrapWebhookEvent r]
  (let [data (webhook-data->map (.data r))]
    (cond-> (assoc data
                   :id (.id r)
                   :created-at (str (.createdAt r)))
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
