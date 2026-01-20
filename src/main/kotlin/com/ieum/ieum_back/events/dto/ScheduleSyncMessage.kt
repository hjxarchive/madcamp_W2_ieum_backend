package com.ieum.ieum_back.events.dto

import java.time.LocalDateTime
import java.util.*

/**
 * 일정 WebSocket 실시간 동기화 메시지
 */
data class ScheduleSyncMessage(
    val eventType: String,  // ADDED, UPDATED, DELETED
    val schedule: ScheduleDto,
    val userId: String,
    val timestamp: String
)

data class ScheduleDto(
    val id: String,
    val title: String,
    val date: String,
    val time: String?,
    val colorHex: String,
    val description: String?
) {
    companion object {
        fun from(response: EventResponse): ScheduleDto {
            return ScheduleDto(
                id = response.id.toString(),
                title = response.title,
                date = response.startDate.toLocalDate().toString(),
                time = if (!response.isAllDay) response.startDate.toLocalTime().toString() else null,
                colorHex = "#FF6B9D",  // 기본 색상
                description = response.description
            )
        }
    }
}
