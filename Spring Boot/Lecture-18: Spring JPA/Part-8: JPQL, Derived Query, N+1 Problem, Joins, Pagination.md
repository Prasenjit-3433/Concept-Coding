# Step 1 — Why Do We Need Custom Queries?

## The Problem First

So far, our repository interface looks like this — completely empty:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

}
```

And in the service class, we just call the built-in JPA methods:

```java
public UserDetails saveUser(UserDetails user) {
    return userDetailsRepository.save(user);       // insert / update
}

public UserDetails findByID(Long primaryKey) {
    return userDetailsRepository.findById(primaryKey).get();  // fetch by PK
}
```

These built-in methods (`save`, `findById`, `deleteById`, etc.) are already implemented inside the JPA framework — we don't write any implementation, yet they work.

**But here's the problem:**

```
What if your requirement is more complex than just "find by ID"?

Example:
  - Find all users whose name starts with "A" AND phone is "12312"
  - Delete all users with a specific name
  - Find users with a JOIN across two tables
  - Search using LIKE, IN, BETWEEN, etc.

The built-in methods are NOT enough for this.
```

So JPA gives us three ways to write custom queries. This lecture covers the first two:

```
CUSTOM QUERY OPTIONS
====================

1. Derived Query      → write a method name, JPA auto-generates the SQL
                        (simple to moderate queries)

2. JPQL               → write your own query, but using Entity class
                        names instead of table names
                        (moderate to complex queries)

3. Native Query  }
   Criteria API  }    → covered in next part
   Specification }
```

---

## Derived Query — The Idea

The core idea is simple:

> You write a **method name** following a specific naming convention.
> JPA reads (parses) that method name and automatically builds the SQL query for you.
> You write zero SQL.

```
YOUR METHOD NAME
================
findUserDetailsByName(String userName)
        |
        | JPA parses this
        v
SELECT user_id, user_name, phone
FROM   user_details
WHERE  user_name = ?
        |
        | ? gets replaced with the value of userName
        v
      result returned
```

You don't write the query. JPA does it internally. That's why it's called a **derived** query — the query is *derived from* the method name.

---

## The Naming Convention (this is mandatory)

JPA uses a regex inside a class called `PartTree.java` to parse your method name. Your method **must** follow this pattern — otherwise JPA can't parse it and will throw an error.

```
METHOD NAME STRUCTURE
=====================

[PREFIX] + [Optional Description] + "By" + [Field Name(s)] + [Comparison Keywords]

   (1)           (2)                 (3)         (4)                  (5)
```

Let's break each part down:

### Part 1 — Prefix (method must START with one of these)

```
┌─────────────────────────────────────────────────────────┐
│  ALLOWED PREFIXES                                       │
├──────────────┬──────────────────────────────────────────┤
│  find        │  → SELECT query                          │
│  read        │  → SELECT query                          │
│  get         │  → SELECT query                          │
│  query       │  → SELECT query                          │
│  search      │  → SELECT query                          │
│  stream      │  → SELECT query (as stream)              │
│  count       │  → COUNT query                           │
│  exists      │  → EXISTS check                          │
│  delete      │  → DELETE query                          │
│  remove      │  → DELETE query                          │
└──────────────┴──────────────────────────────────────────┘
```

### Part 2 — Optional description (zero or more characters, starts with uppercase)

```
findUserDetails...
     ^^^^^^^^^^
     This part is optional. You can write anything here
     starting with an uppercase letter.
     JPA ignores this part during parsing — it's just
     for readability.
```

### Part 3 — "By" keyword

```
findUserDetailsBy...
               ^^
               This is the separator. Everything AFTER
               "By" becomes your WHERE clause.
```

### Part 4 — Field name(s) from your Entity class

```java
// Your Entity:
@Entity
public class UserDetails {
    private Long userId;
    private String name;    // <-- field name
    private String phone;   // <-- field name
}

// Your method:
findUserDetailsByName(String userName)
//               ^^^^
//               This must match the ENTITY field name (camelCase)
//               NOT the DB column name
```

### Part 5 — Comparison keywords (optional, goes after field name)

```
┌──────────────────────────────────────────────────────────┐
│  COMPARISON KEYWORDS (from Part.java enum)               │
├──────────────────────────┬───────────────────────────────┤
│  IsIn / In               │  WHERE name IN (list)         │
│  IsLike / Like           │  WHERE name LIKE ?            │
│  IsBetween / Between     │  WHERE x BETWEEN a AND b      │
│  IsNull / Null           │  WHERE x IS NULL              │
│  IsNotNull / NotNull     │  WHERE x IS NOT NULL          │
│  IsLessThan              │  WHERE x < ?                  │
│  IsGreaterThan           │  WHERE x > ?                  │
│  StartingWith            │  WHERE name LIKE 'val%'       │
│  EndingWith              │  WHERE name LIKE '%val'       │
│  Containing              │  WHERE name LIKE '%val%'      │
└──────────────────────────┴───────────────────────────────┘
```

---

## Use Cases with Examples

### Use Case 1 — Simple find by one field

```java
List<UserDetails> findUserDetailsByName(String userName);

// Generated SQL:
// SELECT user_id, user_name, phone
// FROM   user_details
// WHERE  user_name = ?
```

### Use Case 2 — AND condition

```java
List<UserDetails> findUserDetailsByNameAndPhone(String userName, String phone);

// Generated SQL:
// SELECT user_id, user_name, phone
// FROM   user_details
// WHERE  user_name = ? AND phone = ?
```

### Use Case 3 — OR condition

```java
List<UserDetails> findUserDetailsByNameAndPhoneOrUserId(
    String userName, String phone, Long id);

// Generated SQL:
// WHERE  user_name = ? AND phone = ? OR user_id = ?
```

### Use Case 4 — IN keyword

```java
List<UserDetails> findUserDetailsByNameIsIn(List<String> userName);

// Generated SQL:
// WHERE  user_name IN (?)
//
// Note: parameter becomes a List because IN takes multiple values
```

### Use Case 5 — LIKE keyword

```java
List<UserDetails> findUserDetailsByNameLike(String userName);

// Generated SQL:
// WHERE  user_name LIKE ? escape '\'
```

### Use Case 6 — DELETE (needs @Transactional)

```java
@Transactional
void deleteByName(String userName);
```

```
WHY @Transactional for delete?
==============================
Because we are MODIFYING the database.
Any operation that changes data (insert, update, delete)
must run inside a transaction.

Also note what JPA does internally for deleteByName:
  Step 1 → SELECT all matching rows (to load them into persistence context)
  Step 2 → DELETE each one by its ID (one delete per row)

So if 10 rows match the name "A", it runs:
  1 SELECT + 10 DELETEs = 11 queries total.
```

---

## Important Limitation of Derived Query

```
┌─────────────────────────────────────────────────────────┐
│  DERIVED QUERY: WHEN TO USE vs WHEN NOT TO USE          │
├─────────────────────────────┬───────────────────────────┤
│  Good for                   │  Not good for             │
├─────────────────────────────┼───────────────────────────┤
│  Simple WHERE conditions    │  Complex JOINs            │
│  AND / OR / IN / LIKE       │  Aggregations             │
│  StartingWith, Between etc. │  Subqueries               │
│  Delete by field            │  Very long method names   │
└─────────────────────────────┴───────────────────────────┘

