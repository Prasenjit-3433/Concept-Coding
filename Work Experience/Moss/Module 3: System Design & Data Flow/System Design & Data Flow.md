# Step 2: System Design & Data Flows

This step is about tracing exactly what happens — technically — when a business action occurs. This is where interviewers love to go deep: *"Walk me through what happens when an employee submits an expense."* You need to trace it from HTTP request all the way to database, Kafka, and back.

We'll cover the most important flows in our system. Since your team owns **Expense & Reimbursement + Invoice & AP**, I'll give those the most depth.

---

## First — The Overall System Map

Before individual flows, let's establish how all services relate to each other:

```
                        ┌─────────────┐
                        │   Clients   │
                        │ (Web/Mobile)│
                        └──────┬──────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │   API Gateway    │
                    │ (JWT validation, │
                    │    routing)      │
                    └────────┬─────────┘
                             │
         ┌───────────────────┼────────────────────┐
         │                   │                    │
         ▼                   ▼                    ▼
┌─────────────┐    ┌──────────────────┐    ┌────────────┐
│Auth Service │    │  User & Org      │    │Card Service│
│             │    │  Service         │    │            │
│- Login      │    │- Companies       │    │- Issue     │
│- JWT issue  │    │- Teams           │    │  cards     │
│- Refresh    │    │- Roles           │    │- Set limits│
│- Logout     │    │- Budget centers  │    │- Track txns│
└─────────────┘    └──────────────────┘    └────────────┘

         ┌───────────────────┬────────────────────┐
         │                   │                    │
         ▼                   ▼                    ▼
┌─────────────────┐  ┌───────────────┐  ┌────────────────┐
│    Expense &    │  │  Invoice & AP │  │    Payment     │
│  Reimbursement  │  │    Service    │  │    Service     │
│    Service      │  │               │  │                │
│                 │  │- Ingest       │  │- SEPA transfer │
│- Submit expense │  │  invoices     │  │- Multi-currency│
│- Receipt OCR    │  │- AP workflows │  │- Actual money  │
│- Approval flow  │  │- Payment runs │  │  movement      │
│- Reimbursements │  │- E-invoicing  │  │                │
└────────┬────────┘  └───────┬───────┘  └───────┬────────┘
         │                   │                  │
         └───────────────────┴──────────────────┘
                             │
                    ┌────────▼────────┐
                    │      Kafka      │
                    │  (Event Bus)    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
     ┌──────────────┐ ┌──────────┐ ┌────────────────┐
     │ Notification │ │  Audit   │ │  Accounting    │
     │   Service    │ │  Service │ │ Integration    │
     │              │ │          │ │    Service     │
     │- Email       │ │- Immut.  │ │- DATEV export  │
     │- In-app push │ │  log of  │ │- Xero sync     │
     │- Slack       │ │  all     │ │- NetSuite      │
     │  webhook     │ │  actions │ │                │
     └──────────────┘ └──────────┘ └────────────────┘
```

---

## Now The Key Data Flows

### Flow 1 — Employee Submits an Expense

This is the most fundamental flow in your team's service. Let's trace it completely.

**Business scenario:** An employee paid €85 for a client lunch on their personal card. They want to get reimbursed.

