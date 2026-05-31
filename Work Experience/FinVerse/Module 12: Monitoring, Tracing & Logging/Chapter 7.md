# Monitoring, Distributed Tracing & Logging — Chapter 7: Observability at FinVerse — End-to-End Setup

---

## Why This Chapter Exists

The previous six chapters taught you each pillar of observability individually. This chapter puts everything together into one coherent picture — the complete setup, the complete request lifecycle with every observability signal firing, and the production stories you will tell in interviews.

This is the chapter you return to before an interview. Everything here should feel like something you lived, not something you read.

---

## The Complete NestJS Observability Setup

Let's look at the full file structure and startup sequence. Order matters critically — instrumentation must initialise before anything else.

```
OBSERVABILITY FILE STRUCTURE — CORE PRODUCT SERVICE

src/
├── instrumentation.ts          ← loaded FIRST via --require
├── main.ts                     ← NestJS bootstrap (loaded second)
├── app.module.ts               ← registers LoggerModule, middleware
├── common/
│   ├── context/
│   │   └── request-context.ts  ← AsyncLocalStorage definition
│   ├── middleware/
│   │   └── request-context.middleware.ts  ← sets context per request
│   ├── interceptors/
│   │   └── metrics.interceptor.ts         ← records HTTP metrics
│   ├── logger/
│   │   └── logger.service.ts              ← wrapper around Pino
│   └── metrics/
│       └── metrics.service.ts             ← OTEL metrics wrapper
└── modules/
    └── accounts/
        ├── account.service.ts             ← uses LoggerService
        └── ...
```

### Step 1: instrumentation.ts — Everything Starts Here

```typescript
// src/instrumentation.ts
// Loaded via: node --require ./dist/instrumentation.js dist/main.js
// MUST execute before any other module is imported

import { NodeSDK } from '@opentelemetry/sdk-node'
import { OTLPTraceExporter } from '@opentelemetry/exporter-otlp-http'
import { OTLPMetricExporter } from '@opentelemetry/exporter-otlp-http'
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics'
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base'
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node'
import { Resource } from '@opentelemetry/resources'
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions'
import { TraceIdRatioBasedSampler } from '@opentelemetry/sdk-trace-base'

const collectorEndpoint = process.env.OTEL_EXPORTER_OTLP_ENDPOINT
  ?? 'http://localhost:4318'

const sdk = new NodeSDK({
  // Service identity — appears in every span, metric, and log
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]:
      process.env.SERVICE_NAME ?? 'core-product',
    [SemanticResourceAttributes.SERVICE_VERSION]:
      process.env.APP_VERSION ?? '0.0.0',
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]:
      process.env.NODE_ENV ?? 'development',
  }),

  // TRACES
  spanProcessor: new BatchSpanProcessor(
    new OTLPTraceExporter({
      url: `${collectorEndpoint}/v1/traces`,
    }),
    {
      maxQueueSize: 2048,
      maxExportBatchSize: 512,
      scheduledDelayMillis: 5_000,
      exportTimeoutMillis: 30_000,
    }
  ),

  // SAMPLING
  // Production: 10% base rate, always sample errors
  // Development/staging: 100% (see full traces locally)
  sampler: new TraceIdRatioBasedSampler(
    process.env.NODE_ENV === 'production' ? 0.1 : 1.0
  ),

  // METRICS
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: `${collectorEndpoint}/v1/metrics`,
    }),
    exportIntervalMillis: 60_000,   // export every 60 seconds
    exportTimeoutMillis: 30_000,
  }),

  // AUTO-INSTRUMENTATION
  // Patches: http, https, @prisma/client, ioredis, dns
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-http': {
        enabled: true,
        ignoreIncomingRequestHook: (req) =>
          req.url === '/health' || req.url === '/metrics',
      },
      '@opentelemetry/instrumentation-prisma': {
        enabled: true,
      },
      '@opentelemetry/instrumentation-ioredis': {
        enabled: true,
        dbStatementSerializer: (cmdName, cmdArgs) => {
          // Redact actual values from Redis commands in spans
          // We want: "SET usr:prefs:*" not "SET usr:prefs:usr_123 {json...}"
          return `${cmdName} ${cmdArgs[0] ?? ''}`
        },
      },
      // Disable unused auto-instrumentations (reduce overhead)
      '@opentelemetry/instrumentation-graphql': { enabled: false },
      '@opentelemetry/instrumentation-mongoose': { enabled: false },
      '@opentelemetry/instrumentation-mysql': { enabled: false },
    }),
  ],
})

sdk.start()

console.log('OpenTelemetry SDK initialised')

// Graceful shutdown — flush all buffered telemetry before process exits
// Without this, the last N seconds of data are lost on deployment
process.on('SIGTERM', async () => {
  try {
    await sdk.shutdown()
    console.log('OpenTelemetry SDK shut down successfully')
  } catch (error) {
    console.error('Error shutting down OpenTelemetry SDK', error)
  } finally {
    process.exit(0)
  }
})
```

