# FinVerse — Step 6: RabbitMQ

---

## Part 1 — What Is RabbitMQ and Why Does FinVerse Need It?

### The Problem It Solves — Start Here

Before understanding RabbitMQ, understand the problem it solves. Because in an interview, "why did you use RabbitMQ" is always the first question.

Imagine this scenario without RabbitMQ:

A user's monthly budget threshold gets crossed. Core Product detects this. It needs to notify the user. So it directly calls the Notification Service's API:

```
Core Product ──── HTTP POST /notify ────► Notification Service
```

This seems simple. But think about what can go wrong:

**Problem 1 — Tight coupling.**
Core Product now needs to know that Notification Service exists, where it lives, and what its API looks like. If Notification Service changes its API, Core Product breaks. If you add a second consumer — say, an Analytics Service that also wants to know about budget threshold events — you have to modify Core Product again to call that too. Every new consumer means a code change in Core Product.

**Problem 2 — Availability dependency.**
If Notification Service is down — deploying, crashing, scaling — Core Product's HTTP call fails. You either lose the notification entirely or you have to build retry logic inside Core Product. Now Core Product has retry logic, timeout handling, and circuit breakers — complexity that has nothing to do with its core responsibility.

**Problem 3 — No backpressure handling.**
Imagine end-of-month: thousands of users simultaneously cross their budget thresholds. Core Product fires thousands of HTTP calls to Notification Service simultaneously. Notification Service gets overwhelmed and starts dropping requests. There is no natural way to say "slow down, process these at a rate I can handle."

**RabbitMQ solves all three problems in one move:**

```
Core Product ──── publishes event ────► RabbitMQ ──── delivers when ready ────► Notification Service
```

- Core Product does not know Notification Service exists — it just fires an event into RabbitMQ
- If Notification Service is down, messages queue up safely and are delivered when it recovers
- Notification Service consumes at its own pace — natural backpressure
- Adding Analytics Service as a second consumer requires zero changes to Core Product

---

### What RabbitMQ Actually Is

RabbitMQ is a **message broker** — a middleman that accepts messages from producers and delivers them to consumers.

Three core concepts you must understand:

**Producer** — the application that sends a message. In FinVerse: Core Product, Payment Service.

**Consumer** — the application that receives and processes messages. In FinVerse: Notification Service, and sometimes Core Product itself (consuming events from Payment Service).

**Message** — the data being sent. In FinVerse, messages are JSON payloads representing domain events:
```json
{
  "eventType": "budget.threshold.exceeded",
  "userId": "usr_123",
  "category": "Dining Out",
  "spent": 187.50,
  "limit": 200.00
}
```

But here is the key thing that makes RabbitMQ different from a simple HTTP call — **the producer and consumer are completely decoupled in time and space.** The producer fires the message and moves on immediately. The consumer processes it whenever it is ready. They never talk to each other directly.

---

## Part 2 — RabbitMQ Architecture: The Core Building Blocks

This is where most explanations lose people. Let me build it up piece by piece.

### The Naive Mental Model (Wrong but Useful to Start)

You might first think RabbitMQ works like this:

```
Producer ──── message ────► Queue ──── message ────► Consumer
```

Producer sends to a queue, consumer reads from a queue. Simple.

This works — but it is too limited. What if you want the same message to go to multiple queues? What if you want certain messages to go to Queue A and other messages to go to Queue B based on their content? The simple model cannot handle this.

This is why RabbitMQ introduces the **Exchange**.

---

### The Real Model: Producer → Exchange → Queue → Consumer

```
Producer
   │
   │  publishes message with routing key
   ▼
Exchange
   │
   │  routes message to one or more queues
   │  based on exchange type + routing key + bindings
   ▼
Queue(s)
   │
   │  stores messages until consumer is ready
   ▼
Consumer
```

The **Exchange** is the routing brain of RabbitMQ. The producer never sends directly to a queue — it sends to an exchange with a **routing key** (a string label describing what the message is). The exchange then decides which queue(s) receive the message, based on its type and the **bindings** configured between the exchange and queues.

Let me explain each exchange type clearly.

---

### Exchange Types — The Four Types You Must Know

#### 1. Direct Exchange

**How it works:** Routes a message to a queue if the message's routing key **exactly matches** the binding key of that queue.

