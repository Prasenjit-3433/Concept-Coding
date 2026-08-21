# Step 6: Producer/consumer testing via CLI scripts

Select: Done

# Step 6: Testing with Producer & Consumer (CLI)

## Setup recap before testing

All four nodes are up and confirmed healthy from Step 5. Now the instructor wraps up by proving the cluster actually works — creating a topic, inspecting it, and publishing/consuming messages, all through the CLI scripts rather than a real application.

> **Instructor's framing:** today's goal was to set up the *cluster*, not to build a full producer/consumer application. Scripts are perfect for this kind of quick, no-code verification — a real Spring Boot producer/consumer app comes later.
> 

---

## Creating a Sample Topic

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic order-events-topic
```

**Two things worth noting here:**

1. **We're not specifying partition count or replication factor.** Since neither is passed explicitly, the **active Controller falls back to its configured defaults** — recall from Step 2, that's `num.partitions=3` and `default.replication.factor=2`.
2. **`-bootstrap-server localhost:9092`** — this is Broker1's client-facing port (from Step 3). This address is only needed as an **entry point**; the request itself doesn't get handled by this broker directly. Instead:

```
CLI request: "create topic order-events-topic"
        │
        ▼
Sent to localhost:9092 (Broker1, just an entry point)
        │
        ▼
Broker1 forwards the request to the Active Controller
        │
        ▼
Active Controller creates the topic, decides leader/follower
placement for all partitions, using its default.replication.factor=2
```

This is exactly the create-topic flow from the architecture series (Part 3) — clients never talk to the Controller directly, always through *any* broker.

---

## Describing the Topic

![image.png](Step%206%20Producer%20consumer%20testing%20via%20CLI%20scripts/image.png)

```bash
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic order-events-topic
```

**Sample output:**

```
Topic: order-events-topic   TopicId: FOBAR84DQkC4MbmWuaNk2A   PartitionCount: 3   ReplicationFactor: 2   Configs: min.insync.replicas=1,segment.bytes=1073741824

    Topic: order-events-topic   Partition: 0   Leader: 3   Replicas: 3,4   Isr: 3,4   Elr:   LastKnownElr:
    Topic: order-events-topic   Partition: 1   Leader: 4   Replicas: 4,3   Isr: 4,3   Elr:   LastKnownElr:
    Topic: order-events-topic   Partition: 2   Leader: 3   Replicas: 3,4   Isr: 3,4   Elr:   LastKnownElr:
```

**Reading this against what we configured:**

- **`PartitionCount: 3`, `ReplicationFactor: 2`** — exactly the controller defaults from Step 2, confirming the fallback worked as expected.
- Each partition row shows **Leader**, **Replicas**, and **Isr** — direct, tangible instances of the Leader-Follower and ISR concepts from the architecture series, now attached to real broker node IDs (`3` = Broker1, `4` = Broker2).
- Notice the leaders are split across both brokers (Partition 0 & 2 → Broker1 as leader; Partition 1 → Broker2 as leader) — this is the "no single broker holds everything" distribution rule in action.

```
order-events-topic
   ├── Partition 0 → Leader: Broker1 (3)   Replicas/ISR: [3, 4]
   ├── Partition 1 → Leader: Broker2 (4)   Replicas/ISR: [4, 3]
   └── Partition 2 → Leader: Broker1 (3)   Replicas/ISR: [3, 4]
```

---

## Producer Test

![image.png](Step%206%20Producer%20consumer%20testing%20via%20CLI%20scripts/image%201.png)

```bash
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-events-topic
```

This opens an interactive prompt where you can type messages line by line, and each becomes a published event:

```
>hello
>this is shrayansh
>how are you
```

**What's happening internally (tying back to the full producer write flow from Part 4):** since no key is provided here, partitioning likely falls back to round-robin (or a similar default) across the topic's partitions. The producer first fetches cluster metadata (via any broker), figures out which broker is the leader for the target partition, and then publishes directly to that leader.

---

## Consumer Test

![image.png](Step%206%20Producer%20consumer%20testing%20via%20CLI%20scripts/image%202.png)

```bash
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events-topic --from-beginning
```

**Output — reads back exactly what was produced:**

```
hello
this is shrayansh
how are you
```

The `--from-beginning` flag tells the consumer to start reading from the **earliest available offset**, rather than only new messages published after the consumer starts — this is the replay capability from the very first EDA lecture (Streaming model vs Pub/Sub), now demonstrated directly.

---

## Recap of Step 6

| Action | Command | What it confirms |
| --- | --- | --- |
| **Create topic** | `kafka-topics.sh ... --create` | Controller defaults (`num.partitions`, `default.replication.factor`) apply when not explicitly overridden |
| **Describe topic** | `kafka-topics.sh ... --describe` | Leader/Replica/ISR assignment is real and distributed across both brokers |
| **Produce** | `kafka-console-producer.sh` | Messages can be published directly via CLI, no application needed |
| **Consume** | `kafka-console-consumer.sh --from-beginning` | Messages are durably stored and replayable from the start of the log |

---

## End of Lecture — Full Summary

| Step | Core Idea |
| --- | --- |
| **Step 1** | Downloaded Kafka binary (v4.2.0); understood `bin/` (executable scripts) and `config/` (property templates) folders |
| **Step 2** | Wrote `controller1.properties` / `controller2.properties` — dedicated controller roles, quorum voter list, listener naming, log dirs, topic-creation defaults |
| **Step 3** | Wrote `broker1.properties` / `broker2.properties` — dedicated broker roles, `advertised.listeners` (how clients ultimately find the right broker), `inter.broker.listener.name`, retention config, `offsets.topic.replication.factor` |
| **Step 4** | Generated a cluster ID via `kafka-storage.sh random-uuid`; formatted all 4 nodes with `kafka-storage.sh format`, producing `meta.properties` for each |
| **Step 5** | Started controllers first, then brokers; verified leader election via `quorum-state` and overall cluster health via `kafka-metadata-quorum.sh describe --status` |
| **Step 6** | Created a topic (using controller defaults), described it (confirming leader/replica/ISR distribution), and tested end-to-end publish/consume via CLI scripts |

**Interview-ready one-liners from this lecture:**

- *"`advertised.listeners` is the address a broker reports to the Controller — it's exactly what a producer or consumer is ultimately handed to connect to the right leader broker."*
- *"Controllers must start before brokers, since a broker's first job on startup is fetching cluster metadata from the controllers."*
- *"`CurrentVoters` in `kafka-metadata-quorum.sh` output are the controllers (they vote in Raft elections); `CurrentObservers` are the brokers (they just consume metadata)."*
- *"A topic created without explicit partition/replication settings falls back to whatever `num.partitions` and `default.replication.factor` the active controller is configured with."*

---