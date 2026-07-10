English | [Português](README.pt.md)

# back-template-spring

Spring Boot backend template. Java 17, Maven, Postgres + Flyway + Spring Data JPA, JWT auth via Spring Security, reference `Note` CRUD domain. Sibling to `back-template-laravel` and `back-template-nest`.

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

Every user has a `role` (`USER` | `ADMIN`, default `USER`), carried as a signed claim in the JWT (`JwtService.generateAccessToken`) and turned into a `ROLE_*` `GrantedAuthority` by `JwtAuthFilter` — never trust a `role` from a request body. `GET /admin/users` is the reference for protecting a route: `@PreAuthorize("hasRole('ADMIN')")` (needs `@EnableMethodSecurity` on `SecurityConfig`, already added). No self-serve way to become admin — flip the column directly (`UPDATE users SET role = 'ADMIN' WHERE email = '...'`) for local testing.

## API documentation

OpenAPI docs are generated from Bean Validation annotations and [springdoc-openapi](https://springdoc.org/) `@Tag`/`@Operation`/`@SecurityRequirement` annotations on controllers. With the app running, open `http://localhost:8081/swagger-ui/index.html` for the interactive Swagger UI (raw spec at `/v3/api-docs`). Use the "Authorize" button with a JWT from `/auth/login` to try protected routes (`account`, `api/notes`).

## Pre-commit hook

```bash
git config core.hooksPath .githooks
```

Runs `spotless:check` before every commit.

## Env vars

See `.env.example` for the full list. `JWT_SECRET` is required (min 32 chars) — the app fails fast on boot if it's missing or too short. Leave `MAIL_HOST` unset in dev to use the console fallback for transactional email (verification/reset tokens get logged instead of actually sent).

## Design notes

- `hibernate.ddl-auto` is `validate` everywhere — Flyway owns the schema. Never switch it to `update`/`create`, even locally; it silently masks migrations you forgot to write.
- **`@ConfigurationProperties` binding path must match the YAML nesting exactly.** A flat Java field (e.g. `jwtSecret`) only binds to a flat property (`app.jwt-secret`), never to a nested YAML path like `app.jwt.secret` — those need a real nested class (see `AppProperties.Jwt`). This bit us: the mismatch silently left the field `null` regardless of how the value was supplied (env var, system property, whatever), and a weak unit test masked it because a `null` field *also* fails `@NotBlank` validation, so the "fail fast" test passed for the wrong reason. If you add a new nested config group, mirror the YAML shape in a nested class — don't flatten it.
- Also avoid `@Configuration` + `@EnableConfigurationProperties(X.class)` on the *same* class `X` — it double-registers the bean (one instance goes through proper binding, the other is a bare CGLIB-proxied instance with unbound fields) and whichever one autowiring picks up first may be the wrong one. Use plain `@ConfigurationProperties` + `@ConfigurationPropertiesScan` on the main application class instead.
- `ConditionalOnProperty` cannot express "empty vs. unset" cleanly — an env-resolved empty string (`${MAIL_HOST:}` with nothing set) still counts as "present" for a bare `@ConditionalOnProperty(name = "host")` check, so both `ConsoleEmailService` and `SmtpEmailService` activated at once. Fixed with explicit `Condition` classes keyed on `StringUtils.hasText`.
- Actuator auto-includes a mail health indicator once `spring-boot-starter-mail` is on the classpath; it tries to reach the configured (or default `localhost:587`) SMTP host and fails whenever none is configured — exactly the dev/console-fallback scenario this template is built around. Disabled via `management.health.mail.enabled=false`.
- The `maven:3.9.9-eclipse-temurin-17` Docker build-stage image sets `MAVEN_CONFIG=/root/.m2`; the Maven Wrapper script injects that value as the *first* CLI argument to Maven, which then tries to parse it as a lifecycle phase and fails ("Unknown lifecycle phase /root/.m2"). Cleared via `ENV MAVEN_CONFIG=""` in the Dockerfile.
- `docker-compose.yml`'s `env_file: .env` injects `.env`'s `PORT` (meant as the *host*-side port for the `ports:` mapping) into the container too, which makes Spring bind to that port internally — desyncing from `EXPOSE 8080` and the `HEALTHCHECK`, leaving the container stuck "health: starting" forever. Fixed by explicitly overriding `PORT=8080` in the `app` service's `environment:` block (which takes precedence over `env_file:` for the same key).
- Testcontainers needs a Docker daemon reachable from wherever tests run. GitHub-hosted Actions runners have this by default; self-hosted runners need Docker installed and the socket accessible. On some Windows/Docker Desktop combinations the npipe transport docker-java uses can fail outright (`BadRequestException` on every named pipe) — if that happens locally, verify the flow manually against the compose Postgres instead (`make docker-up`, run the packaged jar, curl the endpoints) and trust CI for the actual Testcontainers run.
- JWT parsing has no clock-skew leeway configured; a dev machine with a wrong system clock will see intermittent 401s on freshly issued tokens.
- Bucket4j buckets are in-memory and per-instance — resets on restart, and doesn't coordinate across multiple app instances. Fine for a template/single-instance deploy; would need a Redis-backed bucket store to scale horizontally.
- `make db-generate` only scaffolds an empty timestamped file — Flyway Community has no entity-diff autogeneration; write the SQL by hand.
- No `test:watch` — the JVM/Maven ecosystem has no idiomatic file-watch test runner without a third-party plugin; re-run `make test` manually or use your IDE's test runner.
- Flipping the *last* character of a base64url-encoded JWT to "tamper" it in a test can decode to the exact same bytes (unused padding bits in the final character) — the "tampered" token then verifies successfully anyway, making the test flaky. Flip a character in the middle instead.
