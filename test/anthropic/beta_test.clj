(ns anthropic.beta-test
  (:require [clojure.test :refer [deftest is testing]]
            [anthropic.beta :as beta])
  (:import (com.anthropic.models.beta.skills SkillCreateParams
                                             SkillCreateResponse)
           (com.anthropic.models.beta.memorystores BetaManagedAgentsMemoryStore
                                                   MemoryStoreCreateParams
                                                   MemoryStoreUpdateParams)
           (com.anthropic.models.beta.agents AgentCreateParams
                                             AgentUpdateParams
                                             BetaManagedAgentsAgent
                                             BetaManagedAgentsAgentReference)
           (com.anthropic.models.beta.sessions SessionCreateParams
                                               SessionUpdateParams)
           (com.anthropic.models.beta.sessions.events BetaManagedAgentsEventParams
                                                       BetaManagedAgentsSendSessionEvents
                                                       BetaManagedAgentsSessionEvent
                                                       BetaManagedAgentsUserMessageEvent
                                                       EventSendParams)
           (com.anthropic.models.beta.sessions.threads BetaManagedAgentsSessionThread
                                                        ThreadArchiveParams
                                                        ThreadListParams
                                                        ThreadRetrieveParams)
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
           (com.anthropic.models.beta.memorystores.memoryversions MemoryVersionListParams
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
                                               BetaWebhookSessionCreatedEventData
                                               UnwrapWebhookEvent)))

