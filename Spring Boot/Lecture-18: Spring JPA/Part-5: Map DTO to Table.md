# Step 1 — `spring.jpa.hibernate.ddl-auto` ⚙️

---

## What is it?

This is a configuration property you add in your `application.properties` file. It tells Hibernate **how to create and manage your database schema** when the application starts or stops.

```properties
spring.jpa.hibernate.ddl-auto = <value>
```

---

## The 5 Values — What Each One Does

```
┌─────────────────────────────────────────────────────────────────────┐
│              spring.jpa.hibernate.ddl-auto                          │
├──────────────┬────────┬────────┬────────┬────────────────────────── │
│   Value      │ Create │ Update │ Delete │ When to Use?              │
│              │ Schema │ Schema │ Schema │                           │
├──────────────┼────────┼────────┼────────┼───────────────────────────│
│ none         │  ✗     │  ✗     │  ✗     │ ✅ PRODUCTION              │
│ update       │  ✓     │  ✓     │  ✗     │ ✅ DEVELOPMENT             │
│ validate     │  ✗     │  ✗     │  ✗     │ 🔍 Validation only        │
│ create       │  ✓     │  ✓     │  ✓     │ ⚠️ Testing only           │
│ create-drop  │  ✓     │  ✓     │  ✓     │ ⚠️ In-memory DBs (H2)     │
└──────────────┴────────┴────────┴────────┴───────────────────────────┘
```

---

## Each Value Explained

### 1. `none`
- Does **absolutely nothing** — no create, no update, no delete.
- **Use in production. Always.**
- Why? Because in production, DB scripts are written separately and handed over to a **DBA (Database Administrator)** who runs them carefully, with a rollback plan in place. You never want Hibernate auto-managing your production database.

---

### 2. `update`
- Creates the schema **if it doesn't exist**.
- If the schema already exists and you've added a new column to your entity, it will **add that column**.
- It will **never delete** any existing data or columns.
- **Use during development.** Safe and convenient.

```
Entity has 4 columns in DB already.
You add a 5th column to your Entity class.
→ Hibernate adds the 5th column to the table.
→ Existing 4 columns + their data? Untouched. ✅
```

---

### 3. `validate`
- Does **not create, update, or delete** anything.
- At application startup, it simply **compares** your entity classes with the actual DB schema.
- If there's any mismatch → **throws an exception** and the app won't start.
- Useful when you want to make sure your code and DB are perfectly in sync.

---

### 4. `create`
- At **every application startup**: first **drops** everything (all tables, all data), then **recreates** fresh.
- Dangerous — you lose all data every time you restart.

```
App starts → DROP all tables → CREATE fresh tables
```

---

### 5. `create-drop`
- At **startup**: creates the schema.
- At **shutdown**: drops the schema.
- Very similar to `create`, but the timing of the drop is different.

```
App starts  → CREATE tables
App running → everything works normally
App stops   → DROP all tables
```

> This is the **default behavior for in-memory databases** like H2.

---

## `create` vs `create-drop` — Side by Side

```
┌─────────────────┬──────────────────────────────┬──────────────────────────────┐
│                 │         create               │        create-drop           │
├─────────────────┼──────────────────────────────┼──────────────────────────────┤
│ On startup      │ DROP first, then CREATE      │ Only CREATE                  │
│ On shutdown     │ Nothing                      │ DROP                         │
└─────────────────┴──────────────────────────────┴──────────────────────────────┘
```

---

## 💡 The Golden Rule (Instructor's Tip)

```
┌──────────────────────────────────────────────┐
│  PRODUCTION  →  always use "none"            │
│  DEVELOPMENT →  "update" is sufficient       │
└──────────────────────────────────────────────┘
```

> In production, DBAs own the database. Hibernate should never touch it automatically.

---
# Step 2 — DB vs Schema 🗄️

---

## Why is the Instructor Explaining This?

Before jumping into the `@Table` annotation, the instructor pauses to explain **what a Schema is** — because `@Table` has a `schema` attribute, and without understanding this concept, that attribute won't make sense.

> This is a **pure database concept**, nothing to do with Java.

---

## Database vs Schema — The Relationship

