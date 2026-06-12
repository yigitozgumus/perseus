package com.yigitozgumus.perseus.provider

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.key.NavigationKey

/**
 * Provider for Compose-based screens.
 *
 * Implement one per [NavigationKey] type and register with your DI framework.
 * The entry provider registry collects all implementations and dispatches
 * to the matching one for each navigation key.
 *
 * @param K The specific [NavigationKey] type this provider handles.
 */
public interface ComposeScreenProvider<K : NavigationKey> {
    /** Returns `true` if this provider can render the given [key]. */
    public fun canProvide(key: NavigationKey): Boolean

    /** Renders the composable content for the given [key]. */
    @Composable public fun Content(key: K)
}