### Step 2: main.ts — NestJS Bootstrap

```typescript
// src/main.ts
// Note: instrumentation.ts has already run before this file is imported

import { NestFactory } from '@nestjs/core'
import { AppModule } from './app.module'
import { Logger } from 'nestjs-pino'
import { ValidationPipe } from '@nestjs/common'

async function bootstrap() {
  const app = await NestFactory.create(AppModule, {
    // Disable NestJS default logger — Pino takes over
    bufferLogs: true,
  })

  // Use Pino as the NestJS logger
  app.useLogger(app.get(Logger))

  // Global validation pipe — validates all DTOs automatically
  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,       // strip unknown properties
    forbidNonWhitelisted: true,
    transform: true,       // auto-transform primitive types
  }))

  await app.listen(3000)
}

bootstrap()
```

### Step 3: app.module.ts — Register Everything

```typescript
// src/app.module.ts
import { Module, NestModule, MiddlewareConsumer } from '@nestjs/common'
import { LoggerModule } from 'nestjs-pino'
import { RequestContextMiddleware } from './common/middleware/request-context.middleware'
import { MetricsInterceptor } from './common/interceptors/metrics.interceptor'
import { APP_INTERCEPTOR } from '@nestjs/core'

@Module({
  imports: [
    // Pino logger setup — replaces NestJS default logger
    LoggerModule.forRoot({
      pinoHttp: {
        level: process.env.NODE_ENV === 'production' ? 'info' : 'debug',
        base: {
          service: process.env.SERVICE_NAME ?? 'core-product',
          environment: process.env.NODE_ENV,
          version: process.env.APP_VERSION,
        },
        transport: process.env.NODE_ENV !== 'production'
          ? { target: 'pino-pretty', options: { colorize: true } }
          : undefined,
        redact: {
          paths: [
            'req.headers.authorization',
            '*.iban',
            '*.password',
            '*.secret',
            '*.token',
          ],
          censor: '[REDACTED]',
        },
        serializers: {
          req: (req) => ({ method: req.method, url: req.url }),
          res: (res) => ({ statusCode: res.statusCode }),
        },
        autoLogging: {
          ignore: (req) => req.url === '/health',
        },
        customProps: (req) => ({
          correlationId: req.correlationId,
          traceId: req.traceId,
          spanId: req.spanId,
        }),
      },
    }),

    // ... BullModule, PrismaModule, and domain modules
  ],

  providers: [
    // Global interceptor — records HTTP metrics for every request
    {
      provide: APP_INTERCEPTOR,
      useClass: MetricsInterceptor,
    },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    // Request context middleware — runs on every request
    // Sets up AsyncLocalStorage with traceId, correlationId, userId
    consumer
      .apply(RequestContextMiddleware)
      .forRoutes('*')
  }
}
```

---

## The Request Lifecycle With Full Observability

Now let's trace exactly what happens — every observability signal that fires — for a single request: `POST /v1/accounts/:accountId/sync` (manual sync trigger).

