# Card Service Diagrams

## ER Diagram
```mermaid
erDiagram
    cards {
        BIGINT id PK
        BIGINT list_id
        VARCHAR title
        TEXT description
        INT position
    }
```

## Class Diagram
```mermaid
classDiagram
    class CardController {
        +createCard(req: CardRequest) CardDTO
    }
    class CardServiceImpl {
        -CardRepository cardRepository
    }
    class CardRepository {
        <<interface>>
        +findByListId(id: Long) List~Card~
    }
    CardController --> CardServiceImpl
    CardServiceImpl --> CardRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>CardController: POST /cards
    CardController->>CardServiceImpl: createCard()
    CardServiceImpl->>CardRepository: save()
    CardRepository-->>CardServiceImpl: Card
    CardServiceImpl->>Kafka: emit CardCreated
    CardServiceImpl-->>CardController: CardDTO
```

## Component Diagram
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
