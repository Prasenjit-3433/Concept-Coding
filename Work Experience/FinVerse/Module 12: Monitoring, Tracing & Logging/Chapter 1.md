Writing Chapter 1 now.

---

# Monitoring, Distributed Tracing & Logging — Chapter 1: The Problem Space

---

## Why This Chapter Exists

Every engineer who has worked in production has been in this situation:

A user files a support ticket. "My transactions aren't loading." You open your laptop, stare at the application, and have no idea where to start. Is it the database? The GoCardless API? The BullMQ worker? The Redis cache? The network between services?

Without observability, you are guessing. With observability, you are debugging with evidence.

This chapter explains *why* observability exists as a category of problem — before we touch a single tool or write a single line of code.

---

## The Three Questions Every Production System Must Answer

At any moment in a production system, an engineer needs to be able to answer three questions:

```
┌─────────────────────────────────────────────────────────────────┐
│              THE THREE QUESTIONS OF OBSERVABILITY               │
├─────────────────────┬───────────────────────────────────────────┤
│  Question           │  What you need to answer it               │
├─────────────────────┼───────────────────────────────────────────┤
│  Is anything wrong  │  METRICS                                  │
│  right now?         │  Numbers that change over time:           │
│                     │  error rate, latency, queue depth,        │
│                     │  memory usage, request rate               │
├─────────────────────┼───────────────────────────────────────────┤
│  What exactly       │  LOGS                                     │
│  happened?          │  Timestamped records of discrete events:  │
│                     │  "job_4821 failed with GoCardless 429",   │
│                     │  "user usr_123 connected Monzo account"   │
├─────────────────────┼───────────────────────────────────────────┤
│  Where did it       │  TRACES                                   │
│  go wrong?          │  The path a single request took through   │
│                     │  the system — which service, which        │
│                     │  function, how long each step took        │
└─────────────────────┴───────────────────────────────────────────┘
```

These three — **Metrics, Logs, and Traces** — are called the **Three Pillars of Observability**. You will hear this phrase in every senior engineering interview. But more important than knowing the phrase is understanding *what each one actually tells you and what it cannot tell you*.

---

## Pillar 1 — Metrics: The Vital Signs

A metric is a number that changes over time. Think of it like a hospital patient's vital signs — heart rate, blood pressure, temperature. You do not read them once. You watch them continuously, and you set alarms when they cross dangerous thresholds.

```
METRICS — WHAT THEY LOOK LIKE

finverse.transaction_sync.duration_ms  →  p95: 8,200ms  ← too slow
finverse.transaction_sync.error_rate   →  2.3%          ← too high
finverse.bullmq.queue_depth            →  1,847 jobs    ← backing up
finverse.http.request_rate             →  340 req/s     ← normal
finverse.postgres.connection_pool      →  18/20 in use  ← almost full
```

**What metrics tell you:** *Something is wrong right now.* They answer the "is the system healthy?" question in real time. If the sync error rate climbs from 0.2% to 4% at 14:30, metrics tell you the moment it happens.

**What metrics cannot tell you:** *Why* it is wrong. A spike in error rate tells you something failed. It does not tell you which specific request failed, which line of code threw, or which user was affected.

---

## Pillar 2 — Logs: The Event Record

A log is a timestamped record of something that happened. Every time your code does something significant — starts a job, fails an API call, completes a sync — it writes a log entry.

```
LOGS — WHAT THEY LOOK LIKE

2024-01-15T14:23:11.421Z  INFO  [TransactionSyncWorker]
  job=initial-sync-usr_123  account=acc_456  status=started

2024-01-15T14:23:14.892Z  ERROR  [TransactionSyncWorker]
  job=initial-sync-usr_123  account=acc_456
  error="GoCardless 429: rate limit exceeded"
  attemptsMade=1  nextRetryIn=5000ms

2024-01-15T14:23:20.103Z  INFO  [TransactionSyncWorker]
  job=initial-sync-usr_123  account=acc_456
  status=completed  transactionsFetched=247  inserted=231  duplicates=16
```

