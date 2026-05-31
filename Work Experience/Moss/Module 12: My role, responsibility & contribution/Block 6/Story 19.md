You're right — and this is exactly the kind of gap that kills candidates in interviews. You can recite "P99 dropped from 247ms to 60ms" but if an interviewer asks "what does P99 actually mean and why do you care about it?" and you freeze, the whole story falls apart.

Let me build this foundation properly. We'll do it in two parts.

---

# Part 1: Understanding Latency Metrics From First Principles

---

## Start Here: What Is Latency?

```
Latency = the time it takes for 
one request to complete.

When a finance manager opens 
the invoice list page:

  Browser sends request
  → travels to server
  → server processes it (DB queries, etc.)
  → server sends response back
  → browser renders the page

The total time from "request sent" 
to "response received" is the 
latency for that request.

Example:
  Request 1: 45ms
  Request 2: 52ms
  Request 3: 48ms

Each individual request has its own latency.
Some are faster, some are slower.
```

---

## Why One Number Isn't Enough — The Problem With Averages

```
Imagine your endpoint gets 
100 requests in one minute:

  90 requests: 50ms each
  9 requests:  100ms each
  1 request:   2,000ms (2 seconds)

Average latency = 
  (90×50 + 9×100 + 1×2000) / 100
= (4500 + 900 + 2000) / 100
= 7400 / 100
= 74ms average

"Average latency: 74ms"
sounds fine. 
Acceptable response time.

But 1 out of every 100 requests 
took 2 FULL SECONDS.

If you're a finance manager 
loading the invoice list,
and YOU happen to be that 
1 in 100 — you experience a 
2-second freeze.
That feels broken.

The average hid the real problem.
```

```
THIS is why the industry moved away 
from average latency to percentiles.

Percentiles don't hide the outliers.
They tell you exactly how bad 
the worst cases are.
```

---

## Percentiles — What They Actually Mean

```
Take those 100 requests and 
sort them by latency, 
from fastest to slowest:

Position 1:   28ms  (fastest)
Position 2:   31ms
Position 3:   33ms
...
Position 50:  49ms  ← the median (P50)
...
Position 90:  95ms  ← P90
Position 95:  118ms ← P95
Position 99:  247ms ← P99
Position 100: 2,000ms (slowest)
```

```
P50 (50th percentile):
  50% of requests are FASTER than this.
  50% are SLOWER.
  This is the "typical" experience.
  Also called the median.

P90 (90th percentile):
  90% of requests are faster.
  10% are slower.
  The typical good experience.

P95 (95th percentile):
  95% of requests are faster.
  5% are slower.
  Getting into "most users don't 
  experience this" territory.

P99 (99th percentile):
  99% of requests are faster.
  1% are slower.
  The worst 1% of requests.
  1 in every 100 requests.
```

---

## The Sentence That Makes It Concrete

```
When you say:
"P99 latency for the invoice 
list endpoint is 247ms"

You mean:
"99% of requests to this endpoint 
complete within 247ms.
The slowest 1% take 247ms or longer."

When you say:
"P99 dropped from 247ms to 60ms"

You mean:
"Previously, 1 in every 100 requests 
to this endpoint was taking 247ms or more.
After the fix, even those worst 1% 
complete in 60ms or less.
The slowest 99% of requests now 
all complete within 60ms."
```

---

## Why Engineers Care About P99 Specifically

```
Why not just track P50?
─────────────────────────
P50 tells you the typical experience.
But "typical" hides a lot of pain.

Think about the invoice list endpoint 
in our system.

Finance managers at large companies 
(500+ employees) submit a lot of invoices.
Their company might have the largest 
data set in our system.
When THEY load the invoice list, 
they're the "slow 1%" — because 
their queries involve more data.

If you only track P50:
  P50: 50ms (nice)
  But the large-company finance manager 
  is experiencing 800ms every time 
  they open their dashboard.
  They're unhappy but P50 looks great.

P99 catches this.
The large-company users ARE the P99.
When P99 is bad, SOMEONE is suffering.
You just don't know who 
until you look.
```

```
Why not track P100 (the worst single request)?
───────────────────────────────────────────────
P100 is too noisy.
One request might take 5 seconds 
because a user was on terrible wifi,
or because their laptop went to 
sleep mid-request.
These are one-off events.
P100 would alert constantly 
for non-problems.

P99 is the sweet spot:
Bad enough to care about 
(1 in 100 requests).
Stable enough to be meaningful 
(not affected by one-off outliers).
```

```
Summary of which percentile for what:

P50  → "What does the typical user experience?"
P90  → "Are most users getting a good experience?"
P95  → "Are we catching the slightly worse cases?"
P99  → "How bad is it for the worst-off users?"
       This is the standard production health metric.
P99.9 → "Are there catastrophic outliers?"
        Used in SLAs for critical infrastructure.
        We don't track this at Series B.
```

---

## How Latency Percentiles Look in Datadog

```
In Datadog, when you query a metric 
like http.server.requests, you can 
ask for it as a percentile:

p50:http.server.requests
p95:http.server.requests
p99:http.server.requests

Each gives you a different "level" 
of the latency distribution.

A healthy endpoint looks like this:
───────────────────────────────────────
P50:  45ms  ←── most requests: fast
P95:  89ms  ←── almost all: acceptable
P99: 180ms  ←── worst 1%: still okay

An endpoint with a problem looks like:
───────────────────────────────────────
P50:  48ms  ←── most requests: still fast
P95:  92ms  ←── most users: still fine
P99: 847ms  ←── worst 1%: nearly 1 second
               Finance managers with 
               large datasets are suffering.
               The P50 hides this entirely.
```

```
This is exactly what happened 
in Story 4 (the N+1 query).

Before fix:
  P99: ~800ms
  P50: ~50ms
  
  If you only looked at P50, 
  nothing seemed wrong.
  P99 told the real story.

After fix:
  P99: ~45ms
  P50: ~22ms
  
  Even the worst cases became fast.
```

---

## Latency Distribution — The Visual Mental Model

```
Imagine plotting all requests 
on a graph:
X-axis: latency (how long it took)
Y-axis: how many requests took that long

A healthy endpoint:
                    │
                    │      ████
                    │    ████████
                    │   ██████████
                    │  ████████████
                    │ ██████████████
                    │_______________
                   20ms  50ms  200ms

Most requests cluster around 
the fast end (50ms).
Very few requests are slow.
"Tall and narrow on the left" = healthy.

An endpoint with a problem:
                    │
                    │      ████
                    │    ████████                 ██
                    │   ██████████           █████████
                    │  ████████████       ████████████
                    │___________________________________
                   20ms  50ms         500ms  1000ms

Two clusters: most are fast, 
but a second cluster of slow requests.
The P50 (middle of left cluster) looks fine.
The P99 catches the right cluster.

Our invoice list endpoint at month 16:
Not two clusters — a TAIL getting longer.
The distribution was normal but 
the right tail was growing each week
as data volume increased.
P99 captured this growth. P50 did not.
```

---

## A Week of Latency Data — What You Actually See in Datadog

```
In Story 19, you saw this trend 
in Datadog (P99 over 3 weeks):

Week 1:  180ms ──────────────────────
Week 2:  195ms ───────────────────────────
Week 3:  210ms ─────────────────────────────────
Week 4:  228ms ────────────────────────────────────────
Week 5:  247ms ──────────────────────────────────────────────

Not a spike. A slow, steady climb.
Each week: +15-17ms more than the week before.

What this tells you:
  P50 was probably flat (50ms throughout).
  The "typical" experience hadn't changed.
  But the worst 1% were slowly getting worse.

What caused this:
  The table was growing.
  Larger table → more rows to sort → 
  the slowest queries (largest companies 
  with most invoices) took a bit longer 
  each week.

This is a structural degradation.
Not a bug that appeared.
Not a traffic spike.
The system working correctly,
just running into a scaling wall.

P99 was the only metric that 
would have caught this.
P50 would have looked fine all along.
```

