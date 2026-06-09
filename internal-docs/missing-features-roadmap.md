# Missing Features Roadmap

Date: 2026-06-08
Branch: `v3-implementation`

This document captures potential Perseus features that are not currently implemented but would improve production readiness, developer experience, and navigation ergonomics.

## v3 implementation tracker

Verification command: `./gradlew build` ✅ (2026-06-09)

### To-do / implementation stages

1. ✅ Back behavior policy
   - Added `PerseusBackBehavior`, `RootBackBehavior`, `TabBackBehavior`.
   - Added `PerseusNavigator.handleBack(...)` and wired `PerseusNavHost` back callbacks through it.
2. ✅ Scope removal result
   - Added `ScopeNavigationHandle`, `pushScopeForResult(...)`, and `removeScope(scopeId, result)`.
3. ✅ Safe root replacement helper
   - Added `PerseusScopeNavigator.replaceApp(...)` alias for app/session transitions.
4. ✅ Pop to root helpers
   - Added `popToRoot`, `popTabToRoot`, and `popCurrentTabToRoot`.
   - Works for both single-stack and multi-stack owners.
5. ✅ Pop until key/type
   - Added `popUntilKey(key)`, `popUntilKeyType(KClass)`, and reified extension `popUntilKeyType<T>()`.
6. ✅ Restore guards per key
   - Added `NonRestorableKey` marker.
   - Restored stacks are truncated before non-restorable entries can be restored above root.
7. 🟡 Scope restore policy
   - Added `ScopeRestorePolicy` to `SingleStackSpec` and `MultiStackSpec`.
   - Remaining stage: richer fallback behavior for root scopes marked `NeverRestore` requires a restore saver that can see `initialScope` at restore time.
8. ✅ Debug snapshot API
   - Added `PerseusNavigationOwner.debugSnapshot()`.
9. ✅ Provider registration validation
   - Added `validateProviders` to `PerseusNavigatorFactory.create(...)`.
   - `PerseusNavHost` validates initial root providers when enabled.
10. ✅ Better missing-provider errors
    - Missing-provider failures now list registered Compose, Fragment, and Scene providers.
11. ✅ Test utilities
    - Added `createTestPerseusNavigationOwner(...)` and `currentBackStack()` helper.
12. ✅ Deep link handling
    - Added `DeepLinkResolver`, `DeepLinkTarget`, and navigator/scope navigator `handleDeepLink(...)` helpers.
13. ✅ Declarative graph / typed registry
    - Added lightweight `perseusGraph { screen<Key> { ... } }` builder for Compose screen providers.
14. ✅ Animated tab switching
    - Added `tabTransitionSpec = { fromIndex, toIndex -> ContentTransform? }` to `PerseusNavHost` for user-driven tab changes.

---

## High-value API features

### 1. Back behavior policy

Current back behavior is basic: pop if possible, otherwise rely on host/default behavior.

A configurable back policy would cover common app expectations, especially for bottom navigation.

Potential API:

```kotlin
data class PerseusBackBehavior(
    val rootBackBehavior: RootBackBehavior = RootBackBehavior.ExitHost,
    val tabBackBehavior: TabBackBehavior = TabBackBehavior.StayOnCurrentTab,
)

enum class RootBackBehavior {
    ExitHost,
    Block,
}

enum class TabBackBehavior {
    StayOnCurrentTab,
    SwitchToInitialTab,
    ResetCurrentTab,
}
```

Use cases:

- back at root exits the host
- back at root is blocked
- back in non-initial tab switches to the initial tab
- back in current tab resets it to root

---

### 2. Scope removal result

Pushed scopes support “app inside app” flows, but they do not currently return a result naturally.

Potential API:

```kotlin
val handle = scopeNavigator.pushScopeForResult(SingleStackSpec(CheckoutKey))

handle.observeResult<CheckoutResult>()
```

or:

```kotlin
scopeNavigator.removeScope(scopeId, result = CheckoutResult.Success)
```

Use cases:

- checkout flow returns success/cancel
- onboarding returns completed/skipped
- nested feature returns selected data

---

### 3. Safe root replacement helper

Current API:

```kotlin
scopeNavigator.setRootScope(MultiStackSpec(...))
```

This is already explicit, but common app-flow transitions may benefit from a semantic alias:

```kotlin
scopeNavigator.replaceApp(MultiStackSpec(...))
```

This is not essential, but could make session/app transitions more readable.

---

### 4. Pop to root helpers

Common navigation APIs:

```kotlin
navigator.popToRoot()
navigator.popTabToRoot(tabIndex)
navigator.popCurrentTabToRoot()
```

These may be more intuitive than:

```kotlin
navigator.resetCurrentTab(resetRoot = false)
```

Use cases:

