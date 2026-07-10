---
name: nsync
description: Notion 기능명세서·API명세서와 현재 코드를 비교해 추가된 엔드포인트나 기능을 Notion에 동기화한다.
---

# Notion 동기화

코드에 구현된 API/기능 중 Notion 명세서에 누락된 항목을 찾아 추가한다.

## Notion 페이지 정보

| 문서 | URL / ID |
|------|----------|
| API 목록 DB (neeews 페이지 하위) | https://app.notion.com/p/5b0755eccb6e4a41b46cdf74179f7622 |
| API 목록 data_source_id | `e062f354-b072-4937-b81b-f85bce1e844c` |
| 기능 명세서 페이지 | https://app.notion.com/p/3877aff125c3819fa7fecc7c675d2bde |

> **주의**: 워크스페이스에 "API 명세서"라는 이름의 DB가 여럿 있다 (잡담 하위 `a8f7aff1…`, 렌트렐라 하위 `3967aff1…`). 이들은 다른 프로젝트 것이므로 절대 사용하지 말 것. neeews의 API 명세서는 위의 **API 목록** DB다.

## API 목록 DB 스키마

```
API 이름         → title    (API 이름)
Endpoint         → text     (엔드포인트 경로, 예: /auth/login)
Method           → select   (GET / POST / PUT / DELETE / PATCH) ※ 단일 선택, 문자열 하나로 전달
Request Body 예시 → text    (요청 바디 예시 JSON, 없으면 "없음")
Response 예시     → text    (응답 예시)
상태             → select   (미구현 / 개발중 / 완료)
설명             → text     (한 줄 설명)
연결 페이지·컴포넌트 → text  (프론트 연결 지점)
인증 필요         → checkbox ("__YES__" / "__NO__")
```

---

## 실행 순서

### 1단계 — 코드에서 전체 API 목록 추출

컨트롤러는 늘었다 줄었다 하므로 먼저 `find src/main/java -name "*Controller.java"`로 전체 목록을 뽑은 뒤, 나온 파일을 **모두 Read**해서 @RequestMapping, @GetMapping, @PostMapping, @DeleteMapping 등을 파악한다.

각 엔드포인트를 아래 형식으로 정리한다.

```
메서드  | 전체 경로          | group   | 설명
--------|-------------------|---------|-----
POST   | /auth/signup       | auth    | 회원가입
POST   | /auth/login        | auth    | 로그인
...
```

### 2단계 — Notion API 명세서 기존 항목 조회

`notion-search` 도구를 사용해 API 목록 데이터베이스 내 기존 항목을 조회한다.
(`notion-query-data-sources`의 SQL 모드는 Business 플랜 전용이라 사용 불가)

```
query: "API"
data_source_url: "collection://e062f354-b072-4937-b81b-f85bce1e844c"
page_size: 25
```

검색 결과로 이미 등록된 항목(이름/Endpoint)을 파악한다.
결과가 25개(최대치)면 누락이 있을 수 있으니 도메인별 키워드(비밀번호, 검색 기록, RSS, 이미지, 관리자 등)로 추가 검색한다.

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
    "data_source_id": "e062f354-b072-4937-b81b-f85bce1e844c"
  },
  "pages": [
    {
      "properties": {
        "API 이름": "로그인",
        "Endpoint": "/auth/login",
        "Method": "POST",
        "Request Body 예시": "{ \"email\": \"...\", \"password\": \"...\" }",
        "Response 예시": "{ \"accessToken\": \"...\", \"refreshToken\": \"...\" }",
        "상태": "완료",
        "설명": "이메일과 비밀번호로 로그인 후 JWT 토큰 반환",
        "연결 페이지·컴포넌트": "로그인 페이지",
        "인증 필요": "__NO__"
      }
    }
  ]
}
```

> **주의**: Method와 상태는 select(단일 문자열), 인증 필요는 checkbox(`"__YES__"`/`"__NO__"`)로 전달

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
