package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.FragmentProviderMarker
import com.yigitozgumus.perseus.RouterKey

interface ScreenProvider<K : RouterKey> : FragmentProviderMarker {
    override fun canProvide(key: RouterKey): Boolean
    fun provide(key: K): Fragment
}
