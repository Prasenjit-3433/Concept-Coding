# Step 1 — Native Query: What it is & Why it Exists

---

## What is a Native Query?

Simply put, a Native Query is just **plain old SQL** — the same SQL you'd write directly in your database tool. Nothing special about the syntax. The only difference is you're writing it **inside your Spring Boot / JPA code**.

```sql
SELECT * FROM user_details WHERE user_name = 'John'
```

That's it. No JPQL abstractions, no entity magic. Just raw SQL talking directly to your database.

---

## But wait — we already have JPQL. Why do we need this?

This is exactly the question the instructor asks. To answer it, you first need to understand **what JPQL is limited by**.

JPQL (Java Persistence Query Language) is built around **entities**. Every query you write in JPQL revolves around your Java entity classes. That's powerful, but it also means JPQL has a hard ceiling on what it can do.

Here are the situations where JPQL simply **cannot help you**, and Native Query becomes necessary:

---

### Situation 1 — Complex, DB-specific queries (e.g., JSONB, LATERAL JOIN)

Some databases have special features. For example, PostgreSQL supports a **JSONB column** — a column that stores JSON data, and you can query inside that JSON. JPQL has no idea what JSONB is. It can't handle it. Native Query can, because you're writing plain SQL directly for that database.

### Situation 2 — Fetching results that don't map to any entity

In JPQL, every result has to look like an entity or a field of an entity. So you can do:

```java
SELECT u FROM UserDetails u         // full entity ✅
SELECT u.name FROM UserDetails u    // one field ✅
```

But what if you want something like `COUNT(*) AS stress` or some computed column that doesn't exist in your entity? JPQL can't return that. Native Query can, because you're writing SQL and you control exactly what comes back.

### Situation 3 — Joining tables that have NO relationship defined

In JPQL, joins are based on entity relationships (`@OneToOne`, `@OneToMany`, etc.). If two entities have no defined relationship between them, joining them in JPQL becomes complicated or outright impossible.

With Native Query, you just write the JOIN directly in SQL — no relationship needed, just table names and conditions.

### Situation 4 — Performance in Bulk Operations

When you use JPQL, JPA is **managing everything** behind the scenes:
- It keeps the **persistence context** (L1 cache) updated
- It tracks the **lifecycle** of every entity
- It does a lot of bookkeeping

All of this makes JPQL slightly slower for bulk operations. Native Query **bypasses all of that** and hits the database directly, making it faster in these scenarios.

---

## The Trade-offs you must know (Interview Important ⚠️)

The instructor is very clear that Native Query is not a free lunch. Here's what you give up:

```
╔══════════════════════════════════════════════════════════════╗
║                    NATIVE QUERY TRADE-OFFS                   ║
╠══════════════════╦═══════════════════════════════════════════╣
║  What you GAIN   ║  What you LOSE                            ║
╠══════════════════╬═══════════════════════════════════════════╣
║ Complex queries  ║ DB Independence — if you switch from      ║
║ (JSONB, etc.)    ║ MySQL to PostgreSQL tomorrow, your        ║
║                  ║ native queries may break. Code changes    ║
║                  ║ required.                                 ║
╠══════════════════╬═══════════════════════════════════════════╣
║ Non-entity       ║ No Caching — JPA's L1 cache               ║
║ results          ║ (persistence context) is NOT used.        ║
║                  ║ Every query hits the DB directly.         ║
╠══════════════════╬═══════════════════════════════════════════╣
║ Unrelated table  ║ No Entity Lifecycle Management —          ║
║ joins            ║ JPA won't track or manage the entities    ║
║                  ║ returned. You handle everything.          ║
╠══════════════════╬═══════════════════════════════════════════╣
║ Bulk operation   ║ No Lazy Loading — JPA features like       ║
║ speed            ║ lazy loading don't apply here.            ║
╚══════════════════╩═══════════════════════════════════════════╝
```

---

## The Big Picture — When to use What

The instructor summarizes the decision like this:

```
┌─────────────────────────────────────────────────────────────┐
│                    WHICH QUERY TO USE?                      │
│                                                             │
│   Simple queries on entities?                               │
│   → Use JPQL                                                │
│                                                             │
│   Complex SQL, DB-specific features, non-entity results,    │
│   unrelated joins, or bulk performance?                     │
│   → Use Native Query                                        │
│                                                             │
│   Dynamic queries (conditions decided at runtime)?          │
│   → JPQL has NO dynamic query support                       │
│   → Native Query supports dynamic queries                   │
│   → BUT Native Query is DB-dependent                        │
│   → For dynamic + DB-independent → use Criteria API         │
│      (covered later in this lecture)                        │
└─────────────────────────────────────────────────────────────┘
```

This gives you a clean mental model of where each tool fits.

---
# Step 2 — How to Write a Native Query (Basic Syntax + Full Field Mapping)

---

## The Syntax — Extremely Simple

If you already know how to write a JPQL query using `@Query`, then Native Query is just **one extra parameter** added to it.

Here's a JPQL query you'd normally write:

```java
@Query(value = "SELECT u FROM UserDetails u WHERE u.name = :userFirstName")
List<UserDetails> getUserDetailsByName(@Param("userFirstName") String userName);
```

To make it a Native Query, you just add `nativeQuery = true`:

```java
@Query(value = "SELECT * FROM user_details WHERE user_name = :userFirstName", 
       nativeQuery = true)
List<UserDetails> getUserDetailsByName(@Param("userFirstName") String userName);
```

That's literally the only change. Everything else — how you pass parameters, how you define the method — stays exactly the same.

---

## One Critical Difference in How You Write the Query

This is something the instructor really emphasizes, so pay close attention.

In JPQL, you write from the **Java/Entity perspective**:
- Table name = your Java entity class name → `UserDetails`
- Column name = your Java field name → `name`, `phone`

In Native Query, you write from the **Database perspective**:
- Table name = your actual DB table name → `user_details`
- Column name = your actual DB column name → `user_name`, `phone`

```
┌─────────────────────────────────────────────────────────────────┐
│              JPQL vs Native Query — What You Write              │
├──────────────────────────┬──────────────────────────────────────┤
│         JPQL             │         Native Query                 │
├──────────────────────────┼──────────────────────────────────────┤
│ Entity name:             │ DB Table name:                       │
│   UserDetails            │   user_details                       │
├──────────────────────────┼──────────────────────────────────────┤
│ Java field name:         │ DB column name:                      │
│   name                   │   user_name                          │
├──────────────────────────┼──────────────────────────────────────┤
│ SELECT u FROM            │ SELECT * FROM                        │
│   UserDetails u          │   user_details                       │
│ WHERE u.name = :x        │ WHERE user_name = :x                 │
└──────────────────────────┴──────────────────────────────────────┘
```

So when writing a Native Query, always think from the database's point of view — table names, column names, exactly as they exist in the DB.

---

## Full Field Mapping — How JPA Handles It Automatically

Now here's where it gets interesting. Look at this query:

```java
@Query(value = "SELECT * FROM user_details WHERE user_name = :userFirstName", 
       nativeQuery = true)
List<UserDetails> getUserDetailsByName(@Param("userFirstName") String userName);
```

The return type is `List<UserDetails>` — a Java entity. But your query is talking to the DB in DB language (`user_details`, `user_name`). So how does JPA know how to map the DB result back to the Java entity?

The answer: **when you use `SELECT *` (all fields), JPA automatically does the mapping for you.**

Here's how it works internally:

```
┌─────────────────────────────────────────────────────────────────────┐
│           HOW JPA AUTO-MAPS SELECT * IN NATIVE QUERY                │
│                                                                     │
│  DB Result Row:                                                     │
│  ┌───────────┬───────────┬──────────────┐                           │
│  │ user_id   │ user_name │ phone        │                           │
│  │    1      │  "John"   │ "9999999999" │                           │
│  └───────────┴───────────┴──────────────┘                           │
│          │                    │               │                     │
│          ▼                    ▼               ▼                     │
│  JPA reads @Column annotations on entity                            │
│  and matches DB column → Java field                                 │
│          │                    │               │                     │
│          ▼                    ▼               ▼                     │
│  UserDetails entity:                                                │
│  ┌───────────┬───────────┬──────────────┐                           │
│  │ userId    │ name      │ phone        │                           │
│  │    1      │  "John"   │ "9999999999" │                           │
│  └───────────┴───────────┴──────────────┘                           │
│                                                                     │
│  JPA knows: user_name (DB) → name (Java field)                      │
│  because of: @Column(name = "user_name") on the 'name' field        │
└─────────────────────────────────────────────────────────────────────┘
```

JPA looks at the `@Column` annotations in your entity class to figure out which DB column maps to which Java field, and it handles the whole thing for you — silently, automatically.

Here's what the entity looks like for reference:

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "user_name")   // DB column is user_name, Java field is name
    private String name;

    private String phone;         // DB column name same as field name, no @Column needed

    @OneToOne(cascade = CascadeType.ALL)
    private UserAddress userAddress;

    // getters and setters
}
```

And the repository stays clean and simple:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT * FROM user_details WHERE user_name = :userFirstName", 
           nativeQuery = true)
    List<UserDetails> getUserDetailsByName(@Param("userFirstName") String userName);

}
```

---

## The Rule to Remember

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│   SELECT *  →  JPA auto-maps DB columns to entity        │
│                fields using @Column annotations.         │
│                You don't have to do anything extra.      │
│                                                          │
│   SELECT specific fields  →  JPA does NOT auto-map.      │
│                You must handle mapping yourself.         │
│                (This is covered in Step 3)               │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

This is a really important boundary to understand. As long as you're selecting all fields with `*`, you're completely safe — JPA takes full responsibility for mapping. The moment you select only specific fields, you're on your own, and that's exactly what the next step covers.

---
# Step 3 — Partial Field Mapping: The Problem & Two Solutions

---

## The Problem — What Happens When You Select Only Specific Fields?

Let's say instead of `SELECT *`, you only want two fields — `user_name` and `phone`:

```java
@Query(value = "SELECT user_name, phone FROM user_details WHERE user_name = :userFirstName", 
       nativeQuery = true)
List<UserDetails> getUserDetailsByName(@Param("userFirstName") String userName);
```

Looks fine, right? But when you actually run this — **you get an exception**:

```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Column "user_id" not found
```

Here's why this happens:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    WHY THE EXCEPTION OCCURS                         │
│                                                                     │
│  Your query returns only:                                           │
│  ┌───────────┬──────────┐                                           │
│  │ user_name │ phone    │                                           │
│  │  "John"   │ "99999"  │                                           │
│  └───────────┴──────────┘                                           │
│                                                                     │
│  But JPA tries to map result → UserDetails entity                   │
│  UserDetails entity expects ALL fields:                             │
│  ┌───────────┬───────────┬──────────┐                               │
│  │ userId    │ name      │ phone    │                               │
│  │    ?      │  "John"   │ "99999"  │                               │
│  └───────────┴───────────┴──────────┘                               │
│            ▲                                                        │
│            │                                                        │
│      userId is missing from the query result!                       │
│      JPA panics → throws exception                                  │
└─────────────────────────────────────────────────────────────────────┘
```

JPA tries to populate the full `UserDetails` entity but can't find `user_id` in the result — so it crashes. The moment you go partial, **you have to explicitly handle the mapping yourself**.

The instructor gives **two ways** to solve this.

---

## Solution 1 — Using `@SqlResultSetMapping` + `@NamedNativeQuery`

This is the more formal, annotation-driven approach.

### Step 1 — Create a DTO for the fields you expect

Since you're only fetching `user_name` and `phone`, create a simple DTO that holds exactly those:

```java
public class UserDTO {

    String userName;
    String phone;

    public UserDTO(String userName, String phone) {
        this.userName = userName;
        this.phone = phone;
    }

    // getters and setters
}
```

### Step 2 — Add two annotations on your Entity class

This is the key part. Both `@NamedNativeQuery` and `@SqlResultSetMapping` go **above your entity class** (on `UserDetails`):

```java
@Table(name = "user_details")
@Entity
@NamedNativeQuery(
    name = "UserDetails.getUserDetailsByName",         // give your query a name
    query = "SELECT user_name, phone FROM user_details WHERE user_name = :userFirstName",
    resultSetMapping = "UserDTOMapping"                // link to the mapping below
)
@SqlResultSetMapping(
    name = "UserDTOMapping",                           // same name referenced above
    classes = @ConstructorResult(
        targetClass = UserDTO.class,                   // which class to create object of
        columns = {
            @ColumnResult(name = "user_name", type = String.class),  // 1st constructor param
            @ColumnResult(name = "phone", type = String.class)       // 2nd constructor param
        }
    )
)
public class UserDetails {
    // ... fields, getters, setters
}
```

Here's how these two annotations connect and work together:

```
┌──────────────────────────────────────────────────────────────────────┐
│         HOW @NamedNativeQuery + @SqlResultSetMapping WORK            │
│                                                                      │
│  @NamedNativeQuery                                                   │
│  ┌────────────────────────────────────────────────────┐             │
│  │ name    →  "UserDetails.getUserDetailsByName"       │             │
│  │ query   →  SELECT user_name, phone FROM ...         │             │
│  │ resultSetMapping → "UserDTOMapping"  ──────────┐   │             │
│  └────────────────────────────────────────────────│───┘             │
│                                                   │                 │
│                                points to          │                 │
│                                                   ▼                 │
│  @SqlResultSetMapping                                                │
│  ┌────────────────────────────────────────────────────┐             │
│  │ name  →  "UserDTOMapping"                          │             │
│  │ targetClass → UserDTO.class                        │             │
│  │ columns:                                           │             │
│  │   1st → user_name  ──► UserDTO constructor param 1 │             │
│  │   2nd → phone      ──► UserDTO constructor param 2 │             │
│  └────────────────────────────────────────────────────┘             │
│                                                                      │
│  Result: JPA calls new UserDTO("John", "99999") for each row        │
└──────────────────────────────────────────────────────────────────────┘
```

### Step 3 — Use just the query name in the Repository

Since the query is already defined via `@NamedNativeQuery`, your repository becomes very clean — you just reference the name:

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(name = "UserDetails.getUserDetailsByName", nativeQuery = true)
    List<UserDTO> getUserDetailsByName(@Param("userFirstName") String userName);

}
```

Notice: the return type is now `List<UserDTO>`, not `List<UserDetails>`.

---

## Solution 2 — Manual Mapping Using `Object[]`

This is the simpler, more direct approach — no extra annotations needed.

