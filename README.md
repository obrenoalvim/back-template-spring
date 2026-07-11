English | [Português](README.pt.md)

# back-template-spring

Spring Boot backend template: Java 17, Maven, PostgreSQL + Flyway + Spring Data JPA, JWT auth via Spring Security, and a reference `Note` CRUD domain to copy for new resources. Sibling to `back-template-laravel` and `back-template-nest`.

## Features

- JWT auth with short-lived access tokens and rotating, revocable refresh tokens (refresh tokens are hashed and persisted so they can actually be revoked, not just left to expire)
- Email verification and password reset flows, with a console fallback for email in dev so there's nothing to configure to test locally
- Role-based access control (`USER` / `ADMIN`) enforced with `@PreAuthorize`, plus a reference admin-only route
- Rate limiting on `/auth/login` and `/auth/register` (Bucket4j, in-memory token bucket per IP)
- Flyway-managed schema with `hibernate.ddl-auto=validate`, so nothing drifts silently
- OpenAPI docs and Swagger UI generated from controller annotations
- Dockerized app and Postgres via docker-compose, with health checks on both
- CI on GitHub Actions: format check, unit tests, Docker build, Testcontainers integration tests
- Reference `Note` CRUD domain to model new resources on

## Tech stack

- Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator, Mail)
- PostgreSQL, Flyway migrations
- JWT via `jjwt` 0.12.6
- Bucket4j for rate limiting
- springdoc-openapi for API docs and Swagger UI
- Lombok
- JUnit 5 + Testcontainers for integration tests
- Spotless (Google Java Format) for formatting
- Docker / docker-compose
- GitHub Actions CI

## Project structure

```
src/main/java/com/example/backtemplate/
├── account/        change password, delete account
├── admin/          admin-only endpoints (list users)
├── auth/           register, login, refresh, JWT, users, roles
├── common/         base entity, API exceptions, global error handling
├── config/         security chain, JWT filter, rate limiting, OpenAPI, typed app properties
├── email/          SMTP service + console fallback for dev
├── notes/          reference CRUD domain
└── BackTemplateApplication.java

src/main/resources/
├── application.yml, application-{dev,prod,test}.yml
└── db/migration/   Flyway SQL migrations
```

## Prerequisites

Java 17, Maven (or the bundled `./mvnw`), and Docker (for Postgres, Testcontainers, and the full containerized stack).

## Quickstart

```bash
cp .env.example .env
make docker-up      # starts Postgres
make db-migrate
make dev            # runs the app on :8080 (dev profile)
```

Or run the full containerized stack (app + db):

```bash
docker compose up -d --build
```

## Scripts

| Command | Does |
|---|---|
| `make dev` | run with the dev profile (`spring-boot:run`) |
| `make build` | package the jar |
| `make start` | run the packaged jar |
| `make lint` / `make format-check` | Spotless check |
| `make format` | Spotless apply |
| `make test` | unit tests only |
| `make test-e2e` | integration tests (Testcontainers, needs Docker) |
| `make db-migrate` | apply Flyway migrations |
| `make db-generate` | scaffold a new empty timestamped migration file |
| `make docker-up` / `make docker-down` | full stack via compose |

## Endpoints

- `POST /auth/register`, `GET /auth/verify-email?token=`, `POST /auth/login`, `POST /auth/refresh`
- `POST /auth/forgot-password`, `POST /auth/reset-password`
- `PATCH /account/password`, `DELETE /account` (Bearer auth required)
- `GET/POST/PUT/DELETE /api/notes[/{id}]` (Bearer auth required) — reference CRUD
- `GET /admin/users` (admin only) — reference for protecting an admin-only route
- `GET /actuator/health`

## Roles

Every user has a `role` (`USER` or `ADMIN`, default `USER`), carried as a signed claim in the JWT (`JwtService.generateAccessToken`) and turned into a `ROLE_*` `GrantedAuthority` by `JwtAuthFilter`. Never trust a `role` value coming from a request body. `GET /admin/users` is the reference for protecting a route with `@PreAuthorize("hasRole('ADMIN')")` (needs `@EnableMethodSecurity` on `SecurityConfig`, already added). There's no self-serve way to become admin: flip the column directly for local testing (`UPDATE users SET role = 'ADMIN' WHERE email = '...'`).

## Sessions

`login` and `refresh` return `{ accessToken, refreshToken }`, both JWTs (`JwtService`). The refresh token also gets a SHA-256 hash of itself persisted in the `refresh_tokens` table so it can actually be revoked; a bare stateless JWT can't be un-issued before it expires. Access-token validation on every other request stays fully stateless. The DB lookup only happens on the `/auth/refresh` and `/auth/logout` paths.

`refresh` rotates: the old row is deleted the moment a new pair is issued, so a stolen-and-replayed refresh token stops working right after the legitimate client's next refresh. `logout` revokes a refresh token outright and is idempotent (a missing or already-revoked token still returns 200). The refresh JWT also carries a random `jti` claim. Without one, two tokens minted for the same user in the same second would be byte-identical (JWT claims are second-precision), which would silently defeat rotation.

