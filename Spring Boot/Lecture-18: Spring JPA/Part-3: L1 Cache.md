# Step 1 – Quick Revision: JPA Lifecycle

---

Before the instructor jumps into First-Level Caching, he makes it very clear:

> *"Without understanding the lifecycle, first-level caching would be incomplete."*

So let's nail this first.

---

## What is the Persistence Context?

When you work with JPA, you never talk to the database directly. There's a middleman called the **Persistence Context**. Think of it as an **in-memory workspace** where your entities (objects) live and are managed — before anything actually hits the database.

The **EntityManager** is the API you use to interact with this workspace. And under the hood, Hibernate provides the actual implementation via something called **SessionImpl**.

So the relationship is:

```
Your Code
    ↓
EntityManager  (JPA API — interface)
    ↓
SessionImpl    (Hibernate's actual implementation)
    ↓
Persistence Context  (in-memory workspace / map)
    ↓
Database  ← only touched at FLUSH / COMMIT time
```

---

## The 4 States of an Entity

An entity (like a `UserDetails` object) can be in one of these states at any point:

```
┌───────────────────────────────────────────────────────────┐
│                   ENTITY LIFECYCLE                        │
│                                                           │
│   new UserDetails()                                       │
│         │                                                 │
│         ▼                                                 │
│      [NEW]  ──── entityManager.persist(a) ────▶ [MANAGED] │
│                                                    │  ▲   │
│                                           remove() │  │ persist()
│                                                    ▼  │   │
│                                                 [REMOVED] │
│                                                           │
│                                    entityManager.detach() │
│                                                    ▼      │
│                                              [DETACHED]   │
│                                                           │
│   ◀─────────────── Persistence Context ────────────────▶  │
│         Everything above lives HERE, not in DB            │
│                                                           │
│   Only when commit() / flush() happens → DB is touched    │
└───────────────────────────────────────────────────────────┘
```

---

## Walking Through the Instructor's Example

The instructor walks through this exact code mentally:

```java
UserDetails a = new UserDetails();   // State: NEW

transaction.begin();

entityManager.persist(a);   // State: MANAGED (inside Persistence Context)
                             // ⚠️ NOT in DB yet

entityManager.remove(a);    // State: REMOVED (still inside Persistence Context)
                             // ⚠️ still NOT in DB yet

entityManager.persist(a);   // State: MANAGED again (changed mind)
                             // ⚠️ still NOT in DB yet

transaction.commit();        // ✅ NOW flush happens → DB is finally touched
                             // Since state is MANAGED → INSERT query fires
```

### What's the key takeaway here?

All those operations — persist, remove, persist again — happened **only inside the Persistence Context** (in memory). The database had **zero idea** any of this was going on. Only at `commit()` does the flush happen, and the final state gets written to the DB.

This is the instructor's core point:

> *"Till flush happens, everything is handled in memory. That's handled through the Persistence Context. And that's what helps in First-Level Caching."*

---

## One Important Rule

> **1 EntityManager = 1 Persistence Context**

If you have two EntityManagers, they each have their **own** Persistence Context. They are completely isolated from each other — one has no idea what data the other is holding.

```
EntityManager1  →  PersistenceContext1  (isolated)
EntityManager2  →  PersistenceContext2  (isolated)
```

This isolation is **exactly** why First-Level Cache works the way it does — and also where its limitation lies (which leads to Second-Level Caching later).

---

# Step 2 – The Puzzle: What Problem Does First-Level Caching Solve?

---

## The Setup

The instructor starts with a very simple Spring Boot API. Here's what the `/test-jpa` endpoint does:

```java
@GetMapping(path = "/test-jpa")
public UserDetails getUser() {

    UserDetails userDetails = new UserDetails("xyz", "xyz@conceptandcoding.com");

    userDetailsService.saveUser(userDetails);   // Step 1: INSERT

    UserDetails output1 = userDetailsService.getUser(1L);  // Step 2: SELECT
    return output1;
}
```

Simple enough, right? First save a user, then fetch that same user by ID.

So the **expected behavior** in your head is:

```
1. One INSERT query fires  → user gets saved to DB
2. One SELECT query fires  → user gets fetched from DB
```

---

## What Actually Happens — The Puzzle

The instructor hits this API and checks the console logs.

He sees:

```sql
INSERT INTO user_details (email, name, id) VALUES (?, ?, default)
```

✅ INSERT happened. Good.

But then... **no SELECT query**. Nothing. Silence.

Yet the API **returned the correct user data**.

The instructor pauses here and asks:

> *"Where is the SELECT query? Why is there no SELECT query? But how come I am able to fetch the data?"*

And then he answers it himself:

> *"It's because of cache."*

---

## So What's the Problem Being Solved?

