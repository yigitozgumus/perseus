package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.PerseusLogLevel
import com.yigitozgumus.perseus.PerseusLogger
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.createTestPerseusNavigationOwner
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusLoggingTest {

    @Test
    fun loggerReceivesNavigationOperationsAndStackSnapshots() {
        val logger = RecordingLogger()
        val owner = createTestPerseusNavigationOwner(
            initialScope = SingleStackSpec(LoggingHome),
            logger = logger,
        )

        owner.navigator.navigateTo(LoggingDetail)
        owner.navigator.pop()

        assertTrue(logger.messages.any { it.contains("before navigateTo") })
        assertTrue(logger.messages.any { it.contains("after navigateTo") && it.contains("LoggingDetail") })
        assertTrue(logger.messages.any { it.contains("after pop") && it.contains("LoggingHome") })
        assertTrue(logger.messages.any { it.contains("cleanupRemoved") && it.contains("LoggingDetail") })
    }

    private class RecordingLogger : PerseusLogger {
        override val level: PerseusLogLevel = PerseusLogLevel.Debug
        val messages = mutableListOf<String>()

        override fun log(messageLevel: PerseusLogLevel, message: String) {
            messages += "${messageLevel.name}: $message"
        }
    }
}

@Serializable
private data object LoggingHome : RouterKey

@Serializable
private data object LoggingDetail : RouterKey
