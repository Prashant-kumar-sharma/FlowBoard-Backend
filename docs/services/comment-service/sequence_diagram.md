```mermaid
sequenceDiagram
    User->>CommentController: POST /comments
    CommentController->>CommentServiceImpl: addComment()
    CommentServiceImpl->>CommentRepository: save()
    CommentRepository-->>CommentServiceImpl: Comment
    CommentServiceImpl->>Kafka: emit CommentAdded
    CommentServiceImpl-->>CommentController: CommentDTO
```
