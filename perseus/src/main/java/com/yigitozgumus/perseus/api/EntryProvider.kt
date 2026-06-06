package com.yigitozgumus.perseus.api

import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment

/**
 * Factory interface for creating Compose screens from RouterKeys.
 *
 * Each feature provides one or more ComposeScreenProvider implementations.
 * The entry provider registry collects all providers and finds the matching
 * one for each RouterKey.
 *
 * Usage:
 * ```kotlin
 * class HomeComposeProvider : ComposeScreenProvider<HomeKey> {
 *     override fun canProvide(key: RouterKey) = key is HomeKey
 *
 *     @Composable
 *     override fun Content(key: HomeKey) {
 *         HomeScreen(key)
 *     }
 * }
 * ```
 *
 * @param K The specific RouterKey type this provider handles.
 */
interface ComposeScreenProvider<K : RouterKey> {
    /** Returns true if this provider can handle the given key. */
    fun canProvide(key: RouterKey): Boolean

    /** Composable content for the given RouterKey. */
    @Composable
    fun Content(key: K)
}

/**
 * Factory interface for creating Fragments from RouterKeys.
 *
 * Enables incremental migration: existing Fragment-based screens continue
 * to work while new screens can be pure Compose. Fragments are wrapped in
 * a Compose container via [FragmentEntry].
 *
 * Usage:
 * ```kotlin
 * class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
 *     override fun canProvide(key: RouterKey) = key is ProfileKey
 *
 *     override fun provide(key: ProfileKey): Fragment {
 *         return ProfileFragment().apply {
 *             arguments = bundleOf(ARG_ID to key.userId)
 *         }
 *     }
 * }
 * ```
 *
 * Note: The library adds [NavigationContext] to fragment arguments after
 * [provide] returns. Implementations only need to set key-specific arguments.
 *
 * @param K The specific RouterKey type this provider handles.
 */
interface ScreenProvider<K : RouterKey> {
    /** Returns true if this provider can handle the given key. */
    fun canProvide(key: RouterKey): Boolean

    /**
     * Creates a Fragment instance for the given RouterKey.
     *
     * @param key The RouterKey containing navigation arguments.
     * @return A new Fragment instance configured with key-specific arguments.
     */
    fun provide(key: K): Fragment
}

/**
 * Factory interface for creating Compose scene content (dialogs/bottom sheets).
 *
 * Use this for complex scenes that need explicit result and dismiss callbacks.
 * For simpler scenes, implement [ComposeScreenProvider] for a [DialogKey] or
 * [BottomSheetKey] and use [LocalSceneActions] for dismiss/result.
 *
 * Usage:
 * ```kotlin
 * class ConfirmationDialogProvider : ComposeSceneProvider<ConfirmationDialogKey> {
 *     override fun canProvide(key: RouterKey) = key is ConfirmationDialogKey
 *
 *     @Composable
 *     override fun Content(key: ConfirmationDialogKey, onResult: SceneResultCallback, onDismiss: () -> Unit) {
 *         ConfirmationDialog(key, onResult, onDismiss)
 *     }
 * }
 * ```
 *
 * @param K The specific RouterKey type this provider handles.
 */
interface ComposeSceneProvider<K : RouterKey> {
    /** Returns true if this provider can handle the given key. */
    fun canProvide(key: RouterKey): Boolean

    /**
     * Composable content for the scene.
     *
     * @param key The RouterKey containing scene arguments.
     * @param onResult Callback to send results back to the caller.
     * @param onDismiss Callback to dismiss the scene.
     */
    @Composable
    fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit)
}

/**
 * Callback interface for scene results (dialogs and bottom sheets).
 */
interface SceneResultCallback {
    fun <R : Any> sendResult(result: R)
}
