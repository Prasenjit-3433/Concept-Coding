Looking at the planning document, Block 6 Story 20 is the wiki documentation story. Let me write it in full.

---

# Story 20: Wiki Documentation — Leaving Something Behind for People You Haven't Met

---

## Context — Where You Were at Month 17

```
Month 16 had ended with the 
invoice list latency fix.

Nobody had asked you to look 
at the P99 trend.
Nobody had assigned you a ticket.
You noticed it in your morning 
monitoring review, investigated it,
and shipped the fix in the same sprint.

Arjun had posted in #expense-ap-dev:
"That's the difference between 
responding to problems and 
anticipating them."

Lukas had brought it up in retro.

But month 17 started with something 
quieter than all of that.

You were in your third week of month 17
when Kemal joined you on a call
with a question about Kafka consumer lag.

Not a complicated question.
He was debugging a consumer in 
the Expense Service — a new consumer 
he had just shipped — and the lag 
was growing.

He had opened Datadog.
He had found the consumer lag metric.
But he didn't know what to look 
at next.

"What do you check first?" he asked.

You spent 25 minutes walking him 
through it.
After the call, you opened Confluence 
and searched for any documentation 
on debugging Kafka consumer lag 
in your system.

You found a Confluence page from 
Arjun — written 18 months ago.
Three bullet points.
A diagram that referenced a service 
that no longer existed.
Nothing about Datadog, nothing about 
the outbox poller, nothing about 
SKIP LOCKED or lock_timeout.

You had just spent 25 minutes 
teaching Kemal something 
that wasn't written down anywhere.

Next time it would be someone else.
Twenty-five more minutes.
The same conversation.
Slightly different details.

You decided to write it down.
```

---

## The Situation

```
It was the Thursday of week 3, month 17.
3:30pm IST. No meetings until 5pm.

You opened Confluence.
You created a new page in the 
team's knowledge base:

"How to Debug Kafka Consumer Lag 
 in Expense & AP Services"

You blocked out 90 minutes.
```

---

## Your Task

```
Write a guide that is:
  1. Complete enough that someone 
     who has never debugged Kafka 
     consumer lag can follow it 
     without asking anyone.
     
  2. Specific to YOUR system.
     Not a generic Kafka tutorial.
     Your topics. Your consumer groups.
     Your Datadog queries. Your tools.
     
  3. Updated to reflect the current 
     state of the system — not the 
     state from 18 months ago.

What you drew from:
  - Story 8 (Kafka consumer implementation) —
    your first exposure to consumer lag 
    and what it means.
  - Story 9 (production incident) —
    where you first saw Arjun debug 
    consumer lag live, using Datadog 
    to separate "producer publishing too much"
    from "consumer processing too slowly."
  - Story 13 (cache stampede) — 
    the habit of morning monitoring 
    that led to proactive investigation.
  - Story 16 (production incident) —
    where consumer lag was a SYMPTOM 
    of DB connection pool exhaustion,
    not the root cause.
  - The incident in Story 9 —
    the postmortem you wrote that 
    had a timeline, a root cause,
    and contributing factors.
```

---

## Writing the Wiki — What You Actually Wrote

You didn't write a tutorial.
You wrote the document you wished
had existed when you were Kemal —
when you were asking Arjun the same questions.

---

