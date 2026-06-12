package com.yigitozgumus.perseus.sample.recipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yigitozgumus.perseus.StackScopeSnapshot
import com.yigitozgumus.perseus.key.NavigationKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScaffold(
    title: String,
    subtitle: String? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
fun RecipeSection(
    title: String,
    body: String? = null,
    content: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            content()
        }
    }
}

@Composable
fun RecipeButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
fun SecondaryRecipeButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
fun StatePill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun BackStackVisualizer(entries: List<NavigationKey>) {
    RecipeSection(title = "Current back stack") {
        Text(entries.joinToString(" → ") { it.shortName() })
    }
}

@Composable
fun ScopeVisualizer(scope: StackScopeSnapshot) {
    RecipeSection(title = "Current scope") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatePill(scope.kind.name)
            StatePill("Tab: ${scope.currentStackIndex ?: "none"}")
            StatePill("Entries: ${scope.currentBackStack.size}")
        }
        Text("id: ${scope.id.value.take(8)}…", style = MaterialTheme.typography.bodySmall)
        Text("Back stack: ${scope.currentBackStack.joinToString(" → ") { it.shortName() }}")
        if (scope.rootKeys.isNotEmpty()) {
            Text("Roots: ${scope.rootKeys.joinToString { it.shortName() }}")
        }
    }
}

fun NavigationKey.shortName(): String = this::class.simpleName ?: toString()
