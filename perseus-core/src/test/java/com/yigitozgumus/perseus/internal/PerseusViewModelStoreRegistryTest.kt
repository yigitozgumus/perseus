package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
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

        try {
            PerseusViewModelStoreOwners.getOwner("entry-a")
            fail("Owner lookup should be removed when the entry store is cleared")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }
}
