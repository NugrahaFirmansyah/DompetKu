package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isExpense: Boolean
)

object CategoryConstants {
    val expenseCategories = listOf(
        CategoryMeta("Makanan & Minuman", Icons.Default.Restaurant, Color(0xFFF97316), true),
        CategoryMeta("Transportasi", Icons.Default.DirectionsCar, Color(0xFF3B82F6), true),
        CategoryMeta("Belanja", Icons.Default.ShoppingBag, Color(0xFFEC4899), true),
        CategoryMeta("Tagihan & Utilitas", Icons.Default.ReceiptLong, Color(0xFFEAB308), true),
        CategoryMeta("Hiburan", Icons.Default.SportsEsports, Color(0xFF8B5CF6), true),
        CategoryMeta("Kesehatan", Icons.Default.MedicalServices, Color(0xFFEF4444), true),
        CategoryMeta("Pendidikan", Icons.Default.School, Color(0xFF06B6D4), true),
        CategoryMeta("Lainnya", Icons.Default.MoreHoriz, Color(0xFF64748B), true)
    )

    val incomeCategories = listOf(
        CategoryMeta("Gaji", Icons.Default.Payments, Color(0xFF10B981), false),
        CategoryMeta("Bisnis", Icons.Default.Storefront, Color(0xFF0D9488), false),
        CategoryMeta("Investasi", Icons.Default.TrendingUp, Color(0xFF6366F1), false),
        CategoryMeta("Hadiah / Bonus", Icons.Default.CardGiftcard, Color(0xFFD946EF), false),
        CategoryMeta("Lainnya", Icons.Default.AccountBalance, Color(0xFF14B8A6), false)
    )

    val wallets = listOf(
        "Tunai",
        "Rekening Bank",
        "E-Wallet (GoPay/OVO/Dana)"
    )

    fun getCategoryMeta(name: String, isExpense: Boolean): CategoryMeta {
        val list = if (isExpense) expenseCategories else incomeCategories
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: CategoryMeta(name, Icons.Default.MoreHoriz, Color(0xFF64748B), isExpense)
    }
}
