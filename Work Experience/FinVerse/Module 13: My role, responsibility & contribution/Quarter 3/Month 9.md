# Month 9: Earning Your Place in the Room

---

## Foundational Knowledge: What You Need Before These Stories

Month 9 has three threads: a cross-team technical contribution, a first experience presenting to leadership, and a code review that completes a cycle you started in Month 7. Before the stories, one concept and one framing are worth establishing.

---

### Concept 1: What "Presenting to Leadership" Actually Requires in an Engineering Context

At some point in any engineer's career, technical work has to be communicated to people who are not engineers. This is not about dumbing things down. It is about understanding what question the audience is actually asking — which is rarely the same question engineers ask each other.

When Sophie, the VP of Engineering, asks about the Accounts & Open Banking module in the context of Series B preparation, she is not asking "how does the GoCardless integration work?" She is asking something closer to: "can this module support 1.5 million users, and if not, what would it take?"

The difference shapes everything about how you present.

```
TWO WAYS TO FRAME THE SAME TECHNICAL REALITY

ENGINEER-TO-ENGINEER FRAMING:
  "The sync worker runs with concurrency 10 on 1 vCPU
   containers. At 1.5M users with 4-hour periodic syncs,
   we would need approximately 104 containers to drain the
   queue within the 4-hour window, assuming GoCardless rate
   limits stay constant. The current ECS auto-scaling maximum
   is 10 containers, which would handle approximately 140k
   users before we see significant queue backlog."

ENGINEER-TO-LEADERSHIP FRAMING:
  "The sync pipeline currently handles up to 140k active users
   comfortably within our 4-hour sync window. Scaling to 1.5M
   users requires two investments: increasing our ECS scaling
   ceiling and renegotiating our GoCardless rate limit tier,
   which we would do as part of the commercial upgrade at
   Series B. We have a clear path. The architecture does not
   need fundamental redesign."
```

Both framings are accurate. The first answers "how does it work." The second answers "can we scale it and what does it cost." Only the second is what leadership needs to make a funding decision.

Lucas teaches you this distinction in the week before the presentation — not by lecturing, but by reviewing your draft slides and asking one question that reframes everything: "does this slide answer the question Sophie is actually asking?"

---

### Concept 2: Why Code Review Is a Compounding Investment

In Month 7, you reviewed Priya's first PR and wrote structured comments that explained reasoning rather than just flagging issues. In Month 9, you see the result of that investment in a specific, observable way.

The compounding effect of good code review works like this:

```
HOW KNOWLEDGE TRANSFERS THROUGH CODE REVIEW

Month 7:
  You catch a missing composite index in Priya's PR.
  You explain WHY composite indexes matter — not just
  that one is missing. You reference the access pattern,
  the query plan implication, the scale consequence.

Month 8:
  Priya opens her second PR. She added composite indexes
  on every multi-condition query. No prompting needed.

Month 9:
  Priya opens her third PR. She reviews Arjun's Goals PR
  as a secondary reviewer. She catches a missing composite
  index in his code and leaves a structured comment
  explaining the reasoning.

  The knowledge has now propagated:
  Lucas → You (Month 2, ownership check pattern)
  You → Priya (Month 7, composite index pattern)
  Priya → Arjun (Month 9, composite index pattern)
```

This is not unusual. It is how knowledge moves through well-functioning engineering teams. Good review comments are not just about the PR in front of you — they are investments in the reviewer's ability to produce better code on the next PR, and the one after that.

Month 9 is where you see your Month 7 investment return.

---

## The Stories

---

### Story 1: Contributing to the Series B Technical Preparation

**Background:**

In the third week of Month 9, Sophie runs a two-hour engineering-wide session on technical readiness for Series B. The framing she gives in the invitation: "we are preparing materials for investors and for the Series B hiring push. I want each team lead to present the current state of their domain and what the path to 1.5M users looks like."

Lucas is presenting for Core Product as a whole. He divides the module coverage across the team — Elena takes Investing, Tomasz takes Transactions, and Lucas himself covers the overall architecture. He assigns you two slides covering the Accounts & Open Banking module: current sync reliability metrics and the path to scale.

