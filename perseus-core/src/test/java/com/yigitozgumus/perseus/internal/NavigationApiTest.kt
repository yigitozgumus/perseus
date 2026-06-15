package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.LaunchMode
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.PerseusResult
import com.yigitozgumus.perseus.PopUpTo
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.createTestPerseusNavigationOwner
import com.yigitozgumus.perseus.currentBackStack
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.shouldInstallPerseusBackHandler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationApiTest {

    @Test
    fun popToRootAliasesResetCurrentStack() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.popToRoot()

        assertEquals(listOf(NavigationHome), owner.currentBackStack())
    }

    @Test
    fun popUntilKeyRemovesTargetAndEntriesAboveIt() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.navigateTo(NavigationDetail(2))
        owner.navigator.popUntilKey(NavigationDetail(1))

        assertEquals(listOf(NavigationHome), owner.currentBackStack())
    }

    @Test
    fun popUntilKeyTypeRemovesTopMostMatchingEntryAndEntriesAboveIt() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.navigateTo(NavigationSearch)
        owner.navigator.navigateTo(NavigationDetail(2))
        owner.navigator.navigateTo(NavigationCheckout)
        owner.navigator.popUntilKeyType(NavigationDetail::class)

        assertEquals(listOf(NavigationHome, NavigationDetail(1), NavigationSearch), owner.currentBackStack())
    }

    @Test
    fun rootBackCanBeBlockedOrLeftToHost() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        assertTrue(owner.navigator.handleBack(PerseusBackBehavior(rootBackBehavior = RootBackBehavior.Block)))
        assertFalse(owner.navigator.handleBack(PerseusBackBehavior(rootBackBehavior = RootBackBehavior.ExitHost)))
    }

    @Test
    fun backAtNonInitialTabRootCanSwitchToInitialTab() {
        val owner = createTestPerseusNavigationOwner(MultiStackSpec(listOf(NavigationHome, NavigationSearch), initialStackIndex = 1))

        val consumed = owner.navigator.handleBack(
            PerseusBackBehavior(tabBackBehavior = TabBackBehavior.SwitchToInitialTab)
        )

        assertTrue(consumed)
        assertEquals(0, owner.navigator.currentTabIndex)
    }

    @Test
    fun scopeResultHandleUsesPushedScopeIdAsCorrelation() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        val handle = owner.scopeNavigator.pushScopeForResult(SingleStackSpec(NavigationCheckout))

        assertEquals(handle.scopeId.value, handle.correlationId)
    }

    @Test
    fun currentKeyStateFlowTracksVisibleRouteKey() {
        val owner = createTestPerseusNavigationOwner(MultiStackSpec(listOf(NavigationHome, NavigationSearch)))

        assertEquals(NavigationHome, owner.navigator.currentKey.value)

        owner.navigator.navigateTo(NavigationDetail(1))
        assertEquals(NavigationDetail(1), owner.navigator.currentKey.value)

        owner.navigator.switchTab(1)
        assertEquals(NavigationSearch, owner.navigator.currentKey.value)

        owner.navigator.switchTab(0)
        owner.navigator.pop()
        assertEquals(NavigationHome, owner.navigator.currentKey.value)
    }

    @Test
    fun singleTopReusesCurrentEntryForSameRouteKey() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.navigateTo(NavigationDetail(1), launchMode = LaunchMode.SingleTop)

        assertEquals(listOf(NavigationHome, NavigationDetail(1)), owner.currentBackStack())
    }

    @Test
    fun replaceWithSwapsCurrentEntry() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.replaceWith(NavigationSearch)

        assertEquals(listOf(NavigationHome, NavigationSearch), owner.currentBackStack())
    }

    @Test
    fun navigateWithPopUpToRootClearsIntermediateEntriesBeforePush() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.navigateTo(NavigationSearch)
        owner.navigator.navigateTo(NavigationCheckout, popUpTo = PopUpTo.Root)

        assertEquals(listOf(NavigationHome, NavigationCheckout), owner.currentBackStack())
    }

    @Test
    fun removedEntryCompletesHandleAsCancelled() = runBlocking {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))

        val handle = owner.navigator.navigateTo(NavigationDetail(1))
        owner.navigator.pop()

        assertEquals(PerseusResult.Cancelled, withTimeout(1_000) { handle.awaitResult(NavigationResult::class) })
    }

    @Test
    fun hostBackHandlerInstallsOnlyForRootBackPoliciesThatConsume() {
        assertFalse(
            shouldInstallPerseusBackHandler(
                currentBackStackSize = 2,
                isMultiStack = false,
                currentTabIndex = 0,
                behavior = PerseusBackBehavior(rootBackBehavior = RootBackBehavior.Block),
            )
        )
        assertTrue(
            shouldInstallPerseusBackHandler(
                currentBackStackSize = 1,
                isMultiStack = false,
                currentTabIndex = 0,
                behavior = PerseusBackBehavior(rootBackBehavior = RootBackBehavior.Block),
            )
        )
        assertFalse(
            shouldInstallPerseusBackHandler(
                currentBackStackSize = 1,
                isMultiStack = false,
                currentTabIndex = 0,
                behavior = PerseusBackBehavior(rootBackBehavior = RootBackBehavior.ExitHost),
            )
        )
        assertTrue(
            shouldInstallPerseusBackHandler(
                currentBackStackSize = 1,
                isMultiStack = true,
                currentTabIndex = 1,
                behavior = PerseusBackBehavior(tabBackBehavior = TabBackBehavior.SwitchToInitialTab),
            )
        )
        assertTrue(
            shouldInstallPerseusBackHandler(
                currentBackStackSize = 1,
                isMultiStack = true,
                currentTabIndex = 0,
                behavior = PerseusBackBehavior(tabBackBehavior = TabBackBehavior.ResetCurrentTab),
            )
        )
    }

    @Test
    fun navigationMutationFailsWhenNotOnMainThread() {
        val owner = createTestPerseusNavigationOwner(SingleStackSpec(NavigationHome))
        val original = MainThreadGuard.isMainThread
        MainThreadGuard.isMainThread = { false }

        val error = try {
            runCatching { owner.navigator.navigateTo(NavigationDetail(1)) }.exceptionOrNull()
        } finally {
            MainThreadGuard.isMainThread = original
        }

        assertTrue(error is IllegalStateException)
        assertEquals("Perseus navigation mutations must be called on the main thread.", error?.message)
    }
}

@Serializable
private data object NavigationHome : NavigationKey

@Serializable
private data object NavigationSearch : NavigationKey

@Serializable
private data object NavigationCheckout : NavigationKey

@Serializable
private data class NavigationDetail(val id: Int) : NavigationKey

private data class NavigationResult(val value: String)
