# Card Service Diagrams

## ER Diagram
```mermaid
erDiagram
    cards ||--o{ card_activities : records

    cards {
        BIGINT id PK
        BIGINT board_id
        BIGINT list_id
        BIGINT assignee_id
        VARCHAR title
        TEXT description
        VARCHAR status "TO_DO | IN_PROGRESS | IN_REVIEW | DONE"
        VARCHAR priority "LOW | MEDIUM | HIGH | CRITICAL"
        INT position
        DATE due_date
        BOOLEAN archived
        DATETIME created_at
        DATETIME updated_at
    }

    card_activities {
        BIGINT id PK
        BIGINT card_id FK
        BIGINT actor_user_id
        VARCHAR action
        VARCHAR old_value
        VARCHAR new_value
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class CardController {
        +create(CreateCardRequest) CardResponse
        +getByBoard(Long) List~CardResponse~
        +getByList(Long) List~CardResponse~
        +move(Long, MoveCardRequest) CardResponse
        +setStatus(Long, UpdateStatusRequest) CardResponse
        +setPriority(Long, UpdatePriorityRequest) CardResponse
        +assign(Long, AssignCardRequest) CardResponse
        +archive(Long) void
        +activity(Long) List~CardActivityEvent~
    }

    class CardServiceImpl {
        -CardRepository cardRepository
        -CardActivityRepository activityRepository
        -CardEventProducer eventProducer
        -SimpMessagingTemplate messagingTemplate
    }

    class CardEventProducer {
        +publishCardAssigned(Card) void
        +publishCardMoved(Card) void
    }

    CardController --> CardServiceImpl
    CardServiceImpl --> CardRepository
    CardServiceImpl --> CardActivityRepository
    CardServiceImpl --> CardEventProducer
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Angular Board UI
    participant Gateway as API Gateway
    participant Ctrl as CardController
    participant Service as CardServiceImpl
    participant Cards as CardRepository
    participant Activity as CardActivityRepository
    participant WS as WebSocket/STOMP
    participant Kafka as Kafka
    participant Notification as notification-service

    User->>SPA: Drag card to another list
    SPA->>Gateway: PATCH /api/v1/cards/{id}/move
    Gateway->>Ctrl: Forward request
    Ctrl->>Service: moveCard(id, targetListId, position)
    Service->>Cards: findById(id)
    Service->>Service: validate board/list + calculate position
    Service->>Cards: save(updated card)
    Service->>Activity: save(CARD_MOVED)
    Service->>WS: publish /topic/boards/{boardId}/cards
    Service->>Kafka: card.moved event
    Kafka-->>Notification: consume card.moved
    Service-->>Ctrl: CardResponse
    Ctrl-->>Gateway: 200 OK
    Gateway-->>SPA: Updated card
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> CardController

    subgraph Card_Service["card-service :8085"]
        CardController
        CardServiceImpl
        WebSocket[WebSocketConfig + STOMP broker]
        EventProducer[CardEventProducer]
        RedisCache[Spring Cache / Redis]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Card_DB["MySQL card tables"]
        Cards[(cards)]
        Activities[(card_activities)]
    end

    Kafka[(Kafka: card.assigned, card.moved)]
    Notification[(notification-service)]
    Redis[(Redis)]
    Angular[Angular board subscribers]
    Eureka[Eureka Registry]

    CardController --> CardServiceImpl
    CardServiceImpl --> Cards
    CardServiceImpl --> Activities
    CardServiceImpl --> WebSocket --> Angular
    CardServiceImpl --> EventProducer --> Kafka --> Notification
    CardServiceImpl --> RedisCache --> Redis
    Card_Service --> Eureka
    AOP -.execution timing.-> CardServiceImpl
    AOP -.writes.-> Logs
```
