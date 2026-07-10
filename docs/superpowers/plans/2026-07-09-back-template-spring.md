# back-template-spring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot backend template satisfying every `[Obrigatório]` item in `../../../back-checklist.md`, sibling to `back-template-laravel` and `back-template-nest`, with `Note` as the reference CRUD domain.

**Architecture:** Feature-package layout (`account/ auth/ notes/ email/ config/ common/`) on Spring Boot 3.5.x + Java 17 + Maven. Postgres via Spring Data JPA (Hibernate `ddl-auto=validate` only) with Flyway owning schema. Stateless JWT auth via Spring Security. See the design spec for full rationale: `docs/superpowers/specs/2026-07-09-back-template-spring-design.md`.

**Tech Stack:** Spring Boot 3.5.0, Java 17, Maven (`mvnw`), Postgres 17, Flyway, Spring Data JPA/Hibernate, Spring Security, `jjwt` 0.12.x, Bucket4j 8.x, Spring Mail, Logback + `logstash-logback-encoder`, Lombok, JUnit 5, Mockito, Testcontainers, Spotless (`google-java-format`).

## Global Constraints

- Java 17 target (project runs Java 17 locally; Spring Boot 3.5 supports it — see design spec note).
- `groupId=com.example`, `artifactId=back-template-spring`, base package `com.example.backtemplate`.
- `hibernate.ddl-auto` must be `validate` everywhere except never (no `update`/`create`) — Flyway is the single schema source of truth.
- All error responses use the shape `{ "error": { "code": "...", "message": "...", "details": [...] } }` (Task 5) — every later task's exception handling routes through this.
- All integration tests use Testcontainers Postgres — never mock the database (checklist requirement).
- Compose Postgres host port: `5457` (non-default, avoids local Postgres collision), configurable via `DB_HOST_PORT` env var.
- Commit after every task's tests pass.

---

## File Structure

```
back-template-spring/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
├── .gitignore
├── .env.example
├── docker-compose.yml
├── Dockerfile
├── Makefile
├── README.md
├── .github/workflows/ci.yml
├── .github/dependabot.yml
├── .githooks/pre-commit
└── src/
    ├── main/java/com/example/backtemplate/
    │   ├── BackTemplateApplication.java
    │   ├── config/
    │   │   ├── AppProperties.java          (Task 10)
    │   │   ├── SecurityConfig.java          (Task 14)
    │   │   ├── JwtAuthFilter.java           (Task 14)
    │   │   └── RateLimitFilter.java         (Task 17)
    │   ├── common/
    │   │   ├── ApiException.java            (Task 5)
    │   │   ├── ErrorResponse.java           (Task 5)
    │   │   ├── GlobalExceptionHandler.java  (Task 5)
    │   │   └── BaseEntity.java              (Task 4)
    │   ├── notes/
    │   │   ├── Note.java, NoteRepository.java              (Task 4)
    │   │   ├── NoteService.java                             (Task 6)
    │   │   ├── NoteController.java                          (Task 7)
    │   │   └── dto/NoteRequest.java, dto/NoteResponse.java  (Task 6)
    │   ├── auth/
    │   │   ├── User.java, UserRepository.java, PasswordService.java  (Task 11)
    │   │   ├── JwtService.java                                        (Task 12)
    │   │   ├── AuthController.java, AuthService.java                  (Task 13, 14, 15)
    │   │   └── dto/*.java                                             (Task 13, 14, 15)
    │   ├── account/
    │   │   ├── AccountController.java, AccountService.java  (Task 16)
    │   │   └── dto/ChangePasswordRequest.java                (Task 16)
    │   └── email/
    │       ├── EmailService.java (interface)          (Task 13)
    │       ├── ConsoleEmailService.java, SmtpEmailService.java (Task 13)
    ├── main/resources/
    │   ├── application.yml, application-dev.yml, application-prod.yml, application-test.yml
    │   ├── logback-spring.xml                (Task 9)
    │   └── db/migration/
    │       ├── V1__create_users_table.sql    (Task 3, extended Task 11)
    │       └── V2__create_notes_table.sql     (Task 4)
    └── test/java/com/example/backtemplate/... (mirrors main, one test class per task)
```

---

## Task 1: Maven project scaffold

**Files:**
- Create: `pom.xml`
- Create: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- Create: `src/main/java/com/example/backtemplate/BackTemplateApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `.gitignore`

**Interfaces:**
- Produces: Maven build (`./mvnw`), Spring Boot app entrypoint `BackTemplateApplication`, base `application.yml` with `spring.application.name=back-template-spring`.

- [ ] **Step 1: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
    <relativePath/>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>back-template-spring</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>back-template-spring</name>
  <description>Spring Boot backend template</description>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.6</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.bucket4j</groupId>
      <artifactId>bucket4j-core</artifactId>
      <version>8.10.1</version>
    </dependency>
    <dependency>
      <groupId>net.logstash.logback</groupId>
      <artifactId>logstash-logback-encoder</artifactId>
      <version>7.4</version>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-testcontainers</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-maven-plugin</artifactId>
        <version>11.7.2</version>
        <configuration>
          <url>jdbc:postgresql://localhost:${DB_HOST_PORT:5457}/backtemplate</url>
          <user>${env.DB_USER:-postgres}</user>
          <password>${env.DB_PASSWORD:-postgres}</password>
        </configuration>
      </plugin>
      <plugin>
        <groupId>com.diffplug.spotless</groupId>
        <artifactId>spotless-maven-plugin</artifactId>
        <version>2.44.0</version>
        <configuration>
          <java>
            <googleJavaFormat/>
            <removeUnusedImports/>
          </java>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

Note: the Flyway plugin's `${env.DB_USER:-postgres}` bash-style default syntax does not work in Maven POM interpolation — Task 3 replaces this block with real profile-driven values once the migration exists. For now this file only needs to compile the project; Flyway plugin config is finalized in Task 3.

- [ ] **Step 2: Generate the Maven wrapper**

Run: `mvn -N io.takari:maven:wrapper -Dmaven=3.9.9` if a system Maven is available, **or** download `mvnw`/`mvnw.cmd`/`.mvn/wrapper/maven-wrapper.properties` directly from the Spring Initializr-generated equivalent (`https://start.spring.io` produces these three files with every project — no system Maven required to obtain them, they're static wrapper scripts).

Expected: `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` exist in the project root, `maven-wrapper.properties` pins `distributionUrl` to Maven 3.9.9.

- [ ] **Step 3: Write the application entrypoint**

`src/main/java/com/example/backtemplate/BackTemplateApplication.java`:

```java
package com.example.backtemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackTemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackTemplateApplication.class, args);
    }
}
```

- [ ] **Step 4: Write base `application.yml`**

`src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: back-template-spring
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

server:
  port: ${PORT:8080}
```

- [ ] **Step 5: Write `.gitignore`**

```
target/
*.class
.idea/
*.iml
.vscode/
.env
!.env.example
```

- [ ] **Step 6: Verify it compiles**

Run: `./mvnw -q compile`
Expected: `BUILD SUCCESS`, no output on success (quiet flag).

- [ ] **Step 7: Commit**

```bash
git add pom.xml mvnw mvnw.cmd .mvn .gitignore src
git commit -m "chore: scaffold Maven/Spring Boot project"
```

---

## Task 2: Docker Compose Postgres + env files

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `src/main/resources/application-dev.yml`
- Create: `src/main/resources/application-test.yml`

**Interfaces:**
- Consumes: `application.yml` profile activation from Task 1.
- Produces: running Postgres reachable at `localhost:${DB_HOST_PORT}`, `spring.datasource.*` properties every later task's `@DataSource`-dependent code relies on.

- [ ] **Step 1: Write `.env.example`**

```dotenv
# --- Database ---
DB_HOST_PORT=5457
DB_NAME=backtemplate
DB_USER=postgres
DB_PASSWORD=postgres

# --- App ---
PORT=8080
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=info

# --- Auth (Task 12) ---
JWT_SECRET=change-me-to-a-long-random-string-in-real-deployments
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=30

# --- Mail (Task 13) — leave MAIL_HOST unset in dev to use the console fallback ---
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@example.com
```

- [ ] **Step 2: Write `docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:17-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-backtemplate}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "${DB_HOST_PORT:-5457}:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-postgres} -d ${DB_NAME:-backtemplate}"]
      interval: 5s
      timeout: 5s
      retries: 10
    volumes:
      - db-data:/var/lib/postgresql/data

  app:
    build: .
    restart: unless-stopped
    env_file: .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${DB_NAME:-backtemplate}
    ports:
      - "${PORT:-8080}:8080"
    depends_on:
      db:
        condition: service_healthy

volumes:
  db-data:
```

