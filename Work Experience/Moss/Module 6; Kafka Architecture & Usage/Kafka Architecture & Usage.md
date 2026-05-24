# Step 5: Kafka Architecture & Usage

---

## Start With The Honest Foundation

You watched a 3-hour YouTube crash course on Kafka. That means you know:
- What Kafka is (distributed message broker)
- Topics, producers, consumers
- Basic idea of consumer groups

That's it. Everything beyond that — we build from scratch here, explained clearly, so you genuinely understand it before we talk about how your team used it.

---

## Part 1 — Kafka Fundamentals (The Real Mental Model)

### What Kafka Actually Is

Most people describe Kafka as a "message queue." That's wrong, and interviewers who know Kafka will catch it.

```
A message queue (RabbitMQ style):
─────────────────────────────────
Message goes in → consumer reads it → 
message is DELETED.
One consumer gets each message.
Once read, it's gone.

Kafka:
──────
Message goes in → written to disk → 
STAYS THERE for a configured retention period.
Multiple independent consumers can 
read the same message.
Each consumer tracks its own position.
Message is NOT deleted after reading.

The right mental model:
Kafka is a distributed, append-only LOG.
Not a queue.
```

This distinction matters for our system — Notification Service AND Audit Service AND Payment Service all need to read the same `invoice.approved` event. With a queue, only one gets it. With Kafka, all three get it independently.

---

### Core Concepts Explained Simply

```
TOPIC
─────
A named stream of events.
Like a table in a database, but for events.
"invoice.approved" is a topic.
"expense.submitted" is a topic.
Events are appended to the end, never updated.


PARTITION
─────────
Each topic is split into N partitions.
Each partition is an ordered, append-only log.

Why partitions?
One log on one machine has limits.
Split across partitions = parallelism + scale.

Topic: expense.submitted
├── Partition 0: [event1, event4, event7...]
├── Partition 1: [event2, event5, event8...]
└── Partition 2: [event3, event6, event9...]

OFFSET
──────
Position of a message within a partition.
Each message has an offset: 0, 1, 2, 3...
Offset is per-partition (not global).

Consumer remembers: 
"I've read up to offset 47 in partition 0"
This is called committing the offset.

BROKER
──────
One Kafka server.
Stores partitions on disk.
Our cluster has 3 brokers (more on this below).

CLUSTER
───────
Group of brokers working together.
One broker is the Controller 
(manages partition leadership, elections).
```

---

### Producer — How a Message Gets In

```
Your code calls:
kafkaTemplate.send("expense.submitted", expenseId, eventPayload);
                        │                  │            │
                        topic              key          value

Step 1: Serialization
────────────────────
Value (Java object) → JSON bytes (or Avro bytes)
Key (expenseId UUID) → String/bytes

Step 2: Partition Assignment
─────────────────────────────
"Which partition does this message go to?"

If key is provided (our case — we use expenseId):
  hash(key) % numPartitions = partition number

Why does this matter?
All events for the same expenseId 
go to the SAME partition.
Same partition = guaranteed ordering.

expense-uuid-123 → always Partition 1
expense-uuid-456 → always Partition 2

So all events for expense 123 
(submitted → approved → reimbursed)
arrive in order at the consumer.
Critical for state machine correctness.

Step 3: Batching & Sending
───────────────────────────
Producer doesn't send one message at a time.
It batches messages in memory and sends 
in bulk for efficiency.
batch.size and linger.ms control this.

Step 4: Broker Acknowledgment
──────────────────────────────
Producer waits for broker confirmation.
This is controlled by acks setting.
(Covered in producer config below)
```

---

### Consumer — How a Message Gets Out

