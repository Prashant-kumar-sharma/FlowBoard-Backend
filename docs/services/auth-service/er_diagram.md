```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email "UNIQUE"
        VARCHAR password_hash
        VARCHAR role
        DATETIME created_at
    }
```
