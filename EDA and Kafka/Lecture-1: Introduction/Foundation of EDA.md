# Foundation of EDA

Status: Event Driven Architecture

### 1. Why this topic now?

Up until this point in the microservices course, all inter-service communication covered was **synchronous** — one service calling another directly and waiting for a response. This lecture introduces the **asynchronous** side of microservices communication: **Event-Driven Architecture (EDA)**.

**Scope of this lecture (as the instructor explicitly frames it):**

- Only the *introduction/concept* of EDA
- **Not** a deep dive into any specific event router (Kafka, RabbitMQ, etc.) — that comes in a later lecture
- Goal: build the mental model first, tool-specific implementation later

> This matters for how you should read this note — there's no code yet. This lecture is 100% conceptual foundation. Treat it like the "why" before the "how."
> 

---

### 2. What is Event-Driven Architecture?

**Definition (instructor's framing):**

> A system design style where services do **not** call each other directly. Instead, a service **emits an event**, and other services **react** to that event.
> 

Contrast this with what you already know (REST/synchronous microservices):

- REST: Service A directly calls Service B, waits for response
- EDA: Service A emits an event → an intermediary (event router/broker) delivers it → Service B reacts *whenever* it's ready

The core shift: **no direct service-to-service calls**. Everything is mediated through events.

---

### 3. What is an "Event"?

**Definition:** A **fact that something happened in the past.**

This is the single most important mental anchor for the whole topic — get this right and a lot of the design rules below fall out naturally.

#### The Event vs Command vs Query distinction

The instructor draws a sharp three-way distinction here, and this is a classic interview talking point:

| Type | Meaning | Tense | Example |
| --- | --- | --- | --- |
| **Command** | An instruction — "do this" | Imperative | `PlaceOrder` |
| **Event** | A fact — "this already happened" | Past | `OrderCreated` |
| **Query** | A request to fetch something | — | "fetch order details" |

**Why this matters:** In EDA, services never issue commands to each other. They emit **events** — statements of fact about what already occurred. This is a deliberate design choice, not just terminology.

#### Properties of an Event

| Property | Description | Example |
| --- | --- | --- |
| **Immutable** | Cannot be changed once created — because it already happened, and you can't rewrite the past | `OrderCreated at 10:30 PM` |
| **Past tense** | Describes *what happened*, not *what to do* | `OrderCreated`, **not** `PlaceOrder` |
| **Self-contained** | Carries all the information needed to understand it, without needing to ask the source for more context | Contains `orderId`, `amount`, `timestamp` |

**Instructor's own example to internalize immutability:** Once `OrderCreated` has fired, you cannot "undo" the order creation. What you *can* do is listen to that event and decide your own reaction to it — but the fact itself is frozen.

---

### 4. High-level mechanics (just the skeleton, before the full example)

- A service **emits an event** → the event is **listened to** by other services
- Other services **react** based on what's in the event payload
- **No two services talk to each other directly** — all interaction happens *through* events

This is the setup before the instructor walks through the full order-processing example, comparing REST vs EDA side by side — which I'll cover in Part 2, along with the 5 concrete problems of the synchronous/REST approach (availability, latency accumulation, cascading failure, tight coupling, scaling issues) and the sequence diagrams for both.

---

# 🎯Part 2: The REST Example & The 5 Problems of Synchronous Chains

The instructor now grounds the abstract EDA definition with a concrete example — and deliberately starts with the **REST version first**, so the pain points are obvious before EDA is introduced as the fix.

---

## 1. The Scenario Setup

Five services involved in placing an order:

- **User Service**
- **Order Service**
- **Payment Service**
- **Inventory Service**
- **Notification Service**

> Instructor's note: this is *just an example* to illustrate REST vs EDA — not necessarily how a real production order flow is architected end-to-end.
> 

---

## 2. REST Flow — Step by Step

In the **synchronous/REST** version, here's what happens when a user places an order:

1. **User places an order** → hits Order Service
2. **Order Service → Inventory Service**: checks real-time inventory availability
3. **Order Service → Payment Service**: processes the payment
4. **Order Service → Inventory Service**: reserves the inventory (once payment succeeds)
5. **Order Service → Notification Service**: sends the notification
6. Only **after all of this completes**, the response goes back to the user: **"Order Placed"**

#### Diagram — REST Sequence Flow

![image.png](Foundation%20of%20EDA/image.png)

**Key thing to notice:** Order Service sits in the middle of *everything* — it directly calls Payment, Inventory (twice), and Notification, and only responds to the user once the **entire chain** finishes. This is a **long-running, fully synchronous flow**, and that's exactly where the problems start.

---

## 3. The 5 Problems with This Synchronous Chain

The instructor is explicit that these are the "top 5" — not exhaustive, but the ones that matter most for interviews/system design discussions.

### **Problem 1: Availability**

**All services must be up at the same time**, or the request fails outright.

If Inventory Service is down, the *entire* order-placement flow fails — even though User, Order, Payment, and Notification services might all be perfectly healthy. The system's availability is only as strong as its **weakest/least-available link** in the chain.

### **Problem 2: Latency Accumulation**

**Total latency = sum of every service's individual latency.**

```
Total Latency = T(inventory check) + T(payment) + T(reserve inventory) + T(notification)
```

Since every call happens one after another (sequentially), the user is stuck waiting for the *entire* chain to finish before getting a response — even though, conceptually, some of these steps (like sending a notification) don't need to block the response at all.

### **Problem 3: Cascading Failure**

**One slow service can drag down the entire flow.**

Concrete example the instructor gives: say Order Service and Payment Service are fast, but Inventory Service is slow — it can only handle 5 requests at a time out of 10 incoming. Those extra 5 requests get denied by Inventory, and that failure **cascades backward** — first to Payment Service's flow, then further back to Order Service. A slowdown in *one* node ripples through the whole chain.

### **Problem 4: Tight Coupling**

**Order Service has to know everything about the other three services**: their endpoints, how to invoke them, what response format to expect, error handling for each, etc.

This is a design-level cost, not just a runtime one — Order Service becomes a "God object" that's aware of and dependent on the internal contract of every other service it calls.

### **Problem 5: Scaling Issues**

**You cannot scale one service independently based on its own load.**

Example: if you scale up Payment Service from handling 100 requests/min to 1,000 requests/min, that doesn't help if Inventory Service is still capped at 100 requests/min — the *system* is still bottlenecked at Inventory. Since the flow is a tightly-coupled chain, scaling has to be thought of holistically, not per-service — which defeats one of the core promises of microservices (independent scalability).

---

## 4. Instructor's Framing (important for exams/interviews)

He specifically calls this pattern a **"long-running flow"** — defined as a flow where **multiple services are chained together sequentially** to complete one business operation. This is the key smell that should make you *consider* EDA as an alternative — which is exactly what comes next.

> "This is just an example — this is not necessarily the exact production flow for order placement. It's here purely to contrast REST vs EDA."
> 

---

# 🎯Part 3: The Same Flow, Re-Architected with EDA

Now the instructor takes the *exact same* order-placement scenario and redesigns it using an **Event Router** sitting in the middle. This is the direct "fix" to the 5 problems from Part 2.

---

## 1. The New Setup

Same five services, but now there's a new component: the **Event Router** (a generic term at this point — instructor is deliberately *not* naming Kafka or RabbitMQ yet, since that's a later lecture).

