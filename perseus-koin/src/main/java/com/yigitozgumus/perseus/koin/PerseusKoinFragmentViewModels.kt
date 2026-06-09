package com.yigitozgumus.perseus.koin

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.yigitozgumus.perseus.interop.requirePerseusViewModelStoreOwner
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatformTools
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel

/**
 * Returns a lazy Koin-created ViewModel scoped to the Perseus back-stack entry.
 *
 * This mirrors Koin's Fragment ViewModel delegate but uses Perseus' entry-scoped
 * ViewModelStoreOwner, so the ViewModel survives Fragment recreation while the
 * Perseus back-stack entry remains alive.
 */
@OptIn(KoinInternalApi::class)
public inline fun <reified VM : ViewModel> Fragment.perseusKoinViewModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    scope: Scope = KoinPlatformTools.defaultContext().get().scopeRegistry.rootScope,
    noinline parameters: ParametersDefinition? = null,
): Lazy<VM> = lazy(LazyThreadSafetyMode.NONE) {
    val owner = requirePerseusViewModelStoreOwner()
    resolveViewModel(
        vmClass = VM::class,
        viewModelStore = owner.viewModelStore,
        key = key,
        extras = defaultExtras(owner),
        qualifier = qualifier,
        scope = scope,
        parameters = parameters,
    )
}
