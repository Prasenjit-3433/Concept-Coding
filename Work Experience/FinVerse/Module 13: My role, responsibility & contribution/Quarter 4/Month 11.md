# Month 11: Leaving Things Better Than You Found Them

---

## Theme: *The work that outlasts the sprint*

---

## Foundational Knowledge: What You Need Before These Stories

Month 11 has two main threads — a self-directed observability improvement and writing the first comprehensive module documentation. Before the stories, one concept is worth understanding clearly, because it shapes both.

---

### Concept 1: The Difference Between Doing Your Work and Improving the System

There is a category of engineering work that does not appear on any sprint board, is not assigned by anyone, and produces no immediate feature visible to users. It is the work of making the system easier to understand, easier to debug, and easier to hand off to the next person.

This kind of work is easy to skip. Nobody will notice if you do not do it. The ticket backlog is always full of assigned work. There is always a good reason to defer it.

The engineers who do it anyway — who notice something that could be better and choose to fix it even though nobody asked — are the ones who leave a system in better shape than they found it. This is a choice, not an obligation.

```
TWO TYPES OF ENGINEERING WORK

TYPE 1: Assigned work
  Tickets in Linear, deadlines, sprint commitments.
  Visible. Measured. Expected.
  Every engineer does this.

TYPE 2: Unrequested improvement work
  Nobody assigned it. Nobody is waiting for it.
  You noticed it. You proposed it. You did it.
  
  Examples:
  - Noticing a gap in the observability dashboard
    and filling it
  - Writing documentation that does not exist yet
  - Adding a test for an edge case that nobody caught
  - Cleaning up a configuration that was confusing
  
  This work is invisible until it matters.
  Then it matters enormously.
```

Month 11 is largely built around this second category. The observability work was not in the sprint. The documentation was not a ticket. Both happened because you chose to do them.

---

### Concept 2: What Good Technical Documentation Actually Does

Technical documentation gets a bad reputation, and usually for good reason. Most documentation is one of two things: either a description of what the code does (which you can read from the code directly) or a list of steps that was accurate when it was written and is now six months out of date.

Good documentation does something different. It answers the questions a new engineer will have that the code itself cannot answer.

```
WHAT CODE CANNOT TELL YOU

Code tells you:
  WHAT the system does (what functions do, what fields mean)
  HOW it works (the implementation details)

Code cannot tell you:
  WHY certain decisions were made
  WHAT HAPPENS when things go wrong
  HOW TO DIAGNOSE a problem you've never seen before
  WHAT EDGE CASES exist and why they're handled the way they are
  WHERE TO LOOK when a user files a support ticket
  WHAT NOT TO CHANGE and why

These are the questions a new engineer asks in their first month.
Without documentation, they ask them of whoever is available.
That person is usually the engineer who wrote the code —
who now spends an hour explaining something they should have
written down six months ago.

Good documentation answers these questions in advance.
It frees up the experienced engineer.
It unblocks the new engineer.
It preserves institutional knowledge that would otherwise
leave when the author leaves.
```

In your final months at FinVerse, you are the person who has the answers. You have owned the Accounts & Open Banking module for nine months. You know the GoCardless edge cases, the sync failure patterns, the BullMQ job structure, the sync log interpretation, the consent expiry flow. That knowledge lives only in your head unless you write it down.

Writing the module documentation in Month 11 is an act of deliberate knowledge transfer. You are writing for the engineer who comes after you — who you will never meet, who will sit down at the codebase six months from now and need to understand exactly what you understand today.

---

## The Stories

---

### Story 1: The Observability Dashboard — Noticing a Gap and Filling It

**Background:**

The Core Product team's Datadog dashboard for the sync pipeline has existed since before you joined. It was built by Elena when she owned the Accounts module — a good baseline that covers the essentials: sync job duration histograms, error rates, queue depth. It has served the team well.

By Month 11, after ten months of working with the module, you have a precise picture of what the dashboard shows and — more importantly — what it does not show. The rate storm in Month 10 crystallised something you had been vaguely aware of for months: the dashboard is good at telling you something is wrong, but not particularly good at telling you where to look first or which users are affected.

Three specific gaps you have identified:

First — no per-bank error rate breakdown. When a GoCardless error occurs, the current dashboard shows the aggregate error rate across all institutions. During Month 10, you knew GoCardless was rate limiting you, but you could not quickly tell whether the errors were distributed across all institutions or concentrated in specific ones. If they were concentrated — say, only Deutsche Bank accounts were failing — that would point to a bank-specific issue rather than a global rate limit.

Second — no alert for users whose sync has been silent for more than 8 hours. The periodic sync runs every 4 hours per user. A user who has not successfully synced in 8 hours has missed at least two cycles. Something is wrong with their connection. Today, you only discover this when the user files a support ticket. With the right metric, you could be proactive.

Third — no visibility into which GoCardless institution IDs have the highest failure rates historically. This would be immediately useful for support triage — if you know that Deutsche Bank (DEUTSCHE_DEDB) consistently has higher error rates than Monzo (MONZO_MONZGB2L), you can tell a user who reports sync problems that this is a known pattern and not their fault.

---

**S — Situation:**

It is the second week of Month 11. You have no tickets in active development — sprint 18 has two small feature tickets that you finished in the first week. You have four days before the next sprint planning. You send a Slack message to Lucas:

"I want to spend a couple of days improving the sync observability dashboard — I've been noticing gaps since the rate storm and I think it would reduce time-to-root-cause during incidents. Can I take this as a self-assigned task?"

Lucas responds within ten minutes: "yes. Show me what you build at the sprint review."

---

**T — Task:**

Design and implement three observability improvements to the sync pipeline dashboard. Present them at sprint review. Get them into production before the end of the sprint.

---

**A — Action:**

**Gap 1 — Per-bank error rate:**

The existing `recordGoCardlessCall` metric in the MetricsService records errors but tags them only by HTTP status code. You add an `institution_id` tag:

```typescript
// gocardless.service.ts — updated metric recording

async fetchTransactionsSince(
  accountId: string,
  since: Date,
  institutionId: string   // ← pass institution ID through to metric
): Promise<GoCardlessTransaction[]> {
  try {
    const response = await this.httpClient.get(
      `/accounts/${accountId}/transactions`,
      { params: { date_from: since.toISOString() } }
    )

    this.metricsService.recordGoCardlessCall(
      response.status,
      institutionId     // ← now included in the metric
    )
    return response.data.transactions

  } catch (error) {
    const statusCode = error.response?.status ?? 0
    this.metricsService.recordGoCardlessCall(statusCode, institutionId)
    throw error
  }
}
```

```typescript
// metrics.service.ts — updated to accept institution_id tag

recordGoCardlessCall(statusCode: number, institutionId: string): void {
  this.goCardlessApiCalls.add(1, {
    status_code: String(statusCode),
    institution_id: institutionId,   // ← new tag
  })
  if (statusCode >= 400) {
    this.goCardlessApiErrors.add(1, {
      status_code: String(statusCode),
      institution_id: institutionId, // ← new tag
    })
  }
}
```

In Datadog, you create a new dashboard widget: a table showing error rate grouped by `institution_id`, sorted descending. This immediately shows which banks are causing the most problems.

You also add a **top-list widget** — a ranked list of institutions by error rate over the last 7 days. This is the "historical failure rate by institution" view that would have been useful for support triage.

**Gap 2 — Stale sync alert:**

This one requires a new metric — a gauge that counts users whose last successful sync is older than 8 hours. You implement it as an observable gauge in the MetricsService, polled every 5 minutes:

```typescript
// metrics.service.ts — new stale sync gauge

private staleUserSyncsGauge: ObservableGauge

// In onModuleInit():
this.staleUserSyncsGauge = this.meter.createObservableGauge(
  'finverse.transaction_sync.stale_users',
  {
    description: 'Number of active users with no successful sync in 8+ hours',
  }
)

// Register the callback — polled every 60s by OTEL SDK
this.staleUserSyncsGauge.addCallback(async (result) => {
  const count = await this.accountRepository.countStaleUsers(8)
  result.observe(count, { threshold_hours: '8' })
})
```