```
CONFLUENCE PAGE:
────────────────────────────────────────────────────────────
How to Debug Kafka Consumer Lag 
in Expense & AP Services

Team: Expense & AP Engineering
Author: [Your name]
Last updated: [Month 17, Week 3]
Reviewed by: Arjun Sharma (pending)
────────────────────────────────────────────────────────────

WHO THIS IS FOR
────────────────
This guide is for anyone on the team 
who opens Datadog and sees consumer lag 
on one of our Kafka topics — and doesn't 
know what to do next.

It covers:
  - What consumer lag actually means
  - How to tell if it's a real problem
  - The two main causes (more messages 
    OR slower processing)
  - How to find which cause it is
  - The most common specific causes 
    we've seen in our system
  - What to do in each case

If you're not sure whether consumer lag 
is alerting or just a momentary blip, 
read "Is This A Real Problem?" first.

────────────────────────────────────────────────────────────

QUICK REFERENCE — OUR KAFKA TOPICS
──────────────────────────────────────

Topics our team PRODUCES 
(we publish these events):
  expense.submitted
  expense.approved
  expense.rejected
  reimbursement.completed
  invoice.submitted
  invoice.verified
  invoice.approved
  invoice.rejected
  invoice.paid

Topics our team CONSUMES 
(we read these events):
  payment.completed → marks reimbursements 
                      and invoices as PAID
  user.deactivated  → reassigns pending approvals
  invoice.verification.requested → assigns verifier
  ocr.completed     → moves invoice from DRAFT 
                      to PENDING_REVIEW or PENDING_APPROVAL

Consumer Groups:
  expense-service  → consumes events for 
                     Expense & Reimbursement Service
  invoice-service  → consumes events for 
                     Invoice & AP Service

────────────────────────────────────────────────────────────

WHAT IS CONSUMER LAG?
──────────────────────

Consumer lag = how many messages 
are waiting to be processed that 
the consumer hasn't read yet.

Lag of 0: consumer is keeping up.
          Reads events as fast as 
          they're published.

Lag of 500: 500 messages published 
            but not yet processed.
            Consumer is behind.

Lag growing continuously:
  Producer is publishing faster 
  than consumer can process.
  OR consumer is failing and 
  not making progress.

Lag that spikes then recovers:
  A brief traffic burst.
  Consumer caught up.
  Usually not a problem.

─────────────────────────────────────────────────────────────

IS THIS A REAL PROBLEM?
────────────────────────

Not all lag is an emergency.

LOW LAG (0-100):
  Normal. Consumer is healthy.
  No action needed.

MEDIUM LAG (100-1000):
  Monitor it.
  Is it growing? → investigate.
  Is it stable? → usually okay.
  Is it shrinking? → consumer catching 
                     up from a burst.

HIGH LAG (1000+, growing):
  This is what our Datadog alert fires on.
  Threshold: 1,000 events for > 10 minutes.
  
  If alert fired: follow investigation 
  steps below.

HIGH LAG (1000+, stable or shrinking):
  Consumer was falling behind but 
  is now catching up.
  Monitor until it reaches 0.
  No immediate action needed unless 
  it starts growing again.

KEY QUESTION:
  "Is the lag growing, stable, or shrinking?"
  
  Growing → urgent: find the cause.
  Stable   → monitor: watch for 10 minutes.
  Shrinking → wait: consumer is catching up.

─────────────────────────────────────────────────────────────

STEP 1: CHECK THE DATADOG METRICS
──────────────────────────────────

Where to find the lag metric:
  Datadog → Metrics
  Metric name: kafka.consumer.records-lag
  
  Filter by:
    service: expense-service 
    (or invoice-service)
    
    consumer_group: expense-service
    (or invoice-service)
    
    topic: the topic you're investigating
    (e.g. payment.completed)

What to look at:
  1. Is the lag growing continuously?
     → Something is wrong. Continue below.
  
  2. When did the lag start growing?
     Look at the time-series graph.
     Find the exact timestamp where 
     lag started increasing.
     Note this time — you'll search 
     logs for what changed then.
  
  3. Which topic has the lag?
     You need this to find the right 
     consumer in the code.
     (See consumer group mapping above)

─────────────────────────────────────────────────────────────

STEP 2: TWO ROOT CAUSES — IDENTIFY WHICH ONE
───────────────────────────────────────────────

Consumer lag can be caused by:

CAUSE A: Producer is publishing more 
         events than usual.
         
         The consumer is keeping up 
         with its normal capacity, 
         but there's simply more volume 
         coming in.

CAUSE B: Consumer is processing events 
         MORE SLOWLY than usual.
         
         Same volume as before,
         but each event takes longer to handle.

These require different fixes.
CAUSE A → find why volume spiked.
CAUSE B → find why processing is slow.

HOW TO TELL WHICH ONE:

Open two Datadog metric graphs 
side by side for the same time window:

  Graph 1: Producer rate
    Metric: kafka.producer.record-send-rate
    Filter: topic = [your topic]
    
  Graph 2: Consumer throughput
    Metric: kafka.consumer.records-consumed-rate
    Filter: consumer_group = [your group]
             topic = [your topic]

Compare them at the time lag started:

Producer rate spiked AND consumer 
throughput is flat/decreasing?
→ CAUSE A (volume spike)

Producer rate is flat AND consumer 
throughput is decreasing?
→ CAUSE B (consumer slowing down)

Both are flat but lag is growing?
→ This shouldn't happen. Check if 
  consumer is even running 
  (see "Consumer Not Running" below).

─────────────────────────────────────────────────────────────

STEP 3A: IF CAUSE A (VOLUME SPIKE)
──────────────────────────────────

Why might producer volume spike?

In our system:
  expense.submitted spikes on Monday mornings
    (employees submitting weekend expenses)
  
  payment.completed spikes after payment runs
    (Payment Service processes batches 
     on Tue/Thu — we see a burst after)
  
  invoice.approved spikes after finance 
    meetings (multiple approvals at once)

Usual response:
  If the lag is growing but the consumer 
  is healthy (check STEP 3B below),
  the consumer will typically catch up 
  once the burst subsides.
  
  Watch the lag for 10-15 minutes.
  If it peaks then starts decreasing: fine.
  If it keeps growing with no sign 
  of decreasing: the consumer can't 
  keep up even at non-burst rate.
  That's a capacity problem.

Capacity problem fix options:
  1. Increase consumer concurrency
     (more threads per instance)
     See: application.properties 
     spring.kafka.listener.concurrency
     Warning: only helps if you have 
     more partitions than current threads.
     Check partition count first.
     
  2. Increase service instances
     (more instances = more consumers 
      in the consumer group)
     This is an ECS task count change.
     Talk to DevOps or Arjun before 
     doing this in production.

─────────────────────────────────────────────────────────────

STEP 3B: IF CAUSE B (CONSUMER SLOWING DOWN)
─────────────────────────────────────────────

This is the more common and 
more interesting case.

The consumer is processing events 
SLOWER than before.
Something in the processing path 
is taking longer.

What causes consumer processing to slow down?

In our system, the most common causes:

──────────────────────────────────────────
CAUSE B1: Database connection pool exhaustion
──────────────────────────────────────────
The consumer needs a DB connection to 
process each event. If the connection 
pool is exhausted (all connections in use),
the consumer waits for a free connection.
Consumer throughput drops.
Lag grows.

This happened in the INV-2025-04-17 incident.
Root cause was 4 concurrent payment run 
requests holding connections via pessimistic 
locks for 50+ seconds.

How to check:
  Datadog Metric: jdbc.connections.active
  Filter: service = invoice-service (or expense-service)
  
  Normal: 3-5 active out of 10 max.
  Problem: 9-10 active for > 3 minutes.
  
  Also check: jdbc.connections.pending
    Any pending connections = pool exhausted,
    requests are queuing.

If pool is exhausted:
  1. Find what's holding connections.
     Datadog APM → Traces
     Filter: service = [your service]
             duration > 10s
     
     Long-running traces = something 
     is holding a DB connection open 
     for a long time.
  
  2. Check pg_stat_activity 
     for long-running sessions:
     
     SELECT pid, application_name, 
            state, wait_event,
            EXTRACT(EPOCH FROM 
              (NOW() - state_change)) AS seconds
     FROM pg_stat_activity
     WHERE application_name = 'invoice-service'
     AND state = 'idle in transaction'
     ORDER BY seconds DESC;
  
  3. If sessions are stuck in lock contention 
     for > 60 seconds:
     After confirming with on-call lead —
     pg_terminate_backend(pid)
     to release them.
     
     Note: since INV-2025-04-17, lock_timeout = 10s
     is configured. Sessions shouldn't be stuck 
     for > 10s on lock contention.
     If you're seeing > 10s: something changed.

──────────────────────────────────────────
CAUSE B2: A slow DB query in the consumer
──────────────────────────────────────────
The consumer calls a service method 
which calls a repository.
If that repository query has become slow
(table grew, index missing, etc.),
every event takes longer to process.
Consumer throughput decreases.
Lag grows.

This is structurally similar to what 
caused the P99 rise in GET /api/v1/invoices —
but showing up in the consumer instead 
of an HTTP endpoint.

How to check:
  Datadog APM → Traces
  Filter: service = [your service]
          Find the Kafka consumer span.
          Look for long DB child spans.
  
  OR:
  Datadog APM → Top DB Queries
  Sort by total time.
  Look for a query that appeared 
  recently at the top of the list.

If you find a slow query:
  Run EXPLAIN ANALYZE on staging.
  Look for Sort steps, sequential scans 
  on large tables, missing indexes.
  
  Add an index and measure improvement 
  before deploying to production.
  Always use CREATE INDEX CONCURRENTLY 
  on production tables > 50,000 rows.

──────────────────────────────────────────
CAUSE B3: A downstream FeignClient is slow
──────────────────────────────────────────
The consumer calls a service method 
that calls User & Org or another 
service via FeignClient.
If that service is degraded,
each FeignClient call takes longer.
Consumer processing slows down.

How to check:
  Datadog APM → Traces
  Find the consumer trace.
  Look for a FeignClient span 
  with high duration.
  
  Note: most FeignClient calls in 
  consumers go through the approval 
  policy cache (Redis first, 
  FeignClient only on miss).
  
  Check cache hit rate first:
  Datadog Metric: approval_policy.cache.result
    tag: result = HIT/MISS/FALLBACK
  
  FALLBACK rate > 0:
    Redis is unavailable, all FeignClient 
    calls going direct.
    Check Redis health.

──────────────────────────────────────────
CAUSE B4: Consumer exceptions and retries
──────────────────────────────────────────
If the consumer is throwing exceptions 
repeatedly, it retries with backoff 
(3 attempts, 1s/2s/4s).
This adds up to 7 seconds minimum 
per failed event.
If many events are failing, 
throughput drops significantly.

How to check:
  Datadog Logs:
    service: [your service]
    level: ERROR
    logger: *Consumer
    
  Look for repeated errors on the 
  same event (same key in the log).
  Repeated = transient failures retrying.
  
  Then check the DLT topics:
  Datadog Metric: dlt.messages.total
    tag: source_topic
  
  Rising DLT count = permanent failures 
  routing to DLT (not retrying, but 
  events are being lost from the consumer's 
  perspective — see DLT runbook 
  for investigation).

──────────────────────────────────────────
CAUSE B5: Consumer not running
──────────────────────────────────────────
The service instance might have 
crashed or restarted.
Consumer group is rebalancing.
During rebalance: no events processed.

How to check:
  Datadog Logs:
    service: [your service]
    message: *Consumer Group*rebalance*
  
  ECS task events (AWS Console):
    Check if service has fewer 
    running tasks than expected.
  
  If instance crashed:
    Check for OOM errors or 
    crash logs in Datadog.
    ECS will restart the task automatically.
    Lag will clear once consumer restarts.
    
    If it keeps crashing:
    This is an incident. Page on-call.

─────────────────────────────────────────────────────────────

STEP 4: AFTER IDENTIFYING THE CAUSE
──────────────────────────────────────

Once you know the cause, fix it.
Document what you did in #incidents 
(even if it doesn't rise to a full incident).

Format:
  "[Time] Consumer lag on [topic] — 
   Root cause: [cause].
   Action taken: [what you did].
   Current lag: [X] — [growing/stable/shrinking]."

Keep updating until lag reaches 0.

─────────────────────────────────────────────────────────────

STEP 5: WRITE A POSTMORTEM IF:
────────────────────────────────
  - Customer impact occurred
    (delayed payments, failed operations)
  - Lag was > 5,000 for > 30 minutes
  - Root cause was a code or 
    configuration bug (not a traffic burst)

For postmortem template: 
see [link to postmortem template in Confluence].

─────────────────────────────────────────────────────────────

QUICK DECISION TREE
──────────────────────

Consumer lag is growing. What now?

Is it > 1,000?
├── NO:  Monitor for 10 min. 
│        Is it still growing? 
│        YES → continue below.
│        NO  → watch it clear, no action.
└── YES: Continue below.

Is producer rate also spiking?
├── YES: CAUSE A — traffic burst.
│        Will consumer catch up in 10 min?
│        YES → wait.
│        NO  → check consumer health (CAUSE B).
└── NO:  CAUSE B — consumer slowing down.

CAUSE B — check DB connection pool:
Is jdbc.connections.active near max?
├── YES: Something holding connections.
│        Find it in APM traces.
│        Check pg_stat_activity.
└── NO:  Check slow DB queries in APM.
         Check FeignClient latency in APM.
         Check consumer exception logs.
         Check if consumer is even running.

─────────────────────────────────────────────────────────────

REFERENCE: DATADOG QUERIES
────────────────────────────

Consumer lag:
  kafka.consumer.records-lag
  by consumer_group, topic

Producer rate:
  kafka.producer.record-send-rate  
  by topic

Consumer throughput:
  kafka.consumer.records-consumed-rate
  by consumer_group, topic

DB connection pool:
  jdbc.connections.active
  jdbc.connections.pending
  by service

DLT message rate:
  dlt.messages.total
  by source_topic, exception_type

Cache hit rate:
  approval_policy.cache.result
  by result

Consumer errors:
  Datadog Logs
  service:[your-service] level:ERROR 
  logger:*Consumer

─────────────────────────────────────────────────────────────

RELATED PAGES
──────────────
[Link] DLT Investigation and Replay Runbook
[Link] Outbox Poller Health Runbook
[Link] lock_timeout Configuration — Why and How
[Link] Incident Postmortem Template

RELATED INCIDENTS (for context)
─────────────────────────────────
INV-2025-04-17: Outbox poller lost 
  connectivity due to DB connection 
  pool exhaustion from lock contention.
  Consumer lag reached 6,203.
  Resolved via pg_terminate_backend.
  
EXP-2025-03-18: expense.submitted 
  consumer lag spike due to missing 
  index on duplicate submission check.
  Consumer lag reached 4,847.
  Resolved via CREATE INDEX CONCURRENTLY.
  Both incidents show: consumer lag 
  is often a SYMPTOM of a DB problem,
  not a Kafka problem.
────────────────────────────────────────────────────────────
```

