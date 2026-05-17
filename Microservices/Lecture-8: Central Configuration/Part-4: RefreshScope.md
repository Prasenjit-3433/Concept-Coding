---

# Step 1 — The Problem: Why Do We Need `@RefreshScope`?

---

## The Setup (What We Already Know)

Before understanding `@RefreshScope`, you need to know how **Centralized Configuration** works in a Spring microservices setup. Here's the full picture:

```
┌─────────────────────────────────────────────────────────────────┐
│                        GIT REPOSITORY                           │
│              (Central place for all config files)               │
│                                                                 │
│   order-service-dev.properties                                  │
│   custom.message=Hello from order dev!                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           │  Config Server fetches from Git
                           │  *** ONLY AT STARTUP ***
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                      CONFIG SERVER                               │
│                     (Port: 8888)                                 │
│                                                                  │
│   Reads config files from Git repo and serves them               │
└──────┬─────────────────────────────────┬─────────────────────────┘
       │                                 │
       │  Each service fetches           │  fetches at startup
       │  from config server             │  *** ONLY AT STARTUP ***
       │                                 │
       ▼                                 ▼
┌─────────────┐                   ┌─────────────┐
│   Order     │                   │   Payment   │  ...and so on
│   Service   │                   │   Service   │
│  Port:8080  │                   │             │
└─────────────┘                   └─────────────┘
```

So at startup:
- Config Server reads properties from the Git repo and caches them.
- Each microservice (like Order Service) reads properties from the Config Server and stores them in memory.

---

## The Problem

Now imagine this real scenario:

> The Order Service is **already running**. Someone goes and **changes** `custom.message` in the Git repo from `"Hello from order dev!"` to `"Hello from order dev - this is updated!"`.

What happens when you call the Order API now?

```
┌──────────────────────────────────────────────────────────────────┐
│   YOU CALL:  GET localhost:8080/orders/fetch                     │
│                                                                  │
│   RESPONSE:  Hello from order dev!    ← STILL THE OLD VALUE!     │
│                                                                  │
│   Even though Git repo has the new value, the running            │
│   application has NO idea that something changed.                │
└──────────────────────────────────────────────────────────────────┘
```

**Why does this happen?**

Because both the Config Server and the Order Service only fetch configurations **once — at application startup**. After that, the value is saved in memory and never updated automatically.

```
TIMELINE:

App Starts          Git Repo Updated        You call the API
    │                      │                      │
    ▼                      ▼                      ▼
[Fetches config]   [New value in Git]     [Reads OLD value
[Saves in memory]  [Nobody is notified]    from memory]
```

---

## The Naive Solution (and why it's bad)

The first thought is: **"Just restart the service every time config changes."**

But think about this in production:
- You may have **100s of microservices** all reading from the same central config.
- Restarting each one manually is slow, error-prone, and causes **downtime**.
- If someone changes a single property, you'd need to restart ALL affected services.

This is simply **not acceptable** in a real-world distributed system.

---

## The Real Solution: `@RefreshScope`

The answer is **NO**, you don't need to restart. Spring Cloud gives us `@RefreshScope` to handle exactly this — allowing specific beans to **dynamically reload their configuration** at runtime, without any restart.

That's what we'll dig into from Step 2 onwards.

---

Ready for **Step 2 — What is `@RefreshScope` and how does it work internally**?

---

# Step 2 — What is `@RefreshScope` and How Does It Work Internally?

---

## What Does `@RefreshScope` Actually Do?

In simple terms, `@RefreshScope` is a Spring annotation that you put on a **bean** to tell Spring:

> "Hey, this bean is allowed to be **dynamically refreshed** at runtime. Whenever a refresh event happens, destroy the old bean and create a brand new one with the latest property values."

So it does exactly **two things** when a refresh happens:

```
┌────────────────────────────────────────────────────────────────┐
│                  WHAT @RefreshScope DOES                       │
│                                                                │
│   STEP 1:  Destroy the OLD bean                                │
│            (the one holding the outdated property value)       │
│                                                                │
│   STEP 2:  Create a NEW bean                                   │
│            (freshly initialized with the latest config value)  │
└────────────────────────────────────────────────────────────────┘
```

That's it. It doesn't do any polling. It doesn't watch the Git repo. It just **marks** the bean as "eligible for dynamic refresh." The actual refresh has to be **triggered** — we'll cover that in Step 3.

---

## The Full Internal Picture

