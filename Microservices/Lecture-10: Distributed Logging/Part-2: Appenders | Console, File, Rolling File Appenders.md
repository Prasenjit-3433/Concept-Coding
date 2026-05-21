# Step 1 — Quick Recap + What is an Appender & Where it Fits

---

## 🔁 Quick Recap from Part 1 (Prerequisites)

Before diving into appenders, the instructor quickly recalls what was covered in Part 1, because appenders **directly build on top of that foundation**.

In Part 1, we learned:

- What is **SLF4J** → it's just an **interface** (a facade). Your application talks to SLF4J, not directly to any logging library.
- What is **Logback** and **Log4j2** → these are the actual **implementations** of SLF4J.
- How to **create a Logger object** inside your class.
- Different **logger levels**: TRACE → DEBUG → INFO → WARN → ERROR
- **Parent-Child Hierarchy** of loggers — this is very important for understanding appenders.
- The **ROOT logger** — the topmost logger in the hierarchy, always present by default.

---

## 🤔 The Problem / Question that Part 1 Ended With

At the end of Part 1, we created a simple logger like this inside `PaymentController`:

```java
@RestController
public class PaymentController {

    // This creates 1 logger with name = "com.concepts.PaymentController"
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {
        log.info("info log");
        return "successfully fetched all payments";
    }
}
```

And in `application.properties`, we overrode the log level:

```properties
logging.level.com.concepts.PaymentController=DEBUG
```

When we ran the application and hit `/payments`, the log showed up on the **console**.

**But wait — the instructor ended Part 1 with this question:**

> *"We never wrote any appender. Appender is the one who decides where the log goes. So how is the output showing up on the console?"*

This is exactly what Part 2 starts by answering.

---

## 💡 The Answer — Why Logs Show on Console Without Any Appender

This is where the **Parent-Child Hierarchy** comes into play.

When you create a logger with the name `com.concepts.PaymentController`, the logging framework **automatically maintains a hierarchy** of logger objects internally, like this:

```
ROOT  (always exists, has a default Console Appender)
 │
 └── com
      │
      └── com.concepts
               │
               └── com.concepts.PaymentController   ← your logger
```

Even though **you only created one logger object** (for `PaymentController`), the framework internally creates logger objects for each level of the package hierarchy.

Now, since we **never defined any appender** for our `com.concepts.PaymentController` logger:

- It checks: does this logger have an appender? → **No**
- It goes up to `com.concepts` → does this have an appender? → **No**
- It goes up to `com` → does this have an appender? → **No**
- It reaches **ROOT** → does ROOT have an appender? → **YES! A default Console Appender.**

So the log gets printed on the console **because of the ROOT logger's default Console Appender.**

This upward propagation behavior is called **Additivity**, and it is `true` by default. We'll cover this in full detail in Step 2.

---

## 🏗️ What Exactly is an Appender?

> *"Appender is the component that decides **where** the log will go."*

That's it. Simple. The appender answers one question: **Where should this log go?**

It could go to:
- The **Console** (your terminal, Docker logs, Azure Monitor, etc.)
- A **File** on disk
- A **Database**
- **Kafka**
- Any other custom destination

---

## 🖼️ High-Level Architecture — Where Appender Fits

This is the full picture of how everything connects:

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOUR APPLICATION                         │
│                                                                 │
│    log.info("info log")   ← you write this                      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │  uses API of
                     ▼
┌────────────────────────────────┐
│            SLF4J               │
│   (Simple Logging Facade       │
│        for Java)               │
│                                │
│  → Just an INTERFACE           │
│  → Your code only talks        │
│    to SLF4J, not to Logback    │
│    or Log4j2 directly          │
└────────────────┬───────────────┘
                 │
                 │  implemented by (default in Spring Boot)
                 ▼
┌────────────────────────────────┐
│     Logback / Log4j2           │
│     (Actual Implementation)    │
│                                │
│  → Decides: is this log        │
│    accepted based on level?    │
│  → If YES → hands it off       │
│    to the Appender             │
└────────────────┬───────────────┘
                 │
                 │  uses (helper component)
                 ▼
┌────────────────────────────────────────────────────┐
│                    APPENDER                        │
│                                                    │
│  Decides WHERE the log goes:                       │
│                                                    │
│   ┌──────────────────┐                             │
│   │  Console Appender│ → prints to STDOUT          │
│   └──────────────────┘                             │
│   ┌──────────────────┐                             │
│   │  File Appender   │ → saves to a .log file      │
│   └──────────────────┘                             │
│   ┌──────────────────┐                             │
│   │  Custom Appender │ → DB / Kafka / etc.         │
│   └──────────────────┘                             │
│                                                    │
│  Also uses → ENCODER (decides FORMAT of the log)   │
└────────────────┬───────────────────────────────────┘
                 │
                 │  output goes to
                 ▼
┌────────────────────────────────────────────────────┐
│              OUTPUT DESTINATION                    │
│                                                    │
│   - STDOUT / Console                               │
│   - fileName.log                                   │
│   - Database table                                 │
│   - Kafka topic                                    │
└────────────────┬───────────────────────────────────┘
                 │
                 │  read by
                 ▼
┌────────────────────────────────────────────────────┐
│           LOG AGGREGATION TOOLS                    │
│                                                    │
│   - DataDog                                        │
│   - ELK (ElasticSearch + Kibana)                   │
│   - Promtail                                       │
│   - etc.                                           │
└────────────────────────────────────────────────────┘
```

---

## 📌 Important Note the Instructor Makes

> *"Conceptually, both Logback and Log4j2 use Appenders to define where the logs will go, but they differ a little in syntax."*

Since **Logback is the default for Spring Boot** and is also very popular, the instructor covers everything using Logback. If you switch to Log4j2, the concepts are **exactly the same** — only minor syntax differences.

---

## 🎯 Interview Tips from This Section

> ⚠️ This is something many engineers get wrong in interviews too.

**Q: If you never define any appender in your Spring Boot application, how do logs still show up on the console?**

**A:** Because of the **Parent-Child Hierarchy** and **Additivity**. When a logger has no appender defined, it propagates upward through the hierarchy until it reaches the ROOT logger. The ROOT logger always has a **default Console Appender**, which prints the logs to the console. This upward propagation is controlled by the `additivity` property, which is `true` by default.

---

Ready for **Step 2**? — The `logback-spring.xml` file: full structure, syntax, and key concepts like Encoder, Additivity, and Level — with diagrams and code.

# Step 2 — The `logback-spring.xml` File: Structure, Syntax & Key Concepts

---

## 🤔 Why Do We Need `logback-spring.xml`?

Until now, the only way we configured logging was through `application.properties`:

```properties
logging.level.com.concepts.PaymentController=DEBUG
```

This is very limited. You can only set log levels here. You **cannot** define:
- Where logs should go (which appender)
- How logs should look (what format/pattern)
- Different appenders for different loggers
- File rolling policies
- Custom destinations like Kafka or DB

That's exactly why we need `logback-spring.xml` — it gives you **full control** over your entire logging setup.

---

## 📁 Where Does This File Live?

```
src/
 └── main/
      └── resources/
               └── logback-spring.xml   ← MUST be here, MUST have this exact name
```

> ⚠️ The file name **must** be `logback-spring.xml`. Spring Boot's framework automatically looks for this exact file name inside `src/main/resources`. If you name it anything else, Spring Boot won't pick it up.

---

## 🏗️ Overall Structure of `logback-spring.xml`

The file has **3 main building blocks**, always in this order:

```
┌─────────────────────────────────────────────┐
│           logback-spring.xml                │
│                                             │
│  1. APPENDERS  (one or many)                │
│     → decides WHERE logs go                 │
│     → each appender has an ENCODER          │
│       which decides the FORMAT              │
│                                             │
│  2. LOGGERS  (one or many)                  │
│     → maps a logger name to appender(s)     │
│     → has level + additivity settings       │
│                                             │
│  3. ROOT LOGGER  (exactly one, fallback)    │
│     → catches everything not handled        │
│       by specific loggers above             │
└─────────────────────────────────────────────┘
```

---

## 📄 Full Syntax of `logback-spring.xml`

```xml
<configuration>

    <!-- ============================= -->
    <!-- 1. APPENDERS (can have many)  -->
    <!-- ============================= -->
    <appender name="appender name" class="fully.qualified.path.of.appender.class">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- you can define as many appenders as you want -->
    <appender name="another appender" class="...">
        ...
    </appender>


    <!-- ============================= -->
    <!-- 2. LOGGERS (can have many)    -->
    <!-- ============================= -->
    <logger name="logger name" level="INFO/WARN/ERROR etc." additivity="true/false">
        <appender-ref ref="appender name"/>
        <!-- you can associate more than 1 appender to a logger -->
        <appender-ref ref="another appender name"/>
    </logger>

    <!-- you can define as many loggers as you want -->
    <logger name="..." level="..." additivity="...">
        ...
    </logger>


    <!-- ============================= -->
    <!-- 3. ROOT (fallback)            -->
    <!-- ============================= -->
    <root level="INFO/WARN/ERROR etc.">
        <appender-ref ref="appender name"/>
    </root>

</configuration>
```

---

## 🔍 Breaking Down Each Building Block

### 🔷 Block 1: Appender

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>
</appender>
```

