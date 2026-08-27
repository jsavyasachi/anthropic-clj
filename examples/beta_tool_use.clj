(ns examples.beta-tool-use
  "Run a beta Messages request with a local tool function.")

(require '[anthropic.beta.messages :as beta-messages]
         '[anthropic.core :as anthropic])

(comment
  (def client (anthropic/client))

  ;; A tool's :fn is local Clojure code. run-beta-tools removes :fn from the
  ;; request sent to Anthropic, invokes it when the model requests the tool,
  ;; and sends the returned value back as a tool result.
  (def weather-tool
    {:name "get_weather"
     :description "Get the current weather for a city"
     :input-schema {:type "object"
                    :properties {:city {:type "string"}}
                    :required ["city"]}
     :fn (fn [{:keys [city]}]
           (str city " is 18 degrees Celsius and sunny."))})

  (def response
    (beta-messages/run-beta-tools
     client
     {:model "claude-opus-4-8"
      :max-tokens 512
      :messages [{:role :user :content "What's the weather in Paris?"}]
      :tools [weather-tool]}
     {:max-iterations 5
      :on-message #(println "beta response:" (:stop-reason %))}))

  (:content response)
  (:messages response))
