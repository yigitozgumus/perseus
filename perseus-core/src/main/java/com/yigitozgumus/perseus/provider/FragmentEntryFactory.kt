package com.yigitozgumus.perseus.provider
import com.yigitozgumus.perseus.key.RouterKey

import com.yigitozgumus.perseus.NavigationContext


import androidx.compose.runtime.Composable

/**
 * Factory that creates Compose wrappers for Fragment-based screens.
 *
 * The library calls [create] when a [FragmentProviderMarker] is found
 * for a given [RouterKey]. The perseus-interop module provides
 * [DefaultFragmentEntryFactory] which wraps fragments via [FragmentEntry].
 */
public interface FragmentEntryFactory {
    @Composable
    public fun Create(provider: FragmentProviderMarker, key: RouterKey, context: NavigationContext<RouterKey>)
}
