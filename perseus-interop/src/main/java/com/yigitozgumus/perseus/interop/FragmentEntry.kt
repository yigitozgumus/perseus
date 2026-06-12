package com.yigitozgumus.perseus.interop

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.key.DefaultNavigationKeyCodec
import com.yigitozgumus.perseus.key.NavigationKey

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
public fun <K : NavigationKey> FragmentEntry(
    key: K,
    provider: ScreenProvider<K>,
    context: NavigationContext<K>,
    viewModelStoreProvider: PerseusViewModelStoreProvider,
    modifier: Modifier = Modifier,
) {
    val fragmentTemplate = remember(context.entryId) { provider.provide(key) }
    val fragmentClass = fragmentTemplate::class.java as Class<out Fragment>

    val encodedKey = remember(key) { DefaultNavigationKeyCodec.encode(key) }
    remember(context.entryId) { viewModelStoreProvider.getOwner(context.entryId) }

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

    val fragmentSavedState = remember(context.entryId) { mutableStateOf<Fragment.SavedState?>(null) }
    val containerId = remember(context.entryId) { View.generateViewId() }
    val localView = LocalView.current
    val localContext = LocalContext.current
    val fragmentManager = remember(localView) { FragmentManager.findFragmentManager(localView) }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FragmentContainerView(viewContext).apply { id = containerId }
        },
    )

    DisposableEffect(fragmentManager, fragmentClass, context.entryId) {
        val fragment = fragmentManager.findFragmentById(containerId)
            ?: fragmentManager.fragmentFactory
                .instantiate(localContext.classLoader, fragmentClass.name)
                .apply {
                    setInitialSavedState(fragmentSavedState.value)
                    this.arguments = arguments
                    fragmentManager.commitNow {
                        setReorderingAllowed(true)
                        add(containerId, this@apply, context.entryId)
                    }
                }

        onDispose {
            fragmentSavedState.value = fragmentManager.saveFragmentInstanceState(fragment)
            if (!fragmentManager.isStateSaved) {
                fragmentManager.commitNow { remove(fragment) }
            }
        }
    }
}
