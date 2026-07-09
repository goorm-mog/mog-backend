# MOG — 모임의 모든 것을 한 곳에서

> 약속 잡기부터 정산, 기록까지 — 모임의 전 과정을 하나의 앱에서

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)

---

## 서비스 소개

친구들과 약속을 잡을 때 생기는 모든 번거로움을 해결합니다.

| 기존 방식 | MOG 사용 후 |
|---|---|
| 날짜 투표를 메신저로 따로 공유 | 카카오 톡캘린더 연동으로 자동 취합 |
| 어디서 만날지 눈치싸움 | 이동 시간 기반 중간지점 자동 계산 |
| 정산 계산 직접 해야 함 | 영수증 찍으면 Gemini AI가 자동 인식 + 1/N 계산 |
| 모임 기억은 각자의 사진첩에 | 요약 카드로 한 번에 정리 및 공유 |

---

## 주요 기능

- **카카오 소셜 로그인** — Access Token / 인가코드 두 방식 지원
- **그룹 & 방 관리** — 초대 코드 기반 참여, 단계별 방 진행
- **일정 조율** — 슬롯 생성 → 투표 → 확정, 카카오 톡캘린더 자동 등록
- **중간지점 계산** — 카카오 모빌리티 API로 이동 시간 기반 최적 장소 추천
- **실시간 채팅** — WebSocket(STOMP) 기반
- **만남 기록** — 차수별 기록, 사진 업로드(최대 3장)
- **영수증 OCR 정산** — Gemini 멀티모달 AI로 영수증 자동 인식, 1/N 계산
- **실시간 알림** — SSE + Redis Pub/Sub
- **요약 카드** — 모임 전체 정보를 카드로 생성, 카카오톡 공유

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL (AWS RDS) |
| Cache / 메시지 버스 | Redis 7 |
| 인증 | Spring Security, JWT, 카카오 OAuth2 |
| 실시간 통신 | SSE, WebSocket (STOMP) |
| 외부 API | 카카오 모빌리티, 카카오 톡캘린더, Gemini AI |
| Storage | AWS S3 |
| Infra | Docker Compose, Mac mini 자체 서버, Cloudflare DDNS |

---

## 아키텍처

```
[FE - React]
     │
     ▼
[BE - Spring Boot :8080]
     ├── PostgreSQL (AWS RDS)   ← 영속 데이터
     ├── Redis                  ← 채팅 내역, Pub/Sub 알림 버스
     └── AWS S3                 ← 사진, 요약 카드 이미지

외부 연동
     ├── 카카오 OAuth2 / 톡캘린더 / 모빌리티 API
     └── Gemini AI API (영수증 OCR)

인프라
     ├── Mac mini 자체 서버
     ├── Cloudflare DDNS (동적 IP 자동 갱신)
     └── Docker Compose (컨테이너 관리)
```

---

## 로컬 실행

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- 카카오 개발자 앱 등록 (client-id, client-secret)
- Gemini API Key

### 환경 설정

`src/main/resources/application-dev.yaml.example`을 복사해 `application-dev.yaml`을 생성하고 값을 채웁니다.

```bash
cp src/main/resources/application-dev.yaml.example src/main/resources/application-dev.yaml
```

```yaml
# application-dev.yaml 주요 항목
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mog
    username: {DB_USERNAME}
    password: {DB_PASSWORD}

kakao:
  client-id: {KAKAO_CLIENT_ID}
  client-secret: {KAKAO_CLIENT_SECRET}
  redirect-uri: http://localhost:5173/oauth/kakao

gemini:
  api-key: {GEMINI_API_KEY}
```

### 실행

```bash
# 의존 인프라 실행 (PostgreSQL, Redis)
docker compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

서버가 뜨면 Swagger UI에서 API를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

---

## 프로젝트 구조

```
src/main/java/com/mog/project/
├── domain/
│   ├── auth/          # 카카오 로그인, JWT 토큰 재발급
│   ├── chat/          # WebSocket 채팅
│   ├── groups/        # 그룹 CRUD
│   ├── meeting/       # 만남 기록, 사진 업로드, OCR
│   ├── midpoint/      # 출발지, 중간지점 계산, 확정 장소
│   ├── notification/  # SSE 알림, Redis Pub/Sub
│   ├── room/          # 방 관리, 단계 진행
│   ├── schedule/      # 일정 조율, 톡캘린더 연동
│   ├── settlement/    # 정산, 1/N 계산
│   ├── summary/       # 요약 카드
│   └── user/          # 사용자
└── global/
    ├── auth/          # JWT 필터, 카카오 OAuth 클라이언트
    ├── config/        # Security, Swagger, WebSocket, S3, Redis 설정
    ├── exception/     # 공통 에러 코드 및 예외 처리
    ├── gemini/        # Gemini AI 클라이언트
    ├── kakao/         # 카카오 모빌리티 / 톡캘린더 클라이언트
    └── redis/         # Redis Publisher / Subscriber
```

---

## 환경 분리

| 항목 | dev | prod |
|---|---|---|
| DDL | `update` (자동 반영) | `validate` (검증만) |
| SQL 로그 | `show_sql: true` | `show_sql: false` |
| 쿠키 Secure | off | on |
| 로그 레벨 (root) | INFO | WARN |
| 로그 레벨 (앱) | DEBUG | INFO |
| 민감 정보 | yaml 직접 입력 | 환경변수 `${ENV_VAR}` |

---

## API 문서

배포 서버 Swagger UI: `https://apimog-dev.leeseh0806.com/swagger-ui/index.html`

---

## 브랜치 전략

```
main          ← 프로덕션 배포
  └── develop ← 통합 개발
        ├── feature/MOG-{n}  # 기능 개발
        └── fix/MOG-{n}      # 버그 수정
```

- PR 기반 코드 리뷰 후 develop 머지
- develop → main: 릴리즈 단위 머지
