# Step 1 — What is Isolation Level & Why Does It Matter?

---

## Definition

> **Isolation Level** tells how the changes made by one transaction are visible to other transactions running in parallel.

That's the one-liner. Don't worry if it feels abstract right now — by the end of all steps, this definition will make complete sense.

---

## How do you use it in Spring Boot?

When you use the `@Transactional` annotation, you can specify the isolation level like this:

```java
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public void updateUser() {
    // some operations here
}
```

You simply pass `isolation = Isolation.<TYPE>` inside the annotation.

There are **4 types** of isolation levels:

```
1. READ_UNCOMMITTED
2. READ_COMMITTED
3. REPEATABLE_READ
4. SERIALIZABLE
```

---

## What if you don't specify any isolation level?

If you don't pass any isolation level, the **default isolation level of your database** kicks in.

⚠️ It is NOT always the same across databases. It depends on which DB you are using.

| Database | Default Isolation Level |
|----------|------------------------|
| MySQL | REPEATABLE_READ |
| PostgreSQL | READ_COMMITTED |

> 💡 So always check your DB's documentation to confirm what the default is. Don't assume!

---

## The Big Trade-off — Concurrency vs Safety

This is the core idea behind why isolation levels even exist.

```
READ_UNCOMMITTED  ──────────────────────►  SERIALIZABLE
       │                                         │
  Concurrency HIGH                        Concurrency LOW
  (many txns run in parallel)             (very few txns run in parallel)
  Risk HIGH                               Risk LOW
```

- **High concurrency** = more transactions can run at the same time = faster, but risky
- **Low concurrency** = fewer transactions run at the same time = safer, but slower

> You might think — *"high concurrency is always good, right? Let's just use READ_UNCOMMITTED!"*
>
> Not so fast. Every advantage comes with a disadvantage. That's exactly why we need to understand **what problems each isolation level solves** — and that's what Step 2 is all about.

---

## Why is this topic so important?

- It directly impacts **how your app behaves under concurrent load**
- It's a **very popular interview topic** — especially in HLD (High Level Design) rounds
- It's something you'll deal with in **day-to-day backend development**

---

# Step 2 — The 3 Concurrency Problems

Before understanding what each isolation level does, you first need to understand **what problems they are trying to solve**. There are exactly **3 such problems**.

---

## Problem 1 — Dirty Read

### Definition
> Transaction A reads the **uncommitted data** of another transaction. If that other transaction gets **rolled back**, the data which Transaction A already read never actually existed in the DB. This is called a **Dirty Read**.

### Timeline Example

```
TIME        TRANSACTION A            TRANSACTION B          DB STATUS
────────────────────────────────────────────────────────────────────────
 T1       BEGIN_TRANSACTION        BEGIN_TRANSACTION       Id: 123
                                                           Status: Free

 T2                                Update Row id:123       Id: 123
                                   Status = Booked         Status: Booked
                                   (NOT committed yet)     (NOT committed yet)

 T3       Read Row id:123                                  Id: 123
          (gets Status = Booked)                           Status: Booked
                                                           (still NOT committed)

 T4                                ROLLBACK               Id: 123
                                                           Status: Free
                                                           (changes undone)
────────────────────────────────────────────────────────────────────────
```

### What went wrong?
- At **T2**, Transaction B updated the row but **did not commit** yet
- At **T3**, Transaction A read that row and got `Status = Booked`
- At **T4**, Transaction B **rolled back** — meaning that "Booked" status never truly existed
- But Transaction A already read it and made decisions based on it!

> That stale, never-committed data that Transaction A read — that is called **Dirty Data**, and the problem is called **Dirty Read**.

---

## Problem 2 — Non-Repeatable Read

### Definition
> Transaction A reads the **same row multiple times** within the same transaction, but gets **different values** each time. This is called a **Non-Repeatable Read**.

### Timeline Example

