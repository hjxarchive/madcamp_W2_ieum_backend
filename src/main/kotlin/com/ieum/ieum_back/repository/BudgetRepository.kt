package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Budget
import com.ieum.ieum_back.entity.Couple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByCoupleAndYearMonth(couple: Couple, yearMonth: String): Budget?
}
