# Kafka Architecture — Part 1 (Notes)

## Step 1: Context, What is Kafka, Producer, Topic

---

### Why this lecture is different (instructor's framing)

Before jumping into "how to publish an event" or "how to consume an event" in Kafka, the instructor first wants to build a solid mental model of *how Kafka actually works internally*. He's explicit that this won't be a quick 15-20 minute high-level overview — he's going deep, and he warns: if at any point something feels complex, pause the video and re-read/re-watch before moving forward, because this foundation is critical for everything that comes later (producer/consumer implementation, cluster behavior, failure handling, etc.).

This is also framed as **the** classic interview opener: *"What is Kafka?"* — a question the instructor says he's personally been asked a lot. So the goal of this note isn't just conceptual understanding, it's being able to answer this confidently and precisely in an interview.

---

### What is Kafka?

**Definition:** Kafka is a **distributed event streaming platform** that allows you to:
- **Publish** events
- **Store** events (this is what makes it "streaming" — you can replay events later, unlike a pure pub-sub system where a message is gone once delivered)
- **Subscribe to / consume** events

The key word instructors want you to hold onto here is **streaming** — meaning the data isn't just passed through, it's persisted so it can be replayed.

---

### Producer

**Definition:** A producer is an application that **publishes events to Kafka**.

Important nuance: **the producer does not care who consumes the event.** It just publishes. What happens downstream — who reads it, how many readers, when they read it — is completely decoupled from the producer's job.

```
┌──────────┐        ┌───────┐
│ Producer │ ─────► │ Kafka │
└──────────┘        └───────┘
```

This decoupling is actually one of the core ideas of event-driven architecture in general (producer and consumer don't need to know about each other), and Kafka is built around that principle.

---

### Topic

**Definition:** Think of a topic as a **category or folder** to which events are published.

Key clarifications the instructor stresses:
- A topic itself does **not** hold events directly — it's a **logical grouping/concept**, not a physical storage unit. (The actual physical storage happens one level down, inside *partitions* — covered in Step 2.)
- **Many producers can write to the same topic.** It's not a 1:1 relationship.

```
┌───────────┐
│ Producer1 │──────┐
└───────────┘      │      ┌──────────────────┐
                   ├────► │  Topic:          │
┌───────────┐      │      │  order-events    │
│ Producer2 │──────┘      └──────────────────┘
└───────────┘
```

So at this point, the mental model is:
> Producer publishes → Producer doesn't care who reads it → Topic is just the named "folder" the event is filed under → many producers can file into the same folder.

---
## Step 2: Partition — Physical, Ordered, Append-only Log


### Why Partition exists

The instructor introduces Partition as: **"a physical, ordered, append-only log that is part of a Topic."** This one line packs three separate properties into it, and he unpacks each one carefully. Also important: **a topic can have many partitions.**

```
Topic: order-events
        │
        ├──► Partition0 → order-events-0  (partition0 log file)
        ├──► Partition1 → order-events-1  (partition1 log file)
        └──► Partition2 → order-events-2  (partition2 log file)
```

So the topic ("order-events") is just the folder name — the actual data lives inside its partitions, each of which is its own separate log file on disk.

---

### Property 1: Physical

This is the simplest of the three: **events are actually stored on disk, inside the partition.**

```
order-events/                  ← Topic (folder, logical only)
   ├── order-events-0.log      ← Partition0 (physical file)
   ├── order-events-1.log      ← Partition1 (physical file)
   └── order-events-2.log      ← Partition2 (physical file)
```

So while the *topic* is just a logical label, each *partition* is a real file living on the broker's disk.

---

### Property 2: Ordered

This is where offsets come in.

**Rule:** Every time an event arrives at a partition, that partition assigns it the **next incremental offset**. Offsets always increase — 0, 1, 2, 3... — and each partition tracks its own offset counter **independently** of every other partition.

Instructor's example:

- Event0 arrives → goes to Partition0 → gets **Offset 1**
- Event1 arrives → goes to Partition1 → gets **Offset 1**
- Event2 arrives → goes to Partition2 → gets **Offset 1**

```
Topic name = order-events

P0   [ E0 ]                    Offset1
P1   [ E1 ]                    Offset1
P2   [ E2 ]                    Offset1
```

Now say a new event (Event3) comes in and also lands in Partition0. Partition0 checks its own counter (currently at 1), increments it, and assigns **Offset 2** to Event3.

```
P0   [ E0 ][ E3 ]              Offset1  Offset2
P1   [ E1 ]                    Offset1
P2   [ E2 ]                    Offset1
```

**Consumer read behavior:** consumers cannot request events out of sequence (e.g. "give me offset 32, then 42, then 52"). They must read sequentially — e.g. "give me offsets 100 to 200" — always in increasing order.

**Each partition maintains its own independent offset counter.** Partition0 having offset 2 has *zero* relationship to Partition1 or Partition2's offset values — they don't share a counter.

---

### The critical ordering nuance: local order vs. global order

This is the part the instructor spends the most time on, because it's a common interview trap.

> **Within a single partition, order is guaranteed.**

Meaning: since events are written sequentially and assigned strictly increasing offsets, you can always say *"this event happened after that event"* by comparing their offsets **within the same partition**. In the example above, we know **Event3 happened after Event0**, because offset 2 comes after offset 1 — and both are in Partition0.

> **But Kafka does NOT guarantee order globally — i.e., across different partitions of the same topic.**

Example: Event4 sits in Partition1 at offset 2. Can we say whether Event4 happened before or after Event0 (which is in Partition0 at offset 1)? **No** — because Partition0 and Partition1 maintain their offset counters completely independently. There's no shared clock or shared counter between them, so their offsets aren't comparable across partitions.

```
P0:  [ E0 @1 ][ E3 @2 ]   ← order guaranteed: E0 before E3
P1:  [ E1 @1 ][ E4 @2 ]   ← order guaranteed: E1 before E4

But: Is E4 (P1, offset 2) before or after E0 (P0, offset 1)?
     → CANNOT be determined — different partitions, independent counters
```

**Takeaway for interviews:** "Kafka guarantees ordering *within* a partition, not *across* partitions of a topic." This single sentence is exactly the kind of precise, correct answer interviewers are listening for.

---

### Property 3: Append-only log

**Rule:** Whatever file a partition is writing to, new events are **always appended to the end**. Kafka never inserts in the middle and never rewrites old entries.

Walking through the instructor's continued example — current state:

```
Topic: order-events

P0 (order-events-0):  Offset1: E0   Offset2: E3
P1 (order-events-1):  Offset1: E1   Offset2: E4
P2 (order-events-2):  Offset1: E2
```

Now a new event, **E5**, arrives and is routed to **Partition2**. Partition2 computes its next offset (currently at 1, so next is 2) and **appends** E5 to the end of its log file:

```
P0 (order-events-0):  Offset1: E0   Offset2: E3
P1 (order-events-1):  Offset1: E1   Offset2: E4
P2 (order-events-2):  Offset1: E2   Offset2: E5   ← newly appended
```

The file is *always* appended to — this is what "append-only log" means, and it's also the property that (as we'll see much later in the lecture series) makes Kafka's disk writes fast, since sequential appends are the best-case scenario for disk I/O.

