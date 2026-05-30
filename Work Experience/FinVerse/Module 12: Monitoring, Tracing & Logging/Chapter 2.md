Writing Chapter 2 now.

---

# Monitoring, Distributed Tracing & Logging — Chapter 2: Metrics

---

## Why This Chapter Exists

Chapter 1 established that metrics answer the question "is something wrong right now?" But that description is deceptively simple. In practice, metrics are the foundation of everything — they are what Datadog dashboards are built on, what triggers alerts, what you look at first when something goes wrong, and what you reference when you say "we improved performance by X%."

This chapter goes deep on what metrics actually are, how they work mechanically, how NestJS exposes them, and exactly which metrics FinVerse monitors in production.

---

## What a Metric Actually Is

A metric is a **named, timestamped numerical measurement** that is collected repeatedly over time.

That definition has three parts that all matter:

**Named** — every metric has a name that describes what it measures. `finverse.transaction_sync.duration_ms` is more useful than `timer_1`.

**Timestamped** — a single number means nothing. `error_rate = 2.3%` is meaningless without knowing *when*. Is that right now? Yesterday? The average over the last month? Metrics are always paired with a timestamp, which is what makes them plottable on a graph over time.

**Collected repeatedly** — metrics are not logged once. They are collected at regular intervals (every 10 seconds, every 60 seconds) so you can see how they change over time.

```
METRIC OVER TIME — WHAT IT LOOKS LIKE

finverse.transaction_sync.error_rate (%)

4.5% ┤                    ╭──╮
4.0% ┤                   ╭╯  ╰╮
3.5% ┤                  ╭╯    ╰╮
3.0% ┤                 ╭╯      ╰╮
2.5% ┤                ╭╯        ╰───
2.0% ┤               ╭╯
1.5% ┤──────────────╯
1.0% ┤
0.5% ┼──────────────────────────────►
     14:00  14:10  14:20  14:30  14:40

     GoCardless incident began at 14:15
     Resolved at 14:35
     Error rate returned to baseline by 14:42
```

This graph tells a story that no single number can. It tells you *when* the problem started, *how bad* it got, and *when* it resolved. That is what metrics give you.

---

## The Four Metric Types

Every metrics system — whether Micrometer in Java or the OpenTelemetry SDK in Node.js — provides four fundamental metric types. Each one measures a different kind of thing.

---

### Type 1 — Counter

A counter is a number that only ever goes **up**. It counts occurrences of something — how many requests were made, how many jobs were processed, how many errors occurred.

```
COUNTER — EXAMPLES AT FINVERSE

finverse.http.requests.total
  → increments by 1 every time any HTTP request arrives

finverse.transaction_sync.jobs.completed
  → increments by 1 every time a sync job completes successfully

finverse.gocardless.api.calls.total
  → increments by 1 every time we call the GoCardless API

finverse.gocardless.api.errors.total
  → increments by 1 every time GoCardless returns an error
```

**The key property:** counters never decrease (unless the process restarts, at which point they reset to zero). You do not plot a counter directly — you plot its **rate of change**.

```
COUNTER vs RATE

Raw counter value:
  14:00 → 10,420 total requests
  14:01 → 10,775 total requests
  14:02 → 11,130 total requests

Rate (requests per second):
  14:01 → (10,775 - 10,420) / 60 = 5.9 req/s
  14:02 → (11,130 - 10,775) / 60 = 5.9 req/s

The rate is what you graph and alert on.
"Request rate is 6 per second" is meaningful.
"Total requests is 10,775" is not — it just keeps growing.
```

**Java parallel:** In Micrometer, `Counter.builder("http.requests").register(registry)` then `counter.increment()`. In OTEL Node.js, `meter.createCounter("http.requests")` then `counter.add(1)`.

---

### Type 2 — Gauge

A gauge is a number that can go **up or down**. It measures the current state of something — how full is the queue right now, how much memory is being used, how many active database connections exist.

```
GAUGE — EXAMPLES AT FINVERSE

finverse.bullmq.queue.waiting
  → current number of jobs waiting in the transaction-sync queue
  → goes up when new jobs are enqueued
  → goes down when workers pick them up

finverse.postgres.connections.active
  → current number of active PostgreSQL connections
  → goes up as requests arrive
  → goes down as requests complete

finverse.redis.memory.used_bytes
  → current Redis memory usage
  → changes as keys are set and expired

finverse.nodejs.heap.used_bytes
  → current Node.js heap memory usage
  → changes constantly as objects are created and garbage collected
```

