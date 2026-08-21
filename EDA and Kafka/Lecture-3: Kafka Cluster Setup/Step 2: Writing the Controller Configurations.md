# Step 2: Writing the Controller Configurations

Select: Done

## Where these files go

Both controller config files need to be placed inside the `/config` folder:

```
config/
   ├── controller1.properties   ← new, we write this
   ├── controller2.properties   ← new, we write this
   └── ... (existing templates)
```

**Setup decision (instructor's framing):** we're creating **dedicated controllers** — nodes whose *only* job is controller work, with no broker responsibilities. This mirrors what real production Kafka clusters typically do: separate the `*"brain"*` (controller/metadata coordination) from the "workhorse" (broker, serving producer/consumer traffic).

---

## `controller1.properties` — full file

```
process.roles=controller

node.id=1

listeners=CONTROLLER://:9093

controller.listener.names=CONTROLLER

controller.quorum.voters=1@localhost:9093,2@localhost:9193

# PLAINTEXT (no encryption). In production, we should use SSL.
listener.security.protocol.map=CONTROLLER:PLAINTEXT

log.dirs=/tmp/controller1-logs

# Default partitions when creating a topic without specifying
num.partitions=3
default.replication.factor=2
```

## `controller2.properties` — full file

```
process.roles=controller

node.id=2

listeners=CONTROLLER://:9193

controller.listener.names=CONTROLLER

controller.quorum.voters=1@localhost:9093,2@localhost:9193

# PLAINTEXT (no encryption). In production, we should use SSL.
listener.security.protocol.map=CONTROLLER:PLAINTEXT

log.dirs=/tmp/controller2-logs

# Default partitions when creating a topic without specifying
num.partitions=3
default.replication.factor=2
```

Both files are almost identical — the only differences are `node.id`, the port in `listeners`, and `log.dirs`. Now let's go through each property one at a time.

---

## Property-by-property breakdown

### `process.roles` — what job this node performs

Tells Kafka what role this specific node should play in the cluster.

| Value | Meaning |
| --- | --- |
| `controller` | Node only performs controller tasks |
| `broker` | Node only performs broker tasks |
| `controller,broker` | Node performs both (combined node) |

Since we want **dedicated** controllers, both files set `process.roles=controller`.

---

### `node.id` — unique identity within the cluster

Every node in a Kafka cluster — whether controller or broker — needs a **unique ID**. Controller1 gets `node.id=1`, Controller2 gets `node.id=2`. (Later, the brokers will get `3` and `4` — one ID space shared across the whole cluster.)

---

### `listeners` — which ports this node opens, and what to call them

```
listeners=CONTROLLER://:9093
```

**What this does internally:** conceptually, this is Kafka opening something like                              **`new ServerSocket(9093)`** — a *TCP socket* sitting and waiting for incoming connections.

- `CONTROLLER` — this is just a **logical label/name** you give this particular socket. You could name it anything (`C1`, `MYPORT`, etc.) — but as we'll see, this label has real significance later because it needs to be **referenced consistently** elsewhere.
- `:9093` — the port number. If you omit the IP (just `:9093`), Kafka listens on **every IP address** on the machine. If you want to bind to one specific IP, you'd write it as `CONTROLLER://192.168.1.10:9093`.

**A node can open multiple listeners at once**, each with its own name:

```
listeners=CONTROLLER://:9093,BROKER://:9092
```

This tells Kafka: open **two** sockets — one named `CONTROLLER` on port 9093, one named `BROKER` on port 9092. (Our dedicated controllers only need one — the `CONTROLLER` socket — since they never talk to producers/consumers.)

```
Controller1 node
   └──► listeners=CONTROLLER://:9093
              │
              ▼
        new ServerSocket(9093)
        (waiting for TCP connections, labeled "CONTROLLER")
```

---

### `controller.listener.names` — which listener to use for controller-to-controller talk

```
controller.listener.names=CONTROLLER
```

If a node has multiple listeners defined (say `CONTROLLER`, `INTERNAL`, `EXTERNAL`), this property tells Kafka: **among all the listeners defined, use the one named `CONTROLLER` specifically for controller-to-controller communication.**

Recall from the architecture series: multiple controllers stay in sync with each other (active/standby, sharing cluster metadata). This property is what tells the node *which* of its open ports to use for that specific traffic.

```
listeners=CONTROLLER://:9093, INTERNAL://:9092, EXTERNAL://:9094
controller.listener.names=CONTROLLER
```

→ "Out of these three sockets, use the `CONTROLLER` one whenever this node needs to talk to another controller."

---

### `controller.quorum.voters` — the static list of all controllers

```
controller.quorum.voters=1@localhost:9093,2@localhost:9193
```

Each controller maintains a **static list of every controller node** in the cluster — this is the KRaft quorum list from the architecture series (Part 3). The format is:

```
node.id@host:port
```

So here: Controller1 (`1@localhost:9093`) and Controller2 (`2@localhost:9193`) are both listed. **Both controller files contain the exact same list** — every controller needs to know about every other controller to participate in quorum voting (leader election, metadata commit agreement).

---

### `listener.security.protocol.map` — what security protocol each listener uses

```
listener.security.protocol.map=CONTROLLER:PLAINTEXT
```

This maps each **listener name** to a security protocol:

| Protocol | Meaning | Typical use |
| --- | --- | --- |
| `PLAINTEXT` | No encryption, no authentication | Local development / testing |
| `SSL` | TLS encryption | Production |

Here, we're saying: "the listener named `CONTROLLER` should use `PLAINTEXT`." Since we only have one listener right now, there's just one mapping — but this is a **comma-separated list**, so if there were multiple listeners (e.g., `BROKER` too), you could mix protocols: `CONTROLLER:PLAINTEXT,BROKER:SSL`.

**Important consistency rule:** since this maps to the *listener name*, both Controller1 and Controller2 need matching protocol settings for their `CONTROLLER` listener — otherwise one side expects encryption, and the other doesn't, and communication breaks.

---

### `log.dirs` — where this node's data lives on disk

```
log.dirs=/tmp/controller1-logs
```

The directory where this node persists its files — for a controller, this is where the                  `_***cluster _metadata.log***` (covered in the architecture series) actually gets written to disk. Controller2 uses a separate path: `/tmp/controller2-logs`.

---

### `num.partitions` and `default.replication.factor` — fallback defaults for topic creation

```
num.partitions=3
default.replication.factor=2
```

Recall from the architecture series: **the Controller is the one that decides** how many partitions a topic gets and what its replication factor is, when creating a topic. These two properties are the ***fallback* defaults** the Controller uses if a topic is created *without explicitly specifying* partition count or replication factor.

**Note:** these values *could* technically differ between Controller1 and Controller2's config files — but since only the **currently active** controller's decision matters at topic-creation time, whichever one is active at that moment is the one whose defaults apply.

---

### One property explicitly deferred (until Step 3)

The instructor notes: `offsets.topic.replication.factor` (default replication factor for the internal `_consumer_offsets` topic) does **not** belong in the controller config — that's a **broker** concern, since consumer-related information is handled by brokers, not controllers (per the architecture series). We'll see it in Step 3.

---

## Recap of Step 2

| Property | Purpose |
| --- | --- |
| `process.roles` | Declares this node as a dedicated `controller` |
| `node.id` | Unique ID for this node in the cluster |
| `listeners` | Opens a TCP socket on a given port, with a logical label (e.g., `CONTROLLER://:9093`) |
| `controller.listener.names` | Tells the node which labeled listener to use for controller↔controller communication |
| `controller.quorum.voters` | Static list of all controllers (`id@host:port`) — identical across every controller's config |
| `listener.security.protocol.map` | Maps each listener name to a security protocol (`PLAINTEXT` here, `SSL` in production) |
| `log.dirs` | Disk path where this node's cluster metadata log gets persisted |
| `num.partitions` / `default.replication.factor` | Fallback defaults used when a topic is created without explicit values |

---

Ready for Step 3 (`broker1.properties` / `broker2.properties`, including the `advertised.listeners` mechanism and the cluster-metadata JSON walkthrough) whenever you say the word.