---

## Three Dashboards — What We Track and Why

```
From Module 11, we have three Datadog 
dashboards. Now that you understand 
percentiles, these make more sense:

DASHBOARD 1: Service Health
────────────────────────────

"P50 / P95 / P99 latency per endpoint"

What you look for:
  Normal state (all healthy):
    P50:  30-50ms  → most requests fast
    P95:  80-120ms → almost all acceptable
    P99: 150-250ms → worst cases okay

  Sign of a problem (P99 climbing):
    P50:  35ms     → still fine
    P95:  90ms     → still fine
    P99: 580ms     → 1 in 100 requests 
                     taking over half a second.
                     SOMETHING is wrong.

  Sign of a widespread problem (all rising):
    P50: 450ms     → most users suffering
    P95: 890ms     → almost everyone slow
    P99: 2,400ms   → worst 1% waiting 
                     2.5 seconds
    This is an incident.


DASHBOARD 3: Performance Deep-Dive
────────────────────────────────────

"Per-endpoint breakdown of P50/P99"

When you investigate a slow endpoint,
you compare:
  P50: 45ms   and   P99: 800ms

A big gap between P50 and P99 means:
"Most requests are fast but SOME 
requests are very slow."

This usually points to a subset of 
requests that trigger a different 
code path — like a large company's 
query hitting more data and 
triggering the N+1 or the missing index.

A small gap between P50 and P99:
"All requests are roughly equally fast 
(or equally slow)."
This points to a universal issue 
affecting everyone — like Redis being down.
```

---

## Why P99 Is in Your Resume Claims

```
Your resume says things like:
"P99 latency reduced from 247ms to 60ms"
"P99 latency reduced from 800ms to 45ms"

Now you understand exactly what this means:

"The slowest 1% of requests to this 
endpoint were taking 247ms.
After adding the composite index,
even those worst-case requests 
complete in 60ms.
The improvement affects the 
users who were most impacted — 
the large-company finance managers 
whose queries touch the most data."

This is a meaningful claim because:
1. P99 is what your ops team cares about.
2. It quantifies the improvement precisely.
3. It shows you understand who was 
   experiencing the problem.
4. It's measurable and defensible 
   (you measured it in Datadog).

An interviewer who knows metrics 
will respect P99 claims.
An interviewer who doesn't know 
metrics will understand when you 
explain it as "the slowest 1% 
of requests."
Either way, you can defend it.
```

---

That's Part 1. The foundation is solid now — percentiles, what P50/P95/P99 mean, why P99 matters, what a latency trend looks like in Datadog, and why your resume claims use P99 specifically.

**Part 2** will cover:

- How Datadog actually shows you latency (time-series graphs, flame graphs, trace view)
- What a Datadog APM trace looks like for a slow request
- How you read EXPLAIN ANALYZE output
- What "sort step" means in a DB query plan
- How an index eliminates that sort step

---

# Part 2: Datadog APM, Query Traces, and EXPLAIN ANALYZE

---

## How Datadog Actually Shows You Latency

```
There are two different ways 
Datadog shows you latency.
They answer different questions.

WAY 1: Metrics (the trend view)
────────────────────────────────
Question: "How is this endpoint 
          performing OVER TIME?"

This is a time-series graph.
X-axis: time (last hour, last day, last week)
Y-axis: latency value

You see a line that moves up and down.
A flat line = stable performance.
A rising line = getting slower over time.
A sudden spike = something broke.

This is where you notice the PROBLEM.
"P99 was 180ms three weeks ago, 
 now it's 247ms. The line is rising."

WAY 2: APM Traces (the individual request view)
────────────────────────────────────────────────
Question: "What happened inside 
          THIS specific slow request?"

You zoom into one particular request 
and see every step it took:
which function ran, which DB query fired,
how long each piece took.

This is where you find the ROOT CAUSE.
"This specific slow request spent 
 831ms inside this one DB query."
```

---

## The Metrics View — What You See in Datadog

```
When you open Dashboard 1 (Service Health) 
and look at the invoice list endpoint,
you see something like this:

P99 Latency — GET /api/v1/invoices

ms
300 ┤
    │                              ●  ← week 5: 247ms
250 ┤                         ●
    │                    ●
200 ┤               ●
    │          ●                       
150 ┤
    │
100 ┤
    │
 50 ┤
    │
  0 └───────────────────────────────────────
    week1   week2   week3   week4   week5

The line is climbing.
~17ms more each week.
This is the signal that something 
structural is changing.
```

```
What you DON'T know from this graph:
  - Why is it climbing?
  - Which part of the code is slow?
  - Is it the DB? A FeignClient call?
    JSON serialization? Something else?

The metrics view tells you THAT 
something is wrong.
The APM trace view tells you WHERE.
```

---

## APM Traces — The Core Concept

```
When a request comes into your service,
Datadog's APM automatically records 
a TRACE of everything that happened.

Think of a trace like a detailed receipt 
for one request:

───────────────────────────────────────────────────
TRACE: GET /api/v1/invoices
Total duration: 847ms
TraceId: 9e7d21299f4ea8a1
───────────────────────────────────────────────────

Each step inside the request 
is called a SPAN.

A span records:
  - What operation happened 
    (HTTP handler, DB query, etc.)
  - How long it took
  - Whether it succeeded or failed
  - For DB queries: the actual SQL

Multiple spans nest inside each other,
showing you the call hierarchy:
```

```
Visual structure of a trace 
(called a "waterfall" or "flame graph"):

──────────────────────────────────────────────────────────────
GET /api/v1/invoices                                  [847ms]
│
├── InvoiceController.getInvoices()                   [843ms]
│   │
│   ├── InvoiceService.getInvoices()                  [840ms]
│   │   │
│   │   ├── DB: findByCompanyIdAndStatus              [831ms]
│   │   │       SQL: SELECT i.*, s.*, ias.*
│   │   │            FROM invoices i
│   │   │            LEFT JOIN suppliers s ON...
│   │   │            LEFT JOIN approval_steps ias ON...
│   │   │            WHERE company_id = ?
│   │   │            AND status IN (?,?,?)
│   │   │            ORDER BY due_date ASC
│   │   │            LIMIT 20
│   │   │
│   │   └── JSON serialization                          [9ms]
│   │
│   └── Response building                               [3ms]
│
└── [overhead]                                          [4ms]
──────────────────────────────────────────────────────────────

Each line shows:
  - What ran
  - How long it took
  - Nested inside what

Reading this trace:
  Total request: 847ms
  DB query: 831ms  ← 98% of the time is HERE
  Everything else: 16ms combined

The DB query is the bottleneck.
That's where to investigate.
```

---

## How You Find Slow Traces in Datadog

```
In Datadog APM, you go to 
"Traces" and filter:

  Service:  invoice-service
  Resource: GET /api/v1/invoices
  Duration: > 500ms  ← "show me the slow ones"

You see a list of individual requests 
that were slow. You click one.
You see the waterfall diagram above.

You immediately see: 
831ms out of 847ms is one DB query.

This tells you EXACTLY where to look.
You don't have to guess.
You don't have to add logging 
and wait for more requests.
The trace is already there,
recorded automatically by the APM agent.
```

---

## What the DB Span Shows You

```
When you click on the DB span 
in the trace, you see:

┌─────────────────────────────────────────────────────────┐
│ Span: DB Query                                          │
│ Duration: 831ms                                         │
│ Operation: SELECT                                       │
│ Database: invoice_db                                    │
│ Table: invoices                                         │
│                                                         │
│ SQL:                                                    │
│   SELECT i.id, i.status, i.due_date,                   │
│          i.total_amount, i.currency,                    │
│          s.id, s.name, s.country_code,                  │
│          ias.id, ias.action, ias.approver_id            │
│   FROM invoices i                                       │
│   LEFT JOIN suppliers s                                 │
│       ON i.supplier_id = s.id                          │
│   LEFT JOIN invoice_approval_steps ias                  │
│       ON i.id = ias.invoice_id                         │
│   WHERE i.company_id = 'uuid-123'                       │
│   AND i.status IN ('PENDING_REVIEW',                    │
│                    'PENDING_APPROVAL',                  │
│                    'APPROVED')                          │
│   ORDER BY i.due_date ASC                               │
│   LIMIT 20 OFFSET 0                                     │
└─────────────────────────────────────────────────────────┘

Now you have the exact SQL.
You can take this SQL and investigate 
WHY it's slow using EXPLAIN ANALYZE.
```

