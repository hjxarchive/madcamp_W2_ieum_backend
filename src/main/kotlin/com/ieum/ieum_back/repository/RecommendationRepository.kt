package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Couple
import com.ieum.ieum_back.entity.Recommendation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RecommendationRepository : JpaRepository<Recommendation, UUID> {
    fun findByCoupleOrderByCreatedAtDesc(couple: Couple, pageable: Pageable): Page<Recommendation>
    fun findByIdAndCouple(id: UUID, couple: Couple): Recommendation?
}
