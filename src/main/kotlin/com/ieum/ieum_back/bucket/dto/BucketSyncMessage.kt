package com.ieum.ieum_back.bucket.dto

import java.time.LocalDateTime

/**
 * 버킷리스트 WebSocket 실시간 동기화 메시지
 */
data class BucketSyncMessage(
    val eventType: String,  // ADDED, COMPLETED, DELETED
    val bucket: BucketDto,
    val userId: String,
    val timestamp: String
)

data class BucketDto(
    val id: String,
    val title: String,
    val category: String?,
    val isCompleted: Boolean,
    val createdAt: String,
    val completedAt: String?
) {
    companion object {
        fun from(response: BucketResponse): BucketDto {
            return BucketDto(
                id = response.id.toString(),
                title = response.title,
                category = response.category,
                isCompleted = response.isCompleted,
                createdAt = response.createdAt.toString(),
                completedAt = response.completedAt?.toString()
            )
        }
    }
}
