# RabbitMQ Architecture Design

## Core Concepts (Quick Recap)

Before the diagram, let's clarify RabbitMQ components:

1. **Exchange**: Receives messages from publishers, routes them to queues based on routing rules
2. **Queue**: Stores messages until consumed by a service
3. **Routing Key**: A "label" on each message (e.g., `order.created`, `user.registered`)
4. **Binding**: A rule connecting exchange to queue (e.g., "send messages with routing key `order.*` to `investment_orders_queue`")

**Why not publish directly to queues?**

- Flexibility: One message can go to multiple queues (fan-out pattern)
- Decoupling: Publisher doesn't know which services consume the message
- Scalability: Easy to add new consumers without changing publishers

---

## Exchange Types We Use

### **1. `Topic` Exchange** (most flexible)

- Routes based on pattern matching
- Routing key uses dots: `entity.action.detail`
- Bindings use wildcards:  (one word), `#` (zero or more words)

**Example:**

- Message with routing key `order.created.investment`
- Matches binding pattern `order.created.*` ✅
- Matches binding pattern `order.*` ✅
- Matches binding pattern `#` ✅
- Does NOT match `user.*` ❌

### **2. `Fanout` Exchange** (broadcast)

- Sends message to ALL bound queues
- Ignores routing key
- Used when multiple services need the same event

**Example:**

- User registers → Send to: analytics queue, welcome email queue, CRM sync queue

---

## Full RabbitMQ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              PUBLISHERS (Services)                                  │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────┐  ┌──────────────────┐  ┌─────────────────┐  ┌─────────────────┐    │
│  │  Core API   │  │ Investment Engine│  │ Transaction Svc │  │ Background Jobs │    │
│  │  (NestJS)   │  │     (Go)         │  │     (Go)        │  │  (Schedulers)   │    │
│  └──────┬──────┘  └────────┬─────────┘  └────────┬────────┘  └────────┬────────┘    │
│         │                  │                     │                    │             │
│         │ Publishes        │ Publishes           │ Publishes          │ Publishes   │
│         │ events           │ events              │ events             │ events      │
│         │                  │                     │                    │             │
└─────────┼──────────────────┼─────────────────────┼────────────────────┼─────────────┘
          │                  │                     │                    │
          ▼                  ▼                     ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           RABBITMQ MESSAGE BROKER                                   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                        EXCHANGE: "orders_exchange"                         │     │