```
┌─────────────────────────────────────────────────────────┐
│                     DATABASE (1 DB)                     │
│                                                         │
│   ┌─────────────────┐     ┌─────────────────┐           │
│   │   SCHEMA 1      │     │   SCHEMA 2      │           │
│   │  (Team A owns)  │     │  (Team B owns)  │           │
│   │                 │     │                 │           │
│   │ ┌─────────────┐ │     │ ┌─────────────┐ │           │
│   │ │   Table 1   │ │     │ │   Table 4   │ │           │
│   │ │   Table 2   │ │     │ │   Table 5   │ │           │
│   │ │   Table 3   │ │     │ │   Table 6   │ │           │
│   │ └─────────────┘ │     │ └─────────────┘ │           │
│   └─────────────────┘     └─────────────────┘           │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

- One **Database** can have **multiple Schemas**.
- Each **Schema** is a **logical grouping of tables**.
- Each schema can have its own **ownership and permissions** — meaning only certain teams or engineers can access it.

---

## Real World Use Case

Imagine a company with **one shared database** but **multiple teams** working on it.

```
┌──────────────────────────────────────────────────────────────┐
│                      Company DB                              │
│                                                              │
│  ┌──────────────────┐        ┌──────────────────┐            │
│  │ Schema: ONBOARD  │        │ Schema: PAYMENTS │            │
│  │  Owned by Team A │        │  Owned by Team B │            │
│  │                  │        │                  │            │
│  │  - users         │        │  - transactions  │            │
│  │  - roles         │        │  - invoices      │            │
│  └──────────────────┘        └──────────────────┘            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

- Both teams **share one DB** but work in **their own schema**.
- Team A cannot mess with Team B's tables and vice versa — **permissions are scoped per schema**.

---

## What if You Don't Use a Schema?

```
┌─────────────────────────────────────────────────┐
│                  DATABASE                       │
│                                                 │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│   │ Table 1  │  │ Table 2  │  │ Table 3  │      │
│   └──────────┘  └──────────┘  └──────────┘      │
│                                                 │
│   (Tables sit directly under the DB,            │
│    no schema grouping)                          │
└─────────────────────────────────────────────────┘
```

That's totally fine **when your team has its own dedicated DB** — no need to create schemas in that case.

Schemas become important **only when one DB is shared across multiple teams**.

---

## ⚠️ Important Note the Instructor Highlights

> **Hibernate does NOT automatically create schemas for you.**

If you use the `schema` attribute in `@Table`, you must **manually create that schema first** in your `application.properties` datasource URL, like this:

```properties
spring.datasource.url=jdbc:h2:mem:userDB;INIT=CREATE SCHEMA IF NOT EXISTS ONBOARDING
```

You can create multiple schemas too:

```properties
spring.datasource.url=jdbc:h2:mem:userDB;INIT=CREATE SCHEMA IF NOT EXISTS ONBOARDING\;CREATE SCHEMA IF NOT EXISTS PAYMENTS
```

> If you mention a schema in `@Table` but forget to create it → Hibernate will throw a **"schema not found"** error.

---

## Quick Summary

```
┌─────────────────────────────────────────────────────────────┐
│  DB       → the whole database                              │
│  Schema   → logical grouping of tables inside a DB          │
│  Use when → one DB is shared across multiple teams          │
│  Hibernate→ does NOT auto-create schemas, you must do it    │
└─────────────────────────────────────────────────────────────┘
```

---

# Step 3 — `@Table` Annotation 🗂️

---

## Why Do We Need `@Table`?

Remember from previous videos — when you mark a class with `@Entity`, JPA knows this class maps to a table in the DB. But **you never told JPA what to name that table.**

So what does JPA do by default?

```
Entity class name → UserDetails
Hibernate auto-generates table name → USER_DETAILS

(CamelCase → UPPER_SNAKE_CASE)
```

That's fine for simple cases. But what if you want **full control** over the table — its name, which schema it belongs to, which columns should be unique, which columns should be indexed?

That's exactly what `@Table` gives you.

> `@Table` is **optional**. If you skip it, Hibernate uses defaults. But when you need control, this is your tool.

---

