# BullMQ — Chapter 4: BullMQ Architecture: Core Concepts

---

## Why This Chapter Exists

Chapters 1-3 built the context. You now know *why* background job processing exists, *how* Node.js executes code, and *what hardware* your workers run on. This chapter goes inside BullMQ itself.

By the end of this chapter, you'll be able to answer the most common BullMQ interview questions from first principles — not from memory, but because you understand the mechanics.

---

## The Three Moving Parts (Precise Definitions)

Before Redis, before code — let's define the three objects you interact with in BullMQ with precision.

```
┌─────────────────────────────────────────────────────────────────┐
│                    BULLMQ'S THREE OBJECTS                       │
├─────────────────┬───────────────────────────────────────────────┤
│  Queue          │  The PRODUCER side.                           │
│                 │  Lives in your API service.                   │
│                 │  Responsibility: add jobs to Redis,           │
│                 │  pause/resume the queue, inspect job state,   │
│                 │  clean old jobs.                              │
│                 │  Does NOT process jobs.                       │
├─────────────────┼───────────────────────────────────────────────┤
│  Worker         │  The CONSUMER side.                           │
│                 │  Lives in your worker container.              │
│                 │  Responsibility: poll Redis for jobs,         │
│                 │  acquire a lock, call your processor          │
│                 │  function, report success or failure,         │
│                 │  handle retries.                              │
│                 │  Does NOT know about the Queue object.        │
├─────────────────┼───────────────────────────────────────────────┤
│  QueueEvents    │  The OBSERVER side.                           │
│                 │  Optional. Subscribes to Redis pub/sub        │
│                 │  and emits events: completed, failed,         │
│                 │  stalled, progress.                           │
│                 │  Used for metrics, dashboards, alerting.      │
└─────────────────┴───────────────────────────────────────────────┘
```

Queue and Worker never talk to each other directly. They only talk to Redis. Redis is the only shared state.

---

## How BullMQ Uses Redis: The Exact Key Structure

This is the part most tutorials skip. Understanding the Redis key structure is what lets you answer "what happens when X fails" from first principles — because you know exactly where the data lives.

BullMQ uses a **namespace prefix** for all its keys: `bull:{queue-name}:`. Every key BullMQ creates starts with this prefix.

For a queue named `transaction-sync`:

```
REDIS KEY STRUCTURE — queue: "transaction-sync"
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  bull:transaction-sync:id                                       │
│  TYPE: String (counter)                                         │
│  PURPOSE: Auto-incrementing job ID generator                    │
│  EXAMPLE VALUE: "4821"                                          │
│                                                                 │
│  bull:transaction-sync:wait                                     │
│  TYPE: List (Redis LIST)                                        │
│  PURPOSE: Jobs waiting to be picked up by a worker              │
│  EXAMPLE VALUE: ["4820", "4819", "4818"]                        │
│                                                                 │
│  bull:transaction-sync:active                                   │
│  TYPE: List (Redis LIST)                                        │
│  PURPOSE: Jobs currently being processed by a worker            │
│  EXAMPLE VALUE: ["4817", "4816"]                                │
│                                                                 │
│  bull:transaction-sync:completed                                │
│  TYPE: Sorted Set (Redis ZSET)                                  │
│  PURPOSE: Successfully finished jobs                            │
│  SCORE: Unix timestamp of completion                            │
│  EXAMPLE: { "4815": 1735900000, "4814": 1735899950 }            │
│                                                                 │
│  bull:transaction-sync:failed                                   │
│  TYPE: Sorted Set (Redis ZSET)                                  │
│  PURPOSE: Jobs that failed after all retries exhausted          │
│  SCORE: Unix timestamp of failure                               │
│                                                                 │
│  bull:transaction-sync:delayed                                  │
│  TYPE: Sorted Set (Redis ZSET)                                  │
│  PURPOSE: Jobs scheduled for future execution                   │
│  SCORE: Unix timestamp of when to run                           │
│  EXAMPLE: { "4821": 1735986400 }  ← run at this timestamp       │
│                                                                 │
│  bull:transaction-sync:prioritized                              │
│  TYPE: Sorted Set (Redis ZSET)                                  │
│  PURPOSE: Jobs with priority set (lower score = higher prio)    │
│                                                                 │
│  bull:transaction-sync:{jobId}                                  │
│  TYPE: Hash (Redis HASH)                                        │
│  PURPOSE: Individual job data — payload, options, result        │
│  FIELDS:                                                        │
│    name          → "INITIAL_SYNC"                               │
│    data          → '{"userId":"usr_123","accountIds":[...]}'    │
│    opts          → '{"attempts":3,"backoff":{"type":"exp"}}'    │
│    timestamp     → "1735900000000"  (when job was created)      │
│    processedOn   → "1735900005000"  (when worker picked it up)  │
│    finishedOn    → "1735900025000"  (when job completed)        │
│    returnvalue   → '{"synced":247,"skipped":12}'                │
│    failedReason  → ""               (empty if succeeded)        │
│    attemptsMade  → "0"              (increments on each retry)  │
│    stacktrace    → "[]"             (filled on failure)         │
│                                                                 │
│  bull:transaction-sync:{jobId}:lock                             │
│  TYPE: String                                                   │
│  PURPOSE: Distributed lock — prevents two workers from          │
│           processing the same job simultaneously                │
│  VALUE: "{workerToken}"  (unique random string per worker)      │
│  TTL: 30 seconds (auto-expires if worker crashes)               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Why LIST for wait and active, but ZSET for completed, failed, delayed?**

This is a deliberate Redis data structure choice:

```
┌─────────────────────────────────────────────────────────────────┐
│              WHY THESE DATA STRUCTURES                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  wait → LIST                                                    │
│  Jobs are processed in order (FIFO by default).                 │
│  Redis LIST supports O(1) push to tail, pop from head.          │
│  Atomically move job from wait to active with LMOVE.            │
│                                                                 │
│  active → LIST                                                  │
│  Small set of currently running jobs.                           │
│  Worker needs to check "is my job still here?" quickly.         │
│  LIST scan is fine for small sets.                              │
│                                                                 │
│  delayed → ZSET (sorted by run timestamp as score)              │
│  Need to find "which jobs are due to run right now?"            │
│  ZRANGEBYSCORE(0, now) is O(log N + M) — very efficient.        │
│  LIST would require scanning every element.                     │
│                                                                 │
│  completed → ZSET (sorted by completion timestamp)              │
│  Need to clean up "jobs completed more than 24 hours ago."      │
│  ZRANGEBYSCORE(0, cutoff_timestamp) — efficient range query.    │
│                                                                 │
│  failed → ZSET (same reasoning as completed)                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

This is the same thinking you'd apply in any system design interview — choose the data structure based on the access pattern, not habit.

---

## The Complete Job Lifecycle

Now let's trace a single job — an `INITIAL_SYNC` for user `usr_123` — through its entire lifetime.

```
PHASE 1: ENQUEUE
═══════════════

API Service calls:
await this.syncQueue.add('INITIAL_SYNC', { userId: 'usr_123', accountIds: [...] })

BullMQ internally does (in a single Redis transaction):

  1. INCR bull:transaction-sync:id
     → returns "4821" (new job ID)

  2. HSET bull:transaction-sync:4821
       name      "INITIAL_SYNC"
       data      '{"userId":"usr_123","accountIds":["acc_1","acc_2"]}'
       opts      '{"attempts":3,"backoff":{"type":"exponential","delay":5000}}'
       timestamp "1735900000000"
     → job data written to Redis

  3. LPUSH bull:transaction-sync:wait "4821"
     → job ID added to the wait list

API Service receives control back immediately.
HTTP handler returns 202 Accepted to the mobile app.
Job is now sitting in Redis, waiting for a worker.
```

```
PHASE 2: WORKER PICKS UP THE JOB
══════════════════════════════════

Worker polls Redis periodically (using BRPOPLPUSH or LMOVE — blocking pop):

  LMOVE bull:transaction-sync:wait
        bull:transaction-sync:active
        RIGHT LEFT

  This is ATOMIC — no two workers can pop the same job.
  One worker wins, gets "4821", the other gets nothing.

  Worker that won then:

  1. Reads job data:
     HGETALL bull:transaction-sync:4821

  2. Sets the lock:
     SET bull:transaction-sync:4821:lock
         "{workerToken-abc123}"
         PX 30000        ← TTL: 30 seconds
         NX              ← only set if not exists

  3. Updates job hash:
     HSET bull:transaction-sync:4821
       processedOn "1735900005000"

  4. Calls your processor function with the job data.
```

