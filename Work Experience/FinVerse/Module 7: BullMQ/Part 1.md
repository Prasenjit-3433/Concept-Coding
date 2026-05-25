Let me start writing Step 7: BullMQ now.# FinVerse — Step 7: BullMQ

---

## Part 1 — Why BullMQ Exists: The Node.js Reality First

Before understanding BullMQ, you need to understand the problem it solves. And that problem is rooted in how Node.js actually works.

---

### How Node.js Actually Works (No Buzzwords)

You are right about everything you said. Let me make it concrete with a diagram.

```
YOUR NODE.JS PROCESS
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   ONE THREAD — the Event Loop                               │
│                                                             │
│   ┌──────────────────────────────────────────────────┐      │
│   │  Call Stack (your JavaScript code runs here)     │      │
│   │                                                  │      │
│   │  handleRequest() → syncTransactions() → ...      │      │
│   └──────────────────────────────────────────────────┘      │
│                                                             │
│   ┌──────────────────────────────────────────────────┐      │
│   │  Event Queue (callbacks waiting to run)          │      │
│   │                                                  │      │
│   │  [onHttpResponse] [onDbResult] [onFileRead] ...  │      │
│   └──────────────────────────────────────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
          │                           ▲
          │ "go do this I/O"          │ "I/O done, here's result"
          ▼                           │
┌─────────────────────────────────────────────────────────────┐
│         OS / libuv (NOT your JavaScript thread)             │
│                                                             │
│   Actual network calls, file reads, DB queries              │
│   run here — in C, managed by the OS                        │
│                                                             │
│   GoCardless API call ──► OS handles it concurrently        │
│   PostgreSQL query    ──► OS handles it concurrently        │
│   Redis read          ──► OS handles it concurrently        │
└─────────────────────────────────────────────────────────────┘
```

**What this means in plain English:**

