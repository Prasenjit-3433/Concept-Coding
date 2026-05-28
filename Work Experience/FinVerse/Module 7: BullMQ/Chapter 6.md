Writing Chapter 6 now.

---

# BullMQ — Chapter 6: Job Lifecycle — Retries, Delays, Priorities, and Scheduling

---

## Why This Chapter Exists

Chapters 1-5 gave you the foundation — why BullMQ exists, how Node.js executes code, what containers are, how Redis stores job state, and how concurrency works. You can now answer "what is BullMQ" and "how does it work."

But production systems need more than just "add job, process job." They need:

- "Retry this job if it fails, but wait longer each time"
- "Run this job every 4 hours, forever"
- "Don't pick up this job for 30 minutes"
- "This user just connected — process their sync before the scheduled batch"
- "Don't enqueue a second sync for this user if one is already pending"

This chapter covers every knob BullMQ gives you for controlling **how** and **when** jobs run. And because you know Java deeply, every concept will be anchored to something you already understand.

---

## Part 1 — Retries

### The Java Anchor: Spring Retry

In Spring Boot, you handle retry with `@Retryable`:

```java
@Retryable(
  value = { GoCardlessException.class },
  maxAttempts = 3,
  backoff = @Backoff(delay = 5000, multiplier = 2)
)
public List<Transaction> fetchTransactions(String accountId) {
  return goCardlessClient.fetch(accountId);
}
```

This is declarative. Spring AOP intercepts the method call, catches the exception, waits, and calls it again. If all attempts fail, it calls your `@Recover` method.

**The mental model:** retry is a wrapper around a method call, happening within the same thread.

**BullMQ's mental model:** retry is a property of the job itself, managed by the Worker across potentially different executions and different containers.

This is a fundamentally different model. In Java, if the server restarts mid-retry, the retry is lost — it was in memory on that thread. In BullMQ, the retry state lives in Redis. The server can restart, the container can crash, and the retry will still happen when a worker comes back up.

---

### How Retries Work in BullMQ

When your processor function throws an error, BullMQ intercepts it. It checks one thing first:

```
Has this job exhausted its attempts?

  attemptsMade < maxAttempts ?
    YES → calculate delay, move job to delayed state
    NO  → move job to failed state permanently
```

Let's trace this precisely in Redis:

```
JOB FAILS — RETRY FLOW (Redis level)

Initial state:
  bull:transaction-sync:wait = ["job_4821"]
  bull:transaction-sync:4821 = { attemptsMade: "0", opts: '{"attempts":3}' }

Worker picks up job_4821:
  bull:transaction-sync:active = ["job_4821"]

Processor throws: new Error("GoCardless 429: rate limit exceeded")

BullMQ checks: attemptsMade (0) < attempts (3) → YES, retry

BullMQ calculates delay (exponential backoff):
  delay = baseDelay × 2^attemptsMade
        = 5000 × 2^0
        = 5000ms (5 seconds)

BullMQ does (in one Redis transaction):

  1. LREM bull:transaction-sync:active 0 "job_4821"
     → remove from active

  2. HSET bull:transaction-sync:4821
       attemptsMade  "1"
       failedReason  "GoCardless 429: rate limit exceeded"
       stacktrace    "[\"Error: GoCardless 429...\"]"
     → update job hash

  3. ZADD bull:transaction-sync:delayed
         (now + 5000)    ← score = unix timestamp when to run
         "job_4821"
     → job enters delayed state, not visible to workers yet

State after first failure:
  bull:transaction-sync:active  = []
  bull:transaction-sync:delayed = { "job_4821": 1735900005000 }
  bull:transaction-sync:4821    = { attemptsMade: "1" }

After 5 seconds:
  BullMQ internal scheduler fires:
  ZRANGEBYSCORE bull:transaction-sync:delayed 0 (now)
  → finds job_4821 (its score timestamp has passed)
  LMOVE delayed → wait
  → job_4821 returns to wait list

Worker picks it up again (Attempt 2):
  Same flow. If fails again:
  delay = 5000 × 2^1 = 10000ms (10 seconds)
  job moves to delayed again

Attempt 3: delay = 5000 × 2^2 = 20000ms (20 seconds)

If Attempt 3 also fails:
  attemptsMade (3) = attempts (3) → NO more retries

  LREM bull:transaction-sync:active 0 "job_4821"
  ZADD bull:transaction-sync:failed
       (now)
       "job_4821"
  → job moves to failed permanently
  → QueueEvents emits 'failed' event
  → Datadog alert fires
```