```
Producer publishes:
  exchange: "payments"
  routing key: "payment.succeeded"

Bindings:
  Queue "payment-success-queue" bound with key "payment.succeeded"
  Queue "payment-failed-queue" bound with key "payment.failed"

Result:
  Message goes to "payment-success-queue" only
```

**Use case in FinVerse:** Payment Service uses a direct exchange for payment outcomes. `payment.succeeded` goes to one queue, `payment.failed` goes to another. Clean, predictable routing.

---

#### 2. Topic Exchange

**How it works:** Routes based on **pattern matching** of the routing key using wildcards:
- `*` matches exactly one word
- `#` matches zero or more words

Routing keys use dot notation: `budget.threshold.exceeded`, `user.registered`, `investment.order.paid`

```
Producer publishes:
  exchange: "finance.events"
  routing key: "budget.threshold.exceeded"

Bindings:
  Queue "notification-queue" bound with pattern "budget.#"
  Queue "analytics-queue"    bound with pattern "#"  (matches everything)
  Queue "budget-queue"       bound with pattern "budget.threshold.*"

Result:
  Message goes to all three queues
```

**Use case in FinVerse:** Core Product uses a topic exchange called `finance.events`. Notification Service binds with `#` (receives all events it cares about). This allows fine-grained routing — if Analytics Service only wants investment events, it binds with `investment.#`. No producer changes needed.

---

#### 3. Fanout Exchange

**How it works:** Ignores the routing key entirely. Delivers the message to **every queue bound to this exchange**, no exceptions.

```
Producer publishes:
  exchange: "system.broadcasts"
  routing key: (ignored)

All bound queues receive the message
```

**Use case in FinVerse:** Broadcasting system-wide events — for example, when FinVerse does a scheduled maintenance window, a fanout exchange broadcasts to all services simultaneously. Or when a user deletes their account — every service that holds user data needs to know immediately.

---

#### 4. Headers Exchange

**How it works:** Routes based on message header attributes rather than routing key. Rarely used in practice.

**Use case in FinVerse:** Not used. Topic exchange covers all FinVerse routing needs elegantly. Headers exchange adds complexity without benefit at this scale.

---

### FinVerse's Exchange & Queue Architecture

Here is the complete mapping of exchanges, routing keys, queues, and consumers in FinVerse:

```
┌─────────────────────────────────────────────────────────────┐
│                    RABBITMQ TOPOLOGY                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EXCHANGE: user.events (topic)                              │
│  Producer: Core Product                                     │
│                                                             │
│  Routing Key              Queue                Consumer     │
│  user.registered    ───►  user-notif-queue  ►  Notif Svc   │
│  user.verified      ───►  user-notif-queue  ►  Notif Svc   │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EXCHANGE: finance.events (topic)                           │
│  Producer: Core Product                                     │
│                                                             │
│  Routing Key                    Queue              Consumer │
│  budget.threshold.exceeded ───► budget-notif-q  ► Notif Svc│
│  goal.milestone.reached    ───► goal-notif-q    ► Notif Svc│
│  transaction.large.detected───► txn-alert-q     ► Notif Svc│
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EXCHANGE: payment.events (direct)                          │
│  Producer: Payment Service                                  │
│                                                             │
│  Routing Key              Queue                 Consumer    │
│  payment.succeeded  ───►  payment-success-q  ►  Core Prod  │
│                      ───►  payment-notif-q   ►  Notif Svc  │
│  payment.failed     ───►  payment-failed-q   ►  Core Prod  │
│                      ───►  payment-notif-q   ►  Notif Svc  │
│  subscription.      ───►  subscription-q     ►  Core Prod  │
│  activated                                                  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EXCHANGE: investment.events (topic)                        │
│  Producer: Payment Service                                  │
│                                                             │
│  Routing Key              Queue                 Consumer    │
│  investment.order.paid ►  invest-portfolio-q ►  Core Prod  │
│                        ►  invest-notif-q     ►  Notif Svc  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Key observation:** `payment.succeeded` goes to **two queues simultaneously** — `payment-success-q` (consumed by Core Product to update portfolio state) and `payment-notif-q` (consumed by Notification Service to send receipt email). This is one of RabbitMQ's most powerful capabilities — one event, multiple independent consumers, zero coupling between them. Payment Service published once and walked away.

---

## Part 3 — How Messages Flow: Producer to Consumer Step by Step

Let's trace a complete message journey using the `budget.threshold.exceeded` event as the example.

### Step 1 — Producer Publishes (Core Product)

Inside the Core Product NestJS service, after a transaction sync detects a budget threshold breach:

```typescript
// budget-alert.producer.ts
@Injectable()
export class BudgetAlertProducer {
  constructor(
    @Inject('RABBITMQ_CHANNEL')
    private readonly channel: Channel
  ) {}

