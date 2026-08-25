package com.granify.app.data

import kotlinx.coroutines.test.runTest
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
        assertFalse(repository.loadInbox().first { it.id == "statement" }.isUnread)
    }

    @Test
    fun moveToTrash_removesTheMessageFromTheInbox() = runTest {
        val repository = MockMailRepository()

        repository.moveToTrash("statement")

        assertTrue(repository.loadInbox().none { it.id == "statement" })
    }
}
