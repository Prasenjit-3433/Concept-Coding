# Part 5: Failure Scenarios — Controller, Leader/Follower Partition, Topic, Consumer, and Broker Failures

# 🎯Step 1: Active Controller Failure + Leader Partition Failure

---

### Why this lecture is different (instructor's framing)

This is the final lecture of the Kafka Architecture series. Rather than introducing new concepts, the instructor treats this entire lecture as an **interview-question accumulation exercise** — walking through the classic "what happens when X fails?" questions one by one. His framing is direct: if everything from the previous 4 parts is clear, these answers should already feel obvious — this lecture just packages them into the exact shape an interviewer would ask them in.

---

### Question 1: What happens when the active Controller fails?

**Setup (recap from Part 3):** Three controllers exist — Controller 1 is active (leader), Controller 2 and Controller 3 are standby.

```
┌──────────────────────────────────────────────────────────┐
│                   Kafka Cluster                          │
│                                                          │
│   Controller 1        Controller 2        Controller 3   │
│   (ACTIVE)             (Standby)            (Standby)    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**What happens:** Controller 1 crashes.

Recall from Part 3: the active controller continuously sends **heartbeats** to the standby controllers. Once Controller 1 goes down, Controller 2 and Controller 3 stop receiving those heartbeats.

**The recovery flow:**

1. Controller 2 notices the missing heartbeats and **starts a Raft election**, requesting votes from the other controllers.
2. Controller 3 votes for Controller 2.
3. Controller 2 now has **2 votes** (its own + Controller 3's) out of 3 total — a majority, since Controller 1 is down and can't vote.
4. Controller 2 **wins the election** and becomes the new active controller.
5. Controller 3 **remains a standby.**

```
Controller 1 (ACTIVE) ──────► CRASHES
        │
        ▼
No heartbeat reaches Controller 2 / Controller 3
        │
        ▼
Controller 2 starts election, requests votes
        │
        ▼
Controller 3 votes for Controller 2
        │
        ▼
Controller 2 = 2 votes out of 3 → MAJORITY → WINS
        │
        ▼
Controller 2 becomes new ACTIVE controller
Controller 3 remains STANDBY
```

**Impact:** A new leader gets elected, but **the cluster continues working** — this is the same quorum/Raft mechanism from Part 3, just applied to a real failure rather than a planned metadata update.

---

### Question 2: What happens when a leader partition fails?

**Setup:** Partition 0 with replication factor 3 (1 leader + 2 followers), spread across 3 brokers:

```
Broker 1: Partition 0 → Leader
Broker 2: Partition 0 → Follower
Broker 3: Partition 0 → Follower

ISR = [Broker 1, Broker 2, Broker 3]
```

**What happens:** Broker 1 — the leader of Partition 0 — fails.

**The recovery flow:**

1. Every broker sends heartbeats to the Controller (recap from Part 1/3). Since Broker 1 stops sending heartbeats, the **Controller detects the leader failure.**
2. The Controller **triggers a leader election** for Partition 0 — but only among the replicas that are still in the ISR. Before the failure, ISR = [Broker 1, Broker 2, Broker 3]. With Broker 1 now considered down, the Controller picks a new leader from the **remaining ISR members**: Broker 2, Broker 3.
3. An internal election happens among the eligible replicas — if a quorum agrees, **Broker 2 becomes the new leader.**
4. The Controller **updates the metadata**: leader for Partition 0 changes from Broker 1 → Broker 2, and the ISR is updated to `[Broker 2, Broker 3]`.
5. The Controller **publishes this updated metadata to all brokers** in the cluster.
6. Producers and consumers that were previously talking to Broker 1 for Partition 0 now **connect to Broker 2** instead, since Broker 2 is the new leader.

```
Broker 1 (Leader) ──────► CRASHES
        │
        ▼
Controller detects: no heartbeat from Broker 1
        │
        ▼
Controller triggers leader election
   (picks from ISR, excluding Broker 1)
        │
        ▼
ISR before: [Broker 1, Broker 2, Broker 3]
        │
        ▼
Election among remaining ISR members → Broker 2 wins (quorum agrees)
        │
        ▼
Controller updates metadata:
   Leader: Broker 1 → Broker 2
   ISR: [Broker 2, Broker 3]
        │
        ▼
Controller pushes updated metadata to ALL brokers
        │
        ▼
