# Part 1 — Logs vs Tracing: The Problem Statement

---

## What are Logs?

Logs are plain text or JSON records of **what happened inside an application** while processing an API request.

Think of logs as your application's diary — it writes down everything *it* experienced. Nothing more, nothing less.

### What can you log?

- Incoming request details (URL, method, headers, body)
- Events that happened while processing (e.g., "user created", "DB query executed")
- Outgoing response details (status code, time taken, payload)
- Warnings, errors with full stack traces
- Unusual patterns for monitoring

### Example

Say you have a `UserService` and someone hits `POST /user`:

```
INFO  - Received request: POST /user
INFO  - Validating user data...
INFO  - User created successfully for userId=55
INFO  - Response sent: 201 CREATED
```

Clean, useful, easy to debug — **within that one service**.

---

## The Big Problem with Logs

Here's where things break down.

In a microservices architecture, **one API request doesn't stay inside one service**. It travels across multiple services.

```
Client
  │
  ▼
Order Service ──────► Payment Service ──────► Notification Service
```

Each service maintains **its own independent logs**:

```
[OrderService]      → "Order created for userId=55"
[PaymentService]    → "Payment timeout for userId=55"
[NotificationService] → (nothing logged, never reached?)
```

### Now ask yourself these questions:

- Are these two log lines from the **same API request**?
- Did the request that created the order **cause** the payment timeout?
- **How much time** did each service take?
- Which service is the **bottleneck**?
- Did the request even **reach** Notification Service?

**You cannot answer any of these just by looking at logs.**

Each service's logs are like isolated islands. There's no bridge connecting them.

---

## Visualizing the Problem

```
Single API Request: POST /order
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  [OrderService Logs]      [PaymentService Logs]         │
│  ──────────────────       ────────────────────          │
│  Order created userId=55  Payment timeout userId=55     │
│                                                         │
│         ↑                        ↑                      │
│    Island 1                 Island 2                    │
│                                                         │
│   No connection between these two logs!                 │
│   Are they talking about the same request? ← You        │
│   don't know.                                           │
└─────────────────────────────────────────────────────────┘
```

### What logs CAN do:
- Tell you what happened **inside** a single service
- Help debug **within** one application

### What logs CANNOT do:
- Show the **end-to-end journey** of a request across services
- Tell you **which services were involved**
- Tell you **how long each service took**
- Tell you **which service caused the failure**
- **Connect** logs from different services belonging to the same request

---

## So, What's the Solution?

This is exactly the problem **Distributed Tracing** solves.

Distributed Tracing tracks the **complete path** of a request as it travels across multiple microservices — giving you a **bird's eye view** of the entire journey.

```
Single API Request: POST /order
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│           Distributed Tracing gives you THIS:               │
│                                                             │
│  Order Service (12ms) → Payment Service (340ms ← SLOW!)     │
│                       → Notification Service (8ms)          │
│                                                             │
│  ✓ Full path visible                                        │
│  ✓ Time per service visible                                 │
│  ✓ Which service failed visible                             │
│  ✓ All connected under ONE request ID                       │
└─────────────────────────────────────────────────────────────┘
```

### What Distributed Tracing shows you:
- Which service called which service (the full call chain)
- How much time each service took
- Status code of each service (200, 400, 500)
- Which service is the bottleneck or failure point

### Important distinction — Tracing does NOT replace Logs:

| | Logs | Distributed Tracing |
|---|---|---|
| **Focus** | Detailed events inside one service | High-level path across all services |
| **Scope** | Single application | Entire request journey |
| **Tells you** | What happened inside | Which service, how long, what status |
| **Stores** | Detailed event data | Timing + path metadata only |

> They complement each other. You use **tracing to find where the problem is**, then use **logs to understand what exactly happened inside that service**.

---

### Bonus — Modern Distributed Logging (quick preview)

The instructor also mentions something really important here: **Modern distributed logging systems actually use Distributed Tracing's IDs to connect logs.**

Each service, when it writes a log, also stamps it with a common `Trace ID`:

```
TRACE-ID=T123  [OrderService]      Order created for userId=55
TRACE-ID=T123  [PaymentService]    Payment timeout for userId=55
```

Now when you search by `Trace ID = T123` in tools like **Kibana** or **CloudWatch**, all logs from all services involved in that one request appear together — even though they were written independently.

This will be covered in detail in the next topic (Distributed Logging). For now, just keep this in the back of your head.

---

**Part 1 done.** Ready for **Part 2 — How Distributed Tracing works: Trace ID, Span ID, Parent Span ID with full diagram?**

# Part 2 — How Distributed Tracing Works: Trace ID, Span ID & Parent Span ID

---

## The Two Core Concepts: Trace ID & Span ID

To achieve request tracking across microservices, Distributed Tracing introduces two fundamental building blocks.

---

### Trace ID

> **One API Request = One Trace ID**

No matter how many microservices a request travels through, they all share **one single Trace ID**. This is what connects everything together.

Think of Trace ID as a **thread** that is stitched through all the services involved in a single request.

```
Client sends: GET /order
        │
        │  Trace ID = T1 (generated once, carried everywhere)
        │
        ▼
  Order Service        Payment Service       Notification Service
  [Trace ID = T1]  →  [Trace ID = T1]   →   [Trace ID = T1]
```

---

### Span ID

> **One Unit of Work = One Span ID**

Every time a service does a piece of work, it creates a **Span**. A Span is basically a timer — it records when the work started and when it finished, so you know exactly how long that operation took.

Each span has its own **unique Span ID**.

```
Order Service does its work     → Span ID = S1
Payment Service does its work   → Span ID = S2
Notification Service does work  → Span ID = S3
```

So while **Trace ID connects all services** together under one request, **Span ID represents the individual work** done inside each service.

---

### Parent Span ID

This is what creates the **hierarchy** — it tells you which span called which span.

Every span (except the very first one) knows who its parent is via the **Parent Span ID**.

```
Order Service    → Span ID = S1,  Parent Span ID = null  (it's the root)
Payment Service  → Span ID = S2,  Parent Span ID = S1    (called by Order)
Notification     → Span ID = S3,  Parent Span ID = S1    (also called by Order)
```

---

## A Real Example — Full Picture

Let's take the example the instructor walks through. We have 4 services:

```
API Request
    │
    ▼
Service A ──────► Service B ──────► Service D
         │
         └──────► Service C
```

Here's how the Trace ID, Span ID and Parent Span ID get stored internally:

```
┌─────────────────────────────────────────────────────────────────┐
│  Service   │  Span ID  │  Parent Span ID  │  Trace ID           │
├─────────────────────────────────────────────────────────────────┤
│  Service A │    S1     │      null        │    T1               │
│  Service B │    S2     │       S1         │    T1               │
│  Service C │    S3     │       S1         │    T1               │
│  Service D │    S4     │       S2         │    T1               │
└─────────────────────────────────────────────────────────────────┘

Note: Trace ID T1 is COMMON for all services.
      Parent Span ID tells us who called whom.
```

With this information, the tracing backend (like Jaeger) can reconstruct the full **call tree**:

```
Service A (S1)               ← root, no parent
    │
    ├──► Service B (S2)      ← child of S1
    │        │
    │        └──► Service D (S4)   ← child of S2
    │
    └──► Service C (S3)      ← child of S1
```

This is exactly how Jaeger draws those nice visual timelines you see in its UI.

---

