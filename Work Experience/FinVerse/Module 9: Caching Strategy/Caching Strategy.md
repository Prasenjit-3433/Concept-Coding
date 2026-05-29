Writing Step 9 now.

---

# FinVerse — Step 9: Caching

## Part 1 — Why Caching Exists: Start With the Problem

Before any Redis key structure or invalidation strategy, the question every interviewer wants answered first is: **why did you need a cache at all?**

The answer at FinVerse comes from three distinct problems — and each one justifies a different caching pattern.

---

**Problem 1 — Some data is expensive to compute and read repeatedly**

Every time a user opens the home screen, the app calls:
- `GET /v1/accounts` — reads all bank accounts, computes total balance
- `GET /v1/portfolio` — fetches holdings, reads ETF prices, computes returns
- `GET /v1/budgets` — aggregates spending across transactions for the month

The portfolio valuation alone involves joining holdings, reading ETF prices from the Market Data Service, and running return calculations. If 10,000 users open the app simultaneously at 08:30 on a Monday, that is 10,000 portfolio valuation requests hitting the same data. The underlying data — ETF prices — changes every 15 minutes during market hours. Running the full valuation computation on every single request is wasteful when the answer will be identical for all 10,000 requests within the same 15-minute window.

**Problem 2 — Some data is read constantly but changes rarely**

User subscription tier. User notification preferences. User country (drives tax rules, pension API selection, currency defaults). These are read on virtually every authenticated request — auth checks, feature gates, tax logic — but change maybe once every few months per user. Reading from PostgreSQL on every request for data this stable is unnecessary.

**Problem 3 — Some operations need fast, shared state across containers**

Rate limiting. OTP verification codes. Bank sync status. These require state that is shared across all running instances of Core Product. An in-process cache (Node.js memory) cannot serve this — if Container 1 increments a rate limit counter, Container 2 has no visibility. Redis is the only appropriate store for shared, low-latency state.

---

## Part 2 — In-Process Cache vs Distributed Cache

The first architectural decision: **when do you cache in the Node.js process itself, and when do you use Redis?**

```
┌──────────────────────────────────────────────────────────────────┐
│           IN-PROCESS (Node.js Memory) vs DISTRIBUTED (Redis)     │
├─────────────────────────────┬────────────────────────────────────┤
│  IN-PROCESS                 │  DISTRIBUTED (Redis)               │
│  (node-cache, Map, object)  │                                    │
├─────────────────────────────┼────────────────────────────────────┤
│  Latency: ~0ms              │  Latency: ~0.5-2ms (local VPC)     │
│  No network hop             │  One network hop                   │
├─────────────────────────────┼────────────────────────────────────┤
│  Scope: ONE container only  │  Scope: ALL containers             │
│                             │                                    │
│  If 8 Core Product          │  All 8 containers read and         │
│  containers are running,    │  write the same cache              │
│  each has its own cache.    │                                    │
│  Updates in Container 1     │                                    │
│  are invisible to others.   │                                    │
├─────────────────────────────┼────────────────────────────────────┤
│  Persistence: NONE          │  Persistence: configurable         │
│  Container restart = cache  │  Survives container restarts       │
│  gone                       │                                    │
├─────────────────────────────┼────────────────────────────────────┤
│  Memory: container's RAM    │  Memory: separate Redis instance   │
│  directly consumed          │  doesn't compete with app RAM      │
├─────────────────────────────┼────────────────────────────────────┤
│  Right for:                 │  Right for:                        │
│  - Truly static config      │  - Anything shared across          │
│    (loaded once at startup) │    containers                      │
│  - Data that must be        │  - Rate limiting                   │
│    consistent within        │  - Session/OTP state               │
│    one request only         │  - Computed results (portfolio     │
│  - Very high frequency,     │    valuations, expensive queries)  │
│    very low latency needs   │  - Sync status                     │
│    with acceptable          │  - ETF price cache                 │
│    staleness                │                                    │
└─────────────────────────────┴────────────────────────────────────┘
```

**FinVerse's practical rule:**

If the cached data must be consistent across all running containers — use Redis. If the data is truly static (loaded once at startup, never changes during the process lifetime, like environment config or country tax rate tables), in-process is fine. Everything else: Redis.

The tax rate tables are a good example of correct in-process caching at FinVerse. EU tax rates for the 8 supported countries are loaded from PostgreSQL at Core Product startup and stored in a module-level `Map`. They change at most once a year (when tax laws update). During a deployment, the new container loads the new rates from PostgreSQL. No cache invalidation needed — the deployment cycle handles it naturally.

