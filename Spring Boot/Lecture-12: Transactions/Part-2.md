# Step 1 — Hierarchy of Transaction Managers

---

## Why does this hierarchy even exist?

Before jumping into the layers, think about this: your application might talk to different kinds of databases — a relational DB using JDBC, or JPA, or Hibernate, or even a distributed system needing JTA. Each of these talks to the database differently. But Spring needs **one common contract** — a standard set of methods it can always call regardless of which database technology you're using underneath.

That's exactly why this hierarchy exists. It defines a **common interface at the top**, provides **default logic in the middle**, and lets **concrete implementations** handle technology-specific details at the bottom.

---

## The 4-Layer Hierarchy

```
        «interface»
      TransactionManager          ← Layer 1 (Top)
            |
            | extends
            ↓
        «interface»
  PlatformTransactionManager      ← Layer 2
   - getTransaction()
   - commit()
   - rollback()
            |
            | implements
            ↓
       «abstract class»
 AbstractPlatformTransactionManager ← Layer 3
  (default implementation of all
   3 methods above)
            |
     ________|_________________________________
     ↓          ↓            ↓             ↓
DataSource   Hibernate      JPA           JTA
Transaction  Transaction  Transaction  Transaction
Manager      Manager      Manager      Manager
  |                      (Java         (Java
  ↓                    Persistence   Transaction
JDBC                      API)          API)
Transaction
Manager
(Java DB
Connectivity)

←——————————————————————————————————→  ←——————————————→
   Manages LOCAL                          Manages
   Transactions                         DISTRIBUTED
                                       Transactions
```

---

## Layer by Layer Breakdown

### Layer 1 — `TransactionManager` (interface)
This is the **topmost interface**. It's actually empty — no methods inside it. It just exists as the root of the hierarchy, a marker to say *"this thing is a transaction manager."*

---

### Layer 2 — `PlatformTransactionManager` (interface)
This is the **most important interface** in the whole hierarchy. It defines exactly **3 methods** that every transaction manager must have:

| Method | What it does |
|---|---|
| `getTransaction()` | Creates or retrieves a transaction |
| `commit()` | Commits the transaction on success |
| `rollback()` | Rolls back the transaction on failure |

> **Connect this to Part 1:** Remember the interceptor from Part 1? When `@Transactional` is placed on a method, Spring's interceptor kicks in. Internally, it calls `getTransaction()` to start, `rollback()` inside the catch block if something fails, and `commit()` once everything succeeds. Those 3 methods — that's exactly what this interface defines.

---

### Layer 3 — `AbstractPlatformTransactionManager` (abstract class)
This is a **parent abstract class** that provides the **default implementation** of all 3 methods above.

Why is this needed? Because most transaction managers (JDBC, JPA, Hibernate, etc.) share a lot of **common logic** for how get/commit/rollback works. Instead of every concrete class rewriting the same logic, this abstract class handles the common parts. Each concrete class then only **overrides what's specific** to their technology.

---

### Layer 4 — Concrete Transaction Managers

These are the actual classes you'll use in your project. Each one is a child of the abstract class above:

**`DataSourceTransactionManager`**
Works with plain JDBC. You write raw SQL — insert queries, select queries, all manually. Has a child called `JdbcTransactionManager`.

**`HibernateTransactionManager`**
Works with the Hibernate ORM framework. Hibernate is technically a JPA implementation, but it's its own separate framework with its own transaction manager.

**`JpaTransactionManager`**
Works with JPA (Java Persistence API). You map your Java classes to DB tables as entities. Most CRUD operations happen without writing raw SQL. **This is what Spring Boot picks by default** when you just use `@Transactional`.

**`JtaTransactionManager`**
JTA = Java Transaction API. This one is special — it's used specifically for **distributed transactions** (like two-phase commit across multiple databases/services). All the others above handle *local* transactions only.

---

## What about NoSQL databases?

If you're using a relational database, any of the above managers work fine. But if you switch to a **NoSQL database** (like MongoDB, Cassandra, etc.), you need to change your library entirely. Once you bring in the right library, it will come with its **own transaction managers** that know how to talk to that NoSQL DB. The underlying concept remains the same though — get, commit, rollback.

---

