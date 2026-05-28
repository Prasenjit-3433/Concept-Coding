# Story 9: Production Incident War Room — First Time Inside a Real Incident

---

## Context — Where You Were at Month 8

```
Month 7 end — what had changed:
─────────────────────────────────
You had shipped your first Kafka consumer.
Arjun had become your primary mentor 
for event-driven work.
You were starting to understand 
the system beyond just your 
immediate service.

But there was a category of experience 
you still had zero exposure to:
production incidents.

In months 1-7:
  You had caused no production incidents.
  You had fixed bugs before they 
  reached production.
  You had read about incidents 
  in postmortems on Confluence 
  (the team wrote these when 
   something broke in prod).
  But you had never been IN one.
  Never seen how the team responded 
  in real time.
  Never watched an engineer 
  debug something live with 
  real customer impact.

Month 8, week 2.
That changed.
```

---

## The Situation

It was a Tuesday afternoon — 2:30pm IST, 11:00am CET. You were working on a small refactoring ticket, half-focused, when your Slack lit up:

```
#expense-ap-alerts (Slack):
─────────────────────────────
[Datadog Alert] CRITICAL
service: expense-service
metric: kafka.consumer.records-lag
  consumer_group: expense-service
  topic: expense.submitted
  
Current lag: 4,847
Threshold:   1,000
Duration:    12 minutes

[Datadog Alert] WARNING  
service: expense-service
metric: http.server.requests
  uri: PUT /api/v1/expenses/{id}/submit
  outcome: SERVER_ERROR
  
Error rate: 8.3% (normally <0.1%)
Duration:   14 minutes
```

Two alerts firing simultaneously. Consumer lag growing. HTTP errors on the submit endpoint.

Within 90 seconds, the `#incidents` channel had activity:

```
#incidents (Slack):
────────────────────
Arjun: "expense-service consumer lag 
        spike on expense.submitted. 
        Starting investigation.
        
        @Lukas — FYI, potential customer 
        impact. Expense submissions may 
        be failing for some users."

Lukas: "Acknowledged. Keep me posted.
        @Arjun roping in for support 
        if needed?"

Arjun: "[Your name] — join the 
        incident call. Good learning 
        opportunity. 
        Meet: [Google Meet link]"
```

You joined the call within 2 minutes. It was Arjun and Sophie. Arjun was sharing his screen — Datadog was open.

```
Your role in this incident:
────────────────────────────
NOT: lead investigator
NOT: person fixing the problem

YES: observer and support
     Arjun was running the investigation.
     You were there to:
     - Watch how he debugged
     - Help with anything he asked
     - Learn the incident response process
     - Take notes (Arjun asked you to 
       document the timeline as it unfolded)
```

This is an important framing. You were not the hero of this story. Arjun was. What you were doing was learning how a senior engineer diagnoses a live production problem — something no course or book teaches.

---

## Your Task

```
Explicit task from Arjun 
at the start of the call:
──────────────────────────
"[Your name] — two things from you.

 One: keep a timeline document 
 open. Write down each thing 
 we check, what we found, 
 what time it was.
 This becomes the postmortem.
 
 Two: when I ask you to look 
 something up — look it up fast.
 I'll need specific queries 
 or dashboards pulled.
 
 Don't try to diagnose it yourself 
 yet. Just watch how I move 
 through this."
```

You opened a blank Confluence page titled "INCIDENT-2025-03-18 — expense.submitted consumer lag" and started writing.

---

## The Action — Watching How Arjun Debugged

### Step 1 — Establish What Changed

```
Arjun (11:02am CET):
─────────────────────
"First question: what changed 
 recently that could cause this?
 
 Consumer lag means one of two things:
 Either the producer is publishing 
 MORE events than normal —
 OR the consumer is processing 
 events SLOWER than normal.
 
 Or both.
 
 Let's check both."
```

Arjun opened the Datadog dashboard and pulled two metrics side by side:

```
Dashboard: expense.submitted producer rate
  vs consumer throughput

Producer rate (events/min):
  10:40am: 23 events/min (normal)
  10:45am: 24 events/min (normal)
  10:50am: 189 events/min ← spike
  10:55am: 201 events/min
  11:00am: 178 events/min

Consumer throughput (events/min processed):
  10:40am: 23 events/min (normal)
  10:45am: 23 events/min (normal)
  10:50am: 23 events/min ← NOT keeping up
  10:55am: 19 events/min ← slowing down
  11:00am: 12 events/min ← getting worse
```

```
Arjun:
───────
"Producer rate spiked 8x at 10:50am.
 Consumer rate stayed flat then 
 started dropping.
 
 Two separate problems:
 1. Why is producer rate 8x normal?
 2. Why is consumer processing 
    slowing down instead of keeping up?
 
 Usually one causes the other.
 Let's find root cause."
```

```
What you wrote in the timeline:
─────────────────────────────────
11:02am — Two issues identified:
  Producer rate: spiked from ~23/min 
                 to ~200/min at 10:50am
  Consumer rate: stayed flat then 
                 started declining
  Hypothesis: consumer having 
  trouble keeping up with spike
```

### Step 2 — Check What Changed at 10:50am

```
Arjun:
───────
"Something happened at 10:50am.
 Let's check deployments first."
```

He opened GitHub Actions:

```
GitHub Actions — recent deployments:

expense-service:
  10:48am — deployed commit abc1234
  (previous deploy: yesterday 3:12pm)

invoice-service:
  (no recent deploy)
```

```
Arjun:
───────
"Deployment at 10:48am.
 Producer spike started at 10:50am.
 2 minutes after deploy.
 That's not a coincidence.
 
 Let's look at what changed in abc1234."
```

He opened GitHub and pulled the diff:

```
Commit abc1234 — changes:
──────────────────────────
1. EXP-271: Add expense amount 
   validation improvement 
   (Tomás's ticket)
   
   Changed: ExpenseController.java
   Changed: CreateExpenseRequest.java

2. EXP-273: Fix null handling 
   in receipt URL generation
   (Sophie's ticket)
   
   Changed: ReceiptStorageService.java
```

```
Arjun (scanning the diff):
────────────────────────────
"Nothing in the producer code...
 nothing in Kafka config...
 
 Wait.
 
 Look at EXP-271.
 ExpenseController.
 The submit method."
```

He opened the diff for `ExpenseController.java`:

```java
// BEFORE (Tomás's change):
@PostMapping("/{expenseId}/submit")
public ResponseEntity<ExpenseResponse> 
        submitExpense(
        @PathVariable UUID expenseId,
        @RequestHeader("X-User-Id") UUID userId) {

    ExpenseResponse response = 
        expenseService.submitExpense(
            expenseId, userId);

    return ResponseEntity.ok(response);
}

// AFTER (Tomás's change):
@PostMapping("/{expenseId}/submit")
public ResponseEntity<ExpenseResponse> 
        submitExpense(
        @PathVariable UUID expenseId,
        @RequestHeader("X-User-Id") UUID userId) {

    ExpenseResponse response = 
        expenseService.submitExpense(
            expenseId, userId);

    return ResponseEntity.ok(response);
}
```

The controller looked identical. Arjun looked deeper — into the service:

```java
// ExpenseService.submitExpense() 
// BEFORE Tomás's change:
@Transactional
public ExpenseResponse submitExpense(
        UUID expenseId, UUID userId) {

    Expense expense = expenseRepository
        .findById(expenseId).orElseThrow();

    // validation...
    expense.setStatus(
        ExpenseStatus.PENDING_APPROVAL);
    expenseRepository.save(expense);

    // outbox event written
    outboxEventRepository.save(outboxEvent);

    return ExpenseResponse.from(expense);
}
```

