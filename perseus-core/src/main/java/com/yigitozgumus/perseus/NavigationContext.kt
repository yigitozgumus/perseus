package com.yigitozgumus.perseus

import android.os.Bundle
import java.util.UUID

public data class NavigationContext<out K : RouterKey>(
    public val key: K,
    public val correlationId: String = UUID.randomUUID().toString()
) {
    public companion object {
        public const val KEY_CLASS_ENTRY: String = "perseus_key_class"
        public const val CORRELATION_ID_ENTRY: String = "perseus_correlation_id"
    }
}

@Suppress("UNCHECKED_CAST")
public inline fun <reified K : RouterKey> Bundle.getNavigationContext(): NavigationContext<K> {
    val keyClassName: String = getString(NavigationContext.KEY_CLASS_ENTRY)
        ?: error("RouterKey class name not found in arguments.")
    val correlationId: String = getString(NavigationContext.CORRELATION_ID_ENTRY)
        ?: error("Correlation ID not found in arguments.")

    val key: K = try {
        val clazz: Class<*> = Class.forName(keyClassName)
        val instance: Any? = clazz.getDeclaredField("INSTANCE").get(null)
        if (instance is K) instance else error("Cannot resolve RouterKey: $keyClassName is not a singleton of ${K::class.simpleName}")
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("RouterKey class not found: $keyClassName", e)
    }

    return NavigationContext(key, correlationId)
}

public inline fun <reified K : RouterKey> Bundle.getRouterKey(): K = getNavigationContext<K>().key
