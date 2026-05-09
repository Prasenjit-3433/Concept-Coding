# Spring Boot: JPA — Second Level Caching (L2 Cache)

I've read through both the transcript and the PDF notes in detail. Here's the plan for how we'll cover this topic step by step:

**Step 1 — The Problem: Why L1 Cache Isn't Enough**
**Step 2 — What is L2 Cache & How it Solves the Problem**
**Step 3 — Setting Up L2 Cache (Dependencies + Config)**
**Step 4 — The Happy Flow (How it works end to end)**
**Step 5 — Cache Regions (What they are & why they matter)**
**Step 6 — Cache Concurrency Strategies (All 4, with deep dives)**
**Step 7 — Interview Tips & Tricks**

---

Let's start with **Step 1**.

---

## Step 1 — The Problem: Why L1 Cache Isn't Enough

### Quick Recap: What is L1 (First Level) Cache?

Before diving into L2, let's make sure the foundation is solid. Here's the basic setup the instructor uses:

```
HTTP Request
     ↓
UserController  (POST /user, GET /user/{id})
     ↓
UserDetailsService
     ↓
UserDetailsRepository  (extends JpaRepository)
     ↓
UserDetails  (Entity)
```

Now, whenever any HTTP request comes in, **a brand new EntityManager object is created**. Each EntityManager has its **own Persistence Context**, and this Persistence Context *is* the L1 cache.

```
HTTP Request 1 (GET)
     → EntityManager Object 1
          → Persistence Context 1  ← L1 Cache (only lives here)
               → DB hit (SELECT query)

HTTP Request 2 (GET) — same data, same ID
     → EntityManager Object 2
          → Persistence Context 2  ← completely new, knows nothing about Request 1
               → DB hit again (another SELECT query)
```

### The Problem

Even though you're fetching the **exact same data** twice, you still hit the database twice. That's because:

- Every new HTTP request = new EntityManager = new Persistence Context
- L1 cache is **scoped to a single EntityManager session**
- Once that session ends (request is done), the cache is gone
- The next request has zero awareness of what the previous one fetched

> 💬 *As the instructor puts it:* "It is not aware of this one where this object is present. It is only aware of its own persistence context."

### When does L1 Cache actually help then?

L1 cache only helps **within the same request**. For example, if inside a single transaction you call `findById(1L)` twice, the second call is a cache hit because the same EntityManager object is being used. But the moment you're in a new HTTP request — it's a fresh start.

```
Same Request, Same EntityManager:
   1st findById(1)  → cache MISS → DB hit → stored in Persistence Context
   2nd findById(1)  → cache HIT  → returned from Persistence Context ✅

Different Requests, Different EntityManagers:
   Request 1: findById(1) → cache MISS → DB hit
   Request 2: findById(1) → cache MISS → DB hit again ❌
```

### So What's the Fix?

We need a cache layer that is **shared across multiple EntityManagers / Persistence Contexts** — something that lives beyond a single request's lifetime. That's exactly what **Second Level (L2) Cache** is.

---

## Step 2 — What is L2 Cache & How it Solves the Problem

### Where Does L2 Cache Sit?

Without L2 cache, the flow looked like this:

```
Request → EntityManager → Persistence Context (L1) → DB
```

With L2 cache, a new shared layer is introduced **between the Persistence Context and the DB**:

```
Request → EntityManager → Persistence Context (L1) → L2 Cache → DB
```

The key word here is **shared**. L1 is private to each EntityManager. L2 is shared across all of them.

---

### The Big Picture Diagram

```
HTTP Request 1 (POST)
     → EntityManager 1 → Persistence Context 1 ──────────────────→ DB
                                                                     ↑
                                                                     |
HTTP Request 2 (GET)                                                 |
     → EntityManager 2 → Persistence Context 2 → L2 Cache ──────────|
                                                    ↑  ↓
HTTP Request 3 (GET)                                |  |
     → EntityManager 3 → Persistence Context 3 → L2 Cache
                                                (SHARED)
```

So now:
- Request 2 checks its own L1 (miss) → checks L2 (miss, first time) → hits DB → **loads data into L2**
- Request 3 checks its own L1 (miss) → checks L2 → **HIT! Returns from L2, no DB call needed** ✅

---

### The Lookup Order (Very Important)

Every time a GET request comes in, here's the exact sequence JPA follows:

```
Incoming GET request
        ↓
1. Check Persistence Context (L1 Cache)
        ↓ miss
2. Check L2 Cache (shared)
        ↓ miss
3. Hit the Database
        ↓
4. Load data into L2 Cache
        ↓
5. Load data into L1 Cache (Persistence Context)
        ↓
6. Return data to caller
```

> 💬 The instructor says: "Whatever the data comes from DB, it first goes to L2 cache, then L1 cache, and then out."

---

### What About INSERT?

This is a common confusion point. The instructor is very clear about this:

> **During an INSERT, data goes directly to the DB. L2 cache is NOT involved at all.**

L2 cache only comes into the picture on a **GET (read) operation**. The very first GET after an INSERT will always be a cache miss, because nothing was put into L2 during the insert.

```
POST (insert)   → directly goes to DB
                  L2 Cache = still empty ❌

1st GET         → L1 miss → L2 miss → DB hit
                  L2 Cache = now populated ✅

2nd GET (new request) → L1 miss (new EntityManager)
                        → L2 HIT ✅ → no DB call needed
```

---

### Why is L2 Cache Fast?

L2 cache is **in-memory**, just like L1. So reading from it is much faster than going all the way to the database. The difference is just scope — L1 is private per session, L2 is shared across all sessions.

---

### Real World Usage

> 💬 The instructor mentions: *"It's not just a concept — it's used in production level code. Very frequently asked in Big MNC interviews."*

