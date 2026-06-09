package com.yigitozgumus.perseus

import android.util.Log

/** Logging level used by Perseus navigation diagnostics. */
public enum class PerseusLogLevel {
    None,
    Error,
    Info,
    Debug,
}

/**
 * Logger hook for observing Perseus navigation operations.
 *
 * Pass a logger to [PerseusNavigatorFactory.create] to inspect stack mutations,
 * scope changes, provider resolution, and ViewModelStore cleanup.
 */
public interface PerseusLogger {
    /** Lowest level this logger accepts. */
    public val level: PerseusLogLevel

    /** Writes [message] if [level] allows [messageLevel]. */
    public fun log(messageLevel: PerseusLogLevel, message: String)
}

/** Logger that drops every message. */
public object EmptyPerseusLogger : PerseusLogger {
    override val level: PerseusLogLevel = PerseusLogLevel.None
    override fun log(messageLevel: PerseusLogLevel, message: String): Unit = Unit
}

/** Android Logcat logger for Perseus diagnostics. */
public class AndroidPerseusLogger(
    private val tag: String = "Perseus",
    override val level: PerseusLogLevel = PerseusLogLevel.Info,
) : PerseusLogger {
    override fun log(messageLevel: PerseusLogLevel, message: String) {
        if (!level.allows(messageLevel)) return
        when (messageLevel) {
            PerseusLogLevel.None -> Unit
            PerseusLogLevel.Error -> Log.e(tag, message)
            PerseusLogLevel.Info -> Log.i(tag, message)
            PerseusLogLevel.Debug -> Log.d(tag, message)
        }
    }
}

internal fun PerseusLogger.debug(message: String) {
    log(PerseusLogLevel.Debug, message)
}

internal fun PerseusLogger.info(message: String) {
    log(PerseusLogLevel.Info, message)
}

internal fun PerseusLogger.error(message: String) {
    log(PerseusLogLevel.Error, message)
}

internal fun PerseusLogLevel.allows(messageLevel: PerseusLogLevel): Boolean = when (this) {
    PerseusLogLevel.None -> false
    PerseusLogLevel.Error -> messageLevel == PerseusLogLevel.Error
    PerseusLogLevel.Info -> messageLevel == PerseusLogLevel.Error || messageLevel == PerseusLogLevel.Info
    PerseusLogLevel.Debug -> messageLevel != PerseusLogLevel.None
}
