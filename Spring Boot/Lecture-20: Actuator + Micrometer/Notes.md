# Spring Boot Actuator — Step 1: What is it & Why does it exist?

---

## The Problem

Imagine you've built a Spring Boot application and deployed it to production. Now ask yourself:

- Is my application currently **up and running**?
- How much **memory** is the JVM consuming right now?
- Are there any **deadlocked threads** silently killing performance?
- Is my **database connection pool** exhausted?
- How many **HTTP requests** has my server handled today?

Without any tooling, you'd have to dig through logs, SSH into servers, write custom monitoring code yourself — it's a mess.

```
WITHOUT Actuator:
=================

Your App (Production)
      |
      |-----> Is it UP?          --> Go check logs manually
      |-----> Memory usage?      --> SSH into server, run JVM tools
      |-----> Thread deadlock?   --> Pray you notice before users do
      |-----> DB pool status?    --> Write custom code yourself
      |-----> HTTP stats?        --> Again, custom code

Result: No standard way. Each team does it differently.
        Ops team is blind to what's happening inside your app.
```

---

## The Solution: Spring Boot Actuator

Actuator solves this by giving you **production-ready HTTP endpoints** out of the box — no custom code needed. You just add a dependency, and Spring Boot exposes a standard set of URLs that tell you everything about your running application.

```
WITH Actuator:
==============

Your App (Production)
      |
      |-----> GET /actuator/health      --> { "status": "UP" }
      |-----> GET /actuator/metrics     --> JVM memory, CPU, threads...
      |-----> GET /actuator/threaddump  --> All threads + stack traces
      |-----> GET /actuator/beans       --> All Spring beans loaded
      |-----> GET /actuator/env         --> All environment properties
      |
      Result: One dependency. Standard endpoints. Zero custom code.
```

---

## The Big Picture — How Actuator fits in your system

```
+--------------------------------------------------+
|              Your Spring Boot App                |
|                                                  |
|   +------------+        +--------------------+   |
|   |  Business  |        |  Spring Actuator   |   |
|   |   Logic    |        |   (monitoring      |   |
|   | (REST APIs)|        |    layer)          |   |
|   +------------+        +--------------------+   |
|         |                        |               |
+---------|------------------------|---------------+
          |                        |
          v                        v
   Your API consumers        Ops / DevOps team
   (frontend, mobile,        (hits /health, /metrics
    other services)           to monitor the app)
                                   |
                                   v
                         Monitoring Platforms
                      (Datadog, Prometheus, etc.)
```

The key insight: **your business logic and your monitoring layer are completely separate**. Actuator sits alongside your app and exposes a window into its internals — without you writing a single line of monitoring code.

---

## Real-World Use Case

In any production company, there's typically an **Ops/DevOps team** that monitors all running services. They use tools like Datadog, Prometheus, or CloudWatch dashboards. These tools need to **pull data** from your application periodically.

Actuator is what makes that possible. It is the **standard bridge** between your Spring Boot app and any monitoring platform.

```
Real-World Flow:
================

Spring Boot App                Monitoring Platform
(with Actuator)                (Datadog / Prometheus)
      |                                |
      |<------- scrape every 5s -------|
      |                                |
      |------- metrics data ---------->|
                                       |
                               Dashboard / Alerts
                          (graphs, anomaly detection,
                           on-call alerts to engineers)
```

---

## When should you reach for Actuator?

The answer is simple: **always, in any production Spring Boot app.** It's not something you turn on for a special case — it's a standard part of every professional Spring Boot setup.

Specifically think of it when:

- You need to monitor **application health** (is it up? are dependencies up?)
- You need **JVM diagnostics** (memory, GC, threads)
- You need **HTTP traffic stats** (how many requests, how slow?)
- You need to **integrate with a monitoring platform** (Datadog, Prometheus)
- You need to expose **custom operational data** (e.g., cache hit rate, queue size)

---
# Spring Boot Actuator — Step 2: Project Setup

---

## What do you need to get started?

Just **one dependency**. That's it. Spring Boot auto-configures everything else.

---

## Step 2.1 — Adding the Dependency

If you're creating a **fresh project** on [start.spring.io](https://start.spring.io), search for "Spring Boot Actuator" and add it. It will put this into your `pom.xml`:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

If you already have an **existing project**, just paste the above dependency into your `pom.xml` manually.

That's all. Once the dependency is on the classpath, Spring Boot auto-configures Actuator for you.

---

## Step 2.2 — application.properties (Two Key Properties)

After adding the dependency, there are **two important properties** you can set. Both are **optional** — but understanding what happens with and without them is critical.

```
application.properties — Two Optional Properties:
==================================================

Property 1: management.endpoints.web.base-path
------------------------------------------------
  What it does : Sets the base URL path for all actuator endpoints.
  Default      : /actuator   (if you don't set this)
  Example      : /manage     (if you override it)

Property 2: management.endpoints.web.exposure.include
------------------------------------------------------
  What it does : Controls WHICH endpoints are visible/accessible.
  Default      : Only /health and /info are exposed.
  Example      : * means expose ALL endpoints.
                 Or comma-separated list: health,info,metrics,loggers
```

Here's how both properties play out:

```
SCENARIO 1: Neither property set (pure defaults)
=================================================

Base path        -->  /actuator
Exposed endpoints-->  /actuator/health
                      /actuator/info
                      (everything else is hidden)


SCENARIO 2: Both properties set (like in the lecture)
======================================================

Base path        -->  /manage        (overridden)
Exposed endpoints-->  ALL            (* means everything)

So you can now hit:
  /manage/health
  /manage/metrics
  /manage/threaddump
  /manage/beans
  /manage/env
  ... and so on
```

---

## Step 2.3 — The Full application.properties

```properties
# application.properties

spring.application.name=order-service

# OPTIONAL: Override the default base path from /actuator to /manage
# If you skip this, all endpoints are under /actuator
management.endpoints.web.base-path=/manage

# OPTIONAL: Expose which endpoints over HTTP
# Default: only 'health' and 'info' are exposed
# '*' means expose ALL endpoints
# For selective exposure: health,info,metrics,loggers
management.endpoints.web.exposure.include=*
```

---

## The Full Picture — What happens after setup

```
+-------------------------------------------------------+
|                  Spring Boot App                      |
|                                                       |
|   pom.xml                                             |
|   [ spring-boot-starter-actuator ]  <-- add this      |
|                                                       |
|   application.properties                              |
|   [ base-path = /manage          ]  <-- optional      |
|   [ exposure.include = *         ]  <-- optional      |
|                                                       |
+-------------------------------------------------------+
                        |
                        | App starts up
                        v
+-------------------------------------------------------+
|              Actuator Auto-Configuration              |
|                                                       |
|   Spring Boot reads your properties and registers     |
|   all actuator endpoints under the base path.         |
|                                                       |
|   /manage/health       --> always available           |
|   /manage/info         --> always available           |
|   /manage/metrics      --> available (because *)      |
|   /manage/threaddump   --> available (because *)      |
|   /manage/beans        --> available (because *)      |
|   /manage/env          --> available (because *)      |
|   ... and many more                                   |
+-------------------------------------------------------+
                        |
                        v
             Ops team / monitoring tools
             can now hit these endpoints
```

---

## Quick Decision Chart — Which properties to set?

```
Q: Do you want a custom base path?
   YES --> set management.endpoints.web.base-path=/yourpath
   NO  --> leave it, default is /actuator

Q: Do you want all endpoints exposed?
   YES --> management.endpoints.web.exposure.include=*
   NO  --> management.endpoints.web.exposure.include=health,info,metrics
           (comma-separated list of only what you need)

Q: Production vs Development?
   Development --> * is fine (expose everything for testing)
   Production  --> expose only what you need + add security (Step 6)
```

---

## One Important Thing to Note

Just because an endpoint is **exposed** doesn't mean it's **secure**. Right now, anyone who knows your URL and port can hit these endpoints. The instructor specifically calls this out — security comes later in Step 6. For now, we're just getting things running.

```
Exposed ≠ Secure

/manage/health  --> exposed, public, anyone can hit it   [fine for now]
/manage/metrics --> exposed, public, anyone can hit it   [risky in prod]
/manage/env     --> exposed, public, sensitive data!     [must secure]
```

---

