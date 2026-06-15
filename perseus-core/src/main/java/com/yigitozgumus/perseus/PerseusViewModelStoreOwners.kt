package com.yigitozgumus.perseus

import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.ConcurrentHashMap

/** Lookup for Perseus-managed entry-scoped [ViewModelStoreOwner]s. Intended for Perseus infrastructure. */
public object PerseusViewModelStoreOwners {
    private val owners = ConcurrentHashMap<String, ViewModelStoreOwner>()

    /** Returns the owner registered for [entryId]. */
    public fun getOwner(entryId: String): ViewModelStoreOwner = owners[entryId]
        ?: error("No Perseus ViewModelStoreOwner registered for entryId=$entryId")

    /** Registers [owner] for [entryId]. Intended for Perseus infrastructure. */
    public fun register(entryId: String, owner: ViewModelStoreOwner) {
        owners[entryId] = owner
    }

    /** Unregisters the owner for [entryId]. Intended for Perseus infrastructure. */
    public fun unregister(entryId: String) {
        owners.remove(entryId)
    }
}