L2 caching is typically used for:
- Data that is read frequently but updated rarely (e.g. product catalog, config data, user profile)
- Scenarios where DB load needs to be reduced significantly
- High traffic applications where the same rows are fetched by many concurrent users

---

## Step 3 — Setting Up L2 Cache (Dependencies + Configuration)

### Overview of What Changes

The instructor is very clear that the changes needed are **minimal**. Here's a quick map of what changes and what doesn't:

```
UserController        → NO CHANGE
UserDetailsService    → NO CHANGE
UserDetailsRepository → NO CHANGE
UserDetails (Entity)  → ADD @Cache annotation ✅
pom.xml               → ADD 3 dependencies ✅
application.properties → ADD 3 configurations ✅
ehcache.xml           → NEW FILE (optional but recommended) ✅
```

---

### Part 1 — The 3 Dependencies in pom.xml

```xml
<!-- Dependency 1 -->
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
    <version>3.10.8</version>
</dependency>

<!-- Dependency 2 -->
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-jcache</artifactId>
    <version>6.5.2.Final</version>
</dependency>

<!-- Dependency 3 -->
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

Now, why exactly do we need all three? The instructor explains this very clearly. Let's understand the **relationship between these three**:

```
Hibernate
    ↓
    talks to
    ↓
hibernate-jcache  ←── Acts as a BRIDGE / ORCHESTRATOR
    ↓
    talks to
    ↓
JCache APIs (cache-api)  ←── INTERFACES (loose coupling)
    ↓
    implemented by
    ↓
Ehcache / Caffeine / Hazelcast  ←── ACTUAL PROVIDER (core implementation)
```

**Dependency 1 — `ehcache`**
- This is the **actual cache provider**
- It does the real work — storing, retrieving, evicting cache entries
- You can swap this out with Caffeine or Hazelcast — your code won't change at all
- Think of it as the engine under the hood

**Dependency 2 — `cache-api`**
- This provides the **JCache interfaces** (e.g. `getCache()`, `createCache()`, `putCache()`)
- Hibernate doesn't talk to Ehcache directly — it talks to these interfaces
- This is what gives you **loose coupling** — tomorrow if you switch from Ehcache to Caffeine, your code stays the same because you're always talking to the interface, not the implementation

> 💬 *As the instructor puts it:* "Who is the provider? It can change. So this dependency brings the API interfaces that your Hibernate code interacts with."

**Dependency 3 — `hibernate-jcache`**
- This is the **bridge between Hibernate and the JCache interfaces**
- Hibernate on its own doesn't know *when* to call `getCache()`, *when* to call `putCache()`, or how to handle annotations like `@Cache` and `CacheConcurrencyStrategy`
- `hibernate-jcache` understands all of this — it acts as an **orchestrator**
- Based on what strategy you're using (READ_WRITE, READ_ONLY, etc.), it knows what methods to call and what validations to apply

> 💬 *The instructor says:* "Consider it as a bridge between Hibernate and Cache, which understands how to talk with the Cache APIs."

---

### Part 2 — application.properties

```properties
# 1. Enable second level cache (disabled by default)
spring.jpa.properties.hibernate.cache.use_second_level_cache=true

# 2. Tell Hibernate which factory class manages the cache regions
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory

# 3. Tell Spring Boot which cache provider to use
spring.jpa.properties.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider

# 4. (Optional) Enable debug logs to see cache hits/misses in console
logging.level.org.hibernate.cache.spi=DEBUG
```

Let's understand each one:

**Property 1 — Enable L2 Cache**
- By default, L2 caching is **false** in Hibernate
- Without this being `true`, nothing else matters — L2 simply won't function

**Property 2 — Region Factory Class**
- A "Region" is a logical grouping of cached data (more on this in Step 5)
- The Region Factory is responsible for **managing everything related to cache regions** — creation, TTL, eviction policy, etc.
- The instructor recommends using `JCacheRegionFactory` (from `hibernate-jcache`) rather than pointing directly to Ehcache's factory
- Why? Because `JCacheRegionFactory` works through the JCache interfaces, so if you ever switch providers, **you don't have to change this property**

```
Option A (recommended):
factory_class = JCacheRegionFactory  ← works via interfaces, provider-agnostic ✅

Option B (direct):
factory_class = EhcacheRegionFactory ← tightly coupled to Ehcache ❌
```

**Property 3 — Cache Provider**
- Even after adding the Ehcache dependency, Hibernate doesn't automatically know which provider to use
- This property explicitly tells it: *"use Ehcache as your caching provider"*
- If you switch to Caffeine tomorrow, you'd update just this one line

**Property 4 — Debug Logging**
- This is just for development/testing
- It prints cache hits, cache misses, and cache loads in your console so you can actually see L2 cache in action

---

### Part 3 — Entity Change (@Cache annotation)

```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "userDetailsCache"
)
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // Constructors, Getters, Setters
}
```

This is the only change needed at the code level. The `@Cache` annotation tells Hibernate:

> *"For this entity, before touching the DB, check the L2 cache first."*

Two things you set here:
- `usage` — the concurrency strategy (READ_ONLY, READ_WRITE, etc.) — covered in depth in Step 6
- `region` — the logical group this entity belongs to — covered in Step 5

---

### The Full Setup at a Glance

```
pom.xml
  ├── ehcache          → actual cache engine
  ├── cache-api        → JCache interfaces (loose coupling)
  └── hibernate-jcache → bridge/orchestrator between Hibernate & JCache

application.properties
  ├── use_second_level_cache = true   → turn it on
  ├── region.factory_class            → who manages regions
  └── javax.cache.provider            → which provider to use

UserDetails.java
  └── @Cache(usage = ..., region = "...") → opt this entity into L2 cache
