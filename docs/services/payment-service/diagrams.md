# Payment Service Diagrams

## ER Diagram
```mermaid
erDiagram
    payment_orders ||--o| premium_subscriptions : activates

    payment_orders {
        BIGINT id PK
        BIGINT user_id
        VARCHAR provider_order_id
        VARCHAR provider_payment_id
        BIGINT amount_paise
        VARCHAR currency
        VARCHAR status "CREATED | PAID | FAILED"
        DATETIME created_at
    }

    premium_subscriptions {
        BIGINT id PK
        BIGINT user_id
        VARCHAR plan_name
        VARCHAR status "ACTIVE | EXPIRED | CANCELLED"
        INT workspace_limit
        INT member_limit
        DATETIME starts_at
        DATETIME expires_at
        DATETIME created_at
    }
```

## Class Diagram
```mermaid
classDiagram
    class PaymentController {
        +summary(userId) PaymentSummaryResponse
        +createCheckout(userId) CheckoutSessionResponse
        +confirmPayment(ConfirmPaymentRequest, userId) PaymentSummaryResponse
    }

    class InternalPaymentController {
        +getEntitlement(Long) PaymentEntitlementResponse
    }

    class PaymentService {
        -PaymentOrderRepository orderRepository
        -PremiumSubscriptionRepository subscriptionRepository
        -PaymentEventProducer eventProducer
    }

    class PaymentQueryService {
        +summary(Long) PaymentSummaryResponse
        +entitlement(Long) PaymentEntitlementResponse
    }

    PaymentController --> PaymentService
    PaymentController --> PaymentQueryService
    InternalPaymentController --> PaymentQueryService
    PaymentService --> PaymentOrderRepository
    PaymentService --> PremiumSubscriptionRepository
```

## Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SPA as Angular Billing Page
    participant Gateway as API Gateway
    participant Ctrl as PaymentController
    participant Service as PaymentService
    participant Razorpay as Razorpay API
    participant Orders as PaymentOrderRepository
    participant Subs as PremiumSubscriptionRepository
    participant Kafka as Kafka

    User->>SPA: Click upgrade to premium
    SPA->>Gateway: POST /api/v1/payments/checkout
    Gateway->>Ctrl: Forward request
    Ctrl->>Service: createCheckout(userId)
    Service->>Razorpay: create order
    Razorpay-->>Service: providerOrderId
    Service->>Orders: save payment order
    Service-->>Ctrl: CheckoutSessionResponse
    Ctrl-->>SPA: Razorpay order details
    User->>SPA: Complete payment
    SPA->>Gateway: POST /api/v1/payments/confirm
    Gateway->>Ctrl: Forward confirmation
    Ctrl->>Service: confirmPayment(payload)
    Service->>Orders: mark PAID
    Service->>Subs: create/extend premium subscription
    Service->>Kafka: payment.premium.activated
    Service-->>Ctrl: PaymentSummaryResponse
```

## Component Diagram
```mermaid
flowchart TD
    Gateway[API Gateway] --> PaymentController
    Workspace[workspace-service] --> InternalPaymentController
    Auth[auth-service] --> InternalPaymentController

    subgraph Payment_Service["payment-service :8089"]
        PaymentController
        InternalPaymentController
        PaymentService
        PaymentQueryService
        RazorpayConfig[RazorpayClientConfig]
        EventProducer[PaymentEventProducer]
        RedisCache[Spring Cache / Redis]
        AOP[ExecutionTimeLoggingAspect]
        Logs[(Rolling log file)]
    end

    subgraph Payment_DB["MySQL payment tables"]
        Orders[(payment_orders)]
        Subs[(premium_subscriptions)]
    end

    Razorpay[(Razorpay API)]
    Kafka[(Kafka: payment.premium.activated)]
    Redis[(Redis)]
    Eureka[Eureka Registry]

    PaymentController --> PaymentService
    PaymentController --> PaymentQueryService
    InternalPaymentController --> PaymentQueryService
    PaymentService --> RazorpayConfig --> Razorpay
    PaymentService --> Orders
    PaymentService --> Subs
    PaymentQueryService --> Subs
    PaymentService --> EventProducer --> Kafka
    PaymentQueryService --> RedisCache --> Redis
    Payment_Service --> Eureka
    AOP -.execution timing.-> PaymentService
    AOP -.writes.-> Logs
```
