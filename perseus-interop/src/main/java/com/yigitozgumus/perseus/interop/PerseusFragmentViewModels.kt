package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.getNavigationContext
import com.yigitozgumus.perseus.key.RouterKey

/** Returns the [NavigationContext] attached by Perseus fragment interop. */
public fun Fragment.requirePerseusNavigationContext(): NavigationContext<RouterKey> =
    requireArguments().getNavigationContext()

/**
 * Returns a lazy ViewModel scoped to the Perseus back-stack entry, not the Fragment instance.
 */
public inline fun <reified VM : ViewModel> Fragment.perseusScopedViewModel(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null,
): Lazy<VM> = lazy(LazyThreadSafetyMode.NONE) {
    val context = requirePerseusNavigationContext()
    val owner = PerseusViewModelStoreOwners.getOwner(context.entryId)
    val factory = factoryProducer?.invoke() ?: defaultViewModelProviderFactory
    ViewModelProvider(owner, factory)[VM::class.java]
}