```

---

## Step 4 — The Happy Flow (End to End Walkthrough)

This is where everything comes together. The instructor walks through a sequence of HTTP calls and explains exactly what happens at each step — what gets cached, when cache hits happen, and when DB is hit.

---

### The Sequence of Calls

```
1st Call → POST   /user      (INSERT)
2nd Call → GET    /user/1    (first read)
3rd Call → GET    /user/1    (second read, new request)
```

Let's go through each one in detail.

---

### Call 1 — POST (INSERT)

```
POST /user
     ↓
EntityManager created
     ↓
Data inserted directly into DB
     ↓
L2 Cache = untouched ❌
L1 Cache = untouched ❌
```

**What happens:** Data goes straight to the database. The L2 cache is completely bypassed during an insert. Nothing is loaded into the cache at this point.

> 💬 *Instructor says:* "During Insert, data is directly inserted into DB. No cache insertion or validation happens."

**State after Call 1:**
```
DB          → has the data ✅
L2 Cache    → empty ❌
```

---

### Call 2 — GET (First Read)

```
GET /user/1
     ↓
New EntityManager created (new HTTP request)
     ↓
Check L1 Cache (Persistence Context)
     → MISS ❌ (brand new EntityManager, nothing here)
     ↓
Check L2 Cache
     → MISS ❌ (nothing was loaded during insert)
     ↓
Hit DB → SELECT query executed
     ↓
Data loaded into L2 Cache ✅
     ↓
Data loaded into L1 Cache ✅
     ↓
Data returned to caller
```

**What happens:** Since this is the very first GET, neither L1 nor L2 has anything. So it falls all the way through to the DB. But importantly, after fetching from DB, the data is now **loaded into L2 cache** so future requests can benefit from it.

**State after Call 2:**
```
DB          → has the data ✅
L2 Cache    → now has the data ✅
L1 Cache    → has the data (only for this request's lifetime)
```

---

### Call 3 — GET (Second Read, New Request)

```
GET /user/1
     ↓
New EntityManager created (new HTTP request)
     ↓
Check L1 Cache (Persistence Context)
     → MISS ❌ (brand new EntityManager again, fresh Persistence Context)
     ↓
Check L2 Cache
     → HIT ✅ (data was loaded here during Call 2)
     ↓
Data returned directly from L2 Cache
NO DB call made ✅
```

**What happens:** Even though L1 is empty (new request = new EntityManager), L2 is **shared across all EntityManagers**. So the data loaded in Call 2 is still sitting there in L2. Cache hit happens, DB is never touched.

> 💬 *Instructor says:* "L2 is same for multiple persistence contexts. Now it will check L2 — data is present from the previous call. So it would be cached and no DB hit required."

**State after Call 3:**
```
DB          → has the data ✅
L2 Cache    → still has the data ✅
L1 Cache    → has the data (only for this request's lifetime)
No SELECT query fired ✅
```

---

### The Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        COMPLETE FLOW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Call 1: POST                                                   │
│  ─────────────────────────────────────────────────────────────  │
│  Request → EntityManager1 → PersistenceContext1 ──────────→ DB  │
│                             (L1 - empty)          INSERT        │
│                                        L2 Cache = still empty   │
│                                                                 │
│  Call 2: GET (1st read)                                         │
│  ─────────────────────────────────────────────────────────────  │
│  Request → EntityManager2 → PersistenceContext2                 │
│                             L1: MISS ❌                          │
│                                  ↓                              │
│                             L2 Cache: MISS ❌                    │
│                                  ↓                              │
│                             DB: SELECT fired                    │
│                                  ↓                              │
│                             L2 Cache: data loaded ✅             │
│                             L1 Cache: data loaded ✅             │
│                                  ↓                              │
│                             data returned                       │
│                                                                 │
│  Call 3: GET (2nd read, new request)                            │
│  ─────────────────────────────────────────────────────────────  │
│  Request → EntityManager3 → PersistenceContext3                 │
│                             L1: MISS ❌ (new EntityManager)      │
│                                  ↓                              │
│                             L2 Cache: HIT ✅                     │
│                                  ↓                              │
│                             data returned, NO DB call           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### What You'll See in the Console

With `logging.level.org.hibernate.cache.spi=DEBUG` enabled, here's what the console output looks like:

```
Call 1 (POST):
  Hibernate: insert into user_details (email, name, id) values (?, ?, default)

Call 2 (GET - cache miss):
  Cache miss : region = 'userDetailsCache'
  Hibernate: select ud1_0.id, ud1_0.email, ud1_0.name
             from user_details ud1_0
             where ud1_0.id=?
  Caching data from load [region = 'userDetailsCache']

Call 3 (GET - cache hit):
  Cache hit : region = 'userDetailsCache'
  ← no SELECT query fired
```

This is exactly what the instructor shows in the demo. The absence of a SELECT query in Call 3 is your proof that L2 cache is working.

---

### Key Takeaways from the Happy Flow

```
INSERT  → always goes to DB, L2 cache untouched
1st GET → cache miss, DB hit, data loaded into L2
2nd GET → L1 miss (new request), L2 HIT, no DB call ✅
```

The whole point of L2 cache is captured in that last line — **different requests, different EntityManagers, but one shared cache layer that prevents unnecessary DB hits.**

---

## Step 5 — Cache Regions (What They Are & Why They Matter)

### What is a Region?

A **Region** is simply a **logical grouping of cached data**. Think of it like a named bucket inside the L2 cache. Each entity (or group of entities) can be assigned to its own region, and each region can have its own independent configuration.

> 💬 *Instructor says:* "Region helps in logical grouping of cached data. For each region we can apply different caching strategy like eviction policy, TTL, cache size, concurrency strategy."

---

### Why Do We Need Regions?

Imagine you have two entities in your application — `UserDetails` and `OrderDetails`. They have very different access patterns:

```
UserDetails
  - Read very frequently
  - Data doesn't change much
  - Can afford to keep in cache longer
  - Small dataset

OrderDetails
  - Read frequently but also updated often
  - Data changes more regularly
  - Needs a shorter cache lifetime
  - Large dataset
```

If both entities shared the same cache configuration, you'd be forced into a one-size-fits-all approach. That's not ideal. Regions solve this by letting you **configure each group independently**.

---

### How Regions Work in Code

**Entity 1 — UserDetails**
```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "userDetailsCache"    ← assigned to this region
)
public class UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    // Constructors, Getters, Setters
}
```

**Entity 2 — OrderDetails**
```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "orderDetailsCache"    ← assigned to a different region
)
public class OrderDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private int quantity;
    private double price;
    // Getters, Setters
}
```

Now these two entities live in **completely separate regions** inside L2 cache. Whatever configuration you apply to `userDetailsCache` has zero effect on `orderDetailsCache` and vice versa.

---

### Configuring Regions — ehcache.xml

This file goes inside `src/main/resources/` and is where you define the actual configuration for each region:

```xml
<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:noNamespaceSchemaLocation="http://www.ehcache.org/ehcache.xsd">

    <!-- Region 1: UserDetails -->
    <cache alias="userDetailsCache"
           maxElementsInMemory="100"
           timeToLiveSeconds="60"
           evictionStrategy="LIFO" />

    <!-- Region 2: OrderDetails -->
    <cache alias="orderDetailsCache"
           maxElementsInMemory="1000"
           timeToLiveSeconds="200"
           evictionStrategy="FIFO" />

