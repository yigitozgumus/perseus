package com.yigitozgumus.perseus.api

import androidx.navigation3.runtime.NavKey

/**
 * Type-safe screen identification key for Perseus navigation.
 *
 * Extends Navigation3's [NavKey] to bridge into nav3's native key system.
 * Implementations should be `@Serializable` data objects or data classes.
 *
 * Usage:
 * ```kotlin
 * @Serializable data object HomeKey : RouterKey
 * @Serializable data class DetailKey(val id: String) : RouterKey
 * @Serializable data object FullScreenKey : RouterKey {
 *     override val hidesBottomNavigation: Boolean get() = true
 * }
 * ```
 */
interface RouterKey : NavKey {
    /** Whether this screen should hide the bottom navigation bar. */
    val hidesBottomNavigation: Boolean get() = true
}

/**
 * Marker interface for RouterKeys that render as dialogs.
 *
 * Keys implementing this interface are automatically rendered as dialogs
 * via [DialogSceneStrategy] when navigated to.
 */
interface DialogKey : RouterKey

/**
 * Marker interface for RouterKeys that render as bottom sheets.
 *
 * Keys implementing this interface are automatically rendered as bottom sheets
 * via BottomSheetSceneStrategy.
 */
interface BottomSheetKey : RouterKey {
    /** Whether the bottom sheet can be dismissed via back press or clicking outside. */
    val isCancellable: Boolean get() = true
    /** Whether the bottom sheet can be dismissed by swiping down. */
    val isDraggable: Boolean get() = true
}
