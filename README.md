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

### 사용자 `/users`

| 메서드 | 경로 | 설명 | 인증 필요 |
|---|---|---|---|
| GET | `/users/me` | 내 프로필 조회 | O |
| GET | `/users/me/bookmarks` | 내 북마크 목록 | O |
| DELETE | `/users/me` | 회원 탈퇴 | O |

### RSS 수집 `/rss`

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
| IT/과학 | 전자신문, ZDNet 코리아, 연합뉴스, 한겨레 |

## 패키지 구조

```
com.example.neeews
├── article       # 뉴스 기사 조회
├── auth          # 인증 (회원가입, 로그인, JWT, 이메일 인증)
├── bookmark      # 북마크
├── config        # Security, DotEnv 설정
├── exception     # 전역 예외 처리
├── keyword       # 인기 키워드
├── rss           # RSS 수집 스케줄러
├── search        # 기사 검색
├── security      # JWT 필터, UserDetails
└── user          # 사용자 프로필 관리
```