</ehcache>
```

Notice how the `alias` here **matches exactly** the `region` name you gave in the `@Cache` annotation. That's how Hibernate knows which config applies to which entity.

---

### Breaking Down the Configuration Options

For each region you can independently control:

**1. maxElementsInMemory**
How many entries can be stored in this region at a time. Once this limit is hit, the eviction strategy kicks in to make room.

```
userDetailsCache  → max 100 entries
orderDetailsCache → max 1000 entries
```

**2. timeToLiveSeconds (TTL)**
How long a cached entry stays valid before it's automatically removed. After this time, the next request will be a cache miss and will fetch fresh data from DB.

```
userDetailsCache  → 60 seconds TTL
orderDetailsCache → 200 seconds TTL
```

**3. evictionStrategy**
When the cache is full (max entries reached), which entry gets removed to make room for a new one?

```
LIFO → Last In First Out
        the most recently added entry gets removed first

FIFO → First In First Out
        the oldest entry gets removed first

LRU  → Least Recently Used
        the entry that hasn't been accessed for the longest time gets removed
```

---

### The Full Picture — Regions in L2 Cache

```
┌──────────────────────────────────────────────────────────────┐
│                        L2 CACHE                              │
│                                                              │
│  ┌──────────────────────────┐  ┌─────────────────────────┐   │
│  │ Region: userDetailsCache │  │Region: orderDetailsCache│   │
│  │                          │  │                         │   │
│  │  TTL        : 60s        │  │  TTL        : 200s      │   │
│  │  Max entries: 100        │  │  Max entries: 1000      │   │
│  │  Eviction   : LIFO       │  │  Eviction   : FIFO      │   │
│  │                          │  │                         │   │
│  │  [UserDetails id=1]      │  │  [OrderDetails id=1]    │   │
│  │  [UserDetails id=2]      │  │  [OrderDetails id=2]    │   │
│  │  ...                     │  │  ...                    │   │
│  └──────────────────────────┘  └─────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
         ↑                                ↑
  Managed by                       Managed by
  JCacheRegionFactory              JCacheRegionFactory
```

---

### What Does the Region Factory Actually Do?

This connects back to the `application.properties` config from Step 3:

```properties
spring.jpa.properties.hibernate.cache.region.factory_class=
    org.hibernate.cache.jcache.JCacheRegionFactory
```

The `JCacheRegionFactory` is responsible for:

```
→ Creating cache regions on application startup
→ Applying the correct TTL, eviction policy, and max size per region
→ Routing each entity to its correct region
→ Managing cache operations (get, put, invalidate) per region
```

> 💬 *Instructor says:* "It manages everything — insertion, get cache, create cache. It manages the proper region — for this region what is the TTL, what is the cache eviction policy. It takes care of it."

---

### What Can You Group Inside a Region?

The instructor mentions that regions aren't just for entities. You can group:

```
Entities        → individual JPA entity objects
Collections     → one-to-many or many-to-many relationships
Query Results   → results of specific JPQL/HQL queries
```

This gives you very **granular control** over what gets cached and how.

---

### Quick Summary

```
Region = named bucket inside L2 cache

Each region has its own:
  ├── TTL (how long data stays valid)
  ├── Max size (how many entries fit)
  └── Eviction policy (what gets removed when full)

Region name in @Cache annotation
  must match
alias in ehcache.xml

Why regions matter:
  Different entities have different access patterns
  Regions let you fine-tune the cache per entity
  instead of one global config for everything
```

---

## Step 6 — Cache Concurrency Strategies

This is the most important part of the whole topic. The instructor spends the most time here, and it's also the most frequently asked about in interviews.

### What is a Cache Concurrency Strategy?

Before jumping into the four types, let's understand what problem this is solving.

In a real application, multiple requests are happening **at the same time** — some are reading, some are updating, some are deleting. Now you have data sitting in L2 cache AND in the DB. The question is:

> *When reads, updates, and deletes are all happening in parallel — how do you keep the cache and DB in sync? How do you prevent someone from reading stale data from the cache while an update is happening?*

That's exactly what a **Cache Concurrency Strategy** controls.

> 💬 *Instructor says:* "Concurrency strategy is like — how you handle concurrent requests like get, update, delete coming at the same time when you have data in cache and in DB. Which strategy you pick depends on your business use case."

There are **4 strategies**:

```
1. READ_ONLY
2. READ_WRITE
3. NONSTRICT_READ_WRITE
4. TRANSACTIONAL
```

Let's go through each one in detail.

---

### Strategy 1 — READ_ONLY

#### When to use it?
For **static data** — data that is inserted once and **never updated**. Think of things like country codes, configuration constants, dropdown master data.

> 💬 *Instructor says:* "Good for static data. Data which does not require any updates. Not even less updates — ANY update — if you try to update an entity, an exception will come."

#### How it works:

```
POST (insert)
     ↓
