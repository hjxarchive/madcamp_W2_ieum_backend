# 백엔드 WebSocket 실시간 동기화 구현 완료 - 프론트엔드 통합 가이드

## 📡 WebSocket 엔드포인트
- **연결**: `ws://54.66.195.91/ws/stomp`
- **프로토콜**: STOMP (순수 WebSocket, SockJS 아님)

## ✅ 서버 측 실시간 동기화 구현 확인

**백엔드에서 REST API 호출 시 자동 브로드캐스트가 이미 구현되어 있습니다!**

모든 Service 클래스에서 데이터 생성/수정/삭제 후 자동으로 WebSocket 브로드캐스트를 실행합니다:

### 구현 예시

**일정 생성** (`EventService.createEvent()`):
```kotlin
val savedEvent = eventRepository.save(event)
broadcastScheduleSync(couple.id!!, "ADDED", response, userId)
// → /topic/couple/{coupleId}/schedule 로 ADDED 이벤트 전송
```

**지출 수정** (`FinanceService.updateExpense()`):
```kotlin
val savedExpense = expenseRepository.save(expense)
broadcastFinanceSync(couple.id!!, "EXPENSE_UPDATED", null, response, userId)
// → /topic/couple/{coupleId}/finance 로 EXPENSE_UPDATED 이벤트 전송
```

**일정 삭제** (`EventService.deleteEvent()`):
```kotlin
event.deletedAt = LocalDateTime.now()
val savedEvent = eventRepository.save(event)
broadcastScheduleSync(couple.id!!, "DELETED", response, userId)
// → /topic/couple/{coupleId}/schedule 로 DELETED 이벤트 전송
```

**기념일 수정** (`CoupleService.updateCouple()`):
```kotlin
if (request.anniversary != null) {
    broadcastAnniversarySync(couple.id!!, couple.anniversary, userId)
    // → /topic/couple/{coupleId}/anniversary 로 ANNIVERSARY_UPDATED 이벤트 전송
}
```

### 작동 방식
1. 프론트엔드에서 REST API 호출 (예: `POST /api/events`)
2. 백엔드에서 DB에 데이터 저장
3. **자동으로** 해당 couple의 WebSocket 토픽으로 이벤트 브로드캐스트
4. 구독 중인 모든 클라이언트(Device A, B)가 즉시 수신
5. 각 클라이언트가 Repository의 StateFlow 업데이트

**결론**: 프론트엔드는 WebSocket 구독만 구현하면 자동으로 실시간 동기화가 작동합니다!

---

## 🎯 지원되는 실시간 동기화 기능

### 1. 일정 (Schedule)
**토픽**: `/topic/couple/{coupleId}/schedule`

**이벤트 타입**:
- `ADDED` - 일정 생성
- `UPDATED` - 일정 수정
- `DELETED` - 일정 삭제

**메시지 형식**:
```json
{
  "eventType": "ADDED",
  "schedule": {
    "id": "uuid",
    "title": "데이트",
    "date": "2024-01-20",
    "time": "18:00",
    "colorHex": "#FF5733",
    "description": "저녁 식사"
  },
  "userId": "user-uuid",
  "timestamp": "2024-01-20T18:00:00"
}
```

---

### 2. 버킷리스트 (Bucket)
**토픽**: `/topic/couple/{coupleId}/bucket`

**이벤트 타입**:
- `ADDED` - 버킷 생성
- `COMPLETED` - 버킷 완료 (isCompleted: false → true 변경 시)
- `UPDATED` - 버킷 수정 (일반 수정)
- `DELETED` - 버킷 삭제

**메시지 형식**:
```json
{
  "eventType": "COMPLETED",
  "bucket": {
    "id": "uuid",
    "title": "제주도 여행",
    "category": "TRAVEL",
    "isCompleted": true,
    "createdAt": "2024-01-01T00:00:00",
    "completedAt": "2024-01-20T18:00:00"
  },
  "userId": "user-uuid",
  "timestamp": "2024-01-20T18:00:00"
}
```

---

