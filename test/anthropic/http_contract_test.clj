(ns anthropic.http-contract-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as json]
            [anthropic.core :as a]
            [anthropic.beta.messages :as beta])
  (:import (com.sun.net.httpserver HttpExchange HttpHandler HttpServer)
           (java.net InetSocketAddress)
           (java.nio.charset StandardCharsets)))

(def json-mapper (json/object-mapper {:decode-key-fn true}))

(defn- request-map [^HttpExchange exchange]
  {:method (.getRequestMethod exchange)
   :path (str (.getRequestURI exchange))
   :headers (into {} (map (fn [[k vs]] [(str/lower-case k) (first vs)]))
                  (.entrySet (.getRequestHeaders exchange)))
   :body (slurp (.getRequestBody exchange))})

(defn- respond! [^HttpExchange exchange status content-type body]
  (let [bytes (.getBytes body StandardCharsets/UTF_8)]
    (.set (.getResponseHeaders exchange) "content-type" content-type)
    (.sendResponseHeaders exchange status (long (if (= 200 status) 0 (count bytes))))
    (with-open [out (.getResponseBody exchange)]
      (.write out bytes))))

(defn- with-server [handler f]
  (let [requests (atom [])
        server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/"
                   (reify HttpHandler
                     (handle [_ exchange]
                       (let [request (request-map exchange)]
                         (swap! requests conj request)
                         (handler exchange request)))))
    (.start server)
    (try
      (f {:base-url (str "http://127.0.0.1:" (.getPort (.getAddress server)))
          :requests requests})
      (finally
        (.stop server 0)))))

(def stable-response
  (json/write-value-as-string
   {:id "msg_contract_stable"
    :type "message"
    :role "assistant"
    :model "claude-sonnet-4-5"
    :content [{:type "text" :text "stable reply"}]
    :stop_reason "end_turn"
    :stop_sequence nil
    :usage {:input_tokens 3 :output_tokens 2}}))

(def beta-response
  (json/write-value-as-string
   {:id "msg_contract_beta"
    :type "message"
    :role "assistant"
    :model "claude-sonnet-4-5"
    :content [{:type "text" :text "beta reply"}]
    :stop_reason "end_turn"
    :stop_sequence nil
    :usage {:input_tokens 4 :output_tokens 2}}))

(defn- message-request []
  {:model "claude-sonnet-4-5"
   :max-tokens 32
   :messages [{:role :user :content "hello"}]})

(deftest stable-messages-create-contract
  (with-server (fn [exchange _]
                 (respond! exchange 200 "application/json" stable-response))
               (fn [{:keys [base-url requests]}]
                 (let [response (a/create-message (a/client {:api-key "test-key" :base-url base-url})
                                                  (message-request))
                       request (first @requests)
                       body (json/read-value (:body request) json-mapper)]
                   (is (= "POST" (:method request)))
                   (is (= "/v1/messages" (:path request)))
                   (is (= "test-key" (get-in request [:headers "x-api-key"])))
                   (is (= "application/json" (get-in request [:headers "content-type"])))
                   (is (= "claude-sonnet-4-5" (:model body)))
                   (is (= 32 (:max_tokens body)))
                   (is (= [{:role "user" :content "hello"}] (:messages body)))
                   (is (= {:id "msg_contract_stable"
                         :model "claude-sonnet-4-5"
                         :role :assistant
                         :stop-reason :end-turn
                         :content [{:type :text :text "stable reply"}]
                         :usage {:input-tokens 3 :output-tokens 2}}
                        response))))))

(deftest beta-messages-create-contract
  (with-server (fn [exchange _]
                 (respond! exchange 200 "application/json" beta-response))
               (fn [{:keys [base-url requests]}]
                 (let [response (beta/create-beta-message
                                 (a/client {:api-key "test-key" :base-url base-url})
                                 (assoc (message-request) :messages [{:role :user :content "beta hello"}]))
                       request (first @requests)
                       body (json/read-value (:body request) json-mapper)]
                   (is (= "POST" (:method request)))
                 (is (= "/v1/messages?beta=true" (:path request)))
                   (is (= "beta hello" (get-in body [:messages 0 :content])))
                   (is (= "beta reply" (get-in response [:content 0 :text])))))))

(deftest models-list-pagination-contract
  (with-server (fn [exchange _]
                 (respond! exchange 200 "application/json"
                           (json/write-value-as-string
                            {:data [{:type "model" :id "model-contract"
                                     :display_name "Contract model"
                                     :created_at "2025-01-01T00:00:00Z"
                                     :max_input_tokens 100
                                     :max_tokens 20}]
                             :has_more false})))
               (fn [{:keys [base-url requests]}]
                 (let [models (a/list-models (a/client {:api-key "test-key" :base-url base-url})
                                             {:limit 1})
                       request (first @requests)]
                   (is (= "GET" (:method request)))
                   (is (= "/v1/models?limit=1" (:path request)))
                   (is (= [{:id "model-contract"
                          :display-name "Contract model"
                          :created-at "2025-01-01T00:00Z"
                          :max-input-tokens 100
                          :max-tokens 20}]
                        models))))))

(deftest sdk-error-json-contract
  (with-server (fn [exchange _]
                 (respond! exchange 400 "application/json"
                           (json/write-value-as-string
                            {:type "error"
                             :error {:type "invalid_request_error"
                                     :message "contract failure"}})))
               (fn [{:keys [base-url]}]
                 (let [error (try
                               (a/create-message (a/client {:api-key "test-key" :base-url base-url})
                                                 (message-request))
                               nil
                               (catch clojure.lang.ExceptionInfo e e))]
                   (is (= :api-error (:anthropic/error (ex-data error))))
                   (is (= :bad-request (:error-type (ex-data error))))
                   (is (= 400 (:status (ex-data error))))
                   (is (= {:type "error"
                           :error {:type "invalid_request_error"
                                   :message "contract failure"}}
                          (:body (ex-data error))))))))

(def sse-response
  (str "event: message_start\n"
       "data: {\"type\":\"message_start\",\"message\":{\"id\":\"msg_stream\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-sonnet-4-5\",\"content\":[],\"stop_reason\":null,\"stop_sequence\":null,\"usage\":{\"input_tokens\":1,\"output_tokens\":0}}}\n\n"
       "event: content_block_start\n"
       "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n"
       "event: content_block_delta\n"
       "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"hello \"}}\n\n"
       "event: content_block_delta\n"
       "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"stream\"}}\n\n"
       "event: content_block_stop\n"
       "data: {\"type\":\"content_block_stop\",\"index\":0}\n\n"
       "event: message_delta\n"
       "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\",\"stop_sequence\":null},\"usage\":{\"output_tokens\":2}}\n\n"
       "event: message_stop\n"
       "data: {\"type\":\"message_stop\"}\n\n"))

(deftest sse-stream-contract
  (with-server (fn [exchange _]
                 (respond! exchange 200 "text/event-stream" sse-response))
               (fn [{:keys [base-url requests]}]
                 (let [events (atom [])]
                   (is (= "hello stream"
                          (a/stream-text (a/client {:api-key "test-key" :base-url base-url})
                                         (message-request)
                                         #(swap! events conj %))))
                   (is (= "POST" (:method (first @requests))))
                   (is (= "text/event-stream" (get-in (first @requests) [:headers "accept"])))
                   (is (= ["hello " "stream"] @events))))))
