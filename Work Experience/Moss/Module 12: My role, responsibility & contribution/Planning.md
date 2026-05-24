This is exactly the right approach — plan the arc first, then dive deep into each block.

Let me think about this carefully before proposing the breakdown.

---

## The Core Principle of This Planning Step

```
The 18-month arc must feel like a 
REAL person's growth journey — not a 
polished LinkedIn highlight reel.

Real growth looks like:
────────────────────────
- Slow start (confusion, small wins)
- First real mistake (embarrassing but 
  educational)
- Gradual confidence building
- A breakthrough moment
- Taking on more, sometimes failing
- Eventually becoming someone others 
  lean on slightly
- Never fully "arriving" — always 
  something new to learn

NOT like:
──────────
- Month 1: junior
- Month 18: senior
- Linear upward graph
- Every story has a happy ending
- No conflicts, no failures, 
  no self-doubt
```

---

## Dimensions to Plan Per Block

Before writing the blocks, let me establish **what we need to decide** for each 3-month period:

```
1. TECHNICAL WORK
─────────────────
- What type of tasks? (bug fixes, 
  features, refactoring, performance, 
  architecture contribution)
- Which Spring Boot / system concepts 
  from your notes are touched?
- Difficulty level (1-5 scale)
- Which service? (Expense vs Invoice/AP)
- Any new tech introduced this block?

2. COLLABORATION DYNAMICS
──────────────────────────
- Who mentors you this block?
- Who do you pair-program with?
- Who reviews your code?
- Do you start reviewing others?
- Any cross-team interactions?
- Stakeholder interactions? (PM, QA)

3. MISTAKES & LEARNING
────────────────────────
- What went wrong? (technical or soft-skill)
- How was it discovered?
- Who helped you resolve it?
- What did you change after?

4. SOFT-SKILL GROWTH
─────────────────────
- Communication style evolving?
- Asking better questions over time?
- Handling feedback differently?
- Any conflict or tension?
- Taking initiative moments?

5. EMOTIONAL ARC
─────────────────
- How are you FEELING this block?
- Overwhelmed? Gaining confidence?
- Frustration? Excitement?
- Imposter syndrome moments?
- A pride moment?

6. MEASURABLE OUTCOMES
────────────────────────
- What shipped?
- What improved? (backed by metrics)
- What recognition, if any?
```

---

## Proposed 18-Month Arc — Block by Block