```
PHASE 3: YOUR CODE RUNS
════════════════════════

async process(job: Job): Promise<void> {
  const { userId, accountIds } = job.data

  for (const accountId of accountIds) {
    const transactions = await goCardless.fetchTransactions(accountId)
    await prisma.transaction.createMany({ data: transactions })

    // Update progress (visible in Bull Board)
    await job.updateProgress({ current: i + 1, total: accountIds.length })
  }
}

During execution — lock renewal:
Every 15 seconds (half of 30s lockDuration), BullMQ auto-renews:
  SET bull:transaction-sync:4821:lock
      "{workerToken-abc123}"
      PX 30000
      XX   ← only update if exists (safety check)

As long as the worker is alive, the lock stays fresh.
If the worker crashes, renewal stops, lock expires after 30s.
```

```
PHASE 4A: SUCCESS
══════════════════

Your processor function returns without throwing.

BullMQ internally:

  1. LREM bull:transaction-sync:active 0 "4821"
     → remove job from active list

  2. ZADD bull:transaction-sync:completed
         1735900025000    ← score = completion timestamp
         "4821"
     → add to completed sorted set

  3. HSET bull:transaction-sync:4821
       finishedOn   "1735900025000"
       returnvalue  '{"synced":247,"skipped":12}'
     → update job hash with result

  4. DEL bull:transaction-sync:4821:lock
     → release the lock

  5. If removeOnComplete is configured:
     Schedule cleanup after TTL expires.
```

```
PHASE 4B: FAILURE
══════════════════

Your processor function throws an error.

BullMQ checks: attemptsMade < maxAttempts (3)?

  If YES (retry available):

    1. LREM bull:transaction-sync:active 0 "4821"
       → remove from active

    2. Calculate delay:
       exponential backoff: 5000ms × 2^0 = 5000ms (first retry)

    3. ZADD bull:transaction-sync:delayed
             (now + 5000)    ← score = run timestamp
             "4821"
       → job moves to delayed

    4. HSET bull:transaction-sync:4821
         attemptsMade "1"
         failedReason "GoCardless 429: rate limit exceeded"
         stacktrace   "[...]"
       → update job hash

    5. After 5 seconds, BullMQ scheduler moves it back:
       LMOVE delayed → wait
       Worker picks it up again for attempt 2.

  If NO (all retries exhausted):

    1. LREM bull:transaction-sync:active 0 "4821"

    2. ZADD bull:transaction-sync:failed
             1735900060000
             "4821"
       → job moves to failed permanently

    3. HSET bull:transaction-sync:4821
         finishedOn "1735900060000"
       → job hash updated

    4. DEL bull:transaction-sync:4821:lock

    5. QueueEvents emits 'failed' event
       → Datadog alert fires
       → Bull Board shows red
```

The complete lifecycle as a state diagram:

```
                    queue.add()
                        │
                        ▼
                    ┌───────┐
                    │ wait  │ ◄──────────────────────┐
                    └───┬───┘                        │
                        │ worker picks up            │ retry (delay expires)
                        ▼                            │
                    ┌────────┐                    ┌─────────┐
                    │ active │ ──── failure ────► │ delayed │
                    └───┬────┘     (retryable)    └─────────┘
                        │
              ┌─────────┴──────────┐
              │ success            │ failure
              │                    │ (no retries left)
              ▼                    ▼
         ┌───────────┐        ┌────────┐
         │ completed │        │ failed │
         └───────────┘        └────────┘
              │                    │
         auto-cleaned          kept for
         after 24hrs           7 days
                               (investigation)
```

---

## The Lock Mechanism: How Two Workers Never Process the Same Job

This is one of the most important things to understand about BullMQ in production, because it's the answer to "what guarantees do you have about duplicate processing?"

