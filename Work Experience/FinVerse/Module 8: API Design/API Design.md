Perfect. Writing Step 8 now — deep on Accounts & Open Banking, working-level on all other Core Product modules.

---

# FinVerse — Step 8: API Design

## Part 1 — API Design Philosophy & Team Standards

Before going into individual endpoints, understanding *why* the team designs APIs the way it does is critical. An interviewer who asks "walk me through your API design" is really asking "what trade-offs did you think about?"

### The Guiding Constraints

Every API decision in Core Product flows from three realities:

**Reality 1 — The primary client is a mobile app on a cellular connection.**
React Native app on iOS and Android. Users are on 4G, sometimes 3G, occasionally flaky WiFi. Payloads must be lean. Pagination is non-negotiable. Every unnecessary field in a response is wasted bandwidth and battery.

**Reality 2 — This is a regulated fintech.**
Every API that touches financial data — bank accounts, transactions, investments — must be authenticated, authorised, and auditable. There are no public endpoints for sensitive data. Ever.

**Reality 3 — The team is small (6 engineers) and moves fast.**
API conventions must be consistent enough that any engineer can pick up any module's endpoints and understand them immediately. No surprises, no special snowflake patterns.

---

### Team API Standards — The Conventions Everyone Follows

**1. Versioning — always prefix with `/v1/`**

```
All endpoints: /v1/{resource}/{action}

Examples:
  GET  /v1/accounts
  POST /v1/accounts/connect
  GET  /v1/accounts/:accountId/transactions
  POST /v1/auth/login
  GET  /v1/portfolio
```

Why versioning matters at FinVerse: the mobile app cannot be force-updated. A user on iOS might be on v1.2 of the app while the backend is on v1.5. API versioning means the backend can evolve without breaking older app versions. The API Gateway handles routing `/v1/` traffic to the correct service version.

**2. Consistent response envelope**

Every API response — success or error — follows the same shape. The API Gateway enforces this before the response reaches the client:

```typescript
// Success response
{
  "data": { ... },          // the actual payload
  "meta": {                 // pagination, counts, timestamps
    "page": 1,
    "limit": 20,
    "total": 147,
    "hasMore": true
  }
}

// Error response
{
  "error": {
    "code": "ACCOUNT_NOT_FOUND",    // machine-readable, stable
    "message": "Bank account not found or access denied",
    "statusCode": 404,
    "traceId": "abc-123-def"        // links to Datadog trace
  }
}
```

The `traceId` in errors is injected by the API Gateway — it is the same `traceId` that flows through every service in the distributed trace. When a user reports an error to support, the support team looks up the `traceId` in Datadog and sees the entire request journey in one trace.

**3. HTTP status codes — used precisely, not loosely**

```
200 OK          — GET successful, data returned
201 Created     — POST successful, resource created
202 Accepted    — POST accepted, async work enqueued (e.g. sync)
400 Bad Request — validation failure (missing field, wrong format)
401 Unauthorised— no JWT or JWT expired
403 Forbidden   — valid JWT but insufficient permission
404 Not Found   — resource doesn't exist OR user has no access
                  (deliberately ambiguous for security)
409 Conflict    — duplicate resource (e.g. account already connected)
422 Unprocessable Entity — request is valid but business rule blocks it
429 Too Many Requests    — rate limit exceeded
500 Internal Server Error — something we didn't anticipate
503 Service Unavailable  — downstream dependency (GoCardless) is down
```

**4. Authentication — JWT on every private endpoint**

The API Gateway validates the JWT before the request ever reaches Core Product. By the time a request arrives at Core Product's controllers, it carries a decoded `userId` injected by the gateway. Core Product never re-validates the JWT itself — it trusts the gateway's `x-user-id` header.

```typescript
// NestJS decorator — extracts userId injected by API Gateway
@Get()
async getAccounts(@UserId() userId: string): Promise<AccountListResponse> {
  return this.accountService.getAccountsForUser(userId)
}
```

**5. Pagination — cursor-based for transaction lists, offset for short lists**

For large, time-ordered datasets like transactions, the team uses **cursor-based pagination**. For short lists like bank accounts (typically 2–5 per user), **offset pagination** is sufficient.

```
WHY CURSOR-BASED FOR TRANSACTIONS

Offset-based problem:
  Page 1: GET /transactions?page=1&limit=20
  → returns transactions 1-20 (newest first)

  Between page 1 and page 2, 3 new transactions arrive.
  
  Page 2: GET /transactions?page=2&limit=20
  → returns transactions 21-40
  BUT the new transactions shifted everything — transaction 20
  from page 1 now appears again as transaction 23 on page 2.
  DUPLICATE RESULTS. User sees the same transaction twice.

Cursor-based solution:
  Page 1: GET /transactions?limit=20
  Response includes: "cursor": "txn_2024-01-15T14:23:00_abc123"

  Page 2: GET /transactions?limit=20&cursor=txn_2024-01-15T14:23:00_abc123
  → returns transactions that come AFTER this cursor
  No matter how many new transactions arrived, this cursor
  points to a stable position in the dataset.
  No duplicates. No missed records.
```

**6. No sensitive data in URLs**

Bank account IDs, user IDs, and transaction IDs in path parameters are UUIDs — opaque identifiers with no meaning. Nothing sensitive (IBAN, card numbers, amounts) ever appears in a URL. URLs are logged by proxies, load balancers, and sometimes browsers — they are not private.

---

## Part 2 — The Role of Accounts & Open Banking Module

Before the endpoints, understanding *why this module exists* at the system level is what an interviewer expects you to explain first.

---

