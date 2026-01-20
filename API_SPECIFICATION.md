# IEUM API 명세서 (프론트엔드용)

## 📌 기본 정보

**Base URL:** `http://localhost:8080/api`

**인증 방식:** Bearer Token (JWT)
```
Authorization: Bearer {accessToken}
```

**공통 헤더 (인증 필요 시):**
```
X-User-Id: {UUID}
```

**응답 형식:** JSON

---

## 📋 목차

1. [Auth (인증)](#1-auth-인증)
2. [Users (사용자)](#2-users-사용자)
3. [Couples (커플)](#3-couples-커플)
4. [Events (일정)](#4-events-일정)
5. [Budget (예산)](#5-budget-예산)
6. [Expenses (지출)](#6-expenses-지출)
7. [Buckets (버킷리스트)](#7-buckets-버킷리스트)
8. [Memories (추억)](#8-memories-추억)
9. [Chat (채팅)](#9-chat-채팅)
10. [WebSocket (실시간 채팅)](#10-websocket-실시간-채팅)
11. [MBTI (성격 테스트)](#11-mbti-성격-테스트)
12. [Recommendations (추천)](#12-recommendations-추천)
13. [D-days (디데이)](#13-d-days-디데이)
14. [Files (파일)](#14-files-파일)
15. [Enum 타입](#15-enum-타입)

---

## 1. Auth (인증)

### 1.1 Google 로그인

**Endpoint:** `POST /api/auth/google`  
**인증:** 불필요  
**설명:** Google OAuth를 통한 로그인/회원가입

**Request Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6InVzZXJAZ21haWwuY29tIiwiaWF0IjoxNzA1MDAwMDAwLCJleHAiOjE3MDU2MDQ4MDB9.xxxxx",
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

---

### 1.2 로그아웃

| 항목 | 내용 |
|------|------|
| **URL** | `POST /auth/logout` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

---

### 1.3 내 정보 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /auth/me` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@gmail.com",
  "name": "홍길동",
  "nickname": "길동이",
  "profileImage": "https://...",
  "birthday": "1995-03-15",
  "gender": "M",
  "coupleId": "660e8400-e29b-41d4-a716-446655440001",
  "mbtiType": "ENFP",
  "isActive": true
}
```

---

## 2. Users (사용자)

### 2.1 회원가입 (수동)

| 항목 | 내용 |
|------|------|
| **URL** | `POST /users` |
| **인증** | 불필요 |

**Request Body:**
```json
{
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동이",
  "profileImage": "https://...",
  "birthday": "1995-03-15",
  "gender": "M",
  "googleId": null
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동이",
  "profileImage": "https://...",
  "birthday": "1995-03-15",
  "gender": "M",
  "coupleId": null,
  "mbtiType": null,
  "isActive": true
}
```

---

### 2.2 내 정보 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /users/me` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "홍길동",
  "nickname": "길동이",
  "profileImage": "https://...",
  "birthday": "1995-03-15",
  "gender": "M",
  "coupleId": "660e8400-e29b-41d4-a716-446655440001",
  "mbtiType": "ENFP",
  "isActive": true
}
```

---

### 2.3 내 정보 수정

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /users/me` |
| **인증** | 필요 |

**Request Body:** (모든 필드 선택적)
```json
{
  "name": "홍길순",
  "nickname": "길순이",
  "profileImage": "https://new-image.com/...",
  "birthday": "1995-05-20",
  "gender": "F"
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "홍길순",
  "nickname": "길순이",
  "profileImage": "https://new-image.com/...",
  "birthday": "1995-05-20",
  "gender": "F",
  "coupleId": null,
  "mbtiType": null,
  "isActive": true
}
```

---

## 3. Couples (커플)

### 3.1 초대 코드 생성

| 항목 | 내용 |
|------|------|
| **URL** | `POST /couples/invite` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "inviteCode": "ABC123",
  "expiresAt": "2024-01-12T15:30:00"
}
```

---

### 3.2 초대 코드로 연결

| 항목 | 내용 |
|------|------|
| **URL** | `POST /couples/join` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "inviteCode": "ABC123"
}
```

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "anniversary": null,
  "partner": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "partner@gmail.com",
    "name": "파트너",
    "nickname": "자기",
    "profileImage": "https://...",
    "birthday": "1996-07-20",
    "gender": "F",
    "coupleId": "660e8400-e29b-41d4-a716-446655440001",
    "mbtiType": "INFJ",
    "isActive": true
  },
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 3.3 커플 정보 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /couples/me` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "anniversary": "2023-05-15",
  "partner": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "partner@gmail.com",
    "name": "파트너",
    "nickname": "자기",
    "profileImage": "https://...",
    "birthday": "1996-07-20",
    "gender": "F",
    "coupleId": "660e8400-e29b-41d4-a716-446655440001",
    "mbtiType": "INFJ",
    "isActive": true
  },
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 3.4 커플 정보 수정

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /couples/me` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "anniversary": "2023-05-15"
}
```

**Response (200 OK):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "anniversary": "2023-05-15",
  "partner": { ... },
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 3.5 커플 연결 해제

| 항목 | 내용 |
|------|------|
| **URL** | `DELETE /couples/me` |
| **인증** | 필요 |

**Response (204 No Content)**

---

## 4. MBTI

### 4.1 질문 목록 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /mbti/questions` |
| **인증** | 불필요 |

**Response (200 OK):**
```json
{
  "questions": [
    {
      "id": 1,
      "question": "파티에서 나는?",
      "optionA": "여러 사람과 어울리며 에너지를 얻는다",
      "optionB": "소수의 친한 사람들과 깊은 대화를 나눈다",
      "dimension": "EI"
    },
    {
      "id": 2,
      "question": "새로운 사람을 만났을 때?",
      "optionA": "먼저 말을 건다",
      "optionB": "상대방이 먼저 말하기를 기다린다",
      "dimension": "EI"
    }
    // ... 총 12개 질문
  ]
}
```

---

### 4.2 답변 제출

| 항목 | 내용 |
|------|------|
| **URL** | `POST /mbti/submit` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "answers": {
    "1": "A",
    "2": "B",
    "3": "A",
    "4": "B",
    "5": "A",
    "6": "B",
    "7": "A",
    "8": "B",
    "9": "A",
    "10": "B",
    "11": "A",
    "12": "B"
  }
}
```

**Response (200 OK):**
```json
{
  "mbtiType": "ENFP",
  "details": {
    "E": 66,
    "I": 33,
    "S": 33,
    "N": 66,
    "T": 33,
    "F": 66,
    "J": 33,
    "P": 66
  }
}
```

---

### 4.3 커플 MBTI 결과 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /mbti/couple-result` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "myMbti": "ENFP",
  "partnerMbti": "INFJ",
  "compatibility": {
    "score": 75,
    "description": "좋은 궁합! 서로의 다름을 통해 성장할 수 있습니다.",
    "strengths": [
      "정보를 받아들이는 방식이 비슷합니다",
      "서로 다른 결정 방식이 균형을 이룹니다"
    ],
    "challenges": [
      "혼자만의 시간 vs 함께하는 시간에 대한 조율이 필요합니다",
      "계획적 vs 즉흥적 생활방식의 조율이 필요합니다"
    ]
  }
}
```

---

## 5. Memory (추억)

### 5.1 추억 생성

| 항목 | 내용 |
|------|------|
| **URL** | `POST /memories` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "title": "첫 데이트",
  "content": "카페에서 만나서 영화 보고 저녁 먹었다",
  "date": "2023-05-15",
  "location": "서울 강남구",
  "images": [
    "https://storage.com/image1.jpg",
    "https://storage.com/image2.jpg"
  ]
}
```

**Response (201 Created):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "title": "첫 데이트",
  "content": "카페에서 만나서 영화 보고 저녁 먹었다",
  "date": "2023-05-15",
  "location": "서울 강남구",
  "images": [
    "https://storage.com/image1.jpg",
    "https://storage.com/image2.jpg"
  ],
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 5.2 추억 목록 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /memories` |
| **인증** | 필요 |

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | int | 0 | 페이지 번호 |
| size | int | 20 | 페이지 크기 |

**Response (200 OK):**
```json
{
  "memories": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "title": "첫 데이트",
      "content": "카페에서 만나서 영화 보고 저녁 먹었다",
      "date": "2023-05-15",
      "location": "서울 강남구",
      "images": ["https://..."],
      "createdById": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2024-01-11T10:00:00"
    }
  ],
  "totalCount": 15,
  "page": 0,
  "size": 20
}
```

---

### 5.3 추억 상세 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /memories/{memoryId}` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "title": "첫 데이트",
  "content": "카페에서 만나서 영화 보고 저녁 먹었다",
  "date": "2023-05-15",
  "location": "서울 강남구",
  "images": ["https://..."],
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 5.4 추억 수정

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /memories/{memoryId}` |
| **인증** | 필요 |

**Request Body:** (모든 필드 선택적)
```json
{
  "title": "첫 데이트 (수정)",
  "content": "정말 행복했던 날",
  "date": "2023-05-15",
  "location": "서울 강남구 카페거리",
  "images": ["https://..."]
}
```

**Response (200 OK):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "title": "첫 데이트 (수정)",
  "content": "정말 행복했던 날",
  "date": "2023-05-15",
  "location": "서울 강남구 카페거리",
  "images": ["https://..."],
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 5.5 추억 삭제

| 항목 | 내용 |
|------|------|
| **URL** | `DELETE /memories/{memoryId}` |
| **인증** | 필요 |

**Response (204 No Content)**

---

## 6. Events (일정)

### 6.1 일정 생성

| 항목 | 내용 |
|------|------|
| **URL** | `POST /events` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "title": "영화 데이트",
  "description": "듄2 보기",
  "startDate": "2024-03-15T14:00:00",
  "endDate": "2024-03-15T17:00:00",
  "isAllDay": false,
  "location": "CGV 강남",
  "reminderMinutes": 60,
  "repeat": "NONE"
}
```

**repeat 값:** `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`

**Response (201 Created):**
```json
{
  "id": "880e8400-e29b-41d4-a716-446655440003",
  "title": "영화 데이트",
  "description": "듄2 보기",
  "startDate": "2024-03-15T14:00:00",
  "endDate": "2024-03-15T17:00:00",
  "isAllDay": false,
  "location": "CGV 강남",
  "reminderMinutes": 60,
  "repeat": "NONE",
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 6.2 일정 목록 조회 (기간)

| 항목 | 내용 |
|------|------|
| **URL** | `GET /events` |
| **인증** | 필요 |

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | datetime | O | 조회 시작일 (ISO 8601) |
| endDate | datetime | O | 조회 종료일 (ISO 8601) |

**예시:** `GET /events?startDate=2024-03-01T00:00:00&endDate=2024-03-31T23:59:59`

**Response (200 OK):**
```json
{
  "events": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "title": "영화 데이트",
      "description": "듄2 보기",
      "startDate": "2024-03-15T14:00:00",
      "endDate": "2024-03-15T17:00:00",
      "isAllDay": false,
      "location": "CGV 강남",
      "reminderMinutes": 60,
      "repeat": "NONE",
      "createdById": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2024-01-11T10:00:00"
    }
  ]
}
```

---

### 6.3 일정 상세 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /events/{eventId}` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "id": "880e8400-e29b-41d4-a716-446655440003",
  "title": "영화 데이트",
  "description": "듄2 보기",
  "startDate": "2024-03-15T14:00:00",
  "endDate": "2024-03-15T17:00:00",
  "isAllDay": false,
  "location": "CGV 강남",
  "reminderMinutes": 60,
  "repeat": "NONE",
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 6.4 일정 수정

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /events/{eventId}` |
| **인증** | 필요 |

**Request Body:** (모든 필드 선택적)
```json
{
  "title": "영화 데이트 (변경)",
  "location": "롯데시네마 월드타워"
}
```

**Response (200 OK):** 수정된 일정 정보

---

### 6.5 일정 삭제

| 항목 | 내용 |
|------|------|
| **URL** | `DELETE /events/{eventId}` |
| **인증** | 필요 |

**Response (204 No Content)**

---

## 7. D-days

### 7.1 디데이 목록 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /ddays` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "ddays": [
    {
      "title": "우리가 만난 날",
      "date": "2023-05-15",
      "dday": -245,
      "type": "anniversary"
    },
    {
      "title": "100일",
      "date": "2023-08-23",
      "dday": -145,
      "type": "anniversary"
    },
    {
      "title": "1주년",
      "date": "2024-05-15",
      "dday": 120,
      "type": "anniversary"
    },
    {
      "title": "영화 데이트",
      "date": "2024-03-15",
      "dday": 59,
      "type": "event"
    }
  ]
}
```

**dday 값:**
- 음수: D+xxx (지난 날)
- 0: D-Day (오늘)
- 양수: D-xxx (남은 날)

---

## 8. Chat (채팅)

### 8.1 채팅방 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /chat/room` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "coupleId": "660e8400-e29b-41d4-a716-446655440001",
  "partnerId": "550e8400-e29b-41d4-a716-446655440000",
  "partnerName": "파트너",
  "partnerProfileImage": "https://...",
  "lastMessage": {
    "id": "990e8400-e29b-41d4-a716-446655440004",
    "senderId": "550e8400-e29b-41d4-a716-446655440000",
    "content": "오늘 저녁 뭐 먹을까?",
    "type": "TEXT",
    "imageUrl": null,
    "isRead": true,
    "readAt": "2024-01-11T10:05:00",
    "createdAt": "2024-01-11T10:00:00"
  },
  "unreadCount": 3
}
```

---

### 8.2 메시지 전송

| 항목 | 내용 |
|------|------|
| **URL** | `POST /chat/rooms/{roomId}/messages` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "content": "치킨 어때?",
  "type": "TEXT",
  "imageUrl": null
}
```

**type 값:** `TEXT`, `IMAGE`, `STICKER`

**이미지 메시지 예시:**
```json
{
  "content": null,
  "type": "IMAGE",
  "imageUrl": "https://storage.com/chat/image.jpg"
}
```

**Response (201 Created):**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440005",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "치킨 어때?",
  "type": "TEXT",
  "imageUrl": null,
  "isRead": false,
  "readAt": null,
  "createdAt": "2024-01-11T10:10:00"
}
```

---

### 8.3 메시지 목록 조회 (읽음 처리)

| 항목 | 내용 |
|------|------|
| **URL** | `GET /chat/rooms/{roomId}/messages` |
| **인증** | 필요 |

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | int | 0 | 페이지 번호 |
| size | int | 50 | 페이지 크기 |

**Response (200 OK):**
```json
{
  "messages": [
    {
      "id": "990e8400-e29b-41d4-a716-446655440005",
      "senderId": "550e8400-e29b-41d4-a716-446655440000",
      "content": "치킨 어때?",
      "type": "TEXT",
      "imageUrl": null,
      "isRead": true,
      "readAt": "2024-01-11T10:15:00",
      "createdAt": "2024-01-11T10:10:00"
    },
    {
      "id": "990e8400-e29b-41d4-a716-446655440004",
      "senderId": "660e8400-e29b-41d4-a716-446655440001",
      "content": "오늘 저녁 뭐 먹을까?",
      "type": "TEXT",
      "imageUrl": null,
      "isRead": true,
      "readAt": "2024-01-11T10:05:00",
      "createdAt": "2024-01-11T10:00:00"
    }
  ],
  "totalCount": 150,
  "page": 0,
  "size": 50
}
```

---

## 10. WebSocket (실시간 채팅)

### 10.1 WebSocket 연결

**연결 URL:**
```
# 개발 환경
ws://YOUR_EC2_PUBLIC_IP/ws/chat?token={JWT_TOKEN}

# 프로덕션 환경 (SSL + 도메인)
wss://your-domain.com/ws/chat?token={JWT_TOKEN}
```

**인증:** JWT 토큰을 Query Parameter로 전달

**SockJS 폴백 지원:** ✅

---

### 10.2 STOMP 구독 (Subscribe)

클라이언트가 메시지를 받기 위해 구독:

```
SUBSCRIBE /topic/couple/{coupleId}
```

**예시:**
```
SUBSCRIBE /topic/couple/660e8400-e29b-41d4-a716-446655440001
```

---

### 10.3 메시지 전송 (Publish)

```
SEND /app/chat/{coupleId}
```

**Request Payload:**
```json
{
  "type": "TEXT",
  "content": "안녕하세요!",
  "imageUrl": null,
  "tempId": "client-temp-id-12345"
}
```

**Response (브로드캐스트):**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440005",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "senderName": "홍길동",
  "senderProfileImage": "https://...",
  "content": "안녕하세요!",
  "type": "TEXT",
  "imageUrl": null,
  "isRead": false,
  "readAt": null,
  "createdAt": "2024-01-11T10:10:00",
  "tempId": "client-temp-id-12345"
}
```

---

### 10.4 읽음 처리

```
SEND /app/chat/{coupleId}/read
```

**Request Payload:**
```json
[
  "990e8400-e29b-41d4-a716-446655440005",
  "990e8400-e29b-41d4-a716-446655440006"
]
```

**Response (브로드캐스트):**
```json
{
  "type": "READ_RECEIPT",
  "messageIds": ["990e8400-e29b-41d4-a716-446655440005"],
  "readAt": "2024-01-11T10:15:00"
}
```

---

### 10.5 타이핑 인디케이터

```
SEND /app/chat/{coupleId}/typing
```

**Response:**
```json
{
  "type": "SYSTEM",
  "event": "TYPING",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-11T10:10:00"
}
```

---

### 10.6 에러 응답

```json
{
  "type": "ERROR",
  "code": "SEND_FAILED",
  "message": "메시지 전송에 실패했습니다",
  "tempId": "client-temp-id-12345"
}
```

---

### 10.7 E2EE (암호화된 메시지)

#### 10.7.1 공개키 등록

```
PUT /api/users/me/public-key
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "publicKey": "base64_encoded_rsa_public_key"
}
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "publicKey": "base64_encoded_rsa_public_key",
  "hasKey": true
}
```

#### 10.7.2 상대방 공개키 조회

```
GET /api/users/partner/public-key
Authorization: Bearer {JWT}
```

**Response:**
```json
{
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "publicKey": "base64_encoded_rsa_public_key",
  "hasKey": true
}
```

#### 10.7.3 E2EE 메시지 전송 (WebSocket)

```
SEND /app/chat/{coupleId}/e2ee
```

**Request Payload:**
```json
{
  "type": "TEXT",
  "encryptedContent": "base64_encrypted_message",
  "encryptedKey": "base64_encrypted_aes_key",
  "iv": "base64_initialization_vector",
  "tempId": "client-temp-id-12345"
}
```

**Response (브로드캐스트):**
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440005",
  "senderId": "550e8400-e29b-41d4-a716-446655440000",
  "senderName": "홍길동",
  "senderProfileImage": "https://...",
  "type": "TEXT",
  "encryptedContent": "base64_encrypted_message",
  "encryptedKey": "base64_encrypted_aes_key",
  "iv": "base64_initialization_vector",
  "isRead": false,
  "readAt": null,
  "createdAt": "2024-01-11T10:10:00",
  "tempId": "client-temp-id-12345",
  "isEncrypted": true
}
```

**암호화 방식:**
- **초기 키 교환**: RSA-2048 (공개키 암호화)
- **메시지 암호화**: AES-256-GCM (커플당 공유 대칭키)
- **서버**: 암호문만 저장/전달 (복호화 불가)

---

### 10.8 E2EE 공개키 관리 (REST API)

#### 10.8.1 내 공개키 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/users/me/public-key` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "hasKey": true,
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
}
```

**hasKey가 false인 경우:**
```json
{
  "hasKey": false,
  "publicKey": null
}
```

---

### 10.9 E2EE 공유 대칭키 관리 (REST API)

#### 10.9.1 내 공유 대칭키 저장

| 항목 | 내용 |
|------|------|
| **URL** | `POST /api/couples/me/shared-key` |
| **인증** | 필요 |
| **설명** | User1이 자신의 공개키로 암호화한 대칭키 저장 |

**Request Body:**
```json
{
  "encryptedSharedKey": "base64_encoded_encrypted_aes_key_for_me"
}
```

**Response (200 OK):**
```json
{
  "hasSharedKey": true,
  "encryptedSharedKey": "base64_encoded_encrypted_aes_key_for_me"
}
```

---

#### 10.9.2 내 공유 대칭키 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /api/couples/me/shared-key` |
| **인증** | 필요 |
| **설명** | 내 공개키로 암호화된 대칭키 조회 (User2가 복호화하여 사용) |

