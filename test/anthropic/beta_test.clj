(ns anthropic.beta-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string]
            [anthropic.beta :as beta])
  (:import (com.anthropic.models.beta.skills SkillCreateParams
                                             SkillCreateResponse)
           (com.anthropic.models.beta.memorystores BetaManagedAgentsMemoryStore
                                                   MemoryStoreCreateParams
                                                   MemoryStoreUpdateParams)
           (com.anthropic.models.beta.agents AgentCreateParams
                                             AgentUpdateParams
                                             BetaManagedAgentsAgent
                                             BetaManagedAgentsAgentReference
                                             BetaManagedAgentsModelConfig)
           (com.anthropic.models.beta.sessions BetaManagedAgentsDeltaType
                                               BetaManagedAgentsMultiagent
                                               SessionCreateParams
                                               SessionUpdateParams)
           (com.anthropic.models.beta.sessions.events BetaManagedAgentsEventParams
                                                       BetaManagedAgentsAgentMessageEvent
                                                       BetaManagedAgentsAgentMessageEvent$Type
                                                       BetaManagedAgentsSendSessionEvents
                                                       BetaManagedAgentsSessionEvent
                                                       BetaManagedAgentsStreamSessionEvents
                                                       BetaManagedAgentsUserMessageEvent
                                                       EventSendParams)
           (com.anthropic.models.beta.sessions.threads BetaManagedAgentsSessionThread
                                                        BetaManagedAgentsStreamSessionThreadEvents
                                                        ThreadArchiveParams
                                                        ThreadListParams
                                                        ThreadRetrieveParams)
           (com.anthropic.core JsonValue)
           (com.anthropic.core.http StreamResponse)
           (com.anthropic.models.beta.deployments BetaManagedAgentsDeployment
                                                  DeploymentCreateParams
                                                  DeploymentRunParams
                                                  DeploymentUpdateParams)
           (com.anthropic.models.beta.deploymentruns BetaManagedAgentsDeploymentRun)
           (com.anthropic.models.beta.memorystores.memories BetaManagedAgentsDeletedMemory
                                                             BetaManagedAgentsMemory
                                                             MemoryCreateParams
                                                             MemoryDeleteParams
                                                             MemoryListParams
                                                             MemoryRetrieveParams
                                                             MemoryUpdateParams)
           (com.anthropic.models.beta.memorystores.memoryversions BetaManagedAgentsActor
                                                                   BetaManagedAgentsApiActor
                                                                   BetaManagedAgentsApiActor$Type
                                                                   BetaManagedAgentsMemoryVersion
                                                                   BetaManagedAgentsMemoryVersion$Type
                                                                   BetaManagedAgentsMemoryVersionOperation
                                                                   BetaManagedAgentsServiceAccountActor
                                                                   MemoryVersionListParams
                                                                   MemoryVersionRetrieveParams)
           (com.anthropic.models.beta.environments BetaEnvironment
                                                   BetaEnvironmentDeleteResponse
                                                   EnvironmentCreateParams
                                                   EnvironmentUpdateParams)
           (com.anthropic.models.beta.environments.work BetaSelfHostedWork
                                                        BetaSelfHostedWorkHeartbeatResponse
                                                        BetaSelfHostedWorkQueueStats
                                                        WorkAckParams
                                                        WorkHeartbeatParams
                                                        WorkListParams
                                                        WorkPollParams
                                                        WorkRetrieveParams
                                                        WorkStatsParams
                                                        WorkStopParams
                                                        WorkUpdateParams)
           (com.anthropic.models.beta.skills.versions VersionCreateParams
                                                       VersionCreateResponse
                                                       VersionDeleteParams
                                                       VersionDeleteResponse
                                                       VersionDownloadParams
                                                       VersionListParams
                                                       VersionRetrieveParams)
           (com.anthropic.models.beta.vaults BetaManagedAgentsDeletedVault
                                             BetaManagedAgentsVault
                                             VaultCreateParams
                                             VaultUpdateParams)
           (com.anthropic.models.beta.userprofiles BetaUserProfile
                                                   BetaUserProfileEnrollmentUrl
                                                   UserProfileCreateParams
                                                   UserProfileCreateEnrollmentUrlParams
                                                   UserProfileUpdateParams)
           (com.anthropic.models.beta.webhooks BetaWebhookEventData
                                               BetaWebhookEnvironmentArchivedEventData
                                               BetaWebhookEnvironmentCreatedEventData
                                               BetaWebhookEnvironmentDeletedEventData
                                               BetaWebhookEnvironmentUpdatedEventData
                                               BetaWebhookMemoryStoreArchivedEventData
                                               BetaWebhookMemoryStoreCreatedEventData
                                               BetaWebhookMemoryStoreDeletedEventData
                                               BetaWebhookSessionCreatedEventData
                                               UnwrapWebhookEvent)
           (com.anthropic.models.beta.models BetaCapabilitySupport
                                             BetaContextManagementCapability
                                             BetaEffortCapability
                                             BetaModelCapabilities
                                             BetaModelInfo
                                             BetaThinkingCapability
                                             BetaThinkingTypes)
           (java.util Optional)))

