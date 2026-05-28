Writing Chapter 8 now. Core Product Service only.

---

# BullMQ — Chapter 8: BullMQ at FinVerse: Every Queue, Every Decision

---

## Why This Chapter Exists

The previous chapters taught you BullMQ from first principles. This chapter answers the question interviewers actually ask: **"Tell me specifically how you used BullMQ at FinVerse."**

This is not a textbook answer. This is your system, your queues, your trade-offs, and your production problems. An interviewer who asks "why did you use BullMQ?" wants to hear you describe something real — specific queues, specific numbers, specific failures and how you fixed them.

Everything in this chapter is scoped to **Core Product Service only** — the service your team owns.

---

## The Complete Queue Map — Core Product Service

Core Product Service runs four BullMQ queues. Each lives in a dedicated worker container, separate from the API container. The API container only produces jobs. The worker containers only consume them.

```
CORE PRODUCT SERVICE — BULLMQ ARCHITECTURE

┌─────────────────────────────────────────────────────────────────────┐
│                     CORE PRODUCT SERVICE                            │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  API Container (HTTP Server)                                 │   │
│  │  - Handles mobile app requests                               │   │
│  │  - Produces jobs → Redis                                     │   │
│  │  - Never processes jobs                                      │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                     queue.add()                                     │
│                          │                                          │
│                          ▼                                          │
│            ┌─────────────────────────┐                              │
│            │    Redis (ElastiCache)  │                              │
│            │                         │                              │
│            │  bull:transaction-sync  │                              │
│            │  bull:budget-check      │                              │
│            │  bull:tax-report-gen    │                              │
│            │  bull:outbox-publisher  │                              │
│            └─────────────────────────┘                              │
│        │           │           │             │                      │
│        ▼           ▼           ▼             ▼                      │
│  ┌──────────┐ ┌──────────┐ ┌───────┐ ┌────────────────┐             │
│  │tx-sync   │ │budget-   │ │tax-   │ │outbox-publisher│             │
│  │Worker    │ │check     │ │report │ │Worker          │             │
│  │Container │ │Worker    │ │Worker │ │Container       │             │
│  │          │ │Container │ │Cont.  │ │                │             │
│  │conc: 10  │ │conc: 20  │ │conc:5 │ │conc: 1         │             │
│  │1 vCPU    │ │1 vCPU    │ │2 vCPU │ │0.5 vCPU        │             │
│  │2GB RAM   │ │2GB RAM   │ │4GB RAM│ │1GB RAM         │             │
│  └──────────┘ └──────────┘ └───────┘ └────────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                   QUEUE SUMMARY TABLE                               │
├──────────────────────┬───────────┬──────────────────────────────────┤
│  Queue               │  Conc.    │  Module Owner                    │
├──────────────────────┼───────────┼──────────────────────────────────┤
│  transaction-sync    │  10       │  You (Accounts & Open Banking)   │
├──────────────────────┼───────────┼──────────────────────────────────┤
│  budget-check        │  20       │  Tomasz (Budgeting)              │
├──────────────────────┼───────────┼──────────────────────────────────┤
│  tax-report-gen      │  5        │  Isabelle (Tax & Reporting)      │
├──────────────────────┼───────────┼──────────────────────────────────┤
│  outbox-publisher    │  1        │  Lucas (cross-module concern)    │
└──────────────────────┴───────────┴──────────────────────────────────┘
```

---

## Queue 1 — transaction-sync

### What It Does

This is the most important queue in Core Product. It handles everything related to fetching bank transactions from GoCardless and persisting them to PostgreSQL. It processes two job types:

```
INITIAL_SYNC   — fired once, immediately after a user connects a bank account
PERIODIC_SYNC  — fired on a recurring schedule, every 4 hours per connected user
```

### Why It Exists

After a user connects their bank account, FinVerse needs to:

```
For each connected account:
  1. Call GoCardless API → fetch transaction history (up to 3 years)
  2. For each transaction → check if already in PostgreSQL (deduplication)
  3. For each new transaction → run categorisation rules
  4. Bulk insert all new transactions into PostgreSQL
  5. Update account sync status and last_synced_at timestamp
```

A user with 3 bank accounts and 3 years of history can have 15,000 transactions. GoCardless has rate limits. Each account is a separate API call. This work realistically takes 15–30 seconds.

Running this inside the HTTP request handler means:

