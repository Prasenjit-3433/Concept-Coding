# Step 7: Caching Strategy

---

## Start With The Honest Foundation — Why Cache At All?

Before picking any caching tool or pattern, the question is always: **what problem are we actually solving?**

In our services (Expense & Reimbursement + Invoice & AP), the expensive operations that repeat constantly are:

```
1. Approval policy lookup
   ────────────────────────
   Every expense submission asks:
   "What's the approval rule for this company?"
   (amounts > €50 → manager, > €2000 → finance manager)
   
   This policy changes maybe once a quarter.
   But it's fetched on every single expense submission.
   Stored in User & Org Service — means a FeignClient 
   call every time. That's a network round-trip 
   for data that almost never changes.

2. Exchange rates
   ────────────────
   Every expense in GBP, USD, or any non-EUR currency
   needs a EUR conversion at submission time.
   Exchange rates change daily, not per-request.
   Calling an external FX API on every submission 
   is slow and costs money per API call.

3. Employee/manager lookups
   ─────────────────────────
   "Who is this employee's manager?"
   Required during approval routing.
   Fetched from User & Org Service via FeignClient.
   Changes only when someone changes jobs.
   No reason to call the service fresh every time.

4. Supplier details
   ──────────────────
   Invoice service frequently reads supplier 
   name, IBAN, currency for the same vendors.
   A company typically works with 20-50 suppliers.
   Same suppliers appear in every payment run.
```

These four patterns share one thing: **data that is read far more than it's written, and doesn't change often.** That's the exact definition of cacheable data.

---

## Part 1 — Two Levels of Cache: Local vs Distributed

We use **two levels of caching** in our services. Understanding why we need both is the foundation.

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST ARRIVES                          │
│                          │                                  │
│                          ▼                                  │
│              ┌───────────────────────┐                      │
│              │   L1: LOCAL CACHE     │                      │
│              │   (Caffeine)          │                      │
│              │   Lives IN the JVM    │                      │
│              │   heap memory         │                      │
│              │   ~millisecond access │                      │
│              └───────────┬───────────┘                      │
│                          │ MISS                             │
│                          ▼                                  │
│              ┌───────────────────────┐                      │
│              │   L2: DISTRIBUTED     │                      │
│              │   CACHE (Redis)       │                      │
│              │   Shared across all   │                      │
│              │   service instances   │                      │
│              │   ~1-5ms access       │                      │
│              └───────────┬───────────┘                      │
│                          │ MISS                             │
│                          ▼                                  │
│              ┌───────────────────────┐                      │
│              │   SOURCE OF TRUTH     │                      │
│              │   DB or FeignClient   │                      │
│              │   ~10-100ms           │                      │
│              └───────────────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

### L1 — Caffeine (Local, In-Process)

```
What it is:
────────────
A Java in-memory cache library.
Lives inside the JVM of each service instance.
Data stored directly in heap memory.
Zero network — access is nanoseconds to microseconds.

When to use it:
───────────────
Data that:
- Is accessed extremely frequently (every request)
- Is the same for every instance 
  (no user-specific data)
- Is small enough to fit in memory
- Staleness of a few minutes is acceptable

Our use:
─────────
Exchange rates → cached in Caffeine for 1 hour
  (rates don't change per-request, 
   and 1 hour staleness is fine for FX)

Approval policy → cached in Caffeine for 15 minutes
  (policy changes rarely, 
   short TTL catches any changes)

Why NOT Redis for these?
─────────────────────────
Redis requires a network call — 
even at 1-2ms, that adds latency 
on every single request.
For data every instance needs identically,
Caffeine gives the same benefit with 
zero network overhead.
```

### L2 — Redis (Distributed, Shared)

