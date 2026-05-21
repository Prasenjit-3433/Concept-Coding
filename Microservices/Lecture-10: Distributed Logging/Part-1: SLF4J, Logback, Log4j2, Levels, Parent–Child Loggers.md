---

# 📘 Distributed Logging — Part 1
## Step 1: What is Logging? Why does it matter? + The Overall Architecture

---

## What is Logging?

Logging means **recording important application events** — like requests, responses, errors, warnings, or any useful information — so that developers can **monitor and debug** what the application is actually doing at **runtime**.

Think of it this way: you own a `UserService`, and it has an endpoint `/post` to create a user. As part of logging, you write an event like:

```
"User created successfully — userId: 1234"
```

Now, in the future, if a user complains about a problem, you go to the logs and check. If you see that log printed, you know: **"okay, till this point my code ran fine."** That narrows down exactly where the bug is.

> **Key insight from instructor:** Logging is not just for errors. Even simple informational events — things that help you understand what your code did at runtime — are worth logging.

---

## The Big Picture: How Logging Works in a Single Spring Boot Application

Before jumping into distributed logging (multiple microservices), the instructor makes it very clear:

> *"First understand how logging works in a single application. Once that's done, achieving this in distributed logging is very, very easy."*

Here is the **complete architecture flow**:

```
+------------------+        +-------------+       +-----------------------+
|  Spring Boot     |  API   |             | Impl  |   Logback (Default)   |
|  Application     +------->+   SLF4J     +------>+         OR            |
|                  |        | (Interface) |       |   Log4j2              |
+------------------+        +-------------+       +----------+------------+
                                                             |
                                                             | uses
                                                             v
                                                    +--------+--------+
                                                    |    Appender     |
                                                    |  (decides WHERE |
                                                    |   log will go)  |
                                                    +--------+--------+
                                                             |
                                          +------------------+------------------+
                                          |                                     |
                                          v                                     v
                                  +-------+-------+                   +--------+------+
                                  |    Console    |                   |  {file}.log   |
                                  | (default out) |                   |  (log file)   |
                                  +-------+-------+                   +--------+------+
                                          |                                     |
                                          +------------------+------------------+
                                                             |
                                                          READS
                                                             |
                                                             v
                                          +------------------+------------------+
                                          |        Log Aggregation Tools        |
                                          |   - DataDog                         |
                                          |   - ELK (ElasticSearch + Kibana)    |
                                          |   - Promtail                        |
                                          |   - Etc.                            |
                                          +-------------------------------------+
```

---

## Breaking Down Each Layer

### 1. Your Spring Boot Application
This is where you write log statements in your code. Nothing special here — just your normal controllers, services, etc.

---

### 2. SLF4J — Simple Logging Facade for Java
- SLF4J is **only an interface (API)**. It provides **no implementation** whatsoever.
- The full form itself gives it away — **"Facade"** — just like the Facade design pattern in Low Level Design (LLD), which only exposes APIs and hides implementation details.
- It gives you methods like `log.info(...)`, `log.error(...)`, `log.debug(...)` etc. But who actually executes them? The implementation library.

---

### 3. Implementation Libraries — Logback & Log4j2

| Library | Full Form | Who provides it? |
|---|---|---|
| **Logback** | — | Spring Boot's **default**. Comes automatically. |
| **Log4j2** | Logging for Java Version 2 | You have to add it **manually**. |

Both are **implementations of SLF4J**. You use one or the other — not both.

