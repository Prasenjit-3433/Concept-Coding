# Story 16: Production Incident Ownership — First Time You Led the Investigation

---

## Context — Where You Were at Month 13

```
By month 12 end — what had accumulated:
─────────────────────────────────────────
You had been in the war room for 
Story 9's incident.
You had watched Arjun debug live.
You had written the postmortem.
You had added the Flyway migration.

That was month 8.

Five months had passed since then.

In those five months:
  You had designed and shipped 
  the approval policy caching.
  You had found and fixed 
  a cache stampede before anyone 
  else noticed it.
  You had taught Léa @PreAuthorize.
  You had written your first ADR.
  You had become the person 
  Léa came to with questions.
  You had become the person 
  who left artifacts behind.

Lukas had said: 
"You're operating at L2."

But saying it and feeling it 
are different things.

Month 13 was where you felt it.
```

Something else had changed too. You were now the primary owner of the Invoice & AP Service's outbox implementation. Not because anyone had formally assigned it — but because you had touched it in Story 5, improved it in Story 8's Kafka work, and written the health indicator for it in the monitoring module. You knew it better than anyone else on the team who wasn't Arjun.

Ownership doesn't always come from a title. Sometimes it comes from proximity.

---

## The Situation

It was a Thursday morning, month 13, week 2. 9:47am CET (2:17pm IST). You were in the middle of writing a migration for a new feature when the alert fired:

```
#expense-ap-alerts (Slack):
─────────────────────────────
[Datadog Alert] CRITICAL
service: invoice-service
metric: outbox.health.status
  component: OutboxHealthIndicator

Status: DOWN
  unpublishedEvents: 1,247
  threshold:         500
  message: "Outbox backlog too high 
            — poller may be stuck"

Duration: 8 minutes
```

You had written that health indicator in month 10. You had set the threshold at 500 events. You had never seen it fire in production.

It was firing now.

```
1,247 unpublished events.
8 minutes of accumulation.
The outbox poller was supposed 
to run every 100ms.
In 8 minutes (480 seconds), 
it should have run ~4,800 times.
4,800 runs × 100 events per batch 
= 480,000 events processed capacity.

But 1,247 events were sitting 
unpublished.

The poller wasn't running.
Or it was running and failing 
on every single event.
```

You looked at `#incidents`. Nobody had posted yet. You were the first person to see the alert.

Your next action mattered.

In Story 9, Arjun had started the incident call within 90 seconds. You had joined as an observer. Now you were the person who needed to start it — or at least start the investigation.

You typed in `#incidents`:

```
You (#incidents):
──────────────────
"[11:47am CET] OutboxHealthIndicator 
 DOWN in invoice-service.
 1,247 unpublished events, 
 growing.
 
 Starting investigation.
 
 @Arjun @Elena — FYI, keeping 
 you posted."
```

You started a Datadog investigation. You did not immediately ask for help.

```
This was the shift.

In month 8, your first instinct was 
"I need to tell Arjun about this."
In month 13, your first instinct was 
"Let me look at this myself first."

Not because Arjun was unavailable.
Not because you didn't want help.
But because you had enough context 
to know where to look first — 
and you knew that arriving at 
a conversation with a hypothesis 
is better than arriving with 
just the symptom.
```

---

## Your Task

```
What you owned:
────────────────
Lead the investigation.
Identify the root cause.
Communicate status to the team.
Fix the problem or escalate 
with enough context for someone 
else to fix it fast.
Write the postmortem.

What you had available:
────────────────────────
Datadog access (metrics, logs, APM)
The invoice-service codebase 
  (you knew it well)
The OutboxPoller source code 
  (you had read it in month 7)
The OutboxHealthIndicator 
  (you had written it)
Slack (team available if needed)
```

---

## The Investigation — Step By Step

### Step 1 — Establish the Timeline

You opened Datadog and searched for when the outbox backlog started growing. You pulled the metric:

```
outbox_events WHERE published = false
COUNT over time:

12:30am: 0 events
01:00am: 0 events
...
08:00am: 0 events  ← normal overnight
09:00am: 0 events
09:15am: 0 events
09:23am: 4 events  ← first nonzero
09:35am: 89 events ← growing faster
09:47am: 1,247 events ← alert fires
```

```
Your note:
───────────
Backlog started at 9:23am.
Alert fired at 9:47am 
(8 minutes after threshold crossed).
Total accumulation period: ~24 minutes.

Invoice & AP Service normally 
processes high volume on Thursday 
mornings (payment run day — 
finance teams approve invoices 
for the week's payment batch).

Was the outbox poller failing silently?
Or was it not running at all?
```

### Step 2 — Check the Outbox Poller Logs

You searched Datadog Logs:

```
Query: service:invoice-service 
       AND logger:OutboxPoller
       AND @timestamp:[09:20 TO 09:50]

Results:
─────────
09:22:41 INFO  OutboxPoller: 
  Polling outbox — found 2 events
09:22:41 INFO  OutboxPoller: 
  Published event inv-approved-uuid-1
09:22:41 INFO  OutboxPoller: 
  Published event inv-approved-uuid-2
09:22:42 INFO  OutboxPoller: 
  Polling outbox — found 0 events

-- 100ms gap --

09:22:42 INFO  OutboxPoller: 
  Polling outbox — found 1 event
09:22:42 INFO  OutboxPoller: 
  Published event inv-approved-uuid-3

-- gap grows here --

09:23:01 ERROR OutboxPoller: 
  Failed to publish outbox event 
  inv-approved-uuid-4
  correlationId: abc123
  
  com.zaxxer.hikari.pool.HikariPool$
  PoolInitializationException: 
  Failed to initialize pool: 
  FATAL: remaining connection slots 
  are reserved for non-replication 
  superuser connections
```

```
You saw it immediately.

"remaining connection slots are 
reserved for non-replication 
superuser connections"

That's PostgreSQL's error message 
for: the connection pool is full.
No more connections available.

The outbox poller was trying to 
open a DB connection and failing 
because the pool was exhausted.
```

You looked for what happened at 9:23am that could cause DB connection exhaustion. You searched the logs more broadly:

```
Query: service:invoice-service 
       AND level:ERROR
       AND @timestamp:[09:20 TO 09:30]

Results (first 10 errors):
──────────────────────────
09:22:58 ERROR ExpenseController:
  DataAccessException — 
  Unable to acquire JDBC connection
  uri: GET /api/v1/invoices
  
09:23:01 ERROR OutboxPoller:
  Failed to publish outbox event
  PoolInitializationException
  
09:23:01 ERROR InvoiceService:
  Unable to acquire JDBC connection
  method: processInvoiceApproval
  
09:23:04 ERROR InvoiceService:
  Unable to acquire JDBC connection
  method: getInvoicesByPaymentRun
  
...47 more similar errors...
```

```
Your note:
───────────
Not just the outbox poller.
Everything that touches the DB 
is failing from 9:23am onward.

DB connection pool exhausted.

But WHY was it exhausted?
What opened all the connections?
```

### Step 3 — Find What Exhausted the Connection Pool

You pulled the HikariCP metrics from Datadog:

```
Metric: jdbc.connections.active
  filter: service=invoice-service

09:00am: 3 active (normal, pool size=10)
09:10am: 4 active
09:15am: 4 active
09:20am: 4 active
09:21am: 6 active ← slight increase
09:22am: 9 active ← approaching limit
09:23am: 10 active ← FULL
09:23am: pool exhausted, errors start
```

```
Pool went from 4 to 10 in 2 minutes.
6 new connections opened between 
9:21am and 9:23am.

Something opened 6 long-running 
DB connections almost simultaneously.
Long-running means: they opened 
and never closed (or took a very 
long time to close).
```

You searched the APM traces for what happened at 9:21am:

```
Datadog APM — Traces 
[09:20 - 09:23am]:

Notable: 4 traces with duration > 30 seconds
         (normally all traces < 200ms)

Trace 1: 
  POST /api/v1/payment-runs
  Duration: still running (47 seconds and counting)
  Spans:
    InvoiceService.createPaymentRun (still running)
    └── DB: findAndLockForPaymentRun (still running)
        SQL: SELECT i.* FROM invoices i WHERE ...
             FOR UPDATE

Trace 2:
  POST /api/v1/payment-runs
  Duration: still running (46 seconds)
  Spans:
    InvoiceService.createPaymentRun (still running)
    └── DB: findAndLockForPaymentRun (still running)

Trace 3:
  POST /api/v1/payment-runs
  Duration: still running (45 seconds)
  ...same...

Trace 4:
  POST /api/v1/payment-runs
  Duration: still running (45 seconds)
  ...same...
```

```
Four concurrent requests to 
POST /api/v1/payment-runs.
All started within seconds of each other.
All running for 45+ seconds.
All stuck in findAndLockForPaymentRun.

That's the pessimistic lock 
from the payment run creation logic.
```

You looked at the `InvoiceService.createPaymentRun()` method — you knew this code:

```java
// This is what you were looking at:
@Transactional
public PaymentRun createPaymentRun(
        List<UUID> invoiceIds,
        LocalDate scheduledDate,
        UUID createdById) {

    // Pessimistic lock — locks selected invoices
    List<Invoice> invoices = invoiceRepository
        .findAndLockForPaymentRun(invoiceIds);
        // SELECT ... FOR UPDATE
        // This holds the DB connection 
        // for the ENTIRE duration of 
        // the @Transactional method

    // ... payment run creation logic ...
    paymentRunRepository.save(run);
    invoices.forEach(inv -> {
        inv.setPaymentRunId(run.getId());
        inv.setStatus(InvoiceStatus.PAYMENT_PENDING);
    });
    invoiceRepository.saveAll(invoices);

    return run;
}
```

