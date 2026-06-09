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
import java.util.UUID

internal class DefaultPerseusNavigator(
    internal val stateHolder: PerseusNavigationStateHolder,
    private val resultBus: ResultBusAdapter,
    internal val entryRegistry: PerseusEntryProviderRegistry,
    internal val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
    internal val validateProviders: Boolean = false,
) : PerseusNavigator, PerseusScopeNavigator {

    init {
        entryRegistry.onPopCallback = { pop() }
    }

    override val currentScope: StackScopeSnapshot get() = stateHolder.state.currentScope

    override val currentTabIndex: Int get() = stateHolder.currentTabIndex

    override fun navigateTo(
        key: RouterKey,
        groupName: GroupName?,
        transition: ContentTransform?,
    ): NavigationHandle {
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = stateHolder.state.createBackStackKey(
            key = key,
            groupName = groupName,
            correlationId = correlationId,
        )

        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        stateHolder.state.navigateTo(backStackKey)
        return resultBus.createHandle(correlationId)
    }

    override fun pop() {
        if (!stateHolder.isAttached) return
        cleanupRemoved(listOfNotNull(stateHolder.state.goBack()))
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
    }

    override fun popUntilKey(key: RouterKey) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.popUntilKey(key))
    }

    override fun <K : RouterKey> popUntilKeyType(keyClass: kotlin.reflect.KClass<K>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.popUntilKeyType(keyClass))
    }

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        resultBus.send(context.correlationId, result)
    }

    override fun switchTab(tabIndex: Int) {
        stateHolder.state.switchTab(tabIndex)
    }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetTab(tabIndex, resetRoot))
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetCurrentTab(resetRoot))
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
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetAllWithKeys(keys))
        entryRegistry.clearAllTracking()
    }

    override fun setRootScope(scope: StackScopeSpec) {
        cleanupRemoved(stateHolder.setRootScope(scope))
        entryRegistry.clearAllTracking()
    }

    override fun replaceApp(scope: StackScopeSpec) {
        setRootScope(scope)
    }

    override fun replaceCurrentScope(scope: StackScopeSpec) {
        if (!stateHolder.isAttached) {
            setRootScope(scope)
            return
        }
        cleanupRemoved(stateHolder.state.replaceCurrentScope(scope))
    }

    override fun pushScope(scope: StackScopeSpec): StackScopeId {
        check(stateHolder.isAttached) { "PerseusNavigationState not attached. Call pushScope after PerseusNavHost is composed." }
        return stateHolder.state.pushScope(scope)
    }

    override fun pushScopeForResult(scope: StackScopeSpec): ScopeNavigationHandle {
        val scopeId = pushScope(scope)
        return ScopeNavigationHandleImpl(scopeId, resultBus.createHandle(scopeId.value))
    }

    override fun removeScope(scopeId: StackScopeId) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.removeScope(scopeId))
    }

    override fun <R : Any> removeScope(scopeId: StackScopeId, result: R) {
        resultBus.send(scopeId.value, result)
        removeScope(scopeId)
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
