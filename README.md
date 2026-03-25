# 🍽️ Restaurant Reservation System (Spring Boot)

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-yellow)
![Status](https://img.shields.io/badge/Status-Production--Ready-brightgreen)

A production-grade backend system designed to handle real-world restaurant booking scenarios with strong focus on concurrency control, data integrity, and scalable API design.

---

## 🚀 Why This Project?

Restaurant booking systems face critical challenges such as:

* Double booking due to concurrent requests
* Time-based availability conflicts
* Data inconsistency under high load

This system solves these problems using:

* Pessimistic database locking
* Conflict detection algorithms
* Transactional consistency

---

## 🧠 Key Engineering Decisions

### 🔒 Concurrency Control

* Implemented **Pessimistic Locking** to prevent race conditions
* Ensures only one transaction can reserve a table at a time

### ⏱️ Time Conflict Detection

* Uses interval overlap logic:

```
start < existingEnd AND end > existingStart
```

* Prevents double booking at the database level

### 🔐 Authentication & Security

* Stateless JWT authentication with Access & Refresh Tokens
* Role-based access control (ADMIN / USER)

### ⚡ Query Optimization

* Replaced `NOT IN` with `NOT EXISTS` for better performance and correctness
* Added indexes for critical queries:

  * `(table_id, start_time, end_time)`
  * `(user_id)`

---

## 🚀 Key Features

* Concurrency-safe reservation system
* Real-time table availability search
* Pagination support for scalable APIs
* Global exception handling with standardized responses
* Clean DTO mapping and separation of concerns
* Ownership-based access control
* Stateless and secure backend design

---

## 📊 Business Rules

* A user can only access and manage their own reservations
* Reservations cannot overlap on the same table
* Reservation time must be valid (`start < end`)
* Number of guests must not exceed table capacity
* Only active reservations (PENDING, CONFIRMED) block time slots
* A reservation can only be cancelled once
* Table availability is dynamically calculated based on time range

---

## 🔄 Core Flow

1. User authenticates via JWT
2. User checks available tables for a time range
3. System validates availability using optimized queries
4. Table is locked using database-level locking
5. Reservation is created safely without conflicts
6. User can view or cancel their reservations

---

## 🏗️ Architecture

```
Controller → Service → Repository → Entity
        ↘ DTO ↔ Mapper ↗
```

* Clean separation of concerns
* Business logic isolated in service layer
* DTOs used to prevent entity exposure

---

## 🧰 Tech Stack

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA
* MySQL 8
* JWT
* Maven
* Lombok
* Swagger (OpenAPI)

---

## 🔗 REST API Endpoints

All endpoints are prefixed with:

```
/api
```

### 🔐 Auth

| Method | Endpoint       | Description          |
| ------ | -------------- | -------------------- |
| POST   | /auth/register | Register new user    |
| POST   | /auth/login    | Login and get tokens |
| POST   | /auth/refresh  | Refresh token        |
| POST   | /auth/logout   | Logout               |

### 👤 Users

| Method | Endpoint  | Description      |
| ------ | --------- | ---------------- |
| GET    | /users/me | Get current user |
| PUT    | /users/me | Update user      |

### 📅 Reservations

| Method | Endpoint           | Description           |
| ------ | ------------------ | --------------------- |
| POST   | /reservations      | Create reservation    |
| GET    | /reservations/{id} | Get reservation by ID |
| GET    | /reservations/my   | Get user reservations |
| DELETE | /reservations/{id} | Cancel reservation    |

### 🍽️ Restaurants & Tables

| Method | Endpoint                           | Description                     |
| ------ | ---------------------------------- | ------------------------------- |
| GET    | /restaurants/{id}/available-tables | Get available tables            |
| GET    | /admin/restaurants                 | Get all restaurants (paginated) |
| POST   | /admin/restaurants                 | Create restaurant (ADMIN)       |
| GET    | /admin/restaurants/{id}/tables     | Get tables (paginated)          |
| POST   | /admin/tables                      | Create table (ADMIN)            |

---

## 🗄️ Database Design

<p align="center">
<img width="811" height="1186" alt="restaurant-reservation" src="https://github.com/user-attachments/assets/c0f98e15-44fa-4a4b-89f0-798de1572bae" />
</p>

---

## 📄 API Documentation

```
http://localhost:8080/swagger-ui/index.html
```

---

## ⚡ Quick Run

```bash
git clone https://github.com/YOUR_USERNAME/restaurant-reservation-system.git
cd restaurant-reservation-system
mvn spring-boot:run
```

---

## ⚠️ Edge Cases Handled

* Concurrent booking requests
* Invalid time ranges
* Over-capacity reservations
* Unauthorized access
* Repeated cancellation

---

## 🚀 Future Improvements

* Redis caching for availability queries
* Rate limiting
* Docker containerization
* CI/CD pipeline
* Cloud deployment (AWS / Render)
* Monitoring & logging (ELK / Grafana)

---

## 👨‍💻 Author

Mahmoud
Backend Developer | Spring Boot
