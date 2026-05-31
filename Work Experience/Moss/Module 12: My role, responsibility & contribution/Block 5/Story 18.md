# Story 18: Dead Letter Queue Implementation — Making the System Resilient to Permanent Failures

---

## Context — Where You Were at Month 15

```
Month 15. The final month of Block 5.

By this point, the arc of the 
last three months was clear:

Story 16 (month 13):
  Led a production incident.
  Not observed it — led it.
  Root cause in 14 minutes.
  Postmortem written, reviewed, 
  published.

Story 17 (month 14):
  Caught a breaking migration risk 
  in sprint planning.
  Proposed the safe alternative.
  Implemented it across two sprints.
  Zero incidents.

Now month 15.

You were no longer the person 
who implemented what was designed.
You were the person who designed 
and implemented — and who caught 
problems before they reached 
production.

But there was one area of the 
system that still made you 
slightly uneasy when you thought 
about it:

Exception handling in Kafka consumers.
```

In Story 8, Arjun had taught you the distinction between transient and permanent failures. You had implemented it correctly in the `PaymentCompletedConsumer`. You had created `TransientException` and `PermanentException` classes. For permanent failures — data inconsistency, business logic violations — you acknowledged the message (to unblock the partition) and logged at ERROR level.

But "log at ERROR and move on" was not a complete solution. Logged errors were visible in Datadog. But they required someone to notice them, investigate them, and manually intervene. There was no systematic mechanism to capture permanently-failed messages, inspect them, and replay them.

That gap had been fine at Series B scale when permanent failures were rare. But the Invoice Service was processing more volume now. In the previous sprint, Arjun had flagged this in a team Slack message:

```
Arjun (#expense-ap-dev):
──────────────────────────
"Something I want to think about 
 as a team:
 
 invoice-service has three Kafka 
 consumers. All three handle 
 permanent failures by logging 
 ERROR and acknowledging.
 
 In the last 30 days I've seen 
 5 ERROR logs from these consumers 
 for data inconsistency issues —
 all from payment.completed events 
 for invoice IDs that don't exist 
 in our system.
 
 5 events in 30 days that we 
 acknowledged and effectively 
 discarded.
 
 At our current volume that's 
 acceptable.
 At 10x volume, that's 50 events/month
 that we need a process to handle.
 
 I want us to think about whether 
 we should implement Dead Letter 
 Queues for our consumers.
 
 [Your name] — this might be 
 a good ticket for you given 
 your Kafka work so far."
```

That message was in the previous sprint. It sat in the backlog. In month 15 sprint planning, Lukas pulled it into the sprint.

---

## The Situation

Sprint planning, month 15 week 1:

```
Ticket: INV-341
Title:  Implement Dead Letter Queue 
        for Invoice Service Kafka consumers

Background:
────────────
Invoice Service has 3 Kafka consumers:
  1. PaymentCompletedConsumer 
     (payment.completed → mark invoice PAID)
  2. UserDeactivatedConsumer
     (user.deactivated → reassign approvals)
  3. InvoiceVerificationRequestedConsumer
     (invoice.verification.requested → 
      assign verifier)

Each consumer currently handles 
permanent failures by:
  - Logging ERROR
  - Acknowledging the message 
    (unblocks partition)
  - Effectively discarding the event

This means permanently-failed events 
are lost with no ability to inspect, 
investigate, or replay them.

Goal:
──────
Route permanently-failed events to 
Dead Letter Topics (DLTs) instead 
of discarding them.

Provide:
  1. A DLT per topic 
     (payment.completed.DLT, etc.)
  2. Automated retry before DLT 
     routing (3 retries, backoff)
  3. Monitoring: alert when DLT 
     has messages
  4. A runbook for DLT investigation 
     and replay

Story points: 8
Assigned to: You
```

Eight story points. The same as Story 5 (the first complex feature) but you were now a very different engineer than you were at month 5.

---

## Your Task

```
What you owned:
────────────────
Design the DLQ implementation 
for all 3 consumers.

Implement:
  1. DLT routing configuration 
     in Spring Kafka
  2. Retry policy (before DLT)
  3. DLT topic creation
  4. Error handler + exception classification
  5. DLT monitoring alert in Datadog
  6. Runbook for DLT investigation 
     and replay

What you had to figure out 
before writing code:
─────────────────────────────
The current exception handling 
was manual — each consumer's 
catch block decided what to do.
Spring Kafka's DLQ mechanism 
is automatic — it's configured 
at the container factory level.

The question:
  How do you integrate Spring Kafka's 
  DefaultErrorHandler (automatic DLT routing)
  with your existing manual 
  TransientException / PermanentException 
  classification?
  
  Can you tell the error handler 
  "retry for TransientException 
   but go straight to DLT for 
   PermanentException"?
  
  Answer: yes.
  That was the design problem 
  to solve.
```

---

## Day 1 — Design Before Code

You spent the first morning drawing out the design before writing a single line.

```
CURRENT STATE (before DLQ):
──────────────────────────────

Message arrives at consumer
       │
       ▼
Consumer tries to process
       │
  ┌────┴────┐
  │         │
SUCCESS   FAILURE
  │         │
  │    ┌────┴──────────────┐
  ▼    │                   │
Ack  TRANSIENT          PERMANENT
     FAILURE            FAILURE
       │                   │
     Don't ack         Ack + log ERROR
     (retry from         (message lost —
     last offset)         no DLT)


PROBLEMS:
  Transient failures: eventually 
    succeed on retry OR block the 
    partition indefinitely if 
    they never resolve.
  Permanent failures: silently 
    discarded after logging.
    No inspection, no replay.
```