```
User taps "Connect Bank Account"
        │
        ▼
POST /v1/accounts/connect
        │
   [15-30 seconds of work]
        │
        ▼
HTTP response returned

Problems:
  ✗ User stares at a spinner for 30 seconds
  ✗ Most HTTP clients time out at 10-30 seconds
  ✗ If GoCardless returns 429 halfway through,
    the whole thing fails with no retry
  ✗ No visibility into what succeeded and what didn't
```

With BullMQ:

```
User taps "Connect Bank Account"
        │
        ▼
POST /v1/accounts/connect
        │
  queue.add('INITIAL_SYNC', { userId, accountIds })
        │
        ▼
HTTP 202 returned immediately
("we've accepted your request")
        │
        │                BullMQ Worker (separate container)
        │                        │
        └────── Redis ──────────►│ picks up job
                                 │ calls GoCardless
                                 │ inserts transactions
                                 │ handles retries if 429
                                 │ marks job complete
                                 ▼
                         User gets push notification:
                         "Your accounts are ready"
```

### Configuration and Reasoning

```typescript
// transaction-sync.worker.ts
@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,           // max 30 jobs processed
    duration: 10_000,  // per 10-second window
  },
  stalledInterval: 30_000,
  maxStalledCount: 2,
  lockDuration: 60_000,    // extended — syncs can take 30-45s
  lockRenewTime: 30_000,   // renew halfway through lock duration
})
export class TransactionSyncWorker extends WorkerHost {

  async process(job: Job): Promise<void> {
    switch (job.name) {
      case 'INITIAL_SYNC':
        return this.handleInitialSync(job)
      case 'PERIODIC_SYNC':
        return this.handlePeriodicSync(job)
      default:
        throw new Error(`Unknown job type: ${job.name}`)
    }
  }

  private async handleInitialSync(job: Job): Promise<void> {
    const { userId, accountIds } = job.data

    for (let i = 0; i < accountIds.length; i++) {
      // Progress update — visible in Bull Board dashboard
      await job.updateProgress({
        current: i + 1,
        total: accountIds.length,
        currentAccount: accountIds[i],
      })

      const transactions =
        await this.goCardlessService.fetchAllTransactions(accountIds[i])

      // bulkInsert is idempotent — externalId unique constraint
      // prevents duplicate rows on retry
      await this.transactionService.bulkInsert(
        userId,
        accountIds[i],
        transactions
      )
    }
  }

  private async handlePeriodicSync(job: Job): Promise<void> {
    const { userId, accountIds } = job.data

    for (const accountId of accountIds) {
      const lastSync =
        await this.transactionService.getLastSyncTimestamp(accountId)

      const newTransactions =
        await this.goCardlessService.fetchTransactionsSince(
          accountId,
          lastSync
        )

      if (newTransactions.length > 0) {
        await this.transactionService.bulkInsert(
          userId,
          accountId,
          newTransactions
        )
      }
    }
  }
}
```

**Why concurrency 10:**

Each job makes multiple GoCardless HTTP calls and multiple PostgreSQL queries — pure I/O. The event loop is idle during these waits. 10 concurrent jobs means up to 10 GoCardless calls and 10 PostgreSQL operations in flight simultaneously. This is comfortably within the GoCardless rate limit (50 requests/second per API key across all workers) and the PostgreSQL connection pool size.

**Why the rate limiter exists alongside concurrency:**

Concurrency controls how many jobs run simultaneously. But if jobs complete very quickly (periodic sync for a user with few new transactions finishes in 2 seconds), BullMQ refills slots fast — potentially exceeding the GoCardless API limit during burst periods. The rate limiter (30 jobs per 10 seconds across this worker instance) caps throughput regardless of how fast individual jobs finish.

With 3 worker containers at peak: 3 × 30 = 90 jobs per 10 seconds maximum. Each job makes roughly 2–3 GoCardless calls. That is 270 calls per 10 seconds — within the limit.

**Why INITIAL_SYNC and PERIODIC_SYNC live in the same queue:**

They share the same downstream dependencies (GoCardless, PostgreSQL) and the same rate limit budget. Running them in separate queues would split that budget and complicate monitoring. Priority handles urgency:

```typescript
// High priority — user is actively waiting at the screen
await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId, accountIds },
  {
    jobId: `initial-sync-${userId}`,
    priority: 1,
    attempts: 3,
    backoff: { type: 'exponential', delay: 5000 },
  }
)

// Lower priority — background, user is probably offline
await this.syncQueue.add(
  'PERIODIC_SYNC',
  { userId, accountIds },
  {
    jobId: `periodic-sync-${userId}-${Date.now()}`,
    priority: 2,
    attempts: 3,
    backoff: { type: 'exponential', delay: 5000 },
  }
)
```

**Your ownership of this queue:**

This is the queue you own end-to-end. You wrote both job handlers, set the concurrency and rate limiter values, handled the deduplication logic via `externalId` unique constraints, and instrumented progress updates so Bull Board shows real-time sync progress per account. When this queue backs up or has failures, you are the first person paged.

---

## Queue 2 — budget-check

### What It Does

After every successful transaction sync, new spending needs to be checked against the user's monthly budget limits. If any category's spending has crossed the configured threshold (default 80%), a `budget.threshold.exceeded` event gets published to RabbitMQ, which Notification Service consumes.

```typescript
// budget-check.producer.ts (called by transaction-sync worker on completion)
async enqueueBudgetCheck(userId: string): Promise<void> {
  await this.budgetCheckQueue.add(
    'BUDGET_CHECK',
    { userId },
    {
      // One budget check per user per calendar month maximum
      // If a check is already pending for this user this month,
      // BullMQ silently ignores this add call
      jobId: `budget-check-${userId}-${getCurrentYearMonth()}`,
      priority: 2,
      attempts: 3,
      backoff: { type: 'fixed', delay: 3000 },
    }
  )
}
```

### Why It Exists as a Separate Queue

In the original design, budget checking ran inline inside the `transaction-sync` worker — after inserting transactions, the worker immediately ran budget checks in the same job. This worked for users with simple budgets and few categories.

As the user base grew and Premium users started creating budgets across 10–15 categories, the combined sync + check started taking 45+ seconds for large accounts. This pushed jobs dangerously close to the lock TTL, causing stalled job errors.

Lucas made the call in month 4 to extract budget checking into its own queue. The sync job now enqueues a budget-check job after completing, rather than doing the check inline:

```
BEFORE (sync + check in same job):
transaction-sync job
  ├── GoCardless API calls (15-25s)
  ├── Bulk insert transactions (5s)
  └── Budget check across all categories (8-12s)
  Total: 28-42s  ← approaching lock TTL of 30s

AFTER (separate queues):
transaction-sync job
  ├── GoCardless API calls (15-25s)
  ├── Bulk insert transactions (5s)
  └── queue.add('budget-check', ...)  ← 1ms
  Total: 20-30s  ← safe

budget-check job (runs separately, immediately after)
  └── Budget threshold checks (8-12s)
  Total: 8-12s  ← fast, focused
```

Two clear benefits: sync jobs are leaner and no longer risk lock expiry. Budget checks can be independently retried if they fail — a failing budget check no longer blocks or retries an expensive sync job.

**Why concurrency 20:**

