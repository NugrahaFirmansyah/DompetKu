package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val thisMonthIncome: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val todayExpense: Double = 0.0,
    val categoryExpenses: Map<String, Double> = emptyMap(),
    val thisMonthCategoryExpenses: Map<String, Double> = emptyMap(),
    val categoryPercentages: Map<String, Float> = emptyMap()
)

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    // Factory helper
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceViewModel(FinanceRepository(context)) as T
        }
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow("SEMUA")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _walletFilter = MutableStateFlow("SEMUA")
    val walletFilter: StateFlow<String> = _walletFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("SEMUA")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Real-time observation of Transactions
    val transactionsState: StateFlow<UiState<List<Transaction>>> = repository.observeTransactions()
        .map<List<Transaction>, UiState<List<Transaction>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Gagal memuat transaksi")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Real-time observation of Budgets
    val budgetsState: StateFlow<UiState<List<Budget>>> = repository.observeBudgets()
        .map<List<Budget>, UiState<List<Budget>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Gagal memuat anggaran")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Summary calculation derived from transactions
    val summary: StateFlow<FinancialSummary> = transactionsState.map { state ->
        if (state is UiState.Success) {
            val list = state.data
            var income = 0.0
            var expense = 0.0
            var thisMonthInc = 0.0
            var thisMonthExp = 0.0
            var todayExp = 0.0
            val currentMonth = FormatUtils.currentMonthYear()

            val expenseByCategory = mutableMapOf<String, Double>()
            val thisMonthExpenseByCategory = mutableMapOf<String, Double>()

            for (tx in list) {
                val isCurrentMonth = FormatUtils.getMonthYearKey(tx.dateMillis) == currentMonth
                val isToday = FormatUtils.isToday(tx.dateMillis)

                if (tx.type == TransactionType.INCOME.name) {
                    income += tx.amount
                    if (isCurrentMonth) thisMonthInc += tx.amount
                } else {
                    expense += tx.amount
                    if (isCurrentMonth) {
                        thisMonthExp += tx.amount
                        thisMonthExpenseByCategory[tx.category] = (thisMonthExpenseByCategory[tx.category] ?: 0.0) + tx.amount
                    }
                    if (isToday) todayExp += tx.amount
                    expenseByCategory[tx.category] = (expenseByCategory[tx.category] ?: 0.0) + tx.amount
                }
            }

            val percentages = mutableMapOf<String, Float>()
            val baseForPercentage = if (thisMonthExp > 0) thisMonthExp else expense
            val catMapForPercentage = if (thisMonthExp > 0) thisMonthExpenseByCategory else expenseByCategory
            if (baseForPercentage > 0) {
                for ((cat, amount) in catMapForPercentage) {
                    percentages[cat] = ((amount / baseForPercentage) * 100).toFloat()
                }
            }

            FinancialSummary(
                totalIncome = income,
                totalExpense = expense,
                totalBalance = income - expense,
                thisMonthIncome = thisMonthInc,
                thisMonthExpense = thisMonthExp,
                todayExpense = todayExp,
                categoryExpenses = expenseByCategory,
                thisMonthCategoryExpenses = thisMonthExpenseByCategory,
                categoryPercentages = percentages
            )
        } else {
            FinancialSummary()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FinancialSummary()
    )

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactionsState,
        _searchQuery,
        _typeFilter,
        _walletFilter,
        _categoryFilter
    ) { state, query, type, wallet, cat ->
        if (state is UiState.Success) {
            state.data.filter { tx ->
                val matchesQuery = query.isBlank() ||
                        tx.category.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true)

                val matchesType = type == "SEMUA" || tx.type == type
                val matchesWallet = wallet == "SEMUA" || tx.wallet == wallet
                val matchesCategory = cat == "SEMUA" || tx.category.equals(cat, ignoreCase = true)

                matchesQuery && matchesType && matchesWallet && matchesCategory
            }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Dashboard transactions filtered by selected category as UiState
    val dashboardTransactionsState: StateFlow<UiState<List<Transaction>>> = combine(
        transactionsState,
        _categoryFilter
    ) { state, catFilter ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val filtered = if (catFilter == "SEMUA") {
                    state.data
                } else {
                    state.data.filter { it.category.equals(catFilter, ignoreCase = true) }
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    // Dashboard transactions filtered by selected category (convenience list)
    val dashboardTransactions: StateFlow<List<Transaction>> = combine(
        transactionsState,
        _categoryFilter
    ) { state, catFilter ->
        if (state is UiState.Success) {
            if (catFilter == "SEMUA") {
                state.data
            } else {
                state.data.filter { it.category.equals(catFilter, ignoreCase = true) }
            }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _typeFilter.value = type
    }

    fun setWalletFilter(wallet: String) {
        _walletFilter.value = wallet
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun toggleBalanceVisibility() {
        _isBalanceHidden.value = !_isBalanceHidden.value
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        wallet: String,
        note: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                type = type.name,
                amount = amount,
                category = category,
                wallet = wallet,
                note = note,
                dateMillis = dateMillis
            )
            val result = repository.addTransaction(tx)
            if (result.isSuccess) {
                _userMessage.value = "Transaksi berhasil dicatat ke cloud!"
            } else {
                _userMessage.value = "Gagal mencatat transaksi: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val result = repository.updateTransaction(transaction)
            if (result.isSuccess) {
                _userMessage.value = "Transaksi berhasil diperbarui di cloud!"
            } else {
                _userMessage.value = "Gagal memperbarui transaksi: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            if (result.isSuccess) {
                _userMessage.value = "Transaksi dihapus dari cloud."
            } else {
                _userMessage.value = "Gagal menghapus transaksi: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveBudget(category: String, monthlyLimit: Double, monthYear: String = FormatUtils.currentMonthYear()) {
        viewModelScope.launch {
            val budget = Budget(
                category = category,
                monthlyLimit = monthlyLimit,
                monthYear = monthYear
            )
            val result = repository.saveBudget(budget)
            if (result.isSuccess) {
                _userMessage.value = "Target anggaran berhasil disimpan ke cloud!"
            } else {
                _userMessage.value = "Gagal menyimpan anggaran: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(budgetId)
            if (result.isSuccess) {
                _userMessage.value = "Anggaran berhasil dihapus."
            } else {
                _userMessage.value = "Gagal menghapus anggaran: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }
}
