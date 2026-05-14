# Step 1 — The Big Picture: Bulkhead vs Rate Limiter

---

## Where does Bulkhead fit in Resilience4j?

Resilience4j is the library Spring Boot uses to build **fault-tolerant microservices**. It provides multiple mechanisms to protect your system:

```
                        +----------------+
                        |  Resilience4j  |
                        +----------------+
                               |
        +----------+-----------+-----------+----------+
        |          |           |           |          |
   Rate Limiter  Bulkhead    Retry   Circuit Breaker  Time Limiter
   (Part-1)     (Part-2)
                  ^
              We are here
```

In Part-1, Rate Limiter was covered. Today: **Bulkhead**.

---

## The Key Distinction — Rate Limiter vs Bulkhead

This is a very common confusion point. The instructor stresses this hard. Here's the clean mental model:

```
+------------------+------------------------------------------+------------------------------------------+
| Feature          | Rate Limiter                             | Bulkhead                                 |
+------------------+------------------------------------------+------------------------------------------+
| Protects         | YOUR application (from clients)          | YOUR downstream services (from your app) |
+------------------+------------------------------------------+------------------------------------------+
| Concerned with   | How many requests arrive per time window | How many CONCURRENT requests go OUT      |
+------------------+------------------------------------------+------------------------------------------+
| Talks about      | Request rate (count per window)          | Concurrency (simultaneous threads)       |
| concurrency?     | NO                                       | YES                                      |
+------------------+------------------------------------------+------------------------------------------+
| Direction        | Inbound (Client --> Your App)            | Outbound (Your App --> Downstream)       |
+------------------+------------------------------------------+------------------------------------------+
| Example          | Accept max 10 requests per minute        | Send max 3 concurrent calls to Product   |
+------------------+------------------------------------------+------------------------------------------+
```

### Visualizing the Direction Difference

```
                        RATE LIMITER
                        ============

    Client  ----[too many requests?]---->  Order Service  ----->  Product Service
                      ^
                      |
              Rate Limiter sits HERE
              (guards your app's door)
              "I accept only 10 req/min"
              No concern about concurrency.
              10 req can come all at once
              or one by one — doesn't matter.


                        BULKHEAD
                        ========

    Client  ------>  Order Service  ----[too many concurrent calls?]---->  Product Service
                                                   ^
                                                   |
                                        Bulkhead sits HERE
                                        (guards the outbound calls)
                                        "I send only 3 concurrent
                                         requests to downstream"
```

---

## The Ship Analogy — Why "Bulkhead"?

The name comes directly from ship design. The instructor explains it like this:

```
        SHIP (Top View)
        ===============

   +--------+--------+--------+
   |        |        |        |
   | Comp-1 | Comp-2 | Comp-3 |   <-- Compartments separated by walls (bulkheads)
   |        |  WATER |        |
   |        | FILLED |        |
   +--------+--------+--------+
                ^
                |
         Water enters Comp-2.
         The bulkhead walls BLOCK it
         from spilling into Comp-1 & Comp-3.
         Ship stays afloat.
```

In microservices: one slow downstream service filling up all your threads = water flooding one compartment. The Bulkhead pattern **blocks that flood from spreading** to the rest of your application.

---

## Two Types of Bulkhead

There are exactly two types, each solving a different use case:

```
                    +------------+
                    |  Bulkhead  |
                    +------------+
                          |
            +-------------+-------------+
            |                           |
  +-----------------+         +--------------------+
  | Semaphore        |         | Thread Pool        |
  | Bulkhead         |         | Bulkhead           |
  +-----------------+         +--------------------+
  | Controls max     |         | Assigns a DEDICATED|
  | concurrent calls |         | thread pool to a   |
  | using a counter  |         | specific downstream|
  | (semaphore lock) |         | service/endpoint   |
  +-----------------+         +--------------------+
  | Use when:        |         | Use when:          |
  | Downstream is    |         | One slow endpoint  |
  | lightweight &    |         | should NOT starve  |
  | has a hard limit |         | other endpoints    |
  | on concurrency   |         |                    |
  +-----------------+         +--------------------+
```