```
TIME        TRANSACTION A              DB STATUS
──────────────────────────────────────────────────────────────────────
 T1       BEGIN_TRANSACTION           Id: 1
                                      Status: Free

 T2       Read Row Id:1               Id: 1
          (gets Status = Free)        Status: Free

 T3       -----                       Id: 1
                                      Status: Booked
                              ⬆️ Some OTHER transaction changed
                                 this and COMMITTED it

 T4       Read Row Id:1               Id: 1
          (gets Status = Booked)      Status: Booked

 T5       COMMIT
──────────────────────────────────────────────────────────────────────
```

### What went wrong?
- At **T2**, Transaction A read the row and got `Status = Free`
- At **T3**, some **other transaction** updated the same row and **committed** the change
- At **T4**, Transaction A read the **same row again** — but now gets `Status = Booked`
- Same query. Same transaction. **Different result.** That's the problem.

> Note the key difference from Dirty Read — here the other transaction **actually committed** its change. The problem is not about dirty data, it's about the **same row returning different values** within one transaction.

---

## Problem 3 — Phantom Read

### Definition
> Transaction A executes the **same query multiple times**, but the **number of rows returned is different** each time. This is called a **Phantom Read**.

### Timeline Example

```
TIME        TRANSACTION A                    DB STATUS
────────────────────────────────────────────────────────────────────────────
 T1       BEGIN_TRANSACTION                 Id:1, Status: Free
                                            Id:3, Status: Booked
                                            (2 rows total)

 T2       Read rows where                   Id:1, Status: Free
          ID > 0 AND ID < 5                 Id:3, Status: Booked
          (gets 2 rows → Id:1, Id:3)

 T3       ------                            Id:1, Status: Free
                                            Id:2, Status: Free  ⬅️ NEW ROW
                                            Id:3, Status: Booked
                                    ⬆️ Some OTHER transaction INSERTED
                                       Id:2 and COMMITTED it

 T4       Read rows where                   Id:1, Status: Free
          ID > 0 AND ID < 5                 Id:2, Status: Free
          (gets 3 rows → Id:1,Id:2,Id:3)    Id:3, Status: Booked

 T5       COMMIT
────────────────────────────────────────────────────────────────────────────
```

### What went wrong?
- At **T2**, Transaction A ran a range query and got **2 rows**
- At **T3**, another transaction **inserted a new row** (Id:2) and committed it
- At **T4**, Transaction A ran the **exact same query** — now gets **3 rows**
- The extra row that appeared out of nowhere is called a **Phantom Row**

> Key difference from Non-Repeatable Read:
> - Non-Repeatable Read → **same row, different value**
> - Phantom Read → **same query, different number of rows**

---

## Quick Summary of All 3 Problems

```
┌─────────────────────────┬──────────────────────────────────────────────────┐
│ Problem                 │ What happens                                     │
├─────────────────────────┼──────────────────────────────────────────────────┤
│ Dirty Read              │ You read data that was never actually committed  │
│ Non-Repeatable Read     │ Same row gives different value on re-read        │
│ Phantom Read            │ Same query gives different number of rows        │
└─────────────────────────┴──────────────────────────────────────────────────┘
```

---

# Step 3 — DB Locking (Shared Lock vs Exclusive Lock)

Before jumping into how each isolation level solves the 3 problems, you need to understand **how databases use locks** under the hood. Because isolation levels are essentially just **different locking strategies**.

---

## Why Locking?

> Locking makes sure that no other transaction can update (or sometimes even read) a locked row — until the lock is released.

This is what gives isolation levels their power. The DB Transaction Manager handles all of this automatically — you just tell it which isolation level to use, and it figures out which locks to apply, when to take them, and when to release them.

---

## Two Types of Locks

### 1. Shared Lock (S) — also called Read Lock

- Multiple transactions **can hold a shared lock on the same row at the same time**
- But **only for reading** — nobody can modify the row while a shared lock is held
- Think of it like a library book that many people can read at once, but nobody can write in it

