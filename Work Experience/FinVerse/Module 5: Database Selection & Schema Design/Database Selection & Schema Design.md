# FinVerse — Database Selection, Architecture, Schema Design & Scaling

---

## Part 1 — Why PostgreSQL

### The Decision

FinVerse uses **PostgreSQL** as the primary database across all NestJS services. This is a deliberate, justified decision — not a default. Every engineer on the team can defend it, and you need to be able to as well.

### PostgreSQL vs MongoDB — The Real Conversation

The question is never "SQL vs NoSQL in general." That framing is meaningless. The real question is: **what is the nature of your data, and what guarantees does your application require?**

Let's answer that specifically for FinVerse.

---

**Argument 1 — Financial data is deeply and unavoidably relational**

Look at what FinVerse actually stores:

```
User
 ├── BankAccount (one user, many accounts)
 │    └── Transaction (one account, thousands of transactions)
 │          └── Category (many transactions, one category)
 │                └── Budget (one category, one budget per month)
 │
 ├── Portfolio
 │    └── Holding (one portfolio, many ETF holdings)
 │          └── Instrument (many holdings reference one instrument)
 │                └── Price (one instrument, many price points over time)
 │
 └── SavingsGoal
      └── GoalContribution (one goal, many contributions over time)
```

Every entity has foreign key relationships to other entities. A Transaction belongs to a BankAccount which belongs to a User. A Holding references an Instrument. A Budget is tied to a Category and a User and a specific month.

In MongoDB, you would model this as nested documents or manual references. Neither works cleanly here:

- **Nested documents** — you cannot nest thousands of transactions inside a bank account document. MongoDB documents have a 16MB size limit, and more importantly, you would need to update the entire document every time a new transaction arrives. This breaks down immediately at scale.
- **Manual references** — you store a `userId` field inside a transaction document and do a second query to fetch the user. This is just a foreign key without the database enforcing it. Nothing stops you from inserting a transaction with a `userId` that does not exist. The database does not care. You are now responsible for referential integrity in application code — which means bugs, data corruption, and silent inconsistencies.

PostgreSQL enforces referential integrity at the database level. A transaction row cannot exist without a valid `bankAccountId` foreign key. The database guarantees this. You cannot have orphaned records.

---

**Argument 2 — ACID transactions are non-negotiable for financial operations**

Consider what happens when a user initiates a monthly automated investment:

```
1. Deduct amount from user's available balance
2. Create an investment order record
3. Create holding records for each ETF purchased
4. Update portfolio total_invested value
5. Record the transaction in the payment ledger
```

All five of these steps must succeed together or none of them should happen. If step 3 fails after step 2 has already run, you have deducted money but created no investment. That is a financial discrepancy — potentially a regulatory violation.

PostgreSQL wraps all of this in a single ACID transaction:

```sql
BEGIN;
  UPDATE portfolio SET available_balance = available_balance - 100 WHERE id = $1;
  INSERT INTO investment_orders (...) VALUES (...);
  INSERT INTO holdings (...) VALUES (...);
  UPDATE portfolio SET total_invested = total_invested + 100 WHERE id = $1;
  INSERT INTO payment_ledger (...) VALUES (...);
COMMIT;
```

If anything fails, the entire transaction rolls back. The database is left in a consistent state.

MongoDB introduced multi-document ACID transactions in version 4.0 — but they are significantly more complex to use correctly, carry meaningful performance overhead, and are not the natural usage pattern for MongoDB. You are fighting the tool. PostgreSQL was built for exactly this.

---

**Argument 3 — Complex queries are a product requirement, not an edge case**

FinVerse's features require complex queries as a matter of course:

- "Show me all transactions in the Dining Out category between January and March, grouped by week" — a tax report query joins transactions, categories, users, and date ranges
- "Which users have a budget utilisation above 90% this month?" — an aggregate query across transactions and budgets
- "Show the top 5 spending categories for this user over the last 6 months" — GROUP BY, ORDER BY, date windowing

These are straightforward SQL queries. In MongoDB, they require aggregation pipelines — which are powerful but verbose, harder to read, and significantly harder to optimise. Your team writes SQL via Prisma — readable, type-safe, and debuggable.

---

**Argument 4 — Schema enforces data quality at the database level**

MongoDB is schemaless by default — any document shape can be inserted into any collection. This sounds flexible but is a liability in a financial application. If a bug in the application code inserts a transaction with a `null` amount, or without a `currency` field, MongoDB accepts it silently. Your application reads it later and either crashes or produces wrong numbers.

PostgreSQL's schema enforces data quality as a hard constraint:

```sql
amount     DECIMAL(15,2) NOT NULL,
currency   CHAR(3)       NOT NULL,
```

A row without `amount` or `currency` cannot be inserted. The database rejects it. The bug surfaces immediately at write time, not silently at read time months later.

---

**When would MongoDB have been the right choice?**

Be honest about this in interviews — it shows mature thinking:

MongoDB would make sense for the **Education Hub** — storing course content, lesson structures, quiz questions, rich text, embedded media references. This is genuinely document-shaped data with variable structure. No financial calculations depend on it. No ACID guarantees needed. Schema flexibility is actually useful here.

But FinVerse does not run a separate database for the Education Hub. The data volume is small, the team is small, and the operational cost of running a second database engine is not justified at this stage. PostgreSQL handles it fine with a JSONB column where needed. At Series B scale, if the Education Hub grows significantly, extracting it with its own MongoDB store would be a reasonable conversation.

