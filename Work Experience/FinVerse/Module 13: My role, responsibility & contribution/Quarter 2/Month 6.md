# Month 6: Thinking Beyond Your Own Ticket

---

## Foundational Knowledge: What You Need Before These Stories

Two quick concepts before the stories — both come up directly in the technical work.

### Concept 1: Cursor-Based vs Offset Pagination

This comes up when you read Arjun's code during the net worth feature collaboration. Understanding it properly means you can explain it confidently if an interviewer asks.

```
THE PROBLEM WITH OFFSET PAGINATION

Offset pagination:
  Page 1: SELECT * FROM transactions
           ORDER BY date DESC
           LIMIT 20 OFFSET 0

  Page 2: SELECT * FROM transactions
           ORDER BY date DESC
           LIMIT 20 OFFSET 20

Looks simple. Has a real problem.

SCENARIO: user is on page 1, viewing transactions 1-20.
Between page 1 and page 2, three new transactions arrive.
They are inserted at the top of the list (newest first).

Before new transactions:
  Position 1: txn_100
  Position 2: txn_99
  ...
  Position 20: txn_81   ← last item on page 1
  Position 21: txn_80   ← first item on page 2

After three new transactions arrive:
  Position 1:  txn_103  ← NEW
  Position 2:  txn_102  ← NEW
  Position 3:  txn_101  ← NEW
  Position 4:  txn_100
  ...
  Position 20: txn_84   ← shifted — was at position 17
  Position 21: txn_83   ← now first item on page 2
  Position 22: txn_82
  Position 23: txn_81   ← was last item on page 1, now position 23

User loads page 2 with OFFSET 20:
  Gets: txn_83, txn_82, txn_81, txn_80...
  txn_83, txn_82, txn_81 were already shown on page 1
  → DUPLICATE RESULTS

And some transactions are skipped entirely:
  txn_84 (position 20) never appears on page 1 or page 2
```

```
CURSOR-BASED PAGINATION — THE FIX

Instead of "give me items at position 20-40",
you say "give me items that come AFTER this specific item."

Page 1:
  SELECT * FROM transactions
  WHERE userId = 'usr_123'
  ORDER BY date DESC
  LIMIT 20

Response includes last item's cursor:
  "cursor": "2023-11-15T14:23:11Z_txn_81"
  (encodes the date and ID of the last returned item)

Page 2:
  SELECT * FROM transactions
  WHERE userId = 'usr_123'
    AND (date < '2023-11-15T14:23:11Z'   ← strictly before cursor
         OR (date = '2023-11-15T14:23:11Z' AND id < 'txn_81'))
  ORDER BY date DESC
  LIMIT 20

No matter how many new transactions arrive at the top:
  The cursor points to a stable position.
  Page 2 always starts after txn_81.
  No duplicates. No skipped items.
```

```
WHEN TO USE WHICH

Offset pagination:
  ✓ Small, stable datasets (< 1000 items)
  ✓ Admin panels where users jump to specific pages
  ✓ When total count display matters ("page 3 of 47")

Cursor-based pagination:
  ✓ Large, frequently updated datasets (transactions)
  ✓ Infinite scroll / "load more" patterns on mobile
  ✓ When data consistency across pages matters
  ✗ Cannot jump to an arbitrary page number
  ✗ Slightly more complex to implement
```

At FinVerse, the transaction list on the mobile app uses cursor-based pagination. The savings goal contributions list — which Arjun builds — also uses it. You learn this pattern by reading his code during the collaboration.

---

### Concept 2: What an Ownership Check Is and Why It Is Non-Negotiable

This comes up in the code review story — you catch the same bug in Arjun's code that Lucas caught in yours in month 2. Understanding exactly what the bug is and why it matters makes the story concrete.

```
THE OWNERSHIP CHECK PATTERN

Every database query that returns user-specific data
MUST include the userId in the WHERE clause.

WHY IT MATTERS:

  Without ownership check:
    GET /v1/goals/:goalId
    → SELECT * FROM goals WHERE id = 'goal_abc'
    → Returns goal regardless of who owns it
    
    Attacker who knows (or guesses) goal_abc's ID:
    → Sends: GET /v1/goals/goal_abc with their own JWT
    → Gets back someone else's savings goal
    → Sees target amount, current savings, goal name
    → This is a data breach

  With ownership check:
    GET /v1/goals/:goalId
    → SELECT * FROM goals
      WHERE id = 'goal_abc'
      AND userId = 'usr_from_jwt'   ← must match
    → If goal_abc belongs to usr_456
      and request comes from usr_789:
      → Returns null → 404 Not Found
    → No data leak, no error message revealing existence

  The 404 is deliberate ambiguity:
    "Not found OR access denied" — same response either way.
    Attacker cannot distinguish "goal doesn't exist"
    from "goal exists but belongs to someone else."
    No information leakage.
```