Data goes directly to DB
L2 Cache untouched

GET (first read)
     ↓
L1 miss → L2 miss → DB hit
Data loaded into L2 cache ✅

GET (second read, new request)
     ↓
L1 miss → L2 HIT ✅
No DB call

PUT (update attempt)
     ↓
💥 Exception: "Can't update read-only object"
```

#### What happens if you try to update?

The instructor demonstrates this live. When you try to call a PUT on an entity marked as `READ_ONLY`, you get a 500 Internal Server Error:

```
java.lang.UnsupportedOperationException: Can't update read-only object
    at org.hibernate.cache.spi.support.EntityReadOnlyAccess.update(...)
    at org.hibernate.action.internal.EntityUpdateAction.updateCacheItem(...)
```

This exception comes from **Hibernate itself** — specifically from `hibernate-jcache`, which understands the `READ_ONLY` strategy and enforces it.

#### Code:
```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.READ_ONLY,
    region = "userDetailsCache"
)
public class UserDetails { ... }
```

#### Summary:
```
READ_ONLY
  ├── No locks needed (data never changes)
  ├── Fastest of all strategies
  ├── Any update attempt → exception
  └── Best for: country codes, config data, master/reference data
```

---

### Strategy 2 — READ_WRITE

#### When to use it?
When your data **can be read and updated**, and you **cannot tolerate stale data** being served from the cache during or after an update.

> 💬 *Instructor says:* "This is more commonly used in the payment industry."

This is the most detailed strategy and the one the instructor explains most thoroughly.

#### How reads work:

```
GET (read)
     ↓
Puts a SHARED LOCK on the cache entry
     ↓
Multiple reads can happen simultaneously
(all can acquire shared lock at the same time) ✅
     ↓
BUT no write operation is allowed while shared lock is held ❌
```

#### How updates work (the detailed internal flow):

This is the most important part. The instructor walks through this step by step:

```
PUT (update)
     ↓
Transaction begins
     ↓
Acquire EXCLUSIVE LOCK on cache entry
(prevents stale reads — no read OR write allowed now)
     ↓
Mark cache entry as INVALIDATED 🚩
(entry still exists but flagged as invalid)
     ↓
         ┌─────────────────────────┐
         │   Update DB             │
         └─────────────────────────┘
                    ↓
         ┌──────────────────────────────────────┐
         │  Transaction COMMITS successfully?   │
         └──────────────────────────────────────┘
                  ↓              ↓
                YES              NO (Rollback)
                  ↓              ↓
    Update L2 cache         Just release
    with latest data        the lock
    Remove INVALIDATED      (INVALIDATED flag
    flag ✅                  stays) 🚩
    Release lock ✅
```

#### What happens after a rollback?

This is a subtle but important point the instructor makes:

After a rollback, the lock is released but the `INVALIDATED` flag stays on the cache entry. So the next GET request that comes in will:

```
GET after rollback
     ↓
L1 miss (new request)
     ↓
L2 check → sees INVALIDATED flag 🚩
     ↓
Goes to DB for fresh data
     ↓
Overwrites cache with fresh data ✅
Removes INVALIDATED flag ✅
```

This is a really elegant design — even if the cache update fails, the system self-heals on the next read.

> 💬 *Instructor says:* "Cache is sometimes a best-effort call. We should not fail the whole transaction just because we couldn't update the cache. The DB is already updated correctly — that's what matters. The cache will catch up on the next read."

#### What you see in the console with READ_WRITE:

```
Call 1 - POST (insert):
  Hibernate: insert into user_details...
  → goes to DB directly

Call 2 - GET (first read, cache miss):
  Cache miss : region = 'userDetailsCache'
  Hibernate: select ud1_0.id...
  Caching data from load ✅

Call 3 - PUT (update):
  Acquire lock on cache
  Mark INVALIDATED
  Hibernate: update user_details set...
  Transaction commits ✅
  Update cache with latest data
  Remove INVALIDATED flag
  Release lock

Call 4 - GET (after update):
  Cache HIT ✅  ← updated data served from cache
  No SELECT query fired
```

The key proof here is the last GET — after an update, a cache hit still happens because READ_WRITE **updates the cache with the latest data** after a successful commit.

#### Code:
```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    region = "userDetailsCache"
)
public class UserDetails { ... }
```

#### Summary:
```
READ_WRITE
  ├── Shared lock during reads (multiple reads OK simultaneously)
  ├── Exclusive lock during updates (no reads or writes allowed)
  ├── Cache marked INVALIDATED during update
  ├── On commit  → cache updated with fresh data, lock released
  ├── On rollback → lock released, INVALIDATED flag stays
  │                 next GET self-heals from DB
  ├── No stale data served ✅
  └── Best for: payment systems, any data requiring strong consistency
```

---

### Strategy 3 — NONSTRICT_READ_WRITE

#### When to use it?
When your application is **heavily read-oriented**, updates are infrequent, and you can **tolerate a small window of stale data** being served occasionally.

> 💬 *Instructor says:* "The purpose of non-strict is to try to put a lock for the least amount of time possible."

#### How it differs from READ_WRITE:

```
                    READ_WRITE        NONSTRICT_READ_WRITE
                    ──────────        ────────────────────
