package com.yigitozgumus.perseus.interop

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker

/**
 * Default [FragmentEntryFactory] that wraps each fragment via [FragmentEntry].
 *
 * Used by [PerseusNavigatorFactory.create] when no custom factory is provided.
 */
public object DefaultFragmentEntryFactory : FragmentEntryFactory {

    @Composable
    @Suppress("UNCHECKED_CAST")
    public override fun Create(
        provider: FragmentProviderMarker,
        key: RouterKey,
        context: NavigationContext<RouterKey>,
    ) {
        FragmentEntry(
            key = key,
            provider = provider as ScreenProvider<RouterKey>,
            context = context,
        )
    }
}
