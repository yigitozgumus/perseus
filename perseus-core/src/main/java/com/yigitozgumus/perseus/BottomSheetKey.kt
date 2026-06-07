package com.yigitozgumus.perseus

public interface BottomSheetKey : RouterKey {
    public val isCancellable: Boolean get() = true
    public val isDraggable: Boolean get() = true
}
