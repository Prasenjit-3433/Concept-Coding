# Story 15: First ADR Contribution — Writing What You Decided and Why

---

## Context — What an ADR Is and Why It Matters

```
By month 10, you had made real 
technical decisions.

Story 12: Cache-Aside with Redis.
          15-minute TTL with jitter.
          Redis-only first, 
          Caffeine later if needed.
          Event-driven invalidation 
          designed for future.

Story 13: Mutex lock for concurrent 
          miss scenario.
          setIfAbsent with 10-second 
          lock TTL.
          5 retries at 50ms.
          Fallback to FeignClient 
          if retries exhausted.

These were real decisions.
They had reasoning behind them.
They had alternatives considered 
and rejected.
They had tradeoffs accepted.

And all of that reasoning lived 
in your Notion notes,
in Slack messages,
in PR descriptions,
in your own head.

Nowhere permanent.
Nowhere findable.
Nowhere that a new engineer 
joining in month 18 could 
read and understand WHY 
the caching works the way it does.
```

This is the problem ADRs solve.

```
ADR = Architecture Decision Record.

Not a design document 
(which describes what to build).
Not a runbook 
(which describes how to operate something).

An ADR records a specific 
architectural or design decision:
what was decided,
what alternatives were considered,
why this option was chosen,
what consequences it has.

Once written, it becomes part 
of the codebase — checked into 
Git, reviewed like code, 
permanent record.

A year from now, when someone asks:
"Why do we use a mutex lock 
 for cache misses instead of 
 Caffeine with a shorter TTL?"

The answer is in the ADR.
Not lost in Slack history.
Not in someone's memory.
In the repository.
```

---

## The Situation

It was the Thursday of week 3, month 10. The sprint was winding down. Your caching implementation was merged and in production. The stampede fix was merged. Léa's ticket was done.

In your weekly tech sync with Elena, after you had discussed the monitoring numbers from the caching work, Elena said something that surprised you slightly:

```
Elena:
───────
"One more thing before we close.
 The caching work you did this sprint —
 the design doc you wrote before 
 the meeting, the decision to use 
 Redis-only first, the mutex lock 
 for stampede —
 those were real architectural decisions.
 
 And right now, the reasoning only 
 lives in the design doc in Confluence
 and your PR descriptions.
 
 I'd like you to write an ADR for it.
 
 Not a long document.
 Not a research paper.
 Just: what we decided,
 what we considered and rejected,
 and why we chose what we chose.
 
 Have you read an ADR before?"
```

```
You:
─────
"I've seen the folder in Confluence —
 there are a few old ones in there.
 I read through the one about 
 the outbox pattern when I was 
 learning that area.
 But I've never written one."

Elena:
───────
"Good starting point.
 Read that one again.
 Then read the one about 
 why we chose ECS over Kubernetes.
 Get a feel for the format.
 
 Then write yours.
 Keep it under two pages.
 Share it with me and Arjun 
 before you put it in the 
 official folder.
 
 One hour of writing.
 That's all this should take."
```

---

## What You Read First — Understanding the Format

You spent 30 minutes reading the two existing ADRs Elena had pointed to.

The outbox pattern ADR:

