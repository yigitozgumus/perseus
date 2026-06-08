package com.yigitozgumus.perseus

import org.junit.Assert.assertNotNull
import org.junit.Test

class PerseusNavigatorFactoryTest {

    @Test
    fun createProvidesNavigatorForHostAndCommands() {
        val navigator = PerseusNavigatorFactory.create(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
        )

        assertNotNull(navigator)
    }
}