```
┌──────────────────────────────────────────────────────┐
│                     APPENDER                         │
│                                                      │
│  name  → any name you want to give this appender     │
│          (you'll refer to it by this name later)     │
│                                                      │
│  class → the actual Java class that contains         │
│          the logic of WHERE logs go                  │
│          (Logback provides ready-made classes        │
│           for Console, File, RollingFile etc.)       │
│                                                      │
│  ┌─────────────────────────────────────────────┐     │
│  │               ENCODER                       │     │
│  │                                             │     │
│  │  decides the FORMAT / how the log looks     │     │
│  │                                             │     │
│  │  pattern: %d %-5level %logger - %msg%n      │     │
│  │                                             │     │
│  │  %d      → date & time                      │     │
│  │  %level  → log level (INFO, WARN, ERROR)    │     │
│  │  %logger → logger name (class name)         │     │
│  │  %msg    → your actual log message          │     │
│  │  %n      → new line                         │     │
│  └─────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

**Output of the above pattern looks like:**
```
2025-12-16 17:06:54,867 INFO  com.concepts.PaymentController - info log
```

> 📌 **Appender decides WHERE. Encoder decides HOW (format).** These are two separate responsibilities. Without an encoder, the appender doesn't know how to format the log and **won't work**.

---

### 🔷 Block 2: Logger

```xml
<logger name="com.concepts.PaymentController" level="INFO" additivity="true">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>   <!-- you can attach more than one appender -->
</logger>
```

A logger has **3 properties**:

#### Property 1: `name`
This is the **fully qualified name** of the logger — typically the full package path + class name. This is exactly what gets set when you do:

```java
Logger log = LoggerFactory.getLogger(PaymentController.class);
// name = "com.concepts.PaymentController"
```

#### Property 2: `level`

Specifies which log statements this logger accepts.

```
TRACE → DEBUG → INFO → WARN → ERROR
```

If level is `INFO`, then `INFO`, `WARN`, `ERROR` are accepted. `TRACE` and `DEBUG` are rejected.

> ⚠️ **Important:** If you define the level **both** in `logback-spring.xml` and in `application.properties`, the `application.properties` value takes **higher priority**.

> ⚠️ **If level is not defined for a logger**, it will **always** inherit from its immediate parent logger. If the parent also doesn't have a level, it keeps going up until ROOT. This inheritance happens **regardless of the `additivity` setting** — level inheritance is **always upward**, no exceptions.

#### Property 3: `additivity`

This controls **appender propagation only** — whether accepted logs should also run the appenders of parent loggers.

```
additivity = true  (DEFAULT)
→ After running its own appender(s), the log also 
  travels upward and runs parent appenders too (till ROOT)

additivity = false
→ Only runs its own appender(s). Does NOT travel upward.
→ ⚠️ If you set this to false, make sure this logger 
  has AT LEAST ONE appender. Otherwise you'll get a 
  WARNING: "No appenders present in context"
```

> ⚠️ **Very common interview trap:** Many engineers confuse `additivity` with level inheritance. Remember — `additivity` **only** controls appender propagation. Level is **always** inherited from parent if not defined, regardless of `additivity`.

---

### 🔷 Block 3: Root Logger

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
</root>
```

```
┌──────────────────────────────────────────────┐
│                ROOT LOGGER                   │
│                                              │
│  → Always present (you don't create it,      │
│    it always exists)                         │
│                                              │
│  → Acts as the FALLBACK                      │
│    In production you can have thousands of   │
│    classes. You can't write a specific       │
│    <logger> for each one. The hierarchy      │
│    handles most cases, but ROOT is the       │
│    final safety net.                         │
│                                              │
│  → Default level: INFO                       │
│                                              │
│  → Default appender: Console                 │
│    (even without any logback-spring.xml,     │
│     ROOT has console appender by default)    │
└──────────────────────────────────────────────┘
```

---

## 🖼️ Full Picture — How All 3 Blocks Work Together

Let's say we have this setup:

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.concepts.PaymentController" level="INFO" additivity="true">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

And in Java:
```java
log.info("info log");  // from PaymentController
```

Here's exactly what happens step by step:

```
log.info("info log") called
         │
         ▼
┌─────────────────────────────────────────────────┐
│  Logger: com.concepts.PaymentController         │
│  Level: INFO (defined)                          │
│  Appender: CONSOLE                              │
│  Additivity: true                               │
│                                                 │
│  Step 1: Is "info" >= INFO level? → YES         │
│          Log is ACCEPTED                        │
│                                                 │
│  Step 2: Run own appender → CONSOLE             │
│          ✅ Printed on Console (1st time)        │
│                                                 │
│  Step 3: additivity=true → propagate upward     │
└──────────────────┬──────────────────────────────┘
                   │ propagates up
                   ▼
┌─────────────────────────────────────────────────┐
│  Logger: com.concepts                           │
│  No config defined → No appender                │
│  → Nothing happens here, keep going up          │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│  Logger: com                                    │
│  No config defined → No appender                │
│  → Nothing happens here, keep going up          │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│  ROOT Logger                                    │
│  Level: INFO                                    │
│  Appender: CONSOLE                              │
│                                                 │
│  → Appender runs → CONSOLE                      │
│  ✅ Printed on Console (2nd time)                │
└─────────────────────────────────────────────────┘

RESULT: Log printed TWICE on console!
```

> ⚠️ **This is a very common production bug** — logs appearing multiple times. The fix is to set `additivity="false"` on your specific logger once you've assigned it its own appender.

---

## 🎯 Key Rule: Level Inheritance vs Additivity — Don't Mix Them Up

This is something the **instructor specifically calls out as a common mistake** in both day-to-day work and interviews.

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  ADDITIVITY  → controls APPENDER propagation ONLY       │
│                true  = run parent appenders too         │
│                false = stop here, don't go upward       │
│                                                         │
│  LEVEL       → ALWAYS inherited from parent             │
│                if not defined on the current logger     │
│                additivity has ZERO effect on this       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Example to prove this:**

```xml
<logger name="com.concepts" level="WARN">
    <appender-ref ref="CONSOLE"/>
</logger>

<!-- level is MISSING here, additivity is false -->
<logger name="com.concepts.PaymentController" additivity="false">
    <appender-ref ref="CONSOLE"/>
</logger>

<root level="INFO">
    <appender-ref ref="CONSOLE"/>
</root>
```

```java
log.error("error log");  // ✅ accepted
log.warn("warn log");    // ✅ accepted
log.info("info log");    // ❌ rejected
log.debug("debug log");  // ❌ rejected
log.trace("trace log");  // ❌ rejected
```

**Why?**

- `com.concepts.PaymentController` has **no level defined**.
- Even though `additivity=false`, level inheritance **still goes upward**.
- Its immediate parent `com.concepts` has level `WARN`.
- So `com.concepts.PaymentController` **inherits WARN**.
- Only `ERROR` and `WARN` are accepted. `INFO`, `DEBUG`, `TRACE` are rejected.
- Because `additivity=false`, logs do NOT propagate to ROOT — only printed once via its own CONSOLE appender.

---

## 🎯 Interview Tips from This Section

**Q: What is the difference between `additivity` and level inheritance in Logback?**

**A:** `additivity` controls only whether accepted logs should propagate upward through the logger hierarchy to run parent appenders. Level, on the other hand, is **always** inherited from the immediate parent if not explicitly set on the current logger — this behavior has nothing to do with `additivity`. Even if `additivity=false`, the logger will still look upward to inherit its level.

**Q: What happens if you set `additivity=false` but don't attach any appender to that logger?**

**A:** You'll get a warning — "No appenders present in context." The log won't appear anywhere. So whenever you use `additivity=false`, always make sure at least one appender is attached to that logger.

**Q: Which takes higher priority — level set in `logback-spring.xml` or `application.properties`?**

**A:** `application.properties` takes higher priority.

---

Ready for **Step 3**? — Console Appender in depth, with diagram + code + what "console" actually means across different environments (local, Docker, Azure).

# Step 3 — Console Appender

---

## 🤔 What is the Console Appender?

We've been seeing logs on the terminal/console since Part 1, and we now know it's because of the **Console Appender**. But the instructor points out that there's a very common confusion among engineers about what "console" actually means.

> *"Believe me, this confusion comes — what exactly is console?"*

So before jumping into code, let's first understand what console really means.

---

## 💡 What Does "Console" Actually Mean?

From your **Java code's perspective**, the Console Appender does one simple thing:

> It opens an **output stream** and puts the log data into it. That stream has a name: **STDOUT (Standard Output).**

That's it. Java doesn't care about terminals, Docker, or Azure. It just writes to **STDOUT**.

Now, **who reads this STDOUT stream depends on where your application is running:**

```
┌─────────────────────────────────────────────────────────────────────┐
│                         YOUR JAVA CODE                              │
│                                                                     │
│   log.info("info log")                                              │
│          │                                                          │
│          ▼                                                          │
│   Console Appender                                                  │
│          │                                                          │
│          │  writes to                                               │
│          ▼                                                          │
│   ┌─────────────────────┐                                           │
│   │  STDOUT             │  ← Standard Output Stream                 │
│   │  (output stream)    │     Java only knows this much             │
│   └──────────┬──────────┘                                           │
└──────────────┼──────────────────────────────────────────────────────┘
               │
               │  who reads STDOUT depends on environment
               │
    ┌──────────┴────────────────────────────────────────┐
    │                                                   │
    ▼                                                   ▼
┌──────────────────────┐    ┌──────────────────────────────────────┐
│   LOCAL MACHINE      │    │         DOCKER                       │
│                      │    │                                      │
│  Terminal reads      │    │  Docker Logging Driver reads STDOUT  │
│  STDOUT content      │    │  You see logs via:                   │
│                      │    │  "docker logs <container-name>"      │
│  → your IDE console  │    │                                      │
│  → your terminal     │    └──────────────────────────────────────┘
└──────────────────────┘
                                         ▼
                        ┌──────────────────────────────────────┐
                        │         CLOUD (e.g. Azure)           │
                        │                                      │
                        │  Azure Monitor collects STDOUT logs  │
                        │  and shows them on Azure's dashboard │
                        │                                      │
                        └──────────────────────────────────────┘
```

