# E2EE (End-to-End Encryption) 구현 가이드 - Android Kotlin

## 📌 개요

이음 앱의 채팅에 **종단 간 암호화(E2EE)**를 추가하여 서버도 메시지 내용을 볼 수 없게 합니다.

---

## 🔐 E2EE 동작 원리

### 개선된 방식: 커플 연결 시 대칭키 공유

```
┌─────────────────────────────────────────────────────────────┐
│  초기 설정 (커플 연결 시 1회)                                │
└─────────────────────────────────────────────────────────────┘

사용자 A (user1)                  서버                  사용자 B (user2)
    |                              |                           |
1. AES 대칭키 생성                 |                           |
    ↓                              |                           |
2. 내 공개키로 암호화              |                           |
   (keyForA)                       |                           |
    ↓                              |                           |
3. B의 공개키로 암호화             |                           |
   (keyForB)                       |                           |
    ↓                              |                           |
4. ──두 버전 전송──→               |                           |
    |                         keyForA, keyForB 저장            |
    |                              |                           |
    |                              |                    5. ←──keyForB 요청
    |                              |                           ↓
    |                              |                    6. keyForB 수신
    |                              |                           ↓
    |                              |                    7. 개인키로 복호화
    |                              |                           ↓
    |                              |                    8. 대칭키 획득 ✓

┌─────────────────────────────────────────────────────────────┐
│  이후 모든 메시지 (공유 대칭키 사용)                          │
└─────────────────────────────────────────────────────────────┘

사용자 A                           서버                  사용자 B
    |                              |                           |
1. 평문 "Hello"                    |                           |
    ↓                              |                           |
2. 공유 대칭키로 암호화            |                           |
    ↓                              |                           |
3. ──암호문 전송──→                 암호문만 저장                |
    |                              ↓                           |
    |                         ──암호문 전달──→                  |
    |                              |                    1. 암호문 수신
    |                              |                           ↓
    |                              |                    2. 공유 대칭키로 복호화
    |                              |                           ↓
    |                              |                    3. 평문 "Hello"
```

### 장점
- ✅ **빠름**: RSA 암호화가 초기 1회만 필요
- ✅ **간단**: 메시지마다 키 관리 불필요
- ✅ **효율적**: AES 암호화만 사용 (RSA보다 훨씬 빠름)

### 암호화 방식
- **초기 키 교환**: RSA-2048 (공개키 암호화)
- **메시지 암호화**: AES-256-GCM (공유 대칭키)

---

## 📦 1. 의존성 추가

`app/build.gradle.kts`:

```kotlin
dependencies {
    // 기존 WebSocket 의존성
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // E2EE 암호화 라이브러리
    implementation("com.goterl:lazysodium-android:5.1.4@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    
    // 또는 기본 Java 암호화만 사용 (추가 의존성 불필요)
}
```

---

## 🔑 2. 키 관리 클래스

### CryptoManager.kt

```kotlin
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * E2EE 암호화/복호화 관리
 */
class CryptoManager {
    
    companion object {
        private const val RSA_KEY_SIZE = 2048
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    /**
     * RSA 키 쌍 생성 (최초 1회)
     */
    fun generateRSAKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(RSA_KEY_SIZE)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * 공개키를 Base64 문자열로 변환
     */
    fun publicKeyToString(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Base64 문자열을 공개키로 변환
     */
    fun stringToPublicKey(publicKeyString: String): PublicKey {
        val keyBytes = Base64.decode(publicKeyString, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * AES 세션키 생성
     */
    fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_SIZE)
        return keyGenerator.generateKey()
    }

    /**
     * 메시지 암호화 (AES-256-GCM)
     */
    fun encryptMessage(plainText: String, secretKey: SecretKey): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            cipherText = Base64.encodeToString(cipherText, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * 메시지 복호화 (AES-256-GCM)
     */
    fun decryptMessage(encryptedData: EncryptedData, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
        val cipherText = Base64.decode(encryptedData.cipherText, Base64.NO_WRAP)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }

    /**
     * AES 키를 상대방 공개키로 암호화 (RSA)
     */
    fun encryptAESKey(aesKey: SecretKey, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
    }

    /**
     * 암호화된 AES 키를 내 개인키로 복호화 (RSA)
     */
    fun decryptAESKey(encryptedKey: String, privateKey: PrivateKey): SecretKey {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val keyBytes = Base64.decode(encryptedKey, Base64.NO_WRAP)
        val decryptedKeyBytes = cipher.doFinal(keyBytes)
        return SecretKeySpec(decryptedKeyBytes, "AES")
    }
}

/**
 * 암호화된 데이터
 */
data class EncryptedData(
    val cipherText: String,  // Base64 암호문
    val iv: String           // Base64 초기화 벡터
)
```

