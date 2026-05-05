# Auth Service — Deep Dive

The **auth-service** is the central identity provider for the entire FlowBoard platform. It handles user registration, login, password resets, profile management, OAuth2 social login, and platform-level administration — all secured with JWT tokens.

## Key Features

- 🔐 **JWT Authentication** — Stateless access + refresh token pair (JJWT library)
- 📧 **OTP Email Verification** — 6-digit codes for registration, login, and password reset (hashed with BCrypt, rate-limited, time-expiring)
- 🌐 **OAuth2 Social Login** — Google (extensible to GitHub) via Spring Security OAuth2 Client
- 👤 **Profile Management** — Full name, username, avatar URL, bio
- 🛡️ **Role-Based Access Control** — `MEMBER`, `BOARD_ADMIN`, `PLATFORM_ADMIN`, `SYSTEM`
- 🏛️ **Admin Panel API** — User listing, role changes, suspend/restore, permanent deletion, platform stats
- 🔗 **Internal Service API** — Unsecured endpoints for inter-service user lookups (cluster-only)
- 📡 **Kafka Event Producer** — Publishes auth events for downstream consumption

## Entities

### `users` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `full_name` | VARCHAR(255) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `username` | VARCHAR(255) | UNIQUE |
| `password_hash` | VARCHAR(255) | Nullable (OAuth2 users) |
| `role` | ENUM | `MEMBER` / `BOARD_ADMIN` / `PLATFORM_ADMIN` / `SYSTEM` |
| `avatar_url` | VARCHAR(1024) | Optional |
| `bio` | VARCHAR(1024) | Optional |
| `provider` | ENUM | `LOCAL` / `GOOGLE` / `GITHUB` |
| `provider_id` | VARCHAR(255) | OAuth2 provider user ID |
| `is_active` | BOOLEAN | Default `true` |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

### `auth_otps` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `email` | VARCHAR(255) | NOT NULL, indexed |
| `purpose` | ENUM | `REGISTRATION` / `LOGIN` / `RESET_PASSWORD` |
| `otp_hash` | VARCHAR(255) | BCrypt hash of the 6-digit code |
| `expires_at` | DATETIME | NOT NULL |
| `attempt_count` | INT | Rate-limiting counter |
| `consumed_at` | DATETIME | Set when OTP is successfully used |
| `pending_full_name` | VARCHAR(255) | Stored during registration flow |
| `pending_username` | VARCHAR(255) | Stored during registration flow |
| `pending_password_hash` | VARCHAR(255) | Stored during registration flow |
| `created_at` | DATETIME | Auto-set |

## API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/register` | Guest | Direct registration (creates account immediately) |
| `POST` | `/register/request-otp` | Guest | Request a 6-digit registration OTP via email |
| `POST` | `/register/verify-otp` | Guest | Verify OTP and create account |
| `POST` | `/login` | Guest | Login with email + password |
| `POST` | `/login/request-otp` | Guest | Request a passwordless sign-in OTP |
| `POST` | `/login/verify-otp` | Guest | Verify sign-in OTP and receive JWT |
| `POST` | `/reset-password/request-otp` | Guest | Request a password reset OTP |
| `POST` | `/reset-password/confirm` | Guest | Confirm password reset with OTP + new password |
| `POST` | `/logout` | Member | Invalidate current session |
| `POST` | `/refresh` | Member | Exchange refresh token for new access token |

### Profile & Users (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/profile` | Member | Get own profile |
| `PUT` | `/profile` | Member | Update own profile (name, username, avatar, bio) |
| `PUT` | `/password` | Member | Change password (requires old + new) |
| `GET` | `/search?q=` | Member | Search users by name/email (for invitations) |
| `GET` | `/users/{userId}` | Member | Get user by ID |

### Admin Panel (`/api/v1/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users` | Platform Admin | List all users |
| `PATCH` | `/users/{userId}/role` | Platform Admin | Change user role |
| `PATCH` | `/users/{userId}/suspend` | Platform Admin | Suspend a user account |
| `PATCH` | `/users/{userId}/restore` | Platform Admin | Restore a suspended account |
| `DELETE` | `/users/{userId}` | Platform Admin | Permanently delete user |
| `GET` | `/stats` | Platform Admin | Platform-wide stats (total, active, admins) |

### Internal — Service-to-Service (`/api/v1/auth/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users/{userId}` | Cluster only | Get user by ID (no auth) |
| `GET` | `/users/username/{username}` | Cluster only | Get user by username (no auth) |

## Authentication Flow

```
┌──────────┐       ┌──────────────┐       ┌──────────┐       ┌───────┐
│  Client  │──────▶│ API Gateway  │──────▶│ Auth Svc │──────▶│ MySQL │
└──────────┘       └──────────────┘       └──────────┘       └───────┘
                                               │
                                               ├──▶ SMTP (OTP emails)
                                               ├──▶ Kafka (auth events)
                                               └──▶ Google OAuth2
```

**OTP-Based Registration Flow:**
1. Client sends `POST /register/request-otp` with `{ fullName, email, username, password }`
2. Auth Service hashes the OTP, stores pending credentials in `auth_otps`, sends 6-digit code via email
3. Client sends `POST /register/verify-otp` with `{ email, otp }`
4. Auth Service verifies the hash, creates the `User` entity, returns JWT access + refresh tokens

## Project Structure

```
auth-service/
├── src/main/java/com/flowboard/auth/
│   ├── config/           # SecurityConfig, OpenAPI, DataInitializer, PaymentClientConfig
│   ├── controller/       # AuthController, AdminController, InternalUserController
│   ├── dto/
│   │   ├── request/      # RegisterRequest, LoginRequest, VerifyOtpRequest, etc.
│   │   └── response/     # AuthResponse (JWT), UserResponse, OtpChallengeResponse
│   ├── entity/           # User, AuthOtp (JPA entities)
│   ├── exception/        # ResourceNotFoundException, custom error handlers
│   ├── kafka/            # AuthEventProducer
│   ├── repository/       # UserRepository, AuthOtpRepository
│   ├── security/         # JwtUtil, JwtAuthenticationFilter, OAuth2 handlers
│   ├── service/          # AuthService interface + impl
│   └── audit/            # Audit logging
├── Dockerfile            # Multi-stage build (Maven → JRE Alpine)
├── pom.xml
└── .env
```

## Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-security` | Authentication & authorization |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-oauth2-client` | Google/GitHub social login |
| `spring-boot-starter-mail` | OTP email dispatch |
| `spring-kafka` | Kafka event publishing |
| `jjwt-api / jjwt-impl / jjwt-jackson` | JWT token generation & validation |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI auto-generation |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery registration |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `lombok` | Boilerplate reduction |
| `spring-dotenv` | `.env` file loading |
