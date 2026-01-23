# Swagger API 문서 설정 완료 ✅

## 📖 접속 방법

### 로컬 환경
```
http://localhost:8080/swagger-ui/index.html
```

### 프로덕션 환경 (AWS EC2)
```
http://54.66.195.91/swagger-ui/index.html
```

---

## 🔧 구현 내용

### 1. Swagger 의존성 추가 ([build.gradle](build.gradle))
```groovy
// Swagger/OpenAPI
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
```

### 2. Swagger 설정 클래스 생성 ([SwaggerConfig.kt](src/main/kotlin/com/ieum/ieum_back/config/SwaggerConfig.kt))
```kotlin
@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("이음(Ieum) API 명세서")
                    .version("1.0.0")
                    .description("""
                        ## 커플을 위한 종합 관리 플랫폼 API
                        
                        ### 핵심 기능
                        - 🔐 Google OAuth 2.0 기반 소셜 로그인
                        - 💑 초대 코드 기반 커플 매칭
                        - 🔒 End-to-End 암호화 채팅
                        ... (생략)
                    """)
            )
            .servers(
                listOf(
                    Server().url("http://54.66.195.91/api"),
                    Server().url("http://localhost:8080/api")
                )
            )
            .addSecurityItem(SecurityRequirement().addList("Bearer Authentication"))
            .components(
                Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}
```

### 3. Swagger 경로 인증 제외 ([WebMvcConfig.kt](src/main/kotlin/com/ieum/ieum_back/common/WebMvcConfig.kt))
```kotlin
override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(jwtAuthInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/google",
            "/api/users",
            "/api/health",
            "/api/mbti/questions",
            // Swagger UI 경로
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
        )
}
```

---

## 🎯 사용 방법

### 1. Swagger UI 접속
브라우저에서 `http://54.66.195.91/swagger-ui/index.html` 접속

### 2. JWT 토큰 인증
1. **Authorize** 버튼 클릭
2. Google OAuth로 로그인 후 받은 JWT 토큰 입력
   ```
   Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC...
   ```
3. **Authorize** 클릭
4. 이후 모든 API 요청에 자동으로 JWT 포함

### 3. API 테스트
- 각 엔드포인트를 클릭하여 상세 정보 확인
- **Try it out** 버튼으로 직접 API 호출 테스트
- Request/Response 샘플 확인 가능

---

## 📂 주요 API 그룹

### 1. 인증 (Auth)
- `POST /api/auth/google` - Google OAuth 로그인
- `GET /api/auth/me` - 현재 사용자 정보 조회
- `POST /api/auth/logout` - 로그아웃

### 2. 커플 (Couples)
- `POST /api/couples/invite` - 초대 코드 생성
- `POST /api/couples/join` - 초대 코드로 커플 연결
- `GET /api/couples/me` - 내 커플 정보 조회

### 3. MBTI 테스트
- `GET /api/mbti/questions` - 질문 조회 (36문항)
- `POST /api/mbti/submit` - 테스트 제출
- `GET /api/mbti/couple-result` - 커플 궁합 조회

### 4. 채팅 (Chat)
- `GET /api/chat` - 채팅 내역 조회 (페이징)
- WebSocket: `ws://54.66.195.91/ws/stomp` (STOMP)

### 5. 일정 (Events)
- `POST /api/events` - 일정 생성
- `GET /api/events` - 월별 일정 조회
- `PUT /api/events/{id}` - 일정 수정
- `DELETE /api/events/{id}` - 일정 삭제

### 6. 가계부 (Expenses)
- `POST /api/expenses` - 지출 등록
- `GET /api/expenses` - 월별 지출 내역
- `POST /api/budgets` - 예산 설정
- `GET /api/budgets` - 예산 조회

### 7. 버킷리스트 (Buckets)
- `POST /api/buckets` - 버킷리스트 생성
- `GET /api/buckets` - 버킷리스트 조회
- `PATCH /api/buckets/{id}/complete` - 완료 처리

### 8. 파일 업로드
- `POST /api/files/upload` - 이미지 업로드

---

## 🔐 보안

### JWT 토큰 획득 방법
1. **Google OAuth 로그인** (`POST /api/auth/google`)
   ```json
   {
     "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
   }
   ```

2. **응답에서 JWT 토큰 추출**
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     "user": { ... }
   }
   ```

3. **Swagger UI에서 인증**
   - 우측 상단 **Authorize** 버튼 클릭
   - 토큰 입력 (Bearer prefix 불필요)
   - **Authorize** 클릭

---

## 🎨 Swagger UI 기능

### API 문서
- 모든 엔드포인트 자동 문서화
- Request/Response 스키마 표시
- 예제 데이터 제공

### 인터랙티브 테스트
- **Try it out** 버튼으로 즉시 테스트
- 파라미터 입력 및 실행
- 실시간 응답 확인

### 모델 스키마
- 하단 **Schemas** 섹션에서 DTO 구조 확인
- 필드 타입, 제약조건, 설명 표시

---

## ✅ 배포 확인

### 로컬
```bash
curl http://localhost:8080/v3/api-docs
```

### 프로덕션 (EC2)
```bash
curl http://54.66.195.91/v3/api-docs
```

정상 응답 시 JSON 형식의 OpenAPI 스펙 반환

---

## 📝 추가 설정 (선택사항)

### application.yaml에서 Swagger 커스터마이징
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
```

### API 컨트롤러에 문서 추가
```kotlin
@Tag(name = "인증", description = "Google OAuth 및 JWT 인증 API")
@RestController
@RequestMapping("/api/auth")
class AuthController {
    
    @Operation(
        summary = "Google OAuth 로그인",
        description = "Google ID Token을 검증하고 JWT AccessToken을 발급합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "로그인 성공"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 Google Token")
    )
    @PostMapping("/google")
    fun googleLogin(@RequestBody request: GoogleLoginRequest): AuthResponse {
        // ...
    }
}
```

---

**작성일**: 2026년 1월 21일  
**버전**: 1.0.0  
**Springdoc OpenAPI**: 2.2.0
