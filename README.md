# Resource Booking System

A RESTful **Resource Booking System** built with **Spring Boot 3 / Java 17**, **Spring Security + JWT**, and
**MySQL** (PostgreSQL also supported) via JPA/Hibernate. Users can browse resources (rooms, vehicles, equipment) and create/manage
their own reservations; administrators have full CRUD control over resources and reservations.

---

## 1. Tech Stack

| Concern            | Technology                                   |
|---------------------|-----------------------------------------------|
| Language / Runtime  | Java 17                                       |
| Framework           | Spring Boot 3.3.4 (Web, Data JPA, Security, Validation) |
| Auth                | Spring Security + JWT (jjwt 0.12.6), BCrypt password hashing |
| Database            | MySQL (PostgreSQL also supported; H2 in-memory for tests) |
| API Docs            | Postman collection (`/postman` folder)        |
| Build Tool          | Maven                                         |
| Testing             | JUnit 5, Spring Boot Test, MockMvc, Mockito   |

---

## 2. Project Structure

```
src/main/java/com/assignment/booking/
├── BookingApplication.java        Entry point
├── config/                        Security and data-seeding configuration
├── controller/                    REST controllers (Auth, Resource, Reservation)
├── dto/                           Request/response DTOs (auth, resource, reservation, common)
├── entity/                        JPA entities (User, Resource, Reservation) + enums
├── exception/                     Custom exceptions + centralized @RestControllerAdvice
├── repository/                    Spring Data JPA repositories
├── security/                      JwtUtil, JWT filter, UserDetails implementation
├── service/                       Business logic layer
└── specification/                 JPA Specifications used for reservation filtering
```

The layering follows: **Controller → Service → Repository**, with DTOs used at the controller boundary so JPA
entities are never exposed directly over the API.

---

## 3. Getting Started

### 3.1 Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8+ installed and running locally (or on a reachable host) — PostgreSQL is also supported, see below

### 3.2 Create the database

Create an empty database for the app to connect to (Hibernate will create the tables automatically via
`ddl-auto=update`). No manual schema/DDL is required.

```bash
mysql -u root -p -e "CREATE DATABASE booking_db;"
```

(If you'd rather use PostgreSQL: `psql -U postgres -c "CREATE DATABASE booking_db;"` — see the config override
at the bottom of the next section.)

### 3.3 Configure environment variables

Copy `.env.example` to `.env` (or export the variables in your shell) and adjust as needed:

```bash
cp .env.example .env
```

| Variable            | Description                                    | Default (MySQL)                                    |
|----------------------|-------------------------------------------------|-----------------------------------------------------|
| `SERVER_PORT`        | HTTP port the app listens on                    | `8080`                                              |
| `DB_URL`              | JDBC connection URL                             | `jdbc:mysql://localhost:3306/booking_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true` |
| `DB_USERNAME`         | Database username                               | `root`                                              |
| `DB_PASSWORD`         | Database password                               | `root`                                              |
| `DB_DRIVER`           | JDBC driver class                               | `com.mysql.cj.jdbc.Driver`                          |
| `DDL_AUTO`            | Hibernate schema strategy                       | `update`                                            |
| `SHOW_SQL`            | Log SQL statements                              | `false`                                             |
| `JWT_SECRET`          | HMAC signing key for JWTs (32+ chars recommended)| *(placeholder — change in production)*              |
| `JWT_EXPIRATION_MS`   | Token lifetime in milliseconds                  | `86400000` (24h)                                    |
| `LOG_LEVEL`           | Log level for application packages              | `INFO`                                              |

To use **PostgreSQL** instead, set:
```
DB_URL=jdbc:postgresql://localhost:5432/booking_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_DRIVER=org.postgresql.Driver
```

### 3.4 Run the application

```bash
mvn spring-boot:run
```

Or build a jar and run it:

```bash
mvn clean package
java -jar target/resource-booking-system-1.0.0.jar
```

The app starts on `http://localhost:8080` (or your configured `SERVER_PORT`). On first boot, `DataSeeder`
automatically creates seed accounts and sample resources (see below) — no manual SQL needed.

### 3.5 Run the tests

```bash
mvn test
```

Tests run against an in-memory H2 database (`src/test/resources/application-test.yml`), so no external database
is required to run the test suite.

---

## 4. Seed Users

The application seeds two accounts on startup (idempotent — it checks for existing usernames first):

