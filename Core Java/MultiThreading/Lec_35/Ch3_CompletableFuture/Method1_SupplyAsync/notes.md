# CompletableFuture - Basics & supplyAsync()

## 📌 Table of Contents
1. [Introduction to CompletableFuture](#introduction-to-completablefuture)
2. [Why CompletableFuture?](#why-completablefuture)
3. [CompletableFuture vs Future](#completablefuture-vs-future)
4. [supplyAsync() Method](#supplyasync-method)
5. [Thread Management](#thread-management)
6. [Code Examples](#code-examples)
7. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction to CompletableFuture

### What is CompletableFuture?

**CompletableFuture** is an **advanced version of Future** introduced in **Java 8**.
```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T>
```

### Key Points

- ✅ Implements `Future` interface
- ✅ Implements `CompletionStage` interface
- ✅ Provides additional capabilities beyond Future
- ✅ Enables **async programming** with **chaining**

### Hierarchy
```
 
   <<interface>> Future,  <<interface>> CompletionStage 
               \                        /
                \                      /
                 \                    /
                  \                  /
                   \                /
                CompletableFuture (class)
```

---

## 💡 Why CompletableFuture?

### The Big Brother Analogy

> **CompletableFuture is the "big brother" of Future**
```
Future
  ↓
Can do: Check status, get result, cancel
  
CompletableFuture
  ↓
Can do: Everything Future does + MORE
```

### Additional Capabilities

| Feature | Future | CompletableFuture |
|---------|--------|-------------------|
| Track async task | ✅ | ✅ |
| Get result | ✅ | ✅ |
| Cancel task | ✅ | ✅ |
| **Chaining operations** | ❌ | ✅ |
| **Combining multiple futures** | ❌ | ✅ |
| **Non-blocking callbacks** | ❌ | ✅ |
| **Exception handling in chain** | ❌ | ✅ |

---

## 🔄 CompletableFuture vs Future

### Comparison
```java
// Traditional Future - Blocking
Future<String> future = executor.submit(() -> "Result");
String result = future.get(); // Blocks!

// CompletableFuture - Non-blocking with chaining
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "Result");
cf.thenApply(s -> s.toUpperCase())
  .thenAccept(s -> System.out.println(s)); // No blocking!
```

### Key Differences

| Aspect | Future | CompletableFuture |
|--------|--------|-------------------|
| **Blocking** | `get()` blocks | Can be non-blocking |
| **Chaining** | Not supported | Full support |
| **Combining** | Manual | Built-in methods |
| **Callbacks** | Not supported | `thenApply`, `thenAccept`, etc. |
| **Java Version** | Java 5+ | Java 8+ |

---

## 🚀 supplyAsync() Method

### What is supplyAsync()?

**supplyAsync()** is used to **initiate an asynchronous operation**.
```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
```

### Purpose

Similar to `executor.submit()` in ThreadPoolExecutor:
- Creates a new thread (or uses from pool)
- Executes task asynchronously
- Returns `CompletableFuture` to track the task

---

### Method Signatures

#### Variant 1: Without Executor
```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier)
```

**Uses:** ForkJoinPool (default)

#### Variant 2: With Executor
```java
public static <U> CompletableFuture<U> supplyAsync(
    Supplier<U> supplier, 
    Executor executor
)
```

**Uses:** Your custom ThreadPoolExecutor

---

### Comparison with ThreadPoolExecutor
```java
// Old way - ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(/*...*/);
Future<String> future = executor.submit(() -> {
    return "Task completed";
});

// New way - CompletableFuture
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    return "Task completed";
});
```

**Both achieve the same goal:** Execute task asynchronously

---

## 🧵 Thread Management

### Default Thread Pool: ForkJoinPool

When you **don't provide an executor**, CompletableFuture uses **ForkJoinPool**.
```java
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    return "Task"; // Uses ForkJoinPool.commonPool()
});
```

### ForkJoinPool Characteristics

- 🔹 **Shared pool** - Used by all `supplyAsync()` without executor
- 🔹 **Dynamic sizing** - Adjusts based on CPU cores
- 🔹 **No control** - Cannot set min/max pool size
- 🔹 **Work-stealing** - Efficient for parallel tasks
```
ForkJoinPool.commonPool()
    ├── Thread-1
    ├── Thread-2
    ├── Thread-3
    └── ... (based on CPU cores)
```

---

### Custom Thread Pool: Pass Executor

For **more control**, provide your own `ThreadPoolExecutor`:
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                      // corePoolSize
    5,                      // maximumPoolSize
    1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    return "Task";
}, executor); // Uses YOUR executor
```

### Benefits of Custom Executor

- ✅ Control over pool size (min/max threads)
- ✅ Control over queue size
- ✅ Control over rejection policy
- ✅ Separate thread pool for specific tasks

---

### Thread Flow Diagram
```
Main Thread
    |
    |---- CompletableFuture.supplyAsync(supplier)
    |           |
    |           |--- No executor? → ForkJoinPool
    |           |--- With executor? → Custom pool
    |           |
    |           ↓
    |      New Thread picks task
    |           |
    |           |--- Executes supplier
    |           |--- Returns result
    |           |
    |           ↓
    |      CompletableFuture holds result
    |
    |---- Main continues (non-blocking)
```

---

## 💻 Code Examples

### Example 1: Basic supplyAsync()
```java
import java.util.concurrent.*;

public class CompletableFutureBasics {
    public static void main(String[] args) throws Exception {
        // Without custom executor - uses ForkJoinPool
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("Thread: " + Thread.currentThread().getName());
            return "Task completed";
        });
        
        // Get result (blocks until complete)
        String result = future.get();
        System.out.println("Result: " + result);
    }
}
```

**Output:**
```
Thread: ForkJoinPool.commonPool-worker-1
Result: Task completed
```

---

### Example 2: supplyAsync() with Custom Executor
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1,
    1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("Thread: " + Thread.currentThread().getName());
    
    try {
        Thread.sleep(5000); // Simulate work
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    
    return "Task completed";
}, executor); // Pass custom executor

// Get result
String result = future.get();
System.out.println("Result: " + result);

executor.shutdown();
```

**Output:**
```
Thread: pool-1-thread-1
(waits 5 seconds...)
Result: Task completed
```

---

### Example 3: Comparison - ThreadPoolExecutor vs CompletableFuture
```java
// Method 1: Traditional ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

Future<String> future1 = executor.submit(() -> {
    return "Result from ThreadPoolExecutor";
});
String result1 = future1.get();

// Method 2: CompletableFuture with same executor
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    return "Result from CompletableFuture";
}, executor);
String result2 = future2.get();

System.out.println(result1); // Result from ThreadPoolExecutor
System.out.println(result2); // Result from CompletableFuture

executor.shutdown();
```

**Both achieve the same result!**

---

### Example 4: Non-blocking with CompletableFuture
```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try {
        Thread.sleep(3000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Done";
});

System.out.println("Main thread continues...");
System.out.println("Not blocked!");

// Wait for result when needed
String result = future.get();
System.out.println("Result: " + result);
```

**Output:**
```
Main thread continues...
Not blocked!
(waits 3 seconds...)
Result: Done
```

---

### Example 5: Multiple supplyAsync() Tasks
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    3, 3, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

// Start multiple async tasks
CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 1: " + Thread.currentThread().getName());
    return 10;
}, executor);

