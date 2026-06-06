package com.yigitozgumus.perseus.api

/**
 * Configuration for PerseusNavigator.
 *
 * Mirrors Medusa's [NavigatorConfiguration].
 */
data class PerseusNavigatorConfiguration(
    /** The initial tab index to display (default: 0). */
    val initialTabIndex: Int = 0,
    /**
     * If true, pressing back on the initial tab exits the app.
     * If false, pressing back on any tab at root is blocked.
     */
    val alwaysExitFromInitial: Boolean = false
)
