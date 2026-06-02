Looking at all the materials carefully, I can see the Core Product Service is a NestJS modular monolith. Let me build out the complete folder structure with clear explanations of what lives where and why.

---

# Core Product Service — Complete Codebase Structure

---

## The Mental Model First

Before the structure, understand the shape of what you are looking at.

Core Product is a **modular monolith** — one single NestJS application deployed as one container, but internally divided into strict domain modules. Each module has its own folder, its own controllers, services, and DTOs. Modules never reach into each other's folders directly.

Think of it like an apartment building. One building (one deployed service), but each apartment (each module) has its own door. You cannot walk through the wall between apartments — you have to use the hallway (the exported service interface).

---

## Top-Level Structure

```
core-product/
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       ├── deploy-staging.yml
│       └── deploy-production.yml
│
├── prisma/
│   ├── schema.prisma
│   └── migrations/
│       ├── 20230815_initial_schema/
│       │   └── migration.sql
│       ├── 20231103_add_expiry_index_to_bank_connections/
│       │   └── migration.sql
│       ├── 20231120_add_transactions_duplicate_to_sync_log/
│       │   └── migration.sql
│       └── ... (one folder per migration, timestamped)
│
├── src/
│   ├── main.ts
│   ├── app.module.ts
│   ├── instrumentation.ts
│   │
│   ├── common/
│   │   ├── context/
│   │   ├── decorators/
│   │   ├── filters/
│   │   ├── guards/
│   │   ├── interceptors/
│   │   ├── logger/
│   │   ├── metrics/
│   │   ├── middleware/
│   │   ├── prisma/
│   │   └── redis/
│   │
│   └── modules/
│       ├── auth/
│       ├── accounts/           ← YOUR MODULE (primary ownership)
│       ├── transactions/
│       ├── budgets/
│       ├── goals/
│       ├── investing/
│       ├── retirement/
│       ├── tax/
│       └── education/
│
├── test/
│   ├── unit/
│   └── integration/
│
├── Dockerfile
├── docker-compose.yml
├── .nvmrc
├── package.json
├── tsconfig.json
└── nest-cli.json
```

---

## Why Each Top-Level Folder Exists

```
.github/workflows/
  Contains your CI/CD pipeline YAML files.
  These are version-controlled alongside code — not separate.
  If a pipeline needs changing, it goes through PR review
  like any other code change.

prisma/
  Everything related to your database schema lives here.
  schema.prisma      → the single source of truth for all models
  migrations/        → one subfolder per migration, timestamped,
                       committed to Git, applied in order

src/
  All application source code.
  Two children: common/ and modules/
  More on both below.

test/
  Tests live outside src/ deliberately.
  Unit tests: fast, no database, no network, run on every PR
  Integration tests: spin up real PostgreSQL and Redis,
                     test full request-to-database flows

Dockerfile
  Defines how to build the production container image.
  Multi-stage build: builder stage compiles TypeScript,
  production stage contains only the compiled JS output.

docker-compose.yml
  Local development only. Starts PostgreSQL, Redis, and
  RabbitMQ containers on your machine so you can run
  the service locally without connecting to AWS.

.nvmrc
  Pins the exact Node.js version the team uses.
  node --version must match this file.
  Prevents "works on my machine" version problems.
```

---

## The `src/` Folder — Two Children

Everything runnable lives in `src/`. It has exactly two children: `common/` and `modules/`.

The distinction is simple:

- `common/` — code that is shared across every module (logging, auth guards, database connection, Redis client)
- `modules/` — domain code, one folder per business domain

---

## `src/common/` — Shared Infrastructure

```
src/common/
│
├── context/
│   └── request-context.ts
│
├── decorators/
│   └── user-id.decorator.ts
│
├── filters/
│   └── http-exception.filter.ts
│
├── guards/
│   └── jwt-auth.guard.ts
│
├── interceptors/
│   └── metrics.interceptor.ts
│
├── logger/
│   └── logger.service.ts
│
├── metrics/
│   └── metrics.service.ts
│
├── middleware/
│   └── request-context.middleware.ts
│
├── prisma/
│   └── prisma.service.ts
│
└── redis/
    └── redis.service.ts
```

What each file does:

```
request-context.ts
  Defines the AsyncLocalStorage store that carries
  traceId, correlationId, and userId across async
  operations within a single request.
  
  Why this exists: Node.js is asynchronous. A request
  spawns many async callbacks (DB queries, API calls).
  You need a way to carry context (like the user's ID)
  through all of them without passing it as a parameter
  to every single function.
  
  Java equivalent: ThreadLocal + MDC.
  Node.js equivalent: AsyncLocalStorage.

user-id.decorator.ts
  A NestJS parameter decorator used in controllers.
  Usage: @UserId() userId: string
  
  It reads the x-user-id header injected by the API
  Gateway after JWT validation. The controller does not
  manually parse headers — the decorator handles it.

http-exception.filter.ts
  A global NestJS exception filter.
  Catches any unhandled exception thrown anywhere
  in the application and formats it into the standard
  error response envelope:
  { error: { code, message, statusCode, traceId } }
  
  Without this, NestJS would return its own default
  error format, which is inconsistent with the team's
  envelope convention.

jwt-auth.guard.ts
  A NestJS guard applied to protected routes.
  In Core Product, this guard does NOT validate the JWT
  itself — the API Gateway already validated it upstream.
  It simply checks that the x-user-id header is present
  and trusts it as authoritative.

metrics.interceptor.ts
  A NestJS interceptor applied globally.
  Runs before and after every HTTP handler.
  Records: HTTP method, endpoint, status code, duration.
  Sends to OpenTelemetry metrics pipeline → Datadog.

logger.service.ts
  A thin wrapper around Pino (the logging library).
  Every module injects this instead of using console.log.
  Automatically attaches traceId and correlationId from
  AsyncLocalStorage to every log line.

metrics.service.ts
  Wrapper around OpenTelemetry metrics SDK.
  Provides typed methods: recordHttpRequest(),
  recordSyncCompleted(), recordGoCardlessCall(), etc.
  Every module that needs to emit a metric calls this.

request-context.middleware.ts
  NestJS middleware that runs on every incoming request.
  Reads the traceId from the incoming OTEL span.
  Calls requestContext.run() to set up AsyncLocalStorage
  so all downstream async calls inherit the context.

prisma.service.ts
  The single Prisma client instance for the entire
  application. Injectable via NestJS DI.
  Every module's repository injects this — they do not
  create their own Prisma clients.
  Also handles connection lifecycle: connects on startup,
  disconnects on shutdown.

redis.service.ts
  Wraps the ioredis client.
  Single Redis connection shared across all modules.
  Provides typed helper methods for common operations.
```

---

## `src/modules/` — Domain Modules

Each module has the same internal structure. Here it is shown for your module first, then the pattern generalises.

---

### Your Module: `src/modules/accounts/`

```
src/modules/accounts/
│
├── accounts.module.ts
│
├── controllers/
│   └── accounts.controller.ts
│
├── services/
│   └── account.service.ts
│
├── workers/
│   ├── transaction-sync.worker.ts
│   └── check-expiring-consents.worker.ts
│
├── schedulers/
│   ├── sync.scheduler.ts
│   └── consent-check.scheduler.ts
│
├── producers/
│   └── sync.producer.ts
│
├── gocardless/
│   ├── gocardless.service.ts
│   └── gocardless.types.ts
│
├── dto/
│   ├── connect-bank.dto.ts
│   ├── account-list.response.ts
│   ├── account-detail.response.ts
│   ├── sync-status.response.ts
│   └── net-worth.response.ts
│
└── constants/
    └── event-types.ts
```

What each piece does:

```
accounts.module.ts
  The NestJS module definition file.
  Declares: which controllers, services, workers, and
  schedulers belong to this module.
  Imports: BullModule (to get queue references),
  PrismaService, RedisService, GoCardlessService.
  Exports: AccountService (so other modules can call it).

accounts.controller.ts
  Defines HTTP endpoints:
    GET  /v1/accounts
    GET  /v1/accounts/net-worth
    GET  /v1/accounts/:accountId
    POST /v1/accounts/connect
    GET  /v1/accounts/connect/callback
    POST /v1/accounts/:accountId/sync
    GET  /v1/accounts/:accountId/sync-status
    DELETE /v1/accounts/connections/:connectionId
  
  Controllers are thin. They validate input, call the
  service, and return the response. No business logic here.

account.service.ts
  All business logic for the accounts module.
  Methods:
    initiateConnection()     → creates GoCardless requisition
    handleCallbackSuccess()  → processes GoCardless redirect
    getAccountsForUser()     → returns account list
    getAccountById()         → single account detail
    triggerManualSync()      → enqueues sync job
    getSyncStatus()          → reads sync progress
    getNetWorth()            → aggregates balances
    disconnectBank()         → soft-deletes connection
  
  This file grew the most over your 12 months.
  Month 3 incidents, Month 4 features, Month 7 cache
  invalidation fix — all changes went through this file.

transaction-sync.worker.ts
  The BullMQ worker that processes sync jobs.
  Registered as a processor for the 'transaction-sync' queue.
  Handles two job types: INITIAL_SYNC and PERIODIC_SYNC.
  
  Key methods:
    process()            → dispatcher (routes to correct handler)
    handleInitialSync()  → full history fetch for new connections
    handlePeriodicSync() → incremental fetch since last sync
  
  This file contains the concurrent account processing
  you implemented in Month 5 (Promise.all refactor),
  the lock duration configuration from Month 5,
  and the cache invalidation call from Month 7.

check-expiring-consents.worker.ts
  The BullMQ worker for the daily consent expiry check.
  Registered as a processor for the 'consent-check' queue.
  Scans for connections expiring within 7 days.
  Writes outbox events for the notification flow.
  Built in Month 4.

sync.scheduler.ts
  Registers the recurring PERIODIC_SYNC repeatable job
  at NestJS module init.
  Ensures the scheduler survives container restarts
  via the singleton jobId pattern.

consent-check.scheduler.ts
  Registers the daily CHECK_EXPIRING_CONSENTS job.
  Cron: 09:00 UTC daily.
  Singleton jobId: 'check-expiring-consents-daily'.
  Built in Month 4.

sync.producer.ts
  Helper service for enqueuing sync jobs.
  Used by account.service.ts when a user connects
  a bank account or manually triggers a sync.
  Encapsulates the queue.add() call and its options
  (jobId, priority, attempts, backoff config).

gocardless.service.ts
  Wraps all GoCardless API calls.
  Methods:
    createRequisition()     → starts consent flow
    getRequisition()        → checks consent status
    getAccountDetails()     → fetches IBAN, name, type
    fetchAllTransactions()  → initial sync, full history
    fetchTransactionsSince()→ incremental sync
    deleteRequisition()     → revoke access on disconnect
  
  All GoCardless-specific error handling lives here.
  The normaliseIban() function Elena wrote (handling
  Sparkasse's non-standard IBAN format) lives here.

gocardless.types.ts
  TypeScript interfaces for GoCardless API responses.
  GoCardlessTransaction, GoCardlessAccount,
  GoCardlessRequisition, etc.

dto/ (Data Transfer Objects)
  One file per request or response shape.
  
  connect-bank.dto.ts
    Request body for POST /v1/accounts/connect
    Contains: institutionId (string), countryCode (ISO)
    Validation decorators: @IsString, @IsISO31661Alpha2
  
  account-list.response.ts
    Response shape for GET /v1/accounts
    Contains: AccountDto[], totalBalance by currency
  
  account-detail.response.ts
    Response shape for GET /v1/accounts/:id
    More fields than list — includes full connection detail
  
  sync-status.response.ts
    Response shape for GET /v1/accounts/:id/sync-status
    Contains: syncStatus, lastSyncedAt, lastSync (SyncLog)
  
  net-worth.response.ts
    Response shape for GET /v1/accounts/net-worth
    Contains: balancesByCurrency[], accountCount, asOf

constants/event-types.ts
  Shared string constants for outbox event types.
  Used by both the worker (when writing) and the
  outbox publisher (when reading).
  Prevents typos from string literals scattered everywhere.
  Built in Month 12 as part of the consent re-auth work.
```

---

### Every Other Module Follows the Same Pattern

The internal structure is identical across all modules. The files just contain different domain logic.

```
src/modules/auth/
├── auth.module.ts
├── controllers/
│   └── auth.controller.ts        → POST /v1/auth/register, /login, etc.
├── services/
│   └── auth.service.ts
├── strategies/
│   └── jwt.strategy.ts           → Passport JWT strategy
└── dto/
    ├── register.dto.ts
    └── login.dto.ts

src/modules/transactions/
├── transactions.module.ts
├── controllers/
│   └── transactions.controller.ts → GET /v1/transactions
├── services/
│   ├── transaction.service.ts
│   └── categorisation.service.ts  → applies MerchantRule patterns
└── dto/
    └── ...

src/modules/budgets/
├── budgets.module.ts
├── controllers/
│   └── budgets.controller.ts
├── services/
│   └── budget.service.ts          → tracks spent, fires threshold events
└── dto/
    └── ...

src/modules/goals/
├── goals.module.ts
├── controllers/
│   └── goals.controller.ts
├── services/
│   └── goals.service.ts
└── dto/
    └── ...

src/modules/investing/
├── investing.module.ts
├── controllers/
│   └── investing.controller.ts
├── services/
│   └── investing.service.ts
└── dto/
    └── ...

src/modules/retirement/
├── retirement.module.ts
└── ...  (same pattern)

src/modules/tax/
├── tax.module.ts
├── workers/
│   └── tax-report.worker.ts       → BullMQ worker, concurrency 5
├── schedulers/
│   └── tax-report.scheduler.ts   → fires on Jan 1 each year
└── ...

src/modules/education/
├── education.module.ts
└── ...  (same pattern)
```

