## Core Principle: Normalized Design

We follow **3rd Normal Form (3NF)** to avoid data duplication and maintain consistency.

---

## 1. Users & Authentication

### **users** table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- bcrypt hashed
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    date_of_birth DATE,
    country_code CHAR(2),  -- ISO 3166-1 alpha-2 (DE, FR, ES, etc.)

    -- KYC (Know Your Customer) fields
    kyc_status VARCHAR(20) DEFAULT 'pending',  -- pending, verified, rejected
    kyc_verified_at TIMESTAMP,
    kyc_document_id VARCHAR(100),  -- Reference to document storage (S3)

    -- Account status
    status VARCHAR(20) DEFAULT 'active',  -- active, suspended, deleted
    email_verified BOOLEAN DEFAULT false,
    email_verified_at TIMESTAMP,

    -- Preferences
    language VARCHAR(5) DEFAULT 'en',  -- en, de, fr, es
    currency VARCHAR(3) DEFAULT 'EUR',  -- ISO 4217
    timezone VARCHAR(50) DEFAULT 'Europe/London',

    -- Risk profile for investments
    risk_profile VARCHAR(20),  -- conservative, moderate, aggressive
    risk_assessment_completed_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_email (email),
    INDEX idx_kyc_status (kyc_status),
    INDEX idx_country (country_code)
);

```

**Why these fields?**

- `email`: Unique identifier for login
- `kyc_status`: Required for regulatory compliance (can't invest without KYC)
- `risk_profile`: Determines investment portfolio recommendations
- `currency`: User's preferred currency for display (we store everything in EUR internally)

---

### **user_sessions** table

```sql
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,  -- JWT token hash
    device_info JSONB,  -- {deviceType: "iPhone 13", os: "iOS 16.2", ip: "..."}
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_token_hash (token_hash),
    INDEX idx_expires_at (expires_at)
);

```

**Why separate session table?**

- Can track multiple devices per user
- Easy to revoke sessions (delete row)
- Query "active sessions" without scanning users table
- Store device info for security (detect unusual logins)

**Why not just use Redis?**

- Redis for fast lookups (session validation on every request)
- PostgreSQL for persistence (Redis can be flushed, need permanent record for security audit)
- **`Pattern`**: Store in both - Redis for speed, PostgreSQL for durability

---

## 2. Bank Accounts & Transactions

### **bank_connections** table

```sql
CREATE TABLE bank_connections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Plaid/TrueLayer integration
    provider VARCHAR(50) NOT NULL,  -- plaid, truelayer
    provider_account_id VARCHAR(255) UNIQUE NOT NULL,  -- External provider's ID
    access_token_encrypted TEXT NOT NULL,  -- Encrypted access token for API calls

    -- Bank details
    bank_name VARCHAR(255),
    account_type VARCHAR(50),  -- checking, savings
    account_mask VARCHAR(10),  -- Last 4 digits: "****1234"

    status VARCHAR(20) DEFAULT 'active',  -- active, disconnected, error
    last_synced_at TIMESTAMP,
    sync_frequency_hours INT DEFAULT 24,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_last_synced (last_synced_at)
);

```

**Why store encrypted tokens?**

- Security: access tokens allow withdrawing money - must encrypt at rest
- Encryption key stored in AWS Secrets Manager, not in code
- Even if database is compromised, tokens are useless without encryption key

---

### **accounts** table

```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bank_connection_id BIGINT REFERENCES bank_connections(id) ON DELETE SET NULL,

    account_type VARCHAR(50) NOT NULL,  -- checking, savings, investment
    account_name VARCHAR(100),  -- User-defined name: "Main Checking", "Emergency Fund"

    -- Balance (stored in cents to avoid floating point issues)
    balance_cents BIGINT DEFAULT 0,  -- €54.20 stored as 5420
    currency VARCHAR(3) DEFAULT 'EUR',

    -- For investment accounts
    is_investment_account BOOLEAN DEFAULT false,

    status VARCHAR(20) DEFAULT 'active',  -- active, closed

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_bank_connection (bank_connection_id),
    INDEX idx_account_type (account_type),

    CONSTRAINT positive_balance CHECK (balance_cents >= 0)
);

