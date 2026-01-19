# Google OAuth 연결 가이드

이음(IEUM) 프로젝트에서 Google OAuth 2.0 인증을 설정하는 방법을 안내합니다.

---

## 📋 목차
1. [Google Cloud Console 설정](#1-google-cloud-console-설정)
2. [백엔드 설정](#2-백엔드-설정)
3. [Android 앱 연동](#3-android-앱-연동)
4. [테스트 방법](#4-테스트-방법)
5. [문제 해결](#5-문제-해결)

---

## 1. Google Cloud Console 설정

### 1.1 프로젝트 생성
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 상단 프로젝트 드롭다운 클릭 → **새 프로젝트** 선택
3. 프로젝트 이름 입력 (예: `ieum-app`)
4. **만들기** 클릭

### 1.2 OAuth 동의 화면 구성
1. 좌측 메뉴에서 **API 및 서비스** → **OAuth 동의 화면** 선택
2. 사용자 유형 선택:
   - **외부** 선택 (누구나 Google 계정으로 로그인 가능)
   - **만들기** 클릭

3. **앱 정보** 입력:
   ```
   앱 이름: 이음 (IEUM)
   사용자 지원 이메일: your-email@example.com
   앱 로고: (선택사항)
   ```

4. **앱 도메인** (선택사항):
   ```
   애플리케이션 홈페이지: http://54.66.195.91
   개인정보처리방침: http://54.66.195.91/privacy
   서비스 약관: http://54.66.195.91/terms
   ```

5. **승인된 도메인**:
   ```
   54.66.195.91 (프로덕션 서버)
   localhost (개발 환경)
   ```

6. **개발자 연락처 정보**: 이메일 입력

7. **저장 후 계속** 클릭

### 1.3 범위(Scope) 설정
1. **범위 추가 또는 삭제** 클릭
2. 다음 범위 선택:
   - ✅ `.../auth/userinfo.email` - 이메일 주소 보기
   - ✅ `.../auth/userinfo.profile` - 개인정보(공개로 설정한 개인정보 포함) 보기
   - ✅ `openid` - 사용자 인증

3. **업데이트** → **저장 후 계속**

### 1.4 OAuth 클라이언트 ID 생성
1. 좌측 메뉴에서 **API 및 서비스** → **사용자 인증 정보** 선택
2. 상단 **+ 사용자 인증 정보 만들기** → **OAuth 클라이언트 ID** 선택
3. 애플리케이션 유형:
   - **웹 애플리케이션** 선택 (프론트엔드가 웹이면)
   - **Android** 또는 **iOS** (모바일 앱이면)

#### 웹 애플리케이션 설정:
```
이름: IEUM Web Client

승인된 자바스크립트 원본:
- http://localhost:3000 (개발)
- http://54.66.195.91 (프로덕션)

승인된 리디렉션 URI:
- http://localhost:3000/auth/callback (개발)
- http://54.66.195.91/auth/callback (프로덕션)
```

4. **만들기** 클릭
5. **클라이언트 ID**와 **클라이언트 보안 비밀번호** 저장
   ```
   클라이언트 ID: 123456789-abcdefg.apps.googleusercontent.com
   클라이언트 보안 비밀번호: GOCSPX-xxxxxxxxxxxxxxxxxxxxx
   ```

### 1.5 Android/iOS 앱 설정 (모바일 앱인 경우)

#### Android (Kotlin):

**📱 프로젝트가 아직 없는 경우:**
1. **Android Studio**에서 새 프로젝트 생성:
   - **File** → **New** → **New Project**
   - **Phone and Tablet** → **Empty Activity** 선택
   - **Language**: Kotlin 선택
   - **Package name**: `com.ieum.app` (원하는 이름으로)
   - **Save location**: 프로젝트 위치 선택
   - **Finish** 클릭

2. **패키지 이름 확인:**
   - `app/build.gradle.kts` (또는 `build.gradle`) 파일 열기
   - `applicationId` 확인
   ```kotlin
   // app/build.gradle.kts
   android {
       namespace = "com.ieum.app"
       defaultConfig {
           applicationId = "com.ieum.app"  // 이 값이 패키지 이름
           ...
       }
   }
   ```

**또는 기존 프로젝트의 패키지 이름 확인:**
- Android Studio 좌측 **Project** 뷰에서 `app/build.gradle.kts` 확인
- 또는 `AndroidManifest.xml`에서 `package` 속성 확인

**🔑 SHA-1 디지털 지문 생성:**

**옵션 1: Debug 키스토어 사용 (개발/테스트용)**

📍 **실행 위치:** 터미널에서 **아무 디렉토리**에서나 실행 가능 (키스토어 경로를 절대경로로 지정하기 때문)

```bash
# macOS/Linux - 아무 디렉토리에서나 실행
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Windows - 아무 디렉토리에서나 실행
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

💡 **Tip:** 터미널을 열고 바로 실행하면 됩니다!
```bash
# 예시
cd ~  # 홈 디렉토리로 이동 (선택사항)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

출력 예시:
```
Certificate fingerprints:
	 SHA1: AB:CD:Android 프로젝트의 **app 디렉토리**

키스토어가 없다면 새로 생성:
```bash
# 1. Android 프로젝트로 이동
cd /path/to/your/IeumApp

# 2. app 폴더로 이동
cd 
키스토어가 없다면 새로 생성:
```bash
# 1. React Native 프로젝트 루트로 이동
cd /path/to/your/IeumApp

# 2. android/app 폴더로 이동
cd android/app

# 3. 키스토어 생성
keytool -genkeypair -v -storetype PKCS12 -keystore ieum-release.keystore -alias ieum-key -keyalg RSA -keysize 2048 -validity 10000

# 입력 정보:
# - 키 저장소 비밀번호 입력 및 확인 (안전하게 보관!)
# - 이름, 조직, 도시, 국가 등 입력
```

생성된 키스토어에서 SHA-1 추출:
```bash
# app 디렉토리에 있다면
keytool -list -v -keystore ieum-release.keystore -alias ieum-key

# 또는 프로젝트 루트에서
keytool -list -v -keystore app/ieum-release.keystore -alias ieum-key

# 비밀번호 입력
# SHA1 값 복사
```

**⚠️ 중요: 키스토어 파일과 비밀번호는 안전하게 보관!**

**Google Cloud Console 설정:**
1. 애플리케이션 유형: **Android** 선택
2. **패키지 이름**: `com.ieum.app` (위에서 확인한 값)
3. **SHA-1 인증서 지문**: 위에서 복사한 SHA-1 값 입력
4. **만들기** 클릭

**여러 개의 SHA-1 지문 등록 (권장):**
- Debug 키스토어의 SHA-1 (개발용)
- Release 키스토어의 SHA-1 (배포용)
- 팀원들의 Debug 키스토어 SHA-1

각 SHA-1마다 별도의 OAuth 클라이언트 ID를 생성하거나, 하나의 클라이언트 ID에 여러 지문을 추가할 수 있습니다.

#### iOS:

**📱 프로젝트가 아직 없는 경우:**
1. React Native 프로젝트가 생성되어 있어야 함

**🆔 번들 ID 확인:**
1. Xcode에서 프로젝트 열기:
   ```bash
   cd ios
   open IeumApp.xcworkspace  # 또는 .xcodeproj
   ```

2. 프로젝트 네비게이터에서 프로젝트 선택
3. **TARGETS** → 앱 이름 선택
4. **General** 탭 → **Identity** 섹션
5. **Bundle Identifier** 확인 또는 설정 (예: `com.ieum.app`)

**또는 `ios/IeumApp/Info.plist` 확인:**
```xml
<key>CFBundleIdentifier</key>
<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
```

**Google Cloud Console 설정:**
1. 애플리케이션 유형: **iOS** 선택
2. **번들 ID**: `com.ieum.app` (위에서 확인한 값)
3. **App Store ID**: (선택사항, 앱스토어 출시 후)
4. **만들기** 클릭

**📝 번들 ID 네이밍 규칙:**
- 소문자 사용
- 역도메인 형식: `com.회사명.앱이름`
- 예시: `com.ieum.app`, `kr.kaist.ieum`

---

## 2. 백엔드 설정

### 2.1 환경 변수 설정

#### 로컬 개발 환경:
`src/main/resources/application.yaml` 파일 수정:

```yaml
google:
  client-id: 123456789-abcdefg.apps.googleusercontent.com

jwt:
  secret: your-secret-key-at-least-32-characters-long-for-security
  expiration: 604800000  # 7일 (밀리초 단위)
```

**JWT Secret 값 생성 방법:**

📌 **최소 32자 이상의 무작위 문자열**이 필요합니다!

**옵션 1: 터미널에서 생성 (권장)**
```bash
# macOS/Linux - 무작위 64자 문자열 생성
openssl rand -base64 48

# 출력 예시:
# 7J9K2mP4nR8qS5tU3vW6xY0zA1bC4dE7fG9hI2jK5lM8nP0qR3sT6uV9wX2yZ5aB

# 또는 UUID 기반 생성
uuidgen | openssl base64
7J9K2mP4nR8qS5tU3vW6xY0zA1bC4dE7fG9hI2jK5lM8nP0qR3sT6uV9wX2yZ5aB
```

💡 **Tip:** JWT_SECRET은 위에서 생성한 무작위 문자열을 사용하세요!력 예시:
# NTc4ODk5NzAtYjMyYi00ZGU5LTkzYjEtOTQ2ZDY2NzA2ZDcyCg==
```

**옵션 2: 온라인 생성기 사용**
- https://www.uuidgenerator.net/
- https://randomkeygen.com/

**옵션 3: 직접 입력 (간단한 방법)**
```
예시: IeumApp2024SecureJwtSecretKeyForProduction!@#$%^&*()
```

**JWT Expiration 값 설정:**

```yaml
jwt:
  expiration: 604800000  # 7일
  # 604800000 = 7일 × 24시간 × 60분 × 60초 × 1000밀리초
```

다른 만료 시간 예시:
- `3600000` = 1시간
- `86400000` = 1일
- `604800000` = 7일 (기본값)
- `2592000000` = 30일

**⚠️ 보안 주의사항:**
- 개발 환경과 프로덕션 환경의 secret은 **반드시 다르게** 설정
- GitHub 등 공개 저장소에 절대 커밋하지 말 것
- 프로덕션 환경에서는 환경 변수로 관리 (아래 참고)

#### 프로덕션 환경 (서버):
환경 변수로 설정:

```bash
# 서버 접속
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91

# 환경 변수 파일 생성
nano ~/madcamp_W2_ieum_backend/.env
```

`.env` 파일 내용:
```env
GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
JWT_SECRET=your-production-secret-key-at-least-32-characters
```

`docker-compose.yml` 수정:
```yaml
services:
  app:
    build: .
    container_name: spring-app
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/ieum_db
      SPRING_DATASOURCE_USERNAME: hjxarchive
      SPRING_DATASOURCE_PASSWORD: "ieum2580-!"
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      JWT_SECRET: ${JWT_SECRET}
    env_file:
      - .env
```

### 2.2 백엔드 재배포
```bash
# 로컬에서 빌드
cd /Users/hjxarchive/madcamp_W2_ieum_backend
./gradlew clean build -x test

# 서버에 업로드
scp -i ~/Downloads/ieum_key.pem build/libs/ieum_back-0.0.1-SNAPSHOT.jar ubuntu@54.66.195.91:~/

# 서버에서 재시작
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91
cd madcamp_W2_ieum_backend
cp ~/ieum_back-0.0.1-SNAPSHOT.jar build/libs/
docker-compose up -d --build app
```

---

## 3. Android 앱 연동

### 3.1 의존성 추가 (Dependencies)

#### 설치:

**1. Google Play Services 추가**

`app/build.gradle.kts`에 다음 dependencies 추가:
```kotlin
dependencies {
    // 기존 dependencies...
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Credential Manager (권장 - 최신 방식)
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    
    // Retrofit (백엔드 API 통신용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // DataStore (토큰 저장용 - SharedPreferences 대신 권장)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

**참고:** 현재 프로젝트가 `com.ieum` 패키지를 사용하고 있으므로, Google Cloud Console에서 **패키지 이름을 `com.ieum`으로 등록**하세요!

### 3.2 AndroidManifest.xml 확인

**파일 경로:** `app/src/main/AndroidManifest.xml`

현재 프로젝트의 AndroidManifest.xml이 이미 올바르게 설정되어 있습니다:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <application
        android:name=".IeumApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Ieum">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Ieum">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>

</manifest>
```

✅ **이미 설정되어 있는 항목:**
- INTERNET 권한
- IeumApplication 등록
- MainActivity 설정

**추가 작업 불필요!** 그대로 사용하면 됩니다.

### 3.3 코드 구현

프로젝트 구조에 따라 다음 파일들을 생성합니다.

**📁 파일 생성 위치:**
- Android 프로젝트의 `app/src/main/kotlin/com/ieum/` 또는 `app/src/main/java/com/ieum/` 디렉토리 아래에 생성
- 패키지 이름에 맞춰 폴더 구조를 만들어야 합니다

**예시:**
```
YourAndroidProject/
└── app/
    └── src/
        └── main/
            ├── kotlin/  (또는 java/)
            │   └── com/
            │       └── ieum/
            │           ├── IeumApplication.kt
            │           ├── MainActivity.kt
            │           ├── data/
            │           │   ├── api/
            │           │   │   └── AuthService.kt
            │           │   └── repository/
            │           │       └── AuthRepositoryImpl.kt
            │           ├── di/
            │           │   ├── NetworkModule.kt
            │           │   └── RepositoryModule.kt
            │           ├── domain/
            │           │   └── repository/
            │           │       └── AuthRepository.kt
            │           └── presentation/
            │               └── login/
            │                   ├── LoginViewModel.kt
            │                   └── LoginScreen.kt
            └── AndroidManifest.xml
```

#### Step 1: API 서비스 정의

**파일 경로:** `app/src/main/kotlin/com/ieum/data/api/AuthService.kt`

```kotlin
// data/api/AuthService.kt
package com.ieum.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/google")
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

**Step 2: Retrofit 설정 (Hilt 사용)**

**파일 경로:** `app/src/main/kotlin/com/ieum/di/NetworkModule.kt`

```kotlin
// di/NetworkModule.kt
package com.ieum.di

import com.ieum.data.api.AuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@ModuleViewModel & Repository**
```kotlin
// domain/repository/AuthRepository.kt
package com.ieum.domain.repository

import com.ieum.data.api.AuthResponse

interface AuthRepository {
    suspend fun googleLogin(idToken: String): Result<AuthResponse>
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
}

// data/repository/AuthRepositoryImpl.kt
package com.ieum.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ieum.data.api.AuthService
import com.ieum.data.api.GoogleLoginRequest
import com.ieum.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: AuthService
) : AuthRepository {
    
    private val TOKEN_KEY = stringPreferencesKey("access_token")
    
    override suspend fun googleLogin(idToken: String) = runCatching {
        authService.googleLogin(GoogleLoginRequest(idToken))
    }
    
    override suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }
    
    override suspend fun getToken(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[TOKEN_KEY]
        }.first()
    }
}

// presentation/login/LoginViewModel.kt
package com.ieum.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            authRepository.googleLogin(idToken)
                .onSuccess { response ->
                    authRepository.saveToken(response.accessToken)
                    _loginState.value = LoginState.Success(response.user.email)
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "로그인 실패")
                }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val email: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

**Step 4: LoginViewModel에 Google 로그인 메소드 추가**

**✅ LoginScreen.kt는 이미 완벽하게 구현되어 있습니다!**

현재 LoginScreen에서 `viewModel.onClickGoogleLogin(onLoginSuccess)`를 호출하고 있으므로, **LoginViewModel에 이 메소드만 추가**하면 됩니다.

**파일 경로:** `app/src/main/kotlin/com/ieum/presentation/feature/login/LoginViewModel.kt`

기존 `LoginViewModel.kt` 파일에 다음 코드를 추가하세요:

```kotlin
// presentation/feature/login/LoginViewModel.kt
package com.ieum.presentation.feature.login

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ieum.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    // 👇 이 메소드 추가!
    fun onClickGoogleLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Google 로그인 처리는 Composable에서 해야 하므로
            // 여기서는 상태만 업데이트하고 실제 로그인은 별도 함수로 처리
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.googleLogin(idToken)
                .onSuccess { response ->
                    authRepository.saveToken(response.accessToken)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "로그인 실패"
                        ) 
                    }
                }
        }
    }
}
```

**그리고 LoginScreen.kt를 약간 수정:**

```kotlin
// presentation/feature/login/LoginScreen.kt
package com.ieum.presentation.feature.login

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ieum.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()  // 👈 hiltViewModel()로 변경
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.background2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 로그인 UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "이음",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5A3E2B)
            )

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                onClick = {
                    // 👇 Google 로그인 처리
                    coroutineScope.launch {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
                                .build()
                            
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            
                            val result = credentialManager.getCredential(
                                request = request,
                                context = context as Activity
                            )
                            
                            when (val credential = result.credential) {
                                is CustomCredential -> {
                                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential
                                            .createFrom(credential.data)
                                        
                                        viewModel.loginWithGoogle(
                                            googleIdTokenCredential.idToken,
                                            onLoginSuccess
                                        )
                                    }
                                }
                            }
                        } catch (e: GetCredentialException) {
                            // 에러 처리
                        }
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .width(260.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6C8A0),
                    contentColor = Color(0xFF5A3E2B),
                    disabledContainerColor = Color(0xFFE6C8A0).copy(alpha = 0.6f),
                    disabledContentColor = Color(0xFF5A3E2B)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF5A3E2B)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("로그인 중…")
                } else {
                    Text(
                        text = "Google로 시작하기",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 에러 메시지 표시
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}
```

**Step 5: RepositoryModule에 AuthRepository 추가**

**파일 경로:** `app/src/main/kotlin/com/ieum/di/RepositoryModule.kt`

기존 `RepositoryModule.kt` 파일에 `AuthRepository` 바인딩만 추가하세요:

```kotlin
// di/RepositoryModule.kt - 기존 파일에 추가
import com.ieum.data.repository.AuthRepositoryImpl
import com.ieum.domain.repository.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // 👇 이 부분만 추가!
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    // 기존의 다른 Repository 바인딩들...
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    // ... 나머지 기존 코드 유지
}
```

**Step 6: Application 클래스에 Hilt 설정**

**파일 경로:** `app/src/main/kotlin/com/ieum/IeumApplication.kt`

기존 `IeumApplication.kt` 파일에 `@HiltAndroidApp` 어노테이션만 추가하세요:

```kotlin
// IeumApplication.kt
package com.ieum

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp  // 👈 이 어노테이션만 추가!
class IeumApplication : Application() {
    // 기존 코드 유지
}
```

**AndroidManifest.xml 확인:**

현재 AndroidManifest.xml이 이미 올바르게 설정되어 있습니다:
- ✅ `android:name=".IeumApplication"` 설정됨
- ✅ `<uses-permission android:name="android.permission.INTERNET" />` 설정됨
- ✅ MainActivity 설정됨

**추가 작업 불필요!** AndroidManifest.xml은 그대로 두면 됩니다.

**Step 7: 완료! 기존 구조 활용**

**✅ 모든 파일이 이미 준비되어 있습니다!**

현재 상태:
- ✅ `MainNavigation.kt` - Routes.LOGIN → LoginScreen 연결 완료
- ✅ `LoginScreen.kt` - 아름다운 UI 구현 완료  
- ✅ `MainActivity.kt` - @AndroidEntryPoint 설정 완료
- ✅ `AndroidManifest.xml` - 모든 권한 설정 완료

**이제 추가로 해야 할 일:**

1. **Step 3의 Repository와 ViewModel 파일들 생성** (Step 3 참고)
2. **Step 4의 LoginViewModel 코드 추가/수정**
3. **Step 5의 RepositoryModule에 AuthRepository 바인딩 추가**
4. **Step 6의 IeumApplication에 @HiltAndroidApp 추가**
5. **LoginScreen.kt의 `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com`를 실제 Web Client ID로 교체**

그러면 Google OAuth 로그인이 완벽하게 작동합니다! 🎉

**Step 8: 인증 Interceptor 추가 (인증 필요한 API용)**
```kotlin
// di/NetworkModule.kt에 추가
@Provides
@Singleton
fun provideAuthInterceptor(authRepository: AuthRepository): Interceptor {
    return Interceptor { chain ->
        val token = runBlocking { authRepository.getToken() }
        val request = chain.request().newBuilder()
            .apply {
                token?.let {
                    addHeader("Authorization", "Bearer $it")
                }
            }
            .build()
        chain.proceed(request)
    }
}

@Provides
@Singleton
fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
}
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Google로 로그인")
            }
        }
        
        // 에러 메시지
        if (loginState is LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (loginState as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private suspend fun signInWithGoogle(
    context: android.content.Context,
    credentialManager: CredentialManager,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        
        val result = credentialManager.getCredential(
            request = request,
            context = context as Activity
        )
        
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)
                    
                    onSuccess(googleIdTokenCredential.idToken)
                }
            }
        }
    } catch (e: Exception) {
        onError(eYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)
                    
                    val idToken = googleIdTokenCredential.idToken
                    
                    // 백엔드로 ID Token 전송
                    sendTokenToBackend(idToken)
                }
            }
        }
    }
    
    private fun sendTokenToBackend(idToken: String) {
        lifecycleScope.launch {
            try {
                val request = GoogleLoginRequest(idToken)
                val response = RetrofitClient.authService.googleLogin(request)
                
                // 토큰 저장
                saveToken(response.accessToken)
                
                // 메인 화면으로 이동
                navigateToMain()
                
            } catch (e: Exception) {
                Log.e("Login", "Backend error: ${e.message}")
                // 에러 처리
            }
        }
    }
    
    private fun saveToken(token: String) {
        val sharedPref = getSharedPreferences("auth", Context.MODE_PRIVATE)
        sharedPref.edit().putString("accessToken", token).apply()
    }
}
```

**Step 4: Layout (activity_login.xml)**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp">
    
    <Button
        android:id="@+id/btnGoogleLogin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Google로 로그인" />
        
</LinearLayout>
```

**Step 5: API 호출 시 토큰 사용**
```kotlin
// 인증이 필요한 API 호출
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = getToken() // SharedPreferences에서 가져오기
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

// Retrofit 클라이언트에 Interceptor 추가
val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```



---

## 4. 테스트 방법

### 4.1 백엔드 API 테스트

#### 유효한 Google ID Token 생성:
1. Google OAuth Playground 사용: https://developers.google.com/oauthplayground/
2. Step 1: **Google OAuth2 API v2** 선택 → `userinfo.email`, `userinfo.profile` 선택
3. **Authorize APIs** 클릭하여 로그인
4. Step 2: **Exchange authorization code for tokens** 클릭
5. `id_token` 값 복사

#### cURL로 테스트:
```bash
curl -X POST http://54.66.195.91/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
  }'
```

#### 성공 응답:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@gmail.com",
    "name": "홍길동",
    "nickname": null,
    "profileImage": "https://lh3.googleusercontent.com/...",
    "birthday": null,
    "gender": null,
    "coupleId": null,
    "mbtiType": null,
    "isActive": true
  }
}
```

### 4.2 Android 앱 테스트
1. Android Studio에서 앱 실행 (에뮬레이터 또는 실제 기기)
2. Google 로그인 버튼 클릭
3. Google 계정 선택 및 권한 승인
4. Logcat에서 로그인 성공 및 토큰 확인
5. 메인 화면으로 이동 확인

**디버그 팁:**
```kotlin
// LoginViewModel.kt에서 로그 추가
Log.d("LoginViewModel", "ID Token: $idToken")
Log.d("LoginViewModel", "Access Token: ${response.accessToken}")
Log.d("LoginViewModel", "User: ${response.user}")
```

### 4.3 인증 토큰 사용
```kotlin
// API 요청 시 토큰 자동 포함 (AuthInterceptor가 처리)
// 예: 사용자 정보 조회
interface UserService {
    @GET("users/me")
    suspend fun getCurrentUser(): User
}

// ViewModel에서 호출
viewModelScope.launch {
    try {
        val user = userService.getCurrentUser()
        Log.d("User", "Current user: $user")
    } catch (e: Exception) {
        Log.e("User", "Error: ${e.message}")
    }
}
```

---

## 5. 문제 해결

### 5.1 "DEVELOPER_ERROR" 또는 "10" 에러
**원인:** SHA-1 인증서 지문이 Google Cloud Console에 등록되지 않았거나 잘못됨

**해결:**
```bash
# SHA-1 다시 확인
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# 출력된 SHA-1을 Google Cloud Console에 정확히 등록
# Android 클라이언트 ID가 제대로 생성되었는지 확인
```

### 5.2 "Package name mismatch" 에러
**원인:** 앱의 패키지 이름과 Google Cloud Console에 등록된 패키지 이름이 다름

**해결:**
1. `app/build.gradle.kts`에서 `applicationId` 확인
2. Google Cloud Console → **사용자 인증 정보** → Android 클라이언트 ID
3. 패키지 이름이 정확히 일치하는지 확인 (예: `com.ieum`)

### 5.3 "Invalid Google token" 에러 (백엔드)
**원인:** 
- 만료된 ID Token
- 잘못된 Client ID
- 네트워크 문제

**해결:**
```bash
# 백엔드 로그 확인
ssh -i ~/Downloads/ieum_key.pem ubuntu@54.66.195.91
docker logs spring-app --tail 100

# GOOGLE_CLIENT_ID 환경변수 확인
docker exec spring-app env | grep GOOGLE
```

### 5.4 CORS 오류 (Android는 해당 없음)
**참고:** Android 네이티브 앱은 CORS 정책의 영향을 받지 않습니다. 웹뷰를 사용하는 경우에만 해당됩니다.

**원인:** 웹뷰에서 API 호출 시 도메인이 백엔드에서 허용되지 않음

**해결:**
백엔드에 CORS 설정 추가 필요 시:
```kotlin
// WebConfig.kt
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://54.66.195.91"
            )
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}
```

### 5.5 Credential Manager API 사용 시 에러
**원인:** Google Play Services가 최신 버전이 아니거나 설치되지 않음

**해결:**
```kotlin
// 에러 처리 추가
try {
    val result = credentialManager.getCredential(...)
} catch (e: GetCredentialException) {
    when (e) {
        is NoCredentialException -> {
            Log.e("Login", "사용 가능한 Google 계정이 없습니다")
        }
        is GetCredentialCancellationException -> {
            Log.e("Login", "사용자가 로그인을 취소했습니다")
        }
        else -> {
            Log.e("Login", "로그인 에러: ${e.message}")
        }
    }
}
```

**에뮬레이터 설정:**
- Google Play Services가 설치된 에뮬레이터 이미지 사용
- 에뮬레이터에 Google 계정 로그인 필요

### 5.6 "idToken is null" 에러
**원인:** Web Client ID를 잘못 입력했거나 Android Client ID를 입력함

**해결:**
```kotlin
// LoginScreen.kt에서 Web Client ID 사용 확인
val googleIdOption = GetGoogleIdOption.Builder()
    .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com") // ⚠️ 웹 클라이언트 ID 사용!
    .build()
```

**중요:** Credential Manager API는 **웹 클라이언트 ID**를 사용해야 합니다!

---

## 6. 보안 권장사항

### 6.1 JWT Secret 관리
- ✅ 최소 32자 이상의 무작위 문자열 사용
- ✅ 환경 변수로 관리 (코드에 하드코딩 금지)
- ✅ 프로덕션과 개발 환경 분리

### 6.2 Client ID 보호
- ⚠️ 웹 클라이언트 ID는 공개되어도 됨 (프론트엔드 코드에 포함)
- ❌ Client Secret은 절대 프론트엔드에 노출 금지
- ✅ Client Secret은 백엔드에서만 사용 (필요한 경우)

### 6.3 토큰 관리 (Android)
- ✅ DataStore 사용 (SharedPreferences보다 안전)
- ✅ 암호화된 SharedPreferences 사용 고려 (민감한 데이터)
- ✅ Access Token 만료 시간 적절히 설정 (예: 7일)
- ✅ Refresh Token 구현 고려

**암호화된 저장소 사용 (선택):**
```kotlin
// build.gradle.kts
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// EncryptedSharedPreferences 사용
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## 7. 참고 자료

- [Google Identity 공식 문서](https://developers.google.com/identity/protocols/oauth2)
- [Credential Manager API (Android)](https://developer.android.com/training/sign-in/credential-manager)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start-integrating)
- [Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [DataStore (안전한 데이터 저장)](https://developer.android.com/topic/libraries/architecture/datastore)

---

## 8. 체크리스트

### Google Cloud Console
- [ ] 프로젝트 생성
- [ ] OAuth 동의 화면 구성
- [ ] 범위(Scope) 설정
- [ ] OAuth 클라이언트 ID 생성
- [ ] 승인된 도메인/URI 등록

### 백엔드
- [ ] `application.yaml`에 Client ID 설정
- [ ] JWT Secret 설정
- [ ] 환경 변수 구성 (프로덕션)
- [ ] 빌드 및 배포

### Android 앱
- [ ] Dependencies 추가 (Credential Manager, Retrofit, Hilt, DataStore)
- [ ] AndroidManifest.xml 권한 설정
- [ ] SHA-1 인증서 지문 생성 및 Google Cloud Console 등록
- [ ] 패키지 이름 확인 및 등록 (com.ieum)
- [ ] Web Client ID 설정 (LoginScreen.kt)
- [ ] API 서비스 인터페이스 정의
- [ ] Repository 및 ViewModel 구현
- [ ] Compose LoginScreen 구현
- [ ] Hilt DI 설정
- [ ] DataStore 토큰 저장 로직 구현
- [ ] AuthInterceptor로 API 요청 시 토큰 자동 포함

### 테스트
- [ ] 백엔드 API 테스트
- [ ] 프론트엔드 로그인 테스트
- [ ] 인증이 필요한 API 호출 테스트
- [ ] 에러 처리 확인

---

## 📞 문제 발생 시
이슈가 발생하면 다음을 확인하세요:
1. 백엔드 로그: `docker logs spring-app`
2. 브라우저 콘솔 로그
3. 네트워크 탭에서 API 응답 확인
4. Google Cloud Console 설정 재확인

성공적인 OAuth 연동을 기원합니다! 🎉
