# Step 1 — Introduction to Association Types + @OneToOne Unidirectional

---

## What are Association Types?

When you have multiple tables in a database, they are rarely completely independent. They relate to each other. JPA gives us annotations to map these **relationships between entities (tables)** directly in Java code.

The four types are:

```
One-to-One     →   One user has exactly one address
One-to-Many    →   One user has many orders
Many-to-One    →   Many orders belong to one user
Many-to-Many   →   Many students have many courses
```

Today's focus: **One-to-One**, both unidirectional and bidirectional.

---

## @OneToOne Unidirectional

### The Concept

"Unidirectional" means the **relationship exists only in one direction** — from parent to child.

```
UserDetails  ──────────────►  UserAddress
  (Parent)                      (Child)

  ✅ From UserDetails, you CAN access UserAddress
  ❌ From UserAddress, you CANNOT access UserDetails
```

Think of it like a **one-way street.** You can travel from UserDetails to UserAddress, but not the other way around.

### Real-World Example

One user has only one address. The `UserDetails` entity holds a reference (foreign key) to `UserAddress`. The address table has no idea which user it belongs to.

---

## How it looks in the Database

```
USER_DETAILS table                    USER_ADDRESS table
┌────┬──────────────────┬──────┬────────┐    ┌────┬────────────┬──────────┬───────┬─────────┬──────────┐
│ ID │ USER_ADDRESS_ID  │ NAME │ PHONE  │    │ ID │   STREET   │   CITY   │ STATE │ COUNTRY │ PIN_CODE │
│    │   (Foreign Key)  │      │        │    │    │            │          │       │         │          │
└────┴──────────────────┴──────┴────────┘    └────┴────────────┴──────────┴───────┴─────────┴──────────┘
         │                                              ▲
         └──────────────────────────────────────────────┘
              FK points to PK of USER_ADDRESS
```

The foreign key lives **inside the parent table** (`USER_DETAILS`), pointing to the primary key of `USER_ADDRESS`.

---

## The Code

### Entity 1 — UserDetails (Parent / Owner)

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;

    @OneToOne(cascade = CascadeType.ALL)
    private UserAddress userAddress;   // FK will be created here

    // constructors, getters, setters
}
```

### Entity 2 — UserAddress (Child)

```java
@Entity
@Table(name = "user_address")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    // constructors, getters, setters
}
```

Notice — `UserAddress` has **no reference** to `UserDetails`. That's what makes it unidirectional.

---

## What does @OneToOne actually do?

When JPA sees `@OneToOne` on the `userAddress` field inside `UserDetails`, it says:

> "Okay, I need to create a **foreign key column** inside the `user_details` table that points to the primary key of the `user_address` table."

```
@OneToOne
private UserAddress userAddress;
        │
        ▼
Hibernate creates column: user_address_id  (inside USER_DETAILS table)
                                  │
                                  └──► points to ID column of USER_ADDRESS table
```

### Hibernate's Default Naming Rule

If you don't specify a name for the foreign key, Hibernate follows this rule:

```
field name  +  _id   =   foreign key column name

"userAddress"  +  "_id"  =  "user_address_id"
```

---

## Taking Control with @JoinColumn

What if you don't want Hibernate to decide the FK name? Use `@JoinColumn`.

```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

```
@JoinColumn breakdown:

  name = "address_id"         →  this is what the FK column will be called
                                  in the USER_DETAILS table

  referencedColumnName = "id" →  this is the column it points to
                                  in the USER_ADDRESS table
```

Result in the database:

```
USER_DETAILS table
┌────┬────────────┬──────┬────────┐
│ ID │ ADDRESS_ID │ NAME │ PHONE  │   ← "address_id" instead of "user_address_id"
└────┴────────────┴──────┴────────┘
```

---

## What About Composite Keys?

A **composite key** means a table's primary key is made up of **more than one column** combined. For example, in `UserAddress`, both `street` + `pinCode` together form a unique identity.

```
UserAddress with Composite Key:

  street = "123 MG Road"   ┐
                            ├──► together they are unique = composite key
  pinCode = "560001"        ┘
```

In this case, `@JoinColumn` alone won't work because there's more than one column to reference. You need `@JoinColumns` (plural).

### Setting up the Composite Key

```java
// Step 1: Create the embeddable composite key class
@Embeddable
public class UserAddressCK {
    private String street;
    private String pinCode;
    // getters, setters
}

// Step 2: Use it inside UserAddress
@Entity
@Table(name = "user_address")
public class UserAddress {

    @EmbeddedId
    private UserAddressCK id;   // composite key

    private String city;
    private String state;
    private String country;
    // getters, setters
}
```

### Referencing a Composite Key from UserDetails

```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumns({
    @JoinColumn(name = "address_street",   referencedColumnName = "street"),
    @JoinColumn(name = "address_pin_code", referencedColumnName = "pinCode")
})
private UserAddress userAddress;
```

```
USER_DETAILS table with composite FK:

┌────┬────────────────┬──────────────────┬──────┬────────┐
│ ID │ ADDRESS_STREET │ ADDRESS_PIN_CODE │ NAME │ PHONE  │
└────┴────────────────┴──────────────────┴──────┴────────┘
         │                    │
         ▼                    ▼
    street column        pinCode column
    in USER_ADDRESS      in USER_ADDRESS
```

**Why can't Hibernate figure this out automatically?**

With a single primary key, Hibernate knows exactly which column to reference. But with a composite key, there are multiple columns — Hibernate has no way to know which ones you want. So **you must explicitly mention all columns** using `@JoinColumns`.

