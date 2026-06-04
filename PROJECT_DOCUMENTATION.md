# Restaurant Reservation System — Project Documentation

## 1. Overview

A Spring Boot 3.3.5 + Java 21 restaurant management system with table reservations, menu management, order processing, invoicing, and reviews. Uses MySQL 8, Flyway migrations, JWT auth, and is fully containerized with Docker.

**57 unit/integration tests — all green.**

---

## 2. Tech Stack

| Layer          | Technology                                      |
|----------------|-------------------------------------------------|
| Framework      | Spring Boot 3.3.5, Spring MVC, Spring Data JPA |
| Security       | Spring Security 6, JWT (jjwt 0.11.5), BCrypt   |
| Database       | MySQL 8, Flyway migrations, H2 (tests)         |
| Validation     | Jakarta Bean Validation (@NotBlank, @Email, …) |
| API Docs       | SpringDoc OpenAPI 2.3.0 (Swagger UI)           |
| Build          | Maven, Java 21 (Temurin)                        |
| Container      | Docker, docker-compose                          |
| CI             | GitHub Actions                                  |
| Monitoring     | Spring Boot Admin 3.3.3                         |
| Logging        | SLF4J + Logback (logback-spring.xml)            |
| Lombok         | @Builder, @SuperBuilder, @Slf4j, etc.           |

### Key Dependencies (pom.xml)

| Dependency | Purpose |
|------------|---------|
| spring-boot-starter-web | REST API |
| spring-boot-starter-data-jpa | JPA + Hibernate |
| spring-boot-starter-validation | Bean Validation |
| spring-boot-starter-security | AuthN/AuthZ |
| mysql-connector-j | MySQL driver |
| flyway-mysql | DB migrations |
| springdoc-openapi-starter-webmvc-ui | Swagger UI |
| spring-boot-admin-starter-server | Monitoring dashboard |
| jjwt-api / jjwt-impl / jjwt-jackson (0.11.5) | JWT token handling |
| h2 (test) | In-memory DB for tests |
| lombok | Reduce boilerplate |

---

## 3. Enums

### 3.1 RoleName
```
ROLE_USER, ROLE_OWNER, ROLE_ADMIN
```
Seeded by Flyway V2.

### 3.2 UserStatus
```
ACTIVE, INACTIVE, PENDING_VERIFICATION, SUSPENDED
```

### 3.3 ReservationStatus
```
PENDING      → waiting for admin approval
APPROVED     → approved by admin
REJECTED     → rejected by admin
CANCELLED    → cancelled by user or admin
COMPLETED    → auto-set by scheduler after reservation time passes
```
**Workflow:**
- `PENDING → APPROVED → (auto via scheduler) COMPLETED`
- `PENDING → REJECTED`
- `PENDING / APPROVED → CANCELLED`

### 3.4 TableStatus
```
AVAILABLE, RESERVED, OCCUPIED, OUT_OF_SERVICE
```
Managed manually by admin via endpoint.

### 3.5 OrderStatus
```
PENDING → CONFIRMED → PREPARING → READY → SERVED → COMPLETED
                                                     → CANCELLED (any point)
```
When status reaches `COMPLETED`, an `Invoice` is auto-generated.

---

## 4. Database Schema (Flyway Migrations)

### V1__init_schema.sql — Core Tables

| Table            | Key Columns                                                       |
|------------------|-------------------------------------------------------------------|
| `users`          | id, first_name, last_name, email (unique), password_hash, status  |
| `roles`          | id, name (unique: ROLE_USER / ROLE_OWNER / ROLE_ADMIN)            |
| `user_roles`     | user_id, role_id (unique pair)                                    |
| `restaurants`    | id, name, location, is_deleted                                    |
| `dining_tables`  | id, table_number, capacity, restaurant_id (unique per restaurant)  |
| `reservations`   | id, user_id, table_id, start_time, end_time, status, guests_count |
| `refresh_tokens` | id, token_id (unique), user_id, token_hash, expires_at, revoked   |

All tables include: `created_at`, `updated_at`, `is_deleted` (soft delete).

### V2__seed_roles.sql
```sql
INSERT INTO roles (name) VALUES
  ('ROLE_USER'), ('ROLE_OWNER'), ('ROLE_ADMIN');
```

### V3__extend_schema.sql — Restaurant Management Upgrade

**ALTER tables:**
- `restaurants` — added `opening_time TIME`, `closing_time TIME`, `phone VARCHAR(20)`, `description TEXT`, `image_url VARCHAR(500)`
- `dining_tables` — added `table_status VARCHAR(30) DEFAULT 'AVAILABLE'`

