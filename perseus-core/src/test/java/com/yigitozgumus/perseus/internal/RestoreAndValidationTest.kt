package com.yigitozgumus.perseus.internal

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.NonRestorableKey
import com.yigitozgumus.perseus.PerseusNavigatorFactory
import com.yigitozgumus.perseus.ScopeRestorePolicy
import com.yigitozgumus.perseus.SingleStackSpec
import com.yigitozgumus.perseus.SceneResultCallback
import com.yigitozgumus.perseus.key.DialogKey
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeSceneProvider
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreAndValidationTest {

    @Test
    fun neverRestorePushedScopeIsDroppedWhenSnapshotIsRestored() {
        val state = PerseusNavigationState.singleStack(ValidationHome)
        state.pushScope(
            SingleStackSpec(
                initialKey = ValidationCheckout,
                restorePolicy = ScopeRestorePolicy.NeverRestore,
            )
        )
        state.navigateTo(ValidationDetail(1))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(ValidationHome), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun nonRestorableKeyTruncatesRestoredStackBeforeThatEntry() {
        val state = PerseusNavigationState.singleStack(ValidationHome)
        state.navigateTo(ValidationPayment)
        state.navigateTo(ValidationDetail(2))

        val restored = PerseusNavigationState.fromSnapshot(state.toSnapshot())

        assertEquals(listOf(ValidationHome), restored.currentBackStack.map { it.routeKey() })
    }

    @Test
    fun validateProvidersRejectsMissingNavigationProvider() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<ValidationHome>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(ValidationHome))

        val error = runCatching { owner.navigator.navigateTo(ValidationDetail(3)) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("No provider found"))
    }

    @Test
    fun validateProvidersRejectsDuplicateProviderMatches() {
        val owner = PerseusNavigatorFactory.create(
            composeProviders = listOf(providerFor<ValidationHome>(), providerFor<ValidationHome>()),
            fragmentProviders = emptyList(),
            sceneProviders = emptyList(),
            validateProviders = true,
        )
        owner.impl.stateHolder.attach(PerseusNavigationState.singleStack(ValidationHome))

        val error = runCatching { owner.navigator.navigateTo(ValidationHome) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Multiple providers"))
    }

    @Test
    fun validateProvidersRejectsSceneProviderForNonSceneKey() {
        val registry = entryRegistry(
            sceneProviders = listOf(sceneProviderFor<ValidationHome>()),
        )

        val error = runCatching { registry.validateProviderForKey(ValidationHome) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Scene provider found for non-scene key"))
    }

    @Test
    fun validateProvidersAcceptsSceneProviderForDialogKey() {
        val registry = entryRegistry(
            sceneProviders = listOf(sceneProviderFor<ValidationDialog>()),
        )

        registry.validateProviderForKey(ValidationDialog)
    }

    @Test
    fun validateProvidersRejectsMultiStackRootsThatHideBottomNavigation() {
        val registry = entryRegistry(
            composeProviders = listOf(providerFor<ValidationHome>(), providerFor<ValidationDetail>()),
        )

        val error = runCatching {
            registry.validateScope(MultiStackSpec(listOf(ValidationHome, ValidationDetail(1))))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("Override RouterKey.hidesBottomNavigation to false for tab roots"))
    }

    @Test
    fun validateProvidersAcceptsMultiStackRootsThatShowBottomNavigation() {
        val registry = entryRegistry(
            composeProviders = listOf(providerFor<ValidationTabHome>(), providerFor<ValidationTabSearch>()),
        )

        registry.validateScope(MultiStackSpec(listOf(ValidationTabHome, ValidationTabSearch)))
    }

    @Test
    fun fragmentProviderRequiresFragmentEntryFactory() {
        val resultBus = ResultBusAdapter()
        val viewModelStoreRegistry = PerseusViewModelStoreRegistry()
        val registry = PerseusEntryProviderRegistry(
            composeProviders = emptyList(),
            fragmentProviders = listOf(object : FragmentProviderMarker {
                override fun canProvide(key: RouterKey): Boolean = key is ValidationHome
            }),
            sceneProviders = emptyList(),
            resultBus = resultBus,
            viewModelStoreProvider = viewModelStoreRegistry,
            fragmentEntryFactory = null,
        )
        val state = PerseusNavigationState.singleStack(ValidationHome)

        val error = runCatching { registry.provide(state.currentBackStack.last()) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("no fragmentEntryFactory"))
    }

    @Test
    fun scopeResultDeliversActualResult() = runBlocking {
        val navigator = navigatorFixture(ValidationHome)
        val handle = navigator.pushScopeForResult(SingleStackSpec(ValidationCheckout))

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

    private inline fun <reified K : RouterKey> sceneProviderFor(): ComposeSceneProvider<K> =
        object : ComposeSceneProvider<K> {
            override fun canProvide(key: RouterKey): Boolean = key is K

            @Composable
            override fun Content(key: K, onResult: SceneResultCallback, onDismiss: () -> Unit) = Unit
        }

    private fun entryRegistry(
        composeProviders: List<ComposeScreenProvider<*>> = emptyList(),
        fragmentProviders: List<FragmentProviderMarker> = emptyList(),
        sceneProviders: List<ComposeSceneProvider<*>> = emptyList(),
    ): PerseusEntryProviderRegistry {
        val resultBus = ResultBusAdapter()
        return PerseusEntryProviderRegistry(
            composeProviders = composeProviders,
            fragmentProviders = fragmentProviders,
            sceneProviders = sceneProviders,
            resultBus = resultBus,
            viewModelStoreProvider = PerseusViewModelStoreRegistry(),
        )
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
internal data object ValidationHome : RouterKey

@Serializable
internal data object ValidationCheckout : RouterKey

@Serializable
internal data class ValidationDetail(val id: Int) : RouterKey

@Serializable
internal data object ValidationPayment : NonRestorableKey

@Serializable
internal data object ValidationDialog : DialogKey

@Serializable
internal data object ValidationTabHome : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}

@Serializable
internal data object ValidationTabSearch : RouterKey {
    override val hidesBottomNavigation: Boolean = false
}
