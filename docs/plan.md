# Perseus Navigation Library — Implementation Plan

> **Decisions** (from user clarification):
> - **Key type**: `RouterKey` bridges to nav3's `NavKey` (Serializable-based)
> - **Result passing**: nav3's built-in `ResultEventBus` under the hood, exposed through `NavigationHandle` adapter
> - **DI strategy**: Registry + plugins — core library DI-agnostic, Koin/Hilt integration as separate modules

---

## Medusa → Perseus Mapping

Perseus is a drop-in replacement for Medusa. Here's how each Medusa concept maps:

| Medusa API | Perseus Equivalent | Notes |
|-----------|-------------------|-------|
| `Navigator.start(fragment)` | `navigateTo(key, groupName = null)` | Fragment → key-based routing. Fragment is looked up via `ScreenProvider` |
| `start(fragment, fragmentGroupName: String)` | `navigateTo(key, groupName)` | GroupName is a type-safe wrapper, not raw String |
| `start(fragment, tabIndex)` | `switchTab(tabIndex)` + `navigateTo(key)` | Two-step in Perseus for clarity |
| `start(fragment, tabIndex, fragmentGroupName)` | `switchTab(tabIndex)` + `navigateTo(key, groupName)` | Same two-step pattern |
| `start(fragment, ... transitionAnimation)` | Handled by Compose `transitionSpec` | Compose-native animation, not Fragment-level |
| `preloadFragment(fragment, fragmentTag)` | **Not in v1** | Lower priority preloading |
| `startPreloadedFragment(...)` | **Not in v1** | Lower priority |
| `goBack()` | `pop()` | Same semantics |
| `canGoBack()` | `canGoBack()` | Same semantics |
| `switchTab(tabIndex)` | `switchTab(tabIndex)` | Direct mapping |
| `reset(tabIndex, resetRootFragment)` | `resetTab(tabIndex, resetRoot)` | Same semantics |
| `resetCurrentTab(resetRootFragment)` | `resetCurrentTab(resetRoot)` | Same semantics |
| `reset()` | `resetAllWithKeys(keys)` | Keys replace fragment providers |
| `resetWithFragmentProvider(providers)` | `resetAllWithKeys(keys)` | Root keys define tab structure |
| `clearGroup(fragmentGroupName)` | `popUntil(groupName)` | Same semantics, type-safe names |
| `hasOnlyRoot(tabIndex)` | **Not in v1** | Can be derived from state |
| `getCurrentFragment()` | **Not in v1** | Compose state is the source of truth |
| `getPendingOrCurrentFragment()` | **Not in v1** | N/A — Compose handles pending transitions |
| `initialize(savedState)` | Automatic via `rememberSaveable` | Compose-native state survival |
| `onSaveInstanceState(outState)` | Automatic via Saver | Same — process death survival |
| `observeDestinationChanges(...)` | `observeDestinationChanges(...)` | Direct mapping |
| `observeFragmentTransaction(...)` | `observeFragmentTransaction(...)` | Direct mapping |
| `NavigatorConfiguration` | `PerseusNavigatorConfiguration` | `initialTabIndex`, `alwaysExitFromInitial` |
| `Navigator.NavigatorListener.onTabChanged` | `PerseusNavHost.onTabChanged` callback | Composable callback, not interface |
| `Navigator.OnGoBackListener` | **Not in v1** | Fragment-level back interception is a Fragment concern |
| `Navigator.OnNavigatorTransactionListener` | **Not in v1** | Attachment strategy is Compose-managed |

## Architecture Overview

```
Perseus/
├── sample/                        (renamed from app — example usage)
│   ├── compose/                   Compose screen examples
│   ├── fragment/                  Fragment screen examples (wrapped)
│   ├── di/                        Koin module wiring
│   └── result/                    Result passing examples
│
├── perseus/                       Library module
│   ├── api/  (package)            Public API — interfaces, contracts
│   └── impl/ (package)            Implementation — nav3 internals
│
└── docs/
    ├── plan.md                    This file
    └── learnings.md               Log of decisions, warnings, gotchas
```

### Key Design Principles

