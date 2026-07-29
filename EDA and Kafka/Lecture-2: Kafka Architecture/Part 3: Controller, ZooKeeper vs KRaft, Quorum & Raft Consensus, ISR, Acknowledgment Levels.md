# Part 3: Controller, ZooKeeper vs KRaft, Quorum & Raft Consensus, ISR, Acknowledgment Levels

# 🎯Step 1: The Controller

## Picking up exactly where Part 2 left off

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image.png)

Part 2 ended with an open question: in a Kafka cluster with multiple brokers, **who decides** which broker hosts which partition, and which replica of a partition becomes the leader versus the follower? The instructor's one-line teaser was "that's the Controller" — this section unpacks it properly.

---

## What is a Controller?

**Definition:** The Controller is **not a different kind of component** — it is simply **a broker with special responsibilities.**

**The Controller's core jobs:**

| Responsibility | Meaning |
| --- | --- |
| **Topic creation** | Handles requests to create new topics |
| **Partition leader/follower election** | Decides which broker holds the leader replica of a partition, and which hold followers |
| **Broker failure detection** | Notices when a broker goes down |
| **Notify all brokers about changes** | Keeps every broker updated on cluster state |

In short: the Controller acts as a **coordinator among brokers.** All brokers continuously send it **heartbeats**, which is how it knows which brokers are up and which are down. If a broker goes down, the Controller re-assigns whatever partitions that broker was responsible for to other brokers.

**One clean summary line:** the Controller manages the **cluster metadata** — information about topics, partitions, brokers, and all their relationships.

---

## Dedicated vs. Dual-responsibility Controller

A Controller can be set up in two ways:

- **Dedicated Controller** — a broker whose *only* job is controlling the cluster; it does not serve producer/consumer requests at all.
- **Dual-responsibility Controller** — a broker that acts as a normal broker (handling producer/consumer requests) **and** takes on controller duties at the same time.

For simplicity going forward, the instructor uses the **dedicated Controller** model in diagrams and examples — one broker, one job: control.

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%201.png)

```
┌──────────────────────────────────────────────────────────┐
│                      Kafka Cluster                            │
│                                                               │
│  ┌──────────┐    ┌──────────┐    ┌──────────────────────┐ │
│  │ **Broker 1**  │    │ **Broker 2**  │    │     **Controller**         │ │
│  │  Topics   │    │  Topics   │    │ (a broker, but only    │ │
│  │Partitions │    │Partitions │    │  controls brokers —    │ │
│  └──────────┘    └──────────┘    │ doesn't serve prod/    │ │
│       ▲                ▲           │  consumer requests)    │ │
│       │  heartbeats    │           └──────────────────────┘ │
│       └───────────────┴──────────────────┘                 │
└──────────────────────────────────────────────────────────┘
```

---

## What does the Controller store, and where?

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%202.png)

The Controller maintains all cluster metadata in a **cluster metadata log** — a file where it holds information about every topic, every partition, and every broker.

```
Controller
   └──► **_cluster_metadata.log** (file)
            - which topics exist
            - which partitions each topic has
            - which broker holds leader/follower for each partition
            - which brokers are alive
```

---

## The basic Create-Topic flow (single controller, pre-KRaft view)

**Scenario:** A producer or an admin CLI sends a request: *"Create topic `order-events`, with 3 partitions and replication factor 2."*

That means: 3 partitions × 2 replicas = **6 total partition-copies** (3 leaders + 3 followers) need to be created and distributed.

**Key rule: Producers and admin clients never talk to the Controller directly.** They talk to **any broker** — it doesn't matter which one. That broker's only job here is to **forward** the request onward.

**Step-by-step flow:**

1. Producer/Admin CLI sends "create topic" request to **any broker**.
2. That broker **passes the request to the active Controller** — it does not try to handle topic creation itself.
3. The Controller:
    - Creates the topic
    - Decides which broker gets the **leader** for each partition, and which get **followers**
    - Updates this decision into its own **cluster metadata log**
4. The Controller then **forwards this updated metadata to all other brokers**, so every broker in the cluster stays up to date.

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%203.png)