**Response (200 OK):**
```json
{
  "hasSharedKey": true,
  "encryptedSharedKey": "base64_encoded_encrypted_aes_key_for_me"
}
```

**대칭키가 없는 경우:**
```json
{
  "hasSharedKey": false,
  "encryptedSharedKey": null
}
```

---

#### 10.9.3 상대방 공유 대칭키 저장

| 항목 | 내용 |
|------|------|
| **URL** | `POST /api/couples/partner/shared-key` |
| **인증** | 필요 |
| **설명** | User1이 User2의 공개키로 암호화한 대칭키 저장 |

**Request Body:**
```json
{
  "encryptedSharedKey": "base64_encoded_encrypted_aes_key_for_partner"
}
```

**Response (200 OK):**
```json
{
  "hasSharedKey": true,
  "encryptedSharedKey": "base64_encoded_encrypted_aes_key_for_partner"
}
```

---

### 10.10 E2EE 초기화 플로우

#### User1 (초대 코드 생성자):

1. **RSA 키 쌍 생성** → 로컬 저장
2. **공개키 등록**: `PUT /api/users/me/public-key`
3. **상대방 공개키 조회**: `GET /api/users/partner/public-key` (재시도)
4. **AES 대칭키 생성** → 로컬 저장
5. **내 것 암호화 저장**: `POST /api/couples/me/shared-key`
6. **상대방 것 암호화 저장**: `POST /api/couples/partner/shared-key`