- toolbar/home button returns to tab root
- bottom-nav reselection resets current tab
- flow completion returns to root

---

### 5. Pop until key/type

Perseus supports `GroupName` clearing through `popUntil(groupName)`, but not key-based popping.

Potential API:

```kotlin
navigator.popUntilKey(HomeKey)
navigator.popUntilKeyType<HomeKey>()
```

Use cases:

- return to a known screen
- clear intermediate screens without manually tracking a group
- navigate within wizard-like flows

---

## Lifecycle and state safety features

### 6. Restore guards per key

Host-level restore policy exists, but some apps need per-destination restore control.

Potential marker:

```kotlin
interface NonRestorableKey : RouterKey
```

If restored state contains a non-restorable key, Perseus could:

- drop entries above the nearest safe root
- replace the whole scope with `initialScope`
- throw in debug builds

Use cases:

- singleton-backed screens
- payment/SDK flows
- short-lived transaction screens
- screens that cannot be recreated safely after process death

---

### 7. Scope restore policy

Restore behavior could be scope-specific rather than only host-wide.

Potential API:

```kotlin
SingleStackSpec(
    initialKey = PaymentKey,
    restorePolicy = ScopeRestorePolicy.NeverRestore,
)
```

Use cases:

- root app state restores normally
- pushed checkout/payment scope never restores
- temporary flows restart after process death

---

### 8. Debug snapshot API

Observation APIs were removed, but a non-observing debug snapshot could still help diagnostics.

Potential API:

```kotlin
navigationOwner.debugSnapshot()
```

Use cases:

- sample visualizers
- test assertions
- logs/debug screen
- support/debug tooling

This should be a pull-based inspection API, not a lifecycle observer API.

---

## Developer experience features

### 9. Provider registration validation

Perseus could validate provider registration at startup.

Potential API:

```kotlin
PerseusNavigatorFactory.create(
    ...,
    validateProviders = true,
)
```

Validation examples:

- root keys have providers
- no duplicate providers claim the same key unexpectedly
- restored keys can be decoded
- scene keys have compatible scene strategies

Use cases:

- catch integration mistakes early
- improve sample/dev feedback
- reduce runtime “blank screen” or missing-provider confusion

---

### 10. Better missing-provider errors

If no provider exists, Perseus should throw a highly readable error.

Example:

```text
No provider found for DetailKey(id=42).

Registered Compose providers:
- HomeProvider
- SearchProvider

Registered Fragment providers:
- ProfileProvider

Registered Scene providers:
- ConfirmDialogProvider
```

Use cases:

- faster debugging
- clearer onboarding
- better issue reports

---

### 11. Test utilities

A test utility API could help users test ViewModels/navigation logic without Compose.

Potential API:

```kotlin
val owner = createTestPerseusNavigationOwner(...)

owner.navigator.navigateTo(DetailKey(1))

assertThat(owner.currentBackStack()).containsExactly(HomeKey, DetailKey(1))
```

Use cases:

- ViewModel tests
- scope transition tests
- route result tests
- DI integration tests

---

## UI and navigation features

### 12. Deep link handling

Map external inputs to keys or scopes.

Potential API:

```kotlin
interface DeepLinkResolver {
    fun resolve(uri: Uri): StackScopeSpec? // or RouterKey?
}
```

Potential operations:

```kotlin
scopeNavigator.handleDeepLink(uri)
navigator.handleDeepLink(uri)
```

Use cases:

- open detail in current stack
- switch tab then push detail
- replace root with a target scope
- support notification links

---

### 13. Declarative graph / typed registry

Current provider registration uses imperative `canProvide` checks.

A future developer-experience API could be declarative:

```kotlin
perseusGraph {
    screen<HomeKey> { HomeScreen() }
    screen<DetailKey> { key -> DetailScreen(key.itemId) }
    dialog<ConfirmKey> { ConfirmDialog() }
    bottomSheet<InfoSheetKey> { InfoSheet() }
}
```

Use cases:

- less boilerplate
- easier documentation
- compile-time-ish structure
- better provider validation

This is a larger design effort and should probably come after provider validation.

---

### 14. Animated tab switching

Current transitions focus on push/pop navigation.

A multi-stack tab container may need its own animation policy.

Potential API:

```kotlin
tabTransitionSpec = { fromIndex, toIndex ->
    // ContentTransform
}
```

Use cases:

- horizontal movement between tabs
- fade between top-level sections
- disable animation for tab reselection

---

## Top 5 recommendations

Highest-value features to consider first:

1. Provider validation and better missing-provider errors.
2. Back behavior policy.
3. `popToRoot` and `popUntilKey` helpers.
4. Non-restorable key / restore guard.
5. Scope result API.

These features improve production readiness without changing the core architecture too much.