During Read:        Shared lock       NO lock at all ❌
During Update:      Exclusive lock    Minimal lock
After commit:       Cache updated     Cache only INVALIDATED
                    with fresh data   (not updated with fresh data)
After rollback:     Lock released,    Lock released,
                    INVALIDATED stays nothing happens
```

#### How updates work internally:

```
PUT (update)
     ↓
Transaction begins
     ↓
Acquire lock on cache
     ↓
Update DB
     ↓
         ┌──────────────────────────────────────┐
         │  Transaction COMMITS successfully?   │
         └──────────────────────────────────────┘
                  ↓              ↓
                YES              NO (Rollback)
                  ↓              ↓
    Mark cache as           Just release
    INVALIDATED 🚩          the lock
    Release lock            (nothing else happens)
    (does NOT update
    cache with fresh data)
```

#### What happens after the update?

```
GET after update (NONSTRICT_READ_WRITE)
     ↓
L1 miss
     ↓
L2 check → sees INVALIDATED flag 🚩
     ↓
Goes to DB for fresh data (SELECT fired)
     ↓
Cache updated with fresh data ✅
```

Compared to READ_WRITE where the cache is proactively updated after commit, here the cache is just invalidated and the **next GET is responsible for refreshing it from DB**.

#### The Stale Data Risk:

This is the trade-off the instructor highlights. Because reads don't acquire any lock:

```
Timeline:
─────────────────────────────────────────────────────
Update starts → lock acquired → DB updated → commit
                                                ↓
                                         INVALIDATED flag set

Meanwhile... a GET comes in DURING the update:
GET → no lock check → reads directly from L2 cache
    → gets the OLD data before INVALIDATED flag was set
    → STALE DATA served 🚩
─────────────────────────────────────────────────────
```

> 💬 *Instructor says:* "There is a very small window — but there is a chance that a read operation might get stale data. That's the trade-off with NONSTRICT_READ_WRITE."

#### What you see in the console with NONSTRICT_READ_WRITE:

```
Call 1 - POST (insert):
  Hibernate: insert into user_details...

Call 2 - GET (first read, cache miss):
  Cache miss
  Hibernate: select...
  Data loaded into cache ✅

Call 3 - PUT (update):
  DB updated ✅
  Cache marked INVALIDATED 🚩
  (cache NOT updated with fresh data)

Call 4 - GET (after update):
  Cache INVALIDATED → cache miss
  Hibernate: select...   ← new SELECT query fired
  Fresh data loaded into cache ✅
```

Notice the difference from READ_WRITE — in NONSTRICT, after an update, the next GET still fires a SELECT query because the cache was only invalidated, not refreshed.

#### Code:
```java
@Entity
@Cache(
    usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE,
    region = "userDetailsCache"
)
public class UserDetails { ... }
```

#### Summary:
```
NONSTRICT_READ_WRITE
  ├── NO lock during reads (fastest reads)
  ├── Minimal lock during updates
  ├── On commit  → cache INVALIDATED only (not refreshed)
  ├── On rollback → lock released, nothing else
  ├── Next GET after update → DB hit (cache miss)
  ├── Small window where stale data CAN be served 🚩
  └── Best for: heavy read apps, non-critical data,
                infrequent updates, slight staleness acceptable
```

---

### Strategy 4 — TRANSACTIONAL

#### When to use it?
When you need the **strictest consistency** — no stale reads allowed at all, even from the cache. Every read during a cache lock must go directly to DB.

> 💬 *Instructor says:* "It's the most restrictive. During read it acts like a hard lock where no other get call is even allowed to read from L2 cache — go and directly read from the DB."

#### How it works:

```
GET (read)
     ↓
Acquires READ lock on cache entry
     ↓
Any other GET that comes in during this lock:
     → cannot read from L2 cache
     → goes directly to DB instead
     ↓
Any WRITE that comes in during this lock:
     → waits in queue ⏳
     → cannot proceed until READ lock is released

PUT (update)
     ↓
Acquires WRITE lock
     ↓
Update DB
     ↓
On commit → cache updated with fresh data ✅
            (similar to READ_WRITE)
     ↓
Release lock
     ↓
Any queued WRITE operations can now proceed
```

#### The key difference vs READ_WRITE:

```
                READ_WRITE              TRANSACTIONAL
                ──────────              ─────────────
During read:    Shared lock             READ lock
                (others can             (other reads go
                also read from          directly to DB,
                cache)                  not from cache)

During write:   Exclusive lock          WRITE lock
                (no reads/writes        (reads go to DB,
                from cache)             writes wait in queue)

After commit:   Cache updated ✅        Cache updated ✅
```

#### Summary:
```
TRANSACTIONAL
  ├── READ lock during reads
  │     → other reads bypass cache, go directly to DB
  ├── WRITE lock during updates
  │     → other writes wait in queue
  ├── Cache updated after successful commit (like READ_WRITE)
  ├── Strictest consistency — zero stale data risk
  └── Best for: highly sensitive data where absolute
                consistency is non-negotiable
```

---

### Side by Side Comparison — All 4 Strategies

```
┌───────────────────────┬────────────┬─────────────┬──────────────────────┬───────────────┐
│ Strategy              │ Read Lock  │ Write Lock  │ Cache after commit   │ Stale Data?   │
├───────────────────────┼────────────┼─────────────┼──────────────────────┼───────────────┤
│ READ_ONLY             │ None       │ N/A         │ Never updated        │ No            │
│                       │            │ (exception) │ (data never changes) │               │
├───────────────────────┼────────────┼─────────────┼──────────────────────┼───────────────┤
│ READ_WRITE            │ Shared     │ Exclusive   │ Updated with         │ No            │
│                       │ lock       │ lock        │ fresh data ✅         │               │
├───────────────────────┼────────────┼─────────────┼──────────────────────┼───────────────┤
│ NONSTRICT_READ_WRITE  │ No lock    │ Minimal     │ Only INVALIDATED     │ Small window  │
│                       │ at all     │ lock        │ (not refreshed) 🚩   │ possible 🚩   │
├───────────────────────┼────────────┼─────────────┼──────────────────────┼───────────────┤
│ TRANSACTIONAL         │ READ lock  │ WRITE lock  │ Updated with         │ No            │
│                       │ (others    │ (others     │ fresh data ✅         │               │
│                       │ go to DB)  │ wait)       │                      │               │
└───────────────────────┴────────────┴─────────────┴──────────────────────┴───────────────┘
```

---

### Choosing the Right Strategy — Decision Guide

```
Is your data static (never updated)?
     └── YES → READ_ONLY

