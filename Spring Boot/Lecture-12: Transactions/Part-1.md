# Step 1 — The Problem: Critical Section & Data Inconsistency

---

## What is a Critical Section?

> **Definition:** A code segment where shared resources are being accessed AND modified.

Simple enough. But why does this matter? Let's build up with a real-world example the instructor uses.

---

## Real-World Example — Cab Booking 🚕

Imagine you have a **Car table in your database** that looks like this:

```
┌────────┬───────────┐
│   ID   │  Status   │
├────────┼───────────┤
│  1001  │ Available │
└────────┴───────────┘
```

And you have a **code segment** that does this:

```
{
  Read Car Row with id: 1001
  If Status is Available:
      Update it to Booked
}
```

This code segment is your **Critical Section** — because it is both *reading* and *modifying* a shared resource (the car row in DB).

---

## Where's the Problem?

Everything works perfectly fine when **only one request** hits this code at a time.

The real trouble starts when **multiple requests hit this critical section in parallel.**

Let's say **4 people** try to book the same cab at the same time:

```
Request 1 ──┐
Request 2 ──┤──▶  { Read Car 1001 → Status: Available → Update to Booked }
Request 3 ──┤
Request 4 ──┘
```

Here's what actually happens, step by step:

```
┌───────────┬──────────────────────────────┬─────────────────────┐
│  Request  │         Reads Status         │       Writes        │
├───────────┼──────────────────────────────┼─────────────────────┤
│     1     │  Available ✅ (go ahead!)     │  Booked ✅           │
│     2     │  Available ✅ (go ahead!)     │  Booked ✅           │
│     3     │  Available ✅ (go ahead!)     │  Booked ✅           │
│     4     │  Available ✅ (go ahead!)     │  Booked ✅           │
└───────────┴──────────────────────────────┴─────────────────────┘
```

ALL 4 requests read the status as **Available** before any of them had a chance to write **Booked** — so all 4 go ahead and book the same cab!

**Result:**
```
"Your car is booked!" ✅  → Request 1
"Your car is booked!" ✅  → Request 2
"Your car is booked!" ✅  → Request 3
"Your car is booked!" ✅  → Request 4
```

**One cab. Four bookings. Complete mess.** This is called **Data Inconsistency.**

---

## Why Does This Happen?

Because the critical section is **not protected.** There is no mechanism that says:

> *"Hey, while Request 1 is reading and updating this row — nobody else is allowed to touch it."*

Without that protection, all requests run freely and simultaneously, reading stale data and overwriting each other.

---

## The Core Insight 💡

```
Critical Section
       +
Multiple Parallel Requests
       =
Data Inconsistency  ⚠️
```

This is the exact problem that **Transactions** are designed to solve — which is what we'll cover in the next step.

---

# Step 2 — The Solution: Transactions & ACID Properties

---

## What is a Transaction?

A **Transaction** is a group of operations that are treated as a **single unit of work.**

The instructor puts it simply — a transaction helps achieve the **ACID property.** You may have studied this in your first or second year of college in DBMS. Let's revisit it properly, because understanding ACID is the foundation of understanding `@Transactional`.

---

## ACID — One by One

The instructor explains each property with a **money transfer example.** Let's use that.

### Setup:
```
┌──────┬────────┐
│  A   │  ₹10  │
│  B   │  ₹20  │
└──────┴────────┘

Transaction: Debit ₹5 from A, Credit ₹5 to B
```

This transaction has **two operations:**
```
Operation 1 → Debit  A by ₹5
Operation 2 → Credit B by ₹5
```

---

### A — Atomicity ⚛️

> **"All or nothing."**
> If ANY operation inside a transaction fails — the ENTIRE transaction is rolled back.

**Scenario — what if Operation 2 fails?**

```
Operation 1: Debit A  ✅  →  A becomes ₹5
Operation 2: Credit B ❌  →  Something went wrong!
```