(def ->skill-create-params #'beta/->skill-create-params)
(def ->memory-store-create-params #'beta/->memory-store-create-params)
(def ->memory-store-update-params #'beta/->memory-store-update-params)
(def ->agent-create-params #'beta/->agent-create-params)
(def ->agent-update-params #'beta/->agent-update-params)
(def ->session-create-params #'beta/->session-create-params)
(def ->session-update-params #'beta/->session-update-params)
(def ->session-event #'beta/->session-event)
(def ->event-send-params #'beta/->event-send-params)
(def session-event->map #'beta/session-event->map)
(def send-session-events->map #'beta/send-session-events->map)
(def ->thread-retrieve-params #'beta/->thread-retrieve-params)
(def ->thread-list-params #'beta/->thread-list-params)
(def ->thread-archive-params #'beta/->thread-archive-params)
(def session-thread->map #'beta/session-thread->map)
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

(defn- opt [^java.util.Optional o] (when (.isPresent o) (.get o)))

(defn- ex-data-for [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(defn- agent-ref ^BetaManagedAgentsAgentReference []
  (-> (BetaManagedAgentsAgentReference/builder)
      (.id "agent_1")
      (.version 1)
      (.type (com.anthropic.models.beta.agents.BetaManagedAgentsAgentReference$Type/of "agent"))
      (.build)))

(deftest tunnel-params-and-response-mapping
  (let [p (->tunnel-create-params {:display-name "Local"})]
    (is (= "Local" (opt (.displayName p)))))
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (com.anthropic.models.beta.tunnels.BetaTunnel/builder)
              (.id "tun_1") (.archivedAt (java.util.Optional/empty))
              (.createdAt ts) (.displayName "Local") (.domain "localhost")
              (.type (com.anthropic.core.JsonValue/from "tunnel")) (.build))]
    (is (= {:id "tun_1" :display-name "Local" :domain "localhost"
            :created-at "2026-07-04T00:00Z"}
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
  (let [p (->dream-create-params {:inputs [] :model "claude-opus-4-8" :instructions "dream"})]
    (is (= "claude-opus-4-8" (.asString (.model p))))
    (is (= "dream" (opt (.instructions p))))))

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
  (let [^AgentUpdateParams p (->agent-update-params
                              "agent_1"
                              {:version 2
                               :system "new"
                               :skills [{:type :custom :skill-id "skill_1" :version "2"}]
                               :mcp-servers [{:name "github" :url "https://mcp.example.test"}]
                               :tools [{:type :mcp-toolset :mcp-server-name "github"}]})]
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
  (is (= {:anthropic/error :missing-key :key :version}
         (ex-data-for #(->agent-update-params "agent_1" {:system "new"}))))
  (is (= {:anthropic/error :missing-key :key :name}
         (ex-data-for #(->agent-create-params {:model "m"}))))
  (is (= {:anthropic/error :missing-key :key :model}
         (ex-data-for #(->agent-create-params {:name "n"})))))

(deftest session-params
  (let [^SessionCreateParams p (->session-create-params
                                {:agent "agent_1" :title "t" :environment-id "env_1"})]
    (is (some? p))
    (is (= "t" (opt (.title p)))))
  (let [^SessionUpdateParams p (->session-update-params "sess_1" {:title "t2"})]
    (is (= "t2" (opt (.title p)))))
  (is (= {:anthropic/error :missing-key :key :agent}
         (ex-data-for #(->session-create-params {})))))

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
                                    {:name "Ada" :external-id "ada-1" :metadata {:team "x"}})]
    (is (= "Ada" (opt (.name p))))
    (is (= "ada-1" (opt (.externalId p)))))
  (let [^UserProfileUpdateParams p (->user-profile-update-params "up_1" {:name "Ada L"})]
    (is (= "up_1" (opt (.userProfileId p))))
    (is (= "Ada L" (opt (.name p)))))
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
    (is (= "d" (:description m)))))

(deftest agent-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
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
                          (.build)))
              (.multiagent (java.util.Optional/empty))
              (.name "helper")
              (.skills [(com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Skill/ofCustom
                         (-> (com.anthropic.models.beta.agents.BetaManagedAgentsCustomSkill/builder)
                             (.skillId "skill_1")
                             (.type (com.anthropic.models.beta.agents.BetaManagedAgentsCustomSkill$Type/of "custom"))
                             (.version "2")
                             (.build)))])
              (.system "be helpful")
              (.tools [(com.anthropic.models.beta.agents.BetaManagedAgentsAgent$Tool/ofMcpToolset
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
    (is (= [{:type :custom :skill-id "skill_1" :version "2"}] (:skills m)))
    (is (= [{:name "github" :url "https://mcp.example.test"}] (:mcp-servers m)))
    (is (= [{:type :mcp-toolset :mcp-server-name "github"}] (:tools m)))))

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
    (is (= {:data [{:type :user-message :id "evt_2"}]}
           (send-session-events->map sent-with-data)))))

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
    (is (= "idle" (:status m)))))

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
    (is (= "hello" (:content m)))
    (is (= {:id "mem_1" :deleted true} (memory-delete->map d)))))

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
    (is (= {:id "sv_1" :deleted true} (skill-version-delete->map d)))))

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
                             (.build)))
              (.name "nightly")
              (.pausedReason (java.util.Optional/empty))
              (.resources ^java.util.List (java.util.ArrayList.))
              (.schedule (java.util.Optional/empty))
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
    (is (= ["vault_1"] (:vault-ids m)))))

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
    (is (= "2026-07-04T00:00Z" (:created-at m)))))

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
                             (.build)))
              (.updatedAt "2026-07-04T00:00:00Z")
              (.build))
        d (-> (BetaEnvironmentDeleteResponse/builder)
              (.id "env_1")
              (.type (com.anthropic.core.JsonValue/from "environment_deleted"))
              (.build))
        m (environment->map r)]
    (is (= "env_1" (:id m)))
    (is (= "prod" (:name m)))
    (is (= "Production" (:description m)))
    (is (= {:id "env_1" :deleted true} (environment-delete->map d)))))

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
            :started-at "2026-07-04T00:00:30Z"
            :state "running"}
           (environment-work->map work)))
    (is (= {:last-heartbeat "2026-07-04T00:02:00Z"
            :lease-extended true
            :state "running"
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
                             (.build)))
              (.type (com.anthropic.models.beta.vaults.BetaManagedAgentsVault$Type/of "vault"))
              (.createdAt ts)
              (.updatedAt ts)
              (.build))
        d (-> (BetaManagedAgentsDeletedVault/builder)
              (.id "vault_1")
              (.type (com.anthropic.models.beta.vaults.BetaManagedAgentsDeletedVault$Type/of "vault_deleted"))
              (.build))
        m (vault->map r)]
    (is (= "vault_1" (:id m)))
    (is (= "Main Vault" (:display-name m)))
    (is (= "2026-07-04T00:00Z" (:updated-at m)))
    (is (= {:id "vault_1" :deleted true} (vault-delete->map d)))))

(deftest user-profile-response-mapping
  (let [ts (java.time.OffsetDateTime/parse "2026-07-04T00:00:00Z")
        r (-> (BetaUserProfile/builder)
              (.id "up_1")
              (.metadata (-> (com.anthropic.models.beta.userprofiles.BetaUserProfile$Metadata/builder)
                             (.build)))
              (.relationship (com.anthropic.models.beta.userprofiles.BetaUserProfile$Relationship/of "external"))
              (.trustGrants (-> (com.anthropic.models.beta.userprofiles.BetaUserProfile$TrustGrants/builder)
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
