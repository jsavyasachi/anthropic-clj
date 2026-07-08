# Streaming

Streaming calls hit the live Messages API and are billed. They should be used
in integration tests or application code, not ordinary unit tests.

The wrapper has two streaming entry points:

- `stream-text`: text deltas only
- `stream`: every normalized stream event

Both take the same request map shape as `create-message`. Both close the
underlying HTTP stream automatically. API and I/O errors use the same
`ex-info` contract as `create-message`.

## `stream-text`

`stream-text` calls `on-text` with each text delta string as it arrives and
returns the full concatenated assistant text when the stream ends.

```clojure
(require '[anthropic.core :as anthropic])

(def full-text
  (anthropic/stream-text
    client
    {:model "claude-haiku-4-5"
     :max-tokens 64
     :messages [{:role :user
                 :content "Reply with a short sentence about Clojure."}]}
    (fn [delta]
      (print delta))))

full-text
;; => "Clojure is a practical Lisp for hosted runtimes."
```

The callback may be `nil`. In that case `stream-text` still returns the full
text.

`stream-text` ignores non-text events, including thinking and tool-use deltas.
Use `stream` when you need those.

## `stream`

`stream` calls `on-event` with one normalized event map for each server-sent
event and returns the full concatenated assistant text when the stream ends.

```clojure
(def events (atom []))

(def full-text
  (anthropic/stream
    client
    {:model "claude-haiku-4-5"
     :max-tokens 64
     :messages [{:role :user
                 :content "Reply with the single word: pong"}]}
    (fn [event]
      (swap! events conj event))))

full-text
;; => "pong"

(map :type @events)
;; => (:message-start :content-block-start :text-delta ... :message-stop)
```

Event maps are keyed by `:type`.

Message lifecycle:

```clojure
{:type :message-start}
{:type :message-delta :stop-reason :end-turn}
{:type :message-stop}
```

Content block lifecycle:

```clojure
{:type :content-block-start
 :index 0
 :block {:type :text}}

{:type :content-block-stop
 :index 0}
```

Text deltas:

```clojure
{:type :text-delta
 :index 0
 :text "pong"}
```

Thinking deltas:

```clojure
{:type :thinking-delta
 :index 0
 :thinking "hmm"}

{:type :signature-delta
 :index 0}
```

Tool-use deltas:

```clojure
{:type :content-block-start
 :index 1
 :block {:type :tool-use
         :id "toolu_..."
         :name "get_weather"}}

{:type :input-json-delta
 :index 1
 :partial-json "{\"city\""}
```

To rebuild streamed tool input, accumulate `:partial-json` from
`:input-json-delta` events per `:index`. The matching `:content-block-start`
event carries the tool `:id` and `:name`.

The return value of `stream` is only the concatenated text from `:text-delta`
events. If the stream contains only thinking or tool-use events, the returned
string can be empty while callbacks still receive events.

## Minimal Event Handling

```clojure
(anthropic/stream
  client
  {:model "claude-haiku-4-5"
   :max-tokens 128
   :messages [{:role :user :content "Think briefly, then answer."}]}
  (fn [{:keys [type text thinking partial-json]}]
    (case type
      :text-delta (print text)
      :thinking-delta (print thinking)
      :input-json-delta (print partial-json)
      nil)))
```

The callback may be `nil`. In that case `stream` still consumes the stream and
returns the full text.