The complete retry timeline:

```
TIME ─────────────────────────────────────────────────────────────►

T+0s:    Job enqueued → wait
T+1s:    Worker picks up → active
T+2s:    Processor throws → delayed (5s delay)
T+7s:    Delay expires → wait
T+8s:    Worker picks up → active (attempt 2)
T+9s:    Processor throws → delayed (10s delay)
T+19s:   Delay expires → wait
T+20s:   Worker picks up → active (attempt 3)
T+21s:   Processor throws → failed permanently
         Alert fires → engineer investigates
```

---

### The Four Backoff Strategies

BullMQ ships with two built-in strategies, and lets you write custom ones. Let's map each to its Resilience4j equivalent from your Spring Boot notes.

```
┌─────────────────────────────────────────────────────────────────────┐
│              RETRY STRATEGY COMPARISON                              │
├───────────────────────┬──────────────────────────────────────────── │
│  JAVA (Resilience4j)  │  BULLMQ EQUIVALENT                          │
├───────────────────────┼─────────────────────────────────────────────│
│  Fixed interval       │  { type: 'fixed', delay: 5000 }             │
│                       │  Each retry waits exactly 5 seconds         │
├───────────────────────┼─────────────────────────────────────────────│
│  Exponential backoff  │  { type: 'exponential', delay: 5000 }       │
│  (no jitter)          │  delay × 2^attempt                          │
│                       │  5s, 10s, 20s, 40s...                       │
├───────────────────────┼─────────────────────────────────────────────│
│  Exponential + jitter │  Custom function (shown below)              │
│  (industry standard)  │                                             │
├───────────────────────┼─────────────────────────────────────────────│
│  Custom               │  { type: 'custom', delay: yourFn }          │
│  IntervalFunction     │  receives attemptsMade, returns ms          │
└───────────────────────┴─────────────────────────────────────────────┘
```

Your Spring Boot notes explain exactly why jitter matters — the **Thundering Herd problem**. If 500 jobs all fail at the same time (GoCardless went down for 30 seconds), and they all use pure exponential backoff, they all retry at exactly the same moment. You send 500 requests to GoCardless simultaneously the instant it recovers. It immediately gets overwhelmed and goes down again.

Jitter randomises the retry window so retries spread out:

```typescript
// Pure exponential — THUNDERING HERD RISK
// All 500 jobs retry at T+5s, T+10s, T+20s simultaneously
{
  type: 'exponential',
  delay: 5000,
}

// Exponential + jitter — INDUSTRY STANDARD
// Custom backoff function:
const backoffWithJitter = (attemptsMade: number): number => {
  const baseDelay = 5000
  const maxDelay = 60_000  // cap at 60 seconds
  const exponential = baseDelay * Math.pow(2, attemptsMade)
  const cappedDelay = Math.min(maxDelay, exponential)
  // Add random jitter: 0% to 100% of the delay
  const jitter = Math.random() * cappedDelay
  return Math.floor(jitter)
}
```

With jitter, those 500 jobs spread their retries across the full delay window. GoCardless recovers, gets a steady trickle of requests, handles them normally.

---

### FinVerse's Retry Configuration Per Queue

```typescript
// Different queues have different retry needs

// transaction-sync: GoCardless can rate-limit, needs jitter
BullModule.registerQueue({
  name: 'transaction-sync',
  defaultJobOptions: {
    attempts: 3,
    backoff: {
      type: 'custom',
      delay: backoffWithJitter,   // exponential + jitter
    },
  },
})

// outbox-publisher: publishing to RabbitMQ
// RabbitMQ is internal — if it's down, something serious is wrong
// Retry quickly, fail fast for alerting
BullModule.registerQueue({
  name: 'outbox-publisher',
  defaultJobOptions: {
    attempts: 5,
    backoff: {
      type: 'fixed',
      delay: 2000,    // retry every 2 seconds — want to know fast
    },
  },
})

// tax-report-generation: heavy, runs once a year
// Don't retry aggressively — each attempt is expensive
BullModule.registerQueue({
  name: 'tax-report-generation',
  defaultJobOptions: {
    attempts: 2,          // one retry only
    backoff: {
      type: 'exponential',
      delay: 30_000,      // 30s, then 60s — give the system time to recover
    },
  },
})
```

