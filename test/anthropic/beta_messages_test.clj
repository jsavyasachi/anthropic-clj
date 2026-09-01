(ns anthropic.beta-messages-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [anthropic.beta.messages :as messages]
            [anthropic.core])
  (:import (com.anthropic.core JsonValue RequestOptions)
           (com.anthropic.core.http StreamResponse)
           (com.anthropic.helpers BetaToolRunner)
           (com.anthropic.models.beta.messages BetaMessage BetaTextBlock BetaUsage BetaTool
                                               BetaMessageTokensCount BetaCacheCreation BetaContainer MessageCountTokensParams
                                               MessageCreateParams BetaRawContentBlockDeltaEvent
                                               BetaRawMessageStreamEvent BetaStopReason BetaToolUnion
                                               MessageCountTokensParams$Tool)
           (com.anthropic.models.messages Tool ToolUnion)
           (com.anthropic.services.blocking.beta MessageService)
           (java.time OffsetDateTime)
           (com.anthropic.models.beta.messages.batches BatchCreateParams
                                                       BetaDeletedMessageBatch
                                                       BetaMessageBatch
                                                       BetaMessageBatchIndividualResponse
                                                       BetaMessageBatchCanceledResult)))

(def ->params #'messages/->params)
(def ->count-params #'messages/->count-params)
(def beta-message->map #'messages/beta-message->map)
(def beta-message-param->map #'messages/beta-message-param->map)

(deftest beta-thinking-block-binding
  (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                          :thinking {:type :enabled :budget-tokens 1024
                                                     :block-binding {:prefix-mismatch-behavior :drop-block}}})
        thinking (.get (.thinking p))
        enabled (.asEnabled thinking)]
    (is (= "DROP_BLOCK"
           (.asString (.get (.prefixMismatchBehavior (.get (.blockBinding enabled))))))))
  (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                          :thinking {:type :adaptive
                                                     :block-binding {:prefix-mismatch-behavior :error}}})
        thinking (.get (.thinking p))]
    (is (= "ERROR"
           (.asString (.get (.prefixMismatchBehavior (.get (.blockBinding (.asAdaptive thinking))))))))
  ))

(deftest beta-message-param-new-fields
  (let [p (-> (com.anthropic.models.beta.messages.BetaMessageParam/builder)
              (.content "hello")
              (.role (com.anthropic.models.beta.messages.BetaMessageParam$Role/of "user"))
              (.clearAt (com.anthropic.models.beta.messages.BetaMessageParam$ClearAt/of "next_user_message"))
              (.outputConfig (-> (com.anthropic.models.beta.messages.BetaSystemMessageOutputConfig/builder)
                                 (.effort (com.anthropic.models.beta.messages.BetaSystemMessageOutputConfig$Effort/of "high"))
                                 (.build)))
              (.build))
        m (beta-message-param->map p)]
    (is (= :next-user-message (:clear-at m)))
    (is (= :high (get-in m [:output-config :effort])))))
  (let [p (-> (com.anthropic.models.beta.messages.BetaMessageParam/builder)
              (.content "hello")
              (.role (com.anthropic.models.beta.messages.BetaMessageParam$Role/of "user"))
              (.outputConfig (.build (com.anthropic.models.beta.messages.BetaSystemMessageOutputConfig/builder)))
              (.build))]
    (is (not (contains? (:output-config (beta-message-param->map p)) :effort))))
  (let [^MessageCreateParams params
        (->params {:messages [{:role :user :content "hello"
                               :clear-at :next-user-message
                               :output-config {:effort :high}}]})
        ^com.anthropic.models.beta.messages.BetaMessageParam message (first (.messages params))
        config (.get (.outputConfig message))
        effort (.get (.effort config))]
    (is (= "NEXT_USER_MESSAGE" (.asString (.get (.clearAt message)))))
    (is (= "HIGH" (.asString effort))))