**The key property:** gauges represent point-in-time state. You read them as-is — no rate calculation needed.

```
GAUGE — WHAT IT LOOKS LIKE ON A GRAPH

finverse.bullmq.queue.waiting (job count)

2000 ┤     ╭──╮
1800 ┤    ╭╯  ╰╮
1600 ┤   ╭╯    ╰╮
1400 ┤  ╭╯      ╰╮
1200 ┤ ╭╯        ╰╮
1000 ┤╭╯          ╰╮
 800 ┤╯            ╰╮
 600 ┤              ╰╮
 400 ┤               ╰╮
 200 ┤                ╰╮
   0 ┼─────────────────╰────────────►
     08:00   08:30   09:00   09:30

     Periodic sync triggered at 08:00 → 2,000 jobs enqueued
     Workers draining the queue → gauge falls
     Queue empty by 09:20
```

**Java parallel:** `Gauge.builder("bullmq.queue.waiting", queue, Queue::getWaitingCount).register(registry)`. In OTEL Node.js, `meter.createObservableGauge("bullmq.queue.waiting")` with a callback that reads the current value.

---

### Type 3 — Histogram

A histogram measures the **distribution** of values. It answers questions like "what does the spread of response times look like?" by recording many individual measurements and bucketing them.

This is the most important metric type for performance work — and the most commonly misunderstood.

```
THE PROBLEM HISTOGRAM SOLVES

Imagine 100 sync job durations (in seconds):
  11, 12, 10, 11, 13, 11, 12, 10, 11, 45, 11, 12, ...

Average: 12.3 seconds

But wait — that 45 seconds is a real user who waited 45 seconds.
The average hides it completely.

A histogram shows you the distribution:

  Bucket 0-10s:  ██████████████████ (18 jobs)
  Bucket 10-15s: ████████████████████████████████████ (79 jobs)
  Bucket 15-20s: (0 jobs)
  Bucket 20-30s: (0 jobs)
  Bucket 30-60s: ██ (2 jobs) ← the outliers
  Bucket 60s+:   (1 job) ← the very bad one

Percentiles derived from histogram:
  p50 (median):  11.2s   ← 50% of jobs finish faster than this
  p95:           13.8s   ← 95% of jobs finish faster than this
  p99:           44.1s   ← 99% of jobs finish faster than this
  p99.9:         71.0s   ← the worst 0.1%
```

**Why percentiles matter more than averages:**

The average masks outliers. If 99 users have a 1-second response time and 1 user has a 101-second response time, the average is 2 seconds — which sounds fine. But that one user's experience was terrible.

Percentiles reveal the true shape of your performance:

```
PERCENTILE INTERPRETATION AT FINVERSE

p50 = 11s   → Half of all syncs complete in under 11 seconds
              This is "typical" performance

p95 = 14s   → 95% of syncs complete in under 14 seconds
              This is what most users experience in the worst case

p99 = 44s   → 1% of syncs take 44 seconds or longer
              These are the users who had a bad experience today

p99.9 = 71s → 0.1% of syncs took over 71 seconds
              These are the users who had a very bad experience

When you say "we improved sync time" in an interview,
you say which percentile improved and by how much:
"p95 improved from 28s to 11s"
NOT "average improved from 20s to 12s"
```

**Java parallel:** `Timer.builder("sync.duration").register(registry)` in Micrometer records a histogram automatically. In OTEL Node.js, `meter.createHistogram("sync.duration")` then `histogram.record(durationMs)`.

---

### Type 4 — Summary

A summary is similar to a histogram but calculates percentiles client-side rather than server-side. In practice, histograms are more commonly used in production systems because they can be aggregated across multiple instances — summaries cannot.

At FinVerse, histograms are used everywhere. You will not need to work with summaries directly.

---

## The Metric Naming Convention at FinVerse

Metric names are not arbitrary. They follow a convention that makes them searchable, filterable, and understandable.