  async publishBudgetAlert(data: BudgetAlertPayload): Promise<void> {
    const message = Buffer.from(JSON.stringify({
      eventType: 'budget.threshold.exceeded',
      timestamp: new Date().toISOString(),
      ...data
    }))

    this.channel.publish(
      'finance.events',          // exchange name
      'budget.threshold.exceeded', // routing key
      message,
      {
        persistent: true,        // message survives broker restart
        contentType: 'application/json',
        messageId: uuid(),       // unique ID for deduplication
      }
    )
  }
}
```

Two things to note:
- `persistent: true` — this tells RabbitMQ to write the message to disk, not just keep it in memory. If RabbitMQ restarts, the message survives.
- `messageId` — a unique ID per message, used by consumers for idempotency checks.

### Step 2 — Exchange Routes the Message

RabbitMQ's `finance.events` topic exchange receives the message with routing key `budget.threshold.exceeded`. It checks its bindings:

```
binding: "budget.#" → budget-notifications-queue ✅ (matches)
binding: "goal.#"   → goal-notifications-queue   ❌ (no match)
binding: "#"        → analytics-queue            ✅ (matches everything)
```

Message is copied and placed into `budget-notifications-queue` and `analytics-queue`.

### Step 3 — Queue Holds the Message

The message sits in `budget-notifications-queue`. It is persisted to disk (because `persistent: true` was set). It will stay here until:
- A consumer picks it up and acknowledges it
- It expires (if a TTL is configured — more on this later)
- It exceeds the queue length limit and gets dead-lettered (more on this too)

### Step 4 — Consumer Receives and Processes (Notification Service)

```typescript
// budget-alert.consumer.ts in Notification Service
@Injectable()
export class BudgetAlertConsumer implements OnModuleInit {
  constructor(
    @Inject('RABBITMQ_CHANNEL')
    private readonly channel: Channel,
    private readonly notificationService: NotificationService
  ) {}

  async onModuleInit() {
    await this.channel.consume(
      'budget-notifications-queue',
      async (message) => {
        if (!message) return

        const payload = JSON.parse(message.content.toString())

        try {
          await this.notificationService.sendBudgetAlert(payload)
          this.channel.ack(message)  // ✅ Tell RabbitMQ: processed successfully
        } catch (error) {
          // ❌ Processing failed — what happens here is critical
          // Covered in detail in Part 5
          this.channel.nack(message, false, true) // requeue for retry
        }
      },
      { noAck: false }  // Manual acknowledgement mode — ALWAYS use this
    )
  }
}
```

### Step 5 — Acknowledgement

This is one of the most important concepts in RabbitMQ.

When a consumer receives a message, RabbitMQ marks it as **unacknowledged** — it is delivered but not yet confirmed processed. The message stays tracked by RabbitMQ until one of three things happens:

```
channel.ack(message)
→ "I processed this successfully, remove it from the queue"
→ RabbitMQ deletes the message permanently

channel.nack(message, false, true)
→ "I failed to process this, put it back in the queue"
→ RabbitMQ requeues the message for retry

channel.nack(message, false, false)
→ "I failed and do not requeue"
→ Message goes to Dead Letter Queue (if configured)
```

**Why `noAck: false` is critical:**
If you set `noAck: true`, RabbitMQ considers the message delivered and done the moment it is sent to the consumer — before the consumer has processed it. If the consumer crashes mid-processing, the message is permanently lost. `noAck: false` (manual acknowledgement) means RabbitMQ holds the message until it receives an explicit `ack` or `nack`. This guarantees at-least-once delivery.

---

## Part 4 — Resilience & Failure Scenarios

This is the section that makes or breaks an interview on this topic. Every question below is one you will be asked.

---

### "What happens when the consumer is down?"

This is RabbitMQ's core value proposition. When Notification Service goes down — for a deployment, a crash, or a scaling event — messages do not disappear.

```
Core Product publishes budget.threshold.exceeded
         │
         ▼