### "What is the role of the Accounts & Open Banking module in the whole system?"

This is the question you must answer confidently and completely.

**The short answer:**

The Accounts & Open Banking module is the data ingestion layer of FinVerse. Every piece of financial data that flows through the platform — transactions, balances, spending history — originates here. Without this module, FinVerse has no data to budget with, no data to track savings against, and no baseline for investment recommendations. It is the foundation that every other module in Core Product builds on top of.

**The longer answer — the full role:**

```
┌──────────────────────────────────────────────────────────────────┐
│           ACCOUNTS & OPEN BANKING MODULE — FULL ROLE             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. BANK CONNECTION MANAGEMENT                                   │
│     Handles the GoCardless PSD2 consent flow — the               │
│     mechanism by which a user authorises FinVerse to read        │
│     their bank data. Creates and manages BankConnection          │
│     and BankAccount records.                                     │
│                                                                  │
│  2. TRANSACTION SYNC                                             │
│     Periodically fetches new transactions from every             │
│     connected bank account via the GoCardless API.               │
│     Handles deduplication, error recovery, and consent           │
│     expiry. Enqueues BullMQ sync jobs — does not do the          │
│     actual fetching in the request-response cycle.               │
│                                                                  │
│  3. DATA FEED FOR DOWNSTREAM MODULES                             │
│     Everything downstream depends on what this module            │
│     produces:                                                    │
│     → Transactions module reads the accounts this module         │
│       creates                                                    │
│     → Budgeting module aggregates transaction data that          │
│       this module ingested                                       │
│     → Goals module tracks contributions against balances         │
│       this module maintains                                      │
│     → Retirement module uses income data from transactions       │
│       this module fetched                                        │
│     → Tax module uses transaction history this module            │
│       accumulated over the year                                  │
│                                                                  │
│  4. NET WORTH DASHBOARD INPUT                                    │
│     The balance of each connected account (current and           │
│     available) feeds into the user's net worth dashboard.        │
│     This module keeps balances current.                          │
│                                                                  │
│  5. CONSENT LIFECYCLE MANAGEMENT                                 │
│     GoCardless PSD2 consent expires every 90 days.               │
│     This module tracks expiry, sends proactive re-consent        │
│     reminders via RabbitMQ events, and gracefully handles        │
│     expired connections without corrupting existing data.        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**How it fits into the bigger system diagram:**

```
EXTERNAL WORLD
      │
      │  GoCardless Bank Account Data API
      │  (PSD2-licensed, 2,300+ EU banks)
      ▼
┌─────────────────────────────────────────────────────────────────┐
│              ACCOUNTS & OPEN BANKING MODULE                     │
│                                                                 │
│  BankConnection records (one per bank the user linked)          │
│  BankAccount records   (one per account within each bank)       │
│  SyncLog records       (audit trail of every sync operation)    │
│                                                                 │
│  Produces:                                                      │
│  - New Transaction records → picked up by Transactions module   │
│  - Updated balance figures → used by Net Worth dashboard        │
│  - Sync status events → Notification Service via RabbitMQ       │
└────────────────────────────┬────────────────────────────────────┘
                             │
          ┌──────────────────┼───────────────────┐
          ▼                  ▼                   ▼
  Transactions &       Budgeting           Goals & Savings
  Categorisation       module              module
  module
          │
          ▼
  Tax & Reporting
  module
```

**In one sentence for an interview:**

*"The Accounts & Open Banking module is the data ingestion foundation of the platform — it manages the bank connection lifecycle via GoCardless PSD2 and continuously syncs transaction data that every other module in Core Product depends on."*

---

## Part 3 — Accounts & Open Banking API — Deep Dive

### The Complete Endpoint Map

```
ACCOUNTS & OPEN BANKING — ALL ENDPOINTS

Bank Connection Flow:
  POST   /v1/accounts/connect              — initiate bank connection
  GET    /v1/accounts/connect/callback     — GoCardless OAuth callback
  DELETE /v1/accounts/connections/:id      — disconnect a bank

Account Management:
  GET    /v1/accounts                      — list all connected accounts
  GET    /v1/accounts/:accountId           — single account detail
  POST   /v1/accounts/:accountId/sync      — manually trigger a sync
  GET    /v1/accounts/:accountId/sync-status — check sync progress

Net Worth:
  GET    /v1/accounts/net-worth            — aggregated balance summary
```

---

### Endpoint 1 — POST /v1/accounts/connect

**Purpose:** Start the GoCardless bank connection flow. The user has selected their bank in the app — this endpoint creates a GoCardless requisition and returns the consent URL the app needs to open.

**Request:**

```typescript
// POST /v1/accounts/connect
// Headers: Authorization: Bearer <JWT>
// Body:
{
  "institutionId": "MONZO_MONZGB2L",   // GoCardless institution ID
  "countryCode": "GB"                   // ISO 3166-1 alpha-2
}
```

**Validation (NestJS DTO with class-validator):**

```typescript
// connect-bank.dto.ts
export class ConnectBankDto {
  @IsString()
  @IsNotEmpty()
  @Length(3, 100)
  institutionId: string

