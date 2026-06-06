package com.yigitozgumus.perseus.api

import android.os.Bundle
import java.util.UUID

/**
 * Context passed to child screens containing navigation metadata.
 *
 * For process death / fragment argument transport, keys are stored
 * by their fully-qualified class name. Data objects (singletons) are
 * resolved via Class.forName + objectInstance. For data classes with
 * constructor arguments, the fragment must use a custom resolver.
 */
data class NavigationContext<out K : RouterKey>(
    val key: K,
    val correlationId: String = UUID.randomUUID().toString()
) {
    companion object {
        const val KEY_CLASS_ENTRY = "perseus_key_class"
        const val CORRELATION_ID_ENTRY = "perseus_correlation_id"
    }
}

/**
 * Retrieves the NavigationContext from fragment arguments.
 * Resolves the RouterKey from its class name.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified K : RouterKey> Bundle.getNavigationContext(): NavigationContext<K> {
    val keyClassName = getString(NavigationContext.KEY_CLASS_ENTRY)
        ?: error("RouterKey class name not found in arguments.")
    val correlationId = getString(NavigationContext.CORRELATION_ID_ENTRY)
        ?: error("Correlation ID not found in arguments.")

    val key: K = try {
        val clazz = Class.forName(keyClassName)
        val instance = clazz.getDeclaredField("INSTANCE").get(null)
        if (instance is K) instance else error("Cannot resolve RouterKey: $keyClassName is not a singleton object of type ${K::class.simpleName}")
    } catch (e: ClassNotFoundException) {
        error("RouterKey class not found: $keyClassName")
    }

    return NavigationContext(key, correlationId)
}

/** Retrieves the RouterKey from fragment arguments. */
inline fun <reified K : RouterKey> Bundle.getRouterKey(): K = getNavigationContext<K>().key