│  │                        Type: Topic                                         │     │
│  │                                                                            │     │
│  │  Receives messages with routing keys like:                                 │     │
│  │  - order.created                                                           │     │
│  │  - order.calculated                                                        │     │
│  │  - order.completed                                                         │     │
│  │  - order.failed                                                            │     │
│  └────────┬──────────────────┬────────────────────┬───────────────────────────┘     │
│           │                  │                    │                                 │
│           │ Binding:         │ Binding:           │ Binding:                        │
│           │ order.created    │ order.calculated   │ order.completed                 │
│           │                  │                    │                                 │
│           ▼                  ▼                    ▼                                 │
│  ┌──────────────────┐ ┌──────────────────┐ ┌───────────────────────┐                │
│  │ QUEUE:           │ │ QUEUE:           │ │ QUEUE:                │                │
│  │ investment_      │ │ transaction_     │ │ notification_orders   │                │
│  │ orders_queue     │ │ orders_queue     │ │                       │                │
│  │                  │ │                  │ │                       │                │
│  │ Messages:        │ │ Messages:        │ │ Messages:             │                │
│  │ - New orders     │ │ - Calculated     │ │ - Completed orders    │                │
│  │   needing        │ │   orders ready   │ │   (notify user)       │                │
│  │   allocation     │ │   for execution  │ │                       │                │
│  └──────────────────┘ └──────────────────┘ └───────────────────────┘                │
│                                                                                     │
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                    EXCHANGE: "notifications_exchange"                      │     │
│  │                    Type: Topic                                             │     │
│  │                                                                            │     │
│  │  Receives messages with routing keys like:                                 │     │
│  │  - notification.order.completed                                            │     │
│  │  - notification.budget.exceeded                                            │     │
│  │  - notification.goal.milestone                                             │     │ 
│  │  - notification.kyc.verified                                               │     │
│  └──────────┬─────────────────┬───────────────────┬───────────────────────────┘     │
│             │                 │                   │                                 │
│             │ Binding:        │ Binding:          │ Binding:                        │
│             │ notification.*  │ notification.*.   │ notification.budget.*           │
│             │ .email          │ .push             │                                 │
│             │                 │                   │                                 │
│             ▼                 ▼                   ▼                                 │
│  ┌───────────────────┐ ┌──────────────────┐ ┌────────────────────┐                  │
│  │ QUEUE:            │ │ QUEUE:           │ │ QUEUE:             │                  │
│  │ email_            │ │ push_            │ │ budget_alerts_     │                  │
│  │ notifications_    │ │ notifications_   │ │ queue              │                  │
│  │ queue             │ │ queue            │ │                    │                  │
│  │                   │ │                  │ │                    │                  │
│  │ Messages:         │ │ Messages:        │ │ Messages:          │                  │
│  │ - All email       │ │ - All push       │ │ - Budget exceeded  │                  │
│  │   notifications   │ │   notifications  │ │   alerts only      │                  │
│  └───────────────────┘ └──────────────────┘ └────────────────────┘                  │
│                                                                                     │
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                       EXCHANGE: "accounts_exchange"                        │     │
│  │                       Type: Topic                                          │     │
│  │                                                                            │     │
│  │  Receives messages with routing keys like:                                 │     │
│  │  - account.synced                                                          │     │
│  │  - account.connected                                                       │     │
│  │  - account.sync.failed                                                     │     │
│  └────────┬────────────────────┬──────────────────────────────────────────────┘     │
│           │                    │                                                    │
│           │ Binding:           │ Binding:                                           │
│           │ account.synced     │ account.*                                          │
│           │                    │                                                    │
│           ▼                    ▼                                                    │
│  ┌──────────────────┐  ┌──────────────────────┐                                     │
│  │ QUEUE:           │  │ QUEUE:               │                                     │
│  │ budget_update_   │  │ analytics_accounts_  │                                     │
│  │ queue            │  │ queue                │                                     │
│  │                  │  │                      │                                     │
│  │ Messages:        │  │ Messages:            │                                     │
│  │ - Account synced │  │ - All account events │                                     │
│  │   (recalc budget)│  │   (for reporting)    │                                     │
│  └──────────────────┘  └──────────────────────┘                                     │
│                                                                                     │
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                         EXCHANGE: "users_exchange"                         │     │
│  │                         Type: Fanout                                       │     │
│  │                         (Broadcasts to all bound queues)                   │     │
│  │                                                                            │     │
│  │  Receives messages with events like:                                       │     │
│  │  - user.registered                                                         │     │
│  │  - user.kyc.verified                                                       │     │
│  │  (Routing key ignored - goes to ALL queues)                                │     │
│  └──────┬──────────────┬────────────────┬─────────────────────────────────────┘     │
│         │              │                │                                           │
│         │ Binding      │ Binding        │ Binding                                   │
│         │ (no pattern) │ (no pattern)   │ (no pattern)                              │
│         │              │                │                                           │
│         ▼              ▼                ▼                                           │
│  ┌────────────┐ ┌────────────────┐ ┌──────────────────┐                             │
│  │ QUEUE:     │ │ QUEUE:         │ │ QUEUE:           │                             │
│  │ welcome_   │ │ analytics_     │ │ crm_sync_        │                             │
│  │ email_     │ │ users_queue    │ │ queue            │                             │
│  │ queue      │ │                │ │                  │                             │
│  │            │ │                │ │                  │                             │
│  │ (Send      │ │ (Track user    │ │ (Sync to         │                             │
│  │  welcome   │ │  signups)      │ │  HubSpot)        │                             │
│  │  email)    │ │                │ │                  │                             │
│  └────────────┘ └────────────────┘ └──────────────────┘                             │
│                                                                                     │
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                       EXCHANGE: "analytics_exchange"                       │     │
│  │                       Type: Topic                                          │     │
│  │                                                                            │     │
│  │  Receives ALL events for analytics tracking:                               │     │
│  │  - *.created, *.updated, *.deleted                                         │     │
│  │  - order.*, account.*, budget.*, goal.*                                    │     │
│  └────────────────────────────────┬───────────────────────────────────────────┘     │
│                                   │                                                 │
│                                   │ Binding: #                                      │
│                                   │ (Matches everything)                            │
│                                   │                                                 │
│                                   ▼                                                 │
│                          ┌─────────────────────┐                                    │
│                          │ QUEUE:              │                                    │
│                          │ analytics_events_   │                                    │
│                          │ queue               │                                    │
│                          │                     │                                    │
│                          │ Messages:           │                                    │
│                          │ - ALL events        │                                    │
│                          │   (comprehensive    │                                    │
│                          │    logging)         │                                    │
│                          └─────────────────────┘                                    │
│                                                                                     │
│                                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                    EXCHANGE: "deadletter_exchange"                         │     │
│  │                    Type: Fanout                                            │     │
│  │                                                                            │     │
│  │  Receives messages that:                                                   │     │
│  │  - Failed processing 3 times (max retries)                                 │     │
│  │  - Rejected by consumer                                                    │     │
│  │  - Expired (TTL exceeded)                                                  │     │
│  └────────────────────────────────┬───────────────────────────────────────────┘     │
│                                   │                                                 │
│                                   │ Binding                                         │
│                                   │                                                 │
│                                   ▼                                                 │
│                          ┌─────────────────────┐                                    │
│                          │ QUEUE:              │                                    │
│                          │ dead_letter_queue   │                                    │
│                          │                     │                                    │
│                          │ Messages:           │                                    │
│                          │ - Failed messages   │                                    │
│                          │   (manual review)   │                                    │
│                          └─────────────────────┘                                    │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
          │                  │                     │                    │
          │ Consumes         │ Consumes            │ Consumes           │ Consumes
          │                  │                     │                    │
          ▼                  ▼                     ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CONSUMERS (Services)                                   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌──────────────────┐  ┌─────────────────┐  ┌────────────────┐  ┌──────────────┐    │
