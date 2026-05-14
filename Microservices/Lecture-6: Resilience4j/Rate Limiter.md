# Section 1: What is a Fault-Tolerant Microservice & Why Do We Need It?

---

## What is a Fault-Tolerant Microservice?

A **fault-tolerant microservice** is a service that **continues to work even when a downstream service (a service it depends on) fails or becomes slow.**

Instead of crashing or letting the failure spread, it **handles the failure gracefully.**

> Simple way to think about it: "Even if someone I depend on is down, I won't go down because of them."

---

## Why is This Needed? — The Cascading Failure Problem

In a microservices system, services talk to each other **over the network.** This means if one service has a problem, it can potentially **bring down the entire system** — even services that have no bug of their own.

This is called a **Cascading Failure.**

Let's understand this with the exact example the instructor uses.

---

### Normal Flow (Everything Working Fine)

```
                    calls                      calls
  Client  ───────────────────►  Order  ───────────────────►  Product
         ◄───────────────────         ◄───────────────────
                 response                      response
```

Simple. Client calls Order, Order calls Product, Product responds, Order responds back to Client. No issues.

---

### Problem Flow — Product Becomes Slow

Now imagine a **buggy code** gets deployed to the Product service and every API call to it now takes **60 seconds** to respond.

```
                    calls                      calls
  Client  ───────────────────►  Order  ───────────────────►  Product
                                                              ⟳ (too slow,
                                                               taking 60s)
```

Now here's what happens step by step:

---

### Step-by-Step: How One Slow Service Brings Down Everything

```
┌─────────────────────────────────────────────────────────────────┐
│                        THREAD POOL (Order Service)              │
│                                                                 │
│  Request 1 ──► Thread 1 ──► waiting for Product... (60s) 🔴     │
│  Request 2 ──► Thread 2 ──► waiting for Product... (60s) 🔴     │
│  Request 3 ──► Thread 3 ──► waiting for Product... (60s) 🔴     │
│  Request 4 ──► Thread 4 ──► waiting for Product... (60s) 🔴     │
│       ...                                                       │
│  Request N ──► ??? ──► NO THREAD AVAILABLE ──► ❌ REJECTED       │
└─────────────────────────────────────────────────────────────────┘
```

**What's happening here:**

Every service has a **thread pool** — a limited number of threads that handle incoming requests. When Order calls Product and Product is slow, the thread handling that request is **blocked and waiting** for 60 seconds.

Now if a **sudden burst of requests** comes in to Order service:
- Each request grabs a thread
- Each thread gets stuck waiting for Product
- Slowly, **all threads in the pool get exhausted**
- When a new request comes in — **no thread is free** → request gets **rejected**

And it doesn't stop there. The clients calling Order service **also start getting rejected or hanging.** The failure that started at Product has now **cascaded to Order, and then to the clients.**

---

### The Full Cascading Picture

```
         😐 Client                                        
            │                                            
            │ requests pile up, start getting rejected   
            ▼                                            
┌─────────────────────┐                                  
│   Order Service     │  ← threads exhausted, starts    
│   ❌ Thread pool     │    rejecting requests             
│      exhausted      │                                  
└──────────┬──────────┘                                  
           │ all threads stuck waiting here              
           ▼                                             
┌─────────────────────┐                                  
│   Product Service   │  ← root cause (buggy/slow)       
│   🐢 Taking 60s     │    gets MORE load on top of it   
│   per request       │    making things even worse      
└─────────────────────┘                                  
```

> The instructor makes an important point here: **the Product service's situation also keeps getting worse** — because more and more requests keep coming into it (since Order keeps retrying/sending), so Product deteriorates further too.

---

### Key Takeaway

| What failed | What got affected |
|---|---|
| Product service (buggy/slow) | Order service threads exhausted |
| Order service (threads gone) | Clients start getting rejected |
| One service | Entire system potentially down |

**This is Cascading Failure** — the failure of one service propagates like dominoes across your whole system.

---

### So What Do We Need?

We need the **Order service to be fault-tolerant** — meaning:

> *"Even if Product goes down or becomes slow, Order should not crash. It should handle that failure gracefully and not let it cascade to its own callers."*

This is exactly what **Resilience4j** helps us build. We'll get into that in the next section.

---
# Section 2: Resilience4j — The 5 Mechanisms & Why Ordering Matters

---

## What is Resilience4j?

**Resilience4j** is a lightweight fault-tolerance library designed for Java (specifically works great with Spring Boot). It gives you a set of tools/mechanisms that you can plug into your microservices to make them fault-tolerant.

Think of it as a **protection layer** that sits around your inter-service calls.