```typescript
// account.repository.ts — new query

async countStaleUsers(thresholdHours: number): Promise<number> {
  const cutoff = new Date(
    Date.now() - thresholdHours * 60 * 60 * 1000
  )

  // Count active users who have connected accounts
  // where the most recent successful sync is before the cutoff
  const result = await this.prisma.bankAccount.groupBy({
    by: ['userId'],
    where: {
      isActive: true,
      syncStatus: { not: 'SYNCING' },
      OR: [
        { lastSyncedAt: null },
        { lastSyncedAt: { lt: cutoff } }
      ]
    },
    _count: { userId: true },
  })

  return result.length
}
```

You add a Datadog monitor for this metric — a warning at 500 stale users, a critical alert at 2,000. In normal operation, this number should be near zero. A spike indicates either a widespread sync failure or GoCardless instability affecting many users simultaneously.

**Gap 3 — Sprint review presentation:**

You build a new "Sync Health" section of the dashboard with three panels: the per-bank error rate table, the stale user count gauge over time, and a heatmap showing sync duration distribution by institution. You write three sentences of description under each panel explaining what it measures and what action to take if it looks abnormal.

Lucas reviews the dashboard on Thursday morning before the sprint review. He spends five minutes looking at the per-bank error rate table — it shows Deutsche Bank (DEUTSCHE_DEDB) has a historically higher error rate than other institutions, at around 2.1% versus the overall 0.3%. He says: "that is useful. I've seen more support tickets from Deutsche Bank users than from others but I never had data to confirm it. Now I do."

---

**Sprint review:**

At the sprint review Thursday afternoon, you present the dashboard changes. Sophie — the VP of Engineering — is in the review as usual. You walk through the three gaps you identified, the metrics you added, and the dashboard panels. You show the per-bank error rate table with Deutsche Bank's elevated rate.

Sophie asks one question: "can we use this data to proactively contact users whose sync is consistently failing?"

You say: "yes — the stale user count metric tells us who, and we have the institution ID so we know which bank they're on. If we combine that with a notification trigger, we could send users a proactive message when their sync has been stale for more than 8 hours."

Sophie looks at the product manager, Céline: "this is worth a ticket." Céline writes a note.

You did not plan for this outcome. You built observability tooling for the engineering team. The data it exposed opened a product conversation you did not anticipate.

Lucas says one thing after the review: "good initiative. This is the kind of work that makes the next incident 10 minutes shorter."

---

**R — Result:**

The dashboard changes go to production on Thursday. The stale user monitor fires once in the remaining weeks of your contract — a 40-minute window where a GoCardless token refresh issue caused syncs to fail across a subset of users. The on-call engineer sees the stale user count spike, immediately knows the scope of the problem, and resolves it in 22 minutes using the per-bank breakdown to confirm which institution is affected.

Without the dashboard change, that diagnosis would have started with a user support ticket and ended with 40 minutes of log searching. With it, the on-call engineer sees the problem before users report it and has the relevant context immediately.

The proactive notification feature that Sophie and Céline discussed makes it onto the Series B roadmap two months after you leave. You do not implement it — your contract ends before it gets prioritised — but the metric you built is the data foundation it requires.

---

### Story 2: Writing the Module Documentation

**Background:**

In the third week of Month 11, Tomasz mentions in a Slack thread that he spent 45 minutes trying to understand why a specific GoCardless API response was being handled differently for one German bank versus others. The answer was buried in a comment in the sync worker from 14 months ago — a workaround Elena had added for a specific institution that returns non-standard IBAN formats. There was no documentation explaining it.

Tomasz was not complaining. He was just noting it in passing. But you hear it differently. You have been the owner of this module for nine months. You know about that workaround. You know about six other similar edge cases that are not documented anywhere. You know the patterns in sync failures, the consent expiry flow details, how to read a SyncLog record, how to replay failed BullMQ jobs from Bull Board.

All of that knowledge lives only in your head. And your contract ends in less than two months.

---

**S — Situation:**

You bring it up in your 1:1 with Lucas in week three of Month 11. "I want to write proper documentation for the Accounts & Open Banking module before I leave. I know things about this module that aren't written down anywhere, and when I'm gone, someone is going to spend a week rediscovering them."

Lucas: "how long do you think that would take?"

You: "three days for a first version, probably. Maybe a fourth for review and corrections."

Lucas: "do it. Put it in the onboarding reading list when you're done."

---