**The answer to "why not MongoDB" is not "MongoDB is bad." It is "MongoDB is the wrong fit for financial data that requires relational integrity, ACID transactions, and complex aggregate queries."**

---

## Part 2 — Database Architecture: Database Per Service

### The Pattern

In a microservices architecture, the standard pattern is **database per service** — each service owns its own database, and no other service can query it directly. This is a fundamental principle of service autonomy.

FinVerse follows this pattern across services:

```
┌─────────────────────┐     ┌─────────────────────┐
│   Core Product      │     │   Payment Service   │
│   Service           │     │                     │
│                     │     │                     │
│  ┌───────────────┐  │     │  ┌───────────────┐  │
│  │  PostgreSQL   │  │     │  │  PostgreSQL   │  │
│  │  core_product │  │     │  │  payments_db  │  │
│  │  database     │  │     │  │  database     │  │
│  └───────────────┘  │     │  └───────────────┘  │
└─────────────────────┘     └─────────────────────┘

┌─────────────────────┐     ┌─────────────────────┐
│  Notification       │     │  Market Data        │
│  Service            │     │  Service (Go)       │
│                     │     │                     │
│  ┌───────────────┐  │     │  ┌───────────────┐  │
│  │  PostgreSQL   │  │     │  │  PostgreSQL   │  │
│  │  notif_db     │  │     │  │  market_db    │  │
│  └───────────────┘  │     │  └───────────────┘  │
└─────────────────────┘     └─────────────────────┘
```

In production on AWS RDS, this means either:
- **One RDS instance per service** — full isolation, higher cost, better fault isolation
- **One shared RDS instance with separate databases** — lower cost, still logically isolated, acceptable at Series A

FinVerse uses **one shared RDS instance with separate named databases per service** at Series A — a pragmatic cost decision. Each service connects only to its own database. There are no cross-database queries in application code. The separation is enforced by connection strings — Core Product's application only has credentials for `core_product_db`. It cannot connect to `payments_db` even if a developer tried.

### What This Means Inside Core Product — Schema Per Module

Core Product is a modular monolith — one deployed service, one database, but multiple domain modules. The question becomes: **how do you maintain module boundaries at the database level within a single database?**

FinVerse uses **PostgreSQL schemas** (not to be confused with "database schema" generically — in PostgreSQL, a schema is a namespace within a database):

```
core_product_db
 ├── schema: auth          (users, sessions, refresh_tokens)
 ├── schema: banking       (bank_accounts, bank_connections)
 ├── schema: transactions  (transactions, categories, merchant_rules)
 ├── schema: budgets       (budgets, budget_periods)
 ├── schema: goals         (savings_goals, goal_contributions)
 ├── schema: investing     (portfolios, holdings, instruments, orders)
 ├── schema: retirement    (retirement_plans, pension_estimates)
 ├── schema: tax           (tax_reports, tax_events)
 └── schema: education     (courses, lessons, user_progress)
```

Each module's Prisma models live in their corresponding PostgreSQL schema. A module's service code only queries its own schema directly. Cross-module data access goes through the module's exported NestJS service interface — not direct cross-schema SQL joins in application code.

**Why this matters for interviews:**
This is a deliberate architectural decision that prepares the modular monolith for future service extraction. If the Tax & Reporting module ever needs to become its own service, its data is already isolated in its own schema. Extraction becomes a matter of:
1. Point a new service at the `tax` schema
2. Migrate the schema to its own database
3. Remove the Tax module from Core Product

No data entanglement to untangle. No cross-table foreign keys to break. The schema boundary was maintained from day one.

---

## Part 3 — Prisma Schema Design

### How Prisma is Set Up in Core Product

Core Product uses a **single Prisma schema file** with all models defined together, but each model is prefixed with its module name in the table mapping to enforce the PostgreSQL schema namespace:

```prisma
// prisma/schema.prisma

generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

Table names use the `@@map` directive to route each model to its correct PostgreSQL schema:

```prisma
model User {
  // ...
  @@map("auth.users")
}

model BankAccount {
  // ...
  @@map("banking.bank_accounts")
}
```

Now let's go through each module's schema — deep on yours, working knowledge on the rest.

---

### Module 1 — Users & Auth (Lucas's ownership)

```prisma
enum SubscriptionTier {
  FREE
  PREMIUM
  FAMILY
}

enum KycStatus {
  PENDING
  VERIFIED
  REJECTED
}

model User {
  id                String           @id @default(uuid())
  email             String           @unique
  passwordHash      String
  firstName         String
  lastName          String
  country           String           // ISO 3166-1 alpha-2: "DE", "FR", "NL" etc.
  currency          String           @default("EUR")
  subscriptionTier  SubscriptionTier @default(FREE)
  kycStatus         KycStatus        @default(PENDING)
  isEmailVerified   Boolean          @default(false)
  isActive          Boolean          @default(true)
  createdAt         DateTime         @default(now())
  updatedAt         DateTime         @updatedAt

  // Relations
  bankAccounts      BankAccount[]
  portfolio         Portfolio?
  savingsGoals      SavingsGoal[]
  budgets           Budget[]
  retirementPlan    RetirementPlan?
  taxReports        TaxReport[]
  notificationPrefs NotificationPreference?

  @@map("auth.users")
}