## The Full Structure of `@Table`

```java
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {
    String name() default "";                        // table name in DB
    String schema() default "";                      // which schema it belongs to
    UniqueConstraint[] uniqueConstraints() default {}; // unique constraints
    Index[] indexes() default {};                    // indexes on columns
}
```

4 attributes. Let's go through each one.

---

## Attribute 1 — `name`

Lets you define **exactly what the table should be called in the DB**.

```java
@Table(name = "USER_DETAILS")
@Entity
public class UserDetails {
    // fields
}
```

```
Without @Table  →  Hibernate decides the name (USER_DETAILS by default)
With @Table     →  YOU decide the name, whatever you put here appears in DB
```

---

## Attribute 2 — `schema`

Tells Hibernate which **schema this table belongs to**.

```java
@Table(name = "USER_DETAILS", schema = "ONBOARDING")
@Entity
public class UserDetails {
    // fields
}
```

```
┌──────────────────────────────────────────┐
│             DATABASE                     │
│                                          │
│   ┌──────────────────────────────┐       │
│   │      Schema: ONBOARDING      │       │
│   │                              │       │
│   │   ┌──────────────────────┐   │       │
│   │   │   TABLE: USER_DETAILS│   │       │
│   │   └──────────────────────┘   │       │
│   └──────────────────────────────┘       │
│                                          │
│   ┌──────────────────────────────┐       │
│   │   TABLE: ORDER_DETAILS       │       │
│   │   (No schema — sits directly │       │
│   │    under DB)                 │       │
│   └──────────────────────────────┘       │
└──────────────────────────────────────────┘
```

> Remember — schema must be **pre-created** in your datasource URL. Hibernate won't create it for you.

---

## Attribute 3 — `uniqueConstraints`

Lets you define **which columns must have unique values** across all rows.

Two types:

### Single Column Unique Constraint
Only one column needs to be unique on its own.
```
phone column → every row must have a different phone number
```

### Composite Unique Constraint
Two or more columns **together** must be unique.
```
name + email together → the combination must be unique
(same name is ok, same email is ok, but same name AND email together is not allowed)
```

```java
@Table(
    name = "USER_DETAILS",
    schema = "ONBOARDING",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "phone"),             // single column
        @UniqueConstraint(columnNames = {"name", "email"})   // composite
    }
)
@Entity
public class UserDetails {

    @Id
    private Long id;
    private String name;
    private String email;
    private String phone;
}
```

```
In DB Schema it looks like this:

┌─────────────────────────────────────────────────────┐
│               TABLE: USER_DETAILS                   │
├──────────┬───────────┬───────────┬──────────────────┤
│    ID    │   NAME    │   EMAIL   │     PHONE        │
│  (PK)    │           │           │                  │
├──────────┴───────────┴───────────┴──────────────────┤
│ Unique Key 1: phone                                 │
│ Unique Key 2: name + email  (composite)             │
└─────────────────────────────────────────────────────┘
```

---

## Attribute 4 — `indexes`

Indexes **speed up data retrieval** on frequently queried columns. Instead of scanning the whole table, DB can jump directly to the relevant rows.

Again two types — single column index and composite index:

```java
@Table(
    name = "USER_DETAILS",
    schema = "ONBOARDING",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "phone"),
        @UniqueConstraint(columnNames = {"name", "email"})
    },
    indexes = {
        @Index(name = "index_phone", columnList = "phone"),          // single column
        @Index(name = "index_name_email", columnList = "name, email") // composite
    }
)
@Entity
public class UserDetails {

    @Id
    private Long id;
    private String name;
    private String email;
    private String phone;
}
```

> In `@Index`, the `columnList` is a **comma-separated string** — not an array like in `@UniqueConstraint`.

```
@UniqueConstraint  →  columnNames = {"name", "email"}   // array
@Index             →  columnList  = "name, email"        // comma-separated string
```

---

## Full Picture — All 4 Attributes Together