```
METRIC NAMING STRUCTURE

{service}.{component}.{what_is_measured}.{unit}

Examples:
  finverse.transaction_sync.duration.ms
  finverse.bullmq.queue.waiting.count
  finverse.gocardless.api.errors.total
  finverse.http.requests.total
  finverse.postgres.connections.active.count
  finverse.redis.memory.used.bytes
```

**Tags / Labels — the second dimension:**

Every metric can also have tags (called labels in Prometheus, tags in Datadog). Tags add dimensions to a metric without creating separate metric names.

```
WITHOUT TAGS — ONE METRIC FOR EVERYTHING:
  finverse.http.requests.total = 10,420

  But: which endpoint? Which status code? Which method?
  You cannot tell.

WITH TAGS — FILTERABLE BY DIMENSION:
  finverse.http.requests.total{
    endpoint: "/v1/accounts",
    method: "GET",
    status_code: "200"
  } = 8,230

  finverse.http.requests.total{
    endpoint: "/v1/accounts/connect",
    method: "POST",
    status_code: "201"
  } = 340

  finverse.http.requests.total{
    endpoint: "/v1/accounts/connect",
    method: "POST",
    status_code: "503"
  } = 12   ← GoCardless was down

Now you can ask:
  "Show me error rate for POST /v1/accounts/connect"
  "Show me p95 latency for GET /v1/accounts"
  "Show me total requests per endpoint"
```

**Java parallel:** In Micrometer, tags are added as `Tags.of("endpoint", "/v1/accounts", "method", "GET")`. In OTEL Node.js, attributes are added to each measurement: `histogram.record(duration, { "endpoint": "/v1/accounts", "method": "GET" })`.

---

## How Metrics Flow From NestJS to Datadog

Now let's get concrete about the mechanics. How does a number produced in NestJS code end up as a graph in Datadog?

```
METRICS FLOW — FINVERSE

NestJS Code
  histogram.record(syncDurationMs, { job_type: 'INITIAL_SYNC' })
       │
       ▼
OpenTelemetry SDK (in-process)
  Buffers measurements in memory
  Aggregates every 60 seconds (configurable)
  Computes histogram buckets and percentiles
       │
       │  OTLP format (protobuf over HTTP or gRPC)
       ▼
OTEL Collector (sidecar container alongside NestJS)
  Receives OTLP data
  Applies any transformations (tag enrichment, filtering)
  Batches for efficiency
       │
       │  Datadog exporter
       ▼
Datadog API
  Stores time-series data
  Makes it queryable in dashboards and monitors
       │
       ▼
Datadog Dashboard
  Graph: finverse.transaction_sync.duration.ms
  Filtered by: job_type: INITIAL_SYNC
  Percentile: p95
  Time window: last 7 days
```

---

## Setting Up Metrics in NestJS — The Code

Let's look at exactly how this is done. The setup must happen before anything else in the application — before NestJS bootstraps, before any module loads.

```typescript
// src/instrumentation.ts
// This file is loaded BEFORE main.ts via Node.js --require flag
// It MUST be the first thing that runs — before any imports

import { NodeSDK } from '@opentelemetry/sdk-node'
import { OTLPMetricExporter } from '@opentelemetry/exporter-otlp-http'
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics'
import { Resource } from '@opentelemetry/resources'
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions'

const sdk = new NodeSDK({
  // Identify this service in all telemetry data
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'core-product',
    [SemanticResourceAttributes.SERVICE_VERSION]: process.env.APP_VERSION,
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.NODE_ENV,
  }),

  // Metrics: export every 60 seconds to OTEL Collector
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
      // e.g. http://localhost:4318/v1/metrics (OTEL Collector)
    }),
    exportIntervalMillis: 60_000,  // every 60 seconds
  }),
})

sdk.start()

// Graceful shutdown — flush any buffered metrics before process exits
process.on('SIGTERM', () => {
  sdk.shutdown().finally(() => process.exit(0))
})
```

```
// package.json — how instrumentation.ts is loaded first
{
  "scripts": {
    "start": "node --require ./dist/instrumentation.js dist/main.js"
  }
}
```

**Why `--require` and not just importing at the top of main.ts:**

OpenTelemetry works by monkey-patching Node.js modules — it wraps `http`, `pg`, `ioredis`, and other libraries to automatically capture spans and metrics. This patching must happen before those modules are first loaded. If you import `http` before OTEL is initialised, OTEL cannot wrap it.

