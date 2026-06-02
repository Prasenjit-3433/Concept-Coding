# Month 5: The Performance Investigation

---

## Foundational Knowledge: What You Need Before This Story

Two concepts to understand clearly before the story — because the investigation depends on both of them.

### Concept 1: What p95 Latency Actually Means (and Why It Matters More Than Average)

When engineers talk about performance, they almost always use **percentiles** rather than averages. Understanding why is important — interviewers test this directly.

Imagine 100 sync job durations in seconds:

```
100 INITIAL_SYNC job durations (seconds):

8, 9, 10, 8, 11, 9, 10, 8, 9, 10,   ← batch 1 (10 jobs)
9, 10, 8, 11, 9, 10, 8, 9, 10, 9,   ← batch 2
10, 8, 9, 11, 10, 8, 9, 10, 8, 9,   ← batch 3
... (similar values for batches 4-9)
45, 52, 38, 61, 49                   ← last 5 jobs (very slow)
```

```
AVERAGE vs PERCENTILES

Average:
  Sum all 100 values / 100
  = roughly 12.3 seconds
  "Average sync time is 12 seconds" — sounds fine

p50 (median):
  The value below which 50% of jobs fall
  = 9.8 seconds
  "Half of all syncs finish in under 10 seconds"

p95:
  The value below which 95% of jobs fall
  = 28.4 seconds
  "95% of syncs finish in under 28 seconds"
  → 5 out of every 100 users wait longer than 28 seconds

p99:
  The value below which 99% of jobs fall
  = 41 seconds
  "1% of syncs take 41 seconds or longer"
  → the worst experiences

WHY AVERAGE HIDES THE PROBLEM:

  The 5 slow jobs (45s, 52s, 38s, 61s, 49s) barely move the average
  because they are outnumbered by 95 fast jobs.

  But p95 = 28.4s reveals them clearly.
  These are real users with a bad experience.
  Average would tell you "everything is fine."
  p95 tells you "5% of your users are suffering."
```

This is why the alert threshold in Datadog is set on p95, not average. And it is why when you tell an interviewer "I improved sync performance," the correct phrasing is **"p95 dropped from 28.4 seconds to 11.2 seconds"** — not "average improved."

---

### Concept 2: Sequential vs Concurrent Async Operations in Node.js

This is the root cause of the performance problem you find. Understanding it precisely means you can explain the fix clearly under cross-questioning.

```
SEQUENTIAL ASYNC (the problem):

async function syncAllAccounts(accountIds: string[]) {
  for (const accountId of accountIds) {
    await fetchAndInsert(accountId)
    // await means: start the operation,
    // WAIT for it to complete,
    // THEN move to the next iteration
  }
}

TIMELINE (user with 3 accounts, each taking ~8 seconds):

Time ────────────────────────────────────────────────────────────►

Account 1: [GoCardless call (3s)][DB insert (5s)]
Account 2:                                        [GoCardless (3s)][DB insert (5s)]
Account 3:                                                                          [GoCardless (3s)][DB insert (5s)]

Total: 8s + 8s + 8s = 24 seconds

The event loop is not blocked — Node.js handles the awaits
correctly. But the LOGICAL flow is sequential: account 2
does not start until account 1 is fully complete.
```

```
CONCURRENT ASYNC (the fix):

async function syncAllAccounts(accountIds: string[]) {
  await Promise.all(
    accountIds.map(accountId => fetchAndInsert(accountId))
  )
  // Promise.all: starts ALL operations simultaneously
  // waits for ALL of them to complete
  // total time = longest single operation, not sum of all
}

TIMELINE (same user with 3 accounts):

Time ────────────────────────────────────────────────────────────►

Account 1: [GoCardless call (3s)][DB insert (5s)]
Account 2: [GoCardless call (3s)][DB insert (5s)]
Account 3: [GoCardless call (3s)][DB insert (5s)]
           ←─────── all running simultaneously ──────────────────►

Total: 8 seconds (not 24)

All three GoCardless calls go out at the same time.
All three DB inserts run at the same time (within DB limits).
The event loop manages all of them concurrently.
```