#### User2 (초대 코드 입력자):

1. **RSA 키 쌍 생성** → 로컬 저장
2. **공개키 등록**: `PUT /api/users/me/public-key`
3. **암호화된 대칭키 조회**: `GET /api/couples/me/shared-key` (재시도)
4. **개인키로 복호화** → 대칭키 획득 → 로컬 저장

#### 이후 메시지 송수신:

- **송신**: 공유 대칭키로 AES 암호화 → WebSocket 전송
- **수신**: WebSocket 수신 → 공유 대칭키로 AES 복호화

---

## 9. Buckets (버킷리스트)

### 9.1 버킷리스트 생성

| 항목 | 내용 |
|------|------|
| **URL** | `POST /buckets` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "title": "제주도 여행 가기",
  "description": "우도, 성산일출봉 필수!",
  "category": "여행"
}
```

**Response (201 Created):**
```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440006",
  "title": "제주도 여행 가기",
  "description": "우도, 성산일출봉 필수!",
  "category": "여행",
  "isCompleted": false,
  "completedAt": null,
  "completedImage": null,
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 9.2 버킷리스트 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /buckets` |
| **인증** | 필요 |

**Response (200 OK):**
```json
{
  "buckets": [
    {
      "id": "aa0e8400-e29b-41d4-a716-446655440006",
      "title": "제주도 여행 가기",
      "description": "우도, 성산일출봉 필수!",
      "category": "여행",
      "isCompleted": false,
      "completedAt": null,
      "completedImage": null,
      "createdById": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2024-01-11T10:00:00"
    },
    {
      "id": "aa0e8400-e29b-41d4-a716-446655440007",
      "title": "스카이다이빙",
      "description": null,
      "category": "액티비티",
      "isCompleted": true,
      "completedAt": "2024-01-10T15:00:00",
      "completedImage": "https://storage.com/skydiving.jpg",
      "createdById": "660e8400-e29b-41d4-a716-446655440001",
      "createdAt": "2023-12-01T10:00:00"
    }
  ],
  "totalCount": 10,
  "completedCount": 3
}
```

---

### 9.3 버킷리스트 수정/완료

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /buckets/{bucketId}` |
| **인증** | 필요 |

