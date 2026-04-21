# 💊 Capsule Pharmacy — Microservices Healthcare Platform

A production-grade Spring Boot microservices system for online pharmacy and doctor appointment management.

---

## 🏗️ Architecture Overview

```
                        ┌─────────────────────────────┐
                        │   Eureka Service Registry    │
                        │      (Port 8761)             │
                        └─────────────┬───────────────┘
                                      │ (all services register)
                        ┌─────────────▼───────────────┐
                        │       API Gateway            │
    Client ────────────▶│   JWT Auth + Routing         │
    (port 8080)         │      (Port 8080)             │
                        └─────────────┬───────────────┘
          ┌──────────────┬────────────┼───────────┬──────────────┐
          ▼              ▼            ▼           ▼              ▼
   ┌─────────┐   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
   │  User   │   │  Doctor  │ │ Patient  │ │ Booking  │ │ Payment  │
   │ Service │   │ Service  │ │ Service  │ │ Service  │ │ Service  │
   │  :8081  │   │  :8082   │ │  :8084   │ │  :8083   │ │  :8087   │
   └─────────┘   └──────────┘ └──────────┘ └────┬─────┘ └────┬─────┘
                                                  │ Kafka       │ Kafka
                        ┌─────────────────────────┼─────────────┘
                        ▼              ▼           ▼
                   ┌──────────┐ ┌──────────┐ ┌──────────────┐
                   │ Pharmacy │ │Notif.    │ │ Config       │
                   │ Service  │ │ Service  │ │ Server       │
                   │  :8085   │ │  :8086   │ │  :8888       │
                   └──────────┘ └──────────┘ └──────────────┘
                   AWS S3 + OpenAI  Email SMTP
```

---

## 📦 Services

| Service | Port | Eureka Name | Description |
|---|---|---|---|
| **eureka-server** | 8761 | EUREKA-SERVER | Service registry & discovery |
| **config-server** | 8888 | CONFIG-SERVER | Centralised configuration |
| **api-gateway** | 8080 | API-GATEWAY | JWT auth, routing, load balancing |
| **user-service** | 8081 | USER-LOGIN-SIGNUP | Register, login, JWT issuance |
| **doctor-service** | 8082 | DOCTOR-SERVICE | Doctor profiles & availability |
| **booking-service** | 8083 | BOOKING-SERVICE | Appointments + reminder scheduler |
| **patient-service** | 8084 | PATIENT-SERVICE | Patient profiles & medical history |
| **pharmacy-service** | 8085 | PHARMACY-SERVICE | Medicines, prescriptions, cart, orders |
| **notification-service** | 8086 | NOTIFICATION-SERVICE | Email notifications via Kafka |
| **payment-service** | 8087 | PAYMENT-SERVICE | Order payment processing |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Apache Kafka 3.x (or Docker Compose)
- Docker (optional)

### Run with Docker Compose (Recommended)

```bash
# Set env variables
export OPENAI_API_KEY=your-key
export AWS_ACCESS_KEY=your-key
export AWS_SECRET_KEY=your-key
export MAIL_USERNAME=your-gmail
export MAIL_PASSWORD=your-app-password

# Start everything
docker-compose up -d

# Check status
docker-compose ps
```

### Run Manually (startup order matters!)

```bash
# 1. Start MySQL and Kafka first

# 2. eureka-server
cd eureka-server && mvn spring-boot:run

# 3. config-server
cd config-server && mvn spring-boot:run

# 4. api-gateway
cd api-gateway && mvn spring-boot:run

# 5. All other services (in any order)
cd user-service      && mvn spring-boot:run &
cd doctor-service    && mvn spring-boot:run &
cd patient-service   && mvn spring-boot:run &
cd booking-service   && mvn spring-boot:run &
cd pharmacy-service  && mvn spring-boot:run &
cd payment-service   && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
```

---

## 🔑 API Usage

### 1. Register & Login
```http
POST http://localhost:8080/api/auth/register
{
  "fullName": "John Patient",
  "email": "john@test.com",
  "password": "password123",
  "role": "PATIENT"
}

POST http://localhost:8080/api/auth/login
→ Returns { "token": "Bearer eyJ..." }
```

