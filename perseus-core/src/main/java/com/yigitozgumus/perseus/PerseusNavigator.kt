package com.yigitozgumus.perseus
import com.yigitozgumus.perseus.key.RouterKey

import com.yigitozgumus.perseus.key.GroupName


import androidx.lifecycle.LifecycleOwner

/**
 * Core navigator interface for Perseus.
 *
 * Drop-in replacement for Medusa's [Navigator], adapted for key-based routing.
 * All navigation is driven by [RouterKey] — fragments and composables are resolved
 * via the entry provider registry, not passed directly.
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
public interface PerseusNavigator {

    // ── Navigation ──────────────────────────────────────────────────────────

    /**
     * Navigates to the screen identified by [key].
     *
     * @param key The RouterKey identifying the target screen.
     * @param groupName Optional navigation group for [popUntil] clearing.
     * @return A [NavigationHandle] for observing results from this navigation.
     */
    public fun navigateTo(key: RouterKey, groupName: GroupName? = null): NavigationHandle

    /** Pops the current screen from the back stack. */
    public fun pop()

    /** Returns true if the back stack has more than one entry. */
    public fun canGoBack(): Boolean

    /**
     * Pops all screens in the specified navigation group from the current back stack.
     * The root entry is never removed.
     */
    public fun popUntil(groupName: GroupName)

    // ── Result Passing ──────────────────────────────────────────────────────

    /**
     * Sends a result from a child screen back to its parent.
     *
     * The result is routed to the [NavigationHandle] that matches the
     * correlation ID in the provided [context].
     */
    public fun <R : Any> sendResult(context: NavigationContext<*>, result: R)

    // ── Tab Management (authenticated state) ─────────────────────────────────

    /** Switches to the given tab index. Preserves per-tab back stack state. */
    public fun switchTab(tabIndex: Int)

    /**
     * Resets the specified tab to its root.
     *
     * @param tabIndex The tab to reset.
     * @param resetRoot If true, recreates the root entry. If false, keeps existing root.
     */
    public fun resetTab(tabIndex: Int, resetRoot: Boolean = false)

    /** Resets the current tab to its root. */
    public fun resetCurrentTab(resetRoot: Boolean = false)

    /**
     * Resets all tabs and navigation state with the given root keys.
     * Replaces Medusa's `resetWithFragmentProvider()`.
     */
    public fun resetAllWithKeys(keys: List<RouterKey>)

    /** The currently selected tab index. */
    public val currentTabIndex: Int

    // ── Auth State ─────────────────────────────────────────────────────────

    /** Transition to authenticated mode with the given tab root keys. */
    public fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)

    /** Start in unauthenticated mode with the given initial screen. */
    public fun startUnauthenticated(initialKey: RouterKey)

    // ── Observation ─────────────────────────────────────────────────────────

    /**
     * Observes destination changes in the current back stack.
     * Mirrors Medusa's `observeDestinationChanges()`.
     *
     * @param lifecycleOwner Controls the observer's lifecycle (auto-removed on destroy).
     * @param listener Called with the RouterKey that became visible.
     */
    public fun observeDestinationChanges(
        lifecycleOwner: LifecycleOwner,
        listener: (RouterKey) -> Unit
    )

    /**
     * Observes fragment/composable transitions between screens.
     * Mirrors Medusa's `observeFragmentTransaction()`.
     *
     * @param lifecycleOwner Controls the observer's lifecycle.
     * @param listener Called with the previous and next RouterKey on each transition.
     */
    public fun observeTransaction(
        lifecycleOwner: LifecycleOwner,
        listener: (previousKey: RouterKey?, nextKey: RouterKey?) -> Unit
    )
}