```

**Why balance in cents?**

- **Avoid floating point errors**:
    - `0.1 + 0.2 = 0.30000000000000004` in JavaScript/many languages
    - Financial calculations with floats lead to rounding errors over time
    - Storing cents (integers) is exact: `10 + 20 = 30` always
- **Display**: Convert to currency on read: `balance_cents / 100 = €54.20`

**Why separate accounts table from bank_connections?**

- One user can have multiple accounts from same bank connection
- Investment accounts are internal (not from external bank)
- Allows manual accounts (user enters balance manually if bank not supported)

---

### **transactions** table

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,

    -- Transaction details
    amount_cents BIGINT NOT NULL,  -- Negative for expenses, positive for income
    currency VARCHAR(3) DEFAULT 'EUR',
    description TEXT,
    merchant_name VARCHAR(255),

    -- Categorization
    category VARCHAR(50),  -- food, transportation, entertainment, etc.
    subcategory VARCHAR(50),
    is_recurring BOOLEAN DEFAULT false,

    -- External reference (from bank)
    external_transaction_id VARCHAR(255),  -- Plaid transaction ID
    external_category VARCHAR(100),  -- Original category from Plaid

    -- Dates
    transaction_date DATE NOT NULL,
    posted_date DATE,

    -- Status
    status VARCHAR(20) DEFAULT 'posted',  -- pending, posted, cancelled

    -- User modifications
    user_modified_category BOOLEAN DEFAULT false,  -- Did user manually change category?
    notes TEXT,  -- User can add notes

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_account_id (account_id),
    INDEX idx_transaction_date (transaction_date),
    INDEX idx_category (category),
    INDEX idx_external_id (external_transaction_id),

    -- Prevent duplicate imports from bank
    UNIQUE (account_id, external_transaction_id)
);

```

**Why track user_modified_category?**

- Auto-categorization (ML model) can be wrong
- If user manually changes "Starbucks" from "shopping" to "food", remember preference
- Next time we see "Starbucks", use "food" category for this user
- **Learning system**: Improve categorization over time per user

**Why unique constraint on (account_id, external_transaction_id)?**

- When syncing bank transactions daily, same transaction might appear multiple times
- Constraint prevents duplicate imports
- On conflict, we update existing transaction instead of inserting new

---

## 3. Budgets & Goals

### **budgets** table

```sql
CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Budget period
    period_type VARCHAR(20) DEFAULT 'monthly',  -- weekly, monthly, yearly
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,

    -- Category
    category VARCHAR(50) NOT NULL,  -- food, transportation, etc.

    -- Budget limit (in cents)
    limit_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Spending tracking
    spent_cents BIGINT DEFAULT 0,  -- Calculated from transactions
    last_calculated_at TIMESTAMP,

    -- Alerts
    alert_threshold_percent INT DEFAULT 80,  -- Alert when 80% spent
    alert_sent BOOLEAN DEFAULT false,
    alert_sent_at TIMESTAMP,

    -- Rollover (carry unused budget to next month)
    allow_rollover BOOLEAN DEFAULT false,
    rollover_amount_cents BIGINT DEFAULT 0,

    status VARCHAR(20) DEFAULT 'active',  -- active, archived

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_period_dates (period_start_date, period_end_date),
    INDEX idx_category (category),

    -- One budget per category per period
    UNIQUE (user_id, category, period_start_date)
);

```

**How spending is calculated:**

```sql
-- Triggered by INSERT/UPDATE on transactions table
-- Updates spent_cents in budgets table

CREATE OR REPLACE FUNCTION update_budget_spending()
RETURNS TRIGGER AS $$
BEGIN
    -- Find matching budget for this transaction
    UPDATE budgets
    SET
        spent_cents = (
            SELECT COALESCE(SUM(ABS(amount_cents)), 0)
            FROM transactions
            WHERE
                user_id = NEW.user_id
                AND category = NEW.category
                AND transaction_date >= budgets.period_start_date
                AND transaction_date <= budgets.period_end_date
                AND amount_cents < 0  -- Only expenses (negative amounts)
        ),
        last_calculated_at = NOW()
    WHERE
        user_id = NEW.user_id
        AND category = NEW.category
        AND NEW.transaction_date >= period_start_date
        AND NEW.transaction_date <= period_end_date;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_budget_spending
AFTER INSERT OR UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION update_budget_spending();

```

**Why use *database trigger instead of application code*?**

