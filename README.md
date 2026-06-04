# 🍽️ Restaurant Reservation System

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-%234479A1?logo=mysql)](https://www.mysql.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT_+_Refresh-%23000000?logo=jsonwebtokens)](https://jwt.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-%232496ED?logo=docker)](https://www.docker.com/)
[![Flyway](https://img.shields.io/badge/Migrations-Flyway-%23CC0200?logo=flyway)](https://flywaydb.org/)
[![Tests](https://img.shields.io/badge/Tests-57_✔️-brightgreen)]()
[![Swagger](https://img.shields.io/badge/API-Swagger_UI-%2385EA2D?logo=swagger)](https://swagger.io/)
[![CI](https://img.shields.io/badge/CI-GitHub_Actions-%232088FF?logo=githubactions)](.github/workflows/ci.yml)

A production-grade restaurant reservation and management backend built with **Spring Boot 3.3.5** and **Java 21**. Covers the complete restaurant lifecycle — table reservations with admin approval workflows, menu management, order processing, auto-generated invoicing, and customer reviews.

> 🏗️ **Architecture:** Layered monolithic design (Controller → Service → Repository → DB) with JWT-secured REST API, Flyway versioned migrations, rate limiting, and Docker containerization.

---

## 📐 System Architecture

```mermaid
flowchart TB
    Client["🌐 HTTP Clients"]
    Security["🛡️ Security Layer<br/>JWT · Rate Limiter · CORS · @PreAuthorize"]
    Controller["🎮 Controller Layer<br/>Auth · User · Reservation · Restaurant<br/>Menu · Order · Invoice · Review · Admin"]
    Service["⚙️ Service Layer<br/>Auth · User · Reservation · Admin<br/>Menu · Order · Invoice · Review<br/>Scheduler: auto-complete expired"]
    Repo["📦 Repository Layer<br/>User · Role · Reservation · DiningTable<br/>Restaurant · MenuCat · MenuItem<br/>Order · OrderItem · Invoice · Review"]
    DB["🗄️ Database (MySQL 8)<br/>Flyway: V1 → V2 → V3"]

    Client --> Security
    Security --> Controller
    Controller --> Service
    Service --> Repo
    Repo --> DB
```

---

## 🗄️ Database Schema

<img width="735" alt="restaurant" src="https://github.com/user-attachments/assets/e69b867e-5718-49d5-8c7d-445521b9c8fd" />

### Tables Overview

| Table | Key Columns | Purpose |
|-------|-------------|---------|
| `users` | email (unique), password_hash, status, email_verified | User accounts |
| `roles` | name (ROLE_USER / ROLE_OWNER / ROLE_ADMIN) | Role definitions (seeded) |
| `user_roles` | user_id + role_id (unique pair) | Many-to-many join |
| `restaurants` | name, location, opening_time, closing_time, phone, description, image_url | Restaurant profiles |
| `dining_tables` | table_number, capacity, table_status, restaurant_id | Physical tables |
| `reservations` | user_id, table_id, start_time, end_time, status, guests | Booking records |
| `refresh_tokens` | token_id (unique), token_hash, user_id, expires_at, revoked | JWT refresh tokens |
| `menu_categories` | name (unique), description | Menu grouping |
| `menu_items` | category_id, name, price, description, image_url, available | Menu catalog |
| `orders` | table_id, restaurant_id, user_id, status, notes | Customer orders |
| `order_items` | order_id, menu_item_id, quantity, unit_price (snapshot) | Line items |
| `invoices` | order_id, subtotal, tax (14%), service (10%), total | Auto-generated billing |
| `reviews` | user_id, restaurant_id, rating (1-5), comment | Customer feedback |

> 💡 All entities extend `BaseEntity` with `id`, `created_at`, `updated_at`, `is_deleted` (soft delete via `@Where`).

---

## 🔄 Reservation Lifecycle

1. **User** creates reservation → `POST /api/reservations`
2. **System** locks table, checks conflicts & capacity
3. Status set to `PENDING`
4. **Admin** approves → `PUT /admin/reservations/{id}/approve`
5. Status set to `APPROVED`
6. **Scheduler** runs hourly → auto-completes expired reservations
7. Status set to `COMPLETED`

### Status Transitions

```
PENDING → APPROVED → COMPLETED (auto)
PENDING → REJECTED
PENDING → CANCELLED
APPROVED → CANCELLED
```

---

## 🛡️ Security

### Auth Flow

1. **Login**: `POST /api/auth/login` with email + password
2. Backend finds user, checks BCrypt hash, generates JWT + refresh token
3. Client receives `{ accessToken, refreshToken }`
4. **Subsequent requests**: Include `Authorization: Bearer <token>`
5. `AuthTokenFilter` extracts JWT, validates signature, extracts roles
6. `@PreAuthorize` enforces role-based access

### Security Components

| Component | Type | Responsibility |
|-----------|------|----------------|
| `JwtUtils` | Utility | Generate, parse, validate JWT tokens |
| `AuthTokenFilter` | OncePerRequestFilter | Extract JWT from `Authorization: Bearer` |
| `JwtAuthEntryPoint` | AuthenticationEntryPoint | Return 401 JSON for unauthenticated |
| `ShopUserDetailsService` | UserDetailsService | Load user by email |
| `RateLimitFilter` | OncePerRequestFilter | 20 req/min per IP (ConcurrentHashMap) |

### Roles

| Role | Permissions |
|------|-------------|
| 🟢 `ROLE_USER` | Own profile, own reservations, menu, orders, reviews |
| 🟡 `ROLE_OWNER` | Inherits USER + manage own restaurants |
| 🔴 `ROLE_ADMIN` | Full access — all CRUD, approve/reject, role assignment |

---

## 📡 API Endpoints

### 🔓 Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login → JWT + refresh token |
| POST | `/api/auth/refresh` | Refresh expired access token |
| POST | `/api/auth/forgot-password` | Request password reset OTP |
| POST | `/api/auth/reset-password` | Reset password with OTP |

### 🔐 Authenticated

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get profile |
| PUT | `/api/users/me` | Update profile |
| PUT | `/api/users/me/change-password` | Change password |
| POST | `/api/auth/logout` | Revoke refresh token |
| POST | `/api/reservations` | Create reservation (PENDING) |
| GET | `/api/reservations/my` | My reservations (paginated) |
| GET | `/api/reservations/{id}` | Get reservation |
| DELETE | `/api/reservations/{id}` | Cancel own reservation |
| GET | `/api/restaurants/{id}/available-tables` | Available tables |
| GET | `/api/restaurants/{id}/menu` | Get menu |
| POST | `/api/orders` | Create order |
| POST | `/api/orders/{id}/items` | Add item to order |
| PUT | `/api/orders/{id}/status` | Update order status |
| GET | `/api/orders/{id}` | Get order |
| GET | `/api/orders/table/{tableId}` | Orders by table |
| GET | `/api/invoices/{id}` | Get invoice |
| GET | `/api/invoices/order/{orderId}` | Invoice by order |
| POST | `/api/restaurants/{restaurantId}/reviews` | Create review |
| GET | `/api/restaurants/{restaurantId}/reviews` | Get reviews |

### 🛡️ Admin Only

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/restaurants` | Create restaurant |
| PUT | `/api/admin/restaurants/{id}` | Update restaurant |
| DELETE | `/api/admin/restaurants/{id}` | Soft delete |
| GET | `/api/admin/restaurants` | List (paginated) |
| POST | `/api/admin/tables` | Create dining table |
| PUT | `/api/admin/tables/{id}` | Update table |
| PUT | `/api/admin/tables/{id}/status` | Update table status |
| DELETE | `/api/admin/tables/{id}` | Soft delete |
| GET | `/api/admin/restaurants/{id}/tables` | Tables by restaurant |
| PUT | `/api/admin/users/{userId}/role` | Assign OWNER role |
| DELETE | `/api/admin/reservations/{id}` | Cancel any reservation |
| PUT | `/api/admin/reservations/{id}/approve` | Approve reservation |
| PUT | `/api/admin/reservations/{id}/reject` | Reject reservation |
| POST | `/api/admin/categories` | Create category |
| PUT | `/api/admin/categories/{id}` | Update category |
| DELETE | `/api/admin/categories/{id}` | Soft delete |
| POST | `/api/admin/menu-items` | Create menu item |
| PUT | `/api/admin/menu-items/{id}` | Update menu item |
| DELETE | `/api/admin/menu-items/{id}` | Soft delete |

---

## ⚙️ Invoice Calculation

Auto-generated when order reaches `COMPLETED`:

```
subtotal        = Σ (unitPrice × quantity)
tax             = subtotal × 14%
service_charge  = subtotal × 10%
total           = subtotal + tax + service_charge
```

> 🔒 **Idempotent:** Skips if invoice already exists for the order.

---

## 🧪 Testing (57 tests — all green)

| Layer | Tests | Strategy |
|-------|-------|----------|
| 🎮 Controller | 10 | @WebMvcTest + MockMvc (addFilters=false) |
| ⚙️ Service | 33 | Mockito + JUnit 5 |
| 📦 Repository | 13 | @DataJpaTest (H2, real queries) |
| 🏗️ Context | 1 | @SpringBootTest |

---

## 🚀 Quick Start

### Prerequisites
- Java 21, MySQL 8, Maven (or `mvnw`)

### Database
```sql
CREATE DATABASE restaurant_reservation_db;
CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'Dev@2026#';
GRANT ALL ON restaurant_reservation_db.* TO 'dev_user'@'localhost';
```

### Run
```bash
./mvnw spring-boot:run
```
Server → `http://localhost:8080`  
Swagger → `http://localhost:8080/swagger-ui.html`

### Tests
```bash
./mvnw test
```

### Docker
```bash
docker compose up --build
```

---

## 🔧 Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:mysql://localhost:3306/restaurant_reservation_db` | JDBC URL |
| `DB_USERNAME` | `dev_user` | Database user |
| `DB_PASSWORD` | `Dev@2026#` | Database password |
| `JWT_SECRET` | Base64 HMAC-SHA256 key | JWT signing secret |
| `JWT_EXPIRATION` | `86400000` (24h) | JWT expiry |

---

## 📁 Project Structure

```
src/main/java/com/mahmoud/reservation/
├── config/          # OpenAPI / Swagger
├── controller/      # 9 REST controllers
├── dto/             # 30+ request/response DTOs
├── entity/          # 12 JPA entities (BaseEntity)
├── enums/           # 5 enums
├── exception/       # 9 exceptions + GlobalExceptionHandler
├── mapper/          # Entity ↔ DTO mapping
├── repository/      # 9 Spring Data JPA repos
├── security/        # JWT, auth filter, rate limiter, CORS
└── service/         # 8 service pairs
```

---

## 👤 Author

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/MahmoudYoussef-web">
        <img src="https://github.com/MahmoudYoussef-web.png" width="100" height="100" style="border-radius:50%;"/>
      </a>
    </td>
    <td>
      <b>Mahmoud Youssef</b><br/>
      <sub>Backend Engineer — Spring Boot · Java · REST APIs</sub><br/><br/>
      <a href="https://github.com/MahmoudYoussef-web">
        <img src="https://img.shields.io/badge/GitHub-MahmoudYoussef--web-181717?style=for-the-badge&logo=github"/>
      </a>
      &nbsp;
      <a href="https://www.linkedin.com/in/mahmoud-youssef-dev/">
        <img src="https://img.shields.io/badge/LinkedIn-mahmoud--youssef--dev-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white"/>
      </a>
    </td>
  </tr>
</table>