```typescript
// tax-rates.cache.ts — loaded once at NestJS module init
@Injectable()
export class TaxRatesCache implements OnModuleInit {
  private readonly rates = new Map<string, CountryTaxRates>()

  constructor(private readonly prisma: PrismaService) {}

  async onModuleInit(): Promise<void> {
    const taxRates = await this.prisma.countryTaxRate.findMany()
    taxRates.forEach(rate => {
      this.rates.set(rate.countryCode, rate)
    })
    this.logger.log(`Loaded tax rates for ${this.rates.size} countries`)
  }

  getRatesForCountry(countryCode: string): CountryTaxRates | undefined {
    return this.rates.get(countryCode)
  }
}
```

No Redis involved. No TTL. No invalidation. It just works because the data's change frequency aligns perfectly with the deployment cycle.

---

## Part 3 — How FinVerse Uses Redis: Complete Map

Redis at FinVerse serves six distinct purposes. Each has its own key namespace, TTL strategy, and eviction behaviour.

```
┌──────────────────────────────────────────────────────────────────────┐
│                   REDIS USAGE MAP — FINVERSE                         │
├─────────────────────────────┬──────────────┬─────────────────────────┤
│  Purpose                    │  Key Pattern │  TTL                    │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  Rate limiting counters     │  rl:{userId} │  Window duration        │
│  (API Gateway)              │  :{endpoint} │  (60s, 600s etc.)       │
│                             │  :{window}   │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  OTP verification codes     │  otp:{userId}│  5 minutes              │
│  (Auth module)              │              │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  Access token blocklist     │  bl:{jti}    │  Remaining token        │
│  (logout / revoke)          │              │  lifetime               │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  ETF price cache            │  mkt:price:  │  20 minutes             │
│  (Market Data Service)      │  {symbol}    │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  Portfolio valuation cache  │  pf:val:     │  20 minutes             │
│  (Market Data Service)      │  {userId}    │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  User preferences cache     │  usr:prefs:  │  1 hour                 │
│  (Core Product)             │  {userId}    │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  Budget alert dedup         │  alert:bgt:  │  24 hours               │
│  (Notification Service)     │  {userId}:   │                         │
│                             │  {category}: │                         │
│                             │  {date}      │                         │
├─────────────────────────────┼──────────────┼─────────────────────────┤
│  BullMQ job state           │  bull:*      │  Managed by BullMQ      │
│  (All worker services)      │              │  (removeOnComplete etc) │
└─────────────────────────────┴──────────────┴─────────────────────────┘
```

---

## Part 4 — Caching Strategies: Which Pattern, When and Why

### Strategy 1 — Cache-Aside (Lazy Loading)

**How it works:**

```
REQUEST ARRIVES
      │
      ▼
Check Redis cache
      │
  ┌───┴────────────────────────┐
  │ HIT                        │ MISS
  │                            │
  ▼                            ▼
Return cached value    Query PostgreSQL
      │                        │
      │                        ▼
      │               Store result in Redis
      │               (with TTL)
      │                        │
      └───────────┬────────────┘
                  │
                  ▼
            Return to caller
```

**Code — user preferences cache in Core Product:**

```typescript
// user-preferences.service.ts
@Injectable()
export class UserPreferencesService {

  constructor(
    private readonly prisma: PrismaService,
    @InjectRedis() private readonly redis: Redis
  ) {}

  async getUserPreferences(userId: string): Promise<UserPreferences> {
    const cacheKey = `usr:prefs:${userId}`

    // Step 1: Try Redis first
    const cached = await this.redis.get(cacheKey)
    if (cached) {
      return JSON.parse(cached)   // cache HIT — return immediately
    }

    // Step 2: Cache MISS — query PostgreSQL
    const prefs = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        subscriptionTier:    true,
        country:             true,
        currency:            true,
        notificationPrefs:   true,
      }
    })

    if (!prefs) {
      throw new NotFoundException('USER_NOT_FOUND')
    }

    // Step 3: Store in Redis with TTL
    await this.redis.set(
      cacheKey,
      JSON.stringify(prefs),
      'EX', 3600    // 1 hour TTL
    )

    return prefs
  }
}
```

**When Cache-Aside is the right choice:**
- Read-heavy data with infrequent writes (user preferences, account settings)
- You can tolerate some staleness (up to the TTL duration)
- Cache misses are acceptable (first request pays the full DB cost)
- The cached object is self-contained — you can reconstruct it from a single source

**The trade-off:** on a cache miss, the request takes the full PostgreSQL query time. Under high load with a cold cache (after deployment or Redis restart), every concurrent request for the same data misses simultaneously and hammers the database. This is the **Cache Stampede** problem — covered in Part 6.

---

### Strategy 2 — Write-Through

**How it works:** Every write to the database also writes to the cache synchronously. The cache is always up to date — no misses after the first population.

```
WRITE REQUEST
      │
      ▼
Write to PostgreSQL
      │
      ▼
Write to Redis cache   ← same data, same transaction (as close as possible)
      │
      ▼
Return success
```

