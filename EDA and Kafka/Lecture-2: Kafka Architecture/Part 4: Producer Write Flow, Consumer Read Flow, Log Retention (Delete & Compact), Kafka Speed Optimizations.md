# Part 4: Producer Write Flow, Consumer Read Flow, Log Retention (Delete & Compact), Kafka Speed Optimizations

Select: Done

# 🎯Step 1: Complete Producer Write Flow

---

### Why this section exists (instructor's framing)

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image.png)

Up to this point, Producer, Topic, Partition, Segments, Broker, Cluster, Controller, KRaft, and ISR have all been covered as **separate pieces**. Now the instructor stitches everything together into one continuous, end-to-end flow: *what actually happens, step by step, from the moment a producer creates an event to the moment it gets confirmed as "published successfully."*

**Starting assumption for this walkthrough:** the topic is already created (via the Controller), and cluster metadata has already been propagated to all brokers — so this flow picks up from a steady-state cluster.

---

### The full flow, step by step

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%201.png)

**Step 1 — Producer creates the event**

The producer prepares the publish message with three things: the **topic**, the **key**, the **value**, and this time also specifies **`ack = all`** — which, as covered in Part 3, means the producer wants to wait for every replica currently in the ISR before considering the write successful.

```
Publish:
{
  "order-events",      ← topic
  orderId,              ← key
  orderJson,            ← value
  ack = all              ← acknowledgment level
}
```

---

**Step 2 — Producer fetches cluster metadata**

Before the producer can send anything, it needs to know **which broker to actually talk to** — specifically, which broker holds the **leader** replica of the target partition (only the leader can accept writes).

So the producer asks **any broker**: *"give me the metadata."*

- If that broker already has current metadata cached (since the Controller pushes metadata updates to all brokers), it responds directly.
- If its metadata is stale/expired, it forwards the request to the **active Controller**, gets the fresh metadata back, and passes it along to the producer.

```
Producer
   │  "give me metadata for order-events"
   ▼
Any Broker
   │
   ├── has fresh metadata? → respond directly
   │
   └── stale/expired? → forward to Active Controller
                              │
                              ▼
                    Controller returns metadata
                              │
                              ▼
                    Broker relays it to Producer
```

The metadata tells the producer: for this topic, this partition, **which broker holds the leader replica.**

---

**Step 3 — Producer calculates the partition**

Using the familiar formula from Part 1:

```
partition = hash(key) % num_partitions
```

Worked example: with 3 partitions, the hash computation lands on **Partition 1.**

---

**Step 4 — Producer identifies the leader broker for that partition**

From the metadata fetched in Step 2, the producer now looks up: *"who is the leader of Partition 1?"* Say the answer is **Broker 2.**

```
Metadata says: order-events, Partition 1 → Leader = Broker 2
```

---

**Step 5 — Producer sends the write request directly to Broker 2**

The producer doesn't go through any intermediary — it invokes Broker 2 directly, since Broker 2 is the leader and only the leader handles writes.

---

**Step 6 — Broker 2 writes the event to Partition 1's log**

The event lands in Partition 1's active segment, gets the next incremental offset — say **offset 101** — and is appended there (same append-only mechanics from Part 1).

```
Topic: order-events
  └──► Partition 1 (hosted on Broker 2, as Leader)
           └──► Segment (active)
                    └──► New event written at Offset 101
```

---

**Step 7 — Broker 2 checks the ack setting and waits on ISR**

Since the producer specified `ack = all`, Broker 2 doesn't respond yet. It checks the current **ISR list** for Partition 1 — say `ISR = [Broker 1, Broker 2, Broker 3]` (Broker 2 is the leader, Broker 1 and Broker 3 are followers).

Broker 2 (the leader) has already written the event. Now it **waits** for Broker 1 and Broker 3 to each **poll** the new event, replicate it, and acknowledge back.

```
ISR = [Broker 1, Broker 2, Broker 3]

Broker 2 (Leader): writes offset 101 → done
        │
        ▼
Waits for followers to catch up:
   Broker 1 (Follower): polls → replicates offset 101 → ACKs
   Broker 3 (Follower): polls → replicates offset 101 → ACKs
        │
        ▼
Once ALL ISR members have acknowledged →
Broker 2 responds to Producer: "event published successfully"
```