This is the first time you are asked to prepare something for a company-wide forum involving leadership.

---

**S — Situation:**

It is Monday of the third week. The presentation is Friday. You have four days. Lucas asks you to share a draft by Wednesday so he can review before you finalise.

You know your module better than anyone. You know the sync pipeline inside out after eight months of ownership. What you do not know yet is how to frame that knowledge for Sophie and for the broader engineering audience.

---

**T — Task:**

Prepare two slides that answer the question Sophie is actually asking — can this module support 1.5M users, and what would it take — using real numbers from Datadog, and frame it as a plan rather than a problem list.

---

**A — Action:**

**Building the content — starting with the metrics:**

You open Datadog and spend an hour pulling the numbers that matter. Not all the numbers — the ones that directly answer "is this reliable today and can it scale?"

```
METRICS PULLED FROM DATADOG
(30-day window, production environment)

Sync Reliability:
  INITIAL_SYNC success rate:     99.2%
  PERIODIC_SYNC success rate:    99.7%
  p50 sync duration:             6.1 seconds
  p95 sync duration:             11.2 seconds
  p99 sync duration:             18.7 seconds

Scale Capacity:
  Current active users:          ~180,000 MAU
  Peak queue depth (08:00 sync): ~1,800 jobs
  Peak container count (ECS):    8 containers
  GoCardless rate limit tier:    Standard (50 req/s per API key)
  Current utilisation at peak:   ~34% of rate limit budget

Series B Target: 1.5M registered users
  Estimated MAU at 1.5M registered: ~600,000 (40% activation rate)
  Required queue processing:     ~6,000 jobs per 4-hour window
  Required container count:      ~67 at current concurrency settings
  Current ECS auto-scaling max:  10 containers
  Gap:                           57 containers (needs config change)
  GoCardless rate limit at scale: would need upgraded tier
```

**The first draft — the mistake:**

Your first draft of slide 1 is titled "Current Limitations." It lists three things: the ECS scaling ceiling, the GoCardless rate limit tier, and the PostgreSQL replica lag that caused the race condition in Month 8. All three are accurately described. All three frame the module as something with problems.

You send it to Lucas on Tuesday evening.

**Lucas's review — the reframe:**

Lucas responds Wednesday morning with a voice note rather than written comments — unusual for him, which tells you he has something more substantive to say than a line edit.

The voice note is three minutes. The substance of it:

"The numbers are right. The framing is wrong. 'Current Limitations' is an answer to the question 'what is broken.' Sophie is asking 'can we scale this.' Those are different questions. A limitations list makes her think the module is fragile. A plan makes her think the team is ready. The difference is not spin — it is accuracy. The module is not fragile. It has a clear path. Show her the path."

He also says one other thing: "you mention the replica lag incident. That is already fixed. Do not present a solved problem as an open issue. Present it as evidence that the team identifies and resolves issues quickly — if you present it at all."

You sit with the feedback for an hour. Then you rewrite the slides entirely.

**The revised slides:**

Slide 1: "Sync Pipeline — Current Baseline"

Not "limitations." Baseline. The slide shows the Datadog numbers — p95 at 11.2 seconds, 99.7% success rate on periodic syncs, peak queue depth of 1,800 jobs handled cleanly by auto-scaling. One sentence at the bottom: "The pipeline has been stable for 6 months. The race condition identified in Month 8 was resolved within the same sprint it was found."

Slide 2: "Path to 1.5M Users — Two Investments"

Not a problems list. Two investments with clear owners and clear timelines:

```
INVESTMENT 1: ECS Auto-Scaling Ceiling
  Current maximum: 10 containers
  Required at 1.5M users: 67 containers (based on current concurrency)
  Action: Update ECS service configuration
  Owner: Platform & DevOps
  Timeline: 1 day of work, can be done before Series B closes
  Cost implication: ~€4,200/month additional ECS cost at peak
                    (scales down automatically off-peak)

INVESTMENT 2: GoCardless Rate Limit Tier Upgrade
  Current tier: Standard (50 req/s per API key)
  Required at 1.5M users: ~200 req/s
  Action: Commercial tier upgrade with GoCardless (enterprise agreement)
  Owner: Business/Commercial team + Platform
  Timeline: 2-4 weeks negotiation lead time
  Cost implication: part of Series B commercial budget
```