```
┌──────────────────────────────────────────────────────────────────┐
│  @Table                                                          │
│                                                                  │
│  name              →  what the table is called in DB             │
│  schema            →  which schema the table belongs to          │
│  uniqueConstraints →  which columns (or combo) must be unique    │
│  indexes           →  which columns (or combo) to index          │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Recap

```
┌────────────────────────────────────────────────────────────────┐
│  @Table is OPTIONAL                                            │
│  Without it → Hibernate auto-names your table                  │
│  With it    → You have full control                            │
│                                                                │
│  schema attribute → pre-create schema manually first           │
│  uniqueConstraints → single column OR composite                │
│  indexes → single column OR composite (comma-separated string) │
└────────────────────────────────────────────────────────────────┘
```

---

# Step 4 — `@Column` Annotation 📋

---

## Why Do We Need `@Column`?

Just like `@Table` gives you control over the **table**, `@Column` gives you control over **individual columns** inside that table.

Without `@Column`, JPA still maps your fields to columns — but with **default values** that Hibernate decides. When you need to customize a specific column's behavior, `@Column` is your tool.

> `@Column` is also **optional**. If you don't use it, JPA maps the field with default settings.

---

## What Defaults Does JPA Apply Without `@Column`?

```
┌─────────────────────────────────────────────────────────────┐
│  Field: name                                                │
│                                                             │
│  Without @Column →  JPA creates column with:                │
│                     column name  = "NAME" (or "name")       │
│                     unique       = false                    │
│                     nullable     = true                     │
│                     length       = 255                      │
└─────────────────────────────────────────────────────────────┘
```

These defaults may not always be what you want — that's where `@Column` comes in.

---

## The Key Attributes of `@Column`

```java
@Column(
    name     = "full_name",   // what the column is called in DB
    unique   = true,          // should values in this column be unique?
    nullable = false,         // can this column hold null values?
    length   = 255            // max length (for String fields)
)
private String name;
```

Let's break each one down.

---

## Attribute 1 — `name`

By default, Hibernate uses the **field name** as the column name. With `name`, you can give it a **custom name in the DB**.

```
Java field name  →  name
DB column name   →  full_name   (because we specified it)
```

```java
@Table(name = "user_details")
@Entity
public class UserDetails {

    @Id
    private Long id;

    @Column(name = "full_name", unique = true, nullable = false, length = 255)
    private String name;

    private String email;   // no @Column → JPA uses defaults
    private String phone;   // no @Column → JPA uses defaults
}
```

```
┌──────────────────────────────────────────┐
│         TABLE: USER_DETAILS              │
├──────────────────────────────────────────┤
│  ID          (from @Id, no @Column)      │
│  FULL_NAME   (custom name via @Column)   │
│  EMAIL       (default, no @Column)       │
│  PHONE       (default, no @Column)       │
└──────────────────────────────────────────┘
```

---

## Attribute 2 — `unique`

Makes that **single column** unique — no two rows can have the same value in this column.

```java
@Column(unique = true)
private String phone;
```

> You saw `uniqueConstraints` in `@Table` earlier — that works at the **table level** and supports composite unique constraints. `unique = true` in `@Column` works at the **column level** for a single column. Both are valid approaches for single column uniqueness.

```
┌──────────────────────────────────────────────────┐
│  Single column uniqueness — two ways to do it:   │
│                                                  │
│  Option 1 (column level):                        │
│  @Column(unique = true)                          │
│  private String phone;                           │
│                                                  │
│  Option 2 (table level):                         │
│  @UniqueConstraint(columnNames = "phone")        │
│  inside @Table                                   │
└──────────────────────────────────────────────────┘
```

---

## Attribute 3 — `nullable`

Controls whether the column can hold **null values**.

```java
@Column(nullable = false)
private String name;
```

```
nullable = true  →  column CAN be empty/null    (default)
nullable = false →  column CANNOT be null, must always have a value
```

---

## Attribute 4 — `length`

Defines the **maximum length** of the column — only meaningful for `String` fields.

```java
@Column(length = 255)
private String name;
```

> Default length is 255 if not specified.

---

## Full Picture — With and Without `@Column`

```
┌──────────────────────────────────────────────────────────────────────┐
│  Java Entity                    →   DB Table                         │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  @Id                                                                 │
│  private Long id;               →   ID         (default settings)    │
│                                                                      │
│  @Column(                                                            │
│    name = "full_name",                                               │
│    unique = true,               →   FULL_NAME   (your settings)      │
│    nullable = false,                                                 │
│    length = 255)                                                     │
│  private String name;                                                │
│                                                                      │
│  private String email;          →   EMAIL       (default settings)   │
│                                                                      │
│  private String phone;          →   PHONE       (default settings)   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Quick Recap