```
TARGET STATE (with DLQ):
──────────────────────────

Message arrives at consumer
       │
       ▼
Consumer tries to process
       │
  ┌────┴────┐
  │         │
SUCCESS   FAILURE
  │         │
  ▼    ┌────┴──────────────┐
 Ack   │                   │
     TRANSIENT          PERMANENT
     FAILURE            FAILURE
       │                   │
   Retry (up to 3x        Send to DLT
   with exponential       immediately
   backoff)               (no retries)
       │                   │
  ┌────┴────┐              ▼
  │         │    payment.completed.DLT
SUCCESS   STILL           │
  │       FAILING         ▼
  ▼         │         Datadog alert
 Ack        │         fires (DLT has 
         Send to DLT  messages)
             │              │
             ▼              ▼
  payment.completed.DLT  On-call engineer
             │           investigates
             ▼           
         Messages        
         inspectable     
         + replayable   
```

You sent this diagram to Arjun with a question:

```
You (Slack to Arjun):
──────────────────────
"Quick design question before 
 I start implementing.
 
 The current consumers have manual 
 try-catch blocks that classify 
 exceptions as transient or permanent.
 
 Spring Kafka's DefaultErrorHandler 
 is configured at the container 
 factory level — it handles 
 all consumers with the same 
 retry + DLT logic.
 
 Can I configure the DefaultErrorHandler 
 to skip retries for PermanentException 
 (go straight to DLT) while still 
 retrying for other exceptions?
 
 Or do I need to keep the manual 
 try-catch in each consumer and 
 route to DLT from inside the catch?"
```

Arjun replied:

```
Arjun:
───────
"Yes — DefaultErrorHandler supports this.
 It has a method called 
 addNotRetryableExceptions().
 
 Any exception class you add there 
 will go straight to DLT on 
 first failure — no retries.
 
 So you'd configure:
   errorHandler.addNotRetryableExceptions(
       PermanentException.class
   )
 
 And DataAccessException (DB down) 
 would still go through the retry path.
 
 The manual try-catch can be 
 simplified significantly — 
 instead of catching and classifying 
 in each consumer, you just throw 
 the right exception type and let 
 the error handler route it.
 
 But keep catch(DataAccessException) 
 in the consumer — I want that 
 explicit, not left to the 
 framework to classify."
```

Good. You had your design approach. You wrote a short design doc — not an ADR, but a planning note in Confluence:

```
DESIGN NOTE: DLQ Implementation
─────────────────────────────────

APPROACH:
Spring Kafka DefaultErrorHandler with:
  - Retry policy: 3 attempts, 
    1s/2s/4s exponential backoff
  - Not-retryable: PermanentException
    (goes straight to DLT)
  - Retryable: everything else
    (DataAccessException, 
     TransientException, 
     unexpected exceptions)
  - DLT publisher: 
    DeadLetterPublishingRecoverer

DLT NAMING:
  {original-topic}.DLT
  Examples:
    payment.completed.DLT
    user.deactivated.DLT
    invoice.verification.requested.DLT

DLT ROUTING:
  Messages sent to DLT carry 
  original message headers plus 
  additional headers:
    kafka_dlt-exception-message: 
      error message
    kafka_dlt-exception-stacktrace: 
      full stack trace
    kafka_dlt-original-topic: 
      source topic
    kafka_dlt-original-partition: 
      source partition
    kafka_dlt-original-offset: 
      original offset
  These headers are added 
  automatically by Spring Kafka.
  Critical for investigation.

CONSUMER CHANGES:
  Simplify catch blocks.
  Consumers throw the right 
  exception type.
  Error handler routes automatically.
  
  Keep explicit catch(DataAccessException) 
  in each consumer — want this visible,
  not hidden in framework.

MONITORING:
  Custom metric: dlt.messages.total
    tags: topic, reason
  Datadog alert: dlt.messages.total > 0 
    for 5 minutes → WARNING

RUNBOOK:
  Written in Confluence.
  Steps: find DLT messages, 
  read headers, investigate, 
  replay or discard.
```

---

## The Implementation

### Step 1 — Kafka Consumer Configuration

The core of the DLQ implementation was the `KafkaConsumerConfig`:

```java
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaTemplate<String, Object> 
        kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String, Object>
            kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> 
                consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<
            String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // Manual ack — we control offset commits
        factory.getContainerProperties()
            .setAckMode(
                ContainerProperties.AckMode.MANUAL);

        // Set the error handler
        factory.setCommonErrorHandler(
            buildErrorHandler());

        return factory;
    }

    private CommonErrorHandler buildErrorHandler() {

        // Where to send messages 
        // that exhausted retries 
        // or are not-retryable.
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    // DLT topic = original topic + ".DLT"
                    // DLT partition = same partition 
                    // as original (preserves ordering 
                    // of DLT messages per partition)
                    String dltTopic = 
                        record.topic() + ".DLT";

                    // Record custom metric
                    recordDltMessage(
                        record.topic(), 
                        exception
                    );

                    log.error(
                        "Routing message to DLT. " +
                        "Topic: {}, Partition: {}, " +
                        "Offset: {}, Exception: {}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception.getMessage()
                    );

                    return new TopicPartition(
                        dltTopic,
                        record.partition()
                    );
                }
            );

        // Retry policy: exponential backoff
        // Attempt 1: immediately
        // Attempt 2: after 1 second
        // Attempt 3: after 2 seconds
        // After attempt 3: route to DLT
        ExponentialBackOffWithMaxRetries backOff =
            new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);  // 1 second
        backOff.setMultiplier(2.0);           // 1s → 2s → 4s
        backOff.setMaxInterval(10_000L);      // cap at 10s

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(recoverer, backOff);

        // PermanentException = no retries.
        // Route to DLT immediately on first failure.
        // These represent data inconsistency or 
        // business logic violations that retrying 
        // will never fix.
        errorHandler.addNotRetryableExceptions(
            PermanentException.class
        );

        // DataAccessException is retryable —
        // DB may be temporarily unavailable.
        // DefaultErrorHandler retries ALL exceptions 
        // except the ones in addNotRetryableExceptions().
        // So DataAccessException is retried by default.
        // No explicit configuration needed.

        return errorHandler;
    }

    private void recordDltMessage(
            String sourceTopic,
            Exception exception) {

        Counter.builder("dlt.messages.total")
            .tag("source_topic", sourceTopic)
            .tag("exception_type",
                exception.getClass().getSimpleName())
            .description(
                "Total messages routed to DLT")
            .register(meterRegistry)
            .increment();
    }
}
```

