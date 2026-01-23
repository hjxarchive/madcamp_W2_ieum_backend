# 이음(Ieum) 백엔드 기술 문서

## 📚 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [아키텍처 설계](#아키텍처-설계)
4. [보안 시스템](#보안-시스템)
5. [데이터베이스 설계](#데이터베이스-설계)
6. [API 명세](#api-명세)
7. [실시간 통신](#실시간-통신)
8. [배포 및 인프라](#배포-및-인프라)

---

## 프로젝트 개요

**이음(Ieum)**은 커플을 위한 종합 관리 플랫폼으로, 연애 관계에서 필요한 다양한 기능을 제공합니다.

### 핵심 기능
- 🔐 **Google OAuth 2.0 기반 소셜 로그인**
- 💑 **초대 코드 기반 커플 매칭**
- 🔒 **End-to-End 암호화 채팅**
- 📅 **공유 일정 및 기념일 관리**
- 💰 **커플 가계부 (수입/지출 추적)**
- 🎯 **버킷리스트 및 추억 저장**
- 🧬 **연애 스타일 MBTI 테스트 (36문항)**
- 🔔 **WebSocket 실시간 알림**

---

## 기술 스택

### Backend Framework
```yaml
Language: Kotlin 1.9.25
Framework: Spring Boot 3.5.9
Build Tool: Gradle 8.14.3
JDK: Eclipse Temurin 21
```

### Core Dependencies
| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Spring Data JPA | 3.5.9 | ORM, 데이터 영속성 관리 |
| Spring Web | 3.5.9 | RESTful API 구현 |
| Spring Validation | 3.5.9 | 요청 데이터 검증 |
| Spring WebSocket | 3.5.9 | 실시간 양방향 통신 |
| PostgreSQL | 15 | 관계형 데이터베이스 |
| Google API Client | 2.2.0 | Google OAuth 토큰 검증 |
| JJWT | 0.12.3 | JWT 토큰 생성/검증 |
| Jackson Kotlin | - | JSON 직렬화/역직렬화 |

### Infrastructure
```yaml
Database: PostgreSQL 15
Container: Docker & Docker Compose
Web Server: Nginx (Reverse Proxy)
Cloud: AWS EC2 (Ubuntu 22.04)
```

---

## 아키텍처 설계

### 계층형 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                  │
│              (Controllers, DTOs, Filters)            │
├─────────────────────────────────────────────────────┤
│                   Business Layer                     │
│                   (Services, Logic)                  │
├─────────────────────────────────────────────────────┤
│                  Persistence Layer                   │
│             (Repositories, Entities)                 │
├─────────────────────────────────────────────────────┤
│                   Database Layer                     │
│                   (PostgreSQL 15)                    │
└─────────────────────────────────────────────────────┘
```

### 디렉토리 구조

```
src/main/kotlin/com/ieum/ieum_back/
├── auth/                    # 인증/인가
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── JwtProvider.kt       # JWT 토큰 생성/검증
│   └── JwtAuthFilter.kt     # JWT 인증 필터
├── users/                   # 사용자 관리
├── couples/                 # 커플 관리
├── chat/                    # 실시간 채팅
├── events/                  # 일정 관리
├── finance/                 # 가계부 (예산/지출)
├── bucket/                  # 버킷리스트
├── memory/                  # 추억 저장
├── mbti/                    # 연애 MBTI 테스트
├── recommendation/          # AI 추천
├── files/                   # 파일 업로드
├── entity/                  # JPA 엔티티
├── repository/              # 데이터 액세스
├── config/                  # 설정 (CORS, WebSocket 등)
├── exception/               # 예외 처리
└── common/                  # 공통 유틸리티
```

### 주요 설계 패턴

#### 1. **Dependency Injection (DI)**
Spring의 생성자 주입 방식 사용:
```kotlin
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService  // 생성자 주입
) {
    // ...
}
```

#### 2. **DTO Pattern**
엔티티와 API 응답 분리로 캡슐화 보장:
```kotlin
data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String,
    val mbtiType: String?
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id!!,
            email = user.email,
            name = user.name,
            mbtiType = user.mbtiType
        )
    }
}
```

#### 3. **Repository Pattern**
Spring Data JPA의 Repository 인터페이스 활용:
```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByGoogleId(googleId: String): User?
}
```

#### 4. **Strategy Pattern**
다양한 Google Client ID 지원을 위한 설정 기반 인증:
```kotlin
@Value("\${google.client-ids}")
private val googleClientIds: List<String>

