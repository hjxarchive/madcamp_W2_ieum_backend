package com.ieum.ieum_back.events.dto

import com.ieum.ieum_back.entity.Event
import com.ieum.ieum_back.entity.enums.RepeatType
import java.time.LocalDateTime
import java.util.*

data class CreateEventRequest(
    val title: String,
    val description: String? = null,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isAllDay: Boolean = false,
    val location: String? = null,
    val reminderMinutes: Int? = null,
    val repeat: RepeatType = RepeatType.NONE
)

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val isAllDay: Boolean? = null,
    val location: String? = null,
    val reminderMinutes: Int? = null,
    val repeat: RepeatType? = null
)

data class EventResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isAllDay: Boolean,
    val location: String?,
    val reminderMinutes: Int?,
    val repeat: RepeatType,
    val createdById: UUID,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(event: Event): EventResponse = EventResponse(
            id = event.id!!,
            title = event.title,
            description = event.description,
            startDate = event.startDate,
            endDate = event.endDate,
            isAllDay = event.isAllDay,
            location = event.location,
            reminderMinutes = event.reminderMinutes,
            repeat = event.repeat,
            createdById = event.createdBy.id!!,
            createdAt = event.createdAt
        )
    }
}

data class EventListResponse(
    val events: List<EventResponse>
)