## Interview Tip 💡
> If asked *"What is PlatformTransactionManager?"* — the answer is: it's the core interface in Spring's transaction abstraction. It defines 3 methods — `getTransaction`, `commit`, and `rollback` — that all transaction managers must implement, regardless of the underlying technology (JDBC, JPA, Hibernate, JTA).

> If asked *"Which transaction manager does Spring Boot use by default?"* — answer: **JpaTransactionManager**, as long as you're using a relational database with JPA on the classpath.

---

# Step 2 — Declarative vs Programmatic Transaction Management

---

## The Big Picture

Spring gives you **two ways** to manage transactions:

```
              Transaction Management
                       |
          _____________|_____________
          ↓                         ↓
     Declarative                Programmatic
  (via Annotation)             (via Code)
  
  Spring hides             You write the
  everything               logic yourself
  for you                  
  
  Simple but               Flexible but
  less flexible            harder to maintain
```

---

## Declarative Transaction Management

This is what you already know from Part 1. You simply put `@Transactional` on a method and Spring handles everything behind the scenes.

```java
@Component
public class User {

    @Transactional
    public void updateUser() {
        System.out.println("UPDATE QUERY to update the user db values");
    }
}
```

When you do this, Spring Boot **automatically picks the right transaction manager** based on whatever data source you're using (JDBC, JPA, etc.). You don't have to think about it.

---

### But what if you want to choose a specific transaction manager?

Say you don't want Spring to auto-pick JPA. You want to explicitly use the `DataSourceTransactionManager` (plain JDBC). Here's how you tell Spring Boot which one to use:

**Step 1 — Create a bean in your AppConfig class:**

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager userTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

> Notice the method is named `userTransactionManager` — Spring uses this **method name as the bean name** by default (unless you explicitly provide a name).

**Step 2 — Tell `@Transactional` which manager to use:**

```java
@Component
public class UserDeclarative {

    @Transactional(transactionManager = "userTransactionManager")
    public void updateUserProgrammatic() {
        // SOME DB OPERATIONS
        System.out.println("Insert Query ran");
        System.out.println("Update Query ran");
    }
}
```

When Spring sees `@Transactional(transactionManager = "userTransactionManager")`, it looks for a **bean with that exact name** and uses it. Simple.

---

## Programmatic Transaction Management

### First — WHY does it even exist?

This is the most important question. The instructor gives a very clear real-world scenario for this.

Imagine you have a method `updateUser()` that does **3 things in sequence:**

```
       updateUser()
            |
    ________|________
    ↓       ↓       ↓
  Step 1  Step 2  Step 3
  Update  External Update
   DB     API      DB
          Call
  (fast)  (SLOW   (fast)
        3-4 secs)
```

If you put `@Transactional` on top of `updateUser()`, here's what happens:

```
@Transactional
updateUser() {
    // Step 1 - Update DB          ← DB connection OPEN
    // Step 2 - External API call  ← DB connection still OPEN (waiting 3-4 sec)
    // Step 3 - Update DB          ← DB connection finally released
}
```

The DB connection stays **held open** for the entire duration — including the 3-4 seconds your external API call is taking. During peak traffic, when hundreds of requests are doing the same thing, your DB connection pool gets **choked**. The system slows down or crashes.

**The solution?** You want transactions only around Step 1 and Step 3 — not around the external API call. With `@Transactional` you can't do that — it wraps the entire method. This is exactly where **Programmatic** transaction management helps. You write the transaction logic yourself, so you control **exactly** which lines are inside a transaction and which aren't.

---

### The tradeoff:

| | Declarative | Programmatic |
|---|---|---|
| How | `@Transactional` annotation | Manual code |
| Control | Spring handles everything | You control every line |
| Flexibility | Less flexible | Very flexible |
| Maintenance | Easy (annotation only) | Hard (code in 100 places) |
| Best for | Standard DB operations | Mixed operations (DB + external API calls, etc.) |

---

## A Quick Note on How Spring Auto-selects Transaction Manager

When you use plain `@Transactional` without specifying a manager, Spring Boot looks at what's on your classpath and what datasource you've configured, and silently picks the most appropriate one. In most standard Spring Boot + JPA projects, it will pick **JpaTransactionManager**. You never even see it happen.

---