`--require instrumentation.js` loads the file before Node.js processes any `import` or `require` statements in main.ts. This is the same reason your Spring Boot notes mention that Micrometer setup happens through auto-configuration at the very start of the Spring context — before any beans are created.

---

## The Metrics Service — A Centralised Wrapper

Rather than importing the OTEL meter everywhere in the codebase, FinVerse wraps it in a NestJS injectable service. This follows the same pattern as Micrometer's `MeterRegistry` being injected via `@Autowired` in Spring Boot.

```typescript
// src/common/metrics/metrics.service.ts
import { Injectable, OnModuleInit } from '@nestjs/common'
import { metrics } from '@opentelemetry/api'
import type { Counter, Histogram, ObservableGauge } from '@opentelemetry/api'

@Injectable()
export class MetricsService implements OnModuleInit {
  private meter = metrics.getMeter('core-product')

  // Counters
  private httpRequestsTotal: Counter
  private syncJobsCompleted: Counter
  private syncJobsFailed: Counter
  private goCardlessApiCalls: Counter
  private goCardlessApiErrors: Counter

  // Histograms
  private syncDuration: Histogram
  private httpRequestDuration: Histogram

  // Gauges (observable — value read on demand, not pushed)
  private bullmqQueueWaiting: ObservableGauge

  onModuleInit() {
    // Counters
    this.httpRequestsTotal = this.meter.createCounter(
      'finverse.http.requests.total',
      { description: 'Total number of HTTP requests received' }
    )

    this.syncJobsCompleted = this.meter.createCounter(
      'finverse.transaction_sync.jobs.completed',
      { description: 'Number of sync jobs completed successfully' }
    )

    this.syncJobsFailed = this.meter.createCounter(
      'finverse.transaction_sync.jobs.failed',
      { description: 'Number of sync jobs that failed after all retries' }
    )

    this.goCardlessApiCalls = this.meter.createCounter(
      'finverse.gocardless.api.calls.total',
      { description: 'Total GoCardless API calls made' }
    )

    this.goCardlessApiErrors = this.meter.createCounter(
      'finverse.gocardless.api.errors.total',
      { description: 'GoCardless API calls that returned an error' }
    )

    // Histograms
    this.syncDuration = this.meter.createHistogram(
      'finverse.transaction_sync.duration.ms',
      {
        description: 'Duration of transaction sync jobs in milliseconds',
        unit: 'ms',
        // Bucket boundaries — where to draw the lines for percentile calculation
        // Chosen based on known sync time distribution (most under 30s)
        advice: {
          explicitBucketBoundaries: [
            1000, 3000, 5000, 10000, 15000, 20000, 30000, 45000, 60000
          ]
        }
      }
    )

    this.httpRequestDuration = this.meter.createHistogram(
      'finverse.http.request.duration.ms',
      {
        description: 'HTTP request duration in milliseconds',
        unit: 'ms',
        advice: {
          explicitBucketBoundaries: [10, 25, 50, 100, 250, 500, 1000, 2500, 5000]
        }
      }
    )

    // Observable gauge — OTEL polls this callback every export interval
    // rather than us pushing a value
    this.bullmqQueueWaiting = this.meter.createObservableGauge(
      'finverse.bullmq.queue.waiting',
      { description: 'Number of jobs waiting in the transaction-sync queue' }
    )
  }

  // Public API — called from worker and service code

  recordSyncCompleted(
    jobType: 'INITIAL_SYNC' | 'PERIODIC_SYNC',
    durationMs: number
  ): void {
    this.syncJobsCompleted.add(1, { job_type: jobType })
    this.syncDuration.record(durationMs, { job_type: jobType })
  }

  recordSyncFailed(
    jobType: 'INITIAL_SYNC' | 'PERIODIC_SYNC',
    errorType: string
  ): void {
    this.syncJobsFailed.add(1, {
      job_type: jobType,
      error_type: errorType   // "gocardless_429", "postgres_timeout", etc.
    })
  }

  recordGoCardlessCall(statusCode: number): void {
    this.goCardlessApiCalls.add(1, {
      status_code: String(statusCode)
    })
    if (statusCode >= 400) {
      this.goCardlessApiErrors.add(1, {
        status_code: String(statusCode)
      })
    }
  }

  recordHttpRequest(
    method: string,
    endpoint: string,
    statusCode: number,
    durationMs: number
  ): void {
    const tags = { method, endpoint, status_code: String(statusCode) }
    this.httpRequestsTotal.add(1, tags)
    this.httpRequestDuration.record(durationMs, tags)
  }

  // Called during setup to register the queue depth gauge callback
  registerQueueDepthGauge(
    getWaitingCount: () => Promise<number>
  ): void {
    this.bullmqQueueWaiting.addCallback(async (result) => {
      const count = await getWaitingCount()
      result.observe(count, { queue: 'transaction-sync' })
    })
  }
}
```