---

## What Happened When You Shared It

You finished writing at 5:15pm IST.
You posted in #expense-ap-dev:

```
You (#expense-ap-dev):
───────────────────────
"Wrote a Kafka consumer lag debugging 
 guide for our services.
 
 Motivation: Kemal asked me how to 
 debug consumer lag this morning.
 I spent 25 minutes explaining it.
 Then I searched Confluence and 
 found Arjun's 3-bullet-point page 
 from 18 months ago.
 
 So I wrote the one I wish had 
 existed.
 
 [Link to Confluence page]
 
 @Arjun — would appreciate your 
 review since you know this area 
 better than anyone. Especially 
 the CAUSE B section — want to 
 make sure I haven't missed 
 a common cause we've seen."
```

Arjun replied within the hour:

```
Arjun:
───────
"Reading it now.
 First impression: this is 
 better than what I wrote 18 months ago.
 
 You included the connection pool 
 cause — I didn't put that in 
 the original because we hadn't 
 seen it happen yet.
 Now it's one of the first things 
 I think about."
```

He reviewed it and added two comments on the Confluence page.

```
Arjun Comment 1:
─────────────────
"Under CAUSE B3 (FeignClient slow):
 
 Add a note that if the FeignClient 
 is timing out AND Redis fallback 
 is also failing (Redis down), 
 consumers can pile up waiting 
 for FeignClient responses.
 
 This is the 'double failure' scenario —
 cache miss AND upstream slow at 
 the same time.
 
 Add: 'If you see both 
 approval_policy.cache.result FALLBACK 
 rate rising AND high FeignClient 
 latency in APM, check Redis health 
 first — cache recovery will 
 solve both issues simultaneously.'"
```