### Step 1 — Return `List<Object[]>` from the repository

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT user_name, phone FROM user_details WHERE user_name = :userFirstName", 
           nativeQuery = true)
    List<Object[]> getUserDetailsByName(@Param("userFirstName") String userName);

}
```

Instead of mapping to an entity or DTO, you're just saying — give me the raw result as an array of objects. Each row in the result becomes one `Object[]`.

Here's what the result looks like in memory:

```
┌─────────────────────────────────────────────────┐
│           List<Object[]> structure              │
│                                                 │
│  Index 0 → Object[] { "John",   "99999" }       │
│  Index 1 → Object[] { "Jane",   "88888" }       │
│  Index 2 → Object[] { "James",  "77777" }       │
│                                                 │
│  Each Object[]:                                 │
│    [0] → user_name (String)                     │
│    [1] → phone (String)                         │
└─────────────────────────────────────────────────┘
```

### Step 2 — Manually map `Object[]` to your DTO in the Service class

```java
public List<UserDTO> getUserDetailsByName(String name) {

    List<Object[]> results = userDetailsRepository.getUserDetailsByName(name);

    return results.stream()
        .map(obj -> new UserDTO((String) obj[0], (String) obj[1]))
        .collect(Collectors.toList());
}
```

You iterate over the results, take each `Object[]`, cast the values, pass them into your DTO constructor, and collect everything into a list.

---

## Side by Side Comparison — Which Solution to Use When?

```
┌──────────────────────────┬────────────────────────┬──────────────────────────┐
│                          │  Solution 1            │  Solution 2              │
│                          │  @SqlResultSetMapping  │  Manual Object[]         │
├──────────────────────────┼────────────────────────┼──────────────────────────┤
│ Extra annotations?       │ Yes, on entity class   │ No                       │
├──────────────────────────┼────────────────────────┼──────────────────────────┤
│ Manual mapping in        │ No, JPA handles it     │ Yes, you do it in        │
│ service layer?           │ via annotations        │ the service class        │
├──────────────────────────┼────────────────────────┼──────────────────────────┤
│ Code cleanliness         │ Cleaner service layer  │ Slightly more code       │
│                          │                        │ in service               │
├──────────────────────────┼────────────────────────┼──────────────────────────┤
│ Flexibility              │ Fixed at annotation    │ You control mapping      │
│                          │ level                  │ at runtime               │
├──────────────────────────┼────────────────────────┼──────────────────────────┤
│ Best for                 │ Fixed queries with     │ Quick, ad-hoc partial    │
│                          │ known DTO structure    │ field fetching           │
└──────────────────────────┴────────────────────────┴──────────────────────────┘
```

---

## The Complete Rule — All Three Scenarios Together

```
┌──────────────────────────────────────────────────────────────────┐
│              NATIVE QUERY MAPPING — COMPLETE PICTURE             │
│                                                                  │
│  SELECT *                                                        │
│  → JPA auto-maps to entity                                       │
│  → Return type: List<YourEntity>                                 │
│  → Nothing extra needed ✅                                        │
│                                                                  │
│  SELECT specific fields — Approach 1                             │
│  → Use @NamedNativeQuery + @SqlResultSetMapping on entity        │
│  → JPA maps result to DTO via constructor                        │
│  → Return type: List<YourDTO>                                    │
│  → More annotation work, cleaner service layer                   │
│                                                                  │
│  SELECT specific fields — Approach 2                             │
│  → Return List<Object[]> from repository                         │
│  → Manually cast and map in service layer                        │
│  → Return type: List<YourDTO> (after manual mapping)             │
│  → Less annotation work, more code in service                    │
└──────────────────────────────────────────────────────────────────┘
```

---
# Step 4 — Dynamic Native Query

---

## The Problem — Why Do We Need Dynamic Queries?

So far, every query we've written has been **static** — the structure of the query is fixed. The only thing that changes is the value of the parameter.

But think about a real-world scenario. You have a search screen where a user can filter by:
- Name
- Phone number
- City

The user might fill in all three, or just one, or none. You don't know at compile time which filters will be active. So your WHERE clause needs to be **built at runtime**, depending on what the user actually provides.

With `@Query` annotation, this is **not possible**. The query is fixed at compile time — you can't add or remove conditions dynamically.

This is where **Dynamic Native Query** comes in.

---

## The Approach — Stop Using Repository, Use EntityManager Directly

For dynamic native queries, the repository interface is no longer in the picture. Instead, you work directly inside the **Service class** using `EntityManager`.

```
┌──────────────────────────────────────────────────────────────────┐
│              STATIC vs DYNAMIC — WHERE THINGS LIVE               │
│                                                                  │
│  Static Native Query (@Query annotation)                         │
│  Controller → Service → Repository (@Query) → DB                 │
│                                                                  │
│  Dynamic Native Query (EntityManager)                            │
│  Controller → Service (EntityManager does everything) → DB       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## How to Build a Dynamic Native Query — Step by Step

The instructor walks through this very carefully. Let's follow the same flow.

### Step 1 — Inject EntityManager into your Service class

```java
@Service
public class UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

}
```

`@PersistenceContext` is the correct annotation to inject `EntityManager` in a Spring managed bean.

---

### Step 2 — Start building the query using StringBuilder

You start with the base query — the part that is always present, no matter what:

```java
StringBuilder queryBuilder = new StringBuilder(
    "SELECT ud.user_name AS user_name, ud.phone AS phone, ua.city AS city "
);
queryBuilder.append("FROM user_details ud ");
queryBuilder.append("JOIN user_address ua ON ud.user_address_id = ua.id ");
queryBuilder.append("WHERE 1=1 ");
```

Now, why `WHERE 1=1`? The instructor explains this really well.

```
┌──────────────────────────────────────────────────────────────────┐
│                   WHY WHERE 1=1 ?                                │
│                                                                  │
│  Without WHERE 1=1:                                              │
│  - If condition 1 is active → "WHERE condition1"                 │
│  - If condition 1 is NOT active, condition 2 is → ???            │
│    Now you need to figure out: did WHERE already appear?         │
│    Should I write WHERE or AND?                                  │
│  - If NO conditions are active → query breaks without WHERE      │
│                                                                  │
│  With WHERE 1=1:                                                 │
│  - WHERE is always present (1=1 is always true, costs nothing)   │
│  - Every dynamic condition just adds "AND condition"             │
│  - If no conditions are active → WHERE 1=1 is harmless           │
│  - Clean, no special case handling needed ✅                      │
└──────────────────────────────────────────────────────────────────┘
```

---

### Step 3 — Create a parameter list to track values for `?` placeholders

```java
List<Object> parameters = new ArrayList<>();
```

Every time you add a `?` placeholder to the query string, you also add the corresponding value to this list. They stay in sync — position matters.

---

### Step 4 — Dynamically add conditions based on input

```java
if (userName != null && !userName.isEmpty()) {
    queryBuilder.append("AND ud.user_name = ? ");
    parameters.add(userName);
}

// you can keep adding more conditions the same way
// if (phone != null && !phone.isEmpty()) {
//     queryBuilder.append("AND ud.phone = ? ");
//     parameters.add(phone);
// }
```

Each condition is only added **if the value is actually present**. This is the core of dynamic query building.

At this point, your query string might look like this (assuming userName was provided):

```sql
SELECT ud.user_name AS user_name, ud.phone AS phone, ua.city AS city 
FROM user_details ud 
JOIN user_address ua ON ud.user_address_id = ua.id 
WHERE 1=1 AND ud.user_name = ?
```

---

### Step 5 — Create the native query from the built string

```java
Query nativeQuery = entityManager.createNativeQuery(queryBuilder.toString());
```

At this point, the `?` placeholders are still unfilled. You have the structure of the query, but the actual values haven't been plugged in yet.

---

### Step 6 — Fill in the `?` placeholders with actual values

```java
for (int i = 0; i < parameters.size(); i++) {
    nativeQuery.setParameter(i + 1, parameters.get(i));
}
```

Important detail the instructor points out: **your parameter list is 0-indexed** (Java list), but **JPA's `?` positions are 1-indexed**. That's why you do `i + 1`.

```
┌──────────────────────────────────────────────────────────────────┐
│              HOW PARAMETER FILLING WORKS                         │
│                                                                  │
│  Query:  WHERE 1=1 AND ud.user_name = ?  AND ud.phone = ?        │
│                                              ▲           ▲       │
│                                         position 1   position 2  │
│                                                                  │
│  parameters list:  [ "John",  "99999" ]                          │
│                       index 0   index 1                          │
│                                                                  │
│  Loop:                                                           │
│    i=0 → setParameter(1, "John")   fills first  ?                │
│    i=1 → setParameter(2, "99999")  fills second ?                │
└──────────────────────────────────────────────────────────────────┘
```

---

### Step 7 — Execute the query and handle the result

```java
List<Object[]> result = nativeQuery.getResultList();

// map to DTO
return result.stream()
    .map(obj -> new UserDTO((String) obj[0], (String) obj[1]))
    .collect(Collectors.toList());
```

