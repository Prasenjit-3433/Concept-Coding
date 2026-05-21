# FinVerse - Detailed System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER INTERFACES                                │
├──────────────────────────────┬──────────────────────────────────────────────┤
│   React Native Mobile App    │     React Internal Dashboards (Amplify)      │
│   (iOS + Android)            │     (Support, Ops, Analytics Teams)          │
│   - Investment screens       │     - Customer support tools                 │
│   - Budget tracking          │     - Transaction monitoring                 │
│   - Goal management          │     - KYC approval dashboard                 │
└──────────────┬───────────────┴────────────────────┬─────────────────────────┘
               │ HTTPS/REST                         │ HTTPS/REST
               │ WebSocket (real-time updates)      │
               └────────────────┬───────────────────┘
                                │
                                ▼
         ┌──────────────────────────────────────────────────────────┐
         │           **AWS APPLICATION LOAD BALANCER (ALB)**        │
         │  - SSL/TLS Termination                                   │
         │  - Health checks (HTTP /health on each service)          │
         │  - Path-based routing:                                   │
         │    • /api/v1/users/*        → Core API                   │
         │    • /api/v1/budgets/*      → Core API                   │
         │    • /api/v1/investments/*  → Investment Engine          │
         │    • /api/v1/transactions/* → Transaction Service        │
         │  - Rate limiting (AWS WAF integration)                   │
         └────────┬──────────────┬───────────────┬──────────────────┘
                  │              │               │
        ┌─────────┘              │               └─────────────┐
        │                        │                             │
        ▼                        ▼                             ▼
┌────────────────────┐  ┌────────────────────┐  ┌─────────────────────────┐
│   CORE API         │  │  INVESTMENT ENGINE │  │  TRANSACTION PROCESSING │
│   MONOLITH         │  │  SERVICE           │  │  SERVICE                │
│   (**NestJS**)     │  │  (**Go**)          │  │  (**Go**)               │
│                    │  │                    │  │                         │
│  Running on:       │  │  Running on:       │  │  Running on:            │
│  *AWS Elastic*     │  │  *AWS ECS Fargate* │  │  *AWS ECS Fargate*      │
│  *Beanstalk*       │  │  (2 tasks)         │  │  (2 tasks)              │
│  (2-4 instances)   │  │                    │  │                         │
│                    │  │  Endpoints:        │  │  Endpoints:             │
│  Modules:          │  │  POST /calculate   │  │  POST /orders/execute   │
│  ┌──────────────┐  │  │  POST /rebalance   │  │  POST /transfers        │
│  │ User Module  │  │  │  GET  /portfolio   │  │  GET  /ledger           │
│  │ - Auth       │  │  │       /:userId     │  │  POST /settle           │
│  │ - Profile    │  │  │  GET  /tax-report  │  │                         │
│  │ - KYC        │  │  │                    │  │  Database:              │
│  └──────────────┘  │  │  Database:         │  │  PostgreSQL (Primary)   │
│  ┌──────────────┐  │  │  PostgreSQL        │  │  - orders table         │
│  │Account Module│  │  │  (Read Replica)    │  │  - ledger table         │
│  │ - Bank sync  │  │  │  Redis (cache)     │  │  - settlements table    │
│  │ - Balance    │  │  │  - ETF prices      │  │                         │
│  └──────────────┘  │  │  - Market data     │  │  Redis:                 │
│  ┌──────────────┐  │  │                    │  │  - Idempotency keys     │
│  │Budget Module │  │  └────────┬───────────┘  │  - Order deduplication  │
│  │ - Expenses   │  │           │              │                         │
│  │ - Categories │  │           │              └───────────┬─────────────┘
│  │ - Alerts     │  │           │                          │
│  └──────────────┘  │           │                          │
│  ┌──────────────┐  │           │                          │
│  │ Goal Module  │  │           │    EVENT PUBLISHING      │
│  │ - Savings    │  │           │    (after processing)    │
│  │ - Tracking   │  │           │                          │
│  └──────────────┘  │           │                          │
│  ┌──────────────┐  │           │                          │
│  │Education Mod │  │           │                          │
│  │ - Courses    │  │           │                          │
│  │ - Lessons    │  │           │                          │
│  └──────────────┘  │           │                          │
│  ┌──────────────┐  │           │                          │
│  │Subscription  │  │           │                          │
│  │ - Billing    │  │           │                          │
│  │ - Plans      │  │           │                          │
│  └──────────────┘  │           │                          │
│                    │           │                          │
│  Analytics         │           │                          │
│  (internal)        │           │                          │
│                    │           │                          │
│  Database:         │           │                          │
│  PostgreSQL (Prim) │           │                          │
│  MongoDB           │           │                          │
│                    │           │                          │
│  EVENT PUBLISHING: │           │                          │
│  Publishes to      │           │                          │
│  RabbitMQ:         │           │                          │
│  - user.created    │           │                          │
│  - account.synced  │           │                          │
│  - budget.exceeded │           │                          │
│  - order.created   │◄──────────┼──────────────────────────┘
└────────┬───────────┘           │
         │                       │
         │ EVENT PUBLISHING      │ EVENT PUBLISHING
         │                       │
         └───────────────────────┴──────────────┐
                                                │
                                                ▼
              ┌─────────────────────────────────────────────────────┐
              │          MESSAGE BROKER: **AWS AMAZON MQ**          │
              │                (RabbitMQ)                           │
              │                                                     │
              │  Exchanges & Queues:                                │
              │  ┌────────────────────────────────────────────────┐ │
              │  │ Exchange: "orders"                             │ │
              │  │  └─> Queue: "investment.orders"                │ │
              │  │       (consumed by Investment Engine)          │ │
              │  │  └─> Queue: "transaction.orders"               │ │
              │  │       (consumed by Transaction Service)        │ │
              │  └────────────────────────────────────────────────┘ │
              │  ┌────────────────────────────────────────────────┐ │
              │  │ Exchange: "notifications"                      │ │
              │  │  └─> Queue: "email.notifications"              │ │
              │  │  └─> Queue: "push.notifications"               │ │
              │  │  └─> Queue: "sms.notifications"                │ │
              │  │       (all consumed by Notification Service    │ │
              │  └────────────────────────────────────────────────┘ │
              │  ┌────────────────────────────────────────────────┐ │
              │  │ Exchange: "events"                             │ │
              │  │  └─> Queue: "analytics.events"                 │ │
              │  │  └─> Queue: "audit.events"                     │ │
              │  └────────────────────────────────────────────────┘ │
              └───────────┬─────────────────────────────────────────┘
                          │
                          │ CONSUME EVENTS
                          │
                          ▼
              ┌──────────────────────────────────────────┐
              │    NOTIFICATION SERVICE (NestJS)         │
              │    Running on: *AWS ECS Fargate* (1 task │
              │                                          │
              │    Components:                           │
              │    ┌─────────────────────────────────┐   │
              │    │ RabbitMQ Consumer (Listeners)   │   │
              │    │ - Listens to notification queues│   │
              │    └──────────┬──────────────────────┘   │
              │               │                          │
              │               ▼                          │
              │    ┌─────────────────────────────────┐   │
              │    │ Notification Processor          │   │
              │    │ - Routes to correct channel     │   │
              │    │ - Applies templates             │   │
              │    │ - Handles retries               │   │
              │    └──────────┬──────────────────────┘   │
              │               │                          │
              │               ▼                          │
              │    ┌─────────────────────────────────┐   │
              │    │ BullMQ Producer                 │   │
              │    │ - Adds jobs to queues           │   │
              │    │ - Sets retry policies           │   │
              │    │ - Schedules delayed sends       │   │
              │    └──────────┬──────────────────────┘   │
              └───────────────┼──────────────────────────┘
                              │
                              │ Writes ***jobs*** to Redis
                              ▼
              ┌──────────────────────────────────────────────────────┐
              │              REDIS (**AWS ElastiCache**)             │
              │          cache.t3.medium + 1 replica                 │
              │                                                      │
              │  *BullMQ's Queues* (stored in Redis):                │
              │  ┌────────────────────────────────────────────────┐  │
              │  │ Queue: "email-queue"                           │  │
              │  │  - Jobs: {userId, template, data, priority}    │  │
              │  │  - Retry: 3 attempts, exponential backoff      │  │
              │  └────────────────────────────────────────────────┘  │
              │  ┌────────────────────────────────────────────────┐  │
              │  │ Queue: "push-queue"                            │  │
              │  │  - Jobs: {userId, title, body, deviceTokens}   │  │
              │  └────────────────────────────────────────────────┘  │
              │  ┌────────────────────────────────────────────────┐  │
              │  │ Queue: "sms-queue"                             │  │
              │  │  - Jobs: {phone, message}                      │  │
              │  └────────────────────────────────────────────────┘  │
              │  ┌────────────────────────────────────────────────┐  │
              │  │ Queue: "scheduled-jobs-queue"                  │  │
              │  │  - Recurring investments (cron: 0 0 1 * *)     │  │
              │  │  - Portfolio rebalancing (cron: 0 2 * * 6)     │  │
              │  │  - Tax reports (delayed: end of year)          │  │
              │  └────────────────────────────────────────────────┘  │
              │                                                      │
              │  Also used for:                                      │
              │  - Session storage (user JWT tokens)                 │
              │  - API rate limiting counters                        │
              │  - Cached data (ETF prices, portfolio values)        │
              └───────────┬──────────────────────────────────────────┘
                          │
                          │ WORKERS CONSUME JOBS
                          ▼
        ┌──────────────────────────────────────────────────────────┐
        │           BULLMQ WORKERS (Background Processors)         │
        │                                                          │
        │  Running as separate ***processes*** within services:    │
        │                                                          │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ EMAIL WORKER (in Notification Service)             │  │
        │  │  - Polls "email-queue" in Redis                    │  │
        │  │  - Processes jobs: fetch template from MongoDB     │  │
        │  │  - Sends via Twilio SendGrid API                   │  │
        │  │  - On failure: retry or move to dead-letter queue  │  │
        │  │  Concurrency: 5 jobs at a time                     │  │
        │  └────────────────────────────────────────────────────┘  │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ PUSH WORKER (in Notification Service)              │  │
        │  │  - Polls "push-queue" in Redis                     │  │
        │  │  - Sends via AWS SNS (mobile push notifications)   │  │
        │  │  Concurrency: 10 jobs at a time                    │  │
        │  └────────────────────────────────────────────────────┘  │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ SMS WORKER (in Notification Service)               │  │
        │  │  - Polls "sms-queue" in Redis                      │  │
        │  │  - Sends via Twilio SMS API                        │  │
        │  │  Concurrency: 3 jobs at a time                     │  │
        │  └────────────────────────────────────────────────────┘  │
        │  ┌────────────────────────────────────────────────────┐  │
        │  │ SCHEDULED JOBS WORKER (in Core API)                │  │
        │  │  - Polls "scheduled-jobs-queue" in Redis           │  │
        │  │  - Processes:                                      │  │
        │  │    • Recurring investments (publishes order event) │  │
        │  │    • Portfolio rebalancing (calls Invest Engine)   │  │
        │  │    • Tax report generation (generates PDF)         │  │
        │  │  Concurrency: 2 jobs at a time                     │  │
        │  └────────────────────────────────────────────────────┘  │
        │                                                          │
        │  How workers operate:                                    │
        │  1. Worker polls Redis queue (blocking pop)              │
        │  2. Picks up job from queue                              │
        │  3. Executes job (call external API, DB operation)       │
        │  4. On success: mark job complete, remove from queue     │
        │  5. On failure: retry based on policy or move to DLQ     │
        │  6. Repeat (long-running process)                        │
        └────────────┬───────────────────┬─────────────────────────┘
                     │                   │
                     │ Sends via         │ Sends via
                     ▼                   ▼
        ┌─────────────────────┐  ┌──────────────────┐
        │  Twilio SendGrid    │  │    AWS SNS       │
        │  (Email delivery)   │  │  (Push notifs)   │
        └─────────────────────┘  └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA LAYER                                         │
├────────────────────────────┬────────────────────────────────────────────────┤
│                            │                                                │
│  ┌──────────────────────┐  │  ┌──────────────────────────────────────────┐  │
│  │  PostgreSQL (RDS)    │  │  │  MongoDB Atlas                           │  │
│  │  db.t3.large         │  │  │  M10 tier                                │  │
│  │                      │  │  │                                          │  │
│  │  Primary instance:   │  │  │  Collections:                            │  │
│  │  - users             │  │  │  - activity_logs                         │  │
│  │  - accounts          │  │  │    {userId, action, timestamp, data}     │  │
│  │  - transactions      │  │  │  - educational_content                   │  │
│  │  - budgets           │  │  │    {courseId, lessons[], videos[]}       │  │
│  │  - goals             │  │  │  - notification_templates                │  │
│  │  - orders            │  │  │    {type, subject, body, variables[]}    │  │
│  │  - portfolio         │  │  │  - analytics_events                      │  │
│  │  - ledger            │  │  │    {event, properties, userId, date}     │  │
│  │  - subscriptions     │  │  │                                          │  │
│  │                      │  │  │  Used by:                                │  │
│  │  Read Replica:       │  │  │  - Core API (read/write)                 │  │
│  │  - Used by:          │  │  │  - Notification Service (read templates) │  │
│  │    Investment Engine │  │  │  - Analytics queries                     │  │
│  │    (portfolio calcs) │  │  │                                          │  │
│  │    Analytics queries │  │  └──────────────────────────────────────────┘  │
│  │                      │  │                                                │
│  │  Written by:         │  │                                                │
│  │  - Core API          │  │                                                │
│  │  - Transaction Svc   │  │                                                │
│  │                      │  │                                                │
│  │  Read by:            │  │                                                │
│  │  - All services      │  │                                                │
│  └──────────────────────┘  │                                                │
│                            │                                                │
└────────────────────────────┴────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                      EXTERNAL INTEGRATIONS                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌──────────────────┐     │
│  │ Plaid / TrueLayer   │  │ Stripe              │  │ Broker APIs      │     │
│  │ (Bank aggregation)  │  │ (Subscriptions)     │  │ (ETF trading)    │     │
│  │                     │  │                     │  │                  │     │
│  │ Used by: Core API   │  │ Used by: Core API   │  │ Used by:         │     │
│  │ Flow:               │  │ Flow:               │  │ Transaction Svc  │     │
│  │ 1. User connects    │  │ 1. User subscribes  │  │                  │     │
│  │    bank account     │  │ 2. Create customer  │  │ Flow:            │     │
│  │ 2. Exchange token   │  │ 3. Attach payment   │  │ 1. Receive order │     │
│  │ 3. Fetch balances   │  │ 4. Create sub       │  │ 2. Execute trade │     │
│  │ 4. Sync daily       │  │ 5. Webhook for      │  │ 3. Confirm       │     │
│  │                     │  │    payment events   │  │ 4. Update ledger │     │
│  └─────────────────────┘  └─────────────────────┘  └──────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

```

---

## ⚙️**Detailed Flow Examples**

### **Flow 1: User Places Investment Order**

```
1. User Action (Mobile App)
   │
   ├─> User taps "Invest €500 in Growth Portfolio"
   │
   ▼
2. API Request
   │
   ├─> POST /api/v1/investments/orders
   │   Headers: Authorization: Bearer <JWT>
   │   Body: {amount: 500, portfolioType: "growth"}
   │
   ▼
3. ALB Routing
   │
   ├─> ALB receives request
   ├─> Checks health of Core API instances
   ├─> Routes to healthy Core API instance
   │
   ▼
4. Core API Processing (NestJS)
   │
   ├─> Auth middleware validates JWT (checks Redis session)
   ├─> Controller: InvestmentController.createOrder()
   ├─> Service: InvestmentService.validateAndCreate()
   │   │
   │   ├─> Check user balance in PostgreSQL
   │   ├─> Verify KYC status
   │   ├─> Create pending order record in PostgreSQL
   │   │   INSERT INTO orders (user_id, amount, status, type)
   │   │   VALUES (123, 500, 'pending', 'growth')
   │   │
   │   ├─> Publish event to RabbitMQ
   │   │   Exchange: "orders"
   │   │   Routing Key: "order.created"
   │   │   Payload: {orderId: 456, userId: 123, amount: 500, type: "growth"}
   │   │
   │   └─> Return response to user
   │       {orderId: 456, status: "pending", message: "Processing..."}
   │
   ▼
5. Investment Engine Consumes Event (Go)
   │
   ├─> RabbitMQ Consumer listening on "investment.orders" queue
   ├─> Receives event: {orderId: 456, ...}
   ├─> Processor: CalculateAllocation()
   │   │
   │   ├─> Fetch user risk profile from PostgreSQL (read replica)
   │   ├─> Calculate ETF allocation:
   │   │   Growth portfolio = 70% stocks, 30% bonds
   │   │   €500 → €350 to VUSA.L, €150 to VGOV.L
   │   │
   │   ├─> Write allocation to PostgreSQL
   │   │   UPDATE orders SET allocation = {...}, status = 'calculated'
   │   │
   │   └─> Publish event to RabbitMQ
   │       Exchange: "orders"
   │       Routing Key: "allocation.calculated"
   │       Payload: {orderId: 456, allocation: [{etf: "VUSA.L", amount: 350}, ...]}
   │
   ▼
6. Transaction Service Consumes Event (Go)
   │
   ├─> RabbitMQ Consumer listening on "transaction.orders" queue
   ├─> Receives event: {orderId: 456, allocation: [...]}
   ├─> Processor: ExecuteTrade()
   │   │
   │   ├─> Begin PostgreSQL transaction
   │   ├─> Debit user account: UPDATE accounts SET balance = balance - 500
   │   ├─> Call broker API to execute trades
   │   │   POST https://broker-api.com/orders
   │   │   Body: [{symbol: "VUSA.L", quantity: 10, type: "buy"}, ...]
   │   │
   │   ├─> Record in ledger table
   │   │   INSERT INTO ledger (user_id, type, amount, description)
   │   │   VALUES (123, 'debit', 500, 'Investment order 456')
   │   │
   │   ├─> Update order status
   │   │   UPDATE orders SET status = 'completed', executed_at = NOW()
   │   │
   │   ├─> Commit transaction
   │   │
   │   └─> Publish event to RabbitMQ
   │       Exchange: "notifications"
   │       Routing Key: "order.completed"
   │       Payload: {orderId: 456, userId: 123, amount: 500}
   │
   ▼
7. Notification Service Consumes Event (NestJS)
   │
   ├─> RabbitMQ Consumer listening on "email.notifications" queue
   ├─> Receives event: {orderId: 456, userId: 123, ...}
   ├─> Processor: SendOrderConfirmation()
   │   │
   │   ├─> Fetch notification template from MongoDB
   │   │   db.notification_templates.findOne({type: "order_completed"})
   │   │   Returns: {subject: "Investment Complete", body: "Hi {{name}}..."}
   │   │
   │   ├─> Fetch user details from PostgreSQL
   │   │   SELECT email, name FROM users WHERE id = 123
   │   │
   │   ├─> Render template with user data
   │   │
   │   ├─> Add job to BullMQ email queue
   │   │   emailQueue.add('send-email', {
   │   │     to: 'user@example.com',
   │   │     subject: 'Investment Complete',
   │   │     body: '<html>...',
   │   │     priority: 1
   │   │   })
   │   │
   │   └─> Job stored in Redis "email-queue"
   │
   ▼
8. BullMQ Email Worker Processes Job
   │
   ├─> Worker polling Redis "email-queue" (blocking BLPOP command)
   ├─> Picks up job: {to: 'user@example.com', subject: '...', body: '...'}
   ├─> Processor: SendEmailHandler()
   │   │
   │   ├─> Call Twilio SendGrid API
   │   │   POST https://api.sendgrid.com/v3/mail/send
   │   │   Body: {personalizations: [...], from: {...}, content: [...]}
   │   │
   │   ├─> On success: Mark job complete, remove from Redis
   │   ├─> On failure: Retry (3 attempts with exponential backoff)
   │   │   Attempt 1: immediate
   │   │   Attempt 2: after 1 minute
   │   │   Attempt 3: after 5 minutes
   │   │   If all fail: move to dead-letter queue for manual review
   │   │
   │   └─> Log delivery status to MongoDB
   │       db.notification_logs.insert({
   │         userId: 123, type: 'email', status: 'sent', sentAt: NOW()
   │       })
   │
   ▼
9. Push Notification (Parallel)
   │
   ├─> Notification Service also adds push job to BullMQ
   ├─> pushQueue.add('send-push', {
   │     userId: 123,
   │     title: 'Investment Complete',
   │     body: 'Your €500 investment is now active',
   │     deviceTokens: ['fcm-token-abc123']
   │   })
   │
   ├─> Push Worker picks up job from Redis "push-queue"
   ├─> Calls AWS SNS
   │   sns.publish({
   │     Message: 'Your €500 investment is now active',
   │     TargetArn: 'arn:aws:sns:...'
   │   })
   │
   └─> User receives push notification on phone
       "💰 Investment Complete: Your €500 is now invested!"

```

**Total time from user tap to completion:**

- User sees "Processing..." response: ~200ms
- Order executed and confirmed: ~2-3 seconds (async)
- Email/push received: ~5-10 seconds (async, queued)

---

### **Flow 2: Scheduled Recurring Investment (BullMQ Cron)**

```
1. User Setup (One-time)
   │
   ├─> User enables "Auto-invest €200 on 1st of every month"
   ├─> Core API creates recurring job
   │   recurringQueue.add('monthly-investment', {
   │     userId: 123,
   │     amount: 200,
   │     portfolioType: 'balanced'
   │   }, {
   │     repeat: {cron: '0 0 1 * *'}  // At 00:00 on day 1 of month
   │   })
   │
   └─> Job definition stored in Redis with cron metadata
       Redis Key: bull:recurring-queue:repeat:abc123
       Value: {pattern: '0 0 1 * *', jobData: {...}}

2. BullMQ Scheduler (Built-in)
   │
   ├─> BullMQ internal scheduler checks Redis every minute
   ├─> On Jan 1, 2026 at 00:00:
   │   Scheduler sees cron '0 0 1 * *' should trigger
   │   Creates new job instance in "scheduled-jobs-queue"
   │
   └─> Worker can now process this job instance

3. Scheduled Jobs Worker Processes
   │
   ├─> Worker polls "scheduled-jobs-queue" in Redis
   ├─> Picks up job: {userId: 123, amount: 200, type: 'balanced'}
   ├─> Processor: ProcessRecurringInvestment()
   │   │
   │   ├─> Fetch user account from PostgreSQL
   │   ├─> Check balance (must have ≥ €200)
   │   ├─> If sufficient:
   │   │   │
   │   │   ├─> Publish "order.created" event to RabbitMQ
   │   │   │   (Same flow as manual order from Flow 1)
   │   │   │
   │   │   └─> Log to MongoDB
   │   │       db.activity_logs.insert({
   │   │         userId: 123,
   │   │         action: 'recurring_investment_triggered',
   │   │         amount: 200
   │   │       })
   │   │
   │   └─> If insufficient balance:
   │       │
   │       ├─> Send notification: "Recurring investment failed (low balance)"
   │       └─> Retry tomorrow (add delayed job)
   │           recurringQueue.add('retry-investment', {...}, {delay: 86400000})
   │
   └─> Next month (Feb 1), scheduler triggers again automatically

```

---

### **Flow 3: User Views Dashboard (Read Path)**

```
1. User Opens App
   │
   ├─> Mobile app loads Dashboard screen
   ├─> Makes API call: GET /api/v1/dashboard
   │   Headers: Authorization: Bearer <JWT>
   │
   ▼
2. ALB Routes to Core API
   │
   ├─> ALB receives request
   ├─> Routes to Core API instance based on least connections
   │
   ▼
3. Core API Processing (NestJS)
   │
   ├─> Auth middleware validates JWT
   │   │
   │   ├─> Check Redis for session
   │   │   Redis GET "session:token:abc123"
   │   │   Returns: {userId: 123, email: "user@example.com", exp: ...}
   │   │
   │   └─> If valid, proceed; if expired, return 401 Unauthorized
   │
   ├─> Controller: DashboardController.getDashboard()
   ├─> Service: DashboardService.fetchUserDashboard(userId: 123)
   │   │
   │   ├─> Step 1: Check Redis cache first
   │   │   Redis GET "dashboard:user:123"
   │   │   │
   │   │   ├─> If cache hit: return cached JSON (TTL: 5 minutes)
   │   │   │   Response time: ~5-10ms
   │   │   │
   │   │   └─> If cache miss: proceed to fetch from databases
   │   │
   │   ├─> Step 2: Fetch data from PostgreSQL (*primary*)
   │   │   │
   │   │   ├─> Query 1: Account balance
   │   │   │   SELECT balance, currency FROM accounts
   │   │   │   WHERE user_id = 123
   │   │   │   Returns: {balance: 5420.50, currency: "EUR"}
   │   │   │
   │   │   ├─> Query 2: Budget summary (current month)
   │   │   │   SELECT category, spent, budget_limit
   │   │   │   FROM budgets
   │   │   │   WHERE user_id = 123 AND month = '2026-01'
   │   │   │   Returns: [{category: "food", spent: 450, limit: 600}, ...]
   │   │   │
   │   │   └─> Query 3: Goals progress
   │   │       SELECT goal_name, target_amount, current_amount
   │   │       FROM goals
   │   │       WHERE user_id = 123 AND status = 'active'
   │   │       Returns: [{name: "Emergency Fund", target: 10000, current: 3200}, ...]
   │   │
   │   ├─> Step 3: Fetch portfolio data from PostgreSQL (*read replica*)
   │   │   │
   │   │   ├─> Query to read replica (reduces load on *primary*)
   │   │   │    SELECT p.portfolio_type, SUM(h.quantity * h.current_price) as value
   │   │   │    FROM portfolios p
   │   │   │    JOIN holdings h ON p.id = h.portfolio_id
   │   │   │    WHERE p.user_id = 123
   │   │   │    GROUP BY p.portfolio_type
   │   │   │    Returns: [{type: "growth", value: 12350.75}, ...]
   │   │   │
   │   │   └─> Get current ETF prices from Redis cache
   │   │       Redis MGET "etf:price:VUSA.L" "etf:price:VGOV.L"
   │   │       (Prices cached from market data feed, updated every 15 min)
   │   │
   │   ├─> Step 4: Fetch recent activity from MongoDB
   │   │   │
   │   │   └─> Query MongoDB for last 10 activities
   │   │       db.activity_logs.find({userId: 123})
   │   │         .sort({timestamp: -1})
   │   │         .limit(10)
   │   │       Returns: [
   │   │         {action: "investment_completed", amount: 500, date: "2026-01-18"},
   │   │         {action: "budget_updated", category: "food", date: "2026-01-15"},
   │   │         ...
   │   │       ]
   │   │
   │   ├─> Step 5: Aggregate all data
   │   │   dashboardData = {
   │   │     balance: 5420.50,
   │   │     portfolioValue: 12350.75,
   │   │     budgets: [...],
   │   │     goals: [...],
   │   │     recentActivity: [...]
   │   │   }
   │   │
   │   ├─> Step 6: Cache in Redis for next request
   │   │   Redis SETEX "dashboard:user:123" 300 JSON.stringify(dashboardData)
   │   │   (TTL: 300 seconds = 5 minutes)
   │   │
   │   └─> Return dashboardData
   │
   └─> Response sent to mobile app
       {
         balance: 5420.50,
         portfolioValue: 12350.75,
         budgets: [{category: "food", spent: 450, limit: 600}, ...],
         goals: [{name: "Emergency Fund", progress: 32}, ...],
         recentActivity: [...]
       }

```

**Response times:**

- Cache hit: ~5-10ms (Redis lookup only)
- Cache miss: ~150-250ms (multiple DB queries + aggregation)
- User perception: Dashboard loads instantly on repeat views

---

### **Flow 4: Bank Account Sync (Background Job)**

```
1. Daily Sync Trigger (Scheduled Job)
   │
   ├─> BullMQ cron job: "0 3 * * *" (runs at 3 AM every day)
   ├─> Scheduled Jobs Worker picks up job
   ├─> Processor: SyncAllUserBankAccounts()
   │   │
   │   ├─> Fetch all users with connected bank accounts
   │   │   SELECT user_id, account_id, plaid_access_token
   │   │   FROM bank_connections
   │   │   WHERE status = 'active'
   │   │   Returns: 120,000 users (out of 450K total)
   │   │
   │   └─> Process in batches of 100 users
   │       For each batch, add individual sync jobs to BullMQ
   │       syncQueue.addBulk([
   │         {name: 'sync-user-account', data: {userId: 1, token: "..."}},
   │         {name: 'sync-user-account', data: {userId: 2, token: "..."}},
   │         ... (100 jobs)
   │       ])
   │
   ▼
2. Sync Worker Processes Individual Jobs
   │
   ├─> Worker polls "sync-queue" from Redis
   ├─> Picks up job: {userId: 123, plaidAccessToken: "access-prod-..."}
   ├─> Processor: SyncUserBankAccount()
   │   │
   │   ├─> Call Plaid API to fetch transactions
   │   │   POST https://production.plaid.com/transactions/get
   │   │   Body: {
   │   │     access_token: "access-prod-...",
   │   │     start_date: "2026-01-01",
   │   │     end_date: "2026-01-19"
   │   │   }
   │   │   Returns: {
   │   │     transactions: [
   │   │       {id: "tx1", amount: -45.20, name: "Starbucks", date: "2026-01-18"},
   │   │       {id: "tx2", amount: -120.00, name: "Grocery Store", date: "2026-01-17"},
   │   │       ...
   │   │     ]
   │   │   }
   │   │
   │   ├─> For each transaction, check if already exists
   │   │   SELECT id FROM transactions
   │   │   WHERE user_id = 123 AND external_id = 'tx1'
   │   │   │
   │   │   ├─> If not exists: Insert new transaction
   │   │   │   INSERT INTO transactions
   │   │   │   (user_id, external_id, amount, merchant, date, category)
   │   │   │   VALUES (123, 'tx1', -45.20, 'Starbucks', '2026-01-18', 'food')
   │   │   │
   │   │   └─> Auto-categorize using *ML model* (simple keyword matching)
   │   │       "Starbucks" → category: "food"
   │   │       "Shell Gas" → category: "transportation"
   │   │
   │   ├─> Update account balance
   │   │   UPDATE accounts
   │   │   SET balance = (SELECT SUM(amount) FROM transactions WHERE user_id = 123),
   │   │       last_synced_at = NOW()
   │   │   WHERE user_id = 123
   │   │
   │   ├─> Publish event to RabbitMQ
   │   │   Exchange: "events"
   │   │   Routing Key: "account.synced"
   │   │   Payload: {userId: 123, newTransactions: 15}
   │   │
   │   └─> Clear dashboard cache in Redis
   │       Redis DEL "dashboard:user:123"
   │       (Force refresh on next app open)
   │
   ▼
3. Budget Module Consumes Event (Within Core API)
   │
   ├─> Event listener: onAccountSynced(event)
   ├─> Check if any budget limits exceeded
   │   │
   │   ├─> Query current month spending by category
   │   │   SELECT category, SUM(amount) as spent
   │   │   FROM transactions
   │   │   WHERE user_id = 123 AND date >= '2026-01-01'
   │   │   GROUP BY category
   │   │
   │   ├─> Compare with budget limits
   │   │   SELECT category, budget_limit
   │   │   FROM budgets
   │   │   WHERE user_id = 123
   │   │
   │   ├─> If exceeded:
   │   │   Food: spent €650 / limit €600 (108% - EXCEEDED!)
   │   │   │
   │   │   └─> Publish notification event
   │   │       Exchange: "notifications"
   │   │       Routing Key: "budget.exceeded"
   │   │       Payload: {
   │   │         userId: 123,
   │   │         category: "food",
   │   │         spent: 650,
   │   │         limit: 600,
   │   │         percentage: 108
   │   │       }
   │   │
   │   └─> Notification Service picks up and sends push
   │       "⚠️ Budget Alert: You've spent €650 on food this month (limit: €600)"

```

**Processing time:**

- Syncing 120,000 users with 10 concurrent workers: ~2-3 hours
- Each individual sync: ~2-5 seconds (Plaid API + DB operations)

---

### **Flow 5: Portfolio Rebalancing (Weekly Scheduled Job)**

```
1. Weekly Trigger (Saturday 2 AM)
   │
   ├─> BullMQ cron job: "0 2 * * 6" (Saturday at 2 AM)
   ├─> Scheduled Jobs Worker picks up job
   ├─> Processor: RebalanceAllPortfolios()
   │   │
   │   ├─> Fetch all users who need rebalancing
   │   │   SELECT user_id, portfolio_id, target_allocation
   │   │   FROM portfolios
   │   │   WHERE auto_rebalance = true
   │   │   AND last_rebalanced_at < NOW() - INTERVAL '30 days'
   │   │   Returns: 85,000 portfolios
   │   │
   │   └─> Add individual rebalancing jobs to BullMQ
   │       rebalanceQueue.addBulk([
   │         {name: 'rebalance-portfolio', data: {portfolioId: 1001}},
   │         {name: 'rebalance-portfolio', data: {portfolioId: 1002}},
   │         ... (batches of 500)
   │       ])
   │
   ▼
2. Worker Calls Investment Engine Service
   │
   ├─> Worker polls "rebalance-queue" from Redis
   ├─> Picks up job: {portfolioId: 1001}
   ├─> Makes HTTP call to Investment Engine (Go service)
   │   │
   │   ├─> POST http://investment-engine:8080/api/rebalance
   │   │   Body: {portfolioId: 1001}
   │   │
   │   └─> Investment Engine processes:
   │
   ▼
3. Investment Engine Calculates (Go)
   │
   ├─> Fetch portfolio details from PostgreSQL (*read replica*)
   │   SELECT user_id, portfolio_type, target_allocation
   │   FROM portfolios WHERE id = 1001
   │   Returns: {
   │     userId: 123,
   │     type: "balanced",
   │     target: {stocks: 60%, bonds: 40%}
   │   }
   │
   ├─> Fetch current holdings
   │   SELECT etf_symbol, quantity, current_price
   │   FROM holdings
   │   WHERE portfolio_id = 1001
   │   Returns: [
   │     {symbol: "VUSA.L", quantity: 50, price: 85.50}, // Value: €4,275
   │     {symbol: "VGOV.L", quantity: 100, price: 25.30} // Value: €2,530
   │   ]
   │   Total portfolio value: €6,805
   │
   ├─> Calculate current allocation
   │   Stocks: €4,275 / €6,805 = 62.8%
   │   Bonds: €2,530 / €6,805 = 37.2%
   │
   ├─> Check if rebalancing needed (threshold: ±5%)
   │   Target: 60% stocks, 40% bonds
   │   Current: 62.8% stocks, 37.2% bonds
   │   Deviation: stocks +2.8%, bonds -2.8%
   │   │
   │   ├─> Deviation < 5% threshold → NO REBALANCING NEEDED
   │   │   Log to database and skip
   │   │
   │   └─> (If deviation was >5%, would calculate trades):
   │       Example if stocks were 70%:
   │       - Sell €680 of VUSA.L (bring from 70% to 60%)
   │       - Buy €680 of VGOV.L (bring from 30% to 40%)
   │       - Publish order events to RabbitMQ
   │       - Transaction Service executes trades
   │
   └─> Update last_rebalanced_at in PostgreSQL
       UPDATE portfolios
       SET last_rebalanced_at = NOW(),
           last_rebalance_result = 'no_action_needed'
       WHERE id = 1001

```

**Processing time:**

- Checking 85,000 portfolios with 20 concurrent workers: ~1-2 hours
- Actual rebalancing needed: ~5-10% of portfolios (drift > 5%)

---

## **Key Architectural Patterns Explained**

### **1. Why Separate Workers for BullMQ?**

**Question**: Are BullMQ workers separate processes?

**Answer**: Yes, but they run **within the same service containers**.

**Example in Notification Service:**

```tsx
// notification-service/src/main.ts
async function bootstrap() {
  // 1. Start NestJS HTTP server (handles RabbitMQ consumers)
  const app = await NestFactory.create(AppModule);
  await app.listen(3000);

  // 2. Start BullMQ workers (separate Node.js processes/threads)
  const emailWorker = new Worker('email-queue', async (job) => {
    await sendEmailViaSendGrid(job.data);
  }, { connection: redisConnection });

  const pushWorker = new Worker('push-queue', async (job) => {
    await sendPushViaSNS(job.data);
  }, { connection: redisConnection });

  // Both workers run concurrently with HTTP server
}

```

**How it works:**

- **Main process**: NestJS app handles HTTP requests + RabbitMQ consumers
- **Worker threads**: BullMQ workers poll Redis queues in background
- **Running in same ECS task**: One Docker container runs both (efficient resource usage)

**Why this approach?**

- Simpler deployment (one container, not 5 separate services)
- Workers share Redis connection pool
- Easier monitoring (all logs in one place)

---

### **2. API Gateway vs Application Load Balancer**

**Question**: Should there be a separate API Gateway?

**Answer**: At Series A scale, **ALB acts as the API Gateway**.

**What ALB provides:**

- **Path-based routing**:
    - `/api/v1/users/*` → Core API
    - `/api/v1/investments/*` → Investment Engine
    - `/api/v1/transactions/*` → Transaction Service
- **SSL/TLS termination**: HTTPS → HTTP to backend
- **Health checks**: Only routes to healthy instances
- **Rate limiting**: Via AWS WAF (Web Application Firewall)
- **Basic authentication**: Can validate JWT signatures at ALB level (advanced config)

**When to add a dedicated API Gateway (AWS API Gateway or Kong)?**

- Series B+ when you need:
    - Request/response transformation
    - Complex routing rules (canary deployments, A/B testing)
    - API versioning with deprecation
    - Developer portal for external APIs
    - Fine-grained rate limiting per endpoint

**For now**: ALB is simpler, cheaper ($25/month vs $200+ for API Gateway), and sufficient.

---

### **3. Database Read/Write Splitting**

**PostgreSQL Setup:**

```
┌─────────────────────────────────────────┐
│         PostgreSQL (RDS)                    │
├─────────────────────────────────────────┤
│                                             │
│  Primary Instance (Writes)                  │
│  - All INSERT, UPDATE, DELETE               │
│  - Used by:                                 │
│    • Core API (user writes)                 │
│    • Transaction Service (orders)           │
│                                             │
│  ↓ Replication (async, ~1 sec lag)          │
│                                             │
│  Read Replica (Reads only)                  │
│  - SELECT queries for heavy reports         │
│  - Used by:                                 │
│    • Investment Engine (portfolio calc)     │
│    • Analytics queries                      │
│    • Dashboard data (tolerates slight       │
│      staleness for performance)             │
│                                             │
└─────────────────────────────────────────┘

```

**Why this matters:**

- **Primary** handles ~1,000 writes/sec (orders, transactions, updates)
- **Replica** handles ~5,000 reads/sec (dashboards, reports, calculations)
- Separating prevents read-heavy queries from slowing down writes
- Cost: +50% ($200 → $300/month for RDS), but massive performance gain

---

### **4. Caching Strategy (Redis Usage)**

**What's cached:**

1. **Session data** (TTL: 7 days)
    - Key: `session:token:abc123`
    - Value: `{userId: 123, email: "...", exp: 1737504000}`
2. **Dashboard data** (TTL: 5 minutes)
    - Key: `dashboard:user:123`
    - Value: `{balance: 5420.50, portfolioValue: 12350.75, ...}`
3. **ETF prices** (TTL: 15 minutes)
    - Key: `etf:price:VUSA.L`
    - Value: `85.50`
    - Updated by background job every 15 min
4. **Rate limiting counters** (TTL: 1 minute)
    - Key: `ratelimit:user:123:2026-01-19:14:30`
    - Value: `47` (request count in this minute)
    - Max: 100 requests/minute

✨**`Cache invalidation`:**

- **On write**: When user updates budget, delete `dashboard:user:123`
- **On sync**: When bank account syncs, delete dashboard cache
- **Time-based**: ETF prices auto-expire after 15 minutes

---

### **5. Event Flow Summary**

**RabbitMQ vs BullMQ - When to Use What?**

| Scenario | Use RabbitMQ | Use BullMQ |
| --- | --- | --- |
| Real-time event between services | ✅ Yes | ❌ No |
| Scheduled/delayed job | ❌ No | ✅ Yes |
| Retry with exponential backoff | ❌ No | ✅ Yes |
| Cron-like recurring task | ❌ No | ✅ Yes |
| Fan-out to multiple consumers | ✅ Yes | ❌ No |
| Priority queues | ❌ Use BullMQ | ✅ Yes |

**Example:**

- **Order created** → RabbitMQ (need immediate processing by multiple services)
- **Send email** → BullMQ (can delay, retry, prioritize)
- **Monthly investment** → BullMQ (scheduled cron job)

---

## **Deployment Architecture Details**

### **How Services Are Deployed**

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS CLOUD (eu-west-1)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────┐      │
│  │  ***Elastic Beanstalk Environment***: "Core API"  │      │
│  │                                                   │      │
│  │  ┌──────────────┐  ┌──────────────┐               │      │
│  │  │ EC2 Instance │  │ EC2 Instance │               │      │
│  │  │ t3.medium    │  │ t3.medium    │               │      │
│  │  │              │  │              │               │      │
│  │  │ Docker:      │  │ Docker:      │               │      │
│  │  │ Core API     │  │ Core API     │               │      │
│  │  │ (NestJS)     │  │ (NestJS)     │               │      │
│  │  │              │  │              │               │      │
│  │  │ + BullMQ     │  │ + BullMQ     │               │      │
│  │  │   Scheduled  │  │   Scheduled  │               │      │
│  │  │   Jobs       │  │   Jobs       │               │      │
│  │  │   Worker     │  │   Worker     │               │      │
│  │  └──────────────┘  └──────────────┘               │      │
│  │         │                    │                    │      │
│  │         └──────────┬─────────┘                    │      │
│  │                    │                              │      │
│  │         ┌──────────▼──────────┐                   │      │
│  │         │  Application Load   │                   │      │
│  │         │  Balancer           │                   │      │
│  │         └─────────────────────┘                   │      │
│  └───────────────────────────────────────────────────┘      │
│                                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │  ***ECS Cluster***: "finverse-services"            │     │
│  │                                                    │     │
│  │  ┌──────────────────────────────────────────┐      │     │
│  │  │ ECS Service: "investment-engine"         │      │     │
│  │  │                                          │      │     │
│  │  │ ┌─────────────┐  ┌─────────────┐         │      │     │
│  │  │ │ Fargate Task│  │ Fargate Task│         │      │     │
│  │  │ │ (Go binary) │  │ (Go binary) │         │      │     │
│  │  │ │ 0.5 vCPU    │  │ 0.5 vCPU    │         │      │     │
│  │  │ │ 1 GB RAM    │  │ 1 GB RAM    │         │      │     │
│  │  │ └─────────────┘  └─────────────┘         │      │     │
│  │  └──────────────────────────────────────────┘      │     │
│  │                                                    │     │
│  │  ┌──────────────────────────────────────────┐      │     │
│  │  │ ECS Service: "transaction-service"       │      │     │
│  │  │                                          │      │     │
│  │  │ ┌─────────────┐  ┌─────────────┐         │      │     │
│  │  │ │ Fargate Task│  │ Fargate Task│         │      │     │
│  │  │ │ (Go binary) │  │ (Go binary) │         │      │     │
│  │  │ │ 0.5 vCPU    │  │ 0.5 vCPU    │         │      │     │
│  │  │ │ 1 GB RAM    │  │ 1 GB RAM    │         │      │     │
│  │  │ └─────────────┘  └─────────────┘         │      │     │
│  │  └──────────────────────────────────────────┘      │     │
│  │                                                    │     │
│  │  ┌──────────────────────────────────────────┐      │     │
│  │  │ ECS Service: "notification-service"      │      │     │
│  │  │                                          │      │     │
│  │  │ ┌─────────────┐                          │      │     │
│  │  │ │ Fargate Task│                          │      │     │
│  │  │ │ (NestJS)    │                          │      │     │
│  │  │ │ 0.5 vCPU    │                          │      │     │
│  │  │ │ 1 GB RAM    │                          │      │     │
│  │  │ │             │                          │      │     │
│  │  │ │ + BullMQ    │                          │      │     │
│  │  │ │   Email     │                          │      │     │
│  │  │ │   Push      │                          │      │     │
│  │  │ │   SMS       │                          │      │     │
│  │  │ │   Workers   │                          │      │     │
│  │  │ └─────────────┘                          │      │     │
│  │  └──────────────────────────────────────────┘      │     │
│  └────────────────────────────────────────────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

```

---

This architecture is **realistic for Series A**, balances **simplicity with scalability**, and avoids over-engineering while maintaining clear separation of concerns. The system can handle **450K users** today and scale to **1.5M+ users** at Series B with minimal changes.