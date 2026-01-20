package com.ieum.ieum_back.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthInterceptor: WebSocketAuthInterceptor,
    private val stompConnectInterceptor: StompConnectInterceptor
) : WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 순수 WebSocket 엔드포인트 (SockJS 없음) - 권장
        // 클라이언트: ws://server/ws/stomp?token={JWT}
        // 안드로이드 STOMP 클라이언트가 직접 STOMP 프레임 전송
        registry.addEndpoint("/ws/stomp")
            .setAllowedOriginPatterns("*")  // CORS 설정 (프로덕션에서는 특정 도메인으로 제한)
            .addInterceptors(webSocketAuthInterceptor)  // JWT 인증 인터셉터
        
        // SockJS 엔드포인트 (폴백용)
        // 클라이언트: ws://server/ws/chat/{serverId}/{sessionId}/websocket?token={JWT}
        // SockJS는 자동으로 /{serverId}/{sessionId}/websocket 경로 생성
        // 주의: SockJS는 STOMP 프레임을 JSON 배열로 감싸야 함
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(webSocketAuthInterceptor)
            .withSockJS()  // SockJS 지원 (폴백 포함)
    }

    /**
     * 메시지 브로커 설정
     * - /topic: 클라이언트가 구독하는 목적지 (서버 -> 클라이언트)
     * - /app: 클라이언트가 메시지를 보내는 목적지 (클라이언트 -> 서버)
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")  // 단순 메모리 기반 브로커
        registry.setApplicationDestinationPrefixes("/app")  // @MessageMapping의 prefix
    }

    /**
     * WebSocket Transport 설정
     * 클라이언트가 STOMP CONNECT 프레임을 보낼 시간 확보
     */
    override fun configureWebSocketTransport(registry: WebSocketTransportRegistration) {
        registry
            .setMessageSizeLimit(128 * 1024)         // 128KB - 메시지 최대 크기
            .setSendTimeLimit(30 * 1000)             // 30초 - 전송 타임아웃
            .setSendBufferSizeLimit(512 * 1024)      // 512KB - 전송 버퍼 크기
            .setTimeToFirstMessage(60 * 1000)        // 60초 - ✅ STOMP CONNECT 대기 시간 (중요!)
    }

    /**
     * 클라이언트 인바운드 채널 설정
     * STOMP CONNECT 프레임 인증 인터셉터 등록
     */
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompConnectInterceptor)
    }
}
