package com.yigitozgumus.perseus.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.yigitozgumus.perseus.api.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.api.RouterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry holding ViewModelStores per RouterKey.
 *
 * Implements [PerseusViewModelStoreProvider] — the single source of truth
 * for NavEntry-scoped ViewModels used by both Compose screens and Fragment screens.
 */
class PerseusViewModelStoreRegistry : PerseusViewModelStoreProvider {

    private val stores = ConcurrentHashMap<RouterKey, ViewModelStore>()

    override fun getOwner(key: RouterKey): ViewModelStoreOwner {
        stores.getOrPut(key) { ViewModelStore() }
        return StoreOwner(key, stores)
    }

    override fun clear(key: RouterKey) {
        stores.remove(key)?.clear()
    }

    override fun retainOnly(keys: Set<RouterKey>) {
        val toRemove = stores.keys - keys
        toRemove.forEach { clear(it) }
    }

    /**
     * Internal ViewModelStoreOwner backed by the registry.
     * Each call to [viewModelStore] returns the same store for the given key.
     */
    private class StoreOwner(
        private val key: RouterKey,
        private val stores: ConcurrentHashMap<RouterKey, ViewModelStore>
    ) : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = stores[key] ?: ViewModelStore().also { stores[key] = it }
    }
}
