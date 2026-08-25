# Step 1: How Kafka Producer Works Internally

Select: In-Progress

### ☠️The illusion of simplicity (instructor's framing)

Setting up a Kafka producer in Spring Boot looks deceptively simple:

```java
kafkaTemplate.send(topic, key, value);
```

One line, and the event is published. Out of all the producer configurations that exist, only **one is mandatory** — which broker(s) the producer should talk to (`bootstrap-servers`). Everything else — serializer choice, batching, compression, retries — is optional, with sensible defaults kicking in if you don't set them.

But the instructor is explicit: this lecture is **not** about staying at that surface level. The goal here is to go one level deeper and understand *exactly* what happens inside the producer between the moment you call `.send()` and the moment a network call actually leaves your application.

---

### The core idea: `.send()` doesn't immediately hit the network

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image.png)

**Key mental model shift:** when a producer calls `.send()`, the message does **not** immediately go to the broker. Instead, it passes through **multiple internal stages inside the producer itself** — all of this happens before any network call is made.

```
┌──────────┐                                ┌──────────────┐
│ Producer │ ──────── send() ───────────►   │ Kafka Broker │
└──────────┘                                └──────────────┘

But internally, before that arrow ever becomes a real network call:

send("topicName", "key", "{json}")
        │
        ▼
   [ Stage 1: Serializer ]
        │
        ▼
   [ Stage 2: Partitioner ]
        │
        ▼
   [ Stage 3: Record Accumulator ]
        │
        ▼
   [ Stage 4: Compression ]
        │
        ▼
   [ Stage 5: Sender Thread ]  ──── (only NOW does the network call happen)
        │
        ▼
      Broker
```

Five distinct stages, each with its own job. This step covers Stage 1 in full detail; the rest follow in subsequent steps.

# 🎯Stage 1: Serializer

---

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%201.png)

### Why serialization is required at all

**The problem:** our message (event) is generally a **Java object** — a `String`, a custom POJO, a JSON structure, whatever your application works with. But **Kafka only understands bytes.** It has no concept of "integer," "string," "custom POJO" — none of that. All Kafka sees and stores is raw byte arrays.

This mismatch is exactly why the serialization stage exists: it's the bridge between "whatever Java object your application is working with" and "the byte array Kafka actually stores."

**Critical rule:** if serialization fails, **the message does not move to the next stage.** It's a hard gate — nothing gets partitioned, batched, or sent if it can't first be turned into bytes.

---

### What actually gets serialized

When you call `.send()`, you're really providing **three things conceptually**: the topic, a key, and a value. Of these, **both the key and the value get serialized independently** — because both of them are what ultimately get persisted inside Kafka.

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%202.png)

```
send("topicName", "key", "{json}")
        │
        ▼
┌─────────────────────────────────────────────────┐
│              Serializer Layer                   │
│                                                 │
│   Our Application                               │
│   Java Object (String, POJO, JSON)              │
│        │                    │                   │
│        │ key                │ value             │
│        ▼                    ▼                   │
│  Converts key → byte[]  Converts value → byte[] │
└─────────────────────────────────────────────────┘
```

---

### The `Serializer` interface

Every serializer — built-in or custom — implements the same interface:

```java
public interface Serializer<T> {
    byte[] serialize(String topic, T data);
}
```

Simple contract: give it a topic name and a piece of data of type `T`, and it hands back a `byte[]`. That's the entire job of a serializer.

---

### Built-in serializers

Kafka ships with ready-made serializers for common types — you don't need to write any of these yourself:

| Serializer | Java Type |
| --- | --- |
| `StringSerializer` | `String` |
| `IntegerSerializer` | `Integer` |
| `LongSerializer` | `Long` |
| `DoubleSerializer` | `Double` |
| `FloatSerializer` | `Float` |
| `ShortSerializer` | `Short` |
| `JsonSerializer` | Any POJO (most useful — converts a Java object to bytes) |

`JsonSerializer` deserves a special mention: it's the one you reach for whenever you're dealing with **plain old Java objects (POJOs)** — which, in a typical Spring Boot event-driven service, is most of the time.

---

### Custom serializers

Beyond the built-ins, you can also write your **own** serializer by implementing the same `Serializer<T>` interface. The instructor calls this out as useful when you need **full control** over how data is converted to bytes — two concrete examples he gives:

- **Removing sensitive fields** before the object ever gets converted to bytes and sent out
- **Encrypting the data** before byte conversion

```
┌────────────────────┐
│  Custom Serializer │
├────────────────────┤
│ - Strip sensitive  │
│   fields           │
│ - Encrypt data     │
│   before byte      │
│   conversion       │
└────────────────────┘
```

This is a deliberate design choice, not a limitation — Kafka gives you a hook to control *exactly* what bytes leave your application, rather than forcing a one-size-fits-all serialization.

