# Navigation

## Back behavior policy

Each scope configures how Perseus consumes root back presses and applies tab-root behavior.

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = MultiStackSpec(
        listOf(HomeKey, SearchKey),
        backBehavior = PerseusBackBehavior(
            rootBackBehavior = RootBackBehavior.Block,
            tabBackBehavior = TabBackBehavior.SwitchToInitialTab,
        ),
    ),
)
```

Options:

- `RootBackBehavior.ExitHost` — do not consume root back; let the host/activity handle it.
- `RootBackBehavior.Block` — consume root back and stay in Perseus.
- `TabBackBehavior.StayOnCurrentTab` — at a tab root, stay on the current tab.
- `TabBackBehavior.SwitchToInitialTab` — at a non-initial tab root, switch to tab `0`.
- `TabBackBehavior.ResetCurrentTab` — at a tab root, recreate/reset the current tab root.

You can also call it directly from custom back handling. By default it uses the current scope policy; pass a behavior only for a one-off override:

```kotlin
val consumed = navigator.handleBack()
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

Observe the current visible key:

```kotlin
navigator.currentKey
    .onEach { key ->
        // Update app chrome, analytics, or feature state.
    }
    .launchIn(scope)
```

Check whether the active back stack can pop:

```kotlin
if (navigator.canGoBack()) {
    navigator.pop()
}
```

Common stack helpers and launch options:

```kotlin
navigator.popToRoot()
navigator.popCurrentTabToRoot()
navigator.popTabToRoot(tabIndex = 1)
navigator.popUntilKey(DetailKey(42))
navigator.popUntilKeyType<DetailKey>()

navigator.navigateTo(DetailKey(42), launchMode = LaunchMode.SingleTop)
navigator.navigateTo(HomeKey, popUpTo = PopUpTo.Root)
navigator.replaceWith(DetailKey(43))
```

Call Perseus navigator APIs from the main thread. If navigation is triggered
from background work, hop to the main dispatcher before mutating navigation
state:

```kotlin
viewModelScope.launch(Dispatchers.Main.immediate) {
    navigator.navigateTo(DetailKey(42))
}
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

`navigateTo(...)` returns a `NavigationHandle`. Await or observe typed completion from it.

```kotlin
val handle = navigator.navigateTo(PickerKey)

when (val result = handle.awaitResult<PickerResult>()) {
    is PerseusResult.Success -> {
        // Handle the result for this exact navigation session.
        val value = result.value
    }
    PerseusResult.Cancelled -> {
        // The destination was popped or removed without sending a result.
    }
}
```

For Flow-based code:

```kotlin
handle.resultFlow<PickerResult>()
    .onEach { result -> /* Success or Cancelled */ }
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
each parent only receives results from its own navigation session. Result delivery
is one-shot: the first success or cancellation wins, wrong result types fail with
a clear mismatch error, and removed entries complete as `PerseusResult.Cancelled`.

Pushed scopes can also return results; see [Scope navigation](navigation.md#scope-navigation-replacing-or-stacking-app-surfaces).

---
