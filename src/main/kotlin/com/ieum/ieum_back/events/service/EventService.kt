package com.ieum.ieum_back.events.service

import com.ieum.ieum_back.entity.Event
import com.ieum.ieum_back.events.dto.*
import com.ieum.ieum_back.exception.NotFoundException
import com.ieum.ieum_back.repository.EventRepository
import com.ieum.ieum_back.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    fun createEvent(userId: UUID, request: CreateEventRequest): EventResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val event = Event(
            couple = couple,
            createdBy = user,
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            isAllDay = request.isAllDay,
            location = request.location,
            reminderMinutes = request.reminderMinutes,
            repeat = request.repeat
        )

        return EventResponse.from(eventRepository.save(event))
    }

    @Transactional(readOnly = true)
    fun getEvents(userId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): EventListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val events = eventRepository.findByCoupleAndDateRange(couple, startDate, endDate)
        return EventListResponse(events.map { EventResponse.from(it) })
    }

    @Transactional(readOnly = true)
    fun getEvent(userId: UUID, eventId: UUID): EventResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val event = eventRepository.findByIdAndCoupleAndDeletedAtIsNull(eventId, couple)
            ?: throw NotFoundException("Event not found")

        return EventResponse.from(event)
    }

    fun updateEvent(userId: UUID, eventId: UUID, request: UpdateEventRequest): EventResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val event = eventRepository.findByIdAndCoupleAndDeletedAtIsNull(eventId, couple)
            ?: throw NotFoundException("Event not found")

        request.title?.let { event.title = it }
        request.description?.let { event.description = it }
        request.startDate?.let { event.startDate = it }
        request.endDate?.let { event.endDate = it }
        request.isAllDay?.let { event.isAllDay = it }
        request.location?.let { event.location = it }
        request.reminderMinutes?.let { event.reminderMinutes = it }
        request.repeat?.let { event.repeat = it }

        return EventResponse.from(eventRepository.save(event))
    }

    fun deleteEvent(userId: UUID, eventId: UUID) {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        val couple = user.couple ?: throw NotFoundException("Couple not found")

        val event = eventRepository.findByIdAndCoupleAndDeletedAtIsNull(eventId, couple)
            ?: throw NotFoundException("Event not found")

        event.deletedAt = LocalDateTime.now()
        eventRepository.save(event)
    }
}
