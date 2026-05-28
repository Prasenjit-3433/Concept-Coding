Writing Chapter 7 now.

---

# BullMQ — Chapter 7: Failure Handling and Production Resilience

---

## Why This Chapter Exists

Chapter 6 covered how to configure jobs to handle *expected* failures — retries, backoff, priority. But production systems fail in ways you don't configure for. Workers crash mid-job. Redis goes down. A bug in your processor corrupts job state. A job stalls silently and nobody notices for hours.

This chapter is about the *unexpected* failures — and how BullMQ, Redis, and the FinVerse team's practices handle them.

By the end, you'll be able to answer every "what happens when X fails" question an interviewer throws at you. Not from memory, but because you understand the mechanics at the Redis level.

---

## Failure Mode 1: Worker Crashes Mid-Job

This is the most common failure scenario and the one BullMQ's lock mechanism was specifically designed for.

### The Setup

Recall from Chapter 4: when a worker picks up a job, it:
1. Atomically moves the job from `wait` to `active` via `LMOVE`
2. Sets a lock key with a 30-second TTL: `bull:{queue}:{jobId}:lock`
3. Renews the lock every 15 seconds while processing

The crash scenario:

```
WORKER CRASH — TIMELINE

T+0s:   Worker A picks up job_4821 (INITIAL_SYNC for usr_123)
        bull:transaction-sync:active = ["job_4821"]
        bull:transaction-sync:job_4821:lock = "token-wA"  TTL: 30s

T+12s:  Worker A renews lock
        bull:transaction-sync:job_4821:lock = "token-wA"  TTL: 30s (reset)

T+18s:  Worker A's container is killed by OOM (Out of Memory)
        Process exits. Lock renewal stops.

T+48s:  Lock TTL expires (18s + 30s TTL)
        bull:transaction-sync:job_4821:lock key is GONE from Redis

BullMQ stalled job detection runs every stalledInterval (30s):

  Lua script runs atomically on Redis:
  "For every job in active list:
     Does its lock key still exist?
     IF NO → this job is stalled"

  Finds job_4821 in active — no lock key
  → job_4821 is STALLED

BullMQ handles stalled job:
  Checks: stalledCount(job_4821) < maxStalledCount(2)?
    YES →
      LREM bull:transaction-sync:active 0 "job_4821"
      HSET bull:transaction-sync:job_4821
           stalledCount "1"
      LPUSH bull:transaction-sync:wait "job_4821"
      → job returns to wait list
      → any available worker picks it up
      → processing starts again from the beginning

    NO (stalledCount >= maxStalledCount) →
      LREM bull:transaction-sync:active 0 "job_4821"
      ZADD bull:transaction-sync:failed (now) "job_4821"
      → job moves to failed permanently
      → QueueEvents emits 'stalled' event followed by 'failed'
      → Datadog alert fires
```

The full picture as a diagram:

```
STALLED JOB FLOW

┌────────────────────────────────────────────────────────────────┐
│                                                                │
│   Worker A                    Redis                            │
│   Container                                                    │
│   ┌────────┐                                                   │
│   │ active │──picks up job──►  active: [job_4821]              │
│   │        │                   lock:   job_4821  ←TTL:30s      │
│   │ process│                                                   │
│   │  ing.. │──renews lock──►   lock:   job_4821  ←TTL:30s      │
│   │        │                                                   │
│   │ CRASH  │                                                   │
│   └────────┘                                                   │
│                                                                │
│   (container dead)            lock TTL expires after 30s       │
│                               lock key GONE                    │
│                                                                │
│                               stalled check fires:             │
│                               job_4821 in active, no lock      │
│                               → stalled                        │
│                               → back to wait                   │
│                                                                │
│   Worker B                                                     │
│   Container                                                    │
│   ┌────────┐                                                   │
│   │ picks  │◄──job returns─── wait: [job_4821]                 │
│   │ up     │                                                   │
│   │job_4821│                                                   │
│   │process │                                                   │
│   │ again  │                                                   │
│   └────────┘                                                   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**The consequence you must understand:** job_4821 will be processed again from the beginning. Worker B has no idea what Worker A completed before crashing. This is **at-least-once delivery** — the job will definitely complete, but it may run more than once.

**The requirement this creates:** your processor must be **idempotent**.

---

### Idempotency: The Contract BullMQ Requires

Idempotency means: running the same job twice produces the same result as running it once. No duplicates, no corrupt state, no errors on the second run.

This is not automatic. You have to design for it. Let's look at how FinVerse implements idempotency for each job type:

```
┌─────────────────────────────────────────────────────────────────┐
│              IDEMPOTENCY STRATEGY PER JOB TYPE                  │
├───────────────────────┬─────────────────────────────────────────┤
│  Job Type             │  Idempotency Mechanism                  │
├───────────────────────┼─────────────────────────────────────────┤
│  INITIAL_SYNC /       │  externalId UNIQUE constraint on        │
│  PERIODIC_SYNC        │  transactions table.                    │
│                       │                                         │
│                       │  Worker inserts with:                   │
│                       │  prisma.transaction.upsert({            │
│                       │    where: { externalId: tx.id },        │
│                       │    update: {},  ← no-op if exists       │
│                       │    create: { ...tx }                    │
│                       │  })                                     │
│                       │                                         │
│                       │  Second run: upsert finds existing      │
│                       │  rows, does nothing. Clean completion.  │
├───────────────────────┼─────────────────────────────────────────┤
│  MONTHLY_INVESTMENT   │  InvestmentOrder has UNIQUE constraint  │
│  _ORDER               │  on (userId, portfolioId, month).       │
│                       │                                         │
│                       │  If order already exists → skip         │
│                       │  payment and return early.              │
│                       │  Never charge twice for same month.     │
├───────────────────────┼─────────────────────────────────────────┤
│  TAX_REPORT_          │  S3 key includes userId + taxYear.      │
│  GENERATION           │  PDF upload overwrites existing file.   │
│                       │  PostgreSQL upsert on (userId, year).   │
│                       │                                         │
│                       │  Second run: same PDF regenerated,      │
│                       │  same S3 key overwritten, same DB       │
│                       │  record updated. Identical end state.   │
├───────────────────────┼─────────────────────────────────────────┤
│  OUTBOX_PUBLISHER     │  OutboxEvent has status field.          │
│                       │  Worker checks status before publish:   │
│                       │                                         │
│                       │  IF status === 'PUBLISHED' → skip       │
│                       │  IF status === 'PENDING'  → publish     │
│                       │                                         │
│                       │  Atomic status update after publish     │
│                       │  prevents double-publishing.            │
└───────────────────────┴─────────────────────────────────────────┘
```

The Java parallel here is exactly what your Spring Boot notes describe for `@Transactional` with distributed systems — you can't rely on the transaction wrapping the entire work unit if the work spans network calls. You design for idempotency at the data layer instead.

---

## Failure Mode 2: Processor Throws Unrecoverable Error

Covered in Chapter 6 but worth revisiting from the resilience perspective.

When a job throws `UnrecoverableError`, BullMQ immediately moves it to `failed` without attempting retries. This is intentional — some failures are not transient:

```typescript
async process(job: Job): Promise<void> {
  const { userId } = job.data

  // Check if user still exists before doing expensive work
  const user = await this.userService.findById(userId)

  if (!user) {
    // User was deleted between job enqueue and execution
    // Retrying won't help — the user is gone
    // Don't waste 3 retry attempts
    throw new UnrecoverableError(
      `User ${userId} no longer exists — job cannot be processed`
    )
  }

  if (!user.isActive) {
    // User deactivated their account
    throw new UnrecoverableError(
      `User ${userId} account is deactivated`
    )
  }

  // Proceed with actual work
  await this.syncTransactions(user)
}
```

When this hits `failed`, QueueEvents fires and Datadog logs it — but without the urgency of a retry-exhausted failure. The alert message includes the `UnrecoverableError` text, so the on-call engineer immediately knows it's a data state issue, not a system failure.

---

## Failure Mode 3: What Happens When Redis Goes Down

This is the scary one. And it deserves complete honesty.

**BullMQ is entirely dependent on Redis.** Redis is not just a cache here — it is the job store, the lock system, the scheduler, the state machine. If Redis is unreachable, BullMQ stops working entirely.

Let's trace what happens at each layer:

```
REDIS OUTAGE — WHAT HAPPENS

