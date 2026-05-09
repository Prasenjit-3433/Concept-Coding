# Step 1 — One-to-Many: Unidirectional

---

## What problem are we solving?

Before jumping into code, the instructor wants you to **think logically** first.

Say you have two tables: **User** and **Order**.
The relationship is: **one user can have many orders**.

Now the question is — *how do you store this relationship in the database?*

Look at the User table:

```
USER TABLE
| user_id | name  | phone |
|---------|-------|-------|
|    1    | John  | 9999  |
|    2    | Alice | 8888  |
```

User 1 has Order 1, Order 2, Order 3.
Can you store all three order IDs inside that single User row? **No.** One row can't hold multiple values in one column cleanly.

So there are **two ways** to handle this:

---

## The Two Ways

### Way 1 — Create a new (join) table
A separate table that maps user to orders:

```
USER_DETAILS_ORDER_DETAILS (new table)
| user_details_id | order_details_id |
|-----------------|-----------------|
|        1        |        1        |
|        1        |        2        |
|        1        |        3        |
```

### Way 2 — Store the foreign key inside the Order table itself
Since one order belongs to only one user, you can just add a `user_id` column inside the Order table:

```
ORDER TABLE
| order_id | product_name | user_id_fk |
|----------|-------------|------------|
|    1     |  Ice Cream  |     1      |
|    2     |  Cold Drink |     1      |
|    3     |  Burger     |     1      |
```

---

## What does JPA do by default?

**JPA creates a new table (Way 1) by default.**

When you just write `@OneToMany` without anything else, JPA automatically creates a third table like `user_details_order_details` to store the mapping.

```
USER TABLE          ORDER TABLE
| id | name | phone |    | id | product_name |
```
```
USER_DETAILS_ORDER_DETAILS  ← JPA creates this automatically
| order_details_id | user_details_id |
```

---

## The Better Way — Use `@JoinColumn`

The instructor's recommendation is **Way 2** for One-to-Many.
Storing the foreign key directly inside the child table (Order table) is simpler, more readable, and faster to query. You avoid maintaining an extra table.

To tell JPA *"don't create a new table, instead put the foreign key in the child table"*, you use `@JoinColumn`.

---

## The Entities

### UserDetails (Parent)

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String phone;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id_fk", referencedColumnName = "userId")
    private List<OrderDetails> orderDetails = new ArrayList<>();

    // Constructors, getters, setters
}
```

### OrderDetails (Child)

```java
@Table(name = "order_details")
@Entity
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    // getters, setters
}
```

---

## Breaking down `@JoinColumn`

```java
@JoinColumn(name = "user_id_fk", referencedColumnName = "userId")
```

| Attribute | What it means |
|---|---|
| `name` | The name of the new column that will be created **inside the Order table** to store the foreign key |
| `referencedColumnName` | Which column in the **User table** this foreign key points to (here it's `userId`) |

So after this, your tables look like:

```
USER TABLE                    ORDER TABLE
| userId | name | phone |     | id | product_name | user_id_fk |
|--------|------|-------|     |----|--------------|------------|
|   1    | John | 9999  |     |  1 |  Ice Cream   |     1      |
                              |  2 |  Cold Drink  |     1      |
```

Clean. No extra table. The Order table itself carries the foreign key pointing back to its parent User.

---

## Full Picture — What's happening end to end

```
POST /api/user
{
  "name": "JohnXYZ",
  "phone": "1234567890",
  "orderDetails": [
    { "productName": "IceCream" },
    { "productName": "ColdDrinks" }
  ]
}
```

Because of `cascade = CascadeType.ALL`:
- JPA inserts the User first → gets `userId = 1`
- Then inserts both Orders → sets `user_id_fk = 1` in each Order row automatically

```
USER TABLE
| userId | name    | phone      |
|--------|---------|------------|
|   1    | JohnXYZ | 1234567890 |

ORDER TABLE
| id | user_id_fk | product_name |
|----|------------|--------------|
|  1 |     1      |   IceCream   |
|  2 |     1      |  ColdDrinks  |
```

Exactly what we wanted — no extra table, foreign key sits neatly in the child.

---

## Key Takeaways from Step 1

- One-to-Many means **one parent, many children**. You can't store multiple child IDs in one parent row.
- JPA's **default behavior** is to create a **new join table**.
- Use `@JoinColumn` to override this and store the **foreign key inside the child table** instead.
- The instructor says this is the **better approach for One-to-Many** — extra tables should generally only be created for **Many-to-Many**.
- `@JoinColumn(name=..., referencedColumnName=...)` — `name` is the FK column in the child, `referencedColumnName` is the PK column in the parent.

---
# Step 2 — Lazy vs Eager Loading in One-to-Many

---

## What is Lazy Loading?

The instructor already covered Lazy and Eager loading in a previous video, but here's a clean recap in the context of One-to-Many.

**By default, One-to-Many is LAZY.**

This means — when you fetch a User from the database, JPA does **not** automatically fetch its Orders. It only fetches Orders when you **explicitly ask for them** in your code.

Think of it like this:

```
LAZY Loading:
─────────────────────────────────────────────────
Step 1: Fetch User       →  SELECT * FROM user_details WHERE id = ?
                            (Only user data comes. Orders? Not yet.)

Step 2: You access       →  SELECT * FROM order_details WHERE user_id_fk = ?
        getOrderDetails()   (NOW orders are fetched. Only when asked.)
