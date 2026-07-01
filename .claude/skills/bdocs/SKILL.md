---
name: bdocs
description: 백엔드 소스코드를 읽어 Notion "기능 정리 > 백엔드" 페이지에 도메인별 상세 문서를 생성하고, 엔티티 DB를 별도로 만들어 각 필드 정보를 정리한다.
---

# bdocs — 백엔드 코드 문서화

백엔드 소스코드를 **전체 읽은 뒤** Notion "🪛 기능 정리 > 백엔드" 하위에 도메인별 페이지를 생성한다.
각 페이지에는 **클래스 목록 · 메서드 시그니처 · 매개변수 · 반환타입 · 전체 코드**가 포함된다.

---

## Notion 페이지 정보

| 항목 | 값 |
|------|----|
| 기능 정리 페이지 ID | `38b7aff1-25c3-80bb-9ac3-f22f8e72b8de` |
| 기능 정리 URL | https://app.notion.com/p/38b7aff125c380bb9ac3f22f8e72b8de |

---

## 소스 파일 목록 (도메인별)

```
# article
src/main/java/com/example/neeews/article/controller/ArticleController.java
src/main/java/com/example/neeews/article/controller/ImageController.java
src/main/java/com/example/neeews/article/domain/Article.java
src/main/java/com/example/neeews/article/dto/response/ArticleDetailResponse.java
src/main/java/com/example/neeews/article/dto/response/ArticleResponse.java
src/main/java/com/example/neeews/article/repository/ArticleRepository.java
src/main/java/com/example/neeews/article/scheduler/ImageCleanupScheduler.java
src/main/java/com/example/neeews/article/service/ArticleService.java

# auth
src/main/java/com/example/neeews/auth/controller/AuthController.java
src/main/java/com/example/neeews/auth/domain/EmailVerification.java
src/main/java/com/example/neeews/auth/domain/RefreshToken.java
src/main/java/com/example/neeews/auth/domain/Role.java
src/main/java/com/example/neeews/auth/domain/User.java
src/main/java/com/example/neeews/auth/dto/request/EmailSendRequest.java
src/main/java/com/example/neeews/auth/dto/request/EmailVerifyRequest.java
src/main/java/com/example/neeews/auth/dto/request/LoginRequest.java
src/main/java/com/example/neeews/auth/dto/request/PasswordResetRequest.java
src/main/java/com/example/neeews/auth/dto/request/RefreshRequest.java
src/main/java/com/example/neeews/auth/dto/request/SignupRequest.java
src/main/java/com/example/neeews/auth/dto/response/TokenResponse.java
src/main/java/com/example/neeews/auth/dto/response/UserResponse.java
src/main/java/com/example/neeews/auth/repository/EmailVerificationRepository.java
src/main/java/com/example/neeews/auth/repository/RefreshTokenRepository.java
src/main/java/com/example/neeews/auth/repository/UserRepository.java
src/main/java/com/example/neeews/auth/service/AuthService.java
src/main/java/com/example/neeews/auth/service/EmailVerificationService.java

# bookmark
src/main/java/com/example/neeews/bookmark/controller/BookmarkController.java
src/main/java/com/example/neeews/bookmark/domain/Bookmark.java
src/main/java/com/example/neeews/bookmark/repository/BookmarkRepository.java
src/main/java/com/example/neeews/bookmark/service/BookmarkService.java

# keyword
src/main/java/com/example/neeews/keyword/controller/KeywordController.java
src/main/java/com/example/neeews/keyword/domain/TrendingKeyword.java
src/main/java/com/example/neeews/keyword/dto/response/TrendingKeywordResponse.java
src/main/java/com/example/neeews/keyword/repository/TrendingKeywordRepository.java
src/main/java/com/example/neeews/keyword/service/TrendingKeywordService.java

# rss
src/main/java/com/example/neeews/rss/controller/RssController.java
src/main/java/com/example/neeews/rss/domain/NewsSource.java
src/main/java/com/example/neeews/rss/scheduler/RssScheduler.java
src/main/java/com/example/neeews/rss/service/RssFetchService.java

# search
src/main/java/com/example/neeews/search/controller/SearchController.java
src/main/java/com/example/neeews/search/domain/SearchHistory.java
src/main/java/com/example/neeews/search/repository/SearchHistoryRepository.java
src/main/java/com/example/neeews/search/service/SearchHistoryService.java

# suggestion
src/main/java/com/example/neeews/suggestion/controller/SuggestionController.java
src/main/java/com/example/neeews/suggestion/domain/Suggestion.java
src/main/java/com/example/neeews/suggestion/domain/SuggestionStatus.java
src/main/java/com/example/neeews/suggestion/dto/request/SuggestionRequest.java
src/main/java/com/example/neeews/suggestion/dto/request/SuggestionStatusRequest.java
src/main/java/com/example/neeews/suggestion/dto/response/SuggestionResponse.java
src/main/java/com/example/neeews/suggestion/repository/SuggestionRepository.java
src/main/java/com/example/neeews/suggestion/service/SuggestionService.java

# user
src/main/java/com/example/neeews/user/controller/UserController.java
src/main/java/com/example/neeews/user/service/UserService.java

# admin
src/main/java/com/example/neeews/admin/controller/AdminSuggestionController.java

# security
src/main/java/com/example/neeews/security/CustomUserDetailsService.java
src/main/java/com/example/neeews/security/JwtAuthenticationFilter.java
src/main/java/com/example/neeews/security/JwtUtil.java

# config
src/main/java/com/example/neeews/config/SecurityConfig.java
src/main/java/com/example/neeews/config/WebConfig.java
src/main/java/com/example/neeews/config/DotEnvPostProcessor.java

# exception
src/main/java/com/example/neeews/exception/GlobalExceptionHandler.java
```

