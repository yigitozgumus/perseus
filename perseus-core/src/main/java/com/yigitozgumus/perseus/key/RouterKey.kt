package com.yigitozgumus.perseus.key

import androidx.navigation3.runtime.NavKey

/**
 * Type-safe screen key for Perseus navigation.
 *
 * Extends Navigation3's [NavKey] to bridge into nav3's native key system.
 * Implement as `@Serializable` data object (no-arg screens) or data class
 * (screens with arguments).
 *
 * Usage:
 * ```kotlin
 * @Serializable data object HomeKey : NavigationKey
 * @Serializable data class DetailKey(val id: Int) : NavigationKey
 * @Serializable data object FullScreenKey : NavigationKey {
 *     override val hidesBottomNavigation: Boolean get() = true
 * }
 * ```
 */
public interface NavigationKey : NavKey {
    /** Whether this screen hides the bottom navigation bar. Default is `true`. */
    public val hidesBottomNavigation: Boolean get() = true
}