---

### Full flow diagram (end to end)

```
Producer
   │ Step 1: Create event (topic, key, value, ack=all)
   ▼
Step 2: Fetch metadata ──► Any Broker ──(if stale)──► Active Controller
   │                                                        │
   │◄───────────────────── metadata returned ───────────────┘
   ▼
Step 3: Calculate partition = hash(key) % num_partitions  →  Partition 1
   ▼
Step 4: From metadata, find leader of Partition 1  →  Broker 2
   ▼
Step 5: Send write request directly to Broker 2
   ▼
Step 6: Broker 2 writes event → Partition 1 log → Offset 101
   ▼
Step 7: ack=all → wait for ISR [Broker 1, Broker 2, Broker 3]
           Broker 1 polls + replicates + ACKs
           Broker 3 polls + replicates + ACKs
   ▼
All ISR acknowledged → Broker 2 tells Producer: "Published successfully"
```

---

### Recap of Step 1

| Stage | What happens |
| --- | --- |
| **Event creation** | Producer prepares topic, key, value, and ack level |
| **Metadata fetch** | Producer asks any broker → broker asks Controller if stale → metadata returned |
| **Partition calculation** | `hash(key) % num_partitions` decides the target partition |
| **Leader lookup** | From metadata, producer finds which broker holds the leader replica |
| **Write request** | Producer sends directly to the leader broker (never to a follower) |
| **Write + offset assignment** | Leader appends the event to its partition log with the next incremental offset |
| **ISR wait (if ack=all)** | Leader waits for every replica in the ISR to replicate and acknowledge before confirming success to the producer |

---

# 🎯Step 2: Complete Consumer Read Flow

---

### Why this flow looks similar to the producer flow

Just like the producer, a consumer is also just a **client** — all the real work (partition assignment, offset tracking, leader lookups) is handled by the Kafka **server** (brokers). The instructor walks through this end-to-end, introducing one new named concept along the way: the **Group Coordinator.**

---

### The full flow, step by step

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%202.png)

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%203.png)

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%204.png)

**Step 1 — Consumer starts and wants to join a group**

Say a consumer wants to join the group `notification-service`.

---

**Step 2 — Find the partition number for this group (within `_consumer_offsets`)**

Recall from Part 2: `_consumer_offsets` has 50 partitions by default, and each consumer group's data lives in exactly one of those partitions, computed via:

```
partition = hash(group.id) % 50
```

Worked example: `hash("notification-service") % 50 = 23` → this group's information is always handled by **Partition 23** of `_consumer_offsets`.

**Why this matters:** in a multi-broker cluster, some broker holds the **leader** replica of Partition 23 — and the consumer needs to know exactly which broker that is, since only the leader can serve this request.

---

**Step 3 — Consumer fetches cluster metadata**

Same mechanism as the producer: the consumer asks **any broker** for metadata. If that broker's cached metadata is stale, it forwards the request to the **active Controller**, gets the fresh response, and relays it back.

```
Consumer
   │  "give me metadata"
   ▼
Any Broker ──(if stale)──► Active Controller
   │◄──────────── metadata returned ─────────┘
   ▼
Consumer now knows: _consumer_offsets Partition 23 → Leader = Broker 3
```

This is genuinely **cluster metadata** — which broker holds which partition as leader/follower — so it's the Controller's job to be the source of truth for it, exactly as with the producer flow.

---

**Step 4 — Consumer invokes the leader broker (the "Group Coordinator")**

Since only a leader replica can serve reads/writes, the consumer must talk to whichever broker holds the **leader** of Partition 23 — say, **Broker 3.**

This is exactly why that broker earns a special name for this group: the **Group Coordinator.** Because the formula `hash(group.id) % 50` always resolves to the same partition for a given group, requests for the `notification-service` group will *always* land on the same broker — Broker 3, in this case.

> **Partition, here, is really just a mechanism for spreading load across multiple servers** — and "Group Coordinator" is just the name given to whichever broker happens to hold the leader of the partition responsible for a specific group's data.
> 

---

**Step 5 — Group Coordinator updates its consumer metadata (adds the consumer to the group)**