```
What it is:
────────────
An in-memory data store running as 
a separate service.
Shared across ALL instances of a service.
Network call required — but still 
10-50x faster than DB.

When to use it:
───────────────
Data that:
- Is user/entity-specific 
  (can't share in local cache meaningfully)
- Must be consistent across 
  multiple service instances
- Is too large or too dynamic 
  for local cache

Our use:
─────────
Employee/manager lookup → Redis, TTL 30 minutes
  (specific to each employee UUID,
   must be same across all instances)

Supplier details → Redis, TTL 1 hour
  (specific to each supplier UUID)

Rate limiting state → Redis
  (must be shared — can't do per-instance 
   rate limiting, requests hit different pods)

Approval state cache (for dashboard) → Redis, TTL 5 minutes
  (finance manager's "pending approvals" count —
   same data must be consistent across instances)
```

### Why Not Just Use Redis for Everything?

```
Redis has a network round-trip cost.
Even at 1-2ms, if you're calling it 
50 times per request, that's 50-100ms 
added latency just from cache calls.

For truly global, rarely-changing data 
(exchange rates, approval policies),
Caffeine + local cache avoids this completely.

For per-entity data (employee, supplier),
Redis is correct — local cache would 
diverge across instances.

The rule:
──────────
Is the data the same for every instance? 
→ Caffeine (L1)

Is the data entity-specific, 
or must it be consistent across instances? 
→ Redis (L2)
```

---

## Part 2 — Caching Patterns

### Cache-Aside (Lazy Loading) — Our Primary Pattern

This is what we use for most of our caching. The application controls the cache, not the database.

```
HOW IT WORKS:
─────────────

READ:
┌─────────────────────────────────────────────────────┐
│ 1. Application checks cache                         │
│         │                                           │
│    HIT  │  MISS                                     │
│    ▼    │    ▼                                      │
│ Return  │  Fetch from DB/FeignClient                │
│ cached  │    │                                      │
│ data    │    ▼                                      │
│         │  Write to cache (with TTL)                │
│         │    │                                      │
│         │    ▼                                      │
│         │  Return data                              │
└─────────────────────────────────────────────────────┘

WRITE:
┌─────────────────────────────────────────────────────┐
│ 1. Write to DB                                      │
│ 2. Invalidate (delete) the cache entry              │
│    (NOT update — explained below)                   │
└─────────────────────────────────────────────────────┘
```

**Implementation in our service:**

```java
@Service
@RequiredArgsConstructor
public class ApprovalPolicyService {

    private final UserOrgFeignClient userOrgFeignClient;
    private final RedisTemplate<String, ApprovalPolicy> redisTemplate;

    private static final Duration POLICY_TTL = Duration.ofMinutes(15);
    private static final String CACHE_KEY_PREFIX = "approval_policy:";

    public ApprovalPolicy getApprovalPolicy(UUID companyId) {

        String cacheKey = CACHE_KEY_PREFIX + companyId;

        // Step 1: Check Redis (L2)
        ApprovalPolicy cached = redisTemplate.opsForValue()
            .get(cacheKey);

        if (cached != null) {
            return cached;  // Cache hit
        }

        // Step 2: Cache miss — fetch from User & Org Service
        ApprovalPolicy policy = userOrgFeignClient
            .getApprovalPolicy(companyId);

        // Step 3: Store in Redis with TTL
        redisTemplate.opsForValue().set(
            cacheKey,
            policy,
            POLICY_TTL
        );

        return policy;
    }

    // Called when policy is updated
    public void invalidateApprovalPolicy(UUID companyId) {
        String cacheKey = CACHE_KEY_PREFIX + companyId;
        redisTemplate.delete(cacheKey);
        // Next request will re-fetch fresh data
    }
}
```

**Why delete on write instead of updating the cache?**

```
Seems wasteful — why not just update the 
cache value instead of deleting it?

The subtle bug with update:
────────────────────────────
Thread A: reads old policy → 
Thread B: updates policy in DB, 
          updates cache with new value →
Thread A: writes OLD value to cache 
          (overwrites the correct value)

Result: cache has stale data, 
        no TTL will save you — 
        the stale value just refreshed its TTL.

Delete-on-write is safe:
─────────────────────────
Thread A: reads old policy →
Thread B: updates policy in DB, 
          DELETES cache entry →
Thread A: tries to write old value to cache,
          but next reader will find cache empty
          and fetch fresh from DB anyway.

With delete, the worst case is a cache miss
(slightly slower). With update, the worst 
case is wrong data for the full TTL duration.
```