You updated the page with Arjun's addition.

```
Arjun Comment 2:
─────────────────
"The quick decision tree is excellent.
 
 One thing to add at the very top 
 before 'Is it > 1,000?':
 
 First question should be:
 'Is an alert already firing 
  in #expense-ap-alerts?'
 
 If yes: this is an active incident.
 Go to the DLT runbook or the 
 outbox health runbook, not this guide.
 This guide is for investigation — 
 the incident runbooks are for response.
 
 Separating investigation guides 
 from incident response procedures 
 is important so people know 
 which to use when."
```

```
This was a useful distinction 
you hadn't thought to make.

Investigation guide:
  "Something looks wrong, 
   let me figure out why."
  No active alert.
  No customer impact yet.
  Diagnostic purpose.

Incident response runbook:
  "Alert is firing.
   Customer impact may be occurring.
   Follow these steps."
  Procedural. Fast. Prescriptive.

You had written an investigation guide.
Arjun was right that the framing 
should make that clear from the start —
so someone in an active incident 
doesn't waste time reading 
a diagnostic guide when they need 
a response procedure.

You added to the top of the page:
"This is a DIAGNOSTIC guide, not 
an incident response procedure.
If an alert is actively firing 
in #expense-ap-alerts: 
go to the relevant incident runbook first."
```