---

## The 5 Mechanisms Resilience4j Provides

```
                        ┌─────────────────┐
                        │   Resilience4j  │
                        └────────┬────────┘
                                 │
         ┌───────────┬───────────┼───────────┬───────────┐
         ▼           ▼           ▼           ▼           ▼
   ┌──────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────┐
   │  Rate    │ │Bulkhead │ │  Time   │ │Circuit  │ │ Retry │
   │ Limiter  │ │         │ │ Limiter │ │ Breaker │ │       │
   └──────────┘ └─────────┘ └─────────┘ └─────────┘ └───────┘
```

Each one solves a **different problem.** Here's a quick one-line intro for each (we'll cover each in depth in their own sections):

| Mechanism | What it does (simple) |
|---|---|
| **Rate Limiter** | Controls how many requests are allowed in a given time window |
| **Bulkhead** | Limits how many concurrent calls can go to a downstream service |
| **Time Limiter** | Sets a timeout — if a call takes too long, cut it off |
| **Circuit Breaker** | If a service keeps failing, stop calling it for a while |
| **Retry** | If a call fails, automatically try again |

---

## Why Does the ORDER of Applying These Mechanisms Matter?

The instructor makes a very important point here — and this is something **frequently asked in interviews.**

> There is a **recommended logical ordering** in which these mechanisms should be applied:

```
Rate Limiter → Bulkhead → Time Limiter → Circuit Breaker → Retry
```

This is not a hard rule enforced by the framework, but it is the **de facto standard** that should be followed. Let me explain why with the instructor's own example.

---

### Why Rate Limiter BEFORE Retry?

Imagine you flip the order — you apply **Retry first, then Rate Limiter.**

```
Incoming Request
      │
      ▼
  ┌────────┐       ┌─────────────┐
  │ Retry  │──────►│ Rate Limiter│──► BLOCKED ❌
  └────────┘       └─────────────┘
  (retries 3x)      (was going to
                     block anyway)
```

What happens?
- A request comes in
- Retry logic kicks in and retries it 3 times
- Rate Limiter then blocks all 3 of those retried requests anyway
- You wasted **3x the computation** for no reason

Now flip it the correct way — **Rate Limiter first, then Retry:**

```
Incoming Request
      │
      ▼
  ┌─────────────┐       ┌────────┐
  │ Rate Limiter│──────►│ Retry  │──► actual call
  └─────────────┘       └────────┘
  (blocks excess         (only retries
   traffic early)         what passed)
```

- Rate Limiter blocks traffic that exceeds the limit **right at the gate**
- Only requests that **pass** the rate limiter go to the retry logic
- No wasted computation retrying things that would've been blocked anyway

---

### The Full Ordering — Intuition Behind Each Step

```
Incoming Request
      │
      ▼
┌─────────────┐
│ Rate Limiter│  ← Step 1: Is the overall traffic within allowed limit?
└──────┬──────┘           If too many requests → reject early
       │
       ▼
┌─────────────┐
│  Bulkhead   │  ← Step 2: Are we sending too many CONCURRENT calls
└──────┬──────┘           to the downstream? If yes → reject
       │
       ▼
┌─────────────┐
│ Time Limiter│  ← Step 3: If the call is taking too long → cut it off
└──────┬──────┘           don't let threads hang forever
       │
       ▼
┌─────────────┐
│Circuit Break│  ← Step 4: If downstream keeps failing → stop calling
└──────┬──────┘           it altogether for a while (open the circuit)
       │
       ▼
┌─────────────┐
│    Retry    │  ← Step 5: If call failed but it's worth trying again
└──────┬──────┘           retry — but only after all above checks pass
       │
       ▼
  Actual Downstream Call (e.g. Product Service)
```

> **Interview Tip:** If asked "what is the recommended order of Resilience4j mechanisms and why?" — always say Rate Limiter → Bulkhead → Time Limiter → Circuit Breaker → Retry, and explain that the ordering ensures you don't waste computation on requests that would have been blocked or timed out anyway.

---

## One More Important Point — The Dependency

All 5 mechanisms come from a **single Resilience4j dependency** in your `pom.xml`. You don't need separate libraries for each:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

Add this once, and you get access to all 5 mechanisms.

---

## Summary So Far

```
Problem                          Solution
───────────────────────────────────────────────────────
One slow/down service can   →   Build Fault-Tolerant
bring down whole system         Microservices
(Cascading Failure)

How?                        →   Use Resilience4j with
                                5 mechanisms in the
                                correct order:
                                Rate Limiter → Bulkhead
                                → Time Limiter →
                                Circuit Breaker → Retry
```

