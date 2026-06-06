package com.yigitozgumus.perseus.sample.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.api.ComposeScreenProvider
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeScreenProvider : ComposeScreenProvider<HomeKey>, KoinComponent {
    private val navigator: PerseusNavigator by inject()

    override fun canProvide(key: RouterKey) = key is HomeKey

    @Composable
    override fun Content(key: HomeKey) {
        HomeScreen(
            onItemClick = { navigator.navigateTo(DetailKey(it)) },
            onProfileClick = { navigator.navigateTo(ProfileKey) }
        )
    }
}

class DetailScreenProvider : ComposeScreenProvider<DetailKey> {
    override fun canProvide(key: RouterKey) = key is DetailKey

    @Composable
    override fun Content(key: DetailKey) {
        DetailScreen(itemId = key.itemId)
    }
}

@Composable
fun HomeScreen(
    onItemClick: (Int) -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Perseus Sample",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item {
                Button(
                    onClick = dropUnlessResumed { onProfileClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Go to Profile (Fragment)")
                }
            }
            items(10) { index ->
                val itemId = index + 1
                Text(
                    text = "Item $itemId",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = dropUnlessResumed { onItemClick(itemId) })
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun DetailScreen(itemId: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Detail Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Item ID: $itemId",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
