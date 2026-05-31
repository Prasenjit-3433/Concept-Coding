# Monitoring, Distributed Tracing & Logging — Chapter 5: OpenTelemetry: The Glue Layer

---

## Why This Chapter Exists

Chapters 2, 3, and 4 covered metrics, traces, and logs individually. But in production, these three signals do not live in separate silos — they flow through a unified pipeline into one platform where they are linked together. OpenTelemetry is what makes that unified pipeline possible.

This chapter explains what OpenTelemetry actually is at the mechanical level, why FinVerse uses it, what the OTEL Collector does and why it exists, and how everything — NestJS metrics, NestJS traces, NestJS logs, and Go traces — flows into one coherent picture in Datadog.

---

## The Problem Before OpenTelemetry: Vendor Lock-In

Before OpenTelemetry existed (before 2019), every observability vendor had their own SDK. If you used Datadog, you installed the Datadog SDK and wrote Datadog-specific code. If you used New Relic, you installed the New Relic SDK. If you later decided to switch from Datadog to New Relic — or to an open-source stack like Jaeger + Prometheus — you rewrote all your instrumentation code.

```
BEFORE OPENTELEMETRY — VENDOR LOCK-IN

Code using Datadog SDK:
  const tracer = require('dd-trace').init()
  const span = tracer.startSpan('my-operation')
  // Datadog-specific API calls throughout the codebase

Code using New Relic SDK:
  const newrelic = require('newrelic')
  newrelic.startSegment('my-operation', ...)
  // New Relic-specific API calls throughout the codebase

Code using Jaeger SDK:
  const { initTracer } = require('jaeger-client')
  const tracer = initTracer(config, options)
  // Jaeger-specific API calls throughout the codebase

Problem:
  ✗ Every vendor has a different API
  ✗ Switching vendors = rewriting ALL instrumentation code
  ✗ Two services using different vendors cannot share trace context
  ✗ Library authors cannot instrument their code
    (they don't know which vendor you're using)
```

This is the exact same problem that **SLF4J** solves for Java logging — your Spring Boot notes explain this clearly. SLF4J is a facade: your code calls `log.info()` against the SLF4J API, and at runtime you plug in Logback or Log4j2. Switching logging implementations means changing a dependency, not rewriting every log call.

OpenTelemetry is the SLF4J for observability — but for metrics, traces, and logs simultaneously.

---

## What OpenTelemetry Actually Is

OpenTelemetry is not a backend. It is not a UI. It is not a server you run. It is a **specification** — a set of APIs, SDKs, and a wire protocol — that defines how telemetry data is emitted and transmitted, independent of where it is stored.