## Interview Tip 💡
> *"When would you choose Programmatic over Declarative?"*
> Answer: When your method mixes DB operations with time-consuming non-DB work like external API calls. Using `@Transactional` in that case keeps the DB connection open for too long, which becomes a bottleneck under high load. Programmatic lets you open and close transactions precisely around only the DB operations.

---

# Step 3 — Programmatic Approach 1
## Manually managing transactions using `PlatformTransactionManager`

---

## The Core Idea

In the declarative approach, Spring's interceptor was calling `getTransaction()`, `commit()`, and `rollback()` for you behind the scenes. In Approach 1, **you write that exact same logic yourself** — explicitly, in your own code.

```
        Declarative                    Programmatic Approach 1
        ___________                    ______________________
        
    Spring Interceptor                    YOUR CODE
    does this for you:                does this manually:
    
    - getTransaction()                - getTransaction()
    - your method runs                - your logic runs
    - commit() / rollback()           - commit() / rollback()
    
    (hidden from you)                 (written by you)
```

---

## Step-by-step breakdown

### Step 1 — Get a `PlatformTransactionManager` object

From the hierarchy we studied, `PlatformTransactionManager` is the interface that has all 3 methods. You need an object of this to call those methods. But since it's an interface, you can't do `new PlatformTransactionManager()` — you need a concrete class object.

So first, you create a bean in `AppConfig`:

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager userTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);  
        // telling Spring: use JDBC-based transaction manager
    }
}
```

Now during application startup, Spring creates this bean and keeps it ready.

---

### Step 2 — Inject it into your class

You can use either `@Autowired` or constructor injection (constructor injection is the cleaner, recommended way):

```java
@Component
public class UserDAO {

    PlatformTransactionManager userTransactionManager;

