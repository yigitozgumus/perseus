package com.yigitozgumus.perseus.sample.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import org.koin.core.annotation.Single

@Single
class SearchScreenProvider : ComposeScreenProvider<SearchKey> {
    override fun canProvide(key: RouterKey) = key is SearchKey

    @Composable
    override fun Content(key: SearchKey) {
        SearchScreen()
    }
}

@Composable
fun SearchScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        var count by rememberSaveable { mutableIntStateOf(0) }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(20) { index ->
                Text(
                    text = "Search result ${index + 1}${if (count > 0) " • taps: $count" else ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
