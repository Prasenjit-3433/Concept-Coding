# Part 1 — Sync vs Async Logging

## The Problem First

Till now, everything we've done in logging (writing to file, DB, Kafka, console) — all of it happens **synchronously**. That means your **request thread** (the thread handling the user's HTTP request) does everything — it writes the log, waits for it to finish, and only then moves on.

Let's visualize what's happening:

```
Incoming Request
      │
      ▼
 Request Thread
      │
      ├──► Business Logic
      │
      ├──► log.info("Payment done")  ◄─── Thread is STUCK here
      │         │
      │         ▼
      │    Write to File / DB / Kafka   ◄─── This takes time (I/O)
      │         │
      │         ▼
      │    Done writing ✓
      │
      └──► Continue with next task
```

This looks fine when traffic is low. But think about **heavy traffic**:

- Hundreds of requests hitting your service per second
- Each request writing multiple log statements
- Each log statement doing an I/O operation (writing to file, or worse — writing to DB or Kafka)
- Your request thread is **stuck waiting** for each log write to finish before it can move on

This directly **increases your response latency** and **kills your application's performance**. The logging system, which is supposed to be a side utility, starts becoming a bottleneck.

---

## The Solution — Async Logging

The core idea is simple: **don't let the request thread do the logging work.** Hand it off to a background thread and let the request thread move on immediately.

Now there are **two types** of async logging. The instructor is very clear about the difference:

---

### Type 1 — AsyncAppender (Logback feature)

Only the **formatting + writing to appender** part becomes async. Everything before that (level check, log event creation) still happens on the request thread synchronously.

```
Incoming Request
      │
      ▼
 Request Thread
      │
      ├──► log.info("Payment done")
      │
      ├──► Level Check          ──► SYNC (still on request thread)
      │
      ├──► Log Event Creation   ──► SYNC (still on request thread)
      │
      ├──► Put event in QUEUE   ──► Request thread is FREE now ✓
      │
      │                              │
      │                    Background Worker Thread
      │                              │
      │                              ▼
      │                        Formatting
      │                              │
      │                              ▼
      │                    Write via Appender (File/DB/Kafka)
      │
      └──► Continue immediately
```

- This is a **Logback** feature
- Good for **medium level traffic**
- Logback only supports AsyncAppender, NOT AsyncLogger

---

### Type 2 — AsyncLogger (Log4j2 feature)

Here, **everything** after `log.info()` happens asynchronously — level check, event creation, formatting, writing — all of it.

```
Incoming Request
      │
      ▼
 Request Thread
      │
      ├──► log.info("Payment done")
      │
      └──► DONE. Thread is free immediately ✓

                    │
          Background Thread picks up EVERYTHING
                    │
                    ▼
              Level Check
                    │
                    ▼
          Log Event Creation
                    │
                    ▼
             Formatting
                    │
                    ▼
         Write via Appender
```

- This is a **Log4j2** feature
- Good for **very high traffic**
- Log4j2 supports **both** AsyncAppender and AsyncLogger
- Logback does **NOT** support AsyncLogger

---

## Quick Comparison Table

| | AsyncAppender | AsyncLogger |
|---|---|---|
| Framework | Logback | Log4j2 |
| What's async | Only formatting + write | Entire logging pipeline |
| Traffic suitability | Medium | High |
| Level check | Sync | Async |
| Log event creation | Sync | Async |

---

## Key Takeaway / Interview Tip

> If someone asks: *"What is the difference between AsyncAppender and AsyncLogger?"*

The answer is: In AsyncAppender (Logback), only the appender work — formatting and writing — is done asynchronously. The request thread still does level checking and log event creation. In AsyncLogger (Log4j2), the **entire** logging pipeline is async, so the request thread is freed up immediately after calling `log.info()`. AsyncLogger is better for very high traffic scenarios, while AsyncAppender works well for medium traffic.

> Also important: **Logback does not support AsyncLogger.** If you're using Logback (which Spring Boot uses by default), you only get AsyncAppender.

---

Ready for **Part 2 — AsyncAppender in Depth** (internal architecture + all configuration options + full code)? Let me know!

# Part 2 — AsyncAppender in Depth

## How AsyncAppender Works Internally

Before jumping into config and code, let's build a very clear mental model of what's actually happening inside AsyncAppender.

```
Request Thread                    In-Memory Queue                Worker Thread
      │                                                                │
      │   log.info("Payment done")                                    │
      │          │                                                     │
      │          ▼                                                     │
      │     Level Check (SYNC)                                        │
      │          │                                                     │
      │          ▼                                                     │
      │   Log Event Creation (SYNC)                                   │
      │          │                                                     │
      │          ▼                                                     │
      │   ┌─────────────────────────────┐                            │
      │   │  Put Log Event into Queue   │                            │
      │   └─────────────────────────────┘                            │
      │          │                          ┌──────────────────┐     │
      │          │                          │   Log Event 1    │     │
      │   Request Thread                    │   Log Event 2    │◄────┤
      │   is FREE now ✓                     │   Log Event 3    │     │
      │                                     │      ...         │     │
      │                                     └──────────────────┘     │
      │                                              │                │
      │                                    Worker Thread picks up     │
      │                                    log event from Queue       │
      │                                              │                │
      │                                              ▼                │
      │                                        Formatting             │
      │                                              │                │
      │                                              ▼                │
      │                                     Write via Appender        │
      │                                     (File / DB / Kafka)       │
```

Two very important things the instructor highlights here:

1. **AsyncAppender is just a wrapper.** It doesn't do the actual writing itself. The real appender (FileAppender, for example) still does all the actual work. AsyncAppender just sits in front of it and provides the Queue + Worker Thread mechanism.

2. **AsyncAppender provides two things:** a Queue (to hold log events) and a Worker Thread (to pick events from the queue and pass them to the actual appender).

---

## All Configuration Options — One by One

### 1. queueSize
```
┌─────────────────────────────────────────┐
│           In-Memory Queue               │
│  [  event  |  event  |  event  | ....] │
│                                         │
│  Default size = 256 slots               │
│  Can be changed, e.g. queueSize = 1000  │
└─────────────────────────────────────────┘
```
- This is an **in-memory** queue
- Default is **256**
- You can increase it based on your traffic needs

---

### 2. discardingThreshold

This is a smart feature. The queue doesn't just blindly accept all log events when it's almost full. It starts **discarding low-priority events** to save space for critical ones.

```
Queue capacity = 100 slots

discardingThreshold = 10 means:

  0%────────────────────────90%────100%
  │                          │       │
  │   Accept ALL log levels  │ Only  │
  │   (DEBUG, TRACE, INFO,   │ ERROR │
  │    WARN, ERROR)          │ WARN  │
  │                          │ INFO  │
  │                          │       │
  └──────────────────────────┘       │
         Normal zone          Last 10%
                             Critical
                               only
                           (DEBUG/TRACE
                            discarded)
```

