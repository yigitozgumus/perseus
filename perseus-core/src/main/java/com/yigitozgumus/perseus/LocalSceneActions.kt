package com.yigitozgumus.perseus

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSceneActions = staticCompositionLocalOf<SceneActions> {
    error("No SceneActions provided. Scene content must be rendered within a SceneStrategy.")
}