---

Ready for **Section 3: Rate Limiter — All 5 Algorithms explained with diagrams?**

This is the longest and most detailed section of the lecture. The instructor covers Fixed Window Counter, Sliding Log, Sliding Window Counter (both flavors), Token Bucket, and Leaky Bucket — all with examples.

# Section 3: Rate Limiter — Deep Dive

---

## What is a Rate Limiter?

A Rate Limiter **controls the number of requests allowed to a microservice in a given time window.**

In simple words — it's a gatekeeper that says:

> *"Only this many requests are allowed in this time period. If more come in, reject them."*

### Why do we need it?
It protects your system from:
- Sudden **traffic spikes**
- **DDoS attacks** (someone hammering your service with thousands of requests)
- A misbehaving upstream service **flooding** your downstream service

---

## The 5 Rate Limiting Algorithms

```
                    ┌──────────────┐
                    │  Rate Limiter │
                    └──────┬───────┘
                           │
        ┌──────────────────┼─────────────────────┐
        ▼                  ▼                      ▼
┌───────────────┐  ┌──────────────┐     ┌─────────────────────┐
│ Fixed Window  │  │ Sliding Log  │     │ Sliding Window      │
│   Counter     │  │              │     │ Counter             │
└───────────────┘  └──────────────┘     └──────────┬──────────┘
                                                   │
                                      ┌────────────┴────────────┐
                                      ▼                         ▼
                             ┌────────────────┐    ┌────────────────────┐
                             │  Sub-Window    │    │  Weighted Window   │
                             │  Flavor        │    │  Flavor            │
                             └────────────────┘    └────────────────────┘

        ┌──────────────┐        ┌───────────────┐
        │ Token Bucket │        │ Leaky Bucket  │
        └──────────────┘        └───────────────┘
```

Let's go one by one.

---

## Algorithm 1: Fixed Window Counter

### How it works
- Time is divided into **fixed windows** of equal size (e.g. every 10 seconds)
- Each window has a **maximum request limit**
- Count requests in the current window — if count exceeds limit → **reject**
- When window resets → counter resets → new requests allowed again

### Example
```
Limit = 5 requests
Fixed Window Size = 10 seconds

Timeline:
0────────────────10────────────────20────────────────30
│   Window 1      │    Window 2     │    Window 3     │
│   Limit = 5     │    Limit = 5    │    Limit = 5    │
└─────────────────┴─────────────────┴─────────────────┘

Window 1 (0-10s):
  Request at 1s ✅ (count=1)
  Request at 3s ✅ (count=2)
  Request at 5s ✅ (count=3)
  Request at 7s ✅ (count=4)
  Request at 9s ✅ (count=5)  ← limit reached

Window 2 (10-20s):
  Request at 11s ✅ (count=1)  ← fresh window, counter reset
  Request at 12s ✅ (count=2)
  Request at 13s ✅ (count=3)
  Request at 14s ✅ (count=4)
  Request at 15s ✅ (count=5)  ← limit reached
  Request at 16s ❌ REJECTED   ← 6th request, over limit
  Request at 17s ❌ REJECTED
```

Simple and easy to implement. But there's a problem...

### ⚠️ Disadvantage — The Edge Spike Problem

```
Limit = 5 per 10s window

0──────────────────10──────────────────20
│     Window 1     │     Window 2      │
└──────────────────┴───────────────────┘

What if all 5 requests come at the END of Window 1
AND all 5 requests come at the START of Window 2?

        5 requests          5 requests
        pile up here        pile up here
             │                   │
             ▼                   ▼
0─────────── 9──10──11 ──────────20
             ↑↑↑↑↑  ↑↑↑↑↑
             
In just 1 second (9s to 11s) → 10 requests got through!
Even though limit was 5 per 10 seconds.
```

> The fixed window doesn't account for **requests clustering at the edges.** Your system might receive 10 requests in 1 second even though the limit is 5 per 10 seconds.

---

## Algorithm 2: Sliding Log

### How it works
- For every **accepted request**, store its **exact timestamp**
- When a new request comes in — look back at the last X seconds (window size)
- Count how many requests are in that window
- If count has reached the limit → **reject**
- The window **slides** with time — it's always "last X seconds from now"