> **Key point from instructor:** When you create a Spring Boot app from [start.spring.io](https://start.spring.io), Logback is already included. You don't need to add anything.

---

### 4. Appender
- Appender is a **component** whose job is to decide the **destination** of the log.
- Where should this log go? Console? A file? A database? Kafka? Cloud?
- The implementation library (Logback or Log4j2) uses the appender to produce the **output**.

---

### 5. Output
Depending on the appender configured:
- **Console Appender** → logs printed to standard output (your terminal)
- **File Appender** → logs written to a `.log` file

---

### 6. Log Aggregation Tools
These tools **read from the output** (console or file) and give you a centralized place to search, filter, and monitor logs across your entire system.

Examples: DataDog, ELK Stack (ElasticSearch + Kibana), Promtail.

> Note: Your application controls **where the output goes**. The log aggregation tool's job is to **read from that place** and make it searchable.

---

## Why Learn Single-App Logging First?

Because in a distributed system, you have **multiple microservices**, each doing their own logging. But the foundation — SLF4J, Logback, Appenders, Log Levels — is **identical** in every service. Once this is solid, distributed logging is just an extension on top.

---

Ready for **Step 2** — What does it take to implement logging in code, and what happens behind the scenes (dependency internals + how `LoggerFactory.getLogger()` works internally with UML)?

---

## Step 2: What Does It Take to Implement Logging? + What Happens Behind the Scenes

---

## The Minimal Code to Start Logging

Here is all it takes to add logging to a Spring Boot controller:

```java
@RestController
public class PaymentController {

    // Step 1: Get a Logger object
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {

        // Step 2: Start logging events
        log.info("fetch the payments successfully");

        return "successfully fetched all payments";
    }
}
```

When you hit `/payments`, two things happen:
- The endpoint returns: `"successfully fetched all payments"`
- The console prints: `fetch the payments successfully`

**That's it. No extra dependency. No extra configuration.** Just get a logger object and start logging.

> The instructor's point here is: don't overthink the setup. The libraries are already there. Just understand *why* they're already there — that's what we cover next.

---

## What Happens Behind the Scenes?

### Part A — The Dependency Chain

When you add this to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

Spring Boot **automatically pulls in** these jars for you:

```
spring-boot-starter
        |
        v
spring-boot-starter-logging
        |
        +---> slf4j-api.jar          (the SLF4J interface/API)
        |
        +---> logback-classic.jar    (implementation of SLF4J)
        |
        +---> logback-core.jar       (core engine of Logback)
```

So you get:
- The **API** (SLF4J) to write log statements against
- The **Implementation** (Logback) to actually execute them
- The **default Console Appender** already wired in

You don't configure any of this. It's all there out of the box.

---

### Part B — What If You Want Log4j2 Instead of Logback?

You'd add this dependency manually:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

But now your classpath has **two SLF4J implementations** — Logback (added automatically) and Log4j2 (added manually).

**Which one will Spring Boot use?**

Looking at the actual `LoggerFactory` framework code:

```java
/******** LoggerFactory (Framework code) ********/

private final static void bind() {
    try {
        List<SLF4JServiceProvider> providersList = findServiceProviders();
        reportMultipleBindingAmbiguity(providersList);

        if (providersList != null && !providersList.isEmpty()) {

            // From the list of providers, it simply picks index 0
            PROVIDER = providersList.get(0);
            ...
        }
    }
}
```

It just does **`get(0)`** — picks the first one from the list. So you might get Logback, you might get Log4j2. **It's unpredictable.**

Spring Boot also prints a **warning** (not an error) when it detects this:

```
SLF4J: Class path contains multiple SLF4J providers.
SLF4J: Found provider [ch.qos.logback.classic...]
SLF4J: Found provider [org.apache.logging.slf4j...]
```

The app still starts, but which logger is actually running is **unreliable in production**.

---

### ✅ The Right Way: Exclude the Default, Add What You Want

If you want Log4j2, you must **explicitly exclude** the default Logback library first:

```xml
<!-- Step 1: Exclude the default logging (Logback) from spring-boot-starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <!-- This sub-library brings logback-classic + logback-core -->
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Step 2: Manually add the implementation library you want -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

Now only **one** SLF4J implementation is on the classpath. No ambiguity. Predictable behavior.

```
Classpath (WRONG way — two providers):           Classpath (RIGHT way — one provider):
+-------------------------+                      +-------------------------+
|  logback-classic.jar    |  <-- auto-added      |  log4j2.jar             | <-- manually added
|  logback-core.jar       |                      +-------------------------+
|  log4j2.jar             |  <-- manually added  (logback excluded cleanly)
+-------------------------+
  get(0) = unpredictable!                          get(0) = always log4j2 ✅
```

---

### Part C — How `LoggerFactory.getLogger()` Works Internally (UML Walkthrough)

This is the most important internal detail the instructor covers. Here is the full UML:

```
org.slf4j
+------------------+          <<interface>>
|  LoggerFactory   |--------->|   Logger   |<---------+
|                  |  has-a   |            |          |
+--------+---------+          +------------+          |
         |                                        is-a|        is-a|
         | has-a                            +----------+    +-------+--------+
         v                                  |  Logger  |    |    Logger      |
   <<interface>>                            | (fields) |    |   (fields)     |
  +---------------+                         |          |    |                |
  | ILoggerFactory|                         |+name     |    |+name           |
  |+getLogger(    |                         |+level    |    |+loggerConfigLvl|
  |  String name) |                         |+parent   |    |+loggerConfig   |
  +-------+-------+                         |+child[]  |    |  Parent        |
          |                                 +----------+    +----------------+
      is-a|            is-a|               logback.classic      Log4j.core
          |                |
  +-------+--------+   +---+------------------+
  |  LoggerContext |   | AbstractLoggerAdapter|
  |                |   |                      |
  |Map<String,     |   |Map<LoggerContext,    |
  | Logger>        |   | ConcurrentMap<String,|
  | loggerCache;   |   |  L>> registry        |
  |                |   |                      |
  |+getLogger(     |   |+getLogger(           |
  |  String name)  |   |  String name)        |
  +----------------+   +----------------------+
   logback.classic           Log4j
```

---

### What Exactly Happens When You Call `LoggerFactory.getLogger(PaymentController.class)`?

**Step-by-step flow:**

```
YOUR CODE
    |
    | LoggerFactory.getLogger(PaymentController.class)
    v
+------------------+
|  LoggerFactory   |   Step 1: Looks at the classpath
|  (SLF4J)         |           to find which implementation
|                  |           library is present
|                  |   Step 2: Invokes the LoggerContext
|                  |           of that specific implementation
+--------+---------+
         |
         v
+------------------+          (if Logback is the impl)
|  LoggerContext   |   Step 3: Checks its internal cache
|  (Logback)       |
|  Map<String,     |   Is logger for name
|  Logger>         |   "com.concepts.PaymentController"
|  loggerCache     |   already in the map?
+--------+---------+
         |
    +----+----+
    |         |
   YES        NO
    |         |
    |         v
    |   Create new Logger object
    |   Put it in the map
    |         |
    +---------+
         |
         v
  Return Logger object
  (holds reference to either
   Logback Logger or Log4j2 Logger)
```

**Key takeaway:** For one logger name → only **one logger object** is ever created and reused. This is **caching by logger name**.

> In real-world applications, the standard practice is to create **one logger per class**, using the class name as the logger name. That's why you always see `LoggerFactory.getLogger(PaymentController.class)` — it uses the full class path (e.g., `com.concepts.PaymentController`) as the name.

---

## 🎯 Interview Tips

> **Q: What is SLF4J and why do we use it?**
> SLF4J is just an interface — a logging API with no implementation. It lets you write log statements in your code without tying yourself to a specific logging library. You can swap Logback for Log4j2 without changing a single line of application code.

> **Q: What is the default logging implementation in Spring Boot?**
> Logback. It comes automatically via `spring-boot-starter-logging` which is pulled in by `spring-boot-starter`.

> **Q: What happens if both Logback and Log4j2 are on the classpath?**
> `LoggerFactory` internally does a `get(0)` on the list of available providers — so which one is used is unpredictable. The right fix is to **exclude `spring-boot-starter-logging`** before adding Log4j2 manually.

> **Q: Is a new Logger object created every time you call `getLogger()`?**
> No. The `LoggerContext` maintains a `Map<String, Logger>` cache. For a given logger name, the same object is reused every time.

---

Ready for **Step 3** — The Logger Object in depth: `name`, `level`, `parent/child` hierarchy, level filtering rules, and log level inheritance with full ASCII tree diagram?

---

## Step 3: The Logger Object In Depth — Name, Level, Parent/Child Hierarchy

---

## The Logger Object — 4 Key Fields

Every Logger object that gets created and cached has these four important fields:

```
+---------------------------+
|       Logger Object       |
+---------------------------+
|  String name              |  <-- identity of this logger
|  Level level              |  <-- what log statements it accepts
|  Logger parent            |  <-- one parent (always)
|  List<Logger> child       |  <-- zero or more children
+---------------------------+
```

Let's go through each one carefully.

---

## Field 1 — `name`

The name is the **identity** of a logger. It can be:
- A class name (most common in production)
- A package name
- Any arbitrary string

```java
// Logger name = "com.concepts.PaymentController"
Logger log = LoggerFactory.getLogger(PaymentController.class);
```

When you pass `PaymentController.class`, the framework uses the **fully qualified class name** as the logger name — `com.concepts.PaymentController`.

> **Rule:** For one logger name → only one logger object is ever created and reused (caching, as we saw in Step 2).

> **Real-world practice:** Always create **one logger per class** using the class itself as the name. This is what you'll see in every production codebase.

---

## Field 2 — `level`

This is where things get really interesting.

### The 5 Log Levels (Highest → Lowest Priority)

```
+-------+------------------+
| Level | Priority         |
+-------+------------------+
| ERROR | Highest          |
| WARN  |                  |
| INFO  | <-- Default      |
| DEBUG |                  |
| TRACE | Lowest           |
+-------+------------------+
```

### Two Things Have a Level:
1. The **Logger object itself** — you configure this
2. Each **log statement** — this is implicit based on the method you call

```java
log.error("...");   // this statement has level = ERROR
log.warn("...");    // this statement has level = WARN
log.info("...");    // this statement has level = INFO
log.debug("...");   // this statement has level = DEBUG
log.trace("...");   // this statement has level = TRACE
```

### The Golden Rule of Log Levels:

> **A logger prints a log statement ONLY IF the log statement's level is SAME OR HIGHER than the logger's configured level.**

### Example — Logger Level = WARN:

```
Logger Object (Level = WARN)
         |
         +---> log.error("error log")   ERROR > WARN  --> ✅ ACCEPTED  (printed)
         |
         +---> log.warn("warning log")  WARN = WARN   --> ✅ ACCEPTED  (printed)
         |
         +---> log.info("info log")     INFO < WARN   --> ❌ REJECTED  (skipped)
         |
         +---> log.debug("debug log")   DEBUG < WARN  --> ❌ REJECTED  (skipped)
         |
         +---> log.trace("trace log")   TRACE < WARN  --> ❌ REJECTED  (skipped)
```

### Example — Logger Level = INFO (Default):

```java
@RestController
public class PaymentController {

    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {

        log.error("error log");    // ✅ printed
        log.warn("warning log");   // ✅ printed
        log.info("info log");      // ✅ printed
        log.debug("debug log");    // ❌ skipped
        log.trace("trace log");    // ❌ skipped

        return "successfully fetched all payments";
    }
}
```

Console output:
```
ERROR ... com.concepts.PaymentController : error log
WARN  ... com.concepts.PaymentController : warning log
INFO  ... com.concepts.PaymentController : info log
```

Debug and Trace are silently skipped — because their level is lower than INFO.

---

### Changing the Logger Level via Configuration

You don't hardcode the level in Java. You set it in `application.properties`:

```properties
# Format: logging.level.<logger-name>=<LEVEL>
logging.level.com.concepts.PaymentController=DEBUG
```

Now with level = DEBUG, the output becomes:

```
ERROR ... com.concepts.PaymentController : error log
WARN  ... com.concepts.PaymentController : warning log
INFO  ... com.concepts.PaymentController : info log
DEBUG ... com.concepts.PaymentController : debug log
```

Only TRACE is skipped now — because TRACE < DEBUG.

---

## Fields 3 & 4 — `parent` and `List<Logger> child` (The Hierarchy)

This is the **most important and most underrated** part of the logging framework.

### What the Framework Actually Does When You Call `getLogger()`

You think calling `LoggerFactory.getLogger(PaymentController.class)` creates just **one** logger object. It actually creates **multiple** — one for every segment of the package path — and arranges them in a **tree**.

```java
// You write this one line:
Logger log = LoggerFactory.getLogger(PaymentController.class);
// Logger name = "com.concepts.PaymentController"
```

The framework internally creates this entire hierarchy:

```
                    +----------------------+
                    |   Logger             |
                    |   name = ROOT        |  <-- Always created by framework.
                    |                      |      You cannot delete it.
                    |                      |      Parent of ALL loggers.
                    +----------+-----------+
                               |
               +---------------+---------------+
               |                               |
    +----------+-----------+       +-----------+----------+
    |   Logger             |       |   Logger             |
    |   name = com         |       |   name = org         |
    |   parent = ROOT      |       |   parent = ROOT      |
    +----------+-----------+       +----------------------+
               |
    +----------+-----------+
    |   Logger             |
    |   name = com.concepts|
    |   parent = com       |
    +----------+-----------+
               |
    +----------+------------------+
    |   Logger                    |
    |   name =                    |
    |   com.concepts.             |
    |   PaymentController         |
    |   parent = com.concepts     |
    +-----------------------------+
```

The instructor verified this by putting a **debugger point** inside Logback's `Logger` class. Here's what he saw at runtime:

```
this = Logger[com.concepts.PaymentController]
  name    = "com.concepts.PaymentController"
  level   = DEBUG
  parent  = Logger[com.concepts]
              name   = "com.concepts"
              level  = null
              parent = Logger[com]
                         name   = "com"
                         level  = null
                         parent = Logger[ROOT]
                                    name   = "ROOT"
                                    level  = INFO
                                    parent = null
```

> **Key insight:** Even though you wrote only one `getLogger()` call in your code, the framework silently created the entire chain — `ROOT → com → com.concepts → com.concepts.PaymentController` — in the background.

---

## Why Maintain This Hierarchy? — 3 Powerful Advantages

---

### Advantage 1 — Log Level Inheritance

You don't need to configure the log level for every single class. Set it once on a **parent (package-level) logger** and all child loggers automatically inherit it.

**The problem without inheritance:**
```properties
# Imagine doing this for thousands of classes — nightmare!
logging.level.com.concepts.PaymentController=DEBUG
logging.level.com.concepts.UserController=DEBUG
logging.level.com.concepts.OrderController=DEBUG
logging.level.com.concepts.ProductController=DEBUG
# ... and so on
```

**The solution with inheritance:**
```properties
# Set it once on the parent — all children inherit DEBUG
logging.level.com=DEBUG
```

```
Logger (name=ROOT, level=INFO)
    |
Logger (name=com, level=DEBUG)  <-- set here once
    |
    +---> Logger (name=com.concepts.PaymentController)
    |         level = null --> INHERITS DEBUG from parent ✅
    |
    +---> Logger (name=com.concepts.UserController)
    |         level = null --> INHERITS DEBUG from parent ✅
    |
    +---> Logger (name=com.concepts.OrderController)
              level = null --> INHERITS DEBUG from parent ✅
```

---

### Advantage 2 — Override a Specific Child's Behavior

Even when you've set a level at the parent, you can override it for any specific child:

```properties
# All children of "com" use DEBUG
logging.level.com=DEBUG

# But for this specific class, override to WARN
logging.level.com.concepts.PaymentController=WARN
```

```
Logger (name=com, level=DEBUG)
    |
    +---> Logger (name=com.concepts.UserController)
    |         level = null --> inherits DEBUG ✅
    |
    +---> Logger (name=com.concepts.PaymentController)
              level = WARN  --> overridden! Only WARN and above printed
```

---

### Advantage 3 — Accepted Logs Are Propagated Upward

This is covered in full detail in Step 4 (it ties directly into Appenders). For now, just remember the rule:

> **If a log statement is REJECTED → STOP. Do not propagate.**
> **If a log statement is ACCEPTED → propagate upward till ROOT, executing all appenders along the way.**

---

## 🎯 Interview Tips

> **Q: What are the log levels in Spring Boot and what is the default?**
> ERROR, WARN, INFO (default), DEBUG, TRACE — in order of highest to lowest priority. A logger only prints statements at its configured level or higher.

> **Q: How do you change the log level for a specific class or package?**
> In `application.properties`: `logging.level.<logger-name>=<LEVEL>`. For example `logging.level.com.concepts.PaymentController=DEBUG`.

> **Q: If you have 500 classes in a package and want to enable DEBUG for all of them, do you configure each one separately?**
> No. Set the level on the package-level logger: `logging.level.com.concepts=DEBUG`. All child loggers inherit it automatically. You can still override specific classes individually if needed.

> **Q: When you call `LoggerFactory.getLogger(MyClass.class)`, how many logger objects does the framework actually create?**
> More than one. It creates one logger object for every segment of the package path — maintaining a tree hierarchy from ROOT down to your class. This hierarchy enables level inheritance and log propagation.

> **Q: What is the ROOT logger?**
> It is a special logger always created by the framework itself. It sits at the top of the hierarchy and is the parent of all loggers. You cannot delete it. By default, it has a console appender attached — which is why logs appear on the console even when you write no appender configuration yourself.

---

Ready for **Step 4** — Log Propagation + Appenders: the most critical concept — how accepted logs travel upward through the hierarchy, how all parent appenders execute, and why you see console output even without writing any appender — with a full diagram tracing a `log.info()` call step by step?

---

## Step 4: Log Propagation + Appenders — The Most Critical Concept

---

## What is an Appender?

Appender is a **component** whose job is to decide **where the log will go**.

```
+------------------+
|     Appender     |
|                  |
|  Decides the     |
|  DESTINATION     |
|  of the log      |
+------------------+
         |
         +---> Console (Standard Output)
         |
         +---> File ({fileName}.log)
         |
         +---> Database
         |
         +---> Kafka
         |
         +---> Cloud
```

The implementation library (Logback or Log4j2) uses the appender to **produce the output**. Without an appender, a log statement goes nowhere — even if it's accepted by the logger.

> **Important:** Each logger in the hierarchy **can have its own appender**. Some loggers may have one, some may have none.

---

## The Full Propagation Rule

This is the single most important rule in the entire logging framework:

```
+-------------------------------------------------------+
|                                                       |
|  If log is REJECTED  --> STOP. Do not propagate.      |
|                                                       |
|  If log is ACCEPTED  --> Propagate UPWARD till ROOT.  |
|                          Execute ALL appenders        |
|                          along the way.               |
|                                                       |
|  NOTE: Once accepted, propagation does NOT re-check   |
|        parent logger levels. It just runs appenders.  |
|                                                       |
+-------------------------------------------------------+
```

Let's build this understanding with a concrete example.

---

## Full Example — Tracing a `log.info()` Call Step by Step

Here is the setup:

```java
@RestController
public class PaymentController {

    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {

        // This log statement is accepted.
        // PaymentController Logger level is INFO
        log.info("info log");

        return "successfully fetched all payments";
    }
}
```

And here is the logger hierarchy with their levels and appenders configured:

```
+--------------------------------------------+----------+---------------------+
|           Logger                           | Level    | Appender            |
+--------------------------------------------+----------+---------------------+
| ROOT                                       | INFO     | Console Appender    |
| com                                        | (null)   | No appender         |
| com.concepts                               | WARN     | File Appender       |
| com.concepts.PaymentController             | INFO     | No appender         |
+--------------------------------------------+----------+---------------------+
```

---

### Step-by-Step Trace:

```
YOUR CODE calls:
log.info("info log");
        |
        v
+-------+----------------------------------+
|  Logger: com.concepts.PaymentController  |
|  Level = INFO                            |
|  Appender = NONE                         |
+------------------------------------------+

STEP 1: Is this log statement accepted?
        log statement level = INFO
        logger level        = INFO
        INFO >= INFO  -->  ✅ ACCEPTED

STEP 2: Does this logger have an appender?
        No appender here.
        --> Nothing printed yet.

STEP 3: Propagate UPWARD to parent.
        (Level check is DONE. Will NOT re-check levels from here.)
        |
        v
+-------+---------------------------+
|  Logger: com.concepts             |
|  Level = WARN                     |
|  Appender = File Appender         |
+-----------------------------------+

STEP 4: Does this logger have an appender?
        YES --> File Appender executes.
        --> "info log" written to {fileName}.log ✅

        NOTE: Even though this logger's level is WARN,
        the level is NOT re-checked here.
        Once accepted at PaymentController, it
        just propagates and runs appenders.

STEP 5: Propagate UPWARD to parent.
        |
        v
+-------+-----------+
|  Logger: com       |
|  Level = (null)    |
|  Appender = NONE   |
+--------------------+

STEP 6: Does this logger have an appender?
        No appender here.
        --> Nothing happens here.

STEP 7: Propagate UPWARD to parent.
        |
        v
+-------+---------------------------------+
|  Logger: ROOT                           |
|  Level = INFO                           |
|  Appender = Console Appender (DEFAULT)  |
+-----------------------------------------+

STEP 8: Does this logger have an appender?
        YES --> Console Appender executes.
        --> "info log" printed to Console ✅

STEP 9: ROOT has no parent.
        Propagation STOPS here.
```

### Final Result:

```
The single log.info("info log") statement ended up in TWO places:
+----------------------+      +----------------------+
|   Console Output     |      |   {fileName}.log     |
|                      |      |                      |
|  INFO ... info log   |      |  INFO ... info log   |
+----------------------+      +----------------------+
     (via ROOT's                  (via com.concepts'
    Console Appender)              File Appender)
