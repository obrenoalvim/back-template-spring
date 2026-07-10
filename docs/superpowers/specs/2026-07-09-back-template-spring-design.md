# back-template-spring — Design

## Purpose

Backend template using Java/Spring Boot, sibling to `back-template-laravel` and `back-template-nest`. Must satisfy every `[Obrigatório]` item in `../../../back-checklist.md` (shared checklist, one level above the other four templates). Reference CRUD domain is `Note` (title, content, ownerId), matching the other backend templates so the set is easy to compare.

## Stack

- Java 21 (LTS), Spring Boot 3.5.x, Maven (`mvnw` wrapper committed).
- Postgres, Spring Data JPA (Hibernate) for persistence, Flyway for versioned migrations (hand-written SQL under `src/main/resources/db/migration`, `hibernate.ddl-auto=validate` only — Hibernate never mutates schema).
- Spring Security + JWT (`io.jsonwebtoken:jjwt`), stateless, mirrors `back-template-nest`'s passport-jwt shape (access token + refresh token).
- Bucket4j (in-memory) for rate limiting on `/auth/login` and `/auth/register`, keyed by client IP.
- Spring Mail (`JavaMailSender`) for transactional email; dev fallback logs the email body to console when `MAIL_HOST` is unset instead of requiring SMTP.
- SLF4J + Logback; `logstash-logback-encoder` for JSON output gated behind the `prod` profile, human-readable pattern in `dev`/`test`. Level configurable via `LOG_LEVEL` env var.
- JUnit 5 + Mockito for unit tests; Testcontainers (Postgres) for integration/e2e tests — no DB mocking, per checklist.
- Spotless with `google-java-format` for lint + format (single tool covers both `[Obrigatório]` items).

## Package layout (feature-based, mirrors Nest template)

```
com.example.notetemplate
├── account/      # change password, delete account
├── auth/         # register, login, verify-email, reset-password, JWT issuing/parsing
├── notes/        # reference CRUD (controller, service, repository, entity, DTOs)
├── email/        # EmailService interface + SMTP impl + console-fallback impl
├── health/       # thin wrapper around Actuator, or Actuator alone if sufficient
├── common/       # global exception handler, error response shape, base entity
└── config/       # env-var validation, SecurityConfig, JacksonConfig, RateLimitConfig
```

Each feature package holds its own controller/service/repository/DTOs — no separate horizontal `controllers/` or `services/` root packages. Cross-cutting concerns (error shape, security, config) live in `common`/`config` only.

## Auth flow

- Register → creates unverified `User`, sends verification email (token, 24h expiry stored on the user row).
- `GET /auth/verify-email?token=` → marks verified, single-use token.
- Login → email+password, rejects unverified accounts, returns JWT access token (short-lived) + refresh token (longer-lived, stored hashed).
- `POST /auth/forgot-password` → issues reset token (short expiry), emails link; no user-enumeration leak (always 200).
- `POST /auth/reset-password` → consumes token, updates password, invalidates existing refresh tokens.
- `PATCH /account/password`, `DELETE /account` → authenticated endpoints.
- Rate limit: 5 req/min/IP on login and register (Bucket4j, in-memory bucket per IP, no Redis — matches Nest's default in-memory throttler).

## Error shape

`@RestControllerAdvice` maps all exceptions to:
```json
{ "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [...] } }
```
Bean Validation failures, auth failures, not-found, and rate-limit-exceeded all normalize to this shape with distinct `code` values.

## Testing

- Unit: service-layer logic (password hashing, token expiry checks, rate-limit edge cases) with Mockito-mocked repositories.
- Integration/e2e: `@SpringBootTest` + Testcontainers Postgres, full HTTP round-trip via `MockMvc` or `RestAssured` against the real schema (Flyway runs on container start).
- One real example of each committed and passing before the template is considered done.

## Docker

- Multi-stage: `maven:3.9-eclipse-temurin-21` (build, `mvn -q -DskipTests package`) → `eclipse-temurin:21-jre-alpine` (runtime).
- Non-root user in runtime stage.
- `HEALTHCHECK` hits `127.0.0.1:$PORT/actuator/health` (not `localhost`, avoids IPv6 resolution trap — same rule as sibling templates).
- `docker-compose.yml`: Postgres service on a non-default host port (e.g. `5457:5432`, configurable via env) + app service with `depends_on: condition: service_healthy`.

## CI (GitHub Actions)

1. `build` — `mvn -q spotless:check compile`, then unit tests.
2. `docker` — builds the image (no push).
3. `e2e` — runs integration tests against a real Postgres service container.
4. `dependabot.yml` — weekly, ecosystems: `maven`, `docker`, `github-actions`.

## Scripts (Makefile — Maven has no native named-script convention)

`dev`, `build`, `start`, `lint`, `format`, `format:check`, `test`, `test:e2e`, `db:migrate`, `db:generate` (scaffolds a new empty timestamped Flyway file — Flyway CE has no auto-diff-from-entities), `docker:up`, `docker:down`.

**Known gap**: no `test:watch` — JVM/Maven has no idiomatic file-watch test runner without adding a third-party plugin. Documented as a limitation in the README's design notes rather than bolted on. No `db:studio` either — Flyway has no bundled data browser (checklist marks this `[Opcional]`); README points to `docker compose exec db psql` instead.

## Env validation

`@ConfigurationProperties`-backed config classes annotated `@Validated` (Jakarta Bean Validation) — missing/invalid required vars fail the Spring context on boot with a clear message, satisfying "fail fast" without a bespoke provider. `.env.example` fully commented, consumed by `docker-compose.yml` via `env_file`.

## Design notes (to document in README, mirrors sibling templates' "lessons learned" sections)

- `hibernate.ddl-auto` must stay `validate` (never `update`) — Flyway is the single source of schema truth; letting Hibernate auto-migrate silently diverges from committed migrations.
- Testcontainers needs Docker-in-Docker (or a Docker socket mount) in CI — document the GitHub Actions runner already provides this, no extra setup, but self-hosted runners would need it.
- Spotless + `google-java-format` enforces import order and 2-space-equivalent formatting; conflicts with default IntelliJ formatter — document the IDE import-order fix so contributors don't fight the formatter.
- JWT clock skew: allow a few seconds of leeway on expiry checks or clock-skewed dev machines intermittently fail auth tests.
- Bucket4j in-memory buckets reset on app restart/are per-instance — acceptable for a template/single-instance dev setup, called out as a scaling limitation (would need a Redis-backed bucket store behind multiple instances).

## Out of scope

- Multi-instance rate limiting (Redis-backed Bucket4j) — single-instance in-memory is enough for a template.
- OAuth/social login — checklist only requires email/password at minimum.
- Kotlin variant — Java only, matches the "Spring" choice made in conversation.