```
                        ROW: Id:1, Status: Free
                               │
              ┌────────────────┴────────────────┐
              │                                 │
      Transaction 1                     Transaction 2
      Takes Shared Lock (S)             Takes Shared Lock (S)
      ✅ Can READ                        ✅ Can READ
      ❌ Cannot WRITE                    ❌ Cannot WRITE
```

### 2. Exclusive Lock (X) — also called Write Lock

- Only **one transaction** can hold an exclusive lock on a row at a time
- While this lock is held, **no other transaction can read OR write** that row
- Think of it like a locked editing room — only one person inside, nobody else can even peek in

```
                        ROW: Id:1, Status: Free
                               │
                       Transaction 1
                       Takes Exclusive Lock (X)
                       ✅ Can READ
                       ✅ Can WRITE
                               │
                       Transaction 2
                       ❌ Cannot READ
                       ❌ Cannot WRITE
                       (completely blocked)
```

---

## Lock Compatibility Table

This is important — understand which locks can co-exist:

```
┌──────────────────────┬──────────────────────┬────────────────────────┐
│ Current Lock on Row  │ Another Shared Lock? │ Another Exclusive Lock?│
├──────────────────────┼──────────────────────┼────────────────────────┤
│ Shared Lock (S)      │ ✅ YES — allowed      │ ❌ NO — blocked         │
│ Exclusive Lock (X)   │ ❌ NO — blocked       │ ❌ NO — blocked         │
└──────────────────────┴──────────────────────┴────────────────────────┘
```

In plain words:
- **Shared + Shared** → ✅ Fine, both can read together
- **Shared + Exclusive** → ❌ Not allowed, can't write while someone is reading
- **Exclusive + Shared** → ❌ Not allowed, can't even read while someone is writing
- **Exclusive + Exclusive** → ❌ Not allowed, only one writer at a time

---

## Very Important Point — Where does this locking code live?

You might wonder — *"I only write SELECT or UPDATE queries in my application. Where is all this locking happening?"*

> The locking logic is **not in your application code**. It lives inside the **DB Transaction Manager** — which is part of the database itself.

```
Your Application
      │
      │  "Hey, use READ_COMMITTED isolation level"
      │   + writes SELECT / INSERT / UPDATE queries
      ▼
DB Transaction Manager
      │
      │  Figures out:
      │  - Which lock to take (Shared or Exclusive)
      │  - When to take it
      │  - When to release it
      ▼
   Database
```

> You just declare the isolation level. Everything else is abstracted away by the DB Transaction Manager. That's the beauty of it.

---

## Quick Recap Before We Move On

```
┌─────────────────┬──────────────────┬───────────────────────────────────┐
│ Lock Type       │ Also Known As    │ Behaviour                         │
├─────────────────┼──────────────────┼───────────────────────────────────┤
│ Shared Lock (S) │ Read Lock        │ Multiple txns can read together   │
│                 │                  │ Nobody can write                  │
├─────────────────┼──────────────────┼───────────────────────────────────┤
│ Exclusive Lock  │ Write Lock       │ Only one txn can hold it          │
│ (X)             │                  │ Nobody else can read or write     │
└─────────────────┴──────────────────┴───────────────────────────────────┘
```

---

# Step 4 — The 4 Isolation Levels

Now this is where everything comes together. We'll go through each isolation level one by one — what locking strategy it uses, which problems it solves, and which it doesn't.

Here's the full picture we'll be building up to:

```
┌─────────────────────┬──────────────┬──────────────────────┬──────────────────────┐
│ Isolation Level     │ Dirty Read   │ Non-Repeatable Read  │ Phantom Read         │
├─────────────────────┼──────────────┼──────────────────────┼──────────────────────┤
│ READ_UNCOMMITTED    │ ❌ Possible   │ ❌ Possible           │ ❌ Possible           │
│ READ_COMMITTED      │ ✅ Solved     │ ❌ Possible           │ ❌ Possible           │
│ REPEATABLE_READ     │ ✅ Solved     │ ✅ Solved             │ ❌ Possible           │
│ SERIALIZABLE        │ ✅ Solved     │ ✅ Solved             │ ✅ Solved             │
└─────────────────────┴──────────────┴──────────────────────┴──────────────────────┘

                   Concurrency HIGH ◄────────────────► Concurrency LOW
```

---

## Isolation Level 1 — READ_UNCOMMITTED

### Locking Strategy
```
┌────────────────────────────────────────────────────────┐
│  READ  → No lock acquired at all                       │
│  WRITE → No lock acquired at all                       │
└────────────────────────────────────────────────────────┘
```

### Problems Solved
```
Dirty Read          → ❌ NOT solved
Non-Repeatable Read → ❌ NOT solved
Phantom Read        → ❌ NOT solved
```

### Why does it fail all three?

Since there is **absolutely no locking** — neither for reads nor for writes:

```
ROW: Id:1, Status: Free

Transaction 1                          Transaction 2
─────────────────────────────────────────────────────
Read Id:1                              
(no lock taken, just reads it)         

                                       Write Id:1 → Status: Booked
                                       (no lock taken, just writes it)
                                       ← NOT committed yet

Read Id:1 again                        
(gets Booked — uncommitted data!)      

                                       ROLLBACK
                                       (Booked never existed!)

Transaction 1 already acted on
dirty data → Dirty Read Problem!
```

Same story for Non-Repeatable and Phantom Read — since there's no locking at all, any other transaction can freely change or insert data at any point.

### Then why does this isolation level even exist?

> READ_UNCOMMITTED is only useful when your application is **purely read-only** and has **static data** that never changes. In that case, none of the 3 problems will actually affect your business logic — and you get **maximum concurrency** in return.

⚠️ Outside of that very specific case — this isolation level is **very risky** to use.

---

## Isolation Level 2 — READ_COMMITTED

### Locking Strategy
```
┌────────────────────────────────────────────────────────────────────────┐
│  READ  → Shared Lock acquired, but released AS SOON AS read is done    │
│  WRITE → Exclusive Lock acquired, kept till END of transaction         │
└────────────────────────────────────────────────────────────────────────┘
```

### Problems Solved
```
Dirty Read          → ✅ SOLVED
Non-Repeatable Read → ❌ NOT solved
Phantom Read        → ❌ NOT solved
```

### How does it solve Dirty Read?

Because of the **Exclusive Lock on Write** — held till end of transaction:

```
ROW: Id:1, Status: Free

Transaction 1                          Transaction 2
──────────────────────────────────────────────────────────────────
                                       Takes Exclusive Lock (X)
                                       Updates Status → Booked
                                       (NOT committed yet,
                                        lock still held)

Try to Read Id:1                       
❌ BLOCKED — Exclusive lock            
   is present, cannot read             

                                       COMMIT / ROLLBACK
                                       Exclusive Lock released

Now Read Id:1                          
✅ Gets the committed value            
   (either Booked or Free              
    depending on what happened)        
──────────────────────────────────────────────────────────────────
```

> Transaction 1 cannot read the row until Transaction 2 either commits or rolls back. So it will **always read committed data only**. Dirty Read is solved ✅

### Why does Non-Repeatable Read still happen?

Because the **Shared Lock on Read is released immediately** after reading:

```
ROW: Id:1, Status: Free

Transaction 1                          Transaction 2
──────────────────────────────────────────────────────────────────
Takes Shared Lock (S)
Reads Id:1 → Status: Free
Releases Shared Lock immediately ✅    

                                       Takes Exclusive Lock (X)
                                       Updates Status → Booked
                                       COMMITS
                                       Exclusive Lock released

Reads Id:1 again                       
Gets Status: Booked 😱                 
(value changed between reads!)         
──────────────────────────────────────────────────────────────────
```

