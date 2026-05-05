# FlowBoard — Task Management and Collaboration Platform

A Trello-inspired full-stack Kanban platform built with **Java 17 / Spring Boot 3.2** microservices, an **Angular 17+** SPA frontend, and event-driven communication via **Apache Kafka**.

---

## Architecture

### Application Services

| Service | Port | Responsibility |
|---|---|---|
| **auth-service** | 8081 | Authentication (JWT + OAuth2), OTP email verification, user & profile management, admin panel |
| **workspace-service** | 8082 | Workspaces, membership, plan checks via payment-service |
| **board-service** | 8083 | Boards, board members |
| **list-service** | 8084 | Kanban columns, position management |
| **card-service** | 8085 | Cards, drag-drop, activity log, Kafka event producer |
| **comment-service** | 8086 | Threaded comments, file attachments |
| **label-service** | 8087 | Labels, checklists, checklist items |
| **payment-service** | 8089 | Subscription plans, payment status |
| **notification-service** | 8088 | In-app + email notifications, Kafka event consumer |

### Infrastructure Services

| Service | Port | Responsibility |
|---|---|---|
| **api-gateway** | 8080 | Spring Cloud Gateway — single entry point, JWT validation, route forwarding |
| **eureka-server** | 8761 | Netflix Eureka — service discovery & registry |
| **admin-server** | 9090 | Spring Boot Admin — health monitoring dashboard |
| **flowboard-frontend** | 4200 | Angular 17 SPA |

---

## Auth Service — Deep Dive

The **auth-service** is the central identity provider for the entire FlowBoard platform. It handles user registration, login, password resets, profile management, OAuth2 social login, and platform-level administration — all secured with JWT tokens.

### Key Features

- 🔐 **JWT Authentication** — Stateless access + refresh token pair (JJWT library)
- 📧 **OTP Email Verification** — 6-digit codes for registration, login, and password reset (hashed with BCrypt, rate-limited, time-expiring)
- 🌐 **OAuth2 Social Login** — Google (extensible to GitHub) via Spring Security OAuth2 Client
- 👤 **Profile Management** — Full name, username, avatar URL, bio
- 🛡️ **Role-Based Access Control** — `MEMBER`, `BOARD_ADMIN`, `PLATFORM_ADMIN`, `SYSTEM`
- 🏛️ **Admin Panel API** — User listing, role changes, suspend/restore, permanent deletion, platform stats
- 🔗 **Internal Service API** — Unsecured endpoints for inter-service user lookups (cluster-only)
- 📡 **Kafka Event Producer** — Publishes auth events for downstream consumption

### Entities

#### `users` table

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

#### `auth_otps` table

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

### API Endpoints

#### Authentication (`/api/v1/auth`)

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

#### Profile & Users (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/profile` | Member | Get own profile |
| `PUT` | `/profile` | Member | Update own profile (name, username, avatar, bio) |
| `PUT` | `/password` | Member | Change password (requires old + new) |
| `GET` | `/search?q=` | Member | Search users by name/email (for invitations) |
| `GET` | `/users/{userId}` | Member | Get user by ID |

#### Admin Panel (`/api/v1/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users` | Platform Admin | List all users |
| `PATCH` | `/users/{userId}/role` | Platform Admin | Change user role |
| `PATCH` | `/users/{userId}/suspend` | Platform Admin | Suspend a user account |
| `PATCH` | `/users/{userId}/restore` | Platform Admin | Restore a suspended account |
| `DELETE` | `/users/{userId}` | Platform Admin | Permanently delete user |
| `GET` | `/stats` | Platform Admin | Platform-wide stats (total, active, admins) |

#### Internal — Service-to-Service (`/api/v1/auth/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users/{userId}` | Cluster only | Get user by ID (no auth) |
| `GET` | `/users/username/{username}` | Cluster only | Get user by username (no auth) |

### Authentication Flow

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

### Project Structure

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

### Dependencies

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


---

## Workspace Service — Deep Dive

The **workspace-service** is the organizational backbone of FlowBoard. It manages workspaces (team containers), membership, role assignments, visibility controls, and audit trails. It communicates with **payment-service** for plan entitlements and **board-service** for cascade deletes via inter-service HTTP calls, and publishes events to **Kafka**.

### Key Features

