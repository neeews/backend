# neeews 백엔드

다양한 언론사의 뉴스를 RSS로 수집하고 카테고리별로 제공하는 뉴스 애그리게이터 서비스입니다.

## 기술 스택

| 분류 | 기술 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.1.0 |
| 데이터베이스 | MySQL + Spring Data JPA |
| 인증 | Spring Security + JWT |
| RSS 파싱 | Rome 2.1.0 |
| 빌드 도구 | Gradle |

## 실행 방법

### 1. 환경 변수 설정

`.env.example`을 참고하여 `.env` 파일을 생성합니다.

```env
DB_URL=jdbc:mysql://localhost:3306/neeews
DB_USERNAME=root
DB_PASSWORD=비밀번호
JWT_SECRET=시크릿키
MAIL_USERNAME=이메일주소
MAIL_PASSWORD=앱비밀번호
```

### 2. 데이터베이스 생성

```sql
CREATE DATABASE neeews CHARACTER SET utf8mb4;
```

### 3. 빌드 및 실행

```bash
./gradlew bootRun
```

### Docker로 실행

```bash
docker-compose up -d
```

## API 명세

### 인증 `/auth`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| POST | `/auth/signup` | 회원가입 | X |
| POST | `/auth/login` | 로그인 | X |
| POST | `/auth/email/send` | 이메일 인증 코드 발송 | X |
| POST | `/auth/email/verify` | 이메일 인증 코드 확인 | X |
| POST | `/auth/token/refresh` | 액세스 토큰 갱신 | X |
| POST | `/auth/logout` | 로그아웃 | X |
| GET | `/auth/me` | 내 정보 조회 | O |

### 뉴스 기사 `/articles`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/articles` | 기사 목록 (카테고리·정렬·페이지 지원) | X |
| GET | `/articles/breaking` | 속보 기사 | X |
| GET | `/articles/latest` | 최신 기사 | X |
| GET | `/articles/hot` | 인기 기사 | X |
| GET | `/articles/{id}` | 기사 상세 조회 | X |
| POST | `/articles/{id}/bookmark` | 북마크 추가 | O |
| DELETE | `/articles/{id}/bookmark` | 북마크 삭제 | O |

**쿼리 파라미터 (`GET /articles`)**

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `category` | 없음 (전체) | 정치 / 경제 / 사회 / 연예문화 / 스포츠 / 세계 / IT과학 |
| `sort` | `latest` | `latest` (최신순) / `popular` (인기순) |
| `page` | `1` | 페이지 번호 |

### 검색 `/search`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/search?q={키워드}` | 기사 검색 | X |

### 인기 키워드 `/keywords`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/keywords/trending` | 오늘의 인기 키워드 | X |

### AI 모델 `/models`

Hugging Face에서 받아 옵니다. 양자화 사본(FP8·GGUF 등)은 제외합니다.

각 모델에는 기반 모델(`baseModel`), 라이선스 이름·원문 링크(`license`, `licenseUrl`), 상업 이용 여부(`commercialUse`)가 붙습니다.
`commercialUse`는 MIT·Apache-2.0 같은 잘 알려진 라이선스만 `ALLOWED`, CC BY-NC 계열은 `NON_COMMERCIAL`로 판정하고, 나머지(직접 만든 라이선스, Llama·Gemma 등 조건부 라이선스)는 `CHECK_REQUIRED`입니다.
출시 목록은 모델 카드를 Ollama로 요약한 한국어 설명(`summary`)도 함께 내려줍니다. 트렌딩에는 없습니다.

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/models/releases?page={n}` | 주요 AI 회사의 새 모델 출시 (좋아요 `app.huggingface.release-min-likes`개 이상) | X |
| GET | `/models/trending` | 요즘 뜨는 모델 Top 10 | X |

### 논문 `/papers`

Hugging Face Daily Papers에서 받아 옵니다. HF가 전체 건수를 주지 않아 `total` 대신 다음 페이지 여부를 내려줍니다: `{ "papers": [...], "page": 1, "hasNext": true }`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/papers?page={n}` | 전체 논문, 최신순 20개씩 | X |
| GET | `/papers/daily` | 최근 이틀 치 중 추천 수 Top 10 | X |
| GET | `/papers/daily?date=2026-10-20&page={n}` | 그날 올라온 논문, 추천순 20개씩 | X |
| GET | `/papers/weekly?week=2026-W43&page={n}` | 그 주(ISO 주차)의 논문, 추천순 20개씩 | X |
| GET | `/papers/monthly?month=2026-09&page={n}` | 그 달의 논문, 추천순 20개씩 | X |

### 사용자 `/users`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/users/me` | 내 프로필 조회 | O |
| GET | `/users/me/bookmarks` | 내 북마크 목록 | O |
| DELETE | `/users/me` | 회원 탈퇴 | O |

### RSS 수집 `/rss`

모든 경로는 관리자(`ROLE_ADMIN`) 권한이 필요합니다. 자동 수집은 스케줄러가 직접 실행하므로 이 API와 무관합니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/rss/sync` | 전체 소스 수동 수집 |
| POST | `/rss/sync/{source}` | 특정 소스 수동 수집 |

## RSS 뉴스 소스

30분마다 자동으로 수집합니다.

| 카테고리 | 언론사 |
|---|---|
| 정치 | 연합뉴스, 한겨레 |
| 경제 | 연합뉴스, 한국경제 |
| 사회 | 연합뉴스, 한겨레 |
| 연예/문화 | 연합뉴스 (연예), 연합뉴스 (문화) |
| 스포츠 | 연합뉴스, 한겨레 |
| 세계 | 연합뉴스, 한겨레 |
| IT/과학 | 전자신문, ZDNet 코리아, 연합뉴스, 한겨레, 바이라인네트워크 |
| AI/개발 | AI타임스, GeekNews, OpenAI, Hugging Face, Google DeepMind, Google AI |

## 패키지 구조

```
com.example.neeews
├── article       # 뉴스 기사 조회
├── auth          # 인증 (회원가입, 로그인, JWT, 이메일 인증)
├── bookmark      # 북마크
├── config        # Security, DotEnv 설정
├── exception     # 전역 예외 처리
├── huggingface   # AI 모델 출시·트렌딩, Daily Papers (Hugging Face API)
├── keyword       # 인기 키워드
├── rss           # RSS 수집 스케줄러
├── search        # 기사 검색
├── security      # JWT 필터, UserDetails
└── user          # 사용자 프로필 관리
```
