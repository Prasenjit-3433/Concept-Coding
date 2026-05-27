# BullMQ — Chapter 5: Concurrency, Worker Pools, and Scaling

---

## Why This Chapter Exists

You now know what BullMQ is, how Redis stores job state, and how the lock mechanism works. But there's a gap between understanding the mechanics and being able to make real production decisions.

In an interview, the question won't be "explain the Redis key structure." It will be "how did you decide on concurrency 10?" or "your queue was backing up — what did you do?" or "how do you make sure two workers don't process the same job?"

This chapter answers all of those questions from first principles.

---

## What `concurrency: N` Actually Does Inside the Worker

Let's be completely precise about this, because it's one of the most commonly misunderstood things about BullMQ.

When you write:

```typescript
@Processor('transaction-sync', { concurrency: 10 })
```

You are not creating 10 threads. You are not creating 10 processes. You are telling BullMQ's internal scheduler: **maintain up to 10 jobs in the `active` state simultaneously, within this single Worker instance.**

Here is what BullMQ's internal scheduler loop looks like conceptually:

```
BULLMQ INTERNAL SCHEDULER (simplified)
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  while (true) {                                                 │
│                                                                 │
│    currentActive = count jobs in active state for this worker   │
│                                                                 │
│    slotsAvailable = concurrency - currentActive                 │
│                                                                 │
│    if (slotsAvailable > 0) {                                    │
│      job = await pickNextJobFromWaitList()                      │
│      if (job) {                                                 │
│        // Do NOT await this — fire and move on                  │
│        processJob(job)   ← starts async, doesn't block          │
│        currentActive++                                          │
│      }                                                          │
│    }                                                            │
│                                                                 │
│    // When a job finishes (success or failure):                 │
│    // currentActive-- and the loop fills the slot again         │
│                                                                 │
│    await sleep(pollingInterval)                                 │
│  }                                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The key line is `processJob(job)` — it is called **without await**. BullMQ fires the job processor as an async operation and immediately loops back to check if more slots are available. The event loop handles all 10 running processors concurrently — none of them block the others.

Let's visualise this across time:

```
concurrency: 5 — what actually happens on the event loop

Time ─────────────────────────────────────────────────────────────►

Job 1: [await goCardless....................][await prisma....][done]
Job 2:   [await goCardless.........................][await prisma][done]
Job 3:     [await goCardless............][await prisma.........][done]
Job 4:       [await goCardless...................][done]
Job 5:         [await goCardless..............................][done]
                                                               │
                                                    Job 6 fills the slot
                                                    as soon as Job 4 finishes

Event loop: ─────────────────────────────────────────────────────►
  (never blocked — only runs JS code between I/O awaits)
```

During the `await goCardless...` wait, the event loop is free. It can advance Job 2, Job 3, Job 4, Job 5 — all at the same time, from the event loop's perspective. All those I/O calls are in flight at the OS level simultaneously.

---

## The Right Mental Model: Slots, Not Threads

The cleanest way to think about `concurrency: N` is as **N slots**.

```
CONCURRENCY AS SLOTS

concurrency: 5

┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│ Slot 1│ │ Slot 2│ │ Slot 3│ │ Slot 4│ │ Slot 5│
│       │ │       │ │       │ │       │ │       │
│ Job 1 │ │ Job 2 │ │ Job 3 │ │ Job 4 │ │ Job 5 │
│active │ │active │ │active │ │active │ │active │
└───────┘ └───────┘ └───────┘ └───────┘ └───────┘

All 5 slots filled → no new jobs picked up until a slot opens.

Job 4 finishes:

┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│ Slot 1│ │ Slot 2│ │ Slot 3│ │ Slot 4│ │ Slot 5│
│       │ │       │ │       │ │       │ │       │
│ Job 1 │ │ Job 2 │ │ Job 3 │ │  OPEN │ │ Job 5 │
│active │ │active │ │active │ │       │ │active │
└───────┘ └───────┘ └───────┘ └───────┘ └───────┘