Without atomicity, you'd be left with:
```
A = ₹5   (debited)
B = ₹20  (never credited) ← ₹5 just vanished!
```

**With atomicity**, since Operation 2 failed — Operation 1 is also **rolled back:**
```
A = ₹10  ← restored back ✅
B = ₹20  ← unchanged     ✅
```

The entire transaction is undone. No partial updates. Money is safe.

---

### C — Consistency 📏

> **"Before and after a transaction, the database must always be in a consistent state."**

Think of it as — the DB should never be left in a broken or half-done state.

```
BEFORE transaction:        AFTER successful transaction:
┌──────┬────────┐          ┌──────┬────────┐
│  A   │  ₹10  │          │  A   │  ₹5   │
│  B   │  ₹20  │    →     │  B   │  ₹25  │
└──────┴────────┘          └──────┴────────┘
   Consistent ✅               Consistent ✅
   (₹30 total)                 (₹30 total)


AFTER failed transaction (with Atomicity kicking in):
┌──────┬────────┐
│  A   │  ₹10  │
│  B   │  ₹20  │
└──────┴────────┘
   Consistent ✅
   (₹30 total — rolled back)
```

The **inconsistent state** that must never happen:
```
┌──────┬────────┐
│  A   │  ₹5   │  ← debited
│  B   │  ₹20  │  ← never credited  ❌
└──────┴────────┘
   INCONSISTENT ❌
   (₹25 total — ₹5 missing!)
```

Atomicity and Consistency are deeply linked — Atomicity is the *mechanism*, Consistency is the *guarantee.*

---

### I — Isolation 🔒

> **"Even if multiple transactions run in parallel, they should NOT interfere with each other."**
> Each transaction should feel like it's running alone, in its own isolated environment.

**Example — 3 transactions running at the same time, all wanting to update A:**

```
Transaction 1 → Debit A ₹5,  Credit B ₹5
Transaction 2 → Debit A ₹2,  Credit B ₹2
Transaction 3 → Credit A ₹3, Debit  B ₹3
```

From the **outside**, it looks like all 3 are running in parallel simultaneously. But internally, transactions use **locking** to make sure only one of them touches a resource at a time:

```
Outside view (what we see):
T1 ──────────────────▶
T2 ──────────────────▶  (appears parallel)
T3 ──────────────────▶

Inside reality (what actually happens):
T1 acquires lock on A → does its work → releases lock
                   T2 acquires lock on A → does its work → releases lock
                                      T3 acquires lock on A → does its work
```

Each transaction feels like it ran in isolation — no interference, no dirty reads, no confusion.

---

### D — Durability 💾

> **"Once a transaction is committed — that data is permanent. Even if the system crashes, the data will NOT be lost."**

```
Transaction commits ✅
        ↓
Data written to DB permanently
        ↓
System crashes 💥
        ↓
Data is still there after restart ✅
```

This is why databases write to **disk logs** — so even a power failure can't undo a committed transaction.

---

## Full ACID Picture

```
┌─────────────────┬────────────────────────────────────────────────────┐
│    Property     │                    Guarantees                      │
├─────────────────┼────────────────────────────────────────────────────┤
│  Atomicity      │  All operations succeed, or all are rolled back    │
│  Consistency    │  DB is always in a valid state before & after      │
│  Isolation      │  Parallel transactions don't interfere             │
│  Durability     │  Committed data survives crashes                   │
└─────────────────┴────────────────────────────────────────────────────┘
```

---

## Why Does This Matter in Real Life?

The instructor specifically points this out —

> Financial applications — banking, payments, trading — almost always use databases that support ACID properties. Because in finance, you simply cannot afford data inconsistency.

---

## So — Transactions Solve Our Cab Problem

Going back to the cab booking example from Step 1 — with a transaction wrapping that critical section, only **one request at a time** can hold the lock, read the status, and update it. The other 3 wait their turn. No more 4 bookings for 1 cab.