```
Consumer Group: a group of consumers 
that together consume a topic.

Rule: Each partition is assigned to 
      exactly ONE consumer in a group.

Example:
Topic: expense.submitted (3 partitions)
Consumer Group: notification-service

  Partition 0 → Consumer Instance A
  Partition 1 → Consumer Instance B  
  Partition 2 → Consumer Instance C

If we have 2 instances instead of 3:
  Partition 0 → Consumer Instance A
  Partition 1 → Consumer Instance A (double duty)
  Partition 2 → Consumer Instance B

If we have 4 instances:
  Partition 0 → Consumer Instance A
  Partition 1 → Consumer Instance B
  Partition 2 → Consumer Instance C
  Consumer Instance D → IDLE (no partition assigned)

Key insight:
Max parallelism = number of partitions.
Adding more consumers than partitions 
gives you zero benefit.
```

---

## Part 2 — Our Kafka Cluster Setup

### Cluster Architecture

```
At Series B/C scale (5,000 SMEs):
We don't need a massive cluster.
But we need reliability — this is financial data.

Our setup: 3 Kafka brokers on AWS
─────────────────────────────────

  ┌─────────┐   ┌──────────┐   ┌──────────┐
  │ Broker 1│   │ Broker 2 │   │ Broker 3 │
  │ (Leader │   │ (Follower│   │ (Follower│
  │  for    │   │  for     │   │  for     │
  │ some    │   │  some    │   │  some    │
  │ parts)  │   │  parts)  │   │  parts)  │
  └─────────┘   └──────────┘   └──────────┘
       │               │             │
       └───────────────┴─────────────┘
                       │
                 ZooKeeper / KRaft
              (cluster coordination)

Why 3 brokers?
──────────────
Minimum for fault tolerance with 
replication factor of 3.
If one broker dies:
- Other two still have all data
- Cluster elects new leaders for 
  affected partitions
- Zero data loss, minimal downtime

Not using Confluent Cloud or MSK?
──────────────────────────────────
At Series B, self-managed on AWS EC2 
gives more control and costs less.
MSK (AWS managed Kafka) is an option
but adds cost. DevOps team manages 
the cluster directly.
```

### Topic Configuration for Our Services

```
Topics owned by Expense & Reimbursement Service 
(producer side — we publish these):
────────────────────────────────────────────────
expense.submitted
expense.approved  
expense.rejected
reimbursement.completed

Topics owned by Invoice & AP Service 
(producer side — we publish these):
────────────────────────────────────
invoice.submitted
invoice.verified
invoice.approved
invoice.rejected
invoice.paid

Topics we CONSUME (produced by other services):
───────────────────────────────────────────────
payment.completed  → produced by Payment Service
                     we consume to mark invoice PAID
user.deactivated   → produced by User & Org Service
                     we handle if approver is deactivated
```

### Partition & Replication Config Per Topic

```sql
-- Example for invoice.approved topic:

Topic Name:         invoice.approved
Partitions:         6
Replication Factor: 3
Retention:          7 days
Min ISR:            2

Why 6 partitions?
──────────────────
We have up to 6 consumer instances 
across our services consuming this topic
(Payment Service, Notification Service, 
 Accounting Integration Service, Audit Service).
6 partitions = 6 can run in parallel.

Why retention 7 days?
──────────────────────
If Accounting Integration Service is down 
for a weekend (maintenance), it needs to 
catch up when it restarts.
7 days gives enough replay window.
Not forever — disk costs money.

Min ISR (In-Sync Replicas) = 2:
────────────────────────────────
ISR = replicas that are caught up with leader.
acks=all means: wait for all ISR to confirm.
Min ISR=2 means: at least 2 replicas must 
confirm before producer gets acknowledgment.
Prevents data loss if broker crashes 
right after write.
```

---

## Part 3 — Producer Setup in Our Service

### Spring Boot Kafka Producer Configuration

```java
// application.properties (expense-service)

# Brokers
spring.kafka.bootstrap-servers=kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092

# Serialization
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Reliability settings
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.retry.backoff.ms=1000

# Idempotency (explained below)
spring.kafka.producer.enable.idempotence=true

# Batching
spring.kafka.producer.batch-size=16384
spring.kafka.producer.linger.ms=5
```

### Producer Idempotency — Why It Matters