---

### What Errors Should NOT Trigger a Retry

This is a nuance your Spring Boot notes cover well — not all errors are retryable. Retrying a non-retryable error wastes attempts and delays the failure alert.

```typescript
@Processor('transaction-sync', { concurrency: 10 })
export class TransactionSyncWorker extends WorkerHost {

  async process(job: Job): Promise<void> {
    try {
      await this.handleSync(job)
    } catch (error) {

      // 4xx from GoCardless = our fault (bad request, expired token)
      // Retrying won't help — fail immediately
      if (error.status >= 400 && error.status < 500) {
        // Throw UnrecoverableError — BullMQ skips ALL remaining attempts
        // and moves the job directly to failed
        throw new UnrecoverableError(
          `GoCardless client error ${error.status}: ${error.message}`
        )
      }

      // 5xx from GoCardless = their fault (server error, overload)
      // Worth retrying — rethrow normally so BullMQ retries
      throw error
    }
  }
}
```

`UnrecoverableError` is a special BullMQ class. When you throw it, BullMQ immediately moves the job to `failed` regardless of remaining attempts. It signals "don't waste retries on this — a human needs to look at it."

Java equivalent: in Resilience4j, `ignoreExceptions` in `RetryConfig` — exceptions in that list are not retried.

---

## Part 2 — Delayed Jobs

### The Java Anchor: ScheduledThreadPoolExecutor

In Java, to run something after a delay:

```java
ScheduledExecutorService scheduler = 
  Executors.newScheduledThreadPool(1);

scheduler.schedule(
  () -> syncBankAccount(accountId),
  30,                    // delay amount
  TimeUnit.MINUTES       // delay unit
)
```

The JVM keeps a `DelayedQueue` internally (a priority queue sorted by execution timestamp). When the delay expires, the scheduler picks the task and submits it to a thread. If the JVM restarts, the scheduled task is gone.

BullMQ's delayed job is the persistent equivalent — it survives restarts because the delay state lives in Redis.

---

### How Delayed Jobs Live in Redis

In Chapter 4 you learned that `bull:{queue}:delayed` is a Redis **Sorted Set (ZSET)** where the score is the Unix timestamp of when the job should run.

This is worth understanding precisely, because it's also how retries work internally and how repeatable jobs work. The ZSET is the single mechanism behind all three.

```
REDIS ZSET: bull:transaction-sync:delayed

Structure: { jobId: score }
where score = unix timestamp (milliseconds) of when to run

Example content at 14:00:00:
{
  "job_4821": 1735905000000,  ← run at 14:30:00 (30 min from now)
  "job_4822": 1735908600000,  ← run at 15:30:00 (1.5 hrs from now)
  "job_4823": 1735900305000,  ← run at 14:05:05 (5 sec from now)
}

ZSET is sorted by score (ascending).
Lowest score = soonest job = appears first.

BullMQ internal scheduler loop (runs every N milliseconds):

  ZRANGEBYSCORE bull:transaction-sync:delayed
    -inf         ← from the beginning
    (now)        ← up to current timestamp
    LIMIT 0 100  ← max 100 jobs per check

  This returns all jobs whose "run at" timestamp has passed.
  BullMQ moves them from delayed → wait.
  Workers pick them up normally.
```

Why ZSET and not a simple LIST? Because you need efficient range queries by time. `ZRANGEBYSCORE 0 now` is O(log N + M) — extremely fast even with millions of delayed jobs. A LIST would require scanning every element — O(N).

This is the same data structure reasoning from Chapter 4 — choose your structure based on the access pattern.

---

### Adding a Delayed Job in FinVerse

