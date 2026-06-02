# Quarter 1 — Month 2: Building Something Real

---

## Foundational Knowledge: What You Need Before These Stories

Two concepts come up directly in Month 2 that are worth understanding before the stories. Both will feel obvious in hindsight — but without them, the actions in the stories will seem routine rather than meaningful.

---

### Concept 1: Why Ownership Checks Are Non-Negotiable in Financial APIs

When you build a project by yourself, authentication is usually the only security concern you think about. If someone is logged in, they can access things. If they are not logged in, they cannot. That mental model is incomplete — and in a financial application, the gap between "logged in" and "can access this specific resource" is exactly where serious security bugs live.

The problem has a name: **broken object-level authorisation**. It is consistently one of the top vulnerabilities in real-world APIs. Here is what it looks like in practice:

```
THE PROBLEM — NO OWNERSHIP CHECK

GET /v1/accounts/acc_456
Authorization: Bearer <valid JWT for usr_123>

Without an ownership check:
  → The JWT is valid ✓
  → The account acc_456 exists ✓
  → Response: returns acc_456's data ✗

But acc_456 belongs to usr_789, not usr_123.
usr_123 just read someone else's bank account data.

With an ownership check:
  → The JWT is valid ✓
  → Does acc_456 belong to usr_123? NO ✗
  → Response: 404 Not Found

The 404 is intentional — not 403 Forbidden.
A 403 tells the attacker the account exists.
A 404 tells them nothing useful.
```

In a financial application, this is not a theoretical concern. If a user can enumerate account IDs — and UUIDs are not as hard to guess as you might think when attackers have thousands of tries — they can read balances, transactions, and personal financial data belonging to other users.

The fix is one line in every Prisma query that returns user-specific data:

```typescript
// WRONG — any authenticated user can read any account
const account = await prisma.bankAccount.findUnique({
  where: { id: accountId }
})

// CORRECT — only the owning user can read this account
const account = await prisma.bankAccount.findFirst({
  where: {
    id: accountId,
    userId: userId   // ← this is the ownership check
  }
})
```

The `userId` comes from the decoded JWT — injected by the API Gateway into every request as a trusted header. It is not something the client can spoof. The query says: "find an account with this ID *and* owned by this user." If the account exists but belongs to someone else, `findFirst` returns `null`, and you return a 404.

This pattern — always filtering by `userId` on every query that returns user-specific data — becomes a reflex by the end of your contract. But it starts as something a senior engineer has to catch in your first code review.

---

### Concept 2: Payload Efficiency — Why `select` Matters on Mobile Clients

When you build a personal project, you typically fetch everything from the database and return it all to the frontend. The frontend picks out what it needs. This works fine when you are building for a desktop browser on a fast connection and you are the only user.

In production, this approach has real costs:

```
THE PROBLEM WITH FETCHING EVERYTHING

A bank account record in PostgreSQL has ~20 fields:
  id, userId, bankConnectionId, externalAccountId,
  iban, accountName, institutionName, accountType,
  currency, currentBalance, availableBalance,
  isActive, syncStatus, lastSyncedAt, lastSyncError,
  syncRetryCount, createdAt, updatedAt...

The mobile app's account list screen needs 8 of them:
  id, accountName, institutionName, accountType,
  currency, currentBalance, syncStatus, lastSyncedAt

Using `include` (fetches everything):
  → Transfers ~800 bytes per account over the network
  → Exposes internal fields like syncRetryCount, lastSyncError
    that the frontend has no business seeing
  → Loads unnecessary data into Node.js memory

Using `select` (fetches only what you need):
  → Transfers ~300 bytes per account
  → No internal fields exposed
  → Less memory pressure in the Node.js process
```

On a desktop browser with a fast Wi-Fi connection, the difference between 800 bytes and 300 bytes per account is invisible. On a React Native app running on 4G in rural Germany, multiplied across thousands of concurrent users, it compounds. Bandwidth costs money. Battery life matters. Load times affect whether users trust the app.

The pattern is simple:

```typescript
// WRONG — fetches all 20 fields
const accounts = await prisma.bankAccount.findMany({
  where: { userId },
  include: { bankConnection: true }
})

// CORRECT — fetches only the 8 fields the mobile app needs
const accounts = await prisma.bankAccount.findMany({
  where: { userId },
  select: {
    id:              true,
    accountName:     true,
    institutionName: true,
    accountType:     true,
    currency:        true,
    currentBalance:  true,
    syncStatus:      true,
    lastSyncedAt:    true,
  }
})
```