```
The problem was clear now.

Four finance managers had simultaneously 
clicked "Create Payment Run" on 
their dashboards — probably after 
a Thursday morning finance meeting 
where they all agreed to run payments.

Each request:
1. Started a @Transactional context
2. Called findAndLockForPaymentRun 
   (SELECT FOR UPDATE)
3. Got a DB connection from the pool
4. HELD that connection for the 
   entire transaction duration

But the FOR UPDATE locks conflicted.
Request 1 locked invoices A, B, C.
Request 2 tried to lock overlapping 
invoices and BLOCKED — waiting 
for Request 1 to release.
Request 3 also blocked.
Request 4 also blocked.

4 requests × 1 connection each 
= 4 connections held for 45+ seconds
PLUS the normal pool usage of 4-6 
connections for other operations
= pool exhausted.

Every other operation needing the DB 
— including the outbox poller — 
got nothing.
```

You had the root cause. Now you needed to understand how to fix it.

---

### Step 4 — Immediate Mitigation

You typed in `#incidents`:

```
You (#incidents):
──────────────────
"[10:01am CET] Root cause found.
 
 4 concurrent payment run creation 
 requests holding DB connections 
 via pessimistic locks 
 (SELECT FOR UPDATE).
 Each waiting for the others.
 DB connection pool exhausted.
 Outbox poller can't get connections.
 1,247 events backlogged.
 
 The 4 payment run requests are 
 stuck in a lock contention deadlock.
 
 Immediate mitigation options:
 
 Option 1: Kill the 4 stuck requests.
   They'll rollback. Finance managers 
   see error. They retry one at a time.
   Fast — 2 minutes.
   Downside: bad UX, no clear 
   guidance to users.
 
 Option 2: Wait for PostgreSQL's 
   lock_timeout to kick in.
   Do we have one configured?
   
 Option 3: Increase pool size 
   temporarily.
   Risk: could mask the real problem.
   Not recommended.
 
 Lean toward Option 1 unless 
 @Arjun has a better idea.
 
 @Arjun — thoughts?"
```

Arjun replied within 3 minutes:

```
Arjun:
───────
"Good analysis. Option 1 is right.
 
 Check if we have lock_timeout 
 configured on the DB session first —
 if we do, they'll timeout on their 
 own within that window.
 
 If not: kill the idle-in-transaction 
 sessions via pg_terminate_backend.
 
 For the UX problem — can we add 
 a 409 response to the UI 
 so users get a clear message 
 instead of a 500?
 
 That's a code fix for later, 
 not for right now."
```

You checked the PostgreSQL session config:

```sql
-- Check lock_timeout for invoice-service sessions
SELECT application_name, 
       state, 
       wait_event_type,
       wait_event,
       query_start,
       state_change,
       EXTRACT(EPOCH FROM (NOW() - state_change)) 
         AS seconds_in_state
FROM pg_stat_activity
WHERE application_name = 'invoice-service'
ORDER BY state_change;

-- Result:
-- 4 sessions with state = 'idle in transaction'
-- wait_event = 'Lock'
-- seconds_in_state = 52, 51, 50, 50
-- (been waiting 50+ seconds for locks)

SHOW lock_timeout;
-- Result: 0
-- (no timeout configured — they'll wait forever)
```

```
Your note:
───────────
No lock_timeout configured.
These sessions will wait forever 
unless killed manually.

Need to:
1. Kill the 4 stuck sessions
2. Add lock_timeout configuration 
   to prevent this in the future
```

You posted in `#incidents`:

```
You:
─────
"No lock_timeout configured.
 Sessions will wait indefinitely.
 
 Killing the 4 stuck sessions now.
 
 Query to execute (will post 
 output after):"
```

You ran the kill command:

```sql
-- Find and kill the stuck payment-run sessions
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE application_name = 'invoice-service'
  AND state = 'idle in transaction'
  AND wait_event = 'Lock'
  AND EXTRACT(EPOCH FROM (NOW() - state_change)) > 30;

-- Result: 4 rows returned
-- pg_terminate_backend
-- ─────────────────────
-- t
-- t
-- t
-- t
-- (all 4 terminated)
```

You posted in `#incidents`:

```
You:
─────
"[10:08am CET] 4 stuck sessions 
 terminated. Watching recovery."
```

You watched Datadog:

```
Recovery metrics (10:08am - 10:15am):

jdbc.connections.active:
  10:08am: 10 (still full — sessions 
            being cleaned up)
  10:09am: 5 (released)
  10:10am: 3 (normal)

outbox.health.status:
  10:08am: DOWN (1,247 events)
  10:09am: DOWN (1,183 events — 
           poller restarted, burning down)
  10:11am: WARNING (612 events — 
           below 1000 threshold)
  10:15am: UP (44 events — 
           nearly clear)
  10:18am: UP (0 events — resolved)

invoice.approved Kafka events published:
  10:08am - 10:18am: 1,247 events 
  published in order
  (confirmed by checking topic 
   consumer offsets)
```

You posted the all-clear:

```
You (#incidents):
──────────────────
"[10:18am CET] Resolved.

 Outbox backlog cleared to 0.
 All 1,247 invoice.approved events 
 published to Kafka.
 Connection pool back to normal (3 active).
 
 Total incident duration: 55 minutes.
 
 Root cause: 4 concurrent payment run 
 creation requests in lock contention.
 No lock_timeout configured — 
 sessions held connections indefinitely.
 
 Immediate fix: terminated stuck sessions.
 
 Needed: 
 1. lock_timeout on DB sessions 
    (prevents recurrence)
 2. 409 response for concurrent 
    payment run conflict 
    (UX improvement — users get 
     clear error instead of timeout)
 3. Postmortem this afternoon.
 
 @Lukas — customer impact:
   No invoice payments were delayed 
   (payment runs were not completed —
    they rolled back when sessions 
    were killed).
   Finance managers will need to 
   retry creating their payment runs.
   Notification Service consumers 
   will process the backlog events —
   finance teams will receive their 
   expected notifications now."
```