- 🏢 **Workspace CRUD** — Create, read, update, delete workspaces with owner tracking
- 👥 **Membership Management** — Add/remove members, assign workspace-level roles (`ADMIN` / `MEMBER`)
- 🔒 **Visibility Control** — `PUBLIC` (discoverable) or `PRIVATE` (invite-only) workspaces
- 💳 **Payment Entitlement Checks** — Validates subscription status via payment-service before workspace creation
- 📋 **Audit Trail** — Records workspace actions (create, delete, member changes) with actor, target, and details
- 🧹 **Cascade Cleanup** — Calls board-service to delete all boards when a workspace is deleted
- 📡 **Kafka Events** — Publishes workspace lifecycle events for downstream consumers
- ⚡ **Redis Caching** — Caches frequently accessed workspace data for performance
- 🔌 **WebSocket Support** — Real-time workspace updates via STOMP

### Entities

#### `workspaces` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | Optional |
| `owner_id` | BIGINT | NOT NULL (references auth-service user) |
| `visibility` | ENUM | `PUBLIC` / `PRIVATE` (default: `PRIVATE`) |
| `logo_url` | VARCHAR(255) | Optional |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

#### `workspace_members` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `workspace_id` | BIGINT | FK → `workspaces.id`, NOT NULL |
| `user_id` | BIGINT | NOT NULL (references auth-service user) |
| `role` | ENUM | `ADMIN` / `MEMBER` (default: `MEMBER`) |
| `joined_at` | DATETIME | Auto-set on creation |

> **Unique constraint:** `(workspace_id, user_id)` — a user can only be a member once per workspace.

#### `workspace_audit_events` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `workspace_id` | BIGINT | NOT NULL |
| `actor_id` | BIGINT | NOT NULL (user who performed the action) |
| `action` | VARCHAR(255) | NOT NULL (e.g. `WORKSPACE_CREATED`, `MEMBER_ADDED`) |
| `target_type` | VARCHAR(255) | Optional (e.g. `MEMBER`, `WORKSPACE`) |
| `target_id` | VARCHAR(255) | Optional |
| `details` | VARCHAR(1000) | Optional (human-readable description) |
| `created_at` | DATETIME | Auto-set |

### API Endpoints

#### Workspace CRUD (`/api/v1/workspaces`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new workspace |
| `GET` | `/{id}` | Member | Get workspace by ID |
| `GET` | `/my` | Member | Get all workspaces where user is a member |
| `GET` | `/public` | Member | Get all public workspaces |
| `PUT` | `/{id}` | Owner/Admin | Update workspace (name, description, visibility) |
| `DELETE` | `/{id}` | Owner | Delete workspace (cascades to boards) |

#### Membership (`/api/v1/workspaces/{id}/members`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/` | Member | List all members of a workspace |
| `POST` | `/` | Admin/Owner | Add a member to the workspace |
| `DELETE` | `/{userId}` | Admin/Owner | Remove a member from the workspace |
| `PUT` | `/{userId}/role` | Admin/Owner | Update a member's role (`ADMIN` / `MEMBER`) |

#### Platform Admin (`/api/v1/workspaces/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/all` | Platform Admin | List every workspace on the platform |
| `DELETE` | `/{id}` | Platform Admin | Force-delete any workspace |
| `GET` | `/audit` | Platform Admin | List all workspace audit events |

### Inter-Service Communication

```
┌──────────────────┐       HTTP        ┌──────────────────┐
│ Workspace Svc    │──────────────────▶│ Payment Svc      │
│ (port 8082)      │  entitlement      │ (port 8089)      │
└──────────────────┘  check            └──────────────────┘
         │
         │             HTTP
         ├────────────────────────────▶┌──────────────────┐
         │         cascade delete      │ Board Svc        │
         │                             │ (port 8083)      │
         │                             └──────────────────┘
         │
         ├──▶ Kafka (workspace events)
         └──▶ Redis (workspace cache)
```

### Project Structure

