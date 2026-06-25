---
name: nsync
description: Notion 기능명세서·API명세서와 현재 코드를 비교해 추가된 엔드포인트나 기능을 Notion에 동기화한다.
---

# Notion 동기화

코드에 구현된 API/기능 중 Notion 명세서에 누락된 항목을 찾아 추가한다.

## Notion 페이지 정보

| 문서 | URL / ID |
|------|----------|
| API 명세서 DB | https://app.notion.com/p/a8f7aff125c383bb955e81d449d06595 |
| API 명세서 data_source_id | `a977aff1-25c3-83b8-b5c6-8714e1f51b8f` |
| 기능 명세서 페이지 | https://app.notion.com/p/3877aff125c3819fa7fecc7c675d2bde |

## API 명세서 DB 스키마

```
이름          → title       (API 이름)
HTTP header  → multi_select (POST / GET / DELETE / PUT / PATCH)
userDefined:URL → text      (엔드포인트 경로, 예: /auth/login)
group        → text         (그룹명, 예: auth / articles / users / rss)
Type         → multi_select (Parameter / None / JSON)
requestBody  → text         (요청 바디 설명)
responseBody → text         (응답 바디 설명)
설명          → text         (한 줄 설명)
```

---

## 실행 순서

### 1단계 — 코드에서 전체 API 목록 추출

아래 파일들을 **모두 Read**해서 @RequestMapping, @GetMapping, @PostMapping, @DeleteMapping 등을 파악한다.

```
src/main/java/com/example/neeews/auth/controller/AuthController.java
src/main/java/com/example/neeews/article/controller/ArticleController.java
src/main/java/com/example/neeews/user/controller/UserController.java
src/main/java/com/example/neeews/search/controller/SearchController.java
src/main/java/com/example/neeews/keyword/controller/KeywordController.java
src/main/java/com/example/neeews/rss/controller/RssController.java
```

각 엔드포인트를 아래 형식으로 정리한다.

```
메서드  | 전체 경로          | group   | 설명
--------|-------------------|---------|-----
POST   | /auth/signup       | auth    | 회원가입
POST   | /auth/login        | auth    | 로그인
...
```

### 2단계 — Notion API 명세서 기존 항목 조회

`notion-search` 도구를 사용해 API 명세서 데이터베이스 내 기존 항목을 조회한다.

```
query: "API 명세서"
page_url: "https://app.notion.com/p/a8f7aff125c383bb955e81d449d06595"
page_size: 25
```

검색 결과로 이미 등록된 URL 목록을 파악한다.
결과가 25개 미만이면 추가 검색 없이 진행해도 된다.

### 3단계 — 코드 vs Notion 비교

1단계에서 정리한 엔드포인트 목록과 2단계에서 조회한 Notion 항목을 비교한다.
- Notion에 URL이 없거나 이름이 없으면 → **추가 대상**
- 이미 있으면 → 건너뜀

### 4단계 — 누락 항목을 Notion API 명세서에 추가

`notion-create-pages` 도구로 추가 대상을 **한 번에** 일괄 생성한다.

```json
{
  "parent": {
    "type": "data_source_id",
    "data_source_id": "a977aff1-25c3-83b8-b5c6-8714e1f51b8f"
  },
  "pages": [
    {
      "properties": {
        "이름": "로그인",
        "HTTP header": "[\"POST\"]",
        "userDefined:URL": "/auth/login",
        "group": "auth",
        "Type": "[\"JSON\"]",
        "requestBody": "{ email, password }",
        "responseBody": "{ accessToken, refreshToken }",
        "설명": "이메일과 비밀번호로 로그인 후 JWT 토큰 반환"
      }
    }
  ]
}
```

> **주의**: HTTP header, Type은 JSON 배열 문자열로 전달 (예: `"[\"POST\"]"`)

### 5단계 — 기능 명세서 구현 상태 확인 (선택)

`notion-fetch`로 기능 명세서를 읽어 "미구현"으로 표시된 항목 중 코드에 이미 구현된 것이 있는지 확인한다.

```
id: "https://app.notion.com/p/3877aff125c3819fa7fecc7c675d2bde"
```

코드에 구현되어 있으나 Notion에서 미구현으로 표시된 항목은 사용자에게 알리고, 
업데이트 여부를 확인한 후 `notion-update-page`로 상태를 수정한다.

---

## 완료 보고

작업 완료 후 다음 형식으로 요약 보고한다.

```
✅ Notion API 명세서에 추가된 항목 (N개):
- POST /xxx — 설명
- GET  /xxx — 설명

⚠️  기능 명세서에서 미구현으로 표시되었으나 코드에 구현된 항목:
- 00. 기능명 (업데이트 여부 확인 필요)

변경 없음 항목: (이미 동기화된 것들)
```
