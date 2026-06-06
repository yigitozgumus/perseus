package com.yigitozgumus.perseus.impl

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry as Nav3Entry
import androidx.navigation3.scene.DialogSceneStrategy
import com.yigitozgumus.perseus.api.BottomSheetKey
import com.yigitozgumus.perseus.api.ComposeSceneProvider
import com.yigitozgumus.perseus.api.ComposeScreenProvider
import com.yigitozgumus.perseus.api.DialogKey
import com.yigitozgumus.perseus.api.GroupName
import com.yigitozgumus.perseus.api.LocalSceneActions
import com.yigitozgumus.perseus.api.NavigationContext
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.api.SceneActions
import com.yigitozgumus.perseus.api.SceneResultCallback
import com.yigitozgumus.perseus.api.ScreenProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry that provides [Nav3Entry] instances for both Compose screens and Fragment screens.
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
class PerseusEntryProviderRegistry(
    private val composeProviders: List<ComposeScreenProvider<*>>,
    private val fragmentProviders: List<ScreenProvider<*>>,
    private val sceneProviders: List<ComposeSceneProvider<*>>,
    private val resultBus: ResultBusAdapter
) {
    // Group tracking: key → groupName
    private val pendingGroups = ConcurrentHashMap<RouterKey, GroupName>()
    private val providedGroups = ConcurrentHashMap<RouterKey, GroupName>()

    // Correlation ID tracking: key → correlationId
    private val pendingCorrelationIds = ConcurrentHashMap<RouterKey, String>()
    private val providedCorrelationIds = ConcurrentHashMap<RouterKey, String>()

    /** Pop callback set by the navigator for scene dismissal. */
    var onPopCallback: (() -> Unit)? = null

    fun setPendingGroup(key: RouterKey, groupName: GroupName) {
        pendingGroups[key] = groupName
    }

    fun setPendingCorrelationId(key: RouterKey, correlationId: String) {
        pendingCorrelationIds[key] = correlationId
    }

    fun getGroupForKey(key: RouterKey): GroupName? = providedGroups[key]
    fun clearTrackingForKey(key: RouterKey) {
        pendingGroups.remove(key); providedGroups.remove(key)
        pendingCorrelationIds.remove(key); providedCorrelationIds.remove(key)
    }

    fun clearAllTracking() {
        pendingGroups.clear(); providedGroups.clear()
        pendingCorrelationIds.clear(); providedCorrelationIds.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun provide(key: RouterKey): Nav3Entry<RouterKey> {
        val metadata = computeAndCacheMetadata(key)

        // 1. Compose screen provider
        composeProviders.find { it.canProvide(key) }?.let { foundProvider ->
            @Suppress("UNCHECKED_CAST")
            val typed = foundProvider as ComposeScreenProvider<RouterKey>
            val isScene = key is DialogKey || key is BottomSheetKey
            return Nav3Entry(key = key, metadata = metadata) {
                if (isScene) {
                    CompositionLocalProvider(LocalSceneActions provides createSceneActions(key)) {
                        typed.Content(key)
                    }
                } else {
                    typed.Content(key)
                }
            }
        }

        // 2. Compose scene provider
        sceneProviders.find { it.canProvide(key) }?.let { provider ->
            val sceneCallback = object : SceneResultCallback {
                override fun <R : Any> sendResult(result: R) {
                    providedCorrelationIds[key]?.let { resultBus.send(it, result) }
                }
            }
            val dismiss: () -> Unit = { onPopCallback?.invoke() ?: Unit }
            return Nav3Entry(key = key, metadata = metadata) {
                (provider as ComposeSceneProvider<RouterKey>).Content(key, sceneCallback, dismiss)
            }
        }

        // 3. Fragment provider
        fragmentProviders.find { it.canProvide(key) }?.let { provider ->
            val corrId = providedCorrelationIds[key]
            val ctx = corrId?.let { NavigationContext(key, it) } ?: NavigationContext(key)
            return Nav3Entry(key = key, metadata = metadata) {
                FragmentEntry(
                    key = key,
                    provider = provider as ScreenProvider<RouterKey>,
                    context = ctx
                )
            }
        }

        throw IllegalArgumentException(
            "No entry registered for ${key::class.simpleName}. " +
                    "Ensure a ComposeScreenProvider, ScreenProvider, or ComposeSceneProvider is registered."
        )
    }

    private fun computeAndCacheMetadata(key: RouterKey): Map<String, Any> {
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
        val groupMeta = pendingGroups.remove(key)?.let { mapOf(GROUP_KEY to it) } ?: emptyMap()
        if (groupMeta.isNotEmpty()) providedGroups[key] = groupMeta[GROUP_KEY] as GroupName
        pendingCorrelationIds.remove(key)?.let { providedCorrelationIds[key] = it }
        return sceneMeta + groupMeta
    }

    private fun createSceneActions(key: RouterKey): SceneActions = object : SceneActions {
        override fun <R : Any> sendResult(result: R) {
            providedCorrelationIds[key]?.let { resultBus.send(it, result) }
        }

        override fun dismiss() {
            onPopCallback?.invoke()
        }
    }

    companion object {
        const val GROUP_KEY = "perseus_group"
    }
}
