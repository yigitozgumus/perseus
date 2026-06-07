package com.yigitozgumus.perseus

/** Core-facing interface for fragment-based screen providers. Implemented by [ScreenProvider] in interop module. */
interface FragmentProviderMarker {
    fun canProvide(key: RouterKey): Boolean
}