Since you're returning specific fields (not `*`), the result comes back as `List<Object[]>` and you manually map it to your DTO — exactly like Solution 2 from Step 3.

---

## The Complete Dynamic Native Query — Full Picture

```java
@Service
public class UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<UserDTO> getUserDetailsByNameNativeQuery(String userName) {

        // Step 1: Base query — always present
        StringBuilder queryBuilder = new StringBuilder(
            "SELECT ud.user_name AS user_name, ud.phone AS phone, ua.city AS city "
        );
        queryBuilder.append("FROM user_details ud ");
        queryBuilder.append("JOIN user_address ua ON ud.user_address_id = ua.id ");
        queryBuilder.append("WHERE 1=1 ");

        // Step 2: Parameter list
        List<Object> parameters = new ArrayList<>();

        // Step 3: Dynamic conditions
        if (userName != null && !userName.isEmpty()) {
            queryBuilder.append("AND ud.user_name = ? ");
            parameters.add(userName);
        }

        // Step 4: Create native query
        Query nativeQuery = entityManager.createNativeQuery(queryBuilder.toString());

        // Step 5: Fill placeholders
        for (int i = 0; i < parameters.size(); i++) {
            nativeQuery.setParameter(i + 1, parameters.get(i));
        }

        // Step 6: Execute
        List<Object[]> result = nativeQuery.getResultList();

        // Step 7: Map and return
        return UserDTO.mapResultToDTO(result);
    }
}
```

---

## The Full Flow as a Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                 DYNAMIC NATIVE QUERY — FULL FLOW                    │
│                                                                     │
│  1. StringBuilder                                                   │
│     → Base SQL query (always fixed part)                            │
│     → WHERE 1=1 (so AND can always be appended safely)              │
│                  │                                                  │
│                  ▼                                                  │
│  2. Check each input condition                                      │
│     → if present: append "AND condition = ?" to query               │
│                   add value to parameters list                      │
│     → if absent:  skip, query unchanged                             │
│                  │                                                  │
│                  ▼                                                  │
│  3. entityManager.createNativeQuery(queryBuilder.toString())        │
│     → Query object created, ? still unfilled                        │
│                  │                                                  │
│                  ▼                                                  │
│  4. Loop through parameters list                                    │
│     → nativeQuery.setParameter(i+1, value)                          │
│     → fills each ? in order (1-indexed)                             │
│                  │                                                  │
│                  ▼                                                  │
│  5. nativeQuery.getResultList()                                     │
│     → returns List<Object[]>                                        │
│                  │                                                  │
│                  ▼                                                  │
│  6. Manual mapping → List<UserDTO>                                  │
│     → return to controller                                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The Honest Trade-off the Instructor Points Out

Dynamic Native Query is powerful, but it comes with **a lot of manual work**:

```
┌──────────────────────────────────────────────────────┐
│         WHAT YOU MANAGE YOURSELF                     │
│                                                      │
│  ✋ Build the query string (StringBuilder)            │
│  ✋ Track parameter values (parameters list)          │
│  ✋ Fill ? placeholders in correct order              │
│  ✋ Execute the query                                 │
│  ✋ Map Object[] result to DTO manually               │
│  ✋ Handle pagination & sorting yourself              │
│     (covered in next step)                           │
└──────────────────────────────────────────────────────┘
```

And on top of all this — it's still **database-dependent**. You're writing plain SQL, so switching databases could break things.

This is exactly why the instructor later introduces **Criteria API** — which solves both problems: dynamic queries + database independence. But first, let's see how pagination and sorting work in Native SQL.

---
# Step 5 — Pagination & Sorting in Native SQL

---

## Two Ways to Do Pagination & Sorting in Native SQL

The instructor covers two approaches here, and they map directly to the two styles of writing native queries we've already seen:

```
┌─────────────────────────────────────────────────────────┐
│         PAGINATION & SORTING — TWO APPROACHES           │
│                                                         │
│  Way 1 → Dynamic Native Query (EntityManager +          │
│           StringBuilder)                                │
│           You manually append ORDER BY, LIMIT, OFFSET   │
│                                                         │
│  Way 2 → Static Native Query (@Query annotation)        │
│           Just pass a Pageable parameter —              │
│           JPA handles everything automatically          │
└─────────────────────────────────────────────────────────┘
```

---

## Way 1 — Pagination & Sorting in Dynamic Native Query

This is a continuation of Step 4's approach. After you've built your WHERE clause dynamically, you just **keep appending** to the same StringBuilder.

### Sorting — Append ORDER BY

```java
// After all dynamic WHERE conditions are added:
queryBuilder.append("ORDER BY ").append("ud.user_name").append(" DESC");
```

That's it. Since your query is just a String, sorting is just appending the ORDER BY clause. Nothing special — plain SQL.

### Pagination — Append LIMIT and OFFSET

In plain SQL, pagination is done with `LIMIT` (how many records per page) and `OFFSET` (where to start from).

```java
int size = 5;   // records per page
int page = 0;   // page number (0 = first page)

queryBuilder.append(" LIMIT ? OFFSET ? ");
parameters.add(size);
parameters.add(page * size);
```

These two `?` placeholders go into the same parameters list you've been maintaining. They'll get filled in the same loop that fills all other `?` placeholders.

Here's how LIMIT and OFFSET translate to pages:

```
┌──────────────────────────────────────────────────────────────────┐
│              HOW LIMIT & OFFSET WORK                             │
│                                                                  │
│  Total records: 10  │  Page size (LIMIT): 3                      │
│                                                                  │
│  page=0 → OFFSET = 0*3 = 0  → records 1, 2, 3                    │
│  page=1 → OFFSET = 1*3 = 3  → records 4, 5, 6                    │
│  page=2 → OFFSET = 2*3 = 6  → records 7, 8, 9                    │
│  page=3 → OFFSET = 3*3 = 9  → records 10                         │
│                                                                  │
│  Formula: OFFSET = page * size                                   │
└──────────────────────────────────────────────────────────────────┘
```

### The Complete Way 1 Code

```java
public List<UserDTO> getUserDetailsByNameNativeQuery(String userName) {

    // Base query
    StringBuilder queryBuilder = new StringBuilder(
        "SELECT ud.user_name AS user_name, ud.phone AS phone, ua.city AS city "
    );
    queryBuilder.append("FROM user_details ud ");
    queryBuilder.append("JOIN user_address ua ON ud.user_address_id = ua.id ");
    queryBuilder.append("WHERE 1=1 ");

    List<Object> parameters = new ArrayList<>();

    // Dynamic conditions
    if (userName != null && !userName.isEmpty()) {
        queryBuilder.append("AND ud.user_name = ? ");
        parameters.add(userName);
    }

    // Sorting — append after WHERE clause
    queryBuilder.append("ORDER BY ").append("ud.user_name").append(" DESC ");

    // Pagination — append after ORDER BY
    int size = 5;
    int page = 0;
    queryBuilder.append("LIMIT ? OFFSET ? ");
    parameters.add(size);
    parameters.add(page * size);

    // Create query
    Query nativeQuery = entityManager.createNativeQuery(queryBuilder.toString());

    // Fill all ? placeholders (conditions + pagination)
    for (int i = 0; i < parameters.size(); i++) {
        nativeQuery.setParameter(i + 1, parameters.get(i));
    }

    // Execute and map
    List<Object[]> result = nativeQuery.getResultList();
    return UserDTO.mapResultToDTO(result);
}
```

Notice that the pagination `?` values go into the **same parameters list** as your filter conditions. The loop at the end fills all of them in order — filter values first, then size, then offset.