Imagine you're inside a single HTTP request and you:
- Save an entity
- Then immediately fetch that same entity (by primary key)

Without caching, JPA would fire **two database queries** — an INSERT and then a SELECT. That SELECT is completely **wasteful** because you just saved that object moments ago. The data is already known. Why go all the way to the database again?

First-Level Caching solves exactly this:

> **If the entity you're looking for is already tracked by the current Persistence Context, JPA will return it directly from memory — no database query needed.**

---

## The Simple Mental Model

```
┌─────────────────────────────────────────────────────────┐
│                  ONE HTTP REQUEST                       │
│                                                         │
│   saveUser(userDetails)                                 │
│        │                                                │
│        ▼                                                │
│   Persistence Context  ◀──── entity stored here         │
│        │                     (key = entity type + ID)   │
│        │                                                │
│   getUser(1L)                                           │
│        │                                                │
│        ▼                                                │
│   JPA checks Persistence Context first                  │
│        │                                                │
│        ├──── Found? ✅ → return from memory (NO DB hit)  │
│        │                                                │
│        └──── Not found? → go to DB, fetch, store here   │
└─────────────────────────────────────────────────────────┘
```

---

## Why Does This Matter in Real Projects?

In real applications, it's very common to:
- Save something and then read it back in the same request
- Call the same `findById()` multiple times within the same flow
- Have multiple service methods in a chain, each trying to load the same entity

Without First-Level Caching, each of those would fire a separate SELECT query to the database — unnecessary load, unnecessary latency.

With First-Level Caching, all those redundant fetches within the **same EntityManager scope** are served from memory instantly.

---

## One Line Summary

> First-Level Caching ensures that within a single EntityManager's lifetime, the same entity is **never fetched from the database twice.**

---

# Step 3 – How It Actually Works Internally

---

This is where the instructor goes deep. He traces the exact internal flow — from your code, all the way down to where the caching actually happens.

---

## The Internal Flow — Tracing Every Step

When you call `userDetailsRepository.save(user)`, here's what actually happens under the hood:

```
userDetailsRepository.save(user)
        │
        ▼
SimpleJpaRepository.save()        ← Spring's built-in implementation
        │
        ▼
entityManager.persist(entity)     ← JPA API call
        │
        ▼
SessionImpl                       ← Hibernate's actual implementation
        │
        ▼
StatefulPersistenceContext        ← The in-memory workspace / cache
        │
        ▼
addEntity(key, entity)            ← Stored in a MAP
```

---

## The Persistence Context is Just a Map

This is the instructor's most important internal detail. The Persistence Context, at its core, is just a **HashMap**.

When you persist an entity, it gets stored like this:

```
┌─────────────────────────────────────────────────────────────┐
│              StatefulPersistenceContext (MAP)               │
│                                                             │
│   KEY                          VALUE                        │
│   ─────────────────────────    ──────────────────────────   │
│   EntityKey                    The actual entity object     │
│   (entity type + primary key)  (UserDetails object)         │
│                                                             │
│   e.g.                                                      │
│   EntityKey[UserDetails, 1]  → UserDetails{id=1,            │
│                                  name="xyz",                │
│                                  email="xyz@..."}           │
└─────────────────────────────────────────────────────────────┘
```

The instructor points this out directly from the `StatefulPersistenceContext.java` source code:

```java
public void addEntity(EntityKey key, Object entity) {
    EntityHolderImpl holder = EntityHolderImpl.forEntity(key, 
                              key.getPersister(), entity);
    final EntityHolderImpl oldHolder = getOrInitializeEntitiesByKey()
                                       .putIfAbsent(key, holder);
    ...
}
```

So `putIfAbsent` — if this key doesn't already exist in the map, put it in. This is exactly where your entity gets cached.

---

## Now — What Happens During a Find?

When you call `userDetailsRepository.findById(1L)`, here's the internal flow:

```
userDetailsRepository.findById(1L)
        │
        ▼
entityManager.find(UserDetails.class, 1L)
        │
        ▼
SessionImpl
        │
        ▼
PersistenceContext.getEntity(key)   ← checks the MAP first
        │
        ├─── Key found in map? ✅
        │         │
        │         ▼
        │    Return entity from memory
        │    (NO SELECT query fired)
        │
        └─── Key NOT found in map? ❌
                  │
                  ▼
             Go to Database
             Fire SELECT query
             Store result in map
             Return entity
```

---

## Putting It All Together — The Full Picture