### Example
```
Window Size = 10 seconds
Limit = 5

Timestamps of accepted requests stored in log:
[10:00:02, 10:00:05, 10:00:07, 10:00:09]  ← 4 requests accepted

Current time = 10:00:11
Sliding window = 10:00:01 to 10:00:11

       Sliding Window (10 seconds)
       ┌──────────────────────────┐
───────┤ 02  05  07  09           ├──────────
       └──────────────────────────┘
         ↑    ↑   ↑   ↑
         all 4 are inside the window

Count in window = 4, Limit = 5 → ✅ New request ACCEPTED
Log now = [10:00:02, 10:00:05, 10:00:07, 10:00:09, 10:00:11]

─────────────────────────────────────────────────

Current time = 10:00:12
Sliding window = 10:00:02 to 10:00:12

       Sliding Window (10 seconds)
       ┌──────────────────────────┐
───────┤ 02  05  07  09  11       ├──────────
       └──────────────────────────┘

Count in window = 5, Limit = 5 → ❌ New request REJECTED

─────────────────────────────────────────────────

Current time = 10:00:13
Sliding window = 10:00:03 to 10:00:13
                 ↑
                 Notice: 10:00:02 has now FALLEN OUT of window
                 It must be CLEANED UP from the log

       Sliding Window (10 seconds)
       ┌──────────────────────────┐
──[02]─┤ 05  07  09  11           ├──────────
  (expired,                       
  cleaned up)                     
       └──────────────────────────┘

Count in window = 4, Limit = 5 → ✅ New request ACCEPTED
```

This solves the edge spike problem — but introduces new costs.

### ⚠️ Disadvantages
- You must **store a timestamp for every accepted request** → needs more memory as traffic grows
- You must **clean up expired timestamps** that fall outside the window → extra overhead

---

## Algorithm 3: Sliding Window Counter

This comes in **two flavors.** Both try to fix the memory/cleanup problem of Sliding Log.

---

### Flavor 1: Sliding Window Counter with Sub-Windows

### How it works
- The main window is divided into **equal-sized sub-windows**
- Each sub-window just maintains a **count** (not individual timestamps)
- To decide if a new request should be accepted — **sum up counts of all active sub-windows**
- Main window **slides by one sub-window size** at a time

### Example
```
Main Window = 10 seconds
Sub-Window size = 2 seconds  →  5 sub-windows inside main window
Limit = 5

Initial state (time = 0 to 10s):
┌────────────────────────────────────────────────────┐  MAIN WINDOW
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐       │
│ │ 0-2s │ │ 2-4s │ │ 4-6s │ │ 6-8s │ │8-10s │       │
│ │  1   │ │  1   │ │  1   │ │  1   │ │  0   │       │
│ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘       │
└────────────────────────────────────────────────────┘
  SW1       SW2       SW3       SW4       SW5

Request comes at 9th second:
Sum of all active sub-windows = 1+1+1+1+0 = 4
4 < 5 (limit) → ✅ REQUEST ACCEPTED
```

Now time crosses 10 seconds — main window must slide:

```
Main window slides by 1 sub-window size (2 seconds)
New main window = 2s to 12s

BEFORE sliding:                    AFTER sliding:
┌──────────────────────────────┐   ┌──────────────────────────────┐
│ [0-2]  2-4  4-6  6-8  8-10   │   │  2-4   4-6  6-8  8-10  10-12 │
│  ❌    SW1  SW2  SW3   SW4    │   │  SW1   SW2  SW3   SW4   SW5  │
└──────────────────────────────┘   └──────────────────────────────┘
  0-2s sub-window is now EXPIRED     New sub-window 10-12s added
  and no longer counted              with count = 0

Request comes at 11th second:
Sum = 1(2-4) + 1(4-6) + 1(6-8) + 1(8-10) + 0(10-12) = 4
4 < 5 (limit) → ✅ REQUEST ACCEPTED
```

### ⚠️ Disadvantages
- More complex than fixed window — you have to manage sub-windows
- Each sub-window needs to maintain its own count → more memory than fixed window

---

### Flavor 2: Sliding Window Counter with Weighted Window

This is a **simpler approximation** — it combines Fixed Window + Sliding Log concepts, but avoids the need for sub-windows entirely.

### How it works
- You still have fixed windows (e.g. 0-10s, 10-20s)
- But you also have a **slider** of the same size that moves smoothly
- When the slider overlaps two fixed windows, you calculate a **weighted count**:

```
Total = (requests in current fixed window) 
      + (% of slider in previous window × total requests in previous window)
```

