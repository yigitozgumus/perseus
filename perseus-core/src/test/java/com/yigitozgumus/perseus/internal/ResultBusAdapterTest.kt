package com.yigitozgumus.perseus.internal

import com.yigitozgumus.perseus.PerseusResult
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultBusAdapterTest {

    @Test
    fun observerReceivesResultSentBeforeObservationStarts() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "ready")

        val result = withTimeout(1_000) { handle.awaitResult(String::class) }

        assertEquals(PerseusResult.Success("ready"), result)
    }

    @Test
    fun resultsAreIsolatedByCorrelationId() = runBlocking {
        val bus = ResultBusAdapter()
        val first = bus.createHandle("correlation-a")
        val second = bus.createHandle("correlation-b")

        bus.send("correlation-b", "second")
        bus.send("correlation-a", "first")

        assertEquals(PerseusResult.Success("first"), withTimeout(1_000) { first.awaitResult(String::class) })
        assertEquals(PerseusResult.Success("second"), withTimeout(1_000) { second.awaitResult(String::class) })
    }

    @Test
    fun duplicateCompletionKeepsFirstResult() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "one")
        bus.send("correlation-a", "two")
        bus.cancel("correlation-a")

        assertEquals(PerseusResult.Success("one"), withTimeout(1_000) { handle.awaitResult(String::class) })
    }

    @Test
    fun activeObserverReceivesLaterResult() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")
        val deferred = async {
            withTimeout(1_000) { handle.awaitResult(String::class) }
        }

        bus.send("correlation-a", "later")

        assertEquals(PerseusResult.Success("later"), deferred.await())
    }

    @Test
    fun cancellationCompletesObserver() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.cancel("correlation-a")

        assertEquals(PerseusResult.Cancelled, withTimeout(1_000) { handle.awaitResult(String::class) })
    }

    @Test
    fun wrongTypeFailsWithActionableMessage() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", 42)

        val error = runCatching {
            withTimeout(1_000) { handle.awaitResult(String::class) }
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("Perseus result type mismatch") == true)
    }

    @Test
    fun resultFlowEmitsTypedCompletion() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "ready")

        assertEquals(PerseusResult.Success("ready"), withTimeout(1_000) { handle.resultFlow(String::class).first() })
    }

    @Test
    fun consumedResultStreamsAreCleanedUp() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "ready")
        withTimeout(1_000) { handle.awaitResult(String::class) }

        assertEquals(0, bus.streamCount())
    }
}