---

## 💾 3. 키 저장 관리

### KeyStorageManager.kt

```kotlin
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * 키 저장소 (SharedPreferences에 안전하게 저장)
 * 프로덕션에서는 EncryptedSharedPreferences 사용 권장
 */
class KeyStorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "e2ee_keys",
        Context.MODE_PRIVATE
    )
    private val cryptoManager = CryptoManager()

    /**
     * 키 쌍이 존재하는지 확인
     */
    fun hasKeyPair(): Boolean {
        return prefs.contains("private_key") && prefs.contains("public_key")
    }

    /**
     * 키 쌍 저장
     */
    fun saveKeyPair(privateKey: PrivateKey, publicKey: PublicKey) {
        prefs.edit().apply {
            putString("private_key", Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP))
            putString("public_key", Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP))
            apply()
        }
    }

    /**
     * 개인키 가져오기
     */
    fun getPrivateKey(): PrivateKey? {
        val keyString = prefs.getString("private_key", null) ?: return null
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * 공개키 가져오기
     */
    fun getPublicKey(): PublicKey? {
        val keyString = prefs.getString("public_key", null) ?: return null
        return cryptoManager.stringToPublicKey(keyString)
    }

    /**
     * 공개키 문자열 가져오기 (서버 전송용)
     */
    fun getPublicKeyString(): String? {
        return prefs.getString("public_key", null)
    }

    /**
     * 상대방 공개키 저장
     */
    fun savePartnerPublicKey(publicKeyString: String) {
        prefs.edit().putString("partner_public_key", publicKeyString).apply()
    }

    /**
     * 상대방 공개키 가져오기
     */
    fun getPartnerPublicKey(): PublicKey? {
        val keyString = prefs.getString("partner_public_key", null) ?: return null
        return cryptoManager.stringToPublicKey(keyString)
    }

    /**
     * 공유 대칭키 저장 (커플 전용)
     */
    fun saveSharedKey(sharedKey: SecretKey) {
        val keyString = Base64.encodeToString(sharedKey.encoded, Base64.NO_WRAP)
        prefs.edit().putString("shared_aes_key", keyString).apply()
    }

    /**
     * 공유 대칭키 가져오기
     */
    fun getSharedKey(): SecretKey? {
        val keyString = prefs.getString("shared_aes_key", null) ?: return null
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 공유 대칭키 존재 여부
     */
    fun hasSharedKey(): Boolean {
        return prefs.contains("shared_aes_key")
    }

    /**
     * 모든 키 삭제
     */
    fun clearKeys() {
        prefs.edit().clear().apply()
    }
}
```

---

## 🔌 4. E2EE 초기화 및 대칭키 설정

### E2EEInitializer.kt