**Request Body:** (모든 필드 선택적)
```json
{
  "title": "제주도 3박4일 여행",
  "description": "렌트카 필수",
  "category": "여행",
  "isCompleted": true,
  "completedImage": "https://storage.com/jeju.jpg"
}
```

**Response (200 OK):**
```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440006",
  "title": "제주도 3박4일 여행",
  "description": "렌트카 필수",
  "category": "여행",
  "isCompleted": true,
  "completedAt": "2024-01-11T10:30:00",
  "completedImage": "https://storage.com/jeju.jpg",
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T10:00:00"
}
```

---

### 9.4 버킷리스트 삭제

| 항목 | 내용 |
|------|------|
| **URL** | `DELETE /buckets/{bucketId}` |
| **인증** | 필요 |

**Response (204 No Content)**

---

## 10. Expenses (지출)

### 10.1 지출 추가

| 항목 | 내용 |
|------|------|
| **URL** | `POST /expenses` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "amount": 45000,
  "category": "FOOD",
  "description": "삼겹살 저녁",
  "date": "2024-01-11",
  "paidBy": "ME"
}
```

**category 값:** `FOOD`, `TRANSPORT`, `ENTERTAINMENT`, `SHOPPING`, `ETC`

**paidBy 값:** `ME`, `PARTNER`, `SPLIT`

**Response (201 Created):**
```json
{
  "id": "bb0e8400-e29b-41d4-a716-446655440008",
  "amount": 45000.00,
  "category": "FOOD",
  "description": "삼겹살 저녁",
  "date": "2024-01-11",
  "paidBy": "ME",
  "createdById": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-01-11T20:00:00"
}
```

---

### 10.2 지출 목록 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /expenses` |
| **인증** | 필요 |

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| yearMonth | string | null | 조회 월 (예: "2024-01") |
| page | int | 0 | 페이지 번호 |
| size | int | 50 | 페이지 크기 |

