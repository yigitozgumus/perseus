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
        val removed = stateHolder.state.goBack()
        if (removed != null) {
            entryRegistry.clearTrackingForKey(removed)
            viewModelStoreRegistry.clear(removed.backStackId())
        }
    }

    override fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        if (!stateHolder.isAttached) return
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        removed.forEach { key ->
            entryRegistry.clearTrackingForKey(key)
            viewModelStoreRegistry.clear(key.backStackId())
        }
    }

    // ── Result ──────────────────────────────────────────────────────────────

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        resultBus.send(context.correlationId, result)
    }

    // ── Tab Management ──────────────────────────────────────────────────────

    override fun switchTab(tabIndex: Int) { stateHolder.state.switchTab(tabIndex) }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        stateHolder.state.resetTab(tabIndex, resetRoot)
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        val removed = stateHolder.state.currentBackStack.toList().drop(1)
        stateHolder.state.resetCurrentTab(resetRoot)
        removed.forEach {
            entryRegistry.clearTrackingForKey(it)
            viewModelStoreRegistry.clear(it.backStackId())
        }
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        viewModelStoreRegistry.retainOnly(emptySet())
        entryRegistry.clearAllTracking()
        stateHolder.state.resetAllWithKeys(keys)
    }

    // ── Auth State ─────────────────────────────────────────────────────────

    override fun transitionToAuthenticated(tabRootKeys: List<RouterKey>) {
        stateHolder.transitionToAuthenticated(tabRootKeys)
    }

    override fun startUnauthenticated(initialKey: RouterKey) {
        stateHolder.startUnauthenticated(initialKey)
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
