package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.internal.PerseusNavigatorImpl

/**
 * Concrete owner for a Perseus navigation graph.
 *
 * Pass this to [PerseusNavHost]. Use [navigator] from ViewModels, screens,
 * and other non-Compose code to perform navigation actions.
 */
public class PerseusController internal constructor(
    internal val impl: PerseusNavigatorImpl,
) {
    /** Public navigation API backed by this controller. */
    public val navigator: PerseusNavigator = impl
}