The `LMOVE` command (moving a job from `wait` to `active`) is atomic in Redis. This means only one worker can win the race for any given job. But atomicity alone isn't enough — what if the worker crashes after picking up the job?

This is where the lock comes in:

```
LOCK LIFECYCLE
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Worker A picks up job_4821                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  SET bull:transaction-sync:4821:lock                     │   │
│  │      "worker-token-abc123"                               │   │
│  │      PX 30000  ← 30 second TTL                           │   │
│  │      NX        ← only if key doesn't exist               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Worker A is healthy — renews lock every 15 seconds             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  SET bull:transaction-sync:4821:lock                     │   │
│  │      "worker-token-abc123"                               │   │
│  │      PX 30000  ← reset TTL to another 30 seconds         │   │
│  │      XX        ← only if key already exists (safety)     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Scenario A — Worker A completes successfully:                  │
│    DEL bull:transaction-sync:4821:lock                          │
│    Job moves to completed.                                      │
│    ✅ Clean, no issue.                                           │
│                                                                 │
│  Scenario B — Worker A crashes at T=18s:                        │
│    Lock renewal stops.                                          │
│    At T=48s (18s + 30s TTL), lock key disappears.               │
│                                                                 │
│  BullMQ stalledCheck fires every 30s:                           │
│    "Are there jobs in active with no lock?"                     │
│    Finds job_4821 in active — no lock key.                      │
│    → Marks job as STALLED                                       │
│    → Moves job_4821 back to wait                                │
│    → Worker B picks it up                                       │
│    → Job processes again from beginning                         │
│                                                                 │
│  This is AT-LEAST-ONCE delivery.                                │
│  The job will always complete — but may run more than once.     │
│  Your processor must be IDEMPOTENT.                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**What is idempotency in this context?**

It means running the same job twice produces the same result as running it once. For the `INITIAL_SYNC` job, this is handled by the `externalId` unique constraint on the transactions table. If job_4821 syncs 247 transactions, crashes, and runs again — the second run tries to insert the same 247 transactions, hits the unique constraint for each one, skips them as duplicates, and completes cleanly. The end state is identical to a single successful run.

---

## The Stalled Job: What It Is and the Configuration That Controls It

A stalled job is a job that is in the `active` list but whose lock has expired. It indicates a worker that either crashed or got stuck.

```typescript
// Worker configuration that controls stalled job behaviour
new Worker('transaction-sync', processor, {
  stalledInterval: 30_000,  // Check for stalled jobs every 30 seconds
  maxStalledCount: 2,        // After stalling 2 times, move to failed
                             // (prevents infinite crash loops)
})
```

`maxStalledCount` is critical. Without it:

```
Without maxStalledCount:
  Job crashes Worker → stalled → requeued → crashes Worker again
  → stalled → requeued → crashes Worker again
  → infinite loop, thrashing

With maxStalledCount: 2:
  Stall 1 → requeued → Worker crashes again
  Stall 2 → requeued → Worker crashes again
  Stall 3 → maxStalledCount reached → job moves to failed
  → Datadog alert fires → engineer investigates
  → No more infinite loop
```

---

## NestJS Integration: What the Code Looks Like

Let's make this concrete with actual NestJS code that matches the FinVerse setup.

```typescript
// ─── app.module.ts ───────────────────────────────────────────────
// Register BullMQ module with Redis connection and default options