Is your data updated, and stale data is completely unacceptable?
     └── YES, and reads during lock can go to DB → TRANSACTIONAL
     └── YES, but reads during lock can still use cache → READ_WRITE

Is your application heavily read-oriented, and occasional
stale data is acceptable for a short window?
     └── YES → NONSTRICT_READ_WRITE
```

---

## Step 7 — Interview Tips & Tricks

This is everything the instructor drops throughout the lecture that is directly relevant to interviews, plus the key conceptual questions that naturally come out of this topic. These are the things that separate someone who just "knows about" L2 caching from someone who actually **understands** it.

---

### Tip 1 — L1 vs L2 Cache (Most Common Starting Question)

This is almost always the first question. Interviewers use it to gauge depth of understanding.

```
┌─────────────────────┬──────────────────────────────┬────────────────────────────────┐
│ Aspect              │ L1 Cache                     │ L2 Cache                       │
├─────────────────────┼──────────────────────────────┼────────────────────────────────┤
│ Also called         │ First Level Cache            │ Second Level Cache             │
│ Where it lives      │ Inside EntityManager         │ Outside EntityManager          │
│ Scope               │ Single session/request       │ Shared across all sessions     │
│ Enabled by default  │ YES (always on)              │ NO (must enable manually)      │
│ Survives request?   │ NO (dies with EntityManager) │ YES (lives across requests)    │
│ Configured via      │ Nothing needed               │ @Cache + application.properties│
│ Provider needed?    │ No                           │ Yes (Ehcache/Caffeine/etc.)    │
└─────────────────────┴──────────────────────────────┴────────────────────────────────┘
```

> 🎯 **Key line to remember:** L1 is private to a session. L2 is shared across all sessions. That one sentence answers half the interview questions on this topic.

---

### Tip 2 — "What is L2 Cache?" — Answer it in Layers

Don't just say "it's a shared cache." Build the answer in layers:

```
Layer 1 (basic):
"L2 cache is a shared cache layer that sits between the
Persistence Context and the Database in JPA/Hibernate."

Layer 2 (add the problem it solves):
"In L1 cache, every new HTTP request creates a new EntityManager
with its own Persistence Context — so cached data from one request
is invisible to another. L2 solves this by providing a cache that
is shared across all EntityManagers."

Layer 3 (add the lookup order):
"When a GET request comes in, Hibernate first checks L1,
then L2, and only hits the DB if both miss. Data fetched
from DB is loaded into L2 so subsequent requests benefit."

Layer 4 (production awareness):
"It's commonly used in production for read-heavy entities
like product catalogs, user profiles, or config data —
anything that is fetched frequently but doesn't change often."
```

---

### Tip 3 — Why 3 Dependencies? (Frequently Asked)

Interviewers love asking this because it tests whether you actually understand what you're configuring or just copy-pasted it.

```
"There are three dependencies because they serve three
different responsibilities:

1. ehcache → the actual cache engine (can be swapped
             with Caffeine or Hazelcast)

2. cache-api → JCache interfaces that Hibernate talks to.
               This gives loose coupling — if you swap
               Ehcache for Caffeine, your code doesn't change
               because you're always talking to the interface.

3. hibernate-jcache → the bridge between Hibernate and the
                      JCache interfaces. It understands
                      @Cache, CacheConcurrencyStrategy, and
                      knows when to call getCache(), putCache(),
                      etc. Think of it as the orchestrator."
```

> 🎯 **Key point to emphasize:** The `cache-api` dependency is what gives you **loose coupling** between Hibernate and the actual cache provider. This is a design principle question hidden inside a dependency question.

---

### Tip 4 — INSERT doesn't populate L2 Cache

This is a classic trick question:

> *"If I insert a record and then immediately do a GET, will L2 cache be hit?"*

**Answer: No.** INSERT goes directly to DB. L2 cache is only populated on the **first GET (read) operation**. So the first GET after an INSERT will always be a cache miss, and L2 gets populated at that point.

```
Common wrong answer: "Yes, insert loads it into L2"
Correct answer:      "No, INSERT bypasses L2 entirely.
                      L2 is only populated on first GET."
```

---

### Tip 5 — The 4 Concurrency Strategies — How to Answer in Interview

When asked "explain the 4 cache concurrency strategies," structure your answer like this:

```
"The strategy you choose depends on two things:
 1. Whether your data gets updated
 2. Whether you can tolerate stale data

READ_ONLY:
  → For data that never changes. Any update attempt
    throws an exception. Fastest strategy.
  → Example: country codes, dropdown master data

READ_WRITE:
  → For data that gets updated, and stale data is
    not acceptable. Uses shared lock for reads and
    exclusive lock for updates. Cache is updated
    with fresh data after successful commit.
  → Commonly used in payment systems.

NONSTRICT_READ_WRITE:
  → For heavy read applications where updates are
    rare and a small stale data window is acceptable.
    No lock during reads. After update, cache is only
    invalidated (not refreshed). Next GET will fetch
    fresh data from DB.