─────────────────────────────────────────────────
```

vs.

```
EAGER Loading:
─────────────────────────────────────────────────
Step 1: Fetch User       →  SELECT * FROM user_details
                            LEFT JOIN order_details
                            ON user_details.id = order_details.user_id_fk
                            WHERE user_id = ?
                            (User + ALL orders fetched in one shot.)
─────────────────────────────────────────────────
```

---

## Why does the instructor use a DTO to demonstrate Lazy Loading?

This is a really important point the instructor makes.

If you directly return the `UserDetails` entity from your controller like this:

```java
@GetMapping("/user/{id}")
public UserDetails fetchUser(@PathVariable Long id) {
    return userDetailsService.findByID(id);  // directly returning entity
}
```

You will still get the Orders in the response — even though it's Lazy. **Why?**

Because when Spring converts your response to JSON, it uses **Jackson** internally. Jackson calls `getOrderDetails()` on your entity automatically during serialization. That getter call **triggers the lazy load**.

So you'd be confused — *"I thought it was lazy, why did the orders get fetched?"*

That's exactly why the instructor creates a **DTO** — to show you **exactly at what point** the Order query fires, by printing a message just before accessing `getOrderDetails()`.

---

## The Code

### UserController

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @Autowired
    UserDetailsService userDetailsService;

    @PostMapping(path = "/user")
    public UserDetails insertUser(@RequestBody UserDetails userDetails) {
        return userDetailsService.saveUser(userDetails);
    }

    @GetMapping("/user/{id}")
    public UserDetailsDTO fetchUser(@PathVariable Long id) {

        // Step 1: Only User is fetched here. Orders? NOT YET.
        UserDetails output = userDetailsService.findByID(id);

        System.out.println("going to map UserDetails to UserDTO");

        // Step 2: DTO construction triggers the order fetch
        UserDetailsDTO userDTO = output.mapUserDetailsToUserDTO();

        return userDTO;
    }
}
```

### UserDetailsDTO

```java
public class UserDetailsDTO {

    private Long id;
    private String name;
    private String phone;
    private List<OrderDetails> orders;

    public UserDetailsDTO(UserDetails userDetails) {
        this.id = userDetails.getUserId();
        this.name = userDetails.getName();
        this.phone = userDetails.getPhone();

        System.out.println("going to query order table here now");

        // THIS LINE triggers the second SELECT query on order_details
        this.orders = userDetails.getOrderDetails();
    }

    // getters and setters
}
```

---

## What happens step by step at runtime

```
GET /api/user/1
       │
       ▼
findByID(1)
       │
       ▼
Hibernate fires:
SELECT user_id, name, phone
FROM user_details
WHERE user_id = 1
       │
       │   ← Only user data in memory. Orders not touched yet.
       ▼
Prints: "going to map UserDetails to UserDTO"
       │
       ▼
Enters DTO constructor
       │
       ▼
Prints: "going to query order table here now"
       │
       ▼
userDetails.getOrderDetails()  ← THIS triggers lazy load
       │
       ▼
Hibernate fires:
SELECT user_id_fk, id, product_name
FROM order_details
WHERE user_id_fk = 1
       │
       ▼
Orders are now loaded. DTO is built. Response sent.
```

So the console output looks like this in order:

```
[Hibernate] SELECT from user_details WHERE user_id = ?

going to map UserDetails to UserDTO

going to query order table here now

[Hibernate] SELECT from order_details WHERE user_id_fk = ?
```

This proves that the second query only fires **when you explicitly access the orders** — that's Lazy Loading.

---

## What if you don't need orders at all?

That's the whole point of Lazy loading. If your business logic doesn't need orders, you simply **don't call `getOrderDetails()`** — and the second SELECT never fires. You save an unnecessary DB call.

---

## Switching to Eager Loading

If you always need the orders whenever you fetch a user, switch to Eager:

```java
@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
@JoinColumn(name = "user_id_fk", referencedColumnName = "userId")
private List<OrderDetails> orderDetails = new ArrayList<>();
```

Now Hibernate fires a **single JOIN query** instead of two separate queries:

```sql
SELECT
    ud1_0.user_id,
    ud1_0.name,
    ud1_0.phone,
    od1_0.user_id_fk,
    od1_0.id,
    od1_0.product_name
FROM user_details ud1_0
LEFT JOIN order_details od1_0
    ON ud1_0.user_id = od1_0.user_id_fk
WHERE ud1_0.user_id = ?
```

Everything comes in **one shot**. No second query needed.

---

## Lazy vs Eager — When to use which?

```
┌─────────────────────────────────────────────────────┐
│  Use LAZY (default) when:                           │
│  - You don't always need child data                 │
│  - You want to avoid unnecessary DB calls           │
│  - Performance matters in large datasets            │
├─────────────────────────────────────────────────────┤
│  Use EAGER when:                                    │
│  - You ALWAYS need child data with the parent       │
│  - Simpler code matters more than query count       │
└─────────────────────────────────────────────────────┘
```

---

## Key Takeaways from Step 2

- One-to-Many is **Lazy by default** — child records are not fetched until you access them.
- Jackson's serialization **calls getters automatically** — so returning the entity directly can confuse you into thinking eager loading is happening.
- Use a **DTO** to clearly control and observe when the child query fires.
- Switching to `fetch = FetchType.EAGER` makes Hibernate use a **LEFT JOIN** and fetch everything in one query.
- Lazy = two queries, but only when needed. Eager = one JOIN query, always.

---
# Step 3 — Cascade Types & Orphan Removal

---

## Cascade Types — Quick Recap

The instructor already explained cascade types in the previous video (1-to-1), but here's a clean summary in the context of One-to-Many:

The core idea of cascade is — **whatever you do to the parent, do the same to the child automatically.**

```
┌──────────────────────┬────────────────────────────────────────────┐
│   Cascade Type       │   What happens                             │
├──────────────────────┼────────────────────────────────────────────┤
│ CascadeType.PERSIST  │ Save parent → child also gets saved        │
├──────────────────────┼────────────────────────────────────────────┤
│ CascadeType.MERGE    │ Update parent → child also gets updated    │
├──────────────────────┼────────────────────────────────────────────┤
│ CascadeType.REMOVE   │ Delete parent → child also gets deleted    │
├──────────────────────┼────────────────────────────────────────────┤
│ CascadeType.ALL      │ All of the above combined                  │
└──────────────────────┴────────────────────────────────────────────┘
```

In code:
```java
@OneToMany(cascade = CascadeType.ALL)
@JoinColumn(name = "user_id_fk", referencedColumnName = "userId")
private List<OrderDetails> orderDetails = new ArrayList<>();
```

With `CascadeType.ALL`:
- Create a user with orders → orders also get created
- Delete the user → all its orders also get deleted
- Update the user → associated orders also get updated

---

## Now the interesting part — Orphan Removal

This is where it gets really interesting. The instructor introduces a scenario that cascade alone **cannot handle.**

---

## The Problem Orphan Removal Solves

Imagine this situation:

```
USER 1
  ├── Order 1  (user_id_fk = 1)
  ├── Order 2  (user_id_fk = 1)
  ├── Order 3  (user_id_fk = 1)
  └── Order 4  (user_id_fk = 1)
```

Now in your Java code, you fetch User 1, **remove Order 4 from the collection**, and call `save()` again:

```java
UserDetails output = userDetailsService.findByID(id);
output.getOrderDetails().remove(0);   // removing Order 4 from the list
userDetailsService.saveUser(output);  // calling save, NOT delete
```

Notice — you are **not calling delete**. You are just updating the collection and saving.

---

## What happens WITHOUT Orphan Removal?

JPA sees that Order 4 is no longer in the collection. So it updates Order 4's foreign key to `null` in the DB. But **it does NOT delete the row.**

```
ORDER TABLE (after save, orphanRemoval = false)
| id | user_id_fk | product_name |
|----|------------|--------------|
|  1 |     1      |   IceCream   |   ← still has parent
|  2 |     1      |  ColdDrinks  |   ← still has parent
|  3 |     1      |   Burger     |   ← still has parent
|  4 |    null    |   Pizza      |   ← foreign key set to null
                                       but row still EXISTS!
```

Order 4 is now sitting in the DB with no parent. It has no `user_id_fk`. Nobody owns it. This is called an **Orphan**.

---

## What is an Orphan?

```
┌─────────────────────────────────────────────────┐
│  Orphan = a child row in the DB that no longer  │
│  has a parent (foreign key is null)             │
│                                                 │
│  It exists in the table but belongs to nobody.  │
└─────────────────────────────────────────────────┘
```

---

## The Fix — `orphanRemoval = true`

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "user_id_fk", referencedColumnName = "userId")
private List<OrderDetails> orderDetails = new ArrayList<>();
```

Now when you remove an order from the collection and call `save()`, JPA does a **two step process**:

```
Step 1: UPDATE order_details
        SET user_id_fk = null
        WHERE user_id_fk = ? AND id = ?

Step 2: DELETE FROM order_details
        WHERE id = ?
```

First it sets FK to null, then it **deletes the orphan row entirely.** The DB stays clean.

```
ORDER TABLE (after save, orphanRemoval = true)
| id | user_id_fk | product_name |
|----|------------|--------------|
|  1 |     1      |   IceCream   |
|  2 |     1      |  ColdDrinks  |
|  3 |     1      |   Burger     |
← Order 4 is completely GONE from the table
```

---

## How does JPA know about this?

The instructor explains this through the **Persistence Context**:

```
┌──────────────────────────────────────────────────────┐
│              PERSISTENCE CONTEXT (Memory)            │
│                                                      │
│   User 1                                             │
│     ├── Order 1                                      │
│     ├── Order 2                                      │
│     ├── Order 3                                      │
│     └── Order 4   ← you remove this from collection  │
│                                                      │
│   JPA is watching everything in memory.              │
│   When you call save(), JPA sees Order 4 is gone     │
│   from the collection → triggers delete in DB.       │
└──────────────────────────────────────────────────────┘
```

JPA tracks all your entities in memory via the Persistence Context. So when you remove Order 4 from the Java list and call save, JPA knows what changed and acts accordingly.

---

## The Full Demo Flow

```
Step 1 — INSERT
POST /api/user
{
  "name": "JohnXYZ",
  "orderDetails": [
    { "productName": "IceCream" },
    { "productName": "ColdDrinks" }
  ]
}

Result in DB:
USER TABLE: 1 row (JohnXYZ)
ORDER TABLE: 2 rows (IceCream, ColdDrinks) — both with user_id_fk = 1
```

```
Step 2 — TEST ORPHAN REMOVAL
GET /api/user/1  (this endpoint also removes index 0 and saves)

Inside the code:
  output.getOrderDetails().remove(0);  ← removes IceCream from list
  userDetailsService.saveUser(output); ← saves without delete call

With orphanRemoval = false:
  ORDER TABLE:
  | id | user_id_fk | product_name |
  |  1 |    null    |   IceCream   |  ← orphan, still in DB
  |  2 |     1      |  ColdDrinks  |

With orphanRemoval = true:
  ORDER TABLE:
  | id | user_id_fk | product_name |
  |  2 |     1      |  ColdDrinks  |  ← IceCream is completely deleted