```
┌──────────────────────────────────────────────────────────────┐
│  @Column is OPTIONAL                                         │
│  Without it → JPA maps field with default values             │
│  With it    → YOU control each column's behavior             │
│                                                              │
│  name     → custom column name in DB                         │
│  unique   → single column uniqueness                         │
│  nullable → whether null is allowed                          │
│  length   → max length for String fields (default 255)       │
└──────────────────────────────────────────────────────────────┘
```

---
# Step 6 — `@GeneratedValue` Annotation 🔢

---

## The Problem It Solves

You now know `@Id` marks a field as primary key. But here's the thing:

> By default, primary key columns are **NOT auto-filled**. You have to manually provide a unique value every time you insert a record.

```java
// Without @GeneratedValue
UserDetails user = new UserDetails();
user.setId(1L);      // YOU have to provide this
user.setName("John");
```

This is painful and error-prone. What if two inserts accidentally use the same ID? That's where `@GeneratedValue` comes in — it **transfers the responsibility of generating unique IDs to Hibernate**.

```java
// With @GeneratedValue
UserDetails user = new UserDetails();
user.setName("John");
// No need to set ID — Hibernate handles it automatically ✅
```

> `@GeneratedValue` only works with `@Id` — and only for **single primary keys**, not composite ones.

---

## The 3 Strategies (That Matter)

```
┌──────────────────────────────────────────────────────┐
│             @GeneratedValue Strategies               │
│                                                      │
│   1. GenerationType.IDENTITY                         │
│   2. GenerationType.SEQUENCE  ← most used            │
│   3. GenerationType.TABLE     ← almost never used    │
└──────────────────────────────────────────────────────┘
```

---

## Strategy 1 — `GenerationType.IDENTITY`

### What it does:
Tells the DB to **auto-increment** the ID column. Every new insert gets the next number automatically.

```java
@Entity
@Table(name = "user_details")
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
}
```

### How it behaves:

```
Insert 1 → { name: "John", phone: "1111" } → ID assigned: 1
Insert 2 → { name: "Jane", phone: "2222" } → ID assigned: 2
Insert 3 → { name: "Bob",  phone: "3333" } → ID assigned: 3

You never set the ID. DB auto-increments it. ✅
```

### Key characteristic:
```
┌─────────────────────────────────────────────────────┐
│  IDENTITY is very tightly tied to a specific table  │
│  Each table manages its OWN auto-increment counter  │
│  Table A: 1, 2, 3, 4...                             │
│  Table B: 1, 2, 3, 4... (separate counter)          │
└─────────────────────────────────────────────────────┘
```

---

## Strategy 2 — `GenerationType.SEQUENCE`

### What is a Sequence?

A **Sequence** is a separate DB object — completely independent of any table — that generates unique numbers based on rules YOU define.

```sql
-- Creating a sequence in DB directly:
CREATE SEQUENCE user_seq
    INCREMENT BY 25
    START WITH 100
    MAXVALUE 9999;
```

```
┌──────────────────────────────────────────────────────┐
│  Sequence: user_seq                                  │
│                                                      │
│  Start  : 100                                        │
│  Step   : 25                                         │
│  Max    : 9999                                       │
│                                                      │
│  Call 1 → returns 100                                │
│  Call 2 → returns 125                                │
│  Call 3 → returns 150                                │
│  ...goes on till 9999                                │
└──────────────────────────────────────────────────────┘
```

### How to use it in JPA:

Two annotations work together here — `@SequenceGenerator` and `@GeneratedValue`:

```java
@Entity
@Table(name = "user_details")
public class UserDetails {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "unique_user_seq"       // links to @SequenceGenerator below
    )
    @SequenceGenerator(
        name = "unique_user_seq",           // internal JPA name (used above)
        sequenceName = "db_seq_name",       // actual name created in DB
        initialValue = 100,                 // sequence starts from 100
        allocationSize = 5                  // prefetch 5 IDs at a time
    )
    private Long id;

    private String name;
    private String phone;
}
```

