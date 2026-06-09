package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.createTestPerseusNavigationOwner
import com.yigitozgumus.perseus.currentBackStack
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusV3FeatureTest {

    @Test
    fun popToRootAliasesResetCurrentStack() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(V3Home))

        owner.navigator.navigateTo(V3Detail(1))
        owner.navigator.popToRoot()

        assertEquals(listOf(V3Home), owner.currentBackStack())
    }

    @Test
    fun popUntilKeyRemovesTargetAndEntriesAboveIt() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(V3Home))

        owner.navigator.navigateTo(V3Detail(1))
        owner.navigator.navigateTo(V3Detail(2))
        owner.navigator.popUntilKey(V3Detail(1))

        assertEquals(listOf(V3Home), owner.currentBackStack())
    }

    @Test
    fun rootBackCanBeBlockedOrLeftToHost() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(V3Home))

        assertTrue(owner.navigator.handleBack(PerseusBackBehavior(rootBackBehavior = RootBackBehavior.Block)))
        assertFalse(owner.navigator.handleBack(PerseusBackBehavior(rootBackBehavior = RootBackBehavior.ExitHost)))
    }

    @Test
    fun backAtNonInitialTabRootCanSwitchToInitialTab() {
        val owner = createTestPerseusNavigationOwner(MultiStackSpec(listOf(V3Home, V3Search), initialStackIndex = 1))

        val consumed = owner.navigator.handleBack(
            PerseusBackBehavior(tabBackBehavior = TabBackBehavior.SwitchToInitialTab)
        )

        assertTrue(consumed)
        assertEquals(0, owner.navigator.currentTabIndex)
    }

    @Test
    fun scopeResultHandleUsesPushedScopeIdAsCorrelation() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(V3Home))

        val handle = owner.scopeNavigator.pushScopeForResult(SingleStackSpec(V3Checkout))

        assertEquals(handle.scopeId.value, handle.correlationId)
    }
}

@Serializable
private data object V3Home : RouterKey

@Serializable
private data object V3Search : RouterKey

@Serializable
private data object V3Checkout : RouterKey

@Serializable
private data class V3Detail(val id: Int) : RouterKey
