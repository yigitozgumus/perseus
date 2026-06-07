package com.yigitozgumus.perseus.sample.recipe.result

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.LocalNavigationContext
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.ReceiverKey
import com.yigitozgumus.perseus.sample.keys.SenderKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ResultActivity : ComponentActivity() {

    private val resultState = MutableStateFlow<String?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val navigator: PerseusNavigator by lazy {
        createNavigator(
            composeProviders = listOf(SenderProvider(), ReceiverProvider()),
        )
    }

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

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    inner class SenderProvider : ComposeScreenProvider<SenderKey> {
        override fun canProvide(key: RouterKey) = key is SenderKey

        @Composable
        override fun Content(key: SenderKey) {
            val lastResult by resultState.collectAsState()

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
                    Button(onClick = {
                        val handle = navigator.navigateTo(ReceiverKey)
                        scope.launch {
                            handle.observeResult<String>().collect { r ->
                                resultState.value = r
                            }
                        }
                    }) {
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
                verticalArrangement = Arrangement.spacedBy(
                    16.dp, Alignment.CenterVertically,
                ),
            ) {
                Text("Pick a value", style = MaterialTheme.typography.titleMedium)
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
