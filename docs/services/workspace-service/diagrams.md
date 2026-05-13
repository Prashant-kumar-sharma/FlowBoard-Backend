# Workspace Service Diagrams

## ER Diagram
```mermaid
erDiagram
    workspaces ||--o{ workspace_members : contains
    workspaces {
        BIGINT id PK
        VARCHAR name
        BIGINT owner_id
    }
    workspace_members {
        BIGINT id PK
        BIGINT workspace_id FK
        BIGINT user_id
    }
```

## Class Diagram
```mermaid
classDiagram
    class WorkspaceController {
        +createWorkspace(req: WorkspaceRequest) WorkspaceDTO
    }
    class WorkspaceServiceImpl {
        -WorkspaceRepository workspaceRepository
        -PaymentClient paymentClient
    }
    class WorkspaceRepository {
        <<interface>>
        +save(ws: Workspace) Workspace
    }
    WorkspaceController --> WorkspaceServiceImpl
    WorkspaceServiceImpl --> WorkspaceRepository
    WorkspaceServiceImpl --> PaymentClient
```

## Sequence Diagram
```mermaid
sequenceDiagram
    User->>WorkspaceController: POST /workspaces
    WorkspaceController->>WorkspaceServiceImpl: createWorkspace()
    WorkspaceServiceImpl->>PaymentClient: checkStatus()
    PaymentClient-->>WorkspaceServiceImpl: ACTIVE
    WorkspaceServiceImpl->>WorkspaceRepository: save()
    WorkspaceRepository-->>WorkspaceServiceImpl: Workspace
    WorkspaceServiceImpl->>Kafka: emit WorkspaceCreated
    WorkspaceServiceImpl-->>WorkspaceController: WorkspaceDTO
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Workspace_Service
        WorkspaceController
        WorkspaceServiceImpl
        PaymentClient
    end
    subgraph Workspace_DB
        workspaces[(workspaces table)]
    end
    WorkspaceController --> WorkspaceServiceImpl
    WorkspaceServiceImpl --> workspaces
    WorkspaceServiceImpl --> PaymentClient
```
