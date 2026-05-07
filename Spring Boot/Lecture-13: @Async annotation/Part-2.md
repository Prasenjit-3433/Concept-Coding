# Step 1 — Conditions for @Async to Work Properly

---

## The Core Question

When you put `@Async` on a method, you expect it to run in a **new thread**. But sometimes, even after adding `@Async`, the method still runs in the **same thread** as the caller. Why?

Because two conditions must be met for `@Async` to work as expected.

---

## The Two Conditions

### Condition 1 — The method must be in a **different class** from where it is called
### Condition 2 — The method must be **public**

---

## Why These Two Conditions? — The Real Reason (AOP & Proxy)

This is the most important part. The answer lies in how Spring handles `@Async` internally.

When you use `@Async`, Spring does **not** magically create a new thread by itself at that exact line. Instead, it uses something called **AOP (Aspect Oriented Programming)** behind the scenes.

Here's how it works step by step:

```
Your Code calls Method A
        |
        v
  AOP Intercepts the call       <-- This is where the magic happens
        |
        v
  Spring creates a Proxy object
        |
        v
  Proxy submits the method
  to a thread pool (new thread)
        |
        v
  Method runs in a NEW thread
```

So the key actor here is **interception**. AOP has to intercept your method call first, before it can do anything (like spinning up a new thread).

Now here's the problem — **interception only works under two specific situations:**

| Situation | Does Interception Work? |
|---|---|
| Calling a method from **Class A → Class B** | ✅ Yes |
| Calling a method **within the same class** | ❌ No |
| Calling a **public** method | ✅ Yes |
| Calling a **private/protected** method | ❌ No |

This is exactly why those two conditions exist. If either condition is broken, AOP cannot intercept, the proxy is never involved, and `@Async` does nothing — your method runs in the same thread as the caller.

---

## The Wrong Way — What Happens When Conditions Are Broken

```
@RestController
public class UserController {

    @GetMapping("/getuser")
    public String getUserMethod() {
        // calling asyncMethodTest from THE SAME CLASS
        asyncMethodTest();
        return null;
    }

    @Async
    public void asyncMethodTest() {
        // expects to run in new thread
        System.out.println(Thread.currentThread().getName());
    }
}
```

Both methods are in the **same class**. So even though `@Async` is written, AOP cannot intercept this internal call. The proxy never gets involved.

**Output you'd see:**
```
inside getUserMethod:   http-nio-8080-exec-1
inside asyncMethodTest: http-nio-8080-exec-1
```

Both lines print the **exact same thread name** — meaning no new thread was ever created. `@Async` did nothing here.

---

## The Correct Way — Conditions Met

```
         UserController                    UserService
         (Caller Class)                  (Different Class)
         ──────────────                  ──────────────────
         getUserMethod()   ──calls──>    @Async
                                         asyncMethodTest()   ──> NEW THREAD
```

Move the `@Async` method to a **separate class**, keep it **public**, and now AOP can intercept the cross-class call properly. A new thread gets created as expected.

---

## 🎯 Interview Tip

> **Q: What are the conditions for `@Async` to work properly?**

**Answer to give:**
There are two conditions. First, the method annotated with `@Async` must be in a **different class** from where it is being called — because Spring uses AOP and proxy-based interception under the hood, and interception only works on cross-class method calls. If you call an `@Async` method within the same class, the proxy is bypassed entirely. Second, the method must be **public**, because AOP interception does not work on private or protected methods.

---

# Step 2 — @Async + Transaction Management

---

## The Core Question

How do `@Async` and `@Transactional` work together? Are there any problems when you combine them?

The short answer is — **yes, there are challenges**, and the root cause is one important rule:

> **Transaction context does NOT transfer from the caller thread to a new thread created by @Async.**

Let's understand what "transaction context" means first, and then walk through all three use cases.

---

## What is Transaction Context?

When a `@Transactional` method runs, Spring creates a **transaction context** — think of it as a container that holds all transaction-related information for that thread:

```
Transaction Context (lives on Thread 1)
┌─────────────────────────────────────┐
│  - Propagation level                │
│  - Isolation level                  │
│  - Rollback rules                   │
│  - DB Connection info               │
│  - Thread pool being used           │
└─────────────────────────────────────┘
```

This context is **thread-local** — meaning it belongs to one specific thread. When `@Async` creates a brand new thread, that new thread starts completely fresh — with **no transaction context at all**.

---

## Use Case 1 ❌ — @Transactional calls an @Async method inside it

### The Setup

```
UserController          UserService               UserUtility
──────────────          ───────────               ───────────
updateUserMethod()  --> @Transactional        --> @Async
                        updateUser()               updateUserBalance()
                        {
                          //1. update user status
                          //2. update first name
                          //3. calls updateUserBalance()  --> new thread spawned here
                        }
```

### What Happens

```
Thread 1 (Main)
│
├── Transaction STARTS here (@Transactional)
│       │
│       ├── update user status  ✅ inside transaction
│       ├── update first name   ✅ inside transaction
│       │
│       └── calls updateUserBalance() --> @Async spawns Thread 2
│                                              │
│                                         Thread 2 (New)
│                                         NO transaction context
│                                         update balance runs
│                                         WITHOUT any transaction ❌
│
└── If something fails --> rollback happens for Thread 1 only
                          Thread 2 changes are NOT rolled back ❌
```

### The Problem

The new thread created by `@Async` has **zero transaction context**. So `updateUserBalance()` runs completely outside any transaction. If something goes wrong anywhere and a rollback is triggered, only the changes made in Thread 1 (update status, update first name) will be rolled back. The balance update in Thread 2 will **not** be rolled back.

This can cause **data inconsistency** — partial updates go through, partial ones get rolled back.

### Verdict: ❌ Avoid this pattern completely

---

## Use Case 2 ⚠️ — Same method has both @Transactional and @Async

### The Setup

```
UserController          UserService
──────────────          ───────────
updateUserMethod()  --> @Transactional
                        @Async
                        updateUser()
                        {
                          //1. update user status
                          //2. update first name
                          //3. update user
                        }
```

### What Happens

```
Thread 1 (Main)
│
└── calls updateUser() --> @Async spawns Thread 2
                                  │
                             Thread 2 (New)
                             @Transactional creates a
                             FRESH new transaction here
                             │
                             ├── update user status  ✅
                             ├── update first name   ✅
                             └── update user         ✅
                             All in a transaction, rollback works ✅
```

At first glance this looks okay — a new thread is created, and that thread does have its own transaction. So if anything fails inside, rollback will happen within that thread.

### But Why "Use With Precaution"?

The problem appears when the **caller (Thread 1) also has a transaction**, and you're relying on **propagation levels**.

For example, you might set propagation as `SUPPORTS` — meaning "if a parent transaction exists, use it." But because `@Async` always creates a brand new thread with no inherited context, the parent transaction is **invisible** to Thread 2. So propagation settings become meaningless here.

```
Thread 1 has a transaction
       │
       └──> @Async creates Thread 2
                    │
                    └── Propagation = SUPPORTS
                        "use parent transaction if exists"
                        But Thread 2 has NO parent context ❌
                        So it starts a completely new transaction
                        Propagation did NOT work as expected ❌
```

### Verdict: ⚠️ Use with precaution — transaction works, but propagation from parent does not

---

## Use Case 3 ✅ — The Correct, Industry-Standard Way

### The Setup

```
UserController       UserService          UserUtility
──────────────       ───────────          ───────────
updateUserMethod()--> @Async          --> @Transactional
                      updateUser()         updateUser()
                      {                    {
                        calls               //1. update status
                        userUtility         //2. update first name
                        .updateUser()       //3. update user
                      }                    }
```

### What Happens

```
Thread 1 (Main)
│
└── calls updateUser() on UserService --> @Async spawns Thread 2
│                                               │
│   Main thread is FREE immediately ✅     Thread 2 (New)
│   No waiting                             calls UserUtility.updateUser()
│                                               │
│                                          @Transactional starts
│                                          fresh transaction on Thread 2
│                                               │
│                                          All DB operations run
│                                          inside this transaction ✅
│                                          Failure = rollback ✅
```