    UserDAO(PlatformTransactionManager userTransactionManager) {
        this.userTransactionManager = userTransactionManager;
        // Spring finds the bean we created in AppConfig
        // and injects it here automatically
    }
}
```

---

### Step 3 — Write your transaction logic manually

Now here's the full method with manual transaction handling:

```java
public void dbOperationWithRequiredPropagationUsingProgrammaticApproach1() {

    // Define transaction properties
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setName("Testing REQUIRED propagation");
    transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

    // Start the transaction (equivalent to what interceptor does in @Transactional)
    TransactionStatus status = userTransactionManager.getTransaction(transactionDefinition);

    try {
        // YOUR BUSINESS LOGIC GOES HERE
        System.out.println("Insert Query ran");
        System.out.println("Update Query ran");

        // Everything went well — commit
        userTransactionManager.commit(status);

    } catch (Exception e) {
        // Something went wrong — rollback
        userTransactionManager.rollback(status);
    }
}
```

---

## What is `DefaultTransactionDefinition`?

When the interceptor called `getTransaction()` in Part 1, it was passing `null` — meaning Spring would auto-assign a name and default propagation. Here, since you're doing it manually, you use `DefaultTransactionDefinition` to **explicitly set** these details yourself:

```
  DefaultTransactionDefinition
  ____________________________
  |                          |
  | .setName(...)            | ← give your transaction a meaningful name
  |                          |   (otherwise Spring assigns a random one)
  | .setPropagationBehavior  | ← tell Spring which propagation you want
  |  (PROPAGATION_REQUIRED)  |   (we'll cover all propagation types in Step 5)
  |__________________________|
```

Then you pass this definition object into `getTransaction()` so Spring knows exactly how to create the transaction.

---

## The full picture of what's happening:

```
   AppConfig
   _________
   Creates bean:
   PlatformTransactionManager  ←———————————————————┐
   (DataSourceTransactionManager)                   |
                                                    | injected via
                                                    | constructor
   UserDAO                                          |
   _______                                          |
   |                                                |
   | userTransactionManager ————————————————————————┘
   |
   | dbOperation...() {
   |
   |   1. DefaultTransactionDefinition   ← set name + propagation
   |              ↓
   |   2. getTransaction(definition)     ← transaction STARTS here
   |              ↓
   |   3. your DB logic runs
   |              ↓
   |   4a. commit(status)     ← if success
   |   4b. rollback(status)   ← if exception
   | }
```

---

## What's the problem with Approach 1?

It works, but look at your code — `getTransaction`, `commit`, `rollback`, the try-catch block — all of this is **transaction plumbing mixed right into your business logic**.

If you have 100 methods that need transactions, you're copy-pasting this structure 100 times. That's hard to maintain and messy to read.

This is why Spring gives you a cleaner option — **Approach 2 using `TransactionTemplate`**.

---

## Interview Tip 💡
> If asked *"How do you manage transactions programmatically in Spring?"*
> Answer: You inject a `PlatformTransactionManager` bean, create a `DefaultTransactionDefinition` to set the transaction name and propagation, call `getTransaction()` to start it, run your logic, and then explicitly call `commit()` or `rollback()` inside a try-catch. This gives fine-grained control over exactly which lines of code are wrapped in a transaction.

---

# Step 4 — Programmatic Approach 2
## Using `TransactionTemplate` — The Cleaner Way

---

## The Problem Approach 1 Left Behind

Look at what Approach 1 code looks like in practice:

```
Your method
___________

DefaultTransactionDefinition def = ...   ← transaction plumbing
TransactionStatus status = manager       ← transaction plumbing
                          .getTransaction(def)

try {
    // actual business logic               ← your real work
    insert query...
    update query...
    
    manager.commit(status)               ← transaction plumbing
} catch(Exception e) {
    manager.rollback(status)             ← transaction plumbing
}
```

Your **actual business logic** (the insert/update queries) is just 2 lines. But it's completely **buried inside transaction plumbing code**. Imagine doing this in 100 different methods. It becomes a nightmare to read and maintain.

Spring's solution? **`TransactionTemplate`** — it takes all that plumbing code and wraps it inside a reusable template, so your method only needs to provide the business logic.

---

## What is `TransactionTemplate`?

Think of it as a **blueprint or wrapper** that already has `getTransaction`, `commit`, and `rollback` baked inside it. You just hand it your business logic, and it handles the rest.

```
        TransactionTemplate  (a wrapper / blueprint)
        _______________________________________________
        |                                             |
        |   1. getTransaction()    ← already inside   |
        |                                             |
        |   2. YOUR BUSINESS       ← you provide      |
        |      LOGIC runs here       this part only   |
        |                                             |
        |   3. commit()            ← already inside   |
        |      or rollback()                          |
        |_____________________________________________|
```

You stop worrying about the transaction lifecycle. You only write what matters — your actual DB operations.

---

## Setting it up — AppConfig

Just like Approach 1 needed a `PlatformTransactionManager` bean, Approach 2 needs a `TransactionTemplate` bean. And when creating the `TransactionTemplate`, you tell it **which transaction manager to use**:

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager userTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionTemplate transactionTemplate(
                    PlatformTransactionManager userTransactionManager) {

        TransactionTemplate template = 
                    new TransactionTemplate(userTransactionManager);

        // you can also set propagation and name right here
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        template.setName("TRANSACTION TEMPLATE REQUIRED PROPAGATION");

        return template;
    }
}
```

> Notice: In Approach 1, you set the name and propagation inside the method at runtime using `DefaultTransactionDefinition`. In Approach 2, you set those **once in AppConfig** when creating the bean. Cleaner.

---

## Using it in your class

```java
@Component
public class UserDAO {

    TransactionTemplate transactionTemplate;

    UserDAO(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
        // Spring injects the bean created in AppConfig
    }

    public void dbOperationWithRequiredPropagationUsingProgrammaticApproach2() {

        // Define your business logic as a callback (lambda expression)
        TransactionCallback<TransactionStatus> operations = (TransactionStatus status) -> {
            // YOUR ACTUAL BUSINESS LOGIC HERE
            System.out.println("Insert Query ran");
            System.out.println("Update Query ran");
            return status;
        };

        // Hand it to the template — it handles get/commit/rollback
        TransactionStatus status = transactionTemplate.execute(operations);
    }
}
```

---

## What is `TransactionCallback`?

The `execute()` method of `TransactionTemplate` accepts a `TransactionCallback` — which is a **functional interface**. It has one method called `doInTransaction()`.

Since it's a functional interface, you can pass your logic as a **lambda expression** instead of writing a whole separate class. That's exactly what the instructor does above — the lambda is your business logic being passed as a function.

```
transactionTemplate.execute(
    your business logic as a lambda
)

internally this does:
1. getTransaction()
2. calls your lambda (doInTransaction)  ← your insert/update runs here
3. commit() if success
4. rollback() if exception
```

---

## Side by side comparison — Approach 1 vs Approach 2

```
APPROACH 1                              APPROACH 2
__________                              __________

DefaultTransactionDefinition def        (set once in AppConfig bean)
  .setName(...)
  .setPropagation(...)

TransactionStatus status =              transactionTemplate.execute(
  manager.getTransaction(def)             (status) -> {
                                      
try {                                       // business logic
  // business logic                         insert query
  insert query                              update query
  update query                              return status;
  manager.commit(status)                }
} catch(Exception e) {               );
  manager.rollback(status)
}

❌ Plumbing mixed with logic           ✅ Only business logic visible
❌ Repeat in every method              ✅ Template reused everywhere
❌ Harder to read                      ✅ Clean and readable
```

---

## The complete flow diagram

```
   AppConfig
   _________
   Bean 1: PlatformTransactionManager
           (DataSourceTransactionManager)
                      ↓ passed into
   Bean 2: TransactionTemplate
           - propagation set
           - name set
                      |
                      | injected via constructor
                      ↓
   UserDAO
   _______
   transactionTemplate.execute(
       lambda with your DB logic
   )
         |
         ↓
   TransactionTemplate internally:
   ┌─────────────────────────────┐
   │ 1. getTransaction()         │
   │ 2. runs your lambda         │ ← insert/update queries
   │ 3. commit() ✅              │
   │    or rollback() ❌         │
   └─────────────────────────────┘
```

---

## Interview Tip 💡
> If asked *"What is TransactionTemplate in Spring?"*
> Answer: It's a helper class that simplifies programmatic transaction management. It wraps the `getTransaction`, `commit`, and `rollback` calls inside a reusable template. You only need to provide your business logic as a `TransactionCallback` (using a lambda). It's cleaner than manually calling those methods yourself because the transaction plumbing is separated from your business logic.

> If asked *"What's the difference between Approach 1 and Approach 2 of programmatic transaction management?"*
> Answer: In Approach 1, you manually call `getTransaction`, `commit`, and `rollback` inside every method — mixing plumbing with logic. In Approach 2, `TransactionTemplate` handles that plumbing internally. You just pass your logic as a lambda to `execute()`, making the code cleaner and easier to maintain.

---

# Step 5 — Transaction Propagation
## How Spring decides what to do with transactions when methods call each other

---

## First — What is Propagation and Why does it exist?

Here's the situation that makes propagation necessary. Imagine this:

```
        Request comes in
               ↓
          Method One
          ──────────
          @Transactional        ← Spring creates Transaction 1
          
          // doing some work...
          
          calls Method Two ──→  Method Two
                                ──────────
                                @Transactional   ← tries to get a transaction
                                
                                // doing some work...
```

Now a very natural question comes up — when Method Two tries to get a transaction, **what should happen?**

Should it **join** Transaction 1 that Method One already created? Or should it **create a brand new** Transaction 2 of its own? Or should it run **without any transaction** at all?

This decision is controlled by the **Propagation** value you set on `@Transactional`.

> The instructor puts it simply: *"When we try to create a new transaction, it first checks the propagation value set, and based on that only it will decide whether to create a transaction or not."*

---

## The 6 Propagation Types

```
                    PROPAGATION TYPES
                    _________________
                    
        REQUIRED         ← default
        REQUIRED_NEW
        SUPPORTS
        NOT_SUPPORTED
        MANDATORY
        NEVER
```

Let's go through each one carefully.

---

## 1. REQUIRED (Default)

This is the **default propagation**. Even if you don't write anything, Spring uses this.

```java
@Transactional(propagation = Propagation.REQUIRED)
// same as just writing @Transactional
```

**The rule:**
```
If parent transaction present  →  USE the same transaction
If no parent transaction       →  CREATE a new transaction
```

**Example from the instructor's demo:**

Method One starts → Transaction 1 created with name:
`com.conceptandcoding.learningspringboot.TransactionManagement.UserDeclarative.updateUser`

Method Two is called (also `REQUIRED`):
- Is there a parent transaction? Yes.
- So Method Two **joins** Transaction 1.
- Transaction name inside Method Two? **Same as Method One's.**
- No new transaction was created.

```
Method One ────────────────────────────────────────────┐
  Transaction 1 (name: ...updateUser)                  │
  │                                                     │
  │   some initial DB work                              │
  │         │                                           │
  │         ↓                                           │
  │   calls Method Two                                  │
  │         │                                           │
  │         ↓                                           │
  │   Method Two runs INSIDE Transaction 1 ✅           │
  │   (same transaction, no new one created)            │
  │         │                                           │
  │         ↓                                           │
  │   some final DB work                                │
  └─────────────────────────────────────────────────────┘
  Transaction 1 commits
```

---

## 2. REQUIRED_NEW

```java
@Transactional(propagation = Propagation.REQUIRED_NEW)
```

**The rule:**
```
If parent transaction present  →  SUSPEND parent transaction
                                  CREATE a brand new transaction
                                  Once done, RESUME parent transaction
If no parent transaction       →  CREATE a new transaction
```

> Important: **Suspend ≠ Abort.** Suspending just means the parent transaction is paused and waiting. It is NOT rolled back or cancelled.

**Example from the instructor's demo:**

```
Method One ──────────────────────────────────────────────────┐
  Transaction 1 (name: ...updateUser)                        │
  │                                                           │
  │   some initial DB work                                    │
  │         │                                                 │
  │         ↓                                                 │
  │   calls Method Two                                        │
  │         │                                                 │
  │   Transaction 1 SUSPENDED ⏸️                              │
  │         ↓                                                 │
  │   Method Two runs in NEW Transaction 2 ✅                 │
  │   (name: ...methodTwo — DIFFERENT from parent)            │
  │         │                                                 │
  │   Transaction 2 commits or rolls back                     │
  │         │                                                 │
  │   Transaction 1 RESUMES ▶️                                │
  │         │                                                 │
  │   some final DB work                                      │
  └───────────────────────────────────────────────────────────┘
  Transaction 1 commits
```

---

## 3. SUPPORTS

```java
@Transactional(propagation = Propagation.SUPPORTS)
```

**The rule:**
```
If parent transaction present  →  USE it
If no parent transaction       →  Execute method WITHOUT any transaction
```

Think of it as: *"I'll work with a transaction if one exists, but I don't need one."*

This is useful for **read operations** where a transaction isn't strictly necessary but you'll participate in one if it's already there.

```
With parent txn:               Without parent txn:
────────────────               ───────────────────
Method Two joins               Method Two runs
parent transaction             with NO transaction
```

---

## 4. NOT_SUPPORTED

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
```

**The rule:**
```
If parent transaction present  →  SUSPEND parent transaction
                                  Execute method WITHOUT any transaction
                                  RESUME parent transaction
If no parent transaction       →  Execute method WITHOUT any transaction
```

Think of it as: *"I don't want a transaction, no matter what."* If a parent transaction exists, it gets pushed aside while this method runs, then brought back.

```
With parent txn:               Without parent txn:
────────────────               ───────────────────
Parent SUSPENDED ⏸️             Method Two runs
Method Two runs                with NO transaction
  with NO transaction
Parent RESUMES ▶️
```

---

## 5. MANDATORY

```java
@Transactional(propagation = Propagation.MANDATORY)
```

**The rule:**
```
If parent transaction present  →  USE it
If no parent transaction       →  THROW EXCEPTION ❌
```

Think of it as: *"A transaction MUST already exist before calling me. I will never create one myself."*

This is useful when you have a method that must only ever be called from within an existing transaction — it enforces that contract hard.

```
With parent txn:               Without parent txn:
────────────────               ───────────────────
Method Two joins               💥 Exception thrown
parent transaction             IllegalTransactionStateException
```

---

## 6. NEVER

```java
@Transactional(propagation = Propagation.NEVER)
```

**The rule:**
```
If parent transaction present  →  THROW EXCEPTION ❌
If no parent transaction       →  Execute method WITHOUT any transaction
```

This is the **opposite of MANDATORY**. It says: *"I must NOT be called from within a transaction. If you try, I'll throw an exception."*

```
With parent txn:               Without parent txn:
────────────────               ───────────────────
💥 Exception thrown            Method Two runs
                               with NO transaction
```

---

## All 6 at a glance

```
┌────────────────┬──────────────────────────────┬──────────────────────────────┐
│  Propagation   │   Parent txn EXISTS          │   No parent txn              │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ REQUIRED       │ Use parent txn               │ Create new txn               │
│ (default)      │                              │                              │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ REQUIRED_NEW   │ Suspend parent, create new   │ Create new txn               │
│                │ txn, resume parent after     │                              │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ SUPPORTS       │ Use parent txn               │ Run WITHOUT txn              │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ NOT_SUPPORTED  │ Suspend parent, run WITHOUT  │ Run WITHOUT txn              │
│                │ txn, resume parent after     │                              │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ MANDATORY      │ Use parent txn               │ 💥 Throw Exception           │
├────────────────┼──────────────────────────────┼──────────────────────────────┤
│ NEVER          │ 💥 Throw Exception           │ Run WITHOUT txn              │
└────────────────┴──────────────────────────────┴──────────────────────────────┘
```

---

## How to set Propagation — Declarative vs Programmatic

**Declarative (annotation):**
```java
@Transactional(propagation = Propagation.REQUIRED_NEW)
public void myMethod() {
    // your logic
}
```

**Programmatic Approach 1:**
```java
DefaultTransactionDefinition def = new DefaultTransactionDefinition();
def.setName("my transaction");
def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

TransactionStatus status = userTransactionManager.getTransaction(def);
try {
    // business logic
    userTransactionManager.commit(status);
} catch (Exception e) {
    userTransactionManager.rollback(status);
}
```

**Programmatic Approach 2 (TransactionTemplate):**
```java
// Set once in AppConfig bean:
template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
template.setName("my transaction");

// Then use normally:
transactionTemplate.execute((status) -> {
    // business logic
    return status;
});
```

---

## Where does Spring actually check propagation internally?

The instructor points this out — you can verify this yourself in Spring's source code. Inside `TransactionInterceptor`, there's a method called `createTransactionIfNecessary()`. That calls `getTransaction()`. Inside `getTransaction()` in `AbstractPlatformTransactionManager`, Spring checks the propagation value and runs the corresponding if/else logic — exactly what we described above for each propagation type.

---

## Interview Tips 💡

> *"What is the default propagation in Spring?"*
> **REQUIRED** — joins existing transaction if present, creates new one if not.

> *"What's the difference between REQUIRED and REQUIRED_NEW?"*
> REQUIRED reuses the parent transaction. REQUIRED_NEW always creates a fresh transaction, suspending the parent until it finishes.

> *"What's the difference between MANDATORY and NEVER?"*
> MANDATORY demands a parent transaction already exists — throws exception if not. NEVER demands NO parent transaction exists — throws exception if one is present. They are exact opposites.

> *"What's the difference between SUPPORTS and NOT_SUPPORTED?"*
> SUPPORTS participates in a transaction if one exists, otherwise runs without. NOT_SUPPORTED always runs without a transaction — it suspends the parent if one exists.

> *"What's suspend vs abort in REQUIRED_NEW?"*
> Suspend means the parent transaction is **paused**, not cancelled. Once the new transaction finishes, the parent **resumes** from where it left off.

---

## That wraps up the full lecture! Here's the complete overview of what we covered:

```
Part 2 — Transaction Management
│
├── Step 1: Hierarchy of Transaction Managers
│   └── TransactionManager → PlatformTransactionManager
│       → AbstractPlatformTransactionManager
│       → Concrete managers (JDBC, JPA, Hibernate, JTA)
│
├── Step 2: Declarative vs Programmatic
│   └── When external API calls make @Transactional dangerous
│
├── Step 3: Programmatic Approach 1
│   └── Manually calling getTransaction / commit / rollback
│
├── Step 4: Programmatic Approach 2
│   └── TransactionTemplate — cleaner wrapper approach
│
└── Step 5: Propagation (6 types)
    └── REQUIRED, REQUIRED_NEW, SUPPORTS,
        NOT_SUPPORTED, MANDATORY, NEVER
```

Next lecture the instructor says he'll cover **Isolation Levels** in depth — that'll be Part 3!