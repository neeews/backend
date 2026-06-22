# CLAUDE.md

## Project Overview

## Tech Stack

- **Java 21** with **Lombok**
- **Spring Boot 4.1.0**
- **Spring MVC** (`spring-boot-starter-webmvc`)
- **Spring Data JPA** + **MySQL**
- **Spring Security**

## Package Structure Convention

Root package: `com.example.neeews`

- `controller` — `@RestController` classes
- `service` — business logic
- `repository` — `@Repository` / Spring Data JPA interfaces
- `domain` (or `entity`) — `@Entity` classes
- `dto` — request/response objects
- `config` — `@Configuration` classes
