# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.26.0] - 2026-08-19

### Changed

- Bump `com.anthropic/anthropic-java` (and the Bedrock and Vertex modules) to
  2.55.0.

### Added

- User-profile `:access-type` (`:application` or `:passthrough`) on
  `create-user-profile` and `update-user-profile`, and in the returned profile
  map.
- Memory-version `:created-by` in the memory-version map, covering all actor
  variants: `:api`, `:user`, `:service-account`, and `:session`.

### Removed

- The `:mid-conversation-system` content block. Upstream removed the
  `mid_conv_system` block from the Messages API in 2.55.0, so it is no longer a
  supported content-block type.

### Fixed

- User-profile `:relationship` and environment `:description` response mapping
  now handle the `Optional` return types these accessors adopted in
  anthropic-java 2.55.0.

## [0.25.0] - 2026-08-13

### Added
- `:output-type` on `create-message` and `count-tokens`, in both the stable and
  the beta namespaces. It takes a Java `Class` and builds the schema from that
  class. Combine it with `:effort`, or use `:response-format` as before.
- `:output-behavior` on `create-dream`, either `{:type :create-new}` or
  `{:type :update-existing :memory-store-id "..."}`. Dream maps carry the same
  key.
- `:base-url` on `vertex-client`.

### Changed
- Track `com.anthropic/anthropic-java` 2.54.0.
- The test matrix covers Clojure 1.10, 1.11, and 1.12.

## [0.24.1] - 2026-08-13

### Changed
- Docstrings and prose documentation rewritten in Simplified Technical English.
  No behavior change.

## [0.24.0] - 2026-08-08

### Removed
- Breaking: `create-completion` and `stream-completion`. Anthropic has withdrawn
  `/v1/complete`, which now returns HTTP 400 pointing callers at `/v1/messages`.
  Use `create-message` and `stream-message`.

## [0.23.2] - 2026-08-08

### Fixed
- The library compiles on Clojure 1.10 and 1.11 again. Ten enum helpers passed a
  static method as a bare value, which only Clojure 1.12 accepts, so requiring
  `anthropic.beta` threw "Unable to find static field: of" on older Clojure. The
  test matrix caught it; the default 1.12 toolchain did not.

## [0.23.1] - 2026-08-08

First release verified against the live API rather than against the SDK jar alone.

### Fixed
- Beta content blocks report their `:type` as a keyword, matching the stable path.
  Only the top-level type was converted before, so a block arrived with a string
  type and code dispatching on `(= :text (:type block))` silently stopped matching
  when a request moved to the beta API. A tool call's `:input` is left as-is: it is
  caller-defined JSON where a `type` key is data, not a discriminator.

### Changed
- `create-completion` and `stream-completion` are documented as retired. Anthropic
  has withdrawn the `/v1/complete` endpoint, which now answers every request with a
  400 surfaced as `:anthropic/error :api-error`. The functions remain so the SDK
  surface stays covered, but they cannot succeed. Use `create-message` and
  `stream-message`.

## [0.23.0] - 2026-08-07

Closes every known parity gap against `anthropic-java` 2.53.0. Every non-deprecated
operation, request field, and response field the SDK exposes is now reachable through
a Clojure-shaped map. This was verified by repeated independent audits of the SDK jar
rather than asserted: the final audit reports no unreachable operation and no dropped
field.

### Added
- Every list operation takes an options map exposing the filters, pagination, ordering,
  and beta headers the SDK offers, and each still works with no options. This covers
  skills, skill versions, memory stores, agents, sessions, deployments, deployment runs,
  environments, vaults, tunnels, tunnel certificates, dreams, vault credentials, user
  profiles, message batches, and files.
- `:betas` on agent, session, deployment, environment, vault, model-list, and file
  operations, accepting strings or keywords.
- Server-tool specs take an optional `:version` selecting a dated variant of that tool
  family, on both the stable and beta paths. Omitting it keeps the latest variant, so
  existing specs are unchanged.
- Beta tool builders reach their full option sets, including computer-use input examples,
  advisor caching, web search and web fetch response inclusion, and eager input streaming.
- Custom tools accept `:eager-input-streaming` and `:input-examples` on both paths.
- Beta output config accepts `:task-budget`.
- Session create accepts `:resources` and `:vault-ids`; session update accepts `:agent`
  and `:vault-ids`. Resource specs accept `:checkout`, `:access`, and `:instructions`.
- User profiles accept `:relationship`.
- Model config carries `:speed` in and out, and session agents carry their effort,
  inference geo, and speed.
- Response maps carry every field the SDK returns, including session agent, metadata,
  outcome evaluations, resources, stats, vault ids, and deployment id; thread
  `:startup-seconds`; and the `:type` and `:metadata` of skills, memories, agents,
  dreams, credentials, profiles, tunnels, and delete responses.
