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

    private val owners = ConcurrentHashMap<String, StoreOwner>()

    override fun getOwner(entryId: String): ViewModelStoreOwner {
        val existed = owners.containsKey(entryId)
        val owner = owners.getOrPut(entryId) { StoreOwner(ViewModelStore()) }
        logger.debug("viewModelStoreOwner entryId=$entryId reused=$existed activeStores=${owners.size}")
        PerseusViewModelStoreOwners.register(entryId, owner)
        return owner
    }

    override fun clear(entryId: String) {
        val removed = owners.remove(entryId)
        removed?.viewModelStore?.clear()
        PerseusViewModelStoreOwners.unregister(entryId)
        logger.debug("viewModelStoreClear entryId=$entryId existed=${removed != null} activeStores=${owners.size}")
    }

    override fun retainOnly(entryIds: Set<String>) {
        val toRemove = owners.keys - entryIds
        logger.debug("viewModelStoreRetainOnly keep=${entryIds.size} remove=${toRemove.size}")
        toRemove.forEach { clear(it) }
    }

    /** Internal ViewModelStoreOwner with a fixed store instance. */
    private class StoreOwner(
        override val viewModelStore: ViewModelStore,
    ) : ViewModelStoreOwner
}
