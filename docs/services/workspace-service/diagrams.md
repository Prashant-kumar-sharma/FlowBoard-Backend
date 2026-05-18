# Workspace Service Diagrams

## ER Diagram
```mermaid
erDiagram
    workspaces ||--o{ workspace_members : has
    workspaces ||--o{ workspace_audit_events : records

    workspaces {
        BIGINT id PK
        VARCHAR name
        VARCHAR description
        BIGINT owner_id
        VARCHAR visibility "PUBLIC | PRIVATE"
        DATETIME created_at
        DATETIME updated_at
    }

    workspace_members {
        BIGINT id PK
        BIGINT workspace_id FK
        BIGINT user_id
        VARCHAR role "OWNER | ADMIN | MEMBER"
        DATETIME joined_at
    }

    workspace_audit_events {
        BIGINT id PK
        BIGINT workspace_id FK
        BIGINT actor_user_id
        VARCHAR action
        VARCHAR details
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class WorkspaceController {
        +create(CreateWorkspaceRequest, userId) WorkspaceResponse
        +getById(Long, userId) WorkspaceResponse
        +getMy(userId) List~WorkspaceResponse~
        +getPublic() List~WorkspaceResponse~
        +update(Long, UpdateWorkspaceRequest, userId) WorkspaceResponse
        +delete(Long, userId) void
        +addMember(Long, AddMemberRequest, userId) WorkspaceMemberResponse
        +updateRole(Long, Long, UpdateRoleRequest) void
    }

    class WorkspaceService {
        <<interface>>
    }

    class WorkspaceServiceImpl {
        -WorkspaceRepository workspaceRepository
        -WorkspaceMemberRepository memberRepository
        -WorkspaceAuditEventRepository auditRepository
        -PaymentEntitlementClient paymentClient
        -BoardCleanupClient boardCleanupClient
        -WorkspaceEventProducer eventProducer
    }

    class PaymentEntitlementClient {
        <<FeignClient>>
        +getEntitlement(Long) PaymentEntitlementResponse
    }

    class BoardCleanupClient {
        <<FeignClient>>
        +deleteByWorkspace(Long) void
    }

    WorkspaceController --> WorkspaceService
    WorkspaceService <|.. WorkspaceServiceImpl
    WorkspaceServiceImpl --> WorkspaceRepository
    WorkspaceServiceImpl --> WorkspaceMemberRepository
    WorkspaceServiceImpl --> WorkspaceAuditEventRepository
    WorkspaceServiceImpl --> PaymentEntitlementClient
    WorkspaceServiceImpl --> BoardCleanupClient
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Angular SPA
    participant Gateway as API Gateway
    participant Ctrl as WorkspaceController
    participant Service as WorkspaceServiceImpl
    participant Payment as payment-service
    participant Repo as WorkspaceRepository
    participant Members as WorkspaceMemberRepository
    participant Audit as AuditRepository
    participant Kafka as Kafka

    User->>SPA: Create workspace
    SPA->>Gateway: POST /api/v1/workspaces
    Gateway->>Ctrl: Forward with X-User-Id/JWT context
    Ctrl->>Service: createWorkspace(request, userId)
    Service->>Payment: GET /internal/payments/users/{id}/entitlement
    Payment-->>Service: plan limits + premium status
    Service->>Repo: countByOwnerId(userId)
    Service->>Service: enforce free/premium workspace limit
    Service->>Repo: save(workspace)
    Service->>Members: save(owner membership)
    Service->>Audit: save(WORKSPACE_CREATED)
    Service->>Kafka: workspace.created / member.invited
    Service-->>Ctrl: WorkspaceResponse
    Ctrl-->>Gateway: 201 Created
    Gateway-->>SPA: WorkspaceResponse
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> WorkspaceController

    subgraph Workspace_Service["workspace-service :8082"]
        WorkspaceController
        WorkspaceServiceImpl
        PaymentClient[PaymentEntitlementClient]
        BoardCleanup[BoardCleanupClient]
        EventProducer[WorkspaceEventProducer]
        RedisCache[Spring Cache / Redis]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Workspace_DB["MySQL workspace tables"]
        Workspaces[(workspaces)]
        Members[(workspace_members)]
        Audit[(workspace_audit_events)]
    end

    Payment[(payment-service :8089)]
    Board[(board-service cleanup API)]
    Kafka[(Kafka workspace/member topics)]
    Redis[(Redis)]
    Eureka[Eureka Registry]

    WorkspaceController --> WorkspaceServiceImpl
    WorkspaceServiceImpl --> Workspaces
    WorkspaceServiceImpl --> Members
    WorkspaceServiceImpl --> Audit
    WorkspaceServiceImpl --> PaymentClient --> Payment
    WorkspaceServiceImpl --> BoardCleanup --> Board
    WorkspaceServiceImpl --> EventProducer --> Kafka
    WorkspaceServiceImpl --> RedisCache --> Redis
    Workspace_Service --> Eureka
    AOP -.execution timing.-> WorkspaceServiceImpl
    AOP -.writes.-> Logs
```