> Since the shared lock was released right after the first read, Transaction 2 was free to come in and change the value. When Transaction 1 reads again — different value. **Non-Repeatable Read is NOT solved** ❌

> Phantom Read is also not solved for the same reason — range queries release their shared lock immediately, so another transaction can freely insert new rows in between reads.

---

## Isolation Level 3 — REPEATABLE_READ

### Locking Strategy
```
┌──────────────────────────────────────────────────────────────────────────────┐
│  READ  → Shared Lock acquired, kept till END of transaction (not released    │
│          immediately like READ_COMMITTED)                                    │
│  WRITE → Exclusive Lock acquired, kept till END of transaction               │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Problems Solved
```
Dirty Read          → ✅ SOLVED
Non-Repeatable Read → ✅ SOLVED
Phantom Read        → ❌ NOT solved
```

### How does it solve Non-Repeatable Read?

Because the **Shared Lock is now held for the entire transaction**:

```
ROW: Id:1, Status: Free

Transaction 1                          Transaction 2
──────────────────────────────────────────────────────────────────
Takes Shared Lock (S)
Reads Id:1 → Status: Free
🔒 Lock NOT released yet               

                                       Tries to take Exclusive Lock
                                       to update Id:1
                                       ❌ BLOCKED — Shared Lock
                                          is still present!

Reads Id:1 again                       
Gets Status: Free ✅                   
(same value — nobody could change it)  

COMMIT
Shared Lock released                   

                                       Now Transaction 2 can
                                       take Exclusive Lock
                                       and update
──────────────────────────────────────────────────────────────────
```

> Since the shared lock is held for the entire duration of Transaction 1, no other transaction can modify that row in between. So no matter how many times Transaction 1 reads the same row — it always gets the same value. **Non-Repeatable Read is solved** ✅

### Why does Phantom Read still happen?

Because the shared lock only locks the **rows that were actually read** — it does NOT lock the **gaps between rows** where new rows could be inserted:

```
DB State:
Id:1, Status: Free
Id:3, Status: Booked

Transaction 1                          Transaction 2
──────────────────────────────────────────────────────────────────
Takes Shared Lock on Id:1 🔒
Takes Shared Lock on Id:3 🔒
Reads rows where ID > 0 AND ID < 5
Gets 2 rows (Id:1, Id:3)

                                       Inserts NEW row:
                                       Id:2, Status: Free
                                       ✅ No lock on Id:2 gap!
                                       COMMITS

Reads rows where ID > 0 AND ID < 5
Gets 3 rows (Id:1, Id:2, Id:3) 😱
(new phantom row appeared!)
──────────────────────────────────────────────────────────────────
```

> The gap where Id:2 sits had no lock on it — so Transaction 2 could freely insert there. **Phantom Read is NOT solved** ❌

---

## Isolation Level 4 — SERIALIZABLE

### Locking Strategy
```
┌────────────────────────────────────────────────────────────────────────────────┐
│  Same as REPEATABLE_READ, PLUS applies a RANGE LOCK                            │
│                                                                                │
│  READ  → Shared Lock acquired + Range Lock on the query range,                 │
│          both kept till END of transaction                                     │
│  WRITE → Exclusive Lock acquired, kept till END of transaction                 │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Problems Solved
```
Dirty Read          → ✅ SOLVED
Non-Repeatable Read → ✅ SOLVED
Phantom Read        → ✅ SOLVED
```

### How does Range Lock solve Phantom Read?

