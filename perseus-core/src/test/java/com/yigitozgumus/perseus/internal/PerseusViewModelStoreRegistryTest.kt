package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class PerseusViewModelStoreRegistryTest {

    @Test
    fun ownersAreScopedByEntryId() {
        val registry = PerseusViewModelStoreRegistry()

        val first = registry.getOwner("entry-a")
        val second = registry.getOwner("entry-a")
        val other = registry.getOwner("entry-b")

        assertSame(first.viewModelStore, second.viewModelStore)
        if (first.viewModelStore === other.viewModelStore) {
            fail("Different entry IDs should not share a ViewModelStore")
        }
    }

    @Test
    fun ownerLookupIsRegisteredAndUnregisteredWithRegistryLifecycle() {
        val registry = PerseusViewModelStoreRegistry()
        val owner = registry.getOwner("entry-a")

        assertSame(owner.viewModelStore, PerseusViewModelStoreOwners.getOwner("entry-a").viewModelStore)

        registry.clear("entry-a")

        assertNoRegisteredOwner("entry-a")
    }

    @Test
    fun staleOwnerDoesNotRecreateStoreAfterClear() {
        val registry = PerseusViewModelStoreRegistry()
        val staleOwner = registry.getOwner("entry-a")
        val staleStore = staleOwner.viewModelStore

        registry.clear("entry-a")

        assertSame(staleStore, staleOwner.viewModelStore)
        assertNoRegisteredOwner("entry-a")

        val replacementOwner = registry.getOwner("entry-a")
        assertNotSame(staleStore, replacementOwner.viewModelStore)
        assertSame(replacementOwner.viewModelStore, PerseusViewModelStoreOwners.getOwner("entry-a").viewModelStore)
    }

    @Test
    fun retainOnlyClearsRemovedStoresAndUnregistersOwners() {
        val registry = PerseusViewModelStoreRegistry()
        val keptOwner = registry.getOwner("entry-a")
        val removedOwner = registry.getOwner("entry-b")
        val removedStore = removedOwner.viewModelStore

        registry.retainOnly(setOf("entry-a"))

        assertSame(keptOwner.viewModelStore, PerseusViewModelStoreOwners.getOwner("entry-a").viewModelStore)
        assertSame(removedStore, removedOwner.viewModelStore)
        assertNoRegisteredOwner("entry-b")
    }

    private fun assertNoRegisteredOwner(entryId: String) {
        try {
            PerseusViewModelStoreOwners.getOwner(entryId)
            fail("Owner lookup should be removed when the entry store is cleared")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }
}
