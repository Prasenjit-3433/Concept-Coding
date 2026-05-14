# Circuit Breaker — Resilience4j (Part 4)

---

## Step 1 — The Problem: Why Do We Need a Circuit Breaker?

---

### The Setup

Let's say you have two microservices:

- **Order Service** — your main service
- **Product Service** — a downstream service that Order calls to fetch product details

```
Client  →→→  Order Service  →→→  Product Service
```

Now imagine **Product Service goes down**.

---

### What Happens Without a Circuit Breaker?

```
                    ❌ FAILS
Client → Order → Product (DOWN)
                    ❌ FAILS
Client → Order → Product (DOWN)
                    ❌ FAILS
Client → Order → Product (DOWN)
                    ❌ FAILS
Client → Order → Product (DOWN)
         ...keeps happening...
```

Order Service **keeps hammering** Product Service with calls, even though Product is down. Every single call is going to fail. But Order doesn't know that — it just keeps trying.

---

### The Two Core Problems This Causes

**Problem 1 — Product Service takes even longer to recover**

Product Service is already sick. It's struggling. And now Order Service is throwing even more requests at it. This extra load makes it harder for Product to heal and come back up. You're essentially kicking someone who's already down.

**Problem 2 — Order Service wastes its own resources**

Think about what happens on Order's side for every single failing call:

```
Order calls Product
    → waits... waits... waits... (until timeout)
    → timeout hits (say, 5 seconds)
    → returns error to client
    → thread was BLOCKED this entire time
```

Two things are being wasted here:
- **Latency** — every call is waiting till timeout before failing
- **Threads** — threads are sitting blocked, doing nothing useful, just waiting for a response that will never come

If thousands of requests are coming in, thousands of threads are blocked. Your Order Service starts degrading too — even though the problem was only in Product Service. This is how **one failing service can bring down your entire system**.

---

### The Core Question

> How does Order Service know **when to stop** calling Product Service, and **when it's safe to start again**?

Order Service has no way of knowing on its own:
- Is Product down right now?
- Has it recovered?
- Should I try again?

**Someone needs to give Order Service this intelligence.** That someone is the **Circuit Breaker**.

---

### In One Line

> The Circuit Breaker pattern **prevents an application from making repeated calls to a downstream service that is likely to fail.**

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 2 — The Solution: How Circuit Breaker Works

---

### The Core Idea

The Circuit Breaker sits **between Order Service and Product Service**. It monitors every call Order makes to Product. It keeps track of how many are succeeding and how many are failing.

Once failures cross a certain limit — it steps in and says:

> *"Hey Order, stop calling Product. I'll handle it from here. I'll let you know when Product is healthy again."*

```
                    [Circuit Breaker]
                          |
Client → Order Service → CB → Product Service
                          |
                    (watching every call,
                     tracking failures)
```

---

### How It Actually Solves the Problem — Step by Step

**Phase 1 — Everything is fine, calls go through normally**

```
Order → [CB watching...] → Product ✅
Order → [CB watching...] → Product ✅
Order → [CB watching...] → Product ✅
```

**Phase 2 — Product starts failing, CB starts counting**

```
Order → [CB: failure 1] → Product ❌
Order → [CB: failure 2] → Product ❌
Order → [CB: failure 3] → Product ❌
Order → [CB: failure 4] → Product ❌
Order → [CB: failure 5] → Product ❌
         ⚠️ Threshold crossed!
```

**Phase 3 — CB trips, stops all calls to Product immediately**

```
Order → [CB: STOP ✋] ❌ (call blocked instantly, no hit to Product)
Order → [CB: STOP ✋] ❌ (call blocked instantly, no hit to Product)
Order → [CB: STOP ✋] ❌ (call blocked instantly, no hit to Product)
```

Notice what changed here:
- Calls are **failed immediately** — no waiting, no timeout
- **No load** is added to Product Service — it gets breathing room to recover
- Order Service **threads are not blocked** — they fail fast and free up

**Phase 4 — After waiting some time, CB sends a few test calls**

```
Order → [CB: testing...] → Product ✅ (trial call 1)
Order → [CB: testing...] → Product ✅ (trial call 2)
Order → [CB: testing...] → Product ✅ (trial call 3)
         ✅ All passed! Product is healthy again.
```

**Phase 5 — CB opens the gate again, everything back to normal**

```
Order → [CB watching...] → Product ✅
Order → [CB watching...] → Product ✅
```

---

### The Full Picture in One Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   Client                                                    │
│     │                                                       │
│     ▼                                                       │
│  Order Service                                              │
│     │                                                       │
│     ▼                                                       │
│  ┌──────────────────────────────┐                           │
│  │       Circuit Breaker        │                           │
│  │                              │                           │
│  │  - Monitors every call       │                           │
│  │  - Tracks success/failure    │                           │
│  │  - Trips when threshold hit  │                           │
│  │  - Tests if service is back  │                           │
│  └──────────────────────────────┘                           │
│     │                                                       │
│     ▼                                                       │
│  Product Service                                            │
│  (downstream — may be UP or DOWN)                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### What Does "Threshold" Mean Here?

You configure the Circuit Breaker with a threshold. For example:

> *"If 50% of the last 10 calls fail — trip the circuit."*

So out of 10 calls, if 5 or more fail → Circuit Breaker kicks in.

This threshold is fully configurable — you decide what makes sense for your system. We'll go deep into all configurations in Step 4.

---

### The Fallback

When the Circuit Breaker blocks a call (because the circuit is open), it doesn't just throw an ugly error at the client. You define a **fallback method** — a graceful degraded response.

For example:
- Return a cached response
- Return a default value
- Return a friendly error message

```java
// Instead of crashing, this gets called:
public void fallback(Throwable ex) {
    System.out.println("Product service is currently unavailable. Please try later.");
}
```

The fallback is **invoked on every failure attempt** — whether the circuit is open or a call actually failed while the circuit was closed.

---

### Quick Summary So Far

