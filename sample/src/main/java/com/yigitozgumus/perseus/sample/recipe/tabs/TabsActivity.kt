package com.yigitozgumus.perseus.sample.recipe.tabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner
import com.yigitozgumus.perseus.sample.recipe.ui.BackStackVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton

class TabsActivity : ComponentActivity() {
    private val navigationOwner: PerseusNavigationOwner = createNavigationOwner(
        composeProviders = listOf(HomeProvider(), SearchProvider(), DetailProvider()),
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = MultiStackSpec(listOf(HomeKey, SearchKey)),
                modifier = Modifier,
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

    inner class HomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: NavigationKey) = key is HomeKey
        @Composable override fun Content(key: HomeKey) = TabContent("Home", 100)
    }

    inner class SearchProvider : ComposeScreenProvider<SearchKey> {
        override fun canProvide(key: NavigationKey) = key is SearchKey
        @Composable override fun Content(key: SearchKey) = TabContent("Search", 200)
    }

    @Composable
    private fun TabContent(label: String, idBase: Int) {
        RecipeScaffold(
            title = "$label tab",
            subtitle = "Each tab owns an independent back stack",
        ) {
            RecipeSection("Try it", "Push details in one tab, switch tabs, then come back.")
            RecipeButton("Push detail in $label") {
                navigator.navigateTo(DetailKey(idBase + navigator.currentTabIndex))
            }
            SecondaryRecipeButton("Reset current tab") {
                navigator.resetCurrentTab(resetRoot = false)
            }
        }
    }

    inner class DetailProvider : ComposeScreenProvider<DetailKey> {
        override fun canProvide(key: NavigationKey) = key is DetailKey
        @Composable
        override fun Content(key: DetailKey) {
            RecipeScaffold(title = "Detail ${key.itemId}", subtitle = "Still inside tab ${navigator.currentTabIndex}") {
                BackStackVisualizer(navigationOwner.scopeNavigator.currentScope.currentBackStack)
                RecipeButton("Push another detail") { navigator.navigateTo(DetailKey(key.itemId + 1)) }
                SecondaryRecipeButton("Pop") { navigator.pop() }
            }
        }
    }
}
