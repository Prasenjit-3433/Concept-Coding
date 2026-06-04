# Issue #1244 — Complete STAR Story (Interview-Ready)

---

## SITUATION

To explain this properly to an interviewer, you need to tell a story that starts from Kafka, moves to AutoMQ, and then lands on the specific problem. Here it is:

---

### The Kafka Background the Interviewer Needs First

"Let me give you some context on the project before I explain the problem I solved."

```
Standard Kafka — how traffic flows across brokers:
────────────────────────────────────────────────────────────────

A Kafka topic has multiple partitions.
Each partition has a leader broker.
The Kafka producer sends messages round-robin
across all partitions by default.

Topic: orders  (6 partitions, 3 brokers)

  Broker 1          Broker 2          Broker 3
  ────────          ────────          ────────
  partition-0       partition-1       partition-2
  (leader)          (leader)          (leader)
  partition-3       partition-4       partition-5
  (leader)          (leader)          (leader)

Producer sends:
  msg1 → partition-0 (broker1)
  msg2 → partition-1 (broker2)
  msg3 → partition-2 (broker3)
  msg4 → partition-3 (broker1)
  ... round-robin forever

Result: Traffic is EVENLY spread.
Each broker handles roughly 1/3 of total traffic.
```

This is the default, happy-path Kafka behavior. Traffic is balanced.

---

### The Problem Kafka Has With Scaling

Now here's where Kafka struggles:

```
Scenario: Traffic suddenly spikes 5x on broker 1
─────────────────────────────────────────────────────────────

Maybe a specific consumer group is consuming heavily
from broker 1's partitions.
Maybe a batch job is writing specifically to certain topics.
Maybe a misconfigured producer is routing to specific keys.

Result:
  Broker 1  ████████████████████  (overloaded, 80% CPU)
  Broker 2  ████░░░░░░░░░░░░░░░░  (underloaded, 20% CPU)
  Broker 3  ████░░░░░░░░░░░░░░░░  (underloaded, 20% CPU)

What does Kafka do about this?
──────────────────────────────
NOTHING automatically.

In standard Kafka, you have to:
  Step 1: Manually detect the imbalance (via JMX metrics)
  Step 2: Decide which partitions to move
  Step 3: Run kafka-reassign-partitions.sh manually
  Step 4: Wait hours for data to physically copy
          from broker1's disk to broker2's disk
  Step 5: Traffic finally balances

This is painful, slow, and requires human intervention.
For a 10TB partition, step 4 alone can take hours.
In a production outage, every minute matters.
```

---

### How AutoMQ Solves This — The Key Differentiator

```
AutoMQ's core architectural difference:
────────────────────────────────────────────────────────────────

In Kafka:
  Data lives on LOCAL DISK inside each broker.
  Moving a partition = physically copying gigabytes of data.
  Broker 1's disk → Network → Broker 2's disk
  Slow. Expensive. Disruptive.

In AutoMQ:
  Data lives in S3 (cloud object storage).
  Brokers are STATELESS — they don't own the data.
  S3 owns the data. Brokers just process it.

  Moving a partition in AutoMQ:
  = Just update the metadata record:
    "partition-0 is now handled by broker2 instead of broker1"
  = No data movement needed — data is already in S3!
  = Takes SECONDS, not hours.

This is AutoMQ's signature feature:
  AUTOMATIC PARTITION SELF-BALANCING

  Broker 1  ████████████████████  (overloaded detected!)
                    │
                    │ AutoMQ controller detects imbalance
                    ▼
  AutoMQ moves partition-0 and partition-3
  from broker1 to broker2 — in seconds
                    │
                    ▼
  Broker 1  ██████████░░░░░░░░░░  (balanced)
  Broker 2  ██████████░░░░░░░░░░  (balanced)
  Broker 3  ████░░░░░░░░░░░░░░░░  (balanced)

No human intervention.
No data copying.
Seconds, not hours.
```

---

### The Actual Problem That Created This Issue

