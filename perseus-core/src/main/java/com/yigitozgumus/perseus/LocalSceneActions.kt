package com.yigitozgumus.perseus

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing [SceneActions] to dialog and bottom sheet content.
 *
 * Usage:
 * ```kotlin
 * val actions = LocalSceneActions.current
 * Button(onClick = { actions.sendResultAndDismiss(MyResult.Ok) }) {
 *     Text("Confirm")
 * }
 * ```
 */
public val LocalSceneActions: ProvidableCompositionLocal<SceneActions> =
    staticCompositionLocalOf<SceneActions> {
        error(
            "No SceneActions provided. " +
                "Scene content must be rendered within a SceneStrategy."
        )
    }
