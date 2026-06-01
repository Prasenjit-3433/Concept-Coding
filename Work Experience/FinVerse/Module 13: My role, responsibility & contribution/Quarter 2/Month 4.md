# Month 4: First Feature, First Ownership

---

## Theme: *Building something real, end to end*

---

## Foundational Knowledge: What You Need Before These Stories

One concept worth understanding before diving into Month 4's stories — because it comes up directly in the sprint planning story.

### The Full Lifecycle of a Code Change

In your first three months, you estimated tasks by thinking about how long the code itself would take to write. This is the most common estimation mistake junior engineers make. The code is only one part of shipping a change.

```
THE FULL LIFECYCLE OF A CODE CHANGE
(what you actually need to estimate)

1. Understanding the requirement
   Reading the ticket, asking clarifying questions,
   understanding the edge cases.

2. Writing the code
   The part junior engineers estimate correctly.

3. Writing tests
   Unit tests for the happy path.
   Unit tests for the edge cases.
   Integration tests if needed.

4. Database migration (if schema changes)
   Writing the migration, verifying it does not break
   existing queries, testing it locally.

5. PR description
   Writing a clear description — what changed, why,
   how to test it locally.

6. Code review cycle
   Waiting for review, addressing comments, possibly
   multiple rounds before approval.

7. Cross-service or cross-module coordination
   If your change affects another team's service,
   you need to coordinate with them.

8. Staging deployment and smoke test
   Verifying your change works in staging with real data,
   not just locally with seeded test data.

9. Monitoring after production deployment
   Watching Datadog for unexpected errors or metric
   changes in the hour after your change goes live.
```

Most junior engineers estimate steps 2 and 3. Experienced engineers estimate all nine.

---

## The Stories

---

### Story 1: The Consent Expiry Feature — First End-to-End Ownership

**Background:**

GoCardless PSD2 consent windows expire after 90 days. When a user connects their bank account, FinVerse receives permission to read their transaction data for 90 days. After that, the connection silently breaks — sync jobs start failing, balances stop updating, the user sees stale data — until the user manually reconnects.

Before this feature, FinVerse had no proactive handling of this. Users only found out their bank had disconnected when they noticed their transaction list had stopped updating. Support tickets would come in: "my account hasn't synced in two weeks." The support team would investigate, find an expired consent, and tell the user to reconnect. A completely avoidable friction.

The product requirement: send users a reminder notification 7 days before their consent expires, so they can reconnect before sync breaks.

The backend work: a BullMQ repeatable job that scans for connections expiring within 7 days and publishes events via the outbox pattern.

---

**S — Situation:**

It is the first week of November. You have been the formal owner of the Accounts & Open Banking module for about three weeks. Lucas assigns you a ticket labelled "Consent expiry proactive notifications" with a short note: "design first, build after. Slack me when you have a proposal."

This is not a bug fix. This is a feature — one that requires you to propose the design, build it, coordinate with Tomasz who owns the Notification Service consumer side, write migrations, set up a BullMQ repeatable job, and test the full flow end to end. Nobody tells you exactly how to build it.

---

**T — Task:**

Design and implement the consent expiry notification feature end to end: detect expiring connections, avoid duplicate notifications, publish the right event via the outbox, and coordinate with Tomasz so Notification Service can consume it.

---

**A — Action:**

#### Step 1: Understanding What Data You Already Have

You start by reading the existing `BankConnection` schema. `consentExpiresAt` is already stored — Elena added it during the original GoCardless integration, setting it to 90 days from the moment consent was granted. That is your trigger condition. You already have the data. You just need to act on it.

```prisma
// The existing BankConnection model (before your changes)
// prisma/schema.prisma

model BankConnection {
  id                   String               @id @default(uuid())
  userId               String
  institutionId        String
  institutionName      String
  requisitionId        String               @unique
  status               BankConnectionStatus @default(PENDING)
  consentExpiresAt     DateTime?            // ← already exists
  lastRequisitionError String?
  createdAt            DateTime             @default(now())
  updatedAt            DateTime             @updatedAt

  user         User          @relation(fields: [userId], references: [id])
  bankAccounts BankAccount[]

  @@index([userId])
  @@index([requisitionId])
  @@map("banking.bank_connections")
}
```

The query you need: find all `BankConnection` records where `status = ACTIVE` and `consentExpiresAt` is between now and 7 days from now.

But before writing a single line of code, you think about the performance of this query. The `bank_connections` table will grow as FinVerse grows. Eventually it could have hundreds of thousands of rows. Scanning the entire table every day looking for expiring connections without an index would be a full sequential scan — PostgreSQL reads every row, which gets slower as the table grows.

