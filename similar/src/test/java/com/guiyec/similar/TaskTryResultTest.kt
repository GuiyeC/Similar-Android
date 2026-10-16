package com.guiyec.similar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTryResultTest {

    private fun error() = RequestError.LocalError(IllegalStateException("failure"))

    @Test
    fun firstResultEndsTheTask() {
        val completed = Task<String>()
        assertTrue(completed.tryComplete("first"))
        assertEquals(TaskState.Completed, completed.state)

        val failed = Task<String>()
        assertTrue(failed.tryFail(error()))
        assertEquals(TaskState.Failed, failed.state)
    }

    @Test
    fun laterResultsAreIgnored() {
        val failed = Task<String>()
        var failures = 0
        failed.catch { failures++ }
        failed.tryFail(error())
        assertFalse(failed.tryFail(error()))
        assertFalse(failed.tryComplete("late"))
        assertEquals(TaskState.Failed, failed.state)
        assertEquals(1, failures)

        val completed = Task<String>()
        var outputs = listOf<String>()
        completed.sink { outputs = outputs + it }
        completed.tryComplete("first")
        assertFalse(completed.tryComplete("second"))
        assertFalse(completed.tryFail(error()))
        assertEquals(TaskState.Completed, completed.state)
        assertEquals(listOf("first"), outputs)
    }

    @Test
    fun cancelledTaskTakesNoResult() {
        val cancelled = Task<String>()
        cancelled.cancel()
        assertFalse(cancelled.tryComplete("late"))
        assertFalse(cancelled.tryFail(error()))
        assertEquals(TaskState.Cancelled, cancelled.state)
    }
}
