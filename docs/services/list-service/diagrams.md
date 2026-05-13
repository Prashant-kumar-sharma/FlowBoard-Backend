# List Service Diagrams

## ER Diagram
```mermaid
erDiagram
    lists {
        BIGINT id PK
        BIGINT board_id
        VARCHAR name
        INT position
    }
```

## Class Diagram
```mermaid
classDiagram
    class ListController {
        +createList(req: ListRequest) ListDTO
    }
    class ListServiceImpl {
        -ListRepository listRepository
    }
    class ListRepository {
        <<interface>>
        +findByBoardId(id: Long) List~ListEntity~
    }
    ListController --> ListServiceImpl
    ListServiceImpl --> ListRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>ListController: POST /lists
    ListController->>ListServiceImpl: createList()
    ListServiceImpl->>ListRepository: save()
    ListRepository-->>ListServiceImpl: ListEntity
    ListServiceImpl->>Kafka: emit ListCreated
    ListServiceImpl-->>ListController: ListDTO
```

## Component Diagram
```mermaid
flowchart TD
    subgraph List_Service
        ListController
        ListServiceImpl
    end
    subgraph List_DB
        lists[(lists table)]
    end
    ListController --> ListServiceImpl
    ListServiceImpl --> lists
```