```
ADR-004: Use Transactional Outbox Pattern 
         for Kafka Event Publishing
         
Date: [earlier date]
Status: Accepted
Author: Elena Müller
Reviewers: Arjun Sharma, Lukas Becker

CONTEXT:
─────────
Services need to update their database 
AND publish a Kafka event atomically.
Direct Kafka publish from within a 
@Transactional method creates a 
dual-write problem: if Kafka is 
unavailable after the DB update commits,
the event is lost permanently.

DECISION:
──────────
Use the Transactional Outbox Pattern.
Write to an outbox_events table in the 
same @Transactional method as the 
business update. A separate OutboxPoller 
reads unpublished events and publishes 
them to Kafka.

ALTERNATIVES CONSIDERED:
─────────────────────────
1. Direct kafkaTemplate.send() within 
   @Transactional
   Rejected: Kafka does not participate 
   in JDBC transactions. DB update and 
   Kafka publish can diverge.

2. Kafka Transactions (exactly-once semantics)
   Rejected: Requires both producer and 
   consumer to be part of the same Kafka 
   transaction. Adds significant complexity 
   and doesn't solve the DB ↔ Kafka 
   atomicity problem.

CONSEQUENCES:
─────────────
Positive:
  DB update and event publish are atomic.
  Events are retried automatically if 
  Kafka is temporarily unavailable.
  
Negative:
  Adds operational complexity 
  (OutboxPoller must be monitored).
  Small latency between DB commit 
  and Kafka publish (poller interval: 100ms).
```

The ECS vs Kubernetes ADR was similar in structure — context, decision, alternatives, consequences. It was clear and direct. No padding. No justifying the decision more than necessary. Just the reasoning.

```
What you noticed about both ADRs:
───────────────────────────────────
They were SHORT.
The outbox ADR fit on one page.
The ECS ADR was one and a half.

They didn't try to prove the 
decision was perfect.
They recorded what was decided 
and why — including the negatives.

The ECS ADR explicitly said:
"This decision may be revisited 
 when the DevOps team grows beyond 
 5 people and Kubernetes expertise 
 is available."

That honesty — acknowledging 
when to revisit — was something 
you hadn't expected in a 
formal document.
It made the document feel more 
trustworthy, not less.

You wrote a note to yourself:
"ADR = honest record, not justification.
 Include the negatives.
 Include when to revisit."
```

---

## Writing the ADR — First Draft

You sat down and wrote. You gave yourself one hour as Elena had said.

You opened a new Confluence page. You looked at the existing ADR numbering — ADR-001 through ADR-011. You were writing ADR-012.

