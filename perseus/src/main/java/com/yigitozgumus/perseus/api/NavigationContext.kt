package com.yigitozgumus.perseus.api

import android.os.Bundle
import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Context passed to child screens containing navigation metadata.
 *
 * Wraps a [RouterKey] with a [correlationId] for scoped result routing.
 * The child screen uses this context to send results back to the correct parent
 * via [PerseusNavigator.sendResult].
 *
 * @param K The specific RouterKey type for type-safe access.
 */
data class NavigationContext<out K : RouterKey>(
    val key: K,
    val correlationId: String = UUID.randomUUID().toString()
) {
    companion object {
        @PublishedApi internal const val KEY_BUNDLE_ENTRY = "perseus_key"
        @PublishedApi internal const val CORRELATION_ID_BUNDLE_ENTRY = "perseus_correlation_id"

        @PublishedApi internal val json = Json { ignoreUnknownKeys = true }

        inline fun <reified K : RouterKey> writeToBundle(bundle: Bundle, context: NavigationContext<K>) {
            bundle.putString(KEY_BUNDLE_ENTRY, Base64.encodeToString(
                json.encodeToString(context.key).toByteArray(), Base64.NO_WRAP
            ))
            bundle.putString(CORRELATION_ID_BUNDLE_ENTRY, context.correlationId)
        }
    }
}

/**
 * Retrieves the NavigationContext from fragment arguments.
 *
 * Usage in Fragment:
 * ```kotlin
 * private val navigationContext: NavigationContext<DetailKey> by lazy {
 *     requireArguments().getNavigationContext()
 * }
 * ```
 *
 * @throws IllegalStateException if context is not found.
 */
inline fun <reified K : RouterKey> Bundle.getNavigationContext(): NavigationContext<K> {
    val keyJson = getString(NavigationContext.KEY_BUNDLE_ENTRY)
        ?: error("NavigationContext key not found. Ensure the screen was opened via PerseusNavigator.")
    val correlationId = getString(NavigationContext.CORRELATION_ID_BUNDLE_ENTRY)
        ?: error("NavigationContext correlationId not found.")
    val decoded = String(Base64.decode(keyJson, Base64.NO_WRAP))
    val key = Json.decodeFromString<K>(decoded)
    return NavigationContext(key, correlationId)
}

/**
 * Retrieves the RouterKey from fragment arguments.
 *
 * @throws IllegalStateException if RouterKey is not found.
 */
inline fun <reified K : RouterKey> Bundle.getRouterKey(): K {
    return getNavigationContext<K>().key
}
