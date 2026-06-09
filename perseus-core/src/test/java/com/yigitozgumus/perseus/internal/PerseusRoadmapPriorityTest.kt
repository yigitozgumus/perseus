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

class PerseusRoadmapPriorityTest {

    @Test
    fun neverRestorePushedScopeIsDroppedWhenSnapshotIsRestored() {
        val state = PerseusNavigationState.singleStack(RoadmapHome)
        state.pushScope(
            SingleStackSpec(
                initialKey = RoadmapCheckout,
                restorePolicy = ScopeRestorePolicy.NeverRestore,
            )
        )
        state.navigateTo(RoadmapDetail(1))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(RoadmapHome), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun nonRestorableKeyTruncatesRestoredStackBeforeThatEntry() {
        val state = PerseusNavigationState.singleStack(RoadmapHome)
        state.navigateTo(RoadmapPayment)
        state.navigateTo(RoadmapDetail(2))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(RoadmapHome), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun validateProvidersRejectsMissingNavigationProvider() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<RoadmapHome>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(RoadmapHome))

        val error = runCatching { owner.navigator.navigateTo(RoadmapDetail(3)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("No provider found"))
    }

    @Test
    fun validateProvidersRejectsDuplicateProviderMatches() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<RoadmapHome>(), providerFor<RoadmapHome>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(RoadmapHome))

        val error = runCatching { owner.navigator.navigateTo(RoadmapHome) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Multiple providers"))
    }

    @Test
    fun scopeResultDeliversActualResult() = runBlocking {
        val navigator = navigatorFixture(RoadmapHome)
        val handle = navigator.pushScopeForResult(SingleStackSpec(RoadmapCheckout))

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
internal data object RoadmapHome : RouterKey

@Serializable
internal data object RoadmapCheckout : RouterKey

@Serializable
internal data class RoadmapDetail(val id: Int) : RouterKey

@Serializable
internal data object RoadmapPayment : NonRestorableKey