```
STEP 1: Employee opens mobile app, fills expense form
        - Amount: €85.00
        - Currency: EUR
        - Category: Client Entertainment
        - Description: "Client lunch - Acme Corp"
        - Uploads receipt photo

STEP 2: Mobile app sends multipart request to API Gateway
        POST /api/expenses
        Headers: Authorization: Bearer <JWT>
        Body: {expense details + receipt image}

STEP 3: API Gateway
        - Validates JWT signature
        - Extracts userId, companyId, role from JWT claims
        - Attaches headers: X-User-Id, X-Company-Id, X-User-Role
        - Routes to Expense & Reimbursement Service

STEP 4: Expense Service receives request
        Controller → Service → multiple things happen:

        4a. Receipt image uploaded to AWS S3
            - Key: receipts/{companyId}/{expenseId}/{filename}
            - S3 returns a URL
            - URL stored in expense record

        4b. Expense record created in PostgreSQL
            Status: PENDING_SUBMISSION (draft state)

        4c. OCR triggered asynchronously (@Async)
            - Calls third-party OCR service
            - Extracts amount, date, merchant from receipt
            - Updates expense record with extracted data
            - Employee can correct if OCR is wrong

STEP 5: Employee reviews OCR result, clicks "Submit"
        PUT /api/expenses/{expenseId}/submit

STEP 6: Expense Service
        - Validates expense (amount > 0, category set, receipt attached)
        - Determines approver:
          Fetches approval policy from User & Org Service (FeignClient)
          Policy says: expenses > €50 need manager approval
          Fetches manager of this employee
        - Updates expense status: SUBMITTED → PENDING_APPROVAL
        - Saves approver assignment

STEP 7: Expense Service publishes Kafka event
        Topic: expense.submitted
        Payload: {
          expenseId, employeeId, companyId,
          amount, currency, approverId, submittedAt
        }

STEP 8: Notification Service consumes expense.submitted
        - Sends email to manager/approver:
          "John submitted an expense of €85 for your approval"
        - Sends in-app notification

STEP 9: Audit Service consumes expense.submitted
        - Writes immutable audit log entry:
          "Expense EXP-001 submitted by user U-123 at 14:32:01"

STEP 10: Employee sees expense in "Pending Approval" state
         Manager gets email + in-app notification
```

**Diagram:**

```
Employee                API Gateway          Expense Service
   │                        │                      │
   │──POST /api/expenses────▶│                      │
   │                        │──route + headers─────▶│
   │                        │                      │──upload receipt──▶ S3
   │                        │                      │◀──receipt URL─────┘
   │                        │                      │──save DRAFT──────▶ PostgreSQL
   │                        │                      │──OCR (@Async)────▶ OCR Service
   │◀───201 Created─────────│◀─────────────────────│
   │                        │                      │
   │──PUT /submit───────────▶│                      │
   │                        │──route───────────────▶│
   │                        │                      │──FeignClient────▶ User & Org Service
   │                        │                      │◀──approver info──┘
   │                        │                      │──update DB───────▶ PostgreSQL
   │                        │                      │──publish event───▶ Kafka (expense.submitted)
   │◀───200 OK──────────────│◀─────────────────────│
   │                        │                      │
   │                    Kafka consumers:            │
   │                    Notification Service ◀──────┤ (consumes expense.submitted)
   │                    Audit Service ◀─────────────┘ (consumes expense.submitted)
```

---

### Flow 2 — Manager Approves an Expense

```
STEP 1: Manager clicks "Approve" in web app
        PUT /api/expenses/{expenseId}/approve
        Headers: Authorization: Bearer <JWT> (manager's token)

STEP 2: API Gateway validates JWT, routes to Expense Service

STEP 3: Expense Service
        - Checks: is this user actually the assigned approver? (authorization check)
        - Checks: is expense in PENDING_APPROVAL status?
        - Updates expense status: PENDING_APPROVAL → APPROVED
        - Records: approvedBy, approvedAt

STEP 4: Publishes Kafka event
        Topic: expense.approved
        Payload: {
          expenseId, employeeId, companyId,
          amount, currency, approvedBy, approvedAt
        }

STEP 5: Multiple consumers act on expense.approved:

        Payment Service:
        - Creates a reimbursement payment record
        - Schedules SEPA transfer to employee's bank account
        - Usually batched (all approvals from Mon-Fri processed Friday)

        Notification Service:
        - Emails employee: "Your €85 expense was approved"
        - In-app notification

        Accounting Integration Service:
        - Queues this expense for next DATEV/Xero export
        - Maps category → accounting code

        Audit Service:
        - Logs approval event immutably
```

**Key design decision here:**

Why does Payment Service create the reimbursement, not Expense Service directly?

Because **Expense Service should not know about money movement**. It knows about expense lifecycle. Payment Service knows how to move money. This is separation of concerns — and it's also why a bug in Payment Service doesn't break expense submission.

---

### Flow 3 — Supplier Invoice Ingestion & Approval

**Business scenario:** A finance manager receives a €3,200 invoice from a software vendor. They need to process it — verify, approve, and pay it.

