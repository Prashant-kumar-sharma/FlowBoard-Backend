# FlowBoard — Task Management and Collaboration Platform

A Trello-inspired full-stack Kanban platform built with **Java Spring Boot** microservices and an **Angular 17+** SPA frontend.

---

## Architecture

| Service | Port | Responsibility |
|---|---|---|
| auth-service | 8081 | JWT, OAuth2 (Google/GitHub), user management |
| workspace-service | 8082 | Workspaces, membership |
| board-service | 8083 | Boards, board members |
| list-service | 8084 | Kanban columns, position management |
| card-service | 8085 | Cards, drag-drop, activity log, Kafka producer |
| comment-service | 8086 | Threaded comments, file attachments |
| label-service | 8087 | Labels, checklists, checklist items |
| notification-service | 8088 | In-app notifications, Kafka consumer |
| flowboard-frontend | 80/4200 | Angular 17 SPA |

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
- Angular Frontend: http://localhost
- Auth Service: http://localhost:8081/swagger-ui.html
- Card Service: http://localhost:8085/swagger-ui.html
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

Each service exposes Swagger UI at `/swagger-ui.html`. Example:
- Auth:  http://localhost:8081/swagger-ui.html
- Cards: http://localhost:8085/swagger-ui.html

All endpoints require `Authorization: Bearer <JWT>` except `/api/v1/auth/register` and `/api/v1/auth/login`.

---

## Running Tests

```bash
# Run all tests with coverage
mvn clean verify

# SonarQube analysis
mvn sonar:sonar \
  -Dsonar.host.url=${SONAR_HOST_URL} \
  -Dsonar.login=${SONAR_TOKEN}
```

---

## Environment Variables

See `.env.example` for all required variables. Key ones:

| Variable | Description |
|---|---|
| `JWT_SECRET` | 256-bit base64-encoded secret |
| `DB_*` | MySQL connection details |
| `REDIS_*` | Redis connection details |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers |
| `GOOGLE_CLIENT_ID/SECRET` | Google OAuth2 credentials |
| `AUTH_BASE_URL` | Public base URL used for OAuth callbacks, e.g. `http://localhost:8081` for local auth-service |

---

## Key Domain Rules

- **Card status**: `TO_DO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`
- **Card priority**: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- **Overdue**: `dueDate < today AND status != DONE`
- **Soft delete**: Cards/lists set `isArchived=true`; hard delete only on archived items
- **Real-time**: WebSocket (STOMP) broadcasts card moves and comment additions
- **Async events**: Kafka topics handle notification dispatch

---

## Technology Stack

**Backend:** Java 17, Spring Boot 3.x, Spring Security (JWT + OAuth2), Spring Data JPA, Spring Kafka, Spring WebSocket  
**Frontend:** Angular 17+, NgRx, Angular Material, CDK Drag-Drop, Tailwind CSS, StompJS  
**Infrastructure:** MySQL 8, Redis 7, Apache Kafka, SonarQube, Docker, Docker Compose  
**Testing:** Mockito, JUnit 5, JaCoCo coverage
# FlowBoard-Backend