  @IsISO31661Alpha2()   // validates country code format
  countryCode: string
}
```

**Controller:**

```typescript
// accounts.controller.ts
@Post('connect')
@UseGuards(JwtAuthGuard)          // JWT enforced
@HttpCode(HttpStatus.CREATED)
async initiateConnection(
  @UserId() userId: string,
  @Body() dto: ConnectBankDto
): Promise<ConnectBankResponse> {
  return this.accountService.initiateConnection(userId, dto)
}
```

**Service logic — what happens step by step:**

```typescript
// account.service.ts
async initiateConnection(
  userId: string,
  dto: ConnectBankDto
): Promise<ConnectBankResponse> {

  // Step 1: Check if user already has an active connection
  // to this institution — prevent duplicate connections
  const existingConnection = await this.prisma.bankConnection.findFirst({
    where: {
      userId,
      institutionId: dto.institutionId,
      status: { in: ['ACTIVE', 'PENDING'] }
    }
  })

  if (existingConnection) {
    throw new ConflictException(
      'INSTITUTION_ALREADY_CONNECTED',
      `An active connection to this bank already exists`
    )
  }

  // Step 2: Call GoCardless to create a requisition
  // A requisition is GoCardless's term for the consent initiation object
  const requisition = await this.goCardlessService.createRequisition({
    institutionId: dto.institutionId,
    redirectUri: `${process.env.API_BASE_URL}/v1/accounts/connect/callback`,
    reference: userId,   // passed back in callback — used to identify user
    userLanguage: 'EN'
  })
  // Returns: { id: "req_abc123", link: "https://ob.gocardless.com/..." }

  // Step 3: Store the requisition in our database
  // status: PENDING because user hasn't completed consent yet
  await this.prisma.bankConnection.create({
    data: {
      userId,
      institutionId:  dto.institutionId,
      institutionName: await this.getInstitutionName(dto.institutionId),
      requisitionId:  requisition.id,
      status:         'PENDING',
      consentExpiresAt: null,   // set after consent is granted
    }
  })

  // Step 4: Return the consent URL to the app
  // App opens this URL in a WebView — user completes bank login there
  return {
    consentUrl: requisition.link,
    requisitionId: requisition.id
  }
}
```

**Response:**

```typescript
// 201 Created
{
  "data": {
    "consentUrl": "https://ob.gocardless.com/psd2/start/req_abc123/MONZO_MONZGB2L",
    "requisitionId": "req_abc123"
  }
}
```

**What happens next (outside this endpoint):**
The app opens the `consentUrl` in a WebView. The user logs into their bank on GoCardless's hosted page. GoCardless redirects back to our callback URL when the user completes or cancels.

**Edge cases handled:**
- Institution already connected → 409 Conflict
- GoCardless API unavailable → 503, error logged with traceId
- Invalid institution ID → GoCardless returns 400, we translate to 422

---

### Endpoint 2 — GET /v1/accounts/connect/callback

**Purpose:** GoCardless redirects the user back here after they complete (or cancel) the consent flow. This is not called by the app directly — GoCardless calls it via redirect. The app monitors the connection status separately.

**Why this is a GET and not a POST:**
OAuth redirects are GET requests by definition. The redirect carries query parameters, not a body. This is a standard OAuth 2.0 callback pattern.

**Request:**

```
GET /v1/accounts/connect/callback?ref={userId}&error={optional}
```

GoCardless appends the `ref` parameter we sent during requisition creation (which was the `userId`) plus an optional `error` parameter if the user cancelled or something went wrong.

**Controller & Service:**

```typescript
// accounts.controller.ts
@Get('connect/callback')
// No JWT guard — this is called by GoCardless's redirect, not by the app
// Security is via the signed requisitionId matched to our database
async handleCallback(
  @Query('ref') userId: string,
  @Query('error') error?: string,
): Promise<void> {
  if (error) {
    // User cancelled or bank rejected — update status to DISCONNECTED
    await this.accountService.handleCallbackCancellation(userId, error)
    // Redirect to mobile deep link so app shows appropriate message
    // e.g. finverse://connect/cancelled
    return
  }

  await this.accountService.handleCallbackSuccess(userId)
  // Redirect to: finverse://connect/success
}

