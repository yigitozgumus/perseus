package com.yigitozgumus.perseus.sample.recipe.dialog

import android.os.Bundle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.LocalSceneActions
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.ConfirmDialogKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator

@OptIn(ExperimentalMaterial3Api::class)
class DialogActivity : ComponentActivity() {

    private val navigator: PerseusNavigator = createNavigator(
        composeProviders = listOf(DialogHomeProvider(), ConfirmDialogProvider()),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialKey = HomeKey,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    inner class DialogHomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            var lastResult by remember { mutableStateOf<String?>(null) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Dialog") },
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
                    Button(
                        onClick = {
                            val handle = navigator.navigateTo(ConfirmDialogKey)
                            // Simplified: dialog auto-dismisses via pop()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show Confirm Dialog")
                    }
                    if (lastResult != null) {
                        Text("Result: $lastResult")
                    }
                }
            }
        }
    }

    inner class ConfirmDialogProvider : ComposeScreenProvider<ConfirmDialogKey> {
        override fun canProvide(key: RouterKey) = key is ConfirmDialogKey

        @Composable
        override fun Content(key: ConfirmDialogKey) {
            val actions = LocalSceneActions.current
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Confirm Action", style = MaterialTheme.typography.titleLarge)
                Text("Are you sure you want to proceed?")
                Button(onClick = {
                    actions.sendResultAndDismiss("confirmed")
                }) {
                    Text("Yes")
                }
                Button(onClick = { actions.dismiss() }) {
                    Text("No")
                }
            }
        }
    }
}
