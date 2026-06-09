package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.NonRestorableKey
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.ScopeRestorePolicy
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerseusV4RoadmapTest {

    @Test
    fun neverRestorePushedScopeIsDroppedWhenSnapshotIsRestored() {
        val state = PerseusNavigationState.singleStack(V4Home)
        state.pushScope(
            SingleStackSpec(
                initialKey = V4Checkout,
                restorePolicy = ScopeRestorePolicy.NeverRestore,
            )
        )
        state.navigateTo(V4Detail(1))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(V4Home), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun nonRestorableKeyTruncatesRestoredStackBeforeThatEntry() {
        val state = PerseusNavigationState.singleStack(V4Home)
        state.navigateTo(V4Payment)
        state.navigateTo(V4Detail(2))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(V4Home), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun validateProvidersRejectsMissingNavigationProvider() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<V4Home>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(V4Home))

        val error = runCatching { owner.navigator.navigateTo(V4Detail(3)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("No provider found"))
    }

    @Test
    fun validateProvidersRejectsDuplicateProviderMatches() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<V4Home>(), providerFor<V4Home>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(V4Home))

        val error = runCatching { owner.navigator.navigateTo(V4Home) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Multiple providers"))
    }

    @Test
    fun scopeResultDeliversActualResult() = runBlocking {
        val navigator = navigatorFixture(V4Home)
        val handle = navigator.pushScopeForResult(SingleStackSpec(V4Checkout))

        navigator.removeScope(handle.scopeId, "done")

        val result = withTimeout(1_000) { handle.observeResult<String>().first() }
        assertEquals("done", result)
    }

    private inline fun <reified K : RouterKey> providerFor(): ComposeScreenProvider<K> =
        object : ComposeScreenProvider<K> {
            override fun canProvide(key: RouterKey): Boolean = key is K

            @Composable
            override fun Content(key: K) = Unit
        }

    private fun navigatorFixture(initialKey: RouterKey): DefaultPerseusNavigator {
        val state = PerseusNavigationState.singleStack(initialKey)
        val stateHolder = PerseusNavigationStateHolder().also { it.attach(state) }
        val resultBus = ResultBusAdapter()
        val viewModelStoreRegistry = PerseusViewModelStoreRegistry()
        val entryRegistry = PerseusEntryProviderRegistry(
            composeProviders = emptyList(),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            resultBus = resultBus,
            viewModelStoreProvider = viewModelStoreRegistry,
        )
        return DefaultPerseusNavigator(
            stateHolder = stateHolder,
            resultBus = resultBus,
            entryRegistry = entryRegistry,
            viewModelStoreRegistry = viewModelStoreRegistry,
        )
    }
}

@Serializable
internal data object V4Home : RouterKey

@Serializable
internal data object V4Checkout : RouterKey

@Serializable
internal data class V4Detail(val id: Int) : RouterKey

@Serializable
internal data object V4Payment : NonRestorableKey
