package com.yigitozgumus.perseus.provider
import com.yigitozgumus.perseus.key.RouterKey


import androidx.compose.runtime.Composable

public interface ComposeScreenProvider<K : RouterKey> {
    public fun canProvide(key: RouterKey): Boolean
    @Composable public fun Content(key: K)
}