---

## Quick Summary of Step 1

```
Concept          What it does
─────────────────────────────────────────────────────────
@OneToOne        Creates a FK in the parent table pointing to child table
@JoinColumn      Lets YOU name the FK column and specify which column it references
@JoinColumns     Same as above, but for composite keys (multiple columns)
@Embeddable      Marks a class as a composite key definition
@EmbeddedId      Uses that composite key class inside an entity
```

---
# Step 2 — CascadeType (All 6 Types) In Depth

---

## The Problem CascadeType Solves

First, let's understand **why** CascadeType even exists.

Imagine you have a parent entity `UserDetails` and a child entity `UserAddress`. The child's **existence depends on the parent**. If the parent is deleted, the child should also be deleted. If the parent is inserted, the child should also be inserted.

**Without CascadeType**, JPA treats them as completely independent:

```
WITHOUT CascadeType:

  save(userDetails)     →   only UserDetails gets saved
                            UserAddress is IGNORED ❌

  delete(userDetails)   →   only UserDetails gets deleted
                            UserAddress still sits in DB ❌  (data mess!)

  update(userDetails)   →   only UserDetails gets updated
                            UserAddress stays old ❌
```

Managing child entities manually every single time is **error-prone** and messy.

**With CascadeType**, any operation on the parent **automatically flows down** to the child:

```
WITH CascadeType:

  save(userDetails)     →   UserDetails saved  ✅
                            UserAddress saved  ✅  (automatically!)

  delete(userDetails)   →   UserDetails deleted ✅
                            UserAddress deleted ✅  (automatically!)
```

Think of it like this — **cascade = "waterfall effect"** from parent to child.

---

## Real World Analogy

```
        SCHOOL  (parent)
           │
           │  if school is destroyed...
           ▼
        ROOMS   (child)

Rooms cannot exist without the school.
If school is gone → rooms should also be gone.
It makes no sense to have rooms of a school that no longer exists.
```

This is exactly the kind of relationship CascadeType is designed to manage.

---

## The 6 CascadeType Values

```
CascadeType
    │
    ├── PERSIST   →  Insert
    ├── MERGE     →  Update
    ├── REMOVE    →  Delete
    ├── REFRESH   →  Bypass cache, re-read from DB
    ├── DETACH    →  Remove from persistence context
    └── ALL       →  All of the above
```

Let's go through each one in detail.

---

## 1. CascadeType.PERSIST

**What it does:** When you **insert/save** the parent entity, the child entity is also automatically inserted.

```java
@OneToOne(cascade = CascadeType.PERSIST)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

### Flow

```
POST /api/user
Body: {
  "name": "JohnXYZ",
  "phone": "1234567890",
  "userAddress": {
    "street": "123 Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "India",
    "pinCode": "10001"
  }
}

         ▼

  userDetailsRepository.save(userDetails)

         ▼

  Hibernate fires TWO insert queries:

  INSERT INTO user_address (city, country, pin_code, state, street)
  VALUES (?, ?, ?, ?, ?)

  INSERT INTO user_details (name, phone, address_id)
  VALUES (?, ?, ?)

         ▼

  Both rows created ✅
```

Even though you only called `save()` on `UserDetails`, the child `UserAddress` also got saved — because of `CascadeType.PERSIST`.

---

## 2. CascadeType.MERGE

**What it does:** When you **update** the parent entity, the child entity is also automatically updated.

### What happens if you use ONLY PERSIST and try to update?

```java
@OneToOne(cascade = CascadeType.PERSIST)   // only persist, no merge
```

```
PUT /api/user/1
Body: {
  "id": 1,
  "name": "XYZ_updated",        ← changing name
  "userAddress": {
    "id": 1,
    "city": "Bengaluru"          ← also changing city
  }
}

         ▼

  Result:
  UserDetails → name updated ✅
  UserAddress → city NOT updated ❌  (because no MERGE!)
```

PERSIST only handles inserts. For updates, you need MERGE.

### With PERSIST + MERGE together:

```java
@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

```
PUT /api/user/1   (same request as above)

         ▼

  Result:
  UserDetails → name updated ✅
  UserAddress → city updated ✅   (because MERGE is now there!)
```

---

## 3. CascadeType.REMOVE

**What it does:** When you **delete** the parent entity, the child entity is also automatically deleted.

```java
@OneToOne(cascade = {
    CascadeType.PERSIST,
    CascadeType.MERGE,
    CascadeType.REMOVE
})
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

### Flow

```
DELETE /api/user/1

         ▼

  userDetailsRepository.deleteById(1)

         ▼

  Hibernate fires TWO delete queries:

  DELETE FROM user_details WHERE id = 1
  DELETE FROM user_address WHERE id = ?   ← child also deleted!

         ▼

  Both tables empty ✅
```

Even though you only called `deleteById()` on the `UserDetails` repository, the associated `UserAddress` row also got wiped out — because of `CascadeType.REMOVE`.

---

## 4. CascadeType.REFRESH

This one is less frequently used. To understand it, you need to know about the **Persistence Context** and **First Level Caching**.

### Quick Recap of Persistence Context

```
Your Code
    │
    ▼
EntityManager  ◄──── manages ────►  Persistence Context (First Level Cache)
    │                                       │
    │                                       │  holds entity objects in memory
    ▼                                       │  during a transaction
  Database                                  │
                                    UserDetails { id=1, name="John" }
                                    UserAddress { id=1, city="Bangalore" }