Important distinction the instructor draws here: **consumer group membership info is *not* cluster metadata** — it never goes through the Controller. It's stored entirely within the `_consumer_offsets` topic itself.

So Broker 3 (the Group Coordinator) updates its own record: *"This new consumer — say, Consumer 3 — is now part of the `notification-service` group, and it's being assigned Partition 2 (of the actual `order-events` topic)."*

**Replication note:** even though a consumer doesn't specify any `ack` level (there's no such setting for consumers), this membership update still behaves **as if `ack = all`** — the Group Coordinator waits for all the replicas of `_consumer_offsets` Partition 23 (i.e., the followers backing up Broker 3) to also acknowledge this update before considering it done.

```
Broker 3 (Group Coordinator, leader of _consumer_offsets P23)
   │
   │ Adds Consumer 3 to "notification-service" group
   │ Assigns it Partition 2 of order-events
   ▼
Waits for all replicas of _consumer_offsets P23 to acknowledge
   (same behavior as ack=all, even though consumer sets no ack config)
```

---

**Step 6 — Consumer fetches the last committed offset**

Now that Consumer 3 knows it owns Partition 2, it needs to know **where to resume from** — a previous consumer may have crashed partway through.

So it goes back to the **same Group Coordinator** (Broker 3, via the same `hash(group.id) % 50 = 23` lookup) and asks: *"For `order-events`, Partition 2 — what's the last committed offset for this group?"*

The coordinator looks this up by key `(group.id, topic, partition)` inside `_consumer_offsets`, and returns the stored value — say, **offset 101.**

---

**Step 7 — Consumer fetches actual data from the real partition's leader**

This is a different broker/partition than the coordinator lookup — now the consumer needs the actual **event data** from `order-events`, Partition 2. From the metadata already fetched, it knows the leader of this partition is, say, **Broker 2.**

The consumer calls Broker 2 directly: *"Fetch me starting from offset 101, and give me up to 200 bytes"* (the amount is configurable, based on available memory).

Broker 2 returns events sequentially — from offset 101 up to, say, offset 501 (however many events fit inside 200 bytes).

```
Consumer ──"fetch from offset 101, max 200 bytes"──► Broker 2 (Leader, order-events P2)
                                                              │
                                                              ▼
                                                  Returns events: offset 101 → 501
```

---

**Step 8 — Consumer processes and commits (manual, batch-wise)**

Following the manual batch-commit strategy from Part 2: once the whole batch is processed, the consumer commits.

To commit, it goes back to the **Group Coordinator** (Broker 3) once more, and writes into `_consumer_offsets` Partition 23: *"This group has now processed up to offset 501 for `order-events` Partition 2."*

---

**Step 9 — The loop continues (poll-based, not push-based)**

Once committed, the consumer simply repeats Step 7 — asking Broker 2 for the next 200 bytes, starting from offset 502. This is the instructor's explicit callout: **Kafka consumers are poll-based, not push-based** — the consumer keeps actively asking for more, rather than the broker pushing data at it.

---

### Full flow diagram (end to end)

```
Consumer wants to join group "notification-service"
   │
Step 2: hash(group.id) % 50 = 23  → this group's data lives in _consumer_offsets P23
   ▼
Step 3: Fetch cluster metadata ──► Any Broker ──(if stale)──► Active Controller
   │◄────────────── metadata: P23 leader = Broker 3 ─────────────┘
   ▼
Step 4: Invoke Broker 3 (the "Group Coordinator" for this group)
   ▼
Step 5: Coordinator adds Consumer 3 to group, assigns Partition 2 of order-events
           (waits for _consumer_offsets P23's replicas to ack, like ack=all)
   ▼
Step 6: Consumer asks Coordinator: "last committed offset for P2?" → 101
   ▼
Step 7: Consumer asks Broker 2 (leader of order-events P2):
           "fetch from offset 101, 200 bytes" → returns offsets 101–501
   ▼
Consumer processes the batch
   ▼
Step 8: Consumer commits: tells Coordinator (Broker 3) → "processed till 501"
   ▼
Step 9: Loop back to Step 7 → fetch from offset 502, repeat forever (poll-based)
```

---

### Recap of Step 2

