(ns anthropic.stream-test
  (:require [clojure.test :refer [deftest is]]
            [anthropic.stream :as stream])
  (:import (com.anthropic.core.http StreamResponse)))

(defn- response
  ([events closed?] (response events closed? nil))
  ([events closed? consumed]
   (reify StreamResponse
     (stream [_]
       (let [s (.stream (java.util.ArrayList. events))]
         (if consumed
           (.map s (reify java.util.function.Function
                     (apply [_ value] (swap! consumed conj value) value)))
           s)))
     (close [_] (reset! closed? true)))))

(deftest cancellation-closes-response-and-stops-consumption
  (let [closed? (atom false)
        consumed (atom [])
        seen (atom [])
        handle* (atom nil)
        handle (stream/start! #(response [1 2 3] closed? consumed)
                              #(swap! seen conj %)
                              nil)]
    (reset! handle* handle)
    (is (= :done (stream/await-stream handle 1000)))
    (is (= [1 2 3] @consumed))
    (is @closed?)
    (stream/cancel-stream! handle)
    (is (= :done (stream/await-stream handle 1000)))))

(deftest cancellation-from-callback-stops-real-consumption
  (let [closed? (atom false)
        consumed (atom [])
        seen (atom [])
        handle-ready (promise)
        handle (stream/start! #(response [1 2 3] closed? consumed)
                              (fn [event]
                                (swap! seen conj event)
                                (when (= event 1)
                                  (stream/cancel-stream! @handle-ready)))
                              nil)]
    (deliver handle-ready handle)
    (is (= :cancelled (stream/await-stream handle 1000)))
    (is (= [1] @consumed))
    (is (= [1] @seen))
    (is @closed?)))

(deftest bounded-queue-blocks-producer-until-consumed
  (let [closed? (atom false)
        consumed (atom [])
        handle (stream/start! #(response [1 2 3] closed? consumed)
                              nil
                              {:buffer-size 1})]
    (try
      (Thread/sleep 100)
      (is (= 1 (.size ^java.util.concurrent.BlockingQueue (:queue handle))))
      (is (= [1 2] @consumed))
      (is (= ::still-running (deref @(:future handle) 50 ::still-running)))
      (is (= 1 (stream/take-stream-event handle 1000)))
      (is (= 2 (stream/take-stream-event handle 1000)))
      (is (= 3 (stream/take-stream-event handle 1000)))
      (is (nil? (stream/take-stream-event handle 1000)))
      (finally
        (stream/close-stream! handle)))))

(deftest bounded-queue-signals-producer-errors
  (let [handle (stream/start! #(throw (ex-info "boom" {:kind :test})) nil
                              {:buffer-size 2})]
    (is (= :error (stream/await-stream handle 1000)))
    (is (= "boom"
           (try (stream/take-stream-event handle 1000)
                (catch clojure.lang.ExceptionInfo error
                  (.getMessage error)))))
    (is (nil? (stream/take-stream-event handle 1000)))
    (stream/close-stream! handle)))
