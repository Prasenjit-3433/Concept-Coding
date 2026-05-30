Writing Chapter 3 now.

---

# Monitoring, Distributed Tracing & Logging — Chapter 3: Distributed Tracing

---

## Why This Chapter Exists

Metrics tell you *something is wrong*. Logs tell you *what happened* in one place. But in a distributed system, neither answers the most important debugging question: **which service, which function, which line of code is responsible for this slowness or this error?**

Distributed tracing answers that question. It is the tool you reach for when a user says "the app is slow" and you need to find exactly where the time is being lost — across multiple services, databases, and external APIs.

This is also the chapter where the Java-to-Node.js mapping is most direct. Your Spring Boot notes cover OpenTelemetry in detail. The concepts are identical. The code is different. Let's build the map.

---

## Start With the Problem: Logs Are Islands

Before understanding tracing, you need to feel the pain it solves.

Imagine a user opens the FinVerse app and the portfolio screen takes 8 seconds to load. They report it. You need to find out why.

Here is what you have without distributed tracing:

```
WITHOUT DISTRIBUTED TRACING

API Gateway logs (Container 1, eu-west-1a):
  2024-01-15T14:23:11.421Z INFO  GET /v1/portfolio → forwarded to core-product

Core Product logs (Container 2, eu-west-1b):
  2024-01-15T14:23:11.489Z INFO  [PortfolioController] Loading portfolio
  2024-01-15T14:23:11.491Z INFO  [CacheService] Redis miss: pf:val:usr_123
  2024-01-15T14:23:11.492Z INFO  [MarketDataClient] Calling market data service
  2024-01-15T14:23:19.103Z INFO  [PortfolioController] Portfolio loaded

Market Data Service logs (Container 3, Go, eu-west-1c):
  2024-01-15T14:23:11.521Z INFO  portfolio valuation request received
  2024-01-15T14:23:19.089Z INFO  portfolio valuation completed in 7568ms

PostgreSQL query logs:
  2024-01-15T14:23:11.930Z  SELECT * FROM holdings WHERE portfolio_id = ...
  duration: 43ms

Questions you cannot answer:
  ✗ Which container processed this specific user's request?
  ✗ Was the 8 seconds all in Market Data Service, or split?
  ✗ What was slow inside Market Data Service?
  ✗ Was the PostgreSQL query part of Market Data or Core Product?
  ✗ How do these four log entries connect to the same request?
```

The logs exist. The data is there. But the connection between them is missing. You know the Market Data Service took 7.5 seconds total — but you do not know which step inside it was slow.

This is what distributed tracing solves.

---

## The Core Concepts: Trace, Span, Parent Span

Let's define these precisely — no buzzwords.

### Trace

A **trace** is the complete record of a single request's journey through your entire system. Every service the request touches, every database query it triggers, every external API call it makes — all of it is part of one trace.

A trace is identified by a single **Trace ID** — a random 128-bit hexadecimal string — that is generated when the request first enters the system and is carried through every service the request touches.

```
ONE TRACE = ONE REQUEST'S COMPLETE JOURNEY

Trace ID: 9e7d21299f4ea8a1cb3f4d2e8b1a0c5f

Everything that happened for this one request:
  API Gateway → Core Product → Redis → Market Data → PostgreSQL
  All connected by this single Trace ID
```

### Span

A **span** is one unit of work within a trace. Every operation — an HTTP handler, a database query, a Redis read, an external API call — creates its own span.

A span records:
- **Name** — what operation this was (`GET /v1/portfolio`, `SELECT holdings`, `Redis GET`)
- **Start time** — when the operation began
- **End time** — when it finished
- **Duration** — end minus start
- **Status** — success or error
- **Attributes** — key-value metadata (`http.status_code: 200`, `db.statement: SELECT...`)
- **Span ID** — a unique identifier for this specific span
- **Parent Span ID** — which span triggered this one

### Parent Span ID — The Connection Between Spans

The Parent Span ID is what turns a flat list of spans into a tree that shows the request hierarchy.