---

# Step 3 — The Old Way vs The New Way

---

## How a Transaction Actually Works — The Manual Way

Before Spring Boot made things easy, if you wanted transaction support, you had to write it yourself — **every single time**, in **every single method** that touched the database.

The structure looks like this:

```
BEGIN_TRANSACTION
    
    Operation 1  →  Debit from A
    Operation 2  →  Credit to B

    if all success:
        COMMIT        ← persist to DB permanently
    else:
        ROLLBACK      ← undo everything

END_TRANSACTION
```

Clean and logical, right? But here's where the real pain begins.

---

## The Problem With Doing This Manually

Let's say your application has a `UserService` class with multiple methods — all of them touching the database:

```
class UserService {

    updateUser()          → updates user in DB
    updateBulkUsers()     → updates many users in DB
    updateUserById()      → updates user by ID in DB
    updateUserByEmail()   → updates user by email in DB
    ... and so on
}
```

Now, for EACH of these methods, you have to wrap your logic like this:

```
updateUser() {

    BEGIN_TRANSACTION          ← boilerplate
    
        // your actual logic
        debit from A
        credit to B
    
    if all success:
        COMMIT                 ← boilerplate
    else:
        ROLLBACK               ← boilerplate
    
    END_TRANSACTION            ← boilerplate
}


updateBulkUsers() {

    BEGIN_TRANSACTION          ← same boilerplate again
    
        // your actual logic
    
    if all success:
        COMMIT                 ← same boilerplate again
    else:
        ROLLBACK               ← same boilerplate again
    
    END_TRANSACTION            ← same boilerplate again
}
```

And this is just ONE class. Imagine doing this across:

```
UserService      → 10 methods
EmployeeService  → 10 methods
OrderService     → 10 methods
CarService       → 10 methods
...and 50 more classes
```

---

## What's the Actual Problem Here?

The instructor points out something very important — look at your method carefully:

```
updateUser() {

    BEGIN_TRANSACTION     ← NOT your business logic
    
        debit from A      ← ✅ YOUR actual business logic
        credit to B       ← ✅ YOUR actual business logic
    
    if all success:
        COMMIT            ← NOT your business logic
    else:
        ROLLBACK          ← NOT your business logic
    
    END_TRANSACTION       ← NOT your business logic
}
```

Out of everything written in this method — **only 2 lines are actually yours.** The rest is just repetitive ceremony that has nothing to do with your real work.

```
┌─────────────────────────────────────────┐
│  Your actual business logic    →  20%   │
│  Boilerplate transaction code  →  80%   │
└─────────────────────────────────────────┘
```

And this 80% boilerplate is **identical** across hundreds of methods. You're copying and pasting the same wrapper everywhere. This is exactly what good code should avoid.

---

## The New Way — `@Transactional` ✨

Spring Boot solves this entire problem with just **one annotation:**

```java
@Component
public class User {

    @Transactional
    public void updateUser() {
        // just your business logic here
        // debit from A
        // credit to B
    }
}
```

That's it. No `BEGIN_TRANSACTION`. No `COMMIT`. No `ROLLBACK`. No `END_TRANSACTION`.

All that boilerplate is now **handled automatically** by Spring Boot behind the scenes. You just focus on what actually matters — your business logic.

---

## Side by Side Comparison

```
WITHOUT @Transactional          │   WITH @Transactional
────────────────────────────────┼──────────────────────────────
BEGIN_TRANSACTION               │
                                │   @Transactional
  debit from A                  │   public void updateUser() {
  credit to B                   │       debit from A
                                │       credit to B
if all success:                 │   }
    COMMIT                      │
else:                           │
    ROLLBACK                    │
                                │
END_TRANSACTION                 │
────────────────────────────────┼──────────────────────────────
Repeated in every method ❌     │   Written once, works everywhere ✅
You manage transaction ❌       │   Spring manages transaction ✅
Cluttered with boilerplate ❌   │   Clean business logic only ✅
```

