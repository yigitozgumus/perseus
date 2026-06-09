# v4 Roadmap

Branch: `v4-implementation`

Potential high-value additions after the v3 navigation API expansion.

## Recommended next work

1. **CI workflow**
   - Skipped for this pass by request.
   - Future workflow should run:
     - `./gradlew checkKotlinAbi`
     - `./gradlew build`

2. ✅ **Better scope restore behavior**
   - Strengthened `ScopeRestorePolicy.NeverRestore` semantics.
   - Restored pushed scopes marked `NeverRestore` are dropped, returning to the previous safe scope.

3. ✅ **Result lifecycle cleanup**
   - Added cleanup for consumed `ResultBusAdapter` streams.
   - Avoids long-lived unused result streams after pending results are delivered.

4. ✅ **Provider validation expansion**
   - Validation now runs for navigated keys and replacement/pushed scopes when enabled.
   - Added duplicate provider match detection.
   - Added scene-provider mismatch validation for non-scene keys.

5. ✅ **More focused tests**
   - Added scope result delivery test that collects an actual result.
   - Added `NonRestorableKey` restore truncation test.
   - Added `ScopeRestorePolicy.NeverRestore` pushed-scope restore test.
   - Added provider validation tests for missing and duplicate providers.
   - Added result stream cleanup test.

6. **Deep link sample with real Android intent filter**
   - Add a manifest intent filter.
   - Demonstrate reading `intent.data` and routing through `DeepLinkResolver`.

7. **API docs / Dokka**
   - Generate public API docs for `perseus-core` and `perseus-interop`.

8. **Public API polish**
   - Revisit whether test utilities belong in the main artifact or a separate `perseus-test` module.
   - Revisit the graph builder surface if it grows beyond Compose screen registration.

## Priority order

1. CI workflow (remaining; intentionally skipped in this pass)
2. ✅ Better `ScopeRestorePolicy.NeverRestore` restore semantics
3. ✅ Result bus cleanup
4. ✅ Focused tests for the new APIs
5. ✅ Provider validation expansion
