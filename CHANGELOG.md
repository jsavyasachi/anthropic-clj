# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.6.1] - 2026-06-18

### Changed
- Bump `com.anthropic/anthropic-java` 2.40.1 -> 2.42.0.
- `:code-execution` now maps to the newest `code_execution_20260521` tool,
  which adds `:allowed-callers` support (the older `20260120` had none).

## [0.6.0] - 2026-06-14

### Added
- Text-block `:citations` parsing (char / page / content-block / web-search /
  search-result locations), each with `:cited-text`.

### Notes
- This release completes the **GA** Messages surface. The beta API
  (`beta.messages`, MCP connectors, `file_id` content, webhooks, the Managed
  Agents platform) is a separate parallel surface and is intentionally out of
  scope - use the official Java SDK for those.

## [0.5.0] - 2026-06-14

### Added
- Server-side tools by `:type` (latest version of each): `:web-search`,
  `:web-fetch`, `:code-execution`, `:bash`, `:text-editor`, `:memory`, with
  config (domains, max-uses, user-location, allowed-callers, max-characters,
  max-content-tokens). `->tool` now returns a `ToolUnion`.
- Response parsing for `:server-tool-use` and the server-tool result blocks
  (web-search/web-fetch/code-execution/bash/text-editor/tool-search/
  container-upload) plus `:redacted-thinking`.

## [0.4.0] - 2026-06-14

### Added
- Content blocks: `:image` (base64/url) and `:document` (base64/url/plain-text
  PDF, with `:title`/`:context`) for vision and document input.
- `:cache-control` on any content block (ephemeral, optional `:ttl`) for
  prompt-cache breakpoints.
- Files API (beta): `upload-file`, `get-file`, `list-files`, `download-file`,
  `delete-file`.

## [0.3.0] - 2026-06-14

### Added
- Structured output: `create-message` accepts `:response-format` (a JSON Schema
  map) and `:effort` (`:low`…`:max`); responses with a format carry `:parsed`.
- Models API: `list-models` (paged) and `get-model`.
- Message Batches: `create-batch`, `get-batch`, `list-batches`, `cancel-batch`,
  `delete-batch`, and `batch-results` (succeeded results carry the parsed
  `:message`). Batch requests reuse the `create-message` request shape.

### Dependencies
- Add `metosin/jsonista` for decoding structured-output JSON.

## [0.2.0] - 2026-06-14

### Added
- `count-tokens` - count a request's input tokens without sending it.
- `stream` - surfaces every normalized stream event (message and content-block
  lifecycle, plus text/thinking/input-json/signature deltas), returning the full
  text. `stream-text` is now a thin convenience over it.
- `create-message` request controls: `:temperature`, `:top-p`, `:top-k`,
  `:stop-sequences`, `:tool-choice`, `:thinking`, `:metadata`, `:service-tier`.

### Changed
- `:usage` now includes `:cache-creation-input-tokens` /
  `:cache-read-input-tokens` when the response reports prompt caching.

## [0.1.1] - 2026-06-14

### Changed
- Standardize README structure and badges (docs only).

## [0.1.0] - 2026-06-14

Initial release: an idiomatic Clojure wrapper over the official Anthropic Java
SDK (`com.anthropic/anthropic-java` 2.40.1).

### Added
- `anthropic.core/client` - construct a client (env `ANTHROPIC_API_KEY` or
  explicit `:api-key`).
- `anthropic.core/create-message` - Clojure request map to response map, with
  `:model`/`:max-tokens`/`:system`/`:messages`/`:tools`, parsed content blocks
  (text, thinking, tool_use), stop-reason, and usage.
- `anthropic.core/stream-text` - stream a request, invoking a callback per text
  delta and returning the full text.
- Tool use: tools as Clojure maps, parsed `tool_use` blocks (input keywordized),
  and `:tool-result` / assistant-echo block content to complete the agentic loop.
- Reflection-clean; CI across JDK 11/17/21 and Clojure 1.10/1.11/1.12.
