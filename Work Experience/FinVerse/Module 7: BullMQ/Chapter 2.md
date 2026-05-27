# BullMQ — Chapter 2: Node.js Execution Model

---

## Why This Chapter Exists

Every time BullMQ documentation says "concurrency: 10" or "worker process" or "non-blocking", it assumes you already have a mental model of how Node.js executes code. Without that model, these terms are just words.

You already have a very solid mental model — but it's the Java one. This chapter's job is to map every concept you know from Java onto its Node.js equivalent, show you where the mapping breaks down, and build a precise picture of what's actually happening when BullMQ runs jobs.

---

## Start With What You Know: The Java Thread Model

In Java, when your Spring Boot application starts, the JVM creates threads. Each thread is a real OS thread — the operating system knows about it, schedules it, and can run it on a physical CPU core.

```
JAVA / SPRING BOOT PROCESS
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Main Thread        Thread-1         Thread-2       Thread-3    │
│  ┌──────────┐      ┌──────────┐     ┌──────────┐  ┌─────────┐   │
│  │          │      │          │     │          │  │         │   │
│  │  Boots   │      │ Handles  │     │ Handles  │  │ @Async  │   │
│  │  app     │      │ Request A│     │ Request B│  │  job    │   │
│  │          │      │          │     │          │  │         │   │
│  └──────────┘      └──────────┘     └──────────┘  └─────────┘   │
│                                                                 │
│  Each is a real OS thread.                                      │
│  All four can run simultaneously on four CPU cores.             │
│  TRUE PARALLELISM.                                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

When Request A comes in, Tomcat picks a thread from its thread pool and hands the request to it. That thread runs your controller, your service, your repository — all the way through. If it hits a database call, that thread **blocks and waits** for the database to respond. It does nothing else while waiting. When the database responds, the thread resumes.

This is called **blocking I/O**. The thread is allocated to the request for its entire duration, even during waits.

The consequence: if your thread pool has 200 threads and 200 slow database calls arrive simultaneously, thread 201 waits. This is why tuning Tomcat thread pool size matters in Java services.

---

## Now The Node.js Model: One Thread, Never Blocks

Node.js takes a completely different approach. There is **one main thread**. One. It runs all your JavaScript code — your controllers, your service logic, your database queries, everything.

"But wait," you're thinking. "If there's only one thread and it hits a database call, doesn't everything freeze?"

No. And this is the core insight.

```
NODE.JS PROCESS
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    ONE THREAD — Event Loop                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                                                          │   │
│  │  "Do I have any JavaScript code to run right now?"       │   │
│  │  YES → run it                                            │   │
│  │  NO  → wait for something to happen                      │   │
│  │                                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │        ▲                            │
│              "go do       │        │  "I/O done,                │
│               this I/O"   │        │   here's result"           │
│                           ▼        │                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              libuv (C library, NOT JavaScript)           │   │
│  │                                                          │   │
│  │  Manages ALL I/O operations using OS primitives          │   │
│  │                                                          │   │
│  │  PostgreSQL query ──► OS handles it                      │   │
│  │  GoCardless HTTP  ──► OS handles it                      │   │
│  │  Redis read       ──► OS handles it                      │   │
│  │  File read        ──► OS handles it                      │   │
│  │                                                          │   │
│  │  All running concurrently at the OS level                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

When your Node.js code hits `await prisma.transaction.findMany(...)`, it does not sit and wait. It hands the database query to libuv, which hands it to the OS, and immediately returns control to the event loop. The event loop picks up the next thing in its queue. When the database responds, the OS notifies libuv, which puts a callback into the event loop's queue. When the event loop gets to that callback, your code resumes from where it left off.

**This is non-blocking I/O.** The single thread is never sitting idle. It's always doing something — either running JavaScript code, or waiting for callbacks to arrive (during which time the OS is doing the actual I/O work).

---

## The Side-By-Side That Makes This Click

