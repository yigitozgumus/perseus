package com.yigitozgumus.perseus.api

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Actions available to scene content (dialogs/bottom sheets).
 *
 * Provided by the navigation infrastructure when rendering scene keys
 * (DialogKey, BottomSheetKey) via SceneStrategy.
 */
interface SceneActions {
    /** Send a result back to the navigation handle observer. */
    fun <R : Any> sendResult(result: R)

    /** Dismiss the current scene (pops from back stack). */
    fun dismiss()

    /** Send a result and dismiss the scene in one call. */
    fun <R : Any> sendResultAndDismiss(result: R) {
        sendResult(result)
        dismiss()
    }
}

/**
 * CompositionLocal providing [SceneActions] to dialog/bottom sheet content.
 *
 * Usage in scene content:
 * ```kotlin
 * @Composable
 * fun MySheet() {
 *     val actions = LocalSceneActions.current
 *     Button(onClick = { actions.sendResultAndDismiss(MyResult.Ok) }) {
 *         Text("Confirm")
 *     }
 * }
 * ```
 */
val LocalSceneActions = staticCompositionLocalOf<SceneActions> {
    error("No SceneActions provided. Scene content must be rendered within a SceneStrategy.")
}

/**
 * CompositionLocal providing the current screen's [NavigationContext].
 * Available in all Perseus-managed Compose screens for result sending.
 */
val LocalNavigationContext = staticCompositionLocalOf<NavigationContext<*>?> {
    null
}
