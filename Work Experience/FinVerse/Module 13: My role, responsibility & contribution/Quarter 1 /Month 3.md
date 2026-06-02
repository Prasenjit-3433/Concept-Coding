# Quarter 1 — Month 3: What Ownership Actually Means

---

## Foundational Knowledge: What You Need Before These Stories

Two concepts are worth understanding before these stories. Both come up directly in Month 3. The first explains why module handovers are a careful, deliberate process in a production team. The second explains a specific failure mode in distributed systems that you accidentally cause — and understanding it properly is what makes the story meaningful rather than just embarrassing.

---

### Concept 1: What Module Ownership Means in a Production System

In a tutorial project, you own everything. There is no distinction between the code you wrote today and the code you wrote three months ago — it is all yours, you understand all of it, and if something breaks you know exactly where to look.

In a production team with multiple engineers, ownership is deliberately divided. Each module has one engineer who is primarily responsible for it. This does not mean other engineers cannot touch it — it means one person is the point of contact, the expert, and the first person paged when something goes wrong.

```
WHY MODULE OWNERSHIP EXISTS

Without clear ownership:
  Bug is discovered in the accounts sync flow.
  Who investigates? Everyone looks at each other.
  Nobody knows the GoCardless edge cases.
  Nobody knows why that specific retry configuration
  was chosen six months ago.
  Investigation takes 3 hours.

With clear ownership:
  Bug is discovered in the accounts sync flow.
  You are the owner — you investigate.
  You know the GoCardless edge cases.
  You know the sync worker internals.
  You know where to look first.
  Investigation takes 20 minutes.
```

Ownership means being the person who:
- Knows the module's code deeply — not just what it does, but why it does it that way
- Is the first responder when something breaks in that module
- Reviews PRs from others that touch that module
- Understands the known edge cases and historical decisions
- Onboards new engineers into that module's quirks

Ownership does not mean:
- You wrote every line of code in it
- Nobody else can touch it without your permission
- You are solely blamed if something goes wrong

When Elena hands over the Accounts & Open Banking module to you, she is not just explaining the code. She is transferring all of that contextual knowledge — the GoCardless edge cases, the sync patterns, the historical decisions — that does not live in any file but is essential for the module to function reliably in production.

---

### Concept 2: The Outbox Pattern — Why Writing to a Database and Publishing to RabbitMQ Are Not the Same Thing

This is the concept behind the deployment incident you cause in Month 3. Understanding it properly is what turns the mistake from an embarrassing story into a genuinely educational one.

When your code does two things — writes to a database AND publishes a message to RabbitMQ — these are two separate operations. They are not wrapped in the same transaction. One can succeed while the other fails.

```
THE PROBLEM — TWO SEPARATE OPERATIONS

Your code:
  Step 1: prisma.bankConnection.update({ status: 'ACTIVE' })
  Step 2: prisma.bankAccount.createMany({ data: accounts })
  Step 3: rabbitMQ.publish('bank.connection.completed', payload)

What can go wrong:
  Steps 1 and 2 succeed — database updated ✓
  Step 3 fails  — RabbitMQ publish throws ✗

  OR worse:
  Steps 1, 2, and 3 all succeed individually —
  but Step 3 is accidentally removed during a code change.
  Database is updated ✓
  Event is never published ✗
  Notification Service never receives the completion event.
  User never gets their "bank connected" push notification.
  Support tickets follow.
```

The Outbox Pattern is the proper solution to this problem. Instead of publishing directly to RabbitMQ, you write the event to an `outbox_events` table in PostgreSQL — in the same database transaction as your business data. A separate background process (the outbox publisher) then reads from that table and publishes to RabbitMQ.

```
THE OUTBOX PATTERN — HOW IT SOLVES THE PROBLEM

Step 1: Open a PostgreSQL transaction
Step 2: Update bankConnection status → ACTIVE
Step 3: Create bankAccount records
Step 4: Write to outbox_events table:
          { eventType: 'bank.connection.completed', payload: {...} }
Step 5: Commit transaction

All four steps succeed together or none of them do.
The event record is now in the database.
Even if RabbitMQ is temporarily down,
the outbox publisher will pick it up and publish it later.
The database state and the intent to notify are always consistent.
```

You do not implement the Outbox Pattern correctly in your first attempt at Month 3 — you accidentally remove the outbox event write entirely during a refactor. Understanding *why* it matters is what makes Lucas's post-incident feedback land so clearly.