Lukas:

```
Lukas:
───────
"Good work. 
 Write the postmortem today.
 @Arjun — can you review it 
 before we publish?"
```

---

## The Root Cause — Deeper Analysis

After the incident was resolved, you spent an hour doing deeper analysis before writing the postmortem. You wanted to understand not just what happened but why the payment run endpoint allowed concurrent conflicting requests at all.

You looked at the frontend behavior:

```
You (Slack to Sophie):
───────────────────────
"Quick question on the payment 
 run creation flow —
 is there any UI guard that 
 prevents a finance manager 
 from clicking 'Create Payment Run' 
 multiple times?
 
 Today we had 4 concurrent requests 
 from 4 different finance managers 
 creating payment runs with 
 overlapping invoice selections."
```

```
Sophie:
────────
"No UI guard.
 
 The UI disables the button 
 after one click to prevent 
 double-submission from a 
 SINGLE user.
 
 But nothing prevents 4 different 
 finance managers from each 
 creating a payment run with 
 the same invoices.
 
 That's actually a valid use case 
 in theory — each finance manager 
 might be creating runs for 
 different invoice sets.
 
 But the fact that they overlap 
 means the locks contend."
```

```
Your analysis:
───────────────
Two distinct problems:

Problem 1 (what caused today's incident):
  No lock_timeout on DB sessions.
  When locks contend, sessions 
  wait indefinitely.
  This exhausted the connection pool.
  
  Fix: Configure lock_timeout.
  If a session waits more than 
  10 seconds for a lock, throw 
  LockTimeoutException.
  Application handles it gracefully 
  (409 to the user: "payment run 
  in progress for these invoices, 
  please try again").

Problem 2 (structural, harder):
  No prevention of overlapping 
  payment run selections.
  Finance Manager A selects 
  invoices 1, 2, 3.
  Finance Manager B selects 
  invoices 2, 3, 4.
  Invoice 2 and 3 will be in 
  two payment runs simultaneously —
  double payment risk.
  
  Current handling: pessimistic 
  lock catches this — one request 
  succeeds, other gets lock timeout.
  With lock_timeout, other gets 
  409 immediately.
  
  Long-term fix: a "lock invoices 
  for selection" step in the UI 
  before creating the payment run.
  But that's a product decision, 
  not an incident fix.
```

---

## The Postmortem

You wrote it yourself. You sent it to Arjun for review at 3pm.

