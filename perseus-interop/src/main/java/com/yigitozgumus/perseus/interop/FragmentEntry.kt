package com.yigitozgumus.perseus.interop

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.RouterKey

@Suppress("UNCHECKED_CAST")
@Composable
public fun <K : RouterKey> FragmentEntry(
    key: K,
    provider: ScreenProvider<K>,
    context: NavigationContext<K>,
    modifier: Modifier = Modifier,
) {
    val fragmentTemplate = remember(key) { provider.provide(key) }
    val fragmentClass = fragmentTemplate::class.java as Class<out Fragment>

    val keyClassName = remember(key) { key::class.qualifiedName ?: key::class.java.name }

    val arguments = remember(keyClassName, context) {
        Bundle().apply {
            fragmentTemplate.arguments?.let { putAll(it) }
            putString(NavigationContext.KEY_CLASS_ENTRY, keyClassName)
            putString(NavigationContext.CORRELATION_ID_ENTRY, context.correlationId)
        }
    }

    key(key) {
        AndroidFragment(
            clazz = fragmentClass,
            modifier = modifier,
            fragmentState = rememberFragmentState(),
            arguments = arguments
        )
    }
}