| Stage | What happens |
| --- | --- |
| **Join group** | Consumer requests to join a named group (e.g., `notification-service`) |
| **Find coordinator** | `hash(group.id) % 50` determines which `_consumer_offsets` partition — and therefore which broker (the **Group Coordinator**) — handles this group |
| **Metadata fetch** | Same broker→Controller fallback mechanism as the producer, since this is cluster metadata |
| **Partition assignment** | Group Coordinator assigns the new consumer a partition of the real topic, replicated like `ack=all` even though consumers set no ack config |
| **Last offset lookup** | Consumer asks the coordinator for the last committed offset, to know where to resume |
| **Actual data fetch** | Consumer asks the **leader of the real topic-partition** (a different broker than the coordinator) for the next batch of events |
| **Manual commit** | After processing a batch, consumer commits progress back to the coordinator |
| **Poll loop** | Kafka consumers are poll-based — they continuously ask for more data rather than having it pushed to them |

---

### A key clarification worth holding onto

**Cluster metadata** (which broker holds which topic-partition as leader/follower) always goes through the **Controller**.
**Consumer group metadata** (group membership, partition assignment, committed offsets) is stored entirely within `_consumer_offsets` and never touches the Controller.

Both flows still rely on the same underlying idea, though: **a partition is just a way of distributing load across multiple brokers** — whether that partition belongs to `order-events` or to the internal `_consumer_offsets` topic.

---

# 🎯Step 3: Log Retention Policies — Delete vs. Compact

---

### Why this comes up now

Kafka keeps appending events to partition logs forever (as covered in Part 1) — but disks aren't infinite. So the natural question: **when and how does old data get removed?** Kafka gives you two configurable cleanup policies, and — important detail the instructor stresses — **both operate at the segment level, not the event level.**

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%205.png)

```
cleanup.policy = delete    (default)
cleanup.policy = compact
```

---

### Policy 1: Delete (default)

Old **segments** get deleted based on either **time** or **size** — both can be configured together.

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%206.png)

**Time-based retention:**

```
retention.ms = 7 days (in milliseconds)
```

**Rule:** a segment gets deleted once it's older than the configured retention period — but this check happens **per segment**, not per event.

**Worked example:**

```
Segment created 8 days ago  → exceeds 7-day retention → DELETED
Segment created 6 days ago  → within 7-day retention  → KEPT
```

```
Partition (order-events-0)
   ├── Segment1 (created 8 days ago)  ✗ DELETED (past retention.ms)
   ├── Segment2 (created 6 days ago)  ✓ KEPT
   └── Segment3 (active, still being written)  ✓ KEPT
```

**Size-based retention:**

```
retention.bytes = 1 GB (maximum total size)
```

**Worked example:**

```
Segment1: 1 GB
Segment2: 200 MB
Total: 1.2 GB  → exceeds the 1 GB limit
```

**Rule:** when the total size of a partition's segments exceeds the configured limit, Kafka deletes the **oldest segment** first to bring the total back under the limit.

```
Partition total = 1.2 GB > 1 GB limit
   → delete the OLDER segment (Segment1, 1 GB)
   → remaining size now under the 1 GB cap
```

**Key takeaway:** whether it's time-based or size-based, the **unit of deletion is always a whole segment** — never an individual event within a segment.

---

### Policy 2: Compact

**`cleanup.policy = compact`** works completely differently — instead of deleting based on age or size, it removes **outdated values for the same key**, keeping only the **latest value per key.**

**Important detail:** compaction is an **asynchronous background job** — it does not run synchronously, and it does not block writes. It runs per segment, in the background, so immediate cleanup after a new write doesn't happen right away.

**Worked example (instructor's key-based walkthrough):**

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%207.png)

Say a segment currently holds these records (each with a key and a value):

```
Offset 100:  key = user1,  value = v1
Offset 101:  key = user2,  value = v1
Offset 102:  key = user1,  value = v2     ← duplicate key (user1)
Offset 103:  key = user3,  value = v1
Offset 104:  key = user2,  value = v2     ← duplicate key (user2)
```

Here, `user1` appears twice (v1, then v2) and `user2` appears twice (v1, then v2). `user3` appears only once.

**After the background compaction job runs**, only the **latest value per key** survives:

