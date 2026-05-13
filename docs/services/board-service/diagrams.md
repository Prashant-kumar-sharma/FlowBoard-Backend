# Board Service Diagrams

## ER Diagram
```mermaid
erDiagram
    boards {
        BIGINT id PK
        BIGINT workspace_id
        VARCHAR name
    }
```

## Class Diagram
```mermaid
classDiagram
    class BoardController {
        +createBoard(req: BoardRequest) BoardDTO
    }
    class BoardServiceImpl {
        -BoardRepository boardRepository
    }
    class BoardRepository {
        <<interface>>
        +findByWorkspaceId(id: Long) List~Board~
    }
    BoardController --> BoardServiceImpl
    BoardServiceImpl --> BoardRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>BoardController: POST /boards
    BoardController->>BoardServiceImpl: createBoard()
    BoardServiceImpl->>BoardRepository: save()
    BoardRepository-->>BoardServiceImpl: Board
    BoardServiceImpl->>Kafka: emit BoardCreated
    BoardServiceImpl-->>BoardController: BoardDTO
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Board_Service
        BoardController
        BoardServiceImpl
    end
    subgraph Board_DB
        boards[(boards table)]
    end
    BoardController --> BoardServiceImpl
    BoardServiceImpl --> boards
```