**Code — updating user notification preferences:**

```typescript
async updateNotificationPreferences(
  userId: string,
  updates: UpdateNotificationPrefsDto
): Promise<void> {

  // Step 1: Update PostgreSQL (source of truth)
  const updatedPrefs = await this.prisma.notificationPreference.update({
    where: { userId },
    data: updates
  })

  // Step 2: Immediately update the cache
  // Write-through: don't wait for the cache to expire naturally
  const cacheKey = `usr:prefs:${userId}`
  await this.redis.set(
    cacheKey,
    JSON.stringify(updatedPrefs),
    'EX', 3600
  )
}
```

**When Write-Through is the right choice:**
- Data that is read extremely frequently right after being written
- Low tolerance for stale reads (e.g. subscription tier — if a user just upgraded to Premium, they must see Premium features immediately)
- Write frequency is manageable (write-through adds latency to every write)

**The trade-off:** every write is slightly slower because it involves two operations — PostgreSQL and Redis. If you write data that is rarely or never read back from cache, you are paying the write cost for no benefit. Write-through is best combined with Cache-Aside for reads — the cache always has fresh data, and reads always hit it.

**The critical case — subscription tier:**

When a user upgrades from Free to Premium, their `subscriptionTier` in the `user` table changes. Every subsequent request checks this tier for feature gates. With Cache-Aside alone, the cached value would show FREE for up to 1 hour — the user just paid and cannot access Premium features. This is a critical bug.

Write-through on the subscription update ensures the cache is immediately consistent:

```typescript
// subscription.service.ts
async activatePremium(userId: string): Promise<void> {

  // Update PostgreSQL
  await this.prisma.user.update({
    where: { id: userId },
    data: { subscriptionTier: 'PREMIUM' }
  })

  // Write-through: update cache immediately
  // Cannot afford even 1-second staleness here
  const cacheKey = `usr:prefs:${userId}`
  const cached = await this.redis.get(cacheKey)

  if (cached) {
    const prefs = JSON.parse(cached)
    prefs.subscriptionTier = 'PREMIUM'
    await this.redis.set(cacheKey, JSON.stringify(prefs), 'EX', 3600)
  }
  // If no cache exists yet, next read will populate it via Cache-Aside
}
```

---

### Strategy 3 — Write-Behind (Write-Back)

**How it works:** Writes go to the cache immediately and to the database asynchronously — the write is considered complete once Redis is updated, and a background process flushes changes to PostgreSQL later.

```
WRITE REQUEST
      │
      ▼
Write to Redis cache ← immediate, fast
      │
      ▼
Return success immediately

Background process (every N seconds):
      │
      ▼
Flush pending Redis writes to PostgreSQL
```

**Does FinVerse use Write-Behind?**

No — and it is important to understand why.

Write-Behind introduces a window of data loss. If Redis crashes or is evicted before the background flush happens, writes are permanently lost. For financial data — transactions, account balances, investment orders, user preferences — data loss is unacceptable. The risk profile does not justify the latency improvement.

Write-Behind makes sense for high-volume, low-stakes writes where some loss is acceptable — website analytics event counters, real-time leaderboard scores, view counts. None of FinVerse's write patterns fit this profile.

**The honest interview answer:** "We evaluated write-behind for budget `spent` counter updates — there are potentially hundreds of transaction categorisation events per second during sync. But losing even a single update would silently corrupt a user's budget. We went with synchronous PostgreSQL writes for all financial data. The write latency is acceptable — budget updates are part of a BullMQ background job, not in the request-response path."

---

## Part 5 — How Data is Stored in Redis: Format, Structure, Best Practices

### Key Design

Every Redis key at FinVerse follows a deliberate naming convention:

```
{namespace}:{entity}:{identifier}:{optional-qualifier}

Examples:
  usr:prefs:usr_abc123               — user preferences
  mkt:price:VWCE.DE                  — ETF price
  pf:val:usr_abc123                  — portfolio valuation
  rl:usr_abc123:/v1/accounts:1735900 — rate limit counter
  otp:usr_abc123                     — OTP code
  bl:jti_abc123                      — blocklisted JWT
  alert:bgt:usr_abc123:Dining:2024-01-15 — budget alert dedup
```

**Why structured namespaces matter:**

```
BENEFITS OF NAMESPACE CONVENTION

1. Redis SCAN by namespace
   SCAN 0 MATCH "usr:prefs:*" COUNT 100
   → finds all user preference keys without scanning all keys
   → useful for bulk invalidation (e.g. when user account is deleted)

2. Prevents key collision between services
   Core Product, Notification Service, and Market Data Service
   all write to the same Redis cluster.
   Without namespaces, a key "price:VWCE.DE" from Market Data
   could collide with something else.

3. Monitoring by namespace
   Datadog Redis integration tags metrics by key prefix.
   You can see: "usr:prefs cache hit ratio: 94%"
   separately from "mkt:price cache hit ratio: 99%"

4. TTL reasoning per namespace
   Different namespaces have very different TTLs.
   The naming makes it obvious what TTL to expect.
```