| Situation | What CB Does |
|---|---|
| Product is healthy | Lets all calls through, keeps watching |
| Failures cross threshold | Trips open, blocks all calls instantly |
| Circuit is open | Fails calls immediately, no thread blocking |
| After wait period | Sends a few test calls to Product |
| Test calls succeed | Closes circuit, normal calls resume |
| Test calls fail | Stays open, waits again |

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 3 — The Three States of Circuit Breaker

---

### The Analogy — Think of a Real Electrical Circuit

The instructor makes a very important point here because engineers often get confused:

> ❌ **"Circuit is CLOSED"** does NOT mean calls are blocked.
> ✅ **"Circuit is CLOSED"** means calls are FLOWING — just like electricity flows through a closed circuit.
> ✅ **"Circuit is OPEN"** means calls are BLOCKED — just like electricity stops when a circuit is open/broken.

Remember this always. It's the opposite of what your intuition might say.

---

### The Three States

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│                        ● INITIAL STATE                              │
│                              │                                      │
│                              ▼                                      │
│                    ┌─────────────────┐                              │
│                    │                 │                              │
│                    │    CLOSED  🟢   │ ◄───────────────────┐        │
│                    │                 │                     │        │
│                    │ All calls pass  │                     │        │
│                    │ through to      │                     │        │
│                    │ downstream      │          Test calls │        │
│                    │                 │      success = 100% │        │
│                    └────────┬────────┘                     │        │
│                             │                              │        │
│                    Failure rate crosses                    │        │
│                    threshold (eg: 50%)                     │        │
│                             │                              │        │
│                             ▼                              │        │
│                    ┌─────────────────┐                     │        │
│                    │                 │                     │        │
│                    │    OPEN    🔴   │                     │        │
│                    │                 │                     │        │
│                    │ ALL calls are   │                     │        │
│                    │ blocked & fail  │                     │        │
│                    │ immediately     │                     │        │
│                    │ No hit to       │                     │        │
│                    │ downstream      │◄──────────────┐     │        │
│                    └────────┬────────┘               │     │        │
│                             │                        │     │        │
│                    After wait duration               │     │        │
│                    (eg: 10 seconds)        Test calls│     │        │
│                             │                success │     │        │
│                             ▼                ≠ 100%  │     │        │
│                    ┌─────────────────┐               │     │        │
│                    │                 │               │     │        │
│                    │  HALF-OPEN 🟡   │               │     │        │
│                    │                 │               │     │        │
│                    │ Only LIMITED    │───────────────┘     │        │
│                    │ test calls      │                     │        │
│                    │ allowed through │─────────────────────┘        │
│                    │ (eg: 3 calls)   │                              │
│                    │ Track success   │                              │
│                    └─────────────────┘                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

### State 1 — CLOSED 🟢 (The Happy Path)

This is where every application **starts** and where you **want to stay**.

- All calls from Order Service flow freely to Product Service
- Circuit Breaker is silently watching in the background
- It tracks every call — success or failure
- It uses a **sliding window** to evaluate the failure rate

```
Order → [CB: closed, watching] → Product ✅
Order → [CB: closed, watching] → Product ✅
Order → [CB: closed, watching] → Product ❌  (failure count: 1)
Order → [CB: closed, watching] → Product ✅
Order → [CB: closed, watching] → Product ❌  (failure count: 2)
...
```

**When does it leave CLOSED?**
→ When the failure rate crosses the configured threshold within the sliding window.

---

### State 2 — OPEN 🔴 (The Protective State)

The circuit has tripped. Product Service is considered unhealthy.

- **Zero calls** reach Product Service
- Every incoming call is **failed immediately** — no waiting, no timeout
- Threads are **not blocked** — they fail fast and are freed up
- The fallback method is invoked for every blocked call
- Circuit Breaker starts an internal **timer** (wait duration)

```
Order → [CB: OPEN 🔴] ✋ → call fails instantly (fallback runs)
Order → [CB: OPEN 🔴] ✋ → call fails instantly (fallback runs)
Order → [CB: OPEN 🔴] ✋ → call fails instantly (fallback runs)
         ...waiting 10 seconds...
```

**When does it leave OPEN?**
→ After the configured wait duration expires (e.g., 10 seconds), it automatically moves to HALF-OPEN.

> 💡 This automatic transition is handled internally by `ScheduledThreadPoolExecutor` — we'll cover this in detail in Step 7 (Internal Working).

---

### State 3 — HALF-OPEN 🟡 (The Testing State)

The circuit is cautiously testing if Product Service has recovered.

- Only a **limited number of trial calls** are allowed through (e.g., 3 calls)
- These calls actually hit Product Service — CB is watching their results
- All other calls during this time are still failed immediately

```
Order → [CB: HALF-OPEN, trial 1/3] → Product ✅
Order → [CB: HALF-OPEN, trial 2/3] → Product ✅
Order → [CB: HALF-OPEN, trial 3/3] → Product ✅
         ✅ 100% success! → Move to CLOSED
```

```
Order → [CB: HALF-OPEN, trial 1/3] → Product ✅
Order → [CB: HALF-OPEN, trial 2/3] → Product ❌
Order → [CB: HALF-OPEN, trial 3/3] → Product ❌
         ❌ Not 100%! → Go back to OPEN
```

**When does it leave HALF-OPEN?**
→ **Success = 100%** → moves to **CLOSED** (normal operation resumes)
→ **Success < 100%** → moves back to **OPEN** (wait again, retry later)

---

### State Transition — Clean Summary Table

| From | To | Trigger |
|---|---|---|
| CLOSED | OPEN | Failure rate crosses threshold |
| OPEN | HALF-OPEN | Wait duration expires |
| HALF-OPEN | CLOSED | All test calls succeed (100%) |
| HALF-OPEN | OPEN | Any test call fails (< 100%) |

---

### One Thing to Always Remember

The instructor specifically highlights this:

> The **fallback method is invoked on every failure attempt** — not just when the circuit is open. Even when the circuit is CLOSED and a call actually fails, the fallback runs for that failed call too.

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 4 — Configuration Deep Dive

---

### First, The Full Configuration Block

Here is the complete `application.properties` configuration the instructor uses. Read it once fully before we break it down:

```properties
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

#product service - circuit breaker configurations
resilience4j.circuitbreaker.instances.productService.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.productService.sliding-window-size=10
resilience4j.circuitbreaker.instances.productService.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.productService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.productService.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.productService.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.productService.automatic-transition-from-open-to-half-open-enabled=true
```

Now let's go through **every single property** one by one.

---

### Property 1 & 2 — Sliding Window Type & Size

```properties
resilience4j.circuitbreaker.instances.productService.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.productService.sliding-window-size=10
```

These two always go together. First you pick the **type**, then you set the **size**.

The sliding window defines **what chunk of calls** the Circuit Breaker looks at to evaluate the failure rate. Think of it as the CB's observation window.

---

**Option A — COUNT_BASED**

```
sliding-window-type = COUNT_BASED
sliding-window-size = 10
```

Means: *"Look at the last 10 calls. Based on those 10, evaluate the failure rate."*

```
Call history (most recent 10):
[ ✅ ✅ ❌ ✅ ❌ ❌ ✅ ❌ ✅ ❌ ]
           ↑
    Window of 10 calls
    5 failures out of 10 = 50% failure rate
```

As new calls come in, the window **slides** — oldest call drops off, newest call added.

---

**Option B — TIME_BASED**

```
sliding-window-type = TIME_BASED
sliding-window-size = 10
```

Means: *"Look at all calls made in the last 10 seconds. Based on those, evaluate the failure rate."*

```
Last 10 seconds of calls:
[ ✅ ❌ ✅ ✅ ❌ ❌ ✅ ]  ← could be 7, could be 100 calls
           ↑
    Window of 10 seconds
    Whatever came in, evaluate failure % from those
```

The number of calls doesn't matter here — only the time window matters.

---

> ⚠️ **Don't confuse this** with Rate Limiter's sliding window — that's a completely different concept. This sliding window is only about **evaluating failure rate** for circuit breaking decisions.

---

### Property 3 — Minimum Number of Calls

```properties
resilience4j.circuitbreaker.instances.productService.minimum-number-of-calls=5
```

**Why does this exist?**

This is a very thoughtful configuration. Here's the problem it solves:

Imagine your window size is 10 (count-based) and failure threshold is 50%. But only 2 calls have come in so far, and 1 of them failed.

```
Window: [ ❌ ✅ ]
Failure rate = 50% → Should CB trip? 🤔
```

That's a **false alarm**. Only 2 calls came in — you can't make a reliable judgment from that. Maybe it was a one-off network blip. If you retry, it might pass.

So `minimum-number-of-calls=5` says:

> *"Don't even start evaluating the failure rate until at least 5 calls have been made."*

```
Call 1 fails → failure count: 1 → below minimum (5), don't evaluate yet
Call 2 fails → failure count: 2 → below minimum (5), don't evaluate yet
Call 3 fails → failure count: 3 → below minimum (5), don't evaluate yet
Call 4 fails → failure count: 4 → below minimum (5), don't evaluate yet
Call 5 fails → failure count: 5 → minimum reached! NOW start evaluating ✅
```

This prevents false positives when traffic is very low.

---

### Property 4 — Failure Rate Threshold

```properties
resilience4j.circuitbreaker.instances.productService.failure-rate-threshold=50
```

This is a **percentage**. Once the minimum number of calls is reached, if the failure rate within the sliding window hits or exceeds this percentage — the circuit moves from **CLOSED → OPEN**.

With our configuration:
- Window size = 10 calls
- Threshold = 50%
- So: if **5 or more out of last 10 calls fail** → circuit trips

```
Window: [ ❌ ❌ ❌ ❌ ❌ ✅ ✅ ✅ ✅ ✅ ]
          5 failures out of 10 = 50% = threshold hit!
          → Circuit moves to OPEN 🔴
```

---

### Property 5 — Wait Duration in Open State

```properties
resilience4j.circuitbreaker.instances.productService.wait-duration-in-open-state=10s
```

Once the circuit is OPEN, how long should it stay open before moving to HALF-OPEN?

Here it's **10 seconds**.

```
Circuit trips OPEN at t=0s
    → All calls blocked for 10 seconds
    → At t=10s, automatically moves to HALF-OPEN
```

This gives the downstream service (Product) time to recover before you start testing it again.

> 💡 The automatic transition from OPEN → HALF-OPEN is only possible if you set `automatic-transition-from-open-to-half-open-enabled=true` (Property 7 below). Internally this uses `ScheduledThreadPoolExecutor` — covered in Step 7.

---

### Property 6 — Permitted Number of Calls in Half-Open State

```properties
resilience4j.circuitbreaker.instances.productService.permitted-number-of-calls-in-half-open-state=3
```

Once in HALF-OPEN, how many trial calls are allowed to hit Product Service?

Here it's **3 calls**.

```
HALF-OPEN state:
Trial call 1 → hits Product → result tracked
Trial call 2 → hits Product → result tracked
Trial call 3 → hits Product → result tracked
              ↓
  All 3 success → CLOSED ✅
  Any failure   → OPEN 🔴
```

All other calls during HALF-OPEN (beyond these 3) are still failed immediately.

---

### Property 7 — Automatic Transition from Open to Half-Open

```properties
resilience4j.circuitbreaker.instances.productService.automatic-transition-from-open-to-half-open-enabled=true
```

If this is `true` → the circuit **automatically** moves from OPEN to HALF-OPEN after the wait duration, without needing an incoming request to trigger it.

If this is `false` → the transition only happens when the next request comes in after the wait duration is over.

Setting it to `true` is the cleaner approach — the CB manages itself without relying on an incoming request to wake it up.

---

### Bonus — Controlling Which Exceptions Are Tracked

By default, **all RuntimeExceptions and Errors** are counted as failures. But you can control this:

```properties
# Only track these specific exceptions as failures:
resilience4j.circuitbreaker.instances.productService.record-exceptions=\
  java.io.IOException,\
  org.springframework.web.client.HttpServerErrorException

# Completely ignore these exceptions (don't count as failure):
resilience4j.circuitbreaker.instances.productService.ignore-exceptions=\
  java.lang.IllegalArgumentException
```