DURING the outage:

  Producers (queue.add()):
    → Redis connection error thrown
    → queue.add() rejects its Promise
    → Your API handler catches it
    → HTTP 503 returned to client (or queued in memory — dangerous)

  Workers (actively processing):
    → Lose connection to Redis
    → Cannot renew locks
    → Cannot mark jobs complete or failed
    → Cannot pick up new jobs
    → Currently-in-flight jobs: continue executing in memory
      BUT cannot update Redis state when done

  Workers (between jobs, polling):
    → Poll attempts fail
    → Workers enter retry/reconnect loop
    → No new jobs processed

  BullMQ's reconnection behaviour:
    → Built-in exponential backoff reconnection to Redis
    → Workers keep trying to reconnect automatically
    → When Redis comes back: workers resume normally
    → In-flight jobs that finished during outage:
       Try to update Redis state → succeed on reconnection

AFTER Redis recovers:

  Locks that weren't renewed:
    → TTL may have expired during outage
    → stalled check fires → jobs back to wait
    → Processed again (at-least-once delivery)
    → Idempotency handles this correctly

  Jobs that were being processed:
    → If processor completed → tries to move to completed
    → If Redis was down longer than lock TTL → job is stalled
    → Will be reprocessed after recovery
```

**FinVerse's production setup to minimise Redis downtime:**

```
AWS ElastiCache — Multi-AZ Configuration

  Primary Node (AZ-a)
  ┌──────────────────────────┐
  │  All writes go here      │
  │  Replicates to replica   │
  └──────────────────────────┘
           │ async replication
           ▼
  Replica Node (AZ-b)
  ┌──────────────────────────┐
  │  Reads can come here     │
  │  Promoted on failure     │
  └──────────────────────────┘

Automatic failover:
  If primary node fails:
  ElastiCache detects within ~30 seconds
  Promotes replica to primary
  DNS endpoint updated automatically
  BullMQ reconnects to new primary

Total downtime during failover: ~30-60 seconds
Jobs in flight during that window: handled by idempotency on recovery

AOF persistence enabled:
  Every write operation appended to disk
  If entire ElastiCache cluster fails (extremely rare):
  Restore from AOF → all job state recovers
  Some data loss possible (last few seconds of writes)
```

**The honest answer for interviews:**

"Redis going down is the worst-case failure for BullMQ. We mitigate it with Multi-AZ ElastiCache with automatic failover — typical downtime is under a minute. Our workers reconnect automatically. Any jobs that were in flight get reprocessed after recovery because of the stalled job detection mechanism. Our processors are designed to be idempotent, so reprocessing the same job doesn't cause data corruption. We accept that BullMQ cannot guarantee exactly-once processing — we design for at-least-once and handle duplicates at the data layer."

This is the same trade-off discussion from your Spring Boot notes on transaction management — distributed systems cannot achieve exactly-once without significant complexity. At-least-once with idempotency is the industry-standard approach.

---

## Failure Mode 4: The Failed Job as a Dead Letter Queue

In RabbitMQ (covered in your Module 6 notes), the Dead Letter Queue (DLQ) is where messages go after they're rejected or TTL-expired. You inspect the DLQ to understand what failed and why.

BullMQ's `failed` sorted set is the exact equivalent.

```
BULLMQ FAILED STATE = RABBITMQ DEAD LETTER QUEUE

RabbitMQ DLQ:
  message → retried → retried → retried → DLQ
  DLQ holds: full message payload + reason it was dead-lettered

BullMQ failed:
  job → retried → retried → retried → failed ZSET
  failed ZSET holds: jobId reference
  bull:{queue}:{jobId} Hash holds:
    data        → full original payload
    failedReason → last error message
    stacktrace  → full stack trace of last failure
    attemptsMade → how many times it was tried
    timestamp   → when it was originally enqueued
    processedOn → when it was last picked up
    finishedOn  → when it entered failed state