**T — Task:**

Write comprehensive documentation for the Accounts & Open Banking module — specifically, documentation that answers the questions a new engineer will have that the code cannot answer. Focus on why decisions were made, how to debug common problems, and what edge cases exist.

---

**A — Action:**

You start by asking yourself the question from the Foundational Knowledge section: what would I have needed to know in week one that took me months to discover? You make a list. It runs to 23 items.

You group them into five sections:

```
MODULE DOCUMENTATION STRUCTURE

1. Architecture & Overview
   How the module fits into the system.
   The GoCardless integration flow end to end.
   The BullMQ job types and when each fires.
   The outbox events this module publishes and why.

2. GoCardless Integration — Edge Cases & Known Issues
   Banks that return non-standard IBAN formats
   and the workarounds in place.
   GoCardless API rate limits and how the rate limiter
   is configured to stay within them.
   What happens when consent expires — the full flow
   from expiry detection to user notification.
   Known unreliable institutions and their typical
   failure patterns.

3. Sync Log Interpretation — How to Read a SyncLog Record
   What each status value means.
   What transactionsDuplicate > 0 means and when
   it is expected vs unexpected.
   What a FAILED status with no errorMessage means.
   The difference between a stalled job and a failed job.

4. Debugging Common Problems — Step by Step
   "User reports their transactions haven't updated"
   → specific Datadog queries to run, in order.
   "Sync status shows SYNCING but never completes"
   → how to diagnose, how to recover.
   "User sees their bank as disconnected after reconnecting"
   → the race condition in consent status, how it resolves.

5. Operations — Bull Board and Replay Procedures
   How to replay failed BullMQ jobs from Bull Board.
   How to pause and resume the sync queue safely.
   When to replay versus when to investigate first.
   The GoCardless rate limit runbook.
```

You write the first draft over three days. It is 2,800 words. You go through it twice more — once to cut anything that merely describes the code (which the code already says), and once to add specific Datadog queries for the debugging section so they can be copy-pasted rather than reconstructed.

The debugging section is the hardest part to write. For each problem, you need to reconstruct the investigation path you have learned over nine months — not just the answer, but the sequence of steps that leads to the answer efficiently.

Here is one example from the documentation, the entry for "User reports transactions haven't updated":

```
DEBUGGING: "User reports transactions haven't updated"

This is the most common support ticket pattern.
Work through these steps in order.

Step 1: Find the user's most recent sync attempt
  Datadog Log Explorer:
  service:core-product @userId:{userId} "sync"
  Sort by: timestamp descending
  Look for: last SyncLog record for this user

  Tells you: when the last sync ran and what happened

Step 2: Check the SyncLog status
  If status: SUCCESS, lastSyncedAt recent:
  → Sync ran successfully. Problem may be UI caching.
  → Ask user to force-refresh the app.

  If status: FAILED:
  → Check errorMessage field.
  → Most common: "GoCardless 401: token expired"
    → Consent has expired. User needs to reconnect.
    → Check consentExpiresAt on BankConnection record.
  → Less common: "GoCardless 503"
    → Transient GoCardless failure. Check if sync
       was retried (look for subsequent SyncLog entries).

  If status: SYNCING (and startedAt more than 10 mins ago):
  → Job may be stalled. Check Bull Board active jobs.
  → If job appears stalled in Bull Board:
    → BullMQ will detect and requeue within 30-60s.
    → Wait 2 minutes before taking manual action.

Step 3: If no recent SyncLog exists at all
  Check BullMQ queue for pending jobs for this user:
  Bull Board → transaction-sync → waiting
  Filter by userId in job data.

  If job is waiting:
  → Queue is backed up. Check queue depth metric.
  → No action needed — it will process.

  If no job exists:
  → The periodic sync scheduler may have missed
    this user. Check if their BankConnection status
    is ACTIVE.
  → If ACTIVE but no sync job: manually enqueue via
    POST /v1/accounts/{accountId}/sync (admin endpoint).

GoCardless status page: https://status.gocardless.com
Bull Board: https://[internal-url]/admin/queues
```

