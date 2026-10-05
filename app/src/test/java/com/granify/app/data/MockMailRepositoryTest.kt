package com.granify.app.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockMailRepositoryTest {
    @Test
    fun markDone_marksTheMessageAsRead() = runTest {
        val repository = MockMailRepository()

        assertTrue(repository.loadMessage("statement").summary.isUnread)
        repository.markDone("statement")

        assertFalse(repository.loadMessage("statement").summary.isUnread)
        assertFalse(repository.loadInbox().messages.first { it.id == "statement" }.isUnread)
    }

    @Test
    fun moveToTrash_removesTheMessageFromTheInbox() = runTest {
        val repository = MockMailRepository()

        repository.moveToTrash("statement")

        assertTrue(repository.loadInbox().messages.none { it.id == "statement" })
    }

    @Test
    fun loadInbox_returnsTheSameMessagesTwiceAndDoesNotGrow() = runTest {
        val repository = MockMailRepository()

        val first = repository.loadInbox()
        val second = repository.loadInbox()

        assertEquals(first.messages.map { it.id }, second.messages.map { it.id })
        assertEquals(first.messages.size, second.messages.size)
        assertTrue(first.isComplete)
        assertTrue(second.isComplete)
    }
}