```
Producer / Admin CLI
      │
      │  "Create topic order-events,
      │   3 partitions, replication factor 2"
      ▼
  Any Broker  ──────► forwards request ──────► Active Controller
                                                       │
                                                       ▼
                                        Decides leader/follower placement
                                                       │
                                                       ▼
                                        Updates *_cluster_metadata.log*
                                                       │
                                                       ▼
                                  Forwards updated metadata to ALL brokers
                                             │
             ┌────────────────────────────┼────────────────────────────┐
             ▼                               ▼                              ▼
      **Broker 1** updated             **Broker 2** updated             **Broker 3** updated
   (knows its own leader/           (knows its own leader/       (knows its own leader/
    follower assignments)             follower assignments)       follower assignments)
```

**Why this matters:** any change happening in the cluster — new topic, partition reassignment, broker going down — always flows **through the Controller first**. The Controller is the single source of truth that then propagates changes outward to every broker.

---

## Recap of Step 1

| Concept | Core takeaway |
| --- | --- |
| **Controller** | A broker with special responsibilities — not a separate type of system component |
| **4 core jobs** | Create topics, elect partition leaders/followers, detect broker failure, notify all brokers of changes |
| **Heartbeats** | All brokers send heartbeats to the Controller so it knows who's alive |
| **Dedicated vs Dual** | Controller can be a broker with *only* controller duties, or one that also serves producer/consumer requests (instructor uses dedicated for simplicity) |
| **Cluster metadata log** | Where the Controller stores all topic/partition/broker information |
| **Create-topic flow** | Producer/Admin → any broker → forwarded to active Controller → Controller decides leader/follower placement → updates its own metadata log → pushes updates to all brokers |

---

# 🎯Step 2: The `Single Point of Failure` Problem → ZooKeeper vs. KRaft

## The question that naturally comes up next

Now that we know the Controller is essentially the **"brain" of the cluster** — it stores all cluster metadata, decides leader/follower placement, and is the only thing brokers report to — an obvious concern follows:

> **What happens if the Controller itself goes down?**
> 

---

## Why a single Controller is dangerous

If there's only **one** Controller and it fails:

- **No new topics can be created** — there's no one to process that request.
- **No one can detect broker failures** — heartbeats have nowhere to go, so a dead broker just... sits there, undetected.
- **No one can reassign partitions** — if a broker with important leader partitions goes down right when the Controller is also down, nothing hands those partitions over to a healthy broker.
- **All cluster metadata could be lost** — since the Controller is the one holding the cluster metadata log.

In short: **the Controller is currently a single point of failure** for the entire cluster.

```
┌────────────────────────────────────────────────┐
│                Kafka Cluster                       │
│                                                    │
│  Broker 1       Broker 2        Broker 3           │
│     │               │               │              │
│     └──────────────┼──────────────┘             │
│                     ▼                              │
│               ❌ **Controller** (DOWN!)                │
│                                                    │
│   → No topic creation                              │
│   → No broker failure detection                    │
│   → No partition reassignment                      │
│   → Cluster metadata at risk                       │
└────────────────────────────────────────────────┘
```

---

## The obvious first instinct: *"just add more controllers"*

Since Kafka already solves broker-level failure by having *multiple* brokers, the natural fix seems to be: **have multiple controllers too** — Controller 1, Controller 2, Controller 3.

But this immediately raises a **recursive problem**:

> If multiple brokers need a Controller to coordinate them... **do multiple Controllers need something to coordinate *them*?**
> 

This can't turn into an infinite loop of "a controller for the controllers, and a controller for that..." — so a different mechanism is needed to let multiple Controllers coordinate **among themselves**, without needing yet another layer above them.

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%204.png)

**The answer: KRaft (modern) or Zookeeper (legacy).**

---

## Zookeeper (legacy approach)

Zookeeper is an **external distributed system** that Kafka relies on to manage this multi-controller coordination.

**The core problem with Zookeeper:** it is **external** to Kafka —

- It has to be **deployed separately**
- **Monitored separately**
- **Maintained separately**

It's an entirely separate piece of infrastructure that Kafka *depends on*, rather than something built into Kafka itself.

