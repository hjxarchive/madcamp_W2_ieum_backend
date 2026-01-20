# 프론트엔드 WebSocket 연동 가이드 (Kotlin/Android)

## 📱 Android Kotlin 클라이언트 구현 가이드

---

## 1. 의존성 추가

`app/build.gradle.kts`에 다음 의존성을 추가하세요:

```kotlin
dependencies {
    // OkHttp (WebSocket 클라이언트)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // STOMP Protocol
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    
    // Gson (JSON 파싱)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines (비동기 처리)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
}
```

`settings.gradle.kts`에 JitPack 저장소 추가:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // 추가
    }
}
```

---

## 2. 데이터 모델 정의

### ChatMessage.kt
```kotlin
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderProfileImage: String?,
    val content: String?,
    val type: MessageType,
    val imageUrl: String?,
    val isRead: Boolean,
    val readAt: String?,
    val createdAt: String,
    val tempId: String? = null
)

enum class MessageType {
    TEXT, IMAGE, STICKER
}
```

### SendMessageRequest.kt
```kotlin
data class SendMessageRequest(
    val type: String = "TEXT",
    val content: String? = null,
    val imageUrl: String? = null,
    val tempId: String = UUID.randomUUID().toString()
)
```

---

## 3. WebSocket 클라이언트 구현

### ChatWebSocketClient.kt

```kotlin
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.*

