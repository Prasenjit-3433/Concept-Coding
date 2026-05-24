# Step 4: Database Selection & Schema Design

---

## Part 1 — SQL vs NoSQL Decision

This is one of the most common interview topics. The interviewer won't just ask "what database did you use?" — they'll ask **"why?"** and then throw curveballs. Let's build the reasoning properly.

---

### The Business Context First

Before picking any database, you look at what the data actually *is* and how it's *used*.

Your team manages:
- Expense submissions with receipts, amounts, categories, approval states
- Invoice ingestion with line items, VAT, vendor details, payment terms
- Approval workflows — who approved what, when, in what order
- Reimbursement records — who gets paid, how much, via which bank account
- Audit trails — immutable history of every state change

Ask three questions about this data:

```
1. Is it structured or flexible?
   → Every expense has the same fields: amount, currency,
     category, submittedBy, status, approvedBy, receiptUrl.
     Not random/unpredictable shape. STRUCTURED.

2. Are there relationships between entities?
   → Expense belongs to an Employee
     Employee belongs to a Company
     Expense has many ApprovalSteps
     Invoice has many LineItems
     Invoice belongs to a Supplier
     Strong RELATIONAL data.

3. Do operations need to be atomic?
   → "Approve an invoice" must update invoice status
     AND create an audit log entry together.
     Either both happen or neither happens.
     TRANSACTIONAL requirement.
```

All three answers point directly to a **relational database**.

---

### Why PostgreSQL Specifically

Once you decide SQL, the next question is which one.

```
OPTION 1: MySQL
───────────────
✓ Very mature, widely used
✗ Weaker support for advanced data types
✗ JSON support is an afterthought
✗ Less powerful window functions

OPTION 2: PostgreSQL
─────────────────────
✓ JSONB — store semi-structured data 
  (receipt OCR raw output) efficiently
✓ Excellent window functions 
  (useful for financial reporting queries)
✓ Strong ACID guarantees
✓ UUID as native type
✓ Enum types (cleaner than VARCHAR for status)
✓ Row-level locking — critical for 
  concurrent approval scenarios
✓ Most EU fintechs default to Postgres
```

For a spend management platform, **PostgreSQL wins** — particularly because:

- Invoice line items can have semi-structured VAT breakdowns → JSONB
- Concurrent approvals need reliable row-level locking
- Financial reporting queries (monthly spend by category, team, cost center) benefit from window functions

---

### Why Not NoSQL — The Interview Answer

Interviewers love asking this. Here's how to answer it confidently:

**"Why not MongoDB?"**

```
MongoDB is good when:
- Schema changes frequently and unpredictably
- Data has no relationships (or relationships 
  don't need to be enforced)
- You're doing document-style reads 
  (grab one document, it has everything)
- Write speed matters more than consistency

Our data is the opposite:
- Schema is stable and well-defined
- Relationships are central 
  (expense → employee → company → team)
- We need JOINs (expense + approver + policy)
- We need ACID transactions 
  (status update + audit log atomically)
- Consistency is non-negotiable 
  (financial data — you cannot afford 
   a payment going through without 
   a proper approval record)
```

**"What if your schema needs to change?"**

```
We use Flyway migrations.
Schema changes are versioned SQL scripts,
reviewed in PRs like code.
PostgreSQL handles schema evolution fine —
it's not rigid, it's just explicit.
Explicit is good for financial systems.
```

**"What about scale — won't SQL become a bottleneck?"**

```
At 5,000 SME customers, Series B/C stage:
- We're not at Facebook scale
- PostgreSQL handles millions of rows 
  without breaking a sweat
- We use connection pooling (HikariCP)
- Read replicas for reporting queries
  so they don't hit the primary DB
- When you actually hit PostgreSQL limits
  (hundreds of millions of rows), 
  you solve it with partitioning, 
  not by switching to NoSQL

Switching to NoSQL to solve a scale problem
you don't have yet is over-engineering.
That's a mistake early-stage companies make.
```

---

### Where NoSQL *Does* Appear in Our System

Being honest about this actually impresses interviewers — it shows you're not dogmatic.

