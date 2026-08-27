(ns examples.client-setup
  "Create an Anthropic client using environment or explicit non-secret options.")

(require '[anthropic.core :as anthropic])

(comment
  ;; The zero-argument client reads ANTHROPIC_API_KEY from the environment.
  ;; Set it before starting the REPL; never put the key in this file.
  (def client (anthropic/client))

  ;; The options form is useful for endpoint and request configuration. Keep
  ;; credentials in environment variables or your secret manager, then inject
  ;; them at the REPL rather than committing them to source control.
  (def configured-client
    (anthropic/client
     {:api-key (System/getenv "ANTHROPIC_API_KEY")
      :base-url (or (System/getenv "ANTHROPIC_BASE_URL")
                    "https://api.anthropic.com")
      :timeout-ms 30000
      :max-retries 2
      :log-level :info}))

  ;; Reuse either client in the other examples:
  client)
