# Changelog

All notable changes to Perseus will be documented in this file.

Perseus follows semantic versioning once a stable `1.0.0` API is published. Before `1.0.0`, minor versions may include source or binary breaking API changes.

## 0.1.0-SNAPSHOT

### Added

- Main-thread enforcement for navigation mutations.
- Typed one-shot result APIs with explicit `Success` and `Cancelled` states.
- Result cancellation when entries/scopes are removed before sending a result.
- `LaunchMode.SingleTop`, `PopUpTo`, and `replaceWith` navigation primitives.
- Official Fragment helpers for `requirePerseusKey` and `perseusViewModels`.
- CI workflow covering build, unit tests, lint, and a JitPack-compatible publish dry run.
- JitPack build configuration for core and interop release publications.
- Maven POM metadata for core and interop artifacts.

### Changed

- Deprecated `NavigationHandle.observeResult()` in favor of typed result completion APIs.

### Fixed

- Back behavior policies now install a host-level back handler at stack roots, so root blocking and tab-root switching work even when `NavDisplay` has no entry to pop.
