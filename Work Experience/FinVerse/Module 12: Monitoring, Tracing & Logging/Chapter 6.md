Writing Chapter 6 now.

---

# Monitoring, Distributed Tracing & Logging — Chapter 6: Alerting & Datadog

---

## Why This Chapter Exists

Metrics, traces, and logs are only valuable if someone acts on them. A dashboard that nobody looks at is worthless. An error rate spike at 3am that nobody knows about until users file support tickets the next morning is a failure of the observability system.

Alerting is the mechanism that turns passive observation into active response. This chapter explains how FinVerse's alerting is designed — what triggers an alert, who gets notified, what they do when they receive it, and how the team avoids the most common alerting failure: being paged so often that engineers start ignoring alerts.

---

## What a Datadog Monitor Is

A Datadog Monitor is a rule that continuously evaluates a metric, log query, or trace query against a threshold. When the threshold is breached, Datadog fires a notification — a Slack message, a PagerDuty page, an email — and keeps notifying until the condition resolves or someone acknowledges it.

```
DATADOG MONITOR — ANATOMY

┌───────────────────────────────────────────────────────────────────┐
│                    MONITOR DEFINITION                             │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Query:                                                           │
│    avg(last_5m):                                                  │
│    sum:finverse.transaction_sync.jobs.failed{env:production}      │
│    / sum:finverse.transaction_sync.jobs.completed{env:production} │
│    * 100                                                          │
│                                                                   │
│  Threshold:                                                       │
│    ALERT when > 2%   (something is wrong)                         │
│    WARN  when > 0.5% (worth watching)                             │
│    OK    when < 0.5% (normal operation)                           │
│                                                                   │
│  Evaluation:                                                      │
│    Check every 1 minute                                           │
│    Evaluate over last 5 minutes                                   │
│                                                                   │
│  Notification:                                                    │
│    ALERT → #incidents Slack + PagerDuty (wakes someone up)        │
│    WARN  → #core-product-team Slack (informational)               │
│    RECOVERY → #incidents Slack ("alert resolved")                 │
│                                                                   │
│  Message (what the alert says):                                   │
│    "Transaction sync error rate is {{value}}%                     │
│     (threshold: 2%)                                               │
│     Runbook: https://notion.so/finverse/runbooks/sync-errors      │
│     Dashboard: https://app.datadoghq.eu/dashboard/..."            │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

**Java parallel:** Datadog Monitors are equivalent to Spring Boot Actuator's health indicators combined with external alerting. In Spring Boot, `HealthIndicator` returns UP or DOWN — but it does not send notifications. Datadog Monitors provide the notification layer that Spring Boot lacks natively. Some Java teams use Micrometer's `Gauge` + external alertmanager for the same purpose.

---

## The Three States Every Monitor Cycles Through

```
MONITOR STATE MACHINE

          threshold breached
OK ──────────────────────────► ALERT
│                                  │
│   condition recovers             │  condition persists
│◄─────────────────────────────────┘
│
│   threshold partially exceeded
├──────────────────────────────► WARN
│                                 │
│   condition improves            │  condition worsens
│◄─────────────────────────────────► ALERT

Notifications fire on:
  OK → ALERT:     immediate notification (page on-call)
  OK → WARN:      notification (Slack, no page)
  ALERT → OK:     recovery notification ("resolved")
  ALERT → WARN:   state change notification
  WARN → ALERT:   escalation notification
```

---

## FinVerse's Complete Alert Ruleset — Core Product Service

These are the monitors that actually run in production for Core Product. Each one has a specific reason it exists and a specific action the on-call engineer takes.

---

### Alert 1 — Sync Error Rate

```
NAME: Transaction Sync — High Error Rate

QUERY:
  avg(last_5m):
    sum:finverse.transaction_sync.jobs.failed{env:production}.as_rate()
    /
    sum:finverse.transaction_sync.jobs.total{env:production}.as_rate()
    * 100

