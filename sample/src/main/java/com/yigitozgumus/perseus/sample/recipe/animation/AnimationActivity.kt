package com.yigitozgumus.perseus.sample.recipe.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator

@OptIn(ExperimentalMaterial3Api::class)
class AnimationActivity : ComponentActivity() {

    private val navigator: PerseusNavigator by lazy {
        createNavigator(
            composeProviders = listOf(AnimHomeProvider(), AnimDetailProvider()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Horizontal slide transition
        val slide = slideInHorizontally(tween(300)) { it } togetherWith
            slideOutHorizontally(tween(300)) { -it }

        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialKey = HomeKey,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { slide },
                popTransitionSpec = { slide },
            )
        }
    }

    inner class AnimHomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Custom Animation") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "Horizontal slide transition (300ms)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(10) { index ->
                        Text(
                            "Open Detail ${index + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clickable(onClick = dropUnlessResumed {
                                    navigator.navigateTo(DetailKey(index + 1))
                                })
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    inner class AnimDetailProvider : ComposeScreenProvider<DetailKey> {
        override fun canProvide(key: RouterKey) = key is DetailKey

        @Composable
        override fun Content(key: DetailKey) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Detail ${key.itemId}", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Press back to see reverse slide animation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
