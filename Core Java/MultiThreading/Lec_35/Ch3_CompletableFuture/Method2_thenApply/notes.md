# CompletableFuture - thenApply() & thenApplyAsync()

## 📌 Table of Contents
1. [Introduction](#introduction)
2. [What is thenApply()?](#what-is-thenapply)
3. [Method Signatures](#method-signatures)
4. [thenApply vs thenApplyAsync](#thenapply-vs-thenapplyasync)
5. [Thread Execution Behavior](#thread-execution-behavior)
6. [Code Examples](#code-examples)
7. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction

### The Chaining Concept

**CompletableFuture's biggest advantage:** **Chaining operations**
```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(s -> s + "!")
    .thenAccept(System.out::println);
```

**Result:** Each operation builds on the previous one!

---

## 💡 What is thenApply()?

### Definition

**thenApply()** applies a function to the result of the previous async computation.

### Purpose

- ✅ Transform the result of previous stage
- ✅ Chain multiple operations
- ✅ Returns a new `CompletableFuture`
- ✅ Enables pipeline-style programming

---

### Visual Representation
```
supplyAsync()          thenApply()           thenApply()
     ↓                      ↓                     ↓
  "Concept"  →  "Concept" + " And "  →  "Concept And " + "Coding"
     ↓                      ↓                     ↓
CompletableFuture<String> → CompletableFuture<String> → CompletableFuture<String>
```

---

### Basic Flow
```
Main Thread
    |
    |--- supplyAsync(() -> "Concept")
    |         ↓
    |    Thread-1 executes → returns "Concept"
    |         ↓
    |    thenApply(s -> s + " And ")
    |         ↓
    |    Transforms to "Concept And "
    |         ↓
    |    CompletableFuture<String>
```

---

## 📝 Method Signatures

### thenApply()
```java
public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn)
```

**Parameters:**
- `Function<T, U>` - Takes input of type `T`, returns output of type `U`

**Returns:**
- `CompletableFuture<U>` - New CompletableFuture with transformed result

---

### thenApplyAsync()
```java
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn)
```
```java
public <U> CompletableFuture<U> thenApplyAsync(
    Function<? super T, ? extends U> fn,
    Executor executor
)
```

**Two Variants:**
1. Without executor - Uses ForkJoinPool
2. With executor - Uses your custom pool

---

## 🔄 thenApply vs thenApplyAsync

### Key Difference: Thread Execution

| Method | Thread Used | Execution |
|--------|-------------|-----------|
| **thenApply()** | Same thread that completed previous stage | **Synchronous** |
| **thenApplyAsync()** | New thread from pool | **Asynchronous** |

---

### Visual Comparison

#### thenApply() - Synchronous
```
Main Thread          Thread-1
    |                    |
    |-- supplyAsync ---->|
    |                    | executes supplier
    |                    | returns result
    |                    |
    |                    | (SAME thread continues)
    |                    | executes thenApply
    |                    | returns result
    |                    |
    |<---- result -------|
```

**Thread-1 does BOTH tasks sequentially**

---

#### thenApplyAsync() - Asynchronous
```
Main Thread          Thread-1              Thread-2
    |                    |                     |
    |-- supplyAsync ---->|                     |
    |                    | executes supplier   |
    |                    | returns result      |
    |                    | (releases)          |
    |                    |                     |
    |-- thenApplyAsync -------------------->   |
    |                                          | executes function
    |                                          | returns result
    |                                          |
    |<------------- result --------------------|
```

**Thread-1 finishes and releases. Thread-2 picks up next stage**

---

## 🧵 Thread Execution Behavior

### Synchronous Execution: thenApply()

**When to use:**
- When transformation is lightweight
- When you want to keep thread busy
- When you don't want overhead of thread switching
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("supplyAsync: " + Thread.currentThread().getName());
        return "Concept";
    })
    .thenApply(s -> {
        System.out.println("thenApply: " + Thread.currentThread().getName());
        return s + "And";
    });
```

**Output:**
```
supplyAsync: pool-1-thread-1
thenApply: pool-1-thread-1     ← SAME THREAD!
```

---

### Asynchronous Execution: thenApplyAsync()

**When to use:**
- When transformation is heavyweight
- When you want to release the thread quickly
- When you want parallel execution
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("supplyAsync: " + Thread.currentThread().getName());
        return "Concept";
    })
    .thenApplyAsync(s -> {
        System.out.println("thenApplyAsync: " + Thread.currentThread().getName());
        return s + "And";
    });
```

**Output:**
```
supplyAsync: pool-1-thread-1
thenApplyAsync: ForkJoinPool.commonPool-worker-1    ← DIFFERENT THREAD!
```

---

### With Custom Executor
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2, 5, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        return "Concept";
    }, executor)
    .thenApplyAsync(s -> {
        return s + "And";
    }, executor);  // Use same executor
```

**Now both stages use YOUR executor!**

---

## 💻 Code Examples

### Example 1: Basic thenApply() Chain
```java
import java.util.concurrent.*;

public class ThenApplyExample {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10)
        );

        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Thread for supplyAsync: " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(5000); // Simulate work
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Concept";
            }, executor)
            .thenApply(val -> {
                System.out.println("Thread for thenApply: " + 
                    Thread.currentThread().getName());
                return val + "And";
            });

        String result = future.get();
        System.out.println("Result: " + result);

        executor.shutdown();
    }
}
```

**Output:**
```
Thread for supplyAsync: pool-1-thread-1
(waits 5 seconds...)
Thread for thenApply: pool-1-thread-1     ← SAME THREAD
Result: ConceptAnd
```

**Explanation:**
- Thread-1 executes `supplyAsync()` → returns "Concept"
- **Same** Thread-1 executes `thenApply()` → returns "ConceptAnd"

---

### Example 2: thenApplyAsync() with Different Thread
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Thread for supplyAsync: " + 
            Thread.currentThread().getName());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Concept";
    }, executor)
    .thenApplyAsync(val -> {
        System.out.println("Thread for thenApplyAsync: " + 
            Thread.currentThread().getName());
        return val + "And";
    }); // No executor - uses ForkJoinPool

String result = future.get();
System.out.println("Result: " + result);
```

