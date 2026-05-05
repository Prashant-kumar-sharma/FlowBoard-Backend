# Board Service — Deep Dive

The **board-service** manages Kanban boards within workspaces. It handles board creation, visibility, closing/archiving, board-level membership with granular roles (`OBSERVER`, `MEMBER`, `ADMIN`), audit logging, and cascade cleanup of downstream lists and cards when a board is deleted.

## Key Features

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

## Entities

### `boards` table

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

### `board_members` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `board_id` | BIGINT | FK → `boards.id`, NOT NULL |
| `user_id` | BIGINT | NOT NULL |
| `role` | ENUM | `OBSERVER` / `MEMBER` / `ADMIN` (default: `MEMBER`) |
| `added_at` | DATETIME | Auto-set on creation |

> **Unique constraint:** `(board_id, user_id)` — a user can only be added once per board.

### `board_audit_events` table

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

## API Endpoints

### Board CRUD (`/api/v1/boards`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new board in a workspace |
| `GET` | `/{id}` | Member | Get board by ID |
| `GET` | `/workspace/{workspaceId}` | Member | Get all boards in a workspace |
| `GET` | `/my` | Member | Get boards where user is a member |
| `PUT` | `/{id}` | Board Admin | Update board (name, description, visibility, background) |
| `PATCH` | `/{id}/close` | Board Admin | Close (archive) a board |
| `DELETE` | `/{id}` | Board Admin | Delete board (cascades to lists & cards) |

### Board Membership (`/api/v1/boards/{id}/members`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/` | Member | List all board members |
| `POST` | `/` | Board Admin | Add a member with role (`OBSERVER` / `MEMBER` / `ADMIN`) |
| `DELETE` | `/{userId}` | Board Admin | Remove a member |
| `PUT` | `/{userId}/role` | Board Admin | Update a member's role |

### Platform Admin (`/api/v1/boards/admin`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/all` | Platform Admin | List every board on the platform |
| `PATCH` | `/{id}/close` | Platform Admin | Force-close any board |
| `DELETE` | `/{id}` | Platform Admin | Force-delete any board |
| `GET` | `/audit` | Platform Admin | List all board audit events |

### Internal — Service-to-Service (`/api/v1/boards/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `DELETE` | `/workspace/{workspaceId}?actorId=` | Cluster only | Delete all boards in a workspace (called by workspace-service) |

## Inter-Service Communication

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

## Project Structure

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

## Dependencies

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