```
OPENTELEMETRY — THREE COMPONENTS

┌─────────────────────────────────────────────────────────────────┐
│                    OPENTELEMETRY                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. OTEL API                                                    │
│     ────────                                                    │
│     The interface your application code calls.                  │
│     trace.getTracer(), metrics.getMeter(), logs.getLogger()     │
│                                                                 │
│     Same API across ALL languages:                              │
│     Node.js, Go, Java, Python, Rust, .NET — identical concepts  │
│                                                                 │
│     The API has NO implementation — it does nothing by itself.  │
│     It is like SLF4J: just interfaces.                          │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  2. OTEL SDK                                                    │
│     ────────                                                    │
│     The implementation of the OTEL API for each language.       │
│     @opentelemetry/sdk-node (Node.js)                           │
│     go.opentelemetry.io/otel (Go)                               │
│     io.opentelemetry:opentelemetry-sdk (Java)                   │
│                                                                 │
│     The SDK:                                                    │
│     - Actually creates spans and records measurements           │
│     - Applies sampling decisions                                │
│     - Batches data for efficient export                         │
│     - Exports data in OTLP format to a destination              │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  3. OTLP — OpenTelemetry Protocol                               │
│     ─────────────────────────────                               │
│     The wire format for transmitting telemetry data.            │
│     Protobuf over HTTP or gRPC.                                 │
│     Vendor-agnostic — every OTEL-compatible backend reads it.   │
│                                                                 │
│     Datadog reads OTLP.                                         │
│     Jaeger reads OTLP.                                          │
│     Grafana Tempo reads OTLP.                                   │
│     Switching backends = change the OTLP endpoint URL.          │
│     Application code: unchanged.                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Java parallel from your notes:**

Your Spring Boot notes describe exactly this:

- `spring-boot-starter-actuator` → provides Micrometer (the metrics API)
- `micrometer-tracing-bridge-otel` → bridges Micrometer to OTEL SDK
- `opentelemetry-exporter-otlp` → exports in OTLP format
- `management.otlp.tracing.endpoint` → configure which backend to send to

In Node.js at FinVerse:

- `@opentelemetry/api` → the OTEL API (interfaces)
- `@opentelemetry/sdk-node` → the OTEL SDK (implementation)
- `@opentelemetry/exporter-otlp-http` → exports in OTLP format
- `OTEL_EXPORTER_OTLP_ENDPOINT` → configure which backend

The structure is identical. The Java version has an extra bridge layer (Micrometer) because Spring Boot's ecosystem was built around Micrometer before OTEL existed. Node.js uses OTEL directly.

---

## The OTEL Collector: Why It Exists

At FinVerse, telemetry data does not go directly from NestJS to Datadog. It goes through an intermediate component: the **OTEL Collector**.

This is a separate process (running as a sidecar container alongside each NestJS container) that receives telemetry data, processes it, and forwards it to Datadog.

Many teams skip the Collector and export directly to Datadog. FinVerse uses the Collector. Understanding why is important.

```
WITHOUT OTEL COLLECTOR (direct export):

NestJS → OTLP exporter → Datadog API

Diagram:
  [NestJS Container]
    └── OTLPTraceExporter → https://api.datadoghq.com/v0.2/traces
    └── OTLPMetricExporter → https://api.datadoghq.com/api/v2/series

Problems:
  ✗ Each service must be configured with Datadog API keys
    (stored in Secrets Manager — manageable but spread out)
  ✗ If you add a second destination (e.g. send traces to both
    Datadog AND an internal Jaeger for debugging), you change
    every service
  ✗ No central place to add enrichment (tags, attributes) to
    all telemetry across all services
  ✗ No buffering — if Datadog is briefly unavailable, spans
    are dropped
  ✗ Every service retries independently on Datadog errors