```
Redis (our team uses this):
────────────────────────────
Not for primary data storage.
For caching — approval policy lookups,
rate limiting, session data.
Key-value access pattern, TTL-based.
Exactly what Redis is designed for.

S3 (object storage):
──────────────────────
Receipt images, invoice PDFs.
These are binary files — you don't
store a 2MB PDF as a blob in PostgreSQL.
S3 is the right tool.
Not a database, but worth mentioning.
```

---

### Database-Per-Service Pattern

Since this is microservices, each service has its **own PostgreSQL instance** (or at minimum, its own schema on a shared cluster).

```
┌─────────────────────────────────────┐
│  Expense & Reimbursement Service    │
│  └── expense_db (PostgreSQL)        │
│       ├── expenses                  │
│       ├── expense_audit_logs        │
│       ├── approval_steps            │
│       └── outbox_events             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  Invoice & AP Service               │
│  └── invoice_db (PostgreSQL)        │
│       ├── invoices                  │
│       ├── invoice_line_items        │
│       ├── suppliers                 │
│       ├── payment_runs              │
│       └── outbox_events             │
└─────────────────────────────────────┘
```

**Why separate databases?**

```
If shared:
- Card Service's bad migration can 
  corrupt Expense Service's tables
- Can't scale databases independently
- Teams step on each other's schema
- One slow query from another service
  locks your tables

If separate:
- Total isolation — their problem 
  stays their problem
- Each team owns their schema fully
- Scale each DB independently
- Clear ownership boundary
```

**The trade-off interviewers will push on:**

```
Q: "How do you do joins across services?"

A: You don't — not at DB level.
   If Expense Service needs user details,
   it calls User Service via FeignClient
   and gets the data at application level.
   
   Or for reporting queries that need 
   data from multiple services, we have 
   a separate read model / data warehouse
   (not in scope for our team's work).
   
   The rule is: no cross-service DB joins.
   Period. That's the price of isolation,
   and it's worth it.
```

---

## Part 2 — Schema Design

Now let's design the actual tables. We'll go service by service.

---

### Service 1 — Expense & Reimbursement Service

#### Entity Relationship Diagram

```
companies ──────────────────────────────────────────┐
    │                                               │
    │ 1:N                                           │
    ▼                                               │
employees ──────────────────────────────────────────┤
    │                                               │
    │ 1:N                                           │
    ▼                                               │
expenses ──────────── 1:N ──── approval_steps       │
    │                                    │          │
    │ 1:1                                │ N:1      │
    ▼                                    ▼          │
expense_receipts                     employees ─────┘
    
expenses ──── 1:1 ──── reimbursements
                              │
                              │ N:1
                              ▼
                         payment_batches

expenses ──── 1:N ──── expense_audit_logs
expenses ──── outbox_events (same DB, decoupled)
```

---

#### Table: `companies`

```sql
CREATE TABLE companies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    country_code    CHAR(2) NOT NULL,         -- ISO 3166 (DE, NL, GB)
    currency        CHAR(3) NOT NULL,         -- ISO 4217 (EUR, GBP)
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Why UUID over auto-increment integer?**
```
In microservices, multiple services generate IDs.
If Service A uses integer IDs and Service B 
uses integer IDs, you'll get collisions when 
data is combined in any reporting layer.
UUID is globally unique by definition.
Also, integer IDs are guessable — 
UUID is not (security benefit for financial data).
```

**Why TIMESTAMPTZ over TIMESTAMP?**
```
TIMESTAMPTZ stores timezone-aware timestamps.
We serve DE, NL, GB — different timezones.
TIMESTAMP without timezone silently drops 
timezone info. In financial systems, 
"when did this happen" must be unambiguous.
Always use TIMESTAMPTZ.
```

---

#### Table: `employees`

```sql
CREATE TABLE employees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id),
    email           VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL,     
    -- EMPLOYEE, FINANCE_MANAGER, ADMIN
    manager_id      UUID REFERENCES employees(id), 
    -- NULL for top-level
    department      VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_employee_email_company 
        UNIQUE (company_id, email)
);

CREATE INDEX idx_employees_company_id 
    ON employees(company_id);
CREATE INDEX idx_employees_manager_id 
    ON employees(manager_id);
