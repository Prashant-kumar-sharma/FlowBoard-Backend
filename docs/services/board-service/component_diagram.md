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
