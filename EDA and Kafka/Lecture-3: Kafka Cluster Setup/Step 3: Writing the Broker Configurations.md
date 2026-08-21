# Step 3: Writing the Broker Configurations

Select: Done

# Step 3: Writing the Broker Configurations

## Where these files go

Same as before — both go inside `/config`:

```
config/
   ├── controller1.properties   (done)
   ├── controller2.properties   (done)
   ├── **broker1.properties**       ← **new**, we write this
   └── **broker2.properties**       ← **new**, we write this
```

---

## `broker1.properties` — full file

```
process.roles=broker

node.id=3

# WHERE to fetch cluster metadata from (which controllers to connect to)
controller.quorum.voters=1@localhost:9093,2@localhost:9193

# This is where our Spring Boot apps (producer/consumer) will connect
listeners=BROKER://:9092

# passed this address to controller to store in their cluster metadata
advertised.listeners=BROKER://localhost:9092

# internally when follower broker try to fetch from leader, which listener to connect
inter.broker.listener.name=BROKER

# when broker try to communicate with controller what listener they need to use.
controller.listener.names=CONTROLLER

listener.security.protocol.map=CONTROLLER:PLAINTEXT,BROKER:PLAINTEXT

log.dirs=/tmp/broker1-logs

# Keep messages for 7 days (168 hours)
log.retention.hours=168

# Max size of a single log segment file (1GB)
log.segment.bytes=1073741824

# __consumer_offsets topic RF
offsets.topic.replication.factor=2
```

## `broker2.properties` — full file

```
process.roles=broker

node.id=4

# WHERE to fetch metadata from (which controllers to connect to)
controller.quorum.voters=1@localhost:9093,2@localhost:9193

# This is where our Spring Boot apps (producer/consumer) will connect
listeners=BROKER://:9192

# passed this address to controller to store in their cluster metadata
advertised.listeners=BROKER://localhost:9192

# internally when follower broker try to fetch from leader, which listener to connect
inter.broker.listener.name=BROKER

# when broker try to communicate with controller what listener they need to use.
controller.listener.names=CONTROLLER

listener.security.protocol.map=CONTROLLER:PLAINTEXT,BROKER:PLAINTEXT

# Directory in which this node data will persist.
log.dirs=/tmp/broker2-logs

# Keep messages for 7 days (168 hours)
log.retention.hours=168

# Max size of a single log segment file (1GB)
log.segment.bytes=1073741824

# __consumer_offsets topic RF
offsets.topic.replication.factor=2
```

Node IDs continue from the controllers (`1`, `2` were controllers → brokers get `3`, `4`). Now, property by property — focusing especially on the two properties that are genuinely new concepts here: `advertised.listeners` and the two-fold use of `controller.listener.names`.

## 🎯Property-by-property breakdown

---

### `process.roles` and `node.id`

Same mechanics as controllers — `process.roles=broker` marks this as a **dedicated broker** (no controller duties), and `node.id` gives it a cluster-wide unique ID (`3` for Broker1, `4` for Broker2).

---

### `controller.quorum.voters` — brokers track controllers too

```
controller.quorum.voters=1@localhost:9093,2@localhost:9193
```

Just like every controller maintains this list, **every broker also maintains the same static list of controller nodes.** This is how a broker knows where to go to fetch cluster metadata — it needs to know the address of the controllers before it can ask them anything.

---

### `listeners` — the broker's client-facing port

```
listeners=BROKER://:9092
```

This opens a socket named `BROKER` on port 9092. **This is the port producers and consumers will actually connect to** — this is where your Spring Boot application (or, in our case today, the CLI producer/consumer scripts) will talk to this broker.

---

### `advertised.listeners` — the new, important concept

```
advertised.listeners=BROKER://localhost:9092
```

This is the property that needs the most careful unpacking, because it explains *how* a producer ever finds the right broker to talk to.

**The flow to understand:**

A broker can open multiple listeners on multiple addresses/ports. But whichever address it wants **clients to actually use to reach it**, it has to explicitly ***advertise*** that address to the Controller. The Controller stores this in its cluster metadata.

```
Broker1
   │
   │  "Hey Controller, store this: I'm reachable at localhost:9092"
   ▼
Controller's cluster metadata
   {
     "order-events, Partition 0": { "leader": "Broker1", "address": "localhost:9092" }
   }
```

**Now, here's the full picture — connecting back to the architecture series:**

```
Producer wants to publish to Topic-A, Partition 0
        │
        ▼
Producer asks ANY broker: "give me the metadata"
        │
        ▼
That broker forwards to the Active Controller
        │
        ▼
Controller returns cluster metadata:
   - Topic-A, Partition 0 → Leader = Broker1
   - Broker1's address    → localhost:9092   (from advertised.listeners)
        │
        ▼
Producer now connects DIRECTLY to localhost:9092 (Broker1)
```

So `advertised.listeners` is literally the answer to: *"once the Controller tells a client which broker is the leader, what address does it actually hand them to connect to?"* Without this, the Controller would have no way of knowing the broker's externally reachable address.

---

### `inter.broker.listener.name` — which listener brokers use to talk to each other

```
inter.broker.listener.name=BROKER
```

