package com.yigitozgumus.perseus

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.yigitozgumus.perseus.internal.BottomSheetSceneStrategy
import com.yigitozgumus.perseus.internal.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.internal.PerseusNavigationState
import com.yigitozgumus.perseus.internal.rememberPerseusViewModelStoreNavEntryDecorator
import com.yigitozgumus.perseus.key.RouterKey

/** Default fade duration in milliseconds. */
public const val DefaultTransitionDurationMs: Int = 200

/** Fast fade-in / fade-out transition used by default. */
public fun AnimatedContentTransitionScope<*>.fastFadeTransition(
    durationMs: Int = DefaultTransitionDurationMs,
): ContentTransform = fadeIn(tween(durationMs)) togetherWith fadeOut(tween(durationMs))

/**
 * Main navigation host composable for Perseus.
 *
 * Renders a [NavDisplay] driven by [PerseusNavigationState]. Supports
 * single-stack and multi-stack navigation containers, dialog/bottom sheet
 * scenes, and process death survival.
 *
 * Stack switching and reset are handled directly via [navigator].
 *
 * @param navigator The [PerseusNavigator] driving all navigation.
 * @param initialScope The initial stack scope to show before any root scope replacement.
 * @param modifier Compose modifier for the host container.
 * @param bottomBar Slot for the bottom navigation bar (multi-stack mode).
 *   Receives the current stack index and a callback for stack selection.
 * @param onStackChanged Notified when the selected stack changes (for UI state).
 * @param transitionSpec Forward navigation transition animation.
 * @param popTransitionSpec Back navigation transition animation.
 * @param predictivePopTransitionSpec Predictive back gesture animation.
 */
@Composable
public fun PerseusNavHost(
    navigator: PerseusNavigator,
    initialScope: StackScopeSpec,
    modifier: Modifier = Modifier,
    bottomBar: @Composable (
        selectedIndex: Int,
        onStackSelected: (Int) -> Unit,
    ) -> Unit = { _, _ -> },
    onStackChanged: (Int) -> Unit = {},
    transitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.() -> ContentTransform = {
        fastFadeTransition()
    },
    popTransitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.() -> ContentTransform = {
        fastFadeTransition()
    },
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.(progress: Int) -> ContentTransform = {
        fastFadeTransition()
    },
) {
    val stateHolder = navigator.stateHolder
    val entryRegistry = navigator.entryRegistry
    val viewModelStoreRegistry = navigator.viewModelStoreRegistry

    val navigationState = rememberSaveable(saver = PerseusNavigationState.Saver) {
        PerseusNavigationState.fromSpec(initialScope)
    }

    DisposableEffect(navigationState) {
        stateHolder.attach(navigationState)
        entryRegistry.onPopCallback = { navigator.pop() }
        onDispose { stateHolder.detach() }
    }

    val sceneStrategies = remember {
        listOf(
            BottomSheetSceneStrategy<RouterKey>(),
            DialogSceneStrategy(),
            SinglePaneSceneStrategy()
        )
    }

    if (!navigationState.isMultiStack) {
        NavDisplay(
            backStack = navigationState.currentBackStack,
            onBack = { navigator.pop() },
            modifier = modifier.fillMaxSize(),
            sceneStrategies = sceneStrategies,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberPerseusViewModelStoreNavEntryDecorator(viewModelStoreRegistry),
            ),
            entryProvider = { key -> entryRegistry.provide(key) },
        )
    } else {
        MultiStackHost(
            navigationState = navigationState,
            entryRegistry = entryRegistry,
            viewModelStoreRegistry = viewModelStoreRegistry,
            sceneStrategies = sceneStrategies,
            navigator = navigator,
            bottomBar = bottomBar,
            onStackChanged = onStackChanged,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            modifier = modifier,
        )
    }
}

@Composable
private fun MultiStackHost(
    navigationState: PerseusNavigationState,
    entryRegistry: PerseusEntryProviderRegistry,
    viewModelStoreRegistry: PerseusViewModelStoreProvider,
    sceneStrategies: List<SceneStrategy<RouterKey>>,
    navigator: PerseusNavigator,
    bottomBar: @Composable (Int, (Int) -> Unit) -> Unit,
    onStackChanged: (Int) -> Unit,
    transitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.() -> ContentTransform,
    popTransitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.() -> ContentTransform,
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<RouterKey>>.(Int) -> ContentTransform,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(navigationState.currentStackIndex) {
        onStackChanged(navigationState.currentStackIndex)
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
                onBack = { navigator.pop() },
                sceneStrategies = sceneStrategies,
                transitionSpec = transitionSpec,
                popTransitionSpec = popTransitionSpec,
                predictivePopTransitionSpec = predictivePopTransitionSpec,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberPerseusViewModelStoreNavEntryDecorator(viewModelStoreRegistry),
                ),
                entryProvider = { key -> entryRegistry.provide(key) },
            )
        }
        if (showBottomBar) {
            bottomBar(navigationState.currentStackIndex) { index ->
                if (index == navigationState.currentStackIndex) {
                    navigator.resetCurrentStack(resetRoot = false)
                } else {
                    navigator.switchStack(index)
                }
            }
        }
    }
}