> 📌 **Key Point:** Java writes to STDOUT. The **platform** (local machine, Docker, Azure, etc.) is responsible for reading STDOUT and showing it wherever it shows. You don't have to worry about this — the platform handles log collection automatically.

---

## ✅ Characteristics of Console Appender

```
┌──────────────────────────────────────────────┐
│           CONSOLE APPENDER                   │
│                                              │
│  ✅ Fast                                      │
│     → Just writes to a stream, nothing heavy │
│                                              │
│  ✅ No persistence                            │
│     → Logs are NOT saved anywhere on disk    │
│     → No risk of running out of disk space   │
│                                              │
│  ✅ Platform handles log collection           │
│     → You don't need to manage anything      │
│     → Local: Terminal reads it               │
│     → Docker: Docker Logging Driver reads it │
│     → Azure: Azure Monitor reads it          │
│                                              │
│  ⚠️ No persistence means                     │
│     → Once the log scrolls past or the       │
│        container restarts, it's gone         │
│     → Not suitable alone for production      │
│        where you need to query past logs     │
└──────────────────────────────────────────────┘
```

---

## 💻 Code — Console Appender Setup

### Java Class (PaymentController.java)

```java
@RestController
public class PaymentController {

    // Creates 1 logger with name = "com.concepts.PaymentController"
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {
        log.info("info log");
        return "successfully fetched all payments";
    }
}
```

### logback-spring.xml — Basic Console Appender (No specific logger defined)

```xml
<configuration>

    <!-- Console Appender: writes logs to STDOUT -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">

        <!-- Encoder decides the FORMAT of the log -->
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>

    </appender>

    <!-- No specific logger defined for com.concepts.PaymentController -->
    <!-- So it will propagate upward till ROOT and use ROOT's appender -->

    <!-- ROOT Logger — fallback -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

**What happens here:**

```
log.info("info log") called from PaymentController
        │
        ▼
com.concepts.PaymentController → no appender defined → propagate up
        │
        ▼
com.concepts → no appender defined → propagate up
        │
        ▼
com → no appender defined → propagate up
        │
        ▼
ROOT → level INFO, appender = CONSOLE
        │
        ▼
✅ Log printed once on console
```

---

### logback-spring.xml — Console Appender with Specific Logger (additivity=true)

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Specific logger for PaymentController -->
    <!-- additivity=true → will ALSO run parent appenders after its own -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="true">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

**What happens here:**

```
log.info("info log") called
        │
        ▼
com.concepts.PaymentController
  → level INFO, log accepted ✅
  → runs own CONSOLE appender → ✅ printed (1st time)
  → additivity=true → propagates upward
        │
        ▼
com.concepts → no appender → skip
        │
        ▼
com → no appender → skip
        │
        ▼
ROOT → CONSOLE appender → ✅ printed (2nd time)

⚠️ RESULT: Log printed TWICE!
```

---

### logback-spring.xml — Console Appender with Specific Logger (additivity=false) ✅ Correct Way

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- additivity=false → will NOT run parent appenders -->
    <!-- ✅ Log printed only once -->
    <!-- ⚠️ Make sure at least 1 appender is attached when additivity=false -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

**What happens here:**

```
log.info("info log") called
        │
        ▼
com.concepts.PaymentController
  → level INFO, log accepted ✅
  → runs own CONSOLE appender → ✅ printed (1st time)
  → additivity=false → STOPS here, does NOT go upward

ROOT's CONSOLE appender → ❌ NOT executed

✅ RESULT: Log printed exactly ONCE
```

---

## 🔍 Understanding the Log Pattern

Let's break down what `%d %-5level %logger - %msg%n` actually produces:

```
Pattern:   %d        %-5level   %logger                        - %msg      %n
           │         │          │                                │          │
           ▼         ▼          ▼                                ▼          ▼
Output:    2025-12-16 INFO      com.concepts.PaymentController - info log  (newline)
```

```
┌──────────────────────────────────────────────────────────┐
│               PATTERN PLACEHOLDERS                       │
│                                                          │
│  %d          → full date and time                        │
│                e.g. 2025-12-16 17:06:54,867              │
│                                                          │
│  %-5level    → log level, padded to 5 chars              │
│                e.g. INFO , WARN , ERROR, DEBUG           │
│                (the - means left-aligned, 5 = width)     │
│                                                          │
│  %logger     → full logger name (package + class)        │
│                e.g. com.concepts.PaymentController       │
│                                                          │
│  %msg        → your actual log message                   │
│                e.g. "info log"                           │
│                                                          │
│  %n          → newline character                         │
│                moves to next line after each log         │
└──────────────────────────────────────────────────────────┘
```

**Full sample output:**
```
2025-12-16 17:06:54,867 INFO  com.concepts.PaymentController - info log
```

> 📌 The instructor mentions the full pattern details will be covered separately in a later part. For now, understand that the encoder + pattern defines exactly how your log line looks.

---

## ⚠️ Common Mistake — additivity=false with No Appender

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- ❌ WRONG: additivity=false but NO appender attached -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="false">
        <!-- appender-ref is commented out / missing -->
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

**Result:**
```
⚠️ WARNING: No appenders present in context 
   [com.concepts.PaymentController]

→ Your log disappears completely.
→ It won't show on console.
→ It won't go to ROOT either (additivity=false).
→ It's just gone.
```

> ✅ **Rule:** Whenever `additivity=false`, **always** make sure the logger has at least one `<appender-ref>` attached.

---

## 🎯 Interview Tips from This Section

**Q: What does Console Appender actually do internally?**

**A:** It opens an output stream called **STDOUT (Standard Output)** and writes the log data into it. The platform (local terminal, Docker, Azure, etc.) is then responsible for reading this stream and displaying it. Java only writes to STDOUT — it doesn't know or care about the platform.

**Q: Why would the same log appear multiple times on the console?**

**A:** Because of `additivity=true` (which is the default). If a specific logger has a Console Appender, and the ROOT logger also has a Console Appender, and `additivity=true`, the log travels upward and gets printed by both appenders. The fix is to set `additivity=false` on the specific logger — but then make sure it has at least one appender attached.

**Q: Is Console Appender suitable for production?**

**A:** It depends on the setup. Console Appender itself is fast and has no persistence overhead. In containerized environments like Docker or Kubernetes, it's actually common to use Console Appender and let the platform (Docker logging driver, cloud monitor) handle log collection. However, if you need persistent, queryable logs stored on disk, you'd combine it with a Rolling File Appender or a custom appender.

---

Ready for **Step 4**? — File Appender: how it works, why it's NOT safe for production alone, and the `append=true/false` behavior — with diagram + code.

# Step 4 — File Appender

---

## 🤔 What is the File Appender?

So far, Console Appender just writes to STDOUT — **no persistence**. Once the log scrolls past or the application restarts, it's gone.

The **File Appender** solves this by writing logs directly to a **file on disk**. The logs are now saved and can be read later.

But the instructor immediately flags something important:

> *"It persists logs, but it is NOT safe for production. Don't just go for File Appender."*

Let's understand why — but first, let's see how it works.

---

## 💡 How File Appender Works

```
┌─────────────────────────────────────────────────────────────────┐
│                        YOUR JAVA CODE                           │
│                                                                 │
│   log.info("info log")                                          │
│          │                                                      │
│          ▼                                                      │
│   File Appender                                                 │
│          │                                                      │
│          │  writes to                                           │
│          ▼                                                      │
│   ┌─────────────────────────────┐                               │
│   │   logs/app.log              │  ← file on disk               │
│   │                             │                               │
│   │   2025-12-16 INFO  Pay.. -  │                               │
│   │   info log                  │                               │
│   │   2025-12-16 INFO  Pay.. -  │                               │
│   │   info log                  │  ← keeps growing              │
│   │   2025-12-16 INFO  Pay.. -  │     forever...                │
│   │   info log                  │     ⚠️ No size limit          │
│   │   ...                       │                               │
│   └─────────────────────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

Every log statement gets **appended** to this file. The file keeps growing with every request, every restart, every log statement — **forever**, with no automatic size control.

That's exactly why it's not safe for production — **your disk can run out of space.**

---

## 🔍 The `append` Property — True vs False

File Appender has a very important property called `append`:

```
┌────────────────────────────────────────────────────────────────┐
│                     append = true                              │
│                                                                │
│  → Logs are RETAINED even when the application restarts        │
│  → When app restarts, new logs are added AFTER old logs        │
│  → File keeps growing across restarts                          │
│                                                                │
│  app.log before restart:                                       │
│  ┌──────────────────────────────────┐                          │
│  │ 2025-12-16 INFO Pay.. - log 1   │                           │
│  │ 2025-12-16 INFO Pay.. - log 2   │                           │
│  └──────────────────────────────────┘                          │
│                                                                │
│  app.log after restart:                                        │
│  ┌──────────────────────────────────┐                          │
│  │ 2025-12-16 INFO Pay.. - log 1   │ ← old logs still here     │
│  │ 2025-12-16 INFO Pay.. - log 2   │                           │
│  │ 2025-12-16 INFO Pay.. - log 3   │ ← new logs added after    │
│  │ 2025-12-16 INFO Pay.. - log 4   │                           │
│  └──────────────────────────────────┘                          │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     append = false                             │
│                                                                │
│  → When application RESTARTS, the file is CLEARED first        │
│  → Then new logs start getting written from scratch            │
│  → Old logs from previous run are LOST                         │
│                                                                │
│  app.log before restart:                                       │
│  ┌──────────────────────────────────┐                          │
│  │ 2025-12-16 INFO Pay.. - log 1   │                           │
│  │ 2025-12-16 INFO Pay.. - log 2   │                           │
│  └──────────────────────────────────┘                          │
│                                                                │
│  app.log after restart:                                        │
│  ┌──────────────────────────────────┐                          │
│  │ 2025-12-16 INFO Pay.. - log 3   │ ← file wiped, fresh start │
│  │ 2025-12-16 INFO Pay.. - log 4   │                           │
│  └──────────────────────────────────┘                          │
└────────────────────────────────────────────────────────────────┘
```

