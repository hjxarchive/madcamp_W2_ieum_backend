package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.Expense
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface ExpenseRepository : JpaRepository<Expense, UUID> {
    fun findByCoupleAndDeletedAtIsNullOrderByDateDesc(couple: Couple, pageable: Pageable): Page<Expense>

    @Query("SELECT e FROM Expense e WHERE e.couple = :couple AND e.deletedAt IS NULL AND e.date >= :startDate AND e.date <= :endDate ORDER BY e.date DESC")
    fun findByCoupleAndDateRange(couple: Couple, startDate: LocalDate, endDate: LocalDate): List<Expense>

    fun findByIdAndCoupleAndDeletedAtIsNull(id: UUID, couple: Couple): Expense?
}