The instructor notes this will be shown concretely with actual code once we get to the producer setup lecture — for now, the goal is just to understand *why* this stage exists and *what* it does conceptually.

---

### Recap of Step 1

| Concept | Core takeaway |
| --- | --- |
| **Illusion of simplicity** | `.send()` looks like one line, but only `bootstrap-servers` is mandatory — everything else, including all 5 internal stages, has defaults working silently underneath |
| **Multi-stage internal flow** | A message passes through Serializer → Partitioner → Record Accumulator → Compression → Sender Thread *inside the producer*, before any network call happens |
| **Why serialization exists** | Kafka only understands bytes — it has no awareness of Integer, String, POJO, JSON, etc. |
| **What gets serialized** | Both the **key** and the **value** — independently — since both get persisted in Kafka |
| **Hard gate** | If serialization fails, the message never proceeds to partitioning, batching, or sending |
| **`Serializer<T>` interface** | `byte[] serialize(String topic, T data)` — every serializer, built-in or custom, implements this |
| **Custom serializers** | Used for full control — e.g., stripping sensitive fields or encrypting data before byte conversion |

# 🎯Step 2: Stage 2 — Partitioner

---

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%203.png)

### Where this stage fits

Once the key and value have both been converted to bytes in Stage 1, they're passed into the **Partitioner** stage. This is where Kafka decides: **for this event, which partition of the topic should it actually land in?**

```
send("topicName", "key", "{json}")
        │
        ▼
   [ Stage 1: SERIALIZER ]        (object → byte[])
        │
        ▼
   [ Stage 2: PARTITIONER ]
```

The instructor makes a nice aside here: if you open up Kafka's own source code, there's a `KafkaProducer` class with a `doSend()` method — and you can literally see these stages laid out as sequential steps in the code itself (serialize, then partition, and so on). You could even put a debugger breakpoint there and watch it happen. This isn't an abstract mental model — it's a direct reflection of how the actual client code is structured.

---

### The `Partitioner` interface

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%204.png)

```java
public interface Partitioner {
    int partition(
        String topic,
        Object key,
        byte[] keyBytes,
        Object value,
        byte[] valueBytes,
        Cluster cluster
    );
}
```

**What goes in:** topic name, the key (both raw object and its serialized bytes), the value (both raw object and its serialized bytes), and `cluster` — which gives the partitioner visibility into the cluster's current state (e.g., how many partitions this topic actually has).

**What comes out:** a single integer — **the partition number** this event should be routed to.

```
┌─────────────────────────────────┐
│  public interface Partitioner   │
│                                 │
│  int partition(                 │
│     String topic,               │
│     Object key,                 │
│     byte[] keyBytes,            │
│     Object value,               │
│     byte[] valueBytes,          │
│     Cluster cluster             │
│  );                             │
└──────────────────────────v──────┘
              │
              ▼
      returns partition number
```

There are **three distinct cases** the partitioner has to handle, depending on what the producer actually provided when calling `.send()`.

---

### Case 1: Partition explicitly provided

Spring's `KafkaTemplate` has multiple **overloaded** `send()` methods. One of them lets you pass the partition number directly:

```java
**// KafkaTemplate.java**
@Override
public CompletableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, @Nullable V data) {
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, partition, key, data);
    return observeSend(producerRecord);
}
```

**In this case, the Partitioner does essentially nothing.** You've already told Kafka exactly which partition this event belongs in — there's no decision left to make. The event is wrapped into a `ProducerRecord` carrying that partition number directly.

---

### Case 2: Key is provided

This is the case most people actually use in practice. Another overloaded `send()` method takes a topic, key, and value — no explicit partition:

```java
**// KafkaTemplate.java**
@Override
public CompletableFuture<SendResult<K, V>> send(String topic, K key, @Nullable V data) {
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, data);
    return observeSend(producerRecord);
}
```

**Rule: Kafka ensures the same key always goes to the same partition.** This is done via the familiar formula (also covered back in the architecture series):

```
partition = hash(keyBytes) % numberOfPartitions
```

The partitioner runs this hash-and-mod calculation on the **serialized key bytes** from Stage 1, and the result deterministically decides which partition the event lands in. Since the same key always produces the same hash, all events sharing that key are guaranteed to land in the same partition — and consequently (recall from the architecture series), guaranteed to be read **in order relative to each other**, since ordering is preserved within a single partition.

```
Key: "orderId-123"
        │
        ▼
   hash("orderId-123") % 3 = 1
        │
        ▼
   → Partition 1
```

---

### Case 3: No key is provided

There's a **third** overloaded `send()` method — topic and value only, no key at all:

```java
// KafkaTemplate.java
@Override
public CompletableFuture<SendResult<K, V>> send(String topic, @Nullable V data) {
    ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, data);
    return observeSend(producerRecord);
}
```