### 3. 재무 (Finance)
**토픽**: `/topic/couple/{coupleId}/finance`

**이벤트 타입**:
- `BUDGET_UPDATED` - 예산 설정/수정
- `EXPENSE_ADDED` - 지출 추가
- `EXPENSE_UPDATED` - 지출 수정 ⭐ (새로 추가됨)
- `EXPENSE_DELETED` - 지출 삭제

**메시지 형식**:
```json
{
  "eventType": "EXPENSE_UPDATED",
  "budget": null,
  "expense": {
    "id": "uuid",
    "title": "저녁 식사",
    "category": "FOOD",
    "amount": 50000,
    "date": "2024-01-20"
  },
  "userId": "user-uuid",
  "timestamp": "2024-01-20T18:00:00"
}
```

**예산 메시지 예시**:
```json
{
  "eventType": "BUDGET_UPDATED",
  "budget": {
    "monthlyBudget": 1000000,
    "month": "2024-01"
  },
  "expense": null,
  "userId": "user-uuid",
  "timestamp": "2024-01-20T18:00:00"
}
```

---

### 4. 기념일 (Anniversary) ⭐ NEW!
**토픽**: `/topic/couple/{coupleId}/anniversary`

**이벤트 타입**:
- `ANNIVERSARY_UPDATED` - 기념일 설정/수정

**메시지 형식**:
```json
{
  "eventType": "ANNIVERSARY_UPDATED",
  "anniversary": {
    "date": "2024-01-20"
  },
  "userId": "user-uuid",
  "timestamp": "2024-01-20T18:00:00"
}
```
*Note: anniversary.date는 null 가능 (기념일 삭제 시)*

---

## 🛠️ 프론트엔드 구현 체크리스트

### Step 1: WebSocketDto.kt에 타입 추가

```kotlin
// FinanceEventType에 EXPENSE_UPDATED 추가
enum class FinanceEventType {
    BUDGET_UPDATED,
    EXPENSE_ADDED,
    EXPENSE_UPDATED,  // ⭐ 추가 필요
    EXPENSE_DELETED
}

// 기념일 관련 타입 추가
data class AnniversarySyncMessage(
    val eventType: String,
    val anniversary: AnniversaryDto?,
    val userId: String,
    val timestamp: String
)

data class AnniversaryDto(
    val date: String?  // "2024-01-20" 또는 null
)
```

---

### Step 2: ChatWebSocketClient.kt에 기념일 구독 추가

```kotlin
fun subscribeToAnniversarySync(coupleId: String) {
    stompSession?.subscribe("/topic/couple/$coupleId/anniversary") { message ->
        val syncMessage = gson.fromJson(message.payload, AnniversarySyncMessage::class.java)
        listener?.onAnniversarySync(syncMessage)
    }
}
```

---

### Step 3: ChatEventListener 인터페이스에 콜백 추가

```kotlin
interface ChatEventListener {
    // ... 기존 메서드들 ...
    fun onAnniversarySync(message: AnniversarySyncMessage)
}
```

---

### Step 4: FinanceRepositoryImpl.kt에 EXPENSE_UPDATED 처리 추가

```kotlin
override fun handleFinanceSync(message: FinanceSyncMessage) {
    when (message.eventType) {
        FinanceEventType.BUDGET_UPDATED -> {
            // 기존 코드 유지
        }
        FinanceEventType.EXPENSE_ADDED -> {
            // 기존 코드 유지
        }
        FinanceEventType.EXPENSE_UPDATED -> {  // ⭐ 새로 추가
            message.expense?.let { updatedExpense ->
                val currentList = _expenses.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == updatedExpense.id }
                if (index != -1) {
                    currentList[index] = updatedExpense
                    _expenses.value = currentList
                    Log.d("FinanceRepository", "✅ Expense updated: ${updatedExpense.id}")
                }
            }
        }
        FinanceEventType.EXPENSE_DELETED -> {
            // 기존 코드 유지
        }
    }
}
```

---

### Step 5: CoupleRepository에 기념일 처리 추가