1. **`RouterKey` extends `NavKey`**: Keys are Kotlin `@Serializable` data objects/classes. `RouterKey` adds optional properties (`hidesBottomNavigation`, `statusBarState`, groups, scene markers).

2. **Result passing is nav3-native**: The library uses nav3's `ResultEventBus` internally (scoped per NavEntry). `NavigationHandle` is a thin adapter that works in both `@Composable` (via `ResultEffect`) and Fragment ViewModel (via injected accessor).

3. **Entry provider registry is DI-agnostic**: The core takes a simple `(RouterKey) -> NavEntry<RouterKey>` lambda. Koin integration provides `persuesGetEntryProvider()` that auto-collects from Koin modules using koin-navigation3's infrastructure. Hilt integration uses `@IntoSet EntryProviderInstaller` pattern.

4. **ViewModel lifetime = RouterKey lifetime (NOT Fragment lifetime)**: This is the critical interop guarantee. In pure Compose with Nav3, `rememberViewModelStoreNavEntryDecorator()` gives each NavEntry its own ViewModelStore that survives as long as the entry is in the back stack. For Fragment screens, this is NOT automatic — when a Fragment's view is destroyed (e.g., another screen pushed on top), the Fragment's default ViewModelStore is cleared. Perseus must provide a NavEntry-scoped ViewModelStore for fragments, and fragments must use it via `perseusScopedViewModel()`. The store is cleared only when the RouterKey is popped from the stack.

---

## Phase 1: Project Setup

### 1.1 Update Version Catalog

Update `gradle/libs.versions.toml` with Navigation3, Koin, Fragment Compose:

```toml
[versions]
agp = "9.2.1"
kotlin = "2.2.10"
# ... keep existing ...
navigation3 = "1.2.0-alpha02"        # match nav3-recipes
fragmentCompose = "1.8.9"
koin = "4.2.1"
koinNavigation3 = "4.2.1"
lifecycleViewmodelNav3 = "2.11.0-beta01"
kotlinxSerialization = "1.11.0"

[libraries]
# ... keep existing ...
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-fragment-compose = { module = "androidx.fragment:fragment-compose", version.ref = "fragmentCompose" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
koin-compose-viewmodel = { group = "io.insert-koin", name = "koin-compose-viewmodel", version.ref = "koin" }
koin-navigation3 = { group = "io.insert-koin", name = "koin-compose-navigation3", version.ref = "koinNavigation3" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

[plugins]
# ... keep existing ...
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 1.2 Rename `app` → `sample`

- Rename directory `app/` → `sample/`
- Update `settings.gradle.kts`: `include(":sample")`
- Update `sample/build.gradle.kts`: set `namespace = "com.yigitozgumus.perseus.sample"`
- Move source to `com.yigitozgumus.perseus.sample` package

### 1.3 Create `perseus` Library Module

Create `perseus/` with standard Android library structure:
- `perseus/build.gradle.kts` — library plugin, nav3 + fragment + koin dependencies
- `perseus/src/main/AndroidManifest.xml` (minimal)
- Package: `com.yigitozgumus.perseus.api` and `com.yigitozgumus.perseus.impl`

**Verification**: `./gradlew :perseus:assembleDebug` compiles. `./gradlew :sample:assembleDebug` compiles.

---

## Phase 2: Core API (`perseus/api` package)

### 2.1 `PerseusViewModelStoreProvider` — Unified ViewModel scoping (CRITICAL)

This is the **single source of truth** for per-RouterKey ViewModelStores. Both Compose screens and Fragment screens use the same store — no duplication, no split-brain.

```
RouterKey "DetailKey#1"
        │
        └── PerseusViewModelStoreProvider  ← single ConcurrentHashMap
                │
                ├── Compose screen: custom NavEntry decorator that reads
                │   from this provider and sets LocalViewModelStoreOwner
                │   → Compose's standard viewModel() picks it up
                │
                └── Fragment screen: perseusScopedViewModel() delegates
                    to same provider, keyed by RouterKey from arguments