```
The problem without idempotency:
─────────────────────────────────
Producer sends message.
Broker writes it, sends ACK.
ACK is lost in network.
Producer thinks: "no ACK = failure, retry!"
Producer sends SAME message again.
Broker writes it AGAIN.

Result: invoice.approved published TWICE.
Payment Service triggers payment TWICE.
Customer gets charged twice. 
That's a very bad day.

enable.idempotence=true solution:
──────────────────────────────────
Each producer gets a unique Producer ID (PID).
Each message gets a sequence number.
Broker tracks: PID + sequence number.
If same sequence arrives again: IGNORED.
Exactly-once at the producer→broker level.

This is NOT optional for financial systems.
```

### KafkaTemplate — How We Actually Send Events

```java
@Service
@RequiredArgsConstructor
public class ExpenseEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishExpenseSubmitted(Expense expense) {
        
        ExpenseSubmittedEvent event = ExpenseSubmittedEvent.builder()
            .expenseId(expense.getId().toString())
            .employeeId(expense.getEmployeeId().toString())
            .companyId(expense.getCompanyId().toString())
            .amount(expense.getAmount())
            .currency(expense.getCurrency())
            .approverId(expense.getAssignedApproverId().toString())
            .submittedAt(expense.getSubmittedAt())
            .build();

        kafkaTemplate.send(
            "expense.submitted",      // topic
            expense.getId().toString(), // key — ensures ordering per expense
            event                      // value
        );
    }
}
```

**But wait — this is the naive approach.**

If we call `kafkaTemplate.send()` directly after the DB update, we have the dual-write problem.

```
DB update: expense.status = SUBMITTED  ✓
Kafka send: expense.submitted event    ✗ (Kafka down)

Result: DB says submitted.
        No one knows. 
        No approval triggered.
        Expense sits stuck forever.
```

This is exactly why we need the Outbox Pattern.

---

## Part 4 — The Outbox Pattern (Why We Actually Need It)

### The Problem Restated Clearly

```
We need two things to happen together:
1. Update expense status in PostgreSQL
2. Publish event to Kafka

These are TWO different systems.
You cannot wrap them in ONE transaction.
@Transactional only covers your DB operations.
It knows nothing about Kafka.

So either:
- DB succeeds, Kafka fails → silent inconsistency
- Kafka succeeds, DB fails → ghost event 
  (payment triggered for expense that 
   was never actually approved in DB)

Both are catastrophic for a financial system.
```

### The Outbox Solution

```
Core idea:
──────────
Instead of writing to Kafka directly,
write to an OUTBOX TABLE in the SAME database.
Then a separate process reads from outbox 
and publishes to Kafka.

Now the "two systems" problem becomes 
"one system" problem:
DB update + outbox insert = ONE transaction.
Either both commit or both rollback.
Atomically guaranteed by PostgreSQL.
Kafka is now a separate, retryable concern.

┌──────────────────────────────────────┐
│         @Transactional               │
│                                      │
│  UPDATE expenses                     │
│  SET status = 'SUBMITTED'            │
│  WHERE id = ?                        │
│                                      │
│  INSERT INTO outbox_events           │
│  (aggregate_id, event_type, payload) │
│  VALUES (expenseId, 'expense.        │
│  submitted', {...})                  │
│                                      │
│  COMMIT ←── atomic                   │
└──────────────────────────────────────┘
            │
            │ (separate process)
            ▼
┌──────────────────────────────────────┐
│         Outbox Poller                │
│  (runs every 100ms)                  │
│                                      │
│  SELECT * FROM outbox_events         │
│  WHERE published = FALSE             │
│  ORDER BY created_at                 │
│  LIMIT 100                           │
│                                      │
│  For each event:                     │
│    kafkaTemplate.send(...)           │
│    UPDATE outbox_events              │
│    SET published = TRUE              │
└──────────────────────────────────────┘
```

### Outbox Poller Implementation

