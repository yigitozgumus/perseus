package com.yigitozgumus.perseus.sample.compose

import androidx.lifecycle.ViewModel
import com.yigitozgumus.perseus.api.NavigationContext
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.RouterKey

class DetailViewModel(
    private val navigator: PerseusNavigator
) : ViewModel() {

    fun sendResult(context: NavigationContext<*>, itemId: Int) {
        navigator.sendResult(context, "Selected item $itemId")
        navigator.pop()
    }
}
