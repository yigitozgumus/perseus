package com.yigitozgumus.perseus

import kotlinx.coroutines.flow.Flow

/**
 * Handle representing a specific navigation session, returned by [PerseusNavigator.navigateTo].
 *
 * Enables scoped result observation: even if multiple screens open the same child key type,
 * each parent only receives results from the navigation it initiated.
 *
 * Usage in ViewModel:
 * ```kotlin
 * val handle = navigator.navigateTo(DetailKey(id), groupName = null)
 *
 * handle.observeResult<DetailResult>()
 *     .onEach { result -> handleResult(result) }
 *     .launchIn(viewModelScope)
 * ```
 *
 * Note: Results are delivered via SharedFlow. Late observers won't receive
 * results emitted before subscription.
 */
public interface NavigationHandle {
    /** Unique identifier linking this handle to a specific navigation session. */
    public val correlationId: String

    /**
     * Observes results sent by the child screen for this navigation session.
     *
     * Results are filtered by correlation ID — only results from the navigation
     * this handle represents will be emitted.
     *
     * @param R The expected result type.
     * @return A Flow emitting results of type R.
     */
    public fun <R : Any> observeResult(): Flow<R>
}
