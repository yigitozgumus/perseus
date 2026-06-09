package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.DefaultPerseusNavigator
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationState
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter
import com.yigitozgumus.perseus.provider.ComposeSceneProvider
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/** Creates an attached navigation owner for JVM tests without composing [PerseusNavHost]. */
public fun createTestPerseusNavigationOwner(
    initialScope: StackScopeSpec,
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    sceneProviders: List<ComposeSceneProvider<*>> = emptyList(),
): PerseusNavigationOwner {
    val state = PerseusNavigationState.fromSpec(initialScope)
    val stateHolder = PerseusNavigationStateHolder().also { it.attach(state) }
    val resultBus = ResultBusAdapter()
    val viewModelStore = PerseusViewModelStoreRegistry()
    val entryRegistry = PerseusEntryProviderRegistry(
        composeProviders = composeProviders,
        fragmentProviders = fragmentProviders,
        sceneProviders = sceneProviders,
        resultBus = resultBus,
        viewModelStoreProvider = viewModelStore,
    )
    return PerseusNavigationOwner(
        DefaultPerseusNavigator(stateHolder, resultBus, entryRegistry, viewModelStore)
    )
}

public fun PerseusNavigationOwner.currentBackStack(): List<com.yigitozgumus.perseus.key.RouterKey> =
    debugSnapshot().currentBackStack