@Module({
  imports: [
    BullModule.forRoot({
      connection: {
        host: process.env.REDIS_HOST,
        port: 6379,
        password: process.env.REDIS_PASSWORD,
        // TLS in production (ElastiCache requires it)
        tls: process.env.NODE_ENV === 'production' ? {} : undefined,
      },
      defaultJobOptions: {
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 5000,         // 5s, 10s, 20s between retries
        },
        removeOnComplete: {
          age: 86_400,         // remove completed jobs after 24 hours
          count: 1_000,        // always keep last 1000 completed jobs
        },
        removeOnFail: {
          age: 7 * 86_400,     // keep failed jobs for 7 days
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
// ─── transaction-sync.producer.ts ────────────────────────────────
// Lives in the API service — adds jobs to the queue

@Injectable()
export class TransactionSyncProducer {
  constructor(
    @InjectQueue('transaction-sync')
    private readonly syncQueue: Queue
  ) {}

  async enqueueInitialSync(
    userId: string,
    accountIds: string[]
  ): Promise<void> {
    await this.syncQueue.add(
      'INITIAL_SYNC',
      { userId, accountIds },
      {
        // Deterministic job ID prevents duplicate enqueue
        // If this exact jobId already exists in wait or active,
        // BullMQ silently ignores the second add() call
        jobId: `initial-sync-${userId}`,
        priority: 1,  // highest priority — user is waiting
      }
    )
  }

  async enqueuePeriodicSync(
    userId: string,
    accountIds: string[]
  ): Promise<void> {
    await this.syncQueue.add(
      'PERIODIC_SYNC',
      { userId, accountIds },
      {
        // Timestamp in jobId — each periodic run is a distinct job
        jobId: `periodic-sync-${userId}-${Date.now()}`,
        priority: 2,  // lower priority than user-triggered syncs
      }
    )
  }
}
```

```typescript
// ─── transaction-sync.worker.ts ──────────────────────────────────
// Lives in the WORKER container — processes jobs

@Processor('transaction-sync', {
  concurrency: 10,    // 10 concurrent async jobs on event loop
  limiter: {
    max: 30,          // max 30 jobs processed per 10 seconds
    duration: 10_000, // rate limit window — respects GoCardless limits
  },
  stalledInterval: 30_000,
  maxStalledCount: 2,
})
export class TransactionSyncWorker extends WorkerHost {
  constructor(
    private readonly goCardlessService: GoCardlessService,
    private readonly transactionService: TransactionService,
  ) {
    super()
  }

  // BullMQ calls this method with each job
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

    for (let i = 0; i < accountIds.length; i++) {
      const accountId = accountIds[i]

      // Progress update — visible in Bull Board dashboard
      await job.updateProgress({
        current: i + 1,
        total: accountIds.length,
        currentAccount: accountId,
      })

      const transactions =
        await this.goCardlessService.fetchAllTransactions(accountId)

      await this.transactionService.bulkInsert(
        userId,
        accountId,
        transactions    // idempotent — externalId unique constraint
      )
    }
  }

  private async handlePeriodicSync(job: Job): Promise<void> {
    const { userId, accountIds } = job.data

    for (const accountId of accountIds) {
      const lastSync = await this.transactionService
        .getLastSyncTimestamp(accountId)

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

---

## Chapter 4 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 4 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Three BullMQ objects:                                          │
│  Queue (producer) → Redis (state) → Worker (consumer)           │
│  They never talk to each other directly                         │
│                                                                 │
│  Redis key structure:                                           │
│  :wait   → LIST  (FIFO queue of job IDs)                        │
│  :active → LIST  (currently processing)                         │
│  :delayed → ZSET (sorted by run timestamp)                      │
│  :completed → ZSET (sorted by completion time)                  │
│  :failed → ZSET (sorted by failure time)                        │
│  :{jobId} → HASH (all job data, payload, result, errors)        │
│  :{jobId}:lock → String with TTL (distributed lock)             │
│                                                                 │
│  LMOVE wait→active is atomic — only one worker wins a job       │
│                                                                 │
│  Lock TTL = 30s, renewed every 15s while worker lives           │
│  If worker crashes → lock expires → stalled job detected        │
│  → job requeued → at-least-once delivery                        │
│                                                                 │
│  Consequence: your processor MUST be idempotent                 │
│  FinVerse handles this via externalId unique constraints        │
│                                                                 │
│  maxStalledCount prevents infinite crash loops                  │
│  After N stalls → job moves to failed → alert fires             │
│                                                                 │
│  Job lifecycle: wait → active → completed                       │
│                              → delayed (retry)→ wait            │
│                              → failed (retries exhausted)       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 4 done. Ready for Chapter 5 — Concurrency, Worker Pools, and Scaling — where we go deep on what the `concurrency` setting actually does inside the event loop, how to reason about the right number, and how multiple containers coordinate through Redis without stepping on each other.