## API documentation

OpenAPI docs are generated from Bean Validation annotations and [springdoc-openapi](https://springdoc.org/)'s `@Tag`/`@Operation`/`@SecurityRequirement` annotations on controllers. With the app running, open the Swagger UI at `/swagger-ui/index.html` (raw spec at `/v3/api-docs`): `http://localhost:8080` if you started it with `make dev`, or `http://localhost:8081` if you're running the docker-compose stack (the default host port from `.env.example`, override with `PORT`). Use the "Authorize" button with a JWT from `/auth/login` to try protected routes (`account`, `api/notes`).

## Pre-commit hook

```bash
git config core.hooksPath .githooks
```

Runs `spotless:check` before every commit.

## Env vars

See `.env.example` for the full list. `JWT_SECRET` is required (min 32 chars); the app fails fast on boot if it's missing or too short. Leave `MAIL_HOST` unset in dev to use the console fallback for transactional email: verification and reset tokens get logged instead of actually sent.

## Design notes

- `hibernate.ddl-auto` is `validate` everywhere; Flyway owns the schema. Never switch it to `update` or `create`, even locally, since it silently masks migrations you forgot to write.
- **`@ConfigurationProperties` binding path must match the YAML nesting exactly.** A flat Java field (e.g. `jwtSecret`) only binds to a flat property (`app.jwt-secret`), never to a nested YAML path like `app.jwt.secret`; that needs a real nested class (see `AppProperties.Jwt`). This bit us in practice: the mismatch silently left the field `null` no matter how the value was supplied (env var, system property, whatever), and a weak unit test masked it because a `null` field *also* fails `@NotBlank` validation, so the "fail fast" test passed for the wrong reason. If you add a new nested config group, mirror the YAML shape in a nested class instead of flattening it.
- Also avoid `@Configuration` plus `@EnableConfigurationProperties(X.class)` on the *same* class `X`: it double-registers the bean (one instance goes through proper binding, the other is a bare CGLIB-proxied instance with unbound fields), and whichever one autowiring picks up first may be the wrong one. Use plain `@ConfigurationProperties` with `@ConfigurationPropertiesScan` on the main application class instead.
- `ConditionalOnProperty` cannot express "empty vs. unset" cleanly. An env-resolved empty string (`${MAIL_HOST:}` with nothing set) still counts as "present" for a bare `@ConditionalOnProperty(name = "host")` check, so both `ConsoleEmailService` and `SmtpEmailService` activated at once. Fixed with explicit `Condition` classes keyed on `StringUtils.hasText`.
- Actuator auto-includes a mail health indicator once `spring-boot-starter-mail` is on the classpath. It tries to reach the configured (or default `localhost:587`) SMTP host and fails whenever none is configured, which is exactly the dev/console-fallback scenario this template is built around. Disabled via `management.health.mail.enabled=false`.
- The `maven:3.9.9-eclipse-temurin-17` Docker build-stage image sets `MAVEN_CONFIG=/root/.m2`; the Maven Wrapper script injects that value as the *first* CLI argument to Maven, which then tries to parse it as a lifecycle phase and fails ("Unknown lifecycle phase /root/.m2"). Cleared via `ENV MAVEN_CONFIG=""` in the Dockerfile.
- `docker-compose.yml`'s `env_file: .env` injects `.env`'s `PORT` (meant as the *host*-side port for the `ports:` mapping) into the container too, which makes Spring bind to that port internally. That desyncs from `EXPOSE 8080` and the `HEALTHCHECK`, leaving the container stuck at "health: starting" forever. Fixed by explicitly overriding `PORT=8080` in the `app` service's `environment:` block, which takes precedence over `env_file:` for the same key.
- Testcontainers needs a Docker daemon reachable from wherever tests run. GitHub-hosted Actions runners have this by default; self-hosted runners need Docker installed and the socket accessible. On some Windows/Docker Desktop combinations the npipe transport docker-java uses can fail outright (`BadRequestException` on every named pipe). If that happens locally, verify the flow manually against the compose Postgres instead (`make docker-up`, run the packaged jar, curl the endpoints) and trust CI for the actual Testcontainers run.
- JWT parsing has no clock-skew leeway configured; a dev machine with a wrong system clock will see intermittent 401s on freshly issued tokens.
- Bucket4j buckets are in-memory and per-instance: they reset on restart and don't coordinate across multiple app instances. Fine for a template or single-instance deploy; scaling horizontally would need a Redis-backed bucket store.
- `make db-generate` only scaffolds an empty timestamped file. Flyway Community has no entity-diff autogeneration, so write the SQL by hand.
- No `test:watch` command. The JVM/Maven ecosystem has no idiomatic file-watch test runner without a third-party plugin, so re-run `make test` manually or use your IDE's test runner.
- Flipping the *last* character of a base64url-encoded JWT to "tamper" it in a test can decode to the exact same bytes (unused padding bits in the final character), so the "tampered" token verifies successfully anyway and the test goes flaky. Flip a character in the middle instead.
