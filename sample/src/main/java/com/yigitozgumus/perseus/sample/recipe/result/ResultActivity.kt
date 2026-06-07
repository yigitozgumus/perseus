package com.yigitozgumus.perseus.sample.recipe.result

import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitozgumus.perseus.LocalNavigationContext
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.ReceiverKey
import com.yigitozgumus.perseus.sample.keys.SenderKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ResultActivity : ComponentActivity() {

    private val navigator: PerseusNavigator = createNavigator(
        composeProviders = listOf(SenderProvider(), ReceiverProvider()),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialKey = SenderKey,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    inner class SenderProvider : ComposeScreenProvider<SenderKey> {
        override fun canProvide(key: RouterKey) = key is SenderKey

        @Composable
        override fun Content(key: SenderKey) {
            var lastResult by remember { mutableStateOf<String?>(null) }
            var handle by remember { mutableStateOf<NavigationHandle?>(null) }

            if (handle != null) {
                androidx.compose.runtime.LaunchedEffect(handle!!.correlationId) {
                    handle!!.observeResult<String>().collect { lastResult = it }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Result Passing") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Open receiver, pick a value and come back.")
                    Button(onClick = { handle = navigator.navigateTo(ReceiverKey) }) {
                        Text("Open Receiver")
                    }
                    if (lastResult != null) {
                        Text(
                            text = "Received: $lastResult",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }

    inner class ReceiverProvider : ComposeScreenProvider<ReceiverKey> {
        override fun canProvide(key: RouterKey) = key is ReceiverKey

        @Composable
        override fun Content(key: ReceiverKey) {
            val ctx = LocalNavigationContext.current
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text("Pick a value to send back", style = MaterialTheme.typography.titleMedium)
                listOf("Alpha", "Beta", "Gamma").forEach { value ->
                    Button(onClick = {
                        ctx?.let { navigator.sendResult(it, value) }
                        navigator.pop()
                    }) {
                        Text(value)
                    }
                }
            }
        }
    }
}
