# Notification Service Diagrams

## ER Diagram
```mermaid
erDiagram
    notifications {
        BIGINT id PK
        BIGINT user_id
        VARCHAR type
        VARCHAR title
        TEXT message
        VARCHAR target_url
        BOOLEAN read
        DATETIME created_at
        DATETIME read_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class NotificationKafkaConsumer {
        +handleCardAssigned(event)
        +handleCardMoved(event)
        +handleWorkspaceInvite(event)
        +handleBoardInvite(event)
        +handlePaymentActivated(event)
        +handleAccountStatusChanged(event)
    }

    class NotificationController {
        +getAll(userId) List~NotificationResponse~
        +unreadCount(userId) Map
        +markRead(Long, userId) void
        +markAllRead(userId) void
        +deleteRead(userId) void
        +broadcast(BroadcastRequest) void
    }

    class NotificationServiceImpl {
        -NotificationRepository notificationRepository
        -AuthUserClient authUserClient
        -CardLookupClient cardLookupClient
        -EmailService emailService
    }

    NotificationKafkaConsumer --> NotificationServiceImpl
    NotificationController --> NotificationServiceImpl
    NotificationServiceImpl --> NotificationRepository
    NotificationServiceImpl --> AuthUserClient
    NotificationServiceImpl --> CardLookupClient
    NotificationServiceImpl --> EmailService
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    participant Kafka as Kafka Topics
    participant Consumer as NotificationKafkaConsumer
    participant Service as NotificationServiceImpl
    participant Auth as auth-service internal API
    participant Card as card-service API
    participant Repo as NotificationRepository
    participant SMTP as SMTP Server
    participant SPA as Angular Notification Bell

    Kafka->>Consumer: card.assigned event
    Consumer->>Service: processCardAssigned(event)
    Service->>Auth: GET /internal/users/{assigneeId}
    Auth-->>Service: assignee email/name
    Service->>Card: GET /cards/{cardId}
    Card-->>Service: card title/board context
    Service->>Repo: save(notification)
    Service->>SMTP: send email notification
    SPA->>Service: GET /api/v1/notifications/unread-count
    Service->>Repo: countUnread(userId)
    Service-->>SPA: unread count
```

## Component Diagram
```mermaid
flowchart TD
    Kafka[(Kafka topics)] --> Consumer
    Gateway[API Gateway] --> NotificationController

    subgraph Notification_Service["notification-service :8088"]
        Consumer[NotificationKafkaConsumer]
        NotificationController
        NotificationServiceImpl
        AuthClient[AuthUserClient]
        CardClient[CardLookupClient]
        Email[EmailService]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    Notifications[(notifications)]
    Auth[(auth-service internal API)]
    Card[(card-service card lookup)]
    SMTP[(SMTP server)]
    Angular[Angular notification panel]
    Eureka[Eureka Registry]

    Consumer --> NotificationServiceImpl
    NotificationController --> NotificationServiceImpl
    NotificationServiceImpl --> Notifications
    NotificationServiceImpl --> AuthClient --> Auth
    NotificationServiceImpl --> CardClient --> Card
    NotificationServiceImpl --> Email --> SMTP
    NotificationController --> Angular
    Notification_Service --> Eureka
    AOP -.execution timing.-> NotificationServiceImpl
    AOP -.writes.-> Logs
```
