# CompletableFuture - thenAccept() & thenAcceptAsync()

## 📌 Table of Contents
1. [Introduction](#introduction)
2. [What is thenAccept()?](#what-is-thenaccept)
3. [Method Signatures](#method-signatures)
4. [thenAccept as Terminal Operation](#thenaccept-as-terminal-operation)
5. [Thread Execution Behavior](#thread-execution-behavior)
6. [Code Examples](#code-examples)
7. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction

### The Concept

**thenAccept()** is used when you want to **consume a result** without returning anything.
```java
CompletableFuture.supplyAsync(() -> "Result")
    .thenAccept(value -> {
        System.out.println("Received: " + value);
        // No return statement
    });
```

**Key Characteristic:** Accepts input but returns **void** (nothing)

---

## 💡 What is thenAccept()?

### Definition

**thenAccept()** is generally considered the **end stage** in a chain of async operations.

### Purpose

- ✅ Consume/process the result
- ✅ Perform side effects (logging, printing, saving to DB, etc.)
- ✅ End the processing chain
- ❌ Does NOT return a value

---

### Functional Interface: Consumer
```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
```

**Characteristics:**
- Accepts one input parameter `T`
- Returns `void` (nothing)
- Used for side effects

---

### Visual Flow
```
supplyAsync()          thenAccept()
     ↓                      ↓
  Returns "Hello"    →  Prints "Hello"
     ↓                      ↓
CompletableFuture<String> → CompletableFuture<Void>
                              ↓
                         END OF CHAIN
```

---

## 📝 Method Signatures

### thenAccept()
```java
public CompletableFuture<Void> thenAccept(Consumer<? super T> action)
```

**Parameters:**
- `Consumer<T>` - Accepts input of type `T`, returns nothing

**Returns:**
- `CompletableFuture<Void>` - Future with no value

---

### thenAcceptAsync()
```java
// Variant 1: Uses ForkJoinPool
public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action)

// Variant 2: Uses custom executor
public CompletableFuture<Void> thenAcceptAsync(
    Consumer<? super T> action,
    Executor executor
)
```

---

## 🛑 thenAccept as Terminal Operation

### Why It's Terminal
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(value -> {
        System.out.println("Printing: " + value);
    });
// Returns CompletableFuture<Void>
```

**What happens if you try to chain more?**
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(value -> {
        System.out.println(value);
    })
    .thenApply(???);  // ❌ What to pass? It's void!
```

---

### The Problem with Chaining After thenAccept()
```java
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");

CompletableFuture<Void> cf2 = cf1.thenAccept(s -> {
    System.out.println(s);
});

// Now try to chain thenApply
cf2.thenApply(voidValue -> {
    // What is voidValue? It's void!
    // You have to accept void and above (using wildcards)
    // This makes no sense!
    return "Something"; // ❌ Doesn't work logically
});
```

**Issue:**
- `thenAccept()` returns `CompletableFuture<Void>`
- Next stage must accept `Void` type
- `Void` has no meaningful value
- **Cannot chain meaningfully after `thenAccept()`**

---

### Correct Usage Pattern
```java
// ✅ Correct - thenAccept at the end
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(s -> s + "!")
    .thenAccept(result -> {
        System.out.println("Final result: " + result);
    });
// End of chain - nothing after thenAccept

// ❌ Wrong - trying to chain after thenAccept
CompletableFuture.supplyAsync(() -> "Hello")
    .thenAccept(s -> System.out.println(s))
    .thenApply(v -> v + "???");  // What is v? Void!
```

---

## 🧵 Thread Execution Behavior

### thenAccept() - Synchronous

**Uses the same thread** that completed the previous stage.
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Supply: " + Thread.currentThread().getName());
        return "Hello";
    }, executor)
    .thenAccept(value -> {
        System.out.println("Accept: " + Thread.currentThread().getName());
        System.out.println("Value: " + value);
    });
```

**Output:**
```
Supply: pool-1-thread-1
Accept: pool-1-thread-1      ← SAME THREAD
Value: Hello
```

---

### thenAcceptAsync() - Asynchronous

**Uses a new thread** from the pool.
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Supply: " + Thread.currentThread().getName());
        return "Hello";
    }, executor)
    .thenAcceptAsync(value -> {
        System.out.println("Accept: " + Thread.currentThread().getName());
        System.out.println("Value: " + value);
    }, executor);
```

**Output:**
```
Supply: pool-1-thread-1
Accept: pool-1-thread-2      ← DIFFERENT THREAD
Value: Hello
```

---

## 💻 Code Examples

### Example 1: Basic thenAccept()
```java
import java.util.concurrent.*;

public class ThenAcceptExample {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10)
        );

        CompletableFuture<Void> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Thread: " + Thread.currentThread().getName());
                return "Task completed";
            }, executor)
            .thenAccept(value -> {
                System.out.println("Printing value: " + value);
            });

        // Wait for completion
        future.get();
        
        executor.shutdown();
    }
}
```

**Output:**
```
Thread: pool-1-thread-1
Printing value: Task completed
```

---

### Example 2: thenAcceptAsync()
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Supply Thread: " + Thread.currentThread().getName());
        return "Hello World";
    }, executor)
    .thenAcceptAsync(value -> {
        System.out.println("Accept Thread: " + Thread.currentThread().getName());
        System.out.println("Received: " + value);
    }, executor);

future.get();
```

**Output:**
```
Supply Thread: pool-1-thread-1
Accept Thread: pool-1-thread-1 or pool-1-thread-2 (could be different)
Received: Hello World
```

---

### Example 3: Complete Processing Pipeline
```java
CompletableFuture<Void> pipeline = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Step 1: Fetch data");
        return "Data";
    }, executor)
    .thenApply(data -> {
        System.out.println("Step 2: Process " + data);
        return data + " Processed";
    })
    .thenApply(processed -> {
        System.out.println("Step 3: Transform " + processed);
        return processed.toUpperCase();
    })
    .thenAccept(result -> {
        System.out.println("Step 4 (Final): Save to database - " + result);
        // Simulate saving to DB
    });