Let's now see how this all fits together end to end:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          GIT REPOSITORY                                 │
│                  custom.message=Hello from order dev!                   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                    (1) Fetches at startup
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          CONFIG SERVER                                  │
│                           Port: 8888                                    │
│                                                                         │
│   Holds properties fetched from Git.                                    │
│   Serves them to any microservice that asks.                            │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                    (2) Fetches at startup
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE                                   │
│                           Port: 8080                                    │
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │              OrderProperties Bean                               │   │
│   │                                                                 │   │
│   │   @Component                                                    │   │
│   │   @ConfigurationProperties(prefix = "custom")                   │   │
│   │   @RefreshScope        ← MARKED FOR DYNAMIC REFRESH             │   │
│   │   public class OrderProperties {                                │   │
│   │       private String message; // = "Hello from order dev!"      │   │
│   │   }                                                             │   │
│   └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│   This bean holds the property value in memory.                         │
│   Because it is marked @RefreshScope, Spring knows:                     │
│   "When a refresh event fires, destroy and recreate this bean."         │
└─────────────────────────────────────────────────────────────────────────┘


          NOW SOMEONE UPDATES Git Repo:
          custom.message = "Hello from order dev - UPDATED!"


          ┌─────────────────────────────────┐
          │   Refresh Event is triggered    │  ← We'll see HOW in Step 3
          └────────────────┬────────────────┘
                           │
                           ▼
          ┌─────────────────────────────────────────────────┐
          │   Spring looks for all beans marked             │
          │   with @RefreshScope inside Order Service       │
          │                                                 │
          │   Found: OrderProperties bean                   │
          └────────────────┬────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
  ┌───────────────────┐     ┌───────────────────────────────┐
  │  OLD bean         │     │  NEW bean                     │
  │  DESTROYED        │     │  CREATED                      │
  │                   │     │                               │
  │  message =        │     │  message =                    │
  │  "Hello from      │     │  "Hello from order dev        │
  │   order dev!"     │     │   - UPDATED!"                 │
  └───────────────────┘     └───────────────────────────────┘