So if `discardingThreshold = 10`:
- When queue is 0–90% full → accept everything
- When queue is 90–100% full → only keep INFO, WARN, ERROR. Drop DEBUG and TRACE.

If `discardingThreshold = 40`:
- When queue is 0–60% full → accept everything
- When queue is 60–100% full → only keep INFO, WARN, ERROR. Drop DEBUG and TRACE.

If `discardingThreshold = 0`:
- Never discard anything, always accept all log levels regardless of queue fullness.

---

### 3. neverBlock

This config decides what happens when the **queue is completely full** and a new log event tries to enter.

```
Queue is FULL
      │
      ▼
New log event arrives...

neverBlock = false (DEFAULT):
┌─────────────────────────────────────┐
│  Request thread WAITS               │
│  until space is available in queue  │
│  (thread is blocked)                │
└─────────────────────────────────────┘

neverBlock = true:
┌─────────────────────────────────────┐
│  Log event is DROPPED               │
│  Request thread moves on immediately│
│  (no waiting, no blocking)          │
└─────────────────────────────────────┘
```

- Default is `false` — thread blocks and waits
- Set to `true` if you absolutely cannot afford any latency, even at the cost of losing some logs

---

### 4. Worker Thread

- By default, there is only **1 worker thread**
- There is no direct config to increase this in Logback's AsyncAppender
- The single worker thread continuously picks events from the queue and passes them to the actual appender

---

## Full Code Implementation

Here's how the whole thing is wired up. Read it top to bottom — it tells a complete story:

```xml
<!-- ─────────────────────────────────────── -->
<!-- STEP 1: Define the actual File Appender -->
<!-- This is the real appender doing the work -->
<!-- ─────────────────────────────────────── -->
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>logs/app.log</file>
    <append>false</append>
    <encoder>
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>
</appender>

<!-- ─────────────────────────────────────────────── -->
<!-- STEP 2: Wrap it with AsyncAppender              -->
<!-- AsyncAppender is just a wrapper over FILE above -->
<!-- ─────────────────────────────────────────────── -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">

    <!-- Queue can hold 1000 log events in memory -->
    <queueSize>1000</queueSize>

    <!-- When only 10% space left, drop DEBUG & TRACE -->
    <discardingThreshold>10</discardingThreshold>

    <!-- If queue is full:          -->
    <!-- false (default) = thread waits  -->
    <!-- true = drop log, thread moves on -->
    <neverBlock>false</neverBlock>

    <!-- Point to the actual appender that will do the writing -->
    <appender-ref ref="FILE"/>

</appender>

<!-- ─────────────────────────────────────────────── -->
<!-- STEP 3: Attach AsyncAppender to your Logger     -->
<!-- ─────────────────────────────────────────────── -->
<logger name="com.concepts.PaymentController"
        level="INFO"
        additivity="false">
    <appender-ref ref="ASYNC_FILE"/>
</logger>
```

---

## The Full Picture in One Diagram

```
Your Code:
log.info("Payment done")
        │
        ▼
  [Request Thread]
        │
        ├─► Level Check ──────────────────────────── SYNC
        │
        ├─► Log Event Created ────────────────────── SYNC
        │
        ▼
  ┌─────────────────────────────────┐
  │         ASYNC_FILE              │  ← AsyncAppender (wrapper)
  │                                 │
  │   queueSize      = 1000         │
  │   discardThresh  = 10           │
  │   neverBlock     = false        │
  │                                 │
  │   ┌─────────────────────────┐   │
  │   │       Queue             │   │
  │   │ [evt][evt][evt]...      │   │
  │   └──────────┬──────────────┘   │
  │              │                  │
  │   ┌──────────▼──────────────┐   │
  │   │    Worker Thread (x1)   │   │
  │   └──────────┬──────────────┘   │
  └──────────────┼──────────────────┘
                 │
                 ▼
          ┌─────────────┐
          │    FILE      │  ← Actual FileAppender
          │  Appender    │    does the real writing
          └─────────────┘
                 │
                 ▼
          logs/app.log ✓

[Request Thread is already FREE
 by the time Worker Thread writes]
```

---

## Key Takeaways / Interview Tips

> **"Is AsyncAppender truly async end to end?"**

No. Only the formatting and writing part is async. Level checking and log event creation still happen on the request thread synchronously.

> **"What happens if the queue fills up?"**

Depends on `neverBlock`. If false (default), request thread waits. If true, the log event is dropped and thread moves on.

> **"What is discardingThreshold?"**

It's a safety valve. When the queue is filling up (say last 10% space left), it starts rejecting low-priority logs (DEBUG, TRACE) to preserve space for important ones (INFO, WARN, ERROR).

> **"How many worker threads does AsyncAppender use?"**

Only 1, by default. And Logback doesn't give you a config to increase it in AsyncAppender.

---

Ready for **Part 3 — App Shutdown Behavior + Filter Strategy** (what happens to queued logs when app shuts down, and how to mix sync + async smartly using filters + code)? Let me know!

# Part 3 — App Shutdown Behavior + Filter Strategy (Sync for ERROR, Async for the rest)

## The Problem with Async on App Shutdown

Remember — the Queue in AsyncAppender is **in-memory**. It lives inside the JVM. So what happens when your application shuts down?

```
Normal Running State:
┌─────────────────────────────────────────┐
│              JVM Running                │
│                                         │
│   ┌─────────────────────────────────┐   │
│   │         In-Memory Queue         │   │
│   │  [evt1] [evt2] [evt3] [evt4]   │   │
│   └─────────────────────────────────┘   │
│         Worker Thread processing...     │
└─────────────────────────────────────────┘

App Shutdown Triggered:
┌─────────────────────────────────────────┐
│         JVM Shutdown Initiated          │
│                                         │
│   Step 1: Stop accepting new log events │
│           ❌ No new events in queue     │
│                                         │
│   Step 2: Try to flush remaining events │
│           ⚠️  Best effort, no guarantee │
│                                         │
│   Step 3: JVM shuts down               │
│           💀 Queue is discarded        │
│           💀 Unflushed events are LOST │
└─────────────────────────────────────────┘
```

