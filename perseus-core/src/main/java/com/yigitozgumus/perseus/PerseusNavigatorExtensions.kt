package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.NavigationKey

/** Reified convenience overload for [PerseusNavigator.popUntilKeyType]. */
public inline fun <reified K : NavigationKey> PerseusNavigator.popUntilKeyType(): Unit =
    popUntilKeyType(K::class)

/** Alias for [PerseusNavigator.resetCurrentTab] that reads like common navigation APIs. */
public fun PerseusNavigator.popToRoot(resetRoot: Boolean = false): Unit =
    resetCurrentTab(resetRoot)

/** Alias for [PerseusNavigator.resetTab] that reads like common navigation APIs. */
public fun PerseusNavigator.popTabToRoot(tabIndex: Int, resetRoot: Boolean = false): Unit =
    resetTab(tabIndex, resetRoot)

/** Alias for [PerseusNavigator.resetCurrentTab] that reads like common navigation APIs. */
public fun PerseusNavigator.popCurrentTabToRoot(resetRoot: Boolean = false): Unit =
    resetCurrentTab(resetRoot)
