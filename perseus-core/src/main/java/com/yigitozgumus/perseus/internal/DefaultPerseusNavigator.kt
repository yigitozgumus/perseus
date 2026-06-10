package com.yigitozgumus.perseus.internal

import androidx.compose.animation.ContentTransform
import com.yigitozgumus.perseus.EmptyPerseusLogger
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusBackBehavior
import com.yigitozgumus.perseus.PerseusLogger
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusScopeNavigator
import com.yigitozgumus.perseus.RootBackBehavior
import com.yigitozgumus.perseus.ScopeNavigationHandle
import com.yigitozgumus.perseus.StackScopeId
import com.yigitozgumus.perseus.StackScopeSnapshot
import com.yigitozgumus.perseus.StackScopeSpec
import com.yigitozgumus.perseus.TabBackBehavior
import com.yigitozgumus.perseus.debug
import com.yigitozgumus.perseus.info
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
    private val logger: PerseusLogger = EmptyPerseusLogger,
) : PerseusNavigator, PerseusScopeNavigator {

    private val _currentKey = MutableStateFlow<RouterKey?>(null)

    init {
        entryRegistry.onPopCallback = { pop() }
        syncCurrentKey()
        logger.info("navigatorCreated validateProviders=$validateProviders")
    }

    override val currentScope: StackScopeSnapshot get() = stateHolder.requireState("currentScope").currentScope

    override val currentTabIndex: Int get() = stateHolder.currentTabIndex

    override val currentKey: StateFlow<RouterKey?> = _currentKey

    override fun navigateTo(
        key: RouterKey,
        groupName: GroupName?,
        transition: ContentTransform?,
    ): NavigationHandle {
        if (validateProviders) entryRegistry.validateProviderForKey(key)
        logBefore("navigateTo key=${key.shortName()} group=${groupName?.name} transition=${transition != null}")
        val state = stateHolder.requireState("navigateTo")
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = state.createBackStackKey(
            key = key,
            groupName = groupName,
            correlationId = correlationId,
        )

        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        state.navigateTo(backStackKey)
        syncCurrentKey()
        logAfter("navigateTo entryId=${backStackKey.backStackId()} correlationId=$correlationId")
        return resultBus.createHandle(correlationId)
    }

    override fun pop() {
        val state = stateHolder.requireState("pop")
        logBefore("pop")
        cleanupRemoved(listOfNotNull(state.goBack()))
        syncCurrentKey()
        logAfter("pop")
    }

    override fun handleBack(behavior: PerseusBackBehavior): Boolean {
        val state = stateHolder.requireState("handleBack")
        logBefore("handleBack tab=${behavior.tabBackBehavior} root=${behavior.rootBackBehavior}")
        if (state.currentBackStack.size > 1) {
            pop()
            logAfter("handleBack consumed=pop")
            return true
        }
        if (state.isMultiStack) {
            when (behavior.tabBackBehavior) {
                TabBackBehavior.StayOnCurrentTab -> Unit
                TabBackBehavior.SwitchToInitialTab -> if (state.currentTabIndex != 0) {
                    switchTab(0)
                    logAfter("handleBack consumed=switchToInitialTab")
                    return true
                }
                TabBackBehavior.ResetCurrentTab -> {
                    resetCurrentTab(resetRoot = true)
                    logAfter("handleBack consumed=resetCurrentTab")
                    return true
                }
            }
        }
        val consumed = behavior.rootBackBehavior == RootBackBehavior.Block
        logAfter("handleBack consumed=$consumed")
        return consumed
    }

    override fun canGoBack(): Boolean = stateHolder.requireState("canGoBack").currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        val state = stateHolder.requireState("popUntil")
        logBefore("popUntil group=${groupName.name}")
        val removed = state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntil removed=${removed.size}")
    }

    override fun popUntilKey(key: RouterKey) {
        val state = stateHolder.requireState("popUntilKey")
        logBefore("popUntilKey key=${key.shortName()}")
        val removed = state.popUntilKey(key)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntilKey removed=${removed.size}")
    }

    override fun <K : RouterKey> popUntilKeyType(keyClass: kotlin.reflect.KClass<K>) {
        val state = stateHolder.requireState("popUntilKeyType")
        logBefore("popUntilKeyType keyClass=${keyClass.simpleName}")
        val removed = state.popUntilKeyType(keyClass)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntilKeyType removed=${removed.size}")
    }

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        logger.debug("sendResult correlationId=${context.correlationId} result=${result::class.simpleName}")
        resultBus.send(context.correlationId, result)
    }

    override fun switchTab(tabIndex: Int) {
        val state = stateHolder.requireState("switchTab")
        logBefore("switchTab from=${stateHolder.currentTabIndex} to=$tabIndex")
        state.switchTab(tabIndex)
        syncCurrentKey()
        logAfter("switchTab current=${stateHolder.currentTabIndex}")
    }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        val state = stateHolder.requireState("resetTab")
        logBefore("resetTab tab=$tabIndex resetRoot=$resetRoot")
        val removed = state.resetTab(tabIndex, resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("resetTab removed=${removed.size}")
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        val state = stateHolder.requireState("resetCurrentTab")
        logBefore("resetCurrentTab resetRoot=$resetRoot")
        val removed = state.resetCurrentTab(resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("resetCurrentTab removed=${removed.size}")
    }

    override fun popToRoot(resetRoot: Boolean) {
        stateHolder.requireState("popToRoot")
        popCurrentTabToRoot(resetRoot)
    }

    override fun popTabToRoot(tabIndex: Int, resetRoot: Boolean) {
        stateHolder.requireState("popTabToRoot")
        resetTab(tabIndex, resetRoot)
    }

    override fun popCurrentTabToRoot(resetRoot: Boolean) {
        val state = stateHolder.requireState("popCurrentTabToRoot")
        logBefore("popCurrentTabToRoot resetRoot=$resetRoot")
        val removed = state.popCurrentStackToRoot(resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popCurrentTabToRoot removed=${removed.size}")
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        val state = stateHolder.requireState("resetAllWithKeys")
        logBefore("resetAllWithKeys keys=${keys.map { it.shortName() }}")
        val removed = state.resetAllWithKeys(keys)
        cleanupRemoved(removed)
        entryRegistry.clearAllTracking()
        syncCurrentKey()
        logAfter("resetAllWithKeys removed=${removed.size}")
    }

    override fun setRootScope(scope: StackScopeSpec) {
        if (validateProviders) entryRegistry.validateScope(scope)
        logBefore("setRootScope scope=${scope.describe()}")
        val removed = stateHolder.setRootScope(scope)
        cleanupRemoved(removed)
        entryRegistry.clearAllTracking()
        syncCurrentKey()
        logAfter("setRootScope removed=${removed.size}")
    }

    override fun replaceApp(scope: StackScopeSpec) {
        setRootScope(scope)
    }

    override fun replaceCurrentScope(scope: StackScopeSpec) {
        val state = stateHolder.requireState("replaceCurrentScope")
        if (validateProviders) entryRegistry.validateScope(scope)
        logBefore("replaceCurrentScope scope=${scope.describe()}")
        val removed = state.replaceCurrentScope(scope)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("replaceCurrentScope removed=${removed.size}")
    }

    override fun pushScope(scope: StackScopeSpec): StackScopeId {
        val state = stateHolder.requireState("pushScope")
        if (validateProviders) entryRegistry.validateScope(scope)
        logBefore("pushScope scope=${scope.describe()}")
        val scopeId = state.pushScope(scope)
        syncCurrentKey()
        logAfter("pushScope scopeId=${scopeId.value}")
        return scopeId
    }

    override fun pushScopeForResult(scope: StackScopeSpec): ScopeNavigationHandle {
        val scopeId = pushScope(scope)
        logger.debug("pushScopeForResult scopeId=${scopeId.value}")
        return ScopeNavigationHandleImpl(scopeId, resultBus.createHandle(scopeId.value))
    }

    override fun removeScope(scopeId: StackScopeId) {
        val state = stateHolder.requireState("removeScope")
        logBefore("removeScope scopeId=${scopeId.value}")
        val removed = state.removeScope(scopeId)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("removeScope removed=${removed.size}")
    }

    override fun <R : Any> removeScope(scopeId: StackScopeId, result: R) {
        stateHolder.requireState("removeScope")
        logger.debug("removeScopeWithResult scopeId=${scopeId.value} result=${result::class.simpleName}")
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

    private fun logBefore(operation: String) {
        logger.debug("before $operation stack=${stackDescription()}")
    }

    private fun logAfter(operation: String) {
        logger.info("after $operation currentKey=${_currentKey.value?.shortName()} stack=${stackDescription()}")
    }

    private fun stackDescription(): String =
        if (stateHolder.isAttached) stateHolder.state.debugDescription() else "<detached>"

    private fun RouterKey.shortName(): String =
        routeKey()::class.simpleName ?: keyClassName(routeKey())

    private fun StackScopeSpec.describe(): String = when (this) {
        is com.yigitozgumus.perseus.SingleStackSpec -> "SingleStack(root=${initialKey.shortName()}, id=${id?.value})"
        is com.yigitozgumus.perseus.MultiStackSpec -> "MultiStack(roots=${rootKeys.map { it.shortName() }}, initial=$initialStackIndex, id=${id?.value})"
    }

    private fun cleanupRemoved(removed: List<RouterKey>) {
        if (removed.isNotEmpty()) {
            logger.debug("cleanupRemoved entries=${removed.map { "${it.shortName()}#${it.backStackId().take(8)}" }}")
        }
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