```kotlin
class E2EEInitializer(
    private val context: Context,
    private val apiService: ApiService
) {
    private val cryptoManager = CryptoManager()
    private val keyStorage = KeyStorageManager(context)

    /**
     * E2EE 초기 설정 (커플 연결 후 최초 1회 실행)
     * 
     * User1 (초대 코드 생성자)이 실행:
     * 1. 공유 대칭키 생성
     * 2. 자신의 공개키로 암호화하여 저장
     * 3. 상대방의 공개키로 암호화하여 저장
     */
    suspend fun setupAsUser1() {
        try {
            // 1. 내 키 쌍 생성 (없으면)
            if (!keyStorage.hasKeyPair()) {
                val keyPair = cryptoManager.generateRSAKeyPair()
                keyStorage.saveKeyPair(keyPair.private, keyPair.public)
            }

            // 2. 내 공개키 서버에 등록
            val myPublicKey = keyStorage.getPublicKeyString()!!
            apiService.uploadPublicKey(PublicKeyRequest(myPublicKey))

            // 3. 상대방 공개키 가져오기 (재시도 로직 포함)
            var partnerPublicKey: String? = null
            for (i in 1..5) {
                val response = apiService.getPartnerPublicKey()
                if (response.hasKey && response.publicKey != null) {
                    partnerPublicKey = response.publicKey
                    keyStorage.savePartnerPublicKey(partnerPublicKey)
                    break
                }
                delay(2000) // 2초 대기 후 재시도
            }

            if (partnerPublicKey == null) {
                throw IllegalStateException("상대방이 아직 공개키를 등록하지 않았습니다")
            }

            // 4. 공유 AES 대칭키 생성
            val sharedKey = cryptoManager.generateAESKey()
            keyStorage.saveSharedKey(sharedKey)

            // 5. 내 공개키로 암호화하여 저장
            val myPublicKeyObj = keyStorage.getPublicKey()!!
            val encryptedKeyForMe = cryptoManager.encryptAESKey(sharedKey, myPublicKeyObj)
            
            apiService.setMySharedKey(SharedKeyRequest(encryptedKeyForMe))

            // 6. 상대방 공개키로 암호화하여 저장
            val partnerPublicKeyObj = cryptoManager.stringToPublicKey(partnerPublicKey)
            val encryptedKeyForPartner = cryptoManager.encryptAESKey(sharedKey, partnerPublicKeyObj)
            
            apiService.setPartnerSharedKey(SharedKeyRequest(encryptedKeyForPartner))

            Log.d("E2EE", "✅ User1 대칭키 설정 완료")

        } catch (e: Exception) {
            Log.e("E2EE", "❌ User1 설정 실패", e)
            throw e
        }
    }

    /**
     * E2EE 초기 설정 (커플 연결 후 최초 1회 실행)
     * 
     * User2 (초대 코드 입력자)가 실행:
     * 1. 공개키 등록
     * 2. 서버에서 암호화된 대칭키 가져오기
     * 3. 복호화하여 저장
     */
    suspend fun setupAsUser2() {
        try {
            // 1. 내 키 쌍 생성 (없으면)
            if (!keyStorage.hasKeyPair()) {
                val keyPair = cryptoManager.generateRSAKeyPair()
                keyStorage.saveKeyPair(keyPair.private, keyPair.public)
            }

            // 2. 내 공개키 서버에 등록
            val myPublicKey = keyStorage.getPublicKeyString()!!
            apiService.uploadPublicKey(PublicKeyRequest(myPublicKey))

            // 3. User1이 설정한 암호화된 대칭키 가져오기 (재시도 로직)
            var encryptedSharedKey: String? = null
            for (i in 1..10) {
                val response = apiService.getMySharedKey()
                if (response.hasSharedKey && response.encryptedSharedKey != null) {
                    encryptedSharedKey = response.encryptedSharedKey
                    break
                }
                delay(2000) // 2초 대기 후 재시도
            }

            if (encryptedSharedKey == null) {
                throw IllegalStateException("User1이 아직 대칭키를 설정하지 않았습니다")
            }

            // 4. 내 개인키로 대칭키 복호화
            val myPrivateKey = keyStorage.getPrivateKey()!!
            val sharedKey = cryptoManager.decryptAESKey(encryptedSharedKey, myPrivateKey)
            
            // 5. 대칭키 저장
            keyStorage.saveSharedKey(sharedKey)

            Log.d("E2EE", "✅ User2 대칭키 설정 완료")

        } catch (e: Exception) {
            Log.e("E2EE", "❌ User2 설정 실패", e)
            throw e
        }
    }

    /**
     * 기존 커플이 E2EE를 활성화하는 경우
     * (User1, User2 순서 상관없음)
     */
    suspend fun setupExistingCouple() {
        try {
            // 1. 공개키 등록
            if (!keyStorage.hasKeyPair()) {
                val keyPair = cryptoManager.generateRSAKeyPair()
                keyStorage.saveKeyPair(keyPair.private, keyPair.public)
            }

            val myPublicKey = keyStorage.getPublicKeyString()!!
            apiService.uploadPublicKey(PublicKeyRequest(myPublicKey))

            // 2. 상대방 공개키 가져오기
            val partnerResponse = apiService.getPartnerPublicKey()
            if (!partnerResponse.hasKey || partnerResponse.publicKey == null) {
                throw IllegalStateException("상대방이 먼저 공개키를 등록해야 합니다")
            }
            keyStorage.savePartnerPublicKey(partnerResponse.publicKey)

            // 3. 서버에 대칭키가 있는지 확인
            val sharedKeyResponse = apiService.getMySharedKey()
            
            if (sharedKeyResponse.hasSharedKey && sharedKeyResponse.encryptedSharedKey != null) {
                // 대칭키가 이미 있음 -> 복호화하여 사용
                val myPrivateKey = keyStorage.getPrivateKey()!!
                val sharedKey = cryptoManager.decryptAESKey(
                    sharedKeyResponse.encryptedSharedKey, 
                    myPrivateKey
                )
                keyStorage.saveSharedKey(sharedKey)
                Log.d("E2EE", "✅ 기존 대칭키 복호화 완료")
            } else {
                // 대칭키가 없음 -> 새로 생성 (첫 번째 사용자)
                val sharedKey = cryptoManager.generateAESKey()
                keyStorage.saveSharedKey(sharedKey)

                // 내 것 암호화
                val myPublicKeyObj = keyStorage.getPublicKey()!!
                val encryptedKeyForMe = cryptoManager.encryptAESKey(sharedKey, myPublicKeyObj)
                apiService.setMySharedKey(SharedKeyRequest(encryptedKeyForMe))

                // 상대방 것 암호화
                val partnerPublicKeyObj = cryptoManager.stringToPublicKey(partnerResponse.publicKey)
                val encryptedKeyForPartner = cryptoManager.encryptAESKey(sharedKey, partnerPublicKeyObj)
                apiService.setPartnerSharedKey(SharedKeyRequest(encryptedKeyForPartner))

                Log.d("E2EE", "✅ 새 대칭키 생성 완료")
            }

        } catch (e: Exception) {
            Log.e("E2EE", "❌ 기존 커플 설정 실패", e)
            throw e
        }
    }
}
```