```
DB State:
Id:1, Status: Free
Id:3, Status: Booked

Transaction 1                          Transaction 2
──────────────────────────────────────────────────────────────────
Reads rows where ID > 0 AND ID < 5

Takes Shared Lock on Id:1 🔒
Takes Shared Lock on Id:3 🔒
Takes RANGE LOCK on entire
range 0 to 5 🔒🔒🔒
(locks the gaps too — including
where Id:2 would sit)

                                       Tries to INSERT Id:2
                                       ❌ BLOCKED — Range Lock
                                          covers that gap!
                                          Cannot insert!

Reads rows where ID > 0 AND ID < 5
Gets same 2 rows ✅
(Id:1, Id:3 — no new rows!)

COMMIT
All locks released                     

                                       Now Transaction 2 can
                                       insert Id:2
──────────────────────────────────────────────────────────────────
```

> The Range Lock covers not just the existing rows, but also **any gap within the queried range** where new rows could be inserted. So no phantom rows can appear. **Phantom Read is solved** ✅

### The trade-off

> Because Serializable locks everything — rows and gaps — for the entire duration of the transaction, **very few transactions can run in parallel**. This means **concurrency is very low**. It's the safest but the slowest.

---

## Full Locking Strategy Summary

```
┌──────────────────────┬─────────────────────────────────────────────────────────────┐
│ Isolation Level      │ Locking Strategy                                            │
├──────────────────────┼─────────────────────────────────────────────────────────────┤
│ READ_UNCOMMITTED     │ Read  → No lock                                             │
│                      │ Write → No lock                                             │
├──────────────────────┼─────────────────────────────────────────────────────────────┤
│ READ_COMMITTED       │ Read  → Shared Lock, released immediately after read        │
│                      │ Write → Exclusive Lock, held till end of transaction        │
├──────────────────────┼─────────────────────────────────────────────────────────────┤
│ REPEATABLE_READ      │ Read  → Shared Lock, held till end of transaction           │
│                      │ Write → Exclusive Lock, held till end of transaction        │
├──────────────────────┼─────────────────────────────────────────────────────────────┤
│ SERIALIZABLE         │ Read  → Shared Lock + Range Lock, held till end             │
│                      │ Write → Exclusive Lock, held till end of transaction        │
└──────────────────────┴─────────────────────────────────────────────────────────────┘
```

---

# Step 5 — Interview Tips & When to Use Which Isolation Level

---

## The Golden Question Interviewers Ask

> *"Which isolation level would you use for this system, and why?"*

This is a very common question in **HLD (High Level Design)** rounds. Shreyansh gives a very clean framework to answer this — let's break it down.

---

## How to Decide — The Decision Framework

Don't just blurt out an isolation level. Instead, **ask a clarifying question first**:

```
Interviewer: "Which isolation level would you use?"

You: "Can I ask — is Non-Repeatable Read a problem
      for this use case or not?"
```

Based on the answer:

```
                    Is Non-Repeatable Read
                      a problem here?
                            │
              ┌─────────────┴──────────────┐
              │                            │
             NO                           YES
              │                            │
              ▼                            ▼
      Use READ_COMMITTED          Use REPEATABLE_READ
      (good concurrency,          (slightly lower concurrency,
       dirty read solved)          but non-repeatable read
                                   also solved)
```

> In most real-world backend systems, you'll be choosing between these two. SERIALIZABLE is rarely used in practice because its concurrency is too low for most high-traffic systems.

---

## When to Use Each — Real World Thinking

```
┌──────────────────────┬───────────────────────────────────────────────────────┐
│ Isolation Level      │ When to use it                                        │
├──────────────────────┼───────────────────────────────────────────────────────┤
│ READ_UNCOMMITTED     │ Only when your app is purely read-only with static    │
│                      │ data. You need maximum concurrency and you are 100%   │
│                      │ sure none of the 3 problems will affect your          │
│                      │ business logic.                                       │
├──────────────────────┼───────────────────────────────────────────────────────┤
│ READ_COMMITTED       │ When dirty reads are unacceptable but you can         │
│                      │ tolerate a row's value changing between reads within  │
│                      │ the same transaction. Good balance of safety and      │
│                      │ concurrency. Most commonly used in practice.          │
├──────────────────────┼───────────────────────────────────────────────────────┤
│ REPEATABLE_READ      │ When you need to read the same row multiple times     │
│                      │ within one transaction and the value must stay        │
│                      │ consistent. E.g. ticket booking, bank balance checks. │
├──────────────────────┼───────────────────────────────────────────────────────┤
│ SERIALIZABLE         │ When absolute data integrity is needed and you can    │
│                      │ afford very low concurrency. Rare in practice.        │
└──────────────────────┴───────────────────────────────────────────────────────┘
```