---

### Recap of Partition's 3 properties

| Property | Meaning |
|---|---|
| **Physical** | Events are actually stored on disk inside the partition (not just a logical concept, unlike Topic) |
| **Ordered** | Each partition assigns strictly increasing offsets to incoming events; order is guaranteed *within* a partition, not *globally* across partitions |
| **Append-only log** | New events are always added to the end of the file — never inserted in the middle, never overwritten |

---
## Step 3: Segments + Index Files


### Going one level deeper into Partition

Up to this point, the mental model was: **one partition = one log file.** The instructor now breaks that assumption — in reality, a partition's log file is **further divided into multiple smaller segment files.**

```
Topic: order-events
    │
    └──► Partition0: order-events-0
              │
              ├──► Segment1: 000000.log   → stores events with offset 0–499
              ├──► Segment2: 000500.log   → stores events with offset 500–999
              └──► Segment3: 001000.log   → stores events with offset starting at 1000 (ongoing)
```

**Naming convention:** each segment file is named after the **starting offset** it holds. So `000500.log` tells you immediately: "this segment's events start from offset 500." Since the previous segment (`000000.log`) covers up to 499, this one picks up right where that left off.

---

### Why split into segments? (the "why" the instructor insists on)

He poses this as a direct question: *why not just keep one giant log file per partition?*

**The scenario:** a consumer says, *"I want to read from Partition0, offset 550 to 800."*

- If there's only **one big log file**, Kafka has to load that entire file into memory and scan through it sequentially to find offset 550 — slow and inefficient.
- With **segments**, Kafka can look at the segment names, recognize that offset 550 falls inside `Segment2: 000500.log`, and **jump directly to that segment** — skipping Segment1 and Segment3 entirely. Only that one segment needs to be loaded into memory.

```
Consumer wants: offset 550 → 800

Segment1 (000000.log) 0–499     ✗ skip
Segment2 (000500.log) 500–999   ✓ jump directly here
Segment3 (001000.log) 1000+     ✗ skip
```

This is the **first efficiency win**: segment-based jumping avoids scanning irrelevant data.

**Bonus control:** segment size itself is configurable — e.g., you can set each segment to be capped at 1 GB.

