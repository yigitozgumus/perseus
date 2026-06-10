# Perseus Simplification & Hardening Implementation Plan

Date: 2026-06-10  
Scope: `perseus-core`, `perseus-interop`, and targeted sample/instrumentation tests.  
Source: simplicity/risk audit and test-validation review.

## Goal

Make Perseus' existing behavior more explicit, safer, and easier to verify before adding new navigation features.

The core mental model to preserve:

> **RouterKey describes a destination. Providers render keys. Scopes own stacks.**

This plan intentionally avoids a large rewrite. Each phase is small, testable, and independently shippable.

## Non-goals

- Do not redesign the public navigation model.
- Do not add new navigation features.
- Do not replace AndroidX Navigation3.
- Do not migrate fragment interop back into core.
- Do not remove public aliases immediately unless API stability policy allows it.

## Current validation baseline

Run before and after each phase:

```bash
./gradlew :perseus-core:testDebugUnitTest :perseus-interop:testDebugUnitTest :sample:testDebugUnitTest
```

Run when Android/instrumentation tests are added:

```bash
./gradlew :perseus-core:connectedDebugAndroidTest :perseus-interop:connectedDebugAndroidTest :sample:connectedDebugAndroidTest
```

---

# Phase 1 — Make detached/pre-host behavior explicit

## Problem

`PerseusNavigationStateHolder` buffers only `setRootScope`, while other public methods have inconsistent behavior before `PerseusNavHost` attaches state:

- some crash via `stateHolder.state`
- some silently no-op
- some throw custom checks
- only `setRootScope` buffers

This is a common timing trap for DI/ViewModel-created navigators.

## Desired contract

Recommended simple policy:

1. `setRootScope(...)` / `replaceApp(...)` may be called before host attachment and are buffered.
2. All route/tab/current-scope operations before attachment fail with one clear error message.
3. No public method silently ignores detached state unless its KDoc explicitly says it is best-effort.

Example error message:

```text
PerseusNavigator.navigateTo() called before PerseusNavHost attached navigation state. Only setRootScope()/replaceApp() are supported before host composition.
```

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/PerseusNavigationStateHolder.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/DefaultPerseusNavigator.kt`
- `perseus-core/src/test/java/com/yigitozgumus/perseus/internal/...`

## Implementation steps

1. Add an explicit attachment API to `PerseusNavigationStateHolder`:
   - `val isAttached: Boolean`
   - `fun requireState(operationName: String): PerseusNavigationState`
   - `fun stateOrNull(): PerseusNavigationState?` only if still needed internally
2. Update `PerseusNavigationStateHolder` KDoc:
   - describe exactly which calls are buffered
   - remove broad language implying all calls replay
3. Update `DefaultPerseusNavigator`:
   - use `requireState("navigateTo")` for methods that require attached state
   - keep `setRootScope` / `replaceApp` using the buffering path
   - decide whether `pop()` and `removeScope()` should fail or no-op; recommended: fail consistently
4. Add detached behavior tests for every public method on:
   - `PerseusNavigator`
   - `PerseusScopeNavigator`

## Tests to add

Suggested file:

- `perseus-core/src/test/java/com/yigitozgumus/perseus/internal/DetachedNavigationBehaviorTest.kt`

Test cases:

- `setRootScope before attach is replayed after attach`
- `replaceApp before attach is replayed after attach`
- `navigateTo before attach throws clear error`
- `pop before attach throws clear error`
- `handleBack before attach throws clear error`
- `switchTab before attach throws clear error`
- `resetTab before attach throws clear error`
- `resetAllWithKeys before attach throws clear error`
- `replaceCurrentScope before attach throws clear error`
- `pushScope before attach throws clear error`
- `removeScope before attach throws clear error`

## Acceptance criteria

- All public methods have deterministic detached behavior.
- KDoc and implementation agree.
- No accidental `stateHolder.state` access remains outside controlled helper methods.
- Existing debug unit tests pass.

---

# Phase 2 — Harden restore normalization

## Problem

Fresh scope specs validate invariants, but restored snapshots build internal state directly. Old/corrupt snapshots can create invalid multi-stack state, invalid selected tab indexes, empty stacks, or ambiguous `NonRestorableKey` roots.

## Desired contract

Restore should never crash because of malformed saved state if a safe fallback is possible.

Define explicit behavior for:

- empty single-stack snapshots
- empty multi-stack roots
- invalid `currentStackIndex`
- missing current stack
- root route that is `NonRestorableKey`
- non-root scope marked `NeverRestore`

Recommended fallback:

- If root scope cannot be restored safely, fall back to the current `initialScope` supplied to `PerseusNavHost` if available through saver flow, or drop invalid non-root scopes and keep the last valid root scope.
- If a non-root scope is invalid, drop it.
- If selected tab index is invalid, clamp or reset to `0` when roots exist.

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/PerseusNavigationState.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/PerseusRestoreOptions.kt`
- `perseus-core/src/test/java/com/yigitozgumus/perseus/internal/PerseusNavigationStateRestoreTest.kt`

