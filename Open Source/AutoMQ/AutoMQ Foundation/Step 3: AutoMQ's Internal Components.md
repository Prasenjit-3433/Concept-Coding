# Step 3: AutoMQ's Internal Components — The Engine Room

In Step 2 we saw the big picture: brokers are stateless, S3 is the source of truth, WAL buffers writes. Now let's open the hood and understand the actual internal components — because this is where your 4 issues live.

---

## 3.1 The Problem With Just "Using S3"

Before we look at components, understand WHY AutoMQ needed to build custom internals.

```
Kafka's storage model:
──────────────────────

Topic: orders    Partition 0          Partition 1
                 ┌─────────────┐      ┌─────────────┐
                 │ segment-0   │      │ segment-0   │
                 │ segment-1   │      │ segment-1   │
                 │ segment-2   │      │ segment-2   │
                 └─────────────┘      └─────────────┘
                 
Each segment = a plain file on local disk.
Kafka just appends to files. Simple.

─────────────────────────────────────────────────────

Now try to map this to S3:
──────────────────────────

Problem 1: S3 objects are IMMUTABLE
  → You can't append to an S3 object
  → Kafka constantly appends to segment files
  → These two are fundamentally incompatible

Problem 2: Partition = fixed assignment to one broker
  → In Kafka, partition-0 always belongs to Broker 1
  → In AutoMQ, Broker 1 might die and Broker 2 takes over
  → The storage layer must support this flexibility

Problem 3: Kafka's Log class assumes LOCAL filesystem
  → It uses FileChannel, memory-mapped files etc.
  → All of this breaks when storage is S3

Conclusion: AutoMQ couldn't just "point Kafka at S3"
They had to rebuild the entire storage layer.
```

---

## 3.2 The Stream — AutoMQ's Core Abstraction

AutoMQ invented a new primitive called a **Stream**. This is the most important concept to understand.

```
What is a Stream?
─────────────────────────────────────────────────────

A Stream is an append-only, infinitely growing
sequence of records stored in S3.

Think of it like this:

Stream #42:
┌─────────────────────────────────────────────────────────┐
│  offset 0  │  offset 1  │  offset 2  │  offset 3  │...
│  [record]  │  [record]  │  [record]  │  [record]  │
└─────────────────────────────────────────────────────────┘
      ▲                                        ▲
   start                                   latest

Properties:
  ✅ Append-only (just like Kafka log)
  ✅ Identified by a numeric Stream ID
  ✅ Not tied to any specific broker
  ✅ Lives entirely in S3
  ✅ Any broker can open and read it
  ✅ Only ONE broker can write to it at a time
```

Now here's the key mapping:

```
Kafka concept          AutoMQ implementation
─────────────          ─────────────────────

Topic Partition    →   One or more Streams
                       ┌─────────────────────────┐
                       │  Partition 0            │
                       │  ├── Data Stream #42    │ ← actual messages
                       │  └── Meta Stream #43    │ ← partition metadata
                       └─────────────────────────┘

Log Segment        →   S3 Object (immutable chunk)

Segment Index      →   Stream metadata in S3

Log Append         →   Stream append operation
                       (goes through WAL first)
```

**Every Kafka partition in AutoMQ is backed by at least 2 streams — a data stream and a meta stream.**

---

## 3.3 The Four Core Components

AutoMQ's storage engine has 4 main components. Let's place them on a diagram first, then explain each:

```
┌──────────────────────────────────────────────────────────────┐
│                        BROKER                                │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐     │
│  │                   ElasticLog                        │     │
│  │  (replaces Kafka's UnifiedLog / Log class)          │     │
│  │                                                     │     │
│  │  Partition 0's brain:                               │     │
│  │  - Knows which streams belong to this partition     │     │
│  │  - Handles read/write requests from Kafka           │     │ 
│  │  - Manages lifecycle (create, delete, recover)      │     │
│  └──────────────┬──────────────────────────────────────┘     │
│                 │ uses                                       │
│                 ▼                                            │
│  ┌──────────────────────────────────────────────────────┐    │
│  │              Stream Client (ElasticStreamClient)     │    │
│  │                                                      │    │
│  │  API layer for stream operations:                    │    │
│  │  - openStream(streamId)                              │    │
│  │  - append(streamId, records)                         │    │
│  │  - fetch(streamId, offset, maxBytes)                 │    │
│  │  - closeStream(streamId)                             │    │
│  └──────────────┬───────────────────────────────────────┘    │
│                 │ uses                                       │
│                 ▼                                            │
│  ┌──────────────────────────────────────────────────────┐    │
│  │                   WAL                                │    │
│  │         (BlockWALService)                            │    │
│  │                                                      │    │
│  │  Local disk buffer:                                  │    │
│  │  - Receives all writes first                         │    │
│  │  - Gives fast ACK to callers                         │    │
│  │  - Background uploader pushes to S3                  │    │
│  └──────────────┬───────────────────────────────────────┘    │
│                 │ flushes to                                 │
└─────────────────┼────────────────────────────────────────────┘
                  ▼
     ┌────────────────────────┐
     │          S3            │
     │                        │
     │  Stream data objects   │
     │  Metadata objects      │
     └────────────────────────┘
```

Now let's understand each component properly.

---

## 3.4 Component 1: ElasticLog

This is the most important class in AutoMQ. It's the direct replacement for Kafka's `Log` / `UnifiedLog` class.

```
What Kafka's UnifiedLog does:
──────────────────────────────
- Manages a partition's log segments on local disk
- Handles read/write/truncate operations
- Tracks start offset, end offset, log size
- Called by Kafka's ReplicaManager for every produce/fetch

What ElasticLog does (same interface, different internals):
──────────────────────────────────────────────────────────

┌─────────────────────────────────────────────────────┐
│                   ElasticLog                        │
│                                                     │
│  Partition identity:                                │
│  - topicId, partitionId                             │
│  - epoch (changes each time leadership changes)     │
│                                                     │
│  Stream references:                                 │
│  ┌───────────────────────────────────────────┐      │
│  │  streamMap {                              │      │
│  │    DATA   → Stream #42  (messages)        │      │
│  │    OFFSET → Stream #43  (offset index)    │      │
│  │    TIME   → Stream #44  (time index)      │      │
│  │    META   → Stream #45  (kv metadata)     │      │
│  │  }                                        │      │
│  └───────────────────────────────────────────┘      │
│                                                     │
│  Key methods:                                       │
│  - append()    → write to DATA stream via WAL       │
│  - read()      → fetch from DATA stream             │
│  - recover()   → rebuild state from S3 on startup   │
│  - destroy()   → delete all streams from S3         │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**Why is this relevant to your issues?**

```
Issue #1842 — "Cleanup metadata when a topic is deleted"
─────────────────────────────────────────────────────────

The issue says: the Partition → MetaStream KV mapping
is NOT being deleted when a topic is deleted.

Look at ElasticLog.destroy() method (around line 625):

  def destroy(): Unit = {
    // Deletes the DATA, OFFSET, TIME streams ✅
    // But does NOT delete the KV metadata entry
    // that maps this partition to its stream IDs ❌
  }

The KV store here is a separate metadata store that
tracks "partition X uses stream IDs 42, 43, 44, 45"

When destroy() runs, it cleans up the streams themselves
but forgets to remove this index entry.

Result: Stale metadata accumulates over time as
topics are created and deleted repeatedly.

YOUR FIX will be: find where this KV entry is written,
and make destroy() also delete it.
```

This is why we need to understand ElasticLog deeply — your Issue #1842 fix lives here.

---

## 3.5 Component 2: The WAL (BlockWALService)

The WAL is the local fast-write buffer. Let's understand its internals:

```
BlockWALService internal structure:
────────────────────────────────────

Local disk:
┌────────────────────────────────────────────────────┐
│                  WAL File                          │
│                                                    │
│  ┌──────────┬──────────┬──────────┬────────────┐   │
│  │ Block 0  │ Block 1  │ Block 2  │  Block 3   │   │
│  │[records] │[records] │[records] │ [records]  │   │
│  └──────────┴──────────┴──────────┴────────────┘   │
│       ▲                                  ▲         │
│  already flushed                    latest write   │
│  to S3, can be                                     │
│  reclaimed                                         │
└────────────────────────────────────────────────────┘

Write flow:
1. append(records) called
2. Write records to next available Block
3. Immediately return ACK to caller ✅
4. Background thread: batch blocks → upload to S3
5. After S3 upload confirmed: mark blocks as reclaimable
6. Reclaim disk space for new writes (circular buffer)