```

---

## ⭐ Interview Tip — Cascade Delete vs Orphan Removal

The instructor specifically calls this out as a common interview question:

```
┌─────────────────────────────────────────────────────────────┐
│  Q: What is the difference between Cascade Delete           │
│     and Orphan Removal?                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  CASCADE DELETE                                             │
│  - You DELETE the PARENT                                    │
│  - JPA automatically deletes all its CHILDREN too           │
│  - Example: Delete User 1 → Orders 1,2,3 also deleted       │
│                                                             │
│  ORPHAN REMOVAL                                             │
│  - You do NOT delete the parent                             │
│  - You REMOVE a child from the parent's COLLECTION          │
│    in Java code                                             │
│  - JPA detects the child has no parent anymore              │
│  - JPA automatically deletes that child from the DB         │
│  - Example: Remove Order 4 from user.getOrders() list       │
│    → Order 4 gets deleted from DB on next save()            │
│                                                             │
│  KEY DIFFERENCE:                                            │
│  Cascade delete = parent is deleted                         │
│  Orphan removal = child is removed from collection          │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Takeaways from Step 3

- `CascadeType.ALL` covers persist, merge, remove — whatever happens to parent, happens to child.
- **Orphan** = a child row with `null` foreign key — it exists in DB but has no parent.
- Without `orphanRemoval`, removing a child from a Java collection only sets its FK to `null`. The row stays in DB.
- With `orphanRemoval = true`, JPA deletes that row entirely from DB.
- JPA can do this because the **Persistence Context** tracks everything in memory.
- The deletion is a **two-step process** — first set FK to null, then delete the row.
- Interview question to remember: **Cascade Delete vs Orphan Removal** — different triggers, different scenarios.

---
# Step 4 — One-to-Many: Bidirectional

---

## What is Bidirectional?

In Unidirectional (what we did so far), the reference exists **only one way** — from Parent (User) to Child (Order). The Order entity has no idea about the User.

In Bidirectional, **both sides know about each other:**

```
UNIDIRECTIONAL:
User ──────────────► Order
(User knows Order)   (Order knows nothing)


BIDIRECTIONAL:
User ◄──────────────► Order
(User knows Order)    (Order also knows User)
```

But here's something the instructor is very clear about:

> **Bidirectional does NOT change your DB structure at all.**
> The tables look exactly the same as unidirectional.
> The difference is only in the Java code — you can navigate from both sides.

```
DB is the same either way:
USER TABLE                    ORDER TABLE
| userId | name | phone |     | id | product_name | user_id_fk |
```

---

## The Two Sides — This is where people get confused

The instructor introduces two important terms here and immediately clears up a common misconception:

```
┌─────────────────────────────────────────────────────────────┐
│  Common misconception:                                      │
│  "Parent = Owning Side, Child = Inverse Side"               │
│                                                             │
│  THIS IS WRONG.                                             │
├─────────────────────────────────────────────────────────────┤
│  The correct definition:                                    │
│                                                             │
│  OWNING SIDE  = the side that holds the Foreign Key         │
│                 in the database table                       │
│                                                             │
│  INVERSE SIDE = the side that does NOT hold the FK          │
│                 in the database table                       │
└─────────────────────────────────────────────────────────────┘
```

---

## So who is the Owning Side in One-to-Many?

In 1-to-1, the **parent** held the foreign key — so the parent was the owning side.

But in One-to-Many, think about it logically:

```
USER TABLE                      ORDER TABLE
| userId | name | phone |       | id | product_name | user_id_fk |
|--------|------|-------|       |----|--------------|------------|
|   1    | John | 9999  |       |  1 |  Ice Cream   |     1      |
                               |  2 |  Cold Drink  |     1      |
                               |  3 |  Burger      |     1      |
```

The **foreign key (`user_id_fk`) lives in the Order table** — the child table.

So in One-to-Many:
```
┌──────────────────────────────────────────┐
│  CHILD (Order)  = OWNING SIDE            │
│  (because FK lives in the Order table)   │
│                                          │
│  PARENT (User)  = INVERSE SIDE           │
│  (because no FK in the User table)       │
└──────────────────────────────────────────┘
```

This is the **opposite** of 1-to-1. The owning side flipped to the child!

---

## The Rules for Bidirectional

Once you know owning and inverse, the coding rules are simple:

```
┌─────────────────────────────────────────────────────────┐
│  OWNING SIDE (Order / Child):                           │
│  - Use @ManyToOne annotation                            │
│  - Use @JoinColumn (because FK lives here)              │
│                                                         │
│  INVERSE SIDE (User / Parent):                          │
│  - Use @OneToMany annotation                            │
│  - Use mappedBy = "fieldName"                           │
│  - This tells JPA: "I am NOT managing the FK.           │
│    Look at the other side for that."                    │
└─────────────────────────────────────────────────────────┘
```

---

## The Entities

### OrderDetails — Owning Side

```java
@Table(name = "order_details")
@Entity
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "id")
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @ManyToOne                          // ← Many orders to One user
    @JoinColumn(name = "user_id_owing_fk",
                referencedColumnName = "userID")  // ← FK lives here
    private UserDetails userDetails;    // ← reference to parent

    // getters and setters
}
```

### UserDetails — Inverse Side