## What is a Span, Really?

A Span is simply a **start timestamp + end timestamp** wrapped around a unit of work, along with some metadata.

```
┌──────────────────────────────────────────────────────┐
│                     SPAN                             │
│                                                      │
│  Trace ID       : T1                                 │
│  Span ID        : S2                                 │
│  Parent Span ID : S1                                 │
│  Operation Name : POST /payment                      │
│  Start Time     : 10:00:00.000                       │
│  End Time       : 10:00:00.340                       │
│  Duration       : 340ms                              │
│  Status         : 200 OK                             │
└──────────────────────────────────────────────────────┘
```

---

## When Does a Span Get Created Automatically?

The instructor makes a very important point here. Once you add the tracing library to your Spring Boot app, spans are **automatically created** for these three events — you don't have to write any code for these:

```
┌─────────────────────────────────────────────────────────────────┐
│          AUTO SPAN CREATION TRIGGERS                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Incoming HTTP Request                                       │
│     → When a request enters your application                    │
│     → Span starts                                               │
│                                                                 │
│  2. Thread Handoff                                              │
│     → When your app creates a new thread                        │
│     → New span is created for that thread                       │
│                                                                 │
│  3. Outgoing HTTP Call                                          │
│     → When your app calls another microservice                  │
│     → New span is created to track that outgoing call           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

You can also create **manual spans** for custom operations like DB calls, heavy computations, etc. — we'll cover that in Part 6.

---

## The Timeline View — How Jaeger Visualizes This

This is what you'll actually see in the Jaeger UI when you look at a trace. The instructor explains this with a synchronous call flow:

```
Time ──────────────────────────────────────────────────────────►

Service A Span  ├──────────────────────────────────────────────┤
                │                                              │
  Service B Span│    ├──────────────┤                          │
                │    │              │                          │
  Service D Span│    │  ├──────┤    │                          │
                │    │              │                          │
  Service C Span│                        ├─────────┤           │
                │                                              │
                ▲                                              ▲
           Request enters                              Request fully
            Service A                                   completes
```

Reading this timeline:
- **Service A's span** is the widest — it covers the entire duration because it's the root
- **Service B** starts after A invokes it, ends before A finishes
- **Service D** is called by B, so it sits nested inside B's span
- **Service C** is called after B returns, so it starts later

This tells you at a glance: "Service B + D together took the most time — that's your bottleneck."

---

## Custom Spans Within a Service

The instructor also points out that you're not limited to one span per service. Within a single service, you can **wrap any heavy operation** in its own span.

For example, inside Order Service:

```
Order Service Span (S1)  ├─────────────────────────────────┤
                         │                                 │
  DB Query Span (S1-a)   │    ├──────────────┤             │
  (child of S1)          │                                 │
                         │                                 │
  External API Span(S1-b)│                  ├──────────┤   │
  (child of S1)          │                                 │
```

This lets you pinpoint exactly which **internal operation** inside a service is causing slowness.

---

## Quick Summary Before Moving On

```
┌──────────────────────────────────────────────────────────────┐
│                    MENTAL MODEL                              │
│                                                              │
│  Trace ID  →  The whole story (one per API request)          │
│  Span ID   →  One chapter of the story (one per service      │
│               or unit of work)                               │
│  Parent    →  Who started this chapter? (links the           │
│  Span ID      chapters together into a tree)                 │
│                                                              │
│  Together they let you reconstruct the entire journey        │
│  of any API request across all your microservices.           │
└──────────────────────────────────────────────────────────────┘
```

---

**Part 2 done.** Ready for **Part 3 — Old/Legacy Flow (Spring Cloud Sleuth) vs Modern Flow (Micrometer + OpenTelemetry): Why the change happened and how the modern architecture works?**

# Part 3 — Old/Legacy Flow vs Modern Flow

---

## First, Understand the Big Picture

Before diving into old vs new, understand that **any distributed tracing system needs to do these 3 core jobs**, regardless of which library or tool you use:

```
┌─────────────────────────────────────────────────────────────────┐
│           3 CORE JOBS OF ANY TRACING SYSTEM                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. CREATE    → Generate Trace ID and Span ID                   │
│                 for every incoming request                      │
│                                                                 │
│  2. PROPAGATE → Pass the Trace ID along to every               │
│                 other microservice this request visits          │
│                 (via HTTP headers)                              │
│                                                                 │
│  3. EXPORT    → Send the completed spans to a                   │
│                 backend (Zipkin / Jaeger / Grafana)             │
│                 so they can be visualized                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Both the old and new flow do these same 3 jobs — just differently. Let's see how.

---

## The Old / Legacy Flow — Spring Cloud Sleuth + Zipkin

### How it worked

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        OLD / LEGACY FLOW                                     │
│                                                                              │
│  ┌─────────────────────┐                                                     │
│  │  Spring Cloud Sleuth │  ← Internally hardcoded to use "Brave" library     │
│  │  (inside your app)   │                                                    │
│  └──────────┬──────────┘                                                     │
│             │  Does all 3 jobs:                                               │
│             │  ✓ Creates Trace ID                                             │
│             │  ✓ Creates Span ID                                              │
│             │  ✓ Propagates Trace ID in headers                               │
│             │                                                                 │
│             │  Completed spans dumped into...                                 │
│             ▼                                                                 │
│  ┌─────────────────────┐                                                     │
│  │  In-Memory Async Q   │  ← Holds completed spans temporarily               │
│  │  [S1][S2][S3]...     │                                                    │
│  └──────────┬──────────┘                                                     │
│             │                                                                 │
│             ▼                                                                 │
│  ┌─────────────────────┐                                                     │
│  │   Zipkin Exporter    │  ← Reads from queue, pushes to Zipkin backend      │
│  │   (Brave-specific)   │    Span format is Zipkin-specific                  │
│  └──────────┬──────────┘                                                     │
│             │                                                                 │
│             ▼                                                                 │
│  ┌─────────────────────┐    ┌──────────────────┐                             │
│  │   Zipkin Backend     │───►│    Zipkin UI      │                            │
│  │                      │    │  (visualize trace)│                            │
│  └─────────────────────┘    └──────────────────┘                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### What the Zipkin Backend does

Once it receives the spans, it:
- Stores all the span info (in DB or in memory)
- Uses Trace ID, Span ID, and Parent Span ID to **build the tree**
- Shows it visually in the Zipkin UI

---

## The Problem with the Old Flow

The instructor explains this clearly. The root cause of the problem is one thing:

> **Spring Cloud Sleuth was tightly coupled to the Brave library, which only spoke Zipkin's language.**

Think of it like this — every backend (Zipkin, Jaeger, Grafana) has its own **format/protocol** for receiving span data. It's like different countries speaking different languages.

```
┌─────────────────────────────────────────────────┐
│         THE FORMAT MISMATCH PROBLEM              │
│                                                 │
│  Brave Library generates spans in:              │
│  → Zipkin format (B3 format)                    │
│                                                 │
│  But:                                           │
│  Jaeger  understands → its own format           │
│  Grafana understands → its own format           │
│                                                 │
│  So if you wanted Jaeger instead of Zipkin?     │
│  → Hacks needed                                 │
│  → Extra configuration                          │
│  → Not straightforward at all                   │
│                                                 │
└─────────────────────────────────────────────────┘
```

This is the **main reason Spring Cloud Sleuth got deprecated** — new, more popular backends like Jaeger and Grafana emerged, and Sleuth couldn't support them cleanly.