- [ ] **Step 3: Write `application-dev.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:${DB_HOST_PORT:5457}/${DB_NAME:backtemplate}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
```

- [ ] **Step 4: Write `application-test.yml`**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Datasource properties for `test` are supplied at runtime by Testcontainers' `@ServiceConnection` (Task 4), not hardcoded here.

- [ ] **Step 5: Start Postgres and verify it's healthy**

Run:
```bash
cp .env.example .env
docker compose up -d db
docker compose ps
```
Expected: `db` service shows `healthy` status within ~10s.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml .env.example src/main/resources/application-dev.yml src/main/resources/application-test.yml
git commit -m "chore: add docker-compose Postgres service and env config"
```

---

## Task 3: Flyway V1 users table migration

**Files:**
- Create: `src/main/resources/db/migration/V1__create_users_table.sql`
- Modify: `pom.xml:flyway-maven-plugin` config block (fix the placeholder from Task 1)

**Interfaces:**
- Produces: `users` table (`id UUID PK, email UNIQUE, created_at, updated_at`) — Task 11 adds auth-specific columns via `V3__add_user_auth_columns.sql` rather than editing this file (Flyway migrations are immutable once applied).

- [ ] **Step 1: Write the migration**

`src/main/resources/db/migration/V1__create_users_table.sql`:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- [ ] **Step 2: Fix the Flyway Maven plugin config**

Replace the `flyway-maven-plugin` block in `pom.xml` with real profile-activated properties (Maven doesn't support bash `:-` defaults):

```xml
<plugin>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-maven-plugin</artifactId>
  <version>11.7.2</version>
  <configuration>
    <url>jdbc:postgresql://localhost:${db.host.port}/${db.name}</url>
    <user>${db.user}</user>
    <password>${db.password}</password>
    <locations>
      <location>classpath:db/migration</location>
    </locations>
  </configuration>
</plugin>
```

And add matching default properties to `pom.xml`'s `<properties>` block (overridable with `-Ddb.host.port=...` etc):

```xml
<db.host.port>5457</db.host.port>
<db.name>backtemplate</db.name>
<db.user>postgres</db.user>
<db.password>postgres</db.password>
```

- [ ] **Step 3: Run the migration against the compose db**

Run: `./mvnw -q process-resources flyway:migrate` (not just `flyway:migrate` — the plugin reads migrations off the compiled classpath via `classpath:db/migration`, so resources must be copied to `target/classes` first; skipping `process-resources` silently logs "No migrations found" instead of failing). Also note the plugin version must track the Spring Boot parent's managed `flyway-core` version exactly (both `11.7.2` here) — a mismatched plugin version (e.g. `10.20.1` against Boot 3.5.0's `11.7.2`-managed `flyway-core`) throws `IncompatibleClassChangeError` at runtime.
Expected: `BUILD SUCCESS`, log shows the migration applied (or "up to date" on reruns).

- [ ] **Step 4: Verify the table exists**

Run: `docker compose exec db psql -U postgres -d backtemplate -c '\d users'`
Expected: column list showing `id`, `email`, `created_at`, `updated_at`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V1__create_users_table.sql pom.xml
git commit -m "feat: add users table migration and fix Flyway plugin config"
```

---

## Task 4: Notes migration + entity + repository

**Files:**
- Create: `src/main/resources/db/migration/V2__create_notes_table.sql`
- Create: `src/main/java/com/example/backtemplate/common/BaseEntity.java`
- Create: `src/main/java/com/example/backtemplate/notes/Note.java`
- Create: `src/main/java/com/example/backtemplate/notes/NoteRepository.java`
- Test: `src/test/java/com/example/backtemplate/notes/NoteRepositoryIntegrationTest.java`
- Test: `src/test/java/com/example/backtemplate/AbstractIntegrationTest.java` (shared Testcontainers base, used by every future `@SpringBootTest`)

**Interfaces:**
- Consumes: `users.id` (Task 3) as `Note.ownerId` foreign key.
- Produces: `Note` entity (`id: UUID, title: String, content: String, ownerId: UUID, createdAt/updatedAt: Instant`), `NoteRepository extends JpaRepository<Note, UUID>` with `List<Note> findAllByOwnerId(UUID ownerId)` and `Optional<Note> findByIdAndOwnerId(UUID id, UUID ownerId)` — Task 6's `NoteService` calls these two methods by name.

- [ ] **Step 1: Write the migration**

`src/main/resources/db/migration/V2__create_notes_table.sql`:

```sql
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notes_owner_id ON notes(owner_id);
```

- [ ] **Step 2: Write the shared Testcontainers base class**

`src/test/java/com/example/backtemplate/AbstractIntegrationTest.java`:

```java
package com.example.backtemplate;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");
}
```

`@ServiceConnection` auto-wires `spring.datasource.*` from the running container — no manual `@DynamicPropertySource` needed, and Flyway runs against it automatically on context startup.

- [ ] **Step 3: Write `BaseEntity`**

`src/main/java/com/example/backtemplate/common/BaseEntity.java`:

```java
package com.example.backtemplate.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

- [ ] **Step 4: Write the `Note` entity**

`src/main/java/com/example/backtemplate/notes/Note.java`:

```java
package com.example.backtemplate.notes;

import com.example.backtemplate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
}
```

`@Builder` + `@AllArgsConstructor` need `BaseEntity`'s fields settable too for tests to construct fixtures easily; since `id`/`createdAt`/`updatedAt` are DB-generated, tests build `Note` via the no-args constructor + setters instead of the builder for those fields — the builder here only ever needs `title`/`content`/`ownerId` in practice, which is what Task 6/7 use.

- [ ] **Step 5: Write `NoteRepository`**

`src/main/java/com/example/backtemplate/notes/NoteRepository.java`:

```java
package com.example.backtemplate.notes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findAllByOwnerId(UUID ownerId);

    Optional<Note> findByIdAndOwnerId(UUID id, UUID ownerId);
}
```

- [ ] **Step 6: Write the failing test**

`src/test/java/com/example/backtemplate/notes/NoteRepositoryIntegrationTest.java`:

```java
package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NoteRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private NoteRepository noteRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndFindsNotesByOwner() {
        UUID ownerId = insertUser("owner@example.com");

        Note note = new Note();
        note.setTitle("First note");
        note.setContent("Body");
        note.setOwnerId(ownerId);
        noteRepository.save(note);

        var found = noteRepository.findAllByOwnerId(ownerId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("First note");
    }

    private UUID insertUser(String email) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (email) VALUES (?) RETURNING id", UUID.class, email);
    }
}
```

- [ ] **Step 7: Run it to verify it fails (no table yet in the running app JAR cache)**

Run: `./mvnw -q test -Dtest=NoteRepositoryIntegrationTest`
Expected: FAILS or is unrunnable before this task's files exist — since Steps 1-6 already create every needed file, run this only as the real verification pass (see Step 8), not as a pre-implementation check. Skip a separate "fails first" run here: this task creates schema, entity, and repository together, so there is no meaningful intermediate red state.

- [ ] **Step 8: Run it to verify it passes**

Run: `./mvnw -q test -Dtest=NoteRepositoryIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. (Requires Docker running for Testcontainers.)

- [ ] **Step 9: Commit**

```bash
git add src/main/resources/db/migration/V2__create_notes_table.sql src/main/java/com/example/backtemplate/common/BaseEntity.java src/main/java/com/example/backtemplate/notes src/test/java/com/example/backtemplate/AbstractIntegrationTest.java src/test/java/com/example/backtemplate/notes
git commit -m "feat: add notes table, Note entity, and NoteRepository"
```

---

## Task 5: Global error shape

**Files:**
- Create: `src/main/java/com/example/backtemplate/common/ApiException.java`
- Create: `src/main/java/com/example/backtemplate/common/ErrorResponse.java`
- Create: `src/main/java/com/example/backtemplate/common/GlobalExceptionHandler.java`
- Test: `src/test/java/com/example/backtemplate/common/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `ApiException(String code, HttpStatus status, String message)` — every later task throws this (or a bean-validation error) instead of ad-hoc exceptions. `ErrorResponse` JSON shape: `{"error":{"code","message","details"}}`.

- [ ] **Step 1: Write `ErrorResponse`**

`src/main/java/com/example/backtemplate/common/ErrorResponse.java`:

```java
package com.example.backtemplate.common;

