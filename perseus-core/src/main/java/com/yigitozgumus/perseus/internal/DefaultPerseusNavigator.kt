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

    override val currentScope: StackScopeSnapshot get() = stateHolder.state.currentScope

    override val currentTabIndex: Int get() = stateHolder.currentTabIndex

    override val currentKey: StateFlow<RouterKey?> = _currentKey

    override fun navigateTo(
        key: RouterKey,
        groupName: GroupName?,
        transition: ContentTransform?,
    ): NavigationHandle {
        if (validateProviders) entryRegistry.validateProviderForKey(key)
        logBefore("navigateTo key=${key.shortName()} group=${groupName?.name} transition=${transition != null}")
        val correlationId = UUID.randomUUID().toString()
        val backStackKey = stateHolder.state.createBackStackKey(
            key = key,
            groupName = groupName,
            correlationId = correlationId,
        )

        if (transition != null) entryRegistry.setPendingTransition(backStackKey, transition)

        stateHolder.state.navigateTo(backStackKey)
        syncCurrentKey()
        logAfter("navigateTo entryId=${backStackKey.backStackId()} correlationId=$correlationId")
        return resultBus.createHandle(correlationId)
    }

    override fun pop() {
        if (!stateHolder.isAttached) {
            logger.debug("pop ignored state=detached")
            return
        }
        logBefore("pop")
        cleanupRemoved(listOfNotNull(stateHolder.state.goBack()))
        syncCurrentKey()
        logAfter("pop")
    }

    override fun handleBack(behavior: PerseusBackBehavior): Boolean {
        if (!stateHolder.isAttached) {
            logger.debug("handleBack ignored state=detached")
            return false
        }
        logBefore("handleBack tab=${behavior.tabBackBehavior} root=${behavior.rootBackBehavior}")
        if (canGoBack()) {
            pop()
            logAfter("handleBack consumed=pop")
            return true
        }
        val state = stateHolder.state
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

    override fun canGoBack(): Boolean = stateHolder.currentBackStack.size > 1

    override fun popUntil(groupName: GroupName) {
        if (!stateHolder.isAttached) {
            logger.debug("popUntil ignored state=detached group=${groupName.name}")
            return
        }
        logBefore("popUntil group=${groupName.name}")
        val removed = stateHolder.state.removeWhere { key ->
            entryRegistry.getGroupForKey(key) == groupName
        }
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntil removed=${removed.size}")
    }

    override fun popUntilKey(key: RouterKey) {
        if (!stateHolder.isAttached) {
            logger.debug("popUntilKey ignored state=detached key=${key.shortName()}")
            return
        }
        logBefore("popUntilKey key=${key.shortName()}")
        val removed = stateHolder.state.popUntilKey(key)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntilKey removed=${removed.size}")
    }

    override fun <K : RouterKey> popUntilKeyType(keyClass: kotlin.reflect.KClass<K>) {
        if (!stateHolder.isAttached) {
            logger.debug("popUntilKeyType ignored state=detached keyClass=${keyClass.simpleName}")
            return
        }
        logBefore("popUntilKeyType keyClass=${keyClass.simpleName}")
        val removed = stateHolder.state.popUntilKeyType(keyClass)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popUntilKeyType removed=${removed.size}")
    }

    override fun <R : Any> sendResult(context: NavigationContext<*>, result: R) {
        logger.debug("sendResult correlationId=${context.correlationId} result=${result::class.simpleName}")
        resultBus.send(context.correlationId, result)
    }

    override fun switchTab(tabIndex: Int) {
        logBefore("switchTab from=${stateHolder.currentTabIndex} to=$tabIndex")
        stateHolder.state.switchTab(tabIndex)
        syncCurrentKey()
        logAfter("switchTab current=${stateHolder.currentTabIndex}")
    }

    override fun resetTab(tabIndex: Int, resetRoot: Boolean) {
        if (!stateHolder.isAttached) {
            logger.debug("resetTab ignored state=detached tab=$tabIndex resetRoot=$resetRoot")
            return
        }
        logBefore("resetTab tab=$tabIndex resetRoot=$resetRoot")
        val removed = stateHolder.state.resetTab(tabIndex, resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("resetTab removed=${removed.size}")
    }

    override fun resetCurrentTab(resetRoot: Boolean) {
        if (!stateHolder.isAttached) {
            logger.debug("resetCurrentTab ignored state=detached resetRoot=$resetRoot")
            return
        }
        logBefore("resetCurrentTab resetRoot=$resetRoot")
        val removed = stateHolder.state.resetCurrentTab(resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("resetCurrentTab removed=${removed.size}")
    }

    override fun popToRoot(resetRoot: Boolean) {
        popCurrentTabToRoot(resetRoot)
    }

    override fun popTabToRoot(tabIndex: Int, resetRoot: Boolean) {
        resetTab(tabIndex, resetRoot)
    }

    override fun popCurrentTabToRoot(resetRoot: Boolean) {
        if (!stateHolder.isAttached) {
            logger.debug("popCurrentTabToRoot ignored state=detached resetRoot=$resetRoot")
            return
        }
        logBefore("popCurrentTabToRoot resetRoot=$resetRoot")
        val removed = stateHolder.state.popCurrentStackToRoot(resetRoot)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("popCurrentTabToRoot removed=${removed.size}")
    }

    override fun resetAllWithKeys(keys: List<RouterKey>) {
        if (!stateHolder.isAttached) {
            logger.debug("resetAllWithKeys ignored state=detached keys=${keys.map { it.shortName() }}")
            return
        }
        logBefore("resetAllWithKeys keys=${keys.map { it.shortName() }}")
        val removed = stateHolder.state.resetAllWithKeys(keys)
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
        if (!stateHolder.isAttached) {
            logger.debug("replaceCurrentScope redirected=setRootScope state=detached scope=${scope.describe()}")
            setRootScope(scope)
            return
        }
        if (validateProviders) entryRegistry.validateScope(scope)
        logBefore("replaceCurrentScope scope=${scope.describe()}")
        val removed = stateHolder.state.replaceCurrentScope(scope)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("replaceCurrentScope removed=${removed.size}")
    }

    override fun pushScope(scope: StackScopeSpec): StackScopeId {
        check(stateHolder.isAttached) { "PerseusNavigationState not attached. Call pushScope after PerseusNavHost is composed." }
        if (validateProviders) entryRegistry.validateScope(scope)
        logBefore("pushScope scope=${scope.describe()}")
        val scopeId = stateHolder.state.pushScope(scope)
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
        if (!stateHolder.isAttached) {
            logger.debug("removeScope ignored state=detached scopeId=${scopeId.value}")
            return
        }
        logBefore("removeScope scopeId=${scopeId.value}")
        val removed = stateHolder.state.removeScope(scopeId)
        cleanupRemoved(removed)
        syncCurrentKey()
        logAfter("removeScope removed=${removed.size}")
    }

    override fun <R : Any> removeScope(scopeId: StackScopeId, result: R) {
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