---

## 💻 Code — File Appender Setup

### Java Class (PaymentController.java)

```java
@RestController
public class PaymentController {

    // Creates 1 logger with name = "com.concepts.PaymentController"
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {
        log.info("info log");
        return "successfully fetched all payments";
    }
}
```

### logback-spring.xml — File Appender + Console Appender

```xml
<configuration>

    <!-- Appender 1: Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!--
        Appender 2: File Appender
        → It will create a file "app.log" inside the "logs" folder
        → And keep on appending the log statements to it

        append = true
            → Logs are retained even when application is restarted.
            → New logs are added after old logs.

        append = false
            → When application restarts, "app.log" file is CLEARED
            → Fresh logs start getting written again from scratch.

        ⚠️ Remember: this file will keep on growing with no size limit
           So using FileAppender alone in production should be avoided.
    -->
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/app.log</file>
        <append>true</append>
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!--
        This Logger has 2 appenders associated with it:
        → CONSOLE appender
        → FILE appender
        So every accepted log statement will go to BOTH console AND file.

        additivity = false
        → Will NOT propagate upward to ROOT.
        → ROOT's console appender will NOT run.
        → Logs printed exactly once on console + once in file.
    -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 🖼️ Full Flow Diagram — What Happens When We Hit `/payments`

```
log.info("info log") called from PaymentController
        │
        ▼
┌───────────────────────────────────────────────────────┐
│  Logger: com.concepts.PaymentController               │
│  Level:  INFO (defined)                               │
│  Appenders: CONSOLE + FILE                            │
│  Additivity: false                                    │
│                                                       │
│  Step 1: Is "info" >= INFO? → YES → Log ACCEPTED ✅    │
│                                                       │
│  Step 2: Run CONSOLE appender                         │
│          → writes to STDOUT                           │
│          ✅ Log visible on terminal/console            │
│                                                       │
│  Step 3: Run FILE appender                            │
│          → writes to logs/app.log on disk             │
│          ✅ Log saved in file                          │
│                                                       │
│  Step 4: additivity=false → STOPS here                │
│          ROOT's appender does NOT run                 │
└───────────────────────────────────────────────────────┘

OUTPUT:
  Console: 2025-12-16 19:53:19,587 INFO com.concepts.PaymentController - info log
  File (logs/app.log): 2025-12-16 19:53:19,587 INFO com.concepts.PaymentController - info log
```

---

## 📁 Folder Structure After Running the App

```
your-project/
 └── logs/
      └── app.log    ← created automatically by File Appender
```

**Contents of `logs/app.log`:**
```
2025-12-16 19:53:19,587 INFO  com.concepts.PaymentController - info log
```

---

## ⚠️ Why File Appender is NOT Safe for Production

```
┌──────────────────────────────────────────────────────────────┐
│               THE PROBLEM WITH FILE APPENDER                 │
│                                                              │
│  Imagine your application is running in production           │
│  with heavy traffic — thousands of requests per minute.      │
│                                                              │
│  Day 1:   app.log = 500 MB                                   │
│  Day 2:   app.log = 1.2 GB                                   │
│  Day 3:   app.log = 2.8 GB                                   │
│  Day 10:  app.log = 15 GB  ← disk almost full                │
│  Day 11:  ❌ Disk OUT OF SPACE                                │
│           → Application crashes                              │
│           → Server goes down                                 │
│                                                              │
│  There is NO size control in File Appender.                  │
│  The file just keeps growing forever.                        │
│                                                              │
│  ✅ SOLUTION → Rolling File Appender                          │
│     (covered in Step 5 and Step 6)                           │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Console Appender vs File Appender — Side by Side

```
┌─────────────────────┬──────────────────────┬───────────────────────┐
│     Property        │   Console Appender   │    File Appender      │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Where logs go       │ STDOUT stream        │ File on disk          │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Persistence         │ ❌ No                 │ ✅ Yes                 │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Disk space risk     │ ❌ None               │ ⚠️ High (no limit)    │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Speed               │ ✅ Fast               │ Slightly slower       │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Production safe?    │ ✅ Yes (with          │ ⚠️ NOT alone          │
│                     │    platform support) │                       │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Size control        │ N/A                  │ ❌ No                  │
├─────────────────────┼──────────────────────┼───────────────────────┤
│ Logback class       │ ch.qos.logback.core  │ ch.qos.logback.core   │
│                     │ .ConsoleAppender     │ .FileAppender         │
└─────────────────────┴──────────────────────┴───────────────────────┘
```

---

## 🎯 Interview Tips from This Section

**Q: What is the difference between Console Appender and File Appender?**

**A:** Console Appender writes logs to STDOUT — it's fast, has no persistence, and the platform (terminal, Docker, Azure) handles reading it. File Appender writes logs to a file on disk, giving you persistence. However, File Appender has no size control — the file keeps growing forever, making it unsafe to use alone in production.

**Q: What does `append=true` vs `append=false` do in File Appender?**

**A:** With `append=true`, logs are retained across application restarts — new logs are added after existing ones. With `append=false`, the log file is wiped clean every time the application restarts, and logging starts fresh.

**Q: Is File Appender production-ready?**

**A:** No, not on its own. The file has no size limit — it keeps growing indefinitely and can cause the server to run out of disk space and crash. The production-ready alternative is the **Rolling File Appender** with a **Size + Time Based Rolling Policy**, which gives full control over file size, rotation frequency, and total disk usage.

---

Ready for **Step 5**? — Rolling File Appender with **Time-Based Rolling Policy**: how log rotation works, the `%d{}` pattern, `maxHistory`, and full code + diagram.

# Step 5 — Rolling File Appender: Time-Based Rolling Policy

---

## 🤔 What Problem Does Rolling File Appender Solve?

We saw in Step 4 that File Appender has one big problem — **no size control**. The file grows forever and can crash your server by filling up disk space.

**Rolling File Appender** solves this by introducing the concept of **Log Rotation**:

> *"When a certain condition is met — like a day has passed, or the file has reached a size limit — close the current log file, archive it, and start writing to a new fresh file."*

This gives you:
- ✅ Persistence (logs saved to disk)
- ✅ Controlled file size
- ✅ Automatic archiving of old logs
- ✅ Automatic deletion of logs older than a set limit

Rolling File Appender supports **different policies** that decide **when and how** log files are rotated. In this step, we cover the first one — **Time-Based Rolling Policy**.

---

## 💡 What is Time-Based Rolling Policy?

> *"It will create a new log file based on time — daily, hourly, every minute, monthly — whatever you configure."*

The rotation frequency is controlled by the **`%d{}`** placeholder inside `<fileNamePattern>`:

```
┌──────────────────────────────────────────────────────────────┐
│              %d{} — Controls Rotation Frequency              │
│                                                              │
│  %d{yyyy-MM-dd}       → rotation happens DAILY               │
│                          new file created every day          │
│                                                              │
│  %d{yyyy-MM-dd-HH}    → rotation happens HOURLY              │
│                          new file created every hour         │
│                                                              │
│  %d{yyyy-MM-dd-HH-mm} → rotation happens every MINUTE        │
│                          new file created every minute       │
│                          (good for testing/demo purposes)    │
│                                                              │
│  %d{yyyy-MM}          → rotation happens MONTHLY             │
│                          new file created every month        │
└──────────────────────────────────────────────────────────────┘
```

> 📌 The instructor uses **minute-wise** rotation for the demo so that results are visible quickly during testing. In production you'd typically use **daily** rotation.

---

## 🖼️ How Time-Based Rolling Works — The Two-File Concept

This is something that confuses many people. The instructor explains it clearly:

```
┌──────────────────────────────────────────────────────────────────┐
│              HOW TIME-BASED ROLLING WORKS                        │
│                                                                  │
│  There are always TWO types of files at play:                    │
│                                                                  │
│  1. ACTIVE FILE (logs/app.log)                                   │
│     → The current time period's logs go here                     │
│     → This is the file being actively written to RIGHT NOW       │
│                                                                  │
│  2. ARCHIVED FILES (logs/app-{date}.log)                         │
│     → When the time period ends (e.g. minute changes),           │
│       the active file is "archived" with a timestamp in name     │
│     → A fresh app.log is created for the new time period         │
│                                                                  │
│  Example (minute-wise rotation):                                 │
│                                                                  │
│  20:41 → logs being written to → logs/app.log                    │
│  20:42 → time period changes!                                    │
│          old logs archived to → logs/app-2025-12-16-2041.log     │
│          new logs written to  → logs/app.log (fresh)             │
│  20:43 → time period changes!                                    │
│          old logs archived to → logs/app-2025-12-16-2042.log     │
│          new logs written to  → logs/app.log (fresh)             │
│                                                                  │
│  So at any point:                                                │
│  logs/app.log              ← current minute (active)             │
│  logs/app-2025-12-16-2041.log ← previous minute (archived)       │
│  logs/app-2025-12-16-2040.log ← 2 minutes ago (archived)         │
│  ...                                                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔍 What is `maxHistory`?

`maxHistory` controls **how many archived files to keep**. The key is to **read it relative to the rotation frequency you chose**:

```
┌──────────────────────────────────────────────────────────────┐
│                   maxHistory = 30                            │
│                                                              │
│  If rotation is MINUTE-wise → keep last 30 MINUTES of logs   │
│  If rotation is HOURLY      → keep last 30 HOURS of logs     │
│  If rotation is DAILY       → keep last 30 DAYS of logs      │
│  If rotation is MONTHLY     → keep last 30 MONTHS of logs    │
│                                                              │
│  Once the limit is exceeded, the OLDEST archived file        │
│  gets automatically deleted.                                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 💻 Code — Time-Based Rolling Policy Setup