**예시:** `GET /expenses?yearMonth=2024-01`

**Response (200 OK):**
```json
{
  "expenses": [
    {
      "id": "bb0e8400-e29b-41d4-a716-446655440008",
      "amount": 45000.00,
      "category": "FOOD",
      "description": "삼겹살 저녁",
      "date": "2024-01-11",
      "paidBy": "ME",
      "createdById": "550e8400-e29b-41d4-a716-446655440000",
      "createdAt": "2024-01-11T20:00:00"
    }
  ],
  "totalAmount": 350000.00,
  "totalCount": 15,
  "page": 0,
  "size": 50
}
```

---

### 10.3 지출 수정

| 항목 | 내용 |
|------|------|
| **URL** | `PATCH /expenses/{expenseId}` |
| **인증** | 필요 |

**Request Body:** (모든 필드 선택적)
```json
{
  "amount": 50000,
  "description": "삼겹살 + 냉면"
}
```

**Response (200 OK):** 수정된 지출 정보

---

### 10.4 지출 삭제

| 항목 | 내용 |
|------|------|
| **URL** | `DELETE /expenses/{expenseId}` |
| **인증** | 필요 |

**Response (204 No Content)**

---

## 11. Budgets (예산)

### 11.1 월 예산 설정

| 항목 | 내용 |
|------|------|
| **URL** | `PUT /budgets/{yyyy-MM}` |
| **인증** | 필요 |