```java
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 100)   // runs every 100ms
    @Transactional
    public void pollAndPublish() {
        
        List<OutboxEvent> unpublished = outboxRepository
            .findTop100ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : unpublished) {
            try {
                // Deserialize payload back to object
                Object payload = objectMapper.readValue(
                    outboxEvent.getPayload().toString(), 
                    Object.class
                );

                // Send to Kafka
                kafkaTemplate.send(
                    outboxEvent.getEventType(),  // topic name
                    outboxEvent.getAggregateId().toString(), // key
                    payload
                ).get();  
                // .get() makes it synchronous — 
                // we wait for broker confirmation
                // before marking as published

                // Mark as published
                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(Instant.now());
                outboxRepository.save(outboxEvent);

            } catch (Exception e) {
                // Log and continue to next event
                // This event will be retried 
                // on next poll cycle
                log.error("Failed to publish outbox event: {}", 
                    outboxEvent.getId(), e);
            }
        }
    }
}
```

**Key design decisions here:**

```
Why .get() (synchronous send)?
───────────────────────────────
We need to know if Kafka confirmed 
before marking published=true.
If we mark published=true and then 
Kafka fails, we've lost the event forever.
Synchronous here is intentional.

Why LIMIT 100?
───────────────
If service was down for 2 hours,
outbox might have 10,000 unprocessed events.
Processing all at once could overwhelm 
Kafka broker and delay normal operations.
Batching in chunks of 100 is safer.

What if poller crashes mid-batch?
──────────────────────────────────
Events that were published but not marked
as published will be retried.
Kafka gets duplicate event.

This means consumers MUST be idempotent.
(Covered in consumer section below)
```

---

## Part 5 — Consumer Setup in Our Service

### Which Topics Does Our Team Consume?

```
Expense & Reimbursement Service consumes:
──────────────────────────────────────────
payment.completed → mark reimbursement as COMPLETED
user.deactivated  → reassign pending approvals

Invoice & AP Service consumes:
───────────────────────────────
payment.completed → mark invoice as PAID
user.deactivated  → reassign pending approvals
```

### Spring Kafka Consumer Configuration

```java
// application.properties

# Consumer group IDs
spring.kafka.consumer.group-id=expense-service
# (invoice service uses: invoice-service)

# Deserialization
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.moss.events.*

# Offset behavior
spring.kafka.consumer.auto-offset-reset=earliest
# earliest = if no committed offset found,
# start from beginning of topic.
# Good for new consumer groups.
# 'latest' = only new messages. 
# Risk: miss events that arrived 
# before consumer started.

# Manual offset commit (explained below)
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual
```

### Why Manual Offset Commit?

```
Auto-commit behavior:
──────────────────────
Every 5 seconds (default), consumer 
commits its current offset automatically.

Problem:
Consumer reads message at offset 47.
Auto-commit fires: offset 47 committed.
Consumer starts processing.
Processing FAILS (DB error, NPE, anything).
Consumer restarts.
Starts from offset 48. 
Message 47 is LOST FOREVER.

Manual commit behavior:
────────────────────────
Consumer reads message.
Consumer processes it successfully.
Consumer manually commits offset.
Only then is the offset advanced.

If processing fails:
Consumer restarts.
Starts from offset 47 again.
Message is reprocessed.
This is "at-least-once delivery."

For financial systems:
at-least-once + idempotent consumers 
= effectively exactly-once behavior.
```

### Consumer Implementation

```java
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceAuditLogRepository auditLogRepository;

    @KafkaListener(
        topics = "payment.completed",
        groupId = "invoice-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            PaymentCompletedEvent event,
            Acknowledgment acknowledgment) {   
            // manual ack

        log.info("Received payment.completed for invoiceId: {}", 
            event.getInvoiceId());

        try {
            processPaymentCompleted(event);
            
            // Only commit offset AFTER 
            // successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process payment.completed " +
                "for invoiceId: {}", event.getInvoiceId(), e);
            
            // Do NOT acknowledge.
            // Message will be redelivered.
            // But we need to be careful here —
            // see Dead Letter Queue section below.
        }
    }

    @Transactional
    private void processPaymentCompleted(PaymentCompletedEvent event) {
        
        UUID invoiceId = UUID.fromString(event.getInvoiceId());
        
        // IDEMPOTENCY CHECK — critical
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // If already PAID, this is a duplicate event.
        // Do nothing. Still acknowledge.
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.warn("Invoice {} already PAID. " + 
                "Duplicate event ignored.", invoiceId);
            return;
        }

        // Update status
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(event.getCompletedAt());
        invoice.setPaymentReference(event.getPaymentReference());
        invoiceRepository.save(invoice);

        // Write audit log
        auditLogRepository.save(InvoiceAuditLog.builder()
            .invoiceId(invoiceId)
            .action("PAID")
            .oldStatus(InvoiceStatus.PAYMENT_PENDING)
            .newStatus(InvoiceStatus.PAID)
            .createdAt(Instant.now())
            .build());
    }
}
```