As queries get complex, the method name becomes unreadable.
That's when you move to JPQL (covered in Step 3).
```

---
# Step 2 — Pagination & Sorting in Derived Query

## The Problem First

Imagine your `user_details` table has **10,000 rows**. If you call `findUserDetailsByName("A")`, JPA fetches ALL matching rows at once and dumps them into memory. That's a serious performance problem in real applications.

The solution is **pagination** — fetch only a small chunk at a time (like how Google shows 10 results per page, not all billion results at once).

And along with that, you often want results in a specific **order** — newest first, alphabetically, etc. That's **sorting**.

JPA gives you two interfaces for this:

```
JPA INTERFACES FOR PAGINATION & SORTING
========================================

                    Spring Data JPA
                         |
          ┌──────────────┴──────────────┐
          │                             │
       Pageable                        Sort
  (org.springframework              (org.springframework
   .data.domain)                     .data.domain)
          │                             │
    ┌─────┴──────┐                controls order
    │            │                (ASC / DESC)
pageNumber    pageSize
(which page)  (how many
               per page)
```

---

## Part A — Pagination with Pageable

### How pagination works conceptually

```
EXAMPLE: 7 records total, pageSize = 5
=======================================

DB Records:  [1] [2] [3] [4] [5] [6] [7]
              |_________________|  |_____|
                   Page 0           Page 1
               (5 records)        (2 records)

Page numbers start from 0 (zero-indexed).
```

### Step 1 — Add Pageable to your repository method

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    List<UserDetails> findUserDetailsByNameStartingWith(
        String userName,
        Pageable page      // <-- just add this parameter
    );
}
```

No change in the method name logic. The `Pageable` parameter is recognized by JPA automatically — it's not treated as part of the WHERE clause.

```
CONVENTION NOTE:
================
Put the Pageable parameter at the END of the method.
There's no strict rule forcing this, JPA can handle it
in the middle too — but keeping it last is the standard
convention everyone follows.
```

### Step 2 — Create a PageRequest in your service class

```java
public List<UserDetails> findByNameDerived(String name) {

    Pageable pageable = PageRequest.of(0, 5);
    //                               ^  ^
    //                               |  └── pageSize: 5 records per page
    //                               └───── pageNumber: fetch page 0 (first page)

    return userDetailsRepository.findUserDetailsByNameStartingWith(name, pageable);
}
```

### What SQL gets generated

```
Generated SQL (Hibernate):
==========================
SELECT   user_id, user_name, phone
FROM     user_details
WHERE    user_name LIKE ? escape '\'
OFFSET   ? rows          ← skip the first N rows (based on page number)
FETCH    first ? rows only  ← take only pageSize rows
```

---

## Part B — Getting More Info with Page\<T\> return type

When return type is `List`, you only get the records. But sometimes you also want to know:

- How many total pages are there?
- Is this the first page?
- Is this the last page?

For that, change the return type from `List<UserDetails>` to `Page<UserDetails>`:

```java
// Repository:
Page<UserDetails> findUserDetailsByNameStartingWith(String userName, Pageable page);
```

```java
// Service:
public List<UserDetails> findByNameDerived(String name) {

    Pageable pageable = PageRequest.of(0, 5);

    Page<UserDetails> userDetailsPage =
        userDetailsRepository.findUserDetailsByNameStartingWith(name, pageable);

    // Extract the actual records
    List<UserDetails> userDetailsList = userDetailsPage.getContent();

    // Extra info available from Page object
    System.out.println("total pages : " + userDetailsPage.getTotalPages());
    System.out.println("is first page: " + userDetailsPage.isFirst());
    System.out.println("is last page : " + userDetailsPage.isLast());

    return userDetailsList;
}
```

```
Page<T> vs List<T>
===================
┌────────────────────┬──────────────────────────────────────┐
│  List<T>           │  Page<T>                             │
├────────────────────┼──────────────────────────────────────┤
│  Just the records  │  Records + metadata                  │
│                    │  → getTotalPages()                   │
│                    │  → getTotalElements()                │
│                    │  → isFirst()                         │
│                    │  → isLast()                          │
│                    │  → getContent() → gives you the List │
└────────────────────┴──────────────────────────────────────┘
Use Page<T> when you need to show pagination info to the user.
Use List<T> when you only care about the data itself.
```

---

## Part C — Sorting Only (no pagination)

If you just want sorted results without pagination, use the `Sort` interface instead of `Pageable`:

```java
// Repository:
List<UserDetails> findUserDetailsByNameStartingWith(String userName, Sort sort);

// Service:
return userDetailsRepository.findUserDetailsByNameStartingWith(
    name,
    Sort.by("name").descending()
    //         ^^^^
    //         This is the ENTITY field name, not DB column name
);
```

---

## Part D — Pagination WITH Sorting combined

You can combine both by passing a `Sort` into `PageRequest.of()`:

```java
Pageable pageable = PageRequest.of(0, 5, Sort.by("name").descending());
//                               ^  ^    ^
//                               |  |    └── sort by entity field "name", descending
//                               |  └─────── 5 records per page
//                               └────────── page 0
```

---

## Part E — Sorting by Multiple Fields

Sometimes one field has duplicates, so you need a second field as a tiebreaker.

```
EXAMPLE:
========
DB has 3 users, all with name = "A"
Phone numbers are: 1, 2, 3

Sort by name DESC → all three have same name, so order is unclear.
Add phone DESC as tiebreaker → A/3, A/2, A/1  ✓
```

### Option 1 — Same direction for all fields

```java
Sort.by("name", "phone").ascending()
// First sorts by name. If duplicates found, uses phone as tiebreaker.
// Both fields go in the SAME direction (both ASC here).
```

### Option 2 — Different direction per field

```java
Sort sort = Sort.by(
    Sort.Order.asc("name"),    // name → ascending
    Sort.Order.desc("phone")   // phone → descending
);

return userDetailsRepository.findUserDetailsByNameStartingWith(name, sort);
```

```
MULTI-FIELD SORTING RULE:
==========================
Sorting is applied LEFT TO RIGHT.

Sort.by("name", "phone").ascending()

Step 1: Sort all records by "name" ascending.
Step 2: Among records with the SAME name,
        sort those by "phone" ascending.
Step 3: If you had a 3rd field, it would be used
        for remaining ties, and so on.
```

---

## Full Picture — All combinations at a glance

```
┌──────────────────────┬────────────────────────────────────────────────┐
│  What you want       │  How to do it                                  │
├──────────────────────┼────────────────────────────────────────────────┤
│  Pagination only     │  Pass Pageable (PageRequest.of(page, size))    │
│  Sorting only        │  Pass Sort (Sort.by("field").ascending())      │
│  Pagination+Sorting  │  PageRequest.of(page, size, Sort.by(...))      │
│  Multi-field sort    │  Sort.by("f1","f2") or Sort.Order.asc/desc     │
│  Extra page info     │  Use Page<T> as return type instead of List<T> │
└──────────────────────┴────────────────────────────────────────────────┘
```

---
# Step 2 — Pagination & Sorting in Derived Query

## The Problem First

