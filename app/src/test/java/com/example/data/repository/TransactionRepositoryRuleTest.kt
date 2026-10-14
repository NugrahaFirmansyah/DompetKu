package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TransactionRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObserveTransaction_realtimeSync_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository: TransactionRepository = FirestoreTransactionRepository(firestore, auth)

        val uniqueNote = "Catatan-${UUID.randomUUID()}"
        val tx = Transaction(
            type = TransactionType.EXPENSE.name,
            amount = 75000.0,
            category = "Makanan & Minuman",
            wallet = "Tunai",
            note = uniqueNote,
            dateMillis = System.currentTimeMillis()
        )

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(tx)
        }
        assertTrue("Add transaction should succeed", addResult.isSuccess)
        val txId = addResult.getOrThrow()

        // Realtime observe
        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == txId } }
        }
        val found = emittedList.firstOrNull { it.id == txId }
        assertNotNull(found)
        assertEquals(uniqueNote, found?.note)
        assertEquals(75000.0, found?.amount ?: 0.0, 0.01)

        // Fetch single doc
        val getResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getTransactionById(txId)
        }
        assertTrue(getResult.isSuccess)
        assertEquals(uniqueNote, getResult.getOrThrow()?.note)

        // Update transaction
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTransaction(found!!.copy(amount = 80000.0))
        }
        assertTrue(updateResult.isSuccess)

        // Delete transaction
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteTransaction(txId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_repo@dompetku.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
