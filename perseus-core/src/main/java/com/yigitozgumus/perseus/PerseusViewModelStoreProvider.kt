package com.yigitozgumus.perseus

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Provides ViewModelStore scoped to a navigation entry's lifetime.
 *
 * This is the single source of truth for per-entry ViewModelStores.
 * Both Compose screens (via a custom NavEntry decorator) and Fragment
 * screens use this provider.
 *
 * ## Why this exists
 *
 * In Nav3, each NavEntry gets its own ViewModelStore. For Fragment
 * screens wrapped via `AndroidFragment`, the Fragment's default store
 * is tied to the Fragment view lifecycle — it is cleared when the
 * view is destroyed (another screen is pushed on top, or the user
 * switches tabs). This provider decouples ViewModel lifetime from
 * Fragment view lifetime: the store lives as long as the entry is
 * in the back stack.
 */
public interface PerseusViewModelStoreProvider {
    /** Returns a [ViewModelStoreOwner] scoped to the given [entryId]. */
    public fun getOwner(entryId: String): ViewModelStoreOwner

    /** Clears and removes the store for [entryId]. Called when the entry is popped. */
    public fun clear(entryId: String)

    /** Keeps only the given [entryIds], clearing all others. */
    public fun retainOnly(entryIds: Set<String>)
}
