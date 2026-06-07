package com.yigitozgumus.perseus

import androidx.navigation3.runtime.NavEntry
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey

/**
 * Registry that provides [NavEntry] instances for [RouterKey]s.
 *
 * This is the DI-agnostic entry point for the navigation system.
 * The impl module provides [PerseusEntryProviderRegistry] which
 * satisfies this contract and adds group tracking.
 *
 * @see com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
 */
public interface EntryRegistry {
    /** Pop callback for scene dismissal. Set by the navigator. */
    public var onPopCallback: (() -> Unit)?

    /** Provide a [NavEntry] for the given [RouterKey]. */
    public fun provide(key: RouterKey): NavEntry<RouterKey>

    /** Get the navigation group for a key, if any. */
    public fun getGroupForKey(key: RouterKey): GroupName?

    /** Set a pending group for a key before its entry is provided. */
    public fun setPendingGroup(key: RouterKey, groupName: GroupName)

    /** Set a pending correlation ID for a key before its entry is provided. */
    public fun setPendingCorrelationId(key: RouterKey, correlationId: String)

    /** Clear tracking data for a key (called when key is popped). */
    public fun clearTrackingForKey(key: RouterKey)

    /** Clear all tracking data (called on session reset). */
    public fun clearAllTracking()
}
