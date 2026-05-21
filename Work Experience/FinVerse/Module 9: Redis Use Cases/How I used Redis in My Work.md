# My Redis Usage Throughout 15 Months at FinVerse

## Redis Instances Found in My Work

### Month 2-3: Pagination with Redis Caching (First Redis Exposure)

**Story: Add Pagination to Transactions Endpoint**

- Added Redis caching for total transaction count
- Cache key: `transactions:count:${userId}`
- TTL: 1 hour (3600 seconds)

### Month 2-3: Budget Alert Notifications

- No direct Redis usage (used RabbitMQ/BullMQ which internally uses Redis)
- BullMQ job queue storage

### Month 4-6: Savings Goals Module

- Auto-save scheduling via BullMQ (which uses Redis internally)
- No direct Redis commands in my code

### Month 7: Cache Stampede Incident (Major Redis Learning)

- Dashboard caching with TTL jitter
- Cache key: `dashboard:user:${userId}`
- Added random jitter to prevent synchronized expiries

### Month 7-12: Budget Calculation Performance Optimization

- Smart caching strategy with per-budget granularity
- Cache key: `budget:spent:${budgetId}:${periodStart}`
- TTL: 1 hour
- Cache invalidation on transaction sync

### Month 7-12: Recurring Investments

- No direct Redis usage (BullMQ handles Redis internally)

### Month 13-15: Analytics Dashboard

- No direct Redis caching (data aggregated overnight in PostgreSQL)

## Deep Dive: My Direct Redis Usage

### 1. Transaction Count Caching (Month 2)

### 2. Dashboard Caching with Jitter (Month 7)

### 3. Smart Budget Caching (Month 7-12)

# 🙇How I Used Redis in My Work at FinVerse

## A Deep Dive into Caching Strategy, Implementation & Learning

---

## Table of Contents

1. Transaction Count Caching (Month 2) - First Redis Experience
2. Cache Stampede Incident & Resolution (Month 7) - Major Learning
3. Smart Budget Caching Strategy (Month 7-12) - Advanced Implementation

---

# ✨Story 1: Transaction Count Caching (Month 2-3)

**Context: The Pagination Feature**

## **Situation:**

When I was implementing pagination for the transactions endpoint, Sofia (senior engineer) suggested: "You should cache the total count - it doesn't change often, but you're querying it on every page request."

I had never used Redis before. I understood the concept theoretically (in-memory cache), but had no idea how to actually implement it in code.

## **Task:**

Add Redis caching for the total transaction count to avoid hitting PostgreSQL on every pagination request.

**My Knowledge Level:**

- ✅ Knew: Redis is fast, in-memory key-value store
- ❌ Didn't know: How to connect Redis in NestJS
- ❌ Didn't know: What commands to use (GET, SET, SETEX?)
- ❌ Didn't know: How to structure cache keys
- ❌ Didn't know: When to invalidate cache

---

## Action:

**Implementation Journey**

### Step 1: Understanding the Problem (Pair Programming with Sofia)

**Before caching:**

```
User requests page 1 (transactions 1-50)
  ↓
GET /transactions?page=1&limit=50
  ↓
Query 1: SELECT * FROM transactions WHERE user_id = 123
         ORDER BY date DESC LIMIT 50 OFFSET 0
  ↓
Query 2: SELECT COUNT(*) FROM transactions WHERE user_id = 123
         ↓ (This runs EVERY time - slow for users with 5000+ transactions)
  ↓
Return: {
  transactions: [...50 items],
  pagination: {
    page: 1,
    totalPages: 100,  ← Calculated from count
    totalCount: 5000  ← From COUNT(*) query
  }
}

```

**The problem Sofia explained:**
"The `COUNT(*)` query scans the entire transactions table for this user - 5000 rows. It takes 50-80ms. If the user navigates page 1 → 2 → 3, we run this query 3 times, wasting 150-240ms. The count doesn't change unless new transactions are synced (which happens once a day at 3 AM)."

### Step 2: Learning Redis Basics

Sofia showed me the Redis documentation for NestJS:

**Installing Redis client:**

```bash
npm install ioredis
npm install @types/ioredis --save-dev

```

**Setting up Redis module in NestJS:**

```tsx
// src/redis/redis.module.ts
import { Module } from '@nestjs/common';
import { Redis } from 'ioredis';

export const REDIS_CLIENT = 'REDIS_CLIENT';

@Module({
  providers: [
    {
      provide: REDIS_CLIENT,
      useFactory: () => {
        return new Redis({
          host: process.env.REDIS_HOST || 'localhost',
          port: parseInt(process.env.REDIS_PORT) || 6379,
          password: process.env.REDIS_PASSWORD,
          db: 0, // Database 0 (Redis has 16 databases by default)
        });
      },
    },
  ],
  exports: [REDIS_CLIENT],
})
export class RedisModule {}

```

Sofia explained:

- "ioredis is the most popular Redis client for Node.js"
- "We inject it as REDIS_CLIENT so any service can use it"
- "AWS ElastiCache gives us host, port, password - we put in .env"

### Step 3: My First Redis Implementation (Cache-Aside Pattern)

**What is Cache-Aside Pattern?**
Sofia drew this on the whiteboard:

```
┌─────────────────────────────────────────────────────────┐
│ Cache-Aside Pattern (Lazy Loading)                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Application Request                                     │
│       ↓                                                  │
│  1. Check Cache (Redis)                                  │
│       ↓                                                  │
│  ┌────────────────┐                                      │
│  │ Cache Hit?     │                                      │
│  └────┬───────┬───┘                                      │
│       │       │                                          │
│    YES│       │NO                                        │
│       │       │                                          │
│       ↓       ↓                                          │
│  Return    2. Query Database                             │
│  Cached       ↓                                          │
│  Value    Get Data from PostgreSQL                       │
│               ↓                                          │
│           3. Store in Cache                              │
│               ↓                                          │
│           4. Return Data                                 │
│                                                          │
└─────────────────────────────────────────────────────────┘

```

**Why it's called "Cache-Aside":**

- Application code manages cache (not automatic)
- Cache is "aside" from the database (separate path)
- Application decides when to read/write cache

**My implementation:**

```tsx
// src/transactions/transactions.service.ts
import { Injectable, Inject } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Transaction } from './entities/transaction.entity';
import { Redis } from 'ioredis';
import { REDIS_CLIENT } from '../redis/redis.module';

@Injectable()
export class TransactionsService {
  constructor(
    @InjectRepository(Transaction)
    private readonly transactionsRepo: Repository<Transaction>,

    @Inject(REDIS_CLIENT)
    private readonly redis: Redis, // Sofia: "This is your Redis client"
  ) {}

  async findPaginated(
    userId: number,
    page: number,
    limit: number,
  ) {
    const offset = (page - 1) * limit;

    // ==========================================
    // STEP 1: Try to get count from Redis cache
    // ==========================================

    // Sofia: "Make descriptive cache keys with namespaces"
    const cacheKey = `transactions:count:${userId}`;

    // Sofia: "GET returns string or null"
    let totalCount = await this.redis.get(cacheKey);

    console.log(`Cache lookup for key: ${cacheKey}`);

    if (totalCount) {
      // ==========================================
      // CACHE HIT - Found in Redis
      // ==========================================
      console.log(`✅ Cache HIT - Count: ${totalCount}`);

      // Sofia: "Redis stores everything as strings, convert to number"
      totalCount = parseInt(totalCount, 10);

    } else {
      // ==========================================
      // CACHE MISS - Query PostgreSQL
      // ==========================================
      console.log(`❌ Cache MISS - Querying database`);

      // Query PostgreSQL for count (slow operation)
      const { count } = await this.transactionsRepo
        .createQueryBuilder('transaction')
        .where('transaction.user_id = :userId', { userId })
        .getCount();

      totalCount = count;

      console.log(`Database returned count: ${totalCount}`);

      // ==========================================
      // STEP 2: Store in Redis for next time
      // ==========================================

      // Sofia: "SETEX = SET with EXpiry in seconds"
      // TTL = Time To Live = 3600 seconds = 1 hour
      await this.redis.setex(
        cacheKey,           // Key
        3600,               // TTL (seconds)
        totalCount.toString() // Value (must be string)
      );

      console.log(`Cached count for 1 hour`);
    }

    // ==========================================
    // Fetch actual transactions (not cached)
    // ==========================================
    const transactions = await this.transactionsRepo
      .createQueryBuilder('transaction')
      .where('transaction.user_id = :userId', { userId })
      .orderBy('transaction.transaction_date', 'DESC')
      .limit(limit)
      .offset(offset)
      .getMany();

    return {
      transactions,
      pagination: {
        page,
        limit,
        totalPages: Math.ceil(totalCount / limit),
        totalCount,
      },
    };
  }
}

```

**What I learned from this code:**

1. **Redis GET returns string or null:**

    ```tsx
    let totalCount = await this.redis.get(cacheKey);
    // Returns: "5000" (string) or null (not found)
    
    ```

2. **Redis stores everything as strings:**

    ```tsx
    // Storing number:
    await this.redis.setex(key, 3600, totalCount.toString());
    
    // Reading number:
    const count = parseInt(await this.redis.get(key), 10);
    
    ```