**What logs tell you:** *What exactly happened*, in detail, for a specific event. When you know something went wrong (from metrics), you look at logs to understand what happened and why.

**What logs cannot tell you:** *Where in the overall system* the problem originated. If a request touches four services — API Gateway, Core Product, Market Data Service, PostgreSQL — the logs for each are completely separate. There is no built-in way to connect "this log line in Core Product" to "this log line in Market Data Service" that was part of the same user request.

This is the gap that traces fill.

---

## Pillar 3 — Traces: The Request Journey

A trace is the complete record of a single request's journey through your system — every service it touched, every function it called, how long each step took.

```
TRACE — WHAT IT LOOKS LIKE (for GET /v1/portfolio)

Trace ID: 9e7d21299f4ea8a1  (one ID, shared across all services)
Total duration: 342ms

  ┌─────────────────────────────────────────────────────────────┐
  │  [API Gateway]  JWT validation + routing           12ms     │
  └──────────────────────────┬──────────────────────────────────┘
                             │
  ┌──────────────────────────▼──────────────────────────────────┐
  │  [Core Product]  GET /v1/portfolio handler         318ms    │
  │                                                             │
  │    ├── Redis cache read (portfolio:val:usr_123)    2ms      │
  │    │   MISS — cache expired                                 │
  │    │                                                        │
  │    ├── HTTP call to Market Data Service            287ms    │
  │    │                                                        │
  │    │     ┌────────────────────────────────────────────┐     │
  │    │     │  [Market Data Service — Go]        285ms   │     │
  │    │     │                                            │     │
  │    │     │   ├── Redis read (mkt:price:VWCE.DE) 1ms   │     │
  │    │     │   ├── Redis read (mkt:price:IWDA)    1ms   │     │
  │    │     │   ├── PostgreSQL: load holdings      43ms  │     │
  │    │     │   └── Valuation computation          238ms │     │
  │    │     │        ← THIS IS THE SLOW PART             │     │
  │    │     └────────────────────────────────────────────┘     │
  │    │                                                        │
  │    └── PostgreSQL: load holding metadata          18ms      │
  └─────────────────────────────────────────────────────────────┘
```

Without the trace, you would have seen: "portfolio endpoint is slow (318ms)." You might have guessed it was the database. But the trace shows you the truth — the valuation computation in the Go service is taking 238ms. That is where the investigation should start.

**What traces tell you:** *Where* in the system the time is being spent for a specific request. The precise answer to "which service, which step, how long."

**What traces cannot tell you:** Historical patterns across thousands of requests. A trace is a snapshot of one request. For patterns — "p95 latency is climbing" — you need metrics.

---

## Why a Monolith Does Not Need Distributed Tracing

This distinction is important and interviewers test it.

In a monolith — one process, one database, one deployment — everything runs in the same memory space. When something is slow, you can add timing logs, attach a profiler, or look at the call stack. You know every function call is in the same process.

```
MONOLITH — ALL IN ONE PLACE

Request → [one process: everything happens here] → Response

Debugging: attach profiler, look at call stack, add timing logs
Single log file, single database, single deployment
```

The moment you split into multiple services, this breaks down. A request now crosses process boundaries. The API Gateway log is in one place, the Core Product log is in another, the Go service log is in a third. They are running on different containers, potentially different machines. The connection between them exists only in the request — a shared identifier that must be explicitly propagated.

```
DISTRIBUTED SYSTEM — LOGS ARE ISLANDS

Mobile App
    │
    ▼
API Gateway (Container 1)      Log: "request received for usr_123"
    │
    ▼
Core Product (Container 2)     Log: "loading portfolio for usr_123"
    │
    ▼
Market Data Service (Container 3)  Log: "computing valuation"
    │
    ▼
PostgreSQL                     Query log: "SELECT * FROM holdings..."

WITHOUT DISTRIBUTED TRACING:
  These four log lines exist on four different systems.
  Nothing connects them.
  If the user reports "portfolio was slow", you have no idea
  which of these four steps was the bottleneck.

WITH DISTRIBUTED TRACING:
  All four lines share the same Trace ID: 9e7d21299f4ea8a1
  One search in Datadog returns all four, in order, with timing.
  You see the full picture in seconds.
```

