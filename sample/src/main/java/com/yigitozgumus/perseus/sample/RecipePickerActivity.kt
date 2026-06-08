package com.yigitozgumus.perseus.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.sample.recipe.animation.AnimationActivity
import com.yigitozgumus.perseus.sample.recipe.animation.PerTransitionActivity
import com.yigitozgumus.perseus.sample.recipe.bottomsheet.BottomSheetActivity
import com.yigitozgumus.perseus.sample.recipe.compose.ComposeOnlyActivity
import com.yigitozgumus.perseus.sample.recipe.customsheet.CustomSheetActivity
import com.yigitozgumus.perseus.sample.recipe.dialog.DialogActivity
import com.yigitozgumus.perseus.sample.recipe.interop.InteropActivity
import com.yigitozgumus.perseus.sample.recipe.result.ResultActivity
import com.yigitozgumus.perseus.sample.recipe.scope.ScopeActivity

class RecipePickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipePickerScreen(
                onRecipeSelected = { activity ->
                    startActivity(Intent(this, activity))
                }
            )
        }
    }
}

private data class Recipe(
    val title: String,
    val description: String,
    val activity: Class<out ComponentActivity>,
)

private val recipes = listOf(
    Recipe(
        "Compose Only",
        "Basic navigation between Compose screens",
        ComposeOnlyActivity::class.java,
    ),
    Recipe(
        "Fragment Interop",
        "Compose screens + legacy Fragment wrapped in Compose",
        InteropActivity::class.java,
    ),
    Recipe(
        "Result Passing",
        "Sending results between screens via PerseusNavigator",
        ResultActivity::class.java,
    ),
    Recipe(
        "Dialog",
        "DialogKey entries rendered as dialogs via DialogSceneStrategy",
        DialogActivity::class.java,
    ),
    Recipe(
        "Bottom Sheet",
        "BottomSheetKey entries with swipe-to-dismiss",
        BottomSheetActivity::class.java,
    ),
    Recipe(
        "Custom Sheet",
        "Custom bottom sheet implementation",
        CustomSheetActivity::class.java,
    ),
    Recipe(
        "Animations",
        "Custom enter/exit transitions via PerseusNavHost",
        AnimationActivity::class.java,
    ),
    Recipe(
        "Per-Transition",
        "Each screen opens/closes with a different animation",
        PerTransitionActivity::class.java,
    ),
    Recipe(
        "Stack Scopes",
        "Replace root scopes and push/remove temporary scopes",
        ScopeActivity::class.java,
    ),
    Recipe(
        "Full Demo",
        "Multi-stack interface with all features (original sample)",
        SampleActivity::class.java,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipePickerScreen(
    onRecipeSelected: (Class<out ComponentActivity>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perseus Recipes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(recipes.size) { index ->
                val recipe = recipes[index]
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = dropUnlessResumed {
                                onRecipeSelected(recipe.activity)
                            }
                        ),
                    headlineContent = {
                        Text(
                            text = recipe.title,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
    }
}
