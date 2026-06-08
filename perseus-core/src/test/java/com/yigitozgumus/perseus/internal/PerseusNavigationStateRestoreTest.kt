package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
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
        state.switchStack(1)

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(tab0, tab1), restored.topLevelRoutes)
        restored.switchStack(1)
        assertEquals(tab1, restored.currentBackStack.first().routeKey())
    }

    private fun navigatorFor(state: PerseusNavigationState): PerseusNavigator {
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
        return PerseusNavigator(
            stateHolder = stateHolder,
            resultBus = resultBus,
            entryRegistry = entryRegistry,
            viewModelStoreRegistry = viewModelStoreRegistry,
        )
    }
}

private object RestoreGroup : GroupName("restore-group")

@Serializable
private data class RestoreKey(
    val id: Int,
    val label: String,
) : RouterKey