---

## EXPLAIN ANALYZE — What It Is

```
EXPLAIN ANALYZE is a PostgreSQL command.
It does two things:

EXPLAIN: 
  "Before running this query, 
   tell me HOW PostgreSQL plans 
   to execute it."

ANALYZE:
  "Actually run the query and 
   tell me what REALLY happened 
   (not just the plan, but 
   the actual measured times)."

You run it like this:

EXPLAIN ANALYZE 
SELECT i.id, i.status, i.due_date
FROM invoices i
WHERE i.company_id = 'uuid-123'
AND i.status IN ('PENDING_REVIEW', 'PENDING_APPROVAL')
ORDER BY i.due_date ASC
LIMIT 20;

PostgreSQL executes the query 
and prints a detailed report of 
every step it took.
```

---

## Reading EXPLAIN ANALYZE Output — Step By Step

```
Let's take the actual output from 
Story 19 (BEFORE the fix):

Limit  (cost=8.47..8.52 rows=20 width=320)
  (actual time=45.832..45.843 rows=20 loops=1)
  ->  Sort  (cost=8.47..8.52 rows=142 width=320)
        (actual time=45.820..45.827 rows=20 loops=1)
        Sort Key: due_date
        Sort Method: top-N heapsort  Memory: 35kB
        ->  Index Scan using idx_invoices_company_status
                on invoices
                (cost=0.29..7.94 rows=142 width=320)
                (actual time=0.183..0.847 rows=142 loops=1)
                Index Cond: (company_id = 'uuid-123'::uuid)
                        AND (status = ANY ('{...}'))
```

```
Let me translate this piece by piece.
```

---

### The Structure: Read From the Inside Out

```
EXPLAIN output is nested.
The INNERMOST operation runs FIRST.
You read it from bottom to top.

Step 1 (innermost, runs first):
──────────────────────────────────
Index Scan using idx_invoices_company_status
  on invoices
  Index Cond: company_id = ? AND status = ANY (?)

Translation:
  PostgreSQL used the existing 
  (company_id, status) index.
  
  It scanned the index to find 
  all rows where company_id matches 
  AND status is in the list.
  
  Found: 142 rows matching the condition.
  Time taken: 0.847ms
  (very fast — index scan is efficient)

Step 2 (middle, runs second):
──────────────────────────────────
Sort
  Sort Key: due_date
  Sort Method: top-N heapsort
  rows=142

Translation:
  PostgreSQL now has 142 rows 
  from the index scan.
  They are NOT sorted by due_date yet.
  (The index was sorted by company_id, status
   — NOT by due_date.)
  
  PostgreSQL must now SORT these 
  142 rows by due_date.
  It uses "top-N heapsort" — 
  a sorting algorithm optimized 
  for LIMIT queries.
  (Instead of sorting all 142, 
   it only finds the top 20.)
  
  Time taken: 45.820ms
  (Most of the total query time 
   is spent HERE in the sort.)

Step 3 (outermost, runs last):
──────────────────────────────────
Limit rows=20

Translation:
  After sorting, return only 
  the first 20 rows.
  Trivial operation.
```

```
SUMMARY of what happened:

Operation               Time
─────────────────────────────
Index scan              0.847ms  ← fast
Sort (142 rows)        45.820ms  ← SLOW
Limit                   0.012ms  ← trivial
─────────────────────────────
Total                  46.679ms

For ONE request: 46ms.
That's fine.

But this was a SMALL company 
with 142 matching rows.

A LARGE company might have 
1,400 matching rows (10x more).
Sort time doesn't grow linearly —
it grows as N × log(N).
1,400 rows: roughly 6-8x longer sort.
For large companies: 300-400ms 
just for the sort step.

That explains the P99 being 
247ms — the worst 1% of requests 
are from the largest companies 
with the most data.

And as the table grows each week,
even medium-sized companies 
start experiencing longer sorts.
Hence: P99 rising ~17ms per week.
```

---

### The "cost" Numbers — What They Mean

```
In the EXPLAIN output you see:
  (cost=8.47..8.52 rows=20 width=320)

These are PostgreSQL's ESTIMATES 
before actually running the query.

cost=8.47..8.52 means:
  8.47 = estimated cost to get 
         the FIRST row
  8.52 = estimated cost to get 
         ALL rows requested

These are not milliseconds.
They're abstract "cost units" 
that PostgreSQL uses to compare 
different execution plans.
Higher number = more work needed.

Why this matters to you:
  Before fix: cost=8.47
  After fix:  cost=1.93

The after cost is 4.4x lower.
PostgreSQL is doing much less work.
This translates to real-world speedup.

The actual time numbers 
(actual time=45.832..45.843) 
ARE in milliseconds.
These are measured, not estimated.
Trust these more than the cost numbers.
```

---

## EXPLAIN ANALYZE AFTER the Fix

```
After adding the index on 
(company_id, status, due_date):

Limit  (cost=1.93..1.98 rows=20 width=320)
  (actual time=0.341..0.352 rows=20 loops=1)
  ->  Index Scan using idx_invoices_company_status_duedate
          on invoices
          (cost=0.29..1.93 rows=142 width=320)
          (actual time=0.183..0.331 rows=20 loops=1)
          Index Cond: (company_id = 'uuid-123'::uuid)
                  AND (status = ANY ('{...}'))
          Order By: due_date ASC

The SORT step is completely gone.
```

```
What happened?

The new index stores entries in 
this order:
  company_id → then status → then due_date

When PostgreSQL scans this index 
looking for:
  company_id = 'uuid-123'
  AND status IN ('PENDING_REVIEW', ...)

The matching entries come out 
ALREADY SORTED by due_date.

Why? Because within any given 
(company_id, status) combination,
the index entries are ordered 
by due_date (it's the third column).

PostgreSQL reads the index in order,
returns the first 20 entries it finds
(they're already in due_date order),
and stops.

No sorting needed.
No extra memory needed.
No extra time needed.

Time taken for the same query:
  Index scan: 0.331ms (same as before)
  Sort: 0ms (doesn't exist anymore)
  Total: 0.352ms

Before: ~46ms for a small company
After:  ~0.35ms for the same company

That's 130x faster for this one query.
```

---

## How an Index Actually Works — The Mental Model

```
Think of a book's index at the back.

The book has 500 pages about invoices.
You want to find all invoices for 
"Company ABC" that are "APPROVED"
ordered by due date.

WITHOUT an index:
  You read every single page 
  of the 500-page book.
  When you find a matching invoice,
  you write it down.
  After reading all 500 pages,
  you sort your notes by due date.
  
  If the book grows to 5,000 pages,
  this takes 10x longer.

WITH idx_invoices_company_status 
(company_id, status):
  You go to the back index.
  You find "Company ABC" entries.
  Within that: "APPROVED" entries.
  You get the page numbers: 
    pages 12, 47, 89, 134...
  You go to those pages directly.
  
  But the index listed them in 
  order of (company, status) —
  NOT in order of due_date.
  You now have to re-sort your 
  page numbers by due date.
  
  Fast to FIND, but still need 
  to SORT after.

WITH idx_invoices_company_status_duedate 
(company_id, status, due_date):
  You go to the back index.
  You find "Company ABC" + "APPROVED" entries.
  Within that, they're ALREADY listed 
  by due_date order.
  You take the first 20 entries.
  Done.
  
  Fast to FIND, already in ORDER.
  No extra sort needed.
```