```
┌─────────────────┐          ┌─────────────────────┐
│   **Kafka Cluster**   │◄───────►│   **Zookeeper Cluster**   │
│  (brokers +       │ network  │  (external system,    │
│   controllers)    │  calls   │   separate deploy/    │
│                   │          │   monitor/maintain)   │
└─────────────────┘          └─────────────────────┘
```

Because of this external dependency overhead, **Zookeeper is now deprecated** in modern Kafka (Kafka ≥ 3.x).

---

## KRaft (modern approach) — Kafka's own built-in replacement

KRaft = **K**afka + **Raft** (the name comes directly from the consensus algorithm it uses).

**The key difference from Zookeeper:** KRaft's logic lives **inside the Controller itself** — there's no external system to deploy or maintain. It's part of the Controller's own machinery.

```
┌───────────────────────────────────────────┐
│              Kafka Cluster                    │
│                                               │
│   Broker 1    Broker 2    Broker 3            │
│                                               │
│   ┌─────────────────────────────────┐      │
│   │        Controller                  │      │
│   │  ┌─────────────────────────┐    │      │
│   │  │  **Business logic**:          │    │       │
│   │  │  create topic, elect      │    │       │
│   │  │  leader/follower          │    │       │
│   │  ├─────────────────────────┤    │       │
│   │  │  **KRaft layer**:             │    │       │
│   │  │  storage + consensus      │    │       │
│   │  │  logic (no external call) │    │       │
│   │  └─────────────────────────┘    │       │
│   └─────────────────────────────────┘      │
│      (no separate system to deploy/           │
│       monitor/maintain)                       │
└───────────────────────────────────────────┘
```

---

## Both use a consensus algorithm — just different ones

| System | Consensus algorithm used |
| --- | --- |
| **Zookeeper** | ZAB (Zookeeper Atomic Broadcast) |
| **KRaft** | Raft — this is exactly where the name "**K**afka + **Raft**" comes from |

---

## What is `"consensus"`, at a high level?

Before going further, it's worth holding onto *why* consensus is needed at all: with **multiple Controllers**, no single one of them should unilaterally decide things like *"I'm the active controller now"* or *"this metadata change is final."* Instead, they need a way to **agree together** — that agreement mechanism is what a consensus algorithm provides. The next step (Quorum & Raft) unpacks exactly how that agreement happens.

---

## Recap of Step 2

| Concept | Core takeaway |
| --- | --- |
| **Single Controller risk** | If it fails: no topic creation, no failure detection, no reassignment, metadata at risk — a true single point of failure |
| **Naive fix** | Add multiple Controllers — but this raises "who coordinates the coordinators?" |
| **Zookeeper (legacy)** | External distributed system Kafka depends on — separate deploy/monitor/maintain overhead; now deprecated |
| **KRaft (modern)** | Built directly into the Controller — no external dependency; used in Kafka ≥ 3.x |
| **Consensus algorithms** | Zookeeper uses ZAB, KRaft uses Raft — hence the name "KRaft" |
| **Why consensus at all** | Needed so multiple Controllers can agree on decisions (who's active, what metadata changes are final) without an infinite chain of coordinators |

---

# 🎯Step 3: `Quorum` & `Raft` Consensus — The Detailed KRaft Flow

## Multiple Controllers, but only one is active

With KRaft, we now have multiple Controllers — say, **Controller 1, Controller 2, Controller 3.**

**Key rule:** at any given time, in one Kafka cluster, **only one Controller is active** (acting as the leader among controllers). All the others are **standby controllers.**

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%205.png)

```
┌─────────────────────────────────────────────────────┐
│                   Kafka Cluster                          │
│                                                          │
│   Controller 1        Controller 2        Controller 3   │
│   (ACTIVE)             (Standby)            (Standby)    │
│                                                          │
└─────────────────────────────────────────────────────┘
```

This immediately raises the same question as before: **who decides which Controller is active, and which are standby?** The answer, again: **not a person, not another layer above them — they decide it themselves, through the Raft consensus algorithm.**

---

## What is Quorum?

**`Quorum` = majority agreement.** Any decision — whether it's *"who becomes the active controller"* or *"is this metadata change final"* — must be agreed upon by a **majority of the controller nodes**, not just one.

**The formula:**

```
Quorum = (n / 2) + 1
```

Where `n` = total number of controller nodes.

**Worked example (3 controllers):**