model RefreshToken {
  id        String   @id @default(uuid())
  userId    String
  token     String   @unique
  expiresAt DateTime
  createdAt DateTime @default(now())
  isRevoked Boolean  @default(false)

  @@index([userId])
  @@index([token])
  @@map("auth.refresh_tokens")
}
```

**Design decisions worth noting:**
- `country` drives all country-specific logic downstream — tax rules, pension API selection, currency defaults
- `subscriptionTier` is the source of truth for feature access — not Stripe. Stripe is the payment processor; this field is what the application reads
- `RefreshToken` stored in PostgreSQL rather than Redis — refresh tokens need to be revokable and auditable. A compromised refresh token must be invalidatable. Redis TTL-based expiry is insufficient for this security requirement. Access tokens (short-lived, 15 min) live only in Redis

---

### Module 2 — Accounts & Open Banking (Your ownership — deep)

This is your module. You need to know every field, every relationship, every design decision, and why it was made.

```prisma
enum BankConnectionStatus {
  PENDING       // Requisition created, user has not completed consent yet
  ACTIVE        // Bank connected, tokens valid, sync running
  EXPIRED       // GoCardless access token expired, needs reconnection
  ERROR         // Sync failing consistently, manual intervention needed
  DISCONNECTED  // User manually disconnected the bank
}

enum AccountType {
  CHECKING
  SAVINGS
  CREDIT_CARD
  INVESTMENT
}

enum SyncStatus {
  IDLE
  SYNCING
  SUCCESS
  FAILED
}

model BankConnection {
  id                   String              @id @default(uuid())
  userId               String
  institutionId        String              // GoCardless institution ID e.g. "MONZO_MONZGB2L"
  institutionName      String              // Human-readable: "Monzo", "Deutsche Bank"
  requisitionId        String              @unique // GoCardless requisition ID
  status               BankConnectionStatus @default(PENDING)
  consentExpiresAt     DateTime?           // GoCardless consent window — typically 90 days
  lastRequisitionError String?             // Store error message if reconnection fails
  createdAt            DateTime            @default(now())
  updatedAt            DateTime            @updatedAt

  // Relations
  user         User          @relation(fields: [userId], references: [id], onDelete: Cascade)
  bankAccounts BankAccount[]

  @@index([userId])
  @@index([requisitionId])
  @@map("banking.bank_connections")
}

model BankAccount {
  id                 String      @id @default(uuid())
  userId             String
  bankConnectionId   String
  externalAccountId  String      @unique // GoCardless account ID
  iban               String?
  accountName        String      // e.g. "Main Current Account"
  institutionName    String
  accountType        AccountType @default(CHECKING)
  currency           String      @default("EUR")
  currentBalance     Decimal     @db.Decimal(15, 2)
  availableBalance   Decimal?    @db.Decimal(15, 2)
  isActive           Boolean     @default(true)
  syncStatus         SyncStatus  @default(IDLE)
  lastSyncedAt       DateTime?
  lastSyncError      String?     // Last error message if sync failed
  createdAt          DateTime    @default(now())
  updatedAt          DateTime    @updatedAt

  // Relations
  user           User           @relation(fields: [userId], references: [id], onDelete: Cascade)
  bankConnection BankConnection @relation(fields: [bankConnectionId], references: [id])
  transactions   Transaction[]

  @@index([userId])
  @@index([bankConnectionId])
  @@index([externalAccountId])
  @@map("banking.bank_accounts")
}

model SyncLog {
  id             String     @id @default(uuid())
  bankAccountId  String
  syncType       String     // "INITIAL_SYNC" | "PERIODIC_SYNC" | "MANUAL_SYNC"
  status         SyncStatus
  transactionsFetched  Int  @default(0)
  transactionsInserted Int  @default(0)
  transactionsDuplicate Int @default(0)
  errorMessage   String?
  startedAt      DateTime   @default(now())
  completedAt    DateTime?

  bankAccount BankAccount @relation(fields: [bankAccountId], references: [id])

  @@index([bankAccountId])
  @@index([startedAt])
  @@map("banking.sync_logs")
}
```

**Design decisions — be ready to explain every one of these:**

**`BankConnection` vs `BankAccount` — why two separate models?**
One bank connection (one GoCardless requisition, one consent flow) can expose multiple bank accounts — a user's main current account, savings account, and credit card from the same bank all come through one connection. Separating them means one failed sync on the savings account does not block transaction fetching from the current account. It also means you can reconnect a single bank connection when consent expires without touching the account records themselves.

**`consentExpiresAt` on BankConnection:**
GoCardless PSD2 consent windows are typically 90 days. After expiry, the access tokens become invalid and the connection must be re-authorised by the user. Storing this field lets you run a scheduled BullMQ job that finds connections expiring in 7 days and proactively notifies users to reconnect — rather than waiting for sync to fail.

**`currentBalance` vs `availableBalance`:**
These are different concepts in banking. `currentBalance` is the actual account balance including pending transactions. `availableBalance` is what the user can actually spend — it excludes pending outgoing transactions and held funds. Both are returned by GoCardless and stored separately.

**`Decimal(15, 2)` for all monetary values:**
Never use `Float` for money. Floating-point arithmetic is imprecise by nature — `0.1 + 0.2 !== 0.3` in floating-point. For financial data, you need exact decimal arithmetic. `Decimal(15, 2)` means up to 15 total digits with exactly 2 decimal places — sufficient for any realistic account balance in EUR.

**`SyncLog` model:**
This is your audit trail for every sync operation. It records how many transactions were fetched, how many were new inserts, how many were duplicates (already existed), and any error messages. This is what you look at in Datadog when a user reports "my transactions haven't updated." You query `sync_logs` for their `bankAccountId` and see exactly what happened in the last sync — no guessing.

---

### Module 3 — Transactions & Categorisation (Tomasz's ownership — deep, adjacent to yours)

```prisma
enum TransactionType {
  DEBIT   // Money leaving the account
  CREDIT  // Money entering the account
}