### Java Class (PaymentController.java)

```java
@RestController
public class PaymentController {

    // Creates 1 logger with name = "com.concepts.PaymentController"
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {
        log.info("info log");
        return "successfully fetched all payments";
    }
}
```

### logback-spring.xml — Time-Based Rolling Policy

```xml
<configuration>

    <!-- Appender 1: Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!--
        Appender 2: Rolling File Appender with Time-Based Rolling Policy

        → class is still RollingFileAppender (same for all rolling policies)
        → <file> = the ACTIVE file where current time period logs go
        → <rollingPolicy> = defines WHEN and HOW rotation happens
    -->
    <appender name="ROLLING_FILE_TIME_BASED"
              class="ch.qos.logback.core.rolling.RollingFileAppender">

        <!-- Active file: current minute's logs go here -->
        <file>logs/app.log</file>

        <!--
            Rolling Policy: TimeBasedRollingPolicy
            → This class is already present in Logback framework
            → We don't have to write our own implementation
            → All we configure is:
               1. fileNamePattern → naming format for archived files
               2. maxHistory      → how many archived files to keep
        -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">

            <!--
                fileNamePattern → naming convention for ARCHIVED files
                %d{yyyy-MM-dd-HHmm} → minute-wise rotation (for testing)

                So archived files will be named like:
                logs/app-2025-12-16-2041.log
                logs/app-2025-12-16-2042.log
                etc.

                For daily rotation (production), use:
                logs/app-%d{yyyy-MM-dd}.log
            -->
            <fileNamePattern>logs/app-%d{yyyy-MM-dd-HHmm}.log</fileNamePattern>

            <!--
                maxHistory = 30
                Since we are using minute-wise rotation:
                → Last 30 minutes of logs will be kept
                → Older archived files will be automatically deleted

                For daily rotation:
                → Last 30 days of logs will be kept
            -->
            <maxHistory>30</maxHistory>

        </rollingPolicy>

        <!-- Encoder: format of the log written to the file -->
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>

    </appender>

    <!--
        Logger for PaymentController
        → Uses BOTH Console and Rolling File appenders
        → additivity=false → won't propagate to ROOT
        → Logs go to console AND rolling file
    -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ROLLING_FILE_TIME_BASED"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 🖼️ Full Flow Diagram — What Happens Step by Step

```
Application running, minute = 20:41
log.info("info log") called
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  Logger: com.concepts.PaymentController                 │
│  Level: INFO → log ACCEPTED ✅                           │
│  Appenders: CONSOLE + ROLLING_FILE_TIME_BASED           │
│  Additivity: false                                      │
│                                                         │
│  Step 1: CONSOLE appender runs                          │
│          → writes to STDOUT                             │
│          ✅ visible on terminal                          │
│                                                         │
│  Step 2: ROLLING_FILE_TIME_BASED appender runs          │
│          → current minute is 20:41                      │
│          → writes to logs/app.log (active file)         │
│          ✅ saved to file                                │
│                                                         │
│  Step 3: additivity=false → STOPS, ROOT not executed    │
└─────────────────────────────────────────────────────────┘

                    ⏱️ Time passes... minute becomes 20:42

        │
        ▼
┌─────────────────────────────────────────────────────────┐
│  TIME ROTATION TRIGGERED                                │
│                                                         │
│  Logback detects: minute has changed (20:41 → 20:42)    │
│                                                         │
│  Step 1: Close current logs/app.log                     │
│  Step 2: Rename/archive it as:                          │
│          logs/app-2025-12-16-2041.log  ← archived       │
│  Step 3: Create fresh logs/app.log     ← new active     │
│  Step 4: New logs for 20:42 go into                     │
│          the fresh logs/app.log                         │
└─────────────────────────────────────────────────────────┘

RESULT — Folder structure looks like:
logs/
 ├── app.log                      ← current active (20:42)
 └── app-2025-12-16-2041.log      ← archived (20:41)
```

---

## 📁 What the Folder Looks Like After Multiple Rotations

```
logs/
 ├── app.log                         ← current active file (right now)
 ├── app-2025-12-16-2044.log         ← 1 minute ago
 ├── app-2025-12-16-2043.log         ← 2 minutes ago
 ├── app-2025-12-16-2042.log         ← 3 minutes ago
 ├── app-2025-12-16-2041.log         ← 4 minutes ago
 │   ...
 └── app-2025-12-16-2015.log         ← 30 minutes ago (oldest kept)
     ← any file older than 30 minutes gets auto-deleted
```

---

## ⚠️ Why Time-Based Rolling Policy Alone is Still NOT Production-Ready

The instructor is clear about this:

> *"This is not production ready. The best is Size + Time Based Rolling Policy."*

Here's why:

```
┌──────────────────────────────────────────────────────────────────┐
│          PROBLEM WITH TIME-BASED ROLLING ALONE                   │
│                                                                  │
│  Scenario: Daily rotation, maxHistory = 30                       │
│                                                                  │
│  Day 1 (low traffic):  app-2025-12-15.log = 10 MB  ✅             │
│  Day 2 (low traffic):  app-2025-12-16.log = 12 MB  ✅             │
│  Day 3 (HIGH traffic): app-2025-12-17.log = 500 MB ⚠️            │
│  Day 4 (HIGH traffic): app-2025-12-18.log = 800 MB ⚠️            │
│  Day 5 (HIGH traffic): app-2025-12-19.log = 1.2 GB ❌             │
│                                                                  │
│  You have 30 days * potentially huge files                       │
│  = disk can still run out of space!                              │
│                                                                  │
│  TIME-BASED POLICY controls WHEN a new file is created.          │
│  But it does NOT control HOW BIG each file can get.              │
│                                                                  │
│  ✅ SOLUTION → Size + Time Based Rolling Policy                   │
│     (covered in Step 6 — the production-ready approach)          │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📊 File Appender vs Time-Based Rolling File Appender

```
┌──────────────────────┬────────────────────────┬────────────────────────────┐
│      Property        │    File Appender       │  Time-Based Rolling        │
│                      │                        │  File Appender             │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ Persistence          │ ✅ Yes                  │ ✅ Yes                      │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ File rotation        │ ❌ No                   │ ✅ Yes (time-based)         │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ File size control    │ ❌ No                   │ ❌ No                       │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ Auto delete old logs │ ❌ No                   │ ✅ Yes (maxHistory)         │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ Disk space risk      │ ⚠️ Very High           │ ⚠️ Still possible          │
│                      │ (one file grows        │ (each file unbounded       │
│                      │  forever)              │  in size)                  │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ Production ready?    │ ❌ No                   │ ⚠️ Not fully               │
├──────────────────────┼────────────────────────┼────────────────────────────┤
│ Logback class        │ ch.qos.logback.core    │ ch.qos.logback.core        │
│                      │ .FileAppender          │ .rolling                   │
│                      │                        │ .RollingFileAppender       │
│                      │                        │ + TimeBasedRollingPolicy   │
└──────────────────────┴────────────────────────┴────────────────────────────┘
```

---

## 🎯 Interview Tips from This Section

**Q: What is Log Rotation and why do we need it?**

**A:** Log rotation is the process of closing the current log file when a condition is met (time passed or size limit reached), archiving it with a timestamp in the filename, and starting a fresh new log file. We need it because without rotation, a single log file grows indefinitely and can fill up the disk and crash the server.

**Q: What does `maxHistory=30` mean in Time-Based Rolling Policy?**

**A:** It means keep the last 30 archived files. The unit depends on the rotation frequency you've configured — if rotation is daily, it keeps last 30 days; if hourly, last 30 hours; if minute-wise, last 30 minutes. Files older than this limit are automatically deleted by Logback.

**Q: What is the difference between the active file and the archived files in Rolling File Appender?**

**A:** The active file (e.g. `logs/app.log`) is the file being actively written to right now — for the current time period. When the time period ends (e.g. the minute or day changes), that file gets archived with a timestamp in its name (e.g. `logs/app-2025-12-16-2041.log`) and a fresh `app.log` is created for the new time period.

**Q: Is Time-Based Rolling Policy alone production-ready?**

**A:** No. Time-Based Policy controls only **when** a new file is created — not **how big** each file can get. During high traffic, a single day's log file could grow to several gigabytes. The production-ready solution is the **Size + Time Based Rolling Policy**, which adds a per-file size limit and a total disk cap on top of time-based rotation.

---

Ready for **Step 6**? — Rolling File Appender with **Size + Time Based Rolling Policy** — the production-ready approach, with `maxFileSize`, `totalSizeCap`, the `%i` index, and full diagram + code.

# Step 6 — Rolling File Appender: Size + Time Based Rolling Policy (Best for Production)

---

## 🤔 What Problem Does This Solve?

Recall from Step 5:

- Time-Based Policy controls **when** a new file is created ✅
- But it does **NOT** control **how big** each file can get ❌

In a high-traffic production system, even a single day's log file can grow to several gigabytes. That's still dangerous.

**Size + Time Based Rolling Policy** solves this by adding **two more controls** on top of time-based rotation:

```
┌──────────────────────────────────────────────────────────────────┐
│           SIZE + TIME BASED ROLLING POLICY ADDS:                 │
│                                                                  │
│  1. maxFileSize   → how big ONE log file can get                 │
│                     (e.g. 100MB — once reached, rotate NOW)      │
│                                                                  │
│  2. totalSizeCap  → how much disk space ALL log files            │
│                     combined can use                             │
│                     (e.g. 2GB — once reached, delete oldest)     │
└──────────────────────────────────────────────────────────────────┘
```

