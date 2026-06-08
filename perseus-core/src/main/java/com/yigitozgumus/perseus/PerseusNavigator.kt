package com.yigitozgumus.perseus

import androidx.compose.animation.ContentTransform
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusViewModelStoreRegistry
import com.yigitozgumus.perseus.internal.ResultBusAdapter
import com.yigitozgumus.perseus.internal.backStackId
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import java.util.UUID

/**
 * Core navigator for Perseus.
 *
 * All navigation is driven by [RouterKey] — fragments and composables are resolved
 * via the entry provider registry, not passed directly. Pass this same instance to
 * [PerseusNavHost] and to ViewModels/screens that need to issue navigation commands.
 *
 * ## Medusa Mapping
 *
 * | Medusa | Perseus |
 * |--------|---------|
 * | `start(fragment, groupName)` | `navigateTo(key, groupName)` |
 * | `goBack()` | `pop()` |
 * | `switchTab(index)` | `switchTab(index)` |
 * | `reset(index, resetRoot)` | `resetTab(index, resetRoot)` |
 * | `resetCurrentTab(resetRoot)` | `resetCurrentTab(resetRoot)` |
 * | `reset()` / `resetWithFragmentProvider()` | `resetAllWithKeys(keys)` |
 * | `clearGroup(name)` | `popUntil(groupName)` |
 * | `observeDestinationChanges(...)` | `observeDestinationChanges(...)` |
 */
public class PerseusNavigator internal constructor(
    internal val stateHolder: PerseusNavigationStateHolder,
    private val resultBus: ResultBusAdapter,
    internal val entryRegistry: PerseusEntryProviderRegistry,
    internal val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
) {

    init {
        entryRegistry.onPopCallback = { pop() }
    }

    /** Snapshot of the active stack scope. */
    public val currentScope: StackScopeSnapshot get() = stateHolder.state.currentScope

    /** The currently selected tab index. */
    public val currentTabIndex: Int get() = stateHolder.currentTabIndex

    /**
     * Navigates to the screen identified by [key].
     *
     * @param key The RouterKey identifying the target screen.
     * @param groupName Optional navigation group for [popUntil] clearing.
     * @param transition Optional [ContentTransform] for this navigation only.
     * @return A [NavigationHandle] for observing results from this navigation.
     */
    public fun navigateTo(
        key: RouterKey,
        groupName: GroupName? = null,
        transition: ContentTransform? = null,
    ): NavigationHandle {
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = stateHolder.state.createBackStackKey(
            key = key,
            groupName = groupName,
            correlationId = correlationId,
        )

        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        stateHolder.state.navigateTo(backStackKey)
        return resultBus.createHandle(correlationId)
    }

    /** Pops the current screen from the back stack. */
    public fun pop(): Unit {
        if (!stateHolder.isAttached) return
        cleanupRemoved(listOfNotNull(stateHolder.state.goBack()))
    }

    /** Returns true if the back stack has more than one entry. */
    public fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    /**
     * Pops all screens in the specified navigation group from the current back stack.
     * The root entry is never removed.
     */
    public fun popUntil(groupName: GroupName): Unit {
        if (!stateHolder.isAttached) return
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
    }

    /**
     * Sends a result from a child screen back to its parent.
     *
     * The result is routed to the [NavigationHandle] that matches the
     * correlation ID in the provided [context].
     */
    public fun <R : Any> sendResult(context: NavigationContext<*>, result: R): Unit {
        resultBus.send(context.correlationId, result)
    }

    /** Switches to the given tab index. Preserves per-tab back stack state. */
    public fun switchTab(tabIndex: Int): Unit {
        stateHolder.state.switchTab(tabIndex)
    }

    /**
     * Resets the specified tab to its root.
     *
     * @param tabIndex The tab to reset.
     * @param resetRoot If true, recreates the root entry. If false, keeps existing root.
     */
    public fun resetTab(tabIndex: Int, resetRoot: Boolean = false): Unit {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetTab(tabIndex, resetRoot))
    }

    /** Resets the current tab to its root. */
    public fun resetCurrentTab(resetRoot: Boolean = false): Unit {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetCurrentTab(resetRoot))
    }

    /**
     * Resets all tabs and navigation state with the given root keys.
     * Replaces Medusa's `resetWithFragmentProvider()`.
     */
    public fun resetAllWithKeys(keys: List<RouterKey>): Unit {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetAllWithKeys(keys))
        entryRegistry.clearAllTracking()
    }

    /** Replaces the root scope and removes all existing scopes. */
    public fun setRootScope(scope: StackScopeSpec): Unit {
        cleanupRemoved(stateHolder.setRootScope(scope))
        entryRegistry.clearAllTracking()
    }

    /** Replaces the current top scope. */
    public fun replaceCurrentScope(scope: StackScopeSpec): Unit {
        if (!stateHolder.isAttached) {
            setRootScope(scope)
            return
        }
        cleanupRemoved(stateHolder.state.replaceCurrentScope(scope))
    }

    /** Pushes a new scope above the current scope and returns its id. */
    public fun pushScope(scope: StackScopeSpec): StackScopeId {
        check(stateHolder.isAttached) { "PerseusNavigationState not attached. Call pushScope after PerseusNavHost is composed." }
        return stateHolder.state.pushScope(scope)
    }

    /** Removes a non-root scope and cleans all entries owned by it. */
    public fun removeScope(scopeId: StackScopeId): Unit {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.removeScope(scopeId))
    }

    /** Transition to authenticated mode with the given tab root keys. */
    public fun transitionToAuthenticated(tabRootKeys: List<RouterKey>): Unit {
        if (stateHolder.isAttached) {
            cleanupRemoved(stateHolder.state.transitionToAuthenticated(tabRootKeys))
            entryRegistry.clearAllTracking()
        } else {
            stateHolder.transitionToAuthenticated(tabRootKeys)
        }
    }

    /** Start in unauthenticated mode with the given initial screen. */
    public fun startUnauthenticated(initialKey: RouterKey): Unit {
        if (stateHolder.isAttached) {
            cleanupRemoved(stateHolder.state.startUnauthenticated(initialKey))
            entryRegistry.clearAllTracking()
        } else {
            stateHolder.startUnauthenticated(initialKey)
        }
    }

    private fun cleanupRemoved(removed: List<RouterKey>): Unit {
        removed.forEach { key ->
            entryRegistry.clearTrackingForKey(key)
            viewModelStoreRegistry.clear(key.backStackId())
        }
    }

    /**
     * Observes destination changes in the current back stack.
     * Mirrors Medusa's `observeDestinationChanges()`.
     *
     * @param lifecycleOwner Controls the observer's lifecycle (auto-removed on destroy).
     * @param listener Called with the RouterKey that became visible.
     */
    public fun observeDestinationChanges(
        lifecycleOwner: LifecycleOwner,
        listener: (RouterKey) -> Unit,
    ): Unit {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Observe composition-driven changes via state snapshot.
            // Implemented in a later v2 phase.
        })
    }

    /**
     * Observes fragment/composable transitions between screens.
     * Mirrors Medusa's `observeFragmentTransaction()`.
     *
     * @param lifecycleOwner Controls the observer's lifecycle.
     * @param listener Called with the previous and next RouterKey on each transition.
     */
    public fun observeTransaction(
        lifecycleOwner: LifecycleOwner,
        listener: (previousKey: RouterKey?, nextKey: RouterKey?) -> Unit,
    ): Unit {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            // Implemented in a later v2 phase.
        })
    }
}
