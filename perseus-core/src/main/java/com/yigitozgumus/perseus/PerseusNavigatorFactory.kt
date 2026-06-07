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
 * One-shot factory that creates a fully-wired [PerseusController].
 *
 * Hides all implementation classes from consumers. The DI module only
 * needs to call [create] with the provider lists from the DI framework.
 *
 * Usage in Koin:
 * ```kotlin
 * single<PerseusController> {
 *     PerseusNavigatorFactory.createController(
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
     * Creates a fully-wired [PerseusController] with all infrastructure.
     *
     * @param composeProviders Providers for Compose-based screens.
     * @param fragmentProviders Providers for Fragment-based screens.
     * @param sceneProviders Providers for dialog/bottom sheet scenes.
     * @param fragmentEntryFactory Factory for wrapping fragments in Compose.
     * @return A configured [PerseusController] ready for host and navigator use.
     */
    public fun createController(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<FragmentProviderMarker>,
        sceneProviders: List<ComposeSceneProvider<*>>,
        fragmentEntryFactory: FragmentEntryFactory? = null,
    ): PerseusController {
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
        return PerseusController(
            PerseusNavigatorImpl(
                stateHolder, resultBus, entryRegistry, viewModelStore,
            )
        )
    }

    /** Creates only the public navigator API. Prefer [createController] for new host integrations. */
    public fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<FragmentProviderMarker>,
        sceneProviders: List<ComposeSceneProvider<*>>,
        fragmentEntryFactory: FragmentEntryFactory? = null,
    ): PerseusNavigator = createController(
        composeProviders = composeProviders,
        fragmentProviders = fragmentProviders,
        sceneProviders = sceneProviders,
        fragmentEntryFactory = fragmentEntryFactory,
    ).navigator
}
