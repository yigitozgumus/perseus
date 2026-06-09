package com.yigitozgumus.perseus.internal

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.yigitozgumus.perseus.EmptyPerseusLogger
import com.yigitozgumus.perseus.PerseusLogger
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.debug
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry holding ViewModelStores per navigation entry ID.
 *
 * Implements [PerseusViewModelStoreProvider] — the single source of truth
 * for NavEntry-scoped ViewModels used by both Compose screens and Fragment screens.
 */
internal class PerseusViewModelStoreRegistry(
    private val logger: PerseusLogger = EmptyPerseusLogger,
) : PerseusViewModelStoreProvider {

    private val stores = ConcurrentHashMap<String, ViewModelStore>()

    override fun getOwner(entryId: String): ViewModelStoreOwner {
        val existed = stores.containsKey(entryId)
        stores.getOrPut(entryId) { ViewModelStore() }
        logger.debug("viewModelStoreOwner entryId=$entryId reused=$existed activeStores=${stores.size}")
        return StoreOwner(entryId, stores).also { owner ->
            PerseusViewModelStoreOwners.register(entryId, owner)
        }
    }

    override fun clear(entryId: String) {
        val removed = stores.remove(entryId)
        removed?.clear()
        PerseusViewModelStoreOwners.unregister(entryId)
        logger.debug("viewModelStoreClear entryId=$entryId existed=${removed != null} activeStores=${stores.size}")
    }

    override fun retainOnly(entryIds: Set<String>) {
        val toRemove = stores.keys - entryIds
        logger.debug("viewModelStoreRetainOnly keep=${entryIds.size} remove=${toRemove.size}")
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
