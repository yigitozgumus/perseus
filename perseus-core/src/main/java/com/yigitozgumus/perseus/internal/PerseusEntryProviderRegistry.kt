package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.DialogSceneStrategy
import com.yigitozgumus.perseus.key.BottomSheetKey
import com.yigitozgumus.perseus.provider.ComposeSceneProvider
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentEntryFactory
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import com.yigitozgumus.perseus.key.DialogKey
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.LocalNavigationContext
import com.yigitozgumus.perseus.LocalSceneActions
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.PerseusViewModelStoreProvider
import com.yigitozgumus.perseus.SceneActions
import com.yigitozgumus.perseus.SceneResultCallback
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry that provides [NavEntry] instances for both Compose screens and Fragment screens.
 *
 * Resolution priority:
 * 1. Compose screen provider ([ComposeScreenProvider])
 * 2. Compose scene provider ([ComposeSceneProvider]) — for dialogs/bottom sheets
 * 3. Fragment screen provider ([ScreenProvider]) — wrapped via [FragmentEntry]
 *
 * Also tracks:
 * - Group membership for [PerseusNavigatorImpl.clearGroup] operations
 * - Correlation IDs for result routing
 */
internal class PerseusEntryProviderRegistry(
    private val composeProviders: List<ComposeScreenProvider<*>>,
    private val fragmentProviders: List<FragmentProviderMarker>,
    private val sceneProviders: List<ComposeSceneProvider<*>>,
    private val resultBus: ResultBusAdapter,
    private val viewModelStoreProvider: PerseusViewModelStoreProvider,
    private val fragmentEntryFactory: FragmentEntryFactory? = null
) {
    // Group tracking: back-stack id → groupName
    private val pendingGroups = ConcurrentHashMap<String, GroupName>()
    private val providedGroups = ConcurrentHashMap<String, GroupName>()

    // Correlation ID tracking: back-stack id → correlationId
    private val pendingCorrelationIds = ConcurrentHashMap<String, String>()
    private val providedCorrelationIds = ConcurrentHashMap<String, String>()

    // Per-navigate transition storage
    private val pendingTransitions = ConcurrentHashMap<String, ContentTransform>()

    /** Pop callback set by the navigator for scene dismissal. */
    var onPopCallback: (() -> Unit)? = null

    fun setPendingGroup(key: RouterKey, groupName: GroupName) {
        pendingGroups[key.backStackId()] = groupName
    }

    fun setPendingCorrelationId(key: RouterKey, correlationId: String) {
        pendingCorrelationIds[key.backStackId()] = correlationId
    }

    fun getGroupForKey(key: RouterKey): GroupName? = providedGroups[key.backStackId()]
    fun clearTrackingForKey(key: RouterKey) {
        val id = key.backStackId()
        pendingGroups.remove(id); providedGroups.remove(id)
        pendingCorrelationIds.remove(id); providedCorrelationIds.remove(id)
        pendingTransitions.remove(id)
    }

    fun setPendingTransition(key: RouterKey, transition: ContentTransform) {
        pendingTransitions[key.backStackId()] = transition
    }

    fun clearAllTracking() {
        pendingGroups.clear(); providedGroups.clear()
        pendingCorrelationIds.clear(); providedCorrelationIds.clear()
        pendingTransitions.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun provide(backStackKey: RouterKey): NavEntry<RouterKey> {
        val key = backStackKey.routeKey()
        val entryId = backStackKey.backStackId()
        val metadata = computeAndCacheMetadata(backStackKey)

        // 1. Compose screen provider
        composeProviders.find { it.canProvide(key) }?.let { foundProvider ->
            @Suppress("UNCHECKED_CAST")
            val typed = foundProvider as ComposeScreenProvider<RouterKey>
            val isScene = key is DialogKey || key is BottomSheetKey
            val navCtx = NavigationContext(
                key = key,
                entryId = entryId,
                correlationId = providedCorrelationIds[entryId] ?: newCorrelationId(),
            )
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
                    providedCorrelationIds[entryId]?.let { resultBus.send(it, result) }
                }
            }
            val dismiss: () -> Unit = { onPopCallback?.invoke() ?: Unit }
            return NavEntry(key = backStackKey, contentKey = entryId, metadata = metadata) {
                (provider as ComposeSceneProvider<RouterKey>).Content(
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
                correlationId = providedCorrelationIds[entryId] ?: newCorrelationId(),
            )
            val factory = fragmentEntryFactory
                ?: throw IllegalArgumentException(
                    "Fragment provider found for ${key::class.simpleName} but no fragmentEntryFactory set. " +
                    "Add perseus-interop dependency and pass a FragmentEntryFactory to PerseusNavigatorFactory."
                )
            return NavEntry(key = backStackKey, contentKey = entryId, metadata = metadata) {
                factory.Create(provider, key, ctx, viewModelStoreProvider)
            }
        }

        throw IllegalArgumentException(
            "No entry registered for ${key::class.simpleName}. " +
                    "Ensure a ComposeScreenProvider, ScreenProvider, or ComposeSceneProvider is registered."
        )
    }

    private fun computeAndCacheMetadata(backStackKey: RouterKey): Map<String, Any> {
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
        val groupMeta = pendingGroups.remove(id)?.let { mapOf(GROUP_KEY to it) } ?: emptyMap()
        if (groupMeta.isNotEmpty()) providedGroups[id] = groupMeta[GROUP_KEY] as GroupName
        pendingCorrelationIds.remove(id)?.let { providedCorrelationIds[id] = it }
        val transMeta = pendingTransitions.remove(id)?.let {
            mapOf(TRANSITION_KEY to it)
        } ?: emptyMap()
        return sceneMeta + groupMeta + transMeta
    }

    private fun newCorrelationId(): String = UUID.randomUUID().toString()

    private fun createSceneActions(backStackKey: RouterKey): SceneActions = object : SceneActions {
        override fun <R : Any> sendResult(result: R) {
            providedCorrelationIds[backStackKey.backStackId()]?.let { resultBus.send(it, result) }
        }

        override fun dismiss() {
            onPopCallback?.invoke()
        }
    }

    internal companion object {
        const val GROUP_KEY = "perseus_group"
        internal const val TRANSITION_KEY = "perseus_transition"
    }
}
