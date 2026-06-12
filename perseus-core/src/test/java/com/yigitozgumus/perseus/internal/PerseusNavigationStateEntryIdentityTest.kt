package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.key.NavigationKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusNavigationStateEntryIdentityTest {

    @Test
    fun duplicateRouteKeysGetDistinctBackStackEntryIds() {
        val state = PerseusNavigationState.singleStack(EntryIdentityKey)

        state.navigateTo(EntryIdentityKey)
        state.navigateTo(EntryIdentityKey)

        val ids = state.currentBackStack.map { it.backStackId() }

        assertEquals(3, ids.size)
        assertEquals(ids.distinct(), ids)
        state.currentBackStack.forEach { entry ->
            assertEquals(EntryIdentityKey, entry.routeKey())
        }
    }

    @Test
    fun entryIdIsStableForAnEntryUntilItIsRemoved() {
        val state = PerseusNavigationState.singleStack(EntryIdentityKey)
        val initialId = state.currentBackStack.last().backStackId()

        state.navigateTo(EntryIdentityKey)
        val pushedId = state.currentBackStack.last().backStackId()

        assertNotEquals(initialId, pushedId)
        assertEquals(pushedId, state.currentBackStack.last().backStackId())

        val removed = state.goBack()

        assertEquals(pushedId, removed?.backStackId())
        assertTrue(state.currentBackStack.none { it.backStackId() == pushedId })
    }
}

@Serializable
private data object EntryIdentityKey : NavigationKey
