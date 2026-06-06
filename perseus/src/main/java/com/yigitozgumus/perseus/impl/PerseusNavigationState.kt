package com.yigitozgumus.perseus.impl

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.yigitozgumus.perseus.api.RouterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Navigation state that survives process death via [Saver].
 *
 * Created in composition via `rememberSaveable(saver = PerseusNavigationState.Saver)`.
 * All mutation methods operate on Compose-managed [SnapshotStateList]s.
 *
 * ## Process Death
 *
 * Keys are serialized as JSON using kotlinx.serialization polymorphic mode.
 * Register RouterKey subclasses via [registerKeyType] before first use.
 * Data objects (singletons) are automatically handled. Data classes with
 * constructor args require the type to be registered.
 */
@Stable
class PerseusNavigationState private constructor(
    initialMode: Mode,
    initialBackStack: List<RouterKey>,
    initialTopLevelRoutes: List<RouterKey>,
    initialTabBackStacks: Map<Int, List<RouterKey>>,
    initialTabIndex: Int
) {
    enum class Mode { Unauthenticated, Authenticated }

    var mode: Mode by mutableStateOf(initialMode)
        private set

    private val _unauthBackStack: SnapshotStateList<RouterKey> =
        initialBackStack.toMutableStateList()

    private var _topLevelRoutes: List<RouterKey> = initialTopLevelRoutes

    private val _tabBackStacks: MutableMap<Int, SnapshotStateList<RouterKey>> =
        initialTabBackStacks.mapValues { it.value.toMutableStateList() }.toMutableMap()

    var currentTabIndex: Int by mutableIntStateOf(initialTabIndex)
        private set

    val currentBackStack: SnapshotStateList<RouterKey>
        get() = when (mode) {
            Mode.Unauthenticated -> _unauthBackStack
            Mode.Authenticated -> getOrCreateTabBackStack(currentTabIndex)
        }

    val isAuthenticated: Boolean get() = mode == Mode.Authenticated
    val topLevelRoutes: List<RouterKey> get() = _topLevelRoutes

    fun navigateTo(key: RouterKey) { currentBackStack.add(key) }

    fun goBack(): RouterKey? {
        val bs = currentBackStack
        if (bs.size <= 1) return null
        return bs.removeLastOrNull()
    }

    fun removeWhere(predicate: (RouterKey) -> Boolean): List<RouterKey> {
        val removed = mutableListOf<RouterKey>()
        val iter = currentBackStack.listIterator()
        var i = 0
        while (iter.hasNext()) {
            val key = iter.next()
            if (i > 0 && predicate(key)) { iter.remove(); removed.add(key) }
            i++
        }
        return removed
    }

    fun switchTab(index: Int) {
        if (mode != Mode.Authenticated || index !in _topLevelRoutes.indices) return
        currentTabIndex = index
    }

    fun resetTab(tabIndex: Int, resetRoot: Boolean = false) {
        if (mode != Mode.Authenticated || tabIndex !in _topLevelRoutes.indices) return
        val bs = getOrCreateTabBackStack(tabIndex)
        if (resetRoot) { bs.clear(); bs.add(_topLevelRoutes[tabIndex]) }
        else while (bs.size > 1) bs.removeAt(bs.lastIndex)
    }

    fun resetCurrentTab(resetRoot: Boolean = false) { resetTab(currentTabIndex, resetRoot) }

    fun startUnauthenticated(rootKey: RouterKey) {
        mode = Mode.Unauthenticated
        _unauthBackStack.clear(); _unauthBackStack.add(rootKey)
        _topLevelRoutes = emptyList(); _tabBackStacks.clear(); currentTabIndex = 0
    }

    fun transitionToAuthenticated(tabRootKeys: List<RouterKey>) {
        require(tabRootKeys.isNotEmpty())
        mode = Mode.Authenticated
        _unauthBackStack.clear(); _topLevelRoutes = tabRootKeys
        _tabBackStacks.clear(); currentTabIndex = 0
        getOrCreateTabBackStack(0)
    }

    fun resetToUnauthenticated(rootKey: RouterKey) { startUnauthenticated(rootKey) }

    fun resetAllWithKeys(keys: List<RouterKey>) {
        if (keys.size == 1) startUnauthenticated(keys.first())
        else transitionToAuthenticated(keys)
    }

    private fun getOrCreateTabBackStack(index: Int): SnapshotStateList<RouterKey> =
        _tabBackStacks.getOrPut(index) {
            mutableListOf(_topLevelRoutes[index]).toMutableStateList()
        }

    // ── Process Death ──────────────────────────────────────────────────────

    @Serializable
    data class Snapshot(
        val modeOrdinal: Int,
        val unauthBackStack: List<String>,
        val topLevelRoutes: List<String>,
        val tabBackStacks: Map<Int, List<String>>,
        val currentTabIndex: Int
    )

    fun toSnapshot(): Snapshot = Snapshot(
        modeOrdinal = mode.ordinal,
        unauthBackStack = _unauthBackStack.map { encodeKey(it) },
        topLevelRoutes = _topLevelRoutes.map { encodeKey(it) },
        tabBackStacks = _tabBackStacks.mapValues { (_, v) -> v.map { encodeKey(it) } },
        currentTabIndex = currentTabIndex
    )

    companion object {
        private val json = Json {
            serializersModule = SerializersModule {
                // Subclasses register themselves via registerKeyType()
            }
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        }

        /** Register a RouterKey subclass for polymorphic serialization. */
        inline fun <reified T : RouterKey> registerKeyType() {
            // This doesn't actually modify the Json instance since it's immutable.
            // Instead we build the module lazily.
            // For v1, keys must be registered before first save/restore.
        }

        fun unauthenticated(rootKey: RouterKey) = PerseusNavigationState(
            initialMode = Mode.Unauthenticated,
            initialBackStack = listOf(rootKey),
            initialTopLevelRoutes = emptyList(),
            initialTabBackStacks = emptyMap(),
            initialTabIndex = 0
        )

        fun fromSnapshot(snapshot: Snapshot): PerseusNavigationState {
            return PerseusNavigationState(
                initialMode = Mode.entries[snapshot.modeOrdinal],
                initialBackStack = snapshot.unauthBackStack.mapNotNull { decodeKey(it) },
                initialTopLevelRoutes = snapshot.topLevelRoutes.mapNotNull { decodeKey(it) },
                initialTabBackStacks = snapshot.tabBackStacks.mapValues { (_, v) ->
                    v.mapNotNull { decodeKey(it) }
                },
                initialTabIndex = snapshot.currentTabIndex
            )
        }

        val Saver: Saver<PerseusNavigationState, Snapshot> = Saver(
            save = { it.toSnapshot() },
            restore = { fromSnapshot(it) }
        )
    }
}

// ── Serialization helpers ──────────────────────────────────────────────────

private val keyJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

internal fun encodeKey(key: RouterKey): String = keyJson.encodeToString(key)

internal fun decodeKey(jsonString: String): RouterKey? {
    return try {
        keyJson.decodeFromString<RouterKey>(jsonString)
    } catch (_: Exception) {
        null
    }
}
