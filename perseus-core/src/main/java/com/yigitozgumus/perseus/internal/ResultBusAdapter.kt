package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.NavigationHandle
import com.yigitozgumus.perseus.PerseusResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Reliable one-shot result bus for cross-screen result passing.
 *
 * Results are keyed by correlation ID for scoped delivery. A correlation can
 * complete exactly once with either success or cancellation. Completion is kept
 * until an observer consumes it, so observing after a child sends a result is safe.
 */
internal class ResultBusAdapter {

    private val streams = ConcurrentHashMap<String, ResultStream>()

    /** Complete the given correlation ID with a success value. Duplicate completions are ignored. */
    fun <R : Any> send(correlationId: String, result: R) {
        streamFor(correlationId).complete(ResultCompletion.Success(result))
    }

    /** Complete the given correlation ID as cancelled. Duplicate completions are ignored. */
    fun cancel(correlationId: String) {
        streamFor(correlationId).complete(ResultCompletion.Cancelled)
    }

    /** Create a [NavigationHandle] that observes results for the given correlation ID. */
    fun createHandle(correlationId: String): NavigationHandle =
        HandleImpl(correlationId, this)

    suspend fun <T : Any> awaitResult(correlationId: String, type: KClass<T>): PerseusResult<T> {
        val stream = streamFor(correlationId)
        return try {
            stream.completion.filterNotNull().first().toPublicResult(type, correlationId)
        } finally {
            if (stream.isComplete) streams.remove(correlationId, stream)
        }
    }

    fun <T : Any> resultFlow(correlationId: String, type: KClass<T>): Flow<PerseusResult<T>> = flow {
        emit(awaitResult(correlationId, type))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Any> observeSuccess(correlationId: String): Flow<R> = flow {
        when (val result = awaitResult(correlationId, Any::class)) {
            is PerseusResult.Success -> emit(result.value as R)
            PerseusResult.Cancelled -> Unit
        }
    }

    fun streamCount(): Int = streams.size

    private fun streamFor(correlationId: String): ResultStream =
        streams.getOrPut(correlationId) { ResultStream() }

    private class HandleImpl(
        override val correlationId: String,
        private val resultBus: ResultBusAdapter,
    ) : NavigationHandle {
        override suspend fun <T : Any> awaitResult(type: KClass<T>): PerseusResult<T> =
            resultBus.awaitResult(correlationId, type)

        override fun <T : Any> resultFlow(type: KClass<T>): Flow<PerseusResult<T>> =
            resultBus.resultFlow(correlationId, type)

        @Deprecated(
            message = "Use awaitResult(type) or resultFlow(type) for typed success/cancellation handling.",
            replaceWith = ReplaceWith("resultFlow(R::class)"),
        )
        @Suppress("DEPRECATION")
        override fun <R : Any> observeResult(): Flow<R> =
            resultBus.observeSuccess(correlationId)
    }

    private class ResultStream {
        private val completed = AtomicReference<ResultCompletion?>(null)
        val completion = MutableStateFlow<ResultCompletion?>(null)
        val isComplete: Boolean get() = completed.get() != null

        fun complete(result: ResultCompletion) {
            if (completed.compareAndSet(null, result)) {
                completion.value = result
            }
        }
    }

    private sealed interface ResultCompletion {
        data class Success(val value: Any) : ResultCompletion
        data object Cancelled : ResultCompletion
    }

    private fun <T : Any> ResultCompletion.toPublicResult(
        type: KClass<T>,
        correlationId: String,
    ): PerseusResult<T> = when (this) {
        ResultCompletion.Cancelled -> PerseusResult.Cancelled
        is ResultCompletion.Success -> {
            check(type.isInstance(value)) {
                "Perseus result type mismatch for correlationId=$correlationId. " +
                    "Expected ${type.qualifiedName ?: type.simpleName}, received " +
                    (value::class.qualifiedName ?: value::class.simpleName ?: value::class.java.name) + "."
            }
            @Suppress("UNCHECKED_CAST")
            PerseusResult.Success(value as T)
        }
    }
}