private val verifier = GoogleIdTokenVerifier.Builder(/* ... */)
    .setAudience(googleClientIds)  // 웹, Android 1, Android 2
    .build()
```

---

## 보안 시스템

### 1. 인증(Authentication) 및 보안 통신 흐름

```
┌─────────────┐                                    ┌─────────────┐
│   Client    │         HTTPS/TLS 보안 통신        │   Google    │
│   (App)     │◄──────────────────────────────────►│   OAuth     │
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │ ①Google OAuth Login (HTTPS)                     │
       ├─────────────────────────────────────────────────►│
       │                                                  │
       │ ②ID Token (RSA-2048 서명됨)                     │
       │◄─────────────────────────────────────────────────┤
       │                                                  │
       │ ③POST /api/auth/google                          │
       │   HTTPS + JSON                                   │
       │   {idToken: "eyJhbGc..."}                       │
       ▼                                                  │
┌──────────────────────────────────────────────────────────────┐
│                    Backend Server (Nginx + Spring)            │
│                                                               │
│  🔒 TLS/HTTPS Termination (Nginx)                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ ④Google Token Verification                           │   │
│  │   - RSA Signature 검증 (Google Public Key)           │   │
│  │   - Audience/Issuer/Expiration 확인                  │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ ⑤User Lookup/Creation (PostgreSQL)                   │   │
│  │   - googleId 기반 조회/생성                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ ⑥JWT Token Generation                                │   │
│  │   - HS256 (HMAC-SHA256)                              │   │
│  │   - 7일 만료, userId/email 포함                      │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         │ ⑦Response (HTTPS)
                         │ {accessToken: "eyJ...", user: {...}}
                         ▼
                   ┌─────────────┐
                   │   Client    │
                   │ (JWT 저장)  │
                   └─────────────┘
                         │
                         │ ⑧Subsequent Requests (HTTPS)
                         │ Authorization: Bearer {JWT}
                         ▼
              [모든 API 요청에 JWT 사용]

보안 계층:
  🔒 Transport: HTTPS/TLS 1.2+
  🔐 Token: Google ID Token (RSA-2048) + JWT (HS256)
  🛡️ Storage: Secure Storage (Keychain/Keystore)
```

### 2. Google OAuth 2.0 구현

#### 검증 프로세스
```kotlin
private fun verifyGoogleToken(idToken: String): GoogleUserInfo {
    logger.info("🔍 Verifying Google ID Token")
    
    val googleIdToken = try {
        verifier.verify(idToken)  // Google 서명 검증
    } catch (e: Exception) {
        throw UnauthorizedException("Invalid Google token")
    }
    
    if (googleIdToken == null) {
        // 검증 실패 사유:
        // - 만료된 토큰
        // - 잘못된 Audience (aud)
        // - 잘못된 Issuer (iss)
        // - 서명 불일치
        throw UnauthorizedException("Invalid Google token")
    }
    
    val payload = googleIdToken.payload
    return GoogleUserInfo(
        googleId = payload.subject,
        email = payload.email,
        name = payload["name"] as? String,
        profileImage = payload["picture"] as? String
    )
}
```

#### 다중 Client ID 지원
```yaml
# application.yaml
google:
  client-ids:
    - 1088305482605-hfqq6q54rf00bns3l6bnamu9gl3cg27p...  # 웹
    - 1088305482605-6u7jg08cfc7p2omhk31t0iatkqfqbeag...  # Android 1
    - 1088305482605-n4fnsouvlkf5md5ej2nllvpjuev666dd...  # Android 2
```

### 3. JWT (JSON Web Token) 구현

#### 토큰 구조
```
Header
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload
{
  "sub": "6d2485e8-0837-40cf-9abe-6e464ac45605",  // userId
  "iat": 1737360533,                              // 발급 시간
  "exp": 1768896533                               // 만료 시간 (7일 후)
}

