package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.NavigationKey

/** Marker for destinations that should not be restored after process death. */
public interface NonRestorableKey : NavigationKey

/** Controls whether an individual scope participates in process-death restore. */
public enum class ScopeRestorePolicy {
    RestoreSavedState,
    NeverRestore,
}
