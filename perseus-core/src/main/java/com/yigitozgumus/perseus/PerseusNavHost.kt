package com.yigitozgumus.perseus

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.yigitozgumus.perseus.key.NavigationKey

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
 * @param navigationOwner The [PerseusNavigationOwner] driving all navigation.
 * @param initialScope The initial stack scope to show before any root scope replacement.
 * @param modifier Compose modifier for the host container.
 * @param restorePolicy Controls whether saved navigation state is restored or ignored.
 * Back behavior is configured by [initialScope] and by later scope replacements.
 * @param bottomBar Slot for the bottom navigation bar (multi-stack mode).
 *   Receives the current tab index and a callback for tab selection.
 * @param onTabChanged Notified when the selected tab changes (for UI state).
 * @param transitionSpec Forward navigation transition animation.
 * @param popTransitionSpec Back navigation transition animation.
 * @param predictivePopTransitionSpec Predictive back gesture animation.
 * @param tabTransitionSpec Optional transition override for user-driven tab switches.
 */
@Composable
public fun PerseusNavHost(
    navigationOwner: PerseusNavigationOwner,
    initialScope: StackScopeSpec,
    modifier: Modifier = Modifier,
    restorePolicy: PerseusRestorePolicy = PerseusRestorePolicy.RestoreSavedState,
    bottomBar: @Composable (
        selectedIndex: Int,
        onTabSelected: (Int) -> Unit,
    ) -> Unit = { _, _ -> },
    onTabChanged: (Int) -> Unit = {},
    transitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.() -> ContentTransform = {
        fastFadeTransition()
    },
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.() -> ContentTransform = {
        fastFadeTransition()
    },
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.(progress: Int) -> ContentTransform = {
        fastFadeTransition()
    },
    tabTransitionSpec: ((fromIndex: Int, toIndex: Int) -> ContentTransform?)? = null,
) {
    val impl = navigationOwner.impl
    val navigator = navigationOwner.navigator
    val stateHolder = impl.stateHolder
    val entryRegistry = impl.entryRegistry
    val viewModelStoreRegistry = impl.viewModelStoreRegistry

    val navigationState = when (restorePolicy) {
        PerseusRestorePolicy.RestoreSavedState -> rememberSaveable(saver = PerseusNavigationState.Saver) {
            PerseusNavigationState.fromSpec(initialScope)
        }
        PerseusRestorePolicy.AlwaysUseInitialScope -> remember(initialScope) {
            PerseusNavigationState.fromSpec(initialScope)
        }
    }

    DisposableEffect(navigationState) {
        stateHolder.attach(navigationState)
        impl.syncCurrentKey()
        entryRegistry.onPopCallback = { navigator.pop() }
        if (impl.validateProviders) entryRegistry.validateScope(initialScope)
        onDispose { stateHolder.detach() }
    }

    val sceneStrategies = remember {
        listOf(
            BottomSheetSceneStrategy<NavigationKey>(),
            DialogSceneStrategy(),
            SinglePaneSceneStrategy()
        )
    }

    val backBehavior = navigationState.currentBackBehavior

    if (!navigationState.isMultiStack) {
        BackHandler(
            enabled = shouldInstallPerseusBackHandler(
                currentBackStackSize = navigationState.currentBackStack.size,
                isMultiStack = false,
                currentTabIndex = navigationState.currentTabIndex,
                behavior = backBehavior,
            ),
        ) {
            navigator.handleBack()
        }

        NavDisplay(
            backStack = navigationState.currentBackStack,
            onBack = { navigator.handleBack() },
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
            backBehavior = backBehavior,
            bottomBar = bottomBar,
            onTabChanged = onTabChanged,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            tabTransitionSpec = tabTransitionSpec,
            modifier = modifier,
        )
    }
}

@Composable
private fun MultiStackHost(
    navigationState: PerseusNavigationState,
    entryRegistry: PerseusEntryProviderRegistry,
    viewModelStoreRegistry: PerseusViewModelStoreProvider,
    sceneStrategies: List<SceneStrategy<NavigationKey>>,
    navigator: PerseusNavigator,
    backBehavior: PerseusBackBehavior,
    bottomBar: @Composable (Int, (Int) -> Unit) -> Unit,
    onTabChanged: (Int) -> Unit,
    transitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.() -> ContentTransform,
    popTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.() -> ContentTransform,
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavigationKey>>.(Int) -> ContentTransform,
    tabTransitionSpec: ((fromIndex: Int, toIndex: Int) -> ContentTransform?)?,
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

    var pendingTabTransition by remember { mutableStateOf<ContentTransform?>(null) }
    LaunchedEffect(navigationState.currentTabIndex) {
        pendingTabTransition = null
    }

    val backStack = navigationState.currentBackStack
    if (backStack.isEmpty()) return

    BackHandler(
        enabled = shouldInstallPerseusBackHandler(
            currentBackStackSize = backStack.size,
            isMultiStack = navigationState.isMultiStack,
            currentTabIndex = navigationState.currentTabIndex,
            behavior = backBehavior,
        ),
    ) {
        navigator.handleBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                onBack = { navigator.handleBack() },
                sceneStrategies = sceneStrategies,
                transitionSpec = { pendingTabTransition ?: transitionSpec() },
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
            bottomBar(navigationState.currentTabIndex) { index ->
                if (index == navigationState.currentTabIndex) {
                    navigator.resetCurrentTab(resetRoot = false)
                } else {
                    pendingTabTransition = tabTransitionSpec?.invoke(navigationState.currentTabIndex, index)
                    navigator.switchTab(index)
                }
            }
        }
    }
}

internal fun shouldInstallPerseusBackHandler(
    currentBackStackSize: Int,
    isMultiStack: Boolean,
    currentTabIndex: Int,
    behavior: PerseusBackBehavior,
): Boolean {
    if (currentBackStackSize > 1) return false
    if (isMultiStack) {
        when (behavior.tabBackBehavior) {
            TabBackBehavior.StayOnCurrentTab -> Unit
            TabBackBehavior.SwitchToInitialTab -> if (currentTabIndex != 0) return true
            TabBackBehavior.ResetCurrentTab -> return true
        }
    }
    return behavior.rootBackBehavior == RootBackBehavior.Block
}
