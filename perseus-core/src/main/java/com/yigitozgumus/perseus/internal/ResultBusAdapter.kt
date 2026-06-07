package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.NavigationHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Event bus for cross-screen result passing, backed by nav3's ResultEventBus semantics.
 *
 * Results are keyed by correlation ID for scoped delivery:
 * - Parent calls [createHandle] to get a [NavigationHandle] for observing results
 * - Child calls [send] to emit a result
 * - Only the handle with the matching correlation ID receives the result
 */
class ResultBusAdapter {

    private val results = MutableSharedFlow<Pair<String, Any>>(
        extraBufferCapacity = 64
    )

    /** Send a result for the given correlation ID. */
    fun <R : Any> send(correlationId: String, result: R) {
        results.tryEmit(correlationId to result)
    }

    /** Create a [NavigationHandle] that observes results for the given correlation ID. */
    fun createHandle(correlationId: String): NavigationHandle {
        return HandleImpl(correlationId, results)
    }

    @Suppress("UNCHECKED_CAST")
    private class HandleImpl(
        override val correlationId: String,
        private val results: MutableSharedFlow<Pair<String, Any>>
    ) : NavigationHandle {
        override fun <R : Any> observeResult(): Flow<R> {
            return results
                .filter { (id, _) -> id == correlationId }
                .map { (_, result) -> result as R }
        }
    }
}
