# Story 8: Kafka Consumer Implementation — First Real Exposure to Event-Driven Architecture

---

## Context — Where You Were at Month 7

```
Month 6 end — where you stood:
────────────────────────────────
You had just shipped the bulk approval 
feature independently.
Lukas had said "this is L2 behavior" 
in Slack.
Elena's PR comments were down to 3-4.
You were reviewing Kemal's PRs regularly.

You felt like a contributor.
Not senior. Not even mid-level.
But genuinely contributing — 
not just executing.

Then month 7 started.
And Arjun appeared in your world 
properly for the first time.
```

Until month 7, Arjun was someone you knew existed. You'd seen him in standups. You'd read his code in the codebase. You knew he was the distributed systems person on the team. But your work had never overlapped with his directly.

That changed with a ticket in month 7's sprint planning.

```
What you knew about Kafka 
coming into month 7:
──────────────────────────
- What it is: a distributed message broker
- Basic concepts: topics, producers, 
  consumers, consumer groups, offsets
- Watched a 3-hour YouTube crash course
- Never written a single line of 
  Kafka code in production
- Never debugged a Kafka problem
- Never read a Kafka consumer in 
  a real production codebase 
  (only tutorial code)

What you didn't know:
──────────────────────
- How Spring Kafka actually wires things
- Manual offset commits and why they exist
- What consumer lag means in practice
- Dead letter queues
- Idempotency in consumers
- What a Kafka trace looks like in Datadog
- What "consumer group rebalancing" 
  actually does to your service
- Anything about Avro or Schema Registry
- Outbox pattern integration
```

This gap mattered. And Lukas knew it when he assigned the ticket.

---

## The Situation

Sprint planning, month 7. Lukas presented a new ticket:

```
Ticket: EXP-267
Title:  Consume payment.completed events 
        to mark reimbursements as completed

Background:
────────────
Currently, when Payment Service 
processes a reimbursement payment 
via SEPA transfer, it publishes 
a payment.completed event to Kafka.

The Expense & Reimbursement Service 
needs to consume this event and:
1. Update the reimbursement record 
   status from PROCESSING to COMPLETED
2. Record the payment reference 
   and completion timestamp
3. Write an audit log entry
4. Publish a reimbursement.completed 
   event (via outbox) for 
   Notification Service to consume

Currently none of this happens.
Reimbursements stay in PROCESSING 
status forever after payment.

Story points: 5
Assigned to: You

Note: Arjun will pair-program with 
you on this. This is your first 
Kafka consumer implementation.
```

The last line was deliberate. Lukas had arranged this in advance — you found out later that he had asked Arjun specifically to work through this with you, not just answer questions, but actually sit alongside you and walk you through how the team did Kafka work.

```
Why this ticket for your 
first Kafka work:
──────────────────
Consumer side is simpler than 
producer side as a starting point.

You're reading events someone else 
is already publishing.
You don't have to design the 
event schema.
You don't have to worry about 
producer idempotency.
You just have to: read the event,
process it correctly, 
handle failures correctly.

That's still complex.
But it's the right entry point.
```

---

## Your Task

```
What you owned:
────────────────
1. Implement the Kafka consumer 
   for payment.completed topic

2. Update reimbursement status 
   logic when event arrives

3. Handle the idempotency case 
   (what if same event arrives twice)

4. Handle failure cases 
   (what if DB is down when event arrives)

5. Write the outbox event 
   for reimbursement.completed

6. Unit tests + integration tests

What Arjun owned:
──────────────────
Nothing directly — but he sat with 
you through the design and first 
implementation, explaining each 
decision as it happened.

What the existing codebase 
already had:
─────────────
Other Kafka consumers existed 
(Arjun had written them).
You were not starting from scratch.
You were following an established 
pattern — and understanding 
WHY it was that pattern.
```

---

## The Action — Day by Day

### Day 1 — Arjun's Introduction to Kafka in the Real System

You and Arjun jumped on a Google Meet on Monday morning. He shared his screen. He started not with your ticket, but with the existing codebase.

```
"Before we write anything new," 
Arjun said, 
"I want to show you what we already 
have and WHY it's built this way.
Because the pattern in your ticket 
is the same pattern everywhere in 
our Kafka consumers.
If you understand the why, 
you won't need to ask me 
every step."
```

He opened `UserDeactivatedConsumer.java` — an existing consumer in the Expense Service that handled user deactivation events:

```java
// Existing consumer — Arjun walked you 
// through this line by line
@Component
@RequiredArgsConstructor
public class UserDeactivatedConsumer {

    private final ExpenseRepository expenseRepository;
    private final ApprovalStepRepository 
        approvalStepRepository;

    @KafkaListener(
        topics = "user.deactivated",
        groupId = "expense-service"
    )
    public void handleUserDeactivated(
            UserDeactivatedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Received user.deactivated 
            for userId: {}", event.getUserId());

        try {
            processUserDeactivation(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process 
                user.deactivated for userId: {}",
                event.getUserId(), e);
            // Do NOT acknowledge — 
            // message will be redelivered
        }
    }

    @Transactional
    private void processUserDeactivation(
            UserDeactivatedEvent event) {
        // ... reassign pending approvals
    }
}
```

Arjun walked through every line:

```
Arjun's explanation — line by line:
─────────────────────────────────────

@KafkaListener:
"This tells Spring: 
 'this method should be called 
 whenever a message arrives 
 on this topic, for this group.'
 
 The groupId is critical.
 All instances of expense-service 
 share the same groupId.
 That means Kafka treats them 
 as one consumer group.
 Kafka assigns partitions across 
 all instances in the group —
 so each message is processed 
 by exactly ONE instance.
 
 If we used different groupIds per 
 instance, each instance would 
 get every message.
 That's almost never what you want."

Acknowledgment parameter:
"By default, Spring Kafka 
 auto-commits offsets every 5 seconds.
 That means: 5 seconds after reading 
 a message, Kafka marks it as processed —
 regardless of whether your code 
 actually succeeded.
 
 If your DB call fails AFTER the 
 auto-commit, Kafka thinks the 
 message was processed.
 You never process it again.
 The reimbursement stays broken.
 That's data loss in a financial system.
 
 Manual acknowledgment means:
 YOU decide when to call acknowledge().
 You only call it AFTER successful processing.
 If processing fails, you don't acknowledge.
 Kafka redelivers the message.
 
 This gives you at-least-once delivery —
 you might process the same message 
 twice, but you'll never miss one.
 
 That's why we need idempotency 
 in the processing logic — 
 because the same event might arrive twice."

The try-catch structure:
"Notice: acknowledge() is INSIDE the try.
 If processUserDeactivation() throws,
 we don't call acknowledge().
 The message stays uncommitted.
 Kafka redelivers it.
 
 If we put acknowledge() in a finally block,
 we'd acknowledge even on failure.
 That would be the same as auto-commit.
 Wrong."
```

This 30-minute walkthrough taught you more about Kafka in practice than the 3-hour YouTube course had.

```
The thing the YouTube course 
didn't teach you:
──────────────────────────────
The course showed you HOW Kafka works.
Arjun showed you WHY every configuration 
choice exists — specifically in the 
context of financial data where 
losing a message means a customer 
doesn't get paid.

Context turns information into 
understanding.
```

---

### Day 1 Continued — The Application Properties

After the code walkthrough, Arjun showed you `application.properties` for the Kafka consumer config:

```properties
# application-dev.properties — 
# Arjun walked you through each key

# Consumer group — same for all instances
spring.kafka.consumer.group-id=expense-service

# Deserialization
spring.kafka.consumer.key-deserializer=\
  org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=\
  org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties\
  .spring.json.trusted.packages=com.moss.events.*

# CRITICAL — disable auto-commit
# We manage offsets manually
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.listener.ack-mode=manual

# What to do when no committed offset exists
# earliest = start from beginning of topic
# This is for new consumer groups that 
# haven't read this topic before
spring.kafka.consumer.auto-offset-reset=earliest
```

Arjun explained `auto-offset-reset`:

```
Arjun:
───────
"This matters specifically when you 
deploy a new consumer for a topic 
that already has events on it.

For example:
payment.completed has been publishing 
events for months.
Your new consumer just started.
There's no committed offset for 
expense-service group on this topic.

earliest: start from the very first 
          event ever published.
          You'll process all historical 
          payment.completed events.
          This is what we want here —
          there are reimbursements 
          in PROCESSING state that 
          need to be marked COMPLETED
          from historical payments.

latest:   only process new events 
          from this point forward.
          Historical events are ignored.
          This would leave old 
          reimbursements stuck.

For new consumers joining an 
existing event stream where you 
need to catch up: earliest.
For consumers where historical 
events are irrelevant: latest."
```

This was a detail you never would have known to ask about. It was in the YouTube course as a configuration option, but without the context of "what happens when you deploy a new consumer to an existing topic."

---

### Day 2 — Reading the payment.completed Event Schema

Before writing any code, Arjun showed you something else: where to find the event schema for `payment.completed`.

```
Arjun:
───────
"Never assume you know the shape 
 of an event from another service.
 Always find the actual event class 
 or schema.
 
 Payment Service publishes this topic.
 The event class is in a shared 
 common module — common-events.
 Open that."
```

You opened the shared module and found:

```java
// common-events/src/main/java/
// com/moss/events/payment/PaymentCompletedEvent.java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    // Which payment this refers to
    private String paymentId;

    // The reimbursement this payment was for
    // This is how we link back to our data
    private String reimbursementId;

    // The employee who received the payment
    private String employeeId;

    private BigDecimal amount;
    private String currency;

    // SEPA payment reference
    // Important for audit trail
    private String paymentReference;

    private Instant completedAt;

    // What kind of payment this was
    // Could be REIMBURSEMENT or INVOICE_PAYMENT
    private PaymentType paymentType;
}

public enum PaymentType {
    REIMBURSEMENT,
    INVOICE_PAYMENT
}
```

```
Important observation Arjun pointed out:
─────────────────────────────────────────
"Notice paymentType.
 Payment Service publishes payment.completed 
 for BOTH reimbursements AND invoice payments.
 
 Your consumer should only process 
 REIMBURSEMENT type events.
 
 If you don't filter, you'd try to 
 find a reimbursement record for 
 an invoice payment event — 
 reimbursementId would be an invoice ID,
 your DB query returns null,
 and you'd log confusing errors.
 
 Always filter on type 
 when consuming shared topics."
```