---

## The Stories

---

### Story 1: The Net Worth Feature — Cross-Module Collaboration With Arjun

**Background:**

The product team wants a net worth dashboard on the home screen — the user's total cash position across all connected bank accounts, displayed as a single number broken down by currency. For a user with a German bank account (EUR) and a UK Monzo account (GBP), this would show two separate totals rather than an invalid cross-currency sum.

The backend work spans two modules. The cash component — bank account balances — lives in your Accounts & Open Banking module. The savings component — goal balances — lives in Arjun's Goals & Savings module. The product team wants both displayed together on the home screen.

You and Arjun are asked to build your respective pieces and define an API contract so the mobile team can stitch them together.

---

**S — Situation:**

It is the first week of December. You have been working on tickets independently for two months. This is the first time you need to design something in coordination with another engineer — not just coordinate on a Slack thread, but agree on data shapes, decide who owns what, and review each other's code.

You and Arjun set up a 30-minute call on a Wednesday morning to plan the work before either of you writes a line of code.

---

**T — Task:**

Build the `GET /v1/accounts/net-worth` endpoint for the cash component, coordinate with Arjun on the data contract for the savings component, and review each other's PRs.

---

**A — Action:**

#### Step 1: The Planning Call With Arjun

You and Arjun spend 30 minutes on Google Meet mapping out the work. You draw the data flow on a shared Notion page:

```
NET WORTH DATA FLOW

Mobile App calls:
  GET /v1/accounts/net-worth   → your endpoint (cash balances)
  GET /v1/goals/net-worth      → Arjun's endpoint (savings balances)

Mobile team stitches both responses into one dashboard screen.

Why two endpoints instead of one?
  Cross-module data aggregation in a single endpoint would mean
  the Accounts module directly querying the Goals schema —
  which violates module boundaries. Core Product's rule:
  modules never query each other's tables directly.
  Each module exposes its own endpoint.
  Mobile team combines them.
```

You discuss the response shape. Arjun points out that goals can be in different currencies too — a user might have a GBP savings goal and an EUR savings goal. You agree both endpoints should group by currency:

```json
// GET /v1/accounts/net-worth — agreed response shape
{
  "data": {
    "balancesByCurrency": [
      { "currency": "EUR", "amount": 2150.00 },
      { "currency": "GBP", "amount": 1842.50 }
    ],
    "accountCount": 3,
    "asOf": "2023-12-05T09:00:00Z"
  }
}

// GET /v1/goals/net-worth — Arjun's parallel endpoint
{
  "data": {
    "balancesByCurrency": [
      { "currency": "EUR", "amount": 3400.00 }
    ],
    "goalCount": 2,
    "asOf": "2023-12-05T09:00:00Z"
  }
}
```

Same structure. Mobile team adds them by currency. If a currency only appears in one response, it still works.

You also discuss credit cards. A credit card balance is a liability — it reduces net worth, not increases it. You catch this during the planning call:

```
CREDIT CARD HANDLING

User has:
  Checking account: €2,000  → asset, add to total
  Savings account:  €500    → asset, add to total
  Credit card:      €300 outstanding  → liability, SUBTRACT from total

Without this logic:
  Net worth = 2000 + 500 + 300 = €2,800 (wrong — overstates position)

With this logic:
  Net worth = 2000 + 500 - 300 = €2,200 (correct)
```

Arjun had not thought of the credit card case. You flag it. He says "good catch — goals are always assets, no equivalent issue on my side." You confirm you will handle it in your implementation.

---

#### Step 2: Building the Net Worth Endpoint

