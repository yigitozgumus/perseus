package com.yigitozgumus.perseus.key

import com.yigitozgumus.perseus.key.RouterKey

/**
 * Marker interface for [RouterKey] types that render as bottom sheets.
 *
 * Keys implementing this interface are automatically rendered as bottom sheets
 * via [BottomSheetSceneStrategy] when navigated to.
 *
 * Customize dismissal behavior via [isCancellable] and [isDraggable].
 */
public interface BottomSheetKey : RouterKey {
    /** Whether the bottom sheet can be dismissed by back press or tapping outside. */
    public val isCancellable: Boolean get() = true
    /** Whether the bottom sheet can be dismissed by swiping down. */
    public val isDraggable: Boolean get() = true
}