```
SPANS CONNECTED BY PARENT-CHILD RELATIONSHIPS

Trace ID: 9e7d21299f4ea8a1

Span A: GET /v1/portfolio (API Gateway)
  SpanID: aaa111
  ParentSpanID: none (root span)
  Duration: 8,021ms

  Span B: GET /v1/portfolio (Core Product handler)
    SpanID: bbb222
    ParentSpanID: aaa111
    Duration: 7,998ms

    Span C: Redis GET pf:val:usr_123
      SpanID: ccc333
      ParentSpanID: bbb222
      Duration: 1ms
      Status: MISS

    Span D: HTTP GET market-data-service/portfolio/usr_123
      SpanID: ddd444
      ParentSpanID: bbb222
      Duration: 7,568ms   ← THIS IS WHERE THE TIME IS

      Span E: PostgreSQL SELECT holdings
        SpanID: eee555
        ParentSpanID: ddd444
        Duration: 43ms

      Span F: Valuation computation (Go)
        SpanID: fff666
        ParentSpanID: ddd444
        Duration: 7,482ms   ← THE ACTUAL SLOW PART
        Attributes: { portfolio_holdings_count: 847 }

    Span G: PostgreSQL SELECT holding metadata
      SpanID: ggg777
      ParentSpanID: bbb222
      Duration: 18ms
```

In Datadog APM, this renders as a waterfall diagram:

```
DATADOG APM WATERFALL VIEW

Timeline ──────────────────────────────────────────────────────► 8s

[A: API Gateway GET /v1/portfolio                                8021ms]
  [B: Core Product handler                                       7998ms]
    [C: Redis GET ──] 1ms
    [D: Market Data HTTP call                              7568ms      ]
      [E: PostgreSQL SELECT] 43ms
      [F: Valuation computation                     7482ms            ]
                                                    ↑
                                          THIS is the bottleneck
    [G: PostgreSQL SELECT] 18ms

Investigation: 847 holdings — valuation loop over large portfolio
Fix: cache the valuation result more aggressively
```

Without the trace, you would have seen "portfolio endpoint is slow." With the trace, you see that 93.5% of the time is spent in the valuation computation for a user with 847 holdings. The investigation takes 30 seconds instead of hours.

---

## How Trace Context Propagates: The W3C traceparent Header

This is the mechanism that connects spans across service boundaries. It is the same concept your Spring Boot notes describe for `ServerHttpObservationFilter` propagating Trace IDs.

When Core Product calls Market Data Service, it needs to tell Market Data Service: "you are part of Trace `9e7d21299f4ea8a1`. Your parent span is `ddd444`." This information travels in an HTTP header.

The W3C standard for this header is called **`traceparent`**:

```
traceparent: 00-9e7d21299f4ea8a1cb3f4d2e8b1a0c5f-ddd444aabb112233-01

FORMAT: {version}-{traceId}-{parentSpanId}-{flags}

  version:       00  (always 00 for W3C standard)
  traceId:       9e7d21299f4ea8a1cb3f4d2e8b1a0c5f  (128-bit, 32 hex chars)
  parentSpanId:  ddd444aabb112233                   (64-bit, 16 hex chars)
  flags:         01  (01 = sampled, 00 = not sampled)
```

Here is the complete flow:

```
TRACE PROPAGATION ACROSS SERVICES

Mobile App sends request to API Gateway
  No traceparent header yet

API Gateway (OTEL auto-instrumentation):
  Detects: no traceparent header
  Creates: new Trace ID = 9e7d21299f4ea8a1...
  Creates: root Span A (SpanID = aaa111)
  Forwards request to Core Product WITH header:
    traceparent: 00-9e7d21299f4ea8a1...-aaa111...-01

Core Product receives request:
  OTEL auto-instrumentation reads traceparent header
  Extracts: TraceID = 9e7d21299f4ea8a1...
            ParentSpanID = aaa111...
  Creates: Span B (SpanID = bbb222, Parent = aaa111)
  When calling Market Data Service, adds header:
    traceparent: 00-9e7d21299f4ea8a1...-bbb222...-01
                                         ↑
                              Core Product's span becomes
                              the parent for Market Data spans

Market Data Service (Go) receives request:
  OTEL auto-instrumentation reads traceparent header
  Extracts: TraceID = 9e7d21299f4ea8a1...
            ParentSpanID = bbb222...
  Creates: Span D (SpanID = ddd444, Parent = bbb222)
  Creates child spans for PostgreSQL (E) and computation (F)

Result: All spans share TraceID 9e7d21299f4ea8a1...
        All connected via Parent-Child chain
        Datadog receives all spans and reconstructs the tree
```