No key means: **there's nothing to hash.** So how does the partitioner decide where this event goes?

The instructor is explicit that the answer here has **two different behaviors depending on the Kafka version**, and the difference is genuinely important enough that he deliberately **defers the full explanation**:

> **Pre-Kafka 2.4:** Round Robin Partitioning Strategy
**From Kafka 2.4 onwards:** Sticky Partitioning Strategy
> 

He holds off explaining *why* these differ until **after** the next stage (Record Accumulator) is covered — because the reasoning behind *why* sticky partitioning is actually better only makes sense once you understand how batching works. So this case is intentionally left as an open thread here, to be resolved in Step 3.

```
send(topic, value)   ← no key!
        │
        ▼
   Partitioner: "no key to hash... which partition?"
        │
        ▼
   ??? → explained after Record Accumulator (Step 3)
```

---

### Recap of Step 2

| Case | Trigger | What the Partitioner does |
| --- | --- | --- |
| **Case 1** | Partition explicitly passed to `send()` | Nothing — partitioner is bypassed, the given partition is used directly |
| **Case 2** | Key is provided | `partition = hash(keyBytes) % numberOfPartitions` — same key always → same partition, guaranteeing relative order |
| **Case 3** | No key provided | Deferred — Round Robin (pre-Kafka 2.4) vs Sticky Partitioning (Kafka ≥ 2.4), explained after Record Accumulator |

# 🎯Step 3: Stage 3 — Record Accumulator (+ Resolving Case 3: Round Robin vs Sticky Partitioning)

---

### Where this stage fits

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%205.png)

```
send("topicName", "key", "{json}")
        │
        ▼
   [ Stage 1: SERIALIZER ]         (object → byte[])
        │
        ▼
   [ Stage 2: PARTITIONER ]
        │
        ▼
   [ Stage 3: RECORD ACCUMULATOR ]
```

Once the partitioner has decided **which partition** an event should go to, the event doesn't fly off to the broker immediately. It first passes through the **Record Accumulator** — and the instructor flags this as a **very important stage**, because it's the reason Kafka producers achieve such high throughput.

---

### What is the Record Accumulator?

**Definition:** an **in-memory buffer inside the Kafka producer** that collects events **per topic-partition**, and groups them into **batches**.

**The core mental model:** the producer never sends events one at a time to the broker. It always groups events — by topic *and* partition — into a batch first, and only the batch gets sent.

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%206.png)

```
                 Partitioner
                     │
                     │ Event: E1
                     │ Topic: order-events
                     │ Partition: P0
                     ▼
              Record Accumulator
                            │
        ┌───────────────────┼──────────────────┐
        ▼                   ▼                  ▼
Topic: order-events  Topic: order-events  Topic: product-events
Partition: P0        Partition: P1        Partition: P0
┌──────────┐         ┌──────────┐         ┌──────────┐
│   E1     │         │          │         │          │
└──────────┘         └──────────┘         └──────────┘
   Batch                Batch                Batch
```

**Why grouping by topic *and* partition specifically matters:** a topic can have many partitions, and those partitions can live on **different brokers**. If the Record Accumulator didn't separate events this way, the producer could end up mixing events destined for different brokers into a single confused batch. By grouping strictly by topic-partition, every batch that eventually forms is guaranteed to be destined for exactly **one** partition — which, as we'll see in Stage 5, makes it trivial to route efficiently to the right broker.

**Bottom line:** *Producer always maintains topic-partition-wise batches, and sends batches to the broker — never individual events.* This batching is exactly what gives Kafka producers their high throughput (the ability to transmit large volumes of data within a given timeframe).

---

### Now resolving Case 3: Round Robin vs. Sticky Partitioning

Recall from Step 2 — when no key is provided, the partitioner has nothing to hash, so it needs some other strategy to decide the partition. This is where that gets resolved, because the *right* answer only makes sense once you understand batching.

**Setup for the worked example (instructor's numbers):**

```
Topic: order-events
Partitions available: [P0, P1, P2]

Events (all with no key):
Topic: order-events, Key = null, Value = E1
Topic: order-events, Key = null, Value = E2
Topic: order-events, Key = null, Value = E3
Topic: order-events, Key = null, Value = E4
Topic: order-events, Key = null, Value = E5
```

---

#### Pre-Kafka 2.4: Round Robin Partitioning Strategy

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%207.png)

**Rule:** simply cycle through the available partitions, one after another, for every incoming event — regardless of batching state.

```
E1 → P0
E2 → P1
E3 → P2
E4 → P0   (cycle repeats)
E5 → P1
```

