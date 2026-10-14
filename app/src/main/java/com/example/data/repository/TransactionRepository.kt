package com.example.data.repository

import android.content.Context
import com.example.R
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

/**
 * Interface Repository untuk mengelola operasi CRUD transaksi keuangan.
 * Mendukung pengamatan real-time melalui Kotlin Coroutines Flow.
 */
interface TransactionRepository {
    /**
     * Mengamati seluruh transaksi pengguna secara real-time.
     * Mengembalikan Flow yang otomatis memancarkan pembaruan saat data di Firestore berubah.
     */
    fun observeTransactions(): Flow<List<Transaction>>

    /**
     * Mengambil satu transaksi spesifik berdasarkan ID.
     */
    suspend fun getTransactionById(transactionId: String): Result<Transaction?>

    /**
     * Menambahkan transaksi baru ke Firestore dengan server timestamp.
     * Mengembalikan ID dokumen yang berhasil dibuat.
     */
    suspend fun addTransaction(transaction: Transaction): Result<String>

    /**
     * Memperbarui transaksi yang sudah ada.
     */
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>

    /**
     * Menghapus transaksi berdasarkan ID.
     */
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
}

/**
 * Implementasi TransactionRepository berbasis Firebase Cloud Firestore.
 * Menggunakan database ID terisolasi dan autentikasi Google.
 */
class FirestoreTransactionRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : TransactionRepository {

    /**
     * Secondary constructor untuk inisialisasi praktis dari Context / ViewModel Factory.
     * Menyelesaikan firestore_database_id dari resources.
     */
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    /**
     * Memastikan user telah terautentikasi sebelum melakukan operasi.
     * Mencegah pemanggilan query tanpa kredensial yang memicu PERMISSION_DENIED.
     */
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses Firestore.")
    }

    /**
     * Mengamati transaksi secara real-time menggunakan Cloud Firestore snapshots() Flow.
     * Diurutkan berdasarkan dateMillis menurun (transaksi terbaru di atas).
     */
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

    /**
     * Mengambil satu dokumen transaksi spesifik.
     */
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

    /**
     * Menambahkan transaksi baru ke subcollection users/{userId}/transactions/{transactionId}.
     */
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

    /**
     * Memperbarui transaksi dengan mutasi field yang diizinkan dan memperbarui updatedAt.
     */
    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "ID transaksi tidak boleh kosong untuk pembaruan" }
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

    /**
     * Menghapus dokumen transaksi dari Firestore.
     */
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
}