### Example
```
Fixed Window size = 10s
Slider size = 10s
Limit = 5

Fixed Window 1 (0-10s): 5 requests accepted
Fixed Window 2 (10-20s): 0 requests so far

─────────────────────────────────────────
Scenario A: Slider at 1s to 11s (90% in Window1, 10% in Window2)

     0────────────────10────────────────20
     │   Window 1 (5) │   Window 2 (0)  │
     └────────────────┴─────────────────┘
          │◄── 90% ──►│◄ 10% ►│
          └───── Slider (1s to 11s) ─────┘

Request comes at 11th second:
Total = current window requests + 90% of previous window
      = 0 + (0.90 × 5)
      = 0 + 4.5  →  ceil to 5
      = 5

5 = limit → ❌ REQUEST REJECTED

─────────────────────────────────────────
Scenario B: Slider moves to 2s to 12s (80% in Window1, 20% in Window2)

     0────────────────10────────────────20
     │   Window 1 (5) │   Window 2 (0)  │
     └────────────────┴─────────────────┘
            │◄── 80% ──►│◄──20%──►│
            └──── Slider (2s to 12s) ────┘

Request comes at 12th second:
Total = 0 + (0.80 × 5)
      = 0 + 4
      = 4

4 < 5 → ✅ REQUEST ACCEPTED
```

### ⚠️ Disadvantage — The Approximation Problem

```
Assumption the algorithm makes:
"Requests in the previous fixed window were 
 SPREAD EVENLY across the whole window"

But what if all 5 requests came at the END of Window 1?

     0────────────8───10
     │            ↑↑↑↑↑│   ← all 5 requests are here, at the end
     │   Window 1      │
     └─────────────────┘

Now when the slider is 80% in Window 1,
the algorithm says: 80% × 5 = 4

But the TRUTH is: those 5 requests are RIGHT at the boundary,
very close to the current window.

So the algorithm might ALLOW a request it shouldn't,
resulting in MORE requests getting through than the limit.
```

> The weighted window **assumes uniform traffic distribution** — which may not always be true.

---

## Algorithm 4: Token Bucket

### How it works
- Imagine a **physical bucket** that holds tokens
- Each incoming request **consumes one token**
- If **no tokens left** → request is denied
- A **re-filler** adds tokens back at regular intervals
- If bucket is already full when re-filler runs → tokens overflow and are discarded

### Example
```
Bucket capacity = 4 tokens
Re-filler = adds 1 token every 6 seconds

Initial state:
        ┌─────────────┐
Refiller│  ▼ 1 token  │
every   │             │
6 sec   │  ⭕ token    │
        │  ⭕ token    │  ← 4 tokens available
        │  ⭕ token    │
        │  ⭕ token    │
        └─────────────┘
        Capacity = 4

Requests come in:
  1s → Request 1 → consumes token ✅ (3 left)
  2s → Request 2 → consumes token ✅ (2 left)
  3s → Request 3 → consumes token ✅ (1 left)
  4s → Request 4 → consumes token ✅ (0 left)
  5s → Request 5 → NO TOKEN LEFT  ❌ REJECTED

        ┌─────────────┐
        │             │  ← empty bucket
        │             │
        └─────────────┘

  6s → Re-filler adds 1 token
        ┌─────────────┐
        │  ⭕ token   │  ← 1 token added
        └─────────────┘

  6s → Request 6 → consumes token ✅ (0 left)
```

### ⚠️ Disadvantage
```
If system is idle for a long time:

  0s ──────────────────────────────────── 60s
  No requests at all...
  Re-filler keeps adding tokens every 6s
  Bucket fills to max capacity = 4

  60s: Sudden burst of 4 requests comes in
       All 4 get through instantly!

If capacity is set too high (e.g. 1000 tokens),
a burst of 1000 requests can all get through at once.
→ Must set token capacity carefully!
```

---

## Algorithm 5: Leaky Bucket

### How it works
- Incoming requests go into a **queue (the bucket)**
- Requests are **processed at a fixed, constant rate** — like water leaking out of a bucket
- If the queue is **full** → new incoming requests are denied (HTTP 429)

### Flow
```
                 Incoming Requests
                  ↓  ↓  ↓  ↓  ↓

                ┌─────────────┐
  Queue full? ──┤             ├── No → Add to queue
       │        │  R R R R R  │
       │        │  e e e e e  │
       Yes      │  q q q q q  │
       │        └──────┬──────┘
       ▼               │
  ❌ DENIED            │ processed at fixed rate
  HTTP 429             ↓  ↓  ↓
                (constant, steady flow out)
```

### Example
```
Queue size = 5
Processing rate = 1 request per second

Time 0s: 8 requests come in at once
  → 5 go into queue ✅
  → 3 are denied ❌ (queue full, HTTP 429)

Time 1s: 1 request processed and leaves queue
  Queue has space for 1 more now

Time 2s: 1 more request processed
  → steady, constant output regardless of input burst
```

### ⚠️ Disadvantages
- **Latency increases** — requests have to wait in the queue for their turn
- **Queue size is a bottleneck:**
    - Too small → too many requests get denied
    - Too large → latency shoots up