```
Offset 102:  key = user1,  value = v2   (v1 removed — outdated)
Offset 103:  key = user3,  value = v1   (only entry, kept as-is)
Offset 104:  key = user2,  value = v2   (v1 removed — outdated)
```

```
Before compaction:
[user1:v1][user2:v1][user1:v2][user3:v1][user2:v2]

After compaction (background job, async):
[user1:v2][user3:v1][user2:v2]
   ↑ only the latest value per key survives
```

**Why compaction matters:** it's useful when you only care about the **current state** of something identified by a key (e.g., a user's latest profile snapshot) rather than the full history of changes to it.

---

### Delete vs. Compact — Side by Side

| Aspect | Delete (default) | Compact |
| --- | --- | --- |
| **What it removes** | Entire old segments (time or size based) | Outdated values for duplicate keys within a segment |
| **Granularity** | Whole segment | Per-key, within a segment |
| **Trigger** | `retention.ms` and/or `retention.bytes` | Runs as a background async job |
| **Synchronous?** | N/A (just age/size check) | No — async, doesn't block writes; immediate cleanup doesn't happen |
| **Use case** | "I don't need data older than X time / beyond X size" | "I only care about the latest value per key" |

---

### Recap of Step 3

| Concept | Core takeaway |
| --- | --- |
| **`cleanup.policy = delete`** | Default policy — deletes whole segments based on `retention.ms` (time) or `retention.bytes` (size) |
| **Segment-level operation** | Both `delete` and `compact` always act on entire segments, never individual events |
| **`cleanup.policy = compact`** | Keeps only the latest value per key, discarding older duplicates for the same key |
| **Async nature of compaction** | Runs as a background job, doesn't block writes, so cleanup isn't instantaneous |

---

Ready for Step 4 (Why Kafka is Fast — page cache + sequential writes, zero-copy reads) whenever you say the word.

# 🎯Step 4: Why Kafka Is So Fast (Page Cache + Sequential Writes, Zero-Copy Reads)

---

### The question this section answers

Kafka is constantly reading from and writing to disk — and disk I/O is traditionally considered slow. So how does Kafka achieve such high throughput? The instructor breaks the answer into two parts: **optimization at write time** and **optimization at read time.**

---

### Write-time optimization: OS Page Cache + Sequential Writes

**1. Page Cache Optimization**

![image.png](Part%204%20Producer%20Write%20Flow,%20Consumer%20Read%20Flow,%20Lo/image%208.png)

Instead of directly writing every event straight to disk, Kafka **trusts the operating system's caching strategy.** When the leader broker writes an event:

- It first writes into **RAM — specifically, the OS page cache.**
- The actual flush to disk happens **asynchronously**, in the background.

```
Producer writes event
       │
       ▼
Leader Broker
       │
       ▼
Written to RAM (OS Page Cache)  ← fast, no disk wait here
       │
       ▼ (async, in background)
Eventually flushed to Disk
```

**Why this matters:** the write path doesn't have to **block waiting on a physical disk write** — it only needs to land in memory first, which is dramatically faster. The disk write itself happens later, asynchronously.

---

**2. Sequential Writes**

Whenever data *is* written to disk, it happens in **sequential** fashion — and sequential disk writes are the **best-case scenario** for disk I/O performance (much faster than random-access writes).

**Why Kafka naturally gets sequential writes for free:** this ties directly back to the **append-only** log property from Part 1 — Kafka never inserts in the middle of a file and never modifies existing records in place. Every new event simply gets the next incremental offset and is appended to the end.

```
Append-only log:
[offset 32][offset 33][offset 34]... → always writing at the end

This means: every write = sequential write = disk-friendly
```

**Recap — Write path:**

```
Event arrives
   │
   ▼
Write to OS Page Cache (RAM) — no blocking on disk
   │
   ▼
Async flush to disk — always sequential (append-only) → best case for disk I/O
```

---

### Read-time optimization: Zero-Copy (via `sendfile`)

**The problem with a "normal" (non-optimized) read path:**

Without zero-copy, reading a file to send over the network typically takes multiple hops:

```
Disk → OS Page Cache (RAM) → Application's User Space (JVM, for Kafka) → Network
```

