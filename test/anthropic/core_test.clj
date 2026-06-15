(ns anthropic.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [anthropic.core :as a])
  (:import (com.anthropic.models.messages MessageCreateParams
                                          MessageCreateParams$ServiceTier
                                          RawContentBlockDelta
                                          RawContentBlockDeltaEvent
                                          RawContentBlockStartEvent
                                          RawContentBlockStartEvent$ContentBlock
                                          RawContentBlockStopEvent
                                          RawMessageStopEvent
                                          RawMessageStreamEvent
                                          InputJsonDelta TextDelta ThinkingDelta
                                          DirectCaller ToolUseBlock ToolUseBlock$Caller
                                          Usage Usage$Builder)
           (com.anthropic.models.models ModelInfo ModelInfo$Builder)))

(def ->params #'a/->params)
(def usage->map #'a/usage->map)
(def event->map #'a/event->map)
(def model->map #'a/model->map)

(defn- opt [^java.util.Optional o] (when (.isPresent o) (.get o)))

(deftest request-translation
  (testing "a request map becomes MessageCreateParams with the given model + max-tokens"
    (let [^MessageCreateParams p (->params {:model "claude-sonnet-4-6"
                                            :max-tokens 512
                                            :system "be terse"
                                            :messages [{:role :user :content "hi"}]})]
      (is (= "claude-sonnet-4-6" (str (.model p))))
      (is (= 512 (.maxTokens p)))))
  (testing "defaults: opus-4-8 and 1024 max-tokens"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]})]
      (is (= "claude-opus-4-8" (str (.model p))))
      (is (= 1024 (.maxTokens p))))))

(deftest sampling-and-control-params
  (testing "temperature, top-p, top-k, stop-sequences all flow through"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :temperature 0.5
                                            :top-p 0.9
                                            :top-k 40
                                            :stop-sequences ["STOP" "END"]})]
      (is (= 0.5 (opt (.temperature p))))
      (is (= 0.9 (opt (.topP p))))
      (is (= 40 (opt (.topK p))))
      (is (= ["STOP" "END"] (vec (opt (.stopSequences p)))))))
  (testing "metadata user-id"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :metadata {:user-id "u-123"}})]
      (is (= "u-123" (opt (.userId (opt (.metadata p))))))))
  (testing "service-tier maps keyword to the SDK enum"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :service-tier :standard-only})]
      (is (= MessageCreateParams$ServiceTier/STANDARD_ONLY (opt (.serviceTier p))))))
  (testing "tool-choice :any and the {:type :tool :name ...} form are both honored"
    (is (some? (opt (.toolChoice (->params {:messages [{:role :user :content "hi"}]
                                            :tool-choice :any})))))
    (is (some? (opt (.toolChoice (->params {:messages [{:role :user :content "hi"}]
                                            :tool-choice {:type :tool :name "get_weather"}}))))))
  (testing "thinking :enabled with a budget"
    (is (some? (opt (.thinking (->params {:messages [{:role :user :content "hi"}]
                                          :thinking {:type :enabled :budget-tokens 2048}})))))))

(def ^:private empty-opt (java.util.Optional/empty))

(defn- usage
  "Build a Usage with all SDK-required fields set; cache tokens optional."
  ^Usage [in out cc cr]
  (let [^Usage$Builder b (Usage/builder)]
    (doto b
      (.inputTokens (long in)) (.outputTokens (long out))
      (.cacheCreation empty-opt) (.serverToolUse empty-opt) (.serviceTier empty-opt)
      (.inferenceGeo empty-opt) (.outputTokensDetails empty-opt)
      (.cacheCreationInputTokens ^java.util.Optional (if cc (java.util.Optional/of (long cc)) empty-opt))
      (.cacheReadInputTokens ^java.util.Optional (if cr (java.util.Optional/of (long cr)) empty-opt)))
    (.build b)))

