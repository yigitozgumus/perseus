# Perseus Navigation Library — Learnings & Warnings Log

## Session 0: Pre-implementation Research (2026-06-06)

### Sources Analyzed
- **navigation-router** (`~/code/navigation-router`): User's prior implementation attempt with api/impl modules, embedded Koin, Parcelable-based NavigationKey, custom NavigationResultBus
- **nav3-recipes** (`~/code/nav3-recipes`): Official Android Navigation3 recipe collection using `1.2.0-alpha02`, Koin/Hilt modular patterns, interop, multiple stacks, result passing
- **Medusa** (`~/code/medusa`): Trendyol's Fragment-based navigation library. The API Perseus must be a drop-in replacement for.
- **Perseus** (`/Users/yigitozgumus/code/Perseus`): Fresh Android project with just a Hello World Compose activity

### Medusa API Deep Dive

Medusa (`com.trendyol:medusa:0.13.0`) is a pure Fragment-based navigation library. Key architectural elements:

1. **MultipleStackNavigator** — the main implementation class. Constructor receives:
   - `fragmentManager: FragmentManager` — takes over the Activity's fragment manager
   - `containerId: Int` — the view container for fragments
   - `rootFragmentProvider: List<() -> Fragment>` — factory lambdas for root fragments (one per tab)
   - `navigatorListener: Navigator.NavigatorListener?` — tab change callback
   - `navigatorConfiguration: NavigatorConfiguration` — initialTabIndex, alwaysExitFromInitial, transaction type
   - `transitionAnimationType: TransitionAnimationType?` — default animation

2. **Navigator interface** — the public API with ~20 methods:
   - `start(fragment, ...)` — 6 overloads covering groupName, tabIndex, transitionAnimation combinations
   - `goBack()` / `canGoBack()` — back navigation
   - `switchTab(tabIndex)` — tab switching with state preservation
   - `reset(tabIndex, resetRoot)` / `resetCurrentTab(resetRoot)` / `reset()` / `resetWithFragmentProvider()`
   - `clearGroup(groupName)` — group-based fragment clearing
   - `hasOnlyRoot(tabIndex)` — stack depth check
   - `getCurrentFragment()` / `getPendingOrCurrentFragment()` — fragment accessors
   - `initialize(savedState)` / `onSaveInstanceState(outState)` — process death
   - `observeDestinationChanges(lifecycleOwner, listener)` — destination observation
   - `observeFragmentTransaction(lifecycleOwner, listener)` — transaction observation
   - `getFragmentIndexInStackBySameType(tag)` — stack position lookup
   - `preloadFragment(fragment, tag)` / `startPreloadedFragment(fallback, tag)` — preloading

3. **FragmentStackState** — internal state model:
   - `fragmentTagStack: MutableList<Stack<StackItem>>` — per-tab stacks of `StackItem(fragmentTag, groupName)`
   - `tabIndexStack: Stack<Int>` — tab history (for back-navigation ordering)
   - All stack management is manual: push/pop per tab, group-based pop, tab switching

4. **How Medusa manages fragments**:
   - Uses `FragmentManagerController` wrapping FragmentManager
   - Two transaction strategies: ATTACH_DETACH (default) or SHOW_HIDE
   - Fragments are identified by unique generated tags (via `UniqueTagCreator`)
   - When switching tabs: current fragment is detached/hidden, target tab's top fragment is attached/shown
   - Groups are tracked via `StackItem.groupName` string in the fragment stack
   - State is saved to Bundle via `FragmentStackStateMapper` using Parcelable

5. **What Medusa does NOT have** (that Perseus must add):
   - No Compose support at all
   - No key-based routing — fragments are created externally and passed in
   - No result passing between screens (fragments use FragmentResultListener directly)
   - No DI integration — the navigator is manually constructed
   - No auth state management (unauthenticated vs authenticated flows)
   - No dialog/bottom sheet support as navigation destinations

### Key Design Decisions