This is useful when you want to be precise. For example — maybe a `404 Not Found` should NOT trip your circuit breaker (it's a valid response, not a service failure), but a `500 Internal Server Error` should.

---

### All Properties — Quick Reference Table

| Property | What It Does | Example Value |
|---|---|---|
| `sliding-window-type` | Count-based or time-based window | `COUNT_BASED` |
| `sliding-window-size` | Size of the window (calls or seconds) | `10` |
| `minimum-number-of-calls` | Min calls before failure rate is evaluated | `5` |
| `failure-rate-threshold` | % of failures that trips the circuit | `50` |
| `wait-duration-in-open-state` | How long circuit stays OPEN | `10s` |
| `permitted-number-of-calls-in-half-open-state` | Trial calls allowed in HALF-OPEN | `3` |
| `automatic-transition-from-open-to-half-open-enabled` | Auto move OPEN → HALF-OPEN | `true` |
| `record-exceptions` | Only track these as failures | `IOException` |
| `ignore-exceptions` | Never track these as failures | `IllegalArgumentException` |

---

### How All Properties Work Together — Full Picture

```
Incoming calls arrive
        │
        ▼
Is minimum-number-of-calls reached?
        │
   NO → keep letting calls through, don't evaluate yet
        │
   YES → evaluate failure rate within sliding-window-size
        │
        ▼
Has failure-rate-threshold been crossed?
        │
   NO → stay CLOSED, keep watching
        │
   YES → move to OPEN
        │
        ▼
Stay OPEN for wait-duration-in-open-state (10s)
(automatic-transition-from-open-to-half-open-enabled=true)
        │
        ▼
Move to HALF-OPEN
Allow permitted-number-of-calls-in-half-open-state (3) trial calls
        │
   All pass → CLOSED ✅
   Any fail → OPEN 🔴 (wait again...)
```

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 5 — Full Implementation

---

### The Overall Architecture First

Before writing any code, understand what we're building:

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    ORDER SERVICE (port: 8081)            │   │
│  │                                                          │   │
│  │  OrderController        OrderService      ProductClient  │   │
│  │  (REST endpoint)  →→→  (CB lives here) →→→  (Feign)      │   │
│  │  /orders/{id}           @CircuitBreaker    /products/{id}│   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                    [Eureka Service Discovery]                   │
│                              │                                  │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                 PRODUCT SERVICE (DOWN ❌)                 │   │
│  │                  (not started intentionally)             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The instructor is testing the Circuit Breaker by **intentionally not starting** Product Service — so every call to it will fail.

---

### Step 5.1 — Maven Dependency

The instructor makes an important point here:

> You do NOT need a separate Circuit Breaker dependency. It comes bundled with the `resilience4j-spring-boot3` dependency — the same one used for Retry and Bulkhead in previous parts.

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

Add this to your `pom.xml`. That's it — no extra dependency needed for Circuit Breaker specifically.

---

### Step 5.2 — The REST Controller

This is a simple controller that exposes an endpoint. When hit, it delegates to `OrderService`.

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/{id}")
    public void callProductAPI(@PathVariable String id) {
        orderService.invokeProductAPI(id);
    }
}
```

Nothing special here. The real action happens in `OrderService`.

---

### Step 5.3 — The Feign Client

This is how Order Service talks to Product Service. Since we're using **Eureka** for service discovery, no hardcoded URL is needed — just the service name.

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping(value = "/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

> 💡 Because Eureka is used, Feign automatically resolves `product-service` to the actual running instance. If the instance isn't registered (because Product Service is down), this call will fail — which is exactly the scenario we're testing.

---

### Step 5.4 — The Order Service (Where Circuit Breaker Lives)

This is the most important class. The `@CircuitBreaker` annotation goes **on the method** that makes the downstream call.

```java
@Component
public class OrderService {

    @Autowired
    ProductClient productClient;

    /*
     * @CircuitBreaker wraps this method.
     * - name: matches the instance name in application.properties
     * - fallbackMethod: called on every failure attempt
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "fallback")
    public void invokeProductAPI(String id) {
        productClient.getProductById(id);
    }

    /*
     * Fallback method signature rules:
     * - Must have the same return type as the main method (void here)
     * - Must accept a Throwable parameter to capture the exception
     * - Gets invoked on EVERY failure — whether circuit is open or closed
     */
    public void fallback(Throwable ex) {
        System.out.println("not able to invoke product service");
    }
}
```

**Two things to note carefully:**

**1. The `name` in `@CircuitBreaker`**

```java
@CircuitBreaker(name = "productService", ...)
```

This `"productService"` is the **instance name**. It must match exactly what you put in `application.properties`:

```properties
resilience4j.circuitbreaker.instances.productService.sliding-window-size=10
                                       ↑
                               must match this
```

**2. The fallback method**

```java
public void fallback(Throwable ex) { ... }
```

- Same return type as the main method (`void`)
- Takes a `Throwable` — this is the exception that caused the failure
- Called on **every single failure** — not just when the circuit is open
- Even when the circuit is CLOSED and a real call fails, fallback runs

---

### Step 5.5 — application.properties (Complete)

```properties
# Server config
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Circuit Breaker config for productService instance
resilience4j.circuitbreaker.instances.productService.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.productService.sliding-window-size=10
resilience4j.circuitbreaker.instances.productService.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.productService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.productService.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.productService.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.productService.automatic-transition-from-open-to-half-open-enabled=true
```

---

### Step 5.6 — How All Pieces Connect Together

```
HTTP GET /orders/{id}
        │
        ▼
OrderController.callProductAPI(id)
        │
        ▼
OrderService.invokeProductAPI(id)   ← @CircuitBreaker is here
        │
        ├── [Circuit is CLOSED] → ProductClient.getProductById(id)
        │                                │
        │                         [Feign resolves "product-service"
        │                          via Eureka and makes HTTP call]
        │                                │
        │                         ✅ success → return response
        │                         ❌ failure → fallback() runs
        │
        └── [Circuit is OPEN] → fallback() runs immediately
                                 (no call to ProductClient at all)
```

---

### Step 5.7 — Fallback Method Rules (Important!)

The instructor doesn't go deep on this but it's critical to get right, otherwise your app will crash at startup:

```
✅ CORRECT — same return type, Throwable parameter:
public void fallback(Throwable ex) { ... }