You check the existing indexes on `BankConnection`:

```
Existing indexes:
  @@index([userId])          ← for "show all connections for this user"
  @@index([requisitionId])   ← covered by @unique, for callback lookup

Missing:
  No index on (status, consentExpiresAt)
  → Your daily scan query has no index to use
  → Full table scan every day
```

This becomes your first PR.

---

#### Step 2: The Design Proposal

Before touching any code, you draft the design in a Slack thread to Lucas and Tomasz. You keep it short — a paragraph and a simple flow:

```
DESIGN PROPOSAL (sent to Lucas and Tomasz on Slack)

A BullMQ repeatable job fires daily at 09:00 UTC.

It queries BankConnection for records where:
  status = ACTIVE
  consentExpiresAt BETWEEN now AND (now + 7 days)

For each matching connection:
  Write to outbox_events table (in the same DB transaction)
  Outbox publisher sends event to RabbitMQ
  Notification Service consumes, sends reminder push + email

Job configuration:
  name: CHECK_EXPIRING_CONSENTS
  schedule: cron '0 9 * * *' (daily at 09:00 UTC)
  concurrency: 1 (only one run at a time)
  jobId: 'check-expiring-consents-daily' (singleton — prevents duplicates)
```

Lucas responds within the hour with one question: "what happens if the job fires today and tomorrow for the same user? Does the user get two notifications?"

You had not thought of this. The job fires daily. A connection expiring in 7 days will match the query on day 1, day 2, day 3 — every day until expiry. Without deduplication, the user gets seven notifications for one expiring connection.

You sit with it for ten minutes and update the proposal:

```
DEDUPLICATION ADDITION

Before writing the outbox event, check Redis:
  key: consent:expiry:notif:{connectionId}:{expiryDateISO}
  TTL: 24 hours

If Redis key exists → already notified today → skip
If Redis key does not exist → write outbox event → set Redis key

Result:
  At most one notification per connection per day.
  Job can fire multiple times safely — idempotent.
```

Lucas approves. Tomasz asks what fields he needs in the event payload to build the notification message. You tell him you will include `userId`, `connectionId`, `institutionName`, and `consentExpiresAt`. He confirms that is enough.

---

#### Step 3: PR 1 — The Index Migration

```typescript
// What you add to schema.prisma

model BankConnection {
  // ... all existing fields unchanged ...

  // NEW: composite index supporting the daily expiry scan query
  // Query pattern: WHERE status = 'ACTIVE' AND consentExpiresAt BETWEEN x AND y
  // PostgreSQL can use this index to jump directly to ACTIVE connections
  // with upcoming expiry timestamps — no full table scan
  @@index([status, consentExpiresAt])
}
```

You generate the migration:

```bash
npx prisma migrate dev --name add_expiry_index_to_bank_connections
```

Prisma generates:

```sql
-- Migration SQL (auto-generated by Prisma)
-- prisma/migrations/20231103_add_expiry_index_to_bank_connections/migration.sql

CREATE INDEX "banking_bank_connections_status_consentExpiresAt_idx"
ON "banking"."bank_connections"("status", "consentExpiresAt");
```

This is a safe, additive migration — it adds an index without touching existing data or columns. PostgreSQL builds the index without locking the table for reads. You confirm this in your PR description.

Lucas reviews and approves in one pass with one comment: "good to also note why you chose a composite index over two separate indexes." You add a comment to the schema:

```prisma
// Composite index on (status, consentExpiresAt):
// The daily expiry scan always filters by status first,
// then by consentExpiresAt range.
// A composite index handles both conditions in one index scan.
// Two separate indexes would force PostgreSQL to choose one
// and scan the other condition manually — less efficient.
@@index([status, consentExpiresAt])
```

---

#### Step 4: PR 2 — The Job Logic and Redis Deduplication

This is the core of the feature. The BullMQ worker that runs daily and does the actual work.