```
REQUEST: POST /v1/accounts/acc_456/sync
USER: usr_123, via React Native app

────────────────────────────────────────────────────────────────
STEP 1: Request arrives at NestJS HTTP server
────────────────────────────────────────────────────────────────

OTEL auto-instrumentation (http module patch):
  → Reads incoming headers
  → No traceparent header (request from mobile app, not another service)
  → Generates new Trace ID: 7c4a9b2e1f3d8a0c...
  → Creates root span:
       name: "POST /v1/accounts/:accountId/sync"
       kind: SERVER
       startTime: 1705324991421
  → Makes this span the active span for this request

Pino + nestjs-pino (pinoHttp):
  → Logs incoming request automatically:
    {
      "level": "info",
      "msg": "incoming request",
      "req": { "method": "POST", "url": "/v1/accounts/acc_456/sync" }
    }

────────────────────────────────────────────────────────────────
STEP 2: RequestContextMiddleware fires
────────────────────────────────────────────────────────────────

RequestContextMiddleware:
  → Reads active OTEL span → extracts traceId, spanId
  → Calls requestContext.run({ traceId, spanId, correlationId })
  → Sets X-Correlation-Id response header

From this point: ALL async operations on this execution chain
have access to the context via requestContext.getStore()
Including: service methods, repository calls, BullMQ enqueue

────────────────────────────────────────────────────────────────
STEP 3: MetricsInterceptor fires (before controller)
────────────────────────────────────────────────────────────────

MetricsInterceptor:
  → Records startTime = Date.now()
  → Waits for controller to complete (via RxJS tap)

────────────────────────────────────────────────────────────────
STEP 4: JWT Guard validates token
────────────────────────────────────────────────────────────────

OTEL auto-instrumentation:
  → Creates child span: "JwtAuthGuard"
  → Duration: 3ms (token validation is fast)
  → Sets userId attribute: "usr_123"

RequestContextMiddleware update:
  → After guard confirms userId, store is updated:
    requestContext.getStore().userId = 'usr_123'

────────────────────────────────────────────────────────────────
STEP 5: AccountsController.triggerSync() executes
────────────────────────────────────────────────────────────────

No manual span needed — auto-instrumentation covers the
HTTP handler span. Controller calls AccountService.

────────────────────────────────────────────────────────────────
STEP 6: AccountService.triggerManualSync() executes
────────────────────────────────────────────────────────────────

LoggerService.info() call #1:
  {
    "level": "info",
    "traceId": "7c4a9b2e1f3d8a0c",
    "correlationId": "7c4a9b2e1f3d8a0c",
    "userId": "usr_123",
    "accountId": "acc_456",
    "msg": "Manual sync triggered"
  }

OTEL auto-instrumentation (Prisma):
  → Creates child span: "prisma:findFirst bank_accounts"
  → Runs query: SELECT * FROM bank_accounts WHERE id=$1 AND userId=$2
  → Duration: 7ms
  → Attributes: db.operation=findFirst, db.model=BankAccount

OTEL auto-instrumentation (Prisma):
  → Creates child span: "prisma:update bank_accounts"
  → Runs query: UPDATE bank_accounts SET syncStatus='SYNCING'...
  → Duration: 5ms

────────────────────────────────────────────────────────────────
STEP 7: BullMQ job enqueued
────────────────────────────────────────────────────────────────

OTEL auto-instrumentation (ioredis):
  → BullMQ calls Redis internally (LPUSH, HSET, etc.)
  → Creates child spans for each Redis command
  → Duration: 2ms total

LoggerService.info() call #2:
  {
    "level": "info",
    "traceId": "7c4a9b2e1f3d8a0c",
    "correlationId": "7c4a9b2e1f3d8a0c",
    "userId": "usr_123",
    "accountId": "acc_456",
    "jobId": "manual-sync-acc_456",
    "msg": "Sync job enqueued"
  }

────────────────────────────────────────────────────────────────
STEP 8: HTTP response returned
────────────────────────────────────────────────────────────────

Controller returns: { message: 'Sync started', syncStatus: 'SYNCING' }
HTTP status: 202 Accepted

OTEL auto-instrumentation (http):
  → Root span ends
  → Duration recorded: 42ms
  → Status: OK, http.status_code: 202
  → Span added to BatchSpanProcessor queue

MetricsInterceptor (post-request):
  → duration = Date.now() - startTime = 42ms
  → metricsService.recordHttpRequest('POST', '/v1/accounts/:accountId/sync', 202, 42)
  → Increments: finverse.http.requests.total{method:POST, endpoint:..., status:202}
  → Records: finverse.http.request.duration.ms{...} = 42ms histogram

Pino + nestjs-pino (pinoHttp):
  → Logs response automatically:
    {
      "level": "info",
      "traceId": "7c4a9b2e1f3d8a0c",
      "correlationId": "7c4a9b2e1f3d8a0c",
      "userId": "usr_123",
      "req": { "method": "POST", "url": "/v1/accounts/acc_456/sync" },
      "res": { "statusCode": 202 },
      "responseTime": 42,
      "msg": "request completed"
    }

────────────────────────────────────────────────────────────────
STEP 9 (async, separate container): BullMQ Worker processes job
────────────────────────────────────────────────────────────────

requestContext.run({ traceId: job.data._traceContext.traceId, ... }):
  → Worker sets up its own async context

Manual span created:
  → tracer.startActiveSpan('bullmq.process.PERIODIC_SYNC', ...)
  → Links to original HTTP trace via job.data._traceContext

LoggerService.info():
  {
    "level": "info",
    "traceId": "7c4a9b2e1f3d8a0c",   ← same trace ID propagated
    "correlationId": "7c4a9b2e1f3d8a0c",
    "userId": "usr_123",
    "jobId": "manual-sync-acc_456",
    "msg": "Sync job started"
  }

GoCardless HTTP call:
  OTEL auto-instrumentation (http module):
    → Creates child span: "GET bankaccountdata.gocardless.com"
    → Duration: 2,340ms (GoCardless latency)
    → Attributes: http.url, http.status_code: 200

Prisma createMany:
  OTEL auto-instrumentation (Prisma):
    → Creates child span: "prisma:createMany transactions"
    → Duration: 890ms (bulk insert of 183 transactions)

MetricsService.recordSyncCompleted():
  → Increments: finverse.transaction_sync.jobs.completed
  → Records: finverse.transaction_sync.duration.ms = 4,820ms histogram

LoggerService.info():
  {
    "level": "info",
    "traceId": "7c4a9b2e1f3d8a0c",
    "correlationId": "7c4a9b2e1f3d8a0c",
    "userId": "usr_123",
    "jobId": "manual-sync-acc_456",
    "transactionsFetched": 183,
    "transactionsInserted": 167,
    "transactionsDuplicate": 16,
    "durationMs": 4820,
    "msg": "Sync job completed successfully"
  }
```