- **Data consistency**: Spending always matches transactions (can't get out of sync)
- **Atomicity**: Transaction insert + budget update happens in one database transaction
- **Performance**: Database calculates sum faster than fetching to app and calculating
- **Reliability**: Even if app crashes, trigger ensures budget is updated

---

### **savings_goals** table

```sql
CREATE TABLE savings_goals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Goal details
    name VARCHAR(255) NOT NULL,  -- "Emergency Fund", "Vacation to Italy"
    description TEXT,
    target_amount_cents BIGINT NOT NULL,
    current_amount_cents BIGINT DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Timeline
    target_date DATE,
    started_at DATE DEFAULT CURRENT_DATE,

    -- Linked account (where money is saved)
    linked_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,

    -- Auto-save settings
    auto_save_enabled BOOLEAN DEFAULT false,
    auto_save_amount_cents BIGINT,
    auto_save_frequency VARCHAR(20),  -- daily, weekly, monthly
    auto_save_day_of_week INT,  -- 1=Monday, 7=Sunday (for weekly)
    auto_save_day_of_month INT,  -- 1-31 (for monthly)

    status VARCHAR(20) DEFAULT 'active',  -- active, completed, cancelled
    completed_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_target_date (target_date),

    CONSTRAINT positive_target CHECK (target_amount_cents > 0)
);

```

**Auto-save example:**

- User sets: *"Save €50 every Monday towards Emergency Fund"*
- BullMQ cron job runs every Monday
- Checks all goals with `auto_save_enabled = true` and `auto_save_day_of_week = 1`
- Transfers €50 from checking account to savings account
- Updates `current_amount_cents`

---

### **goal_contributions** table

```sql
CREATE TABLE goal_contributions (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    amount_cents BIGINT NOT NULL,
    contribution_type VARCHAR(20),  -- manual, auto, bonus (e.g., tax refund)

    -- Link to transaction if applicable
    transaction_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,

    contributed_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_goal_id (goal_id),
    INDEX idx_user_id (user_id),
    INDEX idx_contributed_at (contributed_at)
);

```

**Why separate contributions table?**

- Track history of all contributions to goal
- Calculate progress over time (show chart: *"You've saved €200 in last 3 months"*)
- Distinguish between manual vs auto contributions (analytics)
- Allow undo (user can delete contribution if added by mistake)

---

## 4. Investments & Portfolio

### **portfolios** table

```sql
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Portfolio configuration
    name VARCHAR(255) NOT NULL,  -- User-defined: "Retirement Fund", "Growth Portfolio"
    portfolio_type VARCHAR(50) NOT NULL,  -- conservative, balanced, growth, aggressive

    -- Target allocation (stored as JSON for flexibility)
    target_allocation JSONB NOT NULL,
    -- Example: {"stocks": 60, "bonds": 30, "cash": 10}
    -- Or detailed: [
    --   {"asset": "VUSA.L", "allocation": 35},
    --   {"asset": "VEUR.L", "allocation": 25},
    --   {"asset": "VGOV.L", "allocation": 30},
    --   {"asset": "CASH", "allocation": 10}
    -- ]

    -- Current value (calculated from holdings)
    total_value_cents BIGINT DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Performance tracking
    total_invested_cents BIGINT DEFAULT 0,  -- How much user has put in
    total_return_cents BIGINT DEFAULT 0,  -- Profit/loss
    return_percentage DECIMAL(10, 4),  -- 12.5% stored as 12.5000

    -- Rebalancing settings
    auto_rebalance BOOLEAN DEFAULT true,
    rebalance_threshold_percent INT DEFAULT 5,  -- Rebalance if allocation drifts >5%
    last_rebalanced_at TIMESTAMP,

    -- Tax optimization
    tax_loss_harvesting_enabled BOOLEAN DEFAULT false,

    status VARCHAR(20) DEFAULT 'active',  -- active, closed

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_portfolio_type (portfolio_type),
    INDEX idx_status (status)
);

```

**Why JSONB for target_allocation?**

- **Flexibility**: Different portfolio types have different allocations
    - Conservative: 80% bonds, 20% stocks
    - Aggressive: 90% stocks, 10% bonds
- **Evolution**: Can add new asset classes without schema migration
    - Start with stocks/bonds
    - Later add: real estate, commodities, crypto
- **Query support**: PostgreSQL JSONB is indexable and queryable

    ```sql
    -- Find portfolios with >50% stocksSELECT * FROM portfolios WHERE (target_allocation->>'stocks')::int > 50;
    
    ```


---

### **holdings** table

```sql
CREATE TABLE holdings (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Asset details
    asset_type VARCHAR(20) NOT NULL,  -- etf, stock, bond, cash
    symbol VARCHAR(20) NOT NULL,  -- VUSA.L, VGOV.L, etc.
    asset_name VARCHAR(255),  -- "Vanguard S&P 500 ETF"

    -- Holdings
    quantity DECIMAL(18, 8) NOT NULL,  -- Support fractional shares: 10.5 shares
    average_buy_price_cents BIGINT NOT NULL,  -- Average price paid per share

    -- Current value
    current_price_cents BIGINT,  -- Latest market price (updated every 15 min)
    current_value_cents BIGINT,  -- quantity * current_price_cents

    -- Cost basis (for tax calculations)
    total_cost_basis_cents BIGINT,  -- How much user paid for these shares
    unrealized_gain_loss_cents BIGINT,  -- current_value - cost_basis

    -- Dates
    first_purchased_at TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_portfolio_id (portfolio_id),
    INDEX idx_user_id (user_id),
    INDEX idx_symbol (symbol),

    -- One holding per symbol per portfolio
    UNIQUE (portfolio_id, symbol)
);

```

**Why track average_buy_price?**

- User buys 10 shares of VUSA.L at €80 each
- Later buys 5 more shares at €90 each
- Average buy price: `(10 * 80 + 5 * 90) / 15 = €83.33`
- Needed for calculating profit/loss and tax reporting

**How unrealized_gain_loss is calculated:**

```sql
CREATE OR REPLACE FUNCTION calculate_unrealized_gain()
RETURNS TRIGGER AS $$
BEGIN
    NEW.current_value_cents = NEW.quantity * NEW.current_price_cents;
    NEW.unrealized_gain_loss_cents = NEW.current_value_cents - NEW.total_cost_basis_cents;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_calculate_gain
BEFORE INSERT OR UPDATE ON holdings
FOR EACH ROW
EXECUTE FUNCTION calculate_unrealized_gain();

```

---

### **orders** table

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,

    -- Order details
    order_type VARCHAR(20) NOT NULL,  -- buy, sell, rebalance
    status VARCHAR(20) DEFAULT 'pending',  -- pending, processing, completed, failed, cancelled

    -- Investment amount
    total_amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Allocation (what to buy/sell)
    allocation JSONB,
    -- Example for buy order:
    -- [
    --   {"symbol": "VUSA.L", "amount_cents": 30000, "quantity": null},
    --   {"symbol": "VGOV.L", "amount_cents": 20000, "quantity": null}
    -- ]
    -- After execution, quantities are filled in

    -- Execution details
    executed_at TIMESTAMP,
    execution_details JSONB,  -- Broker response, trade confirmations

    -- Fees
    platform_fee_cents BIGINT DEFAULT 0,
    broker_fee_cents BIGINT DEFAULT 0,
    total_fees_cents BIGINT DEFAULT 0,

    -- Error handling
    error_message TEXT,
    retry_count INT DEFAULT 0,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_portfolio_id (portfolio_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_order_type (order_type)
);

```

**Order lifecycle:**

1. **pending**: User places order, waiting for validation
2. **processing**: Investment Engine calculates allocation, Transaction Service executing trades
3. **completed**: All trades executed successfully, holdings updated
4. **failed**: Error during execution (insufficient funds, market closed, API error)
5. **cancelled**: User cancelled before execution

---

### **order_executions** table

```sql
CREATE TABLE order_executions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,

    -- What was traded
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 8) NOT NULL,
    price_cents BIGINT NOT NULL,  -- Execution price per share
    total_amount_cents BIGINT NOT NULL,  -- quantity * price

    -- Broker details
    broker_order_id VARCHAR(255),  -- External broker's order ID
    broker_name VARCHAR(100),

    -- Timing
    executed_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_order_id (order_id),
    INDEX idx_symbol (symbol),
    INDEX idx_executed_at (executed_at)
);