```
┌──────────────────────────────────────────────────────────────────┐
│                     ONE HTTP REQUEST                             │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │                    EntityManager                        │     │
│  │                                                         │     │
│  │  ┌───────────────────────────────────────────────────┐  │     │
│  │  │           Persistence Context (MAP)               │  │     │
│  │  │                                                   │  │     │
│  │  │  persist(userDetails)                             │  │     │
│  │  │       │                                           │  │     │
│  │  │       ▼                                           │  │     │
│  │  │  [UserDetails#1] ──▶ {id=1, name="xyz", ...}      │  │     │
│  │  │                                                   │  │     │
│  │  │  findById(1L)                                     │  │     │
│  │  │       │                                           │  │     │
│  │  │       ▼                                           │  │     │
│  │  │  Check map → KEY FOUND ✅ → return from here       │  │     │
│  │  │  (no DB call)                                     │  │     │
│  │  └───────────────────────────────────────────────────┘  │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                  │
│                      ↕ only at commit/flush                      │
│                                                                  │
│                        DATABASE                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## When Does the INSERT Actually Fire?

The instructor clarifies this very specifically.

`save()` in Spring Data JPA is annotated with `@Transactional`. If you remember from the Transactional video — there's a **Transactional Interceptor** that automatically does `begin` and `commit` around the method.

So the flow is:

```
@Transactional
save(user)
    │
    ├── begin transaction
    │
    ├── entityManager.persist(user)
    │       → entity stored in Persistence Context map
    │
    └── commit()
            → flush happens
            → INSERT query fires to DB
            → DB is now in sync
```

So after `save()` completes, the entity exists in **two places**:
1. Inside the Persistence Context map (in memory)
2. Inside the Database

When you then call `findById(1L)` — JPA checks the map first, finds it there, and **never needs to go to the DB**.

---

## The Critical Rule — 1 EntityManager = 1 Persistence Context

The instructor keeps coming back to this rule because it's the heart of understanding when caching works and when it doesn't:

```
┌─────────────────────┐        ┌─────────────────────┐
│   EntityManager 1   │        │   EntityManager 2   │
│                     │        │                     │
│  PersistenceContext │        │  PersistenceContext │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │ UserDetails#1 │  │        │  │    EMPTY      │  │
│  └───────────────┘  │        │  └───────────────┘  │
│                     │        │                     │
│  ← knows this data  │   ✗    │  ← no idea about    │
│                     │        │    EM1's data       │
└─────────────────────┘        └─────────────────────┘
        ISOLATED                       ISOLATED
```

> Two EntityManagers have **zero visibility** into each other's Persistence Context. Completely isolated.

---

## Where Does This Lead?

This isolation is both the **strength** and the **limitation** of First-Level Caching:

- ✅ **Strength** — within one EntityManager, repeated fetches of the same entity cost nothing extra
- ❌ **Limitation** — across two different EntityManagers (e.g., two separate HTTP requests), there's no shared cache at all

That limitation is exactly what **Second-Level Caching** addresses — but the instructor says that's for a later video.

---

# Step 4 – Live Demo Walkthrough: `/test-jpa` and `/read-jpa`

---

This is where the instructor actually runs the application and proves everything he explained in Step 3. He uses two API endpoints to demonstrate two different scenarios.

---

## The Setup — Two APIs

```java
// API 1 — Save first, then fetch
@GetMapping(path = "/test-jpa")
public UserDetails getUser() {
    UserDetails userDetails = new UserDetails("xyz", "xyz@conceptandcoding.com");
    userDetailsService.saveUser(userDetails);      // INSERT
    UserDetails output1 = userDetailsService.getUser(1L);  // SELECT?
    return output1;
}

// API 2 — Only fetch (no save here)
@GetMapping(path = "/read-jpa")
public UserDetails getUser2() {
    UserDetails output1 = userDetailsService.getUser(1L);  // SELECT?
    return output1;
}
```

And the Service + Repository are straightforward:

```java
@Service
public class UserDetailsService {

    @Autowired
    UserDetailsRepository userDetailsRepository;

    public void saveUser(UserDetails user) {
        userDetailsRepository.save(user);
    }

    public UserDetails getUser(Long primaryKey) {
        return userDetailsRepository.findById(primaryKey).get();
    }
}
```

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {
    // Spring Data JPA handles everything internally
}
```

---

## Scenario 1 — Hitting `/test-jpa`

### What the instructor expects vs what actually happens:

```
Expected:
    → One INSERT query
    → One SELECT query

Actual:
    → One INSERT query  ✅
    → NO SELECT query   ← This is the cache working
```

### Why does this happen? — Step by step:

```
HTTP Request hits /test-jpa
        │
        ▼
┌───────────────────────────────────────────────────────┐
│                    EntityManager (EM1)                │
│              created for THIS HTTP request            │
│                                                       │
│  Step 1: saveUser(userDetails)                        │
│      │                                                │
│      ▼                                                │
│  entityManager.persist(user)                          │
│      │                                                │
│      ▼                                                │
│  Persistence Context MAP                              │
│  ┌─────────────────────────────────────────────┐      │
│  │  KEY: EntityKey[UserDetails, 1]             │      │
│  │  VAL: UserDetails{id=1, name="xyz", ...}    │      │
│  └─────────────────────────────────────────────┘      │
│      │                                                │
│      ▼                                                │
│  @Transactional commit() → flush → INSERT fires ✅     │
│  DB is now in sync                                    │
│                                                       │
│  Step 2: getUser(1L)                                  │
│      │                                                │
│      ▼                                                │
│  entityManager.find(UserDetails.class, 1L)            │
│      │                                                │
│      ▼                                                │
│  Check Persistence Context MAP                        │
│      │                                                │
│      ├── KEY FOUND ✅                                  │
│      │                                                │
│      ▼                                                │
│  Return from memory — NO SELECT query fired 🎯        │
└───────────────────────────────────────────────────────┘
```

### Console output the instructor sees:

```sql
Hibernate:
    insert into user_details (email, name, id)
    values (?, ?, default)

-- No SELECT query appears here
-- But the API still returns correct data ✅
```

---

## Why Is the Same EntityManager Used for Both save and find?

This is a question the instructor anticipates you'll ask. He answers it clearly:

> *"Dispatcher Servlet is the one which creates an EntityManager at the time of every HTTP request."*

Here's what happens at the framework level:

```
Browser/Client sends HTTP Request
        │
        ▼
DispatcherServlet  ← Spring's front controller
        │
        ▼
applyPreHandle()   ← runs BEFORE your controller method
        │
        ▼
OpenEntityManagerInViewInterceptor.preHandle()
        │
        ▼
EntityManager em = createEntityManager()   ← ONE EM created here
        │
        ▼
Bound to this HTTP request's scope
        │
        ▼
Your Controller method runs
    → saveUser() uses this EM
    → getUser() uses this SAME EM
        │
        ▼
HTTP Response sent
        │
        ▼
EntityManager closed
```

So the key insight is:

> **One HTTP Request → One EntityManager → One Persistence Context**

Both `saveUser()` and `getUser()` internally resolve to the **same EntityManager** because they're part of the same HTTP request. That's why the cache hit happens.

---

## Scenario 2 — Hitting `/read-jpa` (New HTTP Request)

Now the instructor hits a **different API** — `/read-jpa`. This is a **brand new HTTP request**.

```java
@GetMapping(path = "/read-jpa")
public UserDetails getUser2() {
    UserDetails output1 = userDetailsService.getUser(1L);  // called once
    return output1;
}
```

Wait — but in the actual demo, the instructor's `/read-jpa` calls `getUser(1L)` **twice** internally to prove the point. Let's trace both calls:

### What happens:

```
NEW HTTP Request hits /read-jpa
        │
        ▼
┌───────────────────────────────────────────────────────┐
│              NEW EntityManager (EM2) created          │
│         EM2 has NO idea about EM1's data              │
│                                                       │
│  Persistence Context MAP — starts EMPTY               │
│  ┌─────────────────────────────────────────────┐      │
│  │               (empty)                       │      │
│  └─────────────────────────────────────────────┘      │
│                                                       │
│  First call: getUser(1L)                              │
│      │                                                │
│      ▼                                                │
│  Check Persistence Context MAP                        │
│      │                                                │
│      └── KEY NOT FOUND ❌                              │
│              │                                        │
│              ▼                                        │
│         Go to Database → SELECT query fires ✅         │
│         Fetch UserDetails{id=1, name="xyz", ...}      │
│              │                                        │
│              ▼                                        │
│  Store in Persistence Context MAP                     │
│  ┌─────────────────────────────────────────────┐      │
│  │  KEY: EntityKey[UserDetails, 1]             │      │
│  │  VAL: UserDetails{id=1, name="xyz", ...}    │      │
│  └─────────────────────────────────────────────┘      │
│                                                       │
│  Second call: getUser(1L)  (if called again)          │
│      │                                                │
│      ▼                                                │
│  Check Persistence Context MAP                        │
│      │                                                │
│      └── KEY FOUND ✅ → return from memory             │
│              NO SELECT query fired 🎯                 │
└───────────────────────────────────────────────────────┘
```

### Console output the instructor sees:

```sql
Hibernate:
    select ud1_0.id, ud1_0.email, ud1_0.name
    from user_details ud1_0
    where ud1_0.id=?

-- Only ONE select query even though getUser(1L) was called twice
-- Second call was served from Persistence Context cache ✅
```

---

## Side by Side Comparison — The Full Picture

