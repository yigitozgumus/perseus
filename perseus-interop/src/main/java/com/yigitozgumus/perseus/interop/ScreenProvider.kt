package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import com.yigitozgumus.perseus.key.RouterKey

public interface ScreenProvider<K : RouterKey> : FragmentProviderMarker {
    public override fun canProvide(key: RouterKey): Boolean
    public fun provide(key: K): Fragment
}