Signature
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

#### 토큰 생성 코드
```kotlin
@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.expiration}")
    private val expiration: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(userId: UUID, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
```

### 4. JWT 인증 필터

```kotlin
@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Authorization 헤더에서 토큰 추출
            val token = extractTokenFromRequest(request)
            
            if (token != null && jwtProvider.validateToken(token)) {
                // 토큰에서 사용자 ID 추출
                val userId = jwtProvider.getUserIdFromToken(token)
                val user = userRepository.findById(UUID.fromString(userId))
                
                if (user.isPresent) {
                    // SecurityContext에 인증 정보 설정
                    val authentication = UsernamePasswordAuthenticationToken(
                        userId, null, emptyList()
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            logger.error("JWT authentication failed", e)
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken?.startsWith("Bearer ") == true) {
            bearerToken.substring(7)
        } else null
    }
}
```

### 5. End-to-End 암호화 채팅

#### RSA 키 교환 방식
```
User A                          Backend                         User B
  │                               │                               │
  │ ①Generate RSA Key Pair        │                               │
  │ (Public Key, Private Key)     │                               │
  │                               │                               │
  │ ②POST /api/users/public-key  │                               │
  │   {publicKey: "RSA_PUB_A"}    │                               │
  ├──────────────────────────────>│                               │
  │                               │ Store in DB                   │
  │                               │                               │
  │                               │  ③POST /api/users/public-key  │
  │                               │    {publicKey: "RSA_PUB_B"}   │
  │                               │<──────────────────────────────┤
  │                               │ Store in DB                   │
  │                               │                               │
  │ ④Generate AES Shared Key      │                               │
  │ AES_KEY = random(256bit)      │                               │
  │                               │                               │
  │ ⑤Encrypt AES with RSA_PUB_A   │                               │
  │ ⑥Encrypt AES with RSA_PUB_B   │                               │
  │                               │                               │
  │ ⑦POST /api/couples/shared-key │                               │
  │   {encryptedForUser1: "...",  │                               │
  │    encryptedForUser2: "..."}  │                               │
  ├──────────────────────────────>│                               │
  │                               │ Store Encrypted Keys          │
  │                               │                               │
  │ ⑧Send Message                 │                               │
  │ Encrypted = AES(message)      │                               │
  ├──────────────────────────────>│──────────────────────────────>│
  │                               │   ⑨Receive Encrypted Message  │
  │                               │   Decrypt with AES Key        │
```

#### 공개키 등록 API
```kotlin
@PostMapping("/public-key")
fun registerPublicKey(
    @CurrentUser userId: UUID,
    @RequestBody request: PublicKeyRequest
): ResponseEntity<Map<String, String>> {
    // RSA 공개키 저장
    userService.updatePublicKey(userId, request.publicKey)
    return ResponseEntity.ok(mapOf("message" to "Public key registered"))
}
```

#### 공유키 암호화 저장
```kotlin
@PostMapping("/shared-key")
fun saveSharedKey(
    @CurrentUser userId: UUID,
    @RequestBody request: SharedKeyRequest
): ResponseEntity<Map<String, String>> {
    // 각 사용자의 RSA 공개키로 암호화된 AES 키 저장
    coupleService.saveEncryptedSharedKey(
        userId, 
        request.encryptedForUser1,
        request.encryptedForUser2
    )
    return ResponseEntity.ok(mapOf("message" to "Shared key saved"))
}
```

### 6. CORS 설정

```kotlin
@Configuration
class CorsConfig {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("*")  // 프로덕션에서는 특정 도메인만 허용
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = false
        }
        
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
```

---

## 데이터베이스 설계

### ERD (Entity Relationship Diagram)