```
ADR-012: Approval Policy Caching Strategy

Date: [Month 10, Week 3]
Status: DRAFT
Author: [Your name]
Reviewers: Elena Müller, Arjun Sharma

─────────────────────────────────────────

CONTEXT
────────
The Expense Service fetches approval policy 
from User & Org Service via FeignClient 
on every expense submission. Approval policy 
defines per-company rules for when manager 
and finance manager approval is required 
(e.g. amounts > €50 require manager approval; 
> €2,000 require manager then finance manager).

This policy changes infrequently — 
estimated once per quarter per company at most.
Despite this, as of [Month 10] the service 
was making ~340 FeignClient calls per minute 
to User & Org during peak load 
(Monday 9-10am CET), with P99 latency 
of 145ms per call.

User & Org Service CPU spiked from a 
baseline of 15-20% to 68-72% during 
these peaks, representing an unnecessary 
load caused by repetitive fetches 
of rarely-changing data.

─────────────────────────────────────────

DECISION
─────────
Cache approval policy in Redis using 
the Cache-Aside (Lazy Loading) pattern.

Key design choices:
  Cache key: "approval_policy:{companyId}"
  TTL: 15 minutes base + random 0-5 minutes 
       (jitter to prevent stampede on expiry)
  Serialization: JSON via 
                 GenericJackson2JsonRedisSerializer
  Mutex lock: Redis setIfAbsent (NX EX 10s)
              on cache miss to prevent 
              concurrent miss stampede
  Fallback: if Redis is unavailable,
            fall through to FeignClient —
            Redis is a performance optimization,
            not a hard dependency
  Invalidation: TTL-based (primary).
                Event-driven planned 
                (user.policy_updated Kafka event)
                when User & Org Service 
                adds publishing support.

─────────────────────────────────────────

ALTERNATIVES CONSIDERED
────────────────────────

1. Local cache only (Caffeine, no Redis)

   Pros:
   Zero network latency.
   No Redis operational dependency.
   
   Cons:
   Each service instance has its own cache.
   Cannot invalidate across instances 
   when policy changes without Redis Pub/Sub.
   Maximum staleness per instance = TTL,
   with no way to force immediate refresh.
   
   Rejected because:
   Policy changes (e.g. company lowers 
   approval threshold from €2,000 to €500) 
   must propagate to all instances within 
   a bounded window. Local-only caching 
   cannot guarantee this without additional 
   infrastructure (Redis Pub/Sub) — which 
   adds the same complexity anyway.

2. Redis + Caffeine (two-level caching)

   Pros:
   Caffeine provides sub-millisecond access.
   Redis provides cross-instance consistency.
   
   Cons:
   Requires Redis Pub/Sub to synchronize 
   Caffeine evictions across instances 
   when a key is invalidated in Redis.
   Adds meaningful implementation complexity.
   
   Rejected for initial implementation.
   Redis-only reduces FeignClient P99 
   from 145ms to 1-3ms — sufficient improvement.
   Two-level caching can be added later 
   if Redis latency becomes a measured bottleneck.

3. No TTL, event-driven invalidation only

   Pros:
   Cache is never stale — only invalidated 
   when policy genuinely changes.
   
   Cons:
   User & Org Service does not currently 
   publish user.policy_updated events.
   Timeline for this is ~2-3 months.
   Blocking on upstream team's roadmap 
   is not acceptable.
   
   Rejected: TTL-based invalidation is 
   the practical approach now. 
   Event-driven invalidation will be 
   layered on when available, 
   making TTL a safety net rather 
   than the primary mechanism.

4. Increase FeignClient timeout and 
   rely on User & Org Service scaling

   Pros:
   No caching complexity.
   
   Cons:
   Doesn't fix the unnecessary load.
   Scaling User & Org Service costs money.
   The data is the same on every call —
   caching is the architecturally 
   correct answer.
   
   Rejected: treats the symptom, 
   not the cause.

─────────────────────────────────────────

CONSEQUENCES
─────────────

Positive:
  FeignClient calls reduced from ~340/min 
  to ~44/min at peak (87% reduction).
  Expense submission P99 latency reduced 
  from ~165ms to ~23ms for cache hits.
  User & Org Service CPU returns to 
  baseline (19%) during peak.
  Redis failure degrades gracefully to 
  pre-caching behavior.

Negative:
  Approval policy data is stale for up 
  to 20 minutes after a policy change.
  This is acceptable for approval routing —
  a company changing thresholds mid-day 
  is unusual, and the window is bounded.
  
  Adds operational dependency on Redis 
  availability (mitigated by fallback).
  
  Mutex lock adds up to 250ms latency 
  for concurrent cache miss scenarios
  (rare in practice — P99 lock wait 
  observed at 142ms over 2-week period).

─────────────────────────────────────────

FUTURE CONSIDERATIONS
──────────────────────
When User & Org Service publishes 
user.policy_updated Kafka events:
  Add a consumer in Expense Service 
  that calls invalidateApprovalPolicy().
  TTL becomes safety net only.
  Effective staleness window drops to 
  near-zero for policy changes.

If Redis P99 latency rises above 10ms 
consistently (monitoring threshold):
  Revisit adding Caffeine L1 cache 
  with Redis Pub/Sub invalidation.

─────────────────────────────────────────

LINKS
──────
Related PR: EXP-271 (initial caching)
Related PR: EXP-276 (mutex lock fix)
Design doc: [Confluence link]
Datadog dashboard: [link]
```

You shared it in Slack with Elena and Arjun:

```
You (Slack DM to Elena and Arjun):
────────────────────────────────────
"ADR-012 draft ready for review 
 before I move it to the official folder.
 
 [Confluence link]
 
 First time writing one so let me 
 know if the format or depth 
 is off."
```

---

## Elena's Review

Elena responded the next morning with comments directly on the Confluence page.

Comment 1 — on the Context section:

```
Elena's comment:
─────────────────
"Good context. One addition:
 mention the business risk 
 of the status quo, not just 
 the technical cost.
 
 You describe User & Org CPU 
 spiking to 68-72%. That's a 
 technical observation.
 
 But WHY does that matter?
 
 A reader in 12 months might not 
 understand why high CPU on a 
 different service is our problem.
 
 Add one sentence: if User & Org 
 Service degrades under this load,
 approval policy lookups fail,
 expense submissions return 500 errors,
 and employees cannot submit expenses.
 
 Make the business risk explicit."
```

You updated the Context:

```
...representing an unnecessary load caused 
by repetitive fetches of rarely-changing data.
If User & Org Service were to degrade or 
fail under this sustained load, 
ApprovalPolicyService would return errors,
expense submissions would fail,
and employees would be unable to submit 
reimbursement claims.
```

Comment 2 — on the Consequences section:

```
Elena's comment:
─────────────────
"The 20-minute staleness consequence 
 is listed. Good.
 
 But add: what's the actual business 
 impact if staleness causes a problem?
 
 Not just 'policy data is stale.'
 What happens to an expense submitted 
 during the staleness window after 
 a policy change?"
```

You thought about this carefully before writing it:

```
You added:
───────────
If a company lowers their approval 
threshold (e.g. from €2,000 to €500) 
during the staleness window, expenses 
between €500-€2,000 submitted in 
that window will be routed as 
single-level approvals instead of 
two-level approvals.

These expenses will reach the 
approved state with only manager 
approval, bypassing the finance 
manager step that the company 
intended to require.

Mitigation: the company's finance 
team can identify and re-route 
any such expenses manually.
This is a compliance inconvenience,
not a data integrity issue —
no money moves without at least 
one approval level.
```

Comment 3 — on tone:

```
Elena's comment:
─────────────────
"One phrasing note throughout:
 ADRs are written in the past tense 
 for decisions already made 
 ('We decided X because Y')
 and future tense only for 
 things not yet done 
 ('When User & Org adds X, we will Y').
 
 A few places you've written 
 'is rejected' or 'is accepted' 
 (present tense).
 Small thing, but consistency 
 matters for readability."
```

You did a pass through the document and updated tense throughout.

Arjun left one comment:

```
Arjun:
───────
"The mutex lock section in DECISION 
 is a bit dense for someone who 
 hasn't seen the stampede problem before.
 
 Add one sentence of context before 
 the technical detail:
 
 'To prevent multiple concurrent requests 
 from calling FeignClient simultaneously 
 on cache miss (thundering herd), 
 a distributed mutex lock is acquired 
 using Redis setIfAbsent before 
 fetching from upstream.'
 
 Then the technical detail makes sense 
 to someone reading this fresh."
```

You added it.

---

## The Final Version — What Changed

The final ADR was about 20% longer than your first draft. Not because Elena had asked for more — but because the changes she requested revealed gaps you hadn't noticed:

```
What changed from draft to final:
───────────────────────────────────

1. Context: added business risk 
   (not just technical observation)
   
2. Consequences (negative): 
   explained what staleness actually 
   means for expenses in the window
   
3. Mutex lock explanation:
   added plain-English context 
   before technical detail
   (Arjun's suggestion)
   
4. Tense consistency throughout

What DIDN'T change:
────────────────────
The decisions themselves.
The alternatives.
The reasoning.
All of that was right.

The changes were about clarity 
and completeness —
making sure a reader six months 
from now could understand not just 
what was decided but why it matters.
```

Elena approved it in Confluence:

```
Elena (Confluence comment):
────────────────────────────
"Good ADR. Approved.
 Move to the official folder.
 
 One reflection for you:
 The hardest part of writing 
 an ADR is the consequences section.
 Most people list the positives 
 and write one vague line about 
 tradeoffs.
 
 You listed specific, honest negatives —
 the 20-minute staleness window,
 the business impact if that 
 window catches a policy change,
 the 250ms worst-case lock wait.
 
 That's what makes an ADR useful.
 A document that only records 
 why something was a good idea 
 is not a record — it's marketing.
 
 A document that records the 
 real tradeoffs lets the team 
 revisit the decision with 
 full information when conditions change."
```

You moved it to the official folder. ADR-012 was in the repository.

---

## What Happened Six Weeks Later

You didn't expect this part.

Six weeks after you wrote ADR-012, a new engineer joined the Accounting Integration team — Miguel, mid-level. He was building a feature that would also need to cache approval policy data for his service's use case.

He found ADR-012 in the repository. He read it. He sent you a Slack message:

```
Miguel (Slack DM):
───────────────────
"Hey — I'm Miguel, just joined 
 Accounting Integration team.
 
 I'm working on something that 
 needs to cache approval policy 
 and found your ADR.
 
 Quick question: the ADR says 
 you considered Caffeine + Redis 
 and rejected it for initial impl.
 
 Our service doesn't have the same 
 concurrent load profile — we run 
 batch jobs, not real-time per-request.
 For batch, a local Caffeine cache 
 might actually be sufficient.
 
 Did your team document any guidance 
 on when Caffeine-only is appropriate?"
```

```
This was not a question you had 
anticipated.

But you had an answer.
```

```
You (reply):
─────────────
"Good question — and you're right 
 that batch vs real-time changes 
 the calculus.
 
 The main reason we used Redis 
 was cross-instance invalidation.
 If policy changes, we need all 
 running instances to clear 
 their cache within a bounded window.
 
 For batch jobs running on a 
 single instance (or where 
 each job run re-creates the 
 JVM), local Caffeine is fine —
 each run starts fresh anyway.
 
 If your batch jobs are long-running 
 processes that need to handle 
 policy changes mid-run,
 that changes things.
 
 Two questions that would tell you:
 1. How long does your batch job run?
 2. Does a policy change during 
    a batch run need to be reflected 
    immediately, or can it wait 
    for the next run?
 
 If the job runs < 15 minutes and 
 policy change can wait for next run:
 Caffeine with 15-minute TTL is fine.
 
 If the job runs hours or needs 
 immediate reflection:
 Use Redis — same pattern as ADR-012."
```

Miguel:

```
Miguel:
────────
"Job runs 5-10 minutes.
 Policy change can absolutely 
 wait for next run.
 Caffeine it is.
 
 Thanks — the ADR saved me 
 from over-engineering this."
```

```
You closed Slack and sat with 
this for a moment.

Six weeks ago you wrote a document 
because Elena asked you to.
You didn't think much about 
who would read it or whether 
it would matter.

Today it saved someone from 
making a more complex decision 
than they needed to.

That's what permanent documentation 
does.
It's not about the moment 
you write it.
It's about the moments you 
can't predict, six weeks or 
six months later, when 
someone needs the reasoning 
and you're not in the room.
```

---

## The Conversation With Lukas — Performance Review Mention

At the end of month 10, you had your quarterly 1:1 with Lukas. He went through several things, including the caching work. Near the end:

```
Lukas:
───────
"One thing I've noticed this quarter
 that I want to name:
 
 The design doc before the meeting.
 The ADR after the implementation.
 The load test script in the repo.
 
 You've been leaving artifacts behind —
 things that make the work 
 understandable and reproducible 
 for the next person.
 
 Junior engineers often don't do this.
 They focus on shipping the feature 
 and move on.
 
 You're thinking about 
 who comes after you.
 That's a mid-level habit.
 
 Keep doing it."
```