### Why This is the Best Way

- The main thread is freed immediately — no blocking ✅
- The async work runs in a separate thread ✅
- That separate thread has its own clean `@Transactional` — no confusion about propagation from a parent ✅
- If anything fails in the async work, rollback happens correctly ✅
- No mixing of `@Async` and `@Transactional` on the same method ✅

### Verdict: ✅ This is the industry standard

---

## Quick Summary Table

| Use Case | Setup | Problem | Use it? |
|---|---|---|---|
| 1 | `@Transactional` calls `@Async` inside | Async runs without any transaction, no rollback | ❌ Never |
| 2 | Same method has both `@Transactional` & `@Async` | Transaction works but propagation from parent breaks | ⚠️ With caution |
| 3 | `@Async` calls a separate method with `@Transactional` | Clean separation, works perfectly | ✅ Always prefer |

---

## 🎯 Interview Tip

> **Q: How do @Async and @Transactional work together? What are the challenges?**

**Answer to give:**
The core challenge is that transaction context is thread-local and does not carry forward to a new thread created by `@Async`. There are three scenarios. First, if a `@Transactional` method calls an `@Async` method inside it, the async method runs with no transaction at all — rollback won't work for it. Second, if both annotations are on the same method, the async thread gets its own fresh transaction, but any propagation settings that depend on a parent transaction will silently fail. Third, the correct industry-standard way is to put `@Async` on one method that calls a separate method in another class annotated with `@Transactional` — this gives you both async execution and proper transaction management with no conflicts.

---

# Step 3 — Return Types of @Async Methods

---

## The Core Question

When an `@Async` method runs in a new thread, your main thread has already moved on. So if that async method produces some result — **how do you get that result back?**

That's exactly what this step is about.

---

## The Three Possible Return Types

```
@Async Method Return Types
         │
         ├── void               → fire and forget, no result needed
         ├── Future<T>          → old way, deprecated now
         └── CompletableFuture<T> → modern way, use this in industry
```

---

## Return Type 1 — void (Fire and Forget)

This is the simplest case. You call the async method, it runs in a new thread, and you don't care about its result at all.

```
Main Thread                        New Thread
────────────                       ──────────
calls asyncMethod()   ──spawns──>  does its work
        │                          no result returned
        │ (doesn't wait)
        v
continues its own work
```

No mechanism needed to retrieve anything. Just call it and move on. We'll talk about exception handling for this case in Step 4.

---

## Return Type 2 — Future\<T\> (Old Way — Now Deprecated)

### What Problem Does It Solve?

Sometimes you DO need the result of the async work. For example — you send a task to a new thread to fetch some data, and at some point later you need that data in your main thread.

`Future<T>` is a placeholder — it says *"I don't have the result right now, but I will give it to you when it's ready."*

### How It Works

```
Main Thread                            New Thread (Thread 1)
────────────                           ─────────────────────
calls performTaskAsync()  ──spawns──>  does the work
        │                              returns "async task result"
        │ gets back a Future<String>          │
        │ (just a reference, not              │
        │  the actual result yet)             │
        │                                     │
        │ continues doing other work          │
        │                                     │
        │                              task completes
        │                                     │
        └──> result.get()  ◄──────────────────┘
              │
              Main thread WAITS here
              until result is available
              then prints the output
```

### Code Structure

```java
// In UserService (different class)
@Async
public Future<String> performTaskAsync() {
    return new AsyncResult<>("async task result");
}

// In UserController (caller)
public String getUserMethod() {
    Future<String> result = userService.performTaskAsync();
    // main thread is free here, can do other work

    String output = null;
    try {
        output = result.get();  // waits here until async task finishes
        System.out.println(output);
    } catch (Exception e) {
        System.out.println("some exception");
    }
    return output;
}
```

### Methods Available on Future Interface