```
┌─────────────────┐          ┌─────────────────┐
│     users       │          │    couples      │
├─────────────────┤          ├─────────────────┤
│ id (PK)         │◄────────┤│ id (PK)         │
│ email (UNIQUE)  │          │ user1_id (FK)   │
│ name            │          │ user2_id (FK)   │
│ nickname        │          │ anniversary     │
│ google_id       │          │ invite_code     │
│ profile_image   │          │ invite_expires  │
│ mbti_type       │          │ encrypted_key_1 │
│ mbti_answers    │          │ encrypted_key_2 │
│ public_key      │          │ created_at      │
│ couple_id (FK)  │          │ deleted_at      │
│ birthday        │          └─────────────────┘
│ gender          │                   │
│ created_at      │                   │
│ updated_at      │                   │
│ is_active       │                   │
└─────────────────┘                   │
         │                            │
         │                            │
         ▼                            ▼
┌─────────────────┐          ┌─────────────────┐
│    events       │          │  chat_messages  │
├─────────────────┤          ├─────────────────┤
│ id (PK)         │          │ id (PK)         │
│ couple_id (FK)  │          │ couple_id (FK)  │
│ created_by (FK) │          │ sender_id (FK)  │
│ title           │          │ content         │
│ date            │          │ sent_at         │
│ is_recurring    │          │ is_encrypted    │
│ recurrence      │          └─────────────────┘
│ notification    │
│ event_type      │
└─────────────────┘

┌─────────────────┐          ┌─────────────────┐
│    buckets      │          │    expenses     │
├─────────────────┤          ├─────────────────┤
│ id (PK)         │          │ id (PK)         │
│ couple_id (FK)  │          │ couple_id (FK)  │
│ created_by (FK) │          │ created_by (FK) │
│ title           │          │ category        │
│ description     │          │ amount          │
│ is_completed    │          │ description     │
│ completed_at    │          │ expense_date    │
│ target_date     │          │ payment_method  │
│ created_at      │          │ created_at      │
└─────────────────┘          └─────────────────┘

┌─────────────────┐          ┌─────────────────┐
│    memories     │          │    budgets      │
├─────────────────┤          ├─────────────────┤
│ id (PK)         │          │ id (PK)         │
│ couple_id (FK)  │          │ couple_id (FK)  │
│ created_by (FK) │          │ month           │
│ title           │          │ category        │
│ content         │          │ budget_amount   │
│ memory_date     │          │ spent_amount    │
│ location        │          │ created_at      │
│ photo_url       │          │ updated_at      │
│ created_at      │          └─────────────────┘
└─────────────────┘

┌─────────────────┐
│ recommendations │
├─────────────────┤
│ id (PK)         │
│ couple_id (FK)  │
│ title           │
│ description     │
│ category        │
│ location        │
│ price_range     │
│ rating          │
│ image_url       │
│ created_at      │
└─────────────────┘
```

### 주요 테이블 상세

#### 1. users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    google_id VARCHAR(255) UNIQUE,
    profile_image TEXT,
    mbti_type VARCHAR(4),           -- MDEP, ITCF 등
    mbti_answers JSONB,              -- {"1":"A", "2":"B", ...}
    public_key TEXT,                 -- RSA 공개키
    couple_id UUID REFERENCES couples(id),
    birthday DATE,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
```

#### 2. couples
```sql
CREATE TABLE couples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID REFERENCES users(id),
    user2_id UUID REFERENCES users(id),
    anniversary DATE,
    invite_code VARCHAR(6) UNIQUE NOT NULL,  -- 6자리 랜덤 코드
    invite_expires_at TIMESTAMP,
    encrypted_shared_key_user1 TEXT,         -- User1의 RSA로 암호화된 AES 키
    encrypted_shared_key_user2 TEXT,         -- User2의 RSA로 암호화된 AES 키
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);
```

#### 3. chat_messages
```sql
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID NOT NULL REFERENCES couples(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,              -- 암호화된 메시지
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_encrypted BOOLEAN DEFAULT TRUE
);
```

### 인덱스 전략

```sql
-- 자주 조회되는 컬럼에 인덱스 생성
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_couples_invite_code ON couples(invite_code);
CREATE INDEX idx_events_couple_id_date ON events(couple_id, date);
CREATE INDEX idx_chat_messages_couple_sent ON chat_messages(couple_id, sent_at);
CREATE INDEX idx_expenses_couple_date ON expenses(couple_id, expense_date);
```

---

## API 명세

### Base URL
```
Production: http://54.66.195.91/api
Local: http://localhost:8080/api
```

### 인증 헤더
```
Authorization: Bearer {JWT_TOKEN}
```

### 1. 인증 API

#### 1.1 Google 로그인
```http
POST /api/auth/google
Content-Type: application/json

