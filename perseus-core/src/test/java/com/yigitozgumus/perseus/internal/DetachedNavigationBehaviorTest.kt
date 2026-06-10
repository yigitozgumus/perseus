package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.EmptyPerseusLogger
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetachedNavigationBehaviorTest {

    @Test
    fun setRootScopeBeforeAttachIsReplayedAfterAttach() {
        val fixture = detachedNavigatorFixture()

        fixture.navigator.setRootScope(SingleStackSpec(DetachedHome))
        val state = PerseusNavigationState.singleStack(DetachedLogin)
        fixture.stateHolder.attach(state)
        fixture.navigator.syncCurrentKey()

        assertEquals(listOf(DetachedHome), state.currentBackStack.map { it.routeKey() })
        assertEquals(DetachedHome, fixture.navigator.currentKey.value)
    }

    @Test
    fun replaceAppBeforeAttachIsReplayedAfterAttach() {
        val fixture = detachedNavigatorFixture()

        fixture.navigator.replaceApp(MultiStackSpec(listOf(DetachedHome, DetachedSearch), initialStackIndex = 1))
        val state = PerseusNavigationState.singleStack(DetachedLogin)
        fixture.stateHolder.attach(state)
        fixture.navigator.syncCurrentKey()

        assertEquals(1, state.currentTabIndex)
        assertEquals(listOf(DetachedSearch), state.currentBackStack.map { it.routeKey() })
        assertEquals(DetachedSearch, fixture.navigator.currentKey.value)
    }

    @Test
    fun detachedStateAccessorsThrowClearError() {
        val fixture = detachedNavigatorFixture()

        assertDetachedFailure("currentScope") { fixture.navigator.currentScope }
        assertEquals(0, fixture.navigator.currentTabIndex)
        assertNull(fixture.navigator.currentKey.value)
    }

    @Test
    fun routeAndTabOperationsBeforeAttachThrowClearError() {
        val fixture = detachedNavigatorFixture()
        val groupName = GroupName("checkout")

        assertDetachedFailure("navigateTo") { fixture.navigator.navigateTo(DetachedHome) }
        assertDetachedFailure("pop") { fixture.navigator.pop() }
        assertDetachedFailure("handleBack") { fixture.navigator.handleBack(PerseusBackBehavior()) }
        assertDetachedFailure("canGoBack") { fixture.navigator.canGoBack() }
        assertDetachedFailure("popUntil") { fixture.navigator.popUntil(groupName) }
        assertDetachedFailure("popUntilKey") { fixture.navigator.popUntilKey(DetachedHome) }
        assertDetachedFailure("popUntilKeyType") { fixture.navigator.popUntilKeyType(DetachedHome::class) }
        assertDetachedFailure("switchTab") { fixture.navigator.switchTab(1) }
        assertDetachedFailure("resetTab") { fixture.navigator.resetTab(0) }
        assertDetachedFailure("resetCurrentTab") { fixture.navigator.resetCurrentTab() }
        assertDetachedFailure("popToRoot") { fixture.navigator.popToRoot() }
        assertDetachedFailure("popTabToRoot") { fixture.navigator.popTabToRoot(0) }
        assertDetachedFailure("popCurrentTabToRoot") { fixture.navigator.popCurrentTabToRoot() }
        assertDetachedFailure("resetAllWithKeys") { fixture.navigator.resetAllWithKeys(listOf(DetachedHome)) }
    }

    @Test
    fun scopeOperationsExceptRootReplacementBeforeAttachThrowClearError() {
        val fixture = detachedNavigatorFixture()
        val unknownScopeId = com.yigitozgumus.perseus.StackScopeId.create()

        assertDetachedFailure("replaceCurrentScope") {
            fixture.navigator.replaceCurrentScope(SingleStackSpec(DetachedHome))
        }
        assertDetachedFailure("pushScope") {
            fixture.navigator.pushScope(SingleStackSpec(DetachedHome))
        }
        assertDetachedFailure("pushScope") {
            fixture.navigator.pushScopeForResult(SingleStackSpec(DetachedHome))
        }
        assertDetachedFailure("removeScope") {
            fixture.navigator.removeScope(unknownScopeId)
        }
        assertDetachedFailure("removeScope") {
            fixture.navigator.removeScope(unknownScopeId, "result")
        }
    }

    private fun detachedNavigatorFixture(): DetachedNavigatorFixture {
        val stateHolder = PerseusNavigationStateHolder()
        val resultBus = ResultBusAdapter()
        val viewModelStoreRegistry = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            resultBus = resultBus,
            viewModelStoreProvider = viewModelStoreRegistry,
        )
        val navigator = DefaultPerseusNavigator(
            stateHolder = stateHolder,
            resultBus = resultBus,
            entryRegistry = entryRegistry,
            viewModelStoreRegistry = viewModelStoreRegistry,
            logger = EmptyPerseusLogger,
        )
        return DetachedNavigatorFixture(stateHolder, navigator)
    }

    private fun assertDetachedFailure(operationName: String, block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertTrue("Expected IllegalStateException for $operationName", error is IllegalStateException)
        assertTrue(
            "Expected operation name in message for $operationName but was: ${error?.message}",
            error?.message?.contains("PerseusNavigator.$operationName() called before PerseusNavHost attached") == true,
        )
        assertTrue(
            "Expected supported pre-host operations in message for $operationName but was: ${error?.message}",
            error?.message?.contains("Only setRootScope()/replaceApp()") == true,
        )
    }

    private data class DetachedNavigatorFixture(
        val stateHolder: PerseusNavigationStateHolder,
        val navigator: DefaultPerseusNavigator,
    )
}

@Serializable
private data object DetachedLogin : RouterKey

@Serializable
private data object DetachedHome : RouterKey

@Serializable
private data object DetachedSearch : RouterKey
