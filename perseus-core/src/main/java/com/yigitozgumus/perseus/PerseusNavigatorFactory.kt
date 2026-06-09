package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.DefaultPerseusNavigator
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter
import com.yigitozgumus.perseus.provider.ComposeSceneProvider
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/**
 * One-shot factory that creates a fully-wired [PerseusNavigationOwner].
 *
 * Hides all implementation classes from consumers. The DI module only
 * needs to call [create] with the provider lists from the DI framework.
 *
 * Usage in Koin:
 * ```kotlin
 * single<PerseusNavigationOwner> {
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
     * Creates a fully-wired [PerseusNavigationOwner] with all infrastructure.
     *
     * @param composeProviders Providers for Compose-based screens.
     * @param fragmentProviders Providers for Fragment-based screens.
     * @param sceneProviders Providers for dialog/bottom sheet scenes.
     * @param fragmentEntryFactory Factory for wrapping fragments in Compose.
     * @param validateProviders If true, [PerseusNavHost] validates initial root providers at startup.
     * @param logger Optional diagnostics logger for navigation operations and stack mutations.
     * @return A configured [PerseusNavigationOwner] ready for host and navigator use.
     */
    public fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<FragmentProviderMarker>,
        sceneProviders: List<ComposeSceneProvider<*>>,
        fragmentEntryFactory: FragmentEntryFactory? = null,
        validateProviders: Boolean = false,
        logger: PerseusLogger = EmptyPerseusLogger,
    ): PerseusNavigationOwner {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStore = PerseusViewModelStoreRegistry(logger)
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders = composeProviders,
            fragmentProviders = fragmentProviders,
            sceneProviders = sceneProviders,
            resultBus = resultBus,
            viewModelStoreProvider = viewModelStore,
            fragmentEntryFactory = fragmentEntryFactory,
            logger = logger,
        )
        return PerseusNavigationOwner(
            DefaultPerseusNavigator(
                stateHolder,
                resultBus,
                entryRegistry,
                viewModelStore,
                validateProviders,
                logger,
            )
        )
    }
}
