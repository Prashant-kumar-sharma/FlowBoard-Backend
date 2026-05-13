```mermaid
erDiagram
    notifications {
        BIGINT id PK
        BIGINT user_id
        TEXT message
        BOOLEAN is_read
        DATETIME created_at
    }
```
