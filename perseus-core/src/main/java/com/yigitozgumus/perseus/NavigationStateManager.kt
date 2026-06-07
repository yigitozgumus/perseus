package com.yigitozgumus.perseus
import com.yigitozgumus.perseus.key.RouterKey


/**
 * Interface for managing navigation state transitions between auth modes.
 *
 * Used to transition between unauthenticated (login/onboarding) and
 * authenticated (tabbed main interface) navigation states.
 *
 * Usage:
 * ```kotlin
 * // After successful login:
 * stateManager.transitionToAuthenticated(listOf(HomeKey, SearchKey, ProfileKey))
 *
 * // On logout:
 * stateManager.resetToUnauthenticated(LoginKey)
 * ```
 */
public interface NavigationStateManager {
    /** Start in unauthenticated mode with the given initial screen. */
    public fun startUnauthenticated(initialKey: RouterKey)

    /** Transition to authenticated mode with tab root keys. */
    public fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)

    /** Reset to unauthenticated mode (e.g., logout). */
    public fun resetToUnauthenticated(initialKey: RouterKey)

    /** Whether the navigator is currently in authenticated (tabbed) mode. */
    public val isAuthenticated: Boolean
}