```
n = 3
Quorum = (3 / 2) + 1 = 2   (integer division)
```

So with 3 controllers, **at least 2 must agree** before any decision — who's active, or any cluster metadata change — becomes final.

This same quorum mechanism decides **both**:

- Which controller becomes the active one
- Whether any given cluster metadata change is accepted

*(The instructor is explicit that the internal mechanics of how Raft itself reaches this agreement is out of scope — what matters here is the quorum requirement and its effect on the flow.)*

---

## The full Create-Topic flow, now with KRaft in the picture

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%206.png)

This is the same scenario as Step 1 (create topic `order-events`, 3 partitions, replication factor 2) — but now walked through with the quorum mechanism included.

**Step-by-step:**

1. Producer/Admin CLI sends the create-topic request to **any broker**.
2. That broker forwards it to the **active Controller** (Controller 1, say).
3. Controller 1:
    - Decides the topic's leader/follower placement across brokers
    - Creates a **new metadata record** for this change — say the current last-committed offset is **99**, and this new record would be **offset 100**
4. Controller 1 passes this new metadata record to its **own KRaft layer**.
5. Controller 1's KRaft layer:
    - Writes offset 100 to its **local** cluster metadata log — but marks it as **not yet committed**
    - Its **last committed offset remains 99** for now
    - **Initiates the quorum process** — it calls the KRaft layer of every standby controller

```
Active Controller 1
   │
   │ Decides leader/follower placement for order-events
   ▼
Creates metadata record: offset 100 (new change)
   │
   ▼
Passes to its own KRaft layer
   │
   ▼
KRaft layer writes offset 100 LOCALLY → marked UNCOMMITTED
   (last committed offset still = 99)
   │
   ▼
Initiates QUORUM: calls Controller 2's and Controller 3's KRaft layers
```

1. **Controller 2's KRaft** and **Controller 3's KRaft** each independently:
    - Agree with the change
    - Write offset 100 to their **own local** metadata logs — also marked **uncommitted**
    - Their last committed offset also still shows 99
    - Send a response back to Controller 1: **"yes, agreed"**

```
**Controller 2** (Standby)                       **Controller 3** (Standby)
   │                                               │
   │ Agrees with the change                        │ Agrees with the change
   ▼                                               ▼
Writes offset 100 locally → UNCOMMITTED          Writes offset 100 locally → UNCOMMITTED
(last committed still 99)                        (last committed still 99)
   │                                                │
   └──────────► sends "agreed" ◄─────────────────┘
                       │
                       ▼
              back to Controller 1 (Active)
```

1. Controller 1 checks: **did I get agreement from a majority?** (Quorum = 2 out of 3 here). Since both Controller 2 and Controller 3 agreed, **quorum is achieved.**
2. Controller 1 now **commits** offset 100 — its last committed offset updates from 99 → **100**.
3. Controller 1 pushes this now-committed cluster metadata to **all the brokers**, so every broker knows the new leader/follower assignments for `order-events`.

```
Controller 1 checks: got majority agreement (2 of 3)? YES
   │
   ▼
COMMIT offset 100 (last committed offset: 99 → 100)
   │
   ▼
Pushes committed metadata to all brokers
   │
   ├──► Broker 1 (updated)
   ├──► Broker 2 (updated)
   └──► Broker 3 (updated)
```

---

## `"High watermark"` — just another name for last committed offset

The instructor flags this explicitly as a term you may have already heard: **"high watermark"** is not some separate new concept — it's simply **the last committed offset.** If the high watermark is 99, that means every controller has agreed on all metadata changes up through offset 99; anything beyond that (like offset 100) is still pending agreement.

---

### How do standby controllers eventually catch up and commit?

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%207.png)

At this point, Controller 2 and Controller 3 have written offset 100 locally, but **haven't yet marked it as committed** on their own end. How do they find out it's now safe to commit?

**Answer: through heartbeats.**

- The **active Controller continuously sends heartbeats** to standby controllers — this is necessary anyway, because if the active controller ever goes down, one of the standbys needs to become active.
- Along with each heartbeat, the active Controller also sends its **current last-committed offset.**

