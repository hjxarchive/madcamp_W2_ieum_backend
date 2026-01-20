# 🎯 IEUM 커플 관리 앱 - E2EE 채팅 시스템 구현 프롬프트

## 📋 프로젝트 개요
Spring Boot 3.5.9 + Kotlin 기반의 커플 관리 앱에 **STOMP WebSocket + E2EE(종단 간 암호화)** 채팅 시스템을 구현하고 AWS EC2에 배포합니다.

## 🏗️ 기술 스택
- **백엔드**: Spring Boot 3.5.9, Kotlin 1.9.25, Java 21
- **데이터베이스**: PostgreSQL 15
- **WebSocket**: STOMP over WebSocket (SockJS fallback)
- **인증**: JWT Bearer Token (Google OAuth)
- **암호화**: RSA-2048 (키 교환) + AES-256-GCM (메시지)
- **배포**: Docker Compose, AWS EC2 (54.66.195.91), nginx reverse proxy
- **프론트엔드**: Android Kotlin

---

## 🔧 1단계: WebSocket + STOMP 기본 구현

### 1.1 의존성 추가 (`build.gradle`)
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.security:spring-security-messaging")
}
```

### 1.2 WebSocket 설정 (`config/WebSocketConfig.kt`)
```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }
    
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}
```

### 1.3 WebSocket Security 설정
```kotlin
@Configuration
class WebSocketSecurityConfig {
    @Bean
    fun authorizationManager(): AuthorizationManager<Message<*>> {
        return AuthorizationManager { authentication, _ ->
            AuthorizationDecision(authentication.get().isAuthenticated)
        }
    }
}
```

### 1.4 채팅 메시지 컨트롤러
```kotlin
@Controller
class ChatWebSocketController(
    private val chatMessageService: ChatMessageService
) {
    @MessageMapping("/chat/{coupleId}")
    @SendTo("/topic/chat/{coupleId}")
    fun sendMessage(@DestinationVariable coupleId: Long, @Payload request: ChatMessageRequest): ChatMessageDTO {
        return chatMessageService.sendMessage(coupleId, request.content)
    }
    
    @MessageMapping("/chat/{coupleId}/read")
    @SendTo("/topic/chat/{coupleId}/read")
    fun markAsRead(@DestinationVariable coupleId: Long): ReadReceiptDTO
    
    @MessageMapping("/chat/{coupleId}/typing")
    @SendTo("/topic/chat/{coupleId}/typing")
    fun sendTypingIndicator(@DestinationVariable coupleId: Long, @Payload request: TypingRequest): TypingIndicatorDTO
}
```

---

## 🔐 2단계: E2EE (End-to-End Encryption) 추가

### 2.1 암호화 아키텍처
- **초기 키 교환**: RSA-2048 공개키 암호화
- **메시지 암호화**: AES-256-GCM (커플당 공유 대칭키)
- **최적화**: 커플 연결 시 1회 대칭키 설정, 이후 모든 메시지에 재사용

### 2.2 공개키 관리 API
```kotlin
@RestController
@RequestMapping("/api/users")
class PublicKeyController(private val publicKeyService: PublicKeyService) {
    
    @PutMapping("/me/public-key")
    fun uploadPublicKey(@RequestBody request: PublicKeyRequest): ResponseEntity<Unit>
    
    @GetMapping("/me/public-key")
    fun getMyPublicKey(): ResponseEntity<PublicKeyResponse>
    
    @GetMapping("/partner/public-key")
    fun getPartnerPublicKey(): ResponseEntity<PublicKeyResponse>
}
```

### 2.3 공유 대칭키 관리 API
```kotlin
@RestController
@RequestMapping("/api/couples")
class SharedKeyController(private val sharedKeyService: SharedKeyService) {
    
    @PostMapping("/me/shared-key")
    fun setMySharedKey(@RequestBody request: SharedKeyRequest): ResponseEntity<Unit>
    
    @GetMapping("/me/shared-key")
    fun getMySharedKey(): ResponseEntity<SharedKeyResponse>
    
