package com.yigitozgumus.perseus

/** Configures what Perseus should do when back is pressed at stack/tab roots. */
public data class PerseusBackBehavior(
    val rootBackBehavior: RootBackBehavior = RootBackBehavior.ExitHost,
    val tabBackBehavior: TabBackBehavior = TabBackBehavior.StayOnCurrentTab,
)

public enum class RootBackBehavior {
    /** Do not consume the root back press; let the host/activity handle it. */
    ExitHost,

    /** Consume the root back press and keep the user on the current root. */
    Block,
}

public enum class TabBackBehavior {
    /** At a tab root, keep the current tab selected. */
    StayOnCurrentTab,

    /** At a non-initial tab root, switch to the initial tab. */
    SwitchToInitialTab,

    /** At a tab root, reset the current tab root entry. */
    ResetCurrentTab,
}
