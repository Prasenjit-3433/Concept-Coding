Writing Chapter 4 now.

---

# Monitoring, Distributed Tracing & Logging — Chapter 4: Logging

---

## Why This Chapter Exists

Metrics tell you something is wrong. Traces tell you where in the system the problem is. Logs tell you *what exactly happened* — the detail layer. When you click on a failing span in Datadog APM and want to understand why it failed, you look at the logs emitted during that span.

But production logging is not just `console.log`. Production logging requires structured output, correlation with traces, sensitive data protection, performance awareness, and the ability to search millions of log lines efficiently.

This chapter maps everything you know from Java (SLF4J, Logback, LogstashEncoder, MDC) to its Node.js equivalent — and shows exactly how FinVerse's Core Product Service is configured.

---

## The Java Mental Model You Already Have

Your Spring Boot notes cover logging thoroughly. Let's extract the key concepts and map them directly:

```
┌──────────────────────────────────────────────────────────────────────┐
│              JAVA vs NODE.JS LOGGING — COMPLETE MAP                  │
├────────────────────────────────┬─────────────────────────────────────┤
│  JAVA                          │  NODE.JS                            │
├────────────────────────────────┼─────────────────────────────────────┤
│  SLF4J                         │  No standard facade exists          │
│  (logging facade — interface   │  Teams pick a library directly      │
│   only, no implementation)     │  Most common: Pino, Winston, Bunyan │
├────────────────────────────────┼─────────────────────────────────────┤
│  Logback                       │  Pino                               │
│  (SLF4J implementation)        │  (the production standard)          │
├────────────────────────────────┼─────────────────────────────────────┤
│  LogstashEncoder               │  Pino default output                │
│  (produces structured JSON)    │  (JSON by default — no plugin)      │
├────────────────────────────────┼─────────────────────────────────────┤
│  MDC (Mapped Diagnostic        │  AsyncLocalStorage                  │
│  Context) — ThreadLocal map    │  (Node.js equivalent of             │
│  for per-request context       │  ThreadLocal — async-aware)         │
├────────────────────────────────┼─────────────────────────────────────┤
│  MDC.put("correlationId", id)  │  store.set('correlationId', id)     │
│  MDC.clear() in finally        │  (cleared automatically when        │
│                                │  async context ends)                │
├────────────────────────────────┼─────────────────────────────────────┤
│  Logger log =                  │  const logger =                     │
│  LoggerFactory.getLogger(      │    pino({ name: 'AccountsService' })│
│    AccountsService.class)      │                                     │
├────────────────────────────────┼─────────────────────────────────────┤
│  log.info("message {}", var)   │  logger.info({ key: value },        │
│  log.error("msg", exception)   │    'message')                       │
├────────────────────────────────┼─────────────────────────────────────┤
│  Log levels:                   │  Log levels:                        │
│  ERROR, WARN, INFO,            │  fatal, error, warn, info,          │
│  DEBUG, TRACE                  │  debug, trace                       │
├────────────────────────────────┼─────────────────────────────────────┤
│  Rolling file appender         │  stdout only (container logging)    │
│  (writes to disk)              │  Docker/ECS collects stdout         │
│                                │  → Datadog Log Agent                │
├────────────────────────────────┼─────────────────────────────────────┤
│  logback-spring.xml            │  Pino config object in code         │
│  (XML configuration)           │  (no separate config file)          │
└────────────────────────────────┴─────────────────────────────────────┘
```