```

---

## One Very Important Clarification

`@RefreshScope` by itself does **nothing** until a **refresh event** is fired. Think of it like this:

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   @RefreshScope  =  just a label / marker on the bean      │
│                                                            │
│   It says: "I am eligible to be refreshed"                 │
│                                                            │
│   But the actual refresh only happens when                 │
│   someone TRIGGERS a refresh event.                        │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

So there are **two separate concerns**:

| Concern | Answer |
|---|---|
| Which bean should be refreshed? | Mark it with `@RefreshScope` |
| When should it be refreshed? | A refresh event must be triggered (via Actuator or Spring Cloud Bus) |

---

## Where Should You Put `@RefreshScope`?

At this point, a natural question is: which class do I put this annotation on?

The instructor is very deliberate here — he uses it on the `OrderProperties` class (a `@ConfigurationProperties` POJO), **not** on the controller or service. The full reason for this is a critical point that we'll cover in detail in **Step 5**. For now, just note that he intentionally avoids putting it on the controller.

The `OrderProperties` class is a simple POJO — it has no logic, no state, just configuration fields:

```java
@Component
@ConfigurationProperties(prefix = "custom")
@RefreshScope
public class OrderProperties {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

When `custom.message` in the Git repo changes and a refresh is triggered, Spring will destroy this bean and recreate it — this time picking up `"Hello from order dev - UPDATED!"` as the value of `message`.

---

Ready for **Step 3 — The Trigger: How the refresh event occurs via `/actuator/refresh`**?

---

# Step 3 — The Trigger: How the Refresh Event Occurs via `/actuator/refresh`

---

## The Core Question

We now know that `@RefreshScope` just **marks** a bean as eligible for refresh. But how does the actual refresh event get fired? Who tells Spring: "Hey, go fetch the latest config and recreate the marked beans"?

The answer (at least for the approach covered in this lecture) is: **Spring Boot Actuator** — specifically, a custom actuator endpoint that Spring Cloud Config provides out of the box:

```
POST /actuator/refresh
```

---

## Quick Background: What is a Custom Actuator Endpoint?

The instructor references the Actuator video here. The key idea is:

> Spring Boot Actuator lets you create **custom endpoints** using `@Endpoint` annotation. Spring Cloud Config uses exactly this feature to expose `/actuator/refresh`.

Spring Cloud Config has already written this for you internally. Here's the actual framework code the instructor shows:

```java
/*
 * Id: forms the URL path: /actuator/{id}
 * So @Endpoint(id = "refresh") → becomes → /actuator/refresh
 */
@Endpoint(id = "refresh")
public class RefreshEndpoint {

    private final ContextRefresher contextRefresher;

    public RefreshEndpoint(ContextRefresher contextRefresher) {
        this.contextRefresher = contextRefresher;
    }

    @WriteOperation  // @WriteOperation = POST request
    public Collection<String> refresh() {
        /*
         * 1. If its Config Server:
         *    → Fetches latest properties from Git (CentralConfigs repo)
         *
         * 2. If its Config Client (like Order Service):
         *    → First calls Config Server to get latest properties
         *    → Then finds all @RefreshScope marked beans
         *    → Destroys old beans and recreates them with fresh values
         */
        Set<String> keys = this.contextRefresher.refresh();
        return keys;
    }
}
```

You don't write this code. Spring Cloud Config ships it. All you do is **add the actuator dependency** and **expose the refresh endpoint** in your `application.properties`.

---

## How `/actuator/refresh` Behaves Differently Based on WHERE You Call It

This is a subtle but very important point the instructor makes:

```
┌──────────────────────────────────────────────────────────────────────┐
│          BEHAVIOR OF POST /actuator/refresh                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  If called on CONFIG SERVER (port 8888):                             │
│  → Goes to Git repo                                                  │
│  → Fetches the latest property values                                │
│  → Config Server is now up to date                                   │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  If called on ORDER SERVICE (port 8080):                             │
│  → First calls Config Server to get latest properties                │
│  → Then scans for all beans marked with @RefreshScope                │
│  → Destroys old beans + recreates them with fresh property values    │
│  → Returns a list of property keys that were refreshed               │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## The Full Refresh Flow Step by Step

Here is the complete picture of what happens when a config property is changed and you want it reflected in the running Order Service — **without restarting anything**:

```
  GIT REPO                CONFIG SERVER                    ORDER SERVICE
  (updated)                (port 8888)                      (port 8080)
     │                          │                                │
     │  custom.message          │                                │
     │  is now updated          │                                │
     │                          │                                │
     │                          │                                │
     │         STEP 1: POST /actuator/refresh                    │
     │◄─────────────────────────┤                                │
     │                          │                                │
     │  Config Server fetches   │                                │
     │  latest values from Git  │                                │
     │─────────────────────────►│                                │
     │                          │                                │
     │                          │  Config Server is now          │
     │                          │  holding latest values         │
     │                          │                                │
     │                          │   STEP 2: POST                 │
     │                          │   /actuator/refresh            │
     │                          │◄───────────────────────────────┤
     │                          │                                │
     │                          │  Order Service calls           │
     │                          │  Config Server to get          │
     │                          │  latest properties             │
     │                          │───────────────────────────────►│
     │                          │                                │
     │                          │                                │  Scans for
     │                          │                                │  @RefreshScope
     │                          │                                │  beans
     │                          │                                │
     │                          │                                │  Destroys OLD
     │                          │                                │  OrderProperties
     │                          │                                │  bean
     │                          │                                │
     │                          │                                │  Creates NEW
     │                          │                                │  OrderProperties
     │                          │                                │  bean with
     │                          │                                │  latest value
     │                          │                                │
     │                          │  Returns: ["custom.message"] ◄─┤
     │                          │  (list of refreshed keys)      │
```

Response from `POST localhost:8080/actuator/refresh`:
```json
[
  "config.client.version",
  "custom.message"
]
```
This tells you exactly **which properties** were updated during the refresh.

---

## Why is `/actuator/refresh` a POST and not a GET?

Because internally it uses `@WriteOperation`. In Spring Actuator:

| Actuator Annotation | Maps to HTTP Method |
|---|---|
| `@ReadOperation` | GET |
| `@WriteOperation` | POST |
| `@DeleteOperation` | DELETE |

Since refresh is a **state-changing action** (destroying and recreating beans), it is a POST operation. You must call it with POST, not GET.

---

## Summary of This Step

```
┌──────────────────────────────────────────────────────────────────┐
│                     RECAP OF STEP 3                              │
│                                                                  │
│  1. Spring Cloud Config ships a built-in custom actuator         │
│     endpoint: POST /actuator/refresh                             │
│                                                                  │
│  2. You don't write this endpoint — it's provided by the         │
│     framework. You just expose it in application.properties.     │
│                                                                  │
│  3. When called on Config Server → fetches latest from Git       │
│                                                                  │
│  4. When called on Order Service →                               │
│     a. Calls Config Server for latest properties                 │
│     b. Finds all @RefreshScope beans                             │
│     c. Destroys + recreates them with fresh values               │
│                                                                  │
│  5. It's a POST request because it's a @WriteOperation           │
│                                                                  │
│  6. Returns a list of property keys that were refreshed          │
└──────────────────────────────────────────────────────────────────┘
```

---

---

# Step 4 — Full Implementation: Config Server + Order Service

---

## The Complete Project Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                    WHAT WE ARE BUILDING                         │
│                                                                 │
│   ┌─────────────────┐          ┌─────────────────────────┐      │
│   │   Git Repo      │          │     Config Server       │      │
│   │                 │          │     Port: 8888          │      │
│   │  order-service  │◄─────────│                         │      │
│   │  -dev.properties│  fetches │  spring-cloud-config    │      │
│   │                 │          │  -server                │      │
│   │  custom.message │          │  spring-boot-actuator   │      │
│   │  =Hello from    │          │                         │      │
│   │   order dev!    │          └────────────┬────────────┘      │
│   └─────────────────┘                       │                   │
│                                             │ serves config     │
│                                             ▼                   │
│                                ┌────────────────────────┐       │
│                                │     Order Service      │       │
│                                │     Port: 8080         │       │
│                                │                        │       │
│                                │  spring-cloud-starter  │       │
│                                │  -config               │       │
│                                │  spring-boot-actuator  │       │
│                                │                        │       │
│                                │  OrderProperties       │       │
│                                │  @RefreshScope ✓       │       │
│                                │                        │       │
│                                │  OrderController       │       │
│                                │  @RefreshScope ✗       │       │
│                                └────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Part 1: Git Repo — Central Config File

This is the file sitting in your Git repository. This is the file whose changes we want reflected in the running Order Service without restart.

```
File: orderservice/order-service-dev.properties
```

```properties
custom.message=Hello from order dev!
```

---

## Part 2: Config Server

### pom.xml
Two dependencies needed — the config server itself, and actuator:

```xml
<!-- Config Server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>

<!-- Actuator — needed to expose /actuator/refresh -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Main Application Class

```java
@SpringBootApplication
@EnableConfigServer          // ← tells Spring: this app is a Config Server
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

### application.properties

```properties
# Config Server runs on port 8888
server.port=8888

# Point to your Git repo where all config files live
spring.cloud.config.server.git.uri=https://gitlab.com/shrayansh8/centralconfigs
spring.cloud.config.server.git.username=${GIT_USERNAME}
spring.cloud.config.server.git.password=${GIT_ACCESS_TOKEN}

# Which folders inside the repo to look into
spring.cloud.config.server.git.search-paths=global, orderservice

# Clone the repo at startup (don't wait for first request)
spring.cloud.config.server.git.clone-on-start=true

# Which branch to use
spring.cloud.config.server.git.default-label=refreshscope_test

# IMPORTANT: expose the refresh endpoint so we can call POST /actuator/refresh
management.endpoints.web.exposure.include=refresh,health
```

---

## Part 3: Order Service (Config Client)

### pom.xml
Two dependencies needed — the config client, and actuator:

```xml
<!-- Config Client — to fetch properties from Config Server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<!-- Actuator — needed to expose /actuator/refresh here too -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### application.properties

```properties
spring.application.name=order-service

# Where to find the Config Server
spring.config.import=optional:configserver:http://localhost:8888

# Active profile — will fetch order-service-dev.properties from Git
spring.profiles.active=dev

# Local fallback value (used if Config Server is unreachable)
custom.message=Hello from local default!

# IMPORTANT: expose the refresh endpoint on Order Service too
management.endpoints.web.exposure.include=health,refresh
```

### OrderProperties.java — The Bean Marked with `@RefreshScope`

This is the key class. It is a simple POJO that maps the `custom.*` properties from the config file. This is where `@RefreshScope` lives:

```java
@Component
@ConfigurationProperties(prefix = "custom")  // maps custom.message → message field
@RefreshScope                                  // ← marks this bean for dynamic refresh
public class OrderProperties {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

```
┌────────────────────────────────────────────────────────────┐
│              HOW THE MAPPING WORKS                         │
│                                                            │
│   In Git repo:                                             │
│   custom.message = Hello from order dev!                   │
│       │                                                    │
│       │  prefix = "custom"                                 │
│       │  field  = "message"                                │
│       ▼                                                    │
│   OrderProperties.message = "Hello from order dev!"        │
└────────────────────────────────────────────────────────────┘
```

### OrderController.java — NO `@RefreshScope` Here

The controller just uses `OrderProperties` to read the config value. Notice there is NO `@RefreshScope` on the controller — the instructor is very deliberate about this (full explanation in Step 5):

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private int counter = 0;   // tracks how many times endpoint was hit
                               // (used to demonstrate bean state — explained in Step 5)

    @Autowired
    OrderProperties orderProperties;   // inject the @RefreshScope bean

    @GetMapping("/fetch")
    public String getOrders() {
        counter++;
        return "fetched orders and message: " + orderProperties.getMessage()
                + " and counter value is: " + counter;
    }
}
```

---

## Part 4: The Complete Working Flow with All Pieces Together

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        COMPLETE FLOW                                    │
│                                                                         │
│  STEP 1: Start Config Server (port 8888)                                │
│          → Clones Git repo at startup                                   │
│          → Holds: custom.message = "Hello from order dev!"              │
│                                                                         │
│  STEP 2: Start Order Service (port 8080)                                │
│          → Fetches from Config Server at startup                        │
│          → OrderProperties.message = "Hello from order dev!"            │
│                                                                         │
│  STEP 3: Call GET localhost:8080/orders/fetch                           │
│          → Response: "fetched orders and message:                       │
│                        Hello from order dev! and counter value is: 1"   │
│                                                                         │
│  STEP 4: Update Git repo                                                │
│          custom.message = "Hello from order dev - this is updated!"     │
│                                                                         │
│  STEP 5: Call GET localhost:8080/orders/fetch again                     │
│          → Response: STILL "Hello from order dev!"  ← old value!        │
│          (refresh not triggered yet)                                    │
│                                                                         │
│  STEP 6: POST localhost:8888/actuator/refresh  (on Config Server)       │
│          → Config Server fetches latest from Git                        │
│          → Config Server now has the updated value                      │
│                                                                         │
│  STEP 7: POST localhost:8080/actuator/refresh  (on Order Service)       │
│          → Order Service calls Config Server                            │
│          → Gets latest value                                            │
│          → Finds OrderProperties bean (marked @RefreshScope)            │
│          → Destroys old bean                                            │
│          → Creates new bean with updated value                          │
│          → Returns: ["custom.message"]                                  │
│                                                                         │
│  STEP 8: Call GET localhost:8080/orders/fetch again                     │
│          → Response: "fetched orders and message:                       │
│             Hello from order dev - this is updated!                     │
│             and counter value is: 3"                                    │
│          ✓ Updated WITHOUT restarting anything!                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference: What Goes Where

```
┌────────────────────┬───────────────────────────────────────────────────┐
│   WHERE            │   WHAT TO ADD                                     │
├────────────────────┼───────────────────────────────────────────────────┤
│ Config Server      │ spring-cloud-config-server dependency             │
│ pom.xml            │ spring-boot-starter-actuator dependency           │
├────────────────────┼───────────────────────────────────────────────────┤
│ Config Server      │ management.endpoints.web.exposure.include=        │
│ application        │ refresh,health                                    │
│ .properties        │                                                   │
├────────────────────┼───────────────────────────────────────────────────┤
│ Order Service      │ spring-cloud-starter-config dependency            │
│ pom.xml            │ spring-boot-starter-actuator dependency           │
├────────────────────┼───────────────────────────────────────────────────┤
│ Order Service      │ management.endpoints.web.exposure.include=        │
│ application        │ health,refresh                                    │
│ .properties        │                                                   │
├────────────────────┼───────────────────────────────────────────────────┤
│ OrderProperties    │ @RefreshScope ✓                                   │
│ .java              │ @ConfigurationProperties ✓                        │
├────────────────────┼───────────────────────────────────────────────────┤
│ OrderController    │ @RefreshScope ✗ — NEVER put it here               │
│ .java              │                                                   │
└────────────────────┴───────────────────────────────────────────────────┘
```

---

Ready for **Step 5 — The Big Gotcha: Why NEVER put `@RefreshScope` on a Controller or Service, and why `@ConfigurationProperties` is the right place?**

---

# Step 5 — The Big Gotcha: Why NEVER Put `@RefreshScope` on a Controller or Service?

---

## First, Understanding the Root Cause

Remember what `@RefreshScope` does when a refresh event fires:

```
┌────────────────────────────────────────────────────────────────┐
│              WHAT HAPPENS DURING REFRESH                       │
│                                                                │
│   1. OLD bean is DESTROYED                                     │
│      → Everything on that object is gone                       │
│      → All instance variables reset                            │
│      → All in-memory state lost                                │
│                                                                │
│   2. NEW bean is CREATED                                       │
│      → Fresh object, initialized from scratch                  │
│      → Picks up latest property values                         │
└────────────────────────────────────────────────────────────────┘
```

The critical word here is: **object state is lost.**

For a simple config POJO like `OrderProperties`, this is completely fine — it only holds property values, nothing else. But for a Controller or Service, this can be **catastrophic**.

---

## Demonstrating the Problem with a Counter

The instructor uses a `counter` variable inside `OrderController` to demonstrate exactly what goes wrong. Let's walk through it carefully.

### The Dangerous Setup (What NOT to do):

```java
@RestController
@RequestMapping("/orders")
@RefreshScope                    // ← WRONG: never do this on a controller
public class OrderController {

    private int counter = 0;     // ← this is object state

    @Value("${custom.message}")  // ← reading property directly here
    String message;

    @GetMapping("/fetch")
    public String getOrders() {
        counter++;
        return "fetched orders and message: " + message
                + " and counter value is: " + counter;
    }
}
```

### What Happens Step by Step:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TIMELINE OF THE PROBLEM                          │
│                                                                     │
│  App starts                                                         │
│  → OrderController bean created                                     │
│  → counter = 0                                                      │
│  → message = "Hello from order dev!"                                │
│                                                                     │
│  Call 1: GET /orders/fetch                                          │
│  → counter becomes 1                                                │
│  → Response: "Hello from order dev! and counter value is: 1"        │
│                                                                     │
│  Call 2: GET /orders/fetch                                          │
│  → counter becomes 2                                                │
│  → Response: "Hello from order dev! and counter value is: 2"        │
│                                                                     │
│  ────────────────────────────────────────────────────────────────   │
│  Someone calls POST /actuator/refresh on Order Service              │
│  (even without changing any config value)                           │
│                                                                     │
│  → Spring finds OrderController is marked @RefreshScope             │
│  → DESTROYS the old OrderController bean  ← counter = 2 is GONE     │
│  → CREATES a brand new OrderController bean                         │
│  → counter resets back to 0                                         │
│  ────────────────────────────────────────────────────────────────   │
│                                                                     │
│  Call 3: GET /orders/fetch                                          │
│  → counter becomes 1 again (reset to 0, then incremented)           │
│  → Response: "Hello from order dev! and counter value is: 1"        │
│                                                                     │
│  Expected counter value was 3, but we got 1!                        │
│  The object state was silently destroyed!                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## The Visual Proof

```
FIRST INVOCATION:              SECOND INVOCATION:
counter = 1                    counter = 2
┌─────────────────┐            ┌─────────────────┐
│ OrderController │            │ OrderController │
│ counter = 1     │            │ counter = 2     │
│ SAME BEAN       │            │ SAME BEAN       │
│ (singleton)     │            │ (singleton)     │
└─────────────────┘            └─────────────────┘

       │
       │  POST /actuator/refresh is called
       ▼

OLD BEAN DESTROYED:            NEW BEAN CREATED:
┌─────────────────┐            ┌─────────────────┐
│ OrderController │            │ OrderController │
│ counter = 2     │  DELETED   │ counter = 0     │
│                 │───────────►│ (fresh object)  │
└─────────────────┘            └─────────────────┘

THIRD INVOCATION:
counter = 1    ← should have been 3!
┌─────────────────┐
│ OrderController │
│ counter = 1     │
│ NEW BEAN        │
└─────────────────┘
```

---

## An Even More Dangerous Problem in Production

The counter issue is just for demonstration. The real danger the instructor points out is far worse:

```
┌─────────────────────────────────────────────────────────────────────┐
│                    REAL PRODUCTION DANGER                           │
│                                                                     │
│  Imagine:                                                           │
│  → A user sends a request to OrderController                        │
│  → The request is being processed (mid-flight)                      │
│  → At the SAME TIME, someone calls /actuator/refresh                │
│                                                                     │
│  What happens?                                                      │
│  → Spring DESTROYS the OrderController bean                         │
│  → The in-flight request is dropped / fails                         │
│  → User gets an error                                               │
│                                                                     │
│  This is completely unacceptable in production!                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Why `@ConfigurationProperties` is the Right Place

Now you understand the danger. So the fix is: **only put `@RefreshScope` on stateless beans** — beans that hold no object state, no instance variables that change over time, no work in progress.

`@ConfigurationProperties` classes are perfect for this because:

```
┌─────────────────────────────────────────────────────────────────────┐
│              WHY @ConfigurationProperties IS SAFE                   │
│                                                                     │
│  It is a pure POJO                                                  │
│  → Only holds configuration property fields                         │
│  → No logic, no counters, no in-flight work                         │
│  → No object state that matters                                     │
│                                                                     │
│  When it is destroyed and recreated:                                │
│  → Nothing important is lost                                        │
│  → The new bean just has the latest property values                 │
│  → Perfectly safe                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### The Safe Setup (What the instructor recommends):

```java
// ✓ SAFE — stateless POJO, only holds config properties
@Component
@ConfigurationProperties(prefix = "custom")
@RefreshScope                               // ← correct place for @RefreshScope
public class OrderProperties {

    private String message;                 // only config value, no object state

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

```java
// ✓ SAFE — no @RefreshScope here, no state loss risk
@RestController
@RequestMapping("/orders")
public class OrderController {