```
WHY THIS WORKS IN NODE.JS:

GoCardless API call = network I/O = non-blocking
  → Node.js sends the HTTP request
  → Hands off to libuv / OS
  → Event loop is FREE while waiting for response
  → Can start account 2's request immediately

PostgreSQL insert = network I/O = non-blocking
  → Prisma sends the query
  → OS handles the TCP connection
  → Event loop handles other callbacks while waiting

With Promise.all, all three accounts' I/O operations
are in flight simultaneously at the OS level.
Node.js coordinates the callbacks as they complete.
No CPU parallelism needed — pure I/O concurrency.
```

The important nuance: `Promise.all` is not "unlimited parallelism." There is a GoCardless rate limit. You cannot fire 50 concurrent API calls from the same API key. The `limiter` config on the BullMQ worker handles this — it caps total throughput across all concurrent jobs.

Now the story.

---

## The Story: The Stalled Sync Jobs Investigation

---

**S — Situation:**

It is the third week of November. You are on your third week of formal ownership of the Accounts & Open Banking module.

On a Tuesday afternoon, the "Failed Jobs Accumulating" Datadog monitor fires. The Slack alert appears in `#incidents`:

```
[ALERT] finverse.bullmq.queue.failed change > 20 in last 1 hour
Queue: transaction-sync
Current failed count: 34 (was 8 one hour ago)
Runbook: notion.so/finverse/runbooks/sync-errors
Dashboard: app.datadoghq.eu/dashboard/...
```

You open Datadog. This is your module. You own the investigation.

---

**T — Task:**

Find out why sync jobs are failing, identify the root cause, propose and implement a fix, and measure the improvement before and after.

---

**A — Action:**

#### Step 1: Reading the Alert and Opening the Right View

Your first instinct is to open Bull Board and look at the failed jobs. But you remember something Lucas said during the RabbitMQ incident in month 3 — "check the metric first, it tells you what kind of problem you have before you read a single log line."

You open the Datadog metrics dashboard for `transaction-sync`. The failed jobs count is climbing. But more interesting is the `finverse.transaction_sync.duration.ms` histogram:

```
Datadog — finverse.transaction_sync.duration.ms
Filter: job_type = INITIAL_SYNC
Time window: last 7 days

p50:  15.2 seconds
p95:  28.4 seconds   ← dangerously close to the 30s lock TTL
p99:  41.0 seconds   ← already exceeding the lock TTL
```

The p99 of 41 seconds immediately tells you something important. BullMQ's default lock TTL is 30 seconds. If a job runs longer than 30 seconds without the worker renewing the lock, BullMQ considers the worker crashed and marks the job as stalled. A 41-second p99 means some jobs are regularly exceeding the lock duration.

You check what `lockDuration` is configured to in the worker:

```typescript
// transaction-sync.worker.ts — current configuration
@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,
    duration: 10_000,
  },
  stalledInterval: 30_000,
  maxStalledCount: 2,
  // lockDuration: not explicitly configured
  // → defaults to 30 seconds
})
```

Not explicitly configured. Defaults to 30 seconds. p99 is 41 seconds. There is your first signal.

---

#### Step 2: Confirming the Root Cause in APM Traces

You open Datadog APM and filter for failed `INITIAL_SYNC` spans:

```
Datadog APM — Filter:
  service: core-product
  operation: bullmq.process.INITIAL_SYNC
  status: error
  time: last 24 hours
```

Every failed span has the same error:

```
error.type:    BullMQError
error.message: "Job stalled"
attemptsMade:  2
```

Not a GoCardless error. Not a PostgreSQL error. A BullMQ stalled job error — the lock expired before the job completed.