WITH OTEL COLLECTOR (FinVerse's approach):

NestJS → OTLP → Collector → Datadog API

Diagram:
  [NestJS Container]           [Collector Sidecar Container]
    └── OTLPTraceExporter  →   Receives OTLP
    └── OTLPMetricExporter →   Processes (enrichment, filtering)
                               Exports → Datadog API
                          also Exports → future destination
```

The Collector provides four things that direct export cannot:

**1. Separation of concerns — services don't know about Datadog:**

```
Services know: "send telemetry to http://localhost:4317"
               (the local Collector)

Collector knows: "forward everything to Datadog"

If FinVerse switches from Datadog to Grafana Cloud:
  Change: Collector configuration (one file)
  Unchanged: every NestJS service, every Go service
```

**2. Enrichment — add tags to all telemetry in one place:**

```typescript
// otel-collector-config.yaml
processors:
  resource:
    attributes:
      # Add these attributes to EVERY span, metric, and log
      # from ALL services sending to this collector
      - key: deployment.environment
        value: "production"
        action: upsert
      - key: cloud.provider
        value: "aws"
        action: upsert
      - key: cloud.region
        value: "eu-west-1"
        action: upsert
```

Without the Collector, each service would need to add these attributes itself. With the Collector, it is done once centrally.

**3. Buffering and retry — protection against Datadog outages:**

```
Collector pipeline:

  NestJS → [Collector memory buffer] → Datadog

  If Datadog is unavailable for 60 seconds:
    Collector buffers spans in memory
    Retries with exponential backoff
    When Datadog recovers: flush buffer, no data lost

  Without Collector:
    NestJS OTLP exporter gets connection refused
    Spans are dropped (export timeout)
    Data loss during Datadog maintenance windows
```

**4. Fan-out — send to multiple destinations:**

```yaml
# otel-collector-config.yaml
exporters:
  datadog:
    api:
      key: ${DATADOG_API_KEY}

  # During debugging: also send to local Jaeger
  jaeger:
    endpoint: localhost:14250

service:
  pipelines:
    traces:
      exporters: [datadog, jaeger]  # fan-out to both
```

---

## The Complete OTEL Collector Configuration at FinVerse

```yaml
# otel-collector-config.yaml
# Deployed as a sidecar container alongside each NestJS service

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317    # NestJS sends traces here
      http:
        endpoint: 0.0.0.0:4318    # NestJS sends metrics here

processors:
  # Batch spans before exporting — reduces API calls
  batch:
    timeout: 5s              # wait up to 5s to fill a batch
    send_batch_size: 512     # or export when 512 spans accumulated

  # Add environment-level attributes to all telemetry
  resource:
    attributes:
      - key: deployment.environment
        value: "${ENVIRONMENT}"   # injected from ECS task definition
        action: upsert
      - key: cloud.region
        value: "eu-west-1"
        action: upsert

  # Memory limiter — prevent Collector from consuming too much RAM
  # If memory exceeds limit, stop accepting new data (backpressure)
  memory_limiter:
    limit_mib: 512           # max 512MB for the Collector sidecar
    spike_limit_mib: 128
    check_interval: 5s

  # Filter out noisy spans that add no value
  filter:
    spans:
      exclude:
        match_type: strict
        span_names:
          - "health_check"
          - "GET /health"
          - "GET /metrics"

exporters:
  # Send traces to Datadog APM
  datadog:
    api:
      key: "${DATADOG_API_KEY}"
      site: datadoghq.eu   # EU data residency — important for GDPR

  # Send logs to Datadog Log Management
  # (logs from Pino go via CloudWatch → Datadog separately,
  #  but structured log events can also go through OTEL)
  logging:
    verbosity: basic        # Collector's own internal logs

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, resource, batch, filter]
      exporters: [datadog]

    metrics:
      receivers: [otlp]
      processors: [memory_limiter, resource, batch]
      exporters: [datadog]
```

---

## How the Go Market Data Service Connects

The Go service uses the Go OTEL SDK — same OTLP protocol, same Collector endpoint. From the Collector's perspective, it does not matter whether the telemetry comes from NestJS or Go. Both speak OTLP.

```go
// market-data-service/internal/telemetry/setup.go

package telemetry

import (
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
    "go.opentelemetry.io/otel/sdk/resource"
    "go.opentelemetry.io/otel/sdk/trace"
    semconv "go.opentelemetry.io/otel/semconv/v1.21.0"
)

func InitTracing(ctx context.Context) (func(), error) {
    // Export to the OTEL Collector sidecar — same endpoint as NestJS
    exporter, err := otlptracehttp.New(ctx,
        otlptracehttp.WithEndpoint(os.Getenv("OTEL_COLLECTOR_ENDPOINT")),
        otlptracehttp.WithInsecure(),  // TLS terminated at the sidecar
    )
    if err != nil {
        return nil, err
    }

    tp := trace.NewTracerProvider(
        trace.WithBatcher(exporter),
        trace.WithSampler(trace.ParentBased(
            // If the parent (Core Product) sampled this request,
            // we also sample it — maintain the sampling decision
            trace.TraceIDRatioBased(0.1),
        )),
        trace.WithResource(resource.NewWithAttributes(
            semconv.SchemaURL,
            semconv.ServiceName("market-data"),
            semconv.ServiceVersion(os.Getenv("APP_VERSION")),
            semconv.DeploymentEnvironment(os.Getenv("ENVIRONMENT")),
        )),
    )

    otel.SetTracerProvider(tp)

    // Return a cleanup function for graceful shutdown
    return func() {
        tp.Shutdown(ctx)
    }, nil
}
```

```go
// market-data-service/internal/handlers/portfolio.go
// How the Go service reads the traceparent header and creates child spans

import (
    "go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/attribute"
)

var tracer = otel.Tracer("market-data-portfolio")