// account.service.ts
async handleCallbackSuccess(userId: string): Promise<void> {

  // Step 1: Find the PENDING connection for this user
  const pendingConnection = await this.prisma.bankConnection.findFirst({
    where: { userId, status: 'PENDING' },
    orderBy: { createdAt: 'desc' }
  })

  if (!pendingConnection) {
    // Should not happen — log and return, do not throw
    this.logger.error(`Callback received but no PENDING connection found`,
      { userId })
    return
  }

  // Step 2: Confirm requisition with GoCardless
  // This fetches the list of account IDs the user authorised access to
  const requisitionDetails = await this.goCardlessService
    .getRequisition(pendingConnection.requisitionId)
  // Returns: { status: 'LN', accounts: ['acc_id_1', 'acc_id_2'] }

  if (requisitionDetails.status !== 'LN') {
    // 'LN' means "Linked" — consent granted
    // Any other status means something went wrong
    await this.prisma.bankConnection.update({
      where: { id: pendingConnection.id },
      data: { status: 'ERROR', lastRequisitionError: requisitionDetails.status }
    })
    return
  }

  // Step 3: For each account GoCardless returned,
  // fetch metadata and create our BankAccount records
  const accountIds = requisitionDetails.accounts

  await this.prisma.$transaction(async (tx) => {

    // Update the connection to ACTIVE
    await tx.bankConnection.update({
      where: { id: pendingConnection.id },
      data: {
        status: 'ACTIVE',
        consentExpiresAt: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000)
        // GoCardless consent is valid for 90 days
      }
    })

    // Create a BankAccount record for each authorised account
    for (const accountId of accountIds) {
      const accountDetail = await this.goCardlessService
        .getAccountDetails(accountId)

      await tx.bankAccount.upsert({
        where: { externalAccountId: accountId },
        update: { isActive: true },
        create: {
          userId,
          bankConnectionId:  pendingConnection.id,
          externalAccountId: accountId,
          iban:              accountDetail.iban,
          accountName:       accountDetail.name,
          institutionName:   pendingConnection.institutionName,
          accountType:       this.mapAccountType(accountDetail.type),
          currency:          accountDetail.currency,
          currentBalance:    new Decimal(accountDetail.balances.current),
          availableBalance:  new Decimal(accountDetail.balances.available),
          syncStatus:        'IDLE',
        }
      })
    }
  })

  // Step 4: Enqueue initial sync BullMQ job
  // DO NOT run sync inline — this callback must return quickly
  await this.syncQueue.add(
    'INITIAL_SYNC',
    { userId, accountIds },
    {
      jobId: `initial-sync-${userId}`,
      priority: 1,
      attempts: 3,
      backoff: { type: 'exponential', delay: 5000 }
    }
  )

  // Step 5: Publish event so app knows connection is complete
  // Notification Service consumes this → pushes notification to app
  await this.prisma.outboxEvent.create({
    data: {
      eventType: 'bank.connection.completed',
      payload: {
        userId,
        accountCount: accountIds.length,
        institutionName: pendingConnection.institutionName
      }
    }
  })
}
```

**Why the `$transaction` wrapping the BankConnection update and BankAccount creation:**
Both must succeed or neither should. If we update the connection to ACTIVE but then fail to create the account records (GoCardless error on detail fetch), we end up with an ACTIVE connection that has no accounts — a broken state. The transaction rolls back everything cleanly.

**Why the sync is enqueued rather than run inside the callback:**
The callback endpoint must return quickly so GoCardless's redirect completes. If sync ran inline — potentially 15–30 seconds — GoCardless's redirect would time out and the user would see an error screen on the bank's page. The sync running as a BullMQ job means the callback returns in milliseconds and the user sees a success screen immediately.

---

### Endpoint 3 — GET /v1/accounts

**Purpose:** List all connected bank accounts for the authenticated user. The app displays this on the accounts overview screen.

**Mobile-first design considerations:**
- Response must be lean — just what the UI needs to render the list
- Balance figures must be formatted correctly for the user's currency
- Sync status must be included so the UI can show sync state indicators
- No pagination needed here — a user realistically has 2–8 accounts

**Controller:**

```typescript
@Get()
@UseGuards(JwtAuthGuard)
async getAccounts(
  @UserId() userId: string
): Promise<AccountListResponse> {
  return this.accountService.getAccountsForUser(userId)
}
```

**Service:**

```typescript
async getAccountsForUser(userId: string): Promise<AccountListResponse> {
  const accounts = await this.prisma.bankAccount.findMany({
    where: {
      userId,
      isActive: true      // exclude soft-deleted accounts
    },
    select: {
      id:                true,
      accountName:       true,
      institutionName:   true,
      accountType:       true,
      currency:          true,
      currentBalance:    true,
      availableBalance:  true,
      syncStatus:        true,
      lastSyncedAt:      true,
      bankConnection: {
        select: {
          id:               true,
          status:           true,
          consentExpiresAt: true,
          institutionId:    true,
        }
      }
    },
    orderBy: { createdAt: 'asc' }   // stable order — same order every time
  })

  return {
    accounts: accounts.map(account => this.mapToDto(account)),
    totalBalance: this.computeTotalBalance(accounts),
  }
}

private mapToDto(account: BankAccountWithConnection): AccountDto {
  return {
    id:              account.id,
    name:            account.accountName,
    institution:     account.institutionName,
    type:            account.accountType,
    currency:        account.currency,
    balance: {
      current:   account.currentBalance.toNumber(),
      available: account.availableBalance?.toNumber() ?? null,
    },
    sync: {
      status:      account.syncStatus,
      lastSyncedAt: account.lastSyncedAt?.toISOString() ?? null,
    },
    connection: {
      id:                 account.bankConnection.id,
      status:             account.bankConnection.status,
      consentExpiresAt:   account.bankConnection.consentExpiresAt?.toISOString() ?? null,
      needsReconnection:  this.isConsentExpiringSoon(
                            account.bankConnection.consentExpiresAt
                          ),
    }
  }
}

private isConsentExpiringSoon(expiresAt: Date | null): boolean {
  if (!expiresAt) return false
  const daysUntilExpiry =
    (expiresAt.getTime() - Date.now()) / (1000 * 60 * 60 * 24)
  return daysUntilExpiry <= 7   // warn 7 days before expiry
}
```

**Response:**

```typescript
// 200 OK
{
  "data": {
    "accounts": [
      {
        "id": "acc_uuid_1",
        "name": "Main Current Account",
        "institution": "Monzo",
        "type": "CHECKING",
        "currency": "GBP",
        "balance": {
          "current": 1842.50,
          "available": 1800.00
        },
        "sync": {
          "status": "SUCCESS",
          "lastSyncedAt": "2024-01-15T10:30:00Z"
        },
        "connection": {
          "id": "conn_uuid_1",
          "status": "ACTIVE",
          "consentExpiresAt": "2024-04-15T00:00:00Z",
          "needsReconnection": false
        }
      }
    ],
    "totalBalance": {
      "EUR": 2150.00,
      "GBP": 1842.50
    }
  }
}
```

**Why `select` instead of fetching all fields:**
The `bankAccount` table has 20+ fields including `lastSyncError`, `externalAccountId` (internal), `syncRetryCount`, and other operational fields the UI never needs. Selecting only what the mobile app renders keeps the payload minimal and prevents accidentally leaking internal fields.

**Why `needsReconnection` is computed server-side:**
The 7-day consent expiry warning is business logic. If it lives in the app, every app version needs updating when we change the threshold. Server-side computation means we change it once, all app versions benefit immediately.

**Why `totalBalance` groups by currency:**
A user might have accounts in EUR (German bank) and GBP (UK bank). Summing across currencies would be meaningless — €1000 + £1000 is not €2000. The response groups by currency so the UI can display multi-currency balances correctly.

---

### Endpoint 4 — GET /v1/accounts/:accountId

**Purpose:** Fetch full detail for a single account. Called when the user taps on an account from the list screen.

**Security — ownership check:**
A user can only access their own accounts. The account UUID in the URL means nothing without this check — an attacker who guesses another user's account UUID must not be able to read it.

```typescript
@Get(':accountId')
@UseGuards(JwtAuthGuard)
async getAccount(
  @UserId() userId: string,
  @Param('accountId', ParseUUIDPipe) accountId: string
): Promise<AccountDetailResponse> {
  return this.accountService.getAccountById(userId, accountId)
}