model Category {
  id          String  @id @default(uuid())
  name        String  // "Groceries", "Dining Out", "Transport", "Income"
  icon        String  // Emoji or icon identifier for mobile UI
  color       String  // Hex color for UI
  isSystem    Boolean @default(true)  // System categories vs user-created
  parentId    String? // For subcategories e.g. "Fast Food" under "Dining Out"

  parent      Category?  @relation("CategoryHierarchy", fields: [parentId], references: [id])
  children    Category[] @relation("CategoryHierarchy")
  transactions Transaction[]
  budgets     Budget[]

  @@map("transactions.categories")
}

model MerchantRule {
  id          String @id @default(uuid())
  pattern     String // Regex pattern matched against transaction description
  categoryId  String
  priority    Int    @default(0) // Higher priority rules evaluated first
  isActive    Boolean @default(true)
  createdAt   DateTime @default(now())

  category Category @relation(fields: [categoryId], references: [id])

  @@index([isActive, priority])
  @@map("transactions.merchant_rules")
}

model Transaction {
  id                  String          @id @default(uuid())
  bankAccountId       String
  userId              String
  externalId          String          @unique // GoCardless transaction ID — deduplication key
  amount              Decimal         @db.Decimal(15, 2)
  currency            String
  type                TransactionType
  description         String          // Raw description from bank
  merchantName        String?         // Cleaned merchant name if extractable
  categoryId          String?
  isCategoryManual    Boolean         @default(false) // User manually set category
  date                DateTime        // Transaction date (not processing date)
  bookingDate         DateTime?       // When it was booked by the bank
  isRecurring         Boolean         @default(false)
  recurringGroupId    String?         // Groups recurring transactions together
  metadata            Json?           // Raw GoCardless response fields
  createdAt           DateTime        @default(now())

  bankAccount BankAccount @relation(fields: [bankAccountId], references: [id])
  category    Category?   @relation(fields: [categoryId], references: [id])

  @@index([userId, date(sort: Desc)])
  @@index([bankAccountId, date(sort: Desc)])
  @@index([userId, categoryId, date(sort: Desc)])
  @@index([externalId])
  @@map("transactions.transactions")
}
```

**Design decisions:**

**`externalId` as the deduplication key:**
GoCardless assigns each transaction a unique ID. This is stored as `externalId` with a `@unique` constraint. Before inserting any transaction, the sync worker checks if this `externalId` already exists. If it does, the transaction is skipped. This prevents duplicate entries when GoCardless returns overlapping date windows on consecutive syncs — which it does regularly.

**`isCategoryManual` flag:**
When a user manually overrides a transaction's category, this flag is set to `true`. The auto-categorisation engine never overwrites manual categorisations — it checks this flag before applying rules. This respects user intent.

**`metadata Json?`:**
Raw GoCardless response fields that do not fit neatly into the schema are stored here. This gives flexibility for country-specific bank data formats without requiring a schema migration every time a new bank returns an unexpected field.

**Composite indexes on `(userId, date)`:**
The most common query pattern in the app is "show me all transactions for this user, sorted by date, optionally filtered by category." The composite index on `(userId, date DESC)` makes this fast regardless of how many total transactions exist in the table.

---

### Module 4 — Budgeting (Tomasz's ownership — working knowledge)

```prisma
model Budget {
  id          String   @id @default(uuid())
  userId      String
  categoryId  String
  amount      Decimal  @db.Decimal(15, 2)
  period      String   // "2024-01" — year-month string
  spent       Decimal  @db.Decimal(15, 2) @default(0)
  alertAt     Int      @default(80) // Alert threshold percentage
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  user     User     @relation(fields: [userId], references: [id])
  category Category @relation(fields: [categoryId], references: [id])

  @@unique([userId, categoryId, period])
  @@index([userId, period])
  @@map("budgets.budgets")
}
```

**Key point — `spent` field:**
`spent` is a denormalised running total — updated every time a new transaction arrives in this category for this period. This means the budget screen reads one row to show budget utilisation — it does not need to `SUM` all transactions on every screen load. The trade-off is that `spent` must be kept in sync when transactions are categorised, re-categorised, or deleted. This is handled inside the Transactions module's service — whenever a transaction's category changes, it updates the relevant Budget rows.

---

### Module 5 — Goals & Savings (Arjun's ownership — working knowledge)

```prisma
enum GoalStatus {
  ACTIVE
  COMPLETED
  PAUSED
  CANCELLED
}

