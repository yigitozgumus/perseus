package com.yigitozgumus.perseus.internal

import androidx.compose.animation.ContentTransform
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.StackScopeId
import com.yigitozgumus.perseus.StackScopeSnapshot
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.key.GroupName
import com.yigitozgumus.perseus.key.RouterKey
import java.util.UUID

internal class DefaultPerseusNavigator(
    internal val stateHolder: PerseusNavigationStateHolder,
    private val resultBus: ResultBusAdapter,
    internal val entryRegistry: PerseusEntryProviderRegistry,
    internal val viewModelStoreRegistry: PerseusViewModelStoreRegistry,
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

    override fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        if (!stateHolder.isAttached) return
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
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

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.resetAllWithKeys(keys))
        entryRegistry.clearAllTracking()
    }

    override fun setRootScope(scope: StackScopeSpec) {
        cleanupRemoved(stateHolder.setRootScope(scope))
        entryRegistry.clearAllTracking()
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

    override fun removeScope(scopeId: StackScopeId) {
        if (!stateHolder.isAttached) return
        cleanupRemoved(stateHolder.state.removeScope(scopeId))
    }

    private fun cleanupRemoved(removed: List<RouterKey>) {
        removed.forEach { key ->
            entryRegistry.clearTrackingForKey(key)
            viewModelStoreRegistry.clear(key.backStackId())
        }
    }
}