```java
@Table(name = "user_details")
@Entity
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "userId")
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String phone;

    @OneToMany(mappedBy = "userDetails", cascade = CascadeType.ALL)
    // ↑ mappedBy = the field name in OrderDetails that holds the FK
    // ↑ telling JPA: "don't create FK here, the other side manages it"
    private List<OrderDetails> orderDetails = new ArrayList<>();

    // special setter — explained below
    public void setOrderDetails(List<OrderDetails> orderDetails) {
        this.orderDetails = orderDetails;
        for (OrderDetails order : orderDetails) {
            order.setUserDetails(this);  // ← manually linking each order to this user
        }
    }
}
```

---

## Why the special setter? — Very important

This is the part the instructor says trips people up the most.

When you create a User and give it a list of Orders, remember:

- The **inverse side** (`UserDetails`) is using `mappedBy` — it is NOT managing the FK
- JPA will NOT automatically fill in `userDetails` field inside each `OrderDetails` object
- So if you just call `setOrderDetails(list)`, each Order's `userDetails` field stays **null**
- The FK column (`user_id_owing_fk`) in the Order table would be **null**

The fix is — inside the setter of the inverse side, **manually set the back-reference** on each child:

```java
public void setOrderDetails(List<OrderDetails> orderDetails) {
    this.orderDetails = orderDetails;
    for (OrderDetails order : orderDetails) {
        order.setUserDetails(this);  // ← "this" order belongs to "this" user
    }
}
```

```
Without this:
  Order 1 → userDetails = null  ← FK will be null in DB!
  Order 2 → userDetails = null

With this:
  Order 1 → userDetails = User1  ← FK properly set in DB ✓
  Order 2 → userDetails = User1  ← FK properly set in DB ✓
```

---

## Why `@JsonIdentityInfo`? — Solving the recursion problem

In bidirectional mapping, during JSON serialization:

```
User has → List of Orders
Each Order has → reference back to User
User again has → List of Orders
... infinite recursion → StackOverflowError
```

`@JsonIdentityInfo` solves this by giving each object a unique identity. When Jackson encounters the same object again during serialization, instead of serializing the whole object again, it just puts the **ID**:

```json
{
  "userId": 1,
  "name": "JohnXYZ",
  "orderDetails": [
    {
      "id": 1,
      "productName": "IceCream",
      "userDetails": 1        ← just the ID, not the full User object again
    },
    {
      "id": 2,
      "productName": "ColdDrinks",
      "userDetails": 1        ← same, just the ID
    }
  ]
}
```

No infinite loop. Clean response.

The instructor also mentions `@JsonIgnore` as another option — put it on the child side's parent reference if you don't want parent info repeated in the response at all:

```java
// In OrderDetails
@JsonIgnore
private UserDetails userDetails;
```

The instructor's general preference:
```
Generally, access flows from Parent → Child.
So put @JsonIgnore on the child's reference back to parent.
That way parent gives you child info, but child doesn't
repeat parent info again.
```

---

## Full Picture — Bidirectional flow

```
POST /api/user
{
  "name": "JohnXYZ",
  "phone": "1234567890",
  "orderDetails": [
    { "productName": "IceCream" },
    { "productName": "ColdDrinks" }
  ]
}

          │
          ▼
  setOrderDetails() is called
          │
          ▼
  for each Order → order.setUserDetails(this user)
          │
          ▼
  JPA sees OrderDetails.userDetails is set
  (owning side, has @JoinColumn)
          │
          ▼
  Hibernate inserts User first
  Then inserts each Order with user_id_owing_fk properly filled

DB Result:
USER TABLE                         ORDER TABLE
| userId | name    | phone |       | id | user_id_owing_fk | product_name |
|--------|---------|-------|       |----|------------------|--------------|
|   1    | JohnXYZ | 9999  |       |  1 |        1         |   IceCream   |
                                   |  2 |        1         |  ColdDrinks  |
```

---

## Key Takeaways from Step 4

- Bidirectional = both sides have Java references to each other. **DB structure stays the same.**
- **Owning Side = whoever holds the FK in the DB table.** In One-to-Many, that's the **child (Order).**
- **Inverse Side = whoever does NOT hold the FK.** In One-to-Many, that's the **parent (User).**
- Owning side uses `@ManyToOne` + `@JoinColumn`.
- Inverse side uses `@OneToMany` + `mappedBy`.
- `mappedBy` tells JPA — *"don't manage FK here, the other side does it."*
- Because of `mappedBy`, you must **manually set the back-reference** in the inverse side's setter.
- Use `@JsonIdentityInfo` or `@JsonIgnore` to prevent infinite recursion during JSON serialization.

---
# Step 5 — Many-to-One: Unidirectional & Bidirectional

---

## First — What is Many-to-One?

The instructor makes a very important point right at the start:

> "One-to-Many and Many-to-One are the **same relationship**. It's just the **direction you're looking at it from** that changes."

```
ONE-TO-MANY  →  talking from Parent's perspective
"One User can have Many Orders"
        User ──────────────► Order
        (Parent knows Child)


MANY-TO-ONE  →  talking from Child's perspective
"Many Orders belong to One User"
        Order ──────────────► User
        (Child knows Parent)
```

Same two tables. Same FK in the DB. Just a different way of framing the relationship in Java code.

---

## DB Structure — Exactly the same

This is a key point the instructor repeats clearly:

```
ONE-TO-MANY setup:          MANY-TO-ONE setup:
USER TABLE                  USER TABLE
| userId | name | phone |   | userId | name | phone |
                                    (no change)

ORDER TABLE                 ORDER TABLE
| id | product | user_fk |  | id | product | user_fk |
                                    (no change)
```

The tables are identical. The FK still lives in the Order (child) table. **Nothing changes in the DB.**

---

## So what actually changes?

