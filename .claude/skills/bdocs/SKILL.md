---
name: bdocs
description: 백엔드 소스코드를 읽어 Notion "기능 정리 > 백엔드" 페이지에 도메인별 상세 문서를 생성하고, 엔티티 DB를 별도로 만들어 각 필드 정보를 정리한다.
---

# 백엔드 기능 문서화 (bdocs)

## Notion 페이지 정보

| 항목 | 값 |
|------|-----|
| 백엔드 페이지 ID | `38b7aff1-25c3-8023-8b32-dfcc34db35d1` |
| 백엔드 페이지 URL | https://app.notion.com/p/38b7aff125c380238b32dfcc34db35d1 |

---

## 실행 순서

### 1단계 — 백엔드 페이지 현황 확인

`notion-fetch`로 백엔드 페이지를 조회해 이미 생성된 하위 페이지/DB 목록을 파악한다.

```
id: "38b7aff1-25c3-8023-8b32-dfcc34db35d1"
```

- 이미 존재하는 도메인 페이지 → `notion-update-page`로 최신 코드 기준으로 덮어씀
- 존재하지 않는 도메인 페이지 → `notion-create-pages`로 새로 생성
- "엔티티" DB가 이미 존재하면 → 각 엔티티 항목을 `notion-update-page`로 갱신
- "엔티티" DB가 없으면 → `notion-create-database`로 새로 생성 후 항목 추가

---

### 2단계 — 소스코드 전체 읽기

아래 Bash 명령어로 읽을 파일 목록을 먼저 확인한 뒤, 각 파일을 **모두 Read**한다.

```bash
find src/main/java/com/example/neeews -type f -name "*.java" \
  \( -path "*/controller/*" -o -path "*/service/*" -o -path "*/scheduler/*" -o -path "*/domain/*" \)
```

---

### 3단계 — 엔티티 DB 생성 또는 갱신

#### DB가 없을 때 → `notion-create-database`로 생성

```json
{
  "parent": { "type": "page_id", "page_id": "38b7aff1-25c3-8023-8b32-dfcc34db35d1" },
  "title": "엔티티",
  "schema": "CREATE TABLE (\"엔티티명\" TITLE, \"테이블명\" RICH_TEXT, \"도메인\" SELECT('auth':blue, 'article':green, 'bookmark':yellow, 'search':orange, 'keyword':purple, 'user':pink, 'rss':gray))"
}
```

생성 후 `notion-create-pages`로 각 엔티티를 DB 항목으로 추가한다.

```json
{
  "parent": { "type": "data_source_id", "data_source_id": "<생성된 DB의 data_source_id>" },
  "pages": [
    {
      "properties": {
        "엔티티명": "Article",
        "테이블명": "articles",
        "도메인": "[\"article\"]"
      },
      "content": "| 필드 | 타입 | 설명 |\n|------|------|------|\n| id | Long | PK, 자동 증가 |\n| title | String | 기사 제목 |\n..."
    }
  ]
}
```

#### DB가 이미 있을 때 → 각 항목을 `notion-update-page`로 content 갱신

---

### 4단계 — 도메인별 하위 페이지 생성 또는 갱신

읽은 코드를 바탕으로 **7개 도메인** 페이지를 처리한다.

#### 존재하지 않는 페이지 → `notion-create-pages`로 일괄 생성

```json
{
  "parent": { "type": "page_id", "page_id": "38b7aff1-25c3-8023-8b32-dfcc34db35d1" },
  "pages": [
    { "title": "auth — 인증", "content": "..." },
    { "title": "article — 기사", "content": "..." },
    { "title": "rss — RSS 수집", "content": "..." },
    { "title": "search — 검색", "content": "..." },
    { "title": "keyword — 인기 키워드", "content": "..." },
    { "title": "bookmark — 북마크", "content": "..." },
    { "title": "user — 사용자", "content": "..." }
  ]
}
```

#### 이미 존재하는 페이지 → `notion-update-page`로 내용 갱신

```json
{
  "id": "<기존 페이지 ID>",
  "content": "...(최신 코드 기준으로 다시 작성한 전체 내용)..."
}
```

---

## 도메인 페이지 템플릿

```
## 목적
이 도메인이 왜 존재하는지, 어떤 문제를 해결하는지 2~4문장으로 설명.

---

## 사용 클래스

| 종류 | 클래스명 | 역할 |
|------|----------|------|
| Controller | XxxController | HTTP 요청 수신 및 응답 |
| Service | XxxService | 비즈니스 로직 |

---

## API 목록

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | /auth/login | 불필요 | 로그인, JWT 반환 |
| ... | ... | ... | ... |

---

## 서비스 메서드 상세

### 메서드명(파라미터)
- **동작**: 무엇을 하는지 구체적으로 설명. 조건 분기가 있으면 "A이면 → B, C이면 → D" 형식으로 나열
- **사용 클래스**: 어떤 Repository/Service를 호출하는지
- **반환**: 무엇을 반환하는지

(모든 public 메서드를 위 형식으로 나열)
```

---

## 엔티티 항목 content 작성 지침

각 엔티티의 page content는 필드 테이블 하나만 작성한다.

```
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK, 자동 증가 |
| fieldName | String | 어떤 값이 들어가는지 한 줄 설명 |
| ... | ... | ... |
```

- `@Column(nullable = false)` → 설명에 "필수" 표기
- `@Column(unique = true)` → 설명에 "중복 불가" 표기
- `@Enumerated` → 타입에 enum 이름 표시 (예: `NewsSource (enum)`)
- `@Builder.Default` 기본값이 있으면 설명에 표기 (예: "기본값 0")
- 연관관계 (`@ManyToOne` 등) → 타입에 참조 엔티티 표시 (예: `User (FK)`)

---

## 도메인 페이지 작성 지침

### 목적
- 이 도메인이 없으면 어떤 기능이 안 되는지 기준으로 설명

### API 목록
- Controller의 `@RequestMapping` + 각 메서드의 매핑 어노테이션으로 전체 경로 조합
- 인증 여부: `Authentication` 파라미터 있으면 "필요", 없으면 "불필요"

### 서비스 메서드 상세
- `private` 메서드는 제외, `public` 메서드만 문서화
- `@Transactional(readOnly = true)` → "읽기 전용 트랜잭션" 표기
- `@Scheduled` → "스케줄러 (cron: xxx 또는 fixedDelay: xxxms)" 표기
- 조건 분기 로직이 있으면 **동작** 항목에 "A이면 → B, C이면 → D" 형식으로 반드시 명시

---

## 완료 보고

```
✅ 새로 생성된 도메인 페이지 (N개): ...
🔄 갱신된 도메인 페이지 (N개): ...
🗃️ 엔티티 DB: 새로 생성 / 기존 갱신 (N개 항목)
```
