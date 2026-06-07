package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusNavigatorImpl
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter
import com.yigitozgumus.perseus.provider.ComposeSceneProvider
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/**
 * One-shot factory that creates a fully-wired [PerseusNavigator].
 *
 * Hides all implementation classes from consumers. The DI module only
 * needs to call [create] with the provider lists from the DI framework.
 *
 * Usage in Koin:
 * ```kotlin
 * single<PerseusNavigator> {
 *     PerseusNavigatorFactory.create(
 *         composeProviders = getAll(),
 *         fragmentProviders = getAll(),
 *         sceneProviders = emptyList(),
 *         fragmentEntryFactory = DefaultFragmentEntryFactory,
 *     )
 * }
 * ```
 */
public object PerseusNavigatorFactory {

    /**
     * Creates a fully-wired [PerseusNavigator] with all infrastructure.
     *
     * @param composeProviders Providers for Compose-based screens.
     * @param fragmentProviders Providers for Fragment-based screens.
     * @param sceneProviders Providers for dialog/bottom sheet scenes.
     * @param fragmentEntryFactory Factory for wrapping fragments in Compose.
     * @return A configured [PerseusNavigator] ready for use.
     */
    public fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<FragmentProviderMarker>,
        sceneProviders: List<ComposeSceneProvider<*>>,
        fragmentEntryFactory: FragmentEntryFactory? = null,
    ): PerseusNavigator {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStore = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders,
            fragmentProviders,
            sceneProviders,
            resultBus,
            viewModelStore,
            fragmentEntryFactory,
        )
        return PerseusNavigatorImpl(
            stateHolder, resultBus, entryRegistry, viewModelStore,
        )
    }
}