Each of those hops costs time, memory, and CPU cycles — the data literally gets copied from one memory location to another at each step.

**Kafka's fix: the `sendfile` system call.**

Instead of loading data into its own application memory (JVM/user space) before sending it out, Kafka makes a direct **system call** that lets data flow **straight from the OS page cache to the network** — skipping the JVM/user-space copy entirely.

```
WITHOUT zero-copy:
Disk → OS Page Cache → Kafka JVM (User Space) → Network
                              ↑
                     extra copy + extra hop (slow)

WITH zero-copy (sendfile):
Disk → OS Page Cache ─────────────────────────► Network
                     (Kafka JVM never touches the data)
```

**Why this matters:** by skipping the user-space copy, Kafka saves:

- **Time** (one fewer hop)
- **Memory** (no duplicate copy sitting in JVM heap)
- **CPU cycles** (no extra copy operation)

...which together translate into significantly **higher throughput** on reads.

---

### Recap of Step 4

| Optimization | When it applies | Core mechanism |
| --- | --- | --- |
| **OS Page Cache** | Write time | Events are written to RAM first; disk flush happens asynchronously — no blocking on disk |
| **Sequential Writes** | Write time | Append-only design means every write lands at the end of the file — the best case for disk I/O |
| **Zero-Copy (`sendfile`)** | Read time | Data flows directly from OS page cache to the network, skipping the JVM/application memory hop entirely |

**Interview-ready takeaway:** *"Kafka is fast because it avoids blocking disk writes by using the OS page cache and always writes sequentially due to its append-only design — and on reads, it uses zero-copy via the `sendfile` system call to skip the application-memory hop entirely."*

---

## End of Part 4 — Full Summary

| Section | Core Idea |
| --- | --- |
| **Producer Write Flow** | Event created → metadata fetched (broker → Controller if stale) → partition calculated via `hash(key) % partitions` → leader broker located → write sent directly to leader → offset assigned → if `ack=all`, wait for full ISR to replicate before confirming success |
| **Consumer Read Flow** | Consumer joins group → group's coordinator found via `hash(group.id) % 50` → metadata fetched → coordinator assigns partition (replicated like ack=all) → last committed offset fetched from coordinator → actual data fetched from the real partition's leader → processed → manually committed back to coordinator → poll loop continues |
| **Cluster metadata vs. group metadata** | Cluster metadata (leader/follower info) always flows through the Controller; consumer group metadata (membership, offsets) lives entirely in `_consumer_offsets`, never touching the Controller |
| **Retention: Delete** | Default policy — deletes whole segments based on `retention.ms` (time) or `retention.bytes` (size) |
| **Retention: Compact** | Async background job — keeps only the latest value per key within a segment, discards older duplicates |
| **Speed — Write path** | OS page cache (async disk flush, no blocking) + sequential writes (thanks to append-only design) |
| **Speed — Read path** | Zero-copy via `sendfile` — data flows disk → page cache → network directly, skipping the JVM/user-space hop |

**Interview-ready one-liners from this lecture:**

- *"A producer always writes to the leader broker directly — never a follower — and with `ack=all`, it waits until every ISR replica has acknowledged before the write is considered successful."*
- *"The 'Group Coordinator' is just the broker that holds the leader replica of the `_consumer_offsets` partition responsible for a given consumer group — found via `hash(group.id) % 50`."*
- *"Consumer group metadata (membership, offsets) is stored in `_consumer_offsets` and never touches the Controller — only true cluster metadata (leader/follower assignments) does."*
- *"Both delete and compact cleanup policies operate at the segment level, never on individual events."*
- *"Kafka's speed comes from avoiding blocking disk writes via the OS page cache, always writing sequentially (append-only), and using zero-copy (`sendfile`) on reads to skip the JVM memory hop."*

---

That wraps up everything taught in this lecture — the instructor explicitly stops here and defers **failure scenarios** to the next part.

**Suggested file name**, consistent with your naming convention:

> **Part 4: Producer Write Flow, Consumer Read Flow, Log Retention (Delete & Compact), Kafka Speed Optimizations**
> 

Ready whenever you have the transcript for Part 5 (the final lecture in this Kafka Architecture series, covering failure scenarios).