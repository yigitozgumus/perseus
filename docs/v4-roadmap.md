# v4 Roadmap

Branch: `v4-implementation`

Potential high-value additions after the v3 navigation API expansion.

## Recommended next work

1. **CI workflow**
   - Add GitHub Actions that runs:
     - `./gradlew checkKotlinAbi`
     - `./gradlew build`
   - Protects against accidental API and build breaks.

2. **Better scope restore behavior**
   - Strengthen `ScopeRestorePolicy.NeverRestore` semantics.
   - Example: if a restored pushed scope is marked `NeverRestore`, drop it and return to the previous safe scope.

3. **Result lifecycle cleanup**
   - Add cleanup for `ResultBusAdapter` streams when handles/scopes complete.
   - Avoid long-lived unused result streams.

4. **Provider validation expansion**
   - Extend validation beyond initial root keys.
   - Consider checks for:
     - duplicate provider matches
     - graph-declared keys
     - scene key/provider mismatch

5. **More focused tests**
   - Scope result delivery test that collects an actual result.
   - `NonRestorableKey` restore truncation test.
   - Provider validation and missing-provider message tests.
   - Deep link helper tests.

6. **Deep link sample with real Android intent filter**
   - Add a manifest intent filter.
   - Demonstrate reading `intent.data` and routing through `DeepLinkResolver`.

7. **API docs / Dokka**
   - Generate public API docs for `perseus-core` and `perseus-interop`.

8. **Public API polish**
   - Revisit whether test utilities belong in the main artifact or a separate `perseus-test` module.
   - Revisit the graph builder surface if it grows beyond Compose screen registration.

## Priority order

1. CI workflow
2. Better `ScopeRestorePolicy.NeverRestore` restore semantics
3. Result bus cleanup
4. Focused tests for the new APIs
5. Provider validation expansion
