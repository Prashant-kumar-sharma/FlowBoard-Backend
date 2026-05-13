```mermaid
sequenceDiagram
    User->>BoardController: POST /boards
    BoardController->>BoardServiceImpl: createBoard()
    BoardServiceImpl->>BoardRepository: save()
    BoardRepository-->>BoardServiceImpl: Board
    BoardServiceImpl->>Kafka: emit BoardCreated
    BoardServiceImpl-->>BoardController: BoardDTO
```