The most important insight: **in containers, you never write logs to files.** Your Spring Boot notes cover file appenders and rolling file appenders — these make sense for traditional server deployments where logs live on disk. In containers (ECS Fargate), the container filesystem is ephemeral and shared log directories do not exist in the same way. The production pattern in containers is: write all logs to **stdout**, let the container runtime collect them, and send them to a log aggregation service (Datadog in FinVerse's case).

---

## Why Pino and Not Winston

The Node.js logging landscape has three main players. Understanding the choice matters for interviews.

```
┌──────────────────────────────────────────────────────────────────┐
│              NODE.JS LOGGING LIBRARIES COMPARISON                │
├────────────────┬────────────────────┬────────────────────────────┤
│  Library       │  Performance       │  Production Usage          │
├────────────────┼────────────────────┼────────────────────────────┤
│  Pino          │  Fastest           │  Industry standard for     │
│                │  ~5× faster than   │  high-throughput services  │
│                │  Winston           │  NestJS has official       │
│                │  JSON by default   │  Pino integration          │
│                │  Async transport   │                            │
├────────────────┼────────────────────┼────────────────────────────┤
│  Winston       │  Moderate          │  Very common in older      │
│                │  Synchronous by    │  codebases                 │
│                │  default           │  Highly configurable       │
│                │  JSON via plugin   │  More familiar to devs     │
│                │  (winston-json)    │  from other backgrounds    │
├────────────────┼────────────────────┼────────────────────────────┤
│  Bunyan        │  Moderate          │  Declining usage           │
│                │  JSON by default   │  Older projects only       │
└────────────────┴────────────────────┴────────────────────────────┘
```

**Why Pino specifically:**

Pino's performance advantage comes from its architecture. When you call `logger.info(...)`, Pino does the minimum work on the hot path — it serialises the log object to JSON as fast as possible and writes it to a stream. The expensive work (formatting for human readability, transporting to external systems) happens in a separate worker thread via **pino-transport**, not on the main event loop thread.

```
PINO ARCHITECTURE

Main Thread (Event Loop)
  logger.info({ userId: 'usr_123' }, 'sync started')
         │
         │  Pino serialises to JSON string (fast, synchronous)
         │  '{"level":30,"time":1705324991421,"userId":"usr_123","msg":"sync started"}'
         │
         │  Writes to pino stream (non-blocking)
         ▼
  Continues processing immediately

pino-transport Worker Thread (separate)
  Reads from stream
  Formats / transforms if needed
  Sends to Datadog Log Agent
  (does NOT block the event loop)

Result: logging overhead on the main thread is minimal
        expensive transport work is offloaded
```

**Java parallel:** this is similar to the Async Appender in Logback — your Spring Boot notes cover this exact pattern. The main thread hands off to an async queue, a background thread does the actual write. Pino's transport is the same idea, but at the library level rather than the appender level.

---

## Structured Logging: Why Plain Text Does Not Scale

Your Spring Boot notes explain this for Java — LogstashEncoder produces JSON instead of `%d %-5level %logger - %msg%n`. The same reasoning applies in Node.js.

```
PLAIN TEXT LOG (what you write in development):

2024-01-15 14:23:11 INFO  [TransactionSyncWorker] job initial-sync-usr_123 
started syncing account acc_456 for user usr_123

Problems in production:
  ✗ How do you search for all logs with userId = usr_123?
    → string search, fragile, depends on exact format
  ✗ How do you filter logs by job type?
    → impossible without regex that matches the specific format
  ✗ How do you calculate error rate from logs?
    → parse each line, error-prone
  ✗ Different developers write different formats
    → log aggregation tool cannot understand the structure


STRUCTURED JSON LOG (what Pino produces):

{
  "level": "info",
  "time": 1705324991421,
  "service": "core-product",
  "component": "TransactionSyncWorker",
  "traceId": "9e7d21299f4ea8a1",
  "spanId": "bbb222aabb112233",
  "correlationId": "9e7d21299f4ea8a1",
  "jobId": "initial-sync-usr_123",
  "jobType": "INITIAL_SYNC",
  "userId": "usr_123",
  "accountId": "acc_456",
  "msg": "sync started"
}

Benefits in production:
  ✓ Filter by userId: click, done — Datadog indexes every field
  ✓ Filter by jobType = INITIAL_SYNC: one dropdown
  ✓ Count ERROR logs per minute: built-in metric from logs
  ✓ Click traceId → see the full distributed trace
  ✓ Consistent structure regardless of who wrote the log line
```

The JSON fields that Datadog automatically indexes and makes searchable are called **log facets**. Every field in your structured log becomes a facet. `userId`, `jobType`, `traceId`, `spanId` — all searchable, filterable, and graphable without any additional configuration.

---

## Setting Up Pino in NestJS

NestJS has a first-class integration with Pino via the `nestjs-pino` package. This replaces NestJS's default logger while keeping all the NestJS-specific features (module-scoped logging, `@Logger()` injection).

```typescript
// src/instrumentation.ts — already exists from Chapter 2/3
// No changes needed here for logging

// src/app.module.ts
import { Module } from '@nestjs/common'
import { LoggerModule } from 'nestjs-pino'

@Module({
  imports: [
    LoggerModule.forRoot({
      pinoHttp: {
        // Service identification — appears in every log line
        base: {
          service: 'core-product',
          environment: process.env.NODE_ENV,
          version: process.env.APP_VERSION,
        },

        // Log level by environment
        level: process.env.NODE_ENV === 'production' ? 'info' : 'debug',

        // JSON in production, human-readable in development
        transport: process.env.NODE_ENV !== 'production'
          ? {
              target: 'pino-pretty',    // human-readable for local dev
              options: {
                colorize: true,
                translateTime: 'HH:MM:ss.l',
                ignore: 'pid,hostname',
              }
            }
          : undefined,   // production: raw JSON to stdout

        // Custom serialisers — control what appears in logs
        serializers: {
          req: (req) => ({
            method: req.method,
            url: req.url,
            // Never log request headers — may contain JWT tokens
            // Never log request body — may contain sensitive data
          }),
          res: (res) => ({
            statusCode: res.statusCode,
          }),
          err: (err) => ({
            type: err.type,
            message: err.message,
            // Include stack trace in non-production environments only
            stack: process.env.NODE_ENV !== 'production' ? err.stack : undefined,
          }),
        },

        // Redact sensitive fields — they appear as [Redacted]
        // This is the Pino equivalent of MaskingJsonGeneratorDecorator
        // in your LogstashEncoder Java notes
        redact: {
          paths: [
            'req.headers.authorization',  // JWT token
            '*.iban',                      // bank account IBAN
            '*.password',                  // passwords
            '*.secret',                    // API secrets
            '*.token',                     // access tokens
          ],
          censor: '[REDACTED]',
        },

        // Auto-log every HTTP request and response
        // Same as Spring Boot's request logging filter
        autoLogging: {
          ignore: (req) => req.url === '/health',  // skip health checks
        },

        // Custom fields added to EVERY log line
        // This is where trace context is injected
        customProps: (req, res) => ({
          // These are populated by the AsyncLocalStorage middleware
          // (covered in the next section)
          correlationId: req.correlationId,
          traceId: req.traceId,
          spanId: req.spanId,
        }),
      },
    }),
  ],
})
export class AppModule {}
```

**What every HTTP request log looks like automatically:**

```json
{
  "level": "info",
  "time": 1705324991421,
  "service": "core-product",
  "environment": "production",
  "version": "1.4.2",
  "correlationId": "9e7d21299f4ea8a1cb3f4d2e8b1a0c5f",
  "traceId": "9e7d21299f4ea8a1cb3f4d2e8b1a0c5f",
  "spanId": "bbb222aabb112233",
  "req": {
    "method": "POST",
    "url": "/v1/accounts/connect"
  },
  "res": {
    "statusCode": 202
  },
  "responseTime": 1847,
  "msg": "request completed"
}
```

---

## AsyncLocalStorage: The Node.js Equivalent of MDC

This is the most important concept in this chapter — and the one that confuses Node.js developers the most.

### The Java Mental Model

In Java, `MDC` (Mapped Diagnostic Context) works because Java is multi-threaded. Each HTTP request runs on its own thread. `MDC.put("userId", "usr_123")` stores the value in a `ThreadLocal` map — a map that is specific to the current thread. Every log statement on that thread automatically includes `userId: usr_123`. When the request finishes, `MDC.clear()` empties the map. The next request on that thread starts clean.

```
JAVA — MDC via ThreadLocal

Thread 1 (handling Request A):
  MDC.put("userId", "usr_123")
  → all log statements on Thread 1 include userId: usr_123
  → Thread 2 is unaffected

Thread 2 (handling Request B):
  MDC.put("userId", "usr_456")
  → all log statements on Thread 2 include userId: usr_456
  → Thread 1 is unaffected

ThreadLocal: each thread has its own private copy of the map
```

### The Node.js Problem: One Thread, Many Requests

Node.js has one thread. All requests run on the same thread. If you use a regular JavaScript variable or object to store per-request context:

```typescript
// WRONG — a global variable in Node.js

let currentUserId: string  // shared across ALL requests

// Request A arrives:
currentUserId = 'usr_123'

// Event loop switches to Request B:
currentUserId = 'usr_456'

// Event loop comes back to Request A:
// currentUserId is now 'usr_456' — WRONG
// Request A's logs will show usr_456 instead of usr_123
```

This is a race condition. Multiple async operations interleave on the same thread. A simple variable cannot track which value belongs to which request.

### The Solution: AsyncLocalStorage

`AsyncLocalStorage` is Node.js's built-in solution, added in Node.js 12.17.0. It is the async-aware equivalent of `ThreadLocal`.

Instead of being tied to a thread (which doesn't work in Node.js), `AsyncLocalStorage` ties a storage context to an **async execution chain**. When you `store.run(data, callback)`, every `await` inside that callback — and every function called from it, no matter how deep — has access to the same `data`. When the execution chain ends, the context disappears.

```
ASYNCLOCALSTORAGE — HOW IT WORKS

AsyncLocalStorage creates an "execution context" rather than
a "thread context"

Request A arrives:
  store.run({ userId: 'usr_123', traceId: 'aaa...' }, async () => {
    await doSomething()     // store.getStore() returns { userId: 'usr_123' }
    await doSomethingElse() // still returns { userId: 'usr_123' }
  })

While Request A is awaiting doSomething(),
Request B starts:
  store.run({ userId: 'usr_456', traceId: 'bbb...' }, async () => {
    await doSomething()     // store.getStore() returns { userId: 'usr_456' }
  })

The contexts are ISOLATED even though they run on the same thread.
Node.js tracks which async chain each operation belongs to.
Each chain has its own copy of the store.
```

This is the fundamental mechanism. Now let's see how FinVerse uses it.

---

## Implementing Correlation ID with AsyncLocalStorage

The Correlation ID is a unique identifier assigned to each request. It appears in every log line for that request. When a user reports a problem and gives you the Correlation ID from the error response, you search Datadog for that ID and see every log line from every service for that request.

At FinVerse, the Correlation ID is the same as the Trace ID from Chapter 3. One ID connects the distributed trace and all the logs.

```typescript
// src/common/context/request-context.ts
// The AsyncLocalStorage store definition

import { AsyncLocalStorage } from 'async_hooks'

export interface RequestContext {
  correlationId: string
  traceId: string
  spanId: string
  userId?: string
}

// One global instance — the store lives for the lifetime of the process
// But each request gets its own isolated copy of the data inside
export const requestContext = new AsyncLocalStorage<RequestContext>()
```

```typescript
// src/common/middleware/request-context.middleware.ts
// NestJS middleware — runs on every incoming request

import { Injectable, NestMiddleware } from '@nestjs/common'
import { Request, Response, NextFunction } from 'express'
import { trace } from '@opentelemetry/api'
import { requestContext } from '../context/request-context'

@Injectable()
export class RequestContextMiddleware implements NestMiddleware {

  use(req: Request, res: Response, next: NextFunction): void {

    // Get trace context from the active OTEL span
    // OTEL auto-instrumentation already created a span for this request
    // We just read the IDs from it
    const activeSpan = trace.getActiveSpan()
    const spanContext = activeSpan?.spanContext()

    const traceId = spanContext?.traceId ?? this.generateFallbackId()
    const spanId = spanContext?.spanId ?? this.generateFallbackId()

    // Use traceparent from incoming header as correlationId
    // If this request came from another service, it already has a traceId
    // If it came from the mobile app, we generate one above
    const correlationId = traceId

    // Store in request object for Pino's customProps
    req.correlationId = correlationId
    req.traceId = traceId
    req.spanId = spanId

    // Add correlation ID to response header
    // Mobile app can read this and include it in bug reports
    res.setHeader('X-Correlation-Id', correlationId)

    // Run the rest of the request inside the AsyncLocalStorage context
    // Everything downstream — services, repositories, BullMQ producers —
    // can call requestContext.getStore() and get this same object
    requestContext.run(
      { correlationId, traceId, spanId },
      () => next()
    )
  }

  private generateFallbackId(): string {
    // If OTEL tracing is disabled (e.g. local dev without collector),
    // generate a UUID as fallback
    return crypto.randomUUID().replace(/-/g, '')
  }
}
```

```typescript
// src/app.module.ts — register the middleware
import { Module, NestModule, MiddlewareConsumer } from '@nestjs/common'
import { RequestContextMiddleware } from './common/middleware/request-context.middleware'

@Module({ ... })
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer
      .apply(RequestContextMiddleware)
      .forRoutes('*')   // apply to every route
  }
}
```

---

## The Logger Service — Using Context in Every Log Line

Now that the context exists in AsyncLocalStorage, we need to inject it into every log line. The `nestjs-pino` integration handles this automatically via `customProps` — but for manual log calls inside services, we create a wrapper:

```typescript
// src/common/logger/logger.service.ts
import { Injectable } from '@nestjs/common'
import { PinoLogger } from 'nestjs-pino'
import { requestContext } from '../context/request-context'

@Injectable()
export class LoggerService {
  constructor(private readonly logger: PinoLogger) {}

  private getContextFields(): Record<string, string | undefined> {
    // Read from AsyncLocalStorage — returns undefined if no context
    // (e.g. during application startup before any request)
    const store = requestContext.getStore()
    return {
      correlationId: store?.correlationId,
      traceId: store?.traceId,
      spanId: store?.spanId,
      userId: store?.userId,
    }
  }

  info(message: string, data?: Record<string, unknown>): void {
    this.logger.info(
      { ...this.getContextFields(), ...data },
      message
    )
  }

  warn(message: string, data?: Record<string, unknown>): void {
    this.logger.warn(
      { ...this.getContextFields(), ...data },
      message
    )
  }

  error(
    message: string,
    error?: Error,
    data?: Record<string, unknown>
  ): void {
    this.logger.error(
      {
        ...this.getContextFields(),
        ...data,
        // Structured error object — never log the full error as a string
        err: error ? {
          type: error.constructor.name,
          message: error.message,
          // Stack trace only in non-production — too verbose for prod
          stack: process.env.NODE_ENV !== 'production'
            ? error.stack
            : undefined,
        } : undefined,
      },
      message
    )
  }

  debug(message: string, data?: Record<string, unknown>): void {
    // Debug logs only in non-production environments
    if (process.env.NODE_ENV !== 'production') {
      this.logger.debug(
        { ...this.getContextFields(), ...data },
        message
      )
    }
  }
}
```

**How this is used in a service:**

```typescript
// src/modules/accounts/account.service.ts
@Injectable()
export class AccountService {

  constructor(
    private readonly logger: LoggerService,
    private readonly prisma: PrismaService,
    private readonly goCardlessService: GoCardlessService,
  ) {}

  async initiateConnection(
    userId: string,
    dto: ConnectBankDto
  ): Promise<ConnectBankResponse> {

    this.logger.info('Bank connection initiation started', {
      userId,
      institutionId: dto.institutionId,
    })

    const existingConnection = await this.prisma.bankConnection.findFirst({
      where: { userId, institutionId: dto.institutionId, status: { in: ['ACTIVE', 'PENDING'] } }
    })

    if (existingConnection) {
      this.logger.warn('Duplicate bank connection attempt blocked', {
        userId,
        institutionId: dto.institutionId,
        existingConnectionId: existingConnection.id,
      })
      throw new ConflictException('INSTITUTION_ALREADY_CONNECTED')
    }

    try {
      const requisition = await this.goCardlessService.createRequisition({
        institutionId: dto.institutionId,
        redirectUri: `${process.env.API_BASE_URL}/v1/accounts/connect/callback`,
        reference: userId,
      })

      this.logger.info('GoCardless requisition created', {
        userId,
        institutionId: dto.institutionId,
        requisitionId: requisition.id,
        // DO NOT log: requisition.link — contains sensitive auth URL
        // DO NOT log: any API keys or secrets
      })

      return { consentUrl: requisition.link, requisitionId: requisition.id }

    } catch (error) {
      this.logger.error('GoCardless requisition creation failed', error, {
        userId,
        institutionId: dto.institutionId,
      })
      throw error
    }
  }
}
```

**What the log output looks like in Datadog:**

```json
{
  "level": "info",
  "time": 1705324991421,
  "service": "core-product",
  "correlationId": "9e7d21299f4ea8a1cb3f4d2e8b1a0c5f",
  "traceId": "9e7d21299f4ea8a1cb3f4d2e8b1a0c5f",
  "spanId": "bbb222aabb112233",
  "userId": "usr_123",
  "institutionId": "MONZO_MONZGB2L",
  "requisitionId": "req_abc123",
  "msg": "GoCardless requisition created"
}
```

Every field is searchable. `traceId` links to the APM trace. `correlationId` connects all logs for this request. `userId` lets you filter all logs for this user. `institutionId` lets you find all connections to Monzo.

---

## AsyncLocalStorage in BullMQ Workers

The `requestContext.run()` call in the middleware populates the store for the HTTP request lifecycle. But BullMQ workers do not go through HTTP middleware — they process jobs from Redis directly.

This means workers need their own context setup:

```typescript
// transaction-sync.worker.ts
@Processor('transaction-sync', { concurrency: 10 })
export class TransactionSyncWorker extends WorkerHost {

  constructor(
    private readonly logger: LoggerService,
    // ... other dependencies
  ) {
    super()
  }

  async process(job: Job): Promise<void> {
    const { _traceContext, userId, accountIds } = job.data

    // Set up logging context for this job
    // Equivalent of MDC.put() before processing in Java
    await requestContext.run(
      {
        correlationId: _traceContext?.traceId ?? job.id,
        traceId: _traceContext?.traceId ?? job.id,
        spanId: _traceContext?.spanId ?? 'worker',
        userId,
      },
      async () => {
        // Everything inside this callback has the context
        // All logger calls will include correlationId, traceId, userId
        this.logger.info('Sync job started', {
          jobId: job.id,
          jobType: job.name,
          accountCount: accountIds.length,
          attemptsMade: job.attemptsMade,
        })

        try {
          await this.handleSync(job)

          this.logger.info('Sync job completed successfully', {
            jobId: job.id,
            jobType: job.name,
          })

        } catch (error) {
          this.logger.error('Sync job failed', error, {
            jobId: job.id,
            jobType: job.name,
            attemptsMade: job.attemptsMade,
            willRetry: job.attemptsMade < (job.opts.attempts ?? 3) - 1,
          })
          throw error
        }
      }
    )
    // Context automatically cleaned up when run() completes
    // No equivalent of MDC.clear() needed
  }
}
```

**Why AsyncLocalStorage is superior to MDC in this regard:**

In Java, you must call `MDC.clear()` in a `finally` block. If you forget, the next request on that thread inherits stale MDC data — log pollution. Your Spring Boot notes specifically cover this gotcha.

In Node.js with `AsyncLocalStorage`, the context is automatically scoped to the `run()` callback. When the callback resolves or rejects, the context is gone. No `clear()` needed, no pollution risk.

---

## Log Levels in Production

What actually gets logged in production versus development:

```
LOG LEVEL DECISIONS AT FINVERSE

PRODUCTION: level = 'info'
  → Everything at info and above is logged
  → debug and trace are silenced (too verbose, too expensive)

STAGING: level = 'debug'
  → All levels except trace

LOCAL DEV: level = 'debug' with pino-pretty
  → Human-readable output
  → Coloured, formatted

┌─────────────────────────────────────────────────────────────────┐
│  WHAT GOES AT EACH LEVEL                                        │
├──────────────┬──────────────────────────────────────────────────┤
│  fatal       │  Application cannot continue                     │
│              │  Process is about to exit                        │
│              │  e.g. "Cannot connect to PostgreSQL at startup"  │
├──────────────┼──────────────────────────────────────────────────┤
│  error       │  Something failed — needs investigation          │
│              │  Always include the Error object                 │
│              │  e.g. "GoCardless API call failed"               │
│              │  e.g. "BullMQ job failed after all retries"      │
├──────────────┼──────────────────────────────────────────────────┤
│  warn        │  Unexpected but handled — worth monitoring       │
│              │  e.g. "Duplicate connection attempt blocked"     │
│              │  e.g. "Redis cache miss rate above threshold"    │
│              │  e.g. "GoCardless returning 429, retrying"       │
├──────────────┼──────────────────────────────────────────────────┤
│  info        │  Normal significant events                       │
│              │  One or two per request — not every operation    │
│              │  e.g. "Bank connection initiated"                │
│              │  e.g. "Sync job completed: 247 transactions"     │
│              │  e.g. "User usr_123 connected Monzo account"     │
├──────────────┼──────────────────────────────────────────────────┤
│  debug       │  Detailed diagnostic information                 │
│              │  Useful for local development and staging        │
│              │  Never in production (too verbose)               │
│              │  e.g. "Checking Redis for pf:val:usr_123"        │
│              │  e.g. "GoCardless response received: 200"        │
├──────────────┼──────────────────────────────────────────────────┤
│  trace       │  Extremely detailed (function entry/exit)        │
│              │  Almost never used in production                 │
└──────────────┴──────────────────────────────────────────────────┘
```

**Java parallel:** exact same level hierarchy (ERROR, WARN, INFO, DEBUG, TRACE) with the same conventions. The production level in Spring Boot is also INFO by default.

---

## Sensitive Data: What Never Goes in Logs

This is non-negotiable at FinVerse — GDPR and PSD2 compliance requires that certain data never appears in logs.

```
FINVERSE — DATA THAT NEVER GOES IN LOGS

FINANCIAL DATA:
  ✗ IBAN numbers (bank account identifiers)
  ✗ Transaction amounts in isolation
     (aggregates are fine: "247 transactions synced")
  ✗ Account balances
  ✗ Card numbers

AUTHENTICATION DATA:
  ✗ JWT tokens (access or refresh)
  ✗ OTP codes
  ✗ Passwords (even hashed — never log hashed passwords)
  ✗ GoCardless API keys or secrets
  ✗ Stripe secret keys

PERSONAL DATA (PII — GDPR):
  ✗ Full name combined with financial data
  ✗ Email address in error logs (userId is sufficient)
  ✗ Date of birth

WHAT IS SAFE TO LOG:
  ✓ userId (opaque UUID — no PII value alone)
  ✓ accountId (opaque UUID)
  ✓ institutionId ("MONZO_MONZGB2L" — no user link)
  ✓ requisitionId (GoCardless internal ID)
  ✓ Counts and aggregates ("247 transactions", "3 accounts")
  ✓ Status codes and error types
  ✓ Durations and performance metrics
  ✓ traceId, correlationId, spanId
```

Pino's `redact` configuration (shown earlier) provides automatic redaction — any field matching the configured paths is replaced with `[REDACTED]` before the log line is written. This catches accidental logging of sensitive data even if a developer forgets.

**Java parallel:** your Spring Boot notes cover `MaskingJsonGeneratorDecorator` in LogstashEncoder — this is the exact equivalent. Same purpose, different configuration syntax.

---

## How Datadog Receives Logs from ECS Containers

The flow from NestJS stdout to Datadog:

```
LOG PIPELINE — FINVERSE

NestJS (Pino)
  logger.info({...}, 'sync started')
         │
         │  Serialises to JSON
         │  Writes to process.stdout
         ▼
ECS Fargate Container Runtime
  Captures stdout from container
  (no file system involved — ephemeral containers)
         │
         ▼
AWS CloudWatch Logs
  ECS automatically sends container stdout to CloudWatch
  Log group: /ecs/core-product
  Log stream: per container instance
         │
         │  Datadog CloudWatch Logs integration
         │  (lambda or subscription filter)
         ▼
Datadog Log Management
  Parses JSON automatically (source: nodejs detected)
  Indexes all fields as facets
  Links logs to APM traces via traceId
  Applies retention policy (30 days for production)
  Enables search, filtering, live tail
```

Alternatively, FinVerse can use the Datadog Agent as a sidecar container alongside the NestJS container — the agent reads stdout directly and forwards to Datadog, bypassing CloudWatch. Both approaches work. The CloudWatch integration is simpler to set up; the sidecar approach has lower latency.

---

## Chapter 4 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 4 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Java vs Node.js logging:                                       │
│  SLF4J/Logback → Pino (no standard facade in Node.js)           │
│  LogstashEncoder → Pino default (JSON out of the box)           │
│  MDC/ThreadLocal → AsyncLocalStorage                            │
│  File appenders → stdout only (container logging model)         │
│                                                                 │
│  Why Pino: fastest Node.js logger, async transport,             │
│  JSON by default, official NestJS integration                   │
│  ~5× faster than Winston — matters at high throughput           │
│                                                                 │
│  Structured logging: every log is a JSON object                 │
│  Every field is searchable in Datadog without parsing           │
│  traceId links logs to APM traces (one click in Datadog)        │
│                                                                 │
│  AsyncLocalStorage: the Node.js ThreadLocal equivalent          │
│  Scoped to async execution chain, not a thread                  │
│  Automatically cleaned up — no MDC.clear() needed               │
│  Works across all awaited calls in the same chain               │
│                                                                 │
│  Correlation ID = Trace ID at FinVerse                          │
│  One ID connects distributed traces and all log lines           │
│  Set in middleware for HTTP requests                            │
│  Set manually in requestContext.run() for BullMQ workers        │
│                                                                 │
│  Sensitive data: never log IBANs, tokens, amounts, PII          │
│  Pino redact config catches accidental logging                  │
│  Log userId (opaque UUID), never email or full name             │
│                                                                 │
│  Production log level: info                                     │
│  debug only in staging/dev — too verbose and expensive          │
│  Always include Error object in error logs — never              │
│  just the message string                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

