package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.yigitozgumus.perseus.key.DefaultRouterKeyCodec
import com.yigitozgumus.perseus.key.EncodedRouterKey
import com.yigitozgumus.perseus.key.GroupName
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
    initialScopes: List<StackScopeState>,
) {
    private val scopeStack: SnapshotStateList<StackScopeState> =
        initialScopes.toMutableStateList()

    private val currentScope: StackScopeState
        get() = scopeStack.lastOrNull() ?: error("PerseusNavigationState requires at least one scope.")

    private val currentContainer: StackContainerState
        get() = currentScope.container

    val currentBackStack: SnapshotStateList<RouterKey>
        get() = when (val container = currentContainer) {
            is SingleStackState -> container.backStack
            is MultiStackState -> container.getOrCreateStack(container.currentStackIndex)
        }

    val isMultiStack: Boolean get() = currentContainer is MultiStackState

    val isAuthenticated: Boolean get() = isMultiStack

    val topLevelRoutes: List<RouterKey>
        get() = (currentContainer as? MultiStackState)?.rootKeys ?: emptyList()

    val currentTabIndex: Int
        get() = (currentContainer as? MultiStackState)?.currentStackIndex ?: 0

    fun createBackStackKey(
        key: RouterKey,
        groupName: GroupName? = null,
        correlationId: String = UUID.randomUUID().toString(),
    ): RouterKey = PerseusBackStackKey(
        id = UUID.randomUUID().toString(),
        routeKey = key,
        groupName = groupName,
        correlationId = correlationId,
    )

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
        val container = currentContainer as? MultiStackState ?: return
        if (index !in container.rootKeys.indices) return
        container.currentStackIndex = index
    }

    fun resetTab(tabIndex: Int, resetRoot: Boolean = false): List<RouterKey> {
        val container = currentContainer as? MultiStackState ?: return emptyList()
        if (tabIndex !in container.rootKeys.indices) return emptyList()
        val bs = container.getOrCreateStack(tabIndex)
        val removed = if (resetRoot) bs.toList() else bs.drop(1)
        if (resetRoot) {
            bs.clear(); bs.add(createBackStackKey(container.rootKeys[tabIndex].routeKey()))
        } else {
            while (bs.size > 1) bs.removeAt(bs.lastIndex)
        }
        return removed
    }

    fun resetCurrentTab(resetRoot: Boolean = false): List<RouterKey> =
        resetTab(currentTabIndex, resetRoot)

    fun startUnauthenticated(rootKey: RouterKey): List<RouterKey> =
        replaceRootScope(SingleStackState(listOf(createBackStackKey(rootKey)).toMutableStateList()))

    fun transitionToAuthenticated(tabRootKeys: List<RouterKey>): List<RouterKey> {
        require(tabRootKeys.isNotEmpty())
        return replaceRootScope(
            MultiStackState(
                rootKeys = tabRootKeys,
                backStacks = mutableMapOf(
                    0 to listOf(createBackStackKey(tabRootKeys.first())).toMutableStateList()
                ),
                initialStackIndex = 0,
            )
        )
    }

    fun resetToUnauthenticated(rootKey: RouterKey): List<RouterKey> =
        startUnauthenticated(rootKey)

    fun resetAllWithKeys(keys: List<RouterKey>): List<RouterKey> =
        if (keys.size == 1) startUnauthenticated(keys.first())
        else transitionToAuthenticated(keys)

    private fun replaceRootScope(container: StackContainerState): List<RouterKey> {
        val removed = allBackStackEntries()
        scopeStack.clear()
        scopeStack.add(StackScopeState(id = UUID.randomUUID().toString(), container = container))
        return removed
    }

    private fun allBackStackEntries(): List<RouterKey> =
        scopeStack.flatMap { it.container.allBackStackEntries() }

    // ── Process Death ──────────────────────────────────────────────────────

    data class Snapshot(
        val scopes: List<ScopeSnapshot>,
    ) : java.io.Serializable

    data class ScopeSnapshot(
        val id: String,
        val container: ContainerSnapshot,
    ) : java.io.Serializable

    data class ContainerSnapshot(
        val type: Int,
        val singleBackStack: List<EntrySnapshot> = emptyList(),
        val rootRoutes: List<RouteSnapshot> = emptyList(),
        val multiBackStacks: Map<Int, List<EntrySnapshot>> = emptyMap(),
        val currentStackIndex: Int = 0,
    ) : java.io.Serializable

    data class RouteSnapshot(
        val keyClassName: String,
        val keyPayload: String,
    ) : java.io.Serializable

    data class EntrySnapshot(
        val id: String,
        val route: RouteSnapshot,
        val groupName: String?,
        val correlationId: String,
    ) : java.io.Serializable

    fun toSnapshot(): Snapshot = Snapshot(
        scopes = scopeStack.map { scope ->
            ScopeSnapshot(
                id = scope.id,
                container = containerSnapshot(scope.container),
            )
        }
    )

    companion object {
        private const val SINGLE_STACK_TYPE = 0
        private const val MULTI_STACK_TYPE = 1

        fun unauthenticated(rootKey: RouterKey) = PerseusNavigationState(
            initialScopes = listOf(
                StackScopeState(
                    id = UUID.randomUUID().toString(),
                    container = SingleStackState(
                        listOf(createInitialBackStackKey(rootKey)).toMutableStateList()
                    ),
                )
            )
        )

        fun fromSnapshot(snapshot: Snapshot): PerseusNavigationState {
            require(snapshot.scopes.isNotEmpty()) { "PerseusNavigationState snapshot requires at least one scope." }
            return PerseusNavigationState(
                initialScopes = snapshot.scopes.map { scope ->
                    StackScopeState(
                        id = scope.id,
                        container = restoreContainer(scope.container),
                    )
                }
            )
        }

        private fun restoreContainer(snapshot: ContainerSnapshot): StackContainerState =
            when (snapshot.type) {
                SINGLE_STACK_TYPE -> SingleStackState(
                    snapshot.singleBackStack.map { restoreEntry(it) }.toMutableStateList()
                )
                MULTI_STACK_TYPE -> MultiStackState(
                    rootKeys = snapshot.rootRoutes.map { restoreRoute(it) },
                    backStacks = snapshot.multiBackStacks.mapValues { (_, v) ->
                        v.map { restoreEntry(it) }.toMutableStateList()
                    }.toMutableMap(),
                    initialStackIndex = snapshot.currentStackIndex,
                )
                else -> error("Unknown stack container type: ${snapshot.type}")
            }

        private fun restoreEntry(snapshot: EntrySnapshot): RouterKey =
            PerseusBackStackKey(
                id = snapshot.id,
                routeKey = restoreRoute(snapshot.route),
                groupName = snapshot.groupName?.let { GroupName(it) },
                correlationId = snapshot.correlationId,
            )

        private fun restoreRoute(snapshot: RouteSnapshot): RouterKey =
            DefaultRouterKeyCodec.decode(
                EncodedRouterKey(
                    className = snapshot.keyClassName,
                    payload = snapshot.keyPayload,
                )
            )

        private fun createInitialBackStackKey(key: RouterKey): RouterKey =
            PerseusBackStackKey(
                id = UUID.randomUUID().toString(),
                routeKey = key,
                groupName = null,
                correlationId = UUID.randomUUID().toString(),
            )

        val Saver: Saver<PerseusNavigationState, Snapshot> = Saver(
            save = { it.toSnapshot() },
            restore = { fromSnapshot(it) }
        )
    }
}

