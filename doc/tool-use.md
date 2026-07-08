# Tool Use

Tool calls are Messages API calls and are billed. `count-tokens` is also an API
call, but it only counts the request's input tokens and does not send a message.

## Custom Tools

Define a custom tool with `:name`, optional `:description`, and
`:input-schema`.

```clojure
(require '[anthropic.core :as anthropic])

(def weather-tool
  {:name "get_weather"
   :description "Get the current weather for a city"
   :input-schema {:type "object"
                  :properties {:city {:type "string"}}
                  :required ["city"]}})

(def ask
  {:role :user :content "Use the get_weather tool for Paris."})

(def r1
  (anthropic/create-message
    client
    {:model "claude-haiku-4-5"
     :max-tokens 400
     :tools [weather-tool]
     :messages [ask]}))
```

When the model asks for a tool, the response has `:stop-reason :tool-use` and
one or more `:tool-use` blocks in `:content`:

```clojure
(def call
  (first (filter #(= :tool-use (:type %)) (:content r1))))

call
;; => {:type :tool-use
;;     :id "toolu_..."
;;     :name "get_weather"
;;     :input {:city "Paris"}}
```

You run the tool yourself. Then send the next message with:

- the original user turn
- the assistant turn containing `(:content r1)`
- a user turn containing a `:tool-result` block

```clojure
(anthropic/create-message
  client
  {:model "claude-haiku-4-5"
   :max-tokens 100
   :tools [weather-tool]
   :messages [ask
              {:role :assistant :content (:content r1)}
              {:role :user
               :content [{:type :tool-result
                          :tool-use-id (:id call)
                          :content "18C and sunny"}]}]})
```

`:tool-result` `:content` can be a string or any Clojure value. Non-string
values are JSON-encoded when converted to the SDK content block. Add
`:is-error true` when the tool failed.

## `run-tools`

`run-tools` drives the manual loop for local functions.

```clojure
(def weather-tool-with-fn
  (assoc weather-tool
         :fn (fn [{:keys [city]}]
               {:city city :forecast "sunny"})))

(def result
  (anthropic/run-tools
    client
    {:model "claude-haiku-4-5"
     :max-tokens 400
     :messages [ask]
     :tools [weather-tool-with-fn]}
    {:max-iterations 5
     :on-message println}))
```

Each tool `:fn` receives the parsed `:input` map from a `:tool-use` block and
returns the tool result content.

Behavior:

- `:fn` is stripped from tools before every API call.
- If the request's `:messages` is a string, it is normalized to one user turn.
- If a response stops with `:stop-reason :tool-use`, every `:tool-use` block in
  that response is executed.
- Parallel tool calls become one user turn containing ordered `:tool-result`
  blocks.
- If a tool `:fn` throws, the exception message becomes a `:tool-result` with
  `:is-error true`; the loop continues.
- If the model calls a tool that has no matching `:fn`, `run-tools` throws
  `ex-info` with `{:anthropic/error :no-tool-fn :name "..."}`.
- `:max-iterations` defaults to `10`. Exceeding it throws `ex-info` with
  `{:anthropic/error :max-iterations-exceeded :iterations n :messages [...]}`.
- `:on-message`, when supplied, is called with each response map in order.
- The returned value is the final response map plus `:messages`, the accumulated
  conversation including the final assistant turn.

The returned `:messages` can be passed into a later `create-message` call to
continue the conversation.

## Server-side Tools

Server-side tools are declared in `:tools` and executed by Anthropic. The model
returns `:server-tool-use` blocks and typed result blocks.

Supported server tool `:type` values:

- `:web-search`
- `:web-fetch`
- `:code-execution`
- `:bash`
- `:text-editor`
- `:memory`
- `:tool-search`

```clojure
(anthropic/create-message
  client
  {:model "claude-haiku-4-5"
   :max-tokens 1024
   :tools [{:type :web-search
            :max-uses 2
            :allowed-domains ["clojure.org"]
            :allowed-callers [:direct]}
           {:type :web-fetch
            :max-content-tokens 4096}
           {:type :code-execution}
           {:type :bash}
           {:type :text-editor
            :max-characters 2000}
           {:type :memory}
           {:type :tool-search
            :variant :bm25}
           {:type :tool-search
            :variant :regex
            :defer-loading true
            :strict true}]
   :messages [{:role :user
               :content "Search the web for current Clojure news."}]})
```

Supported server-tool options:

- `:web-search`: `:max-uses`, `:allowed-domains`, `:blocked-domains`,
  `:user-location`, `:allowed-callers`
- `:web-fetch`: `:max-uses`, `:max-content-tokens`, `:allowed-domains`,
  `:blocked-domains`, `:allowed-callers`
- `:code-execution`: `:allowed-callers`
- `:text-editor`: `:max-characters`
- `:tool-search`: `:variant` as `:bm25` or `:regex`, `:allowed-callers`,
  `:cache-control`, `:defer-loading`, `:strict`

`:user-location` supports `:city`, `:region`, `:country`, and `:timezone`.
`:allowed-callers` values are keywords such as `:direct`.

Server-side result block types parsed by the wrapper include:

- `:web-search-result`
- `:web-fetch-result`
- `:code-execution-result`
- `:bash-code-execution-result`
- `:text-editor-code-execution-result`
- `:tool-search-result`

The beta agents platform in `anthropic.beta` is a separate surface. See the
README for that API.

## Counting Tokens With Tools

`count-tokens` accepts the same tool specs as `create-message`, including custom
tools and server-side tools. It ignores `:max-tokens` and sampling params.

```clojure
(anthropic/count-tokens
  client
  {:model "claude-haiku-4-5"
   :tools [weather-tool
           {:type :web-search :max-uses 2}]
   :messages [{:role :user
               :content "How many input tokens does this request use?"}]})
;; => {:input-tokens 42}
```