THRESHOLDS:
  ALERT: > 2%    (more than 1 in 50 syncs failing)
  WARN:  > 0.5%  (worth monitoring)

WHY THIS THRESHOLD:
  In normal operation, sync error rate is 0.1-0.2%
  (some failures are expected — transient GoCardless errors)
  0.5% = 2.5× normal = something worth watching
  2% = 10× normal = something is definitely wrong

NOTIFICATION:
  ALERT → #incidents Slack + PagerDuty
  Message includes: current error rate, top error_type tag,
                    link to runbook, link to APM traces

ON-CALL RESPONSE (runbook):
  1. Open Datadog APM → filter failed INITIAL_SYNC/PERIODIC_SYNC spans
  2. Look at error.message attribute → what type of error?
  3. If "429": check GoCardless status page → likely their incident
     → reduce worker concurrency via ECS task definition update
     → increase retry delay in BullMQ config
  4. If "503": GoCardless service unavailable
     → pause the transaction-sync queue from Bull Board
     → monitor GoCardless status page
     → resume when GoCardless recovers
  5. If "PostgreSQL timeout": check RDS metrics → CPU/connections
     → check for long-running queries in RDS Performance Insights
```

---

### Alert 2 — Queue Backlog

```
NAME: BullMQ — Transaction Sync Queue Depth High

QUERY:
  avg(last_10m):
    avg:finverse.bullmq.queue.waiting{
      env:production,
      queue:transaction-sync
    }

THRESHOLDS:
  ALERT: > 500 jobs waiting
  WARN:  > 100 jobs waiting

WHY THIS THRESHOLD:
  Normal: 0-50 jobs waiting (workers keep up)
  During 08:00 periodic sync: up to 2000 jobs (expected, auto-scales)
  The alert has a 10-minute average so the 08:00 spike
  does not trigger it — scaling resolves it within minutes

  If queue stays above 500 for 10 minutes:
  workers cannot keep up → users' syncs are delayed

ON-CALL RESPONSE:
  1. Check ECS console — is auto-scaling working?
     → Are new worker containers launching?
     → If yes: wait, it will drain
  2. If auto-scaling is not firing:
     → Check CloudWatch metric for queue depth → is it publishing?
     → Manually scale ECS service: set desiredCount = 5
  3. If workers are running but queue is not draining:
     → Check worker logs for errors
     → Is GoCardless returning 429s? → rate limit is the bottleneck
     → Reduce concurrency to stay within GoCardless limits
```

---

### Alert 3 — No Active Workers During Peak Window

```
NAME: BullMQ — No Active Workers (Peak Hours)

QUERY:
  avg(last_5m):
    avg:finverse.bullmq.queue.active{
      env:production,
      queue:transaction-sync
    }

THRESHOLDS:
  ALERT: = 0
  Time restriction: only between 06:00-22:00 UTC
  (outside peak hours, 0 active is expected)

WHY THIS EXISTS:
  If there are jobs waiting but zero active,
  workers have crashed or cannot connect to Redis.
  This is a service-down scenario.

ON-CALL RESPONSE:
  1. Check ECS console → are worker tasks running?
     → If no tasks: restart the ECS service
  2. If tasks are running but not processing:
     → Check Redis connectivity (ElastiCache health in AWS console)
     → Check worker logs for connection errors
     → If Redis is down: ECS workers will reconnect automatically
        once Redis recovers — monitor ElastiCache failover
```

---

### Alert 4 — GoCardless Error Rate Spike

```
NAME: GoCardless API — High Error Rate

QUERY:
  avg(last_5m):
    sum:finverse.gocardless.api.errors{
      env:production,
      status_code:429
    }.as_rate()
    /
    sum:finverse.gocardless.api.calls{env:production}.as_rate()
    * 100

THRESHOLDS:
  ALERT: > 5%   (GoCardless is rate limiting us heavily)
  WARN:  > 1%   (some rate limiting, worth watching)

