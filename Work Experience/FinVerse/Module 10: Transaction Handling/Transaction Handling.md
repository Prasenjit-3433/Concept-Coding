Before I start writing, let me confirm my understanding of what Step 10 needs to cover, and then I'll begin.

**What Step 10 will cover:**

**Part 1 — The Foundation:** What a database transaction actually is, why it matters in financial systems, and how Prisma handles transactions in NestJS. Starting from zero because you said you want to genuinely understand this.

**Part 2 — Core Product's Transaction Patterns:** The standard ways the team uses `prisma.$transaction()`, what the two modes are (sequential operations vs interactive), and when each is used.

**Part 3 — Accounts & Open Banking specifically:** Where transactions appear in your module — the callback handler, the sync worker, the disconnect flow — and exactly why each one needs a transaction.

**Part 4 — Distributed Transactions:** What happens when an operation spans multiple services (Core Product + Payment Service, for example). Why a single database transaction cannot help here. What SAGA and Outbox actually are — explained from scratch with diagrams, no buzzwords.

**Part 5 — Transactions with BullMQ:** The tricky cases where async jobs and database transactions interact — and how FinVerse handles the edge cases.

**Part 6 — Best Practices the team follows:** Isolation levels, concurrency, deadlock prevention, and what Lucas enforces in code review.

---

# FinVerse — Step 10: Transaction Handling

## Part 1 — What a Transaction Actually Is (From First Principles)

Before any Prisma code, you need a solid mental model of what a transaction is and why financial applications cannot function without them.

### The Problem Without Transactions

Imagine a user's monthly automated investment fires. FinVerse needs to do three things:

```
Step 1: Deduct €100 from user's available balance
Step 2: Create an investment order record
Step 3: Create holding records for the ETFs purchased
```

Now imagine Step 2 completes, but the server crashes before Step 3 runs.

```
WITHOUT TRANSACTIONS — CRASH SCENARIO

Database state after crash:
  ✅ available_balance reduced by €100  (money is gone)
  ✅ investment_order created           (order exists)
  ❌ holding records never created      (ETFs never recorded)

Result:
  User lost €100.
  No ETFs in their portfolio.
  The money went nowhere.
  This is a financial discrepancy — potentially a regulatory violation.
```

This is called a **partial write** — some operations succeeded, some didn't, and the database is now in an inconsistent, broken state.

A transaction solves this by wrapping multiple operations into a single all-or-nothing unit:

```
WITH TRANSACTIONS

BEGIN TRANSACTION
  Step 1: Deduct €100 from balance
  Step 2: Create investment order
  Step 3: Create holding records
COMMIT  ← all three land in the database together

If anything fails between BEGIN and COMMIT:
ROLLBACK  ← none of the three changes land at all
           ← database returns to its state before BEGIN
```

This is the **ACID guarantee**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    ACID — WHAT IT MEANS                         │
├───────────────────┬─────────────────────────────────────────────┤
│  A — Atomicity    │  All operations succeed together, or none   │
│                   │  of them do. No partial writes.             │
├───────────────────┼─────────────────────────────────────────────┤
│  C — Consistency  │  The database moves from one valid state    │
│                   │  to another valid state. Foreign key        │
│                   │  constraints, unique constraints, and       │
│                   │  check constraints are always enforced.     │
├───────────────────┼─────────────────────────────────────────────┤
│  I — Isolation    │  Concurrent transactions don't see each     │
│                   │  other's in-progress changes. Each          │
│                   │  transaction sees a consistent snapshot.    │
├───────────────────┼─────────────────────────────────────────────┤
│  D — Durability   │  Once committed, changes survive restarts,  │
│                   │  crashes, and power failures.               │
│                   │  PostgreSQL writes to disk before           │
│                   │  confirming commit.                         │
└───────────────────┴─────────────────────────────────────────────┘
```

For FinVerse, **Atomicity** is the one you use daily. Consistency is enforced by PostgreSQL's schema constraints (the Prisma schema with `@unique`, `NOT NULL`, foreign keys). Durability comes from PostgreSQL itself. Isolation becomes relevant in the concurrency section later.

---

## Part 2 — How Prisma Handles Transactions in NestJS

Prisma gives you two ways to use transactions. Understanding the difference is important because they solve different problems.

### Mode 1 — Sequential Operations (`prisma.$transaction([...])`)

You pass an array of Prisma operations. Prisma wraps them all in a single database transaction automatically.

```typescript
// All three operations succeed together or all roll back
await prisma.$transaction([
  prisma.portfolio.update({
    where: { id: portfolioId },
    data: { availableBalance: { decrement: 100 } }
  }),
  prisma.investmentOrder.create({
    data: { portfolioId, amount: 100, status: 'PENDING' }
  }),
  prisma.holding.createMany({
    data: holdingRecords
  })
])
```

What happens under the hood:

```
YOUR CODE                          POSTGRESQL
     │
     │  prisma.$transaction([...])
     │
     ├──────────────────────────►  BEGIN;
     │
     ├──────────────────────────►  UPDATE portfolio SET ...
     │
     ├──────────────────────────►  INSERT INTO investment_orders ...
     │
     ├──────────────────────────►  INSERT INTO holdings ...
     │
     │  all succeeded?
     ├──────────────────────────►  COMMIT;
     │
     │  any error?
     └──────────────────────────►  ROLLBACK;
