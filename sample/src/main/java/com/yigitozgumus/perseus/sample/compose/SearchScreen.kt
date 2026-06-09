package com.yigitozgumus.perseus.sample.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class SearchScreenProvider : ComposeScreenProvider<SearchKey>, KoinComponent {
    private val navigator: PerseusNavigator by inject()

    override fun canProvide(key: RouterKey) = key is SearchKey

    @Composable
    override fun Content(key: SearchKey) {
        SearchScreen(
            onItemClick = { navigator.navigateTo(DetailKey(it)) },
        )
    }
}

@Composable
fun SearchScreen(onItemClick: (Int) -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    val allItems = List(50) { "Item ${it + 1} — ${("Alpha Beta Gamma Delta Epsilon").split(" ").random()}" }
    val filtered = if (query.isBlank()) emptyList()
    else allItems.filter { it.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            placeholder = { Text("Type to search…") },
            singleLine = true,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filtered.size) { index ->
                val item = filtered[index]
                val itemId = allItems.indexOf(item) + 1
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = dropUnlessResumed { onItemClick(itemId) })
                        .padding(12.dp),
                )
            }
            if (query.isNotBlank() && filtered.isEmpty()) {
                item {
                    Text(
                        "No results for \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