```

**Why self-referencing `manager_id`?**
```
Approval policy says expenses above €50 
go to the employee's manager.
Manager is also an employee in the same company.
Self-join on this table is cleaner than 
a separate managers table.
```

---

#### Table: `expenses`

This is the core table. Most of the complexity lives here.

```sql
CREATE TYPE expense_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'PENDING_APPROVAL',
    'APPROVED',
    'REJECTED',
    'REIMBURSED',
    'CANCELLED'
);

CREATE TYPE expense_category AS ENUM (
    'TRAVEL',
    'ACCOMMODATION',
    'CLIENT_ENTERTAINMENT',
    'OFFICE_SUPPLIES',
    'SOFTWARE',
    'TRAINING',
    'OTHER'
);

CREATE TABLE expenses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id),
    employee_id         UUID NOT NULL REFERENCES employees(id),
    
    -- Money fields
    amount              NUMERIC(12, 2) NOT NULL,
    currency            CHAR(3) NOT NULL,
    amount_in_eur       NUMERIC(12, 2),        
    -- converted at submission time
    exchange_rate       NUMERIC(10, 6),        
    -- rate used at submission time
    
    -- Classification
    category            expense_category NOT NULL,
    description         TEXT,
    merchant_name       VARCHAR(255),
    expense_date        DATE NOT NULL,         
    -- date of actual spend, not submission
    
    -- Workflow
    status              expense_status NOT NULL DEFAULT 'DRAFT',
    assigned_approver_id UUID REFERENCES employees(id),
    submitted_at        TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    approved_by_id      UUID REFERENCES employees(id),
    rejected_at         TIMESTAMPTZ,
    rejected_by_id      UUID REFERENCES employees(id),
    rejection_reason    TEXT,
    
    -- Receipt
    receipt_s3_key      VARCHAR(500),
    ocr_extracted_data  JSONB,                 
    -- raw OCR output stored as JSONB
    
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_company_id 
    ON expenses(company_id);
CREATE INDEX idx_expenses_employee_id 
    ON expenses(employee_id);
CREATE INDEX idx_expenses_status 
    ON expenses(status);
CREATE INDEX idx_expenses_company_status 
    ON expenses(company_id, status);   
-- composite: very common query pattern
CREATE INDEX idx_expenses_submitted_at 
    ON expenses(submitted_at);
```

**Why `NUMERIC(12,2)` for money and not `FLOAT` or `DOUBLE`?**
```
FLOAT and DOUBLE are binary floating point.
They cannot represent 0.10 exactly in binary.
0.1 + 0.2 = 0.30000000000000004 in floating point.

For financial data, this is unacceptable.
NUMERIC(12,2) is exact decimal arithmetic.
12 digits total, 2 after decimal point.
Handles up to €9,999,999,999.99 — more than enough.

This is a non-negotiable rule in fintech.
```

**Why store `amount_in_eur` and `exchange_rate`?**
```
An employee in UK submits £85.
Today's rate: 1 GBP = 1.17 EUR.
amount_in_eur = 99.45, exchange_rate = 1.170000

Six months later, the accountant runs a report.
GBP rate is now different.
If we only stored £85, we'd have to 
recalculate using today's rate — 
which gives a different EUR amount than 
what was approved and reimbursed.

In financial systems, you always record 
the exchange rate at the time of the 
transaction. Never recalculate historical amounts.
```

**Why `expense_date` (DATE) separate from `submitted_at` (TIMESTAMPTZ)?**
```
An employee had lunch on Monday (expense_date).
They submit the expense on Wednesday (submitted_at).
These are two different things.

DATEV and accounting systems care about 
when the expense occurred (expense_date),
not when it was submitted in the system.
```

**Why `ocr_extracted_data JSONB`?**
```
OCR returns different fields depending on 
receipt type — restaurant receipts have 
line items, airline receipts have flight numbers,
hotel receipts have check-in/check-out dates.

The raw OCR output shape is unpredictable.
JSONB lets us store it without forcing 
a rigid schema on something inherently flexible.
The structured parts (amount, merchant, date)
we extract and store in proper columns.
The rest stays in JSONB for reference.
```

---

#### Table: `approval_steps`

For multi-level approvals (e.g., manager approves, then finance manager approves for amounts > €2000).

```sql
CREATE TYPE approval_action AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'DELEGATED'
);

