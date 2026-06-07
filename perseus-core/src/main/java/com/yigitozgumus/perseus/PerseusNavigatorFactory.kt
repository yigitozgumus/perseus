package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusNavigatorImpl
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter

object PerseusNavigatorFactory {

    fun create(
        composeProviders: List<ComposeScreenProvider<*>>,
        fragmentProviders: List<FragmentProviderMarker>,
        sceneProviders: List<ComposeSceneProvider<*>>,
        fragmentEntryFactory: (@Composable (FragmentProviderMarker, RouterKey, NavigationContext<RouterKey>) -> Unit)? = null
    ): PerseusNavigator {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStore = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders, fragmentProviders, sceneProviders, resultBus, fragmentEntryFactory
        )
        return PerseusNavigatorImpl(stateHolder, resultBus, entryRegistry, viewModelStore)
    }
}
