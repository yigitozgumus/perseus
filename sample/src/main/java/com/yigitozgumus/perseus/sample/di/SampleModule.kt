package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.api.ComposeScreenProvider
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.api.ScreenProvider
import com.yigitozgumus.perseus.impl.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.impl.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.impl.PerseusNavigatorImpl
import com.yigitozgumus.perseus.impl.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.impl.ResultBusAdapter
import com.yigitozgumus.perseus.sample.compose.DetailScreenProvider
import com.yigitozgumus.perseus.sample.compose.HomeScreenProvider
import com.yigitozgumus.perseus.sample.compose.LoginScreenProvider
import com.yigitozgumus.perseus.sample.fragment.ProfileFragmentProvider
import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.dsl.module

val sampleModule = module {
    // Infrastructure (activity-scoped, wired with explicit provider lists)
    activityRetainedScope {
        scoped { PerseusNavigationStateHolder() }
        scoped { ResultBusAdapter() }
        scoped { PerseusViewModelStoreRegistry() }

        scoped {
            PerseusEntryProviderRegistry(
                composeProviders = listOf(
                    HomeScreenProvider(),
                    DetailScreenProvider(),
                    LoginScreenProvider()
                ),
                fragmentProviders = listOf(
                    ProfileFragmentProvider()
                ),
                sceneProviders = emptyList(),
                resultBus = get()
            )
        }

        scoped<PerseusNavigator> { PerseusNavigatorImpl(get(), get(), get(), get()) }
    }
}

fun createEntryProvider(registry: PerseusEntryProviderRegistry): (RouterKey) -> androidx.navigation3.runtime.NavEntry<RouterKey> = { key ->
    registry.provide(key)
}