After Arjun marked his comments as resolved:

```
Arjun (Slack):
───────────────
"This is genuinely good documentation.
 
 The two related incidents at the bottom 
 are especially useful — they give 
 historical context that no generic 
 Kafka guide can provide.
 
 Marking as reviewed."
```

---

## What Happened Next — Three Weeks Later

Three weeks after you published the page, you saw a message in #expense-ap-dev:

```
Léa (#expense-ap-dev):
────────────────────────
"Quick question — seeing consumer lag 
 on invoice.verification.requested.
 Lag is ~400, stable for last 20 min.
 Not alerting.
 
 Is this normal after Thursday 
 morning invoices upload batch?"
```

Before you could respond, Kemal replied:

```
Kemal:
───────
"Use [Your name]'s debugging guide —
 first question in the decision tree:
 is it > 1000?
 
 400 stable = monitor.
 If it starts growing: check producer 
 rate vs consumer throughput."
```

Léa: "Ah! Thanks. Found it. Reading now."

```
You saw this exchange in Slack.

You were the second person to see it.
Kemal answered before you did.

Using your guide.
Correctly.

Two months ago Kemal was asking you 
how to debug consumer lag.
Today he was pointing Léa to 
the guide you'd written.

The guide had done its job.
The conversation that used to be 
"ask [your name]" was now 
"read the page."

You didn't need to be in the room.
```