This was your first encounter with a real shared topic — one topic consumed by multiple services for different purposes. The YouTube course had shown simple examples where one producer and one consumer had a one-to-one relationship. Reality was messier.

---

### Day 2 Continued — Drawing the Flow Before Writing Code

You had learned from Story 5 (multi-level approval) to draw the state machine before writing code. You applied this here:

```
PAYMENT COMPLETED FLOW
───────────────────────

payment.completed event arrives
          │
          ▼
Filter: is paymentType == REIMBURSEMENT?
          │
    ┌─────┴──────┐
   NO            YES
    │             │
    ▼             ▼
  Skip     Find reimbursement 
  (ack)    by reimbursementId
                  │
         ┌────────┴──────────┐
         │                   │
    NOT FOUND           FOUND
         │                   │
         ▼                   ▼
    Log warning       Check status
    Ack anyway        (idempotency)
                           │
                  ┌────────┴────────┐
                  │                 │
           Already COMPLETED    PROCESSING
                  │                 │
                  ▼                 ▼
           Log duplicate       Update to COMPLETED
           Ack (skip)          Set paymentReference
                               Set completedAt
                                    │
                                    ▼
                               Write audit log
                               (REQUIRES_NEW)
                                    │
                                    ▼
                               Insert outbox event
                               (reimbursement.completed)
                                    │
                                    ▼
                               Acknowledge offset
```

You showed this to Arjun.

```
Arjun's feedback on the diagram:
──────────────────────────────────
"Good structure. One thing to add:
 what happens if the reimbursement 
 is in a state OTHER than 
 PROCESSING or COMPLETED?
 
 For example: FAILED or CANCELLED.
 
 If Payment Service sends 
 payment.completed for a reimbursement 
 that we had marked as FAILED 
 (maybe an earlier attempt failed),
 what do we do?
 
 Think about it — and add it 
 to your diagram."
```

You thought about it for a few minutes:

```
Your answer:
─────────────
"If it's FAILED, that's unexpected —
 payment.completed means money moved.
 Even if we thought it failed,
 if payment went through we should 
 mark it COMPLETED and flag it 
 for investigation.
 
 If it's CANCELLED, money shouldn't 
 have moved. That's a data 
 inconsistency between Payment Service 
 and Expense Service.
 Log it as an error, acknowledge 
 the message (don't keep retrying — 
 it won't resolve itself),
 and flag for manual investigation."
```

```
Arjun:
───────
"Good reasoning. 
 The key insight is: 
 'this message should never arrive 
 for this state' is different from 
 'I should crash and retry.'
 
 Some unexpected states are bugs 
 in another service — retrying 
 won't help and will just 
 fill up Kafka consumer lag.
 
 Acknowledge and flag is correct 
 for data inconsistency cases.
 Only DON'T acknowledge when 
 it's a transient failure 
 (DB down, network blip) 
 that will resolve on retry."
```

This was one of the most important things Arjun taught you in this whole block. The difference between:

```
Types of consumer failures:
────────────────────────────

TRANSIENT FAILURES (don't acknowledge — retry):
  - DB connection timeout
  - Network blip
  - Temporary service unavailability
  → Will resolve on its own
  → Retry is the right behavior

PERMANENT FAILURES (acknowledge + flag):
  - Data inconsistency from another service
  - Business logic violation
  - Invalid event data
  → Retrying won't fix it
  → DLQ or manual investigation needed
  → Blocking the partition is wrong

The consumer code must distinguish 
between these two categories.
```

You updated your diagram to include the unexpected state handling.

---

### Day 3 — The Implementation

With the flow fully drawn and Arjun's feedback incorporated, you started writing.

**Step 1: The Kafka Consumer Class**

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final ReimbursementService 
        reimbursementService;

    @KafkaListener(
        topics = "payment.completed",
        groupId = "expense-service",
        containerFactory = 
            "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            PaymentCompletedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Received payment.completed: " +
            "paymentId={}, reimbursementId={}, " +
            "type={}",
            event.getPaymentId(),
            event.getReimbursementId(),
            event.getPaymentType()
        );

        // Filter — we only handle REIMBURSEMENT type
        if (event.getPaymentType() != 
                PaymentType.REIMBURSEMENT) {
            log.debug("Skipping payment.completed " +
                "event — type is {}, not REIMBURSEMENT",
                event.getPaymentType());
            acknowledgment.acknowledge();
            return;
        }

        try {
            reimbursementService
                .processPaymentCompleted(event);

            // Only acknowledge AFTER 
            // successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed " +
                "payment.completed for " +
                "reimbursementId: {}",
                event.getReimbursementId());

        } catch (TransientException e) {
            // Transient failure — 
            // DO NOT acknowledge
            // Kafka will redeliver
            log.error("Transient failure " +
                "processing payment.completed " +
                "for reimbursementId: {}. " +
                "Will retry.",
                event.getReimbursementId(), e);
            // No acknowledgment — 
            // message stays uncommitted

        } catch (PermanentException e) {
            // Permanent failure — 
            // acknowledge to unblock partition
            // but log as error for investigation
            log.error("PERMANENT FAILURE processing " +
                "payment.completed for " +
                "reimbursementId: {}. " +
                "Requires manual investigation.",
                event.getReimbursementId(), e);
            acknowledgment.acknowledge();
        }
    }
}
```

**Step 2: Custom Exception Types**

This was something you added after Arjun's explanation about transient vs permanent failures:

```java
// Transient — retry is appropriate
public class TransientException 
        extends RuntimeException {

    public TransientException(
            String message, Throwable cause) {
        super(message, cause);
    }
}