```
┌──────────────────────────────┬──────────────────────────────┐
│      /test-jpa               │      /read-jpa               │
│      (HTTP Request 1)        │      (HTTP Request 2)        │
├──────────────────────────────┼──────────────────────────────┤
│  EntityManager 1 created     │  EntityManager 2 created     │
│                              │                              │
│  save() → entity in PC map   │  PC map starts EMPTY         │
│         → INSERT fires       │                              │
│                              │  1st find() → DB hit         │
│  find() → KEY FOUND in map   │            → SELECT fires    │
│         → NO SELECT          │            → stored in map   │
│                              │                              │
│                              │  2nd find() → KEY FOUND      │
│                              │            → NO SELECT       │
├──────────────────────────────┼──────────────────────────────┤
│  DB Queries: 1 INSERT        │  DB Queries: 1 SELECT        │
│  Cache hits: 1               │  Cache hits: 1               │
└──────────────────────────────┴──────────────────────────────┘
```

---

## The Golden Rule the Instructor Repeats

> *"Always easy to remember — Dispatcher Servlet is the one which creates an EntityManager at the time of every HTTP request. So one HTTP request = one EntityManager = one Persistence Context."*

This one rule explains everything:
- Why `/test-jpa` had no SELECT — same EM, same PC, cache hit
- Why `/read-jpa` had one SELECT — new EM, empty PC, DB hit on first call
- Why `/read-jpa`'s second call had no SELECT — same EM within that request, cache hit

---

# Step 5 – Manual EntityManager Example

---

So far, Spring was managing the EntityManager behind the scenes — created automatically per HTTP request by the DispatcherServlet. That's convenient, but it can make it hard to *see* exactly what's happening.

So the instructor now creates EntityManagers **manually**, by hand. No Spring magic. This makes the isolation between two Persistence Contexts impossible to miss.

---

## The Code

```java
@Service
public class UserDetailsService {

    @Autowired
    EntityManagerFactory entityManagerFactory;  // auto-wired once at startup
                                                // same factory object reused

    public UserDetails saveUser(UserDetails user) {

        // ── SESSION 1 ──────────────────────────────────────────
        EntityManager entityManager = 
                entityManagerFactory.createEntityManager();  // EM1 created

        entityManager.getTransaction().begin();   // transaction started

        entityManager.persist(user);              // user stored in PC map
                                                  // NOT in DB yet

        entityManager.find(UserDetails.class, 1L);          // find #1
        UserDetails output = 
                entityManager.find(UserDetails.class, 1L);  // find #2

        System.out.println("i am able to find the data, name is:" 
                            + output.getName());

        entityManager.getTransaction().commit();  // flush → INSERT fires
                                                  // DB now in sync

        entityManager.close();                    // EM1 closed, PC1 destroyed
        // ───────────────────────────────────────────────────────


        // ── SESSION 2 ──────────────────────────────────────────
        EntityManager entityManager2 = 
                entityManagerFactory.createEntityManager();  // EM2 created
                                                             // brand new PC

        entityManager2.getTransaction().begin();  // transaction started

        entityManager2.find(UserDetails.class, 1L);          // find #1
        UserDetails output2 = 
                entityManager2.find(UserDetails.class, 1L);  // find #2

        System.out.println("Session2: i am able to find the data, name is:" 
                            + output2.getName());

        entityManager2.getTransaction().commit();  // nothing to flush
        entityManager2.close();                    // EM2 closed
        // ───────────────────────────────────────────────────────

        return output2;
    }
}
```

---

## Tracing Session 1 — Step by Step

```
EntityManagerFactory (shared, created at app startup)
        │
        ▼
entityManagerFactory.createEntityManager()
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│                  EntityManager 1 (EM1)                    │
│                                                           │
│  transaction.begin()                                      │
│                                                           │
│  persist(user)                                            │
│      │                                                    │
│      ▼                                                    │
│  Persistence Context 1 (PC1) — MAP                        │
│  ┌───────────────────────────────────────────────────┐    │
│  │  KEY: EntityKey[UserDetails, 1]                   │    │
│  │  VAL: UserDetails{id=1, name="xyz", ...}          │    │
│  └───────────────────────────────────────────────────┘    │
│      │                                                    │
│      │  ⚠️ NOT in DB yet — commit hasn't happened         │
│                                                           │
│  find(UserDetails.class, 1L)  ← find #1                   │
│      │                                                    │
│      ▼                                                    │
│  Check PC1 map → KEY FOUND ✅ → return from memory         │
│  NO DB query                                              │
│                                                           │
│  find(UserDetails.class, 1L)  ← find #2                   │
│      │                                                    │
│      ▼                                                    │
│  Check PC1 map → KEY FOUND ✅ → return from memory         │
│  NO DB query                                              │
│                                                           │
│  transaction.commit()                                     │
│      │                                                    │
│      ▼                                                    │
│  flush() → INSERT query fires → DB now has the data ✅     │
│                                                           │
│  entityManager.close() → PC1 destroyed                    │
└───────────────────────────────────────────────────────────┘
```

