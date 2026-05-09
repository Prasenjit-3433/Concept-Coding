# Spring Boot JPA — Part 1 Notes
---

## Step 1 — The Big Picture: How Everything Connects

Before jumping into JPA, the instructor wants you to understand the **full sequence** of components involved when a Spring Boot app talks to a database. Think of it like a chain.

```
Your App (Business Logic)
        │
        ▼
   ┌─────────┐
   │   JPA   │  ◄──── Interface (you talk to this)
   └─────────┘
        │
        ▼
   ┌───────────┐
   │ Hibernate │  ◄──── Implementation of JPA
   └───────────┘        (most popular one)
        │
        ▼
   ┌────────┐
   │  JDBC  │  ◄──── Interface again
   └────────┘
        │
        ▼
   ┌──────────────────┐
   │ Specific DB      │  ◄──── Implementation of JDBC
   │ Driver           │        (e.g. MySQL Connector/J,
   └──────────────────┘         PostgreSQL Driver, H2 Driver)
        │
        ▼
   ┌──────────────────┐
   │  Relational DB   │  (MySQL / PostgreSQL / H2 etc.)
   └──────────────────┘
```

### What is JPA?

JPA (Java Persistence API) is **just an interface** — it gives you a set of APIs to work with, but it does **not** provide the actual implementation. The implementation is provided by frameworks like:
- **Hibernate** (most popular, default in Spring Boot)
- OpenJPA
- EclipseLink

So your application talks to JPA → JPA's implementation (Hibernate) does the actual work → Hibernate uses JDBC internally.

### What is JDBC?

JDBC (Java Database Connectivity) is also **just an interface**. It lets you connect to a DB, run queries, and fetch results. The actual work is done by **specific DB drivers**.

For example:
- **MySQL** → Driver: Connector/J → Class: `com.mysql.cj.jdbc.Driver`
- **PostgreSQL** → Driver: PostgreSQL JDBC Driver → Class: `org.postgresql.Driver`
- **H2 (in-memory)** → Driver: H2 Database Engine → Class: `org.h2.Driver`

### What is ORM?

ORM stands for **Object Relational Mapping**. It's a bridge between your **Java objects** and your **relational database tables**.

Without ORM (plain JDBC): You write raw SQL queries, get results back, and manually map them to Java objects.

With ORM (JPA + Hibernate): You work directly with Java objects. You don't write SQL — Hibernate figures out the SQL for you and handles the mapping automatically.

> 💡 **Key takeaway from the instructor:** JPA and JDBC are both **just for relational databases**. JPA sits on top of JDBC and makes your life easier by letting you work with objects instead of raw SQL.

---

## Step 2 — JDBC Recap & Plain JDBC (Without Spring Boot)

### What does JDBC actually do?

JDBC gives you an interface to do 3 things:
1. **Make a connection** with the database
2. **Query** the database (insert, update, delete, select)
3. **Process the result** — whatever data comes back from the DB

The actual implementation of all this is done by the **specific DB driver** you choose.

---

### Plain JDBC — How it works (No Spring Boot)

The instructor walks through a simple example using **H2** — an in-memory database. He picks H2 specifically so you don't have to install MySQL or start any external server. But here's the important thing he says:

> *"It doesn't matter if you use MySQL, PostgreSQL, or H2 — your code will NOT change. You're always calling JDBC APIs. Only the driver changes internally."*

The example has two classes:

---

### Class 1 — `DatabaseConnection`

This class has one job: **give you a connection to the database.**

```
Step 1 → Load the driver into JVM     (Class.forName)
Step 2 → Establish connection with DB  (DriverManager.getConnection)
```

```java
public class DatabaseConnection {

    public Connection getConnection() {
        try {
            // Step 1: Load the H2 driver into JVM
            Class.forName("org.h2.Driver");

            // Step 2: Establish connection with DB
            // If "userDB" doesn't exist, it will be created
            return DriverManager.getConnection(
                "jdbc:h2:mem:userDB",  // URL → in-memory H2 DB named "userDB"
                "sa",                  // default username for H2
                ""                     // no password
            );
        } catch (ClassNotFoundException | SQLException e) {
            // handle exception
        }
        return null;
    }
}
```

> 💡 **About H2:** The instructor explains H2 has two flavors:
> - **In-memory** (`mem:`) → Data lives only while the app is running. Once app shuts down, data is gone. This is what he uses here.
> - **Persistent** → Data survives even after the app shuts down.

