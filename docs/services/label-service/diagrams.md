# Label Service Diagrams

## ER Diagram
```mermaid
erDiagram
    labels ||--o{ card_labels : assigned_to
    checklists ||--o{ checklist_items : contains

    labels {
        BIGINT id PK
        BIGINT board_id
        VARCHAR name
        VARCHAR color
        DATETIME created_at
    }

    card_labels {
        BIGINT id PK
        BIGINT card_id
        BIGINT label_id FK
        DATETIME created_at
    }

    checklists {
        BIGINT id PK
        BIGINT card_id
        VARCHAR title
        DATETIME created_at
    }

    checklist_items {
        BIGINT id PK
        BIGINT checklist_id FK
        VARCHAR text
        BIGINT assignee_id
        BOOLEAN completed
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class LabelController {
        +createLabel(Long, CreateLabelRequest) LabelResponse
        +getLabelsByBoard(Long) List~LabelResponse~
        +addToCard(Long, Long) void
        +removeFromCard(Long, Long) void
        +getForCard(Long) List~LabelResponse~
        +createChecklist(Long, CreateChecklistRequest) ChecklistResponse
        +addItem(Long, CreateChecklistItemRequest) ChecklistItemResponse
        +toggleItem(Long) ChecklistItemResponse
        +getProgress(Long) Map
    }

    class LabelServiceImpl {
        -LabelRepository labelRepository
        -CardLabelRepository cardLabelRepository
        -ChecklistRepository checklistRepository
        -ChecklistItemRepository itemRepository
    }

    LabelController --> LabelServiceImpl
    LabelServiceImpl --> LabelRepository
    LabelServiceImpl --> CardLabelRepository
    LabelServiceImpl --> ChecklistRepository
    LabelServiceImpl --> ChecklistItemRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Card Detail Dialog
    participant Gateway as API Gateway
    participant Ctrl as LabelController
    participant Service as LabelServiceImpl
    participant Labels as LabelRepository
    participant CardLabels as CardLabelRepository
    participant Checklists as ChecklistRepository
    participant Items as ChecklistItemRepository

    User->>SPA: Add label and checklist item
    SPA->>Gateway: POST /api/v1/labels/cards/{cardId}/labels/{labelId}
    Gateway->>Ctrl: Forward request
    Ctrl->>Service: addLabelToCard(cardId, labelId)
    Service->>Labels: findById(labelId)
    Service->>CardLabels: save(card-label link)
    Service-->>Ctrl: 200 OK

    SPA->>Gateway: POST /api/v1/labels/checklists/{id}/items
    Gateway->>Ctrl: Forward item request
    Ctrl->>Service: addChecklistItem(checklistId, request)
    Service->>Checklists: findById(checklistId)
    Service->>Items: save(item)
    Service-->>Ctrl: ChecklistItemResponse
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> LabelController

    subgraph Label_Service["label-service :8087"]
        LabelController
        LabelServiceImpl
        WebSocket[Optional WebSocketConfig]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Label_DB["MySQL label/checklist tables"]
        Labels[(labels)]
        CardLabels[(card_labels)]
        Checklists[(checklists)]
        Items[(checklist_items)]
    end

    Eureka[Eureka Registry]

    LabelController --> LabelServiceImpl
    LabelServiceImpl --> Labels
    LabelServiceImpl --> CardLabels
    LabelServiceImpl --> Checklists
    LabelServiceImpl --> Items
    Label_Service --> Eureka
    AOP -.execution timing.-> LabelServiceImpl
    AOP -.writes.-> Logs
```