At the bottom of slide 2: "No architectural redesign required. The current system design scales horizontally. The investments are operational and commercial, not technical."

That last line is what Sophie needs to hear. The architecture is sound. The gap is configuration and commercial, not engineering debt.

**The presentation:**

Friday afternoon. You present for eight minutes. The room is twelve people — the full engineering team plus Sophie and two PMs.

You walk through the baseline metrics first. When you say "p95 sync duration of 11.2 seconds," Sophie asks: "what does that mean in terms of user experience?" You are ready for this: "a user who connects their bank account sees their first transactions appearing in the app within about 15 seconds on average. The 11 seconds is the sync worker time — the app shows a loading state during this period so users see progress rather than a blank screen."

She nods. You continue to slide 2. When you present the two investments, she asks: "the ECS ceiling — is that a blocker or just a dial we turn?" You say: "a dial. The configuration change takes less than a day. The container capacity is available in the AWS region. We would do this as part of the Series B infrastructure sprint."

She writes something down. Not a concern — a note. You read it as a positive signal.

After the session, Lucas sends you a Slack message: "that was good. The reframe worked."

---

**R — Result:**

The two slides become part of the technical appendix in the Series B investor deck. You do not know this until Lucas mentions it three weeks later in a 1:1 — he tells you matter-of-factly, not as praise, just as information. "Sophie used the sync reliability metrics in the deck. The 99.7% figure and the path-to-scale framing."

The number you pulled from Datadog on a Tuesday afternoon is now in a document that will be read by Series B investors. That is not something you expected when you took the ticket.

What you carry from this experience has nothing to do with the presentation itself. It is the reframe Lucas taught you: the same accurate information, presented as a plan rather than a problems list, answers a fundamentally different question. You use this framing consciously from this point forward — in retrospectives, in sprint planning, in every time you surface a finding to someone more senior.

---

### Story 2: Reviewing Priya's Third PR — The Cycle Completes

**Background:**

In week two of Month 9, Priya opens her third non-trivial PR — an endpoint for the Education Hub that tracks which learning path a user is currently enrolled in, `POST /v1/education/learning-paths/:pathId/enroll`. This is meaningfully more complex than her first two PRs. It involves a state machine: a user can only be enrolled in one learning path at a time, and enrolling in a new one should unenroll them from the current one in the same transaction.

Isabelle is the primary reviewer. You are added as secondary. You estimate it will take you thirty minutes to review properly.

---

**S — Situation:**

You open the PR expecting to find the same categories of issues you found in Months 7 and 8 — missing validations, index gaps, status code errors. You find none of those. Priya has handled all of them independently. The DTO has `ParseUUIDPipe` on the path parameter. The schema has a composite index on `(userId, learningPathId)`. The response uses 201. The ownership check is present.

What you find instead is something more interesting — a subtle atomicity issue in the transaction logic.

---

**T — Task:**

Review the PR thoroughly, catch the atomicity issue and explain it clearly, and write a review that is proportionate to where Priya is now — not treating her as a beginner, but as someone who is handling the basics well and is ready for a more advanced concept.

---

**A — Action:**

**The code Priya wrote:**

```typescript
// education.service.ts — Priya's implementation

async enrollInLearningPath(
  userId: string,
  pathId: string
): Promise<EnrollmentResponse> {

  // Check if user is already enrolled in this path
  const existingEnrollment = await this.prisma.userLearningPath.findFirst({
    where: { userId, learningPathId: pathId, status: 'ACTIVE' }
  })

  if (existingEnrollment) {
    return { enrollment: this.mapToDto(existingEnrollment), isNew: false }
  }

  // Unenroll from any current active path
  await this.prisma.userLearningPath.updateMany({
    where: { userId, status: 'ACTIVE' },
    data: { status: 'INACTIVE' }
  })

  // Enroll in the new path
  const newEnrollment = await this.prisma.userLearningPath.create({
    data: {
      userId,
      learningPathId: pathId,
      status: 'ACTIVE',
      enrolledAt: new Date(),
    }
  })

  return { enrollment: this.mapToDto(newEnrollment), isNew: true }
}
```