You click into one of the failed traces to see the waterfall:

```
Datadog APM — Trace Detail

[bullmq.process.INITIAL_SYNC — 32,847ms — ERROR: Job stalled]
  │
  ├── [GoCardless GET /accounts/acc_1/transactions — 7,230ms]
  │     http.status_code: 200
  │
  ├── [prisma:createMany transactions — 4,891ms]
  │     db.rowsAffected: 1,247
  │
  ├── [GoCardless GET /accounts/acc_2/transactions — 8,102ms]
  │     http.status_code: 200
  │
  ├── [prisma:createMany transactions — 5,340ms]
  │     db.rowsAffected: 892
  │
  ├── [GoCardless GET /accounts/acc_3/transactions — 6,994ms]
  │     http.status_code: 200
  │
  └── [prisma:createMany transactions — ???ms]
        ← span ends here — lock expired, worker killed
```

The waterfall tells the full story. The spans are **sequential**: GoCardless call for account 1, then DB insert for account 1, then GoCardless call for account 2, then DB insert for account 2, then GoCardless call for account 3, then the lock expires mid-insert for account 3.

For a user with 3 accounts this job is taking 32 seconds. With 4 accounts it would take even longer. The lock TTL of 30 seconds is not enough.

---

#### Step 3: Correlating With Logs to Understand the User Pattern

You use the `traceId` from one of the failing spans to filter logs in Datadog Log Explorer:

```
Datadog Log Explorer — Filter:
  traceId: 9e7d21299f4ea8a1
  service: core-product

Results:

14:23:11  INFO  Sync job started
  { jobId: "initial-sync-usr_789", userId: "usr_789",
    accountCount: 4, attemptsMade: 0 }

14:23:14  INFO  Account synced
  { accountId: "acc_1", transactionsFetched: 1247,
    transactionsInserted: 1231, durationMs: 7230 }

14:23:24  INFO  Account synced
  { accountId: "acc_2", transactionsFetched: 892,
    transactionsInserted: 887, durationMs: 8102 }

14:23:41  ERROR  Sync job failed
  { jobId: "initial-sync-usr_789", error: "Job stalled",
    accountsSynced: 2, accountsRemaining: 2 }
```

The logs confirm it: accounts are being processed one by one. Account 1 completes, then account 2, then the clock runs out before account 3 even starts.

You now have everything you need to understand the root cause:

```
ROOT CAUSE ANALYSIS

The sync worker processes accounts sequentially:
  for (const accountId of accountIds) {
    await this.syncAccount(accountId)
  }

Each account takes ~8-12 seconds (GoCardless API + DB insert).

User with 3 accounts: 3 × 10s = 30s → right at the lock TTL
User with 4 accounts: 4 × 10s = 40s → exceeds lock TTL → stalled

The problem compounds during the 08:00 periodic sync window
when all users sync simultaneously:
  - Users with 1-2 accounts: fine
  - Users with 3 accounts: borderline (12% stall rate)
  - Users with 4+ accounts: almost always stall

This is not a GoCardless problem.
This is not a database problem.
This is an architectural problem in how we process accounts.
```

---

#### Step 4: Measuring the "Before" State Precisely

Before writing a single line of fix code, you record the exact before metrics. This is critical for the interview story — you need before and after numbers.

```
BEFORE STATE (measured in Datadog, 7-day average)

Metric: finverse.transaction_sync.duration.ms
Filter: job_type = INITIAL_SYNC
Window: 7-day rolling average

  p50:  15.2 seconds
  p95:  28.4 seconds
  p99:  41.0 seconds

Metric: finverse.transaction_sync.jobs.failed
Filter: error_type = gocardless_transient excluded
        (only counting genuine stall failures)

  Stall rate for users with 3+ accounts: ~12%
  Absolute failed count per day: 34-47 jobs

Lock TTL: 30 seconds (default, not configured)
```