---

### Write-Through — Where We Use It

```
HOW IT WORKS:
─────────────
Every write goes to BOTH cache AND DB 
at the same time, synchronously.
Cache is always up to date.
No stale reads possible.

WRITE:
┌────────────────────────────────────────┐
│ 1. Write to DB                         │
│ 2. Write to cache                      │  ← both happen
│ (both in same operation)               │
└────────────────────────────────────────┘

READ:
┌────────────────────────────────────────┐
│ Always hits cache first                │
│ Cache is always warm (never cold miss  │
│ for recently written data)             │
└────────────────────────────────────────┘
```

**Where we use it in our team:**

```
Expense status tracking on the dashboard:
──────────────────────────────────────────
Finance manager's dashboard shows:
- "14 expenses pending approval"
- "3 invoices overdue"

These counts are read on every page load.
Every time an expense is approved/rejected/submitted,
we update the count in Redis immediately 
alongside the DB write.

Why Write-Through here and not Cache-Aside?
────────────────────────────────────────────
Cache-Aside would mean the count goes stale
until the next request triggers a re-fetch.
Finance manager approves expense 1,
looks at dashboard immediately —
still shows old count. Bad UX.

With Write-Through, count updates instantly.
Dashboard always shows correct number.
```

```java
@Service
@RequiredArgsConstructor
public class ExpenseStatusCountService {

    private final RedisTemplate<String, Integer> redisTemplate;
    private static final String COUNT_KEY = 
        "pending_approval_count:";

    @Transactional
    public void onExpenseApproved(UUID companyId) {

        // DB update happens in calling service

        // Write-Through: update cache 
        // immediately after DB write
        String key = COUNT_KEY + companyId;
        redisTemplate.opsForValue().decrement(key);
        // One less pending expense
    }

    @Transactional
    public void onExpenseSubmitted(UUID companyId) {

        String key = COUNT_KEY + companyId;
        redisTemplate.opsForValue().increment(key);
        // One more pending expense
    }
}
```

---

### Write-Behind (Write-Back) — Why We Don't Use It

```
HOW IT WORKS:
─────────────
Write to cache immediately.
Write to DB asynchronously later (batched).
Very fast writes — DB write is not 
in the critical path.

WHY WE DON'T USE IT:
──────────────────────
If the cache (Redis) goes down before 
the DB write happens → data loss.

For financial data (expense amounts, 
invoice status, payment records), 
data loss is unacceptable.

The DB is always the source of truth.
Cache is always derived from DB.
We never accept "maybe this will 
reach the DB eventually."

Write-Behind is useful for analytics 
event counters, view counts, game scores —
where some loss is acceptable.
Not for us.
```

---

## Part 3 — Redis Data Structures in Our Services

Redis is not just a key-value store — it has several data structures. Using the right one matters.

### How We Actually Store Data