```typescript
// src/modules/accounts/account.service.ts — new method

async getNetWorth(userId: string): Promise<NetWorthResponse> {

  // Fetch all active accounts for this user
  // select only what we need — currency, balance, type
  const accounts = await this.prisma.bankAccount.findMany({
    where: {
      userId,
      isActive: true,
    },
    select: {
      currency:       true,
      currentBalance: true,
      accountType:    true,
    },
  })

  // Group balances by currency, treating credit cards as liabilities
  const balancesByCurrency = accounts.reduce(
    (acc, account) => {
      const currency = account.currency

      // Initialise currency bucket if first account in this currency
      if (!acc[currency]) {
        acc[currency] = new Decimal(0)
      }

      if (account.accountType === 'CREDIT_CARD') {
        // Credit card outstanding balance is money OWED
        // Subtract from net worth — it is a liability
        acc[currency] = acc[currency].minus(account.currentBalance)
      } else {
        // Checking, savings, investment accounts — add to net worth
        acc[currency] = acc[currency].plus(account.currentBalance)
      }

      return acc
    },
    {} as Record<string, Decimal>
  )

  // Convert to response array
  // Filter out any currency with exactly zero net balance
  const balancesArray = Object.entries(balancesByCurrency)
    .filter(([_, amount]) => !amount.isZero())
    .map(([currency, amount]) => ({
      currency,
      amount: amount.toNumber(),
    }))

  this.logger.info('Net worth calculated', {
    userId,
    currencyCount: balancesArray.length,
    accountCount:  accounts.length,
  })

  return {
    balancesByCurrency: balancesArray,
    accountCount:       accounts.length,
    asOf:               new Date().toISOString(),
  }
}
```

Let's trace through a concrete example to make sure the logic is clear:

```
CONCRETE EXAMPLE

User has 4 accounts:

  Deutsche Bank checking (EUR):  €2,150.00  → CHECKING → add
  Deutsche Bank savings (EUR):   €800.00    → SAVINGS  → add
  Monzo checking (GBP):          £1,842.50  → CHECKING → add
  Barclaycard credit (GBP):      £340.00    → CREDIT_CARD → subtract

Processing:

  acc = {}

  Account 1 (EUR CHECKING):
    acc['EUR'] = 0 + 2150.00 = €2,150.00

  Account 2 (EUR SAVINGS):
    acc['EUR'] = 2150.00 + 800.00 = €2,950.00

  Account 3 (GBP CHECKING):
    acc['GBP'] = 0 + 1842.50 = £1,842.50

  Account 4 (GBP CREDIT_CARD):
    acc['GBP'] = 1842.50 - 340.00 = £1,502.50

Final result:
  [
    { currency: 'EUR', amount: 2950.00 },
    { currency: 'GBP', amount: 1502.50 }
  ]
```

The controller:

```typescript
// src/modules/accounts/account.controller.ts — new route

@Get('net-worth')
@UseGuards(JwtAuthGuard)
async getNetWorth(
  @UserId() userId: string
): Promise<NetWorthResponse> {

  // Note: no :accountId param — this aggregates ALL accounts
  // for the authenticated user. No ownership check needed
  // beyond the JWT userId — we are querying by userId directly.
  return this.accountService.getNetWorth(userId)
}
```

Lucas reviews this PR. Two comments:

First: "why `!amount.isZero()` filter?" You explain: if a user has a credit card with exactly the same balance as their checking account in the same currency, the net is zero. Showing `{ currency: 'EUR', amount: 0 }` in the response is confusing — it implies the user has EUR accounts but no money. Filtering it out is cleaner. Lucas approves.

Second: "the `asOf` timestamp — what does this actually represent? Is it when balances were last synced or when this endpoint was called?" You think about it. You are showing the balance currently in the database — which was last updated by the sync worker. The endpoint call time is not meaningful. You update it:

```typescript
// BEFORE
asOf: new Date().toISOString()  // misleading — this is query time

// AFTER — find the most recent sync timestamp across all accounts
const mostRecentSync = accounts
  .map(a => a.lastSyncedAt)
  .filter(Boolean)
  .sort((a, b) => b!.getTime() - a!.getTime())[0]

return {
  balancesByCurrency: balancesArray,
  accountCount:       accounts.length,
  asOf: mostRecentSync?.toISOString() ?? null,
  // null if none of the accounts have been synced yet
}
```

This requires adding `lastSyncedAt` to the `select` clause. Lucas approves the updated PR.

---

#### Step 3: Reading Arjun's Code and Learning Cursor Pagination

Arjun finishes his `GET /v1/goals/net-worth` endpoint and tags you for review alongside Isabelle. You open the PR.

The endpoint itself is straightforward. But while reading his code you notice he has also built a `GET /v1/goals/:goalId/contributions` endpoint in the same PR — listing the contribution history for a savings goal. This is where you see cursor-based pagination implemented for the first time:

```typescript
// Arjun's GET /v1/goals/:goalId/contributions endpoint
// (simplified for clarity)

async getGoalContributions(
  userId:   string,
  goalId:   string,
  cursor?:  string,   // optional — absent on first page
  limit:    number = 20
): Promise<GoalContributionsResponse> {

  // Build the WHERE clause
  const where: Prisma.GoalContributionWhereInput = {
    goalId,
    goal: { userId },  // ownership check via join
  }

  // If cursor provided, only return items created BEFORE it
  // Cursor is encoded as: "{createdAt}_{contributionId}"
  if (cursor) {
    const [cursorDate, cursorId] = cursor.split('_')
    where.OR = [
      // Items with an earlier date
      { createdAt: { lt: new Date(cursorDate) } },
      // Items with the same date but earlier ID (tiebreaker)
      {
        createdAt: { equals: new Date(cursorDate) },
        id:        { lt: cursorId },
      },
    ]
  }

  // Fetch one extra item to know if there is a next page
  const contributions = await this.prisma.goalContribution.findMany({
    where,
    orderBy: { createdAt: 'desc' },
    take:    limit + 1,   // fetch limit+1 to detect next page
    select: {
      id:        true,
      amount:    true,
      note:      true,
      createdAt: true,
    },
  })

  // If we got limit+1 results, there IS a next page
  const hasMore = contributions.length > limit

  // Remove the extra item before returning
  const items = hasMore ? contributions.slice(0, limit) : contributions

  // Build cursor from the last returned item
  const nextCursor = hasMore
    ? `${items[items.length - 1].createdAt.toISOString()}_${items[items.length - 1].id}`
    : null

  return {
    items: items.map(c => ({
      id:        c.id,
      amount:    c.amount.toNumber(),
      note:      c.note ?? null,
      createdAt: c.createdAt.toISOString(),
    })),
    meta: {
      hasMore,
      nextCursor,   // client passes this back on the next request
    },
  }
}
```

You had never seen this pattern before. You read it twice. You understand the `limit + 1` trick — fetch one extra to detect whether there is a next page without running a separate COUNT query. You understand the cursor encoding — date plus ID as a tiebreaker for items created at the same timestamp.

You send Arjun a Slack message: "can you walk me through the cursor logic on a quick call? I want to make sure I understand it before I leave a review." He spends 15 minutes explaining it on a call. You ask three questions. By the end you understand it well enough that you use the same pattern in a subsequent endpoint you build in month 7.

This is a deliberate habit you are developing: when you see a pattern you do not know, ask the person who wrote it to explain it rather than pretending you understood it from reading.

---

#### Step 4: Reviewing Arjun's PR — Catching a Real Bug

After the call with Arjun, you go back to the PR and review it properly. You find two things.

**Finding 1 — Missing composite index:**

The contributions query filters by `goalId` and orders by `createdAt DESC`. There is an existing index on `goalId`, but no index on `(goalId, createdAt)`. For a user with thousands of contributions across many goals, this query would fetch all rows matching `goalId`, then sort them — a sort on a potentially large result set rather than letting the index do it.

```typescript
// Arjun's current schema for GoalContribution
model GoalContribution {
  id        String   @id @default(uuid())
  goalId    String
  amount    Decimal  @db.Decimal(15, 2)
  note      String?
  createdAt DateTime @default(now())

  goal SavingsGoal @relation(fields: [goalId], references: [id])

  @@index([goalId])   // existing index — covers filter but not sort
  @@map("goals.goal_contributions")
}
```

You leave a review comment:

```
Review comment on GoalContribution model:

"The contributions query filters by goalId and orders by
createdAt DESC. The existing @@index([goalId]) covers the
filter but not the sort — PostgreSQL still needs to sort
the filtered rows after the index scan.

A composite @@index([goalId, createdAt(sort: Desc)]) would
let PostgreSQL return rows in the right order directly from
the index — no separate sort step.

For a user with 500 contributions across one goal this might
not be visible. But at scale, or for a goal that has been
active for years, it matters.

Suggested addition:
  @@index([goalId, createdAt(sort: Desc)])
"
```

Arjun responds: "you are right — I only thought about filtering, not ordering. Adding it." He updates the schema and generates the migration.

**Finding 2 — The Missing Ownership Check:**

This is the bigger one. In the `getGoalContributions` method, Arjun's `where` clause is:

```typescript
// Arjun's original where clause
const where: Prisma.GoalContributionWhereInput = {
  goalId,
  goal: { userId },  // ownership check via relation
}
```

At first glance this looks correct — it checks `goal.userId` via a join. But you notice the `getGoalContributions` method is called from the controller like this:

```typescript
// Arjun's controller — original version
@Get(':goalId/contributions')
@UseGuards(JwtAuthGuard)
async getGoalContributions(
  @UserId() userId: string,
  @Param('goalId', ParseUUIDPipe) goalId: string,
  @Query('cursor') cursor?: string,
) {
  // userId is passed in from the JWT — correct
  // BUT: it is passed to getGoalContributions() separately
  // from goalId. What if the service method does not use it?

  return this.goalsService.getGoalContributions(goalId, cursor)
  //                                             ↑
  //                                    userId not passed!
}
```

The controller extracts `userId` from the JWT but does not pass it to the service method. The service method's signature only takes `goalId` and `cursor`:

```typescript
// Arjun's service method signature — original
async getGoalContributions(
  goalId:  string,
  cursor?: string,
): Promise<GoalContributionsResponse> {
  // userId is never used here — it is not a parameter
  // The where clause uses goal: { userId } but userId
  // is undefined inside this method
  ...
}
```

Without `userId` being passed in and used in the WHERE clause, the query is effectively:

```sql
SELECT * FROM goal_contributions
WHERE goalId = 'goal_abc'
-- userId filter is missing — any authenticated user
-- can retrieve contributions for any goalId they know
```

Any authenticated user who knows or guesses another user's goal ID can retrieve their full contribution history — how much they save each month, their notes, everything.

You leave the review comment:

```
Review comment on getGoalContributions controller:

"I think there is a security issue here. The controller
extracts userId from the JWT but does not pass it to
getGoalContributions(). The service method signature does
not take userId as a parameter, so the ownership check
via goal: { userId } in the where clause references an
undefined variable — it does not filter by the requesting
user's ID.

Any authenticated user who knows a goalId can retrieve
its contribution history.

Fix: pass userId as the first parameter to the service
method, include it in the where clause, and verify the
goal belongs to the requesting user before returning data.

Same pattern as the ownership check on GET /v1/accounts/:accountId —
always include userId in the WHERE clause, not just goalId."
```

You add this as a blocking comment — not just a suggestion, but a required change before the PR can merge.

Arjun reads it on the same day. He messages you on Slack: "you are right, I missed this completely. Fixing it now." He updates the controller to pass `userId` and the service method to use it:

```typescript
// AFTER — fixed controller
@Get(':goalId/contributions')
@UseGuards(JwtAuthGuard)
async getGoalContributions(
  @UserId() userId: string,
  @Param('goalId', ParseUUIDPipe) goalId: string,
  @Query('cursor') cursor?: string,
) {
  return this.goalsService.getGoalContributions(
    userId,   // ← now passed
    goalId,
    cursor
  )
}

// AFTER — fixed service method
async getGoalContributions(
  userId:  string,   // ← now a required parameter
  goalId:  string,
  cursor?: string,
): Promise<GoalContributionsResponse> {

  const where: Prisma.GoalContributionWhereInput = {
    goalId,
    goal: { userId },  // ← now references the actual userId param
  }

  // ... rest of the method unchanged
}
```

Your review catches a real security vulnerability before it reaches production.

---

**R — Result:**

Both endpoints ship to production in mid-December. The mobile team integrates them and the net worth dashboard goes live on the home screen.

The net worth calculation correctly handles multi-currency balances and subtracts credit card liabilities. No user sees an inflated net worth figure. No user's savings data is accessible to another user.

Arjun sends a Slack message the day after the PR merges: "good reviews — the ownership check would have been a nasty one to find after deployment. Thanks for catching it."

You remember this moment specifically because it is a reversal: in month 2, Lucas caught the exact same ownership check bug in your code. Now you catch it in Arjun's. The pattern you learned the hard way becomes pattern recognition when you read someone else's code.

---

## What Month 6 Taught You Overall

**From the net worth collaboration:** Working with another engineer on a shared feature requires upfront agreement on contracts before either person writes code. The 30-minute planning call with Arjun saved several days of back-and-forth later. The credit card handling decision — a business domain detail — came out of that conversation, not from reading any technical documentation. Cross-module work is as much a coordination exercise as a coding exercise.

**From reading Arjun's cursor pagination:** When you encounter a pattern you have not seen before, the fastest path to understanding it is asking the person who wrote it to explain it — not guessing from reading alone. The 15-minute call with Arjun taught you cursor pagination in a way that reading the code alone would not have. You use the same pattern in month 7.

**From the code review:** Your past mistakes are your most reliable checklist items. The ownership check is the first thing Lucas caught on you. It is now the first thing you look for in every endpoint you review. The value of receiving thorough code review compounds — not just in making your own code better, but in training the pattern recognition you apply when reviewing others.