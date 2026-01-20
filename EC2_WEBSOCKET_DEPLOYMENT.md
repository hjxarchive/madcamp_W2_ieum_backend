# AWS EC2 환경 WebSocket 배포 가이드

## 📌 EC2 환경 개요

현재 이음 프로젝트는 AWS EC2 + Docker + nginx 환경에서 실행됩니다.

---

## 🔧 1. EC2 보안 그룹 설정

WebSocket 통신을 위해 다음 포트를 열어야 합니다:

### 인바운드 규칙 추가

```
포트    프로토콜    소스           설명
80      TCP        0.0.0.0/0     HTTP (nginx)
443     TCP        0.0.0.0/0     HTTPS/WSS (프로덕션)
8080    TCP        0.0.0.0/0     Spring Boot (개발 시만, 옵션)
```

### AWS 콘솔에서 설정
1. EC2 콘솔 → 인스턴스 선택
2. 보안 → 보안 그룹 클릭
3. 인바운드 규칙 편집
4. 위 규칙 추가

---

## 🐳 2. Docker Compose 설정

현재 `docker-compose.yml` 확인:

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=${DATABASE_URL}
      - JWT_SECRET=${JWT_SECRET}
    networks:
      - ieum-network

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"  # SSL 사용 시
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
      # SSL 인증서 (프로덕션)
      # - ./ssl/cert.pem:/etc/ssl/certs/cert.pem
      # - ./ssl/key.pem:/etc/ssl/private/key.pem
    depends_on:
      - app
    networks:
      - ieum-network

  db:
    image: postgres:15
    environment:
      POSTGRES_DB: ieumdb
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - ieum-network

networks:
  ieum-network:
    driver: bridge

volumes:
  postgres-data:
```

---

## 🌐 3. nginx 설정 (이미 완료)

`nginx/default.conf`는 이미 WebSocket을 지원하도록 설정되어 있습니다:

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN_OR_IP;

    # WebSocket 프록시
    location /ws/ {
        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 타임아웃
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    # 일반 HTTP
    location / {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 🚀 4. 배포 순서

### EC2에서 실행

```bash
# 1. EC2 접속
ssh -i your-key.pem ubuntu@YOUR_EC2_PUBLIC_IP

# 2. 프로젝트 디렉토리 이동
cd /path/to/madcamp_W2_ieum_backend

# 3. 최신 코드 pull
git pull origin main

# 4. 환경 변수 설정 (.env 파일)
nano .env
```

**`.env` 파일 내용:**
```env
DATABASE_URL=jdbc:postgresql://db:5432/ieumdb
DB_USER=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret_key_minimum_32_characters
GOOGLE_CLIENT_ID=your_google_client_id
```

```bash
# 5. Docker 빌드 및 실행
docker-compose down
docker-compose up --build -d

# 6. 로그 확인
docker-compose logs -f app

# 7. 정상 동작 확인
curl http://localhost:8080/actuator/health
```

---

## 📱 5. 프론트엔드 연결 주소

### 개발 환경 (HTTP)
```kotlin
val serverUrl = "ws://YOUR_EC2_PUBLIC_IP/ws/chat"
```

**예시:**
```kotlin
val serverUrl = "ws://54.180.123.45/ws/chat"  // EC2 퍼블릭 IP
```

### 프로덕션 환경 (HTTPS + 도메인)
```kotlin
val serverUrl = "wss://api.ieum.com/ws/chat"  // 도메인 사용
```

---

## 🔐 6. SSL/TLS 설정 (프로덕션)

### Let's Encrypt 무료 SSL 인증서

```bash
# 1. Certbot 설치
sudo apt update
sudo apt install certbot

# 2. 인증서 발급 (nginx 중지 필요)
sudo docker-compose stop nginx
sudo certbot certonly --standalone -d your-domain.com

# 3. 인증서 위치
# /etc/letsencrypt/live/your-domain.com/fullchain.pem
# /etc/letsencrypt/live/your-domain.com/privkey.pem