---

### Class 2 — `UserDAO`

This class has 3 methods — **create table**, **insert a user**, **read users**.

Notice the pattern in every single method:
```
1. Get a DB connection
2. Write your SQL query
3. Execute it
4. Catch SQLException
5. Close everything in finally block
```

```java
public class UserDAO {

    // Method 1: Create the users table
    public void createUserTable() {
        try {
            Connection connection = new DatabaseConnection().getConnection();
            Statement statementQuery = connection.createStatement();

            String sql = "CREATE TABLE users(" +
                         "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "user_name VARCHAR(100), " +
                         "age INT)";

            statementQuery.executeUpdate(sql);
        }
        catch (SQLException e) { /** handle exception **/ }
        finally { /** close statementQuery and db connection **/ }
    }

    // Method 2: Insert a user
    public void createUser(String userName, int userAge) {
        try {
            Connection connection = new DatabaseConnection().getConnection();

            String sqlQuery = "INSERT INTO users(user_name, age) VALUES (?, ?)";

            // PreparedStatement lets you fill in dynamic values safely
            PreparedStatement preparedQuery = connection.prepareStatement(sqlQuery);
            preparedQuery.setString(1, userName);  // index 1 → first ?
            preparedQuery.setInt(2, userAge);       // index 2 → second ?
            preparedQuery.executeUpdate();
        }
        catch (SQLException e) { /** handle exception **/ }
        finally { /** close preparedQuery and db connection **/ }
    }

    // Method 3: Read all users
    public void readUsers() {
        try {
            Connection connection = new DatabaseConnection().getConnection();

            String sqlQuery = "SELECT * FROM users";
            PreparedStatement preparedQuery = connection.prepareStatement(sqlQuery);

            // ResultSet = list of rows returned
            ResultSet output = preparedQuery.executeQuery();

            while (output.next()) {
                String userDetails =
                    output.getInt("user_id") + ":" +
                    output.getString("user_name") + ":" +
                    output.getInt("age");

                System.out.println(userDetails);
            }
        }
        catch (SQLException e) { /** handle exception **/ }
        finally { /** close preparedQuery and db connection **/ }
    }
}
```

---

### The Problems with Plain JDBC

This is where the instructor gets to the real point — **why do we even need Spring Boot's help here?**

He lists 5 pain points:

```
┌─────────────────────────────────────────────────────────────────┐
│                  PROBLEMS WITH PLAIN JDBC                       │
├─────┬───────────────────────────────────────────────────────────┤
│  1  │ Driver class loading                                      │
│     │ You have to manually do Class.forName(...) every time     │
├─────┼───────────────────────────────────────────────────────────┤
│  2  │ DB Connection Making                                      │
│     │ You manually call DriverManager.getConnection(url, u, p)  │
│     │ every single time you need to talk to the DB              │
├─────┼───────────────────────────────────────────────────────────┤
│  3  │ Exception Handling                                        │
│     │ You only get SQLException — a very high-level, vague      │
│     │ error. Could be duplicate key, timeout, anything.         │
│     │ You have no idea what exactly went wrong.                 │
├─────┼───────────────────────────────────────────────────────────┤
│  4  │ Closing DB Connection & other resources                   │
│     │ Every method needs a finally block to close               │
│     │ PreparedStatement + Connection manually.                  │
│     │ Forget this → memory leak.                                │
├─────┼───────────────────────────────────────────────────────────┤
│  5  │ Manual DB Connection Pool handling                        │
│     │ Creating a fresh connection every time is wasteful.       │
│     │ Ideally you maintain a pool of pre-created connections    │
│     │ and reuse them. In plain JDBC, you handle this yourself.  │
└─────┴───────────────────────────────────────────────────────────┘
```

> 💡 **Connection Pool explained simply:** Instead of opening and closing a DB connection every single request (which is expensive), you keep a **pool** of say 10 connections ready. A request picks one up, uses it, and returns it back to the pool. Much more efficient.

---

These 5 problems are exactly what Spring Boot solves. And that's what we cover next.

## Step 3 — Spring Boot + JdbcTemplate (Solving all 5 Problems)

### First — Add the Dependencies (`pom.xml`)

Before anything, you need two dependencies:

```xml
<!-- 1. JDBC support from Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- 2. H2 in-memory database -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

> You can replace H2 with MySQL or PostgreSQL — your code won't change at all. Only the dependency and the `application.properties` values change.

---

### The Structure — 3 Classes + 1 Properties File

```
┌──────────────────────────────────────────────────┐
│                  Your App                        │
│                                                  │
│   UserController                                 │
│        │                                         │
│        ▼                                         │
│   UserService  (@Component / @Service)           │
│   → Business logic lives here                    │
│        │                                         │
│        ▼                                         │
│   UserRepository  (@Repository)                  │
│   → Talks to DB using JdbcTemplate               │
│                                                  │
│   application.properties                         │
│   → DB connection config lives here              │
└──────────────────────────────────────────────────┘
```

---

### `application.properties` — All DB config in one place

```properties
spring.datasource.url=jdbc:h2:mem:userDB
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
```

Compare this to plain JDBC where you were hardcoding the URL, username, password inside your Java class itself. Now it's all cleanly separated into one config file.

---

### Class 1 — `User` (Simple POJO)

```java
public class User {
    int userId;
    String userName;
    int age;

    // getters and setters
}
```

Nothing fancy — just a plain Java object representing a user row in the DB.

---

### Class 2 — `UserRepository` (@Repository)

This is where `JdbcTemplate` does all the heavy lifting. Notice what's **completely gone** compared to plain JDBC:
- No `Class.forName()`
- No `DriverManager.getConnection()`
- No `finally` block to close connections
- No `SQLException` handling

```java
@Repository
public class UserRepository {

    @Autowired
    JdbcTemplate jdbcTemplate;  // Spring Boot injects this for you

    // Create table
    public void createTable() {
        jdbcTemplate.execute(
            "CREATE TABLE users (user_id INT AUTO_INCREMENT PRIMARY KEY, " +
            "user_name VARCHAR(100), age INT)"
        );
    }

    // Insert a user
    public void insertUser(String name, int age) {
        String insertQuery = "INSERT INTO users (user_name, age) VALUES (?, ?)";
        jdbcTemplate.update(insertQuery, name, age);
    }

    // Read all users
    public List<User> getUsers() {
        String selectQuery = "SELECT * FROM users";
        return jdbcTemplate.query(selectQuery, (rs, rowNum) -> {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setUserName(rs.getString("user_name"));
            user.setAge(rs.getInt("age"));
            return user;
        });
    }
}
```

> 💡 **What is `@Repository`?** It does two things:
> 1. Works like `@Component` — Spring creates a bean of this class automatically
> 2. Whenever Spring sees `@Repository`, any `SQLException` that occurs gets automatically translated into **granular, specific exceptions** (more on this below)

---

### Class 3 — `UserService` (@Component)

This is your business logic layer. It doesn't talk to the DB directly — it calls the repository.

```java
@Component  // or @Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public void createTable() {
        userRepository.createTable();
    }

    public void insertUser(String userName, int age) {
        userRepository.insertUser(userName, age);
    }

    public List<User> getUsers() {
        List<User> users = userRepository.getUsers();
        for (User user : users) {
            System.out.println(user.userId + ":" + user.getUserName() + ":" + user.getAge());
        }
        return users;
    }
}
```

> 💡 The instructor's point here: After fetching data from the DB, any processing or business logic you want to do on that data — **do it here in the service layer**, not in the repository.

---

### Now — How Spring Boot solves all 5 problems

```
┌─────────────────────────────────────────────────────────────────────┐
│            HOW SPRING BOOT FIXES PLAIN JDBC PROBLEMS                │
├─────┬───────────────────────────────────────────────────────────────┤
│  1  │ Driver class loading                                          │
│     │ JdbcTemplate does Class.forName() automatically at app        │
│     │ startup itself via DriverManager. You don't touch it.         │
├─────┼───────────────────────────────────────────────────────────────┤
│  2  │ DB Connection Making                                          │
│     │ JdbcTemplate handles it internally whenever you call          │
│     │ execute(), update(), or query(). It either creates a new      │
│     │ connection or picks one from the pool automatically.          │
├─────┼───────────────────────────────────────────────────────────────┤
│  3  │ Exception Handling                                            │
│     │ Instead of the vague SQLException, Spring gives you           │
│     │ granular exceptions from org.springframework.dao package:     │
│     │  → DuplicateKeyException                                      │
│     │  → QueryTimeoutException                                      │
│     │  → DataIntegrityViolationException                            │
│     │  → CannotAcquireLockException  ... and more                   │
├─────┼───────────────────────────────────────────────────────────────┤
│  4  │ Closing DB connection & resources                             │
│     │ JdbcTemplate handles this in its own finally block.           │
│     │ After every update() or query(), it knows whether to          │
│     │ close the connection or return it back to the pool.           │
│     │ You write zero finally blocks.                                │
├─────┼───────────────────────────────────────────────────────────────┤
│  5  │ DB Connection Pool                                            │
│     │ Spring Boot provides HikariCP by default — completely         │
│     │ automatic. No setup needed. Default pool size is 10.          │
│     │ You can configure it in application.properties if needed.     │
└─────┴─────────────────────────────────────────────────────────────--┘
```

---

### HikariCP — The Default Connection Pool

Spring Boot uses **HikariCP** as its default connection pool. The instructor mentions it's extremely fast and efficient.

Default config (you can override in `application.properties`):

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

If you want to use a **different connection pool** (not Hikari), you can define a `DataSource` bean manually in a config class:

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl("jdbc:h2:mem:userDB");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
        // Replace HikariDataSource with any other pool you want
    }
}
```

