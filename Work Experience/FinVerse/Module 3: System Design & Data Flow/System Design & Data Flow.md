# FinVerse — System Design & Data Flows

## Overview

This document covers the major end-to-end flows across the FinVerse system — the flows that cross service boundaries, involve async messaging, background job processing, or third-party integrations. These are the flows that reveal how the whole system actually works together.

**Flows covered:**

1. User Registration & Onboarding
2. Bank Account Connection (Open Banking / PSD2)
3. Transaction Sync & Categorisation
4. Budget Alert — End-to-End
5. Investment Order — End-to-End
6. Subscription Payment (Stripe)
7. Portfolio Valuation & Market Data Refresh
8. Tax Report Generation

---

## Flow 1 — User Registration & Onboarding

**What happens:** A new user downloads the React Native app, creates an account, verifies their email, and lands on the onboarding screen.

```
React Native App
      │
      │  POST /v1/auth/register
      │  { email, password, country, name }
      ▼
API Gateway
      │  Validates request shape
      │  No JWT check (public endpoint)
      │  Forwards to Core Product
      ▼
Core Product — Users & Auth Module
      │
      ├─ 1. Validates email uniqueness (PostgreSQL query)
      ├─ 2. Hashes password (bcrypt)
      ├─ 3. Creates user record in DB (status: PENDING_VERIFICATION)
      ├─ 4. Generates email verification token (UUID, stored in Redis, TTL: 24hrs)
      ├─ 5. Publishes event to RabbitMQ:
      │        exchange: user.events
      │        routing key: user.registered
      │        payload: { userId, email, name, country }
      │
      ├─ 6. Returns 201 to API Gateway → React Native App
      │        { message: "Verification email sent" }
      │
RabbitMQ
      │
      ▼
Notification Service
      │  Consumes user.registered event
      ├─ Renders welcome + verification email template
      ├─ Sends via SendGrid
      └─ Records delivery in notification_log table

─────────────────────────────────────────────
User clicks verification link in email
─────────────────────────────────────────────

React Native App
      │
      │  POST /v1/auth/verify-email
      │  { token }
      ▼
API Gateway → Core Product — Users & Auth Module
      │
      ├─ 1. Looks up token in Redis — validates it exists and is not expired
      ├─ 2. Marks user status: ACTIVE in PostgreSQL
      ├─ 3. Deletes token from Redis
      ├─ 4. Issues JWT (access token: 15min TTL, refresh token: 30 days)
      └─ 5. Returns 200 with JWT pair → App navigates to onboarding flow
```

**Key design points:**
- Email verification token lives in Redis, not PostgreSQL — it is short-lived, has a TTL, and does not need to be part of the relational data model
- The welcome email is sent asynchronously via RabbitMQ — registration response is not blocked by email delivery
- If Notification Service is down, the event stays in the RabbitMQ queue and is processed when the service recovers — the user registration itself is unaffected

---

## Flow 2 — Bank Account Connection (Open Banking / PSD2)

**What happens:** An authenticated user connects their bank account through the GoCardless Bank Account Data integration so FinVerse can read their transactions.

```
React Native App
      │
      │  POST /v1/accounts/connect
      │  { bankId, countryCode }
      │  Authorization: Bearer <JWT>
      ▼
API Gateway
      │  Validates JWT
      │  Forwards to Core Product
      ▼
Core Product — Accounts & Open Banking Module
      │
      ├─ 1. Calls GoCardless API (external HTTP):
      │        POST https://bankaccountdata.gocardless.com/api/v2/requisitions/
      │        { institution_id, redirect_uri, reference }
      │
      ├─ 2. GoCardless returns a requisition_id and a redirect link
      │        (this link opens the bank's own consent screen)
      │
      ├─ 3. Stores requisition_id in PostgreSQL (status: PENDING)
      │
      └─ 4. Returns redirect URL to React Native App
               → App opens bank consent screen in WebView

─────────────────────────────────────────────
User completes bank consent on bank's screen
GoCardless redirects back to FinVerse redirect URI
─────────────────────────────────────────────

      │  GET /v1/accounts/connect/callback?ref={reference}
      ▼
Core Product — Accounts & Open Banking Module
      │
      ├─ 1. Looks up requisition by reference in PostgreSQL
      ├─ 2. Calls GoCardless API to confirm requisition status
      ├─ 3. Fetches list of account IDs linked under this requisition
      ├─ 4. For each account:
      │        - Fetches account metadata (IBAN, currency, bank name)
      │        - Creates account record in PostgreSQL (status: ACTIVE)
      │
      ├─ 5. Enqueues BullMQ job:
      │        queue: transaction-sync
      │        job: INITIAL_SYNC
      │        payload: { userId, accountIds }
      │        (runs immediately, one-time initial transaction pull)
      │
      └─ 6. Returns success to App → App shows connected accounts screen
```

