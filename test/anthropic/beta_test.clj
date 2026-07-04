(ns anthropic.beta-test
  (:require [clojure.test :refer [deftest is testing]]
            [anthropic.beta :as beta])
  (:import (com.anthropic.models.beta.skills SkillCreateParams
                                             SkillCreateResponse)
           (com.anthropic.models.beta.memorystores BetaManagedAgentsMemoryStore
                                                   MemoryStoreCreateParams
                                                   MemoryStoreUpdateParams)
           (com.anthropic.models.beta.agents AgentCreateParams
                                             AgentUpdateParams)
           (com.anthropic.models.beta.sessions SessionCreateParams
                                               SessionUpdateParams)))

(def ->skill-create-params #'beta/->skill-create-params)
(def ->memory-store-create-params #'beta/->memory-store-create-params)
(def ->memory-store-update-params #'beta/->memory-store-update-params)
(def ->agent-create-params #'beta/->agent-create-params)
(def ->agent-update-params #'beta/->agent-update-params)
(def ->session-create-params #'beta/->session-create-params)
(def ->session-update-params #'beta/->session-update-params)
(def skill-create->map #'beta/skill-create->map)
(def memory-store->map #'beta/memory-store->map)

(defn- opt [^java.util.Optional o] (when (.isPresent o) (.get o)))

(defn- ex-data-for [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

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
                               :system "be helpful" :description "d"})]
    (is (= "helper" (.name p)))
    (is (= "be helpful" (opt (.system p)))))
  (let [^AgentUpdateParams p (->agent-update-params "agent_1" {:version 2 :system "new"})]
    (is (= "new" (opt (.system p)))))
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