Imagine your `user_details` table has **10,000 rows**. If you call `findUserDetailsByName("A")`, JPA fetches ALL matching rows at once and dumps them into memory. That's a serious performance problem in real applications.

The solution is **pagination** — fetch only a small chunk at a time (like how Google shows 10 results per page, not all billion results at once).

And along with that, you often want results in a specific **order** — newest first, alphabetically, etc. That's **sorting**.

JPA gives you two interfaces for this:

```
JPA INTERFACES FOR PAGINATION & SORTING
========================================

                    Spring Data JPA
                         |
          ┌──────────────┴──────────────┐
          │                             │
       Pageable                        Sort
  (org.springframework              (org.springframework
   .data.domain)                     .data.domain)
          │                             │
    ┌─────┴──────┐                controls order
    │            │                (ASC / DESC)
pageNumber    pageSize
(which page)  (how many
               per page)
```

---

## Part A — Pagination with Pageable

### How pagination works conceptually

```
EXAMPLE: 7 records total, pageSize = 5
=======================================

DB Records:  [1] [2] [3] [4] [5] [6] [7]
              |_________________|  |_____|
                   Page 0           Page 1
               (5 records)        (2 records)

Page numbers start from 0 (zero-indexed).
```

### Step 1 — Add Pageable to your repository method

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    List<UserDetails> findUserDetailsByNameStartingWith(
        String userName,
        Pageable page      // <-- just add this parameter
    );
}
```

No change in the method name logic. The `Pageable` parameter is recognized by JPA automatically — it's not treated as part of the WHERE clause.

```
CONVENTION NOTE:
================
Put the Pageable parameter at the END of the method.
There's no strict rule forcing this, JPA can handle it
in the middle too — but keeping it last is the standard
convention everyone follows.
```

### Step 2 — Create a PageRequest in your service class

```java
public List<UserDetails> findByNameDerived(String name) {

    Pageable pageable = PageRequest.of(0, 5);
    //                               ^  ^
    //                               |  └── pageSize: 5 records per page
    //                               └───── pageNumber: fetch page 0 (first page)

    return userDetailsRepository.findUserDetailsByNameStartingWith(name, pageable);
}
```

### What SQL gets generated

```
Generated SQL (Hibernate):
==========================
SELECT   user_id, user_name, phone
FROM     user_details
WHERE    user_name LIKE ? escape '\'
OFFSET   ? rows          ← skip the first N rows (based on page number)
FETCH    first ? rows only  ← take only pageSize rows
```

---

## Part B — Getting More Info with Page\<T\> return type

When return type is `List`, you only get the records. But sometimes you also want to know:

- How many total pages are there?
- Is this the first page?
- Is this the last page?

For that, change the return type from `List<UserDetails>` to `Page<UserDetails>`:

```java
// Repository:
Page<UserDetails> findUserDetailsByNameStartingWith(String userName, Pageable page);
```

```java
// Service:
public List<UserDetails> findByNameDerived(String name) {

    Pageable pageable = PageRequest.of(0, 5);

    Page<UserDetails> userDetailsPage =
        userDetailsRepository.findUserDetailsByNameStartingWith(name, pageable);

    // Extract the actual records
    List<UserDetails> userDetailsList = userDetailsPage.getContent();

    // Extra info available from Page object
    System.out.println("total pages : " + userDetailsPage.getTotalPages());
    System.out.println("is first page: " + userDetailsPage.isFirst());
    System.out.println("is last page : " + userDetailsPage.isLast());

    return userDetailsList;
}
```

```
Page<T> vs List<T>
===================
┌────────────────────┬──────────────────────────────────────┐
│  List<T>           │  Page<T>                             │
├────────────────────┼──────────────────────────────────────┤
│  Just the records  │  Records + metadata                  │
│                    │  → getTotalPages()                   │
│                    │  → getTotalElements()                │
│                    │  → isFirst()                         │
│                    │  → isLast()                          │
│                    │  → getContent() → gives you the List │
└────────────────────┴──────────────────────────────────────┘
Use Page<T> when you need to show pagination info to the user.
Use List<T> when you only care about the data itself.
```

---

## Part C — Sorting Only (no pagination)

If you just want sorted results without pagination, use the `Sort` interface instead of `Pageable`:

```java
// Repository:
List<UserDetails> findUserDetailsByNameStartingWith(String userName, Sort sort);

// Service:
return userDetailsRepository.findUserDetailsByNameStartingWith(
    name,
    Sort.by("name").descending()
    //         ^^^^
    //         This is the ENTITY field name, not DB column name
);
```

---

## Part D — Pagination WITH Sorting combined

You can combine both by passing a `Sort` into `PageRequest.of()`:

```java
Pageable pageable = PageRequest.of(0, 5, Sort.by("name").descending());
//                               ^  ^    ^
//                               |  |    └── sort by entity field "name", descending
//                               |  └─────── 5 records per page
//                               └────────── page 0
```

---

## Part E — Sorting by Multiple Fields

Sometimes one field has duplicates, so you need a second field as a tiebreaker.

```
EXAMPLE:
========
DB has 3 users, all with name = "A"
Phone numbers are: 1, 2, 3

Sort by name DESC → all three have same name, so order is unclear.
Add phone DESC as tiebreaker → A/3, A/2, A/1  ✓
```

### Option 1 — Same direction for all fields

```java
Sort.by("name", "phone").ascending()
// First sorts by name. If duplicates found, uses phone as tiebreaker.
// Both fields go in the SAME direction (both ASC here).
```

### Option 2 — Different direction per field

```java
Sort sort = Sort.by(
    Sort.Order.asc("name"),    // name → ascending
    Sort.Order.desc("phone")   // phone → descending
);

return userDetailsRepository.findUserDetailsByNameStartingWith(name, sort);
```

```
MULTI-FIELD SORTING RULE:
==========================
Sorting is applied LEFT TO RIGHT.

Sort.by("name", "phone").ascending()

Step 1: Sort all records by "name" ascending.
Step 2: Among records with the SAME name,
        sort those by "phone" ascending.
Step 3: If you had a 3rd field, it would be used
        for remaining ties, and so on.
```

---

## Full Picture — All combinations at a glance

```
┌──────────────────────┬────────────────────────────────────────────────┐
│  What you want       │  How to do it                                  │
├──────────────────────┼────────────────────────────────────────────────┤
│  Pagination only     │  Pass Pageable (PageRequest.of(page, size))    │
│  Sorting only        │  Pass Sort (Sort.by("field").ascending())      │
│  Pagination+Sorting  │  PageRequest.of(page, size, Sort.by(...))      │
│  Multi-field sort    │  Sort.by("f1","f2") or Sort.Order.asc/desc     │
│  Extra page info     │  Use Page<T> as return type instead of List<T> │
└──────────────────────┴────────────────────────────────────────────────┘
```

---
# Step 3 — JPQL (Java Persistence Query Language)

## The Problem First

Derived queries are convenient, but they have a hard limit — complex queries become either impossible or the method name turns into an unreadable mess:

```
findUserDetailsByNameStartingWithAndPhoneIsNotNullAndUserIdGreaterThan(...)