```typescript
// User connected their bank — we want to sync immediately (no delay)
// But we DON'T want to spam GoCardless if their API is recovering
// after a known outage window at 14:00-14:05

await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId: 'usr_123', accountIds: ['acc_1', 'acc_2'] },
  {
    delay: 5 * 60 * 1000,   // 5 minutes — wait for outage to clear
    jobId: `initial-sync-usr_123`,  // deduplication (covered in Part 4)
  }
)

// The job won't appear in the wait list.
// It sits in bull:transaction-sync:delayed with score = now + 5min.
// After 5 minutes, the scheduler moves it to wait.
// A worker picks it up.
```

The user's app shows "Connecting your bank, this may take a few minutes" — they don't experience the delay as a failure. The delay is invisible to them.

---

## Part 3 — Repeatable Jobs (Scheduled / Cron Jobs)

### The Java Anchor: @Scheduled

In Spring Boot:

```java
@Scheduled(cron = "0 0 */4 * * *")    // every 4 hours
public void periodicTransactionSync() {
  userService.getAllActiveUsers()
             .forEach(user -> syncService.enqueueSync(user));
}

@Scheduled(fixedDelay = 14_400_000)   // every 4 hours after last run
public void periodicSync() { ... }
```

Spring's scheduler runs inside the JVM. It's simple, it works, but it has limitations:
- If the JVM restarts at the scheduled time, the job doesn't run
- Multiple instances of the same service create duplicate execution (unless you use ShedLock or similar)
- No visibility into "did the last scheduled run succeed?"

BullMQ's repeatable jobs solve the duplication problem — because Redis coordinates which worker picks up the job, only one worker ever runs it at a time, even if you have 8 worker containers.

---

### How Repeatable Jobs Work in Redis

BullMQ generates a deterministic job ID for each scheduled occurrence of a repeatable job, based on the job name, cron/interval pattern, and the next execution timestamp.

```
REPEATABLE JOB — REDIS INTERNALS

When you call:
  queue.add('PERIODIC_SYNC', data, {
    repeat: { cron: '0 */4 * * *' }  // every 4 hours
  })

BullMQ does:

  1. Stores the repeat config:
     HSET bull:transaction-sync:repeat
       'PERIODIC_SYNC:::0 */4 * * *'
       '{"cron":"0 */4 * * *","tz":"UTC","prevMillis":0}'

  2. Calculates the next execution timestamp from cron:
     nextRun = 1735920000000  (next 4-hour mark)

  3. Creates a job with deterministic ID:
     jobId = 'repeat:PERIODIC_SYNC:::0 */4 * * *:1735920000000'
     (name + pattern + timestamp = always unique per occurrence)

  4. Adds job to delayed ZSET:
     ZADD bull:transaction-sync:delayed
          1735920000000          ← score = nextRun timestamp
          'repeat:PERIODIC_SYNC:::...:1735920000000'

At 1735920000000 (scheduled time):
  Scheduler moves job from delayed → wait
  Worker picks up, processes

After successful completion:
  BullMQ automatically calculates NEXT occurrence:
  nextRun = 1735934400000  (4 hours later)
  Creates new job with new deterministic ID
  Adds to delayed ZSET again
  → cycle repeats forever
```

The critical insight: **the next occurrence is only scheduled after the current one completes.** This means if a repeatable job takes 3 hours to run and you have a 4-hour interval, the next run starts 4 hours after the previous one *finished* (when using `fixedDelay`) — or 4 hours after the previous one *started* (when using cron). This is an important distinction.

---

### Cron vs Fixed Interval: Which to Use

