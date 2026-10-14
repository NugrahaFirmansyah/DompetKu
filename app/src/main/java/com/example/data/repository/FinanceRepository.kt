package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : TransactionRepository {
    // Secondary constructor that resolves named database ID
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeTransactions(): Flow<List<Transaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Transaction::class.java) }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun getTransactionById(transactionId: String): Result<Transaction?> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(snapshot.toObject(Transaction::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<String> {
        val uid = requireUserId()
        val txId = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "type" to transaction.type,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "wallet" to transaction.wallet,
            "dateMillis" to transaction.dateMillis,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.note.isNotBlank()) {
            payload["note"] = transaction.note
        }

        return try {
            docRef.set(payload).await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)

        val updates = mutableMapOf<String, Any>(
            "type" to transaction.type,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "wallet" to transaction.wallet,
            "dateMillis" to transaction.dateMillis,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.note.isNotBlank()) {
            updates["note"] = transaction.note
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeBudgets(): Flow<List<Budget>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/budgets"
        emitAll(
            db.collection("users").document(uid).collection("budgets")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Budget::class.java) }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveBudget(budget: Budget): Result<String> {
        val uid = requireUserId()
        val budgetId = if (budget.id.isNotBlank()) budget.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)

        val payload = mapOf(
            "userId" to uid,
            "category" to budget.category,
            "monthlyLimit" to budget.monthlyLimit,
            "monthYear" to budget.monthYear,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(budgetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
