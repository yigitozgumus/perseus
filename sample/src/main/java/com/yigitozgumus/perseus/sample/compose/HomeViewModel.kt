package com.yigitozgumus.perseus.sample.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusResult
import com.yigitozgumus.perseus.resultFlow
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val navigator: PerseusNavigator
) : ViewModel() {

    private val _lastResult = MutableStateFlow<String?>(null)
    val lastResult: StateFlow<String?> = _lastResult.asStateFlow()

    fun navigateToDetail(itemId: Int) {
        val handle = navigator.navigateTo(DetailKey(itemId))
        viewModelScope.launch {
            handle.resultFlow<String>().collect { result ->
                if (result is PerseusResult.Success) _lastResult.value = result.value
            }
        }
    }

    fun navigateToProfile() {
        val handle = navigator.navigateTo(ProfileKey)
        viewModelScope.launch {
            handle.resultFlow<String>().collect { result ->
                if (result is PerseusResult.Success) _lastResult.value = result.value
            }
        }
    }
}
