# anthropic-clj

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/anthropic-clj.svg)](https://clojars.org/net.clojars.savya/anthropic-clj)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/anthropic-clj)](https://cljdoc.org/d/net.clojars.savya/anthropic-clj)
[![test](https://github.com/jsavyasachi/anthropic-clj/actions/workflows/test.yml/badge.svg)](https://github.com/jsavyasachi/anthropic-clj/actions/workflows/test.yml)
[![Renovate](https://img.shields.io/badge/Renovate-enabled-1A1F6C?style=flat&logo=renovate&logoColor=fff)](https://github.com/jsavyasachi/anthropic-clj/issues?q=is%3Aissue+Dependency+Dashboard)

An idiomatic Clojure wrapper over the **official** Anthropic Java SDK
([`com.anthropic/anthropic-java`](https://github.com/anthropics/anthropic-sdk-java)).
Build a request as a Clojure map, get a Clojure map back.

> **Unofficial.** A community library, not affiliated with or endorsed by
> Anthropic. It wraps Anthropic's official Java SDK; it is not itself an official
> Anthropic SDK.

## Stack

<a href="https://clojure.org"><img src="https://img.shields.io/badge/Clojure-5881D8?style=flat&logo=clojure&logoColor=fff" alt="Clojure" /></a>
<a href="https://docs.anthropic.com"><img src="https://img.shields.io/badge/Anthropic-D97757?style=flat&logo=anthropic&logoColor=fff" alt="Anthropic" /></a>

## Why

Every other Clojure Anthropic library hand-rolls HTTP against the REST API, which
means each one is perpetually chasing Anthropic's surface and tends to fall
behind. This one wraps Anthropic's own actively-maintained Java SDK instead, so
streaming, tool use, retries, and every new model and feature arrive the moment
Anthropic ships them in Java - you just get a Clojure-shaped API on top: maps in,
maps out, keywords for roles and block types.

## Installation

Leiningen (`project.clj`):

```clojure
[net.clojars.savya/anthropic-clj "0.2.0"]
```

tools.deps (`deps.edn`):

```clojure
net.clojars.savya/anthropic-clj {:mvn/version "0.2.0"}
```

Set `ANTHROPIC_API_KEY` in your environment (or pass `:api-key` to `client`).

## Usage

```clojure
(require '[anthropic.core :as anthropic])

(def client (anthropic/client))   ; reads ANTHROPIC_API_KEY

;; A single message. :model defaults to "claude-opus-4-8", :max-tokens to 1024.
(anthropic/create-message
  client
  {:model "claude-opus-4-8"
   :max-tokens 1024
   :system "You are concise."
   :messages [{:role :user :content "Name three primary colors."}]})
;; => {:id "msg_..." :model "claude-opus-4-8" :role :assistant
;;     :stop-reason :end-turn
;;     :content [{:type :text :text "Red, blue, yellow."}]
;;     :usage {:input-tokens 18 :output-tokens 9}}
```

`create-message` also accepts the optional controls `:temperature`, `:top-p`,
`:top-k`, `:stop-sequences`, `:tool-choice` (`:auto`/`:any`/`:none` or
`{:type :tool :name "x"}`), `:thinking` (`{:type :enabled :budget-tokens N}`,
`{:type :adaptive}`, or `{:type :disabled}`), `:metadata` (`{:user-id "..."}`),
and `:service-tier` (`:auto`/`:standard-only`). When the response uses prompt
caching, `:usage` also carries `:cache-creation-input-tokens` and
`:cache-read-input-tokens`.

### Counting tokens

`count-tokens` takes the same request map and returns the input-token count
without sending the message (`:max-tokens` and sampling params are ignored).

```clojure
(anthropic/count-tokens
  client
  {:messages [{:role :user :content "How many tokens is this?"}]})
;; => {:input-tokens 13}
```

### Streaming

`stream-text` calls your callback with each text delta and returns the full text.

```clojure
(anthropic/stream-text
  client
  {:model "claude-opus-4-8" :max-tokens 256
   :messages [{:role :user :content "Write a haiku about parentheses."}]}
  #(print %))   ; prints each delta as it arrives
;; => returns the complete string when the stream ends
```

For thinking or tool-use streams, `stream` surfaces every normalized event. Each
event is a map keyed by `:type`: `:message-start`, `:content-block-start`
(`:index`, `:block`), `:text-delta`/`:thinking-delta`/`:input-json-delta`/
`:signature-delta` (`:index` plus payload), `:content-block-stop` (`:index`),
`:message-delta` (`:stop-reason`), and `:message-stop`. It still returns the full
text. To rebuild a streamed tool call, accumulate `:input-json-delta`
`:partial-json` per `:index` - the matching `:content-block-start` carries the
tool `:id`/`:name`.

```clojure
(anthropic/stream
  client
  {:max-tokens 256 :messages [{:role :user :content "Think, then answer."}]}
  (fn [ev]
    (case (:type ev)
      :thinking-delta (print "[thinking]" (:thinking ev))
      :text-delta     (print (:text ev))
      nil)))
```

### Tool use

Declare tools as maps; `tool_use` blocks come back parsed, and you complete the
loop by echoing the assistant turn and sending a `:tool-result` block.

```clojure
(def weather-tool
  {:name "get_weather"
   :description "Get the current weather for a city"
   :input-schema {:type "object"
                  :properties {:city {:type "string"}}
                  :required ["city"]}})

(def ask {:role :user :content "What's the weather in Paris?"})

(def r1 (anthropic/create-message
          client {:tools [weather-tool] :messages [ask]}))

;; r1 :stop-reason is :tool-use; find the call:
(def call (first (filter #(= :tool-use (:type %)) (:content r1))))
;; => {:type :tool-use :id "toolu_..." :name "get_weather" :input {:city "Paris"}}

;; Run the tool yourself, then send the result back:
(anthropic/create-message
  client
  {:tools [weather-tool]
   :messages [ask
              {:role :assistant :content (:content r1)}
              {:role :user :content [{:type :tool-result
                                      :tool-use-id (:id call)
                                      :content "18°C and sunny"}]}]})
;; => {... :content [{:type :text :text "It's 18°C and sunny in Paris."}]}
```

## What's covered

- `create-message` - request map ↔ response map, with the full set of request
  controls (sampling, stop sequences, tool-choice, thinking, metadata,
  service-tier) and cache-token usage
- `count-tokens` - input-token count without sending
- `stream-text` - incremental text deltas
- `stream` - every normalized stream event (message + content-block lifecycle,
  text/thinking/tool-use/signature deltas)
- Tool use - tools, parsed `tool_use` blocks, `tool_result` round-trip
- Roadmap (0.3): structured outputs (`output_config.format`), batches, files

## Tests

Unit tests (the request/response translation) run with no network:

```
lein test
```

The `:integration` suite hits the live API and is billed - it needs
`ANTHROPIC_API_KEY` and is run explicitly:

```
ANTHROPIC_API_KEY=sk-... lein test :integration
```

## License

Copyright © 2026 Savyasachi

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).
The wrapped `com.anthropic/anthropic-java` SDK is MIT-licensed and remains the
property of Anthropic.
