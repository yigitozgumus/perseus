package com.yigitozgumus.perseus.sample.recipe

import com.yigitozgumus.perseus.PerseusController
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import org.koin.dsl.koinApplication
import org.koin.dsl.module

fun createController(
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    fragmentEntryFactory: FragmentEntryFactory? = null,
): PerseusController {
    val app = koinApplication {
        modules(
            module {
                single<PerseusController> {
                    PerseusNavigatorFactory.createController(
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

fun createNavigator(
    composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
    fragmentProviders: List<FragmentProviderMarker> = emptyList(),
    fragmentEntryFactory: FragmentEntryFactory? = null,
): PerseusNavigator = createController(
    composeProviders = composeProviders,
    fragmentProviders = fragmentProviders,
    fragmentEntryFactory = fragmentEntryFactory,
).navigator