The difference is **which entity knows about the other in Java code:**

```
┌──────────────────────────────────────────────────────────┐
│  ONE-TO-MANY (Parent's view):                            │
│  - UserDetails has a List<OrderDetails>                  │
│  - User knows about its orders                           │
│  - Order knows nothing about User                        │
│  - Annotation: @OneToMany on the User side               │
├──────────────────────────────────────────────────────────┤
│  MANY-TO-ONE (Child's view):                             │
│  - OrderDetails has a UserDetails field                  │
│  - Order knows about its parent User                     │
│  - User knows nothing about its orders                   │
│  - Annotation: @ManyToOne on the Order side              │
└──────────────────────────────────────────────────────────┘
```

---

## Many-to-One: Unidirectional — The Code

### UserDetails — Parent (knows nothing about orders)

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String phone;

    // No List<OrderDetails> here!
    // Parent has zero knowledge of its children.

    // getters and setters
}
```

### OrderDetails — Child (knows its parent)

```java
@Table(name = "order_details")
@Entity
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @ManyToOne(cascade = CascadeType.ALL)   // ← Many orders, One user
    @JoinColumn(name = "user_id_owing_fk",
                referencedColumnName = "userID")  // ← FK lives here
    private UserDetails userDetails;        // ← child knows parent

    // getter and setter
}
```

---

## Side by side comparison — One-to-Many vs Many-to-One

```
┌─────────────────────────────────────────────────────────────────┐
│                    ONE-TO-MANY                                  │
│                                                                 │
│  UserDetails:                  OrderDetails:                    │
│  @OneToMany                    (nothing)                        │
│  @JoinColumn                                                    │
│  List<OrderDetails>            no reference to user             │
│                                                                 │
│  Parent knows Child ✓          Child is unaware ✗               │
├─────────────────────────────────────────────────────────────────┤
│                    MANY-TO-ONE                                  │
│                                                                 │
│  UserDetails:                  OrderDetails:                    │
│  (nothing)                     @ManyToOne                       │
│                                @JoinColumn                      │
│  no reference to orders        UserDetails userDetails          │
│                                                                 │
│  Parent is unaware ✗           Child knows Parent ✓             │
└─────────────────────────────────────────────────────────────────┘
```

---

## How it works — Insert via Many-to-One

Since the child (Order) is now the one driving the relationship, you create an Order and give it a User:

```
POST /api/order
{
  "productName": "IceCream",
  "userDetails": {
    "name": "SJ",
    "phone": "1111212121"
  }
}
```

Because of `CascadeType.ALL` on the Order side:
- JPA first creates the User
- Then creates the Order with `user_id_owing_fk` properly set

```
DB Result:
USER TABLE                        ORDER TABLE
| userId | name | phone     |     | id | user_id_owing_fk | product_name |
|--------|------|-----------|     |----|------------------|--------------|
|   1    |  SJ  | 1111212121|     |  1 |        1         |   IceCream   |
```

---

## Many-to-One: Bidirectional

The instructor is very direct here:

> "Many-to-One Bidirectional is **exactly the same** as One-to-Many Bidirectional. Same piece of code. Just the way of talking is different."

```
┌──────────────────────────────────────────────────────────┐
│  One-to-Many Bidirectional:                              │
│  "One User has Many Orders"                              │
│  → We start the conversation from the Parent side        │
│                                                          │
│  Many-to-One Bidirectional:                              │
│  "Many Orders belong to One User"                        │
│  → We start the conversation from the Child side         │
│                                                          │
│  Both result in the EXACT SAME code setup:               │
│  - Child has @ManyToOne + @JoinColumn (owning side)      │
│  - Parent has @OneToMany + mappedBy (inverse side)       │
└──────────────────────────────────────────────────────────┘
```

So there is nothing new to learn for Many-to-One Bidirectional — it is the same as what we covered in Step 4.

---

## The Full Mental Model — Putting it all together

The instructor wants you to think of One-to-Many and Many-to-One not as two different things, but as **two perspectives of the same thing:**

```
THE SAME RELATIONSHIP, TWO WAYS TO TALK ABOUT IT:

        USER (Parent)                   ORDER (Child)
        ─────────────                   ─────────────

One-to-Many:
"User has many Orders"
User ──────────────────────────────────► Order
@OneToMany on User side                  no annotation


Many-to-One:
"Many Orders belong to one User"
User ◄────────────────────────────────── Order
no annotation                            @ManyToOne on Order side


