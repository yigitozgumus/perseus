package com.yigitozgumus.perseus.sample.recipe.v3

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.DeepLinkResolver
import com.yigitozgumus.perseus.DeepLinkTarget
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.NonRestorableKey
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.ScopeRestorePolicy
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.handleDeepLink
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.perseusGraph
import com.yigitozgumus.perseus.popUntilKeyType
import com.yigitozgumus.perseus.sample.recipe.ui.BackStackVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.ScopeVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class V3FeaturesActivity : ComponentActivity() {

    private var scopeResultText by mutableStateOf("No scope result yet")

    private val graph = perseusGraph {
        screen<V3HomeKey> { HomeContent() }
        screen<V3SearchKey> { SearchContent() }
        screen<V3SettingsKey> { SettingsContent() }
        screen<V3DetailKey> { DetailContent(it) }
        screen<V3CheckoutKey> { CheckoutContent() }
        screen<V3OneTimePaymentKey> { OneTimePaymentContent() }
    }

    private val navigationOwner: PerseusNavigationOwner = PerseusNavigatorFactory.create(
        composeProviders = graph.composeProviders,
        fragmentProviders = emptyList(),
        sceneProviders = emptyList(),
        validateProviders = true,
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator
    private val scopeNavigator: PerseusScopeNavigator get() = navigationOwner.scopeNavigator

    private val deepLinkResolver = DeepLinkResolver { uri ->
        when (uri.host) {
            "detail" -> DeepLinkTarget.Key(V3DetailKey(uri.lastPathSegment?.toIntOrNull() ?: 0))
            "settings" -> DeepLinkTarget.Scope(SingleStackSpec(V3SettingsKey))
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = MultiStackSpec(
                    rootKeys = listOf(V3HomeKey, V3SearchKey),
                    restorePolicy = ScopeRestorePolicy.RestoreSavedState,
                ),
                modifier = Modifier.fillMaxSize(),
                backBehavior = PerseusBackBehavior(
                    rootBackBehavior = RootBackBehavior.Block,
                    tabBackBehavior = TabBackBehavior.SwitchToInitialTab,
                ),
                tabTransitionSpec = { _, _ ->
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                },
                bottomBar = { selected, onTabSelected ->
                    NavigationBar {
                        listOf("Home", "Search").forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { onTabSelected(index) },
                                icon = {},
                                label = { Text(label) },
                            )
                        }
                    }
                },
            )
        }
    }

    @Composable
    private fun HomeContent() {
        val coroutineScope = rememberCoroutineScope()
        RecipeScaffold(
            title = "v3 feature samples",
            subtitle = "Back policy, scope results, graph registration, deep links, and helpers",
        ) {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "Back behavior policy",
                body = "Back pops when possible. At a tab root it switches to the first tab; at the first root it is blocked.",
            )
            RecipeSection(
                title = "Scope results",
                body = scopeResultText,
            ) {
                RecipeButton("Open checkout scope for result") {
                    val handle = scopeNavigator.pushScopeForResult(
                        SingleStackSpec(
                            initialKey = V3CheckoutKey,
                            restorePolicy = ScopeRestorePolicy.NeverRestore,
                        )
                    )
                    coroutineScope.launch {
                        scopeResultText = handle.observeResult<String>().first()
                    }
                }
            }
            RecipeSection(
                title = "Navigation helpers",
                body = "Push two detail screens, then use popUntilKeyType<Detail>() to clear the flow.",
            ) {
                RecipeButton("Push detail flow") { navigator.navigateTo(V3DetailKey(1)) }
                SecondaryRecipeButton("Open deep link: detail/42") {
                    navigator.handleDeepLink(Uri.parse("perseus://detail/42"), deepLinkResolver)
                }
            }
            RecipeSection(
                title = "Scope helpers",
                body = "replaceApp is a semantic alias for replacing the root app/session scope.",
            ) {
                SecondaryRecipeButton("replaceApp(Settings)") {
                    scopeNavigator.replaceApp(SingleStackSpec(V3SettingsKey))
                }
                SecondaryRecipeButton("Scope deep link: settings") {
                    scopeNavigator.handleDeepLink(Uri.parse("perseus://settings"), deepLinkResolver)
                }
            }
        }
    }

    @Composable
    private fun SearchContent() {
        RecipeScaffold(
            title = "Search tab",
            subtitle = "Tab switching uses tabTransitionSpec",
        ) {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "Tab back behavior",
                body = "Press system back at this tab root to switch back to Home instead of exiting.",
            )
            RecipeButton("Push non-restorable payment screen") {
                navigator.navigateTo(V3OneTimePaymentKey)
            }
        }
    }

    @Composable
    private fun SettingsContent() {
        RecipeScaffold(title = "Settings app scope") {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "replaceApp / deep link target",
                body = "This single-stack root replaced the tabbed app scope.",
            )
            RecipeButton("Restore tabbed app") {
                scopeNavigator.replaceApp(MultiStackSpec(listOf(V3HomeKey, V3SearchKey)))
            }
        }
    }

    @Composable
    private fun DetailContent(key: V3DetailKey) {
        RecipeScaffold(title = "Detail ${key.id}") {
            BackStackVisualizer(navigationOwner.debugSnapshot().currentBackStack)
            if (key.id == 1) {
                RecipeButton("Push detail 2") { navigator.navigateTo(V3DetailKey(2)) }
            } else {
                RecipeButton("popUntilKeyType<Detail>") { navigator.popUntilKeyType<V3DetailKey>() }
            }
            SecondaryRecipeButton("popToRoot") { navigator.popToRoot() }
        }
    }

    @Composable
    private fun CheckoutContent() {
        RecipeScaffold(title = "Checkout scope") {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "ScopeRestorePolicy.NeverRestore",
                body = "This temporary flow is marked as a non-restoring scope in its spec.",
            )
            RecipeButton("Complete checkout") {
                scopeNavigator.removeScope(scopeNavigator.currentScope.id, "Checkout completed")
            }
            SecondaryRecipeButton("Cancel checkout") {
                scopeNavigator.removeScope(scopeNavigator.currentScope.id, "Checkout cancelled")
            }
        }
    }

    @Composable
    private fun OneTimePaymentContent() {
        RecipeScaffold(title = "One-time payment") {
            RecipeSection(
                title = "NonRestorableKey",
                body = "This key implements NonRestorableKey. If process state is restored with it present, Perseus truncates the restored stack before this entry.",
            )
            RecipeButton("Pop current tab to root") { navigator.popCurrentTabToRoot() }
        }
    }
}

@Serializable
private data object V3HomeKey : RouterKey

@Serializable
private data object V3SearchKey : RouterKey

@Serializable
private data object V3SettingsKey : RouterKey

@Serializable
private data object V3CheckoutKey : RouterKey

@Serializable
private data class V3DetailKey(val id: Int) : RouterKey

@Serializable
private data object V3OneTimePaymentKey : NonRestorableKey