func portfolioHandler(w http.ResponseWriter, r *http.Request) {
    // otelhttp middleware (wrapping this handler) already:
    // 1. Read traceparent header from the request
    // 2. Created a span with Core Product's span as parent
    // 3. Made this span the "current" span in context

    ctx := r.Context()  // context already has the active span

    // Create a child span for the valuation computation
    ctx, span := tracer.Start(ctx, "compute_portfolio_valuation",
        trace.WithAttributes(
            attribute.String("user.id", getUserId(r)),
            attribute.Int("holdings.count", holdingsCount),
        ),
    )
    defer span.End()  // Go's defer = Node.js's finally

    result, err := computeValuation(ctx, userId)
    if err != nil {
        span.RecordError(err)
        span.SetStatus(codes.Error, err.Error())
        http.Error(w, "valuation failed", 500)
        return
    }

    span.SetAttributes(
        attribute.Float64("portfolio.current_value", result.CurrentValue),
        attribute.Float64("portfolio.return_pct", result.ReturnPercent),
    )

    json.NewEncoder(w).Encode(result)
}
```

**The critical detail — ParentBased sampler in Go:**

When Core Product samples a request (10% base rate), it includes `traceflags: 01` in the `traceparent` header — the `01` means "sampled". When Market Data Service receives this header, the `ParentBased` sampler reads the flag and **preserves the parent's sampling decision**. If Core Product sampled it, Market Data Service also samples it. If Core Product did not sample it, Market Data Service does not either.

This ensures complete traces — you never get a situation where Core Product has spans but Market Data Service has none for the same request.

```
SAMPLING DECISION PROPAGATION

traceparent: 00-9e7d...-bbb222-01
                                ↑↑
                                01 = SAMPLED

Go service ParentBased sampler:
  Reads flag from traceparent
  Parent sampled? → YES → also sample → create spans
  Parent not sampled? → NO → do not create spans

Result: Core Product and Market Data Service always
        have the same sampling decision for any given request.
        Traces are always complete — never partial.
```

---

## The Complete Data Flow: From Code to Datadog

Now let's trace a complete piece of telemetry — a span from a NestJS sync job — through every step of the pipeline until it appears in Datadog APM.

```
COMPLETE TELEMETRY PIPELINE — ONE SPAN'S JOURNEY

STEP 1: Span created in NestJS (Worker Container)
─────────────────────────────────────────────────
  TransactionSyncWorker.process() starts
  OTEL SDK creates a span:
    name: "bullmq.process.INITIAL_SYNC"
    startTime: 1705324991421
    traceId: 9e7d21299f4ea8a1
    spanId: fff333ccc111222
    parentSpanId: none (root of this async trace)
    attributes: {
      bullmq.job.id: "initial-sync-usr_123",
      user.id: "usr_123"
    }
    links: [{ traceId: b2f8d1a0... }]  ← link to HTTP trace


STEP 2: Span ends, added to BatchSpanProcessor queue
─────────────────────────────────────────────────────
  Job completes (or fails)
  span.end() called
  Duration calculated: 12,430ms
  Status set: OK
  Span added to in-memory batch queue


STEP 3: BatchSpanProcessor exports to OTEL Collector
─────────────────────────────────────────────────────
  Every 5 seconds OR when batch reaches 512 spans:
  Spans serialised to OTLP protobuf format
  HTTP POST to http://localhost:4318/v1/traces
  (OTEL Collector sidecar in the same ECS task)
  Payload: ~2KB for this one span


STEP 4: OTEL Collector receives and processes
──────────────────────────────────────────────
  memory_limiter: checks memory pressure → OK
  resource processor: adds attributes:
    deployment.environment: "production"
    cloud.region: "eu-west-1"
  filter processor: not a health check → keep
  batch processor: accumulates with other spans


STEP 5: Collector exports to Datadog
──────────────────────────────────────
  Collector sends batch to Datadog API (EU endpoint)
  Datadog API: https://trace.agent.datadoghq.eu
  Authentication: DATADOG_API_KEY from Secrets Manager
  Format: OTLP (Datadog accepts OTLP natively since 2022)