```
Why exponential backoff?
────────────────────────────
Attempt 1: immediately
  First try — maybe it was a 
  momentary DB blip.

Attempt 2: after 1 second
  Give DB time to recover.

Attempt 3: after 2 seconds
  One more try before giving up.

After 3 failures: DLT.

Why exponential (1s, 2s, 4s) 
and not fixed (1s, 1s, 1s)?
  If 100 consumers are all failing 
  on DB connection issues simultaneously
  and retrying every 1 second, 
  they hammer the DB in waves.
  
  With exponential backoff, 
  retries spread out over time.
  If DB is recovering, the later 
  retries give it more time to 
  come back without constant hammering.

Why cap at 10 seconds (maxInterval)?
  Exponential without a cap would 
  eventually produce wait times 
  of minutes or hours.
  For our consumers, 10 seconds 
  is long enough to let transient 
  failures resolve without 
  blocking partitions excessively.
```

### Step 2 — Simplifying the Consumers

With the error handler in place, each consumer could be significantly simplified. Previously, each one had a complex try-catch structure. Now the catch blocks became cleaner:

```java
// BEFORE (PaymentCompletedConsumer):
@KafkaListener(
    topics = "payment.completed",
    groupId = "invoice-service"
)
public void handlePaymentCompleted(
        PaymentCompletedEvent event,
        Acknowledgment acknowledgment) {

    try {
        invoiceService.processPaymentCompleted(event);
        acknowledgment.acknowledge();

    } catch (DataAccessException e) {
        log.error("DB failure — will retry.",
            event.getInvoiceId(), e);
        // No ack — error handler retries

    } catch (TransientException e) {
        log.error("Transient failure — will retry.",
            event.getInvoiceId(), e);
        // No ack — error handler retries

    } catch (PermanentException e) {
        log.error("PERMANENT FAILURE — manual " +
            "investigation required.",
            event.getInvoiceId(), e);
        acknowledgment.acknowledge(); // ← was manual
        // ← now this is handled by error handler

    } catch (Exception e) {
        log.error("Unexpected failure — will retry.",
            event.getInvoiceId(), e);
        // No ack
    }
}
```

```java
// AFTER (PaymentCompletedConsumer):
// Significantly simplified.
// Error handler manages retry + DLT routing.
@KafkaListener(
    topics = "payment.completed",
    groupId = "invoice-service"
)
public void handlePaymentCompleted(
        PaymentCompletedEvent event,
        Acknowledgment acknowledgment) {

    try {
        invoiceService.processPaymentCompleted(event);
        acknowledgment.acknowledge();

    } catch (DataAccessException e) {
        // DB unavailable — transient.
        // Do NOT acknowledge.
        // Error handler retries with backoff.
        // After 3 attempts → DLT.
        log.error("DB failure processing " +
            "payment.completed for invoiceId: {}. " +
            "Retry will be attempted.",
            event.getInvoiceId(), e);
        throw e;   // ← re-throw for error handler

    }
    // PermanentException: thrown by invoiceService,
    // propagates to error handler,
    // immediately routed to DLT.
    // No catch needed here.
    
    // Other exceptions: propagate to error handler,
    // retried 3 times, then DLT if still failing.
}
```

```
Key change: manual acknowledge() 
for permanent failures REMOVED.

Before: consumer caught PermanentException,
        manually acknowledged to unblock partition.

After: PermanentException propagates 
       to error handler, which routes 
       to DLT and acknowledges automatically.
       
The behavior is the same — partition unblocked —
but now the message is in the DLT 
instead of being silently discarded.

The crucial difference:
Before: logged and gone.
After: logged AND in DLT for investigation.
```

You applied the same simplification to all three consumers:

```java
// UserDeactivatedConsumer — simplified
@KafkaListener(
    topics = "user.deactivated",
    groupId = "invoice-service"
)
public void handleUserDeactivated(
        UserDeactivatedEvent event,
        Acknowledgment acknowledgment) {

    try {
        invoiceService.processUserDeactivated(event);
        acknowledgment.acknowledge();

    } catch (DataAccessException e) {
        log.error("DB failure processing " +
            "user.deactivated for userId: {}. " +
            "Retry will be attempted.",
            event.getUserId(), e);
        throw e;
    }
}

// InvoiceVerificationRequestedConsumer — simplified
@KafkaListener(
    topics = "invoice.verification.requested",
    groupId = "invoice-service"
)
public void handleVerificationRequested(
        InvoiceVerificationRequestedEvent event,
        Acknowledgment acknowledgment) {

    try {
        invoiceService.processVerificationRequested(event);
        acknowledgment.acknowledge();

    } catch (DataAccessException e) {
        log.error("DB failure processing " +
            "invoice.verification.requested " +
            "for invoiceId: {}. " +
            "Retry will be attempted.",
            event.getInvoiceId(), e);
        throw e;
    }
}
```

