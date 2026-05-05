# Payment Service — Deep Dive

The **payment-service** manages premium subscriptions via **Razorpay** — checkout order creation, payment verification, subscription activation, and internal entitlement APIs.

## Key Features

- 💳 **Razorpay Checkout** — Creates orders, returns checkout session
- ✅ **Payment Verification** — Confirms payment, activates premium
- 📊 **Subscription Summary** — Premium status, plan, dates
- 🔗 **Internal Entitlement API** — Workspace-service checks premium status
- 🧹 **User Data Cleanup** — GDPR-compliant data deletion
- 📡 **Kafka Events** — `premium.activated` event
- ⚡ **Redis Caching** — Entitlement lookups

## Entities

### `payment_orders`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | NOT NULL |
| `provider_order_id` | VARCHAR(64) | UNIQUE |
| `provider_payment_id` | VARCHAR(64) | Optional |
| `provider_name` | VARCHAR(32) | NOT NULL |
| `plan_code` | VARCHAR(64) | NOT NULL |
| `amount_paise` | INT | NOT NULL |
| `currency` | VARCHAR(8) | NOT NULL |
| `status` | ENUM | `CREATED`/`PAID`/`FAILED` |
| `created_at` / `updated_at` | DATETIME | Auto-set |

### `premium_subscriptions`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | UNIQUE |
| `plan_code` | VARCHAR(64) | NOT NULL |
| `provider_name` | VARCHAR(16) | NOT NULL |
| `status` | ENUM | `ACTIVE`/`INACTIVE` |
| `activated_at` | DATETIME | Optional |
| `created_at` / `updated_at` | DATETIME | Auto-set |

## API Endpoints

### User-Facing (`/api/v1/payments`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/summary` | Member | Premium summary |
| `POST` | `/checkout` | Member | Create Razorpay order |
| `POST` | `/confirm` | Member | Verify & activate |

### Internal (`/api/v1/internal/payments`)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users/{userId}/entitlement` | Cluster only | Check premium |
| `DELETE` | `/users/{userId}` | Cluster only | Delete user data |

## Kafka Topic

`flowboard.payment.premium.activated` → consumed by notification-service for confirmation email + invoice.

## Dependencies

`spring-boot-starter-web`, `data-jpa`, `data-redis`, `cache`, `validation`, `actuator`, `spring-kafka`, `springdoc-openapi`, `eureka-client`, `admin-client`, `mysql-connector-j`, `spring-dotenv`, `lombok`