```
AutoMQ wants to DEMONSTRATE this self-balancing to users.

The standard way to show it:
  Step 1: Create a topic with many partitions
  Step 2: Deliberately send ALL traffic to ONE broker
          (creating an artificial hotspot)
  Step 3: Watch AutoMQ detect the imbalance
  Step 4: Watch AutoMQ automatically rebalance in seconds

The tool for this is:
  kafka-producer-perf-test.sh
  (a standard Kafka benchmarking tool)

The PROBLEM:
────────────────────────────────────────────────────────────────
kafka-producer-perf-test.sh sends messages
round-robin to ALL partitions by DEFAULT.

You CANNOT tell it:
  "Only send to partitions on broker 1"

So you CANNOT create a controlled hotspot.
So you CANNOT easily trigger or demonstrate self-balancing.

It's like having a fire extinguisher but no way
to start a controlled fire to test it!

This is the gap Issue #1244 was created to fill.
```

---

## TASK

```
The task was to add a new command-line parameter:

  --broker <broker1,broker2,...>

to kafka-producer-perf-test.sh so that:

  BEFORE (no --broker):
  ─────────────────────────────────────────────────────────
  Messages go to ALL partitions round-robin.
  Traffic is evenly spread across ALL brokers.
  No hotspot. Self-balancing never triggers.

  AFTER (with --broker 1):
  ─────────────────────────────────────────────────────────
  Messages go ONLY to partitions whose leader is broker 1.
  ALL traffic concentrates on broker 1.
  Hotspot created. AutoMQ detects it.
  Self-balancing kicks in automatically in seconds.
  User watches AutoMQ do its magic live.

Concrete example of what it enables:
─────────────────────────────────────────────────────────────
  # Before my fix — no way to do this:
  bin/kafka-producer-perf-test.sh \
    --topic orders \
    --num-records 1000000 \
    --record-size 1024 \
    --throughput -1
  # ↑ sends evenly to all brokers — no hotspot

  # After my fix — hotspot on demand:
  bin/kafka-producer-perf-test.sh \
    --topic orders \
    --num-records 1000000 \
    --record-size 1024 \
    --throughput -1 \
    --broker 1           ← NEW: all traffic → broker 1
  # ↑ AutoMQ detects overload, rebalances in seconds ✅

The scope was deliberately narrow:
  → One Java file to modify: ProducerPerformance.java
  → One test file to create: BrokerBoundPartitionerTest.java
  → Zero changes to AutoMQ's storage engine
  → Zero changes to the shell script itself
  → Fully backward compatible — existing behavior unchanged
```

---

## ACTION

This is what you actually did, in the order you did it:

---

### Step 1 — Read and Understood the Codebase

```
ProducerPerformance.java is a 661-line Java file
that is the backbone of kafka-producer-perf-test.sh.

Key things I found while reading it:

FINDING 1: How CLI arguments are defined
  The file uses the argparse4j library.
  Every argument follows the same pattern:
    parser.addArgument("--topic")
        .action(store())
        .required(true)
        .type(String.class)
        .dest("topicName")
        .help("...");
  → My --broker argument would follow this exact pattern.

FINDING 2: How the producer is created
  KafkaProducer<byte[], byte[]> producer =
      createKafkaProducer(config.producerProps);
  → producerProps is a Properties object.
  → I can inject a custom partitioner into it
    before the producer is created.
    This is the standard Kafka way to control routing.

FINDING 3: How messages are sent
  record = new ProducerRecord<>(config.topicName, payload);
  producer.send(record, cb);
  → The record has NO explicit partition set.
  → Partition selection is entirely delegated to the partitioner.
  → This means: plug in the right partitioner =
    messages go exactly where I want. Clean.

FINDING 4: The Partitioner interface
  Kafka's producer has a built-in extension point:
  ProducerConfig.PARTITIONER_CLASS_CONFIG
  → Set this to a custom class that implements Partitioner
  → Kafka calls partition() on every send()
  → Your code decides which partition number to return
  → This is the correct, idiomatic way to control routing.
    No hacks. No workarounds. Clean Kafka API.
```

---

### Step 2 — Designed the Solution