✅ CORRECT — if main method returns String:
public String fallback(Throwable ex) { return "default"; }

❌ WRONG — missing Throwable:
public void fallback() { ... }  // won't work

❌ WRONG — wrong return type:
public String fallback(Throwable ex) { ... }  // when main returns void
```

---

### Complete File Structure — What You Need

```
order-service/
│
├── pom.xml                          ← resilience4j dependency
│
└── src/main/
    ├── java/
    │   ├── OrderController.java     ← REST endpoint
    │   ├── OrderService.java        ← @CircuitBreaker annotation here
    │   └── ProductClient.java       ← Feign client
    │
    └── resources/
        └── application.properties  ← all CB configurations
```

---

### Quick Recap — Implementation Checklist

```
✅ 1. Add resilience4j-spring-boot3 dependency in pom.xml
✅ 2. Create Feign client interface for downstream service
✅ 3. Add @CircuitBreaker on the method making downstream call
        - name must match application.properties instance name
        - fallbackMethod must point to a valid fallback
✅ 4. Write fallback method
        - same return type as main method
        - accepts Throwable parameter
✅ 5. Configure all properties in application.properties
        - sliding-window-type & size
        - minimum-number-of-calls
        - failure-rate-threshold
        - wait-duration-in-open-state
        - permitted-number-of-calls-in-half-open-state
        - automatic-transition-from-open-to-half-open-enabled
```

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 6 — Live Output Walkthrough

---

### The Test Setup

The instructor's test scenario is simple but effective:

- **Order Service** → running on port 8081 ✅
- **Product Service** → intentionally NOT started ❌
- **Eureka Server** → running ✅

Every call Order makes to Product will fail because Product has no registered instance in Eureka.

Configuration being used:
```
sliding-window-type  = COUNT_BASED
sliding-window-size  = 10
minimum-number-of-calls = 5
failure-rate-threshold  = 50%  →  means 5 out of 10 calls must fail
wait-duration-in-open-state = 10s
permitted-number-of-calls-in-half-open-state = 3
```

---

### Phase 1 — CLOSED State (Calls 1 to 5)

The circuit starts in **CLOSED** state. Every call actually tries to reach Product Service. But since Product is down, every call fails and fallback runs.

---

**Call 1:**
```
State       : CLOSED 🟢
Action      : Actually tries to hit Product Service
Result      : ❌ FAIL — no instance found in Eureka
Fallback    : "not able to invoke product service" printed
─────────────────────────────────────────────────
Failure count    : 1
Minimum calls    : 5  → NOT YET REACHED, don't evaluate
Window size      : 10
Threshold        : 50% of 10 = 5 failures needed
─────────────────────────────────────────────────
Circuit state stays: CLOSED 🟢
```

---

**Call 2:**
```
State       : CLOSED 🟢
Action      : Actually tries to hit Product Service
Result      : ❌ FAIL
Fallback    : "not able to invoke product service" printed
─────────────────────────────────────────────────
Failure count    : 2
Minimum calls    : 5  → NOT YET REACHED, don't evaluate
─────────────────────────────────────────────────
Circuit state stays: CLOSED 🟢
```

---

**Call 3:**
```
State       : CLOSED 🟢
Action      : Actually tries to hit Product Service
Result      : ❌ FAIL
Fallback    : "not able to invoke product service" printed
─────────────────────────────────────────────────
Failure count    : 3
Minimum calls    : 5  → NOT YET REACHED, don't evaluate
─────────────────────────────────────────────────
Circuit state stays: CLOSED 🟢
```

---

**Call 4:**
```
State       : CLOSED 🟢
Action      : Actually tries to hit Product Service
Result      : ❌ FAIL
Fallback    : "not able to invoke product service" printed
─────────────────────────────────────────────────
Failure count    : 4
Minimum calls    : 5  → NOT YET REACHED, don't evaluate
─────────────────────────────────────────────────
Circuit state stays: CLOSED 🟢
```

---

**Call 5 — THE CRITICAL CALL:**
```
State       : CLOSED 🟢
Action      : Actually tries to hit Product Service
Result      : ❌ FAIL
Fallback    : "not able to invoke product service" printed
─────────────────────────────────────────────────
Failure count    : 5
Minimum calls    : 5  → ✅ MINIMUM REACHED! Start evaluating now.
Window size      : 10
Threshold        : 50% of 10 = 5 failures needed to trip
Failure count    : 5 out of last 10 = 50% = THRESHOLD HIT! ⚠️
─────────────────────────────────────────────────
🔴 Circuit state changes: CLOSED → OPEN
```

At this exact moment in the logs you'll see:

```
CircuitBreaker 'productService' changed state from CLOSED to OPEN
```

---

### Phase 1 — Visual Summary

```
Call  1: [CLOSED] → hits Product ❌ | failures: 1/5 min | don't evaluate
Call  2: [CLOSED] → hits Product ❌ | failures: 2/5 min | don't evaluate
Call  3: [CLOSED] → hits Product ❌ | failures: 3/5 min | don't evaluate
Call  4: [CLOSED] → hits Product ❌ | failures: 4/5 min | don't evaluate
Call  5: [CLOSED] → hits Product ❌ | failures: 5/5 min | 50% threshold hit!
                                                          ↓
                                              🔴 CLOSED → OPEN
```

---

### Phase 2 — OPEN State (10 second wait)

Circuit is now OPEN. The instructor checks the timestamp in logs.

```
Time at OPEN  : :20 (seconds)
Wait duration : 10 seconds
Expected HALF-OPEN at : :30 (seconds)
```

During these 10 seconds, ANY call that comes in:

```
State   : OPEN 🔴
Action  : CB intercepts IMMEDIATELY — no call reaches Product
Result  : ❌ Fail instantly (no timeout wait, no thread blocking)
Fallback: "not able to invoke product service" printed instantly
```

```
Call 6 : [OPEN 🔴] ✋ blocked instantly → fallback runs
Call 7 : [OPEN 🔴] ✋ blocked instantly → fallback runs
Call 8 : [OPEN 🔴] ✋ blocked instantly → fallback runs
         ...no calls reach Product Service at all...
