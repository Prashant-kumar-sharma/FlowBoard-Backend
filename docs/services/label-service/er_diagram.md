```mermaid
erDiagram
    labels ||--o{ card_labels : contains
    labels {
        BIGINT id PK
        BIGINT board_id
        VARCHAR name
        VARCHAR color
    }
    card_labels {
        BIGINT card_id FK
        BIGINT label_id FK
    }
```