pipeline.get();
```

**Output:**
```
Step 1: Fetch data
Step 2: Process Data
Step 3: Transform Data Processed
Step 4 (Final): Save to database - DATA PROCESSED
```

**Key:** `thenAccept()` is the **final step** - no more chaining!

---

### Example 4: Real-World Use Case - Logging
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        // Fetch user data
        return new User("John", "john@example.com");
    })
    .thenApply(user -> {
        // Process user
        return user.getName() + " <" + user.getEmail() + ">";
    })
    .thenAccept(userInfo -> {
        // Log the information (terminal operation)
        System.out.println("User Info: " + userInfo);
        logger.info("User processed: " + userInfo);
    });

future.get();
```

---

### Example 5: Demonstrating Terminal Nature
```java
// This works fine
CompletableFuture<Void> future1 = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(s -> System.out.println(s));

// ❌ This doesn't make sense
CompletableFuture<Void> future2 = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(s -> System.out.println(s))
    .thenApply(voidValue -> {
        // voidValue is Void type - no meaningful value!
        // What can you do with Void?
        return "Something";  // Doesn't make logical sense
    });
```

**Why it doesn't work:**
- After `thenAccept()`, result is `Void`
- `Void` is a placeholder type with no value
- Cannot perform meaningful operations on `Void`

---

### Example 6: Multiple thenAccept() (Parallel Consumption)
```java
CompletableFuture<String> source = CompletableFuture.supplyAsync(() -> "Result");

// Multiple consumers can consume the same result
source.thenAccept(value -> {
    System.out.println("Consumer 1: " + value);
});

source.thenAccept(value -> {
    System.out.println("Consumer 2: " + value);
});

source.thenAccept(value -> {
    System.out.println("Consumer 3: " + value);
});

// Wait for source to complete
source.get();
```

**Output:**
```
Consumer 1: Result
Consumer 2: Result
Consumer 3: Result
```

**Use Case:** Multiple side effects from same result (logging, metrics, notifications)

---

### Example 7: Comparison - thenApply vs thenAccept
```java
// thenApply - returns value, can chain
CompletableFuture<String> cf1 = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")      // Returns value
    .thenApply(s -> s + "!")           // Can continue chaining
    .thenApply(s -> s.toUpperCase());  // Can continue chaining

String result1 = cf1.get();
System.out.println(result1); // HELLO WORLD!

// thenAccept - returns void, terminal
CompletableFuture<Void> cf2 = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(s -> s + "!")
    .thenAccept(s -> {
        System.out.println(s);         // Just consumes, doesn't return
    });
// Cannot chain meaningfully after this

cf2.get(); // Returns null (void)
```

---

