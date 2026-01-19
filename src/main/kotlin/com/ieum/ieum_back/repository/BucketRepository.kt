package com.ieum.ieum_back.repository

import com.ieum.ieum_back.entity.Bucket
import com.ieum.ieum_back.entity.Couple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BucketRepository : JpaRepository<Bucket, UUID> {
    fun findByCoupleAndDeletedAtIsNullOrderByCreatedAtDesc(couple: Couple): List<Bucket>
    fun findByIdAndCoupleAndDeletedAtIsNull(id: UUID, couple: Couple): Bucket?
}