```
1. APPROVAL POLICY — String (serialized JSON)
──────────────────────────────────────────────
Key:   "approval_policy:{companyId}"
Value: JSON string of ApprovalPolicy object
TTL:   15 minutes

{
  "companyId": "uuid-123",
  "rules": [
    {"minAmount": 0,    "maxAmount": 50,    "approverRole": "SELF"},
    {"minAmount": 50,   "maxAmount": 2000,  "approverRole": "MANAGER"},
    {"minAmount": 2000, "maxAmount": null,  "approverRole": "FINANCE_MANAGER"}
  ]
}

Why String (not Hash)?
The whole policy is always fetched together.
No need to fetch individual fields.
Serialize the whole object → store as one value.


2. EMPLOYEE DETAILS — Hash
──────────────────────────
Key:   "employee:{employeeId}"
Field: "name", "email", "managerId", "role"
TTL:   30 minutes

HSET employee:uuid-456 
  name "Sarah Chen"
  email "sarah@company.com"
  managerId "uuid-789"
  role "FINANCE_MANAGER"

Why Hash (not String)?
Sometimes we only need the manager's name 
for a notification — not the full object.
Hash lets us fetch one field:
HGET employee:uuid-456 managerId
Efficient — doesn't deserialize the 
whole object just to get one field.


3. PENDING APPROVAL COUNTS — String (integer)
─────────────────────────────────────────────
Key:   "pending_count:{companyId}:{type}"
       e.g. "pending_count:uuid-123:EXPENSE"
            "pending_count:uuid-123:INVOICE"
Value: Integer count
TTL:   None (Write-Through keeps it current)
       → but we SET an expiry as safety net (1 day)

Operations:
INCR pending_count:uuid-123:EXPENSE  → +1
DECR pending_count:uuid-123:EXPENSE  → -1

Why String with INCR/DECR?
Redis INCR is atomic.
Two requests simultaneously incrementing 
won't lose each other's increment.
No locks needed. Redis single-threaded 
command execution guarantees this.


4. EXCHANGE RATES — Hash
─────────────────────────
Key:   "fx_rates:{date}"  
       e.g. "fx_rates:2025-03-15"
Field: currency code
Value: rate against EUR

HSET fx_rates:2025-03-15
  GBP "1.170000"
  USD "1.082000"
  PLN "0.232000"
TTL: 24 hours (refreshed daily by scheduled job)

Why Hash?
All currency rates for a day stored together.
Fetching GBP rate: HGET fx_rates:2025-03-15 GBP
Fetching all rates: HGETALL fx_rates:2025-03-15
One key per day — clean, easy to reason about.


5. RATE LIMITING — String with INCR + EXPIRE
─────────────────────────────────────────────
Key:   "rate_limit:{companyId}:{endpoint}:{window}"
       e.g. "rate_limit:uuid-123:POST_expense:2025031510"
            (window = hour bucket)
Value: request count
TTL:   1 hour

Logic:
INCR → increment count
If count == 1 → EXPIRE key 3600 (set TTL on first hit)
If count > limit → reject request with 429

Why this structure?
Sliding window rate limiting without 
any external library.
Pure Redis atomic operations.
```

---

## Part 4 — TTL Design Decisions

TTL (Time To Live) is not arbitrary. Each value has a reason.

```
DATA                    TTL         REASONING
──────────────────────────────────────────────────────────────
Approval policy         15 min      Policy changes are rare but 
                                    do happen (e.g. company 
                                    raises approval threshold).
                                    15 min is acceptable staleness.
                                    Longer TTL risks serving 
                                    wrong approval route.

Exchange rates          24 hours    Rates are fetched from FX API 
                                    once daily by scheduled job.
                                    Serving same rate all day is 
                                    correct — that's the policy.

Employee details        30 min      Employee changes (role change,
                                    manager change) are rare.
                                    30 min catches changes without
                                    too many unnecessary refreshes.

Supplier details        1 hour      Supplier IBAN rarely changes.
                                    Long TTL is safe.

Pending count           1 day       Write-Through keeps it fresh.
(safety TTL)                        1 day TTL is a safety net in 
                                    case Write-Through misses 
                                    an update (bug, crash).
                                    Daily expiry forces re-fetch 
                                    from DB as fallback.

Pre-signed S3 URL       15 min      S3 pre-signed URLs expire 
(receipt download)                  in 15 min. TTL matches 
                                    the URL's own validity.
                                    No point caching an expired URL.
```

---

## Part 5 — Cache Invalidation

This is the hardest part of caching — and interviewers know it. The famous quote: *"There are only two hard things in computer science: cache invalidation and naming things."*

### When Does Our Cache Go Stale?