class ChatWebSocketClient(
    private val serverUrl: String = "ws://54.66.195.91/ws/chat",  // 개발: EC2 IP
    // private val serverUrl: String = "wss://your-domain.com/ws/chat",  // 프로덕션: 도메인 + SSL
    private val jwtToken: String
) {
    private var stompClient: StompClient? = null
    private val gson = Gson()
    private val compositeDisposable = CompositeDisposable()

    // 연결 상태
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // 수신 메시지
    private val _receivedMessages = MutableStateFlow<ChatMessage?>(null)
    val receivedMessages: StateFlow<ChatMessage?> = _receivedMessages

    // 읽음 상태 업데이트
    private val _readReceipts = MutableStateFlow<ReadReceipt?>(null)
    val readReceipts: StateFlow<ReadReceipt?> = _readReceipts

    /**
     * WebSocket 연결
     */
    fun connect(coupleId: String) {
        if (stompClient != null) {
            disconnect()
        }

        // URL에 JWT 토큰 추가
        val url = "$serverUrl?token=$jwtToken"
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url).apply {
            // 연결 상태 리스너
            lifecycle().subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        subscribeToCoupleChannel(coupleId)
                    }
                    LifecycleEvent.Type.ERROR -> {
                        _connectionState.value = ConnectionState.ERROR
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                    else -> {}
                }
            }.let { compositeDisposable.add(it) }

            // 연결 시작
            connect()
        }
    }

    /**
     * 커플 채팅방 구독
     */
    private fun subscribeToCoupleChannel(coupleId: String) {
        stompClient?.topic("/topic/couple/$coupleId")?.subscribe { topicMessage ->
            try {
                val json = topicMessage.payload
                
                // 메시지 타입 파싱 (type 필드로 구분)
                val messageType = gson.fromJson(json, Map::class.java)["type"]
                
                when (messageType) {
                    "READ_RECEIPT" -> {
                        val receipt = gson.fromJson(json, ReadReceipt::class.java)
                        _readReceipts.value = receipt
                    }
                    "SYSTEM" -> {
                        // 시스템 메시지 처리 (타이핑 등)
                    }
                    "ERROR" -> {
                        // 에러 처리
                    }
                    else -> {
                        // 일반 채팅 메시지
                        val message = gson.fromJson(json, ChatMessage::class.java)
                        _receivedMessages.value = message
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }?.let { compositeDisposable.add(it) }
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(coupleId: String, content: String, type: String = "TEXT") {
        val request = SendMessageRequest(
            type = type,
            content = content,
            tempId = UUID.randomUUID().toString()
        )

        stompClient?.send("/app/chat/$coupleId", gson.toJson(request))?.subscribe(
            {
                // 전송 성공
            },
            { error ->
                error.printStackTrace()
            }
        )?.let { compositeDisposable.add(it) }
    }

    /**
     * 이미지 메시지 전송
     */
    fun sendImageMessage(coupleId: String, imageUrl: String) {
        val request = SendMessageRequest(
            type = "IMAGE",
            imageUrl = imageUrl,
            tempId = UUID.randomUUID().toString()
        )

        stompClient?.send("/app/chat/$coupleId", gson.toJson(request))?.subscribe()
            ?.let { compositeDisposable.add(it) }
    }

    /**
     * 읽음 처리
     */
    fun markAsRead(coupleId: String, messageIds: List<String>) {
        stompClient?.send("/app/chat/$coupleId/read", gson.toJson(messageIds))?.subscribe()
            ?.let { compositeDisposable.add(it) }
    }

    /**
     * 타이핑 인디케이터 전송
     */
    fun sendTypingIndicator(coupleId: String) {
        stompClient?.send("/app/chat/$coupleId/typing", "")?.subscribe()
            ?.let { compositeDisposable.add(it) }
    }

    /**
     * 연결 해제
     */
    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
        stompClient = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

enum class ConnectionState {
    CONNECTED, DISCONNECTED, ERROR
}

data class ReadReceipt(
    val type: String,
    val messageIds: List<String>,
    val readAt: String
)
```

---

## 4. ViewModel 구현

### ChatViewModel.kt

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private lateinit var webSocketClient: ChatWebSocketClient
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    /**
     * WebSocket 연결 초기화
     */
    fun initializeWebSocket(jwtToken: String, coupleId: String) {
        webSocketClient = ChatWebSocketClient(
            serverUrl = "ws://54.66.195.91/ws/chat",  // EC2 퍼블릭 IP 또는 도메인
            jwtToken = jwtToken
        )
        
        // 연결 상태 관찰
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
        
        // 수신 메시지 관찰
        viewModelScope.launch {
            webSocketClient.receivedMessages.collect { message ->
                message?.let {
                    _messages.value = _messages.value + it
                }
            }
        }
        
        // 읽음 상태 업데이트 관찰
        viewModelScope.launch {
            webSocketClient.readReceipts.collect { receipt ->
                receipt?.let {
                    updateReadStatus(it.messageIds)
                }
            }
        }
        
        // 연결
        webSocketClient.connect(coupleId)
    }

    /**
     * 메시지 전송
     */
    fun sendMessage(coupleId: String, content: String) {
        webSocketClient.sendMessage(coupleId, content)
    }

    /**
     * 읽음 처리
     */
    fun markMessagesAsRead(coupleId: String, messageIds: List<String>) {
        webSocketClient.markAsRead(coupleId, messageIds)
    }

    /**
     * 읽음 상태 업데이트
     */
    private fun updateReadStatus(messageIds: List<String>) {
        _messages.value = _messages.value.map { message ->
            if (messageIds.contains(message.id)) {
                message.copy(isRead = true)
            } else {
                message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
}
```

---

## 5. Activity/Fragment에서 사용

### ChatActivity.kt

```kotlin
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        val jwtToken = getJwtToken() // SharedPreferences에서 가져오기
        val coupleId = getCoupleId()
        
        // WebSocket 초기화 및 연결
        viewModel.initializeWebSocket(jwtToken, coupleId)
        
        // 메시지 목록 관찰
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                updateMessageList(messages)
            }
        }
        
        // 연결 상태 관찰
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        showToast("연결됨")
                    }
                    ConnectionState.DISCONNECTED -> {
                        showToast("연결 끊김")
                    }
                    ConnectionState.ERROR -> {
                        showToast("연결 오류")
                    }
                }
            }
        }
        
        // 메시지 전송 버튼
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(coupleId, message)
                binding.messageInput.text.clear()
            }
        }
    }
    
    private fun updateMessageList(messages: List<ChatMessage>) {
        // RecyclerView 업데이트
    }
}
```

---

## 6. AndroidManifest.xml 권한 추가

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 7. 재연결 로직 (선택사항)

```kotlin
class ChatWebSocketClient(/* ... */) {
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    
    private fun handleDisconnection() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            Handler(Looper.getMainLooper()).postDelayed({
                connect(currentCoupleId)
            }, 2000L * reconnectAttempts) // 지수 백오프
        }
    }
}
```

---

## 8. 테스트 방법

### 1. 로그 확인
```kotlin
stompClient?.lifecycle()?.subscribe { event ->
    Log.d("WebSocket", "Event: ${event.type}, Message: ${event.message}")
}
```

### 2. 연결 테스트
```kotlin
// 1. WebSocket 연결
viewModel.initializeWebSocket(jwtToken, coupleId)

// 2. 메시지 전송
viewModel.sendMessage(coupleId, "테스트 메시지")

// 3. 로그 확인
// D/WebSocket: Event: OPENED
// D/WebSocket: Message received: {...}
```

---

## 9. 주의사항

### 1. 네트워크 보안 (HTTP 허용)

`res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

`AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### 2. 프로덕션 환경

프로덕션에서는 **WSS (WebSocket Secure)** 사용:
```kotlin
val serverUrl = "wss://your-domain.com/ws/chat"
```

### 3. 백그라운드 처리

앱이 백그라운드로 가면 WebSocket 연결 해제:
```kotlin
override fun onPause() {
    super.onPause()
    viewModel.disconnect()
}

override fun onResume() {
    super.onResume()
    viewModel.reconnect()
}
```

---

## 10. API 엔드포인트 요약

| 동작 | 엔드포인트 | 설명 |
|------|------------|------|
| **연결** | `ws://server:8080/ws/chat?token={JWT}` | WebSocket 연결 |
| **구독** | `/topic/couple/{coupleId}` | 메시지 수신 |
| **메시지 전송** | `/app/chat/{coupleId}` | 메시지 전송 |
| **읽음 처리** | `/app/chat/{coupleId}/read` | 읽음 상태 업데이트 |
| **타이핑** | `/app/chat/{coupleId}/typing` | 타이핑 인디케이터 |

---

## 11. 트러블슈팅

### 연결 실패
- JWT 토큰 유효성 확인
- 서버 URL 확인 (IP, 포트)
- 방화벽 설정 확인

### 메시지 수신 안됨
- 구독 경로 확인 (`/topic/couple/{coupleId}`)
- 로그 확인

### 재연결 문제
- 재연결 로직 구현
- 네트워크 상태 확인

---

## 12. 다음 단계 (Phase 2: E2EE)

E2EE 구현 시 추가 작업:
1. **libsodium** 또는 **Signal Protocol** 라이브러리 추가
2. 키 생성 및 교환 로직
3. 메시지 암호화/복호화
4. 서버는 암호문만 전달

---

**작성일:** 2026-01-20  
**작성자:** Backend Developer