**Key design points:**
- The actual bank consent flow happens on GoCardless / the bank's own UI — FinVerse never handles bank credentials. This is the fundamental PSD2 security model.
- The initial transaction sync is enqueued as a BullMQ job rather than running inline in the request — bank APIs can be slow (1–3 seconds per account), and a user might have 3–5 accounts. Running this inline would make the callback response take 10–15 seconds. The job runs in the background and the app polls or receives a push notification when sync is complete.

---

## Flow 3 — Transaction Sync & Categorisation

**What happens:** FinVerse periodically fetches new transactions from connected bank accounts and categorises them automatically.

```
BullMQ Scheduler (inside Core Product)
      │
      │  Recurring job fires every 4 hours per active user
      │  job: PERIODIC_SYNC
      │  payload: { userId, accountIds }
      ▼
BullMQ Worker — Transaction Sync
      │
      ├─ 1. For each accountId:
      │        Calls GoCardless API:
      │        GET /accounts/{accountId}/transactions?date_from={lastSyncAt}
      │
      ├─ 2. Receives raw transaction list from GoCardless
      │        { amount, currency, date, description, creditorName }
      │
      ├─ 3. Deduplication check:
      │        For each transaction, check if transaction_external_id
      │        already exists in PostgreSQL — skip if duplicate
      │
      ├─ 4. Categorisation:
      │        For each new transaction, run categorisation logic:
      │        - Rule-based matching (regex on description/creditorName)
      │          e.g. "NETFLIX" → Entertainment
      │               "LIDL" or "ALDI" → Groceries
      │               "SALARY" → Income
      │        - If no rule matches → category: Uncategorised
      │
      ├─ 5. Bulk insert new transactions into PostgreSQL
      │        (single INSERT with all new transactions — not one-by-one)
      │
      ├─ 6. Update account.last_synced_at timestamp
      │
      ├─ 7. Check budget thresholds for affected categories:
      │        For each category that received new spend,
      │        check if monthly budget limit is now exceeded or approaching
      │
      ├─ 8. If budget threshold breached → publish event to RabbitMQ:
      │        exchange: finance.events
      │        routing key: budget.threshold.exceeded
      │        payload: { userId, category, spent, limit, percentage }
      │
      └─ 9. Update goal progress if any transaction matches a savings goal rule
```

**Key design points:**
- Sync runs as a BullMQ recurring job — not a cron job running on the server, because BullMQ handles retries, failure tracking, and concurrency control natively
- Deduplication is critical — GoCardless may return overlapping transaction windows on consecutive syncs. Each transaction has a GoCardless-assigned external ID that is used as the uniqueness key
- Bulk insert rather than per-transaction insert — for an initial sync a user might have 3 years of transaction history across 4 accounts. Inserting row-by-row would be extremely slow. A single parameterised bulk insert handles this efficiently
- Budget threshold check happens inside the same worker pass — no need for a separate job or service call

---

## Flow 4 — Budget Alert End-to-End

**What happens:** A user's spending in a category crosses their monthly budget threshold. They receive a push notification and email.

```
Core Product — BullMQ Worker
(continuing from Flow 3, step 8)
      │
      │  Publishes to RabbitMQ:
      │  exchange: finance.events (topic exchange)
      │  routing key: budget.threshold.exceeded
      │  payload: {
      │    userId: "usr_123",
      │    category: "Dining Out",
      │    spent: 187.50,
      │    limit: 200.00,
      │    percentage: 93.75,
      │    currency: "EUR"
      │  }
      ▼
RabbitMQ
      │
      ▼
Notification Service — Consumer
      │
      ├─ 1. Receives and acknowledges message from queue
      │
      ├─ 2. Loads user notification preferences from PostgreSQL:
      │        - push_notifications: true
      │        - email_alerts: true
      │        - budget_alerts: true
      │
      ├─ 3. Checks rate limiting:
      │        Has this user already received a budget alert
      │        for this category today? (check Redis key)
      │        If yes → skip (avoid alert spam)
      │        If no → proceed
      │
      ├─ 4. Sends push notification via FCM/APNs:
      │        title: "Dining Out — 94% of budget used"
      │        body: "You've spent €187.50 of your €200 limit this month"
      │
      ├─ 5. Sends email via SendGrid:
      │        Template: budget_alert
      │        Variables: { category, spent, limit, percentage, remaining }
      │
      ├─ 6. Sets Redis key: alert_sent:{userId}:budget:{category}:{date}
      │        TTL: 24 hours (prevents duplicate alerts same day)
      │
      └─ 7. Writes to notification_log table in PostgreSQL:
               { userId, type: BUDGET_ALERT, channel: [PUSH, EMAIL],
                 status: DELIVERED, sentAt }
```

