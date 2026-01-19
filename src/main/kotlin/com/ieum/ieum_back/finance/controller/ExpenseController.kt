package com.ieum.ieum_back.finance.controller

import com.ieum.ieum_back.finance.dto.*
import com.ieum.ieum_back.finance.service.FinanceService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/expenses")
class ExpenseController(
    private val financeService: FinanceService
) {

    @PostMapping
    fun createExpense(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateExpenseRequest
    ): ResponseEntity<ExpenseResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createExpense(userId, request))
    }

    @GetMapping
    fun getExpenses(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<ExpenseListResponse> {
        return ResponseEntity.ok(financeService.getExpenses(userId, yearMonth, page, size))
    }

    @PatchMapping("/{expenseId}")
    fun updateExpense(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable expenseId: UUID,
        @RequestBody request: UpdateExpenseRequest
    ): ResponseEntity<ExpenseResponse> {
        return ResponseEntity.ok(financeService.updateExpense(userId, expenseId, request))
    }

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable expenseId: UUID
    ): ResponseEntity<Void> {
        financeService.deleteExpense(userId, expenseId)
        return ResponseEntity.noContent().build()
    }
}