```

Then — after exactly 10 seconds:

```
At :30 seconds →
CircuitBreaker 'productService' changed state from OPEN to HALF_OPEN
```

This transition happens **automatically** — driven internally by `ScheduledThreadPoolExecutor`. No incoming request needed to trigger it. (We'll cover how in Step 7.)

---

### Phase 3 — HALF-OPEN State (3 trial calls)

Circuit is now HALF-OPEN. Only 3 trial calls are allowed through to Product.

---

**Trial Call 1:**
```
State       : HALF-OPEN 🟡
Action      : Actually tries to hit Product Service (trial 1 of 3)
Result      : ❌ FAIL — Product still down
Fallback    : runs
─────────────────────────────────────────────────
Trial calls used : 1/3
Max trial calls  : 3
─────────────────────────────────────────────────
Circuit state stays: HALF-OPEN 🟡 (still have 2 more trials)
```

---

**Trial Call 2:**
```
State       : HALF-OPEN 🟡
Action      : Actually tries to hit Product Service (trial 2 of 3)
Result      : ❌ FAIL
Fallback    : runs
─────────────────────────────────────────────────
Trial calls used : 2/3
─────────────────────────────────────────────────
Circuit state stays: HALF-OPEN 🟡
```

---

**Trial Call 3 — THE DECISION CALL:**
```
State       : HALF-OPEN 🟡
Action      : Actually tries to hit Product Service (trial 3 of 3)
Result      : ❌ FAIL
Fallback    : runs
─────────────────────────────────────────────────
Trial calls used : 3/3  → MAX REACHED
Success rate     : 0/3  = 0% → NOT 100%
─────────────────────────────────────────────────
🔴 Circuit state changes: HALF-OPEN → OPEN (again)
```

In the logs:
```
CircuitBreaker 'productService' changed state from HALF_OPEN to OPEN
```

The whole cycle repeats — wait 10 seconds, try again with 3 trial calls, evaluate...

---

### Phase 3 — Visual Summary

```
Trial 1: [HALF-OPEN] → hits Product ❌ | 1/3 used
Trial 2: [HALF-OPEN] → hits Product ❌ | 2/3 used
Trial 3: [HALF-OPEN] → hits Product ❌ | 3/3 used | success = 0% ≠ 100%
                                                      ↓
                                          🔴 HALF-OPEN → OPEN
```

**What if Product had recovered?**

```
Trial 1: [HALF-OPEN] → hits Product ✅ | 1/3 used
Trial 2: [HALF-OPEN] → hits Product ✅ | 2/3 used
Trial 3: [HALF-OPEN] → hits Product ✅ | 3/3 used | success = 100% ✅
                                                      ↓
                                          🟢 HALF-OPEN → CLOSED
```

---

### Full End-to-End Flow — Complete Picture

```
t=0s  Call 1  [CLOSED] → Product ❌ | failures: 1 | min not reached
t=0s  Call 2  [CLOSED] → Product ❌ | failures: 2 | min not reached
t=0s  Call 3  [CLOSED] → Product ❌ | failures: 3 | min not reached
t=0s  Call 4  [CLOSED] → Product ❌ | failures: 4 | min not reached
t=0s  Call 5  [CLOSED] → Product ❌ | failures: 5 | threshold hit!
                                            ↓
                                🔴 CLOSED → OPEN at t=20s
                                            ↓
              [OPEN — all calls blocked for 10 seconds]
                                            ↓
                              🟡 OPEN → HALF-OPEN at t=30s
                                            ↓
t=30s Trial 1 [HALF-OPEN] → Product ❌ | 1/3
t=30s Trial 2 [HALF-OPEN] → Product ❌ | 2/3
t=30s Trial 3 [HALF-OPEN] → Product ❌ | 3/3 | success ≠ 100%
                                            ↓
                                🔴 HALF-OPEN → OPEN
                                            ↓
                         [cycle repeats every 10 seconds]
```

---

### Key Observations From the Output

**1. Calls 1-5 all actually hit Product Service**

Even though they all fail, they still go through because the circuit is CLOSED. CB needs real data to make its decision.

**2. Once OPEN — zero calls reach Product**

Failures are instant. No latency. No thread blocking. This is the whole point.

**3. The OPEN → HALF-OPEN transition is automatic**

You can see in the logs it happens at exactly `t + 10s` without any manual trigger or incoming request.

**4. Fallback runs in ALL cases**

Whether the circuit is CLOSED (real call fails) or OPEN (blocked immediately) — fallback always runs. The client always gets a response, never a hanging request.

**5. Half-Open trial calls actually hit Product**

This is important — trial calls are **real calls** to Product. CB needs to know if Product has genuinely recovered.

---
# Circuit Breaker — Resilience4j (Part 4)

---

## Step 7 — Internal Working

---

### The Big Picture — What Happens Under The Hood

When you put `@CircuitBreaker` on a method, you're not writing the state management logic yourself. Resilience4j handles all of it internally. Let's trace exactly how.

```
Your Code                    Resilience4j Internals
─────────────────────────────────────────────────────────────
@CircuitBreaker              AOP Proxy
invokeProductAPI()    →→→    CircuitBreakerStateMachine.java
                             (manages all state transitions)
                                      │
                             ScheduledThreadPoolExecutor
                             (manages OPEN → HALF-OPEN timer)
                                      │
                             DelayedQueue
                             (schedules the transition task)
```

---

### Part 1 — AOP (Aspect Oriented Programming)

The instructor mentions this across all Resilience4j videos — it's a consistent internal mechanism.

When you annotate a method with `@CircuitBreaker`:

```java
@CircuitBreaker(name = "productService", fallbackMethod = "fallback")
public void invokeProductAPI(String id) {
    productClient.getProductById(id);
}
```

Resilience4j **does not modify your method**. Instead it creates an **AOP Proxy** around it.

```
You call → invokeProductAPI()
                │
                ▼
        [AOP Proxy intercepts]
                │
                ▼
    CircuitBreakerStateMachine
    checks: what state am I in?
                │
        ┌───────┴────────┐
        │                │
    CLOSED/            OPEN
    HALF-OPEN            │
        │                ▼
        ▼         Fail immediately
    Let call      Run fallback
    through
        │
        ▼
    productClient.getProductById(id)
    [actual downstream call]
        │
    ✅ or ❌
        │
        ▼
    Report result back to
    CircuitBreakerStateMachine
    (should state change?)
