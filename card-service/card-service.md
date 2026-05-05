# Card Service — Deep Dive

The **card-service** is the core work-item engine of FlowBoard. It manages cards (tasks) within lists, including creation, assignment, priority/status updates, drag-drop movement, reordering, archiving, activity logging, and Kafka event publishing.

## Key Features

- 🃏 **Card CRUD** — Create, read, update, delete task cards
- 🔢 **Position & Reorder** — Integer-based positioning with bulk reorder
- ↔️ **Cross-List Move** — Move cards between lists
- 👤 **Assignee Management** — Assign/reassign to team members
- ⚡ **Priority** — `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- 📊 **Status** — `TO_DO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`
- 📅 **Due & Start Dates** — Timeline tracking with overdue detection
- 📦 **Archive / Unarchive** — Soft-archive without data loss
- 📋 **Activity Log** — Records every field change with old/new values
- 📡 **Kafka Events** — `card.assigned` and `card.moved` events
- 🔗 **Internal API** — Bulk card deletion from board/list services

## Entities

### `cards` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `list_id` | BIGINT | NOT NULL |
| `board_id` | BIGINT | NOT NULL |
| `title` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | Optional |
| `position` | INT | NOT NULL (0-indexed) |
| `priority` | ENUM | `LOW`/`MEDIUM`/`HIGH`/`CRITICAL` |
| `status` | ENUM | `TO_DO`/`IN_PROGRESS`/`IN_REVIEW`/`DONE` |
| `due_date` | DATE | Optional |
| `start_date` | DATE | Optional |
| `assignee_id` | BIGINT | Optional |
| `created_by_id` | BIGINT | NOT NULL |
| `is_archived` | BOOLEAN | Default `false` |
| `cover_color` | VARCHAR(255) | Optional |
| `created_at` / `updated_at` | DATETIME | Auto-set |

### `card_activities` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL |
| `actor_id` | BIGINT | NOT NULL |
| `action` | VARCHAR(255) | e.g. `CARD_CREATED`, `STATUS_CHANGED` |
| `field_name` | VARCHAR(255) | Optional |
| `old_value` / `new_value` | VARCHAR(255) | Optional |
| `created_at` | DATETIME | Auto-set |

## API Endpoints (21 total)

### Card CRUD (`/api/v1/cards`)
`POST /` · `GET /{id}` · `GET /list/{listId}` · `GET /board/{boardId}` · `GET /assignee/{userId}` · `GET /overdue` · `PUT /{id}` · `DELETE /{id}`

### Movement & Ordering
`PATCH /{id}/move` · `PUT /list/{listId}/reorder` · `PATCH /{id}/archive` · `PATCH /{id}/unarchive`

### Field Updates
`PATCH /{id}/assignee` · `PATCH /{id}/priority` · `PATCH /{id}/status`

### Activity Log
`GET /{id}/activity` · `GET /admin/activity` · `GET /admin/all`

### Internal
`DELETE /internal/board/{boardId}` · `DELETE /internal/list/{listId}`

## Kafka Topics

| Topic | Payload | Trigger |
|---|---|---|
| `flowboard.card.assigned` | `{ cardId, assigneeId, actorId }` | Card assigned |
| `flowboard.card.moved` | `{ cardId, fromListId, toListId, actorId }` | Card moved |

## Inter-Service Communication

```
Board Svc (8083) ── HTTP cascade ──▶ Card Svc (8085) ◀── HTTP cascade ── List Svc (8084)
                                          │
                                          ├──▶ Kafka (card.assigned, card.moved)
                                          └──▶ Redis (card cache)
```

## Dependencies

`spring-boot-starter-web`, `data-jpa`, `data-redis`, `cache`, `validation`, `websocket`, `actuator`, `spring-kafka`, `springdoc-openapi`, `eureka-client`, `admin-client`, `mysql-connector-j`, `jackson-databind`, `lombok`