**Key design points:**
- Core Product does not know Notification Service exists — it simply publishes an event. This is the decoupling RabbitMQ provides. If Notification Service is down, the event queues up and is processed when it recovers. Core Product is unaffected.
- Alert rate limiting in Redis prevents a user from being spammed if multiple transactions come in quickly in the same category
- Notification preferences are respected — if a user has turned off email alerts, only push is sent

---

## Flow 5 — Investment Order End-to-End

**What happens:** A user's monthly automated ETF investment triggers — funds are charged, the order is placed, the portfolio is updated.

```
BullMQ Scheduler (inside Payment Service)
      │
      │  Recurring job fires on each user's investment date
      │  job: MONTHLY_INVESTMENT_ORDER
      │  payload: { userId, portfolioId, amount, currency }
      ▼
BullMQ Worker — Investment Orders
      │
      ├─ 1. Load user's active investment plan from PostgreSQL:
      │        { riskProfile, targetAllocation, monthlyAmount }
      │
      ├─ 2. Fetch current ETF prices from Market Data Service:
      │        GET /internal/market-data/prices?symbols=[...]
      │        (Market Data Service serves from Redis cache)
      │
      ├─ 3. Calculate units to purchase for each ETF:
      │        Based on target allocation % × monthly amount ÷ current price
      │
      ├─ 4. Initiate Stripe charge:
      │        Stripe PaymentIntent for { amount, currency, customerId }
      │
      ├─ 5a. If Stripe charge succeeds:
      │        - Record payment in payment_ledger table
      │        - Publish event to RabbitMQ:
      │            routing key: investment.order.paid
      │            payload: { userId, portfolioId, amount, allocations }
      │
      ├─ 5b. If Stripe charge fails:
      │        - Record failure in payment_ledger table
      │        - Publish event:
      │            routing key: payment.failed
      │            payload: { userId, reason, retryAt }
      │        - BullMQ retries job with exponential backoff (max 3 attempts)
      │
      ▼
RabbitMQ
      │
      ├─────────────────────────────────────────────────┐
      ▼                                                 ▼
Core Product                                  Notification Service
Investing Module                                       │
      │                                        Consumes investment.order.paid
      │  Consumes investment.order.paid         Sends push notification:
      │                                         "Your €100 investment
      ├─ 1. Creates holding records             is confirmed"
      │        in PostgreSQL for each           Sends email receipt
      │        ETF unit purchased
      │
      ├─ 2. Updates portfolio
      │        total_invested,
      │        last_invested_at
      │
      └─ 3. Triggers portfolio
             revaluation request
             to Market Data Service
```

**Key design points:**
- ETF prices are fetched from Market Data Service's Redis cache — not by making a live EODHD API call at order time. This keeps order processing fast and avoids hitting external rate limits during batch investment runs
- The payment charge and the portfolio update are decoupled via RabbitMQ — Payment Service handles money, Core Product handles portfolio state. They are not in the same transaction. If portfolio update fails after payment succeeds, the `investment.order.paid` event remains in the queue and will be reprocessed. This is an eventually consistent design — the money movement and the portfolio record will converge
- Both Core Product and Notification Service consume the same `investment.order.paid` event independently — RabbitMQ delivers the event to both queues

---

## Flow 6 — Subscription Payment (Stripe)

**What happens:** A user upgrades from Free to Premium. Stripe charges them and their account is upgraded.