│  │ Investment Engine│  │ Transaction Svc │  │ Notification   │  │ Analytics    │    │
│  │     (Go)         │  │     (Go)        │  │ Service        │  │ Service      │    │
│  │                  │  │                 │  │ (NestJS)       │  │ (NestJS)     │    │
│  │ Listens to:      │  │ Listens to:     │  │                │  │              │    │
│  │ - investment_    │  │ - transaction_  │  │ Listens to:    │  │ Listens to:  │    │
│  │   orders_queue   │  │   orders_queue  │  │ - email_queue  │  │ - analytics_ │    │
│  │                  │  │                 │  │ - push_queue   │  │   events_    │    │
│  │                  │  │                 │  │ - welcome_     │  │   queue      │    │
│  │                  │  │                 │  │   email_queue  │  │ - analytics_ │    │
│  │                  │  │                 │  │ - budget_      │  │   accounts_  │    │
│  │                  │  │                 │  │   alerts_queue │  │   queue      │    │
│  │                  │  │                 │  │                │  │ - analytics_ │    │
│  │                  │  │                 │  │                │  │   users_queue│    │
│  └──────────────────┘  └─────────────────┘  └────────────────┘  └──────────────┘    │
│                                                                                     │
│  ┌──────────────────┐                                                               │
│  │ Core API         │                                                               │
│  │ (NestJS)         │                                                               │
│  │                  │                                                               │
│  │ Listens to:      │                                                               │
│  │ - budget_update_ │                                                               │
│  │   queue          │                                                               │
│  │ - crm_sync_queue │                                                               │
│  └──────────────────┘                                                               │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘

```

---

## Detailed Exchange & Queue Mappings

### **Exchange 1: orders_exchange (Topic)**

| Routing Key | Queue | Bound Pattern | Consumer | Purpose |
| --- | --- | --- | --- | --- |
| `order.created` | `investment_orders_queue` | `order.created` | Investment Engine (Go) | Calculate ETF allocation for new order |
| `order.calculated` | `transaction_orders_queue` | `order.calculated` | Transaction Service (Go) | Execute calculated orders with broker |
| `order.completed` | `notification_orders_queue` | `order.completed` | Notification Service | Send "Investment complete" notification |
| `order.failed` | `notification_orders_queue` | `order.failed` | Notification Service | Send "Investment failed" notification |
| `order.*` | `analytics_events_queue` | `order.*` | Analytics Service | Track all order events for reporting |

---

### **Exchange 2: notifications_exchange (Topic)**

| Routing Key | Queue | Bound Pattern | Consumer | Purpose |
| --- | --- | --- | --- | --- |
| `notification.order.completed.email` | `email_notifications_queue` | `notification.*.email` | Notification Service | Send email notifications |
| `notification.budget.exceeded.email` | `email_notifications_queue` | `notification.*.email` | Notification Service | Send email notifications |
| `notification.goal.milestone.email` | `email_notifications_queue` | `notification.*.email` | Notification Service | Send email notifications |
| `notification.order.completed.push` | `push_notifications_queue` | `notification.*.push` | Notification Service | Send push notifications |
| `notification.budget.exceeded.push` | `push_notifications_queue` | `notification.*.push` | Notification Service | Send push notifications |
| `notification.budget.exceeded` | `budget_alerts_queue` | `notification.budget.*` | Notification Service | Specialized budget alert handling |

**Why multiple patterns?**

- One message can match multiple patterns (email AND push)
- Message with routing key `notification.budget.exceeded.email` goes to BOTH:
    - `email_notifications_queue` (pattern: `notification.*.email`)
    - `budget_alerts_queue` (pattern: `notification.budget.*`)

---

### **Exchange 3: accounts_exchange (Topic)**

| Routing Key | Queue | Bound Pattern | Consumer | Purpose |
| --- | --- | --- | --- | --- |
| `account.synced` | `budget_update_queue` | `account.synced` | Core API | Recalculate budget spending after sync |
| `account.synced` | `analytics_accounts_queue` | `account.*` | Analytics Service | Track account sync events |
| `account.connected` | `analytics_accounts_queue` | `account.*` | Analytics Service | Track new account connections |
| `account.sync.failed` | `analytics_accounts_queue` | `account.*` | Analytics Service | Track sync failures |

---

### **Exchange 4: users_exchange (Fanout)**

| Event | Queue | Consumer | Purpose |
| --- | --- | --- | --- |
| `user.registered` | `welcome_email_queue` | Notification Service | Send welcome email to new user |
| `user.registered` | `analytics_users_queue` | Analytics Service | Track user signups for metrics |
| `user.registered` | `crm_sync_queue` | Core API | Sync new user to HubSpot CRM |
| `user.kyc.verified` | `welcome_email_queue` | Notification Service | Send "KYC approved" email |
| `user.kyc.verified` | `analytics_users_queue` | Analytics Service | Track KYC completions |
| `user.kyc.verified` | `crm_sync_queue` | Core API | Update HubSpot with KYC status |

**Why Fanout?**

- Same event needs to trigger multiple independent actions
- Don't care about routing key - ALL bound queues receive message
- Simplifies publisher (no need to specify multiple routing keys)

---

### **Exchange 5: analytics_exchange (Topic)**

| Routing Key | Queue | Bound Pattern | Consumer | Purpose |
| --- | --- | --- | --- | --- |
| `order.created` | `analytics_events_queue` | `#` | Analytics Service | Log all events to MongoDB |
| `budget.updated` | `analytics_events_queue` | `#` | Analytics Service | Log all events to MongoDB |
| `goal.milestone` | `analytics_events_queue` | `#` | Analytics Service | Log all events to MongoDB |
| ANY routing key | `analytics_events_queue` | `#` | Analytics Service | `#` matches everything |