```

This information is available programmatically and through Bull Board. Let's look at both.

---

### Inspecting Failed Jobs Programmatically

```typescript
// In an admin endpoint or script — for engineer investigation

@Injectable()
export class JobAdminService {
  constructor(
    @InjectQueue('transaction-sync')
    private readonly syncQueue: Queue,
  ) {}

  // Get all failed jobs with their payloads and errors
  async getFailedJobs(start = 0, end = 50): Promise<FailedJobInfo[]> {
    const failedJobs = await this.syncQueue.getFailed(start, end)

    return failedJobs.map(job => ({
      id: job.id,
      name: job.name,
      data: job.data,               // full payload
      failedReason: job.failedReason,
      stacktrace: job.stacktrace,
      attemptsMade: job.attemptsMade,
      timestamp: new Date(job.timestamp).toISOString(),
      finishedOn: new Date(job.finishedOn).toISOString(),
    }))
  }

  // Retry a specific failed job manually
  async retryFailedJob(jobId: string): Promise<void> {
    const job = await this.syncQueue.getJob(jobId)

    if (!job) {
      throw new NotFoundException(`Job ${jobId} not found`)
    }

    // Moves job from failed → wait
    // Resets attemptsMade to 0 (full retry budget restored)
    await job.retry()
  }

  // Retry ALL failed jobs in bulk
  async retryAllFailed(): Promise<number> {
    const failedJobs = await this.syncQueue.getFailed(0, -1)  // -1 = all

    await Promise.all(failedJobs.map(job => job.retry()))

    return failedJobs.length
  }

  // Discard a failed job — when you've investigated and
  // decided it should not be retried (user deleted, etc.)
  async discardFailedJob(jobId: string): Promise<void> {
    const job = await this.syncQueue.getJob(jobId)
    await job?.remove()   // removes the job Hash and from failed ZSET
  }
}
```

This is exactly how RabbitMQ DLQ replay works — inspect the message, fix the underlying issue, re-enqueue. BullMQ just gives you a cleaner API for it.

---

### Bull Board: Visual Inspection and Management

Bull Board is an open-source dashboard that gives you a web UI over your BullMQ queues. Think of it as the visual equivalent of RabbitMQ's management UI.

```
BULL BOARD — WHAT IT SHOWS

For each queue:
┌─────────────────────────────────────────────────────────────────┐
│  Queue: transaction-sync                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Waiting: 47    Active: 10    Completed: 1,203           │   │
│  │  Delayed: 0     Failed: 3     Paused: No                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Failed Jobs:                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ID: initial-sync-usr_456                                │   │
│  │  Name: INITIAL_SYNC                                      │   │
│  │  Data: { userId: 'usr_456', accountIds: ['acc_7'] }      │   │
│  │  Error: GoCardless 503: service unavailable              │   │
│  │  Attempts: 3/3                                           │   │
│  │  Failed at: 2024-01-15 14:23:11                          │   │
│  │  [Retry] [Discard] [View Stack Trace]                    │   │ 
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Active Jobs (currently running):                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ID: initial-sync-usr_123                                │   │
│  │  Progress: 2/3 accounts synced                           │   │
│  │  Running for: 12 seconds                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

Setting up Bull Board in FinVerse's Core Product Service:

```typescript
// bull-board.setup.ts

import { createBullBoard } from '@bull-board/api'
import { BullMQAdapter } from '@bull-board/api/bullMQAdapter'
import { ExpressAdapter } from '@bull-board/express'

export function setupBullBoard(
  app: INestApplication,
  queues: Queue[]
): void {
  const serverAdapter = new ExpressAdapter()
  serverAdapter.setBasePath('/admin/queues')

  createBullBoard({
    queues: queues.map(q => new BullMQAdapter(q)),
    serverAdapter,
  })

  // Mount on the Express app underneath NestJS
  app.use('/admin/queues', serverAdapter.getRouter())
}

// main.ts
async function bootstrap() {
  const app = await NestFactory.create(AppModule)

  const transactionSyncQueue = app.get<Queue>(
    getQueueToken('transaction-sync')
  )
  const taxQueue = app.get<Queue>(
    getQueueToken('tax-report-generation')
  )

  setupBullBoard(app, [transactionSyncQueue, taxQueue])

  await app.listen(3000)
}
```

