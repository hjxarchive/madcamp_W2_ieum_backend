package com.ieum.ieum_back.mbti.service

import com.ieum.ieum_back.exception.BadRequestException
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.mbti.dto.*
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class MbtiService(
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val questions = listOf(
        // 1. 데이트 계획 (Planner vs Flow) - 9문제
        MbtiQuestion(1, "우리는 데이트 전에 계획이 잡혀 있어야 마음이 편하다.", "X", "O", "PF"),
        MbtiQuestion(2, "데이트 당일, 즉흥적으로 결정해도 괜찮다.", "X", "O", "PF"),
        MbtiQuestion(3, "그날 기분에 따라 데이트 코스를 유동적으로 변경한다.", "X", "O", "PF"),
        MbtiQuestion(4, "데이트 전에 동선과 시간을 정해두는 편이다.", "O", "X", "PF"),
        MbtiQuestion(5, "계획 없는 데이트도 충분히 즐길 수 있다.", "X", "O", "PF"),
        MbtiQuestion(6, "데이트 장소는 미리 정해두는 게 좋다.", "O", "X", "PF"),
        MbtiQuestion(7, "걷다가 마음에 드는 곳에 들어가는 게 좋다.", "X", "O", "PF"),
        MbtiQuestion(8, "당일에 갑자기 바뀌는 일정도 재미있다.", "X", "O", "PF"),
        MbtiQuestion(9, "계획한 대로 진행되는 데이트가 만족스럽다.", "O", "X", "PF"),
        
        // 2. 소비 성향 (Measured vs Indulgent) - 9문제
        MbtiQuestion(10, "데이트에서는 돈보다 경험이 더 중요하다.", "X", "O", "MI"),
        MbtiQuestion(11, "데이트 비용은 합리적인 선에서 쓰고 싶다.", "O", "X", "MI"),
        MbtiQuestion(12, "특별한 날이라면 비용이 커도 괜찮다.", "X", "O", "MI"),
        MbtiQuestion(13, "추억이 남는다면 지출이 아깝지 않다.", "X", "O", "MI"),
        MbtiQuestion(14, "데이트 비용의 상한을 미리 정해두는 게 좋다.", "O", "X", "MI"),
        MbtiQuestion(15, "계획보다 지출이 커지면 신경 쓰인다.", "O", "X", "MI"),
        MbtiQuestion(16, "좋은 경험을 위해 예산을 넘길 수 있다.", "X", "O", "MI"),
        MbtiQuestion(17, "데이트 비용이 부담되면 즐기기 어렵다.", "O", "X", "MI"),
        MbtiQuestion(18, "데이트 비용을 기록하는 게 필요하다.", "O", "X", "MI"),
        
        // 3. 갈등 성향 (Direct vs Thoughtful) - 9문제
        MbtiQuestion(19, "감정이 가라앉을 시간이 필요하다.", "X", "O", "DT"),
        MbtiQuestion(20, "문제가 생기면 그날 해결하고 싶다.", "O", "X", "DT"),
        MbtiQuestion(21, "혼자 생각한 뒤 얘기하는 게 편하다.", "X", "O", "DT"),
        MbtiQuestion(22, "감정 정리가 되지 않으면 말하기 어렵다.", "X", "O", "DT"),
        MbtiQuestion(23, "서로의 생각을 바로 확인하고 싶다.", "O", "X", "DT"),
        MbtiQuestion(24, "혼자 정리할 시간을 존중받고 싶다.", "X", "O", "DT"),
        MbtiQuestion(25, "문제를 미루는 게 불안하다.", "O", "X", "DT"),
        MbtiQuestion(26, "감정이 정리된 후 대화가 더 잘 된다.", "X", "O", "DT"),
        MbtiQuestion(27, "싸운 채로 하루를 넘기기 싫다.", "O", "X", "DT"),
        
        // 4. 도전 성향 (Explorer vs Comfort) - 9문제
        MbtiQuestion(28, "새로운 장소를 가는 데 설렘을 느낀다.", "X", "O", "EC"),
        MbtiQuestion(29, "익숙한 장소가 가장 편하다.", "O", "X", "EC"),
        MbtiQuestion(30, "단골 데이트가 안정적이다.", "O", "X", "EC"),
        MbtiQuestion(31, "신상 맛집이나 공간에 관심이 많다.", "X", "O", "EC"),
        MbtiQuestion(32, "익숙한 루틴이 더 좋다.", "O", "X", "EC"),
        MbtiQuestion(33, "처음 해보는 활동도 도전해보고 싶다.", "X", "O", "EC"),
        MbtiQuestion(34, "여행지에서도 새로운 곳을 찾는 편이다.", "X", "O", "EC"),
        MbtiQuestion(35, "새로운 시도가 재미있다.", "X", "O", "EC"),
        MbtiQuestion(36, "실패하더라도 새로운 걸 해보고 싶다.", "X", "O", "EC")
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
            "M" to 0, "I" to 0,
            "D" to 0, "T" to 0,
            "E" to 0, "C" to 0,
            "P" to 0, "F" to 0
        )

        questions.forEach { question ->
            val answer = request.answers[question.id]
                ?: throw BadRequestException("Missing answer for question ${question.id}")

            when (question.dimension) {
                "MI" -> if (answer == "A") scores["M"] = scores["M"]!! + 1 else scores["I"] = scores["I"]!! + 1
                "DT" -> if (answer == "A") scores["D"] = scores["D"]!! + 1 else scores["T"] = scores["T"]!! + 1
                "EC" -> if (answer == "A") scores["E"] = scores["E"]!! + 1 else scores["C"] = scores["C"]!! + 1
                "PF" -> if (answer == "A") scores["P"] = scores["P"]!! + 1 else scores["F"] = scores["F"]!! + 1
            }
        }

        val mbtiType = buildString {
            append(if (scores["M"]!! >= scores["I"]!!) "M" else "I")
            append(if (scores["D"]!! >= scores["T"]!!) "D" else "T")
            append(if (scores["E"]!! >= scores["C"]!!) "E" else "C")
            append(if (scores["P"]!! >= scores["F"]!!) "P" else "F")
        }

        user.mbtiType = mbtiType
        user.mbtiAnswers = request.answers.mapKeys { it.key.toString() }
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        // 🔔 파트너에게 MBTI 업데이트 알림 (WebSocket)
        user.couple?.let { couple ->
            val partnerId = if (couple.user1Id == userId) couple.user2Id else couple.user1Id
            partnerId?.let {
                messagingTemplate.convertAndSend(
                    "/topic/couple/${couple.id}",
                    mapOf(
                        "type" to "MBTI_UPDATED",
                        "userId" to userId.toString(),
                        "mbtiType" to mbtiType,
                        "timestamp" to LocalDateTime.now()
                    )
                )
            }
        }

        val details = mapOf(
            "M" to scores["M"]!!,
            "I" to scores["I"]!!,
            "D" to scores["D"]!!,
            "T" to scores["T"]!!,
            "E" to scores["E"]!!,
            "C" to scores["C"]!!,
            "P" to scores["P"]!!,
            "F" to scores["F"]!!
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

        // 같은 차원 비교 (M/I, D/T, E/C, P/F)
        if (mbti1[0] == mbti2[0]) score += 15 // M/I (소비 성향)
        if (mbti1[1] == mbti2[1]) score += 10 // D/T (갈등 해결)
        if (mbti1[2] == mbti2[2]) score += 10 // E/C (도전 성향)
        if (mbti1[3] == mbti2[3]) score += 15 // P/F (데이트 계획)

        val strengths = mutableListOf<String>()
        val challenges = mutableListOf<String>()

        // M/I: 소비 성향
        if (mbti1[0] == mbti2[0]) {
            strengths.add("데이트 비용에 대한 생각이 비슷합니다")
        } else {
            challenges.add("합리적 소비 vs 경험 중시 소비에 대한 대화가 필요합니다")
        }

        // D/T: 갈등 해결
        if (mbti1[1] == mbti2[1]) {
            strengths.add("갈등 해결 방식이 잘 맞습니다")
        } else {
            challenges.add("즉시 대화 vs 시간이 필요한 성향을 서로 존중해야 합니다")
        }

        // E/C: 도전 성향
        if (mbti1[2] == mbti2[2]) {
            strengths.add("데이트 장소 선택 취향이 비슷합니다")
        } else {
            challenges.add("새로운 곳 탐험 vs 익숙한 장소 선호의 절충이 필요합니다")
        }

        // P/F: 데이트 계획
        if (mbti1[3] == mbti2[3]) {
            strengths.add("데이트 계획 스타일이 잘 맞습니다")
        } else {
            challenges.add("계획적 데이트 vs 즉흥적 데이트에 대한 균형이 필요합니다")
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