Bidirectional (both at once):
User ◄──────────────────────────────────► Order
@OneToMany + mappedBy on User            @ManyToOne + @JoinColumn on Order
(inverse side)                           (owning side)
```

---

## One clean summary table

```
┌──────────────────┬───────────────────────────┬────────────────────────────┐
│                  │  UserDetails (Parent)     │  OrderDetails (Child)      │
├──────────────────┼───────────────────────────┼────────────────────────────┤
│ One-to-Many Uni  │ @OneToMany + @JoinColumn  │ nothing                    │
│                  │ List<OrderDetails>        │                            │
├──────────────────┼───────────────────────────┼────────────────────────────┤
│ Many-to-One Uni  │ nothing                   │ @ManyToOne + @JoinColumn   │
│                  │                           │ UserDetails userDetails    │
├──────────────────┼───────────────────────────┼────────────────────────────┤
│ Bidirectional    │ @OneToMany + mappedBy     │ @ManyToOne + @JoinColumn   │
│ (same for both)  │ List<OrderDetails>        │ UserDetails userDetails    │
│                  │ (inverse side)            │ (owning side)              │
└──────────────────┴───────────────────────────┴────────────────────────────┘
DB structure is IDENTICAL in all three cases.
FK always lives in the ORDER table.
```

---

## Key Takeaways from Step 5

- Many-to-One and One-to-Many are the **same relationship** — just different perspectives.
- One-to-Many = Parent's view. Many-to-One = Child's view.
- **DB structure does not change** — FK always lives in the child (Order) table.
- In Many-to-One Unidirectional — the **parent knows nothing**, the **child knows its parent** via `@ManyToOne + @JoinColumn`.
- Many-to-One Bidirectional = exactly the same code as One-to-Many Bidirectional. No new concept.
- The key question to always ask yourself: *"From whose perspective am I describing this relationship?"* That determines which annotation goes where.

---
# Step 6 — Many-to-Many: Unidirectional & Bidirectional

---

## What is Many-to-Many?

Before anything else, the instructor makes a fundamental point that separates Many-to-Many from everything we've learned so far:

> "In Many-to-Many, there is **no parent-child concept**. Neither entity is dependent on the other. If one gets deleted, the other does NOT get deleted."

```
ONE-TO-MANY:
User (parent) ──────► Order (child)
If User is deleted → Orders also deleted (child depends on parent)


MANY-TO-MANY:
Order ◄──────────────► Product
Neither depends on the other.
Delete an Order → Products still exist.
Delete a Product → Orders still exist.
```

---

## The Real World Example

```
ORDER 1 ──────────────► PRODUCT 1 (Ice Cream)
ORDER 1 ──────────────► PRODUCT 2 (Cold Drink)
ORDER 2 ──────────────► PRODUCT 1 (Ice Cream)
ORDER 2 ──────────────► PRODUCT 2 (Cold Drink)
```

One order can have many products.
One product can belong to many orders.
**That's Many-to-Many.**

---

## The Problem — Where do you store the FK?

In One-to-Many, the FK lived in the child table. But here — who is the child?

```
Can FK live in ORDER table?
| order_id | product_id |
One row can't hold multiple product IDs. ✗

Can FK live in PRODUCT table?
| product_id | order_id |
One row can't hold multiple order IDs. ✗
```

Neither table can hold the FK. So the **only option** is a new join table. This is why the instructor said earlier — *"new tables are generally only created in Many-to-Many."*

```
NEW JOIN TABLE: order_product
| order_id | product_id |
|----------|------------|
|    1     |     1      |   ← Order 1 has Product 1
|    1     |     2      |   ← Order 1 has Product 2
|    2     |     1      |   ← Order 2 has Product 1
|    2     |     2      |   ← Order 2 has Product 2
```

---

## Owning & Inverse Side in Many-to-Many

Since there is no parent-child, **you can make either side the owning side.** It's your choice.

But remember the rule:
```
┌─────────────────────────────────────────────────────────┐
│  OWNING SIDE  = responsible for creating & managing     │
│                 the join table                          │
│                                                         │
│  INVERSE SIDE = just a Java reference via mappedBy.     │
│                 Does NOT manage the join table.         │
└─────────────────────────────────────────────────────────┘
```

In this example the instructor makes **Order** the owning side.

---

## Many-to-Many Unidirectional — The Code

### OrderDetails — Owning Side

```java
@Table(name = "order_details")
@Entity
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderNo;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "order_product",           // name of the new join table
        joinColumns =
            @JoinColumn(name = "order_id"),         // FK for Order
        inverseJoinColumns =
            @JoinColumn(name = "product_id")        // FK for Product
    )
    private List<ProductDetails> productDetails = new ArrayList<>();

    // getters and setters
}
```

### ProductDetails

```java
@Table(name = "product_details")
@Entity
public class ProductDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String name;
    private double price;

    // getters and setters
    // No reference to Order at all — unidirectional
}
```

---

## Breaking down `@JoinTable`

```java
@JoinTable(
    name = "order_product",
    joinColumns = @JoinColumn(name = "order_id"),
    inverseJoinColumns = @JoinColumn(name = "product_id")
)
```

```
┌─────────────────────────────────────────────────────────────┐
│  name               → what to call the new join table       │
│                                                             │
│  joinColumns        → FK column for the OWNING side         │
│                       (Order, since we're writing this      │
│                        annotation on Order)                 │
│                                                             │
│  inverseJoinColumns → FK column for the OTHER side          │
│                       (Product)                             │
└─────────────────────────────────────────────────────────────┘
```

Result in DB:

```
ORDER TABLE          PRODUCT TABLE        ORDER_PRODUCT (join table)
| order_no |         | product_id |       | order_id | product_id |
|----------|         | name       |       |----------|------------|
|    1     |         | price      |       |    1     |     1      |
|    2     |         |            |       |    1     |     2      |
                                          |    2     |     2      |
```

---

## The Demo — Step by Step

### Step 1 — Create Products first

```
POST /api/product
{ "name": "ice-cream", "price": 100 }
→ productId = 1

POST /api/product
{ "name": "cold-drink", "price": 150 }
→ productId = 2
```

```
PRODUCT TABLE:
| price | product_id | name       |
|-------|------------|------------|
| 100.0 |     1      | ice-cream  |
| 150.0 |     2      | cold-drink |
```

### Step 2 — Create Orders and associate with existing Products

```
POST /api/order
{
  "productDetails": [
    { "productId": 1 },
    { "productId": 2 }
  ]
}
```

The Order controller logic:

```java
@PostMapping(path = "/order")
public OrderDetails insertOrder(@RequestBody OrderDetails orderDetail) {

    // Step 1: take incoming product IDs
    // Step 2: fetch full product details from DB for each ID
    // Step 3: set the managed products list on the order
    // Step 4: save the order

    List<ProductDetails> managedProducts =
        orderDetail.getProductDetails().stream()
            .map(product ->
                productService.findByID(product.getProductId()))
            .collect(Collectors.toList());

    orderDetail.setProductDetails(managedProducts);
    return orderService.saveOrder(orderDetail);
}
```

Why fetch products from DB first? Because you want to associate the Order with **existing** products, not create new ones.

```
After Order 1 (associated with Product 1 and 2):