```
┌──────────────────────────────────────────────────────────────────┐
│           PARAMETERS LIST — ALL ? FILLED IN ORDER                │
│                                                                  │
│  Query:  WHERE 1=1 AND ud.user_name = ?                          │
│                       LIMIT ?  OFFSET ?                          │
│                          ▲        ▲        ▲                     │
│                       pos 1    pos 2    pos 3                    │
│                                                                  │
│  parameters list: [ "John",    5,       0  ]                     │
│                    index 0   index 1  index 2                    │
│                                                                  │
│  Loop:                                                           │
│    i=0 → setParameter(1, "John")  → fills filter condition       │
│    i=1 → setParameter(2, 5)       → fills LIMIT                  │
│    i=2 → setParameter(3, 0)       → fills OFFSET                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## Way 2 — Pagination & Sorting via `@Query` + `Pageable`

This is the much simpler approach — and it works exactly the same way as pagination in JPQL. The instructor reminds you that since we already covered this in the JPQL lecture, nothing is different here. Just add `nativeQuery = true`.

### Repository — just add a `Pageable` parameter

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {

    @Query(value = "SELECT * FROM user_details ud WHERE ud.user_name = :userName",
           nativeQuery = true)
    List<UserDetails> getUserDetailsByNameNativeQuery(
        @Param("userName") String userName, 
        Pageable pageable   // just add this parameter
    );
}
```

You don't write LIMIT or OFFSET anywhere in the query. You don't write ORDER BY either. JPA takes care of appending all of that automatically based on the `Pageable` object you pass in.

### Service — create and pass a `Pageable` object

```java
public List<UserDetails> getUserDetailsByNameNativeQuery(String name) {

    Pageable pageableObj = PageRequest.of(
        0,                          // page number (0 = first page)
        5,                          // page size (5 records per page)
        Sort.by("phone").descending() // sort by phone, descending
    );

    return userDetailsRepository.getUserDetailsByNameNativeQuery(name, pageableObj);
}
```

That's all. One object carries page number, page size, and sort information — and JPA translates it all into the right SQL automatically.

### What JPA generates behind the scenes

When you pass a `Pageable` object, Hibernate generates the full SQL for you:

```sql
SELECT *
FROM user_details ud
WHERE ud.user_name = ?
ORDER BY ud.phone DESC
FETCH FIRST ? ROWS ONLY
```

You wrote none of that pagination/sorting SQL — JPA handled it completely.

---

## Side by Side — Way 1 vs Way 2

```
┌─────────────────────────┬──────────────────────────┬───────────────────────────┐
│                         │  Way 1                   │  Way 2                    │
│                         │  StringBuilder +         │  @Query + Pageable        │
│                         │  EntityManager           │                           │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Query is dynamic?       │ Yes — conditions built   │ No — query is fixed       │
│                         │ at runtime               │ at compile time           │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Sorting                 │ Manually append          │ Pass Sort in              │
│                         │ ORDER BY to string       │ PageRequest.of()          │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Pagination              │ Manually append          │ Pass page & size in       │
│                         │ LIMIT ? OFFSET ?         │ PageRequest.of()          │
│                         │ add to parameters list   │                           │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Who writes the SQL?     │ You write everything     │ JPA generates             │
│                         │                          │ pagination SQL            │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Code effort             │ More manual work         │ Very clean, minimal code  │
├─────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Best for                │ Queries with runtime     │ Fixed queries that just   │
│                         │ conditions               │ need pagination & sorting │
└─────────────────────────┴──────────────────────────┴───────────────────────────┘
```

---

## The Correct Order to Append Things (Way 1) — Important ⚠️

The instructor implicitly follows this order in his code. Always append in this sequence:

```
┌──────────────────────────────────────────────────┐
│         CORRECT QUERY BUILDING ORDER             │
│                                                  │
│  1. SELECT ... FROM ...                          │
│  2. JOIN (if any)                                │
│  3. WHERE 1=1                                    │
│  4. AND condition1 (if present)                  │
│  5. AND condition2 (if present)                  │
│       ... more conditions                        │
│  6. ORDER BY field DESC/ASC                      │
│  7. LIMIT ? OFFSET ?                             │
│                                                  │
│  Parameters list follows the same order:         │
│  [ filter values..., size, page*size ]           │
└──────────────────────────────────────────────────┘
```

If you mix up the order — like putting LIMIT before ORDER BY — you'll get a SQL syntax error.

---

## Where We Are Now — The Bigger Picture

```
┌──────────────────────────────────────────────────────────────────┐
│                    WHAT WE'VE COVERED SO FAR                     │
│                                                                  │
│  ✅ Native Query — what it is & when to use it                    │
│  ✅ Basic syntax — nativeQuery = true                             │
│  ✅ Full field mapping — SELECT * → JPA auto-maps                 │
│  ✅ Partial field mapping — two solutions                         │
│  ✅ Dynamic Native Query — StringBuilder + EntityManager          │
│  ✅ Pagination & Sorting — both ways                              │
│                                                                  │
│  ❓ Remaining problem:                                            │
│     Dynamic queries are powerful but DB-dependent.               │
│     What if we want dynamic + DB-independent + type-safe?        │
│                                                                  │
│  → This is exactly why Criteria API exists                       │
│  → That's what we cover next                                     │
└──────────────────────────────────────────────────────────────────┘
```

---
# Step 6 — Criteria API: Why It Exists & The Hierarchy

---

## The Problem That Leads to Criteria API

At this point in the lecture, the instructor pauses and asks a very natural question that any developer would ask:

> *"Hey, we have JPQL, but Native Query feels much more powerful — especially since I can write dynamic queries. Dynamic queries are very important in the industry because almost nothing is static. So why would I ever use JPQL again? Should I just always use Native Query?"*

This is a genuinely good question. And the answer is: **Native Query has two problems that JPQL doesn't have.**

```
┌──────────────────────────────────────────────────────────────────┐
│              TWO PROBLEMS WITH NATIVE QUERY                      │
│                                                                  │
│  Problem 1 — DB Dependent                                        │
│  You're writing plain SQL. If tomorrow you switch from           │
│  MySQL to PostgreSQL, your queries may break.                    │
│  Code changes required. JPQL is DB-independent                   │
│  because JPA translates it for whatever DB you use.              │
│                                                                  │
│  Problem 2 — Not Type Safe                                       │
│  You're writing everything as a raw String.                      │
│  If you make a typo — like writing "ANDD" instead of "AND"       │
│  or wrong column name — you won't find out until                 │
│  runtime. No compile-time safety at all.                         │
└──────────────────────────────────────────────────────────────────┘
```

The instructor gives a very relatable example for the type-safety problem:

> *"Here everything is a string. If you wanted to write AND but wrote AMD instead — you won't get any error at compile time. You'll only find out when the query runs and blows up."*

So the ideal solution needs to be:
- **Dynamic** — conditions built at runtime ✅
- **DB Independent** — works no matter what database you use ✅
- **Type Safe** — errors caught at compile time, not runtime ✅
- **JPA Managed** — caching, lifecycle, persistence context all work ✅

**Criteria API gives you all four of these.**

---

## Where Each Tool Stands

```
┌──────────────────────────────────────────────────────────────────────┐
│                    TOOL COMPARISON — FULL PICTURE                    │
│                                                                      │
│              │ DB         │ Dynamic  │ Type  │ JPA      │ Caching    │
│              │ Independent│ Queries  │ Safe  │ Managed  │            │
│──────────────┼────────────┼──────────┼───────┼──────────┼─────────── │
│ JPQL         │     ✅      │    ❌     │  ✅    │    ✅     │    ✅       │
│──────────────┼────────────┼──────────┼───────┼──────────┼─────────── │
│ Native Query │     ❌      │    ✅     │  ❌    │    ❌     │    ❌       │
│──────────────┼────────────┼──────────┼───────┼──────────┼─────────── │
│ Criteria API │     ✅      │    ✅     │  ✅    │    ✅     │    ✅       │
└──────────────┴────────────┴──────────┴───────┴──────────┴────────────┘

Criteria API = the best of JPQL + dynamic query capability of Native Query
```

