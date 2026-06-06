package com.yigitozgumus.perseus.api

import androidx.navigation3.runtime.NavEntry
import com.yigitozgumus.perseus.api.GroupName
import com.yigitozgumus.perseus.api.RouterKey

/**
 * Registry that provides NavEntry instances for RouterKeys.
 *
 * This is the DI-agnostic entry point for the navigation system.
 * The impl module provides [PerseusEntryProviderRegistry] which
 * satisfies this contract and adds group tracking.
 */
interface EntryRegistry {
    /** Pop callback for scene dismissal. */
    var onPopCallback: (() -> Unit)?

    /** Provide a NavEntry for the given RouterKey. */
    fun provide(key: RouterKey): NavEntry<RouterKey>

    /** Get the navigation group for a key, if any. */
    fun getGroupForKey(key: RouterKey): GroupName?
}