Together with `maxHistory` from the time-based policy, you now have **full control** over your logging storage.

---

## 💡 How Size + Time Based Rolling Policy Works

Let's walk through this carefully with the instructor's example — **daily rotation**, `maxFileSize=100MB`, `totalSizeCap=2GB`, `maxHistory=30`.

### Scenario 1: Normal Day (file stays under 100MB)

```
Day: 2025-12-19
                                                                    
logs/app.log  ← active file, current day's logs go here
              size = 50MB (under 100MB limit)

Midnight hits → day changes to 2025-12-20

logs/app-2025-12-19.0.log  ← archived (the 50MB file)
logs/app.log               ← fresh active file for 2025-12-20
```

Simple — just like time-based rotation. One file per day.

---

### Scenario 2: High Traffic Day (file exceeds 100MB)

This is where the `%i` index comes into the picture:

```
Day: 2025-12-19, Heavy Traffic

logs/app.log grows...
  10MB... 50MB... 80MB... 100MB ← maxFileSize reached!

Logback immediately:
  Step 1: Close logs/app.log
  Step 2: Archive it as logs/app-2025-12-19.0.log  ← index starts at 0
  Step 3: Create fresh logs/app.log

logs/app.log grows again...
  10MB... 50MB... 100MB ← maxFileSize reached again!

Logback immediately:
  Step 1: Close logs/app.log
  Step 2: Archive it as logs/app-2025-12-19.1.log  ← index increments to 1
  Step 3: Create fresh logs/app.log

logs/app.log grows again...
  10MB... 100MB ← maxFileSize reached again!

  → archived as logs/app-2025-12-19.2.log  ← index = 2
  → fresh logs/app.log created

...and so on for the same day
```

So for one high-traffic day, your logs folder looks like:

```
logs/
 ├── app.log                    ← current active file
 ├── app-2025-12-19.0.log       ← 0-100MB  (first file of the day)
 ├── app-2025-12-19.1.log       ← 100-200MB
 ├── app-2025-12-19.2.log       ← 200-300MB
 └── app-2025-12-19.3.log       ← 300-400MB (currently being archived)
```

> 📌 **This is exactly what `%i` does in the `fileNamePattern`.** It's an auto-incrementing index that differentiates multiple archived files for the same time period (same day in this case).

---

## 🔍 Understanding `totalSizeCap`

Even with `maxFileSize` controlling individual file sizes, you could still end up with too many files eating up all your disk:

```
30 days × multiple 100MB files per day = potentially tens of GBs!
```

`totalSizeCap` puts a hard limit on the **combined size of all log files**:

```
┌──────────────────────────────────────────────────────────────────┐
│                    totalSizeCap = 2GB                            │
│                                                                  │
│  All log files combined CANNOT exceed 2GB.                       │
│                                                                  │
│  If adding a new archived file would push total over 2GB:        │
│  → Logback automatically deletes the OLDEST archived file        │
│  → This keeps total storage always within 2GB                    │
│                                                                  │
│  Example:                                                        │
│  file1 (oldest) = 100MB                                          │
│  file2          = 100MB                                          │
│  ...                                                             │
│  file20         = 100MB  ← total = 2GB, cap reached!             │
│  New file being archived → oldest file (file1) gets deleted      │
│  → total stays within 2GB always                                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Important Warning the Instructor Gives

> *"Even though you have set 21 days (maxHistory), because of totalSizeCap there might not be enough space to store that much data. So you might lose some logs even within the 21-day window."*

```
┌──────────────────────────────────────────────────────────────────┐
│              THREE CONTROLS WORKING TOGETHER                     │
│                                                                  │
│  maxHistory   = 30 days  → "keep at most 30 days of logs"        │
│  maxFileSize  = 100MB    → "each file max 100MB"                 │
│  totalSizeCap = 2GB      → "all files combined max 2GB"          │
│                                                                  │
│  Whichever limit is hit FIRST triggers cleanup:                  │
│                                                                  │
│  → If 30 days worth of files fit within 2GB → great, keep all    │
│  → If 10 days worth of files already = 2GB  → older files        │
│    get deleted even though they're within 30-day window          │
│                                                                  │
│  ✅ So set these values based on:                                 │
│     → Your server's available disk capacity                      │
│     → Your application's traffic & logging volume                │
│     → How many days back you need to query logs                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🖼️ Full Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│              SIZE + TIME BASED ROLLING POLICY                       │
│                                                                     │
│   log.info("info log") called                                       │
│          │                                                          │
│          ▼                                                          │
│   ┌──────────────────────────────┐                                  │
│   │  logs/app.log  (ACTIVE FILE) │ ← current logs always go here    │
│   │  size growing...             │                                  │
│   └──────────────┬───────────────┘                                  │
│                  │                                                  │
│         Two conditions can trigger rotation:                        │
│                  │                                                  │
│     ┌────────────┴─────────────┐                                    │
│     │                          │                                    │
│     ▼                          ▼                                    │
│  TIME changes             SIZE hits maxFileSize                     │
│  (e.g. day changes)       (e.g. 100MB reached)                      │
│     │                          │                                    │
│     └────────────┬─────────────┘                                    │
│                  │                                                  │
│                  ▼                                                  │
│         ROTATION TRIGGERED                                          │
│         Step 1: Close logs/app.log                                  │
│         Step 2: Archive it as                                       │
│                 logs/app-{date}.{i}.log                             │
│                 (%i auto-increments: 0, 1, 2...)                    │
│         Step 3: Create fresh logs/app.log                           │
│                  │                                                  │
│                  ▼                                                  │
│         CHECK LIMITS                                                │
│         → Total files > maxHistory?  → delete oldest                │
│         → Total size  > totalSizeCap? → delete oldest               │
│                  │                                                  │
│                  ▼                                                  │
│         Continue logging into fresh logs/app.log                    │
└─────────────────────────────────────────────────────────────────────┘

RESULT — Folder at any point in time:
logs/
 ├── app.log                    ← active (current)
 ├── app-2025-12-19.0.log       ← today's first 100MB
 ├── app-2025-12-19.1.log       ← today's second 100MB
 ├── app-2025-12-18.0.log       ← yesterday
 ├── app-2025-12-17.0.log       ← 2 days ago
 │   ...
 └── app-2025-11-20.0.log       ← 29 days ago (oldest kept)
     ← anything older OR total > 2GB → auto deleted
```

---

## 💻 Code — Size + Time Based Rolling Policy Setup

### Java Class (PaymentController.java)

```java
@RestController
public class PaymentController {

    // Creates 1 logger with name = "com.concepts.PaymentController"
    Logger log = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping("/payments")
    public String getPayments() {
        log.info("info log");
        return "successfully fetched all payments";
    }
}
```

### logback-spring.xml — Size + Time Based Rolling Policy (Production Ready ✅)

```xml
<configuration>

    <!-- Appender 1: Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!--
        Appender 2: Rolling File Appender
        with Size + Time Based Rolling Policy

        → Same RollingFileAppender class as before
        → Only the rollingPolicy class changes to
          SizeAndTimeBasedRollingPolicy
    -->
    <appender name="ROLLING_FILE_SIZE_TIME_BASED"
              class="ch.qos.logback.core.rolling.RollingFileAppender">

        <!-- Active file: current logs always go here -->
        <file>logs/app.log</file>

        <!--
            Rolling Policy: SizeAndTimeBasedRollingPolicy
            → Already present in Logback framework
            → We just configure the parameters
        -->
        <rollingPolicy
            class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">

            <!--
                fileNamePattern for ARCHIVED files

                %d{yyyy-MM-dd} → daily rotation
                %i             → auto-incrementing index (0, 1, 2...)
                                 needed because multiple files can be
                                 created for the same day when size
                                 limit is hit multiple times

                Result: logs/app-2025-12-19.0.log
                        logs/app-2025-12-19.1.log
                        logs/app-2025-12-19.2.log
                        etc.
            -->
            <fileNamePattern>
                logs/app-%d{yyyy-MM-dd}.%i.log
            </fileNamePattern>

            <!--
                maxFileSize = 100MB
                → One single log file CANNOT exceed 100MB
                → Once 100MB is reached, even mid-day:
                  current file is archived (with .0, .1, .2 index)
                  and a fresh app.log is created immediately
            -->
            <maxFileSize>100MB</maxFileSize>

            <!--
                maxHistory = 30
                → Since rotation is daily: keep last 30 DAYS of logs
                → Archived files older than 30 days are auto-deleted
                → Works together with totalSizeCap (whichever
                  triggers first wins)
            -->
            <maxHistory>30</maxHistory>

            <!--
                totalSizeCap = 2GB
                → ALL log files combined cannot exceed 2GB
                → If adding a new archived file pushes total over 2GB,
                  the OLDEST archived file gets automatically deleted
                → This is your hard disk usage guarantee
                → Set this based on your server's available disk space
                  and your application's logging volume
            -->
            <totalSizeCap>2GB</totalSizeCap>

        </rollingPolicy>

        <!-- Encoder: format of logs written to file -->
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>

    </appender>

    <!--
        Logger for PaymentController
        → Uses BOTH Console and Rolling File appenders
        → additivity=false → won't propagate to ROOT
        → Logs appear on console AND in rolling files
    -->
    <logger name="com.concepts.PaymentController" level="INFO" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ROLLING_FILE_SIZE_TIME_BASED"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 🖼️ Step-by-Step Flow for a High Traffic Day

