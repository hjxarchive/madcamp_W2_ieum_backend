package com.ieum.ieum_back.bucket.dto

import com.ieum.ieum_back.entity.Bucket
import java.time.LocalDateTime
import java.util.*

data class CreateBucketRequest(
    val title: String,
    val description: String? = null,
    val category: String? = null
)

data class UpdateBucketRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val isCompleted: Boolean? = null,
    val completedImage: String? = null
)

data class BucketResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val category: String?,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?,
    val completedImage: String?,
    val createdById: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(bucket: Bucket): BucketResponse = BucketResponse(
            id = bucket.id!!,
            title = bucket.title,
            description = bucket.description,
            category = bucket.category,
            isCompleted = bucket.isCompleted,
            completedAt = bucket.completedAt,
            completedImage = bucket.completedImage,
            createdById = bucket.createdBy.id!!,
            createdAt = bucket.createdAt
        )
    }
}

data class BucketListResponse(
    val buckets: List<BucketResponse>,
    val totalCount: Int,
    val completedCount: Int
)
