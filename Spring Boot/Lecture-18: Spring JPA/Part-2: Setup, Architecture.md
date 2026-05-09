# Step 1 — What is ORM? Why does it exist?

---

## The Problem with JDBC (Quick Recap)

In Part 1, the instructor covered JDBC. The flow looked like this:

```
Your Application Logic  ──────────────────►  JDBC  ──►  Database
```

With JDBC, **you directly write SQL**. You have to manually:
- Write `INSERT`, `SELECT`, `UPDATE`, `DELETE` queries
- Map the result set back to Java objects yourself
- Handle connections, exceptions, etc.

It works, but it's tedious. Every time you want to talk to the DB, you're thinking in SQL, not in Java.

---

## What ORM Does — The Big Idea

ORM stands for **Object-Relational Mapping**.

The idea is simple: **what if you never had to write SQL at all, and just worked with plain Java objects?**

ORM is a framework that sits **in between** your application and JDBC:

```
Your Application Logic
        │
        ▼
  ┌───────────────────────────────┐
  │        ORM Framework          │
  │  ┌────────────┐  ┌──────────┐ │
  │  │   JPA      │─►│Hibernate │ │
  │  │(Interface) │  │  (Impl.) │ │
  │  └────────────┘  └──────────┘ │
  └───────────────────────────────┘
        │
        ▼
       JDBC
        │
        ▼
    Database
```

You talk to JPA using **Java objects**. JPA + Hibernate figure out the SQL for you internally. You don't write SQL at all (in most cases).

---

## JPA vs Hibernate — What's the Difference?

This is a very common interview question. The instructor explains it cleanly:

> Think of JPA as a **menu** (interface/contract), and Hibernate as the **kitchen** (the actual implementation that does the work).

| | JPA | Hibernate |
|---|---|---|
| What it is | An interface / specification | An implementation of JPA |
| Who defines it? | Jakarta (Java community) | Red Hat / open source |
| Does it do real work? | No, just defines rules | Yes, actual logic lives here |
| Can it be replaced? | — | Yes, with OpenJPA, EclipseLink |

So when you add the JPA dependency in Spring Boot:
- JPA (the interface) comes in
- **Hibernate is automatically pulled in as the default implementation** — you don't need to add it separately

If you want to use a different implementation like EclipseLink or OpenJPA, you can override it. But by default, **Spring Boot uses Hibernate**.

---

## The Formal Definition (in plain English)

> ORM acts as a **bridge** between your Java objects and your database tables.
> Unlike JDBC where you work with SQL, with ORM you interact with the database using **Java objects directly**.

---

## Interview Tip 💡
If someone asks **"What is the difference between JPA and Hibernate?"** — the clean answer is:
> JPA is a **specification** (just an interface, no implementation). Hibernate is one of the **implementations** of that specification. Spring Boot uses Hibernate as the default JPA implementation, but it can be swapped out.

---

# Step 2 — The Happy Flow: 4 Things You Need to Set Up JPA

---

The instructor says: *"Before diving into the internal architecture, let's first see one complete happy flow — what all things are required to run one simple program using JPA. Because once we've seen that, when we go into individual components internally, we get more clarity."*

So here are the **4 things** you need:

```
1. pom.xml          → Add the JPA dependency
2. application.properties  → DB connection config
3. Entity class     → Java class = DB Table
4. Repository interface → Your gateway to DB operations
```

Let's go through each one.

---

## 1️⃣ pom.xml — Add the Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

> **Important:** When you add this one dependency, it **automatically pulls in Hibernate** as well. You don't need to add Hibernate separately. It comes bundled with it.

---

## 2️⃣ application.properties — DB Connection Config

```properties
# Database connection
spring.datasource.url=jdbc:h2:mem:userDB
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# See the SQL queries Hibernate runs internally (for dev/testing only)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Enable H2 in-browser console (for dev/testing only)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

> The instructor is using **H2** here — an in-memory database. Meaning: every time you restart the app, all data is wiped and tables are recreated fresh. Good for testing, not for production.

> `show-sql=true` and `format_sql=true` let you **see the actual SQL queries Hibernate runs internally** in your console — very useful while learning or debugging.

---

## 3️⃣ Entity Class — Java Class = DB Table

This is the **new concept** introduced in JPA.

> An **Entity** is simply a Java class that maps to a table in your database.

```java
@Entity
public class UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    // Default constructor (MANDATORY for JPA)
    public UserDetails() {}

    public UserDetails(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
}
```

Here's how the mapping works:

```
Java World                    Database World
──────────────────────────────────────────────
UserDetails  (class)    ───►  user_details (table)
id           (field)    ───►  id           (column) ← Primary Key, Auto Generated
name         (field)    ───►  name         (column)
email        (field)    ───►  email        (column)

new UserDetails(...)    ───►  A new ROW in the table
```

> **Key point the instructor makes:**
> - The **class** = the **table**
> - Each **field** = a **column**
> - Each **new object** you create = a new **row** being inserted

> **Why is the default constructor mandatory?**
> JPA internally needs to create instances of your entity class (when fetching from DB). It does this using reflection, which requires a no-arg constructor.

---

## 4️⃣ Repository Interface — Your Gateway to DB Operations

```java
@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, Long> {
    // Nothing needed here for basic operations!
    // JpaRepository already gives you: save, findAll, findById, delete, etc.
}
```

> You create one repository interface **per entity**. You extend `JpaRepository` and tell it:
> - Which entity it works on → `UserDetails`
> - What the type of the primary key is → `Long`

`JpaRepository` already comes with a ton of built-in methods:

```
save()        → Insert or Update
findAll()     → Get all rows
findById()    → Get one row by ID
delete()      → Delete a row
count()       → Count rows
... and many more
```

> If you need a **custom query** that isn't covered by these built-in methods, you can define it right here in this interface using JPQL (Java Persistence Query Language) or HQL (Hibernate Query Language). That's the other reason this separate interface exists.

---

## Putting It All Together — The Full Flow

The instructor wires it all up with a Controller → Service → Repository pattern:

```java
// Controller
@RestController
@RequestMapping("/api/")
public class UserController {

    @Autowired
    UserDetailsService userDetailsService;

    @GetMapping("/test-jpa")
    public List<UserDetails> getUser() {
        UserDetails user = new UserDetails("xyz", "xyz@conceptandcoding.com");
        userDetailsService.saveUser(user);         // Save to DB
        return userDetailsService.getAllUsers();    // Fetch from DB
    }
}