```
workspace-service/
├── src/main/java/com/flowboard/workspace/
│   ├── config/           # WebSocket, Redis, OpenAPI configuration
│   ├── controller/       # WorkspaceController
│   ├── dto/
│   │   ├── request/      # CreateWorkspaceRequest, AddMemberRequest, UpdateRoleRequest
│   │   └── response/     # WorkspaceResponse, MemberResponse, WorkspaceAuditEventResponse
│   ├── entity/           # Workspace, WorkspaceMember, WorkspaceAuditEvent
│   ├── exception/        # UnauthorizedException, ResourceNotFoundException
│   ├── kafka/            # WorkspaceEventProducer
│   ├── repository/       # WorkspaceRepository, WorkspaceMemberRepository
│   └── service/
│       ├── WorkspaceService.java
│       ├── BoardCleanupClient.java          # HTTP client → board-service
│       ├── PaymentEntitlementClient.java     # HTTP client → payment-service
│       └── impl/                            # WorkspaceServiceImpl
├── Dockerfile
└── pom.xml
```

### Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-data-redis` | Redis caching layer |
| `spring-boot-starter-cache` | Spring Cache abstraction |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-websocket` | WebSocket / STOMP real-time updates |
| `spring-boot-starter-actuator` | Health & metrics endpoints |
| `spring-kafka` | Kafka event publishing |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `jackson-databind` | JSON serialization |
| `lombok` | Boilerplate reduction |


---

## Board Service — Deep Dive

The **board-service** manages Kanban boards within workspaces. It handles board creation, visibility, closing/archiving, board-level membership with granular roles (`OBSERVER`, `MEMBER`, `ADMIN`), audit logging, and cascade cleanup of downstream lists and cards when a board is deleted.

### Key Features

- 📋 **Board CRUD** — Create, read, update, close, and delete boards within a workspace
- 🎨 **Custom Backgrounds** — Boards support custom background colors (default `#0079BF`)
- 🔒 **Visibility Control** — `PUBLIC` or `PRIVATE` boards within a workspace
- 📦 **Board Closing** — Soft-close boards (`isClosed=true`) without deleting data
- 👥 **Board Membership** — Add/remove members with 3 roles: `OBSERVER`, `MEMBER`, `ADMIN`
- 📋 **Audit Trail** — Records board actions with actor, target, and details
- 🧹 **Cascade Cleanup** — Calls list-service and card-service to delete children when a board is deleted
- 🔗 **Internal API** — Workspace-service can trigger bulk board deletion via internal endpoint
- 📡 **Kafka Events** — Publishes board lifecycle events for downstream consumers
- ⚡ **Redis Caching** — Caches board data for performance

### Entities

#### `boards` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | Optional |
| `workspace_id` | BIGINT | NOT NULL (references workspace-service) |
| `created_by_id` | BIGINT | NOT NULL (user who created the board) |
| `background` | VARCHAR(255) | Default `#0079BF` |
| `visibility` | ENUM | `PUBLIC` / `PRIVATE` (default: `PRIVATE`) |
| `is_closed` | BOOLEAN | Default `false` |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

#### `board_members` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `board_id` | BIGINT | FK → `boards.id`, NOT NULL |
| `user_id` | BIGINT | NOT NULL |
| `role` | ENUM | `OBSERVER` / `MEMBER` / `ADMIN` (default: `MEMBER`) |
| `added_at` | DATETIME | Auto-set on creation |

> **Unique constraint:** `(board_id, user_id)` — a user can only be added once per board.

#### `board_audit_events` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `board_id` | BIGINT | NOT NULL |
| `actor_id` | BIGINT | NOT NULL |
| `action` | VARCHAR(255) | NOT NULL (e.g. `BOARD_CREATED`, `BOARD_CLOSED`) |
| `target_type` | VARCHAR(255) | Optional |
| `target_id` | VARCHAR(255) | Optional |
| `details` | VARCHAR(1000) | Optional |
| `created_at` | DATETIME | Auto-set |

### API Endpoints

#### Board CRUD (`/api/v1/boards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new board in a workspace |
| `GET` | `/{id}` | Member | Get board by ID |
| `GET` | `/workspace/{workspaceId}` | Member | Get all boards in a workspace |
| `GET` | `/my` | Member | Get boards where user is a member |
| `PUT` | `/{id}` | Board Admin | Update board (name, description, visibility, background) |
| `PATCH` | `/{id}/close` | Board Admin | Close (archive) a board |
| `DELETE` | `/{id}` | Board Admin | Delete board (cascades to lists & cards) |