```
┌─────────────────────────────────────────────────────────────────────┐
│                    HANDLING 3 CONCURRENT DB CALLS                   │
├────────────────────────────┬────────────────────────────────────────┤
│         JAVA               │              NODE.JS                   │
├────────────────────────────┼────────────────────────────────────────┤
│                            │                                        │
│  Thread-1: DB call ──────► │  Event Loop: DB call 1 ─────────────►  │
│  Thread-1: WAITING...      │  Event Loop: immediately continues     │
│  Thread-1: WAITING...      │  Event Loop: DB call 2 ─────────────►  │
│  Thread-1: WAITING...      │  Event Loop: immediately continues     │
│                            │  Event Loop: DB call 3 ─────────────►  │
│  Thread-2: DB call ──────► │  Event Loop: immediately continues     │
│  Thread-2: WAITING...      │                                        │
│  Thread-2: WAITING...      │  All 3 queries running at OS level     │
│  Thread-2: WAITING...      │  simultaneously                        │
│                            │                                        │
│  Thread-3: DB call ──────► │  OS: DB1 responds ──► callback queued  │
│  Thread-3: WAITING...      │  OS: DB2 responds ──► callback queued  │
│  Thread-3: WAITING...      │  OS: DB3 responds ──► callback queued  │
│  Thread-3: WAITING...      │                                        │
│                            │  Event Loop: runs callback 1           │
│  3 threads sitting idle    │  Event Loop: runs callback 2           │
│  3 OS threads consumed     │  Event Loop: runs callback 3           │
│  3 × memory allocated      │                                        │
│                            │  1 thread, 3 × no memory overhead      │
└────────────────────────────┴────────────────────────────────────────┘
```

Java uses threads to achieve concurrency. Multiple threads can literally run simultaneously on multiple CPU cores — true parallelism.

Node.js uses the event loop to achieve concurrency. One thread handles many operations simultaneously by never waiting for any of them — concurrent I/O, but not parallel execution of JavaScript code.

**The practical consequence:**
- Java is naturally good at CPU-intensive work (multiple threads crunching numbers in parallel)
- Node.js is naturally good at I/O-intensive work (one thread managing thousands of concurrent network calls without blocking)

---

## The Part Nobody Explains: libuv Has Its Own Thread Pool

Here is something most Node.js tutorials skip entirely, and it matters for understanding BullMQ.

libuv — the C library that handles all I/O under Node.js — actually has its own small thread pool. By default, it has **4 threads**.

This thread pool is not for your JavaScript code. You never interact with it directly. It's used by libuv for specific operations that don't have a truly async OS API — like DNS lookups, file system operations, and some crypto operations.

```
NODE.JS PROCESS (full picture)
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Your JavaScript (ONE thread)                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Event Loop                                               │  │
│  │  Runs all your async/await code                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  libuv                                                    │  │
│  │                                                           │  │
│  │  Network I/O ──────────────────► OS kernel (async)        │  │
│  │  (HTTP, TCP, Redis, PostgreSQL)                           │  │
│  │                                                           │  │
│  │  File I/O, DNS, Crypto ────────► libuv thread pool        │  │
│  │                                  (4 threads by default)   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Why does this matter for BullMQ? Because when BullMQ talks to Redis — which it does constantly for every job operation — that's a network I/O call. It goes through the OS kernel directly, completely asynchronously, without touching the libuv thread pool. BullMQ workers can handle many concurrent jobs because Redis communication is pure async network I/O — exactly what Node.js is designed for.

---

## Where Node.js Breaks Down: CPU-Bound Work

Everything above works beautifully for I/O-bound work. But there is one scenario where the single-thread model completely falls apart: **CPU-bound computation**.

CPU-bound means work where the CPU is continuously busy calculating — no I/O waits, just arithmetic, loops, data transformation.

```
EVENT LOOP BLOCKED BY CPU WORK

Time ──────────────────────────────────────────────────────────────►

[Handle Request A] [CPU-HEAVY CALCULATION....................BLOCKS]
                                        ▲
                     During this entire time:
                     - Request B is waiting
                     - Request C is waiting
                     - Redis callbacks are waiting
                     - EVERYTHING IS FROZEN
```

When CPU-bound work runs on the event loop thread, it blocks everything. The event loop cannot check for callbacks, cannot handle new requests, cannot do anything — until the computation finishes.

**Examples of CPU-bound work:**
- Portfolio valuation — computing current value, return percentage for thousands of users
- Tax report generation — applying country-specific tax rules across a full year of transactions
- PDF generation — rendering and encoding

**This is exactly why FinVerse's Market Data Service is written in Go.** Portfolio valuation is CPU-bound numeric computation. Running it in Node.js would block the event loop and degrade the entire service. Go handles CPU-bound concurrent work naturally — goroutines, real OS threads, no event loop to worry about.

For the Core Product Service (NestJS), all BullMQ jobs are I/O-bound — they call GoCardless, read from PostgreSQL, write to Redis. This is the sweet spot for Node.js.

---

## Worker Threads: The Honest Production Story

Worker Threads were introduced in Node.js 10 and stabilised in Node.js 12. They give you real OS threads — actual parallelism — from within a Node.js process. They can run JavaScript code in parallel on separate CPU cores.

"So doesn't this solve the CPU problem?" Yes, it does — in theory.

Here's what a Worker Thread looks like:

```
NODE.JS PROCESS WITH WORKER THREADS
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Main Thread (Event Loop)                                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Your regular Node.js code                                │  │
│  └──────────────────┬────────────────────────────────────────┘  │
│                     │  new Worker('./heavy-task.js')            │
│                     │  postMessage(data)                        │
│                     ▼                                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Worker Thread 1 (real OS thread)                         │  │
│  │  Runs heavy-task.js in complete isolation                 │  │
│  │  Has its own event loop, its own V8 instance              │  │
│  │  Communicates via messages (like postMessage in browser)  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Why aren't Worker Threads the standard solution in production?**