```
BLOCK 1 (Months 1-3): "Finding My Feet"
─────────────────────────────────────────
Theme: Survival, orientation, first wins

Technical level:    1/5 — very simple
Service:            Expense Service only
New tech:           Nothing new — 
                    applying Spring Boot 
                    knowledge from courses 
                    to real codebase
Work type:          Bug fixes, small 
                    validation features,
                    reading existing code,
                    writing unit tests

Key events:
- Week 1-2: overwhelming onboarding
  (Docker Compose, real codebase vs 
   tutorial codebase is shocking)
- First ticket: validation fix 
  (8 PR comments from Elena, 
   feels like a lot)
- First mistake: push to main directly
  (branch protection saves you)
- Imposter syndrome peak
- First sprint demo (nervous, but did it)
- Helping Marta onboard at month 3 
  (she joined after you — 
   first time you "know more" than someone)

Mentorship:         Elena (heavy — 
                    weekly tech syncs,
                    detailed PR comments),
                    Tomás (day-to-day 
                    codebase navigation)

Emotional arc:      Overwhelmed → 
                    slowly stabilizing

Measurable outcome: 3 small features 
                    shipped, 0 production bugs

─────────────────────────────────────────
BLOCK 2 (Months 4-6): "Building Confidence"
─────────────────────────────────────────
Theme: First real ownership, first real mistake

Technical level:    2/5
Service:            Expense Service (primary),
                    first tiny touch of 
                    Invoice Service
New tech:           Deeper JPA 
                    (complex queries, 
                     Specification API),
                    Spring Security 
                    (@PreAuthorize, roles)
Work type:          Feature ownership 
                    end-to-end (first time),
                    fixing an N+1 problem 
                    (with Finn's help),
                    writing integration tests
                    with Testcontainers

Key events:
- First feature owned end-to-end:
  multi-level approval workflow 
  for expenses > €2000
  (touches controller, service, 
   repository, DB migration, tests)
- N+1 bug discovered in production
  (Finn helps debug via Datadog APM)
  → this becomes your first 
    performance improvement story
- @Transactional on private method mistake
  (Elena catches in code review, 
   you add SonarQube rule after)
- First time you push back on a PM's 
  request in sprint planning 
  (politely, technically justified)
- Start reviewing Kemal's PRs 
  (first time on the other side)

Mentorship:         Elena (less frequent — 
                    bi-weekly now),
                    Finn (query optimization,
                    informal),
                    Arjun (starts appearing —
                    you ask him about 
                    distributed systems 
                    in general)

Emotional arc:      Gaining confidence → 
                    setback with N+1 bug →
                    recovered stronger

Measurable outcome: N+1 fix — endpoint 
                    800ms → 45ms
                    (measured via Datadog)

─────────────────────────────────────────
BLOCK 3 (Months 7-9): "Expanding Horizon"
─────────────────────────────────────────
Theme: First Kafka exposure, cross-service work

Technical level:    3/5
Service:            Expense Service + 
                    first real Invoice 
                    Service work
New tech:           Kafka consumers 
                    (reading/understanding 
                     existing consumers first,
                     then writing your first 
                     consumer — payment.completed
                     → mark reimbursement done),
                    RestClient 
                    (replacing a RestTemplate 
                     call — maintenance mode),
                    Basic Redis 
                    (reading existing cached 
                     data, not designing yet)

Work type:          Kafka consumer implementation,
                    cross-service debugging 
                    (trace across services 
                     using Jaeger for first time),
                    first Invoice Service feature
                    (invoice status tracking 
                     page — simple CRUD + 
                     state machine validation)

Key events:
- Arjun introduces you to Kafka properly
  (pair programming sessions — 
   you shadow his work first,
   then implement your own consumer)
- First production incident you're 
  involved in: Kafka consumer lag spike
  (you're not the one who fixes it,
   but you're in the war room,
   watching Arjun debug with Datadog)
- First cross-team interaction:
  Payment Service team needs to change
  the payment.completed event schema —
  you represent your team in that discussion
  (with Arjun's guidance)
- Conflict moment: you and Tomás disagree 
  on implementation approach for a feature
  (Elena mediates, you learn to 
   argue technically, not personally)
- Using Jaeger for the first time to 
  debug a broken trace (RestClient.create() 
  vs RestClient.Builder mistake)

Mentorship:         Arjun (primary for Kafka),
                    Priya (Redis basics — 
                    you shadow her work),
                    Elena (architecture 
                    decisions on Invoice Service)

Emotional arc:      Excited by new complexity →
                    humbled by incident →
                    proud of cross-team 
                    interaction

Measurable outcome: payment.completed consumer
                    shipped with 0 bugs,
                    reimbursement processing 
                    now fully event-driven

─────────────────────────────────────────
BLOCK 4 (Months 10-12): "Trusted Contributor"
─────────────────────────────────────────
Theme: Redis ownership, outbox pattern, 
       mentoring others

Technical level:    3.5/5
Service:            Invoice Service (primary),
                    Expense Service (maintenance)
New tech:           Redis caching 
                    (Cache-Aside pattern,
                     TTL design, 
                     cache invalidation via 
                     Kafka event),
                    Outbox Pattern 
                    (understanding + 
                     implementing for 
                     Invoice Service),
                    Custom Micrometer metrics
                    (building expense.approval
                    .duration timer)

Work type:          Designing and implementing 
                    approval policy caching 
                    (Priya mentors, 
                    you implement),
                    Outbox poller implementation
                    for Invoice Service,
                    Building custom Datadog 
                    dashboard panels,
                    Mentoring Léa on 
                    Spring Boot concepts

Key events:
- First time you propose a technical 
  solution in a team design discussion
  (caching strategy for approval policy —
   Elena asks for your input,
   you present Cache-Aside + Redis,
   team accepts with minor changes)
- Cache stampede bug in staging 
  (you discover it, research the fix,
   implement mutex lock solution —
   first time you solve something 
   without asking for help first)
- Léa comes to you with Spring 
  Security confusion — you explain
  @PreAuthorize and feel proud 
  that you can teach it clearly
- 6-month performance review 
  (Lukas says you're performing at 
   the L1-L2 boundary — 
   first formal recognition)
- Writing your first ADR 
  (Architecture Decision Record) 
  for the caching approach

Mentorship:         Priya (Redis — 
                    you're now implementing
                    what she designed),
                    Elena (ADR review),
                    You → Léa (reverse 
                    mentorship begins)

Emotional arc:      Quiet confidence →
                    excited to be trusted →
                    proud teaching moment

Measurable outcome: Cache hit rate 87% 
                    for approval policy,
                    FeignClient calls to 
                    User & Org Service 
                    reduced by ~85%
                    (measured via Datadog)

─────────────────────────────────────────
BLOCK 5 (Months 13-15): "Feature Ownership"
─────────────────────────────────────────
Theme: End-to-end ownership of a 
       significant feature, production incident

Technical level:    4/5
Service:            Invoice Service (primary)
New tech:           Kafka producer 
                    (writing your first 
                     Kafka producer + 
                     outbox together),
                    Dead Letter Queue 
                    (implementing DLQ 
                     for invoice.approved 
                     consumer in downstream),
                    Transaction handling 
                    edge cases 
                    (@Async + @Transactional,
                    AFTER_COMMIT listener)

Work type:          Owning invoice verification
                    feature end-to-end 
                    (full flow: upload → OCR → 
                     verify → approve → 
                     payment run),
                    Production incident 
                    ownership 
                    (you lead the investigation,
                     Arjun supports),
                    Writing runbook for 
                    outbox poller failure,
                    Contributing to sprint 
                    planning estimation 
                    more actively

Key events:
- First production incident YOU own:
  outbox events piling up — 
  invoice.approved events not publishing.
  You investigate using Datadog 
  (outbox health indicator DOWN),
  find the root cause 
  (DB connection leak in outbox poller),
  fix it, write the postmortem.
  Arjun reviews your postmortem.
  Team adopts your fix.
- Invoice verification feature: 
  PM asks for a feature that would 
  require breaking DB schema change.
  You flag it in sprint planning,
  propose a backward-compatible 
  migration approach,
  PM agrees after you explain the risk.
  First time you influence product 
  decisions with technical reasoning.
- You start being the person 
  who onboards new people to 
  the Invoice Service codebase
  (Kemal moves to helping on 
   Invoice Service)

Mentorship:         Arjun (incident support),
                    Elena (postmortem review),
                    You → Kemal (Invoice 
                    Service onboarding)

Emotional arc:      High pressure during 
                    incident → 
                    relief after resolution →
                    pride in ownership →
                    growing into a 
                    team resource

Measurable outcome: Outbox incident resolved,
                    0 recurrence after fix,
                    invoice verification 
                    feature shipped on time,
                    postmortem adopted 
                    as team template

─────────────────────────────────────────
BLOCK 6 (Months 16-18): "Growing Into the Role"
─────────────────────────────────────────
Theme: Broader contributions, 
       approaching L2 boundary,
       reflection on growth

Technical level:    4/5
Service:            Both (comfortable 
                    across both now)
New tech:           Distributed tracing 
                    deeper dive 
                    (manual spans for 
                     OCR and outbox poller),
                    Custom Actuator endpoint
                    (for outbox health),
                    Performance profiling 
                    workflow solidified

Work type:          Performance improvement 
                    initiative 
                    (you propose + lead 
                     a cross-cutting 
                     latency investigation),
                    Custom monitoring 
                    dashboard contribution,
                    Code review quality 
                    improves significantly 
                    (Elena notices),
                    Contributing meaningfully 
                    to architectural 
                    discussions

Key events:
- You notice (via Datadog) that 
  invoice list endpoint p99 is 
  creeping up over 3 weeks.
  Nobody assigned this to you.
  You investigate proactively,
  find a missing composite index,
  add migration, p99 drops 60%.
  This is your "initiative" story.
- Final 1:1 with Lukas:
  "You're operating at L2. 
   We'd promote you if you were 
   full-time, not contract."
  That's your growth validation.
- You write a wiki page on 
  "How to debug Kafka consumer lag 
   in our system" — 
  becomes team reference document.
- Léa thanks you publicly in Slack 
  for helping her navigate 
  her first production bug.
  Small moment, but means a lot.
- Honest self-reflection: 
  you still have gaps 
  (Kafka internals deeply, 
   system design at scale) —
  you know what to learn next.

Mentorship:         You → Léa, Kemal 
                    (now clearly a mentor),
                    Elena (peer-level 
                    technical conversations 
                    starting),
                    Arjun (still learning 
                    from him on distributed 
                    systems)

Emotional arc:      Settled, confident,
                    clear on what you 
                    know and don't know,
                    genuinely proud of 
                    the journey

Measurable outcome: Invoice list endpoint
                    p99 reduced 60% 
                    (measured via Datadog),
                    team wiki contribution,
                    approaching L2 boundary
```

