# WebSocket 채팅 구현 완료! 🎉

## ✅ 구현 완료 사항

### Backend (Spring Boot + STOMP)

1. **의존성 추가** ✅
   - `spring-boot-starter-websocket` 추가
   
2. **WebSocket 설정** ✅
   - [WebSocketConfig.kt](src/main/kotlin/com/ieum/ieum_back/config/WebSocketConfig.kt) - STOMP 엔드포인트 및 메시지 브로커 설정
   - [WebSocketAuthInterceptor.kt](src/main/kotlin/com/ieum/ieum_back/config/WebSocketAuthInterceptor.kt) - JWT 인증 인터셉터
   - [WebSocketEventListener.kt](src/main/kotlin/com/ieum/ieum_back/config/WebSocketEventListener.kt) - 연결/해제 이벤트 핸들러

3. **Controller** ✅
   - [ChatWebSocketController.kt](src/main/kotlin/com/ieum/ieum_back/chat/controller/ChatWebSocketController.kt)
   - 메시지 송수신, 읽음 처리, 타이핑 인디케이터

4. **Service** ✅
   - [ChatWebSocketService.kt](src/main/kotlin/com/ieum/ieum_back/chat/service/ChatWebSocketService.kt)
   - 메시지 저장, 권한 검증, 읽음 처리

5. **DTO** ✅
   - [WebSocketMessageRequest.kt](src/main/kotlin/com/ieum/ieum_back/chat/dto/WebSocketMessageRequest.kt)
   - [WebSocketMessageResponse.kt](src/main/kotlin/com/ieum/ieum_back/chat/dto/WebSocketMessageResponse.kt)
   - [ReadReceiptMessage.kt](src/main/kotlin/com/ieum/ieum_back/chat/dto/ReadReceiptMessage.kt)
   - [SystemMessage.kt](src/main/kotlin/com/ieum/ieum_back/chat/dto/SystemMessage.kt)
   - [WebSocketErrorResponse.kt](src/main/kotlin/com/ieum/ieum_back/chat/dto/WebSocketErrorResponse.kt)

6. **nginx 설정** ✅
   - [default.conf](nginx/default.conf) - WebSocket 프록시 설정 추가

---

## 📡 API 엔드포인트

### WebSocket 연결
```
ws://localhost:8080/ws/chat?token={JWT_TOKEN}
```

### STOMP 목적지

| 동작 | 엔드포인트 | 설명 |
|------|------------|------|
| **구독** | `/topic/couple/{coupleId}` | 메시지 수신 |
| **메시지 전송** | `/app/chat/{coupleId}` | 메시지 전송 |
| **읽음 처리** | `/app/chat/{coupleId}/read` | 읽음 상태 업데이트 |
| **타이핑** | `/app/chat/{coupleId}/typing` | 타이핑 인디케이터 |

---

## 🚀 시작하기

### 1. 의존성 설치
```bash
./gradlew build
```

### 2. 서버 실행
```bash
./gradlew bootRun
```

### 3. Docker로 실행
```bash
docker-compose up --build
```

### 4. WebSocket 테스트

브라우저 콘솔에서:
```javascript
const socket = new WebSocket('ws://localhost:8080/ws/chat?token=YOUR_JWT_TOKEN');

socket.onopen = () => {
    console.log('Connected');
    
    // STOMP CONNECT
    socket.send('CONNECT\naccept-version:1.1,1.2\n\n\x00');
};

socket.onmessage = (event) => {
    console.log('Received:', event.data);
};
```

---

## 📱 프론트엔드 연동

### Android Kotlin 클라이언트

자세한 구현 가이드: **[FRONTEND_WEBSOCKET_GUIDE.md](FRONTEND_WEBSOCKET_GUIDE.md)**

#### 의존성 추가
```kotlin
implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.google.code.gson:gson:2.10.1")
```

#### 간단한 예시
```kotlin
// 연결
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "ws://YOUR_SERVER_IP:8080/ws/chat?token=$jwtToken"
)
stompClient.connect()

// 구독
stompClient.topic("/topic/couple/$coupleId").subscribe { message ->
    val chatMessage = gson.fromJson(message.payload, ChatMessage::class.java)
    onMessageReceived(chatMessage)
}

// 메시지 전송
val request = mapOf(
    "type" to "TEXT",
    "content" to "Hello!"
)
stompClient.send("/app/chat/$coupleId", gson.toJson(request)).subscribe()
```

---

## 📚 문서