// Service
@Service
public class UserDetailsService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    public void saveUser(UserDetails user) {
        userDetailsRepository.save(user);       // JpaRepository's built-in save
    }

    public List<UserDetails> getAllUsers() {
        return userDetailsRepository.findAll(); // JpaRepository's built-in findAll
    }
}
```

The call chain looks like this:

```
POST /api/test-jpa
        │
        ▼
  UserController
        │  calls saveUser() then getAllUsers()
        ▼
  UserDetailsService
        │  calls .save() and .findAll()
        ▼
  UserDetailsRepository  (JpaRepository)
        │  internally calls EntityManager
        ▼
  Hibernate (JPA Implementation)
        │  generates SQL
        ▼
  JDBC
        │
        ▼
  H2 Database
```

---

## What Happens When You Start the App?

Since H2 is in-memory, the moment you start the application, you'll see this in the logs:

```
Hibernate: drop table if exists user_details
Hibernate: create table user_details (id bigint, name varchar, email varchar)
```

JPA/Hibernate **automatically creates the table** for you based on your entity class. No need to write any SQL DDL yourself.

---

## Checking Data in H2 Console

Hit this URL in your browser:
```
http://localhost:8080/h2-console
```
Connect using:
- JDBC URL: `jdbc:h2:mem:userDB`
- Username: `sa`
- Password: (empty)

You can then run `SELECT * FROM USER_DETAILS` and see your data.

---

## One Interesting Thing the Instructor Points Out 🤔

When you hit `/api/test-jpa`, the app first saves a user and then fetches all users. But in the logs, you only see an **INSERT query — no SELECT query**. Why?

> Because of **first-level caching** (Persistence Context caching). Hibernate already has the entity in memory after saving it, so it doesn't go back to the DB to fetch it. It returns from the cache directly.

The instructor says: *"I will go in depth of that later — it's a very very important topic."* (That's covered in Part 3.)

---

## Summary of the 4 Steps

```
┌─────────────────────────────────────────────────┐
│  Step 1: pom.xml → Add JPA dependency           │
│  Step 2: application.properties → DB config     │
│  Step 3: @Entity class → maps to DB table       │
│  Step 4: Repository interface → DB operations   │
└─────────────────────────────────────────────────┘
           That's all you need to get started!
```

---

# Step 3 — JPA Architecture: What Happens Behind the Scenes

---

The instructor says: *"Looks very simple from outside right? But what's actually happening inside? Let's go into the architecture — all the components involved behind the scene."*

Here is the **complete JPA architecture diagram** the instructor walks through:

```
APPLICATION STARTUP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  application.properties
  (Persistence Unit)
        │
        │  1:1
        ▼
  EntityManagerFactory        ◄── created once at startup
        │
        │  1:1
        ▼
  Transaction Manager         ◄── created once at startup


API IS CALLED (Runtime)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  EntityManagerFactory
        │
        │  1:Many
        ▼
  EntityManager               ◄── created per operation/request
        │
        │  1:1
        ▼
  PersistenceContext          ◄── holds entities in memory
        │
        ▼
  Dialect  (JPQL → SQL translation)
        │
        ▼
  JDBC
        │
        ▼
  Database
```

Now let's go through each component one by one, exactly as the instructor explains.

---

## Component 1 — Persistence Unit

> *"Persistence Unit is where we provide all our configurations — DB connection, dialect, driver, etc."*

Think of it as a **configuration block** for one database. It's a logical grouping of entity classes that all share the same DB configuration.

```
Persistence Unit = {
    Which entities belong to this DB?
    What is the DB URL?
    What is the driver?
    What is the username/password?
    Which JPA provider to use? (Hibernate, OpenJPA, etc.)
    What dialect to use?
    What transaction type? (RESOURCE_LOCAL or JTA)
}
```

### Without Spring Boot — persistence.xml

If you're NOT using Spring Boot, you manually create a `persistence.xml` file:

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence" version="2.1">

    <persistence-unit name="myPersistenceUnit" 
                      transaction-type="RESOURCE_LOCAL">

        <!-- Entities that share this configuration -->
        <class>com.conceptandcoding.entity.SampleEntity1</class>
        <class>com.conceptandcoding.entity.SampleEntity2</class>

        <!-- Which JPA provider to use -->
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>

        <properties>
            <!-- Dialect -->
            <property name="hibernate.dialect" 
                      value="org.hibernate.dialect.MySQL5Dialect"/>

            <!-- DB Connection -->
            <property name="javax.persistence.jdbc.driver" 
                      value="org.h2.Driver"/>
            <property name="javax.persistence.jdbc.url"    
                      value="jdbc:h2:mem:userDB"/>
            <property name="javax.persistence.jdbc.user"     value="sa"/>
            <property name="javax.persistence.jdbc.password" value=""/>
        </properties>

    </persistence-unit>

</persistence>
```

### With Spring Boot — application.properties

Spring Boot **removes all this boilerplate**. Your `application.properties` IS your persistence unit:

```properties
# DB Connection (mandatory)
spring.datasource.url=jdbc:h2:mem:userDB
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Below all are OPTIONAL — Spring Boot auto-picks these
spring.jpa.packages-to-scan=com.conceptandcoding.entity
spring.jpa.properties.javax.persistence.provider=org.hibernate.jpa.HibernatePersistenceProvider
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.javax.persistence.transactionType=RESOURCE_LOCAL
```

> **Why are most of these optional?**
> Spring Boot does a LOT of auto-configuration. It scans for all `@Entity` classes automatically, picks Hibernate as the provider, figures out the right dialect for your DB, and defaults to `RESOURCE_LOCAL` transaction type. You only need to override these if you want something different.

---

### What if you have MORE than one Database?

This is where it gets interesting. The instructor explains:

```
One DB  →  One Persistence Unit  →  One EntityManagerFactory
Two DBs →  Two Persistence Units →  Two EntityManagerFactories
```

```
┌─────────────────────┐      ┌──────────────────────┐
│  Persistence Unit 1 │      │  Persistence Unit 2  │
│  (H2 config)        │      │  (MySQL config)      │
│  Entity1 - Entity40 │      │  Entity41 - Entity100│
└────────┬────────────┘      └──────────┬───────────┘
         │                              │
         ▼                              ▼
EntityManagerFactory1        EntityManagerFactory2
```

