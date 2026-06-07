package com.yigitozgumus.perseus.internal

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry holding ViewModelStores per navigation entry ID.
 *
 * Implements [PerseusViewModelStoreProvider] — the single source of truth
 * for NavEntry-scoped ViewModels used by both Compose screens and Fragment screens.
 */
internal class PerseusViewModelStoreRegistry : PerseusViewModelStoreProvider {

    private val stores = ConcurrentHashMap<String, ViewModelStore>()

    override fun getOwner(entryId: String): ViewModelStoreOwner {
        stores.getOrPut(entryId) { ViewModelStore() }
        return StoreOwner(entryId, stores).also { owner ->
            PerseusViewModelStoreOwners.register(entryId, owner)
        }
    }

    override fun clear(entryId: String) {
        stores.remove(entryId)?.clear()
        PerseusViewModelStoreOwners.unregister(entryId)
    }

    override fun retainOnly(entryIds: Set<String>) {
        val toRemove = stores.keys - entryIds
        toRemove.forEach { clear(it) }
    }

    /**
     * Internal ViewModelStoreOwner backed by the registry.
     * Each call to [viewModelStore] returns the same store for the given key.
     */
    private class StoreOwner(
        private val entryId: String,
        private val stores: ConcurrentHashMap<String, ViewModelStore>
    ) : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = stores[entryId] ?: ViewModelStore().also { stores[entryId] = it }
    }
}
