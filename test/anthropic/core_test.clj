(ns anthropic.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [jsonista.core :as json]
            [anthropic.core :as a])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.models.messages CacheCreation Container
                                          Message MessageCountTokensParams
                                          MessageCreateParams
                                          MessageCreateParams$ServiceTier
                                          RawContentBlockDelta
                                          RawContentBlockDeltaEvent
                                          RawContentBlockStartEvent
                                          RawContentBlockStartEvent$ContentBlock
                                          RawContentBlockStopEvent
                                          RawMessageStopEvent
                                          RawMessageStreamEvent
                                          RefusalStopDetails RefusalStopDetails$Category
                                          InputJsonDelta TextDelta ThinkingDelta
                                          DirectCaller ToolUseBlock ToolUseBlock$Caller
                                          OutputConfig OutputTokensDetails
                                          ServerToolUsage Usage Usage$Builder
                                          StopReason Usage$ServiceTier)
           (com.anthropic.models.models ModelInfo ModelInfo$Builder)
           (com.anthropic.models.beta.files FileMetadata)
           (com.anthropic.models.messages.batches BatchCreateParams$Request
                                                  BatchCreateParams$Request$Params)))

(def ->params #'a/->params)
(def ->count-params #'a/->count-params)
(def usage->map #'a/usage->map)
(def message->map #'a/message->map)
(def event->map #'a/event->map)
(def model->map #'a/model->map)
(def parse-text #'a/parse-text)
(def ->batch-request #'a/->batch-request)
(def ->content-block #'a/->content-block)
(def ->tool #'a/->tool)
(def file->map #'a/file->map)

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

(deftest client-construction
  (doseq [opts [{:api-key "sk-test"}
                {:auth-token "token-test"}
                {:base-url "https://api.anthropic.com"}
                {:timeout-ms 1000}
                {:max-retries 1}]]
    (testing (str "client option " opts)
      (is (instance? AnthropicClient (a/client opts))))))

(deftest newer-request-params
  (testing "create params include container, inference-geo, user-profile-id, cache-control"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :container "container_123"
                                            :inference-geo "us"
                                            :user-profile-id "user_123"
                                            :cache-control true})]
      (is (= "container_123" (opt (.container p))))
      (is (= "us" (opt (.inferenceGeo p))))
      (is (= "user_123" (opt (.userProfileId p))))
      (is (some? (opt (.cacheControl p))))))
  (testing "count params include supported shared keys and skip unsupported keys"
    (let [^MessageCountTokensParams p (->count-params {:messages [{:role :user :content "hi"}]
                                                       :container "container_123"
                                                       :inference-geo "us"
                                                       :user-profile-id "user_123"
                                                       :cache-control true
                                                       :response-format {:type "object"}})]
      (is (= "user_123" (opt (.userProfileId p))))
      (is (some? (opt (.cacheControl p))))
      (is (some? (opt (.outputConfig p)))))))

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

(deftest server-tools
  (testing "each server-tool spec maps to the right ToolUnion variant"
    (is (.isPresent (.webSearchTool20260318 (->tool {:type :web-search :max-uses 3
                                                     :allowed-domains ["clojure.org"]}))))
    (is (.isPresent (.webFetchTool20260318 (->tool {:type :web-fetch}))))
    (is (.isPresent (.codeExecutionTool20260521 (->tool {:type :code-execution}))))
    (is (.isPresent (.bash20250124 (->tool {:type :bash}))))
    (is (.isPresent (.textEditor20250728 (->tool {:type :text-editor :max-characters 1000}))))
    (is (.isPresent (.memoryTool20250818 (->tool {:type :memory})))))
  (testing "code-execution carries allowed-callers"
    (let [ce (.get (.codeExecutionTool20260521
                    (->tool {:type :code-execution :allowed-callers [:direct]})))
          callers (.get (.allowedCallers ce))]
      (is (= 1 (count callers)))
      (is (= "direct" (str (first callers))))))
  (testing "a custom tool maps to ofTool"
    (is (.isPresent (.tool (->tool {:name "x"
                                    :input-schema {:type "object" :properties {}}}))))))

(deftest count-token-tools
  (testing "custom and server tools are both present in count-token params"
    (let [^MessageCountTokensParams p
          (->count-params {:messages [{:role :user :content "hi"}]
                           :tools [{:name "x"
                                    :input-schema {:type "object" :properties {}}}
                                   {:type :web-search :max-uses 2}]})
          tools (vec (opt (.tools p)))]
      (is (= 2 (count tools)))
      (is (.isPresent (.tool (first tools))))
      (is (.isPresent (.webSearchTool20260318 (second tools)))))))

(deftest content-blocks
  (testing "image block, base64 and url sources"
    (is (.isPresent (.image (->content-block {:type :image
                                              :source {:type :base64
                                                       :media-type "image/png"
                                                       :data "abc123"}}))))
    (is (.isPresent (.image (->content-block {:type :image
                                              :source {:type :url
                                                       :url "https://x/i.png"}})))))
  (testing "document block, base64 / url / text sources"
    (doseq [src [{:type :base64 :data "JVBER"}
                 {:type :url :url "https://x/d.pdf"}
                 {:type :text :data "plain text doc"}]]
      (is (.isPresent (.document (->content-block {:type :document :source src}))))))
  (testing "cache-control attaches to a text block"
    (let [cb (->content-block {:type :text :text "cache me" :cache-control true})]
      (is (.isPresent (.cacheControl (.get (.text cb)))))))
  (testing "cache-control with a ttl"
    (let [cb (->content-block {:type :text :text "x" :cache-control {:ttl :1h}})]
      (is (.isPresent (.cacheControl (.get (.text cb)))))))
  (testing "tool-result map content is encoded as JSON"
    (let [cb (->content-block {:type :tool-result
                               :tool-use-id "toolu_1"
                               :content {:x 1 :nested {:ok true}}})
          content (.get (.content (.get (.toolResult cb))))]
      (is (= {:x 1 :nested {:ok true}}
             (json/read-value (.asString content) (json/object-mapper {:decode-key-fn true}))))))
  (testing "tool-result string content passes through unchanged"
    (let [cb (->content-block {:type :tool-result
                               :tool-use-id "toolu_1"
                               :content "plain text"})
          content (.get (.content (.get (.toolResult cb))))]
      (is (= "plain text" (.asString content))))))

(deftest structured-output-params
  (let [schema {:type "object"
                :properties {:answer {:type "string"}}
                :required ["answer"]}]
    (testing ":response-format sets output_config.format"
      (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                              :response-format schema})
            oc ^OutputConfig (opt (.outputConfig p))]
        (is (some? oc))
        (is (.isPresent (.format oc)))))
    (testing ":effort sets output_config.effort"
      (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                              :effort :low})
            oc ^OutputConfig (opt (.outputConfig p))]
        (is (some? oc))
        (is (.isPresent (.effort oc)))))
    (testing "no structured keys -> no output_config"
      (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]})]
        (is (not (.isPresent (.outputConfig p))))))))