### Step 3 — DLT Topic Creation

You added a Kafka topic configuration bean to create the DLT topics on startup:

```java
@Configuration
public class KafkaTopicConfig {

    // Application topics (already existed)
    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder
            .name("payment.completed")
            .partitions(6)
            .replicas(3)
            .build();
    }

    // DLT topics — created alongside 
    // their source topics
    @Bean
    public NewTopic paymentCompletedDlt() {
        return TopicBuilder
            .name("payment.completed.DLT")
            // Same partition count as source —
            // DLT messages routed to same partition
            // as original for ordering correlation
            .partitions(6)
            .replicas(3)
            // Longer retention for DLT —
            // messages may need investigation 
            // over days or weeks.
            // Source topic: 7 days.
            // DLT: 30 days.
            .config(
                TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(
                    Duration.ofDays(30).toMillis())
            )
            .build();
    }

    @Bean
    public NewTopic userDeactivatedDlt() {
        return TopicBuilder
            .name("user.deactivated.DLT")
            .partitions(3)
            .replicas(3)
            .config(
                TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(
                    Duration.ofDays(30).toMillis())
            )
            .build();
    }

    @Bean
    public NewTopic invoiceVerificationRequestedDlt() {
        return TopicBuilder
            .name(
                "invoice.verification.requested.DLT")
            .partitions(3)
            .replicas(3)
            .config(
                TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(
                    Duration.ofDays(30).toMillis())
            )
            .build();
    }
}
```

```
Why 30-day retention for DLT?
────────────────────────────────
Source topics have 7-day retention.
If a message fails and lands in the DLT,
you might not investigate it immediately.
  
  Maybe it happens on a Friday evening.
  The on-call engineer acknowledges the alert,
  confirms there's no immediate customer impact,
  and defers investigation to Monday.
  
  With 7-day DLT retention: fine.
  
  But what if the team is in a busy sprint
  and the investigation gets deferred
  to the following week?
  With 7-day retention: message gone.
  With 30-day retention: still there.

DLT messages are rare.
30 days of extra retention 
costs almost nothing in storage.
Losing an uninvestigated DLT message
costs potentially hours of debugging.
```

### Step 4 — Testing the DLQ

This was the most interesting testing challenge. How do you test that messages route to the DLT correctly?

```java
@SpringBootTest
@Testcontainers
class DLTRoutingIntegrationTest {

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(
            DockerImageName.parse(
                "confluentinc/cp-kafka:7.4.0")
        );

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {
        registry.add(
            "spring.kafka.bootstrap-servers",
            kafka::getBootstrapServers);
        registry.add("spring.datasource.url",
            postgres::getJdbcUrl);
        // ... other properties
    }

    @Autowired
    private KafkaTemplate<String, Object> 
        kafkaTemplate;

    @MockBean
    private InvoiceService invoiceService;

    @Autowired
    private KafkaListenerEndpointRegistry 
        listenerRegistry;

    // ── Test: PermanentException → DLT ──────────

    @Test
    void whenPermanentExceptionThrown_shouldRouteDirectlyToDlt()
            throws Exception {

        UUID invoiceId = UUID.randomUUID();

        // Service throws PermanentException 
        // (invoice not found, data inconsistency)
        given(invoiceService.processPaymentCompleted(any()))
            .willThrow(new PermanentException(
                "Invoice not found: " + invoiceId
            ));

        PaymentCompletedEvent event = buildEvent(invoiceId);

        // Publish to source topic
        kafkaTemplate.send(
            "payment.completed",
            invoiceId.toString(),
            event
        );

        // Verify:
        // 1. Message appeared in DLT (not retried)
        // 2. Service called ONCE (no retries 
        //    for PermanentException)
        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {

                // Service should have been called exactly once
                // (PermanentException = no retry)
                verify(invoiceService, times(1))
                    .processPaymentCompleted(any());

                // DLT should have the message
                assertDltHasMessage(
                    "payment.completed.DLT",
                    invoiceId.toString()
                );
            });
    }

    // ── Test: Transient failure → retry → DLT ──

    @Test
    void whenTransientExceptionThenPermanentFailure_shouldRetryThenDlt()
            throws Exception {

        UUID invoiceId = UUID.randomUUID();

        // First 3 calls throw DataAccessException 
        // (DB unavailable — transient)
        given(invoiceService.processPaymentCompleted(any()))
            .willThrow(new DataAccessException(
                "DB unavailable") {})
            .willThrow(new DataAccessException(
                "DB unavailable") {})
            .willThrow(new DataAccessException(
                "DB unavailable") {});

        kafkaTemplate.send(
            "payment.completed",
            invoiceId.toString(),
            buildEvent(invoiceId)
        );

        // After 3 retries, should go to DLT
        await()
            .atMost(Duration.ofSeconds(30))
            // Enough time for 3 retries with backoff
            // (1s + 2s + 4s = 7s total wait)
            .untilAsserted(() -> {

                // Service called 3 times 
                // (original + 2 retries = 3 attempts)
                verify(invoiceService, times(3))
                    .processPaymentCompleted(any());

                // Message in DLT after exhausting retries
                assertDltHasMessage(
                    "payment.completed.DLT",
                    invoiceId.toString()
                );
            });
    }

    // ── Test: Success on retry ──────────────────

    @Test
    void whenTransientThenSuccess_shouldSucceedWithoutDlt()
            throws Exception {

        UUID invoiceId = UUID.randomUUID();

        // First call throws, second succeeds
        given(invoiceService.processPaymentCompleted(any()))
            .willThrow(new DataAccessException(
                "DB momentarily unavailable") {})
            .willReturn(null); // success on second call

        kafkaTemplate.send(
            "payment.completed",
            invoiceId.toString(),
            buildEvent(invoiceId)
        );

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {

                // Called twice (fail + succeed)
                verify(invoiceService, times(2))
                    .processPaymentCompleted(any());

                // DLT should be empty
                assertDltIsEmpty("payment.completed.DLT");
            });
    }

    // ── Helpers ─────────────────────────────────

    private void assertDltHasMessage(
            String dltTopic,
            String expectedKey) {

        // Create a temporary consumer to check DLT
        try (KafkaConsumer<String, Object> consumer =
                createDltConsumer()) {

            consumer.subscribe(
                Collections.singletonList(dltTopic));
            consumer.seekToBeginning(
                consumer.assignment());

            ConsumerRecords<String, Object> records =
                consumer.poll(Duration.ofSeconds(5));

            assertThat(records.count())
                .isGreaterThan(0);

            boolean found = StreamSupport
                .stream(records.spliterator(), false)
                .anyMatch(r -> 
                    expectedKey.equals(r.key()));

            assertThat(found)
                .as("Expected message with key %s " +
                    "in DLT %s", expectedKey, dltTopic)
                .isTrue();
        }
    }

    private void assertDltIsEmpty(String dltTopic) {
        try (KafkaConsumer<String, Object> consumer =
                createDltConsumer()) {

            consumer.subscribe(
                Collections.singletonList(dltTopic));

            ConsumerRecords<String, Object> records =
                consumer.poll(Duration.ofSeconds(3));

            assertThat(records.count()).isZero();
        }
    }
}
```

