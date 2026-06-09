# Perseus

Perseus is a type-safe navigation library built on top of AndroidX Navigation 3.
It routes by serializable keys, supports Compose and Fragment screens, and gives
large apps explicit ownership boundaries for regular navigation, tabs, and
app-level scope changes.

Perseus is designed around three ideas:

1. **Keys describe destinations** — screens are addressed by `RouterKey` objects.
2. **Providers render keys** — Compose or Fragment providers turn keys into UI.
3. **Scopes own stacks** — a navigation scope can be a single stack or a multi-stack tab container.

---

## Contents

- [Core concepts](#core-concepts)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Creating keys](#creating-keys)
- [Rendering Compose screens](#rendering-compose-screens)
- [Declarative graph registration](#declarative-graph-registration)
- [Creating the navigation owner](#creating-the-navigation-owner)
- [Provider validation and missing-provider errors](#provider-validation-and-missing-provider-errors)
- [Hosting Perseus](#hosting-perseus)
- [Back behavior policy](#back-behavior-policy)
- [Navigating between screens](#navigating-between-screens)
- [Single-stack vs multi-stack scopes](#single-stack-vs-multi-stack-scopes)
- [Tabs with `MultiStackSpec`](#tabs-with-multistackspec)
- [Scope navigation: replacing or stacking app surfaces](#scope-navigation-replacing-or-stacking-app-surfaces)
- [Returning results](#returning-results)
- [Deep links](#deep-links)
- [Dialogs and bottom sheets](#dialogs-and-bottom-sheets)
- [Fragment interop](#fragment-interop)
- [Transitions](#transitions)
- [Hiding bottom navigation](#hiding-bottom-navigation)
- [Process death and saved state](#process-death-and-saved-state)
- [Restore policy](#restore-policy)
- [Debug snapshots and test utilities](#debug-snapshots-and-test-utilities)
- [ViewModel lifetime](#viewmodel-lifetime)
- [Grouping and clearing flows](#grouping-and-clearing-flows)
- [Recommended DI setup](#recommended-di-setup)
- [Sample recipes](#sample-recipes)
- [Development notes](#development-notes)

---

## Core concepts

### `RouterKey`

A `RouterKey` is the type-safe identity of a destination.

```kotlin
@Serializable
data object HomeKey : RouterKey

@Serializable
data class DetailKey(val itemId: Int) : RouterKey
```

Keys should be `@Serializable` so Perseus can restore navigation state after
process death.

### `ComposeScreenProvider`

A provider renders one key type.

```kotlin
class HomeProvider : ComposeScreenProvider<HomeKey> {
    override fun canProvide(key: RouterKey): Boolean = key is HomeKey

    @Composable
    override fun Content(key: HomeKey) {
        HomeScreen()
    }
}
```

### `PerseusNavigationOwner`

`PerseusNavigationOwner` owns one Perseus runtime. Pass it to `PerseusNavHost`.
It exposes two narrower public capabilities:

```kotlin
val navigator: PerseusNavigator = navigationOwner.navigator
val scopeNavigator: PerseusScopeNavigator = navigationOwner.scopeNavigator
```

Use:

- `PerseusNavigator` for normal route navigation and tabs.
- `PerseusScopeNavigator` for replacing or stacking navigation scopes.

This prevents ordinary screens from accidentally replacing the whole app scope.

### `PerseusNavigator`

Route + tab navigation:

```kotlin
navigator.navigateTo(DetailKey(42))
navigator.pop()
navigator.switchTab(1)
navigator.resetCurrentTab()
```

### `PerseusScopeNavigator`

App/scope ownership:

```kotlin
scopeNavigator.setRootScope(SingleStackSpec(LoginKey))
scopeNavigator.setRootScope(MultiStackSpec(listOf(HomeKey, SearchKey, ProfileKey)))

val flow = scopeNavigator.pushScope(SingleStackSpec(CheckoutStartKey))
scopeNavigator.removeScope(flow)
```

---

## Installation

Perseus currently lives in this repository as Android library modules.

For an app module in this project:

```kotlin
dependencies {
    implementation(project(":perseus-core"))

    // Optional: Fragment interop support
    implementation(project(":perseus-interop"))
}
```

If Perseus is published later, replace the project dependencies with the
published coordinates.

---

## Quick start

### 1. Define keys

```kotlin
@Serializable
data object HomeKey : RouterKey

@Serializable
data class DetailKey(val itemId: Int) : RouterKey
```

### 2. Create providers

```kotlin
class HomeProvider(
    private val navigator: PerseusNavigator,
) : ComposeScreenProvider<HomeKey> {
    override fun canProvide(key: RouterKey): Boolean = key is HomeKey

    @Composable
    override fun Content(key: HomeKey) {
        Button(onClick = { navigator.navigateTo(DetailKey(1)) }) {
            Text("Open detail")
        }
    }
}

class DetailProvider(
    private val navigator: PerseusNavigator,
) : ComposeScreenProvider<DetailKey> {
    override fun canProvide(key: RouterKey): Boolean = key is DetailKey

    @Composable
    override fun Content(key: DetailKey) {
        Column {
            Text("Detail ${key.itemId}")
            Button(onClick = { navigator.pop() }) {
                Text("Back")
            }
        }
    }
}
```

### 3. Create a navigation owner

```kotlin
val navigationOwner = PerseusNavigatorFactory.create(
    composeProviders = listOf(
        HomeProvider(navigator),
        DetailProvider(navigator),
    ),
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
)
```

In real apps, prefer DI so providers and ViewModels receive the same
`PerseusNavigator` instance. See [Recommended DI setup](#recommended-di-setup).

### 4. Host it

```kotlin
setContent {
    PerseusNavHost(
        navigationOwner = navigationOwner,
        initialScope = SingleStackSpec(HomeKey),
        modifier = Modifier.fillMaxSize(),
    )
}
```

---

## Creating keys

Keys are normal Kotlin objects/classes implementing `RouterKey`.

```kotlin
@Serializable
data object HomeKey : RouterKey

@Serializable
data class ProductKey(
    val productId: String,
) : RouterKey
```

### Bottom navigation visibility

`RouterKey.hidesBottomNavigation` defaults to `true`. For tab roots, override it
to keep the bottom bar visible.

```kotlin
@Serializable
data object HomeKey : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data class FullScreenDetailKey(val id: String) : RouterKey {
    override val hidesBottomNavigation: Boolean = true
}
```

---

## Rendering Compose screens

Implement `ComposeScreenProvider<K>` for each key type.

```kotlin
class ProductProvider : ComposeScreenProvider<ProductKey> {
    override fun canProvide(key: RouterKey): Boolean = key is ProductKey

    @Composable
    override fun Content(key: ProductKey) {
        ProductScreen(productId = key.productId)
    }
}
```

Providers are registered when creating `PerseusNavigationOwner`.

```kotlin
PerseusNavigatorFactory.create(
    composeProviders = listOf(ProductProvider()),
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
)
```

---

## Declarative graph registration

For Compose-only screens, you can register providers with the lightweight typed graph builder.

```kotlin
val graph = perseusGraph {
    screen<HomeKey> { HomeScreen() }
    screen<DetailKey> { key -> DetailScreen(key.itemId) }
}

val navigationOwner = PerseusNavigatorFactory.create(
    composeProviders = graph.composeProviders,
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
)
```

Use explicit provider classes when you need DI-managed provider instances or more custom provider logic.

---

## Creating the navigation owner

`PerseusNavigatorFactory.create(...)` returns a `PerseusNavigationOwner`.

```kotlin
val navigationOwner: PerseusNavigationOwner = PerseusNavigatorFactory.create(
    composeProviders = listOf(HomeProvider(), DetailProvider()),
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
    validateProviders = true, // optional startup validation
)
```

The owner is the object you pass to `PerseusNavHost`.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(HomeKey),
)
```

The owner also exposes narrower APIs:

```kotlin
val navigator: PerseusNavigator = navigationOwner.navigator
val scopeNavigator: PerseusScopeNavigator = navigationOwner.scopeNavigator
```

---

## Provider validation and missing-provider errors

Pass `validateProviders = true` to validate initial root providers when the host starts.

```kotlin
PerseusNavigatorFactory.create(
    composeProviders = getAll(),
    fragmentProviders = getAll(),
    sceneProviders = getAll(),
    validateProviders = true,
)
```

If a key cannot be rendered, Perseus throws a readable error that includes the missing key and registered Compose, Fragment, and Scene providers.

---

## Hosting Perseus

### Single-stack host

Use `SingleStackSpec` when the app surface has one back stack.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(LoginKey),
    modifier = Modifier.fillMaxSize(),
)
```

### Multi-stack host with bottom navigation

Use `MultiStackSpec` when the current app surface has tabs.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = MultiStackSpec(
        rootKeys = listOf(HomeKey, SearchKey, ProfileKey),
        initialStackIndex = 0,
    ),
    bottomBar = { selectedIndex, onTabSelected ->
        NavigationBar {
            listOf("Home", "Search", "Profile").forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    icon = {},
                    label = { Text(label) },
                )
            }
        }
    },
    onTabChanged = { index ->
        // Optional: sync UI state or analytics.
    },
)
```

---

## Back behavior policy

`PerseusNavHost` can consume root back presses and apply tab-root behavior.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = MultiStackSpec(listOf(HomeKey, SearchKey)),
    backBehavior = PerseusBackBehavior(
        rootBackBehavior = RootBackBehavior.Block,
        tabBackBehavior = TabBackBehavior.SwitchToInitialTab,
    ),
)
```

Options:

- `RootBackBehavior.ExitHost` — do not consume root back; let the host/activity handle it.
- `RootBackBehavior.Block` — consume root back and stay in Perseus.
- `TabBackBehavior.StayOnCurrentTab` — at a tab root, stay on the current tab.
- `TabBackBehavior.SwitchToInitialTab` — at a non-initial tab root, switch to tab `0`.
- `TabBackBehavior.ResetCurrentTab` — at a tab root, recreate/reset the current tab root.

You can also call it directly from custom back handling:

```kotlin
val consumed = navigator.handleBack(PerseusBackBehavior())
```

---

## Navigating between screens

Inject or pass `PerseusNavigator` to code that performs regular navigation.

```kotlin
class HomeViewModel(
    private val navigator: PerseusNavigator,
) : ViewModel() {
    fun openDetail(id: Int) {
        navigator.navigateTo(DetailKey(id))
    }

    fun back() {
        navigator.pop()
    }
}
```

Check whether the active back stack can pop:

```kotlin
if (navigator.canGoBack()) {
    navigator.pop()
}
```

Common stack helpers:

```kotlin
navigator.popToRoot()
navigator.popCurrentTabToRoot()
navigator.popTabToRoot(tabIndex = 1)
navigator.popUntilKey(DetailKey(42))
navigator.popUntilKeyType<DetailKey>()
```

---

## Single-stack vs multi-stack scopes

A **scope** is a navigation container.

Perseus has two scope specs:

```kotlin
SingleStackSpec(LoginKey)
```

One back stack. Good for:

- login
- onboarding
- checkout
- focused feature flows

```kotlin
MultiStackSpec(listOf(HomeKey, SearchKey, ProfileKey))
```

Multiple sibling back stacks. Good for:

- bottom navigation tabs
- top-level app sections

A navigator can also have a stack of scopes. This enables “open a new app inside
an app” behavior:

```text
Root scope: Main tabs
└── Pushed scope: Checkout flow
```

---

## Tabs with `MultiStackSpec`

Tabs are part of `MultiStackSpec`. Each tab owns an independent back stack.

```kotlin
navigator.switchTab(1)
navigator.resetTab(tabIndex = 1, resetRoot = false)
navigator.resetCurrentTab(resetRoot = false)
```

Typical bottom bar behavior:

```kotlin
bottomBar = { selectedIndex, onTabSelected ->
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onTabSelected(0) },
            icon = {},
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onTabSelected(1) },
            icon = {},
            label = { Text("Search") },
        )
    }
}
```

`PerseusNavHost` handles the default behavior:

- selecting another tab switches tabs
- selecting the current tab resets that tab to its root

---

## Scope navigation: replacing or stacking app surfaces

Use `PerseusScopeNavigator` for operations that change app-level navigation
containers.

```kotlin
class SessionNavigationController(
    private val scopeNavigator: PerseusScopeNavigator,
) {
    fun showLogin() {
        scopeNavigator.setRootScope(SingleStackSpec(LoginKey))
    }

    fun showMainApp() {
        scopeNavigator.replaceApp(
            MultiStackSpec(listOf(HomeKey, SearchKey, ProfileKey))
        )
    }
}
```

`replaceApp(...)` is a semantic alias for `setRootScope(...)`.

### Push a temporary app-like flow

```kotlin
val checkoutScopeId = scopeNavigator.pushScope(
    SingleStackSpec(CheckoutStartKey)
)
```

Normal navigation now affects the pushed scope.

```kotlin
navigator.navigateTo(CheckoutAddressKey)
navigator.navigateTo(CheckoutPaymentKey)
```

Remove the whole pushed scope when done:

```kotlin
scopeNavigator.removeScope(checkoutScopeId)
```

Or push a scope that can return a result:

```kotlin
val handle = scopeNavigator.pushScopeForResult(SingleStackSpec(CheckoutStartKey))

handle.observeResult<CheckoutResult>()
    .onEach { result -> /* checkout completed/cancelled */ }
    .launchIn(viewModelScope)

scopeNavigator.removeScope(handle.scopeId, CheckoutResult.Success)
```

### Replace only the current scope

```kotlin
scopeNavigator.replaceCurrentScope(SingleStackSpec(CheckoutCompleteKey))
```

### Query the current scope

```kotlin
val scope = scopeNavigator.currentScope

when (scope.kind) {
    StackScopeKind.SingleStack -> Unit
    StackScopeKind.MultiStack -> Unit
}
```

---

## Returning results

`navigateTo(...)` returns a `NavigationHandle`. Observe typed results from it.

```kotlin
val handle = navigator.navigateTo(PickerKey)

handle.observeResult<PickerResult>()
    .onEach { result ->
        // Handle the result for this exact navigation session.
    }
    .launchIn(viewModelScope)
```

In a Compose destination, read the current navigation context and send a result.

```kotlin
@Composable
fun PickerScreen(navigator: PerseusNavigator) {
    val context = LocalNavigationContext.current

    Button(
        onClick = {
            if (context != null) {
                navigator.sendResult(context, PickerResult("Selected value"))
                navigator.pop()
            }
        }
    ) {
        Text("Choose")
    }
}
```

Results are scoped by a correlation ID. If two parents open the same destination,
each parent only receives results from its own navigation session.

Pushed scopes can also return results; see [Scope navigation](#scope-navigation-replacing-or-stacking-app-surfaces).

---

## Deep links

Perseus provides small helpers for mapping URIs to keys or scopes.

```kotlin
val resolver = DeepLinkResolver { uri ->
    when (uri.host) {
        "detail" -> DeepLinkTarget.Key(DetailKey(uri.lastPathSegment!!.toInt()))
        "home" -> DeepLinkTarget.Scope(MultiStackSpec(listOf(HomeKey, SearchKey)))
        else -> null
    }
}

navigator.handleDeepLink(uri, resolver)      // navigates to key targets
scopeNavigator.handleDeepLink(uri, resolver) // replaces root for scope targets
```

The resolver is intentionally app-owned so URL parsing, authentication, and routing policy stay outside Perseus.

---

## Dialogs and bottom sheets

Dialogs and bottom sheets are ordinary keys with marker interfaces.

### Dialog

```kotlin
@Serializable
data object ConfirmDeleteKey : RouterKey, DialogKey
```

Render it with a normal Compose provider:

```kotlin
class ConfirmDeleteProvider : ComposeScreenProvider<ConfirmDeleteKey> {
    override fun canProvide(key: RouterKey): Boolean = key is ConfirmDeleteKey

    @Composable
    override fun Content(key: ConfirmDeleteKey) {
        val actions = LocalSceneActions.current

        AlertDialog(
            onDismissRequest = { actions.dismiss() },
            confirmButton = {
                Button(onClick = { actions.sendResultAndDismiss(ConfirmResult.Yes) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { actions.dismiss() }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete item?") },
        )
    }
}
```

Open it and observe the result:

```kotlin
navigator.navigateTo(ConfirmDeleteKey)
    .observeResult<ConfirmResult>()
    .onEach { result -> /* ... */ }
    .launchIn(viewModelScope)
```

### Bottom sheet

```kotlin
@Serializable
data object InfoSheetKey : RouterKey, BottomSheetKey {
    override val isCancellable: Boolean = true
    override val isDraggable: Boolean = true
}
```

Use `LocalSceneActions` to dismiss or send a result.

```kotlin
val actions = LocalSceneActions.current
Button(onClick = { actions.dismiss() }) {
    Text("Close")
}
```

For more complex scenes, use `ComposeSceneProvider<K>`:

```kotlin
class InfoSheetProvider : ComposeSceneProvider<InfoSheetKey> {
    override fun canProvide(key: RouterKey): Boolean = key is InfoSheetKey

    @Composable
    override fun Content(
        key: InfoSheetKey,
        onResult: SceneResultCallback,
        onDismiss: () -> Unit,
    ) {
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}
```

---

## Fragment interop

Add the interop module:

```kotlin
implementation(project(":perseus-interop"))
```

Create a Fragment provider:

```kotlin
class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
    override fun canProvide(key: RouterKey): Boolean = key is ProfileKey

    override fun provide(key: ProfileKey): Fragment = ProfileFragment()
}
```

Register it:

```kotlin
PerseusNavigatorFactory.create(
    composeProviders = listOf(HomeProvider()),
    fragmentProviders = listOf(ProfileFragmentProvider()),
    sceneProviders = emptyList(),
    fragmentEntryFactory = DefaultFragmentEntryFactory,
)
```

Read the key/context inside a Fragment:

```kotlin
class ProfileFragment : Fragment() {
    private val context: NavigationContext<ProfileKey> by lazy {
        requireArguments().getNavigationContext<ProfileKey>()
    }

    private val key: ProfileKey get() = context.key
}
```

---

## Transitions

### Host-level transitions

`PerseusNavHost` accepts host-level transition specs.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(HomeKey),
    transitionSpec = {
        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
    },
    popTransitionSpec = {
        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
    },
)
```

### Per-navigation transition

Pass a `ContentTransform` to a single navigation call.

```kotlin
val scaleTransition = scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()

navigator.navigateTo(
    DetailKey(5),
    transition = scaleTransition,
)
```

Per-navigation transitions use Navigation 3 transition metadata and apply to
that entry only.

### Tab transition

For multi-stack hosts, use `tabTransitionSpec` to animate user-driven tab changes.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = MultiStackSpec(listOf(HomeKey, SearchKey)),
    tabTransitionSpec = { fromIndex, toIndex ->
        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
    },
)
```

Return `null` to fall back to the normal host transition.

---

## Hiding bottom navigation

In a multi-stack scope, the host bottom bar is hidden when the current key has
`hidesBottomNavigation = true`.

```kotlin
@Serializable
data object HomeKey : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data object FullScreenDetailKey : RouterKey {
    override val hidesBottomNavigation: Boolean = true
}
```

---

## Process death and saved state

Perseus uses `rememberSaveable` and a custom saver for navigation state. Internally, the saved snapshot is encoded as a JSON `String`, which is Bundle-native; route keys are still serialized with kotlinx.serialization.

Saved state includes:

- active scope stack
- single-stack and multi-stack containers
- selected tab index
- every back-stack entry
- stable entry IDs
- serialized key payloads
- group names
- result correlation IDs

`initialScope` is only used when there is no saved navigation state.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(LoginKey),
)
```

If the user starts at login, authenticates into a `MultiStackSpec`, backgrounds
the app, and Android kills the process, a saved-state restore should restore the
multi-stack state. A true fresh cold start still begins from `initialScope`.

Avoid calling `scopeNavigator.setRootScope(...)` unconditionally from
`onCreate`, because doing so can overwrite restored state.

---

## Restore policy

By default, Perseus restores saved navigation state after process death:

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(LoginKey),
    restorePolicy = PerseusRestorePolicy.RestoreSavedState, // default
)
```

If some screens are unsafe to restore after process death, opt out and always
rebuild from `initialScope`:

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = if (isAuthenticated) {
        MultiStackSpec(listOf(HomeKey, SearchKey, ProfileKey))
    } else {
        SingleStackSpec(LoginKey)
    },
    restorePolicy = PerseusRestorePolicy.AlwaysUseInitialScope,
)
```

Use `AlwaysUseInitialScope` when you want process death to rebuild from your
current app/auth state instead of restoring internal screens.

### Per-key restore guard

Mark a destination as non-restorable if it cannot safely survive process death.

```kotlin
@Serializable
data object PaymentSdkKey : NonRestorableKey
```

If restored state contains a `NonRestorableKey`, Perseus truncates that restored stack before the non-restorable entry while keeping the root when possible.

### Scope restore policy

Scopes can also declare restore intent.

```kotlin
SingleStackSpec(
    initialKey = CheckoutStartKey,
    restorePolicy = ScopeRestorePolicy.NeverRestore,
)
```

`ScopeRestorePolicy` is stored with scope specs and snapshots. Host-level `PerseusRestorePolicy` still decides whether saved state is used at all.

---

## Debug snapshots and test utilities

Use a pull-based snapshot for debug screens, logs, or assertions.

```kotlin
val snapshot: StackScopeSnapshot = navigationOwner.debugSnapshot()
```

For JVM tests, create an attached owner without composing `PerseusNavHost`.

```kotlin
val owner = createTestPerseusNavigationOwner(SingleStackSpec(HomeKey))

owner.navigator.navigateTo(DetailKey(1))
assertThat(owner.currentBackStack()).containsExactly(HomeKey, DetailKey(1))
```

---

## ViewModel lifetime

Perseus provides entry-scoped ViewModelStores.

This matters when:

- the same route key is pushed multiple times
- a Fragment view is destroyed while its back-stack entry remains
- switching tabs hides one stack while preserving its state

Each back-stack entry receives a stable entry ID. ViewModel stores are cleared
when the owning entry is removed.

In Compose screens, use normal lifecycle ViewModel APIs; Perseus supplies the
entry-scoped owner through its NavEntry decorator.

```kotlin
@Composable
fun CounterScreen() {
    val owner = LocalViewModelStoreOwner.current
    val viewModel = remember(owner) {
        ViewModelProvider(checkNotNull(owner))[CounterViewModel::class.java]
    }
}
```

---

## Grouping and clearing flows

Use `GroupName` to tag related entries and clear them together.

```kotlin
private object CheckoutGroup : GroupName("checkout")

navigator.navigateTo(CheckoutStepKey(1), groupName = CheckoutGroup)
navigator.navigateTo(CheckoutStepKey(2), groupName = CheckoutGroup)
navigator.navigateTo(CheckoutStepKey(3), groupName = CheckoutGroup)

navigator.popUntil(CheckoutGroup)
```

`popUntil(groupName)` removes matching entries from the current back stack. The
root entry is never removed.

---

## Recommended DI setup

Bind one `PerseusNavigationOwner`, then expose narrower capabilities.

```kotlin
val navigationModule = module {
    single<PerseusNavigationOwner> {
        PerseusNavigatorFactory.create(
            composeProviders = getAll(),
            fragmentProviders = getAll(),
            sceneProviders = getAll(),
            fragmentEntryFactory = DefaultFragmentEntryFactory,
        )
    }

    single<PerseusNavigator> {
        get<PerseusNavigationOwner>().navigator
    }

    single<PerseusScopeNavigator> {
        get<PerseusNavigationOwner>().scopeNavigator
    }
}
```

Use the narrowest dependency possible:

```kotlin
class HomeViewModel(
    private val navigator: PerseusNavigator,
)

class SessionCoordinator(
    private val scopeNavigator: PerseusScopeNavigator,
)
```

The host receives the owner:

```kotlin
class MainActivity : ComponentActivity(), KoinComponent {
    private val navigationOwner: PerseusNavigationOwner by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = SingleStackSpec(LoginKey),
            )
        }
    }
}
```

---

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
| Hide Bottom Bar | `RouterKey.hidesBottomNavigation` |
| Custom Sheet | Custom sheet-style UI |
| Animations | Host-level transitions |
| Per-Transition | Per-navigation transitions |
| Stack Scopes | `PerseusScopeNavigator`, root replacement, pushed scopes |
| Process Death Restore | Manual saved-state restore flow |
| Group Pop | `GroupName` + `popUntil` |
| ViewModel Lifetime | Entry-scoped ViewModel stores |
| Back Behavior Policy | Root and tab back behavior controls |
| Scope Results | `pushScopeForResult` and `removeScope(result)` |
| Navigation Helpers | Deep links, pop helpers, graph builder, and provider validation |
| Restore Guards | `NonRestorableKey` and `ScopeRestorePolicy` |
| Full Demo | Multi-feature sample |

Run the `sample` app and open the recipe picker to explore them.

---

## Development notes

### Verification

Useful verification commands:

```bash
./gradlew :perseus-core:testDebugUnitTest --console=plain
./gradlew :perseus-interop:compileDebugKotlin :sample:compileDebugKotlin --console=plain
./gradlew check --console=plain
```