---

## The BullMQ Worker Containers

This is the part that confuses most people — where do the workers live relative to this folder structure?

The workers are **defined inside** `src/modules/` but **deployed separately** as their own ECS containers.

```
HOW ONE CODEBASE BECOMES MULTIPLE CONTAINERS

The same Git repository, the same src/ folder.
But there are multiple entry points:

Entry point 1: src/main.ts
  Starts the HTTP API server.
  Registers controllers and middleware.
  Does NOT start BullMQ workers.
  → Deployed as the "core-product-api" ECS service

Entry point 2: src/workers/tx-sync.main.ts
  Starts ONLY the transaction-sync BullMQ worker.
  No HTTP server. No controllers.
  Just the worker processor and its dependencies.
  → Deployed as the "tx-sync-worker" ECS service

Entry point 3: src/workers/budget-check.main.ts
  Starts ONLY the budget-check BullMQ worker.
  → Deployed as the "budget-check-worker" ECS service

Entry point 4: src/workers/tax-report.main.ts
  Starts ONLY the tax-report BullMQ worker.
  → Deployed as the "tax-report-worker" ECS service

Entry point 5: src/workers/outbox-publisher.main.ts
  Starts ONLY the outbox-publisher BullMQ worker.
  → Deployed as the "outbox-publisher-worker" ECS service
```

The worker entry point files live in `src/workers/`:

```
src/workers/
├── tx-sync.main.ts
├── budget-check.main.ts
├── tax-report.main.ts
└── outbox-publisher.main.ts
```

Each one is a minimal NestJS bootstrap that loads only the modules it needs:

```typescript
// src/workers/tx-sync.main.ts

import { NestFactory } from '@nestjs/core'
import { Module } from '@nestjs/common'
import { BullModule } from '@nestjs/bullmq'
import { TransactionSyncWorker } from '../modules/accounts/workers/transaction-sync.worker'
import { GoCardlessService } from '../modules/accounts/gocardless/gocardless.service'
import { TransactionService } from '../modules/transactions/services/transaction.service'
import { PrismaService } from '../common/prisma/prisma.service'
import { RedisService } from '../common/redis/redis.service'
import { LoggerService } from '../common/logger/logger.service'

// A minimal NestJS module containing only what this worker needs
@Module({
  imports: [
    BullModule.registerQueue({ name: 'transaction-sync' }),
  ],
  providers: [
    TransactionSyncWorker,   // the processor
    GoCardlessService,       // what it calls
    TransactionService,      // what it writes to
    PrismaService,           // database connection
    RedisService,            // Redis connection
    LoggerService,           // structured logging
  ],
})
class TxSyncWorkerModule {}

async function bootstrap() {
  const app = await NestFactory.create(TxSyncWorkerModule)
  await app.init()
  // No app.listen() — this is not an HTTP server
  // It just stays alive processing BullMQ jobs
  console.log('Transaction sync worker started')
}

bootstrap()
```

This is why the worker is in a separate container but uses the exact same source code. The container image is the same Docker image. The difference is which entry point the container runs:

```
API container:    CMD ["node", "dist/main.js"]
Worker container: CMD ["node", "dist/workers/tx-sync.main.js"]
```

---

## How the Pieces Connect — A Full Request Flow

To make the folder structure concrete, trace one real operation: a user taps "Sync now" in the app.