BullMQ immediately picks Job 6 from wait list → fills Slot 4.
```

Java comparison: this is conceptually similar to `ThreadPoolExecutor` with `corePoolSize = maxPoolSize = N` and an unbounded queue. Except instead of OS threads, these are async operations on a single event loop thread.

---

## How to Choose the Right Concurrency Number

This is a question interviewers love, because there's no universal right answer — it depends on the job type. Here is the reasoning framework:

```
┌─────────────────────────────────────────────────────────────────┐
│              HOW TO REASON ABOUT CONCURRENCY                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  QUESTION 1: Is the job I/O-bound or CPU-bound?                 │
│                                                                 │
│  I/O-bound (GoCardless calls, DB queries, Redis reads):         │
│    → High concurrency is fine.                                  │
│    → The event loop is mostly idle during I/O waits.            │
│    → Concurrency 10, 20, even 50 can work well.                 │
│                                                                 │
│  CPU-bound (PDF generation, heavy calculations):                │
│    → High concurrency HURTS you.                                │
│    → Each job hogs the event loop.                              │
│    → Concurrency 1 or 2 is often correct.                       │
│    → Better answer: extract to Go/Rust service.                 │
│                                                                 │
│  QUESTION 2: What are the downstream constraints?               │
│                                                                 │
│  GoCardless rate limit: 50 requests/second per API key.         │
│    → Each sync job makes 1 API call per account.                │
│    → Users average 2.5 accounts each.                           │
│    → concurrency 10 = up to 25 API calls in flight.             │
│    → With 3 containers: up to 75 calls — approaching limit.     │
│    → Solution: add rate limiter in worker config.               │
│                                                                 │
│  PostgreSQL connection pool: HikariCP default = 10 connections. │
│    → With concurrency 10, each job might need a DB connection.  │
│    → 10 concurrent jobs × 3 containers = 30 simultaneous        │
│       DB connections needed.                                    │
│    → Make sure your connection pool is sized accordingly.       │
│                                                                 │
│  QUESTION 3: What does memory pressure look like?               │
│                                                                 │
│  Each in-flight job holds its data in memory.                   │
│  Transaction sync for a user with 3 years of history            │
│  might hold ~5MB of transaction data in memory at once.         │
│  concurrency 10 × 5MB = 50MB peak memory per worker.            │
│  Your container has 2GB — this is fine.                         │
│  But 5000 concurrent jobs would be 25GB — not fine.             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## FinVerse's Concurrency Settings — With Reasoning

```
┌──────────────────────────────────────────────────────────────────┐
│           FINVERSE CORE PRODUCT — CONCURRENCY DECISIONS          │
├─────────────────────┬──────────────┬──────────────────────────── │
│  Queue              │  Concurrency │  Reasoning                  │
├─────────────────────┼──────────────┼─────────────────────────────│
│  transaction-sync   │  10          │  Pure I/O-bound.            │
│                     │              │  GoCardless + PostgreSQL    │
│                     │              │  calls. Event loop mostly   │
│                     │              │  idle. 10 is safe before    │
│                     │              │  hitting rate limits.       │
├─────────────────────┼──────────────┼─────────────────────────────│
│  budget-check       │  20          │  Very lightweight.          │
│                     │              │  One DB read + one Redis    │
│                     │              │  write per job. Almost no   │
│                     │              │  CPU. High concurrency      │
│                     │              │  is fine.                   │
├─────────────────────┼──────────────┼─────────────────────────────│
│  tax-report-        │  5           │  Heavy DB reads + PDF       │
│  generation         │              │  rendering. More CPU than   │
│                     │              │  other jobs. Deliberately   │
│                     │              │  throttled to protect       │
│                     │              │  PostgreSQL during the      │
│                     │              │  annual Jan 1st run.        │
├─────────────────────┼──────────────┼─────────────────────────────│
│  outbox-publisher   │  1           │  Order matters.             │
│                     │              │  Events must publish in     │
│                     │              │  the sequence they were     │
│                     │              │  written to the outbox.     │
│                     │              │  Single-threaded by design. │
└─────────────────────┴──────────────┴─────────────────────────────┘
```

