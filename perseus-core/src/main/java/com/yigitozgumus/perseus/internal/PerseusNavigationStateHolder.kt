package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.yigitozgumus.perseus.NavigationStateManager
import com.yigitozgumus.perseus.RouterKey

/**
 * Bridge between DI-injected code and Composition-owned [PerseusNavigationState].
 *
 * The actual state is created via `rememberSaveable` in the host composable.
 * This holder stores a reference once [attach] is called.
 *
 * Calls to auth transition methods before [attach] are buffered and
 * replayed when the state becomes available.
 */
class PerseusNavigationStateHolder : NavigationStateManager {

    private sealed interface Pending {
        data class StartUnauthenticated(val key: RouterKey) : Pending
        data class TransitionToAuthenticated(val keys: List<RouterKey>) : Pending
        data class ResetToUnauthenticated(val key: RouterKey) : Pending
    }

    private var _state: PerseusNavigationState? = null
    private var pending: Pending? = null

    val state: PerseusNavigationState
        get() = _state ?: error("PerseusNavigationState not attached. Call attach() from host composable.")

    val isAttached: Boolean get() = _state != null

    fun attach(state: PerseusNavigationState) {
        _state = state
        pending?.let { p ->
            when (p) {
                is Pending.StartUnauthenticated -> state.startUnauthenticated(p.key)
                is Pending.TransitionToAuthenticated -> state.transitionToAuthenticated(p.keys)
                is Pending.ResetToUnauthenticated -> state.resetToUnauthenticated(p.key)
            }
            pending = null
        }
    }

    fun detach() { _state = null }

    // ── NavigationStateManager ──────────────────────────────────────────────

    override fun startUnauthenticated(initialKey: RouterKey) {
        _state?.startUnauthenticated(initialKey) ?: run { pending = Pending.StartUnauthenticated(initialKey) }
    }

    override fun transitionToAuthenticated(tabRootKeys: List<RouterKey>) {
        _state?.transitionToAuthenticated(tabRootKeys) ?: run { pending = Pending.TransitionToAuthenticated(tabRootKeys) }
    }

    override fun resetToUnauthenticated(initialKey: RouterKey) {
        _state?.resetToUnauthenticated(initialKey) ?: run { pending = Pending.ResetToUnauthenticated(initialKey) }
    }

    override val isAuthenticated: Boolean get() = _state?.isAuthenticated ?: false

    val currentBackStack: SnapshotStateList<RouterKey> get() = state.currentBackStack
    val currentTabIndex: Int get() = _state?.currentTabIndex ?: 0
    val mode: PerseusNavigationState.Mode get() = _state?.mode ?: PerseusNavigationState.Mode.Unauthenticated
    val topLevelRoutes: List<RouterKey> get() = _state?.topLevelRoutes ?: emptyList()
}
