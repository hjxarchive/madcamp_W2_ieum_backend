package com.ieum.ieum_back.chat.controller

import com.ieum.ieum_back.chat.dto.*
import com.ieum.ieum_back.chat.service.ChatWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.util.*

/**
 * WebSocket STOMP 메시지 핸들러
 * 
 * @MessageMapping: 클라이언트가 /app/chat/{coupleId}로 메시지 전송
 * @SendTo: 구독자들에게 /topic/couple/{coupleId}로 브로드캐스트
 */
@Controller
class ChatWebSocketController(
    private val chatWebSocketService: ChatWebSocketService
) {

    private val logger = LoggerFactory.getLogger(ChatWebSocketController::class.java)

    /**
     * 채팅 메시지 전송
     * 
     * 클라이언트: SEND /app/chat/{coupleId}
     * 브로드캐스트: /topic/couple/{coupleId}
     */
    @MessageMapping("/chat/{coupleId}")
    @SendTo("/topic/couple/{coupleId}")
    fun sendMessage(
        @DestinationVariable coupleId: String,
        @Payload request: WebSocketMessageRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ): Any {
        return try {
            // WebSocket 세션에서 userId 추출 (Handshake 시 저장됨)
            val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
                ?: throw IllegalArgumentException("인증되지 않은 사용자입니다")

            logger.info("Message received from user $userId to couple $coupleId")

            // 메시지 저장 및 처리
            val response = chatWebSocketService.sendMessage(
                coupleId = UUID.fromString(coupleId),
                senderId = userId,
                request = request
            )

            response

        } catch (e: Exception) {
            logger.error("Error sending message to couple $coupleId", e)
            
            // 에러 응답
            WebSocketErrorResponse(
                code = ErrorCode.SEND_FAILED,
                message = e.message ?: "메시지 전송에 실패했습니다",
                tempId = request.tempId
            )
        }
    }

    /**
     * 메시지 읽음 처리
     * 
     * 클라이언트: SEND /app/chat/{coupleId}/read
     * 브로드캐스트: /topic/couple/{coupleId}/read (별도 토픽)
     */
    @MessageMapping("/chat/{coupleId}/read")
    @SendTo("/topic/couple/{coupleId}/read")
    fun markAsRead(
        @DestinationVariable coupleId: String,
        @Payload messageIds: List<String>,
        headerAccessor: SimpMessageHeaderAccessor
    ): Any {
        return try {
            val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
                ?: throw IllegalArgumentException("인증되지 않은 사용자입니다")

            logger.info("Mark as read request from user $userId for ${messageIds.size} messages")

            val ids = messageIds.map { UUID.fromString(it) }
            chatWebSocketService.markMessagesAsRead(
                coupleId = UUID.fromString(coupleId),
                userId = userId,
                messageIds = ids
            )

        } catch (e: Exception) {
            logger.error("Error marking messages as read", e)
            
            WebSocketErrorResponse(
                code = ErrorCode.SEND_FAILED,
                message = e.message ?: "읽음 처리에 실패했습니다"
            )
        }
    }

    /**
     * 타이핑 인디케이터
     * 
     * 클라이언트: SEND /app/chat/{coupleId}/typing
     * 브로드캐스트: /topic/couple/{coupleId}/typing (별도 토픽)
     */
    @MessageMapping("/chat/{coupleId}/typing")
    @SendTo("/topic/couple/{coupleId}/typing")
    fun handleTyping(
        @DestinationVariable coupleId: String,
        @Payload request: TypingRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ): TypingIndicatorResponse? {
        return try {
            val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
                ?: return null

            TypingIndicatorResponse(
                userId = userId.toString(),
                isTyping = request.isTyping
            )

        } catch (e: Exception) {
            logger.error("Error handling typing indicator", e)
            null
        }
    }

    /**
     * E2EE 암호화된 메시지 전송
     * 
     * 클라이언트: SEND /app/chat/{coupleId}/e2ee
     * 브로드캐스트: /topic/couple/{coupleId}
     */
    @MessageMapping("/chat/{coupleId}/e2ee")
    @SendTo("/topic/couple/{coupleId}")
    fun sendE2EEMessage(
        @DestinationVariable coupleId: String,
        @Payload request: E2EEMessageRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ): Any {
        return try {
            val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
                ?: throw IllegalArgumentException("인증되지 않은 사용자입니다")

            logger.info("E2EE message received from user $userId to couple $coupleId")

            // E2EE 메시지 저장 및 처리
            val response = chatWebSocketService.sendE2EEMessage(
                coupleId = UUID.fromString(coupleId),
                senderId = userId,
                request = request
            )

            response

        } catch (e: Exception) {
            logger.error("Error sending E2EE message to couple $coupleId", e)
            
            WebSocketErrorResponse(
                code = ErrorCode.SEND_FAILED,
                message = e.message ?: "E2EE 메시지 전송에 실패했습니다",
                tempId = request.tempId
            )
        }
    }
}