```java
// ExpenseService.submitExpense() 
// AFTER Tomás's change — 
// validation was added here
@Transactional
public ExpenseResponse submitExpense(
        UUID expenseId, UUID userId) {

    Expense expense = expenseRepository
        .findById(expenseId).orElseThrow();

    // NEW: Tomás added this validation
    validateExpenseForSubmission(expense);

    expense.setStatus(
        ExpenseStatus.PENDING_APPROVAL);
    expenseRepository.save(expense);

    outboxEventRepository.save(outboxEvent);

    return ExpenseResponse.from(expense);
}

// NEW method added by Tomás:
private void validateExpenseForSubmission(
        Expense expense) {

    if (expense.getAmount() == null || 
            expense.getAmount()
                   .compareTo(BigDecimal.ZERO) 
                   <= 0) {
        throw new InvalidExpenseException(
            "Amount must be positive");
    }

    if (expense.getCategory() == null) {
        throw new InvalidExpenseException(
            "Category is required");
    }

    // NEW validation Tomás added:
    // Check for duplicate submissions
    // (same employee, same amount, 
    //  same date, submitted in last hour)
    boolean isDuplicate = expenseRepository
        .existsDuplicateSubmission(
            expense.getEmployeeId(),
            expense.getAmount(),
            expense.getExpenseDate(),
            Instant.now().minus(1, HOURS)
        );

    if (isDuplicate) {
        throw new DuplicateExpenseException(
            "Possible duplicate submission 
             detected");
    }
}
```

```
Arjun (spotting it):
──────────────────────
"Found it.
 
 Tomás added a duplicate submission 
 check. Let's look at that query."
```

He opened `ExpenseRepository`:

```java
// The query Tomás wrote:
@Query("""
    SELECT COUNT(e) > 0 FROM Expense e
    WHERE e.employeeId = :employeeId
    AND e.amount = :amount
    AND e.expenseDate = :expenseDate
    AND e.submittedAt > :since
    AND e.status NOT IN ('DRAFT', 'CANCELLED')
    """)
boolean existsDuplicateSubmission(
    @Param("employeeId") UUID employeeId,
    @Param("amount") BigDecimal amount,
    @Param("expenseDate") LocalDate expenseDate,
    @Param("since") Instant since
);
```

```
Arjun:
───────
"This query runs on every single 
 expense submission.
 
 Let's check what it's doing in 
 the DB right now."
```

He opened Datadog APM and searched for slow queries in the last 30 minutes. Filtered by expense-service. Sorted by total time:

```
Top DB queries by total time 
(last 30 min):
───────────────────────────────────────
1. existsDuplicateSubmission query
   Average: 2,340ms ← 
   Total calls: 847
   Total DB time: 1,979,580ms (33 minutes)

2. findByCompanyIdAndStatus (expenses)
   Average: 45ms (normal)
   Total calls: 1,203
   
3. findById (expenses)  
   Average: 8ms (normal)
   Total calls: 4,891
```

```
Arjun:
───────
"There it is.
 
 2,340 milliseconds average 
 for one query.
 That query runs on EVERY 
 expense submission.
 
 With 200 submissions per minute:
 200 × 2,340ms = 468 seconds 
 of DB time per minute.
 
 But the consumer pool can only 
 handle maybe 10-15 concurrent 
 DB connections.
 Those connections are being held 
 for 2+ seconds each.
 
 New submissions are queuing.
 DB connection pool is exhausted.
 Some submissions time out.
 
 That's why you see HTTP errors 
 on the submit endpoint —
 and why the Kafka consumer is 
 slowing down (it also uses 
 DB connections, and the pool 
 is saturated)."
```

```
What you wrote in the timeline:
─────────────────────────────────
11:09am — Root cause identified:
  Tomás's duplicate check query 
  (added in deploy abc1234) 
  running at avg 2,340ms per call.
  No index on the columns it searches.
  At 200 submissions/min, 
  saturating DB connection pool.
  Causing: HTTP errors on submit endpoint,
           Kafka consumer slowdown 
           (shared DB pool)
  
  Missing index:
  expenses(employee_id, amount, 
           expense_date, submitted_at)
```

### Step 3 — The Decision