```

**The limitation of this mode:** you cannot use the result of one operation as input to the next. You pass a static array of operations — they are prepared before any of them execute. If you need to read the ID of a newly created record and use it in the next operation, Mode 1 cannot help you.

---

### Mode 2 — Interactive Transactions (`prisma.$transaction(async (tx) => {...})`)

You pass a function that receives a special `tx` object (a Prisma client scoped to the transaction). Inside this function, you can do anything — read data, use results, make conditional decisions — all within the same transaction.

```typescript
await prisma.$transaction(async (tx) => {
  // Step 1: Read current balance
  const portfolio = await tx.portfolio.findUnique({
    where: { id: portfolioId }
  })

  // Step 2: Business logic using the read result
  if (portfolio.availableBalance < 100) {
    throw new Error('Insufficient balance')
    // This throw causes automatic ROLLBACK
  }

  // Step 3: Deduct balance
  await tx.portfolio.update({
    where: { id: portfolioId },
    data: { availableBalance: { decrement: 100 } }
  })

  // Step 4: Create order (uses portfolioId from the read)
  const order = await tx.investmentOrder.create({
    data: { portfolioId, amount: 100, status: 'PENDING' }
  })

  // Step 5: Create holdings (uses order.id from Step 4)
  await tx.holding.createMany({
    data: holdingRecords.map(h => ({
      ...h,
      orderId: order.id   // ← uses result of Step 4
    }))
  })
  // If we reach here without throwing: automatic COMMIT
})
```

What happens under the hood:

```
YOUR CODE                          POSTGRESQL (one connection held open)
     │
     │  prisma.$transaction(async (tx) => {
     │
     ├──────────────────────────►  BEGIN;
     │
     │  const portfolio = await tx.portfolio.findUnique(...)
     ├──────────────────────────►  SELECT * FROM portfolios WHERE ...
     │◄──────────────────────────  returns row
     │
     │  if (portfolio.availableBalance < 100) throw ...
     │  ← business logic using the result
     │
     │  await tx.portfolio.update(...)
     ├──────────────────────────►  UPDATE portfolios SET ...
     │
     │  const order = await tx.investmentOrder.create(...)
     ├──────────────────────────►  INSERT INTO investment_orders ...
     │◄──────────────────────────  returns new row with generated id
     │
     │  await tx.holding.createMany(...)   ← uses order.id
     ├──────────────────────────►  INSERT INTO holdings ...
     │
     │  function returns normally (no throw)
     ├──────────────────────────►  COMMIT;
     │
     │  OR: function throws an error
     └──────────────────────────►  ROLLBACK;
```

**Important:** the `tx` object must be used for every operation inside the transaction. If you accidentally use `prisma` (the global client) instead of `tx`, that operation runs outside the transaction — it commits immediately and will not roll back if the rest fails.

```typescript
await prisma.$transaction(async (tx) => {
  await tx.portfolio.update({ ... })      // ✅ inside transaction
  await prisma.syncLog.create({ ... })    // ❌ WRONG — uses global client
                                          //    commits immediately
                                          //    won't roll back if tx fails
})
```

This is a subtle bug that only appears during failures. Lucas flags this in every code review — any operation inside a `$transaction` callback must use `tx`, never `prisma`.

---

### The Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│        WHEN TO USE EACH PRISMA TRANSACTION MODE                 │
├────────────────────────────┬────────────────────────────────────┤
│  Sequential ([...])        │  Interactive (async tx)            │
├────────────────────────────┼────────────────────────────────────┤
│  Operations are            │  You need results from one         │
│  independent — no output   │  operation to feed the next        │
│  of one feeds the next     │                                    │
├────────────────────────────┼────────────────────────────────────┤
│  No conditional logic      │  Conditional rollback based        │
│  needed inside             │  on data read within transaction   │
├────────────────────────────┼────────────────────────────────────┤
│  Simpler, slightly         │  Holds DB connection open for      │
│  faster (batch send)       │  longer (one round trip per op)    │
├────────────────────────────┼────────────────────────────────────┤
│  Example: write event to   │  Example: check balance, then      │
│  outbox + update budget    │  deduct, then create order         │
│  in same transaction       │  (needs the check result)          │
└────────────────────────────┴────────────────────────────────────┘
```

In FinVerse, **interactive transactions are far more common in Core Product.** Financial operations almost always involve reading current state, making a business logic decision, and then writing. Sequential mode is used for simpler cases like the Outbox pattern (write business data + write outbox event together, no reads needed).

---

## Part 3 — Transactions in the Accounts & Open Banking Module

Your module has three places where transactions are critical. Let's go through each one.

---

### Transaction 1 — The GoCardless Callback Handler

When GoCardless redirects back after the user completes bank consent, your code needs to do several things. This is one of the most important transactions in your module.

```
WHAT NEEDS TO HAPPEN ATOMICALLY:

1. Update BankConnection status from PENDING → ACTIVE
2. Set consentExpiresAt on the BankConnection
3. Create BankAccount records for each authorised account

WHY THIS MUST BE ATOMIC:

If Step 1 succeeds but Step 3 fails (GoCardless error 
on one account's detail fetch):

  BankConnection is ACTIVE ← looks connected
  But zero BankAccount records exist ← nothing to sync

  User opens app: sees "Deutsche Bank connected" ✅
  Taps on it: no accounts appear ❌
  Sync tries to run: nothing to sync ❌
  Data inconsistency — connection says active, nothing underneath

With a transaction:
  If any account detail fetch fails → everything rolls back
  BankConnection stays PENDING
  User sees "connection failed" → tries again from the start
  Clean state, no orphaned records
```

Here is the actual code:

```typescript
// account.service.ts
async handleCallbackSuccess(userId: string): Promise<void> {
  const pendingConnection = await this.prisma.bankConnection.findFirst({
    where: { userId, status: 'PENDING' },
    orderBy: { createdAt: 'desc' }
  })

  if (!pendingConnection) {
    this.logger.error('Callback received but no PENDING connection found', { userId })
    return
  }

  // Fetch account IDs from GoCardless BEFORE the transaction
  // GoCardless calls should never run inside a transaction
  // (explained below)
  const requisitionDetails = await this.goCardlessService
    .getRequisition(pendingConnection.requisitionId)

  if (requisitionDetails.status !== 'LN') {
    await this.prisma.bankConnection.update({
      where: { id: pendingConnection.id },
      data: { status: 'ERROR', lastRequisitionError: requisitionDetails.status }
    })
    return
  }

  // Fetch all account details from GoCardless — again, OUTSIDE the transaction
  const accountDetails = await Promise.all(
    requisitionDetails.accounts.map(id =>
      this.goCardlessService.getAccountDetails(id)
    )
  )

  // NOW open the transaction — only database writes go inside
  await this.prisma.$transaction(async (tx) => {

    await tx.bankConnection.update({
      where: { id: pendingConnection.id },
      data: {
        status: 'ACTIVE',
        consentExpiresAt: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000)
      }
    })

    for (const detail of accountDetails) {
      await tx.bankAccount.upsert({
        where: { externalAccountId: detail.id },
        update: { isActive: true },
        create: {
          userId,
          bankConnectionId:  pendingConnection.id,
          externalAccountId: detail.id,
          iban:              detail.iban,
          accountName:       detail.name,
          institutionName:   pendingConnection.institutionName,
          accountType:       this.mapAccountType(detail.type),
          currency:          detail.currency,
          currentBalance:    new Decimal(detail.balances.current),
          availableBalance:  new Decimal(detail.balances.available ?? 0),
          syncStatus:        'IDLE',
        }
      })
    }

    // Write outbox event inside the same transaction
    // If this write fails, the whole transaction rolls back
    // The connection stays PENDING — no inconsistency
    await tx.outboxEvent.create({
      data: {
        eventType: 'bank.connection.completed',
        payload: {
          userId,
          accountCount: accountDetails.length,
          institutionName: pendingConnection.institutionName
        }
      }
    })
  })

  // Only reached if transaction committed successfully
  // Now safe to enqueue BullMQ job
  await this.syncQueue.add('INITIAL_SYNC', { userId, accountIds: requisitionDetails.accounts }, {
    jobId: `initial-sync-${userId}`,
    priority: 1,
    attempts: 3,
    backoff: { type: 'exponential', delay: 5000 }
  })
}
```

**The critical rule you see here:** GoCardless API calls happen before the transaction opens. Never put external HTTP calls inside a database transaction. Here is why:

```
WHY EXTERNAL CALLS MUST STAY OUTSIDE TRANSACTIONS

When you open a prisma.$transaction(), PostgreSQL assigns a
connection from the pool and issues BEGIN.

That connection is HELD OPEN until COMMIT or ROLLBACK.

A GoCardless API call can take 2-5 seconds.
With 10 concurrent callback handlers, each holding a 
connection open for 5 seconds:

  10 requests × 5 seconds = all 10 connections consumed
  Request 11 arrives: no connection available → waits
  Request 12-20: queuing up
  PostgreSQL connection pool exhausted
  All new requests time out

This is connection pool starvation. It can bring down
the entire service.

THE RULE:
  Transactions should be as short as possible.
  Do all external calls, computations, and data fetching
  BEFORE opening the transaction.
  Inside the transaction: only database writes.
  Get in, write, get out.
```

---

### Transaction 2 — The Disconnect Flow

When a user disconnects a bank, three things must happen atomically:

```typescript
// account.service.ts
async disconnectBank(userId: string, connectionId: string): Promise<void> {

  const connection = await this.prisma.bankConnection.findFirst({
    where: { id: connectionId, userId },
    include: { bankAccounts: true }
  })

  if (!connection) {
    throw new NotFoundException('CONNECTION_NOT_FOUND')
  }

  // Sequential transaction — no reads needed inside, no result chaining
  await this.prisma.$transaction([

    // Soft-delete the connection
    this.prisma.bankConnection.update({
      where: { id: connectionId },
      data: { status: 'DISCONNECTED' }
    }),

    // Soft-delete all accounts under this connection
    this.prisma.bankAccount.updateMany({
      where: { bankConnectionId: connectionId },
      data: { isActive: false }
    }),

    // Write outbox event — downstream modules need to know
    this.prisma.outboxEvent.create({
      data: {
        eventType: 'bank.connection.disconnected',
        payload: {
          userId,
          connectionId,
          accountIds: connection.bankAccounts.map(a => a.id)
        }
      }
    })
  ])

  // GoCardless revocation happens AFTER the transaction — outside it
  // Best-effort: if this fails, the connection is still disconnected
  // in FinVerse. The GoCardless token expires naturally in 90 days.
  try {
    await this.goCardlessService.deleteRequisition(connection.requisitionId)
  } catch (err) {
    this.logger.warn('GoCardless revocation failed', { connectionId })
  }
}
```

**Why the GoCardless call is after the transaction and wrapped in try-catch:**

The database state (connection disconnected) is the source of truth for FinVerse. Whether GoCardless also revokes the token is a best-effort operation. If the GoCardless call throws and it was inside the transaction, the entire disconnect would roll back — the user's bank would remain "connected" in FinVerse even though they explicitly disconnected it. That is far worse than a lingering GoCardless token.

---

### Transaction 3 — Sync Log + Status Update

Every time the BullMQ sync worker completes a sync (success or failure), two records must update atomically: the `bankAccount.syncStatus` and a new `SyncLog` record.

```typescript
// transaction-sync.worker.ts
private async completeSyncWithLog(
  bankAccountId: string,
  syncType: string,
  result: SyncResult
): Promise<void> {

  await this.prisma.$transaction([

    // Update account sync status
    this.prisma.bankAccount.update({
      where: { id: bankAccountId },
      data: {
        syncStatus:   result.success ? 'SUCCESS' : 'FAILED',
        lastSyncedAt: result.success ? new Date() : undefined,
        lastSyncError: result.success ? null : result.errorMessage,
      }
    }),

    // Create sync log record
    this.prisma.syncLog.create({
      data: {
        bankAccountId,
        syncType,
        status:               result.success ? 'SUCCESS' : 'FAILED',
        transactionsFetched:  result.fetched,
        transactionsInserted: result.inserted,
        transactionsDuplicate: result.duplicates,
        errorMessage:         result.errorMessage ?? null,
        startedAt:            result.startedAt,
        completedAt:          new Date(),
      }
    })
  ])
}
```

**Why atomic here:** if the status update succeeds but the log write fails (or vice versa), the sync status and the audit trail are inconsistent. An engineer investigating a sync failure would see a "SUCCESS" status with no corresponding log — or a log with no matching status. The transaction ensures they always stay in sync (pun intended).

---

## Part 4 — Distributed Transactions: When One Transaction Is Not Enough

This is where things get genuinely complex — and where many junior engineers get confused because the tools you normally use (database transactions) simply cannot help.

### The Problem

A database transaction only works within **one database**. If an operation spans two different services with two different databases, there is no way to wrap them in a single `BEGIN / COMMIT`.

Here is a real FinVerse example:

```
INVESTMENT ORDER FLOW — spans two services

Payment Service (payments_db):
  Step 1: Charge user €100 via Stripe
  Step 2: Record payment in payment_ledger

Core Product Service (core_product_db):
  Step 3: Create investment_order record
  Step 4: Create holding records
```

These four steps span two services and two databases. There is no single transaction that can wrap all four.

```
WHAT CAN GO WRONG

Payment Service                  Core Product Service
      │                                │
      │  Step 1: Stripe charge ✅       │
      │  Step 2: payment_ledger ✅      │
      │                                │
      │  publishes event →             │
      │                                │  Step 3: investment_order ✅
      │                                │  Step 4: holdings FAILS ❌
      │                                │
      │                         Result:
      │                           - User was charged €100 ✅
      │                           - Payment recorded ✅
      │                           - Order created ✅
      │                           - Holdings missing ❌
      │                           
      │                         Portfolio shows no ETFs purchased
      │                         User lost €100 with nothing to show
```

This is a **distributed consistency problem**. How do you ensure all four steps either succeed or the failure is handled correctly?

There are two established patterns for solving this. FinVerse uses both.

---

## Part 5 — The Outbox Pattern (Already Familiar From BullMQ Module)

You have seen this in Module 7 (BullMQ Chapter 8) when we discussed the notification race condition. Now let's understand it properly as a transaction pattern.

### The Problem It Solves

```
WITHOUT OUTBOX — THE RELIABILITY GAP

Core Product BullMQ worker (after sync):

  Step 1: INSERT transactions into PostgreSQL ✅
  Step 2: UPDATE budget.spent in PostgreSQL ✅

  Step 3: rabbitMQ.publish('budget.threshold.exceeded') 
          ← THIS IS A SEPARATE OPERATION
          ← if RabbitMQ is down right now → ❌ LOST
          ← if the process crashes here → ❌ LOST
          ← if there's a network hiccup → ❌ LOST

The database says the threshold was exceeded.
The user never gets notified.
Silent inconsistency.
```

### How Outbox Solves This

The idea is simple: **instead of publishing directly to RabbitMQ, write the event to a database table in the same transaction as the business data.** Then a separate background process reads from that table and publishes to RabbitMQ.

```
WITH OUTBOX

BEGIN TRANSACTION (PostgreSQL)
  INSERT transactions ... ✅
  UPDATE budget.spent ... ✅
  INSERT outbox_events (eventType='budget.threshold.exceeded', status='PENDING') ✅
COMMIT

Now: even if RabbitMQ is down, the event exists in PostgreSQL.
It will be published when RabbitMQ recovers.
The business data and the intent to publish are atomically consistent.
```

The diagram of the full flow:

```
OUTBOX PATTERN — COMPLETE FLOW

┌─────────────────────────────────────────────────────────────────┐
│  Core Product BullMQ Worker (transaction-sync)                  │
│                                                                 │
│  BEGIN TRANSACTION                                              │
│    INSERT INTO transactions (new bank transactions)             │
│    UPDATE budgets SET spent = ...                               │
│    INSERT INTO outbox_events                                    │
│      (eventType='budget.threshold.exceeded', status='PENDING')  │
│  COMMIT                                                         │
│                                                                 │
│  ← All three writes land atomically or none do                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    outbox_events table now has
                    a PENDING event
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  BullMQ outbox-publisher Worker (runs every 5 seconds)          │
│                                                                 │
│  SELECT * FROM outbox_events WHERE status='PENDING'             │
│  ORDER BY createdAt ASC LIMIT 100                               │
│                                                                 │
│  For each pending event:                                        │
│    rabbitMQChannel.publish(eventType, payload)                  │
│    UPDATE outbox_events SET status='PUBLISHED' WHERE id=...     │
│                                                                 │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    Event arrives in RabbitMQ
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│  Notification Service (RabbitMQ consumer)                       │
│                                                                 │
│  Receives 'budget.threshold.exceeded'                           │
│  Sends push notification and email                              │
└─────────────────────────────────────────────────────────────────┘
```

**The outbox_events table in the Prisma schema:**

```prisma
model OutboxEvent {
  id          String   @id @default(uuid())
  eventType   String   // "budget.threshold.exceeded", "bank.connection.completed"
  payload     Json     // the full event payload
  status      String   @default("PENDING")  // "PENDING" | "PUBLISHED" | "FAILED"
  createdAt   DateTime @default(now())
  publishedAt DateTime?

  @@index([status, createdAt])   // for the polling query
  @@map("outbox.outbox_events")
}
```

**Where Outbox is used in Core Product:**

```
┌─────────────────────────────────────────────────────────────────┐
│  OUTBOX EVENTS PUBLISHED BY CORE PRODUCT                        │
├────────────────────────────────┬────────────────────────────────┤
│  Event                         │  Trigger                       │
├────────────────────────────────┼────────────────────────────────┤
│  budget.threshold.exceeded     │  After sync: budget limit hit  │
├────────────────────────────────┼────────────────────────────────┤
│  bank.connection.completed     │  GoCardless callback success   │
├────────────────────────────────┼────────────────────────────────┤
│  bank.connection.disconnected  │  User disconnects bank         │
├────────────────────────────────┼────────────────────────────────┤
│  account.balance.updated       │  After sync: balance changed   │
└────────────────────────────────┴────────────────────────────────┘
```

All of these are written in the same transaction as the business data they relate to.

---

## Part 6 — The SAGA Pattern (Distributed Transaction Coordination)

Now for the bigger question: what about the investment order flow that spans Payment Service and Core Product?

### What SAGA Is

SAGA is a pattern for managing operations that span multiple services, where you need to handle failures gracefully without being able to use a single database transaction.

The name comes from the saga structure: a sequence of local transactions, each in their own service and database. If any local transaction fails, previously completed ones are undone by **compensating transactions** — essentially "reverse" operations that undo what was done.

There are two types of SAGA:

```
┌─────────────────────────────────────────────────────────────────┐
│              SAGA TYPES                                         │
├──────────────────────────┬──────────────────────────────────────┤
│  Choreography            │  Orchestration                       │
├──────────────────────────┼──────────────────────────────────────┤
│  No central coordinator  │  One central orchestrator service    │
│                          │  tells each service what to do       │
│  Each service listens    │                                      │
│  for events and decides  │  Like a conductor directing an       │
│  what to do next         │  orchestra                           │
│                          │                                      │
│  Services publish events │  Orchestrator publishes commands     │
│  → other services react  │  → services execute and report back  │
├──────────────────────────┼──────────────────────────────────────┤
│  Simpler to implement    │  Clearer to reason about             │
│  at small scale          │  Easier to track saga state          │
│  Hard to debug when      │  More infrastructure (orchestrator   │
│  things go wrong         │  service needed)                     │
└──────────────────────────┴──────────────────────────────────────┘
```

### Does FinVerse Implement a Full SAGA?

**No — and understanding why is important.**

A full SAGA with explicit compensating transactions is complex. It requires:
- Tracking which step the saga is currently on
- Storing what compensation to perform if a later step fails
- Ensuring compensating transactions themselves don't fail
- Handling the case where a compensating transaction also fails

For FinVerse's current scale and service count, this complexity is not justified. Instead, FinVerse achieves the same safety guarantees with a simpler combination of three things:

```
┌─────────────────────────────────────────────────────────────────┐
│  FINVERSE'S APPROACH TO DISTRIBUTED CONSISTENCY                 │
│  (Instead of full SAGA)                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. OUTBOX PATTERN                                              │
│     Ensures the event (investment.order.paid) is reliably       │
│     delivered from Payment Service to Core Product.             │
│     Even if RabbitMQ is down at the moment of payment,          │
│     the event will be delivered when it recovers.               │
│                                                                 │
│  2. IDEMPOTENT CONSUMERS                                        │
│     Core Product processes investment.order.paid events.        │
│     If the same event arrives twice (RabbitMQ at-least-once     │
│     delivery), processing it twice produces the same result.    │
│     Duplicate holding creation is blocked by the                │
│     @@unique([portfolioId, instrumentId]) constraint.           │
│                                                                 │
│  3. RECONCILIATION JOB                                          │
│     A BullMQ job runs hourly checking for investment orders     │
│     in PAID status with no corresponding holdings.              │
│     If found, it retries the Core Product update, or            │
│     triggers a refund if Core Product cannot complete.          │
│     This is the "compensating transaction" equivalent.          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The investment order flow visualised with this approach:

```
INVESTMENT ORDER — DISTRIBUTED FLOW AT FINVERSE

Payment Service                               Core Product Service
(payments_db)                                 (core_product_db)
      │                                              │
      │  BEGIN TX                                    │
      │  Stripe.charge(€100)                         │
      │  INSERT payment_ledger                       │
      │  INSERT outbox_events                        │
      │    (investment.order.paid, PENDING)          │
      │  COMMIT                                      │
      │                                              │
      │  outbox-publisher fires                      │
      │  RabbitMQ.publish(investment.order.paid) ───►│
      │                                              │
      │                                    RabbitMQ delivers to Core Product
      │                                              │
      │                                    Core Product consumer receives:
      │                                              │
      │                                    BEGIN TX
      │                                    SELECT portfolio (check exists)
      │                                    INSERT investment_order (idempotent)
      │                                    INSERT holdings (unique constraint)
      │                                    COMMIT
      │                                               │
      │  Reconciliation job (runs hourly):            │
      │  SELECT investment_orders WHERE               │
      │    status='PAID' AND holdings count = 0       │
      │  ← if found, retry or initiate refund         │
```

**When would FinVerse consider a full SAGA?**

If FinVerse expanded to 5+ services all involved in a single business transaction — payment, investment, tax ledger, regulatory reporting, portfolio rebalancing — the combination of Outbox + idempotency + reconciliation becomes hard to reason about and maintain. That is when an orchestration-based SAGA, potentially using a dedicated saga orchestrator service, becomes the right conversation. At Series A with 3-4 services, the simpler approach is correct.

---

## Part 7 — Isolation Levels: What They Are and When They Matter

This is an intermediate topic that interviewers at slightly more senior companies will probe. You do not need to have implemented this yourself, but you need to understand it.

### What Isolation Levels Control

When two transactions run at the same time, isolation levels define what each one is allowed to see of the other's in-progress changes.

```
SCENARIO: Two BullMQ workers processing syncs simultaneously

Worker A: syncing user_123's account
Worker B: generating budget report for user_123

Worker A is in the middle of inserting 500 new transactions.
Worker B starts reading transactions to compute the budget.

Should Worker B see Worker A's in-progress insertions?
That depends on the isolation level.
```

PostgreSQL's isolation levels, from least to most strict:

```
┌─────────────────────────────────────────────────────────────────┐
│           POSTGRESQL ISOLATION LEVELS                           │
├──────────────────────────┬──────────────────────────────────────┤
│  Level                   │  What it prevents                    │
├──────────────────────────┼──────────────────────────────────────┤
│  READ COMMITTED          │  Dirty reads                         │
│  (PostgreSQL default)    │  ← you only read committed data      │
│                          │                                      │
│                          │  Does NOT prevent:                   │
│                          │  Non-repeatable reads                │
│                          │  Phantom reads                       │
├──────────────────────────┼──────────────────────────────────────┤
│  REPEATABLE READ         │  Dirty reads                         │
│                          │  Non-repeatable reads                │
│                          │                                      │
│                          │  Does NOT prevent:                   │
│                          │  Phantom reads (in some databases,   │
│                          │  but PostgreSQL prevents these here) │
├──────────────────────────┼──────────────────────────────────────┤
│  SERIALIZABLE            │  Everything — transactions behave    │
│                          │  as if they ran one after the other  │
│                          │  even if they run concurrently       │
│                          │  Highest correctness, highest cost   │
└──────────────────────────┴──────────────────────────────────────┘
```

**What the terms mean simply:**

**Dirty read** — reading data that another transaction has written but not yet committed. If that other transaction rolls back, you read data that never actually existed.

**Non-repeatable read** — you read a row, another transaction updates and commits it, you read it again within your transaction and get a different value. The same read, different results.

**Phantom read** — you run a query that returns 10 rows, another transaction inserts a new row matching your query, you run the same query again and get 11 rows. A "phantom" row appeared.

### What FinVerse Uses

**READ COMMITTED** — PostgreSQL's default — is what FinVerse uses for the vast majority of operations.

```typescript
// This is READ COMMITTED — the default
// No special configuration needed
await prisma.$transaction(async (tx) => {
  const transactions = await tx.transaction.findMany({
    where: { userId, date: { gte: startOfMonth } }
  })
  // compute budget spend...
})
```

READ COMMITTED is sufficient for most sync and budget operations because:
- Workers process one user's data at a time (the `userId` filter means no real concurrent conflict)
- BullMQ job deduplication (deterministic `jobId`) prevents two workers processing the same user simultaneously
- The natural isolation is sufficient — we read committed data, perform calculations, write results

**SERIALIZABLE** — used only for the most critical financial operations:

```typescript
// SERIALIZABLE for investment order processing
// Prevents any concurrency anomaly during balance check → deduct
await prisma.$transaction(
  async (tx) => {
    const portfolio = await tx.portfolio.findUnique({
      where: { id: portfolioId }
    })

    if (portfolio.availableBalance < orderAmount) {
      throw new InsufficientBalanceError()
    }

    await tx.portfolio.update({
      where: { id: portfolioId },
      data: { availableBalance: { decrement: orderAmount } }
    })

    // ... create order and holdings
  },
  {
    isolationLevel: Prisma.TransactionIsolationLevel.Serializable
  }
)
```

**Why SERIALIZABLE here specifically:**

The check-then-act pattern (check balance, then deduct) has a race condition at lower isolation levels:

```
RACE CONDITION WITHOUT SERIALIZABLE

Time ──────────────────────────────────────────────────────────►

Transaction A (Worker 1 — monthly investment):
  READ portfolio: availableBalance = €200
  DECISION: €200 >= €100 → proceed
  ... (other work) ...
  DEDUCT €100 → balance = €100

Transaction B (Worker 2 — goal transfer, concurrent):
  READ portfolio: availableBalance = €200
  DECISION: €200 >= €150 → proceed
  ... (other work) ...
  DEDUCT €150 → balance = €50

Both transactions decided to proceed based on reading €200.
Both deducted.
Final balance: €50
But the user only had €200 to start with.
They were charged €250 total.
OVERDRAFT — a financial error.

With SERIALIZABLE:
  One transaction runs first completely.
  The second sees the updated balance.
  If the second's deduction would overdraft, it fails cleanly.
```

In practice, this race condition is rare for FinVerse because BullMQ's job deduplication means only one investment order job runs per user per month (deterministic `jobId`). But for payment-adjacent operations involving balance manipulation, SERIALIZABLE is the correct choice.

---

## Part 8 — Concurrency in the Accounts Module: The Sync Lock

There is one concurrency problem specific to your module that is worth understanding deeply: what happens if two sync jobs run for the same account simultaneously?

```
CONCURRENT SYNC PROBLEM

Worker A: periodic sync for user_123, account acc_456
Worker B: manual sync triggered for user_123, account acc_456
          (user tapped "Sync now" while periodic sync was running)

Both workers:
  1. Call GoCardless for transactions since lastSyncedAt
  2. Get the same 50 new transactions back
  3. Both try to INSERT those 50 transactions

Result without protection:
  Race condition on bulk insert
  Duplicate transactions possible (if the externalId unique
  constraint is violated, PostgreSQL throws an error)
  Both workers fail
  Neither marks the sync as complete
  syncStatus stuck in a broken state
```

**FinVerse's solution — the deterministic `jobId` as a distributed lock:**

```typescript
// When enqueueing sync jobs, always use deterministic jobId:

// Periodic sync — one per user, not per account
await this.syncQueue.add(
  'PERIODIC_SYNC',
  { userId, accountIds },
  {
    jobId: `periodic-sync-${userId}-${currentHourBucket}`,
    // currentHourBucket = Math.floor(Date.now() / (4 * 60 * 60 * 1000))
    // Changes every 4 hours — so one periodic sync per 4-hour window per user
  }
)

// Manual sync — one per account at a time
await this.syncQueue.add(
  'PERIODIC_SYNC',
  { userId, accountIds: [accountId] },
  {
    jobId: `manual-sync-${accountId}`,
    // If a manual sync is already pending or active for this account,
    // BullMQ silently ignores this second add() call
  }
)
```

BullMQ's `jobId` deduplication is the distributed lock here. At most one sync job exists per account in the `wait` or `active` state at any time. The second `queue.add()` call is a no-op if a job with that ID already exists.

The `upsert` in the sync worker handles the rare case where deduplication is bypassed (e.g. different job types syncing the same account):

```typescript
// bulkInsert in transaction service — idempotent by design
async bulkInsert(userId: string, accountId: string, transactions: GoCardlessTransaction[]) {
  // createMany with skipDuplicates handles the externalId unique constraint
  // Any transaction that already exists is silently skipped
  await this.prisma.transaction.createMany({
    data: transactions.map(tx => ({
      bankAccountId: accountId,
      userId,
      externalId:   tx.transactionId,   // unique constraint
      amount:       new Decimal(tx.transactionAmount.amount),
      // ...
    })),
    skipDuplicates: true   // ← Prisma's built-in duplicate handling
  })
}
```

`skipDuplicates: true` tells Prisma to use `INSERT ... ON CONFLICT DO NOTHING` in PostgreSQL. No error thrown on duplicates — they are silently skipped. The operation is idempotent by design.

---

## Part 9 — Transaction Handling Best Practices the Team Follows

These are the conventions Lucas enforces in code review. Knowing these lets you speak confidently about "what your team does."

```
┌─────────────────────────────────────────────────────────────────┐
│       CORE PRODUCT TRANSACTION BEST PRACTICES                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. KEEP TRANSACTIONS SHORT                                     │
│     Do all external calls, reads, and computations outside      │
│     the transaction. Inside: only the writes.                   │
│     Short transaction = connection held for less time =         │
│     lower connection pool pressure.                             │
│                                                                 │
│  2. ALWAYS USE tx INSIDE $transaction CALLBACKS                 │
│     Any prisma.X call that accidentally uses the global         │
│     client instead of tx runs outside the transaction.          │
│     ESLint rule added by Lucas flags this automatically.        │
│                                                                 │
│  3. OUTBOX FOR ALL CROSS-SERVICE EVENTS                         │
│     Never publish directly to RabbitMQ inside business          │
│     logic. Always write to outbox_events inside the same        │
│     transaction as the business data.                           │
│     The outbox-publisher worker handles delivery separately.    │
│                                                                 │
│  4. skipDuplicates FOR BULK INSERTS                             │
│     All bulk inserts in sync operations use skipDuplicates.     │
│     The externalId unique constraint is the real dedup          │
│     mechanism. skipDuplicates makes inserts idempotent.         │
│                                                                 │
│  5. NEVER HARD-DELETE FINANCIAL RECORDS                         │
│     BankConnections, BankAccounts, Transactions, Orders —       │
│     all soft-deleted (isActive: false / status: DISCONNECTED).  │
│     Hard deletes lose audit trail. In regulated fintech,        │
│     audit trail is required.                                    │
│                                                                 │
│  6. SERIALIZABLE ONLY FOR BALANCE MUTATIONS                     │
│     READ COMMITTED is the default. SERIALIZABLE is explicitly   │
│     set only when a check-then-act pattern involves money.      │
│     Overusing SERIALIZABLE causes lock contention and           │
│     slows the database.                                         │
│                                                                 │
│  7. PRISMA $TRANSACTION FOR MULTI-WRITE OPERATIONS ONLY         │
│     Single-table single-write operations do not need a          │
│     transaction wrapper — PostgreSQL guarantees single          │
│     statement atomicity by default.                             │
│     Wrapping a single write in a transaction adds               │
│     overhead for no benefit.                                    │
│                                                                 │
│  8. TIMEOUT ON INTERACTIVE TRANSACTIONS                         │
│     Interactive transactions that run longer than 5 seconds     │
│     are a sign something is wrong (slow query, external call    │
│     accidentally inside, missing index).                        │
│     Prisma allows setting a timeout:                            │
│                                                                 │
│     prisma.$transaction(async (tx) => { ... }, {                │
│       timeout: 5000   // throw if transaction takes > 5s        │
│     })                                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 10 — The Complete Mental Map (For Interviews)

When an interviewer asks "tell me how your team handles transactions," here is the complete, structured answer:

**For operations within a single service:**
"We use Prisma's interactive transactions for any operation that involves reading current state, making a business decision, and then writing — which covers most of our financial flows. We keep transactions short — all external calls happen before we open the transaction. Inside: only database writes. For cross-module events, we always use the Outbox pattern — write the event to an `outbox_events` table in the same transaction as the business data, and a separate BullMQ worker publishes it to RabbitMQ. This means our business data and our intent to notify other services are always atomically consistent."

**For operations that span services:**
"We use the Outbox pattern combined with idempotent consumers. Payment Service writes an event to its outbox atomically with the payment record. The event reaches Core Product via RabbitMQ. Core Product processes it idempotently — `skipDuplicates` for holdings, `upsert` for order status. A reconciliation job runs hourly to catch any order in a paid state without corresponding holdings and retries or initiates a refund. We deliberately chose this over a full SAGA pattern because the operational complexity of explicit compensating transactions is not justified at our current service count. The simpler combination of Outbox, idempotency, and reconciliation achieves the same safety guarantee."

**For the isolation level question:**
"READ COMMITTED is our default — PostgreSQL's default. We only reach for SERIALIZABLE explicitly for balance mutation flows: read balance, check sufficiency, deduct. Without SERIALIZABLE, two concurrent transactions could both read the same balance, both decide it's sufficient, and both deduct — causing an overdraft. SERIALIZABLE prevents this. We avoid overusing it because it increases lock contention and slows the database."

---

