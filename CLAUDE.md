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

## Package Structure Convention

Root package: `com.example.neeews`

As the project grows, follow the function base under this root. Example:
- `controller` — `@RestController` classes
- `service` — business logic
- `repository` — `@Repository` / Spring Data JPA interfaces
- `domain` (or `entity`) — `@Entity` classes
- `dto` — request/response objects
- `config` — `@Configuration` classes (Security, etc.)
