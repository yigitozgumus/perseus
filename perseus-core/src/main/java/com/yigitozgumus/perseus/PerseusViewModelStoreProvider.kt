package com.yigitozgumus.perseus
import com.yigitozgumus.perseus.key.RouterKey


import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Provides ViewModelStore scoped to a RouterKey's lifetime in the back stack.
 *
 * This is the single source of truth for per-RouterKey ViewModelStores.
 * Both Compose screens (via a custom NavEntry decorator) and Fragment screens
 * (via [perseusScopedViewModel]) use this same provider — ensuring a single
 * ViewModelStore per RouterKey regardless of screen type.
 *
 * ## Why this exists
 *
 * In pure Compose + Nav3, each NavEntry gets its own ViewModelStore. But for
 * Fragment screens wrapped via [AndroidFragment], the Fragment's default
 * ViewModelStore is tied to the Fragment's view lifecycle — it's cleared when
 * the view is destroyed (another screen pushed on top, or tab switched away).
 *
 * This provider decouples ViewModel lifetime from Fragment view lifetime.
 * The store lives as long as the RouterKey is in the back stack.
 */
public interface PerseusViewModelStoreProvider {

    /**
     * Returns a ViewModelStoreOwner scoped to the given RouterKey.
     * Creates the store on first access (lazy).
     */
    public fun getOwner(key: RouterKey): ViewModelStoreOwner

    /**
     * Clears and removes the ViewModelStore for the given key.
     * Called ONLY when the key is popped from the back stack.
     */
    public fun clear(key: RouterKey)

    /**
     * Keeps only the specified keys, clearing all others.
     * Used on full navigation resets (e.g., logout).
     */
    public fun retainOnly(keys: Set<RouterKey>)
}
