Looking at the planning document and the existing month files (Month 1 and Month 4 especially), I have a clear picture of the format, depth, and storytelling style required.

A few quick confirmations before I start:

1. The planning doc says Quarter 3 covers Months 7, 8, and 9 — I'll write one month per response, starting with Month 7 now.

2. Month 7's defining moments from the plan: proactively surfacing the Redis stale balance bug, and Priya joining where you naturally become an informal mentor. I'll build the stories around these.

3. The format follows Month 1 and Month 4 exactly: theme, foundational knowledge section (teaching a concept before the story), then STAR-format stories.

Ready to write Month 7. Starting now.

---

# Month 7: Noticing What Isn't Yours to Notice

---

## Theme: *From module owner to system thinker*

---

## Foundational Knowledge: What You Need Before These Stories

Two concepts come up directly in Month 7 that are worth understanding before the stories. Without this context, the actions will read as routine. With it, you will understand why they mattered.

---

### Concept 1: Cache Invalidation — The Problem That Keeps Coming Back

You learned caching deeply in Module 9. But Month 7 introduces a specific failure mode — not a dramatic crash, but a quiet inconsistency that sits in production making no noise until a user notices something wrong.

The scenario is straightforward. Two systems both hold the same piece of data:

```
CACHE STALENESS — THE SILENT INCONSISTENCY

PostgreSQL (source of truth):
  bank_accounts.current_balance = €2,150.00
  (just updated by the sync worker at 14:00)

Redis (cache):
  key: usr:acct:bal:acc_456
  value: €1,840.00
  TTL: expires at 14:45 (set 45 minutes ago)

User opens the app at 14:10:
  GET /v1/accounts
  → Service reads from Redis (cache HIT)
  → Returns €1,840.00

What the user sees:
  Their balance has not changed despite syncing 10 minutes ago.
  They tap "Sync now".
  App shows "Already synced 10 minutes ago".
  Balance still shows €1,840.00.

What actually happened:
  The sync worker updated PostgreSQL correctly.
  It did NOT invalidate the Redis key.
  The cache will serve the stale value for another 35 minutes.
```

This is the cache invalidation problem. The fix is conceptually simple: when data changes, invalidate the cache. But the failure mode is subtle — it does not throw an error, does not trigger an alert, and leaves no trace in the logs. The database is correct. The cache is wrong. The user sees the wrong number.

In a financial application, a stale balance is not an abstract concern. A user making a spending decision based on a balance that is €310 higher than their actual balance could overspend. A user checking whether they have enough for rent could be misled. The numbers matter.

What makes this particularly tricky to catch is that it only manifests under a specific timing condition: the sync completes, the user opens the app before the TTL expires, and the cache has not been explicitly invalidated. In testing, you almost never hit this timing window. In production with hundreds of thousands of users, you hit it constantly.

---

### Concept 2: What "Owning the System" Actually Means at Your Level

There is a difference between owning a module and thinking about the system. Every engineer on the Core Product team owns something. Tomasz owns Transactions. Arjun owns Goals. You own Accounts and Open Banking.

Module ownership means: you are responsible for the correctness, reliability, and quality of the code within your module's boundaries. When a sync fails, it is your job to investigate. When the GoCardless integration breaks, you fix it.

System thinking is something different. It means noticing connections between modules — seeing that a decision made in your module has consequences in someone else's module, or that a pattern in someone else's module creates a vulnerability in yours. It means asking: "does the system as a whole behave correctly?" — not just "does my module behave correctly?"

```
MODULE OWNERSHIP vs SYSTEM THINKING

Module ownership (what everyone does):
  "My sync worker updates balances in PostgreSQL correctly."
  ✓ True
  ✓ Responsibility fulfilled
  ✗ Incomplete picture

System thinking (what senior engineers do naturally):
  "My sync worker updates balances in PostgreSQL correctly.
   But the caching layer in the service response path reads
   from Redis, not PostgreSQL. If the cache is not invalidated
   when the balance updates, the user sees wrong data.
   The system as a whole does not behave correctly."
```

Junior engineers become mid-level engineers not by writing better code in their own module, but by developing the habit of noticing how their module interacts with everything around it. Month 7 is the first time you demonstrate this habit in a way that someone other than you benefits from.

---

## The Stories

---

### Story 1: The Redis Stale Balance Bug

**Background:**

In July 2024, you are working on a small improvement to the sync status endpoint — adding the `transactionsDuplicate` field to the sync status response so the mobile app can show users how many transactions were new versus duplicates. Routine work. You are reading through the account service code to understand how the balance fields flow from the sync worker into the API response.

