# Step 4: Generating the Cluster ID & Mapping All Nodes

Select: Done

# Step 4: Generating the Cluster ID & Mapping All Nodes

## Why this step exists

So far, we've written four **individual** configuration files — `controller1.properties`, `controller2.properties`, `broker1.properties`, `broker2.properties`. Each one knows about *itself* and has a static list of the other controllers, but nothing yet ties all four together as **one single Kafka cluster**. That's exactly what this step does.

```
controller1.properties  ┐
controller2.properties  │   4 separate configs so far —
broker1.properties      │   not yet part of one cluster
broker2.properties      ┘
```

---

## Generate a Cluster ID

Kafka provides a script for this, inside `bin/`:

```bash
bin/kafka-storage.sh random-uuid
```

Running this generates a random UUID that will serve as this cluster's unique identity.

**Sample output (I ran it!):**

```
yKBE9lQhR6OPnwRYPZUAOQ
```

This is just a random string — every fresh cluster you ever create will get its own unique one.

---

## Attach the Cluster ID to Every Node

Now that we have a cluster ID, we need to **format** each of the four nodes' storage with it — this is what officially says "you four nodes now belong to this one cluster." The subcommand for this is `format`, using the same `kafka-storage.sh` script:

```bash
bin/kafka-storage.sh format -t <cluster-id> -c <path-to-node's-properties-file>
```

**Run this once per node — four times total:**

```bash
# 1. Controller1
bin/kafka-storage.sh format -t yKBE9lQhR6OPnwRYPZUAOQ -c config/controller1.properties

# 2. Controller2
bin/kafka-storage.sh format -t yKBE9lQhR6OPnwRYPZUAOQ -c config/controller2.properties

# 3. Broker1
bin/kafka-storage.sh format -t yKBE9lQhR6OPnwRYPZUAOQ -c config/broker1.properties

# 4. Broker2
bin/kafka-storage.sh format -t yKBE9lQhR6OPnwRYPZUAOQ -c config/broker2.properties
```

```
Cluster ID: yKBE9lQhR6OPnwRYPZUAOQ
        │
        ├──► format ──► controller1.properties  →  node.id=1 tagged with this cluster
        ├──► format ──► controller2.properties  →  node.id=2 tagged with this cluster
        ├──► format ──► broker1.properties      →  node.id=3 tagged with this cluster
        └──► format ──► broker2.properties      →  node.id=4 tagged with this cluster
```

🚨**A quick side note on a related workflow step:** if you're redoing a setup (as the instructor does when demonstrating this from scratch), you'd first stop any already-running servers and clear out their previously persisted data — since each node's `log.dirs` path already has files in it from an earlier run. Clearing those out (`rm -rf` on each `log.dirs` path) ensures the node starts completely fresh before formatting.

---

## What this produces: `meta.properties`

![image.png](Step%204%20Generating%20the%20Cluster%20ID%20&%20Mapping%20All%20Nod/image.png)

Running `format` creates a new file called **`meta.properties`** inside each node's `log.dirs` path.

```
/tmp/
   ├── controller1-logs/
   │      └── meta.properties   ← newly created
   ├── controller2-logs/
   │      └── meta.properties   ← newly created
   ├── broker1-logs/
   │      └── meta.properties   ← newly created
   └── broker2-logs/
          └── meta.properties   ← newly created
```

**Example contents (Controller1's `meta.properties`):**

```
#
#Thu Feb 26 22:04:56 IST 2026

cluster.id=zJVVVZAkS9aIBfjY0BlYJA

directory.id=LYSuEbfGps8gnV7YsC9JHA

node.id=1

version=1
```

Notice: this file records the **same `cluster.id`** we generated, alongside this node's own `node.id` (1, in this case) and a unique `directory.id`. Every one of the four nodes will have its own `meta.properties`, but all four will share that identical `cluster.id` — that shared value is what actually binds them together as one cluster.

---

## Recap of Step 4

| Concept | Core takeaway |
| --- | --- |
| **`kafka-storage.sh random-uuid`** | Generates a new, random cluster ID — one per cluster |
| **`kafka-storage.sh format -t <id> -c <config>`** | Attaches a given cluster ID to a specific node's config — run once per node (4 times here: 2 controllers + 2 brokers) |
| **`meta.properties`** | Auto-created inside each node's `log.dirs` after formatting; stores that node's `cluster.id`, `node.id`, and a `directory.id` |
| **Why this matters** | This is the actual mechanism that turns 4 independently-configured nodes into one cohesive Kafka cluster — they all now reference the same `cluster.id` |

---

Ready for Step 5 (starting all four servers in the correct order, and verifying cluster status via `kafka-metadata-quorum.sh`) whenever you say the word.