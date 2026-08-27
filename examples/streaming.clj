(ns examples.streaming
  "Stream text and inspect normalized Messages events.")

(require '[anthropic.core :as anthropic])

(comment
  (def client (anthropic/client))

  ;; stream-text calls the callback once for every text delta and returns the
  ;; complete text after the HTTP stream closes.
  (def text
    (anthropic/stream-text
     client
     {:model "claude-opus-4-8"
      :max-tokens 256
      :messages [{:role :user
                  :content "Write a haiku about parentheses."}]}
     #(print %)))
  (println "\nComplete text:" text)

  ;; stream-message reconstructs the complete response map while the callback
  ;; receives normalized event maps such as :text-delta and :message-stop.
  (def streamed-response
    (anthropic/stream-message
     client
     {:model "claude-opus-4-8"
      :max-tokens 256
      :messages [{:role :user :content "Explain Clojure's comment form."}]}
     #(println (:type %))))

  (:content streamed-response))