| Method | What it does |
|---|---|
| `get()` | Waits for task to complete, then returns the result |
| `get(timeout, unit)` | Waits only for the given time, throws `TimeoutException` if not done |
| `isDone()` | Returns true if task completed (normally, with exception, or cancelled) |
| `isCancelled()` | Returns true if task was cancelled before completing |
| `cancel(boolean)` | Tries to cancel the task |

> **Note from instructor:** `Future` is now **deprecated**. `CompletableFuture` is what the industry uses today. Understand `Future` conceptually, but always prefer `CompletableFuture` in real code.

---

## Return Type 3 — CompletableFuture\<T\> (Modern Way — Use This)

### What is it?

`CompletableFuture` was introduced in **Java 8**. Think of it as an **enhanced, more powerful version of Future**. The basic usage is the same — it's a placeholder for a result that will arrive later — but it comes with a lot of extra capabilities like chaining, combining multiple async tasks, and more.

### How It Works (Basic Usage — same flow as Future)

```
Main Thread                            New Thread (Thread 1)
────────────                           ─────────────────────
calls performTaskAsync()  ──spawns──>  does the work
        │                              returns CompletableFuture
        │ gets back                    with "async task result"
        │ CompletableFuture<String>            │
        │ (just a reference)                   │
        │                                      │
        │ continues doing other work           │
        │                                      │
        │                              task completes
        │                                      │
        └──> result.get()  ◄───────────────────┘
              │
              Main thread WAITS here
              until result is available
              then prints the output
```

### Code Structure

```java
// In UserService (different class)
@Async
public CompletableFuture<String> performTaskAsync() {
    return CompletableFuture.completedFuture("async task result");
}

// In UserController (caller)
public String getUserMethod() {
    CompletableFuture<String> result = userService.performTaskAsync();
    // main thread is free here

    String output = null;
    try {
        output = result.get();  // waits here until async task finishes
        System.out.println(output);
    } catch (Exception e) {
        System.out.println("some exception");
    }
    return output;
}
```

Notice — the code change from `Future` to `CompletableFuture` is **minimal**. Just swap the return type and use `CompletableFuture.completedFuture()` instead of `new AsyncResult<>()`.

---

## Future vs CompletableFuture — Side by Side

```
Feature                  Future          CompletableFuture
───────────────────────────────────────────────────────────
Introduced in            Java 5          Java 8
Status                   Deprecated      Active, preferred
Get result               get()           get()
Chaining tasks           ❌ No           ✅ Yes (thenApply, thenAccept...)
Combine multiple tasks   ❌ No           ✅ Yes (allOf, anyOf...)
Manual completion        ❌ No           ✅ Yes (complete())
Industry usage           ❌ Avoid        ✅ Always prefer
```

> **Instructor's note:** CompletableFuture has very powerful chaining capabilities — `thenApply`, `thenAccept`, `allOf`, `anyOf` and more. These are deep Java topics. The instructor has covered them in depth in a separate **Java Multithreading video (#35 — Future, Callable & CompletableFuture)**. If you want to go deeper, check that out.

---

## The Full Picture — All Three Return Types

```
@Async Method
      │
      ├── void
      │     └── Fire and forget
      │         Main thread doesn't need result
      │         No mechanism needed
      │
      ├── Future<T>          (deprecated)
      │     └── Main thread gets a Future reference
      │         Can do other work
      │         Calls .get() when result is needed
      │         .get() blocks until async task finishes
      │
      └── CompletableFuture<T>   ✅ use this
            └── Same as Future but more powerful
                Supports chaining, combining tasks
                Industry standard today
```

---

## 🎯 Interview Tip

> **Q: What can an @Async method return?**

**Answer to give:**
An `@Async` method can return `void`, `Future<T>`, or `CompletableFuture<T>`. If you don't need the result, use `void` — fire and forget. If you need the result, wrap it in `CompletableFuture<T>`, which is the modern industry standard introduced in Java 8. `Future<T>` also works the same way but is now deprecated. In both cases, the caller gets back a reference immediately, can continue doing other work, and then calls `.get()` when it actually needs the result — at which point the main thread will wait until the async task completes.