```

**Why separate executions table?**

- One order can result in multiple trades (buying 3 different ETFs)
- Track each individual trade separately
- Broker might execute at different times/prices (especially for large orders)
- Needed for detailed tax reporting (each execution is a taxable event)

---

## 5. Financial Ledger (Accounting)

### **ledger** table

```sql
CREATE TABLE ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,

    -- Transaction type
    entry_type VARCHAR(50) NOT NULL,
    -- investment_debit, investment_credit, fee_debit, dividend_credit,
    -- transfer_debit, transfer_credit, refund_credit

    -- Amount
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    balance_after_cents BIGINT,  -- Account balance after this entry

    -- References
    reference_type VARCHAR(50),  -- order, transaction, subscription, goal
    reference_id BIGINT,  -- ID of the referenced entity

    -- Description
    description TEXT,

    -- Audit trail
    created_by VARCHAR(50) DEFAULT 'system',  -- system, admin, user
    created_at TIMESTAMP DEFAULT NOW(),

    -- ***Immutability*** (ledger entries should NEVER be updated or deleted)
    is_reversal BOOLEAN DEFAULT false,  -- True if this reverses a previous entry
    reverses_entry_id BIGINT REFERENCES ledger(id),

    INDEX idx_user_id (user_id),
    INDEX idx_account_id (account_id),
    INDEX idx_entry_type (entry_type),
    INDEX idx_created_at (created_at),
    INDEX idx_reference (reference_type, reference_id)
);