```
┌─────────────────────────────────────────────────────────────────┐
│              CRON vs FIXED INTERVAL                             │
├───────────────────────┬─────────────────────────────────────────┤
│  CRON                 │  FIXED INTERVAL                         │
├───────────────────────┼─────────────────────────────────────────┤
│  Runs at specific     │  Runs N ms after previous run           │
│  clock times          │  finished (fixedDelay)                  │
│                       │  OR N ms after previous run             │
│                       │  started (fixedRate / every)            │
├───────────────────────┼─────────────────────────────────────────┤
│  { cron: '0 1 * * *'} │  { every: 14_400_000 }                  │
│  Runs at 01:00 daily  │  Runs every 4 hours from first run      │
├───────────────────────┼─────────────────────────────────────────┤
│  Good for:            │  Good for:                              │
│  Tax report gen       │  Transaction sync                       │
│  (Jan 1st at 00:00)   │  (4 hours after last sync)              │
│  Monthly investment   │  Outbox publisher                       │
│  (1st of month)       │  (every 30 seconds)                     │
│  EU market hours      │  Portfolio valuation                    │
│  (market open/close)  │  (every 15 minutes)                     │
└───────────────────────┴─────────────────────────────────────────┘
```

---

### Do Repeatable Jobs Survive Redis Restarts?

This is one of the most common interview questions about BullMQ scheduling, and the answer has nuance.

```
SCENARIO: Redis restarts at 15:00
Next scheduled run of PERIODIC_SYNC was at 16:00

The repeat config is stored in Redis:
  bull:transaction-sync:repeat → includes the cron pattern

If Redis restarts with persistence enabled (RDB or AOF):
  ✅ Repeat config survives restart
  ✅ The scheduled job in delayed ZSET survives restart
  ✅ Next run at 16:00 happens as expected

If Redis restarts WITHOUT persistence (default in dev):
  ❌ All BullMQ data is lost
  ❌ Repeatable jobs are gone
  ❌ You must call queue.add() again to reschedule

FinVerse uses AWS ElastiCache for Redis in production.
ElastiCache is configured with:
  - AOF (Append-Only File) persistence enabled
  - Multi-AZ replication (primary + replica in different AZs)
  - Automatic failover

Result: Redis restart or even primary node failure → replica
  promotes → repeatable jobs survive intact.
```

The practical rule: in production, always run Redis with persistence. BullMQ's schedule resilience depends entirely on Redis's durability.

---

### FinVerse's Repeatable Job Setup

```typescript
// In Core Product — service initialisation
// These are set up ONCE when the service starts

@Injectable()
export class JobSchedulerService implements OnModuleInit {
  constructor(
    @InjectQueue('transaction-sync')
    private readonly syncQueue: Queue,

    @InjectQueue('tax-report-generation')
    private readonly taxQueue: Queue,
  ) {}

  async onModuleInit(): Promise<void> {
    await this.setupRepeatableJobs()
  }

  private async setupRepeatableJobs(): Promise<void> {

    // The schedulerService runs on Core Product startup.
    // It does NOT add a new job on every startup — BullMQ checks if
    // a repeatable job with this key already exists and skips it.

    // Periodic sync trigger (every 4 hours)
    // This enqueues one SCHEDULE_PERIODIC_SYNC_BATCH job on schedule.
    // The batch job then fans out individual sync jobs for each user.
    await this.syncQueue.add(
      'SCHEDULE_PERIODIC_SYNC_BATCH',
      {},
      {
        repeat: {
          every: 4 * 60 * 60 * 1000,   // every 4 hours
        },
        jobId: 'periodic-sync-batch-scheduler', // stable ID prevents duplicates
      }
    )

    // Annual tax report generation (Jan 1st at 00:05 UTC)
    await this.taxQueue.add(
      'ANNUAL_TAX_REPORT_GENERATION',
      {},
      {
        repeat: {
          cron: '5 0 1 1 *',   // minute 5 of midnight on Jan 1st
          tz: 'UTC',
        },
        jobId: 'annual-tax-report-scheduler',
      }
    )
  }
}
```

---

## Part 4 — Job Deduplication

### The Problem Without Deduplication

Consider this scenario:

```
14:00:00 — User connects bank account
           INITIAL_SYNC job enqueued → bull:transaction-sync:wait

14:00:05 — User gets impatient, taps "Connect" again
           Another INITIAL_SYNC job enqueued for the same user

Now:
  bull:transaction-sync:wait = ["job_4821", "job_4822"]
  Both jobs have identical payloads: { userId: 'usr_123' }

Worker 1 picks up job_4821 — syncing usr_123
Worker 2 picks up job_4822 — also syncing usr_123 simultaneously

Result:
  Both workers call GoCardless for the same user
  Both try to insert the same transactions
  Race conditions on bulk insert
  User gets charged GoCardless quota twice
  Duplicate processing — wasteful, potentially incorrect
```