### Console output after Session 1:

```sql
Hibernate:
    insert into user_details (email, name, id)
    values (?, ?, default)

i am able to find the data, name is: xyz
```

Notice — only **one INSERT**, and the two `find()` calls produced **zero SELECT queries** because both hits were served from PC1's map.

---

## A Subtle but Important Point Here

The instructor highlights something worth paying close attention to:

> *"Currently data is not there into the DB because we have not done the flush yet. But find() is still able to return it — because it's in the Persistence Context."*

This means:

```
┌──────────────────────────────────────────────────────────┐
│  Timeline inside Session 1                               │
│                                                          │
│  persist(user) ──▶ entity in PC map                      │
│                    (DB has nothing yet)                  │
│                                                          │
│  find() ──────▶ PC map check → FOUND ✅                   │
│                 (still no DB query needed)               │
│                 (data doesn't even need to be in DB yet) │
│                                                          │
│  commit() ────▶ flush → INSERT → DB finally updated      │
└──────────────────────────────────────────────────────────┘
```

The cache isn't just serving data that's already in the DB — it's serving data that **hasn't even reached the DB yet**. The Persistence Context is the single source of truth within that EntityManager's lifetime.

---

## Tracing Session 2 — Step by Step

```
entityManagerFactory.createEntityManager()
        │
        ▼
┌───────────────────────────────────────────────────────────┐
│                  EntityManager 2 (EM2)                    │
│         Brand new — completely isolated from EM1          │
│                                                           │
│  Persistence Context 2 (PC2) — MAP starts EMPTY           │
│  ┌───────────────────────────────────────────────────┐    │
│  │               (empty)                             │    │
│  └───────────────────────────────────────────────────┘    │
│                                                           │
│  ⚠️ Even though EM1 had UserDetails#1 in its PC           │
│     EM2 has absolutely no knowledge of that               │
│                                                           │
│  transaction.begin()                                      │
│                                                           │
│  find(UserDetails.class, 1L)  ← find #1                   │
│      │                                                    │
│      ▼                                                    │
│  Check PC2 map → EMPTY ❌ → go to DB                       │
│      │                                                    │
│      ▼                                                    │
│  SELECT query fires → fetches UserDetails{id=1,...}       │
│      │                                                    │
│      ▼                                                    │
│  Store result in PC2 map                                  │
│  ┌───────────────────────────────────────────────────┐    │
│  │  KEY: EntityKey[UserDetails, 1]                   │    │
│  │  VAL: UserDetails{id=1, name="xyz", ...}          │    │
│  └───────────────────────────────────────────────────┘    │
│                                                           │
│  find(UserDetails.class, 1L)  ← find #2                   │
│      │                                                    │
│      ▼                                                    │
│  Check PC2 map → KEY FOUND ✅ → return from memory         │
│  NO DB query                                              │
│                                                           │
│  transaction.commit() → nothing to flush                  │
│  entityManager2.close() → PC2 destroyed                   │
└───────────────────────────────────────────────────────────┘
```

### Console output after Session 2:

```sql
Hibernate:
    select ud1_0.id, ud1_0.email, ud1_0.name
    from user_details ud1_0
    where ud1_0.id=?

Session2: i am able to find the data, name is: xyz
```

Only **one SELECT** — even though `find()` was called **twice** in Session 2. The second call was served from PC2's cache.

---

## Full Picture — Both Sessions Together

```
┌─────────────────────────────────────────────────────────────────┐
│                    EntityManagerFactory                         │
│                  (one shared instance)                          │
└────────────────────────┬────────────────────────────────────────┘
                         │ creates
           ┌─────────────┴──────────────┐
           ▼                            ▼
┌─────────────────────┐      ┌─────────────────────┐
│   EntityManager 1   │      │   EntityManager 2   │
│                     │      │                     │
│  PC1 Map:           │      │  PC2 Map:           │
│  [UserDetails#1]✅   │  ✗   │  [empty at start]   │
│                     │      │  [UserDetails#1]✅   │
│  persist → in PC1   │      │  (after DB fetch)   │
│  find ×2 → cache ✅  │      │                     │
│  commit → INSERT    │      │  find #1 → DB hit   │
│  close → PC1 gone   │      │  find #2 → cache ✅  │
│                     │      │  commit → nothing   │
│                     │      │  close → PC2 gone   │
└─────────────────────┘      └─────────────────────┘

DB Queries:  1 INSERT              DB Queries: 1 SELECT
Cache hits:  2 finds               Cache hits: 1 find
```

---

## Total DB Queries Across Both Sessions

