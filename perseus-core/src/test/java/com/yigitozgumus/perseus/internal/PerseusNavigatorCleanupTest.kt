package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PerseusNavigatorCleanupTest {

    @Test
    fun resetTabCleansViewModelStoresForRemovedEntriesOnNonCurrentTab() {
        val fixture = navigatorFixture(CleanupTab0)
        fixture.navigator.transitionToAuthenticated(listOf(CleanupTab0, CleanupTab1))
        fixture.navigator.switchTab(1)
        fixture.navigator.navigateTo(CleanupChild)
        val childEntryId = fixture.state.currentBackStack.last().backStackId()
        fixture.viewModelStoreRegistry.getOwner(childEntryId)

        fixture.navigator.switchTab(0)
        fixture.navigator.resetTab(tabIndex = 1, resetRoot = false)

        assertNoOwner(childEntryId)
        fixture.navigator.switchTab(1)
        assertEquals(1, fixture.state.currentBackStack.size)
        assertEquals(CleanupTab1, fixture.state.currentBackStack.first().routeKey())
    }

    @Test
    fun startUnauthenticatedCleansExistingEntryStores() {
        val fixture = navigatorFixture(CleanupTab0)
        fixture.navigator.navigateTo(CleanupChild)
        val entryIds = fixture.state.currentBackStack.map { it.backStackId() }
        entryIds.forEach { fixture.viewModelStoreRegistry.getOwner(it) }

        fixture.navigator.startUnauthenticated(CleanupLogin)

        entryIds.forEach(::assertNoOwner)
        assertEquals(CleanupLogin, fixture.state.currentBackStack.single().routeKey())
    }

    private fun navigatorFixture(initialKey: RouterKey): NavigatorFixture {
        val state = PerseusNavigationState.unauthenticated(initialKey)
        val stateHolder = PerseusNavigationStateHolder().also { it.attach(state) }
        val resultBus = ResultBusAdapter()
        val viewModelStoreRegistry = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            resultBus = resultBus,
            viewModelStoreProvider = viewModelStoreRegistry,
        )
        val navigator = PerseusNavigator(
            stateHolder = stateHolder,
            resultBus = resultBus,
            entryRegistry = entryRegistry,
            viewModelStoreRegistry = viewModelStoreRegistry,
        )
        return NavigatorFixture(state, navigator, viewModelStoreRegistry)
    }

    private fun assertNoOwner(entryId: String) {
        try {
            PerseusViewModelStoreOwners.getOwner(entryId)
            fail("Expected no ViewModelStoreOwner for entryId=$entryId")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }

    private data class NavigatorFixture(
        val state: PerseusNavigationState,
        val navigator: PerseusNavigator,
        val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
    )
}

@Serializable
private data object CleanupTab0 : RouterKey

@Serializable
private data object CleanupTab1 : RouterKey

@Serializable
private data object CleanupChild : RouterKey

@Serializable
private data object CleanupLogin : RouterKey