```
Arjun (11:10am):
─────────────────
"Two options.
 
 Option 1: Rollback the deploy.
   Fast. Safe. Removes the problem.
   Downside: Tomás's other fix 
   (EXP-271 other changes) also 
   rolls back.
   
 Option 2: Add the index now.
   Faster than rollback if the 
   index builds quickly.
   Keeps Tomás's fix.
   Slightly more risk.
   
 How big is the expenses table?
 [Your name] — can you check?"
```

You ran a quick query:

```sql
SELECT COUNT(*) FROM expenses;
-- Result: 284,391 rows

SELECT pg_size_pretty(
    pg_total_relation_size('expenses')
);
-- Result: 847 MB
```

You reported back:

```
You:
─────
"284,000 rows, 847MB total."
```

```
Arjun:
───────
"Index build on 284,000 rows 
 will take about 10-15 seconds 
 for PostgreSQL.
 
 We can do this WITHOUT locking 
 the table using CONCURRENTLY.
 
 Going with Option 2 — 
 adding the index live.
 
 Lukas — adding index CONCURRENTLY 
 to fix the slow query. 
 ETA 2 minutes."
```

```
Lukas (Slack):
───────────────
"Approved. Proceed."
```

### Step 4 — The Fix

Arjun connected to the production DB (he had credentials — you did not yet) and ran:

```sql
-- CREATE INDEX CONCURRENTLY means:
-- PostgreSQL builds the index 
-- without locking the table.
-- Reads and writes continue normally 
-- during index build.
-- Takes longer than regular CREATE INDEX
-- but safe for production use.

CREATE INDEX CONCURRENTLY 
    idx_expenses_duplicate_check
ON expenses(
    employee_id, 
    amount, 
    expense_date, 
    submitted_at
)
WHERE status NOT IN ('DRAFT', 'CANCELLED');
-- Partial index — only indexes rows 
-- that the query actually filters
```

```
Arjun (explaining while he typed):
────────────────────────────────────
"CONCURRENTLY is important here.
 Regular CREATE INDEX takes a lock 
 on the table — no writes during build.
 With 200 submissions/minute hitting 
 this table, a regular index would 
 cause more downtime while building.
 
 CONCURRENTLY builds the index 
 in the background.
 No table lock.
 Takes longer but safe.
 
 The partial index WHERE clause 
 matches the query's filter.
 Smaller index = faster build, 
 less memory."
```

The index built in 14 seconds.

### Step 5 — Watching the Recovery

All three of you watched the Datadog dashboard in real time:

```
Metrics after index created 
(11:14am - 11:20am):

existsDuplicateSubmission avg duration:
  11:11am: 2,340ms
  11:12am: 2,280ms (index building)
  11:14am: 8ms    ← index ready
  11:15am: 6ms
  11:16am: 7ms

HTTP error rate (submit endpoint):
  11:11am: 8.3%
  11:14am: 1.2%
  11:15am: 0.1%
  11:16am: 0.0% ← resolved

Kafka consumer lag (expense.submitted):
  11:11am: 6,203 (still growing)
  11:14am: 5,891 (slowing)
  11:16am: 3,421 (decreasing)
  11:20am: 847   (burning down)
  11:28am: 0     ← resolved
```

```
Arjun (watching the lag decrease):
────────────────────────────────────
"Consumer is burning down the backlog.
 Lag hit zero at 11:28am.
 Total incident duration: 38 minutes.
 
 No data loss — at-least-once delivery,
 all events will be processed.
 
 Good. Now we write the postmortem."
```

---

### The Postmortem — What You Wrote

Arjun assigned you to write the postmortem with his guidance. This was intentional — writing a postmortem forces you to understand what happened deeply enough to explain it clearly.