**Security:** Bull Board's `/admin/queues` endpoint must be protected. In FinVerse, it sits behind the API Gateway JWT check with `hasRole('ADMIN')` — the same mechanism from your Spring Security notes. It never faces the public internet.

---

## Failure Mode 5: Silent Failures — Jobs That Never Move

The most insidious failure mode is a job that doesn't crash — it just gets stuck. The processor is running, the lock is being renewed, but the job is making no progress. GoCardless API is hanging indefinitely with no timeout. The worker is waiting forever.

Without a timeout, this job will hold its slot in `active` forever. `concurrency: 10` slots means this one stuck job takes up 10% of your worker capacity. If 10 such jobs exist, the worker is completely paralysed.

**Solution: Job-level timeout**

```typescript
// Queue configuration
BullModule.registerQueue({
  name: 'transaction-sync',
  defaultJobOptions: {
    // If a job runs for more than 60 seconds,
    // BullMQ forcefully terminates the processor
    // and marks the job as failed
    timeout: 60_000,    // 60 seconds
  },
})
```

When a job hits its timeout:

```
JOB TIMEOUT FLOW

T+0s:    Worker picks up job_4821
         Processor starts running
         Calls GoCardless API

T+60s:   timeout threshold hit
         BullMQ throws MoveToAbandonedError internally
         Processor function is forcefully interrupted

         bull:transaction-sync:active → job_4821 removed
         bull:transaction-sync:failed → job_4821 added
         bull:transaction-sync:job_4821.failedReason =
           "Job timed out after 60000ms"

         If attempts remain: moves to delayed for retry
         If attempts exhausted: stays in failed permanently
```

**Timeout values per FinVerse queue:**

```typescript
'transaction-sync'       → timeout: 120_000   // 2 min (large accounts)
'budget-check'           → timeout: 10_000    // 10 sec (should be fast)
'tax-report-generation'  → timeout: 600_000   // 10 min (heavy PDF work)
'outbox-publisher'       → timeout: 15_000    // 15 sec (RabbitMQ publish)
```

---

## Monitoring: QueueEvents and Datadog Integration

You cannot operate production BullMQ without observability. BullMQ provides `QueueEvents` — a class that subscribes to Redis pub/sub channels and emits events for every state transition.

Here is what FinVerse monitors:

```typescript
// queue-monitoring.service.ts

@Injectable()
export class QueueMonitoringService implements OnModuleInit {
  private readonly logger = new Logger(QueueMonitoringService.name)

  constructor(
    @InjectQueue('transaction-sync')
    private readonly syncQueue: Queue,
  ) {}

  async onModuleInit(): Promise<void> {
    // QueueEvents connects to Redis pub/sub
    // Different Redis connection from the worker (important!)
    const queueEvents = new QueueEvents('transaction-sync', {
      connection: redisConnection,
    })

    // JOB COMPLETED
    queueEvents.on('completed', ({ jobId, returnvalue }) => {
      // Datadog custom metric: increment counter for each
      // completed job — used for throughput tracking
      datadogClient.increment('bullmq.job.completed', 1, [
        'queue:transaction-sync',
        `job_type:${returnvalue?.jobName ?? 'unknown'}`,
      ])
    })

    // JOB FAILED
    queueEvents.on('failed', ({ jobId, failedReason }) => {
      this.logger.error(
        `Job ${jobId} failed: ${failedReason}`,
        { jobId, failedReason }
      )

      // Datadog alert — any failed job triggers notification
      datadogClient.increment('bullmq.job.failed', 1, [
        'queue:transaction-sync',
      ])
      // Datadog monitor watches this counter
      // If count > 0 in last 5 minutes → Slack alert
    })

    // JOB STALLED
    queueEvents.on('stalled', ({ jobId }) => {
      this.logger.warn(`Job ${jobId} stalled — worker may have crashed`)

      datadogClient.increment('bullmq.job.stalled', 1, [
        'queue:transaction-sync',
      ])
      // Stalled jobs are usually a signal of worker crash or OOM
      // Worth alerting on even though BullMQ recovers automatically
    })

    // QUEUE DEPTH (polled separately — QueueEvents doesn't push this)
    setInterval(async () => {
      const [waiting, active, delayed, failed] = await Promise.all([
        this.syncQueue.getWaitingCount(),
        this.syncQueue.getActiveCount(),
        this.syncQueue.getDelayedCount(),
        this.syncQueue.getFailedCount(),
      ])

      datadogClient.gauge('bullmq.queue.waiting', waiting, [
        'queue:transaction-sync',
      ])
      datadogClient.gauge('bullmq.queue.active', active, [
        'queue:transaction-sync',
      ])
      datadogClient.gauge('bullmq.queue.failed', failed, [
        'queue:transaction-sync',
      ])
      datadogClient.gauge('bullmq.queue.delayed', delayed, [
        'queue:transaction-sync',
      ])
    }, 60_000)
  }
}
```