> **Important rule the instructor gives:**
> If you have **only one DB** → use `application.properties` (Spring Boot handles everything)
> If you have **more than one DB** → don't use `application.properties`. Instead, use a `@Configuration` class and manually define each persistence unit with its own `EntityManagerFactory`.

---

## Component 2 — EntityManagerFactory

> *"Using the Persistence Unit configuration, EntityManagerFactory object gets created during application startup itself."*

```
application.properties
(Persistence Unit)
        │
        │  Spring Boot reads this at startup
        ▼
EntityManagerFactory  ←── Java object, created ONCE, lives for app lifetime
        │
        │  Its job = create EntityManager objects whenever needed
        ▼
   EntityManager  (created on demand, per operation)
```

Think of it like this:

```
EntityManagerFactory  =  A factory/warehouse
EntityManager         =  A worker that comes out of the factory
                         whenever there's work to do
```

One `EntityManagerFactory` per persistence unit (per DB). It's **expensive to create**, so it's created once at startup and reused.

### If you want to create it manually (for multiple DBs):

```java
@Configuration
public class H2Config {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setJdbcUrl("jdbc:h2:mem:userDB");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
        return adapter;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter) {

        LocalContainerEntityManagerFactoryBean emf = 
            new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(jpaVendorAdapter);
        emf.setPackagesToScan("com.conceptandcoding.learningspringboot.jpa");
        emf.setPersistenceUnitName("h2Factory"); // unique name

        return emf;
    }
}
```

> If you want a second DB (say MySQL), you create another `@Configuration` class — say `MySQLConfig` — and repeat the same pattern with MySQL-specific settings. Each config = one persistence unit = one `EntityManagerFactory`.

---

## Component 3 — Transaction Manager

> *"After EntityManagerFactory is created, based on the transaction type you specified, a Transaction Manager object also gets created during startup."*

The Transaction Manager is responsible for **managing begin, commit, and rollback** of transactions.

There are **two types**:

```
┌─────────────────────────────────────────────────────────┐
│             Transaction Manager Types                   │
├──────────────────────────┬──────────────────────────────┤
│   RESOURCE_LOCAL         │   JTA                        │
│   (Default)              │   (Java Transaction API)     │
├──────────────────────────┼──────────────────────────────┤
│ Transaction scoped to    │ Transaction can span         │
│ ONE DB only              │ MULTIPLE DBs                 │
│                          │                              │
│ Uses:                    │ Uses:                        │
│ JpaTransactionManager    │ JtaTransactionManager        │
│                          │                              │
│ Simple, most common      │ Complex, uses 2-phase commit │
└──────────────────────────┴──────────────────────────────┘
```

### Relationship between all three startup components:

```
For each DB you have:

  Persistence Unit
       │
       │ creates (1:1)
       ▼
  EntityManagerFactory
       │
       │ paired with (1:1)
       ▼
  Transaction Manager
```

> The instructor says: *"JTA is a big topic by itself — it involves concepts like Atomikos, XADataSource, two-phase commit. I won't cover it today, it's out of scope for this video."*

### Creating Transaction Manager manually:

```java
@Bean
public JpaTransactionManager transactionManager(
        EntityManagerFactory entityManagerFactory) {

    return new JpaTransactionManager(entityManagerFactory);
}
```

You pass the `EntityManagerFactory` to it — because the transaction manager needs to know **which DB** it's managing transactions for.

---

## The Big Picture — Everything at Startup

```
╔══════════════════════════════════════════════════════╗
║           HAPPENS AT APPLICATION STARTUP             ║
╠══════════════════════════════════════════════════════╣
║                                                      ║
║  application.properties                              ║
║  (Persistence Unit config)                           ║
║          │                                           ║
║          ▼                                           ║
║  EntityManagerFactory  ←── created (1 per DB)        ║
║          │                                           ║
║          ▼                                           ║
║  Transaction Manager   ←── created (1 per EMF)       ║
║                                                      ║
║  Your application is now READY to serve requests     ║
╚══════════════════════════════════════════════════════╝

╔══════════════════════════════════════════════════════╗
║           HAPPENS WHEN AN API IS CALLED              ║
╠══════════════════════════════════════════════════════╣
║                                                      ║
║  EntityManagerFactory                                ║
║          │                                           ║
║          ▼                                           ║
║  EntityManager  ←── created per operation            ║
║          │                                           ║
║          ▼                                           ║
║  PersistenceContext  ←── holds entities in memory    ║
║          │                                           ║
║          ▼                                           ║
║  Dialect  ←── translates JPQL → SQL                  ║
║          │                                           ║
║          ▼                                           ║
║  JDBC  →  Database                                   ║
╚══════════════════════════════════════════════════════╝
```

---

## What is Dialect and Why is it Needed?

> *"Hibernate works in JPQL or HQL. But JDBC only understands SQL. Somebody needs to translate. That's the dialect."*

```
Your Code (JPQL/HQL)
        │
        ▼
     Dialect          ← translates to proper SQL for your specific DB
        │
        ▼
    SQL Query
        │
        ▼
      JDBC
        │
        ▼
    Database
```

Different databases have slightly different SQL syntax. For example, pagination in MySQL vs Oracle is written differently. The dialect handles all of that translation automatically.

> Spring Boot auto-picks the right dialect based on which DB driver you've added. So you usually don't need to specify it manually.

---

## Interview Tips 💡

**Q: What is a Persistence Unit?**
> It's a logical grouping of entity classes that share the same DB configuration — URL, driver, dialect, JPA provider, transaction type, etc. In Spring Boot, `application.properties` acts as the persistence unit.

**Q: What is the relationship between Persistence Unit and EntityManagerFactory?**
> It's 1:1. One Persistence Unit creates one EntityManagerFactory. If you have two databases, you need two persistence units and two EntityManagerFactories.

**Q: What is the difference between RESOURCE_LOCAL and JTA?**
> `RESOURCE_LOCAL` manages transactions scoped to a single DB. `JTA` (Java Transaction API) manages transactions that can span across multiple databases using two-phase commit internally.

**Q: When should you NOT use application.properties for JPA config?**
> When your application connects to more than one database. In that case, use `@Configuration` classes and manually define each `EntityManagerFactory` and `TransactionManager`.

---

# Step 4 — EntityManager & Persistence Context (Runtime Behavior)

---

The instructor says: *"All the above steps happened during application startup. Now let's talk about what happens when your API actually gets called — and that's where EntityManager and PersistenceContext come into the picture."*

---

## What is EntityManager?

