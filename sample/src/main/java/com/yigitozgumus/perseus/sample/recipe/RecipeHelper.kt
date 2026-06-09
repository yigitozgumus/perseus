package com.yigitozgumus.perseus.sample.recipe

import com.yigitozgumus.perseus.AndroidPerseusLogger
import com.yigitozgumus.perseus.PerseusLogLevel
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun createNavigationOwner(
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    fragmentEntryFactory: FragmentEntryFactory? = null,
): PerseusNavigationOwner {
    val app = koinApplication {
        modules(
            module {
                single<PerseusNavigationOwner> {
                    PerseusNavigatorFactory.create(
                        composeProviders = composeProviders,
                        fragmentProviders = fragmentProviders,
                        sceneProviders = emptyList(),
                        fragmentEntryFactory = fragmentEntryFactory,
                        logger = AndroidPerseusLogger(tag = "Perseus", level = PerseusLogLevel.Debug)
                    )
                }
            }
        )
    }
    return app.koin.get()
}

fun createNavigator(
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    fragmentEntryFactory: FragmentEntryFactory? = null,
): PerseusNavigator = createNavigationOwner(
    composeProviders = composeProviders,
    fragmentProviders = fragmentProviders,
    fragmentEntryFactory = fragmentEntryFactory,
).navigator