Ready for **Step 3 — The `/health` endpoint** (basic usage + extending it with custom DB and Cache health checks)?

# Spring Boot Actuator — Step 3: The `/health` Endpoint

---

## What does `/health` do?

It tells you the **overall health status** of your application. Spring Boot aggregates the health of all components and returns one of four possible statuses:

```
Possible Health Statuses:
==========================

  UP           --> Everything is running fine
  DOWN         --> Something is broken
  OUT_OF_SERVICE --> Temporarily taken offline (e.g. maintenance)
  UNKNOWN      --> Health could not be determined
```

By default, hitting `/manage/health` gives you just this:

```json
{
  "status": "UP"
}
```

Simple. But in real production systems, this is not enough. Let's understand why.

---

## The Problem with Just "App is UP"

Think about a real production service. Your Spring Boot app being up is only **one part** of the story. What about:

```
Real Production System:
========================

  Your Spring Boot App  -->  UP   (Tomcat running fine)
         |
         |-----> Database          -->  ??? (is it up?)
         |-----> Cache (Redis)     -->  ??? (is it up?)
         |-----> Message Broker    -->  ??? (is it up?)

If DB is DOWN, your app is effectively DOWN for users.
But /health says "UP" because Tomcat is running.
That's misleading!
```

So the instructor says: we need to **extend** `/health` to check all sub-components. Only when **all of them are up** should the overall status be `UP`. If **any one is down**, the overall should be `DOWN`.

---

## How `/health` Aggregation Works

```
/health Aggregation Logic:
===========================

  Sub-component 1: DB     --> UP
  Sub-component 2: Cache  --> DOWN   <-- one is down
  Sub-component 3: App    --> UP
         |
         v
  Aggregated Status --> DOWN
  (if ANY sub-component is DOWN, overall is DOWN)

  Only when ALL are UP:
  ---------------------
  Sub-component 1: DB     --> UP
  Sub-component 2: Cache  --> UP
  Sub-component 3: App    --> UP
         |
         v
  Aggregated Status --> UP
```

---

## Step 3.1 — Extending `/health` with Custom Health Indicators