#### Board Membership (`/api/v1/boards/{id}/members`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/` | Member | List all board members |
| `POST` | `/` | Board Admin | Add a member with role (`OBSERVER` / `MEMBER` / `ADMIN`) |
| `DELETE` | `/{userId}` | Board Admin | Remove a member |
| `PUT` | `/{userId}/role` | Board Admin | Update a member's role |

#### Platform Admin (`/api/v1/boards/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/all` | Platform Admin | List every board on the platform |
| `PATCH` | `/{id}/close` | Platform Admin | Force-close any board |
| `DELETE` | `/{id}` | Platform Admin | Force-delete any board |
| `GET` | `/audit` | Platform Admin | List all board audit events |

#### Internal — Service-to-Service (`/api/v1/boards/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `DELETE` | `/workspace/{workspaceId}?actorId=` | Cluster only | Delete all boards in a workspace (called by workspace-service) |

### Inter-Service Communication

```
┌──────────────────┐                   ┌──────────────────┐
│ Workspace Svc    │── HTTP cascade ──▶│ Board Svc        │
│ (port 8082)      │   delete          │ (port 8083)      │
└──────────────────┘                   └──────────────────┘
                                              │
                                              ├── HTTP ──▶ List Svc (port 8084)
                                              │            cascade delete lists
                                              │
                                              ├── HTTP ──▶ Card Svc (port 8085)
                                              │            cascade delete cards
                                              │
                                              ├──▶ Kafka (board events)
                                              └──▶ Redis (board cache)
```
 
### Project Structure

```
board-service/
├── src/main/java/com/flowboard/board/
│   ├── config/           # WebSocket, Redis, OpenAPI configuration
│   ├── controller/       # BoardController
│   ├── dto/
│   │   ├── request/      # CreateBoardRequest, AddBoardMemberRequest, UpdateBoardMemberRoleRequest
│   │   └── response/     # BoardResponse, BoardMemberResponse, BoardAuditEventResponse
│   ├── entity/           # Board, BoardMember, BoardAuditEvent
│   ├── exception/        # AccessDeniedException, ResourceNotFoundException
│   ├── kafka/            # BoardEventProducer
│   ├── repository/       # BoardRepository, BoardMemberRepository
│   └── service/
│       ├── BoardService.java
│       ├── ListCleanupClient.java     # HTTP client → list-service
│       ├── CardCleanupClient.java     # HTTP client → card-service
│       └── impl/                      # BoardServiceImpl
├── Dockerfile
└── pom.xml
```

### Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-data-redis` | Redis caching layer |
| `spring-boot-starter-cache` | Spring Cache abstraction |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-websocket` | WebSocket / STOMP support |
| `spring-boot-starter-actuator` | Health & metrics endpoints |
| `spring-kafka` | Kafka event publishing |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `jackson-databind` | JSON serialization |
| `lombok` | Boilerplate reduction |

---

## List Service — Deep Dive

The **list-service** manages the Kanban columns (lists) within boards. It handles list creation, position ordering, drag-drop reordering, archiving, cross-board moves, and cascade cleanup when a board is deleted. Each list tracks its position for consistent column ordering on the frontend.

### Key Features

- 📝 **List CRUD** — Create, read, update, and delete Kanban columns within a board
- 🔢 **Position Management** — Integer-based ordering ensures consistent column layout
- 🔄 **Drag-Drop Reorder** — Bulk reorder endpoint accepts an ordered list of IDs
- 📦 **Archive / Unarchive** — Soft-archive lists (`isArchived=true`) without data loss
- 🎨 **Custom Colors** — Lists support custom color values (default `#E2E4E9`)
- ↔️ **Cross-Board Move** — Move a list from one board to another
- 🧹 **Cascade Cleanup** — Calls card-service to delete cards when a list is deleted
- 🔗 **Internal API** — Board-service can trigger bulk list deletion via internal endpoint
- ⚡ **Redis Caching** — Caches list data for performance

### Entity

#### `task_lists` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `board_id` | BIGINT | NOT NULL (references board-service) |
| `position` | INT | NOT NULL (0-indexed column order) |
| `color` | VARCHAR(255) | Default `#E2E4E9` |
| `is_archived` | BOOLEAN | Default `false` |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

### API Endpoints

