# Advanced topics

## Logging

Perseus can log navigation operations and stack snapshots. Logging is disabled
by default.

```kotlin
val navigationOwner = PerseusNavigatorFactory.create(
    composeProviders = composeProviders,
    fragmentProviders = fragmentProviders,
    sceneProviders = sceneProviders,
    fragmentEntryFactory = DefaultFragmentEntryFactory,
    logger = AndroidPerseusLogger(level = PerseusLogLevel.Debug),
)
```

`Debug` logs before/after stack state, provider resolution, transition tracking,
and ViewModelStore cleanup. `Info` logs operation results and the resulting
stack. Example shape:

```text
after navigateTo entryId=... currentKey=Detail stack=scope=... single [Home#..., Detail#...]
after switchTab current=1 currentKey=Search stack=scope=... multi currentTab=1 tabs=[0:[Home#...], *1:[Search#...]]
```

You can provide your own logger if you need to route messages to another
system:

```kotlin
class MyPerseusLogger : PerseusLogger {
    override val level = PerseusLogLevel.Debug

    override fun log(messageLevel: PerseusLogLevel, message: String) {
        analytics.log("Perseus", message)
    }
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

Fragment interop screens are hosted through `AndroidFragment`, so they do not
reliably participate in Compose/NavDisplay scene transitions. Use Compose
providers for screens where slide/tab scene animation is required, or animate the
Fragment root view inside the Fragment.

---

## Hiding bottom navigation

In a multi-stack scope, the host bottom bar is hidden when the current key has
`hidesBottomNavigation = true`.

```kotlin
@Serializable
data object HomeKey : NavigationKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data object FullScreenDetailKey : NavigationKey {
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

## Debug snapshots

Use a pull-based snapshot for debug screens, logs, or assertions.

```kotlin
val snapshot: StackScopeSnapshot = navigationOwner.debugSnapshot()
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