## Implementation steps

1. Introduce restore normalization helpers near `fromSnapshot`:
   - `restoreScopeOrNull(...)`
   - `restoreContainerOrNull(...)`
   - `normalizeMultiStack(...)`
   - `normalizeSingleStack(...)`
2. Ensure restored containers satisfy:
   - current stack is non-empty
   - multi-stack roots are non-empty
   - `currentStackIndex` is valid
   - every existing stack has at least one entry or can be recreated from root key
3. Revisit `dropNonRestorableEntries()`:
   - explicitly handle root `NonRestorableKey`
   - document whether root is retained, replaced, or causes scope drop
4. Add tests with hand-built snapshots rather than only snapshots generated by current code.

## Tests to add

Suggested additions to `PerseusNavigationStateRestoreTest.kt` or a new file:

- `restore drops invalid non root scope`
- `restore clamps invalid multi stack index to first tab`
- `restore drops multi stack snapshot with empty roots`
- `restore recreates missing tab stack from root key`
- `restore handles root non restorable key explicitly`
- `restore never returns empty current back stack`

## Acceptance criteria

- Restore code cannot produce invalid current stack state.
- Invalid snapshot behavior is documented in tests.
- Existing restore tests still pass.
- No broad state-machine rewrite.

---

# Phase 3 — Clarify result typing and cleanup

## Problem

`NavigationHandle.observeResult<R>()` says results are filtered to type `R`, but `ResultBusAdapter` casts `Any` to `R`. Wrong generic usage can crash late. Unobserved streams can also remain retained unless observed/cancelled.

## Desired contract

Pick one of these before implementation:

### Option A — Exact-type contract, minimal change

- Update KDoc: caller must observe the exact type sent.
- Keep cast but wrap with a clearer domain error.
- Add tests proving wrong type fails predictably.

### Option B — Filtering contract, safer but slightly more work

- Implement real runtime filtering with type tokens or reified extension.
- Mismatched values do not crash collectors.
- Requires more API design because generic `Flow<R>` type erasure limits runtime checks.

Recommended for short-term simplicity: **Option A**.

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/NavigationHandle.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/ResultBusAdapter.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/DefaultPerseusNavigator.kt`
- `perseus-core/src/test/java/com/yigitozgumus/perseus/internal/ResultBusAdapterTest.kt`

## Implementation steps

1. Update `NavigationHandle.observeResult<R>()` KDoc to match actual contract.
2. Add optional `ResultBusAdapter.clear(correlationId: String)`.
3. Decide cleanup trigger:
   - clear result stream when the destination entry with that correlation ID is removed, or
   - intentionally allow result to survive destination pop until parent observes it.
4. If retaining after pop is intentional, add a bounded cleanup path and document lifetime.
5. Add tests for wrong type and unobserved results.

## Tests to add

- `wrong result type fails with clear message`
- `result before observation is delivered once`
- `unobserved result cleanup follows documented lifetime`
- `duplicate destination result handles are isolated`
- `scope result correlation remains isolated from route result correlation`

## Acceptance criteria

- KDoc and implementation match.
- Result lifetime is explicit.
- Wrong-type behavior is tested.
- Existing result ordering/isolation tests pass.

---

# Phase 4 — Prevent stale ViewModelStore owner resurrection

## Problem

A stale `ViewModelStoreOwner` can access `viewModelStore` after `clear(entryId)` and recreate a new store outside the normal registry lifecycle.

## Desired contract

After an entry is cleared, stale owners must not create new tracked state.

Recommended implementation:

- `StoreOwner` holds a fixed `ViewModelStore` instance created at owner creation.
- `clear(entryId)` clears and unregisters that store.
- Accessing a stale owner returns the cleared store, not a newly registered store.

Alternative:

- stale owner throws after clear.

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/PerseusViewModelStoreRegistry.kt`
- `perseus-core/src/test/java/com/yigitozgumus/perseus/internal/PerseusViewModelStoreRegistryTest.kt`