#### List CRUD (`/api/v1/lists`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new list in a board |
| `GET` | `/{id}` | Member | Get list by ID |
| `GET` | `/board/{boardId}` | Member | Get all active lists in a board (ordered by position) |
| `GET` | `/board/{boardId}/archived` | Member | Get archived lists in a board |
| `PUT` | `/{id}` | Member | Update list (name, color) |
| `DELETE` | `/{id}` | Member | Delete list (cascades to cards) |

#### Position & Movement (`/api/v1/lists`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `PUT` | `/board/{boardId}/reorder` | Member | Reorder lists — accepts `[id1, id2, id3, ...]` |
| `PATCH` | `/{id}/archive` | Member | Archive a list |
| `PATCH` | `/{id}/unarchive` | Member | Unarchive a list |
| `PATCH` | `/{id}/move` | Member | Move list to a different board |

#### Internal — Service-to-Service (`/api/v1/lists/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `DELETE` | `/board/{boardId}` | Cluster only | Delete all lists in a board (called by board-service) |

### Inter-Service Communication

```
┌──────────────────┐                   ┌──────────────────┐
│ Board Svc        │── HTTP cascade ──▶│ List Svc         │
│ (port 8083)      │   delete          │ (port 8084)      │
└──────────────────┘                   └──────────────────┘
                                              │
                                              ├── HTTP ──▶ Card Svc (port 8085)
                                              │            cascade delete cards
                                              │
                                              └──▶ Redis (list cache)
```

### Project Structure

```
list-service/
├── src/main/java/com/flowboard/list/
│   ├── config/           # Redis, OpenAPI, WebSocket configuration
│   ├── controller/       # ListController
│   ├── dto/
│   │   ├── request/      # CreateListRequest, MoveListRequest
│   │   └── response/     # ListResponse
│   ├── entity/           # TaskList
│   ├── exception/        # ResourceNotFoundException
│   ├── kafka/            # (empty — no events published yet)
│   ├── repository/       # TaskListRepository
│   └── service/
│       ├── ListService.java
│       ├── CardCleanupClient.java     # HTTP client → card-service
│       └── impl/                      # ListServiceImpl
├── Dockerfile
└── pom.xml
```

### Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-data-redis` | Redis caching layer |
| `spring-boot-starter-cache` | Spring Cache abstraction |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-websocket` | WebSocket / STOMP support |
| `spring-boot-starter-actuator` | Health & metrics endpoints |
| `spring-kafka` | Kafka (included but not yet producing events) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `jackson-databind` | JSON serialization |
| `lombok` | Boilerplate reduction |

---

## Card Service — Deep Dive

The **card-service** is the core work-item engine of FlowBoard. It manages cards (tasks) within lists, including creation, assignment, priority/status updates, drag-drop movement between lists, reordering, archiving, activity logging, and Kafka event publishing for notifications.

### Key Features

- 🃏 **Card CRUD** — Create, read, update, and delete task cards within lists
- 🔢 **Position & Reorder** — Integer-based positioning with bulk reorder endpoint
- ↔️ **Cross-List Move** — Move cards between lists with position targeting
- 👤 **Assignee Management** — Assign/reassign cards to team members
- ⚡ **Priority Levels** — `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- 📊 **Status Tracking** — `TO_DO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`
- 📅 **Due & Start Dates** — Track timelines with overdue detection
- 🎨 **Cover Colors** — Visual card customization
- 📦 **Archive / Unarchive** — Soft-archive cards without data loss
- 📋 **Activity Log** — Records every field change with old/new values and actor
- 📡 **Kafka Events** — Publishes `card.assigned` and `card.moved` events for notification-service
- 🔗 **Internal API** — Board-service and list-service can trigger bulk card deletion
- ⚡ **Redis Caching** — Caches card data for performance

### Entities

#### `cards` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `list_id` | BIGINT | NOT NULL (references list-service) |
| `board_id` | BIGINT | NOT NULL (references board-service) |
| `title` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | Optional |
| `position` | INT | NOT NULL (0-indexed order within list) |
| `priority` | ENUM | `LOW` / `MEDIUM` / `HIGH` / `CRITICAL` (default: `MEDIUM`) |
| `status` | ENUM | `TO_DO` / `IN_PROGRESS` / `IN_REVIEW` / `DONE` (default: `TO_DO`) |
| `due_date` | DATE | Optional |
| `start_date` | DATE | Optional |
| `assignee_id` | BIGINT | Optional (references auth-service user) |
| `created_by_id` | BIGINT | User who created the card |
| `is_archived` | BOOLEAN | Default `false` |
| `cover_color` | VARCHAR(255) | Optional |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