```
INCIDENT POSTMORTEM — EXP-2025-03-18
Drafted by: [Your name]
Reviewed by: Arjun Sharma

TIMELINE:
──────────
10:48am — Deployment of commit abc1234
           (EXP-271, EXP-273)
10:50am — expense.submitted producer 
           rate spike (23/min → 200/min)
10:52am — Kafka consumer lag begins 
           accumulating
10:53am — HTTP error rate on submit 
           endpoint begins rising (8% errors)
11:00am — Datadog alerts fire (lag > 1000,
           error rate > threshold)
11:02am — Incident call started 
           (Arjun, Sophie, [Your name])
11:09am — Root cause identified:
           missing index on duplicate 
           submission check query
11:10am — Decision: add index CONCURRENTLY
11:13am — CREATE INDEX CONCURRENTLY 
           executed
11:14am — Index ready, query performance 
           restored (2,340ms → 8ms)
11:16am — HTTP errors resolved
11:28am — Kafka consumer lag resolved (0)

TOTAL DURATION: 38 minutes
CUSTOMER IMPACT: Expense submission 
  failures for ~38 minutes. 
  Estimated 8% of submission attempts 
  failed during peak.

ROOT CAUSE:
────────────
A new duplicate submission detection 
query was added in EXP-271 without 
a corresponding DB index.

The query searches 4 columns 
(employee_id, amount, expense_date, 
submitted_at) with no existing index 
covering these columns.

At normal submission rate (~23/min), 
this was tolerable (though still slow).

At a higher-than-normal submission 
rate following a weekly batch of 
employee expense submissions 
(Tuesday morning is historically 
the busiest submission time —
employees submitting weekend expenses),
the query became the bottleneck.

WHY DIDN'T WE CATCH THIS IN STAGING?
───────────────────────────────────────
Staging database has ~4,000 rows 
(test data only, not production scale).
The query ran in 12ms on staging.
No performance issue visible.
Production has 284,000 rows.
The query ran in 2,340ms on production.

CONTRIBUTING FACTORS:
──────────────────────
1. No query performance review 
   as part of the PR process.
   The query was reviewed for 
   correctness, not performance.

2. Staging DB is too small to 
   represent production query behavior.
   284,000 rows in production vs 
   4,000 rows in staging = 70x difference.

3. Tuesday morning is peak submission time.
   Deploy was Monday morning.
   The problem only manifested at scale 
   the next morning.

ACTION ITEMS:
──────────────
1. Add Flyway migration to create 
   the index properly 
   (already exists as CONCURRENTLY 
    in production — migration ensures 
    it exists in staging and new 
    deployments).
   
   Owner: [Your name]
   Due: This sprint

2. Add to PR review checklist:
   "For any new DB query — 
    what index supports it?"
   
   Owner: Arjun (to add to team wiki)
   Due: This week

3. Consider seeding staging DB 
   with a representative subset 
   of production data (anonymized).
   Current staging data too sparse 
   for query performance testing.
   
   Owner: Lukas (to discuss with DevOps)
   Due: Next sprint discussion

WHAT WENT WELL:
────────────────
- Datadog alerts fired correctly 
  and quickly (12-14 minutes 
  after problem started)
- Root cause identified in 7 minutes 
  after incident call started
- Fix applied without additional 
  downtime (CONCURRENTLY)
- At-least-once Kafka delivery 
  prevented data loss —
  all submission events were 
  eventually processed
- Consumer backlog cleared 
  completely within 14 minutes 
  of fix
```

You sent the draft to Arjun. He read it and added one comment:

```
Arjun's edit to the postmortem:
─────────────────────────────────
"Add this under Contributing Factors:

4. The new query was added in a service 
   method that runs on every expense 
   submission — a hot path.
   New queries added to hot paths 
   need explicit performance review 
   regardless of their apparent simplicity.
   
   A 'simple' COUNT query can be 
   catastrophic without an index 
   at production scale."
```

You added it. Lukas approved the postmortem. It was published on Confluence.

---

### The Follow-Up — Action Item 1

The next day, you created the Flyway migration for the index:

```sql
-- V12__add_index_for_duplicate_expense_check.sql

-- This index was created CONCURRENTLY 
-- in production during incident 
-- EXP-2025-03-18 to fix a query 
-- performance issue.
-- This migration ensures the index 
-- exists in all environments.

-- Note: CONCURRENTLY cannot be used 
-- inside a transaction block.
-- Flyway wraps migrations in transactions 
-- by default.
-- We disable the transaction wrapper 
-- for this specific migration.
-- See: flywayDisableTransactionForMigration=true

CREATE INDEX IF NOT EXISTS 
    idx_expenses_duplicate_check
ON expenses(
    employee_id,
    amount,
    expense_date,
    submitted_at
)
WHERE status NOT IN ('DRAFT', 'CANCELLED');
```

This required a non-trivial configuration — Flyway wraps migrations in transactions by default, but `CREATE INDEX CONCURRENTLY` cannot run inside a transaction. You had to mark this specific migration as non-transactional.

You went back to Arjun:

```
You:
─────
"The index needs CONCURRENTLY 
 to avoid locking the table.
 But Flyway runs migrations in 
 transactions by default.
 CONCURRENTLY can't run inside 
 a transaction.
 
 How do I handle this?"
```

```
Arjun:
───────
"Two options.
 
 Option 1: Use CREATE INDEX 
 without CONCURRENTLY in the migration.
 Regular index creation locks 
 the table briefly.
 For staging and new deployments 
 (empty tables), that's fine.
 For production, we already have 
 the index from the live fix.
 
 Option 2: Mark the migration 
 as non-transactional using 
 Flyway's executeInTransaction=false.
 Then CONCURRENTLY works.
 
 For production safety — 
 always CONCURRENTLY on large tables.
 Option 2 is better."
```

You marked the migration with `@NonTransactional` using a Flyway callback:

```sql
-- V12__add_index_for_duplicate_expense_check.sql
-- flyway:executeInTransaction=false

CREATE INDEX CONCURRENTLY IF NOT EXISTS 
    idx_expenses_duplicate_check
ON expenses(
    employee_id,
    amount,
    expense_date,
    submitted_at
)
WHERE status NOT IN ('DRAFT', 'CANCELLED');
```

The `IF NOT EXISTS` clause handles the case where production already has the index (from the live fix) — it skips silently.

Sophie reviewed the migration:

```
Sophie:
────────
"Good. The IF NOT EXISTS is correct — 
 without it, this would fail in 
 production because the index 
 already exists there.
 
 One thing to add to the migration 
 comment: explain WHY this is marked 
 non-transactional.
 Someone reading this in 6 months 
 shouldn't have to figure it out."
```

You updated the comment:

```sql
-- V12__add_index_for_duplicate_expense_check.sql
-- flyby:executeInTransaction=false
-- 
-- Reason for non-transactional:
-- CREATE INDEX CONCURRENTLY cannot run 
-- inside a transaction block.
-- CONCURRENTLY is required to avoid 
-- locking the expenses table during 
-- index build (production has 280k+ rows).
-- Without CONCURRENTLY, index build 
-- would block all writes for ~15 seconds.
--
-- This migration was created after 
-- incident EXP-2025-03-18.
-- See postmortem in Confluence.

CREATE INDEX CONCURRENTLY IF NOT EXISTS 
    idx_expenses_duplicate_check
ON expenses(
    employee_id,
    amount,
    expense_date,
    submitted_at
)
WHERE status NOT IN ('DRAFT', 'CANCELLED');
```

---

## The Broader Lesson — What the Incident Taught You

You kept notes after this incident. Three things stood out:

```
Lesson 1: Debug by elimination, 
          not by guessing
──────────────────────────────────
Arjun never guessed.
He moved through a sequence:

1. What are the symptoms? 
   (lag + errors)
2. What are the possible causes? 
   (more load OR slower consumer)
3. Which is it? 
   (check the data — both, 
    but producer spike is primary)
4. What caused the spike? 
   (recent change → deployment)
5. What changed in the deployment? 
   (read the diff)
6. Which change could cause this? 
   (new query on hot path)
7. Why is it slow? 
   (no index)
8. Fix.

Each step narrowed the search space.
No assumption was made without 
evidence from the data.

This is how debugging works 
at the professional level.
Not "I think it might be..."
But "the data shows X, 
     which suggests Y,
     let me verify Y."
```