| Username | Password    | Role  |
|----------|-------------|-------|
| `admin`  | `Admin@123` | ADMIN |
| `user`   | `User@123`  | USER  |

Passwords are stored BCrypt-hashed; these are the **plaintext** credentials to use when calling `POST /auth/login`.

Three sample resources (a room, a vehicle, and a projector) are also seeded so `GET /resources` returns data
immediately.

---

## 5. API Documentation

API documentation and testing are provided via a **Postman collection** (no Swagger UI in this project).

### 5.0 Postman Collection

A ready-to-import Postman collection and environment are included in the `postman/` folder:

- `postman/Resource-Booking-System.postman_collection.json`
- `postman/Resource-Booking-System.postman_environment.json`

**To use it:**
1. In Postman: **Import** → select both files (or drag them in).
2. Select the **"Resource Booking - Local"** environment from the environment dropdown (top right).
3. Run **Auth → Login as Admin** and **Auth → Login as User** — these automatically save the returned tokens into
   the `adminToken` / `userToken` collection variables via a small test script, so every other request in the
   collection is pre-wired to use the right token.
4. Run any other request in the **Resources** or **Reservations** folders. Requests that create a resource or
   reservation also auto-capture the new `id` into `resourceId` / `reservationId` for later requests.

The collection also includes a few intentionally-failing requests (e.g. "USER tries to Confirm own reservation",
"Create Resource as USER") that demonstrate the RBAC/ownership rules by expecting `403`/`400` responses.

### 5.1 Authentication