In Datadog, this entire flow is visible as:

```
DATADOG VIEW — COMPLETE PICTURE

APM Trace (TraceID: 7c4a9b2e1f3d8a0c):
  [POST /v1/accounts/:id/sync  42ms]
    [JwtAuthGuard  3ms]
    [prisma:findFirst bank_accounts  7ms]
    [prisma:update bank_accounts  5ms]
    [ioredis LPUSH  2ms]

  Linked Trace (async job):
  [bullmq.process.PERIODIC_SYNC  4,820ms]
    [GET bankaccountdata.gocardless.com  2,340ms]
    [prisma:createMany transactions  890ms]

Correlated Logs (filtered by traceId: 7c4a9b2e1f3d8a0c):
  14:23:11.421  info  incoming request
  14:23:11.424  info  Manual sync triggered
  14:23:11.436  info  Sync job enqueued
  14:23:11.463  info  request completed (42ms, 202)
  14:23:15.201  info  Sync job started (worker container)
  14:23:20.021  info  Sync job completed (4,820ms)

Metric data point recorded:
  finverse.http.request.duration.ms{endpoint:/v1/accounts/:id/sync} = 42ms
  finverse.transaction_sync.duration.ms{job_type:PERIODIC_SYNC} = 4820ms
```

---

## The Interview Story: Identifying and Fixing a Performance Problem

This is the STAR-format story you will tell when asked "tell me about a performance improvement you made" or "how did you use observability in your work." This is a real, defensible story built from everything in this module.

---

### Story 1 — The Sync Timeout Incident (Chapter 2's story, now complete)

**Situation:**

Three months into my contract, I started seeing stalled job alerts firing sporadically in Datadog — specifically the "No Active Workers" monitor was not firing, but the "Failed Jobs Accumulating" monitor was, with failed jobs slowly growing in the `transaction-sync` queue. The Slack message linked directly to the Datadog dashboard and the Bull Board runbook.

**Task:**

