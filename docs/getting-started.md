# Getting started

## Core concepts

### `NavigationKey`

A `NavigationKey` is the type-safe identity of a destination.

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class DetailKey(val itemId: Int) : NavigationKey
```

Keys should be `@Serializable` so Perseus can restore navigation state after
process death. If you already have Android `Parcelable` keys, Perseus can also
restore those as a fallback without requiring a kotlinx.serialization migration.

### `ScreenProvider`

A provider renders one key type.

```kotlin
class HomeProvider : ScreenProvider<HomeKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is HomeKey

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

Perseus is published through JitPack from Git tags, branches, or commits.

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.yigitozgumus.perseus:perseus-core:<version>")

    // Optional: Fragment interop support
    implementation("com.github.yigitozgumus.perseus:perseus-interop:<version>")
}
```

For this branch before tagging a release, use JitPack's branch snapshot version:

```kotlin
implementation("com.github.yigitozgumus.perseus:perseus-core:v5-SNAPSHOT")
implementation("com.github.yigitozgumus.perseus:perseus-interop:v5-SNAPSHOT")
```

For an app module in this project:

```kotlin
dependencies {
    implementation(project(":perseus-core"))

    // Optional: Fragment interop support
    implementation(project(":perseus-interop"))
}
```

For local publishing tests, publish the release AARs to Maven Local:

```bash
./gradlew :perseus-core:publishToMavenLocal :perseus-interop:publishToMavenLocal
```

Then consume them from another project:

```kotlin
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation("com.yigitozgumus.perseus:perseus-core:0.1.0-SNAPSHOT")

    // Optional: Fragment interop support
    implementation("com.yigitozgumus.perseus:perseus-interop:0.1.0-SNAPSHOT")
}
```

The repository includes `jitpack.yml`; JitPack builds the release publications
with `publishReleasePublicationToMavenLocal` for both library modules.

---

## Quick start

### 1. Define keys

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class DetailKey(val itemId: Int) : NavigationKey
```

### 2. Create providers

```kotlin
class HomeProvider(
    private val navigator: PerseusNavigator,
) : ScreenProvider<HomeKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is HomeKey

    @Composable
    override fun Content(key: HomeKey) {
        Button(onClick = { navigator.navigateTo(DetailKey(1)) }) {
            Text("Open detail")
        }
    }
}

class DetailProvider(
    private val navigator: PerseusNavigator,
) : ScreenProvider<DetailKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is DetailKey

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
`PerseusNavigator` instance. See [Recommended DI setup](advanced.md#recommended-di-setup).

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

Keys are normal Kotlin objects/classes implementing `NavigationKey`.

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class ProductKey(
    val productId: String,
) : NavigationKey
```

Parcelable keys are also supported as a restore fallback:

```kotlin
@Parcelize
data class ProductKey(
    val productId: String,
) : NavigationKey, Parcelable
```

`@Serializable` remains the recommended default because the saved payload is
stable and easier to inspect, but Parcelable support helps migrate existing
Android navigation models incrementally.

### Bottom navigation visibility

`NavigationKey.hidesBottomNavigation` defaults to `true`. For tab roots, override it
to keep the bottom bar visible.

```kotlin
@Serializable
data object HomeKey : NavigationKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data class FullScreenDetailKey(val id: String) : NavigationKey {
    override val hidesBottomNavigation: Boolean = true
}
```

---

## Rendering Compose screens

Implement `ScreenProvider<K>` for each Compose key type.

```kotlin
class ProductProvider : ScreenProvider<ProductKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is ProductKey

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