WHY SEPARATE FROM SYNC ERROR RATE:
  Sync failures can happen for many reasons.
  This monitor isolates GoCardless specifically.
  429s from GoCardless mean we are approaching their rate limit —
  the fix (reduce concurrency/rate limit) is different from
  other failure types.
```

---

### Alert 5 — Failed Jobs Accumulating

```
NAME: BullMQ — Failed Jobs Count Growing

QUERY:
  change(avg(last_1h),last_1h):
    avg:finverse.bullmq.queue.failed{
      env:production,
      queue:transaction-sync
    }

THRESHOLDS:
  ALERT: increase of > 20 in the last hour
  (20 new failed jobs in an hour = something systematic)

WHY NOT JUST "failed > 0":
  Some failed jobs are expected — edge cases, transient errors.
  The alert should fire when failures are accumulating,
  not when an isolated failure happens.
  "Change over time" catches trends without noise.

ON-CALL RESPONSE:
  1. Open Bull Board → failed jobs tab
  2. Look at failedReason for the most recent failures
  3. If same error across many jobs → systematic issue
  4. If varied errors → unrelated individual failures, lower urgency
  5. After root cause fixed: bulk retry from Bull Board
```

---

### Alert 6 — HTTP Error Rate

```
NAME: Core Product API — High HTTP Error Rate

QUERY:
  avg(last_5m):
    sum:finverse.http.requests.total{
      env:production,
      status_code:5*
    }.as_rate()
    /
    sum:finverse.http.requests.total{env:production}.as_rate()
    * 100

THRESHOLDS:
  ALERT: > 1%   (more than 1 in 100 API calls is a 5xx)
  WARN:  > 0.1%

NOTE ON 4xx ERRORS:
  4xx errors (400, 401, 403, 404) are NOT included.
  4xx errors are client errors — the user sent a bad request.
  They are expected and not indicative of a server problem.
  Only 5xx (server errors) indicate something we broke.
```

---

### Alert 7 — PostgreSQL Connection Pool Pressure

```
NAME: PostgreSQL — Connection Pool Near Exhaustion

QUERY:
  avg(last_5m):
    avg:db.client.connections.usage{
      env:production,
      service:core-product
    }

THRESHOLDS:
  ALERT: > 18   (out of 20 max connections — 90% full)
  WARN:  > 14   (out of 20 — 70% full)

WHY THIS MATTERS:
  At 20/20 connections: new requests queue waiting for a connection
  At 20+: Prisma throws "connection pool timeout" errors
  These appear as 500 errors to users

ON-CALL RESPONSE:
  1. Identify which operation is holding connections open
     → Long-running transactions (check RDS Performance Insights)
     → Missing await on Prisma calls (code bug)
     → Too many concurrent BullMQ jobs × too high concurrency
  2. Immediate mitigation:
     → Temporarily reduce BullMQ concurrency (fewer concurrent DB ops)
  3. Root cause fix:
     → Add connection pool sizing to Prisma config
     → Review long-running transaction patterns
```

---

### Alert 8 — Redis Memory Pressure

```
NAME: Redis (ElastiCache) — Memory Usage High

QUERY:
  avg(last_10m):
    avg:aws.elasticache.database_memory_usage_percentage{
      env:production,
      cache_cluster_id:finverse-redis-prod
    }

THRESHOLDS:
  ALERT: > 80%   (approaching eviction territory)
  WARN:  > 65%

ON-CALL RESPONSE:
  1. Check which key namespaces are growing
     → redis-cli: MEMORY USAGE <key> for suspicious keys
     → Datadog Redis integration: key pattern breakdown
  2. Common cause: BullMQ removeOnComplete not configured properly
     → Check that completed jobs are being cleaned up
  3. Immediate mitigation:
     → redis-cli SCAN + DEL for old/orphaned keys
  4. Long-term:
     → Increase ElastiCache instance size
     → Tighten TTLs on cached objects
```

---

## SLOs: Defining What "Good" Means

An alert tells you when something is wrong *right now*. An **SLO (Service Level Objective)** defines what percentage of the time the service must be "good" over a longer window — a week, a month, a quarter.

SLOs answer the question: "are we meeting our users' expectations on a sustained basis?"

```
SLO STRUCTURE