-- Prevent updates and deletes on ledger (immutable)
CREATE RULE ledger_no_update AS ON UPDATE TO ledger DO INSTEAD NOTHING;
CREATE RULE ledger_no_delete AS ON DELETE TO ledger DO INSTEAD NOTHING;

```

**Why an *immutable* ledger?**

- **Regulatory requirement**: Financial records must be tamper-proof
- **Audit trail**: *Every money movement must be traceable*
- **Reconciliation**: If account balance is wrong, ledger shows exact history
- **Mistake correction**: Don't delete/edit wrong entry - add reversal entry instead

**Example ledger entries for investment order:**

```sql
-- User invests €500
INSERT INTO ledger (user_id, account_id, entry_type, amount_cents, description, reference_type, reference_id)
VALUES
    (123, 456, 'investment_debit', -50000, 'Investment order #789', 'order', 789),
    (123, 456, 'fee_debit', -150, 'Platform fee for order #789', 'order', 789);

-- Later, dividend received
INSERT INTO ledger (user_id, account_id, entry_type, amount_cents, description, reference_type, reference_id)
VALUES
    (123, 456, 'dividend_credit', 1250, 'Dividend from VUSA.L - Q1 2026', 'holding', 101);

```

---

## 6. Subscriptions & Payments

### **subscriptions** table

```sql
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Plan details
    plan_type VARCHAR(50) NOT NULL,  -- free, premium, family
    billing_cycle VARCHAR(20),  -- monthly, yearly

    -- Pricing
    price_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Status
    status VARCHAR(20) DEFAULT 'active',  -- active, cancelled, past_due, suspended

    -- Stripe integration
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    stripe_payment_method_id VARCHAR(255),

    -- Billing dates
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    next_billing_date DATE,

    -- Cancellation
    cancel_at_period_end BOOLEAN DEFAULT false,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,

    -- Trial
    trial_start DATE,
    trial_end DATE,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_next_billing_date (next_billing_date),
    INDEX idx_stripe_customer (stripe_customer_id)
);

```

---

### **payments** table

```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id BIGINT REFERENCES subscriptions(id) ON DELETE SET NULL,

    -- Payment details
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Status
    status VARCHAR(20) NOT NULL,  -- pending, succeeded, failed, refunded

    -- Stripe
    stripe_payment_intent_id VARCHAR(255),
    stripe_charge_id VARCHAR(255),
    stripe_invoice_id VARCHAR(255),

    -- Payment method
    payment_method_type VARCHAR(50),  -- card, sepa_debit, bank_transfer
    payment_method_last4 VARCHAR(4),  -- Last 4 digits of card

    -- Failure handling
    failure_code VARCHAR(100),
    failure_message TEXT,

    -- Refund
    refunded_at TIMESTAMP,
    refund_amount_cents BIGINT,
    refund_reason TEXT,

    -- Dates
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_status (status),
    INDEX idx_stripe_payment_intent (stripe_payment_intent_id)
);

```

---

## 7. Education & Content

### **courses** table

```sql
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,

    -- Course info
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,  -- URL-friendly: "investing-101"
    description TEXT,
    thumbnail_url VARCHAR(500),

    -- Content
    difficulty_level VARCHAR(20),  -- beginner, intermediate, advanced
    estimated_duration_minutes INT,

    -- Access control
    is_free BOOLEAN DEFAULT true,
    required_plan VARCHAR(50),  -- free, premium (null = available to all)

    -- Ordering
    display_order INT DEFAULT 0,

    -- Status
    status VARCHAR(20) DEFAULT 'draft',  -- draft, published, archived
    published_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_slug (slug),
    INDEX idx_status (status),
    INDEX idx_display_order (display_order)
);