**New tables:**
| Table             | Key Columns                                                      |
|-------------------|------------------------------------------------------------------|
| `menu_categories` | id, name (unique), description                                   |
| `menu_items`      | id, category_id (FK), name, description, price DECIMAL(10,2), image_url, available |
| `orders`          | id, table_id (FK), restaurant_id (FK), user_id (FK nullable), status, notes |
| `order_items`     | id, order_id (FK), menu_item_id (FK), quantity, unit_price (snapshot) |
| `invoices`        | id, order_id (FK unique), subtotal, tax_percent (14%), service_charge_percent (10%), tax_amount, service_charge_amount, total, generated_at |
| `reviews`         | id, user_id (FK), restaurant_id (FK), rating (1-5), comment      |

---

## 5. Entities

All entities extend `BaseEntity` which provides:
```
id (Long, auto-generated), createdAt, updatedAt, isDeleted (soft delete)
@Where(clause = "is_deleted = false") — global soft-delete filter
```

| Entity          | Builder Type     | Key Relationships                                    |
|-----------------|------------------|-------------------------------------------------------|
| `User`          | `@Builder`       | OneToMany → UserRole                                  |
| `Role`          | `@Builder`       | —                                                     |
| `UserRole`      | `@Builder`       | ManyToOne → User, ManyToOne → Role                    |
| `RefreshToken`  | `@Builder`       | ManyToOne → User                                      |
| `Restaurant`    | `@SuperBuilder`  | OneToMany → DiningTable                               |
| `DiningTable`   | `@SuperBuilder`  | ManyToOne → Restaurant, OneToMany → Reservation       |
| `Reservation`   | `@SuperBuilder`  | ManyToOne → DiningTable                               |
| `MenuCategory`  | `@SuperBuilder`  | OneToMany → MenuItem                                  |
| `MenuItem`      | `@SuperBuilder`  | ManyToOne → MenuCategory                              |
| `Order`         | `@SuperBuilder`  | ManyToOne → DiningTable, ManyToOne → Restaurant       |
| `OrderItem`     | `@SuperBuilder`  | ManyToOne → Order, ManyToOne → MenuItem               |
| `Invoice`       | `@SuperBuilder`  | OneToOne → Order                                      |
| `Review`        | `@SuperBuilder`  | ManyToOne → User, ManyToOne → Restaurant              |

**Important:** `User`, `Role`, `UserRole`, `RefreshToken` use `@Builder` (NOT `@SuperBuilder`). You must call `setId()` after `build()` — the fluent `.id(Long)` method is not available.

---

## 6. Security Architecture

### 6.1 Authentication Flow
1. `POST /api/auth/login` → returns `accessToken` (JWT) + `refreshToken` (opaque UUID)
2. All authenticated requests include `Authorization: Bearer <accessToken>`
3. When JWT expires: `POST /api/auth/refresh` with `refreshToken` → new JWT + new refresh token
4. `POST /api/auth/logout` → revokes the refresh token

### 6.2 JWT Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `auth.jwt.secret` | base64 test key | HMAC-SHA256 signing secret |
| `auth.jwt.expiration` | 86400000 (24h) | Token expiry in ms |

Both configurable via environment variables (`JWT_SECRET`, `JWT_EXPIRATION`).

### 6.3 Role Hierarchy
```
ROLE_ADMIN → can access everything
ROLE_OWNER → user endpoints + own reservations
ROLE_USER  → own profile + own reservations
```
`@PreAuthorize("hasRole('ADMIN')")` protects all admin endpoints.

### 6.4 Security Components
| Component | Responsibility |
|-----------|---------------|
| `JwtUtils` | Generate, validate, parse JWT tokens |
| `AuthTokenFilter` | OncePerRequestFilter — extracts JWT from Authorization header |
| `JwtAuthEntryPoint` | Returns 401 for unauthenticated requests |
| `ShopUserDetailsService` | Loads user from DB by email |
| `ShopUserDetails` | Implements UserDetails with user ID |
| `SecurityConfig` | CORS, CSRF disable, stateless sessions, endpoint permissions |
| `RateLimitFilter` | ConcurrentHashMap-based IP rate limiter |

### 6.5 CORS
```yaml
Allowed origins: http://localhost:3000, http://localhost:5173
Allowed methods: GET, POST, PUT, DELETE, OPTIONS
Allowed headers: *
Credentials: true
```

