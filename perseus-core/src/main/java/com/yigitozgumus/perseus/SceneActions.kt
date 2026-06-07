package com.yigitozgumus.perseus

interface SceneActions {
    fun <R : Any> sendResult(result: R)
    fun dismiss()
    fun <R : Any> sendResultAndDismiss(result: R) {
        sendResult(result)
        dismiss()
    }
}