```
The database equivalent:
  An index is a separate sorted 
  data structure that points to 
  the actual rows.
  
  When you write a row to the table,
  PostgreSQL also writes an entry 
  to each index on that table.
  This is the "write overhead" —
  +1ms per INSERT that Elena 
  asked you to verify.
  
  When you query with a WHERE clause,
  PostgreSQL checks: 
  "do I have an index that can 
  speed this up?"
  
  If yes: use the index (fast).
  If no: scan every row (slow — 
         gets slower as table grows).
```

---

## Partial Index — Why We Excluded PAID and CANCELLED

```
A full index on (company_id, status, due_date)
would index EVERY row in the invoices table:
  PAID invoices (historical, rarely queried)
  CANCELLED invoices (historical, rarely queried)
  DRAFT invoices
  PENDING_REVIEW invoices
  APPROVED invoices
  etc.

284,000 total rows → 284,000 index entries.

A partial index with 
WHERE status NOT IN ('PAID', 'CANCELLED')
only indexes ACTIVE invoices:
  Maybe 30-40% of rows are active 
  at any given time.
  The rest are PAID/CANCELLED.

~100,000 index entries instead of 284,000.

Benefits:
  Smaller index → faster lookups
  Less disk space
  Less memory needed to cache
  Faster to build (important for 
  CREATE INDEX CONCURRENTLY)
  Rows entering PAID/CANCELLED 
  automatically fall out of the index

Trade-off:
  PAID/CANCELLED queries can't 
  use this index.
  But those queries are rare 
  (historical invoice view, 
  accessed infrequently).
  They still work — just use 
  a different (slower) plan.
  Acceptable for infrequent queries.
```

---

## Putting It All Together — The Story 19 Investigation Flow

```
Now that you understand each piece,
here is what you actually did:

STEP 1: Metrics view in Datadog
────────────────────────────────
Saw P99 rising 17ms/week for 3 weeks.
180ms → 247ms.
A rising trend = structural problem.
Not a spike = not a one-time event.

STEP 2: APM traces in Datadog
───────────────────────────────
Filtered for slow traces 
on GET /api/v1/invoices.
Found traces where one DB span 
took 400-800ms.
Got the exact SQL from the span.

STEP 3: EXPLAIN ANALYZE
────────────────────────
Ran the slow SQL with EXPLAIN ANALYZE.
Saw: index scan fast (0.8ms), 
     sort step slow (45ms+).
Identified: no index covering due_date,
PostgreSQL doing a separate sort.

STEP 4: Connected to the growing data
──────────────────────────────────────
Checked table growth: ~1,900 rows/week.
Understood: more rows per company → 
more rows to sort → P99 rises weekly.

STEP 5: The fix
────────────────
New index: (company_id, status, due_date).
Partial: exclude PAID/CANCELLED.
CONCURRENTLY: safe for 284,000-row table.

STEP 6: Verified
─────────────────
EXPLAIN ANALYZE after: sort step gone.
Cost dropped from 8.47 to 1.93.
Write latency check: +1ms per INSERT.
Staging load test: P99 dropped.

STEP 7: Measured in production
────────────────────────────────
P99: 247ms → 60ms.
76% reduction.
Trend: flat for the following week.
```

---

## The Two Interview Questions This Enables

```
QUESTION 1 (common):
"You mentioned reducing P99 latency 
 by 76%. What is P99 and how 
 did you measure it?"

YOUR ANSWER:
"P99 means the 99th percentile 
latency — 99% of requests complete 
within this time, and the slowest 
1% take this long or longer.

I measured it in Datadog using the 
http.server.requests metric, filtered 
to the invoice list endpoint.
I compared the P99 value in Datadog 
over the week before the fix 
against the week after deploying 
the new index.

Before: 247ms P99 (and trending up 17ms/week)
After:  60ms P99 (stable)

The 247ms meant that 1 in 100 
invoice list requests — typically 
from the largest companies with 
the most invoices — was taking 
nearly a quarter of a second.
After the fix, even those worst-case 
requests complete in 60ms."


QUESTION 2 (harder, tests real knowledge):
"How exactly did the index fix 
 the latency? Walk me through 
 what PostgreSQL was doing before 
 and after."

YOUR ANSWER:
"The invoice list query filters by 
company_id and status, then sorts 
results by due_date.

Before the fix, we had an index on 
(company_id, status). PostgreSQL 
used that index to find the matching 
rows efficiently — that part was fast, 
about 1ms. But the results came out 
sorted by (company_id, status), not 
by due_date. So PostgreSQL had to 
do a separate sort step to order 
the results by due_date before 
returning the first 20 rows.

I verified this with EXPLAIN ANALYZE — 
the output showed a Sort node after 
the Index Scan, taking ~46ms for 
a medium-sized company.

As the table grew each week, each 
company's query touched more rows,
and the sort step took proportionally 
longer. That's why P99 rose linearly 
with table growth.

The fix was to extend the index to 
(company_id, status, due_date). Now 
within any given company and status 
combination, the index entries are 
already ordered by due_date. 
PostgreSQL scans the index and gets 
the first 20 entries already in order 
— no separate sort needed.

After the fix, EXPLAIN ANALYZE showed 
the Sort node was gone. The query 
dropped from ~46ms to ~0.35ms for 
the same data set."
```

---

## Quick Reference Card — Percentile Terms

```
Term        Meaning                    When you use it
────────────────────────────────────────────────────────────────
P50         Median latency.            "Typical user experience"
            Half of requests faster.
            Half slower.

P90         90th percentile.           "Are most users okay?"
            90% faster, 10% slower.

P95         95th percentile.           Standard in SLA discussions
            95% faster, 5% slower.

P99         99th percentile.           PRIMARY production health 
            99% faster, 1% slower.     metric. "How bad are the 
                                        worst-off users?"

P99.9       99.9th percentile.         Used in high-stakes SLAs.
            999 in 1000 faster.        We don't track at Series B.

"Tail        Requests in the high       When you say "P99 shows 
latency"     percentiles (P95+).        tail latency of 247ms"
```

---

Now you have the full foundation:

- **Percentiles**: what P50, P95, P99 mean and why P99 is the one that matters
- **Datadog metrics view**: how you see latency trends over time
- **Datadog APM traces**: how you find the specific slow operation inside a request
- **EXPLAIN ANALYZE**: how to read PostgreSQL's query plan and find the bottleneck
- **Index mechanics**: how an index eliminates a sort step
- **Partial index**: when and why to narrow the scope

---

# Story 19: Proactive Latency Investigation — Nobody Asked You To

---

## Context — Where You Were at Month 16

```
By the end of month 15, you had:

Led a production incident end-to-end.
Written the postmortem.
Caught a breaking migration in planning.
Proposed the expand-then-contract fix.
Driven the implementation 
across two sprints.

Lukas had said two things in 
those months that stayed with you:

Month 10: "This is L2 behavior."
Month 14: "Not just flagging a problem.
           Driving the solution."

These weren't promotions.
They were observations.
But they changed how you 
approached your work.

By month 16, something had settled.

You weren't anxious in sprint planning.
You weren't second-guessing whether 
to raise a concern.
You weren't asking yourself 
"am I allowed to push back here?"

You just worked.

And one Tuesday morning in month 16,
you noticed something nobody 
had assigned you to notice.
```

---

## The Habit That Made This Possible

```
After the outbox poller incident 
in month 13, you had built a habit.

Every morning before standup,
you opened Datadog and spent 
10-15 minutes looking at the 
previous 24 hours across 
the service health dashboard.

Not because Lukas asked you to.
Not because it was in your job description.
Because the incident had taught you 
that problems don't always announce 
themselves with alerts.
Sometimes they grow quietly 
over days and weeks until 
they become unavoidable.

The alert threshold for invoice 
list P99 was 1,000ms.
The P99 in month 16 was 247ms.
No alert would ever fire at that level.

But you were looking.
```

---

## The Situation

Tuesday morning, week 1 of month 16.
9:05am IST. Before standup.