```

---

## The Most Common Question — Why Do Logs Appear on Console Without Any Appender Configuration?

This is something every developer notices but rarely understands. Here's the full explanation:

```
You write this in your class:
Logger log = LoggerFactory.getLogger(PaymentController.class);
log.info("fetch the payments successfully");

You have written ZERO appender configuration.
Yet logs appear on the console. Why?
```

Here is what actually happens:

```
Logger: com.concepts.PaymentController
    Level = INFO (default)
    Appender = NONE (you wrote nothing)
    |
    | log.info accepted (INFO >= INFO)
    | no appender here, nothing printed
    v
Logger: com.concepts
    Appender = NONE (you wrote nothing)
    |
    | no appender here, nothing printed
    v
Logger: com
    Appender = NONE (you wrote nothing)
    |
    | no appender here, nothing printed
    v
Logger: ROOT
    Appender = Console Appender  <-- FRAMEWORK provides this by default!
    |
    | Console Appender executes
    v
"fetch the payments successfully" printed on Console ✅
```

> **The ROOT logger always has a Console Appender attached by default — provided by the framework itself. You never write it. It's always there.**
>
> This is why logs appear on the console in every Spring Boot app right out of the box — even when you write zero appender configuration.

---

## What Happens When a Log is REJECTED — Propagation Stops Immediately

```java
// application.properties
logging.level.com.concepts.PaymentController=DEBUG