---

# 🎯Step 4 — Exception Handling in @Async Methods

---

## The Core Question

When an `@Async` method runs in a new thread and throws an exception — **how does the main thread even know about it?**

The main thread already moved on after calling the async method. It has no way to naturally catch exceptions happening in another thread. So how do we handle this?

The answer depends on the **return type** of the async method. There are two scenarios:

```
Exception Handling in @Async
         │
         ├── Method with return type (Future / CompletableFuture)
         │     └── Simpler — exception surfaces at .get()
         │
         └── Method with void return type
               └── Tricky — exception silently disappears
                   Two ways to handle it:
                   ├── try-catch inside the async method
                   └── Custom AsyncUncaughtExceptionHandler ✅ (industry standard)
```

---

## Scenario 1 — Async Method WITH a Return Type

This one is straightforward. Since the caller holds a `Future` or `CompletableFuture` reference, the exception gets **stored inside that future object** and only surfaces when you call `.get()`.

### How It Works

```
Main Thread                          New Thread
────────────                         ──────────
calls performTaskAsync()  ──spawns──>  does work
        │                              💥 exception occurs!
        │ gets Future reference         exception stored inside
        │                              the Future object
        │ continues other work
        │
        └──> result.get()
              │
              💥 Exception is THROWN HERE
              Main thread catches it in try-catch
              Handle however you want —
              log it, rethrow it, print it
```

### Code Structure

```java
// Caller (Main Thread)
public String getUserMethod() {
    CompletableFuture<String> result = userService.performTaskAsync();
    // main thread free here

    String output = null;
    try {
        output = result.get();  // 💥 if async threw exception,
                                //    it surfaces HERE
        System.out.println(output);
    } catch (Exception e) {
        System.out.println("some exception");  // handle it here
    }
    return output;
}
```

Simple. The exception travels through the future object and reaches the main thread exactly at the `.get()` call. You catch it normally.

---

## Scenario 2 — Async Method with VOID Return Type

This is where it gets tricky and interesting.

```java
// In UserService
@Async
public void performTaskAsync() {
    // perform some task
    // 💥 what if exception happens here?
}

// In UserController
public String getUserMethod() {
    userService.performTaskAsync();  // fire and forget
    return "";
    // main thread already gone — has NO idea about exception
}
```

The main thread called the async method and moved on. There's no Future reference. There's no `.get()`. If an exception happens in the new thread — **it silently disappears** unless you handle it.

### Two Ways to Handle This

---

### Way 1 — try-catch Inside the Async Method Itself

```java
@Async
public void performTaskAsync() {
    try {
        // perform some task
    } catch (Exception e) {
        // handle exception here
        // log it, alert, whatever
    }
}
```

This works. But the problem is — if you have **many async void methods**, you'd have to write try-catch inside **every single one** of them. That's repetitive and hard to maintain.

There's a better, cleaner way.

---

### Way 2 — Custom AsyncUncaughtExceptionHandler ✅ (Industry Standard)

Before understanding the solution, let's first see what **Spring Boot does by default** when an exception occurs in a void async method.

### What Spring Boot Does By Default

Spring Boot already has a built-in class for this:

```
Spring Boot's built-in class:
──────────────────────────────
SimpleAsyncUncaughtExceptionHandler
        implements AsyncUncaughtExceptionHandler
        │
        └── handleUncaughtException(Throwable ex, Method method, Object... params)
                  │
                  └── logs: "Unexpected exception occurred invoking async method: "
                            + method name + exception details
```

So if you do nothing, Spring will catch the exception and log it automatically. That's the default behavior.

**Default output looks like:**

```
task-5] s.a.SimpleAsyncUncaughtExceptionHandler :
Unexpected exception occurred invoking async method
java.lang.ArithmeticException: / by zero
    at UserService.performTaskAsync(UserService.java:18)
```

It logs the exception and the method name. But you have no control over what happens — you can't add custom logic, custom alerts, or custom logging format.

