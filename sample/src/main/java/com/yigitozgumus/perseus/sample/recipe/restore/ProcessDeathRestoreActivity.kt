package com.yigitozgumus.perseus.sample.recipe.restore

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
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.DetailKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.LoginKey
import com.yigitozgumus.perseus.sample.keys.RestoreAuthKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.ScopeVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton

class ProcessDeathRestoreActivity : ComponentActivity() {
    private val navigationOwner: PerseusNavigationOwner = createNavigationOwner(
        composeProviders = listOf(LoginProvider(), HomeProvider(), SearchProvider(), DetailProvider(), AuthProvider()),
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator
    private val scopeNavigator: PerseusScopeNavigator get() = navigationOwner.scopeNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = SingleStackSpec(LoginKey),
                modifier = Modifier,
                bottomBar = { selected, onTabSelected ->
                    NavigationBar {
                        listOf("Home", "Search").forEachIndexed { index, label ->
                            NavigationBarItem(selected = selected == index, onClick = { onTabSelected(index) }, icon = {}, label = { Text(label) })
                        }
                    }
                },
            )
        }
    }

    inner class LoginProvider : ComposeScreenProvider<LoginKey> {
        override fun canProvide(key: NavigationKey) = key is LoginKey
        @Composable override fun Content(key: LoginKey) {
            RecipeScaffold(title = "Process death restore", subtitle = "Cold start begins at Login") {
                RecipeSection("Manual test", "Authenticate, push details, switch tabs, background the app, kill the process, then relaunch from recents. Saved NavHost state should restore the multi-stack scope.")
                RecipeButton("Authenticate into multi-stack") {
                    scopeNavigator.setRootScope(MultiStackSpec(listOf(HomeKey, SearchKey)))
                }
            }
        }
    }

    inner class HomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: NavigationKey) = key is HomeKey
        @Composable override fun Content(key: HomeKey) = AuthContent("Home", 1)
    }

    inner class SearchProvider : ComposeScreenProvider<SearchKey> {
        override fun canProvide(key: NavigationKey) = key is SearchKey
        @Composable override fun Content(key: SearchKey) = AuthContent("Search", 10)
    }

    inner class AuthProvider : ComposeScreenProvider<RestoreAuthKey> {
        override fun canProvide(key: NavigationKey) = key is RestoreAuthKey
        @Composable override fun Content(key: RestoreAuthKey) = AuthContent("Auth", 20)
    }

    @Composable private fun AuthContent(title: String, id: Int) {
        RecipeScaffold(title = title, subtitle = "Restored by rememberSaveable after process death") {
            ScopeVisualizer(scopeNavigator.currentScope)
            RecipeButton("Push detail") { navigator.navigateTo(DetailKey(id)) }
            SecondaryRecipeButton("Logout / replace with Login") { scopeNavigator.setRootScope(SingleStackSpec(LoginKey)) }
        }
    }

    inner class DetailProvider : ComposeScreenProvider<DetailKey> {
        override fun canProvide(key: NavigationKey) = key is DetailKey
        @Composable override fun Content(key: DetailKey) {
            RecipeScaffold(title = "Detail ${key.itemId}") {
                ScopeVisualizer(scopeNavigator.currentScope)
                SecondaryRecipeButton("Pop") { navigator.pop() }
            }
        }
    }
}
