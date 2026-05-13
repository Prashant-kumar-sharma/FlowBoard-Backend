```mermaid
flowchart TD
    subgraph Card_Service
        CardController
        CardServiceImpl
    end
    subgraph Card_DB
        cards[(cards table)]
    end
    CardController --> CardServiceImpl
    CardServiceImpl --> cards
```