Your JavaScript code runs on one thread. When it hits an I/O operation — a database query, an HTTP call to GoCardless, a Redis read — it does NOT sit and wait. It hands the task to the OS (via libuv, Node.js's internal C library) and immediately goes back to the event loop to process the next thing. When the OS completes the I/O, it places a callback in the Event Queue. The event loop picks it up when the call stack is empty.

This is why Node.js handles thousands of concurrent HTTP requests without breaking — it is not doing them in parallel (no multiple threads), it is doing them concurrently (one thread, never sitting idle, always processing the next thing while I/O is in flight).

**The part that breaks this model:**

CPU-bound work. If your JavaScript code is doing heavy computation — tight loops, complex calculations — it does NOT yield to the event loop. It sits on the call stack and blocks everything else.

```
Event Loop Thread — what happens with CPU-bound work:

Time ──────────────────────────────────────────────────────►

[Handle Request A] [HEAVY CPU LOOP.......BLOCKS EVERYTHING...]
                                         ▲
                              During this entire time:
                              - Request B is waiting
                              - Request C is waiting  
                              - Redis callbacks are waiting
                              - EVERYTHING is frozen
```

**The multi-core problem:**

Your server has 4, 8, or 16 CPU cores. One Node.js process uses exactly ONE of them. The other cores sit idle. You are paying for hardware you are not using.

**How Node.js solves the multi-core problem:**

You run multiple Node.js processes — one per CPU core. This is what PM2's cluster mode does, and what Kubernetes does when it runs multiple container replicas. You achieve parallelism through multiple processes, not multiple threads within one process.

```
8-core server running Core Product Service:

┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Process 1│ │ Process 2│ │ Process 3│ │ Process 4│
│ Core 1   │ │ Core 2   │ │ Core 3   │ │ Core 4   │
│ Port 3001│ │ Port 3002│ │ Port 3003│ │ Port 3004│
└──────────┘ └──────────┘ └──────────┘ └──────────┘
      ▲             ▲             ▲             ▲
      └─────────────┴─────────────┴─────────────┘
                          │
                    Load Balancer
                    (distributes
                     HTTP requests)
```

Each process is completely independent — its own memory, its own event loop, its own call stack. They share no state.

---

### So Where Does BullMQ Fit?

Now that you understand the above, here is the problem BullMQ solves:

**Problem 1 — You cannot do long-running work inside an HTTP request handler.**

When a user connects their bank account, FinVerse needs to call GoCardless for each account and fetch potentially years of transactions. This might take 20 seconds. You cannot hold the HTTP connection open for 20 seconds — the user's app will time out, and during those 20 seconds, that Node.js process is doing nothing useful between I/O waits that it cannot hand off to something else.

**Problem 2 — You need scheduled, recurring work.**

Transaction sync needs to run every 4 hours per user. Investment orders need to run on the 1st of each month per user. Tax reports need to run on January 1st for every Premium user. These are not triggered by HTTP requests — they need to happen on a schedule, reliably, even if the server restarts.

**Problem 3 — You need controlled parallelism for CPU or I/O intensive batch work.**

Generating tax reports for 50,000 users simultaneously would hammer PostgreSQL. You need to say: "process 5 at a time, queue the rest."

**Problem 4 — You need retries and failure tracking.**

If a GoCardless call fails mid-sync, you need to retry it. If it fails 3 times, you need to know about it. In a plain HTTP handler, failed work is just lost.

**BullMQ is a job queue built on Redis that solves all four problems:**

```
WHAT BULLMQ GIVES YOU:

Core Product NestJS Process          Redis (BullMQ's brain)
(HTTP Handler)                        
      │                              ┌──────────────────────┐
      │  "I need to sync             │  transaction-sync    │
      │   transactions for           │  queue               │
      │   this user later"           │                      │
      ├──── enqueue job ────────────►│  [job1] [job2] [job3]│
      │                              │  [job4] [job5] ...   │
      │  Returns HTTP 200            └──────────────────────┘
      │  immediately                          │
      │  (user is not waiting)                │
                                             │
                              ┌──────────────▼──────────────┐
                              │    BullMQ Worker Process     │
                              │    (separate Node.js process)│
                              │                              │
                              │    Picks up jobs one by one  │
                              │    (or N at a time)          │
                              │    Retries on failure        │
                              │    Tracks success/failure    │
                              └──────────────────────────────┘
```

The HTTP handler and the worker are separate Node.js processes. The HTTP handler's job is done the moment it enqueues the job. The worker processes jobs in the background, at its own pace, independently.

---

## Part 2 — BullMQ Architecture: How It Works Internally

### The Three Roles

Every BullMQ setup has three parts:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Queue       │     │     Worker       │     │  QueueEvents    │
│                 │     │                 │     │  (optional)     │
│ - add() jobs    │     │ - processes()   │     │                 │
│ - pause/resume  │     │ - runs job fn   │     │ - listens to    │
│ - get job state │     │ - handles retry │     │   completed,    │
│ - drain/clean   │     │ - reports done  │     │   failed, etc.  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │     Redis     │
                         │               │
                         │  All state    │
                         │  lives here   │
                         └───────────────┘
```

**Queue** — the producer side. Your NestJS service code calls `queue.add()` to enqueue a job. The Queue class also lets you pause/resume processing, inspect job states, and clean old jobs.

**Worker** — the consumer side. You give it a function — your actual job processing logic. BullMQ calls your function with the job data. The Worker handles all the complexity: polling Redis, locking jobs, retrying on failure, marking completion.

**QueueEvents** — an event emitter that lets you listen to what's happening in the queue. In FinVerse, this is how Datadog receives job metrics — a QueueEvents listener emits to the OpenTelemetry collector on every job completion or failure.

---

### How Jobs Are Stored in Redis — The Key Structure

This is what makes BullMQ reliable. Everything is stored in Redis with a specific key structure. Understanding this structure is what lets you answer "what happens during a crash."

```
Redis Key Structure for queue named "transaction-sync":

bull:{queue-name}:              ← namespace prefix

bull:transaction-sync:id        ← auto-incrementing job ID counter

bull:transaction-sync:wait      ← LIST  — jobs waiting to be picked up
bull:transaction-sync:active    ← LIST  — jobs currently being processed
bull:transaction-sync:completed ← ZSET  — finished jobs (sorted by timestamp)
bull:transaction-sync:failed    ← ZSET  — failed jobs (sorted by timestamp)
bull:transaction-sync:delayed   ← ZSET  — future jobs (sorted by run timestamp)
bull:transaction-sync:paused    ← LIST  — jobs when queue is paused

bull:transaction-sync:{jobId}   ← HASH  — individual job data:
                                          { data, opts, timestamp,
                                            processedOn, finishedOn,
                                            returnValue, failedReason,
                                            attemptsMade, stacktrace }
```

**The lifecycle of a job through these data structures:**

```
add() called
     │
     ▼
Job data written to hash: bull:transaction-sync:{jobId}
Job ID pushed to:         bull:transaction-sync:wait  (LIST)

     │
     ▼ Worker polls Redis

Worker atomically moves job ID:
FROM: bull:transaction-sync:wait   (LIST)
TO:   bull:transaction-sync:active (LIST)
using LMOVE command (atomic, no race condition)

     │
     ▼ Your job function runs

Success:
  Job ID moved FROM active TO completed (ZSET)
  Return value stored in job hash

Failure (after all retries exhausted):
  Job ID moved FROM active TO failed (ZSET)
  Error + stack trace stored in job hash
```

**Why this matters for crash safety:**

If the Worker process crashes while a job is in `active`, that job stays in `active` forever — it is never moved to `completed` or `failed`. This is the **stalled job** problem. BullMQ has a specific mechanism to handle this, covered in Part 4.

---

### The Lock Mechanism — How Workers Don't Step on Each Other

When you run multiple Worker processes (for horizontal scaling), two workers must never process the same job simultaneously. BullMQ uses Redis locks for this.

```
Worker picks up job from wait list
          │
          ▼
Worker sets a Redis lock:
  Key:   bull:transaction-sync:{jobId}:lock
  Value: {workerToken}  (unique random string per worker instance)
  TTL:   30 seconds     (lockDuration — configurable)

          │
          ▼
Worker runs your job function
          │
     ┌────┴─────────────────────────────────────────┐
     │                                              │
Job finishes < 30s                    Job takes > 30s
     │                                              │
Worker removes lock                   Lock expires!
Marks job complete                    BullMQ stalledCheck
                                      sees job in active
                                      with no lock
                                      → marks job as stalled
                                      → moves back to wait
                                      → another worker picks it up
```

**The lock renewal mechanism:**

For jobs that legitimately take longer than `lockDuration`, the Worker automatically extends the lock while the job is running. BullMQ does this by calling a lock renewal at half the `lockDuration` interval. So if `lockDuration=30s`, renewal fires at 15s, resetting the TTL back to 30s. As long as the worker is alive and running, the lock stays fresh.

If the worker crashes, it stops renewing. The lock expires. Another worker picks up the job. This is how BullMQ achieves **at-least-once processing** — a job will always be processed, even if the first worker dies mid-way.

---

## Part 3 — BullMQ in FinVerse: Every Queue, Every Job

Here is the complete picture of every BullMQ queue used across the system:

```
┌───────────────────────────────────────────────────────────────────┐
│              BULLMQ QUEUES IN FINVERSE                            │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  SERVICE: Core Product                                            │
│                                                                   │
│  Queue: transaction-sync                                          │
│  Jobs:  INITIAL_SYNC, PERIODIC_SYNC, MANUAL_SYNC                  │
│  Used:  After bank connect, every 4hrs per user, user-triggered   │
│  Concurrency: 10 (10 users synced in parallel)                   │
│                                                                   │
│  Queue: budget-check                                              │
│  Jobs:  CHECK_BUDGET_THRESHOLD                                    │
│  Used:  After each transaction sync batch completes               │
│  Concurrency: 20 (lightweight check, high parallelism ok)        │
│                                                                   │
│  Queue: tax-report-generation                                     │
│  Jobs:  GENERATE_ANNUAL_TAX_REPORT                                │
│  Used:  January 1st each year, per Premium user                  │
│  Concurrency: 5  (heavy DB read — strictly throttled)            │
│                                                                   │
│  Queue: outbox-publisher                                          │
│  Jobs:  PUBLISH_OUTBOX_EVENT                                      │
│  Used:  Polls outbox table, publishes to RabbitMQ                 │
│  Concurrency: 1  (single publisher, order matters)               │
│                                                                   │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  SERVICE: Payment Service                                         │
│                                                                   │
│  Queue: investment-orders                                         │
│  Jobs:  MONTHLY_INVESTMENT_ORDER                                  │
│  Used:  Each user's scheduled investment date                     │
│  Concurrency: 10                                                  │
│                                                                   │
│  Queue: subscription-billing                                      │
│  Jobs:  RETRY_FAILED_PAYMENT                                      │
│  Used:  Stripe webhook triggers retry on payment failure          │
│  Concurrency: 5                                                   │
│                                                                   │
│  Queue: reconciliation                                            │
│  Jobs:  RECONCILE_INVESTMENT_ORDERS                               │
│  Used:  Hourly — finds paid orders with no portfolio update       │
│  Concurrency: 1                                                   │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

### How BullMQ Is Set Up in NestJS

```typescript
// app.module.ts — registering BullMQ
import { BullModule } from '@nestjs/bullmq'

@Module({
  imports: [
    BullModule.forRoot({
      connection: {
        host: process.env.REDIS_HOST,
        port: 6379,
        password: process.env.REDIS_PASSWORD,
        tls: process.env.NODE_ENV === 'production' ? {} : undefined,
      },
      defaultJobOptions: {
        attempts: 3,               // retry up to 3 times on failure
        backoff: {
          type: 'exponential',
          delay: 5000,             // 5s, 10s, 20s between retries
        },
        removeOnComplete: {
          age: 86400,              // keep completed jobs for 24 hours
          count: 1000,             // keep last 1000 completed jobs
        },
        removeOnFail: {
          age: 7 * 86400,          // keep failed jobs for 7 days
        },
      },
    }),

    BullModule.registerQueue(
      { name: 'transaction-sync' },
      { name: 'budget-check' },
      { name: 'tax-report-generation' },
      { name: 'outbox-publisher' },
    ),
  ],
})
export class AppModule {}
```

```typescript
// transaction-sync.producer.ts — adding jobs to the queue
@Injectable()
export class TransactionSyncProducer {
  constructor(
    @InjectQueue('transaction-sync')
    private readonly syncQueue: Queue
  ) {}

  // Called immediately after bank account connection completes
  async enqueueInitialSync(userId: string, accountIds: string[]) {
    await this.syncQueue.add(
      'INITIAL_SYNC',              // job name — used to route to correct processor
      { userId, accountIds },      // job data — available inside the worker
      {
        jobId: `initial-sync-${userId}`,  // deterministic ID — prevents duplicate jobs
                                          // if this is called twice for same user,
                                          // second call is a no-op
        priority: 1,               // 1 = highest priority (lower number = higher)
      }
    )
  }

  // Called by the scheduler every 4 hours per active user
  async enqueuePeriodicSync(userId: string, accountIds: string[]) {
    await this.syncQueue.add(
      'PERIODIC_SYNC',
      { userId, accountIds },
      {
        jobId: `periodic-sync-${userId}-${Date.now()}`,
      }
    )
  }
}
```

```typescript
// transaction-sync.worker.ts — processing jobs
@Processor('transaction-sync', {
  concurrency: 10,   // process up to 10 jobs simultaneously within this worker
  limiter: {
    max: 50,         // max 50 jobs processed per duration
    duration: 10000, // per 10 seconds — rate limiting at the worker level
  }
})
export class TransactionSyncWorker extends WorkerHost {

  constructor(
    private readonly goCardlessService: GoCardlessService,
    private readonly transactionService: TransactionService,
  ) {
    super()
  }

  // BullMQ calls this method with the job
  async process(job: Job): Promise<void> {

    // Route to correct handler based on job name
    switch (job.name) {
      case 'INITIAL_SYNC':
        return this.handleInitialSync(job)
      case 'PERIODIC_SYNC':
        return this.handlePeriodicSync(job)
      default:
        throw new Error(`Unknown job name: ${job.name}`)
    }
  }

  private async handleInitialSync(job: Job): Promise<void> {
    const { userId, accountIds } = job.data

    for (const accountId of accountIds) {

      // Update job progress — visible in Bull Board dashboard
      await job.updateProgress({
        current: accountIds.indexOf(accountId) + 1,
        total: accountIds.length,
        accountId
      })

      const transactions = await this.goCardlessService.fetchAllTransactions(accountId)
      await this.transactionService.bulkInsert(userId, accountId, transactions)
    }
  }
}
```

---

## Part 4 — The Battlefield Questions

Now every question you listed, answered for FinVerse's exact setup.

---

### "What happens when the Worker crashes mid-job?"

```
Timeline:

T=0s    Worker picks up INITIAL_SYNC job for userId=usr_123
        Job moves: wait → active
        Lock set:  bull:transaction-sync:job_456:lock  TTL=30s

T=5s    Worker calls GoCardless for account 1 — success
T=10s   Worker calls GoCardless for account 2 — success  
T=14s   BullMQ auto-renews lock (at 15s = half of 30s TTL)
        Lock TTL reset to 30s

T=18s   Worker CRASHES (OOM, SIGKILL, server dies)
        Lock renewal stops
        Job stays in: active list
        Lock expires at T=48s (18s + 30s TTL)

T=48s   Lock key disappears from Redis

T=60s   BullMQ stalledCheck fires (runs every stalledInterval, default 30s)
        Checks: are there jobs in active with no lock?
        Finds: job_456 in active, no lock
        → Marks job as STALLED
        → Moves job back to: wait list
        → Increments job.stalledCounter

T=61s   Another Worker picks up job_456
        Runs INITIAL_SYNC from the beginning for usr_123
```

**The consequence:** the job runs again from the start. This is **at-least-once processing** — you are guaranteed the job will complete, but it might run more than once. This means your job handler must be **idempotent** — running it twice must produce the same result as running it once.

In the `INITIAL_SYNC` case: `bulkInsert` checks `externalId` uniqueness. Transactions already inserted from the first run are simply skipped as duplicates. The second run completes cleanly.

---

### "What is a Stalled Job and how does BullMQ handle it?"

A stalled job is exactly the scenario above — a job that is in the `active` list but whose lock has expired. The Worker holding it either crashed or got stuck.

```
Configuration:
new Worker('transaction-sync', processor, {
  stalledInterval: 30000,   // check for stalled jobs every 30s
  maxStalledCount: 2,        // if a job stalls more than 2 times,
                             // move it to failed (stop trying)
})
```

`maxStalledCount` is critical. Without it, a job that consistently crashes the Worker would loop forever: stall → retry → crash → stall → retry → crash. Setting `maxStalledCount: 2` means after 2 stalls, the job is moved to `failed` and the team gets alerted.

---

### "What happens when Redis goes down?"

This is the most important reliability question for BullMQ. Redis is BullMQ's entire backbone — queue state, job data, locks — all in Redis. If Redis goes down:

```
Redis DOWN scenario:

HTTP Handler tries to add job:
  queue.add() → tries to connect to Redis → connection refused
  → throws error
  → HTTP request fails with 500

Worker tries to process:
  Worker loses Redis connection
  → stops processing immediately
  → BullMQ enters reconnection loop
  → no jobs processed until Redis recovers

When Redis comes back:
  BullMQ auto-reconnects (ioredis handles this)
  Jobs that were in wait/delayed → still there, processing resumes
  Jobs that were in active → checked for stalledInterval → either
    still locked (Worker recovered quickly) → continues processing
    lock expired → stall handling kicks in → requeued
```

**The gaps this creates:**

Jobs that were supposed to be enqueued while Redis was down were never enqueued. Unlike RabbitMQ (where the Outbox pattern saves you), BullMQ has no built-in persistence beyond Redis. If Redis is down during a transaction sync trigger, that sync simply does not happen.

**How FinVerse handles this:**

For periodic syncs, the recurring job fires again in 4 hours anyway — a missed trigger is self-healing. For initial syncs triggered by bank connection (which must not be lost), the Accounts module stores a `syncStatus: PENDING` on the account record. A reconciliation BullMQ job runs hourly and queries for accounts with `syncStatus: PENDING AND lastSyncedAt IS NULL` — if it finds any, it re-enqueues the initial sync. This is the application-level safety net for Redis downtime.

**AWS ElastiCache reliability:**

In production, FinVerse runs Redis on AWS ElastiCache with Multi-AZ replication. If the primary node fails, ElastiCache promotes a replica within ~30 seconds. During those 30 seconds, BullMQ's ioredis client retries the connection with exponential backoff and reconnects automatically when the new primary is ready.

---

### "What happens during duplicate job processing?"

There are two causes:

**Cause 1 — Stalled job recovery** (covered above): the job runs again from scratch. Idempotency handles it.

**Cause 2 — Duplicate enqueue**: two HTTP requests both trigger `enqueueInitialSync` for the same user. Without protection, two identical jobs sit in the queue and both run.

**Fix — deterministic job IDs:**

```typescript
await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId, accountIds },
  {
    jobId: `initial-sync-${userId}`
    // If a job with this ID already exists in wait or active,
    // BullMQ silently rejects the second add() — no duplicate
  }
)
```

When `jobId` is provided, BullMQ checks if a job with that ID already exists. If it does, the new `add()` call is ignored. For `INITIAL_SYNC`, using `userId` as part of the job ID means no matter how many times the connection flow fires for the same user, only one sync job runs.

For `PERIODIC_SYNC`, the job ID includes a timestamp — each sync run is intentionally a different job.

---

### "How do delayed jobs work in BullMQ?"

Delayed jobs are jobs that should not run immediately — they are scheduled for a specific time in the future.

```
How BullMQ stores delayed jobs:

bull:transaction-sync:delayed  ← Redis ZSET (sorted set)
                                  Member: jobId
                                  Score:  Unix timestamp of when to run

Example:
  Job to run at 14:00 tomorrow
  Score: 1735900000 (Unix timestamp)

BullMQ's internal scheduler checks the delayed ZSET:
  Every N milliseconds (default: 5000ms — configurable)
  Finds jobs where score ≤ current timestamp
  Moves them from delayed → wait
  Worker picks them up normally
```

**In FinVerse — the investment order use case:**

```typescript
// Schedule a user's monthly investment on their chosen date
async scheduleMonthlyInvestment(userId: string, investmentDayOfMonth: number) {
  const nextRunDate = getNextInvestmentDate(investmentDayOfMonth)
  const delay = nextRunDate.getTime() - Date.now()

  await this.investmentQueue.add(
    'MONTHLY_INVESTMENT_ORDER',
    { userId },
    {
      delay,           // milliseconds from now
      jobId: `investment-${userId}-${nextRunDate.toISOString().slice(0,7)}`,
      // job ID includes year-month — prevents duplicate for same month
    }
  )
}
```

After a job runs successfully, the worker enqueues the next month's job — a self-scheduling chain.

---

### "What are repeatable jobs and how do they work?"

For the 4-hour transaction sync, you do not want to manually enqueue the next job after each run. BullMQ has built-in support for repeatable jobs.

```typescript
// Add once — BullMQ schedules it to repeat forever
await this.syncQueue.add(
  'PERIODIC_SYNC',
  { userId },
  {
    repeat: {
      every: 4 * 60 * 60 * 1000,   // every 4 hours in milliseconds
      // OR use cron syntax:
      // pattern: '0 */4 * * *'
    },
    jobId: `periodic-sync-${userId}`,
  }
)
```

**How repeatable jobs are stored in Redis:**

```
bull:transaction-sync:repeat   ← ZSET

Each entry:
  Member: key:jobId:nextRunTimestamp
  Score:  nextRunTimestamp

BullMQ's scheduler:
  Checks this ZSET every 5 seconds
  For each due repeatable job:
    Creates a real job in the wait list
    Updates the ZSET entry with the NEXT run timestamp
```

**Important — repeatable jobs and Redis restart:**

Repeatable jobs are stored in Redis. If Redis loses all data (a full flush, not just a restart — ElastiCache replication handles normal restarts), all repeatable job registrations are lost. FinVerse addresses this with an `OnModuleInit` hook in NestJS that re-registers all repeatable jobs at startup if they are not already present:

```typescript
@Injectable()
export class SyncScheduler implements OnModuleInit {
  async onModuleInit() {
    const existingRepeatables = await this.syncQueue.getRepeatableJobs()
    const registered = existingRepeatables.map(j => j.key)

    // Re-register any missing periodic syncs for active users
    const activeUsers = await this.userService.getActiveUsersWithBankAccounts()

    for (const user of activeUsers) {
      const key = `periodic-sync-${user.id}`
      if (!registered.includes(key)) {
        await this.enqueuePeriodicSync(user.id, user.accountIds)
      }
    }
  }
}
```

---

### "How does retry work in BullMQ?"

```
Job fails (your processor function throws an error):

  Attempt 1 fails
       │
       ▼ BullMQ checks: attempts < maxAttempts (3)?
       │ YES
       ▼
  Calculate delay (exponential backoff):
    delay = initialDelay × 2^(attemptsMade)
    5000ms × 2^0 = 5000ms  (5 seconds)
       │
       ▼
  Job moved to: delayed list
  Score (run time) = now + 5000ms
  job.attemptsMade incremented to 1
       │
       ▼ After 5 seconds
  Job moves: delayed → wait
  Worker picks it up → Attempt 2

  Attempt 2 fails
    delay = 5000ms × 2^1 = 10000ms (10 seconds)
    Moved to delayed again, attemptsMade = 2

  Attempt 3 fails
    attempts (3) >= maxAttempts (3)
    Job moved to: failed list
    Error + stack trace stored in job hash
    QueueEvents emits 'failed' event
    Datadog alert fires
```

```typescript
// Configuring retries with exponential backoff
BullModule.forRoot({
  defaultJobOptions: {
    attempts: 3,
    backoff: {
      type: 'exponential',
      delay: 5000,   // base delay in ms
    },
  },
})

// Or override per job type:
await this.syncQueue.add('INITIAL_SYNC', data, {
  attempts: 5,          // more retries for initial sync — it's critical
  backoff: {
    type: 'exponential',
    delay: 10000,       // 10s, 20s, 40s, 80s, 160s
  },
})
```

---

### "How does priority work?"

BullMQ supports job priorities — lower number = higher priority (1 is highest).

```
wait list with priorities:

Priority 1: [INITIAL_SYNC for new user]    ← user is waiting, process first
Priority 2: [PERIODIC_SYNC for user A]
Priority 2: [PERIODIC_SYNC for user B]
Priority 3: [GENERATE_TAX_REPORT]          ← batch work, can wait

Worker always picks the highest priority job
(lowest priority number) from wait list
```

In FinVerse, initial syncs (triggered by a user actively waiting to see their accounts) are `priority: 1`. Background periodic syncs are `priority: 2`. Annual tax reports are `priority: 3` — they run overnight and can wait behind real user-triggered work.

---

### "What is rate limiting at the Worker level?"

This is different from API rate limiting. This is about controlling how fast your Worker sends requests to an external API — to avoid getting blocked.

GoCardless has API rate limits. If 500 users all trigger periodic sync at the same time, your Workers would fan out 500 × 3 concurrent GoCardless calls simultaneously — hitting the rate limit, getting 429 responses, triggering retries, making it worse.

```typescript
@Processor('transaction-sync', {
  concurrency: 10,    // max 10 jobs running simultaneously
  limiter: {
    max: 30,          // max 30 jobs per duration window
    duration: 10000,  // per 10 seconds
    // = max 3 jobs per second on average
    // even if 1000 jobs are waiting
  }
})
```

When the rate limit is hit, BullMQ does not fail the jobs. It pauses pickup from the queue until the window resets. Jobs stay safely in `wait` and are processed as capacity allows.

---

### "How do you handle exactly-once semantics?"

BullMQ guarantees **at-least-once** by default. Exactly-once requires you to implement idempotency in your job handler.

```
The challenge:

  Job runs → processes transactions → crashes before ack
  BullMQ retries → job runs again

  Without idempotency:
    Duplicate transactions inserted into PostgreSQL
    User sees their €20 coffee appear twice

  With idempotency (externalId deduplication):
    Second run checks externalId for each transaction
    Already-inserted transactions → skipped
    New transactions → inserted
    End result is identical to a single successful run
```

**Idempotency patterns used in FinVerse by job type:**

| Job | Idempotency Mechanism |
|---|---|
| INITIAL_SYNC / PERIODIC_SYNC | `externalId` unique constraint on transactions table |
| MONTHLY_INVESTMENT_ORDER | Deterministic `jobId` prevents duplicate enqueue; holding `@@unique([portfolioId, instrumentId])` prevents duplicate DB insert |
| GENERATE_TAX_REPORT | `@@unique([userId, taxYear])` on tax_reports — second run upserts |
| PUBLISH_OUTBOX_EVENT | Outbox record marked `PUBLISHED` after send; second run skips published records |

---

### "How do you handle memory pressure in Redis?"

BullMQ stores every job in Redis — data payload, result, error, stack trace. Without cleanup, Redis memory grows indefinitely.

**Two configurations that keep Redis lean:**

```typescript
defaultJobOptions: {
  removeOnComplete: {
    age: 86400,     // remove completed jobs older than 24 hours
    count: 1000,    // always keep last 1000 completed jobs
                    // (for Bull Board inspection — you need recent history)
  },
  removeOnFail: {
    age: 7 * 86400, // keep failed jobs for 7 days
                    // (engineers need time to investigate and replay)
  },
}
```

**Why keep failed jobs longer than completed jobs:**
A completed job has done its work — its data is no longer needed after a day. A failed job is a bug or an incident — you need the payload and stack trace for investigation and potential manual replay.

**Tax report job data is large** — the payload includes user IDs and year references, but the actual report is in S3. Job payloads in BullMQ should never contain large binary data or full document content. Always store the reference (S3 key, database ID) in the job payload, never the content itself.

---

### "How do you scale Workers?"

**Vertical scaling** — increase `concurrency` inside one Worker process:

```typescript
@Processor('transaction-sync', { concurrency: 20 })
// One process, handles 20 jobs simultaneously
// Effective for I/O-bound jobs (GoCardless calls, DB queries)
// because Node.js handles concurrent I/O efficiently
```

**Horizontal scaling** — run multiple Worker processes (Docker containers on ECS):

```
ECS Service: transaction-sync-worker

Container 1 (concurrency: 10) ──┐
Container 2 (concurrency: 10) ──┤──► Same Redis queue
Container 3 (concurrency: 10) ──┘    BullMQ distributes jobs across all

Total: 30 concurrent jobs processed simultaneously
```

All workers connect to the same Redis. BullMQ's lock mechanism (covered in Part 2) ensures two workers never process the same job. Adding or removing worker containers requires no code change — just ECS scaling rules.

**When to scale horizontally vs vertically:**

Scale concurrency (vertically) first — it is free and simple. Scale to multiple containers when a single process's CPU or memory becomes the bottleneck, or when you need fault isolation (one container crash does not stop all job processing).

---

### "How do you observe what's happening in your queues?"

**Bull Board** — a web UI that reads directly from Redis and shows queue health in real time.

```typescript
// Mounted inside NestJS as an Express middleware
import { createBullBoard } from '@bull-board/api'
import { BullMQAdapter } from '@bull-board/api/bullMQAdapter'
import { ExpressAdapter } from '@bull-board/express'

const serverAdapter = new ExpressAdapter()
serverAdapter.setBasePath('/admin/queues')

createBullBoard({
  queues: [
    new BullMQAdapter(transactionSyncQueue),
    new BullMQAdapter(taxReportQueue),
    new BullMQAdapter(investmentOrdersQueue),
  ],
  serverAdapter,
})

app.use('/admin/queues', serverAdapter.getRouter())
```

This gives the team a live dashboard showing:
- Jobs waiting / active / completed / failed per queue
- Job payload and error details for failed jobs
- Ability to manually retry failed jobs with one click
- Throughput metrics (jobs per minute)

Bull Board is secured behind `hasRole('ADMIN')` in the SecurityFilterChain — it is an internal tool, never exposed publicly.

**Datadog metrics via QueueEvents:**

```typescript
@Injectable()
export class QueueMetricsListener implements OnModuleInit {
  constructor(
    private readonly queueEvents: QueueEvents,
    private readonly metricsService: MetricsService
  ) {}

  onModuleInit() {
    this.queueEvents.on('completed', ({ jobId, returnvalue }) => {
      this.metricsService.increment('bullmq.job.completed', {
        queue: 'transaction-sync'
      })
    })

    this.queueEvents.on('failed', ({ jobId, failedReason }) => {
      this.metricsService.increment('bullmq.job.failed', {
        queue: 'transaction-sync',
        reason: failedReason
      })
      // Datadog alert fires if failed count > threshold in 5 min window
    })

    this.queueEvents.on('stalled', ({ jobId }) => {
      this.metricsService.increment('bullmq.job.stalled', {
        queue: 'transaction-sync'
      })
    })
  }
}
```

Key Datadog metrics monitored:
- `bullmq.queue.waiting` — jobs backed up (worker falling behind)
- `bullmq.job.failed` — failure rate
- `bullmq.job.stalled` — worker crash indicator
- `bullmq.job.duration` — processing time per job type
- `bullmq.queue.delayed` — scheduled backlog

---

## Part 5 — BullMQ vs RabbitMQ: Why Both?

This is a question you will definitely be asked. "You have both BullMQ and RabbitMQ in the system — why not just use one?"

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE FUNDAMENTAL DIFFERENCE                   │
├──────────────────────────┬──────────────────────────────────────┤
│        RabbitMQ          │             BullMQ                   │
├──────────────────────────┼──────────────────────────────────────┤
│ SERVICE-to-SERVICE       │ BACKGROUND JOB processing            │
│ communication            │ within a service                     │
├──────────────────────────┼──────────────────────────────────────┤
│ Core Product doesn't     │ Core Product needs to sync           │
│ know Notification Svc    │ transactions for a user              │
│ exists — it just         │ without blocking the HTTP            │
│ fires an event           │ request thread                       │
├──────────────────────────┼──────────────────────────────────────┤
│ Multiple consumers       │ Single consumer type                 │
│ (Core Product AND        │ (the Worker for that queue)          │
│  Notification Svc both   │                                      │
│  receive same event)     │                                      │
├──────────────────────────┼──────────────────────────────────────┤
│ No scheduling            │ Built-in: delay, repeat,             │
│                          │ cron, priority                       │
├──────────────────────────┼──────────────────────────────────────┤
│ Broker is external       │ Broker is Redis — already            │
│ infrastructure           │ in your stack for caching            │
│ (Amazon MQ)              │                                      │
├──────────────────────────┼──────────────────────────────────────┤
│ Survives Redis failure   │ Depends entirely on Redis            │
│ (uses its own storage)   │                                      │
└──────────────────────────┴──────────────────────────────────────┘
```

**The one-sentence answer for an interview:**

"RabbitMQ is for cross-service event-driven communication — one service telling another something happened. BullMQ is for background job processing within a service — deferring heavy or scheduled work off the request thread. They solve different problems. That is why we use both."

---

That is the complete Step 7 — BullMQ from Node.js fundamentals through to every battlefield question, with FinVerse's exact setup throughout.

**Ready for Step 8: API Design?**