CREATE TABLE approval_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses(id),
    step_order      INTEGER NOT NULL,          
    -- 1 = first approver, 2 = second, etc.
    approver_id     UUID NOT NULL REFERENCES employees(id),
    action          approval_action NOT NULL DEFAULT 'PENDING',
    comment         TEXT,
    acted_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_expense_step 
        UNIQUE (expense_id, step_order)
);

CREATE INDEX idx_approval_steps_expense_id 
    ON approval_steps(expense_id);
CREATE INDEX idx_approval_steps_approver_id 
    ON approval_steps(approver_id);
-- approver's dashboard query: 
-- "show me all expenses waiting for my approval"
```

---

#### Table: `reimbursements`

```sql
CREATE TYPE reimbursement_status AS ENUM (
    'PENDING',
    'SCHEDULED',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

CREATE TABLE reimbursements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id          UUID NOT NULL REFERENCES expenses(id),
    employee_id         UUID NOT NULL REFERENCES employees(id),
    company_id          UUID NOT NULL REFERENCES companies(id),
    
    amount              NUMERIC(12, 2) NOT NULL,
    currency            CHAR(3) NOT NULL,
    
    status              reimbursement_status NOT NULL DEFAULT 'PENDING',
    
    -- Bank details (encrypted at application level)
    bank_account_iban   VARCHAR(34),           
    -- stored encrypted
    bank_account_name   VARCHAR(255),
    
    payment_batch_id    UUID,                  
    -- which payment batch this went in
    payment_reference   VARCHAR(100),          
    -- SEPA reference
    
    scheduled_at        TIMESTAMPTZ,
    processed_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    failure_reason      TEXT,
    
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_expense_reimbursement 
        UNIQUE (expense_id)                    
    -- one reimbursement per expense
);
```

---

#### Table: `expense_audit_logs`

```sql
CREATE TABLE expense_audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses(id),
    action          VARCHAR(100) NOT NULL,     
    -- 'SUBMITTED', 'APPROVED', 'REJECTED', etc.
    performed_by_id UUID NOT NULL,             
    -- employee id (no FK — user might be deleted)
    old_status      expense_status,
    new_status      expense_status,
    metadata        JSONB,                     
    -- any extra context
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_expense_id 
    ON expense_audit_logs(expense_id);
```

**Why no FK on `performed_by_id`?**
```
Audit logs are immutable history.
If an employee is deactivated/deleted,
their past actions must still be visible.
A FK would prevent deleting the employee record
or require cascading deletes into audit history.
Both are wrong for compliance reasons.

We store the ID, accept it won't be enforced
at DB level, and enforce it at application level.
```

**Why is this table in Expense DB and not a separate Audit Service DB?**
```
The Audit Service has its OWN copy 
(consumed via Kafka event).
But we also keep a local copy in expense_db 
for two reasons:
1. Regulatory requirement — expense data 
   and its audit trail in one place
2. If Kafka is temporarily down, 
   local audit log is still written 
   (same DB transaction as the status update)
```

---

#### Table: `outbox_events`

This is the Outbox Pattern table — we'll go deep into *why* this exists in the Kafka step. For now, just know the structure.

```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,     -- 'EXPENSE', 'REIMBURSEMENT'
    aggregate_id    UUID NOT NULL,             -- expense_id or reimbursement_id
    event_type      VARCHAR(100) NOT NULL,     -- 'expense.submitted', 'expense.approved'
    payload         JSONB NOT NULL,            -- full event payload
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished 
    ON outbox_events(published, created_at) 
    WHERE published = FALSE;
-- partial index — only indexes unpublished rows
-- keeps it fast as table grows
```

---

### Service 2 — Invoice & AP Service

#### Entity Relationship Diagram

```
suppliers ──────────────────────────────────────┐
    │                                           │
    │ 1:N                                       │
    ▼                                           │
invoices ──── 1:N ──── invoice_line_items       │
    │                                           │
    │ 1:N                                       │
    ▼                                           │
invoice_approval_steps ── N:1 ── employees*    │
                                               │
invoices ──── N:1 ──── payment_runs            │
                                               │
invoices ──── 1:N ──── invoice_audit_logs      │
invoices ──── outbox_events                    │
                                               │
*employees fetched via FeignClient from        │
 User & Org Service, not stored locally        │
