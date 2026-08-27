(ns anthropic.stream
  (:import (java.util.concurrent ArrayBlockingQueue BlockingQueue TimeUnit)))

(def ^:private complete-token ::complete)
(def ^:private cancelled-token ::cancelled)

(defn- close-response! [handle]
  (when-let [response @(:response handle)]
    (try (.close ^java.lang.AutoCloseable response) (catch Exception _))))

(defn cancel-stream! [handle]
  (when (compare-and-set! (:cancelled? handle) false true)
    (close-response! handle)
    (when-let [queue ^BlockingQueue (:queue handle)]
      (.clear queue)
      (.offer queue cancelled-token)))
  (when-let [f @(:future handle)]
    (when-not (identical? (Thread/currentThread) @(:worker-thread handle))
      (future-cancel f)))
  handle)

(defn close-stream! [handle] (cancel-stream! handle))

(defn- publish! [handle event]
  (let [event (if-let [map-event (:map-event handle)] (map-event event) event)]
  (if-let [queue ^BlockingQueue (:queue handle)]
    (.put queue event)
    (when-let [on-event (:on-event handle)] (on-event event)))))

(defn- run-stream! [handle open-stream]
  (reset! (:worker-thread handle) (Thread/currentThread))
  (try
    (with-open [response (open-stream)]
      (reset! (:response handle) response)
      (if @(:cancelled? handle)
        :cancelled
        (do
          (let [iterator (.iterator (.stream ^com.anthropic.core.http.StreamResponse response))]
            (loop []
              (when-not @(:cancelled? handle)
                (when (.hasNext ^java.util.Iterator iterator)
                  (publish! handle (.next ^java.util.Iterator iterator))
                  (recur)))))
          (if @(:cancelled? handle) :cancelled :done))))
    (catch InterruptedException _ :cancelled)
    (catch Throwable t
      (reset! (:error handle) t)
      (when-let [queue ^BlockingQueue (:queue handle)] (.put queue t))
      :error)
    (finally
      (reset! (:done? handle) true)
      (when-let [queue ^BlockingQueue (:queue handle)]
        (when-not @(:cancelled? handle) (.put queue complete-token))))))

(defn start!
  "Start a stream producer. With `:buffer-size`, events are pulled with
  `take-stream-event`; otherwise `on-event` is invoked from the producer."
  [open-stream on-event {:keys [buffer-size map-event] :or {buffer-size nil}}]
  (when (and buffer-size (not (pos-int? buffer-size)))
    (throw (IllegalArgumentException. "buffer-size must be positive")))
  (let [queue (when buffer-size (ArrayBlockingQueue. (int buffer-size)))
        handle-p (promise)
        handle {:queue queue :on-event on-event :map-event map-event :future (atom nil)
                :response (atom nil) :cancelled? (atom false)
                :done? (atom false) :error (atom nil)
                :worker-thread (atom nil)}
        worker (future (run-stream! @handle-p open-stream))]
    (reset! (:future handle) worker)
    (deliver handle-p handle)
    handle))

(defn await-stream
  ([handle] (try @(:future handle) (catch java.util.concurrent.CancellationException _ :cancelled)))
  ([handle timeout-ms]
   (try (deref @(:future handle) timeout-ms ::timeout)
        (catch java.util.concurrent.CancellationException _ :cancelled))))

(defn take-stream-event [handle timeout-ms]
  (when-not (:queue handle)
    (throw (IllegalArgumentException. "handle has no bounded queue")))
  (let [value (.poll ^BlockingQueue (:queue handle) (long timeout-ms) TimeUnit/MILLISECONDS)]
    (cond
      (= value complete-token) nil
      (= value cancelled-token) (throw (ex-info "stream cancelled" {:type :stream-cancelled}))
      (instance? Throwable value) (throw ^Throwable value)
      :else value)))