(deftest structured-parse
  (testing "parse-text decodes the first text block as JSON with keyword keys"
    (is (= {:answer "blue" :n 7}
           (parse-text {:content [{:type :text :text "{\"answer\":\"blue\",\"n\":7}"}]}))))
  (testing "no text block -> nil"
    (is (nil? (parse-text {:content [{:type :tool-use :id "x" :name "y" :input {}}]})))))

(deftest batch-request-translation
  (let [^BatchCreateParams$Request r
        (->batch-request {:custom-id "r1"
                          :params {:model "claude-haiku-4-5" :max-tokens 64
                                   :system "be terse"
                                   :messages [{:role :user :content "hi"}]}})]
    (is (= "r1" (.customId r)))
    (let [^BatchCreateParams$Request$Params p (.params r)]
      (is (= "claude-haiku-4-5" (str (.model p))))
      (is (= 64 (.maxTokens p))))))

(deftest usage-cache-tokens
  (testing "cache tokens surface when present"
    (is (= {:input-tokens 10 :output-tokens 20
            :cache-creation-input-tokens 3 :cache-read-input-tokens 7}
           (usage->map (usage 10 20 3 7)))))
  (testing "absent cache tokens leave the keys off"
    (is (= {:input-tokens 10 :output-tokens 20}
           (usage->map (usage 10 20 nil nil))))))

(deftest usage-completeness
  (let [u (-> (Usage/builder)
              (.inputTokens 10)
              (.outputTokens 20)
              (.cacheCreation (-> (CacheCreation/builder)
                                  (.ephemeral1hInputTokens 3)
                                  (.ephemeral5mInputTokens 4)
                                  (.build)))
              (.cacheCreationInputTokens 7)
              (.cacheReadInputTokens 8)
              (.serverToolUse (-> (ServerToolUsage/builder)
                                  (.webSearchRequests 2)
                                  (.webFetchRequests 1)
                                  (.build)))
              (.serviceTier Usage$ServiceTier/PRIORITY)
              (.inferenceGeo "us")
              (.outputTokensDetails (-> (OutputTokensDetails/builder)
                                        (.thinkingTokens 5)
                                        (.build)))
              (.build))]
    (is (= {:input-tokens 10
            :output-tokens 20
            :cache-creation-input-tokens 7
            :cache-read-input-tokens 8
            :server-tool-use {:web-fetch-requests 1
                              :web-search-requests 2}
            :service-tier :priority
            :cache-creation {:ephemeral-1h-input-tokens 3
                             :ephemeral-5m-input-tokens 4}
            :inference-geo "us"
            :output-tokens-details {:thinking-tokens 5}}
           (usage->map u)))))