You screenshot the Datadog graphs and save them. You will need them to prove improvement after the fix.

---

#### Step 5: Proposing the Fix to Lucas

You do not immediately start coding. You write a short proposal and bring it to Lucas:

```
Slack message to Lucas:

"Found the root cause for the stalled sync jobs. Sharing
my investigation and proposed fix — want your eyes before
I start coding.

ROOT CAUSE:
  Accounts are synced sequentially. A user with 3+ accounts
  takes 30-40+ seconds total, which exceeds the 30s lock TTL.
  p95 is 28.4s, p99 is 41s. Confirmed via APM trace — clear
  sequential waterfall for every failing job.

PROPOSED FIX (three changes):

  1. Extend lockDuration to 60 seconds, lockRenewTime to 30s.
     This buys immediate relief for borderline cases while
     the structural fix is deployed.

  2. Refactor account sync loop to use Promise.all()
     instead of sequential for/await.
     All accounts fetch and insert concurrently.
     Expected result: 3-account sync drops from ~30s to ~10s.

  3. Add job.updateProgress() between accounts.
     updateProgress() also triggers lock renewal as a
     secondary benefit — provides an additional heartbeat.

CONCERN about Promise.all():
  If we run all accounts concurrently, we make N GoCardless
  calls simultaneously per job. With concurrency:10, that
  could be 10 jobs × 4 accounts = 40 simultaneous GoCardless
  calls. The limiter (max:30 per 10s per container) should
  handle this — but wanted to confirm you agree before coding."
```

Lucas responds: "root cause analysis looks correct. Promise.all approach is right — the limiter will handle the GoCardless throughput. One additional suggestion: add a per-account timeout with Promise.race() so one slow GoCardless call can't hold up the others and push total time back over the lock duration. Go ahead and implement all four changes."

---

#### Step 6: Implementing the Fix

**Change 1: Extend lock configuration**

```typescript
// transaction-sync.worker.ts — updated worker config

@Processor('transaction-sync', {
  concurrency: 10,
  limiter: {
    max: 30,
    duration: 10_000,
  },
  stalledInterval: 30_000,
  maxStalledCount: 2,

  // CHANGED: explicit lock configuration
  lockDuration: 60_000,   // 60 seconds — double the default
  lockRenewTime: 30_000,  // renew at 30s — halfway through lock duration
  // This means: as long as the worker is alive and running,
  // it renews every 30s and the lock never expires.
  // Only if the worker CRASHES does the lock fail to renew.
})
```

**Change 2: Refactor to concurrent processing with progress updates**

Before:

```typescript
// BEFORE — sequential processing
private async handleInitialSync(job: Job): Promise<void> {
  const { userId, accountIds } = job.data

  for (const accountId of accountIds) {
    const transactions =
      await this.goCardlessService.fetchAllTransactions(accountId)

    await this.transactionService.bulkInsert(
      userId,
      accountId,
      transactions
    )
  }
}
```

After:

```typescript
// AFTER — concurrent processing with progress and per-account timeout

private async handleInitialSync(job: Job): Promise<void> {
  const { userId, accountIds } = job.data

  // Track completion count for progress updates
  let completedCount = 0

  // Process all accounts concurrently
  await Promise.all(
    accountIds.map(async (accountId: string) => {

      // Per-account timeout: 25 seconds
      // If one GoCardless call hangs, it cannot hold the entire job.
      // 25s is well within the 60s lock duration.
      // If it times out, only this account fails — others continue.
      const transactions = await Promise.race([
        this.goCardlessService.fetchAllTransactions(accountId),
        new Promise<never>((_, reject) =>
          setTimeout(
            () => reject(
              new Error(
                `GoCardless timeout after 25s for account ${accountId}`
              )
            ),
            25_000
          )
        )
      ])

      await this.transactionService.bulkInsert(
        userId,
        accountId,
        transactions
      )

      // Increment completed count and update progress
      // job.updateProgress() serves two purposes:
      //   1. Visible in Bull Board dashboard (real-time progress)
      //   2. Triggers BullMQ lock renewal — additional heartbeat
      //      beyond the automatic renewal timer
      completedCount++
      await job.updateProgress({
        current:        completedCount,
        total:          accountIds.length,
        currentAccount: accountId,
      })

      this.logger.info('Account synced', {
        jobId:                 job.id,
        userId,
        accountId,
        transactionsFetched:   transactions.length,
      })
    })
  )
}
```

