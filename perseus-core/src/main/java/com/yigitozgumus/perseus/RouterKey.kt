package com.yigitozgumus.perseus

import androidx.navigation3.runtime.NavKey

public interface RouterKey : NavKey {
    public val hidesBottomNavigation: Boolean get() = true
}
