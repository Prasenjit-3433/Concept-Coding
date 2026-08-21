# Step 1: Binary download + folder structure (bin/config/libs/logs)

Select: Done

# Kafka Cluster Setup — Step 1: Download Binary + Understand Folder Structure

## Why this lecture is different (instructor's framing)

Up until now, everything covered was **conceptual architecture** — how Kafka works internally. This lecture is the first **hands-on** one: actually standing up a real Kafka cluster with **2 brokers and 2 controllers** (dedicated, not combined), then creating a topic with partitions and testing it end-to-end.

The instructor is explicit: if you haven't watched the architecture series, watch that first — this lecture assumes you already know what a broker, controller, partition, and replication factor are. Everything here is just "putting that theory into actual running processes."

**What we're building today (from the PDF diagram):**

![image.png](Step%201%20Binary%20download%20+%20folder%20structure%20(bin%20con/image.png)

Note something important here, which will matter a lot in Steps 2–3: brokers and controllers exchange **heartbeats** with each other, and the Controller pushes **metadata updates** out to the brokers — this is the live version of everything from the architecture lectures (Controller elections, ISR, leader/follower) now expressed as actual network connections between real processes.

---

## Docker vs. Binary — why the instructor picks Binary

**Note from the instructor:** Kafka can also be run via a Docker image, but since Docker hasn't been covered yet in this series, the setup here uses the **raw binary download** instead.

This is actually the better learning path anyway — going binary-first forces you to understand *what* each configuration file and folder actually does, since you're wiring it up by hand. Once that's clear, running the same thing via Docker later becomes trivial — it's just packaging the same binary + config inside a container.

---

## Step 1: Download the Kafka Binary

![image.png](Step%201%20Binary%20download%20+%20folder%20structure%20(bin%20con/image%201.png)

Go to the official Kafka downloads page and grab the **latest binary release**.

- The instructor used **version 4.2.0**, released Feb 17, 2026.
- Download link format: `kafka_2.13-4.2.0.tgz` (plus `.asc` and `.sha512` for signature/checksum verification, if you want to confirm integrity).

Once downloaded and extracted, you'll see the following folder structure:

```
kafka_2.13-4.2.0/
   ├── bin/
   ├── config/
   ├── libs/
   ├── LICENSE
   ├── licenses/
   ├── logs/
   ├── NOTICE
   └── site-docs/
```

---

## Step 2: Understand Each Sub-folder

Two folders matter most for this setup: **`bin`** and **`config`**. The rest are supporting infrastructure.

### `bin/` — the executable scripts

![image.png](Step%201%20Binary%20download%20+%20folder%20structure%20(bin%20con/image%202.png)

This folder contains shell scripts that let you perform every Kafka operation directly from the command line — without needing a full producer/consumer application (e.g., a Spring Boot app) running yet.

**What these scripts let you do:**

- Start the controller server
- Start the broker server
- Create / delete a topic
- Publish a message
- Consume a message
- ...and more

**Key mental model:** these scripts are just **wrappers around Kafka's internal Java APIs**. Running `kafka-console-producer.sh`, for instance, isn't some separate tool — it's literally invoking the same underlying producer logic a real application would use, just exposed via CLI for convenience/testing.

```
bin/
   ├── kafka-server-start.sh        ← starts a broker or controller node
   ├── kafka-storage.sh             ← formats/attaches cluster ID to a node
   ├── kafka-topics.sh              ← create/delete/describe topics
   ├── kafka-console-producer.sh    ← publish messages via CLI
   ├── kafka-console-consumer.sh    ← consume messages via CLI
   ├── kafka-metadata-quorum.sh     ← check controller/cluster quorum status
   └── ... (many more)
```

We'll use `kafka-storage.sh`, `kafka-server-start.sh`, `kafka-topics.sh`, `kafka-console-producer.sh`, `kafka-console-consumer.sh`, and `kafka-metadata-quorum.sh` directly in this walkthrough.

### `config/` — the configuration templates

![image.png](Step%201%20Binary%20download%20+%20folder%20structure%20(bin%20con/image%203.png)

**This is the folder we'll spend the most time in**, since it holds all the configuration for controller and broker nodes.

```
config/
   ├── broker.properties         ← default template for a Broker node
   ├── consumer.properties       ← default template for a Consumer
   ├── controller.properties     ← default template for a Controller node
   ├── producer.properties       ← default template for a Producer
   ├── server.properties         ← default template for a node that is **BOTH** Controller    |                                                                       + Broker
   └── ... (other connect/tooling configs, not relevant here)   
```

**Important nuance the instructor stresses:** these are just **default templates** — you're not meant to use them as-is in a real setup. The convention (and what we'll do next) is to **write your own dedicated `.properties` files** for each node, tailored to its exact role, ID, and ports. `server.properties` exists specifically for the case where one node handles both controller and broker duties combined — but since this setup uses **dedicated** controllers and brokers (matching real production practice), we won't use it.

### Other folders (brief)

| Folder | Purpose |
| --- | --- |
| **`libs/`** | Contains the Kafka JAR files and their dependencies — the ***actual compiled code*** that the `bin/` scripts invoke under the hood |
| **`logs/`** | Where runtime application logs (INFO / WARN / ERROR messages) get written once brokers/controllers start running — useful for debugging startup issues |

---

## Recap of Step 1

| Concept | Core takeaway |
| --- | --- |
| **This lecture's scope** | Hands-on: stand up a real cluster — 2 dedicated brokers, 2 dedicated controllers — then create and test a topic |
| **Binary vs Docker** | Binary chosen here since Docker isn't covered yet; binary-first also builds *deeper understanding* |
| **`bin/`** | Executable shell scripts — wrappers around Kafka's internal Java APIs, used to start servers, manage topics, and test producer/consumer flows from the CLI |
| **`config/`** | Holds default `.properties` templates for broker/controller/consumer/producer/combined nodes — but real setups should define their own dedicated config files, not reuse the templates directly |
| **`libs/` and `logs/`** | Supporting folders — JAR dependencies and runtime logs, respectively |

---

Ready for Step 2 (writing `controller1.properties` and `controller2.properties`, property by property) whenever you say the word.