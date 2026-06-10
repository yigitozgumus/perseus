package com.yigitozgumus.perseus.internal

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultBusAdapterTest {

    @Test
    fun observerReceivesResultSentBeforeObservationStarts() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "ready")

        val result = withTimeout(1_000) { handle.observeResult<String>().first() }

        assertEquals("ready", result)
    }

    @Test
    fun resultsAreIsolatedByCorrelationId() = runBlocking {
        val bus = ResultBusAdapter()
        val first = bus.createHandle("correlation-a")
        val second = bus.createHandle("correlation-b")

        bus.send("correlation-b", "second")
        bus.send("correlation-a", "first")

        assertEquals("first", withTimeout(1_000) { first.observeResult<String>().first() })
        assertEquals("second", withTimeout(1_000) { second.observeResult<String>().first() })
    }

    @Test
    fun multiplePendingResultsAreDeliveredInOrder() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "one")
        bus.send("correlation-a", "two")

        val results = withTimeout(1_000) {
            handle.observeResult<String>().take(2).toList()
        }

        assertEquals(listOf("one", "two"), results)
    }

    @Test
    fun activeObserverReceivesLaterResult() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")
        val deferred = async {
            withTimeout(1_000) { handle.observeResult<String>().first() }
        }

        bus.send("correlation-a", "later")

        assertEquals("later", deferred.await())
    }

    @Test
    fun consumedResultStreamsAreCleanedUp() = runBlocking {
        val bus = ResultBusAdapter()
        val handle = bus.createHandle("correlation-a")

        bus.send("correlation-a", "ready")
        withTimeout(1_000) { handle.observeResult<String>().first() }

        assertEquals(0, bus.streamCount())
    }

    @Test
    fun unobservedResultStreamCanBeClearedExplicitly() {
        val bus = ResultBusAdapter()

        bus.send("correlation-a", "ready")
        assertEquals(1, bus.streamCount())

        bus.clear("correlation-a")

        assertEquals(0, bus.streamCount())
    }
}