Budget checks are extremely lightweight. Each check is:
1. One PostgreSQL aggregate query (sum of transactions per category this month)
2. One PostgreSQL read (user's budget limits)
3. Comparison logic (pure CPU, negligible)
4. Conditional RabbitMQ publish (only if threshold crossed)

The only I/O is two PostgreSQL queries. The event loop is idle almost the entire time. The PostgreSQL connection pool (15 connections per worker) handles 20 concurrent queries comfortably.

---

## Queue 3 — tax-report-generation

### What It Does

Once per year, on January 1st, FinVerse generates PDF tax reports for all Premium users. Each report covers the user's investment activity, capital gains or losses, and applicable deductions under their country's tax rules (8 different EU jurisdictions). The PDF is uploaded to AWS S3 and the user is notified.

```typescript
// tax-report.producer.ts
// This runs as a one-time scheduled job at midnight January 1st
async enqueueAnnualReports(taxYear: number): Promise<void> {
  const premiumUsers = await this.userService.getAllPremiumUsers()

  for (const user of premiumUsers) {
    await this.taxReportQueue.add(
      'ANNUAL_TAX_REPORT',
      {
        userId: user.id,
        taxYear,
        country: user.country,
      },
      {
        // Deterministic jobId — safe to replay without generating duplicates
        jobId: `tax-report-${user.id}-${taxYear}`,
        attempts: 2,
        backoff: { type: 'fixed', delay: 60_000 },  // 1 minute between retries
      }
    )
  }
}
```

### Why concurrency 5 specifically

Tax report generation is the most resource-intensive job in Core Product. For a Premium user with 3 years of investment history across multiple ETFs, one job:

- Reads potentially 50,000+ transaction rows from PostgreSQL
- Loads all holding records, order history, and dividend data
- Applies country-specific tax calculation logic
- Renders a multi-page PDF document in memory
- Uploads the PDF to S3

This is not purely I/O-bound. The tax calculation loop and PDF rendering consume real CPU time. Unlike the sync worker where the event loop is mostly idle, the tax report worker genuinely stresses the CPU during the calculation and rendering phase.

```
RESOURCE PROFILE COMPARISON

transaction-sync job:
  Event loop: ─────────────────────────────────►
  CPU:        ▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
              (mostly idle — waiting for I/O)

tax-report job:
  Event loop: ─────────────────────────────────►
  CPU:        ▓▓▓▓▓▓▓░░░░▓▓▓▓▓▓▓░░░░▓▓▓▓▓▓▓░░░
              (active during calculation and
               PDF rendering phases)

At concurrency 5:
  5 jobs × moderate CPU = ~60-70% CPU on 2-vCPU container
  Headroom: 30-40% for Redis communication, process management

At concurrency 10 (tried and reverted):
  10 jobs × moderate CPU = ~90%+ CPU → response time degradation
  PostgreSQL also becomes contention point
```

This is why the tax-report worker container gets **2 vCPU** while the transaction-sync worker gets 1 vCPU. The heavier CPU profile needs the extra core.

**Why only 2 retry attempts with fixed 1-minute delay:**

Tax reports fail for different reasons than sync jobs. A sync job fails because GoCardless returned 429 — a transient network problem, worth retrying quickly with exponential backoff. A tax report fails because of a data issue (missing holding records, edge case in a country's tax calculation) or a PDF rendering error. Retrying immediately doesn't help — the data issue needs investigation. Two attempts with 1 minute between them is enough to rule out transient failures (S3 blip, momentary PostgreSQL overload). After that, the job goes to failed and an engineer looks at it.

**Idempotency:**

The `jobId: tax-report-{userId}-{year}` pattern means if the entire job batch is replayed (which happened in year 2 after a Redis incident), the second run overwrites the S3 file and updates the PostgreSQL record. Same end state as a single successful run. No duplicate reports, no stale data.

---

## Queue 4 — outbox-publisher

### What It Does

This queue implements the Outbox pattern for reliable event publishing to RabbitMQ. When Core Product needs to publish a domain event — for example `budget.threshold.exceeded` — it writes the event to an `outbox_events` table in PostgreSQL in the same transaction as the business data. A separate BullMQ worker polls that table every 5 seconds and publishes pending events to RabbitMQ.

```
THE OUTBOX PROBLEM IT SOLVES

Without Outbox (direct publish):

  BEGIN TRANSACTION
    INSERT INTO transactions (...)   ← writes to PostgreSQL
    INSERT INTO budgets (...)        ← updates budget spent
  COMMIT
        │
        ▼ (separate operation, NOT in transaction)
  rabbitMQ.publish('budget.threshold.exceeded')
        │
  What if this fails? What if RabbitMQ is down?
  → Transactions exist in PostgreSQL
  → Event was never published
  → User never gets notified
  → Inconsistency between data and notifications — permanently


With Outbox (write to DB first):

  BEGIN TRANSACTION
    INSERT INTO transactions (...)          ← business data
    INSERT INTO budgets (...)               ← budget update
    INSERT INTO outbox_events (             ← event record
      eventType = 'budget.threshold.exceeded',
      payload   = { userId, category, ... },
      status    = 'PENDING'
    )
  COMMIT
  ← All three writes are atomic. Either all succeed or none do.

  BullMQ outbox-publisher worker (every 5 seconds):
    SELECT * FROM outbox_events WHERE status = 'PENDING'
    → publishes each to RabbitMQ
    → marks each as 'PUBLISHED'

  Result:
  → If RabbitMQ was down, events accumulate in outbox_events
  → When RabbitMQ recovers, they get published
  → No events ever lost
```

```typescript
// outbox-publisher.worker.ts
@Processor('outbox-publisher', {
  concurrency: 1,  // single-threaded — order matters
})
export class OutboxPublisherWorker extends WorkerHost {

  async process(job: Job): Promise<void> {
    const pendingEvents = await this.prisma.outboxEvent.findMany({
      where: { status: 'PENDING' },
      orderBy: { createdAt: 'asc' },   // always publish in order written
      take: 100,                        // process in batches of 100
    })

    for (const event of pendingEvents) {
      await this.rabbitMQChannel.publish(
        this.getExchange(event.eventType),
        event.eventType,
        Buffer.from(JSON.stringify(event.payload)),
        {
          persistent: true,
          messageId: event.id,  // idempotency — if published twice,
                                // Notification Service deduplicates by messageId
        }
      )

      await this.prisma.outboxEvent.update({
        where: { id: event.id },
        data: { status: 'PUBLISHED', publishedAt: new Date() }
      })
    }
  }
}
```

The repeatable job that fires this worker every 5 seconds:

```typescript
// registered once on NestJS module init
await this.outboxQueue.add(
  'POLL_OUTBOX',
  {},
  {
    repeat: { every: 5_000 },
    jobId: 'outbox-publisher-recurring',   // singleton — only one ever exists
    removeOnComplete: { count: 1 },        // only keep the last completed run
  }
)
```

### Why concurrency 1

Events must be published to RabbitMQ in the exact order they were written to the outbox table. This is essential for correctness — if `user.registered` is published after `user.verified`, Notification Service may send a verification email to a user whose account was already activated.

Concurrency 1 means only one outbox publisher job runs at any point. It reads pending events in `createdAt` ascending order and publishes them sequentially. Ordering is guaranteed.

**Why BullMQ and not a plain `setInterval`:**

A `setInterval` inside a Node.js process runs in that process's memory:

```
WITHOUT BULLMQ (setInterval approach):

Core Product API Container 1:
  setInterval(() => pollOutbox(), 5000)
  ← polls every 5 seconds

Core Product API Container 2 (scaled out):
  setInterval(() => pollOutbox(), 5000)
  ← ALSO polls every 5 seconds

Result:
  Both containers read the same PENDING events
  Both publish them to RabbitMQ
  Notification Service receives duplicate events
  User gets two push notifications, two emails

WITH BULLMQ (repeatable job approach):

  jobId: 'outbox-publisher-recurring' exists ONCE in Redis
  Only ONE worker container picks it up at any time
  (LMOVE is atomic — two workers cannot pick the same job)
  No duplicates regardless of how many containers are running
```

This is exactly the kind of problem BullMQ solves that `setInterval` cannot — coordination across multiple container instances without any inter-process communication.

---

## BullMQ vs RabbitMQ — The Definitive Answer for FinVerse

Every interviewer who sees both BullMQ and RabbitMQ on your resume will ask: "You have both. When did you use which, and why not just use one?"

The answer requires understanding that they solve completely different problems:

```
┌─────────────────────────────────────────────────────────────────────┐
│                   BULLMQ vs RABBITMQ AT FINVERSE                    │
├─────────────────────────────────────┬───────────────────────────────┤
│  BULLMQ                             │  RABBITMQ                     │
├─────────────────────────────────────┼───────────────────────────────┤
│  What it is:                        │  What it is:                  │
│  A job processing system            │  A message delivery system    │
│  within a single service            │  between different services   │
├─────────────────────────────────────┼───────────────────────────────┤
│  Used for:                          │  Used for:                    │
│  Work that is too slow,             │  Domain events that multiple  │
│  too risky, or too complex          │  services need to react to    │
│  to run inside an HTTP handler      │  independently                │
├─────────────────────────────────────┼───────────────────────────────┤
│  Core Product examples:             │  Core Product examples:       │
│  - Transaction sync (30s+)          │  - budget.threshold.exceeded  │
│  - Budget checks (post-sync)        │    → Notification Service     │
│  - Tax report generation            │  - investment.order.paid      │
│  - Outbox event polling             │    → Core Product (portfolio) │
│  - Investment order processing      │  - user.registered            │
│                                     │    → Notification Service     │
├─────────────────────────────────────┼───────────────────────────────┤
│  What it gives you that             │  What it gives you that       │
│  RabbitMQ doesn't:                  │  BullMQ doesn't:              │
│  ✓ Per-job state tracking           │  ✓ Fan-out to multiple        │
│  ✓ Retries with backoff             │    independent consumers      │
│  ✓ Delayed execution                │  ✓ Topic routing              │
│  ✓ Recurring job scheduling         │  ✓ Publisher unaware of       │
│  ✓ Priority ordering                │    who consumes               │
│  ✓ Job deduplication                │  ✓ Consumer-side isolation    │
│  ✓ Concurrency control              │    (one consumer failure      │
│  ✓ Per-job observability            │    doesn't affect another)    │
│    (Bull Board — payload,           │                               │
│     errors, stack traces)           │                               │
│  ✓ Rate limiting                    │                               │
├─────────────────────────────────────┼───────────────────────────────┤
│  Backend:                           │  Backend:                     │
│  Redis (ElastiCache)                │  AMQP broker (Amazon MQ)      │
└─────────────────────────────────────┴───────────────────────────────┘
```

**The one-sentence answer:**

"RabbitMQ is how Core Product tells other services that something happened. BullMQ is how Core Product manages its own heavy work that can't run in an HTTP handler."

**Why not use RabbitMQ alone for everything?**

You could publish a `sync.requested` message to RabbitMQ and have a consumer service handle it. But that consumer service would need to implement job state tracking, retries with backoff, delayed execution, recurring scheduling, priority ordering, and deduplication — all from scratch. BullMQ gives you all of that built in. Additionally, RabbitMQ has no concept of "how many workers are currently processing this type of message" — you'd have no visibility into what is actively running, what failed, or why.

**Why not use BullMQ alone for everything?**

When `budget.threshold.exceeded` fires, both Notification Service and Analytics Service need to react — independently, without one blocking the other. BullMQ has no concept of multiple independent consumers for the same job. RabbitMQ's topic exchange delivers the event to both consumers simultaneously with a single publish call from Core Product. If you tried to replicate this with BullMQ, you'd need to manually enqueue two separate jobs — one for each consumer — which means Core Product now has to know about every service that cares about budget alerts. That coupling is exactly what the event-driven architecture is designed to avoid.

---

## STAR Format Stories — Core Product Service

These are the stories you tell when an interviewer asks "tell me about a challenging technical problem you solved."

---

### Story 1 — The Sync Timeout Incident

**Situation:**

Three months into my contract, I started seeing a pattern in Datadog: `INITIAL_SYNC` jobs for users with 4+ bank accounts were failing with stalled job errors at a rate of roughly 12%. The jobs would start, run for about 28 seconds, and then BullMQ's stalled check would find the lock had expired — the lock TTL was 30 seconds and the renewal interval was 15 seconds, so for a job that crossed the 30-second mark, there was a window where the lock could expire before the next renewal fired. The job would be requeued, stall again, and after `maxStalledCount: 2`, move to failed permanently. Users with multiple bank accounts were seeing their initial sync silently fail with no indication of what went wrong.

**Task:**

Diagnose the root cause and fix it so large-account syncs complete reliably without manual replays.

**Action:**

I traced through the job processing flow in Datadog APM. The pattern was consistent: jobs for users with 4 accounts were taking 28–34 seconds. The sync processed all accounts sequentially in a single loop — GoCardless calls, deduplication, bulk insert — without any checkpoints. The lock renewal at T=15s was fine. But for a job that took 32 seconds, the renewal at T=30s sometimes didn't fire in time because the event loop was briefly busy during a bulk insert.

I made three changes:

```typescript
// Change 1: Extend lock duration and set renewal to halfway point
@Processor('transaction-sync', {
  concurrency: 10,
  lockDuration: 60_000,     // extended from default 30s to 60s
  lockRenewTime: 30_000,    // renew at 30s — halfway through lock duration
})

// Change 2: Add progress updates between accounts
// job.updateProgress() also signals to BullMQ that the job
// is alive, which triggers lock renewal
for (let i = 0; i < accountIds.length; i++) {
  await job.updateProgress({
    current: i + 1,
    total: accountIds.length,
    currentAccount: accountIds[i],
  })

  // GoCardless call + bulk insert for this account
  // ...
}

// Change 3: Per-account timeout using Promise.race()
// A single slow GoCardless response can't block the entire sync
const transactions = await Promise.race([
  this.goCardlessService.fetchAllTransactions(accountId),
  new Promise((_, reject) =>
    setTimeout(() => reject(new Error('GoCardless timeout after 25s')), 25_000)
  )
])
```

The `job.updateProgress()` call between accounts was the key insight — BullMQ triggers lock renewal on progress updates, so even if the event loop is briefly occupied during a bulk insert, the lock is renewed as soon as we reach the next progress checkpoint.

I also wrote a one-off admin endpoint (reviewed and deployed by Lucas) that replayed the ~400 failed jobs from the DLQ, filtering specifically for the stalled-job failure reason.

**Result:**

Stalled job rate for large-account users dropped from 12% to under 0.3% within 24 hours of deployment. The 400 failed jobs were replayed successfully. Users with 4+ bank accounts now see their initial sync complete reliably within 45–60 seconds, with real-time per-account progress visible in the app through Bull Board's progress indicator.

---

### Story 2 — The GoCardless Rate Limit Storm

**Situation:**

During month 6, FinVerse ran a marketing campaign that brought 8,000 new users over 48 hours. Each user connected their bank account, triggering an `INITIAL_SYNC` job. The `transaction-sync` queue backed up to 8,000 jobs. ECS Auto Scaling saw the queue depth spike and launched 8 worker containers (from the usual 1). Without a rate limiter, all 8 containers began processing simultaneously — 80 concurrent jobs, each making multiple GoCardless API calls. Within 2 minutes, GoCardless started returning 429s for all requests.

At this point, the retry storm began. Jobs failing with 429 were retrying with exponential backoff — but the backoff wasn't long enough. With 80 concurrent jobs all retrying on similar schedules, each retry wave hit GoCardless with another burst of calls, which caused another wave of 429s, which caused more retries. The queue was not draining — it was cycling.

**Task:**

Stop the retry storm immediately, restore normal sync operation, and add safeguards so this can never happen again.

**Action:**

Immediate response: I paused the `transaction-sync` queue from Bull Board. This is a built-in BullMQ feature — pausing a queue stops workers from picking up new jobs without losing any data. Within 3 minutes of pausing, GoCardless 429s stopped completely.

I then deployed two changes before resuming:

```typescript
// Change 1: Add worker-level rate limiter
@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,           // max 30 jobs per 10-second window per container
    duration: 10_000,
  },
})
// With 8 containers: 8 × 30 = 240 jobs / 10s = 24 jobs/s
// Each job makes ~3 GoCardless calls = 72 calls/s
// GoCardless limit: 50 calls/s → still over limit
// Reduced to 3 containers max in Auto Scaling config
// 3 × 30 = 90 jobs/10s → ~27 calls/s → well within limit


// Change 2: Error-type-aware backoff
// GoCardless 429s need longer recovery time than generic errors
settings: {
  backoffStrategy: (attemptsMade: number, type: string, err: Error) => {
    if (err.message.includes('429') || err.message.includes('rate limit')) {
      // Minimum 30 seconds for rate limit errors
      // Plus jitter to prevent synchronized retries
      const minDelay = 30_000
      const jitter = Math.random() * 30_000
      return minDelay + jitter
    }
    // Standard exponential backoff for other errors
    return 5000 * Math.pow(2, attemptsMade)
  }
}
```

After deploying, I resumed the queue from Bull Board. The 8,000 jobs drained over approximately 6 hours with zero additional 429s.

I also updated the ECS Auto Scaling rule — the maximum container count for the transaction-sync worker was reduced from 10 to 3. Three containers at concurrency 10 with the rate limiter gives FinVerse 270 GoCardless calls per 10 seconds maximum — within the API limits while still processing the queue significantly faster than a single container.

**Result:**

The queue drained cleanly. The system handled a follow-up 5,000-user campaign two months later with zero manual intervention. GoCardless error rate during that campaign stayed below 0.1%. The combination of the worker-level rate limiter, error-aware jitter backoff, and a realistic Auto Scaling ceiling made the sync pipeline genuinely resilient.

---

### Story 3 — The Notification Race Condition

**Situation:**

In month 8, Lucas flagged a user complaint pattern he had noticed across support tickets: users were receiving budget alert push notifications before the transactions that triggered the alert appeared in their transaction list. The notification would arrive saying "You've spent 94% of your Dining Out budget", the user would open the app, and the transaction list would show 91% — or sometimes the new transactions wouldn't appear at all for several seconds.

This was intermittent — roughly 5% of budget alerts had this timing issue. Small enough to be easy to dismiss, but the product team considered it a trust-eroding experience.

**Task:**

Understand the race condition and eliminate it completely.

**Action:**

I traced the full event flow in Datadog using distributed trace IDs. The sequence was:

```
transaction-sync worker:
  T=0:   bulk INSERT transactions into PostgreSQL
  T=50ms: PostgreSQL commit acknowledged
  T=51ms: rabbitMQ.publish('budget.threshold.exceeded')  ← direct publish

RabbitMQ delivers event to Notification Service in ~5ms

Notification Service:
  T=56ms: receives event
  T=57ms: sends push notification → user's phone

User opens app:
  T=2000ms: GET /v1/transactions
  Core Product API reads from PostgreSQL
  → Sometimes returns data without the new transactions
    (PostgreSQL read replica lag — the replica hadn't
     caught up with the primary's commit yet)
```

The direct RabbitMQ publish was happening 50 milliseconds after the PostgreSQL commit — fast enough that the read replica (used for GET queries) sometimes hadn't replicated the new rows yet.

The fix was to move all event publishing into the Outbox pattern. Instead of publishing directly to RabbitMQ after the bulk insert, the transaction-sync worker writes the event to the `outbox_events` table in the **same PostgreSQL transaction** as the bulk insert:

```typescript
// BEFORE: direct publish after insert
await this.transactionService.bulkInsert(userId, accountId, transactions)
await this.rabbitMQChannel.publish('finance.events', 'budget.threshold.exceeded', ...)

// AFTER: event written atomically with business data
await this.prisma.$transaction([
  this.prisma.transaction.createMany({ data: newTransactions }),
  this.prisma.budget.update({
    where: { id: budget.id },
    data: { spent: newSpentAmount }
  }),
  this.prisma.outboxEvent.create({
    data: {
      eventType: 'budget.threshold.exceeded',
      payload: { userId, category, spent, limit }
    }
  })
])
```

The outbox-publisher worker polls every 5 seconds. It only publishes events after they are committed to PostgreSQL. By the time the event reaches RabbitMQ and Notification Service fires the push notification, the transactions have been in PostgreSQL for at least 5 seconds — more than enough time for the read replica to catch up.

This change was also the moment the `outbox-publisher` queue was properly established as a first-class concern. Before this, the outbox polling was a less disciplined `setInterval` inside the API container. I worked with Lucas to formalise it as a BullMQ repeatable job with a singleton `jobId`, which also fixed the duplicate-publish problem that would occur when the API container scaled to multiple instances.

**Result:**

The race condition was eliminated. Zero reported cases of notification timing mismatch after the fix deployed. The Outbox pattern also gave us a natural audit trail — every published event is permanently recorded in `outbox_events` with its status and published timestamp, which proved useful for debugging notification delivery issues in subsequent months.

---

## Chapter 8 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 8 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  4 queues in Core Product Service:                              │
│                                                                 │
│  transaction-sync (concurrency 10, rate limited)                │
│  → long-running I/O work, GoCardless + PostgreSQL               │
│  → INITIAL_SYNC (priority 1) and PERIODIC_SYNC (priority 2)     │
│  → your ownership end-to-end                                    │
│                                                                 │
│  budget-check (concurrency 20, lightweight)                     │
│  → extracted from sync to prevent lock expiry                   │
│  → fast: 2 DB queries + comparison + conditional publish        │
│                                                                 │
│  tax-report-gen (concurrency 5, CPU-heavy, 2 vCPU container)    │
│  → annual run, idempotent via deterministic jobId               │
│  → conservative retry (data issues need investigation)          │
│                                                                 │
│  outbox-publisher (concurrency 1, ordering enforced)            │
│  → Outbox pattern for reliable RabbitMQ publishing              │
│  → singleton job — BullMQ coordinates across containers         │
│  → fixed the notification race condition                        │
│                                                                 │
│  BullMQ = job lifecycle within Core Product                     │
│  RabbitMQ = event delivery to other services                    │
│  They solve different problems. You need both.                  │
│                                                                 │
│  3 production stories:                                          │
│  1. Sync timeout → extended lock TTL + progress checkpoints     │
│  2. GoCardless rate storm → worker limiter + jitter backoff     │
│  3. Notification race → Outbox pattern + atomic writes          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 8 done. Ready for Chapter 9 — Worker Thread Deep Dive — whenever you are.