# 4. nginx 설정 업데이트
```

**nginx SSL 설정:**
```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # WebSocket 프록시 (WSS)
    location /ws/ {
        proxy_pass http://app:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        
        proxy_connect_timeout 7d;
        proxy_send_timeout 7d;
        proxy_read_timeout 7d;
    }

    location / {
        proxy_pass http://app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

# HTTP -> HTTPS 리다이렉트
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 🧪 7. 테스트 방법

### 1. 서버 접근 확인
```bash
# EC2에서
curl http://localhost:8080/actuator/health

# 외부에서
curl http://YOUR_EC2_PUBLIC_IP/actuator/health
```

### 2. WebSocket 연결 테스트 (브라우저)

브라우저 개발자 도구 콘솔에서:

```javascript
// JWT 토큰 준비 (로그인 후 받은 토큰)
const token = "eyJhbGciOiJIUzI1NiJ9...";

// WebSocket 연결
const socket = new WebSocket(`ws://YOUR_EC2_PUBLIC_IP/ws/chat?token=${token}`);

socket.onopen = () => {
    console.log('✅ WebSocket Connected!');
    
    // STOMP CONNECT 프레임
    const connectFrame = 'CONNECT\naccept-version:1.1,1.2\n\n\x00';
    socket.send(connectFrame);
};

socket.onmessage = (event) => {
    console.log('📨 Received:', event.data);
};

socket.onerror = (error) => {
    console.error('❌ Error:', error);
};

socket.onclose = () => {
    console.log('🔌 Disconnected');
};
```

### 3. Android 앱에서 테스트

```kotlin
// ChatWebSocketClient.kt
val webSocketClient = ChatWebSocketClient(
    serverUrl = "ws://54.180.123.45/ws/chat",  // 실제 EC2 IP 사용
    jwtToken = "your_jwt_token"
)

webSocketClient.connect(coupleId)
```

---

## 🐛 8. 트러블슈팅

### 연결 실패 (Connection refused)

**원인:**
- EC2 보안 그룹에서 포트 차단
- nginx가 실행되지 않음
- 잘못된 IP 주소

**해결:**
```bash
# 1. 보안 그룹 확인 (AWS 콘솔)

# 2. nginx 상태 확인
docker-compose ps

# 3. nginx 로그 확인
docker-compose logs nginx

# 4. 포트 리스닝 확인
sudo netstat -tlnp | grep 80
```

---

### 502 Bad Gateway

**원인:**
- Spring Boot 앱이 실행되지 않음
- nginx와 앱 간 통신 실패

**해결:**
```bash
# 1. 앱 로그 확인
docker-compose logs app

# 2. 앱 재시작
docker-compose restart app

# 3. 네트워크 확인
docker network ls
docker network inspect madcamp_w2_ieum_backend_ieum-network
```

---

### WebSocket Upgrade 실패

**원인:**
- nginx 설정 누락
- HTTP/1.1 미지원

**해결:**
```bash
# nginx 설정 확인
docker-compose exec nginx cat /etc/nginx/conf.d/default.conf

# Upgrade 헤더 확인
# proxy_set_header Upgrade $http_upgrade;
# proxy_set_header Connection "Upgrade";
```

---

### JWT 인증 실패

**원인:**
- 잘못된 토큰
- 토큰 만료

**해결:**
```bash
# 앱 로그 확인
docker-compose logs app | grep "WebSocket"

# 로그 예시:
# WebSocket connection rejected: Invalid token
```

---

## 📊 9. 모니터링

### 로그 확인
```bash
# 전체 로그
docker-compose logs -f

# 특정 서비스
docker-compose logs -f app
docker-compose logs -f nginx

# WebSocket 관련 로그만
docker-compose logs app | grep WebSocket
```

### 리소스 모니터링
```bash
# Docker 컨테이너 상태
docker-compose ps

# 리소스 사용량
docker stats

# EC2 메모리/CPU
htop
```

---

## 🔄 10. 자동 재시작 설정

**docker-compose.yml에 restart 정책 추가:**

```yaml
services:
  app:
    restart: unless-stopped
    # ... 기존 설정

  nginx:
    restart: unless-stopped
    # ... 기존 설정

  db:
    restart: unless-stopped
    # ... 기존 설정
```

---

## 📝 11. 체크리스트

배포 전 확인사항:

- [ ] EC2 보안 그룹에 80, 443 포트 오픈
- [ ] `.env` 파일 환경변수 설정
- [ ] Docker Compose 실행 (`docker-compose up -d`)
- [ ] nginx 로그 확인 (에러 없음)
- [ ] Spring Boot 앱 로그 확인 (정상 시작)
- [ ] 브라우저에서 WebSocket 연결 테스트
- [ ] Android 앱에서 연결 테스트
- [ ] 메시지 송수신 테스트
- [ ] SSL 인증서 설정 (프로덕션)

---

## 🎯 프론트엔드에 전달할 정보

```
WebSocket 서버 주소:
- 개발: ws://[EC2_PUBLIC_IP]/ws/chat
- 프로덕션: wss://[DOMAIN]/ws/chat

인증: JWT 토큰을 query parameter로 전달
예: ws://54.180.123.45/ws/chat?token=eyJhbGc...

구독 경로: /topic/couple/{coupleId}
메시지 전송: /app/chat/{coupleId}
```

---

**작성일:** 2026-01-20  
**EC2 환경:** AWS EC2 + Docker + nginx + PostgreSQL