## Implementation steps

1. Change `StoreOwner` from lazy map lookup to fixed store ownership.
2. Ensure `ownerFor(entryId)` returns the existing owner/store consistently.
3. Ensure `clear(entryId)` clears the fixed store and unregisters global owner.
4. Ensure `retainOnly(...)` still works or remove/deprecate if confirmed unused.

## Tests to add

- `stale owner does not recreate store after clear`
- `ownerFor same entry returns same store before clear`
- `retainOnly clears removed stores and unregisters owners`

## Acceptance criteria

- Stale owner cannot recreate registry state.
- Global owner lookup is unregistered after clear.
- Existing ViewModelStore cleanup tests pass.

---

# Phase 5 — Strengthen provider validation and bottom-bar guardrails

## Problem

Provider resolution has hidden priority when validation is off:

1. Compose screen
2. Compose scene
3. Fragment

Also, `RouterKey.hidesBottomNavigation` defaults to `true`, which is surprising for multi-stack tab roots.

## Desired contract

- Development/sample setup should catch ambiguous providers early.
- Multi-stack root keys should not accidentally hide the bottom bar without an obvious warning/failure.

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/internal/PerseusEntryProviderRegistry.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/PerseusNavigatorFactory.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/key/RouterKey.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/PerseusNavHost.kt`
- `sample/src/main/java/...`
- `README.md`

## Implementation steps

1. Update sample DI/owner creation to use `validateProviders = true`.
2. Update README to recommend `validateProviders = true` during development.
3. Add validation for `MultiStackSpec.rootKeys` when provider validation is enabled:
   - warn or throw if a root key has `hidesBottomNavigation = true`
   - recommended first step: throw only in validation mode
4. Add tests for provider ambiguity and scene-provider misuse.

## Tests to add

- `validateScope validates every multi stack root provider`
- `scene provider for non scene key throws when validation enabled`
- `compose and fragment provider collision throws when validation enabled`
- `multi stack root hiding bottom navigation fails validation or emits warning`

## Acceptance criteria

- Samples/docs guide users toward validation.
- Provider collisions are easier to diagnose.
- Tab root bottom-bar mistakes are caught earlier.

---

# Phase 6 — Add minimal host/instrumentation coverage

## Problem

Most current tests exercise internal state directly. Runtime behavior through Compose, Navigation3, scenes, fragment interop, and Activity saved state is under-tested.

## Desired contract

A small, stable instrumentation suite protects the highest-risk runtime paths without trying to automate every recipe.

## Files

Likely new files:

- `sample/src/androidTest/java/.../PerseusNavHostSmokeTest.kt`
- `sample/src/androidTest/java/.../PerseusMultiStackHostTest.kt`
- `perseus-interop/src/androidTest/java/.../FragmentInteropSmokeTest.kt`
- `sample/src/androidTest/java/.../SceneResultSmokeTest.kt`
- `sample/src/androidTest/java/.../RestorePolicySmokeTest.kt`

Potential helper files:

- test-only Activity
- test keys
- test providers
- stable test tags

## Implementation steps

1. Add a minimal test Activity or Compose test rule host for `PerseusNavHost`.
2. Add stable test tags to sample/test-only screens.
3. Implement tests in this order:
   1. host root → push → back
   2. multi-stack independent stacks and reselect reset
   3. dialog/bottom-sheet show, dismiss, result
   4. fragment destination render and context decode
   5. Activity recreate restore policy
4. Keep tests small and avoid animation/fling sensitivity where possible.

## Tests to add

### Host smoke

- initial root is visible
- navigate to detail shows detail
- back returns to root

### Multi-stack

- tab 0 detail survives switch to tab 1 and back
- reselecting tab resets current stack to root
- configured back behavior switches to initial tab at tab root

### Fragment interop

- fragment provider renders a fragment view
- fragment decodes `NavigationContext`
- fragment-scoped ViewModel survives view recreation and is cleared after pop/reset

### Scene behavior

- dialog confirms result and dismisses
- bottom sheet dismisses on back/outside when cancellable
- non-cancellable bottom sheet does not dismiss on back/outside

### Restore policy

- `RestoreSavedState` survives Activity recreation
- `AlwaysUseInitialScope` ignores previous saved navigation state

## Acceptance criteria

- At least one connected test proves real `PerseusNavHost` behavior.
- At least one connected test proves fragment interop behavior.
- At least one connected test proves scene result/dismissal behavior.
- CI command for connected tests is documented.

---

# Phase 7 — Public API surface pass

## Problem

The library exposes overlapping names for similar operations, increasing decision cost:

- `resetCurrentTab`
- `popToRoot`
- `popCurrentTabToRoot`
- `resetTab`
- `popTabToRoot`
- `setRootScope`
- `replaceApp`

`popUntilKey` also means “remove above and including this key,” which many developers may read as “pop until this key remains.”

## Desired contract

One canonical name per concept in docs and samples.

## Files

- `perseus-core/src/main/java/com/yigitozgumus/perseus/PerseusNavigator.kt`
- `perseus-core/src/main/java/com/yigitozgumus/perseus/PerseusScopeNavigator.kt`
- `README.md`
- sample recipes

## Implementation steps

1. Decide canonical names:
   - current stack root operation: recommended `popToRoot(resetRoot = false)`
   - tab-specific root operation: recommended `resetTab(tabIndex, resetRoot = false)`
   - app root replacement: choose either `replaceApp` or `setRootScope`
2. Update README and samples to use canonical names only.
3. Mark aliases in KDoc as aliases/compatibility conveniences.
4. Consider renaming or adding clearer aliases:
   - `popThroughKey(...)`
   - `popThroughKeyType(...)`
5. Add/adjust tests to use canonical names while retaining alias behavior tests if aliases remain.

## Acceptance criteria

- README does not present multiple names as equally preferred for the same concept.
- KDoc makes aliases obvious.
- Ambiguous `popUntilKey` behavior is clearly documented or replaced with clearer naming.

---

# Recommended execution order

1. Phase 1 — Detached behavior
2. Phase 4 — ViewModelStore stale owner fix
3. Phase 2 — Restore normalization
4. Phase 3 — Result typing/lifetime
5. Phase 5 — Provider/bottom-bar guardrails
6. Phase 6 — Minimal instrumentation coverage
7. Phase 7 — API surface pass

Rationale:

- Phase 1 and 4 are small and reduce confusing lifecycle bugs quickly.
- Phase 2 and 3 harden the most failure-prone state/result paths.
- Phase 5 improves developer feedback without a large architecture change.
- Phase 6 adds confidence after the model-level fixes settle.
- Phase 7 is easiest once behavior is stable and tests protect aliases.

---

# Definition of done for the whole plan

- Existing JVM tests pass.
- New detached behavior tests pass.
- Restore edge-case tests pass.
- Result wrong-type/lifetime tests pass.
- ViewModelStore stale-owner regression test passes.
- Provider validation and bottom-bar guardrail tests pass.
- At least three runtime/instrumentation smoke tests exist:
  - host navigation/multi-stack
  - fragment interop
  - scene result/dismissal or restore policy
- README documents the canonical/simple path and avoids presenting every alias as a primary API.
- No new navigation features are introduced while executing the plan.