**Why `#` pattern?**

- Analytics needs ALL events for comprehensive tracking
- `#` = wildcard matching zero or more words
- Every published event to this exchange → analytics queue

---

### **Exchange 6: deadletter_exchange (Fanout)**

| Source Queue | Dead Letter Queue | Why Message Sent Here |
| --- | --- | --- |
| `investment_orders_queue` | `dead_letter_queue` | Order processing failed 3 times |
| `transaction_orders_queue` | `dead_letter_queue` | Broker API error (persistent) |
| `email_notifications_queue` | `dead_letter_queue` | SendGrid rejected email (invalid) |
| `push_notifications_queue` | `dead_letter_queue` | Device token invalid |

**What happens to dead letters?**

- Manual review by operations team
- Alert sent to on-call engineer
- Investigate why message failed
- Fix issue, requeue message manually

---

## Complete Event Flow Table

| Publisher | Event | Exchange | Routing Key | Queue(s) | Consumer(s) | Purpose |
| --- | --- | --- | --- | --- | --- | --- |
| **Core API** | User places order | `orders_exchange` | `order.created` | `investment_orders_queue` | Investment Engine | Calculate allocation |
| **Investment Engine** | Allocation calculated | `orders_exchange` | `order.calculated` | `transaction_orders_queue` | Transaction Service | Execute trades |
| **Transaction Service** | Order executed | `orders_exchange` | `order.completed` | `notification_orders_queue` | Notification Service | Notify user |
| **Transaction Service** | Order executed | `notifications_exchange` | `notification.order.completed.email` | `email_notifications_queue` | Notification Service | Send email |
| **Transaction Service** | Order executed | `notifications_exchange` | `notification.order.completed.push` | `push_notifications_queue` | Notification Service | Send push |
| **Core API** | Bank sync completed | `accounts_exchange` | `account.synced` | `budget_update_queue` | Core API | Recalculate budgets |
| **Core API** | Bank sync completed | `accounts_exchange` | `account.synced` | `analytics_accounts_queue` | Analytics Service | Track sync |
| **Core API** | User registers | `users_exchange` | `user.registered` | `welcome_email_queue` | Notification Service | Send welcome email |
| **Core API** | User registers | `users_exchange` | `user.registered` | `analytics_users_queue` | Analytics Service | Track signup |
| **Core API** | User registers | `users_exchange` | `user.registered` | `crm_sync_queue` | Core API | Sync to HubSpot |
| **Core API** | Budget exceeded | `notifications_exchange` | `notification.budget.exceeded.email` | `email_notifications_queue` | Notification Service | Send alert email |
| **Core API** | Budget exceeded | `notifications_exchange` | `notification.budget.exceeded.push` | `push_notifications_queue` | Notification Service | Send alert push |
| **Core API** | Budget exceeded | `notifications_exchange` | `notification.budget.exceeded` | `budget_alerts_queue` | Notification Service | Special handling |
| **Core API** | Goal milestone | `notifications_exchange` | `notification.goal.milestone.email` | `email_notifications_queue` | Notification Service | Send email |
| **Core API** | Goal milestone | `notifications_exchange` | `notification.goal.milestone.push` | `push_notifications_queue` | Notification Service | Send push |
| **Core API** | KYC verified | `users_exchange` | `user.kyc.verified` | `welcome_email_queue` | Notification Service | Send approval email |
| **Core API** | KYC verified | `users_exchange` | `user.kyc.verified` | `analytics_users_queue` | Analytics Service | Track KYC |
| **Transaction Service** | Payment failed | `notifications_exchange` | `notification.payment.failed.email` | `email_notifications_queue` | Notification Service | Alert user |
| **ALL SERVICES** | Any event | `analytics_exchange` | `*.created`, `*.updated`, etc. | `analytics_events_queue` | Analytics Service | Comprehensive logging |

---

## Message Routing Examples

### **Example 1: Investment Order Flow**

