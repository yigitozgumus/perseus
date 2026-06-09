package com.yigitozgumus.perseus

/**
 * Actions available to scene content (dialogs and bottom sheets).
 *
 * Provided by the navigation infrastructure via [LocalSceneActions]
 * when rendering [DialogKey] or [BottomSheetKey] entries.
 */
public interface SceneActions {
    /** Send a result back to the navigation handle observer. */
    public fun <R : Any> sendResult(result: R)

    /** Dismiss the current scene by popping it from the back stack. */
    public fun dismiss()

    /** Send a result and dismiss the scene in a single call. */
    public fun <R : Any> sendResultAndDismiss(result: R) {
        sendResult(result)
        dismiss()
    }
}
