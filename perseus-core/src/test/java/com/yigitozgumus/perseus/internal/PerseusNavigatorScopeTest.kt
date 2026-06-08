package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.StackScopeKind
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class PerseusNavigatorScopeTest {

    @Test
    fun setRootScopeReplacesSingleRootWithMultiRoot() {
        val fixture = navigatorFixture(ScopeLogin)

        fixture.navigator.setRootScope(MultiStackSpec(listOf(ScopeHome, ScopeSearch)))

        assertEquals(StackScopeKind.MultiStack, fixture.navigator.currentScope.kind)
        assertEquals(listOf(ScopeHome, ScopeSearch), fixture.navigator.currentScope.rootKeys)
        assertEquals(0, fixture.navigator.currentScope.currentStackIndex)
        assertEquals(ScopeHome, fixture.state.currentBackStack.single().routeKey())
    }

    @Test
    fun setRootScopeReplacesMultiRootWithSingleRoot() {
        val fixture = navigatorFixture(ScopeLogin)
        fixture.navigator.setRootScope(MultiStackSpec(listOf(ScopeHome, ScopeSearch)))

        fixture.navigator.setRootScope(SingleStackSpec(ScopeLogin))

        assertEquals(StackScopeKind.SingleStack, fixture.navigator.currentScope.kind)
        assertNull(fixture.navigator.currentScope.currentStackIndex)
        assertEquals(listOf(ScopeLogin), fixture.navigator.currentScope.currentBackStack)
    }

    @Test
    fun pushAndRemoveScopeRestoresPreviousScopeAndCleansEntries() {
        val fixture = navigatorFixture(ScopeHome)
        val rootScopeId = fixture.navigator.currentScope.id

        val pushedScopeId = fixture.navigator.pushScope(SingleStackSpec(ScopeCheckout))
        fixture.navigator.navigateTo(ScopeChild)
        val pushedEntryIds = fixture.state.currentBackStack.map { it.backStackId() }
        pushedEntryIds.forEach { fixture.viewModelStoreRegistry.getOwner(it) }

        fixture.navigator.removeScope(pushedScopeId)

        assertEquals(rootScopeId, fixture.navigator.currentScope.id)
        assertEquals(listOf(ScopeHome), fixture.navigator.currentScope.currentBackStack)
        pushedEntryIds.forEach(::assertNoOwner)
    }

    private fun navigatorFixture(initialKey: RouterKey): NavigatorFixture {
        val state = PerseusNavigationState.singleStack(initialKey)
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
private data object ScopeLogin : RouterKey

@Serializable
private data object ScopeHome : RouterKey

@Serializable
private data object ScopeSearch : RouterKey

@Serializable
private data object ScopeCheckout : RouterKey

@Serializable
private data object ScopeChild : RouterKey
