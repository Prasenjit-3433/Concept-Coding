# CompletableFuture - thenCombine() & thenCombineAsync()

## 📌 Table of Contents
1. [Introduction](#introduction)
2. [What is thenCombine()?](#what-is-thencombine)
3. [Method Signatures](#method-signatures)
4. [When to Use thenCombine()](#when-to-use-thencombine)
5. [Thread Execution Behavior](#thread-execution-behavior)
6. [Code Examples](#code-examples)
7. [Complete Summary - All 5 Methods](#complete-summary---all-5-methods)
8. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction

### The Problem

**Scenario:** You have **two independent async tasks** and want to **combine their results**.
```java
Task 1: Fetch product price → Returns Integer (10)
Task 2: Fetch product name → Returns String ("K")

Combine: price + name → Result: "10K"
```

**Both tasks run independently (parallel), then combine results!**

---

## 💡 What is thenCombine()?

### Definition

**thenCombine()** is used to **combine the results of two independent `CompletableFuture` tasks**.

### Purpose

- ✅ Combine results from two async tasks
- ✅ Both tasks run independently (parallel)
- ✅ Waits for both to complete
- ✅ Applies a combining function
- ✅ Returns a new `CompletableFuture` with combined result

---

### Visual Representation
```
                Main Thread
                    |
        ┌───────────┴───────────┐
        ↓                       ↓
   Task 1 Async            Task 2 Async
   (Thread-1)              (Thread-2)
        |                       |
   Returns 10              Returns "K"
        |                       |
        └───────────┬───────────┘
                    ↓
              thenCombine()
             (10, "K") → "10K"
                    ↓
        CompletableFuture<String>
```

---

### Key Characteristics

1. **Independent Execution**: Both tasks run in parallel
2. **Wait for Both**: `thenCombine` waits for both tasks to complete
3. **Combine Results**: Uses a `BiFunction` to combine results
4. **Returns New Future**: Returns `CompletableFuture` with combined result

---

## 📝 Method Signatures

### thenCombine()
```java
public <U, V> CompletableFuture<V> thenCombine(
    CompletionStage<? extends U> other,
    BiFunction<? super T, ? super U, ? extends V> fn
)
```

**Parameters:**
- `other` - The other `CompletableFuture` to combine with
- `BiFunction<T, U, V>` - Function that takes two inputs and returns combined result

**Returns:**
- `CompletableFuture<V>` - Future with combined result

---

### thenCombineAsync()
```java
// Variant 1: Uses ForkJoinPool
public <U, V> CompletableFuture<V> thenCombineAsync(
    CompletionStage<? extends U> other,
    BiFunction<? super T, ? super U, ? extends V> fn
)

// Variant 2: Uses custom executor
public <U, V> CompletableFuture<V> thenCombineAsync(
    CompletionStage<? extends U> other,
    BiFunction<? super T, ? super U, ? extends V> fn,
    Executor executor
)
```

---

### Understanding BiFunction
```java
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
```

**Characteristics:**
- Takes **two input parameters**: `T` and `U`
- Returns **one result**: `R`
- Used to combine two values into one

**Example:**
```java
BiFunction<Integer, String, String> combine = (num, str) -> num + str;
String result = combine.apply(10, "K"); // "10K"
```

---

## 🎯 When to Use thenCombine()

### Use Cases

1. **Independent API Calls**
```java
   Call API 1: Get user details
   Call API 2: Get user preferences
   Combine: Create complete user profile
```

2. **Parallel Data Processing**
```java
   Task 1: Calculate statistics
   Task 2: Generate report header
   Combine: Create final report
```

3. **Multiple Database Queries**
```java
   Query 1: Fetch orders
   Query 2: Fetch customer info
   Combine: Create invoice
```

---

### When NOT to Use

❌ **Don't use when tasks are dependent**
```java
// ❌ Wrong - Task 2 depends on Task 1 result
Task 1: Get user ID
Task 2: Get orders for that user ID

// ✅ Use thenCompose instead
getUserId().thenCompose(userId -> getOrders(userId))
```

**Rule:** If Task 2 needs Task 1's result to start, use `thenCompose()`, not `thenCombine()`

---

## 🧵 Thread Execution Behavior

### thenCombine() - Synchronous
```java
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 1: " + Thread.currentThread().getName());
    return 10;
}, executor);

CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 2: " + Thread.currentThread().getName());
    return "K";
}, executor);

CompletableFuture<String> combined = task1.thenCombine(task2, (num, str) -> {
    System.out.println("Combine: " + Thread.currentThread().getName());
    return num + str;
});
```

**Output:**
```
Task 1: pool-1-thread-1
Task 2: pool-1-thread-2
Combine: pool-1-thread-2    ← Uses thread of last completed task
```

**Key:** Combining function runs on the **thread that completed the last task**

---

### thenCombineAsync() - Asynchronous
```java
CompletableFuture<String> combined = task1.thenCombineAsync(task2, (num, str) -> {
    System.out.println("Combine: " + Thread.currentThread().getName());
    return num + str;
}, executor);
```

**Output:**
```
Task 1: pool-1-thread-1
Task 2: pool-1-thread-2
Combine: pool-1-thread-3    ← Uses NEW thread from executor
```

**Key:** Combining function runs on a **new thread** from the pool

---

## 💻 Code Examples

### Example 1: Basic thenCombine()
```java
import java.util.concurrent.*;

public class ThenCombineExample {
    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            3, 3, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10)
        );

        // Task 1: Returns Integer
        CompletableFuture<Integer> asyncTask1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Task 1 Thread: " + Thread.currentThread().getName());
            return 10;
        }, executor);

        // Task 2: Returns String
        CompletableFuture<String> asyncTask2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("Task 2 Thread: " + Thread.currentThread().getName());
            return "K";
        }, executor);

        // Combine both tasks
        CompletableFuture<String> combined = asyncTask1.thenCombine(
            asyncTask2,
            (num, str) -> {
                System.out.println("Combine Thread: " + Thread.currentThread().getName());
                return num + str;
            }
        );

        String result = combined.get();
        System.out.println("Final Result: " + result);

        executor.shutdown();
    }
}
```

**Output:**
```
Task 1 Thread: pool-1-thread-1
Task 2 Thread: pool-1-thread-2
Combine Thread: pool-1-thread-1 or pool-1-thread-2
Final Result: 10K
```

---

### Example 2: thenCombineAsync()
```java
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 1: " + Thread.currentThread().getName());
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return 100;
}, executor);

CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 2: " + Thread.currentThread().getName());
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return 200;
}, executor);

CompletableFuture<Integer> combined = task1.thenCombineAsync(
    task2,
    (result1, result2) -> {
        System.out.println("Combining on: " + Thread.currentThread().getName());
        return result1 + result2;
    },
    executor
);

Integer result = combined.get();
System.out.println("Sum: " + result);
```

**Output:**
```
Task 1: pool-1-thread-1
Task 2: pool-1-thread-2
(waits 2 seconds for both to complete...)
Combining on: pool-1-thread-3
Sum: 300
```

---

### Example 3: Real-World Use Case - API Calls
```java
// Simulate API calls
static CompletableFuture<Double> getProductPrice() {
    return CompletableFuture.supplyAsync(() -> {
        System.out.println("Fetching price...");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 299.99;
    });
}

static CompletableFuture<String> getProductName() {
    return CompletableFuture.supplyAsync(() -> {
        System.out.println("Fetching name...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Laptop";
    });
}

public static void main(String[] args) throws Exception {
    // Both API calls happen in parallel
    CompletableFuture<String> productInfo = getProductPrice()
        .thenCombine(getProductName(), (price, name) -> {
            return name + " - $" + price;
        });

    String result = productInfo.get();
    System.out.println("Product Info: " + result);
}
```

**Output:**
```
Fetching price...
Fetching name...
(both run in parallel, waits 1.5 seconds...)
Product Info: Laptop - $299.99
```

---

### Example 4: Multiple Data Sources
```java
CompletableFuture<List<String>> getUserOrders = CompletableFuture.supplyAsync(() -> {
    System.out.println("Fetching user orders...");
    return Arrays.asList("Order1", "Order2", "Order3");
});

CompletableFuture<String> getUserName = CompletableFuture.supplyAsync(() -> {
    System.out.println("Fetching user name...");
    return "John Doe";
});

CompletableFuture<String> userReport = getUserOrders.thenCombine(
    getUserName,
    (orders, name) -> {
        return "User: " + name + ", Orders: " + orders.size();
    }
);

String report = userReport.get();
System.out.println(report);
```

**Output:**
```
Fetching user orders...
Fetching user name...
User: John Doe, Orders: 3
```

---

### Example 5: Different Return Types
```java
// Task 1: Returns Integer
CompletableFuture<Integer> intTask = CompletableFuture.supplyAsync(() -> 42);

// Task 2: Returns String
CompletableFuture<String> strTask = CompletableFuture.supplyAsync(() -> "Answer");

// Combine into custom object
CompletableFuture<Result> combined = intTask.thenCombine(
    strTask,
    (num, str) -> new Result(str, num)
);

Result result = combined.get();
System.out.println(result); // Result{message='Answer', value=42}

// Result class
class Result {
    String message;
    int value;
    
    Result(String message, int value) {
        this.message = message;
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "Result{message='" + message + "', value=" + value + "}";
    }
}
```

---

### Example 6: Chaining After thenCombine()
```java
CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> 10)
    .thenCombine(
        CompletableFuture.supplyAsync(() -> 20),
        (a, b) -> a + b  // Returns 30
    )
    .thenApply(sum -> {
        return "Sum is: " + sum;  // Transform to String
    })
    .thenApply(str -> {
        return str.toUpperCase();  // Transform to uppercase
    });

System.out.println(result.get()); // SUM IS: 30
```

**Key:** You **CAN chain** after `thenCombine()` because it returns a value, unlike `thenAccept()`

---

### Example 7: Three Task Combination
```java
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> 20);
CompletableFuture<Integer> task3 = CompletableFuture.supplyAsync(() -> 30);

// Combine task1 and task2
CompletableFuture<Integer> combined12 = task1.thenCombine(task2, (a, b) -> a + b);

// Combine result with task3
CompletableFuture<Integer> finalResult = combined12.thenCombine(task3, (ab, c) -> ab + c);

Integer result = finalResult.get();
System.out.println("Sum of all three: " + result); // 60
```

---

### Example 8: Error Handling with thenCombine()
```java
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
    if (Math.random() > 0.5) {
        throw new RuntimeException("Task 1 failed");
    }
    return 10;
});

CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> combined = task1.thenCombine(
    task2,
    (a, b) -> a + b
).exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return 0; // Default value
});

Integer result = combined.get();
System.out.println("Result: " + result);
```

**Output (if error):**
```
Error: java.lang.RuntimeException: Task 1 failed
Result: 0
```

**Output (if success):**
```
Result: 30
```

---

### Example 9: Timing Comparison - Sequential vs Parallel
```java
// Sequential execution (slow)
long start1 = System.currentTimeMillis();

CompletableFuture<Integer> sequential = CompletableFuture
    .supplyAsync(() -> {
        sleep(2000); // 2 seconds
        return 10;
    })
    .thenApply(a -> {
        sleep(2000); // 2 seconds
        return a + 20;
    });

sequential.get();
long end1 = System.currentTimeMillis();
System.out.println("Sequential time: " + (end1 - start1) + "ms"); // ~4000ms

// Parallel execution with thenCombine (fast)
long start2 = System.currentTimeMillis();

CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
    sleep(2000); // 2 seconds
    return 10;
});

CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> {
    sleep(2000); // 2 seconds
    return 20;
});

CompletableFuture<Integer> parallel = task1.thenCombine(task2, (a, b) -> a + b);

parallel.get();
long end2 = System.currentTimeMillis();
System.out.println("Parallel time: " + (end2 - start2) + "ms"); // ~2000ms

static void sleep(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

**Output:**
```
Sequential time: 4000ms
Parallel time: 2000ms
```

**Advantage:** Tasks run in **parallel** - much faster!

---

### Example 10: Complete Real-World Scenario
```java
// E-commerce checkout scenario
public static void main(String[] args) throws Exception {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        5, 5, 1, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(10)
    );

    // Task 1: Validate payment
    CompletableFuture<Boolean> paymentValidation = CompletableFuture.supplyAsync(() -> {
        System.out.println("Validating payment...");
        sleep(1000);
        return true; // Payment valid
    }, executor);

    // Task 2: Check inventory
    CompletableFuture<Boolean> inventoryCheck = CompletableFuture.supplyAsync(() -> {
        System.out.println("Checking inventory...");
        sleep(1500);
        return true; // In stock
    }, executor);

    // Task 3: Calculate shipping
    CompletableFuture<Double> shippingCost = CompletableFuture.supplyAsync(() -> {
        System.out.println("Calculating shipping...");
        sleep(800);
        return 15.99;
    }, executor);

    // Combine payment and inventory
    CompletableFuture<Boolean> validationResult = paymentValidation.thenCombine(
        inventoryCheck,
        (payment, inventory) -> payment && inventory
    );

    // Combine validation with shipping
    CompletableFuture<String> checkoutResult = validationResult.thenCombine(
        shippingCost,
        (valid, shipping) -> {
            if (valid) {
                return "Checkout successful! Shipping: $" + shipping;
            } else {
                return "Checkout failed!";
            }
        }
    );

    String result = checkoutResult.get();
    System.out.println(result);

    executor.shutdown();
}
```

**Output:**
```
Validating payment...
Checking inventory...
Calculating shipping...
(all run in parallel, waits ~1.5 seconds)
Checkout successful! Shipping: $15.99
```

---

## 📊 Complete Summary - All 5 Methods

### Quick Comparison Table

| Method | Input | Output | Use Case | Returns |
|--------|-------|--------|----------|---------|
| `supplyAsync()` | None (Supplier) | `T` | Start async task | `CompletableFuture<T>` |
| `thenApply()` | `T` | `U` | Transform value | `CompletableFuture<U>` |
| `thenCompose()` | `T` | `CompletableFuture<U>` | Chain async ops | `CompletableFuture<U>` |
| `thenAccept()` | `T` | `void` | Terminal operation | `CompletableFuture<Void>` |
| `thenCombine()` | `T`, `U` | `V` | Combine two futures | `CompletableFuture<V>` |

---

### Visual Flow Chart
```
supplyAsync()
    ↓
CompletableFuture<T>
    ↓
    ├─→ thenApply(T → U)
    │       ↓
    │   CompletableFuture<U>
    │
    ├─→ thenCompose(T → CF<U>)
    │       ↓
    │   CompletableFuture<U>
    │
    ├─→ thenAccept(T → void)
    │       ↓
    │   CompletableFuture<Void> [END]
    │
    └─→ thenCombine(T, U → V)
            ↓
        CompletableFuture<V>
```

---

### Complete Example Using All 5 Methods
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 5, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

// 1. supplyAsync - Start async task
CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
    return "Hello";
}, executor);

// 2. thenApply - Transform value
CompletableFuture<String> task2 = task1.thenApply(s -> s + " World");

// 3. thenCompose - Chain another async operation
CompletableFuture<String> task3 = task2.thenCompose(s -> 
    CompletableFuture.supplyAsync(() -> s + "!", executor)
);

// 4. thenCombine - Combine with another independent task
CompletableFuture<String> anotherTask = CompletableFuture.supplyAsync(() -> {
    return " [2024]";
}, executor);

CompletableFuture<String> task4 = task3.thenCombine(anotherTask, (s1, s2) -> s1 + s2);

// 5. thenAccept - Terminal operation
task4.thenAccept(result -> {
    System.out.println("Final Result: " + result);
});

executor.shutdown();
```

**Output:**
```
Final Result: Hello World! [2024]
```

---

## 🎓 Key Takeaways

### ✅ Summary

1. **thenCombine() Purpose**
    - Combine results from **two independent async tasks**
    - Both tasks run in **parallel**
    - Waits for **both to complete**
    - Uses **BiFunction** to combine results

2. **BiFunction Interface**
```java
   BiFunction<T, U, R> → R apply(T t, U u)
```
- Takes two inputs
- Returns one combined result

3. **Thread Behavior**
```java
   thenCombine()        // Uses thread of last completed task
   thenCombineAsync()   // Uses new thread
```

4. **When to Use**
    - ✅ Independent parallel tasks
    - ✅ Multiple API calls that don't depend on each other
    - ✅ Parallel data processing
    - ❌ Don't use for dependent tasks (use `thenCompose()`)

5. **Performance Benefit**
    - Tasks run in **parallel** → Faster execution
    - Example: 2 tasks of 2 seconds each → Total ~2 seconds (not 4!)

---

### 📊 Method Selection Guide
```
Starting a new async task?
    ↓
Use: supplyAsync()

Transform result to another value?
    ↓
Use: thenApply()

Chain another async operation (dependent)?
    ↓
Use: thenCompose()

Just consume result (no return)?
    ↓
Use: thenAccept()

Combine two independent async tasks?
    ↓
Use: thenCombine() ✅
```

---

### ⚠️ Important Notes

> **Independence**: Use `thenCombine()` only when tasks are **independent**

> **Parallel Execution**: Both tasks run in **parallel** for better performance

> **Wait for Both**: Combining happens only after **both tasks complete**

> **Chainable**: Unlike `thenAccept()`, you **CAN chain** after `thenCombine()`

> **BiFunction**: Combining function takes **two parameters** and returns **one result**

---

### 🔍 Common Patterns

#### Pattern 1: Multiple Independent API Calls
```java
getUser().thenCombine(getSettings(), (user, settings) -> 
    new Profile(user, settings)
)
```

#### Pattern 2: Parallel Data Processing
```java
processDatasetA().thenCombine(processDatasetB(), (resultA, resultB) -> 
    merge(resultA, resultB)
)
```

#### Pattern 3: Multiple Database Queries
```java
queryOrders().thenCombine(queryCustomer(), (orders, customer) -> 
    createInvoice(orders, customer)
)
```

---

## 📝 Quick Reference
```java
// Basic thenCombine
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> "K");

CompletableFuture<String> combined = task1.thenCombine(
    task2,
    (num, str) -> num + str
);

// thenCombineAsync with executor
CompletableFuture<String> combined = task1.thenCombineAsync(
    task2,
    (num, str) -> num + str,
    executor
);

// Real-world pattern
CompletableFuture<Report> report = 
    fetchData1().thenCombine(
        fetchData2(),
        (data1, data2) -> generateReport(data1, data2)
    );
```

---

### 🎯 Final Progress Check

**Completed - All 5 Methods:**
1. ✅ supplyAsync() - Start async task
2. ✅ thenApply() / thenApplyAsync() - Transform value
3. ✅ thenCompose() / thenComposeAsync() - Chain async ops
4. ✅ thenAccept() / thenAcceptAsync() - Terminal operation
5. ✅ thenCombine() / thenCombineAsync() - Combine two futures

---

## 🎉 Conclusion

### What We Learned

**CompletableFuture provides 5 essential methods for async programming:**

1. **supplyAsync()** - Start the async journey
2. **thenApply()** - Transform along the way
3. **thenCompose()** - Chain dependent operations
4. **thenAccept()** - Consume at the end
5. **thenCombine()** - Merge parallel paths

### Key Principles

- 🔹 Use `Async` variants when you need **different threads**
- 🔹 Chain operations for **pipeline-style** programming
- 🔹 Combine independent tasks for **parallel processing**
- 🔹 Choose the right method based on **dependencies**

### Production Usage Note

> **From the lecture:** In production code, `supplyAsync()` + `get()` is most common. Chaining methods are important for **interviews** but less frequently used in production.

---

## 📚 Practice Recommendations

1. ✅ Practice each method individually
2. ✅ Create your own use cases
3. ✅ Understand thread behavior (sync vs async)
4. ✅ Practice combining multiple methods
5. ✅ Make notes and diagrams

**As the instructor says:**
> "Please please please do practice it. Make your own notes. Then only you would get clear what's happening."

---

*Congratulations! You've completed all 5 CompletableFuture methods! 🎊*