Request:
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "6d2485e8-0837-40cf-9abe-6e464ac45605",
    "email": "user@gmail.com",
    "name": "홍길동",
    "nickname": "길동이",
    "profileImage": "https://...",
    "mbtiType": "MDEP",
    "coupleId": "860d1b96-4cc5-4165-904a-4998f0d6f3f8",
    "birthday": "1995-03-15",
    "gender": "MALE"
  }
}

Error: 401 Unauthorized
{
  "message": "Invalid Google token",
  "timestamp": "2026-01-21T07:30:00Z"
}
```

#### 1.2 현재 사용자 정보 조회
```http
GET /api/auth/me
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "id": "6d2485e8-0837-40cf-9abe-6e464ac45605",
  "email": "user@gmail.com",
  "name": "홍길동",
  "nickname": "길동이",
  "profileImage": "https://...",
  "mbtiType": "MDEP",
  "coupleId": "860d1b96-4cc5-4165-904a-4998f0d6f3f8"
}
```

#### 1.3 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "message": "Logged out successfully"
}
```

### 2. 커플 API

#### 2.1 커플 초대 코드 생성
```http
POST /api/couples/invite
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "inviteCode": "A7K9M2",
  "expiresAt": "2026-01-22T07:30:00Z"
}
```

#### 2.2 초대 코드로 커플 연결
```http
POST /api/couples/join
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "inviteCode": "A7K9M2",
  "anniversary": "2024-02-14"
}

Response: 200 OK
{
  "coupleId": "860d1b96-4cc5-4165-904a-4998f0d6f3f8",
  "user1": {
    "id": "...",
    "name": "홍길동",
    "mbtiType": "MDEP"
  },
  "user2": {
    "id": "...",
    "name": "김영희",
    "mbtiType": "ITCF"
  },
  "anniversary": "2024-02-14"
}
```

#### 2.3 내 커플 정보 조회
```http
GET /api/couples/me
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "coupleId": "860d1b96-4cc5-4165-904a-4998f0d6f3f8",
  "partner": {
    "id": "a21a8010-4f1e-4ff0-ac12-db6be93467e3",
    "email": "partner@gmail.com",
    "name": "김영희",
    "nickname": "영희",
    "mbtiType": "ITCF",
    "profileImage": "https://..."
  },
  "anniversary": "2024-02-14",
  "daysTogetherCount": 342
}
```

### 3. MBTI 테스트 API

#### 3.1 질문 조회
```http
GET /api/mbti/questions

Response: 200 OK
{
  "questions": [
    {
      "id": 1,
      "question": "우리는 데이트 전에 계획이 잡혀 있어야 마음이 편하다.",
      "optionA": "X",
      "optionB": "O",
      "dimension": "PF"
    },
    // ... 36개 질문
  ]
}
```

#### 3.2 테스트 제출
```http
POST /api/mbti/submit
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "answers": {
    "1": "A",
    "2": "B",
    "3": "A",
    // ... 36번까지
  }
}

Response: 200 OK
{
  "mbtiType": "MDEP",
  "details": {
    "M": 7, "I": 2,  // 소비 성향
    "D": 5, "T": 4,  // 갈등 해결
    "E": 6, "C": 3,  // 도전 성향
    "P": 8, "F": 1   // 데이트 계획
  }
}
```

#### 3.3 커플 궁합 조회
```http
GET /api/mbti/couple-result
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "myMbti": "MDEP",
  "partnerMbti": "ITCF",
  "compatibility": {
    "score": 70,
    "description": "좋은 궁합! 서로의 다름을 통해 성장할 수 있습니다.",
    "strengths": [
      "데이트 비용에 대한 생각이 비슷합니다",
      "갈등 해결 방식이 잘 맞습니다"
    ],
    "challenges": [
      "새로운 곳 탐험 vs 익숙한 장소 선호의 절충이 필요합니다"
    ]
  }
}
```

### 4. 채팅 API