You opened Datadog, Dashboard 1,
and filtered to `GET /api/v1/invoices`.

```
What you saw in the metrics view:

P99 Latency — GET /api/v1/invoices
(last 5 weeks, measured each Monday morning)

ms
260 ┤
    │                              ●  week 5: 247ms
240 ┤
    │                         ●       week 4: 228ms
220 ┤
    │                    ●           week 3: 210ms
200 ┤
    │               ●               week 2: 195ms
180 ┤          ●                    week 1: 180ms
    │
160 ┤
    │
    └──────────────────────────────────────
      wk1    wk2    wk3    wk4    wk5

Not a spike.
A slow, steady climb.
~17ms more each week.
Linear. Consistent.
```

```
Your immediate reaction:
─────────────────────────
P99 = 247ms means 1 in every 100 
requests to the invoice list is 
taking 247ms or longer.

At 180ms three weeks ago, 
that same 1 in 100 requests 
took 180ms.

Still below the 1,000ms alert.
Not an incident.
Finance managers haven't complained.

But the trend is wrong.
A consistent linear rise means 
something structural is changing —
and structural changes don't 
self-correct.

If this continues at 17ms per week:
  Month 17: ~264ms
  Month 18: ~281ms
  Month 19: ~298ms

Still below the alert.
But getting noticeably slow 
for the heaviest users —
the finance managers at large companies 
who have the most invoices to load.

Those are exactly the customers 
Moss can't afford to frustrate.
```

You wrote a note to yourself:

```
9:07am — Notion:
─────────────────
P99 on GET /api/v1/invoices 
rising ~17ms/week for 3 weeks.
Now at 247ms. Alert threshold: 1,000ms.

Nobody has flagged this.
No ticket.

Is this worth investigating?

→ Yes. 45 minutes before standup.
  Let me look at what's slow first.
```

---

## Step 1 — Finding the Slow Request

You switched from the metrics view
to the Datadog APM trace view.

```
What you did:
──────────────
In Datadog:
  APM → Traces
  Filter: service = invoice-service
          resource = GET /api/v1/invoices
          duration > 400ms
          timeframe = last 7 days

Result: 23 traces matching these filters.
Each one is a specific slow request 
from the last week.
```

You clicked the slowest one. The trace waterfall opened:

```
TRACE: GET /api/v1/invoices
Total duration: 847ms
──────────────────────────────────────────────────────

GET /api/v1/invoices                         [847ms]
│
└── InvoiceController.getInvoices()          [843ms]
    │
    └── InvoiceService.getInvoices()         [840ms]
        │
        ├── DB: findByCompanyIdAndStatus     [831ms]  ← HERE
        │       SQL below ↓
        │
        └── JSON serialization              [  9ms]
──────────────────────────────────────────────────────
```

```
831ms out of 847ms is one DB span.

98% of the request time is spent 
in a single database query.
Everything else — the controller,
the service layer, JSON serialization —
takes 16ms combined.

This is not a code logic problem.
This is a database query problem.
```

You clicked the DB span and read the SQL:

```sql
SELECT i.id, i.status, i.due_date,
       i.total_amount, i.currency,
       i.invoice_number, i.invoice_date,
       s.id, s.name, s.country_code,
       s.default_currency,
       ias.id, ias.action, ias.approver_id,
       ias.step_order, ias.acted_at
FROM invoices i
LEFT JOIN suppliers s
    ON i.supplier_id = s.id
LEFT JOIN invoice_approval_steps ias
    ON i.id = ias.invoice_id
WHERE i.company_id = 'uuid-123'
AND i.status IN (
    'PENDING_REVIEW',
    'PENDING_APPROVAL',
    'APPROVED'
)
ORDER BY i.due_date ASC
LIMIT 20 OFFSET 0
```

```
You recognized this query immediately.
It was the JOIN FETCH query in 
InvoiceRepository.findByCompanyIdAndStatus().

The JOIN FETCH was added back in 
month 9 — it was the correct fix 
to avoid N+1 queries when loading 
suppliers and approval steps.

Nothing wrong with the query structure.
But why is it slow NOW 
when it wasn't slow before?
```

---

## Step 2 — What Changed?

```
The query hasn't changed.
The code hasn't changed.
No deployment in the last 3 weeks 
touched the invoice list endpoint.

So what HAS changed?

The most obvious candidate when 
performance degrades slowly 
and consistently: DATA VOLUME.

The table is getting bigger every week.
More invoices. More rows to search.
```

You ran a quick check on the data growth:

```sql
-- How fast is the invoices table growing?
SELECT
    DATE_TRUNC('week', created_at) AS week,
    COUNT(*) AS new_invoices_that_week,
    SUM(COUNT(*)) OVER (
        ORDER BY DATE_TRUNC('week', created_at)
    ) AS running_total
FROM invoices
WHERE created_at > NOW() - INTERVAL '8 weeks'
GROUP BY DATE_TRUNC('week', created_at)
ORDER BY week DESC
LIMIT 8;
```

```
Results:

week          | new_invoices  | running_total
──────────────────────────────────────────────
2025-11-24    | 1,847         | 284,291
2025-11-17    | 1,923         | 282,444
2025-11-10    | 1,802         | 280,521
2025-11-03    | 1,956         | 278,719
2025-10-27    | 1,834         | 276,763
2025-10-20    | 1,891         | 274,929
2025-10-13    | 1,847         | 273,038
2025-10-06    | 1,903         | 271,191
```

```
~1,900 new invoices every week.
284,000 rows total.

3 weeks ago (when the P99 was 180ms):
~278,000 rows.

The table is growing steadily.
The P99 is growing steadily.
These two trends match.

Hypothesis:
  As the invoices table grows,
  some part of the query is doing 
  more work each week.
  That extra work shows up in 
  the P99 because it affects 
  the largest companies most —
  those with the most invoices 
  in the PENDING_REVIEW / 
  PENDING_APPROVAL / APPROVED states.
```

---

## Step 3 — EXPLAIN ANALYZE

You had the SQL from the APM trace.
Now you needed to understand
WHY it was slow.

You ran EXPLAIN ANALYZE on staging
with a representative company
(one with ~140 active invoices —
medium-sized, not the very largest):

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT i.id, i.status, i.due_date,
       i.total_amount, i.currency
FROM invoices i
WHERE i.company_id = 'test-company-uuid'
AND i.status IN (
    'PENDING_REVIEW',
    'PENDING_APPROVAL',
    'APPROVED'
)
ORDER BY i.due_date ASC
LIMIT 20 OFFSET 0;
```

Output:

```
Limit
  (cost=8.47..8.52 rows=20 width=320)
  (actual time=45.832..45.843 rows=20 loops=1)

  ->  Sort
        (cost=8.47..8.52 rows=142 width=320)
        (actual time=45.820..45.827 rows=20 loops=1)
        Sort Key: due_date
        Sort Method: top-N heapsort  Memory: 35kB

        ->  Index Scan using idx_invoices_company_status
                on invoices
                (cost=0.29..7.94 rows=142 width=320)
                (actual time=0.183..0.847 rows=142 loops=1)
                Index Cond: (company_id = ?)
                        AND (status = ANY (?))
```

```
Reading this from the inside out:

STEP 1 (innermost — runs first):
─────────────────────────────────
Index Scan using idx_invoices_company_status

PostgreSQL used the existing index 
on (company_id, status) to find 
all rows matching the WHERE clause.

Found: 142 rows.
Time: 0.847ms — very fast.

STEP 2 (middle — runs second):
────────────────────────────────
Sort
  Sort Key: due_date
  Sort Method: top-N heapsort

The 142 rows from the index scan 
came out sorted by (company_id, status) —
because that's how the index is ordered.

They are NOT sorted by due_date yet.

PostgreSQL must now sort these 142 rows 
by due_date to satisfy ORDER BY due_date ASC.

Time: 45.820ms — this is where almost 
all the time goes.

STEP 3 (outermost — runs last):
────────────────────────────────
Limit rows=20