> *"EntityManager is an interface in JPA that provides methods to perform CRUD operations on entities. Without an EntityManager object, you simply cannot interact with the DB."*

Think of it like this:

```
EntityManager  =  Your session with the database

Creating an EntityManager  =  Opening a session with DB
Closing an EntityManager   =  Closing that session
```

In Hibernate terms, EntityManager is called a **Session**. Same concept, different name:

```
JPA Term          Hibernate Term
─────────────────────────────────
EntityManager  =  Session
persist()      =  save()
merge()        =  update()
find()         =  get()
```

---

## Methods EntityManager Provides

```
┌─────────────────────────────────────────────────┐
│              EntityManager Interface            │
├─────────────────────────────────────────────────┤
│  persist()      → Save/Insert entity to DB      │
│  merge()        → Update existing entity        │
│  find()         → Fetch entity by ID            │
│  remove()       → Delete entity from DB         │
│  createQuery()  → Run custom JPQL queries       │
└─────────────────────────────────────────────────┘
          ▲
          │  implemented by
          │
┌─────────────────────────────────────────────────┐
│         Hibernate (SessionImpl.java)            │
│  → provides actual logic for each method above  │
└─────────────────────────────────────────────────┘
```

> EntityManager is **just an interface** — no actual logic lives inside it. The real logic is provided by your JPA provider. In Spring Boot's case, that's **Hibernate's SessionImpl class**.

---

## How EntityManager Fits in the Full Picture

```
Your Code
    │
    │  calls
    ▼
JpaRepository.save()          ← you interact here (most of the time)
    │
    │  internally calls
    ▼
EntityManager.persist()       ← actual JPA interface method
    │
    │  implemented by
    ▼
Hibernate (SessionImpl)       ← real logic lives here
    │
    │  uses
    ▼
Dialect                       ← translates JPQL → SQL
    │
    ▼
JDBC
    │
    ▼
Database
```

> So `JpaRepository` is just a **convenient wrapper** on top of `EntityManager`. When you call `.save()` on your repository, internally it's calling `entityManager.persist()` behind the scenes.

---

## EntityManager is Created Per Operation (1:Many with Factory)

```
EntityManagerFactory          ← created ONCE at startup, lives forever
        │
        │  creates on demand (1:Many)
        ▼
EntityManager 1               ← created for operation/request 1
EntityManager 2               ← created for operation/request 2
EntityManager N               ← created for operation/request N
```

> The factory is **heavy** (expensive to create) — created once.
> The EntityManager is **lightweight** — created whenever needed, closed when done.

---

## What is PersistenceContext?

> *"For every EntityManager object that gets created, a PersistenceContext is also created alongside it. Consider it as a first-level cache — a placeholder for all the entities the EntityManager is currently working on."*

```
EntityManager
    │
    │  has its own (1:1)
    ▼
PersistenceContext
    │
    ├── UserDetails (id=1, name="xyz")     ← entity being managed
    ├── OrderDetails (id=5, ...)           ← another entity
    └── InvoiceDetails (id=2, ...)         ← another entity
```

Think of PersistenceContext as a **temporary in-memory workspace**:

```
┌─────────────────────────────────────────┐
│           PersistenceContext            │
│         (In-memory workspace)           │
│                                         │
│  UserDetails obj  → id=1, name="xyz"    │
│  OrderDetails obj → id=5, amt=500       │
│                                         │
│  Changes happen HERE first              │
│  DB gets updated only when flushed      │
└─────────────────────────────────────────┘
              │
              │  flush() syncs to DB
              ▼
          Database
```

> All your changes (update name, delete record, etc.) happen **inside the PersistenceContext first**. The actual DB is only updated when a **flush** happens — either automatically at transaction commit, or manually triggered.

---

## A Concrete Example — What Actually Happens

Let's say you want to **update** a user's name:

```
Step 1: You call entityManager.find(UserDetails.class, 1L)
            │
            ▼
        PersistenceContext checks:
        "Do I already have UserDetails with id=1?"
            │
            ├── YES → return from cache (no DB call!) ← first level caching
            │
            └── NO  → go to DB, fetch it, store it in PersistenceContext
                        then return it to you

Step 2: You do  user.setName("ABC")   ← change happens in PersistenceContext
            (DB still has old name "XYZ")

Step 3: Transaction commits → flush() happens automatically
            │
            ▼
        Hibernate generates:
        UPDATE user_details SET name='ABC' WHERE id=1
            │
            ▼
        DB is now updated
```

> This is exactly why the instructor saw only an INSERT but no SELECT in the logs earlier. After saving, Hibernate already had the entity in PersistenceContext. When `findAll()` was called, it served from the cache — no need to go to the DB again.

---

## Why JpaRepository Over Direct EntityManager?

The instructor explains this really well. Let's compare:

```
┌─────────────────────────────────────────────────────────────┐
│                    JpaRepository                            │
├─────────────────────────────────────────────────────────────┤
│ ✅ Built-in methods: save, findAll, findById, delete, etc.   │
│ ✅ Pagination and sorting built-in                           │
│ ✅ Auto adds @Transactional on insert/update/delete          │
│ ✅ Auto manages EntityManager lifecycle (open/close)         │
│ ✅ Batch operations: deleteAll, saveAll, etc.                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Direct EntityManager                       │
├─────────────────────────────────────────────────────────────┤
│ ❌ No findAll, deleteAll out of the box                      │
│ ❌ No pagination/sorting built in                            │
│ ❌ YOU must add @Transactional manually                      │
│ ❌ YOU must manage EntityManager open/close                  │
│ ✅ More control if you need very custom behavior             │
└─────────────────────────────────────────────────────────────┘
```

---

## The @Transactional Requirement — Very Important

The instructor demonstrates this with a live example.

For **insert, update, delete** → EntityManager **requires an open transaction**. If there's no transaction, it throws an exception.

### Without @Transactional → FAILS ❌

```java
@Service
public class UserDetailsService {

    @PersistenceContext
    EntityManager entityManager;

    public void saveUser(UserDetails user) {
        entityManager.persist(user);   // ❌ THROWS EXCEPTION
        // TransactionRequiredException:
        // No EntityManager with actual transaction available for current thread
    }
}
```

### With @Transactional → WORKS ✅

```java
@Service
public class UserDetailsService {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional                     // ✅ transaction is now open
    public void saveUser(UserDetails user) {
        entityManager.persist(user);   // works perfectly
    }
}
```

