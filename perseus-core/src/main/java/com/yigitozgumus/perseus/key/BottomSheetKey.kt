package com.yigitozgumus.perseus.key

import com.yigitozgumus.perseus.key.NavigationKey

/**
 * Marker interface for [NavigationKey] types that render as bottom sheets.
 *
 * Keys implementing this interface are automatically rendered as bottom sheets
 * via [BottomSheetSceneStrategy] when navigated to.
 *
 * Customize dismissal behavior via [isCancellable] and [isDraggable].
 */
public interface BottomSheetKey : NavigationKey {
    /** Whether the bottom sheet can be dismissed by back press or tapping outside. */
    public val isCancellable: Boolean get() = true
    /** Whether the bottom sheet can be dismissed by swiping down. */
    public val isDraggable: Boolean get() = true
}