### The 4 fields of `@SequenceGenerator`:

```
┌──────────────────────────────────────────────────────────────────┐
│  name          → internal name used within JPA/Java code         │
│                  referenced in @GeneratedValue(generator = ...)  │
│                                                                  │
│  sequenceName  → actual name of the sequence in DB               │
│                  if already exists in DB → uses it               │
│                  if doesn't exist → creates it                   │
│                                                                  │
│  initialValue  → the starting number of the sequence             │
│                                                                  │
│  allocationSize→ how many IDs Hibernate prefetches at once       │
│                  and caches in memory                            │
└──────────────────────────────────────────────────────────────────┘
```

### The Magic of `allocationSize` — Caching IDs:

This is the key efficiency feature of SEQUENCE. Instead of going to the DB for every single insert, Hibernate asks for a batch of IDs upfront and caches them.

```
allocationSize = 5 means:
Hibernate asks DB → "give me 5 IDs"
DB returns        → 100, 101, 102, 103, 104
Hibernate caches  → these 5 IDs in memory

Insert 1 → uses 100 (no DB call) ✅
Insert 2 → uses 101 (no DB call) ✅
Insert 3 → uses 102 (no DB call) ✅
Insert 4 → uses 103 (no DB call) ✅
Insert 5 → uses 104 (no DB call) ✅
Insert 6 → cache empty! → goes to DB again → gets next 5 IDs
```

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  1st call  ──────────────────────────────► INSERT (id=100)       │
│  2nd call  ──────────────────────────────► INSERT (id=101)       │
│  3rd call  ──────────────────────────────► INSERT (id=102)       │
│  4th call  ──────────────────────────────► INSERT (id=103)       │
│  5th call  ──────────────────────────────► INSERT (id=104)       │
│                  ↑                                               │
│  After 5th call → Hibernate fetches next 5 values from DB seq    │
│                                                                  │
│  6th call  ──────────────────────────────► INSERT (id=105)       │
│  7th call  ──────────────────────────────► INSERT (id=106)       │
│  ...                                                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## Strategy 3 — `GenerationType.TABLE`

### What it does:
Creates a **separate table in DB** just to manage unique IDs. Uses `@TableGenerator` annotation.