```

---

### **lessons** table

```sql
CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    -- Lesson info
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,

    -- Content type
    content_type VARCHAR(50),  -- video, article, quiz, interactive

    -- For video lessons
    video_url VARCHAR(500),
    video_duration_seconds INT,
    video_thumbnail_url VARCHAR(500),

    -- For article lessons (stored in MongoDB for rich text)
    article_mongo_id VARCHAR(50),  -- Reference to MongoDB document

    -- For quiz lessons
    quiz_mongo_id VARCHAR(50),  -- Reference to MongoDB quiz document

    -- Ordering
    display_order INT DEFAULT 0,

    -- Prerequisites
    requires_lesson_id BIGINT REFERENCES lessons(id),  -- Must complete this lesson first

    -- Estimated time
    estimated_minutes INT,

    status VARCHAR(20) DEFAULT 'published',

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_course_id (course_id),
    INDEX idx_display_order (display_order),
    INDEX idx_status (status),

    UNIQUE (course_id, slug)
);

```

**Why some content in MongoDB instead of PostgreSQL?**

- **Rich text articles**: Complex formatting (bold, italic, images, code blocks)
    - PostgreSQL can store as TEXT, but querying/rendering is harder
    - MongoDB stores as structured document with formatting metadata
- **Quiz questions**: Variable number of questions/answers
    - Flexible schema: multiple choice, true/false, fill-in-blank
    - Easier to add new question types without schema migration

**Hybrid approach:**

- **PostgreSQL**: Metadata (title, duration, ordering, relationships)
- **MongoDB**: Content (article body, quiz questions)
- **Why**: Best of both worlds - relational structure + flexible content

---

### **user_course_progress** table

```sql
CREATE TABLE user_course_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    -- Progress tracking
    completed_lessons_count INT DEFAULT 0,
    total_lessons_count INT,
    progress_percent INT DEFAULT 0,  -- Calculated: (completed / total) * 100

    -- Status
    status VARCHAR(20) DEFAULT 'in_progress',  -- not_started, in_progress, completed

    -- Timing
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT NOW(),

    -- Current position
    current_lesson_id BIGINT REFERENCES lessons(id),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_course_id (course_id),
    INDEX idx_status (status),

    -- One progress record per user per course
    UNIQUE (user_id, course_id)
);

```

**How progress is updated:**

```sql
-- When user completes a lesson, trigger updates course progress

CREATE OR REPLACE FUNCTION update_course_progress()
RETURNS TRIGGER AS $$
DECLARE
    total_lessons INT;
    completed_lessons INT;
BEGIN
    -- Count total lessons in course
    SELECT COUNT(*) INTO total_lessons
    FROM lessons
    WHERE course_id = (
        SELECT course_id FROM lessons WHERE id = NEW.lesson_id
    );

    -- Count completed lessons for this user
    SELECT COUNT(*) INTO completed_lessons
    FROM user_lesson_progress
    WHERE user_id = NEW.user_id
    AND lesson_id IN (
        SELECT id FROM lessons
        WHERE course_id = (SELECT course_id FROM lessons WHERE id = NEW.lesson_id)
    )
    AND status = 'completed';

    -- Update course progress
    UPDATE user_course_progress
    SET
        completed_lessons_count = completed_lessons,
        total_lessons_count = total_lessons,
        progress_percent = (completed_lessons::float / total_lessons * 100)::int,
        status = CASE
            WHEN completed_lessons = total_lessons THEN 'completed'
            ELSE 'in_progress'
        END,
        completed_at = CASE
            WHEN completed_lessons = total_lessons THEN NOW()
            ELSE NULL
        END,
        last_accessed_at = NOW()
    WHERE user_id = NEW.user_id
    AND course_id = (SELECT course_id FROM lessons WHERE id = NEW.lesson_id);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_course_progress
AFTER INSERT OR UPDATE ON user_lesson_progress
FOR EACH ROW
EXECUTE FUNCTION update_course_progress();