Let's trace through what this looks like in practice for a user with 3 accounts:

```
CONCURRENT EXECUTION — user with 3 accounts

T+00:00  Promise.all starts
         All 3 fetchAllTransactions() calls initiated simultaneously

         [GoCardless HTTP — acc_1]  ← in flight
         [GoCardless HTTP — acc_2]  ← in flight
         [GoCardless HTTP — acc_3]  ← in flight

         Event loop free — waiting for callbacks

T+03:10  acc_2 GoCardless responds first (fastest response)
         [prisma:createMany — acc_2] starts

T+03:45  acc_1 GoCardless responds
         [prisma:createMany — acc_1] starts

T+03:50  acc_2 DB insert completes
         Progress update: { current: 1, total: 3 }
         Lock renewal triggered

T+04:20  acc_3 GoCardless responds
         [prisma:createMany — acc_3] starts

T+05:10  acc_1 DB insert completes
         Progress update: { current: 2, total: 3 }

T+05:40  acc_3 DB insert completes
         Progress update: { current: 3, total: 3 }
         Promise.all resolves
         Job completes

Total duration: 5.7 seconds
(was 24-30 seconds with sequential processing)
```

```
WHAT THE APM TRACE NOW LOOKS LIKE

[bullmq.process.INITIAL_SYNC — 5,724ms — OK]
  │
  ├── [GoCardless GET acc_1 — 3,450ms]   ─┐
  ├── [GoCardless GET acc_2 — 3,100ms]    ├── all overlapping
  ├── [GoCardless GET acc_3 — 4,200ms]   ─┘   in the waterfall
  │
  ├── [prisma:createMany acc_2 — 740ms]  ─┐
  ├── [prisma:createMany acc_1 — 890ms]   ├── overlapping inserts
  └── [prisma:createMany acc_3 — 1,020ms]─┘

Compare to before:
  Sequential waterfall — each span starts only after previous ends
  Total: 32,847ms

After:
  Overlapping spans — all starting near T+00:00
  Total: 5,724ms
```

---

#### Step 7: Testing in Staging Before Deploying

You do not push directly to production. You deploy to staging and test with real-shaped data — users with 1, 2, 3, and 4 connected accounts.

You manually trigger sync jobs for test users in each category and watch the Datadog APM traces:

```
STAGING VALIDATION RESULTS

1-account user:
  Before: 8.1s  After: 8.0s  (no change — only one account, no benefit)

2-account user:
  Before: 16.4s  After: 8.7s  (concurrent — limited by slowest account)

3-account user:
  Before: 24.6s  After: 9.2s  (concurrent — big improvement)

4-account user:
  Before: 33.1s  After: 11.4s (concurrent — previously would stall)
```

You also verify the rate limiter is doing its job. With `concurrency: 10` and now concurrent account processing, the maximum GoCardless calls per 10 seconds is:

```
RATE LIMIT CALCULATION

concurrency: 10 (10 concurrent jobs on this worker container)
accounts per user: average 2.5

Peak GoCardless calls per 10s window:
  10 jobs × 2.5 accounts × (10s / avg_call_duration_5s)
  = 10 × 2.5 × 2
  = 50 calls per 10s

BullMQ limiter: max 30 per 10s per container
  → limiter kicks in before rate limit is hit
  → GoCardless stays happy

With 3 worker containers at peak:
  3 × 30 = 90 calls per 10s across the cluster
  GoCardless limit: typically 50-100 req/s per API key
  → borderline, but the limiter prevents sustained bursts
```

