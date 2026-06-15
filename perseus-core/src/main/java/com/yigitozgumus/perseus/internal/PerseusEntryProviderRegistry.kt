package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.yigitozgumus.perseus.key.BottomSheetKey
import com.yigitozgumus.perseus.provider.SceneProvider
import com.yigitozgumus.perseus.provider.ScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import com.yigitozgumus.perseus.key.DialogKey
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.LocalNavigationContext
import com.yigitozgumus.perseus.LocalSceneActions
import com.yigitozgumus.perseus.EmptyPerseusLogger
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.PerseusLogger
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.SceneActions
import com.yigitozgumus.perseus.SceneResultCallback
import com.yigitozgumus.perseus.debug
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry that provides [NavEntry] instances for both Compose screens and Fragment screens.
 *
 * Resolution priority:
 * 1. Compose screen provider ([ScreenProvider])
 * 2. Compose scene provider ([SceneProvider]) — for dialogs/bottom sheets
 * 3. Fragment screen provider ([ScreenProvider]) — wrapped via [FragmentEntry]
 *
 * Also tracks:
 * - Group membership for group pop operations
 * - Correlation IDs for result routing
 */
internal class PerseusEntryProviderRegistry(
    private val composeProviders: List<ScreenProvider<*>>,
    private val fragmentProviders: List<FragmentProviderMarker>,
    private val sceneProviders: List<SceneProvider<*>>,
    private val resultBus: ResultBusAdapter,
    private val viewModelStoreProvider: PerseusViewModelStoreProvider,
    private val fragmentEntryFactory: FragmentEntryFactory? = null,
    private val logger: PerseusLogger = EmptyPerseusLogger,
) {
    // Per-navigate transition storage
    private val pendingTransitions = ConcurrentHashMap<String, ContentTransform>()

    /** Pop callback set by the navigator for scene dismissal. */
    var onPopCallback: (() -> Unit)? = null

    fun getGroupForKey(key: NavigationKey): GroupName? = key.groupName()
    fun clearTrackingForKey(key: NavigationKey) {
        logger.debug("clearTrackingForKey entryId=${key.backStackId()} key=${key.shortName()}")
        pendingTransitions.remove(key.backStackId())
    }

    fun setPendingTransition(key: NavigationKey, transition: ContentTransform) {
        logger.debug("setPendingTransition entryId=${key.backStackId()} key=${key.shortName()} transition=${transition::class.simpleName}")
        pendingTransitions[key.backStackId()] = transition
    }

    fun clearAllTracking() {
        logger.debug("clearAllTracking pendingTransitions=${pendingTransitions.size}")
        pendingTransitions.clear()
    }

    fun validateScope(scope: StackScopeSpec) {
        val keys = when (scope) {
            is SingleStackSpec -> listOf(scope.initialKey)
            is MultiStackSpec -> scope.rootKeys
        }
        keys.forEach(::validateProviderForKey)
    }

    fun validateProviderForKey(key: NavigationKey) {
        val matches = providerMatchesFor(key)
        require(matches.isNotEmpty()) { missingProviderMessage(key) }
        require(matches.size == 1) {
            "Multiple providers found for $key (${key::class.qualifiedName}).\n\n" +
                matches.joinToString(separator = "\n") { "- $it" } +
                "\n\nOnly one provider should claim a NavigationKey when provider validation is enabled."
        }
        if (matches.single().startsWith("Scene") && key !is DialogKey && key !is BottomSheetKey) {
            throw IllegalArgumentException(
                "Scene provider found for non-scene key $key (${key::class.qualifiedName}). " +
                    "Scene providers should handle DialogKey or BottomSheetKey destinations."
            )
        }
    }

    fun hasProviderFor(key: NavigationKey): Boolean = providerMatchesFor(key).isNotEmpty()

    private fun providerMatchesFor(key: NavigationKey): List<String> = buildList {
        composeProviders.filter { it.canProvide(key) }.forEach { add("Compose: ${providerName(it)}") }
        fragmentProviders.filter { it.canProvide(key) }.forEach { add("Fragment: ${providerName(it)}") }
        sceneProviders.filter { it.canProvide(key) }.forEach { add("Scene: ${providerName(it)}") }
    }

    @Suppress("UNCHECKED_CAST")
    fun provide(backStackKey: NavigationKey): NavEntry<NavigationKey> {
        val key = backStackKey.routeKey()
        val entryId = backStackKey.backStackId()
        val metadata = computeAndCacheMetadata(backStackKey)

        // 1. Compose screen provider
        composeProviders.find { it.canProvide(key) }?.let { foundProvider ->
            @Suppress("UNCHECKED_CAST")
            val typed = foundProvider as ScreenProvider<NavigationKey>
            val isScene = key is DialogKey || key is BottomSheetKey
            val navCtx = NavigationContext(
                key = key,
                entryId = entryId,
                correlationId = backStackKey.correlationId() ?: error("Missing correlationId for $entryId"),
            )
            logger.debug("provide Compose entryId=$entryId key=${key.shortName()} provider=${providerName(foundProvider)}")
            return NavEntry(key = backStackKey, contentKey = entryId, metadata = metadata) {
                CompositionLocalProvider(LocalNavigationContext provides navCtx) {
                    if (isScene) {
                        CompositionLocalProvider(LocalSceneActions provides createSceneActions(backStackKey)) {
                            typed.Content(key)
                        }
                    } else {
                        typed.Content(key)
                    }
                }
            }
        }

        // 2. Compose scene provider
        sceneProviders.find { it.canProvide(key) }?.let { provider ->
            val sceneCallback = object : SceneResultCallback {
                override fun <R : Any> sendResult(result: R) {
                    backStackKey.correlationId()?.let { resultBus.send(it, result) }
                }
            }
            val dismiss: () -> Unit = { onPopCallback?.invoke() ?: Unit }
            logger.debug("provide Scene entryId=$entryId key=${key.shortName()} provider=${providerName(provider)}")
            return NavEntry(key = backStackKey, contentKey = entryId, metadata = metadata) {
                (provider as SceneProvider<NavigationKey>).Content(
                    key = key,
                    onResult = sceneCallback,
                    onDismiss = dismiss
                )
            }
        }

        // 3. Fragment provider
        fragmentProviders.find { it.canProvide(key) }?.let { provider ->
            val ctx = NavigationContext(
                key = key,
                entryId = entryId,
                correlationId = backStackKey.correlationId() ?: error("Missing correlationId for $entryId"),
            )
            val factory = fragmentEntryFactory
                ?: throw IllegalArgumentException(
                    "Fragment provider found for ${key::class.simpleName} but no fragmentEntryFactory set. " +
                    "Add perseus-interop dependency and pass a FragmentEntryFactory to PerseusNavigatorFactory."
                )
            logger.debug("provide Fragment entryId=$entryId key=${key.shortName()} provider=${providerName(provider)}")
            return NavEntry(key = backStackKey, contentKey = entryId, metadata = metadata) {
                factory.Create(provider, key, ctx, viewModelStoreProvider)
            }
        }

        throw IllegalArgumentException(missingProviderMessage(key))
    }

    private fun missingProviderMessage(key: NavigationKey): String = buildString {
        appendLine("No provider found for ${key} (${key::class.qualifiedName}).")
        appendLine()
        appendLine("Registered Compose providers:")
        if (composeProviders.isEmpty()) appendLine("- <none>") else composeProviders.forEach { appendLine("- ${providerName(it)}") }
        appendLine()
        appendLine("Registered Fragment providers:")
        if (fragmentProviders.isEmpty()) appendLine("- <none>") else fragmentProviders.forEach { appendLine("- ${providerName(it)}") }
        appendLine()
        appendLine("Registered Scene providers:")
        if (sceneProviders.isEmpty()) appendLine("- <none>") else sceneProviders.forEach { appendLine("- ${providerName(it)}") }
    }

    private fun providerName(provider: Any): String =
        provider::class.qualifiedName ?: provider::class.simpleName ?: provider.toString()

    private fun NavigationKey.shortName(): String =
        routeKey()::class.simpleName ?: keyClassName(routeKey())

    private fun computeAndCacheMetadata(backStackKey: NavigationKey): Map<String, Any> {
        val key = backStackKey.routeKey()
        val id = backStackKey.backStackId()
        val sceneMeta = when (key) {
            is DialogKey -> DialogSceneStrategy.dialog(
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            )

            is BottomSheetKey -> BottomSheetSceneStrategy.bottomSheet(
                BottomSheetProperties(
                    dismissOnBackPress = key.isCancellable,
                    dismissOnSwipeDown = key.isDraggable,
                    dismissOnClickOutside = key.isCancellable
                )
            )

            else -> emptyMap()
        }
        val groupMeta = backStackKey.groupName()?.let { mapOf(GROUP_KEY to it) } ?: emptyMap()
        val transMeta = pendingTransitions.remove(id)?.let { transition ->
            NavDisplay.transitionSpec { transition }
        } ?: emptyMap()
        return sceneMeta + groupMeta + transMeta
    }

    private fun createSceneActions(backStackKey: NavigationKey): SceneActions = object : SceneActions {
        override fun <R : Any> sendResult(result: R) {
            backStackKey.correlationId()?.let { resultBus.send(it, result) }
        }

        override fun dismiss() {
            onPopCallback?.invoke()
        }
    }

    internal companion object {
        const val GROUP_KEY = "perseus_group"
    }
}