```

---

### **user_lesson_progress** table

```sql
CREATE TABLE user_lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    -- Progress for video lessons
    video_progress_seconds INT DEFAULT 0,
    video_completed_percent INT DEFAULT 0,

    -- Progress for article lessons
    scroll_position_percent INT DEFAULT 0,

    -- Quiz attempts
    quiz_attempts INT DEFAULT 0,
    quiz_score_percent INT,
    quiz_passed BOOLEAN DEFAULT false,

    -- Status
    status VARCHAR(20) DEFAULT 'in_progress',  -- in_progress, completed

    -- Timing
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT NOW(),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_lesson_id (lesson_id),
    INDEX idx_status (status),

    UNIQUE (user_id, lesson_id)
);

```

**Why track detailed progress?**

- **Resume feature**: User can pick up video where they left off
- **Completion criteria**:
    - Video: Watched 90%+
    - Article: Scrolled to bottom
    - Quiz: Passed with 70%+ score
- **Engagement analytics**: See which lessons users struggle with

---

## 8. Notifications

### **notification_preferences** table

```sql
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- Channel preferences
    email_enabled BOOLEAN DEFAULT true,
    push_enabled BOOLEAN DEFAULT true,
    sms_enabled BOOLEAN DEFAULT false,

    -- Notification types
    budget_alerts BOOLEAN DEFAULT true,
    investment_updates BOOLEAN DEFAULT true,
    goal_milestones BOOLEAN DEFAULT true,
    educational_content BOOLEAN DEFAULT true,
    marketing_emails BOOLEAN DEFAULT false,

    -- Frequency
    digest_frequency VARCHAR(20) DEFAULT 'weekly',  -- daily, weekly, monthly, never
    digest_day_of_week INT DEFAULT 1,  -- 1=Monday
    digest_time_of_day TIME DEFAULT '09:00',

    -- Quiet hours
    quiet_hours_enabled BOOLEAN DEFAULT false,
    quiet_hours_start TIME,
    quiet_hours_end TIME,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id)
);

```

**Why separate preferences table?**

- Users should control what notifications they receive
- GDPR compliance: Must respect user preferences
- Different channels (email, push, SMS) have different opt-in requirements
- Can query: "Which users want daily digest emails?"

---

### **notification_queue** table

```sql
CREATE TABLE notification_queue (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Notification details
    notification_type VARCHAR(50) NOT NULL,
    -- budget_exceeded, order_completed, goal_milestone, etc.

    channel VARCHAR(20) NOT NULL,  -- email, push, sms

    -- Priority
    priority INT DEFAULT 5,  -- 1=highest, 10=lowest

    -- Content
    subject VARCHAR(255),
    body TEXT,
    data JSONB,  -- Additional data for rendering template

    -- Template reference (for MongoDB templates)
    template_id VARCHAR(50),

    -- Status
    status VARCHAR(20) DEFAULT 'pending',  -- pending, sent, failed

    -- Delivery
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,

    -- Error handling
    error_message TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP,

    -- Tracking
    external_id VARCHAR(255),  -- SendGrid message ID, SNS message ID

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_channel (channel),
    INDEX idx_created_at (created_at),
    INDEX idx_next_retry_at (next_retry_at)
);

```

**Why store notifications in PostgreSQL before sending?**

- **Audit trail**: Know exactly what was sent to user and when
- **Retry logic**: If SendGrid/SNS fails, can retry from database
- **Analytics**: Track open rates, click rates (for improving content)
- **User history**: "Show me all notifications I received last month"
- **Debugging**: If user says "I didn't get notification", can verify in database

**Flow:**

1. Core API inserts notification into `notification_queue`
2. Publishes event to RabbitMQ
3. Notification Service picks up event
4. Checks user's `notification_preferences`
5. If user allows this type, adds to BullMQ for delivery
6. Worker sends via SendGrid/SNS
7. Updates `notification_queue` with delivery status

---

## 9. Audit & Compliance

### **audit_logs** table

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,

    -- Who
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    admin_id BIGINT REFERENCES users(id) ON DELETE SET NULL,  -- If action by admin

    -- What
    action VARCHAR(100) NOT NULL,
    -- user.login, user.logout, order.created, account.deleted, kyc.verified, etc.

    entity_type VARCHAR(50),  -- user, order, account, transaction
    entity_id BIGINT,

    -- How
    ip_address INET,
    user_agent TEXT,

    -- Details
    old_values JSONB,  -- State before action
    new_values JSONB,  -- State after action
    metadata JSONB,  -- Additional context

    -- When
    created_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
);

-- Make audit logs immutable
CREATE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;

```