```
1. Approval policy changes
   → Admin updates approval rules in the system
   → Expense Service's cache of that policy is stale
   
2. Employee role/manager changes
   → HR updates employee's manager in User & Org Service
   → Expense Service's cached employee details are stale

3. Supplier IBAN changes
   → Finance team updates supplier's bank details
   → Invoice Service's cached supplier data is stale
```

### Invalidation Strategy 1: Event-Driven Invalidation (Our Primary Approach)

```
When User & Org Service updates an employee:
─────────────────────────────────────────────
1. User & Org Service publishes Kafka event:
   Topic: user.employee_updated
   Payload: { employeeId, companyId, changedFields }

2. Expense Service consumes user.employee_updated
3. Expense Service deletes from Redis:
   DEL employee:{employeeId}

Next request → cache miss → fresh fetch 
from User & Org Service → re-cached.

WHY KAFKA AND NOT A DIRECT API CALL?
──────────────────────────────────────
User & Org Service should not know 
that Expense Service caches its data.
That's a tight coupling — if Expense Service 
adds caching later or removes it,
User & Org Service code has to change.

With Kafka:
User & Org Service publishes an event 
("something changed").
Expense Service decides what to do with it
(in this case, invalidate cache).
Loose coupling. Correct separation of concerns.
```

**Consumer implementation:**

```java
@Component
@RequiredArgsConstructor
public class EmployeeUpdatedConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(
        topics = "user.employee_updated",
        groupId = "expense-service"
    )
    public void handleEmployeeUpdated(
            EmployeeUpdatedEvent event,
            Acknowledgment acknowledgment) {

        String cacheKey = "employee:" + event.getEmployeeId();
        redisTemplate.delete(cacheKey);

        log.info("Invalidated cache for employee: {}", 
            event.getEmployeeId());

        acknowledgment.acknowledge();
    }
}
```

---

### Invalidation Strategy 2: TTL-Based Expiry (Fallback)

```
Not everything has a Kafka event.
Some data changes in systems we don't control.
TTL is the safety net.

Even if our Kafka consumer misses an event
(e.g., consumer was down), the TTL 
ensures we don't serve stale data forever.

TTL = maximum acceptable staleness.
Event-driven invalidation = how we 
achieve faster refresh when we know 
something changed.

Both together = robust invalidation strategy.
```

---

### Invalidation Strategy 3: Local Cache (Caffeine) Invalidation

```
Problem:
─────────
We delete the Redis (L2) entry.
But each service instance also has 
a Caffeine (L1) entry for the same data.

Deleting from Redis does NOT 
delete from each instance's local cache.

Example:
─────────
3 instances of Expense Service.
Instance 1 handles the invalidation, 
deletes from Redis.
Instance 2 and Instance 3 still have 
the stale entry in their Caffeine cache.

For the next 15 minutes (Caffeine TTL),
requests hitting Instance 2 or 3 
get stale data.

Solution: Short TTLs on Caffeine.
──────────────────────────────────
Caffeine TTL for approval policy: 5 minutes
Redis TTL for approval policy:   15 minutes

Reasoning:
- If policy changes, event-driven invalidation 
  clears Redis immediately.
- Caffeine entries expire within 5 minutes 
  at most.
- At 5 minutes maximum staleness in L1,
  the UX impact is acceptable for policy changes.

Alternative: Pub/Sub invalidation across instances
────────────────────────────────────────────────────
Redis supports Pub/Sub messaging.
When invalidation happens:
- Delete from Redis
- Publish a message on a Redis channel: 
  "invalidate:approval_policy:{companyId}"
- Each instance subscribes to this channel
- Each instance clears its own Caffeine entry

We implemented this for approval policy 
(highest risk of staleness causing 
wrong routing decisions).
Not implemented for employee details 
(5 min TTL staleness acceptable).
```

**Redis Pub/Sub invalidation implementation:**

