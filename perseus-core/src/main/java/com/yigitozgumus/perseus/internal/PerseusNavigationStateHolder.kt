package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.key.RouterKey

/**
 * Bridge between DI-injected code and Composition-owned [PerseusNavigationState].
 *
 * The actual state is created via `rememberSaveable` in the host composable.
 * This holder stores a reference once [attach] is called.
 *
 * Only root-scope replacement is buffered before [attach]. Route, tab,
 * and pushed-scope operations must run after the host composable attaches
 * the state, otherwise they fail with a clear lifecycle error.
 */
internal class PerseusNavigationStateHolder {

    private sealed interface Pending {
        data class SetRootScope(val spec: StackScopeSpec) : Pending
    }

    private var _state: PerseusNavigationState? = null
    private var pending: Pending? = null

    val state: PerseusNavigationState
        get() = requireState("state")

    val isAttached: Boolean get() = _state != null

    fun requireState(operationName: String): PerseusNavigationState = _state ?: error(
        "PerseusNavigator.$operationName() called before PerseusNavHost attached navigation state. " +
            "Only setRootScope()/replaceApp() are supported before host composition."
    )

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

    fun setRootScope(spec: StackScopeSpec): List<RouterKey> =
        _state?.setRootScope(spec) ?: run {
            pending = Pending.SetRootScope(spec)
            emptyList()
        }

    val currentBackStack: SnapshotStateList<RouterKey> get() = requireState("currentBackStack").currentBackStack
    val currentTabIndex: Int get() = _state?.currentTabIndex ?: 0
    val topLevelRoutes: List<RouterKey> get() = _state?.topLevelRoutes ?: emptyList()
}