// account.service.ts
async getAccountById(
  userId: string,
  accountId: string
): Promise<AccountDetailResponse> {

  const account = await this.prisma.bankAccount.findFirst({
    where: {
      id: accountId,
      userId,           // ownership check built into the query
      isActive: true    // always filter deleted accounts
    },
    include: {
      bankConnection: true,
    }
  })

  // findFirst with userId means:
  // - if accountId doesn't exist → null → 404
  // - if accountId exists but belongs to another user → null → 404
  // Both cases return the same 404 — no information leakage
  if (!account) {
    throw new NotFoundException(
      'ACCOUNT_NOT_FOUND',
      'Bank account not found or access denied'
    )
  }

  return this.mapToDetailDto(account)
}
```

**Why the error message says "not found OR access denied":**
If the error said "access denied" for accounts owned by other users, an attacker could enumerate valid account IDs. By always returning 404 with the same message, the API reveals nothing about whether the ID exists.

This is a standard security pattern — it appears in the NestJS docs and Lucas introduced it in the team's code review guidelines in month 3.

---

### Endpoint 5 — POST /v1/accounts/:accountId/sync

**Purpose:** Allow a user to manually trigger a sync for a specific account. Called when the user pulls to refresh on the transactions screen or taps "Sync now."

**Why this returns 202 instead of 200:**

```typescript
@Post(':accountId/sync')
@UseGuards(JwtAuthGuard)
@HttpCode(HttpStatus.ACCEPTED)   // 202 — not 200, not 201
async triggerSync(
  @UserId() userId: string,
  @Param('accountId', ParseUUIDPipe) accountId: string
): Promise<TriggerSyncResponse> {
  return this.accountService.triggerManualSync(userId, accountId)
}

async triggerManualSync(
  userId: string,
  accountId: string
): Promise<TriggerSyncResponse> {

  // Verify ownership
  const account = await this.findAccountOrThrow(userId, accountId)

  // Verify connection is in a syncable state
  if (account.bankConnection.status !== 'ACTIVE') {
    throw new UnprocessableEntityException(
      'CONNECTION_NOT_ACTIVE',
      `Bank connection is ${account.bankConnection.status}. 
       Please reconnect your bank.`
    )
  }

  // Check if a sync is already in progress for this account
  // Prevents queuing duplicate syncs when user taps rapidly
  if (account.syncStatus === 'SYNCING') {
    return {
      message: 'Sync already in progress',
      syncStatus: 'SYNCING'
    }
  }

  // Update sync status to SYNCING immediately
  // So the UI reflects the state change without waiting for the worker
  await this.prisma.bankAccount.update({
    where: { id: accountId },
    data: { syncStatus: 'SYNCING' }
  })

  // Enqueue BullMQ job — high priority (user-initiated)
  await this.syncQueue.add(
    'PERIODIC_SYNC',
    { userId, accountIds: [accountId] },
    {
      jobId: `manual-sync-${accountId}-${Date.now()}`,
      priority: 1,    // user is waiting — high priority
      attempts: 3,
      backoff: { type: 'exponential', delay: 5000 }
    }
  )

  return {
    message: 'Sync started',
    syncStatus: 'SYNCING'
  }
}
```

**Why 202 and not 200:**
202 Accepted means "we have accepted your request and will process it, but it is not done yet." This is semantically correct — the sync has not happened, it has been queued. If the endpoint returned 200, the app might assume the data is already fresh and not poll for updated status.

**Why the syncStatus update happens synchronously before the queue:**
The app needs immediate feedback that something is happening. If we only update the status in the BullMQ worker (which runs in a separate container), there could be a 1–5 second delay between the user tapping "Sync now" and the UI showing "Syncing." Updating the DB synchronously in the endpoint means the very next call to `GET /v1/accounts` shows `syncStatus: SYNCING` immediately.

---

### Endpoint 6 — GET /v1/accounts/:accountId/sync-status

**Purpose:** Let the app poll for the current sync state. Called after a manual sync is triggered — the app polls this every 3 seconds until status is `SUCCESS` or `FAILED`.

**Why polling instead of WebSockets:**
WebSockets are stateful connections — they require the client to maintain a persistent connection to a specific server instance. With horizontal scaling (multiple Core Product containers), a user's WebSocket might be on Container 1 while the sync job runs on a BullMQ worker that updates the database. The sync status update would need to propagate across containers.

Polling is simpler, stateless, and works perfectly for a use case where the status changes once over 10–30 seconds. The overhead of 3-second polling is negligible.

```typescript
@Get(':accountId/sync-status')
@UseGuards(JwtAuthGuard)
async getSyncStatus(
  @UserId() userId: string,
  @Param('accountId', ParseUUIDPipe) accountId: string
): Promise<SyncStatusResponse> {

  const account = await this.findAccountOrThrow(userId, accountId)

  // Get the most recent sync log for this account
  const lastSync = await this.prisma.syncLog.findFirst({
    where: { bankAccountId: accountId },
    orderBy: { startedAt: 'desc' }
  })

  return {
    accountId:     account.id,
    syncStatus:    account.syncStatus,
    lastSyncedAt:  account.lastSyncedAt?.toISOString() ?? null,
    lastSync: lastSync ? {
      type:                 lastSync.syncType,
      status:               lastSync.status,
      transactionsFetched:  lastSync.transactionsFetched,
      transactionsInserted: lastSync.transactionsInserted,
      errorMessage:         lastSync.errorMessage,
      startedAt:            lastSync.startedAt.toISOString(),
      completedAt:          lastSync.completedAt?.toISOString() ?? null,
    } : null
  }
}
```

**Response — while syncing:**

```typescript
// 200 OK (during sync)
{
  "data": {
    "accountId": "acc_uuid_1",
    "syncStatus": "SYNCING",
    "lastSyncedAt": "2024-01-15T06:30:00Z",
    "lastSync": {
      "type": "MANUAL_SYNC",
      "status": "SYNCING",
      "transactionsFetched": 0,
      "transactionsInserted": 0,
      "errorMessage": null,
      "startedAt": "2024-01-15T14:22:00Z",
      "completedAt": null
    }
  }
}
```

**Response — after sync completes:**

```typescript
// 200 OK (sync finished)
{
  "data": {
    "accountId": "acc_uuid_1",
    "syncStatus": "SUCCESS",
    "lastSyncedAt": "2024-01-15T14:22:47Z",
    "lastSync": {
      "type": "MANUAL_SYNC",
      "status": "SUCCESS",
      "transactionsFetched": 23,
      "transactionsInserted": 18,
      "errorMessage": null,
      "startedAt": "2024-01-15T14:22:00Z",
      "completedAt": "2024-01-15T14:22:47Z"
    }
  }
}
```

**Why include `transactionsFetched` vs `transactionsInserted`:**
The difference tells the app — and the user — something meaningful. If 23 were fetched but only 18 inserted, 5 were duplicates (already in the database). This is normal. If 23 were fetched but 0 inserted, the user might worry their transactions aren't being saved. The sync log makes this transparent.

---

### Endpoint 7 — GET /v1/accounts/net-worth

**Purpose:** Return an aggregated summary of the user's total cash position across all connected accounts. Used by the net worth dashboard on the home screen.

```typescript
@Get('net-worth')
@UseGuards(JwtAuthGuard)
async getNetWorth(
  @UserId() userId: string
): Promise<NetWorthResponse> {
  return this.accountService.getNetWorth(userId)
}

