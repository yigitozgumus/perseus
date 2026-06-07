package com.yigitozgumus.perseus

import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavigationContext = staticCompositionLocalOf<NavigationContext<*>?> {
    null
}