model SavingsGoal {
  id              String     @id @default(uuid())
  userId          String
  name            String
  targetAmount    Decimal    @db.Decimal(15, 2)
  currentAmount   Decimal    @db.Decimal(15, 2) @default(0)
  currency        String     @default("EUR")
  targetDate      DateTime?
  status          GoalStatus @default(ACTIVE)
  autoTransferDay Int?       // Day of month for automatic contribution
  autoTransferAmt Decimal?   @db.Decimal(15, 2)
  createdAt       DateTime   @default(now())
  updatedAt       DateTime   @updatedAt

  user          User               @relation(fields: [userId], references: [id])
  contributions GoalContribution[]

  @@index([userId, status])
  @@map("goals.savings_goals")
}

model GoalContribution {
  id          String   @id @default(uuid())
  goalId      String
  amount      Decimal  @db.Decimal(15, 2)
  note        String?
  createdAt   DateTime @default(now())

  goal SavingsGoal @relation(fields: [goalId], references: [id])

  @@index([goalId, createdAt])
  @@map("goals.goal_contributions")
}
```

---

### Modules 6–9 — Investing, Retirement, Tax, Education (Elena & Isabelle's ownership — shape only)

```prisma
// INVESTING MODULE — Elena's ownership
model Portfolio {
  id              String   @id @default(uuid())
  userId          String   @unique
  riskProfile     String   // "CONSERVATIVE" | "MODERATE" | "GROWTH"
  totalInvested   Decimal  @db.Decimal(15, 2) @default(0)
  monthlyAmount   Decimal  @db.Decimal(15, 2)
  isActive        Boolean  @default(true)
  createdAt       DateTime @default(now())

  user     User      @relation(fields: [userId], references: [id])
  holdings Holding[]
  orders   InvestmentOrder[]

  @@map("investing.portfolios")
}

model Instrument {
  id       String @id @default(uuid())
  symbol   String @unique // e.g. "VWCE.DE"
  name     String         // "Vanguard FTSE All-World UCITS ETF"
  isin     String @unique
  exchange String         // "XETRA", "EURONEXT"
  currency String

  holdings Holding[]

  @@map("investing.instruments")
}

model Holding {
  id           String  @id @default(uuid())
  portfolioId  String
  instrumentId String
  units        Decimal @db.Decimal(15, 6) // ETF units — needs 6 decimal places
  avgBuyPrice  Decimal @db.Decimal(15, 4)
  createdAt    DateTime @default(now())
  updatedAt    DateTime @updatedAt

  portfolio  Portfolio  @relation(fields: [portfolioId], references: [id])
  instrument Instrument @relation(fields: [instrumentId], references: [id])

  @@unique([portfolioId, instrumentId])
  @@map("investing.holdings")
}

model InvestmentOrder {
  id          String   @id @default(uuid())
  portfolioId String
  amount      Decimal  @db.Decimal(15, 2)
  status      String   // "PENDING" | "COMPLETED" | "FAILED"
  executedAt  DateTime?
  createdAt   DateTime @default(now())

  portfolio Portfolio @relation(fields: [portfolioId], references: [id])

  @@index([portfolioId, createdAt])
  @@map("investing.investment_orders")
}

// RETIREMENT MODULE — Elena's ownership
model RetirementPlan {
  id                  String   @id @default(uuid())
  userId              String   @unique
  targetRetirementAge Int
  currentAge          Int
  monthlyContribution Decimal  @db.Decimal(15, 2)
  statePensionEstimate Decimal? @db.Decimal(15, 2)
  updatedAt           DateTime @updatedAt

  user User @relation(fields: [userId], references: [id])

  @@map("retirement.retirement_plans")
}

// TAX MODULE — Isabelle's ownership
model TaxReport {
  id          String   @id @default(uuid())
  userId      String
  taxYear     Int
  country     String
  s3Key       String   // Path to PDF in AWS S3
  generatedAt DateTime @default(now())
  status      String   // "GENERATING" | "READY" | "FAILED"

  user User @relation(fields: [userId], references: [id])

  @@unique([userId, taxYear])
  @@index([userId])
  @@map("tax.tax_reports")
}

// EDUCATION MODULE — Isabelle + Priya's ownership
model Course {
  id          String   @id @default(uuid())
  title       String
  description String
  isPremium   Boolean  @default(false)
  createdAt   DateTime @default(now())

  lessons     Lesson[]
  userProgress UserCourseProgress[]

  @@map("education.courses")
}

model UserCourseProgress {
  id            String   @id @default(uuid())
  userId        String
  courseId      String
  completedAt   DateTime?
  lastLessonId  String?

  course Course @relation(fields: [courseId], references: [id])

  @@unique([userId, courseId])
  @@map("education.user_course_progress")
}
```

---

## Part 4 — Migrations

### How Prisma Migrations Work

Prisma migrations are the mechanism for evolving the database schema over time in a controlled, versioned, and reversible way. Understanding this end-to-end is important — schema changes in production are one of the riskiest operations a backend engineer performs.

**The development workflow:**

```bash
# Step 1 — You modify the Prisma schema file
# e.g. add a new field to BankAccount:
# syncRetryCount Int @default(0)

# Step 2 — Generate a migration file
npx prisma migrate dev --name add_sync_retry_count_to_bank_accounts

