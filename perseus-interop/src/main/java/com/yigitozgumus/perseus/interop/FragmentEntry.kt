package com.yigitozgumus.perseus.interop

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.key.DefaultRouterKeyCodec
import com.yigitozgumus.perseus.key.RouterKey

/**
 * Composable that wraps an existing Fragment for use in Navigation3.
 *
 * Enables incremental migration: existing [ScreenProvider] implementations
 * continue to work while new screens can be pure Compose.
 *
 * The [NavigationContext] is stored in fragment arguments as:
 * - `perseus_key_class`: fully-qualified class name
 * - `perseus_key_payload`: serialized key payload
 * - `perseus_entry_id`: unique back-stack entry ID
 * - `perseus_correlation_id`: correlation ID for result routing
 *
 * Use [getNavigationContext] in the fragment to retrieve the key.
 */
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

    val encodedKey = remember(key) { DefaultRouterKeyCodec.encode(key) }

    val arguments = remember(encodedKey, context) {
        Bundle().apply {
            fragmentTemplate.arguments?.let { putAll(it) }
            putString(
                NavigationContext.KEY_CLASS_ENTRY,
                encodedKey.className,
            )
            putString(
                NavigationContext.KEY_PAYLOAD_ENTRY,
                encodedKey.payload,
            )
            putString(
                NavigationContext.ENTRY_ID_ENTRY,
                context.entryId,
            )
            putString(
                NavigationContext.CORRELATION_ID_ENTRY,
                context.correlationId,
            )
        }
    }

    key(key) {
        AndroidFragment(
            clazz = fragmentClass,
            modifier = modifier,
            fragmentState = rememberFragmentState(),
            arguments = arguments,
        )
    }
}