#### 4.1 채팅 내역 조회
```http
GET /api/chat?page=0&size=50
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "messages": [
    {
      "id": "message-uuid",
      "senderId": "user-uuid",
      "senderName": "홍길동",
      "content": "암호화된 메시지 내용",
      "sentAt": "2026-01-21T10:30:00Z",
      "isEncrypted": true
    }
  ],
  "hasNext": true
}
```

#### 4.2 메시지 전송 (WebSocket)
```javascript
// WebSocket STOMP
SEND /app/chat.send
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "content": "암호화된 메시지"
}
```

### 5. 일정 API

#### 5.1 일정 생성
```http
POST /api/events
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "title": "데이트",
  "date": "2026-01-25T19:00:00Z",
  "isRecurring": false,
  "notification": "THIRTY_MINUTES",
  "eventType": "DATE"
}

Response: 201 Created
{
  "id": "event-uuid",
  "title": "데이트",
  "date": "2026-01-25T19:00:00Z",
  "createdBy": "user-uuid",
  "isRecurring": false,
  "eventType": "DATE"
}
```

#### 5.2 월별 일정 조회
```http
GET /api/events?year=2026&month=1
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "events": [
    {
      "id": "event-uuid",
      "title": "데이트",
      "date": "2026-01-25T19:00:00Z",
      "eventType": "DATE",
      "createdBy": {
        "id": "user-uuid",
        "name": "홍길동"
      }
    }
  ]
}
```

### 6. 가계부 API

#### 6.1 지출 등록
```http
POST /api/expenses
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "category": "FOOD",
  "amount": 45000,
  "description": "저녁 식사",
  "expenseDate": "2026-01-21",
  "paymentMethod": "CARD"
}

Response: 201 Created
{
  "id": "expense-uuid",
  "category": "FOOD",
  "amount": 45000,
  "description": "저녁 식사",
  "expenseDate": "2026-01-21",
  "createdBy": "user-uuid"
}
```

#### 6.2 월별 지출 내역 조회
```http
GET /api/expenses?year=2026&month=1
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "expenses": [
    {
      "id": "expense-uuid",
      "category": "FOOD",
      "amount": 45000,
      "description": "저녁 식사",
      "expenseDate": "2026-01-21",
      "createdBy": {
        "id": "user-uuid",
        "name": "홍길동"
      }
    }
  ],
  "totalAmount": 450000
}
```

#### 6.3 예산 설정
```http
POST /api/budgets
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "month": "2026-01",
  "category": "FOOD",
  "budgetAmount": 500000
}

Response: 201 Created
{
  "id": "budget-uuid",
  "month": "2026-01",
  "category": "FOOD",
  "budgetAmount": 500000,
  "spentAmount": 45000
}
```

### 7. 버킷리스트 API

#### 7.1 버킷리스트 생성
```http
POST /api/buckets
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request:
{
  "title": "제주도 여행",
  "description": "한라산 등반하기",
  "targetDate": "2026-06-01"
}

Response: 201 Created
{
  "id": "bucket-uuid",
  "title": "제주도 여행",
  "description": "한라산 등반하기",
  "isCompleted": false,
  "targetDate": "2026-06-01",
  "createdBy": "user-uuid"
}
```

#### 7.2 버킷리스트 완료 처리
```http
PATCH /api/buckets/{bucketId}/complete
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "id": "bucket-uuid",
  "isCompleted": true,
  "completedAt": "2026-01-21T15:30:00Z"
}
```

### 8. 파일 업로드 API

#### 8.1 이미지 업로드
```http
POST /api/files/upload
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data

Request:
- file: (binary)

Response: 200 OK
{
  "fileUrl": "http://54.66.195.91/api/files/abc123.jpg",
  "fileName": "abc123.jpg",
  "fileSize": 2048576
}
```

---

## 실시간 통신

### WebSocket & STOMP 설정

#### 연결 엔드포인트
```
ws://54.66.195.91/ws
```

#### STOMP 구독 토픽

| 토픽 | 설명 | 메시지 형식 |
|------|------|------------|
| `/topic/couple/{coupleId}` | 커플 전체 알림 | JSON |
| `/topic/chat/{coupleId}` | 채팅 메시지 | ChatMessage |
| `/user/queue/notifications` | 개인 알림 | Notification |

