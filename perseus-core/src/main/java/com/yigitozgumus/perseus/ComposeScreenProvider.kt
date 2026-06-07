package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable

interface ComposeScreenProvider<K : RouterKey> {
    fun canProvide(key: RouterKey): Boolean
    @Composable fun Content(key: K)
}