---

## 🔌 5. E2EE WebSocket 클라이언트 (간소화)

이제 매번 상대방 공개키로 암호화할 필요 없이, 커플이 공유하는 대칭키로만 암호화하면 됩니다!

### E2EEChatWebSocketClient.kt

```kotlin
class E2EEChatWebSocketClient(
    private val context: Context,
    private val coupleId: Long,
    private val jwtToken: String,
    private val listener: ChatEventListener
) {
    private var stompClient: StompClient? = null
    private val cryptoManager = CryptoManager()
    private val keyStorage = KeyStorageManager(context)

    fun connect() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://54.66.195.91:8080/ws/chat?token=$jwtToken",
            null,
            client
        )

        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("WebSocket", "연결 성공")
                    subscribeToMessages()
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("WebSocket", "연결 오류", lifecycleEvent.exception)
                    listener.onError(lifecycleEvent.exception)
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("WebSocket", "연결 종료")
                    listener.onDisconnected()
                }
            }
        }

        stompClient?.connect()
    }

    private fun subscribeToMessages() {
        // E2EE 메시지 구독
        stompClient?.topic("/topic/chat/$coupleId/e2ee")?.subscribe { message ->
            try {
                val json = JSONObject(message.payload)
                
                // 공유 대칭키로 복호화
                val sharedKey = keyStorage.getSharedKey()
                    ?: throw IllegalStateException("대칭키가 없습니다. E2EE 초기화를 먼저 수행하세요")

                val decryptedContent = cryptoManager.decryptMessage(
                    encryptedContent = json.getString("encryptedContent"),
                    iv = json.getString("iv"),
                    secretKey = sharedKey
                )

                val chatMessage = ChatMessage(
                    id = json.getLong("id"),
                    coupleId = json.getLong("coupleId"),
                    senderId = json.getLong("senderId"),
                    content = decryptedContent,
                    sentAt = json.getString("sentAt"),
                    isRead = json.getBoolean("isRead"),
                    isEncrypted = true
                )

                listener.onMessageReceived(chatMessage)
            } catch (e: Exception) {
                Log.e("E2EE", "메시지 복호화 실패", e)
                listener.onError(e)
            }
        }

        // 읽음 확인 구독
        stompClient?.topic("/topic/chat/$coupleId/read")?.subscribe { message ->
            val json = JSONObject(message.payload)
            listener.onReadReceipt(json.getLong("readerId"))
        }

        // 타이핑 인디케이터 구독
        stompClient?.topic("/topic/chat/$coupleId/typing")?.subscribe { message ->
            val json = JSONObject(message.payload)
            listener.onTypingIndicator(json.getLong("userId"), json.getBoolean("isTyping"))
        }
    }

    /**
     * E2EE 메시지 전송 (간소화)
     */
    fun sendE2EEMessage(content: String) {
        try {
            // 공유 대칭키로 암호화
            val sharedKey = keyStorage.getSharedKey()
                ?: throw IllegalStateException("대칭키가 없습니다. E2EE 초기화를 먼저 수행하세요")

            val encrypted = cryptoManager.encryptMessage(content, sharedKey)

            val payload = JSONObject().apply {
                put("encryptedContent", encrypted.first)  // 암호화된 내용
                put("iv", encrypted.second)               // IV
            }

            stompClient?.send("/app/chat/$coupleId/e2ee", payload.toString())
                ?.subscribe(
                    {
                        Log.d("E2EE", "암호화 메시지 전송 성공")
                        listener.onMessageSent()
                    },
                    { error ->
                        Log.e("E2EE", "메시지 전송 실패", error)
                        listener.onError(error)
                    }
                )

        } catch (e: Exception) {
            Log.e("E2EE", "메시지 암호화 실패", e)
            listener.onError(e)
        }
    }

    fun sendReadReceipt() {
        val payload = JSONObject()
        stompClient?.send("/app/chat/$coupleId/read", payload.toString())?.subscribe()
    }

    fun sendTypingIndicator(isTyping: Boolean) {
        val payload = JSONObject().apply {
            put("isTyping", isTyping)
        }
        stompClient?.send("/app/chat/$coupleId/typing", payload.toString())?.subscribe()
    }

    fun disconnect() {
        stompClient?.disconnect()
    }
}
```

