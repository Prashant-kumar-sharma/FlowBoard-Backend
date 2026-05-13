```mermaid
erDiagram
    workspaces ||--o{ workspace_members : contains
    workspaces {
        BIGINT id PK
        VARCHAR name
        BIGINT owner_id
    }
    workspace_members {
        BIGINT id PK
        BIGINT workspace_id FK
        BIGINT user_id
    }
```