> This is exactly why `JpaRepository` is convenient — **it already has `@Transactional` on all its insert/update/delete methods internally**. You don't have to think about it.

Here's the actual Spring source code the instructor shows:

```java
@Transactional                         // ← already there for you!
public <S extends T> S save(S entity) {
    if (this.entityInformation.isNew(entity)) {
        this.entityManager.persist(entity);   // new entity → persist
        return entity;
    } else {
        return this.entityManager.merge(entity); // existing → merge/update
    }
}
```

> Notice: Spring's `save()` is smart — if the entity is **new** (no ID yet), it calls `persist()`. If it **already exists** (has an ID), it calls `merge()` to update it.

> **Read operations** like `find()` and `findAll()` do **NOT** require a transaction. Only write operations do.

---

## The Complete Runtime Picture

```
╔═══════════════════════════════════════════════════════════╗
║                  API CALLED (Runtime)                     ║
╠═══════════════════════════════════════════════════════════╣
║                                                           ║
║  EntityManagerFactory                                     ║
║          │                                                ║
║          │  creates                                       ║
║          ▼                                                ║
║  EntityManager ◄──────────────────────────────────────    ║
║          │                          │                     ║
║          │  has its own             │ implemented by      ║
║          ▼                          ▼                     ║
║  PersistenceContext          Hibernate (SessionImpl)      ║
║  (in-memory workspace)               │                    ║
║          │                           │ uses               ║
║          │                           ▼                    ║
║          │                        Dialect                 ║
║          │                    (JPQL → SQL)                ║
║          │                           │                    ║
║          └──────── flush() ─────────►▼                    ║
║                                    JDBC                   ║
║                                      │                    ║
║                                      ▼                    ║
║                                  Database                 ║
╚═══════════════════════════════════════════════════════════╝
```

---

## Interview Tips 💡

**Q: What is EntityManager?**
> It's the core JPA interface used to interact with the database. It provides methods like `persist()`, `merge()`, `find()`, `remove()`, and `createQuery()`. Its methods are implemented by the JPA provider (Hibernate in Spring Boot's case).

**Q: What is PersistenceContext?**
> It's an in-memory workspace created for each EntityManager. It holds all the entities the EntityManager is currently working on. It also acts as a first-level cache — if you fetch the same entity twice, the second call is served from PersistenceContext without hitting the DB.

**Q: Why do insert/update/delete need @Transactional but reads don't?**
> Because write operations need to be atomic — they must either fully complete or fully rollback. Reads are generally safe without a transaction. EntityManager enforces this by throwing a `TransactionRequiredException` if you try to write without an active transaction.

**Q: Can you use EntityManager directly instead of JpaRepository?**
> Yes, but JpaRepository gives you a lot for free — built-in CRUD methods, pagination, sorting, automatic `@Transactional` on writes, and automatic EntityManager lifecycle management. Using EntityManager directly means you handle all of that yourself.

---

# Step 5 — Entity Lifecycle Inside PersistenceContext

---

The instructor says: *"Let me tell you the lifecycle of an entity inside a PersistenceContext. This is very important to understand because PersistenceContext manages the lifecycle of every entity it is working on."*

---

## The Big Picture — All States an Entity Can Be In

```
                    new UserDetails()
                          │
                          ▼
                 ┌─────────────────┐
                 │      NEW        │
                 │  (Transient)    │
                 └────────┬────────┘
                          │
                          │  entityManager.persist()
                          ▼
                 ┌─────────────────┐
          ┌─────►│    MANAGED      │◄──────────────┐
          │      │  (Persistent)   │               │
          │      └────────┬────────┘               │
          │               │                        │
          │    ┌──────────┴──────────┐             │
          │    │                     │             │
          │  remove()            flush()/        merge()
          │    │               find()/get()        │
          │    ▼                     │             │
          │  ┌──────────┐           ▼              │
          │  │  REMOVED │        Database          │
          │  └────┬─────┘                          │
          │       │                                │
          │  persist()    detach()/close()         │
          └───────┘            │                   │
                               ▼                   │
                      ┌─────────────────┐          │
                      │    DETACHED     │──────────┘
                      └─────────────────┘
```

There are **4 states** an entity can be in:

```
1. NEW (Transient)   → Object created, JPA doesn't know about it yet
2. MANAGED           → JPA/PersistenceContext is tracking it
3. REMOVED           → Marked for deletion, not yet deleted from DB
4. DETACHED          → Was managed, but now disconnected from PersistenceContext
```

Let's go through each state exactly as the instructor explains.

---

## State 1 — NEW (Transient)

```java
UserDetails user = new UserDetails("xyz", "xyz@gmail.com");
```

```
┌─────────────────────────────────────────────┐
│              NEW / TRANSIENT                │
│                                             │
│  UserDetails user = new UserDetails(...)    │
│                                             │
│  → Object exists in Java memory (heap)      │
│  → PersistenceContext has NO idea it exists │
│  → DB has NO idea it exists                 │
│  → JPA is NOT tracking it at all            │
└─────────────────────────────────────────────┘
```

> It's just a plain Java object at this point. Nothing special about it. JPA doesn't know it exists, DB doesn't know it exists.

---

## State 2 — MANAGED (Persistent)

This is the most important state. There are **two ways** an entity enters the MANAGED state:

### Way 1 — You persist a new entity

```java
entityManager.persist(user);
```

```
NEW object
    │
    │  entityManager.persist(user)
    ▼
PersistenceContext          ← entity is NOW stored here
    │
    │  at flush / transaction commit
    ▼
Database                    ← INSERT query runs
```

### Way 2 — You fetch an existing entity from DB

```java
UserDetails user = entityManager.find(UserDetails.class, 1L);
```

```
entityManager.find(UserDetails.class, 1L)
    │
    ▼
PersistenceContext checks:
"Do I already have UserDetails with id=1?"
    │
    ├── YES → return from cache (NO DB call)   ← first level cache
    │
    └── NO  → go to DB
                │
                ▼
            Database → fetch UserDetails(id=1)
                │
                ▼
            Store it in PersistenceContext    ← now it's MANAGED
                │
                ▼
            Return to your code
```

### What being MANAGED means:

```
┌─────────────────────────────────────────────────┐
│              MANAGED State                      │
│                                                 │
│  PersistenceContext is NOW tracking this entity │
│                                                 │
│  → Any changes you make to the object           │
│    are automatically detected                   │
│  → JPA will sync changes to DB at flush time    │
│  → You don't need to call save() again!         │
│    (Hibernate's dirty checking handles it)      │
└─────────────────────────────────────────────────┘
```

