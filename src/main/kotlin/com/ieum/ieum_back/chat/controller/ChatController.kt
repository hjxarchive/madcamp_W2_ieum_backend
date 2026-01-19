package com.ieum.ieum_back.chat.controller

import com.ieum.ieum_back.chat.dto.*
import com.ieum.ieum_back.chat.service.ChatService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    @GetMapping("/room")
    fun getChatRoom(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<ChatRoomResponse> {
        return ResponseEntity.ok(chatService.getChatRoom(userId))
    }

    @PostMapping("/rooms/{roomId}/messages")
    fun sendMessage(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable roomId: UUID,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<ChatMessageResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(userId, roomId, request))
    }

    @GetMapping("/rooms/{roomId}/messages")
    fun getMessages(
        @RequestHeader("X-User-Id") userId: UUID,
        @PathVariable roomId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<ChatMessageListResponse> {
        return ResponseEntity.ok(chatService.getMessages(userId, roomId, page, size))
    }
}
