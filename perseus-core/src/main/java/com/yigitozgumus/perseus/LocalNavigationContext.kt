package com.yigitozgumus.perseus

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing the current screen's [NavigationContext].
 *
 * Available in all Perseus-managed Compose screens. The context contains
 * the correlation ID needed to send results back to the parent via
 * [PerseusNavigator.sendResult].
 *
 * Returns `null` if no navigation context is available (should not happen
 * in Perseus-managed screens).
 */
public val LocalNavigationContext: ProvidableCompositionLocal<NavigationContext<*>?> =
    staticCompositionLocalOf<NavigationContext<*>?> { null }
