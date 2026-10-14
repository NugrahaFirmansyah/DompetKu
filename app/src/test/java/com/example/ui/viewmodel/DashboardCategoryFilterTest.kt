package com.example.ui.viewmodel

import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCategoryFilterTest {

    private val sampleTransactions = listOf(
        Transaction(
            id = "tx1",
            type = TransactionType.EXPENSE.name,
            amount = 35000.0,
            category = "Makanan & Minuman",
            wallet = "Tunai",
            note = "Makan Siang",
            dateMillis = 1700000000000L
        ),
        Transaction(
            id = "tx2",
            type = TransactionType.EXPENSE.name,
            amount = 20000.0,
            category = "Transportasi",
            wallet = "E-Wallet",
            note = "Ojek Online",
            dateMillis = 1700001000000L
        ),
        Transaction(
            id = "tx3",
            type = TransactionType.INCOME.name,
            amount = 8500000.0,
            category = "Gaji",
            wallet = "Rekening Bank",
            note = "Gaji Bulanan",
            dateMillis = 1700002000000L
        ),
        Transaction(
            id = "tx4",
            type = TransactionType.EXPENSE.name,
            amount = 50000.0,
            category = "Makanan & Minuman",
            wallet = "Tunai",
            note = "Makan Malam",
            dateMillis = 1700003000000L
        ),
        Transaction(
            id = "tx5",
            type = TransactionType.EXPENSE.name,
            amount = 150000.0,
            category = "Belanja",
            wallet = "Rekening Bank",
            note = "Belanja Bulanan",
            dateMillis = 1700004000000L
        )
    )

    private fun filterByCategory(transactions: List<Transaction>, categoryFilter: String): List<Transaction> {
        return if (categoryFilter == "SEMUA") {
            transactions
        } else {
            transactions.filter { it.category.equals(categoryFilter, ignoreCase = true) }
        }
    }

    @Test
    fun filterByCategory_all_returnsAllTransactions() {
        val result = filterByCategory(sampleTransactions, "SEMUA")
        assertEquals(5, result.size)
    }

    @Test
    fun filterByCategory_food_returnsOnlyFoodTransactions() {
        val result = filterByCategory(sampleTransactions, "Makanan & Minuman")
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "Makanan & Minuman" })
    }

    @Test
    fun filterByCategory_transport_returnsOnlyTransportTransactions() {
        val result = filterByCategory(sampleTransactions, "Transportasi")
        assertEquals(1, result.size)
        assertEquals("Transportasi", result.first().category)
        assertEquals(20000.0, result.first().amount, 0.001)
    }

    @Test
    fun filterByCategory_salary_returnsOnlySalaryIncomeTransaction() {
        val result = filterByCategory(sampleTransactions, "Gaji")
        assertEquals(1, result.size)
        assertEquals("Gaji", result.first().category)
        assertEquals(TransactionType.INCOME.name, result.first().type)
        assertEquals(8500000.0, result.first().amount, 0.001)
    }

    @Test
    fun filterByCategory_nonExistentCategory_returnsEmptyList() {
        val result = filterByCategory(sampleTransactions, "Kesehatan")
        assertTrue(result.isEmpty())
    }

    @Test
    fun filterByCategory_caseInsensitive_matchesCorrectly() {
        val result = filterByCategory(sampleTransactions, "makanan & minuman")
        assertEquals(2, result.size)
    }
}
