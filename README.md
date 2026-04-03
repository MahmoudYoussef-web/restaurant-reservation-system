# 🍽️ Restaurant Reservation System

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-yellow)
![Status](https://img.shields.io/badge/Status-Production--Ready-brightgreen)

---

## 🚀 Overview

A **production-grade restaurant reservation system** designed to handle real-world booking challenges such as **concurrent requests, time conflicts, and data consistency**.

This project focuses on **backend engineering principles**, not just CRUD:

- Concurrency control
- Transactional consistency
- Business rule enforcement
- Scalable API design

---

## 🧠 Engineering Highlights

- Pessimistic locking to prevent race conditions
- Time conflict detection using interval overlap logic
- Optimized queries (`NOT EXISTS` instead of `NOT IN`)
- Indexed queries for high-performance lookups
- Stateless JWT authentication (Access + Refresh Tokens)
- Role-based access control (ADMIN / USER)
- Clean layered architecture with DTO isolation

---

## ⚙️ How to Run

### 1. Clone the repository

```bash
git clone https://github.com/MahmoudYoussef-web/restaurant-reservation-system.git
cd restaurant-reservation-system
````

---

### 2. Requirements

* Java 21
* MySQL 8

---

### 3. Configuration

Update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/restaurant_reservation
spring.datasource.username=your_user
spring.datasource.password=your_password
```

---

### 4. Run the application

```bash
mvn spring-boot:run
```

---

### 5. Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📡 API Overview

### 🔐 Authentication

* POST `/api/auth/register`
* POST `/api/auth/login`
* POST `/api/auth/refresh`
* POST `/api/auth/logout`

---

### 👤 Users

* GET `/api/users/me`
* PUT `/api/users/me`

---

### 📅 Reservations

* POST `/api/reservations`
* GET `/api/reservations/{id}`
* GET `/api/reservations/my`
* DELETE `/api/reservations/{id}`

---

### 🍽️ Restaurants & Tables

* GET `/api/restaurants/{id}/available-tables`
* GET `/api/admin/restaurants`
* POST `/api/admin/restaurants`
* GET `/api/admin/restaurants/{id}/tables`
* POST `/api/admin/tables`

---

## 🏗️ Architecture

```
Controller → Service → Repository → Database
        ↘ DTO ↔ Mapper ↗
```

### Design Principles:

* Separation of concerns
* Business logic isolated in service layer
* DTOs used to prevent entity exposure
* Stateless authentication

---

## ⚡ Core Flow

```
User → Authenticate (JWT)
     → Check available tables
     → Validate time conflicts
     → Lock table (pessimistic locking)
     → Create reservation
     → Retrieve / cancel reservation
```

---

## 📊 Business Rules

* Users can only manage their own reservations
* Reservations cannot overlap on the same table
* Time must be valid (`start < end`)
* Guests must not exceed table capacity
* Only active reservations block time slots
* Reservations can only be cancelled once

---

## ⚖️ Trade-offs

| Decision            | Trade-off                 |
| ------------------- | ------------------------- |
| Pessimistic locking | Reduced concurrency       |
| Complex validation  | Increased code complexity |
| No caching          | Higher DB load            |

---

## 🧪 Edge Cases Handled

* Concurrent booking requests
* Overlapping time ranges
* Invalid reservation input
* Unauthorized access
* Duplicate cancellation attempts

---

## 🗄️ Database Design

![Database](https://github.com/user-attachments/assets/c0f98e15-44fa-4a4b-89f0-798de1572bae)

---

## 🎯 What This Project Demonstrates

* Concurrency handling in real systems
* Conflict detection algorithms
* Transaction-safe operations
* Secure authentication and authorization
* Clean backend architecture

---

## 👨‍💻 Author

Mahmoud Youssef
Backend Developer (Spring Boot)

---

## 🏁 Final Result

✔ Concurrency-safe
✔ Consistent
✔ Secure
✔ Scalable
✔ Production-ready

````

ونكمل 🔥
