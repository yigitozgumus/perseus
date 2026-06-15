# Perseus Production Hardening Report

Date: 2026-06-15
Branch: `v5`

## Baseline

- `./gradlew test` — PASS before hardening work.
- `./gradlew test` — PASS after hardening work.
- `./gradlew lint` — PASS after hardening work.
- `./gradlew :perseus-core:publishReleasePublicationToMavenLocal :perseus-interop:publishReleasePublicationToMavenLocal` — PASS after hardening work; this matches the JitPack install command.
- Connected Android tests were not run in this environment because no device/emulator was confirmed.

## Test matrix and current coverage

| Area | Coverage added/verified | Remaining gap |
| --- | --- | --- |
| Compose-only navigation | JVM coverage for push, pop helpers, current key, replace, singleTop, popUpTo. | Add UI instrumentation for rendered host/back behavior. |
| Fragment interop | Official key/context/ViewModel helper APIs added. | Add Fragment instrumentation for arguments, duplicate keys, cleanup, recreation. |
| Dialogs | Existing sample/docs; typed result docs updated. | Add modal UI instrumentation. |
| Bottom sheets | Existing sample/docs; typed result docs updated. | Add modal UI instrumentation. |
| Tabs / `MultiStackSpec` | Existing JVM coverage for tab back and current key. | Add UI instrumentation for tab switching/recreation. |
| Process death restore | Existing JVM restore tests retained. | Add Activity recreation/saved-state instrumentation. |
| Result delivery | JVM coverage for success, cancellation, wrong type, duplicate completion, late/active observers, cleanup. | Add host-level UI result tests. |
| ViewModel scoping | Existing registry tests retained. | Add Compose/Fragment lifetime instrumentation. |
| Deep links | Existing docs/API retained. | Add resolver navigation tests. |
| Threading | JVM guard tests added through injectable main-thread checker. | Add instrumentation proving Android main thread succeeds/background thread fails. |

## Implemented phases

- Phase 2: main-thread guard for route, tab, scope, reset, pop, replacement, and result completion mutations.
- Phase 3: explicit typed one-shot result delivery with cancellation and mismatch errors.
- Phase 4: official Fragment helpers for key/context/ViewModel access.
- Phase 5: `LaunchMode.SingleTop`, `PopUpTo`, and `replaceWith` primitives.
- Phase 6: CI, JitPack config, Maven POM metadata, changelog, and README API policy.

## Deferred work

- Real Compose/Fragment instrumentation tests from Phase 1 require an emulator/device and should be implemented next.
- Binary compatibility validation/API dumps are not configured yet.
- Dokka documentation JARs are not wired into the publications yet.
- Navigation interceptors/screen-level back interception from Phase 5 remain future work.
