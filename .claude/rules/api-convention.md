# API Convention

## Controller

- `@RestController` + `@RequestMapping("/도메인루트")` + `@RequiredArgsConstructor`를 사용한다.
- 경로는 소문자 복수형 리소스명으로 시작한다 (`/articles`, `/bookmarks`).
- 메서드는 항상 `ResponseEntity<T>`를 반환한다.

## Response Shape

- 단일 도메인 리소스 응답은 전용 Response DTO(`XxxResponse`)를 반환한다.
- 여러 필드를 조합한 응답(목록 + 페이지 정보 등)은 `Map.of(...)`로 구성한다. 예: `Map.of("articles", ..., "total", ..., "page", ...)`.
- 새 Response DTO는 정적 팩토리 메서드 `from(entity)` / `of(entity, ...)`로 생성한다. 생성자를 직접 노출하지 않는다.

## Authenticated User

- 사용자 식별이 필요한 엔드포인트는 `Authentication authentication` 파라미터를 받는다.
- 이메일 추출은 반드시 `AuthUtils.resolveEmail(authentication)`을 사용한다. `authentication.getName()`을 직접 호출하거나 캐스팅해서 파싱하지 않는다.
- 비로그인 사용자도 접근 가능한 엔드포인트는 `email`이 `null`일 수 있음을 감안해 Service에서 널 체크한다.

## Error Handling

- 클라이언트 잘못(잘못된 파라미터, 존재하지 않는 리소스 등)은 `IllegalArgumentException`을 던진다. `GlobalExceptionHandler`가 400으로 변환한다.
- 예외 메시지는 사용자에게 그대로 노출될 수 있으므로 한국어로, 원인을 설명하는 문구로 작성한다 (예: `"기사를 찾을 수 없습니다."`).
- 새로운 예외 클래스가 필요하면 `exception` 패키지에 추가하고 `GlobalExceptionHandler`에 `@ExceptionHandler`를 함께 등록한다. 컨트롤러나 서비스에서 try-catch로 임의 처리하지 않는다.
- 예상 못한 서버 오류는 `GlobalExceptionHandler`의 `handleException`이 잡아 500으로 응답한다 — 이 핸들러를 우회하는 별도 catch-all을 추가하지 않는다.

## HTTP Status

- `ResponseEntity.ok(...)`, `ResponseEntity.badRequest()`, `ResponseEntity.status(HttpStatus.XXX)`처럼 Spring이 제공하는 헬퍼/상수를 사용한다. 상태 코드를 숫자로 하드코딩하지 않는다 (`ResponseEntity.status(500)` 금지).