At FinVerse, even though Core Product is a modular monolith, the system still has multiple services — Core Product, Market Data (Go), Payment Service, Notification Service. Distributed tracing is not optional. It is required to diagnose cross-service performance issues.

---

## The Interview Trap: "How Did You Measure That?"

This is the most important section in this chapter for your interviews.

When you write on your resume:

> *"Optimised transaction sync pipeline, reducing average sync time from 28 seconds to 11 seconds"*

An interviewer will immediately ask: **"How did you measure that?"**

The wrong answer sounds like this: *"I noticed the sync was slow, so I made some changes and it felt faster."*

The right answer requires you to have measured the before state, implemented the change, and measured the after state — with specific numbers from a specific tool.

Here is what measuring this properly looks like:

```
THE MEASUREMENT STORY — WHAT YOU NEED TO BE ABLE TO SAY

BEFORE the fix:
  "In Datadog, I looked at the custom metric
   finverse.transaction_sync.duration_ms, filtered to
   job_type: INITIAL_SYNC. The p95 was 28.4 seconds.
   The p99 was 41 seconds. This was measured over a 7-day window."

THE FIX:
  "I identified via distributed tracing in Datadog APM that
   the GoCardless API calls were happening sequentially —
   each account waited for the previous one to finish.
   I refactored to use Promise.all() for concurrent account
   fetching within the rate limit."

AFTER the fix:
  "I deployed to staging, ran a load test with realistic
   data (users with 3 accounts, 2 years of history).
   Measured the same metric over 24 hours.
   p95 dropped to 11.2 seconds. p99 dropped to 18 seconds."

THE RESULT:
  "In production, Datadog APM showed the INITIAL_SYNC
   span duration drop from p95: 28s to p95: 11s within
   the first hour of deployment, measured across real users."
```

This is what "I measured it with Datadog" actually means. Not a vague claim. A specific metric, a specific window, a specific before and after number.

Every performance improvement story you tell in an interview must have this structure. We will build the specific FinVerse stories in Chapter 7. For now, understand the principle: **you cannot claim an improvement without knowing what you measured, how you measured it, and what the before and after numbers were.**

---

## How Java Teams Do This vs How Node.js Teams Do This

You already have a solid mental model from Spring Boot. Let's build the comparison now so the Node.js version feels familiar rather than foreign.

