package com.yigitozgumus.perseus.sample.recipe.viewmodellifetime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigationOwner
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.sample.keys.CounterKey
import com.yigitozgumus.perseus.sample.recipe.createNavigationOwner
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeScaffold
import com.yigitozgumus.perseus.sample.recipe.ui.RecipeSection
import com.yigitozgumus.perseus.sample.recipe.ui.SecondaryRecipeButton
import com.yigitozgumus.perseus.sample.recipe.ui.StatePill
import java.util.UUID

class ViewModelLifetimeActivity : ComponentActivity() {
    private val navigationOwner: PerseusNavigationOwner = createNavigationOwner(
        composeProviders = listOf(CounterProvider()),
    )
    private val navigator: PerseusNavigator get() = navigationOwner.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                navigationOwner = navigationOwner,
                initialScope = SingleStackSpec(CounterKey("root")),
                modifier = Modifier,
            )
        }
    }

    inner class CounterProvider : ComposeScreenProvider<CounterKey> {
        override fun canProvide(key: NavigationKey) = key is CounterKey
        @Composable override fun Content(key: CounterKey) {
            val owner = checkNotNull(LocalViewModelStoreOwner.current)
            val viewModel = remember(owner) {
                ViewModelProvider(owner)[CounterViewModel::class.java]
            }
            RecipeScaffold(title = "Entry ViewModel", subtitle = key.label) {
                RecipeSection("Entry-scoped store", "Push the same route multiple times. Each entry gets a distinct ViewModelStore and counter.") {
                    StatePill("vm: ${viewModel.id.take(8)}")
                    StatePill("count: ${viewModel.count}")
                }
                RecipeButton("Increment this entry") { viewModel.count++ }
                RecipeButton("Push same route again") { navigator.navigateTo(CounterKey("duplicate")) }
                SecondaryRecipeButton("Pop") { navigator.pop() }
            }
        }
    }
}

class CounterViewModel : ViewModel() {
    val id: String = UUID.randomUUID().toString()
    var count: Int by mutableIntStateOf(0)
}