---

### Data Format — JSON Strings vs Redis Native Types

**Simple cached objects — JSON string:**

Most cached values at FinVerse are serialised as JSON strings:

```typescript
// Writing
await this.redis.set(
  `usr:prefs:${userId}`,
  JSON.stringify(userPrefs),
  'EX', 3600
)

// Reading
const raw = await this.redis.get(`usr:prefs:${userId}`)
const prefs = raw ? JSON.parse(raw) as UserPreferences : null
```

**Why JSON strings over Redis Hash (HSET/HGET):**

Redis Hashes let you store objects field-by-field and read individual fields with HGET. Sounds efficient. But in practice:

```
HASH approach:
  HSET usr:prefs:usr_abc123 subscriptionTier PREMIUM country DE currency EUR
  → Reading one field: HGET usr:prefs:usr_abc123 subscriptionTier

JSON string approach:
  SET usr:prefs:usr_abc123 '{"subscriptionTier":"PREMIUM","country":"DE","currency":"EUR"}'
  → Reading: GET → JSON.parse

For FinVerse's use case:
  We almost always read ALL fields of the cached object at once.
  A single GET + JSON.parse is one Redis command.
  HGETALL (get all hash fields) is also one Redis command.
  Performance is equivalent.

  But JSON serialisation is simpler:
  - Standard JSON.parse/stringify everywhere
  - Type-safe with TypeScript interfaces
  - No field mapping code needed
  - Partial updates require full re-serialise anyway
    (we always update the whole object, not individual fields)
```

**Counters — Redis native integers:**

Rate limiting counters use Redis native integer operations:

```typescript
// Rate limit counter — Redis INCR is atomic
const count = await this.redis.incr(key)  // atomic increment, returns new value
if (count === 1) {
  await this.redis.expire(key, windowSeconds)  // set TTL on first increment
}
```

Why not JSON here? Because `INCR` is an atomic operation — it reads and increments in a single command. With JSON, you would need to GET, parse, increment, stringify, and SET — five operations, not atomic. Under concurrent requests, two requests could both GET the same value, both increment, and both SET the same result — effectively one increment instead of two. Race condition.

**Sets — Redis native Set (SADD/SMEMBERS):**

The access token blocklist uses a Redis Set:

```typescript
// On logout — add token ID to blocklist
await this.redis.sadd(`bl:tokens`, jti)
// Set TTL equal to the token's remaining lifetime
await this.redis.expire(`bl:tokens`, remainingLifetimeSeconds)

// On authenticated request — check if token is blocklisted
const isBlocked = await this.redis.sismember(`bl:tokens`, jti)
```

**Sorted Sets — used by BullMQ internally (not by application code):**

BullMQ's delayed and completed job queues use Redis ZSETs internally — covered in the BullMQ module. Application code never writes to these directly.

---

### TTL Strategy — How Expiry Is Decided

Every key at FinVerse has a TTL. No permanent keys in the application layer. The TTL is chosen based on:

```
TTL DECISION FRAMEWORK

Question 1: How often does this data change?
  Changes multiple times per minute → TTL: 30-60 seconds
  Changes at most every few hours   → TTL: 15-60 minutes
  Changes at most daily             → TTL: 1-24 hours
  Changes rarely (weeks/months)     → TTL: 24+ hours

Question 2: What is the cost of serving stale data?
  User sees wrong balance    → low TTL (or write-through)
  User sees slightly old ETF price → 15-20 min TTL acceptable
  User sees correct subscription tier → write-through required

Question 3: What is the cost of a cache miss?
  Expensive DB query     → longer TTL to reduce miss rate
  Cheap DB query         → shorter TTL acceptable

FINVERSE TTL DECISIONS:

  OTP codes: 5 minutes
    → Security-sensitive. Short enough to limit brute force window.
    → User expects to use it immediately.

  Rate limit counters: equal to window duration (60s, 600s)
    → Must expire when the window closes. Natural fit.

  User preferences: 1 hour
    → Changes infrequently. 1 hour staleness acceptable.
    → Write-through on critical updates (subscription tier).

  ETF prices: 20 minutes
    → Market Data Service polls every 15 minutes.
    → 20 minute TTL = always-fresh between polls + 5 min buffer
    → If polling misses a cycle, cache doesn't expire immediately.

  Portfolio valuations: 20 minutes
    → Recomputed when ETF prices refresh.
    → TTL matches ETF price TTL.

  Budget alert dedup: 24 hours
    → Prevents duplicate alerts on the same day.
    → Resets at midnight naturally via TTL expiry.
```

