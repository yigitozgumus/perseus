package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.impl.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.sample.compose.DetailViewModel
import com.yigitozgumus.perseus.sample.compose.HomeViewModel
import com.yigitozgumus.perseus.impl.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.impl.PerseusNavigatorImpl
import com.yigitozgumus.perseus.impl.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.impl.ResultBusAdapter
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.yigitozgumus.perseus.sample")
class SampleModule

/** Infrastructure dependencies not covered by @Single annotations. */
val infrastructureModule = module {
    single { PerseusNavigationStateHolder() }
    single { ResultBusAdapter() }
    single { PerseusViewModelStoreRegistry() }
    single<PerseusNavigator> { PerseusNavigatorImpl(get(), get(), get(), get()) }
    single {
        PerseusEntryProviderRegistry(
            composeProviders = getAll(),
            fragmentProviders = getAll(),
            sceneProviders = emptyList(),
            resultBus = get()
        )
    }

    // ViewModels
    factory { HomeViewModel(get()) }
    factory { DetailViewModel(get()) }
}