- **[WEBSOCKET_IMPLEMENTATION_PLAN.md](WEBSOCKET_IMPLEMENTATION_PLAN.md)** - 구현 계획서
- **[FRONTEND_WEBSOCKET_GUIDE.md](FRONTEND_WEBSOCKET_GUIDE.md)** - 프론트엔드 연동 가이드
- **[API_SPECIFICATION.md](API_SPECIFICATION.md)** - API 명세서 (WebSocket 섹션 추가)

---

## 🔐 보안

### Phase 1 (현재) ✅
- ✅ WSS/TLS 암호화 (프로덕션)
- ✅ JWT 기반 인증
- ✅ 커플 멤버 권한 검증
- ✅ CORS 설정

### Phase 2 (계획) 🔜
- ⬜ E2EE (End-to-End Encryption)
- ⬜ Signal Protocol 또는 LibSodium
- ⬜ 키 교환 메커니즘

---

## 🧪 테스트

### 1. 연결 테스트
```bash
# JWT 토큰 발급 (로그인)
curl -X POST http://localhost:8080/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken": "YOUR_GOOGLE_ID_TOKEN"}'

# WebSocket 연결 (브라우저 또는 WebSocket 클라이언트)
ws://localhost:8080/ws/chat?token=YOUR_JWT_TOKEN
```

### 2. 메시지 전송 테스트
STOMP 프레임:
```
CONNECT
accept-version:1.1,1.2

SUBSCRIBE
id:sub-0
destination:/topic/couple/YOUR_COUPLE_ID

SEND
destination:/app/chat/YOUR_COUPLE_ID
content-type:application/json

{"type":"TEXT","content":"Hello World!"}
```

---

## 📊 프로젝트 구조

```
src/main/kotlin/com/ieum/ieum_back/
├── config/
│   ├── WebSocketConfig.kt              # WebSocket 설정
│   ├── WebSocketAuthInterceptor.kt     # JWT 인증
│   └── WebSocketEventListener.kt       # 연결/해제 이벤트
├── chat/
│   ├── controller/
│   │   └── ChatWebSocketController.kt  # STOMP 메시지 핸들러
│   ├── service/
│   │   └── ChatWebSocketService.kt     # 비즈니스 로직
│   └── dto/
│       ├── WebSocketMessageRequest.kt
│       ├── WebSocketMessageResponse.kt
│       ├── ReadReceiptMessage.kt
│       ├── SystemMessage.kt
│       └── WebSocketErrorResponse.kt
└── entity/
    └── ChatMessage.kt                  # 메시지 엔티티
```

---

## 🔧 설정

### application.yaml
WebSocket 관련 추가 설정 불필요 (기본 설정 사용)

### nginx (default.conf)
```nginx
location /ws/ {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "Upgrade";
    ...
}
```

---

## 🌟 주요 기능

- ✅ **실시간 양방향 통신** - STOMP over WebSocket
- ✅ **JWT 인증** - Query Parameter 방식
- ✅ **메시지 영구 저장** - PostgreSQL
- ✅ **읽음 처리** - 실시간 읽음 상태 업데이트
- ✅ **타이핑 인디케이터** - 상대방 타이핑 표시
- ✅ **에러 핸들링** - 구조화된 에러 응답
- ✅ **SockJS 폴백** - WebSocket 미지원 환경 대응
- ✅ **nginx 프록시** - 프로덕션 환경 지원

---

## 🐛 트러블슈팅

### 1. 연결 실패
- JWT 토큰 유효성 확인
- 서버 실행 상태 확인
- 포트 번호 확인 (8080)

### 2. 메시지 수신 안됨
- 구독 경로 확인 (`/topic/couple/{coupleId}`)
- 커플 ID 일치 확인
- 로그 확인

### 3. CORS 에러
- `WebSocketConfig`에서 `setAllowedOriginPatterns("*")` 확인
- 프로덕션에서는 특정 도메인으로 제한

---

## 📝 다음 단계

### Phase 2: E2EE 구현
- [ ] Signal Protocol 또는 LibSodium 라이브러리 추가
- [ ] 키 생성 및 교환 API
- [ ] 메시지 암호화/복호화 로직
- [ ] 프론트엔드 E2EE 클라이언트 구현

### 추가 기능
- [ ] 파일 전송 (이미지 외)
- [ ] 음성/영상 메시지
- [ ] 메시지 검색
- [ ] 알림 푸시

---

## 👥 기여

프론트엔드 팀에게:
- **[FRONTEND_WEBSOCKET_GUIDE.md](FRONTEND_WEBSOCKET_GUIDE.md)** 참조하여 구현
- 문제 발생 시 백엔드 팀에 문의

---

**구현 완료일:** 2026-01-20  
**구현자:** Backend Developer  
**Status:** ✅ Phase 1 Complete
