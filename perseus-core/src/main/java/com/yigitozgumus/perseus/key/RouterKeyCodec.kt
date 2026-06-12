package com.yigitozgumus.perseus.key

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Serialized representation of a [NavigationKey]. */
@Serializable
public data class EncodedNavigationKey(
    public val className: String,
    public val payload: String,
) : java.io.Serializable

/** Encodes and decodes [NavigationKey] instances for saved state and fragment arguments. */
public interface NavigationKeyCodec {
    /** Encodes [key] into a stable string payload. */
    public fun encode(key: NavigationKey): EncodedNavigationKey

    /** Decodes a [NavigationKey] from [encoded]. */
    public fun decode(encoded: EncodedNavigationKey): NavigationKey
}

/**
 * Default [NavigationKeyCodec] backed by kotlinx.serialization generated serializers,
 * with a Parcelable fallback for Android-only keys.
 *
 * Prefer `@Serializable` keys when possible: the payload is stable, readable, and
 * easier to evolve. Keys that already implement [Parcelable] can still be saved
 * and restored without adding kotlinx.serialization annotations.
 */
public object DefaultNavigationKeyCodec : NavigationKeyCodec {
    private const val PARCELABLE_PREFIX = "parcelable:"

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    public override fun encode(key: NavigationKey): EncodedNavigationKey {
        val payload = runCatching {
            val serializer = serializerFor(key::class.java)
            json.encodeToString(serializer, key)
        }.getOrElse { serializationError ->
            if (key is Parcelable) {
                encodeParcelable(key)
            } else {
                throw notEncodableError(key::class.java, serializationError)
            }
        }
        return EncodedNavigationKey(
            className = key::class.qualifiedName ?: key::class.java.name,
            payload = payload,
        )
    }

    public override fun decode(encoded: EncodedNavigationKey): NavigationKey {
        val clazz = try {
            Class.forName(encoded.className)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(
                "NavigationKey class not found: ${encoded.className}",
                e,
            )
        }

        if (encoded.payload.startsWith(PARCELABLE_PREFIX)) {
            return decodeParcelable(clazz, encoded.payload)
        }

        val serializer = serializerFor(clazz)
        return try {
            json.decodeFromString(serializer, encoded.payload)
        } catch (e: SerializationException) {
            throw IllegalArgumentException(
                "NavigationKey payload could not be decoded for ${encoded.className}",
                e,
            )
        }
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun serializerFor(clazz: Class<*>): KSerializer<NavigationKey> {
        if (!NavigationKey::class.java.isAssignableFrom(clazz)) {
            throw IllegalArgumentException("Class is not a NavigationKey: ${clazz.name}")
        }
        return try {
            clazz.kotlin.serializer() as KSerializer<NavigationKey>
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "NavigationKey ${clazz.name} must be accessible and annotated with @Serializable",
                e,
            )
        }
    }

    private fun encodeParcelable(key: Parcelable): String {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeParcelable(key, 0)
            val bytes = parcel.marshall()
            PARCELABLE_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            parcel.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeParcelable(clazz: Class<*>, payload: String): NavigationKey {
        if (!Parcelable::class.java.isAssignableFrom(clazz)) {
            throw IllegalArgumentException("Parcelable payload found for non-Parcelable NavigationKey: ${clazz.name}")
        }
        val bytes = Base64.decode(payload.removePrefix(PARCELABLE_PREFIX), Base64.NO_WRAP)
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val restored = parcel.readParcelable<Parcelable>(clazz.classLoader)
            restored as? NavigationKey
                ?: throw IllegalArgumentException("Parcelable payload did not restore a NavigationKey: ${clazz.name}")
        } finally {
            parcel.recycle()
        }
    }

    private fun notEncodableError(clazz: Class<*>, cause: Throwable): IllegalArgumentException =
        IllegalArgumentException(
            "NavigationKey ${clazz.name} must be annotated with @Serializable or implement Parcelable.",
            cause,
        )
}