(deftest usage-cache-tokens
  (testing "cache tokens surface when present"
    (is (= {:input-tokens 10 :output-tokens 20
            :cache-creation-input-tokens 3 :cache-read-input-tokens 7}
           (usage->map (usage 10 20 3 7)))))
  (testing "absent cache tokens leave the keys off"
    (is (= {:input-tokens 10 :output-tokens 20}
           (usage->map (usage 10 20 nil nil))))))

(defn- model-info
  "Build a ModelInfo with required fields; token limits optional."
  ^ModelInfo [id display-name mit mt]
  (let [^ModelInfo$Builder b (ModelInfo/builder)]
    (doto b
      (.id id) (.displayName display-name)
      (.createdAt (java.time.OffsetDateTime/parse "2026-01-01T00:00:00Z"))
      (.type (com.anthropic.core.JsonValue/from "model"))
      (.capabilities empty-opt)
      (.maxInputTokens ^java.util.Optional (if mit (java.util.Optional/of (long mit)) empty-opt))
      (.maxTokens ^java.util.Optional (if mt (java.util.Optional/of (long mt)) empty-opt)))
    (.build b)))

(deftest model-mapping
  (let [m (model->map (model-info "claude-x" "Claude X" 200000 64000))]
    (is (= "claude-x" (:id m)))
    (is (= "Claude X" (:display-name m)))
    (is (= 200000 (:max-input-tokens m)))
    (is (= 64000 (:max-tokens m)))
    (is (string? (:created-at m)))
    (is (clojure.string/starts-with? (:created-at m) "2026-01-01")))
  (testing "absent optional token limits are omitted"
    (let [m (model->map (model-info "claude-y" "Claude Y" nil nil))]
      (is (not (contains? m :max-input-tokens)))
      (is (not (contains? m :max-tokens))))))

(defn- delta-event [^RawContentBlockDelta d]
  (RawMessageStreamEvent/ofContentBlockDelta
   (-> (RawContentBlockDeltaEvent/builder) (.index 0) (.delta d) (.build))))

(deftest stream-event-normalization
  (testing "text delta"
    (is (= {:type :text-delta :index 0 :text "hi"}
           (event->map (delta-event (RawContentBlockDelta/ofText
                                     (-> (TextDelta/builder) (.text "hi") (.build))))))))
  (testing "thinking delta"
    (is (= {:type :thinking-delta :index 0 :thinking "hmm"}
           (event->map (delta-event (RawContentBlockDelta/ofThinking
                                     (-> (ThinkingDelta/builder) (.thinking "hmm") (.build))))))))
  (testing "input-json delta (tool-use streaming)"
    (is (= {:type :input-json-delta :index 0 :partial-json "{\"ci"}
           (event->map (delta-event (RawContentBlockDelta/ofInputJson
                                     (-> (InputJsonDelta/builder) (.partialJson "{\"ci") (.build))))))))
  (testing "content-block-start for a tool_use block carries id + name"
    (let [tu (-> (ToolUseBlock/builder) (.id "tu_1") (.name "get_weather")
                 (.input (com.anthropic.core.JsonValue/from {}))
                 (.caller (ToolUseBlock$Caller/ofDirect (.build (DirectCaller/builder))))
                 (.build))
          ev (RawMessageStreamEvent/ofContentBlockStart
              (-> (RawContentBlockStartEvent/builder)
                  (.index 0)
                  (.contentBlock (RawContentBlockStartEvent$ContentBlock/ofToolUse tu))
                  (.build)))]
      (is (= {:type :content-block-start :index 0
              :block {:type :tool-use :id "tu_1" :name "get_weather"}}
             (event->map ev)))))
  (testing "content-block-stop and message-stop"
    (is (= {:type :content-block-stop :index 0}
           (event->map (RawMessageStreamEvent/ofContentBlockStop
                        (-> (RawContentBlockStopEvent/builder) (.index 0) (.build))))))
    (is (= {:type :message-stop}
           (event->map (RawMessageStreamEvent/ofMessageStop
                        (.build (RawMessageStopEvent/builder))))))))

