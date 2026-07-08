# Getting Started

`anthropic-clj` is a Clojure wrapper over the official Anthropic Java SDK.
Request maps go in, response maps come back.

Messages, model listing, and most examples here call the live Anthropic API and
are billed by Anthropic. Do not put live calls in unit tests. Use the existing
unit-test style for request/response translation tests, and reserve live calls
for an explicit integration suite.

## Installation

Leiningen:

```clojure
[net.clojars.savya/anthropic-clj "0.11.1"]
```

tools.deps:

```clojure
net.clojars.savya/anthropic-clj {:mvn/version "0.11.1"}
```

Version `0.11.1` pins `com.anthropic/anthropic-java` `2.48.0`.

## Client

```clojure
(require '[anthropic.core :as anthropic])

(def client (anthropic/client))
```

With no arguments, `client` uses the official SDK's environment lookup. For the
direct Anthropic API, set `ANTHROPIC_API_KEY`.

You can also pass explicit options:

```clojure
(def client
  (anthropic/client
    {:api-key "sk-ant-..."
     :base-url "https://api.anthropic.com"
     :timeout-ms 30000
     :max-retries 2}))
```

Supported client option keys:

- `:api-key`
- `:auth-token`
- `:base-url`
- `:timeout-ms`
- `:max-retries`

Only supplied keys are set on the SDK builder.

## First Message

```clojure
(def response
  (anthropic/create-message
    client
    {:model "claude-opus-4-8"
     :max-tokens 1024
     :system "You are concise."
     :messages [{:role :user :content "Name three primary colors."}]}))

(:role response)
;; => :assistant

(:content response)
;; => [{:type :text :text "Red, blue, yellow."}]

(-> response :content first :text)
;; => "Red, blue, yellow."
```

`create-message` takes a request map and returns a response map. The default
`:model` is `"claude-opus-4-8"` and the default `:max-tokens` is `1024`.

Common request keys:

- `:model`
- `:max-tokens`
- `:system`
- `:messages`
- `:tools`
- `:temperature`
- `:top-p`
- `:top-k`
- `:stop-sequences`
- `:tool-choice`
- `:thinking`
- `:metadata`
- `:service-tier`
- `:container`
- `:inference-geo`
- `:user-profile-id`
- `:cache-control`
- `:response-format`
- `:effort`

Response maps include:

- `:id`
- `:model`
- `:role`
- `:stop-reason`
- `:content`
- `:usage`
- `:container` when present
- `:stop-sequence` when present
- `:stop-details` when present
- `:parsed` when the request includes `:response-format`

There is no top-level `:text` convenience key. Read text from text content
blocks:

```clojure
(def text
  (->> (:content response)
       (filter #(= :text (:type %)))
       first
       :text))
```

`:usage` always includes `:input-tokens` and `:output-tokens`. Newer usage keys
are included when the API reports them, such as `:cache-creation-input-tokens`,
`:cache-read-input-tokens`, `:server-tool-use`, `:service-tier`,
`:cache-creation`, `:inference-geo`, and `:output-tokens-details`.

## Errors

API and I/O failures from the SDK are normalized to `ex-info`.

API failures:

```clojure
(try
  (anthropic/create-message client {:messages [{:role :user :content "hi"}]})
  (catch clojure.lang.ExceptionInfo e
    (ex-data e)))
;; => {:anthropic/error :api-error
;;     :status 429
;;     :error-type :rate-limit}
```

The original SDK exception is preserved as `(ex-cause e)`.

`:error-type` can be:

- `:bad-request`
- `:unauthorized`
- `:permission-denied`
- `:not-found`
- `:unprocessable-entity`
- `:rate-limit`
- `:internal-server`
- `:unexpected-status`
- `:api-error`

I/O failures carry:

```clojure
{:anthropic/error :io-error}
```

Request-shaping errors thrown by the wrapper also use `ex-info` and
`:anthropic/error`, for example `:unsupported-content-block`,
`:unsupported-server-tool`, `:unsupported-tool-search-variant`,
`:unsupported-tool-choice`, `:unsupported-thinking-type`, `:no-tool-fn`, or
`:max-iterations-exceeded`.

Other SDK exceptions can pass through unchanged.

## Models

Model helper calls hit the live API.

```clojure
(anthropic/list-models client)
;; => [{:id "claude-opus-4-8"
;;      :display-name "Claude Opus 4.8"
;;      :created-at "2026-..."
;;      :max-input-tokens 200000
;;      :max-tokens 64000} ...]

(anthropic/get-model client "claude-opus-4-8")
;; => {:id "claude-opus-4-8" :display-name "Claude Opus 4.8" ...}
```

`list-models` follows pages automatically and returns a vector.
