package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface EventRepository : JpaRepository<Event, UUID> {
    @Query("SELECT e FROM Event e WHERE e.couple = :couple AND e.deletedAt IS NULL AND e.startDate >= :startDate AND e.endDate <= :endDate ORDER BY e.startDate")
    fun findByCoupleAndDateRange(couple: Couple, startDate: LocalDateTime, endDate: LocalDateTime): List<Event>

    fun findByIdAndCoupleAndDeletedAtIsNull(id: UUID, couple: Couple): Event?

    @Query("SELECT e FROM Event e WHERE e.couple = :couple AND e.deletedAt IS NULL AND e.startDate >= :now ORDER BY e.startDate")
    fun findUpcomingEvents(couple: Couple, now: LocalDateTime): List<Event>
}