(deftest beta-message-targeted-enum-conversion
  (let [keywordize-types #'messages/keywordize-types
        m (keywordize-types {:reason "legacy-reason"
                             :effort "legacy-effort"
                             :output-config {:effort "HIGH"}
                             :block-binding {:prefix-mismatch-behavior "DROP_BLOCK"}
                             :input-transformations [{:reason "PREFIX_BINDING_MISMATCH"}]})]
    (is (= "legacy-reason" (:reason m)))
    (is (= "legacy-effort" (:effort m)))
    (is (= "HIGH" (get-in m [:output-config :effort])))
    (is (= "DROP_BLOCK" (get-in m [:block-binding :prefix-mismatch-behavior])))
    (is (= "PREFIX_BINDING_MISMATCH" (get-in m [:input-transformations 0 :reason])))))
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
(def ->tool #'messages/->tool)
(def ->server-tool #'messages/->server-tool)
(def ->tool-choice #'messages/->tool-choice)
(def ->custom-tool #'messages/->custom-tool)
(def stable->custom-tool #'anthropic.core/->custom-tool)

(defn- ex-data-for [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))
(def stable->tool #'anthropic.core/->tool)

(defn- beta-test-message [id]
  (-> (BetaMessage/builder)
      (.id id)
      (.type (JsonValue/from "message"))
      (.role (JsonValue/from "assistant"))
      (.model "claude-sonnet-4-6")
      (.stopReason BetaStopReason/END_TURN)
      (.content [])
      (.contextManagement (java.util.Optional/empty))
      (.diagnostics (java.util.Optional/empty))
      (.stopDetails (java.util.Optional/empty))
      (.stopSequence (java.util.Optional/empty))
      (.container (-> (BetaContainer/builder)
                      (.id "container_1")
                      (.expiresAt (OffsetDateTime/parse "2030-01-01T00:00:00Z"))
                      (.skills [])
                      (.build)))
      (.usage (-> (BetaUsage/builder)
                  (.cacheCreation (-> (BetaCacheCreation/builder)
                                      (.ephemeral1hInputTokens 0)
                                      (.ephemeral5mInputTokens 0)
                                      (.build)))
                  (.cacheCreationInputTokens 0)
                  (.cacheReadInputTokens 0)
                  (.fallbackCredit (java.util.Optional/empty))
                  (.inferenceGeo (java.util.Optional/empty))
                  (.iterations (java.util.Optional/empty))
                  (.outputTokensDetails (java.util.Optional/empty))
                  (.serverToolUse (java.util.Optional/empty))
                  (.serviceTier (java.util.Optional/empty))
                  (.speed (java.util.Optional/empty))
                  (.inputTokens 0) (.outputTokens 0) (.build)))
      (.build)))

(defn- beta-test-runner
  ([messages streams]
   (beta-test-runner messages streams {:messages [{:role :user :content "hello"}]}))
  ([messages streams initial-params]
   (beta-test-runner messages streams initial-params (atom nil)))
  ([messages streams initial-params on-create]
   (let [message-index (atom -1)
        stream-index (atom -1)
        service (proxy [MessageService] []
                  (withRawResponse [] nil)
                  (withOptions [_] nil)
                  (batches [] nil)
                  (create [params _opts]
                    (let [index (swap! message-index inc)]
                      (when-let [f @on-create] (f index params))
                      (nth messages index)))
                  (createStreaming [_ _]
                    (nth streams (swap! stream-index inc)))
                  (countTokens [_ _] nil)
                  (toolRunner [_ _] nil))]
    (BetaToolRunner. service
                     (let [p (->params initial-params)]
                       (-> (com.anthropic.models.beta.messages.ToolRunnerCreateParams/builder)
                           (.initialMessageParams p)
                           (.build)))
                     (RequestOptions/none)))))

(defn- beta-test-stream-response [events closed?]
  (reify StreamResponse
    (stream [_] (.stream (java.util.ArrayList. events)))
    (close [_] (reset! closed? true))))

(deftest beta-tool-runner-handle-iterates-messages
  (let [runner (beta-test-runner [(beta-test-message "msg_1")] [])
        handle (messages/beta-tool-runner-handle runner)]
    (is (= ["msg_1"] (mapv :id ((:messages handle)))))
    (is (fn? (:set-next-params! handle)))))

(deftest beta-tool-runner-handle-streams-lazily
  (let [closed? (atom false)
        stop (-> (com.anthropic.models.beta.messages.BetaRawMessageStopEvent/builder)
                 (.type (JsonValue/from "message_stop"))
                 (.build))
        events [(BetaRawMessageStreamEvent/ofMessageStart (beta-test-message "stream_msg"))
                (BetaRawMessageStreamEvent/ofMessageStop stop)]
        runner (beta-test-runner [] [(beta-test-stream-response events closed?)])
        handle (messages/beta-tool-runner-handle runner)]
    (let [events ((:streaming handle))
          first-event (first events)]
      (is (= :message-start (:type first-event)))
      (is (= "stream_msg" (get-in first-event [:message :id]))))
    (is @closed?)))

(deftest beta-tool-runner-handle-sets-next-params-and-reads-last-tool-response
  (let [on-create (atom nil)
        seen-params (atom [])
        runner (beta-test-runner [(beta-test-message "msg_1") (beta-test-message "msg_2")] []
                                 {:messages [{:role :user :content "hello"}]}
                                 on-create)
        handle (messages/beta-tool-runner-handle runner)
        next-params {:messages [{:role :user :content "next"}] :max-tokens 42}]
    (reset! on-create (fn [index params]
                        (swap! seen-params conj [index (.maxTokens ^MessageCreateParams params)])
                        (when (zero? index)
                          ((:set-next-params! handle) next-params))))
    (is (nil? ((:last-tool-response handle))))
    (let [messages ((:messages handle))]
      (is (= "msg_1" (:id (first messages))))
      (is (= 2 (count (doall messages))))
      (is (= [[0 1024] [1 42]] @seen-params)))
    (is (nil? ((:last-tool-response handle))))))

(deftest beta-tool-runner-handle-translates-positive-last-tool-response
  (let [runner (beta-test-runner [] []
                                 {:messages [{:role :assistant
                                              :content [{:type :tool-use
                                                         :id "toolu_1"
                                                         :name "weather"
                                                         :input {:city "Paris"}}]}]})
        handle (messages/beta-tool-runner-handle runner)
        response ((:last-tool-response handle))]
    (is (= :user (:role response)))
    (is (= :tool-result (get-in response [:content 0 :type])))
    (is (= "toolu_1" (get-in response [:content 0 :tool-use-id])))))

(def server-tool-versions
  [{:type :bash
    :versions [["20241022" #(.isBash20241022 ^BetaToolUnion %)]
               ["20250124" #(.isBash20250124 ^BetaToolUnion %)]]}
   {:type :code-execution
    :versions [["20250522" #(.isCodeExecutionTool20250522 ^BetaToolUnion %)]
               ["20250825" #(.isCodeExecutionTool20250825 ^BetaToolUnion %)]
               ["20260120" #(.isCodeExecutionTool20260120 ^BetaToolUnion %)]
               ["20260521" #(.isCodeExecutionTool20260521 ^BetaToolUnion %)]]}
   {:type :computer-use
    :versions [["20241022" #(.isComputerUse20241022 ^BetaToolUnion %)]
               ["20250124" #(.isComputerUse20250124 ^BetaToolUnion %)]
               ["20251124" #(.isComputerUse20251124 ^BetaToolUnion %)]]}
   {:type :text-editor
    :versions [["20241022" #(.isTextEditor20241022 ^BetaToolUnion %)]
               ["20250124" #(.isTextEditor20250124 ^BetaToolUnion %)]
               ["20250429" #(.isTextEditor20250429 ^BetaToolUnion %)]
               ["20250728" #(.isTextEditor20250728 ^BetaToolUnion %)]]}
   {:type :web-search
    :versions [["20250305" #(.isWebSearchTool20250305 ^BetaToolUnion %)]
               ["20260209" #(.isWebSearchTool20260209 ^BetaToolUnion %)]
               ["20260318" #(.isWebSearchTool20260318 ^BetaToolUnion %)]]}
   {:type :web-fetch
    :versions [["20250910" #(.isWebFetchTool20250910 ^BetaToolUnion %)]
               ["20260209" #(.isWebFetchTool20260209 ^BetaToolUnion %)]
               ["20260309" #(.isWebFetchTool20260309 ^BetaToolUnion %)]
               ["20260318" #(.isWebFetchTool20260318 ^BetaToolUnion %)]]}])

(def server-tool-defaults
  [{:type :web-search :predicate #(.isWebSearchTool20260318 ^BetaToolUnion %)}
   {:type :web-fetch :predicate #(.isWebFetchTool20260318 ^BetaToolUnion %)}
   {:type :code-execution :predicate #(.isCodeExecutionTool20260521 ^BetaToolUnion %)}
   {:type :bash :predicate #(.isBash20250124 ^BetaToolUnion %)}
   {:type :text-editor :predicate #(.isTextEditor20250728 ^BetaToolUnion %)}
   {:type :memory :predicate #(.isMemoryTool20250818 ^BetaToolUnion %)}
   {:type :computer-use :predicate #(.isComputerUse20251124 ^BetaToolUnion %)}
   {:type :advisor :predicate #(.isAdvisorTool20260301 ^BetaToolUnion %)}])

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

(defn- stable-custom-tool-shape [^Tool tool]
  {:name (.name tool)
   :description (opt (.description tool))
   :input-schema (let [schema (json-roundtrip (.inputSchema tool))]
                   (if (nil? (:properties schema))
                     (dissoc schema :properties)
                     schema))})

(defn- beta-custom-tool-shape [^BetaTool tool]
  {:name (.name tool)
   :description (opt (.description tool))
   :input-schema (let [schema (json-roundtrip (.inputSchema tool))]
                   (if (nil? (:properties schema))
                     (dissoc schema :properties)
                     schema))})

(deftest beta-server-tool-versions
  (doseq [{:keys [type versions]} server-tool-versions
          [version predicate] versions]
    (is (predicate (->tool (merge {:type type :version (if (= version "20241022") :20241022 version)
                                   :name (name type)}
                                  (when (= type :computer-use)
                                    {:display-height-px 900 :display-width-px 1400})
                                  (when (= type :advisor)
                                    {:model "claude-sonnet-4-6"}))))
        (str type " version " version))))

(deftest beta-server-tool-default-versions
  (doseq [{:keys [type predicate]} server-tool-defaults]
    (is (predicate (->tool (merge {:type type :name (name type)}
                                  (when (= type :computer-use)
                                    {:display-height-px 900 :display-width-px 1400})
                                  (when (= type :advisor)
                                    {:model "claude-sonnet-4-6"}))))
        (str type))))

(deftest beta-server-tool-version-options-and-errors
  (let [tool (.asBash20241022 ^BetaToolUnion
              (->tool {:type :bash :version "20241022" :name "bash"
                       :input-examples [{:command "pwd"}] :strict true}))]
    (is (= "pwd" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples tool)))) "command"))))
    (is (= true (opt (.strict tool)))))
  (let [error (try
                (->tool {:type :bash :version "20990101" :name "bash"})
                nil
                (catch clojure.lang.ExceptionInfo e e))]
    (is (= :unsupported-server-tool-version (:anthropic/error (ex-data error))))
    (is (= :bash (:type (ex-data error))))
    (is (= "20990101" (:version (ex-data error))))))

(deftest beta-count-tool-versions
  (doseq [{:keys [type versions]} server-tool-versions
          [version _] versions]
    (let [tool (first (opt (.tools (->count-params {:messages [{:role :user :content "hi"}]
                                                     :tools [(merge {:type type :version version
                                                                     :name (name type)}
                                                                    (when (= type :computer-use)
                                                                      {:display-height-px 900 :display-width-px 1400}))]}))))]
      (is (instance? MessageCountTokensParams$Tool tool))
      (is (case type
            :bash (case version
                    "20241022" (.isBetaToolBash20241022 ^MessageCountTokensParams$Tool tool)
                    "20250124" (.isBetaToolBash20250124 ^MessageCountTokensParams$Tool tool))
            :code-execution (case version
                              "20250522" (.isBetaCodeExecutionTool20250522 ^MessageCountTokensParams$Tool tool)
                              "20250825" (.isBetaCodeExecutionTool20250825 ^MessageCountTokensParams$Tool tool)
                              "20260120" (.isBetaCodeExecutionTool20260120 ^MessageCountTokensParams$Tool tool)
                              "20260521" (.isBetaCodeExecutionTool20260521 ^MessageCountTokensParams$Tool tool))
            :computer-use (case version
                            "20241022" (.isBetaToolComputerUse20241022 ^MessageCountTokensParams$Tool tool)
                            "20250124" (.isBetaToolComputerUse20250124 ^MessageCountTokensParams$Tool tool)
                            "20251124" (.isBetaToolComputerUse20251124 ^MessageCountTokensParams$Tool tool))
            :text-editor (case version
                           "20241022" (.isBetaToolTextEditor20241022 ^MessageCountTokensParams$Tool tool)
                           "20250124" (.isBetaToolTextEditor20250124 ^MessageCountTokensParams$Tool tool)
                           "20250429" (.isBetaToolTextEditor20250429 ^MessageCountTokensParams$Tool tool)
                           "20250728" (.isBetaToolTextEditor20250728 ^MessageCountTokensParams$Tool tool))
            :web-search (case version
                          "20250305" (.isBetaWebSearchTool20250305 ^MessageCountTokensParams$Tool tool)
                          "20260209" (.isBetaWebSearchTool20260209 ^MessageCountTokensParams$Tool tool)
                          "20260318" (.isBetaWebSearchTool20260318 ^MessageCountTokensParams$Tool tool))
            :web-fetch (case version
                        "20250910" (.isBetaWebFetchTool20250910 ^MessageCountTokensParams$Tool tool)
                        "20260209" (.isBetaWebFetchTool20260209 ^MessageCountTokensParams$Tool tool)
                        "20260309" (.isBetaWebFetchTool20260309 ^MessageCountTokensParams$Tool tool)
                        "20260318" (.isBetaWebFetchTool20260318 ^MessageCountTokensParams$Tool tool)))))))

(deftest beta-output-config-task-budget
  (let [^MessageCreateParams params
        (->params {:messages [{:role :user :content "hi"}]
                   :task-budget {:total 4096 :remaining 1024}})
        config (opt (.outputConfig params))]
    (is (= 4096 (.total (opt (.taskBudget config)))))
    (is (= 1024 (opt (.remaining (opt (.taskBudget config))))))))

(deftest beta-server-tool-unions
  (doseq [[tool predicate]
          [[{:type :web-search :name "web-search"} #(.isWebSearchTool20260318 ^BetaToolUnion %)]
           [{:type :web-fetch :name "web-fetch"} #(.isWebFetchTool20260318 ^BetaToolUnion %)]
           [{:type :code-execution :name "code-execution"} #(.isCodeExecutionTool20260521 ^BetaToolUnion %)]
           [{:type :bash :name "bash"} #(.isBash20250124 ^BetaToolUnion %)]
           [{:type :text-editor :name "text-editor"} #(.isTextEditor20250728 ^BetaToolUnion %)]
           [{:type :memory :name "memory"} #(.isMemoryTool20250818 ^BetaToolUnion %)]
           [{:type :tool-search :variant :bm25 :name "tool-search"} #(.isSearchToolBm25_20251119 ^BetaToolUnion %)]
           [{:type :tool-search :variant :regex :name "tool-search"} #(.isSearchToolRegex20251119 ^BetaToolUnion %)]
           [{:type :computer-use :name "computer-use" :display-height-px 900 :display-width-px 1400}
            #(.isComputerUse20251124 ^BetaToolUnion %)]
           [{:type :advisor :name "advisor" :model "claude-sonnet-4-6"}
            #(.isAdvisorTool20260301 ^BetaToolUnion %)]
           [{:type :mcp-toolset :name "mcp-toolset" :mcp-server-name "weather"}
            #(.isMcpToolset ^BetaToolUnion %)]]]
    (is (and (instance? BetaToolUnion (->tool tool))
             (predicate (->tool tool))))))

(deftest beta-browser-and-computer-toolsets
  (let [browser-union (->tool {:type :browser-toolset :name "browser"
                               :cache-control true
                               :configs {:navigate {:enabled true :defer-loading false}
                                         :screenshot {:enabled true}}})
        computer-union (->tool {:type :computer-toolset :name "computer"
                                :cache-control true
                                :configs {:screenshot {:enabled true :defer-loading false}
                                          :cursor-position {:enabled true}}})]
    (is (.isBrowserToolset20260801 ^BetaToolUnion browser-union))
    (is (.isComputerToolset20260801 ^BetaToolUnion computer-union))
    (when (.isBrowserToolset20260801 ^BetaToolUnion browser-union)
      (let [browser (.asBrowserToolset20260801 ^BetaToolUnion browser-union)
            configs (opt (.configs browser))]
        (is (some? (opt (.cacheControl browser))))
        (is (= true (opt (.enabled (opt (.navigate configs))))))
        (is (= false (opt (.deferLoading (opt (.navigate configs))))))
        (is (= true (opt (.enabled (opt (.screenshot configs))))))))
    (when (.isComputerToolset20260801 ^BetaToolUnion computer-union)
      (let [computer (.asComputerToolset20260801 ^BetaToolUnion computer-union)
            configs (opt (.configs computer))]
        (is (some? (opt (.cacheControl computer))))
        (is (= true (opt (.enabled (opt (.screenshot configs))))))
        (is (= false (opt (.deferLoading (opt (.screenshot configs))))))
        (is (= true (opt (.enabled (opt (.cursorPosition configs))))))))
    (is (.isBrowserToolset20260801 ^BetaToolUnion
                                   (->tool {:type :browser-toolset :name "browser"})))
    (is (.isComputerToolset20260801 ^BetaToolUnion
                                    (->tool {:type :computer-toolset :name "computer"})))
    (is (.isBrowserToolset20260801 ^BetaToolUnion
                                   (first (opt (.tools (->params {:messages [{:role :user :content "hi"}]
                                                                 :tools [{:type :browser-toolset :name "browser"}]}))))))
    (is (.isComputerToolset20260801 ^BetaToolUnion
                                    (first (opt (.tools (->params {:messages [{:role :user :content "hi"}]
                                                                  :tools [{:type :computer-toolset :name "computer"}]}))))))
    (is (.isBetaBrowserToolset20260801 ^MessageCountTokensParams$Tool
                                       (first (opt (.tools (->count-params {:messages [{:role :user :content "hi"}]
                                                                         :tools [{:type :browser-toolset :name "browser"}]}))))))
    (is (.isBetaComputerToolset20260801 ^MessageCountTokensParams$Tool
                                        (first (opt (.tools (->count-params {:messages [{:role :user :content "hi"}]
                                                                          :tools [{:type :computer-toolset :name "computer"}]}))))))))

(deftest beta-custom-tool-options
  (let [tool (->tool {:name "weather"
                      :input-schema {:type "object"}
                      :eager-input-streaming true
                      :input-examples [{:location "Paris"}]
                      :defer-loading true
                      :strict true
                      :allowed-callers [:direct]})]
    (is (and (instance? BetaToolUnion tool) (.isBetaTool ^BetaToolUnion tool)))
    (is (= true (and (instance? BetaToolUnion tool)
                     (.. ^BetaToolUnion tool asBetaTool deferLoading get))))
    (is (= true (and (instance? BetaToolUnion tool)
                     (.. ^BetaToolUnion tool asBetaTool strict get))))
    (is (= true (.. ^BetaToolUnion tool asBetaTool eagerInputStreaming get)))
    (is (= "Paris" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples (.asBetaTool ^BetaToolUnion tool)))) ) "location"))))
    (is (= "direct" (and (instance? BetaToolUnion tool)
                          (str (first (opt (.allowedCallers (.asBetaTool ^BetaToolUnion tool))))))))))

(deftest beta-server-tool-options
  (testing "empty allow-lists are rejected for beta and dated web tools"
    (doseq [tool [{:type :web-search :name "web-search" :allowed-domains []}
                  {:type :web-fetch :name "web-fetch" :allowed-domains []}
                  {:type :web-search :name "web-search" :version :20260209
                   :allowed-domains []}
                  {:type :web-fetch :name "web-fetch" :version :20260309
                   :allowed-domains []}]]
      (let [error (try (->tool tool) nil
                       (catch clojure.lang.ExceptionInfo e e))]
        (is (= :empty-allowed-domains (:anthropic/error (ex-data error)))
            (str "for " tool)))))
  (let [web-search (.asWebSearchTool20260318 ^BetaToolUnion
                                             (->tool {:type :web-search :name "web-search"
                                                      :max-uses 3
                                                      :allowed-domains ["example.com"]
                                                      :blocked-domains ["blocked.example"]
                                                      :response-inclusion :excluded
                                                      :user-location {:city "Paris" :country "FR"}}))
        web-fetch (.asWebFetchTool20260318 ^BetaToolUnion
                                           (->tool {:type :web-fetch :name "web-fetch"
                                                    :max-content-tokens 2048
                                                    :use-cache true
                                                    :response-inclusion :excluded
                                                    :citations {:enabled true}}))
        bash (->tool {:type :bash :name "bash" :input-examples [{:command "pwd"}]})
        text-editor (.asTextEditor20250728 ^BetaToolUnion
                                           (->tool {:type :text-editor :name "text-editor"
                                                    :max-characters 5000
                                                    :input-examples [{:path "/tmp/a"}]}))
        memory (->tool {:type :memory :name "memory" :input-examples [{:query "Paris"}]})
        computer-use (.asComputerUse20251124 ^BetaToolUnion
                                             (->tool {:type :computer-use :name "computer-use"
                                                      :display-height-px 900 :display-width-px 1400
                                                      :display-number 1 :enable-zoom true
                                                      :input-examples [{:action "screenshot"}]}))
        advisor (.asAdvisorTool20260301 ^BetaToolUnion
                                       (->tool {:type :advisor :name "advisor"
                                                :model "claude-sonnet-4-6" :max-tokens 256 :max-uses 2}))]
    (is (= 3 (opt (.maxUses web-search))))
    (is (= ["example.com"] (opt (.allowedDomains web-search))))
    (is (= ["blocked.example"] (opt (.blockedDomains web-search))))
    (is (= "excluded" (str (opt (.responseInclusion web-search)))))
    (is (= "Paris" (opt (.city (opt (.userLocation web-search))))))
    (is (= 2048 (opt (.maxContentTokens web-fetch))))
    (is (= true (opt (.useCache web-fetch))))
    (is (= "excluded" (str (opt (.responseInclusion web-fetch)))))
    (is (= true (.. web-fetch citations get enabled get)))
    (is (= 5000 (opt (.maxCharacters text-editor))))
    (is (= "pwd" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples (.asBash20250124 ^BetaToolUnion bash))))) "command"))))
    (is (= "/tmp/a" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples text-editor)))) "path"))))
    (is (= "Paris" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples (.asMemoryTool20250818 ^BetaToolUnion memory))))) "query"))))
    (is (= 900 (.displayHeightPx computer-use)))
    (is (= 1400 (.displayWidthPx computer-use)))
    (is (= 1 (opt (.displayNumber computer-use))))
    (is (= true (opt (.enableZoom computer-use))))
    (is (= "screenshot" (json-roundtrip (get (._additionalProperties (first (opt (.inputExamples computer-use)))) "action"))))
    (is (= "claude-sonnet-4-6" (str (.model advisor))))
    (is (= 256 (opt (.maxTokens advisor))))
    (is (= 2 (opt (.maxUses advisor))))))

(deftest beta-advisor-caching
  (let [advisor (.asAdvisorTool20260301 ^BetaToolUnion
                                       (->tool {:type :advisor :name "advisor"
                                                :model "claude-sonnet-4-6"
                                                :caching {:ttl :five-minutes}}))]
    (is (= "five-minutes" (str (opt (.ttl (opt (.caching advisor)))))))))

(deftest beta-server-tool-option-unions
  (is (.isCodeExecutionTool20260521 ^BetaToolUnion
       (->tool {:type :code-execution :name "code" :defer-loading true :strict true})))
  (is (.isBash20250124 ^BetaToolUnion
       (->tool {:type :bash :name "bash" :defer-loading true :strict true})))
  (is (.isMemoryTool20250818 ^BetaToolUnion
       (->tool {:type :memory :name "memory" :defer-loading true :strict true})))
  (is (.isSearchToolBm25_20251119 ^BetaToolUnion
       (->tool {:type :tool-search :variant :bm25 :name "search"})))
  (is (.isSearchToolRegex20251119 ^BetaToolUnion
       (->tool {:type :tool-search :variant :regex :name "search"})))
  (is (.isMcpToolset ^BetaToolUnion
       (->tool {:type :mcp-toolset :name "mcp" :mcp-server-name "weather"
                :default-config {:enabled true}}))))

(deftest beta-tool-input-shape-matches-stable-tool
  (let [tool {:type :web-search :name "web-search" :max-uses 3}
        stable (stable->tool tool)
        beta (->tool tool)]
    (is (.isWebSearchTool20260318 ^ToolUnion stable))
    (is (.isWebSearchTool20260318 ^BetaToolUnion beta))))

(deftest beta-tool-function-keeps-custom-union
  (let [tool (->tool {:type :web-search :name "local-search" :input-schema {}
                      :fn identity})]
    (is (.isBetaTool ^BetaToolUnion tool))))

(deftest beta-custom-tool-keeps-an-unrecognized-type
  ;; Only a `:type` in the server-tool set routes server-side. A custom tool may
  ;; carry any other `:type`, with or without an `:input-schema`, and must not be
  ;; mistaken for a server tool.
  (doseq [t [{:name "get_weather" :description "d" :type "custom"}
             {:name "get_weather" :description "d" :type :custom}
             {:name "get_weather" :description "d"}
             {:name "get_weather" :description "d" :type "custom"
              :input-schema {:type "object"}}]]
    (is (.isBetaTool ^BetaToolUnion (->tool t)) (str "routed wrong: " t))))

(deftest beta-custom-tool-input-shape-matches-stable-tool
  (doseq [spec [{:name "no-options"}
                {:name "no-description" :input-schema {:type "object"}}
                {:name "no-schema" :description "described"}
                {:name "no-schema-type" :input-schema {:properties {}}}
                {:name "complete" :description "described"
                 :input-schema {:type "object" :properties {}
                                :required ["city"]}}]]
    (let [stable (.get (.tool ^ToolUnion (stable->tool spec)))
          beta (.asBetaTool ^BetaToolUnion (->tool spec))]
      (is (= (stable-custom-tool-shape stable) (beta-custom-tool-shape beta))
          (str "stable and beta differ for " spec)))))

(deftest beta-count-tool-unions
  (let [server (try
                 (first (opt (.tools (->count-params {:messages [{:role :user :content "hi"}]
                                                       :tools [{:type :web-search :name "web-search"}]}))))
                 (catch Throwable _ nil))
        custom (try
                 (first (opt (.tools (->count-params {:messages [{:role :user :content "hi"}]
                                                       :tools [{:name "weather"
                                                                :input-schema {:type "object"}}]}))))
                 (catch Throwable _ nil))]
    (is (and (instance? MessageCountTokensParams$Tool server)
             (.isBetaWebSearchTool20260318 ^MessageCountTokensParams$Tool server)))
    (is (and (instance? MessageCountTokensParams$Tool custom)
             (.isBeta ^MessageCountTokensParams$Tool custom)))))

(deftest beta-unknown-server-tool
  ;; An unrecognized `:type` never reaches the server-tool dispatch: it is a custom
  ;; tool, matching the stable path. Reaching the dispatch directly with an unknown
  ;; type is the error case.
  (let [error (try
                (->server-tool {:type :unknown-server-tool :name "unknown"})
                nil
                (catch clojure.lang.ExceptionInfo e e))]
    (is (= :unsupported-server-tool (:anthropic/error (ex-data error)))))
  (is (.isBetaTool ^BetaToolUnion (->tool {:type :unknown-server-tool :name "unknown"}))))

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

(deftest beta-system-text-block-citations
  (doseq [[citations expected]
          [[{:type :char-location :cited-text "quote"
             :document-index 0 :start-char-index 0 :end-char-index 5}
            :char-location]
           [{:type :page-location :cited-text "quote"
             :document-index 0 :start-page-number 1 :end-page-number 2}
            :page-location]]]
    (let [^MessageCreateParams p
          (->params {:system [{:text "be precise" :citations [citations]}]
                     :messages [{:role :user :content "hi"}]})
          block (first (.asBetaTextBlockParams (opt (.system p))))]
      (is (= true
             (if (= expected :char-location)
               (.isCharLocation (first (opt (.citations block))))
               (.isPageLocation (first (opt (.citations block))))))
          (str "citations shape: " citations)))))

(deftest beta-content-block-citations
  (testing "text blocks carry citations"
    (is (= {:type "text" :text "cited"
            :citations [{:type "char_location" :cited-text "quote"
                         :document-index 0 :start-char-index 0 :end-char-index 5}]}
           (json-roundtrip
            (->content-block {:type :text :text "cited"
                              :citations [{:type :char-location :cited-text "quote"
                                           :document-index 0 :start-char-index 0
                                           :end-char-index 5}]})))))
  (testing "document blocks carry citations"
    (is (= {:type "document"
            :source {:type "text" :data "source" :media-type "text/plain"}
            :citations {:enabled true}}
           (json-roundtrip
            (->content-block {:type :document
                              :source {:type :text :data "source"}
                              :citations true}))))))

(deftest beta-message-context-management-request-translation
  (let [^MessageCreateParams p
        (->params {:messages [{:role :user :content "hi"}]
                   :context-management {:edits [{:type :clear-tool-uses-20250919
                                                 :clear-tool-inputs true}
                                                {:type :clear-thinking-20251015
                                                 :keep :all}
                                                {:type :compact-20260112
                                                 :instructions "summarize"}]}})
        context-management (opt (.contextManagement p))]
    (is (some? context-management))
    (when-let [edits (and context-management (opt (.edits context-management)))]
      (is (= 3 (count edits)))
      (when (= 3 (count edits))
        (is (.isClearToolUses20250919 (first edits)))
        (is (.isClearThinking20251015 (second edits)))
        (is (.isCompact20260112 (nth edits 2)))))))

(deftest beta-message-diagnostics-request-translation
  (let [^MessageCreateParams p
        (->params {:messages [{:role :user :content "hi"}]
                   :diagnostics {:previous-message-id "msg_previous"}})]
    (is (some? (opt (.diagnostics p))))
    (when-let [diagnostics (opt (.diagnostics p))]
      (is (= "msg_previous" (opt (.previousMessageId diagnostics)))))))

(deftest beta-message-speed-request-translation
  (doseq [[speed expected] [[:standard "standard"] [:fast "fast"]]]
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :speed speed})]
      (is (= expected (str (opt (.speed p)))))))
  (let [error (try
                (->params {:messages [{:role :user :content "hi"}]
                           :speed :turbo})
                nil
                (catch clojure.lang.ExceptionInfo e e))]
    (is (= :unsupported-speed (:anthropic/error (ex-data error))))))

(deftest beta-message-output-format-request-translation
  (let [^MessageCreateParams p
        (->params {:messages [{:role :user :content "hi"}]
                   :output-format {:type "object" :properties {}}})
        body (json-roundtrip (._body p))]
    (is (some? (opt (.outputFormat p))))
    (is (= {:type "object" :properties {}}
           (get-in body [:output-format :schema])))))

(deftest beta-message-structured-output-class-request-translation
  (let [^MessageCreateParams p
        (->params {:messages [{:role :user :content "hi"}]
                   :output-type String
                   :effort :high})
        config (opt (.outputConfig p))
        format (when config (opt (.format config)))
        schema (when format (._schema format))]
    (is (some? config))
    (is (= "high" (str (opt (.effort config)))))
    (is (= "string" (.convert ^JsonValue (get (.values schema) "type") Object)))))

(deftest beta-count-context-management-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :context-management {:edits [{:type :compact-20260112
                                                       :instructions "summarize"}]}})]
    (is (some? (opt (.contextManagement p))))
    (when-let [context-management (opt (.contextManagement p))]
      (is (= 1 (count (opt (.edits context-management))))))))

(deftest beta-count-mcp-servers-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :mcp-servers [{:name "docs" :url "https://example.test/mcp"}]})]
    (is (some? (opt (.mcpServers p))))
    (when-let [servers (opt (.mcpServers p))]
      (is (= "docs" (.name (first servers)))))))

(deftest beta-count-output-config-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :response-format {:type "object"}
                         :effort :high
                         :task-budget {:total 4096 :remaining 1024}})
        config (opt (.outputConfig p))]
    (is (some? config))
    (when config
      (is (= "high" (str (opt (.effort config)))))
      (is (= 4096 (.total (opt (.taskBudget config))))))))

(deftest beta-count-output-format-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :output-format {:type "object" :properties {}}})
        body (json-roundtrip (._body p))]
    (is (= {:type "object" :properties {}}
           (get-in body [:output-format :schema])))))

(deftest beta-count-speed-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :speed :fast})]
    (is (= "fast" (str (opt (.speed p)))))))

(deftest beta-count-user-profile-id-request-translation
  (let [^MessageCountTokensParams p
        (->count-params {:messages [{:role :user :content "hi"}]
                         :user-profile-id "user_123"})]
    (is (= "user_123" (opt (.userProfileId p))))))

(deftest beta-count-shared-request-map-parity
  (let [request {:model "claude-sonnet-4-6"
                 :system "be terse"
                 :messages [{:role :user :content "hi"}]
                 :tools [{:name "weather" :input-schema {:type "object"}}]
                 :thinking {:type :enabled :budget-tokens 2048}
                 :tool-choice :auto
                 :betas ["token-efficient-tools-2025-02-19"]
                 :cache-control true
                 :context-management {:edits [{:type :compact-20260112
                                               :instructions "summarize"}]}
                 :mcp-servers [{:name "docs" :url "https://example.test/mcp"}]
                 :response-format {:type "object"}
                 :effort :high
                 :task-budget {:total 4096}
                 :output-format {:type "object"}
                 :speed :fast
                 :user-profile-id "user_123"
                 :extra-headers {:x-trace "trace-1"}
                 :extra-query {:x-mode "test"}
                 :extra-body {:x-extra true}}]
    (let [^MessageCreateParams create (->params request)
          ^MessageCountTokensParams count (->count-params request)]
      (is (some? create))
      (is (some? count))
      (is (= #{"x-extra"} (set (keys (._additionalBodyProperties count)))))
      (is (contains? (.names (._headers count)) "x-trace"))
      (is (contains? (.keys (._queryParams count)) "x-mode"))
      (is (= {:context-management true
              :cache-control true
              :mcp-servers true
              :output-config true
              :output-format true
              :speed true
              :user-profile-id true}
             {:context-management (some? (opt (.contextManagement count)))
              :cache-control (some? (opt (.cacheControl count)))
              :mcp-servers (some? (opt (.mcpServers count)))
              :output-config (some? (opt (.outputConfig count)))
              :output-format (some? (opt (.outputFormat count)))
              :speed (some? (opt (.speed count)))
              :user-profile-id (some? (opt (.userProfileId count)))})))))

(deftest beta-message-tool-choice-disable-parallel-tool-use
  (doseq [[choice expected]
          [[{:type :auto :disable-parallel-tool-use true} true]
           [{:type :any :disable-parallel-tool-use false} false]
           [{:name "weather" :disable-parallel-tool-use true} true]]]
    (let [^MessageCreateParams p (->params {:messages [{:role :user :content "hi"}]
                                            :tool-choice choice})
          body (json-roundtrip (._body p))]
      (is (= expected (get-in body [:tool-choice :disable-parallel-tool-use])))))
  (doseq [choice [:auto :any :none]]
    (is (some? (->params {:messages [{:role :user :content "hi"}]
                          :tool-choice choice}))))
  (is (some? (->params {:messages [{:role :user :content "hi"}]
                        :tool-choice {:name "weather"}})))
  (let [error (try
                (->params {:messages [{:role :user :content "hi"}]
                           :tool-choice {:type :none :disable-parallel-tool-use true}})
                nil
                (catch clojure.lang.ExceptionInfo e e))]
    (is (= :unsupported-disable-parallel-tool-use
           (:anthropic/error (ex-data error))))))

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

(deftest beta-2-56-message-content-additions
  (let [image (.asImage (->content-block
                         {:type :image
                          :source {:type :file :file-id "file_1"}
                          :transformations {:oversized-image :downsize}}))
        image-source (.source image)
        document (.asDocument (->content-block
                               {:type :document
                                :source {:type :file :file-id "file_2"}}))
        document-source (.source document)
        ^MessageCreateParams params
        (->params {:messages [{:role :user :content "hi"}]
                   :container {:id "c1"
                               :skills [{:skill-id "s1"
                                         :type :custom
                                         :version "1.0.0"}]}})
        ^MessageCreateParams string-container-params
        (->params {:messages [{:role :user :content "hi"}]
                   :container "container_123"})
        container (.asBetaContainerParams (opt (.container params)))
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
    (is (= "1.0.0" (opt (.version skill))))
    (is (= "container_123" (-> string-container-params .container opt .asString)))))

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
                    (.inputTransformations
                     [(-> (com.anthropic.models.beta.messages.BetaThinkingDroppedInputTransformation/builder)
                          (.type (JsonValue/from "thinking_dropped_input_transformation"))
                          (.path "/messages/0")
                          (.reason (com.anthropic.models.beta.messages.BetaThinkingDroppedInputTransformation$Reason/of "prefix_binding_mismatch"))
                          (.build))])
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
    (is (= [{:type :thinking-dropped-input-transformation
             :path "/messages/0" :reason :prefix-binding-mismatch}]
           (:input-transformations result)))
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
    (is (= {:input-tokens 37
            :context-management {:original-input-tokens 37}}
           (beta-tokens-count->map tokens-count)))))

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

(deftest run-beta-tools-uses-messages-returned-by-on-turn
  (let [seen (atom [])
        calls (atom 0)
        params {:messages "start"
                :tools [{:name "weather" :input-schema {} :fn identity}]}
        call-fn (fn [request]
                  (swap! seen conj (:messages request))
                  (if (= 1 (swap! calls inc))
                    {:stop-reason :tool-use
                     :content [{:type :tool-use :id "id-weather"
                                :name "weather" :input {}}]}
                    {:stop-reason :end-turn :content []}))]
    (run-beta-tools* call-fn params
                     {:on-turn (fn [_ p]
                                 (assoc p :messages [{:role :user :content
                                                      [{:type :tool-addition
                                                        :tool {:reference "new-tool"}}]}]))})
    (is (= [{:role :user :content
            [{:type :tool-addition :tool {:reference "new-tool"}}]}]
           (second @seen)))))

(deftest beta-structured-output-parsing
  (is (= {:capital "Sacramento"}
         (parse-beta-text {:content [{:type :text
                                      :text "{\"capital\":\"Sacramento\"}"}]}))))

(deftest beta-tool-choice-none-map-form
  ;; {:type :none} must build the none variant, not silently return nil, and must
  ;; behave the same as the bare :none keyword and as the stable path.
  (doseq [tc [:none {:type :none}]]
    (let [c (->tool-choice tc)]
      (is (some? c) (str "nil tool choice for " tc))
      (is (.isNone ^com.anthropic.models.beta.messages.BetaToolChoice c) (str "wrong variant for " tc))))
  (is (= :unsupported-disable-parallel-tool-use
         (:anthropic/error
          (ex-data-for #(->tool-choice {:type :none :disable-parallel-tool-use true}))))))

(deftest beta-tool-search-rejects-an-unknown-variant
  ;; An unrecognized variant must raise the library's error rather than falling out
  ;; of `case` as a raw IllegalArgumentException.
  (doseq [t [{:type :tool-search :variant :bogus} {:type :tool-search}]]
    (is (= :unsupported-tool-search-variant
           (:anthropic/error (ex-data-for #(->tool t))))
        (str "for " t))))

(deftest beta-custom-tool-schema-properties-reach-the-sdk
  ;; A custom tool's own :name shadows clojure.core/name in the builder's scope, so
  ;; mapping schema property keys through it used to call a String as a function.
  ;; Any beta custom tool with schema properties failed at build time.
  (let [spec {:name "get_weather"
              :description "Look up weather"
              :input-schema {:type "object"
                             :properties {:city {:type "string"}
                                          :units {:type "string"}}
                             :required ["city"]}}
        beta-tool (->custom-tool spec)
        beta-props (-> beta-tool .inputSchema .properties opt ._additionalProperties keys set)]
    (is (= #{"city" "units"} beta-props))
    ;; The same spec must also build on the stable path, which is where this
    ;; mapping has always worked.
    (is (some? (stable->custom-tool spec)))))

(deftest beta-block-types-are-keywords-like-the-stable-path
  ;; A block's :type must read the same on both paths, so code dispatching on
  ;; (= :text (:type block)) keeps working when a request moves to the beta API.
  ;; A tool call's :input is caller-defined JSON and must be left untouched.
  (let [f #'messages/keywordize-types
        mapped (f {:type "message"
                   :content [{:type "text" :text "hi"}
                             {:type "tool_use" :id "tu_1" :name "get_weather"
                              :input {:type "object" :city "Paris"}}
                             {:type "tool_result"
                              :content [{:type "text" :text "sunny"}]}]})]
    (is (= :message (:type mapped)))
    (is (= [:text :tool-use :tool-result] (mapv :type (:content mapped))))
    (is (= :text (-> mapped :content (nth 2) :content first :type))
        "nested block types are converted too")
    (is (= {:type "object" :city "Paris"} (-> mapped :content second :input))
        "tool input is caller data and must not be keywordized")))