Too long. Too confusing. Hard to maintain.
And JOINs across tables? Derived query simply can't handle that well.
```

So JPA gives us JPQL — where **you write the query yourself**, but instead of writing against the database directly, you write against your **Java entity classes**.

---

## What is JPQL?

```
SQL  →  works on TABLE names and COLUMN names  →  database dependent
JPQL →  works on ENTITY names and FIELD names  →  database independent
```

```
JPQL vs SQL
============

SQL:
  SELECT user_id, user_name
  FROM   user_details          ← table name
  WHERE  user_name = 'John'    ← column name

JPQL:
  SELECT u
  FROM   UserDetails u         ← entity CLASS name
  WHERE  u.name = :userName    ← entity FIELD name
```

Because JPQL works on your Java class — not the database directly — you can switch from MySQL to PostgreSQL to Oracle tomorrow and your JPQL queries stay exactly the same. JPA handles the translation to actual SQL internally.

---

## Basic JPQL Syntax

You write JPQL inside the `@Query` annotation on top of your repository method:

```java
@Query("SELECT u FROM UserDetails u WHERE u.name = :userFirstName")
List<UserDetails> findByUserName(@Param("userFirstName") String userName);
```

Let's break every piece of this down:

```
ANATOMY OF A JPQL QUERY
========================

@Query("SELECT u FROM UserDetails u WHERE u.name = :userFirstName")
          |         |             |         |           |
          |         |             |         |           └── named parameter
          |         |             |         |               (binds to @Param)
          |         |             |         |
          |         |             |         └── entity FIELD name
          |         |             |             (not DB column name)
          |         |             |
          |         |             └── alias for entity
          |         |                 (like table alias in SQL)
          |         |
          |         └── entity CLASS name
          |             (not DB table name)
          |
          └── selecting all fields of UserDetails
              (u = the whole object)


@Param("userFirstName")
   |
   └── binds the method parameter "userName"
       to the named parameter ":userFirstName" in the query
       (the name inside @Param must match the :name in @Query)
```

### Your Entity for reference

```java
@Table(name = "user_details")   // ← DB table name (JPQL doesn't use this)
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "user_name")  // ← DB column name (JPQL doesn't use this)
    private String name;         // ← JPQL uses this field name

    private String phone;
}
```

---

## Return Type Rules

```
RETURN TYPE IN JPQL
====================

There is no strict rule — you choose based on what your query returns.

┌─────────────────────────────────┬────────────────────────────────────┐
│  If query can return many rows  │  Use List<UserDetails>             │
│  If query returns only one row  │  Use UserDetails (single object)   │
└─────────────────────────────────┴────────────────────────────────────┘

⚠️  IMPORTANT INTERVIEW POINT:
If your query CAN return multiple rows, but your return type
is a single object — JPQL throws an exception at runtime.

You are responsible for choosing the right return type.
JPA won't warn you at compile time.
```

---

## JPQL with JOIN — OneToOne

Now the real power of JPQL — writing JOINs across related entities.

### Entity setup

```java
@Entity
public class UserDetails {
    private Long userId;
    private String name;
    private String phone;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_address")
    private UserAddress userAddress;   // ← relationship field
}

@Entity
public class UserAddress {
    private Long id;
    private String street;
    private String city;
    private String state;
    private String country;
    private String pinCode;
}
```

### Writing the JOIN query

```java
@Query("SELECT ud FROM UserDetails ud JOIN ud.userAddress ad WHERE ud.name = :userFirstName")
List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userName);
```

```
JPQL JOIN vs SQL JOIN
======================

SQL:
  SELECT *
  FROM   user_details ud
  JOIN   user_address ua ON ud.user_address_id = ua.id  ← you write ON manually
  WHERE  ud.user_name = ?

JPQL:
  SELECT ud
  FROM   UserDetails ud
  JOIN   ud.userAddress ad    ← navigate via the RELATIONSHIP FIELD
  WHERE  ud.name = :userFirstName
  
  NO "ON" CLAUSE NEEDED.
  JPA already knows how the two entities are linked
  (through @JoinColumn and @OneToOne).
  It automatically adds the ON condition in the generated SQL.
```

Generated SQL by Hibernate:

```sql
SELECT ud.user_id, ud.user_name, ud.phone, ud.user_address
FROM   user_details ud
JOIN   user_address ua ON ua.id = ud.user_address   ← JPA added this automatically
WHERE  ud.user_name = ?
```

---

## Selecting Specific Fields (not the whole entity)

Sometimes you don't want ALL fields of both entities. You only want specific columns — say, `name` from UserDetails and `country` from UserAddress.

### Option 1 — Return as Object array (List\<Object[]\>)

```java
@Query("SELECT ud.name, ad.country FROM UserDetails ud JOIN ud.userAddress ad WHERE ud.name = :userFirstName")
List<Object[]> findUserDetailsWithAddress(@Param("userFirstName") String userName);
```

```
Each row in the result is an Object[]:
  Object[0] → ud.name   (String)
  Object[1] → ad.country (String)
```

Then in your service class, you manually parse it:

```java
List<Object[]> dbOutput = userDetailsRepository.findUserDetailsWithAddress(name);
List<UserDTO> output = new ArrayList<>();

for (Object[] val : dbOutput) {
    String userName = (String) val[0];
    String country  = (String) val[1];
    UserDTO dto = new UserDTO(userName, country);
    output.add(dto);
}
```

This works, but feels messy. There's a cleaner way.

### Option 2 — Return directly as custom DTO (cleaner)

First, create a DTO with a matching constructor:

```java
public class UserDTO {
    String userName;
    String country;

    public UserDTO(String userName, String country) {  // ← constructor must exist
        this.userName = userName;
        this.country  = country;
    }
}
```

Then use `new` keyword inside the JPQL query itself:

```java
@Query("SELECT new com.conceptandcoding.learningspringboot.jpa.DTO.UserDTO(ud.name, ad.country) " +
       "FROM UserDetails ud JOIN ud.userAddress ad WHERE ud.name = :userFirstName")
List<UserDTO> findUserDetailsWithAddress(@Param("userFirstName") String userName);
```

```
HOW THIS WORKS:
===============
For every row the query returns, JPQL calls:
  new UserDTO(ud.name, ad.country)

It's exactly like writing:
  new UserDTO("John", "India")

in Java — just happening inside the query for each result row.

You must provide the FULL package path of the DTO class.
The constructor must match the fields you're selecting.
```

```
Object[] vs Custom DTO
=======================
┌──────────────────────┬─────────────────────────────────────┐
│  Object[]            │  Custom DTO                         │
├──────────────────────┼─────────────────────────────────────┤
│  Messy to parse      │  Clean and type-safe                │
│  Manual casting      │  No manual casting needed           │
│  Error-prone         │  Constructor-based, clear intent    │
│  No IDE support      │  Full IDE support                   │
└──────────────────────┴─────────────────────────────────────┘
Always prefer Custom DTO in production code.
```

---

## JPQL with JOIN — OneToMany

The query syntax stays almost identical. Only the relationship type changes in the entity:

```java
@Entity
public class UserDetails {

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private List<UserAddress> userAddressList = new ArrayList<>();
    //                        ^^^^^^^^^^^^^^^
    //                        now a List, because one user → many addresses
}
```

```java
@Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userName);
//                                                   ^^^^^^^^^^^^^^
//                                                   navigate via the List field
```

The query looks the same — you just reference the correct field name (`userAddressList` instead of `userAddress`). JPA handles the rest.

---

## Joining MORE than Two Tables

Exactly like SQL — just keep chaining JOINs:

```
Say:
  Table A → has OneToMany with Table B
  Table B → has OneToMany with Table C

