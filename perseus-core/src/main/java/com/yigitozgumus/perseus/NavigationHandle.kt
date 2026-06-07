package com.yigitozgumus.perseus

import kotlinx.coroutines.flow.Flow

/**
 * Handle returned by [PerseusNavigator.navigateTo] for scoped result observation.
 *
 * Each navigation session gets a unique correlation ID. Results sent by the
 * child screen are routed to the matching handle, so even if multiple parents
 * open the same child key type, each parent only receives its own results.
 *
 * Usage in a ViewModel:
 * ```kotlin
 * val handle = navigator.navigateTo(DetailKey(id))
 * handle.observeResult<DetailResult>()
 *     .onEach { result -> handleResult(result) }
 *     .launchIn(viewModelScope)
 * ```
 *
 * Results are delivered via [SharedFlow][kotlinx.coroutines.flow.SharedFlow].
 * Late observers will not receive results emitted before subscription.
 */
public interface NavigationHandle {
    /** Unique identifier linking this handle to a navigation session. */
    public val correlationId: String

    /**
     * Observes results from this navigation session, filtered to type [R].
     *
     * @param R The expected result type.
     * @return A [Flow] emitting results of type [R] from this session only.
     */
    public fun <R : Any> observeResult(): Flow<R>
}
