package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusNavigationStateRestoreTest {

    @Test
    fun snapshotRestoresDataClassKeysWithArguments() {
        val root = RestoreKey(id = 1, label = "root")
        val child = RestoreKey(id = 2, label = "child")
        val state = PerseusNavigationState.unauthenticated(root)
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
    fun snapshotRestoresAuthenticatedTopLevelDataClassKeys() {
        val tab0 = RestoreKey(id = 10, label = "home")
        val tab1 = RestoreKey(id = 20, label = "profile")
        val state = PerseusNavigationState.unauthenticated(tab0)
        state.transitionToAuthenticated(listOf(tab0, tab1))
        state.switchTab(1)

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertTrue(restored.isAuthenticated)
        assertEquals(listOf(tab0, tab1), restored.topLevelRoutes)
        restored.switchTab(1)
        assertEquals(tab1, restored.currentBackStack.first().routeKey())
    }
}

@Serializable
private data class RestoreKey(
    val id: Int,
    val label: String,
) : RouterKey
