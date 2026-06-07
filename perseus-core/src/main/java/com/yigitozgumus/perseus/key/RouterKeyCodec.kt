package com.yigitozgumus.perseus.key

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Serialized representation of a [RouterKey]. */
@Serializable
public data class EncodedRouterKey(
    public val className: String,
    public val payload: String,
) : java.io.Serializable

/** Encodes and decodes [RouterKey] instances for saved state and fragment arguments. */
public interface RouterKeyCodec {
    /** Encodes [key] into a stable string payload. */
    public fun encode(key: RouterKey): EncodedRouterKey

    /** Decodes a [RouterKey] from [encoded]. */
    public fun decode(encoded: EncodedRouterKey): RouterKey
}

/**
 * Default [RouterKeyCodec] backed by kotlinx.serialization generated serializers.
 *
 * Router keys must be annotated with `@Serializable`. Unknown or non-serializable
 * key types fail with an actionable [IllegalArgumentException].
 */
public object DefaultRouterKeyCodec : RouterKeyCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    public override fun encode(key: RouterKey): EncodedRouterKey {
        val serializer = serializerFor(key::class.java)
        return EncodedRouterKey(
            className = key::class.qualifiedName ?: key::class.java.name,
            payload = json.encodeToString(serializer, key),
        )
    }

    public override fun decode(encoded: EncodedRouterKey): RouterKey {
        val clazz = try {
            Class.forName(encoded.className)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(
                "RouterKey class not found: ${encoded.className}",
                e,
            )
        }

        val serializer = serializerFor(clazz)
        return try {
            json.decodeFromString(serializer, encoded.payload)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(
                "RouterKey payload could not be decoded for ${encoded.className}",
                e,
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun serializerFor(clazz: Class<*>): KSerializer<RouterKey> {
        if (!RouterKey::class.java.isAssignableFrom(clazz)) {
            throw IllegalArgumentException("Class is not a RouterKey: ${clazz.name}")
        }
        return try {
            clazz.kotlin.serializer() as KSerializer<RouterKey>
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "RouterKey ${clazz.name} must be accessible and annotated with @Serializable",
                e,
            )
        }
    }
}
