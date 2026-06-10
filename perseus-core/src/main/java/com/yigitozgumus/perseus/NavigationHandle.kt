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
 * Results are reliable one-shot events. If a result is sent before observation
 * starts, the first observer for this handle still receives it.
 */
public interface NavigationHandle {
    /** Unique identifier linking this handle to a navigation session. */
    public val correlationId: String

    /**
     * Observes results from this navigation session.
     *
     * The requested type [R] must match the type sent by the child screen.
     * Results are correlated by [correlationId], but they are not dynamically
     * type-filtered at runtime.
     *
     * @param R The expected result type sent by the child screen.
     * @return A [Flow] emitting results of type [R] from this session only.
     */
    public fun <R : Any> observeResult(): Flow<R>
}