---

## Where Metrics Are Actually Called — Real Code Locations

Metrics are called from three places in Core Product:

**1. NestJS Interceptor — for all HTTP metrics:**

```typescript
// src/common/interceptors/metrics.interceptor.ts
@Injectable()
export class MetricsInterceptor implements NestInterceptor {
  constructor(private readonly metricsService: MetricsService) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const startTime = Date.now()
    const request = context.switchToHttp().getRequest()
    const method = request.method
    // Normalise path — replace UUIDs with :id to avoid metric explosion
    // /v1/accounts/a3f2b1c0-... → /v1/accounts/:accountId
    const endpoint = this.normalisePath(request.route?.path ?? request.url)

    return next.handle().pipe(
      tap((response) => {
        const duration = Date.now() - startTime
        const statusCode = context.switchToHttp().getResponse().statusCode
        this.metricsService.recordHttpRequest(method, endpoint, statusCode, duration)
      }),
      catchError((error) => {
        const duration = Date.now() - startTime
        const statusCode = error.status ?? 500
        this.metricsService.recordHttpRequest(method, endpoint, statusCode, duration)
        throw error
      })
    )
  }

  private normalisePath(path: string): string {
    // Replace UUIDs with placeholder to avoid cardinality explosion
    // (one metric per UUID = millions of metric series = Datadog bill shock)
    return path.replace(
      /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi,
      ':id'
    )
  }
}
```

**Why normalise paths?** This is a critical production concern called **metric cardinality**. If you include the raw UUID in the metric tag, you create one unique metric series per user per request. With 180,000 MAUs, that is potentially millions of unique metric series — which Datadog charges per series and which makes dashboards unusable. `/v1/accounts/:id` is one metric series. `/v1/accounts/a3f2b1c0-...` per user is a disaster.

**Java parallel:** this is why your Spring Boot notes show `http.server.requests` using `uri` tag with templated paths (`/accounts/{id}`) rather than actual URLs — same reason, same solution.

**2. BullMQ Worker — for job metrics:**

```typescript
// transaction-sync.worker.ts
@Processor('transaction-sync', { concurrency: 10 })
export class TransactionSyncWorker extends WorkerHost {

  constructor(
    private readonly metricsService: MetricsService,
    // ... other dependencies
  ) {
    super()
  }

  async process(job: Job): Promise<void> {
    const startTime = Date.now()

    try {
      await this.handleSync(job)

      const duration = Date.now() - startTime
      // Record success metric with duration
      this.metricsService.recordSyncCompleted(job.name as any, duration)

    } catch (error) {
      // Record failure metric with error type
      const errorType = this.classifyError(error)
      this.metricsService.recordSyncFailed(job.name as any, errorType)
      throw error   // re-throw so BullMQ handles retry/fail
    }
  }

  private classifyError(error: Error): string {
    if (error.message.includes('429')) return 'gocardless_rate_limit'
    if (error.message.includes('503')) return 'gocardless_unavailable'
    if (error.message.includes('timeout')) return 'postgres_timeout'
    return 'unknown'
  }
}
```

**3. GoCardless Service — for external API metrics:**

```typescript
// gocardless.service.ts
async fetchAllTransactions(accountId: string): Promise<GoCardlessTransaction[]> {
  try {
    const response = await this.httpClient.get(
      `/accounts/${accountId}/transactions`
    )

    // Record successful API call
    this.metricsService.recordGoCardlessCall(response.status)
    return response.data.transactions

  } catch (error) {
    const statusCode = error.response?.status ?? 0
    // Record failed API call
    this.metricsService.recordGoCardlessCall(statusCode)
    throw error
  }
}
```

