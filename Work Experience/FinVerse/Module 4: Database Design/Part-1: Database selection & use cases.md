## Database Strategy Overview

We use **two databases** for different purposes:

1. **PostgreSQL (Primary)**: Transactional data that needs ACID guarantees, relationships, consistency
2. **MongoDB**: Flexible schema data, high-write analytics, content management

---

## Why PostgreSQL for Core Data?

**Decision criteria:**

- Financial data needs **strong consistency** (account balances must be exact, not "eventually consistent")
- **ACID transactions** required (when executing investment order: debit account + create order + update ledger must all succeed or all fail)
- **Relational data**: Users have accounts, accounts have transactions, transactions belong to budgets - natural relationships
- **Regulatory compliance**: Audit logs need immutable, traceable records (SQL is standard for audits)
- **Data integrity**: Foreign keys prevent orphaned records (can't have order without user)

**What goes in PostgreSQL:**

- Users, authentication, profiles
- Accounts, balances, bank connections
- Transactions, budgets, goals
- Investment orders, portfolios, holdings
- Subscriptions, payments
- Audit logs

---

## Why MongoDB for Secondary Data?

**Decision criteria:**

- **Flexible schema**: Analytics events evolve (adding new properties shouldn't require migrations)
- **High write volume**: Tracking every user action (clicks, page views) = 1000s of writes/sec
- **Nested documents**: Educational content (courses with nested lessons, videos) fits document model
- **Read-heavy aggregations**: Analytics queries (group by date, filter by action type)

**What goes in MongoDB:**

- User activity logs (clicks, views, actions)
- Educational content (courses, lessons, videos)
- Notification templates
- Analytics events

---

# 🧠Schema Design- Deep Dive!

look at individual files postgres schema design, mongodb schema design, redis schema design in this same directory.

---

# 🤝Database Relationships & Data Flow

## PostgreSQL ↔ MongoDB References

**How they connect:**

1. **PostgreSQL as source of truth for core data**
    - Users, accounts, transactions, orders = PostgreSQL
    - MongoDB references PostgreSQL IDs
2. **MongoDB stores related flexible data**
    - Activity logs reference `userId` from PostgreSQL
    - Articles reference `lessonId` from PostgreSQL
    - Quiz attempts reference `userId` and `lessonId`

**Example data flow:**

```jsx
User completes lesson:

1. PostgreSQL:
   UPDATE user_lesson_progress
   SET status = 'completed'
   WHERE user_id = 123 AND lesson_id = 456

2. Trigger updates user_course_progress in PostgreSQL

3. Application publishes event to RabbitMQ

4. Analytics service writes to MongoDB:
   db.analytics_events.insertOne({
       eventName: "lesson_completed",
       userId: 123,
       properties: { lessonId: 456, ... }
   })

5. Activity logs in MongoDB:
   db.activity_logs.insertOne({
       userId: 123,
       action: "lesson_completed",
       data: { lessonId: 456 }
   })

```

---

## Data Consistency Strategy

### **PostgreSQL (Strong Consistency)**

- **Use for**: Money, orders, accounts, transactions
- **Why**: ACID guarantees, no data loss, exact balances
- **Pattern**: Write to PostgreSQL first, then publish event

### **MongoDB (Eventual Consistency)**

- **Use for**: Logs, analytics, content
- **Why**: High write throughput, flexible schema
- **Pattern**: Can tolerate slight delays/missing records

**Example: Investment Order**

```jsx
Strong consistency needed:

1. PostgreSQL transaction:
   BEGIN;
   UPDATE accounts SET balance = balance - 50000 WHERE id = 456;
   INSERT INTO orders (user_id, amount, status) VALUES (123, 50000, 'pending');
   INSERT INTO ledger (user_id, entry_type, amount) VALUES (123, 'investment_debit', -50000);
   COMMIT;

   If ANY step fails, ALL steps rollback (atomicity)

2. After PostgreSQL commit succeeds, write to MongoDB:
   db.analytics_events.insertOne({...})
   db.activity_logs.insertOne({...})

   If MongoDB write fails, it's okay - we can retry or lose this log
   The money movement is already recorded in PostgreSQL

```

---

# 🦺Backup Strategy

## **PostgreSQL Backups**

```
AWS RDS Automated Backups:
- Daily snapshot at 3 AM
- Retention: 7 days
- Point-in-time recovery: Within 5 minutes of any time in last 7 days

Manual snapshots before deployments:
- Before schema migrations
- Retention: 30 days

```

## **MongoDB Backups**

```
MongoDB Atlas Automated Backups:
- Continuous backup (every 6 hours)
- Retention: 7 days
- Point-in-time recovery available

Manual exports:
- Export analytics_events monthly to S3 (for long-term storage)
- Keep 2 years of historical analytics

```

---

# 📒Database Indexes Summary

## **PostgreSQL Critical Indexes**

```sql
-- Users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_kyc_status ON users(kyc_status);

-- Accounts
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_type ON accounts(account_type);

-- Transactions
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_category ON transactions(category);

-- Orders
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_portfolio_id ON orders(portfolio_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- Portfolios
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);

-- Holdings
CREATE INDEX idx_holdings_portfolio_id ON holdings(portfolio_id);
CREATE INDEX idx_holdings_symbol ON holdings(symbol);

-- Ledger (query by user, date range)
CREATE INDEX idx_ledger_user_id ON ledger(user_id);
CREATE INDEX idx_ledger_created_at ON ledger(created_at);

-- Composite indexes for common queries
CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

```

## **MongoDB Indexes**

```jsx
// Activity logs (query by user and time)
db.activity_logs.createIndex({ userId: 1, timestamp: -1 });
db.activity_logs.createIndex({ action: 1, timestamp: -1 });
db.activity_logs.createIndex({ timestamp: -1 });

// Analytics events
db.analytics_events.createIndex({ userId: 1, timestamp: -1 });
db.analytics_events.createIndex({ eventName: 1, timestamp: -1 });
db.analytics_events.createIndex({ sessionId: 1 });

// Articles
db.articles.createIndex({ lessonId: 1 });
db.articles.createIndex({ status: 1, publishedAt: -1 });

// Quizzes
db.quizzes.createIndex({ lessonId: 1 });

// Quiz attempts
db.quiz_attempts.createIndex({ userId: 1, quizId: 1 });
db.quiz_attempts.createIndex({ lessonId: 1, completedAt: -1 });

// Notification templates
db.notification_templates.createIndex({ templateId: 1 }, { unique: true });

// TTL indexes (auto-delete old data)
db.activity_logs.createIndex(
    { timestamp: 1 },
    { expireAfterSeconds: 7776000 }  // 90 days
);
db.analytics_events.createIndex(
    { timestamp: 1 },
    { expireAfterSeconds: 63072000 }  // 2 years
);

```

---

## Query Performance Examples

### **Slow Query (Without Index)**

```sql
-- Find all transactions for user in January 2026
SELECT * FROM transactions
WHERE user_id = 123
AND transaction_date >= '2026-01-01'
AND transaction_date < '2026-02-01';

-- Without index: Scans entire transactions table (millions of rows)
-- Query time: 2-5 seconds

```

### **Fast Query (With Composite Index)**

```sql
-- Same query with index on (user_id, transaction_date)
-- Execution plan: Uses idx_transactions_user_date
-- Query time: 5-10 milliseconds (500x faster!)

```

---

## Database Size Estimates (Series A Scale)

### **PostgreSQL**

```
450,000 users:
- users: 450K rows × 1 KB = 450 MB
- accounts: 900K rows × 500 bytes = 450 MB (2 accounts per user avg)
- transactions: 50M rows × 500 bytes = 25 GB (daily bank syncs)
- orders: 2M rows × 1 KB = 2 GB
- holdings: 500K rows × 500 bytes = 250 MB
- ledger: 5M rows × 500 bytes = 2.5 GB
- Other tables: 2 GB

Total: ~32 GB data + 15 GB indexes = 47 GB

RDS storage: 500 GB allocated (room for growth)

```

### **MongoDB**

```
- activity_logs: 500M documents × 500 bytes = 250 GB (90 day retention)
- analytics_events: 200M documents × 1 KB = 200 GB (2 year retention)
- articles: 500 documents × 50 KB = 25 MB
- quizzes: 200 documents × 20 KB = 4 MB
- quiz_attempts: 1M documents × 2 KB = 2 GB
- notification_templates: 100 documents × 10 KB = 1 MB

Total: ~452 GB

MongoDB Atlas M10: 10 GB storage (need to archive old logs to S3)
Better: M30 with 50 GB + S3 archival for logs older than 30 days

```

---

## When to Query PostgreSQL vs MongoDB

### **Query PostgreSQL when:**

- Need exact, real-time financial data (account balance, order status)
- Need ACID transactions (updating multiple related tables)
- Need complex joins (user → account → transaction → budget)
- Need strong consistency (can't tolerate stale data)

**Examples:**

```sql
-- User's current portfolio value (must be exact)
SELECT SUM(quantity * current_price_cents)
FROM holdings
WHERE portfolio_id = 101;

-- Monthly spending by category (for budget comparison)
SELECT category, SUM(ABS(amount_cents)) as spent
FROM transactions
WHERE user_id = 123
AND transaction_date >= '2026-01-01'
AND amount_cents < 0
GROUP BY category;

```

### **Query MongoDB when:**

- Need flexible schema data (articles, logs)
- Need high read/write throughput (activity tracking)
- Can tolerate eventual consistency
- Need complex aggregations on nested documents

**Examples:**

```jsx
// User's most common actions (for personalization)
db.activity_logs.aggregate([
    { $match: { userId: 123 } },
    { $group: { _id: "$action", count: { $sum: 1 } } },
    { $sort: { count: -1 } },
    { $limit: 10 }
]);

// Article reading progress
db.articles.findOne({ lessonId: 456 });

```

## Database Decision Checklist

**When designing a new feature, ask:**

1. **Does it involve money?** → PostgreSQL (ACID required)
2. **Does schema change frequently?** → MongoDB (flexibility)
3. **Is it high-write volume?** → MongoDB (logs, events)
4. **Need complex relationships?** → PostgreSQL (joins)
5. **Need real-time accuracy?** → PostgreSQL (strong consistency)
6. **Is it user-generated content?** → MongoDB (rich documents)

---

This database design supports **450K users at Series A** and can scale to **1.5M+ users at Series B** with:

- PostgreSQL: Vertical scaling (larger RDS instance) + read replicas
- MongoDB: Horizontal scaling (sharding by userId) or archival to S3

The hybrid approach uses each database's strengths while maintaining data integrity and performance.