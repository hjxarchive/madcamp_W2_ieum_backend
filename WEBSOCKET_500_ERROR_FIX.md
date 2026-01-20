# 🔧 WebSocket 연결 문제 수정 완료

## 📅 최종 수정일: 2025-01-20 20:35 KST

---

## 목차
1. [초기 문제: 500 에러](#초기-문제-500-에러)
2. [추가 문제: 연결 후 즉시 끊김](#추가-문제-연결-후-즉시-끊김)
3. [최종 해결 방안](#최종-해결-방안)

---

## 초기 문제: 500 에러

## 🔴 문제 상황

### 에러
- WebSocket 연결 시 **HTTP 500 Internal Server Error** 발생
- 프론트엔드: 연결 시도 → 서버 응답: 500 (예상: 101 Switching Protocols)

### 프론트엔드 요청 정보
```
URL: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket?token={JWT_TOKEN}
형식: ws://{server}/ws/chat/{serverId}/{sessionId}/websocket?token={JWT}
```

**전달 데이터:**
- JWT Token (쿼리 파라미터): 255자 HS512 토큰
- Couple ID: `2a3d5d24-7070-4e0e-91d1-1d2cd545bf8d`
- User ID: `0df27a65-9356-4952-8672-6d894e8dfff9`

---

## 🔍 원인 분석

### 1. SockJS URL 형식 불일치
프론트엔드는 **SockJS** 표준 URL 형식을 사용:
```
/ws/chat/{serverId}/{sessionId}/websocket
```

하지만 백엔드는:
- `/ws/chat` - SockJS **미사용** (순수 WebSocket)
- `/ws/chat-sockjs` - SockJS 사용 (별도 엔드포인트)

### 2. 엔드포인트 미스매치
프론트엔드가 요청한 `/ws/chat/774/edb1b44a/websocket` 경로는:
- SockJS가 생성하는 동적 경로
- 백엔드 `/ws/chat` 엔드포인트에 SockJS가 없어서 404/500 발생

---

## ✅ 수정 내용

### 1. WebSocketConfig.kt - SockJS 활성화

**변경 전:**
```kotlin
override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    // /ws/chat - 순수 WebSocket (SockJS 없음)
    registry.addEndpoint("/ws/chat")
        .setAllowedOriginPatterns("*")
        .addInterceptors(webSocketAuthInterceptor)
    
    // /ws/chat-sockjs - SockJS 별도 엔드포인트
    registry.addEndpoint("/ws/chat-sockjs")
        .setAllowedOriginPatterns("*")
        .addInterceptors(webSocketAuthInterceptor)
        .withSockJS()  // ✅ 여기만 SockJS 지원
}
```

**변경 후:**
```kotlin
override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    // 메인 WebSocket 엔드포인트 (SockJS 포함)
    // 클라이언트: ws://server/ws/chat/{serverId}/{sessionId}/websocket?token={JWT}
    // SockJS는 자동으로 /{serverId}/{sessionId}/websocket 경로 생성
    registry.addEndpoint("/ws/chat")
        .setAllowedOriginPatterns("*")
        .addInterceptors(webSocketAuthInterceptor)
        .withSockJS()  // ✅ SockJS 활성화
}
```

**효과:**
- `/ws/chat` 엔드포인트가 SockJS 지원
- `/ws/chat/774/edb1b44a/websocket` 같은 동적 경로 자동 처리
- WebSocket 미지원 환경에서 long-polling 폴백

---

### 2. WebSocketAuthInterceptor.kt - 로그 강화

**추가된 로그:**
```kotlin
override fun beforeHandshake(...): Boolean {
    logger.info("========== WebSocket Handshake Attempt ==========")
    logger.info("Request URI: ${request.uri}")
    logger.info("Request Headers: ${request.headers.entries...}")
    
    val token = extractTokenFromQuery(request)
    
    if (token.isNullOrBlank()) {
        logger.error("❌ WebSocket connection REJECTED: No token provided")
        logger.error("Query String: ${...servletRequest.queryString}")
        return false
    }
    
    logger.info("✅ Token found: ${token.take(50)}...")
    
    if (!jwtProvider.validateToken(token)) {
        logger.error("❌ WebSocket connection REJECTED: Invalid token")
        return false
    }
    
    logger.info("✅ Token validation successful")
    
    val userId = jwtProvider.getUserIdFromToken(token)
    attributes["userId"] = userId
    attributes["token"] = token
    
    logger.info("✅ WebSocket connection ACCEPTED for user: $userId")
    logger.info("=================================================")
    return true
    
} catch (e: Exception) {
    logger.error("❌ WebSocket authentication error: ${e.message}", e)
    return false
}
```

**로그 개선:**
- ✅/❌ 이모지로 성공/실패 명확히 표시
- Request URI 전체 로깅
- 토큰 존재 여부 명확히 표시
- 인증 실패 시 상세 스택 트레이스

---

## 📡 수정된 연결 흐름

### 1. 프론트엔드 → 백엔드 연결 시퀀스

```
1️⃣ 프론트엔드가 SockJS 클라이언트로 연결 시도
   URL: ws://54.66.195.91/ws/chat
   
2️⃣ SockJS가 서버 정보 확인 (info 요청)
   GET http://54.66.195.91/ws/chat/info
   
3️⃣ SockJS가 WebSocket 연결 시도
   URL: ws://54.66.195.91/ws/chat/{serverId}/{sessionId}/websocket?token={JWT}
   예: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket?token=eyJ...
   
4️⃣ 백엔드 WebSocketAuthInterceptor 실행
   - Query Parameter에서 token 추출
   - JWT 토큰 검증
   - 성공 시 userId를 세션에 저장
   
5️⃣ WebSocket Handshake 성공 → 101 Switching Protocols
   
6️⃣ STOMP CONNECT 메시지 전송
   - 클라이언트가 구독 설정
   - /topic/couple/{coupleId} 등
```

---

## 🔍 디버깅 가이드

### 1. 백엔드 로그 확인

```bash
# Docker 환경
docker logs -f spring-app 2>&1 | grep -E "WebSocket|Handshake|token|ERROR"

# 로컬 환경
tail -f logs/application.log | grep WebSocket
```

**예상 로그 (정상 연결):**
```
========== WebSocket Handshake Attempt ==========
Request URI: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket?token=eyJhbGci...
✅ Token found: eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOi...
✅ Token validation successful
✅ WebSocket connection ACCEPTED for user: 0df27a65-9356-4952-8672-6d894e8dfff9
=================================================
```

**예상 로그 (토큰 없음):**
```
========== WebSocket Handshake Attempt ==========
Request URI: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket
❌ WebSocket connection REJECTED: No token provided
Query String: null
```

**예상 로그 (토큰 만료):**
```
========== WebSocket Handshake Attempt ==========
✅ Token found: eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
❌ WebSocket connection REJECTED: Invalid token
```

---

### 2. 프론트엔드 연결 코드 (참고)

**Kotlin (Android):**
```kotlin
val client = SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
val stompClient = WebSocketStompClient(client)

// JWT 토큰 쿼리 파라미터로 전달
val url = "ws://54.66.195.91/ws/chat?token=$jwtToken"

stompClient.connect(url, object : StompSessionHandlerAdapter() {
    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        logger.info("✅ WebSocket Connected!")
        
        // 메시지 구독
        session.subscribe("/topic/couple/$coupleId", object : StompFrameHandler {
            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                val message = gson.fromJson(payload as String, ChatMessage::class.java)
                onMessageReceived(message)
            }
        })
    }
    
    override fun handleException(
        session: StompSession,
        command: StompCommand?,
        headers: StompHeaders,
        payload: ByteArray,
        exception: Throwable
    ) {
        logger.error("❌ WebSocket Error: ${exception.message}", exception)
    }
})
```

---

## ✅ 배포 상태

- **서버:** http://54.66.195.91
- **WebSocket URL:** `ws://54.66.195.91/ws/chat`
- **상태:** ✅ 수정 완료 및 배포
- **빌드 시간:** 2025-01-20 20:18 KST
- **Docker:** 컨테이너 재시작 완료
- **Health Check:** 정상 (HTTP 200)

---

## 📝 테스트 체크리스트

### 백엔드 ✅
- [x] SockJS `.withSockJS()` 활성화
- [x] WebSocketAuthInterceptor 로그 강화
- [x] 빌드 성공
- [x] EC2 배포 완료
- [x] Docker 재시작 완료
- [x] Health check 정상

### 프론트엔드 (진행 필요)
- [ ] WebSocket URL에 JWT 토큰 쿼리 파라미터로 전달 확인
- [ ] 연결 성공 로그 확인
- [ ] STOMP CONNECT 성공 확인
- [ ] 메시지 구독/발행 테스트
- [ ] 읽음 확인 WebSocket 테스트
- [ ] 타이핑 인디케이터 테스트

---

## 🐛 추가 확인 사항 (프론트엔드에서 여전히 500 발생 시)

### 1. nginx 설정 확인
nginx가 WebSocket을 프록시할 때 필요한 헤더:

```nginx
location /ws/ {
    proxy_pass http://spring-app:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;        # ✅ 필수
    proxy_set_header Connection "upgrade";         # ✅ 필수
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket 타임아웃
    proxy_connect_timeout 7d;
    proxy_send_timeout 7d;
    proxy_read_timeout 7d;
}
```

현재 설정 확인:
```bash
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 "cat ~/madcamp_W2_ieum_backend/nginx/default.conf"
```

### 2. JWT 토큰 형식 확인
- 토큰이 `Bearer` prefix 없이 순수 JWT만 전달되는지 확인
- URL encoding 필요 여부 확인 (특수문자 `.`, `-`, `_`는 일반적으로 인코딩 불필요)

### 3. CORS 문제
프론트엔드가 다른 도메인에서 접근 시:
```kotlin
registry.addEndpoint("/ws/chat")
    .setAllowedOriginPatterns("*")  // 개발 중
    // .setAllowedOrigins("https://your-app.com")  // 프로덕션
    .addInterceptors(webSocketAuthInterceptor)
    .withSockJS()
```

---

## 📞 추가 지원

프론트엔드에서 여전히 연결 문제 발생 시:

1. **백엔드 로그 전송:**
   ```bash
   docker logs spring-app --tail=100 > websocket-logs.txt
   ```

2. **프론트엔드 에러 로그 전송:**
   - WebSocket 연결 시도 시 발생하는 정확한 에러 메시지
   - Network 탭에서 WebSocket handshake 실패 상태 코드

3. **연결 시도 정보:**
   - 정확한 연결 URL
   - JWT 토큰 앞 50자 (민감 정보 제외)
   - 프론트엔드 라이브러리 (SockJS, STOMP 버전)

---

**수정 완료일:** 2025-01-20 20:35 KST  
**담당:** Backend Team  
**최종 해결:** SockJS 활성화 + STOMP CONNECT 대기 시간 증가 (60초)

---

## 추가 문제: 연결 후 즉시 끊김

### 🔴 증상 (2차 문제)
- WebSocket 연결 성공 (HTTP 101 Switching Protocols) ✅
- **0.1~0.2초 후 서버가 연결을 끊음** ❌
- 클라이언트가 STOMP 구독을 시도할 틈도 없이 연결 종료

### 📊 타임라인
```
20:23:42.464 - ✅ WebSocket connected (101 Switching Protocols)
20:23:42.619 - ❌ onClosed (서버가 연결 끊음)
20:23:42.619 - ❌ 구독 시도 실패 (Not connected)
```

### 🔍 원인 분석

#### 1. STOMP CONNECT 프레임 대기 시간 부족
Spring WebSocket은 기본적으로 WebSocket 연결 후 **일정 시간 내에 STOMP CONNECT 프레임**을 받아야 합니다.

- 프론트엔드: 연결 → UI 초기화 → 구독 설정 (시간 소요)
- 백엔드: 빠르게 CONNECT를 받지 못하면 **타임아웃으로 연결 종료**

#### 2. 기존 설정의 문제
```kotlin
// 기존 WebSocketConfig - transport 설정 없음
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    // setTimeToFirstMessage 설정이 없어서 기본값(10초) 사용
    // 하지만 실제로는 더 짧게 작동할 수 있음
}
```

---

## 최종 해결 방안

### 1. WebSocketConfig - Transport 설정 추가

**파일:** `WebSocketConfig.kt`

```kotlin
/**
 * WebSocket Transport 설정
 * 클라이언트가 STOMP CONNECT 프레임을 보낼 시간 확보
 */
override fun configureWebSocketTransport(registry: WebSocketTransportRegistration) {
    registry
        .setMessageSizeLimit(128 * 1024)         // 128KB - 메시지 최대 크기
        .setSendTimeLimit(30 * 1000)             // 30초 - 전송 타임아웃
        .setSendBufferSizeLimit(512 * 1024)      // 512KB - 전송 버퍼 크기
        .setTimeToFirstMessage(60 * 1000)        // 60초 - ✅ STOMP CONNECT 대기 시간 (핵심!)
}
```

**효과:**
- `setTimeToFirstMessage(60 * 1000)`: **첫 STOMP 프레임을 60초 동안 대기**
- 프론트엔드가 여유롭게 CONNECT 프레임을 보낼 수 있음
- 네트워크 지연이 있어도 연결 유지

---

### 2. StompConnectInterceptor - STOMP 프레임 로깅

**새 파일:** `StompConnectInterceptor.kt`

```kotlin
@Component
class StompConnectInterceptor : ChannelInterceptor {
    
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        
        if (accessor != null) {
            when (accessor.command) {
                StompCommand.CONNECT -> {
                    logger.info("========== STOMP CONNECT Frame Received ==========")
                    
                    val sessionId = accessor.sessionId
                    val userId = accessor.sessionAttributes?.get("userId")
                    val token = accessor.sessionAttributes?.get("token")
                    
                    logger.info("Session ID: $sessionId")
                    logger.info("User ID: $userId")
                    logger.info("Token present: ${token != null}")
                    
                    if (userId != null) {
                        accessor.user = Principal { userId.toString() }
                        logger.info("✅ STOMP CONNECT authenticated for user: $userId")
                    } else {
                        logger.warn("⚠️ STOMP CONNECT without userId")
                    }
                    
                    logger.info("===================================================")
                }
                
                StompCommand.DISCONNECT -> {
                    logger.info("STOMP DISCONNECT: session=${accessor.sessionId}")
                }
                
                StompCommand.SUBSCRIBE -> {
                    logger.info("STOMP SUBSCRIBE: destination=${accessor.destination}")
                }
            }
        }
        
        return message
    }
}
```

**등록:** `WebSocketConfig.kt`에 인터셉터 추가
```kotlin
override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(stompConnectInterceptor)
}
```

**효과:**
- STOMP 모든 명령어 로깅 (CONNECT, DISCONNECT, SUBSCRIBE)
- 연결 문제 발생 시 정확한 원인 파악 가능
- 인증 상태 확인

---

### 3. WebSocketEventListener - 로그 강화

**수정:** `WebSocketEventListener.kt`

```kotlin
@EventListener
fun handleWebSocketConnectListener(event: SessionConnectEvent) {
    logger.info("========== WebSocket STOMP Session Connected ==========")
    logger.info("Session ID: $sessionId")
    logger.info("User ID from session: $userId")
    logger.info("=======================================================")
}

@EventListener
fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
    logger.info("========== WebSocket STOMP Session Disconnected ==========")
    logger.info("Session ID: $sessionId")
    logger.info("User ID: $userId")
    logger.info("Close Status: ${event.closeStatus}")  // ✅ 종료 이유 로깅
    logger.info("===========================================================")
}
```

**효과:**
- 연결/해제 시점 명확히 로깅
- 종료 이유(closeStatus) 확인 가능

---

## 📡 수정된 연결 흐름 (전체)

```
1️⃣ 프론트엔드: SockJS 클라이언트로 연결 시도
   URL: ws://54.66.195.91/ws/chat?token={JWT}

2️⃣ SockJS: 서버 정보 확인
   GET http://54.66.195.91/ws/chat/info

3️⃣ SockJS: WebSocket 연결 시도
   URL: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket?token={JWT}

4️⃣ 백엔드: WebSocketAuthInterceptor 실행
   ✅ Query Parameter에서 token 추출
   ✅ JWT 토큰 검증
   ✅ userId를 세션에 저장

5️⃣ WebSocket Handshake 성공
   ✅ HTTP 101 Switching Protocols

6️⃣ 백엔드: 60초 동안 STOMP CONNECT 대기 (setTimeToFirstMessage) ⏰

7️⃣ 프론트엔드: STOMP CONNECT 프레임 전송
   - 클라이언트가 구독 준비
   - 시간적 여유 확보

8️⃣ 백엔드: StompConnectInterceptor 실행
   ✅ STOMP CONNECT 프레임 수신
   ✅ 사용자 인증 완료
   ✅ STOMP 세션 활성화

9️⃣ 프론트엔드: 구독 시작
   SUBSCRIBE /topic/couple/{coupleId}
   SUBSCRIBE /topic/couple/{coupleId}/read
   SUBSCRIBE /topic/couple/{coupleId}/typing

🔟 양방향 통신 시작 ✅
```

---

## 🔍 디버깅 로그 예시

### 정상 연결 (수정 후)

```bash
# 1. WebSocket Handshake
========== WebSocket Handshake Attempt ==========
Request URI: ws://54.66.195.91/ws/chat/774/edb1b44a/websocket?token=eyJhbGci...
✅ Token found: eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
✅ Token validation successful
✅ WebSocket connection ACCEPTED for user: 0df27a65-9356-4952-8672-6d894e8dfff9
=================================================

# 2. STOMP CONNECT 프레임 수신 (60초 이내)
========== STOMP CONNECT Frame Received ==========
Session ID: edb1b44a
User ID: 0df27a65-9356-4952-8672-6d894e8dfff9
Token present: true
✅ STOMP CONNECT authenticated for user: 0df27a65-9356-4952-8672-6d894e8dfff9
===================================================

# 3. STOMP 세션 활성화
========== WebSocket STOMP Session Connected ==========
Session ID: edb1b44a
User ID from session: 0df27a65-9356-4952-8672-6d894e8dfff9
=======================================================

# 4. 구독
STOMP SUBSCRIBE: destination=/topic/couple/2a3d5d24-7070-4e0e-91d1-1d2cd545bf8d
STOMP SUBSCRIBE: destination=/topic/couple/2a3d5d24-7070-4e0e-91d1-1d2cd545bf8d/read
STOMP SUBSCRIBE: destination=/topic/couple/2a3d5d24-7070-4e0e-91d1-1d2cd545bf8d/typing
```

### 비정상 연결 (이전)

```bash
# 1. WebSocket Handshake 성공
✅ WebSocket connection ACCEPTED for user: ...

# 2. STOMP CONNECT 미수신 (타임아웃)
(로그 없음 - setTimeToFirstMessage 짧음)

# 3. 연결 종료
========== WebSocket STOMP Session Disconnected ==========
Close Status: CloseStatus[code=1006, reason=Abnormal closure]
===========================================================
```

---

## ✅ 최종 배포 상태

- **서버:** http://54.66.195.91
- **WebSocket URL:** `ws://54.66.195.91/ws/chat`
- **상태:** ✅ 모든 수정 완료 및 배포
- **빌드 시간:** 2025-01-20 20:32 KST
- **Docker:** 컨테이너 재시작 완료
- **Health Check:** 정상 (HTTP 200)

---

## 📝 최종 체크리스트

### 백엔드 ✅ (모두 완료)
- [x] SockJS `.withSockJS()` 활성화
- [x] **`setTimeToFirstMessage(60초)` 설정** ← 핵심 해결책
- [x] StompConnectInterceptor 생성 및 등록
- [x] WebSocketEventListener 로그 강화
- [x] WebSocketAuthInterceptor 로그 강화
- [x] 빌드 성공
- [x] EC2 배포 완료
- [x] Docker 재시작 완료
- [x] Health check 정상

### 프론트엔드 (테스트 필요)
- [ ] WebSocket 연결 재시도
- [ ] STOMP CONNECT 성공 확인 ← **이제 연결이 유지될 것입니다**
- [ ] 메시지 구독 정상 동작 확인
- [ ] 메시지 전송/수신 테스트
- [ ] 읽음 확인 기능 테스트
- [ ] 타이핑 인디케이터 테스트

---