```
Controller 1 (Active)
   │
   │ heartbeat + "my last committed offset = 100"
   ▼
Controller 2 (Standby)  ──► already has record locally → just marks it COMMITTED
Controller 3 (Standby)  ──► already has record locally → just marks it COMMITTED
```

**Important efficiency detail:** the entire cluster metadata log does **not** get re-copied or re-transmitted every time. Since Controller 2 and Controller 3 already wrote the record locally during the quorum step, all they need is the **confirmation** that it's now safe to mark it committed — not the data itself again.

---

## Recap of Step 3

| Concept | Core takeaway |
| --- | --- |
| **Active vs Standby Controllers** | Only one Controller is active at a time; others are standby — decided via Raft consensus, not manually |
| **Quorum** | Majority agreement required for any decision — formula: `(n/2) + 1` |
| **Uncommitted local write** | Active controller writes a new metadata record locally first, marked uncommitted, before requesting quorum |
| **Quorum process** | Active controller asks standby controllers' KRaft layers to agree; each writes the record locally (uncommitted) and responds |
| **Commit** | Once majority agreement is reached, the active controller commits and updates its last-committed offset |
| **High watermark** | Just another name for "last committed offset" — nothing new |
| **Heartbeats carry commit info** | Active controller's heartbeats to standbys include the latest committed offset, letting them simply mark their already-written local record as committed — no data re-transfer needed |

---

# 🎯Step 4: ISR (In-Sync Replica)

### Where ISR fits in

The Controller maintains cluster metadata — and one more important piece of that metadata (alongside topics, partitions, and broker info) is **ISR: In-Sync Replica.**

---

## What is ISR?

**Definition:** For each partition, **ISR represents the list of replicas that are fully caught up with the leader.**

**Worked example (instructor's numbers):**

```
Topic: order-events
Partition: 1
Replication Factor: 3   → 1 leader + 2 followers = 3 total replicas
```

Say:

- **Broker 1** → holds the **leader** replica of Partition 1
- **Broker 2** → holds a **follower** replica
- **Broker 3** → holds a **follower** replica

If all three are currently caught up with each other, then the **ISR for this partition is:**  

```
**ISR = {Broker 1, Broker 2, Broker 3}**
```

— a simple list of which replicas currently count as "in sync."

```
Topic: order-events, Partition 1 (Replication Factor 3)

Broker 1: Leader     ─┐
Broker 2: Follower    ├── all caught up → ISR = {Broker 1, Broker 2, Broker 3}
Broker 3: Follower   ─┘
```

---

## How does a replica fall out of the ISR?

**Mechanism:** followers are constantly **pulling** from the leader — Kafka's replication, like its consumer model, is **pull-based.** Each follower keeps sending poll requests to the leader: *"any latest updates? any latest updates?"* This means the **leader always knows** exactly how current each follower is.

**Worked example — a follower falling behind:**

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%208.png)

- Broker 1 (leader) is currently at **offset 9**.
- Broker 2 (follower) is also at offset 9 — fully caught up, still in ISR.
- Broker 3 (follower) hasn't sent a poll request in, say, **5 minutes** — it has fallen out of sync.

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%209.png)

**The rule for determining `"in sync"` vs `"lagging"`: a lag-time threshold.**

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%2010.png)

- If a replica's offset is close enough to the leader's latest offset **within a configured time window** (e.g., a default lag tolerance — the instructor uses **30 seconds** as the example), it's still considered **in sync.**
- If the lag exceeds that threshold, the leader considers that replica as **lagging** — no longer in sync.

```
Leader (Broker 1): latest offset = 9

Broker 2 (Follower): last poll → within 30s lag window → IN SYNC ✅
Broker 3 (Follower): last poll → 5 minutes ago → exceeds lag threshold → OUT OF SYNC ❌
```

Once the leader detects that Broker 3 has exceeded the allowed lag, it **updates the ISR list** for that partition:

```
Before: ISR = [Broker 1, Broker 2, Broker 3]
After:  ISR = [Broker 1, Broker 2]     ← Broker 3 temporarily removed
```

---

## Who updates the ISR, and where does that information go?

**Important flow detail:** it's the **leader** (Broker 1, in this example) that detects the lag and updates the ISR list — since the leader is the one every follower is polling, so it naturally has visibility into who's current and who isn't.