### 6.6 Password Reset
1. `POST /api/auth/forgot-password` → generates 6-digit OTP, temporarily stores it
2. `POST /api/auth/reset-password` with email + code + newPassword → validates OTP, encrypts and saves new password

### 6.7 Rate Limiting
- Simple `ConcurrentHashMap<String, RateLimitEntry>` filter
- Default: 20 requests per minute per IP
- Returns 429 Too Many Requests when exceeded

---

## 7. API Endpoints

All endpoints (except `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`) require JWT Bearer token.

Swagger UI: `http://localhost:8080/swagger-ui.html`

### 7.1 Auth (`/api/auth`)

| Method | Path                  | Access        | Description                          |
|--------|-----------------------|---------------|--------------------------------------|
| POST   | `/api/auth/register`  | Public        | Register new user                    |
| POST   | `/api/auth/login`     | Public        | Login → JWT + refresh token          |
| POST   | `/api/auth/refresh`   | Public        | Refresh expired JWT                  |
| POST   | `/api/auth/logout`    | Authenticated | Revoke refresh token                 |
| POST   | `/api/auth/forgot-password` | Public  | Send password reset OTP              |
| POST   | `/api/auth/reset-password`  | Public  | Reset password with OTP              |

### 7.2 User (`/api/users`)

| Method | Path                            | Access        | Description             |
|--------|---------------------------------|---------------|-------------------------|
| GET    | `/api/users/me`                 | Authenticated | Get current user profile|
| PUT    | `/api/users/me`                 | Authenticated | Update profile          |
| PUT    | `/api/users/me/change-password` | Authenticated | Change password         |

### 7.3 Reservations (`/api/reservations`)

| Method | Path                     | Access        | Description                              |
|--------|--------------------------|---------------|------------------------------------------|
| POST   | `/api/reservations`      | Authenticated | Create reservation (status = PENDING)    |
| GET    | `/api/reservations/my`   | Authenticated | Get my reservations (paginated)          |
| GET    | `/api/reservations/{id}` | Authenticated | Get reservation by ID (own only)         |
| DELETE | `/api/reservations/{id}` | Authenticated | Cancel own reservation (PENDING/APPROVED)|

### 7.4 Restaurants — Public (`/api/restaurants`)

| Method | Path                                          | Access        | Description                          |
|--------|-----------------------------------------------|---------------|--------------------------------------|
| GET    | `/api/restaurants/{id}/available-tables`      | Authenticated | Get available tables for time range   |
| GET    | `/api/restaurants/{id}/menu`                  | Authenticated | Get full menu (categories + items)    |
| POST   | `/api/restaurants/{restaurantId}/reviews`     | Authenticated | Create review (need completed reservation) |
| GET    | `/api/restaurants/{restaurantId}/reviews`     | Authenticated | Get reviews (paginated)               |

### 7.5 Orders (`/api/orders`)

| Method | Path                         | Access        | Description               |
|--------|------------------------------|---------------|---------------------------|
| POST   | `/api/orders`                | Authenticated | Create order              |
| POST   | `/api/orders/{id}/items`     | Authenticated | Add item to order         |
| PUT    | `/api/orders/{id}/status`    | Authenticated | Update order status       |
| GET    | `/api/orders/{id}`           | Authenticated | Get order by ID           |
| GET    | `/api/orders/table/{tableId}`| Authenticated | Get orders by table       |

### 7.6 Invoices (`/api/invoices`)

| Method | Path                            | Access        | Description              |
|--------|---------------------------------|---------------|--------------------------|
| GET    | `/api/invoices/{id}`            | Authenticated | Get invoice by ID        |
| GET    | `/api/invoices/order/{orderId}` | Authenticated | Get invoice by order     |

### 7.7 Admin (`/api/admin`) — `ROLE_ADMIN` only

**Restaurants & Tables:**
| Method | Path                                   | Description                      |
|--------|----------------------------------------|----------------------------------|
| POST   | `/api/admin/restaurants`               | Create restaurant                |
| PUT    | `/api/admin/restaurants/{id}`          | Update restaurant                |
| DELETE | `/api/admin/restaurants/{id}`          | Soft delete restaurant           |
| GET    | `/api/admin/restaurants`               | Get all restaurants (paginated)  |
| POST   | `/api/admin/tables`                    | Create dining table              |
| PUT    | `/api/admin/tables/{id}`               | Update dining table              |
| DELETE | `/api/admin/tables/{id}`               | Soft delete dining table         |
| PUT    | `/api/admin/tables/{id}/status`        | Update table status              |
| GET    | `/api/admin/restaurants/{id}/tables`   | Get tables by restaurant         |