---

## Quick Tip the Instructor Mentions 📝

`@Transactional` can be applied at **two levels:**

```
@Transactional
├── Class Level
│     └── Applied automatically to ALL public methods
│         (private methods are NOT affected)
│
└── Method Level
      └── Applied to THAT specific method only
```

**Class level example:**
```java
@Component
@Transactional          ← applies to ALL public methods below
public class CarService {

    public void updateCar() {
        // runs inside a transaction ✅
    }

    public void updateBulkCars() {
        // runs inside a transaction ✅
    }

    private void helperMethod() {
        // NOT affected ❌ (private method)
    }
}
```

**Method level example:**
```java
@Component
public class CarService {

    @Transactional      ← applies to THIS method only
    public void updateCar() {
        // runs inside a transaction ✅
    }

    public void updateBulkCars() {
        // does NOT run inside a transaction ❌
    }
}
```

---

## The Flow So Far 🗺️

```
Problem                    Solution                  Spring Boot Way
──────────────────────────────────────────────────────────────────────
Critical Section      →    Use Transactions     →    @Transactional
(data inconsistency)       (ACID guarantee)          (no boilerplate)
```

---

# Step 4 — Setup: Dependencies & Configuration

---

## What Do You Need to Get `@Transactional` Working?

The instructor walks through this clearly. There are **two mandatory things** and **one optional thing** you need to set up.

---

## 1. Dependencies in `pom.xml`

You need **two dependencies** added to your `pom.xml`:

### Dependency 1 — Spring Boot Data JPA
This is the main dependency that gives you the `@Transactional` annotation when you're working with a **relational database** (MySQL, PostgreSQL, etc.)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

> **What is Data JPA?**
> It stands for Java Persistence API. It helps your Spring Boot application interact with relational databases without writing a lot of repetitive SQL code.

### Dependency 2 — Database Driver
This tells your application **which specific database** it's connecting to. It changes depending on what DB you're using.

```xml
<!-- Example: If you're using MySQL -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Example: If you're using PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

> **Note from the instructor:** The exact database driver dependency he's not covering in detail here — because the very next topic after `@Transactional` will cover database setup in depth. For now, just check Spring Boot's official documentation for whichever DB you're learning with, and you'll find the right driver dependency to add.

---

### What About NoSQL Databases?

The instructor specifically mentions this — `@Transactional` is **not limited to relational databases only.** Many NoSQL databases also support transactions.

```
Relational DB (MySQL, PostgreSQL...)  →  use spring-boot-starter-data-jpa
NoSQL DB (MongoDB, Cassandra...)      →  dependency changes accordingly
                                         but @Transactional still works
```

The annotation stays the same — only the dependency that provides it changes based on your database.

---

### Also — `application.properties`

Along with the driver dependency, you also need to provide your database credentials in `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/yourdbname
spring.datasource.username=root
spring.datasource.password=yourpassword
```

Again — the instructor says this will be covered properly in the next topic. For now just be aware it's needed.

---

## 2. `@EnableTransactionManagement` — Optional

In your main Spring Boot application class, you can add this annotation:

```java
@SpringBootApplication
@EnableTransactionManagement        ← this one
public class SpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class, args);
    }
}
```

### But Wait — Is This Actually Required?

The instructor is very clear about this:

> **It's optional.** Spring Boot auto-configures so many things for us automatically — and `@EnableTransactionManagement` is one of them. Spring Boot adds it on its own even if you don't write it.

So why does the instructor even mention it?

```
┌─────────────────────────────────────────────────────────────┐
│  Just a heads up —                                          │
│  If for some reason Spring Boot does NOT auto-add it,       │
│  and your @Transactional annotations are being ignored,     │
│  this is the first place to check.                          │
│  Manually adding @EnableTransactionManagement will fix it.  │
└─────────────────────────────────────────────────────────────┘
```

Think of it as a **safety net** — you probably won't need it, but it's good to know it exists.

---

## Full Setup Summary

```
┌──────────────────────────────────────────────────────────────────┐
│                    SETUP CHECKLIST                               │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  pom.xml                                                         │
│  ├── ✅  spring-boot-starter-data-jpa    (mandatory)              │
│  └── ✅  database driver dependency      (mandatory)              │
│                                                                  │
│  application.properties                                          │
│  └── ✅  datasource url, username,       (mandatory)              │
│          password                                                │
│                                                                  │
│  Main Application Class                                          │
│  └── ⚙️  @EnableTransactionManagement    (optional —             │
│                                           auto-added by          │
│                                           Spring Boot)           │
└──────────────────────────────────────────────────────────────────┘
```

---

## One Thing to Keep in Mind

The instructor makes a very important point here about where `@Transactional` comes from:

```
Which DB you use
      ↓
