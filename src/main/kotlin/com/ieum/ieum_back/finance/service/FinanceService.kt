package com.ieum.ieum_back.finance.service

import com.ieum.ieum_back.entity.Budget
import com.ieum.ieum_back.entity.Expense
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.finance.dto.*
import com.ieum.ieum_back.repository.BudgetRepository
import com.ieum.ieum_back.repository.ExpenseRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

@Service
@Transactional
class FinanceService(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {

    // Expense methods
    fun createExpense(userId: UUID, request: CreateExpenseRequest): ExpenseResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val expense = Expense(
            couple = couple,
            createdBy = user,
            amount = request.amount,
            category = request.category,
            description = request.description,
            date = request.date,
            paidBy = request.paidBy
        )

        val savedExpense = expenseRepository.save(expense)
        val response = ExpenseResponse.from(savedExpense)
        
        // WebSocket 브로드캐스트
        broadcastFinanceSync(couple.id!!, "EXPENSE_ADDED", null, response, userId)
        
        return response
    }

    @Transactional(readOnly = true)
    fun getExpenses(userId: UUID, yearMonth: String?, page: Int, size: Int): ExpenseListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val expenses = if (yearMonth != null) {
            val ym = YearMonth.parse(yearMonth)
            val startDate = ym.atDay(1)
            val endDate = ym.atEndOfMonth()
            expenseRepository.findByCoupleAndDateRange(couple, startDate, endDate)
        } else {
            val pageable = PageRequest.of(page, size)
            expenseRepository.findByCoupleAndDeletedAtIsNullOrderByDateDesc(couple, pageable).content
        }

        val totalAmount = expenses.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }

        return ExpenseListResponse(
            expenses = expenses.map { ExpenseResponse.from(it) },
            totalAmount = totalAmount,
            totalCount = expenses.size.toLong(),
            page = page,
            size = size
        )
    }

    fun updateExpense(userId: UUID, expenseId: UUID, request: UpdateExpenseRequest): ExpenseResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val expense = expenseRepository.findByIdAndCoupleAndDeletedAtIsNull(expenseId, couple)
            ?: throw NotFoundException("Expense not found")

        request.amount?.let { expense.amount = it }
        request.category?.let { expense.category = it }
        request.description?.let { expense.description = it }
        request.date?.let { expense.date = it }
        request.paidBy?.let { expense.paidBy = it }

        val savedExpense = expenseRepository.save(expense)
        val response = ExpenseResponse.from(savedExpense)
        
        // WebSocket 브로드캐스트
        broadcastFinanceSync(couple.id!!, "EXPENSE_UPDATED", null, response, userId)
        
        return response
    }

    fun deleteExpense(userId: UUID, expenseId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val expense = expenseRepository.findByIdAndCoupleAndDeletedAtIsNull(expenseId, couple)
            ?: throw NotFoundException("Expense not found")

        expense.deletedAt = LocalDateTime.now()
        val savedExpense = expenseRepository.save(expense)
        val response = ExpenseResponse.from(savedExpense)
        
        // WebSocket 브로드캐스트
        broadcastFinanceSync(couple.id!!, "EXPENSE_DELETED", null, response, userId)
    }

    // Budget methods
    fun setBudget(userId: UUID, yearMonth: String, request: SetBudgetRequest): BudgetResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val budget = budgetRepository.findByCoupleAndYearMonth(couple, yearMonth)?.apply {
            this.totalBudget = request.totalBudget
            this.categoryBudgets = request.categoryBudgets
            this.updatedAt = LocalDateTime.now()
        } ?: Budget(
            couple = couple,
            yearMonth = yearMonth,
            totalBudget = request.totalBudget,
            categoryBudgets = request.categoryBudgets
        )

        val savedBudget = budgetRepository.save(budget)
        val (totalSpent, categorySpent) = calculateSpending(couple, yearMonth)
        val response = BudgetResponse.from(savedBudget, totalSpent, categorySpent)
        
        // WebSocket 브로드캐스트
        broadcastFinanceSync(couple.id!!, "BUDGET_UPDATED", response, null, userId)

        return response
    }

    @Transactional(readOnly = true)
    fun getBudget(userId: UUID, yearMonth: String): BudgetResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val budget = budgetRepository.findByCoupleAndYearMonth(couple, yearMonth)
            ?: throw NotFoundException("Budget not found for $yearMonth")

        val (totalSpent, categorySpent) = calculateSpending(couple, yearMonth)

        return BudgetResponse.from(budget, totalSpent, categorySpent)
    }

    private fun calculateSpending(couple: com.ieum.ieum_back.entity.Couple, yearMonth: String): Pair<BigDecimal, Map<String, BigDecimal>> {
        val ym = YearMonth.parse(yearMonth)
        val startDate = ym.atDay(1)
        val endDate = ym.atEndOfMonth()

        val expenses = expenseRepository.findByCoupleAndDateRange(couple, startDate, endDate)

        val totalSpent = expenses.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) }
        val categorySpent = expenses.groupBy { it.category.name }
            .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount) } }

        return Pair(totalSpent, categorySpent)
    }
    
    /**
     * 재무/예산 WebSocket 실시간 동기화 브로드캐스트
     */
    private fun broadcastFinanceSync(
        coupleId: UUID, 
        eventType: String, 
        budgetResponse: BudgetResponse?, 
        expenseResponse: ExpenseResponse?,
        userId: UUID
    ) {
        val message = FinanceSyncMessage(
            eventType = eventType,
            budget = budgetResponse?.let { BudgetDto.from(it) },
            expense = expenseResponse?.let { ExpenseDto.from(it) },
            userId = userId.toString(),
            timestamp = LocalDateTime.now().toString()
        )
        
        messagingTemplate.convertAndSend("/topic/couple/$coupleId/finance", message)
    }
}
