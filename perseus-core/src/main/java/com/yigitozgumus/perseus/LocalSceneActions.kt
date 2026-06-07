package com.yigitozgumus.perseus
import com.yigitozgumus.perseus.key.RouterKey


import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalSceneActions: ProvidableCompositionLocal<SceneActions> = staticCompositionLocalOf<SceneActions> {
    error("No SceneActions provided. Scene content must be rendered within a SceneStrategy.")
}
