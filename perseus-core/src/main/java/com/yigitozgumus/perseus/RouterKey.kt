package com.yigitozgumus.perseus

import androidx.navigation3.runtime.NavKey

interface RouterKey : NavKey {
    val hidesBottomNavigation: Boolean get() = true
}