---

## The Emotional Arc Across All 18 Months

```
Month 1-3:   😰 Overwhelmed but pushing through
Month 4-6:   📈 Growing, first real mistake, 
                recovering
Month 7-9:   🌱 Expanding, humbled by 
                complexity, excited
Month 10-12: 💪 Quiet confidence, 
                first leadership moments
Month 13-15: 🔥 High pressure, real ownership,
                proving yourself
Month 16-18: 🎯 Settled, clear-eyed, 
                genuinely capable
```

---

## Key Relationships Arc

```
ELENA (Tech Lead):
Month 1-3:   Teacher → Student (heavy)
Month 4-9:   Mentor → Mentee (lighter)
Month 10-15: Reviewer → Contributor
Month 16-18: Near peer-level technical 
             conversations

ARJUN (Senior):
Month 1-6:   Aware of him, not working closely
Month 7-9:   Kafka mentor (pair programming)
Month 10-15: Go-to for distributed systems,
             incident support
Month 16-18: Mutual respect, you consult him
             on design, he values your input

TOMÁS (Mid-level peer):
Month 1-3:   Your guide to the codebase
Month 7:     Conflict over implementation
Month 8+:    Resolved, good working relationship

KEMAL (Junior peer):
Month 4-6:   Peer learning together
Month 13-18: You mentor him on Invoice Service

LÉA (Junior):
Month 4-6:   You help her with Spring concepts
Month 16-18: She thanks you publicly — 
             full circle moment

LUKAS (EM):
Month 1-18:  Steady career guidance,
             quarterly feedback,
             final L2 acknowledgment
```

