package com.ieum.ieum_back.memory.controller

import com.ieum.ieum_back.memory.dto.*
import com.ieum.ieum_back.memory.service.MemoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/memories")
class MemoryController(
    private val memoryService: MemoryService
) {

    @PostMapping
    fun createMemory(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateMemoryRequest
    ): ResponseEntity<MemoryResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoryService.createMemory(userId, request))
    }

    @GetMapping
    fun getMemories(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<MemoryListResponse> {
        return ResponseEntity.ok(memoryService.getMemories(userId, page, size))
    }

    @GetMapping("/{memoryId}")
    fun getMemory(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable memoryId: UUID
    ): ResponseEntity<MemoryResponse> {
        return ResponseEntity.ok(memoryService.getMemory(userId, memoryId))
    }

    @PatchMapping("/{memoryId}")
    fun updateMemory(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable memoryId: UUID,
        @RequestBody request: UpdateMemoryRequest
    ): ResponseEntity<MemoryResponse> {
        return ResponseEntity.ok(memoryService.updateMemory(userId, memoryId, request))
    }

    @DeleteMapping("/{memoryId}")
    fun deleteMemory(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable memoryId: UUID
    ): ResponseEntity<Void> {
        memoryService.deleteMemory(userId, memoryId)
        return ResponseEntity.noContent().build()
    }
}