---

## The Complete Metrics Dashboard at FinVerse

This is what the Datadog dashboard for Core Product Service shows. These are real metrics, mapped to real business questions.

```
┌──────────────────────────────────────────────────────────────────────┐
│              CORE PRODUCT — DATADOG DASHBOARD                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ROW 1: SYSTEM HEALTH (is anything on fire right now?)               │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐            │
│  │ HTTP Error Rate│ │ Sync Fail Rate │ │ Queue Depth    │            │
│  │    0.3%        │ │    0.8%        │ │    47 jobs     │            │
│  │    ✅ normal    │ │    ✅ normal    │ │    ✅ normal    │            │
│  └────────────────┘ └────────────────┘ └────────────────┘            │
│                                                                      │
│  ROW 2: LATENCY (how fast are we?)                                   │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  HTTP Request Duration — p50, p95, p99                       │    │
│  │  p50: 45ms   p95: 280ms   p99: 890ms                         │    │
│  │                                                              │    │
│  │  45ms ┤─────────────────────────────────────── p50           │    │
│  │ 280ms ┤─────────────────────────────────────── p95           │    │
│  │ 890ms ┤─────────────────────────────────────── p99           │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ROW 3: SYNC PIPELINE (the most important business metric)           │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  Transaction Sync Duration — p50, p95, p99                   │    │
│  │  INITIAL_SYNC:   p50: 8.2s   p95: 11.4s   p99: 18.7s         │    │
│  │  PERIODIC_SYNC:  p50: 2.1s   p95: 4.8s    p99: 9.2s          │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ROW 4: EXTERNAL DEPENDENCIES (are our suppliers healthy?)           │
│  ┌────────────────────────┐ ┌────────────────────────────────┐       │
│  │ GoCardless Error Rate  │ │ PostgreSQL Connection Pool     │       │
│  │   0.12%  ✅             │ │   14 / 20 active  ✅            │       │
│  └────────────────────────┘ └────────────────────────────────┘       │
│                                                                      │
│  ROW 5: INFRASTRUCTURE                                               │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐            │
│  │ Node.js Heap   │ │ CPU Usage      │ │ Redis Memory   │            │
│  │   412MB / 2GB  │ │   23%          │ │   4.2GB / 10GB │            │
│  │   ✅ fine       │ │   ✅ fine       │ │   ✅ fine       │            │
│  └────────────────┘ └────────────────┘ └────────────────┘            │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Auto-Instrumented Metrics — What You Get For Free

The OpenTelemetry auto-instrumentation package (`@opentelemetry/auto-instrumentations-node`) automatically captures metrics from libraries without any manual code. This is similar to how Spring Boot Actuator auto-configures metrics for Tomcat, JDBC, and HikariCP.

```
AUTO-INSTRUMENTED METRICS IN NESTJS (via OTEL)

HTTP layer (http/https module):
  http.server.request.duration          ← every inbound request
  http.server.active_requests           ← concurrent requests gauge
  http.client.request.duration          ← every outbound HTTP call

PostgreSQL (pg / @prisma/client):
  db.client.connections.usage           ← connection pool gauge
  db.client.operation.duration          ← every query duration

Redis (ioredis):
  db.client.connections.usage           ← Redis connection pool
  db.client.operation.duration          ← every Redis command duration

Node.js runtime:
  process.runtime.nodejs.memory.heap.used    ← heap usage gauge
  process.runtime.nodejs.memory.heap.total   ← heap total
  process.runtime.nodejs.gc.duration         ← garbage collection time
  process.runtime.nodejs.event_loop.delay    ← event loop lag
```

**The event loop delay metric deserves special attention:**

```
EVENT LOOP DELAY — WHY IT MATTERS FOR NODE.JS

In Java, if a thread is slow, other threads continue running.
In Node.js, if the event loop is blocked (CPU-heavy work),
EVERYTHING waits.

Event loop delay measures: how long does a task wait in the
event loop queue before being picked up?

Healthy:  < 10ms   (event loop is free and responsive)
Warning:  10-100ms (something is occasionally blocking it)
Critical: > 100ms  (something is consistently blocking the
                    event loop — CPU-bound code is running)