internal data class StackScopeState(
    val id: String,
    val container: StackContainerState,
)

internal sealed interface StackContainerState {
    fun allBackStackEntries(): List<RouterKey>
}

internal data class SingleStackState(
    val backStack: SnapshotStateList<RouterKey>,
) : StackContainerState {
    override fun allBackStackEntries(): List<RouterKey> = backStack.toList()
}

internal class MultiStackState(
    val rootKeys: List<RouterKey>,
    val backStacks: MutableMap<Int, SnapshotStateList<RouterKey>>,
    initialStackIndex: Int,
) : StackContainerState {
    var currentStackIndex: Int by mutableIntStateOf(initialStackIndex)

    fun getOrCreateStack(index: Int): SnapshotStateList<RouterKey> =
        backStacks.getOrPut(index) {
            mutableListOf(createRootBackStackKey(rootKeys[index])).toMutableStateList()
        }

    override fun allBackStackEntries(): List<RouterKey> =
        backStacks.values.flatMap { it.toList() }
}

// ── Key serialization ──────────────────────────────────────────────────────

internal fun keyClassName(key: RouterKey): String = key::class.qualifiedName ?: key::class.java.name

internal data class PerseusBackStackKey(
    val id: String,
    val routeKey: RouterKey,
    val groupName: GroupName?,
    val correlationId: String,
) : RouterKey {
    override val hidesBottomNavigation: Boolean get() = routeKey.hidesBottomNavigation
}

internal fun RouterKey.backStackId(): String =
    (this as? PerseusBackStackKey)?.id ?: keyClassName(this)

internal fun RouterKey.routeKey(): RouterKey =
    (this as? PerseusBackStackKey)?.routeKey ?: this

internal fun RouterKey.groupName(): GroupName? =
    (this as? PerseusBackStackKey)?.groupName

internal fun RouterKey.correlationId(): String? =
    (this as? PerseusBackStackKey)?.correlationId

private fun asBackStackKey(key: RouterKey): RouterKey =
    if (key is PerseusBackStackKey) key else createRootBackStackKey(key)

private fun createRootBackStackKey(key: RouterKey): RouterKey = PerseusBackStackKey(
    id = UUID.randomUUID().toString(),
    routeKey = key,
    groupName = null,
    correlationId = UUID.randomUUID().toString(),
)

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
        groupName = key.groupName()?.name,
        correlationId = key.correlationId() ?: UUID.randomUUID().toString(),
    )

private fun containerSnapshot(container: StackContainerState): PerseusNavigationState.ContainerSnapshot =
    when (container) {
        is SingleStackState -> PerseusNavigationState.ContainerSnapshot(
            type = 0,
            singleBackStack = container.backStack.map { entrySnapshot(it) },
        )
        is MultiStackState -> PerseusNavigationState.ContainerSnapshot(
            type = 1,
            rootRoutes = container.rootKeys.map { routeSnapshot(it) },
            multiBackStacks = container.backStacks.mapValues { (_, v) ->
                v.map { entrySnapshot(it) }
            },
            currentStackIndex = container.currentStackIndex,
        )
    }
