package com.yigitozgumus.perseus

interface BottomSheetKey : RouterKey {
    val isCancellable: Boolean get() = true
    val isDraggable: Boolean get() = true
}