// Permanent — retry will not help
public class PermanentException 
        extends RuntimeException {

    public PermanentException(String message) {
        super(message);
    }

    public PermanentException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Step 3: The Service Method — Processing the Event**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReimbursementService {

    private final ReimbursementRepository 
        reimbursementRepository;
    private final OutboxEventRepository 
        outboxEventRepository;
    private final ReimbursementAuditService 
        auditService;

    @Transactional
    public void processPaymentCompleted(
            PaymentCompletedEvent event) {

        UUID reimbursementId = UUID.fromString(
            event.getReimbursementId()
        );

        Reimbursement reimbursement = 
            reimbursementRepository
                .findById(reimbursementId)
                .orElse(null);

        // Case 1: Reimbursement not found
        // This is a data inconsistency —
        // permanent, retry won't help
        if (reimbursement == null) {
            throw new PermanentException(
                "Reimbursement not found: " + 
                reimbursementId + 
                ". Payment reference: " + 
                event.getPaymentReference()
            );
        }

        // Case 2: Already COMPLETED — 
        // idempotency check
        // Duplicate event — skip silently
        if (reimbursement.getStatus() == 
                ReimbursementStatus.COMPLETED) {

            log.warn("Reimbursement {} already " +
                "COMPLETED. Duplicate " +
                "payment.completed event " +
                "ignored. PaymentRef: {}",
                reimbursementId,
                event.getPaymentReference()
            );
            return;
            // Return normally — 
            // caller will acknowledge
        }

        // Case 3: In PROCESSING state — 
        // expected case
        if (reimbursement.getStatus() == 
                ReimbursementStatus.PROCESSING) {

            Instant now = Instant.now();

            reimbursement.setStatus(
                ReimbursementStatus.COMPLETED);
            reimbursement.setPaymentReference(
                event.getPaymentReference());
            reimbursement.setCompletedAt(now);

            reimbursementRepository.save(reimbursement);

            // Write outbox event — same transaction
            // Notification Service will consume this
            outboxEventRepository.save(
                OutboxEvent.builder()
                    .aggregateType("REIMBURSEMENT")
                    .aggregateId(reimbursementId)
                    .eventType("reimbursement.completed")
                    .payload(buildPayload(
                        reimbursement, event))
                    .build()
            );

            // Audit log — REQUIRES_NEW 
            // (survives outer rollback)
            auditService.logReimbursementCompleted(
                reimbursementId,
                event.getPaymentReference(),
                event.getAmount()
            );

            log.info("Reimbursement {} marked " +
                "COMPLETED. Payment reference: {}",
                reimbursementId,
                event.getPaymentReference()
            );
            return;
        }

        // Case 4: Unexpected state —
        // FAILED, CANCELLED, PENDING, etc.
        // Data inconsistency — 
        // permanent, log and move on
        throw new PermanentException(
            "Unexpected reimbursement state: " + 
            reimbursement.getStatus() + 
            " for reimbursementId: " + 
            reimbursementId + 
            ". Payment reference: " + 
            event.getPaymentReference() +
            ". Requires manual investigation."
        );
    }

    private Map<String, Object> buildPayload(
            Reimbursement reimbursement,
            PaymentCompletedEvent event) {

        return Map.of(
            "reimbursementId", 
                reimbursement.getId().toString(),
            "employeeId", 
                reimbursement.getEmployeeId()
                             .toString(),
            "amount", 
                reimbursement.getAmount(),
            "currency", 
                reimbursement.getCurrency(),
            "paymentReference", 
                event.getPaymentReference(),
            "completedAt", 
                reimbursement.getCompletedAt()
                             .toString()
        );
    }
}
```

---

### Day 3 Continued — The Question About DB Failures

While writing the service method, you asked Arjun a question:

```
You:
─────
"If the DB is down when the consumer 
 tries to save the reimbursement update,
 the @Transactional method will throw 
 a DataAccessException.
 
 That's a TransientException case —
 the consumer should not acknowledge,
 let Kafka redeliver.
 
 But DataAccessException is a Spring 
 exception, not our custom TransientException.
 The consumer catches TransientException —
 so the DataAccessException would 
 fall through to the general catch?
 
 Wait — there's no general catch.
 It would propagate up uncaught.
 What happens to the Kafka listener 
 when an uncaught exception propagates?"
```

This was a good question — and Arjun was visibly pleased you had thought it through:

```
Arjun:
───────
"Exactly right to question this.

 When an uncaught exception propagates 
 out of a @KafkaListener method,
 Spring Kafka's error handler catches it.
 
 With our manual ack mode setup 
 and DefaultErrorHandler,
 the default behavior is:
 retry N times with backoff,
 then send to Dead Letter Topic (DLT).
 
 But here's the subtlety:
 
 If the listener method throws 
 WITHOUT calling acknowledge(),
 the offset is not committed.
 When the consumer restarts 
 (or on rebalance), it will 
 re-read from the last committed offset.
 
 So for transient DB failures:
 Option A — catch DataAccessException 
 specifically as TransientException:
   explicit in your code,
   easy to understand.
   
 Option B — let it propagate uncaught:
 Spring's retry mechanism handles it.
 Same effect, less code.
 
 We use Option A because it makes 
 the intent explicit.
 Anyone reading the consumer knows 
 exactly what's retried and what isn't.
 Don't rely on framework defaults 
 for things that matter this much."
```

You updated the consumer to explicitly catch `DataAccessException`:

```java
try {
    reimbursementService
        .processPaymentCompleted(event);
    acknowledgment.acknowledge();

} catch (DataAccessException e) {
    // DB is down or transient DB issue
    // Don't acknowledge — 
    // Kafka will redeliver
    log.error("DB failure processing " +
        "payment.completed for " +
        "reimbursementId: {}. Will retry.",
        event.getReimbursementId(), e);

} catch (TransientException e) {
    // Other transient failures
    log.error("Transient failure processing " +
        "payment.completed for " +
        "reimbursementId: {}. Will retry.",
        event.getReimbursementId(), e);

} catch (PermanentException e) {
    // Data inconsistency — acknowledge 
    // to unblock, log for investigation
    log.error("PERMANENT FAILURE processing " +
        "payment.completed for " +
        "reimbursementId: {}. " +
        "Manual investigation required.",
        event.getReimbursementId(), e);
    acknowledgment.acknowledge();

} catch (Exception e) {
    // Unexpected — treat as transient 
    // (safer than treating as permanent)
    log.error("Unexpected failure processing " +
        "payment.completed for " +
        "reimbursementId: {}. Will retry.",
        event.getReimbursementId(), e);
}
```

```
Why the final catch-all treats 
unknown exceptions as transient:
────────────────────────────────────
If we don't know what the exception is,
it's safer to retry than to skip.

Skipping (acknowledging) a message 
means we permanently lose the chance 
to process it.
If that was a payment completion event,
the reimbursement stays in PROCESSING.
Customer never gets marked paid.

Retrying (not acknowledging) a message
means we might process it multiple times.
Our idempotency check prevents double-updates.
So retrying is safe.
Skipping might not be.

When in doubt: retry.
```

---

### Day 4 — Writing the Tests

Writing tests for a Kafka consumer was completely new to you. You had written unit tests for services and controllers. But a Kafka consumer test was different — because the consumer was triggered by Kafka, not by a direct method call.

You asked Arjun:

```
You:
─────
"How do I test the consumer?
 Do I need to start an actual 
 Kafka broker in the test?
 Or can I just test the service 
 method directly?"
```

```
Arjun:
───────
"Both — but for different things.

 Unit test the service method directly.
 That tests your processing logic —
 idempotency, state transitions, 
 audit log, outbox event.
 This doesn't need Kafka at all.
 Just call processPaymentCompleted() 
 with a mock event.
 Fast. Simple.
 
 Integration test the consumer 
 using Testcontainers with a real 
 Kafka container.
 That tests: does the event 
 actually trigger the method?
 Does the offset get committed 
 after success?
 Does the offset NOT get committed 
 after failure?
 
 You need both.
 The unit test covers logic.
 The integration test covers 
 the Kafka wiring."
```

**Unit Tests — Service Layer:**

```java
@ExtendWith(MockitoExtension.class)
class ReimbursementServiceTest {

    @Mock
    private ReimbursementRepository 
        reimbursementRepository;

    @Mock
    private OutboxEventRepository 
        outboxEventRepository;

    @Mock
    private ReimbursementAuditService 
        auditService;

    @InjectMocks
    private ReimbursementService 
        reimbursementService;

    // ── Happy path ────────────────────────────

    @Test
    void whenReimbursementInProcessing_shouldMarkCompleted() {

        UUID reimbursementId = UUID.randomUUID();
        Reimbursement reimbursement = 
            buildReimbursement(
                reimbursementId,
                ReimbursementStatus.PROCESSING
            );

        when(reimbursementRepository
            .findById(reimbursementId))
            .thenReturn(Optional.of(reimbursement));

        PaymentCompletedEvent event = 
            buildEvent(reimbursementId.toString());

        reimbursementService
            .processPaymentCompleted(event);

        assertThat(reimbursement.getStatus())
            .isEqualTo(ReimbursementStatus.COMPLETED);
        assertThat(reimbursement.getPaymentReference())
            .isEqualTo(event.getPaymentReference());
        assertThat(reimbursement.getCompletedAt())
            .isNotNull();

        verify(reimbursementRepository).save(
            reimbursement);
        verify(outboxEventRepository).save(
            any(OutboxEvent.class));
        verify(auditService)
            .logReimbursementCompleted(
                eq(reimbursementId),
                eq(event.getPaymentReference()),
                any()
            );
    }

    // ── Idempotency ───────────────────────────

    @Test
    void whenReimbursementAlreadyCompleted_shouldSkipSilently() {

        UUID reimbursementId = UUID.randomUUID();
        Reimbursement reimbursement = 
            buildReimbursement(
                reimbursementId,
                ReimbursementStatus.COMPLETED
            );

        when(reimbursementRepository
            .findById(reimbursementId))
            .thenReturn(Optional.of(reimbursement));

        PaymentCompletedEvent event = 
            buildEvent(reimbursementId.toString());

        // Should return normally — no exception
        assertThatCode(() -> 
            reimbursementService
                .processPaymentCompleted(event))
            .doesNotThrowAnyException();

        // Should NOT save again —
        // already completed
        verify(reimbursementRepository, never())
            .save(any());
        verify(outboxEventRepository, never())
            .save(any());
    }

    // ── Permanent failure ─────────────────────

    @Test
    void whenReimbursementNotFound_shouldThrowPermanentException() {

        UUID reimbursementId = UUID.randomUUID();

        when(reimbursementRepository
            .findById(reimbursementId))
            .thenReturn(Optional.empty());

        PaymentCompletedEvent event = 
            buildEvent(reimbursementId.toString());

        assertThatThrownBy(() -> 
            reimbursementService
                .processPaymentCompleted(event))
            .isInstanceOf(PermanentException.class);
    }

    @Test
    void whenReimbursementInUnexpectedState_shouldThrowPermanentException() {

        UUID reimbursementId = UUID.randomUUID();
        Reimbursement reimbursement = 
            buildReimbursement(
                reimbursementId,
                ReimbursementStatus.CANCELLED
                // Unexpected state
            );

        when(reimbursementRepository
            .findById(reimbursementId))
            .thenReturn(Optional.of(reimbursement));

        PaymentCompletedEvent event = 
            buildEvent(reimbursementId.toString());

        assertThatThrownBy(() -> 
            reimbursementService
                .processPaymentCompleted(event))
            .isInstanceOf(PermanentException.class);
    }

    // ── Helpers ───────────────────────────────

    private Reimbursement buildReimbursement(
            UUID id, ReimbursementStatus status) {

        return Reimbursement.builder()
            .id(id)
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .amount(new BigDecimal("85.00"))
            .currency("EUR")
            .status(status)
            .build();
    }

    private PaymentCompletedEvent buildEvent(
            String reimbursementId) {

        return PaymentCompletedEvent.builder()
            .paymentId(UUID.randomUUID().toString())
            .reimbursementId(reimbursementId)
            .employeeId(UUID.randomUUID().toString())
            .amount(new BigDecimal("85.00"))
            .currency("EUR")
            .paymentReference("MOSS-SEPA-2025-001")
            .completedAt(Instant.now())
            .paymentType(PaymentType.REIMBURSEMENT)
            .build();
    }
}
```

**Integration Test — Consumer Wiring:**

```java
@SpringBootTest
@Testcontainers
class PaymentCompletedConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("expense_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(
            DockerImageName.parse(
                "confluentinc/cp-kafka:7.4.0")
        );

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url",
            postgres::getJdbcUrl);
        registry.add("spring.datasource.username",
            postgres::getUsername);
        registry.add("spring.datasource.password",
            postgres::getPassword);
        registry.add(
            "spring.kafka.bootstrap-servers",
            kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> 
        kafkaTemplate;

    @Autowired
    private ReimbursementRepository 
        reimbursementRepository;

    @Test
    void whenPaymentCompletedEventPublished_shouldUpdateReimbursement()
            throws Exception {

        // Arrange — create a PROCESSING 
        // reimbursement in DB
        UUID reimbursementId = UUID.randomUUID();
        reimbursementRepository.save(
            Reimbursement.builder()
                .id(reimbursementId)
                .status(
                    ReimbursementStatus.PROCESSING)
                // ... other fields
                .build()
        );

        // Act — publish event to Kafka
        PaymentCompletedEvent event = 
            PaymentCompletedEvent.builder()
                .reimbursementId(
                    reimbursementId.toString())
                .paymentType(
                    PaymentType.REIMBURSEMENT)
                .paymentReference("MOSS-001")
                .completedAt(Instant.now())
                .amount(new BigDecimal("85.00"))
                .currency("EUR")
                .build();

        kafkaTemplate.send(
            "payment.completed",
            reimbursementId.toString(),
            event
        );

        // Assert — wait for consumer 
        // to process (with timeout)
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                Reimbursement updated = 
                    reimbursementRepository
                        .findById(reimbursementId)
                        .orElseThrow();

                assertThat(updated.getStatus())
                    .isEqualTo(
                        ReimbursementStatus.COMPLETED);
                assertThat(
                    updated.getPaymentReference())
                    .isEqualTo("MOSS-001");
            });
    }
}
```

```
The await() pattern:
─────────────────────
Kafka consumer runs asynchronously 
in a different thread.
If you assert immediately after publish,
the consumer hasn't processed yet.

