package com.ieum.ieum_back.config

import com.ieum.ieum_back.chat.dto.SystemEvent
import com.ieum.ieum_back.chat.service.ChatWebSocketService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import java.util.*

/**
 * WebSocket 연결/해제 이벤트 핸들러
 */
@Component
class WebSocketEventListener(
    private val chatWebSocketService: ChatWebSocketService
) {

    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    /**
     * WebSocket 연결 시
     */
    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId

        logger.info("========== WebSocket STOMP Session Connected ==========")
        logger.info("Session ID: $sessionId")

        // 사용자 정보 추출 (선택적)
        val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
        logger.info("User ID from session: $userId")
        logger.info("=======================================================")

        // 연결 알림 (필요 시)
        // 커플 ID를 알 수 없으므로, 메시지 전송 시점에 처리
    }

    /**
     * WebSocket 연결 해제 시
     */
    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID

        logger.info("========== WebSocket STOMP Session Disconnected ==========")
        logger.info("Session ID: $sessionId")
        logger.info("User ID: $userId")
        logger.info("Close Status: ${event.closeStatus}")
        logger.info("===========================================================")

        // 연결 해제 알림 (필요 시)
        // 실제로는 사용자가 어느 커플에 속했는지 알기 어려우므로
        // 필요하다면 세션 관리 로직 추가 필요
    }
}
