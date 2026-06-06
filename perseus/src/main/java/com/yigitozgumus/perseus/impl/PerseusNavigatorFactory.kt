package com.yigitozgumus.perseus.impl

import com.yigitozgumus.perseus.api.ComposeSceneProvider
import com.yigitozgumus.perseus.api.ComposeScreenProvider
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.ScreenProvider

/**
 * One-shot factory that creates and wires all Perseus infrastructure.
 *
 * Hides impl classes from consumers — the Koin module only needs
 * to call [create] and bind the result to [PerseusNavigator].
 *
 * Usage in Koin:
 * ```kotlin
 * single { PerseusNavigatorFactory.create(getAll(), getAll(), emptyList()) } bind PerseusNavigator::class
 * ```
 */
object PerseusNavigatorFactory {

    data class Dependencies(
        val navigator: PerseusNavigator,
        val stateHolder: PerseusNavigationStateHolder,
        val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
        val entryRegistry: PerseusEntryProviderRegistry
    )

    fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<ScreenProvider<*>>,
        sceneProviders: List<ComposeSceneProvider<*>>
    ): Dependencies {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStore = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders = composeProviders,
            fragmentProviders = fragmentProviders,
            sceneProviders = sceneProviders,
            resultBus = resultBus
        )
        val navigator = PerseusNavigatorImpl(stateHolder, resultBus, entryRegistry, viewModelStore)
        return Dependencies(navigator, stateHolder, viewModelStore, entryRegistry)
    }
}
