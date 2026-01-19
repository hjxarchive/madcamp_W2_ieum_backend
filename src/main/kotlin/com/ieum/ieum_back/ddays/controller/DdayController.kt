package com.ieum.ieum_back.ddays.controller

import com.ieum.ieum_back.ddays.dto.DdayListResponse
import com.ieum.ieum_back.ddays.service.DdayService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/ddays")
class DdayController(
    private val ddayService: DdayService
) {

    @GetMapping
    fun getDdays(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<DdayListResponse> {
        return ResponseEntity.ok(ddayService.getDdays(userId))
    }
}
