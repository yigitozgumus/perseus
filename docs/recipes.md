# Recipes and development

## Sample recipes

The sample app contains focused recipes for Perseus features:

| Recipe | Demonstrates |
| --- | --- |
| Compose Only | Basic `SingleStackSpec` Compose navigation |
| Tabs / MultiStack | Independent tab back stacks and tab reset |
| Fragment Interop | Fragment screens rendered inside Perseus |
| Result Passing | `NavigationHandle`, `LocalNavigationContext`, `sendResult` |
| Dialog | `DialogKey` scenes |
| Bottom Sheet | `BottomSheetKey` scenes |
| Hide Bottom Bar | `NavigationKey.hidesBottomNavigation` |
| Custom Sheet | Custom sheet-style UI |
| Animations | Host-level transitions |
| Per-Transition | Per-navigation transitions |
| Stack Scopes | `PerseusScopeNavigator`, root replacement, pushed scopes |
| Process Death Restore | Manual saved-state restore flow |
| Group Pop | `GroupName` + `popUntil` |
| ViewModel Lifetime | Entry-scoped ViewModel stores |
| Back Behavior Policy | Root and tab back behavior controls |
| Scope Results | `pushScopeForResult` and `removeScope(result)` |
| Navigation Helpers | Pop helpers, graph builder, and provider validation |
| Restore Guards | `NonRestorableKey` and `ScopeRestorePolicy` |
| Full Demo | Multi-feature sample |

Run the `sample` app and open the recipe picker to explore them.

---

## Development notes

### API documentation

Generate Dokka HTML documentation for the public library modules:

```bash
./gradlew :perseus-core:dokkaGenerateHtml :perseus-interop:dokkaGenerateHtml
```

Generated documentation is written under each module's `build/dokka/` directory.

### Versioning and API policy

- Releases use semantic versioning after `1.0.0`.
- Before `1.0.0`, minor versions may include source or binary breaking API changes.
- Prefer additive APIs; deprecate old APIs before removal when practical.
- Experimental APIs should be documented as experimental in KDoc and release notes.
- Public changes should be recorded in `CHANGELOG.md`.

### Verification

Useful verification commands:

```bash
./gradlew :perseus-core:testDebugUnitTest --console=plain
./gradlew :perseus-interop:compileDebugKotlin :sample:compileDebugKotlin --console=plain
./gradlew :perseus-core:publishToMavenLocal :perseus-interop:publishToMavenLocal --console=plain
./gradlew check --console=plain
```
