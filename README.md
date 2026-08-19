# Software Marketplace — Backend

Multi-vendor digital software marketplace backend (Spring Boot + PostgreSQL + Flyway).

## Stack
- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Security, Validation)
- PostgreSQL 16
- Flyway (schema migrations — Hibernate `ddl-auto` is `validate` only, Flyway owns the schema)
- JWT auth (jjwt)
- Lombok + MapStruct
- Testcontainers (integration tests against a real Postgres, not H2)

## Package structure
```
com.marketplace
├── config       # Spring config beans (security, CORS, OpenAPI, etc.)
├── controller   # REST controllers
├── dto          # Request/response DTOs
├── entity       # JPA entities
├── enums        # Enum types mirroring the Postgres enum types
├── exception    # Custom exceptions + @ControllerAdvice global handler
├── repository   # Spring Data JPA repositories
├── security     # JWT filter, UserDetailsService, auth utilities
├── service      # Business logic
└── util         # Shared helpers
```

## Local setup

1. **Start Postgres:**
   ```bash
   docker compose up -d
   ```

2. **Set env vars** (or rely on the defaults in `application.yml` — fine for local dev only):
   ```bash
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=marketplace_db
   export DB_USERNAME=marketplace_user
   export DB_PASSWORD=marketplace_pass
   export JWT_SECRET=$(openssl rand -base64 48)
   ```

3. **Run the app:**
   ```bash
   mvn spring-boot:run
   ```
   (No Maven Wrapper is checked in yet — if you want one, run `mvn -N io.takari:maven:wrapper` once locally to generate `mvnw`/`mvnw.cmd`.)
   Flyway runs `V1__init.sql` automatically against the DB on startup.

4. **Verify:** app starts on `http://localhost:8080`.

## Migrations
All schema changes go through Flyway — never hand-edit the DB, never let Hibernate auto-generate DDL.
New migration files go in `src/main/resources/db/migration/`, named `V{n}__description.sql`, and must be sequential and immutable once merged (never edit a migration that's already run anywhere).

## Notes
- `spring.jpa.hibernate.ddl-auto` is locked to `validate` — this is intentional. If it mismatches the schema, fix the migration, not this setting.
- Escrow status lives on `order_items`, not `orders`, because a single checkout can span multiple vendors with independent payout timelines.
