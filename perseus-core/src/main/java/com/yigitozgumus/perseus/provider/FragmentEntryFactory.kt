package com.yigitozgumus.perseus.provider

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.key.RouterKey

/**
 * Factory that creates Compose wrappers for Fragment-based screens.
 *
 * Called by the entry provider registry when a [FragmentProviderMarker]
 * matches a given [RouterKey]. Implementations create the composable
 * wrapper that hosts the fragment within Navigation3's NavDisplay.
 *
 * The default implementation ([DefaultFragmentEntryFactory]) in the
 * interop module wraps each fragment via [FragmentEntry].
 */
public interface FragmentEntryFactory {
    /**
     * Creates a composable wrapper for a fragment-based screen.
     *
     * @param provider The fragment provider that matched the key.
     * @param key The router key identifying the screen.
     * @param context The navigation context with correlation ID for results.
     */
    @Composable
    public fun Create(
        provider: FragmentProviderMarker,
        key: RouterKey,
        context: NavigationContext<RouterKey>
    )
}
