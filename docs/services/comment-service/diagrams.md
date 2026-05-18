# Comment Service Diagrams

## ER Diagram
```mermaid
erDiagram
    comments ||--o{ comments : replies
    comments ||--o{ attachments : has

    comments {
        BIGINT id PK
        BIGINT card_id
        BIGINT user_id
        BIGINT parent_comment_id FK
        TEXT content
        DATETIME created_at
        DATETIME updated_at
    }

    attachments {
        BIGINT id PK
        BIGINT card_id
        BIGINT comment_id FK
        VARCHAR file_name
        VARCHAR file_url
        VARCHAR file_type
        BIGINT size_kb
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class CommentController {
        +add(Long, CreateCommentRequest) CommentResponse
        +getByCard(Long) List~CommentResponse~
        +getReplies(Long) List~CommentResponse~
        +update(Long, CreateCommentRequest) CommentResponse
        +delete(Long) void
        +upload(Long, MultipartFile) AttachmentResponse
    }

    class CommentServiceImpl {
        -CommentRepository commentRepository
        -AttachmentRepository attachmentRepository
        -FileStorageService fileStorageService
        -CommentEventProducer eventProducer
        -SimpMessagingTemplate messagingTemplate
    }

    CommentController --> CommentServiceImpl
    CommentServiceImpl --> CommentRepository
    CommentServiceImpl --> AttachmentRepository
    CommentServiceImpl --> FileStorageService
    CommentServiceImpl --> CommentEventProducer
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Card Detail Dialog
    participant Gateway as API Gateway
    participant Ctrl as CommentController
    participant Service as CommentServiceImpl
    participant Repo as CommentRepository
    participant WS as WebSocket/STOMP
    participant Kafka as Kafka

    User->>SPA: Add comment/reply
    SPA->>Gateway: POST /api/v1/comments/cards/{cardId}
    Gateway->>Ctrl: Forward request
    Ctrl->>Service: addComment(cardId, request)
    Service->>Repo: save(comment)
    Service->>WS: publish /topic/cards/{cardId}/comments
    Service->>Kafka: comment.added event
    Service-->>Ctrl: CommentResponse
    Ctrl-->>Gateway: 201 Created
    Gateway-->>SPA: CommentResponse
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> CommentController

    subgraph Comment_Service["comment-service :8086"]
        CommentController
        CommentServiceImpl
        FileStorage[FileStorageService]
        WebSocket[WebSocketConfig + STOMP]
        Producer[CommentEventProducer]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Comment_DB["MySQL comment tables"]
        Comments[(comments)]
        Attachments[(attachments)]
    end

    Uploads[(comment_uploads volume)]
    Kafka[(Kafka comment topics)]
    Angular[Angular card detail subscribers]
    Eureka[Eureka Registry]

    CommentController --> CommentServiceImpl
    CommentServiceImpl --> Comments
    CommentServiceImpl --> Attachments
    CommentServiceImpl --> FileStorage --> Uploads
    CommentServiceImpl --> WebSocket --> Angular
    CommentServiceImpl --> Producer --> Kafka
    Comment_Service --> Eureka
    AOP -.execution timing.-> CommentServiceImpl
    AOP -.writes.-> Logs
```