determines which dependency you add
      ↓
which determines where @Transactional annotation comes from
```

So the annotation itself looks the same — `@Transactional` — but the **import at the top of your file** may differ slightly based on which dependency is providing it. Always make sure you're importing from the right package.

For relational DB with JPA, it comes from:
```java
import org.springframework.transaction.annotation.Transactional;
```

---

Now that setup is out of the way, we get to the most interesting part of this lecture.

# Step 5 — How `@Transactional` Works Internally

---

## First — Why You Need to Know AOP

The instructor starts this section with a strong recommendation:

> *"Transaction management in Spring Boot uses AOP internally. That's why I told you that AOP is the first prerequisite. Because here you have to know about Pointcut and Advice — otherwise you will not understand how transaction management actually works internally."*

So before we go further — here's a **quick refresher** on AOP concepts you need to know:

```
AOP — Aspect Oriented Programming
│
├── Pointcut Expression
│     └── A rule that says "match these specific methods"
│
├── Advice
│     └── The code that runs when a pointcut matches
│         ├── Before  → runs before your method
│         ├── After   → runs after your method
│         └── Around  → wraps around your method
│                       (runs before AND after)
│
└── Join Point
      └── The actual method being intercepted
          (a placeholder inside advice to invoke your method)
```

---

## The Core Problem AOP Solves (Revisited)

Remember from Step 3 — your method was cluttered:

```java
updateUser() {
    BEGIN_TRANSACTION        ← duplicate boilerplate
        debit from A         ← ✅ your actual logic
        credit to B          ← ✅ your actual logic
    if success: COMMIT       ← duplicate boilerplate
    else: ROLLBACK           ← duplicate boilerplate
    END_TRANSACTION          ← duplicate boilerplate
}
```

AOP says:

> *"You focus only on your business logic. All that repetitive boilerplate? I'll take care of it."*

So with `@Transactional`, you only write this:

```java
@Transactional
public void updateUser() {
    debit from A    ← just your logic
    credit to B     ← just your logic
}
```

But the `BEGIN_TRANSACTION`, `COMMIT`, `ROLLBACK` haven't disappeared — they're just **moved somewhere else.** Let's see exactly where.

---

## How It All Fits Together Internally

Here is the complete internal flow, step by step:

### Step A — Application Starts Up

When your Spring Boot application starts, AOP scans your entire codebase using a **Pointcut Expression.**

The pointcut expression that `@Transactional` uses looks something like this:

```java
@within(org.springframework.transaction.annotation.Transactional)
```

In plain English, this means:

> *"Go through all packages, all subpackages, all classes, all methods — and find every method that has the `@Transactional` annotation on it."*

```
Application Starts
       ↓
AOP scans codebase with Pointcut Expression
       ↓
Finds every method annotated with @Transactional
       ↓
