# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Compile and build
./gradlew bootRun        # Run the application (port 8080)
./gradlew test           # Run all tests
./gradlew test --tests "com.core.vdesk.SomeTest"  # Run a single test class
./gradlew clean build    # Clean rebuild
```

**Local infrastructure (required before running):**
```bash
docker-compose up -d     # Start MariaDB + Redis
docker-compose down      # Stop services
```

## Architecture

Spring Boot 3.5.11 / Java 17 REST API. The code is organized into two top-level packages:

- `com.core.vdesk.domain/` — Business logic split by domain: `users`, `devices`, `emails`, `sessions`
- `com.core.vdesk.global/` — Cross-cutting infrastructure: `config`, `security`, `jwt`, `oauth2`, `exception`, `response`

### Authentication Flow

Stateless JWT-based auth with OAuth2 social login support (Google, Kakao, Naver).

1. OAuth2 login → `CustomOAuth2UserService` → `PrincipalDetails`
2. Success handler issues AccessToken + RefreshToken as cookies
3. Every request passes through `JwtAuthenticationFilter` which validates the AccessToken
4. RefreshToken (365-day) stored in Redis; used to reissue AccessTokens

### Key Patterns

- **API responses**: All endpoints return `ApiResponse<T>` wrapper with consistent structure
- **Exceptions**: Throw `BusinessException(ErrorCode.XXX)` — global handler catches and formats response
- **DTOs**: Located in `domain/{feature}/dto/` — used for request/response transformation; entities not exposed directly
- **JPA**: `ddl-auto: create` in application.yml (drops and recreates schema on startup — be careful in dev)

## Infrastructure

| Service  | Default             | Config key               |
|----------|---------------------|--------------------------|
| MariaDB  | localhost:3306      | `spring.datasource.*`    |
| Redis    | localhost:6379 db=1 | `spring.data.redis.*`    |
| App      | port 8080           | `server.port`            |

Timezone is set to `Asia/Seoul` throughout.

## Configuration Notes

`application.yml` is gitignored — it contains OAuth2 client credentials, JWT secret, email credentials, and payment gateway keys (PayPal, Paddle, Welcome). When setting up a new environment, create this file from scratch with valid credentials.

Logging is configured at DEBUG level for `org.springframework.security`, `org.springframework.web`, and `org.springframework.security.oauth2`.