This is a CIRCULAR buffer on disk.
Total size is fixed and small (e.g. 2-4 GB).
It's constantly being written and reclaimed.
```

**This is relevant to Issue #1578** (WAL graceful shutdown) which you've correctly decided to skip for now. But knowing this helps you understand why that issue is complex.

---

## 3.6 Component 3: The Stream Client

This is the API layer between ElasticLog and the actual S3 storage:

```
ElasticStreamClient — what it manages:
────────────────────────────────────────

┌────────────────────────────────────────────────────┐
│              ElasticStreamClient                   │
│                                                    │
│  Stream Registry:                                  │
│  ┌──────────────────────────────────────────────┐  │
│  │  streamId 42 → open, current epoch: 5        │  │
│  │  streamId 43 → open, current epoch: 5        │  │
│  │  streamId 44 → closed                        │  │
│  └──────────────────────────────────────────────┘  │
│                                                    │
│  Operations:                                       │
│  ┌──────────────────────────────────────────────┐  │
│  │                                              │  │
│  │  openStream(id, epoch)                       │  │
│  │    → validates epoch (prevents split-brain)  │  │
│  │    → loads stream metadata from S3           │  │
│  │    → returns Stream object                   │  │
│  │                                              │  │
│  │  append(streamId, records)                   │  │
│  │    → goes through WAL first                  │  │
│  │    → returns future (async)                  │  │
│  │                                              │  │
│  │  fetch(streamId, startOffset, maxBytes)      │  │
│  │    → checks WAL cache first                  │  │
│  │    → falls back to S3 if not in cache        │  │
│  │                                              │  │
│  └──────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

---

## 3.7 Component 4: Metadata Store (The KV Store)

This is the component most directly relevant to **Issue #1842**. Pay close attention.

```
What metadata does AutoMQ need to track?
──────────────────────────────────────────

For each partition, AutoMQ needs to remember:
"Which stream IDs belong to this partition?"

Without this, when a broker restarts, it wouldn't
know which S3 objects contain this partition's data.

The KV Store:
┌─────────────────────────────────────────────────────┐
│                   KV STORE                          │
│           (stored in S3 as well)                    │
│                                                     │
│  Key                        Value                   │
│  ──────────────────────     ──────────────────────  │
│  "partition/topic-A/0"   →  {dataStream: 42,        │
│                              offsetStream: 43,      │
│                              timeStream: 44,        │
│                              metaStream: 45}        │
│                                                     │
│  "partition/topic-A/1"   →  {dataStream: 46,        │
│                              offsetStream: 47, ...} │
│                                                     │
│  "partition/topic-B/0"   →  {dataStream: 50, ...}   │
│                                                     │
└─────────────────────────────────────────────────────┘

When a partition is created:
  → KV entry is WRITTEN ✅

When a partition/topic is deleted:
  → Streams are deleted ✅  (destroy() does this)
  → KV entry is NOT deleted ❌  (the bug in #1842)

The orphaned KV entry just sits there forever,
pointing to stream IDs that no longer exist.
```

---

## 3.8 How All Components Work Together — Full Flow

Let's trace one complete write from producer to S3:

```
Producer sends: "order-123" to topic orders, partition 0
                         │
                         ▼
              ┌──────────────────┐
              │  Kafka Network   │
              │  Layer           │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │  ReplicaManager  │  ← Standard Kafka component
              │                  │    (mostly unchanged)
              └────────┬─────────┘
                       │ calls append()
                       ▼
              ┌──────────────────┐
              │   ElasticLog     │  ← AutoMQ replaces
              │   (partition 0)  │    UnifiedLog with this
              └────────┬─────────┘
                       │ calls streamClient.append()
                       ▼
              ┌──────────────────┐
              │  Stream Client   │
              │                  │
              └────────┬─────────┘
                       │ writes to WAL first
                       ▼
              ┌──────────────────┐
              │  BlockWALService │  ← Local disk buffer
              │                  │
              │  Write to block  │
              │  ACK immediately │──────────────────────▶ Producer ✅
              └────────┬─────────┘
                       │ background flush
                       ▼
              ┌──────────────────┐
              │       S3         │  ← Final home
              │                  │
              │  batch-0042.obj  │
              └──────────────────┘
```