```
The design had 3 parts:

PART A: Add --broker to the argument parser
  → Optional argument (not required — backward compat)
  → Type: String (comma-separated, e.g. "1,2,3")
  → Passed to producerProps when specified

PART B: Wire it into the producer config
  → If --broker is given: inject BrokerBoundPartitioner
  → If --broker is not given: do nothing (existing behavior)

PART C: Implement BrokerBoundPartitioner
  The key design decisions I made:

  Decision 1: WHERE to get cluster topology?
  ────────────────────────────────────────────
  I need to know "which partitions live on which broker?"
  The Kafka Partitioner interface provides a Cluster object
  in its partition() method — this has full topology.
  So I resolve eligible partitions on the FIRST call to
  partition(), not during configure() where Cluster is
  not yet available.

  Decision 2: HOW to distribute among eligible partitions?
  ──────────────────────────────────────────────────────────
  Round-robin across all eligible partitions.
  Reason: if broker 1 has 3 partitions, I want all 3
  to receive traffic — not just one.
  This creates the most realistic hotspot scenario.

  Decision 3: WHAT to do if no partitions match?
  ────────────────────────────────────────────────
  Throw a clear IllegalArgumentException with:
  - Which broker IDs were requested
  - Which broker IDs are actually available
  Reason: silent failure here would be very confusing.
  The user would see messages being sent but AutoMQ's
  self-balancing never triggering — very hard to debug.
```

---

### Step 3 — Wrote the Implementation

**The `--broker` argument (in `argParser()`):**

```java
parser.addArgument("--broker")
    .action(store())
    .required(false)
    .type(String.class)
    .metavar("BROKER_IDS")
    .dest("broker")
    .help("Comma-separated broker IDs (e.g. 1,2,3). " +
          "Messages will only be sent to partitions whose " +
          "leader is one of the specified brokers, creating " +
          "partition hotspots to trigger AutoMQ self-balancing.");
```

**The wiring logic (in `start()`, before producer creation):**

```java
String brokerIds = config.getString("broker");

if (brokerIds != null && !brokerIds.isBlank()) {
    config.producerProps.put(
        ProducerConfig.PARTITIONER_CLASS_CONFIG,
        BrokerBoundPartitioner.class.getName()
    );
    config.producerProps.put(
        BrokerBoundPartitioner.BROKER_IDS_CONFIG,
        brokerIds
    );
}
```

**The `BrokerBoundPartitioner` (static inner class):**

```java
public static class BrokerBoundPartitioner implements Partitioner {

    public static final String BROKER_IDS_CONFIG =
        "broker.bound.partitioner.brokers";

    private List<Integer> targetBrokerIds;
    private List<Integer> eligiblePartitions = null;
    private int counter = 0;

    @Override
    public void configure(Map<String, ?> configs) {
        String brokerIdsStr = (String) configs.get(BROKER_IDS_CONFIG);
        try {
            this.targetBrokerIds = Arrays.stream(brokerIdsStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid broker IDs: '" + brokerIdsStr +
                "'. Expected comma-separated integers, e.g. '1,2,3'", e);
        }
    }

    @Override
    public int partition(String topic,
                         Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes,
                         Cluster cluster) {
        if (eligiblePartitions == null) {
            resolveEligiblePartitions(topic, cluster);
        }
        return eligiblePartitions.get(
            Math.abs(counter++ % eligiblePartitions.size())
        );
    }

    private void resolveEligiblePartitions(String topic, Cluster cluster) {
        this.eligiblePartitions = cluster.partitionsForTopic(topic)
            .stream()
            .filter(p -> p.leader() != null &&
                         targetBrokerIds.contains(p.leader().id()))
            .map(PartitionInfo::partition)
            .sorted()
            .collect(Collectors.toList());

        if (eligiblePartitions.isEmpty()) {
            String available = cluster.nodes().stream()
                .map(n -> String.valueOf(n.id()))
                .sorted()
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                "No partitions found on broker(s) " + targetBrokerIds +
                " for topic '" + topic + "'. " +
                "Available broker IDs: [" + available + "]");
        }

        System.out.println("BrokerBoundPartitioner: routing to " +
            eligiblePartitions.size() + " partition(s) " +
            eligiblePartitions + " on broker(s) " + targetBrokerIds);
    }

    @Override
    public void close() {}
}
```

---

### Step 4 — Wrote 8 Unit Tests