---

### Step 5 — The Monitoring Alert

You added a Datadog alert for the DLT metric:

```
Datadog Alert Configuration:
──────────────────────────────
Alert name: DLT messages detected 
            in invoice-service

Metric: dlt.messages.total
Filter: service:invoice-service

Condition: sum(last_5m) > 0

Severity: WARNING

Notification: #expense-ap-alerts

Message:
  Dead letter messages detected 
  in invoice-service.
  
  Topic: {{source_topic}}
  Exception: {{exception_type}}
  
  Action required:
  1. Check Datadog Logs for full context
     service:invoice-service level:ERROR
  2. Inspect DLT messages using 
     Kafka UI or kafka-console-consumer
  3. Determine: data inconsistency 
     (investigate upstream) or 
     code bug (fix and replay)
  4. See runbook: [Confluence link]
  
  Do NOT ignore DLT messages — 
  they represent events that 
  could not be processed.
```

```
Why sum(last_5m) > 0 
and not a count threshold?
────────────────────────────
A DLT message is never expected 
in normal operation.
Even ONE is worth alerting on.

Compare to consumer lag alerts 
where you set lag > 1000 
before alerting — because some 
lag is normal during catch-up.

DLT = something permanently failed.
The threshold is 0.
Any DLT message needs attention.
```

---

### Step 6 — The Runbook

You wrote the runbook in Confluence. This was something Arjun had specifically asked for in his Slack message — *"A runbook for DLT investigation and replay."*

```
RUNBOOK: DLT Investigation and Replay
Author: [Your name]
Reviewed by: Arjun Sharma

─────────────────────────────────────────

WHEN TO USE THIS RUNBOOK:
──────────────────────────
Datadog alert fires: 
  "DLT messages detected in invoice-service"

─────────────────────────────────────────

STEP 1: FIND THE DLT MESSAGES
───────────────────────────────
In Datadog Logs, search:
  service:invoice-service 
  AND level:ERROR 
  AND message:*Routing message to DLT*
  (time range: last 1 hour)

This shows:
  - Which topic (source_topic)
  - Which partition and offset
  - The exception message

─────────────────────────────────────────

STEP 2: READ THE FULL MESSAGE
───────────────────────────────
The DLT message has headers that 
contain the full error context.

Using kafka-console-consumer in staging 
(or read-only Kafka UI in production):

kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic payment.completed.DLT \
  --from-beginning \
  --property print.headers=true \
  --property print.key=true

Headers to look at:
  kafka_dlt-exception-message: 
    The exception message 
    (e.g. "Invoice not found: uuid-123")
  kafka_dlt-exception-fqcn:
    The exception class
  kafka_dlt-original-topic:
    Which topic it came from
  kafka_dlt-original-offset:
    Original offset (for correlation 
    with producer logs)

─────────────────────────────────────────

STEP 3: DETERMINE THE ROOT CAUSE
───────────────────────────────────
Common scenarios:

SCENARIO A: Data inconsistency
  Example: "Invoice not found: uuid-123"
  
  This means Payment Service published 
  a payment.completed event for an 
  invoice ID that doesn't exist in 
  our database.
  
  Possible causes:
  - Invoice was deleted after payment
  - Event published for wrong invoice ID 
    (data bug in Payment Service)
  - Invoice exists in invoice-service 
    but under a different company 
    (cross-company data issue)
  
  Action:
  1. Check if invoice exists in DB:
     SELECT * FROM invoices 
     WHERE id = 'uuid-123';
  
  2. If invoice exists: 
     Bug in our processing logic.
     Fix the code. Replay message.
  
  3. If invoice doesn't exist:
     Cross-service data inconsistency.
     Inform Payment Service team.
     Check their records for the payment.
     May need manual reconciliation.

SCENARIO B: Transient failure 
            exhausted retries
  Example: DataAccessException after 3 attempts
  
  The DB was unavailable long enough 
  that all 3 retry attempts failed.
  
  Action:
  1. Verify DB is now healthy 
     (Datadog DB metrics, connection pool).
  2. If DB is healthy: replay message.
  3. If DB is still unhealthy: 
     Fix DB first, then replay.

─────────────────────────────────────────

STEP 4: REPLAY A DLT MESSAGE
───────────────────────────────
Replaying means publishing the 
message back to the original topic.
The consumer will then process it again.

Method: Kafka UI (available at 
internal URL) has a "Replay" feature 
for DLT messages.

Or via script:
kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic payment.completed.DLT \
  --from-beginning \
  --max-messages 1 \
  | kafka-console-producer \
  --bootstrap-server kafka:9092 \
  --topic payment.completed

NOTE: Before replaying, ensure the 
root cause is fixed. Replaying a message 
whose root cause hasn't been fixed 
just lands it back in the DLT.

─────────────────────────────────────────

STEP 5: MARK AS RESOLVED
──────────────────────────
After a DLT message has been 
investigated and either:
  - Replayed successfully, or
  - Determined to be a legitimate 
    permanent failure (no further 
    action possible)

Update the Datadog incident 
or Slack thread with:
  - Root cause
  - Action taken
  - Whether replay was performed
  - Any cross-team communication 
    (e.g., informed Payment Service 
    of data inconsistency)

─────────────────────────────────────────

STEP 6: PREVENT RECURRENCE
────────────────────────────
If the DLT message was due to a bug 
in our code:
  Create a ticket for the fix.
  
If due to data inconsistency from 
another service:
  Raise with that team.
  Consider adding validation 
  in our consumer to handle 
  the inconsistency gracefully 
  (e.g., log + skip instead of throw 
  PermanentException, if the 
  scenario is expected).
```