// In your controller:
log.trace("trace log");   // trace statement level = TRACE
```

```
Logger: com.concepts.PaymentController
    Level = DEBUG
    |
    | Is TRACE >= DEBUG?
    | NO --> ❌ REJECTED
    |
    v
STOP. Do not propagate.
Nothing reaches com.concepts, com, or ROOT.
No appender anywhere executes.
Nothing is printed anywhere.
```

---

## Turning Off Propagation — `additivity = false`

By default, propagation is **always ON**. But you can turn it off for a specific logger. This means the log will be handled only by that logger's own appender and will **not travel upward**.

```xml
<!-- In logback.xml configuration -->
<logger name="com.concepts.PaymentController" additivity="false">
    <appender-ref ref="FILE"/>
</logger>
```

```
With additivity = false:

Logger: com.concepts.PaymentController
    Appender = File Appender
    additivity = FALSE
    |
    | log.info accepted
    | File Appender executes --> written to file ✅
    |
    | Propagation BLOCKED here.
    | Does NOT go to com.concepts, com, or ROOT.
    | Console Appender at ROOT never executes.
    |
    v
  STOP.

Result: Log appears ONLY in file. NOT on console.
```

> **When would you use this?** When you want a specific logger's output to go only to a dedicated file — not mixed into the general console output. Very common in production for things like audit logs or payment logs.

---

## Putting It All Together — The Complete Mental Model

```
+---------------------------------------------------------------+
|                    LOGGING FRAMEWORK                          |
|                                                               |
|   ROOT (level=INFO, appender=Console)                         |
|     |                                                         |
|     +---> com (level=null, appender=none)                     |
|              |                                                |
|              +---> com.concepts (level=WARN, appender=File)   |
|                        |                                      |
|                        +---> com.concepts.PaymentController   |
|                                 level=INFO, appender=none     |
|                                                               |
|   FLOW:                                                       |
|   1. Log statement arrives at PaymentController logger        |
|   2. Level check happens HERE and ONLY HERE                   |
|   3. If REJECTED --> STOP. Nothing propagates.                |
|   4. If ACCEPTED --> run own appenders (if any)               |
|                  --> move to parent, run its appenders        |
|                  --> keep going till ROOT                     |
|                  --> ROOT's appenders always run last         |
|                                                               |
|   KEY RULES:                                                  |
|   - Level check = ONCE (at the originating logger only)       |
|   - Appenders = executed at EVERY logger on the way up        |
|   - ROOT = always present, always has Console Appender        |
|   - additivity=false = block propagation at that logger       |
+---------------------------------------------------------------+
```

---

## 🎯 Interview Tips

> **Q: What is an Appender?**
> A component that decides the **destination** of a log — console, file, DB, Kafka, etc. Without an appender, an accepted log goes nowhere.

> **Q: What is log propagation?**
> When a log statement is accepted by a logger, it travels **upward** through the logger hierarchy till ROOT, executing every appender it finds along the way. Rejected logs do not propagate at all.

> **Q: Does propagation re-check the log level at each parent logger?**
> No. Level is checked **only once** — at the originating logger. Once accepted, it just propagates and runs appenders regardless of parent levels. This is a very common interview trap.

> **Q: Why do Spring Boot logs appear on the console without any configuration?**
> Because the ROOT logger — which is always created by the framework — has a **Console Appender attached by default**. Every accepted log propagates up to ROOT and gets printed there.

> **Q: What is `additivity` in logging?**
> It controls whether an accepted log propagates upward. By default it is `true`. Setting it to `false` on a logger stops propagation at that point — the log is only handled by that logger's own appenders and never reaches parent loggers.

> **Q: Can the same log statement end up in multiple destinations?**
> Yes. If a logger at level `com.concepts` has a File Appender, and ROOT has a Console Appender, then a single accepted log statement will be written to **both** the file and the console — because it propagates through both and executes both appenders.

---

Ready for **Step 5** — Consolidated Interview Tips + Quick Revision Summary of the entire Part 1?