STEP 6: Datadog processes and indexes
───────────────────────────────────────
  Datadog receives span
  Indexes all attributes as searchable facets
  Links to correlated logs (matching traceId)
  Links to correlated metrics (time-window overlap)
  Makes available in APM trace view


STEP 7: Engineer views in Datadog APM
───────────────────────────────────────
  Search: service:core-product operation:bullmq.process.*
  Filters: env:production duration:>10s
  Finds: this span (12,430ms)
  Views: waterfall with child spans
  Clicks: linked trace → sees the HTTP request that triggered this job
  Clicks: correlated logs → sees Pino log lines from this job execution
```

---

## How All Three Pillars Connect in Datadog

This is the payoff — the moment where metrics, traces, and logs stop being three separate things and become one unified view.

```
DATADOG — THE THREE PILLARS LINKED BY TRACE ID

SCENARIO: Datadog fires an alert at 14:23
  Monitor: "sync error rate > 2% for 5 minutes"

STEP 1: Engineer opens Datadog
  Sees alert: "transaction_sync error rate: 4.2%"
  Time range: 14:18 - 14:23

STEP 2: Click alert → opens Metrics view
  Graph: finverse.transaction_sync.jobs.failed
  Spike visible: started at 14:19
  Tagged by: error_type = "gocardless_rate_limit"
  Conclusion: GoCardless is rate limiting us

STEP 3: Click "View Traces" (Datadog links metrics to traces)
  Filtered automatically: service:core-product
                          operation:bullmq.process.*
                          time:14:18-14:23
                          status:error

  Shows: 47 failed traces in this window

STEP 4: Click one failed trace
  Waterfall view:
    [bullmq.process.INITIAL_SYNC  12,440ms  ERROR]
      [GoCardless HTTP GET  10,023ms  ERROR  429]
        http.status_code: 429
        http.url: https://bankaccountdata.gocardless.com/...
        error.message: "Too Many Requests"

  Root cause confirmed: GoCardless 429

STEP 5: Click "View Logs" (Datadog links trace to logs)
  Filtered automatically by traceId from this trace
  Shows Pino log lines:

    14:19:23.421  WARN  GoCardless returning 429, retrying
      { traceId: "9e7d...", jobId: "initial-sync-usr_123",
        statusCode: 429, attemptsMade: 1, nextRetryIn: 5000 }

    14:19:28.501  WARN  GoCardless returning 429, retrying
      { traceId: "9e7d...", jobId: "initial-sync-usr_123",
        statusCode: 429, attemptsMade: 2, nextRetryIn: 10000 }

    14:19:39.621  ERROR  Sync job failed
      { traceId: "9e7d...", jobId: "initial-sync-usr_123",
        attemptsMade: 3, willRetry: false }

