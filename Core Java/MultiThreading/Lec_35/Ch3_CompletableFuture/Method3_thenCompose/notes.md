# CompletableFuture - thenCompose() & thenComposeAsync()

## 📌 Table of Contents
1. [Introduction](#introduction)
2. [What is thenCompose()?](#what-is-thencompose)
3. [thenApply vs thenCompose](#thenapply-vs-thencompose)
4. [Method Signatures](#method-signatures)
5. [Ordering Guarantee](#ordering-guarantee)
6. [Thread Execution Behavior](#thread-execution-behavior)
7. [Code Examples](#code-examples)
8. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction

### The Problem thenCompose Solves

**Scenario:** You have multiple **dependent async operations** that need to execute in order.
```java
// Problem: Nested CompletableFutures
CompletableFuture<CompletableFuture<String>> nested = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenApply(s -> CompletableFuture.supplyAsync(() -> s + " World"));

// Solution: Flatten with thenCompose
CompletableFuture<String> flattened = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));
```

---

## 💡 What is thenCompose()?

### Definition

**thenCompose()** chains together **dependent async operations** where the next operation depends on the result of the previous one.

### Purpose

- ✅ Chain dependent async tasks
- ✅ Maintain ordering between async operations
- ✅ Flatten nested `CompletableFuture`
- ✅ Avoid `CompletableFuture<CompletableFuture<T>>` nesting

---

### Key Characteristic: Ordering

> **Critical:** When you have multiple async tasks with dependencies, `thenCompose()` **guarantees ordering**
```
Async Task 1 → Must complete BEFORE → Async Task 2 → Must complete BEFORE → Async Task 3
```

**Without ordering guarantee:**
- Tasks might execute in unpredictable order
- Race conditions
- Incorrect results

**With thenCompose:**
- Task 2 starts only after Task 1 completes
- Task 3 starts only after Task 2 completes
- ✅ **Ordering guaranteed**

---

## 🔄 thenApply vs thenCompose

### Visual Comparison

#### thenApply() - Transform Result
```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")  // Simple transformation
    .thenApply(s -> s + "!")       // Simple transformation
```
```
"Hello" → thenApply → "Hello World" → thenApply → "Hello World!"
```

**Returns:** Transformed value directly

---

#### thenCompose() - Chain Async Operations
```java
CompletableFuture.supplyAsync(() -> "Hello")
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"))
    .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + "!"))
```
```
"Hello" → thenCompose → Another Async Task → "Hello World" 
                     → thenCompose → Another Async Task → "Hello World!"
```

**Returns:** New `CompletableFuture` (another async operation)

---

### Key Differences

| Aspect | thenApply() | thenCompose() |
|--------|-------------|---------------|
| **Input** | Takes result `T` | Takes result `T` |
| **Returns** | Returns value `U` | Returns `CompletableFuture<U>` |
| **Use Case** | Simple transformation | Chain async operations |
| **Result Type** | `CompletableFuture<U>` | `CompletableFuture<U>` (flattened) |
| **Nesting** | No nesting | Prevents nesting |

---

### The Nesting Problem
```java
// ❌ Wrong - Creates nested CompletableFuture
CompletableFuture<CompletableFuture<String>> wrong = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenApply(s -> CompletableFuture.supplyAsync(() -> s + " World"));
// Type: CompletableFuture<CompletableFuture<String>> - NESTED!

// ✅ Correct - Flattens with thenCompose
CompletableFuture<String> correct = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));
// Type: CompletableFuture<String> - FLAT!
```

---

## 📝 Method Signatures

### thenCompose()
```java
public <U> CompletableFuture<U> thenCompose(
    Function<? super T, ? extends CompletionStage<U>> fn
)
```

**Key Points:**
- Input: `T` (result from previous stage)
- Function returns: `CompletionStage<U>` (another async operation)
- Final return: `CompletableFuture<U>` (flattened)

---

### thenComposeAsync()
```java
// Variant 1: Uses ForkJoinPool
public <U> CompletableFuture<U> thenComposeAsync(
    Function<? super T, ? extends CompletionStage<U>> fn
)

// Variant 2: Uses custom executor
public <U> CompletableFuture<U> thenComposeAsync(
    Function<? super T, ? extends CompletionStage<U>> fn,
    Executor executor
)
```

---

### Understanding the Function Parameter
```java
Function<? super T, ? extends CompletionStage<U>>
```

**Breaking it down:**
- `? super T` → Accepts `T` and its parent types
- `? extends CompletionStage<U>` → Returns `CompletionStage<U>` or its subtypes

**In practice:**
```java
.thenCompose(value -> {
    // value is of type T (from previous stage)
    // Must return CompletableFuture<U> or CompletionStage<U>
    return CompletableFuture.supplyAsync(() -> /* compute U */);
})
```

---

## 🔒 Ordering Guarantee

### Why Ordering Matters

**Scenario with multiple async operations:**
```java
Async Task 1: Fetch user data
Async Task 2: Fetch user orders (depends on user ID from Task 1)
Async Task 3: Calculate total (depends on orders from Task 2)
```

**Without ordering:**
- Task 2 might start before Task 1 completes → No user ID → Error!
- Task 3 might start before Task 2 completes → No orders → Error!

**With thenCompose:**
```java
CompletableFuture.supplyAsync(() -> fetchUser())           // Task 1
    .thenCompose(user -> fetchOrders(user.getId()))        // Task 2 - waits for Task 1
    .thenCompose(orders -> calculateTotal(orders))         // Task 3 - waits for Task 2
```

✅ **Ordering guaranteed!**

---

### Internal Mechanism

**thenCompose maintains a stack of dependent actions:**
```
CompletionStage maintains:
    Stack of Dependent Actions
        ↓
    Action 1 (must complete first)
        ↓
    Action 2 (waits for Action 1)
        ↓
    Action 3 (waits for Action 2)
```

**Even with `thenComposeAsync()`:**
- Actions execute in **different threads**
- But **ordering is maintained**
- Next action starts only after previous completes

---

### Ordering Example
```java
CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 1");
    return "A";
})
.thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 2");
    return s + "B";
}))
.thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 3");
    return s + "C";
}));
```

**Output is ALWAYS:**
```
Task 1
Task 2
Task 3
```

**Never:**
```
Task 1
Task 3  ← Will NEVER happen
Task 2
```

---

## 🧵 Thread Execution Behavior

### thenCompose() - Synchronous
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Stage 1: " + Thread.currentThread().getName());
        return "Hello";
    }, executor)
    .thenCompose(s -> {
        System.out.println("Stage 2: " + Thread.currentThread().getName());
        return CompletableFuture.supplyAsync(() -> s + " World", executor);
    });
```

**Thread behavior:**
- Stage 1: Uses thread from executor
- Stage 2: **Same thread** that completed Stage 1
- Inner `supplyAsync()`: Uses new thread from executor

---

### thenComposeAsync() - Asynchronous
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Stage 1: " + Thread.currentThread().getName());
        return "Hello";
    }, executor)
    .thenComposeAsync(s -> {
        System.out.println("Stage 2: " + Thread.currentThread().getName());
        return CompletableFuture.supplyAsync(() -> s + " World", executor);
    }, executor);
```

**Thread behavior:**
- Stage 1: Uses thread from executor
- Stage 2: **New thread** from executor
- Inner `supplyAsync()`: Uses another thread from executor

---

## 💻 Code Examples

### Example 1: Basic thenCompose()
```java
import java.util.concurrent.*;

public class ThenComposeExample {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 2, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10)
        );

        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Task 1: " + Thread.currentThread().getName());
                return "Hello";
            }, executor)
            .thenCompose(value -> {
                System.out.println("Composing: " + Thread.currentThread().getName());
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("Task 2: " + Thread.currentThread().getName());
                    return value + " World";
                }, executor);
            });

        String result = future.get();
        System.out.println("Result: " + result);

        executor.shutdown();
    }
}
```

**Output:**
```
Task 1: pool-1-thread-1
Composing: pool-1-thread-1       ← Same thread
Task 2: pool-1-thread-2          ← New async task
Result: Hello World
```

---

### Example 2: thenComposeAsync()
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Task 1: " + Thread.currentThread().getName());
        return "Hello";
    }, executor)
    .thenComposeAsync(value -> {
        System.out.println("Composing: " + Thread.currentThread().getName());
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Task 2: " + Thread.currentThread().getName());
            return value + " World";
        }, executor);
    }, executor);

String result = future.get();
System.out.println("Result: " + result);
```

**Output:**
```
Task 1: pool-1-thread-1
Composing: pool-1-thread-2       ← Different thread
Task 2: pool-1-thread-1 or pool-1-thread-2
Result: Hello World
```

---

### Example 3: Multiple thenCompose() Chain
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Stage 1: Fetch user");
        return "User123";
    }, executor)
    .thenCompose(userId -> {
        System.out.println("Stage 2: Fetch orders for " + userId);
        return CompletableFuture.supplyAsync(() -> {
            return "Orders[1,2,3]";
        }, executor);
    })
    .thenCompose(orders -> {
        System.out.println("Stage 3: Calculate total for " + orders);
        return CompletableFuture.supplyAsync(() -> {
            return "Total: $150";
        }, executor);
    });

String result = future.get();
System.out.println("Final: " + result);
```

**Output:**
```
Stage 1: Fetch user
Stage 2: Fetch orders for User123
Stage 3: Calculate total for Orders[1,2,3]
Final: Total: $150
```

**Key:** Each stage waits for previous to complete - **ordering guaranteed!**

---

### Example 4: Ordering Test with thenComposeAsync()
```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        System.out.println("Task A");
        return "A";
    })
    .thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        System.out.println("Task B");
        return s + "B";
    }))
    .thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
        System.out.println("Task C");
        return s + "C";
    }));

String result = future.get();
System.out.println("Result: " + result);
```

**Output (ALWAYS):**
```
Task A
Task B
Task C
Result: ABC
```

**Never:** Task C before Task B, or Task B before Task A

---

### Example 5: Demonstrating Ordering Guarantee
```java
// Run this multiple times - order NEVER changes
for (int i = 0; i < 5; i++) {
    System.out.println("\n=== Run " + (i + 1) + " ===");
    
    CompletableFuture<String> future = CompletableFuture
        .supplyAsync(() -> {
            System.out.println("1");
            return "A";
        })
        .thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
            System.out.println("2");
            return s + "B";
        }))
        .thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> {
            System.out.println("3");
            return s + "C";
        }));
    
    future.get();
}
```

**Output (all 5 runs):**
```
=== Run 1 ===
1
2
3

=== Run 2 ===
1
2
3

=== Run 3 ===
1
2
3
...
```

**Always in order!** 1 → 2 → 3

---

### Example 6: Real-World Use Case
```java
// Simulated API calls
static CompletableFuture<String> getUserId(String username) {
    return CompletableFuture.supplyAsync(() -> {
        System.out.println("Fetching user ID for: " + username);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return "USER_" + username.toUpperCase();
    });
}

static CompletableFuture<String> getOrdersForUser(String userId) {
    return CompletableFuture.supplyAsync(() -> {
        System.out.println("Fetching orders for: " + userId);
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        return "Orders: [Order1, Order2, Order3]";
    });
}

static CompletableFuture<String> calculateTotal(String orders) {
    return CompletableFuture.supplyAsync(() -> {
        System.out.println("Calculating total for: " + orders);
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        return "Total: $299.99";
    });
}

// Chain them with thenCompose
public static void main(String[] args) throws Exception {
    CompletableFuture<String> result = getUserId("john")
        .thenCompose(userId -> getOrdersForUser(userId))
        .thenCompose(orders -> calculateTotal(orders));
    
    System.out.println("Final Result: " + result.get());
}
```

**Output:**
```
Fetching user ID for: john
Fetching orders for: USER_JOHN
Calculating total for: Orders: [Order1, Order2, Order3]
Final Result: Total: $299.99
```

**Perfect ordering!** Each step depends on the previous one.

---

### Example 7: thenApply vs thenCompose Comparison
```java
// ❌ Wrong - thenApply with async operation creates nesting
CompletableFuture<CompletableFuture<String>> wrong = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenApply(s -> CompletableFuture.supplyAsync(() -> s + " World"));

// To get the value, need double get() - ugly!
String wrongResult = wrong.get().get();  // ❌ Double get()

// ✅ Correct - thenCompose flattens
CompletableFuture<String> correct = 
    CompletableFuture.supplyAsync(() -> "Hello")
        .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));

// Single get() - clean!
String correctResult = correct.get();  // ✅ Single get()

System.out.println(wrongResult);    // Hello World
System.out.println(correctResult);  // Hello World
```

---

## 🎓 Key Takeaways

### ✅ Summary

1. **thenCompose() Purpose**
    - Chain dependent async operations
    - Flatten nested `CompletableFuture`
    - Maintain ordering between async tasks

2. **When to Use**
    - When next async operation depends on previous result
    - When you need guaranteed execution order
    - When chaining multiple API calls or async tasks

3. **Ordering Guarantee**
    - ✅ Tasks execute in order even with `Async` variant
    - ✅ Maintained internally via dependent actions stack
    - ✅ No race conditions

4. **Thread Behavior**
```java
   thenCompose()       // Same thread
   thenComposeAsync()  // New thread
```

5. **vs thenApply()**
    - `thenApply()` → Simple transformation of value
    - `thenCompose()` → Chain another async operation

---

### 📊 Decision Matrix

| Scenario | Use |
|----------|-----|
| Transform result to another value | `thenApply()` |
| Chain another async operation | `thenCompose()` |
| Need ordering guarantee | `thenCompose()` |
| Dependent API calls | `thenCompose()` |
| Want to flatten nested futures | `thenCompose()` |

---

### ⚠️ Important Notes

> **Flattening**: `thenCompose()` prevents `CompletableFuture<CompletableFuture<T>>` nesting

> **Ordering**: Even `thenComposeAsync()` maintains execution order - guaranteed!

> **Dependencies**: Use when Task 2 depends on Task 1's result

> **Real-World**: Perfect for chaining API calls, database queries, etc.

---

### 🔍 Visual Summary
```
thenApply: T → U
    ↓
CompletableFuture<U>

thenCompose: T → CompletableFuture<U>
    ↓
CompletableFuture<U> (flattened)
```

---

## 📝 Quick Reference
```java
// Basic thenCompose
CompletableFuture.supplyAsync(() -> "Value")
    .thenCompose(v -> CompletableFuture.supplyAsync(() -> v + " More"));

// thenComposeAsync with executor
CompletableFuture.supplyAsync(() -> "Value", executor)
    .thenComposeAsync(v -> CompletableFuture.supplyAsync(() -> v + " More"), executor);

// Multiple chaining with ordering guarantee
CompletableFuture.supplyAsync(() -> step1())
    .thenCompose(r1 -> step2(r1))
    .thenCompose(r2 -> step3(r2))
    .thenCompose(r3 -> step4(r3));
// Order: step1 → step2 → step3 → step4 (GUARANTEED)
```

---

### 🎯 Progress Check

**Completed:**
1. ✅ supplyAsync()
2. ✅ thenApply() / thenApplyAsync()
3. ✅ thenCompose() / thenComposeAsync()

**Remaining:**
4. ⏭️ thenAccept() / thenAcceptAsync()
5. ⏭️ thenCombine() / thenCombineAsync()

---

*Next: [CompletableFuture - thenAccept() & thenAcceptAsync() →](#)*