JPQL:
  SELECT a
  FROM   A a
  JOIN   a.bList b      ← join A to B via relationship field
  JOIN   b.cList c      ← join B to C via relationship field
  WHERE  c.someProperty = :someValue

No ON clause anywhere. JPA figures it all out from the entity mappings.
```

---

## Quick Summary of Step 3 so far

```
┌──────────────────────────────────────────────────────────────────┐
│  JPQL ESSENTIALS                                                 │
├──────────────────┬───────────────────────────────────────────────┤
│  @Query          │  Holds your JPQL string                       │
│  @Param          │  Binds method param to :namedParam in query   │
│  Entity name     │  Used in FROM clause (not table name)         │
│  Field name      │  Used in WHERE clause (not column name)       │
│  Alias           │  Short name for entity (like ud, u, a)        │
│  JOIN            │  Navigate via relationship field, no ON needed│
│  new DTO(...)    │  Construct custom DTO directly in query       │
│  Return type     │  You decide — List or single object           │
└──────────────────┴───────────────────────────────────────────────┘
```

---
# Step 4 — The N+1 Problem & Its Solutions

## The Problem First

At the end of Step 3, we set up a OneToMany query like this:

```java
@Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")
List<UserDetails> findUserDetailsWithAddress(@Param("userFirstName") String userName);
```

Looks fine. But when you actually run this — JPA hits the database **way more times than you expect**. That's the N+1 problem.

---

## Understanding N+1 Step by Step

Let's say the DB has this data:

```
USER_DETAILS table:          USER_ADDRESS table:
====================         ====================
ID | NAME | PHONE            ID | USER_ID | CITY
---+------+------            ---+---------+----------
1  | AA   | 1234             1  |    1    | cityNameA
2  | AA   | 1234             2  |    2    | cityNameB
```

Two users, both named "AA". Each has one address.

Now when you call the query with name = "AA", here's what JPA actually does:

```
WHAT YOU EXPECT:              WHAT ACTUALLY HAPPENS:
=================             =======================

1 query that fetches          Query 1: fetch all users named "AA"
users + their addresses       ──────────────────────────────────
in one shot.                  SELECT ud.user_id, ud.user_name, ud.phone
                              FROM   user_details ud
                              JOIN   user_address ual
                              ON     ud.user_id = ual.user_id
                              WHERE  ud.user_name = ?
                              → returns User 1 and User 2

                              Query 2: fetch addresses for User 1
                              ────────────────────────────────────
                              SELECT * FROM user_address
                              WHERE  user_id = ?  (user_id = 1)

                              Query 3: fetch addresses for User 2
                              ────────────────────────────────────
                              SELECT * FROM user_address
                              WHERE  user_id = ?  (user_id = 2)

                              TOTAL: 3 queries (1 + 2)
```

Now generalize this:

```
N+1 FORMULA
============

Say you have N users matching your query.

  1 query   → fetch all N users (the parent rows)
  N queries → fetch addresses for each user (one per user)
  ─────────────────────────────────────────────────────
  N+1 total queries hit to the database.

If N = 100 users → 101 DB hits
If N = 1000 users → 1001 DB hits

This kills performance in production.
```

---

## Why Doesn't EAGER Fix This?

This is a very common interview question. The answer is subtle.

```
EAGER INITIALIZATION — WHEN IT WORKS vs WHEN IT DOESN'T
=========================================================

CASE 1: Query fetches only ONE parent (e.g. findById)
──────────────────────────────────────────────────────
  JPA knows: "only 1 user, can have many addresses"
  JPA internally drafts a proper JOIN query.
  Fetches user + all addresses in ONE query. ✓
  EAGER works fine here.

CASE 2: Query fetches MULTIPLE parents (e.g. our name search)
──────────────────────────────────────────────────────────────
  JPA knows: "multiple users possible, each with many addresses"
  JPA thinks: "for performance, let me first just get all users.
               I'll fetch addresses only when someone actually
               accesses them."
  
  Result:
    → First fetches all parents (1 query)
    → Then for each parent, fetches its children (N queries)
    → EAGER does NOT collapse this into 1 query. ✗

WHY does JPA behave this way?
  JPA doesn't know in advance how many parents will come back.
  If it tried to JOIN everything upfront for potentially
  thousands of parents, it could be even worse.
  So it optimizes by doing a flat parent fetch first,
  then child fetches on demand.
  This is the N+1 behavior.
```

---

## Three Solutions to N+1

### Solution 1 — JOIN FETCH (JPQL)

The most direct fix. You tell JPQL explicitly: "fetch the children too, in the same query."

```java
// Before (causes N+1):
@Query("SELECT ud FROM UserDetails ud JOIN ud.userAddressList ad WHERE ud.name = :userFirstName")

// After (N+1 fixed):
@Query("SELECT ud FROM UserDetails ud JOIN FETCH ud.userAddressList ad WHERE ud.name = :userFirstName")
//                                         ^^^^^
//                                         just add FETCH here
```

What changes in the generated SQL:

```
WITHOUT JOIN FETCH:                  WITH JOIN FETCH:
====================                 ================

Query 1 (parent fetch):              Single Query:
  SELECT ud.user_id,                   SELECT ud.user_id,
         ud.user_name,                        ud.user_name,
         ud.phone                             ud.phone,
  FROM   user_details ud                      ual.id,
  JOIN   user_address ual                     ual.city,
  ON     ...                                  ual.country,
  WHERE  ud.user_name = ?                     ual.pin_code,
                                              ual.state,
Query 2 (child fetch for user 1):             ual.street
  SELECT * FROM user_address          FROM   user_details ud
  WHERE  user_id = ?                  JOIN   user_address ual
                                      ON     ud.user_id = ual.user_id
Query 3 (child fetch for user 2):     WHERE  ud.user_name = ?
  SELECT * FROM user_address
  WHERE  user_id = ?           →   Everything in ONE query. ✓
```

```
HOW JOIN FETCH WORKS:
======================
Without FETCH:
  "SELECT ud" → JPA thinks "I only need user fields.
                 I'll get addresses later if needed."

With FETCH:
  "JOIN FETCH" → JPA thinks "Even though I'm selecting ud,
                  go ahead and pull all address data too,
                  right now, in this same query."

It's like telling JPA: "don't be lazy, get everything now."
```

---

### Solution 2 — @BatchSize

This one is useful when you're NOT writing a JPQL query — for example, using derived methods or built-in JPA methods.

You can't add `JOIN FETCH` to a derived method name. So instead, you annotate the relationship field with `@BatchSize`:

```java
@Entity
public class UserDetails {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @BatchSize(size = 10)    // ← add this
    @JoinColumn(name = "user_id")
    private List<UserAddress> userAddressList;
}
```

How it changes behavior:

```
WITHOUT @BatchSize (N+1):         WITH @BatchSize(size=10):
==========================        ==========================

1 query → fetch all users         1 query → fetch all users

