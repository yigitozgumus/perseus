package com.yigitozgumus.perseus

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalNavigationContext: ProvidableCompositionLocal<NavigationContext<*>?> = staticCompositionLocalOf<NavigationContext<*>?> {
    null
}
