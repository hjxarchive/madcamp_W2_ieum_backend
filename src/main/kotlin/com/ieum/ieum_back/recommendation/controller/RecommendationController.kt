package com.ieum.ieum_back.recommendation.controller

import com.ieum.ieum_back.recommendation.dto.*
import com.ieum.ieum_back.recommendation.service.RecommendationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping
    fun createRecommendation(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateRecommendationRequest
    ): ResponseEntity<RecommendationResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(recommendationService.createRecommendation(userId, request))
    }

    @GetMapping
    fun getRecommendations(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<RecommendationListResponse> {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, page, size))
    }

    @GetMapping("/{recommendationId}")
    fun getRecommendation(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable recommendationId: UUID
    ): ResponseEntity<RecommendationResponse> {
        return ResponseEntity.ok(recommendationService.getRecommendation(userId, recommendationId))
    }

    @PostMapping("/{recommendationId}/feedback")
    fun submitFeedback(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable recommendationId: UUID,
        @RequestBody request: RecommendationFeedbackRequest
    ): ResponseEntity<RecommendationResponse> {
        return ResponseEntity.ok(recommendationService.submitFeedback(userId, recommendationId, request))
    }
}