    private int counter = 0;               // object state — safe because no @RefreshScope

    @Autowired
    OrderProperties orderProperties;       // inject the safe @RefreshScope bean

    @GetMapping("/fetch")
    public String getOrders() {
        counter++;
        return "fetched orders and message: " + orderProperties.getMessage()
                + " and counter value is: " + counter;
    }
}
```

### What Happens Now During Refresh:

```
POST /actuator/refresh is called
         │
         ▼
Spring looks for @RefreshScope beans
         │
         ▼
Finds: OrderProperties  ← stateless, safe to destroy and recreate
Does NOT touch: OrderController ← no @RefreshScope, left alone
         │
         ▼
OrderProperties OLD bean destroyed
OrderProperties NEW bean created with latest config value
         │
         ▼
OrderController still running, counter still intact
Next call to /orders/fetch returns updated message ✓
```

---

## Side by Side Comparison

```
┌────────────────────────────┬──────────────────────────────────────────┐
│  @RefreshScope on          │  @RefreshScope on                        │
│  Controller / Service      │  @ConfigurationProperties POJO           │
├────────────────────────────┼──────────────────────────────────────────┤
│  Object state is lost      │  No meaningful state to lose             │
│  on every refresh          │                                          │
├────────────────────────────┼──────────────────────────────────────────┤
│  In-flight requests can    │  No requests handled here,               │
│  be dropped                │  no risk of dropping requests            │
├────────────────────────────┼──────────────────────────────────────────┤
│  Unpredictable behavior    │  Predictable, clean refresh              │
│  in production             │  every single time                       │
├────────────────────────────┼──────────────────────────────────────────┤
│  ✗ NEVER do this           │  ✓ Always do this                        │
└────────────────────────────┴──────────────────────────────────────────┘
```

---

## 🎯 Interview Tip (Instructor explicitly flags this)

> **"This is important in interviews also — never use `@RefreshScope` over a Controller or Service. Always use it over a stateless object or stateless bean, like a `@ConfigurationProperties` POJO which only contains configuration. It's stateless — just properties, fields and values."**

If asked in an interview:

**Q: Where should you use `@RefreshScope` and where should you not?**

**A:** `@RefreshScope` should only be used on **stateless beans** — beans that hold no object state, no in-flight work, no instance variables that change over time. The best candidate is a `@ConfigurationProperties` POJO because it only holds configuration values. You should **never** put `@RefreshScope` on a Controller or Service because:
1. Every refresh destroys and recreates the bean, wiping all object state.
2. In-flight requests being processed by that Controller can be dropped mid-execution.
3. This causes unpredictable and dangerous behavior in production.

---

Ready for **Step 6 — The Limitation: The 100 Microservices Problem and the teaser for Spring Cloud Bus**?

---

# Step 6 — The Limitation: The 100 Microservices Problem & Spring Cloud Bus Teaser

---

## Recap of What We Have So Far

At this point, our refresh mechanism works perfectly for a single microservice. The flow is:

```
Update Git Repo
      │
      ▼
POST /actuator/refresh → Config Server (port 8888)
      │
      ▼
POST /actuator/refresh → Order Service (port 8080)
      │
      ▼
OrderProperties bean refreshed ✓
```

This works great. But now let's think about **real-world scale**.

---

## The Real-World Problem

In production, you don't have just one microservice. You have **hundreds** of them — all reading from the same central config:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GIT REPOSITORY                                  │
│                    (Central Config Files)                               │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         CONFIG SERVER                                   │
│                           Port: 8888                                    │
└──────┬──────────────┬──────────────┬──────────────┬─────────────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│  Order   │   │ Payment  │   │   User   │   │Inventory │  ... 100s more
│ Service  │   │ Service  │   │ Service  │   │ Service  │
│  :8080   │   │  :8081   │   │  :8082   │   │  :8083   │
└──────────┘   └──────────┘   └──────────┘   └──────────┘
```

Now someone updates a property in the Git repo. To refresh all services using the **actuator approach**, you would have to:

```
POST localhost:8888/actuator/refresh   → Config Server
POST localhost:8080/actuator/refresh   → Order Service
POST localhost:8081/actuator/refresh   → Payment Service
POST localhost:8082/actuator/refresh   → User Service
POST localhost:8083/actuator/refresh   → Inventory Service
...
...
... (100s more)
```

---

## Why This is a Real Problem

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PROBLEMS WITH ACTUATOR APPROACH AT SCALE             │
│                                                                         │
│  1. MANUAL EFFORT                                                       │
│     → You have to remember and call /actuator/refresh                   │
│       on EVERY single microservice manually                             │
│     → Miss one? That service still has stale config                     │
│                                                                         │
│  2. NOT SCALABLE                                                        │
│     → 100 services = 100 POST calls                                     │
│     → 1000 services = 1000 POST calls                                   │
│     → This simply does not scale                                        │
│                                                                         │
│  3. ERROR PRONE                                                         │
│     → What if one service is temporarily down?                          │
│     → What if you forget a service?                                     │
│     → No easy way to confirm all services refreshed                     │
│                                                                         │
│  4. OPERATIONALLY PAINFUL                                               │
│     → In a large org, you might not even know                           │
│       which services use which config properties                        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## The Ideal Solution We Want

What we really want is this:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       THE IDEAL FLOW                                    │
│                                                                         │
│   Update Git Repo                                                       │
│         │                                                               │
│         ▼                                                               │
│   POST /actuator/refresh → Config Server ONLY                           │
│         │                                                               │
│         │  Config Server broadcasts a message:                          │
│         │  "Hey everyone! Config has changed. Go refresh yourselves!"   │
│         │                                                               │
│         ├──────────────────────────────────────────────────────────┐    │
│         │                    │                   │                  │   │
│         ▼                    ▼                   ▼                  ▼   │
│   Order Service       Payment Service      User Service      Inventory  │
│   auto-refreshes ✓   auto-refreshes ✓    auto-refreshes ✓  refreshes ✓  │
│                                                                         │
│   ONE call → ALL services refreshed automatically                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## The Solution: Spring Cloud Bus

This is exactly what **Spring Cloud Bus** solves. The instructor teases this as the next topic:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       SPRING CLOUD BUS                                  │
│                                                                         │
│   Spring Cloud Bus works by connecting all microservices                │
│   through a MESSAGE BROKER (like RabbitMQ or Kafka)                     │
│                                                                         │
│   When Config Server is refreshed:                                      │
│   → It publishes a "config changed" EVENT to the message broker         │
│   → All microservices are SUBSCRIBED to this broker                     │
│   → They all receive the event automatically                            │
│   → They all refresh their @RefreshScope beans on their own             │
│                                                                         │
│   Result: ONE refresh call → ALL services updated                       │
└─────────────────────────────────────────────────────────────────────────┘
```

Here is the high level picture of how Spring Cloud Bus works:

```
┌──────────────┐
│   Git Repo   │
└──────┬───────┘
       │ config updated
       ▼
┌──────────────────┐    POST /actuator/refresh
│  Config Server   │◄─────────────────────────── You call this ONCE
│   Port: 8888     │
└──────┬───────────┘
       │
       │ publishes "RefreshRemoteApplicationEvent"
       │ to message broker
       ▼
┌──────────────────────────────────────────────┐
│         MESSAGE BROKER                       │
│      (RabbitMQ or Kafka)                     │
└──────┬──────────────┬──────────────┬─────────┘
       │              │              │
       │ event        │ event        │ event
       │ received     │ received     │ received
       ▼              ▼              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐
│  Order   │   │ Payment  │   │   User   │  ... all others too
│ Service  │   │ Service  │   │ Service  │
│          │   │          │   │          │
│ auto     │   │ auto     │   │ auto     │
│ refresh  │   │ refresh  │   │ refresh  │
│    ✓     │   │    ✓     │   │    ✓     │
└──────────┘   └──────────┘   └──────────┘
```

---

## Actuator Approach vs Spring Cloud Bus — Side by Side

```
┌───────────────────────────┬─────────────────────────────────────────────┐
│   ACTUATOR APPROACH       │   SPRING CLOUD BUS APPROACH                 │
│   (What we learned today) │   (Coming next)                             │
├───────────────────────────┼─────────────────────────────────────────────┤
│ You call /actuator/refresh│ You call /actuator/refresh ONCE             │
│ on EVERY microservice     │ on Config Server only                       │
│ manually                  │                                             │
├───────────────────────────┼─────────────────────────────────────────────┤
│ 100 services =            │ 100 services =                              │
│ 100 manual POST calls     │ 1 POST call, all notified via               │
│                           │ message broker automatically                │
├───────────────────────────┼─────────────────────────────────────────────┤
│ Simple setup              │ Requires message broker                     │
│ No extra infrastructure   │ (RabbitMQ or Kafka)                         │
├───────────────────────────┼─────────────────────────────────────────────┤
│ Fine for small setups     │ Designed for large scale production         │
│ or learning               │ distributed systems                         │
├───────────────────────────┼─────────────────────────────────────────────┤
│ @RefreshScope still needed│ @RefreshScope still needed                  │
│ on the bean               │ on the bean — same concept applies          │
└───────────────────────────┴─────────────────────────────────────────────┘
```

---

## Key Takeaway from This Step

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          KEY TAKEAWAY                                   │
│                                                                         │
│  The Actuator approach (/actuator/refresh) works well but has           │
│  ONE major limitation at scale:                                         │
│                                                                         │
│  → You must manually call /actuator/refresh on EVERY                    │
│    microservice that needs to pick up the new config.                   │
│                                                                         │
│  Spring Cloud Bus solves this by:                                       │
│  → Connecting all services via a message broker                         │
│  → Broadcasting the refresh event to ALL services at once               │
│  → Requiring only ONE /actuator/refresh call (on Config Server)         │
│                                                                         │
│  BUT — regardless of which approach you use:                            │
│  → @RefreshScope on the bean is ALWAYS needed                           │
│  → The concept of destroy + recreate the bean stays the same            │
│  → The rule of NEVER using @RefreshScope on Controller/Service          │
│    stays the same                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

Ready for the final step — **Step 7: Complete Interview Tips Summary**?