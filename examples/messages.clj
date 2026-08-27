(ns examples.messages
  "Send a basic Messages API request.")

(require '[anthropic.core :as anthropic])

(comment
  ;; Evaluate the setup form once, or use a client from client_setup.clj.
  (def client (anthropic/client))

  ;; :model and :max-tokens are optional in the wrapper, but are explicit here
  ;; so the request is easy to copy into a REPL and adjust.
  (def response
    (anthropic/create-message
     client
     {:model "claude-opus-4-8"
      :max-tokens 256
      :system "Answer concisely."
      :messages [{:role :user
                  :content "Name three primary colors."}]}))

  (:content response)
  (:usage response))
