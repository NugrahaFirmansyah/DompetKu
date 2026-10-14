package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTransaction_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore, auth)

        val tx = Transaction(
            type = TransactionType.EXPENSE.name,
            amount = 45000.0,
            category = "Makanan",
            wallet = "Tunai",
            note = "Makan Siang",
            dateMillis = System.currentTimeMillis()
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(tx)
        }
        assertTrue("Transaction addition should succeed", result.isSuccess)
        val txId = result.getOrThrow()
        assertTrue("Transaction ID must not be empty", txId.isNotEmpty())
    }

    @Test
    fun observeTransactions_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore, auth)

        val uniqueCategory = "Kategori-${UUID.randomUUID().toString().take(6)}"
        val tx = Transaction(
            type = TransactionType.INCOME.name,
            amount = 1500000.0,
            category = uniqueCategory,
            wallet = "Rekening Bank",
            note = "Bonus",
            dateMillis = System.currentTimeMillis()
        )

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addTransaction(tx)
        }
        val txId = addResult.getOrThrow()

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == txId } }
        }
        assertNotNull(emittedList)
        assertTrue(emittedList.any { it.id == txId && it.category == uniqueCategory })
    }

    @Test
    fun saveBudget_andObserve_returnsBudget() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore, auth)

        val budget = Budget(
            category = "Belanja",
            monthlyLimit = 1000000.0,
            monthYear = "2026-10"
        )

        val budgetResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveBudget(budget)
        }
        assertTrue(budgetResult.isSuccess)
        val budgetId = budgetResult.getOrThrow()

        val emittedBudgets = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBudgets().first { list -> list.any { it.id == budgetId } }
        }
        assertEquals(1, emittedBudgets.filter { it.id == budgetId }.size)
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsImmediately() = runBlocking {
        auth.signOut()
        val repository = FinanceRepository(firestore, auth)

        var caught = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTransactions().first()
            }
        } catch (e: Exception) {
            caught = true
        }
        assertTrue("Unauthenticated call must fail", caught)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@dompetku.test"
        const val BOB_EMAIL = "bob@dompetku.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