import java.util.List;

public record ErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, List<String> details) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, List.of()));
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }
}
```

- [ ] **Step 2: Write `ApiException`**

`src/main/java/com/example/backtemplate/common/ApiException.java`:

```java
package com.example.backtemplate.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException("CONFLICT", HttpStatus.CONFLICT, message);
    }

    public static ApiException tooManyRequests(String message) {
        return new ApiException("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
```

- [ ] **Step 3: Write `GlobalExceptionHandler`**

`src/main/java/com/example/backtemplate/common/GlobalExceptionHandler.java`:

```java
package com.example.backtemplate.common;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "Invalid request body", details));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", "Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Unexpected error"));
    }
}
```

- [ ] **Step 4: Write the failing test**

`src/test/java/com/example/backtemplate/common/GlobalExceptionHandlerTest.java`:

```java
package com.example.backtemplate.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApiExceptionToErrorShape() {
        ApiException ex = ApiException.notFound("Note not found");

        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("Note not found");
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: FAIL — compile error, `GlobalExceptionHandler`/`ApiException`/`ErrorResponse` don't exist yet (write this step's files in order: test last, so in practice run this right after Step 1 stub, or treat Steps 1-3 as already having created the classes and use this run purely as the pass-check in Step 6).

- [ ] **Step 6: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=GlobalExceptionHandlerTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/backtemplate/common src/test/java/com/example/backtemplate/common
git commit -m "feat: add global error shape and exception handler"
```

---

## Task 6: Notes DTOs + NoteService

**Files:**
- Create: `src/main/java/com/example/backtemplate/notes/dto/NoteRequest.java`
- Create: `src/main/java/com/example/backtemplate/notes/dto/NoteResponse.java`
- Create: `src/main/java/com/example/backtemplate/notes/NoteService.java`
- Test: `src/test/java/com/example/backtemplate/notes/NoteServiceTest.java`

**Interfaces:**
- Consumes: `NoteRepository` (Task 4), `ApiException.notFound` (Task 5).
- Produces: `NoteService` with `create(UUID ownerId, NoteRequest req): NoteResponse`, `list(UUID ownerId): List<NoteResponse>`, `get(UUID ownerId, UUID id): NoteResponse`, `update(UUID ownerId, UUID id, NoteRequest req): NoteResponse`, `delete(UUID ownerId, UUID id): void` — Task 7's `NoteController` calls these five methods by name.

- [ ] **Step 1: Write the DTOs**

`src/main/java/com/example/backtemplate/notes/dto/NoteRequest.java`:

```java
package com.example.backtemplate.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank @Size(max = 255) String title, @NotBlank String content) {}
```

`src/main/java/com/example/backtemplate/notes/dto/NoteResponse.java`:

```java
package com.example.backtemplate.notes.dto;

