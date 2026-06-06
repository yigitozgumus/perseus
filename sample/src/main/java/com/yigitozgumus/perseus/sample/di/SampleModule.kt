package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.impl.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.impl.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.impl.PerseusNavigatorFactory
import com.yigitozgumus.perseus.impl.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.sample.compose.DetailViewModel
import com.yigitozgumus.perseus.sample.compose.HomeViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.yigitozgumus.perseus.sample")
class SampleModule

val infrastructureModule = module {
    single {
        PerseusNavigatorFactory.create(
            composeProviders = getAll(),
            fragmentProviders = getAll(),
            sceneProviders = emptyList()
        )
    }
    single<PerseusNavigator> { get<PerseusNavigatorFactory.Dependencies>().navigator }
    single<PerseusNavigationStateHolder> { get<PerseusNavigatorFactory.Dependencies>().stateHolder }
    single<PerseusViewModelStoreRegistry> { get<PerseusNavigatorFactory.Dependencies>().viewModelStoreRegistry }
    single<PerseusEntryProviderRegistry> { get<PerseusNavigatorFactory.Dependencies>().entryRegistry }

    factory { HomeViewModel(get()) }
    factory { DetailViewModel(get()) }
}
