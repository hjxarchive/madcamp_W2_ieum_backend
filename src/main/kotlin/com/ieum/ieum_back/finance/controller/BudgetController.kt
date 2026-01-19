package com.ieum.ieum_back.finance.controller

import com.ieum.ieum_back.finance.dto.BudgetResponse
import com.ieum.ieum_back.finance.dto.SetBudgetRequest
import com.ieum.ieum_back.finance.service.FinanceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/budgets")
class BudgetController(
    private val financeService: FinanceService
) {

    @PutMapping("/{yearMonth}")
    fun setBudget(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable yearMonth: String,
        @RequestBody request: SetBudgetRequest
    ): ResponseEntity<BudgetResponse> {
        return ResponseEntity.ok(financeService.setBudget(userId, yearMonth, request))
    }

    @GetMapping("/{yearMonth}")
    fun getBudget(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable yearMonth: String
    ): ResponseEntity<BudgetResponse> {
        return ResponseEntity.ok(financeService.getBudget(userId, yearMonth))
    }
}
