package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Budget
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseBg
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeBg
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CategoryConstants
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

data class DashboardFilterCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val testTag: String
)

/**
 * Layar Dashboard Utama DompetKu:
 * 1. Menampilkan Saldo Total dengan opsi sembunyikan/tampilkan nominal.
 * 2. Menampilkan Ringkasan Pengeluaran Bulan Ini beserta pembanding pengeluaran hari ini & kategori teratas.
 * 3. Menampilkan Daftar Transaksi Terbaru yang tersinkronisasi secara real-time dari Firestore.
 * 4. Mendukung filter transaksi berdasarkan kategori (Makanan & Minuman, Transportasi, Gaji, dll).
 */
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onAddExpenseClicked: () -> Unit,
    onAddIncomeClicked: () -> Unit,
    onOpenBudgetTab: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onTransactionClicked: (Transaction) -> Unit,
    onSignOutClicked: () -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val transactionsState by viewModel.dashboardTransactionsState.collectAsStateWithLifecycle()
    val rawTransactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val budgetsState by viewModel.budgetsState.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

    val currentUser = Firebase.auth.currentUser
    val userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "Pengguna"
    val currentMonthReadable = FormatUtils.formatMonthYearReadable(FormatUtils.currentMonthYear())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. User Header & Realtime Cloud Sync Badge
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, EmeraldDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Halo, $userName",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Cloud Synced",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Firestore Real-time Aktif",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onSignOutClicked,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // 2. Kartu Saldo Total (Total Balance Hero Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_balance_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0D9488),
                                    Color(0xFF0F766E),
                                    Color(0xFF115E59)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Saldo Bersih",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color(0xFFCCFBF1),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleBalanceVisibility() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("toggle_balance_visibility")
                            ) {
                                Icon(
                                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Sembunyikan/Tampilkan Saldo",
                                    tint = Color(0xFFCCFBF1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isBalanceHidden) "Rp ••••••••" else FormatUtils.formatRupiah(summary.totalBalance),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.testTag("total_balance_text")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Baris Ringkasan Pemasukan & Pengeluaran Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.22f))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Pemasukan
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Total Masuk",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = if (isBalanceHidden) "•••••" else FormatUtils.formatRupiah(summary.totalIncome),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            // Total Pengeluaran
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Total Keluar",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFECDD3),
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = if (isBalanceHidden) "•••••" else FormatUtils.formatRupiah(summary.totalExpense),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Ringkasan Pengeluaran Bulan Ini (Monthly Expense Summary Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_expense_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ExpenseBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pengeluaran Bulan Ini",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentMonthReadable,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Nominal Pengeluaran Bulan Ini
                        Text(
                            text = if (isBalanceHidden) "Rp •••••" else FormatUtils.formatRupiah(summary.thisMonthExpense),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            ),
                            modifier = Modifier.testTag("this_month_expense_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail Pengeluaran Hari Ini & Pemasukan Bulan Ini
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pengeluaran Hari Ini:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Text(
                            text = if (isBalanceHidden) "•••••" else FormatUtils.formatRupiah(summary.todayExpense),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Top 3 Kategori Pengeluaran Bulan Ini
                    if (summary.thisMonthCategoryExpenses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pos Terbanyak Bulan Ini:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val topCategories = summary.thisMonthCategoryExpenses.toList()
                            .sortedByDescending { it.second }
                            .take(3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topCategories.forEach { (catName, amount) ->
                                val meta = CategoryConstants.getCategoryMeta(catName, true)
                                val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) meta.color.copy(alpha = 0.28f)
                                            else meta.color.copy(alpha = 0.12f)
                                        )
                                        .clickable {
                                            viewModel.setCategoryFilter(if (isSelected) "SEMUA" else catName)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("top_category_shortcut_${catName.lowercase().replace(" ", "_")}")
                                ) {
                                    Column {
                                        Text(
                                            text = catName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = meta.color,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(amount),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Tombol Aksi Cepat (Quick Actions: Catat Pengeluaran & Pemasukan)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddExpenseClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_add_expense_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseBg,
                        contentColor = ExpenseRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Catat Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onAddIncomeClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_add_income_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeBg,
                        contentColor = IncomeGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Catat Pemasukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 5. Header Transaksi Terbaru dari Firestore & Filter Kategori
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Transaksi Terbaru",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedCategory == "SEMUA") "Real-time dari Cloud Firestore"
                            else "Kategori: $selectedCategory",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (selectedCategory == "SEMUA") MaterialTheme.colorScheme.onSurfaceVariant else EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == "SEMUA") FontWeight.Normal else FontWeight.SemiBold
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedCategory != "SEMUA") {
                            TextButton(
                                onClick = { viewModel.setCategoryFilter("SEMUA") },
                                modifier = Modifier.testTag("reset_category_filter_button")
                            ) {
                                Text(
                                    text = "Reset",
                                    color = ExpenseRed,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                        TextButton(
                            onClick = onViewAllTransactions,
                            modifier = Modifier.testTag("view_all_transactions_button")
                        ) {
                            Text(
                                text = "Lihat Semua",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 5b. Baris Filter Kategori (Category Filter Chips Row)
        item {
            val baseCategories = listOf(
                DashboardFilterCategory("SEMUA", "Semua", Icons.Default.FilterList, EmeraldPrimary, "filter_chip_all"),
                DashboardFilterCategory("Makanan & Minuman", "Makanan & Minuman", Icons.Default.Restaurant, Color(0xFFF97316), "filter_chip_food"),
                DashboardFilterCategory("Transportasi", "Transportasi", Icons.Default.DirectionsCar, Color(0xFF3B82F6), "filter_chip_transport"),
                DashboardFilterCategory("Gaji", "Gaji", Icons.Default.Payments, Color(0xFF10B981), "filter_chip_salary"),
                DashboardFilterCategory("Belanja", "Belanja", Icons.Default.ShoppingBag, Color(0xFFEC4899), "filter_chip_shopping"),
                DashboardFilterCategory("Tagihan & Utilitas", "Tagihan & Utilitas", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFFEAB308), "filter_chip_bills"),
                DashboardFilterCategory("Bisnis", "Bisnis", Icons.Default.Storefront, Color(0xFF0D9488), "filter_chip_business"),
                DashboardFilterCategory("Investasi", "Investasi", Icons.Default.TrendingUp, Color(0xFF6366F1), "filter_chip_investment"),
                DashboardFilterCategory("Hiburan", "Hiburan", Icons.Default.SportsEsports, Color(0xFF8B5CF6), "filter_chip_entertainment"),
                DashboardFilterCategory("Kesehatan", "Kesehatan", Icons.Default.MedicalServices, Color(0xFFEF4444), "filter_chip_health"),
                DashboardFilterCategory("Pendidikan", "Pendidikan", Icons.Default.School, Color(0xFF06B6D4), "filter_chip_education"),
                DashboardFilterCategory("Hadiah / Bonus", "Hadiah / Bonus", Icons.Default.CardGiftcard, Color(0xFFD946EF), "filter_chip_gift"),
                DashboardFilterCategory("Lainnya", "Lainnya", Icons.Default.MoreHoriz, Color(0xFF64748B), "filter_chip_other")
            )

            val customCategories = if (rawTransactionsState is UiState.Success) {
                val knownKeys = baseCategories.map { it.key }.toSet()
                (rawTransactionsState as UiState.Success<List<Transaction>>).data
                    .map { it.category }
                    .distinct()
                    .filter { it.isNotBlank() && !knownKeys.contains(it) }
                    .map { cat ->
                        val meta = CategoryConstants.getCategoryMeta(cat, true)
                        DashboardFilterCategory(
                            key = cat,
                            label = cat,
                            icon = meta.icon,
                            color = meta.color,
                            testTag = "filter_chip_${cat.lowercase().replace(" ", "_")}"
                        )
                    }
            } else emptyList()

            val allFilterCategories = baseCategories + customCategories

            Column(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_category_filters"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allFilterCategories, key = { it.key }) { catItem ->
                        val isSelected = selectedCategory.equals(catItem.key, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) "SEMUA" else catItem.key
                                viewModel.setCategoryFilter(next)
                            },
                            label = {
                                Text(
                                    text = catItem.label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = catItem.icon,
                                    contentDescription = catItem.label,
                                    tint = if (isSelected) Color.White else catItem.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (catItem.key == "SEMUA") EmeraldPrimary else catItem.color,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = if (catItem.key == "SEMUA") EmeraldPrimary else catItem.color
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag(catItem.testTag)
                        )
                    }
                }

                // Active Filter Banner
                if (selectedCategory != "SEMUA") {
                    val activeMeta = CategoryConstants.getCategoryMeta(selectedCategory, true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeMeta.color.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_category_filter_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = activeMeta.icon,
                                    contentDescription = null,
                                    tint = activeMeta.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Menampilkan: $selectedCategory",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = activeMeta.color
                                    ),
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setCategoryFilter("SEMUA") },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("clear_category_filter_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus filter kategori",
                                    tint = activeMeta.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Daftar Transaksi Terbaru (Live Firestore List, filtered by Category)
        when (transactionsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ExpenseBg),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = (transactionsState as UiState.Error).message,
                            color = ExpenseRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val transactions = (transactionsState as UiState.Success<List<Transaction>>).data
                val allTransactions = (rawTransactionsState as? UiState.Success<List<Transaction>>)?.data ?: emptyList()

                if (allTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_transactions_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Belum Ada Transaksi Tercatat",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Mulai kelola keuangan Anda dengan mencatat pengeluaran atau pemasukan pertama.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (transactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_category_transactions_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val activeMeta = CategoryConstants.getCategoryMeta(selectedCategory, true)
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(activeMeta.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activeMeta.icon,
                                        contentDescription = null,
                                        tint = activeMeta.color,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tidak Ada Transaksi \"$selectedCategory\"",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Belum ada transaksi pengeluaran atau pemasukan yang dicatat dengan kategori ini.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.setCategoryFilter("SEMUA") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("reset_category_filter_empty_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tampilkan Semua Transaksi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val displayCount = if (selectedCategory == "SEMUA") 6 else 10
                    items(transactions.take(displayCount), key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onClick = { onTransactionClicked(tx) }
                        )
                    }
                    if (transactions.size > displayCount) {
                        item {
                            TextButton(
                                onClick = onViewAllTransactions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("see_more_filtered_transactions_button")
                            ) {
                                Text(
                                    text = "Lihat ${transactions.size - displayCount} transaksi lainnya",
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE.name
    val meta = CategoryConstants.getCategoryMeta(transaction.category, isExpense)
    val dateLabel = FormatUtils.formatDateGroup(transaction.dateMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(meta.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = transaction.category,
                    tint = meta.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Kategori, Metode Dompet & Catatan
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = transaction.wallet,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = " • ${transaction.note}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Nominal Rupiah & Label Tanggal
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isExpense) "- ${FormatUtils.formatRupiah(transaction.amount)}"
                    else "+ ${FormatUtils.formatRupiah(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpense) ExpenseRed else IncomeGreen
                    )
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
