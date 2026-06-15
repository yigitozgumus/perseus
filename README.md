# Perseus

Type-safe AndroidX Navigation 3 routing for Jetpack Compose and Fragment interop.

Perseus routes with serializable keys, resolves those keys through providers, and models app surfaces as single-stack or multi-stack scopes.

## Documentation

Full docs live in the MkDocs site:

- **Site:** https://yigitozgumus.github.io/perseus/
- **Source:** [`docs/`](docs/)

Start here:

- [Getting started](docs/getting-started.md)
- [Navigation](docs/navigation.md)
- [Scenes and Fragment interop](docs/scenes-interop.md)
- [Advanced topics](docs/advanced.md)
- [Recipes](docs/recipes.md)
- [Changelog](docs/changelog.md)

## Install

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

For this branch before tagging a release:

```kotlin
implementation("com.github.yigitozgumus.perseus:perseus-core:v6-SNAPSHOT")
implementation("com.github.yigitozgumus.perseus:perseus-interop:v6-SNAPSHOT")
```

## Quick start

```kotlin
@Serializable
data object HomeKey : NavigationKey

@Serializable
data class DetailKey(val itemId: Int) : NavigationKey

val graph = perseusGraph {
    screen<HomeKey> { HomeScreen() }
    screen<DetailKey> { key -> DetailScreen(key.itemId) }
}

val navigationOwner = PerseusNavigatorFactory.create(
    composeProviders = graph.composeProviders,
    fragmentProviders = emptyList(),
    sceneProviders = emptyList(),
)

PerseusNavHost(
    navigationOwner = navigationOwner,
    initialScope = SingleStackSpec(HomeKey),
)

navigationOwner.navigator.navigateTo(DetailKey(42))
```

## Build docs locally

```bash
python3 -m venv /tmp/perseus-mkdocs-venv
/tmp/perseus-mkdocs-venv/bin/pip install mkdocs-material
/tmp/perseus-mkdocs-venv/bin/mkdocs serve
```

## License

Apache License 2.0
