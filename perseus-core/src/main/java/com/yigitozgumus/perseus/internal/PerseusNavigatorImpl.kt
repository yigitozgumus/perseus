package com.yigitozgumus.perseus.internal

import androidx.compose.animation.ContentTransform
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import java.util.UUID

/**
 * Implementation of [PerseusNavigator] that drives [PerseusNavigationState].
 *
 * All navigation operations mutate the Composition-owned state via
 * [PerseusNavigationStateHolder]. The state is a [SnapshotStateList] so
 * NavDisplay recomposes automatically.
 *
 * Result passing goes through [ResultBusAdapter] — a SharedFlow-based event bus.
 * Group tracking is handled by [PerseusEntryProviderRegistry].
 */
internal class PerseusNavigatorImpl(
    internal val stateHolder: PerseusNavigationStateHolder,
    private val resultBus: ResultBusAdapter,
    internal val entryRegistry: PerseusEntryProviderRegistry,
    internal val viewModelStoreRegistry: PerseusViewModelStoreRegistry
) : PerseusNavigator {

    init {
        entryRegistry.onPopCallback = { pop() }
    }

    override val currentTabIndex: Int get() = stateHolder.currentTabIndex

    // ── Navigation ──────────────────────────────────────────────────────────

    override fun navigateTo(
        key: RouterKey,
        groupName: GroupName?,
        transition: ContentTransform?,
    ): NavigationHandle {
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = stateHolder.state.createBackStackKey(key)

        if (groupName != null) entryRegistry.setPendingGroup(backStackKey, groupName)
        entryRegistry.setPendingCorrelationId(backStackKey, correlationId)
        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        stateHolder.state.navigateTo(backStackKey)
        return resultBus.createHandle(correlationId)
    }

    override fun pop() {
        if (!stateHolder.isAttached) return
        cleanupRemoved(listOfNotNull(stateHolder.state.goBack()))
    }

    override fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        if (!stateHolder.isAttached) return
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
    }

    // ── Result ──────────────────────────────────────────────────────────────

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        resultBus.send(context.correlationId, result)
    }

    // ── Tab Management ──────────────────────────────────────────────────────

    override fun switchTab(tabIndex: Int) { stateHolder.state.switchTab(tabIndex) }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetTab(tabIndex, resetRoot))
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetCurrentTab(resetRoot))
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetAllWithKeys(keys))
        entryRegistry.clearAllTracking()
    }

    // ── Auth State ─────────────────────────────────────────────────────────

    override fun transitionToAuthenticated(tabRootKeys: List<RouterKey>) {
        if (stateHolder.isAttached) {
            cleanupRemoved(stateHolder.state.transitionToAuthenticated(tabRootKeys))
            entryRegistry.clearAllTracking()
        } else {
            stateHolder.transitionToAuthenticated(tabRootKeys)
        }
    }

    override fun startUnauthenticated(initialKey: RouterKey) {
        if (stateHolder.isAttached) {
            cleanupRemoved(stateHolder.state.startUnauthenticated(initialKey))
            entryRegistry.clearAllTracking()
        } else {
            stateHolder.startUnauthenticated(initialKey)
        }
    }

    private fun cleanupRemoved(removed: List<RouterKey>) {
        removed.forEach { key ->
            entryRegistry.clearTrackingForKey(key)
            viewModelStoreRegistry.clear(key.backStackId())
        }
    }

    // ── Observation ─────────────────────────────────────────────────────────

    override fun observeDestinationChanges(
        lifecycleOwner: LifecycleOwner,
        listener: (RouterKey) -> Unit
    ) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Observe composition-driven changes via state snapshot
            // For v1: simplistic approach — listeners are notified by the host composable
        })
    }

    override fun observeTransaction(
        lifecycleOwner: LifecycleOwner,
        listener: (previousKey: RouterKey?, nextKey: RouterKey?) -> Unit
    ) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // For v1: handled by the host composable
        })
    }
}