To add a custom health check, you create a class that **implements `HealthIndicator`** interface. Spring Boot automatically picks it up (because it's a `@Component`) and includes it in the aggregated health check.

The pattern is always the same:

```
Pattern for Custom Health Indicator:
======================================

  1. Create a class
  2. Annotate with @Component
  3. Implement HealthIndicator interface
  4. Override the health() method
  5. Inside health(), check your component (DB, Cache, etc.)
  6. Return Health.up() or Health.down() based on the result
```

---

## Step 3.2 — DB Health Indicator (Code)

```java
// DatabaseHealthIndicator.java

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean isDBUp = checkDBConnection();

        if (isDBUp) {
            return Health.up()
                         .withDetail("DB", "Available")
                         .build();
        } else {
            return Health.down()
                         .withDetail("DB", "NotAvailable")
                         .build();
        }
    }

    private boolean checkDBConnection() {
        // In production: actually try to query the DB here
        // e.g. run a simple "SELECT 1" and see if it succeeds
        // For now, returning true to simulate DB is up
        return true;
    }
}
```

---

## Step 3.3 — Cache Health Indicator (Code)

```java
// CacheHealthIndicator.java

@Component
public class CacheHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean isCacheUp = checkCacheStatus();

        if (isCacheUp) {
            return Health.up()
                         .withDetail("Cache", "Available")
                         .build();
        } else {
            return Health.down()
                         .withDetail("Cache", "NotAvailable")
                         .build();
        }
    }

    private boolean checkCacheStatus() {
        // In production: ping your Redis/Memcached here
        // For now, returning false to simulate Cache is DOWN
        return false;
    }
}
```

---

## What happens now when you hit `/manage/health`?

Since Cache is returning `false` (simulating it's down), the aggregated status becomes `DOWN`. But by default, you only see the top-level status:

```json
{
  "status": "DOWN"
}
```

You can see the overall is `DOWN` — but WHERE is the detail? Which component is down? You can't tell yet. That's the next problem to solve.

---

## Step 3.4 — Showing Health Details

By default, Spring Boot hides the component-level details. To see them, add this to `application.properties`:

```properties
# application.properties

# Show full details of each sub-component in /health
# Default is 'never' — only shows the top-level status
management.endpoint.health.show-details=always
```

Now hitting `/manage/health` gives you the full picture:

```json
{
  "status": "DOWN",
  "components": {
    "cache": {
      "status": "DOWN",
      "details": {
        "Cache": "NotAvailable"
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "DB": "Available"
      }
    }
  }
}
```

Now you can clearly see: DB is up, Cache is down, and therefore overall is down.

---

## The Full Flow — Putting it all together

```
Request: GET /manage/health
             |
             v
+------------------------------------------+
|        Spring Boot Health Aggregator      |
|                                           |
|  Collects health from all indicators:     |
|                                           |
|  DatabaseHealthIndicator.health()         |
|    --> checkDBConnection() = true         |
|    --> Health.up() + detail "Available"   |
|                                           |
|  CacheHealthIndicator.health()            |
|    --> checkCacheStatus() = false         |
|    --> Health.down() + detail "N/A"       |
|                                           |
|  Aggregation Rule:                        |
|    ALL up?  --> overall UP                |
|    ANY down?--> overall DOWN              |
|                                           |
|  Result: DOWN (cache is down)             |
+------------------------------------------+
             |
             v
   show-details=never    show-details=always
   +--------------+      +------------------+
   | {            |      | {                |
   |  "status":   |      |  "status":"DOWN",|
   |  "DOWN"      |      |  "components":{  |
   | }            |      |   "cache":{...}, |
   +--------------+      |   "database":{..}|
                         |  }               |
                         | }                |
                         +------------------+
```

---

## Full application.properties so far

```properties
# application.properties

spring.application.name=order-service

# Override default base path
management.endpoints.web.base-path=/manage

# Expose all endpoints
management.endpoints.web.exposure.include=*

# Show full sub-component details in /health
management.endpoint.health.show-details=always
```

---

## Interview Tips

> **Q: What is `HealthIndicator` in Spring Boot Actuator?**
> It's an interface you implement to add a custom health check to the `/health` endpoint. Spring Boot automatically discovers all `@Component` classes implementing `HealthIndicator` and includes them in the aggregated health response.

> **Q: If one sub-component is DOWN, what does `/health` return overall?**
> `DOWN`. The aggregation rule is: ALL must be UP for the overall to be UP. Even one DOWN makes the whole thing DOWN.

> **Q: What is `show-details=always` for?**
> By default `/health` only shows the top-level aggregated status. Setting `show-details=always` exposes the per-component breakdown — which component is up, which is down, and what the detail message says.

---

Ready for **Step 4 — `/metrics`, `/threaddump`, and other useful built-in endpoints**?

# Spring Boot Actuator — Step 4: `/metrics`, `/threaddump` & Other Built-in Endpoints

---

## The Big Picture — What Built-in Endpoints are Available?

Before diving into each one, here's the full map of what Actuator gives you out of the box:

```
All Built-in Actuator Endpoints:
==================================

BASE: /manage  (or /actuator if you didn't override)
        |
        |-----> /health          GET  --> App + sub-component health
        |-----> /metrics         GET  --> List of all available metrics
        |-----> /metrics/{name}  GET  --> Data for a specific metric
        |-----> /threaddump      GET  --> All threads + stack traces
        |-----> /heapdump        GET  --> JVM heap dump (.hprof file)
        |-----> /beans           GET  --> All Spring beans loaded
        |-----> /mappings        GET  --> All @RequestMapping URLs
        |-----> /env             GET  --> All environment properties
        |-----> /env/{property}  GET  --> A specific property value
        |-----> /configprops     GET  --> All @ConfigurationProperties
        |-----> /loggers         GET  --> All loggers + their levels
        |-----> /shutdown        POST --> Gracefully stop the app
```

We covered `/health` in Step 3. Now let's go through the rest, starting with the most important ones.

---

## Step 4.1 — `/metrics` Endpoint

This endpoint works in **two levels**:

```
Level 1: GET /manage/metrics
==============================
  Returns a LIST of all metric names currently available.
  Think of it as a menu — it tells you what you CAN query.

Level 2: GET /manage/metrics/{metric-name}
==========================================
  Returns the ACTUAL DATA for that specific metric.
  You pick one item from the menu and get its value.
```

So the flow is always:

```
Step 1: Hit /manage/metrics
        --> Get list of all metric names
               |
               |--> jvm.memory.used
               |--> jvm.memory.max
               |--> jvm.gc.pause
               |--> jvm.threads.live
               |--> http.server.requests
               |--> system.cpu.usage
               |--> jdbc.connections.active
               |--> executor.pool.core
               ... and many more

Step 2: Pick one, e.g. jvm.memory.used
        Hit /manage/metrics/jvm.memory.used
        --> Get the actual memory value
```

---

## Step 4.2 — Important Metrics Broken Down by Category

The instructor groups the metrics into categories. Let's go through each:

---

### Category 1: JVM Memory Metrics

```
Metric Name       What it tells you           Example Response
-----------------------------------------------------------------
jvm.memory.used   Memory currently used        { "value": 99478616 }
                  by the JVM (in bytes)        (~94 MB in use)

jvm.memory.max    Maximum memory the JVM       { "value": 10989076477 }
                  is allowed to use (bytes)    (~10 GB max allowed)
```

Hit it like this:
```
GET /manage/metrics/jvm.memory.used
GET /manage/metrics/jvm.memory.max
```

Response structure:
```json
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 99478616
    }
  ]
}
```

---

### Category 2: Garbage Collection Metrics

```
Metric Name       What it tells you
--------------------------------------
jvm.gc.pause      Everything about GC pauses:

                  COUNT      --> Total number of GC events that occurred
                  TOTAL_TIME --> Total time spent doing GC (in seconds)
                  MAX        --> Longest single GC pause observed
```

```json
{
  "name": "jvm.gc.pause",
  "measurements": [
    { "statistic": "COUNT",      "value": 12   },
    { "statistic": "TOTAL_TIME", "value": 2.305 },
    { "statistic": "MAX",        "value": 0.9  }
  ]
}
```

Why does this matter? If your `MAX` GC pause is very high (say 5–10 seconds), your app is freezing for users during GC. This metric helps you catch that.

---

### Category 3: Thread Metrics

```
Metric Name         What it tells you
-------------------------------------------
jvm.threads.live    Number of threads currently alive in the JVM
jvm.threads.peak    Highest number of threads ever alive since JVM started
```

```json
// GET /manage/metrics/jvm.threads.live
{
  "measurements": [
    { "statistic": "VALUE", "value": 22 }
  ]
}

// GET /manage/metrics/jvm.threads.peak
{
  "measurements": [
    { "statistic": "VALUE", "value": 50 }
  ]
}
```

---

### Category 4: System Metrics

```
Metric Name         What it tells you
-------------------------------------------
system.cpu.usage    CPU being used by JVM.
                    Range: 0.0 to 1.0
                    So 0.10 means 10% CPU usage
```

---

### Category 5: HTTP Server / Request Metrics

This one is very useful in production. It tells you everything about HTTP traffic hitting your app:

```
Metric Name          What it tells you
-------------------------------------------
http.server.requests  COUNT      --> Total HTTP requests received
                      TOTAL_TIME --> Total time spent handling all requests
                      MAX        --> Longest time taken for a single request
```

```json
{
  "name": "http.server.requests",
  "measurements": [
    { "statistic": "COUNT",      "value": 152  },
    { "statistic": "TOTAL_TIME", "value": 23.45 },
    { "statistic": "MAX",        "value": 0.89  }
  ],
  "availableTags": [
    { "tag": "method", "values": ["GET", "POST"] },
    { "tag": "status", "values": ["200", "404", "500"] }
  ]
}
```

Notice the `availableTags` — you can filter by HTTP method or status code. Very useful for spotting how many 500 errors you're getting.

---

### Category 6: Database / JDBC Metrics

```
Metric Name               What it tells you
----------------------------------------------
jdbc.connections.active   Connections currently being used   (e.g. 3)
jdbc.connections.idle     Connections sitting idle in pool   (e.g. 7)
jdbc.connections.max      Max connections the pool allows    (e.g. 10)
```

This is critical for catching **connection pool exhaustion** — one of the most common production issues in Spring Boot apps. If `active` equals `max`, your pool is full and new requests will start queuing or failing.

```
Connection Pool Health Check (mental model):
=============================================

  jdbc.connections.max    = 10   (pool capacity)
  jdbc.connections.active = 3    (in use right now)
  jdbc.connections.idle   = 7    (free, ready to use)
  --> HEALTHY

  jdbc.connections.max    = 10   (pool capacity)
  jdbc.connections.active = 10   (ALL in use!)
  jdbc.connections.idle   = 0    (none free)
  --> DANGER: pool exhausted, new requests will hang!
```

---

### Category 7: Thread Pool Executor Metrics (from `@Async`)

Remember `@Async` from the previous lecture? Actuator can tell you the state of those thread pools too:

```
Metric Name              What it tells you
--------------------------------------------
executor.pool.core       Core thread pool size
executor.pool.max        Max thread pool size
executor.active          Threads currently executing tasks
executor.queued          Tasks waiting in the queue
executor.completed       Tasks completed so far
```

```
GET /manage/metrics/executor.pool.core
--> tells you the core size of your async thread pool
```

---

## Step 4.3 — `/threaddump` Endpoint

```
GET /manage/threaddump
```

This is your **diagnostic tool for thread problems**. It dumps the full state of every thread running in the JVM at that moment.

What it tells you per thread:

```
Per Thread Information:
========================

  threadName   --> Name of the thread (e.g. "main", "http-nio-8080-exec-1")
  threadId     --> Unique ID of the thread
  threadState  --> Current state:
                     RUNNABLE  --> actively executing code
                     WAITING   --> waiting for something (e.g. lock)
                     BLOCKED   --> blocked trying to acquire a lock
                     TIMED_WAITING --> waiting with a timeout
  stackTrace   --> Exact lines of code this thread is executing right now
  blockedCount --> How many times this thread has been blocked
  waitedCount  --> How many times this thread has waited
```

Example response:

```json
[
  {
    "threadName": "main",
    "threadId": 1,
    "threadState": "RUNNABLE",
    "blockedCount": 0,
    "waitedCount": 0,
    "stackTrace": [
      "com.concepts.MyService.methodName(MyService.java:142)",
      "com.concepts.ActuatorApp.main(ActuatorApp.java:10)"
    ]
  },
  {
    "threadName": "thread2",
    "threadId": 2,
    "threadState": "WAITING",
    "blockedCount": 0,
    "waitedCount": 1,
    "stackTrace": [
      "com.concepts.ClassName.methodName(ClassName.java:12)",
      "com.concepts.ActuatorApp.main(ActuatorApp.java:10)"
    ]
  }
]
```

### When do you use `/threaddump` in real life?

```
Real-World Scenarios for /threaddump:
=======================================

Scenario 1: Deadlock suspected
  --> Hit /threaddump
  --> Look for threads in BLOCKED state
  --> Check their stackTraces
  --> If Thread A is blocked waiting for Thread B,
      and Thread B is blocked waiting for Thread A
      --> Deadlock confirmed!

Scenario 2: App is slow / hanging
  --> Hit /threaddump
  --> Look for threads stuck in WAITING state
  --> stackTrace will show you exactly which line
      of code they're stuck at

Scenario 3: Thread leak suspected
  --> Hit /threaddump repeatedly over time
  --> If thread count keeps growing and never shrinks
      --> Thread leak confirmed
```

---

## Step 4.4 — Other Useful Endpoints (Quick Reference)

```
Endpoint           Method   What it does
----------------------------------------------------------
/beans             GET      Lists ALL Spring beans loaded
                            in your application context.
                            Useful to verify if a bean
                            got registered correctly.

/mappings          GET      Lists ALL @RequestMapping URLs
                            in your app. Useful to see
                            every endpoint your app exposes.

/env               GET      Shows ALL environment properties:
                            application.properties values,
                            system env variables, JVM args.

/env/{property}    GET      Shows value of one specific
                            property. e.g. /env/server.port

/configprops       GET      Lists all @ConfigurationProperties
                            beans and their current values.

/loggers           GET      Lists all loggers and their
                            current log level (INFO, DEBUG, etc.)
                            You can even change log levels
                            at runtime without restart!
```

---

## The Full Metrics Flow — Diagram

```
                    GET /manage/metrics
                           |
                           v
              +------------------------+
              |  Returns list of all   |
              |  available metric      |
              |  names (the "menu")    |
              +------------------------+
                           |
              Pick one: jvm.memory.used
                           |
                           v
          GET /manage/metrics/jvm.memory.used
                           |
                           v
              +------------------------+
              |  Returns actual data   |
              |  for that metric:      |
              |  { "value": 99478616 } |
              +------------------------+


Categories of metrics available:
==================================

  /metrics/jvm.memory.used       \
  /metrics/jvm.memory.max         } JVM Memory
                                 /
  /metrics/jvm.gc.pause          --> GC stats

  /metrics/jvm.threads.live      \
  /metrics/jvm.threads.peak       } Threads

  /metrics/system.cpu.usage      --> CPU

  /metrics/http.server.requests  --> HTTP traffic

  /metrics/jdbc.connections.*    --> DB pool

  /metrics/executor.*            --> Async thread pool
```

---

## Interview Tips

> **Q: How do you check HTTP request count and slowest request time in Spring Boot?**
> Use `/actuator/metrics/http.server.requests`. It gives COUNT (total requests), TOTAL_TIME (cumulative handling time), and MAX (slowest single request).

> **Q: How would you diagnose a deadlock in a Spring Boot app in production?**
> Hit `/actuator/threaddump`. Look for threads in BLOCKED state. Check their stack traces — if two threads are each waiting for a lock the other holds, that's your deadlock.

> **Q: How do you monitor JDBC connection pool exhaustion?**
> Use `/actuator/metrics/jdbc.connections.active` and compare it against `/actuator/metrics/jdbc.connections.max`. If active equals max, the pool is exhausted.

---

Ready for **Step 5 — Critical Endpoints: `/shutdown` & `/heapdump` and why they need special treatment**?

# Spring Boot Actuator — Step 5: Critical Endpoints — `/shutdown` & `/heapdump`

---

## The Problem — Not All Endpoints are Equal

So far we've seen endpoints that are **read-only** — they just tell you things about your app. But two endpoints are fundamentally different because they can either **stop your app** or **expose sensitive data**:

```
Regular Endpoints (read-only, relatively safe):
================================================
  /health    --> just reads app status
  /metrics   --> just reads numbers
  /threaddump --> just reads thread state
  /env       --> reads properties (still needs securing, but read-only)

Critical Endpoints (can cause real damage):
============================================
  /shutdown  --> STOPS your entire application (POST)
  /heapdump  --> DOWNLOADS your JVM heap as a file
                 which can contain: passwords, tokens,
                 session data, user PII, encryption keys
```

---

## Step 5.1 — `/shutdown` Endpoint

### What does it do?

A single POST request to `/manage/shutdown` will **gracefully stop your running Spring Boot application**. The JVM shuts down. Your service goes offline. Every user currently using the app gets disconnected.

```
POST /manage/shutdown
        |
        v
  Spring Boot receives request
        |
        v
  Triggers graceful shutdown sequence:
    --> Stops accepting new requests
    --> Finishes in-flight requests
    --> Closes DB connections
    --> Destroys all beans (@PreDestroy runs)
    --> JVM exits
        |
        v
  Your app is DEAD.
  Users see: Connection Refused
```

### Why is this dangerous?

```
Imagine this scenario:
=======================

  - Your app is running in production
  - 10,000 users are actively using it
  - An attacker (or even a junior dev by mistake)
    knows your actuator URL
  - They POST to /manage/shutdown
  - Your entire service goes down instantly
  - Result: complete outage
```

---

## Step 5.2 — `/heapdump` Endpoint

### What does it do?

It downloads the **entire JVM heap memory** as a `.hprof` file. The heap contains every object currently in memory — which means it can contain:

```
What lives in JVM Heap Memory:
================================

  - Database passwords (from your DataSource config)
  - JWT tokens (currently active user sessions)
  - API keys (third-party service credentials)
  - User PII (names, emails, phone numbers in memory)
  - Encryption keys
  - Internal service URLs
  - Session data

GET /manage/heapdump
      |
      v
  Downloads a .hprof file
      |
      v
  Attacker opens it in VisualVM or Eclipse MAT
      |
      v
  Can read ALL of the above in plain text
  --> Complete security breach
```

---

## Step 5.3 — Spring Boot's Default Behaviour

Spring Boot is aware of how dangerous these two endpoints are. So it makes a deliberate design decision:

```
Default Access Rules:
======================

  All other endpoints (health, metrics, etc.)
  --> access = UNRESTRICTED  (you can hit them freely)

  /shutdown
  --> access = RESTRICTED  (blocked by default)

  /heapdump
  --> access = RESTRICTED  (blocked by default)

  Even if you set:
  management.endpoints.web.exposure.include=*

  --> * exposes all endpoints BUT /shutdown and /heapdump
      still remain RESTRICTED.
  --> You CANNOT hit them until you explicitly unrestrict them.
```

This is Spring Boot saying: **"I know you said expose everything — but these two are too dangerous. You need to explicitly accept the risk."**

---

## Step 5.4 — How to Unrestrict Them (If You Really Need To)

If you consciously decide you need these endpoints, you must **explicitly** unrestrict each one:

```properties
# application.properties

# Expose all endpoints
management.endpoints.web.exposure.include=*

# Explicitly accept the risk for heapdump
management.endpoint.heapdump.access=unrestricted

# Explicitly accept the risk for shutdown
management.endpoint.shutdown.access=unrestricted
```

Just setting `exposure.include=*` is NOT enough. You need BOTH steps:

```
To access /heapdump or /shutdown:
===================================

  Step 1: management.endpoints.web.exposure.include=*
          (or include them specifically)
          --> This makes them "visible" on the web layer

  Step 2: management.endpoint.heapdump.access=unrestricted
          management.endpoint.shutdown.access=unrestricted
          --> This actually UNBLOCKS them

  Both steps required. Either one alone = still blocked.
```

---

## Step 5.5 — The Full Picture

```
                  management.endpoints.web.exposure.include=*
                               |
                               v
        +----------------------------------------------+
        |         All Endpoints "Exposed"               |
        |                                               |
        |  /health   /metrics   /threaddump   /env ...  |
        |       --> UNRESTRICTED (accessible)           |
        |                                               |
        |  /shutdown   /heapdump                        |
        |       --> Still RESTRICTED                    |
        |       --> Spring Boot blocks them             |
        |       --> Returns 404 or blocked response     |
        +----------------------------------------------+
                               |
          Add to properties:   |
          heapdump.access=unrestricted
          shutdown.access=unrestricted
                               |
                               v
        +----------------------------------------------+
        |  /shutdown   /heapdump                        |
        |       --> NOW UNRESTRICTED                    |
        |       --> But MUST secure with auth!          |
        |       --> Otherwise anyone can hit them       |
        +----------------------------------------------+
```

---

## Step 5.6 — The Right Way to Handle These in Production

Even after unrestricting, you should **never leave them open** without authentication. The instructor's advice:

```
Production Best Practice:
==========================

  /heapdump  -->  unrestrict ONLY if you truly need it
                  + lock it behind ADMIN role auth
                  + ideally only accessible from internal network

  /shutdown  -->  think twice before unrestricting in prod
                  Most companies use a process manager
                  (systemd, Kubernetes, Docker) to stop apps
                  NOT the actuator endpoint
                  If you do use it --> ADMIN role + internal only

  General Rule:
  "With great power comes great responsibility"
  (the instructor literally says this in the lecture)
```

---

## Summary Table

```
Endpoint    Default State   Exposed by *?   Need extra property?
----------------------------------------------------------------
/health     UNRESTRICTED    YES             NO
/metrics    UNRESTRICTED    YES             NO
/threaddump UNRESTRICTED    YES             NO
/env        UNRESTRICTED    YES             NO
/beans      UNRESTRICTED    YES             NO
/mappings   UNRESTRICTED    YES             NO
/loggers    UNRESTRICTED    YES             NO
/heapdump   RESTRICTED      NO (blocked)    YES (access=unrestricted)
/shutdown   RESTRICTED      NO (blocked)    YES (access=unrestricted)
```

---

## Interview Tips

> **Q: Why are `/shutdown` and `/heapdump` restricted by default even when `exposure.include=*` is set?**
> Because they are critically dangerous. `/shutdown` can bring down your entire application with a single POST request. `/heapdump` can expose sensitive data like passwords, tokens, and user PII stored in JVM heap memory. Spring Boot deliberately requires you to explicitly unrestrict each one separately as a way of forcing you to consciously accept the risk involved.

> **Q: What two things do you need to do to access `/heapdump`?**
> First, include it in `management.endpoints.web.exposure.include` (or use `*`). Second, explicitly set `management.endpoint.heapdump.access=unrestricted`. Both are required — exposure alone is not sufficient.

> **Q: In production, how should you shut down a Spring Boot application?**
> Preferably through a process manager like systemd, Kubernetes, or Docker — not through the `/shutdown` actuator endpoint. If you do use `/shutdown`, it must be secured behind strong authentication and restricted to internal network access only.

---

Ready for **Step 6 — Security: Protecting all Actuator endpoints with Spring Security**?

# Spring Boot Actuator — Step 6: Securing Actuator Endpoints with Spring Security

---

## The Problem — All Endpoints are Public by Default

Here's the uncomfortable truth about everything we've set up so far:

```
Current State (no security):
==============================

  Your app is running on: http://your-server:8080

  Anyone on the internet who knows your URL can hit:

  GET  http://your-server:8080/manage/health
  GET  http://your-server:8080/manage/metrics
  GET  http://your-server:8080/manage/env        <-- passwords visible!
  GET  http://your-server:8080/manage/beans
  GET  http://your-server:8080/manage/threaddump
  POST http://your-server:8080/manage/shutdown   <-- kills your app!

  No username. No password. No token. Nothing.
  Completely open.
```

This is a serious security risk in production. The instructor specifically calls this out — these endpoints can **expose critical information** about your system to anyone who knows your URL and port.

---

## The Solution — Spring Security Filter Chain

We need to put a **security gate** in front of the actuator endpoints. Spring Security is the standard way to do this in Spring Boot. The instructor refers to the full Spring Security series (9 parts) for depth — here we focus on just securing actuator endpoints.

```
WITHOUT Security:                WITH Security:
==================               =================

  Request                          Request
     |                                |
     v                                v
  Actuator                     +-------------+
  Endpoint                     |  Security   |
  (open to all)                |  Filter     |
                               |  Chain      |
                               +-------------+
                                      |
                              Authenticated?
                              Role check OK?
                                /         \
                              YES          NO
                               |            |
                               v            v
                           Actuator     401 Unauthorized
                           Endpoint     or 403 Forbidden
```

---

## Step 6.1 — Adding Spring Security Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Step 6.2 — The Security Configuration

The instructor shows two levels of security config. Let's go through both:

### Level 1: Basic Setup — permit health & info, lock everything else

```java
// SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // health and info remain public
                // (monitoring tools need these without auth)
                .requestMatchers("/manage/health", "/manage/info")
                .permitAll()

                // everything else requires authentication
                .anyRequest()
                .authenticated()
        )
        // using basic auth for simplicity (testing only)
        // in production: use JWT or OAuth2
        .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### Level 2: Role-based Setup — only ADMIN can access actuator endpoints

```java
// SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // health and info are public
                .requestMatchers("/manage/health", "/manage/info")
                .permitAll()

                // all other actuator endpoints need ADMIN role
                .requestMatchers("/manage/**")
                .hasRole("ADMIN")

                // all other app endpoints just need authentication
                .anyRequest()
                .authenticated()
        )
        .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## Step 6.3 — application.properties (Adding Test Credentials)

For testing with basic auth, Spring Security lets you define a user directly in properties. The instructor uses this for the demo — **not for production**:

```properties
# application.properties

spring.application.name=order-service

# Actuator config
management.endpoints.web.base-path=/manage
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.endpoint.heapdump.access=unrestricted

# Test credentials (basic auth — testing only!)
# In production: use a proper UserDetailsService + DB
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=ADMIN
```

---

## Step 6.4 — What Happens Now?

```
Scenario 1: Hit /manage/health WITHOUT credentials
====================================================

  GET /manage/health
       |
       v
  Security Filter checks --> no credentials provided
       |
       v
  /manage/health is in permitAll() list
       |
       v
  200 OK --> { "status": "UP" }   (public, no auth needed)


Scenario 2: Hit /manage/env WITHOUT credentials
=================================================

  GET /manage/env
       |
       v
  Security Filter checks --> no credentials provided
       |
       v
  /manage/env is NOT in permitAll()
       |
       v
  401 Unauthorized  (blocked!)


Scenario 3: Hit /manage/env WITH credentials
=============================================

  GET /manage/env
  Authorization: Basic dXNlcjpwYXNz  (user:pass in base64)
       |
       v
  Security Filter checks --> credentials valid
       |
       v
  Role check --> user has ADMIN role
       |
       v
  200 OK --> full environment properties shown
```

---

## Step 6.5 — The Full Security Flow Diagram

```
Incoming Request to Actuator Endpoint
              |
              v
+------------------------------------------+
|         Spring Security Filter Chain      |
|                                           |
|  Step 1: Is this /manage/health           |
|          or /manage/info?                 |
|            YES --> permitAll()            |
|             |      --> Pass through       |
|            NO                             |
|             |                             |
|  Step 2: Does request have credentials?  |
|            NO  --> 401 Unauthorized       |
|            YES                            |
|             |                             |
|  Step 3: Are credentials valid?           |
|            NO  --> 401 Unauthorized       |
|            YES                            |
|             |                             |
|  Step 4: Does user have required role?    |
|            NO  --> 403 Forbidden          |
|            YES --> Pass through           |
+------------------------------------------+
              |
              v
      Actuator Endpoint
      (health, metrics,
       env, threaddump
       etc.)
```

---

## Step 6.6 — Special Case: Custom Endpoints with Write & Delete Operations

The instructor makes an important point here. If your **custom actuator endpoint** (which we'll cover in Step 7) has `@WriteOperation` (POST) or `@DeleteOperation` (DELETE), Spring Boot considers these especially critical — because they **change something**, not just read it.

```
Operation Type    HTTP Method    Risk Level    Auth Required?
--------------------------------------------------------------
@ReadOperation    GET            Low           Optional
                                               (but recommended)
@WriteOperation   POST           HIGH          YES - must secure
@DeleteOperation  DELETE         HIGH          YES - must secure
```

For custom endpoints with write/delete, make sure your security config covers them:

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/manage/health", "/manage/info")
    .permitAll()

    // Custom endpoint with write/delete MUST be authenticated
    .requestMatchers("/manage/my-custom-stats")
    .hasRole("ADMIN")

    .anyRequest()
    .authenticated()
)
// IMPORTANT: disable CSRF for actuator POST/DELETE calls
// because actuator clients (monitoring tools) don't send CSRF tokens
.csrf(csrf -> csrf.disable())
.httpBasic(Customizer.withDefaults());
```

---

## Step 6.7 — Production vs Testing Setup Comparison

```
TESTING (what instructor uses in demo):
========================================
  Auth type  : Basic Auth (username + password)
  Credentials: Hardcoded in application.properties
  Roles      : Single ADMIN user
  Purpose    : Quick demo only

  spring.security.user.name=user
  spring.security.user.password=pass
  spring.security.user.roles=ADMIN


PRODUCTION (what you should actually do):
==========================================
  Auth type  : JWT or OAuth2
  Credentials: Stored in DB, managed by UserDetailsService
  Roles      : Proper role hierarchy (ADMIN, OPS, DEV etc.)
  Network    : Actuator endpoints behind internal network / VPN
  Purpose    : Real security

  --> Check Spring Security series (9 parts) for full depth
```

---

## The Full application.properties So Far

```properties
# application.properties

spring.application.name=order-service

# Actuator base path
management.endpoints.web.base-path=/manage

# Expose all endpoints
management.endpoints.web.exposure.include=*

# Show health details
management.endpoint.health.show-details=always

# Unrestrict critical endpoints (only if needed)
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted

# Test credentials (replace with proper auth in production)
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=ADMIN
```

---

## Summary — Security Rules for Actuator

```
Endpoint          Recommended Access Policy
--------------------------------------------
/health           Public (monitoring tools need it)
/info             Public (general app info)
/metrics          Authenticated + OPS/ADMIN role
/env              Authenticated + ADMIN role only
                  (contains sensitive properties)
/threaddump       Authenticated + ADMIN role only
/heapdump         Authenticated + ADMIN role only
                  + unrestrict explicitly
/shutdown         Authenticated + ADMIN role only
                  + unrestrict explicitly
                  + ideally disabled in prod
/beans            Authenticated + ADMIN role only
/mappings         Authenticated + ADMIN role only
Custom endpoints
  @ReadOperation  Authenticated (minimum)
  @WriteOperation Authenticated + ADMIN role (must)
  @DeleteOperation Authenticated + ADMIN role (must)
```

---

## Interview Tips

> **Q: How do you secure Spring Boot Actuator endpoints?**
> Add `spring-boot-starter-security` dependency and configure a `SecurityFilterChain` bean. Use `requestMatchers` to permit `/health` and `/info` publicly, and require authentication (and ideally a specific role like ADMIN) for all other actuator endpoints under `/manage/**`.

> **Q: Why should `/health` and `/info` be kept public even with security enabled?**
> External monitoring tools and load balancers use `/health` to check if the service is up without needing credentials. If it's locked behind auth, automated health checks fail and the service might be incorrectly marked as down.

> **Q: Why do `@WriteOperation` and `@DeleteOperation` in custom actuator endpoints need special security attention?**
> Because they modify state (POST and DELETE HTTP methods). Unlike `@ReadOperation` which just reads data, write and delete operations can change your application's behaviour — so they must always be behind authentication and proper role checks.

---

Ready for **Step 7 — Custom Actuator Endpoints** (`@Endpoint`, `@ReadOperation`, `@WriteOperation`, `@DeleteOperation`)?

# Spring Boot Actuator — Step 7: Custom Actuator Endpoints

---

## Why Custom Endpoints?

The instructor makes a very important point here. The built-in endpoints cover generic concerns — memory, threads, health, etc. But what about **your application's specific operational needs**?

```
Built-in Endpoints cover generic concerns:
===========================================
  /health     --> is app up?
  /metrics    --> JVM, CPU, HTTP stats
  /threaddump --> thread state

What about YOUR app's specific needs?:
=======================================
  --> What is the current cache hit rate?
  --> How many orders are in the processing queue?
  --> What is the current feature flag state?
  --> Force a cache refresh without restarting?
  --> Remove a specific bad entry from cache?

None of these are available out of the box.
You need CUSTOM actuator endpoints for these.
```

Also — and this is a key insight from the instructor — **many Spring frameworks themselves use this exact mechanism internally**. For example, Spring Cloud Config's refresh scope exposes a `/refresh` endpoint through this same custom endpoint mechanism. So understanding this is not just about writing your own endpoints — it helps you understand how the entire Spring ecosystem works.

---

## Step 7.1 — The Building Blocks

Three things you need to know before writing any code:

```
Building Block 1: @Endpoint annotation
========================================
  Goes on the CLASS.
  The 'id' you give becomes the URL path.

  @Endpoint(id = "my-custom-stats")
  --> URL becomes: /actuator/my-custom-stats
                or /manage/my-custom-stats
                   (depending on your base-path)


Building Block 2: @Component
========================================
  The class must be a Spring bean.
  So annotate with @Component alongside @Endpoint.


Building Block 3: Operation annotations (on METHODS)
========================================================
  @ReadOperation   --> HTTP GET
  @WriteOperation  --> HTTP POST
  @DeleteOperation --> HTTP DELETE

  Return type can be anything serializable:
  String, int, Map, List, POJO — anything.
```

---

## Step 7.2 — The URL Structure with Selectors

Before writing code, understand how **selectors** work — because this is how you pass path variables to actuator endpoints:

```
Regular Spring MVC:           Actuator Custom Endpoint:
====================          ==========================
@GetMapping("/users/{id}")    @ReadOperation
public User getUser(          public User getUser(
    @PathVariable String id)      @Selector String id)


URL structure with selectors:
==============================

  No selector:
  /manage/my-custom-stats
  --> matches method with no @Selector params

  One selector:
  /manage/my-custom-stats/shrayansh
  --> matches method with one @Selector param

  Two selectors:
  /manage/my-custom-stats/shrayansh/hello
  --> matches method with two @Selector params
  --> selectors are POSITIONAL (order matters)

  Selector 1 = "shrayansh"  maps to --> String name
  Selector 2 = "hello"      maps to --> String message
```

---

## Step 7.3 — Writing the Custom Endpoint (Full Code)

### Part A: Read Operations (GET)

```java
// MyCustomStatsEndpoint.java

@Component
@Endpoint(id = "my-custom-stats")
// URL: /manage/my-custom-stats
public class MyCustomStatsEndpoint {

    // -------------------------------------------------
    // @ReadOperation (GET) — No selectors
    // URL: GET /manage/my-custom-stats
    // -------------------------------------------------
    @ReadOperation
    public String readAll() {
        // In real life: return your custom stats here
        // e.g. cache hit rate, queue size, feature flags
        return "Hello, Spring Boot!";
    }

    // -------------------------------------------------
    // @ReadOperation (GET) — Two selectors
    // URL: GET /manage/my-custom-stats/{name}/{message}
    // e.g. GET /manage/my-custom-stats/shrayansh/hello
    // -------------------------------------------------
    @ReadOperation
    public String read(
            @Selector String name,
            @Selector String message) {

        // selectors are positional:
        // first path segment  --> name
        // second path segment --> message
        return "Hello: " + name + " msg for you is: " + message;
    }
}
```

### Part B: Write & Delete Operations (POST & DELETE)

```java
// MyCustomStatsEndpoint.java (extended)

@Component
@Endpoint(id = "my-custom-stats")
public class MyCustomStatsEndpoint {

    // READ operations from Part A above...

    // -------------------------------------------------
    // @WriteOperation (POST) — No selectors
    // URL: POST /manage/my-custom-stats
    // Requires authentication!
    // -------------------------------------------------
    @WriteOperation
    public String refresh() {
        // In real life: trigger a cache refresh,
        // reload config, reset a counter, etc.
        return "refreshed";
    }

    // -------------------------------------------------
    // @DeleteOperation (DELETE) — One selector
    // URL: DELETE /manage/my-custom-stats/{key}
    // e.g. DELETE /manage/my-custom-stats/myKey
    // Requires authentication!
    // -------------------------------------------------
    @DeleteOperation
    public String remove(@Selector String key) {
        // In real life: remove specific cache entry,
        // delete a specific in-memory record, etc.
        return "reset done for key: " + key;
    }
}
```

---

## Step 7.4 — How Spring Matches a Request to a Method

This is the key logic you need to understand. When a request comes in, Spring looks at **three things** to pick the right method:

```
Matching Logic:
================

  Incoming request: POST /manage/my-custom-stats

  Spring checks:
    1. HTTP method = POST
       --> look for @WriteOperation methods

    2. Number of path segments after endpoint id = 0
       --> look for method with 0 @Selector params

    3. Match found: refresh()
       --> invoke it


  Incoming request: DELETE /manage/my-custom-stats/myKey

  Spring checks:
    1. HTTP method = DELETE
       --> look for @DeleteOperation methods

    2. Number of path segments after endpoint id = 1
       --> look for method with 1 @Selector param

    3. Match found: remove(@Selector String key)
       --> key = "myKey"
       --> invoke it


  Incoming request: GET /manage/my-custom-stats/shrayansh/hello

  Spring checks:
    1. HTTP method = GET
       --> look for @ReadOperation methods

    2. Number of path segments after endpoint id = 2
       --> look for method with 2 @Selector params

    3. Match found: read(@Selector String name,
                         @Selector String message)
       --> name = "shrayansh", message = "hello"
       --> invoke it
```

---

## Step 7.5 — Exposing Your Custom Endpoint

Just like built-in endpoints, your custom endpoint needs to be **exposed** in `application.properties`. Two options:

```properties
# Option 1: Expose everything (includes your custom endpoint)
management.endpoints.web.exposure.include=*

# Option 2: Expose selectively (list your custom endpoint explicitly)
management.endpoints.web.exposure.include=my-custom-stats,health,info
```

---

## Step 7.6 — Security for Custom Endpoints

As the instructor says — `@WriteOperation` and `@DeleteOperation` are critical. Here's the full security config for custom endpoints:

```java
// SecurityConfig.java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // health and info are public
                .requestMatchers("/manage/health", "/manage/info")
                .permitAll()

                // custom endpoint: GET is fine without auth
                // but POST and DELETE must be authenticated
                // easiest: just require auth for all actuator endpoints
                .anyRequest()
                .authenticated()
        )
        // IMPORTANT: must disable CSRF for POST/DELETE
        // actuator clients don't send CSRF tokens
        .csrf(csrf -> csrf.disable())
        .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

---

## Step 7.7 — The Full Picture: Request Flow for Custom Endpoints

```
Request: GET /manage/my-custom-stats
               |
               v
     Security Filter Chain
               |
         Authenticated?
          /          \
        NO             YES
         |               |
    401 Unauthorized    Role OK?
                        /      \
                      NO        YES
                       |          |
                  403 Forbidden   |
                                  v
                    @Endpoint(id="my-custom-stats")
                    MyCustomStatsEndpoint
                                  |
                         HTTP method = GET?
                         Selectors = 0?
                                  |
                                  v
                           readAll() invoked
                                  |
                                  v
                    Response: "Hello, Spring Boot!"


Request: POST /manage/my-custom-stats
               |
               v
     Security Filter Chain
               |
         Authenticated?   <-- MUST be (WriteOperation)
               |
               v
    @Endpoint(id="my-custom-stats")
               |
      HTTP method = POST?
      Selectors = 0?
               |
               v
       refresh() invoked
               |
               v
    Response: "refreshed"


Request: DELETE /manage/my-custom-stats/myKey
               |
               v
     Security Filter Chain
               |
         Authenticated?   <-- MUST be (DeleteOperation)
               |
               v
    @Endpoint(id="my-custom-stats")
               |
      HTTP method = DELETE?
      Selectors = 1?
               |
               v
    remove(@Selector String key)
    key = "myKey"
               |
               v
    Response: "reset done for key: myKey"
```

---

## Step 7.8 — Complete application.properties

```properties
# application.properties

spring.application.name=order-service

# Actuator base path
management.endpoints.web.base-path=/manage

# Expose custom endpoint + health + info
management.endpoints.web.exposure.include=my-custom-stats,health,info

# Show health details
management.endpoint.health.show-details=always

# Test credentials
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=ADMIN
```

---

## Step 7.9 — Real World Use Cases for Custom Endpoints

The instructor says this pattern is used **heavily** across the Spring ecosystem. Here are concrete examples:

```
Real World Custom Endpoint Examples:
=====================================

1. Cache Management
   GET    /manage/cache-stats    --> hit rate, miss rate, size
   POST   /manage/cache-stats    --> force full cache refresh
   DELETE /manage/cache-stats/{key} --> evict specific cache entry

2. Feature Flags
   GET    /manage/feature-flags  --> list all flags + state
   POST   /manage/feature-flags  --> toggle a flag on/off

3. Queue Monitoring
   GET    /manage/queue-stats    --> pending jobs, failed jobs count

4. Spring Cloud Config (framework itself uses this!)
   POST   /manage/refresh        --> reload config from config server
   --> This is a real built-in endpoint Spring Cloud adds
       using this exact @Endpoint mechanism

5. Circuit Breaker (Resilience4j)
   GET    /manage/circuitbreakers --> state of all circuit breakers
   --> Also exposed through this same mechanism
```

---

## Quick Reference — Everything in One Place

```
Annotation          Level    Maps to       URL Pattern
----------------------------------------------------------
@Endpoint(id="x")  Class    base URL      /manage/x
@Component         Class    Spring Bean   (auto-detected)
@ReadOperation     Method   HTTP GET      /manage/x
                                          /manage/x/{s1}
                                          /manage/x/{s1}/{s2}
@WriteOperation    Method   HTTP POST     /manage/x
                                          /manage/x/{s1}
@DeleteOperation   Method   HTTP DELETE   /manage/x/{s1}
@Selector          Param    Path segment  positional,
                                          order matters
```

---

## Interview Tips

> **Q: How do you create a custom actuator endpoint in Spring Boot?**
> Create a class annotated with both `@Component` and `@Endpoint(id="your-id")`. Inside, use `@ReadOperation` for GET, `@WriteOperation` for POST, and `@DeleteOperation` for DELETE methods. The `id` forms the URL path under the actuator base path.

> **Q: What are `@Selector` parameters in a custom actuator endpoint?**
> They are path variable equivalents for actuator endpoints. Each `@Selector` parameter maps to one additional path segment after the endpoint id, positionally in order. So two `@Selector` params means the URL has two extra segments after the endpoint id.

> **Q: Why do many Spring Cloud and Resilience4j features expose actuator endpoints?**
> Because actuator's custom endpoint mechanism provides a standard, secure, production-ready way to expose operational controls for any framework. Spring Cloud Config uses `/refresh`, Resilience4j exposes circuit breaker states — all through `@Endpoint`. It's the standard extension point for the entire Spring ecosystem.

> **Q: Why must `@WriteOperation` and `@DeleteOperation` always be secured?**
> Because they modify application state — equivalent to POST and DELETE HTTP methods. Unlike read operations which are safe, write and delete operations can change behaviour of a running system (e.g. clearing cache, resetting state), so they must always be behind authentication and role checks.

---

Ready for **Step 8 — Pushing Actuator Metrics to a Monitoring Platform (Datadog)**?

# Spring Boot Actuator — Step 8: Pushing Metrics to a Monitoring Platform (Datadog)

---

## The Problem — Endpoints are Useful, But Not Enough for Production

So far, everything we've done is **pull-based** — someone has to manually hit `/manage/metrics` to get data. In real production systems, that's not how monitoring works.

```
Pull-based (what we have so far):
===================================

  Engineer/Ops person
       |
       | manually hits
       v
  GET /manage/metrics/jvm.memory.used
       |
       v
  Gets current value

  Problems:
  - Manual effort
  - No history (what was memory usage 2 hours ago?)
  - No graphs or visualisation
  - No alerts (notify me when CPU > 80%)
  - Can't correlate across multiple services
  - You have to be watching to notice problems
```

What production teams actually need is **push-based** monitoring — your app automatically sends metrics to a dedicated platform, which stores history, draws graphs, and fires alerts:

```
Push-based (what we want):
============================

  Spring Boot App
  (with Actuator)
       |
       | pushes metrics every 5 seconds automatically
       v
  Monitoring Platform
  (Datadog / Prometheus / CloudWatch)
       |
       v
  +------------------+
  | Historical data  |  --> graphs over time
  | Dashboards       |  --> visualise everything
  | Alerts           |  --> notify on-call engineer
  | Anomaly detection|  --> catch problems early
  +------------------+
```

---

## Step 8.1 — The Big Picture: How it Works

The bridge between Spring Boot Actuator and any monitoring platform is a library called **Micrometer**. Think of Micrometer as a translation layer:

```
How Micrometer fits in:
========================

  Spring Boot Actuator
  (collects metrics internally)
         |
         | uses
         v
     Micrometer
  (vendor-neutral metrics facade)
         |
         | translates to platform-specific format
         v
  +------------+  +------------+  +-------------+
  |  Datadog   |  | Prometheus |  | CloudWatch  |
  | registry   |  | registry   |  | registry    |
  +------------+  +------------+  +-------------+
         |               |               |
         v               v               v
      Datadog        Prometheus       AWS CloudWatch
    (SaaS platform) (self-hosted)    (AWS native)


Key Point:
  Your app code doesn't change regardless of which
  monitoring platform you use.
  You just swap the Micrometer registry dependency.
```

---

## Step 8.2 — Setting Up Datadog Integration

The instructor uses Datadog with a free trial for the demo. Here's the full setup:

### Step 1: Get your Datadog API Key

```
In Datadog:
============
  Profile
    --> Organization Settings
        --> API Keys
            --> Create new key
                --> Copy it
```

### Step 2: Add the Micrometer Datadog Registry Dependency

```xml
<!-- pom.xml -->

<!-- existing actuator dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- ADD THIS: Micrometer registry for Datadog -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-datadog</artifactId>
</dependency>
```

### Step 3: Configure application.properties

```properties
# application.properties

spring.application.name=order-service

# Actuator config
management.endpoints.web.base-path=/manage
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

# -----------------------------------------------
# Datadog Integration
# -----------------------------------------------

# Your Datadog API key
# WARNING: Never hardcode in production!
# Use environment variables instead:
# management.datadog.metrics.export.api-key=${DATADOG_API_KEY}
management.datadog.metrics.export.api-key=e59af51cb6ae3a24129da7230e5dd9f7

# Enable pushing metrics to Datadog
# When true: Spring Boot starts pushing metrics automatically
management.datadog.metrics.export.enabled=true

# How frequently to push metrics to Datadog
# Every 5 seconds in this demo
# In production: 30s or 60s is more typical
management.datadog.metrics.export.step=5s
```

---

## Step 8.3 — What Happens After Setup

Once these three things are in place (dependency + API key + enabled=true), everything is automatic:

```
Application Startup:
=====================

  Spring Boot starts
       |
       v
  Micrometer registry initialises
       |
       v
  Detects: micrometer-registry-datadog on classpath
           + api-key configured
           + enabled = true
       |
       v
  Background thread starts
       |
       | every 5 seconds (as per step=5s)
       v
  Collects ALL actuator metrics:
    jvm.memory.used
    jvm.gc.pause
    http.server.requests
    system.cpu.usage
    jdbc.connections.active
    ... all of them
       |
       v
  Pushes to Datadog API
       |
       v
  Datadog stores it with timestamp
       |
       v
  Available in Datadog dashboard
  for graphs, alerts, analysis
```

---

## Step 8.4 — What it Looks Like in Datadog

```
Datadog Dashboard:
===================

  Metrics
    --> Summary
        --> Search: http.server.requests.count
              |
              v
        +--------------------------------+
        |  http.server.requests.count    |
        |                                |
        |   ^                            |
        |   |        /\                  |
        |   |       /  \    /\           |
        |   |      /    \  /  \          |
        |   |_____/      \/    \____     |
        |                               |
        +---+--+--+--+--+--+--+--+--+---+
           9am 10  11  12  1pm  2   3   |
                                        |
        --> Request count over time     |
        --> Spike at 11am visible       |
        --> Drops at lunch (12pm)       |
        +--------------------------------+

  You can:
  --> Set alert: if error rate > 5%, notify on-call
  --> Set alert: if jvm.memory.used > 80% of max, warn
  --> Build dashboards combining multiple metrics
  --> Compare metrics across multiple instances
```

---

## Step 8.5 — Production Best Practice: Never Hardcode API Keys

The instructor specifically warns about this. Here's the right way:

```
WRONG (what instructor does for demo only):
============================================
management.datadog.metrics.export.api-key=e59af51cb6ae3a24129da7230e5dd9f7


RIGHT (what you do in production):
====================================

Option 1: Environment Variable
--------------------------------
# In application.properties:
management.datadog.metrics.export.api-key=${DATADOG_API_KEY}

# Set env variable on your server / container:
export DATADOG_API_KEY=your_actual_key_here

# In Docker:
docker run -e DATADOG_API_KEY=your_key your-image

# In Kubernetes:
env:
  - name: DATADOG_API_KEY
    valueFrom:
      secretKeyRef:
        name: datadog-secret
        key: api-key


Option 2: Spring Cloud Config / Vault
--------------------------------------
# Store secret in HashiCorp Vault or Spring Cloud Config
# Spring Boot fetches it at startup
# Key never touches your codebase
```

---

## Step 8.6 — Switching to a Different Platform

This is where Micrometer's value really shows. If your company uses Prometheus instead of Datadog, you just swap the dependency and properties — your app code is untouched:

```
For Prometheus (self-hosted, very popular):
============================================

pom.xml:
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

application.properties:
# Prometheus PULLS from your app (scrapes)
# so you just expose the endpoint
management.endpoints.web.exposure.include=prometheus,health,info

# Prometheus then scrapes: GET /manage/prometheus
# at its own interval (configured in prometheus.yml)


For AWS CloudWatch:
====================

pom.xml:
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch2</artifactId>
</dependency>

application.properties:
management.cloudwatch.metrics.export.namespace=MyApp
management.cloudwatch.metrics.export.enabled=true
management.cloudwatch.metrics.export.step=1m
```

---

## Step 8.7 — The Full End-to-End Picture

```
+--------------------------------------------------------+
|                Spring Boot Application                 |
|                                                        |
|  Business Logic          Actuator Layer                |
|  (your REST APIs)        (monitoring)                  |
|       |                       |                        |
|       |              Micrometer Facade                 |
|       |                       |                        |
|       |          +------------+------------+           |
|       |          |            |            |           |
|       |       Datadog    Prometheus   CloudWatch       |
|       |       Registry   Registry    Registry          |
|       |          |            |            |           |
+-------|----------|------------|------------|-----------|+
        |          |            |            |
        v          v            v            v
   Your Users   Datadog    Prometheus    CloudWatch
   (frontend)  Platform   (self-hosted)  (AWS)
                  |            |            |
                  v            v            v
             Dashboards   Dashboards    Dashboards
             Alerts       Alerts        Alerts
             Graphs       Graphs        Graphs
```

---

## Complete Final application.properties

```properties
# application.properties — Complete Setup

spring.application.name=order-service

# ---- Actuator Base Config ----
management.endpoints.web.base-path=/manage
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

# ---- Critical Endpoints (only if needed) ----
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted

# ---- Security (test only — use proper auth in prod) ----
spring.security.user.name=user
spring.security.user.password=pass
spring.security.user.roles=ADMIN

# ---- Datadog Integration ----
# Use env variable in production: ${DATADOG_API_KEY}
management.datadog.metrics.export.api-key=${DATADOG_API_KEY}
management.datadog.metrics.export.enabled=true
management.datadog.metrics.export.step=5s
```

---

## Interview Tips

> **Q: What is Micrometer and why does Spring Boot use it?**
> Micrometer is a vendor-neutral metrics facade — like SLF4J but for metrics. Spring Boot Actuator uses Micrometer internally to collect metrics, and you swap in a platform-specific registry (Datadog, Prometheus, CloudWatch) to push those metrics to your chosen monitoring platform. Your application code never changes regardless of which platform you use.

> **Q: What is the difference between Datadog and Prometheus integration in Spring Boot?**
> Datadog is push-based — Spring Boot actively pushes metrics to Datadog at a configured interval (`step=5s`). Prometheus is pull-based (scrape-based) — Spring Boot exposes a `/prometheus` endpoint and Prometheus scrapes it at its own interval. Both use Micrometer registries, just different transport models.

> **Q: How would you avoid hardcoding the Datadog API key in application.properties?**
> Use environment variables with Spring's `${}` placeholder syntax: `management.datadog.metrics.export.api-key=${DATADOG_API_KEY}`. Set the actual value as an environment variable on your server, in Docker via `-e`, or in Kubernetes via a Secret. For more advanced setups, use HashiCorp Vault or Spring Cloud Config.

---

## Full Lecture Summary — Everything in One Diagram

```
Spring Boot Actuator — Complete Picture
=========================================

  1. Add dependency
     spring-boot-starter-actuator
           |
           v
  2. Configure application.properties
     base-path = /manage
     exposure  = *
           |
           v
  3. Built-in Endpoints
     /health    --> UP/DOWN + custom HealthIndicators
     /metrics   --> JVM, GC, threads, HTTP, JDBC
     /threaddump--> thread states + stack traces
     /env       --> all properties
     /beans     --> all Spring beans
     /mappings  --> all request mappings
     /shutdown  --> (restricted, unrestrict explicitly)
     /heapdump  --> (restricted, unrestrict explicitly)
           |
           v
  4. Secure with Spring Security
     /health, /info --> public
     everything else --> authenticated + ADMIN role
           |
           v
  5. Custom Endpoints
     @Endpoint(id="x") + @Component
     @ReadOperation   --> GET  /manage/x
     @WriteOperation  --> POST /manage/x   (needs auth)
     @DeleteOperation --> DELETE /manage/x (needs auth)
     @Selector        --> path variables
           |
           v
  6. Push to Monitoring Platform
     micrometer-registry-datadog (or prometheus etc.)
     api-key + enabled=true + step=5s
     --> auto-push all metrics to Datadog
     --> dashboards, alerts, graphs
```

---

That completes the full Spring Boot Actuator lecture. All 8 steps covered — setup, built-in endpoints, health indicators, metrics, critical endpoints, security, custom endpoints, and monitoring platform integration.