await() from the Awaitility library
polls the assertion every 100ms 
until it passes or the timeout hits.
This is the correct pattern for 
testing async behavior.
```

---

### Day 5 — PR and Elena's Observation

You opened the PR. Elena reviewed it alongside Arjun. Three comments total — all from Elena:

```
Elena's Comment 1:
───────────────────
"Good exception hierarchy. 
 One question: when you catch 
 a generic Exception at the bottom 
 and treat it as transient —
 what if it's an OutOfMemoryError?
 
 Exception doesn't catch Errors 
 in Java (they're different branches
 of Throwable), so you're fine here.
 But worth knowing why.
 
 OOM should NOT be caught and retried —
 the JVM is in trouble, retrying 
 the same operation makes it worse.
 
 Your code is correct — 
 just make sure you understand 
 why Throwable would be wrong here."
```

You had not thought about this. You looked it up:

```
Java exception hierarchy:
──────────────────────────
Throwable
├── Exception
│   ├── RuntimeException
│   │   ├── DataAccessException
│   │   ├── TransientException (ours)
│   │   └── PermanentException (ours)
│   └── checked exceptions
└── Error  ← NOT caught by catch(Exception e)
    ├── OutOfMemoryError
    ├── StackOverflowError
    └── etc.

catch(Exception e) catches Exception 
and all its subclasses.
Error is NOT a subclass of Exception.
So your catch-all does NOT catch 
JVM-level errors.
That's correct and intentional.
```

```
Elena's Comment 2:
───────────────────
"The integration test is good.
 But the await() timeout of 10 seconds —
 is that realistic for CI?
 
 In our GitHub Actions pipeline, 
 integration tests run in Docker.
 Kafka startup + consumer rebalancing 
 can take 5-8 seconds in a cold container.
 10 seconds might be tight.
 
 Consider 30 seconds for CI reliability,
 or add a comment explaining 
 the timeout choice."