---

## 📱 6. 앱에서 사용하기

### MainActivity.kt (커플 연결 후)

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var e2eeInitializer: E2EEInitializer
    private lateinit var webSocketClient: E2EEChatWebSocketClient
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // E2EE 초기화
        e2eeInitializer = E2EEInitializer(this, apiService)
        
        // 커플 연결 완료 후 E2EE 설정
        lifecycleScope.launch {
            try {
                // User1이면 setupAsUser1(), User2이면 setupAsUser2()
                if (isUser1) {
                    e2eeInitializer.setupAsUser1()
                } else {
                    e2eeInitializer.setupAsUser2()
                }
                
                // WebSocket 연결
                connectWebSocket()
                
            } catch (e: Exception) {
                Log.e("E2EE", "초기화 실패", e)
                // 에러 처리
            }
        }
    }
    
    private fun connectWebSocket() {
        webSocketClient = E2EEChatWebSocketClient(
            context = this,
            coupleId = myCoupleId,
            jwtToken = myJwtToken,
            listener = object : ChatEventListener {
                override fun onMessageReceived(message: ChatMessage) {
                    runOnUiThread {
                        // UI 업데이트
                        addMessageToChat(message)
                    }
                }
                
                override fun onReadReceipt(readerId: Long) {
                    runOnUiThread {
                        updateReadStatus(readerId)
                    }
                }
                
                override fun onTypingIndicator(userId: Long, isTyping: Boolean) {
                    runOnUiThread {
                        showTypingIndicator(isTyping)
                    }
                }
                
                override fun onMessageSent() {
                    Log.d("Chat", "메시지 전송 완료")
                }
                
                override fun onError(error: Throwable) {
                    Log.e("Chat", "에러 발생", error)
                }
                
                override fun onDisconnected() {
                    Log.d("Chat", "연결 종료")
                }
            }
        )
        
        webSocketClient.connect()
    }
    
    // 메시지 전송 버튼
    private fun sendMessage() {
        val messageText = messageInput.text.toString()
        webSocketClient.sendE2EEMessage(messageText)
        messageInput.text.clear()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.disconnect()
    }
}
```

---

## 🔄 7. API Service (Retrofit)

### ApiService.kt

```kotlin
interface ApiService {
    
    // 공개키 관리
    @PUT("api/users/me/public-key")
    suspend fun uploadPublicKey(@Body request: PublicKeyRequest): ApiResponse<Unit>
    
    @GET("api/users/me/public-key")
    suspend fun getMyPublicKey(): PublicKeyResponse
    
    @GET("api/users/partner/public-key")
    suspend fun getPartnerPublicKey(): PublicKeyResponse
    
    // 공유 대칭키 관리
    @POST("api/couples/me/shared-key")
    suspend fun setMySharedKey(@Body request: SharedKeyRequest): ApiResponse<Unit>
    
    @GET("api/couples/me/shared-key")
    suspend fun getMySharedKey(): SharedKeyResponse
    