You mention this concern in your PR description. Lucas responds: "correct analysis. We are within limits but closer than I like — I will keep an eye on the GoCardless error rate metric after deployment. If we see 429s increase, we reduce the container count in the Auto Scaling config."

---

#### Step 8: Deploying and Measuring the After State

The fix deploys to production on a Thursday morning. Lucas watches the Datadog dashboard alongside you.

Within the first hour:

```
AFTER STATE (measured in Datadog, 24 hours post-deployment)

Metric: finverse.transaction_sync.duration.ms
Filter: job_type = INITIAL_SYNC
Window: 24-hour post-deployment

  p50:  6.1 seconds   (was 15.2s — 60% improvement)
  p95:  11.2 seconds  (was 28.4s — 61% improvement)
  p99:  18.7 seconds  (was 41.0s — 54% improvement)

Stall rate for users with 3+ accounts:
  Before: ~12%
  After:  < 0.3%
  (confirmed by Datadog monitor — "Failed Jobs Accumulating"
   has not fired once in the 24 hours post-deployment)
```

You save the Datadog screenshots. Before and after, same metric, same filter, same time window structure. This is what you show in the interview.

---

#### Step 9: Replaying the Previously Failed Jobs

There are 34 jobs sitting in the `failed` state from before the fix. Users with 3+ accounts whose initial sync never completed.

You open Bull Board, filter the failed queue by `error_type: job_stalled`, and bulk retry them:

```
Bull Board — Failed Jobs

Filter: failedReason contains "stalled"
Count: 34 jobs

[Retry All] button clicked

→ All 34 jobs moved from failed → wait
→ Workers pick them up with the new concurrent processing
→ All 34 complete successfully within 15 minutes
→ 34 users now have their initial sync completed
```

You log this in Slack to Lucas and Tomasz: "Replayed 34 stalled jobs from before the fix — all completed successfully. Users should see their transactions now."

---

**R — Result:**

The numbers:

- p95 sync duration: **28.4 seconds → 11.2 seconds** (61% improvement)
- p99 sync duration: **41.0 seconds → 18.7 seconds** (54% improvement)
- Stall rate for multi-account users: **~12% → < 0.3%**
- All 34 previously failed jobs replayed and completed

Both measured in Datadog over comparable time windows — 7-day average before, 24-hour window immediately after deployment.

The "Failed Jobs Accumulating" Datadog monitor does not fire again for stall-related reasons. The `#incidents` Slack channel stays quiet. Lucas mentions it at the sprint review: "good investigation — caught the root cause properly and measured both directions."

More importantly for you personally: this is the first time you own an investigation end to end. You find the signal in a metric, trace it through APM spans to a root cause, measure the before state, propose a fix to a senior, implement it, validate in staging, deploy, and measure the after state. The loop is complete. The numbers prove it worked.

---

## What Month 5 Taught You Overall

**From the investigation:** Debugging production problems is a skill with a repeatable process. You do not guess — you read signals in order. Metric anomaly first, then APM trace to find where, then correlated logs to understand why. Following that process takes you from "jobs are failing" to "sequential async processing exceeds lock TTL for multi-account users" in about 45 minutes.

**From the measurement discipline:** The improvement only becomes real when you have numbers on both sides. "I made sync faster" is not a claim. "p95 dropped from 28.4 to 11.2 seconds, measured in Datadog over comparable windows" is a claim. The before screenshot matters as much as the after screenshot.

**From the Lucas interaction:** You brought the finding with a proposed solution, not just the problem. Lucas's addition — the per-account timeout via `Promise.race()` — made the fix more robust. But you had done enough thinking before approaching him that the conversation was about refinement, not direction. That is the difference between bringing a problem to a senior and bringing a proposal to a senior.