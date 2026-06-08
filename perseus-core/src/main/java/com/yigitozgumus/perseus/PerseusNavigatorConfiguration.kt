package com.yigitozgumus.perseus

/**
 * Configuration for [PerseusNavigator].
 *
 * Mirrors Medusa's `NavigatorConfiguration` with the options relevant
 * for a Compose-based navigation system.
 */
public data class PerseusNavigatorConfiguration(
    /** The initial stack to display in a multi-stack scope (0-based). Default is `0`. */
    public val initialStackIndex: Int = 0,
    /**
     * If `true`, pressing back on the initial stack exits the app.
     * If `false`, pressing back on any stack at root is blocked.
     */
    public val alwaysExitFromInitial: Boolean = false,
)