Three reasons:

**Reason 1 — Spinning up Worker Threads repeatedly is expensive.** Each Worker Thread creates its own V8 JavaScript engine instance — that's not cheap. If you create a new worker thread per request, you've recreated the same problem that thread pools in Java solve. The answer is to use a pool — but now you're managing a thread pool in Node.js, which is more complex than in Java.

**Reason 2 — Teams solve CPU-bound work differently.** Instead of Worker Threads, teams typically do one of: extract CPU-heavy work into a separate microservice (written in Go, Rust, or Python), or offload it to a BullMQ worker process running on its own container. Both approaches avoid the complexity of managing threads inside a Node.js process.

**Reason 3 — Most Node.js applications are I/O-bound, not CPU-bound.** The honest truth is that the majority of Node.js backend services don't hit meaningful CPU-bound bottlenecks. If they do, it's usually a sign that the work should be in a different service.

**Worker thread libraries — Piscina and workerpool:**

If you do need Worker Threads in production, you should not manage them manually. You use a pool.

```
┌──────────────────────────────────────────────────────────────┐
│  PISCINA                                                     │
│                                                              │
│  A production-grade worker thread pool for Node.js.          │
│  Manages thread lifecycle, queuing, and communication.       │
│                                                              │
│  const piscina = new Piscina({ filename: './worker.js' })    │
│  const result = await piscina.run({ data })                  │
│                                                              │
│  Piscina creates N worker threads (you configure N),         │
│  distributes work across them, and manages their lifecycle.  │
│  Very similar to Java's ThreadPoolExecutor in concept.       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  WORKERPOOL                                                  │
│                                                              │
│  Similar to Piscina but also supports child processes        │
│  (separate Node.js processes, not just threads).             │
│  More flexible, slightly more overhead.                      │
└──────────────────────────────────────────────────────────────┘
```

**Does FinVerse use Worker Threads or Piscina in the Core Product Service?**

No. The Core Product Service's BullMQ jobs are all I/O-bound — GoCardless API calls, PostgreSQL reads and writes, Redis operations. Worker Threads exist to solve CPU-bound problems. The Core Product Service doesn't have CPU-bound problems. The CPU-bound work (portfolio valuation) lives in the Market Data Service, which is written in Go and handles CPU work natively with goroutines. Worker Threads are simply not the right tool here.

---

## The Complete Java-to-Node.js Mental Map

```
┌──────────────────────────────────────────────────────────────────────┐
│                    THE MINDMAP TABLE                                 │
├────────────────────────────┬─────────────────────────────────────────┤
│  JAVA CONCEPT              │  NODE.JS EQUIVALENT                     │
├────────────────────────────┼─────────────────────────────────────────┤
│  OS Thread                 │  Worker Thread (real OS thread)         │
│                            │  (rarely used directly in production)   │
├────────────────────────────┼─────────────────────────────────────────┤
│  ThreadPoolExecutor        │  Piscina / workerpool                   │
│  (thread pool)             │  (worker thread pool)                   │
│                            │  OR: multiple Node.js processes         │
├────────────────────────────┼─────────────────────────────────────────┤
│  @Async (off-request work) │  BullMQ job queue                       │
│                            │  (separate process via Redis)           │
├────────────────────────────┼─────────────────────────────────────────┤
│  @Scheduled                │  BullMQ repeatable jobs                 │
│  ScheduledThreadPool       │  (cron or fixed interval)               │
├────────────────────────────┼─────────────────────────────────────────┤
│  Spring Batch              │  BullMQ with concurrency control        │
│                            │  (batch processing via job queue)       │
├────────────────────────────┼─────────────────────────────────────────┤
│  Spring Retry @Retryable   │  BullMQ backoff config                  │
│                            │  (attempts + exponential delay)         │
├────────────────────────────┼─────────────────────────────────────────┤
│  Thread.sleep()            │  await new Promise(resolve =>           │
│  (blocks the thread)       │    setTimeout(resolve, ms))             │
│                            │  (yields event loop, doesn't block)     │
├────────────────────────────┼─────────────────────────────────────────┤
│  synchronized / locks      │  Redis distributed locks                │
│  (single-process)          │  (BullMQ uses these internally)         │
├────────────────────────────┼─────────────────────────────────────────┤
│  Future / CompletableFuture│  Promise / async-await                  │
├────────────────────────────┼─────────────────────────────────────────┤
│  Parallelism               │  Multiple processes or Worker Threads   │
│  (multiple threads, same   │  (not the default model)                │
│   process)                 │                                         │
├────────────────────────────┼─────────────────────────────────────────┤
│  Concurrency               │  Event loop + async/await               │
│  (managing multiple tasks) │  (the default model)                    │
├────────────────────────────┼─────────────────────────────────────────┤
│  Blocking I/O              │  Doesn't exist in normal Node.js code   │
│  (thread waits)            │  (always async via libuv)               │
├────────────────────────────┼─────────────────────────────────────────┤
│  Multiple JVM instances    │  Multiple Node.js processes             │
│  (horizontal scaling)      │  (cluster, PM2, Kubernetes replicas,    │
│                            │   ECS containers)                       │
└────────────────────────────┴─────────────────────────────────────────┘
```

