# FlowBoard — Task Management and Collaboration Platform

A Trello-inspired full-stack Kanban platform built with **Java 17 / Spring Boot 3.2** microservices, an **Angular 17+** SPA frontend, and event-driven communication via **Apache Kafka**.

---

## Table of Contents

- [Architecture](#architecture)
- [Service Documentation](#service-documentation)
- [Inter-Service Communication](#inter-service-communication)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Running Tests](#running-tests)
- [Environment Variables](#environment-variables)
- [Key Domain Rules](#key-domain-rules)
- [Technology Stack](#technology-stack)

---

## Architecture

### Application Services

| Service | Port | Responsibility | Docs |
|---|---|---|---|
| **auth-service** | 8081 | Authentication (JWT + OAuth2), OTP, user management, admin panel | [📄 Deep Dive](auth-service/auth-service.md) |
| **workspace-service** | 8082 | Workspaces, membership, plan checks via payment-service | [📄 Deep Dive](workspace-service/workspace-service.md) |
| **board-service** | 8083 | Boards, board members, cascade cleanup | [📄 Deep Dive](board-service/board-service.md) |
| **list-service** | 8084 | Kanban columns, position management, reordering | [📄 Deep Dive](list-service/list-service.md) |
| **card-service** | 8085 | Cards, drag-drop, activity log, Kafka event producer | [📄 Deep Dive](card-service/card-service.md) |
| **comment-service** | 8086 | Threaded comments, file attachments | [📄 Deep Dive](comment-service/comment-service.md) |
| **label-service** | 8087 | Labels, checklists, checklist items | [📄 Deep Dive](label-service/label-service.md) |
| **notification-service** | 8088 | In-app + email notifications, Kafka event consumer | [📄 Deep Dive](notification-service/notification-service.md) |
| **payment-service** | 8089 | Subscription plans, Razorpay checkout, entitlements | [📄 Deep Dive](payment-service/payment-service.md) |

### Infrastructure Services

| Service | Port | Responsibility |
|---|---|---|
| **api-gateway** | 8080 | Spring Cloud Gateway — single entry point, JWT validation, route forwarding |
| **eureka-server** | 8761 | Netflix Eureka — service discovery & registry |
| **admin-server** | 9090 | Spring Boot Admin — health monitoring dashboard |
| **flowboard-frontend** | 4200 | Angular 17 SPA |

---

## Service Documentation

Each service has a detailed technical guide covering entities, API endpoints, inter-service communication, project structure, and dependencies:

| Service | Entities | Endpoints | Communication | Guide |
|---|---|---|---|---|
| Auth | `users`, `auth_otps` | 18 endpoints | SMTP, Kafka, OAuth2 | [auth-service.md](auth-service/auth-service.md) |
| Workspace | `workspaces`, `workspace_members`, `audit_events` | 13 endpoints | HTTP → Payment, Board; Kafka; Redis | [workspace-service.md](workspace-service/workspace-service.md) |
| Board | `boards`, `board_members`, `audit_events` | 16 endpoints | HTTP → List, Card; Kafka; Redis | [board-service.md](board-service/board-service.md) |
| List | `task_lists` | 11 endpoints | HTTP → Card; Redis | [list-service.md](list-service/list-service.md) |
| Card | `cards`, `card_activities` | 21 endpoints | Kafka producer; Redis | [card-service.md](card-service/card-service.md) |
| Comment | `comments`, `attachments` | 11 endpoints | File storage; WebSocket | [comment-service.md](comment-service/comment-service.md) |
| Label | `labels`, `card_labels`, `checklists`, `checklist_items` | 13 endpoints | — | [label-service.md](label-service/label-service.md) |
| Notification | `notifications` | 7 endpoints + 6 Kafka | Kafka consumer; SMTP; HTTP lookups | [notification-service.md](notification-service/notification-service.md) |
| Payment | `payment_orders`, `premium_subscriptions` | 5 endpoints | Razorpay API; Kafka; Redis | [payment-service.md](payment-service/payment-service.md) |

---

## Inter-Service Communication

### Cascade Delete Chain

```
Workspace → Board → List → Card
   │          │       │
   │          │       └── HTTP DELETE /api/v1/cards/internal/list/{listId}
   │          └── HTTP DELETE /api/v1/lists/internal/board/{boardId}
   └── HTTP DELETE /api/v1/boards/internal/workspace/{workspaceId}
```

### Kafka Event Flow

```
┌─────────────┐                              ┌──────────────────┐
│ card-service │──▶ card.assigned ───────────▶│                  │
│              │──▶ card.moved    ───────────▶│                  │
├─────────────┤                               │                  │
│ workspace   │──▶ workspace.member.invited ─▶│  notification-   │
├─────────────┤                               │  service         │
│ board       │──▶ board.member.invited ─────▶│                  │
├─────────────┤                               │  (Kafka consumer │
│ payment     │──▶ payment.premium.activated ▶│   + SMTP email)  │
├─────────────┤                               │                  │
│ auth        │──▶ account.status.changed ──▶│                  │
└─────────────┘                              └──────────────────┘
```

### Entitlement Check

```
workspace-service ── HTTP GET ──▶ payment-service /api/v1/internal/payments/users/{id}/entitlement
```

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