---

## The Conversation With Elena — The Following Sprint

In your weekly tech sync, two weeks later, Elena brought it up:

```
Elena:
───────
"The Kafka debugging guide.
 I want to ask you something 
 about the motivation behind it.
 
 You wrote it without anyone 
 asking you to.
 Kemal asked you a question,
 you answered, you wrote the guide.
 
 Why didn't you just answer Kemal 
 and move on?
 
 What made you decide to write 
 it down?"
```

```
You thought about this.
```

```
You:
─────
"Honestly — I searched Confluence 
 first.
 I wanted to check if there was 
 something I could point him to.
 
 And I found Arjun's 3-bullet-point 
 page from 18 months ago that 
 didn't have any of the things 
 I ended up telling Kemal about.
 
 So I answered Kemal directly.
 But then I thought: the next person 
 who has this question will probably 
 go through the same thing.
 And the person after that.
 
 If I write it down clearly once,
 those conversations don't have to 
 happen the same way.
 
 Kemal can answer Léa with a link 
 instead of asking me.
 Which is what happened."
```

Elena:

```
Elena:
───────
"That's the right reason.
 
 The wrong reason to write documentation 
 is 'I should document things.'
 
 The right reason is exactly what 
 you said: I answered this once 
 and the answer should outlast 
 the conversation.
 
 One thing I want to name:
 the related incidents section 
 at the bottom of your guide.
 That's not something you'd find 
 in a generic Kafka tutorial.
 It's only possible because you 
 were in those incidents.
 
 You wrote 'consumer lag is often 
 a symptom of a DB problem, 
 not a Kafka problem' — and then 
 gave two real examples from 
 our system.
 
 That's institutional knowledge.
 It lives in the people who were 
 in the room.
 
 Now it lives in a Confluence page.
 
 That's valuable beyond whatever 
 individual contribution you made 
 in those incidents."
```

