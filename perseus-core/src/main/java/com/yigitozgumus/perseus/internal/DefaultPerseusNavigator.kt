package com.yigitozgumus.perseus.internal

import androidx.compose.animation.ContentTransform
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.ScopeNavigationHandle
import com.yigitozgumus.perseus.StackScopeId
import com.yigitozgumus.perseus.StackScopeSnapshot
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

internal class DefaultPerseusNavigator(
    internal val stateHolder: PerseusNavigationStateHolder,
    private val resultBus: ResultBusAdapter,
    internal val entryRegistry: PerseusEntryProviderRegistry,
    internal val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
    internal val validateProviders: Boolean = false,
) : PerseusNavigator, PerseusScopeNavigator {

    private val _currentKey = MutableStateFlow<RouterKey?>(null)

    init {
        entryRegistry.onPopCallback = { pop() }
        syncCurrentKey()
    }

    override val currentScope: StackScopeSnapshot get() = stateHolder.state.currentScope

    override val currentTabIndex: Int get() = stateHolder.currentTabIndex

    override val currentKey: StateFlow<RouterKey?> = _currentKey

    override fun navigateTo(
        key: RouterKey,
        groupName: GroupName?,
        transition: ContentTransform?,
    ): NavigationHandle {
        if (validateProviders) entryRegistry.validateProviderForKey(key)
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = stateHolder.state.createBackStackKey(
            key = key,
            groupName = groupName,
            correlationId = correlationId,
        )

        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        stateHolder.state.navigateTo(backStackKey)
        syncCurrentKey()
        return resultBus.createHandle(correlationId)
    }

    override fun pop() {
        if (!stateHolder.isAttached) return
        cleanupRemoved(listOfNotNull(stateHolder.state.goBack()))
        syncCurrentKey()
    }

    override fun handleBack(behavior: PerseusBackBehavior): Boolean {
        if (!stateHolder.isAttached) return false
        if (canGoBack()) {
            pop()
            return true
        }
        val state = stateHolder.state
        if (state.isMultiStack) {
            when (behavior.tabBackBehavior) {
                TabBackBehavior.StayOnCurrentTab -> Unit
                TabBackBehavior.SwitchToInitialTab -> if (state.currentTabIndex != 0) {
                    switchTab(0)
                    return true
                }
                TabBackBehavior.ResetCurrentTab -> {
                    resetCurrentTab(resetRoot = true)
                    return true
                }
            }
        }
        return behavior.rootBackBehavior == RootBackBehavior.Block
    }

    override fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        if (!stateHolder.isAttached) return
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
        syncCurrentKey()
    }

    override fun popUntilKey(key: RouterKey) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.popUntilKey(key))
        syncCurrentKey()
    }

    override fun <K : RouterKey> popUntilKeyType(keyClass: kotlin.reflect.KClass<K>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.popUntilKeyType(keyClass))
        syncCurrentKey()
    }

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        resultBus.send(context.correlationId, result)
    }

    override fun switchTab(tabIndex: Int) {
        stateHolder.state.switchTab(tabIndex)
        syncCurrentKey()
    }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetTab(tabIndex, resetRoot))
        syncCurrentKey()
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetCurrentTab(resetRoot))
        syncCurrentKey()
    }

    override fun popToRoot(resetRoot: Boolean) {
        popCurrentTabToRoot(resetRoot)
    }

    override fun popTabToRoot(tabIndex: Int, resetRoot: Boolean) {
        resetTab(tabIndex, resetRoot)
    }

    override fun popCurrentTabToRoot(resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.popCurrentStackToRoot(resetRoot))
        syncCurrentKey()
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetAllWithKeys(keys))
        entryRegistry.clearAllTracking()
        syncCurrentKey()
    }

    override fun setRootScope(scope: StackScopeSpec) {
        if (validateProviders) entryRegistry.validateScope(scope)
        cleanupRemoved(stateHolder.setRootScope(scope))
        entryRegistry.clearAllTracking()
        syncCurrentKey()
    }

    override fun replaceApp(scope: StackScopeSpec) {
        setRootScope(scope)
    }

    override fun replaceCurrentScope(scope: StackScopeSpec) {
        if (!stateHolder.isAttached) {
            setRootScope(scope)
            return
        }
        if (validateProviders) entryRegistry.validateScope(scope)
        cleanupRemoved(stateHolder.state.replaceCurrentScope(scope))
        syncCurrentKey()
    }

    override fun pushScope(scope: StackScopeSpec): StackScopeId {
        check(stateHolder.isAttached) { "PerseusNavigationState not attached. Call pushScope after PerseusNavHost is composed." }
        if (validateProviders) entryRegistry.validateScope(scope)
        return stateHolder.state.pushScope(scope).also { syncCurrentKey() }
    }

    override fun pushScopeForResult(scope: StackScopeSpec): ScopeNavigationHandle {
        val scopeId = pushScope(scope)
        return ScopeNavigationHandleImpl(scopeId, resultBus.createHandle(scopeId.value))
    }

    override fun removeScope(scopeId: StackScopeId) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.removeScope(scopeId))
        syncCurrentKey()
    }

    override fun <R : Any> removeScope(scopeId: StackScopeId, result: R) {
        resultBus.send(scopeId.value, result)
        removeScope(scopeId)
    }

    internal fun syncCurrentKey() {
        _currentKey.value = if (stateHolder.isAttached) {
            stateHolder.state.currentBackStack.lastOrNull()?.routeKey()
        } else {
            null
        }
    }

    private fun cleanupRemoved(removed: List<RouterKey>) {
        removed.forEach { key ->
            entryRegistry.clearTrackingForKey(key)
            viewModelStoreRegistry.clear(key.backStackId())
        }
    }

    private class ScopeNavigationHandleImpl(
        override val scopeId: StackScopeId,
        private val delegate: NavigationHandle,
    ) : ScopeNavigationHandle {
        override val correlationId: String get() = delegate.correlationId
        override fun <R : Any> observeResult(): Flow<R> = delegate.observeResult()
    }
}