---

## The Modern Flow — Micrometer + OpenTelemetry

The modern flow solves the problem by **separating concerns** cleanly into layers.

### The Key Idea: Interface + Implementation

The instructor explains this beautifully:

> Micrometer is just an **interface** (a set of APIs). It doesn't do the actual work. You plug in whichever **implementation (SDK)** you want.

This is exactly like how Java's `List` is an interface — you can plug in `ArrayList` or `LinkedList` depending on your need.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         MODERN FLOW                                          │
│                                                                              │
│  ┌──────────────────────────────────────────────┐                            │
│  │              Your Spring Boot App            │                            │
│  │                                              │                            │
│  │  ┌─────────────┐    ┌──────────────────────┐ │                            │
│  │  │  Micrometer │    │  OpenTelemetry SDK   │ │                            │
│  │  │  (Interface/│◄───│  (Implementation of  │ │                            │
│  │  │   APIs only)│    │   Micrometer APIs)   │ │                            │
│  │  └─────────────┘    └──────────────────────┘ │                            │
│  │                             │                │                            │
│  │                Does all 3 core jobs:         │                            │
│  │                ✓ Creates Trace ID            │                            │
│  │                ✓ Creates Span ID             │                            │
│  │                ✓ Propagates Trace ID         │                            │
│  │                             │                │                            │
│  │              Completed spans go into...      │                            │
│  │                             ▼                │                            │
│  │         ┌───────────────────────────────┐    │                            │
│  │         │     In-Memory Async Queue     │    │                            │
│  │         │     [S1][S2][S3]...           │    │                            │
│  │         │     (spans in OTLP format)    │    │                            │
│  │         └───────────────┬───────────────┘    │                            │
│  │                         │                    │                            │
│  │                         ▼                    │                            │
│  │         ┌───────────────────────────────┐    │                            │
│  │         │       OTLP Exporter           │    │                            │
│  │         │  (reads queue, pushes spans   │    │                            │
│  │         │   to whichever backend        │    │                            │
│  │         │   URL you configure)          │    │                            │
│  │         └───────────────┬───────────────┘    │                            │
│  └─────────────────────────┼────────────────────┘                            │
│                            │                                                 │
│              OTLP Protocol (vendor-agnostic format)                          │
│                            │                                                 │
│               ┌────────────┼────────────┐                                    │
│               ▼            ▼            ▼                                    │
│        ┌────────────┐ ┌─────────┐ ┌──────────┐                               │
│        │   Zipkin   │ │  Jaeger │ │  Grafana │                               │
│        │  Backend   │ │ Backend │ │  Backend │                               │
│        └────────────┘ └─────────┘ └──────────┘                               │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## The Secret Weapon: OTLP (OpenTelemetry Protocol)

This is what makes the modern flow **vendor-agnostic**.

> OTLP is a **standardized, universal format** for span data that ALL major backends understand.

```
┌──────────────────────────────────────────────────────────┐
│                  WHY OTLP IS A BIG DEAL                  │
│                                                          │
│  Old Way:                                                │
│  Brave generates Zipkin-specific format                  │
│  → Only Zipkin understands it                            │
│  → Want Jaeger? Write hacks.                             │
│                                                          │
│  New Way (OTLP):                                         │
│  OpenTelemetry SDK generates OTLP format                 │
│  → Zipkin understands OTLP  ✓                            │
│  → Jaeger understands OTLP  ✓                            │
│  → Grafana understands OTLP ✓                            │
│                                                          │
│  Want to switch from Jaeger to Grafana?                  │
│  → Just change the endpoint URL in application.properties│
│  → Zero code change                                      │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

The instructor gives a perfect analogy here — it's like USB-C. One standard connector that works with all devices, instead of a different charger for every brand.

---

## The 3 Libraries You Need (Modern Flow)

The instructor is very specific about this. Every Spring Boot microservice that needs distributed tracing requires **exactly 3 dependencies**:

```
┌──────────────────────────────────────────────────────────────────┐
│              3 REQUIRED DEPENDENCIES                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. spring-boot-starter-actuator                                 │
│     → Brings in Micrometer (the interface/APIs)                  │
│     → Micrometer itself does NOT come standalone,                │
│       it piggybacks on Actuator                                  │
│                                                                  │
│  2. micrometer-tracing-bridge-otel                               │
│     → This is the OpenTelemetry SDK                              │
│     → It is the IMPLEMENTATION of Micrometer APIs                │
│     → Does the actual work: creates IDs, propagates them         │
│                                                                  │
│  3. opentelemetry-exporter-otlp                                  │
│     → The OTLP Exporter                                          │
│     → Reads completed spans from the in-memory queue             │
│     → Pushes them to your configured backend (Jaeger/            │
│       Zipkin/Grafana) at the URL you specify                     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Old vs New — Side by Side Comparison

```
┌────────────────────────┬──────────────────────────────────────────┐
│      OLD FLOW          │            MODERN FLOW                   │
├────────────────────────┼──────────────────────────────────────────┤
│ Spring Cloud Sleuth    │ Micrometer (interface)                   │
│ (deprecated)           │ + OpenTelemetry SDK (implementation)     │
├────────────────────────┼──────────────────────────────────────────┤
│ Brave Library          │ OpenTelemetry SDK                        │
│ (hardcoded inside      │ (pluggable implementation,               │
│  Sleuth)               │  you choose it)                          │
├────────────────────────┼──────────────────────────────────────────┤
│ Zipkin-specific format │ OTLP — universal format                  │
├────────────────────────┼──────────────────────────────────────────┤
│ Only Zipkin backend    │ Any backend: Zipkin, Jaeger,             │
│ works cleanly          │ Grafana — just change the URL            │
├────────────────────────┼──────────────────────────────────────────┤
│ Changing backend =     │ Changing backend =                       │
│ lots of hacks          │ change 1 line in properties file         │
└────────────────────────┴──────────────────────────────────────────┘
```

---

## How the In-Memory Queue + Exporter Work Together

The instructor explains this flow, which is important to understand before jumping to implementation:

```
Your App is processing a request...

  Span starts (timer ON)
       │
       │  ... work happens ...
       │
  Span ends (timer OFF)
       │
       ▼
  Span is "completed" → pushed into In-Memory Async Queue
  [S1 completed][S2 completed][S3 completed]
       │
       │  (OTLP Exporter polls this queue
       │   every X seconds — configurable)
       │
       ▼
  Exporter picks up completed spans
       │
       ▼
  Sends them to backend URL in OTLP format
  e.g., http://localhost:4318/v1/traces  (Jaeger)
       │
       ▼
  Jaeger receives spans, builds the tree, shows in UI
```

> Only **completed spans** (started + ended) go into the queue. An ongoing span is not exported yet.

---

## Summary

```
┌──────────────────────────────────────────────────────┐
│                  KEY TAKEAWAYS                       │
│                                                      │
│  • Spring Cloud Sleuth is DEPRECATED                 │
│  • Modern approach = Micrometer + OpenTelemetry      │
│  • Micrometer = interface only                       │
│  • OpenTelemetry SDK = implementation                │
│  • OTLP = universal span format, works with all      │
│    backends                                          │
│  • Switching backends = just change the URL,         │
│    no code change needed                             │
│  • Every service needs exactly 3 dependencies        │
└──────────────────────────────────────────────────────┘
```

