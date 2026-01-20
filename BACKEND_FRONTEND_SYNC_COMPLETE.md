# 백엔드 프론트엔드 연동 수정 완료 ✅

## 📅 수정일: 2026-01-20

---

## ✅ 완료된 수정사항

### 1. MessageType Enum 확장

**파일:** `MessageType.kt`

**변경 전:**
```kotlin
enum class MessageType {
    TEXT, IMAGE, STICKER
}
```

**변경 후:**
```kotlin
enum class MessageType {
    TEXT,
    IMAGE,
    STICKER,
    SHARED_SCHEDULE,    // 일정 공유
    SHARED_PLACE,       // 장소 공유
    SHARED_BUCKET       // 버킷리스트 공유
}
```

**영향:** 프론트엔드에서 일정, 장소, 버킷리스트를 채팅으로 공유 가능

---

### 2. WebSocket 응답 타입 String 변환

**파일:** `WebSocketMessageResponse.kt`

**변경 사항:**
- `id`: `UUID` → `String`
- `senderId`: `UUID` → `String`
- `readAt`: `LocalDateTime?` → `String?` (ISO-8601)
- `createdAt`: `LocalDateTime` → `String` (ISO-8601)

**이유:** 프론트엔드(Android)에서 UUID와 날짜 파싱을 단순화

**예시 응답:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "senderId": "770e8400-e29b-41d4-a716-446655440001",
  "senderName": "홍길동",
  "content": "안녕하세요",
  "type": "TEXT",
  "isRead": false,
  "readAt": null,
  "createdAt": "2026-01-20T09:55:00"
}
```

---

### 3. 읽음 확인 별도 토픽

**파일:** `ChatWebSocketController.kt`

**변경 전:**
```kotlin
@MessageMapping("/chat/{coupleId}/read")
@SendTo("/topic/couple/{coupleId}")  // 일반 메시지와 동일
```

**변경 후:**
```kotlin
@MessageMapping("/chat/{coupleId}/read")
@SendTo("/topic/couple/{coupleId}/read")  // 별도 토픽
```

**장점:**
- 읽음 확인과 일반 메시지 분리
- 프론트엔드에서 구독 관리 용이

**프론트엔드 구독:**
```kotlin
// 일반 메시지
stompClient.topic("/topic/couple/$coupleId").subscribe { ... }

// 읽음 확인
stompClient.topic("/topic/couple/$coupleId/read").subscribe { ... }
```

---

### 4. 타이핑 인디케이터 개선

**파일:** `ChatWebSocketController.kt`, `TypingRequest.kt`, `TypingIndicatorResponse.kt`

**변경 전:**
```kotlin
@MessageMapping("/chat/{coupleId}/typing")
@SendTo("/topic/couple/{coupleId}")
fun handleTyping(...): SystemMessage?
```

**변경 후:**
```kotlin
@MessageMapping("/chat/{coupleId}/typing")
@SendTo("/topic/couple/{coupleId}/typing")  // 별도 토픽
fun handleTyping(
    @Payload request: TypingRequest  // 페이로드 추가
): TypingIndicatorResponse
```

**새 DTO:**
```kotlin
// 클라이언트 → 서버
data class TypingRequest(
    val isTyping: Boolean
)

// 서버 → 클라이언트
data class TypingIndicatorResponse(
    val userId: String,
    val isTyping: Boolean
)
```

**프론트엔드 사용:**
```kotlin
// 전송
val request = TypingRequest(isTyping = true)
stompClient.send("/app/chat/$coupleId/typing", gson.toJson(request))