```

When you call `repository.save()`, internally JPA uses an `EntityManager`. The `EntityManager` keeps entities in memory (persistence context) and uses this as a **first level cache** — meaning if you ask for the same entity twice, it doesn't hit the DB again, it just returns from memory.

### The `refresh()` method

Sometimes, you want to **bypass this cache** and directly read fresh data from the DB. For that, `EntityManager` has a `refresh()` method.

```
Without refresh:   EntityManager → reads from Persistence Context (cache) → returns data
With refresh:      EntityManager → directly hits DB → returns fresh data
```

### CascadeType.REFRESH in action

```java
@OneToOne(cascade = {CascadeType.REFRESH})
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

```
When Hibernate internally calls refresh() on UserDetails:

  WITHOUT CascadeType.REFRESH:
    UserDetails  → re-read from DB ✅
    UserAddress  → still read from cache ❌ (might be stale)

  WITH CascadeType.REFRESH:
    UserDetails  → re-read from DB ✅
    UserAddress  → also re-read from DB ✅ (fresh!)
```

In short — when the parent is refreshed from DB, the child is also refreshed from DB.

> **Note from instructor:** You'll rarely write `entityManager.refresh()` yourself in day-to-day code. But Hibernate might call it internally in certain scenarios. This cascade type makes sure the child is also covered in those cases.

---

## 5. CascadeType.DETACH

To understand this, you need to know about the **entity lifecycle**.

```
Entity Lifecycle:

  New/Transient  →  Managed  →  Detached  →  Removed
                   (inside       (outside
                  persistence   persistence
                   context)      context)
```

When an entity is **managed**, JPA is tracking it — any changes you make will automatically sync to the DB. When you **detach** it, JPA stops tracking it. Changes won't sync anymore.

`EntityManager` has a `detach()` method to remove an entity from the persistence context.

### CascadeType.DETACH in action

```java
@OneToOne(cascade = {CascadeType.DETACH})
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

```
When Hibernate internally calls detach() on UserDetails:

  WITHOUT CascadeType.DETACH:
    UserDetails  → detached from persistence context ✅
    UserAddress  → still managed by JPA ❌ (inconsistent state)

  WITH CascadeType.DETACH:
    UserDetails  → detached ✅
    UserAddress  → also detached ✅  (consistent!)
```

> **Note from instructor:** Like REFRESH, you'll rarely use DETACH yourself. Hibernate might use it internally to free up memory or manage lifecycle. This cascade type just ensures the child follows the parent.

---

## 6. CascadeType.ALL

Simple — it means **all of the above combined**.

```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

Instead of writing:

```java
cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE,
           CascadeType.REFRESH, CascadeType.DETACH}
```

You just write:

```java
cascade = CascadeType.ALL
```

Same effect. Use this when the child entity's **entire lifecycle** should mirror the parent.

---

## Full Summary — CascadeType Cheat Sheet

```
CascadeType    Triggered by     What happens to child
────────────────────────────────────────────────────────────
PERSIST        save/insert      Child also gets inserted
MERGE          update           Child also gets updated
REMOVE         delete           Child also gets deleted
REFRESH        refresh          Child also re-read from DB
DETACH         detach           Child also removed from persistence context
ALL            any operation    All of the above
```

### When to use what?

```
Use case                                Recommended cascade
──────────────────────────────────────────────────────────────
Child always created with parent        PERSIST
Child always updated with parent        MERGE
Child always deleted with parent        REMOVE
Child lifecycle fully mirrors parent    ALL
Independent child entity                (no cascade)
```

---

## ⚠️ Interview Tip

> If an interviewer asks — *"What is the difference between CascadeType.PERSIST and CascadeType.MERGE?"*

Answer: PERSIST handles the **insert** operation — saving the parent automatically saves the child. MERGE handles the **update** operation — updating the parent automatically updates the child. If you use only PERSIST and try to update the child through the parent, the child won't get updated.

> If they ask — *"What is CascadeType.ALL?"*

Answer: It combines all six cascade types — PERSIST, MERGE, REMOVE, REFRESH, and DETACH. It means any operation on the parent entity will automatically be cascaded to its associated child entity.

---

# Step 3 — Eager Loading vs Lazy Loading

---

## The Question That Leads Here

After understanding CascadeType, a natural question comes up:

> We understood INSERT, UPDATE, DELETE behavior with CascadeType. But what about **GET (read)**? When we fetch the parent entity, does the child entity always get loaded too?

The answer is — **it depends**. And that's exactly what Eager and Lazy loading controls.

---

## The Two Loading Strategies

```
Loading Strategy
      │
      ├── Eager Loading  →  Child loaded IMMEDIATELY with parent
      │
      └── Lazy Loading   →  Child loaded ONLY WHEN you explicitly access it
```

---

## 1. Eager Loading

**What it means:** Whenever you fetch the parent entity, the child entity is **automatically fetched at the same time**, without you asking for it.

Hibernate does this using a **LEFT JOIN** query:

```
SELECT ud.id, ud.name, ud.phone, ua.id, ua.city, ua.street ...
FROM user_details ud
LEFT JOIN user_address ua ON ud.address_id = ua.id
WHERE ud.id = ?

Both parent and child data fetched in ONE query ✅
```

### Default for:

```
@OneToOne    →   default is EAGER  ✅
@ManyToOne   →   default is EAGER  ✅
```

### Why is OneToOne default EAGER?

The instructor explains the reasoning behind JPA's assumption very clearly:

```
@OneToOne or @ManyToOne means...

  Parent has only ONE child
       │
       ▼
  JPA assumes:
  "Since there's only one child, the caller probably
   needs it too when fetching the parent.
   And since it's just ONE row, performance impact is minimal."
       │
       ▼
  So JPA loads it immediately (EAGER)
```

---

## 2. Lazy Loading

**What it means:** When you fetch the parent entity, the child entity is **NOT fetched immediately**. It is only fetched when your code **explicitly tries to access it**.

```
Step 1:  fetch UserDetails
         Hibernate fires:
         SELECT id, name, phone, address_id
         FROM user_details
         WHERE id = ?
         (No JOIN, no UserAddress data yet)

              ▼

Step 2:  your code calls userDetails.getUserAddress()
         NOW Hibernate fires a second query:
         SELECT id, city, street, state ...
         FROM user_address
         WHERE id = ?

              ▼

Step 3:  UserAddress data returned
```

### Default for:

```
@OneToMany   →   default is LAZY  ✅
@ManyToMany  →   default is LAZY  ✅
```

### Why is OneToMany default LAZY?

```
@OneToMany or @ManyToMany means...

  Parent can have MANY children
       │
       ▼
  JPA thinks:
  "There could be 10, 100, or 10,000 child rows.
   Loading ALL of them every time the parent is fetched
   would kill performance.
   Let me wait and load only when explicitly needed."
       │
       ▼
  So JPA loads lazily (LAZY)
```

---

## Visual Summary of Defaults

```
Annotation      Default Loading     Reason
─────────────────────────────────────────────────────────────
@OneToOne       EAGER               Only 1 child, safe to load immediately
@ManyToOne      EAGER               Only 1 child, safe to load immediately
@OneToMany      LAZY                Many children, load on demand
@ManyToMany     LAZY                Many children, load on demand
```

---

## Can We Override the Default?

**Yes!** You can control the loading behavior using the `fetch` parameter inside the annotation.

```java
// Override OneToOne from EAGER to LAZY
@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

```java
// Override OneToMany from LAZY to EAGER
@OneToMany(fetch = FetchType.EAGER)
private List<Order> orders;
```

---

## The Problem With Lazy Loading + JSON Serialization

This is a very important practical issue the instructor highlights. Let's walk through it carefully.

### Setup

```java
@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JoinColumn(name = "address_id", referencedColumnName = "id")
private UserAddress userAddress;
```

We've set `FetchType.LAZY` on `userAddress` inside `UserDetails`.

### What happens during INSERT?

```
POST /api/user  (insert operation)

  1. UserDetails + UserAddress objects created in code
  2. Both placed into Persistence Context (memory / first level cache)
  3. Both inserted into DB

  Now building the response...
  4. Jackson tries to serialize UserDetails
  5. Needs UserAddress too — but wait, is it in memory?
     YES! Both objects are still in Persistence Context
     (same EntityManager, same transaction)
  6. UserAddress fetched from Persistence Context (no DB call needed)
  7. Response built successfully ✅

INSERT works fine even with LAZY ✅
```

### What happens during GET?

```
GET /api/user/1  (read operation)

  1. NEW EntityManager created (fresh, empty Persistence Context)
  2. Hibernate fires SELECT on user_details only:
     SELECT id, name, phone, address_id FROM user_details WHERE id = 1
     (No JOIN because of LAZY — UserAddress NOT fetched)
  3. UserDetails object returned from DB

  Now building the response...
  4. Jackson tries to serialize UserDetails
  5. Encounters userAddress field
  6. Tries to serialize it — but UserAddress data is NOT in memory!
     (Persistence Context is empty, no LEFT JOIN was made)
  7. Jackson doesn't know how to serialize this empty proxy object
  8. 💥 500 Internal Server Error — Serialization failed!

GET FAILS with LAZY ❌
```

### Why didn't GET fail during INSERT?

```
INSERT                              GET
──────────────────────────────────────────────────────
Same EntityManager throughout       NEW EntityManager created
Persistence Context has both        Persistence Context is EMPTY
  UserDetails + UserAddress
Jackson finds UserAddress           Jackson finds UserAddress
  in memory ✅                        is a hollow proxy ❌
Response builds fine ✅             Serialization fails 💥
```

The key difference is the **Persistence Context**. During INSERT, both objects were already loaded in memory from the same operation. During GET, a brand new EntityManager starts fresh — and because of LAZY, UserAddress was never fetched.

---

## How to Fix This — Two Solutions

### Solution 1: @JsonIgnore

Add `@JsonIgnore` on the `userAddress` field. This tells Jackson — **"don't even try to serialize this field."**

```java
@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JoinColumn(name = "address_id", referencedColumnName = "id")
@JsonIgnore
private UserAddress userAddress;
```

```
GET /api/user/1

Response:
{
  "id": 1,
  "name": "JohnXYZ",
  "phone": "1234567890"
  // userAddress is completely gone from response
}
```

**Problem with this approach:**

```
@JsonIgnore removes the field for ALL scenarios:
  - LAZY loading  →  field removed ❌
  - EAGER loading →  field STILL removed ❌

Even if you change back to EAGER, userAddress
will never appear in the response anymore.
It's a blunt tool. ⚠️
```

---

### Solution 2: DTO (Data Transfer Object) ✅ Recommended

This is the cleaner, more professional approach. Let's first understand what a DTO is.

### What is a DTO?

Right now, your flow looks like this:

```
DB  →  Repository  →  Service  →  Controller  →  Client
              (returns Entity directly)