You are not looking for a bug. You are just reading.

---

**S — Situation:**

You are three weeks into Month 7. The consent expiry notification feature from Month 4 has been running in production without issues. The stalled job fix from Month 5 has been stable. You are in a comfortable rhythm with the module — no fires, steady feature work.

While tracing how `currentBalance` flows from the sync worker into the `GET /v1/accounts` response, you notice something. The sync worker updates `currentBalance` in PostgreSQL using `prisma.bankAccount.update()`. Then the API response in `AccountService.getAccountsForUser()` reads from PostgreSQL using a `findMany()` query — but only on a cache miss. On a cache hit, it returns whatever is in Redis under the key `usr:acct:acc:{accountId}`.

You check when the Redis cache is populated. It is populated when a user first calls `GET /v1/accounts`. The TTL is one hour. You look for where the cache is invalidated. You search the entire accounts module. You search the sync worker. You search the transaction service.

There is no cache invalidation call anywhere.

---

**T — Task:**

Understand whether this is actually a problem, assess how often it happens in practice, and figure out the right fix — then decide what to do with the finding.

---

**A — Action:**

**Step 1: Confirm the problem exists**

You write a small mental scenario to verify your understanding is correct:

```
TIMELINE OF THE BUG

T+0:00   User opens app → GET /v1/accounts
         Cache MISS → PostgreSQL read → balance: €1,840
         Cache SET: Redis key = €1,840, TTL = 60 minutes

T+0:15   Periodic sync fires for this user
         GoCardless returns updated balance: €2,150
         sync worker: prisma.bankAccount.update({ currentBalance: 2150 })
         → PostgreSQL updated ✓
         → Redis key NOT invalidated ✗
         Redis still holds: €1,840, TTL now 45 minutes remaining

T+0:20   User opens app again → GET /v1/accounts
         Cache HIT → returns Redis value: €1,840
         User sees stale balance

T+1:00   Redis TTL expires → cache entry removed
         Next request reads from PostgreSQL → returns €2,150
         User finally sees correct balance
         → 40-minute window of incorrect data
```

The scenario is real. The cache TTL is one hour. The sync runs every four hours. In the worst case, a user who opens the app right after their sync completes will see a stale balance for up to an hour.

**Step 2: Check whether it is noticeable in practice**

You look at the Datadog dashboard for the sync pipeline. The periodic sync runs every four hours per user. The last sync for most users happens at one of the fixed four-hour windows — 00:00, 04:00, 08:00, 12:00, 16:00, 20:00 UTC. The cache TTL is one hour.

For a user who syncs at 08:00 and opens the app at 08:10, they would see a stale balance until 09:00 — the cache entry was set before the sync, and has 50 minutes of TTL remaining.

For a user who syncs at 08:00 and first opens the app at 08:05 (before the sync completes), the cache is not yet set. Their balance is fetched fresh from PostgreSQL. No staleness.

The window is real but timing-dependent. It does not affect every user on every sync — only users who have an active Redis cache entry when their sync completes.

**Step 3: Determine how bad this actually is**

You think about the user consequence. FinVerse is a long-term savings and investing platform, not a real-time trading app. A balance that is 40 minutes stale is not going to cause someone to make a catastrophic financial decision. But it erodes trust. A user who syncs their account and then opens the app expecting fresh data — and sees unchanged numbers — will either think the sync failed or think the app is unreliable. Both are bad.

More importantly: this is precisely the kind of silent inconsistency that turns into a support ticket three months from now when someone notices their balance has been wrong. And at that point, debugging a timing-dependent cache issue with no logs and no alerts is a nightmare.

**Step 4: Form the fix**

The fix is simple: invalidate the Redis cache key when the sync worker updates the balance.

```typescript
// transaction-sync.worker.ts — the existing balance update

// BEFORE (missing invalidation):
await this.prisma.bankAccount.update({
  where: { id: bankAccountId },
  data: {
    currentBalance:    new Decimal(latestBalance.current),
    availableBalance:  new Decimal(latestBalance.available ?? 0),
    syncStatus:        'SUCCESS',
    lastSyncedAt:      new Date(),
  }
})
// ← nothing happens to Redis here

// AFTER (with invalidation):
await this.prisma.bankAccount.update({
  where: { id: bankAccountId },
  data: {
    currentBalance:    new Decimal(latestBalance.current),
    availableBalance:  new Decimal(latestBalance.available ?? 0),
    syncStatus:        'SUCCESS',
    lastSyncedAt:      new Date(),
  }
})

// Invalidate the cache for this account
// The next GET /v1/accounts call will read fresh data from PostgreSQL
// and repopulate the cache with the correct balance
await this.redis.del(`usr:acct:acc:${bankAccountId}`)
```

