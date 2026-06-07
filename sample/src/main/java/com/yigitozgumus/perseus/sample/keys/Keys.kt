package com.yigitozgumus.perseus.sample.keys

import com.yigitozgumus.perseus.key.BottomSheetKey
import com.yigitozgumus.perseus.key.DialogKey
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable

// Shared keys
@Serializable data object HomeKey : RouterKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data class DetailKey(val itemId: Int) : RouterKey
@Serializable data object SearchKey : RouterKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data object ProfileKey : RouterKey { override val hidesBottomNavigation: Boolean = false }
@Serializable data object LoginKey : RouterKey

// Recipe-specific keys
@Serializable data object FragmentScreenKey : RouterKey
@Serializable data object SenderKey : RouterKey
@Serializable data object ReceiverKey : RouterKey
@Serializable data object ConfirmDialogKey : RouterKey, DialogKey
@Serializable data object InfoSheetKey : RouterKey, BottomSheetKey
@Serializable data object CustomSheetKey : RouterKey
