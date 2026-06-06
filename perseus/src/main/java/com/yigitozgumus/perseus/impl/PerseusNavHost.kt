package com.yigitozgumus.perseus.impl

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.yigitozgumus.perseus.api.RouterKey

private const val FADE_MS = 200

/**
 * Main navigation host composable for Perseus.
 *
 * Renders a [NavDisplay] driven by [PerseusNavigationState]. Handles:
 * - Unauthenticated mode (single stack)
 * - Authenticated mode (tabbed with bottom navigation)
 * - Dialog and bottom sheet scenes via [SceneStrategy]
 * - Process death survival via `rememberSaveable`
 */
@Composable
fun PerseusNavHost(
    stateHolder: PerseusNavigationStateHolder,
    entryRegistry: PerseusEntryProviderRegistry,
    onPop: () -> Unit,
    initialKey: RouterKey,
    bottomBar: @Composable (selectedIndex: Int, onTabSelected: (Int) -> Unit) -> Unit = { _, _ -> },
    onTabChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navigationState = rememberSaveable(saver = PerseusNavigationState.Saver) {
        PerseusNavigationState.unauthenticated(initialKey)
    }

    DisposableEffect(navigationState) {
        stateHolder.attach(navigationState)
        entryRegistry.onPopCallback = onPop
        onDispose { stateHolder.detach() }
    }

    val sceneStrategies = remember {
        listOf(BottomSheetSceneStrategy<RouterKey>(), DialogSceneStrategy())
    }

    val isUnauthenticated = navigationState.mode == PerseusNavigationState.Mode.Unauthenticated

    if (isUnauthenticated) {
        NavDisplay(
            backStack = navigationState.currentBackStack,
            onBack = onPop,
            modifier = modifier.fillMaxSize(),
            sceneStrategies = sceneStrategies,
            transitionSpec = { fastFade() },
            popTransitionSpec = { fastFade() },
            predictivePopTransitionSpec = { fastFade() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = { key -> entryRegistry.provide(key) }
        )
    } else {
        AuthenticatedHost(
            navigationState = navigationState,
            entryRegistry = entryRegistry,
            sceneStrategies = sceneStrategies,
            onPop = onPop,
            bottomBar = bottomBar,
            onTabChanged = onTabChanged,
            modifier = modifier
        )
    }
}

@Composable
private fun AuthenticatedHost(
    navigationState: PerseusNavigationState,
    entryRegistry: PerseusEntryProviderRegistry,
    sceneStrategies: List<SceneStrategy<RouterKey>>,
    onPop: () -> Unit,
    bottomBar: @Composable (Int, (Int) -> Unit) -> Unit,
    onTabChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(navigationState.currentTabIndex) {
        onTabChanged(navigationState.currentTabIndex)
    }

    val showBottomBar by remember(navigationState.currentBackStack.toList()) {
        derivedStateOf {
            navigationState.currentBackStack.lastOrNull()?.hidesBottomNavigation != true
        }
    }

    val backStack = navigationState.currentBackStack
    if (backStack.isEmpty()) return

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = onPop,
                sceneStrategies = sceneStrategies,
                transitionSpec = { fastFade() },
                popTransitionSpec = { fastFade() },
                predictivePopTransitionSpec = { fastFade() },
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                entryProvider = { key -> entryRegistry.provide(key) }
            )
        }
        if (showBottomBar) {
            bottomBar(navigationState.currentTabIndex) { index ->
                onTabChanged(index)
            }
        }
    }
}

private fun fastFade() = fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
