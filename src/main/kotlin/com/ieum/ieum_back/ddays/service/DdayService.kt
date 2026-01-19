package com.ieum.ieum_back.ddays.service

import com.ieum.ieum_back.ddays.dto.DdayListResponse
import com.ieum.ieum_back.ddays.dto.DdayResponse
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.CoupleRepository
import com.ieum.ieum_back.repository.EventRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional(readOnly = true)
class DdayService(
    private val userRepository: UserRepository,
    private val coupleRepository: CoupleRepository,
    private val eventRepository: EventRepository
) {

    fun getDdays(userId: UUID): DdayListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val today = LocalDate.now()
        val ddays = mutableListOf<DdayResponse>()

        // 기념일 (Anniversary) 추가
        couple.anniversary?.let { anniversary ->
            val daysSince = ChronoUnit.DAYS.between(anniversary, today)
            ddays.add(
                DdayResponse(
                    title = "우리가 만난 날",
                    date = anniversary,
                    dday = -daysSince, // 음수로 표시 (D+xxx)
                    type = "anniversary"
                )
            )

            // 100일, 200일, 300일, 1주년 등 기념일 추가
            listOf(100, 200, 300, 365, 500, 730, 1000, 1095).forEach { days ->
                val targetDate = anniversary.plusDays(days.toLong())
                if (targetDate.isAfter(today) || targetDate.isEqual(today)) {
                    val daysLeft = ChronoUnit.DAYS.between(today, targetDate)
                    val title = when (days) {
                        365 -> "1주년"
                        730 -> "2주년"
                        1095 -> "3주년"
                        else -> "${days}일"
                    }
                    ddays.add(
                        DdayResponse(
                            title = title,
                            date = targetDate,
                            dday = daysLeft,
                            type = "anniversary"
                        )
                    )
                }
            }
        }

        // 다가오는 이벤트 추가
        val upcomingEvents = eventRepository.findUpcomingEvents(couple, LocalDateTime.now())
        upcomingEvents.take(10).forEach { event ->
            val eventDate = event.startDate.toLocalDate()
            val daysLeft = ChronoUnit.DAYS.between(today, eventDate)
            ddays.add(
                DdayResponse(
                    title = event.title,
                    date = eventDate,
                    dday = daysLeft,
                    type = "event"
                )
            )
        }

        return DdayListResponse(
            ddays = ddays.sortedBy { it.dday }
        )
    }
}