**Java parallel:** your Spring Boot notes describe `ServerHttpObservationFilter` (server side — reads `traceparent` on incoming requests) and the interceptor added to `RestClient.Builder` (client side — writes `traceparent` on outgoing requests). In Node.js, OTEL auto-instrumentation does both automatically by patching the `http` and `https` modules.

---

## Auto-Instrumentation in NestJS: What Happens Automatically

When you install `@opentelemetry/auto-instrumentations-node` and initialise it before your application starts, it automatically patches the following libraries:

```
WHAT OTEL AUTO-INSTRUMENTATION PATCHES IN NODE.JS

HTTP/HTTPS (built-in Node.js module):
  → Creates a span for every inbound HTTP request
  → Creates a span for every outbound HTTP request (to GoCardless, Market Data)
  → Adds traceparent header to all outbound requests automatically

@prisma/client (PostgreSQL via Prisma):
  → Creates a span for every database query
  → Records: db.statement (the SQL), db.operation, db.name
  → Duration includes connection acquisition + query execution

ioredis (Redis):
  → Creates a span for every Redis command
  → Records: db.statement (the command), db.type: redis

@nestjs/core:
  → Creates spans for NestJS middleware, guards, interceptors
  → Links controller spans to their HTTP spans
```

This is what your Spring Boot notes describe for `FeignClient` (`auto-configured, works out of the box`) and `RestClient.Builder` (needs to be injected, not created directly) — in Node.js with OTEL, the auto-instrumentation handles all of this transparently.

---

## Setting Up Distributed Tracing in NestJS

The setup builds on the instrumentation file from Chapter 2. We add the tracing configuration:

```typescript
// src/instrumentation.ts — updated with tracing
import { NodeSDK } from '@opentelemetry/sdk-node'
import { OTLPTraceExporter } from '@opentelemetry/exporter-otlp-http'
import { OTLPMetricExporter } from '@opentelemetry/exporter-otlp-http'
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics'
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base'
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node'
import { Resource } from '@opentelemetry/resources'
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions'
import { TraceIdRatioBasedSampler } from '@opentelemetry/sdk-trace-base'

const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'core-product',
    [SemanticResourceAttributes.SERVICE_VERSION]: process.env.APP_VERSION ?? '0.0.0',
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.NODE_ENV,
  }),

  // TRACING CONFIGURATION
  traceExporter: new OTLPTraceExporter({
    url: `${process.env.OTEL_EXPORTER_OTLP_ENDPOINT}/v1/traces`,
  }),

  spanProcessor: new BatchSpanProcessor(
    new OTLPTraceExporter({
      url: `${process.env.OTEL_EXPORTER_OTLP_ENDPOINT}/v1/traces`,
    }),
    {
      maxQueueSize: 2048,       // max spans buffered in memory
      maxExportBatchSize: 512,  // max spans per export request
      scheduledDelayMillis: 5_000,  // export every 5 seconds
    }
  ),

  // SAMPLING — do not trace 100% of requests in production
  sampler: new TraceIdRatioBasedSampler(
    process.env.NODE_ENV === 'production' ? 0.1 : 1.0
    // Production: trace 10% of requests
    // Development/staging: trace 100%
  ),

  // METRICS CONFIGURATION (from Chapter 2)
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: `${process.env.OTEL_EXPORTER_OTLP_ENDPOINT}/v1/metrics`,
    }),
    exportIntervalMillis: 60_000,
  }),

  // AUTO-INSTRUMENTATION — patches http, prisma, redis, etc.
  instrumentations: [
    getNodeAutoInstrumentations({
      // Configure which auto-instrumentations are active
      '@opentelemetry/instrumentation-http': {
        enabled: true,
        // Don't trace health check endpoints — they're noisy and useless
        ignoreIncomingRequestHook: (req) => {
          return req.url === '/health' || req.url === '/metrics'
        },
      },
      '@opentelemetry/instrumentation-prisma': { enabled: true },
      '@opentelemetry/instrumentation-ioredis': { enabled: true },
      // Disable instrumentation for libraries we don't use
      '@opentelemetry/instrumentation-graphql': { enabled: false },
      '@opentelemetry/instrumentation-mongoose': { enabled: false },
    }),
  ],
})

sdk.start()

process.on('SIGTERM', () => {
  sdk.shutdown().finally(() => process.exit(0))
})
```

