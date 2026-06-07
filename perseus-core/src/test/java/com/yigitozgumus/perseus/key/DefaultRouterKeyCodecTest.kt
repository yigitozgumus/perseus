package com.yigitozgumus.perseus.key

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRouterKeyCodecTest {

    @Test
    fun encodesAndDecodesDataObjectKeys() {
        val encoded = DefaultRouterKeyCodec.encode(CodecObjectKey)

        val decoded = DefaultRouterKeyCodec.decode(encoded)

        assertEquals(CodecObjectKey, decoded)
    }

    @Test
    fun encodesAndDecodesDataClassKeysWithArguments() {
        val key = CodecDetailKey(itemId = 42, title = "Answer")
        val encoded = DefaultRouterKeyCodec.encode(key)

        val decoded = DefaultRouterKeyCodec.decode(encoded)

        assertEquals(key, decoded)
        assertTrue(decoded is CodecDetailKey)
    }
}

@Serializable
data object CodecObjectKey : RouterKey

@Serializable
data class CodecDetailKey(
    val itemId: Int,
    val title: String,
) : RouterKey