---

## Elena's Review — And the One Thing She Added

Elena reviewed the PR. Four comments. Three were minor. The fourth was the one that mattered:

```
Elena Comment 4:
─────────────────
"The runbook is clear. 
 Good.
 
 One gap: what happens to in-flight 
 DLT messages when we deploy 
 a new version of the consumer?
 
 Scenario:
 10 messages are in 
 payment.completed.DLT.
 We deploy a new version of 
 invoice-service (bug fix or 
 new feature).
 
 Does the DLT consumer 
 (if we add one later) 
 automatically pick up the 
 old messages?
 
 Or do we need to manually replay 
 them?
 
 Answer: DLT topics retain messages 
 like any other topic (30 days, 
 per your config).
 
 But currently we have NO consumer 
 for the DLT topics — messages 
 sit there until manually replayed.
 
 This is correct for now — manual 
 investigation is appropriate before 
 replay.
 
 But add a note to the runbook 
 clarifying: DLT messages are NOT 
 automatically reprocessed on redeploy.
 They require explicit action.
 
 Future consideration: automated 
 DLT replay after a configurable 
 delay (e.g., retry automatically 
 after 24 hours). Not in scope 
 for this ticket but worth noting."
```

You updated the runbook with that clarification and added a "Future Considerations" section:

```
IMPORTANT: DLT messages are NOT 
automatically reprocessed.
They persist for 30 days and require 
explicit manual replay.
Deploying a new version of 
invoice-service does NOT automatically 
replay DLT messages.

─────────────────────────────────────────

FUTURE CONSIDERATIONS (not in scope):
───────────────────────────────────────
Automated DLT replay after 24 hours:
  A scheduler could automatically 
  replay DLT messages after a delay,
  assuming transient failures have 
  resolved.
  Risk: automated replay for 
  data inconsistency issues 
  just moves them back to DLT again.
  Would need exception classification 
  in DLT message headers to decide 
  which messages are safe to replay.
  
  Flagged for future sprint if 
  DLT volume increases.
```

---

## What Happened After — The First DLT Message in Production

Three weeks after the DLQ implementation deployed to production, the alert fired for the first time:

```
#expense-ap-alerts:
────────────────────
[Datadog Alert] WARNING
  DLT messages detected in invoice-service
  
  source_topic: payment.completed
  exception_type: PermanentException
  Count: 1
```

You were the first to see it. You investigated using the runbook you had written.

```
Datadog Logs search:
─────────────────────
service:invoice-service 
AND message:*Routing message to DLT*

Found:
──────
"Invoice not found: uuid-inv-deleted"
```

You checked the DB:

```sql
SELECT id, status, deleted_at 
FROM invoices 
WHERE id = 'uuid-inv-deleted';

-- Result: 0 rows
```

The invoice didn't exist. You checked the audit log table:

```sql
SELECT * FROM invoice_audit_logs 
WHERE invoice_id = 'uuid-inv-deleted'
ORDER BY created_at DESC;

-- Result: 
-- 2025-05-08 09:15:22 | CANCELLED 
--                     | by: admin-uuid
```

The invoice had been cancelled by an admin at 9:15am. Payment Service had published a `payment.completed` event for it at 9:47am — 32 minutes after cancellation.

You posted in `#incidents`:

```
You (#incidents):
──────────────────
"DLT alert — not an incident.
 
 One payment.completed event for 
 an invoice that was cancelled 
 32 minutes before the payment 
 was confirmed.
 
 Root cause: invoice cancelled after 
 payment was already in progress 
 in Payment Service.
 
 This is a race condition between 
 our cancellation and their payment.
 
 Our consumer correctly threw 
 PermanentException 
 (invoice not found).
 Message correctly routed to DLT.
 
 No customer harm — the invoice 
 was cancelled, so the payment 
 would be reversed by the bank anyway.
 
 Action taken:
 - Marked DLT message as investigated 
   (no replay needed — invoice cancelled)
 - Will inform Payment Service team 
   of the race condition scenario.
 
 Payment Service should ideally check 
 invoice status before publishing 
 payment.completed — or we should 
 handle 'invoice not found but was 
 recently cancelled' as a known case 
 rather than PermanentException.
 
 Creating ticket for discussion 
 with Payment Service: INV-357"
```