- Requires memory to maintain the queue

---

## Quick Comparison of All 5 Algorithms

```
┌──────────────────────────┬──────────────────────────┬───────────────────────────┐
│ Algorithm                │ How it controls traffic  │ Main Disadvantage         │
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Fixed Window Counter     │ Count per fixed window   │ Edge spike possible       │
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Sliding Log              │ Exact timestamps tracked │ High memory + cleanup     │
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Sliding Window (Sub)     │ Count per sub-window     │ Complex, more memory      │
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Sliding Window (Weighted)│ Weighted approximation   │ Assumes uniform traffic   │
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Token Bucket             │ Token consumption        │ Burst if capacity too high│
├──────────────────────────┼──────────────────────────┼───────────────────────────┤
│ Leaky Bucket             │ Fixed output rate        │ Latency, queue bottleneck │
└──────────────────────────┴──────────────────────────┴───────────────────────────┘
```

> **Important:** Resilience4j by default uses the **Token Bucket** algorithm for its Rate Limiter implementation. You'll see this reflected in the configuration properties in the next section.

---

Ready for **Section 4: Implementation in Spring Boot — Annotation + Configuration + Fallback + Custom AOP?**

# Section 4: Rate Limiter Implementation in Spring Boot

---

## The Big Picture — What We're Building

Before jumping into code, let's understand the **system setup** the instructor is using throughout this implementation:

```
                    Feign Client
  ┌──────────────┐  (HTTP call)   ┌─────────────────┐
  │    Order     │───────────────►│    Product      │
  │   Service    │                │    Service      │
  │  (port 8081) │◄───────────────│                 │
  └──────────────┘   response     └─────────────────┘
        ▲
        │
  ┌──────────────┐
  │    Client    │
  │  (Postman)   │
  └──────────────┘

Both services registered with Eureka (Service Discovery)
Rate Limiter is applied on Order Service side
— protecting it from overwhelming the Product Service
```

The **Rate Limiter lives in the Order Service.** It controls how many calls Order is allowed to make to Product in a given time window.

---

## Step 1 — Add the Dependency

Everything — Rate Limiter, Bulkhead, Circuit Breaker, Retry, Time Limiter — all come from this **single dependency:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

> Use any stable version. This one dependency gives you access to all 5 Resilience4j mechanisms.

---

## Step 2 — The Controller

Nothing special here. Just a standard REST controller that accepts a request and delegates to the service layer:

```java
// OrderController.java
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

```
Client hits:  GET /orders/{id}
                    │
                    ▼
            OrderController
                    │
                    ▼
            OrderService  ← Rate Limiter lives here
                    │
                    ▼
            ProductClient (Feign)
                    │
                    ▼
            Product Service
```

---

## Step 3 — The Feign Client

Used for inter-service communication between Order and Product. Already covered in depth in the playlist — nothing new here:

```java
// ProductClient.java
@FeignClient(name = "product-service")  // matches spring.application.name of Product service
public interface ProductClient {

    @GetMapping(value = "/products/{id}")
    String getProductById(@PathVariable("id") String id);
}
```

---

## Step 4 — The Service Layer (Where Rate Limiter is Applied)

This is the **most important part.** The `@RateLimiter` annotation goes on the method that makes the actual downstream call:

```java
// OrderService.java
@Component
public class OrderService {

    @Autowired
    ProductClient productClient;

    /*
     * @RateLimiter uses AOP internally.
     * That's why this method must be:
     * 1. public
     * 2. inside a Spring-managed bean (@Component / @Service)
     */
    @RateLimiter(name = "productRateLimiter", fallbackMethod = "rateLimitedFallback")
    public void invokeProductAPI(String id) {
        String response = productClient.getProductById(id);
        System.out.println("Response from Product API: " + response);
    }

    /*
     * FALLBACK METHOD RULES (very important!):
     * 1. Return type must be SAME as original method (void here)
     * 2. Parameters must be SAME as original method (String id here)
     * 3. One ADDITIONAL parameter: Throwable (to capture the exception)
     * 4. If signature doesn't match → framework uses its own default fallback
     */
    public void rateLimitedFallback(String id, Throwable t) {
        System.out.println("Rate limit exceeded. Try later");
        // You can throw a custom exception here
        // or return a default/cached response
        // handle it according to your business logic
    }
}
```

### Fallback Method — Signature Rules Visualized

```
Original Method:
─────────────────────────────────────────────
public void invokeProductAPI(String id)
       ▲                     ▲
       │                     │
  return type             parameter
  must match              must match
       │                     │
       ▼                     ▼
