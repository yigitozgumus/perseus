package com.yigitozgumus.perseus

public interface SceneActions {
    public fun <R : Any> sendResult(result: R)
    public fun dismiss()
    public fun <R : Any> sendResultAndDismiss(result: R) {
        sendResult(result)
        dismiss()
    }
}