RabbitMQ receives message
Notification Service is DOWN
         │
         ▼
Message sits in budget-notifications-queue
         │
    [persisted to disk — safe even if RabbitMQ restarts]
         │
    Notification Service comes back up
         │
         ▼
Consumer reconnects, RabbitMQ delivers queued messages
Notification Service processes them in order
```

Messages accumulate in the queue during the downtime. When the consumer recovers, it drains the backlog. The user might receive their budget alert a few minutes late — but they receive it. Nothing is lost.

**The important configuration that makes this work:**
Both the queue and the messages must be **durable/persistent**:

```typescript
// Queue declared as durable
await channel.assertQueue('budget-notifications-queue', {
  durable: true  // Queue survives RabbitMQ restart
})

// Messages published as persistent
channel.publish(exchange, routingKey, message, {
  persistent: true  // Message survives RabbitMQ restart
})
```

If `durable: false` — the queue is deleted when RabbitMQ restarts, along with all its messages. Never use this in production.

---

### "What happens when the consumer cannot process a message?"

This is the retry question. There are two scenarios:

**Scenario A — Transient failure (network blip, temporary downstream unavailability):**
The consumer failed to send the email because SendGrid was briefly unavailable. The message should be retried after a short delay.

**Scenario B — Permanent failure (malformed message, bug in consumer code):**
The message itself is bad — maybe it is missing a required field, or the userId does not exist. No amount of retrying will fix this. Retrying forever would just clog the queue.

RabbitMQ handles both scenarios through a combination of **retry with delay** and **Dead Letter Queue**.

---

### Retry Strategy — How FinVerse Does It

**The problem with immediate requeue:**

```typescript
channel.nack(message, false, true) // requeue immediately
```

This puts the message back at the front of the queue and the consumer picks it up again immediately. If the failure was a SendGrid outage lasting 30 seconds, you will retry hundreds of times in those 30 seconds — hammering SendGrid with failed requests, consuming CPU, and producing useless error logs.

**The solution — retry with exponential backoff using a delay queue:**

RabbitMQ does not have a built-in delay mechanism. FinVerse implements retry delays using a **retry exchange pattern**:

```
Original Queue: budget-notifications-queue
      │
      │ message fails (nack, do not requeue)
      ▼
Retry Exchange (direct)
      │
      │ routes to retry queue based on retry count header
      ▼
Retry Queue: budget-notifications-retry-queue
      │ (queue has x-message-ttl: 30000 — 30 second TTL)
      │ (queue has x-dead-letter-exchange pointing back to original exchange)
      │
      │ after 30 seconds, TTL expires
      │ message is dead-lettered back to original exchange
      ▼
budget-notifications-queue (message re-enters for retry)
      │
      ▼
Consumer tries again
```

This creates a delay loop:

```
Attempt 1 fails → wait 30s → Attempt 2
Attempt 2 fails → wait 60s → Attempt 3
Attempt 3 fails → wait 120s → Attempt 4
Attempt 4 fails → message goes to Dead Letter Queue (no more retries)
```

The retry count is tracked in the message headers:

```typescript
async onModuleInit() {
  await this.channel.consume(
    'budget-notifications-queue',
    async (message) => {
      const payload = JSON.parse(message.content.toString())
      const retryCount = message.properties.headers['x-retry-count'] ?? 0
      const maxRetries = 3

      try {
        await this.notificationService.sendBudgetAlert(payload)
        this.channel.ack(message)

      } catch (error) {
        if (retryCount >= maxRetries) {
          // Give up — send to Dead Letter Queue
          this.channel.nack(message, false, false)
          return
        }

        // Republish to retry exchange with incremented retry count
        // and increased delay
        const delayMs = Math.pow(2, retryCount) * 30000 // 30s, 60s, 120s

        this.channel.publish(
          'retry.exchange',
          'budget-notifications-queue', // routing key = original queue name
          message.content,
          {
            headers: {
              'x-retry-count': retryCount + 1,
              'x-original-routing-key': 'budget.threshold.exceeded'
            },
            expiration: delayMs.toString() // per-message TTL
          }
        )
        this.channel.ack(message) // ack original so it is not requeued
      }
    },
    { noAck: false }
  )
}
```

---

### "What is a Dead Letter Queue and why is it important?"

A **Dead Letter Queue (DLQ)** is a special queue where messages go when they cannot be processed successfully after all retries are exhausted.

Think of it as a quarantine zone — messages that failed to be processed are not lost, they are moved to a separate queue where they can be:
- Inspected manually to understand why they failed
- Replayed after the underlying bug is fixed
- Alerted on (Datadog monitors the DLQ size — any message arriving there triggers a Slack alert)

**How a message ends up in the DLQ:**

```
Three scenarios:

1. Consumer calls nack(message, false, false)
   → message rejected without requeue → goes to DLQ

2. Message retry count exceeds maximum
   → consumer explicitly nacks without requeue → goes to DLQ

3. Message TTL expires while sitting in queue
   → RabbitMQ moves expired message → goes to DLQ

4. Queue length limit reached
   → oldest messages are dead-lettered → goes to DLQ
```

**DLQ configuration in FinVerse:**

```typescript
// When declaring the main queue, configure DLX (Dead Letter Exchange)
await channel.assertQueue('budget-notifications-queue', {
  durable: true,
  arguments: {
    'x-dead-letter-exchange': 'dlx.exchange',       // where dead letters go
    'x-dead-letter-routing-key': 'budget.notifications.dead', // routing key in DLX
    'x-message-ttl': 3600000,  // messages expire after 1 hour if not consumed
    'x-max-length': 10000      // max 10,000 messages before dead-lettering oldest
  }
})

// DLX routes to DLQ
await channel.assertExchange('dlx.exchange', 'direct', { durable: true })
await channel.assertQueue('dead-letter-queue', { durable: true })
await channel.bindQueue('dead-letter-queue', 'dlx.exchange', 'budget.notifications.dead')
```

**What the team does with the DLQ:**
Datadog monitors `dead-letter-queue` depth. Any message arriving here triggers a `#incidents` Slack alert. An engineer inspects the message, identifies the root cause (bad payload? consumer bug? downstream service outage?), fixes the issue, and then **replays** the messages from the DLQ back to the original exchange.

---

### "What happens if the queue size limit is reached?"

Every queue in FinVerse has `x-max-length: 10000` configured. This means the queue holds a maximum of 10,000 messages at once.

**What happens when the 10,001st message arrives?**

RabbitMQ applies the **overflow policy**. FinVerse uses `reject-publish-dlx` (the default when DLX is configured):

- The oldest message at the front of the queue is dead-lettered to the DLX → moves to DLQ
- The new message is accepted into the queue

This means under extreme backlog conditions, the oldest messages get quarantined rather than the newest ones being rejected. This is the right behaviour — old pending notifications during an outage get quarantined, and once the consumer recovers, it processes the most recent backlog first.

**Why have a max length at all?**
Without it, an extended consumer outage could accumulate millions of messages, consuming unbounded memory and disk on the RabbitMQ broker. A limit provides a safety ceiling.

---

### "What happens if RabbitMQ itself goes down?"

This is where the **Outbox pattern** (covered in Step 5) becomes critical. Messages published directly to RabbitMQ that have not yet been persisted are at risk if the broker crashes at exactly the wrong moment.

**Publisher Confirms** address this:

```typescript
// Enable publisher confirms on the channel
await channel.confirmSelect()

// Publish with confirm
channel.publish(
  'finance.events',
  'budget.threshold.exceeded',
  message,
  { persistent: true }
)

// Wait for RabbitMQ to confirm the message was written to disk
await new Promise<void>((resolve, reject) => {
  channel.waitForConfirms((err) => {
    if (err) reject(err)  // RabbitMQ did not confirm — retry publish
    else resolve()        // RabbitMQ confirmed — safe to move on
  })
})
```

With publisher confirms enabled, the producer does not consider a message "sent" until RabbitMQ has acknowledged that it persisted the message to disk. If the confirm never arrives (RabbitMQ crashed), the producer knows to retry.

**For the highest-stakes events** (payment confirmed, subscription activated), FinVerse uses the **Outbox pattern** as an additional safety layer on top of publisher confirms. The event is written to the `outbox_events` table in the same PostgreSQL transaction as the business operation. Even if RabbitMQ is completely down, the event is safely in PostgreSQL and will be published when RabbitMQ recovers.

