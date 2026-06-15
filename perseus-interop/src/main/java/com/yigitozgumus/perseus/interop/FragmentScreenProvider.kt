package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/**
 * Provider for Fragment-based screens.
 *
 * Implements [FragmentProviderMarker] so it can be collected by the
 * entry provider registry alongside Compose providers. One implementation
 * per [NavigationKey] type.
 *
 * Example:
 * ```kotlin
 * class ProfileFragmentProvider : FragmentScreenProvider<ProfileKey> {
 *     override fun canProvide(key: NavigationKey) = key is ProfileKey
 *     override fun provide(key: ProfileKey): Fragment = ProfileFragment()
 * }
 * ```
 *
 * @param K The specific [NavigationKey] type this provider handles.
 */
public interface FragmentScreenProvider<K : NavigationKey> : FragmentProviderMarker {
    /** Returns `true` if this provider can render the given [key]. */
    public override fun canProvide(key: NavigationKey): Boolean

    /** Creates a new [Fragment] instance for the given [key]. */
    public fun provide(key: K): Fragment
}
