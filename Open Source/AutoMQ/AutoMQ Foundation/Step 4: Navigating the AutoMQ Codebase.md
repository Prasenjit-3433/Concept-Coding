# Step 4: Navigating the AutoMQ Codebase

Now let's make you dangerous in this repo. We go from top-level structure → finding your exact files → reading Issue #1244's code in detail.

---

## 4.1 The Repo's Top-Level Structure

AutoMQ is a **fork of Apache Kafka**. Most of the repo IS Kafka — AutoMQ only modified specific modules. This is important: don't panic when you see hundreds of folders. Most of them you'll never touch.

```
automq/  (root)
│
├── tools/              ← ✅ YOUR FIRST TARGET (Issue #1244)
│   └── src/main/java/org/apache/kafka/tools/
│       └── ProducerPerformance.java  ← exact file to modify
│
├── s3stream/           ← AutoMQ's custom storage engine (S3Stream)
│   └── src/main/java/com/automq/stream/
│       ├── s3/
│       │   ├── wal/
│       │   │   └── impl/block/
│       │   │       └── BlockWALService.java  ← Issue #1578 (skip)
│       │   └── objects/                      ← S3 object management
│       └── api/                              ← Stream API interfaces
│
├── core/               ← Scala code — Kafka broker core
│   └── src/main/scala/kafka/log/
│       └── streamaspect/
│           └── ElasticLog.scala   ← Issue #1842 lives here
│
├── storage/            ← S3 Storage Adapter layer
│   └── src/main/java/org/apache/kafka/storage/
│
├── clients/            ← Standard Kafka client code
│   └── src/main/java/org/apache/kafka/clients/
│       └── producer/   ← KafkaProducer, Partitioner etc.
│
├── server/             ← Broker server logic
├── metadata/           ← KRaft metadata management
├── connect/            ← Kafka Connect (not relevant to you)
├── streams/            ← Kafka Streams (not relevant to you)
└── docker/             ← Quick start docker-compose
```

**Key insight from the AutoMQ team themselves:**

AutoMQ consists of these key components: an S3 Storage Adapter — an adapter layer that reimplements `UnifiedLog`, `LocalLog`, and `LogSegment` classes to create logs on S3 instead of a local disk; and S3Stream — a shared streaming storage library that encapsulates various storage modules, including WAL and object storage.

So the repo splits cleanly into:
- **Standard Kafka code** (tools, clients, streams, connect) → barely modified
- **AutoMQ's custom code** (s3stream, core/streamaspect, storage adapter) → heavily modified

---

## 4.2 The "Layer Cake" — Where Each Issue Lives

```
┌───────────────────────────────────────────────────────┐
│            LAYER 1: Tools & CLI                       │
│   tools/  → ProducerPerformance.java                  │
│                                                       │
│   Issue #1244 lives HERE ✅                            │
│   Pure Java, Kafka API only, no AutoMQ internals      │
└───────────────────────────────────────────────────────┘
                        │
┌───────────────────────────────────────────────────────┐
│            LAYER 2: Kafka Broker (mostly Kafka)       │
│   server/, metadata/, clients/                        │
│                                                       │
│   Issue #666 (JMX) lives HERE ✅                       │
│   Issue #835 (OTel logging) lives HERE ✅              │
│   Metrics/logging hooks, minimal AutoMQ knowledge     │
└───────────────────────────────────────────────────────┘
                        │
┌───────────────────────────────────────────────────────┐
│            LAYER 3: Storage Adapter (AutoMQ Scala)    │
│   core/src/main/scala/kafka/log/streamaspect/         │
│                                                       │
│   Issue #1842 lives HERE 🟡                           │
│   ElasticLog.scala — bridges Kafka ↔ S3Stream         │
└───────────────────────────────────────────────────────┘
                        │
┌───────────────────────────────────────────────────────┐
│            LAYER 4: S3Stream Engine (Deep AutoMQ)     │
│   s3stream/                                           │
│                                                       │
│   Issue #1578 lives HERE 🔴 (skip)                    │
│   BlockWALService, stream internals                   │
└───────────────────────────────────────────────────────┘
                        │
┌───────────────────────────────────────────────────────┐
│            LAYER 5: Object Storage (S3/MinIO)         │
│   External — AWS S3 or compatible                     │
└───────────────────────────────────────────────────────┘
```

You're working from top to bottom — exactly the right order.

---

## 4.3 Deep Dive Into Issue #1244 — The Actual Code

Here's exactly what the issue says:

The current `kafka-producer-perf-test.sh` cannot send messages to partitions on the specified broker. The plan is to add a new parameter `--broker <broker1,broker2,...>` to `kafka-producer-perf-test.sh` so messages will only be sent to the specified brokers, thereby creating partition hotspots and triggering AutoMQ's partition self-balancing.

Now let's understand the existing code structure so you know exactly where to make changes:

```
ProducerPerformance.java — Current Structure
─────────────────────────────────────────────

public class ProducerPerformance {

    public static void main(String[] args) throws Exception {
        new ProducerPerformance().start(args);
    }

    void start(String[] args) throws IOException {
        // 1. Parse CLI arguments  ← YOU ADD --broker HERE
        ArgumentParser parser = argParser();
        ConfigPostProcessor config = new ConfigPostProcessor(parser, args);

        // 2. Create KafkaProducer
        KafkaProducer<byte[], byte[]> producer = createKafkaProducer(config.producerProps);

        // 3. Send messages in a loop
        for (int i = 0; i < numRecords; i++) {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                topicName,      // topic
                null,           // partition (null = use partitioner)
                null,           // key
                payload         // value
            );
            producer.send(record, cb);
        }
    }

    // This method defines all CLI arguments
    static ArgumentParser argParser() {
        ArgumentParser parser = ArgumentParsers
            .newFor("producer-performance")
            .build();

        // Existing arguments:
        parser.addArgument("--topic")...
        parser.addArgument("--num-records")...
        parser.addArgument("--record-size")...
        parser.addArgument("--throughput")...
        parser.addArgument("--producer-props")...

        // ← YOU ADD --broker HERE
        return parser;
    }
}
```

---

## 4.4 What You Need to Build — Step by Step

Here's the complete mental model of your implementation:

```
WHAT --broker NEEDS TO DO:
───────────────────────────────────────────────────────

User runs:
  kafka-producer-perf-test.sh \
    --topic orders \
    --broker 1,3 \          ← NEW parameter you add
    --num-records 100000 \
    --record-size 1024 \
    --throughput -1

Effect:
  Messages ONLY go to partitions whose LEADER is
  broker 1 or broker 3.

  Normal Kafka (round-robin):     Your implementation:
  ─────────────────────────       ─────────────────────
  partition-0 (leader: broker1)   partition-0 ✅ send here
  partition-1 (leader: broker2)   partition-1 ❌ skip
  partition-2 (leader: broker3)   partition-2 ✅ send here
  partition-3 (leader: broker1)   partition-3 ✅ send here
  partition-4 (leader: broker2)   partition-4 ❌ skip
```

---

## 4.5 The Custom Partitioner — The Heart of the Implementation

The way to control which partition a message goes to in Kafka is through a **custom Partitioner**. You already know this from using Kafka Producer API. Here's exactly what it needs to do:

```java
/**
 * This is the new class you write inside ProducerPerformance.java
 * (as a static inner class, no need for a separate file)
 */
public static class BrokerBoundPartitioner implements Partitioner {

    private List<Integer> targetBrokerIds;    // [1, 3] from --broker param
    private List<Integer> eligiblePartitions; // partitions whose leader is in targetBrokerIds
    private int counter = 0;                  // for round-robin among eligible partitions

    public void configure(Map<String, ?> configs) {
        // Extract the target broker IDs from configs
        // (you'll pass them in via producer props)
        String brokerList = (String) configs.get("broker.bound.partitioner.brokers");
        this.targetBrokerIds = Arrays.stream(brokerList.split(","))
            .map(Integer::parseInt)
            .collect(Collectors.toList());
    }

    public int partition(String topic,
                         Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes,
                         Cluster cluster) {

        // Step 1: Get all partitions for this topic
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);

        // Step 2: Filter to only those whose leader is in our target broker list
        if (eligiblePartitions == null) {
            eligiblePartitions = partitions.stream()
                .filter(p -> p.leader() != null &&
                             targetBrokerIds.contains(p.leader().id()))
                .map(PartitionInfo::partition)
                .collect(Collectors.toList());

            if (eligiblePartitions.isEmpty()) {
                throw new RuntimeException(
                    "No partitions found for brokers: " + targetBrokerIds
                );
            }
        }

        // Step 3: Round-robin among eligible partitions
        return eligiblePartitions.get(
            Math.abs(counter++ % eligiblePartitions.size())
        );
    }

    public void close() {}
}
```

---

## 4.6 How All the Pieces Connect

```
COMPLETE IMPLEMENTATION PLAN:
──────────────────────────────────────────────────────────

File to modify:
  tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java

Change 1: Add --broker argument to argParser()
─────────────────────────────────────────────
  parser.addArgument("--broker")
      .action(store())
      .required(false)
      .type(String.class)
      .dest("broker")
      .metavar("BROKER_IDS")
      .help("Comma-separated list of broker IDs to send messages to. " +
            "Creates partition hotspots to trigger AutoMQ self-balancing.");

Change 2: Read the parsed --broker value in start()
────────────────────────────────────────────────────
  String brokerIds = config.getString("broker");  // null if not specified

Change 3: If --broker is specified, inject the custom partitioner
──────────────────────────────────────────────────────────────────
  if (brokerIds != null) {
      config.producerProps.put(
          ProducerConfig.PARTITIONER_CLASS_CONFIG,
          BrokerBoundPartitioner.class.getName()
      );
      config.producerProps.put(
          "broker.bound.partitioner.brokers",
          brokerIds
      );
  }

Change 4: Add BrokerBoundPartitioner as a static inner class
─────────────────────────────────────────────────────────────
  (the class we designed in section 4.5 above)

Change 5: Update kafka-producer-perf-test.sh (trivial)
────────────────────────────────────────────────────────
  The shell script just passes all args to ProducerPerformance.
  No changes needed — it already forwards all parameters.
```

