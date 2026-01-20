# Frontend Google OAuth 수정사항

## 🔴 문제: AuthService.kt 엔드포인트 경로 오류

### 현재 코드 (잘못됨):
```kotlin
@POST("auth/google")  // ❌ /api가 빠짐
suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse
```

### 수정할 코드:
```kotlin
@POST("api/auth/google")  // ✅ 올바른 경로
suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse
```

---

## 전체 수정된 AuthService.kt

```kotlin
package com.ieum.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/google")  // ← 이 부분 수정!
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse
}

data class GoogleLoginRequest(val idToken: String)

data class AuthResponse(
    val accessToken: String,
    val user: User
)

data class User(
    val id: String,
    val email: String,
    val name: String?,
    val nickname: String?,
    val profileImage: String?,
    val birthday: String?,
    val gender: String?,
    val coupleId: String?,
    val mbtiType: String?,
    val isActive: Boolean
)
```

---

## 📋 체크리스트

### 1. AuthService.kt 수정
- [ ] `@POST("auth/google")` → `@POST("api/auth/google")` 변경

### 2. NetworkModule.kt 확인
```kotlin
private const val BASE_URL = "http://54.66.195.91/"  // ✅ 이미 올바름
```

### 3. AndroidManifest.xml 확인
```xml
<uses-permission android:name="android.permission.INTERNET" />
<application
    android:usesCleartextTraffic="true">  <!-- HTTP 허용 -->
```

### 4. build.gradle.kts 의존성 확인
```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp 로깅 (디버깅용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}
```

---

## 🧪 테스트 방법

수정 후 로그인을 시도하면 다음과 같은 로그가 나와야 합니다:

### 성공 시:
```
D/GoogleLogin: 1. ID Token 받음: eyJhbGciOiJSUzI1NiIsImtpZCI6ImE...
D/GoogleLogin: 2. 서버 요청 시작...
D/GoogleLogin: 3. 서버 응답 성공!
D/GoogleLogin:    - accessToken: eyJhbGciOiJIUzI1NiIsInR5cCI6Ik...
D/GoogleLogin:    - user email: hjxinvest@gmail.com
D/GoogleLogin:    - user id: 550e8400-e29b-41d4-a716-446655440000
D/GoogleLogin: 4. 토큰 저장 완료
```

### 실패 시 (예: 404):
```
E/GoogleLogin: 3. 서버 응답 실패!
E/GoogleLogin:    - 에러 타입: HttpException
E/GoogleLogin:    - 에러 메시지: HTTP 404 Not Found
```

---

## 🎯 예상 결과

- **수정 전**: HTTP 404 (엔드포인트를 찾을 수 없음)
- **수정 후**: HTTP 200 OK → 로그인 성공!

---

## 추가 디버깅 (필요시)

NetworkModule.kt에 로깅 인터셉터 추가:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://54.66.195.91/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("🌐 HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }
}
```

이렇게 하면 HTTP 요청/응답을 모두 로그로 볼 수 있습니다:
```
D/🌐 HTTP: --> POST http://54.66.195.91/api/auth/google
D/🌐 HTTP: Content-Type: application/json
D/🌐 HTTP: {"idToken":"eyJhbGci..."}
D/🌐 HTTP: <-- 200 OK (1234ms)
D/🌐 HTTP: {"accessToken":"eyJ...","user":{...}}
```