---

## Sampling: Why You Don't Trace Everything

This is a nuance your Spring Boot notes touch on (`management.tracing.sampling.probability=1.0` for dev). Let's understand it properly.

At FinVerse, Core Product handles thousands of HTTP requests per minute. If every single request creates a full trace — with spans for every DB query, Redis call, and external API call — the volume of trace data would be enormous.

```
SAMPLING MATH AT FINVERSE

Peak traffic: ~350 requests/second to Core Product
Spans per request: ~8 on average (handler + 2 DB + 1 Redis + 1 external)
Total spans/second: 350 × 8 = 2,800 spans/second
Spans per minute: 168,000 spans/minute
Spans per hour: ~10 million spans/hour

At 100% sampling:
  Datadog cost: very high (charges per span ingested)
  OTEL Collector: high CPU and memory to batch 10M spans/hr
  Storage: significant

At 10% sampling (production):
  1,000,000 spans/hour ingested
  Cost: 10× cheaper
  Still enough data to find performance patterns
  Still traces all errors (error always sampled — see below)
```

**Head-based vs tail-based sampling:**

FinVerse uses **head-based sampling** — the decision to trace a request is made at the entry point (API Gateway) before the request is processed. The `TraceIdRatioBasedSampler` makes this decision randomly based on the Trace ID.

```
HEAD-BASED SAMPLING (what FinVerse uses)

Request arrives at API Gateway
        │
OTEL generates Trace ID → hash it → is hash < 10%? 
        │
   YES (10% chance)           NO (90% chance)
        │                          │
   Create spans                Don't create spans
   Propagate traceparent        Don't add traceparent header
   header downstream            (downstream services also skip)
```

**Always sample errors — the critical override:**

Head-based sampling has one problem: the 90% of requests that are not sampled include both fast, healthy requests AND slow or failing requests. You might miss tracing a critical error.

The solution is to always sample requests that result in errors:

```typescript
// custom sampler that always samples errors
import { Sampler, SamplingResult, SamplingDecision } from '@opentelemetry/sdk-trace-base'

class AlwaysSampleErrorsSampler implements Sampler {
  private baseRate: number

  constructor(baseRate: number) {
    this.baseRate = baseRate
  }

  shouldSample(context, traceId, spanName, spanKind, attributes): SamplingResult {
    // Always sample if the span attributes indicate an error
    if (attributes['http.status_code'] >= 500) {
      return { decision: SamplingDecision.RECORD_AND_SAMPLED }
    }

    // Otherwise, apply base rate sampling
    const traceIdAsNumber = parseInt(traceId.slice(0, 8), 16)
    const shouldSample = (traceIdAsNumber / 0xFFFFFFFF) < this.baseRate

    return {
      decision: shouldSample
        ? SamplingDecision.RECORD_AND_SAMPLED
        : SamplingDecision.NOT_RECORD
    }
  }

  toString(): string {
    return `AlwaysSampleErrorsSampler(${this.baseRate})`
  }
}
```

**Java parallel:** your Spring Boot notes show `management.tracing.sampling.probability=1.0` for dev and mention sampling. The concept is identical — Micrometer Tracing's `ProbabilityBasedSampler` is the Spring Boot equivalent of `TraceIdRatioBasedSampler` in OTEL Node.js.

---

## Manual Span Creation: When Auto-Instrumentation Is Not Enough

Auto-instrumentation captures HTTP calls, DB queries, and Redis commands. But it cannot capture business logic spans — "how long did the categorisation engine take for this specific sync?" You create those manually.

```typescript
// src/modules/transactions/categorisation.service.ts
import { trace, context, SpanStatusCode } from '@opentelemetry/api'

@Injectable()
export class CategorizationService {

  private readonly tracer = trace.getTracer('categorization-service')

  async categorizeTransactions(
    transactions: RawTransaction[]
  ): Promise<CategorizedTransaction[]> {

    // Create a manual span for the categorization work
    return this.tracer.startActiveSpan(
      'categorize_transactions',
      {
        attributes: {
          'transaction.count': transactions.length,
          'transaction.user_id': transactions[0]?.userId,
        }
      },
      async (span) => {
        try {
          const results = await this.runCategorizationRules(transactions)

          // Add result attributes to the span
          span.setAttributes({
            'transaction.categorized_count': results.filter(r => r.categoryId).length,
            'transaction.uncategorized_count': results.filter(r => !r.categoryId).length,
          })

          span.setStatus({ code: SpanStatusCode.OK })
          return results

        } catch (error) {
          // Mark span as error — Datadog APM highlights these in red
          span.setStatus({
            code: SpanStatusCode.ERROR,
            message: error.message
          })
          span.recordException(error)
          throw error

        } finally {
          // ALWAYS end the span — even if an error is thrown
          span.end()
        }
      }
    )
  }
}
```