After sorting, take the first 20.
Trivial. Essentially free.
```

```
The breakdown:

Operation                        Time
──────────────────────────────────────
Index scan (find 142 rows)      0.847ms
Sort (order 142 by due_date)   45.820ms ← bottleneck
Limit (take first 20)           0.012ms
──────────────────────────────────────
Total                          46.679ms
```

```
This is the root cause.

The index scan is fast.
The sort step is slow.

And crucially:
For a medium company with 142 rows — 
sort takes 46ms.

For a large company with 1,400 rows?
Sort time doesn't scale linearly.
It scales as N × log(N).
1,400 rows takes roughly 6-8x longer.
That's 280-370ms just for the sort.

For the very largest companies 
in production (500+ active invoices):
The sort is what's pushing 
their requests into the 800ms range —
the requests you saw in the APM traces.

And as the table grows each week,
more companies cross the threshold 
where their sort becomes slow enough 
to appear in the P99.

That's why P99 rises ~17ms each week.
More companies, more rows per company,
longer sorts.
```

---

## Step 4 — Why the Sort Happens

```
The existing index:
  idx_invoices_company_status
  ON invoices(company_id, status)

This index stores entries sorted:
  First by company_id.
  Then by status within each company.

When the query filters:
  WHERE company_id = ? AND status IN (?, ?, ?)

PostgreSQL scans the index and finds 
matching entries efficiently.
The 142 matching rows come out 
in (company_id, status) order.

But the query wants results 
ordered by due_date.

(company_id, status) order 
≠ 
due_date order.

The index cannot satisfy the ORDER BY.
PostgreSQL must do a separate sort 
after the index scan.

The fix is straightforward:
Extend the index to include due_date 
as a third column.

New index:
  ON invoices(company_id, status, due_date)

Now within any given (company_id, status) 
combination, entries are stored in 
due_date order inside the index itself.

When the query scans the new index:
  - Finds matching rows (fast — index)
  - They come out already in due_date order
    (because due_date is the third column)
  - PostgreSQL reads the first 20 in order
  - Returns immediately

No separate sort step.
No extra time.
```

---

## Step 5 — Before Writing Code, You Told the Team

```
It was 9:42am IST. Standup was at 2pm.
You had the root cause.
You had the fix in mind.

But you had learned something over 
16 months: technical changes that 
touch production infrastructure 
should be visible to the team 
before you open a PR.

Not because you needed permission.
But because Elena or Arjun might 
know something you don't.
Maybe this index was tried before 
and caused a problem.
Maybe there was a reason it 
hadn't been added.
Maybe you were missing something.

