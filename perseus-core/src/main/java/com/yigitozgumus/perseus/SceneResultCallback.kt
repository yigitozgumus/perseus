package com.yigitozgumus.perseus

interface SceneResultCallback {
    fun <R : Any> sendResult(result: R)
}