**예시:** `PUT /budgets/2024-01`

**Request Body:**
```json
{
  "totalBudget": 1000000,
  "categoryBudgets": {
    "FOOD": 400000,
    "TRANSPORT": 100000,
    "ENTERTAINMENT": 200000,
    "SHOPPING": 200000,
    "ETC": 100000
  }
}
```

**Response (200 OK):**
```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440009",
  "yearMonth": "2024-01",
  "totalBudget": 1000000.00,
  "categoryBudgets": {
    "FOOD": 400000.00,
    "TRANSPORT": 100000.00,
    "ENTERTAINMENT": 200000.00,
    "SHOPPING": 200000.00,
    "ETC": 100000.00
  },
  "totalSpent": 350000.00,
  "categorySpent": {
    "FOOD": 200000.00,
    "TRANSPORT": 50000.00,
    "ENTERTAINMENT": 100000.00
  },
  "remainingBudget": 650000.00
}
```

---

### 11.2 월 예산 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /budgets/{yyyy-MM}` |
| **인증** | 필요 |

**예시:** `GET /budgets/2024-01`

**Response (200 OK):**
```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440009",
  "yearMonth": "2024-01",
  "totalBudget": 1000000.00,
  "categoryBudgets": {
    "FOOD": 400000.00,
    "TRANSPORT": 100000.00,
    "ENTERTAINMENT": 200000.00,
    "SHOPPING": 200000.00,
    "ETC": 100000.00
  },
  "totalSpent": 350000.00,
  "categorySpent": {
    "FOOD": 200000.00,
    "TRANSPORT": 50000.00,
    "ENTERTAINMENT": 100000.00
  },
  "remainingBudget": 650000.00
}
```

