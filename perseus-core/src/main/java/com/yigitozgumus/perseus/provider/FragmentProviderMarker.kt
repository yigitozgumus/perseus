package com.yigitozgumus.perseus.provider
import com.yigitozgumus.perseus.key.RouterKey


/** Core-facing interface for fragment-based screen providers. Implemented by [ScreenProvider] in interop module. */
public interface FragmentProviderMarker {
    public fun canProvide(key: RouterKey): Boolean
}
