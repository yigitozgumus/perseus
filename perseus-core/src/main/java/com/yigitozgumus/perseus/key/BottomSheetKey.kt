package com.yigitozgumus.perseus.key
import com.yigitozgumus.perseus.key.RouterKey


public interface BottomSheetKey : RouterKey {
    public val isCancellable: Boolean get() = true
    public val isDraggable: Boolean get() = true
}