```

The Entity is an exact mirror of your DB table. Sending it directly to the client means:

```
Problems with returning Entity directly:

  1. Client sees ALL column names exactly as they are in DB
     (security concern — exposing internal structure)

  2. Client gets ALL fields, even ones meant for internal use only

  3. Serialization issues with lazy-loaded fields (as we just saw)
```

A DTO is a **separate plain Java class** that holds only what you want to send to the client:

```
DB  →  Repository  →  Service  →  Entity-to-DTO mapping  →  Controller  →  Client
                                    (you control exactly
                                     what goes out)
```

```
Entity (mirrors DB exactly)         DTO (what client sees)
──────────────────────────          ──────────────────────
id                          →       id
name                        →       userName   (can rename!)
phone                       →       phoneNumber
internalField1              →       (excluded!)
internalField2              →       (excluded!)
userAddress (lazy proxy)    →       address (just a String, safe!)
```

### The Code

```java
// The DTO class
public class UserDetailsDTO {

    private Long id;
    private String name;
    private String phone;
    private String address;   // just a simple String, not the full entity

    // Constructor that takes the Entity and maps it
    public UserDetailsDTO(UserDetails userDetails) {
        this.id    = userDetails.getId();
        this.name  = userDetails.getName();
        this.phone = userDetails.getPhone();

        System.out.println("going to query user address here now");

        // THIS LINE explicitly accesses userAddress
        // This triggers the lazy load — Hibernate fires the second SELECT here
        this.address = userDetails.getUserAddress() != null
                       ? userDetails.getUserAddress().getStreet()
                       : null;
    }

    // getters and setters
}
```

```java
// Inside UserDetails Entity — add a toDTO() helper method
public UserDetailsDTO toDTO() {
    return new UserDetailsDTO(this);
}
```

```java
// Controller — return DTO instead of Entity
@GetMapping("/user/{id}")
public UserDetailsDTO fetchUser(@PathVariable Long id) {
    return userDetailsService.findByID(id).toDTO();
}
```

### What happens during GET now?

```
GET /api/user/1

  1. findById(1) fires:
     SELECT id, name, phone, address_id FROM user_details WHERE id = 1
     (LAZY — no JOIN yet, UserAddress not fetched)

  2. toDTO() is called on the returned UserDetails object

  3. Inside DTO constructor:
     - id, name, phone → mapped normally
     - userDetails.getUserAddress() is called explicitly
       ↓
       NOW Hibernate fires second query:
       SELECT id, city, street... FROM user_address WHERE id = ?
       UserAddress data fetched ✅

  4. address field in DTO filled with street value

  5. DTO returned — Jackson serializes it
     (DTO has only simple fields, no proxy objects)
     No serialization issue ✅

Response:
{
  "id": 1,
  "name": "JohnXYZ",
  "phone": "1234567890",
  "address": "123 Street"   ✅
}
```

### Why is DTO the recommended approach?

```
@JsonIgnore vs DTO
──────────────────────────────────────────────────────────────
@JsonIgnore    →  Blunt. Field gone forever from response.
                  No control. Works for both LAZY and EAGER.

DTO            →  Surgical. YOU decide what to include.
                  YOU control when lazy load is triggered.
                  YOU decide what field names go out.
                  YOU decide what's hidden from client.
                  Clean separation between DB layer and API layer. ✅
```

---

## Full Picture — Lazy Loading Flow with DTO

```
GET /api/user/1
      │
      ▼
Controller calls service.findByID(1)
      │
      ▼
Repository fires:
SELECT id, name, phone, address_id FROM user_details
(UserAddress NOT fetched — LAZY)
      │
      ▼
UserDetails object returned (userAddress is a hollow proxy)
      │
      ▼
.toDTO() called
      │
      ▼
Inside DTO constructor:
  userDetails.getUserAddress() called explicitly
      │
      ▼
Hibernate fires SECOND query:
SELECT id, city, street... FROM user_address WHERE id = ?
      │
      ▼
UserAddress data now available
      │
      ▼
DTO built with all required fields
      │
      ▼
Jackson serializes DTO — no proxy, no issue ✅
      │
      ▼
Response sent to client ✅
```

---

## ⚠️ Interview Tips

> *"What is the default fetch type for @OneToOne?"*

EAGER. Because there is only one child entity, JPA assumes it will likely be needed and the performance impact is minimal.

> *"What is the default fetch type for @OneToMany?"*

LAZY. Because there can be many child rows, loading all of them every time would hurt performance.

> *"What is the difference between Eager and Lazy loading?"*

Eager loading fetches the child entity immediately along with the parent in a single LEFT JOIN query. Lazy loading fetches only the parent first, and the child is fetched with a separate query only when explicitly accessed in code.

> *"Why would a GET call fail with Lazy loading but an INSERT call succeed?"*

During INSERT, both parent and child are in the same Persistence Context (memory), so Jackson can serialize both from memory. During GET, a new EntityManager starts with an empty Persistence Context — and because of LAZY, only the parent was fetched from DB. Jackson then encounters an empty proxy for the child and fails to serialize it.

> *"What is a DTO and why is it recommended over returning the Entity directly?"*

A DTO (Data Transfer Object) is a plain Java class that holds only the fields you want to expose to the client. It decouples the DB layer from the API layer, prevents exposing internal column names, lets you control which fields are included, and cleanly handles lazy-loaded fields by explicitly triggering them inside the DTO constructor.

---
# Step 4 — @OneToOne Bidirectional

---

## Recap — What Was Missing in Unidirectional?

In unidirectional mapping, the relationship was **one-way only**:

```
UserDetails  ──────────────►  UserAddress
  (Parent)                      (Child)

  ✅ UserDetails CAN access UserAddress
  ❌ UserAddress CANNOT access UserDetails
