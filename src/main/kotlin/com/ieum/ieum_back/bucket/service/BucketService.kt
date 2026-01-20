package com.ieum.ieum_back.bucket.service

import com.ieum.ieum_back.bucket.dto.*
import com.ieum.ieum_back.entity.Bucket
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.BucketRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class BucketService(
    private val bucketRepository: BucketRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
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

        val savedBucket = bucketRepository.save(bucket)
        val response = BucketResponse.from(savedBucket)
        
        // WebSocket 브로드캐스트
        broadcastBucketSync(couple.id!!, "ADDED", response, userId)
        
        return response
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

        var wasCompleted = bucket.isCompleted
        
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

        val savedBucket = bucketRepository.save(bucket)
        val response = BucketResponse.from(savedBucket)
        
        // WebSocket 브로드캐스트 - 완료 상태 변경 시 COMPLETED, 아니면 UPDATED
        val eventType = if (!wasCompleted && response.isCompleted) "COMPLETED" else "UPDATED"
        broadcastBucketSync(couple.id!!, eventType, response, userId)
        
        return response
    }

    fun deleteBucket(userId: UUID, bucketId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val bucket = bucketRepository.findByIdAndCoupleAndDeletedAtIsNull(bucketId, couple)
            ?: throw NotFoundException("Bucket not found")

        bucket.deletedAt = LocalDateTime.now()
        val savedBucket = bucketRepository.save(bucket)
        val response = BucketResponse.from(savedBucket)
        
        // WebSocket 브로드캐스트
        broadcastBucketSync(couple.id!!, "DELETED", response, userId)
    }
    
    /**
     * 버킷리스트 WebSocket 실시간 동기화 브로드캐스트
     */
    private fun broadcastBucketSync(coupleId: UUID, eventType: String, bucketResponse: BucketResponse, userId: UUID) {
        val message = BucketSyncMessage(
            eventType = eventType,
            bucket = BucketDto.from(bucketResponse),
            userId = userId.toString(),
            timestamp = LocalDateTime.now().toString()
        )
        
        messagingTemplate.convertAndSend("/topic/couple/$coupleId/bucket", message)
    }
}