```
INCIDENT POSTMORTEM — INV-2025-04-17
Author: [Your name]
Reviewed by: Arjun Sharma
Approved by: Lukas Becker

TIMELINE:
──────────
09:21am — 4 concurrent payment run 
           creation requests arrive
09:21am — 4 DB connections acquired 
           via findAndLockForPaymentRun 
           (SELECT FOR UPDATE)
09:22am — Lock contention begins between 
           overlapping invoice selections
09:23am — DB connection pool reaches 
           maximum (10 connections)
09:23am — OutboxPoller fails to acquire 
           connection — begins accumulating 
           unpublished events
09:23am — HTTP endpoints begin returning 
           connection errors (8.3% error rate)
09:47am — OutboxHealthIndicator fires CRITICAL 
           (1,247 events, threshold 500)
09:47am — [Your name] begins investigation
10:01am — Root cause identified and posted 
           to #incidents
10:08am — 4 stuck sessions terminated via 
           pg_terminate_backend
10:10am — Connection pool normalizes
10:18am — Outbox backlog cleared (0 events)

TOTAL DURATION: 55 minutes
CUSTOMER IMPACT: 
  - 4 finance managers unable to create 
    payment runs (requests rolled back)
  - Downstream consumers (Notification, 
    Accounting Integration) delayed 
    ~55 minutes for invoice.approved events
  - No data loss — at-least-once Kafka 
    delivery, all events published after 
    connection pool recovered

ROOT CAUSE:
────────────
4 concurrent payment run creation requests 
with overlapping invoice selections caused 
lock contention on the invoices table 
(SELECT FOR UPDATE in createPaymentRun()).

No lock_timeout configured on DB sessions.
Contending sessions waited indefinitely 
for locks, holding their DB connections 
for 50+ seconds.

Combined with normal service load 
(4-6 connections for other operations),
this exhausted the 10-connection pool.

The OutboxPoller — which runs in its own
scheduled thread and requires a DB 
connection for every polling cycle — 
could not acquire connections.
Unprocessed events accumulated in the 
outbox_events table.

CONTRIBUTING FACTORS:
──────────────────────
1. No lock_timeout on DB sessions.
   Contending @Transactional methods 
   hold connections indefinitely when 
   waiting for locks.
   This is a configuration gap 
   across all our services — 
   not specific to Invoice Service.

2. HikariCP pool size of 10 is 
   appropriate for normal load but 
   provides no buffer when long-running 
   transactions hold connections.
   Thursday morning payment run day 
   is predictably high load.

3. No UI or API guard prevents 
   concurrent payment run creation 
   with overlapping invoice selections.
   This is a valid concurrent access 
   scenario that the pessimistic lock 
   is designed to handle — but without 
   lock_timeout, the handling degrades 
   the entire service rather than 
   returning a fast error.

4. OutboxPoller shares the application's 
   DB connection pool.
   When the pool is exhausted, 
   both the application AND the poller 
   are affected — the health of the 
   outbox is coupled to the health 
   of the overall connection pool.

ACTION ITEMS:
──────────────
1. Configure lock_timeout = 10s 
   for all invoice-service DB sessions.
   
   Implementation: set in HikariCP 
   connectionInitSql or in 
   Spring datasource properties.
   
   Effect: contending sessions throw 
   LockTimeoutException after 10 seconds.
   Application returns 409 Conflict 
   to the user with a clear message.
   
   Owner: [Your name]
   Due: This sprint (INV-312)

2. Handle LockTimeoutException in 
   GlobalExceptionHandler — return 
   409 with actionable message.
   
   "Another payment run is being 
   created for some of the selected 
   invoices. Please wait a moment 
   and try again."
   
   Owner: [Your name]
   Due: This sprint (INV-313)

3. Review lock_timeout configuration 
   across all services 
   (expense-service also uses 
   pessimistic locking for 
   payment runs).
   
   Owner: Arjun (team-wide review)
   Due: Next sprint

4. Consider separating OutboxPoller 
   connection pool from application pool.
   This would prevent application-level 
   connection exhaustion from affecting 
   outbox publishing.
   
   Note: This adds operational complexity.
   Evaluate whether lock_timeout fix 
   makes this unnecessary.
   
   Owner: Arjun (design decision)
   Due: Month 14 planning

5. Add a monitoring alert for 
   long-running transactions 
   (sessions holding connections 
   > 15 seconds).
   Proactive detection before 
   pool exhaustion.
   
   Owner: [Your name]
   Due: This sprint (INV-314)

WHAT WENT WELL:
────────────────
- OutboxHealthIndicator fired correctly 
  (written in month 10, first real 
  production trigger — it worked)
- Root cause identified in 14 minutes 
  from start of investigation
- Communication to #incidents was 
  clear and included analysis, 
  options, and recommendation
- No data loss — Kafka at-least-once 
  delivery, all 1,247 events published 
  after pool recovered
- Connection pool recovery was immediate 
  after sessions were terminated
```

---

## Arjun's Review of the Postmortem

Arjun commented on the Confluence draft:

```
Arjun Comment 1:
─────────────────
"The root cause section is accurate.
 
 One addition: explain WHY the 
 outbox poller shares the connection 
 pool.
 
 A reader six months from now 
 should understand the architectural 
 reason, not just the consequence.
 
 Something like:
 'The OutboxPoller is a Spring 
 @Scheduled component running 
 within the invoice-service JVM.
 It uses the same DataSource bean 
 as the rest of the application.
 This is the standard pattern in 
 Spring Boot — there is one 
 connection pool per DataSource.
 The coupling is by design, 
 not by accident.'"
```

You added it.

```
Arjun Comment 2:
─────────────────
"Action item 4 (separate OutboxPoller 
 pool) — I want to flag that this 
 is a non-trivial change.
 
 It would require configuring a 
 second DataSource bean specifically 
 for the poller, with its own 
 HikariCP pool.
 
 That's 2-3 days of work and 
 introduces a new configuration 
 dimension to maintain.
 
 My honest assessment: if we 
 implement lock_timeout (action 1),
 the pool exhaustion scenario 
 becomes very unlikely.
 
 I'd note this as 'under consideration, 
 pending evaluation after action 1 
 is deployed' rather than 
 'due next sprint.'
 
 Don't want to commit the team 
 to complexity that may not be needed."
```

You updated the phrasing:

```
4. Consider separating OutboxPoller 
   connection pool from application pool.
   
   Status: UNDER CONSIDERATION.
   Pending evaluation after action 1 
   (lock_timeout) is deployed 
   and monitored for 2 sprints.
   If lock_timeout prevents recurrence,
   separate pool may be unnecessary.
   
   Owner: Arjun (design decision if pursued)
   Due: Re-evaluate in month 15 planning
```

```
Arjun Comment 3:
─────────────────
"The 'What Went Well' section is 
 accurate and important.
 
 I want to add one thing you 
 didn't write but should:
 
 'The investigating engineer had 
 enough context to identify root 
 cause independently before 
 escalating, which reduced 
 resolution time significantly.'
 
 That's worth naming.
 In Story 9, I led the investigation 
 and you documented.
 In this incident, you led and 
 escalated with analysis, not 
 just a symptom.
 
 That's growth worth recording 
 in the postmortem."
```