The logic is correct in a single-user, single-request world. But there is a gap. The `updateMany` (unenroll old) and the `create` (enroll new) are two separate database operations. They are not wrapped in a transaction.

**The failure scenario:**

```
WHAT CAN GO WRONG WITHOUT A TRANSACTION

T+0ms:  Request A arrives — user taps "Enroll in Path B"
T+1ms:  updateMany fires — Path A status → INACTIVE ✓
T+2ms:  Node.js process crashes (OOM, deployment, whatever)
        The create never runs.

Database state:
  Path A: INACTIVE  (unenrolled)
  Path B: never created (enrollment never happened)

User state:
  No active learning path at all.
  They lost their enrollment history without gaining the new one.
```

This is a partial write — the exact problem database transactions were designed to prevent. In a financial application, partial writes involving money are catastrophic. In an education app, a partial write involving learning path state is lower severity but still wrong — the user's state is corrupted in a way that is hard to recover from.

**Your review comment:**

You write one main comment. You are deliberate about the tone — Priya is not making a beginner mistake here. She has the right intent. The pattern she wrote works correctly when nothing goes wrong. The issue is what happens during failure, which is not obvious unless you have been taught to think about it.

```
Review comment on the enrollInLearningPath method:

"The logic here is correct and well-structured — the check,
the unenroll, the enroll, clean and readable.

One thing to add: the updateMany and create should be
wrapped in a prisma.$transaction(). Right now they are
two separate database operations.

If the process crashes between the updateMany and the
create — during a deployment, an OOM kill, anything — the
user ends up with no active learning path: their old path
is INACTIVE and the new one was never created. Their state
is corrupted.

A transaction makes this atomic:

  await this.prisma.$transaction(async (tx) => {
    await tx.userLearningPath.updateMany({
      where: { userId, status: 'ACTIVE' },
      data: { status: 'INACTIVE' },
    })
    const newEnrollment = await tx.userLearningPath.create({
      data: { userId, learningPathId: pathId, status: 'ACTIVE', ... }
    })
    return newEnrollment
  })

Either both operations happen or neither does.
No partial state.

This matters less for education data than it would for
financial data — but the habit of wrapping related writes
in a transaction is worth building regardless of the
severity. Partial writes in any system create state that
is hard to diagnose and painful to clean up."
```

You also leave one positive comment — not formulaic praise, but something specific:

```
Positive comment on the ownership check:

"The ownership check is solid here — verifying that the
learningPathId exists and is a valid path before attempting
enrollment, rather than letting Prisma throw a foreign key
violation. That is the right approach — clean validation
error returned to the client rather than a 500 from the DB."
```

**What happens in Priya's revision:**

Priya addresses the transaction comment in one revision. Her revised code wraps the two operations correctly using `prisma.$transaction`. She also replies to the comment: "makes sense — I was thinking about what the code does when everything works. Didn't think about what happens when it doesn't."

You reply: "that shift — from thinking about the happy path to thinking about failure modes — is one of the bigger changes in how you think about code as you get more experienced. Worth actively practicing."

---

**R — Result:**

The PR merges cleanly. Three PRs from Priya, three clear progressions: the first needed foundational corrections, the second needed none of those but had the atomicity issue, which you catch and explain. The third — you check her next PR in Month 10 — wraps every multi-step write in a transaction without being prompted.

The more visible result happens in the same week. You are added as a reviewer on an Arjun PR for a goals contribution feature. Arjun's code has a missing composite index on `(userId, goalId)` in a new `GoalMilestone` model. Priya is also a reviewer on the PR. You both see it. Priya comments on it first — with a structured explanation that mirrors the comment you left her in Month 7. You add a `+1` to her comment rather than duplicating it.

The knowledge has moved: Lucas to you, you to Priya, Priya to Arjun. You are not the endpoint of the chain anymore. You are a link in it.

---

### Story 3: The Two-Week Sprint That Closed Quietly

**Background:**

