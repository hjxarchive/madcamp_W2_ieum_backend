package com.ieum.ieum_back.recommendation.service

import com.ieum.ieum_back.entity.Event
import com.ieum.ieum_back.entity.Recommendation
import com.ieum.ieum_back.entity.enums.RecommendationStatus
import com.ieum.ieum_back.entity.enums.RepeatType
import com.ieum.ieum_back.exception.BadRequestException
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.recommendation.dto.*
import com.ieum.ieum_back.repository.EventRepository
import com.ieum.ieum_back.repository.RecommendationRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    fun createRecommendation(userId: UUID, request: CreateRecommendationRequest): RecommendationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val recommendation = Recommendation(
            couple = couple,
            createdBy = user,
            locationAddress = request.locationAddress,
            locationLat = request.locationLat,
            locationLng = request.locationLng,
            date = request.date,
            preferences = request.preferences,
            status = RecommendationStatus.PENDING
        )

        val saved = recommendationRepository.save(recommendation)

        // TODO: 비동기로 AI 추천 처리 시작
        // processRecommendation(saved.id!!)

        return RecommendationResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun getRecommendations(userId: UUID, page: Int, size: Int): RecommendationListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val pageable = PageRequest.of(page, size)
        val recommendations = recommendationRepository.findByCoupleOrderByCreatedAtDesc(couple, pageable)

        return RecommendationListResponse(
            recommendations = recommendations.content.map { RecommendationResponse.from(it) },
            totalCount = recommendations.totalElements,
            page = page,
            size = size
        )
    }

    @Transactional(readOnly = true)
    fun getRecommendation(userId: UUID, recommendationId: UUID): RecommendationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val recommendation = recommendationRepository.findByIdAndCouple(recommendationId, couple)
            ?: throw NotFoundException("Recommendation not found")

        return RecommendationResponse.from(recommendation)
    }

    fun submitFeedback(userId: UUID, recommendationId: UUID, request: RecommendationFeedbackRequest): RecommendationResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val recommendation = recommendationRepository.findByIdAndCouple(recommendationId, couple)
            ?: throw NotFoundException("Recommendation not found")

        if (recommendation.status != RecommendationStatus.COMPLETED) {
            throw BadRequestException("Recommendation is not completed yet")
        }

        recommendation.feedback = mapOf(
            "rating" to request.rating,
            "comment" to (request.comment ?: ""),
            "submittedAt" to LocalDateTime.now().toString()
        )

        if (request.saveAsEvent && recommendation.result != null) {
            val result = recommendation.result!!
            val event = Event(
                couple = couple,
                createdBy = user,
                title = result["title"]?.toString() ?: "추천 일정",
                description = result["description"]?.toString(),
                startDate = recommendation.date?.atStartOfDay() ?: LocalDateTime.now(),
                endDate = recommendation.date?.atTime(23, 59) ?: LocalDateTime.now().plusHours(2),
                isAllDay = true,
                location = recommendation.locationAddress,
                repeat = RepeatType.NONE
            )
            val savedEvent = eventRepository.save(event)
            recommendation.savedEvent = savedEvent
        }

        return RecommendationResponse.from(recommendationRepository.save(recommendation))
    }

    // 실제 AI 추천 처리 (비동기로 호출)
    fun processRecommendation(recommendationId: UUID) {
        val recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow { NotFoundException("Recommendation not found") }

        recommendation.status = RecommendationStatus.PROCESSING
        recommendationRepository.save(recommendation)

        try {
            // TODO: AI API 호출하여 추천 결과 생성
            val result = generateRecommendation(recommendation)

            recommendation.result = result
            recommendation.status = RecommendationStatus.COMPLETED
            recommendation.completedAt = LocalDateTime.now()
        } catch (e: Exception) {
            recommendation.status = RecommendationStatus.FAILED
        }

        recommendationRepository.save(recommendation)
    }

    private fun generateRecommendation(recommendation: Recommendation): Map<String, Any> {
        // TODO: 실제 AI 추천 로직 구현
        // 현재는 mock 데이터 반환
        return mapOf(
            "title" to "추천 데이트 코스",
            "description" to "맛있는 점심과 함께하는 카페 데이트",
            "places" to listOf(
                mapOf(
                    "name" to "분위기 좋은 레스토랑",
                    "category" to "restaurant",
                    "address" to "서울시 강남구"
                ),
                mapOf(
                    "name" to "조용한 카페",
                    "category" to "cafe",
                    "address" to "서울시 강남구"
                )
            ),
            "estimatedTime" to "3-4시간",
            "estimatedCost" to "50,000원"
        )
    }
}