(deftest message-completeness
  (let [m (-> (Message/builder)
              (.id "msg_1")
              (.model "claude-haiku-4-5")
              (.role (com.anthropic.core.JsonValue/from "assistant"))
              (.type (com.anthropic.core.JsonValue/from "message"))
              (.content [])
              (.usage (usage 1 2 nil nil))
              (.container (-> (Container/builder)
                              (.id "container_123")
                              (.expiresAt (java.time.OffsetDateTime/parse "2026-01-01T00:00:00Z"))
                              (.build)))
              (.stopReason StopReason/STOP_SEQUENCE)
              (.stopSequence "END")
              (.stopDetails (-> (RefusalStopDetails/builder)
                                (.category RefusalStopDetails$Category/CYBER)
                                (.explanation "blocked")
                                (.build)))
              (.build))
        mm (message->map m)]
    (is (= {:id "container_123" :expires-at "2026-01-01T00:00Z"} (:container mm)))
    (is (= "END" (:stop-sequence mm)))
    (is (= {:category :cyber :explanation "blocked"} (:stop-details mm)))))

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

(deftest file-mapping
  (let [fm (-> (FileMetadata/builder)
               (.id "file_1") (.filename "a.txt") (.mimeType "text/plain")
               (.sizeBytes 5)
               (.createdAt (java.time.OffsetDateTime/parse "2026-01-01T00:00:00Z"))
               (.type (com.anthropic.core.JsonValue/from "file"))
               (.build))
        m (file->map fm)]
    (is (= "file_1" (:id m)))
    (is (= "a.txt" (:filename m)))
    (is (= "text/plain" (:mime-type m)))
    (is (= 5 (:size-bytes m)))
    (is (str/starts-with? (:created-at m) "2026-01-01"))))

(deftest model-mapping
  (let [m (model->map (model-info "claude-x" "Claude X" 200000 64000))]
    (is (= "claude-x" (:id m)))
    (is (= "Claude X" (:display-name m)))
    (is (= 200000 (:max-input-tokens m)))
    (is (= 64000 (:max-tokens m)))
    (is (string? (:created-at m)))
    (is (str/starts-with? (:created-at m) "2026-01-01")))
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

(deftest ^:integration batch-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [c (a/client)
          b (a/create-batch c [{:custom-id "r1"
                                :params {:model "claude-haiku-4-5" :max-tokens 16
                                         :messages [{:role :user :content "Reply pong"}]}}])
          id (:id b)]
      (is (string? id))
      (is (keyword? (:processing-status b)))
      (is (map? (:request-counts b)))
      (is (= id (:id (a/get-batch c id))))
      (is (some #(= id (:id %)) (a/list-batches c)))
      ;; batches run async; cancel the in-flight one rather than wait for results
      (is (= id (:id (a/cancel-batch c id)))))))

(deftest ^:integration web-search-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [resp (a/create-message
                (a/client)
                {:model "claude-haiku-4-5" :max-tokens 512
                 :tools [{:type :web-search :max-uses 2 :allowed-callers [:direct]}]
                 :messages [{:role :user
                             :content "Search the web: what is the latest stable Clojure version?"}]})
          types (set (map :type (:content resp)))]
      (is (= :assistant (:role resp)))
      (is (some #(= :text (:type %)) (:content resp)))
      (is (contains? types :server-tool-use))
      (is (contains? types :web-search-result))
      ;; the cited answer text carries web-search-result-location citations
      (let [cites (mapcat :citations (filter :citations (:content resp)))]
        (is (seq cites))
        (is (every? :cited-text cites))))))

(deftest ^:integration files-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [c (a/client)
          tmp (java.io.File/createTempFile "anthropic-clj" ".txt")]
      (spit tmp "hello from anthropic-clj")
      (let [up (a/upload-file c tmp)
            id (:id up)]
        (is (string? id))
        (is (string? (:mime-type up)))
        (is (pos? (:size-bytes up)))
        (is (= id (:id (a/get-file c id))))
        (is (some #(= id (:id %)) (a/list-files c)))
        ;; download-file works only for API-generated downloadable files, not
        ;; user uploads, so it can't be exercised against this fixture.
        (is (:deleted (a/delete-file c id))))
      (.delete tmp))))

(deftest ^:integration vision-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [png (str "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAT0lEQVR42u3PQQkA"
                   "AAgEsEty/aMYywi+hcEKLNO+FgEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB"
                   "AQEBAQEBAQEBAQEBAQEBAQGBywLKp0DxvxLbjwAAAABJRU5ErkJggg==")
          resp (a/create-message
                (a/client)
                {:model "claude-haiku-4-5" :max-tokens 16
                 :messages [{:role :user
                             :content [{:type :image
                                        :source {:type :base64 :media-type "image/png" :data png}}
                                       {:type :text :text "Reply with the single word: ok"}]}]})]
      (is (= :assistant (:role resp)))
      (is (string? (:text (first (:content resp))))))))

(deftest ^:integration structured-output-roundtrip
  (when (System/getenv "ANTHROPIC_API_KEY")
    (let [resp (a/create-message (a/client)
                                 {:model "claude-haiku-4-5" :max-tokens 256
                                  :response-format {:type "object"
                                                    :properties {:capital {:type "string"}}
                                                    :required ["capital"]
                                                    :additionalProperties false}
                                  :messages [{:role :user
                                              :content "What is the capital of France?"}]})]
      (is (map? (:parsed resp)))
      (is (string? (get-in resp [:parsed :capital]))))))

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