```
┌──────────────────────────────────────────────────────────────────┐
│  Special table created just for ID management:                   │
│                                                                  │
│  TABLE: id_generator                                             │
│  ┌──────────────────┬────────────┐                               │
│  │  sequence_name   │  next_val  │                               │
│  ├──────────────────┼────────────┤                               │
│  │  user_seq        │     5      │                               │
│  └──────────────────┴────────────┘                               │
│                                                                  │
│  Every time you need an ID:                                      │
│  Step 1 → SELECT current value from this table                   │
│  Step 2 → UPDATE value + 1 in this table                         │
│  Step 3 → Use that value as ID                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Why it's almost never used:

```
┌──────────────────────────────────────────────────────────────────┐
│  Problems with TABLE strategy:                                   │
│                                                                  │
│  1. A whole separate table just for IDs → wasteful               │
│                                                                  │
│  2. Every insert = SELECT + UPDATE on that table → slow          │
│                                                                  │
│  3. Concurrency nightmare:                                       │
│     Two inserts happen simultaneously →                          │
│     Both try to read + update the same row →                     │
│     Needs LOCK/UNLOCK mechanism →                                │
│     Performance bottleneck ❌                                     │
│                                                                  │
│  SEQUENCE handles concurrency with an atomic counter             │
│  internally in the DB → much faster and cleaner ✅                │
└──────────────────────────────────────────────────────────────────┘
```

> The instructor says: *"In 9 years, I have never seen TABLE strategy used in the industry."*

---

## SEQUENCE vs IDENTITY — Full Comparison

This is an important section — the instructor specifically highlights the **advantages of SEQUENCE over IDENTITY**. This is a likely interview topic.

```
┌──────────────────────────────────────────────────────────────────────┐
│              SEQUENCE  vs  IDENTITY                                  │
├────────────────────────────┬─────────────────────────────────────────┤
│  SEQUENCE                  │  IDENTITY                               │
├────────────────────────────┼─────────────────────────────────────────┤
│  Custom logic              │  No custom logic                        │
│  (start point,             │  (just auto-increment                   │
│   increment, max value)    │   from 1)                               │
├────────────────────────────┼─────────────────────────────────────────┤
│  Independent of table      │  Tied to a specific table               │
│  Multiple tables can       │  Each table has its own                 │
│  share one sequence        │  separate counter                       │
├────────────────────────────┼─────────────────────────────────────────┤
│  IDs can be cached         │  No caching                             │
│  (allocationSize) →        │  Every insert = one DB call             │
│  fewer DB hits             │  to generate next ID                    │
├────────────────────────────┼─────────────────────────────────────────┤
│  Better portability        │  DB specific                            │
│  Consistent behavior       │  Auto-increment works                   │
│  across Oracle, MySQL,     │  differently in Oracle vs               │
│  Postgres etc.             │  MySQL vs Postgres                      │
└────────────────────────────┴─────────────────────────────────────────┘
```

---

## 🎯 Real World Usage — Instructor's Industry Tip

> Many big companies have a **dedicated microservice** just for generating unique IDs using sequences. Multiple other microservices call this one service to get unique IDs.

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   Microservice A ──┐                                             │
│                    │                                             │
│   Microservice B ──┼──► ID Generator Microservice ──► Sequence   │
│                    │    (centralized unique ID logic)            │
│   Microservice C ──┘                                             │
│                                                                  │
│   Why?                                                           │
│   → All ID generation logic in one place                         │
│   → Sequence is independent of any table                         │
│   → Can serve multiple services consistently                     │
└──────────────────────────────────────────────────────────────────┘
```

This is only possible with SEQUENCE — because it's **independent of any table**. IDENTITY can't do this since it's tied to a specific table.

---

## Full Summary — Step 6

```
┌──────────────────────────────────────────────────────────────────────┐
│  @GeneratedValue                                                     │
│  → Delegates ID generation responsibility to Hibernate               │
│  → Works ONLY with @Id (single primary key)                          │
│                                                                      │
│  IDENTITY  → auto-increment, table-specific, simple, no caching      │
│                                                                      │
│  SEQUENCE  → independent of table, cacheable (allocationSize),       │
│              custom logic, better portability                        │
│              USE THIS in production / real projects                  │
│                                                                      │
│  TABLE     → separate table for IDs, slow, concurrency issues,       │
│              almost never used in industry                           │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips From This Lecture

```
┌──────────────────────────────────────────────────────────────────────┐
│  Q: What is ddl-auto and what value do you use in production?        │
│  A: It controls schema management. Always use "none" in production.  │
│                                                                      │
│  Q: What is the difference between SEQUENCE and IDENTITY?            │
│  A: SEQUENCE is table-independent, supports caching via              │
│     allocationSize, has custom logic, and is more portable.          │
│     IDENTITY is table-specific and simpler but less flexible.        │
│                                                                      │
│  Q: Why does the composite key class need equals() & hashCode()?     │
│  A: JPA uses HashMap for caching (L1 & L2). HashMap relies on        │
│     equals/hashCode for key comparison. Custom classes need          │
│     these overridden manually.                                       │
│                                                                      │
│  Q: Why implement Serializable for composite keys?                   │
│  A: JPA may need to serialize the key class during distributed       │
│     caching or network transfer. Custom classes aren't               │
│     serializable by default unlike String or Long.                   │
│                                                                      │
│  Q: What is the difference between @IdClass and @EmbeddedId?         │
│  A: Both handle composite keys. @IdClass uses @Id on each field      │
│     in entity + separate key class. @EmbeddedId uses @Embeddable     │
│     on key class + single @EmbeddedId field in entity.               │
└──────────────────────────────────────────────────────────────────────┘
```

---

That's the complete lecture! All 6 steps covered. Let me know if you'd like to revisit any section or if you want a **single consolidated note** of everything! 🚀