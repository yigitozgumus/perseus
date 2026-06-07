package com.yigitozgumus.perseus

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class PerseusNavigatorFactoryTest {

    @Test
    fun createControllerProvidesStableNavigatorApi() {
        val controller = PerseusNavigatorFactory.createController(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
        )

        assertNotNull(controller.navigator)
        assertSame(controller.navigator, controller.navigator)
    }
}
