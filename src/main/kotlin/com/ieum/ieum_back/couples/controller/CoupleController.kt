package com.ieum.ieum_back.couples.controller

import com.ieum.ieum_back.couples.dto.CoupleResponse
import com.ieum.ieum_back.couples.dto.InviteCodeResponse
import com.ieum.ieum_back.couples.dto.JoinCoupleRequest
import com.ieum.ieum_back.couples.dto.UpdateCoupleRequest
import com.ieum.ieum_back.couples.service.CoupleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/couples")
class CoupleController(
    private val coupleService: CoupleService
) {

    @PostMapping("/invite")
    fun createInviteCode(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<InviteCodeResponse> {
        return ResponseEntity.ok(coupleService.createInviteCode(userId))
    }

    @PostMapping("/join")
    fun joinCouple(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: JoinCoupleRequest
    ): ResponseEntity<CoupleResponse> {
        return ResponseEntity.ok(coupleService.joinCouple(userId, request.inviteCode))
    }

    @GetMapping("/me")
    fun getCouple(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<CoupleResponse> {
        return ResponseEntity.ok(coupleService.getCouple(userId))
    }

    @PatchMapping("/me")
    fun updateCouple(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateCoupleRequest
    ): ResponseEntity<CoupleResponse> {
        return ResponseEntity.ok(coupleService.updateCouple(userId, request))
    }

    @DeleteMapping("/me")
    fun deleteCouple(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Void> {
        coupleService.deleteCouple(userId)
        return ResponseEntity.noContent().build()
    }
}