---

## A Practical Example to Nail This in Interviews

Let's say the interviewer asks:

> *"You are building a flight seat booking system. Which isolation level would you use?"*

Here's how you think through it:

```
Step 1: Is Dirty Read acceptable?
        → No! We can't let a user book a seat based on
          uncommitted data that might get rolled back.
        → So READ_UNCOMMITTED is out.

Step 2: Is Non-Repeatable Read a problem?
        → Yes! If we check seat availability and it changes
          before we confirm the booking, that's a real problem.
        → So READ_COMMITTED is not enough.

Step 3: Is Phantom Read a problem?
        → Possibly, but for row-level booking (one seat = one row),
          phantom read is less of a concern than non-repeatable read.
        → REPEATABLE_READ is likely good enough here.

Answer: Use REPEATABLE_READ
```

---

## The Complete Picture — Everything in One Place

```
┌─────────────────────┬────────────┬────────────────────┬──────────────┬─────────────────┐
│ Isolation Level     │ Dirty Read │ Non-Repeatable Read│ Phantom Read │ Concurrency     │
├─────────────────────┼────────────┼────────────────────┼──────────────┼─────────────────┤
│ READ_UNCOMMITTED    │ ❌ Possible │ ❌ Possible         │ ❌ Possible   │ 🔥 Very High     │
│ READ_COMMITTED      │ ✅ Solved   │ ❌ Possible         │ ❌ Possible   │ ⚡ High          │
│ REPEATABLE_READ     │ ✅ Solved   │ ✅ Solved           │ ❌ Possible   │ 🔶 Medium       │
│ SERIALIZABLE        │ ✅ Solved   │ ✅ Solved           │ ✅ Solved     │ 🐢 Very Low     │
└─────────────────────┴────────────┴────────────────────┴──────────────┴─────────────────┘
```

---

## Key Takeaways — What to Remember Forever

```
1. Isolation Level = controls how visible one transaction's
   changes are to other parallel transactions

2. Three problems to always think about:
   → Dirty Read (reading uncommitted data)
   → Non-Repeatable Read (same row, different value on re-read)
   → Phantom Read (same query, different number of rows)

3. Two types of locks under the hood:
   → Shared Lock (Read Lock) — multiple txns can hold together
   → Exclusive Lock (Write Lock) — only one txn, blocks everyone

4. The higher the isolation → safer but slower
   The lower the isolation → faster but riskier

5. In real world — mostly choose between
   READ_COMMITTED and REPEATABLE_READ

6. Always ask the interviewer:
   "Is Non-Repeatable Read a problem in this use case?"
   before picking an isolation level
```

---

## One Last Important Point — Always Check Your DB Default

> Never assume the default isolation level. Always check what your specific database uses as default and confirm it matches your application's requirements.

```
MySQL      → REPEATABLE_READ  (default)
PostgreSQL → READ_COMMITTED   (default)
```

If you are not specifying an isolation level in `@Transactional`, you are silently relying on whatever your DB defaults to — which may or may not be right for your use case.

---

✅ **All 5 Steps Complete!**

That's the entire lecture on Isolation Levels — from what it is, to the 3 problems, to locking, to all 4 isolation levels, to how to answer this in interviews. Everything Shreyansh covered, in clean structured notes.

Let me know if you want a **single consolidated cheat sheet** of the entire lecture, or if you want to move on to the next topic! 🎯