---

## 12. Files (파일)

### 12.1 업로드 URL 발급 (Presigned URL)

| 항목 | 내용 |
|------|------|
| **URL** | `POST /files/presign` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "filename": "photo.jpg",
  "contentType": "image/jpeg"
}
```

**Response (200 OK):**
```json
{
  "fileId": "dd0e8400-e29b-41d4-a716-446655440010",
  "uploadUrl": "https://storage.com/upload/dd0e8400...?signature=xxx",
  "fileUrl": "https://storage.com/files/dd0e8400...",
  "expiresIn": 3600
}
```

---

### 12.2 파일 정보 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /files/{fileId}` |
| **인증** | 불필요 |

**Response (200 OK):**
```json
{
  "fileId": "dd0e8400-e29b-41d4-a716-446655440010",
  "url": "https://storage.com/files/dd0e8400...",
  "filename": "photo.jpg",
  "contentType": "image/jpeg"
}
```

---

## 13. Recommendations (추천)

### 13.1 추천 요청 생성

| 항목 | 내용 |
|------|------|
| **URL** | `POST /recommendations` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "locationAddress": "서울시 강남구",
  "locationLat": 37.4979,
  "locationLng": 127.0276,
  "date": "2024-01-20",
  "preferences": {
    "budget": "medium",
    "mood": "romantic",
    "categories": ["restaurant", "cafe", "activity"]
  }
}
```

**Response (201 Created):**
```json
{
  "id": "ee0e8400-e29b-41d4-a716-446655440011",
  "status": "PENDING",
  "locationAddress": "서울시 강남구",
  "locationLat": 37.49790000,
  "locationLng": 127.02760000,
  "date": "2024-01-20",
  "preferences": {
    "budget": "medium",
    "mood": "romantic",
    "categories": ["restaurant", "cafe", "activity"]
  },
  "result": null,
  "feedback": null,
  "savedEventId": null,
  "createdAt": "2024-01-11T10:00:00",
  "completedAt": null
}
```

**status 값:** `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

