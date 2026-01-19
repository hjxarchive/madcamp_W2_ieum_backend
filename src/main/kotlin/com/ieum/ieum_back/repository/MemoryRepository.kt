package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.Memory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MemoryRepository : JpaRepository<Memory, UUID> {
    fun findByCoupleAndDeletedAtIsNullOrderByDateDesc(couple: Couple, pageable: Pageable): Page<Memory>
    fun findByIdAndCoupleAndDeletedAtIsNull(id: UUID, couple: Couple): Memory?
}
