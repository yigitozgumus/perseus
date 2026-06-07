package com.yigitozgumus.perseus.sample.recipe.bottomsheet

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.LocalSceneActions
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.InfoSheetKey
import com.yigitozgumus.perseus.sample.recipe.createController

@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetActivity : ComponentActivity() {

    private val controller = createController(
        composeProviders = listOf(SheetHomeProvider(), InfoSheetProvider()),
    )
    private val navigator: PerseusNavigator = controller.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                controller = controller,
                initialKey = HomeKey,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    inner class SheetHomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Bottom Sheet") },
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
                    Text("BottomSheetSceneStrategy with swipe-to-dismiss")
                    Button(
                        onClick = { navigator.navigateTo(InfoSheetKey) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Show Bottom Sheet")
                    }
                }
            }
        }
    }

    inner class InfoSheetProvider : ComposeScreenProvider<InfoSheetKey> {
        override fun canProvide(key: RouterKey) = key is InfoSheetKey

        @Composable
        override fun Content(key: InfoSheetKey) {
            val actions = LocalSceneActions.current
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Information", style = MaterialTheme.typography.titleLarge)
                Text("This is a bottom sheet rendered via BottomSheetSceneStrategy.")
                Text("Swipe down to dismiss, or tap the button below.")
                Button(
                    onClick = { actions.sendResultAndDismiss("acknowledged") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Acknowledge")
                }
            }
        }
    }
}