**Users & Reservations:**
| Method | Path                                   | Description                      |
|--------|----------------------------------------|----------------------------------|
| PUT    | `/api/admin/users/{userId}/role`       | Assign OWNER role to user        |
| DELETE | `/api/admin/reservations/{id}`         | Cancel any reservation           |
| PUT    | `/api/admin/reservations/{id}/approve` | Approve pending reservation      |
| PUT    | `/api/admin/reservations/{id}/reject`  | Reject pending reservation       |

**Menu Management:**
| Method | Path                                   | Description                      |
|--------|----------------------------------------|----------------------------------|
| POST   | `/api/admin/categories`                | Create menu category             |
| PUT    | `/api/admin/categories/{id}`           | Update menu category             |
| DELETE | `/api/admin/categories/{id}`           | Soft delete menu category        |
| POST   | `/api/admin/menu-items`                | Create menu item                 |
| PUT    | `/api/admin/menu-items/{id}`           | Update menu item                 |
| DELETE | `/api/admin/menu-items/{id}`           | Soft delete menu item            |

---

## 8. Business Logic Details

### 8.1 Reservation Flow
1. User creates reservation → status = `PENDING`
2. Admin must approve or reject via dedicated endpoints
3. Conflict detection checks overlapping `PENDING` + `APPROVED` reservations only
4. Table availability query filters by `TableStatus.AVAILABLE` AND no overlapping `PENDING`/`APPROVED` reservations
5. **Scheduler** (`@Scheduled(fixedRate = 3_600_000)`): every hour, finds expired `APPROVED` reservations and marks them `COMPLETED`

### 8.2 Order Flow
1. Create order linked to a specific table and restaurant
2. Add items (quantity + menu item reference; price snapshotted in `unitPrice`)
3. Update status through the lifecycle
4. When order status becomes `COMPLETED` → **Invoice auto-generated** (idempotent — checks if invoice already exists for that order)

### 8.3 Invoice Calculation
```
subtotal        = Σ(orderItem.unitPrice * orderItem.quantity)
tax             = subtotal × 14%
service_charge  = subtotal × 10%
total           = subtotal + tax + service_charge
```

### 8.4 Review Rules
- Rating must be 1–5 (validated by `@Min(1) @Max(5)`)
- User must have at least **one COMPLETED reservation** at that restaurant to leave a review
- Paginated retrieval by restaurant

### 8.5 Soft Delete
All entities use `is_deleted` boolean + `@Where(clause = "is_deleted = false")`.  
Repository queries automatically exclude soft-deleted records. Deletes are UPDATEs, not DELETEs.

---

## 9. Services Layer

### 9.1 Service Interfaces & Implementations

```
service/
├── auth/AuthService           → AuthServiceImpl
├── user/UserService           → UserServiceImpl
├── reservation/ReservationService → ReservationServiceImpl
├── admin/AdminService         → AdminServiceImpl
├── menu/MenuService           → MenuServiceImpl
├── order/OrderService         → OrderServiceImpl
├── invoice/InvoiceService     → InvoiceServiceImpl
└── review/ReviewService       → ReviewServiceImpl
```

### 9.2 AdminServiceImpl Responsibilities
- CRUD on restaurants (soft delete)
- CRUD on dining tables (soft delete)
- CRUD on menu categories and menu items
- Approve / reject reservations
- Update table status
- Assign OWNER role to users
- Admin cancel any reservation

### 9.3 ReservationServiceImpl Responsibilities
- Create reservation (with pessimistic lock on table, conflict check, capacity check)
- Cancel own reservation (only PENDING or APPROVED)
- Get reservation by ID (own only — returns 403 if not owner)
- Get user reservations (paginated)
- Find available tables (by time range + status filter)
- `@Scheduled` auto-complete expired reservations

---

## 10. Testing

### 10.1 Test Summary (57 tests)