```typescript
// src/modules/accounts/workers/check-expiring-consents.worker.ts

import { Processor, WorkerHost } from '@nestjs/bullmq'
import { Injectable } from '@nestjs/common'
import { Job } from 'bullmq'
import { InjectRedis } from '@nestjs-modules/ioredis'
import Redis from 'ioredis'
import { PrismaService } from '../../../common/prisma/prisma.service'
import { LoggerService } from '../../../common/logger/logger.service'

interface ExpiringConnection {
  id:               string
  userId:           string
  institutionName:  string
  consentExpiresAt: Date
}

@Processor('consent-check', {
  concurrency: 1,
  // concurrency: 1 because this job scans ALL expiring connections
  // sequentially. Running two instances simultaneously would
  // process the same connections twice and write duplicate
  // outbox events (Redis dedup would catch it, but better
  // to prevent it at the source).
})
export class CheckExpiringConsentsWorker extends WorkerHost {

  constructor(
    private readonly prisma:  PrismaService,
    private readonly logger:  LoggerService,
    @InjectRedis() private readonly redis: Redis,
  ) {
    super()
  }

  async process(job: Job): Promise<void> {
    const now             = new Date()
    const sevenDaysFromNow = new Date(
      now.getTime() + 7 * 24 * 60 * 60 * 1000
    )

    // Find all ACTIVE connections expiring within 7 days
    // Uses the composite index added in PR 1
    const expiringConnections = await this.prisma.bankConnection.findMany({
      where: {
        status: 'ACTIVE',
        consentExpiresAt: {
          gte: now,             // not already expired
          lte: sevenDaysFromNow // expiring within 7 days
        },
      },
      // select only what we need — do not fetch every column
      select: {
        id:               true,
        userId:           true,
        institutionName:  true,
        consentExpiresAt: true,
      },
    })

    this.logger.info('Consent expiry check completed', {
      connectionsFound: expiringConnections.length,
    })

    // Process each expiring connection
    for (const connection of expiringConnections) {
      await this.handleExpiringConnection(connection)
    }
  }

  private async handleExpiringConnection(
    connection: ExpiringConnection
  ): Promise<void> {

    // Build the dedup key — unique per connection per calendar day
    // ISO date format: "2023-11-10" (no time component)
    // This means: at most one notification per connection per day
    const expiryDateISO = connection.consentExpiresAt
      .toISOString()
      .split('T')[0]

    const dedupKey =
      `consent:expiry:notif:${connection.id}:${expiryDateISO}`

    // Check if we already notified for this connection today
    const alreadyNotified = await this.redis.get(dedupKey)

    if (alreadyNotified) {
      this.logger.debug('Skipping — already notified today', {
        connectionId: connection.id,
        userId:       connection.userId,
      })
      return
    }

    // Write outbox event to PostgreSQL
    // The outbox publisher worker will pick this up within 5 seconds
    // and publish to RabbitMQ → Notification Service
    await this.prisma.outboxEvent.create({
      data: {
        eventType: 'bank.connection.expiring.soon',
        payload: {
          userId:           connection.userId,
          connectionId:     connection.id,
          institutionName:  connection.institutionName,
          consentExpiresAt: connection.consentExpiresAt.toISOString(),
        },
      },
    })

    // Set Redis dedup key AFTER successful outbox write
    // TTL: 24 hours — expires naturally at the same time
    // the next day's job run would check again
    try {
      await this.redis.set(dedupKey, '1', 'EX', 86_400)
    } catch (redisError) {
      // Redis set failed after successful DB write.
      // The outbox event exists. The user will be notified.
      // But the dedup key is not set — if the job runs again
      // today, it will write a second outbox event.
      // This is a known, accepted edge case — one duplicate
      // notification is not a financial error. We log it clearly.
      this.logger.warn(
        'Redis dedup key write failed — duplicate notification possible',
        {
          connectionId: connection.id,
          userId:       connection.userId,
          error:        redisError.message,
        }
      )
      // Do NOT throw — the important work (outbox event) succeeded.
    }

    this.logger.info('Consent expiry notification scheduled', {
      userId:       connection.userId,
      connectionId: connection.id,
      expiresAt:    connection.consentExpiresAt.toISOString(),
    })
  }
}
```

Let's trace what happens for a real user to make this concrete:

```
FULL EXECUTION TRACE — single expiring connection

Setup:
  User usr_456 connected Deutsche Bank on Aug 15, 2023
  consentExpiresAt = Nov 13, 2023 (90 days later)
  Today: Nov 6, 2023 (7 days before expiry)

Job fires at 09:00 UTC on Nov 6:

Step 1: Query PostgreSQL
─────────────────────────────────────────────
  SELECT id, userId, institutionName, consentExpiresAt
  FROM banking.bank_connections
  WHERE status = 'ACTIVE'
    AND consentExpiresAt >= '2023-11-06T09:00:00Z'
    AND consentExpiresAt <= '2023-11-13T09:00:00Z'

  Result: [{ id: 'conn_789', userId: 'usr_456',
             institutionName: 'Deutsche Bank',
             consentExpiresAt: '2023-11-13T00:00:00Z' }]

Step 2: Check Redis dedup key
─────────────────────────────────────────────
  GET consent:expiry:notif:conn_789:2023-11-13
  → null (key does not exist — first time today)

Step 3: Write outbox event to PostgreSQL
─────────────────────────────────────────────
  INSERT INTO outbox.outbox_events (eventType, payload, status)
  VALUES (
    'bank.connection.expiring.soon',
    '{
      "userId": "usr_456",
      "connectionId": "conn_789",
      "institutionName": "Deutsche Bank",
      "consentExpiresAt": "2023-11-13T00:00:00Z"
    }',
    'PENDING'
  )

Step 4: Set Redis dedup key
─────────────────────────────────────────────
  SET consent:expiry:notif:conn_789:2023-11-13
      "1"
      EX 86400   ← expires in 24 hours

Step 5 (5 seconds later): Outbox publisher picks up event
─────────────────────────────────────────────
  Outbox publisher worker reads PENDING events
  Publishes to RabbitMQ:
    exchange: bank.events
    routing key: bank.connection.expiring.soon
    payload: { userId, connectionId, institutionName, consentExpiresAt }

Step 6: Notification Service consumes
─────────────────────────────────────────────
  Notification Service receives event
  Sends push notification to usr_456:
    "Your Deutsche Bank connection expires in 7 days.
     Tap to reconnect and keep your transactions syncing."
  Sends email reminder

─────────────────────────────────────────────

Job fires again at 09:00 UTC on Nov 7 (next day):

Step 1: Same query — conn_789 still matches
  (6 days until expiry, still within the 7-day window)

Step 2: Check Redis dedup key
  GET consent:expiry:notif:conn_789:2023-11-13
  → null (yesterday's key expired after 24 hours)

  Wait — shouldn't this prevent a second notification?
  
  The key format includes the EXPIRY DATE, not today's date.
  "2023-11-13" is the expiry date of the connection.
  
  So on Nov 6: key = consent:expiry:notif:conn_789:2023-11-13 → set
  On Nov 7:   key = consent:expiry:notif:conn_789:2023-11-13
              → STILL EXISTS (24h TTL set at 09:00 Nov 6,
                expires at 09:00 Nov 7)
  
  This means the dedup key must have a TTL longer than 24h,
  or use a different key structure.
```

This is the edge case Lucas caught when reviewing your PR. The dedup key uses the expiry date of the connection, not today's date — which means the key needs to live for the full 7-day window, not just 24 hours. Otherwise on day 2, the 24-hour key has expired but the expiry date key stays the same, and the user gets a second notification.

Lucas's review comment: "the key expires after 24 hours but the connection expiry date stays the same for 7 days — won't the user get notified every day?"

You think it through and realise the key structure needs to change:

```typescript
// WRONG: uses expiry date — same key for all 7 days
// After 24h TTL expires, next day sees same key → re-notifies
const dedupKey =
  `consent:expiry:notif:${connection.id}:${expiryDateISO}`

// CORRECT: uses today's date — different key each day
// Each day's notification has its own 24h TTL key
// User gets one notification per day for 7 days
// (this is actually acceptable — most users will reconnect
//  after the first notification. Daily reminders are reasonable.)
const todayISO = new Date().toISOString().split('T')[0]
const dedupKey =
  `consent:expiry:notif:${connection.id}:${todayISO}`
```

But then Lucas raises a follow-up question: "do you actually want to notify users every day for 7 days? That is quite aggressive. Most banking apps send one reminder at 7 days, one at 3 days, one at 1 day."

You discuss this in the PR thread. You agree — daily notifications for 7 days would feel like spam. You update the design to notify at three specific windows:

```typescript
private shouldNotifyToday(consentExpiresAt: Date): boolean {
  const now          = new Date()
  const msUntilExpiry = consentExpiresAt.getTime() - now.getTime()
  const daysUntilExpiry = msUntilExpiry / (1000 * 60 * 60 * 24)

  // Notify at: ~7 days, ~3 days, ~1 day before expiry
  // Use ranges to handle timing variations (job fires at 09:00,
  // but consentExpiresAt could be any time of day)
  return (
    (daysUntilExpiry >= 6.5 && daysUntilExpiry <= 7.5) ||   // 7-day window
    (daysUntilExpiry >= 2.5 && daysUntilExpiry <= 3.5) ||   // 3-day window
    (daysUntilExpiry >= 0.5 && daysUntilExpiry <= 1.5)      // 1-day window
  )
}
```

