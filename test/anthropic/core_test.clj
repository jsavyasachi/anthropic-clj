(ns anthropic.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [jsonista.core :as json]
            [anthropic.core :as a]
            [anthropic.beta.messages :as beta])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue)
           (com.anthropic.core.http StreamResponse)
           (com.anthropic.models.messages CacheCreation Container
                                          ContainerSkill ContainerSkill$Type
                                          Message MessageCountTokensParams
                                          MessageCountTokensTool
                                          MessageCreateParams
                                          MessageCreateParams$System
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
                                          DirectCaller ServerToolCaller ServerToolCaller20260120
                                          TextBlockParam Tool ToolTextEditor20250124
                                          ToolTextEditor20250429 ToolUseBlock ToolUseBlock$Caller
                                          ToolChoice ToolUnion WebFetchTool20260318
                                          OutputConfig OutputTokensDetails
                                          ServerToolUsage Usage Usage$Builder
                                          StopReason ToolResultBlockParam
                                          Usage$ServiceTier)
           (com.anthropic.models.models ModelInfo ModelInfo$Builder)
           (com.anthropic.models.files DeletedFile DeletedFile$Type FileMetadata)
           (com.anthropic.models.beta.messages BetaTool BetaToolUnion
                                               BetaWebSearchTool20260318
                                               BetaWebFetchTool20260318
                                               BetaToolBash20250124
                                               BetaToolTextEditor20250728
                                               BetaMemoryTool20250818)
           (com.anthropic.models.messages.batches BatchCreateParams$Request
                                                  BatchCreateParams$Request$Params
                                                  DeletedMessageBatch
                                                  MessageBatchCanceledResult
                                                  MessageBatchIndividualResponse)
           ))