```
┌─────────────────────────────────────────────────┐
│           Round Robin Partitioning Strategy     │
│                                                 │
│   Partitioner                                   │
│      │                                          │
│      ▼   E1→P0  E2→P1  E3→P2  E4→P0  E5→P1      │
│   Record Accumulator                            │
│      │           │           │                  │
│      ▼           ▼           ▼                  │
│  Topic: P0    Topic: P1   Topic: P2             │
│  ┌────────┐   ┌────────┐  ┌────────┐            │
│  │E1  E4  │   │E2  E5  │  │E3      │            │
│  └────────┘   └────────┘  └────────┘            │
│    Batch        Batch       Batch               │
└─────────────────────────────────────────────────┘
```

**The problem this creates:** instead of one batch collecting all 5 events, you end up with **three small, mostly-empty batches** — P0 gets 2 events, P1 gets 2 events, P2 gets just 1. If your configured batch capacity is, say, 100 events, none of these batches are anywhere close to full.

**Consequence:** since each of these three separate, small batches eventually has to be sent to the broker as its own request, you get **multiple network calls** instead of one — batch size is small, and network efficiency suffers.

---

#### From Kafka 2.4 onwards: Sticky Partitioning Strategy

**The fix:** instead of blindly round-robining every single event, the partitioner picks **one partition randomly** for a given topic, and then **sticks to it** — routing every subsequent no-key event for that topic to the *same* partition, until the batch currently being filled is closed.

**Walking through it, event by event:**

**Event 1 arrives (no key):**

- Partitioner checks: "Do I have a sticky partition remembered for `order-events`?" — No.
- It randomly picks one, say **P1**.
- It stores this in memory: `order-events → P1` (sticky).
- Sends E1 to the Record Accumulator, targeting P1.

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%208.png)

```
┌──────────────────────────────────────────────────┐
│              Sticky Partitioning Strategy        │
│                                                  │
│   Partitioner          - E1 comes, no key        │
│      │                 - Picks random partition, │
│      ▼                   say P1                  │
│   Record Accumulator   - Stores in-memory:       │
│      │                   order-events → P1       │
│      ▼                 - Sends P1 to accumulator │
│  Topic: P0   Topic: P1   Topic: P2               │
│  ┌──────┐   ┌──────┐   ┌──────┐                  │
│  │      │   │  E1  │   │      │                  │
│  └──────┘   └──────┘   └──────┘                  │
│   Batch       Batch      Batch                   │
└──────────────────────────────────────────────────┘
```

**Events 2, 3, 4, 5 arrive (also no key):**

- Each time, the partitioner checks its sticky memory: "For `order-events`, I have P1 remembered."
- It doesn't randomize again — it just returns **P1** every time.
- All of them get routed into the *same* batch.

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%209.png)

```
┌──────────────────────────────────────────────────┐
│   Partitioner          - E2, E3, E4, E5 come,    │
│      │                   also no key             │
│      ▼                 - Checks sticky memory:   │
│   Record Accumulator     order-events → P1       │
│      │                 - Sends P1 to accumulator │
│      ▼                   every time              │
│  Topic: P0   Topic: P1   Topic: P2               │
│  ┌──────┐   ┌──────┐   ┌──────┐                  │
│  │      │   │  E1  │   │      │                  │ 
│  │      │   │  E2  │   │      │                  │
│  │      │   │  E3  │   │      │                  │
│  │      │   │  E4  │   │      │                  │
│  │      │   │  E5  │   │      │                  │
│  └──────┘   └──────┘   └──────┘                  │
│   Batch       Batch      Batch                   │
└──────────────────────────────────────────────────┘
```

**Result:** instead of three small batches, we now have **one full batch** — which means **one single network call** to the broker instead of three.

---

### Why isn't this just "always send everything to one partition forever"?

This is the important nuance that separates sticky partitioning from a naive "always P1" rule. The stickiness only lasts **until the current batch is closed.**

**Worked continuation:** say the batch capacity is 5, and it's now full (E1–E5 are in it, and the batch is closed, ready to be dispatched to the broker).

- Event 6 arrives (no key). The partitioner checks its sticky memory: still says P1. So it routes E6 toward P1.
- But the Record Accumulator says: "That P1 batch is already closed and ready to send — I need to open a **new** batch for P1."
- The moment the Record Accumulator opens a new batch, it signals this back to the partitioner: *"a new batch was just created."*
- On seeing this signal, the partitioner **re-randomizes** — it picks a fresh partition (could be P0, P1, or P2 again, purely random) and updates its sticky memory accordingly. Say it picks **P2** this time.
- From E7 onward, everything sticks to **P2**, until *that* batch closes too — and the cycle repeats.

```
Batch capacity = 5, P1 batch (E1-E5) is FULL and CLOSED

E6 arrives → Partitioner checks sticky memory → still says P1
        │
        ▼
Record Accumulator: "P1 batch is closed, opening a NEW batch"
        │
        ▼
Signals partitioner: "new batch created"
        │
        ▼
Partitioner RE-RANDOMIZES → picks (say) P2 → updates sticky memory:
   order-events → P2
        │
        ▼
E6, E7, E8... all stick to P2 until THAT batch closes too
```

