package com.yigitozgumus.perseus.provider

import com.yigitozgumus.perseus.key.RouterKey

/**
 * Marker interface implemented by fragment-based screen providers.
 *
 * This decouples the core module from Android Fragment types.
 * The interop module's [ScreenProvider] extends this interface.
 *
 * Entry provider registries accept lists of [FragmentProviderMarker]
 * and match keys via [canProvide].
 */
public interface FragmentProviderMarker {
    /** Returns `true` if this provider can render the given [key]. */
    public fun canProvide(key: RouterKey): Boolean
}
