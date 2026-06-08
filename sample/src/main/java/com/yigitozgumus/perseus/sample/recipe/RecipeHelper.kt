package com.yigitozgumus.perseus.sample.recipe

import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun createNavigator(
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    fragmentEntryFactory: FragmentEntryFactory? = null,
): PerseusNavigator {
    val app = koinApplication {
        modules(
            module {
                single<PerseusNavigator> {
                    PerseusNavigatorFactory.create(
                        composeProviders = composeProviders,
                        fragmentProviders = fragmentProviders,
                        sceneProviders = emptyList(),
                        fragmentEntryFactory = fragmentEntryFactory,
                    )
                }
            }
        )
    }
    return app.koin.get()
}