The `del` call is lightweight — a single Redis command, under 1ms. No performance impact.

**Step 5: Bring it to Lucas**

Before writing any code, you send a Slack message to Lucas and Tomasz. You do not say "I found a bug." You say: "while reading through the account caching logic for a different task, I noticed something that might cause users to see stale balances after a sync. Wanted to check my understanding before doing anything — can I share what I found?"

Lucas responds within ten minutes: "yes, share it."

You write a short structured summary in Slack — four bullet points: what you observed, the timing scenario where it manifests, the user impact, and the proposed one-line fix. You include the code snippet.

Lucas reads it and replies: "correct analysis. That has been sitting there since the caching layer was added. Write the ticket, assign it to yourself, fix it."

He does not add "well done" — he says it in the next 1:1 instead, where the framing is more useful. He calls it "starting to think beyond your module." You remember that phrasing.

**Step 6: The fix and the test**

You open a PR with three changes:

The first change is the invalidation call in the sync worker. One line.

The second change is a test that explicitly verifies the invalidation happens. This is harder to write than the fix itself — you need to seed a Redis key, run a mock sync, and then verify the key is gone.

```typescript
// transaction-sync.worker.spec.ts — the new test

it('should invalidate the account balance cache after a successful sync', async () => {
  // Seed a stale balance in Redis
  const cacheKey = `usr:acct:acc:${TEST_ACCOUNT_ID}`
  await redis.set(cacheKey, JSON.stringify({ currentBalance: 1840 }), 'EX', 3600)

  // Verify the key exists before the sync
  const beforeSync = await redis.get(cacheKey)
  expect(beforeSync).not.toBeNull()

  // Run the sync worker (mocked GoCardless returns updated balance)
  await worker.process(mockSyncJob)

  // Verify the key was deleted after sync
  const afterSync = await redis.get(cacheKey)
  expect(afterSync).toBeNull()
})
```

The third change is a comment in the caching layer explaining why the invalidation exists — so the next engineer who reads the code understands the intent:

```typescript
// account.service.ts
// Cache is invalidated by the sync worker (transaction-sync.worker.ts)
// when account balances are updated. Do not add long TTLs here
// without ensuring the sync worker also invalidates on update.
```

Lucas reviews the PR. Two comments — both positive observations, no corrections. The PR merges on the second day.

---

**R — Result:**

The fix is in production by the end of the week. No user-facing incident, no urgent fix — just a quiet improvement that removed a real but silent inconsistency.

The more meaningful result is what happened in the next 1:1 with Lucas. He said: "that finding was not in your ticket. You were working on something else entirely and you stopped to look at something adjacent, figured out whether it was actually a problem, and brought it with a proposed fix rather than just a complaint. That is the difference between doing your work and owning the system."

You write that down. Not because you need to remember the praise, but because the distinction he named — "doing your work" versus "owning the system" — becomes a lens you use consciously from that point forward.

---

### Story 2: Priya Joins — Being on the Giving End

**Background:**

Priya Nair joins the team in the third week of Month 7. She is the most junior engineer on the team, joining fresh with about 18 months of experience at a small software consultancy in Bangalore. Daniel introduces her in the team Slack with a brief message: "Priya is joining the Education Hub and will support Isabelle and Arjun on some Goals work. Please give her a warm welcome."

You are assigned as one of her PR reviewers for her first month — alongside Isabelle, who is her primary mentor. Nobody explicitly tells you how to review her PRs. You have been on the receiving end of reviews for seven months. You know what it felt like when Lucas reviewed yours.

---

**S — Situation:**

Priya opens her first non-trivial PR in week two of Month 7. It is a new endpoint for tracking a user's lesson progress in the Education Hub — `POST /v1/education/lessons/:lessonId/complete`. Isabelle has reviewed it and left several structural comments already. You are added as a second reviewer.

The PR is 160 lines. The logic is reasonable. But you spot three things.

---

**T — Task:**

Review the PR in a way that is genuinely useful to Priya — not just catching issues, but explaining the reasoning so she can carry the pattern forward to the next PR without needing to be told again.

---

**A — Action:**

**The three things you found:**