---

### The second problem: segments can still be big

Even after jumping to the right segment, that segment might still be, say, 1 GB with a huge number of entries. Sequentially scanning *within* that segment to find offset 550 is still slow.

**Kafka's solution: an index file, maintained per segment.**

```
Partition0: order-events-0
    │
    ├──► Segment1: 000000.log     (actual event data)
    └──► Segment1: 000000.index   (index into that segment)
```

The index file stores entries like:

| Offset | Physical position in log file (bytes) |
|---|---|
| 0 | 0 |
| 150 | 4200 |
| 300 | 8900 |
| 450 | 13200 |

**Critical detail:** the index does **NOT** store an entry for *every single offset* (offset 0, offset 1, offset 2...) — if it did, the index file itself would become huge and defeat its own purpose.

**Instead: Kafka indexes every N bytes.** That is, after a configurable number of bytes have been written to the log file, it creates **one** index row. This is controlled by an `index interval bytes` setting.

> "Not every offset is indexed. Instead, it indexes every N bytes. This saves memory and also helps in faster lookup."

So when a consumer asks for offset 550, Kafka checks the index, finds the *nearest* indexed offset at or before 550 (say, offset 450 at byte position 13200), jumps to that byte position in the log file, and then does a much shorter sequential scan from there to reach the exact offset.

**Two levels of efficiency, stacked:**
1. Jump to the correct **segment** (avoid scanning unrelated segments)
2. Jump to the **nearest indexed byte position** within that segment (avoid scanning the whole segment from byte 0)

---

### Worked example (the instructor's full byte-math walkthrough)

**Setup — Partition0 of order-events, events with the following sizes:**

| Offset | Event size |
|---|---|
| 0 | 300 bytes |
| 1 | 500 bytes |
| 2 | 1000 bytes |
| 3 | 2000 bytes |
| 4 | 500 bytes |

**Configuration:**
- Segment size: 1 GB
- Index interval bytes: **4096 bytes** (i.e., after this many bytes are written to the log, create one index entry)

**Walking through how the log file fills up (Segment1: `000000.log`):**

```
Offset 0: size 300 bytes   → starts at file position 0
Offset 1: size 500 bytes   → starts at file position 300     (0 + 300)
Offset 2: size 1000 bytes  → starts at file position 800     (300 + 500)
Offset 3: size 2000 bytes  → starts at file position 1800    (800 + 1000)
Offset 4: size 500 bytes   → starts at file position 3800    (1800 + 2000)
```

**Total bytes written so far:** 300 + 500 + 1000 + 2000 + 500 = **4300 bytes**

Since 4300 bytes > the configured index interval of 4096 bytes, Kafka now creates **one index entry**:

```
Segment1: 000000.index

| Offset | Physical position (bytes) |
|--------|---------------------------|
|   4    |          3800             |
```

Notice: only **one** entry got created here (for offset 4), not one for every offset 0-4. That's the "index every N bytes, not every offset" rule in action — exactly what saves the index file from bloating.

```
Topic: order-events
    └──► Partition0: order-events-0
              ├──► Segment1: 000000.log     [offset0: 300b][offset1: 500b][offset2: 1000b][offset3: 2000b][offset4: 500b]
              └──► Segment1: 000000.index   [offset4 → position 3800]
```

---

### Recap

| Concept | Purpose |
|---|---|
| **Segments** | Split one partition's log into smaller chunks (named by starting offset) so Kafka can jump directly to the relevant chunk instead of scanning one giant file |
| **Index file** | Per-segment file mapping offset → byte position, but only every N bytes (not every offset) — so lookups within a segment are fast without bloating the index itself |

---
## Step 4: Partitioning Algorithms + Broker


### The question: how does a producer decide which partition an event goes to?

Now that we understand topics have multiple partitions, the natural next question the instructor raises is: when a producer publishes Event0, Event1, Event2... how does Kafka decide *which* partition each one lands in? He lays out **three algorithms**.

---

### 1. Key-based Partitioning

**Rule: all events with the same key always go to the same partition.**

When publishing, the producer sends a message with three parts:

```
Publish:
{
  "order-events",     ← topic
  orderId,             ← key
  orderJson            ← event/value
}
```

**How the partition is computed:**

```
partition = hash(key) % num_partitions
```

Instructor's worked example (3 partitions):

```
E0: hash("123") % 3 = Partition 1
E1: hash("456") % 3 = Partition 2
E2: hash("789") % 3 = Partition 0
```

```
Topic name = order-events

P0   [ E2 ]
P1   [ E0 ]
P2   [ E1 ]
```

