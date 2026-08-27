(ns examples.structured-output
  "Request JSON Schema structured output and read the parsed response.")

(require '[anthropic.core :as anthropic])

(comment
  (def client (anthropic/client))

  ;; Object schemas must set additionalProperties to false for the API. The
  ;; wrapper decodes the first text block into the response's :parsed value.
  (def response
    (anthropic/create-message
     client
     {:model "claude-opus-4-8"
      :max-tokens 128
      :response-format {:type "object"
                        :properties {:capital {:type "string"}}
                        :required ["capital"]
                        :additionalProperties false}
      :messages [{:role :user
                  :content "What is the capital of France?"}]}))

  (:parsed response)
  ;; The original JSON text is still available in the normal content blocks.
  (:content response))