**Output:**
```
Thread for supplyAsync: pool-1-thread-1
(waits 5 seconds...)
Thread for thenApplyAsync: ForkJoinPool.commonPool-worker-1   ← DIFFERENT!
Result: ConceptAnd
```

**Explanation:**
- Thread-1 from custom pool executes `supplyAsync()`
- Thread-1 releases
- New thread from ForkJoinPool executes `thenApplyAsync()`

---

### Example 3: thenApplyAsync() with Custom Executor
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Thread for supplyAsync: " + 
            Thread.currentThread().getName());
        return "Concept";
    }, executor)
    .thenApplyAsync(val -> {
        System.out.println("Thread for thenApplyAsync: " + 
            Thread.currentThread().getName());
        return val + "And";
    }, executor); // Pass custom executor

String result = future.get();
System.out.println("Result: " + result);
```

**Output:**
```
Thread for supplyAsync: pool-1-thread-1
Thread for thenApplyAsync: pool-1-thread-1 (or pool-1-thread-2)
Result: ConceptAnd
```

**Explanation:**
- Uses YOUR executor for both stages
- Could be same thread (if available) or different thread from same pool
- **Full control** over thread pool

---

### Example 4: Multiple thenApply() Chaining
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Stage 1: " + Thread.currentThread().getName());
        return "Concept";
    }, executor)
    .thenApply(s -> {
        System.out.println("Stage 2: " + Thread.currentThread().getName());
        return s + "And";
    })
    .thenApply(s -> {
        System.out.println("Stage 3: " + Thread.currentThread().getName());
        return s + "Coding";
    });

String result = future.get();
System.out.println("Final Result: " + result);
```

**Output:**
```
Stage 1: pool-1-thread-1
Stage 2: pool-1-thread-1
Stage 3: pool-1-thread-1
Final Result: ConceptAndCoding
```

**Explanation:**
- All `thenApply()` stages use the **same thread**
- Sequential execution on single thread

---

### Example 5: Mixed - thenApply() and thenApplyAsync()
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Stage 1: " + Thread.currentThread().getName());
        return "Concept";
    }, executor)
    .thenApply(s -> {
        System.out.println("Stage 2: " + Thread.currentThread().getName());
        return s + "And";
    })
    .thenApplyAsync(s -> {
        System.out.println("Stage 3: " + Thread.currentThread().getName());
        return s + "Coding";
    }, executor);