Pointcut Match! ✅
```

---

### Step B — What Happens When a Match is Found?

When the pointcut expression finds a match, it triggers an **Advice** to run.

The type of advice used here is **Around Advice** — because it needs to wrap around your method (run something before AND after your method executes).

```
Pointcut matches @Transactional method
              ↓
    Around Advice is triggered
```

---

### Step C — Where Does This Advice Actually Live?

This is the most important part the instructor explains. The advice method is:

```
Method name  →  invokeWithinTransaction
Lives inside →  TransactionalInterceptor class
                (which extends TransactionAspectSupport — its parent class)
```

So the actual boilerplate code — `BEGIN_TRANSACTION`, `COMMIT`, `ROLLBACK` — is sitting inside `invokeWithinTransaction` method in Spring Boot's own framework code. **You never have to write it. It's already there.**

---

## The Full Internal Flow — Visualized

```
Your Code                    Spring Boot Internally
─────────────────────────────────────────────────────────────────

@Transactional               Pointcut Expression scans
public void updateUser() {   and finds this method ──────────────────┐
    // your logic                                                      │
}                                                                      ▼
                                                          Around Advice triggered
                                                                       │
                                                                       ▼
                                              invokeWithinTransaction() {
                                                                       │
                                                  BEGIN_TRANSACTION ◀──┘
                                                         │
                                                         ▼
                                                  invoke your method
                                                  (updateUser runs here)
                                                         │
                                              ┌──────────┴──────────┐
                                              │                     │
                                        No Exception           Exception
                                              │                occurred ❌
                                              ▼                     │
                                           COMMIT ✅            ROLLBACK 🔄
                                              │                     │
                                              └──────────┬──────────┘
                                                         │
                                                  END_TRANSACTION
                                              }
```

---

## The Actual Code Inside `invokeWithinTransaction`

The instructor opens this method live and walks through it. Here's a simplified version of what it does:

```java
protected Object invokeWithinTransaction(...) {

    // 1. BEGIN TRANSACTION
    createTransactionIfNecessary(...)

    Object retVal;

    try {
        // 2. THIS IS WHERE YOUR METHOD GETS CALLED
        // (the join point — your actual business logic runs here)
        retVal = invocation.proceedWithInvocation();

    } catch (Throwable ex) {

        // 3. ANY EXCEPTION? → ROLLBACK
        completeTransactionAfterThrowing(..., ex);
        throw ex;
    }

    // 4. ALL SUCCESS? → COMMIT
    commitTransactionAfterReturning(...);

    return retVal;
}
```

Map this back to what we wrote manually in Step 3:

```
createTransactionIfNecessary()      →   BEGIN_TRANSACTION
invocation.proceedWithInvocation()  →   your method runs (join point)
completeTransactionAfterThrowing()  →   ROLLBACK on exception
commitTransactionAfterReturning()   →   COMMIT on success
```

**Nothing magical.** The same code you'd write manually — just written once inside Spring Boot's framework, hidden from you, and reused across every `@Transactional` method in your entire application.

---

## Why This is Powerful

```
WITHOUT @Transactional                 WITH @Transactional
──────────────────────────────────────────────────────────
UserService    → 10 methods            Just add @Transactional
EmployeeService → 10 methods           to each method
OrderService   → 10 methods            
CarService     → 10 methods            Spring Boot handles
                                       ALL the boilerplate
= 40 methods × same boilerplate        for ALL 40 methods
= 40 copies of the same code ❌        from ONE place ✅
```

---

## One Clean Real-World Code Example

Here's exactly what the instructor demonstrates:

```java
// Controller
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @Autowired
    User user;

    @GetMapping(path = "/updateuser")
    public String updateUser() {
        user.updateUser();
        return "user is updated successfully";
    }
}


// Service
@Component
public class User {

    @Transactional          ← that's all you need
    public void updateUser() {
        System.out.println("UPDATE QUERY TO update the user db values");
    }
}
```

When `/api/updateuser` is hit:

```
Request hits Controller
        ↓
controller calls user.updateUser()
        ↓