**The idempotency check is the most important part:**

```
Why will we get duplicate events?
───────────────────────────────────
1. Outbox poller retries (as explained above)
2. Kafka's at-least-once delivery guarantee
3. Consumer crashes after processing 
   but before committing offset

All three cause the same event to arrive twice.

The idempotency check:
───────────────────────
Before doing anything, check:
"Has this already been processed?"

For payment.completed:
If invoice.status is already PAID → skip.
Acknowledge the duplicate. Move on.

This makes our consumer safe to call 
multiple times with the same event.
That's idempotency.
```

---

## Part 6 — Exception Handling & Dead Letter Queue

### The Retry Problem

```
Consumer fails to process a message.
Does not acknowledge.
Kafka redelivers.
Fails again.
Kafka redelivers.
Fails again.
Infinitely.

Meanwhile: this partition is BLOCKED.
No other messages in this partition 
can be processed.
This is called a "poison pill" message.

Example poison pill:
payment.completed arrives with 
invoiceId that doesn't exist in our DB.
(Data inconsistency from another service.)
We'll fail every single time.
Retrying forever achieves nothing.
```

### Dead Letter Queue (DLQ) Solution

```
After N retries, give up on this message.
Send it to a separate "dead letter" topic.
Continue processing the next message.
A human (or separate process) 
investigates the DLQ later.

Flow:
──────
Message arrives
    │
    ▼
Process → SUCCESS → acknowledge ✓
    │
    ▼ FAILURE
Retry (attempt 2)
    │
    ▼ FAILURE  
Retry (attempt 3)
    │
    ▼ FAILURE
Send to DLQ topic → acknowledge original
Continue with next message
```

### DLQ Implementation

```java
// Configuration
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> 
            factory = new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);

        // Error handler with DLQ
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                    (record, exception) -> {
                        // DLQ topic naming convention:
                        // original topic + ".DLT"
                        return new TopicPartition(
                            record.topic() + ".DLT",
                            record.partition()
                        );
                    }),
                // Retry policy: 3 attempts, 
                // 1 second between each
                new FixedBackOff(1000L, 3L)
            )
        );

        return factory;
    }
}
```

### DLQ Topic Naming Convention

```
Original topic          DLQ topic
──────────────────────────────────
payment.completed    →  payment.completed.DLT
user.deactivated     →  user.deactivated.DLT
invoice.approved     →  invoice.approved.DLT

DLT = Dead Letter Topic
(some teams use .DLQ — same concept)
```

### What Happens to DLQ Messages?

```
In our team:
─────────────
1. Datadog alert fires when DLT 
   has messages (monitored via 
   consumer lag metric)

2. On-call engineer investigates:
   - Was it a transient error? 
     (DB was briefly down)
     → Replay the message manually
   
   - Was it a data issue?
     (invoice ID doesn't exist)
     → Fix the data, replay
   
   - Was it a bug in our code?
     → Fix the code, redeploy,
       replay from DLT

3. Replay means: read from DLT topic,
   re-publish to original topic.
   Small internal tool for this.

Why not just retry forever?
────────────────────────────
Because partition blocking.
The message after the poison pill 
could be a legitimate payment.completed
for a different invoice.
We cannot let one bad message 
block legitimate processing.
```

---

