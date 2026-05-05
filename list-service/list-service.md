# List Service — Deep Dive

The **list-service** manages Kanban columns within boards — creation, position ordering, drag-drop reordering, archiving, cross-board moves, and cascade cleanup.

## Key Features

- 📝 **List CRUD** — Create, read, update, delete Kanban columns
- 🔢 **Position Management** — Integer-based ordering
- 🔄 **Drag-Drop Reorder** — Bulk reorder endpoint
- 📦 **Archive / Unarchive** — Soft-archive (`isArchived=true`)
- 🎨 **Custom Colors** — Default `#E2E4E9`
- ↔️ **Cross-Board Move** — Move list between boards
- 🧹 **Cascade Cleanup** — Deletes cards via card-service
- 🔗 **Internal API** — Board-service triggers bulk deletion
- ⚡ **Redis Caching**

## Entity — `task_lists`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(255) | NOT NULL |
| `board_id` | BIGINT | NOT NULL |
| `position` | INT | NOT NULL (0-indexed) |
| `color` | VARCHAR(255) | Default `#E2E4E9` |
| `is_archived` | BOOLEAN | Default `false` |
| `created_at` | DATETIME | Auto-set |
| `updated_at` | DATETIME | Auto-set |

## API Endpoints

### List CRUD (`/api/v1/lists`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/` | Member | Create a new list |
| `GET` | `/{id}` | Member | Get list by ID |
| `GET` | `/board/{boardId}` | Member | Get active lists (ordered) |
| `GET` | `/board/{boardId}/archived` | Member | Get archived lists |
| `PUT` | `/{id}` | Member | Update list |
| `DELETE` | `/{id}` | Member | Delete list (cascades) |

### Position & Movement

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `PUT` | `/board/{boardId}/reorder` | Member | Reorder lists |
| `PATCH` | `/{id}/archive` | Member | Archive |
| `PATCH` | `/{id}/unarchive` | Member | Unarchive |
| `PATCH` | `/{id}/move` | Member | Move to different board |

### Internal (`/api/v1/lists/internal`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `DELETE` | `/board/{boardId}` | Cluster only | Delete all lists in board |

## Inter-Service Communication

```
Board Svc (8083) ── HTTP cascade delete ──▶ List Svc (8084)
                                                  │
                                                  ├── HTTP ──▶ Card Svc (8085)
                                                  └──▶ Redis (list cache)
```

## Dependencies

`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis`, `spring-boot-starter-cache`, `spring-boot-starter-validation`, `spring-boot-starter-websocket`, `spring-boot-starter-actuator`, `spring-kafka`, `springdoc-openapi-starter-webmvc-ui`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-admin-starter-client`, `mysql-connector-j`, `jackson-databind`, `lombok`
