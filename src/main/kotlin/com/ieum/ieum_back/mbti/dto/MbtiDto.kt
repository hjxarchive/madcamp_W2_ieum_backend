package com.ieum.ieum_back.mbti.dto

data class MbtiQuestion(
    val id: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val dimension: String // E-I, S-N, T-F, J-P
)

data class MbtiQuestionsResponse(
    val questions: List<MbtiQuestion>
)

data class MbtiSubmitRequest(
    val answers: Map<Int, String> // questionId -> "A" or "B"
)

data class MbtiSubmitResponse(
    val mbtiType: String,
    val details: Map<String, Int> // E: 70, I: 30, ...
)

data class MbtiCoupleResultResponse(
    val myMbti: String?,
    val partnerMbti: String?,
    val compatibility: MbtiCompatibility?
)

data class MbtiCompatibility(
    val score: Int,
    val description: String,
    val strengths: List<String>,
    val challenges: List<String>
)