---

## Rate Limiting at the Worker Level

Concurrency controls how many jobs run simultaneously. But there's a second dimension: **how fast** jobs are picked up over time.

Without a rate limiter, if 1000 jobs are in the queue, BullMQ will pick up 10 simultaneously, finish them, pick up 10 more — as fast as Redis and the event loop allow. For GoCardless API calls, this would immediately exhaust the rate limit.

BullMQ's `limiter` config adds a time-based ceiling:

```typescript
@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,          // maximum 30 jobs processed
    duration: 10_000, // per 10-second window
    // = maximum 3 jobs per second on average
  },
})
```

How the limiter works internally:

```
RATE LIMITER — WHAT HAPPENS WHEN LIMIT IS HIT

Window 1 (0s–10s):
  Jobs processed: 30 ← limit hit at 8s (jobs were fast)
  Worker pauses picking up new jobs for remaining 2s

Window 2 (10s–20s):
  Worker resumes, picks up next batch
  Jobs processed: 30

At no point does BullMQ fail or drop jobs.
They simply stay in the wait list until capacity opens.
```

This is different from the API Gateway rate limiter (which rejects requests with 429). The worker-level rate limiter is a **throttle** — it slows down processing to stay within external API limits. No jobs are lost.

Java comparison: this is similar to `RateLimiter` from Guava — a token bucket that controls throughput.

---

## Multiple Workers Coordinating Through Redis

Now the question you've been building toward: when you have 3 worker containers, each with `concurrency: 10`, how do they coordinate without processing the same job twice?

The answer is the combination of two Redis primitives you already know from Chapter 4:

```
3 WORKER CONTAINERS — COORDINATION THROUGH REDIS

Redis: bull:transaction-sync:wait = ["job_1","job_2","job_3",...,"job_100"]

Worker Container 1 polls:
  LMOVE wait active RIGHT LEFT
  → atomically moves "job_1" to active
  → Worker 1 gets "job_1"
  → Sets lock: bull:transaction-sync:job_1:lock = "token-w1"

Worker Container 2 polls simultaneously:
  LMOVE wait active RIGHT LEFT
  → atomically moves "job_2" to active (job_1 is already gone)
  → Worker 2 gets "job_2"
  → Sets lock: bull:transaction-sync:job_2:lock = "token-w2"

Worker Container 3 polls simultaneously:
  LMOVE wait active RIGHT LEFT
  → atomically moves "job_3" to active
  → Worker 3 gets "job_3"

Key insight:
LMOVE is atomic at the Redis level.
No two workers can pop the same element.
Redis serialises these operations — even if all three
LMOVE commands arrive at Redis simultaneously,
Redis processes them one at a time, each getting a different job.
```

This is the same guarantee as `synchronized` blocks in Java — mutual exclusion, but implemented at the Redis level rather than the JVM level. The distributed lock (the `:lock` key with TTL) then handles the crash safety scenario on top.

---

## The Connection Pool Problem: A Hidden Scaling Trap

This is something most BullMQ tutorials don't mention, and it's exactly the kind of thing that causes production incidents.

When you scale from 1 worker container to 8, your total concurrent database operations jump dramatically:

```
POSTGRESQL CONNECTION MATH

1 container × concurrency 10  = up to 10 simultaneous DB queries
8 containers × concurrency 10 = up to 80 simultaneous DB queries

Your PostgreSQL connection pool (configured in Prisma):
  connectionLimit: 20   ← default in many setups

Result: 80 simultaneous queries competing for 20 connections.
60 queries are queuing for a connection.
Response times spike. Timeouts start appearing.
```

The fix is to size your connection pool to match your peak concurrency, or to use a connection pooler like PgBouncer that sits between your workers and PostgreSQL:

```
WITHOUT PGBOUNCER (naive):

[Worker 1]─┐
[Worker 2]─┤
[Worker 3]─┤
[Worker 4]─┤──────────────────────────► PostgreSQL
[Worker 5]─┤                            (limited connections)
[Worker 6]─┤
[Worker 7]─┤
[Worker 8]─┘

80 connections attempted → most waiting


WITH PGBOUNCER (production-grade):

[Worker 1]─┐
[Worker 2]─┤
[Worker 3]─┤
[Worker 4]─┤──────────► PgBouncer ──────► PostgreSQL
[Worker 5]─┤            (pools and        (consistent
[Worker 6]─┤             multiplexes       connection
[Worker 7]─┤             connections)      count)
[Worker 8]─┘
```

At FinVerse's current scale (Series A, up to 8 worker containers), the team handles this by setting `concurrency: 10` with careful connection pool sizing (`connectionLimit: 15` per worker process × 8 workers = 120 max, but jobs don't all hit the DB simultaneously — the GoCardless calls happen first). PgBouncer is on the roadmap for Series B when AUM and user count grow significantly.

---

## Horizontal Scaling: The Decision Framework

When should you increase `concurrency` vs when should you add more containers?

```
┌─────────────────────────────────────────────────────────────────┐
│         CONCURRENCY vs CONTAINER COUNT — DECISION GUIDE         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  INCREASE CONCURRENCY when:                                     │
│  ─────────────────────────                                      │
│  • Jobs are heavily I/O-bound (lots of waiting)                 │
│  • CPU usage on the container is low (< 40%)                    │
│  • Memory usage is well within limits                           │
│  • Downstream rate limits allow it                              │
│  • Queue is backing up and one container is underutilised       │
│                                                                 │
│  ADD MORE CONTAINERS when:                                      │
│  ─────────────────────────                                      │
│  • CPU is already high on existing containers (> 70%)           │
│  • Concurrency is already at a sensible ceiling                 │
│  • You need fault isolation                                     │
│    (one container crash shouldn't stop all processing)          │
│  • Queue depth is consistently high even with max concurrency   │
│  • You need geographic distribution (different AZs)             │
│                                                                 │
│  REDUCE CONCURRENCY when:                                       │
│  ─────────────────────────                                      │
│  • Hitting downstream rate limits                               │
│  • PostgreSQL connection pool exhausted                         │
│  • Memory pressure (jobs holding large payloads)                │
│  • Error rate spikes (too many simultaneous failures)           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## What Happens When a New Worker Container Starts Up

This is a subtle but important production detail. When ECS launches a new worker container during a scale-out event, BullMQ doesn't need any configuration changes or coordination. The new container:

```
NEW WORKER CONTAINER BOOT SEQUENCE

1. Container starts, Node.js process launches
2. NestJS module initialises
3. BullMQ Worker constructor runs:
   - Opens Redis connection to ElastiCache
   - Registers this worker with the queue name
   - Begins polling: LMOVE wait active
4. Redis: "a new consumer is polling this queue"
   → immediately starts distributing jobs to it
5. No configuration change needed
6. No notification to other workers needed
7. No leader election needed

Within seconds of the container starting,
it is processing jobs alongside the existing containers.
```

This is the beauty of the Redis-based coordination model — new workers are self-registering. Adding capacity is literally just launching more containers.

---

## Observability: Knowing Your Concurrency Is Right

How do you know if your concurrency setting is correct? You measure.

```
KEY METRICS TO WATCH (via Datadog + QueueEvents)

Metric 1: queue.waiting count
  Purpose: How many jobs are queued but not yet processing?
  Healthy: Near 0 most of the time, spikes during sync windows
  Warning: Consistently > 100 for more than 5 minutes
  Action:  Increase concurrency or add containers

