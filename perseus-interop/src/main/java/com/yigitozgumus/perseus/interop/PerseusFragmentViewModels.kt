package com.yigitozgumus.perseus.interop

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusViewModelStoreOwners
import com.yigitozgumus.perseus.getNavigationContext
import com.yigitozgumus.perseus.key.NavigationKey

/** Returns the [NavigationContext] attached by Perseus fragment interop. */
public fun Fragment.requirePerseusNavigationContext(): NavigationContext<NavigationKey> =
    requireArguments().getNavigationContext()

/** Returns the Perseus back-stack-entry scoped [ViewModelStoreOwner] for this Fragment entry. */
public fun Fragment.requirePerseusViewModelStoreOwner(): ViewModelStoreOwner {
    val context = requirePerseusNavigationContext()
    return PerseusViewModelStoreOwners.getOwner(context.entryId)
}

/**
 * Returns a lazy ViewModel scoped to the Perseus back-stack entry, not the Fragment instance.
 *
 * If your ViewModel is created by a DI framework such as Koin, pass that framework's
 * [ViewModelProvider.Factory] through [factoryProducer]. Otherwise Android's default
 * factory is used and constructor-injected ViewModels cannot be created.
 */
public inline fun <reified VM : ViewModel> Fragment.perseusScopedViewModel(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null,
): Lazy<VM> = lazy(LazyThreadSafetyMode.NONE) {
    val owner = requirePerseusViewModelStoreOwner()
    val factory = factoryProducer?.invoke() ?: defaultViewModelProviderFactory
    ViewModelProvider(owner, factory)[VM::class.java]
}
