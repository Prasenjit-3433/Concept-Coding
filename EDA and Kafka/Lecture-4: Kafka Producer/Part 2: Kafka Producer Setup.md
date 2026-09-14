# Kafka Producer Setup

Select: Done

# 🎯Step 1: Dependency + Bootstrap Servers

## Why this lecture is different (instructor's framing)

The previous lecture went deep into the 5 internal stages of the producer — Serializer, Partitioner, Record Accumulator, Compression, Sender Thread. That was all *conceptual*. Now the instructor shifts gears: **"we know a bit about producer, let's start our producer setup."** This is the hands-on lecture — actually wiring up a Spring Boot producer against the 2-broker, 2-controller cluster built in the previous lecture (Kafka Cluster Setup).

---

## Step 1: The Dependency

Only **one** dependency is needed to set up a Kafka producer in Spring Boot:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**What this single dependency actually pulls in:**

```
spring-kafka
   │
   ├──► Spring wrapper layer (abstraction)
   │        ├── KafkaTemplate   → used to SEND messages
   │        └── KafkaAdmin      → used to MANAGE topics
   │
   └──► Kafka client dependency (the real implementation)
            (this is what KafkaTemplate/KafkaAdmin use internally)
```

**The mental model to hold onto:** `spring-kafka` = **Spring wrapper layer** + **actual Kafka client library**.

- The **Kafka client** dependency is the real, low-level implementation — this is the same client that any non-Spring Java application would use directly.
- The **Spring wrapper layer** sits on top of that client and gives you clean, developer-friendly abstractions — most importantly `KafkaTemplate` (for sending events) and `KafkaAdmin` (for managing topics). As a developer working with a Kafka producer, **this wrapper layer is mostly what you'll interact with day to day** — you rarely touch the raw Kafka client API directly.

```
┌─────────────────────────────────────────────┐
│         Your Application Code               │
│                                             │
│   kafkaTemplate.send(topic, key, value)     │  ← you interact with THIS layer
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│      Spring Wrapper Layer (abstraction)     │
│      KafkaTemplate, KafkaAdmin              │
└─────────────────┬───────────────────────────┘
                  │  (internally delegates to)
                  ▼
┌─────────────────────────────────────────────┐
│         Kafka Client (implementation)       │
└─────────────────────────────────────────────┘
```

---

## Step 2: Add Bootstrap Brokers

![image.png](Kafka%20Producer%20Setup/image.png)

Once the dependency is in place, the **only mandatory** configuration is the bootstrap servers:

```
server.port=8081
spring.application.name=kafka-producer-service

# Both brokers: if one is down, producer connects to the other
spring.kafka.bootstrap-servers=localhost:9092,localhost:9192
```

This maps directly back to the 2-broker cluster from the previous lecture — `localhost:9092` (Broker1) and `localhost:9192` (Broker2), the client-facing ports each broker opened via its `listeners` config.

### Why it's called "bootstrap"

This is the key conceptual point the instructor stresses. When the producer starts up **for the very first time**, it has **zero knowledge** of the cluster:

- It doesn't know which broker is the leader for Partition 0 of a given topic.
- It doesn't know which broker is the leader for Partition 1.
- It doesn't even know how to reach the active Controller directly (recall from the architecture series: **clients never talk to the Controller directly** — always through a broker).

So the producer needs **one starting point** — any address it can reach to ask, *"hey, give me the metadata."* That's exactly what `bootstrap-servers` provides: not a permanent dependency, just an **entry point** for the very first request.

```
Producer starts up (first time)
        │
        │  Knows NOTHING about the cluster yet
        ▼
Picks ANY address from bootstrap-servers list
        │
        ▼
Connects to that broker: "Give me cluster metadata"
        │
        ├── If it's a topic-creation request → that broker forwards to the Active Controller
        │
        └── If it's a metadata fetch:
                ├── Broker already has cached metadata → responds directly
                └── Broker's metadata is stale → fetches from Controller → relays back
        │
        ▼
Producer receives metadata, CACHES it in memory
```

### After the first call — bootstrap servers become irrelevant (mostly)

Once the producer has this metadata cached in memory, it **doesn't need the bootstrap server anymore** for routine operations:

```
Producer wants to publish to Topic-A, Partition 0
        │
        ▼
Checks its own in-memory metadata cache
        │
        ▼
"Topic-A, Partition 0 → Leader = Broker X, address = ..."
        │
        ▼
Connects DIRECTLY to Broker X — no bootstrap server involved
```

This is exactly why it's *named* "bootstrap" — like a computer's boot sequence, it's only involved in getting things started, not in ongoing operation.

### How often does this cached metadata refresh?

```
spring.kafka.producer.properties.metadata.max.age.ms=300000
```

**Default: 5 minutes (300000 ms).** This tells the producer: *"refresh your cached metadata every 5 minutes, even if nothing seems wrong."*

```
Producer's in-memory metadata
        │
        ▼
Every 5 minutes (default) → re-fetch from any broker → refresh cache
        │
        ▼
ALSO refreshes immediately if: producer has trouble connecting to
the leader it thought was correct (e.g., leader changed due to failure)
```

**Two triggers for a metadata refresh, in other words:**

1. **Scheduled** — the `metadata.max.age.ms` timer expiring (default 5 min)
2. **Reactive** — the producer hits a problem talking to the broker its stale metadata pointed it to (e.g., a leader election happened and that broker is no longer the leader)

This is a direct callback to the architecture series: leader elections happen automatically on failure, and this refresh mechanism is exactly how the producer's own view of the cluster stays consistent with reality.

---

## Recap of Step 1