---

### 13.2 추천 목록 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /recommendations` |
| **인증** | 필요 |

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | int | 0 | 페이지 번호 |
| size | int | 20 | 페이지 크기 |

**Response (200 OK):**
```json
{
  "recommendations": [
    {
      "id": "ee0e8400-e29b-41d4-a716-446655440011",
      "status": "COMPLETED",
      "locationAddress": "서울시 강남구",
      "locationLat": 37.49790000,
      "locationLng": 127.02760000,
      "date": "2024-01-20",
      "preferences": { ... },
      "result": {
        "title": "로맨틱 강남 데이트",
        "description": "분위기 좋은 레스토랑에서 시작하는 데이트",
        "places": [
          {
            "name": "분위기 좋은 레스토랑",
            "category": "restaurant",
            "address": "서울시 강남구 테헤란로"
          },
          {
            "name": "루프탑 카페",
            "category": "cafe",
            "address": "서울시 강남구 압구정로"
          }
        ],
        "estimatedTime": "4시간",
        "estimatedCost": "80,000원"
      },
      "feedback": null,
      "savedEventId": null,
      "createdAt": "2024-01-11T10:00:00",
      "completedAt": "2024-01-11T10:05:00"
    }
  ],
  "totalCount": 5,
  "page": 0,
  "size": 20
}
```

---

### 13.3 추천 상세 조회

| 항목 | 내용 |
|------|------|
| **URL** | `GET /recommendations/{recommendationId}` |
| **인증** | 필요 |

**Response (200 OK):** 추천 상세 정보 (위 목록과 동일한 구조)

---

### 13.4 피드백 제출

| 항목 | 내용 |
|------|------|
| **URL** | `POST /recommendations/{recommendationId}/feedback` |
| **인증** | 필요 |

**Request Body:**
```json
{
  "rating": 5,
  "comment": "정말 좋았어요! 다음에도 이용할게요",
  "saveAsEvent": true
}
```

**Response (200 OK):**
```json
{
  "id": "ee0e8400-e29b-41d4-a716-446655440011",
  "status": "COMPLETED",
  "locationAddress": "서울시 강남구",
  "locationLat": 37.49790000,
  "locationLng": 127.02760000,
  "date": "2024-01-20",
  "preferences": { ... },
  "result": { ... },
  "feedback": {
    "rating": 5,
    "comment": "정말 좋았어요! 다음에도 이용할게요",
    "submittedAt": "2024-01-21T18:00:00"
  },
  "savedEventId": "880e8400-e29b-41d4-a716-446655440012",
  "createdAt": "2024-01-11T10:00:00",
  "completedAt": "2024-01-11T10:05:00"
}
```

---

## 에러 응답

모든 API는 에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "status": 400,
  "message": "에러 메시지"
}
```

### 주요 HTTP 상태 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 삭제 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 (유효성 검사 실패 등) |
| 401 | 인증 실패 (토큰 없음/만료) |
| 404 | 리소스를 찾을 수 없음 |
| 409 | 충돌 (이미 존재하는 리소스) |
| 500 | 서버 내부 오류 |

---

## 테스트용 cURL 예시

### 1. Google 로그인
```bash
curl -X POST http://localhost:8080/api/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken": "YOUR_GOOGLE_ID_TOKEN"}'
```

### 2. 내 정보 조회
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 커플 초대 코드 생성
```bash
curl -X POST http://localhost:8080/api/couples/invite \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. 추억 생성
```bash
curl -X POST http://localhost:8080/api/memories \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "첫 데이트",
    "content": "행복했던 날",
    "date": "2024-01-15",
    "location": "서울 강남"
  }'
```

### 5. 일정 조회 (기간)
```bash
curl -X GET "http://localhost:8080/api/events?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 6. 지출 추가
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 45000,
    "category": "FOOD",
    "description": "저녁 식사",
    "date": "2024-01-15",
    "paidBy": "ME"
  }'
```