---

### "How does RabbitMQ work in a distributed / clustered setup?"

At FinVerse, RabbitMQ runs on **Amazon MQ for RabbitMQ** — AWS's fully managed RabbitMQ service. The deployment is a **3-node cluster** in active-active configuration across 3 AWS Availability Zones.

**How a RabbitMQ cluster works:**

```
┌─────────────────────────────────────────────────┐
│              RabbitMQ Cluster                   │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  Node 1  │  │  Node 2  │  │  Node 3  │      │
│  │  AZ-a    │  │  AZ-b    │  │  AZ-c    │      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│  All nodes share: Exchange definitions          │
│                   Queue definitions             │
│                   Bindings                      │
│                   Metadata                      │
│                                                 │
│  Queue data:  Mirrored across nodes             │
│               (Quorum Queues)                   │
└─────────────────────────────────────────────────┘
```

**Quorum Queues — the modern approach:**
FinVerse uses **Quorum Queues** (not the older Classic mirrored queues). A Quorum Queue is replicated across a majority of cluster nodes — in a 3-node cluster, data is replicated to all 3 nodes. A write is confirmed only when a majority (2 of 3) acknowledge it. This provides strong durability guarantees:

- If Node 1 fails, Node 2 and Node 3 still have all the data
- Producers and consumers reconnect to the surviving nodes automatically
- No message loss during single-node failure

**What happens during a node failure:**
```
Node 1 fails
         │
         ▼
Producers and consumers detect connection loss
         │
         ▼
They reconnect to Node 2 or Node 3
(connection string is the cluster endpoint, not a specific node)
         │
         ▼
Processing continues — Quorum Queue data is intact on remaining nodes
         │
         ▼
Node 1 recovers and rejoins the cluster
Quorum Queue syncs missing data from peers
```

From the application's perspective — a brief connection error, automatic reconnect, and normal operation resumes. No messages lost, no manual intervention.

---

## Part 5 — Consumer Advanced Configuration

### Prefetch Count — Controlling Consumer Load

By default, RabbitMQ pushes as many messages to a consumer as fast as it can. If a consumer is processing a message that takes 500ms, RabbitMQ might have already pushed 1,000 more messages into its buffer — which are now "in flight" and unavailable to other consumer instances.

**Prefetch count** controls how many unacknowledged messages a consumer can hold at once:

```typescript
// Consumer only holds 10 unacknowledged messages at a time
await channel.prefetch(10)
```

In FinVerse's Notification Service, prefetch is set to `10`. This means:
- Up to 10 messages are in-flight per consumer instance
- Once all 10 are unacknowledged (being processed), RabbitMQ stops pushing more to this consumer
- If there are multiple consumer instances, RabbitMQ distributes to others that have capacity

**Why this matters:**
Without prefetch, one slow consumer instance could hoard all messages. With prefetch, load distributes evenly across all instances. This is critical when Notification Service scales out to 3 instances during a spike — each instance gets roughly equal share of the workload.

---

### Consumer Concurrency

Each consumer instance in Notification Service processes one message at a time per channel. For I/O-bound work like sending emails (waiting for SendGrid HTTP response), this is inefficient — the consumer is idle while waiting for the network.

FinVerse addresses this by creating **multiple channels per consumer instance**, each with its own prefetch:

```typescript
// Create 5 concurrent channels per instance
// Each channel processes up to 10 messages concurrently
// Total: 50 in-flight messages per instance
for (let i = 0; i < 5; i++) {
  const channel = await connection.createChannel()
  await channel.prefetch(10)
  await channel.consume('budget-notifications-queue', messageHandler, { noAck: false })
}
```

This gives you controlled parallelism — 50 concurrent message processings per instance — without unbounded resource consumption.

---

### Idempotent Consumers — Handling Duplicate Messages

RabbitMQ guarantees **at-least-once delivery** — not exactly-once. In failure scenarios (consumer crashes after processing but before acking, network timeout during ack), the same message can be delivered more than once.

Your consumer must be **idempotent** — processing the same message twice produces the same result as processing it once.

In FinVerse's Notification Service:

