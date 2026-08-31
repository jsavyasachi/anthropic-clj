(ns anthropic.pagination)

(def ^:dynamic *error-handler* nil)

(defn ->lazy-pager
  "Map `f` lazily over an SDK auto-pager."
  [f pager]
  (let [^java.util.Iterator iterator (.iterator ^java.lang.Iterable pager)
        error-handler *error-handler*]
    (letfn [(step []
              (lazy-seq
                (try
                  (when (.hasNext iterator)
                    (cons (f (.next iterator)) (step)))
                  (catch Throwable e
                    (if error-handler
                      (error-handler e)
                      (throw e))))))]
      (step))))