At FinVerse, if event loop delay spikes during a tax report
generation run, it confirms that the PDF rendering is blocking
the event loop — a signal to reconsider the concurrency setting
for that worker.
```

**Java parallel:** there is no direct equivalent because Java is multi-threaded — one blocked thread does not affect others. Event loop delay is a Node.js-specific concern that has no counterpart in Spring Boot monitoring.

---

## The "How Did You Measure That" Answer — Built Out

Now let's build the complete, interview-ready answer for a specific FinVerse performance improvement. This is the story structure you will use in interviews.

```
PERFORMANCE IMPROVEMENT STORY — INITIAL_SYNC DURATION

SITUATION:
  Three months into my contract, I noticed in Datadog that the
  INITIAL_SYNC job duration p95 was 28.4 seconds — approaching
  the 30-second lock TTL for BullMQ jobs. Users with 3+ bank
  accounts were experiencing stalled jobs at a rate of ~12%.

MEASUREMENT (before):
  Metric: finverse.transaction_sync.duration.ms
  Filter: job_type = INITIAL_SYNC
  Window: 7-day average
  p50:  15.2s
  p95:  28.4s    ← the problem
  p99:  41.0s

THE INVESTIGATION:
  I looked at Datadog APM traces for slow INITIAL_SYNC jobs
  (duration > 25s). The trace showed:
    - Account 1 GoCardless call: sequential, waited for acc 2
    - Account 2 GoCardless call: sequential, waited for acc 3
    - Accounts were being processed one after another

  Root cause: the original code used a `for` loop with
  `await` inside, which processes accounts sequentially.
  A user with 3 accounts paid the full latency of all
  3 sequential API calls.

THE FIX:
  Refactored to use Promise.all() to fetch accounts
  concurrently (within the existing rate limiter bounds):

  // Before:
  for (const accountId of accountIds) {
    await this.syncAccount(accountId)  // sequential
  }

  // After:
  await Promise.all(
    accountIds.map(id => this.syncAccount(id))  // concurrent
  )

MEASUREMENT (after):
  Deployed to staging, ran 24-hour observation with real data.
  Same metric, same filter, same window:
  p50:  6.1s   (was 15.2s — 60% improvement)
  p95:  11.2s  (was 28.4s — 61% improvement)
  p99:  18.7s  (was 41.0s — 54% improvement)

  Stalled job rate: dropped from 12% to under 0.3%

  In production, the same improvement was visible within
  the first hour of deployment — Datadog APM showed the
  INITIAL_SYNC span duration drop in real time.
```

This is the complete "how did you measure that" answer. Specific metric name. Specific percentile. Specific before and after numbers. Specific tool used to find the root cause. Specific code change. Specific after measurement.

---

## Chapter 2 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 2 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Four metric types:                                             │
│  Counter   → only goes up, plot as rate of change               │
│  Gauge     → current state, read as-is                          │
│  Histogram → distribution of values, used for percentiles       │
│  Summary   → similar to histogram, rarely used in practice      │
│                                                                 │
│  Always use percentiles (p50/p95/p99), not averages             │
│  Averages hide outliers. p99 reveals them.                      │
│                                                                 │
│  Java vs Node.js:                                               │
│  Micrometer Counter → OTEL Counter                              │
│  Micrometer Timer   → OTEL Histogram                            │
│  Micrometer Gauge   → OTEL ObservableGauge                      │
│  MeterRegistry      → metrics.getMeter() in OTEL                │
│                                                                 │
│  Critical production concern: metric cardinality                │
│  Normalise paths (/v1/accounts/:id not raw UUIDs)               │
│  Never use user IDs or request IDs as metric tags               │
│                                                                 │
│  Event loop delay is a Node.js-specific metric                  │
│  High delay = event loop is being blocked by CPU work           │
│  No Java equivalent (multi-threaded model doesn't have this)    │
│                                                                 │
│  Instrumentation must happen BEFORE any imports                 │
│  Use --require flag, not top-level import in main.ts            │
│  Same reason as Spring auto-configuration order                 │
│                                                                 │
│  The interview answer for "how did you measure X%":             │
│  Specific metric + specific percentile + specific window        │
│  + specific before number + specific after number               │
│  + which tool showed you the root cause                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

