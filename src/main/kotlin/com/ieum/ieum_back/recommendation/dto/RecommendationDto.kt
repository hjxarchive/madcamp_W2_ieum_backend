package com.ieum.ieum_back.recommendation.dto

import com.ieum.ieum_back.entity.Recommendation
import com.ieum.ieum_back.entity.enums.RecommendationStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class CreateRecommendationRequest(
    val locationAddress: String? = null,
    val locationLat: BigDecimal? = null,
    val locationLng: BigDecimal? = null,
    val date: LocalDate? = null,
    val preferences: Map<String, Any>? = null
)

data class RecommendationFeedbackRequest(
    val rating: Int, // 1-5
    val comment: String? = null,
    val saveAsEvent: Boolean = false
)

data class RecommendationResponse(
    val id: UUID,
    val status: RecommendationStatus,
    val locationAddress: String?,
    val locationLat: BigDecimal?,
    val locationLng: BigDecimal?,
    val date: LocalDate?,
    val preferences: Map<String, Any>?,
    val result: Map<String, Any>?,
    val feedback: Map<String, Any>?,
    val savedEventId: UUID?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
) {
    companion object {
        fun from(recommendation: Recommendation): RecommendationResponse = RecommendationResponse(
            id = recommendation.id!!,
            status = recommendation.status,
            locationAddress = recommendation.locationAddress,
            locationLat = recommendation.locationLat,
            locationLng = recommendation.locationLng,
            date = recommendation.date,
            preferences = recommendation.preferences,
            result = recommendation.result,
            feedback = recommendation.feedback,
            savedEventId = recommendation.savedEvent?.id,
            createdAt = recommendation.createdAt,
            completedAt = recommendation.completedAt
        )
    }
}

data class RecommendationListResponse(
    val recommendations: List<RecommendationResponse>,
    val totalCount: Long,
    val page: Int,
    val size: Int
)