The dedup key now uses today's date — one notification per day maximum, but the `shouldNotifyToday` check further restricts it to only three specific windows:

```typescript
private async handleExpiringConnection(
  connection: ExpiringConnection
): Promise<void> {

  // Only notify on specific days
  if (!this.shouldNotifyToday(connection.consentExpiresAt)) {
    return
  }

  const todayISO = new Date().toISOString().split('T')[0]
  const dedupKey =
    `consent:expiry:notif:${connection.id}:${todayISO}`

  const alreadyNotified = await this.redis.get(dedupKey)
  if (alreadyNotified) {
    return
  }

  // ... write outbox event, set dedup key
}
```

This takes three review rounds total before Lucas is satisfied. Each round teaches you something specific about production thinking — not just "does the code work" but "what is the user experience, what are the edge cases at 3am when the job fires, what if the job fires twice."

---

#### Step 5: PR 3 — Registering the Repeatable Job

```typescript
// src/modules/accounts/schedulers/consent-check.scheduler.ts

import { Injectable, OnModuleInit } from '@nestjs/common'
import { InjectQueue } from '@nestjs/bullmq'
import { Queue } from 'bullmq'

@Injectable()
export class ConsentCheckScheduler implements OnModuleInit {

  constructor(
    @InjectQueue('consent-check')
    private readonly consentCheckQueue: Queue,
  ) {}

  async onModuleInit(): Promise<void> {
    // Register the repeatable job once at NestJS startup.
    //
    // BullMQ deduplication via jobId:
    // If 'check-expiring-consents-daily' already exists in Redis
    // (from a previous deployment), BullMQ silently ignores this call.
    // Safe to call on every restart — idempotent.
    //
    // This solves the multi-container problem:
    // All 3 API containers call this on startup.
    // BullMQ ensures only ONE scheduled job entry exists in Redis.
    // When it fires, only ONE worker container picks it up
    // (LMOVE is atomic).
    await this.consentCheckQueue.add(
      'CHECK_EXPIRING_CONSENTS',
      {},
      {
        jobId: 'check-expiring-consents-daily',  // singleton key
        repeat: {
          cron: '0 9 * * *',  // 09:00 UTC every day
          tz: 'UTC',
        },
        removeOnComplete: { count: 1 },
        // Only keep the most recent completed run in Redis.
        // Historical runs add no value — the SyncLog table
        // provides the audit trail.
        attempts: 3,
        backoff: {
          type: 'exponential',
          delay: 60_000,  // 1 minute base delay for this job
          // If the expiry check fails (DB down, Redis down),
          // wait longer before retrying — this is a background
          // administrative job, not user-facing.
        },
      }
    )
  }
}
```

A question that comes up during review: what happens when the Core Product service scales to 3 containers? Three containers, three `onModuleInit` calls, three attempts to register the same job.

```
MULTI-CONTAINER SCENARIO

Container 1 starts at 09:00:01:
  consentCheckQueue.add('CHECK_EXPIRING_CONSENTS', {}, { jobId: 'check-expiring-consents-daily', ... })
  → BullMQ checks Redis: does jobId 'check-expiring-consents-daily' exist?
  → No → creates the repeatable job entry in Redis
  → Redis: bull:consent-check:repeat stored

Container 2 starts at 09:00:03:
  consentCheckQueue.add('CHECK_EXPIRING_CONSENTS', {}, { jobId: 'check-expiring-consents-daily', ... })
  → BullMQ checks Redis: does jobId 'check-expiring-consents-daily' exist?
  → YES → silently ignores the call
  → No duplicate job created

Container 3 starts at 09:00:05:
  Same as Container 2 — silently ignored

When the job fires at 09:00 UTC next day:
  One job entry appears in bull:consent-check:wait
  Container 1 worker picks it up (LMOVE is atomic)
  Container 2 and 3 workers see empty wait list → nothing to do

Result: exactly one execution per day, regardless of container count
```

This is the same pattern from BullMQ Chapter 8 — the singleton `jobId` prevents duplicate scheduling across containers. Lucas sees this in your PR and approves without comment — you handled it correctly.

---

#### Step 6: PR 4 — The `needsReconnection` Flag

While building this feature, you realise the mobile app also needs to surface the approaching expiry inline — not just via notification, but in the accounts list itself. A banner on the account card: "Reconnection required in 6 days."

This requires a small addition to the `GET /v1/accounts` response you built in Month 3.