    @POST("api/couples/partner/shared-key")
    suspend fun setPartnerSharedKey(@Body request: SharedKeyRequest): ApiResponse<Unit>
}

// Request/Response DTOs
data class PublicKeyRequest(val publicKey: String)

data class PublicKeyResponse(
    val hasKey: Boolean,
    val publicKey: String?
)

data class SharedKeyRequest(val encryptedSharedKey: String)

data class SharedKeyResponse(
    val hasSharedKey: Boolean,
    val encryptedSharedKey: String?
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
---

## 🎯 8. 전체 플로우 요약

### 초기 설정 (커플 연결 후 1회)

```
User1 (초대 코드 생성자):
1. RSA 키 쌍 생성 및 공개키 서버 등록
2. 상대방(User2) 공개키 대기 및 조회
3. AES-256 대칭키 생성
4. 자신의 공개키로 대칭키 암호화 → 서버 저장
5. 상대방 공개키로 대칭키 암호화 → 서버 저장

User2 (초대 코드 입력자):
1. RSA 키 쌍 생성 및 공개키 서버 등록
2. User1이 저장한 암호화된 대칭키 조회
3. 자신의 개인키로 대칭키 복호화
4. 로컬에 대칭키 저장
```

### 메시지 전송 (이후 모든 메시지)

```
송신자:
1. 평문 메시지 작성
2. 공유 대칭키로 AES-256-GCM 암호화
3. WebSocket으로 전송 (암호문 + IV)

수신자:
1. WebSocket에서 암호문 수신
2. 공유 대칭키로 AES-256-GCM 복호화
3. 평문 메시지 화면 표시
```

---

## ⚠️ 9. 보안 주의사항

1. **개인키 보안**
   - 개인키는 절대 서버에 전송하지 않습니다
   - 로컬 디바이스에만 저장 (EncryptedSharedPreferences 권장)
   - 앱 삭제 시 키도 삭제되므로 주의

2. **공유 대칭키 관리**
   - 커플 당 하나의 대칭키만 사용
   - 보안이 필요한 경우 주기적으로 키 갱신 가능
   - 관계 종료 시 키 삭제 권장

3. **키 백업**
   - 키를 잃어버리면 과거 메시지 복호화 불가
   - 백업 메커니즘 구현 권장 (사용자 PIN + 클라우드 등)

4. **성능 고려**
   - 암호화/복호화로 약간의 지연 발생
   - 하지만 공유 대칭키 방식으로 최소화됨

5. **프로덕션 환경**
   - `SharedPreferences` 대신 `EncryptedSharedPreferences` 사용
   - ProGuard/R8으로 코드 난독화
   - Root 탐지 추가 권장

---

## 🎯 10. 구현 체크리스트

- [ ] CryptoManager 클래스 구현
- [ ] KeyStorageManager 구현 (EncryptedSharedPreferences)
- [ ] E2EEInitializer 구현
- [ ] 공개키 REST API 연동 (3개 엔드포인트)
- [ ] 공유 대칭키 REST API 연동 (3개 엔드포인트)
- [ ] E2EE WebSocket 클라이언트 구현
- [ ] UI에서 E2EE 초기화 플로우 구현
- [ ] 에러 핸들링 (키 없음, 복호화 실패 등)
- [ ] 테스트 (User1/User2 각각)
- [ ] 보안 강화 (ProGuard, Root 탐지)

---

## 📝 11. 요약

이 E2EE 구현은 다음과 같은 특징을 가집니다:

- **하이브리드 암호화**: RSA (키 교환) + AES-256-GCM (메시지)
- **최적화**: 커플당 하나의 공유 대칭키로 빠른 암호화
- **보안성**: 서버는 암호문만 저장, 평문 접근 불가
- **편의성**: 초기 설정 후 자동으로 암호화/복호화

---

**작성일:** 2026-01-20  
**보안 수준:** End-to-End Encryption (E2EE) with Shared Symmetric Key

---

## 🎯 구현 체크리스트

- [ ] CryptoManager 클래스 구현
- [ ] KeyStorageManager 구현
- [ ] 공개키 API 연동
- [ ] E2EE WebSocket 클라이언트 구현
- [ ] UI에서 E2EE 모드 토글
- [ ] 에러 핸들링 (키 없음 등)
- [ ] 테스트

---

**작성일:** 2026-01-20  
**보안 수준:** End-to-End Encryption (E2EE)