This is your second code review lesson — and it comes from the same PR as the ownership check. Both are caught by Lucas before the code reaches production.

---

## The Stories

---

### Story 1: The First Endpoint — And the Security Bug Lucas Caught

**Background:**

By week five, you have been observing how the Accounts module works and you are ready for a more substantial task. Lucas assigns you `GET /v1/accounts/:accountId/sync-status` — a new endpoint that lets the mobile app poll the current sync state for a specific bank account. The app shows a loading indicator while a sync is running and uses this endpoint to know when it finishes.

This is your first endpoint built mostly from scratch. Not a bug fix, not a field addition — a complete controller method, service method, and response DTO. You are responsible for the design, the implementation, and the tests.

---

**S — Situation:**

It is week five. You have read through several existing endpoints in the Accounts module to understand the patterns. You understand the controller-service separation, how the `@UserId()` decorator extracts the user ID from the JWT, and how DTOs shape the response. You feel ready.

You write the endpoint over two days. The logic is straightforward — find the bank account by ID, find the most recent SyncLog record for that account, and return both in a combined response. You write tests for the happy path. You open a PR.

The PR description is better than your first one — you include what the endpoint does, what fields it returns, and a curl command showing how to test it locally. You request Lucas's review.

---

**T — Task:**

Build the sync status endpoint correctly — including proper security, correct HTTP semantics, and mobile-appropriate response shape — and get it through review without a second round of structural feedback.

---

**A — Action:**

Here is the first version of your service method, before Lucas's review:

```typescript
// BEFORE — first attempt (missing ownership check)
async getSyncStatus(accountId: string): Promise<SyncStatusResponse> {
  const account = await this.prisma.bankAccount.findUnique({
    where: { id: accountId },
    select: {
      id:          true,
      syncStatus:  true,
      lastSyncedAt: true,
      lastSyncError: true,
    }
  })

  if (!account) {
    throw new NotFoundException('ACCOUNT_NOT_FOUND')
  }

  const lastSync = await this.prisma.syncLog.findFirst({
    where: { bankAccountId: accountId },
    orderBy: { startedAt: 'desc' },
  })

  return {
    accountId:    account.id,
    syncStatus:   account.syncStatus,
    lastSyncedAt: account.lastSyncedAt?.toISOString() ?? null,
    lastSync: lastSync ? {
      status:               lastSync.status,
      transactionsFetched:  lastSync.transactionsFetched,
      transactionsInserted: lastSync.transactionsInserted,
      startedAt:            lastSync.startedAt.toISOString(),
      completedAt:          lastSync.completedAt?.toISOString() ?? null,
    } : null,
  }
}
```

You are also using `include` in a different part of the same PR for the account list endpoint — fetching all fields from the `bankAccount` table and its related `bankConnection`.

Lucas's review comes back with two comments.

**Comment 1 — The ownership check:**

Lucas's comment on the `findUnique` call:

*"This is missing the `userId` filter. As written, any authenticated user can pass any account ID and read its sync status — even accounts belonging to other users. Always filter by `userId` on every query that returns user-specific data. The userId comes from the JWT, not from the request body or URL — it cannot be forged by the client."*

You read the comment and immediately understand the problem. You had checked that the user was authenticated — the JWT guard was in place. But you had not checked that the authenticated user was the *owner* of the account they were querying. Those are two completely different things.

You update the query:

```typescript
// AFTER — with ownership check
const account = await this.prisma.bankAccount.findFirst({
  where: {
    id:     accountId,
    userId: userId,     // ← ownership check added
    isActive: true,     // ← also filter soft-deleted accounts
  },
  select: {
    id:          true,
    syncStatus:  true,
    lastSyncedAt: true,
  }
})

if (!account) {
  // Return 404 regardless of whether the account exists
  // but belongs to another user — no information leakage
  throw new NotFoundException(
    'ACCOUNT_NOT_FOUND',
    'Bank account not found or access denied'
  )
}
```

You also notice something while making this fix: the error message says "not found *or* access denied." You ask Lucas why not just say "access denied" when the account belongs to another user. His reply: *"Because if you say 'access denied,' you've just told the attacker that the account ID exists. A 404 with an ambiguous message tells them nothing."* You add this to your Notion page.

**Comment 2 — `include` versus `select`:**

Lucas's comment on the account list query:

*"You're using `include: { bankConnection: true }` which fetches every column from both tables. The mobile app account list screen needs maybe 8 fields. Use `select` and specify exactly what you need. Three reasons: smaller payload over the network, no internal fields accidentally exposed to the client, and less memory pressure on the Node.js process."*

You update the account list query to use `select` with only the fields the mobile app actually renders. The response payload shrinks noticeably.

The PR goes through one more round — Lucas asks one clarifying question about why you chose to return `lastSyncError` in the response at all, since it is an internal diagnostic field that means nothing to the mobile app. You remove it. The PR merges on the third day.

---

**R — Result:**

The endpoint is live in staging by the end of week five. The security vulnerability — a missing ownership check — was caught before it ever reached production.

What mattered more than the fix was the reasoning Lucas gave. You did not just learn "add `userId` to the query." You understood *why* — the difference between authentication (who are you?) and authorisation (are you allowed to do this specific thing?). That understanding became a reflex. Every single Prisma query you wrote for the rest of your contract included the `userId` filter on user-specific data, and you caught the same mistake in Priya's PR eight months later without needing to think about it.

The `select` lesson also stuck. From that point forward, you read every Prisma query you wrote and asked: "am I fetching more than the caller actually needs?" More often than you expected, the answer was yes.

---

### Story 2: The Budget Alert Duplicate — First Cross-Team Coordination

**Background:**

In week six, a pattern of support tickets catches Lucas's attention. Users are occasionally receiving two identical budget alert push notifications within seconds of each other for the same spending event. One transaction pushes them over the budget threshold and they get the same alert twice.

Lucas does some initial investigation and narrows the cause to the Core Product side — specifically in how the `budget.threshold.exceeded` event is being published to RabbitMQ. Under certain timing conditions during a transaction sync, the event is being published twice. He assigns the investigation to you.

This is not a module you built or own — it sits in Tomasz's Budgeting module. But the publishing logic is upstream of that, inside the sync worker flow, which overlaps with the Accounts module work you have been doing. Lucas thinks it is a good next step for you.

---

**S — Situation:**

It is week six. You have been assigned to find why the `budget.threshold.exceeded` event is occasionally published twice for the same threshold crossing, and to fix it within Core Product.

You have not investigated a real production bug using Datadog before. You know Datadog exists — you have seen the dashboards in team Slack — but you have never used it to trace a specific problem.

---

**T — Task:**

Find the root cause of the duplicate event publishing within Core Product, propose a fix, coordinate with Tomasz to verify it resolves the duplicate notification delivery, and open a PR.

---

**A — Action:**

**Step 1 — Finding the problem in the logs:**

Lucas gives you one piece of guidance before you start: "Filter Datadog logs by a specific affected user ID and look at the timeline around when the duplicate notifications fired. The pattern should be visible in the logs."

You find a support ticket with a specific user ID and approximate timestamp. You open Datadog Log Explorer for the first time and type a filter: `service:core-product @userId:usr_affected`. You set the time window to cover the period when the duplicate was reported.

The log sequence that comes back:

```
14:19:23.421  INFO  [TransactionSyncWorker]
  Sync completed — 12 transactions inserted
  { userId: usr_affected, accountId: acc_001 }

14:19:23.435  INFO  [BudgetService]
  Budget threshold exceeded — publishing event
  { userId: usr_affected, category: Groceries }

14:19:23.437  INFO  [RabbitMQPublisher]
  Event published: budget.threshold.exceeded
  { userId: usr_affected }

14:19:23.441  INFO  [TransactionSyncWorker]
  Sync completed — 12 transactions inserted    ← duplicate?
  { userId: usr_affected, accountId: acc_001 }

14:19:23.455  INFO  [BudgetService]
  Budget threshold exceeded — publishing event  ← again
  { userId: usr_affected, category: Groceries }

14:19:23.457  INFO  [RabbitMQPublisher]
  Event published: budget.threshold.exceeded    ← twice
  { userId: usr_affected }
```

The sync worker is completing twice for the same account within 20 milliseconds. Both completions trigger the budget check and both publish the event. You need to understand why the sync is completing twice.

**Step 2 — Tracing the duplicate sync:**

You look at the BullMQ job configuration for the `transaction-sync` queue. You find the job enqueue logic in the callback handler — the code that runs after a user connects their bank account and GoCardless redirects back. It enqueues an `INITIAL_SYNC` job for each account.

You notice something. The job does not use a deterministic `jobId`. Without a `jobId`, BullMQ does not deduplicate — if the same job is enqueued twice, both run. And looking at the callback handler code, you find the problem:

```typescript
// BEFORE — no deduplication (the bug)
await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId, accountIds },
  {
    priority: 1,
    attempts: 3,
    backoff: { type: 'exponential', delay: 5000 }
    // ← no jobId — BullMQ will happily enqueue this twice
  }
)
```

In rare cases — usually when a user's mobile connection is slow and the GoCardless redirect fires twice, or when the user taps "connect" again before the first callback processes — two `INITIAL_SYNC` jobs end up in the queue for the same user and the same account. Both run. Both complete. Both trigger the budget check. Both publish the event.

The fix: add a deterministic `jobId` so BullMQ silently ignores the second enqueue if a job with that ID already exists in the queue.

```typescript
// AFTER — with deduplication via deterministic jobId
await this.syncQueue.add(
  'INITIAL_SYNC',
  { userId, accountIds },
  {
    jobId:    `initial-sync-${userId}`,  // ← deterministic, user-scoped
    priority: 1,
    attempts: 3,
    backoff: { type: 'exponential', delay: 5000 }
  }
)
```

Now if the callback fires twice, the second `queue.add()` call finds a job with `initial-sync-usr_123` already in the queue and does nothing. One sync runs. One budget check runs. One event published.

**Step 3 — Coordinating with Tomasz:**

Before opening a PR, you message Tomasz on Slack. Not to ask permission — the fix is entirely within your module's code. But to make sure he understands what is happening and can verify the fix resolves the duplicate delivery on the Notification Service side.

You write:

*"Hey Tomasz — found the root cause of the duplicate budget alert issue. The `INITIAL_SYNC` BullMQ job was being enqueued without a deterministic jobId, so under certain timing conditions the same sync ran twice and published the event twice. Fix is a one-line jobId addition in the callback handler. Can you confirm from your side what the duplicate delivery pattern looked like in Notification Service logs? Want to make sure we're looking at the same root cause before I open the PR."*

Tomasz responds within the hour with a log snippet from the Notification Service showing two identical `budget.threshold.exceeded` events consumed within milliseconds. He confirms the root cause matches — duplicate events in, duplicate notifications out. He says: *"Fix looks correct from my side. Once your PR merges, I'll keep an eye on the queue for a few days to confirm duplicates stop."*

You open the PR. The description explains the root cause, includes the Datadog log snippet as evidence, and describes the fix. You add a test that verifies calling `queue.add()` twice with the same `jobId` results in only one job being processed.

Lucas reviews. Two comments — one clarifying question about whether `initial-sync-${userId}` is sufficiently unique (you explain: one initial sync per user, ever — subsequent syncs use `PERIODIC_SYNC` with a different job type), and one positive observation about the PR description quality. He approves.

---

**R — Result:**

The fix merges at the end of week six. Tomasz monitors the Notification Service for the following week and confirms duplicate delivery drops to zero.

Three things you took from this investigation:

First — **how to use Datadog to investigate a real bug.** Not theoretically, but in practice: filter by user ID, look at the timeline, follow the sequence of events until the pattern emerges. This investigation workflow became your default approach for every bug you investigated afterward.

Second — **the value of a deterministic jobId in BullMQ.** You had read about it during the BullMQ onboarding material, but it felt abstract. Seeing the exact failure mode it prevents — two jobs, two syncs, two events, two notifications — made it concrete. From that point you added a deterministic `jobId` to every `queue.add()` call you wrote, without needing to be reminded.

Third — **what good cross-team coordination looks like.** You did not open a PR and tag Tomasz as a reviewer and wait. You messaged him first, explained what you found, asked him to confirm the root cause matched what he was seeing on his side, and only then opened the PR. The result was that by the time the PR was open, Tomasz already understood the fix and had confirmed it addressed the right problem. One-round review, no back-and-forth confusion.

---

## What Month 2 Taught You Overall

Two months in, you had built your first endpoint from scratch, had a security vulnerability caught in review before it reached production, and investigated and fixed your first production bug using Datadog.

The security lesson — the gap between authentication and authorisation — was the most important technical thing you learned in Month 2. Not because the fix was hard, but because once you understood *why* the ownership check was necessary, you never forgot it. It became a lens you applied to every query you wrote from that point forward.

The cross-team coordination experience taught you something equally important: in a professional team, the social process around a fix is often as important as the fix itself. Telling Tomasz what you found before opening a PR — asking him to confirm the root cause rather than assuming — meant the entire fix went smoothly with no confusion, no back-and-forth, and no wasted review cycles.

---
