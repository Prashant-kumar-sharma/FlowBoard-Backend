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