| Test Class                          | Type              | Tests | What it covers                                   |
|-------------------------------------|-------------------|-------|--------------------------------------------------|
| AuthControllerTest                  | @WebMvcTest       | 5     | Register, login, refresh, logout HTTP endpoints  |
| AdminControllerTest                 | @WebMvcTest       | 2     | Admin create restaurant, assign role             |
| ReservationControllerTest           | @WebMvcTest       | 3     | Create, get my, cancel reservation               |
| DiningTableRepositoryTest           | @DataJpaTest      | 6     | findAvailableTables, findWithLockById, paginated |
| ReservationRepositoryTest           | @DataJpaTest      | 7     | existsConflict, findConflictingTableIds, etc.    |
| AuthServiceImplTest                 | Mockito           | 7     | Login, register, refresh, forgot/reset password  |
| ReservationServiceImplTest          | Mockito           | 13    | Create/cancel/get, conflicts, capacity, paginate |
| AdminServiceImplTest                | Mockito           | 9     | Restaurant/table/menu CRUD, approve/reject       |
| UserServiceImplTest                 | Mockito           | 4     | Get/update profile, change password              |
| RestaurantReservationSystemApplicationTests | @SpringBootTest | 1 | Context loads successfully                 |

### 10.2 Testing Conventions
- **@DataJpaTest:** In-memory H2, Flyway disabled (`spring.flyway.enabled=false`), schema auto-generated from entities
- **@WebMvcTest:** `addFilters = false` to bypass security filters, manual `SecurityContextHolder.setAuthentication()`
- **Mockito tests:** `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` for service under test, `@Mock` for dependencies
- No Testcontainers (junior-friendly approach)

---

## 11. Running the Project

### 11.1 Prerequisites
```
Java 21 (Temurin)
MySQL 8.0+
Maven (or use provided mvnw wrapper)
```

### 11.2 Database Setup
```sql
CREATE DATABASE restaurant_reservation_db;
CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'Dev@2026#';
GRANT ALL PRIVILEGES ON restaurant_reservation_db.* TO 'dev_user'@'localhost';
FLUSH PRIVILEGES;
```

### 11.3 Run Locally (Development)
```bash
# Using Maven wrapper (no need to install Maven)
./mvnw spring-boot:run

# Or using installed Maven
mvn spring-boot:run
```
Server starts at `http://localhost:8080`.

### 11.4 Run Tests
```bash
./mvnw test
```
All 57 tests should pass.

### 11.5 Profiles
| Profile | File | Purpose |
|---------|------|---------|
| `dev` (default) | `application.properties` + `application-dev.yml` | Local MySQL |
| `prod` | `application-prod.yml` | Production config override |

Activate with: `--spring.profiles.active=prod`

### 11.6 Environment Variables

| Variable         | Default                                                  | Description              |
|------------------|----------------------------------------------------------|--------------------------|
| `DB_URL`         | `jdbc:mysql://localhost:3306/restaurant_reservation_db`  | MySQL JDBC URL           |
| `DB_USERNAME`    | `dev_user`                                               | Database user            |
| `DB_PASSWORD`    | `Dev@2026#`                                              | Database password        |
| `JWT_SECRET`     | Base64-encoded test key (256-bit)                        | JWT signing secret       |
| `JWT_EXPIRATION` | `86400000`                                               | JWT expiry in ms (24h)   |

### 11.7 Docker Deployment
```bash
# Build and run
docker compose up --build

# The app will be available at http://localhost:8080
# MySQL is exposed on port 3307 (internal 3306)
```

---

## 12. Docker

### Dockerfile (Multi-stage Build)
```
Stage 1 — Build:
  Base: maven:3.9-eclipse-temurin-21
  Steps: Download dependencies → Build with mvn package -DskipTests

Stage 2 — Runtime:
  Base: eclipse-temurin:21-jre
  Entry: java -jar app.jar
  Expose: 8080
```

### docker-compose.yml
| Service | Image          | Ports        | Notes                        |
|---------|----------------|--------------|------------------------------|
| app     | (builds from ./Dockerfile) | 8080:8080 | Depends on db (healthcheck)  |
| db      | mysql:8.0      | 3307:3306    | Volume: mysql_data, healthcheck |

Environment variables for app service are set with sensible defaults.

---

## 13. CI/CD (GitHub Actions)

**File:** `.github/workflows/ci.yml`

**Triggers:**
- Push to `main`, `master`, `develop`
- Pull requests to `main`, `master`

**Job steps:**
1. Start MySQL 8.0 service container
2. Checkout code
3. Set up JDK 21 (Temurin) with Maven cache
4. Run `mvn verify -B` with CI-specific environment variables

**CI environment overrides:**
```
DB_URL: jdbc:mysql://localhost:3306/restaurant_reservation
DB_USERNAME: root
DB_PASSWORD: root
JWT_SECRET: test-jwt-secret-key...
JWT_EXPIRATION: 86400000
```

---

## 14. Logging

**Config:** `logback-spring.xml`