- Session events, webhook payloads, agent tools, and message content map every variant
  the SDK can send instead of collapsing unrecognized ones. Guards assert every variant
  of every mapped union has a branch, so a variant added by a future SDK release fails
  the suite rather than silently mapping to unknown.
- The beta model service, through `list-beta-models` and `get-beta-model`. It is not the
  stable model service: it reports the allowed fallback models the stable one does not.
- Citations on system, text, document, and search-result blocks, on both paths.
- Token counting takes every param message creation takes.
- Tool use blocks carry their `:caller`, and session event send responses carry each
  variant's typed payload.
- Enum-valued response fields are keywords throughout, matching how the rest of the
  library represents them. Model ids stay strings: they are opaque identifiers the API
  accepts back verbatim, not a closed set.

### Added, earlier in this release
- Stable web fetch accepts `:use-cache` and `:citations`, matching the shapes the
  beta path already took.
- Tool choice accepts `:disable-parallel-tool-use` on the auto, any, and tool
  variants, on both the stable and beta paths. Tool choice `:none` has no parallel
  tool use to disable and reports `:unsupported-disable-parallel-tool-use` rather
  than dropping the key.
- Beta message creation accepts `:context-management`, `:diagnostics`, `:speed`,
  and `:output-format`. `:output-format` sets the top-level output format, which is
  a distinct wire field from the format carried inside `:response-format`'s output
  config.
- Deployment create and update accept `:resources` and `:schedule`, and the
  deployment map carries back resources, schedule, initial events, metadata, and
  type.
- Environment create and update accept `:config`, as either a cloud or a
  self-hosted config, and `:scope`. The environment map carries back all three.
- Vault maps carry `:metadata` and `:type`.

### Fixed
- A deployment resource missing a field the API requires now reports
  `:anthropic/error :missing-key` with the offending key, instead of failing with a
  null pointer raised inside the SDK.
- Beta tool choice `{:type :none}` builds the none variant. It previously returned nil,
  which dropped the tool choice from the request without an error.
- A beta `:tool-search` tool with an unknown or missing `:variant` reports
  `:anthropic/error :unsupported-tool-search-variant` instead of escaping as a raw
  `IllegalArgumentException`.

## [0.22.0] - 2026-08-07

### Added
- Beta Messages server tools. A tool spec with a `:type` now builds the matching
  `BetaToolUnion` variant instead of collapsing to a custom tool, so web search,
  web fetch, code execution, bash, text editor, memory, and both tool-search
  variants work on the beta path, alongside the beta-only computer-use, advisor,
  and MCP toolset tools. Tool specs take the same shape on the stable and beta
  paths, so a spec moves between `create-message` and `create-beta-message`
  unchanged. Token counting dispatches the same way.
- Beta tool options that were previously unreachable: `:max-uses`,
  `:max-content-tokens`, `:use-cache`, `:citations`, `:allowed-domains`,
  `:blocked-domains`, and `:user-location` on the server tools, plus
  `:defer-loading`, `:strict`, and `:allowed-callers` on custom beta tools.

## [0.21.0] - 2026-08-07

### Added
- The legacy Text Completions API: `create-completion` and `stream-completion`.
  This was the one non-deprecated SDK operation the wrapper did not reach, so
  idiomatic parity is now complete. Its sampling controls (`:temperature`,
  `:top-k`, `:top-p`) are deprecated by the SDK for models released after Claude
  Opus 4.6 and are wrapped for parity within the operation. Use `create-message`
  for new work.

## [0.20.0] - 2026-08-07

### Added
- Track `com.anthropic/anthropic-java` 2.53.0.
- Session and deployment budgets. `create-session`, `update-session`,
  `create-deployment`, and `update-deployment` accept `:budget`, shaped as
  `{:max-list-cost {:amount "1.25" :currency :usd} :type :limit}`, and the
  session, deployment, and session-updated event maps carry it back.
- `:usage` on session and session-thread maps, including `:list-cost`,
  `:active-seconds`, and `:server-tool-use` web fetch and web search counts.
- `:inference-geo` on agent create and update, and on the agent map.
- Multiagent rosters. `create-agent` and `update-agent` accept `:multiagent`,
  and the agent map carries it back. Roster entries cover a bare agent id, an
  agent reference with an optional version, self, and an advisor.
- The `session_usage` session event, the redacted content block, and the
  `session.budget_reached` webhook.
- `:created-at` and `:updated-at` on session resources.

### Changed
- Agent slots report `:type`: either `:agent` with an id and version, or
  `:advisor` with a model. This follows the SDK moving each slot behind a union.