The pattern is identical to your Spring Boot notes:

```
MANUAL SPAN CREATION — JAVA vs NODE.JS

Java (from your notes):
  Span parentSpan = tracer.currentSpan()
  Span childSpan = tracer.nextSpan(parentSpan).name("op-name")
  childSpan.start()
  Tracer.SpanInScope scope = tracer.withSpan(childSpan)
  try {
    // your work
  } finally {
    scope.close()
    childSpan.end()
  }

Node.js (OTEL):
  tracer.startActiveSpan('op-name', async (span) => {
    try {
      // your work
    } finally {
      span.end()   // ALWAYS end the span
    }
  })

Same principle:
  → Create a span
  → Make it the "current" span (so child spans parent correctly)
  → Do the work
  → End the span in finally (always, even on error)
```

---

## BullMQ Jobs and Tracing: The Async Boundary Problem

This is a problem that does not exist in Java (because `@Async` stays in the same process) but is critical in Node.js with BullMQ (because jobs run in a completely separate container).

When an HTTP request enqueues a BullMQ job, there is a natural break in the trace. The HTTP handler finishes and returns 202. Later — potentially minutes later, in a different container — a worker picks up the job. How do you connect the trace of the HTTP request to the trace of the job execution?

```
THE ASYNC TRACING PROBLEM

HTTP Request (Core Product API Container)
  Trace ID: 9e7d21299f4ea8a1
  Span: POST /v1/accounts/connect  [returns 202]
        │
        │  queue.add('INITIAL_SYNC', { userId, accountIds })
        │
        ▼
  BullMQ Job in Redis (job data stored)

  ... time passes (seconds to minutes) ...

BullMQ Worker (Worker Container — different process)
  Picks up INITIAL_SYNC job
  Creates new trace: a3f2c1d0e9b8a7f6   ← DIFFERENT trace ID

  The HTTP trace and the job trace are DISCONNECTED.
  In Datadog: two separate traces with no visible relationship.
  You cannot answer: "which user request triggered this job?"
```

**The solution: store the Trace ID in the job payload**

```typescript
// account.service.ts — when enqueueing the job
import { trace } from '@opentelemetry/api'

async handleCallbackSuccess(userId: string): Promise<void> {
  // ... bank connection logic ...

  // Capture the current trace context
  const currentSpan = trace.getActiveSpan()
  const spanContext = currentSpan?.spanContext()

  await this.syncQueue.add(
    'INITIAL_SYNC',
    {
      userId,
      accountIds,
      // Store trace context in the job payload
      // The worker will use this to link its spans to this trace
      _traceContext: spanContext ? {
        traceId: spanContext.traceId,
        spanId: spanContext.spanId,
        traceFlags: spanContext.traceFlags,
      } : undefined,
    },
    {
      jobId: `initial-sync-${userId}`,
      priority: 1,
      attempts: 3,
    }
  )
}
```