```
"Institutional knowledge.
 It lives in the people who were 
 in the room."

You wrote this down.

The phrase named something you 
had been doing without knowing 
it had a name.

Every incident you participated in —
Story 9 (watching Arjun debug),
Story 16 (leading the incident yourself) —
you had learned something specific 
to your system.

Not general Kafka principles.
Not textbook database theory.
The specific failure modes of 
YOUR consumers on YOUR schema 
with YOUR connection pool configuration.

That knowledge had no place to go 
until you wrote the guide.
Now it was accessible to anyone 
who joined the team after you.
```

---

## The Broader Reflection — What This Story Was About

```
In month 3, you wrote a WireMock stub 
and updated the README.
You fixed something that had annoyed you 
during onboarding.
You left it in a better state than 
you found it.

In month 17, you wrote a debugging guide.
You fixed something that would have 
annoyed Kemal, and Léa after him,
and whoever joined next.

Same instinct.
Different depth.

In month 3, the contribution 
was one stub and one README line.
Elena said: "When you fix something 
during onboarding, document it 
immediately — you have the freshest 
eyes on what's missing."

In month 17, the contribution 
was a comprehensive guide that 
contained institutional knowledge 
from 9 months of incidents 
you had participated in.

You couldn't have written Story 20's 
guide in month 3.
You didn't know what SKIP LOCKED was.
You hadn't seen connection pool 
exhaustion cause consumer lag.
You hadn't debugged a consumer 
in production.

The guide was only possible 
because of the 14 months before it.
All those stories accumulated into 
something you could put on a page.

And the measure of whether 
the page was good:
Kemal using it to answer Léa.
Without asking you.

That's the arc of this story.
And of all 20 stories before it.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You wrote this guide without being asked. How did you decide it was worth your time?"**

```
The cost-benefit was simple.

I spent 25 minutes answering 
Kemal's question.
I would spend another 25 minutes 
answering the next person's question.
And the one after that.

I spent 90 minutes writing the guide.
After that, the next person reads 
the guide instead of asking me.
And the one after that.

90 minutes invested once vs 
25 minutes spent every time 
someone new asks the question.

At our team's growth rate — 
we onboard 2-3 new engineers 
per quarter — the break-even 
is about 3-4 conversations.
After that, every additional 
person who reads the guide 
instead of asking is time saved.

But the honest motivation wasn't 
a cost-benefit calculation.
It was that I searched Confluence 
and found nothing useful.
I had just spent 25 minutes 
explaining something that SHOULD 
have been written down 18 months ago.
Writing it down felt like 
the obvious response.

The return wasn't theoretical either.
Three weeks later, Kemal answered 
Léa using my guide without 
coming to me.
That was the payoff.
```

---

**Q2: "Elena called it 'institutional knowledge.' What does that mean and why does it matter?"**

```
Institutional knowledge is information 
that exists only because certain people 
have been in certain situations.

A generic Kafka tutorial can tell you 
what consumer lag is and how to 
measure it.
Only someone who debugged the 
INV-2025-04-17 incident can tell you 
that in OUR system, consumer lag 
is often a symptom of DB connection 
pool exhaustion — and that lock_timeout = 10s 
was added specifically to prevent 
the mechanism that caused that incident.

That detail doesn't exist in any 
book or tutorial.
It exists in the postmortem, 
in the people who were in the room,
and now in the Confluence page.