```
Mid-level habit.

You had heard Lukas use this framing 
before — 'L2 behavior,' 
'mid-level habit.'
He was mapping what you were doing 
to where the team needed you to grow.

You were still L1 on paper.
But the things Lukas was pointing to —
design docs, ADRs, load test scripts,
artifacts that outlast the feature —
these were being noticed.

Not as a path to promotion.
You weren't thinking about that.

But as evidence that your 
understanding of the work 
was deepening.

Not just 'I built it.'
'I built it, I explained why, 
I documented the tradeoffs,
I made it testable, 
I made it understandable.'

Those are different things.
```

---

## What Writing the ADR Taught You About Decisions

```
Before writing ADR-012:
────────────────────────
You made a decision and moved on.
The reasoning lived in your head,
in Slack messages, in PR descriptions.
Ephemeral. Searchable only if 
you knew exactly what to search for.

After writing ADR-012:
──────────────────────
You discovered something 
about your own decision-making.

Writing the alternatives section 
forced you to articulate WHY 
you rejected each option.
Not just "we didn't use Caffeine,"
but specifically:
"because cross-instance invalidation
 without Redis Pub/Sub means 
 policy changes don't propagate 
 bounded within an acceptable window."

That sentence didn't exist in 
your head as a complete thought 
before you wrote it.
It became complete by being written.

This is what good documentation does.
It doesn't just record thinking.
It completes thinking.

The act of writing "why we rejected X"
forces you to verify that you 
actually know why.
If you can't write it clearly,
you don't know it clearly.

Elena's comment — 
"a document that only records 
why something was a good idea 
is not a record, it's marketing" —
was the clearest definition 
of honest technical writing 
you had encountered.

You kept it.
```

---

## The "Tricky Question" Preparation

---

**Q1: "What is an ADR and why does it matter?"**

```
ADR stands for Architecture Decision Record.
It's a short document that records 
a specific architectural or design decision:
what was decided, what alternatives 
were considered, why the chosen option 
was selected, and what the consequences are.

Why it matters:

Systems accumulate decisions over time.
A year from now, someone reading the 
codebase will see a Redis mutex lock 
in the approval policy service.
They might wonder: why is this here?
Why not Caffeine? Why not a longer TTL?
Why a 10-second lock TTL and not 30?

Without an ADR, the answer is 
"go ask whoever built it" — if 
they're still on the team and remember.

With an ADR, the answer is 
"read document 012."

This matters especially at Series B 
where teams are growing quickly.
New engineers join every quarter.
Context that lives only in the 
original team's memory erodes 
every time someone leaves.

ADRs make the reasoning permanent,
independent of team composition.

The format is minimal by design —
context, decision, alternatives, 
consequences.
Not a research paper. 
Not a justification of the decision.
An honest record of what was decided 
and why, including the tradeoffs accepted.
```

---

**Q2: "You wrote this after the implementation. Should ADRs be written before or after?"**

```
Ideally, during or just after —
not months later.

The reasoning is freshest when 
the decision is being made or 
just was made.
Alternatives you considered are 
still in your head.
The context that made Option A 
better than Option B is clear.

Writing six months later means 
reconstructing context that 
may have shifted.
You might remember the decision 
but not the specific constraint 
that made Option B unworkable.

In practice at a Series B startup:
you rarely write the ADR first 
because decisions happen fast,
often in meetings or Slack.
The practical pattern is:
make the decision in the design doc 
or the meeting,
implement it,
write the ADR immediately after —
while the reasoning is still fresh.

That's what I did.
I had a design doc from the 
meeting prep which contained 
most of the reasoning.
The ADR organized it permanently.
The design doc was a working document.
The ADR is the permanent record.

If someone asks "why didn't 
you write it before?" — 
the honest answer is: 
writing it after is better 
than not writing it at all,
and waiting for perfection 
means it never gets written.
```

---

**Q3: "Elena said 'a document that only records why something was a good idea is not a record, it's marketing.' What did that mean to you?"**