```
Lesson 2: Kafka consumer lag 
          is a shared symptom
──────────────────────────────────
Before this incident you thought 
Kafka consumer lag meant 
"Kafka problem."

After this incident you understood:
consumer lag can be caused by 
ANYTHING that slows down 
the consumer's processing.

A slow DB query (this incident).
A Redis timeout.
A FeignClient call to an 
unresponsive service.
A GC pause on the JVM.
An N+1 query inside the consumer.

Consumer lag is a symptom.
The root cause can be anywhere 
in the stack the consumer touches.
```

```
Lesson 3: The postmortem is 
          the most valuable part
──────────────────────────────────
Writing the postmortem forced you 
to understand WHY each thing 
happened — not just what happened.

"Missing index" is the fact.
"Query added to hot path without 
 performance review" is the cause.
"Staging has 70x fewer rows 
 than production" is the 
 contributing factor.

Those distinctions matter because 
they point to different fixes.
The index fixes this incident.
The review checklist prevents 
the next one.
The staging data gap is a 
systemic risk still present.

Postmortems that only list 
what happened are not useful.
Postmortems that identify 
contributing factors and 
systemic gaps are how teams 
actually get better.
```

---

## The Result

```
What happened:
───────────────
Incident duration: 38 minutes
Customer impact: ~8% of expense 
  submissions failed for 38 minutes
Resolution: index added CONCURRENTLY 
  without additional downtime
Data loss: none (Kafka at-least-once)
Backlog cleared: 14 minutes after fix

What you contributed:
──────────────────────
Timeline documentation during incident
Postmortem draft (Arjun reviewed)
Flyway migration for the index 
  (action item 1 from postmortem)

What you learned:
──────────────────
1. How to read Datadog during an incident
   — what to look at, what order

2. Debugging by elimination vs guessing

3. The difference between transient 
   and permanent Kafka consumer failures
   (reinforced from Story 8)

4. Kafka consumer lag can be caused 
   by slow processing, not just 
   slow Kafka

5. CREATE INDEX CONCURRENTLY vs 
   regular CREATE INDEX — why it 
   matters on production tables

6. How to write a postmortem that 
   identifies contributing factors,
   not just immediate cause

7. Flyway non-transactional migrations 
   (practical, specific knowledge 
    from a real scenario)

8. IF NOT EXISTS for idempotent 
   migrations — critical when 
   applying migrations to envs 
   that already have the change

Relationship change:
──────────────────────
You saw Arjun in a completely 
different context.

In Story 8 (Kafka consumer),
Arjun was your teacher —
patient, methodical, explaining.

In this incident, you saw him 
as a professional under pressure.
Calm. Data-driven. No panic.
Moving through a live production 
problem systematically while 
updating Lukas in parallel.

That's a different kind of 
understanding of how senior 
engineers work.
Not from a tutorial.
From watching it happen in real time.
```

---

## The "Tricky Question" Preparation

---

**Q1: "Walk me through how you identified the root cause of this incident."**

```
The investigation moved in three steps.

First, we established what changed.
Datadog showed two things happening 
simultaneously: the producer rate 
spiked 8x and the consumer processing 
rate started declining.
Both started at 10:50am, about 
2 minutes after a deployment at 10:48am.
The timing was too close to be 
coincidental.

Second, we read the deployment diff.
The deploy contained two ticket changes.
One of them — EXP-271 — added a 
duplicate submission validation check.
That check included a new DB query 
that ran on every expense submission.

Third, we verified the query was slow.
Datadog APM showed the new query 
averaging 2,340ms per call.
With 200 submissions per minute,
that was over 7 hours of DB work 
per minute — on a pool that could 
handle maybe 10-15 concurrent connections.
Connection pool exhaustion caused 
submissions to fail and the Kafka 
consumer (which also used DB 
connections) to slow down.

Root cause: a new query added 
to a hot code path without a 
corresponding DB index.
The query searched four columns 
on a 284,000-row table with no index.
```