```
You paused at this comment.

Arjun was adding to the postmortem 
a note about your growth.
Not a compliment in Slack.
A formal note in a document 
that would live in Confluence 
permanently.

You read it three times.
Then you added it to the document.

Not because Arjun told you to.
Because it was accurate.
And because accurate postmortems —
honest ones that capture what 
worked, not just what broke —
are what make teams better.
```

---

## The Implementation — Action Items 1, 2, and 5

You shipped three of the five action items that same sprint.

### Action Item 1 — lock_timeout

```properties
# application-prod.properties — invoice-service

# New: lock_timeout for DB sessions
# After 10 seconds waiting for a lock,
# throw LockTimeoutException.
# Prevents connection pool exhaustion 
# from long-running lock contention.
spring.datasource.hikari.connection-init-sql=\
  SET lock_timeout = '10s'
```

```
Why connectionInitSql?
────────────────────────
lock_timeout is a PostgreSQL 
SESSION parameter.
Setting it in connectionInitSql 
means it runs when each connection 
is first created from the pool.
Every connection in the pool 
will have lock_timeout = 10s 
from the moment it's established.

Alternative: set it in the 
PostgreSQL server config globally.
But that would affect ALL connections 
to the DB — including direct psql 
sessions used by DevOps.
Application-level setting via 
connectionInitSql is more targeted.
```

### Action Item 2 — Handle LockTimeoutException

```java
// Added to GlobalExceptionHandler:
@ExceptionHandler({
    LockTimeoutException.class,
    PessimisticLockingFailureException.class
})
public ResponseEntity<ErrorResponse> 
        handleLockTimeout(
        Exception ex,
        HttpServletRequest request) {

    log.warn("Lock timeout on request: {} {}. " +
        "Possible concurrent modification.",
        request.getMethod(),
        request.getRequestURI()
    );

    return ResponseEntity.status(409).body(
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(409)
            .error("CONCURRENT_MODIFICATION")
            .message(
                "Another operation is in progress " +
                "for the requested resources. " +
                "Please wait a moment and try again."
            )
            .path(request.getRequestURI())
            .traceId(getCurrentTraceId())
            .build()
    );
}
```

```
Why PessimisticLockingFailureException 
as well as LockTimeoutException?
────────────────────────────────────────
Spring wraps different DB-level lock 
exceptions depending on the situation.

LockTimeoutException: 
  Standard JPA exception when 
  lock_timeout fires.

PessimisticLockingFailureException:
  Spring's generic wrapper for 
  pessimistic locking failures 
  (could be DB-level lock timeout 
  or lock unavailable immediately).

Catching both ensures any 
lock-related failure returns 
a 409, not a 500.
```

### Action Item 5 — Long-Running Transaction Alert

```java
// Added to InvoiceMetrics:
@Component
@RequiredArgsConstructor
public class InvoiceMetrics {

    private final MeterRegistry meterRegistry;

    // New: timer for payment run creation
    // Tracks how long createPaymentRun() takes
    // If P99 > 10s → likely lock contention
    public Timer.Sample startPaymentRunTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentRunDuration(
            Timer.Sample sample,
            boolean success) {

        sample.stop(
            Timer.builder("payment_run.creation.duration")
                .tag("success", String.valueOf(success))
                .description(
                    "Duration of payment run creation " +
                    "(includes lock acquisition time)")
                .register(meterRegistry)
        );
    }
}
```

```java
// Updated InvoiceService:
@Transactional
public PaymentRun createPaymentRun(
        List<UUID> invoiceIds,
        LocalDate scheduledDate,
        UUID createdById) {

    Timer.Sample sample = 
        invoiceMetrics.startPaymentRunTimer();

    try {
        List<Invoice> invoices = invoiceRepository
            .findAndLockForPaymentRun(invoiceIds);

        // ... rest of creation logic ...

        invoiceMetrics.recordPaymentRunDuration(
            sample, true);

        return run;

    } catch (LockTimeoutException 
             | PessimisticLockingFailureException e) {
        
        invoiceMetrics.recordPaymentRunDuration(
            sample, false);
        throw e; // Re-throw for GlobalExceptionHandler
    }
}
```

You added a Datadog alert:

```
Alert: payment_run.creation.duration P99 > 8s
Condition: for > 2 minutes
Severity: WARNING
Channel: #expense-ap-alerts

Rationale: 
  lock_timeout = 10s.
  If P99 reaches 8s, some requests 
  are waiting close to the timeout limit.
  This is an early warning before 
  timeouts actually start firing.
  Gives us time to investigate before 
  users see errors.
```

---

## What Happened After — Two Weeks Later

Two weeks after the fix deployed, you pulled the numbers to confirm efficacy:

```
Payment run creation — 2 weeks post-fix:

Lock contention events (P99 > 1s):
  Week before fix: 4 events (the incident)
  Week 1 after fix: 2 events
    → Both returned 409 to users
    → Users retried and succeeded
    → No pool exhaustion
  Week 2 after fix: 0 events

OutboxHealthIndicator:
  Remained UP continuously for 14 days.
  0 CRITICAL or WARNING alerts.
  (Previously: 1 CRITICAL in the incident)

Long-running transaction alert:
  Did not fire in 14 days.
  P99 for payment run creation: 34ms
    (vs 45,000ms during the incident)
```

