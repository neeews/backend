---
name: bdocs
description: 백엔드 소스코드를 읽어 Notion "기능 정리 > 백엔드" 페이지에 도메인별로 사용 기술/오픈소스와 작동 방식을 간단히 정리한다.
---

# bdocs — 백엔드 코드 문서화

백엔드 소스코드를 **전체 읽은 뒤** Notion "🪛 기능 정리 > 백엔드" 하위에 도메인별 페이지를 생성한다.
각 페이지에는 **이 도메인이 무엇을 하는지 · 어떤 오픈소스/기술을 썼는지 · 어떻게 동작하는지**만 간단히 담는다.
클래스 목록, 메서드 시그니처, 매개변수 표, 전체 코드는 넣지 않는다.

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

### 2단계 — 도메인별 소스 읽기

도메인 순서대로 `Read` 도구로 **위 목록의 파일**을 읽고, 이 도메인이 어떤 오픈소스/기술을 쓰는지, 어떤 흐름으로 동작하는지 파악한다.
한 도메인을 다 읽은 뒤 바로 3단계(Notion 페이지 생성)를 진행하고, 다음 도메인으로 넘어간다.

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

```
## 개요
도메인의 역할을 1~2문장으로 설명한다.

## 사용 기술 / 오픈소스
- 이 도메인에서 쓰인 라이브러리·프레임워크 기능·오픈소스를 항목별로 나열하고, 어디에 왜 쓰였는지 한 줄로 덧붙인다.
  (예: KOMORAN — 트렌딩 키워드 추출을 위한 한국어 형태소 분석기)
- 특별한 오픈소스/외부 기술이 없으면 "Spring Data JPA 기본 CRUD만 사용" 처럼 간단히 적는다.

## 작동 방식
- 요청이 들어와서 응답이 나가기까지의 흐름을 3~5줄로 설명한다.
- 핵심 클래스/메서드 이름은 언급해도 되지만 시그니처·매개변수 표·전체 코드는 넣지 않는다.
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
```

---

## 주의

- 이미 "백엔드" 페이지가 있으면 새로 만들지 말고 기존 ID를 사용한다.
- 같은 도메인 페이지가 이미 존재하는지 확인 후 중복 생성을 피한다.
- 클래스 목록, 메서드 시그니처, 매개변수 표, 전체 코드는 넣지 않는다. **개요 / 사용 기술·오픈소스 / 작동 방식** 세 항목만 간단히 작성한다.
- 엔티티 필드 상세 표나 별도 "엔티티 DB 정리" 페이지는 만들지 않는다.