| Concept | Core takeaway |
| --- | --- |
| **Dependency** | Just `spring-kafka` — pulls in both the Spring wrapper layer (`KafkaTemplate`, `KafkaAdmin`) and the underlying Kafka client library |
| **Spring wrapper vs. Kafka client** | Wrapper = abstraction developers actually use; Kafka client = the real implementation underneath, invoked internally |
| **`bootstrap-servers`** | The only mandatory producer config — a list of broker addresses used purely as an entry point |
| **Why "bootstrap"** | Only needed the *first* time — producer has no cluster knowledge yet, so it asks any broker for metadata, which gets forwarded to the Controller if needed |
| **After first fetch** | Producer caches metadata in memory and connects directly to the correct leader broker going forward — bootstrap servers aren't reused for every send |
| **`metadata.max.age.ms`** (default 5 min) | Scheduled refresh interval for the cached metadata; also refreshes reactively if a connection to a previously-known leader fails |

# 🎯Step 2: Topic Creation via Spring Boot

---

## Why create topics through the application at all?

In the Kafka Cluster Setup lecture, topics were created manually via the CLI script (`kafka-topics.sh --create`). But in a real application, you usually want the producer application itself to guarantee the topic it needs actually exists — without a human running a shell command first. Spring Boot lets you do exactly this: define a topic as a **bean**, and on application startup, Spring checks whether that topic exists in the cluster — if not, it creates it.

---

## The basic version — just a name

![image.png](Kafka%20Producer%20Setup/image%201.png)

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic orderEventsTopic() {

        return TopicBuilder.name("order-events").build();
    }
}
```

**What's happening here:**

- The class is annotated `@Configuration` — a Spring configuration class.
- Inside, a `@Bean` method returns a `NewTopic` object — this is the type Spring expects for topic-creation beans.
- `TopicBuilder` is a **builder** (same builder design pattern you'd see in a low-level design course) — you chain calls and finish with `.build()`.
- Here, only `.name("order-events")` is provided — nothing about partitions, replication factor, retention, etc.

```
Application starts up
        │
        ▼
Spring sees the NewTopic bean: **"order-events"**
        │
        ▼
Checks with the cluster: does "order-events" already exist?
        │
        ├── YES → do nothing
        │
        └── NO  → send a **"create topic"** request
                        │
                        ▼
              Any broker → forwards to Active Controller
                        │
                        ▼
              Controller creates it, using *FALLBACK DEFAULTS*
              for anything not explicitly specified
```

### Where do the fallback defaults come from?

This directly connects back to the **Kafka Cluster Setup** lecture. Recall:

- **`controller1.properties` / `controller2.properties`** had:
    
    ```
    num.partitions=3
    default.replication.factor=2
    ```
    
- **`broker1.properties` / `broker2.properties`** had:
    
    ```
    log.retention.hours=168
    log.segment.bytes=1073741824
    ```
    

So if you only give `TopicBuilder.name(...)` and nothing else, the **partition count and replication factor** come from the active Controller's config, and things like **segment size and cleanup policy** come from the broker's config.

```
TopicBuilder.name("order-events").build()
        │
        ▼
   Partitions?          → not specified → pulled from Controller's num.partitions
   Replication Factor?  → not specified → pulled from Controller's                                                default.replication.factor
   Segment size?        → not specified → pulled from Broker's log.segment.bytes
   Cleanup policy?       → not specified → pulled from Broker's cleanup.policy
```

---

## The full version — overriding every default explicitly

![image.png](Kafka%20Producer%20Setup/image%202.png)

`TopicBuilder` also lets you override any of these defaults directly at topic-creation time:

```java
@Bean
public NewTopic orderEventsTopic() {

    return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(2)

            // if not provided here, will be picked from broker properties
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")

            // if not provided here, will be picked from broker properties
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")

            // if not provided here, will be picked from broker properties
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")

            // if not provided here, will be picked from broker properties
            .config(TopicConfig.SEGMENT_BYTES_CONFIG, "1073741824")

            .build();
}
```

**Property-by-property, what each override means (all callbacks to earlier lectures):**

| Call | Meaning | Where it'd come from if omitted |
| --- | --- | --- |
| `.partitions(3)` | This topic gets 3 partitions | Controller's `num.partitions` |
| `.replicas(2)` | Each partition gets 2 replicas (1 leader + 1 follower) | Controller's `default.replication.factor` |
| `RETENTION_MS_CONFIG = "604800000"` | Keep logs for 7 days (604,800,000 ms = 7 days) — same as `log.retention.hours=168` in ms form | Broker's `log.retention.hours` |
| `CLEANUP_POLICY_CONFIG = "delete"` | Use the `delete` policy (vs. `compact`) — recall from architecture series Part 4 | Broker's `cleanup.policy` |
| `MIN_IN_SYNC_REPLICAS_CONFIG = "2"` | Minimum ISR size required for an `ack=all` write to succeed — recall from architecture series Part 3 | Broker's `min.insync.replicas` |
| `SEGMENT_BYTES_CONFIG = "1073741824"` | 1 GB max size per segment file | Broker's `log.segment.bytes` |

**Key takeaway:** whether you give just a name, or spell out every config, both are valid — it just depends on whether the sensible cluster-wide defaults are good enough for this specific topic, or whether this topic has special requirements (e.g., needs stronger durability via a higher `min.insync.replicas`, or a shorter retention window).

```
Two valid approaches:
   1. TopicBuilder.name("order-events").build()
        → everything falls back to Controller/Broker defaults

   2. TopicBuilder.name("order-events")
        .partitions(...).replicas(...).config(...)...build()
        → fully explicit, overrides everything