N queries → 1 per user            Batched queries → instead of
  SELECT * FROM user_address        one query per user, JPA groups
  WHERE user_id = 1                 them into batches:

  SELECT * FROM user_address        SELECT * FROM user_address
  WHERE user_id = 2                 WHERE user_id IN (1,2,3,4,5,6,7,8,9,10)
                                                    ← up to 10 at a time
  SELECT * FROM user_address
  WHERE user_id = 3                 If you have 25 users:
  ...                                 Batch 1: user_id IN (1..10)  → 1 query
                                      Batch 2: user_id IN (11..20) → 1 query
  N queries total                     Batch 3: user_id IN (21..25) → 1 query
                                      Total: 3 queries (not 25) ✓
```

```
@BatchSize — important to understand:
======================================
It does NOT reduce N+1 down to 1 query.
It reduces it from N+1 to roughly (N/batchSize + 1) queries.

For 100 users with @BatchSize(size=10):
  Without: 101 queries
  With:    11 queries  (much better, not perfect)

Use this when JOIN FETCH isn't available
(e.g. derived query methods, built-in findAll, etc.)
```

---

### Solution 3 — @EntityGraph

This is especially helpful with **derived query methods**, where you can't write `JOIN FETCH` because there's no `@Query` annotation.

```java
@EntityGraph(attributePaths = "userAddressList")
List<UserDetails> findUsersBy();
//                ^^^^^^^^^^^^^
//                This is a derived query method —
//                no @Query, so can't use JOIN FETCH
```

```
HOW @EntityGraph WORKS:
========================

@EntityGraph(attributePaths = "userAddressList")

You're telling JPA:
  "Whenever this method runs, automatically fetch the
   'userAddressList' relationship along with the parent —
   in the same query. Don't wait to be asked."

It's like attaching a JOIN FETCH instruction
directly onto a derived method.
```

```
WHICH SOLUTION TO USE WHEN?
=============================

┌─────────────────────────┬──────────────────────────────────────────┐
│  Situation              │  Best Solution                           │
├─────────────────────────┼──────────────────────────────────────────┤
│  Writing JPQL (@Query)  │  JOIN FETCH — cleanest, 1 query          │
│  Using derived method   │  @EntityGraph — works without @Query     │
│  Using built-in methods │  @BatchSize — reduces but not fully 1    │
│  (findAll, etc.)        │  @EntityGraph also works here            │
└─────────────────────────┴──────────────────────────────────────────┘
```

---

## Full N+1 Picture

```
N+1 PROBLEM — COMPLETE OVERVIEW
=================================

WHEN does it occur?
  → Query fetches MULTIPLE parent rows
  → Each parent has MULTIPLE children (OneToMany / ManyToMany)
  → You access children for each parent

WHEN does it NOT occur?
  → Query fetches only ONE parent (findById)
  → EAGER works fine there — JPA uses a proper JOIN internally

HOW MANY QUERIES?
  → 1 (fetch all parents) + N (one per parent for children) = N+1

THREE FIXES:
  1. JOIN FETCH  → 1 query total         (best, JPQL only)
  2. @BatchSize  → fewer queries         (works everywhere, not perfect)
  3. @EntityGraph→ 1 query total         (works on derived methods too)
```

---

## 🎯 Interview Tips

```
INTERVIEW TIPS — N+1 Problem
==============================

Q: What is the N+1 problem in JPA?
A: When fetching N parent rows where each has multiple children,
   JPA fires 1 query for all parents + N queries (one per parent)
   for their children = N+1 total DB hits.

Q: Does EAGER initialization solve N+1?
A: No. EAGER works fine when fetching a single parent (like findById),
   where JPA drafts a proper JOIN. But when the query can return
   multiple parents, EAGER still fires N+1 queries — it first fetches
   all parents, then separately fetches children for each one.

Q: What are the solutions?
A: Three main solutions:
   1. JOIN FETCH in JPQL — forces single query
   2. @BatchSize — groups child fetches into batches
   3. @EntityGraph — useful for derived methods, works like JOIN FETCH
```

---
# Step 5 — @Modifying Annotation, Flush & Clear

## The Problem First

In JPQL, whenever you use `@Query`, JPA has a **default assumption**:

```
@Query → JPA assumes → SELECT query
```

So if you try to write a DELETE, UPDATE, or INSERT query inside `@Query` without telling JPA about it — JPA panics and throws an error:

```
Exception:
org.hibernate.query.sqm.tree.select.SqmSelectStatement
→ "Expecting a SELECT Query"
```

That's exactly what `@Modifying` is for — it tells JPA to drop that assumption.

---

## @Modifying — What It Does

```
@Modifying ANNOTATION
======================

Without @Modifying:              With @Modifying:
====================             ================
@Query("DELETE FROM ...")        @Modifying
→ JPA expects SELECT             @Query("DELETE FROM ...")
→ throws exception               → JPA accepts DELETE/UPDATE/INSERT
                                 → works correctly
```

### Code Example

```java
// Repository:
@Modifying
@Transactional
@Query("DELETE FROM UserDetails ud WHERE ud.name = :userFirstName")
void deleteByUserName(@Param("userFirstName") String userName);
```

```
THREE ANNOTATIONS WORKING TOGETHER:
=====================================

@Modifying      → tells JPA: "this query modifies the DB,
                  don't expect a SELECT"

@Query          → holds your JPQL delete/update/insert statement

@Transactional  → required because we are modifying the DB.
                  Any operation that changes data must run
                  inside a transaction.
                  (Can be placed here on the repository method
                   OR on the calling service method — either works)
```

---

## The Flush & Clear Problem

Now here's where it gets interesting. Even after `@Modifying` is working correctly, there's a **hidden problem** involving the **persistence context**.

### What is Persistence Context (quick recap)

```
PERSISTENCE CONTEXT
====================
Think of it as JPA's first-level cache.
When you fetch something from DB, JPA stores a copy
of that object in the persistence context.
Next time you fetch the same thing, JPA returns
the cached copy — without hitting the DB again.

  DB                    Persistence Context
  ──                    ───────────────────
  user_id=1, name="AA"  user_id=1, name="AA"  ← cached copy
```

### The Problem Scenario

```
SEQUENCE OF EVENTS:
====================

Step 1: findById(1L)
  → JPA hits DB, fetches user with ID=1
  → stores a copy in persistence context
  
  DB:                   Persistence Context:
  user_id=1 exists  →   user_id=1 exists  ✓

Step 2: deleteByUserName("AA")   ← our @Modifying @Query
  → JPA runs DELETE on DB
  → user with ID=1 is now GONE from DB
  
  DB:                   Persistence Context:
  user_id=1 DELETED →   user_id=1 still here ← STALE DATA!

Step 3: findById(1L)
  → JPA checks persistence context first
  → finds user_id=1 there (stale cache)
  → returns it WITHOUT hitting DB
  
  output.isPresent() → TRUE   ← WRONG! User was deleted!
```

This is the bug. The persistence context is out of sync with the actual DB state.

---

## Understanding Flush and Clear

Before seeing the fix, understand what these two operations do:

```
FLUSH vs CLEAR
===============