```
Tests covered:

TEST 1: Happy path
  → --broker 1,3 → only partitions on broker 1 and 3
    are chosen across 30 sends
  → partitions on broker 2 are never chosen

TEST 2: Round-robin distribution
  → --broker 2 (owns 2 partitions)
  → 20 sends → each partition gets exactly 10

TEST 3: Single eligible partition
  → --broker 3 (owns only 1 partition)
  → all 10 sends return the same partition

TEST 4: Non-existent broker
  → --broker 99
  → throws IllegalArgumentException
  → error message contains "99" and "Available broker IDs"

TEST 5: Non-numeric broker ID
  → --broker "broker-one"
  → configure() throws IllegalArgumentException

TEST 6: Argument parsing
  → --broker "1,2,3" parses to string "1,2,3" correctly

TEST 7: Backward compatibility
  → No --broker argument
  → result.getString("broker") returns null
  → existing behavior unchanged

TEST 8: Whitespace tolerance
  → --broker "1, 2, 3" (with spaces)
  → parses correctly, spaces are trimmed
```

---

## RESULT

```
┌────────────────────────────────────────────────────────────────┐
│                        THE RESULT                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Code impact:                                                  │
│  → ProducerPerformance.java: ~60 lines added                   │
│  → BrokerBoundPartitionerTest.java: ~150 lines, 8 tests        │
│  → All existing tests continue to pass                         │
│  → Zero breaking changes                                       │
│                                                                │
│  User impact:                                                  │
│  → AutoMQ users can now run:                                   │
│    --broker 1                                                  │
│    to instantly create a hotspot on broker 1                   │
│    and watch AutoMQ's self-balancing kick in                   │
│    within seconds — live, in front of a demo audience          │
│                                                                │
│  What this demonstrates to an interviewer:                     │
│  → You read real production-grade code (661 lines)             │
│  → You used the correct Kafka extension point (Partitioner)    │
│  → You thought about edge cases before coding                  │
│  → You wrote production-quality tests                          │
│  → You contributed to a real open source project               │
│    with a CEO invitation                                       │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## How to Explain This in an Interview

Here's the natural flow when an interviewer asks:

```
Interviewer: "Tell me about a challenging technical contribution
              you made."

You:
  "I contributed to AutoMQ — a cloud-native alternative to
   Apache Kafka. Let me give you a bit of context first.

   In standard Kafka, when traffic becomes unbalanced —
   say one broker gets overloaded — you have to manually
   reassign partitions, which involves physically copying
   gigabytes of data across the network. This can take hours.

   AutoMQ solves this by storing all data in S3 instead of
   on local broker disks. Because brokers are stateless,
   moving a partition is just a metadata update — it takes
   seconds, not hours. And AutoMQ does this automatically,
   detecting hotspots and rebalancing without human
   intervention.

   The problem I solved was: AutoMQ needed a way to
   *demonstrate* this self-balancing feature, but the
   standard Kafka performance tool sends messages evenly
   across all brokers — you couldn't create a controlled
   hotspot to trigger the rebalancing.

   So I added a --broker parameter to the tool that lets
   you say 'send all messages only to broker 1.' This
   creates an artificial hotspot, AutoMQ detects it, and
   you can watch it automatically rebalance in seconds
   right in front of you.

   Technically, I implemented a custom Kafka Partitioner
   that queries cluster metadata at runtime to find which
   partitions are led by the specified brokers, then
   round-robins messages among those partitions only.
   I also made sure it fails with a clear, actionable
   error message if you specify a broker that doesn't
   exist."

Interviewer: "Why a Partitioner and not something else?"

You:
  "Kafka's Producer API has a built-in extension point
   exactly for this — the Partitioner interface. You
   implement one method: partition(), which receives the
   full cluster topology and returns a partition number.
   It's the idiomatic, clean way to control routing.
   The alternative would have been interceptors, but those
   can't change the partition — they run after the partition
   is already assigned. So Partitioner was the right tool."

Interviewer: "How did you handle the case where the
              specified broker doesn't exist?"

You:
  "I throw an IllegalArgumentException with a message that
   tells the user both which broker they asked for AND
   which broker IDs are actually available in the cluster.
   Silent failure here would be very confusing — you'd see
   messages being sent but the self-balancing never
   triggering, with no indication of why."
```

---

Ready to move to **Issue #666**? Say **"next issue"**!