```
Day: 2025-12-19, Very High Traffic

10:00 AM → logs/app.log = 0MB (fresh start of day)
           logs being written...

10:30 AM → logs/app.log = 100MB ← maxFileSize hit!
           ┌─────────────────────────────────────────┐
           │ ROTATION TRIGGERED (size-based)         │
           │ Archive → logs/app-2025-12-19.0.log     │
           │ Fresh   → logs/app.log (0MB again)      │
           └─────────────────────────────────────────┘

11:00 AM → logs/app.log = 100MB ← maxFileSize hit again!
           ┌─────────────────────────────────────────┐
           │ ROTATION TRIGGERED (size-based)         │
           │ Archive → logs/app-2025-12-19.1.log     │
           │ Fresh   → logs/app.log (0MB again)      │
           └─────────────────────────────────────────┘

...same pattern continues all day...

Midnight → logs/app.log = 60MB ← day changes (time-based)
           ┌─────────────────────────────────────────┐
           │ ROTATION TRIGGERED (time-based)         │
           │ Archive → logs/app-2025-12-19.5.log     │
           │ Fresh   → logs/app.log for 2025-12-20   │
           └─────────────────────────────────────────┘

Final folder for 2025-12-19:
logs/
 ├── app.log                      ← 2025-12-20 active
 ├── app-2025-12-19.0.log         ← 0-100MB
 ├── app-2025-12-19.1.log         ← 100-200MB
 ├── app-2025-12-19.2.log         ← 200-300MB
 ├── app-2025-12-19.3.log         ← 300-400MB
 ├── app-2025-12-19.4.log         ← 400-500MB
 └── app-2025-12-19.5.log         ← 500-560MB (last partial)

CHECK LIMITS:
 → Total size of all files > 2GB? → delete oldest files
 → Any file older than 30 days?   → delete it
```

---

## 📊 All Three Appenders — Final Comparison

```
┌──────────────────┬──────────────┬───────────────────┬──────────────────────┐
│    Property      │   File       │  Time-Based       │  Size+Time Based     │
│                  │   Appender   │  Rolling          │  Rolling             │
│                  │              │  Appender         │  Appender ✅ BEST     │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Persistence      │ ✅ Yes        │ ✅ Yes             │ ✅ Yes                │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ File rotation    │ ❌ No         │ ✅ Time-based      │ ✅ Time + Size        │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Per-file size    │ ❌ No limit   │ ❌ No limit        │ ✅ maxFileSize        │
│ control          │              │                   │                      │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Total disk cap   │ ❌ No         │ ❌ No              │ ✅ totalSizeCap       │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Auto-delete      │ ❌ No         │ ✅ maxHistory      │ ✅ maxHistory         │
│ old logs         │              │                   │   + totalSizeCap     │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Multiple files   │ ❌ No         │ ❌ No              │ ✅ Yes (%i index)     │
│ per time period  │              │                   │                      │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Production ready │ ❌ No         │ ⚠️ Not fully      │ ✅ Yes                │
├──────────────────┼──────────────┼───────────────────┼──────────────────────┤
│ Logback Policy   │ N/A          │ TimeBased         │ SizeAndTimeBased     │
│ class            │              │ RollingPolicy     │ RollingPolicy        │
└──────────────────┴──────────────┴───────────────────┴──────────────────────┘
```

---

## 🎯 Interview Tips from This Section

**Q: What is the difference between Time-Based and Size+Time Based Rolling Policy?**

**A:** Time-Based Policy controls only **when** a new file is created — based on time (daily, hourly, etc.). It does not control how big each file can get. Size+Time Based Policy adds two more controls on top — `maxFileSize` limits how big a single file can grow, and `totalSizeCap` limits how much disk space all log files combined can use. This makes it the production-ready choice.

**Q: What is the role of `%i` in the file name pattern?**

**A:** `%i` is an auto-incrementing index (starting from 0) used in the archived file names. It's needed because with size-based rotation, multiple files can be created within the same time period (same day). For example, if `maxFileSize=100MB` and a day generates 500MB of logs, you'd get `app-2025-12-19.0.log`, `app-2025-12-19.1.log`, up to `app-2025-12-19.4.log`. Without `%i`, these files would overwrite each other.

**Q: What happens when `totalSizeCap` is reached?**

**A:** Logback automatically deletes the oldest archived log file to make room for the new one, keeping total disk usage within the cap at all times.

**Q: How do you decide what values to set for `maxFileSize`, `maxHistory`, and `totalSizeCap`?**

**A:** It depends on three things — your server's available disk capacity, your application's traffic and logging volume, and how many days back you need to be able to query logs. For example, a low-traffic service might be fine with `maxFileSize=50MB`, `maxHistory=21`, `totalSizeCap=1GB`. A high-traffic service might need `maxFileSize=100MB`, `maxHistory=30`, `totalSizeCap=10GB`.

---

Ready for **Step 7**? — Writing your own **Custom Appender** (for DB, Kafka, or any custom destination) — extending `AppenderBase`, overriding `append()`, and plugging it into `logback-spring.xml` — with full diagram + code.

# Step 7 — Writing Your Own Custom Appender (DB / Kafka / Any Custom Destination)

---

## 🤔 When Do You Need a Custom Appender?

All the appenders we've seen so far — Console, File, Rolling File — are **pre-built** and already available inside the Logback framework. You just reference their class and configure them.

But what if you want logs to go to a **custom destination** like:
- A **Database** (insert each log as a row in a table)
- **Kafka** (publish each log as a message to a topic)
- Any other custom place

For these cases, **no pre-existing appender exists** that knows your specific DB schema, your Kafka topic name, your connection details, etc. You have to **write your own appender**.

> *"In production, events are sometimes published into Kafka. So there we might have to write a custom appender like a Kafka appender, and inside that we just provide the implementation."*

---

## 💡 How Does Logback Know Your Class is an Appender?

This is the key question. Logback's entire logging pipeline works on **interfaces and abstract classes**. For Logback to treat your class as a valid appender and call it at the right time, your class **must extend** a specific abstract class:

```
AppenderBase<ILoggingEvent>
```

Let's understand both parts:

```
┌──────────────────────────────────────────────────────────────────┐
│                  AppenderBase<ILoggingEvent>                     │
│                                                                  │
│  AppenderBase                                                    │
│  → Abstract class provided by Logback framework                  │
│  → By extending this, Logback recognizes your class              │
│    as a valid appender                                           │
│  → It handles all the boilerplate (thread safety,                │
│    started/stopped state management, etc.)                       │
│  → You only need to implement ONE method: append()               │
│                                                                  │
│  ILoggingEvent                                                   │
│  → Logback's internal representation of ONE log statement        │
│  → When you write log.info("info log"), Logback internally       │
│    converts that into an ILoggingEvent object                    │
│  → This object carries all the info about that log:              │
│    → the message text                                            │
│    → the logger name (class name)                                │
│    → the timestamp                                               │
│    → the log level                                               │
│    → thread name, MDC data, etc.                                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🖼️ Full Architecture — Custom Appender Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        YOUR JAVA CODE                               │
│                                                                     │
│   log.info("payment processed for user 123")                        │
│          │                                                          │
│          ▼                                                          │
│   Logback converts this into an ILoggingEvent object:               │
│   ┌─────────────────────────────────────────────┐                   │
│   │  ILoggingEvent                              │                   │
│   │  → message   = "payment processed for       │                   │
│   │                 user 123"                   │                   │
│   │  → logger    = "com.concepts.               │                   │
│   │                  PaymentController"         │                   │
│   │  → level     = INFO                         │                   │
│   │  → timestamp = 1734567890000                │                   │
│   └─────────────────────────────────────────────┘                   │
│          │                                                          │
│          ▼                                                          │
│   Is log accepted (level check)? → YES                              │
│          │                                                          │
│          ▼                                                          │
│   Your Custom Appender's append(event) method is called             │
│   (by Logback framework — you never call this yourself)             │
│          │                                                          │
│          ▼                                                          │
│   ┌──────────────────────────────────────────────────────┐          │
│   │  YOUR append() METHOD                                │          │
│   │                                                      │          │
│   │  String message   = event.getFormattedMessage()      │          │
│   │  String logger    = event.getLoggerName()            │          │
│   │  long  timestamp  = event.getTimeStamp()             │          │
│   │                                                      │          │
│   │  → Do whatever you want with this data:              │          │
│   │    Insert into DB, publish to Kafka, send to         │          │
│   │    Slack, write to S3, call an API, etc.             │          │
│   └──────────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 💻 Code — Writing a Custom DB Appender

### Step 1: Create the Custom Appender Class

```java
/*
 * Custom Appender for saving logs to a Database.
 *
 * Rules to follow:
 * 1. MUST extend AppenderBase<ILoggingEvent>
 *    → This is how Logback identifies your class as a valid appender
 *    → AppenderBase is a generic abstract class
 *    → ILoggingEvent is Logback's internal representation of
 *      one single log statement (one log.info / log.error etc.)
 *
 * 2. MUST override the append(ILoggingEvent event) method
 *    → Logback framework calls this method automatically
 *      whenever an accepted log needs to be processed
 *    → You never call this method yourself
 *
 * 3. No encoder needed here
 *    → In Console/File appenders, encoder was needed to format
 *      the log before writing to that destination
 *    → Here, YOU control everything — format, destination, logic
 *    → You extract what you need from ILoggingEvent and handle it
 */
public class DbAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {

        // Extract log data from the ILoggingEvent object
        // This is everything Logback captured about this one log statement

        // The actual log message text
        // e.g. "payment processed for user 123"
        String message = event.getFormattedMessage();

        // Full logger name (package + class name)
        // e.g. "com.concepts.PaymentController"
        String loggerName = event.getLoggerName();

        // Timestamp in milliseconds (epoch time)
        // when this log statement was executed
        long timestamp = event.getTimeStamp();

        // -------------------------------------------------------
        // Custom destination logic goes here
        // In real production code, this is where you would:
        // → Create a DB connection (or use an injected repository)
        // → Build your SQL insert statement
        // → Execute the insert
        // -------------------------------------------------------

