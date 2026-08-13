# Contributing to anthropic-clj

You can report bugs and submit fixes or focused feature contributions to
`anthropic-clj`.

## Before you start

- For anything more than a trivial fix, **open an issue first**. We can then
  agree on the approach before you do the work.
- Read the open issues and pull requests first. Do not duplicate work.

## Development

This is a Clojure library. You need a JDK and [Leiningen](https://leiningen.org/).
A project that uses `deps.edn` uses the Clojure CLI instead: see the README.

```bash
lein test     # run the test suite
lein check    # AOT-compile; must be free of reflection warnings
```

Requirements for a mergeable change:

- **Tests first.** Add or update the tests for the behavior that you change.
  For a bug fix, add a regression test. It must fail before your fix and pass
  after it.
- **Green build.** `lein test` passes and `lein check` reports **zero**
  reflection warnings.
- **One change only.** Keep each pull request to one logical change.

## Commits and pull requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` …).
- Keep the subject in the imperative mood and under ~72 characters.
- Update `CHANGELOG.md` when your change is user-visible.
- Rebase on the latest `main` before opening the pull request.

## License

By contributing, you agree that your contributions will be licensed under the
same license as this project (see `LICENSE` / the README).