ORDER_PRODUCT table:
| order_id | product_id |
|----------|------------|
|    1     |     1      |
|    1     |     2      |


POST /api/order
{
  "productDetails": [
    { "productId": 2 }    ← only associating with Product 2
  ]
}

After Order 2 (associated with only Product 2):

ORDER_PRODUCT table:
| order_id | product_id |
|----------|------------|
|    1     |     1      |
|    1     |     2      |
|    2     |     2      |   ← Order 2 only linked to Product 2
```

---

## Many-to-Many Bidirectional — The Code

Now both sides know about each other. The instructor keeps Order as the owning side. Product becomes the inverse side.

### OrderDetails — Owning Side (same as before)

```java
@Table(name = "order_details")
@Entity
public class OrderDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderNo;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "order_product",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<ProductDetails> productDetails = new ArrayList<>();

    // getters and setters
}
```

### ProductDetails — Inverse Side (new addition)

```java
@Table(name = "product_details")
@Entity
public class ProductDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String name;
    private double price;

    @ManyToMany(mappedBy = "productDetails")  // ← inverse side
    @JsonIgnore                                // ← stops recursion
    List<OrderDetails> orders = new ArrayList<>();

    // getters and setters
}
```

---

## The Critical Warning — Owning vs Inverse matters for DB operations

The instructor gives a very important warning here:

```
SCENARIO 1: You create an Order and add Products to it.
→ Order is the OWNING side
→ JPA manages the join table automatically
→ order_product table gets filled correctly ✓


SCENARIO 2: You create a Product and add Orders to it.
→ Product is the INVERSE side (mappedBy)
→ JPA does NOT manage the join table from this side
→ order_product table does NOT get filled ✗
→ You have to manually handle it in your code
```

This is why the instructor says:

> "Always choose the owning side based on your business logic — which entity is more likely to be the one driving the relationship. If orders come in and they reference products, make Order the owning side."

```
┌──────────────────────────────────────────────────────────────┐
│  RULE OF THUMB for choosing owning side:                     │
│                                                              │
│  Ask yourself: "In my application, which entity              │
│  is created first / drives the relationship?"                │
│                                                              │
│  Example: Products already exist in the system.              │
│  Orders come in and reference products.                      │
│  → Make ORDER the owning side.                               │
│                                                              │
│  If you pick the wrong side, JPA won't update the            │
│  join table automatically — you'll have to do it             │
│  manually in your setter code.                               │
└──────────────────────────────────────────────────────────────┘
```

---

## Complete Picture — All mapping types side by side

```
┌──────────────────┬──────────────────────┬─────────────────────┬───────────────────┐
│  Mapping         │  Who holds FK        │  New table?         │  Annotation       │
├──────────────────┼──────────────────────┼─────────────────────┼───────────────────┤
│ One-to-One       │ Parent table         │ No                  │ @OneToOne         │
│                  │                      │                     │ @JoinColumn       │
├──────────────────┼──────────────────────┼─────────────────────┼───────────────────┤
│ One-to-Many      │ Child table          │ No (with            │ @OneToMany        │
│                  │                      │ @JoinColumn)        │ @JoinColumn       │
│                  │                      │ Yes (by default)    │                   │
├──────────────────┼──────────────────────┼─────────────────────┼───────────────────┤
│ Many-to-One      │ Child table          │ No                  │ @ManyToOne        │
│                  │                      │                     │ @JoinColumn       │
├──────────────────┼──────────────────────┼─────────────────────┼───────────────────┤
│ Many-to-Many     │ Neither — join table │ Always YES          │ @ManyToMany       │
│                  │                      │                     │ @JoinTable        │
└──────────────────┴──────────────────────┴─────────────────────┴───────────────────┘
```

---

## Key Takeaways from Step 6

- Many-to-Many has **no parent-child relationship** — both entities are independent.
- A **new join table is always created** — neither entity can hold FKs for multiple records.
- Use `@JoinTable` on the owning side to name the join table and define both FK columns.
- In bidirectional, the inverse side uses `mappedBy` — same rule as One-to-Many bidirectional.
- **JPA only manages the join table from the owning side.** If you drive the relationship from the inverse side, you must handle FK updates manually.
- Choose your owning side based on business logic — whichever entity drives the relationship in your application.
- Use `@JsonIgnore` on the inverse side to stop infinite recursion during serialization.

---

## That's the complete lecture!

Here's the full roadmap of what we covered:

```
Step 1 → One-to-Many Unidirectional   (@OneToMany + @JoinColumn)
Step 2 → Lazy vs Eager Loading         (default lazy, FetchType.EAGER)
Step 3 → Cascade Types + Orphan Removal (interview tip!)
Step 4 → One-to-Many Bidirectional     (owning vs inverse, mappedBy)
Step 5 → Many-to-One (Uni + Bi)        (same thing, child's perspective)
Step 6 → Many-to-Many (Uni + Bi)       (@JoinTable, no parent-child)
```

Let me know if you want me to revisit or expand on any specific step!