---

### The Solution: Deterministic jobId

BullMQ's deduplication mechanism is simple and elegant: **if a job with a given `jobId` already exists in the `wait` or `active` state, `queue.add()` is silently ignored.**

```typescript
// Without deduplication — two separate jobs created
await syncQueue.add('INITIAL_SYNC', { userId: 'usr_123' })
await syncQueue.add('INITIAL_SYNC', { userId: 'usr_123' })
// → 2 jobs in queue, both will run

// WITH deduplication — deterministic jobId
await syncQueue.add(
  'INITIAL_SYNC',
  { userId: 'usr_123' },
  { jobId: `initial-sync-usr_123` }    // stable, user-specific ID
)
await syncQueue.add(
  'INITIAL_SYNC',
  { userId: 'usr_123' },
  { jobId: `initial-sync-usr_123` }    // same jobId
)
// → Only 1 job in queue. Second add() does nothing.
```

What happens in Redis when a duplicate jobId arrives:

```
DEDUPLICATION — REDIS LEVEL

First add():
  HSET bull:transaction-sync:initial-sync-usr_123 ... (job data)
  LPUSH bull:transaction-sync:wait "initial-sync-usr_123"
  → job created

Second add() with same jobId:
  BullMQ checks:
  EXISTS bull:transaction-sync:initial-sync-usr_123
  → key exists → SKIP
  → no Redis write, no new job
  → add() resolves with the existing job object
```

---

### jobId Design Patterns in FinVerse

```typescript
// Pattern 1: User + Job Type = only one pending per user per type
jobId: `initial-sync-${userId}`
// Use for: INITIAL_SYNC, PORTFOLIO_VALUATION_REQUEST

// Pattern 2: User + Account + Type = only one pending per account
jobId: `periodic-sync-${userId}-${accountId}`
// Use for: PERIODIC_SYNC (per account, not per user)
// Allows different accounts to sync concurrently

// Pattern 3: User + Date + Type = one per day
jobId: `budget-check-${userId}-${todayIso}`
// todayIso = '2024-01-15'
// Prevents multiple budget checks for same user on same day

// Pattern 4: No deduplication (timestamp in ID)
jobId: `outbox-publish-${eventId}-${Date.now()}`
// Use for: outbox-publisher — every event is distinct
// Must run even if a previous publication failed

// Pattern 5: Stable scheduler job
jobId: 'annual-tax-report-scheduler'
// Use for: repeatable jobs — one schedule entry, stable forever
```

The key principle: **a jobId that encodes uniqueness constraints prevents exactly the duplicates you care about.** It's the same thinking as a unique database constraint — define uniqueness, enforce at the system level.

---

## Part 5 — Priority Queues

### The Java Anchor: PriorityBlockingQueue

In Java's `ThreadPoolExecutor`, you can use a `PriorityBlockingQueue` as the work queue. Tasks are compared by priority and the highest-priority task is always picked next.

BullMQ has a similar concept built in — with an important architectural detail.

---

### How Priority Works in BullMQ

When you add a job with a priority, it doesn't go into the regular `wait` list. It goes into a separate `prioritized` sorted set, where the score represents priority:

```
PRIORITY IN BULLMQ

Lower score number = Higher priority
  priority: 1 = highest (processed first)
  priority: 100 = lower
  priority: undefined = treated as normal queue (FIFO)

Redis structure:
  bull:transaction-sync:prioritized ZSET
  {
    "job_4823": 1,    ← priority 1 (user-triggered sync)
    "job_4824": 2,    ← priority 2 (scheduled sync)
    "job_4825": 2,    ← priority 2 (scheduled sync)
    "job_4826": 10,   ← priority 10 (low priority batch)
  }

Worker polls prioritized ZSET first:
  ZRANGE bull:transaction-sync:prioritized 0 0
  → returns job with lowest score (highest priority)
  → processes it
  → moves to active

Then polls regular wait list:
  LMOVE bull:transaction-sync:wait active
  → processes FIFO
```