```

So AOP is the entry point — it intercepts every call and hands it to `CircuitBreakerStateMachine`.

---

### Part 2 — CircuitBreakerStateMachine.java

This is the **brain** of the Circuit Breaker. It lives inside the Resilience4j framework — you don't write this, but understanding it is critical for interviews.

It has specific logic for every state transition. The instructor shows one sample method from this class:

```java
// Transitions to open state when thresholds have been exceeded.
//
// Params: result – the Result
private void checkIfThresholdsExceeded(Result result) {
    if (Result.hasExceededThresholds(result)
            && isClosed.compareAndSet(true, false)) {
        /*
         After every error, it checks if state need to be changed or not.
         Like here its checking, if threshold limit reached and state is CLOSED,
         then transit to OPEN state.

         Likewise similar method is present for different state with specific
         transition logic.
         */
        publishCircuitThresholdsExceededEvent(result, circuitBreakerMetrics);
        transitionToOpenState();
    }
}
```

**Breaking this down line by line:**

```
Result.hasExceededThresholds(result)
→ Has the failure rate crossed our configured threshold (50%)?

isClosed.compareAndSet(true, false)
→ Is the current state CLOSED?
→ If yes, atomically set it to false (not closed anymore)
→ This is thread-safe — prevents multiple threads from
  simultaneously triggering the transition

transitionToOpenState()
→ Move to OPEN state
```

**The pattern is the same for every state:**

```
After every call result (success or failure):
    → Check: does this result require a state change?
    → If yes: transition to the appropriate next state
    → Similar methods exist for HALF-OPEN → CLOSED
      and HALF-OPEN → OPEN transitions
```

```
┌─────────────────────────────────────────────────────────┐
│            CircuitBreakerStateMachine                   │
│                                                         │
│  checkIfThresholdsExceeded()  → CLOSED  → OPEN          │
│  evaluateHalfOpenResults()    → HALF-OPEN → CLOSED      │
│  evaluateHalfOpenResults()    → HALF-OPEN → OPEN        │
│                                                         │
│  Each method runs after every call result               │
│  and decides if a state transition is needed            │
└─────────────────────────────────────────────────────────┘
```

> 💡 **Interview Tip from instructor:** If an interviewer asks about Circuit Breaker internals, open `CircuitBreakerStateMachine.java` in your IDE, put a debug point, and trace through it. You'll see exactly how it moves from one state to another for each failure.

---

### Part 3 — The Open Question

The instructor pauses here and asks a very important question:

> *"For every failure call, we check if state needs to change — that makes sense. But once we move to OPEN state, how does it automatically transition to HALF-OPEN after 10 seconds? Nobody is calling any method. No request is coming in. How does it happen on its own?"*

The answer is `ScheduledThreadPoolExecutor`.

---

### Part 4 — ScheduledThreadPoolExecutor

When the circuit moves to OPEN state, `CircuitBreakerStateMachine` internally calls the `OpenState` constructor. Look at what happens inside it:

```java
OpenState(final int attempts,
          final long waitDurationInMillis,
          final Instant retryAfterWaitDuration,
          CircuitBreakerMetrics circuitBreakerMetrics) {

    this.attempts = attempts;
    this.retryAfterWaitDuration = retryAfterWaitDuration;
    this.circuitBreakerMetrics = circuitBreakerMetrics;

    if (circuitBreakerConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled()) {

        ScheduledExecutorService scheduledExecutorService =
                schedulerFactory.getScheduler();

        /*
         * It passes the request to ScheduledThreadPoolExecutor.
         *
         * 1st parameter → Task: "toHalfOpenState"
         *                  (the job to run — transition OPEN → HALF-OPEN)
         *
         * 2nd parameter → Delay: waitDurationInMillis
         *                  (how long to wait before running — e.g. 10000ms)
         *
         * 3rd parameter → Time Unit: TimeUnit.MILLISECONDS
         */
        transitionToHalfOpenFuture = scheduledExecutorService
                .schedule(this::toHalfOpenState,
                           waitDurationInMillis,
                           TimeUnit.MILLISECONDS);
    } else {
        transitionToHalfOpenFuture = null;
    }

    isOpen = new AtomicBoolean(true);
}
```

**In plain English:**

The moment circuit moves to OPEN, it submits a task to a scheduler:

```
Task    : "transition from OPEN to HALF-OPEN"
Run at  : now + 10 seconds (waitDurationInMillis)
```

This task sits and waits. After 10 seconds, it runs automatically. That's how the transition happens with no incoming request needed.

> ⚠️ This only works if `automatic-transition-from-open-to-half-open-enabled=true` in your config. If it's `false`, `transitionToHalfOpenFuture = null` — no task is scheduled, no automatic transition.

---

### Part 5 — How ScheduledThreadPoolExecutor Works Internally

The instructor goes one level deeper here — this is pure **interview gold**.

`ScheduledThreadPoolExecutor` internally uses a **DelayedQueue**.

```
┌─────────────────────────────────────────────────────────────┐
│                   ScheduledThreadPoolExecutor               │
│                                                             │
│  ┌───────────────────────────────────────────┐              │
│  │              DelayedQueue                 │              │
│  │         (Priority Queue internally)       │              │
│  │                                           │              │
│  │  Tail                          Head       │              │
│  │  [ Task4 ] [ Task3 ] [ Task2 ] [ Task1 ]  │              │
│  │  delay:30s  delay:20s  delay:15s delay:10s│              │
│  │                                    ↑      │              │
│  │              Task with minimum     │      │              │
│  │              delay is always here  │      │              │
│  └───────────────────────────────────────────┘              │
│                         │                                   │
│                         ▼                                   │
│              ┌──────────────────┐                           │
│              │   Worker Threads │                           │
│              │  T1   T2   T3    │                           │
│              └──────────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

