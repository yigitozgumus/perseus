package com.yigitozgumus.perseus.sample.recipe.scope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.StackScopeKind
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.LoginKey
import com.yigitozgumus.perseus.sample.keys.ScopeFlowKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import com.yigitozgumus.perseus.sample.recipe.createNavigator

@OptIn(ExperimentalMaterial3Api::class)
class ScopeActivity : ComponentActivity() {

    private val navigator: PerseusNavigator = createNavigator(
        composeProviders = listOf(
            LoginProvider(),
            HomeProvider(),
            SearchProvider(),
            DetailProvider(),
            ScopeFlowProvider(),
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigator = navigator,
                initialScope = SingleStackSpec(LoginKey),
                modifier = Modifier.fillMaxSize(),
                bottomBar = { selectedIndex, onStackSelected ->
                    NavigationBar {
                        listOf("Home", "Search").forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = { onStackSelected(index) },
                                icon = {},
                                label = { Text(label) },
                            )
                        }
                    }
                },
            )
        }
    }

    inner class LoginProvider : ComposeScreenProvider<LoginKey> {
        override fun canProvide(key: RouterKey) = key is LoginKey

        @Composable
        override fun Content(key: LoginKey) {
            ScopeScaffold(title = "Single-stack root") {
                Text("Current scope: ${navigator.currentScope.kind}")
                Text("This starts as a single-stack scope with LoginKey as root.")
                Button(
                    onClick = dropUnlessResumed {
                        navigator.setRootScope(MultiStackSpec(listOf(HomeKey, SearchKey)))
                    },
                ) {
                    Text("Replace root with multi-stack scope")
                }
            }
        }
    }

    inner class HomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            ScopeScaffold(title = "Home stack") {
                ScopeStatus()
                Button(
                    onClick = dropUnlessResumed { navigator.navigateTo(DetailKey(1)) },
                ) {
                    Text("Push detail on Home stack")
                }
                Button(
                    onClick = dropUnlessResumed {
                        navigator.pushScope(SingleStackSpec(ScopeFlowKey))
                    },
                ) {
                    Text("Push temporary single-stack scope")
                }
                Button(
                    onClick = dropUnlessResumed { navigator.setRootScope(SingleStackSpec(LoginKey)) },
                ) {
                    Text("Replace root with Login scope")
                }
            }
        }
    }

    inner class SearchProvider : ComposeScreenProvider<SearchKey> {
        override fun canProvide(key: RouterKey) = key is SearchKey

        @Composable
        override fun Content(key: SearchKey) {
            ScopeScaffold(title = "Search stack") {
                ScopeStatus()
                Button(
                    onClick = dropUnlessResumed { navigator.navigateTo(DetailKey(2)) },
                ) {
                    Text("Push detail on Search stack")
                }
            }
        }
    }

    inner class ScopeFlowProvider : ComposeScreenProvider<ScopeFlowKey> {
        override fun canProvide(key: RouterKey) = key is ScopeFlowKey

        @Composable
        override fun Content(key: ScopeFlowKey) {
            ScopeScaffold(title = "Temporary scope") {
                ScopeStatus()
                Text("This single-stack scope sits above the multi-stack root.")
                Button(
                    onClick = dropUnlessResumed { navigator.navigateTo(DetailKey(99)) },
                ) {
                    Text("Push detail inside temporary scope")
                }
                Button(
                    onClick = dropUnlessResumed {
                        navigator.removeScope(navigator.currentScope.id)
                    },
                ) {
                    Text("Remove this scope")
                }
            }
        }
    }

    inner class DetailProvider : ComposeScreenProvider<DetailKey> {
        override fun canProvide(key: RouterKey) = key is DetailKey

        @Composable
        override fun Content(key: DetailKey) {
            ScopeScaffold(title = "Detail ${key.itemId}") {
                ScopeStatus()
                Button(onClick = dropUnlessResumed { navigator.pop() }) {
                    Text("Pop")
                }
                if (navigator.currentScope.kind == StackScopeKind.SingleStack) {
                    Button(
                        onClick = dropUnlessResumed {
                            navigator.removeScope(navigator.currentScope.id)
                        },
                    ) {
                        Text("Remove current scope")
                    }
                }
            }
        }
    }

    @Composable
    private fun ScopeScaffold(
        title: String,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                content()
            }
        }
    }

    @Composable
    private fun ScopeStatus() {
        val scope = navigator.currentScope
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Scope: ${scope.kind}")
            Text("Scope id: ${scope.id.value.take(8)}…")
            Text("Current stack index: ${scope.currentStackIndex ?: "none"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Back stack:")
                Text(scope.currentBackStack.joinToString { it::class.simpleName ?: it.toString() })
            }
        }
    }
}

