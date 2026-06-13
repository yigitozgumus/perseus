package com.yigitozgumus.perseus.sample.compose

import androidx.lifecycle.ViewModel
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.sample.keys.DetailKey
import java.util.UUID

class DetailViewModel(
    private val navigator: PerseusNavigator,
    private val key: DetailKey,
) : ViewModel() {

    val instanceId: String = UUID.randomUUID().toString().take(8)

    fun sendResult(context: NavigationContext<*>) {
        navigator.sendResult(context, "Selected item ${key.itemId}")
        navigator.pop()
    }
}
