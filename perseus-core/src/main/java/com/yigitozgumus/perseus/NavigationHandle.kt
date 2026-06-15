package com.yigitozgumus.perseus

import kotlin.reflect.KClass
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
     * Suspends until this navigation session completes with a typed result or cancellation.
     *
     * @throws IllegalStateException when the delivered value does not match [type].
     */
    public suspend fun <T : Any> awaitResult(type: KClass<T>): PerseusResult<T>

    /**
     * Observes this navigation session as one typed completion event.
     *
     * @throws IllegalStateException when the delivered value does not match [type].
     */
    public fun <T : Any> resultFlow(type: KClass<T>): Flow<PerseusResult<T>>

    /**
     * Observes success values from this navigation session.
     *
     * Prefer [awaitResult] or [resultFlow] because they report cancellation explicitly
     * and validate the result type at runtime.
     */
    @Deprecated(
        message = "Use awaitResult(type) or resultFlow(type) for typed success/cancellation handling.",
        replaceWith = ReplaceWith("resultFlow(R::class)"),
    )
    public fun <R : Any> observeResult(): Flow<R>
}

/** Reified convenience overload for [NavigationHandle.awaitResult]. */
public suspend inline fun <reified T : Any> NavigationHandle.awaitResult(): PerseusResult<T> =
    awaitResult(T::class)

/** Reified convenience overload for [NavigationHandle.resultFlow]. */
public inline fun <reified T : Any> NavigationHandle.resultFlow(): Flow<PerseusResult<T>> =
    resultFlow(T::class)