Lucas reviews the completed documentation over one afternoon. He makes twelve edits — mostly adding precision to phrases that were slightly ambiguous, and adding two edge cases you had not covered (what happens when a user has the same bank connected twice from different devices, and how the deduplication works for accounts migrated from an old GoCardless requisition to a new one). He approves and adds it to the onboarding reading list.

Tomasz reads it within a day of publication and sends a Slack message to the team channel: "whoever wrote the sync debugging section of the accounts module docs — thank you. This would have saved me 45 minutes last week."

---

**R — Result:**

The documentation is published in the fourth week of Month 11. It goes into the Core Product team's Notion onboarding page immediately.

Four months after you leave FinVerse, a new engineer joins the Core Product team. You never meet them. But they are given the same onboarding reading list you were given in your first week — and your documentation is on it. The GoCardless edge cases you spent months discovering are now available to them on day three.

The most concrete result is harder to measure than a metric: the knowledge you accumulated over nine months does not disappear when your contract ends. It stays in the system. That is what documentation does when it is written by someone who actually knows the subject.

---

### Story 3: A Quiet Sprint — The Value of Consistent Delivery

**Background:**

Not every story is a dramatic incident or a self-directed initiative. Sprint 18 — the sprint where the observability work and documentation happen — also contains two regular feature tickets. Both are routine. Both ship on time.

This story is brief, but worth telling, because interviewers sometimes ask "describe a typical sprint" — and the answer should demonstrate that good work habits are not reserved for special occasions.

---

**S — Situation:**

Sprint 18 planning. You have two assigned tickets alongside the self-directed work Lucas approved: adding `daysUntilExpiry` as a calculated field to the net worth endpoint response (a small product request from the mobile team), and updating the sync retry count cap from 3 to 4 for users in countries where GoCardless has historically higher error rates (Germany, Italy).

Both are small — 2 and 3 story points respectively. Neither is technically complex. But the country-specific retry logic has a dependency you spot during planning.

---

**T — Task:**

Deliver both tickets on time, flag the dependency clearly in sprint planning rather than discovering it mid-sprint, and do not let the self-directed work affect your committed delivery.

---

**A — Action:**

During sprint planning, you walk through the country-specific retry ticket and notice: the retry count cap logic needs access to the user's country, which lives in the `auth.users` table. The sync worker currently does not load user data — it loads account data. Adding the country read means either a new database query inside the sync worker or a change to the job payload.

You flag it in the planning meeting: "this ticket has a small dependency I want to call out — we need user country data in the sync worker, which means either adding a DB query per job or changing the job payload structure. I want to confirm the approach with Lucas before I implement rather than discovering it mid-week."

Lucas says: "job payload change — add `userCountry` to the payload when the job is enqueued. Simpler than a DB query inside the worker." You update your estimate from 3 to 3 story points — the approach is confirmed, the complexity is the same. Five minutes of planning saved a potential mid-sprint redirect.

Both tickets deliver on Tuesday and Wednesday of week two. The self-directed observability work fills the remainder of the sprint without affecting either commitment.

---

**R — Result:**

Sprint 18 closes with everything delivered. The country-specific retry cap is in production by Wednesday. The net worth `daysUntilExpiry` field ships on Tuesday and the mobile team uses it in a UI update the following week.

Nothing dramatic. Clean delivery, dependency flagged early, self-directed work completed alongside commitments. This is what a consistent engineer looks like in a quiet sprint — not every sprint is an incident or an initiative. Most of them are just this.

---

## What Month 11 Taught You Overall

Two things, both lasting:

**From the observability work:** The most valuable engineering work is sometimes the work that nobody asked for. The per-bank error rate breakdown, the stale user metric, the debugging documentation — none of these were in any sprint backlog. All of them made the system meaningfully better. The ability to see what is missing and choose to fix it, without being told to, is one of the clearest signals that an engineer has moved beyond executing tickets and started taking genuine ownership of the system.

**From the documentation:** Writing documentation forces precision. You cannot write "the sync handles consent expiry" without immediately asking yourself: how, exactly? What triggers the detection? What happens when detection fires? What are the edge cases? The act of writing forces you to surface ambiguity that you had been carrying around as vague understanding. Several times during the documentation writing you discovered you did not understand something as well as you thought — and had to go back to the code to verify before you could write it down. Documentation is not just knowledge transfer. It is knowledge clarification.