```
User  
  OrderService  
    EventRouter  
      PaymentService  
        InventoryService  
          NotificationService
```

The instructor is careful to point out: this router could be Kafka, RabbitMQ, or anything similar — for this lecture it's just a **conceptual placeholder** for "the thing that receives events and routes them to interested consumers."

---

## 2. The Critical Design Insight: Split the Flow into "Critical" vs "Non-Critical"

This is arguably the most important takeaway of this section, and it's a recurring theme the instructor says will show up again: **not every step in a business flow needs to be synchronous.** You split the flow into two categories:

- **Critical path** → must be real-time, must happen before responding to the user
- **Non-critical path** → can happen asynchronously, in parallel, without making the user wait

For the order flow, here's how that split plays out:

---

## 3. Phase 1 — Real-Time Processing (Critical Path)

This part stays **synchronous**, because the user needs an immediate, meaningful response:

1. User places an order
2. Order Service → **checks real-time inventory** (still synchronous — you don't want to accept an order you can't fulfill)
3. Order Service **saves the order** with `status = PENDING`
4. Order Service **publishes an event**: `OrderCreated`
5. Order Service responds to the user: **"Order Accepted"**

Notice: the user already gets a response here — **without** waiting for payment, inventory reservation, or notification to complete. This alone solves the *latency accumulation* problem from Part 2.

---

## 4. Phase 2 — Async & Parallel Processing

Now that `OrderCreated` has been published to the Event Router, this is where things fan out:

1. Event Router **pushes the `OrderCreated` event** to whoever is "interested" in it — in this case, **Payment Service** and **Inventory Service** (both subscribed to this event)
2. Payment Service → processes the payment → publishes `PaymentSuccess`
3. Inventory Service → reserves the inventory → publishes `ReservationSuccess`

