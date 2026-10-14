package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.auth.signOutUser
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel

data class NavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FinanceViewModel,
    onSignOutComplete: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var isInputScreenVisible by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var defaultInputType by remember { mutableStateOf(TransactionType.EXPENSE) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = isInputScreenVisible || selectedTab != 0) {
        if (isInputScreenVisible) {
            isInputScreenVisible = false
            editingTransaction = null
        } else {
            viewModel.selectTab(0)
        }
    }

    if (isInputScreenVisible) {
        TransactionInputScreen(
            initialTransaction = editingTransaction,
            defaultType = defaultInputType,
            onNavigateBack = {
                isInputScreenVisible = false
                editingTransaction = null
            },
            onSaveTransaction = { type, amount, category, wallet, note, dateMillis ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        editingTransaction!!.copy(
                            type = type.name,
                            amount = amount,
                            category = category,
                            wallet = wallet,
                            note = note,
                            dateMillis = dateMillis
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        category = category,
                        wallet = wallet,
                        note = note,
                        dateMillis = dateMillis
                    )
                }
                isInputScreenVisible = false
                editingTransaction = null
            },
            onDeleteTransaction = { id ->
                viewModel.deleteTransaction(id)
                isInputScreenVisible = false
                editingTransaction = null
            }
        )
    } else {
        val tabs = listOf(
            NavTab("Ringkasan", Icons.Default.Dashboard, "tab_dashboard"),
            NavTab("Transaksi", Icons.Default.ReceiptLong, "tab_transactions"),
            NavTab("Laporan", Icons.Default.BarChart, "tab_analytics"),
            NavTab("Anggaran", Icons.Default.Savings, "tab_budget")
        )

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(index) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(text = tab.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onAddExpenseClicked = {
                            defaultInputType = TransactionType.EXPENSE
                            editingTransaction = null
                            isInputScreenVisible = true
                        },
                        onAddIncomeClicked = {
                            defaultInputType = TransactionType.INCOME
                            editingTransaction = null
                            isInputScreenVisible = true
                        },
                        onOpenBudgetTab = { viewModel.selectTab(3) },
                        onViewAllTransactions = { viewModel.selectTab(1) },
                        onTransactionClicked = { tx ->
                            editingTransaction = tx
                            isInputScreenVisible = true
                        },
                        onSignOutClicked = {
                            signOutUser(context, credentialManager, onSignOutComplete, coroutineScope)
                        }
                    )
                    1 -> TransactionsScreen(
                        viewModel = viewModel,
                        onAddClicked = {
                            defaultInputType = TransactionType.EXPENSE
                            editingTransaction = null
                            isInputScreenVisible = true
                        },
                        onTransactionClicked = { tx ->
                            editingTransaction = tx
                            isInputScreenVisible = true
                        }
                    )
                    2 -> AnalyticsScreen(
                        viewModel = viewModel
                    )
                    3 -> BudgetScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