Month 9's final sprint is the least dramatic of the quarter. No incidents, no design discoveries, no presentations. Just a sprint where you committed to three things and delivered all three, with one piece of work that turns out to matter more than the ticket implied.

---

**S — Situation:**

Sprint 16 planning. You pick up three tickets: a small improvement to the sync error classification (adding two new GoCardless error types that have been appearing in logs without a specific `error_type` tag), a documentation update for the consent expiry notification feature (the runbook was written in Month 4 but never updated after the three-window notification change in Month 7), and a new field on the `GET /v1/accounts/:accountId` response — `daysUntilConsentExpiry` as an integer, computed server-side, so the mobile app can display countdown text without client-side date arithmetic.

None of these are interesting tickets individually. Together they represent something: the habits you have built over nine months operating at a consistent level.

---

**T — Task:**

Deliver all three tickets within the sprint. Give particular care to the documentation update — not because it is technically challenging, but because documentation that nobody reads is a waste, and documentation written for the engineer who will need it in a crisis is genuinely valuable.

---

**A — Action:**

**Ticket 1 — Error classification:**

Two new GoCardless error patterns have appeared in the logs since the `error_type` tagging was added in Month 4. They are showing up as `unknown` in the Datadog metrics — which means the monitor for GoCardless-specific errors is not filtering them correctly, and they are generating noise.

```typescript
// transaction-sync.worker.ts — the classification update

private classifyError(error: Error): string {
  const message = error.message.toLowerCase()

  if (message.includes('429') || message.includes('rate limit'))
    return 'gocardless_rate_limit'

  if (message.includes('503') || message.includes('service unavailable'))
    return 'gocardless_transient'

  if (message.includes('504') || message.includes('gateway timeout'))
    return 'gocardless_timeout'

  // NEW — added in this ticket:
  // Consent revoked by user on their bank's side
  if (message.includes('account access revoked') || message.includes('ais_access_revoked'))
    return 'gocardless_consent_revoked'

  // NEW — added in this ticket:
  // Institution temporarily offline (not GoCardless's issue,
  // but the bank's own infrastructure)
  if (message.includes('institution_unavailable') || message.includes('bank_offline'))
    return 'gocardless_institution_unavailable'

  if (message.includes('timeout') && message.includes('postgres'))
    return 'postgres_timeout'

  return 'unknown'
}
```

The Datadog monitor is updated to also exclude `gocardless_institution_unavailable` from the alert condition — this is expected behaviour when a bank has planned maintenance. The `unknown` count in Datadog drops to near zero after this ships.

**Ticket 2 — Documentation update:**

The consent expiry notification runbook was written in Month 4, when the feature first shipped. At that time, the notification fired once — seven days before expiry. In Month 7, you updated the feature to fire at three windows: 7 days, 3 days, and 1 day before expiry. The runbook was never updated to reflect this.

This matters because the runbook is what an on-call engineer reads at 2am when users report not receiving reconnection reminders. If the runbook says "one notification per expiring connection" and the code actually sends three, the on-call engineer wastes time looking for a bug that does not exist.

You rewrite the relevant sections. The most important addition is a decision tree for the most likely support scenarios:

```
RUNBOOK — CONSENT EXPIRY NOTIFICATIONS
Updated: Month 9

HOW NOTIFICATIONS ARE SENT:
  Users receive up to 3 notifications before consent expires:
  1. 7 days before expiry  (~day 83 of 90-day consent window)
  2. 3 days before expiry  (~day 87)
  3. 1 day before expiry   (~day 89)

  Each notification uses Redis deduplication:
  key: consent:expiry:notif:{connectionId}:{today}
  TTL: 24 hours

  A user who reconnects on day 85 will NOT receive the
  day-87 or day-89 notifications — the connection status
  changes to ACTIVE with a new consentExpiresAt, and the
  check-expiring-consents job skips connections with more
  than 7 days remaining.

SUPPORT SCENARIO 1: User says "I never received a reminder"
  Check 1: Is their consentExpiresAt in the past?
    → If yes: consent already expired. Reminders would have
      fired before expiry. Check was the user's push
      notifications enabled at the time?
  Check 2: Query Redis for dedup keys:
    SCAN 0 MATCH consent:expiry:notif:{connectionId}:*
    → Keys present: notifications were sent. Check
      Notification Service delivery logs.
    → No keys: job did not fire for this connection.
      Check BullMQ job history for check-expiring-consents.

SUPPORT SCENARIO 2: User says "I received too many reminders"
  This is expected behaviour — up to 3 reminders is correct.
  If a user reports more than 3, check Redis dedup key TTL.
  A key expiring too early (< 24h TTL) would allow a second
  notification on the same day. Check Redis config.

BULLMQ JOB DETAILS:
  Queue: consent-check
  Job name: CHECK_EXPIRING_CONSENTS
  Schedule: 09:00 UTC daily
  jobId: check-expiring-consents-daily (singleton)
  To verify last run: Bull Board → consent-check → completed
```

