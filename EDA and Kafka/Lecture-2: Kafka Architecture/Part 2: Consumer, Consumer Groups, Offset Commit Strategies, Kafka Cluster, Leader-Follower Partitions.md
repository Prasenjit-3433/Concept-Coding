# Kafka Architecture — Part 2 (Notes)

## Step 1: Consumer and Consumer Group

---

### Recap before starting (instructor's framing)

Part 1 covered: Producer, Topic, Partition (physical/ordered/append-only), Segments, Index files, Partitioning algorithms, and Broker. Now Part 2 picks up with the two remaining core pieces: **Consumer** and **Consumer Group**.

---

### What is a Consumer Group?

A **consumer group** is a named group that a consumer joins in order to read events. You don't just run a "bare" consumer — every consumer belongs to **one of the groups you've created**.

**Example:** Say you've created two groups:
- `notification` group
- `analytics` group

A single consumer can join `notification`, or it can join `analytics` — but it always belongs to **at least one group** (you can also have a consumer that's part of multiple different groups, since group membership is just a config on the consumer).

```
Kafka Broker
┌───────────────────────┐         Consumer Group1 = notification
│ Topic: order-events   │         ┌──────────────┐
│  P0 [        ]        │◄────────┤ Consumer 1   │
│  P1 [        ]        │◄───┐    │   ...        │
│  P2 [        ]        │◄─┐ │    │ Consumer N   │
└───────────────────────┘  │ │    └──────────────┘
                           │ │
                           │ │    Consumer Group2 = analytics
                           │ └────┌──────────────┐
                           │      │ Consumer 1   │
                           └──────┤   ...        │
                                  │ Consumer N   │
                                  └──────────────┘
```

---

### Rule 1: Same partition, same group → only ONE consumer reads it

**The core rule:** within a single consumer group, **no two consumers can read the same partition.** Each partition assigned to a group is owned by exactly one consumer in that group at a time.

But: **different consumers in different groups CAN read the same partition.** That's completely fine.

```
Notification group:  Consumer A reads Partition1   ✅
Analytics group:      Consumer B reads Partition1   ✅ (different group, allowed)

Notification group:  Consumer A reads Partition1
Notification group:  Consumer C also reads Partition1   ❌ NOT allowed (same group)
```

---

### The three consumer-vs-partition scenarios

The instructor walks through three concrete scenarios to make the rule click.

**Scenario 1: Consumers = Partitions**

```
Topic: order-events (3 partitions)
Consumer Group: notification-service (3 consumers)

Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
```
Clean 1:1 mapping — every consumer handles exactly one partition.

**Scenario 2: Consumers < Partitions**

```
Topic: order-events (6 partitions)
Consumer Group: notification-service (3 consumers)

Consumer 1 → Partition 0, Partition 3
Consumer 2 → Partition 1, Partition 4
Consumer 3 → Partition 2, Partition 5
```
Since there are fewer consumers than partitions, **one consumer can take care of more than one partition.** Still respects Rule 1 — no partition is shared between two consumers.

**Scenario 3: Consumers > Partitions**

```
Topic: order-events (3 partitions)
Consumer Group: notification-service (5 consumers)

Consumer 1 → Partition 0
Consumer 2 → Partition 1
Consumer 3 → Partition 2
Consumer 4 → IDLE (no partition)
Consumer 5 → IDLE (no partition)
```
If you have more consumers than partitions, the extra consumers simply sit **idle** — there's nothing left to assign them, since a partition can't be split between two consumers of the same group.

---

### Rule 2: Multiple groups CAN read the same partition independently

```
Topic: order-events

Consumer Group 1: notification-service
    Consumer 1 → Partition 0

Consumer Group 2: analytics-service
    Consumer 2 → Partition 0

Consumer Group 3: audit-service
    Consumer 3 → Partition 0
```

All three groups can independently read Partition 0 — the same events get delivered to each group, each processing them for its own purpose (one for notifications, one for analytics, one for auditing). This is what makes Kafka's streaming model powerful: one event, many independent consumers, each moving through the log at their own pace.

---

### Recap of Step 1

| Concept | Core rule |
|---|---|
| **Consumer Group** | A named group a consumer joins; consumer must belong to at least one group |
| **Rule 1** | Within one group, no two consumers read the same partition — a consumer *can* own multiple partitions if consumers < partitions; extra consumers go idle if consumers > partitions |
| **Rule 2** | Different groups can independently read the same partition — group membership is what isolates read responsibility |

---

## Step 2: Offset Tracking — The `_consumer_offsets` Topic

---

### Why offsets need to be stored per group

Each consumer group maintains its own **partition-wise offset**, completely independent of other groups. This is the mechanic that makes the previous section's "multiple groups reading the same partition" actually work — since each group tracks its own progress separately, one group being ahead or behind doesn't affect another.

**Concrete example:** say within the `notification` group:
- Consumer 1 reads Partition 0 of topic `order-events` → has processed up to offset 500
- Consumer 3 reads Partition 1 of topic `order-events` → has processed up to offset 300

This "processed till here" information needs to be **stored somewhere**, so that if a consumer crashes and a new one takes over, it knows exactly where to resume.

```
(Consumer Group, Topic, Partition) → Committed Offset (processed up to this offset)

notification   order-events   0   105
notification   order-events   1   98
analytics      order-events   0   60
analytics      order-events   1   45
```

**Important nuance the instructor stresses:** this information is stored **group-wise, not consumer-wise.** It's *not* "Consumer 1 of notification group has processed offset 500" — it's "the `notification` group, for this topic, for this partition, has processed offset 500." Why? Because if Consumer 1 crashes and Consumer 2 (also in the `notification` group) takes over Partition 0, Consumer 2 just needs to ask: *"Hey, I belong to the notification group — how far has this group gotten on this topic-partition?"* It doesn't matter which specific consumer did the reading before.

---

### Where is this offset information stored?

This is the elegant part: Kafka doesn't invent some separate storage mechanism for this. Since **topic + partition is Kafka's core strength** for storing anything, offset tracking data is *also* stored in a topic and partition — no different from how your actual events are stored.

Kafka **automatically creates an internal topic** for this called:

```
_consumer_offsets
```

- The leading underscore signals it's an **internal** topic (created by Kafka itself, not by you).
- By default, Kafka creates this topic with **50 partitions**.
- No matter how many consumer groups you have — `notification`, `analytics`, `audit`, or a hundred more — all their offset information lives inside this one internal topic.

```
Kafka Broker
┌─────────────────────────────┐
│ Topic: order-events         │
│  P0 [   ][   ][   ]         │
│  P1 [   ][   ][   ]         │
│  P2 [   ][   ][   ]         │
└─────────────────────────────┘

┌─────────────────────────────┐
│ Topic: _consumer_offsets    │   ← internal, auto-created
│  P0  [ ... ]                │
│  P1  [ ... ]                │
│   ⋮                         │
│  P23 [ ... ]                │
│   ⋮                         │
│  P49 [ ... ]                │
└─────────────────────────────┘   (50 partitions by default)
```

---

### How does a group know *which* partition of `_consumer_offsets` to use?

Since `_consumer_offsets` has 50 partitions, Kafka needs a deterministic way to decide **which one** partition holds a given group's offset data. The logic mirrors key-based partitioning from Part 1:

```
partition = hash(group.id) % 50
```

**Worked example:** for the `notification` group —

```
hash(notification_group_id) % 50 = 23
```

So **all** offset updates for the `notification` group always go to **Partition 23** of `_consumer_offsets`. Since the hash of a fixed group ID always produces the same result, this group's offset data always lands in the same partition — consistent and predictable.

**The message a consumer publishes to commit an offset looks like this:**

```
Publish:
{
  "_consumer_offsets",                          ← topic
  notification_group_id,                        ← key

  message: (notification_group_id, topic, partition, committed_offset)   ← value
}
```

---

### Why bother computing the partition at all? (Connecting back to Broker)

This ties directly back to Part 1's Broker rule: **a broker holds only some partitions of some topics — not everything.** In a multi-broker setup:

```
Broker 1
Broker 2  → holds _consumer_offsets Partition 1 – 20
Broker 3  → holds _consumer_offsets Partition 21 – 50
```

Since `partition 23` falls in Broker 3's range, the consumer needs to know **which broker to actually connect to** in order to commit or fetch this offset data. That's exactly why the hash computation matters — it tells the consumer/group which specific broker is the right one to talk to.

*(The instructor is explicit that the full mechanics of multiple brokers — who decides which broker hosts what — is deferred to the **Kafka Cluster** section later, covered from Step 4 onward. At this point it's just enough to understand *why* the partition number matters.)*

---

### Crash & Recovery Flow — putting it all together

**Setup:** One Kafka broker, one topic `order-events` with partitions P0, P1, P2, plus the internal `_consumer_offsets` topic (50 partitions).

**Walkthrough:**

1. Consumer 1 (part of `notification` group) reads Partition 0 of `order-events`, and has processed up to **offset 500**.
2. It needs to commit this: *"Hey, group `notification`, topic `order-events`, partition 0 — processed till 500."*
3. It computes: `hash(notification_group_id) % 50 = 23` → so this message goes to **Partition 23** of `_consumer_offsets`.
4. It determines which broker hosts Partition 23 of `_consumer_offsets`, connects to that broker, and stores the commit there.

```
Consumer 1 (notification group)
   │
   │ processed order-events, Partition0, up to offset 500
   ▼
Compute: hash(notification_group_id) % 50 = 23
   │
   ▼
Find broker hosting Partition 23 of _consumer_offsets
   │
   ▼
Publish commit message to that broker
   │
   ▼
_consumer_offsets → Partition 23 stores:
   (notification, order-events, partition0) → offset 500
```

**Now say Consumer 1 crashes**, and Consumer 2 (also `notification` group) is assigned to take over Partition 0:

1. Consumer 2 needs to know where to resume from. It doesn't ask "what did Consumer 1 do" — it asks on behalf of the **group**.
2. It computes the same hash: `hash(notification_group_id) % 50 = 23`.
3. It finds which broker hosts Partition 23 of `_consumer_offsets`, connects, and asks: *"For topic `order-events`, partition 0 — how far has this group processed?"*
4. That broker checks its logs for Partition 23, finds the last committed offset (500), and returns it.
5. Consumer 2 resumes reading from **offset 501**.

This is exactly why the offset is stored **group-wise, not consumer-wise** — any consumer in the group can pick up the baton by simply asking the group's stored progress.

---

### Recap of Step 2

| Concept | Purpose |
|---|---|
| **`_consumer_offsets`** | Internal topic (auto-created, 50 partitions by default) that stores every consumer group's committed offsets |
| **Group-wise storage** | Offsets are tracked per (group, topic, partition) — not per individual consumer — so any consumer in the group can resume after a crash |
| **Partition computation** | `hash(group.id) % 50` deterministically decides which partition of `_consumer_offsets` a group's data lives in |
| **Why it matters** | In a multi-broker setup, knowing the partition tells the consumer exactly which broker to connect to |
| **Crash recovery** | New consumer computes the same hash, finds the right broker, asks for the last committed offset, and resumes from the next one |

---

## Step 3: Offset Commit Strategies

---

### The question this section answers

We now know consumers commit offsets to `_consumer_offsets`. But **when** exactly does a consumer decide to commit? Committing after every single event would be wasteful, and waiting too long is risky. The instructor lays out two strategies and is explicit about which one he recommends.

---

### Strategy 1: Auto-Commit

**Configuration:**
```
auto.commit.interval.ms = 5000
```
This tells Kafka: **auto-commit every 5 seconds**, regardless of where exactly the consumer's processing has reached at that moment.

**Why this is risky — the instructor's timeline example:**

```
Time 0s: Consumer polls, gets events 0–99
Time 1s: Consumer starts processing events...
Time 5s: Auto-commit triggers → commits offset 99
Time 6s: Consumer CRASHES (only actually processed up to event 50)
Time 7s: Consumer restarts, asks Kafka: "where did I leave off?"
         Kafka replies: offset 99 → resume from offset 100
```

**Result: Events 51–99 are LOST.** They were never actually processed by the consumer, but because the auto-commit fired on a timer — independent of actual processing progress — Kafka already recorded them as "done." This is what's called **at-most-once delivery**: events are processed *at most* once, but some might not be processed at all if a crash happens between the commit and the actual completion of work.

```
┌──────────────────────────────────────────────────────┐
│  Poll 0-99 → Processing (0...50✓...crash!) → 51-99   │
│                    ▲                          LOST   │
│                    │                                 │
│         Auto-commit already said "99 done"           │
│              at the 5-second mark                    │
└──────────────────────────────────────────────────────┘
```

**Takeaway:** auto-commit is convenient, but it commits based on a **timer**, not based on **actual completed work** — that mismatch is exactly where data loss creeps in.

---

### Strategy 2: Manual Commit (Recommended)

Instead of a timer, the consumer explicitly decides when to commit — and the recommended way is to do it **batch-wise**, only after the entire batch has actually finished processing.

**Step-by-step flow:**
```
Step 1: Poll events 0–99
Step 2: Process ALL events in that batch
Step 3: Commit offset 99 (and wait for confirmation)
Step 4: Kafka confirms the commit
Step 5: Continue to the next batch
```

**What happens if a crash occurs, depending on timing:**

| Crash timing | Outcome |
|---|---|
| **Before commit** (e.g. crashes at event 70, batch not yet committed) | Consumer restarts, resumes from the last committed offset (start of this batch) → events get **reprocessed** → this is **at-least-once delivery**, duplicates are possible but nothing is lost |
| **After commit** | Events are not reprocessed, since the commit already reflects the completed batch |

```
Poll 0-99 → Process all 100 → Commit(99) → Kafka confirms → Next batch
                 │
                 └─ if crash happens HERE (mid-processing, before commit):
                    restart → resume from LAST commit (not 99) → some duplicate processing
                    but ZERO data loss
```

**Why not commit after every single event?**

The instructor explicitly calls this out as **not recommended**. Kafka is built for high-throughput event streams — making a network call to the broker after every individual event defeats that purpose and adds unnecessary overhead. The practical approach is to **pick an appropriate batch size** based on your business needs (balancing safety vs. overhead), and commit once per batch.

---

### Auto-Commit vs Manual Commit — Side by Side

| Aspect | Auto-Commit | Manual Commit (batch-wise) |
|---|---|---|
| **Trigger** | Fixed timer (`auto.commit.interval.ms`) | Explicit, after batch processing completes |
| **Risk** | Can commit offsets for events not yet fully processed | Only commits what's actually been completed |
| **Delivery guarantee** | At-most-once (risk of **data loss**) | At-least-once (risk of **duplicate processing**, not loss) |
| **Recommended?** | No — instructor calls it risky | Yes — this is the recommended strategy |

**Interview-ready takeaway:** *"Auto-commit trades safety for convenience — it can silently lose events on a crash. Manual batch-wise commit trades a small risk of duplicate processing for a guarantee that no event is ever lost."*

---

### Recap of Step 3

| Strategy | Mechanism | Trade-off |
|---|---|---|
| **Auto-Commit** | Commits on a fixed time interval, regardless of actual processing progress | Risky — can lose events between the last real progress and the timer-based commit |
| **Manual Commit (batch-wise)** | Commits only after a full batch is processed, confirmed by Kafka | Recommended — guarantees at-least-once delivery; possible duplicates, never loss |
| **Manual Commit (per-event)** | Commit after every single event | Not recommended — too much overhead for high-throughput systems |

---


## Step 4: Kafka Cluster & Leader-Follower Partitions

---

### Transitioning into "the real Kafka" (instructor's framing)

At this point, the instructor is explicit: if Producer, Topic, Partition, Broker, Consumer, and Consumer Group are all clear, **now we get into the advanced stuff** — this is where multiple brokers actually come into play together, as a coordinated system.

---

### What is a Kafka Cluster?

**Definition:** A Kafka cluster is a **group of brokers working together** to provide three things:

| Property | Meaning |
|---|---|
| **Scalability** | Distribute load across multiple servers |
| **Fault tolerance** | Continue operating even if a broker fails |
| **High availability** | No single point of failure |

The instructor stresses this point strongly: **Kafka does not have a single point of failure.** Whether it's a broker, a partition, or a topic — the system as a whole keeps running.

```
┌────────────────────────────────────────────────────────┐
│                     Kafka Cluster                      │
│   ┌──────────┐     ┌──────────┐     ┌──────────┐       │
│   │ Broker 1 │     │ Broker 2 │     │ Broker 3 │  ...  │
│   └──────────┘     └──────────┘     └──────────┘       │
│   (each broker holds some topics, and some partitions  │
│    of those topics — never everything)                 │
└────────────────────────────────────────────────────────┘
```

The natural next question: **how** does having multiple brokers actually *achieve* scalability, fault tolerance, and high availability? The answer lies in how partitions are replicated and distributed — the **Leader-Follower Partition** model.

---

### Leader-Follower Partition — the core mechanism

**Rule:** For **each partition**, one broker holds the **leader** copy, and the rest hold **follower** copies.

**Worked example (instructor's numbers):**

```
Topic: order-events
Partitions: 3        (P0, P1, P2)
Replication Factor: 2   (2 copies of each partition)
```

Since each of the 3 partitions has a replication factor of 2, that means:

- **P0** → 1 leader + 1 follower
- **P1** → 1 leader + 1 follower
- **P2** → 1 leader + 1 follower

**Total partition-replicas created:** 3 partitions × 2 replicas = **6 partition-copies** — out of these 6, **3 are leaders** and **3 are followers**.

These 6 copies get **distributed across the brokers in the cluster.** With 3 brokers (Broker 1, Broker 2, Broker 3), here's how the instructor spreads them out:

```
Broker 1:  order-events  →  P0 : Leader
                             P1 : Follower

Broker 2:  order-events  →  P1 : Leader
                             P2 : Follower

Broker 3:  order-events  →  P0 : Follower
                             P2 : Leader
```

```
┌───────────────────────────────────────────────────────────┐
│                       Kafka Cluster                       │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐ │
│  │   Broker 1     │  │   Broker 2     │  │  Broker 3    │ │
│  │ order-events   │  │ order-events   │  │ order-events │ │
│  │  P0 : Leader   │  │  P1 : Leader   │  │  P0: Follower│ │
│  │  P1 : Follower │  │  P2 : Follower │  │  P2 : Leader │ │
│  └────────────────┘  └────────────────┘  └──────────────┘ │
└───────────────────────────────────────────────────────────┘
```

**Key observation:** notice that **no single broker holds all the information.** Broker 1 has the leader for P0, but only a *follower* backup of P1 — the actual leader for P1 sits on Broker 2. This spreading-out is exactly what gives the cluster its resilience: losing any one broker doesn't take down the whole topic.

---

### Leader is the only one that handles read/write

**Critical rule:** the **leader** partition-replica is the one that's actually responsible for **all reads and writes** for that partition. Followers do *not* serve any client requests — they exist purely as a **standby**, continuously replicating from the leader to stay up to date.

**Connecting back to Producer (Part 1):** recall that a producer computes which partition an event goes to via:

```
partition = hash(order_id) % num_partitions
```

Once it knows the target partition (say, Partition 1), the producer now also needs to know: **which broker hosts the *leader* of Partition 1** — because only the leader can accept the write. It looks that up, then connects directly to that broker to publish the event.

**Connecting back to Consumer offset commits (Step 2):** this is the exact same reason the `notification` group needed to find which broker hosts Partition 23 of `_consumer_offsets` — because only the **leader** of that partition can process the read/write for the offset commit.

---

### Leader vs Follower — Responsibilities

| Leader Responsibilities | Follower Responsibilities |
|---|---|
| Handle all **producer writes** | **Replicate** data from the leader |
| Handle all **consumer reads** | **Stay in sync** with the leader |
| **Maintain** the partition logs | Be **ready to become leader** if the current leader fails |
| **Coordinate** with followers | **Do NOT serve** any client (producer/consumer) requests |

The instructor's framing here is simple: followers are essentially dormant backups. Their entire job, under normal operation, is just to *keep up* with the leader — nothing more. They only spring into action and take over serving duties **when the leader fails.**

---

### The open question this leads to (deferred to next part)

Everything above raises a natural next question, which the instructor deliberately leaves open here:

> **Who decides:**
> - Which broker hosts which partition replica?
> - Which partition replica becomes the leader?
> - Which partition replica stays as a follower?

The instructor's one-line answer before stopping: **"That's where our Controller comes into the picture — which I will cover in the next part."** He explicitly does not want to rush into Controller within this video, and stops here.

---

### Recap of Step 4

| Concept | Core takeaway |
|---|---|
| **Kafka Cluster** | A group of brokers working together for scalability, fault tolerance, and high availability — no single point of failure |
| **Leader-Follower model** | For each partition, one broker holds the leader replica, others hold follower replicas — set via replication factor |
| **Distribution across brokers** | Leader/follower copies of different partitions are spread across brokers, so no one broker holds all the information for a topic |
| **Leader's job** | Handles all reads/writes, maintains logs, coordinates followers |
| **Follower's job** | Passive replication/standby only — never serves clients directly, takes over only if leader fails |
| **Open question (deferred)** | Who assigns leader/follower roles and broker placement? → Answer: the **Controller**, covered next |

---

## End of Part 2 — Full Summary

| Section | Core Idea |
|---|---|
| **Consumer Group** | Named group a consumer joins; group membership determines partition-read rules |
| **Rule 1** | Same group → no two consumers share a partition (idle consumers possible if consumers > partitions) |
| **Rule 2** | Different groups → can independently read the same partition |
| **`_consumer_offsets`** | Internal topic (50 partitions default) storing group-wise committed offsets, via `hash(group.id) % 50` |
| **Crash recovery** | Any consumer in a group can resume by fetching the group's last committed offset — no per-consumer tracking needed |
| **Offset Commit Strategies** | Auto-commit (risky, at-most-once) vs. Manual batch commit (recommended, at-least-once) |
| **Kafka Cluster** | Multiple brokers working together for scalability, fault tolerance, high availability |
| **Leader-Follower Partition** | Each partition has one leader (handles all reads/writes) and N followers (passive standbys) distributed across brokers |

**Interview-ready one-liners from this lecture:**
- *"Consumer offsets are tracked per consumer group, not per consumer — that's what allows seamless failover."*
- *"Auto-commit risks data loss because it commits on a timer, not on actual processing progress; manual batch commit trades that for possible duplicates instead."*
- *"Only the leader replica of a partition serves reads and writes — followers are pure standbys that only take over if the leader fails."*

---