---

## 실행 순서

### 1단계 — "백엔드" 부모 페이지 생성

`notion-create-pages`로 "기능 정리" 하위에 **백엔드** 페이지를 생성한다.

```json
{
  "parent": { "type": "page_id", "page_id": "38b7aff1-25c3-80bb-9ac3-f22f8e72b8de" },
  "pages": [{ "title": "백엔드" }]
}
```

반환된 페이지 ID를 **BACKEND_PAGE_ID** 로 기억한다.

> 이미 "백엔드" 페이지가 존재한다면 `notion-search`로 ID를 조회해서 BACKEND_PAGE_ID로 사용한다.

---

### 2단계 — 도메인별 소스 전체 읽기

도메인 순서대로 `Read` 도구로 **위 목록의 모든 파일**을 읽는다.
한 도메인 안의 파일을 모두 읽은 뒤 바로 3단계(Notion 페이지 생성)를 진행하고, 다음 도메인으로 넘어간다.

---

### 3단계 — 도메인 페이지를 Notion에 생성

도메인별로 `notion-create-pages`를 호출해 BACKEND_PAGE_ID 하위에 페이지를 생성한다.

```json
{
  "parent": { "type": "page_id", "page_id": "<BACKEND_PAGE_ID>" },
  "pages": [{
    "title": "<도메인명> (예: Article)",
    "content": "<아래 본문 형식 참고>"
  }]
}
```

#### 본문 형식

각 클래스를 아래 구조로 작성한다. **메서드 설명과 해당 코드를 항상 한 블록에 붙여서** 작성한다.