#### Example of automatic change detection (Dirty Checking):

```java
@Transactional
public void updateUser() {
    // fetch → entity is now MANAGED
    UserDetails user = entityManager.find(UserDetails.class, 1L);

    // change the name → happens inside PersistenceContext
    user.setName("ABC");   // DB still has "XYZ" at this point

    // NO need to call save() or persist() again!
    // When transaction commits → flush() happens automatically
    // Hibernate detects the change and runs:
    // UPDATE user_details SET name='ABC' WHERE id=1
}
```

> This automatic detection of changes is called **Dirty Checking** — Hibernate compares the current state of the entity with its original snapshot taken when it first entered the PersistenceContext. If anything changed, it generates an UPDATE automatically.

---

## State 3 — REMOVED

```java
entityManager.remove(user);
```

```
MANAGED entity
    │
    │  entityManager.remove(user)
    ▼
┌─────────────────────────────────────────────────┐
│              REMOVED State                      │
│                                                 │
│  → Entity is marked for deletion                │
│  → It's still inside PersistenceContext         │
│  → DB is NOT updated yet                        │
│  → Actual DELETE runs only at flush()           │
└─────────────────────────────────────────────────┘
    │
    │  flush() / transaction commit
    ▼
Database → DELETE query runs → row is gone
```

### Important — You can UNDO a remove before flush:

```java
entityManager.remove(user);   // marked for deletion

// changed your mind? call persist() again!
entityManager.persist(user);  // back to MANAGED state!

// since flush hasn't happened yet, DB was never touched
// the DELETE never actually ran
```

```
MANAGED
   │
   │ remove()
   ▼
REMOVED ──── persist() ────► MANAGED   (undo is possible before flush!)
   │
   │ flush()
   ▼
Database (DELETE runs — now it's permanent)
```

> The instructor says: *"Before flush happens, if you change your mind, you can call persist() again and bring it back to managed state. The DB was never touched yet."*

---

## State 4 — DETACHED

```java
entityManager.detach(user);   // detach one specific entity
// OR
entityManager.close();         // closes EntityManager → ALL entities become detached
```

```
MANAGED entity
    │
    │  detach(user) or close()
    ▼
┌─────────────────────────────────────────────────┐
│              DETACHED State                     │
│                                                 │
│  → Entity is removed from PersistenceContext    │
│  → JPA is NO longer tracking it                 │
│  → Changes you make will NOT auto-sync to DB    │
│  → YOU are now responsible for it               │
└─────────────────────────────────────────────────┘
```

> The instructor says: *"Once you detach, it means you are taking the responsibility for that entity. JPA won't manage it anymore. If you make changes, you have to handle syncing to DB yourself."*

### Coming back from DETACHED → MANAGED:

```java
entityManager.merge(user);   // brings detached entity back under JPA control
```

```
DETACHED
    │
    │  entityManager.merge(user)
    ▼
MANAGED   ← JPA is tracking it again
```

> `merge()` is essentially saying: *"Hey JPA, I've been working on this entity outside your control. Please take it back and sync it with whatever's in the DB."*

---

## Complete State Diagram — All Transitions Together

```
  new UserDetails()
        │
        ▼
  ┌───────────┐
  │    NEW    │
  │(Transient)│
  └─────┬─────┘
        │ persist()
        ▼
  ┌───────────┐◄────────────────────────────────┐
  │  MANAGED  │                                 │
  │(Persistent│──── flush() ──────►  DB         │
  │           │◄─── find()/get() ── DB          │
  └─────┬─────┘                                 │
        │                                       │
     remove()                               merge()
        │                                       │
        ▼                                       │
  ┌───────────┐                                 │
  │  REMOVED  │── persist() ──► MANAGED         │
  │           │── flush() ───► DB DELETE        │
  └─────┬─────┘                                 │
        │                                       │
   detach()/close()              detach()/close()
        │                                │
        └──────────────┬─────────────────┘
                       ▼
                ┌───────────┐
                │ DETACHED  │──── merge() ────► MANAGED
                └───────────┘
```

---

## How All 4 States Map to Real Code

```java
@Transactional
public void demonstrateLifecycle() {

    // ── STATE 1: NEW ──────────────────────────────────
    UserDetails user = new UserDetails("xyz", "xyz@gmail.com");
    // Just a Java object. JPA doesn't know it exists.

    // ── STATE 2: MANAGED ──────────────────────────────
    entityManager.persist(user);
    // Now PersistenceContext is tracking it.
    // DB not updated yet.

    user.setName("ABC");
    // Change detected automatically (dirty checking).
    // DB still not updated yet.

    // flush() happens at transaction commit →
    // INSERT runs, then UPDATE runs (or just INSERT with final value)

    // ── FETCH → also enters MANAGED ───────────────────
    UserDetails fetched = entityManager.find(UserDetails.class, 1L);
    // Checks PersistenceContext first → if found, no DB call
    // If not found → fetches from DB → stores in PersistenceContext

    // ── STATE 3: REMOVED ──────────────────────────────
    entityManager.remove(fetched);
    // Marked for deletion. DB not touched yet.

    entityManager.persist(fetched);
    // Changed mind → back to MANAGED. No DELETE ever ran.

    // ── STATE 4: DETACHED ─────────────────────────────
    entityManager.detach(fetched);
    // JPA stops tracking it. Your responsibility now.

    fetched.setName("PQR");
    // This change will NOT auto-sync to DB.

    entityManager.merge(fetched);
    // Hand it back to JPA → MANAGED again.
    // JPA will now sync this change to DB.
}
```

---

## Why PersistenceContext = First Level Cache

The instructor keeps referring to PersistenceContext as a first-level cache. Here's why:

```
First Call:
entityManager.find(UserDetails.class, 1L)
    │
    ▼
PersistenceContext: "I don't have id=1"
    │
    ▼
DB → fetch → store in PersistenceContext → return

Second Call (same EntityManager):
entityManager.find(UserDetails.class, 1L)
    │
    ▼
PersistenceContext: "I already have id=1!" → return immediately
    │
    NO DB call at all! ← this is the caching
```

> This is **automatic** — you don't do anything special. As long as you're using the same EntityManager (same session), the same entity won't be fetched from DB twice.

> The instructor says: *"First level caching is a very very important topic — I will go in depth of it in Part 3."*

---

## Interview Tips 💡