---

## Part 6 — Production Cache Failure Modes

This is where interviewers separate candidates who have read about caching from candidates who have thought about it in production. Three failure modes are universally asked about.

---

### Cache Stampede (Thundering Herd)

**The problem:**

```
CACHE STAMPEDE SCENARIO

10:00:00 — ETF price cache key for "VWCE.DE" expires (TTL reached)

10:00:01 — 500 concurrent portfolio valuation requests arrive
           All check Redis: MISS (key just expired)
           All decide to query EODHD/Market Data Service to repopulate
           500 simultaneous requests hit the same downstream source
           EODHD rate limit exceeded → all 500 fail
           Error cascades → all portfolio screens show errors
```

The core problem: when a popular key expires, every concurrent request that misses the cache simultaneously tries to repopulate it. The downstream system gets hit with a burst it wasn't designed to handle.

**FinVerse's solution — the Lock-Based Repopulation pattern:**

Only one request is allowed to repopulate a given cache key. All others wait for that one request to finish and then read the freshly populated cache.

```typescript
// cache.service.ts
@Injectable()
export class CacheService {

  constructor(
    @InjectRedis() private readonly redis: Redis
  ) {}

  async getOrSet<T>(
    key: string,
    ttlSeconds: number,
    fetchFn: () => Promise<T>
  ): Promise<T> {

    // Step 1: Try to read from cache
    const cached = await this.redis.get(key)
    if (cached) {
      return JSON.parse(cached)    // cache HIT
    }

    // Step 2: Cache MISS — acquire a distributed lock
    // Only ONE process wins the lock and repopulates
    const lockKey = `lock:${key}`
    const lockToken = crypto.randomUUID()
    const lockAcquired = await this.redis.set(
      lockKey,
      lockToken,
      'EX', 10,    // lock expires after 10 seconds (safety)
      'NX'         // only set if key does NOT exist (atomic)
    )

    if (lockAcquired) {
      // This process won the lock — it is responsible for repopulation
      try {
        const freshData = await fetchFn()    // call the real data source

        await this.redis.set(
          key,
          JSON.stringify(freshData),
          'EX', ttlSeconds
        )

        return freshData

      } finally {
        // Always release the lock — even if fetchFn threw
        // Only release if we still own it (check token)
        const currentToken = await this.redis.get(lockKey)
        if (currentToken === lockToken) {
          await this.redis.del(lockKey)
        }
      }
    }

    // This process did NOT win the lock — another is repopulating
    // Wait and retry
    await new Promise(resolve => setTimeout(resolve, 100))   // wait 100ms
    return this.getOrSet(key, ttlSeconds, fetchFn)           // retry
  }
}
```

**How it is used in the portfolio service:**

```typescript
async getPortfolioValuation(userId: string): Promise<PortfolioValuation> {
  return this.cacheService.getOrSet(
    `pf:val:${userId}`,
    1200,                           // 20 minutes TTL
    () => this.marketDataService.computeValuation(userId)
  )
}
```

The lock means even under 500 concurrent requests for the same expired key, only one will call `computeValuation`. The other 499 will wait 100ms, retry, and find the freshly populated cache.

**Java parallel:** this is a distributed equivalent of `synchronized` blocks or `ReentrantLock` — mutual exclusion, but across processes via Redis instead of within a single JVM.

---

### Cache Avalanche

**The problem:**

```
CACHE AVALANCHE SCENARIO

After a Redis restart or major deployment:
  ALL cache keys are empty (cold cache)

08:30 — Users start their day, opening the app simultaneously
  → Every request misses the cache
  → Every request hits PostgreSQL directly
  → PostgreSQL connection pool exhausted
  → Query response times spike to 5-10 seconds
  → Some requests timeout
  → Error rate spikes
  → Users see loading errors on the home screen
```

Cache Avalanche differs from Cache Stampede. Stampede is one popular key expiring. Avalanche is many keys expiring at the same time — typically after a cache wipe or when all keys are set with the same TTL and all expire together.

**FinVerse's mitigations:**

**Mitigation 1 — TTL jitter (randomised expiry):**

Instead of all user preference keys expiring at exactly 3600 seconds, each gets a slightly randomised TTL:

```typescript
// Instead of fixed TTL:
await this.redis.set(key, value, 'EX', 3600)

// With jitter: ±10% of base TTL
const baseTtl = 3600
const jitter = Math.floor(Math.random() * baseTtl * 0.2) - (baseTtl * 0.1)
// jitter is between -360 and +360
const ttl = baseTtl + jitter
await this.redis.set(key, value, 'EX', ttl)
```

If all user preference keys were set with the same TTL during a mass-login event, they would all expire at the same time one hour later — avalanche. With jitter, keys expire spread across a 20-minute window around the nominal TTL.

