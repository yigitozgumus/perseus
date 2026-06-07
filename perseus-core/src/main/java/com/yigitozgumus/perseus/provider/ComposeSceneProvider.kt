package com.yigitozgumus.perseus.provider
import com.yigitozgumus.perseus.key.RouterKey

import com.yigitozgumus.perseus.SceneResultCallback


import androidx.compose.runtime.Composable

public interface ComposeSceneProvider<K : RouterKey> {
    public fun canProvide(key: RouterKey): Boolean
    @Composable public fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit)
}