---

## What is Criteria API?

Criteria API is a JPA feature that lets you **build queries using Java objects and method calls** instead of writing query strings. Everything is an object — the table, the conditions, the joins — so the compiler can catch mistakes before your code even runs.

The instructor's exact words:

> *"It allows you to build dynamic, type-safe queries without writing raw SQL. You don't have to write raw SQL where you might make a mistake. Here everything is dealt with as objects — so syntax-wise you don't have to worry."*

---

## Understanding Criteria API Through Its Hierarchy

The instructor says this clearly:

> *"Sometime some engineers feel that Criteria API is very difficult to understand, but I feel it is easy if you understand it through the hierarchy."*

So let's build that hierarchy carefully. Everything in Criteria API flows through **three main building blocks**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CRITERIA API — FULL HIERARCHY                       │
│                                                                         │
│                                                                         │
│   ┌─────────────────┐                                                   │
│   │ CriteriaBuilder │  ◄── Created from EntityManager                   │
│   │                 │      entityManager.getCriteriaBuilder()           │
│   │  "The Factory"  │                                                   │
│   │                 │      Used to CREATE everything below              │
│   └────────┬────────┘                                                   │
│            │                                                            │
│            │ creates                                                    │
│            ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐       │
│   │                     CriteriaQuery                           │       │
│   │              "Defines the query structure"                  │       │
│   │                                                             │       │
│   │   .from()         → FROM clause (which table/entity)        │       │
│   │   .join()         → JOIN another entity                     │       │
│   │   .select()       → SELECT * (full entity)                  │       │
│   │   .multiselect()  → SELECT specific fields                  │       │
│   │   .where()        → WHERE clause (uses Predicate)           │       │
│   │   .orderBy()      → ORDER BY (sorting)                      │       │
│   │   .groupBy()      → GROUP BY                                │       │
│   │   .having()       → HAVING clause                           │       │
│   │                                                             │       │
│   │   Predicate  ◄── conditions built via CriteriaBuilder       │       │
│   │   .and()     ◄── combine conditions with AND                │       │
│   │   .or()      ◄── combine conditions with OR                 │       │
│   │   .not()     ◄── negate a condition                         │       │
│   └─────────────────────────────────────────────────────────────┘       │
│            │                                                            │
│            │ passed into EntityManager.createQuery()                    │
│            ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐       │
│   │                       TypedQuery                            │       │
│   │                  "Executes the query"                       │       │
│   │                                                             │       │
│   │   .getResultList()    → fetch multiple rows                 │       │
│   │   .getSingleResult()  → fetch exactly one row               │       │
│   │   .setFirstResult()   → pagination offset (page number)     │       │
│   │   .setMaxResults()    → pagination limit (page size)        │       │
│   └─────────────────────────────────────────────────────────────┘       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## The Three Steps — Always in This Order

No matter what query you write with Criteria API, you always follow the same three steps:

```
┌──────────────────────────────────────────────────────────────────┐
│           CRITERIA API — ALWAYS FOLLOW THIS ORDER                │
│                                                                  │
│  Step 1 → Get CriteriaBuilder from EntityManager                 │
│           CriteriaBuilder cb =                                   │
│               entityManager.getCriteriaBuilder();                │
│                                                                  │
│  Step 2 → Use CriteriaBuilder to create CriteriaQuery            │
│           Define what each row looks like:                       │
│           → Full entity?   cb.createQuery(UserDetails.class)     │
│           → Partial fields? cb.createQuery(Object[].class)       │
│                                                                  │
│  Step 3 → Build the query structure on CriteriaQuery             │
│           → .from() → .select() → .where() → .orderBy()          │
│                                                                  │
│  Step 4 → Create TypedQuery from EntityManager                   │
│           TypedQuery<X> query =                                  │
│               entityManager.createQuery(crQuery);                │
│                                                                  │
│  Step 5 → Execute via TypedQuery                                 │
│           query.getResultList()                                  │
│           or with pagination:                                    │
│           query.setFirstResult(0).setMaxResults(5)               │
│               .getResultList()                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## One Key Concept — Root

The instructor introduces something called **Root** when explaining `.from()`. It's worth calling out separately because it comes up everywhere in Criteria API code.

```
┌──────────────────────────────────────────────────────────────────┐
│                       WHAT IS ROOT?                              │
│                                                                  │
│  When you write:                                                 │
│  Root<UserDetails> user = crQuery.from(UserDetails.class);       │
│                                                                  │
│  'user' represents your PRIMARY table in the query.              │
│  It is always called the "Root" — the first/main table.          │
│                                                                  │
│  From this root you can:                                         │
│  → Access fields:  user.get("phone"), user.get("name")           │
│  → Do joins:       user.join("userAddress", JoinType.INNER)      │
│                                                                  │
│  If you join another table:                                      │
│  Join<UserDetails, UserAddress> address =                        │
│      user.join("userAddress", JoinType.INNER);                   │
│                                                                  │
│  Now 'address' represents the joined table.                      │
│  You access its fields via: address.get("city")                  │
│                                                                  │
│  Root = first/main table                                         │
│  Join = any table joined to the root                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## What `createQuery()` Type Tells JPA

This is something the instructor is very specific about. When you create a CriteriaQuery, you tell JPA **what each row of the result will look like**:

```
┌──────────────────────────────────────────────────────────────────┐
│         createQuery() TYPE — WHAT IT MEANS                       │
│                                                                  │
│  cb.createQuery(UserDetails.class)                               │
│  → Each row is a complete UserDetails entity                     │
│  → Use when doing SELECT * (all fields)                          │
│  → Pair with: crQuery.select(user)                               │
│                                                                  │
│  cb.createQuery(Object[].class)                                  │
│  → Each row is a mixed array of values                           │
│  → Use when doing SELECT specific fields                         │
│  → Pair with: crQuery.multiselect(field1, field2, ...)           │
│  → You manually map Object[] to DTO afterward                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Why the Instructor Calls CriteriaBuilder "The Factory"

Everything in Criteria API is created through `CriteriaBuilder`. It's the single entry point for building any part of your query:

```
┌──────────────────────────────────────────────────────────────────┐
│           CRITERIABUILDER — WHAT IT CREATES                      │
│                                                                  │
│   entityManager.getCriteriaBuilder()                             │
│            │                                                     │
│            ├──► CriteriaQuery  (the query structure)             │
│            │                                                     │
│            ├──► Predicates     (the WHERE conditions)            │
│            │    cb.equal()                                       │
│            │    cb.notEqual()                                    │
│            │    cb.gt() / cb.ge() / cb.lt() / cb.le()            │
│            │    cb.like() / cb.notLike()                         │
│            │    cb.and() / cb.or() / cb.not()                    │
│            │    cb.in()                                          │
│            │                                                     │
│            └──► Order          (for sorting)                     │
│                 cb.desc(field)                                   │
│                 cb.asc(field)                                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Where We Are — Ready for Code

Now that the hierarchy is clear, writing any Criteria API query becomes a matter of following the same structure every single time. The instructor says it plainly:

> *"If you understand this hierarchy, then Criteria API is very simple — whatever methods you need to invoke, you will find them in the right place."*

In the next step, we'll take this hierarchy and apply it to real code — covering select all fields, select specific fields, joins, and pagination & sorting.

---
# Step 7 — Criteria API: Full Code Walkthrough

---

## Scenario 1 — SELECT * (All Fields)

This is the simplest case. You want all fields from one table, with a WHERE condition applied dynamically.