Arjun replied:

```
Arjun:
───────
"Good response. The scenario is valid —
 payment in flight when invoice cancelled.
 
 Your suggestion for the fix is right:
 distinguish between 'invoice not found' 
 (unknown ID — data error) and 
 'invoice cancelled' (expected state —
 payment should be voided).
 
 INV-357 is the right place for 
 the cross-team discussion.
 
 The DLQ working correctly for 
 its first real trigger — 
 caught something that would have 
 been silently discarded before."
```

```
You read Arjun's message:
"The DLQ working correctly for 
its first real trigger."

The mechanism you built 
worked exactly as designed.
A message that would have been 
logged and lost was now 
inspectable, investigated, 
and properly handled.

And the investigation led to 
discovering a cross-service race 
condition that nobody had previously 
documented — a scenario that would 
keep occurring at higher volume.

INV-357 became a cross-team discussion 
with Payment Service.
You attended it.
You explained the scenario.
Payment Service agreed to add 
an invoice status check before 
publishing payment.completed.

That was month 16.
A different story.
But it started here — with a 
DLT message that, before this sprint,
would have been silently discarded.
```

---

## The Result

```
What shipped:
──────────────
DLT implementation for all 3 
invoice-service consumers.

  - DefaultErrorHandler with 
    3-retry exponential backoff
  - PermanentException → DLT immediately
  - Transient failures → retry then DLT
  - 3 DLT topics created 
    (30-day retention)
  - dlt.messages.total Datadog metric
  - Datadog alert for DLT messages
  - Runbook in Confluence

First production DLT message:
  Triggered 3 weeks post-deploy.
  Investigated using runbook.
  Led to discovering cross-service 
  race condition.
  Cross-team fix initiated (INV-357).

What you learned:
──────────────────
1. Spring Kafka's DefaultErrorHandler —
   how to configure retry policy,
   not-retryable exceptions,
   and DLT routing in one place.
   
2. ExponentialBackOffWithMaxRetries —
   why exponential backoff,
   what initialInterval/multiplier 
   /maxInterval mean in practice.

3. addNotRetryableExceptions() —
   how to tell the error handler 
   which exceptions should skip 
   retries entirely.

4. DeadLetterPublishingRecoverer —
   how Spring Kafka routes to DLT,
   what headers it adds automatically,
   why those headers matter for 
   investigation.

5. DLT retention strategy —
   why DLTs need longer retention 
   than source topics.

6. Runbook writing as engineering work —
   the investigation process is only 
   useful if it's documented before 
   the incident happens.
   
7. The difference between a log 
   and a DLT message:
   A log tells you something failed.
   A DLT message preserves what failed,
   where it came from, and what 
   the full context was.
   You can inspect it, investigate it,
   replay it.
   A log you can only read.
```

---

## The "Tricky Question" Preparation

---

**Q1: "Explain how Spring Kafka's DefaultErrorHandler works and how you configured it."**

```
DefaultErrorHandler is Spring Kafka's 
built-in error handling mechanism 
for @KafkaListener methods.

When a listener method throws an exception,
DefaultErrorHandler intercepts it and 
decides what to do based on configuration.

The two key behaviors we configured:

First: retry policy.
We used ExponentialBackOffWithMaxRetries(3).
This means: retry up to 3 times,
with exponential backoff between attempts.
initialInterval = 1 second (first retry wait),
multiplier = 2.0 (each wait doubles),
maxInterval = 10 seconds (cap).
So: attempt 1 immediately, retry at 1s, 2s, 4s.
After 3 failed retries, the recoverer runs.

Second: not-retryable exceptions.
errorHandler.addNotRetryableExceptions(
    PermanentException.class
)
Any PermanentException goes straight 
to the recoverer (DLT) on first failure.
No retries.

The recoverer is a DeadLetterPublishingRecoverer.
When it runs (either after retry exhaustion 
or for a not-retryable exception), it 
publishes the original message to the DLT topic.
DLT topic name = source topic + ".DLT".

Spring Kafka automatically adds headers 
to the DLT message:
  - kafka_dlt-exception-message
  - kafka_dlt-exception-stacktrace
  - kafka_dlt-original-topic
  - kafka_dlt-original-partition
  - kafka_dlt-original-offset

These headers are critical for investigation —
they tell you exactly what failed, why,
and where the original message was.

After routing to DLT, the error handler 
commits the offset for the original message.
The partition is unblocked.
Processing continues with the next message.
```

---

**Q2: "Before the DLQ, you acknowledged permanently-failed messages manually. After, you don't acknowledge them at all. Explain the difference."**

