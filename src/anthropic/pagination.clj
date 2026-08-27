(ns anthropic.pagination)

(defn ->lazy-pager
  "Map `f` lazily over an SDK auto-pager."
  [f pager]
  (let [^java.util.Iterator iterator (.iterator ^java.lang.Iterable pager)]
    (letfn [(step []
              (lazy-seq
                (when (.hasNext iterator)
                  (cons (f (.next iterator)) (step)))))]
      (step))))