Arjun reviewed the numbers in the sprint retro:

```
Arjun (retro):
───────────────
"The lock_timeout fix worked.
 We had two contention events 
 in week 1 — both handled 
 correctly with 409s.
 No pool exhaustion, no outbox backlog.
 
 The fix is correct.
 The monitoring is working.
 
 One thing I want to name:
 [Your name] ran this incident 
 end-to-end — investigation, 
 mitigation, postmortem, action items.
 
 That's ownership.
 That's what we expect as 
 someone grows into their role.
 
 Good work."
```

---

## What This Story Was Actually About

```
The technical content — lock_timeout, 
pg_terminate_backend, HikariCP pool 
exhaustion, connection-init-sql —
was real and correct.

But the story isn't about PostgreSQL 
configuration.

It's about one thing:

You were the first person to see 
the alert.
You were the first to investigate.
You posted to #incidents with 
analysis, not just a symptom.
You identified root cause 14 minutes 
after you started looking.
You proposed options.
You executed the mitigation.
You watched the recovery.
You wrote the postmortem.
You shipped the fixes.

And you did all of this 
before asking Arjun for help.

You told Arjun what was happening.
He confirmed your direction.
He reviewed your postmortem.

But you led it.

That's the difference between 
month 8 (observing Arjun lead) 
and month 13 (you leading, 
Arjun supporting).

The gap between those two moments 
is what growth looks like.
Not a single insight.
Not a single dramatic breakthrough.
Five months of smaller stories, 
each one adding a layer of 
understanding, until you had 
enough context to move through 
a live production problem 
without freezing.

And the OutboxHealthIndicator — 
the one you wrote in month 10 
because Elena asked you to add 
custom health checks — fired 
correctly and led you directly 
to the problem in under 
10 minutes.

The thing you built two months ago 
helped you solve the incident today.

That's what ownership means over time.
```

---

## The "Tricky Question" Preparation

---

**Q1: "Walk me through how you identified the root cause of this incident."**

```
I worked through it in layers, 
starting from the alert and 
narrowing inward.

The alert came from OutboxHealthIndicator —
1,247 unpublished events, growing.
First question: is the poller running 
or is it running and failing?

I checked the logs filtered to 
OutboxPoller. I found it was running 
but throwing PoolInitializationException — 
PostgreSQL refusing connections because 
the pool was exhausted.

So the outbox wasn't the root cause — 
it was a victim. Something else 
had exhausted the connection pool.

Second question: what exhausted the pool?

I pulled the HikariCP metrics from Datadog.
The jdbc.connections.active went from 
4 to 10 in under 2 minutes, starting at 9:21am.
Something opened 6 long-running connections 
almost simultaneously.

I searched APM traces for long-running 
operations that started around 9:21am.
Found 4 traces for POST /api/v1/payment-runs, 
each running for 45+ seconds.
All stuck in findAndLockForPaymentRun —
a SELECT FOR UPDATE query.

That's pessimistic locking.
4 concurrent requests with overlapping 
invoice selections — each holding a 
connection and waiting for the others 
to release their locks.
No lock_timeout configured — 
they'd wait forever.

4 long-running connections plus 
normal load saturated the 10-connection pool.
Everything else — including the outbox 
poller — couldn't get connections.

Root cause: 4 concurrent payment run 
creation requests in lock contention, 
combined with no lock_timeout, 
exhausting the connection pool.

Total time to root cause: 14 minutes 
from when I started looking.
```

---

**Q2: "You used pg_terminate_backend to kill the stuck sessions. What exactly does that do and is it safe?"**

```
pg_terminate_backend(pid) is a 
PostgreSQL administrative function 
that sends a termination signal 
to a specific backend process 
(a DB session).

The effect on the session:
The connection is forcibly closed.
Any in-progress transaction is 
rolled back.
The client (our service) receives 
a connection error.
HikariCP detects the closed connection 
and removes it from the pool, 
opening a fresh one.

Is it safe?
For our scenario — yes, with caveats.

The 4 sessions were in a deadlock-like 
state — each waiting for the others 
to release locks. No useful work 
was being done. None of them would 
ever complete without external intervention.

Rolling back the in-progress transactions 
meant the payment run creation failed —
the invoices were not locked into a 
payment run. The finance managers 
saw their requests fail.

This is acceptable because:
1. No data was corrupted — the 
   rollback is clean.
2. The invoices went back to their 
   APPROVED state, available for 
   new payment run creation.
3. The alternative — letting them 
   wait indefinitely — would have 
   continued blocking the outbox 
   poller and all other DB operations.

The caveats:
pg_terminate_backend requires superuser 
or pg_signal_backend privileges.
Only engineers with production DB 
access (Arjun, Elena, DevOps team) 
should execute this.
I confirmed with Arjun before executing.

After the incident, we configured 
lock_timeout = 10s, which means 
this kind of manual intervention 
should never be needed again for 
this scenario — the DB will 
terminate the waiting session 
automatically after 10 seconds.
```

---

**Q3: "Why does the OutboxPoller share the application's connection pool? Couldn't you give it its own pool?"**

