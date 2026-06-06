package com.yigitozgumus.perseus.api

import com.yigitozgumus.perseus.impl.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.impl.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.impl.PerseusNavigatorImpl
import com.yigitozgumus.perseus.impl.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.impl.ResultBusAdapter

object PerseusNavigatorFactory {

    fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<ScreenProvider<*>>,
        sceneProviders: List<ComposeSceneProvider<*>>
    ): PerseusNavigator {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStore = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(composeProviders, fragmentProviders, sceneProviders, resultBus)
        return PerseusNavigatorImpl(stateHolder, resultBus, entryRegistry, viewModelStore)
    }
}