async getNetWorth(userId: string): Promise<NetWorthResponse> {

  const accounts = await this.prisma.bankAccount.findMany({
    where: { userId, isActive: true },
    select: {
      currency:        true,
      currentBalance:  true,
      accountType:     true,
    }
  })

  // Group balances by currency
  const balancesByCurrency = accounts.reduce((acc, account) => {
    const currency = account.currency
    if (!acc[currency]) acc[currency] = new Decimal(0)

    // Credit cards reduce net worth — their balance is a liability
    if (account.accountType === 'CREDIT_CARD') {
      acc[currency] = acc[currency].minus(account.currentBalance)
    } else {
      acc[currency] = acc[currency].plus(account.currentBalance)
    }
    return acc
  }, {} as Record<string, Decimal>)

  return {
    balancesByCurrency: Object.entries(balancesByCurrency).map(
      ([currency, amount]) => ({
        currency,
        amount: amount.toNumber()
      })
    ),
    accountCount: accounts.length,
    asOf: new Date().toISOString()
  }
}
```

**Why credit card balances are subtracted:**
A credit card balance represents money owed — it is a liability, not an asset. If a user has €2,000 in their current account and €500 on their credit card, their net cash position is €1,500. Treating the credit card balance as an asset would overstate their financial position.

This seems like a small detail but it's exactly the kind of thing interviewers probe — "how did you handle credit cards in the net worth calculation?"

---

### Endpoint 8 — DELETE /v1/accounts/connections/:connectionId

**Purpose:** Allow a user to disconnect a bank from FinVerse. This does not delete historical data — it stops future syncing.

```typescript
@Delete('connections/:connectionId')
@UseGuards(JwtAuthGuard)
@HttpCode(HttpStatus.NO_CONTENT)   // 204 — no body on successful delete
async disconnectBank(
  @UserId() userId: string,
  @Param('connectionId', ParseUUIDPipe) connectionId: string
): Promise<void> {
  await this.accountService.disconnectBank(userId, connectionId)
}