3. **SETEX vs SET:**
   Sofia explained: "Always use SETEX (SET with EXpiry). If you use plain SET, the key lives forever and Redis runs out of memory."

    ```tsx
    // ❌ BAD - No expiry, memory leak
    await this.redis.set(key, value);
    
    // ✅ GOOD - Auto-expires after 1 hour
    await this.redis.setex(key, 3600, value);
    
    ```


### Step 4: Testing My Implementation

**Test 1: First request (Cache MISS)**

```
GET /transactions?page=1&limit=50

Logs:
Cache lookup for key: transactions:count:123
❌ Cache MISS - Querying database
Database returned count: 5000
Cached count for 1 hour

Response time: 95ms

```

**Test 2: Second request (Cache HIT)**

```
GET /transactions?page=2&limit=50

Logs:
Cache lookup for key: transactions:count:123
✅ Cache HIT - Count: 5000

Response time: 18ms  ← 5x faster!

```

**Test 3: After 1 hour (Cache expired)**

```
GET /transactions?page=1&limit=50

Logs:
Cache lookup for key: transactions:count:123
❌ Cache MISS - Querying database
Database returned count: 5000
Cached count for 1 hour

Response time: 92ms  ← Cache expired, fetch from DB again

```

### Step 5: The Cache Invalidation Problem

**Situation I didn't anticipate:**

Day 1, 10:00 AM: User has 5000 transactions
↓ Cache stores: transactions:count:123 = "5000" (expires 11:00 AM)

Day 1, 10:30 AM: Bank sync runs, adds 150 new transactions
↓ PostgreSQL now has 5150 transactions
↓ But cache still says 5000!

Day 1, 10:35 AM: User loads transactions
↓ Cache HIT - returns 5000 (STALE DATA!)
↓ User sees: "Page 1 of 100" (should be "Page 1 of 103")

I showed this to Sofia. She said: "This is the classic cache invalidation problem. You need to invalidate the cache when data changes."

**Solution - Cache Invalidation:**

```tsx
// src/accounts/accounts.service.ts
async syncBankAccount(userId: number, accountId: number) {
  // ... fetch transactions from Plaid ...

  // Insert new transactions into PostgreSQL
  await this.transactionsRepo.insert(newTransactions);

  // ==========================================
  // INVALIDATE CACHE (so next read fetches fresh count)
  // ==========================================

  const cacheKey = `transactions:count:${userId}`;
  await this.redis.del(cacheKey);

  console.log(`Invalidated transaction count cache for user ${userId}`);

  // Next time user loads transactions, cache will be MISS,
  // query PostgreSQL, get fresh count (5150), cache it again
}

```

**Cache invalidation flow:**

```
Bank Sync Completes
  ↓
Delete cache key: transactions:count:123
  ↓
Cache is now empty for this user
  ↓
Next read will be CACHE MISS
  ↓
Query PostgreSQL (gets fresh count: 5150)
  ↓
Cache the fresh value
  ↓
User sees correct pagination!

```

---

### Visual Diagram: Cache-Aside Pattern I Implemented

```
┌──────────────────────────────────────────────────────────────┐
│                                                               │
│                    USER REQUEST                               │
│           GET /transactions?page=1&limit=50                   │
│                                                               │
└───────────────────────┬──────────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────┐
        │   TransactionsService             │
        │   findPaginated(userId, page)     │
        └───────────────┬───────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────────────────┐
        │ Step 1: Build cache key                        │
        │ cacheKey = `transactions:count:${userId}`      │
        └───────────────┬───────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────────────────┐
        │ Step 2: Try Redis GET                          │
        │ const count = await redis.get(cacheKey)        │
        └───────────────┬───────────────────────────────┘
                        │
                        │
        ┌───────────────┴────────────────┐
        │                                 │
        ▼                                 ▼
  ┌─────────────┐                 ┌──────────────┐
  │ count exists│                 │ count is null│
  │ (CACHE HIT) │                 │ (CACHE MISS) │
  └──────┬──────┘                 └──────┬───────┘
         │                                │
         │                                ▼
         │                  ┌────────────────────────────────┐
         │                  │ Step 3: Query PostgreSQL        │
         │                  │ SELECT COUNT(*) FROM transactions│
         │                  │ WHERE user_id = 123             │
         │                  └──────────┬─────────────────────┘
         │                             │
         │                             ▼
         │                  ┌────────────────────────────────┐
         │                  │ Step 4: Store in Redis          │
         │                  │ redis.setex(                    │
         │                  │   cacheKey,                     │
         │                  │   3600,        // 1 hour TTL    │
         │                  │   count        // value         │
         │                  │ )                               │
         │                  └──────────┬─────────────────────┘
         │                             │
         └──────────────┬──────────────┘
                        │
                        ▼
        ┌───────────────────────────────────────────────┐
        │ Step 5: Use count for pagination               │
        │ totalPages = Math.ceil(count / limit)          │
        └───────────────┬───────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────────────────────┐
        │ Step 6: Fetch actual transactions              │
        │ SELECT * FROM transactions                     │
        │ WHERE user_id = 123                            │
        │ ORDER BY date DESC                             │
        │ LIMIT 50 OFFSET 0                              │
        └───────────────┬───────────────────────────────┘
                        │
                        ▼
                ┌───────────────┐
                │ Return Response│
                └───────────────┘

```

---

## What I Learned (Month 2-3)

### Technical Concepts:

1. **Cache-Aside Pattern:**
    - Application manages cache explicitly
    - Check cache → If miss, query DB → Store in cache → Return data
    - Most common caching pattern
2. **Redis Commands:**
    - `GET key` - Retrieve value (returns string or null)
    - `SETEX key seconds value` - Store with expiry
    - `DEL key` - Delete (for invalidation)
3. **Cache Key Design:**
    - Use namespaces: `transactions:count:${userId}`
    - Not just: `${userId}` (confusing, hard to debug)
    - Pattern: `entity:operation:identifier`
4. **TTL (Time To Live):**
    - Always set expiry to prevent memory leaks
    - Balance: Too short = many cache misses, Too long = stale data
    - 1 hour worked for transaction counts (balance between freshness and performance)
5. **Cache Invalidation:**
    - "There are only two hard things in Computer Science: cache invalidation and naming things" - Phil Karlton
    - Delete cache when underlying data changes
    - Otherwise users see stale data

### Mistakes I Made:

1. **Forgot to invalidate cache initially:**
    - Bank sync added transactions, but cache still had old count
    - Sofia caught this in code review
2. **Didn't convert Redis string to number:**
    - First attempt: `totalCount = await this.redis.get(cacheKey);`
    - Bug: `Math.ceil("5000" / 50)` = NaN
    - Fix: `totalCount = parseInt(await this.redis.get(cacheKey), 10);`
3. **Used plain SET instead of SETEX initially:**
    - Sofia: "Keys without expiry are memory leaks waiting to happen"
    - Changed all to SETEX with appropriate TTL

### Performance Impact:

**Before caching:**

- Every pagination request: 2 PostgreSQL queries
- COUNT(*) query: 50-80ms for users with 5000+ transactions
- Total API response time: 95-120ms

**After caching:**

- First request (cache miss): 95ms (same, has to query DB)
- Subsequent requests (cache hit): 18ms (5x faster!)
- Cache hit rate: ~85% (most users navigate multiple pages)

---

## 🛡️Interview Preparation: Questions I Should Be Ready For

### Q1: "Walk me through how you implemented caching in the pagination feature."

**My Answer:**

"I implemented the Cache-Aside pattern for transaction count caching. When a user requests paginated transactions, my service first checks Redis using a key pattern like `transactions:count:${userId}`.

If it's a cache hit, I return the cached count immediately - about 2ms. If it's a miss, I query PostgreSQL with `SELECT COUNT(*)`, which takes 50-80ms for users with thousands of transactions. After getting the count from the database, I store it in Redis with `SETEX`, setting a 1-hour TTL.

The key insight was realizing that transaction counts don't change frequently - only when bank accounts sync, which happens once daily at 3 AM. So caching for an hour is a good balance between freshness and performance.

For cache invalidation, I added a `redis.del()` call in the bank sync completion handler, so when new transactions are imported, the cached count is cleared and the next request fetches fresh data."

### Q2: "Why did you choose Cache-Aside over other caching patterns?"

**My Answer:**

"Cache-Aside was the natural choice for this use case because:

1. **Read-heavy workload**: Transaction counts are read frequently (every pagination request) but written rarely (once daily during sync).
2. **Application control**: I needed explicit control over when to invalidate the cache - specifically after bank syncs complete. Cache-Aside gives me this control.
3. **Failure resilience**: If Redis goes down, the application still works - it just falls back to querying PostgreSQL. With patterns like Read-Through or Write-Through, if the cache fails, the whole request fails.
4. **Simplicity**: Cache-Aside is the simplest pattern to implement and reason about. As a junior engineer, this was important for me to understand and debug.

Alternative patterns I considered:

- **Read-Through**: Cache automatically loads from database on miss. But I wanted explicit control over invalidation.
- **Write-Through**: Every write goes through cache. Overkill for data that changes once a day.
- **Write-Behind**: Asynchronous writes to database. Too complex and risky for financial data.

Cache-Aside matched our read-heavy, infrequent-write pattern perfectly."

### Q3: "How did you decide on the 1-hour TTL?"

**My Answer:**

"The 1-hour TTL was a deliberate trade-off between data freshness and cache effectiveness.

Here's my reasoning:

1. **Data change frequency**: Transaction counts only change during bank sync (3 AM daily). Between syncs, the count is static.
2. **Acceptable staleness**: Even if cache invalidation fails for some reason, 1-hour-old data is acceptable for pagination. User might see 'Page 1 of 100' when it's actually 'Page 1 of 103' - not a critical issue.
3. **Cache hit rate**: With 1-hour TTL, most users who navigate multiple pages hit the cache. Shorter TTL (e.g., 5 minutes) would cause more cache misses without meaningful freshness improvement.
4. **Memory efficiency**: Longer TTLs (e.g., 24 hours) would keep more keys in Redis, but cache invalidation handles freshness, so extended TTL doesn't add value.

I also considered user behavior: most users load transactions, navigate 2-3 pages, and leave. This happens within minutes, so 1-hour TTL covers typical session length.

If I were optimizing further, I might profile actual user session durations and adjust TTL based on data - maybe 30 minutes or 2 hours - but 1 hour was a sensible starting point."

### Q4: "What happens if cache invalidation fails?"

**My Answer:**

"This is a great question because cache invalidation failures can cause stale data issues.

**Potential failure scenarios:**

1. **Redis is down during invalidation**:

    ```tsx
    try {
      await this.redis.del(cacheKey);
    } catch (error) {
      console.error('Failed to invalidate cache:', error);
      // ⚠️ Cache not invalidated, but sync continues
    }
    
    ```

   **Impact**: User sees old count until TTL expires (max 1 hour).

   **Mitigation**: TTL acts as a safety net. Even if invalidation fails, cache expires naturally.

2. **Application crashes before invalidation**:
    - Bank sync completes, inserts transactions
    - Application crashes before `redis.del()` executes
    - Cache still has old count

   **Mitigation**: Same - TTL expires, cache refreshed on next request.

3. **Network partition during invalidation**:
    - Redis command times out
    - Unclear if deletion succeeded

   **Mitigation**: Wrap in try-catch, log error, rely on TTL.


**Why this is acceptable:**

This isn't financial transaction data (where staleness is unacceptable). It's pagination metadata. The worst case: user sees 'Page 1 of 100' instead of 'Page 1 of 103' for up to 1 hour. Not great, but not catastrophic.

If this were account balance or investment value, I would:

- Never cache (always query authoritative database)
- Or use much shorter TTL (30 seconds) with aggressive retries on invalidation

The TTL acts as an expiry circuit breaker - even if invalidation logic fails entirely, stale data has bounded lifetime."

---

# ✨Story 2: Cache Stampede Incident & Resolution (Month 7)

The Production Incident That Changed How I Think About Caching

---

STAR Format: Cache Stampede Incident

## Situation

**Monday morning, November 13, 2023, 09:30 CET**

I was in daily standup when PagerDuty alerts started flooding the #incidents Slack channel:

```
🚨 CRITICAL: API response time >5s
🚨 Redis connections exhausted
🚨 Users unable to load dashboard

```

Anna (my manager): "Prasenjit, you're on support rotation this week. Can you investigate?"

**My internal state**: Heart racing. First time being responsible for a production incident with real users affected.

**What was happening to users:**

- Dashboard taking 8-12 seconds to load (normally 150ms)
- Some users seeing timeout errors
- Mobile app appearing frozen

---

## Task

**My responsibility:**

1. Identify root cause of performance degradation
2. Implement fix to restore normal service
3. Prevent future occurrences
4. Write post-mortem documentation

---

## Action

### Phase 1: Panic & Initial Investigation (First 10 Minutes)

What I Did

**Checked DataDog Dashboard:**

```
┌──────────────────────────────────────────────┐
│ DataDog APM Metrics (09:27 - 09:35)          │
├──────────────────────────────────────────────┤
│                                               │
│ API Latency:                                  │
│   Normal: 150ms ━━━━━━━━━━━━━━━━━━━━━━━━━   │
│   Current: 8000ms ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲   │
│                                               │
│ Redis Connection Pool:                        │
│   Used: 50/50 (100% - MAXED OUT!)            │
│   Waiting: 150 requests queued                │
│                                               │
│ PostgreSQL:                                   │
│   Connections: 25/100 (Normal)                │
│   Query time: 120ms avg (Normal)              │
│                                               │
│ Error Logs (last 5 min):                      │
│   "TimeoutError: Redis connection timeout"    │
│   "Error: Too many clients" (x 1247)          │
│                                               │
└──────────────────────────────────────────────┘

```

**My observations:**

- ✅ PostgreSQL is fine (not a database issue)
- ❌ Redis is maxed out (50/50 connections used)
- ❌ API latency is 50x normal
- ❌ Many "Redis timeout" errors

**But I had NO idea what was causing this!**

I pinged Anna in Slack:

```
Me: "Anna, dashboard API is slow, Redis connections maxed.
     Not sure why! 😰"

Anna: "Check Redis monitoring. Are there patterns in keys
       being accessed?"

Me: "How do I check that?" (I didn't know Redis had monitoring!)

Anna: "Screen share, I'll show you."

```

---

### Phase 2: Root Cause Discovery (10-30 Minutes)

Anna Teaches Me Redis Monitoring

Anna screen-shared and showed me how to connect to Redis CLI:

```bash
# Connect to AWS ElastiCache Redis
redis-cli -h redis-finverse.cache.amazonaws.com -p 6379 --tls

# Check stats
redis-cli> INFO stats

# Watch real-time commands (VERY useful for debugging!)
redis-cli> MONITOR

```

**What the MONITOR command showed:**

```
1700741220.123456 [0 10.0.1.45:52341] "GET" "dashboard:user:123"
1700741220.134567 [0 10.0.1.46:52342] "GET" "dashboard:user:456"
1700741220.145678 [0 10.0.1.47:52343] "GET" "dashboard:user:789"
1700741220.156789 [0 10.0.1.48:52344] "GET" "dashboard:user:234"
... (hundreds per second!)

1700741220.234567 [0 10.0.1.45:52341] "GET" "dashboard:user:123"
1700741220.245678 [0 10.0.1.45:52341] "GET" "dashboard:user:123"
1700741220.256789 [0 10.0.1.45:52341] "GET" "dashboard:user:123"
... (same keys being requested repeatedly!)

```

Anna: "See this? Thousands of `GET dashboard:user:*` requests per second. This is a **cache stampede**."

**Me:** "What's a cache stampede?"

---