## Part 7 — Consumer Advanced Configuration

### Concurrency

```java
@KafkaListener(
    topics = "invoice.approved",
    groupId = "invoice-service",
    concurrency = "3"   
    // 3 threads consuming in parallel
    // Makes sense only if topic has 
    // >= 3 partitions
)
```

```
Our invoice.approved topic has 6 partitions.
We run 2 instances of Invoice Service 
(for redundancy).
Each instance: concurrency = 3.
Total consumers: 2 × 3 = 6.
Matches partition count perfectly.
Maximum parallelism achieved.
```

### Consumer Lag Monitoring

```
Consumer lag = how far behind the 
consumer is from the latest message.

Example:
Latest offset in partition: 1000
Consumer's committed offset:  950
Lag: 50 messages behind

Why it matters:
Normal lag: near 0 (keeping up)
Spike in lag: consumer is struggling,
              possibly errors or slow processing

We monitor this in Datadog.
Alert fires if lag > 1000 for > 5 minutes.
That means something is wrong.
```

---

## Part 8 — Schema Registry & Avro

### Why Not Just Use JSON?

```
JSON problems at scale:
────────────────────────
Producer sends:
{ "invoiceId": "uuid-123", "amount": 3200.00 }

Three months later, producer changes field name:
{ "invoice_id": "uuid-123", "amount": 3200.00 }

Consumer still expects "invoiceId".
Consumer silently gets null.
Invoice never gets marked as PAID.
No error thrown. Silent failure.

With JSON, there's no contract enforcement.
Any producer can publish anything.
Any consumer has to hope the format 
hasn't changed.
This breaks in production eventually.
```

### Avro + Schema Registry Solution

```
Avro = a data serialization format.
Compact binary (smaller than JSON).
Schema is defined separately from the data.
Schema is enforced at serialization time.

Schema Registry = a central store for schemas.
Every event type has a registered schema.
Producer must match registered schema.
Consumer reads schema to deserialize.

Flow:
──────
                    ┌─────────────────┐
                    │ Schema Registry │
                    │                 │
                    │ invoice.approved│
                    │ schema v1:      │
                    │ {invoiceId,     │
                    │  amount,        │
                    │  currency,      │
                    │  approvedAt}    │
                    └────────┬────────┘
                             │
         ┌───────────────────┼──────────────────┐
         │                   │                  │
    Producer             Kafka               Consumer
    validates            stores              validates
    against              binary +            against
    schema               schema ID           schema
    before               in message          before
    sending              header              deserializing
```

### Avro Schema for `invoice.approved`

```json
// src/main/avro/InvoiceApprovedEvent.avsc

{
  "type": "record",
  "name": "InvoiceApprovedEvent",
  "namespace": "com.moss.events.invoice",
  "fields": [
    {"name": "invoiceId",   "type": "string"},
    {"name": "companyId",   "type": "string"},
    {"name": "supplierId",  "type": "string"},
    {"name": "amount",      "type": "double"},
    {"name": "currency",    "type": "string"},
    {"name": "approvedById","type": "string"},
    {"name": "approvedAt",  "type": "long",
     "logicalType": "timestamp-millis"}
  ]
}
```

### Schema Evolution Rules

```
The whole point of Schema Registry:
safe schema changes without breaking consumers.

BACKWARD COMPATIBLE (safe):
────────────────────────────
Add a NEW field with a DEFAULT value.
Old consumers ignore unknown fields.
New producers can start sending it.

Example:
Add "glCode" with default "":
Old consumer: ignores glCode, works fine.
New consumer: reads glCode.

NOT BACKWARD COMPATIBLE (dangerous):
──────────────────────────────────────
Remove an existing field.
Rename an existing field.
Change a field's type.

Schema Registry enforces compatibility rules.
If you try to register a breaking schema change,
Registry REJECTS it.
Prevents accidental contract breaking.
```

### Do We Actually Use Avro at Series B?