```
Session 1:   1 INSERT   (0 SELECT — both finds served from PC1)
Session 2:   1 SELECT   (second find served from PC2 cache)
─────────────────────────────────────────────────────────────
Total:       1 INSERT + 1 SELECT
```

The instructor confirms this in the console — exactly these two queries, nothing more.

---

## What This Example Proves

The instructor uses this manual example to make three things undeniably clear:

**1. The EntityManagerFactory is shared — EntityManagers are not**
```
Factory → created once at app startup → shared across everything
EM      → created fresh each time → has its own isolated PC
```

**2. Isolation is absolute**
Even if EM1 is still open and has data in its PC, EM2 cannot see any of it. No leakage, no sharing.

**3. The cache works even before flush**
Inside Session 1, both `find()` calls happened **before** `commit()` — meaning before the data was even in the DB. The PC was the only source, and it was enough.

---

## One Thing the Instructor Wants You to Always Remember

> *"1 EntityManager = 1 Persistence Context. EntityManager is created at every HTTP request. So all calls within the same HTTP request share the same EntityManager and therefore the same cache."*

This is the mental model that explains every caching behavior you'll ever see in JPA.

---

# Step 6 – Key Rules to Remember + Interview Tips

---

This is the consolidation step. Everything the instructor taught, distilled into clean rules — plus the interview angles he hints at throughout the video.

---

## The Complete Mental Model — One Final Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        JPA FIRST-LEVEL CACHE                        │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                   HTTP Request Lifecycle                    │    │
│  │                                                             │    │
│  │  Request arrives                                            │    │
│  │      │                                                      │    │
│  │      ▼                                                      │    │
│  │  DispatcherServlet → preHandle()                            │    │ 
│  │      │                                                      │    │
│  │      ▼                                                      │    │
│  │  EntityManager created  ──────────────────────────────┐     │    │
│  │      │                                                │     │    │
│  │      ▼                                                ▼     │    │ 
│  │  Persistence Context (MAP)               isolated scope     │    │
│  │  ┌──────────────────────────┐                         │     │    │
│  │  │ EntityKey → Entity obj   │                         │     │    │
│  │  │ EntityKey → Entity obj   │                         │     │    │
│  │  └──────────────────────────┘                         │     │    │
│  │      │                                                │     │    │
│  │      ▼                                                │     │    │
│  │  find() → check map first                             │     │    │
│  │      ├── HIT  → return from memory (no DB)            │     │    │
│  │      └── MISS → go to DB → store in map → return      │     │    │
│  │                                                       │     │    │
│  │  commit/flush → sync with DB                          │     │    │
│  │                                                       │     │    │
│  │  Response sent → EntityManager closed ────────────────┘     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                     │
│  New Request → New EntityManager → New empty Persistence Context    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The 6 Golden Rules

---

### Rule 1 — First-Level Cache is always ON
You cannot turn it off. It's not a feature you configure. It's built into the core of how JPA and Hibernate work. Every EntityManager automatically has its own Persistence Context acting as a cache.

---

### Rule 2 — Scope is tied to the EntityManager
```
First-Level Cache lives   → inside the Persistence Context
Persistence Context lives → inside the EntityManager
EntityManager lives       → for the duration of one HTTP request
                            (in a Spring MVC application)
```
When the EntityManager is closed → cache is gone. Simple as that.

---

### Rule 3 — Cache is checked before every DB call
Whenever you call `find()` or `findById()`, JPA doesn't immediately go to the database. It always checks the Persistence Context map first:

```
findById(1L)
    │
    ▼
Is EntityKey[UserDetails, 1] in the PC map?
    │
    ├── YES → return from map (no DB query)
    │
    └── NO  → go to DB, store result in map, return
```

---

### Rule 4 — persist() stores in cache first, DB comes later
When you call `persist()`, the entity goes into the Persistence Context map immediately. The actual INSERT to the database only happens at `flush()` / `commit()` time. So the cache can serve data that **hasn't even been written to DB yet**.

```
persist(entity) → in PC map immediately ✅
                  in DB only after commit ⏳
```

---

### Rule 5 — Two EntityManagers are completely isolated
No matter what — two EntityManagers have zero visibility into each other's Persistence Context. This is true even if:
- They were created from the same EntityManagerFactory
- They're running at the same time
- EM1 is still open when EM2 is created

```
EM1's PC  ✗──────✗  EM2's PC
(no sharing, no leakage, completely isolated)
```

---

### Rule 6 — This is why Second-Level Cache exists
First-Level Cache only works within one EntityManager — meaning one HTTP request. Across different requests (different EntityManagers), the cache is cold every time. This cross-request caching problem is exactly what **Second-Level Cache** solves — the instructor says that's coming in a later video.

