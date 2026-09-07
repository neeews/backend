# Architecture

## Package Structure

Root package: `com.example.neeews`

도메인별로 패키지를 나누고, 각 도메인 패키지 아래에 기능별 하위 패키지를 둔다.

```
com.example.neeews.<domain>/
  controller/   # @RestController
  service/      # 비즈니스 로직
  repository/   # Spring Data JPA 인터페이스
  domain/       # @Entity
  dto/
    request/    # 요청 DTO
    response/   # 응답 DTO
  scheduler/    # @Scheduled 작업 (필요한 도메인만)
```

크로스커팅 관심사는 도메인 패키지 밖에 둔다:
- `config` — `@Configuration` 클래스
- `security` — 인증/인가 관련 (JWT 필터, 유틸)
- `exception` — 전역 예외 처리

현재 도메인: `article`, `articleread`, `auth`, `bookmark`, `rss`, `search`, `suggestion`, `user`, `admin`

## Layering Rules

- Controller는 Service만 의존한다. Repository를 직접 호출하지 않는다.
- 다른 도메인의 데이터가 필요하면 그 도메인의 Service를 주입받아 사용한다 (예: `ArticleService`가 `BookmarkService`, `ArticleReadService`를 주입받음). 다른 도메인의 Repository를 직접 참조하지 않는다.
- Service 간 순환 의존이 생기면 설계를 재검토한다 — 억지로 `@Lazy`로 우회하지 않는다.