#### WebSocket 설정 코드
```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/user")
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
    }
    
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS()
    }
}
```

### 채팅 메시지 전송 흐름

```
Client A                    Backend                     Client B
   │                           │                           │
   │ ①SEND /app/chat.send      │                           │
   │   {content: "encrypted"}  │                           │
   ├──────────────────────────>│                           │
   │                           │                           │
   │                           │ ②Validate JWT             │
   │                           │ ③Save to DB               │
   │                           │                           │
   │                           │ ④Broadcast                │
   │                           ├──────────────────────────>│
   │                           │ /topic/chat/{coupleId}    │
   │                           │                           │
   │ ⑤Receive Message          │                           │ ⑥Receive Message
   │<──────────────────────────┤                           │<───────────
```

### 실시간 알림 타입

#### 1. MBTI 업데이트 알림
```json
{
  "type": "MBTI_UPDATED",
  "userId": "user-uuid",
  "mbtiType": "MDEP",
  "timestamp": "2026-01-21T10:30:00Z"
}
```

#### 2. 기념일 동기화 알림
```json
{
  "type": "ANNIVERSARY_SYNC",
  "coupleId": "couple-uuid",
  "anniversary": "2024-02-14",
  "timestamp": "2026-01-21T10:30:00Z"
}
```

#### 3. 채팅 메시지
```json
{
  "id": "message-uuid",
  "senderId": "user-uuid",
  "senderName": "홍길동",
  "content": "암호화된 메시지",
  "sentAt": "2026-01-21T10:30:00Z"
}
```

---

## 배포 및 인프라

### Docker 컨테이너 구조

```
┌─────────────────────────────────────────────────────┐
│                    Host: AWS EC2                     │
│                  Ubuntu 22.04 LTS                    │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │         nginx-proxy (Port 80)                 │  │
│  │         Reverse Proxy & Static Files          │  │
│  └─────────────────┬─────────────────────────────┘  │
│                    │                                 │
│                    ▼                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │         spring-app (Port 8080)                │  │
│  │         Spring Boot 3.5.9 + Kotlin            │  │
│  │         - REST API                            │  │
│  │         - WebSocket                           │  │
│  │         - JWT Authentication                  │  │
│  └─────────────────┬─────────────────────────────┘  │
│                    │                                 │
│                    ▼                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │         postgres-db (Port 5432)               │  │
│  │         PostgreSQL 15                         │  │
│  │         Volume: ./postgres_data               │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### docker-compose.yml

```yaml
version: '3.8'

services:
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
      - ./postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  app:
    build: .
    container_name: spring-app
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/ieum_db
      SPRING_DATASOURCE_USERNAME: hjxarchive
      SPRING_DATASOURCE_PASSWORD: "ieum2580-!"
      GOOGLE_CLIENT_IDS: ${GOOGLE_CLIENT_IDS}
      JWT_SECRET: ${JWT_SECRET}
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - app
    restart: unless-stopped
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Nginx 설정

```nginx
server {
    listen 80;
    server_name _;

    client_max_body_size 50M;

    location / {
        proxy_pass http://spring-app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws {
        proxy_pass http://spring-app:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 배포 프로세스

```bash
# 1. 코드 변경 후 로컬 테스트
./gradlew clean build -x test
docker compose down
docker compose up --build -d

# 2. Git에 커밋
git add .
git commit -m "Feature: ..."
git push origin main

# 3. EC2 서버 배포
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91
cd madcamp_W2_ieum_backend
git pull
./gradlew clean build -x test
docker compose down
docker compose up --build -d

