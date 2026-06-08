package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.StackScopeKind
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PerseusNavigationStateRestoreTest {

    @Test
    fun snapshotRestoresDataClassKeysWithArguments() {
        val root = RestoreKey(id = 1, label = "root")
        val child = RestoreKey(id = 2, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.navigateTo(child)

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(2, restored.currentBackStack.size)
        assertEquals(root, restored.currentBackStack.first().routeKey())
        assertEquals(child, restored.currentBackStack.last().routeKey())
        assertEquals(
            state.currentBackStack.last().backStackId(),
            restored.currentBackStack.last().backStackId(),
        )
    }

    @Test
    fun snapshotRestoresDurableEntryMetadata() {
        val root = RestoreKey(id = 1, label = "root")
        val child = RestoreKey(id = 2, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.navigateTo(
            state.createBackStackKey(
                key = child,
                groupName = RestoreGroup,
                correlationId = "correlation-123",
            )
        )

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())
        val restoredChild = restored.currentBackStack.last()

        assertEquals(child, restoredChild.routeKey())
        assertEquals(RestoreGroup, restoredChild.groupName())
        assertEquals("correlation-123", restoredChild.correlationId())
    }

    @Test
    fun popUntilWorksAfterRestoringGroupMetadata() {
        val root = RestoreKey(id = 1, label = "root")
        val child = RestoreKey(id = 2, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.navigateTo(state.createBackStackKey(child, RestoreGroup, "correlation-123"))
        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())
        val navigator = navigatorFor(restored)

        navigator.popUntil(RestoreGroup)

        assertEquals(listOf(root), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun snapshotRestoresMultiStackTopLevelDataClassKeys() {
        val tab0 = RestoreKey(id = 10, label = "home")
        val tab1 = RestoreKey(id = 20, label = "profile")
        val state = PerseusNavigationState.singleStack(tab0)
        state.setRootScope(MultiStackSpec(listOf(tab0, tab1)))
        state.switchTab(1)

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(tab0, tab1), restored.topLevelRoutes)
        restored.switchTab(1)
        assertEquals(tab1, restored.currentBackStack.first().routeKey())
    }

    @Test
    fun snapshotRestoresPushedScopeAboveRootScope() {
        val root = RestoreKey(id = 1, label = "root")
        val flow = RestoreKey(id = 2, label = "flow")
        val child = RestoreKey(id = 3, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.pushScope(SingleStackSpec(flow))
        state.navigateTo(child)

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(StackScopeKind.SingleStack, restored.currentScope.kind)
        assertEquals(listOf(flow, child), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun popUntilWorksInRestoredPushedScope() {
        val root = RestoreKey(id = 1, label = "root")
        val flow = RestoreKey(id = 2, label = "flow")
        val child = RestoreKey(id = 3, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.pushScope(SingleStackSpec(flow))
        state.navigateTo(state.createBackStackKey(child, RestoreGroup, "correlation-456"))
        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())
        val navigator = navigatorFor(restored)

        navigator.popUntil(RestoreGroup)

        assertEquals(listOf(flow), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun removeRestoredPushedScopeReturnsToRootAndCleansStores() {
        val root = RestoreKey(id = 1, label = "root")
        val flow = RestoreKey(id = 2, label = "flow")
        val child = RestoreKey(id = 3, label = "child")
        val state = PerseusNavigationState.singleStack(root)
        state.pushScope(SingleStackSpec(flow))
        state.navigateTo(child)
        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())
        val pushedScopeId = restored.currentScope.id
        val pushedEntryIds = restored.currentBackStack.map { it.backStackId() }
        val fixture = navigatorFixture(restored)
        pushedEntryIds.forEach { fixture.viewModelStoreRegistry.getOwner(it) }

        fixture.navigator.removeScope(pushedScopeId)

        assertEquals(listOf(root), restored.currentBackStack.map { it.routeKey() })
        pushedEntryIds.forEach(::assertNoOwner)
    }

    private fun assertNoOwner(entryId: String) {
        try {
            PerseusViewModelStoreOwners.getOwner(entryId)
            fail("Expected no ViewModelStoreOwner for entryId=$entryId")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }

    private fun navigatorFor(state: PerseusNavigationState): PerseusNavigator =
        navigatorFixture(state).navigator

    private fun navigatorFixture(state: PerseusNavigationState): NavigatorFixture {
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
        return NavigatorFixture(navigator, viewModelStoreRegistry)
    }

    private data class NavigatorFixture(
        val navigator: PerseusNavigator,
        val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
    )
}

private object RestoreGroup : GroupName("restore-group")

@Serializable
private data class RestoreKey(
    val id: Int,
    val label: String,
) : RouterKey
