package com.yigitozgumus.perseus.sample.recipe.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner

@OptIn(ExperimentalMaterial3Api::class)
class AnimationActivity : ComponentActivity() {

    private val navigationOwner: PerseusNavigationOwner by lazy {
        createNavigationOwner(
            composeProviders = listOf(AnimHomeProvider(), AnimDetailProvider()),
        )
    }
    private val navigator: PerseusNavigator get() = navigationOwner.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = SingleStackSpec(HomeKey),
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    slideInHorizontally(tween(300)) { it } togetherWith
                        slideOutHorizontally(tween(300)) { -it }
                },
                popTransitionSpec = {
                    slideInHorizontally(tween(300)) { -it } togetherWith
                        slideOutHorizontally(tween(300)) { it }
                },
            )
        }
    }

    inner class AnimHomeProvider : ScreenProvider<HomeKey> {
        override fun canProvide(key: NavigationKey) = key is HomeKey

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
                        val id = index + 1
                        val scaleAnim = scaleIn(tween(300)) + fadeIn(tween(300)) togetherWith
                            scaleOut(tween(300)) + fadeOut(tween(300))
                        Text(
                            "Open Detail $id${if (id == 5) " (scale anim)" else if (id == 10) " (fade)" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clickable(onClick = dropUnlessResumed {
                                    navigator.navigateTo(
                                        DetailKey(id),
                                        transition = when (id) {
                                            5 -> scaleAnim
                                            else -> null
                                        },
                                    )
                                })
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    inner class AnimDetailProvider : ScreenProvider<DetailKey> {
        override fun canProvide(key: NavigationKey) = key is DetailKey

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