public void rateLimitedFallback(String id,  Throwable t)
                                            ▲
                                            │
                               ONE extra parameter added
                               (Throwable) — this is mandatory
─────────────────────────────────────────────

If signature doesn't match exactly (return type + params + Throwable):
→ Resilience4j cannot find YOUR fallback
→ It falls back to its OWN default fallback method
→ You lose control over how the failure is handled
```

---

## Step 5 — Configuration in application.properties

```properties
# application.properties (Order Service)

server.port=8081
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Rate Limiter Configuration
# ─────────────────────────────────────────────────────────────────
# limitForPeriod     = number of tokens in the bucket (max requests allowed per refresh period)
# limitRefreshPeriod = how often the bucket is refilled
# timeoutDuration    = how long a request should WAIT for a token before being rejected
# ─────────────────────────────────────────────────────────────────
resilience4j.ratelimiter.instances.productRateLimiter.limitForPeriod=2
resilience4j.ratelimiter.instances.productRateLimiter.limitRefreshPeriod=10s
resilience4j.ratelimiter.instances.productRateLimiter.timeoutDuration=1s
```

### What These Properties Mean — Visualized

```
limitForPeriod = 2  →  Bucket has 2 tokens
                       ┌─────────────┐
                       │  ⭕ token    │
                       │  ⭕ token    │  max = 2
                       └─────────────┘

limitRefreshPeriod = 10s  →  Every 10 seconds, bucket is refilled with 2 tokens
                              (back to max capacity)

timeoutDuration = 1s  →  If no token available, wait UP TO 1 second
                          before giving up and calling fallback

─────────────────────────────────────────────────────────

Flow with these settings:

  Request 1 → token consumed ✅ (1 token left)
  Request 2 → token consumed ✅ (0 tokens left)
  Request 3 → no token... wait 1 second...
              still no token (10s not over yet) → ❌ fallback called
              "Rate limit exceeded. Try later"

  ... wait 10 seconds ...

  Bucket refilled with 2 tokens again

  Request 4 → token consumed ✅
  Request 5 → token consumed ✅
  Request 6 → ❌ fallback called again
```

### Why the name `productRateLimiter` in properties?

```
@RateLimiter(name = "productRateLimiter", ...)
                          │
                          │ must match exactly
                          ▼
resilience4j.ratelimiter.instances.productRateLimiter.limitForPeriod=2
                                   ▲
                                   │
                          this is the instance name

You can have MULTIPLE rate limiters for different downstream services:

resilience4j.ratelimiter.instances.productRateLimiter.limitForPeriod=2
resilience4j.ratelimiter.instances.productRateLimiter.limitRefreshPeriod=10s
resilience4j.ratelimiter.instances.productRateLimiter.timeoutDuration=1s

resilience4j.ratelimiter.instances.inventoryRateLimiter.limitForPeriod=5
resilience4j.ratelimiter.instances.inventoryRateLimiter.limitRefreshPeriod=5s
resilience4j.ratelimiter.instances.inventoryRateLimiter.timeoutDuration=2s
```

---

## Step 6 — How Does @RateLimiter Work Internally? (AOP)

The instructor explains that `@RateLimiter` works via **Spring AOP (Aspect Oriented Programming)** — meaning it **intercepts the method call** before actually executing it.

```
You call:  orderService.invokeProductAPI(id)
                │
                │  AOP intercepts BEFORE the method runs
                ▼
┌───────────────────────────────────────────┐
│           Resilience4j AOP Aspect         │
│                                           │
│  1. Run Token Bucket logic                │
│     → Is there a token available?         │
│                                           │
│     YES → proceed() → actual method runs  │
│            → productClient.getProductById │
│                                           │
│     NO  → wait for timeoutDuration        │
│           still no token?                 │
│           → call fallback method          │
└───────────────────────────────────────────┘
```

> This is why the method **must be public** and the class **must be a Spring-managed bean** (`@Component` / `@Service`). AOP only works on Spring-proxied beans.

---

## Step 7 — Writing a Custom Rate Limiter Using AOP (Bonus)

The instructor also shows that if you want a **different algorithm** (not Token Bucket), you can write your own Rate Limiter from scratch using AOP. Here's how:

### Part A — Define a Custom Annotation

```java
// CustomRateLimiter.java
@Target(ElementType.METHOD)       // applies on methods
@Retention(RetentionPolicy.RUNTIME) // available at runtime
public @interface CustomRateLimiter {
    int limit();               // max requests allowed
    int windowInSeconds();     // sliding window duration
}
```

### Part B — Write the AOP Aspect

```java
// RateLimiterAspect.java
@Aspect
@Component
public class RateLimiterAspect {

