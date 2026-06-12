package com.yigitozgumus.perseus.sample.recipe.grouppop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.CheckoutStepKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner
import com.yigitozgumus.perseus.sample.recipe.ui.BackStackVisualizer
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton

class GroupPopActivity : ComponentActivity() {
    private val navigationOwner: PerseusNavigationOwner = createNavigationOwner(
        composeProviders = listOf(HomeProvider(), CheckoutProvider()),
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = SingleStackSpec(HomeKey),
                modifier = Modifier,
            )
        }
    }

    inner class HomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: NavigationKey) = key is HomeKey
        @Composable override fun Content(key: HomeKey) {
            RecipeScaffold(title = "Group pop", subtitle = "Demonstrates GroupName + popUntil") {
                RecipeSection("Checkout group", "Open several checkout steps, then clear the whole group at once.")
                RecipeButton("Start checkout") {
                    navigator.navigateTo(CheckoutStepKey(1), groupName = CheckoutGroup)
                }
            }
        }
    }

    inner class CheckoutProvider : ComposeScreenProvider<CheckoutStepKey> {
        override fun canProvide(key: NavigationKey) = key is CheckoutStepKey
        @Composable override fun Content(key: CheckoutStepKey) {
            RecipeScaffold(title = "Checkout step ${key.step}", subtitle = "All steps share the checkout group") {
                BackStackVisualizer(navigationOwner.scopeNavigator.currentScope.currentBackStack)
                if (key.step < 3) {
                    RecipeButton("Next checkout step") {
                        navigator.navigateTo(CheckoutStepKey(key.step + 1), groupName = CheckoutGroup)
                    }
                }
                SecondaryRecipeButton("Clear checkout group") { navigator.popUntil(CheckoutGroup) }
                SecondaryRecipeButton("Pop one") { navigator.pop() }
            }
        }
    }
}

private object CheckoutGroup : GroupName("checkout")