- `list-memories` returns memory list items rather than memories, so each entry
  carries `:path` and a `:type` of `:memory` or `:memory-prefix`.

## [0.19.0] - 2026-07-24

### Added
- Track `com.anthropic/anthropic-java` 2.52.0, including the `:claude-opus-5` alias.
- Beta fallback request params: `:fallbacks` and `:fallback-credit-token`.
- Beta `:tool-addition` and `:tool-removal` request blocks.
- Dynamic tool changes in `run-beta-tools` via `:on-turn`.

## [0.18.1] - 2026-07-23

### Changed
- Track `com.anthropic/anthropic-java` 2.51.0, which adds the
  `model_context_window_exceeded` stop reason surfaced as
  `:model-context-window-exceeded`.

## [0.18.0] - 2026-07-22

### Added
- `anthropic.beta.messages`: the beta Messages API, completing idiomatic parity with the SDK. `create-beta-message`, `count-beta-tokens`, `run-beta-tools` (a native agentic tool loop), the beta message batches (`create-beta-batch`, `get-beta-batch`, `list-beta-batches`, `cancel-beta-batch`, `delete-beta-batch`, `beta-batch-results`, `reduce-beta-batch-results`), and streaming (`stream-beta-message`, `stream-beta-text`). Request maps mirror `create-message` plus `:betas` and `:mcp-servers`; responses are converted generically. Structured output (`:response-format` -> `:parsed`) is supported.

## [0.17.0] - 2026-07-22

### Added
- Beta agents-platform parity: `add-session-resource`, `redact-memory-version`, `reveal-tunnel-token`, `rotate-tunnel-token`, and `mcp-oauth-validate-vault-credential`.

## [0.16.0] - 2026-07-22

### Added
- `stream-session-events` and `stream-thread-events`: SSE streaming of beta session and thread events (blocking client, `on-event` callback, returns the event vector), with the `:event-deltas` filter.

## [0.15.0] - 2026-07-22

### Added
- Managed-agent model `:effort`, initial session events, and agent-update `:version` support.
- Environment and memory-store webhook events: environment created, updated, archived, and deleted; memory store created, archived, and deleted.

### Changed
- Track `com.anthropic/anthropic-java` 2.50.0.
- Environment deletion maps now include `:type`; self-hosted work maps now include `:secret`.

## [0.14.1] - 2026-07-21

### Changed
- Resolved the accidental reflection warnings in message batch params and tool-builder configuration (typed dispatch, no behavior change). The remaining reflection in the bedrock/vertex client builders is deliberate, so those backends stay optional dependencies.

## [0.14.0] - 2026-07-21

### Added
- Named model keyword aliases in `anthropic.core/models`: `:model` now accepts
  a keyword or a string.
- Beta agents-platform closure: dreams, tunnels and certificates, vault
  credentials, environment self-hosted work (retrieve/update/list,
  ack/heartbeat/poll/stats/stop), session resources, thread events, agent
  versions, and memory versions.

### Changed
- Track `com.anthropic/anthropic-java` 2.49.1. Its only stable-surface change,
  the `general_harms` refusal category, already flows through the refusal
  stop-details map.
- Document the concurrency stance: blocking client, Clojure-native concurrency,
  and no async namespace.

## [0.13.0] - 2026-07-16

### Added
- Closed the stable-surface parity gaps against the Anthropic Java SDK. All changes are backward compatible.
- Lossless content-block round-trip: server-tool-use / server-tool-result inputs and outputs, and document content sources (text/base64/url/file-id/content) with document citations.
- Complete stable tool configuration: per-tool `allowed-callers`, `cache-control`, `defer-loading`, and `strict` options on custom and server tools.
- Full streaming payloads: `stream`/`stream-message` events now surface message-start data, message-delta usage/container/stop-reason/stop-sequence/stop-details, content-block start/delta (text, thinking + signature, input-json, citation deltas), and content-block-stop.
- Message Batch request fidelity: batch requests carry the same params as `create-message` (cache-control, container, inference-geo, service-tier, structured system blocks).
- Model capability metadata on `list-models`/`get-model`, plus `ModelListParams` options.
- Richer error normalization: the `:anthropic/error` ex-info now includes service headers, body, request-id, SDK error type, and a finer error classification, while keeping the existing keys.
- `bedrock-client` and `vertex-client` constructors backed by optional `:bedrock` / `:vertex` deps.edn aliases (not pulled by base users).

### Changed
- Track `com.anthropic/anthropic-java` 2.49.0. (2.49.0's own additions (the dreams and MCP tunnels beta services) remain unwrapped.)

## [0.12.2] - 2026-07-12

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
