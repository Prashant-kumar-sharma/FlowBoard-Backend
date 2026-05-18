# List Service Diagrams

## ER Diagram
```mermaid
erDiagram
    task_lists {
        BIGINT id PK
        BIGINT board_id
        VARCHAR name
        VARCHAR color
        INT position
        BOOLEAN archived
        DATETIME created_at
        DATETIME updated_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class ListController {
        +create(CreateListRequest) ListResponse
        +getByBoard(Long) List~ListResponse~
        +getArchived(Long) List~ListResponse~
        +update(Long, CreateListRequest) ListResponse
        +reorder(Long, List~Long~) void
        +move(Long, MoveListRequest) ListResponse
        +archive(Long) void
        +unarchive(Long) void
        +delete(Long) void
    }

    class ListServiceImpl {
        -TaskListRepository taskListRepository
        -CardCleanupClient cardCleanupClient
    }

    class CardCleanupClient {
        <<FeignClient>>
        +deleteByList(Long) void
    }

    ListController --> ListServiceImpl
    ListServiceImpl --> TaskListRepository
    ListServiceImpl --> CardCleanupClient
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Angular Board UI
    participant Gateway as API Gateway
    participant Ctrl as ListController
    participant Service as ListServiceImpl
    participant Repo as TaskListRepository
    participant Card as card-service cleanup API
    participant Redis as Redis Cache

    User->>SPA: Reorder lists
    SPA->>Gateway: PUT /api/v1/lists/board/{boardId}/reorder
    Gateway->>Ctrl: Forward ordered list ids
    Ctrl->>Service: reorder(boardId, orderedIds)
    Service->>Repo: findByBoardId(boardId)
    Service->>Service: validate same board + assign positions
    Service->>Repo: saveAll(updatedLists)
    Service->>Redis: evict board list cache
    Service-->>Ctrl: 204 No Content
    Ctrl-->>Gateway: 204 No Content
    Gateway-->>SPA: Reorder accepted

    opt Delete list
        Ctrl->>Service: delete(listId)
        Service->>Card: DELETE /internal/list/{listId}
        Service->>Repo: delete/mark archived
    end
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> ListController

    subgraph List_Service["list-service :8084"]
        ListController
        ListServiceImpl
        CardCleanup[CardCleanupClient]
        RedisCache[Spring Cache / Redis]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    TaskLists[(task_lists)]
    Card[(card-service cleanup API)]
    Redis[(Redis)]
    Eureka[Eureka Registry]

    ListController --> ListServiceImpl
    ListServiceImpl --> TaskLists
    ListServiceImpl --> CardCleanup --> Card
    ListServiceImpl --> RedisCache --> Redis
    List_Service --> Eureka
    AOP -.execution timing.-> ListServiceImpl
    AOP -.writes.-> Logs
```
