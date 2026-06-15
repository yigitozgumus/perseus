package com.yigitozgumus.perseus.provider

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.SceneResultCallback
import com.yigitozgumus.perseus.key.NavigationKey

/**
 * Provider for Compose-based scene content (dialogs and bottom sheets).
 *
 * Use this for complex scenes that need explicit result and dismiss callbacks.
 * For simpler scenes, implement [ScreenProvider] for a [DialogKey] or
 * [BottomSheetKey] and use [LocalSceneActions] for dismiss and result passing.
 *
 * @param K The specific [NavigationKey] type this provider handles.
 */
public interface SceneProvider<K : NavigationKey> {
    /** Returns `true` if this provider can render the given [key]. */
    public fun canProvide(key: NavigationKey): Boolean

    /**
     * Renders the scene composable.
     *
     * @param key The router key containing scene arguments.
     * @param onResult Callback to send results back to the caller.
     * @param onDismiss Callback to dismiss the scene.
     */
    @Composable
    public fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit)
}