---

## What "Concurrency: 10" in BullMQ Actually Means

Now you have the model to understand this precisely.

When you write:

```typescript
@Processor('transaction-sync', { concurrency: 10 })
```

You are telling BullMQ: **this Worker can have up to 10 jobs running simultaneously on the event loop.**

Not 10 threads. Not 10 CPU cores. 10 concurrent async operations on the single event loop thread.

```
ONE NODE.JS PROCESS — BullMQ Worker with concurrency: 10

Event Loop Thread
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  Job 1: await goCardless.fetchTransactions(account_1)           │
│  Job 2: await goCardless.fetchTransactions(account_2)  ──────►  │
│  Job 3: await prisma.transaction.createMany(...)       ──────►  │ All these
│  Job 4: await goCardless.fetchTransactions(account_4)  ──────►  │ I/O calls
│  Job 5: await redis.set('sync_status', ...)            ──────►  │ are in
│  Job 6: await goCardless.fetchTransactions(account_6)  ──────►  │ flight
│  Job 7: await prisma.bankAccount.update(...)           ──────►  │ simultaneously
│  Job 8: await goCardless.fetchTransactions(account_8)  ──────►  │ at the OS
│  Job 9: await prisma.transaction.createMany(...)       ──────►  │ level
│  Job 10: await redis.get('rate_limit_counter')         ──────►  │
│                                                                 │
│  Event loop is free to process any callbacks that arrive        │
│  from any of these 10 concurrent I/O operations                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

While Job 1 is waiting for GoCardless to respond, Jobs 2-10 are progressing. While Job 3 is waiting for PostgreSQL, others are advancing. The event loop coordinates all of them — running whatever JavaScript code can run, handing off I/O to libuv, picking up callbacks as they arrive.

**This works because transaction sync is I/O-bound.** Every job spends most of its time waiting for GoCardless HTTP responses, PostgreSQL query results, and Redis reads — not running JavaScript calculations. The event loop stays free.

**If these were CPU-bound jobs**, concurrency: 10 would not help at all. The first job would grab the event loop and hold it for the duration of its computation. Jobs 2-10 would queue up and wait.

---

## Chapter 2 Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                      CHAPTER 2 SUMMARY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Java: multiple OS threads → true parallelism                   │
│  Node.js: one event loop thread → concurrent async I/O          │
│                                                                 │
│  libuv manages all I/O underneath Node.js                       │
│  Network I/O: fully async via OS kernel                         │
│  File/DNS/Crypto: libuv's own small thread pool (4 threads)     │
│                                                                 │
│  Node.js is brilliant for I/O-bound work                        │
│  Node.js breaks for CPU-bound work (blocks the event loop)      │
│                                                                 │
│  Worker Threads: real OS threads in Node.js                     │
│  Production reality: rarely used directly                       │
│  Piscina/workerpool: thread pools if you need them              │
│  FinVerse: CPU work lives in Go (Market Data Service)           │
│  Core Product: all I/O-bound → Worker Threads not needed        │
│                                                                 │
│  BullMQ concurrency: N = N concurrent async jobs on one         │
│  event loop thread (not N threads, not N CPU cores)             │
│  Works perfectly for I/O-bound jobs                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

Chapter 2 done. Ready for Chapter 3 — Containers, CPUs, and why this matters for BullMQ — where we'll make "scaling workers" concrete by understanding what hardware actually sits under a container, how ECS Fargate allocates CPU, and why all of this connects directly to the DevOps step later.