```
┌─────────────────────────────────────────────────────────────────────┐
│           OBSERVABILITY STACK COMPARISON                            │
├─────────────────────────────┬───────────────────────────────────────┤
│  JAVA / SPRING BOOT         │  NODE.JS / NESTJS                     │
├─────────────────────────────┼───────────────────────────────────────┤
│  METRICS                    │  METRICS                              │
│                             │                                       │
│  Spring Boot Actuator       │  No built-in equivalent               │
│  /metrics endpoint          │  (NestJS has no Actuator)             │
│                             │                                       │
│  Micrometer (facade)        │  prom-client / StatsD client          │
│  (like SLF4J for metrics)   │  OR: OTEL Metrics SDK                 │
│                             │                                       │
│  micrometer-registry-       │  Datadog StatsD agent                 │
│  datadog (push model)       │  OR: OTEL → Datadog exporter          │
├─────────────────────────────┼───────────────────────────────────────┤
│  LOGS                       │  LOGS                                 │
│                             │                                       │
│  SLF4J (facade)             │  No standard facade                   │
│  Logback (implementation)   │  Pino (fastest, JSON by default)      │
│  LogstashEncoder (JSON)     │  → JSON out of the box                │
│                             │                                       │
│  MDC (ThreadLocal map)      │  AsyncLocalStorage                    │
│  for Correlation ID         │  for Correlation ID                   │
│                             │  (Node.js equivalent of ThreadLocal)  │
├─────────────────────────────┼───────────────────────────────────────┤
│  DISTRIBUTED TRACING        │  DISTRIBUTED TRACING                  │
│                             │                                       │
│  Micrometer Tracing         │  @opentelemetry/sdk-node              │
│  (bridge to OTEL)           │  (direct OTEL SDK)                    │
│                             │                                       │
│  micrometer-tracing-        │  @opentelemetry/                      │
│  bridge-otel                │  auto-instrumentations-node           │
│                             │                                       │
│  opentelemetry-exporter-    │  @opentelemetry/exporter-otlp-http    │
│  otlp                       │                                       │
│                             │                                       │
│  Trace propagation:         │  Trace propagation:                   │
│  ServerHttpObservationFilter│  OTEL HTTP instrumentation            │
│  (auto-registers on Spring) │  (auto-instruments http.Server)       │
├─────────────────────────────┼───────────────────────────────────────┤
│  BACKEND / UI               │  BACKEND / UI                         │
│                             │                                       │
│  Jaeger (in your notes)     │  Datadog APM                          │
│  OR: Datadog                │  (FinVerse uses Datadog)              │
│                             │                                       │
│  Both receive OTLP format   │  Same OTLP format                     │
│  Switching = change config  │  Switching = change config            │
│  No code change needed      │  No code change needed                │
└─────────────────────────────┴───────────────────────────────────────┘
```

The most important insight from this table: **the concepts are identical**. Metrics need a facade and a registry. Logs need structured JSON and a correlation ID mechanism. Traces need an SDK that instruments your HTTP calls and propagates a shared ID. The tools have different names, the underlying ideas are the same.

---

## FinVerse's Observability Stack Decision

FinVerse uses **OpenTelemetry → Datadog** for everything. One decision, made deliberately.

Here is the alternative that many teams use and why FinVerse chose differently:

```
ALTERNATIVE STACK (self-hosted open source):

Prometheus   → metrics storage + alerting
Grafana      → metrics dashboards
Jaeger       → distributed tracing
ELK Stack    → logs (Elasticsearch + Logstash + Kibana)

Total: 4 separate systems to deploy, maintain, scale, backup
Team required: dedicated platform/DevOps engineer
Cost: infrastructure costs grow with data volume

FINVERSE'S CHOICE (managed SaaS):

OpenTelemetry (instrumentation layer — language agnostic)
         │
         ▼
Datadog  → metrics + APM + logs + dashboards + alerting
           ALL IN ONE platform

Total: 1 system (SaaS — AWS manages the servers)
Team required: none dedicated (15 minutes to configure)
Cost: Datadog pricing (higher per-unit, but zero ops overhead)
```

**Why this is the right call for a Series A fintech:**

FinVerse has 2 platform engineers for the entire company. Running and maintaining 4 separate observability systems would consume both of them. Datadog is expensive per data point — but it is zero operational overhead. At 180,000 MAUs, the Datadog bill is far cheaper than hiring a third platform engineer to maintain a self-hosted stack.

This is a classic **build vs buy** decision. At Series B scale, when data volume makes Datadog pricing prohibitive, the conversation changes. But at Series A, Datadog is correct.

**Why OpenTelemetry as the instrumentation layer:**

OpenTelemetry is not a backend. It is a standard — a vendor-neutral API and SDK for emitting telemetry data. Your instrumentation code calls OpenTelemetry APIs. OpenTelemetry sends the data wherever you configure it to go.

```
WITHOUT OPENTELEMETRY:

Code → Datadog SDK → Datadog
         ↑
  If you switch to New Relic:
  Rewrite all instrumentation code
  Every service, every language, every manual span

WITH OPENTELEMETRY:

Code → OpenTelemetry SDK → [config change] → Datadog
                         → [config change] → New Relic
                         → [config change] → Jaeger

Switching backends = one config file change.
Instrumentation code: unchanged.
```

