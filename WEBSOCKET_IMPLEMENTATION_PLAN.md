# 이음 앱 웹소켓 채팅 구현 계획서

## 📌 개요

커플 간 실시간 채팅 기능을 위한 WebSocket 통신 구현 계획입니다.

---

## 🏗 기술 스택

### Backend
- **Spring Boot WebSocket**
- **STOMP (Simple Text Oriented Messaging Protocol)** - 메시징 프로토콜
- **SockJS** - WebSocket 폴백 지원
- **Spring Security** - JWT 기반 인증 통합
- **PostgreSQL** - 메시지 영구 저장

### Frontend (Kotlin/Android)
- **OkHttp WebSocket** 또는 **Scarlet** 라이브러리
- **STOMP 클라이언트**

---

## 🔌 WebSocket 아키텍처

```
[Android Client] <---> [nginx] <---> [Spring WebSocket] <---> [PostgreSQL]
                         ↓
                   WebSocket Upgrade
                   + STOMP Protocol
```

### 연결 흐름
1. 클라이언트가 JWT 토큰으로 WebSocket 연결 요청
2. 서버에서 토큰 검증 후 연결 수락
3. 사용자가 커플 채팅방 구독
4. 실시간 메시지 송수신
5. 메시지를 DB에 저장

---

## 📡 WebSocket 엔드포인트 설계

### 1. WebSocket 연결

**연결 URL:**
```
# 개발 환경 (HTTP)
ws://YOUR_EC2_PUBLIC_IP/ws/chat?token={JWT_TOKEN}

# 프로덕션 환경 (HTTPS + 도메인)
wss://your-domain.com/ws/chat?token={JWT_TOKEN}

# 참고: nginx가 80/443 포트에서 프록시하므로 포트 번호 불필요
```

### 2. STOMP 대상 (Destination)

#### 2.1 구독 (Subscribe)
클라이언트가 메시지를 받기 위해 구독하는 목적지:

```
/topic/couple/{coupleId}
```

**예시:**
```
SUBSCRIBE /topic/couple/660e8400-e29b-41d4-a716-446655440001
```

#### 2.2 발행 (Publish)
클라이언트가 메시지를 보내는 목적지:

```
/app/chat/{coupleId}
```

**예시:**
```
SEND /app/chat/660e8400-e29b-41d4-a716-446655440001
```

---

## 📦 메시지 포맷

### 1. 클라이언트 → 서버 (메시지 전송)

```json
{
  "type": "TEXT",
  "content": "안녕하세요!",
  "imageUrl": null,
  "tempId": "client-temp-id-12345"
}
```

**필드 설명:**
- `type`: `TEXT` | `IMAGE` | `STICKER`
- `content`: 텍스트 내용 (TEXT일 때)
- `imageUrl`: 이미지 URL (IMAGE일 때)
- `tempId`: 클라이언트 임시 ID (전송 상태 추적용, optional)

### 2. 서버 → 클라이언트 (메시지 수신)

```json
{
  "id": "990e8400-e29b-41d4-a716-446655440005",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "senderName": "홍길동",
  "senderProfileImage": "https://...",
  "content": "안녕하세요!",
  "type": "TEXT",
  "imageUrl": null,
  "isRead": false,
  "readAt": null,
  "createdAt": "2024-01-11T10:10:00",
  "tempId": "client-temp-id-12345"
}
```

### 3. 읽음 상태 업데이트

```json
{
  "type": "READ_RECEIPT",
  "messageIds": [
    "990e8400-e29b-41d4-a716-446655440005",
    "990e8400-e29b-41d4-a716-446655440006"
  ],
  "readAt": "2024-01-11T10:15:00"
}
```

### 4. 시스템 메시지

```json
{
  "type": "SYSTEM",
  "event": "USER_CONNECTED" | "USER_DISCONNECTED" | "TYPING",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-11T10:10:00"
}
```

---

## 🔐 인증 및 보안

### 1. WebSocket Handshake 인증

**방법:** Query Parameter로 JWT 토큰 전달
```
ws://localhost:8080/ws/chat?token={JWT_TOKEN}
```

**처리 과정:**
1. WebSocket Handshake 시 토큰 추출
2. JWT 토큰 검증
3. 유효한 경우 WebSocket 연결 허용
4. 세션에 사용자 정보 저장