---

**Part 3 done.** Ready for **Part 4 — Full Implementation: Setting up Jaeger + App1 + App2 with all config, pom.xml, application.properties, and controller code?**

# Part 4 — Full Implementation: Jaeger + App1 + App2

---

## The Goal

We are building this:

```
Client
  │
  │  hits GET /api
  ▼
┌─────────────────┐          ┌─────────────────┐
│     App 1       │  calls   │     App 2       │
│  (port: 8080)   │─────────►│  (port: 8082)   │
│                 │          │                 │
└────────┬────────┘          └────────┬────────┘
         │                            │
         │ spans                      │ spans
         ▼                            ▼
┌─────────────────────────────────────────────────┐
│                  Jaeger Backend                 │
│              (port: 4318 for OTLP)              │
│              (port: 16686 for UI)               │
└─────────────────────────────────────────────────┘
```

Both apps send their completed spans to **the same Jaeger instance**. Jaeger stitches them together using Trace ID and shows the full journey in its UI.

---

## Step 1 — Set Up Jaeger (The Backend)

The instructor uses **Docker** to run Jaeger. This is the simplest way — one command and Jaeger is up.

```bash
docker run -d \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

### What each port does:

```
┌──────────────────────────────────────────────────────┐
│              JAEGER PORTS                            │
├──────────┬───────────────────────────────────────────┤
│  16686   │  Jaeger UI                                │
│          │  Open browser: http://localhost:16686     │
├──────────┼───────────────────────────────────────────┤
│  4317    │  OTLP gRPC endpoint                       │
│          │  (if you want to use gRPC protocol)       │
├──────────┼───────────────────────────────────────────┤
│  4318    │  OTLP HTTP endpoint  ← THIS IS WHAT       │
│          │  WE USE. Our exporter will push spans     │
│          │  here.                                    │
└──────────┴───────────────────────────────────────────┘
```

### What Docker does step by step:
1. Checks if the Jaeger image already exists locally
2. If not, pulls `jaegertracing/all-in-one:latest` from Docker Hub
3. Creates a container
4. Runs it in the background (`-d` flag)
5. Exposes the ports

Once it's running, open `http://localhost:16686` — you should see the Jaeger UI with **0 services** (expected, nothing is connected yet).

---

## Step 2 — Set Up App 1

### pom.xml

These are the **exact 3 dependencies** you need (as discussed in Part 3):

```xml
<!-- Dependency 1: Actuator → brings in Micrometer (the interface) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Dependency 2: OpenTelemetry SDK → implementation of Micrometer APIs -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- Dependency 3: OTLP Exporter → pushes completed spans to Jaeger -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### application.properties

```properties
# App running on port 8080
server.port=8080

# This name shows up in Jaeger UI — give it a meaningful name
spring.application.name=Trace-App-1

# Micrometer tracing — sample 100% of requests
# Default is only ~10-30%, we want ALL requests traced
management.tracing.sampling.probability=1.0

# OTLP Exporter — tell it WHERE to push the spans
# This is our Jaeger OTLP HTTP endpoint
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

### Important note on sampling probability:

```
┌──────────────────────────────────────────────────────────┐
│           SAMPLING PROBABILITY                           │
│                                                          │
│  1.0  = trace 100% of requests (use in dev/testing)      │
│  0.1  = trace only 10% of requests                       │
│                                                          │
│  In production with high traffic, tracing 100% of        │
│  requests can be expensive. You'd typically lower        │
│  this to 0.1 or 0.2.                                     │
│                                                          │
│  For learning/dev → always set 1.0                       │
└──────────────────────────────────────────────────────────┘
```

### Controller (App 1 — initial, just for testing)

Before wiring App 1 to call App 2, the instructor first tests that App 1 alone is generating traces correctly:

```java
@RestController
public class App1Controller {

    @GetMapping("/api")
    public String invokeApp2() {
        // Will invoke App2 here later
        // For now just returning from App1 itself
        return "response from App1";
    }
}
```

### Test it

Hit `GET http://localhost:8080/api` and go to Jaeger UI (`http://localhost:16686`).

You should now see `Trace-App-1` appear as a service. Click on it and you'll see **1 span** created for the incoming HTTP request — exactly what we expect.

```
Jaeger UI shows:
─────────────────────────────────────────────
Service:   Trace-App-1
Operation: http get /api
Spans:     1
─────────────────────────────────────────────
Span details:
  traceID   : abc123...
  spanID    : xyz789...
  operation : http get /api
  parent    : none  (it's the root span)
  duration  : Xms
─────────────────────────────────────────────
```

App 1 is working correctly. Now let's set up App 2.

---

## Step 3 — Set Up App 2

### pom.xml

**Exact same 3 dependencies** as App 1:

```xml
<!-- Same 3 dependencies as App1 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### application.properties

```properties
# App 2 runs on a different port
server.port=8082

# This name shows up in Jaeger UI
spring.application.name=Trace-App-2

# Same — trace all requests
management.tracing.sampling.probability=1.0

# Same Jaeger endpoint — both apps push to the same Jaeger
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

### Controller (App 2)

```java
@RestController
public class App2Controller {

    @GetMapping("/api/hello")
    public String helloMethod() {
        return "hello from App2";
    }
}
```

---

## Step 4 — Wire App 1 to Call App 2

Now the real setup. App 1 needs to call App 2's `/api/hello` endpoint.

The instructor uses `RestClient` — the modern way (Spring Boot 3.2+). There's a very important detail here about **HOW** you create the RestClient object. We'll explain why in detail in Part 5, but here's the rule:

> **Always use `RestClient.Builder` to create your RestClient bean — never use `RestClient.create()`.**

Using `RestClient.Builder` lets Spring Boot automatically add the interceptor that **propagates Trace ID in the headers** to the downstream service. `RestClient.create()` skips this — and your traces will break.

### AppConfig.java (App 1)

```java
@Configuration
public class AppConfig {

    @Bean
    RestClient restClientInstance(RestClient.Builder builder) {
        // CORRECT: Use builder — Spring Boot auto-adds
        // the tracing interceptor here
        return builder.build();

        // WRONG: RestClient.create() — interceptor NOT added,
        // trace ID won't propagate to App 2
    }
}
```

### App1Controller.java (updated)

```java
@RestController
public class App1Controller {

    @Autowired
    private RestClient restClient;

    @GetMapping("/api")
    public String invokeApp2() {

        // Calling App 2's endpoint
        String response = restClient.get()
                .uri("http://localhost:8082/api/hello")
                .retrieve()
                .body(String.class);

        return "Response from App2: " + response;
    }
}
```

---

## Step 5 — Full End-to-End Test

Start both apps, then hit:

```
GET http://localhost:8080/api
```

Expected response:
```
Response from App2: hello from App2
```

Now go to Jaeger UI → select `Trace-App-1` → Find Traces.

You should see **1 trace with 3 spans**:

```
┌──────────────────────────────────────────────────────────────────┐
│  Trace: dab580a...          Duration: ~50ms      Spans: 3        │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Span 1 — Trace-App-1: http get /api                             │
│  ├── traceID   : dab580a...                                      │
│  ├── spanID    : 6571f74f...                                     │
│  ├── parent    : none  (root span)                               │
│  ├── process   : p1 (Trace-App-1)                                │
│  └── duration  : 50ms  (covers entire request)                   │
│       │                                                          │
│       ▼                                                          │
│  Span 2 — Trace-App-1: http get  (outgoing call to App2)         │
│  ├── traceID   : dab580a...  (SAME trace ID)                     │
│  ├── spanID    : 01b20cd4...                                     │
│  ├── parent    : 6571f74f... (child of Span 1)                   │
│  ├── process   : p1 (Trace-App-1)                                │
│  └── duration  : 24ms  (includes network latency + App2 time)    │
│       │                                                          │
│       ▼                                                          │
│  Span 3 — Trace-App-2: http get /api/hello  (inside App2)        │
│  ├── traceID   : dab580a...  (SAME trace ID)                     │
│  ├── spanID    : fb97390...                                      │
│  ├── parent    : 01b20cd4... (child of Span 2)                   │
│  ├── process   : p2 (Trace-App-2)                                │
│  └── duration  : 1ms   (actual App2 processing time)             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Why does Span 2 take longer than Span 3?

This is a great point the instructor highlights:

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Span 2 = App1's outgoing call span                          │
│  Span 3 = App2's actual processing span                      │
│                                                              │
│  Span 2 duration > Span 3 duration                           │
│                                                              │
│  Because Span 2 includes:                                    │
│  → Network latency (App1 → App2)                             │
│  → Serialization time                                        │
│  → App2's actual processing time (Span 3)                    │
│  → Network latency (App2 → App1 response)                    │
│  → Deserialization time                                      │
│                                                              │
│  Span 3 only includes:                                       │
│  → App2's actual work time                                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## The Full JSON Jaeger Stores (for reference)

This is the actual raw span data Jaeger receives and stores. The instructor walks through this in the demo:

```json
{
  "data": [
    {
      "traceID": "dab580a0614028158d96fa5dded6b2b3",
      "spans": [
        {
          "traceID": "dab580a0614028158d96fa5dded6b2b3",
          "spanID": "6571f74f2fdf7017",
          "operationName": "http get /api",
          "references": [],
          "startTime": 1763735113048936,
          "duration": 50859,
          "processID": "p1"
        },
        {
          "traceID": "dab580a0614028158d96fa5dded6b2b3",
          "spanID": "01b20cd4bed74e03",
          "operationName": "http get",
          "references": [
            {
              "refType": "CHILD_OF",
              "traceID": "dab580a0614028158d96fa5dded6b2b3",
              "spanID": "6571f74f2fdf7017"
            }
          ],
          "startTime": 1763735113069676,
          "duration": 24497,
          "processID": "p1"
        },
        {
          "traceID": "dab580a0614028158d96fa5dded6b2b3",
          "spanID": "fb9739086e3ebf65",
          "operationName": "http get /api/hello",
          "references": [
            {
              "refType": "CHILD_OF",
              "traceID": "dab580a0614028158d96fa5dded6b2b3",
              "spanID": "01b20cd4bed74e03"
            }
          ],
          "startTime": 1763735113086468,
          "duration": 1193,
          "processID": "p2"
        }
      ],
      "processes": {
        "p1": { "serviceName": "Trace-App-1" },
        "p2": { "serviceName": "Trace-App-2" }
      }
    }
  ]
}
```

Notice:
- All 3 spans share the **same traceID**
- Each span has a unique **spanID**
- Span 2 and Span 3 have `CHILD_OF` references pointing to their parent span
- `processID` tells Jaeger which service the span belongs to (`p1` = App1, `p2` = App2)

Jaeger receives these spans **in any order** (they arrive asynchronously), but using the `CHILD_OF` references and the shared `traceID`, it correctly reconstructs the full hierarchy and timeline.

---

## Full Setup Checklist

```
┌──────────────────────────────────────────────────────────┐
│              SETUP CHECKLIST                             │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ✓ Jaeger running via Docker on ports 16686/4318         │
│  ✓ App1 — 3 dependencies in pom.xml                      │
│  ✓ App1 — application.properties configured              │
│  ✓ App1 — RestClient bean using Builder (NOT create())   │
│  ✓ App1 — Controller calling App2                        │
│  ✓ App2 — 3 dependencies in pom.xml                      │
│  ✓ App2 — application.properties configured              │
│  ✓ App2 — Controller exposing /api/hello                 │
│  ✓ Hit GET /api on App1                                  │
│  ✓ See 3 spans in Jaeger UI under same Trace ID          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

**Part 4 done.** Ready for **Part 5 — How Trace Propagation Works Under the Hood: Filters, Interceptors, and the RestClient vs RestTemplate vs FeignClient comparison?**

# Part 5 — How Trace Propagation Works Under the Hood

---

## The Big Question

When App 1 calls App 2, how does App 2 **know** it should use the same Trace ID instead of creating a new one?

You didn't write any code for this. You just added 3 dependencies and some properties. So what's actually happening behind the scenes?

The answer lies in **two automatic mechanisms** Spring Boot adds for you:

```
┌─────────────────────────────────────────────────────────────────┐
│           TWO MECHANISMS WORKING TOGETHER                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. ServerHttpObservationFilter  (on the INCOMING side)         │
│     → Runs on every service that RECEIVES a request             │
│     → Decides: new trace or continue existing trace?            │
│                                                                 │
│  2. Interceptor  (on the OUTGOING side)                         │
│     → Runs when your service CALLS another service              │
│     → Appends trace info into the outgoing HTTP headers         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Let's understand each one deeply.

---

## Mechanism 1 — ServerHttpObservationFilter (Incoming Side)

Every service that has the micrometer tracing dependency gets this filter **automatically registered**. It intercepts **every incoming HTTP request** before your controller even sees it.

```
Incoming Request
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│            ServerHttpObservationFilter                      │
│                                                             │
│  Checks: does this request have a "traceparent" header?     │
│                                                             │
│  ┌─────────────────────┐    ┌──────────────────────────┐    │
│  │  NO traceparent     │    │  YES traceparent         │    │
│  │  header found       │    │  header found            │    │
│  │                     │    │                          │    │
│  │  → This is a brand  │    │  → This request is part  │    │
│  │    new request      │    │    of an existing trace  │    │
│  │  → Generate new     │    │  → Extract Trace ID from │    │
│  │    Trace ID         │    │    the header            │    │
│  │  → Generate new     │    │  → Reuse that Trace ID   │    │
│  │    Span ID          │    │  → Generate new Span ID  │    │
│  │                     │    │  → Set parent = span from│    │
│  │                     │    │    the header            │    │
│  └─────────────────────┘    └──────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
      │
      ▼
  Your Controller runs
```

This is why App 2 correctly reuses App 1's Trace ID — because App 1's interceptor (we'll see next) puts the Trace ID in the request header, and App 2's filter reads it.

---

## Mechanism 2 — The Interceptor (Outgoing Side)

When App 1 uses `RestClient` (built via `RestClient.Builder`) to call App 2, Spring Boot **automatically registers an interceptor** on that RestClient.

This interceptor runs just before the HTTP call goes out and **appends the tracing information into the request headers**:

```
App 1 calls restClient.get().uri("http://localhost:8082/api/hello")...
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│                  Interceptor runs                           │
│                                                             │
│  Reads current Trace ID  : dab580a...                       │
│  Reads current Span ID   : 6571f74f...                      │
│                                                             │
│  Appends to outgoing request header:                        │
│  traceparent: dab580a...-6571f74f...                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
      │
      │  HTTP Request goes out with header:
      │  traceparent: <traceID>-<spanID>
      ▼