> 💡 If you define a `DataSource` bean manually like this, you don't need to put datasource config in `application.properties` anymore — Spring Boot will use your custom bean instead.

---

## Step 4 — JdbcTemplate Frequently Used Methods

The instructor walks through all the methods you'll commonly use with `JdbcTemplate`. There are essentially **two categories** — methods for **writing data** (insert/update/delete) and methods for **reading data** (select).

---

### Category 1 — Writing Data (Insert / Update / Delete)

There are **two flavors** of the `update()` method:

---

#### Flavor 1 — `update(String sql, Object... args)`

The simpler, more commonly used one. You pass your SQL query and the dynamic values directly as arguments.

```
┌─────────────────────────────────────────────────────────────┐
│  update(String sql, Object... args)                         │
│  Use for → INSERT, UPDATE, DELETE                           │
└─────────────────────────────────────────────────────────────┘
```

```java
// INSERT
String insertQuery = "INSERT INTO users (user_name, age) VALUES (?, ?)";
int rowsAffected = jdbcTemplate.update(insertQuery, "X", 27);
//                                                    ↑    ↑
//                                               index 1  index 2

// UPDATE
String updateQuery = "UPDATE users SET age = ? WHERE user_id = ?";
int rowsAffected = jdbcTemplate.update(updateQuery, 29, 1);

// DELETE
String deleteQuery = "DELETE FROM users WHERE user_id = ?";
int rowsAffected = jdbcTemplate.update(deleteQuery, 1);
```

> 💡 The `?` marks are placeholders. The values you pass after the SQL string fill them in **sequentially** — first value goes to first `?`, second value to second `?`, and so on.

---

#### Flavor 2 — `update(String sql, PreparedStatementSetter pss)`

More granular control. Useful when your queries are **more complex** and you need finer control over how values are set.

```
┌─────────────────────────────────────────────────────────────┐
│  update(String sql, PreparedStatementSetter pss)            │
│  Use for → INSERT, UPDATE, DELETE (complex cases)           │
└─────────────────────────────────────────────────────────────┘
```

```java
// INSERT
String insertQuery = "INSERT INTO users (user_name, age) VALUES (?, ?)";
jdbcTemplate.update(insertQuery, (PreparedStatement ps) -> {
    ps.setString(1, "X");   // index 1 → first ?
    ps.setInt(2, 25);       // index 2 → second ?
});

// UPDATE
String updateQuery = "UPDATE users SET age = ? WHERE user_id = ?";
jdbcTemplate.update(updateQuery, (PreparedStatement ps) -> {
    ps.setInt(1, 29);  // new age value
    ps.setInt(2, 1);   // user_id to match
});

// DELETE
String deleteQuery = "DELETE FROM users WHERE user_id = ?";
jdbcTemplate.update(deleteQuery, (PreparedStatement ps) -> {
    ps.setInt(1, 2);   // user_id to delete
});
```

> 💡 `PreparedStatementSetter` is a **functional interface** — it has only one method to implement, so you can use a **lambda expression** directly, which is exactly what the instructor does here.

---

### Category 2 — Reading Data (SELECT)

There are **4 flavors** for reading, each for a different use case:

```
┌──────────────────────────────────────────────────────────────────┐
│                    READING DATA — 4 FLAVORS                      │
├────┬─────────────────────────────────────────────────────────────┤
│ 1  │ query()          → fetch multiple rows (full objects)       │
│ 2  │ queryForList()   → fetch single column of multiple rows     │
│ 3  │ queryForObject() → fetch a single row (full object)         │
│ 4  │ queryForObject() → fetch a single value                     │
└────┴─────────────────────────────────────────────────────────────┘
```

---

#### Flavor 1 — `query(String sql, RowMapper<T> rowMapper)`

Use this when you want to **fetch multiple rows** and map each row to a Java object.

```java
List<User> users = jdbcTemplate.query("SELECT * FROM users", (rs, rowNum) -> {
    User user = new User();
    user.setUserId(rs.getInt("user_id"));       // map column → field
    user.setUserName(rs.getString("user_name"));
    user.setAge(rs.getInt("age"));
    return user;
});
```

> 💡 **How does RowMapper work internally?**
> JdbcTemplate runs your query, gets back a ResultSet (all the rows). Then for **each row**, it calls your `mapRow` lambda. You tell it how to map that row into a `User` object. JdbcTemplate keeps adding each mapped object to a list and returns the full list at the end.

```
ResultSet (10 rows)
     │
     ├── Row 1 → mapRow() called → User object → added to list
     ├── Row 2 → mapRow() called → User object → added to list
     ├── Row 3 → mapRow() called → User object → added to list
     │   ...
     └── Row 10 → mapRow() called → User object → added to list
                                                        │
                                               Returns List<User>
```

---

#### Flavor 2 — `queryForList(String sql, Class<T> elementType)`

Use this when you want **only one specific column** from multiple rows.

```java
// Get just the usernames of all users
List<String> userNames = jdbcTemplate.queryForList(
    "SELECT user_name FROM users",
    String.class   // the type of that one column
);
```

> 💡 If there are 10 users in the DB, you get back a `List<String>` with 10 usernames. You're not fetching full objects here — just one column's worth of data.

---

#### Flavor 3 — `queryForObject(String sql, Object[] args, Class<T> requiredType)`

Use this when you want to **fetch a single row** and map it to a Java object.

```java
// Fetch one specific user by ID
User user = jdbcTemplate.queryForObject(
    "SELECT * FROM users WHERE user_id = ?",
    new Object[]{1},   // the value that replaces ?
    User.class         // map this row into a User object
);
```

> 💡 The `Object[]` array holds your dynamic values. If you have multiple `?` in your query, just add more values to the array — they fill in sequentially. Spring Boot internally uses getters and setters of your `User` class to map column values to fields.

---

#### Flavor 4 — `queryForObject(String sql, Class<T> requiredType)`

Use this when you just want **a single value** back — not a full row, not a list.

```java
// Get total count of users
int userCount = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM users",
    Integer.class   // the type of the single value coming back
);
```

---

### Full Summary — All Methods at a Glance

```
┌──────────────────────────────────┬───────────────────────────────┬──────────────────────┐
│ Method                           │ Use For                       │ Returns              │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ update(sql, args...)             │ Insert / Update / Delete      │ int (rows affected)  │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ update(sql, PrepStmtSetter)      │ Insert / Update / Delete      │ int (rows affected)  │
│                                  │ (complex queries)             │                      │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ query(sql, RowMapper)            │ Fetch multiple rows           │ List<T>              │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ queryForList(sql, Class)         │ Fetch 1 column, many rows     │ List<T>              │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ queryForObject(sql, args, Class) │ Fetch 1 row                   │ T (single object)    │
├──────────────────────────────────┼───────────────────────────────┼──────────────────────┤
│ queryForObject(sql, Class)       │ Fetch 1 single value          │ T (int, String etc.) │
└──────────────────────────────────┴───────────────────────────────┴──────────────────────┘
```

---

### What's Coming Next (Part 2)

The instructor wraps up Part 1 here. He says now that you understand JDBC and how Spring Boot simplifies it, the next step is to understand **ORM (Object Relational Mapping)** and then move into **JPA with Hibernate** — which takes things even further by removing the need to write SQL altogether.

> 💡 **Why did the instructor spend so much time on JDBC first?** Because if you jump straight into JPA without understanding JDBC, you won't appreciate what JPA is actually solving. The whole point of Part 1 is to build that foundation.

---

That wraps up the complete notes for **Spring Boot JPA — Part 1**! Let me know if you want me to revise any section, add more detail anywhere, or start on Part 2 whenever you're ready.