// 수신
stompClient.topic("/topic/couple/$coupleId/typing").subscribe { message ->
    val response = gson.fromJson(message.payload, TypingIndicatorResponse::class.java)
    if (response.isTyping) {
        showTypingIndicator(response.userId)
    }
}
```

---

### 5. ReadReceiptMessage String 변환

**파일:** `ReadReceiptMessage.kt`

**변경 사항:**
- `messageIds`: `List<UUID>` → `List<String>`
- `readAt`: `LocalDateTime` → `String` (ISO-8601)
- `create()` companion 함수 추가

**응답 예시:**
```json
{
  "type": "READ_RECEIPT",
  "messageIds": [
    "550e8400-e29b-41d4-a716-446655440000",
    "660e8400-e29b-41d4-a716-446655440001"
  ],
  "readAt": "2026-01-20T09:55:30"
}
```

---

### 6. 메시지 유효성 검증 확장

**파일:** `ChatWebSocketService.kt`

**추가된 검증:**
```kotlin
MessageType.SHARED_SCHEDULE,
MessageType.SHARED_PLACE,
MessageType.SHARED_BUCKET -> {
    if (request.content.isNullOrBlank()) {
        throw IllegalArgumentException("공유 컨텐츠는 내용이 필요합니다 (JSON 형식)")
    }
}
```

**공유 컨텐츠 형식:**
```json
{
  "type": "SHARED_SCHEDULE",
  "content": "{\"title\":\"데이트\",\"date\":\"2025-01-20\",\"location\":\"강남\"}",
  "tempId": "uuid"
}
```

---

## 📡 업데이트된 API 엔드포인트

### WebSocket 구독

| 토픽 | 설명 | 응답 타입 |
|------|------|----------|
| `/topic/couple/{coupleId}` | 일반 채팅 메시지 | `WebSocketMessageResponse` |
| `/topic/couple/{coupleId}/read` | 읽음 확인 | `ReadReceiptMessage` |
| `/topic/couple/{coupleId}/typing` | 타이핑 인디케이터 | `TypingIndicatorResponse` |

### WebSocket 전송

| 목적지 | 페이로드 | 설명 |
|--------|----------|------|
| `/app/chat/{coupleId}` | `WebSocketMessageRequest` | 메시지 전송 |
| `/app/chat/{coupleId}/read` | `List<String>` (messageIds) | 읽음 처리 |
| `/app/chat/{coupleId}/typing` | `TypingRequest` | 타이핑 알림 |

---

## 🔄 마이그레이션 가이드 (프론트엔드)

### 1. 타입 변경 적용

**변경 전:**
```kotlin
data class ChatMessage(
    val id: UUID,
    val senderId: UUID,
    // ...
)
```

**변경 후:**
```kotlin
data class ChatMessage(
    val id: String,
    val senderId: String,
    val readAt: String?,
    val createdAt: String,
    // ...
)
```

### 2. 구독 분리

**변경 전:**
```kotlin
stompClient.topic("/topic/couple/$coupleId").subscribe { message ->
    // 모든 메시지 타입 처리
}
```

**변경 후:**
```kotlin
// 일반 메시지
stompClient.topic("/topic/couple/$coupleId").subscribe { message ->
    val chatMessage = gson.fromJson(message.payload, ChatMessage::class.java)
    handleMessage(chatMessage)
}

// 읽음 확인
stompClient.topic("/topic/couple/$coupleId/read").subscribe { message ->
    val receipt = gson.fromJson(message.payload, ReadReceipt::class.java)
    updateReadStatus(receipt.messageIds)
}

// 타이핑 인디케이터
stompClient.topic("/topic/couple/$coupleId/typing").subscribe { message ->
    val typing = gson.fromJson(message.payload, TypingIndicatorResponse::class.java)
    showTypingIndicator(typing.userId, typing.isTyping)
}
```

### 3. MessageType 추가

**Kotlin Enum:**
```kotlin
enum class MessageType {
    TEXT,
    IMAGE,
    STICKER,
    SHARED_SCHEDULE,    // ✨ 추가
    SHARED_PLACE,       // ✨ 추가
    SHARED_BUCKET       // ✨ 추가
}
```

---

## ✅ 배포 상태

- **서버:** http://54.66.195.91
- **상태:** ✅ 배포 완료 및 정상 작동
- **WebSocket:** `ws://54.66.195.91/ws/chat`
- **빌드:** 성공 (2026-01-20 18:55)
- **Docker:** 컨테이너 실행 중

---

## 📝 테스트 체크리스트

### 백엔드 (완료 ✅)
- [x] MessageType enum SHARED_* 타입 추가
- [x] WebSocket 응답 String 변환
- [x] 읽음 확인 별도 토픽
- [x] 타이핑 인디케이터 별도 토픽
- [x] DTO 생성 (TypingRequest, TypingIndicatorResponse)
- [x] 빌드 성공
- [x] EC2 배포 완료
- [x] 서버 정상 작동 확인

### 프론트엔드 (진행 필요)
- [ ] ChatMessage 모델 String 타입 적용
- [ ] 3개 토픽 별도 구독 구현
- [ ] MessageType enum 확장
- [ ] 타이핑 인디케이터 TypingRequest 사용
- [ ] 공유 컨텐츠 (일정/장소/버킷) 전송 구현
- [ ] 실제 WebSocket 연결 테스트

---

## 🐛 알려진 이슈

### None ✅

현재 모든 수정사항이 정상적으로 작동하고 있습니다.

---

## 📞 문의

프론트엔드 구현 시 문제가 있으면:
1. [FRONTEND_WEBSOCKET_GUIDE.md](FRONTEND_WEBSOCKET_GUIDE.md) 참조
2. [API_SPECIFICATION.md](API_SPECIFICATION.md) 확인
3. 백엔드 팀에 문의

---

**수정 완료일:** 2026-01-20 18:55 KST  
**배포자:** Backend Team  
**버전:** v1.1.0
