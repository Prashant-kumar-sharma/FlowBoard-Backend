```mermaid
sequenceDiagram
    User->>ListController: POST /lists
    ListController->>ListServiceImpl: createList()
    ListServiceImpl->>ListRepository: save()
    ListRepository-->>ListServiceImpl: ListEntity
    ListServiceImpl->>Kafka: emit ListCreated
    ListServiceImpl-->>ListController: ListDTO
```
