package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.FragmentProviderMarker
import com.yigitozgumus.perseus.RouterKey

public interface ScreenProvider<K : RouterKey> : FragmentProviderMarker {
    public override fun canProvide(key: RouterKey): Boolean
    public fun provide(key: K): Fragment
}