```
Honest answer: It depends on the team's maturity.

At Series B, many teams start with JSON
because it's simpler to set up and debug.
You can literally read JSON in Kafka UI.
Avro is binary — you need Schema Registry
to read it.

Our team's approach:
────────────────────
Started with JSON (simpler).
As team grew and more services consumed 
our events, schema drift became a real risk.
Around month 10-12, we migrated 
critical topics to Avro with Schema Registry.

Non-critical topics (like audit events 
that only Audit Service reads):
Still JSON. Not worth the overhead.

This is a realistic Series B decision —
pragmatic, not dogmatic.
```

---

## Part 9 — Full Kafka Architecture Diagram

```
EXPENSE & REIMBURSEMENT SERVICE
────────────────────────────────
  Controller
      │
  Service Layer
      │ @Transactional
      ├── UPDATE expenses SET status = 'SUBMITTED'
      └── INSERT INTO outbox_events
                │
                │ (same DB transaction)
                ▼
          outbox_events table
                │
                │ (Outbox Poller, every 100ms)
                ▼
┌───────────────────────────────────────────────────────┐
│                    KAFKA CLUSTER                      │
│  ┌─────────────────┐  ┌──────────────────────────┐    │
│  │expense.submitted│  │   expense.approved       │    │
│  │  P0 P1 P2 P3    │  │     P0 P1 P2 P3          │    │
│  └────────┬────────┘  └───────────┬──────────────┘    │
│           │                       │                   │
│  ┌────────────────────────────────────────────────┐   │
│  │  invoice.approved  │  payment.completed        │   │
│  │  P0 P1 P2 P3 P4 P5 │  P0 P1 P2 P3              │   │
│  └────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
  Notification    Payment Service  Accounting
  Service         (consumes        Integration
  (consumes       invoice.approved (consumes
  expense.        to create        invoice.approved
  submitted,      payment run)     for DATEV export)
  invoice.
  approved)
         │              │              │
         ▼              ▼              ▼
  Audit Service   Invoice Service  Invoice Service
  (consumes ALL   (consumes        (consumes
  events for      payment.         payment.completed
  immutable log)  completed        to mark PAID)
                  to mark PAID)

DLQ Topics (monitored separately):
────────────────────────────────────
payment.completed.DLT
invoice.approved.DLT
expense.submitted.DLT
```

---

## The Interview Questions You'll Get

**Q: "Why Kafka and not RabbitMQ?"**

```
RabbitMQ is a message queue — message deleted 
after consumption. If we add a new consumer 
(say, a new reporting service) later, 
it can't replay historical events.

Kafka retains events for configurable period.
Multiple independent consumers read same events.
We can replay from any point.
For an audit trail and accounting exports 
that run on schedules, this matters.
```

**Q: "How do you ensure exactly-once processing?"**

```
Strictly: we use at-least-once delivery 
with idempotent consumers.
True exactly-once in distributed systems 
is extremely complex and has performance costs.

Our approach:
- Producer idempotency (enable.idempotence=true)
  prevents duplicate publishing.
- Consumer idempotency check 
  (status already PAID? skip.)
  prevents duplicate processing effects.

The business outcome is the same as 
exactly-once, at a fraction of the complexity.
```

**Q: "What happens if your Outbox Poller goes down?"**

```
Events accumulate in outbox_events table.
No data is lost — PostgreSQL is durable.
When poller recovers, it processes 
the backlog in order.
Downstream services receive events 
with a delay, but nothing is lost.

This is acceptable for our use cases.
A 5-minute delay in an approval notification
is annoying. A lost payment event is catastrophic.
We optimize for correctness over latency.
```

**Q: "How do you handle a message that keeps failing?"**

```
After 3 retry attempts (with 1 second backoff),
the message goes to the Dead Letter Topic.
The partition is unblocked.
Datadog alert fires.
On-call engineer investigates.
Root cause fixed, message replayed from DLT.

We never let one bad message block 
legitimate processing indefinitely.
```

---

Step 5 complete.

**Next is Step 6: API Design** — designing the actual REST APIs for our two services, covering endpoint structure, request/response DTOs, versioning, error response format, and pagination. Ready?