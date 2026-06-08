package com.yigitozgumus.perseus.sample.recipe.compose

import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator

@OptIn(ExperimentalMaterial3Api::class)
class ComposeOnlyActivity : ComponentActivity() {

    private val navigator: PerseusNavigator = createNavigator(
        composeProviders = listOf(HomeProvider(), DetailProvider()),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialScope = SingleStackSpec(HomeKey),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    inner class HomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Compose Only") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(10) { index ->
                        val id = index + 1
                        Text(
                            text = "Open Detail $id",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clickable(
                                    onClick = dropUnlessResumed {
                                        navigator.navigateTo(DetailKey(id))
                                    }
                                )
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    inner class DetailProvider : ComposeScreenProvider<DetailKey> {
        override fun canProvide(key: RouterKey) = key is DetailKey

        @Composable
        override fun Content(key: DetailKey) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Detail ${key.itemId}",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
