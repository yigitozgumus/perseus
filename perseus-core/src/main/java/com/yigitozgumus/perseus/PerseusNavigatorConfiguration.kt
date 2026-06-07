package com.yigitozgumus.perseus

/**
 * Configuration for [PerseusNavigator].
 *
 * Mirrors Medusa's `NavigatorConfiguration` with the options relevant
 * for a Compose-based navigation system.
 */
public data class PerseusNavigatorConfiguration(
    /** The initial tab to display (0-based). Default is `0`. */
    public val initialTabIndex: Int = 0,
    /**
     * If `true`, pressing back on the initial tab exits the app.
     * If `false`, pressing back on any tab at root is blocked.
     */
    public val alwaysExitFromInitial: Boolean = false,
)