```
React Native App
      │
      │  POST /v1/subscriptions/upgrade
      │  { plan: "PREMIUM", paymentMethodId: "pm_xxx" }
      │  Authorization: Bearer <JWT>
      ▼
API Gateway → Payment Service
      │
      ├─ 1. Validates user does not already have active Premium subscription
      │
      ├─ 2. Creates or retrieves Stripe Customer:
      │        stripe.customers.create({ email, name, metadata: { userId } })
      │
      ├─ 3. Creates Stripe Subscription:
      │        stripe.subscriptions.create({
      │          customer: stripeCustomerId,
      │          items: [{ price: PREMIUM_PRICE_ID }],
      │          default_payment_method: paymentMethodId,
      │          payment_behavior: 'default_incomplete'
      │        })
      │
      ├─ 4. Records subscription in PostgreSQL:
      │        { userId, plan: PREMIUM, stripeSubscriptionId,
      │          status: ACTIVE, startedAt, nextBillingAt }
      │
      ├─ 5. Publishes event to RabbitMQ:
      │        routing key: subscription.activated
      │        payload: { userId, plan: PREMIUM }
      │
      └─ 6. Returns 200 to App → App unlocks Premium UI
      │
      ▼
RabbitMQ
      │
      ├──────────────────────────────┐
      ▼                              ▼
Core Product                Notification Service
Users Module                       │
      │                    Sends welcome-to-premium email
      ├─ Updates user.subscription_tier
      │    = PREMIUM in PostgreSQL
      └─ Unlocks premium feature flags

─────────────────────────────────────────────
Stripe webhook fires on each monthly renewal
─────────────────────────────────────────────

      │  POST /v1/webhooks/stripe
      │  Stripe-Signature: <signature>
      ▼
Payment Service — Webhook Handler
      │
      ├─ 1. Validates Stripe webhook signature
      │        (prevents spoofed webhook calls)
      │
      ├─ 2. Handles event type:
      │
      │  invoice.payment_succeeded →
      │        Records renewal in payment_ledger
      │        Extends subscription.next_billing_at
      │
      │  invoice.payment_failed →
      │        Updates subscription status: PAST_DUE
      │        Publishes payment.failed event
      │        Notification Service sends dunning email
      │
      └─ 3. Returns 200 to Stripe immediately
               (Stripe retries if it receives non-200)
```

**Key design points:**
- Stripe webhook signature validation is mandatory — without it, anyone could POST a fake `invoice.payment_succeeded` to upgrade accounts for free
- Returning 200 to Stripe immediately before processing — Stripe has a short timeout on webhook responses. Heavy processing happens after the 200 is sent, or in a BullMQ job enqueued by the webhook handler
- Subscription state in PostgreSQL is the source of truth — not Stripe. Stripe is the payment processor; FinVerse's DB is what the application reads for feature access decisions

---

## Flow 7 — Portfolio Valuation & Market Data Refresh

**What happens:** During market hours, ETF prices update and all user portfolio valuations refresh.

```
Market Data Service (Go)
      │
      │  Scheduled goroutine — fires every 15 minutes during market hours
      │  (Market hours vary by exchange: Xetra 09:00–17:30 CET,
      │   Euronext 09:00–17:30 CET, Borsa Italiana 09:00–17:30 CET)
      ▼
Price Refresh Worker (Go goroutines)
      │
      ├─ 1. Loads list of active ETF symbols from internal cache
      │        (symbol catalogue refreshed from PostgreSQL daily)
      │
      ├─ 2. Fans out concurrent HTTP calls to EODHD:
      │        GET https://eodhd.com/api/real-time/{symbol}?api_token=...
      │        One goroutine per symbol — all run concurrently
      │
      ├─ 3. Collects all responses — normalises into internal PriceRecord:
      │        { symbol, price, currency, timestamp, change, changePercent }
      │
      ├─ 4. Writes all price records to Redis:
      │        key: market:price:{symbol}
      │        value: JSON serialised PriceRecord
      │        TTL: 20 minutes (slightly longer than polling interval
      │             — ensures cache never goes stale between polls)
      │
      ├─ 5. Fans out portfolio valuation goroutines:
      │        For each user with an active portfolio:
      │        - Load holdings from PostgreSQL
      │        - Look up current price from Redis (in-process, no network hop)
      │        - Compute: current_value = Σ (units × current_price)
      │        - Compute: return = current_value − total_invested
      │        - Compute: return_percent = (return ÷ total_invested) × 100
      │
      ├─ 6. Writes computed valuations to Redis:
      │        key: portfolio:valuation:{userId}
      │        value: JSON { currentValue, return, returnPercent, updatedAt }
      │        TTL: 20 minutes
      │
      └─ 7. Persists end-of-day snapshot to PostgreSQL
               (once per day at market close — for historical chart data)

─────────────────────────────────────────────
User opens portfolio screen
─────────────────────────────────────────────

React Native App
      │
      │  GET /v1/portfolio
      │  Authorization: Bearer <JWT>
      ▼
API Gateway → Core Product — Investing Module
      │
      ├─ 1. Calls Market Data Service:
      │        GET /internal/portfolio/valuation/{userId}
      │
      ├─ 2. Market Data Service reads from Redis cache:
      │        key: portfolio:valuation:{userId}
      │        Returns cached valuation (sub-millisecond read)
      │
      ├─ 3. Core Product combines:
      │        - Valuation from Market Data Service
      │        - Holdings detail from PostgreSQL
      │        - Contribution history from PostgreSQL
      │
      └─ 4. Returns composed portfolio response to App
```