App 2 receives the request
      │
      ▼
ServerHttpObservationFilter on App 2 reads the header
      │
      ▼
App 2 reuses Trace ID, creates new Span ID,
sets parent = App 1's span ID
```

### What the header looks like:

```
GET /api/hello HTTP/1.1
Host: localhost:8082
traceparent: dab580a0614028158d96fa5dded6b2b3-6571f74f2fdf7017
```

---

## Full Picture — Both Mechanisms Together

```
┌──────────────────────────────────────────────────────────────────────┐
│                         FULL FLOW                                    │
│                                                                      │
│  Client hits App1: GET /api                                          │
│         │                                                            │
│         ▼                                                            │
│  ┌─────────────────────────────────────┐                             │
│  │              APP 1                  │                             │
│  │                                     │                             │
│  │  ServerHttpObservationFilter        │                             │
│  │  → No traceparent header found      │                             │
│  │  → Creates Trace ID = T1            │                             │
│  │  → Creates Span ID  = S1            │                             │
│  │  → Parent = null (root)             │                             │
│  │                                     │                             │
│  │  Controller runs...                 │                             │
│  │                                     │                             │
│  │  restClient calls App2              │                             │
│  │  → Interceptor runs                 │                             │
│  │  → Creates Span ID = S2             │                             │
│  │    (for outgoing call)              │                             │
│  │  → Adds header:                     │                             │
│  │    traceparent: T1-S2               │                             │
│  │                                     │                             │
│  └─────────────────────────────────────┘                             │
│         │                                                            │
│         │  HTTP call with header: traceparent: T1-S2                 │
│         ▼                                                            │
│  ┌─────────────────────────────────────┐                             │
│  │              APP 2                  │                             │
│  │                                     │                             │
│  │  ServerHttpObservationFilter        │                             │
│  │  → traceparent header FOUND!        │                             │
│  │  → Extracts Trace ID = T1           │                             │
│  │  → Reuses Trace ID   = T1           │                             │
│  │  → Creates Span ID   = S3           │                             │
│  │  → Sets Parent       = S2           │                             │
│  │                                     │                             │
│  │  Controller runs...                 │                             │
│  │  Returns "hello from App2"          │                             │
│  │                                     │                             │
│  └─────────────────────────────────────┘                             │
│         │                                                            │
│         │  Both S1, S2, S3 exported to Jaeger                        │
│         ▼                                                            │
│  Jaeger builds tree:                                                 │
│  S1 (App1 incoming) ← root                                           │
│   └── S2 (App1 outgoing call)                                        │
│        └── S3 (App2 incoming)                                        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Critical Detail — RestClient.Builder vs RestClient.create()

This is a very important point the instructor stresses. The interceptor that propagates the Trace ID is **only added when you use `RestClient.Builder`**. If you use `RestClient.create()`, the interceptor is never registered.

```
┌──────────────────────────────────────────────────────────────────┐
│          RestClient.Builder  vs  RestClient.create()             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  RestClient.Builder (CORRECT)                                    │
│  ─────────────────────────────                                   │
│  @Bean                                                           │
│  RestClient restClientInstance(RestClient.Builder builder) {     │
│      return builder.build();                                     │
│  }                                                               │
│                                                                  │
│  → Spring Boot detects micrometer on classpath                   │
│  → Automatically registers                                       │
│    ObservationRestClientCustomizer                               │
│  → This customizer adds the tracing interceptor                  │
│  → Interceptor appends traceparent header on outgoing calls      │
│  → App 2 gets the Trace ID → single trace across both apps ✓     │
│                                                                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  RestClient.create() (WRONG)                                     │
│  ────────────────────────────                                    │
│  RestClient restClient = RestClient.create();                    │
│                                                                  │
│  → No interceptor added                                          │
│  → No traceparent header in outgoing request                     │
│  → App 2's filter finds no header                                │
│  → App 2 creates a BRAND NEW Trace ID                            │
│  → You get 2 separate traces instead of 1 ✗                      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### What broken tracing looks like in Jaeger:

```
With RestClient.create() — BROKEN:
─────────────────────────────────────────
Trace 1 (App1):   Trace ID = aaa111...    1 span
Trace 2 (App2):   Trace ID = bbb222...    1 span

← Two completely separate traces.
  No connection between them.
  You have no idea they belong to the same request.

With RestClient.Builder — CORRECT:
─────────────────────────────────────────
Trace 1:   Trace ID = aaa111...    3 spans
           Trace-App-1 (2 spans) + Trace-App-2 (1 span)

← Single trace. Full journey visible. ✓
```

---

## RestTemplate — Needs Manual Wiring

`RestTemplate` is the older way of making HTTP calls. It does **not** get the interceptor automatically. You have to manually wire it using `ObservationRestTemplateCustomizer`.

```java
@Bean
RestTemplate restTemplate(ObservationRestTemplateCustomizer customizer) {
    RestTemplate restTemplate = new RestTemplate();

    // Manually register the customizer
    // This adds the tracing interceptor to RestTemplate
    customizer.customize(restTemplate);

    return restTemplate;
}
```

Without this, RestTemplate will also create broken separate traces just like `RestClient.create()`.

---

## FeignClient — Works Automatically

For FeignClient, the autoconfiguration is already present. You don't need to do anything extra — the tracing interceptor is added automatically.

```
┌──────────────────────────────────────────────────────────────┐
│  The instructor's note: FeignClient auto-configuration       │
│  is present. Test it out to confirm it works in your         │
│  specific version.                                           │
└──────────────────────────────────────────────────────────────┘
```

---

## Full Comparison — All 3 HTTP Clients

```
┌──────────────────┬─────────────────┬──────────────────────────────┐
│   HTTP Client    │ Needs Manual    │  How to set it up            │
│                  │ Wiring?         │                              │
├──────────────────┼─────────────────┼──────────────────────────────┤
│ RestClient       │ NO              │ Use RestClient.Builder       │
│ (modern)         │ (if using       │ → Spring auto-adds           │
│                  │  Builder)       │   interceptor                │
│                  ├─────────────────┼──────────────────────────────┤
│                  │ YES             │ RestClient.create() breaks   │
│                  │ (if using       │ tracing — DON'T use this     │
│                  │  create())      │                              │
├──────────────────┼─────────────────┼──────────────────────────────┤
│ RestTemplate     │ YES             │ Inject                       │
│ (old way)        │ (always)        │ ObservationRestTemplate      │
│                  │                 │ Customizer and call          │
│                  │                 │ customizer.customize(rt)     │
├──────────────────┼─────────────────┼──────────────────────────────┤
│ FeignClient      │ NO              │ Works out of the box         │
│                  │ (auto-config    │ (verify with your version)   │
│                  │  present)       │                              │
└──────────────────┴─────────────────┴──────────────────────────────┘
```

---

## Summary — What Happens Automatically vs What You Control

```
┌──────────────────────────────────────────────────────────────────┐
│                    AUTOMATIC (zero code needed)                  │
├──────────────────────────────────────────────────────────────────┤
│  ✓ Trace ID generation on first incoming request                 │
│  ✓ Span ID generation for incoming HTTP requests                 │
│  ✓ Span ID generation for outgoing HTTP calls                    │
│  ✓ Span ID generation on thread handoffs                         │
│  ✓ Trace ID propagation via headers (if using Builder/Feign)     │
│  ✓ Completed spans exported to Jaeger via OTLP exporter          │
├──────────────────────────────────────────────────────────────────┤
│                    MANUAL (you must handle)                      │
├──────────────────────────────────────────────────────────────────┤
│  ✗ RestTemplate → must wire ObservationRestTemplateCustomizer    │
│  ✗ RestClient.create() → don't use, use Builder instead          │
│  ✗ Custom spans for DB calls, heavy ops → Part 6 covers this     │
└──────────────────────────────────────────────────────────────────┘
```

---

**Part 5 done.** Ready for **Part 6 — Manual Span Creation: when to use it, how it works, and full code walkthrough with Jaeger output?**

# Part 6 — Manual Span Creation

---

## Why Do You Need Manual Spans?

So far, spans are created automatically for:
- Incoming HTTP requests
- Outgoing HTTP calls
- Thread handoffs

But what if you want to track something **inside** your service — like a slow DB query, a heavy computation, a file processing operation?

Automatic spans won't help here. You need to **manually wrap that operation** in a span so you can see exactly how long it took in Jaeger.

```
┌──────────────────────────────────────────────────────────────────┐
│                    THE PROBLEM                                   │
│                                                                  │
│  App 1 incoming request span shows: 500ms total                  │
│                                                                  │
│  But WHY is it 500ms?                                            │
│  → Is it the DB query?                                           │
│  → Is it the business logic?                                     │
│  → Is it the external API call?                                  │
│                                                                  │
│  Automatic spans won't tell you this.                            │
│  You need to manually create spans around these operations.      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Key Building Blocks

