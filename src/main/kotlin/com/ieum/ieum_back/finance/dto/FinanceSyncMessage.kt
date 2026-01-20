package com.ieum.ieum_back.finance.dto

import java.math.BigDecimal

/**
 * 재무/예산 WebSocket 실시간 동기화 메시지
 */
data class FinanceSyncMessage(
    val eventType: String,  // BUDGET_UPDATED, EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED
    val budget: BudgetDto?,
    val expense: ExpenseDto?,
    val userId: String,
    val timestamp: String
)

data class BudgetDto(
    val monthlyBudget: BigDecimal,
    val month: String
) {
    companion object {
        fun from(response: BudgetResponse): BudgetDto {
            return BudgetDto(
                monthlyBudget = response.totalBudget,
                month = response.yearMonth
            )
        }
    }
}

data class ExpenseDto(
    val id: String,
    val title: String,
    val category: String,
    val amount: BigDecimal,
    val date: String
) {
    companion object {
        fun from(response: ExpenseResponse): ExpenseDto {
            return ExpenseDto(
                id = response.id.toString(),
                title = response.description ?: "",
                category = response.category.name,
                amount = response.amount,
                date = response.date.toString()
            )
        }
    }
}