```
User places €500 order
    │
    ▼
Core API publishes:
    Exchange: orders_exchange
    Routing Key: order.created
    Payload: {orderId: 789, userId: 123, amount: 50000, portfolioType: "growth"}
    │
    ├──────────────────────────────────────┐
    │                                      │
    ▼                                      ▼
investment_orders_queue            analytics_events_queue
    │                                      │
    ▼                                      ▼
Investment Engine                    Analytics Service
    │                                 (Logs event to MongoDB)
    ▼
Calculates allocation:
    60% stocks = €300
    40% bonds = €200
    │
    ▼
Investment Engine publishes:
    Exchange: orders_exchange
    Routing Key: order.calculated
    Payload: {orderId: 789, allocation: [{symbol: "VUSA.L", amount: 30000}, ...]}
    │
    ├──────────────────────────────────────┐
    │                                      │
    ▼                                      ▼
transaction_orders_queue           analytics_events_queue
    │                                      │
    ▼                                      │
Transaction Service                        │
    │                                      │
    ▼                                      │
Executes trades with broker                │
    │                                      │
    ▼                                      │
Transaction Service publishes:             │
    Exchange: orders_exchange              │
    Routing Key: order.completed           │
    Payload: {orderId: 789, executedAt: "..."}
    │                                      │
    ├──────────────────────────┬───────────┼───────────────────┐
    │                          │           │                   │
    ▼                          ▼           ▼                   ▼
notification_orders_queue  Also publishes to:           analytics_events_queue
    │                    notifications_exchange               │
    ▼                    - notification.order.completed.email │
Notification Service     - notification.order.completed.push  │
(Decides which channels)         │                   │        │
                                 ▼                   ▼        │
                         email_notifications     push_notifications
                               _queue                 _queue
                                 │                      │
                                 ▼                      ▼
                         Notification Svc       Notification Svc
                         (Sends email)           (Sends push)

```

**Result**:

- Investment Engine processed order
- Transaction Service executed trades
- User got email notification
- User got push notification
- Analytics logged all events

---

### **Example 2: Budget Exceeded (Multi-Queue Routing)**

```
Bank sync finds new transaction: -€120 at grocery store
    │
    ▼
Core API detects budget exceeded (spent €650 / limit €600)
    │
    ▼
Core API publishes:
    Exchange: notifications_exchange
    Routing Key: notification.budget.exceeded.email
    Payload: {userId: 123, category: "food", spent: 650, limit: 600}
    │
    │ (This routing key matches TWO binding patterns!)
    │
    ├─────────────────────────────────┐
    │                                 │
    │ Pattern: notification.*.email   │ Pattern: notification.budget.*
    │                                 │
    ▼                                 ▼
email_notifications_queue      budget_alerts_queue
    │                                 │
    ▼                                 ▼
Notification Service            Notification Service
(Sends generic email)           (Special budget alert logic:
                                 - Check if user opted in
                                 - Add spending tips
                                 - More detailed email)

```

**Why two queues for same message?**

- `email_notifications_queue`: General email sender (templated)
- `budget_alerts_queue`: Special handling for budget alerts (personalized tips, spending breakdown)
- Same message, different processing logic

---

### **Example 3: User Registration (Fanout Broadcasting)**

```
User completes signup form
    │
    ▼
Core API publishes:
    Exchange: users_exchange (FANOUT)
    Routing Key: user.registered (ignored by fanout)
    Payload: {userId: 123, email: "john@example.com", firstName: "John"}
    │
    │ (Fanout = send to ALL bound queues, ignore routing key)
    │
    ├───────────────────────┬──────────────────────────┐
    │                       │                          │
    ▼                       ▼                          ▼
welcome_email_queue   analytics_users_queue    crm_sync_queue
    │                       │                          │
    ▼                       ▼                          ▼
Notification Service   Analytics Service         Core API
(Send welcome email)   (Log signup metric)   (Sync to HubSpot)

All 3 happen independently and simultaneously!

```

---

## Queue Configuration Details

### **Queue Properties**

| Queue | Durable | Auto-Delete | TTL | Max Length | Dead Letter Exchange |
| --- | --- | --- | --- | --- | --- |
| `investment_orders_queue` | Yes | No | None | 10,000 | `deadletter_exchange` |
| `transaction_orders_queue` | Yes | No | None | 10,000 | `deadletter_exchange` |
| `email_notifications_queue` | Yes | No | 1 hour | 50,000 | `deadletter_exchange` |
| `push_notifications_queue` | Yes | No | 30 min | 50,000 | `deadletter_exchange` |
| `welcome_email_queue` | Yes | No | 24 hours | 10,000 | `deadletter_exchange` |
| `budget_update_queue` | Yes | No | None | 5,000 | `deadletter_exchange` |
| `budget_alerts_queue` | Yes | No | 1 hour | 5,000 | `deadletter_exchange` |
| `analytics_events_queue` | Yes | No | None | 100,000 | None (log errors) |
| `analytics_accounts_queue` | Yes | No | None | 20,000 | None |
| `analytics_users_queue` | Yes | No | None | 20,000 | None |
| `crm_sync_queue` | Yes | No | 24 hours | 5,000 | `deadletter_exchange` |
| `notification_orders_queue` | Yes | No | 30 min | 10,000 | `deadletter_exchange` |
| `dead_letter_queue` | Yes | No | 7 days | Unlimited | None (manual review) |

**Property explanations:**

**Durable: Yes**

