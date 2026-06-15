package com.yigitozgumus.perseus

/**
 * Callback interface for scene results (dialogs and bottom sheets).
 *
 * Used by [SceneProvider] implementations to send results
 * back to the navigation system.
 */
public interface SceneResultCallback {
    /** Send a result of type [R] back to the parent screen. */
    public fun <R : Any> sendResult(result: R)
}