```
## 개요
도메인의 역할을 1~2문장으로 설명한다.

---

## `ClassName`
`com.example.neeews.domain.layer`
`@RestController` `@RequestMapping("/path")` 등 클래스 어노테이션
**의존성:** Dep1, Dep2

---

### `methodName()` — GET /path (컨트롤러면 HTTP 메서드+경로, 서비스면 역할 한 줄)

한 줄 설명.

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| id | Long | ✓ | 기사 ID (@PathVariable) |
| sort | String | - | 정렬 기준, 기본값 latest |

**반환** `ResponseEntity<ArticleResponse>` — 설명

```java
@GetMapping("/{id}")
public ResponseEntity<ArticleResponse> methodName(
        @PathVariable Long id,
        @RequestParam(defaultValue = "latest") String sort) {
    return ResponseEntity.ok(service.get(id, sort));
}
```

(private 메서드도 동일하게 작성, 단 매개변수 표는 생략 가능)

---

## `EntityName` — `table_name` 테이블 (엔티티는 이 형식)

| 필드 | 타입 | 컬럼 | 제약 | 설명 |
|-----|------|------|------|------|
| id | Long | id | PK, AUTO | 기본 키 |

```java
// 엔티티 전체 코드
```

---

## `DtoName` (DTO는 이 형식)

| 필드 | 타입 | 제약 | 설명 |
|-----|------|------|------|
| email | String | @NotBlank | 이메일 |

```java
// DTO 전체 코드
```
```

---

### 4단계 — 엔티티 필드 요약 페이지 생성

모든 `@Entity` 클래스를 읽은 뒤, BACKEND_PAGE_ID 하위에 **"엔티티 DB 정리"** 페이지를 별도로 생성한다.

```json
{
  "parent": { "type": "page_id", "page_id": "<BACKEND_PAGE_ID>" },
  "pages": [{
    "title": "엔티티 DB 정리",
    "content": "<아래 형식>"
  }]
}
```

#### 엔티티 DB 정리 본문 형식

```
## 엔티티 목록

| 엔티티명 | 테이블명 | 도메인 |
|---------|---------|-------|
| Article | article | article |
| User    | user    | auth   |
| ...     | ...     | ...   |

---

## 엔티티 상세

### `엔티티명` (`테이블명`)

| 필드명 | Java 타입 | 컬럼명 | 제약조건 | 설명 |
|-------|---------|-------|---------|------|
| id    | Long    | id    | PK, AUTO | 기본 키 |
| title | String  | title | NOT NULL | 기사 제목 |
| ...   | ...     | ...   | ...      | ...  |

연관관계:
- `@ManyToOne` User user — 기사를 등록한 사용자
- `@OneToMany` List<Bookmark> bookmarks — 이 기사의 북마크 목록
```

---

## 엔티티 파일 목록 (4단계 전용)

```
src/main/java/com/example/neeews/article/domain/Article.java
src/main/java/com/example/neeews/auth/domain/User.java
src/main/java/com/example/neeews/auth/domain/EmailVerification.java
src/main/java/com/example/neeews/auth/domain/RefreshToken.java
src/main/java/com/example/neeews/bookmark/domain/Bookmark.java
src/main/java/com/example/neeews/keyword/domain/TrendingKeyword.java
src/main/java/com/example/neeews/rss/domain/NewsSource.java
src/main/java/com/example/neeews/search/domain/SearchHistory.java
src/main/java/com/example/neeews/suggestion/domain/Suggestion.java
```

---

## 완료 보고

모든 페이지 생성 후 아래 형식으로 보고한다.

```
✅ Notion "기능 정리 > 백엔드" 문서 생성 완료

생성된 도메인 페이지 (N개):
- Article — URL
- Auth — URL
- Bookmark — URL
- Keyword — URL
- Rss — URL
- Search — URL
- Suggestion — URL
- User — URL
- Admin — URL
- Security — URL
- Config — URL
- Exception — URL
- 엔티티 DB 정리 — URL
```

---

## 주의

- 이미 "백엔드" 페이지가 있으면 새로 만들지 말고 기존 ID를 사용한다.
- 같은 도메인 페이지가 이미 존재하는지 확인 후 중복 생성을 피한다.
- 파일 내용이 길더라도 **전체 코드를 생략하지 않는다**.
- 코드 블록은 Notion Markdown 형식(` ``` java `)을 사용한다.
- 메서드 시그니처는 private 메서드도 포함한다.
