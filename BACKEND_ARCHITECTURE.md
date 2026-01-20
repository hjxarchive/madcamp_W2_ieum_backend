# 이음(ieum) 백엔드 아키텍처 상세 문서

> 커플 앱 이음의 백엔드 시스템 기술 스택, 통신 방식, 암호화 구현 등 전체 아키텍처 상세 가이드

**버전**: 0.0.1-SNAPSHOT  
**작성일**: 2026년 1월 21일  
**배포 서버**: http://54.66.195.91

---

## 📚 목차

1. [기술 스택](#1-기술-스택)
2. [프로젝트 구조](#2-프로젝트-구조)
3. [데이터베이스 설계](#3-데이터베이스-설계)
4. [인증 및 보안](#4-인증-및-보안)
5. [통신 구현](#5-통신-구현)
6. [암호화 시스템](#6-암호화-시스템)
7. [WebSocket 실시간 동기화](#7-websocket-실시간-동기화)
8. [배포 및 인프라](#8-배포-및-인프라)
9. [API 엔드포인트](#9-api-엔드포인트)

---

## 1. 기술 스택

### 1.1 언어 및 프레임워크

```yaml
Language: Kotlin 1.9.25 + Java 21
Framework: Spring Boot 3.5.9
Build Tool: Gradle 8.x
Database: PostgreSQL 15
Container: Docker + Docker Compose
Proxy: Nginx
```

### 1.2 핵심 의존성

#### Spring Boot Starters
```gradle
spring-boot-starter-web           # REST API 서버
spring-boot-starter-data-jpa      # ORM (Hibernate)
spring-boot-starter-validation    # 데이터 검증
spring-boot-starter-websocket     # WebSocket + STOMP
```

#### Kotlin 지원
```gradle
kotlin-reflect                    # Kotlin 리플렉션
kotlin-stdlib                     # Kotlin 표준 라이브러리
jackson-module-kotlin             # JSON 직렬화/역직렬화
```

#### 인증 및 보안
```gradle
google-api-client:2.2.0           # Google OAuth 2.0 ID Token 검증
jjwt-api:0.12.3                   # JWT 토큰 생성/검증 (인터페이스)
jjwt-impl:0.12.3                  # JWT 구현체
jjwt-jackson:0.12.3               # JWT JSON 처리
```

#### 데이터베이스
```gradle
postgresql                        # PostgreSQL JDBC 드라이버
spring-boot-starter-data-jpa      # Hibernate ORM
```

### 1.3 주요 기술 선택 이유

| 기술 | 선택 이유 |
|-----|----------|
| **Kotlin** | • Null Safety로 런타임 오류 감소<br>• 간결한 문법으로 생산성 향상<br>• Java 완벽 호환으로 Spring Boot 생태계 활용 |
| **Spring Boot 3.5** | • 최신 Spring Framework 6 기반<br>• Java 21 네이티브 지원<br>• 강력한 DI 컨테이너와 자동 설정 |
| **PostgreSQL** | • JSON 타입 네이티브 지원 (MBTI 설문 저장)<br>• UUID 기본 지원<br>• 안정적인 트랜잭션 처리 |
| **WebSocket + STOMP** | • 실시간 양방향 통신 (채팅, 일정/버킷/재무 동기화)<br>• STOMP 프로토콜로 구독/발행 패턴 구현<br>• 모바일 환경에서 경량 통신 |
| **JWT** | • Stateless 인증 (서버 세션 불필요)<br>• 확장성 우수 (마이크로서비스 대응)<br>• 모바일 앱에 적합 |
| **Docker** | • 일관된 실행 환경 보장<br>• 간편한 배포 및 롤백<br>• 로컬-프로덕션 환경 일치 |

---

## 2. 프로젝트 구조

### 2.1 디렉토리 구조

```
src/main/kotlin/com/ieum/ieum_back/
├── IeumApplication.kt              # Spring Boot 진입점
│
├── auth/                           # 인증 관련
│   ├── controller/                 # 인증 REST API
│   ├── service/                    # Google OAuth, JWT 로직
│   ├── dto/                        # 인증 요청/응답 DTO
│   ├── JwtProvider.kt              # JWT 토큰 생성/검증
│   └── filter/                     # JWT 인증 필터
│
├── config/                         # 설정 클래스
│   ├── WebSocketConfig.kt          # WebSocket + STOMP 설정
│   ├── WebSocketAuthInterceptor.kt # WebSocket JWT 인증
│   └── StompConnectInterceptor.kt  # STOMP CONNECT 프레임 인증
│
├── entity/                         # JPA 엔티티
│   ├── User.kt                     # 사용자 (publicKey 포함)
│   ├── Couple.kt                   # 커플 (encryptedSharedKey 포함)
│   ├── ChatMessage.kt              # 채팅 메시지 (E2EE 필드 포함)
│   ├── Event.kt                    # 일정
│   ├── Bucket.kt                   # 버킷리스트
│   ├── Expense.kt                  # 지출
│   ├── Budget.kt                   # 예산
│   ├── Memory.kt                   # 추억
│   └── Recommendation.kt           # AI 추천
│
├── repository/                     # Spring Data JPA Repository
│   ├── UserRepository.kt
│   ├── CoupleRepository.kt
│   ├── ChatMessageRepository.kt
│   └── ...
│
├── users/                          # 사용자 관리
│   ├── controller/
│   │   ├── UserController.kt       # 사용자 CRUD
│   │   └── PublicKeyController.kt  # E2EE 공개키 관리
│   └── service/
│
├── couples/                        # 커플 관리
│   ├── controller/
│   │   ├── CoupleController.kt     # 커플 초대/연결
│   │   └── SharedKeyController.kt  # E2EE 공유 대칭키 관리
│   ├── service/
│   │   └── CoupleService.kt        # 기념일 WebSocket 브로드캐스트
│   └── dto/
│
├── chat/                           # 실시간 채팅
│   ├── controller/
│   │   └── ChatWebSocketController.kt  # STOMP 메시지 핸들러
│   ├── service/
│   │   └── ChatWebSocketService.kt     # 채팅 메시지 저장/처리
│   └── dto/
│
├── events/                         # 일정 관리
│   ├── controller/
│   ├── service/
│   │   └── EventService.kt         # 일정 CRUD + WebSocket 브로드캐스트
│   └── dto/
│
├── bucket/                         # 버킷리스트
│   ├── controller/
│   ├── service/
│   │   └── BucketService.kt        # 버킷 CRUD + WebSocket 브로드캐스트
│   └── dto/
│
├── finance/                        # 재무 관리
│   ├── controller/
│   │   ├── BudgetController.kt     # 예산 설정
│   │   └── ExpenseController.kt    # 지출 CRUD
│   ├── service/
│   │   └── FinanceService.kt       # 재무 CRUD + WebSocket 브로드캐스트
│   └── dto/
│
├── memory/                         # 추억 관리
├── recommendation/                 # AI 추천 (OpenAI API)
├── mbti/                           # MBTI 설문/분석
├── ddays/                          # D-day 계산
├── files/                          # 파일 업로드/다운로드
│
├── common/                         # 공통 유틸리티
│   ├── BaseTimeEntity.kt           # 생성/수정 시간 자동 추가
│   ├── JpaAuditingConfig.kt        # JPA Auditing 설정
│   └── WebMvcConfig.kt             # CORS 설정
│
└── exception/                      # 예외 처리
    ├── GlobalExceptionHandler.kt   # 전역 예외 핸들러
    └── CustomExceptions.kt         # 커스텀 예외 클래스
```

### 2.2 계층 구조

```
┌─────────────────────────────────────────────────┐
│           Client (Android App)                  │
│    REST API + WebSocket (STOMP)                 │
└───────────────────┬─────────────────────────────┘
                    │ HTTP / WS
┌───────────────────▼─────────────────────────────┐
│           Controller Layer                      │
│  @RestController / @Controller                  │
│  - REST API 엔드포인트                           │
│  - WebSocket @MessageMapping                    │
│  - 요청 검증 (@Valid)                            │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│           Service Layer                         │
│  @Service / @Transactional                      │
│  - 비즈니스 로직                                  │
│  - WebSocket 브로드캐스트                        │
│  - 트랜잭션 관리                                  │
└───────────────────┬─────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────┐
│         Repository Layer                        │
│  @Repository (Spring Data JPA)                  │
│  - DB CRUD 작업                                  │
│  - 커스텀 쿼리 메서드                             │
└───────────────────┬─────────────────────────────┘
                    │ JDBC
┌───────────────────▼─────────────────────────────┐
│          Database (PostgreSQL)                  │
│  - 데이터 영속화                                  │
│  - 트랜잭션 보장                                  │
└─────────────────────────────────────────────────┘
```

---

## 3. 데이터베이스 설계

### 3.1 ERD (주요 엔티티 관계)

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│    User      │ N     1 │   Couple     │ 1     N │ ChatMessage  │
│──────────────│─────────│──────────────│─────────│──────────────│
│ id (UUID)    │         │ id (UUID)    │         │ id (UUID)    │
│ email        │         │ user1_id     │         │ couple_id    │
│ name         │         │ user2_id     │         │ sender_id    │
│ googleId     │         │ anniversary  │         │ content      │
│ couple_id ◄──┼─────────┤ invite_code  │         │ type         │
│ publicKey    │         │ encrypted_   │         │ isEncrypted  │
│ mbti_type    │         │  sharedKey   │         │ encrypted_   │
└──────────────┘         │  _user1      │         │  content     │
                         │ encrypted_   │         │ encrypted_   │
                         │  sharedKey   │         │  key         │
                         │  _user2      │         │ iv           │
                         └──────────────┘         └──────────────┘
                                │ 1
                                │
                     ┌──────────┼──────────┐
                     │          │          │
                   1 │        1 │        1 │
            ┌────────▼───┐ ┌───▼──────┐ ┌─▼────────┐
            │   Event    │ │  Bucket  │ │ Expense  │
            │────────────│ │──────────│ │──────────│
            │ id (UUID)  │ │ id (UUID)│ │ id (UUID)│
            │ couple_id  │ │ couple_id│ │ couple_id│
            │ title      │ │ title    │ │ title    │
            │ startDate  │ │ category │ │ amount   │
            │ endDate    │ │ isComp.. │ │ category │
            │ deleted_at │ │ deleted..│ │ date     │
            └────────────┘ └──────────┘ └──────────┘
```

### 3.2 핵심 테이블 스키마

#### Users (사용자)
```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    nickname        VARCHAR(50),
    profile_image   VARCHAR(500),
    birthday        DATE,
    gender          VARCHAR(20),  -- MALE, FEMALE, OTHER
    google_id       VARCHAR(255) UNIQUE,
    couple_id       UUID REFERENCES couples(id),
    mbti_type       VARCHAR(4),   -- ENFP, INTJ 등
    mbti_answers    JSON,         -- 설문 응답 저장
    public_key      VARCHAR(1000),  -- E2EE 공개키 (Base64)
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

#### Couples (커플)
```sql
CREATE TABLE couples (
    id                             UUID PRIMARY KEY,
    user1_id                       UUID NOT NULL,
    user2_id                       UUID,
    anniversary                    DATE,
    invite_code                    VARCHAR(6) UNIQUE,  -- 초대 코드
    invite_expires_at              TIMESTAMP,
    encrypted_shared_key_user1     TEXT,  -- user1 공개키로 암호화된 공유 대칭키
    encrypted_shared_key_user2     TEXT,  -- user2 공개키로 암호화된 공유 대칭키
    created_at                     TIMESTAMP DEFAULT NOW(),
    deleted_at                     TIMESTAMP
);
```

#### ChatMessages (채팅 메시지)
```sql
CREATE TABLE chat_messages (
    id                  UUID PRIMARY KEY,
    couple_id           UUID NOT NULL REFERENCES couples(id),
    sender_id           UUID NOT NULL REFERENCES users(id),
    content             TEXT,             -- 평문 메시지 (암호화 시 null)
    type                VARCHAR(20),      -- TEXT, IMAGE, SYSTEM
    image_url           VARCHAR(500),
    is_encrypted        BOOLEAN DEFAULT false,
    encrypted_content   TEXT,             -- AES 암호화된 메시지
    encrypted_key       TEXT,             -- RSA로 암호화된 세션키
    iv                  VARCHAR(500),     -- AES 초기화 벡터
    is_read             BOOLEAN DEFAULT false,
    read_at             TIMESTAMP,
    created_at          TIMESTAMP DEFAULT NOW()
);
```

#### Events (일정)
```sql
CREATE TABLE events (
    id                UUID PRIMARY KEY,
    couple_id         UUID NOT NULL REFERENCES couples(id),
    created_by_id     UUID NOT NULL REFERENCES users(id),
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    start_date        TIMESTAMP NOT NULL,
    end_date          TIMESTAMP,
    is_all_day        BOOLEAN DEFAULT false,
    location          VARCHAR(500),
    reminder_minutes  INTEGER,           -- 알림 시간 (분)
    repeat            VARCHAR(20),       -- NONE, DAILY, WEEKLY, MONTHLY
    deleted_at        TIMESTAMP          -- Soft Delete
);
```

### 3.3 인덱스 전략

```sql
-- 성능 최적화를 위한 인덱스
CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_users_couple_id ON users(couple_id);
CREATE INDEX idx_couples_invite_code ON couples(invite_code);
CREATE INDEX idx_chat_messages_couple_id ON chat_messages(couple_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX idx_events_couple_id_date ON events(couple_id, start_date);
CREATE INDEX idx_expenses_couple_id_date ON expenses(couple_id, date DESC);
```

---

## 4. 인증 및 보안

### 4.1 인증 흐름

#### Google OAuth 2.0 로그인 프로세스

```
┌─────────┐                ┌─────────┐              ┌─────────┐              ┌─────────┐
│ Android │                │  ieum   │              │ Google  │              │   DB    │
│   App   │                │ Backend │              │  OAuth  │              │         │
└────┬────┘                └────┬────┘              └────┬────┘              └────┬────┘
     │                          │                        │                        │
     │ 1. Google Sign-In        │                        │                        │
     ├─────────────────────────►│                        │                        │
     │   (ID Token)              │                        │                        │
     │                          │                        │                        │
     │                          │ 2. Verify ID Token     │                        │
     │                          ├───────────────────────►│                        │
     │                          │                        │                        │
     │                          │ 3. Token Valid ✅      │                        │
     │                          │◄───────────────────────┤                        │
     │                          │   (email, googleId)    │                        │
     │                          │                        │                        │
     │                          │ 4. Find/Create User    │                        │
     │                          ├───────────────────────────────────────────────►│
     │                          │                        │                        │
     │                          │ 5. User Data           │                        │
     │                          │◄───────────────────────────────────────────────┤
     │                          │                        │                        │
     │                          │ 6. Generate JWT        │                        │
     │                          │                        │                        │
     │ 7. JWT + User Info       │                        │                        │
     │◄─────────────────────────┤                        │                        │
     │   { accessToken, user }  │                        │                        │
     │                          │                        │                        │
     │ 8. API Requests          │                        │                        │
     ├─────────────────────────►│                        │                        │
     │   Authorization: Bearer  │                        │                        │
     │   {JWT}                  │                        │                        │
     │                          │                        │                        │
```

#### 구현 코드

**1. Google ID Token 검증** (`AuthService.kt`)
```kotlin
@Service
class AuthService(
    private val jwtProvider: JwtProvider,
    @Value("\${google.client-id}") private val googleClientId: String
) {
    private val verifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(googleClientId))
            .build()
    }

    fun googleLogin(request: GoogleLoginRequest): AuthResponse {
        // 1. Google ID Token 검증
        val googleUserInfo = verifyGoogleToken(request.idToken)
        
        // 2. 사용자 조회 또는 생성
        val user = userRepository.findByGoogleId(googleUserInfo.googleId)
            ?: createNewUser(googleUserInfo)
        
        // 3. JWT 생성
        val accessToken = jwtProvider.generateToken(user.id!!, user.email)
        
        return AuthResponse(
            accessToken = accessToken,
            user = UserResponse.from(user)
        )
    }
    
    private fun verifyGoogleToken(idToken: String): GoogleUserInfo {
        val googleIdToken = verifier.verify(idToken)
            ?: throw UnauthorizedException("Invalid Google token")
        
        val payload = googleIdToken.payload
        return GoogleUserInfo(
            googleId = payload.subject,
            email = payload.email,
            name = payload["name"] as? String ?: payload.email.substringBefore("@"),
            profileImage = payload["picture"] as? String
        )
    }
}
```

**2. JWT 생성 및 검증** (`JwtProvider.kt`)
```kotlin
@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val expirationTime: Long  // 7일
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())  // HMAC SHA-256
    }

    fun generateToken(userId: UUID, email: String): String {
        val now = Date()
        val expiration = Date(now.time + expirationTime)

        return Jwts.builder()
            .subject(userId.toString())           // userId를 subject에
            .claim("email", email)                 // email을 claim에
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)                         // HS256 서명
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            !claims.expiration.before(Date())     // 만료 확인
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaims(token)
        return UUID.fromString(claims.subject)
    }
}
```

### 4.2 API 인증 방식

#### HTTP 요청 인증
```http
GET /api/users/me HTTP/1.1
Host: 54.66.195.91
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
```

**인증 필터 동작:**
1. `Authorization` 헤더에서 JWT 추출
2. JWT 검증 (서명, 만료 시간)
3. `userId` 추출 후 `X-User-Id` 헤더에 주입
4. Controller에서 `@RequestHeader("X-User-Id")` 로 사용

#### WebSocket 인증
```kotlin
// WebSocket 연결 시 JWT 인증
ws://54.66.195.91/ws/stomp?token={JWT}

// Handshake Interceptor에서 JWT 검증
@Component
class WebSocketAuthInterceptor(
    private val jwtProvider: JwtProvider
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val token = request.uri.query?.substringAfter("token=")
            ?: return false
        
        if (!jwtProvider.validateToken(token)) {
            return false  // 인증 실패 시 연결 거부
        }
        
        val userId = jwtProvider.getUserIdFromToken(token)
        attributes["userId"] = userId  // WebSocket 세션에 userId 저장
        return true
    }
}
```

### 4.3 보안 설정

#### CORS (Cross-Origin Resource Sharing)
```kotlin
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")               // 프로덕션에서는 특정 도메인으로 제한
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(false)
    }
}
```

---

## 5. 통신 구현

### 5.1 REST API 통신

#### 기본 구조
```
Client ──HTTP──► Nginx ──Proxy──► Spring Boot ──JDBC──► PostgreSQL
                (Port 80)         (Port 8080)          (Port 5432)
```

#### API 설계 원칙
- **RESTful URL 구조**: `/api/{resource}/{id}`
- **HTTP 메서드**: GET(조회), POST(생성), PUT(수정), DELETE(삭제)
- **상태 코드**:
  - `200 OK`: 성공
  - `201 Created`: 리소스 생성 성공
  - `400 Bad Request`: 잘못된 요청
  - `401 Unauthorized`: 인증 실패
  - `404 Not Found`: 리소스 없음
  - `500 Internal Server Error`: 서버 오류

#### 예시: 일정 생성 API

**요청:**
```http
POST /api/events HTTP/1.1
Host: 54.66.195.91
Authorization: Bearer {JWT}
X-User-Id: {userId}
Content-Type: application/json

{
  "title": "데이트",
  "description": "저녁 식사",
  "startDate": "2024-01-20T18:00:00",
  "endDate": "2024-01-20T21:00:00",
  "isAllDay": false,
  "location": "서울 강남",
  "reminderMinutes": 30,
  "repeat": "NONE"
}
```

**응답:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "데이트",
  "description": "저녁 식사",
  "startDate": "2024-01-20T18:00:00",
  "endDate": "2024-01-20T21:00:00",
  "isAllDay": false,
  "location": "서울 강남",
  "reminderMinutes": 30,
  "repeat": "NONE"
}
```

**Controller 구현:**
```kotlin
@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {
    @PostMapping
    fun createEvent(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> {
        val response = eventService.createEvent(
            userId = UUID.fromString(userId),
            request = request
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

**Service 구현:**
```kotlin
@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate  // WebSocket
) {
    fun createEvent(userId: UUID, request: CreateEventRequest): EventResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        
        val couple = user.couple 
            ?: throw NotFoundException("Couple not found")
        
        // 1. DB에 일정 저장
        val event = Event(
            couple = couple,
            createdBy = user,
            title = request.title,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            isAllDay = request.isAllDay,
            location = request.location,
            reminderMinutes = request.reminderMinutes,
            repeat = request.repeat
        )
        val savedEvent = eventRepository.save(event)
        val response = EventResponse.from(savedEvent)
        
        // 2. WebSocket 브로드캐스트 (실시간 동기화)
        broadcastScheduleSync(couple.id!!, "ADDED", response, userId)
        
        return response
    }
    
    private fun broadcastScheduleSync(
        coupleId: UUID, 
        eventType: String, 
        event: EventResponse, 
        userId: UUID
    ) {
        val message = ScheduleSyncMessage(
            eventType = eventType,
            schedule = ScheduleDto.from(event),
            userId = userId.toString(),
            timestamp = LocalDateTime.now().toString()
        )
        messagingTemplate.convertAndSend(
            "/topic/couple/$coupleId/schedule", 
            message
        )
    }
}
```

### 5.2 WebSocket 통신 (STOMP)

#### STOMP 프로토콜 구조

```
┌─────────────────────────────────────────────────────────┐
│               STOMP over WebSocket                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Client                        Server                  │
│    │                              │                    │
│    │  1. CONNECT                  │                    │
│    │  (with JWT in headers)       │                    │
│    ├─────────────────────────────►│                    │
│    │                              │ Verify JWT         │
│    │                              │                    │
│    │  2. CONNECTED                │                    │
│    │◄─────────────────────────────┤                    │
│    │                              │                    │
│    │  3. SUBSCRIBE                │                    │
│    │  /topic/couple/{id}/schedule │                    │
│    ├─────────────────────────────►│                    │
│    │                              │                    │
│    │  4. SEND                     │                    │
│    │  /app/chat/{coupleId}        │                    │
│    ├─────────────────────────────►│                    │
│    │                              │ Save to DB         │
│    │                              │                    │
│    │  5. MESSAGE (broadcast)      │                    │
│    │  /topic/couple/{id}          │                    │
│    │◄─────────────────────────────┤                    │
│    │                              │                    │
│    │  6. DISCONNECT               │                    │
│    ├─────────────────────────────►│                    │
│    │                              │                    │
└─────────────────────────────────────────────────────────┘
```

#### WebSocket 설정

```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthInterceptor: WebSocketAuthInterceptor,
    private val stompConnectInterceptor: StompConnectInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 순수 WebSocket 엔드포인트
        registry.addEndpoint("/ws/stomp")
            .setAllowedOriginPatterns("*")
            .addInterceptors(webSocketAuthInterceptor)  // JWT 검증
        
        // SockJS 폴백 엔드포인트
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(webSocketAuthInterceptor)
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")  // 구독 prefix
        registry.setApplicationDestinationPrefixes("/app")  // 메시지 전송 prefix
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompConnectInterceptor)  // CONNECT 프레임 인증
    }
}
```

#### 채팅 메시지 전송 예시

**Client → Server (SEND):**
```
SEND
destination:/app/chat/550e8400-e29b-41d4-a716-446655440000
content-type:application/json

{
  "content": "안녕하세요!",
  "type": "TEXT",
  "tempId": "temp-123"
}
```

**Server → Clients (MESSAGE):**
```
MESSAGE
destination:/topic/couple/550e8400-e29b-41d4-a716-446655440000
content-type:application/json

{
  "id": "msg-uuid",
  "senderId": "user-uuid",
  "content": "안녕하세요!",
  "type": "TEXT",
  "createdAt": "2024-01-20T18:00:00",
  "tempId": "temp-123"
}
```

**Controller 구현:**
```kotlin
@Controller
class ChatWebSocketController(
    private val chatWebSocketService: ChatWebSocketService
) {
    @MessageMapping("/chat/{coupleId}")
    @SendTo("/topic/couple/{coupleId}")
    fun sendMessage(
        @DestinationVariable coupleId: String,
        @Payload request: WebSocketMessageRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ): Any {
        val userId = headerAccessor.sessionAttributes?.get("userId") as? UUID
            ?: throw IllegalArgumentException("인증되지 않은 사용자입니다")

        // 메시지 저장 및 처리
        val response = chatWebSocketService.sendMessage(
            coupleId = UUID.fromString(coupleId),
            senderId = userId,
            request = request
        )

        return response  // 구독자들에게 브로드캐스트
    }
}
```

---

## 6. 암호화 시스템

### 6.1 E2EE (End-to-End Encryption) 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                    E2EE 암호화 흐름                               │
└──────────────────────────────────────────────────────────────────┘

Device A                   Server                      Device B
   │                         │                            │
   │ 1. 공개키/비밀키 쌍 생성 │                            │
   │    (RSA 2048-bit)       │                            │
   │                         │                            │
   │ 2. 공개키 업로드         │                            │
   ├────────────────────────►│                            │
   │   POST /api/users/      │                            │
   │        me/public-key    │                            │
   │                         │ ✅ DB 저장                  │
   │                         │   (user.publicKey)         │
   │                         │                            │
   │ 3. 커플 연결             │                            │
   ├────────────────────────►│                            │
   │   POST /api/couples/    │                            │
   │        connect          │                            │
   │                         │                            │
   │ 4. 대칭키 생성           │                            │
   │    (AES-256 GCM)        │                            │
   │                         │                            │
   │ 5. 대칭키 암호화         │                            │
   │    - A 공개키로 암호화   │                            │
   │    - B 공개키로 암호화   │                            │
   │                         │                            │
   │ 6. 암호화된 대칭키 전송  │                            │
   ├────────────────────────►│                            │
   │   POST /api/couples/    │                            │
   │        me/shared-key    │                            │
   │                         │ ✅ DB 저장                  │
   │                         │   (couple.encryptedSharedKeyUser1)│
   │                         │   (couple.encryptedSharedKeyUser2)│
   │                         │                            │
   │                         │ 7. B가 자신의 대칭키 조회   │
   │                         │◄───────────────────────────┤
   │                         │   GET /api/couples/        │
   │                         │       me/shared-key        │
   │                         │                            │
   │                         │ 8. 암호화된 대칭키 반환     │
   │                         ├───────────────────────────►│
   │                         │                            │
   │                         │ 9. B 비밀키로 복호화       │
   │                         │                            │◄─┐
   │                         │                            │  │ AES 대칭키 획득
   │                         │                            │──┘
   │                         │                            │
   │ 10. 메시지 암호화        │                            │
   │     (AES-256 GCM)       │                            │
   │                         │                            │
   │ 11. 암호화된 메시지 전송 │                            │
   ├────────────────────────►│                            │
   │   SEND /app/chat/{id}   │                            │
   │   {                     │                            │
   │     encryptedContent,   │                            │
   │     iv,                 │                            │
   │     isEncrypted: true   │                            │
   │   }                     │                            │
   │                         │ ✅ 암호문만 DB 저장         │
   │                         │   (평문 저장 안 함!)        │
   │                         │                            │
   │                         │ 12. 암호문 브로드캐스트     │
   │                         ├───────────────────────────►│
   │                         │   MESSAGE /topic/couple/{id}│
   │                         │                            │
   │                         │ 13. AES 대칭키로 복호화    │
   │                         │                            │◄─┐
   │                         │                            │  │ 평문 획득
   │                         │                            │──┘
```

### 6.2 구현 세부사항

#### 공개키 관리 API

**공개키 저장:**
```kotlin
@RestController
@RequestMapping("/api/users")
class PublicKeyController(
    private val userRepository: UserRepository
) {
    @PutMapping("/me/public-key")
    fun updatePublicKey(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: PublicKeyRequest
    ): ResponseEntity<PublicKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NotFoundException("User not found") }

        user.publicKey = request.publicKey  // Base64 인코딩된 RSA 공개키
        userRepository.save(user)

        return ResponseEntity.ok(PublicKeyResponse(
            publicKey = user.publicKey,
            hasPublicKey = true
        ))
    }
}
```

**상대방 공개키 조회:**
```kotlin
@GetMapping("/partner/public-key")
fun getPartnerPublicKey(
    @RequestHeader("X-User-Id") userId: String
): ResponseEntity<PublicKeyResponse> {
    val user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow { NotFoundException("User not found") }

    val couple = user.couple 
        ?: throw NotFoundException("Couple not found")

    // 상대방 ID 찾기
    val partnerId = if (couple.user1Id == user.id) {
        couple.user2Id ?: throw NotFoundException("Partner not connected")
    } else {
        couple.user1Id
    }

    val partner = userRepository.findById(partnerId)
        .orElseThrow { NotFoundException("Partner not found") }

    return ResponseEntity.ok(PublicKeyResponse(
        publicKey = partner.publicKey,
        hasPublicKey = partner.publicKey != null
    ))
}
```

#### 공유 대칭키 관리 API

**대칭키 저장:**
```kotlin
@RestController
@RequestMapping("/api/couples")
class SharedKeyController(
    private val coupleRepository: CoupleRepository,
    private val userRepository: UserRepository
) {
    @PostMapping("/me/shared-key")
    fun setSharedKey(
        @RequestHeader("X-User-Id") userId: String,
        @RequestBody request: SharedKeyRequest  // { encryptedSharedKey }
    ): ResponseEntity<SharedKeyResponse> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NotFoundException("User not found") }

        val couple = user.couple 
            ?: throw NotFoundException("Couple not found")

        // user1인지 user2인지 확인하여 해당 필드에 저장
        if (couple.user1Id == user.id) {
            couple.encryptedSharedKeyUser1 = request.encryptedSharedKey
        } else if (couple.user2Id == user.id) {
            couple.encryptedSharedKeyUser2 = request.encryptedSharedKey
        }

        coupleRepository.save(couple)

        return ResponseEntity.ok(SharedKeyResponse(
            encryptedSharedKey = request.encryptedSharedKey,
            hasSharedKey = true
        ))
    }
}
```

**대칭키 조회:**
```kotlin
@GetMapping("/me/shared-key")
fun getSharedKey(
    @RequestHeader("X-User-Id") userId: String
): ResponseEntity<SharedKeyResponse> {
    val user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow { NotFoundException("User not found") }

    val couple = user.couple 
        ?: throw NotFoundException("Couple not found")

    // 자신의 공개키로 암호화된 대칭키 조회
    val encryptedKey = when (user.id) {
        couple.user1Id -> couple.encryptedSharedKeyUser1
        couple.user2Id -> couple.encryptedSharedKeyUser2
        else -> null
    }

    return ResponseEntity.ok(SharedKeyResponse(
        encryptedSharedKey = encryptedKey,
        hasSharedKey = encryptedKey != null
    ))
}
```

#### 암호화된 메시지 저장

**ChatMessage 엔티티:**
```kotlin
@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: Couple,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,
    
    @Column(columnDefinition = "text")
    var content: String? = null,  // 평문 (암호화 시 null)
    
    @Enumerated(EnumType.STRING)
    var type: MessageType = MessageType.TEXT,
    
    // E2EE 관련 필드
    @Column(name = "is_encrypted")
    var isEncrypted: Boolean = false,
    
    @Column(name = "encrypted_content", columnDefinition = "text")
    var encryptedContent: String? = null,  // AES 암호화된 메시지 (Base64)
    
    @Column(name = "encrypted_key", columnDefinition = "text")
    var encryptedKey: String? = null,  // RSA로 암호화된 세션키 (Base64)
    
    @Column(name = "iv", length = 500)
    var iv: String? = null,  // AES 초기화 벡터 (Base64)
    
    @Column(name = "is_read")
    var isRead: Boolean = false,
    
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

**메시지 저장 로직:**
```kotlin
@Service
class ChatWebSocketService(
    private val chatMessageRepository: ChatMessageRepository
) {
    fun sendMessage(
        coupleId: UUID,
        senderId: UUID,
        request: WebSocketMessageRequest
    ): WebSocketMessageResponse {
        val message = ChatMessage(
            couple = couple,
            sender = sender,
            content = if (request.isEncrypted) null else request.content,
            type = request.type,
            isEncrypted = request.isEncrypted,
            encryptedContent = request.encryptedContent,
            encryptedKey = request.encryptedKey,
            iv = request.iv
        )
        
        val savedMessage = chatMessageRepository.save(message)
        
        return WebSocketMessageResponse(
            id = savedMessage.id.toString(),
            senderId = savedMessage.sender.id.toString(),
            content = savedMessage.content,  // 평문은 null
            encryptedContent = savedMessage.encryptedContent,  // 암호문만 반환
            encryptedKey = savedMessage.encryptedKey,
            iv = savedMessage.iv,
            isEncrypted = savedMessage.isEncrypted,
            type = savedMessage.type,
            createdAt = savedMessage.createdAt.toString(),
            tempId = request.tempId
        )
    }
}
```

### 6.3 보안 특징

| 항목 | 설명 |
|-----|------|
| **알고리즘** | RSA-2048 (공개키), AES-256-GCM (대칭키) |
| **키 저장** | • 공개키: DB 저장 (Base64)<br>• 비밀키: 클라이언트만 보관 (서버 미저장)<br>• 대칭키: 암호화되어 DB 저장 |
| **평문 노출** | 서버는 암호문만 저장, 평문 접근 불가 |
| **Forward Secrecy** | 세션키 탈취 시에도 과거 메시지 안전 |
| **Man-in-the-Middle 방어** | HTTPS + JWT 인증으로 중간자 공격 방어 |

---

## 7. WebSocket 실시간 동기화

### 7.1 동기화 대상 기능

| 기능 | 토픽 | 이벤트 타입 |
|-----|------|-----------|
| **채팅** | `/topic/couple/{coupleId}` | MESSAGE, READ |
| **일정** | `/topic/couple/{coupleId}/schedule` | ADDED, UPDATED, DELETED |
| **버킷리스트** | `/topic/couple/{coupleId}/bucket` | ADDED, COMPLETED, UPDATED, DELETED |
| **재무** | `/topic/couple/{coupleId}/finance` | BUDGET_UPDATED, EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED |
| **기념일** | `/topic/couple/{coupleId}/anniversary` | ANNIVERSARY_UPDATED |

### 7.2 브로드캐스트 패턴

**Service Layer에서 브로드캐스트:**
```kotlin
@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val messagingTemplate: SimpMessagingTemplate  // WebSocket
) {
    fun createEvent(userId: UUID, request: CreateEventRequest): EventResponse {
        // 1. DB 저장
        val savedEvent = eventRepository.save(event)
        val response = EventResponse.from(savedEvent)
        
        // 2. WebSocket 브로드캐스트
        broadcastScheduleSync(couple.id!!, "ADDED", response, userId)
        
        return response
    }
    
    private fun broadcastScheduleSync(
        coupleId: UUID,
        eventType: String,
        event: EventResponse,
        userId: UUID
    ) {
        val message = ScheduleSyncMessage(
            eventType = eventType,
            schedule = ScheduleDto.from(event),
            userId = userId.toString(),
            timestamp = LocalDateTime.now().toString()
        )
        
        // SimpMessagingTemplate을 사용하여 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/couple/$coupleId/schedule",
            message
        )
    }
}
```

**메시지 형식 (ScheduleSyncMessage):**
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

### 7.3 클라이언트 구독 예시

**Android Kotlin 코드:**
```kotlin
class ChatWebSocketClient(
    private val stompClient: StompClient,
    private val gson: Gson
) {
    fun subscribeToScheduleSync(coupleId: String) {
        stompSession?.subscribe("/topic/couple/$coupleId/schedule") { message ->
            val syncMessage = gson.fromJson(
                message.payload,
                ScheduleSyncMessage::class.java
            )
            
            // Repository에 전달하여 StateFlow 업데이트
            listener?.onScheduleSync(syncMessage)
        }
    }
}

// Repository에서 처리
class EventRepositoryImpl : EventRepository {
    private val _schedules = MutableStateFlow<List<EventDto>>(emptyList())
    val schedules: StateFlow<List<EventDto>> = _schedules
    
    fun handleScheduleSync(message: ScheduleSyncMessage) {
        when (message.eventType) {
            ScheduleEventType.ADDED -> {
                val currentList = _schedules.value.toMutableList()
                currentList.add(message.schedule)
                _schedules.value = currentList
            }
            ScheduleEventType.UPDATED -> {
                val currentList = _schedules.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == message.schedule.id }
                if (index != -1) {
                    currentList[index] = message.schedule
                    _schedules.value = currentList
                }
            }
            ScheduleEventType.DELETED -> {
                val currentList = _schedules.value.toMutableList()
                currentList.removeIf { it.id == message.schedule.id }
                _schedules.value = currentList
            }
        }
    }
}
```

---

## 8. 배포 및 인프라

### 8.1 배포 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│                AWS EC2 (54.66.195.91)                    │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Docker Compose Network                     │ │
│  │                                                    │ │
│  │  ┌──────────────┐   ┌──────────────┐             │ │
│  │  │    Nginx     │   │  PostgreSQL  │             │ │
│  │  │   (Proxy)    │   │    (DB)      │             │ │
│  │  │   Port 80    │   │  Port 5432   │             │ │
│  │  └──────┬───────┘   └──────▲───────┘             │ │
│  │         │                   │                     │ │
│  │         │ Proxy             │ JDBC                │ │
│  │         ▼                   │                     │ │
│  │  ┌──────────────────────────┴───────┐            │ │
│  │  │      Spring Boot App             │            │ │
│  │  │      (Port 8080)                 │            │ │
│  │  │  - REST API                      │            │ │
│  │  │  - WebSocket (/ws/stomp)         │            │ │
│  │  │  - JWT Authentication            │            │ │
│  │  └──────────────────────────────────┘            │ │
│  │                                                   │ │
│  └───────────────────────────────────────────────────┘ │
│                                                          │
└──────────────────────────────────────────────────────────┘
              ▲
              │ HTTP / WebSocket
              │
      ┌───────┴────────┐
      │  Android App   │
      │   (Client)     │
      └────────────────┘
```

### 8.2 Docker Compose 설정

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  # PostgreSQL 데이터베이스
  db:
    image: postgres:15
    container_name: postgres-db
    environment:
      POSTGRES_DB: ieum_db
      POSTGRES_USER: hjxarchive
      POSTGRES_PASSWORD: "ieum2580-!"
    ports:
      - "5432:5432"
    volumes:
      - ./postgres_data:/var/lib/postgresql/data  # 데이터 영속화
    networks:
      - ieum-network

  # Spring Boot 애플리케이션
  app:
    build: .
    container_name: spring-app
    depends_on:
      - db  # DB가 먼저 시작
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/ieum_db
      SPRING_DATASOURCE_USERNAME: hjxarchive
      SPRING_DATASOURCE_PASSWORD: "ieum2580-!"
      GOOGLE_CLIENT_ID: {GOOGLE_CLIENT_ID}
      JWT_SECRET: {JWT_SECRET}
    networks:
      - ieum-network

  # Nginx 리버스 프록시
  nginx:
    image: nginx:latest
    container_name: nginx-proxy
    ports:
      - "80:80"  # HTTP
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - app
    networks:
      - ieum-network

networks:
  ieum-network:
    driver: bridge
```

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Nginx 설정 (nginx/default.conf):**
```nginx
upstream spring-boot {
    server spring-app:8080;
}

server {
    listen 80;
    server_name 54.66.195.91;

    # REST API 프록시
    location /api/ {
        proxy_pass http://spring-boot;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 프록시
    location /ws/ {
        proxy_pass http://spring-boot;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health Check
    location /health {
        proxy_pass http://spring-boot/api/health;
    }
}
```

### 8.3 배포 프로세스

**1. 로컬 빌드:**
```bash
./gradlew clean build -x test
```

**2. EC2로 전송:**
```bash
rsync -avz --delete \
  --exclude '.git' \
  --exclude 'node_modules' \
  --exclude 'postgres_data' \
  . ubuntu@54.66.195.91:~/madcamp_W2_ieum_backend/
```

**3. EC2에서 배포:**
```bash
ssh ubuntu@54.66.195.91
cd ~/madcamp_W2_ieum_backend
docker compose down
docker compose up --build -d
```

**4. 헬스 체크:**
```bash
curl http://54.66.195.91/api/health
# 응답: "이음(ieum) 서버가 정상적으로 응답하고 있습니다!"
```

**5. 로그 확인:**
```bash
docker logs -f spring-app
```

---

## 9. API 엔드포인트

### 9.1 인증 (Auth)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| POST | `/api/auth/google` | Google OAuth 로그인 | ❌ |
| GET | `/api/auth/me` | 현재 사용자 정보 조회 | ✅ |
| POST | `/api/auth/logout` | 로그아웃 | ✅ |

### 9.2 사용자 (Users)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| GET | `/api/users/me` | 내 정보 조회 | ✅ |
| PUT | `/api/users/me` | 내 정보 수정 | ✅ |
| PUT | `/api/users/me/public-key` | 공개키 저장 (E2EE) | ✅ |
| GET | `/api/users/me/public-key` | 내 공개키 조회 | ✅ |
| GET | `/api/users/partner/public-key` | 상대방 공개키 조회 | ✅ |

### 9.3 커플 (Couples)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| POST | `/api/couples` | 커플 초대 코드 생성 | ✅ |
| POST | `/api/couples/connect` | 초대 코드로 연결 | ✅ |
| GET | `/api/couples/me` | 내 커플 정보 조회 | ✅ |
| PUT | `/api/couples` | 커플 정보 수정 (기념일) | ✅ |
| POST | `/api/couples/me/shared-key` | 공유 대칭키 저장 (E2EE) | ✅ |
| GET | `/api/couples/me/shared-key` | 공유 대칭키 조회 (E2EE) | ✅ |

### 9.4 일정 (Events)

| 메서드 | 엔드포인트 | 설명 | WebSocket | 인증 |
|-------|----------|------|-----------|-----|
| POST | `/api/events` | 일정 생성 | ✅ ADDED | ✅ |
| GET | `/api/events` | 일정 목록 조회 | ❌ | ✅ |
| GET | `/api/events/{id}` | 일정 상세 조회 | ❌ | ✅ |
| PUT | `/api/events/{id}` | 일정 수정 | ✅ UPDATED | ✅ |
| DELETE | `/api/events/{id}` | 일정 삭제 | ✅ DELETED | ✅ |

**WebSocket 토픽:** `/topic/couple/{coupleId}/schedule`

### 9.5 버킷리스트 (Bucket)

| 메서드 | 엔드포인트 | 설명 | WebSocket | 인증 |
|-------|----------|------|-----------|-----|
| POST | `/api/buckets` | 버킷 생성 | ✅ ADDED | ✅ |
| GET | `/api/buckets` | 버킷 목록 조회 | ❌ | ✅ |
| PUT | `/api/buckets/{id}` | 버킷 수정 | ✅ UPDATED | ✅ |
| PUT | `/api/buckets/{id}/complete` | 버킷 완료 | ✅ COMPLETED | ✅ |
| DELETE | `/api/buckets/{id}` | 버킷 삭제 | ✅ DELETED | ✅ |

**WebSocket 토픽:** `/topic/couple/{coupleId}/bucket`

### 9.6 재무 (Finance)

| 메서드 | 엔드포인트 | 설명 | WebSocket | 인증 |
|-------|----------|------|-----------|-----|
| PUT | `/api/finance/budget/{yearMonth}` | 예산 설정 | ✅ BUDGET_UPDATED | ✅ |
| GET | `/api/finance/budget/{yearMonth}` | 예산 조회 | ❌ | ✅ |
| POST | `/api/finance/expenses` | 지출 추가 | ✅ EXPENSE_ADDED | ✅ |
| GET | `/api/finance/expenses` | 지출 목록 조회 | ❌ | ✅ |
| PUT | `/api/finance/expenses/{id}` | 지출 수정 | ✅ EXPENSE_UPDATED | ✅ |
| DELETE | `/api/finance/expenses/{id}` | 지출 삭제 | ✅ EXPENSE_DELETED | ✅ |

**WebSocket 토픽:** `/topic/couple/{coupleId}/finance`

### 9.7 채팅 (Chat - WebSocket)

| 목적지 | 설명 |
|-------|------|
| `SEND /app/chat/{coupleId}` | 메시지 전송 |
| `SUBSCRIBE /topic/couple/{coupleId}` | 메시지 수신 |
| `SEND /app/chat/{coupleId}/read` | 읽음 처리 |
| `SUBSCRIBE /topic/couple/{coupleId}/read` | 읽음 상태 수신 |
| `SEND /app/chat/{coupleId}/typing` | 타이핑 중 전송 |
| `SUBSCRIBE /topic/couple/{coupleId}/typing` | 타이핑 중 수신 |

### 9.8 추억 (Memory)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| POST | `/api/memories` | 추억 생성 | ✅ |
| GET | `/api/memories` | 추억 목록 조회 | ✅ |
| GET | `/api/memories/{id}` | 추억 상세 조회 | ✅ |
| DELETE | `/api/memories/{id}` | 추억 삭제 | ✅ |

### 9.9 AI 추천 (Recommendation)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| POST | `/api/recommendations/date` | 데이트 코스 추천 | ✅ |
| POST | `/api/recommendations/gift` | 선물 추천 | ✅ |

### 9.10 MBTI

| 메서드 | 엔드포인트 | 설명 | 인증 |
|-------|----------|------|-----|
| GET | `/api/mbti/questions` | MBTI 설문 조회 | ✅ |
| POST | `/api/mbti/submit` | MBTI 응답 제출 | ✅ |
| GET | `/api/mbti/compatibility` | 커플 궁합 분석 | ✅ |

---

## 10. 성능 최적화

### 10.1 JPA 최적화

```kotlin
// N+1 문제 방지: Fetch Join 사용
@Query("""
    SELECT e FROM Event e
    LEFT JOIN FETCH e.couple
    LEFT JOIN FETCH e.createdBy
    WHERE e.couple = :couple
    AND e.deletedAt IS NULL
    AND e.startDate BETWEEN :startDate AND :endDate
""")
fun findByCoupleAndDateRange(
    couple: Couple,
    startDate: LocalDateTime,
    endDate: LocalDateTime
): List<Event>

// 불필요한 조회 방지: @Transactional(readOnly = true)
@Transactional(readOnly = true)
fun getEvents(userId: UUID, startDate: LocalDateTime, endDate: LocalDateTime): EventListResponse
```

### 10.2 WebSocket 성능

- **SimpleBroker**: 메모리 기반으로 빠른 브로드캐스트
- **연결 재사용**: 하나의 WebSocket 연결로 모든 토픽 구독
- **메시지 크기 제한**: 128KB (설정 가능)

### 10.3 데이터베이스 연결 풀

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 최대 연결 수
      minimum-idle: 5            # 최소 유휴 연결
      connection-timeout: 30000  # 30초
```

---

## 11. 트러블슈팅

### 11.1 WebSocket 연결 실패

**증상:** 클라이언트가 WebSocket에 연결하지 못함

**원인 및 해결:**
1. **JWT 토큰 문제**
   - 확인: `ws://server/ws/stomp?token={JWT}` 형식 확인
   - 해결: 토큰 만료 확인, 새 토큰 발급

2. **Nginx 프록시 설정**
   - 확인: `Upgrade`, `Connection` 헤더 전달 확인
   - 해결: Nginx 설정에 WebSocket 프록시 추가

3. **STOMP CONNECT 타임아웃**
   - 확인: `timeToFirstMessage` 설정 (기본 60초)
   - 해결: 클라이언트가 연결 후 60초 내에 CONNECT 프레임 전송

### 11.2 실시간 동기화 안 됨

**증상:** 한 기기에서 데이터 변경 시 다른 기기에 반영 안 됨

**원인 및 해결:**
1. **서버가 브로드캐스트 안 함**
   - 확인: 서버 로그에서 `convertAndSend` 호출 확인
   - 해결: Service 클래스에 브로드캐스트 코드 추가

2. **클라이언트가 구독 안 함**
   - 확인: `SUBSCRIBE` 프레임 전송 확인
   - 해결: WebSocket 연결 후 토픽 구독

3. **CoupleId 불일치**
   - 확인: 서버 토픽과 클라이언트 구독 토픽 일치 확인
   - 해결: 정확한 coupleId 사용

### 11.3 데이터베이스 연결 오류

**증상:** `Connection refused` 또는 `Unknown database`

**원인 및 해결:**
1. **PostgreSQL 미실행**
   - 확인: `docker ps | grep postgres`
   - 해결: `docker compose up -d db`

2. **환경 변수 오류**
   - 확인: `SPRING_DATASOURCE_URL` 확인
   - 해결: `docker-compose.yml`에서 환경 변수 수정

---

## 12. 향후 개선 사항

### 12.1 단기 개선
- [ ] **Redis 도입**: 세션 관리, 토큰 블랙리스트
- [ ] **S3 파일 업로드**: 이미지 파일 클라우드 저장
- [ ] **Push Notification**: FCM 통합

### 12.2 중기 개선
- [ ] **Spring Security**: 체계적인 인증/인가 관리
- [ ] **Elasticsearch**: 채팅 메시지 전문 검색
- [ ] **RabbitMQ/Kafka**: 확장성 있는 메시지 브로커

### 12.3 장기 개선
- [ ] **Kubernetes**: 컨테이너 오케스트레이션
- [ ] **MSA 전환**: 기능별 마이크로서비스 분리
- [ ] **GraphQL**: 효율적인 데이터 페칭

---

## 13. 참고 자료

### 공식 문서
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### 보안
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [End-to-End Encryption Best Practices](https://www.ncsc.gov.uk/guidance/end-user-devices-security-guidance-introduction)

---

**작성자**: ieum 개발팀  
**최종 수정**: 2026년 1월 21일  
**버전**: 1.0.0