So when shutdown happens, AsyncAppender:
- **Stops** accepting any new log events (even if there's space in the queue)
- **Tries** to flush remaining events as fast as possible
- But **does NOT guarantee** that all events will be written — because as soon as JVM goes down, the in-memory queue is gone

This means **you can lose logs on shutdown** when using AsyncAppender.

---

## Why This Matters — ERROR Logs Are Critical

Think about this real scenario:

```
Customer reports: "Payment failed!"
        │
        ▼
You go to check logs...
        │
        ▼
ERROR log that captured the exception
is MISSING — it was sitting in the
queue when the app shut down 😱
        │
        ▼
You have NO idea what went wrong.
Debugging becomes a nightmare.
```

ERROR logs are the most valuable logs you have when something goes wrong in production. Losing them is unacceptable.

---

## The Instructor's Recommendation

The instructor gives a very clear and practical recommendation:

```
Log Level          Logging Strategy
─────────────────────────────────────
DEBUG    ──────►  ASYNC  ✓ (okay to lose occasionally)
TRACE    ──────►  ASYNC  ✓ (okay to lose occasionally)
INFO     ──────►  ASYNC  ✓ (okay to lose occasionally)
WARN     ──────►  ASYNC  ✓ (okay to lose occasionally)
─────────────────────────────────────
ERROR    ──────►  SYNC   ✓ (MUST NOT lose — go sync)
─────────────────────────────────────
```

The idea is simple: for non-critical levels, async is fine. But for ERROR, we go sync to guarantee the log is written before the thread moves on — no queue, no worker thread, no risk of losing it.

---

## How to Implement This — Using Filters

Now the question is: how do we route ERROR to sync and everything else to async? The answer is **Filters inside Appenders**.

Logback gives us two popular filters:

```
┌─────────────────────────────────────────────────────┐
│                  Logback Filters                     │
│                                                      │
│  1. LevelFilter                                      │
│     → Works on ONE specific level                    │
│     → You say: "if level is X, then ACCEPT or DENY" │
│     → onMatch: ACCEPT / DENY                         │
│     → onMismatch: ACCEPT / DENY / NEUTRAL            │
│       (NEUTRAL = let the next filter decide)         │
│                                                      │
│  2. ThresholdFilter                                  │
│     → Works on a level AND ABOVE                     │
│     → Everything below that level is rejected        │
│     → e.g. level=ERROR means only ERROR gets through │
└─────────────────────────────────────────────────────┘
```

---

## The Full Architecture We're Building

```
log.info("payment done")   OR   log.error("payment failed", e)
              │                           │
              └───────────┬───────────────┘
                          │
                          ▼
                    Logger (INFO level)
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
        ASYNC Appender          FILE_CRITICAL
        (wrapper)                 Appender
              │                (ThresholdFilter:
              │                 only ERROR passes)
        LevelFilter                   │
        (DENY if ERROR)               │
              │                       │
              ▼                       ▼
       FILE_NOT_CRITICAL          critical_app.log
          Appender                  (SYNC write)
       (Queue → Worker
        Thread → File)
              │
              ▼
       non_critical_app.log
         (ASYNC write)
```

So what happens for each log level:

```
log.info(...)
  → Goes to ASYNC appender
  → LevelFilter: is it ERROR? No → NEUTRAL → passes through
  → Gets queued → Worker thread writes to non_critical_app.log ✓
  → Goes to FILE_CRITICAL
  → ThresholdFilter: is it ERROR or above? No → REJECTED ✗

log.error(...)
  → Goes to ASYNC appender
  → LevelFilter: is it ERROR? Yes → DENY → not queued ✗
  → Goes to FILE_CRITICAL
  → ThresholdFilter: is it ERROR or above? Yes → ACCEPTED ✓
  → Written SYNCHRONOUSLY to critical_app.log ✓
```

---

## Full Code Implementation

```xml
<!-- ──────────────────────────────────────────── -->
<!-- STEP 1: Non-critical File Appender           -->
<!-- INFO, WARN, DEBUG, TRACE go here             -->
<!-- This is the ACTUAL appender (not the wrapper)-->
<!-- ──────────────────────────────────────────── -->
<appender name="FILE_NOT_CRITICAL"
          class="ch.qos.logback.core.FileAppender">
    <file>logs/non_critical_app.log</file>
    <encoder>
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>
</appender>

<!-- ──────────────────────────────────────────── -->
<!-- STEP 2: AsyncAppender wrapping               -->
<!--         FILE_NOT_CRITICAL                    -->
<!-- Filter DENIES ERROR before it enters queue   -->
<!-- ──────────────────────────────────────────── -->
<appender name="ASYNC"
          class="ch.qos.logback.classic.AsyncAppender">

    <!-- LevelFilter: explicitly deny ERROR level -->
    <!-- ERROR should go to sync appender instead -->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <level>ERROR</level>
        <onMatch>DENY</onMatch>      <!-- ERROR? Reject it here -->
        <onMismatch>NEUTRAL</onMismatch> <!-- Others? Let next filter decide -->
    </filter>

    <!-- filters run BEFORE log event is pushed to queue -->
    <appender-ref ref="FILE_NOT_CRITICAL"/>

</appender>

<!-- ──────────────────────────────────────────── -->
<!-- STEP 3: Critical File Appender               -->
<!-- Only ERROR reaches here (sync write)         -->
<!-- ThresholdFilter blocks everything below ERROR-->
<!-- ──────────────────────────────────────────── -->
<appender name="FILE_CRITICAL"
          class="ch.qos.logback.core.FileAppender">
    <file>logs/critical_app.log</file>
    <encoder>
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>

    <!-- ThresholdFilter: only ERROR and above pass through -->
    <!-- INFO, WARN, DEBUG, TRACE are all rejected here     -->
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>ERROR</level>
    </filter>

</appender>

<!-- ──────────────────────────────────────────── -->
<!-- STEP 4: Logger wired to BOTH appenders       -->
<!-- ASYNC handles non-critical (async)           -->
<!-- FILE_CRITICAL handles ERROR (sync)           -->
<!-- ──────────────────────────────────────────── -->
<logger name="com.concepts.PaymentController"
        level="INFO"
        additivity="false">
    <appender-ref ref="ASYNC"/>           <!-- async for INFO/WARN/DEBUG/TRACE -->
    <appender-ref ref="FILE_CRITICAL"/>   <!-- sync for ERROR -->
</logger>
```

---

## Tracing a log.error() Call Step by Step

```
log.error("Payment failed for user {}", userId, exception)
        │
        ▼
Logger level check: ERROR >= INFO ✓ passes
        │
        ├──────────────────────────────────────────┐
        │                                          │
        ▼                                          ▼
  ASYNC appender                           FILE_CRITICAL appender
        │                                          │
        ▼                                          ▼
  LevelFilter runs:                      ThresholdFilter runs:
  Is it ERROR? YES                       Is it ERROR or above? YES
  onMatch = DENY                         → ACCEPTED ✓
  → Event REJECTED ✗                            │
  Not added to queue                            ▼
                                     Written SYNCHRONOUSLY
                                     to critical_app.log ✓
                                     (guaranteed, no queue risk)
```

---

## Tracing a log.info() Call Step by Step

```
log.info("Payment initiated for user {}", userId)
        │
        ▼
Logger level check: INFO >= INFO ✓ passes
        │
        ├──────────────────────────────────────────┐
        │                                          │
        ▼                                          ▼
  ASYNC appender                           FILE_CRITICAL appender
        │                                          │
        ▼                                          ▼
  LevelFilter runs:                      ThresholdFilter runs:
  Is it ERROR? NO                        Is it ERROR or above? NO
  onMismatch = NEUTRAL                   → REJECTED ✗
  → passes through ✓                    Not written here
        │
        ▼
  Added to in-memory Queue
        │
        ▼
  Worker Thread picks it up
        │
        ▼
  Written ASYNCHRONOUSLY
  to non_critical_app.log ✓
```

---

## Key Takeaways / Interview Tips

> **"What happens to async logs when the application shuts down?"**

When JVM shutdown is triggered, AsyncAppender stops accepting new log events and tries to flush the remaining ones in the queue. But it does NOT guarantee all events will be written — anything left in the queue when JVM finally goes down is lost.

> **"How do you handle this risk in production?"**

Use a mixed strategy — async for DEBUG, TRACE, INFO, WARN, and sync for ERROR. This way, critical error logs are always written immediately and guaranteed to persist, while non-critical logs use async for performance.

> **"What is the difference between LevelFilter and ThresholdFilter in Logback?"**

LevelFilter works on one exact level — you decide what to do on match and on mismatch (ACCEPT / DENY / NEUTRAL). ThresholdFilter works on a level and everything above it — anything below that level is automatically rejected.

> **"What does NEUTRAL mean in LevelFilter?"**

It means "I don't know, let the next filter in the chain decide." Filters in Logback run top to bottom, and NEUTRAL passes the decision to the next one. If no filter accepts or denies, the event is accepted by default.

---

Ready for **Part 4 — Log Format / Pattern + Placeholder Best Practices**? Let me know!

# Part 4 — Log Format / Pattern + Placeholder Best Practices

## Part A — Log Format / Log Pattern

### How Does a Log Statement Get Formatted?

When you write `log.info("payment done")`, the output you see on console or in a file isn't just the raw message. It has date, level, logger name, thread name, etc. attached to it. **Who decides this format?** The **encoder** inside the appender, using a **pattern**.

```
Your Code:
log.info("payment done")
        │
        ▼
  Log Event Created
  (holds all raw data:
   level, thread, logger,
   timestamp, message...)
        │
        ▼
  Encoder with Pattern
  ("%d %-5level %logger - %msg%n")
        │
        ▼
  Formatted Output:
  2025-12-17 12:04:16,181 INFO  com.concepts.PaymentController - payment done
```

---

### The Pattern and PatternLayout.java

The pattern uses **placeholders** — short codes that get replaced with actual values from the log event. For example:

```
Pattern:   %d      %-5level   %logger           -    %msg       %n
           │       │          │                      │          │
           ▼       ▼          ▼                      ▼          ▼
Output:  date    level    logger name          message    new line

2025-12-17 12:04:16  INFO   com.concepts.PaymentController - payment done
```

Now, how do you know what `%d` means or what `%level` means? The instructor points to a Logback framework class called **PatternLayout.java**. This class has a map of all available placeholders and their implementations.

```
PatternLayout.java (Logback Framework)
┌──────────────────────────────────────────────────────┐
│                                                      │
│  DEFAULT_CONVERTER_SUPPLIER_MAP.put(                 │
│      "d",       DateConverter::new       );          │
│  DEFAULT_CONVERTER_SUPPLIER_MAP.put(                 │
│      "level",   LevelConverter::new      );          │
│  DEFAULT_CONVERTER_SUPPLIER_MAP.put(                 │
│      "logger",  LoggerConverter::new     );          │
│  DEFAULT_CONVERTER_SUPPLIER_MAP.put(                 │
│      "thread",  ThreadConverter::new     );          │
│  DEFAULT_CONVERTER_SUPPLIER_MAP.put(                 │
│      "msg",     MessageConverter::new    );          │
│  ... many more                                       │
└──────────────────────────────────────────────────────┘
```

Each placeholder maps to a **Converter class**. If you want to know what `%level` does internally, just open `LevelConverter.java`:

```java
public class LevelConverter extends ClassicConverter {

    public String convert(ILoggingEvent le) {
        // fetches level from the log event
        return le.getLevel().toString();
    }
}
```

So it simply reads the level from the log event and converts it to a string. Same logic for all other placeholders.

---

### Encoder Config in XML

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <!-- %d = date, %-5level = level (5 chars wide), -->
        <!-- %logger = logger name, %msg = message, %n = newline -->
        <pattern>%d %-5level %logger - %msg%n</pattern>
    </encoder>
</appender>
```

Output on console:
```
2025-12-17 12:04:16,181 INFO  com.concepts.PaymentController - payment done
```

The instructor's tip here: **you don't need to memorize all placeholders.** Whenever you need a specific field in your log output, just open `PatternLayout.java` in your IDE, check if that field is available, and use the placeholder directly.

---

## Part B — Placeholder Best Practices in Log Statements

This is a small but very important section. The instructor is very strong about this.

### The Wrong Way — String Concatenation

```java
// ❌ NEVER do this
log.info("User " + "Shrayansh" + " created with id " + 123);
```

Why is this bad? Let's trace what happens:

```
Logger level = WARN (say, in production)

log.info("User " + "Shrayansh" + " created with id " + 123)
        │
        ▼
Java first builds the full String:
"User Shrayansh created with id 123"   ← String object created in memory
        │
        ▼
Now passes it to log.info()
        │
        ▼
Level check: INFO < WARN → REJECTED ✗

String was built for NOTHING.
Wasted CPU + memory.
In heavy traffic, this adds up fast.
```

---

### The Right Way — Placeholders `{}`

```java
// ✓ Always do this
log.info("User {} created with id {}", "Shrayansh", 123);
```

How this works:

```
Logger level = WARN (say, in production)

log.info("User {} created with id {}", "Shrayansh", 123)
        │
        ▼
Level check: INFO < WARN → REJECTED ✗
        │
        ▼
String is NEVER built.
"Shrayansh" and 123 are passed as
separate objects — only combined
IF the log statement is accepted.

Zero wasted CPU. Zero wasted memory. ✓
```

The string interpolation (filling `{}` with actual values) **only happens if the log level passes the check.** This saves CPU cycles — which matter a lot in high traffic APIs.

---

### Logging Exceptions — The Right Way

This is another thing the instructor is very particular about:

```java
try {
    // some payment operation
    throw new Exception("Payment failed");
} catch (Exception e) {

    // ❌ Wrong — exception in wrong position
    log.error("Payment failed for User {}", e, "Shrayansh");

    // ✓ Correct — exception is ALWAYS the LAST parameter
    log.error("Payment failed for User {}", "Shrayansh", e);
}
```

Why must exception always be last?

```
log.error("Payment failed for User {}", "Shrayansh", e)
                                         │              │
                                         ▼              ▼
                               fills the {}        Logback detects
                               placeholder        this as a Throwable
                               with "Shrayansh"   and prints full
                                                  stack trace ✓

Output:
Payment failed for User Shrayansh
java.lang.Exception: Payment failed
    at com.concepts.PaymentController.getPayments(PaymentController.java:25)
    at ...
    at ...  ← Full stack trace printed ✓
```

If you put the exception anywhere other than last, the stack trace **will not be printed**. You'll lose the most valuable debugging information.

---

## Full Code — All Placeholder Best Practices Together

```java
public String logsWithPlaceholderSamples() {

    // Simple log — fine
    log.info("info log");

    // ❌ NEVER do string concatenation
    // String is built even if log level rejects this statement
    // Wastes CPU + memory in high traffic
    log.info("User " + "Shrayansh" + " created with id " + 123);

    // ✓ ALWAYS use placeholders
    // String is only built if log level accepts this statement
    log.info("User {} created with id {}", "Shrayansh", 123);

    try {
        throw new Exception("Payment failed");
    } catch (Exception e) {

        // ✓ Exception is ALWAYS the last parameter
        // Only then will the full stack trace be printed
        log.error("Payment failed for User {}", "Shrayansh", e);
    }

    return "successfully fetched all payments";
}
```

---

## The Full Picture — Pattern to Output

```
XML Config:
<pattern>%d %-5level %logger - %msg%n</pattern>

                    │
                    ▼

         PatternLayout.java maps:
         %d      → DateConverter
         %level  → LevelConverter
         %logger → LoggerConverter
         %msg    → MessageConverter

                    │
                    ▼

Your code: log.info("User {} created with id {}", "Shrayansh", 123)

                    │
                    ▼

Log Event holds:
  timestamp  = 2025-12-17 12:04:16
  level      = INFO
  loggerName = com.concepts.PaymentController
  message    = User Shrayansh created with id 123  ← {} filled here

                    │
                    ▼

Final Output:
2025-12-17 12:04:16 INFO  com.concepts.PaymentController - User Shrayansh created with id 123
```

---

## Key Takeaways / Interview Tips

> **"Why should we avoid string concatenation in log statements?"**

Because Java builds the string first, before even checking if the log level is enabled. If the logger rejects that log statement (e.g., logger is set to WARN and you're doing log.info), the string was built for nothing — wasted CPU and memory. With placeholders `{}`, the string is only built if the log statement is actually accepted.

> **"Where do log pattern placeholders like %d, %level, %msg come from?"**

They come from `PatternLayout.java` in the Logback framework. Each placeholder maps to a converter class (e.g., `LevelConverter`, `DateConverter`) that reads the value from the log event.

> **"Where should the exception go in a log statement?"**

Always last. Only when the exception (Throwable) is the last parameter does Logback recognize it as a throwable and print the full stack trace. If placed anywhere else, the stack trace is lost.

---

Ready for **Part 5 — Structured Logging (JSON) + LogstashEncoder**? This is where the instructor goes deep into why plain text logging breaks log aggregation tools and how JSON logging solves it. Let me know!

# Part 5 — Structured Logging (JSON) + LogstashEncoder

## The Problem with Plain Text Logging

Till now, all our logs look like this — plain text, formatted using a pattern:

```
2025-12-17 12:04:16,181 INFO  com.concepts.PaymentController - payment done and payment id 123
```

This looks fine to a human reading it. But in production systems, **you're not the one reading logs — machines are.** Tools like **ELK Stack (Elasticsearch, Logstash, Kibana), Splunk, Datadog, Grafana Loki** — these are log aggregation tools that:

- Collect logs from all your services
- Parse and index them
- Let you search, filter, and aggregate across millions of log lines

And this is where plain text logging **completely breaks down.**

---

### Problem 1 — No Consistent Structure, Regex Parsing is Unreliable

Log aggregation tools have to **parse** your plain text to extract fields like date, level, logger name, message. They do this using regex. But there's no standard — every developer can define their own pattern:

```
Developer A uses hyphen:
2025-12-17 12:04:16  INFO  com.concepts.PaymentController - payment done

Developer B uses pipe:
2025-12-17 12:04:16 | INFO | com.concepts.PaymentController | payment done

Developer C uses different date format:
2025-12-17T12:04:16.181+05:30 INFO com.concepts.PaymentController payment done
```

One regex won't work for all three. And in a large system with many services and many appenders, **each appender can have a different encoder pattern.** This makes parsing unreliable and fragile.

---

### Problem 2 — Useful Business Data is Buried in the Message

Look at this log line:

```
2025-12-17 12:04:16  INFO  com.concepts.PaymentController - payment done and payment id 123
```

The `payment id 123` is a critical piece of business data. But it's buried inside a free-form string. The log aggregation tool has no idea that `123` is a payment ID that it should index and make searchable.

```
Query you WANT to run in your log tool:
"Show me all logs where payment_id = 123"

What actually happens:
The tool has no field called payment_id.
It's just a substring inside a plain text message.
Full-text search might work, but it's slow,
unreliable, and not indexable efficiently. ✗
```

---

### Why NOT to Fake JSON in Pattern

The instructor specifically warns against this shortcut:

```xml
<!-- ❌ Don't do this in production -->
<pattern>{"time":"%d", "level":"%level", "msg":"%msg"}</pattern>
```

Why is this bad?

```
Problem 1: As you need more fields, pattern keeps growing:
{"time":"%d", "level":"%level", "logger":"%logger",
 "thread":"%thread", "msg":"%msg", "class":"%class" ...}
 
 → Becomes unmanageable with 20-30+ fields

Problem 2: Very high chance of mistakes
 → A missing quote or comma breaks the entire JSON
 → Not reliable, not maintainable

Problem 3: Dynamic fields (like payment_id, user_id)
 → Cannot be added this way at all
```

---

## The Solution — Structured Logging with LogstashEncoder

The idea is to stop using pattern-based formatting and instead use a **proper JSON encoder** that automatically converts your log event into a well-structured JSON object.

### Before vs After

```
BEFORE (Pattern based):
log.info
  │
  ▼
Convert to Log Event
  │
  ▼
Pattern Formatting  ← free form text, unreliable
  │
  ▼
Appender
  │
  ▼
Output: plain text string


AFTER (LogstashEncoder):
log.info
  │
  ▼
Convert to Log Event
  │
  ▼
JSON Encoder        ← converts event to proper JSON
  │
  ▼
JSON Decorator      ← optional post-processing
(pretty print,        (masking, formatting)
 masking, etc.)
  │
  ▼
Appender
  │
  ▼
Output: clean, structured JSON
```

---

## When to Use JSON — The Instructor's Rule

Very important — the instructor is clear that **not everything needs to be JSON:**

```
Use JSON format ONLY for data on which
you might later need to:

  ✓ Query     → "find all logs where payment_id = 123"
  ✓ Filter    → "show only ERROR logs from service X"
  ✓ Aggregate → "how many unique payment_ids failed today?"

For everything else → plain text is fine.

It's always a MIX of both.
Not 100% JSON. Not 100% plain text.
```

---

## Step 1 — Add the Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version> <!-- always use a stable version -->
</dependency>
```

---

## Step 2 — What Fields Does LogstashEncoder Give You by Default?

LogstashEncoder uses a framework class called `LogstashFieldNames.java` internally. It automatically reads all these fields from your log event:

```
LogstashEncoder — Default Fields:
┌─────────────────────────────────────────────────┐
│                                                 │
│  @timestamp        ← when the log happened      │
│  @version          ← logstash format version    │
│  message           ← your log message           │
│  thread_name       ← which thread logged this   │
│  logger_name       ← which class logged this    │
│  level             ← INFO / ERROR / WARN etc    │
│  level_value       ← numeric value of level     │
│  caller_class_name ← calling class              │
│  caller_method_name← calling method             │
│  caller_file_name  ← source file name           │
│  caller_line_number← line number in source      │
│  stack_trace       ← exception stack trace      │
│  ... more                                       │
└─────────────────────────────────────────────────┘
```

All of these are fetched automatically from the log event — you don't configure anything extra.

---

## Step 3 — Full Code: Wiring LogstashEncoder

```xml
<!-- ─────────────────────────────────────────── -->
<!-- Appender using LogstashEncoder              -->
<!-- Writing to console in JSON format           -->
<!-- ─────────────────────────────────────────── -->
<appender name="JSON_CONSOLE"
          class="ch.qos.logback.core.ConsoleAppender">

    <!-- Replace the old <pattern> encoder with this -->
    <!-- LogstashEncoder reads the log event and     -->
    <!-- automatically converts it to JSON           -->
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    </encoder>

</appender>

<!-- ─────────────────────────────────────────── -->
<!-- Wire it to your logger                      -->
<!-- ─────────────────────────────────────────── -->
<logger name="com.concepts.PaymentController"
        level="INFO"
        additivity="false">
    <appender-ref ref="JSON_CONSOLE"/>
</logger>
```

---

## Step 4 — What the Output Looks Like Now

```java
// Your code
log.info("payment done and payment id {}", 123);
```

```json
{
  "@timestamp": "2025-12-17T13:23:17.042096+05:30",
  "@version": "1",
  "message": "payment done and payment id 123",
  "logger_name": "com.concepts.PaymentController",
  "thread_name": "http-nio-8080-exec-2",
  "level": "INFO",
  "level_value": 20000
}
```

Now the log aggregation tool gets a clean JSON. No regex. No guessing. Every field is clearly labeled and indexable.

---

## Step 5 — Adding Dynamic Fields Using MDC

The default fields are good, but the **real power** of structured logging is adding your own **dynamic business fields** — like `payment_id`, `user_id`, `order_id` — into the JSON so your aggregation tool can query on them.

This is done using **MDC (Mapped Diagnostic Context)**:

```java
@GetMapping("/payments")
public String getPayments() {

    // Put your dynamic field into MDC BEFORE logging
    // Key = field name in JSON, Value = field value
    MDC.put("payment_id", "123");

    // Now when this log event is created,
    // LogstashEncoder reads MDC automatically
    // and adds payment_id into the JSON output
    log.info("payment is successful");

    return "successfully fetched all payments";
}
```

```
IMPORTANT TIMING RULE:
MDC.put() MUST happen BEFORE log.info()

Timeline:
  MDC.put("payment_id", "123")  ← stored in MDC
          │
          ▼
  log.info("payment is successful")
          │
          ▼
  Log Event created
  (reads MDC at this moment → finds payment_id = 123)
          │
          ▼
  JSON Encoder converts to JSON
  (includes payment_id from MDC) ✓


If you do MDC.put() AFTER log.info():
  log.info("payment is successful")
          │
          ▼
  Log Event created
  (reads MDC → payment_id NOT in MDC yet) ✗
          │
          ▼
  MDC.put("payment_id", "123")  ← too late, event already created
```

---

### Output Now With MDC Field

```json
{
  "@timestamp": "2025-12-17T13:28:34.160552+05:30",
  "@version": "1",
  "message": "payment is successful",
  "logger_name": "com.concepts.PaymentController",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "payment_id": "123"
}
```

Now `payment_id` is a **proper JSON field** — the aggregation tool can index it, search on it, and aggregate across it.

---

### Disabling MDC (If Needed)

By default, LogstashEncoder reads MDC automatically. If you don't want this:

```xml
<appender name="JSON_CONSOLE"
          class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">

        <!-- Set to false if you don't want MDC fields in JSON -->
        <includeMdc>false</includeMdc>

    </encoder>
</appender>
```

---

## The Full Internal Flow — Everything Together

```
Your Code:
MDC.put("payment_id", "123")
log.info("payment is successful")
        │
        ▼
┌───────────────────────────────────────────────┐
│              Logging Pipeline                 │
│                                               │
│  1. Level Check                               │
│     INFO >= INFO ✓                            │
│                                               │
│  2. Log Event Created                         │
│     → reads message, level, thread,           │
│       logger, timestamp from context          │
│     → reads MDC → finds payment_id = 123      │
│                                               │
│  3. LogstashEncoder                           │
│     → converts log event to JSON              │
│     → includes all default fields             │
│     → includes MDC fields (payment_id)        │
│                                               │
│  4. JSON Decorator (optional)                 │
│     → post processing                         │
│     → pretty print / masking                  │
│                                               │
│  5. Appender                                  │
│     → writes JSON to console / file           │
└───────────────────────────────────────────────┘
        │
        ▼
Output:
{
  "@timestamp": "...",
  "message": "payment is successful",
  "level": "INFO",
  "logger_name": "...",
  "thread_name": "...",
  "payment_id": "123"   ← came from MDC
}
        │
        ▼
Log Aggregation Tool
(ELK / Splunk / Datadog)
        │
        ▼
Can now query:
"Show all logs where payment_id = 123" ✓
"Count unique payment_ids that failed" ✓
"Filter by level = ERROR today"        ✓
```

---

## Key Takeaways / Interview Tips

> **"Why is plain text logging a problem in distributed systems?"**

Log aggregation tools need to parse plain text using regex, which breaks when different services or appenders use different patterns. There's no standard, so field extraction is unreliable. Also, business data like payment_id buried inside a message string can't be indexed or queried efficiently.

> **"What is LogstashEncoder and why use it?"**

LogstashEncoder is a library from `logstash-logback-encoder` that replaces the pattern-based encoder. It automatically converts your log event into a structured JSON with all standard fields (timestamp, level, logger name, thread name, etc.) already included. It also reads MDC automatically to include dynamic business fields.

> **"Should all logs be in JSON?"**

No. JSON should only be used for data you'll need to query, filter, or aggregate later. Everything else can stay as plain text. It's always a mix.

> **"What is MDC and how does it work with LogstashEncoder?"**

MDC (Mapped Diagnostic Context) is a thread-local map where you can store key-value pairs. LogstashEncoder reads this map when creating the JSON and includes all MDC fields automatically. The critical rule is: MDC.put() must happen before the log statement, otherwise the log event is already created and MDC data won't be included.

---

Ready for **Part 6 — PII Safe Logging + Masking with JSON Decorator**? This is the last topic of this lecture — where the instructor shows how to protect sensitive data in logs using the MaskingJsonGeneratorDecorator. Let me know!

# Part 6 — PII Safe Logging + Masking with JSON Decorator

## What is PII and Why Does It Matter in Logging?

PII stands for **Personally Identifiable Information** — any data that can be used to identify a specific person, directly or indirectly.

```
What counts as PII?
┌─────────────────────────────────────────┐
│                                         │
│  Direct Identifiers:                    │
│  → Full name                            │
│  → Phone number                         │
│  → Email address                        │
│  → Aadhaar card number                  │
│  → PAN card number                      │
│  → Passport number                      │
│                                         │
│  Sensitive Credentials:                 │
│  → Password                             │
│  → OTP                                  │
│  → Auth tokens                          │
│  → Credit card number                   │
│                                         │
│  Indirect Identifiers:                  │
│  → Address                              │
│  → Date of birth                        │
│  → IP address (in some jurisdictions)   │
│  → Any combination that identifies      │
│    a person indirectly                  │
└─────────────────────────────────────────┘
```

---

## The Golden Rule

The instructor gives one very clear rule:

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  If you don't know whether a piece of data          │
│  is PII or not —                                    │
│                                                     │
│              DO NOT LOG IT.                         │
│                                                     │
│  Why?                                               │
│  If it turns out to be PII after you've             │
│  already logged it → it's a compliance issue.       │
│  Government bodies can charge heavy fines.          │
│                                                     │
│  When in doubt → leave it out.                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## The Real-World Risk

Even with the best intentions, in a large codebase with many developers, someone will accidentally log sensitive data. For example:

```java
// Developer logging payment info
MDC.put("payment_id", "123");
MDC.put("user_email", "shrayansh@gmail.com");  // ← PII accidentally added
MDC.put("password", "myPassword123");           // ← extremely sensitive!
log.info("payment is successful");
```

With LogstashEncoder, all MDC fields go straight into the JSON output:

```json
{
  "@timestamp": "...",
  "message": "payment is successful",
  "level": "INFO",
  "payment_id": "123",
  "user_email": "shrayansh@gmail.com",  ← PII in logs! ✗
  "password": "myPassword123"           ← credentials in logs! ✗
}
```

This is a serious compliance and security violation. The question is — how do we **protect against this automatically** at the framework level, so even if a developer accidentally logs it, it gets masked?

---

## The Solution — JSON Decorator with Masking

Remember from Part 5, after the LogstashEncoder converts the log event to JSON, there's an **optional post-processing step** called the **JSON Decorator**. This is where masking happens.

```
Full Pipeline with Masking:

log.info("payment is successful")
        │
        ▼
  Log Event Created
  (reads MDC → payment_id, password, etc.)
        │
        ▼
  LogstashEncoder
  (converts to JSON)
        │
        ▼
  ┌─────────────────────────────────┐
  │     JSON Decorator              │ ← POST PROCESSING
  │  MaskingJsonGeneratorDecorator  │
  │                                 │
  │  Fields to mask:                │
  │  → password   → *****           │
  │  → token      → *****           │
  │  → creditCard → *****           │
  │                                 │
  │  Scans entire JSON              │
  │  (including nested objects)     │
  │  Replaces matched fields        │
  │  with mask placeholder          │
  └─────────────────────────────────┘
        │
        ▼
  Appender
  (writes masked JSON to output)
        │
        ▼
Output:
{
  "payment_id": "123",
  "password": "*****"   ← masked ✓
}
```

---

## How MaskingJsonGeneratorDecorator Works

```
Input JSON (before decorator):
{
  "payment_id": "123",
  "password": "myPassword123",
  "token": "eyJhbGciOiJIUzI1...",
  "user": {
    "creditCard": "4111111111111111",
    "name": "Shrayansh"
  }
}

            │
            ▼

MaskingJsonGeneratorDecorator
scans ALL fields at ALL nesting levels:

  payment_id  → not in mask list → keep as is ✓
  password    → IN mask list     → replace with ***** ✓
  token       → IN mask list     → replace with ***** ✓
  creditCard  → IN mask list     → replace with ***** ✓
  name        → not in mask list → keep as is ✓

            │
            ▼

Output JSON (after decorator):
{
  "payment_id": "123",
  "password": "*****",
  "token": "*****",
  "user": {
    "creditCard": "*****",
    "name": "Shrayansh"
  }
}
```

Key point the instructor makes: **it works on nested objects too.** If `creditCard` is inside a nested user object, it still gets masked. The decorator scans the entire JSON tree.

---

## Full Code Implementation

### Step 1 — The Appender Config with Masking Decorator

```xml
<appender name="JSON_CONSOLE"
          class="ch.qos.logback.core.ConsoleAppender">

    <!-- JSON Encoder — converts log event to JSON -->
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">

        <!-- ─────────────────────────────────────── -->
        <!-- JSON Decorator — optional post-processing -->
        <!-- Can have MORE than one decorator         -->
        <!-- ─────────────────────────────────────── -->
        <jsonGeneratorDecorator
            class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">

            <!-- What the masked value should look like -->
            <defaultMask>*****</defaultMask>

            <!-- Fields to mask — field name anywhere in JSON -->
            <!-- Works on nested objects too                   -->
            <path>password</path>
            <path>token</path>
            <path>creditCard</path>

        </jsonGeneratorDecorator>

    </encoder>

</appender>

<!-- Wire to logger -->
<logger name="com.concepts.PaymentController"
        level="INFO"
        additivity="false">
    <appender-ref ref="JSON_CONSOLE"/>
</logger>
```

---

### Step 2 — The Java Code (Simulating Accidental PII Logging)

```java
@GetMapping("/payments")
public String getPayments() {

    // payment_id → fine, not PII
    MDC.put("payment_id", "123");

    // password → sensitive! accidentally logged
    // but decorator will mask it automatically
    MDC.put("password", "myPassword123");

    log.info("payment is successful");

    return "successfully fetched all payments";
}
```

---

### Step 3 — What the Output Looks Like

```json
{
  "@timestamp": "2025-12-17T14:03:08.935870+05:30",
  "@version": "1",
  "message": "payment is successful",
  "logger_name": "com.concepts.PaymentController",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "payment_id": "123",
  "password": "*****"
}
```

`payment_id` comes through cleanly. `password` is automatically masked to `*****` by the decorator — even though the developer accidentally put it in MDC. The framework saved the day.

---

## The Complete End-to-End Picture — Everything in This Lecture

Now that we've covered all 6 parts, here's the full picture of everything working together:

```
Your Code:
──────────
MDC.put("payment_id", "123")
MDC.put("password", "myPassword123")
log.info("payment is successful")
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│                   Logging Pipeline                       │
│                                                          │
│  1. LEVEL CHECK (sync)                                   │
│     INFO >= INFO ✓                                       │
│                                                          │
│  2. LOG EVENT CREATION (sync)                            │
│     → captures: message, level, thread,                  │
│       logger, timestamp, MDC fields                      │
│                                                          │
│  3. ASYNC APPENDER (if configured)                       │
│     → puts event in in-memory queue                      │
│     → request thread is FREE ✓                           │
│     → worker thread picks up event                       │
│     → LevelFilter: deny ERROR here                       │
│                                                          │
│  4. LOGSTASH ENCODER                                     │
│     → converts log event to JSON                         │
│     → includes default fields automatically              │
│     → reads MDC → adds payment_id, password to JSON      │
│                                                          │
│  5. JSON DECORATOR (MaskingJsonGeneratorDecorator)       │
│     → scans entire JSON                                  │
│     → finds "password" → replaces with *****             │
│     → finds "token" → replaces with *****                │
│     → payment_id not in list → passes through            │
│                                                          │
│  6. APPENDER                                             │
│     → writes final masked JSON to output                 │
│       (console / file / Kafka)                           │
└──────────────────────────────────────────────────────────┘
        │
        ▼
Final Output:
{
  "@timestamp": "2025-12-17T14:03:08.935870+05:30",
  "message": "payment is successful",
  "level": "INFO",
  "logger_name": "com.concepts.PaymentController",
  "thread_name": "http-nio-8080-exec-1",
  "payment_id": "123",       ← business field from MDC ✓
  "password": "*****"        ← masked by decorator ✓
}
        │
        ▼
Log Aggregation Tool
(ELK / Splunk / Datadog)
→ Can query on payment_id ✓
→ password never exposed ✓
```

---

## Key Takeaways / Interview Tips

> **"What is PII and how should you handle it in logging?"**

PII is any information that can identify a person directly or indirectly — phone number, email, password, OTP, credit card, etc. The golden rule is: if you're unsure whether something is PII, don't log it. For data that might accidentally get logged, use MaskingJsonGeneratorDecorator to automatically replace sensitive field values with a placeholder like `*****`.

> **"What is a JSON Decorator in Logback?"**

It's an optional post-processing step that runs after LogstashEncoder converts the log event to JSON, but before the appender writes it to output. It can be used for pretty printing the JSON or masking sensitive fields. You can chain multiple decorators together.

> **"How does MaskingJsonGeneratorDecorator work?"**

You configure a list of field names (paths) to mask and a default mask value. The decorator scans the entire JSON output — including nested objects — and replaces the value of any matching field with the mask placeholder. It works automatically regardless of where in the JSON the field appears.

> **"What comes after this in the series?"**

The instructor mentions that MDC (Mapped Diagnostic Context) will be covered in depth in the next part, along with actual distributed logging — how logs are correlated across multiple microservices. MDC plays a critical role there too, specifically for propagating a trace/correlation ID across service boundaries.

---

## Complete logback.xml Putting Everything Together

```xml
<configuration>

    <!-- ─────────────────────────────────────── -->
    <!-- 1. Non-critical File Appender           -->
    <!-- Receives INFO/WARN/DEBUG/TRACE via ASYNC -->
    <!-- ─────────────────────────────────────── -->
    <appender name="FILE_NOT_CRITICAL"
              class="ch.qos.logback.core.FileAppender">
        <file>logs/non_critical_app.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">

            <!-- Mask sensitive fields even in non-critical logs -->
            <jsonGeneratorDecorator
                class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
                <defaultMask>*****</defaultMask>
                <path>password</path>
                <path>token</path>
                <path>creditCard</path>
            </jsonGeneratorDecorator>

        </encoder>
    </appender>

    <!-- ─────────────────────────────────────── -->
    <!-- 2. AsyncAppender wrapping               -->
    <!--    FILE_NOT_CRITICAL                    -->
    <!-- Denies ERROR — goes to sync instead     -->
    <!-- ─────────────────────────────────────── -->
    <appender name="ASYNC"
              class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>1000</queueSize>
        <discardingThreshold>10</discardingThreshold>
        <neverBlock>false</neverBlock>

        <!-- Deny ERROR — handle it synchronously elsewhere -->
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>DENY</onMatch>
            <onMismatch>NEUTRAL</onMismatch>
        </filter>

        <appender-ref ref="FILE_NOT_CRITICAL"/>
    </appender>

    <!-- ─────────────────────────────────────── -->
    <!-- 3. Critical File Appender               -->
    <!-- Synchronous — only ERROR reaches here   -->
    <!-- ─────────────────────────────────────── -->
    <appender name="FILE_CRITICAL"
              class="ch.qos.logback.core.FileAppender">
        <file>logs/critical_app.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">

            <!-- Mask sensitive fields in critical logs too -->
            <jsonGeneratorDecorator
                class="net.logstash.logback.mask.MaskingJsonGeneratorDecorator">
                <defaultMask>*****</defaultMask>
                <path>password</path>
                <path>token</path>
                <path>creditCard</path>
            </jsonGeneratorDecorator>

        </encoder>

        <!-- Only ERROR and above pass through -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>

    </appender>

    <!-- ─────────────────────────────────────── -->
    <!-- 4. Logger wired to both appenders       -->
    <!-- ─────────────────────────────────────── -->
    <logger name="com.concepts.PaymentController"
            level="INFO"
            additivity="false">
        <appender-ref ref="ASYNC"/>          <!-- async for non-critical -->
        <appender-ref ref="FILE_CRITICAL"/>  <!-- sync for ERROR -->
    </logger>

</configuration>
```

---

That's everything from **Distributed Logging Part 3**. The next part of the series will go deep into **MDC (Mapped Diagnostic Context)** and how it enables **distributed tracing across microservices** — correlating logs from Service A → Service B → Service C using a single trace/correlation ID. That's where all of this structured logging setup truly pays off!