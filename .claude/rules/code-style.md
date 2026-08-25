# Code Style

## Lombok

- `@RequiredArgsConstructor`로 생성자 주입한다. `@Autowired` 필드 주입을 쓰지 않는다.
- Controller/Service는 `@RequiredArgsConstructor` + `private final` 필드 조합을 기본으로 한다.
- Entity에는 `@Setter`를 쓰지 않는다 (domain-convention.md 참고).

## Logging

- 클래스에 `@Slf4j`를 붙이고 `log.info/warn/error`를 사용한다. `System.out.println`을 쓰지 않는다.
- 예외를 로깅할 땐 `log.error("설명 메시지", e)`처럼 예외 객체를 두 번째 인자로 넘겨 스택트레이스가 남게 한다.
- 실패해도 서비스가 계속 동작해야 하는 부가 기능(이미지 캐싱, 크롤링 등)은 예외를 삼키고 `log.warn`으로만 남긴다. 핵심 트랜잭션에 영향을 주는 예외는 삼키지 않는다.

## 상수

- 매직 넘버는 클래스 상단에 `private static final`로 선언하고 의미 있는 이름을 붙인다 (예: `HOT_TOPIC_WINDOW_HOURS`).

## 주석

- 기본적으로 주석은 쓰지 않는다.
- 코드만 봐서는 "왜 이렇게 했는지" 알 수 없는 경우에만 한 줄 한국어 주석을 남긴다 (예: 특정 알고리즘 선택 이유, DB 함수의 엣지케이스 방어 이유).
- "무엇을 하는지"를 설명하는 주석(코드를 그대로 번역한 주석)은 작성하지 않는다.

## Repository

- 단순 조회는 Spring Data JPA의 메서드 이름 규칙(`findByXxx`, `existsByXxx`, `findTopNByXxx`)을 우선 사용한다.
- 복잡한 조건/정렬이 필요할 때만 `@Query`를 쓰고, 가능하면 JPQL을 사용한다.
- Native Query(`nativeQuery = true`)는 JPQL로 표현 불가능한 DB 함수(예: `TIMESTAMPDIFF`, `POW`)가 필요할 때만 사용하고, 쿼리 위에 왜 native가 필요한지 한 줄 주석을 남긴다.