```
STEP 1: Finance manager uploads invoice PDF
        POST /api/invoices
        Body: PDF file + metadata

STEP 2: Invoice & AP Service
        - Saves PDF to S3
        - Creates invoice record: Status = DRAFT
        - Triggers OCR/extraction (async):
          Extracts: vendor name, amount, due date, 
                    line items, VAT breakdown, IBAN
        - Status → PENDING_REVIEW

STEP 3: Finance manager reviews extracted data
        - Corrects any OCR errors
        - Assigns cost center, GL code
        - Assigns to verifier (person who confirms 
          goods/services were received)

STEP 4: Verifier confirms receipt of goods/services
        PUT /api/invoices/{invoiceId}/verify
        Status → VERIFIED

STEP 5: Invoice goes to approval workflow
        - Policy: invoices > €1000 need Finance Manager approval
        - Policy: invoices > €5000 need CFO approval
        Status → PENDING_APPROVAL
        Kafka event: invoice.pending_approval published

STEP 6: Approver approves
        PUT /api/invoices/{invoiceId}/approve
        Status → APPROVED
        Kafka event: invoice.approved published

STEP 7: Payment Service consumes invoice.approved
        - Adds to next payment run
        - Payment runs happen twice a week (configurable)
        - Batches all approved invoices
        - Initiates SEPA batch payment

STEP 8: Payment Service publishes payment.completed
        Invoice & AP Service consumes this:
        - Updates invoice status → PAID
        - Records payment date, reference number

STEP 9: Accounting Integration Service consumes invoice.approved
        - Maps to DATEV/Xero format
        - Exports on nightly schedule
```

**Invoice Status State Machine:**

```
                    ┌─────────┐
                    │  DRAFT  │ ← Invoice uploaded
                    └────┬────┘
                         │ OCR complete
                         ▼
               ┌──────────────────┐
               │  PENDING_REVIEW  │ ← Finance manager reviews
               └────────┬─────────┘
                        │ Assigned to verifier
                        ▼
                 ┌────────────┐
                 │  VERIFIED  │ ← Verifier confirms receipt
                 └─────┬──────┘
                       │ Sent for approval
                       ▼
            ┌────────────────────┐
            │  PENDING_APPROVAL  │ ← Waiting for approver
            └────────┬───────────┘
                     │ Approved
                     ▼
               ┌──────────────┐
               │   APPROVED   │ ← Ready for payment
               └──────┬───────┘
                      │ Payment initiated
                      ▼
             ┌─────────────────┐
             │ PAYMENT_PENDING │ ← SEPA transfer in progress
             └────────┬────────┘
                      │ Payment confirmed
                      ▼
                  ┌────────┐
                  │  PAID  │ ← Done
                  └────────┘
                      
Also possible from most states:
                  ┌──────────┐
                  │ REJECTED │ ← Approver rejects
                  └──────────┘
                  ┌──────────┐
                  │  ON_HOLD │ ← Finance puts on hold
                  └──────────┘
```

---

### Flow 4 — Authentication Flow

Every request starts here. Important to know even though Auth Service isn't your team's service.

```
LOGIN:
──────
User submits email + password
        │
        ▼
API Gateway routes to Auth Service
        │
        ▼
Auth Service:
- Loads user from DB (UserDetailsService)
- BCrypt compares password hash
- On success:
  - Generates access token (JWT, 15 min expiry)
    Payload: { userId, companyId, role, email }
  - Generates refresh token (JWT, 7 days)
- Returns:
  - Access token in response body
  - Refresh token as HttpOnly cookie

SUBSEQUENT REQUESTS:
────────────────────
Client sends: Authorization: Bearer <access_token>
        │
        ▼
API Gateway:
- Validates JWT signature (has the secret key)
- Checks expiry
- Extracts claims
- Adds headers: X-User-Id, X-Company-Id, X-User-Role
- Forwards to target service
        │
        ▼
Target service (e.g. Expense Service):
- Does NOT re-validate JWT
- Reads X-User-Id, X-User-Role headers
- Uses @PreAuthorize for method-level authorization
- Proceeds with business logic

TOKEN REFRESH:
──────────────
Access token expires after 15 min
Client sends refresh token (HttpOnly cookie)
to POST /api/auth/refresh
        │
        ▼
Auth Service validates refresh token
Issues new access token (15 min)
Client continues seamlessly
```

---

### Flow 5 — Cross-Service Data Consistency Problem

This is an interview favourite. *"What happens if your Kafka consumer fails halfway through processing?"*