**Understanding Cache Stampede (Anna's Whiteboard Explanation)**

**Anna drew this timeline:**

```
Time: 09:22:00 AM
  ↓
  10,000 users load dashboard in morning rush
  ↓
  All caches are MISS (first load of the day)
  ↓
  All query PostgreSQL (slow: 500ms per query)
  ↓
  All store in Redis with SETEX:
    redis.setex('dashboard:user:123', 300, data)  ← 5 min TTL
    redis.setex('dashboard:user:456', 300, data)  ← 5 min TTL
    ... (all 10,000 with same TTL: 300 seconds)
  ↓
  All caches expire at EXACTLY THE SAME TIME!

Time: 09:27:00 AM (5 minutes later)
  ↓
  ALL 10,000 cache keys expire simultaneously
  ↓
  Users refresh dashboards
  ↓
  ALL 10,000 requests are cache MISS
  ↓
  ALL 10,000 hit PostgreSQL at once!
  ↓
  Database queries slow down (500ms → 2000ms under load)
  ↓
  While waiting for DB, more users refresh
  ↓
  Even MORE cache misses, MORE database queries
  ↓
  Redis connection pool exhausted (50 connections max)
  ↓
  New requests waiting for Redis connection
  ↓
  STAMPEDE! 🐘🐘🐘

```

**This is called "Cache Stampede" or "Thundering Herd":**

```
┌─────────────────────────────────────────────────────┐
│         Cache Stampede Visualization                 │
├─────────────────────────────────────────────────────┤
│                                                      │
│ Normal Operation:                                    │
│                                                      │
│   User 1 ──→ Cache HIT  ──→ Return (2ms)            │
│   User 2 ──→ Cache HIT  ──→ Return (2ms)            │
│   User 3 ──→ Cache HIT  ──→ Return (2ms)            │
│   ...                                                │
│   User 100 ──→ Cache HIT  ──→ Return (2ms)          │
│                                                      │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                      │
│ During Stampede (all caches expired at once):       │
│                                                      │
│   User 1 ──→ Cache MISS ──┐                         │
│   User 2 ──→ Cache MISS ──┤                         │
│   User 3 ──→ Cache MISS ──┤                         │
│   User 4 ──→ Cache MISS ──┤                         │
│   ...                      ├──→ ALL hit PostgreSQL  │
│   User 98 ──→ Cache MISS ──┤      at the same time! │
│   User 99 ──→ Cache MISS ──┤      (10,000 queries)  │
│   User 100 ──→ Cache MISS ─┘                        │
│                             ↓                        │
│                      Database overload               │
│                      Queries slow (500ms → 2000ms)   │
│                             ↓                        │
│                      Redis pool exhausted            │
│                             ↓                        │
│                      API latency spikes              │
│                             ↓                        │
│                      Users see timeouts              │
│                                                      │
└─────────────────────────────────────────────────────┘

```

---

Why Did This Happen?

**Anna pulled up my code (from Savings Goals feature deployed last week):**

```tsx
// My code - the culprit!
async getDashboard(userId: number) {
  const cacheKey = `dashboard:user:${userId}`;
  const cached = await this.redis.get(cacheKey);

  if (cached) {
    return JSON.parse(cached);
  }

  // Cache miss - fetch from database (SLOW: 500ms)
  const data = await this.fetchDashboardData(userId);

  // Store in cache for 5 minutes
  await this.redis.setex(
    cacheKey,
    300,  // ← THE PROBLEM: All users get same TTL!
    JSON.stringify(data)
  );

  return data;
}

```

**The bug:**

- Every user who loads dashboard between 09:22:00 and 09:22:59 gets cached data with TTL = 300 seconds
- All these caches expire at EXACTLY 09:27:00
- At 09:27:00, all 10,000 users experience cache miss simultaneously
- Thundering herd!

**Anna's diagnosis:**
"You need to add **jitter** to the TTL. Otherwise, synchronized cache expiries cause stampedes."

---

### Phase 3: The Fix - Adding TTL Jitter (30-60 Minutes)

**What is Jitter?**

**Anna explained:**

"Jitter means adding random variation to TTL so caches don't all expire at the same time."

```
┌─────────────────────────────────────────────────────┐
│ Without Jitter (Current - BROKEN):                   │
├─────────────────────────────────────────────────────┤
│                                                      │
│ User A: TTL = 300 seconds → Expires at 09:27:00     │
│ User B: TTL = 300 seconds → Expires at 09:27:00     │
│ User C: TTL = 300 seconds → Expires at 09:27:00     │
│ ...                                                  │
│ User Z: TTL = 300 seconds → Expires at 09:27:00     │
│                                                      │
│ All expire together! ▼▼▼ STAMPEDE                   │
│                                                      │
├─────────────────────────────────────────────────────┤
│ With Jitter (Fixed):                                 │
├─────────────────────────────────────────────────────┤
│                                                      │
│ User A: TTL = 290 seconds → Expires at 09:26:50     │
│ User B: TTL = 305 seconds → Expires at 09:27:05     │
│ User C: TTL = 287 seconds → Expires at 09:26:47     │
│ User D: TTL = 312 seconds → Expires at 09:27:12     │
│ User E: TTL = 294 seconds → Expires at 09:26:54     │
│ ...                                                  │
│ User Z: TTL = 318 seconds → Expires at 09:27:18     │
│                                                      │
│ Expiries spread over ~2 minutes! No stampede! ✓     │
│                                                      │
└─────────────────────────────────────────────────────┘

```

**My Implementation of Jitter**

**First attempt (wrong):**

```tsx
// ❌ I tried this first:
const ttl = 300 + Math.random() * 100;
// Problem: ttl is between 300-400 seconds
// Doesn't spread expiries both directions!

```

**Anna's correction:**

```tsx
// ✅ Correct: Add random offset in BOTH directions
const baseTTL = 300;  // 5 minutes
const jitter = Math.floor(Math.random() * 120) - 60;  // Random ±60 seconds
const ttl = baseTTL + jitter;  // TTL between 240-360 seconds (4-6 minutes)

await this.redis.setex(cacheKey, ttl, JSON.stringify(data));

```

**Why this works:**

- `Math.random()` returns 0.0 to 1.0
- `Math.random() * 120` gives 0 to 120
- `60` shifts range to -60 to +60
- `baseTTL + jitter` = 300 + (-60 to +60) = 240 to 360 seconds

**Visual distribution:**

```
Without jitter:
All expire at: ═══════════════════▼ 09:27:00

With jitter:
Expiries spread: ───────────────────────────────
                 ↑                             ↑
                 09:26:00                      09:28:00
                 (earliest)                    (latest)

Distribution looks like bell curve - gradual!

```

**My Final Implementation**

```tsx
// src/dashboard/dashboard.service.ts

async getDashboard(userId: number) {
  const cacheKey = `dashboard:user:${userId}`;

  // Step 1: Try cache
  const cached = await this.redis.get(cacheKey);
  if (cached) {
    return JSON.parse(cached);
  }

  // Step 2: Cache miss - fetch from database
  const data = await this.fetchDashboardData(userId);

  // Step 3: Calculate TTL with jitter
  const baseTTL = 300; // 5 minutes
  const jitterRange = 120; // ±60 seconds
  const jitter = Math.floor(Math.random() * jitterRange) - (jitterRange / 2);
  const ttl = baseTTL + jitter;

  // Log for debugging (removed after incident resolved)
  console.log(`Caching dashboard for user ${userId} with TTL ${ttl}s (jitter: ${jitter}s)`);

  // Step 4: Store with jittered TTL
  await this.redis.setex(cacheKey, ttl, JSON.stringify(data));

  return data;
}

```

**Emergency Deployment**

**10:10 AM - Created emergency PR:**

- Title: "HOTFIX: Add jitter to dashboard cache TTL to prevent stampede"
- Description: Explained the issue and solution
- Anna approved immediately (skipped normal review process)

**10:15 AM - Deployed to production:**

```bash
# Anna helped me deploy
git push origin hotfix/cache-jitter
# GitHub Actions CI/CD triggered
# Deployment to AWS Elastic Beanstalk (5 minutes)

```

**10:20 AM - Monitoring recovery:**

Watched DataDog dashboard nervously...

```
10:20 AM:
  API Latency: 7500ms (still bad)
  Redis connections: 48/50 (still high)

10:25 AM:
  API Latency: 3200ms (improving!)
  Redis connections: 35/50 (recovering)

10:30 AM:
  API Latency: 800ms (much better)
  Redis connections: 20/50 (good)

10:35 AM:
  API Latency: 150ms (NORMAL! 🎉)
  Redis connections: 15/50 (normal)

```

**10:45 AM - Incident resolved!**

Total duration: 1 hour 15 minutes (09:30 - 10:45)

---

### Phase 4: Post-Mortem & Learning (Next Day)

**Post-Mortem Meeting**

**Attendees:** Me, Anna, Marcus (Tech Lead), Dmitri, Sofia

**Timeline reconstruction:**

```
09:22 AM - Cache warming: 10,000 users loaded dashboard
           (normal morning traffic)

09:27 AM - Cache expiry: All 10,000 cache keys expired
           simultaneously (TTL = 300s)

09:27 AM - Thundering herd: All users hit database at once

09:28 AM - Redis pool exhausted: 50/50 connections used

09:30 AM - Alert triggered: PagerDuty notification sent

09:35 AM - Investigation started: I checked logs

09:50 AM - Root cause identified: Cache stampede

10:10 AM - Fix deployed: Added jitter to cache TTL

10:45 AM - Incident resolved: Metrics back to normal

```

**Root Cause Analysis**

**Primary cause:** Synchronized cache expiries due to fixed TTL

**Contributing factors:**

1. Dashboard query is expensive (500ms - joins 5 tables)
2. All cache keys set with same TTL (no jitter)
3. Redis connection pool too small (50 connections for 10K users)
4. No circuit breaker (kept retrying even when Redis full)

**What Went Well ✅**

- Monitoring detected issue within 3 minutes
- On-call engineer (me) responded quickly
- Escalated to seniors when stuck
- Fix deployed within 90 minutes
- No data loss or corruption

**What Could Be Better ❌**

- Cache stampede not caught in code review
- No load testing before deploying dashboard changes
- I panicked initially (froze for 10 minutes before asking for help)

---

**Action Items**

| Who | Action | Deadline |
| --- | --- | --- |
| Me (Prasenjit) | Write internal wiki doc: "Avoiding Cache Stampedes" | This week |
| Marcus | Add load testing to CI/CD for dashboard endpoint | Next sprint |
| DevOps (Klaus) | Increase Redis connection pool: 50 → 100 | Today |
| Sofia | Add circuit breaker pattern to Redis calls | Next sprint |
| Anna | Review all caching code for similar issues | This week |

---

### My Documentation (The Wiki Article I Wrote)

**Internal Wiki: "Avoiding Cache Stampedes in FinVerse"**

```markdown
# Cache Stampede Prevention Guide

## What is a Cache Stampede?

When many cache keys expire simultaneously, all requests hit the
database at once, causing:
- Database overload
- Slow response times
- Redis connection exhaustion

## Prevention Strategies

### 1. Add Jitter to Cache TTL ✅ (Implemented)

BAD:
```typescript
await redis.setex(key, 300, data); // All expire at same time

```

GOOD:

```tsx
const ttl = 300 + Math.floor(Math.random() * 120) - 60;
await redis.setex(key, ttl, data); // Staggered expiries

```

2. Cache Warming (Preemptive Refresh)

Refresh cache BEFORE it expires:

```tsx
const ttl = await redis.ttl(key);
if (ttl < 60 && ttl > 0) {
  // Less than 60 seconds left - refresh in background
  this.refreshCacheAsync(key);
  return staleData; // Return stale data while refreshing
}

```

3. Circuit Breaker

If Redis is down, fail fast:

```tsx
if (redisFailureCount > 10) {
  // Stop hitting Redis, return data from DB directly
  return await this.fetchFromDatabase();
}

```

Incident Response Checklist

If you see "Redis connection timeout" errors:

1. Check Redis connection pool usage (DataDog)
2. Check for synchronized cache expiries (Redis MONITOR)
3. Add jitter to cache TTL
4. Increase connection pool size (ask DevOps)
5. Write post-mortem (learn from incident)

## Result

**User Impact:**

- ~500 users experienced slow dashboards for 15 minutes
- No money lost, no data corruption
- Annoying but not critical

**Personal Growth:**

- ✅ First production incident handled successfully
- ✅ Learned cache stampede pattern (will never forget!)
- ✅ Got comfortable with incident response process
- ✅ Wrote documentation to help future engineers

**Team Recognition:**

Anna in 1-on-1:
"Prasenjit, you handled your first incident well. You froze
initially, which is normal. But you escalated when stuck - that's
the right move. Most importantly, you learned from it and
documented the solution. This is exactly what we want from
engineers."

Marcus in team meeting:
"The cache stampede doc Prasenjit wrote is excellent. Everyone
should read it. This is now our standard reference for caching
issues."

---

## 🛡️Interview Preparation: Cache Stampede Questions

### Q1: "Tell me about a production incident you resolved."

**My Answer:**

"In my 7th month, I caused a cache stampede incident that affected 500 users for 15 minutes. Here's what happened:

I had deployed a dashboard feature that cached user data with a fixed 5-minute TTL. During morning rush, 10,000 users loaded dashboards between 9:22-9:23 AM, creating 10,000 cache entries - all with TTL = 300 seconds.

At exactly 9:27 AM, all 10,000 caches expired simultaneously. When users refreshed, all hit the database at once. Each dashboard query takes 500ms and joins 5 tables. With 10,000 simultaneous queries, PostgreSQL slowed down, Redis connection pool maxed out (50/50), and API latency spiked from 150ms to 8 seconds.

I was on support rotation, saw the alerts, and investigated. Initially I panicked - didn't know what was wrong. After 10 minutes, I asked my manager for help. She taught me about Redis MONITOR command, which showed thousands of GET requests per second to the same keys.

She explained this was a 'cache stampede' - synchronized expiries causing thundering herd to the database. The fix was to add 'jitter' - random variation to TTL - so caches expire gradually, not all at once.

I implemented it by adding ±60 seconds random jitter to the 300-second TTL. Deployed within 40 minutes of identifying root cause. Metrics stabilized within 15 minutes of deployment.

The key learnings:

1. Always add jitter to cache TTLs to prevent synchronized expiries
2. When stuck during incidents, escalate immediately - don't waste time panicking
3. Write documentation so others learn from your mistakes

I wrote an internal wiki article on cache stampede prevention that became our team's standard reference. This incident taught me more about caching than any textbook could."

### Q2: "What is jitter and why is it important?"

**My Answer:**

"Jitter is adding random variation to cache TTLs to prevent synchronized expiries.

Without jitter, if 1000 users load data at the same time, all get TTL = 300 seconds, so all expire together 5 minutes later - causing a stampede.

With jitter, each user gets a slightly different TTL:

- User A: 290 seconds
- User B: 305 seconds
- User C: 312 seconds

Expiries are now spread over a 2-minute window instead of happening simultaneously.

**Implementation:**

typescript

```jsx
const baseTTL = 300;  // 5 minutes
const jitter = Math.floor(Math.random() * 120) - 60;  // ±60 seconds
const ttl = baseTTL + jitter;  // 240-360 seconds (4-6 minutes)
```

**Why ±60 seconds specifically?**

- Too small (±5s): Doesn't spread enough, still risk of mini-stampedes
- Too large (±300s): Cache freshness varies too much (some expire in 30s, others in 10min)
- ±60s (20% of base TTL): Good balance between spreading and consistency

**When is jitter critical?**

- High-traffic endpoints (thousands of users)
- Expensive database queries (500ms+)
- Synchronized user behavior (morning login rush, market open)

**When is jitter less critical?**

- Low-traffic endpoints (few concurrent users)
- Cheap queries (10ms)
- Already staggered access patterns (users load throughout the day)

The cache stampede incident taught me: jitter is a cheap insurance policy against thundering herd. It costs nothing (1 line of code) but prevents catastrophic load spikes."

### Q3: "How would you debug a cache stampede in production?"

**My Answer:**

"Here's my step-by-step debugging process, based on the incident I resolved:

**Step 1: Identify the symptoms**

- Check API latency metrics (sudden spike?)
- Check Redis connection pool (maxed out?)
- Check error logs (Redis timeout errors?)

**Step 2: Examine Redis behavior**

```bash
# Connect to Redis
redis-cli -h production-redis.aws.com

# Check current stats
redis-cli> INFO stats
# Look for: total_commands_processed, keyspace_hits, keyspace_misses

# Watch real-time commands (THIS IS KEY!)
redis-cli> MONITOR
# You'll see: thousands of GET commands per second
# Pattern recognition: same key prefixes being hammered
```

**Step 3: Identify the problematic keys**

- From MONITOR output, note which keys are getting hit heavily
- Example: `dashboard:user:*` getting thousands of GETs/second

**Step 4: Check TTL distribution**

```bash
# Sample a few keys
redis-cli> TTL dashboard:user:123
(integer) -2  # Key doesn't exist (expired!)

redis-cli> TTL dashboard:user:456
(integer) -2  # Also expired!

# If many keys expired simultaneously, this confirms stampede
```

**Step 5: Review code**

- Find where this cache key is set
- Check if TTL has jitter
- If fixed TTL = problem confirmed

**Step 6: Check database load**

- Look at PostgreSQL slow query log
- If same query executed thousands of times in short window = stampede

**Tools I'd use:**

- DataDog APM: Latency graphs, error rates
- Redis MONITOR: Real-time command visibility (most valuable!)
- PostgreSQL slow query log: Confirm database being hammered
- Application logs: Correlate timestamps

**Red flags that indicate stampede:**

- ✅ Sudden spike in API latency (10x normal)
- ✅ Redis connection pool exhausted
- ✅ Same cache keys being requested thousands of times
- ✅ Many cache keys have TTL = -2 (expired/doesn't exist)
- ✅ Database showing spike in identical queries

**Prevention checklist after fixing:**

1. Add jitter to all cache TTLs (not just the problematic one)
2. Load test to verify fix works under high traffic
3. Set up monitoring alerts for cache hit rate drops
4. Document the incident in team wiki

The key lesson: MONITOR command is your best friend. It shows you exactly what's happening in Redis in real-time, which is invaluable for debugging cache issues."

---

# ✨Story 3: Smart Budget Caching Strategy (Month 7-12)

**Advanced Caching: Per-Budget Granularity with Targeted Invalidation**

---

**STAR Format: Budget Calculation Performance Optimization**

## Situation

**December 2023 - Performance Complaint**

**Context:**
Users with many transactions (5,000+) experienced slow budget page loads: 8-12 seconds. Only ~200 power users affected, but they were our most engaged users - complaining loudly in support tickets.

Product team escalated: "This is hurting our most active users!"

**The Problem:**

When users loaded the budgets page, my code calculated spending for each budget category by querying transactions:

```tsx
// My original implementation (SLOW!)
async getBudgets(userId: number) {
  const budgets = await this.db.query(
    'SELECT * FROM budgets WHERE user_id = $1',
    [userId]
  );

  // For EACH budget, calculate current spending
  for (const budget of budgets) {
    const { sum } = await this.db.query(
      `SELECT SUM(ABS(amount_cents)) as sum
       FROM transactions
       WHERE user_id = $1
       AND category = $2
       AND transaction_date >= $3
       AND transaction_date <= $4
       AND amount_cents < 0`,  -- Only expenses
      [userId, budget.category, budget.period_start, budget.period_end]
    );

    budget.spent = sum || 0;
  }

  return budgets;
}

```

**N+1 Query Problem:**

- 1 query to get budgets (6 categories: food, transport, etc.)
- 6 queries to calculate spending (one per budget)
- **Total: 7 queries**
- Each transaction query scans 5,000 rows
- Takes 1.4 seconds per budget × 6 budgets = **8.4 seconds total!**

---

## Task

**Goal:** Reduce budget page load time from 8-12s to under 1s for users with large transaction histories.

**Constraints:**

- Must be accurate (can't show wrong spending)
- Cache invalidation must work correctly
- Should handle edge cases (new transactions added)

**My Plan (after profiling):**

1. Fix N+1 query with database optimization
2. Add smart caching with per-budget granularity
3. Implement targeted cache invalidation

---

## Action

### Phase 1: Database Optimization (Week 1)

**Step 1: Fixing N+1 Query**

**Dmitri's suggestion:** "Combine queries into one JOIN"

```tsx
// Attempt 1: Single query with JOIN
async getBudgets(userId: number) {
  const result = await this.db.query(`
    SELECT
      b.*,
      COALESCE(SUM(ABS(t.amount_cents)), 0) as spent_cents
    FROM budgets b
    LEFT JOIN transactions t ON (
      t.user_id = b.user_id
      AND t.category = b.category
      AND t.transaction_date >= b.period_start_date
      AND t.transaction_date <= b.period_end_date
      AND t.amount_cents < 0
    )
    WHERE b.user_id = $1
    GROUP BY b.id
  `, [userId]);

  return result.rows;
}

```

**Result:**

- Queries reduced: 7 → 1 ✅
- Response time: 8.4s → 7.5s ❌
- **Still slow!** Only 10% improvement

**Why still slow?**
Dmitri: "The JOIN is still scanning all 5,000 transactions. You need a database index."

---

**Step 2: Adding Database Index**

**Learning moment:** What is a database index?

Dmitri explained with an analogy:

```
┌────────────────────────────────────────────────────┐
│ Without Index (Full Table Scan):                   │
├────────────────────────────────────────────────────┤
│                                                     │
│ Like reading a 1000-page book to find "Chapter 5": │
│ - Read page 1: Not Chapter 5                       │
│ - Read page 2: Not Chapter 5                       │
│ - Read page 3: Not Chapter 5                       │
│ ... (scan all 1000 pages)                          │
│ - Read page 234: Found Chapter 5!                  │
│                                                     │
│ Slow! O(n) where n = all transactions              │
│                                                     │
├────────────────────────────────────────────────────┤
│ With Index (Direct Lookup):                        │
├────────────────────────────────────────────────────┤
│                                                     │
│ Like using book's index/table of contents:         │
│ - Look up "Chapter 5" in index: Page 234           │
│ - Jump directly to page 234                        │
│                                                     │
│ Fast! O(log n)                                     │
│                                                     │
└────────────────────────────────────────────────────┘

```

**Composite Index Design:**

```sql
-- Create index on columns used in WHERE clause
CREATE INDEX CONCURRENTLY idx_transactions_budget_calc
ON transactions(user_id, category, transaction_date)
WHERE amount_cents < 0;  -- Partial index (only expenses)

```

**Why this specific index?**

```
Index columns match query filters:
  ↓
WHERE user_id = 123          ← Indexed column 1
AND category = 'food'        ← Indexed column 2
AND transaction_date >= ...  ← Indexed column 3
AND amount_cents < 0         ← Partial index filter

```

**Key learnings about indexes:**

1. **Column order matters:**

    ```sql
    -- ✅ GOOD: Most selective column first
    (user_id, category, transaction_date)
    
    -- ❌ BAD: Less selective first
    (category, user_id, transaction_date)
    -- Why? category has 10 values, user_id has 450,000 values
    -- Index works left-to-right, so user_id narrows down more
    
    ```

2. **Partial indexes save space:**

    ```sql
    WHERE amount_cents < 0  -- Only index expenses (50% of transactions)
    
    -- Index size: 25M rows instead of 50M rows
    -- Faster lookups, less memory
    
    ```

3. **CONCURRENTLY prevents downtime:**

    ```sql
    CREATE INDEX CONCURRENTLY ...
    
    -- Builds index without locking table
    -- Production stays online during build
    -- Takes longer (45 minutes) but zero downtime
    
    ```


**Creating the migration:**

```tsx
// migrations/20231215_add_budget_calc_index.ts
import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddBudgetCalcIndex1702656000 implements MigrationInterface {

  public async up(queryRunner: QueryRunner): Promise<void> {
    // Build index without locking (safe for production)
    await queryRunner.query(`
      CREATE INDEX CONCURRENTLY idx_transactions_budget_calc
      ON transactions(user_id, category, transaction_date)
      WHERE amount_cents < 0
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    // Drop index if need to rollback
    await queryRunner.query(`
      DROP INDEX idx_transactions_budget_calc
    `);
  }
}

```

**Deployment:**

- Ran migration at 3 AM (low traffic)
- Took 45 minutes to build index on 50M transaction rows
- Zero downtime (CONCURRENTLY magic!)

**Result:**

- Response time: 7.5s → 1.2s ✅
- **83% improvement!**

But still not under 1 second target...

---

### Phase 2: Smart Caching Strategy (Week 2-3)

**Problem with My Previous Caching Approach**

**What I tried first (wrong!):**

```tsx
// ❌ BAD: Cache entire budget list
async getBudgets(userId: number) {
  const cacheKey = `budgets:user:${userId}`;
  const cached = await this.redis.get(cacheKey);

  if (cached) {
    return JSON.parse(cached);
  }

  const budgets = await this.calculateBudgets(userId);
  await this.redis.setex(cacheKey, 3600, JSON.stringify(budgets));

  return budgets;
}

```

**Why this was wrong:**

```
Day 1, 10:00 AM:
  User has 6 budgets (food, transport, entertainment, etc.)
  Cache key: budgets:user:123
  Cached value: {food: {spent: 450}, transport: {spent: 120}, ...}
  TTL: 1 hour

Day 1, 10:30 AM:
  Bank sync adds 1 new transaction: -€5 at Starbucks (category: food)
  PostgreSQL: food spent = €455 (updated)

  Invalidation strategy: Delete entire cache
  await redis.del('budgets:user:123');

  Result:
    ✅ Food budget cache invalidated (correct!)
    ❌ Transport cache also invalidated (unnecessary!)
    ❌ Entertainment cache also invalidated (unnecessary!)
    ❌ All 6 budgets require recalculation (wasteful!)

```

**Cache hit rate: only 15%** (invalidated too often)

---

**Solution: Per-Budget Caching (Granular Caching)**

**Anna's suggestion:** "Why cache all budgets together? Cache each budget separately!"

**Visual comparison:**

```
┌──────────────────────────────────────────────────────┐
│ Old Approach (Coarse-Grained):                        │
├──────────────────────────────────────────────────────┤
│                                                       │
│ Single cache key for all budgets:                    │
│   budgets:user:123 = {                                │
│     food: {spent: 450},                               │
│     transport: {spent: 120},                          │
│     entertainment: {spent: 80},                       │
│     shopping: {spent: 200},                           │
│     utilities: {spent: 150},                          │
│     health: {spent: 60}                               │
│   }                                                   │
│                                                       │
│ Problem: 1 transaction in "food" invalidates ALL     │
│                                                       │
├──────────────────────────────────────────────────────┤
│ New Approach (Fine-Grained):                          │
├──────────────────────────────────────────────────────┤
│                                                       │
│ Separate cache key per budget:                       │
│   budget:spent:101:2024-01 = 450    (food)           │
│   budget:spent:102:2024-01 = 120    (transport)      │
│   budget:spent:103:2024-01 = 80     (entertainment)  │
│   budget:spent:104:2024-01 = 200    (shopping)       │
│   budget:spent:105:2024-01 = 150    (utilities)      │
│   budget:spent:106:2024-01 = 60     (health)         │
│                                                       │
│ Benefit: 1 transaction in "food" only invalidates    │
│          budget 101, other 5 budgets stay cached!    │
│                                                       │
└──────────────────────────────────────────────────────┘

```

**My implementation:**

```tsx
async getBudgets(userId: number) {
  // Step 1: Get budget definitions from PostgreSQL (cheap query)
  const budgets = await this.db.query(
    'SELECT * FROM budgets WHERE user_id = $1',
    [userId]
  );

  // Step 2: For each budget, try cache first
  const budgetsWithSpending = await Promise.all(
    budgets.map(async (budget) => {
      // Build cache key per budget + period
      const cacheKey = `budget:spent:${budget.id}:${budget.period_start}`;

      // Try cache
      let spent = await this.redis.get(cacheKey);

      if (spent) {
        // ✅ Cache HIT for this budget
        console.log(`Cache HIT for budget ${budget.id} (${budget.category})`);

        return {
          ...budget,
          spent: parseInt(spent, 10),
        };
      }

      // ❌ Cache MISS - calculate from database
      console.log(`Cache MISS for budget ${budget.id} (${budget.category})`);

      const { sum } = await this.db.query(`
        SELECT SUM(ABS(amount_cents)) as sum
        FROM transactions
        WHERE user_id = $1
        AND category = $2
        AND transaction_date >= $3
        AND transaction_date <= $4
        AND amount_cents < 0
      `, [userId, budget.category, budget.period_start, budget.period_end]);

      spent = sum || 0;

      // Cache for 1 hour
      await this.redis.setex(cacheKey, 3600, spent.toString());

      return {
        ...budget,
        spent: parseInt(spent, 10),
      };
    })
  );

  return budgetsWithSpending;
}

```

**Cache key structure:**

```
budget:spent:{budgetId}:{periodStart}

Examples:
  budget:spent:101:2024-01-01  → Food budget for January 2024
  budget:spent:102:2024-01-01  → Transport budget for January 2024
  budget:spent:101:2024-02-01  → Food budget for February 2024

```

**Why include period in key?**

- Same budget (e.g., "food") has different spending each month
- January food spending ≠ February food spending
- Need separate cache entries

---

**Step 3: Targeted Cache Invalidation**

**The key insight:** Only invalidate affected budget when transactions change!

**Implementation:**

```tsx
// src/accounts/accounts.service.ts

async onTransactionSynced(transaction: Transaction) {
  const { user_id, category, transaction_date } = transaction;

  // Step 1: Find which budget this transaction affects
  const budget = await this.db.query(`
    SELECT id, period_start_date
    FROM budgets
    WHERE user_id = $1
    AND category = $2
    AND period_start_date <= $3
    AND period_end_date >= $3
  `, [user_id, category, transaction_date]);

  if (!budget.rows.length) {
    // No budget for this category/period, nothing to invalidate
    return;
  }

  // Step 2: Invalidate ONLY this budget's cache
  const cacheKey = `budget:spent:${budget.rows[0].id}:${budget.rows[0].period_start_date}`;

  await this.redis.del(cacheKey);

  console.log(`Invalidated cache for budget ${budget.rows[0].id} (${category})`);

  // Other budgets' caches remain valid! 🎉
}

```

**Visual flow of targeted invalidation:**

```
Bank Sync Adds Transaction:
  -€5 Starbucks (category: food, date: 2024-01-15)

  ↓

Find Affected Budget:
  SELECT * FROM budgets
  WHERE user_id = 123
  AND category = 'food'
  AND period_start_date <= '2024-01-15'
  AND period_end_date >= '2024-01-15'

  ↓

Result: Budget ID 101 (January 2024 food budget)

  ↓

Invalidate Only This Budget:
  redis.del('budget:spent:101:2024-01-01')

  ↓

Other budgets still cached:
  ✅ budget:spent:102:2024-01-01  (transport) - STILL CACHED
  ✅ budget:spent:103:2024-01-01  (entertainment) - STILL CACHED
  ✅ budget:spent:104:2024-01-01  (shopping) - STILL CACHED
  ✅ budget:spent:105:2024-01-01  (utilities) - STILL CACHED
  ✅ budget:spent:106:2024-01-01  (health) - STILL CACHED

  ↓

Next budget page load:
  Food budget: Cache MISS → Query DB → Cache (1 query)
  Other 5 budgets: Cache HIT → Return cached (0 queries)

Total queries: 1 instead of 6! 🎉

```

---

**Performance Analysis**

**Before All Optimizations**

**For user with 5,000 transactions:**

```
Load budgets page:
  ↓
Query 1: SELECT * FROM budgets WHERE user_id = 123
  Time: 5ms
  ↓
Query 2: SELECT SUM(...) FROM transactions
         WHERE category = 'food' AND ...
  Time: 1400ms (scans 5,000 rows, no index)
  ↓
Query 3: SELECT SUM(...) FROM transactions
         WHERE category = 'transport' AND ...
  Time: 1400ms
  ↓
... (6 queries total)
  ↓
Total time: 5ms + (1400ms × 6) = 8,405ms ≈ 8.4 seconds

```

**After Database Index (No Caching Yet)**

```
Load budgets page:
  ↓
Query 1: SELECT * FROM budgets WHERE user_id = 123
  Time: 5ms
  ↓
Single JOIN query with indexed columns:
  SELECT b.*, SUM(...) FROM budgets b
  LEFT JOIN transactions t ON ...
  WHERE b.user_id = 123
  GROUP BY b.id

  Time: 1200ms (index lookup, but still processing 5,000 rows)
  ↓
Total time: 1205ms ≈ 1.2 seconds

Improvement: 8.4s → 1.2s (83% faster!)

```

**After Smart Caching (First Load - All Misses)**

```
Load budgets page (first time today):
  ↓
Query 1: SELECT * FROM budgets WHERE user_id = 123
  Time: 5ms
  ↓
For each budget (cache miss):
  Query: SELECT SUM(...) FROM transactions
         WHERE category = 'food' AND ... (indexed!)
  Time: 200ms × 6 budgets = 1200ms

  Cache each result
  ↓
Total time: 5ms + 1200ms = 1205ms ≈ 1.2 seconds

Same as no caching, but now cached for next time

```

**After Smart Caching (Second Load - All Hits)**

```
Load budgets page (second time):
  ↓
Query 1: SELECT * FROM budgets WHERE user_id = 123
  Time: 5ms
  ↓
For each budget:
  Redis GET: budget:spent:{id}:{period}
  Time: 2ms × 6 budgets = 12ms
  ↓
Total time: 5ms + 12ms = 17ms

Improvement: 1.2s → 17ms (70x faster!)

```

**After Bank Sync (Partial Cache Invalidation)**

```
Bank sync adds 1 transaction (category: food)
  ↓
Invalidate cache: budget:spent:101:2024-01-01
  ↓
Next load budgets page:
  ↓
Query 1: SELECT * FROM budgets WHERE user_id = 123
  Time: 5ms
  ↓
Budget 101 (food):
  Cache MISS → Query PostgreSQL → Cache
  Time: 200ms
  ↓
Budgets 102-106 (others):
  Cache HIT × 5
  Time: 2ms × 5 = 10ms
  ↓
Total time: 5ms + 200ms + 10ms = 215ms

Only 1 budget recalculated, 5 cached!

```

---

**Cache Hit Rate Analysis**

**Metrics After 1 Week of Production**

**Cache hit rate by scenario:**

```
Scenario 1: User loads budgets (no recent transactions)
  Food budget: Cache HIT
  Transport budget: Cache HIT
  Entertainment budget: Cache HIT
  Shopping budget: Cache HIT
  Utilities budget: Cache HIT
  Health budget: Cache HIT

  Hit rate: 6/6 = 100% ✅

Scenario 2: User loads budgets after 1 transaction added
  Food budget: Cache MISS (transaction in food category)
  Transport budget: Cache HIT
  Entertainment budget: Cache HIT
  Shopping budget: Cache HIT
  Utilities budget: Cache HIT
  Health budget: Cache HIT

  Hit rate: 5/6 = 83% ✅

Scenario 3: User loads budgets after 3 transactions in different categories
  Food budget: Cache MISS
  Transport budget: Cache MISS
  Entertainment budget: Cache MISS
  Shopping budget: Cache HIT
  Utilities budget: Cache HIT
  Health budget: Cache HIT

  Hit rate: 3/6 = 50% ✅ (still good!)

```

**Overall metrics (1 week production):**

- **Cache hit rate: 85%**
- Average response time: 180ms (was 8.4s)
- P95 response time: 320ms (was 11.2s)
- P99 response time: 450ms (was 15s)

**Comparison with old approach (coarse-grained caching):**

- Old cache hit rate: 15% (invalidated too often)
- New cache hit rate: 85% (targeted invalidation)
- **5.7x improvement in cache effectiveness!**

---

### Visual Diagram: Smart Budget Caching Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                                                                 │
│                     USER LOADS BUDGETS PAGE                     │
│                                                                 │
└──────────────────────────┬─────────────────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────┐
        │ BudgetsService.getBudgets(userId)     │
        └──────────────┬───────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │ Query PostgreSQL for budget definitions│
        │ SELECT * FROM budgets                 │
        │ WHERE user_id = 123                   │
        │                                       │
        │ Returns: [                            │
        │   {id: 101, category: 'food', ...},   │
        │   {id: 102, category: 'transport',...}│
        │   ... (6 budgets)                     │
        │ ]                                     │
        └──────────────┬───────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │ For EACH budget (parallel):           │
        └──────────────┬───────────────────────┘
                       │
           ┌───────────┼───────────┐
           │           │           │
           ▼           ▼           ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │Budget101│ │Budget102│ │Budget103│  ... (6 budgets)
    │ (food)  │ │(transport)│ │(entertain)│
    └────┬────┘ └────┬────┘ └────┬────┘
         │           │           │
         ▼           ▼           ▼
  ┌─────────────────────────────────────────┐
  │ Build cache key:                         │
  │ budget:spent:{budgetId}:{periodStart}    │
  │                                          │
  │ budget:spent:101:2024-01-01              │
  │ budget:spent:102:2024-01-01              │
  │ budget:spent:103:2024-01-01              │
  └─────────────┬───────────────────────────┘
                │
                ▼
  ┌─────────────────────────────────────────┐
  │ Try Redis GET                            │
  │ const spent = await redis.get(cacheKey)  │
  └─────────────┬───────────────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
    ▼                       ▼
┌─────────┐           ┌──────────┐
│ Cache   │           │ Cache    │
│ HIT     │           │ MISS     │
│ (spent  │           │ (null)   │
│ = 450)  │           │          │
└────┬────┘           └────┬─────┘
     │                     │
     │                     ▼
     │           ┌────────────────────────────┐
     │           │ Query PostgreSQL (indexed!)│
     │           │ SELECT SUM(ABS(amount_cents))│
     │           │ FROM transactions           │
     │           │ WHERE user_id = 123         │
     │           │ AND category = 'food'       │
     │           │ AND date BETWEEN ...        │
     │           │ AND amount_cents < 0        │
     │           │                             │
     │           │ Time: 200ms (fast w/ index!)│
     │           └────────┬───────────────────┘
     │                    │
     │                    ▼
     │           ┌────────────────────────────┐
     │           │ Store in Redis              │
     │           │ redis.setex(                │
     │           │   cacheKey,                 │
     │           │   3600,        // 1 hour TTL│
     │           │   spent        // value     │
     │           │ )                           │
     │           └────────┬───────────────────┘
     │                    │
     └─────────┬──────────┘
               │
               ▼
  ┌─────────────────────────────────────────┐
  │ Return budget with spent amount          │
  │ {                                        │
  │   id: 101,                               │
  │   category: 'food',                      │
  │   spent: 450,                            │
  │   limit: 600                             │
  │ }                                        │
  └─────────────────────────────────────────┘

Final result: Array of 6 budgets with spending
Response time:
  - All cache hits: 17ms
  - 1 miss, 5 hits: 215ms
  - All cache misses: 1.2s

```

---

### Targeted Invalidation Flow

```
┌────────────────────────────────────────────────────┐
│ BANK SYNC COMPLETES                                 │
│ New transaction: -€5 Starbucks                     │
│ Category: food                                      │
│ Date: 2024-01-15                                    │
└──────────────────┬─────────────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │ onTransactionSynced(transaction)      │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────────────┐
    │ Query: Find affected budget                   │
    │ SELECT id, period_start_date                  │
    │ FROM budgets                                  │
    │ WHERE user_id = 123                           │
    │ AND category = 'food'                         │
    │ AND period_start_date <= '2024-01-15'         │
    │ AND period_end_date >= '2024-01-15'           │
    │                                               │
    │ Result: Budget ID 101, Period: 2024-01-01    │
    └──────────────┬───────────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────────────┐
    │ Build cache key                               │
    │ cacheKey = budget:spent:101:2024-01-01        │
    └──────────────┬───────────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────────────┐
    │ Delete from Redis                             │
    │ redis.del('budget:spent:101:2024-01-01')      │
    │                                               │
    │ ✅ Food budget cache invalidated              │
    └──────────────┬───────────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────────────┐
    │ Other budgets REMAIN CACHED:                  │
    │                                               │
    │ ✅ budget:spent:102:2024-01-01 (transport)    │
    │ ✅ budget:spent:103:2024-01-01 (entertainment)│
    │ ✅ budget:spent:104:2024-01-01 (shopping)     │
    │ ✅ budget:spent:105:2024-01-01 (utilities)    │
    │ ✅ budget:spent:106:2024-01-01 (health)       │
    └───────────────────────────────────────────────┘

```

---

## Result

**Production metrics (after 1 month):**

- **User impact:** Power users (200 users) budget page 70x faster
- **Support tickets:** Complaints dropped to zero
- **Cache hit rate:** 85% (vs 15% with coarse-grained caching)
- **Response times:**
    - P50: 18ms (all cache hits)
    - P95: 215ms (1-2 cache misses)
    - P99: 450ms (3-4 cache misses)

**Technical achievements:**

- Fixed N+1 query problem
- Learned database indexing (composite, partial indexes)
- Implemented granular caching strategy
- Mastered targeted cache invalidation

**Personal growth:**

- "I can optimize production systems!"
- Database performance is now in my skillset
- Learned to use profiling tools (DataDog APM)

**Team recognition:**

Marcus in code review:
"Excellent optimization work. The granular caching approach is
senior-level thinking. Most junior engineers would just add a
simple cache and call it done. You analyzed the invalidation
patterns and optimized for cache effectiveness. Well done."

Product team thanked me directly:
"Our power users are thrilled. Page loads are instant now."

---

## 🛡️Interview Preparation: Smart Budget Caching Questions

### Q1: "Explain the caching strategy you designed for budget calculations."

**My Answer:**

"I implemented a two-tier optimization for budget calculation performance:

**Tier 1: Database optimization**
I fixed an N+1 query problem where we queried the database 7 times per page load. I combined these into a single JOIN query and added a composite index on `(user_id, category, transaction_date)` with a partial index filter for `amount_cents < 0`.

The index was crucial. Without it, queries scanned all 5,000 transactions per user. With the index, PostgreSQL could jump directly to relevant transactions. Query time dropped from 1.4s to 200ms per budget.

**Tier 2: Granular caching**
But 200ms × 6 budgets = 1.2s was still not great. So I added Redis caching with per-budget granularity.

Instead of caching all budgets together, I cached each budget separately with keys like `budget:spent:{budgetId}:{periodStart}`. This allowed targeted cache invalidation.

When a bank sync adds a transaction in the 'food' category, I only invalidate the food budget's cache - the other 5 budgets remain cached. This achieved an 85% cache hit rate versus 15% with coarse-grained caching.

**Final performance:**

- All cache hits: 17ms (70x faster than original 1.2s)
- Mixed hits/misses: 215ms (still 5.5x faster)
- All cache misses: 1.2s (same as database-only, but rare)

The key insight was realizing that cache invalidation granularity directly impacts cache effectiveness. Finer-grained invalidation means higher hit rates."

### Q2: "Why did you choose per-budget caching over caching all budgets together?"

**My Answer:**

"This was a deliberate design choice based on invalidation frequency analysis.

**Problem with coarse-grained caching:**
If I cache all 6 budgets together, any new transaction invalidates the entire cache. Since users get new transactions daily during bank sync, the cache would be invalidated every day, giving only a 15% hit rate.

**Benefits of fine-grained caching:**

1. **Targeted invalidation:** When a transaction is added to the 'food' category, only the food budget cache is invalidated. The other 5 budgets remain cached.
2. **Higher hit rate:** With 6 budget categories and transactions distributed across them, most budgets stay cached most of the time. Achieved 85% hit rate.
3. **Graceful degradation:** If 1 budget has a cache miss, we only query the database for that one budget (200ms). The other 5 return from cache (2ms each), so total is 210ms instead of 1200ms.

**Trade-offs I considered:**

*Pros:*

- Much higher cache hit rate (85% vs 15%)
- Faster average response time
- Less database load

*Cons:*

- More complex code (have to iterate through budgets)
- More Redis keys (6 keys per user instead of 1)
- Slightly more memory usage

The memory increase was negligible (6 keys × 10 bytes vs 1 key × 200 bytes - actually uses less!), so the pros vastly outweighed cons.

I also considered caching at the transaction level, but that would be too granular - we'd have thousands of cache keys per user and complex aggregation logic. Per-budget was the sweet spot."

### Q3: "How did you handle the case where cache invalidation fails?"

**My Answer:**

"Great question - this is where TTL acts as a safety net.

**Failure scenarios:**

1. **Redis down during invalidation:**

    ```tsx
    try {
      await this.redis.del(cacheKey);
    } catch (error) {
      console.error('Failed to invalidate cache:', error);
      // Bank sync continues, but cache not invalidated
    }
    
    ```

   **Impact:** User might see stale spending (€450 instead of €455) for up to 1 hour (TTL). Not ideal, but acceptable for budget display.

2. **Application crashes after transaction insert but before invalidation:**
    - Transaction written to PostgreSQL
    - App crashes before `redis.del()` executes
    - Cache still has old value

   **Impact:** Same - stale data for up to 1 hour.


**Why 1 hour TTL is acceptable:**

Budget spending is not critical real-time data. It's okay if the user sees "You've spent €450" when it's actually €455 for a short time. The error is bounded by TTL.

If this were account balance or investment value, I would:

- Use much shorter TTL (30 seconds)
- Add retry logic for invalidation
- Or not cache at all (always query authoritative source)

**Monitoring for invalidation failures:**

I added metrics to track:

```tsx
// After invalidation
const exists = await this.redis.exists(cacheKey);
if (exists) {
  // Deletion failed! Log error
  logger.error('Cache invalidation failed', { cacheKey });
}

```

In production, we logged these failures. If we saw patterns (e.g., Redis connection timeouts), we could increase connection pool size or add retry logic.

**The golden rule:** For financial data that must be accurate (account balances, order amounts), don't rely on cache invalidation - either don't cache or use very short TTLs. For display data like budgets, TTL is acceptable fallback."

### Q4: "What would you do differently if you had to optimize this further?"

**My Answer:**

"If I needed to optimize further, here are the approaches I'd consider:

**1. Probabilistic early cache refresh:**
Instead of waiting for cache to expire, refresh it proactively when TTL is low:

```tsx
const ttl = await this.redis.ttl(cacheKey);

if (ttl > 0 && ttl < 300) {
  // Less than 5 minutes left - refresh in background
  this.refreshCacheInBackground(budgetId);

  // Still return cached value (stale but fast)
  return cachedValue;
}

```

This prevents cache misses entirely - user always gets cached data while we refresh in background.

**2. Cache warming after bank sync:**
Instead of invalidating caches, proactively recalculate and update them:

```tsx
async onBankSyncComplete(userId: number) {
  // Get all affected budgets
  const budgets = await this.getAffectedBudgets(userId);

  // Recalculate and update caches (don't delete)
  for (const budget of budgets) {
    const spent = await this.calculateSpending(budget);
    await this.redis.setex(cacheKey, 3600, spent);
  }

  // User's next request sees fresh cache!
}

```

**3. Redis pipelining for bulk operations:**
When loading 6 budgets, reduce round-trips:

```tsx
// Instead of 6 separate GET commands
const spent1 = await redis.get('budget:spent:101:...');
const spent2 = await redis.get('budget:spent:102:...');
// ... (6 round-trips)

// Use pipeline (1 round-trip)
const pipeline = redis.pipeline();
pipeline.get('budget:spent:101:...');
pipeline.get('budget:spent:102:...');
// ...
const results = await pipeline.exec();

```

Saves ~5-10ms by reducing network round-trips.

**4. Local in-memory cache (L1 cache):**
Add LRU cache in application memory for ultra-frequent queries:

```tsx
// Memory cache (LRU, max 1000 entries)
const memoryCache = new LRU({ max: 1000, ttl: 60000 });

// Check memory cache first (1ms)
let spent = memoryCache.get(cacheKey);
if (spent) return spent;

// Then Redis (2ms)
spent = await redis.get(cacheKey);
if (spent) {
  memoryCache.set(cacheKey, spent);
  return spent;
}

// Finally PostgreSQL (200ms)

```

**5. Aggregate at write time (cache-aside → write-through):**
Update cache when transaction is inserted:

```tsx
async insertTransaction(transaction) {
  // Insert to PostgreSQL
  await db.insert(transaction);

  // Update cache immediately
  const currentSpent = await redis.get(cacheKey) || 0;
  const newSpent = currentSpent + transaction.amount;
  await redis.setex(cacheKey, 3600, newSpent);
}

```

This eliminates cache misses entirely but adds complexity.

**Which would I choose?**

Probabilistic early refresh (#1) gives best bang for buck - simple code, eliminates most cache misses. The others add complexity that may not be worth it at our scale (450K users).

But I'd first measure whether further optimization is needed. Current performance (85% hit rate, 215ms P95) is already very good. Premature optimization is the root of all evil!"