```

What if you need to go **both ways**? That's exactly what **Bidirectional** mapping solves.

```
UserDetails  ◄──────────────►  UserAddress

  ✅ UserDetails CAN access UserAddress
  ✅ UserAddress CAN ALSO access UserDetails
```

---

## Very Important — What Changes in the Database?

This is a common confusion point, and the instructor stresses this clearly.

```
❌ WRONG assumption:
"Bidirectional means both tables have a foreign key of each other"

✅ CORRECT understanding:
"Bidirectional means both Java objects hold a reference to each other,
 but the DB table structure stays EXACTLY the same as unidirectional"
```

```
DATABASE (same as before):

USER_DETAILS table                      USER_ADDRESS table
┌────┬────────────┬──────┬────────┐     ┌────┬────────┬──────┬───────┬─────────┬──────────┐
│ ID │ ADDRESS_ID │ NAME │ PHONE  │     │ ID │ STREET │ CITY │ STATE │ COUNTRY │ PIN_CODE │
└────┴────────────┴──────┴────────┘     └────┴────────┴──────┴───────┴─────────┴──────────┘
         │                                        ▲
         └────────────────────────────────────────┘
              Only ONE foreign key (in USER_DETAILS)
              USER_ADDRESS has NO foreign key of USER_DETAILS


JAVA OBJECTS (this is what changes):

UserDetails object                  UserAddress object
┌─────────────────────┐             ┌──────────────────────┐
│ id                  │             │ id                   │
│ name                │    ◄─────   │ street               │
│ phone               │             │ city                 │
│ userAddress ────────┼─────────►   │ state                │
└─────────────────────┘             │ country              │
                                    │ pinCode              │
                                    │ userDetails  ────────┼──► (back reference)
                                    └──────────────────────┘
```

The backward reference exists **only in the Java object**, not in the DB.

---

## Two Important Terms

### Owner Side
- The entity that **holds the foreign key** in the database
- In our case: `UserDetails` (it has `address_id` FK)
- Uses `@JoinColumn`

### Inverse Side
- The entity that does **NOT hold the foreign key** in the DB
- In our case: `UserAddress`
- Uses `mappedBy` — which tells JPA "don't create a new FK, just map back to the existing one"

```
Owner Side (UserDetails)          Inverse Side (UserAddress)
────────────────────────          ──────────────────────────
Holds FK in DB                    No FK in DB
Uses @JoinColumn                  Uses mappedBy
Is the "source of truth"          Just a back-reference
  for the relationship              in Java object only
```

---

## The Code

### Owner Side — UserDetails (unchanged from unidirectional)

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private UserAddress userAddress;   // FK lives here in DB

    // constructors, getters, setters
}
```

### Inverse Side — UserAddress (this is what changes)

```java
@Entity
@Table(name = "user_address")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    // THIS IS NEW — back reference to UserDetails
    @OneToOne(mappedBy = "userAddress", fetch = FetchType.EAGER)
    private UserDetails userDetails;

    // getters, setters
}
```

### What does `mappedBy = "userAddress"` mean?

```
mappedBy = "userAddress"
                │
                └──► "The relationship is already mapped by
                      the field named 'userAddress'
                      inside the UserDetails class.
                      Don't create a new FK for me.
                      I'm just a back-reference in Java."
```

```
Without mappedBy:
  JPA would think "Oh, UserAddress also has a @OneToOne,
  let me create ANOTHER FK column in user_address table" ❌

With mappedBy:
  JPA understands "This is just the inverse side.
  The FK already exists in user_details table.
  No new column needed." ✅
```

---

## Testing Bidirectional — The Problem That Appears

Now let's say we create a controller for `UserAddress` to test the backward navigation:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserAddressController {

    @Autowired
    UserAddressService userAddressService;

    @GetMapping("/user-address/{id}")
    public UserAddress fetchUser(@PathVariable Long id) {
        return userAddressService.findByID(id);
    }
}
```

### Step 1 — Insert (works fine)

```
POST /api/user
Body: { name, phone, userAddress: { street, city, state, country, pinCode } }

Response:
{
  "id": 1,
  "name": "JohnXYZ",
  "phone": "1234567890",
  "userAddress": {
    "id": 1,
    "street": "123 Street",
    "city": "Bangalore",
    ...
    "userDetails": null    ← null here, that's fine for now
  }
}
```

Hibernate fires two INSERT queries — all good.

### Step 2 — GET on UserAddress (💥 Problem!)

```
GET /api/user-address/1

Expected response:
{
  "id": 1,
  "street": "123 Street",
  "city": "Bangalore",
  ...
  "userDetails": { id, name, phone }   ← backward navigation
}

Actual result:
💥 ERROR — Infinite Recursion / Stack Overflow
```

---

## Why Does Infinite Recursion Happen?

Let's trace exactly what Jackson does during serialization:

```
GET /api/user-address/1

Hibernate fires:
SELECT ua.*, ud.*
FROM user_address ua
LEFT JOIN user_details ud ON ud.address_id = ua.id
WHERE ua.id = 1

(Query is correct ✅ — data fetched properly)

Now Jackson starts building the JSON response...