```java
// Publisher (on invalidation)
@Service
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private final RedisTemplate<String, String> redisTemplate;

    public void publishInvalidation(String cacheKey) {
        redisTemplate.convertAndSend(
            "cache-invalidation",   // channel
            cacheKey                // message
        );
    }
}

// Subscriber (in each instance)
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber 
        implements MessageListener {

    private final Cache caffeineCache; // local L1 cache

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String cacheKey = message.toString();
        caffeineCache.invalidate(cacheKey);
        log.debug("L1 cache invalidated for key: {}", cacheKey);
    }
}
```

---

## Part 6 — Production Problems & How We Handle Them

These are the scary scenarios interviewers love to ask about.

### Problem 1: Cache Stampede (Thundering Herd)

```
THE SCENARIO:
──────────────
Approval policy for a large company 
(500 employees) expires at 09:00 AM.

At 09:00 AM, morning work starts.
200 employees submit expenses simultaneously.

All 200 requests:
1. Check cache → MISS (just expired)
2. All 200 go to User & Org Service 
   simultaneously via FeignClient
3. User & Org Service gets hammered 
   with 200 simultaneous requests 
   for the same data
4. Maybe it falls over, or 
   becomes very slow

This is a cache stampede.
One cache expiry → avalanche of 
identical upstream requests.
```

```
OUR SOLUTION: Probabilistic Early Expiration
─────────────────────────────────────────────
Before the TTL actually expires,
start refreshing with some probability.

For a key with 15 min TTL:
At 12 min remaining → 5% chance of refresh
At 10 min remaining → 20% chance of refresh  
At 5 min remaining  → 50% chance of refresh

One request triggers the refresh early.
Cache is warm before it expires.
No stampede.

SIMPLER SOLUTION WE ACTUALLY USE: 
Mutex Lock on Cache Miss
──────────────────────────────────
When cache misses:
1. Try to acquire a Redis lock for this key
   SET lock:approval_policy:{id} 1 NX EX 10
   (NX = only set if not exists, EX = expire in 10s)

2. If lock acquired (first request):
   → Fetch from upstream
   → Store in cache
   → Release lock

3. If lock NOT acquired (other requests):
   → Wait briefly (50ms) and retry cache read
   → By then, first request has populated cache
   → Cache hit on retry

Result: Only ONE request hits User & Org Service.
All others wait a tiny moment and 
get the cached result.
```

**Implementation:**

```java
public ApprovalPolicy getApprovalPolicyWithLock(UUID companyId) {

    String cacheKey = "approval_policy:" + companyId;
    String lockKey  = "lock:" + cacheKey;

    // Step 1: Check cache
    ApprovalPolicy cached = redisTemplate.opsForValue()
        .get(cacheKey);
    if (cached != null) return cached;

    // Step 2: Try to acquire lock
    Boolean lockAcquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

    if (Boolean.TRUE.equals(lockAcquired)) {
        try {
            // I hold the lock — fetch from upstream
            ApprovalPolicy policy = userOrgFeignClient
                .getApprovalPolicy(companyId);
            redisTemplate.opsForValue().set(
                cacheKey, policy, Duration.ofMinutes(15));
            return policy;
        } finally {
            redisTemplate.delete(lockKey); // release lock
        }
    } else {
        // Another instance is fetching — wait and retry
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Retry — likely a cache hit now
        ApprovalPolicy retried = redisTemplate.opsForValue()
            .get(cacheKey);
        if (retried != null) return retried;

        // Fallback: fetch directly (lock holder may have failed)
        return userOrgFeignClient.getApprovalPolicy(companyId);
    }
}
```

---

### Problem 2: Cache Avalanche

```
THE SCENARIO:
──────────────
Different from stampede.

Stampede = one key expires, many requests 
           for that same key.

Avalanche = MANY different keys expire 
            at the same time, causing 
            a wave of DB/upstream hits.

Example:
─────────
At startup, Expense Service caches 
approval policies for all 5,000 companies.
All set with TTL = 15 minutes.
All expire at the same time (15 min later).
Suddenly, 5,000 FeignClient calls 
hit User & Org Service simultaneously.
That service collapses.
```