```

---

## Live Demo — proving it actually works

The instructor walks through this end-to-end on a **fresh cluster** to prove the auto-creation behavior:

**Setup:**

```java
@Bean
public NewTopic orderEventsTopic() {
    return TopicBuilder.name("order-events")
            .partitions(4)
            .replicas(2)
            // ... rest of the config kept the same
            .build();
}
```

**Steps taken:**

```
1. Wipe everything:
   rm -rf /tmp/broker1-logs 
   rm -rf /tmp/broker2-logs 
   rm -rf /tmp/controller1-logs 
   rm -rf /tmp/controller2-logs

2. Generate a NEW cluster ID
   bin/kafka-storage.sh random-uuid

3. Map all 4 nodes with the new cluster ID
   (controller1, controller2, broker1, broker2)

4. Start Controller1, then Controller2
   → both controllers up

5. Start Broker1, then Broker2
   → both brokers up

6. Check topic status BEFORE starting the Spring Boot app:
   "order-events" → DOES NOT EXIST

7. Start the Spring Boot producer application
   → Spring sees the NewTopic bean on startup
   → Checks cluster: "order-events" not found
   → Sends create-topic request

8. Check topic status AFTER the app starts:
   describe --topic order-events
        → Partition 0, 1, 2, 3   (4 partitions **✓**)
        → Replicas: 2 per partition  (2 replicas **✓**)
```

![image.png](Kafka%20Producer%20Setup/image%203.png)

**Result:** exactly matches what was configured in the bean — **4 partitions, 2 replicas** — confirming the topic was created automatically by the Spring Boot application itself, using nothing more than the `NewTopic` bean. No manual CLI `kafka-topics.sh --create` was needed this time.

```
┌──────────────────────────────────────────────────────────────┐
│              Before starting Producer App                    │
│                                                              │
│   kafka-topics.sh --describe --topic order-events            │
│        → Topic does not exist                                │
└──────────────────────────────────────────────────────────────┘

                        │
                        │  Spring Boot app starts
                        │  NewTopic bean triggers create-topic request
                        ▼

┌──────────────────────────────────────────────────────────────┐
│               After starting Producer App                    │
│                                                              │
│   kafka-topics.sh --describe --topic order-events            │
│        → PartitionCount: 4, ReplicationFactor: 2             │
└──────────────────────────────────────────────────────────────┘
```

**Why this works with just the one dependency:** `spring-kafka` internally brings in **`KafkaAdmin`**, which is exactly the component responsible for this kind of topic management — creating, checking existence, etc. This is the same `KafkaAdmin` mentioned back in Step 1's dependency breakdown.

---

## Recap of Step 2

| Concept | Core takeaway |
| --- | --- |
| **`NewTopic` bean** | A `@Bean` returning `TopicBuilder...build()` inside a `@Configuration` class — Spring checks on startup whether this topic exists, and creates it if not |
| **Minimal version** | `TopicBuilder.name("topic-name").build()` — partitions, replication factor, retention, cleanup policy, segment size all fall back to Controller/Broker defaults |
| **Full override version** | `.partitions()`, `.replicas()`, and `.config(TopicConfig.X, value)` calls let you explicitly set any of these per-topic, overriding cluster defaults |
| **Where defaults come from** | Partitions/replication factor ← Controller's `num.partitions`/`default.replication.factor`; retention/cleanup/segment size ← Broker's equivalent properties |
| **Live demo confirmation** | Topic didn't exist before the app started; starting the app (with the `NewTopic` bean) created it automatically with exactly the configured partition count and replication factor |
| **Enabling dependency** | `spring-kafka` brings in `KafkaAdmin` internally — this is what actually performs the topic-existence check and creation |

# 🎯Step 3: Sending Events (built-in serializer)

### Setting up the requirement

Now that the topic exists, the next step is actually **publishing events** to it. The instructor frames this as a deliberate exercise: since we now understand all 5 internal producer stages (from the previous lecture), we can configure **every single one of them** explicitly — even though, in practice, most of the time you'd just rely on sensible defaults with only `bootstrap-servers` set.

**Use Case 1's requirement, stage by stage:**

```
┌──────────────────────────────────────────────────────────────┐
│  SERIALIZER:                                                 │
│     Key   → String                                           │
│     Value → JSON                                             │
│                                                              │
│  PARTITIONER:                                                │
│     Send WITH key → related events land in same partition    │
│                                                              │
│  RECORD ACCUMULATOR:                                         │
│     Max batch size    = 32 KB                                │
│     Linger             = 20 ms                               │
│     Overall buffer     = 32 MB                               │
│     If buffer full → send() blocks up to 60 sec,             │
│                        then throws an exception              │
│                                                              │
│  COMPRESSION:                                                │
│     Use Snappy                                               │
│                                                              │
│  SENDER THREAD:                                              │
│     In-flight requests per connection = 5                    │
│     Retries on transient failure       = 3                   │
└──────────────────────────────────────────────────────────────┘
```

This maps 1-to-1 onto the 5 stages from the internals lecture — the instructor is deliberately using this use case to show that every stage *can* be tuned, once you understand what each config actually controls.

---

### The Code — top to bottom

#### `OrderController.java` — just a pass-through

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    OrderProducerService orderProducerService;

    @PostMapping("/with-key")
    public ResponseEntity<String> sendWithKey(@RequestBody Order order) {
        orderProducerService.sendWithKey(order);
        return ResponseEntity.accepted().body("Order created and event published!");
    }
}
```

**Nothing special here** — the controller's only job is to receive the HTTP request and hand it straight off to the service layer. All the actual Kafka logic lives in the service.

