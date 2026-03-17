# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Compile and build
./gradlew bootRun        # Run the application (port 8080)
./gradlew test           # Run all tests
./gradlew test --tests "com.core.vdesk.SomeTest"  # Run a single test class
./gradlew compileJava    # Compile only (fast error check)
./gradlew clean build    # Clean rebuild
```

**Local infrastructure (required before running):**
```bash
docker-compose up -d     # Start MariaDB + Redis
docker-compose down      # Stop services
```

## Architecture

Spring Boot 3.5.11 / Java 17. Three top-level packages:

- `com.core.vdesk.domain/` — Business logic: `users`, `devices`, `sessions`, `payments`, `welcome`, `emails`
- `com.core.vdesk.global/` — Cross-cutting: `config`, `security`, `jwt`, `oauth2`, `exception`, `response`, `web`
- `com.core.vdesk.admin/` — Admin-only features: `supportPost` (FAQ/INQUIRY/PARTNERSHIP/NOTICE board)

### Authentication Flow

Stateless JWT in HttpOnly cookies. OAuth2 social login (Google, Kakao, Naver).

1. OAuth2 login → `CustomOauth2UserService` → `PrincipalDetails`
2. `Oauth2SuccessHandler` issues AT+RT via `TokenService.issueAll(email)`, writes cookies via `AuthCookieUtil`
3. Redirect: `ROLE_ADMIN` → `/admin`, others → `/dashboard`
4. Every request: `JwtAuthenticationFilter` reads "AT" cookie → validates → loads `Users` from DB → sets `SecurityContext`
5. RefreshToken stored in Redis; `POST /api/auth/refresh` reissues AT only (no RT rotation)

**`PrincipalDetails.getAuthorities()`** splits `Users.roles` (comma-separated, e.g. `"ROLE_USER,ROLE_ADMIN"`) into `SimpleGrantedAuthority` objects. Role checks everywhere must use `user.getRoles().contains("ROLE_ADMIN")` — there is no `hasRole()` method on `PrincipalDetails`.

### Frontend / Thymeleaf Layout

Page routing is handled by `PageController` (landing/auth/dashboard at `/`, `/login`, `/dashboard`, etc.) and `AdminPageController` (`/admin/**`) — both in `global/web/`.

Two distinct layout patterns:
- **Landing/user pages**: use `th:replace="~{fragments/header :: header('pageName')}"` — standard header with nav
- **Admin pages**: use `th:replace="~{fragments/admin-sidebar :: sidebar('pageName')}"` — sidebar-only, no header

Tailwind CSS is pre-compiled at `static/generated/tailwind.css` — no separate CSS build step needed.

`sec:authorize` (from `thymeleaf-extras-springsecurity6`) works in templates because `JwtAuthenticationFilter` populates `SecurityContext` before Thymeleaf renders. Use:
- `sec:authorize="!isAuthenticated()"` — guest
- `sec:authorize="isAuthenticated() and !hasRole('ADMIN')"` — logged-in regular user
- `sec:authorize="hasRole('ADMIN')"` — admin

### Key Patterns

- **API responses**: All REST endpoints return `ResponseEntity<ApiResponse<T>>` — use `ApiResponse.ok("message", data)`
- **Exceptions**: `throw new BusinessException(ErrorCode.XXX)` or `new BusinessException(ErrorCode.XXX, "custom msg")` — `GlobalExceptionHandler` catches all
- **DTOs**: Static factory methods preferred for responses (e.g. `SomeResponseDto.from(entity)`). Input DTOs use `@Valid` + bean validation annotations
- **Entities**: Never exposed directly; always mapped to DTOs in service layer
- **Roles**: `Users.roles` is a plain comma-separated `String` column — not a collection

### Domain Summary

| Domain | Key Entities | Notes |
|--------|-------------|-------|
| `users` | `Users` | `loginType`: "normal"/"google"/"kakao"/"naver" |
| `devices` | `Device`, `UserDevice` | `UserDevice` is the M:M join with `aliasName`; `HostController` (`/api/host/register`, `/api/host/heartbeat`) serves the C# desktop agent |
| `sessions` | `RemoteSession` | Two controllers: `RemoteSessionController` (`/api/remote/`) for web clients; `DeviceSessionController` (`/api/devices/sessions/`) called by C# desktop agent |
| `payments` | `Payment`, `Orders`, `UserPlan`, `Product` | Multi-gateway: Welcome (KR), PayPal, Paddle |
| `welcome` | `WelcomeBillingKey` | Korean PG integration with separate billing MID |
| `admin/supportPost` | `SupportPosts`, `InquiryDetails`, `PartnershipDetails` | `BoardType`: NOTICE/FAQ/INQUIRY/PARTNERSHIP |

### Security Rules (SecurityConfig)

```
/admin, /admin/**         → hasRole("ADMIN")  [unauthenticated → redirect /login]
/api/admin/**             → hasRole("ADMIN")  [unauthenticated → 401 JSON]
/api/auth/me + most APIs  → authenticated()
/api/auth/login etc.      → permitAll()
```

### Infrastructure

| Service | Default | Config key |
|---------|---------|------------|
| MariaDB | localhost:3306 | `spring.datasource.*` |
| Redis | localhost:6379 db=1 | `spring.data.redis.*` |
| App | port 8080 | `server.port` |

Timezone: `Asia/Seoul` throughout. `ddl-auto: update` (schema is updated, not dropped on restart).

## Configuration Notes

`application.yml` is gitignored — contains OAuth2 client credentials, JWT secret (`app.jwt.secret`, `app.jwt.access-token-expiration`), email SMTP credentials, and payment gateway keys (Welcome MIDs + sign-keys, PayPal client/secret + plan IDs, Paddle pricing IDs + webhook secret).

Logging is configured at DEBUG for `org.springframework.security`, `org.springframework.web`, and `org.springframework.security.oauth2`.