For FinVerse with two services in different languages (NestJS and Go), OpenTelemetry also means one standard across both. The Go service and the NestJS service both use their language's OTEL SDK. Both export traces in OTLP format. Datadog receives both. The traces link across language boundaries automatically via the `traceparent` header.

---

## The Full FinVerse Observability Architecture

```
FINVERSE OBSERVABILITY — COMPLETE PICTURE

┌──────────────────────────────────────────────────────────────────┐
│                    INSTRUMENTED SERVICES                         │
│                                                                  │
│  ┌─────────────────────┐      ┌──────────────────────────┐       │
│  │  Core Product       │      │  Market Data Service     │       │
│  │  (NestJS)           │      │  (Go)                    │       │
│  │                     │      │                          │       │
│  │  OTEL SDK (Node.js) │      │  OTEL SDK (Go)           │       │
│  │  Pino logger        │      │  slog / zerolog          │       │
│  │  Custom metrics     │      │  Custom metrics          │       │
│  └─────────┬───────────┘      └───────────┬──────────────┘       │
│            │                              │                      │
│            │  OTLP (gRPC or HTTP)         │  OTLP                │
│            └──────────────┬───────────────┘                      │
└───────────────────────────┼──────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                    OTEL COLLECTOR (sidecar)                       │
│                                                                   │
│  Receives: Traces, Metrics, Logs                                  │
│  Processes: batching, filtering, enrichment                       │
│  Exports: → Datadog exporter (OTLP → Datadog API)                 │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                         DATADOG                                   │
│                                                                   │
│  ┌──────────┐  ┌───────────┐  ┌───────────┐  ┌───────────────┐    │
│  │  APM     │  │  Logs     │  │  Metrics  │  │  Dashboards   │    │
│  │  Tracing │  │  Explorer │  │  Explorer │  │  & Monitors   │    │
│  └──────────┘  └───────────┘  └───────────┘  └───────────────┘    │
│                                                                   │
│  All linked by Trace ID — click a metric spike → see the traces   │
│  that caused it → click a trace → see the correlated log lines    │
└───────────────────────────────────────────────────────────────────┘
```

The three pillars — metrics, logs, traces — all flow into one platform. The key property is that **they are all linked by the same Trace ID**. When you see an error rate spike in a Datadog metric dashboard, you click on it, and Datadog shows you the traces from that time window. You click on a trace, and Datadog shows you the log lines that were emitted during that trace. Everything connects.

---

## Chapter 1 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 1 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Three Pillars of Observability:                                │
│  Metrics → "Is something wrong right now?"                      │
│  Logs    → "What exactly happened?"                             │
│  Traces  → "Where did it go wrong?"                             │
│                                                                 │
│  None of the three alone is sufficient.                         │
│  You need all three, linked by a shared Trace ID.               │
│                                                                 │
│  Monolith: observability is simpler (one process, one log)      │
│  Distributed system: traces are required to connect the dots    │
│  across service boundaries                                      │
│                                                                 │
│  The interview trap: "I improved performance by X%"             │
│  → Requires: specific metric, specific time window,             │
│    specific before/after numbers from a specific tool           │
│                                                                 │
│  Java vs Node.js: concepts are identical, tools differ          │
│  Micrometer ↔ OTEL Metrics SDK                                  │
│  SLF4J/Logback ↔ Pino                                           │
│  MDC/ThreadLocal ↔ AsyncLocalStorage                            │
│  OTEL tracing: same in both (language-agnostic standard)        │
│                                                                 │
│  FinVerse stack: OpenTelemetry → OTEL Collector → Datadog       │
│  Why: managed SaaS, zero ops overhead, all three pillars        │
│  in one platform, linked by Trace ID                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 1 done. Ready for Chapter 2 — Metrics: Measuring What's Happening Right Now — where we go deep on metric types (counters, gauges, histograms), how NestJS exposes them, what FinVerse actually monitors, and how you build the "before and after" numbers for an interview performance story.