1. **NavigationKey extends NavKey (Serializable, not Parcelable)**
   - Reasoning: nav3 natively uses `NavKey` (Serializable). Bridging means keys must be `@Serializable`. This is a breaking change from navigation-router's Parcelable approach but necessary for nav3 compatibility.
   - Impact: Existing Parcelable key classes will need migration. `@Serializable` requires all constructor params to be serializable.

2. **Result passing uses nav3's ResultEventBus**
   - Reasoning: nav3 provides built-in result scoping per NavEntry. Using it avoids reinventing the wheel.
   - NavigationHandle adapted to Flow: Exposes a Flow-based API for ViewModel consumption while nav3's `ResultEffect` composable handles Compose-side observation.
   - Watch out: ResultEventBus is scoped to NavEntry lifecycle. If a Fragment ViewModel outlives the NavEntry, results may be lost. We need to document this.

3. **DI-agnostic core with pluggable integrations**
   - Core takes simple `(NavigationKey) -> NavEntry<NavigationKey>` lambda
   - Koin integration uses `koin-compose-navigation3` for auto-discovery
   - Hilt integration follows nav3-recipes `@IntoSet EntryProviderInstaller` pattern
   - This mirrors how nav3-recipes handles both Koin and Hilt modular patterns

### Medusa → Perseus Design Implications

12. **Key-based vs Fragment-based routing**: Medusa consumers create Fragment instances and pass them to `start()`. Perseus consumers define NavigationKey + ScreenProvider. This is a paradigm shift:
    - **Before (Medusa)**: `navigator.start(MyFragment.newInstance(args), groupName = "flow1")`
    - **After (Perseus)**: `navigator.navigateTo(MyKey(args), groupName = FlowGroup)`
    - This decouples navigation from Fragment creation and enables Compose screens

13. **Tab initialisation**: Medusa uses `List<() -> Fragment>` — factory lambdas. Perseus uses `List<NavigationKey>` — the entry provider registry resolves keys to fragments/composables when needed.

14. **Saved state**: Medusa manually serializes to Bundle via FragmentStackStateMapper. Perseus uses Compose's `rememberSaveable` + `Saver` which automatically handles process death. This is more robust and Compose-native.

15. **Observation APIs**: Medusa's `observeDestinationChanges` and `observeFragmentTransaction` use LiveData. Perseus should expose equivalent APIs using either Flow or callbacks for ViewModel compatibility.

16. **Preloading**: Medusa supports `preloadFragment()` / `startPreloadedFragment()`. For Perseus v1, this is out of scope — NavDisplay doesn't have a first-class preloading concept. Can be added later if needed.

### Critical API Observations from nav3-recipes

4. **`rememberSaveableStateHolderNavEntryDecorator()` may be internal**
   - The nav3-recipes multiple-stacks example uses this decorator, but it might be `@InternalNav3Api` or removed in newer builds
   - Alternatives: Use `rememberSaveableStateHolder` + custom decorator pattern
   - **ACTION**: Verify this API is public in our target nav3 version

5. **`rememberViewModelStoreNavEntryDecorator()`**
   - From `androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0-beta01`
   - Required for scoped ViewModels per NavEntry
   - Must ensure compatibility with nav3 `1.2.0-alpha02`

6. **Koin `navigation<T>` DSL requires `ActivityRetainedScope`**
   - nav3-recipes Koin modular example places `navigation<T>` in `activityRetainedScope { }`
   - This ensures entries survive config changes
   - `getEntryProvider()` from koin-navigation3 auto-collects all `navigation<T>` declarations

7. **Multiple stacks pattern is the foundation for tab navigation**
   - nav3-recipes `MultipleStacksActivity` shows per-tab back stacks
   - NavigationState holds `Map<NavKey, NavBackStack<NavKey>>` + `topLevelRoute: MutableState<NavKey>`
   - Our PerseusNavigationState generalizes this into auth/unauthenticated modes

### Design Comparisons: navigation-router vs Target Perseus

