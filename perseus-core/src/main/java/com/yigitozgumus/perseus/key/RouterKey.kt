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
 * @Serializable data object HomeKey : RouterKey
 * @Serializable data class DetailKey(val id: Int) : RouterKey
 * @Serializable data object FullScreenKey : RouterKey {
 *     override val hidesBottomNavigation: Boolean get() = true
 * }
 * ```
 */
public interface RouterKey : NavKey {
    /** Whether this screen hides the bottom navigation bar. Default is `true`. */
    public val hidesBottomNavigation: Boolean get() = true
}