```typescript
// transaction-sync.worker.ts — when processing the job
import { trace, context, SpanKind, ROOT_CONTEXT } from '@opentelemetry/api'
import { W3CTraceContextPropagator } from '@opentelemetry/core'

@Processor('transaction-sync', { concurrency: 10 })
export class TransactionSyncWorker extends WorkerHost {

  private readonly tracer = trace.getTracer('transaction-sync-worker')

  async process(job: Job): Promise<void> {
    const { _traceContext, ...jobData } = job.data

    // Reconstruct the trace context from the job payload
    // This creates a LINK between the HTTP trace and this job span
    // rather than a parent-child relationship (they are async)
    const linkedContext = _traceContext
      ? trace.setSpanContext(ROOT_CONTEXT, {
          traceId: _traceContext.traceId,
          spanId: _traceContext.spanId,
          traceFlags: _traceContext.traceFlags,
          isRemote: true,
        })
      : ROOT_CONTEXT

    // Start the job span with a link to the originating HTTP span
    return this.tracer.startActiveSpan(
      `bullmq.process.${job.name}`,
      {
        kind: SpanKind.CONSUMER,
        attributes: {
          'bullmq.job.id': job.id,
          'bullmq.job.name': job.name,
          'bullmq.queue': 'transaction-sync',
          'bullmq.attempts_made': job.attemptsMade,
          'user.id': jobData.userId,
        },
        links: _traceContext ? [{ context: trace.setSpanContext(ROOT_CONTEXT, {
          traceId: _traceContext.traceId,
          spanId: _traceContext.spanId,
          traceFlags: _traceContext.traceFlags,
          isRemote: true,
        }) }] : [],
      },
      async (span) => {
        try {
          await this.handleSync({ ...job, data: jobData })
          span.setStatus({ code: SpanStatusCode.OK })
        } catch (error) {
          span.setStatus({
            code: SpanStatusCode.ERROR,
            message: error.message
          })
          span.recordException(error)
          throw error
        } finally {
          span.end()
        }
      }
    )
  }
}
```

In Datadog APM, the job span appears as a separate trace — but with a **link** pointing back to the originating HTTP trace. You can click from the job trace to the HTTP trace that created it. The async boundary is preserved but the connection is visible.

```
DATADOG APM — LINKED TRACES

Trace 1: POST /v1/accounts/connect (HTTP)
  TraceID: 9e7d21299f4ea8a1
  Duration: 245ms
  Status: 202 Accepted
  ↓ Link →

Trace 2: bullmq.process.INITIAL_SYNC (Worker)
  TraceID: a3f2c1d0e9b8a7f6
  Duration: 12,430ms
  Linked from: 9e7d21299f4ea8a1 (the HTTP request)
  Spans:
    [GoCardless fetchTransactions acc_1]  3,240ms
    [GoCardless fetchTransactions acc_2]  2,890ms
    [GoCardless fetchTransactions acc_3]  3,110ms
    [PostgreSQL createMany 247 rows]        890ms
    [PostgreSQL createMany 183 rows]        720ms
    [categorize_transactions]            1,210ms
```

---

## How the Go Market Data Service Participates

This is one of the most powerful aspects of OpenTelemetry — it is language-agnostic. The Go service uses the Go OTEL SDK, but it speaks the same OTLP wire format and honours the same `traceparent` header.

```go
// market-data-service/main.go (Go)
package main

import (
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
    "go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
)

func main() {
    // OTEL setup — identical concept to Node.js
    // Different language, same OTLP wire format
    exporter, _ := otlptracehttp.New(ctx,
        otlptracehttp.WithEndpoint(os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT")),
    )

    tp := trace.NewTracerProvider(
        trace.WithBatcher(exporter),
        trace.WithResource(resource.NewWithAttributes(
            semconv.SchemaURL,
            semconv.ServiceName("market-data"),
        )),
    )
    otel.SetTracerProvider(tp)

    // otelhttp.NewHandler wraps the HTTP server
    // It reads traceparent from incoming requests automatically
    // Creates child spans under the parent from Core Product
    http.Handle("/portfolio/", otelhttp.NewHandler(
        portfolioHandler,
        "market-data.portfolio",
    ))
}
```

When Core Product calls Market Data Service with the `traceparent` header, the Go service's `otelhttp` middleware reads it, extracts the Trace ID and Parent Span ID, and creates its spans as children of the Core Product span. Both services export to the same OTEL Collector. The Collector sends all spans to Datadog. Datadog reconstructs the complete tree.

```
CROSS-LANGUAGE TRACE — HOW IT WORKS

Core Product (NestJS)                Market Data Service (Go)
        │                                      │
  Creates span D                               │
  SpanID: ddd444                               │
        │                                      │
        │  HTTP request with header:           │
        │  traceparent: 00-9e7d...-ddd444-01   │
        ├─────────────────────────────────────►│
        │                                      │  Reads traceparent
        │                                      │  Creates span D-child
        │                                      │  Parent: ddd444
        │                                      │  Runs computation
        │                                      │  Ends span
        │◄─────────────────────────────────────┤
        │  HTTP response                       │
        │                                      │

Both services export spans to OTEL Collector (OTLP format)
Datadog receives all spans → reconstructs the tree
TraceID is the same across both services → one unified trace
```

