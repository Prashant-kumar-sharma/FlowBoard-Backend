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
