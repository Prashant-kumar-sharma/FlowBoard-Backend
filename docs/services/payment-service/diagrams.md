# Payment Service Diagrams

## ER Diagram
```mermaid
erDiagram
    subscriptions {
        BIGINT id PK
        BIGINT workspace_id
        VARCHAR plan_type
        VARCHAR status
        DATETIME expires_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class PaymentController {
        +getStatus(workspaceId: Long) PaymentStatus
    }
    class PaymentServiceImpl {
        -SubscriptionRepository subscriptionRepository
    }
    class SubscriptionRepository {
        <<interface>>
        +findByWorkspaceId(workspaceId: Long) Optional~Subscription~
    }
    PaymentController --> PaymentServiceImpl
    PaymentServiceImpl --> SubscriptionRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    WorkspaceService->>PaymentController: GET /status?workspaceId=1
    PaymentController->>PaymentServiceImpl: getStatus(1)
    PaymentServiceImpl->>SubscriptionRepository: findByWorkspaceId(1)
    SubscriptionRepository-->>PaymentServiceImpl: Subscription(ACTIVE)
    PaymentServiceImpl-->>PaymentController: PaymentStatus(ACTIVE)
    PaymentController-->>WorkspaceService: 200 OK
```

## Component Diagram
```mermaid
flowchart TD
    subgraph Payment_Service
        PaymentController
        PaymentServiceImpl
    end
    subgraph Payment_DB
        subscriptions[(subscriptions table)]
    end
    PaymentController --> PaymentServiceImpl
    PaymentServiceImpl --> subscriptions
```