Once the leader updates the ISR, it **passes this updated list to the `Controller`**, so the Controller's cluster metadata stays accurate.

```
Leader (Broker 1)
   │
   │ Detects Broker 3 exceeded lag threshold
   ▼
Updates local ISR: [Broker 1, Broker 2]
   │
   ▼
Reports updated ISR to the Controller
   │
   ▼
Controller updates cluster metadata (ISR info for this partition)
```

---

### Recap of Step 4

| Concept | Core takeaway |
| --- | --- |
| **ISR** | For each partition, the list of replicas currently fully caught up with the leader |
| **Pull-based replication** | Followers continuously poll the leader for updates — this is how the leader tracks each follower's freshness |
| **Lag threshold** | If a follower's lag exceeds a configured time window (e.g. 30 seconds), it's considered out of sync and removed from ISR |
| **Who updates ISR** | The leader detects lag and updates the ISR list, then reports it to the Controller |
| **Where it's stored** | The Controller keeps the ISR list as part of its cluster metadata |

---

# 🎯Step 5: Acknowledgment Levels (ack) & `min.insync.replicas`

## Why ISR matters to the producer

![image.png](Part%203%20Controller,%20ZooKeeper%20vs%20KRaft,%20Quorum%20&%20Ra/image%2011.png)

ISR isn't just an internal bookkeeping detail — it directly determines **how a producer's write is acknowledged.** When a producer publishes an event, it can configure an **`acknowledge`** (ack) setting, and this setting interacts directly with the partition's ISR list.

**Setup for this section's example:**

```
Topic: order-events
Key: null → Round Robin partitioning assigns the event to Partition 2
Partition 2's leader: Broker 1
Partition 2's ISR: [Broker 1, Broker 2, Broker 3]
```

Since only the **leader** handles all reads and writes, the write request always goes to **Broker 1** first, regardless of ack setting.

```
Producer
   │
   │ Publish event (Partition 2)
   ▼
Broker 1 (Leader of Partition 2)
   │
   ▼
Persists event → writes into Partition 2's log (into a segment file)
```

That part — the leader persisting the event — always happens the same way. What **differs** based on the `ack` setting is: **what does the producer wait for before it's told "success"?**

---

## The three ack levels

**`ack = 0` — Fire and forget**

- Producer sends the event to the leader and **does not wait for anything** — not even confirmation that the leader received or persisted it.
- If the leader crashes right after, the producer has no idea and doesn't care.
- Fastest, but **riskiest** — no delivery guarantee at all.

```
Producer ──publish──► Leader
   │
   ▼
Producer moves on immediately — no wait, no confirmation
```

**`ack = 1` — Wait for the leader only**

- Producer sends the event and **waits** until the leader has successfully written it to its own log.
- Once the leader confirms its own write succeeded, the producer gets a "published successfully" response.
- **Does not wait for any followers** to replicate it.

```
Producer ──publish──► Leader
                         │
                         ▼
                  Writes to its own log
                         │
                         ▼
              Leader confirms write success
                         │
                         ▼
        Producer receives "published successfully"
        (followers may not have replicated it yet)
```

**`ack = all` — Wait for the leader + all replicas in the ISR**

- Producer sends the event, the leader persists it, and then the leader **waits for every replica currently in the ISR** to also successfully replicate the event.
- Each follower in the ISR pulls the new event, writes it, and sends back an acknowledgment.
- Only once **all ISR replicas have confirmed** does the leader respond "published successfully" to the producer.

```
Producer ──publish──► Leader (Broker 1)
                         │
                         ▼
                  Writes to its own log
                         │
                         ▼
        Waits for ALL replicas in ISR = [Broker 1, Broker 2, Broker 3]
                          │
        ┌────────────────┼────────────────┐
        ▼                                    ▼
   Broker 2 polls,                  Broker 3 polls,
   replicates, ACKs                 replicates, ACKs
        │                                    │
        └────────────────┬────────────────┘
                           ▼
        All ISR replicas confirmed →
        Leader tells producer: "published successfully"
```

**Note:** "all" means all replicas **currently in the ISR** — not necessarily every replica that was ever part of the replication factor. If a replica has already fallen out of ISR (like the lagging Broker 3 from Step 4), it's not counted here.