Producers/Consumers reconnect → now talk to Broker 2 for Partition 0
```

**Instructor's framing, tying back to earlier lectures:** this is the same underlying idea from Part 2 — *"partition is just a way of distributing load among brokers"* — a partition's leadership can move to a different broker without the partition itself "failing."

**Impact:** No data loss, no manual intervention needed — **automatic recovery.**

---

### Recap of Step 1

| Scenario | Who detects it | Recovery mechanism | Impact |
| --- | --- | --- | --- |
| **Active Controller fails** | Standby controllers (missing heartbeats) | Raft election among standby controllers → majority vote → new active controller | Cluster keeps working, no major disruption |
| **Leader partition fails** | Controller (missing heartbeat from that broker) | Controller elects new leader from remaining ISR members, updates metadata, propagates to all brokers | No data loss; producers/consumers auto-reconnect to new leader |

# 🎯Step 2: Follower Partition Failure + The "Topic Failure" Trick Question

---

### Question 3: What happens when a follower partition fails?

**Setup (same as Step 1's leader-failure example):** Partition 0, replication factor 3.

```
Broker 1: Partition 0 → Leader
Broker 2: Partition 0 → Follower
Broker 3: Partition 0 → Follower

ISR = [Broker 1, Broker 2, Broker 3]
```

**What happens:** This time it's a **follower** that crashes — say, Broker 3.

**Key difference from leader failure — who detects it:**

Recall from Part 3: followers are constantly **pulling (fetching)** from the leader to stay up to date — this is how replication works. So it's not the Controller that first notices a follower going down; it's the **leader itself**, because the leader is the one every follower sends fetch requests to.

**The recovery flow:**

1. Broker 3 (follower) crashes. It stops sending fetch requests to the leader (Broker 1).
2. Broker 1 (the leader) notices there's **no timely fetch request** coming from Broker 3, and concludes something is wrong with it — this is the same lag-detection mechanism from Part 3's ISR discussion.
3. Broker 1 **updates the ISR locally**, removing Broker 3: ISR becomes `[Broker 1, Broker 2]`.
4. Broker 1 **passes this updated ISR to the Controller.**
5. The Controller **updates its cluster metadata** to reflect the new ISR.

```
Broker 3 (Follower) ──────► CRASHES
        │
        ▼
No fetch requests arrive at Broker 1 (Leader)
        │
        ▼
Broker 1 (Leader) detects: Broker 3 is unresponsive
        │
        ▼
Broker 1 updates ISR locally: [Broker 1, Broker 2, Broker 3] → [Broker 1, Broker 2]
        │
        ▼
Broker 1 reports updated ISR to the Controller
        │
        ▼
Controller updates cluster metadata
```

**Impact while Broker 3 is down:** The system **continues normally** — producers can still write (since only ISR members matter for `ack=all`, and Broker 1 + Broker 2 are still in the ISR). No issues.

---

### Recovery — what happens when Broker 3 comes back?

1. Broker 3 recovers and **starts sending fetch requests again**, trying to catch up with the leader.
2. Broker 1 (the leader) sees that Broker 3 is fetching and observes it **doing a catch-up** with the latest data.
3. Once Broker 3 is fully caught up, Broker 1 **adds it back to the ISR.**
4. Broker 1 passes this update to the Controller, and the **Controller updates the metadata** again.

```
Broker 3 recovers
        │
        ▼
Broker 3 resumes fetch requests → catching up with leader
        │
        ▼
Broker 1 (Leader) confirms: Broker 3 has caught up
        │
        ▼
Broker 1 adds Broker 3 back to ISR: [Broker 1, Broker 2] → [Broker 1, Broker 2, Broker 3]
        │
        ▼
Broker 1 reports to Controller → Controller updates metadata
```

**Key takeaway:** the timing of re-joining the ISR is entirely dependent on **how quickly the recovered broker catches up** with the leader — there's no fixed schedule, it's purely catch-up-driven.

---

### Question 4: What happens when a topic fails? (The trick question)

The instructor explicitly flags this as a **tricky interview question** — and the trick is in the premise itself.

**The answer: a topic can never fail.**

**Why:** Going all the way back to Part 1's definition — a **topic is just a category or a logical folder.** It doesn't physically hold data itself; it's not a running process, a server, or a piece of infrastructure that can crash. It's a naming/grouping concept.

**What *can* fail is a partition** — specifically, either:

- A **leader partition** failing (Question 2, Step 1)
- A **follower partition** failing (Question 3, this step)

```
❌ "Topic fails" — this doesn't make sense as a concept