Investigate why jobs were failing and find the root cause. The stalled job count was growing by about 5-6 jobs per hour — not catastrophic, but systematic. Users with multiple bank accounts were not getting their initial sync completed after connecting their banks.

**Action:**

I opened Datadog APM and filtered for failed `INITIAL_SYNC` spans in the last 24 hours. Immediately I saw a pattern — every failed job had `bullmq.attempts_made: 2` and `error.message: "Job stalled"`. Not a GoCardless error. Not a database error. A stalled job error.

I looked at the job duration histogram for `INITIAL_SYNC`, filtered to the failed jobs. The p99 was 31.2 seconds. The lock TTL for BullMQ was 30 seconds — I had not configured `lockDuration` explicitly and was running on the default.

I then looked at the correlated logs for one of the failed jobs — using the `traceId` from the APM span to filter logs in Datadog Log Explorer. The logs showed:

```
14:23:11  INFO  Sync job started  { accountCount: 4 }
14:23:15  INFO  Account 1 synced  { transactionsFetched: 892 }
14:23:19  INFO  Account 2 synced  { transactionsFetched: 431 }
14:23:24  INFO  Account 3 synced  { transactionsFetched: 1203 }
[no further logs — worker crashed before account 4]
```

The worker was processing accounts sequentially. A user with 4 accounts could have a GoCardless call plus bulk insert for each one. With large transaction histories, the total time exceeded 30 seconds.

I made three changes:

First, I extended the lock duration and set explicit renewal timing in the BullMQ worker config — `lockDuration: 60_000` and `lockRenewTime: 30_000`. This gave jobs up to 60 seconds before stalling.

Second, I added `job.updateProgress()` calls between each account — not just for UI feedback but because BullMQ renews the lock on progress updates, providing an additional heartbeat mechanism.

Third, I added a per-account timeout using `Promise.race()` so a single slow GoCardless call could not silently block the entire job.

```typescript
// Before:
for (const accountId of accountIds) {
  const transactions =
    await this.goCardlessService.fetchAllTransactions(accountId)
  await this.transactionService.bulkInsert(userId, accountId, transactions)
}

// After:
for (let i = 0; i < accountIds.length; i++) {
  await job.updateProgress({ current: i + 1, total: accountIds.length })

  const transactions = await Promise.race([
    this.goCardlessService.fetchAllTransactions(accountIds[i]),
    new Promise<never>((_, reject) =>
      setTimeout(() => reject(new Error('GoCardless timeout after 25s')), 25_000)
    )
  ])

  await this.transactionService.bulkInsert(userId, accountIds[i], transactions)
}
```

I deployed to staging and monitored the `INITIAL_SYNC` duration histogram for 24 hours.

**Result:**

The stalled job error rate dropped from approximately 12% of multi-account syncs to under 0.3% — confirmed by the Datadog monitor for "Failed Jobs Accumulating" which stopped firing within the first hour of deployment.

I then used Bull Board's bulk retry feature to replay the ~400 jobs that had failed before the fix. All completed successfully.

The p99 for `INITIAL_SYNC` changed:
- Before: 41.0 seconds (causing stalls)
- After: 18.7 seconds (well within the 60-second lock)

And the p95 for the subset of jobs that had previously been stalling:
- Before: 31.2 seconds
- After: 14.8 seconds

---

### Story 2 — The GoCardless Rate Limit Storm (Chapter 8's story, now with observability detail)

**Situation:**

In month six, a marketing campaign brought 8,000 new users over 48 hours. Each connected their bank account, triggering an `INITIAL_SYNC` job. The transaction-sync queue depth gauge in Datadog spiked to 8,000 — triggering the queue depth monitor. ECS auto-scaling launched 8 worker containers. Within two minutes, the GoCardless error rate monitor fired at 23% — far above the 5% threshold.

**Task:**

Stop the retry storm that was hammering GoCardless and restore normal sync operation without losing any of the 8,000 pending jobs.

**Action:**

My first action was to check the runbook linked in the alert. The runbook said: if `error_type = gocardless_rate_limit` → pause the queue. I opened Bull Board and paused the `transaction-sync` queue immediately. The GoCardless error rate metric dropped to zero within 30 seconds — the pause was effective.

