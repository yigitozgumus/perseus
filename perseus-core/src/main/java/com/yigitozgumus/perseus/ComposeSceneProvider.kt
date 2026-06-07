package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable

interface ComposeSceneProvider<K : RouterKey> {
    fun canProvide(key: RouterKey): Boolean
    @Composable fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit)
}
