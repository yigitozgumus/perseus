package com.yigitozgumus.perseus.key

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNavigationKeyCodecTest {

    @Test
    fun encodesAndDecodesDataObjectKeys() {
        val encoded = DefaultNavigationKeyCodec.encode(CodecObjectKey)

        val decoded = DefaultNavigationKeyCodec.decode(encoded)

        assertEquals(CodecObjectKey, decoded)
    }

    @Test
    fun encodesAndDecodesDataClassKeysWithArguments() {
        val key = CodecDetailKey(itemId = 42, title = "Answer")
        val encoded = DefaultNavigationKeyCodec.encode(key)

        val decoded = DefaultNavigationKeyCodec.decode(encoded)

        assertEquals(key, decoded)
        assertTrue(decoded is CodecDetailKey)
    }
}

@Serializable
data object CodecObjectKey : NavigationKey

@Serializable
data class CodecDetailKey(
    val itemId: Int,
    val title: String,
) : NavigationKey
