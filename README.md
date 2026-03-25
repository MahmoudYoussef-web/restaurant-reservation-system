# 🍽️ Restaurant Reservation System (Spring Boot)

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-yellow)
![Status](https://img.shields.io/badge/Status-Production--Ready-brightgreen)

A production-grade restaurant reservation backend system built with Spring Boot, designed to handle real-world booking scenarios including authentication, table management, availability search, and concurrency-safe reservations.

---

## 🚀 Key Highlights

* Scalable layered architecture (Controller → Service → Repository)
* JWT Authentication with Access & Refresh Tokens
* Role-Based Authorization (ADMIN / USER)
* Concurrency-safe reservation system (Pessimistic Locking)
* Double booking prevention
* Table availability search based on time range
* Pagination support for scalable APIs
* Global exception handling with standardized responses
* Clean DTO mapping and separation of concerns
* Stateless and secure backend design

---

## 🧠 Production Improvements

The system includes real-world backend safeguards to ensure consistency and reliability:

* Validation of reservation time ranges
* Prevention of overlapping reservations
* Table capacity validation
* Ownership validation (users access only their reservations)
* Safe cancellation handling
* Defensive programming against invalid input
* Database-level locking to prevent race conditions
* Consistent error handling using custom exceptions

---

## 📊 Business Rules

* A user can only access and manage their own reservations
* Reservations cannot overlap on the same table
* Reservation time must be valid (start < end)
* Number of guests must not exceed table capacity
* Only active reservations (PENDING, CONFIRMED) block time slots
* A reservation can only be cancelled once
* Table availability is dynamically calculated based on time range

---

## 🔄 Core Business Flows

* User authenticates using JWT
* User checks available tables for a time range
* User creates reservation
* System validates availability and locks table
* Reservation is stored safely without conflicts
* User can view or cancel their reservations

---

## 🧰 Tech Stack

* Java 21+
* Spring Boot
* Spring Data JPA
* Spring Security
* JWT
* MySQL
* Maven
* Lombok
* Swagger (OpenAPI)

---

## 🏗️ Architecture

Controller → Service → Repository → Entity  
↘ DTO ↔ Mapper ↗

---

## 📁 Project Structure


src/main/java/com/mahmoud/reservation
├── controller
├── service
├── repository
├── dto
├── mapper
├── entity
├── security
├── exception
├── config


---

🔐 Security

JWT Authentication

Access & Refresh Tokens

Role-based authorization (ADMIN / USER)

Stateless session management



---

🔗 REST API Endpoints

All endpoints are prefixed with:

/api


---

🔐 Auth

Method	Endpoint	Description

POST	/auth/register	Register new user
POST	/auth/login	Login and get tokens
POST	/auth/refresh	Refresh token
POST	/auth/logout	Logout



---

👤 Users

Method	Endpoint	Description

GET	/users/me	Get current user
PUT	/users/me	Update user



---

📅 Reservations

Method	Endpoint	Description

POST	/reservations	Create reservation
GET	/reservations/{id}	Get reservation by ID
GET	/reservations/my	Get user reservations
DELETE	/reservations/{id}	Cancel reservation



---

🍽️ Restaurants & Tables

Method	Endpoint	Description

GET	/restaurants/{id}/available-tables	Get available tables by time
GET	/admin/restaurants	Get all restaurants (paginated)
POST	/admin/restaurants	Create restaurant (ADMIN)
GET	/admin/restaurants/{id}/tables	Get tables (paginated)
POST	/admin/tables	Create table (ADMIN)



---
## 🗄️ Database Design

<p align="center">
<img width="811" height="1186" alt="restaurant-reservation" src="https://github.com/user-attachments/assets/24772d79-7a7b-453b-8de0-3fd129ba7ea9" />
</p>

---

📄 API Documentation

http://localhost:8080/swagger-ui/index.html


---

⚡ Quick Run

git clone https://github.com/YOUR_USERNAME/restaurant-reservation-system.git
cd restaurant-reservation-system
mvn spring-boot:run


---

🚀 Future Improvements

Redis caching for availability endpoint

Rate limiting

Docker containerization

Cloud deployment (AWS / Render)

Unit & integration testing

Monitoring & logging (ELK / Grafana)



---

👨‍💻 Author

Mahmoud
Backend Developer | Spring Boot

