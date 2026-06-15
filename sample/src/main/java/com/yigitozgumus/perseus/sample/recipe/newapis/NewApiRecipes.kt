package com.yigitozgumus.perseus.sample.recipe.newapis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.AndroidPerseusLogger
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.NonRestorableKey
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.PerseusLogLevel
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.PerseusResult
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.ScopeRestorePolicy
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.awaitResult
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.perseusGraph
import com.yigitozgumus.perseus.popCurrentTabToRoot
import com.yigitozgumus.perseus.popToRoot
import com.yigitozgumus.perseus.popUntilKeyType
import com.yigitozgumus.perseus.sample.recipe.ui.BackStackVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.ScopeVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class BackBehaviorActivity : NewApiRecipeActivity(RecipeMode.BackBehavior)
class ScopeResultActivity : NewApiRecipeActivity(RecipeMode.ScopeResult)
class NavigationHelpersActivity : NewApiRecipeActivity(RecipeMode.NavigationHelpers)
class RestoreGuardActivity : NewApiRecipeActivity(RecipeMode.RestoreGuards)

public enum class RecipeMode {
    BackBehavior,
    ScopeResult,
    NavigationHelpers,
    RestoreGuards,
}

abstract class NewApiRecipeActivity(
    private val mode: RecipeMode,
) : ComponentActivity() {

    private var scopeResultText by mutableStateOf("No scope result yet")

    private val graph = perseusGraph {
        screen<NewHomeKey> { HomeContent() }
        screen<NewSearchKey> { SearchContent() }
        screen<NewSettingsKey> { SettingsContent() }
        screen<NewDetailKey> { DetailContent(it) }
        screen<NewCheckoutKey> { CheckoutContent() }
        screen<NewPaymentKey> { PaymentContent() }
    }

    private val navigationOwner: PerseusNavigationOwner = PerseusNavigatorFactory.create(
        composeProviders = graph.composeProviders,
        fragmentProviders = emptyList(),
        sceneProviders = emptyList(),
        validateProviders = true,
        logger = AndroidPerseusLogger(tag = "Perseus", level = PerseusLogLevel.Debug)
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator
    private val scopeNavigator: PerseusScopeNavigator get() = navigationOwner.scopeNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = MultiStackSpec(
                    rootKeys = listOf(NewHomeKey, NewSearchKey),
                    restorePolicy = ScopeRestorePolicy.RestoreSavedState,
                    backBehavior = PerseusBackBehavior(
                        rootBackBehavior = RootBackBehavior.ExitHost,
                        tabBackBehavior = TabBackBehavior.SwitchToInitialTab,
                    ),
                ),
                modifier = Modifier.fillMaxSize(),
                tabTransitionSpec = { _, _ -> fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
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
        when (mode) {
            RecipeMode.BackBehavior -> BackBehaviorContent()
            RecipeMode.ScopeResult -> ScopeResultContent()
            RecipeMode.NavigationHelpers -> NavigationHelpersContent()
            RecipeMode.RestoreGuards -> RestoreGuardsContent()
        }
    }

    @Composable
    private fun BackBehaviorContent() {
        RecipeScaffold(
            title = "Back behavior policy",
            subtitle = "Root and tab back handling",
        ) {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "Configured behavior",
                body = "Root back exits this sample. Back at the Search tab root switches back to Home.",
            )
            RecipeButton("Push detail, then back pops") { navigator.navigateTo(NewDetailKey(1)) }
            SecondaryRecipeButton("Switch to Search tab") { navigator.switchTab(1) }
        }
    }

    @Composable
    private fun ScopeResultContent() {
        val coroutineScope = rememberCoroutineScope()
        RecipeScaffold(
            title = "Scope result API",
            subtitle = "pushScopeForResult and removeScope(result)",
        ) {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(title = "Latest result", body = scopeResultText)
            RecipeButton("Open checkout scope") {
                val handle = scopeNavigator.pushScopeForResult(
                    SingleStackSpec(
                        initialKey = NewCheckoutKey,
                        restorePolicy = ScopeRestorePolicy.NeverRestore,
                    )
                )
                coroutineScope.launch {
                    scopeResultText = when (val result = handle.awaitResult<String>()) {
                        is PerseusResult.Success -> result.value
                        PerseusResult.Cancelled -> "Cancelled"
                    }
                }
            }
            SecondaryRecipeButton("replaceApp(Settings)") {
                scopeNavigator.replaceApp(SingleStackSpec(NewSettingsKey))
            }
        }
    }

    @Composable
    private fun NavigationHelpersContent() {
        RecipeScaffold(
            title = "Navigation helpers",
            subtitle = "Graph registration, pop helpers, provider validation",
        ) {
            BackStackVisualizer(navigationOwner.debugSnapshot().currentBackStack)
            RecipeSection(
                title = "Declarative graph + validation",
                body = "This sample registers screens with perseusGraph { screen<Key> { ... } } and enables validateProviders.",
            )
            RecipeButton("Push detail flow") { navigator.navigateTo(NewDetailKey(1)) }
            SecondaryRecipeButton("Open detail 42") {
                navigator.navigateTo(NewDetailKey(42))
            }
            SecondaryRecipeButton("Replace app with settings") {
                scopeNavigator.replaceApp(SingleStackSpec(NewSettingsKey))
            }
        }
    }

    @Composable
    private fun RestoreGuardsContent() {
        RecipeScaffold(
            title = "Restore guards",
            subtitle = "NonRestorableKey and ScopeRestorePolicy",
        ) {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "Per-key guard",
                body = "The payment screen implements NonRestorableKey, so restored stacks are truncated before it.",
            )
            RecipeButton("Push non-restorable payment screen") { navigator.navigateTo(NewPaymentKey) }
            RecipeSection(
                title = "Scope restore policy",
                body = "Checkout scopes in the scope result sample are created with ScopeRestorePolicy.NeverRestore.",
            )
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
                title = "Try system back",
                body = "At this tab root, back switches to Home because TabBackBehavior.SwitchToInitialTab is configured.",
            )
        }
    }

    @Composable
    private fun SettingsContent() {
        RecipeScaffold(title = "Settings app scope") {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeSection(
                title = "replaceApp target",
                body = "This single-stack root replaced the tabbed app scope.",
            )
            RecipeButton("Restore tabbed app") {
                scopeNavigator.replaceApp(MultiStackSpec(listOf(NewHomeKey, NewSearchKey)))
            }
        }
    }

    @Composable
    private fun DetailContent(key: NewDetailKey) {
        RecipeScaffold(title = "Detail ${key.id}") {
            BackStackVisualizer(navigationOwner.debugSnapshot().currentBackStack)
            if (key.id == 1) {
                RecipeButton("Push detail 2") { navigator.navigateTo(NewDetailKey(2)) }
            } else {
                RecipeButton("popUntilKeyType<Detail>") { navigator.popUntilKeyType<NewDetailKey>() }
            }
            SecondaryRecipeButton("popToRoot") { navigator.popToRoot() }
        }
    }

    @Composable
    private fun CheckoutContent() {
        RecipeScaffold(title = "Checkout scope") {
            ScopeVisualizer(navigationOwner.debugSnapshot())
            RecipeButton("Complete checkout") {
                scopeNavigator.removeScope(scopeNavigator.currentScope.id, "Checkout completed")
            }
            SecondaryRecipeButton("Cancel checkout") {
                scopeNavigator.removeScope(scopeNavigator.currentScope.id, "Checkout cancelled")
            }
        }
    }

    @Composable
    private fun PaymentContent() {
        RecipeScaffold(title = "One-time payment") {
            RecipeSection(
                title = "NonRestorableKey",
                body = "This key opts out of restore. Use this for SDK-backed or transaction-only screens.",
            )
            RecipeButton("Pop current tab to root") { navigator.popCurrentTabToRoot() }
        }
    }
}

@Serializable
private data object NewHomeKey : NavigationKey

@Serializable
private data object NewSearchKey : NavigationKey

@Serializable
private data object NewSettingsKey : NavigationKey

@Serializable
private data object NewCheckoutKey : NavigationKey

@Serializable
private data class NewDetailKey(val id: Int) : NavigationKey

@Serializable
private data object NewPaymentKey : NonRestorableKey