| Aspect | navigation-router | Perseus (Target) |
|--------|-------------------|------------------|
| Key type | Parcelable (NavigationKey) | Serializable (NavigationKey : NavKey) |
| DI | Embedded Koin (`@Single`, KSP) | DI-agnostic + Koin/Hilt modules |
| Result bus | Custom SharedFlow (NavigationResultBus) | nav3 ResultEventBus + NavigationHandle adapter |
| Fragment interop | AndroidFragment + rememberFragmentState + key() {} | Same approach, simplified |
| Bottom sheet | Custom Compose Foundation impl | Same, adapted for NavigationKey |
| State holder | TPayNavigationStateHolder (DI bridge) | Same pattern, renamed |
| Group tracking | ConcurrentHashMap in EntryProviderRegistry | NavEntry metadata (cleaner) |
| NavEntry ViewModel scoping | Manual NavEntryViewModelStoreRegistry | rememberViewModelStoreNavEntryDecorator from lifecycle |

### Potential Issues Identified

8. **Group tracking through NavEntry metadata**
   - navigation-router tracks groups via `ConcurrentHashMap<NavigationKey, GroupName>` in the registry
   - Alternative: Store group as NavEntry metadata. This survives composition better and doesn't need manual cleanup
   - **ACTION**: Implement group tracking via metadata, not side maps

9. **Fragment argument passing**
   - `AndroidFragment` composable takes `arguments: Bundle` parameter
   - Need to pass NavigationKey + NavigationContext to fragment via arguments
   - Extract via extension functions on Bundle (like `getNavigationContext()`)

10. **Process death survival**
    - `rememberSaveable` + `Saver` for PerseusNavigationState
    - All NavigationKeys must be Serializable for this to work
    - SnapshotStateList needs to be reconstructed from saved list

11. **Transition animations**
    - navigation-router uses fast fade (200ms) throughout
    - nav3 supports custom `transitionSpec`, `popTransitionSpec`, `predictivePopTransitionSpec`
    - We should provide sensible defaults but allow customization

12. **ViewModel lifetime in Fragment interop (CRITICAL)**
    - **The problem**: When a Fragment's view is destroyed (another screen pushed on top, or tab switched), the Fragment's default `ViewModelStore` is cleared. This means the ViewModel is destroyed even though the NavigationKey is still in the stack.
    - **Example**: User is on `[HomeFragment → DetailFragment]` in Tab A. They switch to Tab B. Tab A's DetailFragment view is destroyed → its ViewModel is cleared. When they switch back to Tab A, a new ViewModel is created — losing all state.
    - **Expected behavior**: DetailFragment's ViewModel should live as long as `DetailKey` is in the back stack, not as long as the Fragment's view exists.
    - **The solution**: `NavEntryViewModelStoreProvider` — each NavigationKey gets its own ViewModelStore that is independent of any Fragment's lifecycle. The store is created when the key enters the stack and cleared when the key is popped.
    - **Implementation from navigation-router**: `NavEntryViewModelStoreRegistry` uses `ConcurrentHashMap<NavigationKey, ViewModelStore>`. `getOrCreateStore(key)` returns the store; `clear(key)` removes and clears it.
    - **Fragment usage**: Fragments use `perseusScopedViewModel()` delegate instead of `viewModels()`. This delegate reads the NavigationKey from fragment arguments, looks up the NavEntry-scoped ViewModelStoreOwner from `PerseusViewModelStoreProvider`, and scopes the ViewModel there.
    - **In Compose**: Nav3's `rememberViewModelStoreNavEntryDecorator()` already handles this — each NavEntry gets its own store. No extra work needed for Compose screens.
    - **Cleanup timing**: When `pop()` or `popUntil()` or `resetTab()` removes a key from the stack, `NavEntryViewModelStoreProvider.clear(key)` must be called. This is the ONLY time the store is cleared.
    - **Testing required**:
      1. Push B on top of A → A's ViewModel count stays the same (no recreation)
      2. Switch tab away and back → A's ViewModel count stays the same
      3. Pop A from stack → A's ViewModel is cleared (store cleared)
      4. Reset tab → all non-root keys' ViewModels are cleared
