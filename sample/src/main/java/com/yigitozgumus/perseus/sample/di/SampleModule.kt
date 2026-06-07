package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.interop.DefaultFragmentEntryFactory
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
            fragmentEntryFactory = DefaultFragmentEntryFactory
        )
    }

    factory { HomeViewModel(get()) }
    factory { DetailViewModel(get()) }
}