(def ->params #'a/->params)
(def ->tool-choice #'a/->tool-choice)
(def ->count-params #'a/->count-params)
(def usage->map #'a/usage->map)
(def message->map #'a/message->map)
(def event->map #'a/event->map)
(def model->map #'a/model->map)
(def ->model-list-params
  #(when-let [v (ns-resolve 'anthropic.core '->model-list-params)]
     ((deref v) %)))
(def parse-text #'a/parse-text)
(def ->structured-schema #'a/->structured-schema)
(def validate-parsed! #'a/validate-parsed!)
(def parsed-response #'a/parsed-response)
(def ->batch-request #'a/->batch-request)
(def ->content-block #'a/->content-block)
(def ->tool #'a/->tool)
(def beta->tool #'beta/->tool)
(def ->count-tool #'a/->count-tool)
(def reduce-batch-result-stream #'a/reduce-batch-result-stream)
(def file->map #'a/file->map)
(def deleted-file->map
  #(when-let [v (ns-resolve 'anthropic.core 'deleted-file->map)]
     ((deref v) %)))
(def deleted-batch->map
  #(when-let [v (ns-resolve 'anthropic.core 'deleted-batch->map)]
     ((deref v) %)))
(def run-tools* #'a/run-tools*)

(defn- resolved-fn [sym]
  (some-> (ns-resolve 'anthropic.core sym) deref))

(defn- opt [^java.util.Optional o] (when (.isPresent o) (.get o)))

(defn- input-example->map [^com.anthropic.models.messages.Tool$InputExample example]
  (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)]))
        (._additionalProperties example)))

(defn- beta-input-example->map [^com.anthropic.models.beta.messages.BetaTool$InputExample example]
  (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)]))
        (._additionalProperties example)))

(defn- stable-custom-tool-schema-shape [^Tool tool]
  (let [schema (.inputSchema tool)
        properties (when (.isPresent (.properties schema))
                     (.get (.properties schema)))]
    {:type (.convert ^JsonValue (._type schema) Object)
     :properties (when properties
                   (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)])
                                 (._additionalProperties properties))))
     :required (when (.isPresent (.required schema))
                 (vec (.get (.required schema))))}))

(defn- beta-custom-tool-schema-shape [^BetaTool tool]
  (let [schema (.inputSchema tool)
        properties (opt (.properties schema))]
    {:type (.convert ^JsonValue (._type schema) Object)
     :properties (when properties
                   (into {} (map (fn [[k v]] [k (.convert ^JsonValue v Object)])
                                 (._additionalProperties properties))))
     :required (when (.isPresent (.required schema))
                 (vec (.get (.required schema))))}))

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

(deftest named-model-keywords
  (testing "public model aliases expose the verified SDK model ids"
    (is (= 17 (count a/models)))
    (is (= "claude-opus-5" (:claude-opus-5 a/models)))
    (is (= "claude-opus-4-8" (:claude-opus-4-8 a/models))))
  (testing "a keyword model builds the same message params as its string id"
    (let [req {:max-tokens 512 :messages [{:role :user :content "hi"}]}
          ^MessageCreateParams keyword-params (->params (assoc req :model :claude-opus-4-8))
          ^MessageCreateParams string-params (->params (assoc req :model "claude-opus-4-8"))]
      (is (= (str (.model string-params)) (str (.model keyword-params))))))
  (testing "raw model ids remain accepted"
    (is (= "claude-opus-5"
           (str (.model ^MessageCreateParams
                        (->params {:model "claude-opus-5"
                                   :messages [{:role :user :content "hi"}]}))))))
  (testing "unknown model keywords provide structured error data"
    (let [error (try
                  (->params {:model :nope :messages [{:role :user :content "hi"}]})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (instance? clojure.lang.ExceptionInfo error))
      (is (= :unknown-model (:anthropic/error (ex-data error)))))))

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
  (testing "tool-choice :any and the {:type :tool :name ...} form are both supported"
    (is (some? (opt (.toolChoice (->params {:messages [{:role :user :content "hi"}]
                                            :tool-choice :any})))))
    (is (some? (opt (.toolChoice (->params {:messages [{:role :user :content "hi"}]
                                            :tool-choice {:type :tool :name "get_weather"}}))))))
  (testing "thinking :enabled with a budget"
    (is (some? (opt (.thinking (->params {:messages [{:role :user :content "hi"}]
                                          :thinking {:type :enabled :budget-tokens 2048}})))))))

(def web-fetch-tool-spec
  {:type :web-fetch :name "web-fetch"
   :max-content-tokens 2048
   :use-cache true
   :citations {:enabled true}})

(deftest stable-web-fetch-use-cache
  (let [^WebFetchTool20260318 web-fetch
        (.asWebFetchTool20260318 ^ToolUnion (->tool web-fetch-tool-spec))]
    (is (= true (opt (.useCache web-fetch))))))

(deftest stable-web-fetch-citations
  (let [^WebFetchTool20260318 web-fetch
        (.asWebFetchTool20260318 ^ToolUnion (->tool web-fetch-tool-spec))]
    (is (= true (when (.isPresent (.citations web-fetch))
                  (opt (.enabled (opt (.citations web-fetch)))))))))

(deftest stable-web-tools-response-inclusion
  (doseq [[type stable-accessor beta-accessor]
          [[:web-search #(.webSearchTool20260318 ^ToolUnion %)
           #(.webSearchTool20260318 ^BetaToolUnion %)]
           [:web-fetch #(.webFetchTool20260318 ^ToolUnion %)
           #(.webFetchTool20260318 ^BetaToolUnion %)]]]
    (let [spec {:type type :response-inclusion :full}
          stable-tool (opt (stable-accessor (->tool spec)))
          beta-tool (opt (beta-accessor (beta->tool spec)))]
      (is (= (case type
               :web-search (str (opt (.responseInclusion ^BetaWebSearchTool20260318 beta-tool)))
               :web-fetch (str (opt (.responseInclusion ^BetaWebFetchTool20260318 beta-tool))))
             (case type
               :web-search (str (opt (.responseInclusion ^com.anthropic.models.messages.WebSearchTool20260318 stable-tool)))
               :web-fetch (str (opt (.responseInclusion ^WebFetchTool20260318 stable-tool)))))
          (str type " stable and beta response-inclusion differ")))))

(deftest stable-server-tool-input-examples
  (let [specs [{:type :bash :input-examples [{:command "pwd"}]}
               {:type :text-editor :input-examples [{:command "open"}]}
               {:type :memory :input-examples [{:command "view"}]}]
        stable-tools (mapv ->tool specs)
        beta-tools (mapv beta->tool specs)]
    (is (= 3 (count (filter some?
                             [(opt (.inputExamples (opt (.bash20250124 ^ToolUnion (nth stable-tools 0)))))
                              (opt (.inputExamples (opt (.textEditor20250728 ^ToolUnion (nth stable-tools 1)))))
                              (opt (.inputExamples (opt (.memoryTool20250818 ^ToolUnion (nth stable-tools 2)))))]))))
    (is (= (mapv count
                 [(opt (.inputExamples (opt (.bash20250124 ^BetaToolUnion (nth beta-tools 0)))))
                  (opt (.inputExamples (opt (.textEditor20250728 ^BetaToolUnion (nth beta-tools 1)))))
                  (opt (.inputExamples (opt (.memoryTool20250818 ^BetaToolUnion (nth beta-tools 2)))))] )
           [1 1 1]))))

(deftest stable-versioned-tool-input-examples
  (doseq [version [:20250124 :20250429]]
    (is (case version
          :20250124 (.isPresent
                     (.inputExamples
                      (opt (.textEditor20250124 ^ToolUnion
                                                (->tool {:type :text-editor
                                                         :version version
                                                         :input-examples [{:command "open"}]})))))
          :20250429 (.isPresent
                     (.inputExamples
                      (opt (.textEditor20250429 ^ToolUnion
                                                (->tool {:type :text-editor
                                                         :version version
                                                         :input-examples [{:command "open"}]}))))))
         (str "stable text-editor version " version))))

(deftest stable-beta-response-inclusion-errors-match
  (doseq [type [:web-search :web-fetch]]
    (let [stable (opt (case type
                        :web-search (.webSearchTool20260318 ^ToolUnion
                                                          (->tool {:type type
                                                                   :response-inclusion :invalid}))
                        :web-fetch (.webFetchTool20260318 ^ToolUnion
                                                         (->tool {:type type
                                                                  :response-inclusion :invalid}))))
          beta-tool (opt (case type
                           :web-search (.webSearchTool20260318 ^BetaToolUnion
                                                               (beta->tool {:type type
                                                                            :response-inclusion :invalid}))
                           :web-fetch (.webFetchTool20260318 ^BetaToolUnion
                                                              (beta->tool {:type type
                                                                           :response-inclusion :invalid}))))]
      (is (= (case type
               :web-search (str (opt (.responseInclusion ^com.anthropic.models.messages.WebSearchTool20260318 stable)))
               :web-fetch (str (opt (.responseInclusion ^WebFetchTool20260318 stable))))
             (case type
               :web-search (str (opt (.responseInclusion ^BetaWebSearchTool20260318 beta-tool)))
               :web-fetch (str (opt (.responseInclusion ^BetaWebFetchTool20260318 beta-tool)))))))))

(deftest tool-choice-disable-parallel-tool-use
  (testing "disable-parallel-tool-use reaches all supported tool-choice variants"
    (doseq [[tc variant]
            [[{:type :auto :disable-parallel-tool-use true} :auto]
             [{:type :any :disable-parallel-tool-use true} :any]
             [{:name "get_weather" :disable-parallel-tool-use true} :tool]]]
      (let [^ToolChoice choice (->tool-choice tc)]
        (is (= true (case variant
                      :auto (and (instance? ToolChoice choice) (.isAuto ^ToolChoice choice))
                      :any (and (instance? ToolChoice choice) (.isAny ^ToolChoice choice))
                      :tool (and (instance? ToolChoice choice) (.isTool ^ToolChoice choice)))))
        (is (= true (and (instance? ToolChoice choice)
                         (opt (.disableParallelToolUse ^ToolChoice choice))))))))
  (testing "existing keyword and named tool forms remain supported"
    (is (.isAuto ^ToolChoice (->tool-choice :auto)))
    (is (.isAny ^ToolChoice (->tool-choice :any)))
    (is (.isNone ^ToolChoice (->tool-choice :none)))
    (let [^ToolChoice choice (->tool-choice {:type :tool :name "get_weather"})]
      (is (.isTool choice))
      (is (= "get_weather" (.name (.asTool choice)))))
    (let [error (try
                  (->tool-choice {:type :none :disable-parallel-tool-use true})
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      ;; The beta path uses the same error keyword. A caller does not branch on
      ;; the Messages API that produced it.
      (is (= :unsupported-disable-parallel-tool-use
             (:anthropic/error (ex-data error)))))))

(deftest client-construction
  (doseq [opts [{:api-key "sk-test"}
                {:auth-token "token-test"}
                {:base-url "https://api.anthropic.com"}
                {:timeout-ms 1000}
                {:max-retries 1}]]
    (testing (str "client option " opts)
      (is (instance? AnthropicClient (a/client opts))))))

(deftest extended-client-construction
  (let [configured (atom nil)
        c (a/client {:api-key "sk-test"
                     :webhook-key "whsec_test"
                     :log-level :debug
                     :response-validation true
                     :proxy java.net.Proxy/NO_PROXY
                     :headers {"x-test" "header"}
                     :query-params {"test" "query"}
                     :configure #(reset! configured %)})]
    (is (instance? AnthropicClient c))
    (is (some? @configured))))

(deftest cloud-backend-client-construction
  (testing "optional Bedrock and Vertex constructors return the shared client interface"
    (doseq [[sym opts] [['bedrock-client {:region "us-east-1" :api-key "test"}]
                        ['vertex-client {:region "us-east5" :project "test"
                                         :access-token "test"}]]
            :let [f (resolved-fn sym)]]
      (is (fn? f))
      (when f
        (let [configured (atom false)
              c (f (assoc opts :configure (fn [_] (reset! configured true))))]
          (is (instance? AnthropicClient c))
          (is @configured))))))

(deftest vertex-client-base-url
  (let [base-url "https://vertex.example.test"
        configured (atom nil)
        c (a/vertex-client {:region "us-east5"
                            :project "test"
                            :access-token "test"
                            :base-url base-url
                            :configure (fn [builder]
                                         (reset! configured
                                                 (.baseUrl (.build builder))))})]
    (is (instance? AnthropicClient c))
    (is (= base-url @configured))))

(deftest per-call-request-options-api
  (is (some #{'[client req opts]} (:arglists (meta #'a/create-message))))
  (is (some #{'[client req opts]} (:arglists (meta #'a/count-tokens))))
  (is (fn? (resolved-fn '->request-options))))

(deftest newer-request-params
  (testing "create params include container, inference-geo, user-profile-id, cache-control"
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :container "container_123"
                                            :inference-geo "us"
                                            :user-profile-id "user_123"
                                            :cache-control true})]
      (is (= "container_123" (.asString (opt (.container p)))))
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

(deftest stable-request-parity-params
  (testing "system text blocks and tool cache control"
    (let [^MessageCreateParams p
          (->params {:system [{:text "stable" :cache-control {:ttl :1h}}]
                     :tools [{:name "cached"
                              :cache-control true
                              :input-schema {:type "object" :properties {}}}]
                     :messages [{:role :user :content "hi"}]})
          ^MessageCreateParams$System system (opt (.system p))
          ^TextBlockParam block (first (.asTextBlockParams system))
          ^com.anthropic.models.messages.ToolUnion tool-union (first (opt (.tools p)))
          ^Tool tool (.get (.tool tool-union))]
      (is (= "stable" (.text block)))
      (is (.isPresent (.cacheControl block)))
      (is (.isPresent (.cacheControl tool)))))
  (testing "system text blocks carry every text citation variant"
    (let [^MessageCreateParams p
          (->params {:system [{:text "cited"
                               :citations [{:type :char-location
                                            :cited-text "chars"
                                            :document-index 0
                                            :start-char-index 0
                                            :end-char-index 5}
                                           {:type :page-location
                                            :cited-text "pages"
                                            :document-index 0
                                            :start-page-number 1
                                            :end-page-number 2}
                                           {:type :content-block-location
                                            :cited-text "blocks"
                                            :document-index 0
                                            :start-block-index 0
                                            :end-block-index 1}
                                           {:type :web-search-result-location
                                            :cited-text "web"
                                            :url "https://example.com"
                                            :encrypted-index "encrypted"}
                                           {:type :search-result-location
                                            :cited-text "search"
                                            :source "source"
                                            :search-result-index 0
                                            :start-block-index 0
                                            :end-block-index 1}]}]
                     :messages [{:role :user :content "hi"}]})
          ^TextBlockParam block (first (.asTextBlockParams (opt (.system p))))
          citations (opt (.citations block))]
      (is (= [true true true true true]
             (mapv (fn [citation predicate]
                     (boolean (and citation (predicate citation))))
                   citations
                   [#(.isCharLocation ^com.anthropic.models.messages.TextCitationParam %)
                    #(.isPageLocation ^com.anthropic.models.messages.TextCitationParam %)
                    #(.isContentBlockLocation ^com.anthropic.models.messages.TextCitationParam %)
                    #(.isWebSearchResultLocation ^com.anthropic.models.messages.TextCitationParam %)
                    #(.isSearchResultLocation ^com.anthropic.models.messages.TextCitationParam %)])))))
  (testing "additional request properties reach create and count params"
    (let [req {:messages [{:role :user :content "hi"}]
               :extra-headers {"x-stable" "yes"}
               :extra-query {"preview" "true"}
               :extra-body {:future {:enabled true}}}
          ^MessageCreateParams create-p (->params req)
          ^MessageCountTokensParams count-p (->count-params req)]
      (doseq [[headers query body]
              [[(._additionalHeaders create-p) (._additionalQueryParams create-p)
                (._additionalBodyProperties create-p)]
               [(._additionalHeaders count-p) (._additionalQueryParams count-p)
                (._additionalBodyProperties count-p)]]]
        (is (= ["yes"] (vec (.values ^com.anthropic.core.http.Headers headers "x-stable"))))
        (is (= ["true"] (vec (.values ^com.anthropic.core.http.QueryParams query "preview"))))
        (is (= {"enabled" true}
               (.convert ^com.anthropic.core.JsonValue (get body "future") Object)))))))

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

(deftest versioned-server-tools
  (let [families
        [{:type :bash
          :versions {"20250124" #(.isPresent (.bash20250124 %))}}
         {:type :memory
          :versions {"20250818" #(.isPresent (.memoryTool20250818 %))}}
         {:type :code-execution
          :versions {"20250522" #(.isPresent (.codeExecutionTool20250522 %))
                     "20250825" #(.isPresent (.codeExecutionTool20250825 %))
                     "20260120" #(.isPresent (.codeExecutionTool20260120 %))
                     "20260521" #(.isPresent (.codeExecutionTool20260521 %))}}
         {:type :text-editor
          :versions {"20250124" #(.isPresent (.textEditor20250124 %))
                     "20250429" #(.isPresent (.textEditor20250429 %))
                     "20250728" #(.isPresent (.textEditor20250728 %))}}
         {:type :web-search
          :versions {"20250305" #(.isPresent (.webSearchTool20250305 %))
                     "20260209" #(.isPresent (.webSearchTool20260209 %))
                     "20260318" #(.isPresent (.webSearchTool20260318 %))}}
         {:type :web-fetch
          :versions {"20250910" #(.isPresent (.webFetchTool20250910 %))
                     "20260209" #(.isPresent (.webFetchTool20260209 %))
                     "20260309" #(.isPresent (.webFetchTool20260309 %))
                     "20260318" #(.isPresent (.webFetchTool20260318 %))}}]]
    (doseq [{:keys [type versions]} families
            [version selected?] versions]
      (is (selected? (->tool {:type type :version version}))
          (str type " version " version)))
    (doseq [{:keys [type versions]} families]
      (let [latest (last (sort (keys versions)))]
        (is ((get versions latest) (->tool {:type type}))
            (str type " defaults to " latest))))
    (let [tool (->tool {:type :text-editor
                        :version :20250429
                        :allowed-callers [:direct]
                        :cache-control true})]
      (is (= ["direct"] (mapv str
                               (opt (.allowedCallers (.get (.textEditor20250429 tool))))))
          "non-default options reach the selected variant")))
    (is (.isPresent
         (.searchToolBm25_20251119
          (->tool {:type :tool-search :variant :bm25}))))
  (try
    (->tool {:type :bash :version :19990101})
    (is false "unknown server-tool versions must throw")
    (catch clojure.lang.ExceptionInfo e
      (is (= :unsupported-server-tool-version (:anthropic/error (ex-data e))))
      (is (= :bash (:family (ex-data e))))
      (is (= :19990101 (:version (ex-data e)))))))

(deftest versioned-count-tools
  (let [cases [{:type :code-execution :version :20250825
                :selected? #(.isCodeExecutionTool20250825 %)}
               {:type :text-editor :version :20250429
                :selected? #(.isToolTextEditor20250429 %)}
               {:type :web-search :version :20260209
                :selected? #(.isWebSearchTool20260209 %)}
               {:type :web-fetch :version :20260309
                :selected? #(.isWebFetchTool20260309 %)}]]
    (doseq [{:keys [type version selected?]} cases]
      (is (selected? (->count-tool {:type type :version version}))))
    (is (.isCodeExecutionTool20260521
         (->count-tool {:type :code-execution})))
    (is (.isToolTextEditor20250728
         (->count-tool {:type :text-editor})))
    (is (.isWebSearchTool20260318
         (->count-tool {:type :web-search})))
    (is (.isWebFetchTool20260318
         (->count-tool {:type :web-fetch})))
    (try
      (->count-tool {:type :web-fetch :version "19990101"})
      (is false "unknown count-tool versions must throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :unsupported-server-tool-version
               (:anthropic/error (ex-data e))))))))

(deftest tool-search-server-tools
  (testing "tool-search bm25 and regex specs map to the right ToolUnion variants"
    (let [bm25 (->tool {:type :tool-search :variant :bm25
                        :allowed-callers [:direct]
                        :defer-loading true
                        :strict true
                        :cache-control true})
          regex (->tool {:type :tool-search :variant :regex})]
      (is (.isPresent (.searchToolBm25_20251119 bm25)))
      (is (.isPresent (.cacheControl (.get (.searchToolBm25_20251119 bm25)))))
      (is (= true (opt (.deferLoading (.get (.searchToolBm25_20251119 bm25))))))
      (is (= true (opt (.strict (.get (.searchToolBm25_20251119 bm25))))))
      (is (= ["direct"] (mapv str (opt (.allowedCallers (.get (.searchToolBm25_20251119 bm25)))))))
      (is (.isPresent (.searchToolRegex20251119 regex))))))

(deftest complete-stable-tool-configuration
  (testing "custom tools preserve all stable configuration"
    (let [tool (.get (.tool
                      (->tool {:name "configured"
                               :description "all options"
                               :input-schema {:type "object" :properties {}}
                               :eager-input-streaming true
                               :input-examples [{:location "Paris"}]
                               :allowed-callers [:direct]
                               :cache-control true
                               :defer-loading true
                               :strict true})))]
      (is (= ["direct"] (mapv str (opt (.allowedCallers tool)))))
      (is (.isPresent (.cacheControl tool)))
      (is (= true (opt (.deferLoading tool))))
      (is (= true (opt (.strict tool))))
      (is (= true (opt (.eagerInputStreaming tool))))
      (is (= [{"location" "Paris"}]
             (mapv input-example->map (opt (.inputExamples tool)))))))
  (testing "the same custom tool settings reach stable and beta tools"
    (let [spec {:name "weather"
                :description "Current weather"
                :input-schema {:type "object" :properties {}}
                :eager-input-streaming true
                :input-examples [{:location "Paris"}]
                :allowed-callers [:direct]
                :defer-loading true
                :strict true}
          stable (.get (.tool ^ToolUnion (->tool spec)))
          beta-tool (.asBetaTool ^BetaToolUnion (beta->tool spec))]
      (is (= (opt (.eagerInputStreaming stable))
             (opt (.eagerInputStreaming beta-tool))))
      (is (= (mapv input-example->map (opt (.inputExamples stable)))
             (mapv beta-input-example->map (opt (.inputExamples beta-tool)))))))
  (testing "every stable server tool preserves common configuration"
    (doseq [tool [(->tool {:type :web-search :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})
                  (->tool {:type :web-fetch :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})
                  (->tool {:type :code-execution :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})
                  (->tool {:type :bash :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})
                  (->tool {:type :text-editor :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})
                  (->tool {:type :memory :allowed-callers [:direct]
                           :cache-control true :defer-loading true :strict true})]
            :let [configured (or (opt (.webSearchTool20260318 tool))
                                 (opt (.webFetchTool20260318 tool))
                                 (opt (.codeExecutionTool20260521 tool))
                                 (opt (.bash20250124 tool))
                                 (opt (.textEditor20250728 tool))
                                 (opt (.memoryTool20250818 tool)))]]
      (is (= ["direct"] (mapv str (opt (.allowedCallers configured)))))
      (is (.isPresent (.cacheControl configured)))
      (is (= true (opt (.deferLoading configured))))
      (is (= true (opt (.strict configured)))))))

(deftest stable-and-beta-custom-tool-schemas-match
  (doseq [spec [{:name "no-schema"}
                {:name "empty-schema" :input-schema {}}
                {:name "typed" :input-schema {:type "array"}}
                {:name "with-properties"
                 :input-schema {:properties {:city {:type "string"}}}}
                {:name "with-properties-and-required"
                 :input-schema {:properties {:city {:type "string"}}
                                :required ["city"]}}]]
    (let [stable (.get (.tool ^ToolUnion (->tool spec)))
          beta-tool (.asBetaTool ^BetaToolUnion (beta->tool spec))]
      (is (= (stable-custom-tool-schema-shape stable)
             (beta-custom-tool-schema-shape beta-tool))
          (str "stable and beta differ for " spec)))))

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
      (is (.isPresent (.webSearchTool20260318 (second tools))))))
  (testing "tool-search specs are available to count-tokens too"
    (let [bm25 (->count-tool {:type :tool-search :variant :bm25})
          regex (->count-tool {:type :tool-search :variant :regex})]
      (is (.isPresent (.toolSearchToolBm25_20251119 bm25)))
      (is (.isPresent (.toolSearchToolRegex20251119 regex))))))

(deftest count-token-custom-tool-options
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :tools [{:name "weather"
                                  :input-schema {:type "object"}
                                  :eager-input-streaming true
                                  :input-examples [{:location "Paris"}]}]})
        tool (.asTool ^MessageCountTokensTool (first (opt (.tools p))))]
    (is (= true (opt (.eagerInputStreaming tool))))
    (is (= [{"location" "Paris"}]
           (mapv input-example->map (opt (.inputExamples tool)))))))

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
      (is (= "plain text" (.asString content)))))
  (testing "tool-result can mark tool execution errors"
    (let [cb (->content-block {:type :tool-result
                               :tool-use-id "toolu_1"
                               :content "boom"
                               :is-error true})
          tr ^ToolResultBlockParam (.get (.toolResult cb))]
      (is (= true (opt (.isError tr))))))
  (testing "search-result block carries source title content citations and cache-control"
    (let [cb (->content-block {:type :search-result
                               :source "https://example.com/a"
                               :title "Example"
                               :citations true
                               :cache-control true
                               :content [{:type :text
                                          :text "found text"
                                          :citations [{:type :char-location
                                                       :cited-text "found"
                                                       :document-index 0
                                                       :start-char-index 0
                                                       :end-char-index 5}]}]})
          sr (.get (.searchResult cb))]
      (is (= "https://example.com/a" (.source sr)))
      (is (= "Example" (.title sr)))
      (is (= "found text" (.text (first (.content sr)))))
      (is (= 1 (count (opt (.citations (first (.content sr)))))))
      (is (= true (opt (.enabled (.get (.citations sr))))))
      (is (.isPresent (.cacheControl sr)))))
  (testing "thinking block carries thinking and signature"
    (let [th (.get (.thinking (->content-block {:type :thinking
                                                :thinking "internal trace"
                                                :signature "sig_123"})))]
      (is (= "internal trace" (.thinking th)))
      (is (= "sig_123" (.signature th)))))
  (testing "redacted-thinking block carries data"
    (let [rt (.get (.redactedThinking (->content-block {:type :redacted-thinking
                                                        :data "encrypted"})))]
      (is (= "encrypted" (.data rt)))))
  (testing "container-upload block carries file id and cache-control"
    (let [cu (.get (.containerUpload (->content-block {:type :container-upload
                                                       :file-id "file_123"
                                                       :cache-control true})))]
      (is (= "file_123" (.fileId cu)))
      (is (.isPresent (.cacheControl cu))))))

(deftest stable-2-56-message-content-additions
  (let [image (.get (.image (->content-block
                             {:type :image
                              :source {:type :file :file-id "file_1"}
                              :transformations {:oversized-image :downsize}})))
        image-source (.source image)
        document (.get (.document (->content-block
                                   {:type :document
                                    :source {:type :file :file-id "file_2"}})))
        document-source (.source document)
        ^MessageCreateParams params
        (->params {:messages [{:role :user :content "hi"}]
                   :container {:id "c1"
                               :skills [{:skill-id "s1"
                                         :type :custom
                                         :version "1.0.0"}]}})
        container (.asContainerParams (opt (.container params)))
        skill (first (opt (.skills container)))]
    (is (.isFile image-source))
    (is (= "file_1" (.fileId (.asFile image-source))))
    (is (= :downsize
           (-> (opt (.oversizedImage (opt (.transformations image))))
               str str/lower-case keyword)))
    (is (.isFile document-source))
    (is (= "file_2" (.fileId (.asFile document-source))))
    (is (= "c1" (opt (.id container))))
    (is (= 1 (count (opt (.skills container)))))
    (is (= "s1" (.skillId skill)))
    (is (= :custom (-> (.type skill) str str/lower-case keyword)))
    (is (= "1.0.0" (opt (.version skill))))))

(deftest tool-use-caller-mapping
  (testing "direct caller"
    (let [block (-> (ToolUseBlock/builder)
                    (.id "toolu_direct")
                    (.name "direct")
                    (.input (JsonValue/from {}))
                    (.caller (ToolUseBlock$Caller/ofDirect
                              (.build (DirectCaller/builder))))
                    (.build))]
      (is (= {:type :tool-use :id "toolu_direct" :name "direct" :input {}
              :caller {:type :direct}}
             (#'a/block->map (com.anthropic.models.messages.ContentBlock/ofToolUse block))))))
  (testing "20250825 code execution caller"
    (let [block (-> (ToolUseBlock/builder)
                    (.id "toolu_50825")
                    (.name "code")
                    (.input (JsonValue/from {}))
                    (.caller (ToolUseBlock$Caller/ofCodeExecution20250825
                              (-> (ServerToolCaller/builder)
                                  (.toolId "tool_50825")
                                  (.build))))
                    (.build))]
      (is (= {:type :tool-use :id "toolu_50825" :name "code" :input {}
              :caller {:type :code-execution-20250825 :tool-id "tool_50825"}}
             (#'a/block->map (com.anthropic.models.messages.ContentBlock/ofToolUse block))))))
  (testing "20260120 code execution caller"
    (let [block (-> (ToolUseBlock/builder)
                    (.id "toolu_60120")
                    (.name "code")
                    (.input (JsonValue/from {}))
                    (.caller (ToolUseBlock$Caller/ofCodeExecution20260120
                              (-> (ServerToolCaller20260120/builder)
                                  (.toolId "tool_60120")
                                  (.build))))
                    (.build))]
      (is (= {:type :tool-use :id "toolu_60120" :name "code" :input {}
              :caller {:type :code-execution-20260120 :tool-id "tool_60120"}}
             (#'a/block->map (com.anthropic.models.messages.ContentBlock/ofToolUse block))))))
  (testing "unrecognized caller value throws anthropic error"
    (let [error (try
                  (when-let [f (ns-resolve 'anthropic.core 'caller->map)]
                    (f nil))
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= :unsupported-caller
             (some-> error ex-data :anthropic/error))))))

(deftest lossless-stable-content-blocks
  (testing "document sources include content strings, content blocks, and file ids"
    (doseq [source [{:type :content :content "inline document"}
                    {:type :content
                     :content [{:type :text :text "page one"}
                               {:type :image
                                :source {:type :url :url "https://example.com/page.png"}}]}
                    {:type :file :file-id "file_123"}]]
      (let [document (.get (.document
                            (->content-block {:type :document
                                              :source source
                                              :citations true})))]
        (is (some? document))
        (is (= true (opt (.enabled (opt (.citations document)))))))))
  (testing "text citations survive assistant-turn replay"
    (let [text (.get (.text
                      (->content-block
                       {:type :text
                        :text "cited"
                        :citations [{:type :char-location
                                     :cited-text "cited"
                                     :document-index 0
                                     :start-char-index 0
                                     :end-char-index 5}]})))]
      (is (= 1 (count (opt (.citations text)))))))
  (testing "server tool use and result maps replay as SDK input variants"
    (let [use (->content-block {:type :server-tool-use
                                :id "srv_1"
                                :name "web_search"
                                :input {:query "clojure"}
                                :caller {:type :direct}})
          result (->content-block {:type :web-search-result
                                   :tool-use-id "srv_1"
                                   :content {:type :web-search-tool-result-error
                                             :error-code :unavailable}
                                   :caller {:type :direct}})]
      (is (.isPresent (.serverToolUse use)))
      (is (.isPresent (.webSearchToolResult result)))))
  (testing "server tool response blocks emit replayable normalized data"
    (let [caller (.build (DirectCaller/builder))
          use (com.anthropic.models.messages.ContentBlock/ofServerToolUse
               (-> (com.anthropic.models.messages.ServerToolUseBlock/builder)
                   (.id "srv_1")
                   (.name (com.anthropic.models.messages.ServerToolUseBlock$Name/of "web_search"))
                   (.input (com.anthropic.core.JsonValue/from {"query" "clojure"}))
                   (.caller caller)
                   (.build)))
          result (com.anthropic.models.messages.ContentBlock/ofWebSearchToolResult
                  (-> (com.anthropic.models.messages.WebSearchToolResultBlock/builder)
                      (.toolUseId "srv_1")
                      (.caller caller)
                      (.content (-> (com.anthropic.models.messages.WebSearchToolResultError/builder)
                                    (.errorCode com.anthropic.models.messages.WebSearchToolResultErrorCode/UNAVAILABLE)
                                    (.build)))
                      (.build)))
          use-map (#'a/block->map use)
          result-map (#'a/block->map result)]
      (is (= {:type :direct} (:caller use-map)))
      (is (= {:query "clojure"} (:input use-map)))
      (is (= "srv_1" (:tool-use-id result-map)))
      (is (= :web-search-tool-result-error (get-in result-map [:content :type])))
      (is (.isPresent (.serverToolUse (->content-block use-map))))
      (is (.isPresent (.webSearchToolResult (->content-block result-map)))))))

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

(deftest structured-output-class-params
  (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                          :output-type String
                                          :effort :low})
        oc (opt (.outputConfig p))
        format (when oc (opt (.format oc)))]
    (is (some? oc))
    (is (= "low" (str (opt (.effort oc)))))
    (is (= "string" (.convert ^JsonValue (get (._additionalProperties (.schema format)) "type") Object)))))

(s/def ::answer string?)
(s/def ::spec-output (s/keys :req-un [::answer]))

(deftest structured-output-schema-conversion
  (testing "raw JSON Schema maps remain unchanged"
    (let [schema {:type "object" :properties {:answer {:type "string"}}}
          result (->structured-schema schema)]
      (is (= {:schema schema :validator nil} result))))
  (testing "a clojure.spec keyword becomes a JSON Schema and validator"
    (let [{:keys [schema validator]} (->structured-schema ::spec-output)]
      (is (= "object" (:type schema)))
      (is (= ["answer"] (:required schema)))
      (is (= "string" (get-in schema [:properties "answer" :type])))
      (is (true? (validator {:answer "ok"})))
      (is (false? (validator {:answer 3}))))))

(deftest structured-output-spec-params
  (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                          :response-format ::spec-output})
        format (opt (.format (opt (.outputConfig p))))]
    (is (= "object" (.convert ^JsonValue (get (._additionalProperties (.schema format)) "type") Object)))
    (is (= "[answer]" (str (get (._additionalProperties (.schema format)) "required"))))))

(deftest structured-output-spec-output-type
  (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                          :output-type ::spec-output})
        format (opt (.format (opt (.outputConfig p))))]
    (is (= "object" (.convert ^JsonValue (get (._additionalProperties (.schema format)) "type") Object)))
    (is (= {:answer "ok"}
           (:parsed (parsed-response {:content [{:type :text :text "{\"answer\":\"ok\"}"}]}
                                     {:output-type ::spec-output}
                                     {}))))
    (is (= :response-validation-failed
           (try
             (parsed-response {:content [{:type :text :text "{\"answer\":3}"}]}
                              {:output-type ::spec-output}
                              {:response-validation true})
             nil
             (catch clojure.lang.ExceptionInfo e
               (:anthropic/error (ex-data e))))))))

(deftest structured-output-unsupported-output-type
  (let [error (try
                (->params {:messages [{:role :user :content "hi"}]
                           :output-type '(s/coll-of string?)})
                nil
                (catch clojure.lang.ExceptionInfo e e))]
    (is (= :unsupported-schema (:anthropic/error (ex-data error))))))

(deftest structured-output-malli-conversion
  (if (try
        (requiring-resolve 'malli.json-schema/transform)
        (catch Throwable _ nil))
    (let [{:keys [schema validator]} (->structured-schema [:map [:answer string?]])
          ^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :output-type [:map [:answer string?]]})
          format (opt (.format (opt (.outputConfig p))))]
      (is (= "object" (:type schema)))
      (is (= "string" (get-in schema [:properties :answer :type])))
      (is (true? (validator {:answer "ok"})))
      (is (false? (validator {:answer 3})))
      (is (= "object" (.convert ^JsonValue (get (._additionalProperties (.schema format)) "type") Object)))
      (is (= {:answer "ok"}
             (:parsed (parsed-response {:content [{:type :text :text "{\"answer\":\"ok\"}"}]}
                                       {:output-type [:map [:answer string?]]}
                                       {}))))
      (is (= :response-validation-failed
             (try
               (parsed-response {:content [{:type :text :text "{\"answer\":3}"}]}
                                {:output-type [:map [:answer string?]]}
                                {:response-validation true})
               nil
               (catch clojure.lang.ExceptionInfo e
                 (:anthropic/error (ex-data e)))))))
    (is true "Malli is optional and is not loaded in the base test alias")))

(deftest structured-output-validation
  (testing "valid data is returned"
    (is (= {:answer "ok"}
           (validate-parsed! {:answer "ok"} ::spec-output))))
  (testing "invalid data raises the structured error map"
    (let [error (try
                  (validate-parsed! {:answer 3} ::spec-output)
                  nil
                  (catch clojure.lang.ExceptionInfo e e))]
      (is (= :response-validation-failed (:anthropic/error (ex-data error))))
      (is (= {:answer 3} (:value (ex-data error)))))))

(deftest structured-output-decoded-response-validation
  (let [resp {:content [{:type :text :text "{\"answer\":3}"}]}]
    (is (= :response-validation-failed
           (try
             (parsed-response resp {:response-format ::spec-output}
                              {:response-validation true})
             nil
             (catch clojure.lang.ExceptionInfo e
               (:anthropic/error (ex-data e))))))))

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

(deftest batch-request-fidelity
  (let [^BatchCreateParams$Request r
        (->batch-request
         {:custom-id "complete"
          :params {:model "claude-sonnet-4-6"
                   :max-tokens 128
                   :system [{:text "cached system" :cache-control true}]
                   :messages [{:role :user :content "hi"}]
                   :cache-control true
                   :container "container_123"
                   :inference-geo "us"
                   :service-tier :standard-only}})
        ^BatchCreateParams$Request$Params p (.params r)
        system (opt (.system p))]
    (is (= "complete" (.customId r)))
    (is (= "cached system" (.text (first (.asTextBlockParams system)))))
    (is (.isPresent (.cacheControl (first (.asTextBlockParams system)))))
    (is (.isPresent (.cacheControl p)))
    (is (= "container_123" (.asString (opt (.container p)))))
    (is (= "us" (opt (.inferenceGeo p))))
    (is (= "standard_only" (str (opt (.serviceTier p)))))))

(deftest batch-request-preserves-stream
  (let [^BatchCreateParams$Request$Params p
        (.params (->batch-request
                  {:custom-id "streaming"
                   :params {:model "claude-sonnet-4-6"
                            :max-tokens 128
                            :messages [{:role :user :content "hi"}]
                            :stream true}}))]
    (is (= true (opt (.stream p))))))

(deftest batch-list-params-preserve-options
  (let [build (resolved-fn '->batch-list-params)
        p (when build (build {:after-id "after" :before-id "before" :limit 17}))]
    (is (some? build))
    (when p
      (is (= "after" (opt (.afterId p))))
      (is (= "before" (opt (.beforeId p))))
      (is (= 17 (long (opt (.limit p))))))))

(deftest file-list-params-preserve-options
  (let [build (resolved-fn '->file-list-params)
        p (when build
            (build {:ids ["file_1" "file_2"]
                    :page "next-page"
                    :limit 19}))]
    (is (some? build))
    (when p
      (is (= ["file_1" "file_2"] (opt (.ids p))))
      (is (= "next-page" (opt (.page p))))
      (is (= 19 (long (opt (.limit p))))))))

(deftest batch-result-stream-reduction
  (testing "reduces batch results without retaining the full result collection and closes the stream"
    (let [closed? (atom false)
          seen? (atom false)
          mk (fn [id]
               (-> (MessageBatchIndividualResponse/builder)
                   (.customId id)
                   (.result (.build (MessageBatchCanceledResult/builder)))
                   (.build)))
          sr (reify StreamResponse
               (stream [_]
                 (.stream (java.util.ArrayList. [(mk "r1")])))
               (close [_] (reset! closed? true)))]
      (is (= ["r1"]
             (reduce-batch-result-stream
              sr
              (fn [acc m]
                (reset! seen? true)
                (conj acc (:custom-id m)))
              [])))
      (is @seen?)
      (is @closed?))))

(deftest usage-cache-tokens
  (testing "cache tokens are included when present"
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
                              (.skills [(-> (ContainerSkill/builder) (.skillId "skill_1") (.type ContainerSkill$Type/CUSTOM) (.version "1.0.0") (.build))])
                              (.build)))
              (.stopReason StopReason/STOP_SEQUENCE)
              (.stopSequence "END")
              (.stopDetails (-> (RefusalStopDetails/builder)
                                (.category RefusalStopDetails$Category/CYBER)
                                (.explanation "blocked")
                                (.build)))
              (.build))
        mm (message->map m)]
    (is (= {:id "container_123" :expires-at "2026-01-01T00:00Z"
            :skills [{:skill-id "skill_1" :type :custom :version "1.0.0"}]}
           (:container mm)))
    (is (= "END" (:stop-sequence mm)))
    (is (= {:category :cyber :explanation "blocked"} (:stop-details mm)))))

(deftest model-context-window-exceeded-stop-reason-round-trips
  (let [m (-> (Message/builder)
              (.id "msg_1")
              (.model "claude-haiku-4-5")
              (.role (com.anthropic.core.JsonValue/from "assistant"))
              (.type (com.anthropic.core.JsonValue/from "message"))
              (.content [])
              (.usage (usage 1 2 nil nil))
              (.stopDetails (-> (RefusalStopDetails/builder)
                                (.category RefusalStopDetails$Category/CYBER)
                                (.explanation "blocked")
                                (.build)))
              (.stopReason StopReason/MODEL_CONTEXT_WINDOW_EXCEEDED)
              (.stopSequence "END")
              (.build))]
    (is (= :model-context-window-exceeded
           (:stop-reason (message->map m))))))

(deftest thinking-block-round-trips-with-signature
  ;; Extended-thinking responses must return to :messages unchanged. The API
  ;; requires the signature on replayed thinking blocks. Input translation
  ;; throws an NPE without it.
  (let [blk (com.anthropic.models.messages.ContentBlock/ofThinking
             (-> (com.anthropic.models.messages.ThinkingBlock/builder)
                 (.thinking "let me reason")
                 (.signature "sig_abc")
                 (.build)))
        m (#'a/block->map blk)]
    (is (= {:type :thinking :thinking "let me reason" :signature "sig_abc"} m))
    (is (some? (#'a/->params {:model "m"
                              :messages [{:role :assistant :content [m]}]})))))

(deftest redacted-thinking-block-round-trips-with-data
  (let [blk (com.anthropic.models.messages.ContentBlock/ofRedactedThinking
             (-> (com.anthropic.models.messages.RedactedThinkingBlock/builder)
                 (.data "encrypted")
                 (.build)))
        m (#'a/block->map blk)]
    (is (= {:type :redacted-thinking :data "encrypted"} m))))

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
               (.downloadable true)
               (.expiresAt (java.time.OffsetDateTime/parse "2026-02-01T00:00:00Z"))
               (.build))
        m (file->map fm)]
    (is (= "file_1" (:id m)))
    (is (= "a.txt" (:filename m)))
    (is (= "text/plain" (:mime-type m)))
    (is (= 5 (:size-bytes m)))
    (is (= true (:downloadable m)))
    (is (= "2026-02-01T00:00Z" (:expires-at m)))
    (is (str/starts-with? (:created-at m) "2026-01-01"))))

(deftest delete-response-mapping
  (let [file (-> (DeletedFile/builder)
                 (.id "file_1")
                 (.type (DeletedFile$Type/of "file_deleted"))
                 (.build))
        batch (-> (DeletedMessageBatch/builder)
                  (.id "msgbatch_1")
                  (.type (com.anthropic.core.JsonValue/from "message_batch_deleted"))
                  (.build))]
    (is (= {:id "file_1" :deleted true :type :file-deleted}
           (deleted-file->map file)))
    (is (= {:id "msgbatch_1" :deleted true :type :message-batch-deleted}
           (deleted-batch->map batch)))))

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

(deftest model-capabilities-and-list-options
  (testing "all stable capabilities are returned as nested Clojure data"
    (let [caps-json (json/write-value-as-string
                     {:batch {:supported true}
                      :citations {:supported true}
                      :code_execution {:supported false}
                      :context_management {:supported true
                                           :compact_20260112 {:supported true}}
                      :effort {:supported true
                               :low {:supported true} :medium {:supported true}
                               :high {:supported true} :max {:supported false}}
                      :image_input {:supported true}
                      :pdf_input {:supported true}
                      :structured_outputs {:supported true}
                      :thinking {:supported true
                                 :types {:adaptive {:supported true}
                                         :enabled {:supported true}}}})
          caps (.readValue (com.anthropic.core.JsonValue/access$getJSON_MAPPER$cp)
                           caps-json
                           com.anthropic.models.models.ModelCapabilities)
          info (-> (ModelInfo/builder)
                   (.id "claude-capable")
                   (.displayName "Claude Capable")
                   (.createdAt (java.time.OffsetDateTime/parse "2026-01-01T00:00:00Z"))
                   (.type (com.anthropic.core.JsonValue/from "model"))
                   (.capabilities caps)
                   (.maxInputTokens empty-opt)
                   (.maxTokens empty-opt)
                   (.build))
          c (:capabilities (model->map info))]
      (is (= true (get-in c [:batch :supported])))
      (is (= false (get-in c [:code-execution :supported])))
      (is (= true (get-in c [:context-management :compact-20260112 :supported])))
      (is (= false (get-in c [:effort :max :supported])))
      (is (= true (get-in c [:thinking :types :adaptive :supported])))
      (is (= #{:batch :citations :code-execution :context-management :effort
               :image-input :pdf-input :structured-outputs :thinking}
             (set (keys c))))))
  (testing "list-models accepts the stable pagination options"
    (let [p (->model-list-params {:limit 25 :before-id "before" :after-id "after"
                                  :betas ["beta-one" :beta-two]})]
      (is (= 25 (opt (.limit p))))
      (is (= "before" (opt (.beforeId p))))
      (is (= "after" (opt (.afterId p))))
      (is (= ["beta-one" "beta-two"] (mapv str (opt (.betas p)))))
      (is (some #{'[client opts]} (:arglists (meta #'a/list-models)))))))

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
              :block {:type :tool-use :id "tu_1" :name "get_weather"
                      :input {} :caller {:type :direct}}}
             (event->map ev)))))
  (testing "content-block-stop and message-stop"
    (is (= {:type :content-block-stop :index 0}
           (event->map (RawMessageStreamEvent/ofContentBlockStop
                        (-> (RawContentBlockStopEvent/builder) (.index 0) (.build))))))
    (is (= {:type :message-stop}
           (event->map (RawMessageStreamEvent/ofMessageStop
                        (.build (RawMessageStopEvent/builder))))))))

(deftest full-stream-event-payloads
  (testing "message start includes the normalized message"
    (let [message (-> (Message/builder)
                      (.id "msg_stream")
                      (.model "claude-haiku-4-5")
                      (.role (com.anthropic.core.JsonValue/from "assistant"))
                      (.type (com.anthropic.core.JsonValue/from "message"))
                      (.content [])
                      (.usage (usage 3 0 nil nil))
                      (.container empty-opt)
                      (.stopDetails empty-opt)
                      (.stopReason empty-opt)
                      (.stopSequence empty-opt)
                      (.build))
          event (RawMessageStreamEvent/ofMessageStart
                 (-> (com.anthropic.models.messages.RawMessageStartEvent/builder)
                     (.message message)
                     (.build)))]
      (is (= "msg_stream" (get-in (event->map event) [:message :id])))
      (is (= {:input-tokens 3 :output-tokens 0}
             (get-in (event->map event) [:message :usage])))))
  (testing "message delta includes usage and all stop metadata"
    (let [delta (-> (com.anthropic.models.messages.RawMessageDeltaEvent$Delta/builder)
                    (.container (-> (Container/builder)
                                    (.id "container_stream")
                                    (.expiresAt (java.time.OffsetDateTime/parse "2026-02-01T00:00:00Z"))
                                    (.skills [])
                                    (.build)))
                    (.stopReason StopReason/STOP_SEQUENCE)
                    (.stopSequence "DONE")
                    (.stopDetails (-> (RefusalStopDetails/builder)
                                      (.category RefusalStopDetails$Category/CYBER)
                                      (.explanation "blocked")
                                      (.build)))
                    (.build))
          usage (-> (com.anthropic.models.messages.MessageDeltaUsage/builder)
                    (.inputTokens 2)
                    (.outputTokens 7)
                    (.cacheCreationInputTokens empty-opt)
                    (.cacheReadInputTokens 1)
                    (.outputTokensDetails empty-opt)
                    (.serverToolUse empty-opt)
                    (.build))
          event (RawMessageStreamEvent/ofMessageDelta
                 (-> (com.anthropic.models.messages.RawMessageDeltaEvent/builder)
                     (.delta delta)
                     (.usage usage)
                     (.build)))
          m (event->map event)]
      (is (= :stop-sequence (:stop-reason m)))
      (is (= "DONE" (:stop-sequence m)))
      (is (= "container_stream" (get-in m [:container :id])))
      (is (= {:category :cyber :explanation "blocked"} (:stop-details m)))
      (is (= {:input-tokens 2 :output-tokens 7 :cache-read-input-tokens 1}
             (:usage m)))))
  (testing "signature and citation deltas include their payload"
    (let [signature (delta-event
                     (RawContentBlockDelta/ofSignature
                      (-> (com.anthropic.models.messages.SignatureDelta/builder)
                          (.signature "sig_part")
                          (.build))))
          citation (delta-event
                    (RawContentBlockDelta/ofCitations
                     (-> (com.anthropic.models.messages.CitationsDelta/builder)
                         (.citation (-> (com.anthropic.models.messages.CitationCharLocation/builder)
                                        (.citedText "quote")
                                        (.documentIndex 0)
                                        (.startCharIndex 1)
                                        (.endCharIndex 6)
                                        (.documentTitle empty-opt)
                                        (.fileId empty-opt)
                                        (.build)))
                         (.build))))]
      (is (= {:type :signature-delta :index 0 :signature "sig_part"}
             (event->map signature)))
      (is (= {:type :citations-delta :index 0
              :citation {:type :char-location :cited-text "quote"
                         :document-index 0 :start-char-index 1 :end-char-index 6}}
             (event->map citation)))))
  (testing "content-block starts retain text and thinking payloads"
    (let [text (-> (com.anthropic.models.messages.TextBlock/builder)
                   (.text "hello") (.citations []) (.build))
          thinking (-> (com.anthropic.models.messages.ThinkingBlock/builder)
                       (.thinking "reason") (.signature "sig") (.build))
          start (fn [block]
                  (event->map
                   (RawMessageStreamEvent/ofContentBlockStart
                    (-> (RawContentBlockStartEvent/builder)
                        (.index 2) (.contentBlock block) (.build)))))]
      (is (= {:type :text :text "hello" :citations []}
             (:block (start (RawContentBlockStartEvent$ContentBlock/ofText text)))))
      (is (= {:type :thinking :thinking "reason" :signature "sig"}
             (:block (start (RawContentBlockStartEvent$ContentBlock/ofThinking thinking))))))))

(deftest stream-message-api
  (is (fn? (some-> (ns-resolve 'anthropic.core 'stream-message) deref))))

;; Live round-trip - only runs when ANTHROPIC_API_KEY is set (network + billed).
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
      ;; Batches run asynchronously. Cancel the in-flight batch instead of
      ;; waiting for results.
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
        ;; user uploads. This test fixture cannot test it.
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

(defn- base-tool [name f]
  {:name name
   :description (str "tool " name)
   :input-schema {:type "object" :properties {}}
   :fn f})

(defn- text-response [id text]
  {:id id
   :role :assistant
   :stop-reason :end-turn
   :content [{:type :text :text text}]
   :usage {:input-tokens 1 :output-tokens 1}})

(defn- tool-response [& blocks]
  {:id "msg_tool"
   :role :assistant
   :stop-reason :tool-use
   :content (vec blocks)
   :usage {:input-tokens 1 :output-tokens 1}})

(deftest run-tools-loop
  (testing "single tool call continues with assistant and tool-result turns"
    (let [calls (atom [])
          params {:model "claude-haiku-4-5"
                  :messages [{:role :user :content "weather?"}]
                  :tools [(base-tool "get_weather"
                                     (fn [input]
                                       (is (= {:location "Denver"} input))
                                       "sunny"))]}
          responses [(tool-response {:type :tool-use
                                     :id "tu_1"
                                     :name "get_weather"
                                     :input {:location "Denver"}})
                     (text-response "msg_final" "done")]
          call-fn (fn [p]
                    (swap! calls conj p)
                    (doseq [t (:tools p)]
                      (is (not (contains? t :fn))))
                    (when (= 2 (count @calls))
                      (is (= [{:role :user :content "weather?"}
                              {:role :assistant :content (:content (first responses))}
                              {:role :user
                               :content [{:type :tool-result
                                          :tool-use-id "tu_1"
                                          :content "sunny"}]}]
                             (:messages p))))
                    (nth responses (dec (count @calls))))
          result (run-tools* call-fn params {})]
      (is (= "msg_final" (:id result)))
      (is (= [{:role :user :content "weather?"}
              {:role :assistant :content (:content (first responses))}
              {:role :user
               :content [{:type :tool-result
                          :tool-use-id "tu_1"
                          :content "sunny"}]}
              {:role :assistant :content (:content (second responses))}]
             (:messages result)))))
  (testing "parallel tool calls produce one user turn with ordered results"
    (let [calls (atom [])
          params {:messages "run both"
                  :tools [(base-tool "a" (fn [input] (:x input)))
                          (base-tool "b" (fn [input] (inc (:y input))))]}
          first-response (tool-response {:type :tool-use :id "tu_a" :name "a" :input {:x "A"}}
                                        {:type :tool-use :id "tu_b" :name "b" :input {:y 1}})
          responses [first-response (text-response "msg_final" "done")]
          call-fn (fn [p]
                    (swap! calls conj p)
                    (when (= 2 (count @calls))
                      (is (= [{:type :tool-result :tool-use-id "tu_a" :content "A"}
                              {:type :tool-result :tool-use-id "tu_b" :content 2}]
                             (get-in p [:messages 2 :content]))))
                    (nth responses (dec (count @calls))))]
      (is (= "msg_final" (:id (run-tools* call-fn params {}))))
      (is (= [{:role :user :content "run both"}]
             (:messages (first @calls))))))
  (testing "tool exceptions are sent back as error tool-results"
    (let [calls (atom [])
          params {:messages [{:role :user :content "fail"}]
                  :tools [(base-tool "explode"
                                     (fn [_]
                                       (throw (ex-info "bad tool" {}))))]}
          responses [(tool-response {:type :tool-use :id "tu_1" :name "explode" :input {}})
                     (text-response "msg_final" "recovered")]
          call-fn (fn [p]
                    (swap! calls conj p)
                    (when (= 2 (count @calls))
                      (is (= [{:type :tool-result
                               :tool-use-id "tu_1"
                               :content "bad tool"
                               :is-error true}]
                             (get-in p [:messages 2 :content]))))
                    (nth responses (dec (count @calls))))]
      (is (= "msg_final" (:id (run-tools* call-fn params {}))))))
  (testing "called tools must have a function"
    (let [params {:messages [{:role :user :content "weather?"}]
                  :tools [(dissoc (base-tool "get_weather" constantly) :fn)]}
          ex (try
               (run-tools* (constantly (tool-response {:type :tool-use
                                                       :id "tu_1"
                                                       :name "get_weather"
                                                       :input {}}))
                           params
                           {})
               (catch clojure.lang.ExceptionInfo e e))]
      (is (= :no-tool-fn (:anthropic/error (ex-data ex))))
      (is (= "get_weather" (:name (ex-data ex))))))
  (testing "max iterations limits create-message calls"
    (let [calls (atom 0)
          params {:messages [{:role :user :content "loop"}]
                  :tools [(base-tool "loop" (constantly "again"))]}
          call-fn (fn [_]
                    (swap! calls inc)
                    (tool-response {:type :tool-use :id (str "tu_" @calls) :name "loop" :input {}}))
          ex (try
               (run-tools* call-fn params {:max-iterations 2})
               (catch clojure.lang.ExceptionInfo e e))]
      (is (= 2 @calls))
      (is (= :max-iterations-exceeded (:anthropic/error (ex-data ex))))
      (is (= 2 (:iterations (ex-data ex))))
      (is (vector? (:messages (ex-data ex))))))
  (testing "non-string return values remain tool-result content values"
    (let [calls (atom [])
          params {:messages [{:role :user :content "data"}]
                  :tools [(base-tool "data" (constantly {:ok true}))]}
          responses [(tool-response {:type :tool-use :id "tu_1" :name "data" :input {}})
                     (text-response "msg_final" "done")]
          call-fn (fn [p]
                    (swap! calls conj p)
                    (when (= 2 (count @calls))
                      (is (= {:ok true}
                             (get-in p [:messages 2 :content 0 :content]))))
                    (nth responses (dec (count @calls))))]
      (run-tools* call-fn params {})))
  (testing "on-message sees every response in order"
    (let [seen (atom [])
          responses [(tool-response {:type :tool-use :id "tu_1" :name "x" :input {}})
                     (text-response "msg_final" "done")]
          params {:messages [{:role :user :content "x"}]
                  :tools [(base-tool "x" (constantly "ok"))]}]
      (run-tools* (fn [_] (nth responses (count @seen)))
                  params
                  {:on-message #(swap! seen conj %)})
      (is (= responses @seen)))))

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

(def throw-normalized! #'a/throw-normalized!)

(defn- rate-limit-ex ^com.anthropic.errors.RateLimitException []
  (let [ctor (first (.getConstructors com.anthropic.errors.RateLimitException))]
    (.newInstance ctor (object-array [(-> (com.anthropic.core.http.Headers/builder)
                                          (.put "request-id" "req_123")
                                          (.put "retry-after" "1")
                                          (.build))
                                      (com.anthropic.core.JsonValue/from
                                       {"type" "error"
                                        "error" {"type" "rate_limit_error"
                                                 "message" "slow down"}})
                                      nil nil]))))

(deftest api-error-normalization
  (testing "service exceptions become ex-info with status, error-type, and the original as cause"
    (let [orig (rate-limit-ex)
          ex (try (throw-normalized! orig) (catch clojure.lang.ExceptionInfo e e))]
      (is (= :api-error (:anthropic/error (ex-data ex))))
      (is (= 429 (:status (ex-data ex))))
      (is (= :rate-limit (:error-type (ex-data ex))))
      (is (= :rate-limit (:classification (ex-data ex))))
      (is (= true (:retryable (ex-data ex))))
      (is (= "req_123" (:request-id (ex-data ex))))
      (is (= ["1"] (get-in (ex-data ex) [:headers "retry-after"])))
      (is (= "slow down" (get-in (ex-data ex) [:body :error :message])))
      (is (= :rate-limit-error (:sdk-error-type (ex-data ex))))
      (is (identical? orig (ex-cause ex)))))
  (testing "bad requests and SSE failures have distinct classifications"
    (let [headers (.build (com.anthropic.core.http.Headers/builder))
          body (com.anthropic.core.JsonValue/from
                {"type" "error" "error" {"type" "invalid_request_error"}})
          bad (-> (com.anthropic.errors.BadRequestException/builder)
                  (.headers headers) (.body body) (.build))
          sse (-> (com.anthropic.errors.SseException/builder)
                  (.statusCode 502) (.headers headers) (.body body) (.build))
          normalize #(try (throw-normalized! %) (catch clojure.lang.ExceptionInfo e e))]
      (is (= :invalid-request (:classification (ex-data (normalize bad)))))
      (is (= false (:retryable (ex-data (normalize bad)))))
      (is (= :stream-error (:classification (ex-data (normalize sse)))))
      (is (= true (:retryable (ex-data (normalize sse)))))))
  (testing "io exceptions become :io-error ex-info"
    (let [orig (com.anthropic.errors.AnthropicIoException. "boom")
          ex (try (throw-normalized! orig) (catch clojure.lang.ExceptionInfo e e))]
      (is (= :io-error (:anthropic/error (ex-data ex))))
      (is (= :retryable (:classification (ex-data ex))))
      (is (= true (:retryable (ex-data ex))))
      (is (identical? orig (ex-cause ex)))))
  (testing "other Anthropic exceptions pass through unchanged"
    (let [orig (com.anthropic.errors.AnthropicInvalidDataException. "bad" nil)
          ex (try (throw-normalized! orig) (catch Throwable e e))]
      (is (identical? orig ex)))))
