package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.RouterKey
import com.yigitozgumus.perseus.interop.FragmentEntry
import com.yigitozgumus.perseus.interop.ScreenProvider
import com.yigitozgumus.perseus.sample.compose.DetailViewModel
import com.yigitozgumus.perseus.sample.compose.HomeViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.yigitozgumus.perseus.sample")
class SampleModule

val infrastructureModule = module {
    single<PerseusNavigator> {
        PerseusNavigatorFactory.create(
            composeProviders = getAll(),
            fragmentProviders = getAll(),
            sceneProviders = emptyList(),
            fragmentEntryFactory = { provider, key, ctx ->
                @Suppress("UNCHECKED_CAST")
                FragmentEntry(key, provider as ScreenProvider<RouterKey>, ctx)
            }
        )
    }

    factory { HomeViewModel(get()) }
    factory { DetailViewModel(get()) }
}