✅ What can actually fail:
   Topic: order-events (logical folder — cannot fail)
       │
       ├──► Partition 0 (Leader)   ← THIS can fail
       ├──► Partition 1 (Follower) ← THIS can fail
       └──► Partition 2            ← THIS can fail
```

**Interview-ready one-liner:** *"A topic can never fail — it's just a logical category. What can fail is a partition, either as a leader or a follower, and Kafka has automatic recovery mechanisms for both."*

---

### Recap of Step 2

| Scenario | Who detects it | Recovery mechanism | Impact |
| --- | --- | --- | --- |
| **Follower partition fails** | The leader (via missing fetch requests) | Leader removes follower from ISR, reports to Controller; follower rejoins ISR after catching up post-recovery | No issues while down — producers still write fine via remaining ISR |
| **"Topic fails"** | N/A — trick question | Topics are logical, not physical — only partitions (leader or follower) can fail | N/A |

# 🎯Step 3: Consumer Failure — Two Distinct Scenarios

---

### Why consumer failure needs two separate scenarios

The instructor draws a clear line between two very different situations: a consumer that **crashes outright before committing**, versus a consumer that's **still alive but too slow to send a heartbeat** (and gets treated as dead anyway). These lead to different recovery mechanics, so each gets its own walkthrough.

---

### Scenario 1: Consumer crashes before committing offset

**Setup:** A consumer polls a batch of events — offsets 100 to 199.

**What happens:**

1. Consumer polls events **100–199**.
2. Consumer starts processing sequentially — gets through offsets 100 to **150**.
3. At that point, the consumer **crashes** (before committing anything for this batch).
4. Consumer restarts.
5. On restart, it asks the **Group Coordinator** (recap from Part 4: the broker that holds the leader replica of the `_consumer_offsets` partition for this group): *"What's my last committed offset?"*
6. The Group Coordinator returns the **last committed offset — still 99** (from the previous batch, before this one started), since nothing in the 100–199 batch was ever committed.
7. The consumer must **resume from offset 100** — meaning events 100 through 150, which were already processed once, get **reprocessed.**

```
Poll: offsets 100–199
        │
        ▼
Processing... 100, 101, ..., 150 ✓ processed
        │
        ▼
        ✗ CRASH (before commit)
        │
        ▼
Consumer restarts
        │
        ▼
Asks Group Coordinator: "what's my last committed offset?"
        │
        ▼
Group Coordinator returns: 99 (nothing from this batch was committed)
        │
        ▼
Consumer resumes from offset 100 → events 100–150 REPROCESSED (duplicates)
```

**This is the familiar at-least-once behavior from Part 2** — no data is lost, but events already processed before the crash get processed again since they were never committed.

---

### Scenario 2: Consumer heartbeat timeout (long processing time)

This is a subtler case — the consumer doesn't actually crash. It's just **too busy processing to send its heartbeat in time**, and gets treated as dead anyway.

**Setup:**

```
Consumer Group: notification
   Consumer 1 → handles Partition 0 of order-events
   Consumer 2 → handles Partition 1 of order-events
   Consumer 3 → also part of this group

Group Coordinator = the broker holding the leader of _consumer_offsets
                     partition for the "notification" group
                     (found via hash(group.id) % 50, from Part 4)

Config: max.poll.interval.ms = 5 minutes
```

**What `max.poll.interval.ms` means:** the Group Coordinator expects to hear a heartbeat from each consumer within this interval. If a consumer goes silent for longer than this, the coordinator assumes it's dead.

**The flow:**

1. Consumer 1 polls events and starts processing.
2. Processing takes a long time — say, **6 minutes** — because the consumer is busy handling a large or slow batch.
3. Since it's fully occupied processing, Consumer 1 **misses sending its heartbeat** within the configured 5-minute window.
4. The Group Coordinator, having received no heartbeat for 5 minutes, **assumes Consumer 1 is dead.**
5. The coordinator **reassigns Partition 0** (which Consumer 1 was handling) to another consumer in the group — say, **Consumer 3** — and **removes Consumer 1 from the group.**
6. Consumer 3 picks up Partition 0. It asks the coordinator for the **last committed offset** for this partition and **resumes processing from there.**
7. Meanwhile, the *original* Consumer 1 **eventually finishes** its 6-minute processing job and tries to commit its offset (e.g., "processed till 100" for Partition 0).
8. The Group Coordinator **rejects this commit** — because Consumer 1 has already been removed from the group and is no longer recognized as the owner of Partition 0.

```
Consumer 1                          Group Coordinator
   │                                        │
   │──poll events, start processing────────►│
   │                                        │
   │   (6 minutes of processing...)         │
   │   ⚠️ no heartbeat sent within 5 min    │
   │                                        │
   │                                        │──assumes Consumer 1 is dead
   │                                        │
   │                                        │──reassigns Partition 0 → Consumer 3
   │                                        │──removes Consumer 1 from group
   │                                        │
   │                         Consumer 3────►│ "what's the last committed offset
   │                                        │  for Partition 0?"
   │                                        │◄── returns last committed offset
   │                                        │
   │                        Consumer 3 resumes processing Partition 0
   │                                        │
   │──(finishes late) commit "till 100"────►│
   │                                        │
   │◄──────────── REJECTED ─────────────────│
   │   (Consumer 1 no longer in the group)  │
