package com.yigitozgumus.perseus

import androidx.lifecycle.ViewModelStore
import com.yigitozgumus.perseus.key.RouterKey
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Provides ViewModelStore scoped to a [RouterKey]'s lifetime.
 *
 * This is the single source of truth for per-key ViewModelStores.
 * Both Compose screens (via a custom NavEntry decorator) and Fragment
 * screens (via [perseusScopedViewModel]) use this provider.
 *
 * ## Why this exists
 *
 * In Nav3, each NavEntry gets its own ViewModelStore. For Fragment
 * screens wrapped via `AndroidFragment`, the Fragment's default store
 * is tied to the Fragment view lifecycle — it is cleared when the
 * view is destroyed (another screen is pushed on top, or the user
 * switches tabs). This provider decouples ViewModel lifetime from
 * Fragment view lifetime: the store lives as long as the key is
 * in the back stack.
 */
public interface PerseusViewModelStoreProvider {
    /** Returns a [ViewModelStoreOwner] scoped to the given [key]. */
    public fun getOwner(key: RouterKey): ViewModelStoreOwner

    /** Clears and removes the store for [key]. Called when key is popped. */
    public fun clear(key: RouterKey)

    /** Keeps only the given [keys], clearing all others. */
    public fun retainOnly(keys: Set<RouterKey>)
}
