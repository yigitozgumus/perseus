package com.yigitozgumus.perseus.api

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
interface PerseusNavigator {

    // ── Navigation ──────────────────────────────────────────────────────────

    /**
     * Navigates to the screen identified by [key].
     *
     * @param key The RouterKey identifying the target screen.
     * @param groupName Optional navigation group for [popUntil] clearing.
     * @return A [NavigationHandle] for observing results from this navigation.
     */
    fun navigateTo(key: RouterKey, groupName: GroupName? = null): NavigationHandle

    /** Pops the current screen from the back stack. */
    fun pop()

    /** Returns true if the back stack has more than one entry. */
    fun canGoBack(): Boolean

    /**
     * Pops all screens in the specified navigation group from the current back stack.
     * The root entry is never removed.
     */
    fun popUntil(groupName: GroupName)

    // ── Result Passing ──────────────────────────────────────────────────────

    /**
     * Sends a result from a child screen back to its parent.
     *
     * The result is routed to the [NavigationHandle] that matches the
     * correlation ID in the provided [context].
     */
    fun <R : Any> sendResult(context: NavigationContext<*>, result: R)

    // ── Tab Management (authenticated state) ─────────────────────────────────

    /** Switches to the given tab index. Preserves per-tab back stack state. */
    fun switchTab(tabIndex: Int)

    /**
     * Resets the specified tab to its root.
     *
     * @param tabIndex The tab to reset.
     * @param resetRoot If true, recreates the root entry. If false, keeps existing root.
     */
    fun resetTab(tabIndex: Int, resetRoot: Boolean = false)

    /** Resets the current tab to its root. */
    fun resetCurrentTab(resetRoot: Boolean = false)

    /**
     * Resets all tabs and navigation state with the given root keys.
     * Replaces Medusa's `resetWithFragmentProvider()`.
     */
    fun resetAllWithKeys(keys: List<RouterKey>)

    /** The currently selected tab index. */
    val currentTabIndex: Int

    // ── Observation ─────────────────────────────────────────────────────────

    /**
     * Observes destination changes in the current back stack.
     * Mirrors Medusa's `observeDestinationChanges()`.
     *
     * @param lifecycleOwner Controls the observer's lifecycle (auto-removed on destroy).
     * @param listener Called with the RouterKey that became visible.
     */
    fun observeDestinationChanges(
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
    fun observeTransaction(
        lifecycleOwner: LifecycleOwner,
        listener: (previousKey: RouterKey?, nextKey: RouterKey?) -> Unit
    )
}
