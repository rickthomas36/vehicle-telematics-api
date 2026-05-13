# 🚗 Vehicle Telematics API

A production-style Spring Boot REST API simulating Ford's connected vehicle telematics platform.
Built to demonstrate automotive backend development skills for Ford Software Engineering roles.

---

## What This Project Does

This API simulates the backend services that power **FordPass** — Ford's connected vehicle app.
When you tap "Remote Lock" or check your F-150 Lightning battery on your phone, it talks to
a system exactly like this one.

**Core capabilities:**
- Register connected vehicles by VIN
- Ingest real-time telemetry (speed, GPS, battery, fuel, engine health)
- Retrieve live vehicle status (what FordPass shows you)
- Issue and track remote commands (lock, unlock, remote start, charge)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | H2 (dev) / PostgreSQL (prod) |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (Phase 2) |
| Build | Maven |
| Testing | JUnit 5 + Mockito |
| CI/CD | GitHub Actions (coming in Phase 3) |

---

## Getting Started

### Prerequisites
- Java 17+ (`java --version`)
- Maven 3.8+ (`mvn --version`)
- Git

### Run locally (30 seconds)

```bash
# Clone the repo
git clone https://github.com/rickthomas36/vehicle-telematics-api
cd vehicle-telematics-api

# Run it (downloads dependencies automatically)
./mvnw spring-boot:run

# API is live at:
# http://localhost:8080/api/v1/vehicles
# H2 database console: http://localhost:8080/h2-console
```

---

## API Reference

### Register a Vehicle
```bash
POST /api/v1/vehicles
Content-Type: application/json

{
  "vin": "1FTFW1ET5DFC10312",
  "make": "Ford",
  "model": "F-150 Lightning",
  "year": 2024,
  "color": "Antimatter Blue",
  "vehicleType": "EV"
}
```

### Get Vehicle Status (like FordPass)
```bash
GET /api/v1/vehicles/1FTFW1ET5DFC10312/status
```

### Send Telemetry (simulates vehicle heartbeat)
```bash
POST /api/v1/vehicles/1FTFW1ET5DFC10312/telemetry
Content-Type: application/json

{
  "latitude": 42.3314,
  "longitude": -83.0458,
  "speedKph": 105.2,
  "batteryStateOfChargePercent": 73.5,
  "estimatedRangeKm": 210.0,
  "isCharging": false,
  "capturedAt": "2024-01-15T14:30:00"
}
```

### Issue a Remote Command
```bash
POST /api/v1/vehicles/1FTFW1ET5DFC10312/commands
Content-Type: application/json

{
  "commandType": "LOCK",
  "issuedBy": "richard@example.com"
}

# Available commands:
# LOCK, UNLOCK, REMOTE_START, REMOTE_STOP,
# PANIC_ALARM, CHARGE_START, CHARGE_STOP,
# CLIMATE_PRECONDITION, LOCATE
```

### Get Command History
```bash
GET /api/v1/vehicles/1FTFW1ET5DFC10312/commands
```

---

## Project Structure

```
src/main/java/com/ford/telematics/
├── TelematicsApplication.java     ← App entry point
├── controller/
│   ├── VehicleController.java     ← REST endpoints (HTTP layer)
│   └── GlobalExceptionHandler.java← Error handling
├── service/
│   └── VehicleService.java        ← Business logic
├── repository/
│   ├── VehicleRepository.java     ← DB queries for vehicles
│   ├── TelemetryRepository.java   ← DB queries for telemetry
│   └── VehicleCommandRepository.java
├── model/
│   ├── Vehicle.java               ← Vehicle entity
│   ├── TelemetrySnapshot.java     ← Telemetry data entity
│   └── VehicleCommand.java        ← Remote command entity
├── dto/
│   └── VehicleDtos.java           ← Request/response objects
└── security/
    └── SecurityConfig.java        ← Auth config
```

---

## Development Phases

### ✅ Phase 1 (Current) — Core API
- Vehicle registration and management
- Telemetry ingestion and retrieval
- Remote command queue
- Unit tests with Mockito

### 🔲 Phase 2 — Security & Auth
- JWT token authentication
- User registration/login
- Role-based access (vehicle owner vs. fleet manager)
- Rate limiting

### 🔲 Phase 3 — Production Readiness
- Switch to PostgreSQL
- Docker + Docker Compose
- GitHub Actions CI/CD pipeline
- Deploy to AWS (EC2 or ECS)
- API documentation with Swagger/OpenAPI

### 🔲 Phase 4 — Advanced Features
- WebSocket for real-time telemetry streaming
- Geofencing alerts (notify when vehicle leaves area)
- Trip history and analytics
- Low battery / maintenance alerts

---

## Running Tests

```bash
mvn test
```

---

## Author

Richard Seabridge | github.com/rickthomas36
B.S. Computer Science, SNHU 2025
Ford Motor Company — MP&L Associate transitioning to Software Engineering