;; Live round-trip — only runs when ANTHROPIC_API_KEY is set (network + billed).
(deftest ^:integration create-message-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [resp (a/create-message (a/client)
                                 {:model "claude-haiku-4-5"
                                  :max-tokens 16
                                  :messages [{:role :user :content "Reply with the single word: pong"}]})]
      (is (= :assistant (:role resp)))
      (is (= :text (:type (first (:content resp)))))
      (is (string? (:text (first (:content resp)))))
      (is (pos? (-> resp :usage :output-tokens))))))

(deftest ^:integration stream-text-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [deltas (atom [])
          full (a/stream-text (a/client)
                              {:model "claude-haiku-4-5"
                               :max-tokens 16
                               :messages [{:role :user :content "Reply with the single word: pong"}]}
                              #(swap! deltas conj %))]
      (is (string? full))
      (is (pos? (count full)))
      (is (= full (apply str @deltas))))))

(deftest ^:integration models-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [c (a/client)
          ms (a/list-models c)]
      (is (seq ms))
      (is (every? (comp string? :id) ms))
      (let [id (:id (first ms))]
        (is (= id (:id (a/get-model c id))))))))

(deftest ^:integration count-tokens-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [r (a/count-tokens (a/client)
                            {:model "claude-haiku-4-5"
                             :messages [{:role :user :content "How many tokens is this?"}]})]
      (is (pos? (:input-tokens r))))))

(deftest ^:integration stream-events-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [events (atom [])
          full (a/stream (a/client)
                         {:model "claude-haiku-4-5"
                          :max-tokens 16
                          :messages [{:role :user :content "Reply with the single word: pong"}]}
                         #(swap! events conj %))
          types (set (map :type @events))]
      (is (string? full))
      (is (pos? (count full)))
      (is (contains? types :message-start))
      (is (contains? types :content-block-start))
      (is (contains? types :text-delta))
      (is (contains? types :message-delta))
      (is (contains? types :message-stop))
      (is (= full (apply str (keep #(when (= :text-delta (:type %)) (:text %)) @events)))))))

(deftest ^:integration sampling-params-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [resp (a/create-message (a/client)
                                 {:model "claude-haiku-4-5"
                                  :max-tokens 16
                                  :temperature 0.0
                                  :stop-sequences ["STOP"]
                                  :metadata {:user-id "test-user"}
                                  :messages [{:role :user :content "Reply with the single word: pong"}]})]
      (is (= :assistant (:role resp)))
      (is (string? (:text (first (:content resp))))))))

(deftest ^:integration tool-use-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [c (a/client)
          tool {:name "get_weather"
                :description "Get the current weather for a city"
                :input-schema {:type "object"
                               :properties {:city {:type "string"}}
                               :required ["city"]}}
          ask {:role :user :content "Use the get_weather tool for Paris."}
          r1 (a/create-message c {:model "claude-haiku-4-5" :max-tokens 400
                                  :tools [tool] :messages [ask]})
          tu (first (filter #(= :tool-use (:type %)) (:content r1)))]
      (testing "Claude calls the tool with the parsed input"
        (is (= :tool-use (:stop-reason r1)))
        (is (= "get_weather" (:name tu)))
        (is (= "Paris" (get-in tu [:input :city]))))
      (testing "tool_result completes the loop"
        (let [r2 (a/create-message c {:model "claude-haiku-4-5" :max-tokens 100
                                      :tools [tool]
                                      :messages [ask
                                                 {:role :assistant :content (:content r1)}
                                                 {:role :user
                                                  :content [{:type :tool-result
                                                             :tool-use-id (:id tu)
                                                             :content "18C and sunny"}]}]})]
          (is (= :assistant (:role r2)))
          (is (some #(= :text (:type %)) (:content r2))))))))
