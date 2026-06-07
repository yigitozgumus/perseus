package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable

public interface ComposeScreenProvider<K : RouterKey> {
    public fun canProvide(key: RouterKey): Boolean
    @Composable public fun Content(key: K)
}
