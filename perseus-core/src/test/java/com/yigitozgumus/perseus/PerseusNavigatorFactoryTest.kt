package com.yigitozgumus.perseus

import org.junit.Assert.assertNotNull
import org.junit.Test

class PerseusNavigatorFactoryTest {

    @Test
    fun createProvidesNavigationOwnerForHostAndCommands() {
        val navigationOwner = PerseusNavigatorFactory.create(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
        )

        assertNotNull(navigationOwner)
        assertNotNull(navigationOwner.navigator)
        assertNotNull(navigationOwner.scopeNavigator)
    }
}