```

---

#### Table: `suppliers`

```sql
CREATE TABLE suppliers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL,         
    -- which Moss customer this supplier belongs to
    name                VARCHAR(255) NOT NULL,
    tax_id              VARCHAR(50),           
    -- VAT number
    country_code        CHAR(2) NOT NULL,
    default_currency    CHAR(3) NOT NULL DEFAULT 'EUR',
    
    -- Payment details (encrypted)
    iban                VARCHAR(34),
    bic                 VARCHAR(11),
    bank_name           VARCHAR(255),
    
    -- E-invoicing
    e_invoice_routing_id VARCHAR(255),         
    -- for Peppol/ZUGFeRD
    
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_supplier_company_name 
        UNIQUE (company_id, name)
);

CREATE INDEX idx_suppliers_company_id 
    ON suppliers(company_id);
```

---

#### Table: `invoices`

```sql
CREATE TYPE invoice_status AS ENUM (
    'DRAFT',
    'PENDING_REVIEW',
    'VERIFIED',
    'PENDING_APPROVAL',
    'APPROVED',
    'REJECTED',
    'PAYMENT_PENDING',
    'PAID',
    'ON_HOLD',
    'CANCELLED'
);

CREATE TABLE invoices (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id              UUID NOT NULL,
    supplier_id             UUID NOT NULL REFERENCES suppliers(id),
    
    -- Invoice identity
    invoice_number          VARCHAR(100),      
    -- vendor's invoice number
    
    -- Money
    subtotal_amount         NUMERIC(12, 2) NOT NULL,
    vat_amount              NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(12, 2) NOT NULL,
    currency                CHAR(3) NOT NULL,
    total_amount_in_eur     NUMERIC(12, 2),
    exchange_rate           NUMERIC(10, 6),
    
    -- Dates
    invoice_date            DATE NOT NULL,     
    -- date on the invoice document
    due_date                DATE,              
    -- payment due date
    
    -- Classification
    gl_code                 VARCHAR(50),       
    -- General Ledger code for accounting
    cost_center_id          UUID,              
    -- from User & Org Service
    
    -- Workflow
    status                  invoice_status NOT NULL DEFAULT 'DRAFT',
    uploaded_by_id          UUID NOT NULL,     
    -- employee who uploaded
    assigned_verifier_id    UUID,              
    -- who verifies receipt of goods
    verified_at             TIMESTAMPTZ,
    
    -- Document storage
    document_s3_key         VARCHAR(500),
    original_filename       VARCHAR(255),
    ocr_extracted_data      JSONB,
    
    -- Payment
    payment_run_id          UUID,              
    -- which batch payment
    paid_at                 TIMESTAMPTZ,
    payment_reference       VARCHAR(100),
    
    -- E-invoicing
    e_invoice_format        VARCHAR(50),       
    -- 'ZUGFERD', 'XRECHNUNG', 'PEPPOL', NULL for PDF
    
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_invoice_number_supplier 
        UNIQUE (company_id, supplier_id, invoice_number)
);

CREATE INDEX idx_invoices_company_id 
    ON invoices(company_id);
CREATE INDEX idx_invoices_supplier_id 
    ON invoices(supplier_id);
CREATE INDEX idx_invoices_status 
    ON invoices(status);
CREATE INDEX idx_invoices_due_date 
    ON invoices(due_date) 
    WHERE status NOT IN ('PAID', 'CANCELLED');  
-- partial index for upcoming payments
CREATE INDEX idx_invoices_company_status 
    ON invoices(company_id, status);
```

---

#### Table: `invoice_line_items`

```sql
CREATE TABLE invoice_line_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      UUID NOT NULL REFERENCES invoices(id) 
                    ON DELETE CASCADE,
    
    line_number     INTEGER NOT NULL,
    description     TEXT NOT NULL,
    quantity        NUMERIC(10, 3) NOT NULL DEFAULT 1,
    unit_price      NUMERIC(12, 2) NOT NULL,
    vat_rate        NUMERIC(5, 2) NOT NULL DEFAULT 0,   
    -- percentage, e.g. 19.00 for 19% German VAT
    vat_amount      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    line_total      NUMERIC(12, 2) NOT NULL,
    gl_code         VARCHAR(50),                        
    -- can override invoice-level GL code per line
    
    CONSTRAINT uq_invoice_line_number 
        UNIQUE (invoice_id, line_number)
);
```

**Why `ON DELETE CASCADE` on line items?**
```
Line items have no meaning without their invoice.
If an invoice is deleted (e.g., uploaded by mistake),
the line items should go too.
This is one of the few places CASCADE makes sense.