---

## The Stories

---

### Story 1: The Handover — Learning From Someone Who Knows

**Background:**

Three weeks into Month 3, Daniel and Lucas jointly decide you are ready to take formal ownership of the Accounts & Open Banking module. Elena has been maintaining it alongside her primary work on the Investing module — it has never had a dedicated owner. You are the right person because the module feeds directly into the Transactions module (Tomasz's work), and you have been sitting closest to that boundary.

Elena runs the handover session. It is not a meeting — it is a 90-minute deep dive. She treats it like teaching, not like handing off a burden.

---

**S — Situation:**

It is week nine. Daniel confirms the handover in a Slack message to the team: *"From this week, the Accounts & Open Banking module has a new owner. Elena will shadow for one week and then hand over fully."*

You join the handover call with your notebook open and a list of questions you prepared the night before. You have read the module's code already — you understand the structure. But you know there is a difference between understanding code and understanding a module in production. The code tells you what happens. Elena will tell you why, and what breaks when things go wrong.

---

**T — Task:**

Absorb everything Elena knows about the module — the GoCardless integration, the sync patterns, the known edge cases, the historical decisions — so you can own it independently from week ten onwards.

---

**A — Action:**

Elena spends the first 20 minutes on the GoCardless integration flow. Not the happy path — she spends most of this time on the things that break.

She explains three specific edge cases she has discovered over her months maintaining the module:

**Edge case 1 — Banks that return malformed IBAN data:**

```
Some German banks (specifically a few regional Sparkasse branches)
return account IBANs with extra whitespace or lowercase letters.

Our schema expects: DE89370400440532013000
Some banks return: de89 3704 0044 0532 0130 00

If you store this directly, downstream queries that filter
by IBAN will fail silently — the comparison will not match.

The fix Elena added: a normalisation step in the account
creation code that strips whitespace and uppercases the IBAN
before storing it.

Where it lives: account.service.ts → normaliseIban()
```

**Edge case 2 — GoCardless rate limits during initial syncs:**

```
GoCardless allows 50 API requests per second per API key.

During a campaign when many users connect their banks
simultaneously, the INITIAL_SYNC jobs fan out concurrent
GoCardless calls that can hit this limit.

GoCardless returns 429 when this happens.
The BullMQ retry logic handles it — but the rate limiter
configuration on the worker is what prevents the retry
storm from making things worse.

Where it lives: transaction-sync.worker.ts → limiter config
Do not change the limiter values without understanding
the GoCardless rate limit implications.
```

**Edge case 3 — Accounts that fail to reconnect after consent expiry:**

```
GoCardless PSD2 consent windows expire after 90 days.
After expiry, the access tokens become invalid and syncs fail.

When a user reconnects, a new requisition is created.
But the old bankAccount records still exist in the database
pointing to the old requisitionId.

If you upsert on externalAccountId (the GoCardless account ID),
the old records get reactivated correctly.
If you create new records, you end up with duplicates.

The upsert approach is intentional — see the comment
in handleCallbackSuccess explaining why.
Never replace it with a plain create.
```

Elena then walks through the BullMQ job structure — the difference between `INITIAL_SYNC` (fires once when a user first connects) and `PERIODIC_SYNC` (fires every four hours for all active users). She explains the SyncLog model and how to read it when users report sync issues. She shows you the Bull Board admin interface and how to replay failed jobs.

The shadowing week that follows is equally valuable. Two support tickets come in during the week. Elena handles both — but you observe the full investigation each time. In the first one, a user's Deutsche Bank accounts stopped syncing. Elena opens Datadog, finds the sync logs, identifies an expired consent, and explains to the user how to reconnect. In the second, a user's balance is not updating. Elena traces it to a GoCardless rate limit error that caused the periodic sync to fail twice before succeeding on the third attempt.

By the end of the shadow week, you have seen real failure patterns. Not just code — behaviour in production.

---

**R — Result:**

You take formal ownership at the start of week ten. Elena is available for questions but is no longer the first responder.

What you carried from the handover into the rest of your contract was not the code — you had already read the code. It was the institutional knowledge that only Elena had: the Sparkasse IBAN normalisation, the GoCardless rate limit sensitivity, the upsert-not-create pattern on reconnection. None of this was documented anywhere. If Elena had left the company without a handover, you would have discovered each of these through painful production incidents. Instead, you discovered them in a 90-minute call with someone who wanted you to succeed.

This experience shaped how you handled Priya's onboarding eight months later. When she joined and was assigned to a module you were adjacent to, you proactively told her the two edge cases you knew she would hit before she hit them. You remembered what Elena's handover had been worth.

---

### Story 2: The First Ownership Investigation

**Background:**

Three days after taking formal ownership, a support ticket arrives. A user reports that their Deutsche Bank accounts are not syncing — their transaction list has not updated in several days. The user is Premium. The ticket is escalated.

This is your problem now. Not Lucas's. Not Elena's. Yours.

---

**S — Situation:**

It is week ten. The ticket is assigned to you with a note from Daniel: *"Module owner should investigate."* You have never handled a support ticket independently before. You have watched Elena do it twice during the shadow week, but watching is different from doing.

---

**T — Task:**

Investigate why the user's Deutsche Bank accounts are not syncing, identify the root cause, and propose a fix. Bring the finding to Lucas with evidence and a proposed solution — not just the problem.

---

**A — Action:**

**Step 1 — Find the sync logs in Datadog:**

You open Datadog Log Explorer and filter by the user's ID from the support ticket. You look for log lines from the `transaction-sync` queue worker. The most recent sync attempt for this user shows:

```
14:22:11.421  ERROR  [TransactionSyncWorker]
  Sync failed for account acc_789
  { userId: usr_ticket, accountId: acc_789,
    error: "GoCardless: account access revoked — consent expired",
    statusCode: 403 }
```

GoCardless is returning a 403 — access revoked. This matches edge case 3 from Elena's handover: consent expiry.

**Step 2 — Verify in the database:**

You check the `BankConnection` record for this user. The `consentExpiresAt` field shows a date 12 days ago. The `status` is still `ACTIVE` — the status has not been updated because there is no automated check for expired consent. The connection looks active in the database but GoCardless has already revoked the access tokens.

**Step 3 — Find the deeper problem:**

This is where it gets more interesting. The consent expired 12 days ago. Why did it take 12 days for a support ticket to arrive? Because the sync failures have been silently retrying. BullMQ retried the job three times after each failure, all failed, and moved the job to the `failed` state in Bull Board. But there is no alert configured for expired consent failures — they look identical to transient GoCardless errors in the current monitoring setup.

You look at the BullMQ `PERIODIC_SYNC` job scheduling. The repeatable job that fires every four hours for all active users — you find it registered in the scheduler. But the registration code does not filter out connections where `status !== 'ACTIVE'`. It queries all users with `isActive: true` bank accounts. Since the `bankConnection.status` was never updated from `ACTIVE` to `EXPIRED`, this user keeps getting queued for syncs that will always fail.

You have found two things:
1. The immediate problem: this user's consent expired and nobody told them to reconnect
2. The underlying problem: the system does not update `bankConnection.status` when GoCardless returns a 403, so the periodic sync keeps retrying a connection that will never work

**Step 4 — Bring it to Lucas with a proposal:**

You write a structured Slack message to Lucas. Not a question — a finding with a proposed next step:

*"Investigated the Deutsche Bank sync ticket. Root cause: GoCardless consent expired 12 days ago. The bankConnection.status is still ACTIVE because we have no logic to update it when GoCardless returns a 403 access-revoked error. The periodic sync scheduler keeps queuing this user because it only filters on bankAccount.isActive, not bankConnection.status.*

*Two things need fixing:*
*1. Immediate: tell the user to reconnect via support. Their data is not lost — just the access to GoCardless.*
*2. Longer term: when the sync worker receives a GoCardless 403 access-revoked error, it should update bankConnection.status to EXPIRED rather than retrying. This would stop the scheduler from queuing hopeless retries and would surface the issue clearly.*

*I can build fix 2 if you want me to. Happy to discuss the approach first."*

Lucas responds within 20 minutes:

*"Correct diagnosis. Build fix 2 — update the status to EXPIRED when we receive a 403 access-revoked. Add a Datadog log line at WARN level when this happens so we can see it in monitoring. Do not add an automated notification to the user yet — that is a product decision, not just an engineering one. Bring the PR to me when ready."*

You build the fix. When the sync worker receives a GoCardless 403 with the `account access revoked` error message, it now updates the `bankConnection.status` to `EXPIRED` rather than treating it as a retryable error. You also add a `WARN` level log line so Datadog can surface these without the on-call engineer having to search for them.

The PR goes through one round of review. Lucas adds one comment — asking you to also update the `syncStatus` on the `bankAccount` record to `FAILED` with a clear `lastSyncError` message so the mobile app can surface the reconnection prompt to the user. You add it. The PR merges.

---

**R — Result:**

The fix ships to production. The affected user is contacted by support and reconnects their bank account. Their sync resumes.

More importantly: you found the problem independently, understood it at two levels of depth (the immediate symptom and the underlying design gap), and brought Lucas a proposal rather than a question. This is what ownership looks like — not just fixing what is reported, but understanding why it happened and stopping it from happening again silently.

Lucas mentioned it in your end-of-month 1:1: *"Good investigation. You found the problem, you understood the cause, and you came to me with a solution rather than asking me to solve it. That is how ownership is supposed to work."*

---

### Story 3: The Deployment Incident — What You Broke and What You Learned

**Background:**

In the final week of Month 3, you are building a small improvement to the GoCardless callback handler — the code that runs after a user completes the bank consent flow and GoCardless redirects back to FinVerse. You are adding better error handling for the case where GoCardless returns an unexpected status code during the requisition confirmation step.

The change is small. You are confident in it. You test the happy path locally. You open a PR, Lucas reviews and approves it, and it deploys to production on a Thursday afternoon.

Two hours later, Lucas pings you on Slack: *"Something is wrong with bank connections in staging. Check your recent changes."*

---

**S — Situation:**

It is Thursday afternoon of week twelve. The deployment has been live for two hours. Two users in staging who connected their bank accounts after the deployment are stuck — their connections are showing `PENDING` status in the database, not `ACTIVE`. Their accounts were never created. They received no completion notification.

Lucas has narrowed it to your recent deployment. He does not tell you what is wrong — he tells you to find it.

---

**T — Task:**

Find what your change broke, understand why, fix it, and write a test that would have caught the problem before deployment.

---

**A — Action:**

**Step 1 — Read the logs:**

You open Datadog and filter for `bank.connection.completed` events in the outbox table in staging. There are none in the past two hours — the period since your deployment. Before your deployment, these events appear regularly whenever a user connects a bank.

The event is not being written to the outbox. That means the outbox publisher never picked it up, RabbitMQ never received it, and Notification Service never sent the completion push notification. The user's connection stayed in `PENDING` because the callback that should have updated it to `ACTIVE` and created the account records never fired the outbox event.

**Step 2 — Read your own code:**

You open the diff from your PR and read it carefully. Your change added a new early-return path for unexpected GoCardless status codes. You wrapped the main success path inside an `if` block. The logic is correct — but you accidentally moved the `outboxEvent.create()` call to a position outside the `prisma.$transaction()` block.

Before your change:

```typescript
// BEFORE — outbox event written inside the transaction
await this.prisma.$transaction(async (tx) => {
  await tx.bankConnection.update({ ... })
  await tx.bankAccount.createMany({ ... })
  await tx.outboxEvent.create({
    data: {
      eventType: 'bank.connection.completed',
      payload: { userId, ... }
    }
  })
})
```

After your change — accidentally broken:

```typescript
// AFTER — outbox event accidentally removed during refactor
await this.prisma.$transaction(async (tx) => {
  await tx.bankConnection.update({ ... })
  await tx.bankAccount.createMany({ ... })
  // ← outboxEvent.create() was accidentally deleted here
  //   during the refactor. The logic moved but the
  //   outbox write did not come with it.
})

// The new error handling code you added correctly
// But the outbox write is simply gone
if (unexpectedStatus) {
  await this.prisma.bankConnection.update({ status: 'ERROR' })
  return
}
```

You deleted the outbox event write by accident during the refactor. The `bankConnection` and `bankAccount` records are being created correctly — the database updates work. But the event that tells the rest of the system "a bank was connected" is silently gone.

**Step 3 — Fix it:**

The fix is straightforward — restore the `outboxEvent.create()` call inside the transaction. You also restructure the code so the error handling path is clearly separated from the success path, making it harder to accidentally delete something important from one while editing the other.

```typescript
// FIXED — outbox event restored inside transaction
await this.prisma.$transaction(async (tx) => {
  await tx.bankConnection.update({
    where: { id: pendingConnection.id },
    data: {
      status: 'ACTIVE',
      consentExpiresAt: new Date(
        Date.now() + 90 * 24 * 60 * 60 * 1000
      )
    }
  })

  for (const detail of accountDetails) {
    await tx.bankAccount.upsert({
      where: { externalAccountId: detail.id },
      update: { isActive: true },
      create: { ... }
    })
  }

  // Outbox event — restored in the correct position
  await tx.outboxEvent.create({
    data: {
      eventType: 'bank.connection.completed',
      payload: { userId, accountCount: accountDetails.length }
    }
  })
})
```

**Step 4 — Write the test that would have caught it:**

Lucas made this explicit: *"Write a test that would have caught this before the deployment."*

The test verifies that after a successful callback, an outbox event exists in the database:

```typescript
it('should write a bank.connection.completed outbox event after successful callback', async () => {
  // Arrange: set up a pending connection in the database
  const pendingConnection = await createPendingConnection(userId)

  // Mock GoCardless to return a successful response
  goCardlessMock.getRequisition.mockResolvedValue({
    status: 'LN',
    accounts: ['acc_external_1', 'acc_external_2']
  })
  goCardlessMock.getAccountDetails.mockResolvedValue(mockAccountDetail)

  // Act: trigger the callback handler
  await accountService.handleCallbackSuccess(userId)

  // Assert: outbox event was written
  const outboxEvent = await prisma.outboxEvent.findFirst({
    where: {
      eventType: 'bank.connection.completed',
      payload: { path: ['userId'], equals: userId }
    }
  })

  // This assertion would have FAILED on your broken code
  // and caught the problem before production deployment
  expect(outboxEvent).not.toBeNull()
  expect(outboxEvent?.status).toBe('PENDING')
})
```

You add this test to the PR alongside the fix.

**Step 5 — Brief retrospective with Lucas:**

After the fix deploys, Lucas runs a five-minute retrospective with you over Slack. Not a formal meeting — just a direct conversation.

*"What would have caught this before it deployed?"*

You: *"A test that asserted the outbox event is written after a successful callback. I only tested that the database records were created correctly — I did not test that the downstream notification chain was intact."*

Lucas: *"Right. The outbox event is as important as the database record. If you do not test both, you have tested half the feature. What is your personal checklist item from this?"*

You think for a moment: *"After any change to a flow that involves both database writes and event publishing — test both. Not just the data, but the events."*

Lucas: *"Add it to your checklist. This will not be the last time you touch a flow like this."*

---

**R — Result:**

The fix deployed the same evening. The two affected staging users had their connections manually corrected. No production users were affected — the incident was caught in staging before the weekly Thursday production release.

The test you wrote was added to the module's test suite and stayed there for the rest of your contract. When Priya later made a change that touched the callback handler, the test caught a different but related issue before her PR merged. The test outlasted the incident that created it.

What you took from this incident was more precise than anything from Month 1 or 2. Not a general lesson about being careful — a specific, permanent addition to your personal deployment checklist:

*"Any flow that writes to the database AND publishes an event must have tests for both. The database write and the event are equally part of the feature. Testing one without the other is testing half a feature."*

You wrote this in your Notion page. You did not need to be reminded of it again.

---

## What Quarter 1 Taught You Overall

Three months in, you had gone from staring at a 40-file codebase you barely understood to owning a production module, independently investigating real support tickets, and deploying changes to staging — including one change that broke something and taught you more than any clean success could have.

Looking back across the three months, the pattern is clear:

**Month 1** was about learning the professional context — code reviews, migrations, how a real team operates. The mistakes were small and the lessons were foundational.

**Month 2** was about building real things and discovering the gap between working code and production-grade code. The ownership check and the `select` lesson shaped every Prisma query you wrote afterward.

**Month 3** was about what ownership actually feels like — the weight of being the person responsible, the value of knowledge transfer from Elena, and the specific sharpness of a lesson learned from a real deployment incident.

The through-line across all three months was not technical skill. It was learning to operate professionally — to receive feedback without defensiveness, to bring findings with proposed solutions, to ask for help precisely, and to document what you learned so you would not need to learn it twice.

That foundation is what everything in Quarter 2 was built on.

---

*Quarter 2 is next — Month 4 begins with your first significant feature built end to end, and the sprint estimation lesson that permanently changed how you think about engineering work.*