```
OUR SOLUTION: TTL Jitter
─────────────────────────
Instead of a fixed TTL, add random jitter:

TTL = baseTTL + random(0, jitterRange)

For approval policy:
Base TTL:    15 minutes
Jitter:      0-5 minutes random
Actual TTL:  15-20 minutes (different for each key)

Now expirations are spread over 5 minutes
instead of all at once.
No simultaneous wave.
```

**Implementation:**

```java
private static final Duration BASE_TTL = Duration.ofMinutes(15);
private static final int JITTER_SECONDS = 300; // 5 minutes

private Duration getTTLWithJitter() {
    int jitter = ThreadLocalRandom.current()
        .nextInt(0, JITTER_SECONDS);
    return BASE_TTL.plusSeconds(jitter);
}

// Usage:
redisTemplate.opsForValue().set(
    cacheKey,
    policy,
    getTTLWithJitter()  // 15-20 min, random
);
```

---

### Problem 3: Hot Key

```
THE SCENARIO:
──────────────
One key gets disproportionately 
more traffic than all others.

Example:
A company with 3,000 employees 
(large Moss customer) has their 
approval policy requested thousands 
of times per minute.

One Redis node handles this key.
That node becomes CPU-bottlenecked.
Other keys on the same node slow down.
Redis starts rejecting or delaying requests.
```

```
OUR SOLUTION AT SERIES B SCALE:
─────────────────────────────────
Honest answer: At 5,000 SME customers,
we don't have true hot key problems yet.
Our largest customers have a few hundred 
employees, not thousands.

But the pattern we'd use if needed:
Local cache (Caffeine) as first line.
If approval_policy for a large company 
is in every instance's Caffeine cache,
it NEVER reaches Redis.
Redis hot key problem solved by 
not hitting Redis at all.

This is actually why L1 (Caffeine) exists —
it absorbs the hottest keys.

For truly extreme hot keys (theoretical):
Key replication — store the same value 
under multiple keys:
  approval_policy:uuid-123:shard0
  approval_policy:uuid-123:shard1
  approval_policy:uuid-123:shard2

Requests randomly pick one shard.
Load distributed across 3 Redis nodes.

We don't do this yet — premature at our scale.
But knowing it exists impresses interviewers.
```

---

## Part 7 — Multi-Level Cache Synchronization

```
The two-level cache creates a consistency challenge.

L1 (Caffeine): 5 min TTL, per instance
L2 (Redis):   15 min TTL, shared

What if data changes?

Timeline:
─────────────────────────────────────
T=0:   Policy fetched, stored in L1 (5min) + L2 (15min)
T=1:   Admin changes approval policy
T=1:   Kafka event fires → Redis entry deleted
T=1:   Redis Pub/Sub → all L1 caches cleared
T=1:   Clean state — next request fetches fresh data

Without Redis Pub/Sub (simpler approach we started with):
──────────────────────────────────────────────────────────
T=0:   Policy fetched, stored in L1 (5min) + L2 (15min)
T=1:   Admin changes policy
T=1:   Kafka event → Redis entry deleted
T=1:   L1 caches still have old value (up to 5 min remaining)
T=6:   All L1 entries expired
T=6:   Clean state

This means: up to 5 minutes of stale data 
in L1 after a policy change.
For approval routing, we decided 5 minutes 
was too risky — wrong approval routing 
means compliance issues.
We added Redis Pub/Sub to clear L1 
immediately on policy changes.

For employee name/manager name (less critical):
We accepted the 5 min L1 TTL staleness.
A notification saying "approved by old manager name"
for 5 minutes is an acceptable UX trade-off
vs. the complexity of Pub/Sub for every key type.
```

---

## Part 8 — Monitoring Cache Health

```
Interviewers ask: "how did you know 
caching was actually helping?"

We monitor in Datadog:
```

