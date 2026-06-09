package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/**
 * Provider for Fragment-based screens.
 *
 * Implements [FragmentProviderMarker] so it can be collected by the
 * entry provider registry alongside Compose providers. One implementation
 * per [RouterKey] type.
 *
 * Example:
 * ```kotlin
 * class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
 *     override fun canProvide(key: RouterKey) = key is ProfileKey
 *     override fun provide(key: ProfileKey): Fragment = ProfileFragment()
 * }
 * ```
 *
 * @param K The specific [RouterKey] type this provider handles.
 */
public interface ScreenProvider<K : RouterKey> : FragmentProviderMarker {
    /** Returns `true` if this provider can render the given [key]. */
    public override fun canProvide(key: RouterKey): Boolean

    /** Creates a new [Fragment] instance for the given [key]. */
    public fun provide(key: K): Fragment
}