**Why immutable audit logs?**

- **Regulatory compliance**: Financial services must maintain tamper-proof audit trail
- **Security**: If system is compromised, attacker can't hide their tracks
- **Forensics**: Investigate issues by replaying exact sequence of events

**What gets audited:**

- All user authentication events (login, logout, password change)
- All financial transactions (orders, transfers, payments)
- All sensitive data access (viewing another user's data - admin only)
- All configuration changes (KYC approval, account suspension)

**Example audit log entries:**

```sql
-- User login
INSERT INTO audit_logs (user_id, action, ip_address, user_agent, metadata)
VALUES (123, 'user.login', '192.168.1.1', 'Mozilla/5.0...', '{"method": "password"}');

-- Order created
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_values)
VALUES (123, 'order.created', 'order', 789,
    '{"amount_cents": 50000, "portfolio_type": "growth"}');

-- Admin approves KYC
INSERT INTO audit_logs (user_id, admin_id, action, entity_type, entity_id, old_values, new_values)
VALUES (123, 456, 'kyc.verified', 'user', 123,
    '{"kyc_status": "pending"}',
    '{"kyc_status": "verified", "verified_at": "2026-01-19T10:30:00Z"}');

```

---

## 10. System Configuration

### **system_settings** table

```sql
CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,

    key VARCHAR(100) UNIQUE NOT NULL,
    value TEXT NOT NULL,
    value_type VARCHAR(20) DEFAULT 'string',  -- string, integer, boolean, json

    description TEXT,

    -- Access control
    is_public BOOLEAN DEFAULT false,  -- Can users see this setting?

    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_key (key)
);

```

**Example settings:**

```sql
INSERT INTO system_settings (key, value, value_type, description) VALUES
    ('platform_fee_percent', '0.3', 'decimal', 'Platform fee on investments'),
    ('max_investment_per_order_eur', '50000', 'integer', 'Maximum investment per order'),
    ('kyc_verification_enabled', 'true', 'boolean', 'Enable KYC verification'),
    ('maintenance_mode', 'false', 'boolean', 'System maintenance mode'),
    ('supported_countries', '["DE","FR","ES","IT","NL","BE","AT","PL"]', 'json', 'Supported countries');

```

**Why database instead of config file?**

- **Dynamic updates**: Change settings without redeploying code
- **Audit trail**: Track who changed what setting and when
- **Per-environment**: Different settings for dev/staging/prod in same codebase
- **Runtime access**: Application can query current settings

---

## 11. Market Data Cache (PostgreSQL)

### **market_data** table

```sql
CREATE TABLE market_data (
    id BIGSERIAL PRIMARY KEY,

    symbol VARCHAR(20) NOT NULL,
    asset_type VARCHAR(20),  -- etf, stock, bond

    -- Pricing
    current_price_cents BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',

    -- Change tracking
    previous_close_cents BIGINT,
    change_cents BIGINT,
    change_percent DECIMAL(10, 4),

    -- Volume
    volume BIGINT,

    -- Metadata
    market_status VARCHAR(20),  -- open, closed, pre_market, after_hours
    last_trade_at TIMESTAMP,

    -- Update tracking
    data_source VARCHAR(50),  -- alphavantage, yahoo_finance, etc.
    updated_at TIMESTAMP DEFAULT NOW(),

    INDEX idx_symbol (symbol),
    INDEX idx_updated_at (updated_at),

    UNIQUE (symbol)
);

```

**Why in PostgreSQL instead of only Redis?**

- **Redis**: Fast lookups during trading hours (sub-millisecond)
- **PostgreSQL**: Persistent storage, historical tracking
- **Pattern**:
    1. Background job fetches prices every 15 minutes
    2. Writes to PostgreSQL
    3. Writes to Redis with TTL
    4. Application reads from Redis (fast)
    5. If Redis miss, fallback to PostgreSQL

**Update strategy:**

```sql
-- Background job runs every 15 minutes
UPDATE market_data
SET
    current_price_cents = 8550,  -- €85.50
    previous_close_cents = 8520,
    change_cents = 30,
    change_percent = ((8550 - 8520)::float / 8520 * 100),
    updated_at = NOW()
WHERE symbol = 'VUSA.L';

-- Also write to Redis
-- Redis SET "etf:price:VUSA.L" 8550 EX 900 (15 min TTL)

```