I then looked at the Datadog APM traces for the 429 errors. Every failed span had the same pattern: `http.status_code: 429`, started at almost exactly the same time, from 8 different worker containers simultaneously. The retry backoff was exponential but had no jitter — all 80 concurrent jobs (8 containers × concurrency 10) were retrying at exactly the same intervals, creating synchronized bursts.

I deployed two changes:

First, I added the `limiter` configuration to the BullMQ worker:

```typescript
@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,
    duration: 10_000,
  },
})
```

Second, I replaced the exponential backoff with exponential plus jitter in the queue's `defaultJobOptions`:

```typescript
backoff: {
  type: 'custom',
  delay: (attemptsMade: number) => {
    const base = 5000
    const max = 60_000
    const exp = base * Math.pow(2, attemptsMade)
    const capped = Math.min(max, exp)
    return Math.floor(Math.random() * capped)
  }
}
```

I also updated the ECS Auto Scaling maximum container count from 10 to 3 — with the rate limiter, 3 containers gave FinVerse 90 jobs per 10 seconds maximum, comfortably within GoCardless's limits.

After deploying, I resumed the queue from Bull Board. I watched the Datadog dashboard live — GoCardless error rate stayed below 0.5% as the 8,000-job backlog drained over the next 6 hours.

**Result:**

The GoCardless rate limit monitor did not fire again during the campaign or the follow-up campaign two months later. The queue depth gauge showed a clean drain curve — no spikes, no oscillation. All 8,000 pending jobs completed successfully, confirmed by comparing the queue depth gauge reaching zero against the sync completed counter showing 8,000 increments.

The jitter improvement was also visible in the GoCardless API call distribution in Datadog — before, calls arrived in synchronized bursts every N seconds. After, they were uniformly distributed across time.

---

### Story 3 — The Notification Race Condition (Chapter 8's story, now with observability detail)

**Situation:**

In month eight, Lucas noticed a pattern across support tickets — users were receiving budget alert push notifications before the transactions that triggered the alert appeared in their transaction list. This was intermittent and hard to reproduce.

**Task:**

Find the root cause of a timing inconsistency between notification delivery and transaction visibility.

**Action:**

I started by finding correlated logs for a user who had reported the issue. Using their `userId` as a Datadog Log Explorer filter, I found the log lines from their sync:

```
14:19:23.421  INFO  Sync completed: 18 transactions inserted
14:19:23.435  INFO  Budget threshold exceeded, publishing event
14:19:23.437  INFO  RabbitMQ event published: budget.threshold.exceeded
```

I then looked at the Notification Service logs for the same `correlationId`:

```
14:19:23.441  INFO  Received budget.threshold.exceeded event
14:19:23.447  INFO  Push notification sent to usr_456
```

The push notification was sent 26ms after the sync completed. The user opened the app within 2 seconds. But the user's GET /v1/transactions request hit the PostgreSQL read replica — and Datadog APM showed the replica was 3-4 seconds behind the primary at that moment.

The transactions were in the primary. The read replica had not caught up. The user saw the old state.

I found this by looking at the APM trace for the GET /v1/transactions request immediately after the notification. The Prisma span showed `db.host: replica-endpoint` — Core Product was reading from the replica. The replica lag was visible in the RDS CloudWatch metrics linked from the Datadog infrastructure view.

The root cause was clear: direct RabbitMQ publish was so fast (26ms after commit) that the notification arrived before the read replica had replicated the new rows.

The fix was the Outbox pattern — writing the event to `outbox_events` in the same transaction as the business data, and having the outbox-publisher worker poll every 5 seconds before publishing to RabbitMQ. By the time the event reached RabbitMQ (at least 5 seconds after commit), the read replica had fully caught up.

**Result:**

After deploying the Outbox pattern, I monitored for the specific log pattern — `Budget threshold exceeded` within 500ms of `Sync completed` — which was the signal of a race condition. The pattern disappeared from the logs entirely.

More concretely: the support ticket category "notification before transaction" dropped to zero in the two weeks following the fix. I confirmed this by filtering Datadog logs for cases where `budget.threshold.exceeded` was published within 1 second of `sync completed` — from approximately 5 occurrences per day to zero.

---

## The Complete Observability Answer: "How Did You Measure That?"

