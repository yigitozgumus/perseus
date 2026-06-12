package com.yigitozgumus.perseus.sample.compose

import androidx.lifecycle.ViewModel
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.NavigationKey

class DetailViewModel(
    private val navigator: PerseusNavigator
) : ViewModel() {

    fun sendResult(context: NavigationContext<*>, itemId: Int) {
        navigator.sendResult(context, "Selected item $itemId")
        navigator.pop()
    }
}