---

### FinVerse's Priority Strategy

```typescript
// User-triggered sync — they're waiting in the app
// Highest priority
await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId, accountIds },
  {
    jobId: `initial-sync-${userId}`,
    priority: 1,
  }
)

// Periodic scheduled sync — background, no user waiting
// Normal priority
await this.syncQueue.add(
  'PERIODIC_SYNC',
  { userId, accountIds },
  {
    jobId: `periodic-sync-${userId}-${Date.now()}`,
    priority: 2,
  }
)

// Low-value background cleanup jobs
// Lowest priority — only run when nothing else is pending
await this.syncQueue.add(
  'CLEANUP_STALE_CONNECTIONS',
  {},
  {
    priority: 10,
  }
)
```

The practical effect at FinVerse: during the 08:00 sync window when 2,000 periodic syncs are queued, if a user manually connects their bank, their `INITIAL_SYNC` (priority 1) jumps to the front of the queue. It processes within seconds, not hours. The user's experience is fast even during heavy background load.

---

### Priority Tradeoff: Starvation

Priority queues have a well-known problem: **starvation**. If high-priority jobs keep arriving constantly, low-priority jobs never get processed.

```
Starvation scenario:

Queue:
  [priority 1 jobs arriving continuously from user actions]
  [priority 10 jobs waiting since yesterday]

Workers always pick priority 1 jobs first.
Priority 10 jobs never run.
Stale cleanup jobs accumulate forever.

FinVerse's mitigation:
  Only user-initiated syncs get priority 1.
  Scheduled syncs (majority) stay at priority 2.
  Cleanup jobs run during off-peak hours with a separate
  scheduled trigger — so they actually get enqueued
  when the priority 1 flood is low.
```

This is the same problem your Spring Boot notes describe for thread pool starvation — high-priority tasks starve lower-priority ones if you're not careful. The mitigation is the same: ensure low-priority work gets its own dedicated window or separate queue.

---

## Part 6 — Memory Management: removeOnComplete and removeOnFail

### Why This Matters

Every job that completes or fails leaves behind a Redis Hash (`bull:{queue}:{jobId}`) containing its payload, result, error message, and stack trace. If you never clean these up:

```
Without cleanup — Redis memory after 30 days:

  180,000 users × 1 sync/day × 30 days = 5,400,000 completed jobs
  Each job Hash ≈ 500 bytes average
  Total: ~2.7 GB in Redis just from job history

  AWS ElastiCache r6g.large: 13.07 GB total
  2.7 GB of dead job data = ~20% of your cache wasted

  This also affects performance:
  SCAN operations on large Redis keyspaces slow down
  Bull Board becomes unresponsive (can't render 5M jobs)
  Redis memory pressure → eviction of active data
```

BullMQ gives you two config options to prevent this:

```typescript
defaultJobOptions: {

  removeOnComplete: {
    age: 86_400,       // remove completed jobs after 24 hours
    count: 1_000,      // always keep the most recent 1,000 completed jobs
                       // whichever condition is hit first triggers removal
  },

  removeOnFail: {
    age: 7 * 86_400,   // keep failed jobs for 7 days
    count: 5_000,      // keep up to 5,000 failed jobs
  },
}
```

**Why different retention for complete vs fail?**

Completed jobs: you only need them for debugging very recent issues. 24 hours is usually enough — if something went wrong in the last processing run, you'll know within hours from Datadog alerts. You don't need 30 days of successful sync history in Redis. That data is already in PostgreSQL (the actual transactions, sync logs).

Failed jobs: you need more time. A failed tax report job might not be noticed until a user tries to download their report the next week. Engineers need the full payload and stack trace to diagnose what went wrong. 7 days gives you enough window to investigate.