```
KEY METRICS WE TRACK:
──────────────────────

1. Cache Hit Rate
   ──────────────
   hits / (hits + misses) × 100

   Target: > 85% for approval policy
           > 90% for exchange rates
           > 70% for employee details

   If hit rate drops:
   - TTL too short? Data expiring too fast.
   - Invalidation too aggressive?
   - Cache not warming up after deploy?

2. Cache Miss Rate (complement of above)
   Each miss = a FeignClient call or DB query.
   Sudden spike in misses → something wrong.

3. Redis Memory Usage
   ──────────────────
   If Redis memory is full, 
   it starts evicting keys.
   Monitor: used_memory vs maxmemory.
   Alert at 80% usage.

4. Redis Eviction Rate
   ───────────────────
   Keys evicted due to memory pressure
   (not TTL expiry — those are expected).
   Unexpected evictions → cache thrashing.
   Alert if eviction rate > 0 unexpectedly.

5. Redis Latency (P99)
   ────────────────────
   P99 latency for Redis operations.
   Target: < 5ms.
   Spike above 10ms → investigate.

6. Caffeine Hit Rate (per instance)
   ──────────────────────────────────
   Exposed via Spring Boot Actuator + Micrometer.
   Monitored per service instance.
   If one instance has much lower hit rate,
   it may have just restarted (cold cache).
```

**Exposing cache metrics via Micrometer:**

```java
@Configuration
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager(MeterRegistry meterRegistry) {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()   // ← enables hit/miss tracking
        );

        // Bind stats to Micrometer for Datadog
        new CaffeineCacheMetrics(
            manager.getCache("approvalPolicy"),
            "approvalPolicy",
            List.of()
        ).bindTo(meterRegistry);

        return manager;
    }
}
```

---

## Part 9 — Redis Configuration & Eviction Policy

```
In production, Redis is configured with:
─────────────────────────────────────────

maxmemory: 2GB
  (enough for our data volume at Series B)

maxmemory-policy: allkeys-lru
  (when memory full, evict Least Recently Used keys)

Why allkeys-lru and not volatile-lru?
─────────────────────────────────────
volatile-lru: only evict keys WITH a TTL set.
allkeys-lru: evict any key, TTL or not.

All our keys have TTLs — so both would 
behave similarly in practice.
But allkeys-lru gives Redis more 
flexibility to manage memory correctly
if we ever accidentally set a key without TTL.
Safer default.

Why LRU and not LFU (Least Frequently Used)?
─────────────────────────────────────────────
LFU is theoretically better for hot keys —
it keeps frequently accessed keys even if 
they weren't accessed recently.

But LRU is simpler and more predictable.
At our scale, LRU is sufficient.
```

---

## Summary — Quick Reference

```
WHAT WE CACHE        │ WHERE    │ PATTERN      │ TTL
─────────────────────┼──────────┼──────────────┼────────────
Approval policy      │ Redis+L1 │ Cache-Aside  │ 15min+5min
Exchange rates       │ Redis+L1 │ Cache-Aside  │ 24hr+1hr
Employee details     │ Redis    │ Cache-Aside  │ 30 min
Supplier details     │ Redis    │ Cache-Aside  │ 1 hour
Pending counts       │ Redis    │ Write-Through│ 1 day (safety)
Rate limiting        │ Redis    │ INCR + EXPIRE│ 1 hour

PROBLEM              │ SOLUTION
─────────────────────┼─────────────────────────────
Stampede             │ Redis mutex lock on cache miss
Avalanche            │ TTL jitter (random 0-5 min extra)
Hot key              │ L1 Caffeine absorbs hottest keys
Stale L1 on change   │ Redis Pub/Sub → clear all instances
Stale L2 on change   │ Event-driven invalidation via Kafka
```

---

Step 7 complete. This is saved as **Caching Strategy.md** in **Module 8: Caching Strategy**.

**Next is Step 8: Transaction Handling** — covering `@Transactional` best practices, isolation levels, concurrency issues (dirty reads, lost updates), and the tricky scenarios where transactions interact with `@Async` and Kafka. Ready?