```

You updated the timeout to 30 seconds and added a comment:

```java
// 30s timeout to account for Kafka 
// consumer group rebalancing 
// in CI environments (cold containers).
// Typically resolves in 5-8s locally.
await()
    .atMost(Duration.ofSeconds(30))
    .untilAsserted(() -> { ... });
```

```
Elena's Comment 3:
───────────────────
"Notice you're logging at INFO level 
 for duplicate events:
 
 log.warn('Reimbursement already COMPLETED. 
           Duplicate event ignored.')
 
 Good choice of WARN over INFO here.
 Duplicate events are expected 
 occasionally (at-least-once delivery).
 But if you see many duplicates 
 for the same reimbursementId in 
 a short period — that's a signal 
 something is wrong upstream.
 
 WARN makes those visible in 
 Datadog without drowning out 
 normal INFO logs.
 Log levels are a design decision,
 not just a severity label."
```

```
What this taught you about logging:
─────────────────────────────────────
Log levels are not just about 
how important something feels.
They're about who needs to know 
and when.

INFO: expected normal operations.
      Someone reviewing logs 
      should see these.

WARN: something unexpected but 
      handled. Could be a signal 
      of a bigger problem.
      Should be reviewed.

ERROR: something failed that 
       shouldn't have.
       Requires investigation.

The duplicate event case:
  One duplicate: expected, INFO.
  Many duplicates in short window: 
  unexpected, worth WARN so 
  it shows up in monitoring.
  Elena was teaching you to think 
  about who reads the logs and 
  what they need to action.
```

PR approved. Merged.

---

## What Happened After — The Consumer in Production

The consumer was deployed to staging first, then production at the end of the sprint.

Three days after production deployment, Arjun sent you a Datadog link in Slack:

```
Arjun (Slack):
───────────────
"Look at this — 
 your consumer in action.
 
 [Datadog link: kafka.consumer.records-lag 
  for expense-service group, 
  payment.completed topic]
 
 Lag is near 0 continuously.
 Consumer is keeping up with 
 the event rate.
 
 Also look at this:
 [Datadog link: custom outbox metric]
 
 outbox.publish.total for 
 reimbursement.completed events:
 success rate 100%.
 
 And this:
 [DB query: SELECT status, COUNT(*) 
  FROM reimbursements 
  GROUP BY status]
 
 Before your consumer: 
   PROCESSING: 847 records
   COMPLETED: 0 records
 
 After your consumer processed 
 the backlog (auto-offset-reset=earliest):
   PROCESSING: 0 records
   COMPLETED: 847 records
 
 847 reimbursements that had been 
 stuck in PROCESSING for months —
 all marked COMPLETED.
 
 That's the impact of what you built."
```

```
What this moment felt like:
────────────────────────────
You had built things before.
You had fixed bugs. 
You had improved endpoints.

But this was different.
847 reimbursement records, 
stuck for months, 
suddenly resolved because 
of code you wrote.

You could see it in the DB query.
Not abstract. Real data. Real impact.

This was the first time you felt 
the difference between writing code 
and solving a problem.
They're not the same thing.
```

---

## The Result

```
What shipped:
──────────────
PaymentCompletedConsumer in production.
847 historical reimbursements 
  moved from PROCESSING to COMPLETED.
Reimbursement processing now 
  fully event-driven — no manual 
  intervention needed.
0 bugs in first two weeks.

What you learned:
──────────────────
1. Manual offset commit — why it exists 
   and when NOT to call acknowledge()

2. Transient vs permanent failures —
   the most important consumer 
   design decision

3. Idempotency in consumers —
   checking state before updating,
   duplicate events handled correctly

4. auto-offset-reset=earliest and 
   when it matters

5. PaymentType filtering on 
   shared topics

6. Integration test pattern 
   with Testcontainers + Awaitility 
   for async consumer behavior

7. Log level as a design decision —
   not just severity

8. Java exception hierarchy —
   why catch(Exception) is safe 
   but catch(Throwable) is not

Relationship change with Arjun:
────────────────────────────────
Before this story:
  You knew of Arjun.
  He existed in standups and 
  in the codebase.
  You had not worked directly.

After this story:
  Arjun was your primary mentor 
  for anything event-driven.
  You pair-programmed together.
  He explained things in a way 
  that was methodical and patient.
  You started asking him questions 
  outside of specific tickets —
  just to understand things better.
  
  That kind of relationship — 
  where you seek someone out 
  because they explain well,
  not because they're assigned 
  to help you — is how you 
  actually learn in a team.
```

---

## The "Tricky Question" Preparation

---

**Q1: "Why did you use manual offset commits instead of auto-commit?"**

```
Auto-commit commits offsets every 
5 seconds by default — regardless 
of whether your processing code 
actually succeeded.

For a financial system, that's dangerous.
If the DB call to update the 
reimbursement status fails AFTER 
the auto-commit fires,
Kafka marks the message as processed.
You never retry it.
The reimbursement stays in PROCESSING.
The customer's payment is never 
acknowledged in our system.

Manual commit means we call 
acknowledgment.acknowledge() 
only AFTER successful DB write.
If DB fails, we don't acknowledge.
Kafka redelivers the message.
We retry.

This gives us at-least-once delivery —
the same message might arrive twice,
but we never lose one.
Our idempotency check handles 
the duplicate case safely.
```

---

**Q2: "What's the difference between transient and permanent failures in a consumer? How did you implement this?"**

```
Transient failures are temporary —
they'll resolve on their own 
with time or retry.
Examples: DB connection timeout,
network blip, service temporarily down.

Permanent failures won't resolve 
with retrying — they indicate 
a data problem or a bug that 
needs human investigation.
Examples: reimbursement ID doesn't exist,
business logic violation, 
unexpected state in another service.

For transient failures: 
don't acknowledge.
Kafka redelivers.
Eventually the transient condition 
clears and processing succeeds.

For permanent failures: 
acknowledge to unblock the partition
(other messages behind this one 
must not be blocked forever),
but log at ERROR level so 
the on-call engineer investigates.

In the code I created two custom 
exception types — TransientException 
and PermanentException — and 
caught DataAccessException explicitly 
as transient (DB issues).
The consumer's catch blocks 
explicitly handle each category 
differently.

The key insight Arjun gave me:
blocking the partition by 
not acknowledging is only correct 
when retrying WILL eventually work.
For data inconsistency problems,
retrying 10,000 times won't help.
Acknowledge and alert humans.
```

---

**Q3: "What is idempotency and how did you implement it in this consumer?"**

```
Idempotency means: calling the 
same operation multiple times 
produces the same result as 
calling it once.

In our case: if the same 
payment.completed event arrives 
twice — which can happen with 
Kafka's at-least-once guarantee —
the second processing should 
have no effect.

The implementation is straightforward:
before updating the reimbursement,
check its current status.

If it's already COMPLETED:
the first processing already ran.
Log a warning and return normally.
Don't save again, don't create 
another outbox event.

If it's PROCESSING:
first time processing.
Update, save, create outbox event.

This check makes the consumer 
safe to call multiple times 
with the same event.
The state change only happens once.

This works because status transitions 
are one-directional —
COMPLETED can't go back to PROCESSING.
So checking the terminal state 
is a reliable idempotency guard.
```

---

**Q4: "You mentioned auto-offset-reset=earliest caused 847 historical reimbursements to be processed. How does that work?"**

```
payment.completed events had been 
publishing to Kafka for months 
before this consumer existed.

Kafka retains events for a configured 
retention period — in our case 7 days.
But even beyond that, consumer 
group offsets are tracked separately 
from event retention.

The key is auto-offset-reset:
when expense-service consumer group 
reads payment.completed for the 
first time, there is no committed 
offset for this group + topic combination.

Kafka has to decide where to start.
auto-offset-reset=earliest means:
start from the very beginning of 
what's available in the topic.

So when I deployed the consumer,
it started reading from the oldest 
available event and worked forward.
It processed all 847 payment.completed 
events that had accumulated.
For each one, it found a reimbursement 
in PROCESSING state and updated it.

If I had used auto-offset-reset=latest,
it would have only processed new events 
from deployment time forward.
The 847 historical reimbursements 
would have stayed stuck.

Choosing earliest was the right 
decision for this use case because 
we specifically needed to catch up 
on historical data.
```

---

Story 8 complete.

```
What this story shows:
───────────────────────
Technical:
  First real Kafka consumer 
  implementation in production.
  Manual offset commits.
  Transient vs permanent failure 
  distinction — the most important 
  consumer design decision.
  Idempotency implementation.
  Integration testing with 
  Testcontainers + Awaitility.
  Log level as design decision.

Behavioral:
  Shadowed existing code before 
  writing anything new.
  Drew the full flow before coding.
  Asked good questions about edge cases.
  Applied earlier lessons 
  (transient vs permanent from 
   Arjun's explanation).
  Measured real impact in production 
  (847 records fixed).

Relationship:
  First real collaborative work 
  with Arjun — from knowing him 
  to working closely with him.
  This relationship deepens in 
  Stories 9, 10 of Block 3.
```

Shall I begin Story 9 — the production incident war room?