String result = future.get();
System.out.println("Final Result: " + result);
```

**Output:**
```
Stage 1: pool-1-thread-1
Stage 2: pool-1-thread-1      ← Same as Stage 1
Stage 3: pool-1-thread-1 or pool-1-thread-2    ← Could be different
Final Result: ConceptAndCoding
```

**Explanation:**
- Stage 1 & 2: Same thread (thenApply is synchronous)
- Stage 3: Could be different thread (thenApplyAsync is asynchronous)

---

### Example 6: Transform Return Type
```java
// String → Integer transformation
CompletableFuture<Integer> future = CompletableFuture
    .supplyAsync(() -> "123", executor)
    .thenApply(s -> {
        return Integer.parseInt(s); // String to Integer
    })
    .thenApply(num -> {
        return num * 2; // Integer to Integer
    });

Integer result = future.get();
System.out.println("Result: " + result); // 246
```

**Key Point:**
> **thenApply() can change the return type!** String → Integer → Integer

---

### Example 7: Chaining Style Comparison
```java
// Style 1: Separate line for each stage
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> cf2 = cf1.thenApply(s -> s + " World");
CompletableFuture<String> cf3 = cf2.thenApply(s -> s + "!");

// Style 2: Method chaining (more readable)
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(s -> s + "!");

// Both produce same result
String result = future.get();
System.out.println(result); // Hello World!
```

**Recommended:** Use **method chaining** for readability!

---

## 🎓 Key Takeaways

### ✅ Summary

1. **thenApply() Purpose**
    - Transforms result of previous stage
    - Returns new `CompletableFuture`
    - Enables chaining

2. **Two Variants**
```java
   thenApply()        // Synchronous - same thread
   thenApplyAsync()   // Asynchronous - new thread
```

3. **Thread Behavior**
    - `thenApply()` → Same thread continues
    - `thenApplyAsync()` → New thread picks up

4. **When to Use Which?**
    - **Lightweight transformation** → `thenApply()`
    - **Heavy transformation** → `thenApplyAsync()`
    - **Release thread quickly** → `thenApplyAsync()`

5. **Can Transform Types**
```java
   String → Integer → Double → Custom Object
```

---

### 📊 Decision Table

| Scenario | Use |
|----------|-----|
| Quick transformation (< 100ms) | `thenApply()` |
| Heavy computation | `thenApplyAsync()` |
| Want to release thread | `thenApplyAsync()` |
| Don't care about thread | `thenApply()` |
| Need specific executor | `thenApplyAsync(fn, executor)` |

---

### ⚠️ Important Notes

> **Synchronous vs Asynchronous**: The key difference is **which thread executes the function**

> **Thread Reuse**: `thenApply()` keeps the thread busy - efficient for quick operations

> **Thread Release**: `thenApplyAsync()` releases the thread - better for long operations

> **Executor Control**: Pass custom executor to `thenApplyAsync()` for full control

> **Chaining**: You can chain unlimited `thenApply()` / `thenApplyAsync()` calls

---

### 🔍 Visual Summary
```
supplyAsync()
    ↓
Returns: CompletableFuture<T>
    ↓
.thenApply(T → U)
    ↓
Returns: CompletableFuture<U>
    ↓
.thenApply(U → V)
    ↓
Returns: CompletableFuture<V>
    ↓
.get() → V
```

---

## 📝 Quick Reference
```java
// Basic thenApply - same thread
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World");

// thenApplyAsync - new thread (ForkJoinPool)
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApplyAsync(s -> s + " World");

// thenApplyAsync with custom executor
CompletableFuture.supplyAsync(() -> "Hello", executor)
    .thenApplyAsync(s -> s + " World", executor);

// Multiple chaining
CompletableFuture.supplyAsync(() -> "A")
    .thenApply(s -> s + "B")
    .thenApply(s -> s + "C")
    .thenApply(s -> s + "D");
```

---

### 🎯 Next Method

**Completed:**
1. ✅ supplyAsync()
2. ✅ thenApply() / thenApplyAsync()

**Remaining:**
3. ⏭️ thenCompose() / thenComposeAsync()
4. ⏭️ thenAccept() / thenAcceptAsync()
5. ⏭️ thenCombine() / thenCombineAsync()

---

*Next: [CompletableFuture - thenCompose() & thenComposeAsync() →](#)*