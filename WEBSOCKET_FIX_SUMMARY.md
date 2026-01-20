# ✅ WebSocket 연결 문제 해결 완료 (요약)

## 📅 2025-01-20 20:35 KST

---

## 🎯 핵심 해결책

### 문제 1: HTTP 500 에러
**원인:** SockJS 미활성화  
**해결:** `/ws/chat` 엔드포인트에 `.withSockJS()` 추가

### 문제 2: 연결 후 즉시 끊김 ⭐
**원인:** STOMP CONNECT 프레임 대기 시간 부족  
**해결:** `setTimeToFirstMessage(60 * 1000)` 설정 추가

---

## 🔧 주요 수정 파일

### 1. WebSocketConfig.kt
```kotlin
// STOMP CONNECT 대기 시간 60초 확보
override fun configureWebSocketTransport(registry: WebSocketTransportRegistration) {
    registry.setTimeToFirstMessage(60 * 1000)  // ✅ 핵심!
}

// STOMP 프레임 인터셉터 등록
override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(stompConnectInterceptor)
}
```

### 2. StompConnectInterceptor.kt (신규 생성)
```kotlin
// STOMP CONNECT, SUBSCRIBE, DISCONNECT 로깅
@Component
class StompConnectInterceptor : ChannelInterceptor {
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        // STOMP 명령어별 상세 로깅
    }
}
```

### 3. WebSocketEventListener.kt
```kotlin
// 연결 해제 이유 로깅 추가
fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
    logger.info("Close Status: ${event.closeStatus}")  // ✅
}
```

---

## 📡 프론트엔드 가이드

### 연결 테스트
```kotlin
// 이제 이 흐름이 정상 작동합니다
1. WebSocket 연결 (ws://54.66.195.91/ws/chat?token={JWT})
2. ⏰ 60초 대기 가능 (이전: 즉시 끊김)
3. STOMP CONNECT 프레임 전송
4. 구독 시작 (/topic/couple/{coupleId})
5. 메시지 송수신 ✅
```

### 백엔드 로그 확인
프론트엔드에서 연결 시도 후 백엔드 로그를 확인하면:
```bash
docker logs -f spring-app | grep "STOMP CONNECT"

# 예상 로그:
========== STOMP CONNECT Frame Received ==========
✅ STOMP CONNECT authenticated for user: ...
```

---

## ✅ 배포 완료

- **서버:** http://54.66.195.91
- **WebSocket:** ws://54.66.195.91/ws/chat
- **상태:** 정상 작동
- **Health Check:** ✅

---

## 📞 문제 발생 시

프론트엔드에서 여전히 연결이 끊기면:

1. **백엔드 로그 확인**
   ```bash
   ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 "docker logs spring-app --tail=100 | grep -E 'STOMP|CONNECT|disconnect'"
   ```

2. **확인 사항**
   - `STOMP CONNECT Frame Received` 로그가 있는가?
   - `Close Status: ...` 에러 코드는?

3. **일반적인 원인**
   - JWT 토큰 만료 → 새 토큰으로 재시도
   - 네트워크 불안정 → 재연결 로직 추가
   - 프론트엔드 STOMP 라이브러리 버전 → 최신 버전 사용

---

**상세 문서:** [WEBSOCKET_500_ERROR_FIX.md](WEBSOCKET_500_ERROR_FIX.md)
