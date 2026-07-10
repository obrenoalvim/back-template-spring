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
