package com.yigitozgumus.perseus.sample.keys

import com.yigitozgumus.perseus.key.BottomSheetKey
import com.yigitozgumus.perseus.key.DialogKey
import com.yigitozgumus.perseus.key.NavigationKey
import kotlinx.serialization.Serializable

// Shared keys
@Serializable data object HomeKey : NavigationKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data class DetailKey(val itemId: Int) : NavigationKey
@Serializable data object SearchKey : NavigationKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data object ProfileKey : NavigationKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data object LoginKey : NavigationKey

// Recipe-specific keys
@Serializable data object FragmentScreenKey : NavigationKey
@Serializable data object SenderKey : NavigationKey
@Serializable data object ReceiverKey : NavigationKey
@Serializable data object ConfirmDialogKey : NavigationKey, DialogKey
@Serializable data object InfoSheetKey : NavigationKey, BottomSheetKey
@Serializable data object ScopeFlowKey : NavigationKey
@Serializable data class CheckoutStepKey(val step: Int) : NavigationKey
@Serializable data class CounterKey(val label: String) : NavigationKey
@Serializable data object HiddenBottomBarKey : NavigationKey
@Serializable data object RestoreAuthKey : NavigationKey