┌─────────────────────────────────────────────────────────────────┐
│  FLUSH                                                          │
│  ─────                                                          │
│  Pushes any pending changes FROM persistence context TO DB.     │
│  But keeps the data in persistence context.                     │
│                                                                 │
│  DB ← ─────────────── Persistence Context                       │
│  (updated)              (still holds data)                      │
│                                                                 │
│  Think of it as: "commit the changes, but don't erase cache"    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  CLEAR                                                          │
│  ─────                                                          │
│  Wipes the persistence context completely.                      │
│  JPA stops managing those entities.                             │
│  Next fetch will go directly to DB.                             │
│                                                                 │
│  DB          Persistence Context                                │
│  (unchanged) → wiped clean ← now empty                          │
│                                                                 │
│  Think of it as: "erase the cache, force fresh DB call next"    │
└─────────────────────────────────────────────────────────────────┘
```

---

## The Fix — flushAutomatically & clearAutomatically

`@Modifying` has two optional parameters that solve this problem:

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("DELETE FROM UserDetails ud WHERE ud.name = :userFirstName")
void deleteByUserName(@Param("userFirstName") String userName);
```

Let's trace through the same scenario again, now with these flags:

```
WITH flushAutomatically=true, clearAutomatically=true:
=======================================================

Step 1: findById(1L)
  → fetches user from DB
  → stores in persistence context

  DB:                   Persistence Context:
  user_id=1 exists  →   user_id=1 exists

Step 2: deleteByUserName("AA")
  → flushAutomatically=true:
      checks if persistence context has any PENDING changes
      that need to be pushed to DB before running the delete.
      (nothing pending here, so nothing to flush)
  
  → DELETE runs on DB. user_id=1 gone from DB.
  
  → clearAutomatically=true:
      wipes persistence context clean after the DELETE.

  DB:                   Persistence Context:
  user_id=1 DELETED →   EMPTY (wiped clean) ✓

Step 3: findById(1L)
  → JPA checks persistence context → empty
  → goes to DB directly
  → user_id=1 not found in DB
  
  output.isPresent() → FALSE   ← CORRECT! ✓
```

---

## What flushAutomatically Actually Protects Against

The instructor makes a specific point about `flushAutomatically`. Here's the scenario where it matters:

```
WHY flushAutomatically=true IS IMPORTANT:
==========================================

Imagine this sequence:

Step 1: fetch user, modify their name in memory
  → change is sitting in persistence context
  → NOT yet saved to DB

  DB:                   Persistence Context:
  name = "AA"       →   name = "BB"  (pending change)

Step 2: run DELETE WHERE name = "AA"
  → without flushAutomatically:
      JPA runs DELETE first
      The pending "BB" change is never pushed
      Now you've deleted based on old data
      AND lost the pending update

  → with flushAutomatically=true:
      JPA first pushes "BB" to DB (flush)
      THEN runs DELETE WHERE name = "AA"
      DB now has name="BB" → not deleted
      Behavior is correct and predictable ✓
```

---

## Summary Table — @Modifying Flags

```
┌──────────────────────────┬────────────────────────────────────────────┐
│  Flag                    │  What it does                              │
├──────────────────────────┼────────────────────────────────────────────┤
│  flushAutomatically=true │  Before running the modifying query,       │
│                          │  push any pending persistence context      │
│                          │  changes to DB first.                      │
├──────────────────────────┼────────────────────────────────────────────┤
│  clearAutomatically=true │  After running the modifying query,        │
│                          │  wipe the persistence context clean.       │
│                          │  Forces fresh DB call on next fetch.       │
└──────────────────────────┴────────────────────────────────────────────┘
Both are false by default. You opt in only when needed.
```

---

## Full Picture — @Modifying Complete Flow

```
COMPLETE @Modifying FLOW
=========================

Repository method:

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Transactional
  @Query("DELETE FROM UserDetails ud WHERE ud.name = :userFirstName")
  void deleteByUserName(@Param("userFirstName") String userName);

  │
  ▼
Service calls deleteByUserName("AA")
  │
  ├─── flushAutomatically=true
  │    → push any pending persistence context changes to DB first
  │
  ├─── run the DELETE query on DB
  │
  ├─── clearAutomatically=true
  │    → wipe persistence context clean
  │
  └─── next findById() → goes to DB directly → correct result ✓
```

---

## 🎯 Interview Tips

```
INTERVIEW TIPS — @Modifying, Flush, Clear
==========================================

Q: What is @Modifying used for?
A: It tells JPA that the @Query contains a DELETE, UPDATE,
   or INSERT statement — not a SELECT. Without it, JPA
   throws an exception expecting a SELECT query.

Q: Do you need @Transactional with @Modifying?
A: Yes. Any query that modifies the DB must run inside
   a transaction. You can put @Transactional either on
   the repository method or the service method calling it.

Q: What problem do flushAutomatically and clearAutomatically solve?
A: When you modify the DB via JPQL (@Modifying @Query),
   the persistence context (first-level cache) doesn't
   automatically know about the change. So a subsequent
   findById might return stale data from the cache instead
   of hitting the DB. 
   clearAutomatically=true wipes the cache after the query,
   forcing fresh DB calls.
   flushAutomatically=true pushes any pending cache changes
   to DB before the modifying query runs.

Q: What's the difference between flush and clear?
A: Flush → pushes persistence context changes TO the DB,
           but keeps data in persistence context.
   Clear → wipes the persistence context entirely,
           forces next fetch to go to DB directly.
```

---
# Step 6 — Pagination & Sorting in JPQL

## The Good News First

This is a very short step. The instructor explicitly says:

> "Pagination and sorting in JPQL is exactly the same as what we have seen in the derived query."

So there's nothing new to learn here in terms of concept. You just combine what you already know — `@Query` from JPQL + `Pageable`/`Sort` from Step 2.

---

## How It Works

In derived queries, you added `Pageable` or `Sort` as a method parameter. You do **exactly the same** in JPQL methods:

```java
// Repository:
@Query("SELECT ud FROM UserDetails ud WHERE ud.name = :userFirstName")
List<UserDetails> findUserDetails(
    @Param("userFirstName") String userName,
    Pageable pageable    // ← same as derived query, just add it here
);
```

```java
// Service:
public List<UserDetails> findByUserName(String name) {
    Pageable page = PageRequest.of(1, 5);  // page 1, 5 records per page
    return userDetailsRepository.findUserDetails(name, page);
}
```

That's it. No special syntax. No changes to the JPQL string. JPA recognizes the `Pageable` parameter and automatically adds `OFFSET` and `FETCH` to the generated SQL — same as it did for derived queries.

---

## Generated SQL

```
Generated SQL (Hibernate):
==========================
SELECT   ud.user_id, ud.user_name, ud.phone
FROM     user_details ud
WHERE    ud.user_name = ?
OFFSET   ? rows            ← added automatically by JPA
FETCH    first ? rows only ← added automatically by JPA
```

---

## Quick Comparison

```
PAGINATION & SORTING — DERIVED vs JPQL
========================================

                    Derived Query          JPQL (@Query)
                    ─────────────          ─────────────
Pageable param?     Yes                    Yes (same)
Sort param?         Yes                    Yes (same)
PageRequest.of()?   Yes                    Yes (same)
Page<T> return?     Yes                    Yes (same)
Any difference?     None — works the same way in both
```