**Key mechanic to internalize:** the Event Router doesn't tell services *what to do* — it just broadcasts "hey, this happened" to whoever subscribed. Each service independently decides its own reaction. This is the practical embodiment of the "past tense, not a command" property from Part 1.

Also notice: Payment and Inventory are working **in parallel**, not sequentially — a direct fix for the *cascading failure* and *latency accumulation* problems.

---

## 5. Phase 3 — Async Processing (Second-order reactions)

Once `PaymentSuccess` is published, the Event Router pushes it to whoever is interested in *that* event:

1. **Notification Service** is subscribed to `PaymentSuccess` → sends the email/SMS
2. **Order Service** is *also* subscribed to `PaymentSuccess` → updates the order's status to `COMPLETED`

This is the instructor's way of showing that events can trigger **chains of further events** — a service can react to an event by publishing its own event, which then triggers more downstream reactions. It's events all the way down.

---

## 6. Full EDA Sequence Diagram

![image.png](Foundation%20of%20EDA/image%201.png)

---

## 7. How This Maps Back to Solving Part 2's Problems

Worth explicitly connecting the dots here, since this is exactly how the instructor sets up the *next* section (Advantages):

| REST Problem | How EDA's design addresses it |
| --- | --- |
| Availability | Inventory/Payment/Notification being temporarily down doesn't block order acceptance |
| Latency accumulation | User gets a response after just the critical path, not the full chain |
| Cascading failure | Payment and Inventory process in parallel, not sequentially |
| Tight coupling | Order Service no longer knows about Payment/Notification internals — it just publishes events |
| Scaling issues | Each service consumes events at its own pace/scale, independent of others |

---

# 🎯Part 4: Advantages, Core Components, and Push vs Pull Delivery Models

---

## 1. Advantages of Event-Driven Architecture

The instructor frames these as basically the **mirror image** of the 5 REST problems from Part 2 — "whatever the disadvantages of the synchronous chain were, those become the advantages of EDA."

### **1. Loose Coupling**

No service has direct knowledge of another. Order Service doesn't need to know Payment Service's endpoint, response shape, or error handling — it just publishes an event and moves on. No direct REST dependency between services.

### **2. Better Scalability**

Each service can scale **independently**, based on its own load. Payment Service can be scaled up to handle a spike in payment-processing events without needing Inventory or Notification to scale in lockstep.

### **3. Better Resilience**

A temporary issue in one service doesn't bring down the whole system. Instructor's example: if `ReserveInventory` starts failing temporarily, the *rest* of the system doesn't collapse — order acceptance still works fine. Once the failing service recovers, it can pick up the backlog of pending events and continue processing.

#import Replay capability, tying back to the "streaming" model covered later:

### **4. Replay**

Because events (at least in a streaming-style broker) are stored, you get the ability to **re-process old events** — useful for recovering from bugs, backfilling data, or reprocessing after an outage.

### **5. Improvement in Latency**

Because of parallel processing and the real-time/async split (Phase 1 vs Phase 2/3 from Part 3), the user gets a much faster response. Instructor's own framing: instead of waiting the full ~5 seconds for the whole chain, the user might now only wait ~1 second for just the critical path — the rest completes in the background.

---

## 2. Core Components of EDA

From the full example, three components generalize out:

| Component | Role |
| --- | --- |
| **Producer** | The service that **publishes** an event |
| **Broker / Event Router** | Accepts events from producers and **routes** them to the consumers that are interested |
| **Consumer** | The service that **reacts to / consumes** events |

#### Diagram — Core Components

![image.png](Foundation%20of%20EDA/image%202.png)

**Instructor's framing:** Producer publishes an event → Event Router/Broker passes it to whichever consumer(s) are interested in that specific event type. That's the entire mental model, at a high level, before you get into any specific broker implementation.

---

## 3. How Events Move Through EDA: Push vs Pull

The instructor is explicit that **how push/pull is actually implemented depends on the specific framework** (Kafka vs RabbitMQ will differ) — this section is just the *conceptual* distinction, not implementation detail. Deep dive comes later once a specific broker is chosen.

### **Push Model**

- Producer publishes an event to the broker
- Broker **immediately pushes** the message to whichever consumers are interested
- **Risk:** the consumer *must* handle whatever rate the broker sends at — it has no control over pacing, and can get **overwhelmed** if the broker pushes faster than the consumer can process

#### Diagram — Push Model

![image.png](Foundation%20of%20EDA/image%203.png)