```

### Why a single registry matters

If we used Nav3's built-in `rememberViewModelStoreNavEntryDecorator` for Compose and a separate registry for Fragments, the **same RouterKey** would get **two different ViewModelStores**. A `SharedViewModel` would resolve to different instances depending on whether the screen is Compose or Fragment — defeating the purpose of scoped ViewModels.

### Interface

```kotlin
interface PerseusViewModelStoreProvider {
    /** Returns a ViewModelStoreOwner scoped to the RouterKey's lifetime. */
    fun getOwner(key: RouterKey): ViewModelStoreOwner

    /** Clears and removes the ViewModelStore. Called ONLY when the key is popped. */
    fun clear(key: RouterKey)

    /** Keeps only the given keys. Used on full navigation resets. */
    fun retainOnly(keys: Set<RouterKey>)
}
```

### Implementation

Ported from navigation-router's `NavEntryViewModelStoreRegistry`: a `ConcurrentHashMap<RouterKey, ViewModelStore>`. `getOwner()` calls `getOrPut`; `clear()` removes + calls `store.clear()`.

### Usage in Compose screens

A **custom entry decorator** (replacing `rememberViewModelStoreNavEntryDecorator`) provides the provider's ViewModelStoreOwner via `LocalViewModelStoreOwner`. Compose screens use standard `viewModel()` — they automatically get the NavEntry-scoped store:

```kotlin
// In PerseusNavHost, instead of rememberViewModelStoreNavEntryDecorator:
val viewModelDecorator = rememberPerseusViewModelStoreNavEntryDecorator(provider)

NavDisplay(
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        viewModelDecorator  // ← custom, uses PerseusViewModelStoreProvider
    ),
    ...
)
```

### Usage in Fragment screens

Fragments use a Perseus-provided delegate instead of `viewModels()`:

```kotlin
class MyFragment : Fragment() {
    // perseusScopedViewModel reads the RouterKey from fragment arguments,
    // gets the NavEntry-scoped ViewModelStoreOwner from the provider,
    // and scopes the ViewModel there (NOT to the Fragment's own store)
    private val viewModel: MyViewModel by perseusScopedViewModel()

    // For shared ViewModels across Compose + Fragment on the same key:
    private val sharedVM: SharedViewModel by perseusScopedViewModel()
}
```

`perseusScopedViewModel()` internally:
1. Extracts RouterKey from `requireArguments().getRouterKey()`
2. Calls `provider.getOwner(key)` to get the NavEntry-scoped store
3. Delegates ViewModel creation to that store (via `ViewModelProvider(store, factory)`)

### Cleanup timing

| Event | Store action |
|-------|-------------|
| Key enters stack (`navigateTo`) | Store created (lazy, `getOrPut`) |
| Another screen pushed on top | Store **survives** — no action |
| Tab switched away | Store **survives** — no action |
| Config change / process death | Store **survives** — key still in stack |
| Key popped (`pop`, `popUntil`, `resetTab`) | `clear(key)` — ViewModels destroyed |
| Full reset (`resetAllWithKeys`) | `retainOnly(newKeys)` — old stores cleared |

### 2.2 `RouterKey` — Base key type

```kotlin
// perseus/src/main/java/.../api/RouterKey.kt

@Serializable
interface RouterKey : NavKey {
    val hidesBottomNavigation: Boolean get() = true
    val statusBarState: StatusBarState get() = StatusBarState.LIGHT
}