**Mitigation 2 — Graceful degradation, not hard failure:**

For non-critical cached data, if Redis is down or returns an error, the service falls back to PostgreSQL rather than throwing an exception:

```typescript
async getUserPreferences(userId: string): Promise<UserPreferences> {
  try {
    const cached = await this.redis.get(`usr:prefs:${userId}`)
    if (cached) return JSON.parse(cached)
  } catch (redisError) {
    // Redis is unavailable — log it, but don't fail the request
    this.logger.warn(`Redis unavailable for user preferences`, {
      userId, error: redisError.message
    })
    // Fall through to PostgreSQL
  }

  // Direct PostgreSQL read as fallback
  const prefs = await this.prisma.user.findUnique({
    where: { id: userId },
    select: { subscriptionTier: true, country: true, currency: true }
  })

  return prefs
}
```

The application degrades gracefully — responses are slower but correct. Users experience latency, not errors.

**Mitigation 3 — Cache warming on deployment:**

For the most critical and heavily-read data (ETF prices, active user preferences), the deployment pipeline runs a cache warming step after the new containers start:

```typescript
// cache-warmer.service.ts — runs on NestJS module init
@Injectable()
export class CacheWarmerService implements OnModuleInit {
  async onModuleInit(): Promise<void> {
    // Pre-populate user preferences for MAU (monthly active users)
    // Runs once at startup — takes ~30 seconds for 180k MAUs
    // By the time real traffic arrives, cache is warm
    await this.warmUserPreferences()
  }

  private async warmUserPreferences(): Promise<void> {
    const activeUserIds = await this.prisma.user.findMany({
      where: {
        lastActiveAt: { gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) }
      },
      select: { id: true },
      take: 50_000     // warm top 50k active users
    })

    for (const batch of chunk(activeUserIds, 100)) {
      await Promise.all(
        batch.map(({ id }) => this.userPreferencesService.getUserPreferences(id))
      )
    }
  }
}
```

Cache warming trades startup time (slightly longer deployment) for request reliability (no cold start avalanche for the first wave of morning users).

---

### Hot Key Problem

**The problem:**

```
HOT KEY SCENARIO

A single Redis key gets hit by an enormous number of concurrent reads.

Example at FinVerse: the ETF price for "VWCE.DE"
  → Used in portfolio valuations for ~40% of all FinVerse users
  → 08:30 morning spike: 5,000 requests/second all reading
    "mkt:price:VWCE.DE" from the same Redis node
  → A single Redis node handles ~100,000 simple GET ops/second
  → 5,000 req/s reading one key = 5% of that node's capacity
    consumed by ONE key
  → Under extreme load, this can cause latency spikes on the
    Redis node affecting all other keys hosted on it
```

Hot keys are less of an issue at FinVerse's current scale (Series A, 180,000 MAUs) than they would be at Series B targets. But the pattern is worth understanding — interviewers at larger companies ask this frequently.

**Mitigation — Local cache for ultra-hot read-only data:**

For data like ETF prices that are read-only within a request cycle (the same price is read thousands of times but never written by the application layer), a request-scoped in-process cache eliminates the Redis read entirely:

```typescript
// portfolio.service.ts
async computePortfolioValuation(
  userId: string,
  holdings: Holding[]
): Promise<PortfolioValuation> {

  // Request-scoped price cache: one Redis read per symbol,
  // even if the same symbol appears in 1000 users' portfolios
  const priceCache = new Map<string, Decimal>()

  const getPrice = async (symbol: string): Promise<Decimal> => {
    if (priceCache.has(symbol)) {
      return priceCache.get(symbol)!    // in-process hit — zero Redis call
    }
    const price = await this.marketDataService.getPrice(symbol)
    priceCache.set(symbol, price)       // cache for this computation
    return price
  }

  const valuations = await Promise.all(
    holdings.map(async (holding) => {
      const currentPrice = await getPrice(holding.instrument.symbol)
      return {
        symbol:       holding.instrument.symbol,
        units:        holding.units,
        currentValue: holding.units.times(currentPrice),
        costBasis:    holding.units.times(holding.avgBuyPrice),
      }
    })
  )

  return this.aggregateValuation(valuations)
}
```

Within a single portfolio computation, `VWCE.DE` is only read from Redis once regardless of how many holdings reference it. The Map acts as a local buffer — not truly persistent cache, just eliminating redundant reads within one execution.

---

## Part 7 — Cache Invalidation

Cache invalidation is consistently described as one of the hardest problems in computer science — and interviewers at senior levels always probe it.

**The core tension:**