```typescript
async sendBudgetAlert(payload: BudgetAlertPayload, messageId: string) {
  // Check if we have already processed this exact message
  const alreadyProcessed = await this.redis.get(`processed:${messageId}`)
  if (alreadyProcessed) {
    // Duplicate delivery — silently skip
    return
  }

  // Process the notification
  await this.pushNotificationService.send(payload)
  await this.emailService.send(payload)

  // Mark as processed in Redis with TTL of 24 hours
  // (long enough to catch duplicates, short enough to not accumulate forever)
  await this.redis.set(`processed:${messageId}`, '1', 'EX', 86400)
}
```

The `messageId` comes from the message's properties — set by the producer when publishing. Redis is used as the deduplication store with a 24-hour TTL.

---

## Part 6 — RabbitMQ in FinVerse: Setup & Best Practices

### How the Team Declares Topology

All exchanges, queues, and bindings are declared **idempotently at application startup** — meaning the code runs `assertExchange` and `assertQueue` every time the service starts. If they already exist with the same configuration, RabbitMQ does nothing. If they do not exist yet, RabbitMQ creates them.

This means no manual RabbitMQ setup is needed when deploying to a new environment. The application declares its own infrastructure.

```typescript
// rabbitmq.setup.ts — runs on NestJS module initialization
@Injectable()
export class RabbitMQSetup implements OnModuleInit {
  async onModuleInit() {
    // Declare exchange
    await this.channel.assertExchange('finance.events', 'topic', {
      durable: true
    })

    // Declare DLX
    await this.channel.assertExchange('dlx.exchange', 'direct', {
      durable: true
    })

    // Declare main queue with DLX configuration
    await this.channel.assertQueue('budget-notifications-queue', {
      durable: true,
      arguments: {
        'x-dead-letter-exchange': 'dlx.exchange',
        'x-dead-letter-routing-key': 'budget.notifications.dead',
        'x-message-ttl': 3600000,
        'x-max-length': 10000,
        'x-queue-type': 'quorum'  // Use Quorum Queue for durability
      }
    })

    // Declare DLQ
    await this.channel.assertQueue('dead-letter-queue', {
      durable: true
    })

    // Bindings
    await this.channel.bindQueue(
      'budget-notifications-queue',
      'finance.events',
      'budget.#'
    )

    await this.channel.bindQueue(
      'dead-letter-queue',
      'dlx.exchange',
      'budget.notifications.dead'
    )
  }
}
```

---

### Monitoring RabbitMQ in FinVerse

Datadog collects RabbitMQ metrics via the RabbitMQ Prometheus plugin (enabled on Amazon MQ). Key metrics monitored:

| Metric | Alert Threshold | What It Means |
|---|---|---|
| `queue.messages` (per queue) | > 1,000 | Consumer falling behind — possible outage |
| `queue.messages` on DLQ | > 0 | Any dead-lettered message — immediate investigation |
| `consumers` per queue | = 0 | No active consumer — service is down |
| `message.publish_rate` | Spike > 3x baseline | Unusual event volume — possible loop or attack |
| `connections` | Drop to 0 | All services lost connection to broker |

Any `dead-letter-queue` depth > 0 triggers an immediate `#incidents` Slack alert. This is non-negotiable — a message in the DLQ means something failed that needs investigation.

---

### Team Best Practices Summary

1. **Always use `durable: true` for queues and `persistent: true` for messages in production.** Non-durable queues are for local development only.

2. **Always use manual acknowledgement (`noAck: false`).** Auto-ack loses messages on consumer crash.

3. **Always configure a DLX on every production queue.** Messages must never silently disappear.

4. **Always set `x-max-length` on queues.** Unbounded queues are an operational risk.

5. **Always use Quorum Queues in the cluster.** Classic mirrored queues are deprecated.

6. **Consumers must be idempotent.** At-least-once delivery is a guarantee, not a bug.

7. **Use publisher confirms for critical events.** For the highest-stakes events, combine with the Outbox pattern.

8. **Set prefetch count explicitly.** Never let RabbitMQ push unbounded messages to a consumer.

9. **Declare topology at startup idempotently.** Infrastructure as code, not manual setup.

10. **Monitor DLQ depth in Datadog.** Any message there is an incident.

---

That is the complete Step 6 — RabbitMQ from first principles through to production architecture, failure scenarios, retry patterns, dead letter queues, clustering, and team best practices. Built from the ground up so you genuinely understand it.

**Ready for Step 7: BullMQ?**