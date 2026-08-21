# Step 5: Starting the servers (order matters) + verifying cluster status

Select: Done

# Step 5: Starting the Servers & Verifying Cluster Status

## Why order matters

All four nodes are now formatted and tied to the same cluster ID — but nothing is actually **running** yet. The instructor is explicit about the startup order here:

> **Controllers first, then brokers.**
> 

**Why:** when a broker starts up, one of the first things it does is reach out to the controllers to **load the cluster metadata**. If the controllers aren't up yet, the broker has nowhere to fetch that metadata from. So the controllers need to already be running and have elected an active leader among themselves before any broker starts.

```
Startup order:
   1. Controller1 ─┐
   2. Controller2 ─┴──► must be up first (they elect a leader among themselves)
   3. Broker1     ─┐
   4. Broker2     ─┴──► started after, since they need to fetch metadata from 
											   controllers
```

---

## Starting the Controllers

Both use the `kafka-server-start.sh` script, passing the node's own properties file:

```bash
# Controller1:
bin/kafka-server-start.sh config/controller1.properties

# Controller2:
bin/kafka-server-start.sh config/controller2.properties
```

Once both are up, they immediately begin their Raft-based coordination (from the architecture series — Part 3) to figure out which one becomes the **active** controller and which stays **standby**.

---

## Checking Who Became the Leader (Controller Side)

![image.png](Step%205%20Starting%20the%20servers%20(order%20matters)%20+%20veri/image.png)

You can inspect this directly by looking inside either controller's `log.dirs` path — since the cluster metadata is synced between both, checking either one gives the same answer.

```
/tmp/controller2-logs/
   └── __cluster_metadata-0/
          ├── 0000000000...000000.index
          ├── 0000000000...000000.log
          ├── 0000000000...000.timeindex
          ├── leader-epoch-checkpoint
          ├── partition.metadata
          └── quorum-state          ← this file tells us who's leader
```

Opening `quorum-state` shows something like:

```json
{"clusterId":"","leaderId":1,"leaderEpoch":1,"votedId":-1,"appliedOffset":0,"currentVoters":[{"voterId":1},{"voterId":2}],"data_version":0}
```

**`leaderId: 1`** tells us **Controller with ID 1 is the active leader** — this file persists that decision so it's checkable at any time, not just something you'd have to catch in the logs at the moment of election.

---

## Starting the Brokers

![image.png](Step%205%20Starting%20the%20servers%20(order%20matters)%20+%20veri/image%201.png)

Same script, now pointed at the broker config files:

```bash
# Broker1:
bin/kafka-server-start.sh config/broker1.properties
```

![image.png](Step%205%20Starting%20the%20servers%20(order%20matters)%20+%20veri/image%202.png)

```bash
# Broker2:
bin/kafka-server-start.sh config/broker2.properties
```

On successful startup, you'll see log output confirming each broker came up and registered with its node ID — e.g., `Kafka Server started, node.id=3` for Broker1 and `node.id=4` for Broker2, matching what we set in Step 3.

---

## Verifying the Full Cluster Status

![image.png](Step%205%20Starting%20the%20servers%20(order%20matters)%20+%20veri/image%203.png)

Kafka provides a dedicated script for this — `kafka-metadata-quorum.sh` — and, as the instructor notes, **you don't need to memorize this command**; it's the kind of thing you look up when needed. What matters is understanding *what* it's telling you.

```bash
bin/kafka-metadata-quorum.sh --bootstrap-controller localhost:9093 describe --status
```

**Sample output:**

```
ClusterId:              zJVVVZAkS9aIBfjY0BlYJA
LeaderId:                1
LeaderEpoch:             1
HighWatermark:           719
MaxFollowerLag:          0
MaxFollowerLagTimeMs:    0
CurrentVoters:           [{"id": 1, "endpoints": ["CONTROLLER://localhost:9093"]}, {"id": 2, "endpoints": ["CONTROLLER://localhost:9193"]}]
CurrentObservers:        [{"id": 4, "directoryId": "4if9DpGPB_YwhAer14GCZA"}, {"id": 3, "directoryId": "Hczf6rWh7YvteyLMVvfVUg"}]
```

**Reading this output:**

| Field | What it tells us |
| --- | --- |
| `ClusterId` | Confirms this is our cluster (`zJVVVZAkS9aIBfjY0BlYJA`) |
| `LeaderId: 1` | Controller 1 is the active leader — matches what we saw in `quorum-state` |
| `HighWatermark` | The last committed offset in the cluster metadata log (recall from architecture series Part 3 — "high watermark" is just another name for this) |
| `CurrentVoters` | The two **controllers** (IDs 1 and 2) — these are the nodes that participate in Raft voting |
| `CurrentObservers` | The two **brokers** (IDs 3 and 4) — they observe the cluster metadata but don't vote on controller elections |

```
CurrentVoters   →  Controllers (1, 2)   ← these vote in Raft elections
CurrentObservers →  Brokers (3, 4)      ← these just consume metadata, no vote
```

This single command confirms: **all 4 nodes are up, correctly attached to the same cluster, and correctly categorized** as either voting controllers or observing brokers.

---

## A note on why we passed `localhost:9093` here

Notice the command connects to `localhost:9093` — that's Controller1's `CONTROLLER` listener port from Step 2. But recall from the architecture series: **you're never required to know which specific controller is active** to interact with the cluster — any broker (or, as here, any controller endpoint) can be used as an entry point, and requests get routed appropriately. This lines up with what we'll see again in Step 6, where CLI tools only need to be pointed at *any* broker's bootstrap address, not necessarily the active controller specifically.

---

## Recap of Step 5

| Concept | Core takeaway |
| --- | --- |
| **Startup order** | Controllers must start before brokers, since brokers need to fetch cluster metadata from the controllers on startup |
| **`kafka-server-start.sh <config>`** | Same script used for both controllers and brokers — just pointed at the relevant properties file |
| **`quorum-state` file** | Found inside `__cluster_metadata-0/` under a controller's `log.dirs`; shows `leaderId` — which controller is currently active |
| **`kafka-metadata-quorum.sh ... describe --status`** | Verifies full cluster health: cluster ID, active leader, high watermark, and a clear split between `CurrentVoters` (controllers) and `CurrentObservers` (brokers) |
| **High watermark** | Direct callback to the architecture series — just another name for "last committed offset" in the cluster metadata log |

---