The only difference is that in JPQL you have `@Query` on top. The `Pageable` and `Sort` parameters behave identically.

---

# Step 7 — @NamedQuery Annotation

## The Problem First

Imagine you have a useful JPQL query that is needed in **multiple places** across your codebase — maybe in two different repository methods, or across two different repositories.

Without `@NamedQuery`, you'd have to copy-paste the same query string in multiple `@Query` annotations:

```java
// Repository 1:
@Query("SELECT u FROM UserDetails u WHERE u.name = :userFirstName")
List<UserDetails> findByUserName(...);

// Repository 2 (some other file):
@Query("SELECT u FROM UserDetails u WHERE u.name = :userFirstName")
List<UserDetails> searchByUserName(...);    // same query, duplicated!
```

Now if the query needs to change — say you add an extra condition — you have to find and update every copy. That's a maintenance problem.

`@NamedQuery` solves this by letting you **define the query once, give it a name, and reuse that name anywhere**.

---

## How @NamedQuery Works

### Step 1 — Define the named query on the Entity class

```java
@Table(name = "user_details")
@Entity
@NamedQuery(
    name  = "findByUserName",       // ← give it a name (you choose this)
    query = "SELECT u FROM UserDetails u WHERE u.name = :userFirstName"
                                    // ← the actual JPQL query
)
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "user_name")
    private String name;
    private String phone;

    @OneToOne(cascade = CascadeType.ALL)
    private UserAddress userAddress;

    // getters and setters
}
```

```
WHY on the Entity class?
=========================
Because the query is written in terms of THIS entity.
It makes sense to keep the query definition close
to the entity it describes.
```

### Step 2 — Use the named query in your Repository

```java
@Query(name = "findByUserName")    // ← reference by name, no query string needed
List<UserDetails> findUserDetails(
    @Param("userFirstName") String userName,
    Pageable pageable
);
```

Instead of writing the full JPQL string again inside `@Query`, you just pass `name = "findByUserName"` — and JPA looks up the query defined on the entity.

---

## How It All Connects

```
@NamedQuery FLOW
=================

Entity class (UserDetails.java):
─────────────────────────────────
@NamedQuery(
    name  = "findByUserName",          ← defined here ONCE
    query = "SELECT u FROM ..."
)
public class UserDetails { ... }

          │
          │  JPA reads this at startup,
          │  registers the query under name "findByUserName"
          │
          ▼

Repository (UserDetailsRepository.java):
─────────────────────────────────────────
@Query(name = "findByUserName")        ← referenced here
List<UserDetails> findUserDetails(..., Pageable pageable);

          │
          │  JPA looks up "findByUserName",
          │  finds the query defined on the entity,
          │  uses it — same as writing it inline
          │
          ▼

Another Repository (if needed):
────────────────────────────────
@Query(name = "findByUserName")        ← reused here too
List<UserDetails> searchUsers(...);

          │
          └── same query, no duplication ✓
```

---

## @NamedQuery vs @Query inline

```
┌────────────────────────┬──────────────────────────────────────────────┐
│                        │  @Query (inline)   │  @NamedQuery            │
├────────────────────────┼────────────────────┼─────────────────────────┤
│  Where query lives     │  On repository     │  On entity class        │
│                        │  method itself     │                         │
├────────────────────────┼────────────────────┼─────────────────────────┤
│  Reusable across       │  No — must         │  Yes — define once,     │
│  multiple methods?     │  copy-paste        │  use by name anywhere   │
├────────────────────────┼────────────────────┼─────────────────────────┤
│  Easy to maintain?     │  Changes needed    │  Change in one place,   │
│                        │  in every copy     │  reflects everywhere    │
├────────────────────────┼────────────────────┼─────────────────────────┤
│  When to use           │  Query used in     │  Same query needed      │
│                        │  only one place    │  in multiple places     │
└────────────────────────┴────────────────────┴─────────────────────────┘
```

---

## Full Lecture Summary — Everything in One View

```
JPA PART 8 — COMPLETE SUMMARY
================================

PROBLEM: Built-in JPA methods (save, findById) aren't
         enough for custom or complex queries.

SOLUTION 1: DERIVED QUERY
  → Write a method name following JPA naming convention
  → JPA auto-generates the SQL
  → Good for simple to moderate queries
  → Supports: AND, OR, IN, LIKE, Between, StartingWith, etc.
  → DELETE needs @Transactional
  → Pagination: add Pageable param + PageRequest.of(page, size)
  → Sorting: add Sort param + Sort.by("field").ascending()
  → Both together: PageRequest.of(page, size, Sort.by(...))

SOLUTION 2: JPQL
  → Write your own query using @Query annotation
  → Works on Entity names & fields (not table/column names)
  → Database independent
  → @Param binds method params to :namedParams in query
  → JOIN works via relationship field, no ON clause needed
  → Can return List<T>, single object, Object[], or custom DTO
  → Use "new DTO(...)" syntax inside query for clean DTO return
  → Pagination & Sorting: same as derived query

N+1 PROBLEM
  → Occurs when query fetches multiple parents,
    each with multiple children
  → JPA fires 1 query for parents + N queries for children
  → EAGER doesn't fix it for multiple-parent queries
  → Fix 1: JOIN FETCH in JPQL → 1 query total
  → Fix 2: @BatchSize → batches child queries, reduces hits
  → Fix 3: @EntityGraph → works on derived methods

@MODIFYING
  → Required when @Query contains DELETE/UPDATE/INSERT
  → Without it, JPA throws "Expecting SELECT" exception
  → Always pair with @Transactional
  → flushAutomatically=true → push pending cache changes
    to DB before running the modifying query
  → clearAutomatically=true → wipe persistence context
    after query, forces fresh DB call on next fetch

@NAMEDQUERY
  → Define query once on entity class with a name
  → Reference by name in @Query(name="...") anywhere
  → Avoids duplication when same query used in multiple places
  → Future changes needed in only one place
```

---

## 🎯 Final Interview Tips Roundup

```
INTERVIEW TIPS — Full Lecture
================================

Q: Difference between Derived Query and JPQL?
A: Derived query auto-generates SQL from method name —
   good for simple queries. JPQL lets you write your own
   query using entity names/fields — good for complex ones.
   JPQL is also database independent.

Q: Why does JPQL use entity names instead of table names?
A: Because JPA works at the object level. Using entity names
   makes queries database independent — you can switch
   databases without changing any JPQL.

Q: What is the N+1 problem?
A: Fetching N parents with children causes 1 + N DB queries.
   Fix with JOIN FETCH (JPQL), @BatchSize, or @EntityGraph.

Q: What does @Modifying do?
A: Tells JPA the @Query contains a non-SELECT statement.
   Pair with @Transactional always.

Q: What do flushAutomatically and clearAutomatically do?
A: flush → pushes pending persistence context changes to DB
           before the modifying query runs.
   clear → wipes persistence context after the modifying
           query, so next fetch goes directly to DB.

Q: What is @NamedQuery?
A: Lets you define a JPQL query once on the entity class
   with a name, then reuse it across multiple repository
   methods using @Query(name="...").
```

That wraps up the full **JPA Part 8** notes! The next part (Part 9) will cover **Criteria API, Specification API, and Native Query**.