┌─────────────────────────────────────────────────────────────────┐
│                    SLO DEFINITION                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  NAME: Transaction Sync Reliability                             │
│                                                                 │
│  SLI (Service Level Indicator):                                 │
│  The metric being measured:                                     │
│    % of INITIAL_SYNC jobs that complete successfully            │
│    within 60 seconds of being enqueued                          │
│                                                                 │
│  SLO Target:                                                    │
│    99.5% of sync jobs must meet the SLI                         │
│    Measured over: rolling 30-day window                         │
│                                                                 │
│  Error Budget:                                                  │
│    0.5% of syncs can fail or be slow                            │
│    In 30 days: ~43,200 user syncs                               │
│    Error budget: 0.5% × 43,200 = 216 failures allowed           │
│                                                                 │
│  Current burn rate: 0.08% (well within budget)                  │
│  Error budget remaining: 93%                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Why error budgets matter for interviews:**

The error budget is what you reference when an interviewer asks about reliability improvements. "We reduced sync error rate from 1.2% to 0.08%, which improved our error budget consumption from burning 240% of budget per month to 16% — well within our 0.5% SLO target."

This is far more sophisticated than "we reduced errors." It shows you understand reliability engineering, not just that something was fixed.

---

## FinVerse's SLOs for Core Product

```
┌──────────────────────────────────────────────────────────────────┐
│               CORE PRODUCT — SLO DEFINITIONS                     │
├──────────────────────────────┬───────────────────────────────────┤
│  SLO                         │  Target                           │
├──────────────────────────────┼───────────────────────────────────┤
│  API Availability            │  99.9% of HTTP requests return    │
│                              │  non-5xx response                 │
│                              │  (allows 43.8 min downtime/month) │
├──────────────────────────────┼───────────────────────────────────┤
│  API Latency                 │  95% of requests complete         │
│                              │  in under 500ms                   │
│                              │  (p95 < 500ms, rolling 7 days)    │
├──────────────────────────────┼───────────────────────────────────┤
│  Sync Reliability            │  99.5% of sync jobs complete      │
│                              │  successfully within 60 seconds   │
│                              │  (rolling 30 days)                │
├──────────────────────────────┼───────────────────────────────────┤
│  Sync Freshness              │  95% of active users have had     │
│                              │  at least one successful sync     │
│                              │  in the last 4 hours              │
│                              │  (the periodic sync promise)      │
└──────────────────────────────┴───────────────────────────────────┘
```

---

## Alert Fatigue: Why You Don't Alert on Everything

Alert fatigue is when engineers receive so many alerts — most of which are false alarms, or too minor to matter — that they start ignoring or silencing them. This is one of the most dangerous situations in production operations, because when a real critical alert fires, it gets ignored too.

```
ALERT FATIGUE — THE FAILURE MODE

Team A: alerts on everything
  Every 404 → alert
  Every GoCardless retry → alert
  Every Redis cache miss → alert
  Every deployment → alert
  
  Result:
    Engineers receive 50+ alerts per day
    Most are noise — 404s are expected, retries resolve themselves
    Engineers start muting Slack notifications
    Real alert fires at 3am: nobody responds for 2 hours
    Major incident: 2 hours of users with broken sync

Team B (FinVerse): alerts only on actionable conditions
  Only alert when: engineer must do something
  Only page: when something is broken right now
  Only warn: when trend needs monitoring but not immediate action

  Result:
    3-5 real alerts per week on average
    Engineers trust alerts — every page means something real
    Real alert fires at 3am: engineer responds within 5 minutes
    Incident resolved in 20 minutes
```

**The three questions FinVerse asks before adding any new alert:**