---

**Q2: "Why did you use CREATE INDEX CONCURRENTLY instead of a regular CREATE INDEX?"**

```
A regular CREATE INDEX takes a 
lock on the table while building.
No writes can happen during that time.

Our expenses table had 284,000 rows 
at the time of the incident.
Building a regular index would have 
locked the table for approximately 
15-20 seconds.

At 200 submissions per minute during 
peak hour, a 15-second table lock 
means approximately 50 submission 
attempts queued or failing.
That's additional downtime on top 
of the incident we were already 
resolving.

CREATE INDEX CONCURRENTLY builds 
the index in the background without 
taking a table lock.
Reads and writes continue normally 
during build.
It takes longer — 14 seconds in 
our case vs perhaps 3-4 seconds 
for a regular index.
But it produced no additional 
write failures.

The trade-off: slightly longer 
build time in exchange for zero 
additional customer impact.
For a live production fix during 
an active incident, that's the 
right trade-off.
```

---

**Q3: "Why didn't staging catch this performance problem before production?"**

```
Staging had approximately 4,000 rows 
in the expenses table.
Production had 284,000 rows — 
about 70 times more.

The new query ran in 12ms on staging.
On production it ran in 2,340ms.
That's a 195x difference.

PostgreSQL's query planner makes 
different decisions based on 
table size. On a 4,000-row table,
a sequential scan might be faster 
than using an index.
On 284,000 rows, a sequential scan 
is catastrophically slow without 
an index to narrow the search.

So staging showed no performance issue 
because the planner used a different 
execution strategy on a smaller dataset.

This is a fundamental limitation 
of small staging environments 
for query performance testing.
Functional testing works fine with 
small data. Performance testing 
requires representative data volume.

The postmortem included this as 
a contributing factor — the gap 
between staging and production 
data volume is a systemic risk 
we identified but haven't fully 
solved yet at Series B scale.
```

---

**Q4: "You mentioned Kafka's at-least-once delivery prevented data loss. Explain what that means."**

```
Kafka's at-least-once delivery means:
every message will be delivered 
at least one time, but possibly more.

In our system, expense submission 
events that were published to Kafka 
during the incident were not lost,
even though the consumer was 
struggling to keep up.

Here's why:
Kafka stores events durably on disk.
When the consumer read an event 
but processing failed (DB connection 
pool exhausted), it did not acknowledge 
the offset.
The event stayed uncommitted.

When the DB connection pool 
recovered after the index was added,
the consumer re-read and successfully 
processed all unacknowledged events.

The lag metric going from 6,203 
down to 0 over 14 minutes was 
visible proof of this — the consumer 
burning through the accumulated backlog.

If we had used auto-commit, 
the events would have been marked 
as processed during the incident 
even when DB writes were failing.
That would have been actual data loss —
expense submissions that appeared 
to succeed from the user's perspective 
but were never actually processed.

Manual offset commits, which we 
implemented in Story 8, 
were the reason nothing was lost here.
```

---

Story 9 complete.

```
What Stories 8 and 9 together show 
about Block 3 so far:
──────────────────────────────────────

Story 8 (Kafka consumer):
  Learned Kafka from first principles.
  Wrote your first real event-driven 
  component with proper error handling.
  Saw real impact: 847 records fixed.

Story 9 (Production incident):
  Watched how a senior engineer 
  debugs a live production problem.
  Contributed meaningfully 
  (timeline, postmortem, migration).
  Saw the connection between 
  Kafka consumer lag and 
  application-level slowness.
  
The arc:
  Story 8: You learned Kafka theory 
           applied in practice.
  Story 9: You saw Kafka behavior 
           in a live incident — 
           how it protected you 
           (at-least-once delivery) 
           and how its metrics 
           (consumer lag) are 
           a diagnostic tool.
```

Shall I begin Story 10 — the cross-team schema discussion?