**Q: What are the 4 states of an entity in JPA?**
> New/Transient (just created, JPA unaware), Managed/Persistent (PersistenceContext tracking it), Removed (marked for deletion, not yet deleted), Detached (was managed, now disconnected from PersistenceContext).

**Q: What is Dirty Checking in JPA?**
> When an entity is in MANAGED state, Hibernate keeps a snapshot of its original values. At flush time, it compares the current state with the snapshot. If anything changed, it automatically generates an UPDATE query. You don't need to call save() again.

**Q: What is the difference between detach() and remove()?**
> `remove()` marks the entity for **deletion from the DB**. `detach()` simply **disconnects the entity from PersistenceContext** — it's still in the DB, JPA just stops tracking it.

**Q: What is the difference between persist() and merge()?**
> `persist()` is for **new entities** — it adds them to PersistenceContext for the first time. `merge()` is for **detached entities** — it reattaches them back to PersistenceContext so JPA can manage them again.

**Q: What is first-level caching?**
> PersistenceContext acts as a first-level cache. Within the same EntityManager session, if you fetch the same entity twice, the second fetch is served from PersistenceContext — no DB call happens. This is automatic and always on.

---

# Step 6 — Complete Summary & Interview Cheat Sheet

---

The instructor says: *"I hope that with this you would be able to set up JPA, run one flow, and while running the flow you at least know how it is running internally."*

This final step ties everything together into one clean reference.

---

## The Complete JPA Story — From Start to Finish

```
╔══════════════════════════════════════════════════════════════════╗
║                    BEFORE YOU START                              ║
║                  (One time setup)                                ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  1. Add JPA dependency in pom.xml                                ║
║     └── Hibernate gets pulled in automatically                   ║
║                                                                  ║
║  2. Configure DB in application.properties                       ║
║     └── This acts as your Persistence Unit                       ║
║                                                                  ║
║  3. Create @Entity classes                                       ║
║     └── Each class = one DB table                                ║
║         Each field = one column                                  ║
║         Each object = one row                                    ║
║                                                                  ║
║  4. Create Repository interfaces                                 ║
║     └── Extends JpaRepository                                    ║
║         Gets save, findAll, findById, delete... for free         ║
║                                                                  ║
╠══════════════════════════════════════════════════════════════════╣
║                  APPLICATION STARTUP                             ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  Persistence Unit (application.properties)                       ║
║          │                                                       ║
║          │ creates (1:1)                                         ║
║          ▼                                                       ║
║  EntityManagerFactory    ← heavy, created once, lives forever    ║
║          │                                                       ║
║          │ paired with (1:1)                                     ║
║          ▼                                                       ║
║  Transaction Manager     ← RESOURCE_LOCAL (default) or JTA      ║
║                                                                  ║
║  App is now ready to serve requests                              ║
║                                                                  ║
╠══════════════════════════════════════════════════════════════════╣
║                   API IS CALLED (Runtime)                        ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  Your Code → JpaRepository → EntityManager → Hibernate           ║
║                                    │                             ║
║                                    ▼                             ║
║                            PersistenceContext                    ║
║                            (in-memory workspace)                 ║
║                                    │                             ║
║                               flush()                            ║
║                                    │                             ║
║                                    ▼                             ║
║                    Dialect (JPQL/HQL → SQL)                      ║
║                                    │                             ║
║                                    ▼                             ║
║                                  JDBC                            ║
║                                    │                             ║
║                                    ▼                             ║
║                                Database                          ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## Every Component — One Line Summary

```
┌──────────────────────────┬─────────────────────────────────────────────┐
│  Component               │  What it is                                 │
├──────────────────────────┼─────────────────────────────────────────────┤
│  ORM                     │  Bridge between Java objects and DB tables  │
│  JPA                     │  Specification/interface (no logic)         │
│  Hibernate               │  Implementation of JPA (actual logic)       │
│  Persistence Unit        │  Configuration block for one DB             │
│  application.properties  │  Your persistence unit in Spring Boot       │
│  EntityManagerFactory    │  Factory to create EntityManager objects     │
│  Transaction Manager     │  Manages begin/commit/rollback of txns      │
│  EntityManager           │  Your session with the DB (core JPA API)    │
│  PersistenceContext      │  In-memory workspace + first level cache     │
│  Dialect                 │  Translates JPQL/HQL → proper SQL           │
│  JpaRepository           │  Convenient wrapper over EntityManager      │
└──────────────────────────┴─────────────────────────────────────────────┘
```

---

## Entity Lifecycle — Quick Reference

```
┌───────────────┬──────────────────┬──────────────────┬──────────────────┐
│  State        │  How to enter    │  JPA tracking?   │  DB updated?     │
├───────────────┼──────────────────┼──────────────────┼──────────────────┤
│  NEW          │  new Entity()    │  ❌ No           │  ❌ No           │
│  MANAGED      │  persist()       │  ✅ Yes          │  At flush()      │
│               │  find()/get()    │                  │                  │
│  REMOVED      │  remove()        │  ✅ Yes          │  At flush()      │
│               │                  │  (marked only)   │  (DELETE runs)   │
│  DETACHED     │  detach()        │  ❌ No           │  ❌ No           │
│               │  close()         │                  │  (your job now)  │
└───────────────┴──────────────────┴──────────────────┴──────────────────┘

Transitions:
NEW      ──persist()──►  MANAGED
MANAGED  ──remove()───►  REMOVED  ──persist()──► MANAGED (undo!)
MANAGED  ──detach()───►  DETACHED ──merge()────► MANAGED
REMOVED  ──detach()───►  DETACHED
DETACHED ──merge()────►  MANAGED
```

---

## JpaRepository vs EntityManager — Quick Reference

```
┌────────────────────────┬──────────────┬──────────────────┐
│  Feature               │JpaRepository │  EntityManager   │
├────────────────────────┼──────────────┼──────────────────┤
│  Built-in CRUD         │  ✅ Yes      │  Basic only      │
│  findAll, deleteAll    │  ✅ Yes      │  ❌ Manual        │
│  Pagination/Sorting    │  ✅ Yes      │  ❌ Manual        │
│  Auto @Transactional   │  ✅ Yes      │  ❌ You add it    │
│  Auto lifecycle mgmt   │  ✅ Yes      │  ❌ You manage it │
│  Custom JPQL queries   │  ✅ Yes      │  ✅ Yes           │
│  Fine-grained control  │  Limited     │  ✅ Full control  │
└────────────────────────┴──────────────┴──────────────────┘