#### `card_activities` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL |
| `actor_id` | BIGINT | NOT NULL (user who made the change) |
| `action` | VARCHAR(255) | NOT NULL (e.g. `CARD_CREATED`, `STATUS_CHANGED`) |
| `field_name` | VARCHAR(255) | Optional (e.g. `status`, `assigneeId`) |
| `old_value` | VARCHAR(255) | Optional (previous value) |
| `new_value` | VARCHAR(255) | Optional (new value) |
| `created_at` | DATETIME | Auto-set |

### API Endpoints

#### Card CRUD (`/api/v1/cards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new card in a list |
| `GET` | `/{id}` | Member | Get card by ID |
| `GET` | `/list/{listId}` | Member | Get all cards in a list |
| `GET` | `/board/{boardId}` | Member | Get all cards in a board |
| `GET` | `/assignee/{userId}` | Member | Get all cards assigned to a user |
| `GET` | `/overdue` | Member | Get all overdue cards |
| `PUT` | `/{id}` | Member | Update card (title, description, dates, etc.) |
| `DELETE` | `/{id}` | Member | Delete card |

#### Movement & Ordering (`/api/v1/cards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `PATCH` | `/{id}/move` | Member | Move card to a different list with position |
| `PUT` | `/list/{listId}/reorder` | Member | Reorder cards — accepts `[id1, id2, ...]` |
| `PATCH` | `/{id}/archive` | Member | Archive a card |
| `PATCH` | `/{id}/unarchive` | Member | Unarchive a card |

#### Field Updates (`/api/v1/cards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `PATCH` | `/{id}/assignee` | Member | Set/change card assignee |
| `PATCH` | `/{id}/priority` | Member | Set card priority |
| `PATCH` | `/{id}/status` | Member | Set card status |

#### Activity Log (`/api/v1/cards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/{id}/activity` | Member | Get activity log for a card |
| `GET` | `/admin/activity` | Platform Admin | Get all card activity across the platform |
| `GET` | `/admin/all` | Platform Admin | List every card on the platform |

#### Internal — Service-to-Service (`/api/v1/cards/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `DELETE` | `/board/{boardId}` | Cluster only | Delete all cards in a board (called by board-service) |
| `DELETE` | `/list/{listId}` | Cluster only | Delete all cards in a list (called by list-service) |

### Kafka Topics

| Topic | Payload | Triggered When |
|---|---|---|
| `flowboard.card.assigned` | `{ cardId, assigneeId, actorId }` | Card is assigned to a user |
| `flowboard.card.moved` | `{ cardId, fromListId, toListId, actorId }` | Card is moved between lists |

> These events are consumed by **notification-service** to send email/in-app notifications.

### Inter-Service Communication

```
┌──────────────────┐                   ┌──────────────────┐
│ Board Svc        │── HTTP cascade ──▶│                  │
│ (port 8083)      │   delete          │                  │
└──────────────────┘                   │   Card Svc       │
                                       │   (port 8085)    │
┌──────────────────┐                   │                  │
│ List Svc         │── HTTP cascade ──▶│                  │
│ (port 8084)      │   delete          └──────────────────┘
└──────────────────┘                          │
                                              ├──▶ Kafka (card.assigned, card.moved)
                                              └──▶ Redis (card cache)
```

### Project Structure

```
card-service/
├── src/main/java/com/flowboard/card/
│   ├── config/           # Redis, OpenAPI, WebSocket configuration
│   ├── controller/       # CardController
│   ├── dto/
│   │   ├── request/      # CreateCardRequest, MoveCardRequest, AssignCardRequest,
│   │   │                 # UpdatePriorityRequest, UpdateStatusRequest
│   │   └── response/     # CardResponse
│   ├── entity/           # Card, CardActivity
│   ├── exception/        # ResourceNotFoundException
│   ├── kafka/            # CardEventProducer (card.assigned, card.moved)
│   ├── repository/       # CardRepository, CardActivityRepository
│   └── service/
│       ├── CardService.java
│       └── impl/         # CardServiceImpl
├── Dockerfile
└── pom.xml
```

### Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-data-redis` | Redis caching layer |
| `spring-boot-starter-cache` | Spring Cache abstraction |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-websocket` | WebSocket / STOMP support |
| `spring-boot-starter-actuator` | Health & metrics endpoints |
| `spring-kafka` | Kafka event publishing (`card.assigned`, `card.moved`) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `jackson-databind` | JSON serialization (Kafka payloads) |
| `lombok` | Boilerplate reduction |

---

## Comment Service — Deep Dive

The **comment-service** handles discussion and file collaboration on cards. It supports threaded comments (with replies), soft-delete, file attachments (both metadata-only and physical upload), and file retrieval — making it the collaboration hub for each card.

### Key Features

- 💬 **Threaded Comments** — Top-level comments and nested replies via `parentCommentId`
- ✏️ **Edit & Delete** — Authors can edit their own comments; soft-delete with `isDeleted=true`
- 🔢 **Comment Count** — Lightweight endpoint to get comment count per card
- 📎 **Attachment Metadata** — Register external file URLs with metadata (name, type, size)
- 📤 **Physical File Upload** — Multipart file upload with local storage + auto-generated metadata
- 📥 **File Retrieval** — Serve uploaded files inline via `/api/v1/files/{filename}`
- 🔌 **WebSocket Support** — Real-time comment broadcast via STOMP

### Entities

#### `comments` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL (references card-service) |
| `author_id` | BIGINT | NOT NULL (references auth-service user) |
| `content` | TEXT | NOT NULL |
| `parent_comment_id` | BIGINT | Optional (NULL = top-level, set = reply) |
| `is_deleted` | BOOLEAN | Default `false` |
| `created_at` | DATETIME | Auto-set on creation |
| `updated_at` | DATETIME | Auto-set on update |

#### `attachments` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL |
| `uploader_id` | BIGINT | NOT NULL |
| `file_name` | VARCHAR(255) | NOT NULL (original filename) |
| `file_url` | VARCHAR(255) | NOT NULL (storage path or external URL) |
| `file_type` | VARCHAR(255) | Optional (MIME type, e.g. `image/png`) |
| `size_kb` | BIGINT | Optional (file size in KB) |
| `uploaded_at` | DATETIME | Auto-set on creation |

### API Endpoints

#### Comments (`/api/v1`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/cards/{cardId}/comments` | Member | Add a comment (or reply via `parentCommentId`) |
| `GET` | `/cards/{cardId}/comments` | Member | Get top-level comments for a card |
| `GET` | `/comments/{id}/replies` | Member | Get replies to a specific comment |
| `PUT` | `/comments/{id}` | Author | Edit a comment |
| `DELETE` | `/comments/{id}` | Author | Soft-delete a comment |
| `GET` | `/cards/{cardId}/comments/count` | Member | Get comment count for a card |

#### Attachments (`/api/v1`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/cards/{cardId}/attachments` | Member | Add attachment metadata (external URL) |
| `GET` | `/cards/{cardId}/attachments` | Member | List all attachments for a card |
| `DELETE` | `/attachments/{id}` | Uploader | Delete an attachment |
| `POST` | `/cards/{cardId}/attachments/upload` | Member | Upload a physical file (multipart) |
| `GET` | `/files/{filename}` | Member | Retrieve/download an uploaded file |

### Project Structure

```
comment-service/
├── src/main/java/com/flowboard/comment/
│   ├── config/           # OpenAPI, WebSocket configuration
│   ├── controller/       # CommentController
│   ├── dto/
│   │   └── request/      # CreateCommentRequest, CreateAttachmentRequest
│   ├── entity/           # Comment, Attachment
│   ├── exception/        # ResourceNotFoundException
│   ├── kafka/            # (placeholder for future events)
│   ├── repository/       # CommentRepository, AttachmentRepository
│   └── service/
│       ├── CommentService.java
│       ├── FileStorageService.java    # Local file storage (store/load)
│       └── impl/                      # CommentServiceImpl
├── Dockerfile
└── pom.xml
```

### Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API + multipart file upload |
| `spring-boot-starter-data-jpa` | Database access (Hibernate + MySQL) |
| `spring-boot-starter-validation` | Request body validation |
| `spring-boot-starter-websocket` | WebSocket / STOMP support |
| `spring-boot-starter-actuator` | Health & metrics endpoints |
| `spring-kafka` | Kafka (included, not yet publishing) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-admin-starter-client` | Health monitoring |
| `mysql-connector-j` | MySQL JDBC driver |
| `jackson-databind` | JSON serialization |
| `lombok` | Boilerplate reduction |

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local dev without Docker)
- Node 20 + Angular CLI (for local Angular dev)

### 1. Clone and configure
```bash
git clone https://github.com/your-org/flowboard.git
cd flowboard
cp .env.example .env
# Edit .env with your credentials
```

### 2. Start everything with Docker Compose
```bash
docker-compose up --build
```

Services available:
- Angular Frontend: http://localhost:4200
- Auth Service Swagger: http://localhost:8081/swagger-ui.html
- Card Service Swagger: http://localhost:8085/swagger-ui.html
- Eureka Dashboard: http://localhost:8761
- Spring Boot Admin: http://localhost:9090
- SonarQube: http://localhost:9000

### 3. Local development (without Docker)

**Backend:**
```bash
# Start infrastructure only
docker-compose up mysql redis kafka zookeeper -d

# Run each service
cd auth-service && mvn spring-boot:run
cd card-service  && mvn spring-boot:run
# etc.
```

**Frontend:**
```bash
cd flowboard-frontend
npm install
ng serve
# App runs at http://localhost:4200
```

---

## API Documentation

Each service exposes Swagger UI at `/swagger-ui.html`:

| Service | Swagger URL |
|---|---|
| Auth | http://localhost:8081/swagger-ui.html |
| Workspace | http://localhost:8082/swagger-ui.html |
| Board | http://localhost:8083/swagger-ui.html |
| List | http://localhost:8084/swagger-ui.html |
| Card | http://localhost:8085/swagger-ui.html |
| Comment | http://localhost:8086/swagger-ui.html |
| Label | http://localhost:8087/swagger-ui.html |
| Notification | http://localhost:8088/swagger-ui.html |
| Payment | http://localhost:8089/swagger-ui.html |

All endpoints require `Authorization: Bearer <JWT>` except public auth routes (`/register`, `/login`, OTP endpoints).

---

## Running Tests

```bash
# Run all tests with coverage
mvn clean verify

# SonarQube analysis
mvn clean verify sonar:sonar \
  -Dsonar.host.url=${SONAR_HOST_URL:-http://localhost:9000} \
  -Dsonar.token=${SONAR_TOKEN}
```

JaCoCo writes per-module reports to `target/site/jacoco/` and an aggregate XML report for SonarQube to `target/site/jacoco-aggregate/jacoco.xml`.

---

## Environment Variables

See `.env.example` for all required variables. Key ones:

| Variable | Description |
|---|---|
| `JWT_SECRET` | 256-bit base64-encoded secret for signing tokens |
| `DB_*` | MySQL connection details (host, port, name, user, password) |
| `REDIS_*` | Redis connection details |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 credentials |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP server for OTP emails |
| `AUTH_BASE_URL` | Public base URL for OAuth callbacks, e.g. `http://localhost:8081` |

---

## Key Domain Rules

- **Card status**: `TO_DO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`
- **Card priority**: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- **Overdue**: `dueDate < today AND status != DONE`
- **Soft delete**: Cards/lists set `isArchived=true`; hard delete only on archived items
- **Real-time**: WebSocket (STOMP) broadcasts card moves and comment additions
- **Async events**: Kafka topics handle notification dispatch
- **OTP Security**: Codes are BCrypt-hashed, time-limited, and rate-limited by attempt count

---

## Technology Stack

**Backend:** Java 17, Spring Boot 3.2, Spring Security (JWT + OAuth2), Spring Data JPA, Spring Kafka, Spring WebSocket, Spring Cloud Gateway, Netflix Eureka  
**Frontend:** Angular 17+, NgRx, Angular Material, CDK Drag-Drop, Tailwind CSS, StompJS  
**Infrastructure:** MySQL 8, Redis 7, Apache Kafka + Zookeeper, SonarQube, Docker, Docker Compose  
**Testing:** Mockito, JUnit 5, JaCoCo coverage, Spring Security Test  
**Monitoring:** Spring Boot Admin, Eureka Dashboard, Swagger/OpenAPI (SpringDoc)