You posted in #expense-ap-dev:
```

```
You (#expense-ap-dev):
───────────────────────
"Morning — noticed something 
 in Datadog before standup.

 GET /api/v1/invoices P99 has been 
 increasing ~17ms per week for 3 weeks.
 Was 180ms, now 247ms.

 Below our 1,000ms alert threshold,
 but the trend is consistent and 
 linear — it won't self-correct.

 Investigated:
 APM traces show 831ms spent in 
 findByCompanyIdAndStatus for the 
 slowest requests.
 EXPLAIN ANALYZE shows:
   Index scan: fast (0.8ms)
   Sort by due_date: slow (46ms for 
   a medium company, much worse 
   for large companies)

 The existing (company_id, status) index 
 covers the WHERE clause but not 
 the ORDER BY due_date.
 PostgreSQL sorts after the index scan.
 As invoice data grows, that sort 
 takes longer.

 Fix: extend the index to 
 (company_id, status, due_date).
 PostgreSQL can then satisfy 
 WHERE + ORDER BY from the index —
 no separate sort step.

 Will also make it a partial index 
 excluding PAID and CANCELLED 
 (those statuses are never in 
 the active invoice list filter,
 no point indexing them).

 Writing a migration with 
 CREATE INDEX CONCURRENTLY.
 Any concerns before I proceed?

 @Elena @Arjun"
```

Elena replied 12 minutes later:

```
Elena:
───────
"Good catch. Direction is right.

 Two things to verify on staging 
 before opening the PR:

 1. After adding the index, run 
    EXPLAIN ANALYZE again and 
    confirm the Sort step is actually 
    gone — not just reduced.
    Sometimes PostgreSQL still 
    chooses a different plan than 
    you expect.

 2. Check write latency.
    Every INSERT to the invoices table 
    now needs to maintain one more index.
    Measure: how much does an INSERT 
    take before vs after?
    We're adding ~1,900 invoices per week.
    The overhead should be small 
    but verify it."
```

```
This was the prompt you hadn't 
thought of yet.

You had focused entirely on 
the read path — queries getting faster.
Elena immediately asked about 
the write path — inserts getting slower.

This is what senior engineers do.
They don't just evaluate whether 
the fix solves the problem.
They evaluate what the fix 
COSTS on the other side.

You noted this and ran both tests.
```

---

## Step 6 — Verifying on Staging

**Test 1: Does the new index eliminate the sort?**

```sql
-- After adding the index on staging:
EXPLAIN (ANALYZE, BUFFERS)
SELECT i.id, i.status, i.due_date,
       i.total_amount, i.currency
FROM invoices i
WHERE i.company_id = 'test-company-uuid'
AND i.status IN (
    'PENDING_REVIEW',
    'PENDING_APPROVAL',
    'APPROVED'
)
ORDER BY i.due_date ASC
LIMIT 20;
```

Output:

```
Limit
  (cost=1.93..1.98 rows=20 width=320)
  (actual time=0.341..0.352 rows=20 loops=1)

  ->  Index Scan using idx_invoices_company_status_duedate
          on invoices
          (cost=0.29..1.93 rows=142 width=320)
          (actual time=0.183..0.331 rows=20 loops=1)
          Index Cond: (company_id = ?)
                  AND (status = ANY (?))
          Order By: due_date ASC    ← satisfied by index
```

```
The Sort step is completely gone.

Before:
  Index Scan:  0.847ms
  Sort:       45.820ms  ← this step
  Total:      46.679ms

After:
  Index Scan:  0.331ms  ← already ordered
  Sort:        0ms      ← step doesn't exist
  Total:       0.352ms

Same query.
Same 142 rows.
130x faster.

The "Order By: due_date ASC" line 
inside the Index Scan node confirms 
PostgreSQL is using the index to 
satisfy the ordering —
it's not a separate step anymore.

Cost dropped from 8.47 to 1.93.
PostgreSQL's own estimate shows 
it's doing much less work.
```

**Test 2: Write latency impact**

```sql
-- Measure INSERT time before and after index

-- BEFORE (run on fresh staging row):
EXPLAIN ANALYZE
INSERT INTO invoices
    (id, company_id, supplier_id, status,
     due_date, total_amount, currency,
     invoice_date, created_at)
VALUES
    (gen_random_uuid(),
     'test-company-uuid',
     'test-supplier-uuid',
     'DRAFT',
     NOW() + INTERVAL '30 days',
     1500.00,
     'EUR',
     CURRENT_DATE,
     NOW());
-- Actual time: 7.83ms

-- AFTER (same insert, new index exists):
-- Actual time: 8.91ms
```

```
Before new index: ~7.8ms per INSERT
After new index:  ~8.9ms per INSERT
Difference:       +1.1ms per INSERT

Impact calculation:
  ~1,900 new invoices per week
  × 1.1ms additional write time each
  = ~2,090ms (2.1 seconds) of extra 
    write overhead distributed across 
    all instances over the entire week

In practice:
  A single invoice creation request 
  takes ~180ms total 
  (validation, S3 upload, OCR trigger, DB write).
  The DB write portion goes from 
  7.8ms to 8.9ms.
  That's +1.1ms inside a 180ms request.
  Invisible to any user.
  Won't appear in any percentile metric.

Verdict: acceptable.
The read improvement (130x faster 
for this query) far outweighs 
the write cost (+14% on one step 
of a multi-step operation).
```

You posted the staging results:

```
You (#expense-ap-dev):
───────────────────────
"Staging results:

 1. Sort step eliminated ✅
    EXPLAIN ANALYZE shows no Sort node.
    Query time: 46ms → 0.35ms 
    for test company (142 rows).

 2. Write latency:
    INSERT: +1.1ms per row.
    At 1,900 invoices/week:
    ~2.1 seconds additional write 
    time spread across the entire week.
    Immeasurably small per-request impact.

 Opening the PR."
```

Arjun:

```
Arjun:
───────
"Good verification approach —
 checking both read and write 
 impact before shipping an index 
 is the right practice.

 Proceed."
```

---

## The Migration

```sql
-- V24__add_composite_index_invoices_list_query.sql
-- flyway:executeInTransaction=false
--
-- CONTEXT:
-- GET /api/v1/invoices P99 was rising ~17ms/week
-- for 3 weeks (180ms → 247ms at time of this fix).
-- Root cause: existing (company_id, status) index
-- covers the WHERE clause but not ORDER BY due_date.
-- PostgreSQL performs a separate sort step after
-- the index scan. Sort time grows with table size.
--
-- FIX:
-- Extend index to include due_date as third column.
-- Within any (company_id, status) combination,
-- entries are now stored in due_date order inside
-- the index. PostgreSQL can satisfy WHERE + ORDER BY
-- from the index alone — no separate sort.
--
-- VERIFIED ON STAGING:
-- EXPLAIN ANALYZE confirms Sort step eliminated.
-- Query time: 46ms → 0.35ms for test dataset.
-- Write overhead: +1.1ms per INSERT (negligible).
--
-- PARTIAL INDEX (status NOT IN 'PAID', 'CANCELLED'):
-- The invoice list filter only queries active invoices
-- (DRAFT through PAYMENT_PENDING states).
-- PAID and CANCELLED are historical — they appear
-- only on the completed invoices view which has
-- its own query and index.
-- Excluding them keeps this index smaller and faster.
-- Rows entering PAID/CANCELLED fall out automatically.
-- Historical queries still work — they use
-- idx_invoices_company_status or a sequential scan,
-- which is acceptable for infrequent history queries.
--
-- CONCURRENTLY: production table has 284,000+ rows.
-- Regular CREATE INDEX would lock writes for ~15 seconds.
-- CONCURRENTLY builds without write locks.
-- Requires flyway:executeInTransaction=false.
-- IF NOT EXISTS: safe to re-run if migration fails midway.

CREATE INDEX CONCURRENTLY IF NOT EXISTS
    idx_invoices_company_status_duedate
ON invoices(company_id, status, due_date)
WHERE status NOT IN ('PAID', 'CANCELLED');
```

---

## The PR

```
PR: INV-319 — Add composite index 
              for invoice list query optimization

WHY:
─────
GET /api/v1/invoices P99 latency 
rising ~17ms/week for 3 weeks.
180ms → 247ms. Not alerting but 
the trend is structural — won't 
self-correct as table grows.

ROOT CAUSE:
────────────
Existing (company_id, status) index 
covers WHERE clause but not 
ORDER BY due_date.
PostgreSQL performs a separate sort 
after index scan.
Sort time grows with table size.
~1,900 new invoices/week means 
P99 rises ~17ms/week.

Confirmed via:
  - Datadog APM traces showing 
    831ms in DB span for slowest requests
  - EXPLAIN ANALYZE showing Sort node 
    consuming 98% of query time

FIX:
─────
Extended index to (company_id, status, due_date).
Within any (company_id, status) combination,
entries are in due_date order inside the index.
Sort step eliminated.

PARTIAL INDEX (excludes PAID/CANCELLED):
  Active invoice list never filters on these.
  Keeps index smaller. Historical queries unaffected.

VERIFIED ON STAGING:
─────────────────────
1. EXPLAIN ANALYZE: Sort node gone.
   Cost: 8.47 → 1.93.
   Actual query time: 46ms → 0.35ms.

2. Write latency: +1.1ms per INSERT.
   At our volume: ~2.1s additional 
   write overhead per week total.
   Immeasurably small per request.

EXPECTED IMPACT:
─────────────────
P99: ~247ms → estimated ~55-70ms
(based on Sort step elimination 
 and cost reduction in EXPLAIN)

IMPLEMENTATION:
────────────────
CREATE INDEX CONCURRENTLY 
(284,000+ rows — prevents write locks).
Partial index WHERE status NOT IN 
('PAID', 'CANCELLED').
flyway:executeInTransaction=false 
(required for CONCURRENTLY).

No existing ticket — proactive 
from monitoring observation.
Created INV-319 for tracking.
```

Elena reviewed. Two comments.

```
Elena Comment 1:
─────────────────
"INV-319 is correct 
 (Invoice Service, not Expense Service).
 But the PR title says EXP-319.
 Fix the prefix."
```

```
Small mistake — you'd written EXP 
(Expense prefix) instead of INV 
(Invoice prefix). 
Updated both the PR title 
and the description.

Noting this honestly:
Even at month 16, small mistakes happen.
The difference is you catch them 
quickly and fix them without drama.
```

```
Elena Comment 2:
─────────────────
"The partial index explanation 
 in the migration comment is good.
 Add one more sentence answering 
 the 'but what happens to PAID 
 invoice queries?' question.
 
 A reader in 12 months might wonder:
 'can I still query PAID invoices?
  will it break without this index?'
 
 Answer: yes, they can still query 
 PAID invoices. They'll use a 
 different index or a sequential scan.
 That's acceptable because those 
 queries are infrequent."
```

You added to the migration comment:

```sql
-- PAID and CANCELLED queries still work —
-- they use idx_invoices_company_status
-- (the original index, which covers all statuses)
-- or fall back to a sequential scan.
-- Both are acceptable: PAID/CANCELLED invoice
-- history is queried infrequently compared to
-- the active invoice list.
```

PR approved. Merged.

---

## The Result

The migration deployed to production on Thursday afternoon.

You watched Datadog that evening and through the following week:

```
P99 — GET /api/v1/invoices:

Thursday (deploy day):
  Before deploy:  241ms (measured that morning)
  After deploy:    63ms  ← immediate drop

Friday:           58ms
Following Monday: 61ms
Following Tuesday: 59ms
Following Wednesday: 62ms

Stable at 58-63ms.
No more weekly rise.
```

```
Before vs After:

P99 before fix:  247ms (and rising 17ms/week)
P99 after fix:    60ms (stable)

Reduction: (247 - 60) / 247 = 75.7%
≈ 76% reduction in P99 latency.

EXPLAIN ANALYZE confirmed:
  Before: Sort node, cost=8.47, time=46ms
  After:  No Sort node, cost=1.93, time=0.35ms

The ~60ms P99 in production 
vs 0.35ms in the test:
The staging test ran against a 
medium company with 142 rows.
Production P99 reflects the 
largest companies with 500+ 
active invoices — more rows to scan,
more JOIN FETCH data to process.
The index eliminates the SORT,
but the index scan + JOIN FETCH 
still takes some time at scale.
60ms for the worst 1% is still 
a 76% improvement and well 
within acceptable bounds.
```

You posted the results:

```
You (#expense-ap-dev):
───────────────────────
"One week post-deploy:

 GET /api/v1/invoices P99:
   Before: 247ms (rising ~17ms/week)
   After:  ~60ms (stable)
   76% reduction.

 Sort step confirmed eliminated 
 in production EXPLAIN ANALYZE.
 Write latency: no observable impact.

 The weekly trend is flat now —
 table growth no longer affects 
 the query because the sort step 
 doesn't exist anymore.
 The index scan grows very slowly 
 with table size, unlike the sort.

 Updated the Datadog dashboard 
 with an annotation marking 
 the deploy date.
 [link]"
```

Arjun posted in the thread:

```
Arjun:
───────
"Good work.

 Worth naming for the team:
 this was not a ticket.
 Nobody reported a problem.
 No alert fired.

 [Your name] noticed a latency 
 trend in their morning monitoring 
 review, investigated it, identified 
 root cause, proposed and verified 
 the fix, and shipped it — 
 all within one sprint.

 That's the difference between 
 responding to problems and 
 anticipating them.
 Both matter.
 The second is harder."
```

---

## In the Sprint Retro

Lukas brought it up two days later:

```
Lukas (retro):
───────────────
"I want to spend 2 minutes on 
 the invoice list latency fix.

 The P99 was at 247ms and rising.
 Below our alert threshold.
 No customer complaint.
 No ticket.

 [Your name] saw it in their 
 morning monitoring habit,
 investigated it, understood 
 why it was happening,
 proposed the fix with proper 
 verification on staging,
 and shipped it in the same sprint.

 Total time from noticing to 
 merged PR: about 4 hours.

 This is what owning a service 
 looks like.
 Not just responding to issues 
 that are assigned to you.
 Knowing your service well enough 
 to see when something is wrong 
 before it becomes a problem.

 I want the team to notice this 
 and think about where each of 
 you is doing this kind of 
 proactive work."
```

```
You didn't say anything in the retro.
But you wrote down what Lukas said.

"Knowing your service well enough 
to see when something is wrong 
before it becomes a problem."

This described something 
you didn't have language for 
until he said it.

In month 5 (Story 4), Elena found 
the N+1 bug in your PR.
Finn helped you fix it.

In month 16, you found the missing 
index yourself during a morning 
monitoring review.
Elena reviewed your fix.

Same class of problem —
a query without the right index,
causing performance degradation.

Completely different relationship to it.
Now you were the finder,
not the person being found.

That distance — month 5 to month 16 —
is what those 11 months of smaller 
stories actually added up to.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You said P99 was 247ms and trending up. Your alert was at 1,000ms. Why fix something that wasn't alerting?"**

```
Because the trend was wrong, 
not just the current value.

At 247ms, the problem exists 
but nobody's complaining yet.
Finance managers at large companies 
notice a slight slowness but 
attribute it to their connection 
or their browser.

At 600ms, they start complaining 
to their account manager.
At 900ms, support tickets come in.
At 1,000ms, the alert fires.

If I waited for the alert, 
we'd be fixing it under pressure —
with active complaints, 
a support queue building, 
a manager asking "when will 
this be resolved?"

Fixing it at 247ms was quiet,
deliberate, and took 4 hours.
Fixing it at 950ms would have 
been urgent, stressful, and 
potentially required faster 
but riskier changes.

The other reason: structural 
problems don't self-correct.
A rising P99 caused by growing 
data against an inadequate index 
will keep rising at the same rate 
until you fix the index.
There's no natural ceiling 
short of the data stop growing.

I had 45 minutes before standup.
The root cause was findable.
The fix was a Flyway migration.
It would have been strange not to do it.
```

---

**Q2: "Walk me through exactly what EXPLAIN ANALYZE showed and how it led to the fix."**

```
I ran EXPLAIN ANALYZE on the slow query 
from the APM trace.

The output showed two main steps 
running in sequence:

Step 1 — Index Scan using idx_invoices_company_status.
  PostgreSQL used the existing index 
  on (company_id, status) to find all rows 
  matching WHERE company_id = ? AND status IN (?).
  Found 142 rows for a medium-sized company.
  Time: 0.847ms — very fast.

Step 2 — Sort.
  Sort Key: due_date.
  The 142 rows from step 1 came out sorted 
  by (company_id, status) — the order of the index.
  They were NOT in due_date order yet.
  PostgreSQL had to sort them separately 
  to satisfy ORDER BY due_date ASC.
  Time: 45.820ms — this is where 98% of 
  the total query time went.

The fix was to include due_date in the index 
as a third column: (company_id, status, due_date).

Within any given (company_id, status) combination,
the index stores entries in due_date order.
When PostgreSQL scans the new index for matching rows,
they come out already sorted by due_date.
No separate sort step needed.

After adding the index, I ran EXPLAIN ANALYZE again.
The Sort node was completely gone from the output.
The query went from 46ms to 0.35ms for the same data.
In production P99: 247ms → 60ms.
76% reduction.
```

---

**Q3: "Elena asked you to check write latency. What did you find and how did you decide it was acceptable?"**

```
I measured INSERT time before and 
after adding the index on staging.

Before: ~7.8ms per INSERT.
After:  ~8.9ms per INSERT.
Difference: +1.1ms per INSERT.

To evaluate whether this was acceptable:

Impact at our volume:
~1,900 new invoices created per week.
1,900 × 1.1ms = ~2,090ms of additional 
write overhead across the entire week,
distributed across all instances 
and spread across 7 days.

Per-request impact:
An invoice creation request takes 
~180ms total — it includes validation,
S3 upload for the PDF, OCR trigger, 
and the DB write.
The DB write portion goes from 7.8ms 
to 8.9ms within that 180ms request.
That's a 0.6% change in total 
request latency.
Invisible to users.
Doesn't move any percentile metric.

The trade-off:
+1.1ms on writes.
In exchange for: 76% reduction 
on reads at P99 (187ms faster).

The invoice list endpoint is 
hit many times per minute —
every time a finance manager 
opens their dashboard or refreshes it.
Invoice creation happens when 
someone uploads an invoice —
much less frequent and less 
latency-sensitive than dashboard loads.

So we're adding a tiny cost to 
a low-frequency, latency-tolerant 
operation to significantly improve 
a high-frequency, latency-sensitive one.
That's a straightforward trade-off.

Elena asking me to check this 
wasn't because she expected a problem.
It was making sure I thought 
through both sides of an index change.
I apply that automatically now.
```

---

**Q4: "How did you verify the fix actually worked before deploying to production?"**

```
Three steps on staging:

Step 1 — EXPLAIN ANALYZE.
After adding the index on staging,
I ran EXPLAIN ANALYZE on the same 
slow query from the APM trace.
I confirmed the Sort node was gone 
from the output — not just reduced,
completely eliminated.
The query plan showed the index scan 
satisfying ORDER BY due_date directly.
Estimated cost dropped from 8.47 to 1.93.
Actual measured time: 46ms → 0.35ms.

Step 2 — Write latency check.
Measured INSERT time before and after.
Confirmed the overhead was +1.1ms 
per INSERT — acceptable at our volume.

Step 3 — Monitoring after production deploy.
Watched Datadog the evening of the deploy 
and through the following week.
P99 dropped from 241ms (measured that morning)
to 63ms immediately after deploy.
Remained stable at 58-63ms 
with no weekly rise trend.
The rising trend was gone because 
the sort step no longer scales 
with table growth — the index scan 
does, but that scales much more slowly.

The staging verification gave me 
confidence in the structural correctness 
of the fix.
The production monitoring confirmed 
it worked at real scale with 
real company data sizes.
```

---

Story 19 complete — this time with the full foundation underneath it.

```
What this story demonstrates:
───────────────────────────────

Technical:
  P99 latency — what it means, 
    why it matters for production health.
  Datadog metrics view — reading 
    a latency trend over time.
  Datadog APM traces — finding the 
    slow span in a specific request.
  EXPLAIN ANALYZE — reading the 
    query plan, identifying the 
    Sort bottleneck.
  Composite index covering ORDER BY — 
    how extending an index eliminates 
    the sort step.
  Partial index — excluding PAID/CANCELLED 
    to keep the index small.
  CONCURRENTLY — applied now by instinct.
  Write latency impact — both sides 
    of an index change.

Behavioral:
  Self-directed investigation — 
    no ticket, no manager direction.
  Flagged to team before writing code.
  Verified both read AND write impact 
    before shipping.
  PR description explained full reasoning.
  Measured outcome and shared it.

Growth marker:
  Month 5: Elena found the N+1 bug 
    in your PR. Finn helped you debug.
  Month 16: You found the missing 
    index in a morning monitoring review.
    Elena reviewed your fix.
  Same class of problem.
  Completely different relationship to it.

Ready for Story 20 when you are.
```