### The Entity

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "user_name")
    private String name;

    private Long phone;

    // getters and setters
}
```

### The Service Method

```java
public List<UserDetails> getUserDetailsByPhoneCriteriaAPI(Long phoneNo) {

    // Step 1: Get CriteriaBuilder
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // Step 2: Create CriteriaQuery
    // Each row = full UserDetails entity → UserDetails.class
    CriteriaQuery<UserDetails> crQuery = cb.createQuery(UserDetails.class);

    // Step 3: FROM clause — first/main table, known as Root
    Root<UserDetails> user = crQuery.from(UserDetails.class);

    // Step 4: SELECT * — select all fields of UserDetails
    crQuery.select(user);

    // Step 5: WHERE clause — build condition using Predicate
    Predicate predicate = cb.equal(user.get("phone"), phoneNo);
    crQuery.where(predicate);

    // Step 6: Create TypedQuery and execute
    TypedQuery<UserDetails> query = entityManager.createQuery(crQuery);
    List<UserDetails> output = query.getResultList();

    return output;
}
```

### What Hibernate Actually Generates

When you run this, Hibernate translates your Java object calls into this SQL:

```sql
SELECT
    ud1_0.user_id,
    ud1_0.user_name,
    ud1_0.phone
FROM
    user_details ud1_0
WHERE
    ud1_0.phone = ?
```

You wrote zero SQL. JPA built the entire query from your object calls.

### The Flow Visualized

```
┌─────────────────────────────────────────────────────────────────────┐
│              SCENARIO 1 — SELECT * FLOW                             │
│                                                                     │
│  entityManager.getCriteriaBuilder()                                 │
│            │                                                        │
│            ▼                                                        │
│  cb.createQuery(UserDetails.class)                                  │
│  → "each row = full UserDetails entity"                             │
│            │                                                        │
│            ▼                                                        │
│  crQuery.from(UserDetails.class)  → Root<UserDetails> user          │
│  → "fetch from user_details table"                                  │
│            │                                                        │
│            ▼                                                        │
│  crQuery.select(user)                                               │
│  → "SELECT * — all columns"                                         │
│            │                                                        │
│            ▼                                                        │
│  cb.equal(user.get("phone"), phoneNo)  → Predicate                  │
│  crQuery.where(predicate)                                           │
│  → "WHERE phone = ?"                                                │
│            │                                                        │
│            ▼                                                        │
│  entityManager.createQuery(crQuery)  → TypedQuery                   │
│            │                                                        │
│            ▼                                                        │
│  query.getResultList()  → List<UserDetails>                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Scenario 2 — SELECT Specific Fields (Partial / Multiselect)

Now you only want specific fields — say `name` and `phone`. This is where `multiselect` and `Object[]` come in.

```java
public List<UserDTO> getUserDetailsByPhoneCriteriaAPI(Long phoneNo) {

    // Step 1: Get CriteriaBuilder
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // Step 2: Create CriteriaQuery
    // Each row = mixed fields → Object[].class
    CriteriaQuery<Object[]> crQuery = cb.createQuery(Object[].class);

    // Step 3: FROM clause
    Root<UserDetails> user = crQuery.from(UserDetails.class);

    // Step 4: SELECT specific fields — multiselect instead of select
    crQuery.multiselect(user.get("name"), user.get("phone"));

    // Step 5: WHERE clause
    Predicate predicate = cb.equal(user.get("phone"), phoneNo);
    crQuery.where(predicate);

    // Step 6: Execute
    TypedQuery<Object[]> query = entityManager.createQuery(crQuery);
    List<Object[]> results = query.getResultList();

    // Step 7: Manual mapping — Object[] → UserDTO
    List<UserDTO> output = new ArrayList<>();
    for (Object[] row : results) {
        String name = (String) row[0];
        Long phone = (Long) row[1];
        output.add(new UserDTO(name, phone));
    }

    return output;
}
```

### Key Differences from Scenario 1

```
┌──────────────────────────────────────────────────────────────────┐
│         SELECT * vs SELECT Specific Fields                       │
│                                                                  │
│  SELECT *                  │  SELECT specific fields             │
│  ──────────────────────────┼──────────────────────────────────   │
│  createQuery(              │  createQuery(                       │
│    UserDetails.class)      │    Object[].class)                  │
│                            │                                     │
│  crQuery.select(user)      │  crQuery.multiselect(               │
│                            │    user.get("name"),                │
│                            │    user.get("phone"))               │
│                            │                                     │
│  TypedQuery<UserDetails>   │  TypedQuery<Object[]>               │
│                            │                                     │
│  No manual mapping needed  │  Manual mapping required            │
│  JPA maps to entity        │  Object[0] → name                   │
│  automatically             │  Object[1] → phone                  │
└──────────────────────────────────────────────────────────────────┘
```

### Important — Field Names in Criteria API

The instructor is very clear about this. When you write `user.get("phone")` or `user.get("name")`, you use the **Java entity field name**, NOT the DB column name:

```
┌──────────────────────────────────────────────────────────────────┐
│                WHICH NAME TO USE IN user.get()                   │
│                                                                  │
│  Entity field:  private String name;                             │
│  DB column:     user_name                                        │
│                                                                  │
│  In Criteria API → user.get("name")   ✅                          │
│                 → user.get("user_name") ❌                        │
│                                                                  │
│  Criteria API works with entities, not DB tables directly.       │
│  So always use the Java field name.                              │
│                                                                  │
│  This is the OPPOSITE of Native Query where you use DB names.    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Scenario 3 — JOIN Between Two Entities

Now you want data from two related tables — `user_details` joined with `user_address`.

```java
public List<UserDTO> getUserDetailsByPhoneCriteriaAPI(Long phoneNo) {

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // Each row = mixed fields from two tables → Object[].class
    CriteriaQuery<Object[]> crQuery = cb.createQuery(Object[].class);

    // Root = first/main table
    Root<UserDetails> user = crQuery.from(UserDetails.class);

    // JOIN — use the entity field name "userAddress", not the table name
    Join<UserDetails, UserAddress> address = 
        user.join("userAddress", JoinType.INNER);

    // SELECT fields from both tables
    crQuery.multiselect(
        user.get("name"),       // from user_details
        address.get("city")     // from user_address
    );

    // WHERE clause
    Predicate predicate = cb.equal(user.get("phone"), phoneNo);
    crQuery.where(predicate);

    // Execute
    TypedQuery<Object[]> query = entityManager.createQuery(crQuery);
    List<Object[]> results = query.getResultList();

    // Map results
    List<UserDTO> output = new ArrayList<>();
    for (Object[] row : results) {
        String name = (String) row[0];
        String city = (String) row[1];
        output.add(new UserDTO(name, city));
    }

    return output;
}
```

### How the JOIN Works

```
┌──────────────────────────────────────────────────────────────────────┐
│                    HOW JOIN WORKS IN CRITERIA API                    │
│                                                                      │
│  Entity UserDetails has:                                             │
│  @OneToOne(cascade = CascadeType.ALL)                                │
│  private UserAddress userAddress;   ← field name is "userAddress"    │
│                                                                      │
│  In Criteria API:                                                    │
│  user.join("userAddress", JoinType.INNER)                            │
│          ▲                    ▲                                      │
│     Java field name      join type:                                  │
│     from entity          INNER / LEFT / RIGHT                        │
│                                                                      │
│  JPA already knows the relationship between the two entities         │
│  via @OneToOne, so you don't need to write ON condition.             │
│  JPA figures out the join column automatically.                      │
│                                                                      │
│  Root  → user    → represents user_details table                     │
│  Join  → address → represents user_address table                     │
│                                                                      │
│  Access fields:                                                      │
│  user.get("name")    → user_details.name                             │
│  address.get("city") → user_address.city                             │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Scenario 4 — Predicates: All the Conditions You Can Build

The instructor goes through all the types of conditions you can create using `CriteriaBuilder`. Here they are organized clearly:

### Comparison Operators

