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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.recipe.createNavigator
import kotlinx.serialization.Serializable

// Per-transition demo keys
@Serializable data object ScreenA : RouterKey
@Serializable data object ScreenB : RouterKey
@Serializable data object ScreenC : RouterKey
@Serializable data object ScreenD : RouterKey

@OptIn(ExperimentalMaterial3Api::class)
class PerTransitionActivity : ComponentActivity() {

    // Pre-built transitions
    private val slideRight: ContentTransform =
        slideInHorizontally(tween(350)) { it } togetherWith
            slideOutHorizontally(tween(350)) { -it / 3 }

    private val scaleUp: ContentTransform =
        scaleIn(tween(400)) + fadeIn(tween(400)) togetherWith
            scaleOut(tween(400)) + fadeOut(tween(400))

    private val fadeOnly: ContentTransform =
        fadeIn(tween(600)) togetherWith fadeOut(tween(300))

    private val slideUp: ContentTransform =
        slideInHorizontally(tween(350)) { -it } togetherWith
            slideOutHorizontally(tween(350)) { it }

    private val navigator: PerseusNavigator by lazy {
        createNavigator(
            composeProviders = listOf(
                ScreenProviderA(), ScreenProviderB(),
                ScreenProviderC(), ScreenProviderD(),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialKey = ScreenA,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // -- Providers --

    inner class ScreenProviderA : ComposeScreenProvider<ScreenA> {
        override fun canProvide(key: RouterKey) = key is ScreenA
        @Composable
        override fun Content(key: ScreenA) = ColoredScreen(
            "Screen A", "Slide from right →", Color(0xFFE3F2FD),
            onGoB = { navigator.navigateTo(ScreenB, transition = slideRight) },
            onGoC = { navigator.navigateTo(ScreenC, transition = scaleUp) },
        )
    }

    inner class ScreenProviderB : ComposeScreenProvider<ScreenB> {
        override fun canProvide(key: RouterKey) = key is ScreenB
        @Composable
        override fun Content(key: ScreenB) = ColoredScreen(
            "Screen B", "Scale up →", Color(0xFFFCE4EC),
            onGoB = { navigator.navigateTo(ScreenC, transition = scaleUp) },
            onGoC = { navigator.navigateTo(ScreenD, transition = fadeOnly) },
        )
    }

    inner class ScreenProviderC : ComposeScreenProvider<ScreenC> {
        override fun canProvide(key: RouterKey) = key is ScreenC
        @Composable
        override fun Content(key: ScreenC) = ColoredScreen(
            "Screen C", "Fade only →", Color(0xFFE8F5E9),
            onGoB = { navigator.navigateTo(ScreenD, transition = fadeOnly) },
            onGoC = { navigator.navigateTo(ScreenA, transition = slideUp) },
        )
    }

    inner class ScreenProviderD : ComposeScreenProvider<ScreenD> {
        override fun canProvide(key: RouterKey) = key is ScreenD
        @Composable
        override fun Content(key: ScreenD) = ColoredScreen(
            "Screen D", "Last screen", Color(0xFFFFF3E0),
            onGoB = { navigator.navigateTo(ScreenA, transition = slideRight) },
            onGoC = { },
        )
    }
}

@Composable
private fun ColoredScreen(
    title: String,
    subtitle: String,
    bgColor: Color,
    onGoB: () -> Unit,
    onGoC: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Press back to see reverse transition.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onGoB) { Text("Next with transition A") }
            Button(onClick = onGoC) { Text("Next with transition B") }
        }
    }
}