---

### Datadog Monitors: What Triggers an Alert

```
DATADOG ALERT RULES FOR BULLMQ

┌─────────────────────────────────────────────────────────────────┐
│  Alert 1: Job Failures                                          │
│  Metric: bullmq.job.failed > 0 (last 5 min)                     │
│  Severity: HIGH                                                 │
│  Notification: #incidents Slack                                 │
│  Meaning: A job exhausted all retries — needs investigation     │
├─────────────────────────────────────────────────────────────────┤
│  Alert 2: Queue Backlog                                         │
│  Metric: bullmq.queue.waiting > 500 (last 10 min)               │
│  Severity: MEDIUM                                               │
│  Notification: #core-product-team Slack                         │
│  Meaning: Workers can't keep up — scale out or investigate      │
├─────────────────────────────────────────────────────────────────┤
│  Alert 3: No Active Jobs During Sync Window                     │
│  Metric: bullmq.queue.active == 0 AND time between 08:00-10:00  │
│  Severity: HIGH                                                 │
│  Notification: #incidents Slack                                 │
│  Meaning: Worker containers are all down during peak window     │
├─────────────────────────────────────────────────────────────────┤
│  Alert 4: Stalled Jobs Spike                                    │
│  Metric: bullmq.job.stalled > 5 (last 10 min)                   │
│  Severity: MEDIUM                                               │
│  Notification: #core-product-team Slack                         │
│  Meaning: Multiple worker crashes — OOM? Memory leak? Deploy?   │
├─────────────────────────────────────────────────────────────────┤
│  Alert 5: Failed Job Count Growing                              │
│  Metric: bullmq.queue.failed > 50 (absolute count)              │
│  Severity: LOW (informational)                                  │
│  Notification: Daily digest                                     │
│  Meaning: Accumulated failures — review and replay/discard      │
└─────────────────────────────────────────────────────────────────┘
```

---

### How You Actually Respond to an Alert

This is the part interviewers love — "walk me through what you do when an alert fires."

**Scenario: Alert 1 fires at 14:23 — job failure in transaction-sync queue**

```
INCIDENT RESPONSE WORKFLOW

Step 1: Check Datadog APM
  → Find the trace for the failed job
  → Distributed trace shows: API call → BullMQ enqueue → Worker pickup
  → Error at Worker: "GoCardless 503: service unavailable"
  → Stack trace shows exactly which line threw

Step 2: Check Bull Board
  → Navigate to /admin/queues/transaction-sync/failed
  → Find the job: initial-sync-usr_456
  → Payload: { userId: 'usr_456', accountIds: ['acc_7'] }
  → Error: GoCardless 503
  → attemptsMade: 3/3

Step 3: Check GoCardless status page
  → GoCardless had an incident from 14:15-14:20
  → Resolved at 14:20
  → Our job was retrying during the incident, exhausted retries

Step 4: Resolution
  → GoCardless is now healthy
  → Click "Retry" on the failed job in Bull Board
  → OR call admin endpoint: POST /admin/jobs/retry/initial-sync-usr_456
  → Job moves back to wait
  → Worker picks it up, GoCardless is healthy, sync succeeds

Step 5: Post-incident note
  → This was a GoCardless incident, not our code
  → Consider increasing maxAttempts during external service
     incidents (or adding a circuit breaker — future improvement)
```