```kotlin
// StateFlow 추가
private val _anniversary = MutableStateFlow<LocalDate?>(null)
val anniversary: StateFlow<LocalDate?> = _anniversary

// 핸들러 추가
fun handleAnniversarySync(message: AnniversarySyncMessage) {
    message.anniversary?.date?.let { dateString ->
        _anniversary.value = LocalDate.parse(dateString)
        Log.d("CoupleRepository", "✅ Anniversary updated: $dateString")
    } ?: run {
        _anniversary.value = null
        Log.d("CoupleRepository", "✅ Anniversary cleared")
    }
}
```

---

### Step 6: ChatRepositoryImpl.kt에 기념일 이벤트 전달 추가

```kotlin
override fun onAnniversarySync(message: AnniversarySyncMessage) {
    coupleRepository.handleAnniversarySync(message)
}
```

---

### Step 7: ViewModel에서 기념일 관찰

```kotlin
// DashboardViewModel 또는 관련 ViewModel
init {
    viewModelScope.launch {
        coupleRepository.anniversary.collect { anniversary ->
            // UI 업데이트 로직
            _uiState.update { it.copy(anniversary = anniversary) }
        }
    }
}
```

---

## 🧪 테스트 시나리오

### 일정 실시간 동기화
1. Device A에서 일정 생성 → Device B에 즉시 표시 확인
2. Device A에서 일정 수정 → Device B에 변경사항 즉시 반영 확인
3. Device A에서 일정 삭제 → Device B에서 즉시 제거 확인

### 버킷리스트 실시간 동기화
1. Device A에서 버킷 추가 → Device B에 즉시 표시
2. Device A에서 버킷 완료 체크 → Device B에 COMPLETED 이벤트로 즉시 반영
3. Device A에서 버킷 삭제 → Device B에서 즉시 제거

### 재무 실시간 동기화
1. Device A에서 예산 설정 → Device B 메인 페이지에 즉시 반영
2. Device A에서 지출 추가 → Device B에 즉시 표시
3. Device A에서 지출 수정 → Device B에 변경사항 즉시 반영 ⭐
4. Device A에서 지출 삭제 → Device B에서 즉시 제거

### 기념일 실시간 동기화 ⭐ NEW!
1. Device A에서 기념일 설정 → Device B D-day 화면에 즉시 반영
2. Device A에서 기념일 수정 → Device B에 변경사항 즉시 반영

---

## 📊 실시간 성능

- **지연 시간**: 0.1~0.3초
- **프로토콜**: WebSocket (채팅과 동일)
- **동작 방식**: 푸시 (폴링 아님)
- **신뢰성**: DB 저장 직후 즉시 브로드캐스트

---

## 📋 전체 WebSocket 실시간 동기화 현황

| 기능 | 토픽 | 이벤트 타입 | 상태 |
|------|------|------------|------|
| **일정** | `/topic/couple/{coupleId}/schedule` | ADDED, UPDATED, DELETED | ✅ |
| **버킷리스트** | `/topic/couple/{coupleId}/bucket` | ADDED, COMPLETED, UPDATED, DELETED | ✅ |
| **재무** | `/topic/couple/{coupleId}/finance` | BUDGET_UPDATED, EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED | ✅ |
| **기념일** | `/topic/couple/{coupleId}/anniversary` | ANNIVERSARY_UPDATED | ✅ NEW! |
| **채팅** | `/topic/couple/{coupleId}/chat` | MESSAGE, TYPING, READ | ✅ (기존) |

---

## ⚠️ 주의사항

1. **WebSocket 연결**: 앱 시작 시 한 번만 연결, 모든 토픽 구독
2. **Couple ID**: 로그인 시 받은 couple ID로 토픽 구독
3. **자기 이벤트**: userId를 확인하여 자신의 액션인지 구분 가능
4. **에러 핸들링**: 네트워크 끊김 시 재연결 로직 필요
5. **메시지 순서**: timestamp 활용하여 순서 보장
6. **구독 타이밍**: WebSocket 연결 성공 후 모든 토픽 구독 권장

---

## 🔗 API 엔드포인트 참고