TOTAL TIME TO ROOT CAUSE: ~3 minutes
WITHOUT OBSERVABILITY: hours of log searching, guessing
```

This is the concrete value of linking all three pillars by Trace ID. The investigation that would have taken hours of searching through separate log files, manually correlating timestamps across services, takes three minutes of clicking in Datadog.

---

## The OTEL Collector as a Sidecar — ECS Task Definition

In ECS Fargate, the Collector runs as a second container in the same task definition as NestJS. Both containers share the same network namespace — they communicate over localhost.

```json
// ECS Task Definition (simplified)
{
  "family": "core-product-worker",
  "containerDefinitions": [
    {
      "name": "core-product",
      "image": "123456.dkr.ecr.eu-west-1.amazonaws.com/core-product:abc1234",
      "environment": [
        {
          "name": "OTEL_EXPORTER_OTLP_ENDPOINT",
          "value": "http://localhost:4318"
        }
      ],
      "secrets": [
        { "name": "DATABASE_URL", "valueFrom": "arn:aws:secretsmanager:..." }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/core-product",
          "awslogs-region": "eu-west-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    },
    {
      "name": "otel-collector",
      "image": "otel/opentelemetry-collector-contrib:0.91.0",
      "command": ["--config=/etc/otel-collector-config.yaml"],
      "environment": [
        {
          "name": "ENVIRONMENT",
          "value": "production"
        }
      ],
      "secrets": [
        {
          "name": "DATADOG_API_KEY",
          "valueFrom": "arn:aws:secretsmanager:eu-west-1:123456:secret:finverse/datadog-api-key"
        }
      ],
      "portMappings": [
        { "containerPort": 4317 },  // gRPC OTLP
        { "containerPort": 4318 }   // HTTP OTLP
      ],
      "mountPoints": [
        {
          "sourceVolume": "otel-config",
          "containerPath": "/etc/otel-collector-config.yaml"
        }
      ],
      "essential": false
      // essential: false — if Collector crashes, NestJS keeps running
      // We lose observability but we don't lose the service
    }
  ]
}
```

**Why `essential: false` for the Collector:**

The Collector is infrastructure, not the application. If it crashes (OOM, misconfiguration), the NestJS service should continue serving users — we just temporarily lose observability. Making it `essential: true` would mean a Collector crash kills the whole ECS task and takes down the service. That is the wrong trade-off.

---

## The Full Package List — Everything Installed in Core Product

For completeness and interview readiness, here is every OTEL package installed in Core Product Service:

```json
// package.json — observability dependencies

{
  "dependencies": {
    // OTEL API — the interfaces your code calls
    "@opentelemetry/api": "^1.7.0",

    // OTEL SDK — the implementation
    "@opentelemetry/sdk-node": "^0.45.0",

    // Auto-instrumentation — patches http, prisma, ioredis
    "@opentelemetry/auto-instrumentations-node": "^0.40.0",

    // OTLP exporters — for sending to Collector
    "@opentelemetry/exporter-otlp-http": "^0.45.0",

    // Metrics SDK
    "@opentelemetry/sdk-metrics": "^1.18.0",

    // Trace SDK components
    "@opentelemetry/sdk-trace-base": "^1.18.0",
    "@opentelemetry/sdk-trace-node": "^1.18.0",

    // Resource detection — auto-detects AWS environment
    "@opentelemetry/resources": "^1.18.0",
    "@opentelemetry/semantic-conventions": "^1.18.0",
    "@opentelemetry/resource-detector-aws": "^1.3.0",

    // Logging — Pino
    "pino": "^8.17.0",
    "nestjs-pino": "^3.5.0",

    // Dev only: human-readable logs
    "pino-pretty": "^10.3.0"
  }
}
```

---

## Chapter 5 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 5 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OpenTelemetry = SLF4J for observability                        │
│  API: the interfaces your code calls (no implementation)        │
│  SDK: the implementation for each language                      │
│  OTLP: the wire format — vendor-agnostic protobuf               │
│                                                                 │
│  Java comparison:                                               │
│  Micrometer = metrics API (like OTEL Metrics API)               │
│  micrometer-tracing-bridge-otel = bridge to OTEL SDK            │
│  opentelemetry-exporter-otlp = same in both Java and Node.js    │
│  Node.js uses OTEL API directly (no Micrometer bridge)          │
│                                                                 │
│  OTEL Collector: why FinVerse uses it                           │
│  1. Services don't need Datadog credentials                     │
│  2. Central enrichment (env, region tags)                       │
│  3. Buffering and retry during Datadog outages                  │
│  4. Fan-out to multiple backends if needed                      │
│  Runs as sidecar container — essential: false                   │
│                                                                 │
│  Go service participates via Go OTEL SDK                        │
│  Same OTLP format → same Collector → same Datadog               │
│  ParentBased sampler preserves Core Product's                   │
│  sampling decision across the service boundary                  │
│                                                                 │
│  Three pillars linked by Trace ID in Datadog:                   │
│  Metric spike → click → traces from that window                 │
│  Trace → click → correlated log lines                           │
│  One investigation flow: metric → trace → log → root cause      │
│                                                                 │
│  Complete pipeline:                                             │
│  NestJS span → BatchSpanProcessor → OTLP HTTP →                 │
│  Collector (enrich, batch, filter) → Datadog API →              │
│  APM (linked to metrics + logs by Trace ID)                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