    /*
     * @Around advice — runs AROUND the method
     * Intercepts any method annotated with @CustomRateLimiter
     */
    @Around("@annotation(customRateLimiter)")
    public Object rateLimit(ProceedingJoinPoint pjp,
                            CustomRateLimiter customRateLimiter)
                            throws Throwable {

        // Put YOUR custom rate limiting logic here
        // (sliding log, fixed window, whatever you want)
        // Use customRateLimiter.limit() and customRateLimiter.windowInSeconds()

        // If request is allowed:
        return pjp.proceed(); // → actually executes the annotated method

        // If request is denied:
        // throw exception or return fallback response
    }
}
```

### Part C — Use Your Custom Annotation

```java
// In your service
@CustomRateLimiter(limit = 5, windowInSeconds = 60)
public String getProducts() {
    // service call
    return "List of products";
}
```

### The Full AOP Flow Visualized

```
Client calls getProducts()
        │
        │  Spring AOP proxy intercepts
        ▼
┌────────────────────────────────┐
│      RateLimiterAspect         │
│  @Around advice runs first     │
│                                │
│  → your custom algorithm runs  │
│  → check limit, window, etc.   │
│                                │
│  Allowed?                      │
│    YES → pjp.proceed()─────────┼──► getProducts() actually runs
│    NO  → throw exception       │
└────────────────────────────────┘
```

> The instructor's point: Resilience4j's `@RateLimiter` does **exactly this** internally — it's just that they've already written the AOP aspect for you using Token Bucket. If you need a different algorithm, you replicate this pattern with your own logic.

---

## Complete Flow — Everything Together

```
GET /orders/{id}
       │
       ▼
OrderController.callProductAPI(id)
       │
       ▼
OrderService.invokeProductAPI(id)  ← @RateLimiter annotation here
       │
       │  AOP intercepts
       ▼
┌──────────────────────────────────────┐
│     Token Bucket Check               │
│                                      │
│  Token available?                    │
│  YES  ──────────────────────────────►│──► productClient.getProductById(id)
│                                      │           │
│  NO → wait up to 1s (timeoutDuration)│          ▼
│       still no token?                │    Product Service responds
│       ▼                              │           │
│  rateLimitedFallback(id, t)          │           ▼
│  "Rate limit exceeded. Try later"    │    print response ✅
└──────────────────────────────────────┘
```

---

## Output the Instructor Showed

```
# First 2 hits (2 tokens available):
Response from Product API: fetch the product details with id:1
Response from Product API: fetch the product details with id:1

# 3rd hit (no token, waited 1s, still none):
Rate limit exceeded. Try later

# Wait 10 seconds (bucket refills)...

# Next 2 hits work again:
Response from Product API: fetch the product details with id:1
Response from Product API: fetch the product details with id:1

# 3rd hit again:
Rate limit exceeded. Try later
```

---

## Interview Tips — Rate Limiter

> **Q: What is a Rate Limiter and why do we need it?**
> Controls number of requests in a time window. Protects against traffic spikes and DDoS.

> **Q: What algorithm does Resilience4j use by default for Rate Limiting?**
> Token Bucket.

> **Q: What is the difference between Token Bucket and Leaky Bucket?**
> Token Bucket allows bursts (up to bucket capacity) and refills at fixed intervals. Leaky Bucket processes at a fixed constant rate — no bursts allowed, but latency increases.

> **Q: How does @RateLimiter work internally in Spring Boot?**
> Through AOP. It intercepts the method call, runs the token bucket logic, and either calls proceed() or the fallback method.

> **Q: What happens if the fallback method signature doesn't match?**
> Resilience4j cannot find your fallback and uses its own default fallback instead.

> **Q: Why should Rate Limiter come before Retry in the ordering?**
> To avoid wasting computation retrying requests that would be blocked by the Rate Limiter anyway.

---

## Full Code Summary

```
Order Service
├── pom.xml                    → resilience4j-spring-boot3 dependency
├── OrderController.java       → standard REST controller
├── ProductClient.java         → Feign client for Product service
├── OrderService.java          → @RateLimiter annotation + fallback method
└── application.properties     → limitForPeriod, limitRefreshPeriod, timeoutDuration

Optional Custom Rate Limiter
├── CustomRateLimiter.java     → custom annotation
└── RateLimiterAspect.java     → AOP aspect with custom algorithm
```

---

That completes the full Rate Limiter lecture! The next parts of this series will cover **Bulkhead, Time Limiter, Circuit Breaker, and Retry** — each building on top of what we've covered here.