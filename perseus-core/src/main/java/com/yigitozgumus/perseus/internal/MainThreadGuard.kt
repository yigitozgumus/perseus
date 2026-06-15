package com.yigitozgumus.perseus.internal

import android.os.Looper

internal object MainThreadGuard {
    private const val ERROR_MESSAGE =
        "Perseus navigation mutations must be called on the main thread."

    @Volatile
    var isMainThread: () -> Boolean = { Looper.getMainLooper().isCurrentThread }

    fun checkMainThread() {
        val onMainThread = try {
            isMainThread()
        } catch (error: RuntimeException) {
            // Android's local-unit-test Looper stub throws "not mocked". Keep JVM
            // tests useful while enforcing the guard on Android/runtime code.
            if (error.message?.contains("not mocked") == true) return
            throw error
        }
        check(onMainThread) { ERROR_MESSAGE }
    }
}
