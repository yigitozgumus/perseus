package com.yigitozgumus.perseus.api

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
interface NavigationStateManager {
    /** Start in unauthenticated mode with the given initial screen. */
    fun startUnauthenticated(initialKey: RouterKey)

    /** Transition to authenticated mode with tab root keys. */
    fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)

    /** Reset to unauthenticated mode (e.g., logout). */
    fun resetToUnauthenticated(initialKey: RouterKey)

    /** Whether the navigator is currently in authenticated (tabbed) mode. */
    val isAuthenticated: Boolean
}
