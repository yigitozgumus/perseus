package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.interop.DefaultFragmentEntryFactory
import com.yigitozgumus.perseus.sample.compose.DetailScreenProvider
import com.yigitozgumus.perseus.sample.compose.DetailViewModel
import com.yigitozgumus.perseus.sample.compose.HomeScreenProvider
import com.yigitozgumus.perseus.sample.compose.HomeViewModel
import com.yigitozgumus.perseus.sample.compose.LoginScreenProvider
import com.yigitozgumus.perseus.sample.compose.SearchScreenProvider
import com.yigitozgumus.perseus.sample.fragment.ProfileFragmentProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.yigitozgumus.perseus.sample")
class SampleModule

val infrastructureModule = module {
    single<PerseusNavigator> {
        PerseusNavigatorFactory.create(
            composeProviders = listOf(
                HomeScreenProvider(), DetailScreenProvider(),
                LoginScreenProvider(), SearchScreenProvider(),
            ),
            fragmentProviders = listOf(ProfileFragmentProvider()),
            sceneProviders = emptyList(),
            fragmentEntryFactory = DefaultFragmentEntryFactory,
        )
    }

    factory { HomeViewModel(get()) }
    factory { DetailViewModel(get()) }
}