# Prisma does three things:
# 1. Computes the diff between current schema and last migration state
# 2. Generates a SQL migration file in prisma/migrations/{timestamp}_{name}/
# 3. Applies it to your local development database immediately
```

The generated SQL file looks like:
```sql
-- prisma/migrations/20240315_add_sync_retry_count_to_bank_accounts/migration.sql
ALTER TABLE "banking"."bank_accounts"
ADD COLUMN "syncRetryCount" INTEGER NOT NULL DEFAULT 0;
```

This file is committed to Git alongside the schema change. It is the source of truth for what changed and when.

**The production deployment workflow:**

Production migrations never run automatically — they are a deliberate step in the CI/CD pipeline:

```bash
# Run during deployment, before the new application code starts
npx prisma migrate deploy
```

`migrate deploy` only applies pending migrations — migrations that exist in the `prisma/migrations` folder but have not yet been applied to the target database. It does not generate new migrations. It is safe to run in CI/CD.

**Migration state tracking:**
Prisma maintains a `_prisma_migrations` table in the database. Every applied migration is recorded here with its name, checksum, and timestamp. `migrate deploy` reads this table to know which migrations are pending.

---

### Safe Migration Practices the Team Follows

**Rule 1 — Never use `migrate reset` in production.**
`migrate reset` drops the entire database and replays all migrations from scratch. It is for local development only. In production, only `migrate deploy`.

**Rule 2 — Additive migrations only for live deployments.**
Adding a new column, adding a new table, adding a new index — these are safe. Dropping a column, renaming a column, changing a column type — these are dangerous. The team follows a **expand-contract pattern** for breaking changes:

```
Step 1 (Expand):   Add the new column alongside the old one
Step 2 (Migrate):  Deploy application code that writes to both columns
Step 3 (Backfill): Run a background job to populate the new column for existing rows
Step 4 (Switch):   Deploy application code that reads from the new column only
Step 5 (Contract): Drop the old column in a separate migration
```

This means no single deployment is a breaking change. The database is always compatible with both the current and the previous version of the application code — which makes rollbacks safe.

**Rule 3 — Large table migrations use `CONCURRENTLY`.**
Adding an index to the `transactions` table when there are millions of rows takes time. A standard `CREATE INDEX` locks the table for writes during the index build. For large tables, the team writes raw SQL migrations using `CREATE INDEX CONCURRENTLY` — which builds the index without locking:

```sql
-- In a raw SQL migration file
CREATE INDEX CONCURRENTLY idx_transactions_user_date
ON "transactions"."transactions" ("userId", "date" DESC);
```

**Rule 4 — Every migration is reviewed by Lucas.**
Schema changes affect every module that touches the modified table. They also cannot be easily rolled back once data has been written under the new schema. Lucas reviews all migration files in PRs — it is one of the few things that requires his explicit approval regardless of which module it belongs to.

---

## Part 5 — Indexes

### How to Think About Indexes

An index is a data structure that PostgreSQL maintains alongside a table to make certain queries faster. Without an index, PostgreSQL does a **sequential scan** — it reads every row in the table to find the ones matching your query condition. With an index on the relevant column, PostgreSQL jumps directly to the matching rows.

The trade-off: indexes make reads faster but make writes slightly slower (the index must be updated on every INSERT, UPDATE, DELETE) and consume additional disk space.

**The rule for when to add an index:**
Add an index when a column (or combination of columns) is frequently used in `WHERE`, `ORDER BY`, `JOIN ON`, or `GROUP BY` clauses in queries that need to be fast. Do not add indexes speculatively — every index has a maintenance cost.

### Indexes in the Core Product Schema

**`transactions` table — the most queried table in the system:**

```sql
-- Most common query: all transactions for a user, newest first
@@index([userId, date(sort: Desc)])

-- Filtered by category (budget screen, spending breakdown)
@@index([userId, categoryId, date(sort: Desc)])

-- Sync deduplication check
@@index([externalId]) -- covered by @unique constraint automatically