Recall from the architecture series: a **follower broker** continuously fetches from the **leader broker** to stay in sync (replication). This property tells Kafka: *"among all my defined listeners, use the one named `BROKER` specifically for broker-to-broker replication traffic."*

In our simplified setup, we've only opened **one** listener (`BROKER://:9092`), so it's doing double duty — serving producer/consumer clients *and* handling broker-to-broker replication. In a more advanced setup, you could split these:

```
listeners=EXTERNAL://:9092, INTERNAL://:9098

// for clients (producer, consumer)
advertised.listeners=EXTERNAL://:9092

// for inter-broker communication
inter.broker.listener.name=INTERNAL
```

This would mean: producers/consumers use `BROKER` (port 9092), but brokers talk to *each other* over a separate `INTERNAL` listener (port 9098) — keeping client traffic and internal replication traffic on different ports.

---

### `controller.listener.names` — two distinct uses on the broker side

```
controller.listener.names=CONTROLLER
```

This property serves **two purposes** for a broker:

1. **Security protocol mapping** — it tells the broker which listener name to look up in `listener.security.protocol.map` when it needs to talk to a controller.
2. **Picking the right endpoint from cluster metadata** — this is the more interesting one.

**Worked example (from the PDF) — why this matters: (**🕯️***watch this portion!*)**

Say the cluster metadata a broker receives looks like this:

```json
{
  "controllerId": 1,
  "endPoints": [
    { "name": "CONTROLLER", "host": "localhost", "port": 9093, "securityProtocol": 0 },
    { "name": "INTERNAL",   "host": "localhost", "port": 9097, "securityProtocol": 1 }
  ]
}
```

A controller can have **multiple endpoints/listeners** advertised, just like a broker can. So when a broker has decided it needs to talk to **Controller ID 1** (the currently active one), it can't just pick any endpoint — it specifically looks for the one whose `name` matches its own `controller.listener.names` value. Here, that's `"CONTROLLER"` → so it picks `localhost:9093`.

```
Broker needs to reach active Controller (ID: 1)
        │
        ▼
Looks up Controller 1's endpoints in cluster metadata
        │
        ▼
Filters for the endpoint named "CONTROLLER"  ← matches controller.listener.names
        │
        ▼
Connects to localhost:9093
```

**This is exactly why *every controller's config uses the same listener name* (`CONTROLLER`)** — so that no matter which controller is currently **active**, any broker can reliably find the right endpoint by name.

---

### `listener.security.protocol.map` — same concept as controllers, now with two listeners

```
listener.security.protocol.map=CONTROLLER:PLAINTEXT,BROKER:PLAINTEXT
```

Same idea as Step 2, just extended: since a broker has **two** kinds of communication happening — talking to controllers, and talking to producers/consumers/other brokers — both listener names need a protocol mapping. Both are `PLAINTEXT` here for local development.

---

### `log.dirs` — same concept as controllers

```
log.dirs=/tmp/broker1-logs
```

Where this broker's actual **topic/partition data** (segments, index files) gets persisted on disk. Broker2 uses a separate path: `/tmp/broker2-logs`.

---

### `log.retention.hours` and `log.segment.bytes` — retention config, from the architecture series

```
log.retention.hours=168
log.segment.bytes=1073741824
```

Direct callbacks to Part 4's retention discussion:

- `log.retention.hours=168` → keep data for **7 days** (168 hours). Recall: retention deletes whole **segments**, never individual events — once a segment's oldest data exceeds this age, the entire segment is deleted.
- `log.segment.bytes=1073741824` → each segment file caps out at **1 GB** (1073741824 bytes).

---

### `offsets.topic.replication.factor` — the property deferred from Step 2

```
offsets.topic.replication.factor=2
```

This is the default **replication factor for the internal `_consumer_offsets` topic** (recall: 50 partitions by default, from Part 2 of the architecture series). This belongs here in the broker config — not the controller config — because consumer offset tracking is broker territory.

---

## Recap of Step 3

| Property | Purpose |
| --- | --- |
| `process.roles` / `node.id` | Marks this as a dedicated `broker`, with a cluster-unique ID |
| `controller.quorum.voters` | Same static controller list as every controller/broker maintains |
| `listeners` | Opens the port producers/consumers actually connect to |
| `advertised.listeners` | The address the broker reports to the Controller, which then gets handed to clients — this is how producers/consumers ultimately find the right broker |
| `inter.broker.listener.name` | Which listener to use for broker↔broker replication (follower fetching from leader) |
| `controller.listener.names` | Which listener name to use for broker↔controller talk — both for security-protocol lookup and for picking the right endpoint out of a controller's (possibly multiple) advertised endpoints |
| `listener.security.protocol.map` | Security protocol per listener name — now covering both `CONTROLLER` and `BROKER` |
| `log.dirs` | Disk path for this broker's actual topic/partition data |
| `log.retention.hours` / `log.segment.bytes` | Retention policy — 7 days, 1 GB per segment |
| `offsets.topic.replication.factor` | Replication factor for the internal `_consumer_offsets` topic |

---

Ready for Step 4 (generating the cluster ID and formatting/mapping all 4 nodes to it via `kafka-storage.sh`) whenever you say the word.