```
BEFORE ADDING A NEW ALERT — THREE QUESTIONS

1. "Is this actionable?"
   Is there something an engineer can DO when this fires?
   If the only response is "wait and see" → not an alert.
   Make it a dashboard graph instead.

2. "Is this urgent?"
   Does this require waking someone up at 3am?
   Or can it wait until morning?
   If it can wait → WARN to Slack, not ALERT to PagerDuty.

3. "What is the false positive rate?"
   Will this alert fire when nothing is actually wrong?
   If > 10% false positive rate → threshold needs tuning.
   A noisy alert is worse than no alert.
```

---

## The On-Call Runbook Pattern

Every alert at FinVerse links to a runbook — a document in Notion that describes exactly what to do when the alert fires. The runbook is not optional documentation. It is required for every alert.

```
RUNBOOK STRUCTURE — TRANSACTION SYNC ERROR RATE

URL: https://notion.so/finverse/runbooks/sync-error-rate

ALERT: "Transaction Sync — High Error Rate"

SEVERITY: P2 (affects user experience, not complete outage)

FIRST RESPONSE (within 15 minutes):
  1. Acknowledge the alert in PagerDuty
  2. Check GoCardless status: https://status.gocardless.com
  3. Open Datadog APM:
     service:core-product operation:bullmq.process.*
     status:error timerange:last_30m
  4. Look at top error_type tag in failed spans

IF error_type = "gocardless_rate_limit" (429):
  → GoCardless is rate limiting us
  → Check if this started after a traffic spike (dashboard)
  → Reduce rate limiter in BullMQ: max:20 duration:10000
  → Deploy config change via ECS task definition update
  → Monitor: error rate should drop within 10 minutes

IF error_type = "gocardless_unavailable" (503):
  → GoCardless is having an outage
  → DO NOT retry aggressively — it worsens their recovery
  → Pause queue from Bull Board: /admin/queues → Pause
  → Post in #incidents: "Paused sync queue pending GoCardless recovery"
  → Monitor GoCardless status page
  → Resume queue when GoCardless is recovered

IF error_type = "postgres_timeout":
  → Database is under load
  → Check RDS Performance Insights in AWS console
  → Look for long-running queries (> 5 seconds)
  → If connection pool full: reduce BullMQ concurrency immediately

ESCALATION:
  If unresolved after 30 minutes → escalate to Lucas
  If P1 (all syncs failing) → escalate immediately

RESOLUTION:
  1. Confirm error rate returns below 0.5% in Datadog
  2. Replay failed jobs from Bull Board if needed
  3. Post resolution summary in #incidents
  4. Create Linear ticket for root cause analysis if needed
```

**Why runbooks matter for interviews:**

When an interviewer asks "tell me about an incident you handled," the presence of a runbook means you can give a structured answer: "When this alert fired, the runbook guided me to check X first, which revealed Y, and the fix was Z." This demonstrates that your team operates with engineering discipline, not chaos.

---

## Composite Monitors: Alerting on Multiple Conditions

Some conditions are only meaningful when multiple things are true simultaneously. Datadog's composite monitors combine multiple monitors with AND/OR logic.

```
EXAMPLE: GoCardless Impact Monitor

This monitor fires only when:
  1. GoCardless error rate > 5%
     AND
  2. Sync error rate > 2%

Why both conditions?
  GoCardless error rate alone: might be our test environment
  Sync error rate alone: might be a PostgreSQL issue
  Both together: definitely a GoCardless production impact

Without composite monitor:
  Two separate alerts fire
  Engineer might not connect them as the same root cause

With composite monitor:
  One alert fires with clear message:
  "GoCardless impact detected: API errors causing sync failures"
  Engineer immediately knows the root cause before investigating
```

---

## Anomaly Detection: Alerting Without Fixed Thresholds

Fixed thresholds work well for metrics that are stable over time. But some metrics have natural patterns — traffic is higher on Monday mornings than Sunday nights, sync volume spikes at the start of each month when automated investments trigger.

A fixed threshold for queue depth would fire every Monday morning even when everything is working normally.

Datadog's **anomaly detection** uses machine learning to learn the normal pattern for a metric and alerts only when the metric deviates from its expected range — accounting for time-of-day and day-of-week patterns.