async disconnectBank(
  userId: string,
  connectionId: string
): Promise<void> {

  const connection = await this.prisma.bankConnection.findFirst({
    where: { id: connectionId, userId },
    include: { bankAccounts: true }
  })

  if (!connection) {
    throw new NotFoundException(
      'CONNECTION_NOT_FOUND',
      'Bank connection not found or access denied'
    )
  }

  await this.prisma.$transaction(async (tx) => {

    // Soft-delete the connection — never hard-delete financial records
    await tx.bankConnection.update({
      where: { id: connectionId },
      data: { status: 'DISCONNECTED' }
    })

    // Soft-delete all accounts under this connection
    await tx.bankAccount.updateMany({
      where: { bankConnectionId: connectionId },
      data: { isActive: false }
    })
  })

  // Revoke GoCardless access tokens
  // Best-effort — if this fails, we still consider the connection
  // disconnected from FinVerse's perspective
  try {
    await this.goCardlessService.deleteRequisition(connection.requisitionId)
  } catch (err) {
    this.logger.warn(
      `GoCardless requisition deletion failed for ${connection.requisitionId}`,
      { connectionId, error: err.message }
    )
  }

  // Notify downstream modules via outbox event
  // Budgets, goals, reports etc. may want to flag that
  // this connection's data is now static (no new syncs)
  await this.prisma.outboxEvent.create({
    data: {
      eventType: 'bank.connection.disconnected',
      payload: {
        userId,
        connectionId,
        accountIds: connection.bankAccounts.map(a => a.id)
      }
    }
  })
}
```

**Why soft-delete and not hard-delete:**
GDPR gives users the right to request deletion of their data — but that is a separate, deliberate operation handled by a dedicated account-deletion flow. A "disconnect bank" action is not a data deletion request. The user is simply stopping future syncs. Their transaction history is still valuable to them — it feeds their budgets, their tax report, their spending trends. Soft-delete preserves the data while cleanly excluding the connection and accounts from future operations.

**Why the GoCardless revocation is wrapped in try-catch:**
If GoCardless is temporarily unavailable, the user still expects the disconnect to succeed from FinVerse's perspective. The access token will eventually expire anyway (90-day consent window). A GoCardless API failure should not make FinVerse's disconnect operation fail — that would be confusing and frustrating for the user.

---

## Part 4 — API Documentation

### How the Team Documents APIs

FinVerse uses **Swagger/OpenAPI** generated automatically from NestJS decorators. Every endpoint in Core Product is annotated — and this annotation serves two purposes: it generates the Swagger UI for internal use, and it enforces documentation as a code review requirement (Lucas flags any PR that adds an endpoint without Swagger decorators).

**Example — full annotation for the connect endpoint:**

```typescript
@ApiTags('Accounts')
@ApiBearerAuth()
@Controller('v1/accounts')
export class AccountsController {

  @Post('connect')
  @UseGuards(JwtAuthGuard)
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({
    summary: 'Initiate bank account connection',
    description: `
      Starts the GoCardless PSD2 consent flow for a specific bank.
      Returns a consent URL that the mobile app must open in a WebView.
      The user completes bank login on GoCardless's hosted page.
      FinVerse receives a callback when consent is granted or cancelled.
    `
  })
  @ApiBody({ type: ConnectBankDto })
  @ApiCreatedResponse({
    description: 'Requisition created — consent URL returned',
    type: ConnectBankResponse
  })
  @ApiConflictResponse({
    description: 'Active connection to this institution already exists'
  })
  @ApiServiceUnavailableResponse({
    description: 'GoCardless API unavailable'
  })
  async initiateConnection(
    @UserId() userId: string,
    @Body() dto: ConnectBankDto
  ): Promise<ConnectBankResponse> {
    return this.accountService.initiateConnection(userId, dto)
  }
}
```

**Response type DTOs are also annotated:**

```typescript
export class ConnectBankResponse {
  @ApiProperty({
    description: 'GoCardless consent URL — open in WebView',
    example: 'https://ob.gocardless.com/psd2/start/req_abc123/MONZO_MONZGB2L'
  })
  consentUrl: string

  @ApiProperty({
    description: 'GoCardless requisition ID — used to track consent status',
    example: 'req_abc123'
  })
  requisitionId: string
}
```

The Swagger UI is available at `/api/docs` — accessible only within the VPC, not publicly exposed. When the mobile team (React Native engineers) need to understand an endpoint, they check the Swagger UI first. If anything is unclear, the endpoint owner (the module engineer who built it) is the first point of contact.

---

### PR Checklist for New API Endpoints

Lucas enforces this checklist in code review for every PR that adds or changes an endpoint:

```
API ENDPOINT PR CHECKLIST

