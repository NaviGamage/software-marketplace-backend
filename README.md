# Software Marketplace — Backend

Multi-vendor digital software marketplace backend (Spring Boot + PostgreSQL + Flyway).

Vendors list software products for sale, buyers purchase them through an escrow-protected
checkout, and funds are released to vendors automatically once the delivery window clears —
with disputes, refunds, and reviews built in.

## Stack
- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Security, Validation)
- PostgreSQL 16
- Flyway (schema migrations — Hibernate `ddl-auto` is `validate` only, Flyway owns the schema)
- JWT auth (jjwt)
- AWS S3 (compatible storage) — product files, S3 presigned URLs for secure downloads
- Lombok + MapStruct
- Testcontainers (integration tests against a real Postgres, not H2)

## Package structure
```
com.marketplace
├── config       # Spring config beans (security, S3, CORS, etc.)
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
   export AWS_ACCESS_KEY=...
   export AWS_SECRET_KEY=...
   export AWS_S3_ENDPOINT=...
   export AWS_S3_BUCKET=marketplace
   ```

3. **Run the app:**
   ```bash
   mvn spring-boot:run
   ```
   (No Maven Wrapper is checked in yet — if you want one, run `mvn -N io.takari:maven:wrapper` once locally to generate `mvnw`/`mvnw.cmd`.)
   Flyway runs all pending migrations in `db/migration/` automatically against the DB on startup.

4. **Verify:** app starts on `http://localhost:8080`.

## Migrations
All schema changes go through Flyway — never hand-edit the DB, never let Hibernate auto-generate DDL.
New migration files go in `src/main/resources/db/migration/`, named `V{n}__description.sql`, and must be sequential and immutable once merged (never edit a migration that's already run anywhere).

| Version | Adds |
|---|---|
| V1 | Core schema — users, products, orders, order_items, downloads, reviews, payouts |
| V2 | Links `order_items` to `payouts` (prevents an item being paid out twice) |
| V3 | Dispute tracking fields on `order_items` |
| V4 | Rating aggregate columns (`average_rating`, `review_count`) on `products` |

---

## Features implemented

### Phase 1 — Foundation & Trust
- JWT-based authentication and registration, with role-based access control (`ADMIN` / `VENDOR` / `BUYER`) via `@PreAuthorize`
- Strong password validation
- Product CRUD for vendors, with file (ZIP) and image uploads to S3
- Admin approval workflow — products move through `DRAFT → PENDING_REVIEW → APPROVED/REJECTED`; only `APPROVED` products are purchasable
- Global exception handling — consistent JSON error responses across the API

### Phase 2 — Orders & Escrow
- `Order` / `OrderItem` entities using native Postgres enums (`@JdbcTypeCode(SqlTypes.NAMED_ENUM)`)
- `EntityGraph` fetches to avoid N+1 queries on order listings
- Checkout endpoint (currently a mock payment — see **Payments** below) that calculates platform commission and vendor earnings per line item using `BigDecimal`
- Escrow hold on every order item (configurable hold period, default 14 days) before funds become payout-eligible

### Phase 3 — Secure Delivery
- Secure, time-limited downloads via S3 presigned URLs — buyers can only generate a link for items they own, on `PAID` orders, that aren't disputed or refunded
- Automated escrow release — a scheduled job (`@Scheduled`, hourly) flips eligible `order_items` from `HOLDING` to `COMPLETED` via a single bulk `UPDATE` query (no entity loading, no N+1)

### Phase 4 — Vendor Payouts
- Vendor earnings endpoint — total earned, pending, withdrawn, and available-to-withdraw balance
- Withdrawal requests, with a configurable minimum payout amount
- Race-condition-safe balance claiming — eligible order items are locked (`PESSIMISTIC_WRITE`) and tagged with a `payout_id` when a payout is requested, so two concurrent requests can never double-claim the same funds
- Admin approve/reject workflow — rejecting a payout releases its claimed items back into the available pool

### Phase 5 — Disputes & Refunds
- Buyers can open a dispute on a purchased item (only before it's been claimed by a vendor payout)
- Disputed items are excluded from escrow release and from download access
- Admin resolution: approve (marks the item `REFUNDED`) or dismiss (returns the item to `HOLDING`/`COMPLETED`)

### Phase 6 — Reviews & Ratings
- Verified-purchase reviews only — a buyer must hold a `PAID`, non-refunded order item for a product before reviewing it
- One review per buyer per product, enforced at both the DB and application level
- Product rating (`average_rating`, `review_count`) is recalculated with a single bulk SQL query on each new review — never loaded into memory and averaged in Java

---

## API overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Products | `GET/POST /api/products`, `PUT/DELETE /api/products/{id}` |
| Admin — Products | `PATCH /api/admin/products/{id}/approve`, `.../reject` |
| Categories | `GET/POST/PUT/DELETE /api/categories` |
| Orders | `POST /api/orders` (checkout), `GET /api/orders/my`, `GET /api/orders/my-sales` |
| Downloads | `GET /api/orders/items/{orderItemId}/download` |
| Disputes | `POST /api/orders/items/{orderItemId}/dispute`, `GET /api/admin/disputes`, `PATCH /api/admin/disputes/{orderItemId}/resolve` |
| Vendor Payouts | `GET /api/vendor/earnings`, `POST /api/vendor/payouts`, `GET /api/vendor/payouts` |
| Admin — Payouts | `GET /api/admin/payouts`, `PATCH /api/admin/payouts/{id}/approve`, `.../reject` |
| Reviews | `POST/GET /api/products/{productId}/reviews` |

All endpoints return a consistent `ApiResponse<T>` envelope; list endpoints are paginated (`Pageable`).

## Payments

Checkout currently uses a **mock payment flow** — the order is marked `PAID` immediately with no real
payment collected. This let the full purchase → escrow → download → payout → dispute → review flow be
built and tested end-to-end before wiring up a payment provider.

**Swapping in real payments:** create the order as `PENDING` in `OrderService.checkout()`, and only flip
it to `PAID` from a payment provider's webhook handler once payment is actually confirmed. Everything
downstream (escrow, payouts, disputes, reviews) already keys off `OrderStatus.PAID` and needs no changes.

## Notes
- `spring.jpa.hibernate.ddl-auto` is locked to `validate` — this is intentional. If it mismatches the schema, fix the migration, not this setting.
- Escrow status lives on `order_items`, not `orders`, because a single checkout can span multiple vendors with independent payout timelines.
- Financial aggregation (escrow release, rating recalculation) is always done with bulk SQL (`@Modifying @Query`), never by loading entities into memory and summing/averaging in Java — this keeps those operations cheap regardless of data volume.

## Roadmap
- [ ] Live payment gateway integration (Stripe) — replacing the mock checkout
- [ ] API documentation (OpenAPI/Swagger)