```
HTTP POST /api/orders/with-key
        │
        ▼
   OrderController.sendWithKey()
        │
        ▼
   OrderProducerService.sendWithKey(order)   ← all real work happens here
```

---

#### `Order.java` — the POJO

```java
public class Order {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double totalAmount;
    private String status;

    // getters and setters
}
```

This is the Java object that ultimately becomes the **event value** — it's what gets serialized to JSON in this use case.

---

#### `OrderProducerService.java` — where the actual send happens

```java
@Service
public class OrderProducerService {

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendWithKey(Order order) {
        String key = order.getOrderId();

        CompletableFuture<SendResult<String, Order>> future =
                kafkaTemplate.send("order-events", key, order);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Order event sent successfully");
                System.out.println("Partition: " + result.getRecordMetadata().partition());
                System.out.println("Offset: " + result.getRecordMetadata().offset());
            } else {
                System.out.println("Failed to send order event: " + ex.getMessage());
            }
        });
    }
}
```

**Walking through this line by line:**

1. **`@Autowired private KafkaTemplate<String, Order> kafkaTemplate;`** — Spring Boot auto-resolves this. Since `spring-kafka` is on the classpath, Spring automatically creates and wires a `KafkaTemplate` bean. `String` is the key type, `Order` is the value type.
2. **`String key = order.getOrderId();`** — the **order ID** is chosen as the key. This is deliberate: recall from the architecture series, using an ID like `orderId` as the key guarantees all events for that specific order land in the same partition and get read in the exact order they were written.
3. **`kafkaTemplate.send("order-events", key, order)`** — this is the *only* line that actually talks to Kafka. Everything else is bookkeeping. This is the exact call that internally kicks off the full 5-stage journey (Serializer → Partitioner → Record Accumulator → Compression → Sender Thread) from the previous lecture.
4. **`CompletableFuture<SendResult<String, Order>>`** — recall from the internals lecture: the application thread's job ends once the event is handed to the Record Accumulator. `.send()` immediately returns this `Future` and the application thread moves on — it does **not** block waiting for the network call.
5. **`future.whenComplete((result, ex) -> {...})`** — this callback runs **later**, whenever the background **sender thread** actually completes the send (success or failure). Standard Java `CompletableFuture` callback pattern.
    - **On success (`ex == null`):** `result.getRecordMetadata()` gives you the **partition** the event landed in, and the **offset** it was assigned — both are the real, concrete outcomes of the partitioning and offset-assignment steps we studied in the architecture series.
    - **On failure:** the exception message is printed.

```
Application Thread                          Sender Thread (background)
     │                                               │
     │ kafkaTemplate.send(topic, key, order)         │
     │   → Serialize                                 │
     │   → Partition                                 │
     │   → Hand off to Record Accumulator            │
     │                                               │
     │ returns Future immediately, thread is FREE    │
     │                                               │
     │  ... (does other work) ...                    │
     │                                               │
     │                                    checks Record Accumulator
     │                                    for ready batches, sends
     │                                    over network, gets response
     │                                               │
     │◄──── future.whenComplete() invoked ───────────┘
     │       (partition + offset, or exception)
```

---

#### `application.properties` — configuring all 5 stages

```
server.port=8081
spring.application.name=kafka-producer-service

# Both brokers listed for redundancy, if one is down, producer connects to the other
spring.kafka.bootstrap-servers=localhost:9092,localhost:9192

# Metadata refresh interval: how often producer re-fetches cluster info (default 5 min)
spring.kafka.producer.properties.metadata.max.age.ms=300000

# Asking broker to reply only when all ISR successfully get the events
spring.kafka.producer.acks=all

#---------------**SERIALIZER**-------------
# Key needs to be String
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer

# Value: JsonSerializer, auto-converts Java objects to JSON bytes
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

#---------------**RECORD ACCUMULATOR**-------
# Batching, wait 20ms to collect more messages before sending
spring.kafka.producer.properties.linger.ms=20

# Max batch size, send when batch reaches 32KB (even if linger.ms hasn't expired)
spring.kafka.producer.properties.batch.size=32768

# Total buffer memory for all batches: 32MB (if full, send() will get blocked till space created or max block time)
spring.kafka.producer.properties.buffer.memory=33554432

# Max time send() blocks if buffer is full (default 60s), after this time, send() call will fail immediately
spring.kafka.producer.properties.max.block.ms=60000

#---------------**COMPRESSION**-------
# compresses entire batch before sending
spring.kafka.producer.properties.compression.type=snappy

#---------------**SENDER THREAD**-------
# Retry on transient failures (network timeout, leader change, etc.)
spring.kafka.producer.retries=3

# Max in-flight requests
spring.kafka.producer.properties.max.in.flight.requests.per.connection=5
```

**Property-by-property breakdown, grouped by which of the 5 stages they belong to:**

#### Not tied to a specific stage

| Property | Purpose |
| --- | --- |
| `spring.kafka.bootstrap-servers` | Entry-point broker addresses — covered fully in Step 1 |
| `spring.kafka.producer.properties.metadata.max.age.ms=300000` | Metadata cache refresh interval — 5 minutes, matches the default (Step 1) |
| `spring.kafka.producer.acks=all` | Recall from the architecture series' ack levels: `all` means the leader waits for **every replica currently in the ISR** to acknowledge before confirming success to the producer — the strongest durability guarantee, at the cost of latency |

#### Stage 1 — Serializer

| Property | Purpose |
| --- | --- |
| `key-serializer=...StringSerializer` | The order ID (key) is a plain string |
| `value-serializer=...JsonSerializer` | The `Order` POJO gets converted to JSON bytes automatically |

