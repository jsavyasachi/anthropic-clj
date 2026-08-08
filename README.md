# anthropic-clj

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/anthropic-clj.svg)](https://clojars.org/net.clojars.savya/anthropic-clj)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/anthropic-clj)](https://cljdoc.org/d/net.clojars.savya/anthropic-clj)
[![test](https://github.com/jsavyasachi/anthropic-clj/actions/workflows/test.yml/badge.svg)](https://github.com/jsavyasachi/anthropic-clj/actions/workflows/test.yml)

An idiomatic Clojure wrapper over the **official** Anthropic Java SDK
([`com.anthropic/anthropic-java`](https://github.com/anthropics/anthropic-sdk-java)).
Build a request as a Clojure map, get a Clojure map back.

> **Unofficial.** A community library, not affiliated with or endorsed by
> Anthropic. It wraps Anthropic's official Java SDK; it is not itself an official
> Anthropic SDK.

## Stack

<a href="https://clojure.org"><img src="https://img.shields.io/badge/Clojure-5881D8?style=flat&logo=clojure&logoColor=fff" alt="Clojure" /></a>
<a href="https://clojure.org/guides/deps_and_cli"><img src="https://img.shields.io/badge/deps.edn-5881D8?style=flat&logo=clojure&logoColor=fff" alt="deps.edn" /></a>
<a href="https://clojure.github.io/tools.build/"><img src="https://img.shields.io/badge/tools.build-5881D8?style=flat&logo=clojure&logoColor=fff" alt="tools.build" /></a>
<a href="https://docs.anthropic.com"><img src="https://img.shields.io/badge/Anthropic-D97757?style=flat&logo=anthropic&logoColor=fff" alt="Anthropic" /></a>
<a href="https://github.com/metosin/jsonista"><img src="https://img.shields.io/badge/jsonista-2D3748?style=flat&logo=clojure&logoColor=fff" alt="jsonista" /></a>

## Why

This library wraps Anthropic's official Java SDK rather than hand-rolling HTTP,
and commits to idiomatic parity with that SDK: maps in, maps out, and keywords
for roles and block types. Parity is checked against the SDK jar, not asserted.

## Installation

tools.deps (`deps.edn`):

```clojure
net.clojars.savya/anthropic-clj {:mvn/version "0.24.0"}
```

Leiningen (`project.clj`):

```clojure
[net.clojars.savya/anthropic-clj "0.24.0"]
```

Set `ANTHROPIC_API_KEY` in your environment, or pass client options:

- `:api-key`, `:auth-token`, `:base-url` - credentials and endpoint
- `:timeout-ms`, `:max-retries` - request behavior
- `:webhook-key` - key for `unwrap-webhook` signature verification
- `:log-level` - `:off`/`:info`/`:error`/`:debug`
- `:response-validation` - strict response-shape checking
- `:proxy` - a `java.net.Proxy`
- `:headers`, `:query-params` - defaults sent on every request
- `:configure` - receives the raw SDK builder last, for anything not wrapped
  here (interceptors, a custom `jsonMapper`, or a Bedrock/Vertex `backend`)

Tracks [`com.anthropic/anthropic-java` 2.53.0](https://github.com/anthropics/anthropic-sdk-java/releases/tag/v2.53.0) - see `CHANGELOG.md` for the bump history.

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

`create-message` also accepts these optional controls:

- `:temperature`, `:top-p`, `:top-k`, `:stop-sequences` - sampling
- `:tool-choice` - `:auto`/`:any`/`:none` or `{:type :tool :name "x"}`
- `:thinking` - `{:type :enabled :budget-tokens N}`, `{:type :adaptive}`, or
  `{:type :disabled}`
- `:metadata` - `{:user-id "..."}`
- `:service-tier` - `:auto`/`:standard-only`
- `:container`, `:inference-geo`, `:user-profile-id`
- `:cache-control` - top-level prompt-cache breakpoint

`create-message` and `count-tokens` accept a third `opts` map with `:timeout-ms`,
`:response-validation`, and `:include-response`; the latter adds raw HTTP
`:status`, `:request-id`, and headers. Request maps accept `:extra-headers`,
`:extra-query`, and `:extra-body` as forward-compatibility escape hatches.

For structured output, pass `:response-format` and/or `:effort`. Responses
include newer `:usage` fields when present: cache creation/read tokens,
server-tool usage, service-tier, inference geo, cache creation details, and
output-token details.

### Images, PDFs, and prompt caching

Message content can be a vector of blocks. Beyond `:text`, `:tool-use`, and
`:tool-result`, you can send `:image`, `:document`, `:search-result`,
`:thinking`, `:redacted-thinking`, and `:container-upload` blocks. Blocks that
support prompt caching accept `:cache-control`.

```clojure
(anthropic/create-message
  client
  {:max-tokens 256
   :messages [{:role :user
               :content [{:type :image
                          :source {:type :base64 :media-type "image/png" :data "<base64>"}}
                         ;; or {:type :url :url "https://…/photo.jpg"}
                         {:type :document
                          :source {:type :url :url "https://…/paper.pdf"}
                          :title "Paper"}
                         {:type :search-result
                          :source "https://example.com/result"
                          :title "Result"
                          :citations true
                          :content [{:type :text :text "Relevant excerpt"}]}
                         {:type :text :text "Summarize the paper and the image."
                          :cache-control true}]}]})  ; :cache-control {:ttl :1h} for 1-hour
```

Assistant turns that contained thinking can be round-tripped with
`{:type :thinking :thinking "..." :signature "..."}` or
`{:type :redacted-thinking :data "..."}`. Container uploads use
`{:type :container-upload :file-id "file_..."}`.

### Server-side tools

Enable Anthropic-hosted tools by `:type` (latest version of each is used). The
model runs them server-side; the response content carries `:server-tool-use`
blocks and typed result blocks (`:web-search-result`, `:code-execution-result`,
…).

```clojure
(anthropic/create-message
  client
  {:max-tokens 1024
   :tools [{:type :web-search :max-uses 3
            :allowed-domains ["clojure.org"]        ; or :blocked-domains
            :user-location {:city "Paris" :country "FR"}
            :allowed-callers [:direct]}             ; some models need :direct
           {:type :web-fetch :max-content-tokens 4096}
           {:type :code-execution}
           {:type :bash}
           {:type :text-editor :max-characters 2000}
           {:type :memory}
           {:type :tool-search :variant :bm25}   ; or :regex
           {:type :tool-search :variant :regex
            :defer-loading true :strict true
            :allowed-callers [:direct]}]
   :messages [{:role :user :content "Search the web for today's Clojure news."}]})
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
(def r1 (anthropic/create-message client {:tools [weather-tool] :messages [ask]}))
(def call (first (filter #(= :tool-use (:type %)) (:content r1))))

(anthropic/create-message
  client
  {:tools [weather-tool]
   :messages [ask
              {:role :assistant :content (:content r1)}
              {:role :user :content [{:type :tool-result
                                      :tool-use-id (:id call)
                                      :content "18°C and sunny"}]}]})
```

Or let `run-tools` drive the loop with a `:fn` for each tool:

```clojure
(anthropic/run-tools
  client
  {:messages [ask]
   :tools [(assoc weather-tool :fn (fn [{:keys [city]}] (fetch-weather city)))]}
  {:max-iterations 5 :on-message println})
```

A tool `:fn` that throws sends the exception message back as an `:is-error`
tool result instead of aborting, so the model can recover. String returns are
sent as-is; any other value is JSON-encoded. `:fn` is stripped before every
API call.

### Structured output

Pass `:response-format` (a JSON Schema map) to get a `:parsed` Clojure map back.
Object schemas must set `"additionalProperties": false` (an API requirement).
`:effort` (`:low`…`:max`) is accepted alongside or on its own.

```clojure
(anthropic/create-message
  client
  {:max-tokens 256
   :response-format {:type "object"
                     :properties {:capital {:type "string"}}
                     :required ["capital"]
                     :additionalProperties false}
   :messages [{:role :user :content "What is the capital of France?"}]})
;; => {... :content [{:type :text :text "{\"capital\":\"Paris\"}"}]
;;     :parsed {:capital "Paris"}}
```

### Counting tokens

`count-tokens` takes the same request map and returns the input-token count
without sending the message (`:max-tokens` and sampling params are ignored).

```clojure
(anthropic/count-tokens
  client
  {:messages [{:role :user :content "How many tokens is this?"}]})
;; => {:input-tokens 13}
```

### Models

`:model` accepts a raw model-id string or a convenience keyword from
`anthropic.core/models`, such as `:claude-opus-4-8`. An unknown keyword throws
`ex-info` with `{:anthropic/error :unknown-model}`.

```clojure
(anthropic/create-message client {:model :claude-opus-4-8 :max-tokens 64 :messages [{:role :user :content "Hello"}]})
```

```clojure
(anthropic/list-models client)
;; => [{:id "claude-opus-4-8" :display-name "Claude Opus 4.8"
;;      :created-at "2026-..." :max-tokens 64000} ...]

(anthropic/get-model client "claude-opus-4-8")
;; => {:id "claude-opus-4-8" :display-name "Claude Opus 4.8" ...}
```

### Files (beta)

```clojure
(def f (anthropic/upload-file client "paper.pdf"))   ; path/File/Path/InputStream/bytes
;; => {:id "file_..." :filename "paper.pdf" :mime-type "application/pdf"
;;     :size-bytes 12345 :created-at "2026-..."}

(anthropic/get-file client (:id f))
(anthropic/list-files client)
(anthropic/download-file client some-id)   ; bytes; only API-generated downloadable files
(anthropic/delete-file client (:id f))
```

### Message Batches

Submit many requests at the 50%-cost batch tier. Each request is
`{:custom-id "..." :params <same map as create-message>}`.

```clojure
(def batch
  (anthropic/create-batch
    client
    [{:custom-id "a" :params {:max-tokens 64 :messages [{:role :user :content "Hi"}]}}
     {:custom-id "b" :params {:max-tokens 64 :messages [{:role :user :content "Bye"}]}}]))
;; => {:id "msgbatch_..." :processing-status :in-progress
;;     :request-counts {:processing 2 :succeeded 0 ...} ...}

(anthropic/get-batch client (:id batch))     ; poll until :processing-status :ended
(anthropic/list-batches client)
(anthropic/cancel-batch client (:id batch))

;; Once ended, pull results (succeeded entries carry the parsed :message):
(anthropic/batch-results client (:id batch))
;; => [{:custom-id "a" :result {:type :succeeded :message {...}}} ...]

;; Streaming reduction for large result sets:
(anthropic/reduce-batch-results client (:id batch)
  (fn [acc result] (conj acc (:custom-id result)))
  [])
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

For thinking or tool-use streams, `stream` surfaces every normalized event and
still returns the full text. Each event is a map keyed by `:type`:

- `:message-start`
- `:content-block-start` - `:index`, `:block`
- `:text-delta` / `:thinking-delta` / `:input-json-delta` / `:signature-delta` -
  `:index` plus the payload
- `:content-block-stop` - `:index`
- `:message-delta` - `:stop-reason`
- `:message-stop`

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

When you want the assembled result instead of raw events, `stream-message` fires
the same events but returns the **fully reconstructed** response map - all content
blocks, tool `:input`, `:usage`, `:stop-reason`, and `:parsed` when
`:response-format` is set: the same shape `create-message` returns. It reassembles
streamed tool calls for you, so there's no accumulating `:input-json-delta`
`:partial-json` per `:index` by hand.

```clojure
(anthropic/stream-message
  client
  {:max-tokens 256 :messages [{:role :user :content "What's the weather?"}]
   :tools [weather-tool]}
  (fn [ev] (when (= :text-delta (:type ev)) (print (:text ev)))))
;; => {:id ... :content [{:type :tool-use :input {...}} ...] :usage {...}}
```

## Concurrency

This wrapper uses the SDK's blocking client. There is deliberately no async
namespace because the SDK async client returns `CompletableFuture`s, which are
awkward to thread through Clojure. Use `future`/`deref`, `pmap`, or `pcalls` for
ordinary concurrency.

On JDK 21+, a virtual-thread executor supports high concurrency: blocking calls
parked on virtual threads do not consume an OS thread.

```clojure
(with-open [executor (java.util.concurrent.Executors/newVirtualThreadPerTaskExecutor)]
  (let [tasks (mapv (fn [prompt]
                      (.submit executor ^java.util.concurrent.Callable
                               #(anthropic/create-message client {:model :claude-opus-4-8 :max-tokens 64 :messages [{:role :user :content prompt}]})))
                    prompts)]
    (mapv #(.get %) tasks)))
```

Do not put blocking calls inside a core.async `go` block: it parks the fixed
dispatch pool. Use `thread` instead. On JDK < 21, use the SDK async client
through the `:configure` seam when thousands of in-flight requests are needed.

## What's covered

- Messages: `create-message`
- Content blocks: images, PDFs, documents, citations, thinking, caching
- Tools: custom and server-side tools, `run-tools`
- Counting: `count-tokens`
- Streaming: `stream-text`, `stream`, `stream-message`
- Models: `list-models`, `get-model`
- Message Batches: create, get, list, cancel, delete, results, reduce
- Files (beta): upload, get, list, download, delete
- Beta agents platform: `anthropic.beta`
- Beta Messages API: `anthropic.beta.messages`

Async clients, raw-response accessors, and per-call `RequestOptions` are
transport and accessor variants reached through the `:configure` seam and the
`opts`/`:include-response` args, not duplicated as fns. For anything unwrapped,
use the [Java SDK](https://github.com/anthropics/anthropic-sdk-java).

### Beta Messages

`anthropic.beta.messages` supports fallback params, dynamic tool changes, and
beta-only server tools. `run-beta-tools` accepts `:on-turn`, called with
`(response params)` after each assistant turn; its returned params control the
next iteration.

## Errors

All failures throw `ex-info` keyed `:anthropic/error` in `ex-data`:

- Request-shaping errors (bad tool spec, missing key) throw before any network
  call, with an error keyword describing the problem.
- API failures carry `{:anthropic/error :api-error :status <http status>
  :error-type <kw>}` where `:error-type` is one of `:bad-request`,
  `:unauthorized`, `:permission-denied`, `:not-found`,
  `:unprocessable-entity`, `:rate-limit`, `:internal-server`, or
  `:unexpected-status`. The original SDK exception is preserved as
  `(ex-cause e)`.
- Network/IO failures carry `{:anthropic/error :io-error}`, original exception
  as cause.

Other SDK exceptions (e.g. `AnthropicInvalidDataException`) propagate
unchanged.

## Beta agents platform

`anthropic.beta` wraps the beta agents-platform APIs with the same
maps-in/maps-out shape and error contract as `anthropic.core`:

- skills (and skill versions)
- memory stores (and memories)
- agents (including multiagent rosters)
- agent versions
- sessions (events, threads, and resources), with budgets and usage
- thread events
- deployments (and runs)
- environments and the self-hosted work queue (retrieve/update/list,
  ack/heartbeat/poll/stats/stop)
- vaults and vault credentials
- dreams
- tunnels and tunnel certificates
- memory versions
- user profiles
- webhook payload parsing

```clojure
(require '[anthropic.beta :as beta])

(def agent (beta/create-agent
            client
            {:name "helper"
             :model "claude-opus-4-8"
             :effort :high
             :system "be helpful"
             :skills [{:type :anthropic :skill-id "skill_123" :version "2"}]}))

(def session (beta/create-session
              client
              {:agent (:id agent) :title "run 1"
               :initial-events [{:type :user-message :content "hello"}]}))
(beta/send-session-events client (:id session)
                          [{:type :user-message :content "hello"}])
(beta/unwrap-webhook client payload) ;; parse a webhook payload string
(beta/stream-session-events client (:id session) {}
                            (fn [ev] (println (:type ev))))
```

Webhook parsing covers agent, deployment, session, environment, and memory-store
events. `stream-session-events` and `stream-thread-events` open SSE streams over
the blocking client (event maps keyed by `:type`, e.g. `:agent-message`,
`:session-status-running`), matching `anthropic.core`'s `stream`.

Beta endpoints may still change.

## Bedrock and Vertex

The SDK ships separate backend artifacts,
[`com.anthropic/anthropic-java-bedrock` and
`com.anthropic/anthropic-java-vertex`](https://github.com/anthropics/anthropic-sdk-java#amazon-bedrock-and-google-vertex-ai),
for Amazon Bedrock and Google Vertex AI. `client` here builds the direct-API
client, but its `:configure` option can set a `.backend(...)` on the SDK builder,
and every function takes the client as its first argument - so an
`AnthropicClient` built from either backend artifact works with all of them.

## Tests

Unit tests (the request/response translation) run with no network:

```
clojure -M:test
```

The `:integration` suite hits the live API and is billed - it needs
`ANTHROPIC_API_KEY` and is run explicitly:

```
ANTHROPIC_API_KEY=sk-... clojure -M:test --focus-meta :integration
```

## License

Copyright © 2026 Savyasachi

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/).
The wrapped `com.anthropic/anthropic-java` SDK is MIT-licensed and remains the
property of Anthropic.
