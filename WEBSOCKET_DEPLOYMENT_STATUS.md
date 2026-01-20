# WebSocket 배포 상태 📡

## ✅ 배포 완료 (2026-01-20)

### 서버 정보
- **서버 IP**: `54.66.195.91`
- **상태**: 운영 중 ✅

---

## 🔌 엔드포인트

### 1. 순수 WebSocket 엔드포인트 (권장)

```
ws://54.66.195.91/ws/chat?token={JWT_TOKEN}
```

**특징:**
- ✅ SockJS 프레임 없음 (순수 STOMP over WebSocket)
- ✅ 가장 빠르고 안정적
- ✅ 모던 브라우저 및 Android 앱에서 사용
- ❌ `/info` 엔드포인트 없음 (순수 WebSocket이므로 불필요)

**Android 연결 예시:**
```kotlin
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "ws://54.66.195.91/ws/chat?token=$jwtToken"
)
stompClient.connect()
```

---

### 2. SockJS 폴백 엔드포인트 (구형 환경용)

```
http://54.66.195.91/ws/chat-sockjs?token={JWT_TOKEN}
```

**특징:**
- ✅ SockJS 프레임 지원
- ✅ WebSocket 미지원 환경에서 폴백
- ✅ `/info` 엔드포인트 제공
- ⚠️ 추가 프레임 파싱 필요

**SockJS Info 확인:**
```bash
curl http://54.66.195.91/ws/chat-sockjs/info
# {"websocket":true,"cookie_needed":true,...}
```

---

## 📨 STOMP 메시지 구조

### 구독 (Subscribe)

```
목적지: /topic/chat/{coupleId}
```

**예시:**
```kotlin
stompClient.topic("/topic/chat/$coupleId").subscribe { message ->
    val response = gson.fromJson(message.payload, WebSocketMessageResponse::class.java)
    // 암호문: response.content, response.iv
    // 복호화 필요
}
```

### 전송 (Send)

```
목적지: /app/chat/{coupleId}
```

**메시지 형식:**
```json
{
  "type": "TEXT",
  "content": "base64_encrypted_text",
  "iv": "base64_initialization_vector"
}
```

**예시:**
```kotlin
val encrypted = cryptoManager.encryptAESGCM(plainText, sharedKey)
val request = mapOf(
    "type" to "TEXT",
    "content" to encrypted.cipherText,
    "iv" to encrypted.iv
)
stompClient.send("/app/chat/$coupleId", gson.toJson(request)).subscribe()
```

---

## 🔐 인증

**JWT 토큰 필수**

WebSocket 연결 시 쿼리 파라미터로 JWT 토큰 전달:
```
ws://54.66.195.91/ws/chat?token=eyJhbGciOiJIUzI1NiIs...
```

토큰 없이 연결 시 HTTP 400 또는 403 반환

---

## 🧪 테스트 방법

### 1. SockJS 엔드포인트 확인
```bash
curl http://54.66.195.91/ws/chat-sockjs/info
```

**예상 응답:**
```json
{
  "entropy": 123456789,
  "origins": ["*:*"],
  "cookie_needed": true,
  "websocket": true
}
```

### 2. WebSocket 연결 테스트 (JWT 필요)

Android 앱에서:
```kotlin
val jwtToken = "YOUR_JWT_TOKEN" // 로그인 후 받은 토큰
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "ws://54.66.195.91/ws/chat?token=$jwtToken"
)

stompClient.lifecycle().subscribe { event ->
    when (event.type) {
        LifecycleEvent.Type.OPENED -> {
            Log.d("WebSocket", "✅ 연결 성공!")
        }
        LifecycleEvent.Type.ERROR -> {
            Log.e("WebSocket", "❌ 에러: ${event.exception}")
        }
        else -> {}
    }
}

stompClient.connect()
```

---

## ⚠️ 주의사항

### 1. 순수 WebSocket 사용 시

- ✅ **권장**: `/ws/chat` 엔드포인트 사용
- ❌ **주의**: `/ws/chat/info` 는 존재하지 않음 (에러 아님)
- ✅ SockJS 프레임 파싱 불필요
- ✅ 직접 STOMP 프레임 사용

### 2. SockJS 사용 시

- ⚠️ 서버가 `o` (open), `a[...]` (array) 프레임 전송
- ⚠️ 클라이언트가 SockJS 프레임 파싱 필요
- ⚠️ STOMP 라이브러리가 자동으로 처리하지 못할 수 있음

### 3. 암호화

- 🔐 모든 메시지는 **AES-256-GCM 암호화** 필수
- 🔐 `content`와 `iv` 모두 Base64로 인코딩
- 🔐 서버는 암호문만 저장 (평문 확인 불가)

---

## 🚀 프론트엔드 구현 가이드

전체 가이드는 다음 문서 참조:

1. **[FRONTEND_WEBSOCKET_GUIDE.md](FRONTEND_WEBSOCKET_GUIDE.md)** - WebSocket 연결 및 STOMP 사용법
2. **[FRONTEND_E2EE_GUIDE.md](FRONTEND_E2EE_GUIDE.md)** - E2EE 암호화 완전 구현
3. **[API_SPECIFICATION.md](API_SPECIFICATION.md)** - REST API 명세 (E2EE 키 관리 포함)

---

## 📊 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| Spring Boot | ✅ 실행 중 | v3.5.9 |
| PostgreSQL | ✅ 실행 중 | v15 |
| nginx | ✅ 실행 중 | v1.29.4 |
| WebSocket (순수) | ✅ 활성화 | `/ws/chat` |
| WebSocket (SockJS) | ✅ 활성화 | `/ws/chat-sockjs` |
| STOMP | ✅ 활성화 | SimpleBroker |
| JWT 인증 | ✅ 활성화 | Query Parameter |
| E2EE REST API | ✅ 활성화 | `/api/chat/keys/*` |
| 건강 체크 | ✅ 정상 | `/api/health` |

---

## 🐛 알려진 이슈

### None ✅

모든 WebSocket 엔드포인트가 정상 작동 중입니다.

---

## 📞 문의

문제 발생 시:
1. 로그 확인: `docker logs spring-app`
2. nginx 로그: `docker logs nginx-proxy`
3. 연결 테스트: SockJS info 엔드포인트 사용

---

**마지막 업데이트**: 2026-01-20 18:35 KST  
**배포자**: Backend Team  
**상태**: ✅ Production Ready
