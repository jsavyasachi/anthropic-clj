# Examples

These plain `.clj` files are small, copy-paste REPL recipes for `anthropic-clj`.
They are not a separate `deps.edn` project and add no dependencies.

## Index

- [`client_setup.clj`](client_setup.clj) - environment authentication and client options
- [`messages.clj`](messages.clj) - a basic Messages API request
- [`beta_tool_use.clj`](beta_tool_use.clj) - beta Messages with `run-beta-tools`
- [`streaming.clj`](streaming.clj) - text streaming and normalized events
- [`structured_output.clj`](structured_output.clj) - JSON Schema output and `:parsed`
- [`files.clj`](files.clj) - beta Files metadata, listing, and deletion

## REPL use

From the repository root, start a REPL with the library's normal classpath:

```sh
clojure -M
```

Set `ANTHROPIC_API_KEY` in the environment before creating a client. In the
REPL, load an example with `(load-file "examples/messages.clj")`, then evaluate
the forms inside its `(comment ...)` block. Each file has its own namespace;
the `client` definition is intentionally local to that example. The Files
example requires replacing its placeholder path with a local file.
