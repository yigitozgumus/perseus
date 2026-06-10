package com.yigitozgumus.perseus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusLoggerTest {

    @Test
    fun logLevelsAllowExpectedMessages() {
        assertFalse(PerseusLogLevel.None.allows(PerseusLogLevel.Error))

        assertTrue(PerseusLogLevel.Error.allows(PerseusLogLevel.Error))
        assertFalse(PerseusLogLevel.Error.allows(PerseusLogLevel.Info))
        assertFalse(PerseusLogLevel.Error.allows(PerseusLogLevel.Debug))

        assertTrue(PerseusLogLevel.Info.allows(PerseusLogLevel.Error))
        assertTrue(PerseusLogLevel.Info.allows(PerseusLogLevel.Info))
        assertFalse(PerseusLogLevel.Info.allows(PerseusLogLevel.Debug))

        assertTrue(PerseusLogLevel.Debug.allows(PerseusLogLevel.Error))
        assertTrue(PerseusLogLevel.Debug.allows(PerseusLogLevel.Info))
        assertTrue(PerseusLogLevel.Debug.allows(PerseusLogLevel.Debug))
    }
}