**`POST /auth/login`** — public, no token required.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 86400000
}
```

Include the token on every subsequent request:
```
Authorization: Bearer <token>
```

### 5.2 Resources

| Method | Endpoint            | ADMIN | USER | Notes                              |
|--------|----------------------|:-----:|:----:|-------------------------------------|
| GET    | `/resources`          | ✅    | ✅   | Paginated list                     |
| GET    | `/resources/{id}`     | ✅    | ✅   | Single resource                    |
| POST   | `/resources`          | ✅    | ❌   | Create                              |
| PUT    | `/resources/{id}`     | ✅    | ❌   | Full update                         |
| DELETE | `/resources/{id}`     | ✅    | ❌   | Delete                              |

Request body (`POST` / `PUT`):
```json
{
  "name": "Conference Room B",
  "type": "ROOM",
  "description": "6-seat room with whiteboard",
  "available": true
}
```

### 5.3 Reservations

| Method | Endpoint                     | ADMIN | USER            | Notes                                                        |
|--------|-------------------------------|:-----:|:----------------:|----------------------------------------------------------------|
| POST   | `/reservations`                | ✅    | ✅               | Owner is always the authenticated JWT user, never client input |
| GET    | `/reservations`                | ✅ all | ✅ own only      | Supports filtering, pagination, sorting                       |
| GET    | `/reservations/{id}`           | ✅ any | ✅ own only      | 403 if a USER requests someone else's reservation              |
| PATCH  | `/reservations/{id}/status`    | ✅ any status | ✅ CANCELLED only | USER can only cancel their own booking                        |
| PUT    | `/reservations/{id}`           | ✅    | ❌               | Full update (resource, time window, status, price)             |
| DELETE | `/reservations/{id}`           | ✅    | ❌               | Delete                                                          |

Create request body:
```json
{
  "resourceId": 1,
  "startTime": "2026-10-01T09:00:00",
  "endTime": "2026-10-01T10:00:00",
  "price": 49.99
}
```
> Note: there is intentionally **no `userId` field** — the reservation's owner is always taken from the
> authenticated principal (the JWT), so a USER cannot book on another user's behalf even by tampering with the
> request body.

**Filtering & pagination** (all optional, combinable):
```
GET /reservations?status=CONFIRMED&minPrice=10&maxPrice=100&page=0&size=10&sort=price,desc
```

- `status` — one of `PENDING`, `CONFIRMED`, `CANCELLED`
- `minPrice` / `maxPrice` — decimal bounds (inclusive)
- `page` / `size` — standard Spring Data pagination (0-indexed)
- `sort` — e.g. `price,desc` or `startTime,asc`

Status update (owner cancelling their own reservation):
```bash
curl -X PATCH http://localhost:8080/reservations/1/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "CANCELLED"}'
```

---

## 6. Security Notes

- Passwords are hashed with **BCrypt** (`BCryptPasswordEncoder`) — plaintext passwords are never stored.
- Authentication is fully **stateless**: no HTTP session is created; every request is authenticated via the
  `Authorization: Bearer <JWT>` header, validated by `JwtAuthenticationFilter`.
- Authorization is enforced at two levels:
  1. **URL-level RBAC** in `SecurityConfig` (e.g. only `ROLE_ADMIN` may `POST`/`PUT`/`DELETE` `/resources/**`).
  2. **Object-level ownership checks** in `ReservationService` (a USER can only read/cancel *their own*
     reservations; ADMIN bypasses this check).
- JWTs carry the username (subject) and role as claims and are signed with HMAC-SHA256 using `JWT_SECRET`.
- All error responses (validation, auth, not-found, forbidden) return a consistent JSON shape via
  `GlobalExceptionHandler` — see below.

---

## 7. Error Response Format

```json
{
  "timestamp": "2026-08-28T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/reservations",
  "fieldErrors": {
    "price": "Price must not be negative"
  }
}
```

| Status | Scenario                                                        |
|--------|-------------------------------------------------------------------|
| 400    | Validation failure, invalid time window, invalid price range     |
| 401    | Missing/invalid/expired JWT, bad login credentials                |
| 403    | Authenticated but not authorized (wrong role or not the owner)   |
| 404    | Resource/reservation not found                                    |
| 500    | Unexpected server error                                           |

---

## 8. Testing Overview

| Test class                                   | Focus                                                                 |
|-----------------------------------------------|------------------------------------------------------------------------|
| `JwtUtilTest`                                  | Token generation, validation, tampering detection                     |
| `AuthControllerIntegrationTest`                | Login success/failure, validation errors                              |
| `ResourceControllerAuthorizationTest`          | RBAC on resources (USER read-only, ADMIN full CRUD), validation       |
| `ReservationControllerAuthorizationTest`       | Ownership derived from JWT, USER-vs-ADMIN scoping, status transitions, filtering, pagination |
| `ReservationServiceTest`                       | Unit-level business rules (ownership, time-window validation) with mocks |

Run everything with `mvn test`. Tests use an isolated in-memory H2 database and the same `DataSeeder`, so seeded
`admin`/`user` credentials are available in the test context automatically.

---

## 9. Design Decisions

- **Ownership from JWT, not request body** — `ReservationRequest` has no `userId` field; `ReservationService`
  resolves the owner exclusively from the authenticated `CustomUserDetails` principal.
- **JPA Specifications** for reservation filtering keep the query composable (status + price range + user scoping)
  without combinatorial repository methods.
- **DTOs everywhere** — entities are never serialized directly, avoiding accidental exposure of internal fields
  and giving control over validation annotations independent of persistence constraints.
- **Stateless JWT** rather than session-based auth, matching the "RESTful" requirement and allowing horizontal
  scaling without sticky sessions.
- **BigDecimal** for all monetary values (never float/double) to avoid rounding errors.

---

## 10. Troubleshooting

**`Public Key Retrieval is not allowed` on startup (MySQL)**
MySQL 8's default auth plugin (`caching_sha2_password`) needs to exchange an RSA public key when not using SSL,
and the driver blocks this by default for security. Add `allowPublicKeyRetrieval=true` to `DB_URL` (already the
default in `.env.example`), e.g.:
```
DB_URL=jdbc:mysql://localhost:3306/booking_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true
```

**`Communications link failure` / connection refused**
MySQL isn't running, or is on a different host/port than `DB_URL` points to. Confirm with
`mysql -u root -p -e "SELECT 1;"` before starting the app.

**`Access denied for user`**
`DB_USERNAME` / `DB_PASSWORD` don't match your local MySQL credentials — update them in `.env`.

**`Unknown database 'booking_db'`**
Either run `CREATE DATABASE booking_db;` manually, or make sure `createDatabaseIfNotExist=true` is present in
`DB_URL` (it is by default) and that the DB user has permission to create databases.

**Port 8080 already in use**
Set `SERVER_PORT` to a free port, e.g. `SERVER_PORT=8081`.



## Booking conflict prevention

The API prevents overlapping active reservations for the same resource. PENDING and CONFIRMED reservations reserve the resource; CANCELLED reservations do not block a new booking. When an ADMIN updates a reservation, the current reservation is excluded from the overlap check.

________________________________________________________________________________________________
