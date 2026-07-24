(ns anthropic.beta-messages-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [anthropic.beta.messages :as messages])
  (:import (com.anthropic.core JsonValue)
           (com.anthropic.core.http StreamResponse)
           (com.anthropic.models.beta.messages BetaMessage BetaTextBlock BetaUsage
                                               BetaMessageTokensCount MessageCountTokensParams
                                               MessageCreateParams BetaRawContentBlockDeltaEvent
                                               BetaRawMessageStreamEvent)
           (com.anthropic.models.beta.messages.batches BatchCreateParams
                                                       BetaDeletedMessageBatch
                                                       BetaMessageBatch
                                                       BetaMessageBatchIndividualResponse
                                                       BetaMessageBatchCanceledResult)))

(def ->params #'messages/->params)
(def ->count-params #'messages/->count-params)
(def beta-message->map #'messages/beta-message->map)
(def beta-tokens-count->map #'messages/beta-tokens-count->map)
(def ->batch-create-params #'messages/->batch-create-params)
(def ->batch-list-params #'messages/->batch-list-params)
(def batch->map #'messages/batch->map)
(def deleted-batch->map #'messages/deleted-batch->map)
(def reduce-beta-batch-result-stream #'messages/reduce-beta-batch-result-stream)
(def consume-beta-stream #'messages/consume-beta-stream)
(def parse-beta-text #'messages/parse-beta-text)
(def ->content-block #'messages/->content-block)
(def run-beta-tools* #'messages/run-beta-tools*)

(defn- json-value->clj [value]
  (cond
    (instance? java.util.Map value)
    (into {} (map (fn [[k v]] [(-> k str (str/replace "_" "-") keyword)
                               (json-value->clj v)])) value)
    (instance? java.util.List value) (mapv json-value->clj value)
    :else value))

(defn- json-roundtrip [value]
  (json-value->clj (.convert (JsonValue/from value) Object)))

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

(deftest beta-tool-change-content-blocks
  (doseq [[type tool expected-type expected-name]
          [[:tool-addition {:reference "get_weather"} "tool_addition" "get_weather"]
           [:tool-addition {:mcp-tool-reference {:name "lookup" :server-name "weather"}}
            "tool_addition" "lookup"]
           [:tool-addition {:mcp-toolset-reference {:server-name "weather"}}
            "tool_addition" nil]
           [:tool-removal {:reference "get_weather"} "tool_removal" "get_weather"]
           [:tool-removal {:mcp-tool-reference {:name "lookup" :server-name "weather"}}
            "tool_removal" "lookup"]
           [:tool-removal {:mcp-toolset-reference {:server-name "weather"}}
            "tool_removal" nil]]]
    (let [result (json-roundtrip (->content-block {:type type :tool tool
                                                   :cache-control {:ttl :1h}}))]
      (is (= expected-type (:type result)))
      (is (= expected-name (get-in result [:tool :name]))))))

(deftest beta-fallback-request-params
  (let [explicit (->params {:messages [{:role :user :content "hi"}]
                            :fallbacks [{:model "claude-sonnet-4-6" :max-tokens 256}]
                            :fallback-credit-token "credit-token"})
        default (->params {:messages [{:role :user :content "hi"}]
                           :fallbacks :default})
        explicit-json (json-roundtrip (._body explicit))
        default-json (json-roundtrip (._body default))]
    (is (= "claude-sonnet-4-6" (get-in explicit-json [:fallbacks 0 :model])))
    (is (= 256 (get-in explicit-json [:fallbacks 0 :max-tokens])))
    (is (= "credit-token" (:fallback-credit-token explicit-json)))
    (is (= "default" (:fallbacks default-json)))))

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
                                (.fallbackCredit (java.util.Optional/empty))
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

(deftest beta-batch-params-and-mapping
  (let [^BatchCreateParams p
        (->batch-create-params
         {:requests [{:custom-id "request_1"
                      :params {:model "claude-sonnet-4-6"
                               :max-tokens 32
                               :messages [{:role :user :content "hi"}]}}]})
        batch (-> (BetaMessageBatch/builder)
                  (.id "msgbatch_1")
                  (.archivedAt (java.util.Optional/empty))
                  (.cancelInitiatedAt (java.util.Optional/empty))
                  (.endedAt (java.util.Optional/empty))
                  (.resultsUrl (java.util.Optional/empty))
                  (.createdAt (java.time.OffsetDateTime/parse "2026-07-22T00:00:00Z"))
                  (.expiresAt (java.time.OffsetDateTime/parse "2026-07-23T00:00:00Z"))
                  (.processingStatus (com.anthropic.models.beta.messages.batches.BetaMessageBatch$ProcessingStatus/of "in_progress"))
                  (.requestCounts (-> (com.anthropic.models.beta.messages.batches.BetaMessageBatchRequestCounts/builder)
                                      (.processing 1) (.succeeded 0) (.errored 0) (.canceled 0) (.expired 0) (.build)))
                  (.type (JsonValue/from "message_batch"))
                  (.build))]
    (is (= 1 (count (.requests p))))
    (is (= "request_1" (.customId (first (.requests p)))))
    (is (= :message-batch (:type (batch->map batch))))
    (is (= :in-progress (:processing-status (batch->map batch))))))

(deftest beta-batch-delete-and-stream-reduction
  (let [deleted (-> (BetaDeletedMessageBatch/builder)
                    (.id "msgbatch_1") (.type (JsonValue/from "message_batch_deleted")) (.build))
        closed? (atom false)
        response (-> (BetaMessageBatchIndividualResponse/builder)
                     (.customId "request_1")
                     (.result (.build (BetaMessageBatchCanceledResult/builder)))
                     (.build))
        sr (reify StreamResponse
             (stream [_] (.stream (java.util.ArrayList. [response])))
             (close [_] (reset! closed? true)))]
    (is (= {:id "msgbatch_1" :type :message-batch-deleted} (deleted-batch->map deleted)))
    (is (= ["request_1"]
           (reduce-beta-batch-result-stream sr (fn [acc result] (conj acc (:custom-id result))) [])))
    (is @closed?)))

(deftest beta-batch-list-params
  (let [params (->batch-list-params {:after-id "msgbatch_1" :limit 10})]
    (is (fn? messages/list-beta-batches))
    (is (= "msgbatch_1" (opt (.afterId params))))
    (is (= 10 (opt (.limit params))))))

(deftest beta-message-stream-consumption
  (let [closed? (atom false)
        seen (atom [])
        delta (-> (BetaRawContentBlockDeltaEvent/builder)
                  (.textDelta "hello") (.index 0) (.type (JsonValue/from "content_block_delta")) (.build))
        events [(BetaRawMessageStreamEvent/ofContentBlockDelta delta)
                (BetaRawMessageStreamEvent/ofContentBlockDelta
                 (-> (BetaRawContentBlockDeltaEvent/builder)
                     (.textDelta " world") (.index 0) (.type (JsonValue/from "content_block_delta")) (.build)))]
        sr (reify StreamResponse
             (stream [_] (.stream (java.util.ArrayList. events)))
             (close [_] (reset! closed? true)))]
    (is (= "hello world" (consume-beta-stream sr #(swap! seen conj %))))
    (is (= [:content-block-delta :content-block-delta] (mapv :type @seen)))
    (is @closed?)))

(deftest run-beta-tools-loop
  (let [calls (atom [])
        tool-inputs (atom [])
        seen-responses (atom [])
        tool-input {:city "San Francisco"}
        tool-fn (fn [input]
                  (swap! tool-inputs conj input)
                  {:forecast (str "sunny in " (:city input))})
        responses [{:id "msg_tool"
                    :stop-reason :tool-use
                    :content [{:type :tool-use :id "toolu_123" :name "weather"
                               :input tool-input}]}
                   {:id "msg_final" :stop-reason :end-turn
                    :content [{:type :text :text "It is sunny."}]}]
        params {:model "claude-sonnet-4-6"
                :messages [{:role :user :content "What's the weather?"}]
                :tools [{:name "weather" :input-schema {:type "object"}
                         :fn tool-fn}]}]
    (with-redefs [messages/create-beta-message
                  (fn [_ req]
                    (swap! calls conj req)
                    (nth responses (dec (count @calls))))]
      (let [result (messages/run-beta-tools nil params {:on-message #(swap! seen-responses conj %)})]
        (is (= "msg_final" (:id result)))
        (is (= [tool-input] @tool-inputs))
        (is (= {:forecast "sunny in San Francisco"}
               (get-in result [:messages 2 :content 0 :content])))
        (is (= tool-input
               (get-in result [:messages 1 :content 0 :input])))
        (is (= 2 (count @calls)))
        (is (= responses @seen-responses))
        (is (nil? (get-in @calls [0 :tools 0 :fn])))))
    (reset! calls [])
    (with-redefs [messages/create-beta-message
                  (fn [_ req]
                    (swap! calls conj req)
                    (first responses))]
      (is (thrown? clojure.lang.ExceptionInfo
                   (messages/run-beta-tools nil params {:max-iterations 1})))
      (is (= 1 (count @calls))))))

(deftest run-beta-tools-refreshes-tool-functions-after-on-turn
  (let [calls (atom 0)
        used (atom [])
        response (fn [stop name]
                   {:stop-reason stop
                    :content (if name [{:type :tool-use :id (str "id-" name)
                                        :name name :input {}}]
                                 [{:type :text :text "done"}])})
        first-fn (fn [_] (swap! used conj :first))
        second-fn (fn [_] (swap! used conj :second))
        params {:messages "start"
                :tools [{:name "first" :input-schema {}
                         :fn first-fn}]}
        call-fn (fn [_]
                  (case (swap! calls inc)
                    1 (response :tool-use "first")
                    2 (response :tool-use "second")
                    3 (response :end-turn nil)))]
    (is (= :end-turn (:stop-reason
                      (run-beta-tools* call-fn params
                                       {:on-turn (fn [_ p]
                                                   (update p :tools conj
                                                           {:name "second" :input-schema {}
                                                            :fn second-fn}))}))))
    (is (= [:first :second] @used))))

(deftest run-beta-tools-honors-tools-removed-by-on-turn
  (let [params {:messages "start"
                :tools [{:name "weather" :input-schema {} :fn identity}]}
        call-fn (fn [_]
                  {:stop-reason :tool-use
                   :content [{:type :tool-use :id "id-weather"
                              :name "weather" :input {}}]})]
    (let [error (try
                  (run-beta-tools* call-fn params
                                   {:on-turn (fn [_ p] (assoc p :tools []))
                                    :max-iterations 2})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= "Tool call has no matching :fn" (.getMessage error))))))

(deftest beta-structured-output-parsing
  (is (= {:capital "Sacramento"}
         (parse-beta-text {:content [{:type :text
                                      :text "{\"capital\":\"Sacramento\"}"}]}))))