```
ANOMALY DETECTION VS FIXED THRESHOLD

Queue depth — Monday 08:00 (peak time):
  Expected range: 500-2000 jobs (busy morning)
  Actual: 1800 jobs
  Fixed threshold (> 500): FIRES ← false alarm
  Anomaly detection: within expected range → NO ALERT

Queue depth — Tuesday 14:00 (quiet afternoon):
  Expected range: 0-50 jobs
  Actual: 800 jobs
  Fixed threshold (> 500): FIRES ← true alarm
  Anomaly detection: 16× above expected → FIRES ← true alarm

Anomaly detection correctly distinguishes:
  Expected busy period → no alert
  Unexpected spike at quiet time → alert
```

At FinVerse, anomaly detection is used for:

```
METRICS USING ANOMALY DETECTION AT FINVERSE

1. BullMQ queue depth
   Normal patterns: high at 08:00 (periodic sync), low midday

2. HTTP request rate
   Normal patterns: higher during EU market hours (09:00-17:00 CET)

3. GoCardless API call volume
   Normal patterns: spikes when users trigger manual syncs

METRICS USING FIXED THRESHOLDS:
  Error rates (should always be near 0 — no "normal high" pattern)
  Connection pool usage (absolute limit, no time pattern)
  Redis memory (absolute limit, no time pattern)
```

---

## The Full Alerting Stack at FinVerse

```
ALERT FLOW — END TO END

Datadog Monitor evaluates condition
         │
    condition breached
         │
         ▼
Datadog Notification sends to:
         │
    ┌────┴──────────────────────────────────┐
    │                                       │
    ▼                                       ▼
#incidents Slack channel            PagerDuty
(immediate visibility)              (pages on-call engineer)
         │                                  │
         │                          On-call engineer
         │                          acknowledges page
         │                                  │
         │                          Opens runbook link
         │                          from alert message
         │                                  │
         │                          Follows runbook steps
         │                                  │
         │                          Resolves incident
         │                                  │
         └──────────────────────────────────┘
                                   │
                         Posts resolution to #incidents
                         Creates Linear ticket if needed
                         (root cause analysis, future fix)
```

**Who is on-call at FinVerse:**

Given the team size (6 engineers in Core Product), the on-call rotation is weekly. Each engineer is primary on-call for one week every six weeks. Lucas is the escalation path for all P1 incidents regardless of who is on-call.

---

## Chapter 6 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 6 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Datadog Monitor: continuously evaluates a metric query         │
│  against thresholds → fires notification when breached          │
│  Three states: OK → WARN → ALERT → OK (recovery)                │
│                                                                 │
│  8 production alerts for Core Product:                          │
│  1. Sync error rate (> 2%)                                      │
│  2. Queue depth (> 500 for 10 min)                              │
│  3. No active workers during peak hours                         │
│  4. GoCardless error rate (> 5%)                                │
│  5. Failed jobs accumulating (> 20 new per hour)                │
│  6. HTTP error rate (> 1% 5xx)                                  │
│  7. PostgreSQL connection pool (> 18/20)                        │
│  8. Redis memory (> 80%)                                        │
│                                                                 │
│  SLOs: define what "good" means over time                       │
│  SLI: the metric being measured                                 │
│  Error budget: how much failure is allowed                      │
│  Interview language: "we improved error budget consumption      │
│  from 240% to 16% of monthly budget"                            │
│                                                                 │
│  Alert fatigue: the failure mode where engineers ignore alerts  │
│  Solution: only alert on actionable, urgent conditions          │
│  Three questions before adding alert:                           │
│  Is it actionable? Is it urgent? False positive rate?           │
│                                                                 │
│  Every alert links to a runbook                                 │
│  Runbook: exact steps for each error type                       │
│  No guessing, no "check everything" — structured response       │
│                                                                 │
│  Anomaly detection for time-varying metrics                     │
│  Fixed thresholds for absolute limits                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

