# Board Service Diagrams

## ER Diagram
```mermaid
erDiagram
    boards ||--o{ board_members : has
    boards ||--o{ board_audit_events : records

    boards {
        BIGINT id PK
        BIGINT workspace_id
        VARCHAR name
        VARCHAR description
        VARCHAR background
        VARCHAR visibility
        BOOLEAN closed
        DATETIME created_at
        DATETIME updated_at
    }

    board_members {
        BIGINT id PK
        BIGINT board_id FK
        BIGINT user_id
        VARCHAR role "OWNER | ADMIN | MEMBER"
        DATETIME joined_at
    }

    board_audit_events {
        BIGINT id PK
        BIGINT board_id FK
        BIGINT actor_user_id
        VARCHAR action
        VARCHAR details
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class BoardController {
        +create(CreateBoardRequest, userId) BoardResponse
        +getByWorkspace(Long, userId) List~BoardResponse~
        +getMy(userId) List~BoardResponse~
        +update(Long, CreateBoardRequest, userId) BoardResponse
        +close(Long, userId) void
        +delete(Long, userId) void
        +addMember(Long, AddBoardMemberRequest) BoardMemberResponse
        +updateMemberRole(Long, Long, UpdateBoardMemberRoleRequest) BoardMemberResponse
    }

    class BoardServiceImpl {
        -BoardRepository boardRepository
        -BoardMemberRepository memberRepository
        -BoardAuditEventRepository auditRepository
        -ListCleanupClient listCleanupClient
        -CardCleanupClient cardCleanupClient
        -BoardEventProducer eventProducer
    }

    class ListCleanupClient {
        <<FeignClient>>
        +deleteByBoard(Long) void
    }

    class CardCleanupClient {
        <<FeignClient>>
        +deleteByBoard(Long) void
    }

    BoardController --> BoardServiceImpl
    BoardServiceImpl --> BoardRepository
    BoardServiceImpl --> BoardMemberRepository
    BoardServiceImpl --> BoardAuditEventRepository
    BoardServiceImpl --> ListCleanupClient
    BoardServiceImpl --> CardCleanupClient
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Angular SPA
    participant Gateway as API Gateway
    participant Ctrl as BoardController
    participant Service as BoardServiceImpl
    participant Boards as BoardRepository
    participant Members as BoardMemberRepository
    participant Audit as BoardAuditRepository
    participant Kafka as Kafka

    User->>SPA: Create board in workspace
    SPA->>Gateway: POST /api/v1/boards
    Gateway->>Ctrl: Forward JWT context
    Ctrl->>Service: createBoard(request, userId)
    Service->>Service: validate workspace/member permission
    Service->>Boards: save(board)
    Service->>Members: save(owner membership)
    Service->>Audit: save(BOARD_CREATED)
    Service->>Kafka: board.member.invited or board.created
    Service-->>Ctrl: BoardResponse
    Ctrl-->>Gateway: 201 Created
    Gateway-->>SPA: BoardResponse
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> BoardController

    subgraph Board_Service["board-service :8083"]
        BoardController
        BoardServiceImpl
        ListCleanup[ListCleanupClient]
        CardCleanup[CardCleanupClient]
        EventProducer[BoardEventProducer]
        RedisCache[Spring Cache / Redis]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Board_DB["MySQL board tables"]
        Boards[(boards)]
        Members[(board_members)]
        Audit[(board_audit_events)]
    end

    List[(list-service cleanup API)]
    Card[(card-service cleanup API)]
    Kafka[(Kafka board topics)]
    Redis[(Redis)]
    Eureka[Eureka Registry]

    BoardController --> BoardServiceImpl
    BoardServiceImpl --> Boards
    BoardServiceImpl --> Members
    BoardServiceImpl --> Audit
    BoardServiceImpl --> ListCleanup --> List
    BoardServiceImpl --> CardCleanup --> Card
    BoardServiceImpl --> EventProducer --> Kafka
    BoardServiceImpl --> RedisCache --> Redis
    Board_Service --> Eureka
    AOP -.execution timing.-> BoardServiceImpl
    AOP -.writes.-> Logs
```
