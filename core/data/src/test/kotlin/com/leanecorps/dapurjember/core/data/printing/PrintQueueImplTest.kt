package com.leanecorps.dapurjember.core.data.printing

import com.leanecorps.dapurjember.core.domain.printing.PrintAttemptResult
import com.leanecorps.dapurjember.core.domain.printing.PrintJobState
import com.leanecorps.dapurjember.core.domain.printing.PrintJobType
import com.leanecorps.dapurjember.core.testing.FakeTimeProvider
import com.leanecorps.dapurjember.core.testing.database.RoomDatabaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintQueueImplTest : RoomDatabaseTest() {

    private val time = FakeTimeProvider(now = 1_000L)
    private val queue by lazy { PrintQueueImpl(db.printJobDao(), time) }

    @Test
    fun `enqueue adds a PENDING job that shows up in drainable and the pending count`() = runTest {
        queue.enqueue(PrintJobType.KITCHEN, byteArrayOf(1, 2, 3))

        val drainable = queue.drainable()
        assertEquals(1, drainable.size)
        assertEquals(PrintJobType.KITCHEN, drainable.single().type)
        assertEquals(PrintJobState.PENDING, drainable.single().state)
        assertEquals(1, queue.observePendingCount().first())
    }

    @Test
    fun `a successful attempt marks the job DONE and drops it from drainable`() = runTest {
        val id = queue.enqueue(PrintJobType.RECEIPT, byteArrayOf(9))

        queue.markPrinting(id)
        queue.recordResult(id, PrintAttemptResult.Success)

        assertTrue(queue.drainable().isEmpty())
        assertEquals(0, queue.observePendingCount().first())
        assertEquals(PrintJobState.DONE, queue.observeJobs().first().single().state)
    }

    @Test
    fun `a failed attempt keeps the job drainable and records the error until retries run out`() = runTest {
        val id = queue.enqueue(PrintJobType.RECEIPT, byteArrayOf(9))

        repeat(4) { queue.recordResult(id, PrintAttemptResult.Failure("printer offline")) }
        assertEquals(1, queue.drainable().size)
        assertEquals("printer offline", queue.drainable().single().lastError)

        queue.recordResult(id, PrintAttemptResult.Failure("printer offline"))
        assertTrue("5 attempts should exhaust auto-draining", queue.drainable().isEmpty())
        assertEquals(PrintJobState.FAILED, queue.observeJobs().first().single().state)
    }

    @Test
    fun `prune removes only printed jobs older than the cutoff`() = runTest {
        time.now = 100L
        val done = queue.enqueue(PrintJobType.RECEIPT, byteArrayOf(1))
        queue.recordResult(done, PrintAttemptResult.Success)
        time.now = 5_000L
        queue.enqueue(PrintJobType.KITCHEN, byteArrayOf(2))

        queue.prune(olderThanMillis = 1_000L)

        val remaining = queue.observeJobs().first()
        assertEquals(1, remaining.size)
        assertEquals(PrintJobType.KITCHEN, remaining.single().type)
    }
}