#### Stage 2 — Partitioner

This use case doesn't need a dedicated property — the **act of calling `.send(topic, key, value)`** (i.e., providing a key) is what triggers **Case 2** from the internals lecture: `partition = hash(keyBytes) % numPartitions`. Same key → same partition, every time.

#### Stage 3 — Record Accumulator

| Property | Purpose |
| --- | --- |
| `linger.ms=20` | Producer waits **at most 20 ms** for more events to arrive before closing an unfilled batch |
| `batch.size=32768` (32 KB) | A batch closes once it reaches 32 KB, regardless of `linger.ms` |
| `buffer.memory=33554432` (32 MB) | Total memory ceiling across **all** batches for **all** partitions combined |
| `max.block.ms=60000` (60 sec) | If the 32 MB buffer is completely full, `.send()` blocks for up to 60 seconds waiting for space to free up — if nothing frees up in that window, the send call **fails immediately** with an exception |

```
send() call arrives, buffer is FULL (32MB already used)
        │
        ▼
send() BLOCKS, waiting for space
        │
        ▼
   Space freed within 60s?
        │
   ┌────┴──────┐
   YES         NO
   │           │
   ▼           ▼
Proceeds    Throws exception
normally    immediately after 60s
```

#### Stage 4 — Compression

| Property | Purpose |
| --- | --- |
| `compression.type=snappy` | Every closed, ready-to-send batch gets compressed with Snappy before it leaves the producer — saving both network bandwidth and, since Kafka stores compressed batches as-is, disk space on the broker |

#### Stage 5 — Sender Thread

| Property | Purpose |
| --- | --- |
| `spring.kafka.producer.retries=3` | On a transient failure (network blip, leader change mid-flight, etc.), the sender thread automatically retries up to 3 times |
| `max.in.flight.requests.per.connection=5` | Up to 5 unacknowledged requests can be outstanding at once, per TCP connection to a broker |

**Important flag, carried over from the internals lecture:** this exact combination — `max.in.flight.requests.per.connection = 5` (> 1) **and** `retries = 3` (> 0) — is precisely the condition that creates the **message reordering risk**. This use case's config deliberately hits that risk condition; it'll come back at the end of this lecture.

---

### Recap of Step 3

| Stage | Config used | What it does |
| --- | --- | --- |
| **(General)** | `bootstrap-servers`, `metadata.max.age.ms`, `acks=all` | Entry point, metadata refresh, and strongest durability guarantee (wait for full ISR) |
| **Serializer** | `key-serializer=StringSerializer`, `value-serializer=JsonSerializer` | Key as plain string, value (POJO) auto-converted to JSON bytes |
| **Partitioner** | Send with key (`orderId`) | Same key → same partition → guaranteed relative ordering for that order's events |
| **Record Accumulator** | `linger.ms=20`, `batch.size=32768`, `buffer.memory=33554432`, `max.block.ms=60000` | Controls batch fill time/size, total buffer ceiling, and blocking behavior when full |
| **Compression** | `compression.type=snappy` | Compresses batches before sending — saves network + disk |
| **Sender Thread** | `retries=3`, `max.in.flight.requests.per.connection=5` | Auto-retries transient failures; allows 5 concurrent unacked requests per broker connection — but this combo risks message reordering |
| **Code structure** | Controller → Service → `KafkaTemplate.send()` → `CompletableFuture` callback | Controller only forwards the request; service does the actual send and handles partition/offset/failure via the callback |

---

# 🎯Step 4: Sending Events (Custom Serializer + Sticky Partitioning)

### Setting up the requirement

Use Case 2 keeps almost everything the same as Use Case 1, but changes two things specifically — to demonstrate two concepts that were only explained *conceptually* in the internals lecture:

```
┌──────────────────────────────────────────────────────────┐
│  SERIALIZER:                                             │
│     Key   → String  (unchanged)                          │
│     Value → CUSTOM SERIALIZER  (changed)                 │
│                                                          │
│  PARTITIONER:                                            │
│     Send WITHOUT key → triggers STICKY PARTITIONING      │
│                                                          │
│  Rest all same as Use Case 1                             │
│  (Record Accumulator, Compression, Sender Thread         │
│   configs are unchanged)                                 │
└──────────────────────────────────────────────────────────┘
```

---

### The Controller — a new endpoint, same pattern

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    OrderProducerService orderProducerService;

    @PostMapping("/with-no-key")
    public ResponseEntity<String> sendWithoutKey(@RequestBody Order order) {
        orderProducerService.sendWithoutKey(order);
        return ResponseEntity.accepted().body("Order created and event published!");
    }
}
```

Nothing new here — same pass-through pattern as `sendWithKey`, just a different endpoint (`/with-no-key`) invoking a different service method.

---

### Why a custom serializer is needed

Recall the `Order` POJO carries a lot of information:

```java
public class Order {
    private String orderId;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double totalAmount;
    private String status;
    // getters and setters
}
```

**The requirement:** hide sensitive fields before the event goes out. We only want `orderId` and `productId` to actually be published — **not** `customerId`, `quantity`, `totalAmount`, or `status`. This is exactly the "full control" use case flagged back in the internals lecture — a custom serializer lets you decide *precisely* what bytes leave your application, instead of blindly serializing the whole object.

#### `OrderSummarySerializer.java` — the custom serializer

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderSummarySerializer implements Serializer<Order> {

    private final ObjectMapper objectMapper;

    public OrderSummarySerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(String topic, Order order) {
        if (order == null) {
            return null;
        }

        try {
            // Build a map with only the fields we want to expose, rest I removed it
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("orderId", order.getOrderId());
            summary.put("productId", order.getProductId());

            return objectMapper.writeValueAsBytes(summary);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Order summary", e);
        }
    }
}
```