AOP intercepts (pointcut matched @Transactional)
        ↓
invokeWithinTransaction runs
        ↓
BEGIN_TRANSACTION
        ↓
your updateUser() logic runs
        ↓
No exception → COMMIT ✅
```

If you throw an exception inside `updateUser()`:

```java
@Transactional
public void updateUser() {
    // some DB operations
    throw new RuntimeException("something went wrong");
}
```

```
BEGIN_TRANSACTION
        ↓
your updateUser() logic runs
        ↓
RuntimeException thrown ❌
        ↓
invokeWithinTransaction catches it
        ↓
ROLLBACK 🔄  ← DB restored to previous state
```

---

## The Big Picture — Everything Connected

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   Critical Section → Data Inconsistency                     │
│              ↓                                              │
│         Use Transactions (ACID)                             │
│              ↓                                              │
│   Manual transaction code → boilerplate everywhere          │
│              ↓                                              │
│        Use @Transactional                                   │
│              ↓                                              │
│   AOP scans for @Transactional (Pointcut)                   │
│              ↓                                              │
│   Around Advice triggered (invokeWithinTransaction)         │
│              ↓                                              │
│   BEGIN → your method → COMMIT or ROLLBACK                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

# Final Step — Recap & What's Coming Next

---

## Quick Recap — Everything We Covered in Part 1

Before looking ahead, let's consolidate the entire Part 1 into one clean flow:

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  PROBLEM                                                             │
│  ├── Critical Section = code that reads & modifies shared resource   │
│  ├── Multiple parallel requests → Data Inconsistency                 │
│  └── Example: 4 people booking 1 cab simultaneously                  │
│                                                                      │
│  SOLUTION                                                            │
│  ├── Transactions → guarantee ACID properties                        │
│  ├── Atomicity   → all or nothing                                    │
│  ├── Consistency → DB always in valid state                          │
│  ├── Isolation   → parallel transactions don't interfere             │
│  └── Durability  → committed data survives crashes                   │
│                                                                      │
│  OLD WAY                                                             │
│  ├── Manually write BEGIN, COMMIT, ROLLBACK                          │
│  ├── Repeated in every method that touches DB                        │
│  └── 80% boilerplate, 20% actual business logic                      │
│                                                                      │
│  NEW WAY                                                             │
│  ├── Just use @Transactional annotation                              │
│  ├── Class level → applies to all public methods                     │
│  └── Method level → applies to that specific method only             │
│                                                                      │
│  SETUP                                                               │
│  ├── spring-boot-starter-data-jpa (mandatory)                        │
│  ├── Database driver dependency (mandatory)                          │
│  ├── application.properties credentials (mandatory)                  │
│  └── @EnableTransactionManagement (optional — auto-added)            │
│                                                                      │
│  HOW IT WORKS INTERNALLY                                             │
│  ├── Uses AOP under the hood                                         │
│  ├── Pointcut expression scans for @Transactional methods            │
│  ├── Around Advice triggered on match                                │
│  ├── invokeWithinTransaction() inside TransactionalInterceptor       │
│  └── BEGIN → your method → COMMIT (success) or ROLLBACK (failure)    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## What's Coming in Part 2

The instructor ends Part 1 by listing everything that will be covered next. These are all **properties and configurations of `@Transactional`** that give you fine-grained control over how your transactions behave.

Here's a breakdown of each topic with a quick one-liner on what it means — so you know what to expect:

---

### 1. Transaction Context
```
What is the "context" in which a transaction lives?
How does Spring Boot keep track of an ongoing transaction?
```

---

### 2. Transaction Manager
Two ways to manage transactions in Spring Boot:

```
├── Programmatic
│     └── You manually write transaction logic in code
│         (like the old BEGIN/COMMIT/ROLLBACK way)
│
└── Declarative
      └── You use @Transactional annotation
          (what we learnt in Part 1)