Each type maps to a specific real-world use case, which we'll go deep into in Steps 2 and 3.

---

That's the full big picture. The key takeaway before moving forward:

- Rate Limiter = **inbound protection** (from clients), cares about **rate, not concurrency**
- Bulkhead = **outbound protection** (to downstream), cares about **concurrency**
- Two types of Bulkhead: **Semaphore** (use case 1) and **Thread Pool** (use case 2)

---

# Step 2 — Semaphore Bulkhead

---

## The Problem it Solves

Let's say your Order service is calling a Product service downstream. Now this Product service is a small, lightweight service. It can only handle **3 concurrent requests** at a time — not because of any rate limit it put on itself, but simply because it's a small service with limited capacity.

So what happens if your Order service sends 4, 5, or 10 concurrent requests to it? Product service gets overwhelmed. It has no protection on its own side. So **you** — the caller — need to be responsible. You need to make sure you never send more than 3 concurrent requests to it at any point in time.

This is exactly the problem Semaphore Bulkhead solves.

```
Order Service                        Product Service
=============                        ===============

Request 1  -------->                 [Can handle only
Request 2  -------->  (3 allowed)     3 concurrent
Request 3  -------->                  requests]

Request 4  ----> BLOCKED or REJECTED immediately
                 (no slot available)
```

---

## How it Works Internally

The word "Semaphore" comes directly from the internal mechanism it uses — a **Semaphore Lock**.

Think of it like a counter with a fixed number of permits. Let's say the limit is 3.

```
Semaphore Permit Counter
========================

Initial state:  [Permit 1] [Permit 2] [Permit 3]  --> 3 permits available

Request 1 arrives --> takes Permit 1 --> [Permit 2] [Permit 3] remaining
Request 2 arrives --> takes Permit 2 --> [Permit 3] remaining
Request 3 arrives --> takes Permit 3 --> [] no permits left

Request 4 arrives --> no permit available -->
                      either WAIT (if maxWaitDuration > 0)
                      or REJECTED immediately (if maxWaitDuration = 0)
                      --> goes to fallback method

Request 1 finishes --> returns Permit 1 --> [Permit 1] available again
Request 4 can now proceed.
```

When a thread enters the critical section (the place in your code where you call the downstream), it acquires a permit. When it finishes, it releases the permit. The semaphore counter manages this automatically. This is all handled internally by **AOP (Aspect Oriented Programming)** at runtime — you just put an annotation and Spring handles the rest.

The "critical section" here is simply the method where you are invoking the downstream API.

---

## How AOP Fits In

You don't write any semaphore locking code yourself. When Spring sees the `@Bulkhead` annotation on your method, AOP generates a proxy around that method at runtime. That proxy wraps your method call with the semaphore logic — checking the counter, allowing or rejecting the call, and routing to the fallback if rejected. You write normal code, AOP does all the heavy lifting.

```
Your Code                  AOP Proxy (generated at runtime)
=========                  ================================

invokeProductAPI()  ---->  Check semaphore counter
                           |
                           |-- permit available? --> call actual method --> return response
                           |
                           |-- no permit + wait time = 0? --> reject --> call fallbackMethod()
                           |
                           |-- no permit + wait time > 0? --> wait --> retry or reject
```

---

## Dependency

Only one dependency needed. Resilience4j covers Rate Limiter, Bulkhead, Retry, Circuit Breaker — all of it under one single dependency:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## Full Code Implementation

**Controller — nothing special here, just a normal REST controller:**

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