Step 1: Start serializing UserAddress
        → id, street, city, state, country, pinCode  ✅
        → encounters "userDetails" field...

Step 2: Start serializing UserDetails
        → id, name, phone  ✅
        → encounters "userAddress" field...

Step 3: Start serializing UserAddress AGAIN
        → id, street, city ...
        → encounters "userDetails" field AGAIN...

Step 4: Start serializing UserDetails AGAIN
        → ...
        → encounters "userAddress" AGAIN...

Step 5: 🔄 This goes on FOREVER
        💥 Stack Overflow / Infinite Recursion Error
```

Visually:

```
UserAddress
    └──► UserDetails
              └──► UserAddress
                       └──► UserDetails
                                 └──► UserAddress
                                           └──► ... 💥
```

This is a **classic problem with bidirectional mapping** — both sides keep pointing to each other and Jackson has no idea when to stop.

---

## Three Ways to Solve Infinite Recursion

---

### Solution 1: @JsonManagedReference + @JsonBackReference

This is the most straightforward fix. You explicitly tell Jackson which direction to serialize and which to stop at.

```
@JsonManagedReference  →  "Go ahead, serialize this child" (used on OWNER/PARENT side)
@JsonBackReference     →  "Stop here, don't serialize back" (used on INVERSE/CHILD side)
```

```java
// Owner side — UserDetails
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id", referencedColumnName = "id")
@JsonManagedReference    // ← "serialize forward"
private UserAddress userAddress;
```

```java
// Inverse side — UserAddress
@OneToOne(mappedBy = "userAddress", fetch = FetchType.EAGER)
@JsonBackReference       // ← "stop here, don't serialize back"
private UserDetails userDetails;
```

### What happens now?

```
GET /api/user/1  (fetching from PARENT side)

Response:
{
  "id": 1,
  "name": "JohnXYZ",
  "phone": "1234567890",
  "userAddress": {          ← @JsonManagedReference allows this ✅
    "id": 1,
    "street": "123 Street",
    "city": "Bangalore"
    // userDetails field is NOT here ✅ (@JsonBackReference blocked it)
  }
}
```

```
GET /api/user-address/1  (fetching from CHILD side)

Response:
{
  "id": 1,
  "street": "123 Street",
  "city": "Bangalore",
  "state": "Karnataka",
  "country": "India",
  "pinCode": "10001"
  // userDetails is completely absent ✅ (@JsonBackReference removed it)
}
```

### Limitation of this approach

```
When you fetch from child (UserAddress):
  ❌ userDetails is completely missing from the response
  You cannot see the parent data from the child side at all
```

If that's acceptable for your use case — great! But what if you **want** to see the parent data when fetching from the child, **without** infinite recursion? That's where Solution 3 comes in. First, let's see Solution 2.

---

### Solution 2: @JsonIgnore (on the back-reference)

Simply put `@JsonIgnore` on the `userDetails` field inside `UserAddress`:

```java
// Inverse side — UserAddress
@OneToOne(mappedBy = "userAddress", fetch = FetchType.EAGER)
@JsonIgnore
private UserDetails userDetails;
```

This works the same as `@JsonBackReference` — the `userDetails` field is simply excluded from the JSON response. Same limitation applies — you won't see parent data when fetching from child.

---

### Solution 3: @JsonIdentityInfo ✅ Most Powerful

This solution is different from the previous two. Instead of **blocking** serialization of one side, it **tracks** what has already been serialized using a unique identifier.

```
@JsonIdentityInfo approach:

  Jackson assigns a unique ID to each object during serialization.
  When Jackson encounters the same object again,
  instead of serializing it fully again (causing recursion),
  it just outputs the unique ID as a reference.

  First time seen  →  serialize fully
  Seen again       →  just output the ID (no recursion!)
```

### The Code

```java
// UserDetails — add @JsonIdentityInfo at class level
@Table(name = "user_details")
@Entity
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"   // ← use the "id" field as the unique identifier
)
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private UserAddress userAddress;

    // constructors, getters, setters
}
```

```java
// UserAddress — also add @JsonIdentityInfo at class level
@Entity
@Table(name = "user_address")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id"   // ← use the "id" field as the unique identifier
)
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    @OneToOne(mappedBy = "userAddress", fetch = FetchType.EAGER)
    private UserDetails userDetails;   // no @JsonBackReference needed!

    // getters, setters
}
```

### What happens now?

```
GET /api/user/1  (fetching from PARENT side)

Jackson serializes UserDetails:
  → id=1, name, phone  ✅
  → encounters userAddress → serialize it fully:
      id=1, street, city, state, country, pinCode  ✅
      → encounters userDetails inside UserAddress...
        Jackson checks: "Have I seen UserDetails with id=1 before?"
        YES! Already serialized.
        So instead of full object, just output: "userDetails": 1

Response:
{
  "id": 1,
  "name": "JohnXYZ",
  "phone": "1234567890",
  "userAddress": {
    "id": 1,
    "street": "123 Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "India",
    "pinCode": "10001",
    "userDetails": 1        ← just the ID, no recursion! ✅
  }
}
```

```
GET /api/user-address/1  (fetching from CHILD side)

Jackson serializes UserAddress:
  → id=1, street, city, state, country, pinCode  ✅
  → encounters userDetails → serialize it fully:
      id=1, name, phone  ✅
      → encounters userAddress inside UserDetails...
        Jackson checks: "Have I seen UserAddress with id=1 before?"
        YES! Already serialized.
        So instead of full object, just output: "userAddress": 1