```
The OutboxPoller is a @Scheduled 
component inside the invoice-service 
Spring application.
It uses the same DataSource bean 
as the rest of the application.

In Spring Boot, there's one DataSource 
by default, and all components 
that need a DB connection — 
services, repositories, schedulers — 
draw from that same pool.

The coupling is by design, not accident.
Having one pool is simpler to configure, 
monitor, and reason about.

Could we give the poller its own pool?
Yes — configure a second DataSource 
bean specifically for the poller, 
with its own HikariCP pool.
This would mean application-level 
pool exhaustion would not affect 
outbox publishing.

But this adds meaningful complexity:
Two DataSource beans to configure.
Two separate pool metrics to monitor.
Transaction management becomes 
more complicated — which DataSource 
does @Transactional use?
New engineers might be confused 
about why there are two pools.

The action item in the postmortem 
flagged this as under consideration 
pending evaluation of lock_timeout.

After two weeks of monitoring 
post lock_timeout fix: zero outbox 
health alerts. The pool exhaustion 
scenario hasn't recurred.
Our conclusion: lock_timeout prevents 
the conditions that would cause 
pool exhaustion, making a separate 
pool unnecessary for now.

This could be revisited if we 
see a different scenario causing 
pool exhaustion that affects the poller.
But we don't prematurely add 
complexity for problems we're not 
actually having.
```

---

**Q4: "This was the first incident you led. What was different about it compared to Story 9 where you were an observer?"**

```
In Story 9, I was watching Arjun 
and taking notes. My job was to 
document and look things up when asked.

In this incident, I was the person 
who had to decide what to look at next.

The difference wasn't the tools — 
Datadog, the DB queries, the Slack 
communication. Those were the same.

The difference was the mental model.

In Story 9, I saw Arjun ask "what 
changed recently?" and I noted it down.
In this incident, that question was 
the first thing I thought of, 
automatically, because I had 
watched him do it once and 
understood why.

I think what made it possible 
to run this independently was 
that I had enough mental models 
to know what kind of problem 
to look for.

Consumer lag always has two causes: 
more load or slower processing.
Check both.

If the consumer is failing, 
look at what the consumer touches.
A DB connection error means 
the connection pool.
Connection pool exhaustion means 
something is holding connections 
for longer than normal.
Long-running connections often 
mean long-running transactions, 
which often mean lock contention.

That chain of reasoning came quickly 
because each link was something 
I had either seen or thought about before.

The other difference: 
in Story 9, Arjun was the person 
keeping Lukas informed while 
debugging simultaneously.
In this incident, that was me.

Writing "root cause: X, options: A/B/C" 
in #incidents while still investigating 
was something I hadn't had to do before.
Making the communication clear and 
actionable while under pressure 
was a skill I hadn't practiced.

It was harder than the debugging itself.
```

---

**Q5: "You wrote the postmortem yourself and Arjun reviewed it. How did his review change it?"**

```
Three meaningful changes.

First: he asked me to explain WHY 
the outbox poller shares the 
application's connection pool — 
not just that it does.
That's the difference between 
recording a fact and explaining 
a design decision.
Someone reading in six months 
needs the context to evaluate 
whether the situation should change.

Second: he downgraded action item 4 
(separate connection pool) from 
a committed sprint item to 
"under consideration."
He was right — committing to 
3 days of complexity for a 
problem that lock_timeout might 
eliminate would have been premature.
He was protecting the team from 
over-engineering based on one incident.

Third: he added a note about 
my growth to the "What Went Well" 
section — specifically that I led 
the investigation and escalated 
with analysis instead of just 
a symptom.

That third one surprised me.
It was a formal record, in a 
document that lives permanently 
in Confluence, noting that 
I had grown in how I handled incidents.

His framing was that accurate 
postmortems capture what worked 
as well as what broke.
My investigation approach was 
something that worked.
It belonged in the record.

I would not have put it there myself.
I probably would have considered 
it self-congratulatory.
But Arjun was right — postmortems 
that only document failures miss 
half the learning.
```

---

Block 5, Story 16 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  DB connection pool exhaustion —
    how to diagnose with Datadog 
    and pg_stat_activity.
  Pessimistic locking and lock 
    contention in production.
  lock_timeout configuration 
    via connectionInitSql.
  pg_terminate_backend — 
    what it does and when it's safe.
  CREATE INDEX CONCURRENTLY 
    (reinforced from Story 9).
  LockTimeoutException handling 
    in GlobalExceptionHandler.
  Custom Micrometer timer for 
    payment run creation latency.

Behavioral:
  Led the investigation independently 
    before escalating.
  Posted to #incidents with 
    analysis + options, not just symptoms.
  Communicated clearly under pressure.
  Wrote the postmortem with 
    honest contributing factors.
  Shipped 3 of 5 action items 
    in the same sprint.

Growth marker:
  Story 9 (month 8): 
    observer in war room,
    documented Arjun's investigation.
  Story 16 (month 13):
    led the investigation,
    Arjun reviewed the postmortem.
  
  That five-month gap is what 
  "Feature Ownership" means.
```
