package com.ieum.ieum_back.finance.dto

import com.ieum.ieum_back.entity.Budget
import com.ieum.ieum_back.entity.Expense
import com.ieum.ieum_back.entity.enums.ExpenseCategory
import com.ieum.ieum_back.entity.enums.PaidByType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Expense DTOs
data class CreateExpenseRequest(
    val amount: BigDecimal,
    val category: ExpenseCategory,
    val description: String? = null,
    val date: LocalDate,
    val paidBy: PaidByType
)

data class UpdateExpenseRequest(
    val amount: BigDecimal? = null,
    val category: ExpenseCategory? = null,
    val description: String? = null,
    val date: LocalDate? = null,
    val paidBy: PaidByType? = null
)

data class ExpenseResponse(
    val id: UUID,
    val amount: BigDecimal,
    val category: ExpenseCategory,
    val description: String?,
    val date: LocalDate,
    val paidBy: PaidByType,
    val createdById: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(expense: Expense): ExpenseResponse = ExpenseResponse(
            id = expense.id!!,
            amount = expense.amount,
            category = expense.category,
            description = expense.description,
            date = expense.date,
            paidBy = expense.paidBy,
            createdById = expense.createdBy.id!!,
            createdAt = expense.createdAt
        )
    }
}

data class ExpenseListResponse(
    val expenses: List<ExpenseResponse>,
    val totalAmount: BigDecimal,
    val totalCount: Long,
    val page: Int,
    val size: Int
)

// Budget DTOs
data class SetBudgetRequest(
    val totalBudget: BigDecimal,
    val categoryBudgets: Map<String, BigDecimal>? = null
)

data class BudgetResponse(
    val id: UUID,
    val yearMonth: String,
    val totalBudget: BigDecimal,
    val categoryBudgets: Map<String, BigDecimal>?,
    val totalSpent: BigDecimal,
    val categorySpent: Map<String, BigDecimal>,
    val remainingBudget: BigDecimal
) {
    companion object {
        fun from(budget: Budget, totalSpent: BigDecimal, categorySpent: Map<String, BigDecimal>): BudgetResponse = BudgetResponse(
            id = budget.id!!,
            yearMonth = budget.yearMonth,
            totalBudget = budget.totalBudget,
            categoryBudgets = budget.categoryBudgets,
            totalSpent = totalSpent,
            categorySpent = categorySpent,
            remainingBudget = budget.totalBudget.subtract(totalSpent)
        )
    }
}