---

## Ack levels — side by side

| Ack level | What producer waits for | Risk / Trade-off |
| --- | --- | --- |
| **0** | Nothing | Fastest, but zero delivery guarantee — leader crash = silent loss |
| **1** | Leader's own write only | Safer than 0, but a leader crash right after (before followers replicate) can still lose the event |
| **all** | Leader + every replica currently in ISR | Strongest durability guarantee, but slowest — waits on every in-sync replica |

---

## Can the ISR list ever become empty?

**Rule: No — the ISR list can never be empty.**

**Why:** even if every follower falls out of sync due to lag, the **leader itself is always still counted** — a leader can't remove itself from its own ISR. So the absolute minimum ISR is just `[Leader]`.

```
Replication Factor: 3 (1 leader + 2 followers)

Follower 1 lags too much → removed from ISR
Follower 2 lags too much → removed from ISR

ISR = [Leader]   ← still has at least the leader, never fully empty
```

---

## `min.insync.replicas` — the safety net config

There's a related but separate config: **`min.insync.replicas`.** This defines the **minimum number of replicas that must be in the ISR** for a write with `ack = all` to succeed.

**Worked example:**

```
Replication Factor: 3 (1 leader + 2 followers)
min.insync.replicas = 2

Both followers lag and fall out of sync:
ISR = [Leader]   → only 1 replica in ISR

Producer sends event with ack = all
   │
   ▼
Leader checks: current ISR size (1) >= min.insync.replicas (2)?
   │
   ▼
NO → write FAILS
```

Even though the event **was** successfully written to the leader's own log, the request is **rejected/fails** overall — because the durability guarantee the producer asked for (`ack = all`, backed by a minimum of 2 in-sync replicas) can't currently be met.

---

## Recap of Step 5

| Concept | Core takeaway |
| --- | --- |
| **ack = 0** | Fire and forget — no wait, no guarantee, fastest |
| **ack = 1** | Wait for leader's own write only — followers may lag behind |
| **ack = all** | Wait for leader + every replica currently in ISR — strongest guarantee, slowest |
| **ISR can never be empty** | The leader is always counted, even if every follower falls out of sync |
| **`min.insync.replicas`** | Minimum ISR size required for an `ack = all` write to succeed — if ISR shrinks below this, the write fails even though the leader itself wrote it fine |

---

## End of Part 3 — Full Summary

| Section | Core Idea |
| --- | --- |
| **Controller** | A broker with special responsibilities: create topics, elect leader/follower, detect broker failure, notify all brokers — coordinated via heartbeats |
| **Single point of failure** | A lone Controller risks the whole cluster if it goes down — leads to needing multiple Controllers |
| **Zookeeper vs KRaft** | Zookeeper = external dependency (deprecated); KRaft = built into the Controller itself, using Raft consensus |
| **Quorum** | Majority agreement — `(n/2) + 1` — required for any decision among multiple Controllers |
| **KRaft create-topic flow** | Active Controller writes locally (uncommitted) → requests quorum from standbys → majority agrees → commits → pushes to brokers; heartbeats carry the committed offset to standbys |
| **High watermark** | Just another term for "last committed offset" |
| **ISR** | List of replicas fully caught up with the leader; leader detects lag via pull-based polling and a lag-time threshold |
| **Ack levels** | 0 (fire and forget), 1 (leader only), all (leader + all ISR replicas) |
| **min.insync.replicas** | Minimum ISR size required for `ack=all` writes to succeed; if ISR shrinks below it, writes fail even if the leader wrote successfully |

**Interview-ready one-liners from this lecture:**

- *"The Controller is just a broker with special responsibilities — it manages cluster metadata, not actual producer/consumer traffic."*
- *"KRaft replaced Zookeeper because Zookeeper was an external dependency Kafka had to deploy and maintain separately — KRaft brings that consensus logic inside the Controller itself."*
- *"Quorum means majority agreement — with 3 controllers, at least 2 must agree before any cluster metadata change is committed."*
- *"ISR can never be empty — the leader is always part of it, even if every follower falls behind."*
- *"ack=all doesn't mean every replica ever assigned — it means every replica currently in the ISR."*

---