package com.ieum.ieum_back.bucket.service

import com.ieum.ieum_back.bucket.dto.*
import com.ieum.ieum_back.entity.Bucket
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.BucketRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class BucketService(
    private val bucketRepository: BucketRepository,
    private val userRepository: UserRepository
) {

    fun createBucket(userId: UUID, request: CreateBucketRequest): BucketResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val bucket = Bucket(
            couple = couple,
            createdBy = user,
            title = request.title,
            description = request.description,
            category = request.category
        )

        return BucketResponse.from(bucketRepository.save(bucket))
    }

    @Transactional(readOnly = true)
    fun getBuckets(userId: UUID): BucketListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val buckets = bucketRepository.findByCoupleAndDeletedAtIsNullOrderByCreatedAtDesc(couple)

        return BucketListResponse(
            buckets = buckets.map { BucketResponse.from(it) },
            totalCount = buckets.size,
            completedCount = buckets.count { it.isCompleted }
        )
    }

    fun updateBucket(userId: UUID, bucketId: UUID, request: UpdateBucketRequest): BucketResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val bucket = bucketRepository.findByIdAndCoupleAndDeletedAtIsNull(bucketId, couple)
            ?: throw NotFoundException("Bucket not found")

        request.title?.let { bucket.title = it }
        request.description?.let { bucket.description = it }
        request.category?.let { bucket.category = it }
        request.completedImage?.let { bucket.completedImage = it }
        request.isCompleted?.let { isCompleted ->
            if (isCompleted && !bucket.isCompleted) {
                bucket.isCompleted = true
                bucket.completedAt = LocalDateTime.now()
            } else if (!isCompleted) {
                bucket.isCompleted = false
                bucket.completedAt = null
            }
        }

        return BucketResponse.from(bucketRepository.save(bucket))
    }

    fun deleteBucket(userId: UUID, bucketId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val bucket = bucketRepository.findByIdAndCoupleAndDeletedAtIsNull(bucketId, couple)
            ?: throw NotFoundException("Bucket not found")

        bucket.deletedAt = LocalDateTime.now()
        bucketRepository.save(bucket)
    }
}
