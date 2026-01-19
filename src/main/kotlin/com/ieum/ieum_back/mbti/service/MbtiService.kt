package com.ieum.ieum_back.mbti.service

import com.ieum.ieum_back.exception.BadRequestException
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.mbti.dto.*
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class MbtiService(
    private val userRepository: UserRepository
) {
    private val questions = listOf(
        MbtiQuestion(1, "파티에서 나는?", "여러 사람과 어울리며 에너지를 얻는다", "소수의 친한 사람들과 깊은 대화를 나눈다", "EI"),
        MbtiQuestion(2, "새로운 사람을 만났을 때?", "먼저 말을 건다", "상대방이 먼저 말하기를 기다린다", "EI"),
        MbtiQuestion(3, "휴식할 때 선호하는 방식은?", "친구들과 함께 시간 보내기", "혼자만의 시간 갖기", "EI"),
        MbtiQuestion(4, "문제를 해결할 때?", "경험과 사실에 기반하여 해결한다", "직관과 가능성을 탐색한다", "SN"),
        MbtiQuestion(5, "새로운 것을 배울 때?", "단계별로 구체적인 방법을 선호한다", "큰 그림을 먼저 파악하고 싶다", "SN"),
        MbtiQuestion(6, "대화할 때?", "구체적이고 현실적인 이야기를 좋아한다", "추상적이고 이론적인 이야기를 좋아한다", "SN"),
        MbtiQuestion(7, "결정을 내릴 때?", "논리와 객관적 분석을 중시한다", "감정과 가치를 중시한다", "TF"),
        MbtiQuestion(8, "갈등 상황에서?", "공정한 해결책을 찾는다", "조화와 관계를 우선시한다", "TF"),
        MbtiQuestion(9, "피드백을 줄 때?", "솔직하고 직접적으로 말한다", "상대방의 감정을 고려하며 말한다", "TF"),
        MbtiQuestion(10, "일정을 계획할 때?", "미리 계획하고 일정을 정한다", "유연하게 상황에 맞춰 진행한다", "JP"),
        MbtiQuestion(11, "마감 기한에 대해?", "여유를 두고 미리 끝내고 싶다", "마감에 가까워져야 집중이 된다", "JP"),
        MbtiQuestion(12, "여행할 때?", "세부 일정을 미리 계획한다", "즉흥적으로 결정한다", "JP")
    )

    @Transactional(readOnly = true)
    fun getQuestions(): MbtiQuestionsResponse {
        return MbtiQuestionsResponse(questions)
    }

    fun submitAnswers(userId: UUID, request: MbtiSubmitRequest): MbtiSubmitResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        if (request.answers.size != questions.size) {
            throw BadRequestException("All questions must be answered")
        }

        val scores = mutableMapOf(
            "E" to 0, "I" to 0,
            "S" to 0, "N" to 0,
            "T" to 0, "F" to 0,
            "J" to 0, "P" to 0
        )

        questions.forEach { question ->
            val answer = request.answers[question.id]
                ?: throw BadRequestException("Missing answer for question ${question.id}")

            when (question.dimension) {
                "EI" -> if (answer == "A") scores["E"] = scores["E"]!! + 1 else scores["I"] = scores["I"]!! + 1
                "SN" -> if (answer == "A") scores["S"] = scores["S"]!! + 1 else scores["N"] = scores["N"]!! + 1
                "TF" -> if (answer == "A") scores["T"] = scores["T"]!! + 1 else scores["F"] = scores["F"]!! + 1
                "JP" -> if (answer == "A") scores["J"] = scores["J"]!! + 1 else scores["P"] = scores["P"]!! + 1
            }
        }

        val mbtiType = buildString {
            append(if (scores["E"]!! >= scores["I"]!!) "E" else "I")
            append(if (scores["S"]!! >= scores["N"]!!) "S" else "N")
            append(if (scores["T"]!! >= scores["F"]!!) "T" else "F")
            append(if (scores["J"]!! >= scores["P"]!!) "J" else "P")
        }

        user.mbtiType = mbtiType
        user.mbtiAnswers = request.answers.mapKeys { it.key.toString() }
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        val details = mapOf(
            "E" to (scores["E"]!! * 100 / 3),
            "I" to (scores["I"]!! * 100 / 3),
            "S" to (scores["S"]!! * 100 / 3),
            "N" to (scores["N"]!! * 100 / 3),
            "T" to (scores["T"]!! * 100 / 3),
            "F" to (scores["F"]!! * 100 / 3),
            "J" to (scores["J"]!! * 100 / 3),
            "P" to (scores["P"]!! * 100 / 3)
        )

        return MbtiSubmitResponse(mbtiType, details)
    }

    @Transactional(readOnly = true)
    fun getCoupleResult(userId: UUID): MbtiCoupleResultResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }

        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val partnerId = if (couple.user1Id == userId) couple.user2Id else couple.user1Id
        val partner = partnerId?.let { userRepository.findById(it).orElse(null) }

        val compatibility = if (user.mbtiType != null && partner?.mbtiType != null) {
            calculateCompatibility(user.mbtiType!!, partner.mbtiType!!)
        } else null

        return MbtiCoupleResultResponse(
            myMbti = user.mbtiType,
            partnerMbti = partner?.mbtiType,
            compatibility = compatibility
        )
    }

    private fun calculateCompatibility(mbti1: String, mbti2: String): MbtiCompatibility {
        var score = 50

        // 같은 차원 비교
        if (mbti1[0] == mbti2[0]) score += 10 // E/I
        if (mbti1[1] == mbti2[1]) score += 15 // S/N
        if (mbti1[2] == mbti2[2]) score += 10 // T/F
        if (mbti1[3] == mbti2[3]) score += 15 // J/P

        val strengths = mutableListOf<String>()
        val challenges = mutableListOf<String>()

        if (mbti1[0] == mbti2[0]) {
            strengths.add("에너지 충전 방식이 비슷합니다")
        } else {
            challenges.add("혼자만의 시간 vs 함께하는 시간에 대한 조율이 필요합니다")
        }

        if (mbti1[1] == mbti2[1]) {
            strengths.add("정보를 받아들이는 방식이 비슷합니다")
        } else {
            challenges.add("현실적 vs 이상적 관점의 차이를 이해해야 합니다")
        }

        if (mbti1[2] != mbti2[2]) {
            strengths.add("서로 다른 결정 방식이 균형을 이룹니다")
        }

        if (mbti1[3] == mbti2[3]) {
            strengths.add("생활 방식이 비슷하여 갈등이 적습니다")
        } else {
            challenges.add("계획적 vs 즉흥적 생활방식의 조율이 필요합니다")
        }

        val description = when {
            score >= 80 -> "천생연분! 서로를 깊이 이해할 수 있는 조합입니다."
            score >= 60 -> "좋은 궁합! 서로의 다름을 통해 성장할 수 있습니다."
            else -> "도전적인 조합이지만, 노력하면 더 강한 관계를 만들 수 있습니다."
        }

        return MbtiCompatibility(
            score = score,
            description = description,
            strengths = strengths,
            challenges = challenges
        )
    }
}