- Queue survives RabbitMQ server restart
- Messages persisted to disk
- Critical for financial data (can't lose orders)

**Auto-Delete: No**

- Queue remains even if no consumers connected
- Prevents accidental deletion during deployments
- Messages accumulate if consumer crashes (can debug later)

**TTL (Time To Live)**

- How long messages stay in queue before expiring
- `None` = never expire (critical business logic)
- `1 hour` = notifications (stale alert is useless)
- `24 hours` = welcome email (delay is acceptable)
- Expired messages move to dead letter queue

**Max Length**

- Maximum messages queue can hold
- Prevents memory exhaustion if consumer crashes
- When full, oldest messages dropped or rejected
- Higher for high-volume queues (notifications, analytics)

**Dead Letter Exchange**

- Where failed/expired messages go
- Allows manual investigation and retry
- Analytics queues don't need DLX (losing log entry is acceptable)

---

### **Why Different TTLs?**

**No TTL (Critical business logic):**

```
investment_orders_queue: No TTL
transaction_orders_queue: No TTL
budget_update_queue: No TTL

```

**Reasoning:**

- User's money is involved
- Order must be processed no matter how long it takes
- If consumer is down for 2 hours, order should still execute when it comes back up
- Better to process late than never

**Short TTL (Time-sensitive notifications):**

```
email_notifications_queue: 1 hour
push_notifications_queue: 30 minutes
budget_alerts_queue: 1 hour

```

**Reasoning:**

- Notification about "order completed" from 2 hours ago is confusing
- User already saw order status in app
- Stale notification causes poor UX ("Why am I getting this now?")
- Better to drop than send late

**Long TTL (Low priority, eventual delivery):**

```
welcome_email_queue: 24 hours
crm_sync_queue: 24 hours

```

**Reasoning:**

- Welcome email can arrive few hours after signup (acceptable)
- CRM sync is not time-critical
- Gives time to recover if service is down for maintenance

---

### **Retry Strategy**

Each queue has retry policy before sending to dead letter exchange:

| Queue | Max Retries | Backoff Strategy | Retry Delay |
| --- | --- | --- | --- |
| `investment_orders_queue` | 3 | Exponential | 1 min, 5 min, 15 min |
| `transaction_orders_queue` | 3 | Exponential | 1 min, 5 min, 15 min |
| `email_notifications_queue` | 3 | Exponential | 10 sec, 1 min, 5 min |
| `push_notifications_queue` | 2 | Linear | 30 sec, 1 min |
| `budget_update_queue` | 3 | Exponential | 1 min, 5 min, 15 min |
| `crm_sync_queue` | 5 | Exponential | 5 min, 15 min, 30 min, 1 hour, 2 hours |

**How retries work:**

```
Message processing fails (exception thrown)
    │
    ▼
Consumer NACK (negative acknowledgment) with requeue=false
    │
    ▼
RabbitMQ checks: Has this message been retried before?
    │
    ├─ No retries yet
    │   │
    │   ▼
    │   Add to delayed queue (wait 1 minute)
    │   │
    │   ▼
    │   After 1 minute, re-deliver to original queue
    │
    ├─ 1st retry failed
    │   │
    │   ▼
    │   Add to delayed queue (wait 5 minutes)
    │   │
    │   ▼
    │   After 5 minutes, re-deliver to original queue
    │
    ├─ 2nd retry failed
    │   │
    │   ▼
    │   Add to delayed queue (wait 15 minutes)
    │   │
    │   ▼
    │   After 15 minutes, re-deliver to original queue
    │
    └─ 3rd retry failed
        │
        ▼
        Send to dead_letter_queue
        │
        ▼
        Alert operations team

```

**Why exponential backoff?**

- First failure might be temporary glitch (retry quickly)
- Repeated failures indicate systemic issue (wait longer, don't spam)
- Gives time for dependent services to recover

---

## Message Priority

Some queues support priority to process urgent messages first:

| Queue | Priority Levels | Use Case |
| --- | --- | --- |
| `investment_orders_queue` | 1-5 | 5 = large orders (>€10,000), 1 = small orders |
| `email_notifications_queue` | 1-3 | 3 = critical alerts, 2 = transactional, 1 = marketing |
| `transaction_orders_queue` | No priority | All orders equal (FIFO) |

**How priority works:**

```jsx
// Publisher sets priority
channel.publish(
  'orders_exchange',
  'order.created',
  Buffer.from(JSON.stringify(orderData)),
  {
    priority: 5,  // High priority
    persistent: true
  }
);

// Consumer receives high-priority messages first
// Order with priority 5 processed before order with priority 1
// Within same priority, FIFO order maintained

```

**When to use priority:**

- Large investment orders (more revenue, process faster)
- Critical security alerts (KYC rejection, fraud detection)
- NOT for everything (defeats purpose of queue fairness)

---

## Message Deduplication

**Problem:** What if same message published twice?

**Example:**

```
Network glitch during order creation
→ Core API publishes order.created event
→ No acknowledgment received (network timeout)
→ Core API retries, publishes same event again
→ Investment Engine processes order TWICE!
→ User's €500 order becomes €1000 order (BAD!)

```

**Solution: Idempotency with message ID**

Each message includes unique ID:

```jsx
// Publisher generates unique ID
const messageId = `order-${orderId}-${Date.now()}`;

channel.publish(
  'orders_exchange',
  'order.created',
  Buffer.from(JSON.stringify(orderData)),
  {
    messageId: messageId,
    persistent: true
  }
);

// Store in Redis: "processed this message ID?"
await redis.setex(`processed:${messageId}`, 3600, '1');

```

**Consumer checks before processing:**

```jsx
async function handleOrderCreated(message) {
  const messageId = message.properties.messageId;

  // Check if already processed
  const alreadyProcessed = await redis.get(`processed:${messageId}`);

  if (alreadyProcessed) {
    console.log(`Message ${messageId} already processed, skipping`);
    channel.ack(message);  // Acknowledge but don't process
    return;
  }

  // Process message
  await calculateAllocation(message.content);

  // Mark as processed (keep for 1 hour)
  await redis.setex(`processed:${messageId}`, 3600, '1');

  channel.ack(message);
}

```

---

## Monitoring & Observability

### **Key Metrics to Track**

**1. Queue Depth (Messages Waiting)**

```
investment_orders_queue: 0 messages (healthy)
email_notifications_queue: 1,245 messages (healthy, high volume)
transaction_orders_queue: 5,000 messages (ALERT! Consumer down?)

```

**Alert if:**

- Critical queues (orders, transactions) > 100 messages
- Indicates consumer crashed or too slow
- Need to scale consumers or investigate bottleneck

**2. Message Rate**

```
orders_exchange: 150 msg/min published
investment_orders_queue: 150 msg/min consumed

```

**Alert if:**

- Publish rate > consume rate for extended period
- Queue will grow indefinitely
- Need more consumers

**3. Consumer Count**

```
investment_orders_queue: 2 consumers (Investment Engine instances)
email_notifications_queue: 3 consumers (Notification Service workers)

```

**Alert if:**

- Consumer count = 0 (service crashed!)
- Consumer count < expected (instance died)

**4. Failed Messages (Dead Letter Queue)**

```
dead_letter_queue: 0 messages (healthy)
dead_letter_queue: 15 messages (investigate!)

```

**Alert if:**

- ANY message in DLQ (requires manual review)
- Check error logs to diagnose issue

**5. Message Age**

```
Average age in investment_orders_queue: 5 seconds (healthy)
Average age in investment_orders_queue: 10 minutes (SLOW!)

```

**Alert if:**

- Age > 1 minute for critical queues
- Indicates processing bottleneck

---

### **RabbitMQ Management Dashboard**

AWS Amazon MQ provides web UI at: `https://b-xxxx.mq.us-east-1.amazonaws.com`

**What you can see:**

- List all exchanges and queues
- Real-time message rates (publish, deliver, ack)
- Queue depths over time (graphs)
- Consumer connections (which services connected)
- Message details (inspect payload, headers)
- Manually purge queues (emergency cleanup)
- Manually move messages (DLQ → original queue)

---

## Failure Scenarios & Recovery

### **Scenario 1: Consumer Crashes**

**What happens:**

```
Investment Engine crashes while processing order
→ Message was delivered but not acknowledged
→ RabbitMQ waits for ACK timeout (default: 30 minutes)
→ After timeout, message re-queued
→ Another Investment Engine instance picks it up
→ Order processed successfully

```

**No data loss** because:

- Messages not acknowledged stay in queue
- Durable queue persists messages to disk
- Automatic re-delivery on consumer failure

---

### **Scenario 2: RabbitMQ Server Crashes**

**What happens:**

```
RabbitMQ server crashes (AWS maintenance, hardware failure)
→ All in-memory messages lost (if not durable)
→ Durable queues restored from disk
→ Consumers reconnect automatically
→ Processing resumes

```

**Mitigation:**

- All queues are durable (survive restarts)
- Messages marked persistent (written to disk)
- AWS Amazon MQ has automatic failover (multi-AZ)
- Downtime: ~2-3 minutes (failover time)

---

### **Scenario 3: Message Processing Fails 3 Times**

**What happens:**

```
Email sending fails (SendGrid API down)
→ Retry 1: Fails again (1 min later)
→ Retry 2: Fails again (5 min later)
→ Retry 3: Fails again (15 min later)
→ Message moved to dead_letter_queue
→ Alert sent to operations team
→ Manual investigation required

```

**Operations team actions:**

1. Check error logs (why did it fail?)
2. Fix issue (SendGrid credentials expired?)
3. Manually requeue message from DLQ
4. Message processed successfully

---

### **Scenario 4: Downstream Service Unavailable**

**What happens:**

```
Broker API down (can't execute trades)
→ Transaction Service tries to process order
→ Fails immediately (connection refused)
→ NACK message with requeue
→ Exponential backoff: retry in 1 min, 5 min, 15 min
→ If still failing after 3 retries → DLQ

```

**Why not retry forever?**

- If broker is down for hours, queue grows huge
- Better to alert humans to investigate
- Can manually requeue from DLQ when broker recovers

---

## Design Decisions Explained

### **Why Topic Exchange for Most Use Cases?**

**Topic vs Direct vs Fanout:**

| Exchange Type | Routing | Flexibility | Use Case |
| --- | --- | --- | --- |
| **Direct** | Exact match | Low | Simple 1:1 routing |
| **Topic** | Pattern match | High | Complex routing rules |
| **Fanout** | Broadcast all | None | Same message, multiple consumers |

**We chose Topic because:**

- One message can go to multiple queues (pattern matching)
- Easy to add new consumers without changing publishers
- Example: `notification.budget.exceeded` goes to:
    - `email_notifications_queue` (pattern: `notification.*.email`)
    - `budget_alerts_queue` (pattern: `notification.budget.*`)
    - `analytics_events_queue` (pattern: `notification.*`)

**When we use Fanout:**

- User registration (ALL services need to know)
- No conditional routing needed
- Simpler than topic when truly broadcasting

---

### **Why Multiple Queues for Notifications?**

**Could have single `notifications_queue` and route inside consumer:**

```jsx
// ❌ BAD: Single queue, complex consumer logic
async function handleNotification(message) {
  const { type, channel } = message;

  if (channel === 'email') {
    await sendEmail(...);
  } else if (channel === 'push') {
    await sendPush(...);
  } else if (type === 'budget' && channel === 'email') {
    await sendSpecialBudgetEmail(...);
  }
  // Complex nested logic...
}

```

**Better: Separate queues, simple consumers:**

```jsx
// ✅ GOOD: Specialized queues
// email_notifications_queue → simple email sender
// push_notifications_queue → simple push sender
// budget_alerts_queue → specialized budget handler

```

**Benefits:**

- Each consumer has single responsibility
- Can scale independently (10 email workers, 3 push workers)
- Easy to add new notification types (new queue)
- Failure in one channel doesn't affect others

---

### **Why Analytics Gets Everything (`#` pattern)?**

```
analytics_exchange binds analytics_events_queue with pattern: #

```

**Reasoning:**

- Analytics needs comprehensive event log
- Don't want to update analytics consumer every time new event added
- `#` = "give me everything" (zero maintenance)

**Risk:**

- Analytics queue receives HUGE volume
- Solution: High max length (100,000), fast processing

**Alternative considered (rejected):**

- Publish analytics events separately
- Problem: Easy to forget, analytics gets incomplete data
- Current approach: Automatic, always complete

---

### **Why Dead Letter Exchange is Fanout?**

**All failed messages go to single `dead_letter_queue`:**

**Reasoning:**

- Failed messages are rare (should be!)
- Manual review needed regardless of source
- Single queue easier to monitor
- Operations team checks one place

**Alternative considered (rejected):**

- Separate DLQ per original queue
- Problem: Operations team checks 10 different queues
- No benefit (all require manual intervention anyway)

---

## Message Payload Standards

### **Standard Message Format**

All messages follow this structure:

```json
{
  "eventId": "evt_1737285000_abc123",
  "eventType": "order.created",
  "timestamp": "2026-01-19T10:30:00.123Z",
  "source": "core-api",
  "version": "1.0",
  "data": {
    "orderId": 789,
    "userId": 123,
    "amount": 50000,
    "currency": "EUR",
    "portfolioType": "growth"
  },
  "metadata": {
    "requestId": "req_xyz789",
    "userId": 123,
    "sessionId": "sess_abc123"
  }
}

```

**Field explanations:**

- **eventId**: Unique ID for idempotency (prevent duplicate processing)
- **eventType**: What happened (matches routing key)
- **timestamp**: When event occurred (ISO 8601)
- **source**: Which service published (debugging)
- **version**: Schema version (allow evolution)
- **data**: Event-specific payload
- **metadata**: Contextual info (tracing, debugging)

---

### **Message Headers**

RabbitMQ message properties:

```jsx
{
  messageId: 'evt_1737285000_abc123',
  timestamp: 1737285000123,
  persistent: true,  // Survive server restart
  priority: 1,  // 1-10 (if queue supports)
  contentType: 'application/json',
  contentEncoding: 'utf-8',
  correlationId: 'req_xyz789',  // Link request → response
  replyTo: null,  // Not using RPC pattern
  expiration: '3600000',  // TTL in milliseconds (1 hour)
  headers: {
    retryCount: 0,
    originalQueue: 'investment_orders_queue'
  }
}

```

---

## RabbitMQ Configuration Summary

### **AWS Amazon MQ Setup**

```yaml
Broker Type: RabbitMQ 3.11
Deployment Mode: Single-instance (Series A)
Instance Type: mq.t3.micro
Storage: 20 GB EBS
Multi-AZ: No (cost optimization, acceptable downtime risk)
Automatic Minor Version Upgrade: Yes
Maintenance Window: Sunday 3:00 AM - 4:00 AM UTC

```

### **Upgrade Path (Series B)**

When reaching 1.5M users:

```yaml
Deployment Mode: Active/Standby (Multi-AZ)
Instance Type: mq.m5.large
Storage: 50 GB EBS
High Availability: Yes (automatic failover)

```

---

This RabbitMQ design ensures:

- **Reliable message delivery** (durable queues, retries, dead letter handling)
- **Scalable architecture** (topic exchanges allow flexible routing)
- **Independent scaling** (each queue/consumer can scale separately)
- **Operational visibility** (monitoring, dead letter queue for failed messages)
- **Failure recovery** (automatic retries, dead letter queue for manual intervention)

All while remaining simple enough for a Series A team to manage effectively.