```
USER TAPS "SYNC NOW"
        │
        │  POST /v1/accounts/acc_456/sync
        │  Authorization: Bearer <JWT>
        ▼
src/common/guards/jwt-auth.guard.ts
  → Verifies x-user-id header is present
  → Sets userId = 'usr_123' in request

src/common/middleware/request-context.middleware.ts
  → Reads traceId from OTEL span
  → Calls requestContext.run({ traceId, userId })

src/common/interceptors/metrics.interceptor.ts
  → Records start time

src/modules/accounts/controllers/accounts.controller.ts
  → Receives POST /v1/accounts/:accountId/sync
  → Extracts userId via @UserId() decorator
  → Extracts accountId via @Param('accountId', ParseUUIDPipe)
  → Calls accountService.triggerManualSync(userId, accountId)

src/modules/accounts/services/account.service.ts
  → Calls prismaService to find account (ownership check)
  → Calls prismaService to update syncStatus to 'SYNCING'
  → Calls syncProducer.enqueueSyncJob(userId, accountId)

src/modules/accounts/producers/sync.producer.ts
  → Calls syncQueue.add('PERIODIC_SYNC', { userId, accountIds })
  → BullMQ writes job to Redis

src/modules/accounts/controllers/accounts.controller.ts
  → Returns HTTP 202 { message: 'Sync started' }

src/common/interceptors/metrics.interceptor.ts
  → Records duration, calls metricsService.recordHttpRequest()

src/common/metrics/metrics.service.ts
  → Sends metric to OTEL → Datadog

─────────────────────────────────────────
  [5 seconds later, in a different container]

src/workers/tx-sync.main.ts (worker container)
  → BullMQ worker picks up the job from Redis

src/modules/accounts/workers/transaction-sync.worker.ts
  → process() dispatches to handlePeriodicSync()
  → Calls goCardlessService.fetchTransactionsSince()

src/modules/accounts/gocardless/gocardless.service.ts
  → Makes HTTP call to GoCardless API
  → Normalises response, handles IBAN formatting

src/modules/accounts/workers/transaction-sync.worker.ts
  → Calls transactionService.bulkInsert()

src/modules/transactions/services/transaction.service.ts
  → prisma.transaction.createMany({ skipDuplicates: true })
  → prisma.budget.update({ spent: newAmount })
  → prisma.outboxEvent.create({ eventType: 'budget.threshold...' })

src/modules/accounts/workers/transaction-sync.worker.ts
  → redis.del(`usr:acct:acc:${accountId}`)    ← cache invalidation
  → calls completeSyncWithLog()
  → prisma.$transaction([ update bankAccount, create SyncLog ])

─────────────────────────────────────────
  [5 seconds later, in yet another container]

src/workers/outbox-publisher.main.ts (outbox worker container)
  → Reads PENDING rows from outbox_events table
  → Publishes to RabbitMQ exchange
  → Updates outbox_events status to PUBLISHED
```

Every file referenced in that flow is in the folder structure above. Now when you read a code snippet from any module document, you know exactly which folder it lives in and how it connects to everything else.

---

## The `test/` Folder

```
test/
├── unit/
│   └── modules/
│       └── accounts/
│           ├── account.service.spec.ts
│           ├── gocardless.service.spec.ts
│           └── transaction-sync.worker.spec.ts
│
└── integration/
    └── modules/
        └── accounts/
            ├── accounts.controller.integration.spec.ts
            └── sync-flow.integration.spec.ts
```

The naming convention the team follows:

```
*.spec.ts       → test file suffix (Jest convention)
*.unit.spec.ts  → explicitly unit (some teams add this)
*.integration.spec.ts → requires real DB and Redis

Unit tests:
  Fast. Run on every commit.
  Mock everything external: Prisma, Redis, GoCardless.
  Test one function at a time.
  Live in test/unit/ mirroring the src/ structure.

Integration tests:
  Slower. Run on every PR (CI spins up real PostgreSQL
  and Redis via Docker services in the GitHub Action).
  Test a full request-to-database flow.
  The accounts.controller.integration.spec.ts file
  sends real HTTP requests to a live NestJS app
  backed by a real test database.
  
  This is where the ownership check test lives —
  it sends a request with user A's JWT for user B's
  account and verifies it returns 404.
```

---

## One-Page Summary — The Mental Map

```
WHEN YOU SEE A FILE REFERENCE, HERE IS WHERE IT LIVES

"accounts.controller.ts"
→ src/modules/accounts/controllers/accounts.controller.ts
→ HTTP handlers for all /v1/accounts/* endpoints

"account.service.ts"
→ src/modules/accounts/services/account.service.ts
→ All business logic for the accounts module

"transaction-sync.worker.ts"
→ src/modules/accounts/workers/transaction-sync.worker.ts
→ BullMQ processor, runs in its own container

"gocardless.service.ts"
→ src/modules/accounts/gocardless/gocardless.service.ts
→ All GoCardless API calls, IBAN normalisation

"prisma.service.ts"
→ src/common/prisma/prisma.service.ts
→ Single shared Prisma client, injected by every module

"logger.service.ts"
→ src/common/logger/logger.service.ts
→ Pino wrapper, attaches traceId to every log line

"schema.prisma"
→ prisma/schema.prisma
→ All database models, one file for the entire service

Any migration file
→ prisma/migrations/{timestamp}_{name}/migration.sql

Worker entry points
→ src/workers/{name}.main.ts
→ What the worker container actually runs
```