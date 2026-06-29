# CLAUDE.md

## Project Overview

## Tech Stack

- **Java 21** with **Lombok** (use `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, etc. to reduce boilerplate)
- **Spring Boot 4.1.0** — use Spring's standard auto-configuration
- **Spring MVC** (`spring-boot-starter-webmvc`) — REST controllers
- **Spring Data JPA** + **MySQL** — persistence layer; configure `spring.datasource.*` in `application.properties` before running
- **Spring Security** — authentication/authorization (currently unconfigured)
- 모든 설명은 한국어로
- push는 절대 하지 않을 것

## Configuration

`src/main/resources/application.properties` currently only sets `spring.application.name=neeews`. Before running, add MySQL datasource config:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=update
```

## 오류 기록 규칙

작업 중 오류가 발생하고 해결한 경우, **반드시** Notion "⚠️ 생긴 오류" 페이지(ID: `38a7aff1-25c3-80b8-8860-cdffe4a14448`) 안에 **오류 하나당 하위 페이지 하나**를 `notion-create-pages`로 생성한다.

```json
{
  "parent": { "type": "page_id", "page_id": "38a7aff1-25c3-80b8-8860-cdffe4a14448" },
  "pages": [{
    "title": "오류 제목",
    "content": "**발생 시점:** ...\n**증상**\n...\n**원인**\n...\n**해결**\n..."
  }]
}
```

- 페이지 제목 = 오류 이름 (예: `rome-modules 의존성 누락으로 Docker 빌드 실패`)
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
