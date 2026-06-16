# Getting started

Perseus is a small layer over AndroidX Navigation 3. You model destinations as Kotlin objects, register renderers for those objects, then host a `PerseusNavigationOwner`.

## Installation

Perseus is published to Maven Central.

```kotlin
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.yigitozgumus.perseus:perseus-core:0.1.1")

    // Optional: Fragment interop support
    implementation("com.yigitozgumus.perseus:perseus-interop:0.1.1")
}
```

## 1. Define keys

Keys are the identity of your destinations. Prefer `@Serializable` keys so Android saved state can restore them after process death.

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class DetailKey(val itemId: Int) : NavigationKey
```

Parcelable keys also work as a fallback when migrating existing apps.

## 2. Register screens

The shortest path is the graph DSL:

```kotlin
val graph = perseusGraph {
    screen<HomeKey> {
        HomeScreen()
    }

    screen<DetailKey> { key ->
        DetailScreen(itemId = key.itemId)
    }
}
```

If you prefer DI-collected classes, implement `ScreenProvider<K>` directly:

```kotlin
class HomeProvider(
    private val navigator: PerseusNavigator,
) : ScreenProvider<HomeKey> {
    override fun canProvide(key: NavigationKey): Boolean = key is HomeKey

    @Composable
    override fun Content(key: HomeKey) {
        HomeScreen(onOpenDetail = { navigator.navigateTo(DetailKey(42)) })
    }
}
```

## 3. Create the owner

`PerseusNavigationOwner` owns the runtime state, result bus, provider registry, and entry-scoped ViewModel stores.

```kotlin
val navigationOwner = PerseusNavigatorFactory.create(
    composeProviders = graph.composeProviders,
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
    validateProviders = true,
)
```

Expose the narrow capability each screen needs:

```kotlin
val navigator: PerseusNavigator = navigationOwner.navigator
val scopeNavigator: PerseusScopeNavigator = navigationOwner.scopeNavigator
```

Use `PerseusNavigator` for normal screen navigation and tabs. Use `PerseusScopeNavigator` only for app/scope ownership changes.

## 4. Host navigation

Single-stack app surface:

```kotlin
setContent {
    PerseusNavHost(
        navigationOwner = navigationOwner,
        initialScope = SingleStackSpec(HomeKey),
    )
}
```

Tabbed app surface:

```kotlin
PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = MultiStackSpec(
        rootKeys = listOf(HomeKey, SearchKey, ProfileKey),
    ),
    bottomBar = { selectedIndex, onTabSelected ->
        AppBottomBar(selectedIndex, onTabSelected)
    },
)
```

## 5. Navigate

```kotlin
navigator.navigateTo(DetailKey(42))
navigator.pop()
navigator.switchTab(1)
navigator.resetCurrentTab()
```

For app-level replacement or stacked flows:

```kotlin
scopeNavigator.setRootScope(SingleStackSpec(LoginKey))
scopeNavigator.setRootScope(MultiStackSpec(listOf(HomeKey, SearchKey)))

val checkout = scopeNavigator.pushScope(SingleStackSpec(CheckoutStartKey))
scopeNavigator.removeScope(checkout)
```

## Local development

Inside this repo, depend on the modules directly:

```kotlin
dependencies {
    implementation(project(":perseus-core"))
    implementation(project(":perseus-interop"))
}
```

For local publishing tests:

```bash
./gradlew :perseus-core:publishToMavenLocal :perseus-interop:publishToMavenLocal
```