Lucas reviews the runbook. One comment: "add the Bull Board URL to the BULLMQ JOB DETAILS section so the on-call engineer does not have to remember it." You add it.

**Ticket 3 — daysUntilConsentExpiry field:**

This is the smallest ticket technically — one computed field added to the account detail response. But you take a few minutes to think through the edge cases before writing the code, because computed fields have a habit of behaving unexpectedly at the boundaries.

```typescript
// account.service.ts — the new computed field

private getDaysUntilConsentExpiry(
  consentExpiresAt: Date | null
): number | null {

  if (!consentExpiresAt) return null

  const now = new Date()
  const msUntilExpiry = consentExpiresAt.getTime() - now.getTime()

  // Already expired — return 0, not a negative number
  // Negative days would confuse the mobile UI
  if (msUntilExpiry <= 0) return 0

  // Ceil, not floor — if there are 6.2 days remaining,
  // return 7, not 6. The user has not lost day 7 yet.
  // This matches how humans naturally count time remaining:
  // "7 days" means "at least 7 full days remain"
  return Math.ceil(msUntilExpiry / (1000 * 60 * 60 * 24))
}
```

You add a comment in the code explaining the `Math.ceil` choice — because it is non-obvious and whoever reads this later should not have to reverse-engineer the intent. Lucas's review comment on this ticket is one line: "correct, and the comment explains the intent. Merge."

---

**R — Result:**

All three tickets delivered within the sprint. The `unknown` error count in Datadog drops after ticket 1. The runbook is clean and current after ticket 2. The `daysUntilConsentExpiry` field ships without issue after ticket 3.

The result that matters most from this sprint is not any individual ticket. It is the pattern. You committed to three things. You delivered three things. You communicated the dependency on Lucas's runbook review proactively. When the `daysUntilConsentExpiry` edge case question came up during Lucas's review — "what does this return when consent has already expired?" — you had already thought through it and the code and the comment both answered the question.

Daniel mentions in your Month 9 check-in: "velocity is consistent. Estimates are reliable. The team can plan around you." He says this matter-of-factly, not effusively. You understand that it is a real compliment precisely because it is not delivered as one.

---

## What Month 9 Taught You Overall

Three lessons, each quiet in its own way:

**From the Series B presentation:** The same accurate information, framed as a plan rather than a problems list, answers a fundamentally different question. Lucas did not teach you to be optimistic or to hide problems. He taught you to frame things in terms of what they mean for the person asking. Sophie was asking whether the module could scale. "Here is the path" answers that question. "Here are the limitations" does not.

**From Priya's third PR review:** The compounding effect of good reviews is visible by Month 9. The issues you caught in Month 7 — missing indexes, missing validation — are not there anymore. The issue you caught in Month 9 — atomicity — is a more advanced concept that Priya was ready to learn because the foundational ones were already solid. Teaching works the same way: you can only cover the next layer when the previous one is stable. By reviewing well in Month 7, you made Month 9's conversation possible.

**From the quiet sprint:** Consistent delivery is its own kind of contribution. Not every month needs an incident or a discovery. The sprints where you commit accurately, communicate dependencies, and deliver what you said you would — those are the sprints that make you someone the team can plan around. Daniel's comment — "the team can plan around you" — is the result of nine months of being someone they could rely on, one sprint at a time.