### 2. 메시지 전송 권한 검증

- 사용자가 해당 커플의 멤버인지 확인
- 자신의 커플 채팅방에만 메시지 전송 가능

### 3. CORS 설정

```kotlin
registry.addEndpoint("/ws/chat")
    .setAllowedOriginPatterns("*")
    .withSockJS()
```

---

## 🔄 구현 상세 플로우

### 1. 연결 흐름

```
1. 클라이언트: WebSocket 연결 요청 (JWT 토큰 포함)   ws://YOUR_EC2_IP:8080/ws/chat?token={JWT}  (개발)
   wss://your-domain.com/ws/chat?token={JWT}  (프로덕션 - SSL)   ↓
2. 서버: HandshakeInterceptor에서 토큰 검증
   ↓
3. 서버: 연결 성공, 세션 저장
   ↓
4. 클라이언트: STOMP CONNECT 요청
   ↓
5. 서버: STOMP CONNECTED 응답
   ↓
6. 클라이언트: /topic/couple/{coupleId} 구독
   ↓
7. 서버: SUBSCRIBED 응답
```

### 2. 메시지 전송 흐름

```
1. 클라이언트 A: /app/chat/{coupleId}로 메시지 전송
   ↓
2. 서버: @MessageMapping 핸들러에서 수신
   ↓
3. 서버: 메시지 유효성 검증 및 DB 저장
   ↓
4. 서버: /topic/couple/{coupleId}로 브로드캐스트
   ↓
5. 클라이언트 A, B: 메시지 수신
```

### 3. 읽음 처리 흐름

```
1. 클라이언트 B: 메시지 읽음
   ↓
2. 클라이언트 B: /app/chat/{coupleId}/read 전송
   ↓
3. 서버: DB 업데이트 (isRead = true, readAt)
   ↓
4. 서버: READ_RECEIPT 브로드캐스트
   ↓
5. 클라이언트 A: 읽음 상태 업데이트
```

---

## 🗂 데이터베이스 스키마 (기존 활용)

```sql
-- messages 테이블 (기존)
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    couple_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT,
    type VARCHAR(20) NOT NULL,  -- TEXT, IMAGE, STICKER
    image_url TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (couple_id) REFERENCES couples(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- 인덱스
CREATE INDEX idx_messages_couple_created ON messages(couple_id, created_at DESC);
CREATE INDEX idx_messages_unread ON messages(couple_id, is_read, created_at);
```

---

## 📂 구현할 파일 구조

```
src/main/kotlin/com/ieum/ieum_back/
├── config/
│   ├── WebSocketConfig.kt              # WebSocket 설정
│   ├── WebSocketSecurityConfig.kt      # WebSocket 보안 설정
│   └── WebSocketAuthInterceptor.kt     # JWT 인증 인터셉터
├── controller/
│   └── ChatWebSocketController.kt      # STOMP 메시지 핸들러
├── service/
│   ├── ChatWebSocketService.kt         # WebSocket 비즈니스 로직
│   └── MessageService.kt               # 메시지 DB 처리 (기존 활용)
├── dto/
│   ├── WebSocketMessageRequest.kt      # 클라이언트 → 서버 DTO
│   ├── WebSocketMessageResponse.kt     # 서버 → 클라이언트 DTO
│   └── ReadReceiptMessage.kt           # 읽음 처리 DTO
└── domain/
    └── Message.kt                      # 메시지 엔티티 (기존)
```

---

## 🚀 구현 단계

### Phase 1: 기본 WebSocket 연결
- [ ] WebSocketConfig 구성
- [ ] JWT 인증 인터셉터 구현
- [ ] 연결/연결 해제 이벤트 핸들링

### Phase 2: 메시지 송수신
- [ ] ChatWebSocketController 구현
- [ ] 메시지 전송/수신 핸들러
- [ ] DB 저장 로직 연동
- [ ] 에러 핸들링

### Phase 3: 읽음 처리
- [ ] 읽음 상태 업데이트
- [ ] READ_RECEIPT 브로드캐스트

### Phase 4: 추가 기능
- [ ] 타이핑 인디케이터 (optional)
- [ ] 온라인 상태 표시 (optional)
- [ ] 재연결 로직