```typescript
// account.service.ts — updated mapToDto method

private mapToDto(account: BankAccountWithConnection): AccountDto {
  return {
    id:          account.id,
    name:        account.accountName,
    institution: account.institutionName,
    type:        account.accountType,
    currency:    account.currency,
    balance: {
      current:   account.currentBalance.toNumber(),
      available: account.availableBalance?.toNumber() ?? null,
    },
    sync: {
      status:      account.syncStatus,
      lastSyncedAt: account.lastSyncedAt?.toISOString() ?? null,
    },
    connection: {
      id:     account.bankConnection.id,
      status: account.bankConnection.status,
      consentExpiresAt: account.bankConnection.consentExpiresAt
        ?.toISOString() ?? null,

      // NEW: computed server-side
      // true if consent expires within 7 days
      // App uses this to show reconnection banner
      needsReconnection: this.isConsentExpiringSoon(
        account.bankConnection.consentExpiresAt
      ),

      // NEW: days remaining, for display in UI
      // "Reconnect in 6 days" rather than just a boolean flag
      daysUntilExpiry: this.getDaysUntilExpiry(
        account.bankConnection.consentExpiresAt
      ),
    },
  }
}

private isConsentExpiringSoon(expiresAt: Date | null): boolean {
  if (!expiresAt) return false
  const daysUntilExpiry = this.getDaysUntilExpiry(expiresAt)
  return daysUntilExpiry !== null && daysUntilExpiry <= 7
}

private getDaysUntilExpiry(expiresAt: Date | null): number | null {
  if (!expiresAt) return null
  const msUntilExpiry = expiresAt.getTime() - Date.now()
  if (msUntilExpiry <= 0) return 0  // already expired
  return Math.ceil(msUntilExpiry / (1000 * 60 * 60 * 24))
}
```

The response now includes:

```json
{
  "data": {
    "accounts": [
      {
        "id": "acc_uuid_1",
        "name": "Main Current Account",
        "institution": "Deutsche Bank",
        "connection": {
          "id": "conn_789",
          "status": "ACTIVE",
          "consentExpiresAt": "2023-11-13T00:00:00Z",
          "needsReconnection": true,
          "daysUntilExpiry": 6
        }
      }
    ]
  }
}
```

You mention the extension in your PR description: "added `needsReconnection` and `daysUntilExpiry` to the accounts response as a natural complement to the notification feature — the mobile team asked for an inline indicator alongside the push notification. Let me know if you want me to separate this into a different PR."

Lucas replies: "good call, keep it together."

---

**R — Result:**

The feature ships to production in the second week of November across four PRs over six working days.

Within the first week, Datadog shows 340 `bank.connection.expiring.soon` events published through the outbox to RabbitMQ. Notification Service delivers all 340 successfully — you verify this by checking the `outbox_events` table where all 340 rows show `status: PUBLISHED`.

You also verify deduplication is working: you query Redis directly in the staging environment after manually triggering the job twice in the same day. The second run produces zero new outbox events — all connections already have their dedup key set.

Support tickets for "bank disconnected without warning" drop in the weeks that follow. Céline mentions it in the sprint review: "this one has already reduced reactive support volume — good timing."

More personally: this is the first time you design something, propose it to a senior, receive feedback, revise it multiple times, build it in stages, and ship it — with your name on the whole arc. The feeling is meaningfully different from fixing someone else's bug.

---

### Story 2: The Sprint Estimation Lesson

**S — Situation:**

Sprint planning for the consent expiry feature sprint. You are presenting your estimate for the four PRs. You say: "I think this is a 3. The job logic is not complex — it is a database query, a Redis check, and an outbox write."

Lucas asks calmly: "Does that 3 include the migration and its review? The coordination with Tomasz on the event payload? The staging smoke test to verify the job actually fires? Watching Datadog after production deployment?"

You pause. You had not included any of those.

---

**T — Task:**

Revise your estimate to reflect the full lifecycle of the change, not just the code writing time.

---

**A — Action:**

You reconsider out loud, walking through each step:

```
YOUR REVISED BREAKDOWN

Foundational reading and design:        0.5 days
Writing the job + deduplication:        1.5 days
Writing tests:                          0.5 days
Schema migration + review:              0.5 days
PR descriptions + review cycles (×4):  1.0 day
Coordination with Tomasz:               0.5 days
Staging smoke test:                     0.5 days
Total:                                  5.0 days
```

You revise to 5. Lucas nods: "That sounds about right. Always estimate the whole thing — not just the interesting part."

---

**R — Result:**

The feature takes 6 working days — one over the revised estimate. The extra day came from the second and third review rounds on PR 2, which you had not fully accounted for.

