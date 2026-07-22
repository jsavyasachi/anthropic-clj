(ns anthropic.beta-messages-test
  (:require [clojure.test :refer [deftest is testing]]
            [anthropic.beta.messages :as messages])
  (:import (com.anthropic.core JsonValue)
           (com.anthropic.models.beta.messages BetaMessage BetaTextBlock BetaUsage
                                               BetaMessageTokensCount MessageCountTokensParams
                                               MessageCreateParams)))

(def ->params #'messages/->params)
(def ->count-params #'messages/->count-params)
(def beta-message->map #'messages/beta-message->map)
(def beta-tokens-count->map #'messages/beta-tokens-count->map)

(defn- opt [o] (when (.isPresent o) (.get o)))

(deftest create-beta-message-request-translation
  (let [^MessageCreateParams p
        (->params {:model "claude-sonnet-4-6"
                   :max-tokens 512
                   :system [{:text "be terse" :cache-control true}]
                   :messages [{:role :user :content "hi"}]
                   :tools [{:name "weather"
                            :description "Get weather"
                            :input-schema {:type "object" :properties {}}}]
                   :betas [:token-efficient-tools-2025-02-19]
                   :thinking {:type :enabled :budget-tokens 2048}})]
    (is (= "claude-sonnet-4-6" (str (.model p))))
    (is (= 512 (.maxTokens p)))
    (is (= ["token-efficient-tools-2025-02-19"]
           (mapv str (opt (.betas p)))))
    (is (= 1 (count (opt (.tools p)))))
    (is (.isPresent (.thinking p)))))

(deftest rich-content-request-translation
  (let [^MessageCreateParams p
        (->params {:messages [{:role :user
                               :content [{:type :text :text "describe this"}
                                         {:type :image
                                          :source {:type :base64
                                                   :media-type "image/png"
                                                   :data "aGVsbG8="}}
                                         {:type :tool-result
                                          :tool-use-id "toolu_123"
                                          :content "sunny"}]}]})]
    (is (= 3 (count (.asBetaContentBlockParams (.content (first (.messages p)))))))))

(deftest beta-message-json-conversion
  (let [message (-> (BetaMessage/builder)
                    (.id "msg_123")
                    (.model "claude-sonnet-4-6")
                    (.container (java.util.Optional/empty))
                    (.contextManagement (java.util.Optional/empty))
                    (.diagnostics (java.util.Optional/empty))
                    (.role (JsonValue/from "assistant"))
                    (.addContent (-> (BetaTextBlock/builder) (.citations []) (.text "hello") (.build)))
                    (.stopReason (com.anthropic.models.beta.messages.BetaStopReason/of "end_turn"))
                    (.stopDetails (java.util.Optional/empty))
                    (.stopSequence (java.util.Optional/empty))
                    (.type (JsonValue/from "message"))
                    (.usage (-> (BetaUsage/builder)
                                (.inputTokens 12)
                                (.outputTokens 4)
                                (.cacheCreation (java.util.Optional/empty))
                                (.cacheCreationInputTokens (java.util.Optional/empty))
                                (.cacheReadInputTokens (java.util.Optional/empty))
                                (.inferenceGeo (java.util.Optional/empty))
                                (.iterations (java.util.Optional/empty))
                                (.outputTokensDetails (java.util.Optional/empty))
                                (.serverToolUse (java.util.Optional/empty))
                                (.serviceTier (java.util.Optional/empty))
                                (.speed (java.util.Optional/empty))
                                (.build)))
                    (.build))
        result (beta-message->map message)]
    (is (= :assistant (:role result)))
    (is (= :end-turn (:stop-reason result)))
    (is (= :message (:type result)))
    (is (= {:input-tokens 12 :output-tokens 4} (:usage result)))))

(deftest count-beta-tokens-translation-and-conversion
  (let [^MessageCountTokensParams p
        (->count-params {:model "claude-sonnet-4-6"
                         :system "be terse"
                         :messages [{:role :user :content "hi"}]
                         :tools [{:name "weather" :input-schema {:type "object" :properties {}}}]
                         :tool-choice :auto
                         :thinking {:type :enabled :budget-tokens 2048}
                         :betas ["token-efficient-tools-2025-02-19"]})
        tokens-count (-> (BetaMessageTokensCount/builder)
                         (.inputTokens 37)
                         (.contextManagement
                          (-> (com.anthropic.models.beta.messages.BetaCountTokensContextManagementResponse/builder)
                              (.originalInputTokens 37)
                              (.build)))
                         (.build))]
    (is (= "claude-sonnet-4-6" (str (.model p))))
    (is (= 1 (count (opt (.tools p)))))
    (is (= ["token-efficient-tools-2025-02-19"] (mapv str (opt (.betas p)))))
    (is (= {:input-tokens 37} (beta-tokens-count->map tokens-count)))))
