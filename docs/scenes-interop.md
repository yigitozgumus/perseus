# Scenes and Fragment interop

## Dialogs and bottom sheets

Dialogs and bottom sheets are ordinary keys with marker interfaces.

### Dialog

```kotlin
@Serializable
data object ConfirmDeleteKey : NavigationKey, DialogKey
```

Render it with a normal Compose provider:

```kotlin
class ConfirmDeleteProvider : ScreenProvider<ConfirmDeleteKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is ConfirmDeleteKey

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
    .resultFlow<ConfirmResult>()
    .onEach { result -> /* Success or Cancelled */ }
    .launchIn(viewModelScope)
```

### Bottom sheet

```kotlin
@Serializable
data object InfoSheetKey : NavigationKey, BottomSheetKey {
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

For more complex scenes, use `SceneProvider<K>`:

```kotlin
class InfoSheetProvider : SceneProvider<InfoSheetKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is InfoSheetKey

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
class ProfileFragmentProvider : FragmentScreenProvider<ProfileKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is ProfileKey

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
    private val context by lazy { requirePerseusNavigationContext() }
    private val key by lazy { requirePerseusKey<ProfileKey>() }
}
```

For Compose destinations using Koin, `koinViewModel()` works with Perseus automatically because
`PerseusNavHost` provides the entry-scoped `LocalViewModelStoreOwner`:

```kotlin
@Composable
override fun Content(key: DetailKey) {
    val viewModel = koinViewModel<DetailViewModel>(
        parameters = { parametersOf(key) },
    )
}
```

For entry-scoped Fragment ViewModels, use Perseus' entry owner.

```kotlin
private val viewModel by perseusViewModels<MyViewModel>()
```

If your ViewModel uses Koin constructor injection, keep Koin responsible for
creation and pass Perseus' owner to Koin's Fragment delegate. Plain
`by viewModel()` is Fragment-scoped; the `ownerProducer` is what makes it
Perseus-entry-scoped.

```kotlin
private val viewModel by viewModel<MyViewModel>(
    ownerProducer = { requirePerseusViewModelStoreOwner() },
    parameters = { parametersOf(key) },
)
```

For an embedded Koin instance, expose the desired Koin `Scope` from the Fragment
and still use the Perseus owner as the `ownerProducer`.

If you use another DI framework, pass its `ViewModelProvider.Factory` to
`perseusScopedViewModel { ... }` or pass `requirePerseusViewModelStoreOwner()`
to a DI helper that accepts a custom `ViewModelStoreOwner`.

---
