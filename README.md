```md
# 🍽️ Restaurant Reservation System (Spring Boot)

A production-grade backend system designed to handle real-world restaurant booking scenarios with strong focus on concurrency control, data integrity, and scalable API design.

---

## 🚀 Why This Project?

Restaurant booking systems face critical challenges such as:

- Double booking due to concurrent requests
- Time-based availability conflicts
- Data consistency under high load

This system solves these problems using:
- Pessimistic database locking
- Conflict detection algorithms
- Transactional consistency

---

## 🧠 Key Engineering Decisions

### 🔒 Concurrency Control
- Implemented **Pessimistic Locking** to prevent race conditions
- Ensures only one transaction can reserve a table at a time

### ⏱️ Time Conflict Detection
- Uses interval overlap logic:
```

start < existingEnd AND end > existingStart

```
- Prevents double booking at the database level

### 🔐 Authentication & Security
- Stateless JWT authentication with refresh tokens
- Role-based access control (ADMIN / USER)

### ⚡ Query Optimization
- Replaced `NOT IN` with `NOT EXISTS` for better performance and correctness
- Indexed critical columns:
- `(table_id, start_time, end_time)`
- `(user_id)`

---

## 🏗️ Architecture

```

Controller → Service → Repository → Entity
↘ DTO ↔ Mapper ↗

```

- Clean separation of concerns
- Business logic isolated in service layer
- DTOs used to prevent entity exposure

---

## 📊 Core Features

- Concurrency-safe reservation system
- Real-time table availability search
- Pagination for scalable APIs
- Global exception handling
- Ownership-based access control

---

## 🔄 Core Flow

1. User authenticates via JWT
2. User searches available tables
3. System checks conflicts using optimized queries
4. Table is locked using DB-level locking
5. Reservation is created safely

---

## 🧰 Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- MySQL 8
- JWT
- Swagger (OpenAPI)

---

## 📌 API Highlights

- `/api/auth/*` → Authentication
- `/api/reservations/*` → Reservation lifecycle
- `/api/restaurants/{id}/available-tables` → Availability search
- `/api/admin/*` → Admin management

---

## ⚠️ Edge Cases Handled

- Concurrent booking requests
- Invalid time ranges
- Over-capacity reservations
- Unauthorized access
- Repeated cancellation

---

## 🚀 Future Improvements

- Redis caching for availability queries
- Rate limiting for API protection
- Docker & CI/CD pipeline
- Distributed locking (for microservices)
- Observability (logging + metrics)

---

## 👨‍💻 Author

Mahmoud  
Backend Developer (Spring Boot)
```

