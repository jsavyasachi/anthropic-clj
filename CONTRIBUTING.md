# Contributing to anthropic-clj

Thanks for your interest in improving `anthropic-clj`. Bug reports, fixes, and
focused feature contributions are all welcome.

## Before you start

- For anything beyond a trivial fix, **open an issue first** so we can agree on
  the approach before you invest time.
- Check existing issues and pull requests to avoid duplicate work.

## Development

This is a Clojure library. You need a JDK and [Leiningen](https://leiningen.org/)
(projects that have migrated to `deps.edn` use the Clojure CLI instead — see the
README).

```bash
lein test     # run the test suite
lein check    # AOT-compile; must be free of reflection warnings
```

The bar for a mergeable change:

- **Tests first.** Add or update tests for the behavior you change; for a bug
  fix, include a regression test that fails before your fix and passes after.
- **Green build.** `lein test` passes and `lein check` reports **zero**
  reflection warnings.
- **No scope creep.** Keep each pull request to one logical change.

## Commits and pull requests

- Follow [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` …).
- Keep the subject in the imperative mood and under ~72 characters.
- Update `CHANGES.md` / `CHANGELOG.md` when your change is user-visible.
- Rebase on the latest `main` before opening the pull request.

## License

By contributing, you agree that your contributions will be licensed under the
same license as this project (see `LICENSE` / the README).