**Key design points:**
- All valuation computation happens in the Market Data Service Go background worker — not in the request-response path. When a user opens their portfolio, they get a pre-computed cached result. This keeps the API response fast regardless of portfolio size
- End-of-day PostgreSQL snapshot is what powers the historical performance charts in the app — Redis is ephemeral and TTL-based, PostgreSQL is the permanent record
- Go goroutines make the fan-out pattern (concurrent EODHD calls, concurrent portfolio computations) natural and efficient — this would be significantly more complex to implement safely in Node.js

---

## Flow 8 — Tax Report Generation

**What happens:** At year-end, FinVerse generates a tax report for each Premium user covering their investment activity and applicable deductions.

```
BullMQ Scheduler (inside Core Product)
      │
      │  Recurring job fires: 1st January each year
      │  job: ANNUAL_TAX_REPORT_GENERATION
      │  Enqueues one job per Premium user
      ▼
BullMQ Worker — Tax Reports
(runs with concurrency: 5 — processes 5 users in parallel,
 controlled to avoid DB overload)
      │
      ├─ 1. Loads all investment transactions for the tax year
      │        from PostgreSQL (Holdings, Orders, Dividends tables)
      │
      ├─ 2. Loads all relevant bank transactions for the tax year
      │        (for income verification, deduction eligibility)
      │
      ├─ 3. Applies country-specific tax logic:
      │        Germany: Kapitalertragsteuer (25% flat on capital gains)
      │        France: PFU (Prélèvement Forfaitaire Unique — 30%)
      │        Netherlands: Box 3 wealth tax logic
      │        Spain: IRPF savings tax bands
      │        (etc. per user's registered country)
      │
      ├─ 4. Identifies loss harvesting opportunities:
      │        ETF positions with unrealised losses that could be sold
      │        to offset gains — flagged as recommendations
      │
      ├─ 5. Generates PDF report using a template renderer
      │
      ├─ 6. Uploads PDF to AWS S3:
      │        key: tax-reports/{userId}/{year}/report.pdf
      │
      ├─ 7. Stores report metadata in PostgreSQL:
      │        { userId, year, s3Key, generatedAt, country }
      │
      ├─ 8. Publishes event to RabbitMQ:
      │        routing key: tax.report.ready
      │        payload: { userId, year, downloadUrl }
      │
      └─ 9. BullMQ marks job as complete
      │
      ▼
Notification Service
      │
      └─ Consumes tax.report.ready
         Sends email: "Your 2023 tax report is ready"
         with download link
```

**Key design points:**
- BullMQ concurrency is explicitly capped at 5 for this job — generating reports for all Premium users (potentially 50,000+) simultaneously would overwhelm PostgreSQL with read queries. The concurrency setting throttles this to a safe rate
- PDF is stored in S3, not in PostgreSQL — binary blobs do not belong in a relational database. PostgreSQL stores only the metadata and the S3 key
- Country-specific tax logic is isolated within the Tax & Reporting module — adding a new EU country means adding a new country handler to this module, not touching any other part of the system
- The job is idempotent — if it runs twice for the same user and year, the second run overwrites the S3 file and updates the PostgreSQL record. No duplicate reports.

---

That is the complete Step 3. Every major cross-service flow is documented — how data moves, which services are involved, what is synchronous vs asynchronous, where background jobs are used, and why each design decision was made that way.

**Ready for Step 4: Deep Dive — Team Structure & Ownership?**