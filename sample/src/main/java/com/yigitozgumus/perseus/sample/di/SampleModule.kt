package com.yigitozgumus.perseus.sample.di

import com.yigitozgumus.perseus.AndroidPerseusLogger
import com.yigitozgumus.perseus.PerseusLogLevel
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.interop.DefaultFragmentEntryFactory
import com.yigitozgumus.perseus.sample.compose.DetailScreenProvider
import com.yigitozgumus.perseus.sample.compose.DetailViewModel
import com.yigitozgumus.perseus.sample.compose.HomeScreenProvider
import com.yigitozgumus.perseus.sample.compose.HomeViewModel
import com.yigitozgumus.perseus.sample.compose.LoginScreenProvider
import com.yigitozgumus.perseus.sample.compose.SearchScreenProvider
import com.yigitozgumus.perseus.sample.fragment.ProfileFragmentProvider
import com.yigitozgumus.perseus.sample.fragment.ProfileViewModel
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import org.koin.core.module.dsl.viewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.yigitozgumus.perseus.sample")
class SampleModule

val infrastructureModule = module {
    single<PerseusNavigationOwner> {
        PerseusNavigatorFactory.create(
            composeProviders = listOf(
                HomeScreenProvider(), DetailScreenProvider(),
                LoginScreenProvider(), SearchScreenProvider(),
            ),
            fragmentProviders = listOf(ProfileFragmentProvider()),
            sceneProviders = emptyList(),
            fragmentEntryFactory = DefaultFragmentEntryFactory,
            logger = AndroidPerseusLogger(tag = "Perseus", level = PerseusLogLevel.Debug)
        )
    }

    single<PerseusNavigator> { get<PerseusNavigationOwner>().navigator }
    single<PerseusScopeNavigator> { get<PerseusNavigationOwner>().scopeNavigator }

    // Compose sample: koinViewModel() reads Perseus' entry-scoped LocalViewModelStoreOwner.
    viewModel { HomeViewModel(get()) }
    viewModel { (key: DetailKey) -> DetailViewModel(get(), key) }

    // Fragment sample: by viewModel(ownerProducer = { requirePerseusViewModelStoreOwner() })
    // points Koin at the same Perseus entry-scoped owner.
    viewModel { (key: ProfileKey) -> ProfileViewModel(get(), key) }
}