### Example 8: Error Handling with thenAccept()
```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) {
            throw new RuntimeException("Random error");
        }
        return "Success";
    })
    .thenAccept(result -> {
        System.out.println("Result: " + result);
    })
    .exceptionally(ex -> {
        System.out.println("Error occurred: " + ex.getMessage());
        return null; // Must return Void (null for void)
    });

future.get();
```

**Output (if successful):**
```
Result: Success
```

**Output (if error):**
```
Error occurred: Random error
```

---

### Example 9: Side Effects Example
```java
// Common use case: Perform multiple side effects
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        // Fetch order details
        return new Order("ORD123", 299.99);
    })
    .thenAccept(order -> {
        // Side effect 1: Log
        System.out.println("Processing order: " + order.getId());
        
        // Side effect 2: Send email
        sendEmail(order);
        
        // Side effect 3: Update metrics
        updateMetrics(order);
        
        // No return value needed
    });

future.get();
```

---

### Example 10: Waiting for thenAccept()
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Starting task...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Task completed after 3 seconds";
    }, executor)
    .thenAccept(result -> {
        System.out.println("Received: " + result);
        System.out.println("Performing cleanup...");
    });

System.out.println("Main thread continues...");

// Wait for completion
future.get(); // Blocks until thenAccept completes

System.out.println("All done!");

executor.shutdown();
```

**Output:**
```
Main thread continues...
Starting task...
(waits 3 seconds...)
Received: Task completed after 3 seconds
Performing cleanup...
All done!
```

---

## 🎓 Key Takeaways

### ✅ Summary

1. **thenAccept() Purpose**
    - Consume a result without returning anything
    - Perform side effects (logging, saving, printing)
    - **Terminal operation** - end of chain

2. **Functional Interface**
```java
   Consumer<T> → void accept(T t)
```
- Accepts input
- Returns void

3. **Return Type**
```java
   CompletableFuture<Void>
```
- Cannot chain meaningfully after this
- `Void` type has no value

4. **Thread Behavior**
```java
   thenAccept()        // Same thread
   thenAcceptAsync()   // New thread
```

5. **Common Use Cases**
    - Logging results
    - Saving to database
    - Sending notifications
    - Updating metrics
    - Any side effect without return value

---

### 📊 Comparison Table

| Method | Input | Output | Returns | Use Case |
|--------|-------|--------|---------|----------|
| `thenApply()` | `T` | `U` | `CompletableFuture<U>` | Transform value |
| `thenCompose()` | `T` | `CompletableFuture<U>` | `CompletableFuture<U>` | Chain async ops |
| `thenAccept()` | `T` | `void` | `CompletableFuture<Void>` | Terminal operation |

---

### ⚠️ Important Notes

> **Terminal Operation**: `thenAccept()` is typically the **last stage** in the chain

> **No Return Value**: Cannot chain meaningful operations after `thenAccept()`

> **Side Effects**: Perfect for logging, saving, sending notifications

> **CompletableFuture<Void>**: The return type indicates "no meaningful value"

> **Multiple Consumers**: Can attach multiple `thenAccept()` to same source for parallel side effects

---

### 🔍 When to Use
```
Need to return transformed value?
    ↓
Use: thenApply()

Need to chain another async operation?
    ↓
Use: thenCompose()

Need to just consume/process result (no return)?
    ↓
Use: thenAccept() ✅

Need to combine two futures?
    ↓
Use: thenCombine() (next topic!)
```

---

## 📝 Quick Reference
```java
// Basic thenAccept - same thread
CompletableFuture.supplyAsync(() -> "Result")
    .thenAccept(value -> {
        System.out.println(value);
    });

// thenAcceptAsync - new thread
CompletableFuture.supplyAsync(() -> "Result", executor)
    .thenAcceptAsync(value -> {
        System.out.println(value);
    }, executor);

// Complete pipeline ending with thenAccept
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenApply(result -> transform(result))
    .thenAccept(finalResult -> {
        saveToDatabase(finalResult);
        logResult(finalResult);
    });
```

---

### 🎯 Progress Check

**Completed:**
1. ✅ supplyAsync()
2. ✅ thenApply() / thenApplyAsync()
3. ✅ thenCompose() / thenComposeAsync()
4. ✅ thenAccept() / thenAcceptAsync()

**Remaining:**
5. ⏭️ thenCombine() / thenCombineAsync()

---

*Next: [CompletableFuture - thenCombine() & thenCombineAsync() →](#)*