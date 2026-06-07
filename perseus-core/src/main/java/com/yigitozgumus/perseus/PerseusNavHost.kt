package com.yigitozgumus.perseus

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
import com.yigitozgumus.perseus.internal.BottomSheetSceneStrategy
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationState
import com.yigitozgumus.perseus.internal.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.internal.PerseusNavigatorImpl
import com.yigitozgumus.perseus.key.RouterKey

private const val FADE_MS = 200

/**
 * Main navigation host composable for Perseus.
 *
 * Renders a [NavDisplay] driven by [PerseusNavigationState]. Supports
 * unauthenticated (single stack) and authenticated (tabbed) modes,
 * dialog/bottom sheet scenes, and process death survival.
 *
 * @param navigator The [PerseusNavigator] instance driving navigation.
 * @param onPop Called when the user navigates back.
 * @param initialKey The initial screen to show before any auth transition.
 * @param modifier Compose modifier for the host container.
 * @param bottomBar Slot for the bottom navigation bar (authenticated mode).
 * @param onTabChanged Notified when the selected tab changes.
 * @param onSwitchTab Called when the user selects a different tab.
 * @param onResetCurrentTab Called when the current tab is re-selected.
 */
@Composable
public fun PerseusNavHost(
    navigator: PerseusNavigator,
    onPop: () -> Unit,
    initialKey: RouterKey,
    modifier: Modifier = Modifier,
    bottomBar: @Composable (
        selectedIndex: Int,
        onTabSelected: (Int) -> Unit,
    ) -> Unit = { _, _ -> },
    onTabChanged: (Int) -> Unit = {},
    onSwitchTab: (Int) -> Unit = {},
    onResetCurrentTab: () -> Unit = {},
) {
    val impl = navigator as PerseusNavigatorImpl
    val stateHolder = impl.stateHolder
    val entryRegistry = impl.entryRegistry

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

    val isUnauthenticated =
        navigationState.mode == PerseusNavigationState.Mode.Unauthenticated

    if (isUnauthenticated) {
        NavDisplay(
            backStack = navigationState.currentBackStack,
            onBack = onPop,
            modifier = modifier.fillMaxSize(),
            sceneStrategies = sceneStrategies,
            transitionSpec = { fastFade() },
            popTransitionSpec = { fastFade() },
            predictivePopTransitionSpec = { fastFade() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
            ),
            entryProvider = { key -> entryRegistry.provide(key) },
        )
    } else {
        AuthenticatedHost(
            navigationState = navigationState,
            entryRegistry = entryRegistry,
            sceneStrategies = sceneStrategies,
            onPop = onPop,
            bottomBar = bottomBar,
            onTabChanged = onTabChanged,
            onSwitchTab = onSwitchTab,
            onResetCurrentTab = onResetCurrentTab,
            modifier = modifier,
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
    onSwitchTab: (Int) -> Unit,
    onResetCurrentTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(navigationState.currentTabIndex) {
        onTabChanged(navigationState.currentTabIndex)
    }

    val showBottomBar by remember(navigationState.currentBackStack.toList()) {
        derivedStateOf {
            navigationState.currentBackStack.lastOrNull()
                ?.hidesBottomNavigation != true
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
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                ),
                entryProvider = { key -> entryRegistry.provide(key) },
            )
        }
        if (showBottomBar) {
            bottomBar(navigationState.currentTabIndex) { index ->
                if (index == navigationState.currentTabIndex) {
                    onResetCurrentTab()
                } else {
                    onSwitchTab(index)
                }
            }
        }
    }
}

private fun fastFade() =
    fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