```
Too aggressive invalidation: cache is effectively useless
  → Every write blows away the cached value
  → Miss rate stays high
  → Database load stays high
  → No benefit from caching

Too conservative invalidation: cache serves stale data
  → User sees wrong balance, wrong tier, wrong preferences
  → Data inconsistency bugs that are hard to reproduce
  → Trust issues with the product

Goal: invalidate exactly the data that changed,
      at exactly the right time.
```

### FinVerse's Invalidation Patterns by Data Type

**Pattern 1 — TTL-based natural expiry (most common)**

For data where bounded staleness is acceptable, let the TTL do the work:

```typescript
// ETF prices: 20-minute TTL
// If a price updates, worst case: 20 minutes of stale data
// Acceptable for a long-term investment platform (not a trading platform)

// User preferences: 1-hour TTL
// If a user changes their notification setting,
// worst case: 1 hour delay before it takes effect
// Acceptable for most settings

// Budget alert dedup: 24-hour TTL
// Natural reset at end of day — by design
```

**Pattern 2 — Explicit invalidation on write (for critical data)**

For data where staleness is unacceptable, invalidate the cache key immediately on every write:

```typescript
// When user updates their profile:
async updateUserProfile(
  userId: string,
  updates: UpdateProfileDto
): Promise<void> {

  await this.prisma.user.update({
    where: { id: userId },
    data: updates
  })

  // Invalidate immediately — next read will repopulate via Cache-Aside
  await this.redis.del(`usr:prefs:${userId}`)
}
```

**Why delete instead of update (write-through)?**

Delete is simpler and safer for multi-field objects. If you update the cached JSON but miss one field (because the update DTO only contained a subset of fields), the cache now contains a stale partial object. Deleting the key forces a clean repopulation from the database on the next read — always a complete, consistent object.

The exception is write-through for subscription tier (covered in Part 4), where we explicitly update the cached object because we need immediate consistency without waiting for the next read.

**Pattern 3 — Event-driven invalidation (for cross-module data)**

When a write in one module should invalidate cached data managed by another module, the Outbox pattern coordinates the invalidation:

```typescript
// When a bank account sync completes and updates balances:
// The sync worker publishes an event via Outbox

await this.prisma.outboxEvent.create({
  data: {
    eventType: 'account.balance.updated',
    payload: { userId, accountIds: [accountId] }
  }
})

// The outbox publisher sends this to RabbitMQ
// Core Product (Net Worth endpoint) consumes it:

// net-worth.consumer.ts
async handleBalanceUpdated(event: AccountBalanceUpdatedEvent): Promise<void> {
  // Invalidate the net worth cache for this user
  // Next GET /v1/accounts/net-worth will recompute from fresh balances
  await this.redis.del(`nw:${event.userId}`)
}
```

This pattern keeps modules decoupled — the Accounts module does not import or directly call the Net Worth caching service. It publishes an event. The Net Worth module handles invalidation on its own terms.

---

### The Hardest Invalidation Case — Distributed Cache Inconsistency

This is the scenario that makes even senior engineers uncomfortable:

```
RACE CONDITION SCENARIO

T=0ms:  User A updates their notification preference (email off)
        → prisma.update() starts (takes 5ms to complete)

T=2ms:  User A's GET /v1/accounts request arrives (concurrent)
        → reads usr:prefs:usr_A from Redis → returns old pref (email ON)
        → request completes before the write finished

T=5ms:  prisma.update() completes
        → del('usr:prefs:usr_A') — cache invalidated

T=6ms:  Next GET reads from PostgreSQL → correct (email OFF)
        → repopulates cache

Between T=0ms and T=6ms, there was a brief window of stale data.
```

For FinVerse's use cases, this is acceptable. The window is milliseconds. The consequence — a user's notification preference taking effect on the next request rather than the exact concurrent one — is not a financial or security issue.

**Where this window is NOT acceptable:**

Payment confirmation. After a Stripe charge succeeds, the `subscriptionTier` in PostgreSQL changes to PREMIUM. If there is any window where the cache says FREE but the user paid for PREMIUM, the user hits a feature gate and sees a "Premium required" screen after just paying. This is a critical product bug.

The solution is already in place — write-through on subscription tier change, with an explicit `HSET` or JSON update of the cached object at the same moment as the PostgreSQL write. No window.

---

## Part 8 — Redis Configuration & Eviction

### How FinVerse Configures ElastiCache

```
AWS ElastiCache for Redis — FinVerse Configuration

Instance type: cache.r6g.large
  → 13.07 GB memory
  → Why r6g (memory-optimised): Redis is memory-bound.
    More RAM = more keys before eviction.

Multi-AZ: enabled
  → Primary in AZ-a, replica in AZ-b
  → Automatic failover in ~30 seconds if primary fails
  → Covered in Module 7 (BullMQ resilience)

TLS in transit: enabled
  → All Redis connections from ECS containers use TLS
  → Required for GDPR compliance (data in transit protection)

Encryption at rest: enabled
  → Keys stored on disk (AOF persistence) are encrypted

AOF persistence: enabled
  → Every write appended to disk
  → Survives Redis process restart
  → Slight performance cost — acceptable for financial platform

Max memory: 10 GB (leaving 3 GB headroom)
```