The first is a missing composite index on `(userId, courseId)` in the `UserCourseProgress` model. The endpoint that marks a lesson complete first checks whether the user has already completed this course — using a `findFirst` query filtered by both `userId` and `courseId`. Without a composite index, this query scans the entire table. With 450,000 users each potentially having progress records across multiple courses, this becomes a performance issue as the table grows.

The second is a missing DTO validation. The `lessonId` in the URL path is used to look up a lesson from the database, but there is no `ParseUUIDPipe` on the parameter. Any string can be passed as the `lessonId` — including strings that are not UUIDs. This would cause Prisma to throw an unhandled exception rather than returning a clean 400.

The third is a 200 status code on the success response, where it should be 201 — a new `UserCourseProgress` record is being created (or a progress update is being made). The convention the team follows, as Lucas established, is 201 for resource creation.

**How you write the comments:**

You remember the six comments Lucas left on your first PR. What made them useful was not that they told you what was wrong — it was that they told you *why* the rule existed. "Test names should describe expected behaviour, not implementation." Not just "rename this test." The reasoning.

You write three comments the same way:

```
Comment 1 — Index:

"There's a missing composite index on (userId, courseId) in
UserCourseProgress.

The endpoint does:
  prisma.userCourseProgress.findFirst({
    where: { userId, courseId }
  })

Without an index, PostgreSQL scans every row in the
user_course_progress table to find the matching record.
At small scale this is fine. At 450k users with multiple
courses each, this becomes a full table scan on every
lesson completion.

The fix:
  @@index([userId, courseId])
  in the Prisma schema.

Worth reading: Module 5 in the project docs covers index
design reasoning if you want more context on when to add
composite vs single-column indexes."
```

```
Comment 2 — Validation:

"The :lessonId parameter needs a ParseUUIDPipe.

Right now, any string can come through — including strings
that are not valid UUIDs. If a non-UUID string reaches
Prisma's findUnique, it throws a PrismaClientValidationError
which will surface as a 500 to the caller.

Fix:
  @Param('lessonId', ParseUUIDPipe) lessonId: string

This is a pattern the team applies to every UUID path
parameter — you will see it on all the endpoint you look
at in the Accounts module."
```

```
Comment 3 — Status code:

"Small one: the success response should be 201, not 200.

200 means 'here is something that already existed'.
201 means 'I created a new thing as a result of your request'.

Since this endpoint creates a UserCourseProgress record,
201 is semantically correct.

Fix: add @HttpCode(HttpStatus.CREATED) above the handler,
or return res.status(201) explicitly."
```

You also leave one positive comment — because you remember that Lucas did this, and because it is genuinely true:

```
Positive comment:

"The idempotency logic here is clean — checking whether a
progress record already exists before creating a new one,
and returning the existing record rather than throwing a
conflict error. That is the right approach for a 'mark
complete' endpoint where the user might tap the button
twice. Good thinking."
```

Priya addresses all three comments in one revision round. She also responds to the index comment with a question: "would you always add a composite index when you have two where conditions, or is there a threshold?" You reply directly: "generally yes when both conditions appear together frequently in queries. The cost of maintaining an extra index is low compared to the cost of a sequential scan at scale. A good rule: if the same two columns appear together in a WHERE clause in more than one or two queries across the codebase, add a composite index."

She replies: "that makes sense, thank you." The PR merges the next day.

---

**R — Result:**

Priya's PR merges clean. The three issues that would have shipped to production are caught before they get there.

The more lasting result: three weeks later, Priya opens her second PR — a new endpoint for the goals module. You are added as a reviewer again. You read through it. She has included `ParseUUIDPipe` on every UUID path parameter. She has added composite indexes for every multi-condition query. The status codes are correct throughout. She did not need to be told again.

That is the result you were hoping for when you wrote the review comments the way you did. Not just "this PR is fixed" — but "the next PR is better because this one was reviewed well."

You mention it to Arjun on Slack later that week, casually: "Priya picked up the patterns fast." Arjun replies: "good review comments do that." You recognise what he is saying and save it.

---

### Story 3: The Sprint Where Nothing Dramatic Happened

**Background:**

Not every story is an incident. Month 7 also includes a two-week sprint where you simply do the work consistently — deliver what you said you would deliver, collaborate cleanly with Tomasz on a small cross-module concern, and contribute to planning in a way that Lucas notices.

This story matters because interviewers sometimes ask: "walk me through a typical sprint" — and the answer should demonstrate that your good work habits are not just for emergencies.

---

**S — Situation:**

