package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.RouterKey

/** Reified convenience overload for [PerseusNavigator.popUntilKeyType]. */
public inline fun <reified K : RouterKey> PerseusNavigator.popUntilKeyType(): Unit =
    popUntilKeyType(K::class)