**Why this matters:** since the same key always hashes to the same partition, and we know order is guaranteed *within* a partition (from Step 2) — this means **all events sharing a key are guaranteed to be processed in order relative to each other.** This is the classic reason to use key-based partitioning: e.g., using `orderId` as the key ensures all events for that specific order land in the same partition and are read in the exact sequence they were written.

---

### 2. Round Robin (no key)

If the producer publishes without providing a key (`key = null`), Kafka simply cycles through partitions one after another:

```
Publish:
{
  "order-events",   ← topic
  null,             ← key (none)
  orderJson         ← event
}

E0 → Partition 0
E1 → Partition 1
E2 → Partition 2
E3 → Partition 0   (cycle repeats)
...
```

**Trade-off:** you get **even distribution** across partitions, but **ordering is not guaranteed** — related events (e.g., two events for the same order) could land in completely different partitions and be processed out of relative sequence.

---

### 3. Custom Partitioning

You can also write your own routing logic based on business rules. Instructor's example:

```
if country == "India" → Partition 0
if country == "US"    → Partition 1
```

This gives full control over where specific categories of events land — useful when you have a business reason to group certain events together (e.g., by region) rather than by a simple key hash. The instructor notes that when they get to actual implementation later in the series, they'll show how to write this custom partitioning logic in code.

---

### Recap: the three partitioning strategies

| Strategy | Ordering guarantee | Distribution |
|---|---|---|
| **Key-based** | Guaranteed for events with the same key (same key → same partition) | Depends on key distribution |
| **Round Robin** (no key) | Not guaranteed | Even across partitions |
| **Custom** | Depends on your logic | Depends on your logic |

---

### Broker

**Definition:** A broker is a **single Kafka server instance.** It's the one that actually **stores data and serves clients** (both producers and consumers) — i.e., it's the actual server that hosts the partitions we've been discussing.

```
                    ┌─────────────────────┐
┌───────────┐       │    Kafka Broker     │
│ Producer1 │──────►│  Topic: order-events│
└───────────┘       │   P0  P1  P2        │
                    └─────────────────────┘
```

**The important, slightly non-obvious rule:**

> **A broker stores *some* partitions of *some* topics — not everything.**

The instructor spells this out carefully because it's easy to assume a single broker holds all data for a topic. It does not:
- It does **not** hold every topic that exists in the Kafka setup.
- Even for the topics it *does* hold, it does **not** necessarily hold *all* of that topic's partitions.

Example to make this concrete:

```
Topic1: order-events    (partitions: P0, P1, P2)
Topic2: sales-events     (partitions: P0, P1)
Topic3: product-events   (partitions: P0, P1, P2)
```

A single broker might only be responsible for, say, `order-events` P0 and P1, plus `sales-events` P0 — not the rest. In other words:

> **Topics and partitions are distributed across multiple brokers.**

The instructor deliberately does **not** explain *where* the remaining topics/partitions live yet — he flags that this will be covered when discussing the **Kafka cluster** (multi-broker setup), which is content for the next part of this series, not Part 1.

```
┌───────────────────────────────────────────────────────┐
│                  Kafka Cluster (later topic)          │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐          │
│   │ Broker 1 │   │ Broker 2 │   │ Broker 3 │  ...     │
│   └──────────┘   └──────────┘   └──────────┘          │
│   (each holds only some partitions of some topics)    │
└───────────────────────────────────────────────────────┘
```

---

## End of Part 1 — Full Summary

| Concept | Core takeaway |
|---|---|
| **Kafka** | Distributed event streaming platform — publish, store, subscribe/consume events |
| **Producer** | Publishes events; doesn't care who consumes them |
| **Topic** | Logical folder/category; doesn't store data itself; many producers can write to one topic |
| **Partition** | Physical (on disk), Ordered (per-partition offsets, order guaranteed *within* not *across* partitions), Append-only (always written to the end) |
| **Segments** | A partition's log file is split into smaller chunks named by starting offset, so lookups can jump to the right chunk instead of scanning everything |
| **Index files** | Per-segment offset → byte-position lookup, indexed every N bytes (not every offset) for fast, memory-efficient searching |
| **Partitioning algorithms** | Key-based (ordering guaranteed per key), Round Robin (even spread, no ordering), Custom (business-rule driven) |
| **Broker** | A single Kafka server; stores only some partitions of some topics — not everything |

**Interview-ready one-liners from this lecture:**
- *"Kafka guarantees ordering within a partition, not globally across partitions of a topic."*
- *"Segments and index files exist so Kafka doesn't have to scan an entire massive log file just to find one offset."*
- *"A broker does not hold all topics or all partitions of a topic — that's distributed across the cluster."*

---

That wraps up everything taught in this video (he explicitly stops before Consumer & Consumer Groups, which is Part 2). Ready whenever you want to move to the next lecture — just drop the transcript + notes for Part 2.