-- Per-account transaction list
@@index([bankAccountId, date(sort: Desc)])
```

The `(userId, date DESC)` composite index is the most important index in the database. Every transaction list view in the mobile app hits this index. Without it, fetching a user's last 50 transactions would require scanning the entire transactions table.

**`bank_accounts` table:**

```sql
@@index([userId])              -- "show all accounts for this user"
@@index([externalAccountId])   -- covered by @unique, used in sync deduplication
```

**`bank_connections` table:**

```sql
@@index([userId])        -- "show all connections for this user"
@@index([requisitionId]) -- covered by @unique, used in GoCardless callback lookup
```

**`budgets` table:**

```sql
@@unique([userId, categoryId, period]) -- prevents duplicate budgets, also acts as index
@@index([userId, period])              -- "show all budgets for this user this month"
```

**`refresh_tokens` table:**

```sql
@@index([userId])  -- "revoke all tokens for this user" (on logout)
@@index([token])   -- covered by @unique, used on every token validation
```

---

### How You Debug Performance Bottlenecks

This is a question interviewers love asking. "We had a slow query — how did you find and fix it?"

**Step 1 — Identify the slow query via Datadog APM**

Datadog's APM traces every database query made by the application, with timing. When a specific API endpoint is slow (e.g. the transaction list endpoint taking 800ms instead of the expected 50ms), you open Datadog, find the trace for that request, and look at the database spans. You can see exactly which query ran, how long it took, and how many rows it scanned.

**Step 2 — Run EXPLAIN ANALYZE in PostgreSQL**

Once you have the slow query, you run it directly in PostgreSQL with `EXPLAIN ANALYZE`:

```sql
EXPLAIN ANALYZE
SELECT * FROM "transactions"."transactions"
WHERE "userId" = 'usr_123'
ORDER BY "date" DESC
LIMIT 50;
```

The output tells you:
- Whether PostgreSQL is using an index or doing a sequential scan (`Seq Scan` is the warning sign)
- How many rows were scanned vs how many were returned
- How long each step took
- Estimated vs actual row counts (a big difference here means stale statistics — run `ANALYZE` to update them)

**A real example from your module:**
Early in your ownership of the Accounts module, you noticed the bank account sync status endpoint was slow for users with many accounts. `EXPLAIN ANALYZE` showed a sequential scan on `bank_accounts` for the `userId` filter — the index existed but PostgreSQL was not using it because the query was also filtering on `isActive = true` and PostgreSQL estimated a full scan was cheaper (stale statistics). Running `ANALYZE banking.bank_accounts` updated the statistics and PostgreSQL switched to the index. Response time dropped from 340ms to 12ms.

**Step 3 — Consider query structure**

Sometimes the index is fine but the query is inefficient. Common patterns that cause problems:

- `SELECT *` — fetching all columns when only 3 are needed. Prisma's `select` clause fixes this.
- N+1 queries — fetching a list of bank accounts, then making a separate query for each account's last transaction. Prisma's `include` with proper eager loading fixes this.
- Missing pagination — fetching all 5,000 transactions instead of the first 50. Always paginate with `take` and `skip` (or cursor-based pagination for large datasets).

---

## Part 6 — Scaling the Modular Monolith's Database

### The Current State — Single Database, Multiple Schemas

Right now at Series A, Core Product runs one PostgreSQL database with multiple schemas — one per module. This works well for 450K users. But as FinVerse scales toward Series B's target of 1.5M users, certain tables will grow significantly:

- `transactions` — could reach 500M+ rows (1.5M users × average 1,000 transactions per year × 3 years)
- `bank_accounts` — millions of rows
- `sync_logs` — very high write volume

Here is how the database scales at each stage:

---

**Stage 1 — Read replicas (near-term, Series A → B)**

The first scaling lever. AWS RDS supports read replicas — a separate PostgreSQL instance that receives a stream of all writes from the primary and stays in sync. Read-heavy queries (transaction lists, budget summaries, portfolio views) are routed to the read replica. Write operations (new transactions, balance updates) go to the primary.

In Prisma, this is configured by pointing read operations at a separate `DATABASE_URL_REPLICA`:

```typescript
// Using Prisma's read replica extension
const prisma = new PrismaClient().$extends(
  readReplicas({
    url: process.env.DATABASE_URL_REPLICA
  })
)
```

This doubles effective read capacity with minimal application code change.

---

**Stage 2 — Table partitioning for transactions (medium-term)**

The `transactions` table will be the first to show stress at scale. The solution is **PostgreSQL table partitioning** — splitting one logical table into multiple physical partitions based on a partition key.

For transactions, the natural partition key is `date` — partitioned by month or quarter:

```sql
-- Partition transactions by month
CREATE TABLE transactions PARTITION BY RANGE (date);

CREATE TABLE transactions_2024_01
  PARTITION OF transactions
  FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE transactions_2024_02
  PARTITION OF transactions
  FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

When a query filters by `date` — which almost every transaction query does — PostgreSQL only scans the relevant partition(s) rather than the entire table. A query for January 2024 transactions only touches the `transactions_2024_01` partition — a fraction of the total data.

This is **partition pruning** — PostgreSQL automatically eliminates irrelevant partitions from the query plan.

---

**Stage 3 — Module extraction to separate services (Series B, when justified)**

This is the most significant scaling step — and the most expensive. When a specific module's database load is high enough that it is affecting other modules sharing the same PostgreSQL instance, that module gets extracted into its own service with its own database.

The modular monolith's schema separation was specifically designed for this moment:

```
Before extraction:
core_product_db
 ├── schema: auth
 ├── schema: banking        ← high load
 ├── schema: transactions   ← high load
 ├── schema: budgets
 └── ... other schemas

After extraction:
core_product_db              banking_service_db
 ├── schema: auth             ├── schema: banking
 ├── schema: budgets          └── schema: transactions
 └── ... other schemas
```

The extraction process:
1. Create a new `banking-service` NestJS application
2. Point it at a new PostgreSQL database
3. Run `pg_dump` on the `banking` and `transactions` schemas and restore them to the new database
4. Update Core Product to call the new Banking Service's REST API instead of querying its own database directly
5. Remove the `banking` and `transactions` schemas from `core_product_db`

Because module boundaries were maintained from day one — no cross-schema SQL joins in application code, no direct table access from outside the module — this extraction is a well-understood mechanical process, not an untangling exercise.

---

## Part 7 — Consistency Patterns & Microservice Design Patterns

### Where Eventual Consistency is Acceptable in FinVerse

Not everything in FinVerse needs to be immediately consistent. Understanding which data can be eventually consistent is important — it is what allows the system to be decoupled, scalable, and resilient.

**Acceptable eventual consistency:**

- **Notification delivery** — if a budget alert email arrives 3 seconds after the threshold was crossed instead of instantly, that is fine. The event goes through RabbitMQ asynchronously. Nobody is harmed by a 3-second delay.
- **Portfolio valuation** — the portfolio screen shows a cached valuation from Redis, updated every 15 minutes. A user sees a value that is up to 15 minutes old. For a long-term investment platform (not a trading platform), this is perfectly acceptable.
- **Transaction sync** — bank transactions appear in the app within 4 hours of occurring. This is GoCardless's PSD2 polling model — not a real-time push. Users know this.
- **Tax report generation** — runs as a background job. The report is ready minutes or hours after the job is triggered. The user gets notified when it is ready.

