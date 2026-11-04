# CLAUDE.md

## Project Overview

## Tech Stack

- **Java 21** with **Lombok** (use `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, etc. to reduce boilerplate)
- **Spring Boot 4.1.0** — use Spring's standard auto-configuration
- **Spring MVC** (`spring-boot-starter-webmvc`) — REST controllers
- **Spring Data JPA** + **MySQL** — persistence layer; configure `spring.datasource.*` in `application.properties` before running
- **Spring Security** — authentication/authorization (currently unconfigured)

## 작업 규칙

- 모든 설명은 한국어로
- push하기 전에 사용자에게 확인받을 것

## Configuration

`src/main/resources/application.properties` currently only sets `spring.application.name=neeews`. Before running, add MySQL datasource config:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
```

## 오류 기록 규칙

작업 중 오류가 발생하고 해결한 경우, **반드시** 프로젝트 루트의 `ERRORS.md` 끝에 **오류 하나당 섹션 하나**를 추가한다. 파일이 없으면 만든다. `ERRORS.md`는 `.gitignore`에 들어 있어 커밋하지 않는다.

```markdown
## 오류 제목

**발생 시점:** YYYY-MM-DD, 무엇을 하다가

**증상**
...

**원인**
...

**해결**
...
```

- 섹션 제목 = 오류 이름 (예: `rome-modules 의존성 누락으로 Docker 빌드 실패`)
- 본문은 **발생 시점 / 증상 / 원인 / 해결** 순서로 작성
- 사소한 오탈자 수정은 제외하고, 빌드 실패·서버 오류·설정 오류 등 재현 가능한 문제만 기록한다.

## Package Structure Convention

Root package: `com.example.neeews`

As the project grows, follow the function base under this root. Example:
- `controller` — `@RestController` classes
- `service` — business logic
- `repository` — `@Repository` / Spring Data JPA interfaces
- `domain` (or `entity`) — `@Entity` classes
- `dto` — request/response objects
- `config` — `@Configuration` classes (Security, etc.)

세부 컨벤션은 아래 규칙 파일을 따른다:

@.claude/rules/architecture.md
@.claude/rules/api-convention.md
@.claude/rules/domain-convention.md
@.claude/rules/code-style.md
@.claude/rules/testing.md
