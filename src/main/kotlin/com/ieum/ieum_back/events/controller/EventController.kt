package com.ieum.ieum_back.events.controller

import com.ieum.ieum_back.events.dto.*
import com.ieum.ieum_back.events.service.EventService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    fun createEvent(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(userId, request))
    }

    @GetMapping
    fun getEvents(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): ResponseEntity<EventListResponse> {
        return ResponseEntity.ok(eventService.getEvents(userId, startDate, endDate))
    }

    @GetMapping("/{eventId}")
    fun getEvent(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<EventResponse> {
        return ResponseEntity.ok(eventService.getEvent(userId, eventId))
    }

    @PatchMapping("/{eventId}")
    fun updateEvent(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable eventId: UUID,
        @RequestBody request: UpdateEventRequest
    ): ResponseEntity<EventResponse> {
        return ResponseEntity.ok(eventService.updateEvent(userId, eventId, request))
    }

    @DeleteMapping("/{eventId}")
    fun deleteEvent(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Void> {
        eventService.deleteEvent(userId, eventId)
        return ResponseEntity.noContent().build()
    }
}
