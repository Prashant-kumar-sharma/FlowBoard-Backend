# Label Service — Deep Dive

The **label-service** manages labels and checklists — board-scoped colored tags and per-card task breakdowns with assignees, due dates, and progress tracking.

## Key Features

- 🏷️ **Board Labels** — Colored labels scoped to a board
- 🔗 **Card-Label Association** — Many-to-many via join table
- ✅ **Checklists** — Named checklists on cards with ordered items
- 📋 **Checklist Items** — Text, completion toggle, assignee, due date
- 📊 **Progress Tracking** — Completion percentage per checklist

## Entities

### `labels` — `board_id`, `name`, `color`, `created_at`
### `card_labels` — `card_id`, `label_id` (unique constraint)
### `checklists` — `card_id`, `title`, `position`, `created_at`
### `checklist_items` — `checklist_id`, `text`, `is_completed`, `assignee_id`, `due_date`, `position`

## API Endpoints (13 total)

### Labels (`/api/v1`)
`POST /boards/{boardId}/labels` · `GET /boards/{boardId}/labels` · `PUT /labels/{id}` · `DELETE /labels/{id}`

### Card-Label Association
`POST /cards/{cardId}/labels/{labelId}` · `DELETE /cards/{cardId}/labels/{labelId}` · `GET /cards/{cardId}/labels`

### Checklists & Items
`POST /cards/{cardId}/checklists` · `GET /cards/{cardId}/checklists` · `DELETE /checklists/{id}` · `POST /checklists/{checklistId}/items` · `PATCH /checklist-items/{itemId}/toggle` · `GET /checklists/{checklistId}/progress`

## Dependencies

`spring-boot-starter-web`, `data-jpa`, `validation`, `websocket`, `actuator`, `spring-kafka`, `springdoc-openapi`, `eureka-client`, `admin-client`, `mysql-connector-j`, `jackson-databind`, `lombok`