---

## 4.7 The Full Flow After Your Change

```
User provides: --broker 1,3

        ProducerPerformance.start()
               │
               │ sees --broker 1,3
               ▼
        Injects BrokerBoundPartitioner
        into producerProps
               │
               ▼
        KafkaProducer created with
        BrokerBoundPartitioner
               │
               │ for each message send():
               ▼
        BrokerBoundPartitioner.partition()
               │
               │ queries cluster metadata
               │ finds partitions on broker 1 and 3
               ▼
        Returns only eligible partition IDs
               │
               ▼
        Message sent to broker 1 or broker 3 only
               │
               ▼
        Hotspot created on brokers 1 and 3
               │
               ▼
        AutoMQ's Auto Balancer detects imbalance
               │
               ▼
        AutoMQ automatically moves partitions
        to balance load ✅

        (This is the feature AutoMQ wants to showcase!)
```

---

## 4.8 Setting Up Your Local Environment

Before you write a single line of code, you need to be able to **build and run** the project. Here's the minimal path:

```
Step 1: Clone the repo
──────────────────────
  git clone https://github.com/AutoMQ/automq.git
  cd automq

Step 2: Check Java version (Kafka needs JDK 17)
────────────────────────────────────────────────
  java -version
  # Must be 17+

Step 3: Build just the tools module (fast — don't build everything)
────────────────────────────────────────────────────────────────────
  ./gradlew :tools:jar

  This only builds the tools module.
  Full build takes 20+ minutes. This takes ~2 minutes.

Step 4: Run the existing tests for ProducerPerformance
──────────────────────────────────────────────────────
  ./gradlew :tools:test \
    --tests "org.apache.kafka.tools.ProducerPerformanceTest"

  Understand what the existing tests verify
  before adding your own.

Step 5: Quick local run with Docker (optional but useful)
──────────────────────────────────────────────────────────
  # AutoMQ provides a single-node docker-compose
  curl -O https://raw.githubusercontent.com/AutoMQ/automq/refs/tags/1.5.5/docker/docker-compose.yaml
  docker compose -f docker-compose.yaml up -d

  # This runs AutoMQ + MinIO (S3-compatible) locally
  # You can test your --broker parameter against it
```

---

## 4.9 Finding Your Files in the Repo — Quick Reference Card

```
Issue   → File Path (from repo root)
──────────────────────────────────────────────────────────────────

#1244   → tools/src/main/java/org/apache/kafka/tools/
            ProducerPerformance.java
          tools/src/test/java/org/apache/kafka/tools/
            ProducerPerformanceTest.java   ← write tests here

#666    → server/src/main/java/...  (JMX metrics exposure)
          Look for: KafkaServer.java, BrokerServer.java,
          search for "JMX" in the codebase

#835    → Find where OTel SDK is initialized
          Search for: "OpenTelemetry", "OtelSdkConfig"
          Likely in: core/ or server/ module

#1842   → core/src/main/scala/kafka/log/streamaspect/
            ElasticLog.scala  (around line 625)
          Look for: destroy() method + KV store writes
```

---

## Step 4 Summary

```
┌────────────────────────────────────────────────────────────┐
│                  WHAT YOU NOW KNOW                         │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Repo Structure:                                           │
│  AutoMQ = Kafka fork + s3stream module + storage adapter   │
│  Most of Kafka is untouched                                │
│                                                            │
│  Issue #1244 — Exactly what to build:                      │
│  1. Add --broker CLI argument to argParser()               │
│  2. Read broker IDs from parsed args                       │
│  3. Inject BrokerBoundPartitioner into producerProps       │
│  4. Write BrokerBoundPartitioner inner class               │
│     → fetches cluster metadata                             │
│     → filters partitions by leader broker ID               │
│     → round-robins among eligible partitions               │
│                                                            │
│  Build command: ./gradlew :tools:jar (fast)                │
│  Test command:  ./gradlew :tools:test                      │
│  Local run:     docker-compose.yaml (MinIO + AutoMQ)       │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## What's Next

**Step 5** is where we go fully hands-on with Issue #1244:
- Reading the existing `ProducerPerformanceTest.java` so you understand the test patterns
- Writing the complete implementation together
- Writing the tests
- Understanding how to structure your first PR (commit message, PR description, what reviewers look for)

This is where you go from "I understand the problem" to "I have working code ready to submit."

Ready? Say **"next"**!