□ DTO validation on all request fields (class-validator decorators)
□ Ownership check — user can only access their own data
□ Correct HTTP status code (especially 202 vs 200 vs 201)
□ Consistent response envelope ({ data: ..., meta: ... })
□ Error messages use machine-readable codes (ACCOUNT_NOT_FOUND)
□ No sensitive data in URLs or logs
□ Swagger annotation complete (operation, body, responses)
□ Pagination implemented for list endpoints that could return >10 items
□ select{} used instead of include{} for mobile-facing endpoints
□ Unit tests for service logic covering happy path and key error cases
```

The checklist is not written down anywhere formal — it emerged from Lucas's reviews over time. By month 4, every engineer on the team had internalised it.

---

## Part 5 — High-Level Role of Other Core Product Modules

You need to explain what each module does at a working level — enough to answer "what does the Tax module do?" without going deep.

```
┌─────────────────────────────────────────────────────────────────┐
│           CORE PRODUCT MODULES — WORKING-LEVEL OVERVIEW         │
├─────────────────────────┬───────────────────────────────────────┤
│  MODULE                 │  ROLE                                 │
├─────────────────────────┼───────────────────────────────────────┤
│  Users & Auth           │  User registration, login, JWT        │
│  (Lucas)                │  issuance, KYC status, subscription   │
│                         │  tier. The identity foundation —      │
│                         │  every other module reads userId      │
│                         │  from here.                           │
├─────────────────────────┼───────────────────────────────────────┤
│  Accounts & Open Banking│  Bank connection lifecycle via        │
│  (You)                  │  GoCardless PSD2. Transaction sync.   │
│                         │  Balance management. Data foundation  │
│                         │  for all downstream modules.          │
├─────────────────────────┼───────────────────────────────────────┤
│  Transactions &         │  Receives raw transactions from the   │
│  Categorisation         │  Accounts module. Applies rule-based  │
│  (Tomasz)               │  categorisation (regex matching on    │
│                         │  merchant names). Stores the          │
│                         │  categorised transactions that power  │
│                         │  budgets, spending insights, and      │
│                         │  tax reports.                         │
├─────────────────────────┼───────────────────────────────────────┤
│  Budgeting              │  Monthly budgets per spending         │
│  (Tomasz)               │  category. Tracks how much of each    │
│                         │  budget has been spent as             │
│                         │  transactions come in. Triggers       │
│                         │  budget threshold events when the     │
│                         │  user approaches their limit.         │
├─────────────────────────┼───────────────────────────────────────┤
│  Goals & Savings        │  User-defined savings goals (house    │
│  (Arjun)                │  deposit, emergency fund, holiday).   │
│                         │  Automated transfers to savings       │
│                         │  pockets. Progress tracking toward    │
│                         │  each goal's target amount.           │
├─────────────────────────┼───────────────────────────────────────┤
│  Investing              │  ETF portfolio management. Risk       │
│  (Elena)                │  profile-based portfolio allocation.  │
│                         │  Automated monthly investment orders. │
│                         │  Portfolio performance display using  │
│                         │  valuations from the Go Market Data   │
│                         │  Service.                             │
├─────────────────────────┼───────────────────────────────────────┤
│  Retirement Planning    │  Pension gap calculator. Integrates   │
│  (Elena)                │  with state pension APIs per country. │
│                         │  Private pension recommendations.     │
│                         │  Shows projected retirement income    │
│                         │  versus estimated needs.              │
├─────────────────────────┼───────────────────────────────────────┤
│  Tax & Reporting        │  Annual PDF tax reports per user      │
│  (Isabelle)             │  covering investment gains/losses and │
│                         │  applicable deductions. Country-      │
│                         │  specific logic for 8 EU markets.     │
│                         │  Loss harvesting suggestions.         │
├─────────────────────────┼───────────────────────────────────────┤
│  Education Hub          │  Financial literacy courses and       │
│  (Isabelle + Priya)     │  lessons. Free and premium content.   │
│                         │  Progress tracking per user.          │
│                         │  Goal-based learning paths.           │
└─────────────────────────┴───────────────────────────────────────┘
```

---

## Part 6 — Rate Limiting in the API

Rate limiting was deferred from the Database step to here — and it belongs here because it is an API Gateway and application-layer concern.

### Two Layers of Rate Limiting

**Layer 1 — API Gateway (per endpoint, per user, sliding window)**

The API Gateway enforces rate limits using Redis sliding window counters before requests ever reach Core Product. This is the primary defence against abuse.

```typescript
// API Gateway rate limiting logic (simplified)
// Uses Redis INCR + EXPIRE — sliding window counter

async checkRateLimit(
  userId: string,
  endpoint: string,
  limit: number,
  windowSeconds: number
): Promise<void> {

  const key = `rate_limit:${userId}:${endpoint}:${Math.floor(Date.now() / (windowSeconds * 1000))}`

  const count = await this.redis.incr(key)

  // Set TTL on first request in this window
  if (count === 1) {
    await this.redis.expire(key, windowSeconds)
  }

  if (count > limit) {
    throw new TooManyRequestsException(
      'RATE_LIMIT_EXCEEDED',
      `Too many requests. Limit: ${limit} per ${windowSeconds}s.`
    )
  }
}
```

**Per-endpoint limits relevant to Accounts module:**

```
RATE LIMITS — ACCOUNTS ENDPOINTS

POST /v1/accounts/connect
  Limit: 5 requests per hour per user
  Why: Bank connection is not a high-frequency action. This
       prevents automated account enumeration attacks.

POST /v1/accounts/:accountId/sync
  Limit: 3 requests per 10 minutes per account
  Why: Each manual sync hits GoCardless API. Unlimited manual
       syncs could exhaust the API quota, affecting all users.

GET  /v1/accounts
  Limit: 60 requests per minute per user
  Why: Mobile app may call this on every screen load. 60/min
       is generous for normal use but catches runaway polling.

GET  /v1/accounts/:accountId/sync-status
  Limit: 30 requests per minute per account
  Why: App polls this every 3 seconds during active sync.
       30/min = one request every 2 seconds — slightly tighter
       than the app's 3-second polling interval.
```

**Layer 2 — Free vs Premium tier enforcement (business logic layer)**

Beyond request rate limits, Free tier users have product-level restrictions enforced in the service layer:

```typescript
// account.service.ts
async initiateConnection(
  userId: string,
  dto: ConnectBankDto
): Promise<ConnectBankResponse> {

  // Premium feature gate — Free tier limited to 1 bank connection
  const user = await this.userService.findById(userId)
  const existingConnections = await this.prisma.bankConnection.count({
    where: { userId, status: { in: ['ACTIVE', 'PENDING'] } }
  })

  if (user.subscriptionTier === 'FREE' && existingConnections >= 1) {
    throw new ForbiddenException(
      'PREMIUM_FEATURE',
      'Connecting multiple bank accounts requires a Premium subscription'
    )
  }

  // Premium users: no limit on connections
  // Family plan users: same as premium
  // ...continue with connection logic
}
```

This tier gate lives in the service, not the API Gateway, because it requires database context (counting existing connections). The API Gateway has no access to business data.

---

Step 8 is complete and ready to be saved as `API Design.md` in `Module 8: API Design`.

**Ready for Step 9: Caching — Redis deep dive, invalidation, multi-level, production failure modes.**