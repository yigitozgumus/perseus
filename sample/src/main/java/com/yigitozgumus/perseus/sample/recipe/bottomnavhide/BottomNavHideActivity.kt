package com.yigitozgumus.perseus.sample.recipe.bottomnavhide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ScreenProvider
import com.yigitozgumus.perseus.sample.keys.HiddenBottomBarKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton

class BottomNavHideActivity : ComponentActivity() {
    private val navigationOwner: PerseusNavigationOwner = createNavigationOwner(
        composeProviders = listOf(HomeProvider(), SearchProvider(), HiddenProvider()),
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
                                label = { Text(label) })
                        }
                    }
                },
            )
        }
    }

    inner class HomeProvider : ScreenProvider<HomeKey> {
        override fun canProvide(key: NavigationKey) = key is HomeKey
        @Composable
        override fun Content(key: HomeKey) = Root("Home")
    }

    inner class SearchProvider : ScreenProvider<SearchKey> {
        override fun canProvide(key: NavigationKey) = key is SearchKey
        @Composable
        override fun Content(key: SearchKey) = Root("Search")
    }

    @Composable
    private fun Root(label: String) {
        RecipeScaffold(title = "$label root", subtitle = "Bottom bar is visible here") {
            RecipeSection(
                "hidesBottomNavigation",
                "The next screen sets hidesBottomNavigation = true on its NavigationKey."
            )
            RecipeButton("Open full-screen detail") { navigator.navigateTo(HiddenBottomBarKey) }
        }
    }

    inner class HiddenProvider : ScreenProvider<HiddenBottomBarKey> {
        override fun canProvide(key: NavigationKey) = key is HiddenBottomBarKey
        @Composable
        override fun Content(key: HiddenBottomBarKey) {
            RecipeScaffold(
                title = "Full-screen detail",
                subtitle = "Bottom bar hidden by NavigationKey"
            ) {
                RecipeSection("Notice", "The bottom navigation bar is not rendered for this entry.")
                SecondaryRecipeButton("Pop") { navigator.pop() }
            }
        }
    }
}