```

**Key distinction from Scenario 1:** here the consumer didn't crash — it was simply too slow, and the **coordinator's timeout mechanism** treated it as dead, reassigning its work out from under it. The late commit attempt gets rejected as a safety measure, since Consumer 3 has already taken over and may have made its own progress on that partition.

---

### Scenario 1 vs Scenario 2 — Side by Side

| Aspect | Scenario 1: Crash before commit | Scenario 2: Heartbeat timeout |
| --- | --- | --- |
| **Consumer state** | Actually crashed | Still alive, just slow/busy |
| **Detected by** | N/A — consumer restarts and asks for last offset itself | Group Coordinator (missing heartbeat past `max.poll.interval.ms`) |
| **What happens to the partition** | Same consumer resumes it after restart | Reassigned to a different consumer in the group |
| **Outcome** | Reprocessing from last committed offset (duplicates, no loss) | Original consumer's late commit is rejected; new consumer already resumed from last committed offset |
| **Root cause** | Failure/crash | Slow processing exceeding the heartbeat timeout config |

---

### Recap of Step 3

| Concept | Core takeaway |
| --- | --- |
| **Crash before commit** | Consumer restarts, asks Group Coordinator for last committed offset, resumes from there — reprocessing is expected, no data loss |
| **Heartbeat timeout** | A consumer that's alive but too slow to heartbeat within `max.poll.interval.ms` gets treated as dead — its partition is reassigned, and its late commit is rejected once it eventually finishes |
| **Why the late commit is rejected** | The Group Coordinator has already removed that consumer from the group and reassigned ownership — accepting a stale commit could conflict with the new owner's progress |

# 🎯Step 4: General Broker Failure (Combining Case)

---

### Question 5: What happens when a broker fails? (the general/combining case)

The instructor treats this as the **summary scenario** — it doesn't introduce anything new, it just combines the leader-failure mechanics (Step 1) and the follower-failure mechanics (Step 2) into one holistic picture, since a real broker typically holds a **mix** of leader and follower partition-replicas at once.

**Setup:**

```
Cluster: 3 brokers, 6 partitions, replication factor 3
```

Since a broker in a real cluster usually holds the **leader** for some partitions and a **follower** for others (recap from Part 2's Leader-Follower distribution), a broker failure has to be handled as **two things happening simultaneously**, not one.

**What happens:** Broker 1 fails.

**Who detects it:** The **Controller** — because, as covered throughout this series, every broker sends heartbeats to the active Controller. Once Broker 1 stops sending heartbeats, the Controller notices.

**The recovery flow — two parallel actions:**

**1. For every partition where Broker 1 was the leader:**

- The Controller triggers a **new leader election** from the remaining ISR members for that partition (identical mechanism to Step 1's leader-failure case).
- The Controller updates the metadata with the new leader.

**2. For every partition where Broker 1 was a follower:**

- The Controller **removes Broker 1 from the ISR** of that partition (identical mechanism to Step 2's follower-failure case).

**Worked example (instructor's numbers):**

```
Partition 2's ISR before failure: [Broker 1, Broker 2, Broker 3]
   Broker 1 = Leader
   Broker 2 = Follower
   Broker 3 = Follower

Broker 1 was ALSO a follower for a different partition, say Partition 5.
```

```
Broker 1 ──────► CRASHES
        │
        ▼
