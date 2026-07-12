# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

### Changed
- Migrate the build to deps.edn and tools.build, with Leiningen supported via
  lein-tools-deps.

## [0.12.1] - 2026-07-11

Docs-only release: README tightened - streaming section reordered so
`stream-message` no longer splits `stream`'s prose from its example, and the
client-options, `create-message` controls, stream events, and beta service
lists are now bulleted. No code changes.

## [0.12.0] - 2026-07-11

### Added
- Stable client transport options for webhook keys, logging, response
  validation, proxies, default headers/query parameters, and raw builder
  configuration.
- `stream-message` for fully reconstructed streamed message maps.
- Per-call request options and optional raw HTTP response metadata on
  `create-message` and `count-tokens`.
- System text blocks, custom-tool cache control, and additional header, query,
  and body request properties for message creation and token counting.

## [0.11.1] - 2026-07-08

Docs-only release: cljdoc guide articles (Getting Started, Tool Use, Streaming,
Batches/Files/Structured Output) under doc/. No code changes.

## [0.11.0] - 2026-07-07

### Added
- `run-tools` - agentic tool-use loop over `create-message`. Tools may carry
  `:fn` (a function of the parsed tool `:input`); the loop executes every
  requested tool call per turn (parallel calls included), feeds `:tool-result`
  blocks back, and repeats until the model stops asking for tools. Options:
  `:max-iterations` (default 10; exceeding throws `:max-iterations-exceeded`)
  and `:on-message` (called with each response map). A throwing `:fn` becomes
  an `:is-error` tool result instead of aborting. Returns the final response
  map plus `:messages`, the accumulated conversation.
- `:tool-result` input blocks accept `:is-error`.

### Fixed
- `:thinking` response blocks now include `:signature`, so extended-thinking
  assistant turns replay through `:messages` without a NullPointerException
  (the API requires the signature on replayed thinking blocks).

## [0.10.0] - 2026-07-06

### Added
- `anthropic.beta` now wraps the remaining agents-platform services:
  deployments (+ runs), environments, vaults, user profiles, enrollment URLs,
  and webhook unwrapping (including the verifying arity); nested
  sub-resources: skill versions (with download-to-bytes), memories, session
  events (send/list), and session threads; agent `:skills`/`:mcp-servers`/
  `:tools` configuration.

## [0.9.0] - 2026-07-04

### Added
- New `anthropic.beta` namespace wrapping the beta agents-platform APIs:
  skills (`create-skill`/`get-skill`/`list-skills`/`delete-skill`), memory
  stores (create/get/list/update/archive/delete), agents
  (create/get/list/update/archive - update requires `:version` for
  optimistic concurrency), and sessions
  (create/get/list/update/archive/delete). Maps in/maps out, same
  `:anthropic/error` contract as `anthropic.core`. Deployments,
  environments, vaults, user profiles, webhooks, and the nested
  sub-resources are not wrapped yet.

## [0.8.0] - 2026-07-04

### Changed
- **API and I/O failures are now normalized to `ex-info`.** Service errors
  throw `ex-info` with `{:anthropic/error :api-error :status <http-status>
  :error-type <kw>}` and I/O errors with `{:anthropic/error :io-error}`; the
  original SDK exception is always `(ex-cause e)`. Callers that previously
  caught `com.anthropic.errors.AnthropicException` directly must catch
  `clojure.lang.ExceptionInfo` and inspect `ex-data`/`ex-cause` instead.
  Other SDK exceptions still propagate unchanged.

### Added
- README documents using Bedrock/Vertex-built `AnthropicClient` instances
  with this wrapper.

## [0.7.0] - 2026-07-04

### Added
- `client` accepts `:auth-token`, `:base-url`, `:timeout-ms`, and
  `:max-retries` in addition to `:api-key`.
- `create-message` accepts newer request params: `:container`,
  `:inference-geo`, `:user-profile-id`, top-level `:cache-control`,
  `:response-format`, and `:effort`; responses surface newer usage/container/
  stop-detail fields when present.
- Content block params for `:search-result`, `:thinking`,
  `:redacted-thinking`, and `:container-upload`.
- Server-side tool-search tools via
  `{:type :tool-search :variant :bm25|:regex}` for Messages and count-tokens.
- `reduce-batch-results` for streaming reduction over batch results without
  retaining the full result set.

### Changed
- `:tool-result` map/vector content is now JSON-encoded before sending instead
  of being coerced with `str`.
- `count-tokens` now maps server tools through the count-token tool union
  instead of treating server-tool specs as custom tools.

## [0.6.8] - 2026-07-03

### Changed
- Bump `com.anthropic/anthropic-java` 2.47.1 -> 2.48.0. SDK release adding the
  `agent-memory-2026-07-22` beta header for the beta memory-stores surface,
  which this wrapper does not expose; no wrapper-surface changes.

## [0.6.7] - 2026-07-02

### Changed
- Bump `com.anthropic/anthropic-java` 2.47.0 -> 2.47.1. SDK patch release
  removing an unused `MILITARY_WEAPONS` refusal-category enum value; this
  wrapper doesn't touch refusal types, so no code change.

## [0.6.6] - 2026-06-30

### Changed
- Bump `com.anthropic/anthropic-java` 2.45.0 -> 2.47.0. Purely additive on the
  wrapper's surface: SDK 2.46.0 adds the `claude-sonnet-5` model id (already
  usable today since `:model` is passed through as a plain string), and 2.47.0
  adds a new Managed Agents / Webhooks beta API surface (sessions, deployments,
  environments, memory stores) that this wrapper does not expose yet.

## [0.6.5] - 2026-06-29

### Changed
- Bump `com.anthropic/anthropic-java` 2.44.1 -> 2.45.0. The wrapper now targets
  the newest server-side tool versions: web search `20260209 -> 20260318` and
  web fetch `20260309 -> 20260318`. The Clojure tool spec (`{:type :web-search}`
  / `{:type :web-fetch}`) is unchanged.

## [0.6.4] - 2026-06-26

### Changed
- Bump `com.anthropic/anthropic-java` 2.44.0 -> 2.44.1. SDK bug-fix release only
  (Bedrock SSE transcoding uses daemon threads, a skill-creation fix, and token
  counting now accepts a User-Profile-ID); no change to the wrapper's public surface,
  and the newest server-side tool versions are unchanged.

## [0.6.3] - 2026-06-24

### Changed
- Bump `com.anthropic/anthropic-java` 2.43.0 -> 2.44.0. Additive SDK changes only
  (`system.message` streaming events, a new refusal category, and a User-Profile-ID
  request header); no change to the wrapper's public surface, and the newest
  server-side tool versions are unchanged.

## [0.6.2] - 2026-06-22

### Changed
- Bump `com.anthropic/anthropic-java` 2.42.0 -> 2.43.0. Internal SDK changes only
  (x-stainless telemetry header + refusal-fallback interceptor tagging); no change
  to the wrapper's public surface, and the newest server-side tool versions are
  unchanged.

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