```
┌───────────────────────────────────┬──────────────────────────────┐
│  Criteria API Method              │  Equivalent SQL              │
├───────────────────────────────────┼──────────────────────────────┤
│ cb.equal(user.get("phone"), 123)  │  WHERE phone = 123           │
│ cb.notEqual(user.get("phone"),123)│  WHERE phone <> 123          │
│ cb.gt(user.get("phone"), 123)     │  WHERE phone > 123           │
│ cb.ge(user.get("phone"), 123)     │  WHERE phone >= 123          │
│ cb.lt(user.get("phone"), 123)     │  WHERE phone < 123           │
│ cb.le(user.get("phone"), 123)     │  WHERE phone <= 123          │
└───────────────────────────────────┴──────────────────────────────┘
```

### Logical Operators — Combining Multiple Predicates

```java
// Create individual predicates
Predicate predicate1 = cb.equal(user.get("phone"), phoneNo);
Predicate predicate2 = cb.notEqual(user.get("name"), "AA");

// Combine them
Predicate finalPredicate = cb.and(predicate1, predicate2);

// Apply to query
crQuery.where(finalPredicate);
```

```
┌───────────────────────────────────────────┬──────────────────────────────────┐
│  Criteria API Method                      │  Equivalent SQL                  │
├───────────────────────────────────────────┼──────────────────────────────────┤
│ cb.and(predicate1, predicate2)            │  WHERE cond1 AND cond2           │
│ cb.or(predicate1, predicate2)             │  WHERE cond1 OR cond2            │
│ cb.not(predicate1)                        │  WHERE NOT cond1                 │
└───────────────────────────────────────────┴──────────────────────────────────┘
```

### String Operations

```
┌────────────────────────────────────────┬──────────────────────────────────┐
│  Criteria API Method                   │  Equivalent SQL                  │
├────────────────────────────────────────┼──────────────────────────────────┤
│ cb.like(user.get("name"), "S%")        │  WHERE name LIKE 'S%'            │
│ cb.notLike(user.get("name"), "S%")     │  WHERE name NOT LIKE 'S%'        │
└────────────────────────────────────────┴──────────────────────────────────┘
```

### Collection Operations (IN / NOT IN)

```
┌─────────────────────────────────────────────────┬────────────────────────────┐
│  Criteria API Method                            │  Equivalent SQL            │
├─────────────────────────────────────────────────┼────────────────────────────┤
│ cb.in(user.get("phone")).value(11).value(7)     │  WHERE phone IN (11, 7)    │
│ cb.not(user.get("phone").in(11, 7))             │  WHERE phone NOT IN (11,7) │
└─────────────────────────────────────────────────┴────────────────────────────┘
```

---

## Scenario 5 — Pagination & Sorting

This is the cleanest part of Criteria API. Sorting goes on `CriteriaQuery`, and pagination goes on `TypedQuery`.

```java
public List<UserDetails> getUserDetailsByPhoneCriteriaAPI(Long phoneNo) {

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    CriteriaQuery<UserDetails> crQuery = cb.createQuery(UserDetails.class);

    Root<UserDetails> user = crQuery.from(UserDetails.class);

    crQuery.select(user);

    // WHERE
    Predicate predicate = cb.equal(user.get("phone"), phoneNo);
    crQuery.where(predicate);

    // SORTING — goes on CriteriaQuery
    crQuery.orderBy(cb.desc(user.get("name")));   // ORDER BY name DESC
    // for ascending: cb.asc(user.get("name"))

    // Create TypedQuery
    TypedQuery<UserDetails> query = entityManager.createQuery(crQuery);

    // PAGINATION — goes on TypedQuery
    query.setFirstResult(0);   // offset — page 0 = start from record 0
    query.setMaxResults(5);    // limit  — fetch max 5 records

    List<UserDetails> results = query.getResultList();
    return results;
}
```

### Where Sorting and Pagination Live

```
┌──────────────────────────────────────────────────────────────────┐
│         SORTING vs PAGINATION — WHERE EACH BELONGS               │
│                                                                  │
│  CriteriaQuery                                                   │
│  └──► .orderBy(cb.desc(user.get("name")))                        │
│        → Sorting is part of query STRUCTURE                      │
│        → So it belongs on CriteriaQuery                          │
│                                                                  │
│  TypedQuery                                                      │
│  └──► .setFirstResult(0)    → page offset                        │
│  └──► .setMaxResults(5)     → page size                          │
│        → Pagination is about query EXECUTION control             │
│        → So it belongs on TypedQuery                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Complete Picture — All Scenarios Together

```
┌──────────────────────────────────────────────────────────────────────┐
│               CRITERIA API — ALL SCENARIOS AT A GLANCE               │
│                                                                      │
│  SELECT * (all fields)                                               │
│  → createQuery(Entity.class)                                         │
│  → crQuery.select(root)                                              │
│  → TypedQuery<Entity>                                                │
│  → No manual mapping                                                 │
│                                                                      │
│  SELECT specific fields                                              │
│  → createQuery(Object[].class)                                       │
│  → crQuery.multiselect(field1, field2)                               │
│  → TypedQuery<Object[]>                                              │
│  → Manual mapping to DTO                                             │
│                                                                      │
│  JOIN                                                                │
│  → root.join("entityFieldName", JoinType.INNER/LEFT/RIGHT)           │
│  → Access joined table fields via join variable                      │
│  → No ON condition needed — JPA knows the relationship               │
│                                                                      │
│  WHERE conditions (Predicates)                                       │
│  → Built via CriteriaBuilder methods                                 │
│  → Combined with cb.and() / cb.or() / cb.not()                       │
│  → Applied via crQuery.where(predicate)                              │
│                                                                      │
│  Sorting                                                             │
│  → crQuery.orderBy(cb.desc/asc(root.get("field")))                   │
│                                                                      │
│  Pagination                                                          │
│  → query.setFirstResult(offset)                                      │
│  → query.setMaxResults(limit)                                        │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Dynamic Conditions in Criteria API — The Real Power

Just like Dynamic Native Query, you can make Criteria API conditions dynamic too. Since everything is just Java code, adding or skipping conditions is as natural as an if-statement:

```java
public List<UserDetails> getUserDetailsDynamic(Long phoneNo, String name) {

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<UserDetails> crQuery = cb.createQuery(UserDetails.class);
    Root<UserDetails> user = crQuery.from(UserDetails.class);
    crQuery.select(user);

    // Build conditions dynamically
    List<Predicate> predicates = new ArrayList<>();

    if (phoneNo != null) {
        predicates.add(cb.equal(user.get("phone"), phoneNo));
    }

    if (name != null && !name.isEmpty()) {
        predicates.add(cb.like(user.get("name"), "%" + name + "%"));
    }

    // Combine all active conditions with AND
    crQuery.where(cb.and(predicates.toArray(new Predicate[0])));

    TypedQuery<UserDetails> query = entityManager.createQuery(crQuery);
    return query.getResultList();
}
```

No StringBuilder. No string concatenation. No `?` management. Just clean Java objects — and the compiler catches any mistakes.

---

## Criteria API vs Dynamic Native Query — The Final Comparison

```
┌───────────────────────────┬───────────────────────┬───────────────────────┐
│                           │  Dynamic Native Query │  Criteria API         │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ Dynamic conditions?       │ ✅ Yes                 │ ✅ Yes                 │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ DB Independent?           │ ❌ No                  │ ✅ Yes                 │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ Type Safe?                │ ❌ No (raw strings)    │ ✅ Yes (Java objects)  │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ JPA Managed?              │ ❌ No                  │ ✅ Yes                 │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ Caching?                  │ ❌ No                  │ ✅ Yes                 │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ Code effort               │ High — StringBuilder  │ Moderate — but all    │
│                           │ + parameter list      │ Java objects          │
├───────────────────────────┼───────────────────────┼───────────────────────┤
│ Error detection           │ Runtime only          │ Compile time ✅        │
└───────────────────────────┴───────────────────────┴───────────────────────┘
```

---

That completes the full Criteria API code walkthrough — all scenarios the instructor covered, organized clearly. This wraps up the core content of the lecture.

Would you like a **final summary / cheat sheet** that brings everything from all 7 steps together in one place?