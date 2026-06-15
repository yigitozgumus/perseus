# Perseus

<div class="grid cards" markdown>

-   :material-compass-outline: **Type-safe navigation**

    Route with serializable `NavigationKey` objects instead of strings.

-   :material-layers-triple-outline: **Scopes and tabs**

    Model single-stack flows, tabbed apps, and stacked app surfaces explicitly.

-   :material-android: **Compose + Fragment interop**

    Use Navigation 3 for new Compose screens while keeping existing Fragments alive.

-   :material-restore: **Process death ready**

    Restore keys, entry metadata, results, and ViewModel stores with Android lifecycles in mind.

</div>

## Quick taste

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class DetailKey(val itemId: Int) : NavigationKey

navigator.navigateTo(DetailKey(42))
navigator.pop()
```

## Start here

- [Getting started](getting-started.md) — install Perseus and host your first stack.
- [Navigation](navigation.md) — route, tab, scope, and result APIs.
- [Scenes and interop](scenes-interop.md) — dialogs, bottom sheets, and Fragments.
- [Advanced](advanced.md) — restore policy, ViewModel lifetime, logging, and DI.

## Design model

Perseus is built around three boring ideas:

1. **Keys describe destinations** — screens are addressed by `NavigationKey` objects.
2. **Providers render keys** — Compose or Fragment providers turn keys into UI.
3. **Scopes own stacks** — a navigation scope can be a single stack or a multi-stack tab container.