Response:
{
  "id": 1,
  "street": "123 Street",
  "city": "Bangalore",
  "state": "Karnataka",
  "country": "India",
  "pinCode": "10001",
  "userDetails": {
    "id": 1,
    "name": "JohnXYZ",
    "phone": "1234567890",
    "userAddress": 1        ← just the ID, no recursion! ✅
  }
}
```

Now you get **data from both sides** without any infinite recursion!

---

## Comparing All Three Solutions

```
Solution               How it works                    Can see both sides?
──────────────────────────────────────────────────────────────────────────
@JsonManagedReference  Allows forward serialization     ❌ Child side loses
+ @JsonBackReference   Blocks backward serialization       parent data

@JsonIgnore            Simply ignores the field          ❌ Same limitation
                       on the inverse side

@JsonIdentityInfo      Tracks serialized objects         ✅ Both sides show
                       by unique ID, replaces               data! Back-ref
                       repeated objects with ID             shows as just ID
```

---

## Complete Flow Diagram — Bidirectional

```
                        DATABASE
                    ┌─────────────────────────────────────┐
                    │                                     │
         USER_DETAILS table          USER_ADDRESS table   │
         ┌───┬────────────┬──────┐   ┌───┬────────┬─────┐ │
         │ 1 │ address_id │ John │   │ 1 │ 123 St │ BLR │ │
         └───┴────────────┴──────┘   └───┴────────┴─────┘ │
                  │ FK                       ▲            │
                  └───────────────────────────            │
                    └─────────────────────────────────────┘

                        JAVA OBJECTS
              ┌─────────────────────┐
              │   UserDetails       │
              │   id = 1            │◄──────────────────┐
              │   name = "John"     │                   │
              │   userAddress ──────┼──────────┐        │
              └─────────────────────┘          │        │
                                               ▼        │
                                   ┌───────────────────────┐
                                   │   UserAddress         │
                                   │   id = 1              │
                                   │   street = "123 St"   │
                                   │   city = "Bangalore"  │
                                   │   userDetails ────────┘
                                   └───────────────────────┘

  ✅ Navigate forward:  userDetails.getUserAddress()
  ✅ Navigate backward: userAddress.getUserDetails()
  ✅ Only ONE FK in DB (inside user_details table)
```

---

## ⚠️ Interview Tips

> *"What is the difference between unidirectional and bidirectional @OneToOne mapping?"*

In unidirectional, only the parent holds a reference to the child — navigation is one-way only. In bidirectional, both entities hold references to each other, so you can navigate in both directions. Importantly, the DB table structure is the same in both cases — only one foreign key exists, in the owner/parent entity. The backward reference in bidirectional exists only in the Java object.

> *"What is mappedBy in @OneToOne bidirectional?"*

`mappedBy` is used on the inverse side (child entity) to tell JPA that this relationship is already mapped by a field in the owner entity. It prevents JPA from creating an additional foreign key column in the child's table. The value of `mappedBy` is the field name in the owner entity that owns the relationship.

> *"What is infinite recursion in bidirectional mapping and how do you solve it?"*

When both entities reference each other and you try to serialize them to JSON, Jackson keeps going back and forth between the two objects indefinitely — causing a Stack Overflow. There are three ways to solve it: `@JsonManagedReference` + `@JsonBackReference` (stops backward serialization), `@JsonIgnore` (ignores the back-reference field), or `@JsonIdentityInfo` (tracks serialized objects by unique ID and replaces repeated references with just the ID — this is the most powerful as it allows data from both sides).

> *"What is the owner side and inverse side in bidirectional mapping?"*

The owner side is the entity that holds the foreign key in the database table, and it uses `@JoinColumn`. The inverse side does not have a foreign key in the DB and uses `mappedBy` to point back to the owner. The owner side is the "source of truth" for the relationship.

---

## Full Lecture Summary

```
Topic                    Key Points
────────────────────────────────────────────────────────────────────────
@OneToOne Unidirectional  One FK in parent table, navigation parent→child only
@JoinColumn               Custom FK name and referenced column
@JoinColumns              For composite keys — must mention all columns explicitly
CascadeType               Controls what operations flow from parent to child
  PERSIST                 Insert parent → child also inserted
  MERGE                   Update parent → child also updated
  REMOVE                  Delete parent → child also deleted
  REFRESH                 Refresh parent → child also refreshed from DB
  DETACH                  Detach parent → child also detached
  ALL                     All of the above
Eager Loading             Child loaded immediately with parent (default: @OneToOne)
Lazy Loading              Child loaded only when explicitly accessed (default: @OneToMany)
Lazy + Serialization      GET fails because Jackson can't serialize empty proxy
Fix 1: @JsonIgnore        Removes field from response entirely
Fix 2: DTO                Clean mapping — you control what gets exposed and when
@OneToOne Bidirectional   Both objects reference each other, DB structure unchanged
mappedBy                  Marks inverse side, prevents duplicate FK creation
Infinite Recursion        Jackson loops endlessly between two referencing objects
Fix 1: Managed+Back Ref   Blocks backward serialization
Fix 2: @JsonIgnore        Ignores back-reference field
Fix 3: @JsonIdentityInfo  Tracks by unique ID, prevents re-serialization
```

---

That completes the full lecture on **JPA @OneToOne Mapping — Unidirectional and Bidirectional**! 🎉

The next lecture will cover **One-to-Many, Many-to-One, and Many-to-Many** mappings. Whenever you're ready for that, just let me know!