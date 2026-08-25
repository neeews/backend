# Testing

## 현재 상태

- 테스트는 아직 거의 없다 (`NeeewsApplicationTests`만 존재). 기존 테스트를 깨지 않는 선에서, 요청받은 범위에 한해 테스트를 추가한다.
- 요청받지 않은 대규모 테스트 스위트를 임의로 만들지 않는다.

## 테스트 작성 시

- 레이어에 맞는 슬라이스 테스트를 사용한다:
  - Repository 쿼리 검증 → `@DataJpaTest`
  - Controller 요청/응답, 인증 필터 동작 → `@WebMvcTest` (`spring-boot-starter-webmvc-test`)
  - 여러 레이어를 통합 검증해야 할 때만 `@SpringBootTest`
- 테스트 클래스는 `src/test/java`에 대상 클래스와 동일한 패키지 경로로 둔다.
- Security가 걸린 엔드포인트를 테스트할 땐 `spring-boot-starter-security-test`의 `@WithMockUser` 등을 사용하고, 실제 JWT 발급 로직을 테스트에서 재구현하지 않는다.
