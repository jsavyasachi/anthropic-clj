(ns anthropic.beta
  "Clojure wrappers over the beta agents-platform APIs of the official
  Anthropic Java SDK: skills, memory stores, agents, and sessions.

  These wrap beta endpoints that Anthropic may change; the deeper platform
  surfaces (deployments, environments, vaults, user profiles, webhooks) and
  the nested sub-resources (skill versions, memories, session events/threads)
  are not wrapped yet. Errors follow `anthropic.core`'s contract: API/IO
  failures are ex-info keyed `:anthropic/error` with the SDK exception as
  cause."
  (:require [anthropic.core]
            [clojure.walk :as walk])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue MultipartField)
           (com.anthropic.errors AnthropicException)
           (com.anthropic.models.beta.skills SkillCreateParams
                                             SkillCreateResponse
                                             SkillDeleteResponse
                                             SkillListPage
                                             SkillListResponse
                                             SkillRetrieveResponse)
           (com.anthropic.models.beta.memorystores BetaManagedAgentsMemoryStore
                                                   BetaManagedAgentsDeletedMemoryStore
                                                   MemoryStoreCreateParams
                                                   MemoryStoreCreateParams$Metadata
                                                   MemoryStoreListPage
                                                   MemoryStoreUpdateParams
                                                   MemoryStoreUpdateParams$Metadata)
           (com.anthropic.models.beta.agents AgentCreateParams
                                             AgentCreateParams$Metadata
                                             AgentListPage
                                             AgentUpdateParams
                                             AgentUpdateParams$Metadata
                                             BetaManagedAgentsAgent
                                             BetaManagedAgentsModel
                                             BetaManagedAgentsModelConfig)
           (com.anthropic.models.beta.sessions BetaManagedAgentsDeletedSession
                                               BetaManagedAgentsSession
                                               SessionCreateParams
                                               SessionCreateParams$Metadata
                                               SessionListPage
                                               SessionUpdateParams
                                               SessionUpdateParams$Metadata)
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

(defn- ->agent-create-params ^AgentCreateParams
  [{:keys [name model system description metadata]}]
  (when-not name (missing-key! :name))
  (when-not model (missing-key! :model))
  (let [b (AgentCreateParams/builder)]
    (.name b ^String name)
    (.model b (BetaManagedAgentsModel/of ^String model))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-create-metadata metadata)))
    (.build b)))

(defn- ->agent-update-params ^AgentUpdateParams
  [agent-id {:keys [version name model system description metadata]}]
  (when-not version (missing-key! :version))
  (let [b (AgentUpdateParams/builder)]
    (.agentId b ^String agent-id)
    (.version b (int version))
    (when name (.name b ^String name))
    (when model (.model b (BetaManagedAgentsModel/of ^String model)))
    (when system (.system b ^String system))
    (when description (.description b ^String description))
    (when metadata (.metadata b (->agent-update-metadata metadata)))
    (.build b)))

(defn- agent->map [^BetaManagedAgentsAgent r]
  (cond-> {:id (.id r)
           :name (.name r)
           :model (.id ^BetaManagedAgentsModelConfig (.model r))
           :version (.version r)
           :created-at (str (.createdAt r))
           :updated-at (str (.updatedAt r))}
    (unopt (.system r)) (assoc :system (unopt (.system r)))
    (unopt (.description r)) (assoc :description (unopt (.description r)))
    (unopt (.archivedAt r)) (assoc :archived-at (str (unopt (.archivedAt r))))))

(defn create-agent
  "Create a managed agent: `:name` and `:model` (required), `:system`,
  `:description`, `:metadata`. Skill/tool/MCP configuration is not wrapped
  yet. Returns the agent as a map (`:id`, `:name`, `:model`, `:version`,
  `:system`, `:description`, `:created-at`, `:updated-at`)."
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