**Walking through this:**

1. **`implements Serializer<Order>`** — recall from the internals lecture: every serializer, built-in or custom, implements this same interface:
    
    ```java
    public interface Serializer<T> {
        byte[] serialize(String topic, T data);
    }
    ```
    
    Here, `T` is `Order`.
    
2. **`ObjectMapper`** — a plain Jackson object, used to do the actual object-to-bytes conversion.
3. **Inside `serialize()`:**
    - Null-check first — if the order is null, return null immediately.
    - Build a **new `Map<String, Object>`** containing *only* `orderId` and `productId` — deliberately leaving out `customerId`, `quantity`, `totalAmount`, `status`.
    - Convert **that map** (not the original `Order` object) to bytes via `objectMapper.writeValueAsBytes(summary)`.

```
┌─────────────────────────────────────────────────────────────┐
│                  Order object (full)                        │
│   orderId, customerId, productId, quantity,                 │
│   totalAmount, status                                       │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼  OrderSummarySerializer.serialize()
┌─────────────────────────────────────────────────────────────┐
│         Map<String,Object> summary (filtered)               │
│              orderId, productId  ONLY                       │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼  objectMapper.writeValueAsBytes(summary)
┌───────────────────────────────────────────────────────────┐
│                    byte[]  (sent to Kafka)                │
└───────────────────────────────────────────────────────────┘
```

**Key takeaway:** this is a hard gate at the serialization stage itself — `customerId`, `quantity`, `totalAmount`, and `status` **never leave the application** at all. They're stripped before conversion to bytes, not filtered out afterward.

---

### `OrderProducerService.java` — sending without a key

```java
@Service
public class OrderProducerService {

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendWithoutKey(Order order) {
        CompletableFuture<SendResult<String, Order>> future =
                kafkaTemplate.send("order-events", order);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Order event sent successfully");
                System.out.println("Partition: " + result.getRecordMetadata().partition());
                System.out.println("Offset: " + result.getRecordMetadata().offset());
            } else {
                System.out.println("Failed to send order event: " + ex.getMessage());
            }
        });
    }
}
```

**The one key difference from Use Case 1:** `kafkaTemplate.send("order-events", order)` — **no key argument**. This is exactly **Case 3** from the internals lecture's Partitioner discussion — no key means there's nothing to hash, so the partitioner falls back to its no-key strategy.

**Since this is Kafka ≥ 2.4, that strategy is Sticky Partitioning** (not the older Round Robin):

```
send("order-events", order)   ← no key!
        │
        ▼
Partitioner checks: "Do I have a sticky partition remembered for order-events?"
        │
        ├── First event → No → randomly picks one partition, remembers it
        │
        └── Subsequent events → Yes → keeps routing to that SAME partition,
                                        until the current batch closes
```

This directly reuses everything explained in the internals lecture: sticky partitioning maximizes batch fullness by routing all no-key events for a topic to one randomly-chosen partition, only re-randomizing once that batch closes and a new one opens.

---

### `application.properties` — only the value serializer changes

```
#---------------**SERIALIZER**-------------
# Key needs to be String
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer

# Value: custom serializer
spring.kafka.producer.value-serializer=com.eda.producer.serializer.OrderSummarySerializer
```

Everything else in `application.properties` — `bootstrap-servers`, `acks`, `linger.ms`, `batch.size`, `buffer.memory`, `max.block.ms`, `compression.type`, `retries`, `max.in.flight.requests.per.connection` — is **kept identical** to Use Case 1. Only the `value-serializer` line changes, pointing to the new custom class instead of `JsonSerializer`.

---

### Live Debug-Mode Walkthrough

The instructor confirms this actually works by running the app in **debug mode** with a breakpoint set inside `OrderSummarySerializer.serialize()`:

```
1. Set breakpoint inside OrderSummarySerializer.serialize()

2. Start the Spring Boot app in DEBUG mode

3. Hit the endpoint: POST /api/orders/with-no-key
       Body: { orderId: "O-1111", productId: "PID-3", ... }

4. Execution pauses at the breakpoint
       → Confirms: KafkaProducer's value-serializer.serialize()
          call is invoking OUR custom serializer
          (not the default JsonSerializer)

5. Resume execution → console output:
       "Order event sent successfully"
       Partition: 2
       Offset: 0
```

```
Producer send() call
        │
        ▼
KafkaProducer internals: value serializer.serialize()
        │
        ▼
   Invokes OrderSummarySerializer  ← confirmed via breakpoint
        │
        ▼
   Returns filtered byte[] (orderId + productId only)
```

**Checking the actual data on disk:** the instructor then navigates to the broker holding the leader for that partition — Partition 2's leader was Broker2 (node ID 4) — and opens the raw segment log file for `order-events-2`.

```
Partition 2 → Leader = Broker 2 (node ID 4)
        │
        ▼
Navigate to Broker2's log.dirs → order-events-2/000000.log
        │
        ▼
Raw content shows: orderId = "O-1111", productId = "PID-3"
                    (customerId, quantity, totalAmount, status → ALL absent)
```

**Result confirmed:** the sensitive fields never made it into the stored event at all — exactly what the custom serializer was designed to guarantee.

---

### Recap of Step 4