**The scenario:**
Invoice is approved. Invoice & AP Service updates status to APPROVED in its database, then tries to publish `invoice.approved` to Kafka. But Kafka is temporarily unavailable. Now:
- Database says: APPROVED
- Kafka event: never published
- Payment Service never knows
- Invoice never gets paid

**This is the dual-write problem.**

```
❌ NAIVE APPROACH (what breaks):
─────────────────────────────────
DB Update ──▶ SUCCESS
Kafka Publish ──▶ FAILS
Result: DB and Kafka out of sync forever

✅ WHAT WE DO (Transactional Outbox Pattern):
──────────────────────────────────────────────
Instead of publishing to Kafka directly,
we write to an OUTBOX table in the SAME 
database transaction as the status update.

BEGIN TRANSACTION
  UPDATE invoice SET status = 'APPROVED'
  INSERT INTO outbox_events (event_type, payload, created_at)
  VALUES ('invoice.approved', '{...}', NOW())
COMMIT

A separate Outbox Poller process:
- Reads unpublished events from outbox table
- Publishes to Kafka
- Marks as published

This way: either BOTH the invoice update 
AND the outbox entry succeed, 
or NEITHER does. Atomically.
```

We'll go very deep on this pattern in the Kafka step. I'm mentioning it here so you understand *why* it exists when we get there.

---

### Flow 6 — Config & Secrets Flow

```
At Service Startup:
───────────────────

Service starts
     │
     ▼
Reads bootstrap.properties:
"fetch config from Spring Cloud Config Server"
     │
     ▼
Config Server fetches from Git repo:
- application.properties (common to all)
- expense-service.properties (service-specific)
- expense-service-prod.properties (env-specific)
     │
     ▼
Config Server returns merged properties
     │
     ▼
For sensitive values (DB password, API keys):
Properties contain placeholders like:
  spring.datasource.password=${DB_PASSWORD}
These are resolved from AWS Secrets Manager
(injected as environment variables at ECS task level)
     │
     ▼
Service fully configured, starts up
Flyway runs pending DB migrations
Application ready
```

---

## Putting It All Together — Request Lifecycle Summary

```
Any request through the system follows this path:

CLIENT
  │
  │ HTTPS request + JWT
  ▼
API GATEWAY
  │ Validates JWT
  │ Adds user context headers
  │ Routes to correct service
  ▼
TARGET SERVICE (e.g. Expense Service)
  │
  ├──▶ CONTROLLER (validates input, @Valid)
  │
  ├──▶ SERVICE LAYER (business logic)
  │       │
  │       ├──▶ REPOSITORY (JPA → PostgreSQL)
  │       │
  │       ├──▶ FEIGN CLIENT (sync calls to other services if needed)
  │       │       └──▶ User Service, Card Service etc.
  │       │
  │       └──▶ KAFKA PRODUCER (async events)
  │               └──▶ Notification, Audit, Payment Services
  │
  └──▶ RESPONSE back to client
  
Meanwhile, in the background:
  KAFKA CONSUMERS in each service
  process events from other services
  independently, at their own pace
```

---

## What an Interviewer Might Ask From This Step

**Q: "Why did you use Kafka instead of direct REST calls for notifications?"**

A: If Notification Service is down and we used REST, the invoice approval would fail — which is wrong. A payment blocking on a notification is a bad design. With Kafka, we publish the event and move on. Notification Service processes it when it's available. The core business action (approval) is decoupled from side effects (notification, audit, accounting export).

**Q: "What happens if the same Kafka event gets delivered twice?"**

A: Consumers are designed to be idempotent. For example, before creating a payment record, Payment Service checks: does a payment already exist for this expenseId? If yes, skip. We'll cover this in depth in the Kafka step.

**Q: "How does service A get data about a user if it's owned by User Service?"**

A: Two ways. For real-time data needed in the request flow: FeignClient synchronous call. For data that doesn't change often (like a user's manager): we may cache it in Redis with a TTL, so we don't call User Service on every single request.

---

Step 2 complete. We now have a clear picture of how data moves through the entire system.

**Next is Step 3: Team Structure & Ownership** — this is where we build the org chart, place you in your team, name your colleagues, define the working style, sprint structure, and your onboarding experience.

Ready to proceed?