---

## STAR Stories We'll Write Per Block

```
BLOCK 1: 
  - First PR experience (feedback handling)
  - Onboarding challenge (codebase vs tutorials)
  - Helping Marta onboard (reverse teaching)

BLOCK 2:
  - N+1 bug discovery and fix 
    (performance story with metrics)
  - Multi-level approval feature ownership
    (first end-to-end feature story)
  - @Transactional private method mistake
    (learning/mistake story)
  - Pushing back on PM request
    (communication/conflict story)

BLOCK 3:
  - Kafka consumer implementation 
    (new tech ramp-up story)
  - Production incident war room 
    (observability story)
  - Cross-team schema discussion
    (stakeholder/collaboration story)
  - Tomás conflict + resolution
    (conflict handling story)

BLOCK 4:
  - Caching proposal in design discussion
    (leadership/initiative story)
  - Cache stampede discovery and fix
    (problem-solving story)
  - Teaching Léa @PreAuthorize
    (mentorship story)
  - First ADR contribution
    (ownership/documentation story)

BLOCK 5:
  - Production incident ownership
    (leadership under pressure story)
  - Backward-compatible migration proposal
    (technical influence story)
  - DLQ implementation
    (resilience engineering story)

BLOCK 6:
  - Proactive latency investigation
    (initiative story — nobody asked you)
  - Wiki documentation 
    (team contribution story)
  - Lukas's L2 acknowledgment
    (growth validation story)
  - Léa's public thanks
    (impact/mentorship story)
```

---