### Phase 5: 테스트 및 최적화
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 성능 최적화
- [ ] nginx WebSocket 프록시 설정

---

## 🔧 nginx 설정

```nginx
# WebSocket 프록시 설정
location /ws/ {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "Upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # 타임아웃 설정
    proxy_connect_timeout 7d;
    proxy_send_timeout 7d;
    proxy_read_timeout 7d;
}
```

---

## 🔍 에러 핸들링

### 1. 연결 실패
```json
{
  "type": "ERROR",
  "code": "AUTH_FAILED",
  "message": "인증에 실패했습니다."
}
```

### 2. 권한 없음
```json
{
  "type": "ERROR",
  "code": "UNAUTHORIZED",
  "message": "해당 채팅방에 접근 권한이 없습니다."
}
```

### 3. 메시지 전송 실패
```json
{
  "type": "ERROR",
  "code": "SEND_FAILED",
  "message": "메시지 전송에 실패했습니다.",
  "tempId": "client-temp-id-12345"
}
```

---

## 📱 프론트엔드 가이드 (Kotlin/Android)

### 1. 의존성 추가 (build.gradle.kts)
```kotlin
dependencies {
    // OkHttp WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // STOMP
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. WebSocket 연결 코드 예시
```kotlin
class ChatWebSocketClient(private val jwtToken: String) {
    private val client = OkHttpClient()
    private var stompClient: StompClient? = null
    
    fun connect(coupleId: String) {
        val url = "ws://localhost:8080/ws/chat?token=$jwtToken"
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        
        stompClient?.connect()
        
        // 채팅방 구독
        stompClient?.topic("/topic/couple/$coupleId")?.subscribe { message ->
            val chatMessage = Gson().fromJson(message.payload, ChatMessage::class.java)
            onMessageReceived(chatMessage)
        }
    }
    
    fun sendMessage(coupleId: String, content: String, type: String = "TEXT") {
        val message = mapOf(
            "type" to type,
            "content" to content,
            "tempId" to UUID.randomUUID().toString()
        )
        
        stompClient?.send("/app/chat/$coupleId", Gson().toJson(message))?.subscribe()
    }
    
    fun disconnect() {
        stompClient?.disconnect()
    }
}
```

---

## 🧪 테스트 방법

### 1. WebSocket 연결 테스트 (JavaScript)
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

### 2. Postman/Insomnia
- WebSocket 요청 생성
- URL: `ws://localhost:8080/ws/chat?token={JWT}`
- STOMP 프로토콜 메시지 전송

---

## 📊 모니터링 및 로깅

### 로깅 포인트
1. WebSocket 연결/해제
2. 메시지 송수신
3. 인증 실패
4. 에러 발생

### 메트릭
1. 동시 연결 수
2. 메시지 처리량 (TPS)
3. 평균 응답 시간
4. 에러율

---

## 🔄 REST API와의 관계

### WebSocket 사용
- 실시간 메시지 송수신
- 읽음 상태 업데이트
- 타이핑 인디케이터

### REST API 사용 (기존 유지)
- 과거 메시지 조회 (`GET /chat/rooms/{roomId}/messages`)
- 채팅방 정보 조회 (`GET /chat/room`)
- 파일 업로드 (Presigned URL)

---

## 🎯 예상 이점

1. **실시간성**: 메시지 즉시 전달
2. **효율성**: 폴링 대비 서버 부하 감소
3. **양방향 통신**: 서버 → 클라이언트 푸시 가능
4. **사용자 경험**: 카카오톡과 유사한 실시간 채팅 경험

---

## ⚠️ 주의사항

1. **재연결 로직**: 네트워크 끊김 시 자동 재연결 구현 필요
2. **메시지 순서**: 메시지 순서 보장 (createdAt 기반 정렬)
3. **오프라인 메시지**: WebSocket 미연결 시 REST API 폴백
4. **스케일링**: 추후 Redis Pub/Sub 또는 RabbitMQ 고려
5. **보안**: JWT 토큰 만료 시 재연결 로직

---

## 📝 다음 단계

이 계획서를 검토하신 후:
1. ✅ **승인** → 구현 시작
2. 🔄 **수정 요청** → 계획서 업데이트
3. ❓ **질문** → 추가 설명

---

**작성일:** 2026-01-20  
**작성자:** Backend Developer