| Concept | Core takeaway |
| --- | --- |
| **Why a custom serializer** | To strip sensitive fields (`customerId`, `quantity`, `totalAmount`, `status`) before the object is ever converted to bytes — a hard gate, not a post-hoc filter |
| **`OrderSummarySerializer`** | Implements `Serializer<Order>`; builds a filtered `Map` containing only `orderId` + `productId`, then converts *that* map to bytes via Jackson's `ObjectMapper` |
| **Sending without a key** | `kafkaTemplate.send(topic, order)` — no key means Case 3 from the internals lecture: the partitioner falls back to its no-key strategy |
| **Sticky Partitioning in action** | Since this is Kafka ≥ 2.4, the partitioner randomly picks one partition per topic and sticks with it for all no-key events, until the current batch closes |
| **Config change** | Only `value-serializer` in `application.properties` changes — everything else (accumulator, compression, sender settings) stays identical to Use Case 1 |
| **Debug confirmation** | Breakpoint inside the custom serializer confirms it — not the default `JsonSerializer` — is what actually gets invoked; inspecting the raw log file on the leader broker confirms only `orderId` + `productId` were persisted |

# 🎯Step 5: Handling Multiple `Serializer`

---

## The problem this section solves

So far, `application.properties` has defined **one** value-serializer at a time — first `JsonSerializer`, then `OrderSummarySerializer`. But a real application often has **more than one kind of producer** with different serialization needs:

```
Order Controller     → needs JsonSerializer (or the default POJO conversion)
Sales Controller      → needs a completely different (custom) serializer
Some other producer  → needs yet another serializer
```

**The problem:** `application.properties` is a single, flat file. You can't write two different values for `spring.kafka.producer.value-serializer` and expect Spring to somehow know which one applies to which controller. So how do you support **multiple, differently-configured producers** within the same Spring Boot application?

**The answer: define multiple `KafkaTemplate` beans explicitly, each with its own configuration, and pick between them using `@Qualifier`.**

---

## Building a second, custom `KafkaTemplate` bean

Back in the `@Configuration` class (the same one that had the `NewTopic` bean), a new bean is added:

```java
@Bean
public KafkaTemplate<String, Order> customSerializerKafkaTemplate(KafkaProperties kafkaProperties) {

    // Start with all producer properties from application.properties
    Map<String, Object> props =
            new HashMap<>(kafkaProperties.buildProducerProperties(null));

    // Override ONLY the value serializer - everything else stays the same
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderSummarySerializer.class);

    DefaultKafkaProducerFactory<String, Order> factory =
            new DefaultKafkaProducerFactory<>(props);

    return new KafkaTemplate<>(factory);
}
```

**Walking through this, step by step:**

1. **`KafkaProperties kafkaProperties` (injected parameter)** — this is Spring Boot's own representation of everything currently sitting in `application.properties` under `spring.kafka.*`.
2. **`kafkaProperties.buildProducerProperties(null)`** — this loads **all** the producer-related properties already defined in `application.properties` (bootstrap servers, acks, linger.ms, batch.size, buffer.memory, compression.type, retries, max.in.flight.requests.per.connection — everything) into a `Map`. Nothing is thrown away; this is the complete existing configuration as a starting point.
3. **`props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderSummarySerializer.class)`** — this is the **only override**. `ProducerConfig` is an enum-like class with many constants — one for every possible producer config key — and here we're specifically overriding just the value-serializer key, pointing it at `OrderSummarySerializer` instead of whatever `application.properties` has set as the default.
4. **`DefaultKafkaProducerFactory<>(props)`** — recall that `KafkaTemplate` needs a `ProducerFactory` to be constructed (this is visible if you look inside `KafkaTemplate`'s own constructor). `DefaultKafkaProducerFactory` is the concrete implementation of that `ProducerFactory` interface, and it's built using our modified `props` map.
5. **`return new KafkaTemplate<>(factory);`** — finally, a brand-new `KafkaTemplate` is constructed from this custom factory, and returned as a Spring bean.

```
application.properties
        │
        ▼
kafkaProperties.buildProducerProperties(null)
        │
        ▼
   Map<String, Object> props   (everything from application.properties)
        │
        ▼
   OVERRIDE: props.put(VALUE_SERIALIZER_CLASS_CONFIG, OrderSummarySerializer.class)
        │
        ▼
   DefaultKafkaProducerFactory<>(props)
        │
        ▼
   new KafkaTemplate<>(factory)   ← this is a SECOND, distinctly-configured bean
```

**Meanwhile, `application.properties` itself stays on the default:**

```
#---------------**SERIALIZER**-------------
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer

# Value: JsonSerializer, auto-converts Java objects to JSON bytes
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

So now, **two `KafkaTemplate` beans exist side by side**:

```
Default KafkaTemplate           customSerializerKafkaTemplate
   (auto-created by Spring          (manually defined bean,
    from application.properties)     overrides ONLY value-serializer)
        │                                    │
        ▼                                    ▼
  value-serializer =                  value-serializer =
    JsonSerializer                      OrderSummarySerializer
```

---

## Using the right bean via `@Qualifier`

Now, in the service class, both beans can be autowired side by side — Spring needs `@Qualifier` to know which one you mean, since there's more than one `KafkaTemplate<String, Order>` bean available:

```java
@Service
public class OrderProducerService {

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    @Autowired
    @Qualifier("customSerializerKafkaTemplate")
    private KafkaTemplate<String, Order> customSerializerKafkaTemplate;

    public void sendWithoutKey(Order order) {
        CompletableFuture<SendResult<String, Order>> future =
                customSerializerKafkaTemplate.send("order-events", order);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Order event sent successfully");
                System.out.println("Partition: " + result.getRecordMetadata().partition());
                System.out.println("Offset: " + result.getRecordMetadata().offset());
            } else {
                System.out.println("Failed to send order event: " + ex.getMessage());
            }
        });
    }
}
```

**What's happening:**

- `kafkaTemplate` — the plain, default-autowired bean (uses whatever `application.properties` specifies — `JsonSerializer` in this case).
- `customSerializerKafkaTemplate` — explicitly wired using `@Qualifier("customSerializerKafkaTemplate")`, matching the **bean method name** from the configuration class.
- The `sendWithoutKey` method here deliberately uses `customSerializerKafkaTemplate.send(...)` — so this specific flow goes through `OrderSummarySerializer`, while some *other* service method could just as easily use the plain `kafkaTemplate` and get `JsonSerializer` instead.

```
┌─────────────────────────────────────────────────────────────────┐
│                  OrderProducerService                           │
│                                                                 │
│   kafkaTemplate  ──────► uses application.properties            │
│                            (JsonSerializer)                     │
│                                                                 │
│   customSerializerKafkaTemplate ──► uses OrderSummarySerializer │
│                            (overridden in the bean itself)      │
└─────────────────────────────────────────────────────────────────┘
```

**Why this pattern matters:** it means a single Spring Boot application can host **any number of differently-configured producers** — different serializers, potentially even different `acks` levels or retry settings per use case — all coexisting cleanly, by defining one bean per configuration and picking between them with `@Qualifier`. `application.properties` supplies the sensible shared baseline; individual beans override only what's actually different for their specific need.

---

## Closing — the message reordering risk, restated

The lecture ends by circling back to a risk flagged at the very end of the *internals* lecture. Both use cases in this setup lecture used:

```
spring.kafka.producer.retries=3
spring.kafka.producer.properties.max.in.flight.requests.per.connection=5
```

Recall the exact condition for risk:

```
max.in.flight.requests.per.connection > 1        (5 > 1 ✓)
                    AND