Sprint 14 planning. You are asked to estimate three tickets for the upcoming sprint alongside the consent re-authentication improvement from the backlog. Tomasz also has a ticket that depends on a small API contract change in your module — he needs the `lastSyncedAt` field added to the `GET /v1/accounts/:accountId` response for a budget recalculation feature he is building.

---

**T — Task:**

Estimate your own tickets accurately, communicate the cross-module dependency clearly, and deliver on your commitments by the end of the sprint.

---

**A — Action:**

In sprint planning, you walk through each ticket using the full lifecycle estimate you learned in Month 4:

```
SPRINT 14 ESTIMATES — YOUR BREAKDOWN

Ticket 1: Add lastSyncedAt to single account endpoint
  Code change:      0.5 days  (add one field to DTO + select)
  Migration:        0 days    (field already exists, just not returned)
  Tests:            0.5 days  (update snapshot tests)
  PR description:   0.5 hours
  Review cycles:    0.5 days  (Tomasz needs to know this is ready)
  Total:            1.5 days → story point: 2

Ticket 2: Add institution logo URL to account response
  Code change:      1 day     (new field, new GoCardless call)
  Migration:        0.5 days  (new column on bank_connections)
  Tests:            1 day     (mock GoCardless response)
  PR description:   0.5 hours
  Review cycles:    1 day     (Lucas will review schema migration)
  Total:            3.5 days → story point: 5

Ticket 3: Consent check job — add 3-day notification window
  Code change:      1 day     (update shouldNotifyToday logic)
  Tests:            0.5 days  (three timing scenarios to test)
  PR description:   0.5 hours
  Review cycles:    0.5 days
  Total:            2 days → story point: 3
```

You also flag the Tomasz dependency explicitly in planning rather than waiting for it to surface during the sprint: "Ticket 1 is a dependency for Tomasz's budget recalculation work. I can have the PR open by Wednesday — is that timeline okay?" Tomasz confirms Wednesday works. Lucas notes the dependency in the sprint board with a link between the two tickets.

During the sprint:

Ticket 1 merges on Wednesday as committed. You message Tomasz in Slack: "the lastSyncedAt field is in the account response now — tagged the PR with the issue number in case you want to reference it."

Ticket 3 merges Friday of week one. One round of Lucas's review — he asks one clarifying question about the 3-day window logic, you answer directly in the PR comment, he approves.

Ticket 2 slips by half a day because the GoCardless logo API has an unexpected rate limit you did not account for. You flag it on Monday of week two: "Ticket 2 will land mid-week instead of Tuesday — GoCardless's institution logo endpoint has a 100-request-per-minute limit I did not know about. I am adding a simple in-process cache for the logo URLs to stay within the limit. ETA Wednesday." It merges Thursday.

---

**R — Result:**

All three tickets delivered within the sprint — two on schedule, one half a day late with advance notice and a clear explanation.

What Lucas says in the sprint retrospective about the sprint generally: "good dependency flagging this sprint. Catching the Tomasz dependency in planning rather than discovering it on Friday saved both of them context-switching." He does not single you out by name — the comment is about the team's planning quality — but Tomasz catches your eye during the call and gives a small nod.

The GoCardless rate limit discovery adds one item to your personal "things I have learned" document: GoCardless's institution endpoint has rate limits that the documentation does not make obvious. When you later help Priya with a GoCardless integration question in Month 8, you mention this unprompted. She does not hit the same issue because you had already hit it.

---

## What Month 7 Taught You Overall

Three things, each distinct:

**From the cache invalidation finding:** Proactive contribution is a choice you make by paying attention to what is around you, not just what is in front of you. The bug was not in your ticket. Nobody asked you to find it. You found it because you were reading carefully and you asked yourself "is this correct?" rather than "does this work for my current task?" Those are different questions. The second one is passive. The first one is ownership.

**From Priya's review:** The most useful thing Lucas ever did in your reviews was not correcting your code — it was explaining the reasoning behind each correction so you would not need to be corrected again. You reproduced that approach in your reviews of Priya's code, and the result was exactly what you were hoping for: her second PR was better than her first because her first was reviewed well. This is how knowledge compounds on a team.

**From the ordinary sprint:** Good engineering is not just incident response and dramatic fixes. Most of the job is committing accurately, communicating dependencies early, flagging slippage before it surprises someone, and delivering what you said you would. The GoCardless rate limit discovery is a microcosm of this: you hit an unexpected obstacle, you communicated it immediately with a timeline and a solution, and you delivered on the revised commitment. No drama. That is the baseline.