**Settings:**
- **Console appender:** Pattern `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
- **File appender:** `logs/restaurant-reservation.log`
  - RollingPolicy: SizeBasedTriggeringPolicy (max 10MB)
  - Max history: 10 files
- **Root level:** INFO
- **Rate limiter logger:** DEBUG

---

## 15. Project Structure

```
restaurant-reservation-system/
├── .github/workflows/ci.yml          # CI pipeline
├── src/
│   ├── main/
│   │   ├── java/com/mahmoud/reservation/
│   │   │   ├── RestaurantReservationSystemApplication.java
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── ReservationController.java
│   │   │   │   ├── RestaurantController.java
│   │   │   │   ├── MenuController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── InvoiceController.java
│   │   │   │   ├── ReviewController.java
│   │   │   │   └── AdminController.java
│   │   │   ├── dto/
│   │   │   │   ├── admin/       (4 DTOs)
│   │   │   │   ├── auth/        (5 DTOs)
│   │   │   │   ├── common/      (2 DTOs)
│   │   │   │   ├── invoice/     (1 DTO)
│   │   │   │   ├── menu/        (4 DTOs)
│   │   │   │   ├── order/       (5 DTOs)
│   │   │   │   ├── reservation/ (2 DTOs)
│   │   │   │   ├── restaurant/  (1 DTO)
│   │   │   │   ├── review/      (2 DTOs)
│   │   │   │   ├── table/       (1 DTO)
│   │   │   │   └── user/        (2 DTOs)
│   │   │   ├── entity/
│   │   │   │   ├── BaseEntity.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── UserRole.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   ├── Restaurant.java
│   │   │   │   ├── DiningTable.java
│   │   │   │   ├── Reservation.java
│   │   │   │   ├── MenuCategory.java
│   │   │   │   ├── MenuItem.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── Invoice.java
│   │   │   │   └── Review.java
│   │   │   ├── enums/
│   │   │   │   ├── RoleName.java
│   │   │   │   ├── UserStatus.java
│   │   │   │   ├── ReservationStatus.java
│   │   │   │   ├── TableStatus.java
│   │   │   │   └── OrderStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── ApiException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── ForbiddenException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   ├── ConflictException.java
│   │   │   │   ├── ReservationConflictException.java
│   │   │   │   ├── ApiErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── mapper/
│   │   │   │   ├── UserMapper.java
│   │   │   │   └── ReservationMapper.java
│   │   │   ├── repository/     (9 repositories)
│   │   │   ├── security/
│   │   │   │   ├── config/SecurityConfig.java
│   │   │   │   ├── jwt/ (JwtUtils, AuthTokenFilter, JwtAuthEntryPoint)
│   │   │   │   ├── user/ (ShopUserDetails, ShopUserDetailsService)
│   │   │   │   └── ratelimit/RateLimitFilter.java
│   │   │   └── service/        (8 service pairs)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── logback-spring.xml
│   │       └── db/migration/
│   │           ├── V1__init_schema.sql
│   │           ├── V2__seed_roles.sql
│   │           └── V3__extend_schema.sql
│   └── test/java/com/mahmoud/reservation/  (10 test classes)
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .dockerignore
├── pom.xml
└── PROJECT_DOCUMENTATION.md
```

---

## 16. Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| H2 for tests instead of Testcontainers | Junior-friendly, simpler setup, no Docker requirement for tests |
| Flyway disabled in tests | H2 + JPA auto DDL handles schema creation for tests |
| `@AutoConfigureMockMvc(addFilters = false)` | Avoid full security context setup in controller tests |
| Simple `ConcurrentHashMap` rate limiter | No external library dependencies (Bucket4j, etc.) |
| Price snapshot in `OrderItem.unitPrice` | Historical accuracy when menu prices change over time |
| Invoice auto-generated on order COMPLETED | No payment system (per user requirement) |
| Review requires COMPLETED reservation | Prevents fake/invalid reviews |
| Table status admin-managed (not auto-changed) | Full manual control for restaurant staff |
| Reservation status: CONFIRMED → APPROVED/REJECTED | Two-stage admin approval workflow |
| `User`/`Role`/`UserRole`/`RefreshToken` use `@Builder` (not `@SuperBuilder`) | Legacy design; use `setId()` after `build()` |
| `@Where(clause = "is_deleted = false")` on BaseEntity | Global soft-delete filter, all queries exclude deleted records automatically |
| `@PrePersist` + `@PreUpdate` for email normalization | Ensures email uniqueness is case-insensitive at DB level |