Recommendation: Always prefer JpaRepository unless you
need very specific low-level control.
```

---

## Transaction Types — Quick Reference

```
┌─────────────────────┬────────────────────────────────────────┐
│  RESOURCE_LOCAL     │  JTA                                   │
│  (default)          │  (Java Transaction API)                │
├─────────────────────┼────────────────────────────────────────┤
│  One DB only        │  Multiple DBs                          │
│  Simple             │  Complex                               │
│  JpaTransactionMgr  │  JtaTransactionManager                 │
│  Most common        │  Distributed systems                   │
│                     │  Uses 2-phase commit internally        │
│                     │  Needs Atomikos / XADataSource         │
└─────────────────────┴────────────────────────────────────────┘
```

---

## Multiple DB Setup — Quick Reference

```
Single DB (most common):
─────────────────────────
Use application.properties
Spring Boot handles everything automatically


Multiple DBs:
─────────────────────────
Use @Configuration classes (one per DB)
Manually create:
  → DataSource
  → JpaVendorAdapter
  → EntityManagerFactory  (one per DB)
  → TransactionManager    (one per DB)
```

---

## The Flow When You Call .save() — Full Depth

```
userDetailsRepository.save(user)
        │
        ▼
SimpleJpaRepository.save()        ← Spring's implementation
  @Transactional                  ← transaction opened here
        │
        ├── is new entity?
        │     YES → entityManager.persist(user)
        │     NO  → entityManager.merge(user)
        │
        ▼
  EntityManager (interface)
        │
        ▼
  Hibernate SessionImpl           ← actual implementation
        │
        ▼
  PersistenceContext              ← entity stored here
        │
        │  (transaction commits)
        ▼
  flush() triggered
        │
        ▼
  Dialect                         ← generates proper SQL
        │
        ▼
  JDBC
        │
        ▼
  Database ✅
```

---

## Everything the Instructor Said to Remember

These are the key takeaways the instructor specifically emphasized throughout the video:

**On ORM:**
> JPA is the interface, Hibernate is the default implementation. You can swap Hibernate for OpenJPA or EclipseLink if needed.

**On Persistence Unit:**
> `application.properties` = one persistence unit = one DB. More than one DB? Use `@Configuration` classes.

**On EntityManagerFactory:**
> Created once at startup. Heavy to create. One per persistence unit. Acts as a factory to produce EntityManager objects on demand.

**On Transaction Manager:**
> Created right after EntityManagerFactory at startup. One per EntityManagerFactory. RESOURCE_LOCAL by default — scoped to one DB only.

**On EntityManager:**
> This is the core. Without an EntityManager object, you cannot interact with the DB at all. JpaRepository is just a convenient wrapper on top of it.

**On PersistenceContext:**
> Think of it as the EntityManager's personal in-memory workspace. Holds all entities being worked on. Also acts as first-level cache. Manages entity lifecycle.

**On @Transactional:**
> Insert, Update, Delete MUST have an active transaction or EntityManager throws an exception. JpaRepository handles this automatically. If you use EntityManager directly, you must add @Transactional yourself.

**On Dirty Checking:**
> Once an entity is MANAGED, you don't need to call save() again after changing it. Hibernate detects changes automatically at flush time and generates the UPDATE query.

**On first-level caching:**
> Within the same EntityManager session, the same entity is never fetched from DB twice. PersistenceContext serves it from memory. Deep dive coming in Part 3.

---

## Interview Cheat Sheet — Most Asked Questions

**Q1: What is JPA?**
> JPA (Java Persistence API) is a specification that defines how Java objects should be mapped to relational database tables. It provides a standard interface — actual implementation is done by providers like Hibernate.

**Q2: What is the difference between JPA and Hibernate?**
> JPA is just an interface/specification with no implementation. Hibernate is one concrete implementation of JPA. Spring Boot uses Hibernate as the default JPA provider.

**Q3: What is an Entity in JPA?**
> A Java class annotated with `@Entity` that maps to a DB table. Each field maps to a column, and each object instance represents a row.

**Q4: What is PersistenceContext?**
> An in-memory workspace tied to an EntityManager. It tracks all entities the EntityManager is working on, manages their lifecycle, and acts as a first-level cache.

**Q5: What are the 4 entity states in JPA?**
> New/Transient, Managed/Persistent, Removed, Detached.

**Q6: What is Dirty Checking?**
> Hibernate's ability to automatically detect changes made to MANAGED entities and generate UPDATE queries at flush time — without you calling save() again.

**Q7: What is the difference between persist() and merge()?**
> `persist()` is for brand new entities entering the PersistenceContext for the first time. `merge()` is for detached entities being reattached back to PersistenceContext.

**Q8: What is the difference between detach() and remove()?**
> `remove()` marks entity for deletion from the DB. `detach()` just disconnects it from PersistenceContext — it still exists in the DB.

**Q9: Why use JpaRepository over direct EntityManager?**
> JpaRepository gives you built-in CRUD, pagination, sorting, automatic `@Transactional` on write operations, and automatic EntityManager lifecycle management — all for free.

**Q10: What is Dialect in JPA?**
> A component that translates JPQL/HQL queries into database-specific SQL. Different databases have different SQL syntax — dialect handles that translation automatically.

**Q11: What is RESOURCE_LOCAL vs JTA?**
> RESOURCE_LOCAL transactions are scoped to a single DB. JTA transactions can span multiple databases using two-phase commit.

**Q12: What is first-level caching in JPA?**
> Within the same EntityManager session, if the same entity is requested twice, the second request is served from PersistenceContext — no DB call happens. It's automatic and always enabled.

---

## What's Coming in Part 3

The instructor ends with:

> *"Now we will go with Part 3 of JPA which is first-level caching. See you soon!"*

```
Part 3 will cover:
→ First-level caching in depth
→ How PersistenceContext serves as cache
→ Exactly when DB is hit and when it's not
→ How this impacts your application behavior
```

---

## Your Complete Learning Path So Far

```
Part 1 → Plain JDBC vs Spring Boot JDBC
Part 2 → ORM, JPA Setup, Architecture, Entity Lifecycle  ← YOU ARE HERE
Part 3 → First-level Caching (coming next)
```

---

These notes cover everything the instructor taught in this lecture — from the problem ORM solves, to the happy flow setup, to the full internal architecture, to the entity lifecycle — all the way to interview prep. You're fully covered! 🎉