The `count` parameter is a safety ceiling — even within the age window, if somehow 10,000 completed jobs pile up (shouldn't happen, but defensive), keep only the most recent 1,000.

---

### The `age` vs `count` Interaction

```
removeOnComplete: { age: 86400, count: 1000 }

Timeline with 50 completions per minute:

Hour 1:  3,000 completed jobs in Redis
Hour 2:  6,000 completed jobs → count threshold (1,000) hit
         BullMQ removes 5,000 oldest completed jobs
         Keeps most recent 1,000

Hour 25: Jobs from Hour 1 are now 25 hours old → age threshold hit
         BullMQ removes all completed jobs older than 24 hours

Steady state: Redis holds at most 1,000 recent completed jobs
              plus jobs from the last 24 hours (capped at 1,000)
```

In practice, BullMQ evaluates both conditions after each job completes and removes jobs that exceed either threshold.

---

## Putting It All Together: A Single Job's Complete Configuration

Here is the full `queue.add()` call for FinVerse's most important job, with every option we've covered:

```typescript
// Core Product — BankAccountService
async enqueueInitialSync(
  userId: string,
  accountIds: string[],
  options?: { delayMs?: number }
): Promise<void> {

  await this.syncQueue.add(
    'INITIAL_SYNC',
    {
      userId,
      accountIds,
      requestedAt: new Date().toISOString(),
    },
    {
      // DEDUPLICATION
      // If a sync is already pending for this user, ignore this call
      jobId: `initial-sync-${userId}`,

      // PRIORITY
      // User-triggered → jump to front of queue
      priority: 1,

      // DELAY (optional)
      // Used when GoCardless has a known issue window
      delay: options?.delayMs ?? 0,

      // RETRIES
      attempts: 3,
      backoff: {
        type: 'custom',
        delay: (attemptsMade: number) => {
          const base = 5000
          const max = 60_000
          const exp = base * Math.pow(2, attemptsMade)
          const capped = Math.min(max, exp)
          return Math.floor(Math.random() * capped)   // jitter
        },
      },

      // MEMORY MANAGEMENT
      removeOnComplete: {
        age: 86_400,    // 24 hours
        count: 1_000,
      },
      removeOnFail: {
        age: 7 * 86_400,
        count: 5_000,
      },
    }
  )
}
```

---

## Chapter 6 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 6 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  RETRIES                                                        │
│  → State lives in Redis: attemptsMade tracked in job Hash       │
│  → Failed job moves to delayed ZSET, then back to wait          │
│  → Backoff strategies: fixed, exponential, custom+jitter        │
│  → Jitter prevents Thundering Herd (same as Resilience4j)       │
│  → UnrecoverableError skips remaining attempts immediately      │
│  → Not all errors should retry: 4xx = don't retry, 5xx = retry  │
│                                                                 │
│  DELAYED JOBS                                                   │
│  → Stored in ZSET, score = unix timestamp of execution          │
│  → Scheduler polls ZRANGEBYSCORE 0 now periodically             │
│  → Moves due jobs from delayed → wait                           │
│  → Same mechanism used internally by retries AND repeatable     │
│                                                                 │
│  REPEATABLE JOBS                                                │
│  → Cron or fixed interval                                       │
│  → Next occurrence scheduled after current one completes        │
│  → Survive Redis restarts IF persistence is enabled             │
│  → Use stable jobId to prevent duplicate schedule entries       │
│  → Multiple containers: only one worker processes each run      │
│                                                                 │
│  DEDUPLICATION                                                  │
│  → Deterministic jobId = only one job per uniqueness constraint │
│  → Second add() with same jobId is silently ignored             │
│  → Design jobId to encode exactly the constraint you need       │
│                                                                 │
│  PRIORITY                                                       │
│  → Stored in separate prioritized ZSET, lower score = higher    │
│  → User-triggered jobs jump to front, scheduled jobs follow     │
│  → Starvation risk — mitigate with off-peak windows             │
│                                                                 │
│  MEMORY MANAGEMENT                                              │
│  → removeOnComplete: 24h + 1,000 count ceiling                  │
│  → removeOnFail: 7 days + 5,000 count ceiling                   │
│  → Completed data already in PostgreSQL — Redis is temporary    │
│  → Failed jobs need longer retention for investigation          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 6 done. Ready for Chapter 7 — Failure Handling and Production Resilience — covering what happens when a worker crashes mid-job, what happens when Redis goes down, the failed job as a dead letter queue equivalent, idempotency implementation per job type, Bull Board, and Datadog alerting via QueueEvents.