```
It meant that the value of an ADR 
is in the tradeoffs, not the decision.

A document that only says 
"we chose Redis and it was great, 
here's why it was great" 
is useless to the next engineer 
who needs to decide whether to 
revisit that decision.

They can't tell:
What constraints made Caffeine wrong?
Under what conditions would we 
add Caffeine later?
What's the actual downside of 
the approach we chose?

If I write the ADR honestly —
including "policy data can be stale 
for up to 20 minutes, and here's 
exactly what that means for 
expenses submitted in that window" —
then a future engineer can look at 
their context and ask: 
"are those tradeoffs still acceptable?"

If the answer is no — maybe 
the company now has compliance 
requirements that make 20-minute 
staleness unacceptable — 
the ADR tells them exactly 
where the design needs to change.

Marketing documents lead to 
sacred cows — decisions that 
nobody questions because they 
only remember the positive case.

Honest records lead to 
informed decisions — 
revisiting when conditions change,
with the original reasoning intact 
as context not as mandate.

That's why the consequences section 
is the most important part of an ADR.
And why it's the hardest to write.
```

---

**Q4: "Miguel found your ADR six weeks later and it saved him from over-engineering. Is that a realistic outcome or are you overstating it?"**

```
I don't think I'm overstating it,
but I want to be precise about 
what actually happened.

Miguel was building caching for 
a batch job. He found ADR-012 
in the repository. The ADR recorded 
that we had considered Caffeine-only 
and rejected it specifically because 
of cross-instance invalidation 
requirements for real-time workloads.

His workload was batch — not real-time.
The constraint that made us reject 
Caffeine didn't apply to him.

Without the ADR, he might have:
1. Used Redis anyway (correct but 
   over-engineered for his case), or
2. Asked me directly (fine, but 
   I might not have been available 
   or remembered the exact reasoning)

With the ADR, he could:
1. Read the specific constraint 
   that made us choose Redis
2. Evaluate whether that constraint 
   applied to his context
3. Correctly determine it didn't
4. Use Caffeine without guilt

He said it himself: 
"the ADR saved me from 
over-engineering this."

I can't claim I predicted that outcome 
when I wrote it. I didn't know 
Miguel would join six weeks later 
with that specific question.

But that's the nature of documentation —
you can't predict who will read it 
or when. You write it precisely 
and honestly, and it becomes useful 
in ways you couldn't anticipate.

That's not overstating it.
That's what good documentation 
is supposed to do.
```

---

Block 4 complete. All four stories done.

```
BLOCK 4 SUMMARY — 
"Trusted Contributor" (Months 10-12):
──────────────────────────────────────────

Story 12 (Caching proposal):
  First time you spoke up architecturally 
  in a team design meeting.
  Came with data, not just opinion.
  Proposed Cache-Aside + Redis.
  Owned the implementation.
  87% reduction in FeignClient calls.

Story 13 (Cache stampede fix):
  Found a production risk nobody 
  had caught — including the 
  senior engineer who reviewed 
  your design.
  Solved it independently.
  Brought it to the team already fixed.
  Became a team norm 
  (concurrent load testing 
  for caching work).

Story 14 (Teaching Léa):
  First time someone came to you 
  specifically for help.
  Explained @PreAuthorize deeply —
  the three-piece chain,
  the proxy limitation,
  defense in depth.
  Made it safe to ask follow-up questions.
  Three days later, Léa answered 
  Kemal's question before you did.

Story 15 (ADR):
  First permanent architectural document.
  Recorded what was decided and why —
  including honest tradeoffs.
  Elena's comment: honest record, 
  not marketing.
  Six weeks later: helped a new engineer 
  from another team make a better 
  decision without asking anyone.

The arc across Block 4:
  Story 12: you proposed to the team.
  Story 13: you found before the team.
  Story 14: the team came to you.
  Story 15: you left something behind 
            for the team you hadn't met yet.

  That's "Trusted Contributor" — 
  not a title, not a promotion.
  A mode of working where your 
  contribution extends beyond 
  the ticket you're assigned.
```
