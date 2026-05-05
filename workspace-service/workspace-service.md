# Workspace Service — Deep Dive

The **workspace-service** is the organizational backbone of FlowBoard. It manages workspaces (team containers), membership, role assignments, visibility controls, and audit trails. It communicates with **payment-service** for plan entitlements and **board-service** for cascade deletes via inter-service HTTP calls, and publishes events to **Kafka**.

## Key Features

- 🏢 **Workspace CRUD** — Create, read, update, delete workspaces with owner tracking
- 👥 **Membership Management** — Add/remove members, assign workspace-level roles (`ADMIN` / `MEMBER`)
- 🔒 **Visibility Control** — `PUBLIC` (discoverable) or `PRIVATE` (invite-only) workspaces
- 💳 **Payment Entitlement Checks** — Validates subscription status via payment-service before workspace creation
- 📋 **Audit Trail** — Records workspace actions (create, delete, member changes) with actor, target, and details
- 🧹 **Cascade Cleanup** — Calls board-service to delete all boards when a workspace is deleted
- 📡 **Kafka Events** — Publishes workspace lifecycle events for downstream consumers
- ⚡ **Redis Caching** — Caches frequently accessed workspace data for performance
- 🔌 **WebSocket Support** — Real-time workspace updates via STOMP

## Entities

### `workspaces` table

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

### `workspace_members` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `workspace_id` | BIGINT | FK → `workspaces.id`, NOT NULL |
| `user_id` | BIGINT | NOT NULL (references auth-service user) |
| `role` | ENUM | `ADMIN` / `MEMBER` (default: `MEMBER`) |
| `joined_at` | DATETIME | Auto-set on creation |

> **Unique constraint:** `(workspace_id, user_id)` — a user can only be a member once per workspace.

### `workspace_audit_events` table

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

## API Endpoints

### Workspace CRUD (`/api/v1/workspaces`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new workspace |
| `GET` | `/{id}` | Member | Get workspace by ID |
| `GET` | `/my` | Member | Get all workspaces where user is a member |
| `GET` | `/public` | Member | Get all public workspaces |
| `PUT` | `/{id}` | Owner/Admin | Update workspace (name, description, visibility) |
| `DELETE` | `/{id}` | Owner | Delete workspace (cascades to boards) |

### Membership (`/api/v1/workspaces/{id}/members`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/` | Member | List all members of a workspace |
| `POST` | `/` | Admin/Owner | Add a member to the workspace |
| `DELETE` | `/{userId}` | Admin/Owner | Remove a member from the workspace |
| `PUT` | `/{userId}/role` | Admin/Owner | Update a member's role (`ADMIN` / `MEMBER`) |

### Platform Admin (`/api/v1/workspaces/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/all` | Platform Admin | List every workspace on the platform |
| `DELETE` | `/{id}` | Platform Admin | Force-delete any workspace |
| `GET` | `/audit` | Platform Admin | List all workspace audit events |

## Inter-Service Communication

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

## Project Structure

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

## Dependencies

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