```
Before DLQ:
The consumer's catch block caught 
PermanentException, called 
acknowledgment.acknowledge() explicitly,
and then the message was gone.

No automatic offset commit from 
Spring Kafka — we did it manually.

The message was logged at ERROR 
and effectively discarded.

After DLQ:
The consumer does NOT call acknowledge() 
for permanent failures.
Instead, the exception propagates 
to the DefaultErrorHandler.

The error handler:
1. Routes the message to the DLT 
   (publishes a copy with full headers)
2. Commits the offset for the original topic

The offset commitment happens INSIDE 
the error handler, not in the consumer.
The consumer just throws the exception.

Why is this better?
Before: you acknowledged → offset committed → 
        message gone. Only a log remains.

After: error handler routes to DLT → 
       then commits offset → 
       original message committed but 
       copy preserved in DLT with full context.

The end state is the same for the 
original partition (offset committed, 
partition unblocked). But now you 
have a preserved, inspectable copy 
of what failed — with the original 
message payload and headers plus 
error context headers added by Spring Kafka.

The key insight: you lose nothing 
by using the error handler instead 
of manual acknowledgment — same 
offset behavior — but you gain 
the ability to inspect, investigate, 
and replay the failed message.
```

---

**Q3: "Why did you set DLT retention to 30 days when source topic retention is 7 days?"**

```
Source topic retention exists to 
bound Kafka storage costs while 
giving consumers time to catch up 
after downtime.

7 days is sufficient for our source topics —
if a consumer is down for more than 7 days,
we have bigger problems than a few 
missed events.

DLT retention serves a different purpose.

DLT messages require:
1. Someone to notice the alert.
2. Someone to investigate the root cause.
3. Someone to determine the correct action
   (replay vs discard vs cross-team escalation).
4. Possibly: wait for another team to fix 
   their side before replaying.

This process might take:
  Best case: same day (on-call engineer, 
  clear root cause, straightforward fix).
  
  Realistic: 2-5 days 
  (investigation + cross-team discussion).
  
  Worst case: 2-3 weeks 
  (complex cross-service issue, 
  waiting for other team's sprint).

With 7-day DLT retention:
If investigation takes 8 days, 
the message is gone before you 
can replay it.

With 30-day retention:
Even a 3-week cross-team fix timeline 
leaves the message available for replay.

The cost difference is negligible —
DLT messages are rare by design.
30 days of storage for a handful 
of messages per month is essentially free.

Losing an uninvestigated DLT message 
because it expired before you could 
act on it defeats the entire purpose 
of having a DLT.
```

---

**Q4: "The first DLT message in production revealed a race condition between invoice cancellation and payment completion. How did you handle that?"**

```
The DLT message had PermanentException:
"Invoice not found: uuid-inv-deleted"

I checked the DB — no row for that ID.
I checked the audit log — 
the invoice had been cancelled 32 minutes 
before Payment Service published 
payment.completed.

This was a race condition:
  1. Finance team cancels invoice.
  2. Payment Service had already 
     initiated the payment before the 
     cancellation — payment in flight.
  3. Payment completes, Payment Service 
     publishes payment.completed.
  4. Our consumer tries to mark 
     the invoice PAID — but it's cancelled.
  5. PermanentException → DLT.

Before the DLQ: 
this would have been logged at ERROR 
and discarded. Nobody would have 
noticed unless they specifically 
looked for that error log.

After the DLQ:
the alert fired, I investigated, 
I found the root cause.

There were two decisions:

First — for this specific message:
No replay needed. The invoice was 
legitimately cancelled. The payment 
would be reversed by the bank.
I marked it investigated and discarded.

Second — for the underlying problem:
This scenario will recur at higher volume.
Our consumer was right to throw 
PermanentException for an unknown invoice ID.
But "invoice cancelled" is a different case — 
it's expected, not a data error.

I created INV-357 to discuss with 
Payment Service:
  Option A: Payment Service checks 
    invoice status before publishing 
    payment.completed — don't publish 
    for cancelled invoices.
  Option B: Our consumer distinguishes 
    between "invoice not found" 
    (data inconsistency — PermanentException) 
    and "invoice found but cancelled" 
    (expected state — log and skip).

Payment Service team agreed with Option A 
as the cleaner solution — 
they shouldn't be publishing payment.completed 
for invoices that were cancelled.

The fix was in their next sprint.
We also added Option B as a defensive 
fallback in our consumer in the 
same sprint — defence in depth.

The DLT message caught a cross-service 
design gap that had existed silently.
That's exactly what a DLT is supposed to do.
```

---

Block 5 complete. All three stories done.

```
BLOCK 5 SUMMARY — 
"Feature Ownership" (Months 13-15):
──────────────────────────────────────────

Story 16 (Production Incident Ownership):
  First time you led an incident 
  end-to-end — not observed.
  DB connection pool exhausted by 
  pessimistic lock contention.
  Root cause in 14 minutes.
  Mitigation in 7 more.
  Postmortem written, reviewed, published.
  lock_timeout + LockTimeoutException 
  handler shipped.
  Arjun formally noted your growth 
  in the postmortem.

Story 17 (Backward-Compatible Migration):
  Caught a breaking migration risk 
  in sprint planning before 
  any estimation happened.
  Proposed expand-then-contract.
  Implemented across two sprints.
  847,000 rows migrated without 
  table lock using SKIP LOCKED.
  Zero production incidents.
  "Not just flagging a problem. 
  Driving the solution." — Lukas

Story 18 (DLQ Implementation):
  Designed and shipped Dead Letter 
  Queue for all 3 invoice-service 
  consumers.
  DefaultErrorHandler with retry 
  + DLT routing.
  Runbook for investigation and replay.
  First production DLT message 
  caught a cross-service race 
  condition that would have been 
  silently discarded before.
  Led to cross-team fix in month 16.

The arc across Block 5:
  Story 16: you ran the incident.
  Story 17: you stopped the incident 
            from happening.
  Story 18: you built the system 
            that makes future 
            incidents more visible.

  That progression —
  reactive → preventive → systemic —
  is what "Feature Ownership" means.
  Not just shipping features.
  Making the system more resilient 
  than you found it.
```

