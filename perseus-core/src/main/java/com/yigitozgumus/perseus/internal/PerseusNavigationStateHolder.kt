package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.key.NavigationKey

/**
 * Bridge between DI-injected code and Composition-owned [PerseusNavigationState].
 *
 * The actual state is created via `rememberSaveable` in the host composable.
 * This holder stores a reference once [attach] is called.
 *
 * Calls to state transition methods before [attach] are buffered and
 * replayed when the state becomes available.
 */
internal class PerseusNavigationStateHolder {

    private sealed interface Pending {
        data class SetRootScope(val spec: StackScopeSpec) : Pending
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
                is Pending.SetRootScope -> state.setRootScope(p.spec)
            }
            pending = null
        }
    }

    fun detach() { _state = null }

    fun setRootScope(spec: StackScopeSpec): List<NavigationKey> =
        _state?.setRootScope(spec) ?: run {
            pending = Pending.SetRootScope(spec)
            emptyList()
        }

    val currentBackStack: SnapshotStateList<NavigationKey> get() = state.currentBackStack
    val currentTabIndex: Int get() = _state?.currentTabIndex ?: 0
    val topLevelRoutes: List<NavigationKey> get() = _state?.topLevelRoutes ?: emptyList()
}
