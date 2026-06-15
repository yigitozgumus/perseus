package com.yigitozgumus.perseus

import androidx.compose.animation.ContentTransform
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.NavigationKey
import kotlinx.coroutines.flow.StateFlow

/**
 * Route and tab navigator for Perseus.
 *
 * All navigation is driven by [NavigationKey] — fragments and composables are resolved
 * via the entry provider registry, not passed directly.
 *
 * Scope/container ownership APIs live on [PerseusScopeNavigator] instead.
 */
public interface PerseusNavigator {
    /** The currently selected tab index in the current multi-stack scope. */
    public val currentTabIndex: Int

    /** Current visible route key, or null before the host attaches navigation state. */
    public val currentKey: StateFlow<NavigationKey?>

    /**
     * Navigates to the screen identified by [key].
     *
     * @param key The NavigationKey identifying the target screen.
     * @param groupName Optional navigation group for [popUntil] clearing.
     * @param transition Optional [ContentTransform] for this navigation only.
     * @param launchMode Controls whether an existing top entry can be reused.
     * @param popUpTo Entries to remove before adding [key].
     * @return A [NavigationHandle] for observing results from this navigation.
     */
    public fun navigateTo(
        key: NavigationKey,
        groupName: GroupName? = null,
        transition: ContentTransform? = null,
        launchMode: LaunchMode = LaunchMode.Standard,
        popUpTo: PopUpTo? = null,
    ): NavigationHandle

    /** Replaces the current entry with [key]. */
    public fun replaceWith(
        key: NavigationKey,
        groupName: GroupName? = null,
        transition: ContentTransform? = null,
    ): NavigationHandle

    /** Pops the current screen from the back stack. */
    public fun pop()

    /**
     * Handles a back press according to the current scope's back behavior.
     * Pass [behavior] to override the scope policy for this call only.
     * Returns true when consumed.
     */
    public fun handleBack(behavior: PerseusBackBehavior? = null): Boolean

    /** Returns true if the back stack has more than one entry. */
    public fun canGoBack(): Boolean

    /**
     * Pops all screens in the specified navigation group from the current back stack.
     * The root entry is never removed.
     */
    public fun popUntil(groupName: GroupName)

    /** Pops all entries above and including [key] in the current stack, keeping the root entry. */
    public fun popUntilKey(key: NavigationKey)

    /** Pops all entries above and including the top-most entry whose route key is [K]. */
    public fun <K : NavigationKey> popUntilKeyType(keyClass: kotlin.reflect.KClass<K>)

    /**
     * Sends a result from a child screen back to its parent.
     *
     * The result is routed to the [NavigationHandle] that matches the
     * correlation ID in the provided [context].
     */
    public fun <R : Any> sendResult(context: NavigationContext<*>, result: R)

    /** Switches to the given tab index in the current multi-stack scope. */
    public fun switchTab(tabIndex: Int)

    /**
     * Resets the specified tab to its root.
     *
     * @param tabIndex The tab to reset.
     * @param resetRoot If true, recreates the root entry. If false, keeps existing root.
     */
    public fun resetTab(tabIndex: Int, resetRoot: Boolean = false)

    /** Resets the current stack to its root. */
    public fun resetCurrentTab(resetRoot: Boolean = false)

    /**
     * Resets all stacks and navigation state with the given root keys.
     * Replaces Medusa's `resetWithFragmentProvider()`.
     */
    public fun resetAllWithKeys(keys: List<NavigationKey>)
}