Compare to audit logs — you'd NEVER cascade delete 
audit logs. They're compliance records.
```

---

#### Table: `invoice_approval_steps`

Same pattern as expenses, slightly different policy:

```sql
CREATE TABLE invoice_approval_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      UUID NOT NULL REFERENCES invoices(id),
    step_order      INTEGER NOT NULL,
    approver_id     UUID NOT NULL,             
    -- employee id from User Service
    approver_role   VARCHAR(50) NOT NULL,      
    -- 'FINANCE_MANAGER', 'CFO' etc.
    -- stored at time of creation 
    -- (role might change later)
    action          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment         TEXT,
    acted_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uq_invoice_step 
        UNIQUE (invoice_id, step_order)
);

CREATE INDEX idx_invoice_approval_approver 
    ON invoice_approval_steps(approver_id, action) 
    WHERE action = 'PENDING';
-- partial index — approver dashboard query
```

---

#### Table: `payment_runs`

Invoices are paid in batches, not one by one. This is how SEPA batch payments work.

```sql
CREATE TYPE payment_run_status AS ENUM (
    'DRAFT',
    'SCHEDULED',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'PARTIALLY_FAILED'
);

CREATE TABLE payment_runs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL,
    
    status              payment_run_status NOT NULL DEFAULT 'DRAFT',
    
    total_amount        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency            CHAR(3) NOT NULL DEFAULT 'EUR',
    invoice_count       INTEGER NOT NULL DEFAULT 0,
    
    scheduled_date      DATE NOT NULL,
    executed_at         TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    
    -- SEPA batch file reference
    sepa_file_reference VARCHAR(100),
    
    created_by_id       UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

### Flyway Migration File Naming

Just so you're clear on how these tables actually get created in production:

```
src/main/resources/db/migration/
├── V1__create_companies_and_employees.sql
├── V2__create_expenses_table.sql
├── V3__create_approval_steps.sql
├── V4__create_reimbursements.sql
├── V5__create_expense_audit_logs.sql
├── V6__create_outbox_events.sql
├── V7__add_index_expenses_company_status.sql
└── V8__add_ocr_extracted_data_column.sql
```

```
Rules Flyway enforces:
- V prefix, double underscore, sequential number
- Once a migration file is applied to any 
  environment, it is NEVER modified
- If you need to change something, 
  write a new migration (V9__, V10__, ...)
- Flyway stores a checksum of each file —
  if you modify an applied migration,
  Flyway detects checksum mismatch and 
  refuses to start the application
```

---

## Interview Questions You'll Get On This

**Q: "Why did you pick PostgreSQL over MongoDB?"**

Answer: Our data is highly relational and transactional. Expenses belong to employees, employees belong to companies, approvals reference both — and approval + audit log must be written atomically. MongoDB would force us to either denormalize everything into one giant document (making updates painful) or manage relationships manually at the application layer. PostgreSQL gives us proper JOINs, ACID transactions, and row-level locking — all of which matter for financial data.

**Q: "How did you handle multi-currency?"**

Answer: We store the original amount and currency as submitted, then also store the EUR-equivalent amount and the exchange rate used at submission time. We never recalculate historical amounts using current exchange rates — that would make historical financial records inconsistent. The exchange rate at the time of the transaction is the truth.

**Q: "Why UUIDs instead of auto-increment IDs?"**

Answer: In a microservices system, multiple services generate IDs independently. Auto-increment integers would collide if any data is ever correlated across services. UUIDs are globally unique by design. There's also a security benefit — integer IDs are sequential and guessable; UUIDs are not.

**Q: "How do you handle schema changes without downtime?"**

Answer: Flyway migrations, deployed before the new application code. The pattern is backward-compatible migrations — add a column as nullable first, deploy the code that writes to it, then in a later migration make it NOT NULL once all rows have data. Never drop a column until the code that references it is removed first.

---

Step 4 complete.

**Next is Step 5: Kafka Architecture & Usage** — this is where we go deep on how events flow through the system, producer/consumer setup, the Outbox Pattern, exception handling, and Dead Letter Queues. Ready?