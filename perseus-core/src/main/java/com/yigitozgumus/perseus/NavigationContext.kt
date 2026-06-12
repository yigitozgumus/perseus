package com.yigitozgumus.perseus

import android.os.Bundle
import com.yigitozgumus.perseus.key.DefaultNavigationKeyCodec
import com.yigitozgumus.perseus.key.EncodedNavigationKey
import com.yigitozgumus.perseus.key.NavigationKey
import java.util.UUID

/**
 * Context passed to child screens containing navigation metadata.
 *
 * Wraps a [NavigationKey] with a stable [entryId] for the current back-stack
 * entry and a [correlationId] used for scoped result routing. The child
 * screen uses this context to send results back to the correct parent via
 * [PerseusNavigator.sendResult].
 *
 * ## Fragment transport
 *
 * The entry ID, correlation ID, key class name, and serialized key payload
 * are stored in fragment arguments.
 *
 * @param K The specific [NavigationKey] type for type-safe access.
 */
public data class NavigationContext<out K : NavigationKey>(
    public val key: K,
    public val entryId: String = UUID.randomUUID().toString(),
    public val correlationId: String = UUID.randomUUID().toString(),
) {
    public companion object {
        /** Bundle key for the NavigationKey class name. */
        public const val KEY_CLASS_ENTRY: String = "perseus_key_class"
        /** Bundle key for the serialized NavigationKey payload. */
        public const val KEY_PAYLOAD_ENTRY: String = "perseus_key_payload"
        /** Bundle key for the unique back-stack entry ID. */
        public const val ENTRY_ID_ENTRY: String = "perseus_entry_id"
        /** Bundle key for the correlation ID. */
        public const val CORRELATION_ID_ENTRY: String = "perseus_correlation_id"
    }
}

/**
 * Retrieves the [NavigationContext] from fragment arguments.
 * Resolves the [NavigationKey] from its stored class name and serialized payload.
 *
 * @throws IllegalStateException if the context cannot be found or resolved.
 */
@Suppress("UNCHECKED_CAST")
public inline fun <reified K : NavigationKey> Bundle.getNavigationContext(): NavigationContext<K> {
    val keyClassName: String = getString(NavigationContext.KEY_CLASS_ENTRY)
        ?: error("NavigationKey class name not found in arguments.")
    val keyPayload: String = getString(NavigationContext.KEY_PAYLOAD_ENTRY)
        ?: error("NavigationKey payload not found in arguments.")
    val entryId: String = getString(NavigationContext.ENTRY_ID_ENTRY)
        ?: error("Entry ID not found in arguments.")
    val correlationId: String = getString(NavigationContext.CORRELATION_ID_ENTRY)
        ?: error("Correlation ID not found in arguments.")

    val key: K = DefaultNavigationKeyCodec.decode(
        EncodedNavigationKey(
            className = keyClassName,
            payload = keyPayload,
        )
    ) as? K ?: error(
        "Decoded NavigationKey $keyClassName is not a ${K::class.simpleName}"
    )

    return NavigationContext(
        key = key,
        entryId = entryId,
        correlationId = correlationId,
    )
}

/** Retrieves the [NavigationKey] from fragment arguments. */
public inline fun <reified K : NavigationKey> Bundle.getNavigationKey(): K =
    getNavigationContext<K>().key