### Eviction Policy — What Happens When Redis Gets Full

If the Redis instance runs out of memory and a new write arrives, Redis must evict (delete) existing keys to make space. The eviction policy determines which keys get evicted.

```
REDIS EVICTION POLICIES — WHAT THEY MEAN

noeviction:
  → Redis returns an error on writes when full
  → NEVER use in production — your app starts failing writes

allkeys-lru (Least Recently Used):
  → Evict the key that has not been accessed for the longest time
  → Works across ALL keys, regardless of TTL
  → Good general-purpose choice

volatile-lru:
  → Evict LRU key, but ONLY from keys that have a TTL set
  → Keys without TTL are never evicted
  → FinVerse's choice — BullMQ keys (managed carefully by BullMQ)
    and any permanent config keys are protected

volatile-ttl:
  → Evict the key with the shortest remaining TTL first
  → Can be dangerous — rate limit counters have short TTLs
    and would be evicted first, breaking rate limiting

FINVERSE CHOICE: volatile-lru

Reasoning:
  All application cache keys have TTLs.
  BullMQ keys also have TTLs (managed by removeOnComplete).
  When memory pressure occurs, the least-recently-used
  cache key is evicted — exactly the right behaviour.
  
  Under memory pressure, stale / unpopular cache entries
  are removed first. Active keys stay.
  The cache degrades gracefully under pressure rather
  than failing writes.
```

---

## Part 9 — Monitoring Cache Performance

An interviewer who asks "how did you measure your caching improvements?" expects specific metrics, not vague claims.

**Key metrics FinVerse monitors in Datadog:**

```
┌──────────────────────────────────────────────────────────────────┐
│                   CACHE METRICS IN DATADOG                       │
├─────────────────────────────────┬────────────────────────────────┤
│  Metric                         │  Target / Alert Threshold      │
├─────────────────────────────────┼────────────────────────────────┤
│  Cache hit rate (per namespace) │  Target: >90%                  │
│                                 │  Alert: <70% (cache not        │
│                                 │  working as expected)          │
├─────────────────────────────────┼────────────────────────────────┤
│  Redis memory usage             │  Alert: >80% of max (8GB)      │
│                                 │  (approaching eviction)        │
├─────────────────────────────────┼────────────────────────────────┤
│  Redis eviction count           │  Alert: >0 per minute          │
│                                 │  (memory pressure happening)   │
├─────────────────────────────────┼────────────────────────────────┤
│  Redis latency (p95)            │  Target: <5ms                  │
│                                 │  Alert: >20ms                  │
├─────────────────────────────────┼────────────────────────────────┤
│  PostgreSQL query rate          │  Correlated with cache hits:   │
│                                 │  high hit rate → lower DB load │
│                                 │  Sudden DB spike suggests      │
│                                 │  cache failure                 │
└─────────────────────────────────┴────────────────────────────────┘
```

**How hit rate is instrumented in Core Product:**

```typescript
// cache.service.ts — wraps every cache read with metrics
async getOrSet<T>(key: string, ttl: number, fetchFn: () => Promise<T>): Promise<T> {
  const namespace = key.split(':')[0]   // e.g. "usr", "mkt", "pf"

  const cached = await this.redis.get(key)

  if (cached) {
    // Record HIT
    datadogClient.increment('cache.hit', 1, [`namespace:${namespace}`])
    return JSON.parse(cached)
  }

  // Record MISS
  datadogClient.increment('cache.miss', 1, [`namespace:${namespace}`])

  const fresh = await fetchFn()
  await this.redis.set(key, JSON.stringify(fresh), 'EX', ttl)
  return fresh
}
```

Hit rate is then `cache.hit / (cache.hit + cache.miss)` per namespace. This can be queried in Datadog as a formula metric, giving a real-time dashboard showing which namespaces are performing well and which aren't.

**How this translates to interview claims:**

"After introducing user preference caching with a 1-hour TTL, Datadog showed the `usr:prefs` namespace hit rate at 94% during peak hours. PostgreSQL query volume for the user table dropped by approximately 80% during morning peak windows, which we measured by comparing the `pg.queries.count` metric for the `users` table before and after the caching deployment."

That is a defensible, specific, measurable claim. The interviewer can ask exactly how you measured it, and you can explain.

---

Step 9 is complete and ready to be saved as `Caching.md` in `Module 9: Caching`.

**Ready for Step 10: Transaction Handling — PostgreSQL + Prisma, concurrency, isolation levels, and the async edge cases with BullMQ.**