```
Request 1 (EM1) → caches UserDetails#1 → EM1 closes → cache gone
Request 2 (EM2) → PC is empty → must hit DB again ❌

→ This is the gap Second-Level Cache fills
```

---

## Interview Tips — Straight From the Instructor's Explanation

---

### ❓ "What is First-Level Cache in JPA/Hibernate?"

**Answer framework:**
- First-Level Cache is the Persistence Context itself — an in-memory map inside the EntityManager
- It stores entities by their EntityKey (entity type + primary key)
- Every `find()` call checks this map before going to the database
- It's always enabled by default — you can't turn it off
- Its scope is tied to the EntityManager's lifetime

---

### ❓ "What is the scope of First-Level Cache?"

**Key answer:**
- In a Spring MVC app → one HTTP request = one EntityManager = one Persistence Context = one First-Level Cache
- When the request ends, the EntityManager is closed and the cache is destroyed
- Two different HTTP requests always get two completely separate caches

---

### ❓ "What is the difference between First-Level and Second-Level Cache?"

```
┌─────────────────────┬──────────────────────────────────┐
│  First-Level Cache  │  Second-Level Cache              │
├─────────────────────┼──────────────────────────────────┤
│  Always ON          │  Must be explicitly configured   │
│  Per EntityManager  │  Shared across EntityManagers    │
│  Per HTTP request   │  Shared across HTTP requests     │
│  Automatic          │  Needs setup (EhCache, Redis etc)│
│  Short lived        │  Long lived                      │
│  No config needed   │  Needs @Cacheable annotations    │
└─────────────────────┴──────────────────────────────────┘
```

---

### ❓ "If I call findById() twice in the same request, how many DB queries fire?"

**Answer:**
- Only **one** — the first call goes to the DB and stores the result in the Persistence Context
- The second call finds the entity already in the PC map and returns it from memory
- This is First-Level Caching in action

---

### ❓ "What is the relationship between EntityManager and Persistence Context?"

**Answer:**
- It's a strict **1-to-1 relationship**
- One EntityManager manages exactly one Persistence Context
- You can never share a Persistence Context across two EntityManagers
- The EntityManager is the API — the Persistence Context is the actual in-memory storage behind it

---

### ❓ "Who creates the EntityManager in a Spring Boot application?"

**Answer:**
- The `DispatcherServlet` — specifically via the `OpenEntityManagerInViewInterceptor`
- It runs in the `preHandle()` phase — **before** your controller method is even called
- It creates one EntityManager per HTTP request and binds it to that request's scope
- All repository calls, service calls, and any JPA operations within that request use this same EntityManager

---

### ❓ "Can two EntityManagers share the same Persistence Context?"

**Answer:** No — never. Each EntityManager has its own completely isolated Persistence Context. This is by design. It prevents one request from accidentally reading dirty/uncommitted data from another request's cache.

---

## Quick Revision Card

```
┌─────────────────────────────────────────────────────────┐
│            FIRST-LEVEL CACHE — QUICK REVISION           │
├─────────────────────────────────────────────────────────┤
│  What is it?    │  Persistence Context (a HashMap)      │
│  Who manages?   │  EntityManager (via Hibernate's       │
│                 │  SessionImpl)                         │
│  Scope?         │  One EntityManager = One PC           │
│                 │  One HTTP Request = One EM            │
│  Always ON?     │  Yes, cannot be disabled              │
│  When cached?   │  On persist() and on first find()     │
│  When evicted?  │  When EntityManager is closed         │
│  DB skipped?    │  Yes, if entity already in PC map     │
│  Cross-request? │  No — needs Second-Level Cache        │
│  Created by?    │  DispatcherServlet preHandle()        │
└─────────────────────────────────────────────────────────┘
```

---

## What's Coming Next — Second-Level Cache

The instructor closes with this:

> *"There is no knowledge of inter-persistence context. That's where Second-Level Caching comes into the picture. We will see it later."*

So the natural next topic is **Second-Level Cache** — which solves the cross-request caching problem by providing a shared cache across all EntityManagers, typically backed by something like EhCache or Redis.

---

## Full Notes Summary — All 6 Steps

```
Step 1 → JPA Lifecycle: entities live in PC before DB
Step 2 → The puzzle: save + fetch = 1 INSERT, 0 SELECT
Step 3 → Internals: PC is a HashMap, SessionImpl manages it
Step 4 → Demo: /test-jpa (cache hit) vs /read-jpa (DB hit)
Step 5 → Manual EM: isolation between two PCs proven by hand
Step 6 → Rules + Interview tips consolidated
```

---

That's the complete set of notes for **JPA Part 3 — First-Level Caching** from Concept & Coding. Everything the instructor taught, structured cleanly from problem → internals → demo → proof → rules → interview prep.