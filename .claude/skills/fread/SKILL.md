---
name: fread
description: GitHub MCP를 사용해 neeews/frontend 저장소의 파일을 읽는다. 특정 파일 경로, 디렉토리 목록, 코드 검색을 지원한다.
---

# fread — 프론트엔드 코드 읽기

## 저장소 정보

| 항목 | 값 |
|------|-----|
| owner | `neeews` |
| repo | `frontend` |
| GitHub URL | https://github.com/neeews/frontend |

---

## 사용 방법

사용자가 `/fread` 뒤에 인자를 주지 않으면 저장소 루트(`/`)의 파일 목록을 먼저 보여준다.

### 1. 파일 읽기

`mcp__github__get_file_contents`를 사용한다.

```
owner: "neeews"
repo: "frontend"
path: "<읽을 파일 경로>"
```

경로 예시:
- `src/app/page.tsx` — 루트 페이지
- `src/components/ArticleCard.tsx` — 컴포넌트
- `package.json` — 의존성 목록

### 2. 디렉토리 목록 보기

`mcp__github__get_file_contents`에 **파일이 아닌 디렉토리 경로**를 넣으면 해당 디렉토리 안의 항목 목록을 반환한다.

```
owner: "neeews"
repo: "frontend"
path: "src"         # 또는 "src/components" 등
```

### 3. 코드 검색

`mcp__github__search_code`를 사용한다.

```
query: "<검색어> repo:neeews/frontend"
```

예시:
- `articleId repo:neeews/frontend` — articleId 변수/속성 검색
- `getArticleDetail repo:neeews/frontend` — 함수 호출 검색
- `imageUrl repo:neeews/frontend` — imageUrl 사용처 검색

---

## 실행 순서

1. 사용자가 파일 경로를 명시했으면 → 바로 `mcp__github__get_file_contents`로 읽는다.
2. 경로를 명시하지 않았으면 → 루트 목록을 먼저 조회해 구조를 파악한 뒤 관련 파일을 찾아 읽는다.
3. 키워드만 줬으면 → `mcp__github__search_code`로 검색한 뒤 관련 파일을 읽는다.
4. 읽은 내용을 바탕으로 사용자 질문에 답한다.

---

## 주의

- branch는 기본값(`main` 또는 저장소 기본 브랜치)을 사용한다. 별도로 지정하지 않는다.
- 파일이 크면 핵심 부분만 발췌해서 설명한다.
- 백엔드(`neeews/backend`)와 비교가 필요하면 로컬 소스(`src/`)를 함께 참조한다.
