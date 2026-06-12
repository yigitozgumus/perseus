package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.NavigationKey

/** Reified convenience overload for [PerseusNavigator.popUntilKeyType]. */
public inline fun <reified K : NavigationKey> PerseusNavigator.popUntilKeyType(): Unit =
    popUntilKeyType(K::class)
