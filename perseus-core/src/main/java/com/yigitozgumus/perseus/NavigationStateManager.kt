package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.RouterKey

/**
 * Manages navigation state transitions between auth modes.
 *
 * Use [transitionToAuthenticated] after login to show the tabbed interface.
 * Use [startUnauthenticated] or [resetToUnauthenticated] for login flows.
 */
public interface NavigationStateManager {
    /** Start in unauthenticated mode with the given initial screen. */
    public fun startUnauthenticated(initialKey: RouterKey)

    /** Transition to authenticated mode with the given tab root keys. */
    public fun transitionToAuthenticated(tabRootKeys: List<RouterKey>)

    /** Reset to unauthenticated mode (e.g., on logout). */
    public fun resetToUnauthenticated(initialKey: RouterKey)

    /** Whether the navigator is currently in authenticated (tabbed) mode. */
    public val isAuthenticated: Boolean
}
