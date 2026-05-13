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
