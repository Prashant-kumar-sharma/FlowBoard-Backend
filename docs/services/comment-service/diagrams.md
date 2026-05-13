# Comment Service Diagrams

## ER Diagram
```mermaid
erDiagram
    comments {
        BIGINT id PK
        BIGINT card_id
        BIGINT user_id
        TEXT content
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class CommentController {
        +addComment(req: CommentRequest) CommentDTO
    }
    class CommentServiceImpl {
        -CommentRepository commentRepository
    }
    class CommentRepository {
        <<interface>>
        +findByCardId(id: Long) List~Comment~
    }
    CommentController --> CommentServiceImpl
    CommentServiceImpl --> CommentRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>CommentController: POST /comments
    CommentController->>CommentServiceImpl: addComment()
    CommentServiceImpl->>CommentRepository: save()
    CommentRepository-->>CommentServiceImpl: Comment
    CommentServiceImpl->>Kafka: emit CommentAdded
    CommentServiceImpl-->>CommentController: CommentDTO
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Comment_Service
        CommentController
        CommentServiceImpl
    end
    subgraph Comment_DB
        comments[(comments table)]
    end
    CommentController --> CommentServiceImpl
    CommentServiceImpl --> comments
```
