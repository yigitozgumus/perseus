package com.yigitozgumus.perseus.sample.keys

import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data class DetailKey(val itemId: Int) : RouterKey

@Serializable
data object ProfileKey : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
data object LoginKey : RouterKey