Here is the template you use for any performance or reliability claim in an interview:

```
THE MEASUREMENT TEMPLATE

CLAIM: "I improved [X] by [Y%]"

COMPLETE ANSWER STRUCTURE:

  "I identified the problem using [specific Datadog view —
  APM trace / Log Explorer / metric dashboard].

  The before state was [specific metric name] at
  [specific percentile/value] measured over [specific time window].

  I found the root cause by [specific investigation step —
  looking at correlated logs / examining span attributes /
  comparing metric tags].

  The fix was [specific code or config change].

  I measured the after state with the same metric over the same
  window after deployment to staging for 24 hours.
  The result was [specific percentile/value].

  In production, I confirmed the improvement by watching
  [specific metric] in Datadog over the first [N hours/days]
  of deployment."
```

Applying this to the sync stall story:

```
COMPLETE ANSWER — SYNC STALL IMPROVEMENT

"I identified the problem using Datadog APM — filtering for
failed INITIAL_SYNC spans and seeing 'job stalled' as the
error type across all failures.

The before state was: stalled job rate of 12% for users with
3+ bank accounts, and job duration p99 of 41 seconds,
measured over a 7-day window using the
finverse.transaction_sync.duration.ms histogram.

I found the root cause by correlating the APM spans to Pino logs
using the traceId — the logs showed sequential account processing
where total time exceeded the 30-second BullMQ lock TTL.

The fix was: extending lockDuration to 60 seconds, adding
job.updateProgress() calls between accounts to trigger lock
renewal, and adding per-account timeouts via Promise.race().

I measured the after state with the same histogram and the
stalled job counter over 24 hours in staging.
The p99 dropped from 41 seconds to 18.7 seconds.
Stalled job rate dropped from 12% to under 0.3%.

In production, I confirmed the improvement by watching the
Failed Jobs Accumulating monitor — it stopped firing within
the first hour of deployment, and the 400 previously failed
jobs were successfully replayed via Bull Board."
```

---

## Chapter 7 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 7 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  STARTUP ORDER (non-negotiable):                                │
│  1. instrumentation.ts (--require, before anything)             │
│  2. main.ts (NestJS bootstrap)                                  │
│  3. app.module.ts (registers LoggerModule, middleware)          │
│  Without this order: OTEL cannot patch libraries                │
│                                                                 │
│  COMPLETE REQUEST LIFECYCLE (every signal that fires):          │
│  Request arrives → OTEL creates root span                       │
│  → Pino logs incoming request (auto)                            │
│  → RequestContextMiddleware sets AsyncLocalStorage context      │
│  → MetricsInterceptor starts timer                              │
│  → JWT guard (child span)                                       │
│  → Service methods (Prisma spans auto, manual spans for logic)  │
│  → BullMQ enqueue (ioredis spans auto)                          │
│  → Response → MetricsInterceptor records duration               │
│  → Pino logs response with traceId (auto)                       │
│  → Root span ends → exported to Collector → Datadog APM         │
│                                                                 │
│  THREE STAR STORIES:                                            │
│  1. Sync stall: lock TTL → extend lockDuration + progress       │
│     p99: 41s → 18.7s, stall rate: 12% → 0.3%                    │
│  2. Rate limit storm: no jitter + no limiter → jitter + limiter │
│     GoCardless error rate: 23% → 0.5%                           │
│  3. Notification race: direct RabbitMQ → Outbox pattern         │
│     Race condition occurrences: 5/day → 0                       │
│                                                                 │
│  THE MEASUREMENT TEMPLATE:                                      │
│  Specific metric name + specific percentile +                   │
│  specific time window + specific before number +                │
│  specific after number + specific tool used                     │
│  Never: "I made it faster" without numbers                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step 12 Complete

All seven chapters are written and ready to be saved:

```
Chapter 1 — The Problem Space
Chapter 2 — Metrics: Measuring What's Happening Right Now
Chapter 3 — Distributed Tracing: Following a Request Across Services
Chapter 4 — Logging: The Detail Layer
Chapter 5 — OpenTelemetry: The Glue Layer
Chapter 6 — Alerting & Datadog: Turning Signals Into Action
Chapter 7 — Observability at FinVerse: End-to-End Setup
```
