package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.yigitozgumus.perseus.key.DefaultRouterKeyCodec
import com.yigitozgumus.perseus.key.EncodedRouterKey
import com.yigitozgumus.perseus.key.RouterKey
import java.util.UUID

/**
 * Navigation state that survives process death via [Saver].
 *
 * Created in composition via `rememberSaveable(saver = PerseusNavigationState.Saver)`.
 * All mutation methods operate on Compose-managed [SnapshotStateList]s.
 *
 * ## Process Death
 *
 * Keys are stored with their fully-qualified class name and serialized payload.
 * RouterKey implementations must be annotated with `@Serializable` so the
 * default key codec can restore data object and data class keys.
 */
@Stable
internal class PerseusNavigationState private constructor(
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

    fun createBackStackKey(key: RouterKey): RouterKey =
        PerseusBackStackKey(UUID.randomUUID().toString(), key)

    fun navigateTo(key: RouterKey) { currentBackStack.add(asBackStackKey(key)) }

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
        if (resetRoot) { bs.clear(); bs.add(createBackStackKey(_topLevelRoutes[tabIndex])) }
        else while (bs.size > 1) bs.removeAt(bs.lastIndex)
    }

    fun resetCurrentTab(resetRoot: Boolean = false) { resetTab(currentTabIndex, resetRoot) }

    fun startUnauthenticated(rootKey: RouterKey) {
        mode = Mode.Unauthenticated
        _unauthBackStack.clear(); _unauthBackStack.add(createBackStackKey(rootKey))
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
            mutableListOf(createBackStackKey(_topLevelRoutes[index])).toMutableStateList()
        }

    // ── Process Death ──────────────────────────────────────────────────────

    data class Snapshot(
        val modeOrdinal: Int,
        val unauthBackStack: List<EntrySnapshot>,
        val topLevelRoutes: List<RouteSnapshot>,
        val tabBackStacks: Map<Int, List<EntrySnapshot>>,
        val currentTabIndex: Int
    ) : java.io.Serializable

    data class RouteSnapshot(
        val keyClassName: String,
        val keyPayload: String,
    ) : java.io.Serializable

    data class EntrySnapshot(
        val id: String,
        val route: RouteSnapshot,
    ) : java.io.Serializable

    fun toSnapshot(): Snapshot = Snapshot(
        modeOrdinal = mode.ordinal,
        unauthBackStack = _unauthBackStack.map { entrySnapshot(it) },
        topLevelRoutes = _topLevelRoutes.map { routeSnapshot(it) },
        tabBackStacks = _tabBackStacks.mapValues { (_, v) -> v.map { entrySnapshot(it) } },
        currentTabIndex = currentTabIndex
    )

    companion object {
        fun unauthenticated(rootKey: RouterKey) = PerseusNavigationState(
            initialMode = Mode.Unauthenticated,
            initialBackStack = listOf(PerseusBackStackKey(UUID.randomUUID().toString(), rootKey)),
            initialTopLevelRoutes = emptyList(),
            initialTabBackStacks = emptyMap(),
            initialTabIndex = 0
        )

        fun fromSnapshot(snapshot: Snapshot): PerseusNavigationState {
            return PerseusNavigationState(
                initialMode = Mode.entries[snapshot.modeOrdinal],
                initialBackStack = snapshot.unauthBackStack.map { restoreEntry(it) },
                initialTopLevelRoutes = snapshot.topLevelRoutes.map { restoreRoute(it) },
                initialTabBackStacks = snapshot.tabBackStacks.mapValues { (_, v) ->
                    v.map { restoreEntry(it) }
                },
                initialTabIndex = snapshot.currentTabIndex
            )
        }

        private fun restoreEntry(snapshot: EntrySnapshot): RouterKey =
            PerseusBackStackKey(snapshot.id, restoreRoute(snapshot.route))

        private fun restoreRoute(snapshot: RouteSnapshot): RouterKey =
            DefaultRouterKeyCodec.decode(
                EncodedRouterKey(
                    className = snapshot.keyClassName,
                    payload = snapshot.keyPayload,
                )
            )

        val Saver: Saver<PerseusNavigationState, Snapshot> = Saver(
            save = { it.toSnapshot() },
            restore = { fromSnapshot(it) }
        )
    }
}

// ── Key serialization ──────────────────────────────────────────────────────

internal fun keyClassName(key: RouterKey): String = key::class.qualifiedName ?: key::class.java.name

internal data class PerseusBackStackKey(
    val id: String,
    val routeKey: RouterKey,
) : RouterKey {
    override val hidesBottomNavigation: Boolean get() = routeKey.hidesBottomNavigation
}

internal fun RouterKey.backStackId(): String =
    (this as? PerseusBackStackKey)?.id ?: keyClassName(this)

internal fun RouterKey.routeKey(): RouterKey =
    (this as? PerseusBackStackKey)?.routeKey ?: this

private fun asBackStackKey(key: RouterKey): RouterKey =
    if (key is PerseusBackStackKey) key else PerseusBackStackKey(UUID.randomUUID().toString(), key)

private fun routeSnapshot(key: RouterKey): PerseusNavigationState.RouteSnapshot {
    val encoded = DefaultRouterKeyCodec.encode(key.routeKey())
    return PerseusNavigationState.RouteSnapshot(
        keyClassName = encoded.className,
        keyPayload = encoded.payload,
    )
}

private fun entrySnapshot(key: RouterKey): PerseusNavigationState.EntrySnapshot =
    PerseusNavigationState.EntrySnapshot(
        id = key.backStackId(),
        route = routeSnapshot(key),
    )
