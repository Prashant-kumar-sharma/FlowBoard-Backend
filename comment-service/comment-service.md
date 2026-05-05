# Comment Service — Deep Dive

The **comment-service** handles discussion and file collaboration on cards — threaded comments, soft-delete, file attachments, and file retrieval.

## Key Features

- 💬 **Threaded Comments** — Top-level + nested replies via `parentCommentId`
- ✏️ **Edit & Delete** — Authors edit/soft-delete own comments
- 🔢 **Comment Count** — Lightweight count per card
- 📎 **Attachment Metadata** — Register external file URLs
- 📤 **File Upload** — Multipart upload with local storage
- 📥 **File Retrieval** — Serve files via `/api/v1/files/{filename}`

## Entities

### `comments` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL |
| `author_id` | BIGINT | NOT NULL |
| `content` | TEXT | NOT NULL |
| `parent_comment_id` | BIGINT | Optional (NULL = top-level) |
| `is_deleted` | BOOLEAN | Default `false` |
| `created_at` / `updated_at` | DATETIME | Auto-set |

### `attachments` table

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `card_id` | BIGINT | NOT NULL |
| `uploader_id` | BIGINT | NOT NULL |
| `file_name` | VARCHAR(255) | NOT NULL |
| `file_url` | VARCHAR(255) | NOT NULL |
| `file_type` | VARCHAR(255) | Optional (MIME) |
| `size_kb` | BIGINT | Optional |
| `uploaded_at` | DATETIME | Auto-set |

## API Endpoints

### Comments (`/api/v1`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/cards/{cardId}/comments` | Member | Add comment/reply |
| `GET` | `/cards/{cardId}/comments` | Member | Get top-level comments |
| `GET` | `/comments/{id}/replies` | Member | Get replies |
| `PUT` | `/comments/{id}` | Author | Edit comment |
| `DELETE` | `/comments/{id}` | Author | Soft-delete |
| `GET` | `/cards/{cardId}/comments/count` | Member | Comment count |

### Attachments (`/api/v1`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/cards/{cardId}/attachments` | Member | Add metadata |
| `GET` | `/cards/{cardId}/attachments` | Member | List attachments |
| `DELETE` | `/attachments/{id}` | Uploader | Delete attachment |
| `POST` | `/cards/{cardId}/attachments/upload` | Member | Upload file |
| `GET` | `/files/{filename}` | Member | Download file |

## Dependencies

`spring-boot-starter-web`, `data-jpa`, `validation`, `websocket`, `actuator`, `spring-kafka`, `springdoc-openapi`, `eureka-client`, `admin-client`, `mysql-connector-j`, `jackson-databind`, `lombok`