```
Producer            Broker             Consumer
   │                   │                   │
   │──Publish Message─>│                   │
   │                   │──Push Message────>│ (immediately)
   │                   │──Push Message────>│
   │                   │──Push Message────>│
   │                   │                   │  ⚠️ Consumer must handle
   │                   │                   │     whatever rate broker sends
   │                   │                   │  ⚠️ Consumer can get overwhelmed
```

### **Pull Model**

- Producer publishes events to the broker
- Broker **stores** the messages (doesn't push immediately)
- Consumer explicitly **asks** the broker: "give me N messages"
- Broker sends exactly that many
- Consumer processes **at its own pace**, then asks for more when ready

#### Diagram — Pull Model

![image.png](Foundation%20of%20EDA/image%204.png)

```
Producer            Broker             Consumer
   │                   │                   │
   │──Publish Message─>│                   │
   │──Publish Message─>│                   │
   │                   │ [Messages stored] │
   │                   │<──Give me messages│
   │                   │──Take 1 message──>│
   │                   │                   │  ✅ Process at own pace
   │                   │<──Give me more────│
   │                   │        ⋮           │
```

**Key contrast to hold onto:** Push = broker controls the pace (risk of overwhelming consumer). Pull = consumer controls the pace (safer, more resilient, but consumer has to actively ask).

# 🎯Part 5: EDA Models (Pub/Sub vs Streaming) & The 7 Challenges

---

## 1. EDA Models: Pub/Sub vs Streaming

The instructor introduces this as **two distinct flavors of "EDM" (Event Driven Messaging)** — how the broker actually treats events once they're published. This is a very common interview distinction, so worth internalizing precisely.

### **Pub/Sub Model**

- Events are **published to whichever consumers are currently active**, and then **forgotten**
- If a new consumer joins later (say, tomorrow), it will **not** get yesterday's messages — there's no history to replay
- **Used when:** you only care about *delivery*, not about *storing history*
- **Example:** RabbitMQ Exchange

### **Streaming Model**

- Events are **appended to a log**, either forever or based on a retention policy (7 days, 30 days, etc. — configurable)
- **Used when:** you want **replay** capability, or you want **multiple consumers** to be able to read the same event independently
- If a new consumer joins tomorrow, it has a choice: read from the **latest offset**, or read from **day one** (as long as retention allows it)
- **Example:** Kafka

### Side-by-Side Comparison

| Aspect | Pub/Sub | Streaming |
| --- | --- | --- |
| History | Not stored — forgotten after delivery | Stored (in a log), retention-based |
| New consumer joining later | Gets nothing from the past | Can read from day 1 or latest offset |
| Replay capability | ❌ No | ✅ Yes |
| Example | RabbitMQ Exchange | Kafka |

**Instructor's framing — why this distinction matters:** This is the first hint (and setup) for *why* Kafka gets picked as the specific event router for the rest of this series — since replay and multi-consumer support are big deals for the kinds of long-running, multi-service workflows covered in Part 3.

---

## 2. The 7 Challenges of Event-Driven Architecture

The instructor is very deliberate here: "It's not like EDA is perfect." This section exists specifically to counter the impression that EDA has zero downsides after the "Advantages" section. He explicitly flags this as a common **system design interview topic** — expect to be asked to identify and mitigate these.

### **1. Eventual Consistency**

Because events are processed asynchronously, a `GET` call made *immediately* after an action might return **stale data**. A subsequent call, made a bit later, will eventually return the up-to-date result — hence "eventual" consistency.

### **2. Duplicate Events**

Most event routers only guarantee **"at-least-once delivery"** — meaning an event *can* be delivered more than once. Duplicates are considered **normal behavior**, not an edge case — so consuming services need to be designed to handle them properly (this is the setup for **idempotency**, which you flagged as an upcoming advanced topic).

### **3. Ordering Problems**

Example: a publisher sends Event 1, then Event 2 — but the consumer might receive **Event 2 first, then Event 1**. Out-of-order delivery is a real risk that needs explicit handling depending on the use case.

### **4. Schema Evolution**

If the structure (schema) of an event is changed or broken, **every consumer** relying on that schema can crash. This means schema versioning has to be managed carefully — you can't just casually remove or rename a field in an event payload without considering every downstream consumer.

### **5. Debugging Complexity**

Because processing happens across multiple asynchronous hops (as opposed to one clean synchronous chain), **distributed tracing becomes harder**. When everything happens in parallel across services, tracing a single request's full journey is no longer straightforward.

### **6. Poison Messages**

A single **bad/malformed message** can end up **blocking the event router** itself — if no consumer is able to successfully process it, and it just sits there. The instructor deliberately keeps this shallow for now, noting that a proper explanation depends on the specific framework's handling (Kafka vs RabbitMQ will differ) — deeper coverage comes later.

### **7. Operational Overhead**

EDA isn't "set it and forget it" — production systems need active monitoring of:

- **Consumer lag** — e.g., producer publishing 5 events/min but consumer only processing 1/min → lag keeps growing
- **Throughput** — is the queue/topic able to sustain the incoming publish rate?
- **Partitions/queues** — sizing and health of the underlying infrastructure

**Instructor's closing line on this section (important, paraphrased):** these challenges are *critical* — if not handled properly, instead of getting the benefits of EDA, you can get **more trouble than the synchronous approach**, including things like data corruption in your DB if ordering/duplication issues aren't handled correctly.

# 🎯Part 6 (Final): Use-Cases — When to Actually Reach for EDA

The instructor closes the lecture with a practical decision-making lens: **given a design problem, when should your instinct be "use EDA" vs "use REST"?** He gives four high-level signals, explicitly noting there are more in practice, but these four cover the majority of cases.

---

## 1. One Event → N Consumers

**Signal:** A single action needs to trigger **multiple independent reactions**, and those reactions don't need to know about each other.

**Example (order placement):**

```
OrderPlaced, then:
  • Inventory reserves
  • Payment charges
  • Email sent
  • Loyalty points added
  • Analytics updated
```

**Why this points to EDA:** Order Service can't reasonably call all five of these directly (that's back to tight coupling from Part 2). It publishes **one** event — `OrderPlaced` — and each interested service independently reacts. This is the cleanest, most intuitive signal for EDA.

