# Perseus

Type-safe AndroidX Navigation 3 routing for Jetpack Compose and Fragment interop.

[![Maven Central](https://img.shields.io/maven-central/v/com.yigitozgumus.perseus/perseus-core?label=Maven%20Central)](https://central.sonatype.com/artifact/com.yigitozgumus.perseus/perseus-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple.svg?logo=kotlin)](https://kotlinlang.org)

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