### 2. Browse Medicines (public)
```http
GET http://localhost:8080/api/medicines
GET http://localhost:8080/api/medicines/search?q=amoxicillin
GET http://localhost:8080/api/medicines/category/ANTIBIOTIC
```

### 3. Upload Prescription (requires JWT)
```http
POST http://localhost:8080/api/prescriptions/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data
file: prescription.jpg
→ Returns extracted medicine names from OpenAI Vision API
```

### 4. Add to Cart & Place Order
```http
POST http://localhost:8080/api/cart/add?medicineId=1&quantity=2
POST http://localhost:8080/api/orders/place?deliveryAddress=...&city=...&pincode=...
→ Kafka event → Payment Service → payment-completed → Pharmacy confirms
```

### 5. Book Appointment
```http
POST http://localhost:8080/api/appointments/book
  ?doctorId=1
  &appointmentTime=2025-06-01T10:00:00
  &patientEmail=john@test.com
  &patientName=John
→ Kafka event → Notification Service sends confirmation email
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2, Spring Cloud 2023 |
| Security | Spring Security, JWT (JJWT 0.12) |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Inter-service Comm | OpenFeign + Resilience4j Circuit Breaker |
| Event Streaming | Apache Kafka |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| AI Integration | OpenAI GPT-4 Vision API |
| File Storage | AWS S3 |
| Email | Spring Mail (SMTP/Gmail) |
| Containerization | Docker + Docker Compose |
| Orchestration | Kubernetes (K8s) |
| CI/CD | Jenkins |
| IaC | Terraform + Ansible |
| Cloud | AWS EC2, S3 |
| Testing | JUnit 5, Mockito, JaCoCo (80%+ coverage) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven 3.8 |

---

## 📐 Design Patterns

### Saga Pattern (Choreography)
Order placement uses Kafka-based Saga:
```
Patient places order
  → Pharmacy publishes order-placed-topic
  → Payment Service processes payment
  → Payment publishes payment-completed-topic (SUCCESS/FAILED)
  → Pharmacy confirms order OR cancels (compensating transaction)
  → Notification Service sends email
```

### Circuit Breaker (Resilience4j)
- Booking → Doctor Service: circuit breaker on getDoctorById
- Pharmacy → Payment Service: circuit breaker on payment calls
- States: CLOSED → OPEN (after 50% failure rate) → HALF_OPEN → CLOSED
- Monitor: `GET /actuator/circuitbreakers`

---

## 📊 Monitoring Endpoints

| Service | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| Kafka UI | http://localhost:9093 |
| Swagger (any service) | http://localhost:{port}/swagger-ui.html |
| Health | http://localhost:{port}/actuator/health |
| Circuit Breakers | http://localhost:8083/actuator/circuitbreakers |
| Circuit Breaker Events | http://localhost:8083/actuator/circuitbreakerevents |

---

## 🗃️ Database Schema (per service)

| DB | Tables |
|---|---|
| user_db | users |
| doctor_db | doctors |
| patient_db | patients |
| booking_db | appointments |
| pharmacy_db | medicines, prescriptions, carts, cart_items, orders, order_items |
| payment_db | payments |
| notification_db | notifications |

---

## 🔒 Security

- All endpoints protected by JWT (except /api/auth/**, GET /api/medicines/**, Swagger)
- RBAC: PATIENT / DOCTOR / ADMIN roles
- JWT validated at API Gateway + each service (defense in depth)
- Prescription images stored in private S3 bucket with pre-signed URLs
- Passwords hashed with BCrypt

---

## 🧪 Running Tests

```bash
# Run all tests with coverage report
mvn test

# View JaCoCo coverage report
open target/site/jacoco/index.html
```

---

*Built with ❤️ — Sandesh Yadav | github.com/sandeshyadav19114/capsule-pharmacy*
# Capsule-Pharmacy-Microservices-Healthcare-Platform