**Where strong consistency is non-negotiable:**

- **Payment processing** — charging a user and recording the payment must be atomic. You cannot charge €100 and fail to record it, or record it without charging.
- **Investment order execution** — deducting balance, creating order, creating holdings — must be a single atomic transaction.
- **Subscription tier changes** — when a user upgrades to Premium, the `subscriptionTier` field must update immediately and atomically with the Stripe subscription creation. A user who paid for Premium but cannot access Premium features is a critical bug.
- **Authentication** — token issuance and user status updates must be consistent immediately.

---

### The Outbox Pattern — Does FinVerse Use It?

**What problem does the Outbox pattern solve?**

Consider this scenario in Core Product:

```
1. A new transaction sync completes
2. Core Product saves new transactions to PostgreSQL ✅
3. Core Product publishes budget.threshold.exceeded to RabbitMQ
   → RabbitMQ is temporarily unavailable ❌
4. The event is lost — Notification Service never fires
```

You now have a state inconsistency — the database says the budget threshold was exceeded, but the user never got notified. If the application retries the publish in memory and crashes before retrying, the event is permanently lost.

**The Outbox pattern solves this:**

Instead of publishing directly to RabbitMQ, the application writes the event to an `outbox` table in the same PostgreSQL transaction as the business data:

```prisma
model OutboxEvent {
  id          String   @id @default(uuid())
  eventType   String   // "budget.threshold.exceeded"
  payload     Json
  status      String   @default("PENDING") // "PENDING" | "PUBLISHED" | "FAILED"
  createdAt   DateTime @default(now())
  publishedAt DateTime?

  @@index([status, createdAt])
  @@map("outbox.outbox_events")
}
```

```typescript
// In a single PostgreSQL transaction:
await prisma.$transaction([
  prisma.transaction.createMany({ data: newTransactions }),
  prisma.budget.update({ where: { id: budgetId }, data: { spent: newSpent } }),
  prisma.outboxEvent.create({
    data: {
      eventType: 'budget.threshold.exceeded',
      payload: { userId, category, spent, limit }
    }
  })
])
```

A separate BullMQ worker polls the `outbox_events` table for `PENDING` events and publishes them to RabbitMQ:

```typescript
// OutboxPublisher worker — runs every 5 seconds
const pendingEvents = await prisma.outboxEvent.findMany({
  where: { status: 'PENDING' },
  orderBy: { createdAt: 'asc' },
  take: 100
})

for (const event of pendingEvents) {
  await rabbitMQChannel.publish(event.eventType, event.payload)
  await prisma.outboxEvent.update({
    where: { id: event.id },
    data: { status: 'PUBLISHED', publishedAt: new Date() }
  })
}
```

**Does FinVerse use the Outbox pattern?**

Yes — but selectively, only for events where losing the message would cause a real user-facing problem:

- `budget.threshold.exceeded` — user not getting notified is a product failure
- `investment.order.paid` — portfolio not updating after a payment is a financial discrepancy
- `subscription.activated` — user paying but not getting Premium access is critical

For lower-stakes events — like `user.lesson.completed` in the Education Hub — direct RabbitMQ publish is acceptable. If the notification is lost, nothing significant happens.

---

### The SAGA Pattern — Does FinVerse Use It?

**What problem does SAGA solve?**

SAGA is a pattern for managing distributed transactions — operations that span multiple services where you need all-or-nothing semantics but cannot use a single database transaction because the data lives in different services with different databases.

The canonical example in FinVerse would be an investment order that spans Payment Service and Core Product:

```
1. Payment Service: charge user via Stripe      ✅
2. Core Product: create investment order         ✅
3. Core Product: create holding records          ❌ (fails)

Now: money has been charged but no investment was created
```

**Does FinVerse implement a full SAGA?**

At FinVerse's current scale and architecture, a full choreography-based SAGA is not implemented — and for good reason. The investment order flow handles this through a combination of:

1. **The Outbox pattern** ensuring the `investment.order.paid` event is reliably delivered to Core Product after payment succeeds
2. **Idempotent consumers** in Core Product — if the event is delivered twice (RabbitMQ at-least-once delivery), processing it twice produces the same result as processing it once. Duplicate holding creation is prevented by a `@@unique([portfolioId, instrumentId])` constraint.
3. **Compensating logic in Payment Service** — if an investment order fails in Core Product after payment, a reconciliation BullMQ job (runs hourly) detects orders in `PAID` status with no corresponding holdings and either retries the Core Product update or initiates a refund.

A full SAGA implementation — with explicit compensation transactions and a SAGA orchestrator — would be the right pattern if FinVerse had 5+ services all involved in a single business transaction. At the current service count, the combination of Outbox + idempotent consumers + reconciliation jobs achieves the same safety guarantee with less architectural complexity.

---

That is the complete Step 5 — database selection, architecture, schema design, migrations, indexes, performance debugging, scaling strategy, and consistency patterns. Every section written to be genuinely understood, not just documented.

**Ready for Step 6: RabbitMQ & BullMQ?**