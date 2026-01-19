package com.ieum.ieum_back.mbti.controller

import com.ieum.ieum_back.mbti.dto.MbtiCoupleResultResponse
import com.ieum.ieum_back.mbti.dto.MbtiQuestionsResponse
import com.ieum.ieum_back.mbti.dto.MbtiSubmitRequest
import com.ieum.ieum_back.mbti.dto.MbtiSubmitResponse
import com.ieum.ieum_back.mbti.service.MbtiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/mbti")
class MbtiController(
    private val mbtiService: MbtiService
) {

    @GetMapping("/questions")
    fun getQuestions(): ResponseEntity<MbtiQuestionsResponse> {
        return ResponseEntity.ok(mbtiService.getQuestions())
    }

    @PostMapping("/submit")
    fun submitAnswers(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: MbtiSubmitRequest
    ): ResponseEntity<MbtiSubmitResponse> {
        return ResponseEntity.ok(mbtiService.submitAnswers(userId, request))
    }

    @GetMapping("/couple-result")
    fun getCoupleResult(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<MbtiCoupleResultResponse> {
        return ResponseEntity.ok(mbtiService.getCoupleResult(userId))
    }
}