---

## 2. Long-Running Business Workflow

**Signal:** A business process spans multiple steps, takes real time to complete, and is **failure-prone** across that span.

**Example:**

```
order → payment → shipment → delivery
```

**Why this points to EDA:** These flows take time and **can fail midway** — but the flow doesn't need to be in the **critical path** of the user's immediate response. It can be broken into multiple async steps, each of which may fail and be retried independently, rather than one giant synchronous chain that fails entirely if any single step fails.

**Instructor's mental framework here (important, ties back to Part 3):** always separate a workflow into:

- **Critical part** → must be real-time (e.g., checking inventory before accepting an order)
- **Non-critical part** → can be async, even if it takes a few extra seconds (e.g., sending a notification)

---

## 3. Eventual Consistency is Acceptable

**Signal:** Your use case can tolerate a **brief window of staleness** in the data, in exchange for the benefits of async processing.

If a slightly-out-of-date read (for a few seconds) is acceptable for your domain, that's a strong signal EDA fits well. If your use case *requires* strict, immediate consistency on every read, EDA introduces friction you'd need to work around.

---

## 4. Real-Time Analytics

**Signal:** Data is **continuously arriving** and needs to be **continuously processed** — not just read on-demand.

**Example:** Multiple events streaming in constantly, with a consumer continuously processing them for real-time dashboards, metrics, or analytics pipelines. This is a natural fit for the **streaming** model (Kafka-style) covered in Part 5, since you're dealing with an ongoing flow of data rather than a single request/response.

---

## 5. Lecture Wrap-Up (Instructor's Own Framing)

The instructor closes by being explicit that this was **only the introductory/conceptual lecture** — the foundation before diving into a specific broker. His own words (paraphrased): this is just one introductory video on what EDA is, before jumping into a specific event router — and he confirms the next lecture will most likely pick **Kafka** as that router.

---

## ✅ Full Lecture 1 Summary — Quick Recap Table

| Section | Core Idea |
| --- | --- |
| What is EDA | Services emit events, others react — no direct calls |
| What is an Event | A past-tense, immutable, self-contained fact |
| Event vs Command vs Query | Event = fact (past), Command = instruction (imperative), Query = fetch request |
| REST Problems | Availability, Latency Accumulation, Cascading Failure, Tight Coupling, Scaling Issues |
| EDA Fix | Split flow into Critical (sync) vs Non-Critical (async) path |
| Advantages | Loose coupling, Independent scaling, Resilience, Replay, Better latency |
| Core Components | Producer → Broker/Router → Consumer |
| Delivery Models | Push (broker-paced, risk of overwhelm) vs Pull (consumer-paced) |
| EDA Messaging Models | Pub/Sub (no history, e.g. RabbitMQ) vs Streaming (log-based, replayable, e.g. Kafka) |
| 7 Challenges | Eventual consistency, Duplicate events, Ordering, Schema evolution, Debugging complexity, Poison messages, Operational overhead |
| 4 Use-Cases | 1→N consumers, Long-running workflows, Eventual consistency OK, Real-time analytics |