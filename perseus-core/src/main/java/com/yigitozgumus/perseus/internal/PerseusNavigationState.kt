package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.yigitozgumus.perseus.RouterKey

/**
 * Navigation state that survives process death via [Saver].
 *
 * Created in composition via `rememberSaveable(saver = PerseusNavigationState.Saver)`.
 * All mutation methods operate on Compose-managed [SnapshotStateList]s.
 *
 * ## Process Death
 *
 * Keys are stored by their fully-qualified class name. For data objects (singletons),
 * this is sufficient to resolve the key on restore. For data classes with constructor
 * arguments, process death persistence requires a custom [KeyResolver]
 * to be provided (see [companion.resolver]).
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

    data class Snapshot(
        val modeOrdinal: Int,
        val unauthBackStack: List<String>,
        val topLevelRoutes: List<String>,
        val tabBackStacks: Map<Int, List<String>>,
        val currentTabIndex: Int
    ) : java.io.Serializable

    fun toSnapshot(): Snapshot = Snapshot(
        modeOrdinal = mode.ordinal,
        unauthBackStack = _unauthBackStack.map { keyClassName(it) },
        topLevelRoutes = _topLevelRoutes.map { keyClassName(it) },
        tabBackStacks = _tabBackStacks.mapValues { (_, v) -> v.map { keyClassName(it) } },
        currentTabIndex = currentTabIndex
    )

    companion object {
        /**
         * Resolver for restoring RouterKey instances from class names.
         * By default, resolves data objects (singletons) via Class.forName + objectInstance.
         * Register a custom resolver for data class keys that need constructor arguments.
         */
        var resolver: KeyResolver = DefaultKeyResolver

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
                initialBackStack = snapshot.unauthBackStack.mapNotNull { resolver.resolve(it) },
                initialTopLevelRoutes = snapshot.topLevelRoutes.mapNotNull { resolver.resolve(it) },
                initialTabBackStacks = snapshot.tabBackStacks.mapValues { (_, v) ->
                    v.mapNotNull { resolver.resolve(it) }
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

// ── Key serialization ──────────────────────────────────────────────────────

interface KeyResolver {
    fun resolve(className: String): RouterKey?
}

private object DefaultKeyResolver : KeyResolver {
    override fun resolve(className: String): RouterKey? {
        return try {
            val clazz = Class.forName(className)
            if (RouterKey::class.java.isAssignableFrom(clazz)) {
                clazz.getDeclaredField("INSTANCE").get(null) as? RouterKey
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

internal fun keyClassName(key: RouterKey): String = key::class.qualifiedName ?: key::class.java.name