Why it matters:
When people leave a team — and they do,
everyone does eventually — 
their institutional knowledge 
usually goes with them.
The next engineer learns the same 
lesson from the next incident.
The team doesn't accumulate wisdom;
it cycles through the same failure modes.

Documentation breaks that cycle.
The next engineer who sees consumer 
lag on invoice-service doesn't 
have to live through the connection 
pool incident to know to check 
jdbc.connections.active.
They read the guide.
Then they check.

That's what institutional knowledge 
preserved in writing does.
It makes the team smarter than 
any individual who's currently on it.
```

---

**Q3: "Arjun added two comments that improved the guide. What did you take from that?"**

```
Two things.

First: his first addition — 
the double failure scenario where 
Redis is down AND FeignClient is slow —
was something I genuinely hadn't 
thought of. He had seen it.
I hadn't.

This is the gap between someone 
with 7 years of distributed systems 
experience and someone with 
18 months.
He has more failure modes 
memorized than I do.

Even when you write the guide,
having it reviewed by someone 
more experienced makes it better.
I asked Arjun to review specifically 
because I knew the CAUSE B section 
was where I was most likely to 
be missing something.
He confirmed it.

Second: the investigation guide 
vs incident response runbook distinction.
I hadn't thought about that framing.
I had written a diagnostic guide 
but hadn't labeled it as such.
Someone in an active incident 
might have read it looking for 
step-by-step resolution and 
found a diagnostic framework instead.

That's a usability problem.
Arjun caught it.
I added the disclaimer at the top.

What I take from this:
Writing the guide was the right move.
Having it reviewed before declaring 
it "done" was also the right move.
Good documentation isn't 
what one person knows.
It's what a team has validated.
```

---

**Q4: "The guide was 90 minutes of work. Was that a good use of your time during a sprint?"**

```
Yes. Here's why.

During the sprint, my committed 
ticket work was on track.
This happened on a Thursday afternoon 
with 90 minutes before my next meeting.
It wasn't interrupting sprint commitments.

More importantly: the question of 
"is this a good use of sprint time" 
is the wrong frame for documentation.

Documentation is not sprint work 
in the way feature development is.
You don't estimate it.
You don't plan it.
You don't have a Jira ticket for it.

You do it when the cost of NOT 
doing it becomes visible.
The cost became visible when I 
searched for a guide and found 
three bullet points from 18 months ago.

If every engineer only works on 
what's ticketed, documentation 
never gets written.
Because documentation is always 
less urgent than the next feature.
It's never urgent until someone 
is lost and can't find the answer.

The 90 minutes was an investment 
in reducing future friction for 
the team — including for myself.
The next time I'm in an incident 
and need to check Kafka consumer lag 
causes systematically, I can reference 
the guide I wrote.

I also should note: Lukas never 
asked about that 90 minutes.
He saw the guide in the channel, 
saw the Arjun review, and saw Kemal 
using it to answer Léa.
That was the metric that mattered.
```

---

Story 20 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  Consumer lag debugging methodology —
    the systematic approach 
    (not just "check logs").
  CAUSE A vs CAUSE B distinction —
    producer volume vs consumer slowdown.
  Specific causes in your system:
    DB connection pool exhaustion,
    slow queries, FeignClient degradation,
    consumer exceptions and retries.
  The double failure scenario 
    (Arjun's addition).
  Investigation guide vs incident 
    response runbook (Arjun's framing).

Behavioral:
  Self-directed contribution 
    with no ticket, no manager direction.
  Asked the right person for review 
    (Arjun, who knows this area deepest).
  Incorporated review feedback 
    without defensiveness.
  Labeled the guide correctly 
    (diagnostic vs response).
  Connected the guide to real incidents 
    (institutional knowledge preserved).

Growth marker:
  Month 3: WireMock stub and README update.
    "When you fix something during 
    onboarding, document it."
    
  Month 17: Kafka debugging guide.
    "Institutional knowledge. 
    It lives in the people who were 
    in the room. Now it lives in 
    a Confluence page."
    
  Same instinct. 
  Depth of what you were documenting: 
  completely different.
```

Ready for Story 21 — Lukas's L2 acknowledgment — whenever you are.