---

### Building Your Own Custom Handler

The instructor's key insight here is:

> *"If Spring Boot can implement this interface, why can't we?"*
>

Since `SimpleAsyncUncaughtExceptionHandler` implements `AsyncUncaughtExceptionHandler`, you can write your own class that does the same — but with **your own custom logic**.

### Step by Step

**Step 1 — Create your custom handler class:**

```java
@Component
class DefaultAsyncUncaughtExceptionHandler
        implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(
            Throwable ex, Method method, Object... params) {

        System.out.println("in default Uncaught Exception method");
        // Your custom logic here:
        // - custom logging
        // - send alert/notification
        // - write to error DB table
        // - whatever your business needs
    }
}
```

**Step 2 — Register your handler with Spring:**

```java
@Configuration
public class AppConfig implements AsyncConfigurer {

    @Autowired
    private AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler;

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this.asyncUncaughtExceptionHandler;
                // Spring asks "who handles uncaught async exceptions?"
                // You return YOUR handler here
    }
}
```

Spring Boot will call `getAsyncUncaughtExceptionHandler()` whenever it needs to handle an uncaught exception from a void async method. Since you've returned YOUR handler, your custom logic runs instead of Spring's default.

---

## The Full Flow — Custom Handler in Action

```
New Thread
──────────
performTaskAsync() runs
        │
        💥 Exception occurs (e.g. 5/0 = ArithmeticException)
        │
        No try-catch in the method
        │
        Spring Boot kicks in
        │
        Calls getAsyncUncaughtExceptionHandler()
        │
        AppConfig returns YOUR custom handler
        │
        handleUncaughtException() runs
        │
        Your custom logic executes ✅
        (logging, alerts, etc.)
```

**Custom handler output:**

```
[nio-8080-exec-1] : in default Uncaught Exception method
```

Your message, your logic, your control.

---

## Complete Picture — All Exception Handling Paths

```
@Async Exception Handling
         │
         ├── Return type: Future / CompletableFuture
         │         │
         │         └── Exception stored in Future object
         │             Surfaces when caller calls .get()
         │             Catch it in try-catch at .get() ✅
         │
         └── Return type: void
                   │
                   ├── Way 1: try-catch inside async method
                   │         Works but repetitive for many methods ⚠️
                   │
                   └── Way 2: Custom AsyncUncaughtExceptionHandler
                             Implement AsyncUncaughtExceptionHandler
                             Register via AsyncConfigurer
                             One place handles ALL void async exceptions ✅
                             Industry standard ✅
```

---

## 🎯 Interview Tip

> **Q: How do you handle exceptions in @Async methods?**
>

**Answer to give:**
It depends on the return type. If the async method returns a `CompletableFuture` or `Future`, the exception gets stored inside the future object and surfaces when the caller calls `.get()` — you simply wrap that in a try-catch. The tricky case is when the return type is `void`, because the main thread has already moved on and has no reference to catch from. Here you have two options — either put a try-catch inside every async method, which is repetitive, or better, implement a global `AsyncUncaughtExceptionHandler`. You create a class implementing that interface, override `handleUncaughtException()` with your custom logic, and register it by implementing `AsyncConfigurer` in your config class and returning your handler from `getAsyncUncaughtExceptionHandler()`. Spring will automatically invoke your handler whenever any void async method throws an uncaught exception. This is the industry standard approach.

---

## Full Lecture Summary

| Topic | Key Takeaway |
| --- | --- |
| Conditions for @Async | Must be public + must be in a different class (AOP proxy needs cross-class calls) |
| @Async + @Transactional | Never nest async inside transactional; correct way is async calls a separate transactional method |
| Return Types | Use `CompletableFuture<T>` for results, `void` for fire-and-forget |
| Exception Handling | For void methods, use a global `AsyncUncaughtExceptionHandler` — clean, centralized, industry standard |

---

That wraps up the full **@Async Part 2** lecture! All four steps are done. Let me know if you want to revisit anything, go deeper on any topic, or move to the next lecture! 🎯