---

## 3.9 Partition Leadership Change — Why Stateless Matters

Now let's see the most powerful consequence of this design:

```
Scenario: Broker 1 dies mid-operation
──────────────────────────────────────────────────────

KAFKA:
  Broker 1 dies
     → Partition 0 leader is lost
     → Broker 2 (replica) becomes leader
     → Broker 2 already has a COPY of the data
     → (Because it was replicating all along)
     → Takes ~30 seconds for election + stabilization
     → Data in Broker 1's WAL that wasn't replicated = LOST ❌

AUTOMQ:
  Broker 1 dies
     → Partition 0 leader is lost
     → Controller detects failure
     → Assigns Partition 0 leadership to Broker 2
     
  Broker 2 recovery:
  ┌──────────────────────────────────────────────────┐
  │  1. Look up KV store:                            │
  │     "partition/orders/0" → streamIds: 42,43,44   │
  │                                                  │
  │  2. Open Stream #42 with new epoch               │
  │     (epoch prevents Broker 1 zombie writes)      │
  │                                                  │
  │  3. Find last committed offset in stream         │
  │                                                  │
  │  4. Ready to serve reads and writes              │
  │                                                  │
  │  Total time: SECONDS ✅                           │
  └──────────────────────────────────────────────────┘
  
  What about data in Broker 1's WAL that wasn't
  flushed to S3 yet?
  → AutoMQ replicates WAL to at least 2 brokers
    (just the WAL, not all data — tiny amount)
  → So recent unflushed data is also safe ✅
```

---

## 3.10 Connecting Everything to Your 4 Issues

Now let's map each issue to what you've just learned:

```
┌──────────────────────────────────────────────────────────────┐
│              YOUR ISSUES — NOW IN CONTEXT                    │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Issue #1244 — --broker param                                │
│  Component: ProducerPerformance.java (Kafka tool layer)      │
│  AutoMQ internals needed: NONE                               │
│  You're working above the storage layer entirely.            │
│  This is pure Kafka Producer API work. ✅ Easiest             │
│                                                              │
│  Issue #666 — JMX metrics                                    │
│  Component: Metrics/JMX exposure layer                       │
│  AutoMQ internals needed: MINIMAL                            │
│  JMX hooks into Kafka's existing metrics registry.           │
│  Understanding streams/WAL not required. ✅ Easy              │
│                                                              │
│  Issue #835 — OTel logs to server.log                        │
│  Component: Logging initialization                           │
│  AutoMQ internals needed: MINIMAL                            │
│  Find where OTel SDK is initialized, bridge to SLF4J.        │
│  No storage layer involvement. ✅ Easy                        │
│                                                              │
│  Issue #1842 — Metadata cleanup on delete                    │
│  Component: ElasticLog.destroy() + KV Store                  │
│  AutoMQ internals needed: MODERATE                           │
│  You need to understand Stream lifecycle and KV store.       │
│  But the fix itself is small and well-scoped. 🟡 Medium      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Step 3 Summary

```
┌────────────────────────────────────────────────────────────┐
│           AUTOMQ INTERNAL COMPONENTS — SUMMARY             │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Stream      → Core abstraction. Append-only sequence      │
│                in S3. Identified by numeric ID.            │
│                                                            │
│  ElasticLog  → Replaces Kafka's UnifiedLog.                │
│                Owns 4 streams per partition.               │
│                Key class for Issue #1842.                  │
│                                                            │
│  WAL         → Local disk buffer (BlockWALService).        │
│                Fast writes, background S3 flush.           │
│                Key class for Issue #1578 (skip for now).   │
│                                                            │
│  KV Store    → Metadata index: partition → stream IDs.     │
│                Bug in #1842: not cleaned up on delete.     │
│                                                            │
│  Stream      → API layer between ElasticLog and S3.        │
│  Client        Manages stream lifecycle and routing.       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## What's Next

In **Step 4**, we go from concepts directly into the **actual codebase**:
- Where to find each component in the repo
- How to navigate the repo structure without getting lost
- Reading `ProducerPerformance.java` together — the code for Issue #1244
- Setting up your local environment to build AutoMQ

This is where theory ends and hands-on begins. Say **"next"** when ready!