**Feign Client — how Order calls Product:**

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping(value = "/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

**Service — this is where Bulkhead annotation lives:**

```java
@Component
public class OrderService {

    @Autowired
    ProductClient productClient;

    @Bulkhead(
        name = "productService",          // name of this bulkhead instance
        type = Bulkhead.Type.SEMAPHORE,   // we want semaphore type
        fallbackMethod = "productFallback" // called when limit is breached
    )
    public void invokeProductAPI(String id) {
        // THIS is the critical section.
        // Only maxConcurrentCalls threads can be here at the same time.
        String response = productClient.getProductById(id);
        System.out.println("Response from Product api call is: " + response);
    }

    // fallback method — same signature as above + Throwable parameter
    public void productFallback(String id, Throwable t) {
        System.out.println("Too many concurrent requests, please try again later");
        // throw a proper exception here and handle it gracefully in production
    }
}
```

Two things to note about the fallback method:
- It must have the **same method signature** as the original method.
- It takes one **extra `Throwable` parameter** at the end.

---

## Configuration — application.properties

```properties
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Only 2 concurrent calls allowed at a time.
# If no thread available, request fails immediately (maxWaitDuration=0)
resilience4j.bulkhead.instances.productService.maxConcurrentCalls=2
resilience4j.bulkhead.instances.productService.maxWaitDuration=0
```

Notice that the instance name `productService` here must match exactly the `name` you gave in the `@Bulkhead` annotation.

---

## Understanding maxWaitDuration

This controls what happens when the semaphore limit is already reached and a new request comes in:

```
maxWaitDuration = 0
====================
New request arrives, no permit available
--> Rejected IMMEDIATELY
--> Goes to fallback right away
--> No waiting at all


maxWaitDuration = 500ms (or 2s, or 1m)
========================================
New request arrives, no permit available
--> WAITS for that duration
--> If a permit becomes free within that time --> proceeds normally
--> If still no permit after wait time is over --> rejected --> goes to fallback
```

Accepted time unit formats:

```
0       --> reject immediately
300ms   --> wait 300 milliseconds
2s      --> wait 2 seconds
1m      --> wait 1 minute
1h      --> wait 1 hour
```

---

## Full Picture — Semaphore Bulkhead

```
Client
  |
  v
OrderController.callProductAPI()
  |
  v
AOP Proxy intercepts invokeProductAPI()
  |
  v
Semaphore Counter check
  |
  |-- permits available?
  |     YES --> enter critical section --> call ProductClient --> return response
  |
  |-- no permits + maxWaitDuration = 0?
  |     YES --> reject immediately --> productFallback() called
  |
  |-- no permits + maxWaitDuration > 0?
        YES --> wait --> permit freed in time? --> proceed
                     --> permit NOT freed in time? --> productFallback() called
```

---

## When to use Semaphore Bulkhead — Interview Tip

Whenever an interviewer gives you a scenario like:

"You have a downstream service that is small and lightweight and can only handle N concurrent requests. How do you make sure your service respects that limit?"

Your answer: **Semaphore Bulkhead**. It limits concurrent calls to the downstream using a counter (semaphore lock). The N+1th request is either queued for a wait duration or rejected immediately, depending on your `maxWaitDuration` config.

---
# Step 3 — Thread Pool Bulkhead

---

## The Problem it Solves

Before understanding Thread Pool Bulkhead, the instructor wants you to recall a classic system design problem called the **Noisy Neighbor Problem**.

The Noisy Neighbor problem says: imagine you have one application serving multiple tenants. One tenant generates so much traffic that it consumes ALL the shared resources — threads, DB connections, infrastructure, everything. Because of this one noisy tenant, other tenants who are sending perfectly normal traffic start getting their requests rejected. They suffer because of someone else.

Thread Pool Bulkhead solves a very similar problem, but in a narrower context. Here we are only talking about **threads**, not DB or infrastructure. Keep that distinction in mind.

---

## The Concrete Use Case

Your Order service has two endpoints:

```
Order Service
=============

API 1  ----calls---->  Product Service   (FAST — responds in ~100ms)

API 2  ----calls---->  Payment Service   (SLOW — responds in ~5 seconds)
```

Now imagine there is a **sudden spike in traffic to API 2**. API 2 calls Payment Service, which is slow. Each call blocks a thread for 5 seconds. More and more API 2 requests come in, more and more threads get blocked, sitting idle waiting for Payment Service to respond.

Your Order service has a thread pool — let's say 10 threads total. With enough API 2 traffic, all 10 threads are now blocked waiting for Payment Service.

```
Order Service Thread Pool (total 10 threads)
============================================

Thread 1  -->  blocked, waiting for Payment Service (5s)
Thread 2  -->  blocked, waiting for Payment Service (5s)
Thread 3  -->  blocked, waiting for Payment Service (5s)
Thread 4  -->  blocked, waiting for Payment Service (5s)
Thread 5  -->  blocked, waiting for Payment Service (5s)
Thread 6  -->  blocked, waiting for Payment Service (5s)
Thread 7  -->  blocked, waiting for Payment Service (5s)
Thread 8  -->  blocked, waiting for Payment Service (5s)
Thread 9  -->  blocked, waiting for Payment Service (5s)
Thread 10 -->  blocked, waiting for Payment Service (5s)

New request comes in for API 1 (Product Service - fast!)
--> NO THREAD AVAILABLE
--> Request rejected or forced to wait
--> API 1 suffers even though Product Service is perfectly healthy
    and API 1 has very little traffic
```

API 1 is completely innocent here. It calls a fast service. It has low traffic. But it gets killed because API 2 consumed all threads. This is the noisy neighbor problem, but scoped to threads only.

---

## The Solution

The fix is to give API 2 its own **dedicated thread pool**, completely separate from the main Order service thread pool. You put a hard ceiling on how many threads API 2 can ever use.

```
Order Service
=============

Main thread pool (handles incoming requests)
  |
  |--- API 1 request arrives --> uses main thread --> calls Product Service directly
  |
  |--- API 2 request arrives --> handed off to API 2's DEDICATED thread pool
                                        |
                              +---------+---------+
                              | Dedicated Pool    |
                              | for Payment Svc   |
                              | Max: 5 threads    |
                              | Queue: 4 slots    |
                              +---------+---------+
                                        |
                              If pool full + queue full
                                        |
                                   REJECTED
                                   (fallback)
```

Now even if Payment Service is slow and all 5 dedicated threads are blocked — API 1 is completely unaffected. It still has the main thread pool available. The damage is contained, just like the ship compartments.

---

## How it Works Internally — AOP + CompletableFuture

This is where Thread Pool Bulkhead gets more technical than Semaphore Bulkhead, and the instructor goes deep here.

With Semaphore Bulkhead, AOP just wrapped your method with a counter check. The method still ran on the **same thread** that received the request.

With Thread Pool Bulkhead, AOP does something fundamentally different. It **submits your entire method body as a task to a separate dedicated thread pool**. Internally, what AOP is doing looks like this:

```java
// What AOP generates internally (you never write this yourself):

CompletableFuture.supplyAsync(() -> {
    return yourMethodLogic();   // your entire method body runs here
}, bulkheadThreadPoolExecutor); // uses the DEDICATED thread pool, not the default one
```

So your method no longer runs on the request-handling thread. It gets submitted to the dedicated thread pool as a task. The request-handling thread is freed up immediately after submitting the task.

Because of this, there is one very important rule:

**When using Thread Pool Bulkhead, your method's return type MUST be `CompletableFuture<T>`.**

AOP submits the task and returns a `CompletableFuture` to the caller. So your method signature must match that.

---

## The completedFuture vs supplyAsync Distinction — Very Important

Inside your method body, you wrap your result like this:

```java
return CompletableFuture.completedFuture(productClient.getProductById(id));
```

The instructor is very clear about why you use `completedFuture` here and NOT `supplyAsync`:

```
CompletableFuture.supplyAsync(() -> someTask())
=================================================
This SUBMITS the task to a thread pool.
If YOU write supplyAsync inside your method,
YOUR code is doing the thread pool submission.
It will use the DEFAULT common thread pool of your Order service,
NOT the dedicated bulkhead thread pool.
So the whole purpose of Thread Pool Bulkhead is defeated.


CompletableFuture.completedFuture(someResult)
==============================================
This does NOT submit anything to any thread pool.
It is just a WRAPPER.
It takes an already-computed result and wraps it
inside a CompletableFuture object.
That's all.

AOP is the one doing the actual supplyAsync + passing
the dedicated bulkhead thread pool executor.
Your job inside the method is just to wrap the result.
```

So the rule is: **inside your `@Bulkhead` THREADPOOL method, always use `completedFuture`, never `supplyAsync`.**

If you mistakenly use `supplyAsync` inside, your task goes to the common default thread pool, not the bulkhead's dedicated one. The bulkhead does nothing useful in that case.

---

## Full Code Implementation

**Controller — same as before, no changes:**

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

**Feign Client — same as before:**

```java
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping(value = "/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

**Service — notice the differences from Semaphore version:**

```java
@Component
public class OrderService {

    @Autowired
    ProductClient productClient;

    /*
     * AOP proxy intercepts this method call.
     * It submits the entire method body to the Bulkhead's
     * dedicated thread pool (configured in application.properties).
     *
     * Internally AOP does:
     * CompletableFuture.supplyAsync(() -> {
     *     return ourMethodLogic();
     * }, bulkheadThreadPoolExecutor);
     *
     * Inside our method, CompletableFuture.completedFuture(...)
     * just WRAPS the result — it does NOT submit to any thread pool.
     */
    @Bulkhead(
        name = "productService",
        type = Bulkhead.Type.THREADPOOL,      // <-- THREADPOOL type now
        fallbackMethod = "productFallback"
    )
    public CompletableFuture<String> invokeProductAPI(String id) {
        // Print thread name to verify it's using bulkhead's dedicated pool
        System.out.println("Thread name is: " + Thread.currentThread().getName());

        // completedFuture = just a wrapper, NOT submitting to any thread pool
        return CompletableFuture.completedFuture(
            productClient.getProductById(id)
        );
    }

    // Fallback must also return CompletableFuture<String> — must match!
    public CompletableFuture<String> productFallback(String id, Throwable t) {
        // Without this fallback, client gets 500 with BulkheadFullException
        System.out.println("Product Service is busy");
        return CompletableFuture.completedFuture("Product Service is busy");
    }
}
```

Key differences from Semaphore version:
- `type = Bulkhead.Type.THREADPOOL` instead of `SEMAPHORE`
- Return type is `CompletableFuture<String>` instead of `void`
- Inside the method, result is wrapped with `completedFuture`
- Fallback method also returns `CompletableFuture<String>`

---

## Configuration — application.properties

```properties
server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# max 3 threads in the pool, that's also the max pool size
# if all 3 threads are busy, max 2 more requests go into the queue
# from the 6th request onwards, requests get rejected
resilience4j.thread-poolbulkhead.instances.productService.coreThreadPoolSize=3
resilience4j.thread-poolbulkhead.instances.productService.maxThreadPoolSize=3
resilience4j.thread-poolbulkhead.instances.productService.queueCapacity=2
```

Note: for Thread Pool Bulkhead the property prefix is `resilience4j.thread-poolbulkhead` — different from Semaphore which used `resilience4j.bulkhead`.

---

## Understanding the Thread Pool Configuration

```
Thread Pool for productService
===============================

coreThreadPoolSize = 3   --> threads always kept alive in the pool
maxThreadPoolSize  = 3   --> maximum threads pool can ever grow to
queueCapacity      = 2   --> how many tasks can wait in queue
                             when all threads are busy


How it behaves step by step:
=============================

Requests 1, 2, 3 arrive
  --> Thread 1 picks Request 1
  --> Thread 2 picks Request 2
  --> Thread 3 picks Request 3
  --> All 3 threads now busy

Request 4 arrives
  --> No thread free
  --> Goes into Queue (slot 1)

Request 5 arrives
  --> No thread free
  --> Goes into Queue (slot 2)
  --> Queue is now FULL

Request 6 arrives
  --> No thread free
  --> Queue is full
  --> maxThreadPoolSize already reached (3/3)
  --> REJECTED --> productFallback() called

Thread 1 finishes
  --> Picks Request 4 from Queue

Thread 2 finishes
  --> Picks Request 5 from Queue
```

---

## Output Walkthrough — What the instructor showed

When the API was called 6 times with thread pool size 3 and queue size 2:

```
Console Output:
===============

Thread name is: bulkhead-productService-1   <-- Request 1, dedicated pool thread
Thread name is: bulkhead-productService-2   <-- Request 2, dedicated pool thread
Thread name is: bulkhead-productService-3   <-- Request 3, dedicated pool thread

(Requests 4 and 5 inserted into queue)

Product Service is busy                     <-- Request 6 rejected, fallback called

(Thread 1 frees up, picks Request 4 from queue)
Thread name is: bulkhead-productService-1

(Thread 2 frees up, picks Request 5 from queue)
Thread name is: bulkhead-productService-2
```

The thread names confirm two things: the dedicated bulkhead thread pool is being used (not the Order service's default pool), and the queue + rejection behavior is working exactly as configured.

---

## Semaphore vs Thread Pool — Side by Side

```
+----------------------+------------------------+---------------------------+
| Aspect               | Semaphore Bulkhead     | Thread Pool Bulkhead      |
+----------------------+------------------------+---------------------------+
| Controls             | Concurrent call count  | Thread resource isolation |
+----------------------+------------------------+---------------------------+
| Use when             | Downstream has hard    | One slow endpoint should  |
|                      | concurrency limit      | not starve others         |
+----------------------+------------------------+---------------------------+
| Internal mechanism   | Semaphore lock         | Dedicated ThreadPool      |
|                      | (counter of permits)   | + CompletableFuture       |
+----------------------+------------------------+---------------------------+
| Return type          | Normal (void, String)  | CompletableFuture<T>      |
+----------------------+------------------------+---------------------------+
| Same thread?         | YES - runs on the      | NO - method submitted     |
|                      | calling thread         | to dedicated pool thread  |
+----------------------+------------------------+---------------------------+
| Config prefix        | resilience4j.bulkhead  | resilience4j.             |
|                      |                        | thread-poolbulkhead       |
+----------------------+------------------------+---------------------------+
| Key config params    | maxConcurrentCalls     | coreThreadPoolSize        |
|                      | maxWaitDuration        | maxThreadPoolSize         |
|                      |                        | queueCapacity             |
+----------------------+------------------------+---------------------------+
```

---

## When to use Thread Pool Bulkhead — Interview Tip

Whenever an interviewer gives you a scenario like:

"You have multiple endpoints in your service. One endpoint calls a slow downstream. How do you make sure that slow endpoint doesn't consume all threads and bring down your other endpoints?"

Your answer: **Thread Pool Bulkhead**. Assign a dedicated thread pool to that slow downstream call. Even if all threads in that dedicated pool get blocked, the rest of your service keeps running on its own thread pool completely unaffected.

Also remember: the instructor says to be very careful about **what code you put inside the `@Bulkhead` THREADPOOL method**. Since AOP submits the entire method body to the dedicated thread pool, you should only put the downstream call inside. Don't mix in unrelated business logic or validation there.

---
# Step 4 — Time Limiter + Interview Tips + Quick Reference

---

## Time Limiter — Why it's skipped for now

The instructor covers this briefly just so you know it exists and understand why it's being deferred.

### What is it?

Time Limiter is used to prevent an **async call from hanging indefinitely**. Imagine you fired off an async task to a thread pool — that thread is now working in the background. How do you make sure that thread doesn't keep running forever without ever finishing? That's what Time Limiter handles.

### Why not cover it now?

The instructor makes a very clear distinction here:

For **blocking calls** — like RestTemplate, RestClient, and FeignClient — threads wait for the response. But they don't hang indefinitely because you already control that through connection timeout and read timeout directly in your client config:

```properties
# FeignClient timeout config example
feign.client.config.product-service.connectTimeout=3000
feign.client.config.product-service.readTimeout=5000
```

Connection timeout = how long to wait to establish the HTTP/TCP connection.
Read timeout = how long to wait before getting a response from downstream.

So for blocking calls, timeout is already handled. Time Limiter is not needed there.

Time Limiter is specifically designed for **async, non-blocking calls** — the kind that return reactive types like `Mono` or `Flux` in Spring WebFlux. That's a completely different programming model. The instructor will cover Time Limiter properly when the WebFlux / Reactive Programming series begins. Covering it now without that context would not make sense.

```
Blocking calls                         Async / Reactive calls
(RestTemplate, FeignClient)            (WebFlux - Mono, Flux)
===========================            ======================
Timeout handled via                    Timeout handled via
connectTimeout + readTimeout           Time Limiter
in client config                       (Resilience4j)

Already covered. Done.                 Will cover with WebFlux.
```

---

## Interview Tips — All in One Place

Here are all the interview-relevant points the instructor flagged across the entire lecture:

**Tip 1 — Rate Limiter vs Bulkhead distinction**

This is the most commonly confused pair in interviews. The moment an interviewer gives you a scenario, ask yourself two questions: Is the concern about requests coming IN to my service, or requests going OUT to a downstream? Is the concern about rate (count per window) or concurrency (simultaneous threads)? Rate Limiter = inbound + rate. Bulkhead = outbound + concurrency.

**Tip 2 — Which Bulkhead type to use**

If the scenario says: "downstream service can only handle N concurrent requests" — answer is Semaphore Bulkhead.

If the scenario says: "one slow endpoint is consuming all threads and affecting other endpoints" — answer is Thread Pool Bulkhead.

These two use cases are the clearest signals. Once you recognise which one is being described, the answer is immediate.

**Tip 3 — Noisy Neighbor connection**

Thread Pool Bulkhead solves the same conceptual problem as the Noisy Neighbor pattern in system design, but scoped only to threads within a single service. If an interviewer brings up Noisy Neighbor in a microservices context and asks how you handle it at the thread level — Thread Pool Bulkhead is your answer.

**Tip 4 — completedFuture vs supplyAsync**

If asked about Thread Pool Bulkhead implementation, this is a very likely follow-up. Inside a `@Bulkhead(type=THREADPOOL)` method, you must use `CompletableFuture.completedFuture()` to wrap your result, not `supplyAsync()`. Because AOP is already doing the `supplyAsync` + passing the dedicated executor internally. If you call `supplyAsync` yourself, you bypass the bulkhead's thread pool entirely and use the default common pool instead.

**Tip 5 — Return type rule**

Semaphore Bulkhead → normal return type (void, String, etc.)
Thread Pool Bulkhead → return type must be `CompletableFuture<T>`
Fallback method → must match the original method signature + one extra `Throwable` parameter at the end.

**Tip 6 — AOP handles everything**

Both bulkhead types are fully managed by Spring AOP at runtime via proxy. You never write semaphore locking code or thread pool submission code yourself. The annotation + application.properties configuration is all you need. AOP reads the annotation, looks up the config by the instance name, and generates the appropriate proxy code.

---

## Quick Reference — Full Bulkhead Cheatsheet

### Concept Summary

```
Bulkhead
========
Purpose  : Control concurrent outbound calls to downstream services
Direction: Outbound (Your service --> Downstream)
Concern  : Concurrency (not rate)
Library  : Resilience4j (resilience4j-spring-boot3)
Managed  : Spring AOP (annotation-driven, proxy-based)
```

### Two Types at a Glance

```
+---------------------+-----------------------------+--------------------------------+
| Feature             | Semaphore Bulkhead          | Thread Pool Bulkhead           |
+---------------------+-----------------------------+--------------------------------+
| Problem solved      | Downstream has hard         | Slow endpoint starving         |
|                     | concurrency limit           | other endpoints                |
+---------------------+-----------------------------+--------------------------------+
| Internal mechanism  | Semaphore lock (counter)    | Dedicated ThreadPoolExecutor   |
+---------------------+-----------------------------+--------------------------------+
| Runs on             | Same calling thread         | Dedicated pool thread          |
+---------------------+-----------------------------+--------------------------------+
| Return type         | Normal (void, String, etc.) | CompletableFuture<T>           |
+---------------------+-----------------------------+--------------------------------+
| Annotation type     | Bulkhead.Type.SEMAPHORE     | Bulkhead.Type.THREADPOOL       |
+---------------------+-----------------------------+--------------------------------+
| Config prefix       | resilience4j.bulkhead       | resilience4j.thread-poolbulkhead|
+---------------------+-----------------------------+--------------------------------+
| Key configs         | maxConcurrentCalls          | coreThreadPoolSize             |
|                     | maxWaitDuration             | maxThreadPoolSize              |
|                     |                             | queueCapacity                  |
+---------------------+-----------------------------+--------------------------------+
```

### Annotation Syntax

```java
// Semaphore Bulkhead
@Bulkhead(
    name = "instanceName",
    type = Bulkhead.Type.SEMAPHORE,
    fallbackMethod = "fallbackMethodName"
)

// Thread Pool Bulkhead
@Bulkhead(
    name = "instanceName",
    type = Bulkhead.Type.THREADPOOL,
    fallbackMethod = "fallbackMethodName"
)
```

### application.properties — Semaphore

```properties
resilience4j.bulkhead.instances.{instanceName}.maxConcurrentCalls=3
resilience4j.bulkhead.instances.{instanceName}.maxWaitDuration=0
```

### application.properties — Thread Pool

```properties
resilience4j.thread-poolbulkhead.instances.{instanceName}.coreThreadPoolSize=3
resilience4j.thread-poolbulkhead.instances.{instanceName}.maxThreadPoolSize=3
resilience4j.thread-poolbulkhead.instances.{instanceName}.queueCapacity=2
```

### maxWaitDuration values (Semaphore only)

```
0       --> reject immediately
300ms   --> wait 300 milliseconds
2s      --> wait 2 seconds
1m      --> wait 1 minute
1h      --> wait 1 hour
```

### Fallback method rules

```
1. Same method name as specified in fallbackMethod attribute
2. Same parameters as original method
3. One extra Throwable parameter at the end
4. Same return type as original method
   (CompletableFuture<T> for Thread Pool, normal type for Semaphore)

// Semaphore fallback example
public void productFallback(String id, Throwable t) { }

// Thread Pool fallback example
public CompletableFuture<String> productFallback(String id, Throwable t) { }
```

### Thread Pool Bulkhead — Request Flow

```
Request arrives
     |
     v
AOP intercepts @Bulkhead(THREADPOOL) method
     |
     v
Submits method as task to dedicated thread pool
     |
     +---> Thread available in pool?
     |         YES --> task runs on that thread --> result wrapped in completedFuture
     |
     +---> No thread, but queue has space?
     |         YES --> task queued --> picked up when thread frees
     |
     +---> No thread, queue full, max pool reached?
               YES --> REJECTED --> fallbackMethod() called
```

### Time Limiter — One liner

```
Blocking calls  --> use connectTimeout + readTimeout in client config
Async calls     --> use Time Limiter (will cover with Spring WebFlux)
```

---

That's the complete lecture on Bulkhead — all 4 steps done! You now have the full picture from concept to code to configuration to interview readiness. Good luck with the next lecture, James!