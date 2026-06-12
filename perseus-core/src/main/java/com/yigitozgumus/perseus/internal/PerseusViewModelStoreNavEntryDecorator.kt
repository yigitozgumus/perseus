package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntryDecorator
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.key.NavigationKey

@Composable
internal fun rememberPerseusViewModelStoreNavEntryDecorator(
    viewModelStoreProvider: PerseusViewModelStoreProvider,
): NavEntryDecorator<NavigationKey> = remember(viewModelStoreProvider) {
    NavEntryDecorator(
        decorate = { entry ->
            val entryId = entry.contentKey as String
            val owner = remember(entryId) { viewModelStoreProvider.getOwner(entryId) }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                entry.Content()
            }
        },
    )
}
