package com.yigitozgumus.perseus.key

/**
 * Marker interface for [NavigationKey] types that render as dialogs.
 *
 * Keys implementing this interface are automatically rendered as dialogs
 * via [DialogSceneStrategy] when navigated to via [PerseusNavigator.navigateTo].
 */
public interface DialogKey : NavigationKey