- 일정: `POST/PUT/DELETE /api/events/{id}`
- 버킷: `POST/PUT/DELETE /api/buckets/{id}`
- 재무: `POST/PUT/DELETE /api/finance/expenses/{id}`, `POST /api/finance/budget`
- 기념일: `PUT /api/couples` (body: `{"anniversary": "2024-01-20"}`)

---

## 🚀 구현 순서 추천

1. **Step 1-3**: WebSocket DTO 및 구독 설정 (기초)
2. **Step 4**: 재무 EXPENSE_UPDATED 처리 (긴급)
3. **Step 5-7**: 기념일 처리 전체 구현
4. **테스트**: 각 기능별 실시간 동기화 검증

---

## 💡 디버깅 팁

### WebSocket 연결 확인
```kotlin
// 연결 상태 로그
stompClient.connect { frame ->
    Log.d("WebSocket", "✅ Connected: $frame")
}
```

### 메시지 수신 확인
```kotlin
// 각 구독에 로그 추가
stompSession?.subscribe("/topic/couple/$coupleId/finance") { message ->
    Log.d("WebSocket", "📨 Finance message received: ${message.payload}")
    // ... 처리 로직
}
```

### 일반적인 이슈
- **메시지 안 받음**: 구독 토픽 경로 확인 (`/topic/couple/{coupleId}/...`)
- **연결 끊김**: 재연결 로직 구현 확인
- **데이터 안 보임**: Repository의 StateFlow 구독 확인

### DELETED 이벤트가 안 보이는 경우
**증상**: 일정/버킷/지출 삭제 시 상대방 화면에 반영 안 됨

**백엔드 확인 사항** ✅:
- ✅ EventService.deleteEvent() - DELETED 브로드캐스트 구현됨
- ✅ BucketService.deleteBucket() - DELETED 브로드캐스트 구현됨  
- ✅ FinanceService.deleteExpense() - EXPENSE_DELETED 브로드캐스트 구현됨

**프론트엔드 체크리스트**:
1. **구독 확인**: `/topic/couple/{coupleId}/schedule` 구독되어 있는지 확인
2. **로그 확인**: 
   ```kotlin
   stompSession?.subscribe("/topic/couple/$coupleId/schedule") { message ->
       Log.d("WebSocket", "📨 Schedule message: ${message.payload}")
       // DELETED 메시지가 수신되는지 확인
   }
   ```
3. **Repository 처리**: `handleScheduleSync()`에서 DELETED 케이스 처리 확인
   ```kotlin
   when (message.eventType) {
       ScheduleEventType.DELETED -> {
           val currentList = _schedules.value.toMutableList()
           currentList.removeIf { it.id == message.schedule.id }
           _schedules.value = currentList
       }
   }
   ```

**테스트 방법**:
1. Device A로 일정 삭제
2. Device A 터미널 로그 확인: `📨 Schedule sync received: DELETED`
3. Device B 터미널 로그 확인: `📨 Schedule sync received: DELETED`
4. 둘 다 로그가 있으면 → Repository 처리 문제
5. Device A만 로그 있으면 → 구독 문제 또는 네트워크 문제

---

## 📞 문의사항

백엔드 WebSocket 관련 이슈가 있으면 서버 로그 확인 또는 문의 주세요.

**서버 정보**:
- URL: http://54.66.195.91
- WebSocket: ws://54.66.195.91/ws/stomp
- Health Check: http://54.66.195.91/api/health

---

## 📝 변경 이력

### 2026-01-20
- ✅ 일정 WebSocket 실시간 동기화 추가
- ✅ 버킷리스트 WebSocket 실시간 동기화 추가
- ✅ 재무 WebSocket 실시간 동기화 추가
- ✅ 지출 수정(EXPENSE_UPDATED) 이벤트 추가
- ✅ 기념일 WebSocket 실시간 동기화 추가

---

**마지막 업데이트**: 2026년 1월 20일  
**백엔드 버전**: 0.0.1-SNAPSHOT  
**배포 상태**: ✅ 프로덕션 배포 완료