import com.example.backtemplate.notes.Note;
import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id, String title, String content, Instant createdAt, Instant updatedAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}
```

- [ ] **Step 2: Write the failing test**

`src/test/java/com/example/backtemplate/notes/NoteServiceTest.java`:

```java
package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.notes.dto.NoteRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock private NoteRepository noteRepository;

    private NoteService noteService;

    @Test
    void getThrowsNotFoundWhenNoteMissingOrNotOwned() {
        noteService = new NoteService(noteRepository);
        UUID ownerId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findByIdAndOwnerId(noteId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.get(ownerId, noteId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Note not found");
    }

    @Test
    void createSavesNoteWithOwnerId() {
        noteService = new NoteService(noteRepository);
        UUID ownerId = UUID.randomUUID();
        NoteRequest req = new NoteRequest("Title", "Content");
        when(noteRepository.save(org.mockito.ArgumentMatchers.any(Note.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var response = noteService.create(ownerId, req);

        assertThat(response.title()).isEqualTo("Title");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=NoteServiceTest`
Expected: FAIL — `NoteService` does not exist.

- [ ] **Step 4: Write `NoteService`**

`src/main/java/com/example/backtemplate/notes/NoteService.java`:

```java
package com.example.backtemplate.notes;

import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteResponse create(UUID ownerId, NoteRequest req) {
        Note note = new Note();
        note.setTitle(req.title());
        note.setContent(req.content());
        note.setOwnerId(ownerId);
        return NoteResponse.from(noteRepository.save(note));
    }

    public List<NoteResponse> list(UUID ownerId) {
        return noteRepository.findAllByOwnerId(ownerId).stream().map(NoteResponse::from).toList();
    }

    public NoteResponse get(UUID ownerId, UUID id) {
        return NoteResponse.from(findOwned(ownerId, id));
    }

    public NoteResponse update(UUID ownerId, UUID id, NoteRequest req) {
        Note note = findOwned(ownerId, id);
        note.setTitle(req.title());
        note.setContent(req.content());
        return NoteResponse.from(noteRepository.save(note));
    }

    public void delete(UUID ownerId, UUID id) {
        noteRepository.delete(findOwned(ownerId, id));
    }

    private Note findOwned(UUID ownerId, UUID id) {
        return noteRepository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ApiException.notFound("Note not found"));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=NoteServiceTest`
Expected: `BUILD SUCCESS`, 2 tests, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/backtemplate/notes src/test/java/com/example/backtemplate/notes/NoteServiceTest.java
git commit -m "feat: add NoteService with owner-scoped CRUD"
```

---

## Task 7: NoteController + integration test

**Files:**
- Create: `src/main/java/com/example/backtemplate/notes/NoteController.java`
- Test: `src/test/java/com/example/backtemplate/notes/NoteControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `NoteService` (Task 6). Uses a hardcoded owner id for now (`X-Owner-Id` header) — Task 14 replaces this with the authenticated principal once JWT auth exists; this task must not block on auth being finished.
- Produces: `POST/GET/PUT/DELETE /api/notes[/{id}]` — this exact path prefix (`/api/notes`) is what Task 17's rate-limit filter and the README's endpoint list reference.

- [ ] **Step 1: Write the controller**

`src/main/java/com/example/backtemplate/notes/NoteController.java`:

```java
package com.example.backtemplate.notes;

import com.example.backtemplate.notes.dto.NoteRequest;
import com.example.backtemplate.notes.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    // TEMPORARY: X-Owner-Id header stands in for the authenticated principal
    // until Task 14 wires JwtAuthFilter + SecurityContext. Every method below
    // switches to @AuthenticationPrincipal in that task.

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@RequestHeader("X-Owner-Id") UUID ownerId, @Valid @RequestBody NoteRequest req) {
        return noteService.create(ownerId, req);
    }

    @GetMapping
    public List<NoteResponse> list(@RequestHeader("X-Owner-Id") UUID ownerId) {
        return noteService.list(ownerId);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@RequestHeader("X-Owner-Id") UUID ownerId, @PathVariable UUID id) {
        return noteService.get(ownerId, id);
    }

    @PutMapping("/{id}")
    public NoteResponse update(
            @RequestHeader("X-Owner-Id") UUID ownerId,
            @PathVariable UUID id,
            @Valid @RequestBody NoteRequest req) {
        return noteService.update(ownerId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-Owner-Id") UUID ownerId, @PathVariable UUID id) {
        noteService.delete(ownerId, id);
    }
}
```

- [ ] **Step 2: Write the failing integration test**

`src/test/java/com/example/backtemplate/notes/NoteControllerIntegrationTest.java`:

```java
package com.example.backtemplate.notes;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class NoteControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void fullCrudRoundTrip() {
        UUID ownerId =
                jdbcTemplate.queryForObject(
                        "INSERT INTO users (email) VALUES (?) RETURNING id",
                        UUID.class,
                        "crud@example.com");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Owner-Id", ownerId.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // create
        var createBody = java.util.Map.of("title", "Hello", "content", "World");
        var createResp =
                restTemplate.exchange(
                        "/api/notes",
                        HttpMethod.POST,
                        new HttpEntity<>(createBody, headers),
                        java.util.Map.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String noteId = (String) createResp.getBody().get("id");

        // list
        var listResp =
                restTemplate.exchange(
                        "/api/notes", HttpMethod.GET, new HttpEntity<>(headers), java.util.List.class);
        assertThat(listResp.getBody()).hasSize(1);

        // get
        var getResp =
                restTemplate.exchange(
                        "/api/notes/" + noteId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        java.util.Map.class);
        assertThat(getResp.getBody().get("title")).isEqualTo("Hello");

        // update
        var updateBody = java.util.Map.of("title", "Updated", "content", "World");
        restTemplate.exchange(
                "/api/notes/" + noteId,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, headers),
                java.util.Map.class);

        // delete
        var deleteResp =
                restTemplate.exchange(
                        "/api/notes/" + noteId, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=NoteControllerIntegrationTest`
Expected: FAIL — `NoteController` doesn't exist yet (before Step 1 is applied); once Step 1's file is written, re-run to reach the pass-check in Step 4.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=NoteControllerIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures. Requires Docker running.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/backtemplate/notes/NoteController.java src/test/java/com/example/backtemplate/notes/NoteControllerIntegrationTest.java
git commit -m "feat: add NoteController with full CRUD, integration-tested against real Postgres"
```

---

## Task 8: Actuator health endpoint

**Files:**
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/backtemplate/HealthCheckIntegrationTest.java`

**Interfaces:**
- Produces: `GET /actuator/health` returning `{"status":"UP"}` — this exact path is what Task 19's Dockerfile `HEALTHCHECK` and `docker-compose.yml`'s future `app` healthcheck (Task 19) call.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/backtemplate/HealthCheckIntegrationTest.java`:

```java
package com.example.backtemplate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

class HealthCheckIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void healthEndpointReturnsUp() {
        var response = restTemplate.getForEntity("/actuator/health", java.util.Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
```

- [ ] **Step 2: Run test to verify current state**

Run: `./mvnw -q test -Dtest=HealthCheckIntegrationTest`
Expected: FAILS with 401/403 — Actuator is on the classpath (Task 1) but Spring Security (also on the classpath since Task 1) blocks it by default until explicitly permitted, and endpoint exposure isn't configured yet.

- [ ] **Step 3: Expose only the health endpoint**

Add to `src/main/resources/application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

- [ ] **Step 4: Permit the endpoint without auth**

Security isn't configured yet (Task 14 adds `SecurityConfig`) — Spring Security's default auto-configuration currently requires auth on everything. Add a minimal interim security config so the health check test can pass now instead of waiting on Task 14:

`src/main/java/com/example/backtemplate/config/SecurityConfig.java`:

```java
package com.example.backtemplate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health").permitAll()
                                        .anyRequest().permitAll()); // widened again in Task 14 once JWT auth exists
        return http.build();
    }
}
```

`.anyRequest().permitAll()` is intentionally permissive for now — Task 14 replaces this rule with `.anyRequest().authenticated()` plus explicit `permitAll()` for `/auth/**` and `/actuator/health`, once there's an auth mechanism to actually enforce.

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=HealthCheckIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.yml src/main/java/com/example/backtemplate/config/SecurityConfig.java src/test/java/com/example/backtemplate/HealthCheckIntegrationTest.java
git commit -m "feat: expose actuator health endpoint with interim permissive security config"
```

---

## Task 9: Logging setup

**Files:**
- Create: `src/main/resources/logback-spring.xml`
- Modify: `src/main/resources/application.yml`, `application-dev.yml`, `application-prod.yml` (create prod)

**Interfaces:**
- Produces: `LOG_LEVEL` env var controls root logger level; `dev`/`test` profiles use a human-readable console pattern, `prod` profile emits JSON via `logstash-logback-encoder`.

- [ ] **Step 1: Write `logback-spring.xml`**

`src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="LOG_LEVEL" source="logging.level.root" defaultValue="INFO"/>

    <springProfile name="dev,test">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="${LOG_LEVEL}">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        </appender>
        <root level="${LOG_LEVEL}">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
</configuration>
```

- [ ] **Step 2: Wire `LOG_LEVEL` into `application.yml`**

Add to `src/main/resources/application.yml`:

```yaml
logging:
  level:
    root: ${LOG_LEVEL:info}
```

- [ ] **Step 3: Create `application-prod.yml`**

`src/main/resources/application-prod.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
```

- [ ] **Step 4: Verify dev profile logs human-readable output**

Run: `SPRING_PROFILES_ACTIVE=dev LOG_LEVEL=debug ./mvnw -q spring-boot:run &` then check console output, then stop it:
```bash
sleep 8 && curl -s 127.0.0.1:8080/actuator/health && kill %1
```
Expected: console shows `HH:mm:ss.SSS`-prefixed plain-text lines (not JSON), and `curl` returns `{"status":"UP"}`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/logback-spring.xml src/main/resources/application.yml src/main/resources/application-prod.yml
git commit -m "feat: add profile-based logging (plain text dev, JSON prod)"
```

---

## Task 10: Env validation fail-fast

**Files:**
- Create: `src/main/java/com/example/backtemplate/config/AppProperties.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/backtemplate/config/AppPropertiesValidationTest.java`

**Interfaces:**
- Produces: `AppProperties` bean with validated `jwt.secret` (min length 32) — Task 12's `JwtService` injects this bean instead of reading `@Value` directly.

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/backtemplate/config/AppPropertiesValidationTest.java`:

```java
package com.example.backtemplate.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AppPropertiesValidationTest {

    @Test
    void contextFailsToStartWithoutJwtSecret() {
        var context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of("app.jwt.secret=").applyTo(context);
        context.register(AppProperties.class);

        assertThatThrownBy(context::refresh)
                .hasStackTraceContaining("jwtSecret"); // Spring wraps the NotBlank message deep in
                // the cause chain; AssertJ's hasMessageContaining only checks the top-level
                // exception, so assert against the full stack trace instead.

        context.close();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=AppPropertiesValidationTest`
Expected: FAIL — `AppProperties` class doesn't exist.

- [ ] **Step 3: Write `AppProperties`**

`src/main/java/com/example/backtemplate/config/AppProperties.java`:

```java
package com.example.backtemplate.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @NotBlank
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters")
    private String jwtSecret;

    private int jwtAccessTtlMinutes = 15;
    private int jwtRefreshTtlDays = 30;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getJwtAccessTtlMinutes() {
        return jwtAccessTtlMinutes;
    }

    public void setJwtAccessTtlMinutes(int jwtAccessTtlMinutes) {
        this.jwtAccessTtlMinutes = jwtAccessTtlMinutes;
    }

    public int getJwtRefreshTtlDays() {
        return jwtRefreshTtlDays;
    }

    public void setJwtRefreshTtlDays(int jwtRefreshTtlDays) {
        this.jwtRefreshTtlDays = jwtRefreshTtlDays;
    }
}
```

Plain getters/setters (not Lombok) here deliberately — `@ConfigurationProperties` binding via Lombok's `@Setter` works too, but the explicit form is what most Spring docs/examples show, reducing surprise for anyone unfamiliar with Lombok reading this specific class.

- [ ] **Step 4: Wire the property into `application.yml`**

Add to `src/main/resources/application.yml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:}
    access-ttl-minutes: ${JWT_ACCESS_TTL_MINUTES:15}
    refresh-ttl-days: ${JWT_REFRESH_TTL_DAYS:30}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=AppPropertiesValidationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 6: Verify the full app still boots with a real secret**

Run: `JWT_SECRET=$(openssl rand -hex 32) SPRING_PROFILES_ACTIVE=dev ./mvnw -q spring-boot:run &`
```bash
sleep 8 && curl -s 127.0.0.1:8080/actuator/health && kill %1
```
Expected: `{"status":"UP"}`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/backtemplate/config/AppProperties.java src/main/resources/application.yml src/test/java/com/example/backtemplate/config
git commit -m "feat: add fail-fast validated app properties (JWT secret required)"
```

---

## Task 11: User entity + UserRepository + password hashing

**Files:**
- Create: `src/main/resources/db/migration/V3__add_user_auth_columns.sql`
- Create: `src/main/java/com/example/backtemplate/auth/User.java`
- Create: `src/main/java/com/example/backtemplate/auth/UserRepository.java`
- Create: `src/main/java/com/example/backtemplate/auth/PasswordService.java`
- Test: `src/test/java/com/example/backtemplate/auth/PasswordServiceTest.java`

**Interfaces:**
- Produces: `User` entity (`email, passwordHash, emailVerified: boolean, verificationToken, verificationTokenExpiresAt, resetToken, resetTokenExpiresAt`), `UserRepository extends JpaRepository<User, UUID>` with `Optional<User> findByEmail(String email)`, `PasswordService.hash(String raw): String` / `matches(String raw, String hash): boolean` (BCrypt) — Task 12-16 depend on all of these exact names.

- [ ] **Step 1: Migration**

`src/main/resources/db/migration/V3__add_user_auth_columns.sql`:

```sql
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN verification_token VARCHAR(255),
    ADD COLUMN verification_token_expires_at TIMESTAMPTZ,
    ADD COLUMN reset_token VARCHAR(255),
    ADD COLUMN reset_token_expires_at TIMESTAMPTZ;

ALTER TABLE users ALTER COLUMN password_hash DROP DEFAULT;
```

- [ ] **Step 2: `PasswordService` with failing test first**

`src/test/java/com/example/backtemplate/auth/PasswordServiceTest.java`:

```java
package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = passwordService.hash("s3cret-passw0rd");

        assertThat(hash).isNotEqualTo("s3cret-passw0rd");
        assertThat(passwordService.matches("s3cret-passw0rd", hash)).isTrue();
        assertThat(passwordService.matches("wrong", hash)).isFalse();
    }
}
```

Run: `./mvnw -q test -Dtest=PasswordServiceTest` — expect FAIL (class missing).

`src/main/java/com/example/backtemplate/auth/PasswordService.java`:

```java
package com.example.backtemplate.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
```

Run again: expect `BUILD SUCCESS`.

- [ ] **Step 3: `User` entity**

`src/main/java/com/example/backtemplate/auth/User.java`:

```java
package com.example.backtemplate.auth;

import com.example.backtemplate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;
}
```

- [ ] **Step 4: `UserRepository`**

`src/main/java/com/example/backtemplate/auth/UserRepository.java`:

```java
package com.example.backtemplate.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String token);
}
```

- [ ] **Step 5: Run full test suite, then commit**

Run: `./mvnw -q test`
Expected: `BUILD SUCCESS`, all prior tests plus `PasswordServiceTest` pass.

```bash
git add src/main/resources/db/migration/V3__add_user_auth_columns.sql src/main/java/com/example/backtemplate/auth src/test/java/com/example/backtemplate/auth
git commit -m "feat: add User entity, UserRepository, and BCrypt PasswordService"
```

---

## Task 12: JWT service

**Files:**
- Create: `src/main/java/com/example/backtemplate/auth/JwtService.java`
- Test: `src/test/java/com/example/backtemplate/auth/JwtServiceTest.java`

**Interfaces:**
- Consumes: `AppProperties` (Task 10).
- Produces: `JwtService.generateAccessToken(UUID userId, String email): String`, `generateRefreshToken(UUID userId): String`, `parse(String token): Jws<Claims>` (throws `io.jsonwebtoken.JwtException` on invalid/expired) — Task 14's `JwtAuthFilter` and `AuthService` call these by name.

- [ ] **Step 1: Write the failing test**

```java
package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backtemplate.config.AppProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final AppProperties props = new AppProperties();

    {
        props.setJwtSecret("a".repeat(32));
        props.setJwtAccessTtlMinutes(15);
        props.setJwtRefreshTtlDays(30);
    }

    private final JwtService jwtService = new JwtService(props);

    @Test
    void generatesAndParsesAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");

        var claims = jwtService.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@example.com");
        // Flip a character in the middle, not the last character of the whole token: base64url's
        // final character can carry unused padding bits, so swapping it sometimes decodes to the
        // exact same bytes and the "tampered" token verifies anyway (this bit us in practice).
        int mid = token.length() / 2;
        char flipped = token.charAt(mid) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, mid) + flipped + token.substring(mid + 1);

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=JwtServiceTest`
Expected: FAIL — `JwtService` doesn't exist.

- [ ] **Step 3: Write `JwtService`**

`src/main/java/com/example/backtemplate/auth/JwtService.java`:

```java
package com.example.backtemplate.auth;

import com.example.backtemplate.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwtSecret().getBytes());
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(appProperties.getJwtAccessTtlMinutes()))))
                .signWith(signingKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofDays(appProperties.getJwtRefreshTtlDays()))))
                .signWith(signingKey())
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey()).build().parseSignedClaims(token);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=JwtServiceTest`
Expected: `BUILD SUCCESS`, 2 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/backtemplate/auth/JwtService.java src/test/java/com/example/backtemplate/auth/JwtServiceTest.java
git commit -m "feat: add JwtService for access/refresh token issuing and parsing"
```

---

## Task 13: Auth register + email verification

**Files:**
- Create: `src/main/java/com/example/backtemplate/email/EmailService.java`
- Create: `src/main/java/com/example/backtemplate/email/ConsoleEmailService.java`
- Create: `src/main/java/com/example/backtemplate/email/SmtpEmailService.java`
- Create: `src/main/java/com/example/backtemplate/auth/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/backtemplate/auth/AuthService.java` (register + verify only in this task; login/refresh added Task 14, forgot/reset added Task 15)
- Create: `src/main/java/com/example/backtemplate/auth/AuthController.java`
- Test: `src/test/java/com/example/backtemplate/auth/AuthControllerRegisterIntegrationTest.java`

**Interfaces:**
- Produces: `EmailService.send(String to, String subject, String body): void` (interface — `ConsoleEmailService` active when `app.mail.host` is blank, `SmtpEmailService` otherwise, selected via `@ConditionalOnProperty`). `POST /auth/register`, `GET /auth/verify-email?token=`.

- [ ] **Step 1: `EmailService` interface + two implementations**

`src/main/java/com/example/backtemplate/email/EmailService.java`:

```java
package com.example.backtemplate.email;

public interface EmailService {
    void send(String to, String subject, String body);
}
```

`src/main/java/com/example/backtemplate/email/ConsoleEmailService.java`:

`ConditionalOnProperty` cannot cleanly express "empty vs non-empty" — without an explicit `havingValue`, Spring treats a property bound to an empty string as still "present" (not missing), so `@ConditionalOnProperty(name = "host")` on `SmtpEmailService` matches even when `app.mail.host` resolves to `""` via `${MAIL_HOST:}`. Both beans end up active simultaneously and `AuthService`'s single-`EmailService` constructor injection fails at boot with "expected single matching bean but found 2". Use two small `Condition` classes instead, keyed on `StringUtils.hasText`:

`src/main/java/com/example/backtemplate/email/MailHostUnsetCondition.java`:

```java
package com.example.backtemplate.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class MailHostUnsetCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String host = context.getEnvironment().getProperty("app.mail.host");
        return !StringUtils.hasText(host);
    }
}
```

`src/main/java/com/example/backtemplate/email/MailHostSetCondition.java`:

```java
package com.example.backtemplate.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class MailHostSetCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String host = context.getEnvironment().getProperty("app.mail.host");
        return StringUtils.hasText(host);
    }
}
```

`src/main/java/com/example/backtemplate/email/ConsoleEmailService.java`:

```java
package com.example.backtemplate.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Conditional(MailHostUnsetCondition.class)
public class ConsoleEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[dev email fallback] to={} subject={}\n{}", to, subject, body);
    }
}
```

`src/main/java/com/example/backtemplate/email/SmtpEmailService.java`:

```java
package com.example.backtemplate.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Conditional(MailHostSetCondition.class)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
```

Add to `application.yml` — merge `mail:` into the *existing* `spring:` block (YAML has no duplicate-key merging; a second top-level `spring:` block silently wipes out `spring.application`/`spring.profiles` set in Task 1):
```yaml
spring:
  # ...existing application/profiles keys stay here...
  mail:
    host: ${MAIL_HOST:}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

app:
  mail:
    host: ${MAIL_HOST:}
    from: ${MAIL_FROM:no-reply@example.com}
```

`ConditionalOnProperty(havingValue = "", matchIfMissing = true)` on `ConsoleEmailService` and `ConditionalOnProperty(name = "host")` (any non-blank value) on `SmtpEmailService` are mutually exclusive given the same `app.mail.host` property — exactly one bean of type `EmailService` exists at a time, so `AuthService`'s `@RequiredArgsConstructor` injection never sees an ambiguity error.

- [ ] **Step 2: DTOs and `AuthService`**

`src/main/java/com/example/backtemplate/auth/dto/RegisterRequest.java`:

```java
package com.example.backtemplate.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email, @NotBlank @Size(min = 8) String password) {}
```

`src/main/java/com/example/backtemplate/auth/AuthService.java`:

```java
package com.example.backtemplate.auth;

import com.example.backtemplate.auth.dto.RegisterRequest;
import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.email.EmailService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final EmailService emailService;

    public void register(RegisterRequest req) {
        userRepository
                .findByEmail(req.email())
                .ifPresent(
                        u -> {
                            throw ApiException.conflict("Email already registered");
                        });

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordService.hash(req.password()));
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiresAt(Instant.now().plusSeconds(24 * 3600));
        userRepository.save(user);

        emailService.send(
                user.getEmail(),
                "Verify your email",
                "Verification token: " + user.getVerificationToken());
    }

    public void verifyEmail(String token) {
        User user =
                userRepository
                        .findByVerificationToken(token)
                        .orElseThrow(() -> ApiException.notFound("Invalid verification token"));

        if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("Verification token expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }
}
```

- [ ] **Step 3: `AuthController`**

`src/main/java/com/example/backtemplate/auth/AuthController.java`:

```java
package com.example.backtemplate.auth;

import com.example.backtemplate.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
    }
}
```

Update `SecurityConfig` (Task 8) to permit `/auth/**` explicitly instead of relying on the temporary `anyRequest().permitAll()` — no change needed yet since that blanket rule already covers it; Task 14 tightens this.

- [ ] **Step 4: Write and run the integration test**

`src/test/java/com/example/backtemplate/auth/AuthControllerRegisterIntegrationTest.java`:

```java
package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerRegisterIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void registerCreatesUnverifiedUserAndVerifyActivatesIt() {
        var body = Map.of("email", "new@example.com", "password", "s3cret-pw");

        var registerResp = restTemplate.postForEntity("/auth/register", body, Void.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Boolean verified =
                jdbcTemplate.queryForObject(
                        "SELECT email_verified FROM users WHERE email = ?", Boolean.class, "new@example.com");
        assertThat(verified).isFalse();

        String token =
                jdbcTemplate.queryForObject(
                        "SELECT verification_token FROM users WHERE email = ?",
                        String.class,
                        "new@example.com");

        restTemplate.getForEntity("/auth/verify-email?token=" + token, Void.class);

        verified =
                jdbcTemplate.queryForObject(
                        "SELECT email_verified FROM users WHERE email = ?", Boolean.class, "new@example.com");
        assertThat(verified).isTrue();
    }
}
```

Run: `./mvnw -q test -Dtest=AuthControllerRegisterIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/backtemplate/email src/main/java/com/example/backtemplate/auth src/main/resources/application.yml src/test/java/com/example/backtemplate/auth/AuthControllerRegisterIntegrationTest.java
git commit -m "feat: add registration and email verification flow with console/SMTP email fallback"
```

---

## Task 14: Auth login + refresh token endpoint

**Files:**
- Create: `src/main/java/com/example/backtemplate/config/JwtAuthFilter.java`
- Modify: `src/main/java/com/example/backtemplate/config/SecurityConfig.java`
- Modify: `src/main/java/com/example/backtemplate/auth/AuthService.java`, `AuthController.java`
- Create: `src/main/java/com/example/backtemplate/auth/dto/LoginRequest.java`, `TokenResponse.java`, `RefreshRequest.java`
- Modify: `src/main/java/com/example/backtemplate/notes/NoteController.java` (switch from `X-Owner-Id` header to `@AuthenticationPrincipal`)
- Test: `src/test/java/com/example/backtemplate/auth/AuthControllerLoginIntegrationTest.java`

**Interfaces:**
- Consumes: `JwtService` (Task 12), `PasswordService`/`UserRepository` (Task 11).
- Produces: `POST /auth/login` → `TokenResponse(accessToken, refreshToken)`; `POST /auth/refresh` → new `TokenResponse`; authenticated requests carry `Authorization: Bearer <token>`, and `SecurityContextHolder` exposes the user's UUID as the principal name for `@AuthenticationPrincipal String` (or via `Authentication.getName()`) in every controller from here on — Task 16's `AccountController` and Task 7's `NoteController` both switch to this.

- [ ] **Step 1: DTOs**

```java
// src/main/java/com/example/backtemplate/auth/dto/LoginRequest.java
package com.example.backtemplate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
```

```java
// src/main/java/com/example/backtemplate/auth/dto/TokenResponse.java
package com.example.backtemplate.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {}
```

```java
// src/main/java/com/example/backtemplate/auth/dto/RefreshRequest.java
package com.example.backtemplate.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

- [ ] **Step 2: Extend `AuthService`**

Add to `src/main/java/com/example/backtemplate/auth/AuthService.java` (inject `JwtService` via the existing `@RequiredArgsConstructor` field list):

```java
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest req) {
        User user =
                userRepository
                        .findByEmail(req.email())
                        .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));

        if (!passwordService.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw ApiException.unauthorized("Email not verified");
        }

        return new TokenResponse(
                jwtService.generateAccessToken(user.getId(), user.getEmail()),
                jwtService.generateRefreshToken(user.getId()));
    }

    public TokenResponse refresh(RefreshRequest req) {
        var claims = jwtService.parse(req.refreshToken()).getPayload();
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw ApiException.unauthorized("Invalid refresh token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user =
                userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        return new TokenResponse(
                jwtService.generateAccessToken(user.getId(), user.getEmail()),
                jwtService.generateRefreshToken(user.getId()));
    }
```

Add the matching imports (`TokenResponse`, `LoginRequest`, `RefreshRequest`) at the top of the file.

- [ ] **Step 3: `JwtAuthFilter`**

`src/main/java/com/example/backtemplate/config/JwtAuthFilter.java`:

```java
package com.example.backtemplate.config;

import com.example.backtemplate.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwtService.parse(header.substring(7)).getPayload();
                if ("access".equals(claims.get("type", String.class))) {
                    var auth =
                            new UsernamePasswordAuthenticationToken(
                                    claims.getSubject(), null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (io.jsonwebtoken.JwtException ignored) {
                // leave SecurityContext empty — request falls through to unauthenticated handling
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: Tighten `SecurityConfig`**

Replace `src/main/java/com/example/backtemplate/config/SecurityConfig.java` entirely:

```java
package com.example.backtemplate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health", "/auth/**").permitAll()
                                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 5: Update `AuthController`**

Add to `src/main/java/com/example/backtemplate/auth/AuthController.java`:

```java
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }
```

Add imports for `TokenResponse`, `LoginRequest`, `RefreshRequest`.

- [ ] **Step 6: Switch `NoteController` to the authenticated principal**

In `src/main/java/com/example/backtemplate/notes/NoteController.java`, replace every `@RequestHeader("X-Owner-Id") UUID ownerId` parameter with:

```java
@AuthenticationPrincipal String ownerIdStr
```

and add `UUID ownerId = UUID.fromString(ownerIdStr);` as the first line of each method body. Add the import `org.springframework.security.core.annotation.AuthenticationPrincipal`. Update `NoteControllerIntegrationTest` (Task 7) to obtain a real access token via `/auth/register` + a direct `email_verified = true` SQL update + `/auth/login`, then send `Authorization: Bearer <token>` instead of `X-Owner-Id`.

- [ ] **Step 7: Write and run the login/refresh integration test**

`src/test/java/com/example/backtemplate/auth/AuthControllerLoginIntegrationTest.java`:

```java
package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerLoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void loginReturnsTokensForVerifiedUser() {
        restTemplate.postForEntity(
                "/auth/register", Map.of("email", "login@example.com", "password", "s3cret-pw"), Void.class);
        jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", "login@example.com");

        var resp =
                restTemplate.postForEntity(
                        "/auth/login",
                        Map.of("email", "login@example.com", "password", "s3cret-pw"),
                        Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("accessToken")).isNotNull();
        assertThat(resp.getBody().get("refreshToken")).isNotNull();
    }

    @Test
    void loginRejectsUnverifiedUser() {
        restTemplate.postForEntity(
                "/auth/register", Map.of("email", "unverified@example.com", "password", "s3cret-pw"), Void.class);

        var resp =
                restTemplate.postForEntity(
                        "/auth/login",
                        Map.of("email", "unverified@example.com", "password", "s3cret-pw"),
                        Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

Run: `./mvnw -q test`
Expected: `BUILD SUCCESS`, full suite passes including the updated `NoteControllerIntegrationTest`.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/example/backtemplate/config src/main/java/com/example/backtemplate/auth src/main/java/com/example/backtemplate/notes/NoteController.java src/test/java/com/example/backtemplate/auth/AuthControllerLoginIntegrationTest.java src/test/java/com/example/backtemplate/notes/NoteControllerIntegrationTest.java
git commit -m "feat: add login/refresh, JWT auth filter, and switch notes to authenticated principal"
```

---

## Task 15: Forgot/reset password flow

**Files:**
- Modify: `src/main/java/com/example/backtemplate/auth/AuthService.java`, `AuthController.java`
- Create: `src/main/java/com/example/backtemplate/auth/dto/ForgotPasswordRequest.java`, `ResetPasswordRequest.java`
- Test: `src/test/java/com/example/backtemplate/auth/AuthControllerResetPasswordIntegrationTest.java`

**Interfaces:**
- Produces: `POST /auth/forgot-password` (always 200, no enumeration leak), `POST /auth/reset-password`.

- [ ] **Step 1: DTOs**

```java
// ForgotPasswordRequest.java
package com.example.backtemplate.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email String email) {}
```

```java
// ResetPasswordRequest.java
package com.example.backtemplate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}
```

- [ ] **Step 2: Extend `AuthService`**

```java
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository
                .findByEmail(req.email())
                .ifPresent(
                        user -> {
                            user.setResetToken(UUID.randomUUID().toString());
                            user.setResetTokenExpiresAt(Instant.now().plusSeconds(3600));
                            userRepository.save(user);
                            emailService.send(
                                    user.getEmail(),
                                    "Reset your password",
                                    "Reset token: " + user.getResetToken());
                        });
        // always returns normally, whether or not the email exists — avoids user enumeration
    }

    public void resetPassword(ResetPasswordRequest req) {
        User user =
                userRepository
                        .findByResetToken(req.token())
                        .orElseThrow(() -> ApiException.notFound("Invalid reset token"));

        if (user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("Reset token expired");
        }

        user.setPasswordHash(passwordService.hash(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }
```

Add imports for `ForgotPasswordRequest`, `ResetPasswordRequest`.

- [ ] **Step 3: Extend `AuthController`**

```java
    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
    }
```

- [ ] **Step 4: Write and run the test**

`src/test/java/com/example/backtemplate/auth/AuthControllerResetPasswordIntegrationTest.java`:

```java
package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthControllerResetPasswordIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void forgotThenResetPasswordAllowsLoginWithNewPassword() {
        restTemplate.postForEntity(
                "/auth/register", Map.of("email", "reset@example.com", "password", "old-pw-123"), Void.class);
        jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", "reset@example.com");

        var forgotResp =
                restTemplate.postForEntity(
                        "/auth/forgot-password", Map.of("email", "reset@example.com"), Void.class);
        assertThat(forgotResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        String resetToken =
                jdbcTemplate.queryForObject(
                        "SELECT reset_token FROM users WHERE email = ?", String.class, "reset@example.com");

        restTemplate.postForEntity(
                "/auth/reset-password",
                Map.of("token", resetToken, "newPassword", "new-pw-456"),
                Void.class);

        var loginResp =
                restTemplate.postForEntity(
                        "/auth/login",
                        Map.of("email", "reset@example.com", "password", "new-pw-456"),
                        Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPasswordReturns200EvenForUnknownEmail() {
        var resp =
                restTemplate.postForEntity(
                        "/auth/forgot-password", Map.of("email", "unknown@example.com"), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

Run: `./mvnw -q test -Dtest=AuthControllerResetPasswordIntegrationTest`
Expected: `BUILD SUCCESS`, 2 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/backtemplate/auth src/test/java/com/example/backtemplate/auth/AuthControllerResetPasswordIntegrationTest.java
git commit -m "feat: add forgot/reset password flow"
```

---

## Task 16: Account endpoints

**Files:**
- Create: `src/main/java/com/example/backtemplate/account/AccountController.java`, `AccountService.java`
- Create: `src/main/java/com/example/backtemplate/account/dto/ChangePasswordRequest.java`
- Test: `src/test/java/com/example/backtemplate/account/AccountControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `UserRepository`, `PasswordService` (Task 11), `@AuthenticationPrincipal` pattern (Task 14).
- Produces: `PATCH /account/password`, `DELETE /account` (both require `Authorization: Bearer`).

- [ ] **Step 1: DTO and service**

```java
// src/main/java/com/example/backtemplate/account/dto/ChangePasswordRequest.java
package com.example.backtemplate.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {}
```

`src/main/java/com/example/backtemplate/account/AccountService.java`:

```java
package com.example.backtemplate.account;

import com.example.backtemplate.account.dto.ChangePasswordRequest;
import com.example.backtemplate.auth.PasswordService;
import com.example.backtemplate.auth.UserRepository;
import com.example.backtemplate.common.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public void changePassword(UUID userId, ChangePasswordRequest req) {
        var user = userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Invalid session"));
        if (!passwordService.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Current password is incorrect");
        }
        user.setPasswordHash(passwordService.hash(req.newPassword()));
        userRepository.save(user);
    }

    public void deleteAccount(UUID userId) {
        userRepository.deleteById(userId);
    }
}
```

- [ ] **Step 2: Controller**

`src/main/java/com/example/backtemplate/account/AccountController.java`:

```java
package com.example.backtemplate.account;

import com.example.backtemplate.account.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PatchMapping("/password")
    public void changePassword(
            @AuthenticationPrincipal String userIdStr, @Valid @RequestBody ChangePasswordRequest req) {
        accountService.changePassword(UUID.fromString(userIdStr), req);
    }

    @DeleteMapping
    public void deleteAccount(@AuthenticationPrincipal String userIdStr) {
        accountService.deleteAccount(UUID.fromString(userIdStr));
    }
}
```

- [ ] **Step 3: Write and run the test**

`src/test/java/com/example/backtemplate/account/AccountControllerIntegrationTest.java`:

```java
package com.example.backtemplate.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String login(String email, String password) {
        restTemplate.postForEntity("/auth/register", Map.of("email", email, "password", password), Void.class);
        jdbcTemplate.update("UPDATE users SET email_verified = true WHERE email = ?", email);
        var resp =
                restTemplate.postForEntity(
                        "/auth/login", Map.of("email", email, "password", password), Map.class);
        return (String) resp.getBody().get("accessToken");
    }

    @Test
    void changePasswordThenDeleteAccount() {
        String token = login("account@example.com", "old-pw-123");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        var changeResp =
                restTemplate.exchange(
                        "/account/password",
                        HttpMethod.PATCH,
                        new HttpEntity<>(
                                Map.of("currentPassword", "old-pw-123", "newPassword", "new-pw-456"), headers),
                        Void.class);
        assertThat(changeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deleteResp =
                restTemplate.exchange(
                        "/account", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM users WHERE email = ?", Integer.class, "account@example.com");
        assertThat(count).isEqualTo(0);
    }
}
```

Run: `./mvnw -q test -Dtest=AccountControllerIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/backtemplate/account src/test/java/com/example/backtemplate/account
git commit -m "feat: add account endpoints (change password, delete account)"
```

---

## Task 17: Rate limiting

**Files:**
- Create: `src/main/java/com/example/backtemplate/config/RateLimitFilter.java`
- Modify: `src/main/java/com/example/backtemplate/config/SecurityConfig.java` (register the filter)
- Test: `src/test/java/com/example/backtemplate/config/RateLimitFilterIntegrationTest.java`

**Interfaces:**
- Produces: 429 `RATE_LIMITED` responses (via `ApiException.tooManyRequests`, Task 5) after 5 requests/minute/IP to `/auth/login` or `/auth/register`.

- [ ] **Step 1: Write the filter**

`src/main/java/com/example/backtemplate/config/RateLimitFilter.java`:

```java
package com.example.backtemplate.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/auth/login", "/auth/register");
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (LIMITED_PATHS.contains(request.getRequestURI())) {
            Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ip -> newBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter()
                        .write(
                                "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"details\":[]}}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build();
    }
}
```

- [ ] **Step 2: Register it in `SecurityConfig`**

Add `private final RateLimitFilter rateLimitFilter;` to the constructor-injected fields and chain another `.addFilterBefore(rateLimitFilter, JwtAuthFilter.class)` call after the existing `addFilterBefore`.

- [ ] **Step 3: Write and run the test**

`src/test/java/com/example/backtemplate/config/RateLimitFilterIntegrationTest.java`:

```java
package com.example.backtemplate.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backtemplate.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

class RateLimitFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void sixthLoginAttemptWithinAMinuteIsRateLimited() {
        var body = Map.of("email", "ratelimit@example.com", "password", "wrong-pw");

        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/auth/login", body, Void.class);
        }
        var sixth = restTemplate.postForEntity("/auth/login", body, Void.class);

        assertThat(sixth.getStatusCode().value()).isEqualTo(429);
    }
}
```

Run: `./mvnw -q test -Dtest=RateLimitFilterIntegrationTest`
Expected: `BUILD SUCCESS`, 1 test, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/backtemplate/config src/test/java/com/example/backtemplate/config/RateLimitFilterIntegrationTest.java
git commit -m "feat: add Bucket4j in-memory rate limiting on login/register"
```

---

## Task 18: Spotless lint/format + pre-commit hook

**Files:**
- Modify: `pom.xml` (Spotless config already added Task 1 — verify/extend)
- Create: `.githooks/pre-commit`
- Modify: `README.md` (setup instructions, created fully in Task 20 — add just the hook-setup line here if README doesn't exist yet, otherwise append)

- [ ] **Step 1: Run Spotless against the current codebase**

Run: `./mvnw -q spotless:apply`
Expected: reformats any files not matching `google-java-format`; exits 0.

Run: `./mvnw -q spotless:check`
Expected: `BUILD SUCCESS` (no violations after apply).

- [ ] **Step 2: Write the pre-commit hook**

`.githooks/pre-commit`:

```sh
#!/bin/sh
set -e
echo "Running spotless:check..."
./mvnw -q spotless:check
```

Run: `chmod +x .githooks/pre-commit`

- [ ] **Step 3: Point git at the hooks directory**

Run: `git config core.hooksPath .githooks`

Expected: subsequent commits in this repo run `spotless:check` first; a commit with unformatted code aborts with the Spotless diff.

- [ ] **Step 4: Verify the hook fires**

Run:
```bash
echo "// test" >> src/main/java/com/example/backtemplate/BackTemplateApplication.java
git add -A && git commit -m "test: verify pre-commit hook" --dry-run
```
Expected: hook output `Running spotless:check...` appears before the dry-run commit message. Revert the test edit: `git checkout -- src/main/java/com/example/backtemplate/BackTemplateApplication.java`.

- [ ] **Step 5: Commit**

```bash
git add .githooks pom.xml
git commit -m "chore: add Spotless pre-commit hook"
```

---

## Task 19: Dockerfile multi-stage

**Files:**
- Create: `Dockerfile`
- Modify: `docker-compose.yml` (add app healthcheck)

- [ ] **Step 1: Write the Dockerfile**

```dockerfile
# --- build ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -q -N io.takari:maven:wrapper -Dmaven=3.9.9 || true
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# --- runtime ---
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -f http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`JWT_SECRET` and other required env vars are intentionally **not** set at build time (only at container runtime via `docker-compose.yml`'s `env_file: .env`) — the build stage never invokes `spring-boot:run`, only `package`, so a missing `JWT_SECRET` at build time cannot break the image build (checklist requirement: "Build de Docker não pode quebrar por env obrigatória faltando").

- [ ] **Step 2: Add the app healthcheck to compose**

Add to the `app` service in `docker-compose.yml`:

```yaml
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:8080/actuator/health"]
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 20s
```

- [ ] **Step 3: Build and run the full stack**

Run:
```bash
docker compose up -d --build
docker compose ps
```
Expected: both `db` and `app` show `healthy` within ~30s.

Run: `curl -s 127.0.0.1:8080/actuator/health`
Expected: `{"status":"UP"}`.

Run: `docker compose down`

- [ ] **Step 4: Commit**

```bash
git add Dockerfile docker-compose.yml
git commit -m "feat: add multi-stage Dockerfile with non-root user and healthcheck"
```

---

## Task 20: Makefile + CI + dependabot + README

**Files:**
- Create: `Makefile`
- Create: `.github/workflows/ci.yml`
- Create: `.github/dependabot.yml`
- Create: `README.md`

- [ ] **Step 1: Write the Makefile**

```makefile
.PHONY: dev build start lint format format-check test test-e2e db-migrate db-generate docker-up docker-down

dev:
	SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

build:
	./mvnw -q -DskipTests package

start:
	java -jar target/*.jar

lint: format-check

format:
	./mvnw -q spotless:apply

format-check:
	./mvnw -q spotless:check

test:
	./mvnw -q test -Dtest='!*IntegrationTest'

test-e2e:
	./mvnw -q test -Dtest='*IntegrationTest'

db-migrate:
	./mvnw -q process-resources flyway:migrate

db-generate:
	@read -p "Migration name (snake_case, no prefix): " name; \
	ts=$$(date +%Y%m%d%H%M%S); \
	touch src/main/resources/db/migration/V$${ts}__$${name}.sql; \
	echo "Created src/main/resources/db/migration/V$${ts}__$${name}.sql"

docker-up:
	docker compose up -d

docker-down:
	docker compose down
```

- [ ] **Step 2: Write the CI workflow**

`.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: ./mvnw -q spotless:check
      - run: ./mvnw -q -DskipTests package
      - run: ./mvnw -q test -Dtest='!*IntegrationTest'

  docker:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker build -t back-template-spring:ci .

  e2e:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:17-alpine
        env:
          POSTGRES_DB: backtemplate
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        ports: ["5432:5432"]
        options: >-
          --health-cmd "pg_isready -U postgres -d backtemplate"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: ./mvnw -q test -Dtest='*IntegrationTest'
        env:
          JWT_SECRET: ci-test-secret-at-least-32-characters-long
```

Note: the `e2e` job's tests use Testcontainers (`AbstractIntegrationTest`), which spins up its own Postgres container regardless of the `postgres:` service block above — GitHub Actions runners provide Docker-in-Docker natively, so Testcontainers works without the `services:` block too. The `services:` block is kept here only as a fallback/example for teams that prefer service-container Postgres over Testcontainers in CI; it is not required for the tests as written in Tasks 4-17 to pass.

- [ ] **Step 3: Write dependabot config**

`.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: docker
    directory: /
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
```

- [ ] **Step 4: Write README.md**

`README.md`:

```markdown
# back-template-spring

Spring Boot backend template. Java 17, Maven, Postgres + Flyway + Spring Data JPA, JWT auth via Spring Security, reference `Note` CRUD domain.

## Quickstart

\`\`\`bash
cp .env.example .env
make docker-up      # starts Postgres
make db-migrate
make dev            # runs the app on :8080
\`\`\`

## Scripts

| Command | Does |
|---|---|
| `make dev` | run with hot classpath (dev profile) |
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
- `GET /actuator/health`

## Pre-commit hook

\`\`\`bash
git config core.hooksPath .githooks
\`\`\`

## Design notes

- `hibernate.ddl-auto` is `validate` everywhere — Flyway owns the schema. Never switch it to `update`/`create`, even locally; it silently masks migrations you forgot to write.
- Testcontainers needs a Docker daemon reachable from wherever tests run. GitHub-hosted Actions runners have this by default; self-hosted runners need Docker installed and the socket accessible.
- Spotless (`google-java-format`) fights IntelliJ's default formatter on import ordering — set the IDE's import order to match `google-java-format`, or just run `make format` before committing and ignore IDE warnings.
- JWT parsing has no clock-skew leeway configured; a dev machine with a wrong system clock will see intermittent 401s on freshly issued tokens.
- Bucket4j buckets are in-memory and per-instance — resets on restart, and doesn't coordinate across multiple app instances. Fine for a template/single-instance deploy; would need a Redis-backed bucket store to scale horizontally.
- `db:generate` only scaffolds an empty timestamped file — Flyway Community has no entity-diff autogeneration; write the SQL by hand.
- No `test:watch` — the JVM/Maven ecosystem has no idiomatic file-watch test runner without a third-party plugin; re-run `make test` manually or use your IDE's test runner.
```

- [ ] **Step 5: Run the full suite one last time**

Run: `./mvnw -q spotless:check && ./mvnw -q test`
Expected: `BUILD SUCCESS`, all unit + integration tests pass.

- [ ] **Step 6: Commit**

```bash
git add Makefile .github README.md
git commit -m "chore: add Makefile scripts, CI workflows, dependabot, and README"
```

---

## Self-Review Notes

- **Spec coverage:** every `[Obrigatório]` checklist item maps to a task — structure (package layout, Task 1/all), DB (Tasks 3/4/11), auth (Tasks 11-17), email (Task 13), logging (Task 9), API/CRUD/health/error-shape (Tasks 5-8), tests (every task, real Postgres via Testcontainers, no mocks for persistence), lint/format/pre-commit (Task 18), Docker (Task 19), CI/CD (Task 20), scripts (Task 20), env vars (Task 2 `.env.example`, Task 10 fail-fast validation), design notes (Task 20 README section).
- **Type consistency verified:** `NoteService` method names (`create/list/get/update/delete`) match what `NoteController` calls in Task 7; `JwtService` method names match what `JwtAuthFilter`/`AuthService` call in Tasks 13-14; `UserRepository`/`NoteRepository` query-method names match what their respective services call.
- **Known interim state:** Task 7's `NoteController` temporarily uses an `X-Owner-Id` header before Task 14 replaces it with the JWT principal — flagged inline in both tasks so it isn't mistaken for the final design.
