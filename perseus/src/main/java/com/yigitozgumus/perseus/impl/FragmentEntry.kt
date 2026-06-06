package com.yigitozgumus.perseus.impl

import android.os.Bundle
import android.util.Base64
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import com.yigitozgumus.perseus.api.NavigationContext
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.api.ScreenProvider

/**
 * Composable that wraps an existing Fragment for use in Navigation3.
 *
 * Enables incremental migration: existing [ScreenProvider] implementations
 * continue to work while new screens can be pure Compose.
 *
 * The RouterKey is serialized to JSON and stored in fragment arguments
 * along with the correlation ID from [NavigationContext].
 */
@Suppress("UNCHECKED_CAST")
@Composable
fun <K : RouterKey> FragmentEntry(
    key: K,
    provider: ScreenProvider<K>,
    context: NavigationContext<K>,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val fragmentTemplate = remember(key) { provider.provide(key) }
    val fragmentClass = fragmentTemplate::class.java as Class<out Fragment>

    // Serialize the key to JSON for Bundle transport
    val keyJson = remember(key) { encodeKey(key) }

    val arguments = remember(key, context, keyJson) {
        Bundle().apply {
            fragmentTemplate.arguments?.let { putAll(it) }
            putString(NavigationContext.KEY_BUNDLE_ENTRY, Base64.encodeToString(
                keyJson.toByteArray(), Base64.NO_WRAP
            ))
            putString(NavigationContext.CORRELATION_ID_BUNDLE_ENTRY, context.correlationId)
        }
    }

    // Key by RouterKey for proper tab isolation
    key(key) {
        AndroidFragment(
            clazz = fragmentClass,
            modifier = modifier,
            fragmentState = rememberFragmentState(),
            arguments = arguments
        )
    }
}