// Marker interfaces
interface DialogKey : RouterKey
interface BottomSheetKey : RouterKey {
    val isCancellable: Boolean get() = true
    val isDraggable: Boolean get() = true
}
```

**Key difference from navigation-router**: RouterKey now extends NavKey (Serializable) instead of Parcelable. This is the bridge to nav3.

### 2.2 `GroupName` — Type-safe navigation groups

```kotlin
open class GroupName(val name: String) {
    override fun equals(other: Any?) = other is GroupName && name == other.name
    override fun hashCode() = name.hashCode()
}
```

### 2.3 `NavigationHandle` — Result observation

```kotlin
interface NavigationHandle {
    val correlationId: String
    fun <R : Any> observeResult(): Flow<R>
}
```

Adapts nav3's `ResultEventBus` to a Flow-based API usable from ViewModels.

### 2.4 `NavigationContext` — Correlation ID carrier

```kotlin
@Serializable
data class NavigationContext<out K : RouterKey>(
    val key: K,
    val correlationId: String = UUID.randomUUID().toString()
)
```

Serializable (not Parcelable) since RouterKey is Serializable. Extension functions to extract from Bundle.

### 2.5 `PerseusNavigatorConfiguration` — Navigator config

Mirrors Medusa's `NavigatorConfiguration`:

```kotlin
data class PerseusNavigatorConfiguration(
    val initialTabIndex: Int = 0,
    val alwaysExitFromInitial: Boolean = false
)
```

In Medusa there is also `defaultNavigatorTransaction` (ATTACH_DETACH vs SHOW_HIDE). In Perseus this is not needed — NavDisplay via Compose handles entry attachment.

### 2.6 `PerseusNavigator` — Core navigator interface

Mirrors Medusa's `Navigator` interface, adapted for key-based routing:

```kotlin
interface PerseusNavigator {
    // ── Navigation (replaces Medusa's start() overloads) ──
    fun navigateTo(key: RouterKey, groupName: GroupName? = null): NavigationHandle
    fun pop()
    fun canGoBack(): Boolean
    fun popUntil(groupName: GroupName)
    
    // ── Result passing ──
    fun <R : Any> sendResult(context: NavigationContext<*>, result: R)
    
    // ── Tab management (authenticated state) ──
    fun switchTab(tabIndex: Int)
    fun resetTab(tabIndex: Int, resetRoot: Boolean = false)
    fun resetCurrentTab(resetRoot: Boolean = false)
    fun resetAllWithKeys(keys: List<RouterKey>)
    val currentTabIndex: Int
    
    // ── Observation (mirrors Medusa's observe* methods) ──
    fun observeDestinationChanges(
        lifecycleOwner: LifecycleOwner,
        destinationChangedListener: (RouterKey) -> Unit
    )
    fun observeFragmentTransaction(
        lifecycleOwner: LifecycleOwner,
        transactionListener: (currentKey: RouterKey?, nextKey: RouterKey?) -> Unit
    )
}
```

**Key difference from Medusa**: Instead of `start(fragment, ...)` with multiple overloads, Perseus uses `navigateTo(key, groupName?)` with a single entry point. The fragment/composable lookup is handled by the entry provider registry, not by the caller.

### 2.6 Entry Provider Interfaces

```kotlin
// For Compose screens
interface ComposeScreenProvider<K : RouterKey> {
    fun canProvide(key: RouterKey): Boolean
    @Composable
    fun Content(key: K)
}

// For Fragment screens
interface ScreenProvider<K : RouterKey> {
    fun canProvide(key: RouterKey): Boolean
    fun provide(key: K): Fragment
}

// For Compose scenes (dialogs/bottom sheets)
interface ComposeSceneProvider<K : RouterKey> {
    fun canProvide(key: RouterKey): Boolean
    @Composable
    fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit)
}

interface SceneResultCallback {
    fun <R : Any> sendResult(result: R)
}
```

### 2.7 `NavigationStateManager` — Auth state transitions

```kotlin
interface NavigationStateManager {
    fun startUnauthenticated(initialKey: RouterKey)
    fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)
    fun resetToUnauthenticated(initialKey: RouterKey)
    val isAuthenticated: Boolean
}
```

---

## Phase 3: Core Implementation (`perseus/impl` package)

### 3.1 `PerseusNavigationState` — State holder

Based on `TPayNavigationState` from navigation-router, adapted for Serializable keys:

```kotlin
@Stable
class PerseusNavigationState private constructor(...) {
    enum class Mode { Unauthenticated, Authenticated }
    
    var mode: Mode by mutableStateOf(...)
    var currentTabIndex: Int by mutableIntStateOf(...)
    val currentBackStack: SnapshotStateList<RouterKey>
    