**Key takeaway:** sticky partitioning isn't round robin, and it isn't "always the same partition forever" either — it's **"stick to one partition per batch, then re-roll when a new batch opens."** This is exactly what maximizes batch fullness (and therefore minimizes network calls) while still spreading load reasonably evenly across partitions over time.

---

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%2010.png)

### Controlling the batch: `batch.size` and `linger.ms`

Two natural questions follow: *how big should a batch be allowed to get?* and *how long should the producer wait before giving up on filling it further?* Two configs answer these — both **optional**, both with sensible defaults.

#### `batch.size` (default: 16 KB / `16384` bytes)

**Meaning:** the **maximum memory allocated per partition-batch.** Once a batch reaches this size, it's considered closed and ready to be dispatched.

**Worked example:** if one event is roughly 1 KB, a 16 KB batch can hold **around 16 messages**. Once that limit is hit — regardless of anything else — the batch closes and becomes ready for sending.

```
batch.size = 16 KB, Event size = 1 KB
        │
        ▼
Batch fills up to ~16 messages → LIMIT REACHED
        │
        ▼
Batch is CLOSED → ready to send
```

#### `linger.ms` (default: 10 ms in the transcript's example)

**Meaning:** the **maximum time the producer will wait** for more records to arrive and fill the batch, before giving up and sending whatever it has.

**Why this exists:** without it, a batch could sit around indefinitely waiting to fill up if traffic is slow — and the producer would never actually send anything. `linger.ms` guarantees the producer **never waits indefinitely** for a batch to fill during low-traffic periods.

**Worked example:** batch capacity holds ~16 messages, but traffic is light and only 5 events have trickled in. The producer will wait **at most 10 ms** for more events to show up. Once that 10 ms window expires — full or not — the batch is closed and marked ready to send.

```
Batch capacity: ~16 messages    Only 5 events arrived so far
        │
        ▼
Producer waits... up to linger.ms (10ms)
        │
        ▼
10ms elapsed, batch still not full
        │
        ▼
Batch is CLOSED anyway → ready to send
```

**Together, these two configs form the complete "when does a batch close?" rule:** *whichever happens first* — `batch.size` is reached, OR `linger.ms` time elapses.

---

### Recap of Step 3

| Concept | Core takeaway |
| --- | --- |
| **Record Accumulator** | In-memory buffer inside the producer; collects events per topic-partition and groups them into batches — this is the core reason Kafka achieves high throughput |
| **Batching, not single-event sends** | Producer always sends whole batches to the broker, grouped strictly by topic + partition, never individual events |
| **Round Robin (pre-Kafka 2.4)** | Cycles through partitions per event, ignoring batch state — creates multiple small, under-filled batches → more network calls |
| **Sticky Partitioning (Kafka ≥ 2.4)** | Randomly picks one partition per topic and sticks to it for all no-key events, until the current batch closes — then re-randomizes for the next batch. Maximizes batch fullness → fewer network calls |
| **`batch.size`** (default 16 KB) | Maximum size a batch can grow to before it's closed and marked ready to send |
| **`linger.ms`** (default 10ms in this example) | Maximum time the producer waits for more events before closing an unfilled batch — prevents indefinite waiting during low traffic |
| **Batch closes when...** | Whichever comes first: `batch.size` reached, or `linger.ms` elapses |

# 🎯Step 4: Stage 4 — Compression

---

### Where this stage fits

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%2011.png)

```
send("topicName", "key", "{json}")
        │
        ▼
   [ Stage 1: SERIALIZER ]          (object → byte[])
        │
        ▼
   [ Stage 2: PARTITIONER ]
        │
        ▼
   [ Stage 3: RECORD ACCUMULATOR ]
        │
        ▼
   [ Stage 4: COMPRESSION ]
```

Once the Record Accumulator has grouped events into a batch and that batch is closed and ready to send, it passes through one more stage before the Sender Thread takes over: **Compression.**

---

### What happens at this stage

**Rule:** batches which are ready to send get **compressed** — the goal being to reduce network usage and, as a direct result, achieve **higher throughput.**

There are multiple compression algorithms available to choose from — the transcript mentions **gzip** and **Snappy** as examples (the PDF note adds **lz4** and **zstd** to that list). You pick one algorithm, and the producer compresses the batch using it before dispatch.

```
┌────────────────────────────────────────────┐
│         Batch (before compression)         │
│              64 KB                         │
└────────────────────────────────────────────┘
                    │
                    ▼  Compression algorithm (**gzip** / snappy / lz4 / zstd)
                    ▼
┌────────────────────────────────────────────┐
│         Batch (after compression)          │
│              18 KB                         │
└────────────────────────────────────────────┘
```

