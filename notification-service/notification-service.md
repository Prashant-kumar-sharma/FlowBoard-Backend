# Notification Service — Deep Dive

The **notification-service** is the event-driven notification hub. It consumes Kafka events from multiple services and dispatches in-app notifications (MySQL) and email notifications (SMTP).

## Key Features

- 📨 **Kafka Consumer** — 6 topics for real-time event processing
- 🔔 **In-App Notifications** — Persisted with read/unread tracking
- 📧 **Email Dispatch** — Assignments, mentions, invitations, premium, account status
- 📢 **Admin Broadcast** — Send to specific users
- 🔗 **Inter-Service Lookups** — Resolves users from auth-service, cards from card-service

## Entity — `notifications`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK |
| `recipient_id` | BIGINT | NOT NULL |
| `actor_id` | BIGINT | Optional |
| `type` | ENUM | `ASSIGNMENT`/`MENTION`/`DUE_DATE`/`COMMENT`/`MOVE`/`BROADCAST` |
| `title` / `message` | VARCHAR(255) | NOT NULL |
| `related_id` / `related_type` | | Optional |
| `deep_link_url` | VARCHAR(255) | Optional |
| `is_read` | BOOLEAN | Default `false` |
| `created_at` | DATETIME | Auto-set |

## Kafka Topics Consumed

| Topic | Source |
|---|---|
| `flowboard.card.assigned` | card-service |
| `flowboard.mention.notification` | comment-service |
| `flowboard.workspace.member.invited` | workspace-service |
| `flowboard.board.member.invited` | board-service |
| `flowboard.payment.premium.activated` | payment-service |
| `flowboard.account.status.changed` | auth-service |

## API Endpoints (`/api/v1/notifications`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/` | Member | Get all notifications |
| `GET` | `/unread-count` | Member | Unread count |
| `PATCH` | `/{id}/read` | Member | Mark read |
| `PATCH` | `/read-all` | Member | Mark all read |
| `DELETE` | `/read` | Member | Delete read |
| `DELETE` | `/{id}` | Member | Delete one |
| `POST` | `/broadcast` | Platform Admin | Broadcast |

## Dependencies

`spring-boot-starter-web`, `data-jpa`, `validation`, `mail`, `websocket`, `actuator`, `spring-kafka`, `springdoc-openapi`, `eureka-client`, `admin-client`, `mysql-connector-j`, `jackson-databind`, `spring-dotenv`, `lombok`
