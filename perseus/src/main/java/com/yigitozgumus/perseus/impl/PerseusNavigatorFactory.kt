package com.yigitozgumus.perseus.impl

import com.yigitozgumus.perseus.api.ComposeSceneProvider
import com.yigitozgumus.perseus.api.ComposeScreenProvider
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.ScreenProvider

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
