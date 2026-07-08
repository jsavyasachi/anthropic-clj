# Batches, Files & Structured Output

Message Batches, Files, and structured-output message calls hit Anthropic APIs
and are billed according to Anthropic's billing rules for those APIs. Keep live
calls in explicit integration tests or application code.

## Message Batches

`create-batch` submits multiple Messages requests. Each item has a
`:custom-id` and `:params`. `:params` uses the same map shape as
`create-message`, except per-request `:service-tier` is not supported.

```clojure
(require '[anthropic.core :as anthropic])

(def batch
  (anthropic/create-batch
    client
    [{:custom-id "summary-1"
      :params {:model "claude-haiku-4-5"
               :max-tokens 64
               :messages [{:role :user :content "Summarize Clojure in one sentence."}]}}
     {:custom-id "summary-2"
      :params {:model "claude-haiku-4-5"
               :max-tokens 64
               :messages [{:role :user :content "Summarize Java in one sentence."}]}}]))

batch
;; => {:id "msgbatch_..."
;;     :processing-status :in-progress
;;     :request-counts {:processing 2
;;                      :succeeded 0
;;                      :errored 0
;;                      :canceled 0
;;                      :expired 0}
;;     :created-at "..."
;;     :expires-at "..."}
```

Batch maps contain:

- `:id`
- `:processing-status`
- `:request-counts`
- `:created-at`
- `:expires-at`
- `:ended-at` when available
- `:results-url` when available

Poll with `get-batch` until `:processing-status` is `:ended`.

```clojure
(anthropic/get-batch client (:id batch))
```

Other batch helpers:

```clojure
(anthropic/list-batches client)
(anthropic/cancel-batch client (:id batch))
(anthropic/delete-batch client (:id batch))
```

When the batch has ended, fetch results:

```clojure
(anthropic/batch-results client (:id batch))
;; => [{:custom-id "summary-1"
;;      :result {:type :succeeded
;;               :message {:id "msg_..."
;;                         :role :assistant
;;                         :content [...]
;;                         :usage {...}}}}
;;     {:custom-id "summary-2"
;;      :result {:type :errored}}]
```

Result `:type` values are:

- `:succeeded`
- `:errored`
- `:canceled`
- `:expired`
- `:unknown`

For large result sets, use `reduce-batch-results` to avoid retaining the full
collection. The underlying results stream is closed automatically.

```clojure
(anthropic/reduce-batch-results
  client
  (:id batch)
  (fn [acc result]
    (conj acc (:custom-id result)))
  [])
```

## Files

The Files helpers use Anthropic's beta Files API through `anthropic.core`.

```clojure
(def uploaded
  (anthropic/upload-file client "paper.pdf"))

uploaded
;; => {:id "file_..."
;;     :filename "paper.pdf"
;;     :mime-type "application/pdf"
;;     :size-bytes 12345
;;     :created-at "..."}
```

`upload-file` accepts:

- path string
- `java.io.File`
- `java.nio.file.Path`
- `java.io.InputStream`
- byte array

Helpers:

```clojure
(anthropic/get-file client (:id uploaded))
(anthropic/list-files client)
(anthropic/delete-file client (:id uploaded))
```

`get-file` and `list-files` return metadata maps like `upload-file`, plus
`:downloadable` when the API reports it.

`download-file` returns a byte array and closes the HTTP response
automatically:

```clojure
(def bytes
  (anthropic/download-file client "file_..."))
```

Downloaded contents are only available for downloadable files. User uploads may
not be downloadable.

## Structured Output

Pass `:response-format` as a JSON Schema map. The wrapper sends it as the
Messages API output config and adds `:parsed` to the returned response by
decoding the first text block as JSON with keyword keys.

```clojure
(def response
  (anthropic/create-message
    client
    {:model "claude-haiku-4-5"
     :max-tokens 256
     :response-format {:type "object"
                       :properties {:capital {:type "string"}}
                       :required ["capital"]
                       :additionalProperties false}
     :messages [{:role :user
                 :content "What is the capital of France?"}]}))

(:parsed response)
;; => {:capital "Paris"}
```

When `:response-format` is present, `create-message` parses the first
`:content` block with `{:type :text}`. If there is no text block, `:parsed` is
`nil`. If the text is not valid JSON, JSON parsing throws.

You can also pass `:effort` with or without `:response-format`:

```clojure
(anthropic/create-message
  client
  {:model "claude-haiku-4-5"
   :max-tokens 256
   :effort :low
   :messages [{:role :user :content "Answer briefly."}]})
```

## Multimodal Input and Cache Control

Message content can be a string or a vector of content blocks. Supported input
block types include:

- `:text`
- `:image`
- `:document`
- `:search-result`
- `:thinking`
- `:redacted-thinking`
- `:container-upload`
- `:tool-use`
- `:tool-result`

Image sources support:

- `{:type :base64 :media-type "image/png" :data "..."}`
- `{:type :url :url "https://example.com/image.png"}`

Document sources support:

- `{:type :base64 :data "..."}`
- `{:type :url :url "https://example.com/file.pdf"}`
- `{:type :text :data "plain text"}`

Blocks that support prompt caching accept `:cache-control`. The top-level
request also accepts `:cache-control`.

```clojure
{:role :user
 :content [{:type :image
            :source {:type :base64
                     :media-type "image/png"
                     :data "<base64>"}}
           {:type :document
            :source {:type :text :data "reference text"}
            :title "Reference"
            :cache-control {:ttl :1h}}
           {:type :text
            :text "Summarize the image and document."
            :cache-control true}]}
```

`:cache-control true`, `:cache-control :ephemeral`, and
`:cache-control {:ttl :5m}` use ephemeral caching. `{:ttl :1h}` requests a
one-hour TTL where the API supports it.

The README has a broader example covering images, PDFs, search-result blocks,
thinking round trips, container uploads, server tools, and the beta agents
platform pointer.