Before writing code, understand these 4 things the instructor introduces:

```
┌──────────────────────────────────────────────────────────────────┐
│              KEY BUILDING BLOCKS                                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Tracer                                                       │
│     → The main interface from Micrometer                         │
│     → Auto-wired by Spring Boot                                  │
│     → Use it to get current span, create new spans               │
│                                                                  │
│  2. Span                                                         │
│     → Represents one unit of work                                │
│     → Has start() and end() to control the timer                 │
│                                                                  │
│  3. Tracer.SpanInScope                                           │
│     → A wrapper that sets a span as the "current active span"    │
│     → When closed, restores the previous current span            │
│                                                                  │
│  4. tracer.currentSpan()                                         │
│     → Returns whichever span is currently active                 │
│     → Used to get the parent span before creating a child        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Understanding "Current Span" — Very Important Concept

The instructor spends a lot of time on this. You must understand it before writing code.

At any point in time, there is one **"current active span"** in your thread. Think of it like a pointer — it points to whichever span is currently running.

```
Request comes in → Auto span created (S1) → S1 becomes current span
                                                    │
                                                    ▼
                                          [Current Span = S1]
```

Now when you manually create a child span (S2):

```
tracer.nextSpan(parentSpan) → S2 created
childSpan.start()           → S2 timer starts