TRANSACTIONAL:
  → Strictest strategy. Both READ and WRITE locks.
    During a read lock, other reads bypass cache and
    go directly to DB. Writes wait in queue. Cache
    updated after commit like READ_WRITE."
```

---

### Tip 6 — The Rollback Scenario (Deep Dive Question)

> *"What happens to the L2 cache if a transaction rolls back during an update?"*

This is a deep question that tests real understanding. The answer differs by strategy:

```
READ_WRITE on rollback:
  → Lock is released
  → INVALIDATED flag stays on the cache entry
  → Cache is NOT updated with fresh data
  → Next GET sees INVALIDATED flag → goes to DB
  → Self-heals naturally ✅

NONSTRICT_READ_WRITE on rollback:
  → Lock is simply released
  → Nothing else happens
  → Cache still has old data (not even invalidated)
  → This is the stale data risk window 🚩

TRANSACTIONAL on rollback:
  → Lock released
  → Cache not updated
  → Next reads go to DB directly anyway
```

> 🎯 **Key concept to mention:** "Cache operations are best-effort. We should never fail a DB transaction just because the cache couldn't be updated. The DB is the source of truth. The cache will self-heal on the next read."

---

### Tip 7 — What is a Region? (Follow-up Question)

> *"What is a cache region and why do we use them?"*

```
"A region is a named logical grouping of cached data inside
the L2 cache. Each entity can be assigned to its own region,
and each region can have independent configuration — its own
TTL, eviction policy (FIFO/LIFO/LRU), max cache size, and
concurrency strategy.

This gives granular control. For example, UserDetails might
need a 60 second TTL with 100 max entries, while OrderDetails
might need a 200 second TTL with 1000 max entries. Without
regions you'd be stuck with a single global config for all
entities."
```

---

### Tip 8 — Loose Coupling via JCache (Design Question)

> *"Why use JCacheRegionFactory instead of EhcacheRegionFactory directly?"*

```
"JCacheRegionFactory works through the JCache standard interfaces
(cache-api). This means Hibernate never directly depends on Ehcache.
Tomorrow if we want to switch from Ehcache to Caffeine or Hazelcast,
we only change the provider property in application.properties —
no code changes needed.

If we used EhcacheRegionFactory directly, we'd be tightly coupled
to Ehcache and switching providers would require code changes."
```

This is a **dependency inversion / loose coupling** principle question disguised as a caching question.

---

### Tip 9 — When NOT to use L2 Cache

Interviewers sometimes ask this to see if you understand the trade-offs:

```
Don't use L2 cache when:

1. Data changes very frequently
   → Cache will be invalidated constantly, giving
     no real benefit. You'd just add overhead.

2. Data is extremely sensitive and must always be
   real-time accurate
   → Even READ_WRITE has a small window during updates.
     For truly real-time critical data, don't cache it.

3. You have a distributed system with multiple app nodes
   → L2 cache by default is local to each node.
     You'd need a distributed cache like Hazelcast
     to share L2 across nodes. Otherwise each node
     has its own L2 and they can serve different data.

4. Dataset is too large
   → L2 cache is in-memory. Caching huge datasets
     can cause memory pressure and OutOfMemoryErrors.
```

---

### Tip 10 — The "Best Effort" Nature of Cache

The instructor makes this point and it's a great one to bring up in interviews to show maturity:

> 🎯 *"Cache is always best-effort. The database is the source of truth. A well-designed caching strategy should never make the system less reliable — it should only make it faster when it works, and gracefully fall back to DB when it doesn't."*

This shows you understand caching not just technically but from a **system reliability** perspective.

---

### Quick Revision Card — Everything on One Page

```
┌─────────────────────────────────────────────────────────────────┐
│               L2 CACHE — QUICK REVISION                         │
├─────────────────────────────────────────────────────────────────┤
│ What       → Shared cache between Persistence Context & DB      │
│ Why        → L1 dies with each request, L2 survives across all  │
│ Lookup     → L1 → L2 → DB (in that order)                       │
│ INSERT     → always bypasses L2, goes directly to DB            │
│ 1st GET    → L2 miss, DB hit, data loaded into L2               │
│ 2nd GET    → L2 HIT, no DB call                                 │
├─────────────────────────────────────────────────────────────────┤
│ 3 Dependencies:                                                 │
│   ehcache        → actual cache engine                          │
│   cache-api      → JCache interfaces (loose coupling)           │
│   hibernate-jcache → bridge/orchestrator                        │
├─────────────────────────────────────────────────────────────────┤
│ 3 Properties:                                                   │
│   use_second_level_cache = true                                 │
│   region.factory_class = JCacheRegionFactory                    │
│   javax.cache.provider = EhcacheCachingProvider                 │
├─────────────────────────────────────────────────────────────────┤
│ Region = named bucket with its own TTL, size, eviction policy   │
├─────────────────────────────────────────────────────────────────┤
│ Strategies:                                                     │
│   READ_ONLY           → static data, no updates allowed         │
│   READ_WRITE          → updates ok, no stale data, payments     │
│   NONSTRICT_READ_WRITE → heavy reads, small stale window ok     │
│   TRANSACTIONAL       → strictest, reads go to DB during lock   │
├─────────────────────────────────────────────────────────────────┤
│ Rollback → lock released, INVALIDATED flag stays (READ_WRITE)   │
│          → self-heals on next GET from DB                       │
│ Cache = best effort. DB = source of truth. Always.              │
└─────────────────────────────────────────────────────────────────┘
```

---

That wraps up the complete notes for **Spring Boot JPA — Second Level Caching**. All 7 steps covered:

1. ✅ The Problem with L1 Cache
2. ✅ What L2 Cache is & How it Solves it
3. ✅ Setup (Dependencies + Config)
4. ✅ The Happy Flow
5. ✅ Cache Regions
6. ✅ All 4 Concurrency Strategies
7. ✅ Interview Tips & Tricks