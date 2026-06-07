package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.NavigationHandle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

/**
 * Reliable result bus for cross-screen result passing.
 *
 * Results are keyed by correlation ID for scoped delivery:
 * - Parent calls [createHandle] to get a [NavigationHandle] for observing results
 * - Child calls [send] to enqueue a result
 * - Only the handle with the matching correlation ID receives the result
 *
 * Results are stored until an observer consumes them. Sending before observation
 * is safe: the first collector for that correlation ID receives the pending result.
 */
internal class ResultBusAdapter {

    private val streams = ConcurrentHashMap<String, ResultStream>()

    /** Send a result for the given correlation ID. */
    fun <R : Any> send(correlationId: String, result: R) {
        streamFor(correlationId).send(result)
    }

    /** Create a [NavigationHandle] that observes results for the given correlation ID. */
    fun createHandle(correlationId: String): NavigationHandle =
        HandleImpl(correlationId, this)

    @Suppress("UNCHECKED_CAST")
    private fun <R : Any> observe(correlationId: String): Flow<R> = flow {
        val stream = streamFor(correlationId)
        while (currentCoroutineContext().isActive) {
            var emitted = false
            while (true) {
                val result = stream.poll() ?: break
                emitted = true
                emit(result as R)
            }
            if (!emitted) {
                val seenVersion = stream.version.value
                stream.version.first { it != seenVersion }
            }
        }
    }

    private fun streamFor(correlationId: String): ResultStream =
        streams.getOrPut(correlationId) { ResultStream() }

    private class HandleImpl(
        override val correlationId: String,
        private val resultBus: ResultBusAdapter,
    ) : NavigationHandle {
        override fun <R : Any> observeResult(): Flow<R> =
            resultBus.observe(correlationId)
    }

    private class ResultStream {
        val version = MutableStateFlow(0L)
        private val pending = ConcurrentLinkedQueue<Any>()

        fun send(result: Any) {
            pending.add(result)
            version.update { it + 1 }
        }

        fun poll(): Any? = pending.poll()
    }
}
