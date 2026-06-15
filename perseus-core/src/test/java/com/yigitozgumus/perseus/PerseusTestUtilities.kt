package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.DefaultPerseusNavigator
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationState
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter
import com.yigitozgumus.perseus.provider.SceneProvider
import com.yigitozgumus.perseus.provider.ScreenProvider
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/** Creates an attached navigation owner for JVM tests without composing [PerseusNavHost]. */
internal fun createTestPerseusNavigationOwner(
    initialScope: StackScopeSpec,
    composeProviders: List<ScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    sceneProviders: List<SceneProvider<*>> = emptyList(),
    logger: PerseusLogger = EmptyPerseusLogger,
): PerseusNavigationOwner {
    val state = PerseusNavigationState.fromSpec(initialScope)
    val stateHolder = PerseusNavigationStateHolder().also { it.attach(state) }
    val resultBus = ResultBusAdapter()
    val viewModelStore = PerseusViewModelStoreRegistry(logger)
    val entryRegistry = PerseusEntryProviderRegistry(
        composeProviders = composeProviders,
        fragmentProviders = fragmentProviders,
        sceneProviders = sceneProviders,
        resultBus = resultBus,
        viewModelStoreProvider = viewModelStore,
        logger = logger,
    )
    return PerseusNavigationOwner(
        DefaultPerseusNavigator(stateHolder, resultBus, entryRegistry, viewModelStore, logger = logger)
    )
}

internal fun PerseusNavigationOwner.currentBackStack(): List<com.yigitozgumus.perseus.key.NavigationKey> =
    debugSnapshot().currentBackStack