retries > 0                                        (3 > 0 ✓)
        │
        ▼
   RISK OF MESSAGE REORDERING
```

**The scenario, once more:**

```
Request A → sent, in-flight
Request B → sent, in-flight
        │
        ▼
Request A → FAILS (transient error)
Request B → SUCCEEDS (written to the partition's log first)
        │
        ▼
Request A → retries... SUCCEEDS (but now written AFTER Request B)
```

Even though Request A was sent *first*, it ends up persisted **after** Request B — silently violating the within-partition ordering guarantee that the entire architecture series stressed as one of Kafka's core promises.

**The instructor's closing question, left open on purpose:** *"How to resolve this risk?"*

This is deliberately **not answered here** — it's deferred to the next, dedicated lecture: **Kafka Producer Idempotency Internals**, where `enable.idempotence=true` will be covered in full.

---

## Recap of Step 5

| Concept | Core takeaway |
| --- | --- |
| **The problem** | `application.properties` only allows one global producer config — insufficient when different producers in the same app need different serializers (or other settings) |
| **The fix** | Define additional `KafkaTemplate` beans manually — start from `kafkaProperties.buildProducerProperties(null)` (everything from `application.properties`), then override only what's different via `props.put(...)` |
| **`DefaultKafkaProducerFactory`** | The concrete `ProducerFactory` implementation needed to construct a custom `KafkaTemplate` |
| **`@Qualifier`** | Used to disambiguate between multiple `KafkaTemplate<String, Order>` beans when autowiring — matches the bean method's name |
| **Result** | Multiple, independently-configured producers can coexist in one Spring Boot app — each service method picks whichever `KafkaTemplate` fits its need |
| **Closing risk recap** | `max.in.flight.requests.per.connection=5` + `retries=3` → message reordering risk, same as flagged in the internals lecture |
| **What's deferred** | The actual fix — `enable.idempotence=true` — is pushed to the next lecture, **Kafka Producer Idempotency Internals** |

---

## End of "Kafka Producer Setup" — Full Summary

| Step | Core Idea |
| --- | --- |
| **Step 1** | `spring-kafka` dependency (wrapper + client); `bootstrap-servers` as a one-time entry point; metadata caching + `metadata.max.age.ms` refresh |
| **Step 2** | `NewTopic` bean via `TopicBuilder` — minimal (defaults from Controller/Broker) vs. fully explicit overrides; live demo confirming auto-creation on startup |
| **Step 3** | Use Case 1 — full Controller → Service → `KafkaTemplate.send(topic, key, value)` flow, with all 5 producer stages configured explicitly in `application.properties` |
| **Step 4** | Use Case 2 — custom serializer (`OrderSummarySerializer`) to strip sensitive fields; sending without a key to trigger sticky partitioning; debug-mode + raw log confirmation |
| **Step 5** | Multiple `KafkaTemplate` beans via `@Qualifier` for multi-serializer needs; closing re-statement of the message reordering risk, deferred to the idempotency lecture |

**Interview-ready one-liners from this lecture:**

- *"`bootstrap-servers` is only needed for the producer's very first metadata fetch — after that, it caches the metadata and talks directly to the right leader broker."*
- *"A topic can be auto-created on application startup via a `NewTopic` bean — anything not explicitly configured falls back to the Controller's or Broker's defaults."*
- *"A custom serializer is a hard gate — sensitive fields can be stripped before the object is ever converted to bytes, so they never leave the application at all."*
- *"Sending without a key doesn't mean random distribution — since Kafka 2.4, sticky partitioning routes all no-key events to one partition per batch, to maximize batch fullness."*
- *"When one `application.properties` isn't enough for multiple producers with different needs, define additional `KafkaTemplate` beans that override just the differing config, and pick between them with `@Qualifier`."*

---