This is fundamentally different from how you achieve cross-language correlation in Java — where you might use Zipkin-specific formats or Brave instrumentation. OpenTelemetry's OTLP format is language-agnostic. The Go code and the Node.js code produce spans in the same format.

---

## The Complete Trace for POST /v1/accounts/connect

Let's walk through a complete real-world trace at FinVerse. A user connects their Monzo account.

```
COMPLETE TRACE: POST /v1/accounts/connect

TraceID: b2f8d1a0c4e7f3b9
Total Duration: 1,847ms

SPAN TREE:

[A] POST /v1/accounts/connect (API Gateway)
    SpanID: a001
    Duration: 1,847ms
    Attributes: http.method=POST, http.route=/v1/accounts/connect
    │
    └─[B] POST /v1/accounts/connect (Core Product)
          SpanID: b001, Parent: a001
          Duration: 1,821ms
          │
          ├─[C] JWT validation (guard)
          │     SpanID: c001, Parent: b001
          │     Duration: 3ms
          │
          ├─[D] PostgreSQL SELECT bank_connections
          │     (check for existing connection)
          │     SpanID: d001, Parent: b001
          │     Duration: 8ms
          │     Attributes: db.statement=SELECT * FROM bank_connections...
          │
          ├─[E] HTTP POST gocardless.com/api/v2/requisitions
          │     (create GoCardless requisition)
          │     SpanID: e001, Parent: b001
          │     Duration: 1,340ms   ← GoCardless API latency
          │     Attributes: http.url=https://bankaccountdata.gocardless.com
          │                 http.status_code=201
          │
          ├─[F] PostgreSQL INSERT bank_connections
          │     SpanID: f001, Parent: b001
          │     Duration: 12ms
          │
          └─[G] Redis SET usr:prefs:usr_123 (cache invalidation)
                SpanID: g001, Parent: b001
                Duration: 1ms

Linked Trace (async, created when callback fires):
  bullmq.process.INITIAL_SYNC
  Link → TraceID: b2f8d1a0c4e7f3b9
  (the job created by this HTTP request)
```

In Datadog, you see this as:

```
DATADOG WATERFALL

Timeline ──────────────────────────────────────────────────────► 1.85s

[A: API Gateway                                                1847ms]
  [B: Core Product handler                                     1821ms]
    [C: JWT guard] 3ms
    [D: PostgreSQL SELECT] 8ms
    [E: GoCardless HTTP POST                          1340ms          ]
    [F: PostgreSQL INSERT] 12ms
    [G: Redis SET] 1ms

Insight: 72% of this request's time is waiting for GoCardless.
This is expected — it's an external API. No action needed.
But if E started taking 4000ms, Datadog would flag it.
```

---

## Chapter 3 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 3 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Core concepts:                                                 │
│  Trace  → complete record of one request's journey              │
│  Span   → one unit of work within a trace                       │
│  Parent Span ID → connects spans into a tree                    │
│                                                                 │
│  W3C traceparent header:                                        │
│  {version}-{traceId}-{parentSpanId}-{flags}                     │
│  Propagated by OTEL auto-instrumentation automatically          │
│  Same standard across all languages (Node.js, Go, Java)         │
│                                                                 │
│  Java vs Node.js:                                               │
│  ServerHttpObservationFilter ↔ OTEL HTTP auto-instrumentation   │
│  RestClient.Builder interceptor ↔ OTEL http module patch        │
│  Manual span: tracer.nextSpan() ↔ tracer.startActiveSpan()      │
│  Always end span in finally — both Java and Node.js             │
│                                                                 │
│  Sampling: don't trace 100% in production                       │
│  10% base rate + always sample errors                           │
│  Same concept as Spring Boot sampling.probability               │
│                                                                 │
│  BullMQ async boundary:                                         │
│  Store traceContext in job payload at enqueue time              │
│  Reconstruct context in worker to create linked spans           │
│  Result: two traces with a visible link in Datadog APM          │
│                                                                 │
│  Cross-language: Go + Node.js share the same trace              │
│  OTLP format is language-agnostic                               │
│  traceparent header works across any OTEL-instrumented service  │
│                                                                 │
│  Critical setup rule: instrumentation.ts must load BEFORE       │
│  any imports via --require flag                                 │
│  Same principle as Spring auto-configuration order              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