---

## The Complete Failure Mode Map

```
┌─────────────────────────────────────────────────────────────────┐
│              ALL FAILURE MODES AND RESPONSES                    │
├────────────────────────┬────────────────────────────────────────┤
│  Failure Mode          │  BullMQ Response                       │
├────────────────────────┼────────────────────────────────────────┤
│  Worker crashes        │  Lock expires → stalled detected →     │
│  mid-job               │  job back to wait → reprocessed        │
│                        │  (at-least-once delivery)              │
├────────────────────────┼────────────────────────────────────────┤
│  Processor throws      │  Retry with backoff → delayed →        │
│  retryable error       │  wait → reprocessed                    │
│  (5xx, network)        │  If all attempts fail → failed         │
├────────────────────────┼────────────────────────────────────────┤
│  Processor throws      │  Immediately to failed                 │
│  UnrecoverableError    │  No retries wasted                     │
│  (4xx, data issue)     │  Alert fires                           │
├────────────────────────┼────────────────────────────────────────┤
│  Job times out         │  Treated as failure                    │
│  (processor hangs)     │  Retried if attempts remain            │
│                        │  Failed if attempts exhausted          │
├────────────────────────┼────────────────────────────────────────┤
│  Redis goes down       │  Producers: queue.add() throws         │
│                        │  Workers: reconnect automatically      │
│                        │  In-flight jobs: may stall after       │
│                        │  recovery (handled by idempotency)     │
├────────────────────────┼────────────────────────────────────────┤
│  Duplicate job         │  deterministic jobId prevents it       │
│  enqueue               │  Second add() silently ignored         │
├────────────────────────┼────────────────────────────────────────┤
│  Job stalls multiple   │  maxStalledCount reached →             │
│  times (crash loop)    │  moved to failed permanently           │
│                        │  Alert fires → human investigates      │
├────────────────────────┼────────────────────────────────────────┤
│  Silent stuck job      │  timeout config → forceful termination │
│  (hanging I/O)         │  Treated as failure → retried/failed   │
└────────────────────────┴────────────────────────────────────────┘
```

---

## Chapter 7 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 7 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  WORKER CRASH                                                   │
│  → Lock TTL expires after 30s without renewal                   │
│  → stalled check detects job in active with no lock             │
│  → job returns to wait, reprocessed by another worker           │
│  → at-least-once delivery → processor MUST be idempotent        │
│                                                                 │
│  IDEMPOTENCY STRATEGIES                                         │
│  → Upsert with externalId unique constraint (transactions)      │
│  → Check-before-charge with unique constraint (investments)     │
│  → Overwrite-safe operations (PDF upload to fixed S3 key)       │
│  → Status check before action (outbox publisher)                │
│                                                                 │
│  REDIS OUTAGE                                                   │
│  → BullMQ completely dependent on Redis                         │
│  → Mitigate: Multi-AZ ElastiCache + automatic failover          │
│  → Workers auto-reconnect after recovery                        │
│  → Accept at-least-once, design for idempotency                 │
│                                                                 │
│  FAILED JOBS (= BullMQ's DLQ)                                   │
│  → Full payload + error + stack trace in job Hash               │
│  → Inspect via Bull Board (visual) or programmatic API          │
│  → Replay: job.retry() moves back to wait, resets attempts      │
│  → Discard: job.remove() when not worth retrying                │
│                                                                 │
│  SILENT FAILURES                                                │
│  → timeout config forcefully terminates hung processors         │
│  → Different timeout per queue (2 min sync, 10 min tax)         │
│                                                                 │
│  OBSERVABILITY                                                  │
│  → QueueEvents subscribes to Redis pub/sub                      │
│  → Emits: completed, failed, stalled events                     │
│  → Queue depth polled every 60s → Datadog gauges                │
│  → 5 alert rules: failures, backlog, dead workers,              │
│    stall spikes, accumulated failures                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 7 done. Ready for Chapter 8 — BullMQ at FinVerse: Every Queue, Every Decision — where we do the complete queue map, the definitive BullMQ vs RabbitMQ side-by-side for FinVerse's specific use cases, and write the STAR format stories for your interview answers.