Metric 2: job.duration (p50, p95, p99)
  Purpose: How long do jobs actually take?
  Healthy: p95 < 30 seconds for transaction-sync
  Warning: p95 climbing over time (GoCardless getting slower?)
  Action:  Investigate downstream latency

Metric 3: job.failed rate
  Purpose: What percentage of jobs fail?
  Healthy: < 0.5%
  Warning: Spike to > 2%
  Action:  Check GoCardless status page, check error messages
           in failed job payloads

Metric 4: active / concurrency ratio
  Purpose: Are your concurrency slots fully utilised?
  Healthy: active ≈ concurrency (slots are being used)
  Warning: active << concurrency with jobs in wait list
           (workers are picking up jobs slowly — Redis latency?)

Metric 5: Redis memory usage
  Purpose: Is BullMQ's job data growing unboundedly?
  Healthy: Stable, not growing
  Warning: Growing continuously
  Action:  Check removeOnComplete/removeOnFail settings
```

---

## Putting It Together: The Full Scaling Diagram

```
FINVERSE TRANSACTION SYNC — FULL SCALING PICTURE

08:00 — Periodic sync triggers (all EU users)
2,000 jobs enqueued into Redis

BEFORE AUTO-SCALE:
┌─────────────────────────────────────────────────────────────┐
│  1 Worker Container                                         │
│  concurrency: 10                                            │
│  processing: 10 jobs simultaneously                         │
│                                                             │
│  Redis wait list: 1,990 jobs waiting                        │
│  At ~3 jobs/min (rate limited): ~11 hours to drain          │
│                                                             │
│  CloudWatch: WaitingJobs = 1,990 > threshold 50             │
│  → Scale OUT triggered                                      │
└─────────────────────────────────────────────────────────────┘

AFTER AUTO-SCALE (8 containers):
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│Container1│ │Container2│ │Container3│ │Container4│
│conc: 10  │ │conc: 10  │ │conc: 10  │ │conc: 10  │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │            │
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│Container5│ │Container6│ │Container7│ │Container8│
│conc: 10  │ │conc: 10  │ │conc: 10  │ │conc: 10  │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     └────────────┴────────────┴────────────┘
                          │
                     ┌────▼────┐
                     │  Redis  │
                     │  Queue  │
                     └─────────┘

Total concurrent jobs: 8 × 10 = 80
Rate limiter: 30 jobs/10s per container = 240 jobs/10s total
Time to drain 2,000 jobs: ~85 seconds

09:30 — Queue empty, scale back to 1 container
```

---

## Chapter 5 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 5 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  concurrency: N = N async job slots on one event loop           │
│  Not threads. Not CPU cores. Concurrent async operations.       │
│                                                                 │
│  Works for I/O-bound jobs (event loop idle during waits)        │
│  Doesn't help for CPU-bound jobs (blocks event loop)            │
│                                                                 │
│  Choosing concurrency:                                          │
│  → I/O-bound? Higher concurrency (10, 20)                       │
│  → CPU-bound? Lower concurrency (1-2) or use Go/Rust            │
│  → Rate limits? Add limiter config                              │
│  → DB connections? Size pool to concurrency × containers        │
│                                                                 │
│  Multiple containers coordinate via Redis LMOVE (atomic)        │
│  No configuration needed — new containers self-register         │
│                                                                 │
│  Scale concurrency first (free), then add containers            │
│  ECS Auto Scaling based on CloudWatch queue depth metric        │
│                                                                 │
│  Hidden trap: connection pool exhaustion at scale               │
│  Fix: size pool correctly, or use PgBouncer at larger scale     │
│                                                                 │
│  Measure: waiting count, job duration, failed rate,             │
│  active/concurrency ratio, Redis memory                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 5 done. Ready for Chapter 6 — Job Lifecycle: Retries, Delays, Priorities, and Scheduling — where we cover every knob available for controlling how and when jobs run, including how delayed jobs are stored in Redis, how repeatable jobs survive restarts, and how FinVerse uses job deduplication to prevent duplicate processing at enqueue time.