**Worked example (instructor's numbers):** a batch that's 64 KB *before* compression can shrink down to roughly 18 KB *after* compression — a substantial reduction.

---

### Why compression matters — two distinct benefits

**1. Reduced network usage → higher throughput.** When the payload size going from producer to broker is smaller, more data can effectively move through the same network capacity in the same amount of time. Less bytes on the wire directly translates to better throughput.

**2. Disk space savings.** This is an important detail the instructor stresses: Kafka doesn't decompress a batch before storing it — it **stores the compressed batches as-is** on disk. So compression isn't just a network-time win; it also means less disk space consumed on the broker side, for exactly the same data.

```
Compression benefits:
   ├──► Network: smaller payload → higher throughput producer → broker
   └──► Disk: Kafka stores the COMPRESSED batch directly → saves disk space too
```

---

### Why compression works, conceptually

Without getting into the internals of any specific algorithm, the instructor gives a simple intuition: compression works by finding **efficient ways to represent repetitive data.** If a batch has many repeated words or patterns, the algorithm can represent those repetitions far more compactly (e.g., replacing a repeated word with a short reference/token) — bringing down the overall size, while still being fully reversible back to the original data whenever the broker/consumer needs to read it.

---

### The instructor's recommendation

He's fairly direct about this: **use compression.** There's also an "uncompressed" option available (i.e., skip this stage entirely), but given the two benefits above — network savings *and* disk savings — his advice is that if you're currently running uncompressed, it's worth revisiting and picking one of the available algorithms instead.

Like the other stages, this is fully **optional** — if you don't configure a compression algorithm, there's a sensible default already in place. The instructor notes that exactly *how* to configure this (which algorithm, which property) will be shown concretely once we reach the producer setup lecture — for now, the goal is just understanding *why* this stage exists and *what* it buys you.

---

### Recap of Step 4

| Concept | Core takeaway |
| --- | --- |
| **What happens** | Batches that are ready to send get compressed before dispatch, using an algorithm like gzip, Snappy, lz4, or zstd |
| **Network benefit** | Smaller payload size from producer → broker means higher throughput |
| **Disk benefit** | Kafka stores the *compressed* batch as-is — so compression also saves disk space on the broker, not just network time |
| **How it works, conceptually** | Compression algorithms represent repetitive data more efficiently, shrinking size while remaining fully reversible |
| **Instructor's take** | Use compression — the benefits (network + disk) generally outweigh skipping it; configuration is optional, with a default in place if unset |

# 🎯Step 5: Stage 5 — Sender Thread

---

### Where this stage fits

![image.png](Step%201%20How%20Kafka%20Producer%20Works%20Internally/image%2012.png)

```
send("topicName", "key", "{json}")
        │
        ▼
   [ Stage 1: SERIALIZER ]          (object → byte[])
        │
        ▼
   [ Stage 2: PARTITIONER ]
        │
        ▼
   [ Stage 3: RECORD ACCUMULATOR ]
        │
        ▼
   [ Stage 4: COMPRESSION ]
        │
        ▼
   [ Stage 5: SENDER THREAD ]   ← the ONLY stage that actually talks to the network
```

This is the final stage — and the one where an actual network call finally happens.

---

### The critical split: Application Thread vs. Sender Thread

**The single most important idea in this stage:** *your application thread never sends data to the Kafka broker.* A **separate, background sender thread** does the actual network communication.

**What the application thread actually does**, when you call `.send()`:

1. Runs the event through the **Serializer** (Stage 1)
2. Runs it through the **Partitioner** (Stage 2)
3. Hands it off to the **Record Accumulator** (Stage 3) — which buffers it into a batch
4. That's it. **The application thread's job ends here.**

Once the event has been handed off to the Record Accumulator's buffer, the application thread is **free** — it doesn't wait around for the network call to happen. Instead, `.send()` immediately returns a **`Future`** (in Spring's case, a `CompletableFuture<SendResult<K,V>>`), and the application thread goes off to do whatever else it needs to do.

```
Application Thread
   │
   ├──► Serialize (Stage 1)
   ├──► Partition (Stage 2)
   ├──► Submit to Record Accumulator (Stage 3)
   │         │
   │         ▼
   │    returns a Future immediately
   │
   ▼
Application thread is now FREE to do other work
```

Meanwhile, on your own code side, this looks like:

```java
future.whenComplete((result, ex) -> {
    // this callback runs LATER, whenever the sender thread
    // actually completes the send (success or failure)
});
```

The application thread doesn't block on this — it registers the callback and moves on. Later, once the actual send completes, the **sender thread** is the one that invokes this callback (marking the future as complete).

---

### The Sender Thread's responsibilities

The sender thread is a background thread that Kafka manages internally, and its job is to **look for batches that are ready to be sent** and actually get them to the broker. Concretely, it does four things:

1. **Check the Record Accumulator for ready batches** — batches that have been closed (either `batch.size` reached or `linger.ms` elapsed, from Step 3), and compressed (Step 4)
2. **Group batches by broker leader**
3. **Send the request** using a `NetworkClient`
4. **Process responses** (acks, retries), **handle retries** if needed, and **invoke callbacks** once complete

```
Sender Thread Responsibilities:
   1. Check Record Accumulator for ready batches
   2. Group batches by broker leader
   3. Send request via NetworkClient
   4. Process response (acks/retries) → handle retries → invoke callback
```

---

### Grouping batches by broker leader — one more efficiency layer

This is where the topic-partition-wise batching from Stage 3 pays off directly. Consider a setup:

```
Partitions: P0, P1, P2
Broker 1 → Leader for P0 and P2
Broker 2 → Leader for P1
```

Say the Record Accumulator has three ready batches at this point:

```
Batch 1 → Topic A, Partition 0
Batch 2 → Topic A, Partition 2
Batch 3 → Topic A, Partition 1
```

The sender thread looks up: *who's the leader for each of these partitions?*

- Batch 1 (P0) → leader is **Broker 1**
- Batch 2 (P2) → leader is **Broker 1**
- Batch 3 (P1) → leader is **Broker 2**

Instead of sending Batch 1 and Batch 2 to Broker 1 as **two separate network requests**, the sender thread **wraps them together into a single request** — one network call carrying both batches, since they're headed to the same broker anyway.

```
Batch 1 (TopicA-P0) → Leader: Broker1  ┐
Batch 2 (TopicA-P2) → Leader: Broker1  ┴──► wrapped into ONE ProduceRequest → Broker1
Batch 3 (TopicA-P1) → Leader: Broker2  ─────► sent as its own request → Broker2
```

**Why this matters:** this is one more layer of throughput optimization stacked on top of batching itself — grouping *requests*, not just grouping *events*. Fewer network round-trips for the same amount of data.

---

### Can we control the sender thread itself?

**No.** The instructor is explicit: there's no way (to his knowledge) to control how many sender threads exist, or otherwise configure the sender thread directly — Kafka manages this entirely internally.

---

### What we *can* control: `max.in.flight.requests.per.connection`

While the sender thread itself isn't configurable, there's a closely related setting we do control.

**Setup:** the sender thread maintains a **TCP connection per broker.** If you have 3 brokers, the sender thread opens 3 TCP connections — one to each.

**The concept of "in-flight":** once the sender thread sends a request (a message — which, remember, can itself be a group of multiple batches wrapped together) to a broker, that broker takes some time to process it. During that window — request sent, but response not yet received — the request is considered **"in flight."**

**The config:**

```
max.in.flight.requests.per.connection = 5   (default)
```

**Meaning:** on a **single TCP connection**, the client can have up to this many **unacknowledged requests outstanding** before it has to block and wait.

**Worked example:** with `max.in.flight.requests.per.connection = 5` and 3 brokers (3 TCP connections):

```
Connection to Broker1: up to 5 requests in flight
Connection to Broker2: up to 5 requests in flight
Connection to Broker3: up to 5 requests in flight
                                    ─────────────
                        Total: 15 requests in flight across the cluster at once
```

So the sender thread doesn't wait for one request to finish before sending the next — it can have several outstanding simultaneously per connection, and the broker sends back responses (success, failure, retry-needed) as it finishes processing each one. This is one of the core reasons Kafka producers achieve such high throughput: batching reduces the *number* of events per request, and in-flight requests let multiple requests be outstanding *simultaneously* per connection.

```
Sender Thread                          Broker 1
     │                                     │
     │──── message1 (in flight) ─────────► │
     │──── message2 (in flight) ─────────► │  (processing...)
     │──── message3 (in flight) ─────────► │
     │──── message4 (in flight) ─────────► │
     │──── message5 (in flight) ─────────► │
     │                                     │
     │◄─────── response for message2 ──────│
     │◄─────── response for message4 ──────│
     │           ⋮                         │
```

---

### The critical risk: message reordering

This is flagged explicitly as **interview-relevant** — the kind of question an architect or interviewer might specifically probe on.

**The setup for the risk:** whenever **both** of these are true —

```
max.in.flight.requests.per.connection > 1
        AND
retries > 0
```

— there is a **risk of message reordering.**

**Worked example (Request A / Request B):**

1. Request A and Request B are both sent, both currently **in flight** on the same connection.
2. Request A fails with a **transient error** and gets reported back to the sender thread.
3. Meanwhile, **Request B succeeds** — it gets persisted successfully into the partition's log, and gets earlier offsets.
4. The sender thread **retries** Request A (since it was a transient, retryable failure). This retry eventually **succeeds** too — but now it gets written **after** Request B.

```
Request A ──► in flight  ┐
Request B ──► in flight  ┘

Request A ──► FAILS (transient error)
Request B ──► SUCCESS (written successfully into partition)

Request A ──► retries... SUCCESS (but written AFTER Request B)
```

**The result:** even though Request A was sent *first*, it ends up stored in the partition's log **after** Request B — because A had to retry while B sailed through on the first attempt. The **ordering guarantee within a partition** (which the whole architecture series stressed as a core Kafka promise) gets silently violated.

---

### Why you can't just "fix" this by tweaking the obvious knobs

- **Setting `max.in.flight.requests.per.connection = 1`** would solve the reordering problem — but at the direct cost of **throughput**, since you lose the ability to have multiple requests outstanding at once. This setting exists specifically *to increase* throughput, so this trade-off is expensive.
- **Setting `retries = 0`** isn't a real option either — transient failures are a normal, expected part of distributed systems, and retrying is important for not losing data on temporary blips.

Both settings (`max.in.flight.requests.per.connection` and `retries`) matter for good reason — you can't simply disable either one to sidestep this risk.

---

### The resolution (teased, not explained here)

The instructor is explicit that this risk is resolved through a setting called **idempotency** (`enable.idempotence = true`), and that this deserves its **own separate, dedicated lecture** — it's genuinely important enough not to compress into a footnote here. So for this lecture, the reordering risk is flagged and understood conceptually, but the *how it's fixed* is deferred to **Lecture 3: Kafka Producer Idempotency Internals.**

```
Risk:  max.in.flight.requests.per.connection > 1  +  retries > 0
              │
              ▼
     → Message reordering possible
              │
              ▼
     Resolved via: IDEMPOTENCY (enable.idempotence = true)
              │
              ▼
     → Covered separately (next-next lecture)
```

---

### Recap of Step 5

| Concept | Core takeaway |
| --- | --- |
| **Application thread vs. Sender thread** | Application thread only serializes, partitions, and hands off to the Record Accumulator — then it's free. A separate background **sender thread** does the actual network communication |
| **`Future`/callback pattern** | `.send()` returns a `Future` immediately; the sender thread later invokes the registered callback once the send actually completes |
| **Sender thread's 4 responsibilities** | Check Record Accumulator for ready batches → group by broker leader → send via `NetworkClient` → process responses/retries/callbacks |
| **Grouping by broker leader** | Multiple batches headed to the same broker get wrapped into **one** request — fewer network round-trips |
| **Sender thread control** | Cannot be configured directly — Kafka manages it internally |
| **`max.in.flight.requests.per.connection`** (default 5) | Max unacknowledged requests allowed per TCP connection before blocking — one per broker connection, so total in-flight = this value × number of brokers |
| **Message reordering risk** | Occurs when `max.in.flight.requests.per.connection > 1` AND `retries > 0` — a failed-then-retried request can land *after* a request that succeeded on its first attempt, breaking within-partition order |
| **Why you can't just tweak the settings away** | Setting in-flight to 1 kills throughput; setting retries to 0 risks silent data loss on transient errors — neither is a real fix |
| **The actual fix** | `enable.idempotence = true` — deferred to its own dedicated lecture (Producer Idempotency Internals) |

---

All 5 internal producer stages are now fully documented:

| Stage | Core Idea |
| --- | --- |
| **Stage 1: Serializer** | Converts key + value from Java objects to bytes; built-in serializers for common types, custom serializers for full control (e.g., encryption, field stripping) |
| **Stage 2: Partitioner** | Decides target partition — explicit partition (Case 1), key-based hash (Case 2), or no-key strategy (Case 3) |
| **Stage 3: Record Accumulator** | Buffers events into topic-partition-wise batches; resolves Case 3 via Round Robin (pre-2.4) vs. Sticky Partitioning (≥2.4); controlled by `batch.size` and `linger.ms` |
| **Stage 4: Compression** | Compresses ready batches (gzip/Snappy/lz4/zstd) — saves both network bandwidth and broker disk space |
| **Stage 5: Sender Thread** | Background thread that groups batches by broker leader and does the actual network send; introduces the in-flight-requests + retries reordering risk, resolved later via idempotency |

**Interview-ready one-liners from this lecture:**

- *"A Kafka producer's `.send()` doesn't hit the network immediately — it passes through Serializer, Partitioner, Record Accumulator, Compression, and finally the Sender Thread, all inside the producer."*
- *"Sticky partitioning (Kafka ≥ 2.4) improved on round robin by sticking to one randomly chosen partition per batch, instead of spreading events thinly across partitions and creating many small batches."*
- *"The application thread never talks to the broker directly — it only gets as far as the Record Accumulator; a separate sender thread handles all actual network I/O."*
- *"Whenever `max.in.flight.requests.per.connection > 1` and `retries > 0`, message reordering is possible — a retried request can land after a request that succeeded on the first try. This is exactly what idempotency is designed to solve."*