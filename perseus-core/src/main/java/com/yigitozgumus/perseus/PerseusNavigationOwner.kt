package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.DefaultPerseusNavigator

/**
 * Owner for a Perseus navigation runtime.
 *
 * Pass this to [PerseusNavHost]. Expose [navigator] to screens/ViewModels for
 * route and tab navigation, and [scopeNavigator] to app/session orchestration
 * that is allowed to replace or stack navigation scopes.
 */
public class PerseusNavigationOwner internal constructor(
    internal val impl: DefaultPerseusNavigator,
) {
    public val navigator: PerseusNavigator = impl
    public val scopeNavigator: PerseusScopeNavigator = impl
}