        // For demo purposes, just printing to show it works
        // In real code, replace this with actual DB insert logic
        System.out.println(
            "Saving log to DB: "
            + timestamp + " - "
            + loggerName + " - "
            + message
        );

        /*
         * Real production DB insert would look something like:
         *
         * LogEntity logEntity = new LogEntity();
         * logEntity.setMessage(message);
         * logEntity.setLoggerName(loggerName);
         * logEntity.setTimestamp(new Date(timestamp));
         * logRepository.save(logEntity);
         */
    }
}
```

---

### Step 2: Register It in `logback-spring.xml`

```xml
<configuration>

    <!-- Console Appender (still useful alongside custom appender) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d %-5level %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!--
        Custom DB Appender

        name  → any name you want, referenced by logger below
        class → FULL package path of your custom appender class
                Logback will instantiate this class and call
                its append() method for every accepted log

        ⚠️ NO <encoder> needed here!
           Because in your custom append() method, YOU decide:
           → the format (how to structure the data)
           → the destination (which DB table, which Kafka topic)
           → the logic (connection handling, error handling, etc.)
    -->
    <appender name="DB_APPENDER"
              class="com.concepts.customappender.DbAppender">
        <!-- no encoder needed here, all handled inside the Java class -->
    </appender>

    <!--
        Logger for PaymentController
        → Uses DB_APPENDER as its appender
        → additivity=false → won't propagate to ROOT
        → Every accepted log goes to DB via our custom append() method
    -->
    <logger name="com.concepts.PaymentController"
            level="INFO"
            additivity="false">
        <appender-ref ref="DB_APPENDER"/>
    </logger>

    <!-- ROOT Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

---

## 🖼️ Custom Appender vs Pre-built Appender — Side by Side

```
┌────────────────────────────────────────────────────────────────────┐
│                PRE-BUILT APPENDER (e.g. Console, File)             │
│                                                                    │
│  logback-spring.xml:                                               │
│  <appender name="CONSOLE"                                          │
│    class="ch.qos.logback.core.ConsoleAppender">  ← Logback's class │
│    <encoder>                                                       │
│      <pattern>%d %-5level %logger - %msg%n</pattern>               │
│    </encoder>                                     ← format needed  │
│  </appender>                                                       │
│                                                                    │
│  → Class already written by Logback                                │
│  → You just configure it (file name, pattern, etc.)                │
│  → Encoder required to define log format                           │
└────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────┐
│                    CUSTOM APPENDER (e.g. DB, Kafka)                │
│                                                                    │
│  logback-spring.xml:                                               │
│  <appender name="DB_APPENDER"                                      │
│    class="com.concepts.customappender.DbAppender"> ← YOUR class    │
│    <!-- no encoder needed -->                     ← no encoder     │
│  </appender>                                                       │
│                                                                    │
│  DbAppender.java:                                                  │
│  public class DbAppender                                           │
│      extends AppenderBase<ILoggingEvent> {  ← MUST extend this     │
│    @Override                                                       │
│    protected void append(ILoggingEvent event) {                    │
│        // YOUR logic here                   ← YOU handle format    │
│        // extract data, insert into DB      ← YOU handle dest.     │
│    }                                                               │
│  }                                                                 │
│                                                                    │
│  → Class written by YOU                                            │
│  → You control format AND destination                              │
│  → No encoder needed                                               │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 What Can You Extract from `ILoggingEvent`?

```
┌──────────────────────────────────────────────────────────────────┐
│              ILoggingEvent — Available Methods                   │
│                                                                  │
│  event.getFormattedMessage()                                     │
│  → The actual log message text                                   │
│  → e.g. "payment processed for user 123"                         │
│                                                                  │
│  event.getLoggerName()                                           │
│  → Full logger name (package + class)                            │
│  → e.g. "com.concepts.PaymentController"                         │
│                                                                  │
│  event.getTimeStamp()                                            │
│  → Timestamp in milliseconds when log was created                │
│  → e.g. 1734567890000                                            │
│                                                                  │
│  event.getLevel()                                                │
│  → Log level of this statement                                   │
│  → e.g. Level.INFO, Level.ERROR, Level.WARN                      │
│                                                                  │
│  event.getThreadName()                                           │
│  → Name of the thread that created this log                      │
│  → e.g. "http-nio-8080-exec-1"                                   │
│                                                                  │
│  event.getMDCPropertyMap()                                       │
│  → MDC (Mapped Diagnostic Context) key-value pairs               │
│  → Used in distributed tracing (e.g. traceId, spanId)            │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📊 Full Comparison — All Appender Types

```
┌──────────────────┬────────────┬──────────────┬──────────────┬──────────────┐
│    Property      │  Console   │  File        │  Rolling     │  Custom      │
│                  │  Appender  │  Appender    │  File        │  Appender    │
│                  │            │              │  Appender    │  (DB/Kafka)  │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Destination      │ STDOUT     │ File on disk │ File on disk │ Anything you │
│                  │            │              │  (rotated)   │ want         │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Persistence      │ ❌ No       │ ✅ Yes        │ ✅ Yes        │ Depends on   │
│                  │            │              │              │ your logic   │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Size control     │ N/A        │ ❌ No         │ ✅ Yes        │ You decide   │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Encoder needed   │ ✅ Yes      │ ✅ Yes        │ ✅ Yes        │ ❌ No         │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Class provided   │ ✅ By       │ ✅ By         │ ✅ By         │ ❌ You write  │
│ by Logback?      │ Logback    │ Logback      │ Logback      │ it yourself  │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Must extend      │ N/A        │ N/A          │ N/A          │ ✅ MUST       │
│ AppenderBase?    │            │              │              │ extend it    │
├──────────────────┼────────────┼──────────────┼──────────────┼──────────────┤
│ Production ready │ ✅ Yes      │ ❌ No         │ ✅ Yes        │ ✅ Yes        │
│                  │ (with      │              │ (Size+Time)  │ (if impl.    │
│                  │ platform)  │              │              │ is correct)  │
└──────────────────┴────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 🎯 Interview Tips from This Section

**Q: When would you write a custom appender in production?**

**A:** When you need logs to go to a destination that Logback doesn't have a pre-built appender for — most commonly a **Database** or **Kafka**. For example, in a microservices system, you might want to publish log events as Kafka messages so that a centralized log aggregation service can consume them. You'd write a KafkaAppender by extending `AppenderBase<ILoggingEvent>` and implementing the `append()` method with Kafka producer logic.

**Q: What is `ILoggingEvent` and what does it contain?**

**A:** `ILoggingEvent` is Logback's internal representation of a single log statement. When you write `log.info("message")`, Logback converts it into an `ILoggingEvent` object that carries the message text, logger name, timestamp, log level, thread name, and MDC data. Your custom appender receives this object in the `append()` method and can extract any of these fields.

**Q: Why is no encoder needed in a custom appender?**

**A:** Because in a pre-built appender (Console, File), the encoder is responsible for formatting the raw log data into a string before writing it to the output. In a custom appender, **you** control everything inside the `append()` method — you extract exactly the fields you need from `ILoggingEvent` and decide how to format and store them yourself. There's no separate formatting step needed.

**Q: What is the one rule you absolutely must follow when writing a custom appender?**

**A:** Your class **must extend `AppenderBase<ILoggingEvent>`** and **override the `append()` method**. This is how Logback's framework identifies your class as a valid appender and knows to call your `append()` method for every accepted log event. Without extending `AppenderBase`, Logback cannot treat your class as an appender.

---

## ✅ Full Series Summary — Distributed Logging Part 2

```
┌──────────────────────────────────────────────────────────────────┐
│           EVERYTHING COVERED IN PART 2 — APPENDERS               │
│                                                                  │
│  1. What is an Appender                                          │
│     → Decides WHERE logs go                                      │
│     → Without any appender, logs propagate to ROOT's             │
│       default Console Appender                                   │
│                                                                  │
│  2. logback-spring.xml                                           │
│     → 3 blocks: Appenders, Loggers, Root                         │
│     → Encoder: decides FORMAT (not WHERE)                        │
│     → Additivity: appender propagation only (true by default)    │
│     → Level: always inherited from parent if not defined         │
│                                                                  │
│  3. Console Appender                                             │
│     → Writes to STDOUT                                           │
│     → Platform reads STDOUT (Terminal/Docker/Azure)              │
│     → Fast, no persistence, no disk risk                         │
│                                                                  │
│  4. File Appender                                                │
│     → Writes to a file on disk                                   │
│     → append=true/false controls restart behavior                │
│     → NOT production ready (no size limit)                       │
│                                                                  │
│  5. Rolling File Appender — Time-Based Policy                    │
│     → Creates new file based on time (daily/hourly/etc.)         │
│     → maxHistory controls how many files to keep                 │
│     → Still NOT fully production ready (no size limit)           │
│                                                                  │
│  6. Rolling File Appender — Size+Time Based Policy ✅ BEST        │
│     → maxFileSize: per-file size limit                           │
│     → totalSizeCap: total disk usage limit                       │
│     → %i index for multiple files per time period                │
│     → Production ready ✅                                         │
│                                                                  │
│  7. Custom Appender (DB / Kafka)                                 │
│     → Extend AppenderBase<ILoggingEvent>                         │
│     → Override append() method                                   │
│     → Extract data from ILoggingEvent                            │
│     → No encoder needed                                          │
└──────────────────────────────────────────────────────────────────┘
```

> 📌 **What's coming in Part 3** (as the instructor mentions): **Async logging** — because everything we did here is synchronous. Writing to a DB or file on every log call adds latency to your request. Async appenders solve this. The instructor also mentions covering **log formats** in the next part.