(def ->skill-create-params #'beta/->skill-create-params)
(def ->memory-store-create-params #'beta/->memory-store-create-params)
(def ->memory-store-update-params #'beta/->memory-store-update-params)
(def ->agent-create-params #'beta/->agent-create-params)
(def ->agent-update-params #'beta/->agent-update-params)
(def agent-ref->map #'beta/agent-ref->map)
(def ->session-create-params #'beta/->session-create-params)
(def ->session-update-params #'beta/->session-update-params)
(def ->session-event #'beta/->session-event)
(def ->event-send-params #'beta/->event-send-params)
(def session-event->map #'beta/session-event->map)
(def send-session-events->map #'beta/send-session-events->map)
(def user-content->map #'beta/user-content->map)
(def image-source->map #'beta/image-source->map)
(def ->thread-retrieve-params #'beta/->thread-retrieve-params)
(def ->thread-list-params #'beta/->thread-list-params)
(def ->thread-archive-params #'beta/->thread-archive-params)
(def session-thread->map #'beta/session-thread->map)
(def session-thread-stats->map #'beta/session-thread-stats->map)
(def session-resource->map #'beta/session-resource->map)
(def deployment-resource->map #'beta/deployment-resource->map)
(def ->deployment-create-params #'beta/->deployment-create-params)
(def ->deployment-update-params #'beta/->deployment-update-params)
(def ->deployment-run-params #'beta/->deployment-run-params)
(def ->memory-create-params #'beta/->memory-create-params)
(def ->memory-retrieve-params #'beta/->memory-retrieve-params)
(def ->memory-update-params #'beta/->memory-update-params)
(def ->memory-list-params #'beta/->memory-list-params)
(def ->memory-delete-params #'beta/->memory-delete-params)
(def ->memory-version-list-params #'beta/->memory-version-list-params)
(def ->memory-version-retrieve-params #'beta/->memory-version-retrieve-params)
(def memory-version->map #'beta/memory-version->map)
(def dream->map #'beta/dream->map)
(def ->dream-create-params #'beta/->dream-create-params)
(def memory->map #'beta/memory->map)
(def memory-delete->map #'beta/memory-delete->map)
(def ->environment-create-params #'beta/->environment-create-params)
(def ->environment-update-params #'beta/->environment-update-params)
(def ->environment-work-retrieve-params #'beta/->environment-work-retrieve-params)
(def ->environment-work-update-params #'beta/->environment-work-update-params)
(def ->environment-work-list-params #'beta/->environment-work-list-params)
(def ->environment-work-ack-params #'beta/->environment-work-ack-params)
(def ->environment-work-heartbeat-params #'beta/->environment-work-heartbeat-params)
(def ->environment-work-poll-params #'beta/->environment-work-poll-params)
(def ->environment-work-stats-params #'beta/->environment-work-stats-params)
(def ->environment-work-stop-params #'beta/->environment-work-stop-params)
(def ->version-create-params #'beta/->version-create-params)
(def ->version-retrieve-params #'beta/->version-retrieve-params)
(def ->version-list-params #'beta/->version-list-params)
(def ->version-delete-params #'beta/->version-delete-params)
(def ->version-download-params #'beta/->version-download-params)
(def skill-version->map #'beta/skill-version->map)
(def skill-version-delete->map #'beta/skill-version-delete->map)
(def ->vault-create-params #'beta/->vault-create-params)
(def ->vault-update-params #'beta/->vault-update-params)
(def ->user-profile-create-params #'beta/->user-profile-create-params)
(def ->user-profile-update-params #'beta/->user-profile-update-params)
(def ->user-profile-enrollment-url-params #'beta/->user-profile-enrollment-url-params)
(def skill-create->map #'beta/skill-create->map)
(def memory-store->map #'beta/memory-store->map)
(def agent->map #'beta/agent->map)
(def deployment->map #'beta/deployment->map)
(def memory-list-item->map #'beta/memory-list-item->map)
(def deployment-run->map #'beta/deployment-run->map)
(def environment->map #'beta/environment->map)
(def environment-delete->map #'beta/environment-delete->map)
(def environment-work->map #'beta/environment-work->map)
(def environment-work-heartbeat->map #'beta/environment-work-heartbeat->map)
(def environment-work-stats->map #'beta/environment-work-stats->map)
(def environment-work-optional->map #'beta/environment-work-optional->map)
(def vault->map #'beta/vault->map)
(def vault-delete->map #'beta/vault-delete->map)
(def user-profile->map #'beta/user-profile->map)
(def enrollment-url->map #'beta/enrollment-url->map)
(def webhook-event->map #'beta/webhook-event->map)
(def ->tunnel-create-params #'beta/->tunnel-create-params)
(def tunnel->map #'beta/tunnel->map)
(def ->agent-version-list-params #'beta/->agent-version-list-params)
(def ->tunnel-certificate-create-params #'beta/->tunnel-certificate-create-params)
(def tunnel-certificate->map #'beta/tunnel-certificate->map)
(def ->thread-event-list-params #'beta/->thread-event-list-params)
(def ->session-resource-list-params #'beta/->session-resource-list-params)
(def ->dream-create-params #'beta/->dream-create-params)
(def ->session-event-stream-params #'beta/->session-event-stream-params)
(def ->thread-event-stream-params #'beta/->thread-event-stream-params)
(def stream-event->map #'beta/stream-event->map)
(def consume-event-stream #'beta/consume-event-stream)
(def stream-event-json #'beta/stream-event-json)
(def session->map #'beta/session->map)
(def session-agent->map #'beta/session-agent->map)
(def agent-tool->map #'beta/agent-tool->map)
(def deployment->map #'beta/deployment->map)
(def ->managed-agent-model-config #'beta/->managed-agent-model-config)

(defn- private-fn [name]
  (some-> (ns-resolve 'anthropic.beta name) var-get))

(defn- invoke-private [name & args]
  (apply (private-fn name) args))

(defn- opt [^java.util.Optional o] (when (.isPresent o) (.get o)))

(defn- ex-data-for [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest beta-model-list-params-are-wired
  (let [f (private-fn '->beta-model-list-params)
        p (when f (f {:limit 7 :before-id "before" :after-id "after"
                      :betas [:beta-one "beta-two"]}))]
    (is (some? p) "beta model list params must be built")
    (when p
      (is (= 7 (opt (.limit p))))
      (is (= "before" (opt (.beforeId p))))
      (is (= "after" (opt (.afterId p))))
      (is (= ["beta-one" "beta-two"]
             (mapv #(.asString %) (opt (.betas p))))))))

(deftest beta-model-params-accept-no-options
  (doseq [[name args] [['->beta-model-list-params [{}]]
                       ['->beta-model-retrieve-params ["model_1" {}]]]]
    (let [f (private-fn (symbol name))]
      (is (some? (when f (apply f args)))
          (str name " must support omitted opts")))))

(deftest beta-model-response-maps-all-fields
  (let [support (-> (BetaCapabilitySupport/builder) (.supported true) (.build))
        context (-> (BetaContextManagementCapability/builder)
                    (.clearThinking20251015 support)
                    (.clearToolUses20250919 support)
                    (.compact20260112 support)
                    (.supported true)
                    (.build))
        effort (-> (BetaEffortCapability/builder)
                   (.high support) (.low support) (.max support)
                   (.medium support) (.xhigh (Optional/of support))
                   (.supported true) (.build))
        thinking-types (-> (BetaThinkingTypes/builder)
                           (.adaptive support) (.enabled support) (.build))
        thinking (-> (BetaThinkingCapability/builder)
                     (.supported true) (.types thinking-types) (.build))
        capabilities (-> (BetaModelCapabilities/builder)
                         (.batch support) (.citations support)
                         (.codeExecution support) (.contextManagement context)
                         (.effort effort) (.imageInput support)
                         (.pdfInput support) (.structuredOutputs support)
                         (.thinking thinking)
                         (.build))
        model (-> (BetaModelInfo/builder)
                  (.id "model_1")
                  (.displayName "Beta Model")
                  (.createdAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                  (.allowedFallbackModels ["fallback_1"])
                  (.capabilities capabilities)
                  (.maxInputTokens 200000)
                  (.maxTokens 64000)
                  (.type (JsonValue/from "model"))
                  (.build))
        f (private-fn 'beta-model->map)
        actual (when f (f model))]
    (is (= {:id "model_1"
            :display-name "Beta Model"
            :created-at "2026-07-04T00:00Z"
            :allowed-fallback-models ["fallback_1"]
            :max-input-tokens 200000
            :max-tokens 64000
            :capabilities {:batch {:supported true}
                           :citations {:supported true}
                           :code-execution {:supported true}
                           :context-management {:clear-thinking-20251015 {:supported true}
                                                 :clear-tool-uses-20250919 {:supported true}
                                                 :compact-20260112 {:supported true}
                                                 :supported true}
                           :effort {:high {:supported true}
                                    :low {:supported true}
                                    :max {:supported true}
                                    :medium {:supported true}
                                    :xhigh {:supported true}
                                    :supported true}
                           :image-input {:supported true}
                           :pdf-input {:supported true}
                           :structured-outputs {:supported true}
                           :thinking {:supported true
                                      :types {:adaptive {:supported true}
                                              :enabled {:supported true}}}}
            :type :model}
           actual))))

(deftest every-beta-list-builder-is-wired
  (doseq [name '[->skill-list-params ->version-list-params
                 ->memory-store-list-params ->agent-list-params
                 ->session-list-params ->deployment-list-params
                 ->deployment-run-list-params ->environment-list-params
                 ->vault-list-params ->tunnel-list-params
                 ->certificate-list-params ->dream-list-params
                 ->credential-list-params ->user-profile-list-params]]
    (is (fn? (private-fn name))
        (str name " must build SDK list params"))))

(deftest every-beta-list-builder-accepts-no-options
  (doseq [[name args] [['->skill-list-params [{}]]
                       ['->version-list-params ["skill_1" {}]]
                       ['->memory-store-list-params [{}]]
                       ['->agent-list-params [{}]]
                       ['->session-list-params [{}]]
                       ['->deployment-list-params [{}]]
                       ['->deployment-run-list-params [{}]]
                       ['->environment-list-params [{}]]
                       ['->vault-list-params [{}]]
                       ['->tunnel-list-params [{}]]
                       ['->certificate-list-params ["tun_1" {}]]
                       ['->dream-list-params [{}]]
                       ['->credential-list-params ["vault_1" {}]]
                       ['->user-profile-list-params [{}]]]]
    (if-let [f (private-fn (symbol name))]
      (is (some? (apply f args))
          (str name " must support omitted opts"))
      (is false (str name " must support omitted opts")))))

(deftest beta-list-builder-options-reach-sdk
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        cases [{:name '->skill-list-params :args [{}]
                :check (fn [p] [(= 7 (opt (.limit p)))
                                (= "page" (opt (.page p)))
                                (= "custom" (opt (.source p)))
                                (= ["beta-test"] (mapv #(.asString %) (opt (.betas p))))])}
               {:name '->version-list-params :args ["skill_1" {}]
                :check (fn [p] [(= 7 (opt (.limit p))) (= "page" (opt (.page p)))])}
               {:name '->memory-store-list-params :args [{}]
                :check (fn [p] [(= ts (opt (.createdAtGte p)))
                                (= ts (opt (.createdAtLte p)))
                                (= true (opt (.includeArchived p)))])}
               {:name '->agent-list-params :args [{}]
                :check (fn [p] [(= ts (opt (.createdAtGte p)))
                                (= ts (opt (.createdAtLte p)))
                                (= true (opt (.includeArchived p)))])}
               {:name '->session-list-params :args [{}]
                :check (fn [p] [(= "agent_1" (opt (.agentId p)))
                                (= 2 (opt (.agentVersion p)))
                                (= ts (opt (.createdAtGt p)))
                                (= ts (opt (.createdAtGte p)))
                                (= ts (opt (.createdAtLt p)))
                                (= ts (opt (.createdAtLte p)))
                                (= "dep_1" (opt (.deploymentId p)))
                                (= true (opt (.includeArchived p)))
                                (= "store_1" (opt (.memoryStoreId p)))
                                (= "desc" (.asString (opt (.order p))))
                                (= 2 (count (opt (.statuses p))))])}
               {:name '->deployment-list-params :args [{}]
                :check (fn [p] [(= "agent_1" (opt (.agentId p)))
                                (= ts (opt (.createdAtGte p)))
                                (= ts (opt (.createdAtLte p)))
                                (= true (opt (.includeArchived p)))
                                (= "active" (.asString (opt (.status p))))])}
               {:name '->deployment-run-list-params :args [{}]
                :check (fn [p] [(= ts (opt (.createdAtGt p)))
                                (= ts (opt (.createdAtGte p)))
                                (= ts (opt (.createdAtLt p)))
                                (= ts (opt (.createdAtLte p)))
                                (= "dep_1" (opt (.deploymentId p)))
                                (= true (opt (.hasError p)))
                                (= "manual" (.asString (opt (.triggerType p))))])}
               {:name '->environment-list-params :args [{}]
                :check (fn [p] [(= true (opt (.includeArchived p)))])}
               {:name '->vault-list-params :args [{}]
                :check (fn [p] [(= true (opt (.includeArchived p)))])}
               {:name '->tunnel-list-params :args [{}]
                :check (fn [p] [(= true (opt (.includeArchived p)))])}
               {:name '->certificate-list-params :args ["tun_1" {}]
                :check (fn [p] [(= "tun_1" (opt (.tunnelId p)))
                                (= true (opt (.includeArchived p)))])}
               {:name '->dream-list-params :args [{}]
                :check (fn [p] [(= ts (opt (.createdAtGt p)))
                                (= ts (opt (.createdAtLt p)))
                                (= true (opt (.includeArchived p)))
                                (= 1 (count (opt (.statuses p))))])}
               {:name '->credential-list-params :args ["vault_1" {}]
                :check (fn [p] [(= "vault_1" (opt (.vaultId p)))
                                (= true (opt (.includeArchived p)))])}
               {:name '->user-profile-list-params :args [{}]
                :check (fn [p] [(= "desc" (.asString (opt (.order p))))])}]]
    (doseq [{:keys [name args check]} cases]
      (let [opts (cond-> (merge {:limit 7 :page "page" :betas [:beta-test]
                         :created-at-gt ts :created-at-gte ts
                         :created-at-lt ts :created-at-lte ts
                         :include-archived true
                         :agent-id "agent_1" :agent-version 2
                         :deployment-id "dep_1" :memory-store-id "store_1"
                         :order :desc :statuses [:running :idle]
                         :status :active :has-error true :trigger-type :manual
                         :source "custom"}
                        (last args))
                   (= name '->dream-list-params) (assoc :statuses [:pending]))
            actual-args (if (seq args)
                          (conj (vec (butlast args)) opts)
                          [opts])]
        (if-let [f (private-fn name)]
          (let [p (try (apply f actual-args)
                       (catch clojure.lang.ArityException _ nil))]
            (is (and p (every? true? (check p)))
              (str name " options must reach the SDK params"))
          )
          (is false (str name " options must reach the SDK params")))))))

(deftest beta-list-enum-values-reject-unknowns
  (if-let [f (private-fn '->session-list-params)]
    (let [data (ex-data-for #(f {:order :not-real}))]
      (is (contains? data :anthropic/error)))
    (is false "->session-list-params must reject unknown enum values")))

(defn- agent-ref ^BetaManagedAgentsAgentReference []
  (-> (BetaManagedAgentsAgentReference/builder)
      (.id "agent_1")
      (.version 1)
      (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentReference$Type/of "agent"))
      (.build)))

(defn- agent-message-event [id]
  (-> (BetaManagedAgentsAgentMessageEvent/builder)
      (.id id)
      (.content [])
      (.processedAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
      (.type (BetaManagedAgentsAgentMessageEvent$Type/of "agent_message"))
      (.build)))

(deftest event-stream-params
  (let [p (->session-event-stream-params "sess_1" {:event-deltas [:agent-message]})]
    (is (= "sess_1" (opt (.sessionId p))))
    (is (= [BetaManagedAgentsDeltaType/AGENT_MESSAGE] (opt (.eventDeltas p)))))
  (let [p (->thread-event-stream-params "sess_1" "thread_1"
                                        {:event-deltas [:agent-message]})]
    (is (= "sess_1" (.sessionId p)))
    (is (= "thread_1" (opt (.threadId p))))
    (is (= [BetaManagedAgentsDeltaType/AGENT_MESSAGE] (opt (.eventDeltas p))))))

(deftest event-stream-conversion-and-consumption
  (let [session-event (BetaManagedAgentsStreamSessionEvents/ofAgentMessage
                       (agent-message-event "event_1"))
        thread-event (BetaManagedAgentsStreamSessionThreadEvents/ofAgentMessage
                      (agent-message-event "event_2"))]
    (with-redefs-fn {stream-event-json
                      (fn [event]
                        (JsonValue/from (java.util.Map/of
                                         "type" (if (= event session-event)
                                                  "agent.message"
                                                  "session.status_running")
                                         "id" (if (= event session-event) "event_1" "event_2"))))}
      (fn []
      (is (= {:type :agent-message :id "event_1"}
             (select-keys (stream-event->map session-event) [:type :id])))
      (is (= {:type :session-status-running :id "event_2"}
             (select-keys (stream-event->map thread-event) [:type :id])))
      (let [closed? (atom false)
            seen (atom [])
            sr (reify StreamResponse
                 (stream [_] (.stream (java.util.ArrayList. [session-event thread-event])))
                 (close [_] (reset! closed? true)))]
        (is (= [:agent-message :session-status-running]
               (mapv :type (consume-event-stream sr #(swap! seen conj %)))))
        (is (= [:agent-message :session-status-running] (mapv :type @seen)))
        (is @closed?))))))

(deftest tunnel-params-and-response-mapping
  (let [p (->tunnel-create-params {:display-name "Local"})]
    (is (= "Local" (opt (.displayName p)))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (com.anthropic.models.beta.tunnels.BetaTunnel/builder)
              (.id "tun_1") (.archivedAt (java.util.Optional/empty))
              (.createdAt ts) (.displayName "Local") (.domain "localhost")
              (.type (com.anthropic.core.JsonValue/from "tunnel")) (.build))]
    (is (= {:id "tun_1" :display-name "Local" :domain "localhost"
            :created-at "2026-07-04T00:00Z" :type :tunnel}
           (tunnel->map r)))))

(deftest agent-version-params
  (let [p (->agent-version-list-params "agent_1" {:limit 10 :page "next"})]
    (is (= "agent_1" (opt (.agentId p))))
    (is (= 10 (opt (.limit p))))
    (is (= "next" (opt (.page p))))))

(deftest tunnel-certificate-params-and-response-mapping
  (let [p (->tunnel-certificate-create-params "tun_1" {:ca-certificate-pem "pem"})]
    (is (= "tun_1" (opt (.tunnelId p))))
    (is (= "pem" (.caCertificatePem p))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (com.anthropic.models.beta.tunnels.certificates.BetaTunnelCertificate/builder)
              (.id "cert_1") (.archivedAt (java.util.Optional/empty)) (.createdAt ts)
              (.expiresAt (java.util.Optional/empty)) (.fingerprint "fp") (.tunnelId "tun_1")
              (.type (com.anthropic.core.JsonValue/from "tunnel_certificate")) (.build))]
    (is (= {:id "cert_1" :tunnel-id "tun_1" :fingerprint "fp"
            :created-at "2026-07-04T00:00Z"}
           (tunnel-certificate->map r)))))

(deftest thread-event-params
  (let [p (->thread-event-list-params "sess_1" "thread_1" {:limit 10 :page "next"})]
    (is (= "sess_1" (.sessionId p)))
    (is (= "thread_1" (opt (.threadId p))))
    (is (= 10 (opt (.limit p))))))

(deftest session-resource-params
  (let [p (->session-resource-list-params "sess_1" {:limit 10 :page "next"})]
    (is (= "sess_1" (opt (.sessionId p))))
    (is (= 10 (opt (.limit p))))))

(deftest dream-params
  (let [p (->dream-create-params {:inputs [] :model "claude-opus-4-8" :instructions "dream"
                                  :output-behavior {:type :update-existing
                                                    :memory-store-id "store_1"}})]
    (is (= "claude-opus-4-8" (.asString (.model p))))
    (is (= "dream" (opt (.instructions p))))
    (is (.isUpdateExisting (opt (.outputBehavior p))))
    (is (= "store_1" (.memoryStoreId (.asUpdateExisting (opt (.outputBehavior p))))))))

(deftest dream-params-output-behavior-is-optional
  (let [p (->dream-create-params {:inputs [] :model "claude-opus-4-8"})]
    (is (not (.isPresent (.outputBehavior p))))))

(deftest dream-response-enum-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        dream (-> (com.anthropic.models.beta.dreams.BetaDream/builder)
                  (.id "dream_1") (.archivedAt (java.util.Optional/empty))
                  (.endedAt (java.util.Optional/empty))
                  (.error (java.util.Optional/empty))
                  (.instructions (java.util.Optional/empty))
                  (.sessionId (java.util.Optional/empty))
                  (.createdAt ts) (.inputs []) (.outputs [])
                  (.model (-> (com.anthropic.models.beta.dreams.BetaDreamModelConfig/builder)
                              (.id "claude-opus-4-8") (.build)))
                  (.outputBehavior
                   (com.anthropic.models.beta.dreams.BetaOutputBehavior/ofCreateNew
                    (com.anthropic.models.beta.dreams.BetaOutputBehaviorCreateNew$Type/of "create_new")))
                  (.status (com.anthropic.models.beta.dreams.BetaDreamStatus/of "running"))
                  (.type (com.anthropic.models.beta.dreams.BetaDream$Type/of "dream"))
                  (.usage (-> (com.anthropic.models.beta.dreams.BetaDreamUsage/builder)
                              (.cacheCreationInputTokens 0) (.cacheReadInputTokens 0)
                              (.inputTokens 0) (.outputTokens 0) (.build)))
                  (.build))
        mapped (dream->map dream)]
    (is (= :running (:status mapped)))
    (is (= :dream (:type mapped)))
    (is (= {:type :create-new} (:output-behavior mapped)))))

(deftest skill-params
  (let [tmp (doto (java.io.File/createTempFile "skill" ".md") (spit "content"))
        ^SkillCreateParams p (->skill-create-params {:display-title "My Skill"
                                                     :files [(.getPath tmp)]})]
    (is (some? p)))
  (testing "missing keys throw"
    (is (= {:anthropic/error :missing-key :key :files}
           (ex-data-for #(->skill-create-params {:display-title "x"}))))))

(deftest memory-store-params
  (let [^MemoryStoreCreateParams p (->memory-store-create-params
                                    {:name "notes" :description "d" :metadata {:team "x"}})]
    (is (= "notes" (.name p)))
    (is (= "d" (opt (.description p)))))
  (let [^MemoryStoreUpdateParams p (->memory-store-update-params
                                    "ms_1" {:name "renamed"})]
    (is (= "renamed" (opt (.name p)))))
  (is (= {:anthropic/error :missing-key :key :name}
         (ex-data-for #(->memory-store-create-params {})))))

(deftest agent-params
  (let [^AgentCreateParams p (->agent-create-params
                              {:name "helper" :model "claude-opus-4-8"
                               :system "be helpful" :description "d"
                               :skills [{:type :anthropic :skill-id "web" :version "1"}
                                        {:type :custom :skill-id "skill_1"}]
                               :mcp-servers [{:name "github" :url "https://mcp.example.test"}]
                               :tools [{:type :custom
                                        :name "lookup"
                                        :description "Look up a thing"
                                        :input-schema {:type "object"}}
                                       {:type :mcp-toolset
                                        :mcp-server-name "github"}]})]
    (is (= "helper" (.name p)))
    (is (= "be helpful" (opt (.system p))))
    (is (= 2 (count (opt (.skills p)))))
    (is (= 1 (count (opt (.mcpServers p)))))
    (is (= 2 (count (opt (.tools p))))))
  (let [^AgentCreateParams p (->agent-create-params
                              {:name "helper" :model "claude-opus-4-8" :effort :high})]
    (is (true? (.isBetaManagedAgentsModelConfigParams (.model p))))
    (when (.isBetaManagedAgentsModelConfigParams (.model p))
      (is (= "high" (-> p .model .asBetaManagedAgentsModelConfigParams .effort opt
                         .asBetaManagedAgentsEffortLevel .asString)))))
  (let [^AgentUpdateParams p (->agent-update-params
                              "agent_1"
                              {:version 2
                               :system "new"
                               :skills [{:type :custom :skill-id "skill_1" :version "2"}]
                               :mcp-servers [{:name "github" :url "https://mcp.example.test"}]
                               :tools [{:type :mcp-toolset :mcp-server-name "github"}]})]
    (is (= 2 (opt (.version p))))
    (is (= "new" (opt (.system p))))
    (is (= 1 (count (opt (.skills p)))))
    (is (= 1 (count (opt (.mcpServers p)))))
    (is (= 1 (count (opt (.tools p))))))
  (is (= {:anthropic/error :unknown-skill-type :type :x}
         (ex-data-for #(->agent-create-params
                        {:name "helper" :model "m"
                         :skills [{:type :x :skill-id "skill_1"}]}))))
  (is (= {:anthropic/error :unknown-tool-type :type :x}
         (ex-data-for #(->agent-create-params
                        {:name "helper" :model "m"
                         :tools [{:type :x}]}))))
  (is (nil? (opt (.version (->agent-update-params "agent_1" {:system "new"})))))
  (is (= {:anthropic/error :missing-key :key :name}
         (ex-data-for #(->agent-create-params {:model "m"}))))
  (is (= {:anthropic/error :missing-key :key :model}
         (ex-data-for #(->agent-create-params {:name "n"})))))

(deftest agents-platform-request-param-parity
  (let [^AgentCreateParams agent-create (->agent-create-params
                                          {:name "helper" :model "m"
                                           :betas ["beta-create" :beta-keyword]})
        ^AgentUpdateParams agent-update (->agent-update-params
                                          "agent_1" {:betas [:beta-update]})
        ^SessionCreateParams session-create (->session-create-params
                                              {:agent "agent_1"
                                               :environment-id "env_1"
                                               :betas ["beta-session" :session-keyword]
                                               :resources [{:type :file :file-id "file_1" :mount-path "/tmp/file"}
                                                           {:type :github-repository :url "https://github.com/acme/repo"
                                                            :authorization-token "token" :mount-path "/tmp/repo"}
                                                           {:type :memory-store :memory-store-id "ms_1"}]
                                               :vault-ids ["vault_1"]})
        ^SessionUpdateParams session-update (->session-update-params
                                              "sess_1"
                                              {:betas [:update-beta]
                                               :agent {:mcp-servers [{:name "github" :url "https://mcp.example.test"}]}
                                               :vault-ids ["vault_2"]})
        ^DeploymentCreateParams deployment-create (->deployment-create-params
                                                    {:name "d" :agent "a" :environment-id "e"
                                                     :initial-events [] :betas [:deployment-beta]})
        ^DeploymentUpdateParams deployment-update (->deployment-update-params
                                                    "dep_1" {:betas ["deployment-update-beta"]})
        ^EnvironmentCreateParams environment-create (->environment-create-params
                                                      {:name "env" :betas [:environment-beta]})
        ^EnvironmentUpdateParams environment-update (->environment-update-params
                                                      "env_1" {:betas ["environment-update-beta"]})
        ^VaultCreateParams vault-create (->vault-create-params
                                          {:display-name "vault" :betas [:vault-beta]})
        ^VaultUpdateParams vault-update (->vault-update-params
                                          "vault_1" {:betas ["vault-update-beta"]})]
    (is (= ["beta-create" "beta-keyword"] (mapv #(.asString %) (opt (.betas agent-create)))))
    (is (= ["beta-update"] (mapv #(.asString %) (opt (.betas agent-update)))))
    (is (= ["beta-session" "session-keyword"] (mapv #(.asString %) (opt (.betas session-create)))))
    (is (= ["update-beta"] (mapv #(.asString %) (opt (.betas session-update)))))
    (is (= ["vault_1"] (opt (.vaultIds session-create))))
    (is (= ["vault_2"] (opt (.vaultIds session-update))))
    (is (= 3 (count (opt (.resources session-create)))))
    (is (= "file_1" (.fileId (.asFile (first (opt (.resources session-create)))))))
    (is (= "https://github.com/acme/repo"
           (.url (.asGitHubRepository (second (opt (.resources session-create)))))))
    (is (= "ms_1" (.memoryStoreId (.asMemoryStore (nth (opt (.resources session-create)) 2)))))
    (is (= 1 (count (opt (.mcpServers (opt (.agent session-update)))))))
    (is (= ["deployment-beta"] (mapv #(.asString %) (opt (.betas deployment-create)))))
    (is (= ["deployment-update-beta"] (mapv #(.asString %) (opt (.betas deployment-update)))))
    (is (= ["environment-beta"] (mapv #(.asString %) (opt (.betas environment-create)))))
    (is (= ["environment-update-beta"] (mapv #(.asString %) (opt (.betas environment-update)))))
    (is (= ["vault-beta"] (mapv #(.asString %) (opt (.betas vault-create)))))
    (is (= ["vault-update-beta"] (mapv #(.asString %) (opt (.betas vault-update))))))
  (doseq [[resource key] [[{:type :file} :file-id]
                          [{:type :github-repository :authorization-token "token"} :url]
                          [{:type :github-repository :url "https://x.test"} :authorization-token]
                          [{:type :memory-store} :memory-store-id]]]
    (let [d (ex-data-for #(->session-create-params
                           {:agent "agent_1" :environment-id "env_1" :resources [resource]}))]
      (is (= :missing-key (:anthropic/error d)))
      (is (= key (:key d))))))

(deftest agent-multiagent-params
  (let [multiagent {:type :coordinator
                    :agents ["agent_1"
                             {:type :agent :id "agent_2"}
                             {:type :agent :id "agent_3" :version 4}
                             {:type :self}
                             {:type :advisor :model "claude-opus-4-8"}]}
        ^AgentCreateParams create (->agent-create-params {:name "helper"
                                                           :model "claude-opus-4-8"
                                                           :multiagent multiagent})
        ^AgentUpdateParams update (->agent-update-params "agent_0"
                                                         {:multiagent multiagent})
        ^com.anthropic.models.beta.sessions.BetaManagedAgentsMultiagentParams cm
        (opt (.multiagent create))]
    (is (= "coordinator" (.asString (.type cm))))
    (is (= 5 (count (.agents cm))))
    (let [entries (.agents cm)]
      (is (= "agent_1" (.asString (first entries))))
      (is (= "agent_2" (.id (.asAgent (nth entries 1)))))
      (is (nil? (opt (.version (.asAgent (nth entries 1))))))
      (is (= 4 (opt (.version (.asAgent (nth entries 2))))))
      (is (= "agent_3" (.id (.asAgent (nth entries 2))))))
    (is (some? (opt (.multiagent update))))
    (is (some #(.isString %) (.agents cm)))
    (is (some #(.isAgent %) (.agents cm)))
    (is (some #(.isSelf %) (.agents cm)))
    (is (some #(.isAdvisor %) (.agents cm)))))

(deftest agent-multiagent-unknown-roster-entry
  (let [f (ns-resolve 'anthropic.beta '->agent-roster-entry)]
    (is (ifn? f))
    (when f
      (is (= :unknown-multiagent-roster-entry
             (:anthropic/error
              (ex-data-for #(f (Object.)))))))))

(deftest session-params
  (let [^SessionCreateParams p (->session-create-params
                                {:agent "agent_1" :title "t" :environment-id "env_1"
                                 :initial-events [{:type :user-message :content "hello"}]})]
    (is (some? p))
    (is (= "t" (opt (.title p))))
    (is (= 1 (count (opt (.initialEvents p)))))
    (when-let [event (first (opt (.initialEvents p)))]
      (is (= "hello" (.text (.asText (first (.content (.asUserMessage event)))))))))
  (let [^SessionUpdateParams p (->session-update-params "sess_1" {:title "t2"})]
    (is (= "t2" (opt (.title p)))))
  (is (= {:anthropic/error :missing-key :key :agent}
         (ex-data-for #(->session-create-params {})))))

(deftest beta-253-budget-and-inference-geo-params
  (let [budget {:max-list-cost {:amount "1.25" :currency :usd} :type :limit}
        p (->session-create-params {:agent "agent_1" :environment-id "env_1" :budget budget})
        bp (opt (.budget p))]
    (is (= "1.25" (.amount (.maxListCost bp))))
    (is (= "USD" (.asString (.currency (.maxListCost bp)))))
    (is (= "limit" (.asString (.type bp)))))
  (let [p (->agent-create-params {:name "a" :model "claude-opus-4-8"
                                  :effort :high :inference-geo "us"})]
    (is (= "us" (opt (.inferenceGeo (.asBetaManagedAgentsModelConfigParams (.model p)))))))
  (let [p (->agent-create-params {:name "a" :model "claude-opus-4-8"
                                  :inference-geo "us"})]
    (is (.isBetaManagedAgentsModelConfigParams (.model p)))
    (is (= "us" (opt (.inferenceGeo (.asBetaManagedAgentsModelConfigParams (.model p)))))))
  (let [p (->agent-update-params "agent_1" {:model "claude-opus-4-8" :inference-geo "eu"})]
    (is (.isBetaManagedAgentsModelConfigParams (opt (.model p))))
    (is (= "eu" (opt (.inferenceGeo (.asBetaManagedAgentsModelConfigParams (opt (.model p))))))))

(deftest beta-253-speed-model-config
  (doseq [p [(->agent-create-params {:name "helper" :model "m" :speed :fast})
             (->agent-update-params "agent_1" {:model "m" :speed :fast})]]
    (is (= "fast" (.asString (opt (.speed (.asBetaManagedAgentsModelConfigParams
                                           (opt (.model p))))))))))
  (let [p (->deployment-update-params "dep_1"
                                      {:budget {:max-list-cost {:amount "2" :currency :usd}
                                                :type :limit}})]
    (is (= "2" (.amount (.maxListCost (opt (.budget p))))))))

(deftest beta-253-budget-response-mapping-and-round-trip
  (let [money (-> (com.anthropic.models.beta.BetaMonetaryAmount/builder)
                  (.amount "1.25") (.currency com.anthropic.models.beta.BetaCurrency/USD) (.build))
        budget (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit/builder)
                   (.maxListCost money)
                   (.type com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit$Type/LIMIT)
                   (.build))
        ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        agent (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent/builder)
                  (.id "agent_1") (.name "a") (.version 1)
                  (.description "d") (.mcpServers []) (.skills []) (.tools [])
                  (.multiagent (java.util.Optional/empty))
                  (.model (-> (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig/builder)
                              (.id (com.anthropic.models.beta.agents.BetaManagedAgentsModel/of "m")) (.build)))
                  (.system "s")
                  (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Type/of "agent"))
                  (.build))
        session (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSession/builder)
                    (.id "sess_1") (.agent agent) (.environmentId "env_1")
                    (.archivedAt (java.util.Optional/empty)) (.title (java.util.Optional/empty))
                    (.createdAt ts) (.updatedAt ts) (.budget budget)
                    (.metadata (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSession$Metadata/builder) (.build)))
                    (.outcomeEvaluations []) (.resources [])
                    (.stats (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionStats/builder) (.build)))
                    (.status (com.anthropic.models.beta.sessions.BetaManagedAgentsSession$Status/of "idle"))
                    (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSession$Type/of "session"))
                    (.usage (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage/builder) (.build)))
                    (.vaultIds []) (.deploymentId (java.util.Optional/empty)) (.build))
        mapped (session->map session)
        round-trip (->session-update-params "sess_1" {:budget (:budget mapped)})]
    (is (every? #(contains? mapped %) [:id :agent :status :created-at :updated-at :stats :type
                                       :metadata :outcome-evaluations :resources :vault-ids
                                       :environment-id :budget :usage]))
    (is (= :idle (:status mapped)))
    (is (= {:max-list-cost {:amount "1.25" :currency :usd} :type :limit} (:budget mapped)))
    (is (= "1.25" (.amount (.maxListCost (opt (.budget round-trip)))))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        budget (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit/builder)
                   (.maxListCost (-> (com.anthropic.models.beta.BetaMonetaryAmount/builder)
                                     (.amount "2") (.currency com.anthropic.models.beta.BetaCurrency/USD) (.build)))
                   (.type com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit$Type/LIMIT) (.build))
        deployment (-> (BetaManagedAgentsDeployment/builder)
                       (.id "dep_1") (.agent (agent-ref)) (.environmentId "env_1")
                       (.name "d") (.initialEvents []) (.metadata (-> (com.anthropic.models.beta.deployments.BetaManagedAgentsDeployment$Metadata/builder) (.build)))
                       (.resources []) (.vaultIds []) (.createdAt ts) (.updatedAt ts)
                       (.status (com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentStatus/of "running"))
                       (.type (com.anthropic.models.beta.deployments.BetaManagedAgentsDeployment$Type/of "deployment"))
                       (.archivedAt (java.util.Optional/empty)) (.description (java.util.Optional/empty))
                       (.pausedReason (java.util.Optional/empty)) (.schedule (java.util.Optional/empty))
                       (.budget budget) (.build))
        mapped (deployment->map deployment)
        round-trip (->deployment-update-params "dep_1" {:budget (:budget mapped)})]
    (is (= {:max-list-cost {:amount "2" :currency :usd} :type :limit} (:budget mapped)))
    (is (= :running (:status mapped)))
    (is (= "2" (.amount (.maxListCost (opt (.budget round-trip))))))))

(deftest beta-253-session-updated-budget-mapping
  (let [budget (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit/builder)
                   (.maxListCost (-> (com.anthropic.models.beta.BetaMonetaryAmount/builder)
                                     (.amount "3") (.currency com.anthropic.models.beta.BetaCurrency/USD) (.build)))
                   (.type com.anthropic.models.beta.sessions.BetaManagedAgentsBudgetLimit$Type/LIMIT) (.build))
        event (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUpdatedEvent/builder)
                  (.id "evt_1") (.processedAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                  (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUpdatedEvent$Type/of "session_updated"))
                  (.agent (java.util.Optional/empty))
                  (.title (java.util.Optional/empty)) (.budget budget) (.build))]
    (is (= {:type :session-updated :id "evt_1" :processed-at "2026-07-04T00:00Z"
            :budget {:max-list-cost {:amount "3" :currency :usd} :type :limit}}
           (session-event->map (BetaManagedAgentsSessionEvent/ofSessionUpdated event))))))

(deftest beta-253-memory-list-item-mapping
  (let [memory (-> (BetaManagedAgentsMemory/builder)
                   (.id "mem_1") (.contentSha256 "sha") (.contentSizeBytes 1)
                   (.createdAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                   (.memoryStoreId "ms_1") (.memoryVersionId "mv_1") (.path "/a")
                   (.type (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemory$Type/of "memory"))
                   (.updatedAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                   (.content (java.util.Optional/empty)) (.build))
        prefix (-> (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemoryPrefix/builder)
                   (.path "/")
                   (.type (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemoryPrefix$Type/of "memory_prefix"))
                   (.build))]
    (is (= :memory (:type (memory-list-item->map (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemoryListItem/ofMemory memory)))))
    (is (= :memory-prefix (:type (memory-list-item->map (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemoryListItem/ofMemoryPrefix prefix)))))))

(deftest beta-253-usage-and-session-usage-event-mapping
  (let [money (-> (com.anthropic.models.beta.BetaMonetaryAmount/builder)
                  (.amount "0.50")
                  (.currency com.anthropic.models.beta.BetaCurrency/USD)
                  (.build))
        tools (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsServerToolUsage/builder)
                  (.webFetchRequests 2) (.webSearchRequests 3) (.build))
        usage (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsage/builder)
                  (.activeSeconds 4.5) (.cacheReadInputTokens 7) (.inputTokens 8)
                  (.listCost money) (.outputTokens 9) (.serverToolUse tools) (.build))
        f (ns-resolve 'anthropic.beta 'usage->map)]
    (is (= {:active-seconds 4.5 :cache-read-input-tokens 7 :input-tokens 8
            :list-cost {:amount "0.50" :currency :usd} :output-tokens 9
            :server-tool-use {:web-fetch-requests 2 :web-search-requests 3}}
           (f usage))))
  (let [snapshot (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionUsageSnapshot/builder)
                     (.activeSeconds 1.0) (.inputTokens 10) (.build))
        event (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsageEvent/builder)
                  (.id "evt_usage")
                  (.processedAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                  (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionUsageEvent$Type/of "session_usage"))
                  (.usage snapshot) (.build))]
    (is (= {:type :session-usage :id "evt_usage" :processed-at "2026-07-04T00:00Z"
            :usage {:active-seconds 1.0 :input-tokens 10}}
           (session-event->map
            (BetaManagedAgentsSessionEvent/ofSessionUsage event))))))

(deftest session-event-params
  (let [^BetaManagedAgentsEventParams user (->session-event
                                            {:type :user-message
                                             :content "hello"})
        ^BetaManagedAgentsEventParams system (->session-event
                                              {:type :system-message
                                               :content "rules"})
        ^BetaManagedAgentsEventParams outcome (->session-event
                                               {:type :user-define-outcome
                                                :description "done"
                                                :rubric {:type :text :text "Looks good"}
                                                :max-iterations 3})
        ^EventSendParams p (->event-send-params "sess_1" [{:type :user-message
                                                           :content "hello"}])]
    (is (.isUserMessage user))
    (is (= "hello" (-> user .asUserMessage .content first .asText .text)))
    (is (.isSystemMessage system))
    (is (= "rules" (-> system .asSystemMessage .content first .text)))
    (is (.isUserDefineOutcome outcome))
    (is (= 3 (opt (-> outcome .asUserDefineOutcome .maxIterations))))
    (is (= "sess_1" (opt (.sessionId p))))
    (is (= 1 (count (.events p)))))
  (is (= {:anthropic/error :unknown-event-type :type :x}
         (ex-data-for #(->session-event {:type :x :content "no"}))))
  (is (= {:anthropic/error :missing-key :key :content}
         (ex-data-for #(->session-event {:type :user-message}))))
  (is (= {:anthropic/error :missing-key :key :description}
         (ex-data-for #(->session-event {:type :user-define-outcome
                                         :rubric {:type :text :text "ok"}})))))

(deftest beta5-request-field-parity
  (let [hb ((ns-resolve 'anthropic.beta '->environment-work-heartbeat-params)
            "env" "work" {:desired-ttl-seconds 30 :expected-last-heartbeat "t"})
        poll ((ns-resolve 'anthropic.beta '->environment-work-poll-params)
              "env" {:block-ms 10 :reclaim-older-than-ms 20 :anthropic-worker-id "worker"})
        mv ((ns-resolve 'anthropic.beta '->memory-version-list-params)
            "store" {:api-key-id "key" :created-at-gte "2026-01-01T00:00:00Z"
                      :created-at-lte "2026-01-02T00:00:00Z" :session-id "session"})
        update ((ns-resolve 'anthropic.beta '->memory-update-params)
                "store" "memory" {:precondition {:type :if-unmodified
                                                   :content-sha256 "sha"}})]
    (is (= 30 (opt (.desiredTtlSeconds hb))))
    (is (= "t" (opt (.expectedLastHeartbeat hb))))
    (is (= 10 (opt (.blockMs poll))))
    (is (= 20 (opt (.reclaimOlderThanMs poll))))
    (is (= "worker" (opt (.anthropicWorkerId poll))))
    (is (= "key" (opt (.apiKeyId mv))))
    (is (= "session" (opt (.sessionId mv))))
    (is (= "sha" (opt (.contentSha256 (opt (.precondition update))))))
    (doseq [[type predicate]
            [[:user-interrupt #(.isUserInterrupt %)]
             [:user-tool-confirmation #(.isUserToolConfirmation %)]
             [:user-custom-tool-result #(.isUserCustomToolResult %)]
             [:user-tool-result #(.isUserToolResult %)]]]
      (is (predicate (->session-event
                      (merge {:type type}
                             (case type
                               :user-tool-confirmation {:tool-use-id "tool" :result :allow}
                               :user-custom-tool-result {:custom-tool-use-id "custom" :content [{:type :text :text "ok"}]}
                               :user-tool-result {:tool-use-id "tool" :content [{:type :text :text "ok"}]}
                               {}))))))))

(deftest deployment-params
  (let [^DeploymentCreateParams p (->deployment-create-params
                                   {:name "nightly"
                                    :agent "agent_1"
                                    :environment-id "env_1"
                                    :initial-events [{:type :user-message
                                                      :content "start"}]})]
    (is (= "nightly" (.name p)))
    (is (= "env_1" (.environmentId p)))
    (is (.isUserMessage (first (.initialEvents p)))))
  (let [^DeploymentUpdateParams p (->deployment-update-params
                                   "dep_1"
                                   {:name "renamed"
                                    :initial-events [{:type :system-message
                                                      :content "rules"}]})]
    (is (= "dep_1" (opt (.deploymentId p))))
    (is (= "renamed" (opt (.name p))))
    (is (.isSystemMessage (first (opt (.initialEvents p))))))
  (let [^DeploymentRunParams p (->deployment-run-params "dep_1")]
    (is (= "dep_1" (opt (.deploymentId p)))))
  (is (= {:anthropic/error :missing-key :key :name}
         (ex-data-for #(->deployment-create-params
                        {:agent "agent_1" :environment-id "env_1" :initial-events []}))))
  (is (= {:anthropic/error :missing-key :key :agent}
         (ex-data-for #(->deployment-create-params
                        {:name "n" :environment-id "env_1" :initial-events []}))))
  (is (= {:anthropic/error :missing-key :key :environment-id}
         (ex-data-for #(->deployment-create-params
                        {:name "n" :agent "agent_1" :initial-events []}))))
  (is (= {:anthropic/error :missing-key :key :initial-events}
         (ex-data-for #(->deployment-create-params
                        {:name "n" :agent "agent_1" :environment-id "env_1"})))))

(deftest deployment-agent-fields-round-trip
  (let [resources [{:type :file :file-id "file_1" :mount-path "/tmp/file"}
                   {:type :github-repository :url "https://github.com/acme/repo"
                    :authorization-token "token"
                    :mount-path "/tmp/repo"}
                   {:type :memory-store :memory-store-id "ms_1"}]
        schedule {:expression "0 0 * * *" :timezone "UTC" :type :cron}
        ^DeploymentCreateParams p (->deployment-create-params
                                    {:name "nightly" :agent "agent_1" :environment-id "env_1"
                                     :initial-events [] :resources resources :schedule schedule})
        ^DeploymentUpdateParams u (->deployment-update-params "dep_1"
                                    {:resources resources :schedule schedule})]
    (is (= 3 (count (opt (.resources p)))))
    (is (= ["file_1" "https://github.com/acme/repo" "ms_1"]
           (mapv (fn [r] (cond (.isFile r) (.fileId (.asFile r))
                               (.isGitHubRepository r) (.url (.asGitHubRepository r))
                               :else (.memoryStoreId (.asMemoryStore r))))
                 (opt (.resources p)))))
    (is (= "0 0 * * *" (.expression (opt (.schedule p)))))
    (is (= 3 (count (opt (.resources u)))))
    (is (= "UTC" (.timezone (opt (.schedule u)))))))

(deftest deployment-unknown-resource-type
  (is (= :unknown-deployment-resource-type
         (:anthropic/error
          (ex-data-for #(->deployment-create-params
                         {:name "n" :agent "a" :environment-id "e" :initial-events []
                          :resources [{:type :unknown}]}))))))

(deftest deployment-resource-required-keys
  ;; The SDK rejects a null on these fields. A missing key must produce the
  ;; library's :missing-key error, not a null pointer from the SDK.
  (doseq [[resource k] [[{:type :file} :file-id]
                        [{:type :github-repository :authorization-token "t"} :url]
                        [{:type :github-repository :url "https://x.test"} :authorization-token]
                        [{:type :memory-store} :memory-store-id]]]
    (let [d (ex-data-for #(->deployment-create-params
                           {:name "n" :agent "a" :environment-id "e" :initial-events []
                            :resources [resource]}))]
      (is (= :missing-key (:anthropic/error d)) (str "for " resource))
      (is (= k (:key d)) (str "for " resource)))))

(deftest session-thread-params
  (let [^ThreadRetrieveParams rp (->thread-retrieve-params "sess_1" "thread_1")
        ^ThreadListParams lp (->thread-list-params "sess_1")
        ^ThreadArchiveParams ap (->thread-archive-params "sess_1" "thread_1")]
    (is (= "sess_1" (.sessionId rp)))
    (is (= "thread_1" (opt (.threadId rp))))
    (is (= "sess_1" (opt (.sessionId lp))))
    (is (= "sess_1" (.sessionId ap)))
    (is (= "thread_1" (opt (.threadId ap))))))

(deftest memory-params
  (let [^MemoryCreateParams cp (->memory-create-params
                                "ms_1" {:path "/notes/a.md"
                                        :content "hello"
                                        :view "full"})
        ^MemoryRetrieveParams rp (->memory-retrieve-params "ms_1" "mem_1")
        ^MemoryUpdateParams up (->memory-update-params
                                "ms_1" "mem_1" {:path "/notes/b.md"
                                                :content "updated"})
        ^MemoryListParams lp (->memory-list-params "ms_1" {:path-prefix "/notes"
                                                           :depth 1})
        ^MemoryDeleteParams dp (->memory-delete-params
                                "ms_1" "mem_1" {:expected-content-sha256 "abc"})]
    (is (= "ms_1" (opt (.memoryStoreId cp))))
    (is (= "/notes/a.md" (.path cp)))
    (is (= "hello" (opt (.content cp))))
    (is (= "mem_1" (opt (.memoryId rp))))
    (is (= "updated" (opt (.content up))))
    (is (= "/notes" (opt (.pathPrefix lp))))
    (is (= "abc" (opt (.expectedContentSha256 dp)))))
  (is (= {:anthropic/error :missing-key :key :path}
         (ex-data-for #(->memory-create-params "ms_1" {:content "x"})))))

(deftest skill-version-params
  (let [tmp (doto (java.io.File/createTempFile "skill-version" ".md") (spit "content"))
        ^VersionCreateParams cp (->version-create-params "skill_1" {:files [(.getPath tmp)]})
        ^VersionRetrieveParams rp (->version-retrieve-params "skill_1" "2")
        ^VersionListParams lp (->version-list-params "skill_1")
        ^VersionDeleteParams dp (->version-delete-params "skill_1" "2")
        ^VersionDownloadParams dl (->version-download-params "skill_1" "2")]
    (is (= "skill_1" (opt (.skillId cp))))
    (is (= 1 (count (.files cp))))
    (is (= "2" (opt (.version rp))))
    (is (= "skill_1" (opt (.skillId lp))))
    (is (= "2" (opt (.version dp))))
    (is (= "2" (opt (.version dl)))))
  (is (= {:anthropic/error :missing-key :key :files}
         (ex-data-for #(->version-create-params "skill_1" {})))))

(deftest environment-params
  (let [^EnvironmentCreateParams p (->environment-create-params
                                    {:name "prod" :description "Production" :metadata {:team "x"}})]
    (is (= "prod" (.name p)))
    (is (= "Production" (opt (.description p)))))
  (let [^EnvironmentUpdateParams p (->environment-update-params "env_1" {:name "renamed"})]
    (is (= "env_1" (opt (.environmentId p))))
    (is (= "renamed" (opt (.name p)))))
  (is (= {:anthropic/error :missing-key :key :name}
         (ex-data-for #(->environment-create-params {})))))

(deftest environment-agent-fields-round-trip
  (let [config {:type :self-hosted}
        ^EnvironmentCreateParams p (->environment-create-params
                                    {:name "prod" :config config :scope :organization})
        ^EnvironmentUpdateParams u (->environment-update-params "env_1"
                                    {:config config :scope :account})]
    (is (.isSelfHosted (opt (.config p))))
    (is (= "organization" (.asString (opt (.scope p)))))
    (is (.isSelfHosted (opt (.config u))))
    (is (= "account" (.asString (opt (.scope u)))))))

(deftest environment-unknown-config-type
  (is (= :unknown-environment-config-type
         (:anthropic/error
          (ex-data-for #(->environment-create-params
                         {:name "prod" :config {:type :unknown}}))))))

(deftest environment-work-params
  (let [^WorkRetrieveParams retrieve (->environment-work-retrieve-params "env_1" "work_1")
        ^WorkUpdateParams update (->environment-work-update-params "env_1" "work_1" {:metadata {:team "x"}})
        ^WorkListParams list (->environment-work-list-params "env_1" {:limit 10 :page "next"})
        ^WorkAckParams ack (->environment-work-ack-params "env_1" "work_1")
        ^WorkHeartbeatParams heartbeat (->environment-work-heartbeat-params "env_1" "work_1")
        ^WorkPollParams poll (->environment-work-poll-params "env_1")
        ^WorkStatsParams stats (->environment-work-stats-params "env_1")
        ^WorkStopParams stop (->environment-work-stop-params "env_1" "work_1" {:force true})]
    (is (= "env_1" (.environmentId retrieve)))
    (is (= "work_1" (opt (.workId retrieve))))
    (is (= "env_1" (.environmentId update)))
    (is (= "x" (.convert (get (._additionalProperties (.metadata (.betaSelfHostedWorkUpdateRequest update))) "team") String)))
    (is (= "env_1" (opt (.environmentId list))))
    (is (= 10 (opt (.limit list))))
    (is (= "next" (opt (.page list))))
    (is (= "work_1" (opt (.workId ack))))
    (is (= "work_1" (opt (.workId heartbeat))))
    (is (= "env_1" (opt (.environmentId poll))))
    (is (= "env_1" (opt (.environmentId stats))))
    (is (= "work_1" (opt (.workId stop))))
    (is (true? (opt (.force (.betaSelfHostedWorkStopRequest stop)))))))

(deftest vault-params
  (let [^VaultCreateParams p (->vault-create-params
                              {:display-name "Main Vault" :metadata {:team "x"}})]
    (is (= "Main Vault" (.displayName p))))
  (let [^VaultUpdateParams p (->vault-update-params "vault_1" {:display-name "Renamed"})]
    (is (= "vault_1" (opt (.vaultId p))))
    (is (= "Renamed" (opt (.displayName p)))))
  (is (= {:anthropic/error :missing-key :key :display-name}
         (ex-data-for #(->vault-create-params {})))))

(deftest user-profile-params
  (let [^UserProfileCreateParams p (->user-profile-create-params
                                    {:name "Ada" :external-id "ada-1" :metadata {:team "x"}
                                     :relationship :external :access-type :application})]
    (is (= "Ada" (opt (.name p))))
    (is (= "ada-1" (opt (.externalId p))))
    (is (= "external" (some-> (.relationship p) opt .asString)))
    (is (= "application" (some-> (.accessType p) opt .asString))))
  (let [^UserProfileUpdateParams p (->user-profile-update-params "up_1"
                                                                  {:name "Ada L"
                                                                   :relationship :internal
                                                                   :access-type :passthrough})]
    (is (= "up_1" (opt (.userProfileId p))))
    (is (= "Ada L" (opt (.name p))))
    (is (= "internal" (.asString (opt (.relationship p)))))
    (is (= "passthrough" (.asString (opt (.accessType p))))))
  (is (= :invalid-enum-value
         (:anthropic/error (ex-data-for #(->user-profile-create-params {:relationship :unknown})))))
  (let [^UserProfileCreateEnrollmentUrlParams p
        (->user-profile-enrollment-url-params "up_1")]
    (is (= "up_1" (opt (.userProfileId p))))))

(deftest skill-response-mapping
  (let [r (-> (SkillCreateResponse/builder)
              (.id "skill_1")
              (.displayTitle "My Skill")
              (.latestVersion "3")
              (.source "custom")
              (.type "skill")
              (.createdAt "2026-07-04T00:00:00Z")
              (.updatedAt "2026-07-04T00:00:00Z")
              (.build))
        m (skill-create->map r)]
    (is (= "skill_1" (:id m)))
    (is (= "My Skill" (:display-title m)))
    (is (= :skill (:type m)))
    (is (= "2026-07-04T00:00:00Z" (:created-at m)))))

(deftest memory-store-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsMemoryStore/builder)
              (.id "ms_1")
              (.name "notes")
              (.description "d")
              (.type (com.anthropic.core.JsonValue/from "memory_store"))
              (.createdAt ts)
              (.updatedAt ts)
              (.build))
        m (memory-store->map r)]
    (is (= "ms_1" (:id m)))
    (is (= "notes" (:name m)))
    (is (= :memory-store (:type m)))
    (is (= "d" (:description m)))))

(deftest agent-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        multiagent (-> (BetaManagedAgentsMultiagent/builder)
                       (.agents [(com.anthropic.models.beta.sessions.BetaManagedAgentsMultiagent$Agent/ofAgent
                                  (agent-ref))
                                 (com.anthropic.models.beta.sessions.BetaManagedAgentsMultiagent$Agent/ofAdvisor
                                  (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAdvisor/builder)
                                      (.model "claude-opus-4-8")
                                      (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAdvisor$Type/of "advisor"))
                                      (.build)))])
                       (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsMultiagent$Type/of "coordinator"))
                       (.build))
        r (-> (BetaManagedAgentsAgent/builder)
              (.id "agent_1")
              (.archivedAt (java.util.Optional/empty))
              (.createdAt ts)
              (.description "d")
              (.mcpServers [(-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpServerUrlDefinition/builder)
                                (.name "github")
                                (.type (com.anthropic.models.beta.agents.BetaManagedAgentsMcpServerUrlDefinition$Type/of "url"))
                                (.url "https://mcp.example.test")
                                (.build))])
              (.metadata (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Metadata/builder)
                             (.build)))
              (.model (-> (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig/builder)
                          (.id (com.anthropic.models.beta.agents.BetaManagedAgentsModel/of "claude-opus-4-8"))
                          (.effort (-> (com.anthropic.models.beta.agents.BetaManagedAgentsEffortHigh/builder)
                                       (.type (com.anthropic.models.beta.agents.BetaManagedAgentsEffortHigh$Type/of "high"))
                                       (.build)))
                          (.speed (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig$Speed/of "fast"))
                          (.build)))
              (.multiagent multiagent)
              (.name "helper")
              (.skills [(com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Skill/ofCustom
                         (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomSkill/builder)
                             (.skillId "skill_1")
                             (.type (com.anthropic.models.beta.agents.BetaManagedAgentsCustomSkill$Type/of "custom"))
                             (.version "2")
                             (.build)))])
              (.system "be helpful")
              (.tools [(com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofAgentToolset20260401
                        (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401/builder)
                            (.configs [])
                            (.defaultConfig (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig/builder)
                                                (.enabled true)
                                                (.permissionPolicy
                                                 (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig$PermissionPolicy/ofAlwaysAsk
                                                  (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy/builder)
                                                      (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy$Type/of "always_ask"))
                                                      (.build))))
                                                (.build)))
                            (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401$Type/of "agent_toolset_20260401"))
                            (.build)))
                        (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofMcpToolset
                        (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolset/builder)
                            (.configs [])
                            (.defaultConfig (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig/builder)
                                                (.enabled true)
                                                (.permissionPolicy
                                                 (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig$PermissionPolicy/ofAlwaysAsk
                                                  (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy/builder)
                                                      (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy$Type/of "always_ask"))
                                                      (.build))))
                                                (.build)))
                            (.mcpServerName "github")
                            (.type (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolset$Type/of "mcp_toolset"))
                            (.build)))])
              (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Type/of "agent"))
              (.updatedAt ts)
              (.version 7)
              (.build))
        m (agent->map r)]
    (is (= :agent (:type m)))
    (is (= [{:type :custom :skill-id "skill_1" :version "2"}] (:skills m)))
    (is (= [{:name "github" :url "https://mcp.example.test" :type :url}] (:mcp-servers m)))
    (is (= [{:type :agent-toolset-20260401 :configs [] :default-config {:enabled true :permission-policy {:type :always-ask}}}
            {:type :mcp-toolset :mcp-server-name "github" :configs [] :default-config {:enabled true :permission-policy {:type :always-ask}}}] (:tools m)))
    (is (= :high (:effort m)))
    (is (= :fast (:speed m)))
    (is (= {:type :coordinator
            :agents [{:type :agent :id "agent_1" :version 1}
                     {:type :advisor :model "claude-opus-4-8"}]}
           (:multiagent m)))
    ;; The mapped roster must survive a round trip back through the request
    ;; builder with its values intact, not merely be accepted.
    (let [^AgentCreateParams rt (->agent-create-params {:name "helper"
                                                        :model "claude-opus-4-8"
                                                        :multiagent (:multiagent m)})
          ^com.anthropic.models.beta.sessions.BetaManagedAgentsMultiagentParams rtm
          (opt (.multiagent rt))
          entries (.agents rtm)]
      (is (= "coordinator" (.asString (.type rtm))))
      (is (= 2 (count entries)))
      (is (= "agent_1" (.id (.asAgent (first entries)))))
      (is (= 1 (opt (.version (.asAgent (first entries))))))
      (is (= "claude-opus-4-8" (.model (.asAdvisor (second entries))))))))

(deftest session-agent-model-config-response-mapping
  (let [model (-> (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig/builder)
                  (.id (com.anthropic.models.beta.agents.BetaManagedAgentsModel/of "m"))
                  (.inferenceGeo "eu")
                  (.speed (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig$Speed/of "fast"))
                  (.effort (-> (com.anthropic.models.beta.agents.BetaManagedAgentsEffortHigh/builder)
                               (.type (com.anthropic.models.beta.agents.BetaManagedAgentsEffortHigh$Type/of "high"))
                               (.build)))
                  (.build))
        agent (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent/builder)
                  (.id "agent_1")
                  (.model model)
                  (.name "helper")
                  (.version 1)
                  (.description "d")
                  (.system "s")
                  (.mcpServers [])
                  (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Type/of "agent"))
                  (.skills [])
                  (.tools [])
                  (.multiagent (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionMultiagentCoordinator/builder)
                                   (.agents [])
                                   (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionMultiagentCoordinator$Type/of "coordinator"))
                                   (.build)))
                  (.build))
        m (session-agent->map agent)]
    (is (= :high (:effort m)))
    (is (= "eu" (:inference-geo m)))
    (is (= :fast (:speed m)))))

(deftest session-event-response-mapping
  (let [event (BetaManagedAgentsSessionEvent/ofUserMessage
               (-> (BetaManagedAgentsUserMessageEvent/builder)
                   (.id "evt_1")
                   (.addTextContent "hello")
                   (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent$Type/of "user_message"))
                   (.processedAt (java.util.Optional/empty))
                   (.build)))
        sent (-> (BetaManagedAgentsSendSessionEvents/builder)
                 (.data [])
                 (.build))
        sent-with-data
        (-> (BetaManagedAgentsSendSessionEvents/builder)
            (.data [(com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserMessage
                     (-> (BetaManagedAgentsUserMessageEvent/builder)
                         (.id "evt_2")
                         (.addTextContent "hi")
                         (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent$Type/of "user_message"))
                         (.processedAt (java.util.Optional/empty))
                         (.build)))])
            (.build))
        m (session-event->map event)]
    (is (= :user-message (:type m)))
    (is (= "evt_1" (:id m)))
    (is (= ["hello"] (:content m)))
    (is (= {:data []} (send-session-events->map sent)))
    (is (= {:data [{:type :user-message :id "evt_2" :content ["hi"]}]}
           (send-session-events->map sent-with-data)))))

(deftest send-session-events-response-common-fields
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        user-message (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent/builder)
                         (.id "evt_1") (.addTextContent "hello")
                         (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent$Type/of "user_message"))
                         (.processedAt ts) (.build))
        system-message (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSystemMessageEvent/builder)
                           (.id "evt_1") (.content [])
                           (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSystemMessageEvent$Type/of "system_message"))
                           (.processedAt ts) (.build))
        user-define-outcome (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEvent/builder)
                                (.id "evt_1") (.description "done") (.maxIterations 1)
                                (.outcomeId "outcome_1") (.textRubric "good")
                                (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserDefineOutcomeEvent$Type/of "user_define_outcome"))
                                (.processedAt ts) (.build))
        user-interrupt (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEvent/builder)
                           (.id "evt_1")
                           (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserInterruptEvent$Type/of "user_interrupt"))
                           (.processedAt ts) (.build))
        user-tool-confirmation (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEvent/builder)
                                   (.id "evt_1") (.result com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEvent$Result/ALLOW)
                                   (.toolUseId "tool_1") (.denyMessage "no")
                                   (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserToolConfirmationEvent$Type/of "user_tool_confirmation"))
                                   (.processedAt ts) (.build))
        user-custom-tool-result (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEvent/builder)
                                    (.id "evt_1") (.customToolUseId "custom_1") (.addTextContent "ok")
                                    (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserCustomToolResultEvent$Type/of "user_custom_tool_result"))
                                    (.isError true) (.processedAt ts) (.build))
        user-tool-result (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsUserToolResultEvent/builder)
                             (.id "evt_1") (.toolUseId "tool_1") (.addTextContent "ok")
                             (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsUserToolResultEvent$Type/of "user_tool_result"))
                             (.isError true) (.processedAt ts) (.build))
        variants [[:user-message
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserMessage user-message)
                   (BetaManagedAgentsSessionEvent/ofUserMessage user-message)
                   {:content ["hello"]}]
                  [:system-message
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofSystemMessage system-message)
                   (BetaManagedAgentsSessionEvent/ofSystemMessage system-message)
                   {:content []}]
                  [:user-define-outcome
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserDefineOutcome user-define-outcome)
                   (BetaManagedAgentsSessionEvent/ofUserDefineOutcome user-define-outcome)
                   {:description "done" :max-iterations 1 :outcome-id "outcome_1"
                    :rubric {:type :text :text "good"}}]
                  [:user-interrupt
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserInterrupt user-interrupt)
                   (BetaManagedAgentsSessionEvent/ofUserInterrupt user-interrupt)
                   {}]
                  [:user-tool-confirmation
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserToolConfirmation user-tool-confirmation)
                   (BetaManagedAgentsSessionEvent/ofUserToolConfirmation user-tool-confirmation)
                   {:result :allow :deny-message "no"}]
                  [:user-custom-tool-result
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserCustomToolResult user-custom-tool-result)
                   (BetaManagedAgentsSessionEvent/ofUserCustomToolResult user-custom-tool-result)
                   {:custom-tool-use-id "custom_1" :content [{:type :text :text "ok"}]}]
                  [:user-tool-result
                   (com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data/ofUserToolResult user-tool-result)
                   (BetaManagedAgentsSessionEvent/ofUserToolResult user-tool-result)
                   {:content [{:type :text :text "ok"}]}]]]
    (doseq [[type data receive-event payload] variants]
      (let [response (-> (BetaManagedAgentsSendSessionEvents/builder)
                         (.data [data])
                         (.build))
            mapped (first (:data (send-session-events->map response)))
            received (session-event->map receive-event)]
        (is (= (merge {:type type :id "evt_1" :processed-at "2026-07-04T00:00Z"}
                      (case type
                        :user-tool-confirmation {:tool-use-id "tool_1"}
                        :user-custom-tool-result {:is-error true}
                        :user-tool-result {:tool-use-id "tool_1" :is-error true}
                        {} )
                      payload)
               mapped)
            (str type " send payload"))
        (is (= received mapped)
            (str type " send and receive shapes agree"))))))

(deftest document-source-response-mapping
  (let [sources [(com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Source/ofBase64
                  (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64DocumentSource/builder)
                      (.data "aGVsbG8=") (.mediaType "application/pdf")
                      (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64DocumentSource$Type/of "base64"))
                      (.build)))
                 (com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Source/ofText
                  (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsPlainTextDocumentSource/builder)
                      (.data "hello")
                      (.mediaType (com.anthropic.models.beta.sessions.events.BetaManagedAgentsPlainTextDocumentSource$MediaType/of "text/plain"))
                      (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsPlainTextDocumentSource$Type/of "text"))
                      (.build)))
                 (com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Source/ofUrl
                  (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUrlDocumentSource/builder)
                      (.url "https://example.test/doc.pdf")
                      (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUrlDocumentSource$Type/of "url"))
                      (.build)))
                 (com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Source/ofFile
                  (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileDocumentSource/builder)
                      (.fileId "file_1")
                      (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileDocumentSource$Type/of "file"))
                      (.build)))]]
    (doseq [[source expected] (map vector sources
                                  [{:type :base64 :media-type "application/pdf" :data "aGVsbG8="}
                                   {:type :text :media-type "text/plain" :data "hello"}
                                   {:type :url :url "https://example.test/doc.pdf"}
                                   {:type :file :file-id "file_1"}])]
      (let [content (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent$Content/ofDocument
                     (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock/builder)
                         (.source source)
                         (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsDocumentBlock$Type/of "document"))
                         (.build)))]
        (is (= {:type :document :source expected} (user-content->map content)))
        ))))
    (let [base64 (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64ImageSource/builder)
                     (.data "aGVsbG8=") (.mediaType "application/pdf")
                     (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsBase64ImageSource$Type/of "base64"))
                     (.build))
          url (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUrlImageSource/builder)
                  (.url "https://example.test/doc.pdf")
                  (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsUrlImageSource$Type/of "url"))
                  (.build))
          file (-> (com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileImageSource/builder)
                   (.fileId "file_1")
                   (.type (com.anthropic.models.beta.sessions.events.BetaManagedAgentsFileImageSource$Type/of "file"))
                   (.build))]
      (is (= #{:type :media-type :data}
             (set (keys (image-source->map
                         (com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock$Source/ofBase64 base64)))))
      (is (= #{:type :url}
             (set (keys (image-source->map
                         (com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock$Source/ofUrl url)))))
      (is (= #{:type :file-id}
             (set (keys (image-source->map
                         (com.anthropic.models.beta.sessions.events.BetaManagedAgentsImageBlock$Source/ofFile file)))))))
      ))

(deftest session-thread-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsSessionThread/builder)
              (.id "thread_1")
              (.agent (-> (com.anthropic.models.beta.agents.BetaManagedAgentsSessionThreadAgent/builder)
                          (.id "agent_1")
                          (.description "d")
                          (.mcpServers [])
                          (.model (-> (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig/builder)
                                      (.id (com.anthropic.models.beta.agents.BetaManagedAgentsModel/of "claude-opus-4-8"))
                                      (.build)))
                          (.name "helper")
                          (.skills [])
                          (.system "be helpful")
                          (.tools [])
                          (.version 2)
                          (.type (com.anthropic.models.beta.agents.BetaManagedAgentsSessionThreadAgent$Type/of "agent"))
                          (.build)))
              (.archivedAt (java.util.Optional/empty))
              (.createdAt ts)
              (.parentThreadId (java.util.Optional/empty))
              (.sessionId "sess_1")
              (.stats (java.util.Optional/empty))
              (.status (com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadStatus/of "idle"))
              (.type (com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThread$Type/of "session_thread"))
              (.updatedAt ts)
              (.usage (java.util.Optional/empty))
              (.build))
        m (session-thread->map r)]
    (is (= "thread_1" (:id m)))
    (is (= "sess_1" (:session-id m)))
    (is (= :idle (:status m)))
    (is (= {:type :agent :id "agent_1" :version 2} (:agent m)))))

(deftest session-thread-advisor-agent-mapping
  ;; A thread's agent slot is a union: an agent reference or an advisor.
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsSessionThread/builder)
              (.id "thread_2")
              (.agent (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAdvisor/builder)
                          (.model "claude-opus-4-8")
                          (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAdvisor$Type/of "advisor"))
                          (.build)))
              (.archivedAt (java.util.Optional/empty))
              (.createdAt ts)
              (.parentThreadId (java.util.Optional/empty))
              (.sessionId "sess_1")
              (.stats (java.util.Optional/empty))
              (.status (com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadStatus/of "idle"))
              (.type (com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThread$Type/of "session_thread"))
              (.updatedAt ts)
              (.usage (java.util.Optional/empty))
              (.build))
        m (session-thread->map r)]
    (is (= {:type :advisor :model "claude-opus-4-8"} (:agent m)))))

(deftest memory-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsMemory/builder)
              (.id "mem_1")
              (.contentSha256 "sha")
              (.contentSizeBytes 5)
              (.createdAt ts)
              (.memoryStoreId "ms_1")
              (.memoryVersionId "mv_1")
              (.path "/notes/a.md")
              (.type (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsMemory$Type/of "memory"))
              (.updatedAt ts)
              (.content "hello")
              (.build))
        d (-> (BetaManagedAgentsDeletedMemory/builder)
              (.id "mem_1")
              (.type (com.anthropic.models.beta.memorystores.memories.BetaManagedAgentsDeletedMemory$Type/of "memory_deleted"))
              (.build))
        m (memory->map r)]
    (is (= "mem_1" (:id m)))
    (is (= "ms_1" (:memory-store-id m)))
    (is (= :memory (:type m)))
    (is (= "hello" (:content m)))
    (is (= {:id "mem_1" :deleted true :type :memory-deleted} (memory-delete->map d)))))

(deftest memory-version-params
  (let [^MemoryVersionListParams lp
        (->memory-version-list-params "ms_1" {:memory-id "mem_1" :limit 10 :view :full})
        ^MemoryVersionRetrieveParams rp
        (->memory-version-retrieve-params "ms_1" "mv_1" {:view :full})]
    (is (= "ms_1" (opt (.memoryStoreId lp))))
    (is (= "mem_1" (opt (.memoryId lp))))
    (is (= 10 (opt (.limit lp))))
    (is (= "ms_1" (.memoryStoreId rp)))
    (is (= "mv_1" (opt (.memoryVersionId rp))))))

(deftest memory-version-redact-params-and-response-mapping
  (let [f (ns-resolve 'anthropic.beta '->memory-version-redact-params)]
    (is (ifn? f))
    (when f
      (let [p (f "ms_1" "mv_1")]
        (is (= "ms_1" (.memoryStoreId p)))
        (is (= "mv_1" (opt (.memoryVersionId p)))))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z")
        r (-> (com.anthropic.models.beta.memorystores.memoryversions.BetaManagedAgentsMemoryVersion/builder)
              (.id "mv_1") (.memoryStoreId "ms_1") (.memoryId "mem_1")
              (.operation (com.anthropic.models.beta.memorystores.memoryversions.BetaManagedAgentsMemoryVersionOperation/of "redact"))
              (.createdAt ts) (.type (com.anthropic.models.beta.memorystores.memoryversions.BetaManagedAgentsMemoryVersion$Type/of "memory_version"))
              (.redactedAt ts) (.build))]
    (is (= {:id "mv_1" :memory-store-id "ms_1" :memory-id "mem_1"
            :operation :redact :created-at "2026-07-22T00:00Z" :redacted-at "2026-07-22T00:00Z"}
           (memory-version->map r)))))

(deftest memory-version-created-by-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z")
        base #(-> (BetaManagedAgentsMemoryVersion/builder)
                  (.id "mv_1") (.memoryStoreId "ms_1") (.memoryId "mem_1")
                  (.operation (BetaManagedAgentsMemoryVersionOperation/of "create"))
                  (.createdAt ts)
                  (.type (BetaManagedAgentsMemoryVersion$Type/of "memory_version")))
        service-account (-> (BetaManagedAgentsServiceAccountActor/builder)
                            (.serviceAccountId "sa_1")
                            (.build))
        api (-> (BetaManagedAgentsApiActor/builder)
                (.apiKeyId "key_1")
                (.type (BetaManagedAgentsApiActor$Type/of "api"))
                (.build))]
    (is (= {:type :service-account :service-account-id "sa_1"}
           (:created-by (memory-version->map
                         (.build (.createdBy (base)
                                             (BetaManagedAgentsActor/ofServiceAccount service-account)))))))
    (is (= {:type :api :api-key-id "key_1"}
           (:created-by (memory-version->map
                         (.build (.createdBy (base) (BetaManagedAgentsActor/ofApi api)))))))))

(deftest reveal-tunnel-token-params-and-response-mapping
  (let [f (ns-resolve 'anthropic.beta '->tunnel-reveal-token-params)]
    (is (ifn? f))
    (when f (is (= "tun_1" (opt (.tunnelId (f "tun_1")))))))
  (let [r (-> (com.anthropic.models.beta.tunnels.BetaTunnelToken/builder)
              (.id "tt_1") (.tunnelToken "secret")
              (.type (JsonValue/from "tunnel_token")) (.build))
        f (ns-resolve 'anthropic.beta 'tunnel-token->map)]
    (is (ifn? f))
    (when f (is (= {:id "tt_1" :tunnel-token "secret"} (f r))))))

(deftest rotate-tunnel-token-params-and-response-mapping
  (let [f (ns-resolve 'anthropic.beta '->tunnel-rotate-token-params)]
    (is (ifn? f))
    (when f
      (let [p (f "tun_1" {:reason "rotate"})]
        (is (= "tun_1" (opt (.tunnelId p))))
        (is (= "rotate" (opt (.reason p)))))))
  (let [r (-> (com.anthropic.models.beta.tunnels.BetaTunnelToken/builder)
              (.id "tt_1") (.tunnelToken "secret")
              (.type (JsonValue/from "tunnel_token")) (.build))
        f (ns-resolve 'anthropic.beta 'tunnel-token->map)]
    (is (ifn? f))
    (when f (is (= {:id "tt_1" :tunnel-token "secret"} (f r))))))

(deftest session-resource-add-params-and-response-mapping
  (let [f (ns-resolve 'anthropic.beta '->session-resource-add-params)]
    (is (ifn? f))
    (when f
      (let [p (f "sess_1" {:file-id "file_1" :mount-path "/tmp/input"})]
        (is (= "sess_1" (opt (.sessionId p))))
        (is (= "file_1" (.fileId (.betaManagedAgentsFileResourceParams p))))
        (is (= "/tmp/input" (opt (.mountPath (.betaManagedAgentsFileResourceParams p))))))))
  (let [r (-> (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsFileResource/builder)
              (.id "res_1") (.fileId "file_1") (.mountPath "/tmp/input")
              (.createdAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
              (.updatedAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
              (.type (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsFileResource$Type/of "file"))
              (.build))]
    (is (= {:type :file :id "res_1" :file-id "file_1" :mount-path "/tmp/input"
            :created-at "2026-07-22T00:00Z" :updated-at "2026-07-22T00:00Z"}
           ((ns-resolve 'anthropic.beta 'session-resource->map) r)))))

(deftest session-resource-nested-fields-round-trip
  (let [github (-> (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource/builder)
                   (.id "res_2") (.url "https://example.test/repo") (.mountPath "/repo")
                   (.createdAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
                   (.updatedAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
                   (.branchCheckout "main")
                   (.type (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource$Type/of "github_repository"))
                   (.build))
        memory (-> (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource/builder)
                   (.memoryStoreId "store_1") (.access (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource$Access/of "read_only"))
                   (.description "A store") (.instructions "Use it") (.name "Notes")
                   (.mountPath "/memory")
                   (.type (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource$Type/of "memory_store"))
                   (.build))
        github-map (session-resource->map github)
        memory-map (session-resource->map memory)
        github-params (invoke-private '->deployment-resource
                                      (assoc github-map :authorization-token "token"))
        memory-params (invoke-private '->deployment-resource memory-map)]
    (is (= {:type :branch :name "main"} (:checkout github-map)))
    (is (= {:access :read-only :description "A store" :instructions "Use it" :name "Notes"}
           (select-keys memory-map [:access :description :instructions :name])))
    (is (= "main" (.name (.asBranch (opt (.checkout github-params))))))
    (is (= "read-only" (.asString (opt (.access memory-params)))))
    (is (= "Use it" (opt (.instructions memory-params))))))

(deftest session-resource-union-response-mapping-preserves-nested-fields
  (let [github (-> (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource/builder)
                   (.id "res_2") (.url "https://example.test/repo") (.mountPath "/repo")
                   (.branchCheckout "main")
                   (.createdAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
                   (.updatedAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
                   (.type (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsGitHubRepositoryResource$Type/of "github_repository"))
                   (.build))
        memory (-> (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource/builder)
                   (.memoryStoreId "store_1") (.access (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource$Access/of "read_only"))
                   (.description "A store") (.instructions "Use it") (.name "Notes")
                   (.mountPath "/memory")
                   (.type (com.anthropic.models.beta.sessions.resources.BetaManagedAgentsMemoryStoreResource$Type/of "memory_store"))
                   (.build))
        direct-github (session-resource->map github)
        direct-memory (session-resource->map memory)
        retrieve-github (session-resource->map
                         (com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse/ofGitHubRepository github))
        update-github (session-resource->map
                       (com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse/ofGitHubRepository github))
        retrieve-memory (session-resource->map
                         (com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse/ofMemoryStore memory))
        update-memory (session-resource->map
                       (com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse/ofMemoryStore memory))]
    (is (= {:type :branch :name "main"} (:checkout retrieve-github)))
    (is (= {:type :branch :name "main"} (:checkout update-github)))
    (is (= direct-github retrieve-github))
    (is (= direct-github update-github))
    (is (= direct-memory retrieve-memory))
    (is (= direct-memory update-memory))))

(deftest agent-tool-response-mapping-preserves-properties-and-permission-policies
  (let [always-ask (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy/builder)
                       (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAlwaysAskPolicy$Type/of "always_ask"))
                       (.build))
        properties (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema$Properties/builder)
                       (.putAdditionalProperty "query" (JsonValue/from {"type" "string"}))
                       (.build))
        custom (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomTool/builder)
                   (.name "search") (.description "Search")
                   (.inputSchema (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema/builder)
                                     (.type (JsonValue/from "object")) (.properties properties) (.build)))
                   (.type (com.anthropic.models.beta.agents.BetaManagedAgentsCustomTool$Type/of "custom"))
                   (.build))
        mcp-config (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfig/builder)
                       (.name "search") (.enabled true)
                       (.permissionPolicy (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolConfig$PermissionPolicy/ofAlwaysAsk always-ask))
                       (.build))
        mcp-default (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig/builder)
                        (.enabled true)
                        (.permissionPolicy (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolsetDefaultConfig$PermissionPolicy/ofAlwaysAsk always-ask))
                        (.build))
        mcp (-> (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolset/builder)
                (.configs [mcp-config]) (.defaultConfig mcp-default) (.mcpServerName "server")
                (.type (com.anthropic.models.beta.agents.BetaManagedAgentsMcpToolset$Type/of "mcp_toolset"))
                (.build))
        agent-config (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig/builder)
                         (.name (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig$Name/of "search"))
                         (.enabled true)
                         (.permissionPolicy (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolConfig$PermissionPolicy/ofAlwaysAsk always-ask))
                         (.build))
        agent-default (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig/builder)
                          (.enabled true)
                          (.permissionPolicy (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolsetDefaultConfig$PermissionPolicy/ofAlwaysAsk always-ask))
                          (.build))
        agent-toolset (-> (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401/builder)
                          (.configs [agent-config]) (.defaultConfig agent-default)
                          (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentToolset20260401$Type/of "agent_toolset_20260401"))
                          (.build))
        mapped-custom (agent-tool->map (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofCustom custom))
        mapped-mcp (agent-tool->map (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofMcpToolset mcp))
        mapped-agent (agent-tool->map (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofAgentToolset20260401 agent-toolset))]
    (is (= {:query {"type" "string"}} (get-in mapped-custom [:input-schema :properties])))
    (is (= {:type :always-ask} (get-in mapped-mcp [:configs 0 :permission-policy])))
    (is (= {:type :always-ask} (get-in mapped-mcp [:default-config :permission-policy])))
    (is (= {:type :always-ask} (get-in mapped-agent [:configs 0 :permission-policy])))
    (is (= {:type :always-ask} (get-in mapped-agent [:default-config :permission-policy])))))

(deftest session-agent-tool-response-mapping-shares-agent-tool-shape
  (let [custom (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomTool/builder)
                   (.name "search") (.description "Search")
                   (.inputSchema (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema/builder)
                                     (.type (JsonValue/from "object"))
                                     (.properties (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomToolInputSchema$Properties/builder)
                                                      (.putAdditionalProperty "query" (JsonValue/from {"type" "string"}))
                                                      (.build)))
                                     (.build)))
                   (.type (com.anthropic.models.beta.agents.BetaManagedAgentsCustomTool$Type/of "custom"))
                   (.build))
        agent-tool (com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofCustom custom)
        session-tool (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Tool/ofCustom custom)
        model (-> (com.anthropic.models.beta.agents.BetaManagedAgentsModelConfig/builder)
                  (.id (com.anthropic.models.beta.agents.BetaManagedAgentsModel/of "m")) (.build))
        session-agent (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent/builder)
                          (.id "agent_1") (.model model) (.name "helper") (.version 1)
                          (.description "d") (.system "s")
                          (.multiagent (-> (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionMultiagentCoordinator/builder)
                                           (.agents [])
                                           (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionMultiagentCoordinator$Type/of "coordinator"))
                                           (.build)))
                          (.mcpServers []) (.skills []) (.tools [session-tool])
                          (.type (com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Type/of "agent"))
                          (.build))]
    (is (= (agent-tool->map agent-tool)
           (first (:tools (session-agent->map session-agent)))))))

(deftest session-thread-stats-map-startup-seconds
  (let [stats (-> (com.anthropic.models.beta.sessions.threads.BetaManagedAgentsSessionThreadStats/builder)
                  (.activeSeconds 1.0) (.durationSeconds 2.0) (.startupSeconds 0.5) (.build))]
    (is (= 0.5 (:startup-seconds (session-thread-stats->map stats))))))

(deftest deployment-resource-nested-fields-map
  (let [github (-> (com.anthropic.models.beta.deployments.BetaManagedAgentsGitHubRepositoryResourceConfig/builder)
                   (.url "https://example.test/repo") (.branchCheckout "release")
                   (.type (com.anthropic.models.beta.deployments.BetaManagedAgentsGitHubRepositoryResourceConfig$Type/of "github_repository"))
                   (.build))
        memory (-> (com.anthropic.models.beta.deployments.BetaManagedAgentsMemoryStoreResourceConfig/builder)
                   (.memoryStoreId "store_1") (.access (com.anthropic.models.beta.deployments.BetaManagedAgentsMemoryStoreResourceConfig$Access/of "read_only"))
                   (.instructions "Use it")
                   (.type (com.anthropic.models.beta.deployments.BetaManagedAgentsMemoryStoreResourceConfig$Type/of "memory_store"))
                   (.build))]
    (is (= {:type :github-repository :checkout {:type :branch :name "release"}}
           (select-keys (deployment-resource->map
                         (com.anthropic.models.beta.deployments.BetaManagedAgentsSessionResourceConfig/ofGitHubRepository github))
                        [:type :name :checkout])))
    (is (= {:type :memory-store :memory-store-id "store_1" :access :read-only :instructions "Use it"}
           (deployment-resource->map
            (com.anthropic.models.beta.deployments.BetaManagedAgentsSessionResourceConfig/ofMemoryStore memory))))))

(deftest vault-credential-mcp-oauth-validate-params-and-response-mapping
  (let [f (ns-resolve 'anthropic.beta '->credential-mcp-oauth-validate-params)]
    (is (ifn? f))
    (when f
      (let [p (f "vault_1" "cred_1")]
        (is (= "vault_1" (.vaultId p)))
        (is (= "cred_1" (opt (.credentialId p)))))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z")
        r (-> (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation/builder)
              (.credentialId "cred_1") (.vaultId "vault_1") (.hasRefreshToken true)
              (.mcpProbe (java.util.Optional/empty)) (.refresh (java.util.Optional/empty))
              (.status (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidationStatus/of "valid"))
              (.type (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation$Type/of "vault_credential_validation"))
              (.validatedAt ts) (.build))
        f (ns-resolve 'anthropic.beta 'credential-validation->map)]
    (is (ifn? f))
    (when f
      (is (= {:credential-id "cred_1" :vault-id "vault_1" :has-refresh-token true
              :status :valid :type :vault-credential-validation
              :validated-at "2026-07-22T00:00Z"}
             (f r))))))

(deftest skill-version-response-mapping
  (let [r (-> (VersionCreateResponse/builder)
              (.id "sv_1")
              (.createdAt "2026-07-04T00:00:00Z")
              (.description "d")
              (.directory "/")
              (.name "SKILL.md")
              (.skillId "skill_1")
              (.type "skill_version")
              (.version "2")
              (.build))
        d (-> (VersionDeleteResponse/builder)
              (.id "sv_1")
              (.type "skill_version_deleted")
              (.build))
        m (skill-version->map r)]
    (is (= "sv_1" (:id m)))
    (is (= "skill_1" (:skill-id m)))
    (is (= "2" (:version m)))
    (is (= {:id "sv_1" :deleted true :type :skill_version_deleted} (skill-version-delete->map d)))))

(deftest deployment-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsDeployment/builder)
              (.id "dep_1")
              (.agent (agent-ref))
              (.archivedAt (java.util.Optional/empty))
              (.description (java.util.Optional/empty))
              (.environmentId "env_1")
              (.initialEvents ^java.util.List (java.util.ArrayList.))
              (.metadata (-> (com.anthropic.models.beta.deployments.BetaManagedAgentsDeployment$Metadata/builder)
                             (.putAdditionalProperty "team" (JsonValue/from "platform"))
                             (.build)))
              (.name "nightly")
              (.pausedReason (java.util.Optional/empty))
              (.resources ^java.util.List (java.util.ArrayList.))
              (.schedule (-> (com.anthropic.models.beta.deployments.BetaManagedAgentsSchedule/builder)
                             (.expression "0 0 * * *") (.timezone "UTC")
                             (.type (com.anthropic.models.beta.deployments.BetaManagedAgentsSchedule$Type/of "cron"))
                             (.build)))
              (.status (com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentStatus/of "running"))
              (.type (com.anthropic.models.beta.deployments.BetaManagedAgentsDeployment$Type/of "deployment"))
              (.createdAt ts)
              (.updatedAt ts)
              (.vaultIds ^java.util.List (java.util.ArrayList. ["vault_1"]))
              (.build))
        m (deployment->map r)]
    (is (= "dep_1" (:id m)))
    (is (= "nightly" (:name m)))
    (is (= "env_1" (:environment-id m)))
    (is (= "2026-07-04T00:00Z" (:created-at m)))
    (is (= ["vault_1"] (:vault-ids m)))
    (is (= {:team "platform"} (:metadata m)))
    (is (= :deployment (:type m)))
    (is (= "0 0 * * *" (get-in m [:schedule :expression])))
    (is (= "UTC" (get-in m [:schedule :timezone])))
    (is (= "UTC" (.timezone (opt (.schedule (->deployment-update-params "dep_1"
                                                          (select-keys m [:resources :schedule])))))))))

(deftest deployment-run-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsDeploymentRun/builder)
              (.id "dr_1")
              (.agent (agent-ref))
              (.deploymentId "dep_1")
              (.createdAt ts)
              (.error (java.util.Optional/empty))
              (.sessionId (java.util.Optional/empty))
              (.triggerContext
               (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsManualTriggerContext/builder)
                   (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsManualTriggerContext$Type/of "manual"))
                   (.build)))
              (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Type/of "deployment_run"))
              (.build))
        m (deployment-run->map r)]
    (is (= "dr_1" (:id m)))
    (is (= "dep_1" (:deployment-id m)))
    (is (= :deployment-run (:type m)))
    (is (= "2026-07-04T00:00Z" (:created-at m)))
    (is (= {:type :manual} (:trigger-context m)))
    (is (not (instance? BetaManagedAgentsDeploymentRun (:trigger-context m)))))

(defn- deployment-run-error-union [error]
  (cond
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofEnvironmentArchived error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsAgentArchivedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofAgentArchived error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentNotFoundRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofEnvironmentNotFound error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultNotFoundRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofVaultNotFound error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultArchivedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofVaultArchived error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsFileNotFoundRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofFileNotFound error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMemoryStoreArchivedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofMemoryStoreArchived error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSkillNotFoundRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofSkillNotFound error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionResourceNotFoundRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofSessionResourceNotFound error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsWorkspaceArchivedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofWorkspaceArchived error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsOrganizationDisabledRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofOrganizationDisabled error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionRateLimitedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofSessionRateLimited error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionCreationRejectedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofSessionCreationRejected error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsUnknownRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofUnknown error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSelfHostedResourcesUnsupportedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofSelfHostedResourcesUnsupported error)
    (instance? com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMcpEgressBlockedRunError error)
    (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error/ofMcpEgressBlocked error)))

(defn- deployment-run-with-error [error]
  (-> (BetaManagedAgentsDeploymentRun/builder)
      (.id "dr_1")
      (.agent (agent-ref))
      (.deploymentId "dep_1")
      (.createdAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
      (.error (deployment-run-error-union error))
      (.sessionId (java.util.Optional/empty))
      (.triggerContext
       (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsManualTriggerContext/builder)
           (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsManualTriggerContext$Type/of "manual"))
           (.build)))
      (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Type/of "deployment_run"))
      (.build)))

(deftest deployment-run-error-variants-map-to-plain-data
  (doseq [[type error]
          [[:environment_archived
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError$Type/of "environment_archived"))
                (.build))]
           [:agent_archived
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsAgentArchivedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsAgentArchivedRunError$Type/of "agent_archived"))
                (.build))]
           [:environment_not_found
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentNotFoundRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentNotFoundRunError$Type/of "environment_not_found"))
                (.build))]
           [:vault_not_found
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultNotFoundRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultNotFoundRunError$Type/of "vault_not_found"))
                (.build))]
           [:vault_archived
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultArchivedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsVaultArchivedRunError$Type/of "vault_archived"))
                (.build))]
           [:file_not_found
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsFileNotFoundRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsFileNotFoundRunError$Type/of "file_not_found"))
                (.build))]
           [:memory_store_archived
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMemoryStoreArchivedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMemoryStoreArchivedRunError$Type/of "memory_store_archived"))
                (.build))]
           [:skill_not_found
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSkillNotFoundRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSkillNotFoundRunError$Type/of "skill_not_found"))
                (.build))]
           [:session_resource_not_found
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionResourceNotFoundRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionResourceNotFoundRunError$Type/of "session_resource_not_found"))
                (.build))]
           [:workspace_archived
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsWorkspaceArchivedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsWorkspaceArchivedRunError$Type/of "workspace_archived"))
                (.build))]
           [:organization_disabled
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsOrganizationDisabledRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsOrganizationDisabledRunError$Type/of "organization_disabled"))
                (.build))]
           [:session_rate_limited
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionRateLimitedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionRateLimitedRunError$Type/of "session_rate_limited"))
                (.build))]
           [:session_creation_rejected
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionCreationRejectedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSessionCreationRejectedRunError$Type/of "session_creation_rejected"))
                (.build))]
           [:unknown
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsUnknownRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsUnknownRunError$Type/of "unknown"))
                (.build))]
           [:self_hosted_resources_unsupported
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSelfHostedResourcesUnsupportedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsSelfHostedResourcesUnsupportedRunError$Type/of "self_hosted_resources_unsupported"))
                (.build))]
           [:mcp_egress_blocked
            (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMcpEgressBlockedRunError/builder)
                (.message "message")
                (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsMcpEgressBlockedRunError$Type/of "mcp_egress_blocked"))
                (.build))]]]
    (is (= {:type type :message "message"}
           (select-keys (:error (deployment-run->map (deployment-run-with-error error)))
                        [:type :message])))))
  (let [m (deployment-run->map (deployment-run-with-error
                                (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError/builder)
                                    (.message "message")
                                    (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsEnvironmentArchivedRunError$Type/of "environment_archived"))
                                    (.build))))]
    (is (not (re-find #"BetaManagedAgents" (pr-str m))))
    (is (not (instance? BetaManagedAgentsDeploymentRun (:error m))))))

(deftest deployment-run-schedule-trigger-context-maps-to-plain-data
  (let [scheduled-at (java.time.OffsetDateTime/parse "2026-07-04T01:00:00Z")
        r (-> (BetaManagedAgentsDeploymentRun/builder)
              (.id "dr_1")
              (.agent (agent-ref))
              (.deploymentId "dep_1")
              (.createdAt scheduled-at)
              (.error (java.util.Optional/empty))
              (.sessionId (java.util.Optional/empty))
              (.triggerContext
               (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerContext/ofSchedule
                (-> (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsScheduleTriggerContext/builder)
                    (.scheduledAt scheduled-at)
                    (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsScheduleTriggerContext$Type/of "schedule"))
                    (.build))))
              (.type (com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Type/of "deployment_run"))
              (.build))
        m (deployment-run->map r)]
    (is (= {:type :schedule :scheduled-at "2026-07-04T01:00Z"}
           (:trigger-context m)))
    (is (not (re-find #"BetaManagedAgents" (pr-str m))))
    (is (not (instance? BetaManagedAgentsDeploymentRun (:trigger-context m))))))

(deftest environment-response-mapping
  (let [r (-> (BetaEnvironment/builder)
              (.id "env_1")
              (.archivedAt (java.util.Optional/empty))
              (.config
               (-> (com.anthropic.models.beta.environments.BetaSelfHostedConfig/builder)
                   (.type (com.anthropic.core.JsonValue/from "self_hosted"))
                   (.build)))
              (.name "prod")
              (.description "Production")
              (.type (com.anthropic.core.JsonValue/from "environment"))
              (.createdAt "2026-07-04T00:00:00Z")
              (.metadata (-> (com.anthropic.models.beta.environments.BetaEnvironment$Metadata/builder)
                             (.putAdditionalProperty "team" (JsonValue/from "platform"))
                             (.build)))
              (.updatedAt "2026-07-04T00:00:00Z")
              (.scope (com.anthropic.models.beta.environments.BetaEnvironment$Scope/of "organization"))
              (.build))
        d (-> (BetaEnvironmentDeleteResponse/builder)
              (.id "env_1")
              (.type (com.anthropic.models.beta.environments.BetaEnvironmentDeleteResponse$Type/of "environment_deleted"))
              (.build))
        m (environment->map r)]
    (is (= "env_1" (:id m)))
    (is (= "prod" (:name m)))
    (is (= "Production" (:description m)))
    (is (= {:team "platform"} (:metadata m)))
    (is (= :self-hosted (:type (:config m))))
    (is (= :organization (:scope m)))
    (is (.isSelfHosted (opt (.config (->environment-update-params "env_1" m)))))
    (is (= {:id "env_1" :deleted true :type :environment_deleted} (environment-delete->map d)))))

(deftest environment-work-response-mapping
  (let [work (-> (BetaSelfHostedWork/builder)
                 (.id "work_1")
                 (.acknowledgedAt "2026-07-04T00:01:00Z")
                 (.createdAt "2026-07-04T00:00:00Z")
                 (.data (-> (com.anthropic.models.beta.environments.work.BetaSessionWorkData/builder)
                            (.id "sess_1")
                            (.type (com.anthropic.core.JsonValue/from "session"))
                            (.build)))
                 (.environmentId "env_1")
                 (.latestHeartbeatAt "2026-07-04T00:02:00Z")
                 (.metadata (-> (com.anthropic.models.beta.environments.work.BetaSelfHostedWork$Metadata/builder)
                                (.putAdditionalProperty "team" (com.anthropic.core.JsonValue/from "x"))
                                (.build)))
                 (.secret "secret_1")
                 (.startedAt "2026-07-04T00:00:30Z")
                 (.state (com.anthropic.models.beta.environments.work.BetaSelfHostedWork$State/of "running"))
                 (.stopRequestedAt (java.util.Optional/empty))
                 (.stoppedAt (java.util.Optional/empty))
                 (.type (com.anthropic.core.JsonValue/from "self_hosted_work"))
                 (.build))
        heartbeat (-> (BetaSelfHostedWorkHeartbeatResponse/builder)
                      (.lastHeartbeat "2026-07-04T00:02:00Z")
                      (.leaseExtended true)
                      (.state (com.anthropic.models.beta.environments.work.BetaSelfHostedWorkHeartbeatResponse$State/of "running"))
                      (.ttlSeconds 30)
                      (.type (com.anthropic.core.JsonValue/from "self_hosted_work_heartbeat"))
                      (.build))
        stats (-> (BetaSelfHostedWorkQueueStats/builder)
                  (.depth 3)
                  (.oldestQueuedAt "2026-07-04T00:00:00Z")
                  (.pending 2)
                  (.workersPolling 1)
                  (.type (com.anthropic.core.JsonValue/from "self_hosted_work_queue_stats"))
                  (.build))]
    (is (= {:id "work_1"
            :acknowledged-at "2026-07-04T00:01:00Z"
            :created-at "2026-07-04T00:00:00Z"
            :data {:id "sess_1"}
            :environment-id "env_1"
            :latest-heartbeat-at "2026-07-04T00:02:00Z"
            :metadata {:team "x"}
            :secret "secret_1"
            :started-at "2026-07-04T00:00:30Z"
            :state :running}
           (environment-work->map work)))
    (is (= {:last-heartbeat "2026-07-04T00:02:00Z"
            :lease-extended true
            :state :running
            :ttl-seconds 30}
           (environment-work-heartbeat->map heartbeat)))
    (is (= {:depth 3
            :oldest-queued-at "2026-07-04T00:00:00Z"
            :pending 2
            :workers-polling 1}
           (environment-work-stats->map stats)))
    (is (nil? (environment-work-optional->map (java.util.Optional/empty))))))

(deftest vault-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaManagedAgentsVault/builder)
              (.id "vault_1")
              (.archivedAt (java.util.Optional/empty))
              (.displayName "Main Vault")
              (.metadata (-> (com.anthropic.models.beta.vaults.BetaManagedAgentsVault$Metadata/builder)
                             (.putAdditionalProperty "team" (JsonValue/from "platform"))
                             (.build)))
              (.type (com.anthropic.models.beta.vaults.BetaManagedAgentsVault$Type/of "vault"))
              (.createdAt ts)
              (.updatedAt ts)
              (.build))
        d (-> (BetaManagedAgentsDeletedVault/builder)
              (.id "vault_1")
              (.type (com.anthropic.models.beta.vaults.BetaManagedAgentsDeletedVault$Type/of "vault_deleted"))
              (.build))
        m (vault->map r)
        ^VaultUpdateParams u (->vault-update-params "vault_1" m)]
    (is (= "vault_1" (:id m)))
    (is (= "Main Vault" (:display-name m)))
    (is (= "2026-07-04T00:00Z" (:updated-at m)))
    (is (= {:team "platform"} (:metadata m)))
    (is (= :vault (:type m)))
    (is (= "platform" (.convert ^JsonValue (get (._additionalProperties (opt (.metadata u))) "team") String)))
    (is (= {:id "vault_1" :deleted true :type :vault_deleted} (vault-delete->map d)))))

(deftest user-profile-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaUserProfile/builder)
              (.id "up_1")
              (.metadata (-> (com.anthropic.models.beta.userprofiles.BetaUserProfile$Metadata/builder)
                             (.build)))
              (.relationship (com.anthropic.models.beta.userprofiles.BetaUserProfile$Relationship/of "external"))
              (.accessType (com.anthropic.models.beta.userprofiles.BetaUserProfile$AccessType/of "passthrough"))
              (.trustGrants (-> (com.anthropic.models.beta.userprofiles.BetaUserProfile$TrustGrants/builder)
                                (.putAdditionalProperty "source" (JsonValue/from "admin"))
                                (.build)))
              (.name "Ada")
              (.externalId "ada-1")
              (.type (com.anthropic.models.beta.userprofiles.BetaUserProfile$Type/of "user_profile"))
              (.createdAt ts)
              (.updatedAt ts)
              (.build))
        u (-> (BetaUserProfileEnrollmentUrl/builder)
              (.url "https://example.test/enroll")
              (.expiresAt ts)
              (.type (com.anthropic.models.beta.userprofiles.BetaUserProfileEnrollmentUrl$Type/of "user_profile_enrollment_url"))
              (.build))
        m (user-profile->map r)]
    (is (= "up_1" (:id m)))
    (is (= "Ada" (:name m)))
    (is (= "ada-1" (:external-id m)))
    (is (= :external (:relationship m)))
    (is (= :passthrough (:access-type m)))
    (is (= {:source "admin"} (:trust-grants m)))
    (is (= {:url "https://example.test/enroll"
            :expires-at "2026-07-04T00:00Z"}
           (enrollment-url->map u)))))

(deftest webhook-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        data (-> (BetaWebhookSessionCreatedEventData/builder)
                 (.id "sess_1")
                 (.organizationId "org_1")
                 (.workspaceId "ws_1")
                 (.type (com.anthropic.core.JsonValue/from "session.created"))
                 (.build))
        r (-> (UnwrapWebhookEvent/builder)
              (.id "evt_1")
              (.createdAt ts)
              (.data (BetaWebhookEventData/ofSessionCreated data))
              (.type (com.anthropic.core.JsonValue/from "event"))
              (.build))
        m (webhook-event->map r)]
    (is (= :session-created (:type m)))
    (is (= "evt_1" (:id m)))
    (is (= "sess_1" (:data-id m)))
    (is (= "org_1" (:organization-id m)))
    (is (= "ws_1" (:workspace-id m)))))

(deftest beta-253-session-budget-webhook-mapping
  (let [data (-> (com.anthropic.models.beta.webhooks.BetaWebhookSessionBudgetReachedEventData/builder)
                 (.id "sess_1") (.organizationId "org_1") (.workspaceId "ws_1")
                 (.type (JsonValue/from "session.budget_reached")) (.build))
        event (-> (UnwrapWebhookEvent/builder)
                  (.id "evt_budget")
                  (.createdAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                  (.data (BetaWebhookEventData/ofSessionBudgetReached data))
                  (.type (JsonValue/from "event")) (.build))]
    (is (= {:type :session-budget-reached :data-id "sess_1"
            :organization-id "org_1" :workspace-id "ws_1"
            :id "evt_budget" :created-at "2026-07-04T00:00Z"
            :event-type "event"}
           (webhook-event->map event)))))

(deftest webhook-environment-and-memory-store-response-mapping
  (let [event-map
        (fn [type data]
          (let [webhook-data
                (case type
                  :environment-created (BetaWebhookEventData/ofEnvironmentCreated data)
                  :environment-updated (BetaWebhookEventData/ofEnvironmentUpdated data)
                  :environment-archived (BetaWebhookEventData/ofEnvironmentArchived data)
                  :environment-deleted (BetaWebhookEventData/ofEnvironmentDeleted data)
                  :memory-store-created (BetaWebhookEventData/ofMemoryStoreCreated data)
                  :memory-store-archived (BetaWebhookEventData/ofMemoryStoreArchived data)
                  :memory-store-deleted (BetaWebhookEventData/ofMemoryStoreDeleted data))]
            (select-keys
             (webhook-event->map
              (-> (UnwrapWebhookEvent/builder)
                  (.id "evt_1")
                  (.createdAt (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z"))
                  (.data webhook-data)
                  (.type (com.anthropic.core.JsonValue/from "event"))
                  (.build)))
             [:type :data-id :organization-id :workspace-id])))]
    (doseq [[type data]
            [[:environment-created (-> (BetaWebhookEnvironmentCreatedEventData/builder)
                                       (.id "env_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                       (.type (com.anthropic.core.JsonValue/from "environment.created")) (.build))]
             [:environment-updated (-> (BetaWebhookEnvironmentUpdatedEventData/builder)
                                       (.id "env_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                       (.type (com.anthropic.core.JsonValue/from "environment.updated")) (.build))]
             [:environment-archived (-> (BetaWebhookEnvironmentArchivedEventData/builder)
                                        (.id "env_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                        (.type (com.anthropic.core.JsonValue/from "environment.archived")) (.build))]
             [:environment-deleted (-> (BetaWebhookEnvironmentDeletedEventData/builder)
                                       (.id "env_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                       (.type (com.anthropic.core.JsonValue/from "environment.deleted")) (.build))]
             [:memory-store-created (-> (BetaWebhookMemoryStoreCreatedEventData/builder)
                                        (.id "ms_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                        (.type (com.anthropic.core.JsonValue/from "memory_store.created")) (.build))]
             [:memory-store-archived (-> (BetaWebhookMemoryStoreArchivedEventData/builder)
                                         (.id "ms_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                         (.type (com.anthropic.core.JsonValue/from "memory_store.archived")) (.build))]
             [:memory-store-deleted (-> (BetaWebhookMemoryStoreDeletedEventData/builder)
                                        (.id "ms_1") (.organizationId "org_1") (.workspaceId "ws_1")
                                        (.type (com.anthropic.core.JsonValue/from "memory_store.deleted")) (.build))]]]
      (is (= {:type type
              :data-id (if (clojure.string/starts-with? (name type) "environment") "env_1" "ms_1")
              :organization-id "org_1"
              :workspace-id "ws_1"}
             (event-map type data))))))

(defn- union-variant-predicates
  "Every `isX` predicate an SDK union exposes, as the variant names it can hold."
  [^Class c]
  (->> (.getMethods c)
       (map #(.getName ^java.lang.reflect.Method %))
       (filter #(re-matches #"is[A-Z].*" %))
       (remove #{"isValid"})
       set))

(defn- mapper-source
  "The text of `src/anthropic/beta.clj`, for checking union branch coverage."
  []
  (slurp "src/anthropic/beta.clj"))

(deftest every-session-event-variant-is-mapped
  ;; A variant with no mapper branch becomes {:type :unknown}. The mapper then
  ;; drops the event. This guards the whole union, including variants a future
  ;; SDK release adds.
  (let [src (mapper-source)
        missing (remove #(clojure.string/includes? src (str "." %))
                        (union-variant-predicates
                         com.anthropic.models.beta.sessions.events.BetaManagedAgentsSessionEvent))]
    (is (empty? missing)
        (str "session event variants with no mapper branch: " (sort missing)))))

(deftest every-webhook-event-variant-is-mapped
  (let [src (mapper-source)
        missing (remove #(clojure.string/includes? src (str "." %))
                        (union-variant-predicates
                         com.anthropic.models.beta.webhooks.BetaWebhookEventData))]
    (is (empty? missing)
        (str "webhook variants with no mapper branch: " (sort missing)))))

(deftest every-user-message-content-variant-is-mapped
  (let [src (mapper-source)
        missing (remove #(clojure.string/includes? src (str "." %))
                        (union-variant-predicates
                         com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEvent$Content))]
    (is (empty? missing)
        (str "user message content variants with no mapper branch: " (sort missing)))))

(deftest every-send-session-event-variant-is-mapped
  (let [src (mapper-source)
        missing (remove #(clojure.string/includes? src (str "." %))
                        (union-variant-predicates
                         com.anthropic.models.beta.sessions.events.BetaManagedAgentsSendSessionEvents$Data))]
    (is (empty? missing)
        (str "send session event variants with no mapper branch: " (sort missing)))))

(defn- assert-union-mapped [union-class label]
  (let [src (mapper-source)
        missing (remove #(clojure.string/includes? src (str "." %))
                        (union-variant-predicates union-class))]
    (is (empty? missing)
        (str label " variants with no mapper branch: " (sort missing)))))

(deftest every-agent-tool-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool
                       "agent tool"))

(deftest every-agent-skill-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Skill
                       "agent skill"))

(deftest every-session-agent-tool-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.sessions.BetaManagedAgentsSessionAgent$Tool
                       "session agent tool"))

(deftest every-session-resource-retrieve-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.sessions.resources.ResourceRetrieveResponse
                       "session resource retrieve"))

(deftest every-session-resource-update-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.sessions.resources.ResourceUpdateResponse
                       "session resource update"))

(deftest every-mcp-server-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.agents.BetaManagedAgentsMcpServerUrlDefinition
                       "MCP server"))

(deftest every-deployment-initial-event-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.deployments.BetaManagedAgentsDeploymentInitialEventParams
                       "deployment initial event"))

(deftest every-deployment-run-error-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.deploymentruns.BetaManagedAgentsDeploymentRun$Error
                       "deployment run error"))

(deftest every-deployment-run-trigger-context-variant-is-mapped
  (assert-union-mapped com.anthropic.models.beta.deploymentruns.BetaManagedAgentsTriggerContext
                       "deployment run trigger context"))

(deftest deployment-run-unknown-union-variants-throw
  (is (= :unknown-deployment-run-error-type
         (:anthropic/error
          (ex-data-for #((private-fn 'deployment-run-error->map) (Object.))))))
  (is (= :unknown-deployment-run-trigger-context-type
         (:anthropic/error
          (ex-data-for #((private-fn 'deployment-run-trigger-context->map) (Object.)))))))

(deftest credential-validation-carries-nested-http-responses
  ;; The probe and refresh objects each carry an HTTP response whose body, content
  ;; type, status code, and truncation flag were dropped.
  (let [http (-> (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsRefreshHttpResponse/builder)
                 (.body "{\"ok\":true}") (.bodyTruncated false)
                 (.contentType "application/json") (.statusCode 200) (.build))
        probe (-> (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsMcpProbe/builder)
                  (.method "POST") (.httpResponse http) (.build))
        refresh (-> (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsRefreshObject/builder)
                    (.status (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsRefreshObject$Status/of "succeeded"))
                    (.httpResponse http) (.build))
        ts (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z")
        r (-> (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation/builder)
              (.credentialId "cred_1") (.vaultId "vault_1") (.hasRefreshToken true)
              (.status (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidationStatus/of "valid"))
              (.type (com.anthropic.models.beta.vaults.credentials.BetaManagedAgentsCredentialValidation$Type/of "vault_credential_validation"))
              (.mcpProbe probe) (.refresh refresh) (.validatedAt ts) (.build))
        m ((ns-resolve 'anthropic.beta 'credential-validation->map) r)
        expected {:body "{\"ok\":true}" :body-truncated false
                  :content-type "application/json" :status-code 200}]
    (is (= expected (get-in m [:mcp-probe :http-response])))
    (is (= expected (get-in m [:refresh :http-response])))))