But it is close. Much closer than the original 3-point estimate. From this point forward, your estimates become more accurate. You develop a mental habit: before giving a number in sprint planning, you mentally walk through the full lifecycle checklist. By month 6, Daniel notes in a check-in that your velocity estimates have become reliable.

---

### Story 3: The Retry Logic Disagreement With Tomasz

**Background:**

Tomasz notices the `failed` jobs tab in Bull Board has more entries than expected. Many are `PERIODIC_SYNC` jobs that failed with GoCardless 503 errors — brief transient failures that GoCardless resolved within minutes — but the retry window exhausted before GoCardless recovered. Tomasz proposes reducing `maxAttempts` from 3 to 2, arguing fewer retries means less noise. He opens a PR and tags you for review.

---

**S — Situation:**

You are reviewing Tomasz's PR in week three of November. The change is small — a retry configuration update. His reasoning is operational: fewer failed jobs in Bull Board makes monitoring cleaner. But you have a concern after reading it carefully.

---

**T — Task:**

Decide whether Tomasz's change is safe, raise your concern with a specific counterargument, and navigate the disagreement professionally.

---

**A — Action:**

**Understanding the existing retry mechanics first:**

The current configuration:

```typescript
// defaultJobOptions in BullModule.forRoot()
defaultJobOptions: {
  attempts: 3,
  backoff: {
    type: 'exponential',
    delay: 5000,   // base: 5 seconds
  },
}
```

With `attempts: 3` and `exponential delay: 5000ms`, here is the exact timeline when GoCardless returns a 503:

```
RETRY TIMELINE — attempts:3, exponential delay:5000ms

T+00:00  Attempt 1 fires
         GoCardless returns 503
         BullMQ: attemptsMade(0) < attempts(3) → retry available
         Delay = 5000 × 2^0 = 5000ms
         Job moves to delayed ZSET with score = now + 5000

T+00:05  Attempt 2 fires
         GoCardless still 503
         Delay = 5000 × 2^1 = 10000ms
         Job moves to delayed with score = now + 10000

T+00:15  Attempt 3 fires
         GoCardless still 503
         attemptsMade(2) = attempts(3)-1 → NO more retries
         Job moves to FAILED permanently
         Datadog alert fires

Total retry window: 15 seconds
```

Tomasz's proposed change:

```typescript
// Tomasz's PR
defaultJobOptions: {
  attempts: 2,    // ← reduced from 3
  backoff: {
    type: 'exponential',
    delay: 5000,
  },
}
```

With `attempts: 2`:

```
RETRY TIMELINE — attempts:2, exponential delay:5000ms

T+00:00  Attempt 1 fires → GoCardless 503
         Delay = 5000 × 2^0 = 5000ms

T+00:05  Attempt 2 fires → GoCardless still 503
         attemptsMade(1) = attempts(2)-1 → NO more retries
         Job moves to FAILED

Total retry window: 5 seconds
```

Tomasz's argument: "the retry window is shorter than any real GoCardless outage anyway — 15 seconds versus 5 seconds makes no practical difference. Might as well fail faster and keep the queue cleaner."

Before leaving your review comment, you go and check the GoCardless historical status page. You find this:

```
GOCARDLESS HISTORICAL INCIDENTS (past 2 months)

Incident 1: Sept 14, 02:00–02:08 UTC
  Duration: 8 minutes
  Type: "Elevated error rates on transaction endpoints"

Incident 2: Oct 3, 01:30–01:35 UTC
  Duration: 5 minutes
  Type: "Partial service degradation"

Incident 3: Oct 21, 03:00–03:04 UTC
  Duration: 4 minutes
  Type: "Scheduled maintenance window"
```

Now you map the retry windows against these real outage durations:

```
OUTAGE DURATION vs RETRY WINDOW

Real GoCardless outage: 5 minutes (300 seconds)

attempts:3, delay:5000ms → total window: 15 seconds
  Job fails at T+15s → GoCardless still down → FAILED

attempts:2, delay:5000ms → total window: 5 seconds
  Job fails at T+5s  → GoCardless still down → FAILED

CONCLUSION:
  Neither configuration survives a 5-minute outage.
  But Tomasz's change makes it WORSE — 5s instead of 15s.
  Any transient error that resolves between 5s and 15s
  would now cause a permanent failure instead of recovery.
```

You leave your PR comment:

```
Code review comment on PR line 14:

"I have a concern about reducing maxAttempts to 2.

I checked GoCardless's status page — they have had three
incidents in the past two months with durations of 4-8 minutes,
typically between 01:00-03:00 UTC. These are the same off-peak
hours when our periodic sync runs most of its batch.

With attempts:3 and exponential backoff, our retry window is
15 seconds. With attempts:2, it drops to 5 seconds.

Neither survives a 4-minute outage — but reducing attempts
makes us MORE fragile for transient errors that resolve in
the 5s-15s range. A quick GoCardless hiccup that would have
recovered on attempt 3 now becomes a permanent failure.

I understand the monitoring noise concern is real. Is there
a way to reduce the noise without reducing the safety window?
Happy to be wrong here — just want to flag the data."
```

Tomasz responds the same day — he takes the point but his operational concern is genuine. You both bring it to Lucas rather than going back and forth on the PR. You post a summary: here are the two positions, here is the data. What would you do?

Lucas comes back with a third option neither of you had considered: keep 3 retries, but add an `error_type` tag to the failed job metric. Make the monitoring smart enough to distinguish noise from signal, rather than changing the safety configuration to reduce noise.

```typescript
// transaction-sync.worker.ts — Lucas's suggested addition

async process(job: Job): Promise<void> {
  try {
    await this.handleSync(job)
  } catch (error) {
    const errorType = this.classifyError(error)

    // Tag the failure metric with the error type
    // Datadog can now filter by error_type
    this.metricsService.recordSyncFailed(
      job.name as 'INITIAL_SYNC' | 'PERIODIC_SYNC',
      errorType
    )

    throw error  // re-throw so BullMQ handles retry/fail
  }
}

private classifyError(error: Error): string {
  const message = error.message.toLowerCase()

  if (message.includes('429'))
    return 'gocardless_rate_limit'
  if (message.includes('503') || message.includes('service unavailable'))
    return 'gocardless_transient'   // ← the noise category
  if (message.includes('504') || message.includes('gateway timeout'))
    return 'gocardless_timeout'
  if (message.includes('timeout') && message.includes('postgres'))
    return 'postgres_timeout'

  return 'unknown'
}
```

The Datadog monitor is updated:

```
BEFORE Lucas's fix:

  Alert when:
    finverse.transaction_sync.jobs.failed > 0 (any failure)
  → Fires for GoCardless transient 503s → noise

AFTER Lucas's fix:

  Alert when:
    finverse.transaction_sync.jobs.failed{error_type:postgres_timeout} > 0
    OR
    finverse.transaction_sync.jobs.failed{error_type:unknown} > 0
    OR
    finverse.transaction_sync.jobs.failed{error_type:gocardless_rate_limit} > 5

  Do NOT alert when:
    finverse.transaction_sync.jobs.failed{error_type:gocardless_transient}
    → appears on dashboard but does NOT page
    → engineer sees it during normal monitoring,
      does not get woken up at 2am for it
```

Tomasz updates his PR to implement Lucas's suggestion. You approve it. The retry configuration stays at `attempts: 3`.

---

**R — Result:**

The `error_type` tag is added to the failure metric. The Datadog monitor stops paging for `gocardless_transient` errors. The failed jobs still appear in Bull Board — but they are expected, labelled, and not alarming.

More importantly: this is the first time you push back on a peer's technical decision with specific data — not opinion, not gut feel, but a concrete scenario built from the GoCardless status page history. And you do it in a way that does not create friction. Tomasz takes the point. You respect his operational concern. Lucas brings a perspective neither of you had. The outcome is better than either of your original proposals.

After the PR merges, Tomasz sends a short Slack message: "good catch on the status page data. I was too focused on the noise problem to look at the actual outage patterns."

You remember this as the first time a peer thanked you for a code review comment.

---

## What Month 4 Taught You Overall

Three distinct things, each from a different story:

**From the consent expiry feature:** End-to-end ownership feels different from fixing someone else's code. When you design something, propose it, defend it, revise it under feedback, build it across four PRs, and watch it running in production — there is a different quality of understanding. You know every decision and why it was made. If something breaks, you know exactly where to look. You also learn that "done" includes the edge cases Lucas finds in review — the dedup key TTL issue, the daily vs three-window notification decision — not just the happy path you wrote on day one.

**From the estimation lesson:** The code is never the whole job. Every change has a lifecycle — design, coordination, review cycles, migrations, staging, monitoring — and estimation that ignores those parts is not estimation, it is wishful thinking.

**From the Tomasz disagreement:** Technical disagreements are not adversarial if you approach them with evidence rather than ego. Tomasz was not wrong to notice the noise problem. You were not wrong to notice the safety risk. Lucas brought a perspective that neither of you had individually. The best outcome came from all three combining their views, not from one person winning the argument.