CompletableFuture<Integer> task2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 2: " + Thread.currentThread().getName());
    return 20;
}, executor);

CompletableFuture<Integer> task3 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 3: " + Thread.currentThread().getName());
    return 30;
}, executor);

// Get all results
Integer result1 = task1.get();
Integer result2 = task2.get();
Integer result3 = task3.get();

System.out.println("Sum: " + (result1 + result2 + result3)); // 60

executor.shutdown();
```

**Output:**
```
Task 1: pool-1-thread-1
Task 2: pool-1-thread-2
Task 3: pool-1-thread-3
Sum: 60
```

---

## 🎯 What is Supplier?

### Supplier Interface
```java
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

### Characteristics

- 🔹 **Functional Interface** - Can use lambda
- 🔹 **No input parameters** - Doesn't accept anything
- 🔹 **Returns a value** - Generic type `T`
- 🔹 **Similar to Callable** - But simpler

### Example
```java
// Supplier that returns String
Supplier<String> supplier = () -> {
    // Do some work
    return "Result";
};

// Use with supplyAsync
CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);
```

---

## 🎓 Key Takeaways

### ✅ Summary

1. **CompletableFuture = Advanced Future**
    - Does everything Future can do
    - Plus chaining, combining, non-blocking callbacks

2. **supplyAsync() = Starting Point**
    - Initiates async operations
    - Similar to `executor.submit()`
    - Returns `CompletableFuture<T>`

3. **Two Variants**
```java
   // Variant 1: Uses ForkJoinPool
   supplyAsync(Supplier<T>)
   
   // Variant 2: Uses custom executor
   supplyAsync(Supplier<T>, Executor)
```

4. **Thread Management**
    - **Without executor** → ForkJoinPool (shared, dynamic)
    - **With executor** → Your pool (controlled)

5. **Supplier Interface**
    - No input parameters
    - Returns a value
    - Used by `supplyAsync()`

---

### 📊 Comparison Table

| Feature | ThreadPoolExecutor | CompletableFuture |
|---------|-------------------|-------------------|
| **Start async task** | `submit()` | `supplyAsync()` |
| **Returns** | `Future<T>` | `CompletableFuture<T>` |
| **Chaining** | ❌ | ✅ |
| **Custom executor** | Built-in | Pass as parameter |
| **Default pool** | Must create | ForkJoinPool |

---

### ⚠️ Important Notes

> **ForkJoinPool**: Used by default when no executor provided - you have no control over pool size

> **Custom Executor**: Recommended for production - gives you full control

> **Non-blocking**: CompletableFuture enables non-blocking async programming

> **Backward Compatible**: CompletableFuture implements Future - can use all Future methods

---

### 🔍 When to Use What?
```
Need simple async task execution?
    ↓
Use: supplyAsync()

Need control over threads?
    ↓
Use: supplyAsync(supplier, executor)

Need to chain operations?
    ↓
Use: supplyAsync().thenApply()... (Next topic!)

Need to combine multiple futures?
    ↓
Use: thenCombine() (Next topic!)
```

---

## 📝 Quick Reference
```java
// Basic supplyAsync (ForkJoinPool)
CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
    return "Result";
});

// supplyAsync with custom executor
ThreadPoolExecutor executor = new ThreadPoolExecutor(/*...*/);
CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
    return "Result";
}, executor);

// Get result (blocking)
String result = cf1.get();

// Check if done (non-blocking)
boolean done = cf1.isDone();
```

---

### 🎯 Five Important CompletableFuture Methods

In the lecture, the instructor mentions **5 important methods**:

1. ✅ **supplyAsync()** - Covered in this document
2. ⏭️ **thenApply() / thenApplyAsync()** - Next topic
3. ⏭️ **thenCompose() / thenComposeAsync()** - Next topic
4. ⏭️ **thenAccept() / thenAcceptAsync()** - Next topic
5. ⏭️ **thenCombine() / thenCombineAsync()** - Next topic

---

*Next: [CompletableFuture - Chaining Operations →](#)*