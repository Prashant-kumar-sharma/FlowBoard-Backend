# Label Service Diagrams

## ER Diagram
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

## Class Diagram
```mermaid
classDiagram
    class LabelController {
        +createLabel(req: LabelRequest) LabelDTO
    }
    class LabelServiceImpl {
        -LabelRepository labelRepository
    }
    class LabelRepository {
        <<interface>>
        +findByBoardId(id: Long) List~Label~
    }
    LabelController --> LabelServiceImpl
    LabelServiceImpl --> LabelRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>LabelController: POST /labels
    LabelController->>LabelServiceImpl: createLabel()
    LabelServiceImpl->>LabelRepository: save()
    LabelRepository-->>LabelServiceImpl: Label
    LabelServiceImpl-->>LabelController: LabelDTO
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Label_Service
        LabelController
        LabelServiceImpl
    end
    subgraph Label_DB
        labels[(labels table)]
    end
    LabelController --> LabelServiceImpl
    LabelServiceImpl --> labels
```
