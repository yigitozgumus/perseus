package com.yigitozgumus.perseus

/** Controls whether [PerseusNavHost] restores saved navigation state. */
public enum class PerseusRestorePolicy {
    /** Restore saved navigation state for the host when Android saved state is available. */
    RestoreSavedState,

    /** Ignore saved navigation state and always create state from the current initial scope. */
    AlwaysUseInitialScope,
}