BUT — Current Span is STILL S1, not S2!
```

This is the important detail the instructor highlights:

> **`childSpan.start()` starts the timer but does NOT update the current span pointer.**

To update the current span pointer, you need `tracer.withSpan(childSpan)`.

```
┌──────────────────────────────────────────────────────────────────┐
│              start() vs withSpan() — THE DIFFERENCE              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  childSpan.start()                                               │
│  → Starts the timer for childSpan                                │
│  → Does NOT change current span pointer                          │
│  → Current span is still the parent (S1)                         │
│                                                                  │
│  tracer.withSpan(childSpan)                                      │
│  → Sets childSpan as the current active span                     │
│  → Saves old current span (S1) internally                        │
│  → Returns a SpanInScope object                                  │
│                                                                  │
│  spanInScope.close()                                             │
│  → Restores old current span (S1) back                           │
│  → Does NOT stop the timer (that's childSpan.end())              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Why does "current span" even matter?

Because if you create more spans **inside** the child span's operation, they need to know their parent. They find their parent by calling `tracer.currentSpan()`. If you didn't update current span to S2, those inner spans would incorrectly parent themselves to S1.

```
Without withSpan():                  With withSpan():
─────────────────────                ──────────────────────
S1 (root)                            S1 (root)
└── S2 (child - correct)             └── S2 (child - correct)
└── S3 (should be child               └── S3 (correctly child
    of S2 but becomes                      of S2 because
    child of S1 - WRONG)                   current span = S2)
```

---

## The Full Lifecycle of a Manual Span

```
┌──────────────────────────────────────────────────────────────────┐
│              MANUAL SPAN LIFECYCLE                               │
│                                                                  │
│  Step 1: Get current span (will become the parent)               │
│          Span parentSpan = tracer.currentSpan();                 │
│                                                                  │
│  Step 2: Create child span linked to parent                      │
│          Span childSpan = tracer.nextSpan(parentSpan)            │
│                           .name("your-operation-name");          │
│                                                                  │
│  Step 3: Start the timer                                         │
│          childSpan.start();                                      │
│                                                                  │
│  Step 4: Set child as current span (saves parent internally)     │
│          SpanInScope scope = tracer.withSpan(childSpan);         │
│                                                                  │
│  Step 5: Do your work (DB call, heavy computation, etc.)         │
│          // your business logic here                             │
│                                                                  │
│  Step 6: In finally block — close scope first, then end span     │
│          scope.close();      // restores parent as current span  │
│          childSpan.end();    // stops the timer                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

> Always use a `finally` block. If an exception is thrown during your work, you still need to close the scope and end the span — otherwise your spans will never be exported and the current span pointer will be stuck on the child forever.

---

## Full Code — Manual Span Creation

```java
@RestController
public class App1Controller {

    @Autowired
    Tracer tracer;  // Micrometer's Tracer — auto-wired by Spring Boot
                    // Implementation comes from OpenTelemetry SDK

    @GetMapping("/api/test/span")
    public String manualSpanTest() {

        // Step 1: Get the current active span
        // When this endpoint is hit, an auto span already exists
        // for the incoming HTTP request — that becomes our parent
        Span parentSpan = tracer.currentSpan();

        // Step 2: Create a child span under the same Trace ID
        // If you don't pass parentSpan here, it creates a NEW
        // Trace ID — breaking the connection!
        Span childSpan = tracer.nextSpan(parentSpan)
                               .name("manual-child-span");

        // Step 3: Start the timer
        // Note: current span is still parentSpan at this point
        childSpan.start();

        // Step 4: Update current span pointer to childSpan
        // Also saves parentSpan internally so it can be restored
        Tracer.SpanInScope spanInScope = tracer.withSpan(childSpan);

        try {
            // Step 5: Your actual work goes here
            // Simulating a heavy operation (e.g., DB query)
            Thread.sleep(70);

        } catch (Exception ex) {
            // Handle exception
        } finally {
            // Step 6: Always close in finally block

            // Close scope first → restores parentSpan as current span
            if (spanInScope != null) {
                spanInScope.close();
            }

            // End the span → stops the timer → span gets exported
            childSpan.end();
        }

        return "Response from App1";
    }
}
```

---

## What Happens Step by Step During Execution

```
Request hits GET /api/test/span
        │
        ▼
ServerHttpObservationFilter runs
→ No traceparent header (fresh request)
→ Creates Trace ID = T1
→ Creates Span ID  = S1  (for incoming HTTP request)
→ Sets current span = S1
        │
        ▼
Controller runs
→ tracer.currentSpan() returns S1  ← this is parentSpan
        │
        ▼
tracer.nextSpan(parentSpan).name("manual-child-span")
→ Creates new Span S2
→ Same Trace ID = T1  (because parentSpan passed)
→ Parent of S2 = S1
→ Current span still = S1  (not changed yet)
        │
        ▼
childSpan.start()
→ Timer starts for S2
→ Current span still = S1
        │
        ▼
tracer.withSpan(childSpan)
→ Current span updated to S2
→ S1 saved in SpanInScope internally
→ Returns SpanInScope object
        │
        ▼
Thread.sleep(70) — work happens
        │
        ▼
finally block:
spanInScope.close()
→ Current span restored back to S1

childSpan.end()
→ Timer stops for S2
→ S2 (completed) → pushed to in-memory queue → exported to Jaeger
        │
        ▼
S1 also completes when request finishes
→ S1 exported to Jaeger
        │
        ▼
Jaeger receives S1 and S2
→ Same Trace ID T1
→ S2 is CHILD_OF S1
→ Builds the tree correctly
```

---

## What You See in Jaeger UI

```
┌──────────────────────────────────────────────────────────────────┐
│  Trace: 96420ca...                   Duration: ~1.7s   Spans: 2  │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Trace-App-1: http get /api/test/span              [1.7s  ████]  │
│    └── Trace-App-1: manual-child-span              [1.5s  ███ ]  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

The manual child span sits **nested inside** the parent incoming request span — exactly the hierarchy you'd expect.

---

## The Raw JSON in Jaeger

```json
{
  "data": [
    {
      "traceID": "96420ca9ec858978ad6db56830f26899",
      "spans": [
        {
          "traceID": "96420ca9ec858978ad6db56830f26899",
          "spanID": "64dd4cbf327312fc",
          "operationName": "http get /api/test/span",
          "references": [],
          "startTime": 1763745034193419,
          "duration": 1720669,
          "processID": "p1"
        },
        {
          "traceID": "96420ca9ec858978ad6db56830f26899",
          "spanID": "137b2c47fe0e0949",
          "operationName": "manual-child-span",
          "references": [
            {
              "refType": "CHILD_OF",
              "traceID": "96420ca9ec858978ad6db56830f26899",
              "spanID": "64dd4cbf327312fc"
            }
          ],
          "startTime": 1763745034194449,
          "duration": 1578050,
          "processID": "p1"
        }
      ],
      "processes": {
        "p1": { "serviceName": "Trace-App-1" }
      }
    }
  ]
}
```

Notice:
- Both spans share the **same traceID**
- `manual-child-span` has `CHILD_OF` pointing to the incoming request span
- Both belong to `p1` (Trace-App-1) — this is a span created within the same service

---

## What Happens If You Skip nextSpan(parentSpan)?

The instructor makes this point clearly. If you call `tracer.nextSpan()` **without passing the parent span**:

```java
// WRONG — no parent passed
Span childSpan = tracer.nextSpan().name("manual-child-span");
```

```
Result:
→ A brand new Trace ID is generated
→ New span has NO connection to the incoming request span
→ In Jaeger you see 2 completely separate traces
→ You lose the full picture

Jaeger shows:
  Trace 1: http get /api/test/span     (1 span)
  Trace 2: manual-child-span           (1 span — orphan)
```

Always pass the parent span:

```java
// CORRECT — parent passed, same Trace ID reused
Span childSpan = tracer.nextSpan(parentSpan).name("manual-child-span");
```

---

## Common Mistakes — Quick Reference

```
┌──────────────────────────────────────────────────────────────────┐
│                   COMMON MISTAKES                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Not passing parentSpan to nextSpan()                         │
│     → Creates orphan span with new Trace ID                      │
│     → Fix: tracer.nextSpan(parentSpan)                           │
│                                                                  │
│  2. Not calling childSpan.end()                                  │
│     → Span timer never stops                                     │
│     → Span never gets exported to Jaeger                         │
│     → Fix: always call end() in finally block                    │
│                                                                  │
│  3. Not calling spanInScope.close()                              │
│     → Current span pointer stuck on child forever                │
│     → Any new spans created later will wrongly                   │
│       parent themselves to the child                             │
│     → Fix: always close() in finally block                       │
│                                                                  │
│  4. Calling end() before close()                                 │
│     → Span ends but scope still thinks child is current          │
│     → Fix: always close() first, then end()                      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## When Should You Create Manual Spans?

```
┌──────────────────────────────────────────────────────────────────┐
│           WHEN TO CREATE MANUAL SPANS                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ✓ Database queries that might be slow                           │
│    → Wrap in span to see exact DB time in Jaeger                 │
│                                                                  │
│  ✓ Heavy computation (e.g., PDF generation, image processing)    │
│    → See how long it contributes to total request time           │
│                                                                  │
│  ✓ External API calls not using RestClient/RestTemplate          │
│    → Manually track outgoing call time                           │
│                                                                  │
│  ✓ Message queue operations (publishing/consuming)               │
│    → Track how long queue interactions take                      │
│                                                                  │
│  ✗ Simple logic (if/else, string operations)                     │
│    → Not worth the overhead                                      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

--- 

## Complete Notes Summary — All 6 Parts

```
┌──────────────────────────────────────────────────────────────────┐
│           DISTRIBUTED TRACING — COMPLETE PICTURE                 │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  PROBLEM                                                         │
│  → Logs are per-service, no end-to-end visibility                │
│  → Can't tie logs from different services together               │
│                                                                  │
│  SOLUTION                                                        │
│  → Distributed Tracing: tracks full request journey              │
│                                                                  │
│  CORE CONCEPTS                                                   │
│  → Trace ID  : one per API request, shared across services       │
│  → Span ID   : one per unit of work inside each service          │
│  → Parent ID : links spans into a tree/hierarchy                 │
│                                                                  │
│  MODERN STACK                                                    │
│  → Micrometer (interface) + OpenTelemetry SDK (implementation)   │
│  → OTLP protocol → vendor agnostic → works with any backend      │
│  → Jaeger / Zipkin / Grafana all supported                       │
│                                                                  │
│  3 DEPENDENCIES PER SERVICE                                      │
│  → spring-boot-starter-actuator (brings Micrometer)              │
│  → micrometer-tracing-bridge-otel (OpenTelemetry SDK)            │
│  → opentelemetry-exporter-otlp (OTLP exporter)                   │
│                                                                  │
│  PROPAGATION                                                     │
│  → ServerHttpObservationFilter → handles incoming side           │
│  → Interceptor → handles outgoing side                           │
│  → traceparent header carries Trace ID between services          │
│                                                                  │
│  HTTP CLIENT RULES                                               │
│  → RestClient  → use Builder (auto interceptor) ✓                │
│  → RestTemplate → wire ObservationRestTemplateCustomizer ✓       │
│  → FeignClient  → works automatically ✓                          │
│                                                                  │
│  MANUAL SPANS                                                    │
│  → Use for DB calls, heavy ops, anything not auto-tracked        │
│  → Always pass parentSpan to nextSpan()                          │
│  → Always close() scope then end() span in finally block         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

That wraps up the complete lecture on **Distributed Tracing with Micrometer + OpenTelemetry**. All 6 parts covered — concepts, internals, implementation, and manual span creation. The next topic the instructor mentions is **Distributed Logging**, which builds directly on top of these Trace IDs to stitch logs from multiple services together.