    fun navigateTo(key: RouterKey)
    fun goBack(): RouterKey?
    fun removeWhere(predicate: (RouterKey) -> Boolean): List<RouterKey>
    fun switchTab(index: Int)
    fun resetTab(tabIndex: Int, resetRoot: Boolean)
    fun startUnauthenticated(rootKey: RouterKey)
    fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)
    fun resetToUnauthenticated(rootKey: RouterKey)
    
    // Process death survival via Saver
    @Serializable data class Snapshot(...)
    companion object { val Saver: Saver<PerseusNavigationState, Snapshot> }
}
```

### 3.2 `PerseusNavigatorImpl` — Navigator implementation

```kotlin
class PerseusNavigatorImpl(
    private val stateHolder: PerseusNavigationStateHolder,
    private val resultBusAdapter: ResultBusAdapter,
    private val entryRegistry: PerseusEntryProviderRegistry
) : PerseusNavigator {
    // Delegates to state holder + result bus
    // Groups tracked via entryRegistry metadata
}
```

### 3.3 `ResultBusAdapter` — Bridges nav3 ResultEventBus

This is the key piece for result passing:

```kotlin
class ResultBusAdapter {
    // Uses nav3's ResultEventBus under the hood
    // Provides:
    // - createHandle(correlationId): NavigationHandle (for parent)
    // - sendResult(correlationId, result) (for child)
    // - resolveInComposable(correlationId): @Composable {} (ResultEffect wrapper)
}
```

**How it works**:
- `navigateTo()` creates a correlation ID and returns a `NavigationHandle`
- The NavEntry for the child key uses `ResultEffect` to observe results from the child
- When the child calls `sendResult()`, it goes through nav3's `ResultEventBus.sendResult()`
- The `NavigationHandle.observeResult()` internally subscribes to the ResultEventBus flow filtered by correlation ID

### 3.4 `PerseusEntryProviderRegistry` — Entry resolution

```kotlin
class PerseusEntryProviderRegistry(
    private val composeProviders: List<ComposeScreenProvider<*>>,
    private val fragmentProviders: List<ScreenProvider<*>>,
    private val sceneProviders: List<ComposeSceneProvider<*>>
) {
    fun provide(key: RouterKey): NavEntry<RouterKey>
    // Priority: Compose → Scene → Fragment
    // Adds metadata for scenes (DialogKey, BottomSheetKey)
    // Tracks group membership for clearGroup/popUntil
}
```

### 3.5 `FragmentEntry` — Fragment-to-Compose wrapper

```kotlin
@Composable
fun <K : RouterKey> FragmentEntry(
    key: K,
    provider: ScreenProvider<K>,
    viewModelStoreProvider: NavEntryViewModelStoreProvider,
    arguments: Bundle = Bundle()
)
```

Uses `AndroidFragment` composable from `androidx.fragment:fragment-compose`.

**ViewModel scoping**: The `viewModelStoreProvider` is passed through so the fragment can obtain a NavEntry-scoped ViewModelStoreOwner. The RouterKey is registered with the provider before composition, and `clear()` is called when the key is removed from the back stack. Fragments access this via:

```kotlin
// In fragment:
private val entryStore: ViewModelStoreOwner by lazy {
    val key = requireArguments().getRouterKey()
    viewModelStoreProvider.getOwner(key)
}
private val viewModel: MyViewModel by perseusScopedViewModel()
```

The `perseusScopedViewModel()` delegate is a convenience extension provided by Perseus that reads the RouterKey from fragment arguments, retrieves the NavEntry-scoped owner from `PerseusViewModelStoreProvider`, and delegates ViewModel creation there.

### 3.6 `BottomSheetSceneStrategy` — Custom bottom sheet

Adapted from navigation-router's implementation — pure Compose Foundation APIs (no Material3 dependency). Uses `OverlayScene<RouterKey>` from nav3.

### 3.7 `PerseusNavHost` — Main composable

```kotlin
@Composable
fun PerseusNavHost(
    navigator: PerseusNavigator,
    entryProvider: (RouterKey) -> NavEntry<RouterKey>,
    initialKey: RouterKey,
    bottomBar: @Composable (...) -> Unit = {},
    sceneStrategies: List<SceneStrategy<RouterKey>> = defaultSceneStrategies(),
    modifier: Modifier = Modifier
)
```

Handles unauthenticated (single stack) and authenticated (tabs) modes. Integrates BottomSheetSceneStrategy and DialogSceneStrategy.

---

## Phase 4: Koin Integration

### 4.1 Entry Provider Bridge

The koin-navigation3 library provides:
- `navigation<T>` DSL within Koin modules for declaring entries
- `getEntryProvider()` extension on Koin to produce the `(Any) -> NavEntry<Any>` lambda

Perseus Koin integration wraps this:

```kotlin
// In sample app's Koin module:
val appModule = module {
    activityRetainedScope {
        scoped { PerseusNavigatorImpl(get(), get(), get()) } bind PerseusNavigator::class
        scoped { PerseusNavigationStateHolder(/* initial key */) } bind NavigationStateManager::class
        scoped { ResultBusAdapter() }
        scoped {
            PerseusEntryProviderRegistry(
                composeProviders = getAll<ComposeScreenProvider<*>>(),
                fragmentProviders = getAll<ScreenProvider<*>>(),
                sceneProviders = getAll<ComposeSceneProvider<*>>()
            )
        }
    }
}
```

For entry declaration in feature modules:

```kotlin
// Feature module using koin-navigation3's `navigation<T>` DSL:
val homeModule = module {
    activityRetainedScope {
        navigation<HomeKey> { key -> HomeScreen(key) }
        navigation<DetailKey> { key -> DetailScreen(key) }
    }
}
```

The `getEntryProvider()` from koin-navigation3 auto-collects all `navigation<T>` declarations. The bridge adapts this to `(RouterKey) -> NavEntry<RouterKey>`.

### 4.2 Convenience Extensions

```kotlin
// Extension to use koin-navigation3's getEntryProvider with Perseus
@Composable
fun Koin.perseusGetEntryProvider(): (RouterKey) -> NavEntry<RouterKey>
```

---

## Phase 5: Sample App

### 5.1 Screens to Implement

| Screen | Type | Key | Description |
|--------|------|-----|-------------|
| HomeScreen | Compose | `HomeKey` | Tab root — list of items |
| DetailScreen | Compose | `DetailKey(id)` | Item detail |
| ProfileScreen | Fragment | `ProfileKey` | Legacy fragment wrapped in Compose |
| LoginScreen | Compose | `LoginKey` | Simple login form |
| SettingsScreen | Compose | `SettingsKey` | Bottom sheet example |
| ConfirmationDialog | Compose | `ConfirmationDialogKey(title, msg)` | Dialog example |

### 5.2 Auth Flow

1. App starts → unauthenticated → `LoginScreen`
2. Login success → `transitionToAuthenticated([HomeKey, ProfileKey, ...])`
3. Logout → `resetToUnauthenticated(LoginKey)`

### 5.3 Result Passing Examples

1. **Compose → Compose**: `DetailScreen` returns selected item to `HomeScreen`
2. **Compose → Fragment**: `HomeScreen` opens `ProfileFragment`, fragment returns result
3. **Fragment → ViewModel**: Fragment uses injected `PerseusNavigator.sendResult()` to send data back

### 5.4 Group-Based Navigation

Example: Multi-step form flow
```kotlin
navigator.navigateTo(FormStep1Key, groupName = FormFlowGroup)
navigator.navigateTo(FormStep2Key, groupName = FormFlowGroup)
// On cancel:
navigator.popUntil(FormFlowGroup) // Pops both steps
```

### 5.5 Tab Management

- 3 tabs: Home, Search, Profile
- Switch tabs with state preservation
- Reset current tab to root on tab re-tap
- Reset all tabs on logout

---

## Phase 6: Tests

### 6.1 Unit Tests (JVM)

| Test | What it verifies |
|------|-----------------|
| `PerseusNavigationStateTest` | push/pop, group removal, tab switching, auth transitions |
| `GroupNameTest` | equality, hash code |
| `ResultBusAdapterTest` | send → observe correlation, multiple handles isolation |

### 6.2 Android Instrumentation Tests

| Test | What it verifies |
|------|-----------------|
| `ComposeResultPassingTest` | Compose screen sends result → parent receives via NavigationHandle |
| `FragmentResultPassingTest` | Fragment sends result via injected navigator → parent ViewModel receives |
| `ViewModelSurvivalTest` | Fragment ViewModel survives view destruction when another screen is pushed on top; cleared only when key is popped |
| `ViewModelSurvivalTabSwitchTest` | Fragment ViewModel survives tab switch; cleared only when tab is reset |
| `KoinIntegrationTest` | Koin modules wire correctly, entry providers auto-discovered |
| `FragmentComposeInteropTest` | FragmentEntry renders fragment correctly, fragment receives RouterKey |
| `GroupNavigationTest` | popUntil clears all screens in group |
| `TabNavigationTest` | switchTab, resetTab, state preservation across tab switches |

### 6.3 Test Dependencies

```toml
# Add to version catalog
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
koin-test = { group = "io.insert-koin", name = "koin-test", version.ref = "koin" }
```

---

## Implementation Order by Session

### Session 1: Setup + Core API
- Phase 1 (Project setup)
- Phase 2 (Core API interfaces)
- **Commit**: `perseus: project setup and core API interfaces`

### Session 2: Core Implementation
- Phase 3 (State, Navigator, Registry, FragmentEntry)
- **Commit**: `perseus: core implementation with NavDisplay host`

### Session 3: Koin Integration + Sample App Skeleton
- Phase 4 (Koin bridge)
- Phase 5 initial (sample app with basic screens)
- **Commit**: `perseus: koin integration and sample app skeleton`

### Session 4: Sample App — Full Features
- Phase 5 complete (auth flow, result passing, groups, tabs)
- **Commit**: `sample: complete example app with all features`

### Session 5: Tests
- Phase 6 (unit + instrumentation tests)
- **Commit**: `perseus: tests for result passing, koin, interop`

---

## Risks & Warnings

1. **Nav3 API stability**: Navigation3 is alpha (`1.2.0-alpha02`). API surface may change. The library's public API (PerseusNavigator, NavigationHandle) should insulate consumers from nav3 changes.

2. **Serializable vs Parcelable**: Switching from Parcelable to Serializable for keys may require migrating existing key classes. `@Serializable` has different semantics than `@Parcelize` (requires serializer plugin, can't serialize arbitrary objects in constructor params).

3. **ResultEventBus scoping**: nav3's ResultEventBus is scoped per NavEntry. For Fragment ViewModels that need to observe results, we need to ensure the NavEntry is still alive when the ViewModel tries to observe. The `NavigationHandle` Flow-based API should handle this by being independent of composition lifecycle.

4. **`rememberSaveableStateHolderNavEntryDecorator`**: This was removed from public API in newer nav3 builds. The nav3-recipes use `rememberSaveableStateHolderNavEntryDecorator()` which might already be internal. We should verify availability against our target version.

5. **`rememberViewModelStoreNavEntryDecorator`**: From `lifecycle-viewmodel-navigation3`. Ensure this library is compatible with our nav3 version.

6. **Koin `navigation<T>` DSL**: Requires `koin-compose-navigation3` which is tightly coupled to nav3 internals. If nav3 changes its NavEntry structure, koin-navigation3 may need updating too.

7. **Tab state preservation**: When switching tabs, the `SnapshotStateList` for non-visible tabs must retain their content. Our `TPayNavigationState` code handles this, but we need thorough testing with saved state + process death.

8. **Fragment lifecycle in Compose**: `AndroidFragment` composable manages fragment lifecycle. Fragments that use `onActivityResult` or `requestPermissions` need special handling. The library should document these limitations.

9. **ViewModel lifetime ≠ Fragment lifetime (CRITICAL)**: This is the most subtle interop bug. By default, `Fragment.getViewModelStore()` is scoped to the Fragment's `mViewModelStore` field, which is cleared when the Fragment is detached and not retained. In a stack like `[Home → Detail → Form]`, when `Form` is pushed, `Detail`'s view is destroyed and its ViewModelStore may be cleared. Perseus solves this via `PerseusViewModelStoreProvider` — each RouterKey gets its own ViewModelStore that outlives the Fragment's view. **Fragments MUST use `perseusScopedViewModel()` instead of `viewModels()`** to scope to the NavEntry, not the Fragment. Tests must verify:
   - Push screen B on top of A → A's ViewModel survives
   - Switch tab → A's ViewModel survives
   - Pop B off stack → A's ViewModel still survives (was never cleared)
   - Pop A off stack → A's ViewModel IS cleared (key removed from stack)
