package com.ieum.ieum_back.mbti.dto

data class MbtiQuestion(
    val id: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val dimension: String // MI (소비 성향), DT (갈등 해결), EC (도전 성향), PF (데이트 계획)
)

data class MbtiQuestionsResponse(
    val questions: List<MbtiQuestion>
)

data class MbtiSubmitRequest(
    val answers: Map<Int, String> // questionId -> "A" or "B"
)

data class MbtiSubmitResponse(
    val mbtiType: String, // MDEP, ITCF 등 16가지 조합
    val details: Map<String, Int> // M: 7, I: 2, D: 5, T: 4, E: 6, C: 3, P: 8, F: 1 (각 9점 만점)
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