# 4. 로그 확인
docker logs spring-app -f
```

### 환경 변수 관리

```bash
# .env 파일 (Git 제외)
GOOGLE_CLIENT_IDS=client-id-1,client-id-2,client-id-3
JWT_SECRET=your-secret-key-here
POSTGRES_PASSWORD=your-db-password
```

---

## 성능 최적화

### 1. 데이터베이스 최적화

#### 인덱스 전략
```sql
-- 복합 인덱스로 쿼리 성능 향상
CREATE INDEX idx_events_couple_date ON events(couple_id, date DESC);
CREATE INDEX idx_chat_couple_sent ON chat_messages(couple_id, sent_at DESC);
```

#### 페이지네이션
```kotlin
@GetMapping("/chat")
fun getChatHistory(
    @CurrentUser userId: UUID,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "50") size: Int
): Page<ChatMessageResponse> {
    val pageable = PageRequest.of(page, size, Sort.by("sentAt").descending())
    return chatService.getChatHistory(userId, pageable)
}
```

### 2. 캐싱 전략

```kotlin
// MBTI 질문은 변하지 않으므로 캐싱
@Cacheable("mbti-questions")
fun getQuestions(): MbtiQuestionsResponse {
    return MbtiQuestionsResponse(questions)
}
```

### 3. 연결 풀 설정

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

---

## 모니터링 및 로깅

### 로깅 레벨 설정

```yaml
logging:
  level:
    root: INFO
    com.ieum.ieum_back: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 주요 로그 포인트

```kotlin
// 인증 관련
logger.info("✅ Token verified successfully")
logger.info("📧 Email: ${payload.email}")
logger.error("❌ Google token verification failed")

// 비즈니스 로직
logger.debug("Creating couple with invite code: $inviteCode")
logger.warn("Invite code expired: $inviteCode")
```

---

## 보안 체크리스트

- [x] Google OAuth 2.0 토큰 검증
- [x] JWT 토큰 기반 인증
- [x] HTTPS 통신 (프로덕션)
- [x] CORS 설정
- [x] SQL Injection 방지 (JPA/Hibernate)
- [x] XSS 방지 (입력 검증)
- [x] 비밀번호 암호화 (N/A - OAuth만 사용)
- [x] 민감 정보 환경 변수 관리
- [x] End-to-End 암호화 채팅
- [x] API Rate Limiting (추가 구현 권장)
- [x] 파일 업로드 검증

---

## 트러블슈팅 가이드

### 1. Google OAuth 401 에러

**증상**: `Invalid Google token`

**원인**:
- 만료된 ID Token
- 잘못된 Audience (aud)
- Client ID 불일치

**해결**:
```bash
# 로그 확인
docker logs spring-app | grep "Google"

# Client ID 환경 변수 확인
docker exec spring-app env | grep GOOGLE

# 재배포
docker compose down
docker compose up --build -d
```

### 2. WebSocket 연결 실패

**증상**: `WebSocket connection failed`

**원인**:
- Nginx 프록시 설정 누락
- STOMP 헤더 오류

**해결**:
```nginx
# nginx/default.conf
location /ws {
    proxy_pass http://spring-app:8080/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### 3. DB 연결 오류

**증상**: `Connection refused`

**원인**:
- 컨테이너 네트워크 문제
- 잘못된 DB 자격 증명

**해결**:
```bash
# DB 컨테이너 확인
docker ps | grep postgres

# 연결 테스트
docker exec postgres-db psql -U hjxarchive -d ieum_db -c "SELECT 1;"

# 로그 확인
docker logs postgres-db
```

---

## 향후 개선 사항

### 1. 성능 개선
- [ ] Redis 캐싱 도입
- [ ] CDN 연동 (이미지 최적화)
- [ ] 데이터베이스 읽기 복제본 추가

### 2. 보안 강화
- [ ] API Rate Limiting 구현
- [ ] HTTPS 인증서 적용 (Let's Encrypt)
- [ ] OAuth Refresh Token 구현

### 3. 기능 확장
- [ ] 푸시 알림 (FCM)
- [ ] AI 기반 데이트 추천
- [ ] 소셜 공유 기능

### 4. 모니터링
- [ ] Prometheus + Grafana 연동
- [ ] APM 도구 도입 (Scouter, New Relic)
- [ ] 에러 추적 (Sentry)

---

## 참고 문서

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.5.9/reference/html/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [JWT.io](https://jwt.io/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
- [WebSocket & STOMP](https://spring.io/guides/gs/messaging-stomp-websocket/)

---

**작성일**: 2026년 1월 21일  
**버전**: 1.0.0  
**작성자**: Ieum Backend Team