**How the DelayedQueue works:**

It is a **Priority Queue** sorted by minimum remaining delay time. The task that is going to expire soonest is always at the **head**.

```
New task submitted: "transition to HALF-OPEN after 10s"
        │
        ▼
Placed into DelayedQueue
Sorted by delay — task with least remaining time goes to head
        │
        ▼
Worker thread looks at head of queue
"This task has 10s remaining. I can't pick it yet."
        │
        ▼
Thread goes into WAIT/BLOCK state for 10 seconds
        │
        ▼
After 10s → OS wakes up the thread
        │
        ▼
Thread picks task from head, executes it
"toHalfOpenState()" runs
        │
        ▼
Circuit moves from OPEN → HALF-OPEN ✅
```

---

### Part 6 — What If Multiple Worker Threads Are Watching?

This is the subtle part the instructor explains:

```
DelayedQueue head: [ Task1 — 10s remaining ]

Thread 1 looks → "not ready, 10s left" → goes to WAIT for 10s
Thread 2 looks → "not ready, 10s left" → goes to WAIT for 10s
Thread 3 looks → "not ready, 10s left" → goes to WAIT for 10s

After 10s → OS wakes ALL THREE threads

Only ONE thread acquires the lock and picks Task1
Other two threads → move to look at next task in queue
If next task also not ready → they WAIT again
```

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│  T1, T2, T3 all look at head [ Task1: 10s left ]     │
│        │                                             │
│        ▼                                             │
│  All go to WAIT for 10s                              │
│        │                                             │
│        ▼                                             │
│  OS wakes all 3 after 10s                            │
│        │                                             │
│        ▼                                             │
│  T1 acquires lock → picks Task1 → executes ✅         │
│  T2 → looks at next task → not ready → WAIT again    │
│  T3 → looks at next task → not ready → WAIT again    │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

### Part 7 — Interview Tips (Directly From Instructor)

The instructor explicitly calls these out for interviews:

---

**Interview Tip 1 — Circuit Breaker State Machine**

> *"If an interviewer asks how Circuit Breaker manages state transitions internally — mention `CircuitBreakerStateMachine.java`. After every call result, it checks if a state change is needed. For CLOSED → OPEN it checks if failure threshold is exceeded. Similar methods exist for each transition."*

---

**Interview Tip 2 — OPEN → HALF-OPEN Automatic Transition**

> *"A very common follow-up question: once in OPEN state, how does it automatically move to HALF-OPEN? Answer: `ScheduledThreadPoolExecutor`. When circuit enters OPEN state, a task (`toHalfOpenState`) is submitted to the scheduler with the configured wait duration as the delay."*

---

**Interview Tip 3 — How ScheduledThreadPoolExecutor Works**

> *"One level deeper: how does `ScheduledThreadPoolExecutor` work internally? Answer: it uses a `DelayedQueue` which is implemented as a `Priority Queue` sorted by minimum remaining delay time. Worker threads look at the head — if the task isn't ready, they go into WAIT/BLOCK state for that remaining delay period. OS wakes them up after the delay expires. One thread picks the task and executes it."*

---

**Interview Tip 4 — Real World Analogy (WhatsApp disappearing messages)**

> *"A popular system design interview question: how would you implement WhatsApp's disappearing messages feature (messages auto-delete after 24 hours)? Answer: DelayedQueue. Each message is a task with a 24-hour delay. When the delay expires, the message deletion task executes. Many big companies use delayed queue frameworks for exactly this kind of feature."*

---

### Complete Internal Flow — Everything Together

```
@CircuitBreaker method called
        │
        ▼
AOP Proxy intercepts
        │
        ▼
CircuitBreakerStateMachine — what state?
        │
   ┌────┴─────────────────────┐
   │                          │
CLOSED/HALF-OPEN           OPEN
   │                          │
   ▼                          ▼
Let call through          Fail immediately
   │                      Run fallback
   ▼                      (no downstream call)
Downstream call
   │
✅ or ❌
   │
   ▼
Report to StateMachine
checkIfThresholdsExceeded()
   │
   ├── threshold NOT crossed → stay CLOSED
   │
   └── threshold crossed → transitionToOpenState()
                                │
                                ▼
                    OpenState constructor runs
                                │
                                ▼
                    ScheduledThreadPoolExecutor
                    .schedule(toHalfOpenState,
                              10000ms,
                              MILLISECONDS)
                                │
                                ▼
                    Task added to DelayedQueue
                    (Priority Queue, sorted by delay)
                                │
                                ▼
                    Worker thread looks at head
                    "10s remaining — go to WAIT"
                                │
                                ▼
                    After 10s → OS wakes thread
                                │
                                ▼
                    Thread executes toHalfOpenState()
                                │
                                ▼
                    🟡 OPEN → HALF-OPEN
                                │
                    3 trial calls allowed through
                                │
                    ┌───────────┴───────────┐
                    │                       │
                100% success           any failure
                    │                       │
                    ▼                       ▼
                🟢 CLOSED              🔴 OPEN
                                    (cycle repeats)
```

---

### Complete Notes — Summary of All 7 Steps

```
Step 1 → Problem    : Repeated calls to failing downstream
                      wastes resources & delays recovery

Step 2 → Solution   : CB monitors calls, trips when threshold
                      hit, fails fast, tests recovery

Step 3 → States     : CLOSED (calls flow)
                      OPEN (calls blocked)
                      HALF-OPEN (test calls only)

Step 4 → Config     : sliding-window, minimum-calls,
                      failure-threshold, wait-duration,
                      permitted-calls-in-half-open

Step 5 → Code       : @CircuitBreaker annotation +
                      fallback method + application.properties

Step 6 → Output     : Call-by-call trace of state transitions
                      CLOSED→OPEN→HALF-OPEN→OPEN cycle

Step 7 → Internals  : AOP → StateMachine → ScheduledThreadPoolExecutor
                      → DelayedQueue (Priority Queue)
```

---

That's the **complete Circuit Breaker lecture** — every concept the instructor taught, with diagrams, code, and interview tips. 🎉