Controller detects: no heartbeat from Broker 1
        │
        ▼
        ├──► For partitions where Broker 1 was LEADER (e.g. Partition 2):
        │        New leader elected from ISR (e.g. Broker 2)
        │        Metadata updated: Leader = Broker 2
        │
        └──► For partitions where Broker 1 was FOLLOWER (e.g. Partition 5):
                 Broker 1 removed from ISR
                 Metadata updated: ISR no longer includes Broker 1
        │
        ▼
Controller pushes all updated metadata to every broker in the cluster
```

**Once the metadata is propagated:** every broker in the cluster now knows the current leader/ISR state for all affected partitions, and producers/consumers reconnect to the correct (new) leaders exactly as in Step 1.

**Impact:** The system **continues normally** — no data loss, and recovery happens **automatically**, without any manual intervention. This is precisely the "no single point of failure" guarantee from Part 2's Kafka Cluster definition, now shown working end-to-end.

---

### Recap of Step 4

| Scenario | Mechanism | Impact |
| --- | --- | --- |
| **Broker fails (general case)** | Controller detects missing heartbeat → for partitions where it was leader, elects a new leader from ISR; for partitions where it was follower, removes it from ISR → propagates updated metadata to all brokers | No data loss, fully automatic recovery |

---

## End of Part 5 — Full Summary

| Scenario | Detected by | Recovery mechanism | Outcome |
| --- | --- | --- | --- |
| **Active Controller fails** | Standby controllers (missing heartbeats) | Raft election among standbys → majority vote elects new active controller | Cluster keeps working, minimal disruption |
| **Leader partition fails** | Controller | New leader elected from remaining ISR members; metadata updated and propagated | No data loss; clients reconnect to new leader |
| **Follower partition fails** | The leader (missing fetch requests) | Leader removes follower from ISR, reports to Controller; rejoins ISR after catching up | No issues while down; writes continue via remaining ISR |
| **"Topic fails"** | — (trick question) | Topics are logical only — can't fail; only partitions (leader/follower) can | N/A |
| **Consumer crashes before commit** | Consumer itself, on restart | Asks Group Coordinator for last committed offset, resumes from there | Reprocessing/duplicates possible, no data loss |
| **Consumer heartbeat timeout** | Group Coordinator (`max.poll.interval.ms` exceeded) | Partition reassigned to another consumer; original consumer's late commit rejected | New consumer resumes from last committed offset; old consumer's stale commit discarded |
| **Broker fails (general)** | Controller | Combines leader-failure + follower-failure handling across all affected partitions on that broker | No data loss, fully automatic recovery |

**Interview-ready one-liners from this lecture:**

- *"When the active controller fails, the standby controllers detect the missing heartbeats and run a Raft election — whichever gets a majority becomes the new active controller."*
- *"When a leader partition fails, the controller elects a new leader from the ISR and updates cluster metadata — clients automatically reconnect to the new leader."*
- *"When a follower fails, it's the leader — not the controller — that first detects it, via missing fetch requests, and removes it from the ISR."*
- *"A topic can never fail — it's a logical concept. Only partitions, as leaders or followers, can fail."*
- *"A consumer crash before commit causes safe reprocessing from the last committed offset; a heartbeat timeout causes reassignment and rejects the original consumer's late commit."*
- *"A broker failure is really just leader-failure and follower-failure happening together, across every partition that broker was involved in."*

---

## 🎯 End of the Kafka Architecture Series

With this, all 5 lectures of the **Kafka Architecture** series are fully documented:

| Part | Topics |
| --- | --- |
| **Part 1** | Producer, Topic, Partition (Physical/Ordered/Append-only), Segments, Index files, Partitioning algorithms, Broker |
| **Part 2** | Consumer, Consumer Groups, `_consumer_offsets`, Offset Commit Strategies, Kafka Cluster, Leader-Follower Partitions |
| **Part 3** | Controller, ZooKeeper vs KRaft, Quorum & Raft Consensus, ISR, Acknowledgment Levels |
| **Part 4** | End-to-end Producer Write Flow, Consumer Read Flow, Log Retention (Delete/Compact), Kafka Speed Optimizations |
| **Part 5** | Failure Scenarios — Controller, Leader/Follower Partition, Topic, Consumer, Broker |

The instructor's own closing note: with the architecture fully understood, the **next step in the playlist is hands-on implementation** — his framing is that once you know broker, topic, partition, cluster, leader/follower, ISR, and all the failure mechanics this deeply, actually writing producer/consumer code and working with Kafka directly becomes "just connecting the dots."