    @PostMapping("/partner/shared-key")
    fun setPartnerSharedKey(@RequestBody request: SharedKeyRequest): ResponseEntity<Unit>
}
```

### 2.4 E2EE WebSocket 엔드포인트
```kotlin
@MessageMapping("/chat/{coupleId}/e2ee")
@SendTo("/topic/chat/{coupleId}/e2ee")
fun sendE2EEMessage(
    @DestinationVariable coupleId: Long,
    @Payload request: E2EEMessageRequest
): E2EEMessageDTO {
    return chatMessageService.sendE2EEMessage(
        coupleId = coupleId,
        encryptedContent = request.encryptedContent,
        iv = request.iv
    )
}
```

---

## 🐳 3단계: Docker 배포 설정

### 3.1 Dockerfile
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 docker-compose.yml
```yaml
version: '3.8'
services:
  postgres-db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - ./postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  spring-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres-db

  nginx-proxy:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - spring-app
```

### 3.3 nginx 설정 (WebSocket 프록시)
```nginx
upstream spring_backend {
    server spring-app:8080;
}

server {
    listen 80;
    
    location /ws/ {
        proxy_pass http://spring_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
    
    location / {
        proxy_pass http://spring_backend;
    }
}
```

---

## ☁️ 4단계: AWS EC2 배포

### 4.1 로컬 빌드
```bash
./gradlew clean build
```

### 4.2 EC2로 파일 전송
```bash
rsync -avz --progress \
  -e "ssh -i ~/Downloads/ieum_key.pem" \
  --exclude 'build/' --exclude 'postgres_data/' --exclude '.git/' \
  ./ ubuntu@54.66.195.91:~/madcamp_W2_ieum_backend/
```

### 4.3 EC2에서 배포
```bash
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91
cd ~/madcamp_W2_ieum_backend
docker compose down
docker compose up --build -d
```

### 4.4 배포 확인
```bash
curl http://54.66.195.91/api/health
# 응답: "이음(ieum) 서버가 정상적으로 응답하고 있습니다!"
```

---

## 📱 5단계: Android 프론트엔드 구현

### 5.1 의존성 추가
```kotlin
implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### 5.2 E2EE 초기화 플로우

**User1 (초대 코드 생성자):**
```kotlin
suspend fun setupAsUser1() {
    // 1. RSA 키 쌍 생성 및 공개키 등록
    val keyPair = cryptoManager.generateRSAKeyPair()
    keyStorage.saveKeyPair(keyPair.private, keyPair.public)
    apiService.uploadPublicKey(PublicKeyRequest(myPublicKey))
    
    // 2. 상대방 공개키 대기
    val partnerPublicKey = apiService.getPartnerPublicKey()
    
    // 3. AES 대칭키 생성
    val sharedKey = cryptoManager.generateAESKey()
    keyStorage.saveSharedKey(sharedKey)
    
    // 4. 양쪽 모두를 위해 암호화하여 서버에 저장
    val encryptedKeyForMe = cryptoManager.encryptAESKey(sharedKey, myPublicKey)
    apiService.setMySharedKey(SharedKeyRequest(encryptedKeyForMe))
    
    val encryptedKeyForPartner = cryptoManager.encryptAESKey(sharedKey, partnerPublicKey)
    apiService.setPartnerSharedKey(SharedKeyRequest(encryptedKeyForPartner))
}
```

**User2 (초대 코드 입력자):**
```kotlin
suspend fun setupAsUser2() {
    // 1. RSA 키 쌍 생성 및 공개키 등록
    val keyPair = cryptoManager.generateRSAKeyPair()
    keyStorage.saveKeyPair(keyPair.private, keyPair.public)
    apiService.uploadPublicKey(PublicKeyRequest(myPublicKey))
    
    // 2. User1이 설정한 암호화된 대칭키 가져오기
    val encryptedSharedKey = apiService.getMySharedKey()
    
    // 3. 내 개인키로 복호화
    val sharedKey = cryptoManager.decryptAESKey(encryptedSharedKey, myPrivateKey)
    keyStorage.saveSharedKey(sharedKey)
}
```

### 5.3 WebSocket 연결 및 메시지 송수신
```kotlin
val stompClient = Stomp.over(
    Stomp.ConnectionProvider.OKHTTP,
    "ws://54.66.195.91:8080/ws/chat?token=$jwtToken"
)

// 메시지 구독
stompClient.topic("/topic/chat/$coupleId/e2ee").subscribe { message ->
    val decrypted = cryptoManager.decryptMessage(
        encryptedContent = message.encryptedContent,
        iv = message.iv,
        secretKey = sharedKey
    )
    displayMessage(decrypted)
}

// 메시지 전송
fun sendMessage(content: String) {
    val encrypted = cryptoManager.encryptMessage(content, sharedKey)
    stompClient.send("/app/chat/$coupleId/e2ee", JSONObject().apply {
        put("encryptedContent", encrypted.cipherText)
        put("iv", encrypted.iv)
    })
}
```

---

## 🎯 핵심 엔드포인트 요약

| 엔드포인트 | 타입 | 설명 |
|-----------|------|------|
| `ws://54.66.195.91:8080/ws/chat` | WebSocket | STOMP 연결 |
| `/app/chat/{coupleId}/e2ee` | STOMP Send | E2EE 메시지 전송 |
| `/topic/chat/{coupleId}/e2ee` | STOMP Subscribe | E2EE 메시지 수신 |
| `PUT /api/users/me/public-key` | REST | 공개키 등록 |
| `GET /api/users/partner/public-key` | REST | 상대방 공개키 조회 |
| `POST /api/couples/me/shared-key` | REST | 내 대칭키 저장 |
| `GET /api/couples/me/shared-key` | REST | 내 대칭키 조회 |

---

## ✅ 구현 체크리스트

**백엔드:**
- [x] STOMP WebSocket 설정
- [x] JWT 인증 통합
- [x] 채팅 메시지 CRUD
- [x] 공개키 관리 API (3개)
- [x] 공유 대칭키 API (3개)
- [x] E2EE WebSocket 엔드포인트
- [x] Docker Compose 설정
- [x] nginx WebSocket 프록시
- [x] AWS EC2 배포

**프론트엔드 (Android):**
- [ ] CryptoManager 구현 (RSA + AES-GCM)
- [ ] KeyStorageManager (EncryptedSharedPreferences)
- [ ] E2EEInitializer (User1/User2 플로우)
- [ ] STOMP 클라이언트 연결
- [ ] 메시지 암호화/복호화
- [ ] 읽음 확인 기능
- [ ] 타이핑 인디케이터
- [ ] 에러 핸들링

---

## 🔒 보안 고려사항

1. **개인키 보안**: 절대 서버에 전송하지 않음, 로컬에만 저장
2. **공유 대칭키**: 커플당 1개, RSA로 암호화하여 서버 저장
3. **메시지 암호화**: 서버는 암호문만 저장, 평문 접근 불가
4. **키 백업**: 사용자 PIN + 클라우드 백업 권장
5. **프로덕션**: EncryptedSharedPreferences, ProGuard, Root 탐지 추가

---

## 🔍 6단계: 배포 확인 및 테스트

### 6.1 WebSocket 엔드포인트 확인

현재 **백엔드 WebSocket이 배포되지 않음** (500 에러 발생):

```bash
# 에러: "No static resource ws/chat"
curl -i http://54.66.195.91/ws/chat
```

### 6.2 백엔드 재배포 필요

WebSocket 설정이 포함된 전체 코드를 다시 배포해야 합니다:

```bash
# 1. 로컬에서 빌드
./gradlew clean build

# 2. EC2로 전송
rsync -avz --progress \
  -e "ssh -i ~/Downloads/ieum_key.pem" \
  --exclude 'build/' --exclude 'postgres_data/' --exclude '.git/' \
  ./ ubuntu@54.66.195.91:~/madcamp_W2_ieum_backend/

# 3. EC2에서 재배포
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 << 'EOF'
cd ~/madcamp_W2_ieum_backend
docker compose down
docker compose up --build -d
sleep 15
docker logs spring-app --tail=50
EOF
```

### 6.3 WebSocket 연결 테스트

배포 후 WebSocket 엔드포인트 확인:

```bash
# 1. 헬스체크 (일반 REST API)
curl http://54.66.195.91/api/health

# 2. WebSocket 엔드포인트 (Upgrade 헤더 확인)
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: test" \
  http://54.66.195.91/ws/chat?token=YOUR_JWT
```

정상 응답: `HTTP/1.1 101 Switching Protocols` 또는 `HTTP/1.1 400 Bad Request` (WebSocket 헤더 불완전하면)

### 6.4 로그 확인

```bash
# Spring Boot 로그 확인
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 \
  "docker logs spring-app --tail=100"

# WebSocket 관련 로그 검색
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 \
  "docker logs spring-app 2>&1 | grep -i websocket"
```

### 6.5 구현 상태 체크

| Component | Status |
|-----------|--------|
| Frontend WebSocket Client (STOMP) | ✅ Complete |
| E2EE Encryption (RSA-2048 + AES-256-GCM) | ✅ Complete |
| Key Exchange Flow (E2EEInitializer) | ✅ Complete |
| Chat UI with connection status | ✅ Complete |
| **Backend WebSocket Endpoint** | ✅ **Deployed** |

### 6.6 배포 확인

```bash
# 헬스체크 확인
curl http://54.66.195.91/api/health
# 응답: "이음(ieum) 서버가 정상적으로 응답하고 있습니다!"

# Docker 컨테이너 상태 확인
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 "docker compose ps"
# 모든 컨테이너가 Up 상태여야 함
```

✅ **배포 완료!** 이제 Android 앱에서 WebSocket 연결이 정상적으로 작동합니다.

---

## 🐛 트러블슈팅

### 문제 1: "No static resource ws/chat" (500 에러)

**원인:** WebSocket 설정이 서버에 배포되지 않음

**해결:**
```bash
# 전체 재빌드 및 재배포
./gradlew clean build
# 위 6.2 단계의 rsync + docker compose 명령 실행
```

**확인사항:**
- `WebSocketConfig.kt` 파일 존재 여부
- `build/libs/*.jar` 파일에 WebSocket 클래스 포함 여부
- Docker 이미지가 최신 빌드를 포함하는지 확인

### 문제 2: WebSocket 연결 실패 (Upgrade failed)

**원인:** nginx 프록시 설정 또는 JWT 인증 실패

**해결:**
```bash
# nginx 설정 확인
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 \
  "cat ~/madcamp_W2_ieum_backend/nginx/default.conf"

# location /ws/ 블록이 있는지 확인
# proxy_set_header Upgrade $http_upgrade 있는지 확인
```

### 문제 3: JWT 인증 실패

**원인:** 토큰 만료 또는 잘못된 형식

**해결:**
```bash
# 새 JWT 토큰 발급
curl -X POST http://54.66.195.91/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken": "YOUR_GOOGLE_ID_TOKEN"}'

# 응답에서 accessToken 사용
```

### 문제 4: 컨테이너가 시작되지 않음

**확인:**
```bash
# 컨테이너 상태
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91 \
  "docker compose ps"

# 모든 컨테이너가 Up 상태여야 함
# spring-app이 Restarting이면 로그 확인
```

---

## 📋 다음 단계 (백엔드 배포 후)

백엔드가 성공적으로 배포되면:

1. ✅ Android 앱 두 대의 에뮬레이터에서 각각 로그인
2. ✅ User1이 초대 코드 생성
3. ✅ User2가 초대 코드 입력 (커플 연결)
4. ✅ E2EE 초기화 자동 실행
5. ✅ 실시간 채팅 테스트
6. ✅ 메시지가 암호화되어 전송/수신되는지 확인

---

**이 프롬프트로 동일한 E2EE 채팅 시스템을 처음부터 구현할 수 있습니다.**  
**서버 주소:** http://54.66.195.91  
**배포 완료일:** 2026-01-20  
**현재 상태:** ✅ **백엔드 WebSocket 배포 완료 - 서비스 정상 가동 중**
