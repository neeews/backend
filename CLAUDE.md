# CLAUDE.md

## Project Overview

## Tech Stack

- **Java 21** with **Lombok** (use `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, etc. to reduce boilerplate)
- **Spring Boot 4.1.0** — use Spring's standard auto-configuration
- **Spring MVC** (`spring-boot-starter-webmvc`) — REST controllers
- **Spring Data JPA** + **MySQL** — persistence layer; configure `spring.datasource.*` in `application.properties` before running
- **Spring Security** — authentication/authorization (currently unconfigured)
- 모든 설명은 한국어로

## Configuration

`src/main/resources/application.properties` currently only sets `spring.application.name=neeews`. Before running, add MySQL datasource config:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
```

## 오류 기록 규칙

작업 중 오류가 발생하고 해결한 경우, **반드시** Notion의 "⚠️ 생긴 오류" 페이지(ID: `38a7aff1-25c3-80b8-8860-cdffe4a14448`)에 아래 형식으로 추가 기록한다.

```
# 오류 제목

**발생 시점:** 언제 발생했는지

**증상**
무슨 에러 메시지가 나왔는지

**원인**
왜 발생했는지

**해결**
어떻게 고쳤는지 (코드/명령어 포함)
```

- `notion-update-page` 도구의 `insert_content` 커맨드로 기존 내용 아래에 추가한다.
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