```

---

### 3. Propagation — Most Important Topic ⚠️
This controls **what happens when one `@Transactional` method calls another `@Transactional` method.**

```
Method A (has @Transactional)
    calls
Method B (also has @Transactional)

Question: Does Method B join A's transaction?
          Or does it create a brand new one?
          → That's what Propagation controls.
```

There are 7 types the instructor will cover:

```
├── REQUIRED        → use existing transaction, or create new one
├── REQUIRED_NEW    → always create a brand new transaction
├── SUPPORTS        → use existing if available, else run without
├── NOT_SUPPORTED   → always run without a transaction
├── MANDATORY       → must have existing transaction, else throw error
├── NEVER           → must NOT have a transaction, else throw error
└── NESTED          → run inside a nested transaction
```

---

### 4. Isolation Level
Controls **how much one transaction is isolated from others** running in parallel — specifically around reading data.

```
├── READ_UNCOMMITTED  → can read uncommitted data from other transactions
├── READ_COMMITTED    → can only read committed data
├── REPEATABLE_READ   → same read returns same result within transaction
└── SERIALIZABLE      → strictest — full isolation, one at a time
```

---

### 5. Configure Transaction Timeout
```
How long should a transaction wait before it gives up?
→ You can set a timeout limit on your @Transactional method
```

---

### 6. Read Only Transaction
```
If your method is only READING data (no writes)
→ mark it as readOnly = true
→ Spring Boot can optimize performance for this case
```

---

## The Road Ahead — Part 2 Topics at a Glance

```
@Transactional
│
├── Transaction Context
│
├── Transaction Manager
│     ├── Programmatic
│     └── Declarative
│
├── Propagation  ⚠️ (very important)
│     ├── REQUIRED
│     ├── REQUIRED_NEW
│     ├── SUPPORTS
│     ├── NOT_SUPPORTED
│     ├── MANDATORY
│     ├── NEVER
│     └── NESTED
│
├── Isolation Level
│     ├── READ_UNCOMMITTED
│     ├── READ_COMMITTED
│     ├── REPEATABLE_READ
│     └── SERIALIZABLE
│
├── Transaction Timeout
│
└── Read Only Transaction
```

---

## Interview Tips & Tricks 🎯

The instructor doesn't explicitly call these out as interview tips — but based on the concepts stressed repeatedly throughout Part 1, here are the things most likely to come up:

**1. What is a Critical Section?**
> A code segment where shared resources are being accessed and modified. When multiple parallel requests hit it, data inconsistency can happen.

**2. What is ACID? Explain each property.**
> Be ready to explain with the money transfer example. Atomicity = all or nothing. Consistency = DB always valid. Isolation = parallel transactions don't interfere. Durability = committed data survives crashes.

**3. How does `@Transactional` work internally?**
> It uses AOP. A Pointcut expression scans for methods annotated with `@Transactional`. When a match is found, an Around Advice is triggered — specifically the `invokeWithinTransaction()` method inside `TransactionalInterceptor` class. This method handles BEGIN, COMMIT, and ROLLBACK automatically.

**4. What's the difference between class-level and method-level `@Transactional`?**
> Class level applies to all public methods. Method level applies only to that specific method. Private methods are never affected regardless.

**5. Is `@EnableTransactionManagement` required?**
> No — Spring Boot auto-configures it. But if transactions aren't working, this is the first thing to check.

**6. What type of AOP Advice does `@Transactional` use?**
> Around Advice — because it needs to run code both before (BEGIN) and after (COMMIT/ROLLBACK) your method.

---

## That's a Wrap on Part 1! 🎉

```
Part 1 covered  →  The WHY and HOW of @Transactional
Part 2 covers   →  The fine-grained CONTROL of @Transactional
```

These notes cover everything the instructor taught — from the problem, to the solution, to the internals, to the setup. Part 2 is where things get deeper and more practical — especially **Propagation** which the instructor specifically flags as very important.