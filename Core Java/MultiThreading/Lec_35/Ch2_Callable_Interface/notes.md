# Callable Interface in Java

## 📌 Table of Contents
1. [Introduction](#introduction)
2. [Runnable vs Callable](#runnable-vs-callable)
3. [Three Flavors of submit()](#three-flavors-of-submit)
4. [Understanding Future Generics](#understanding-future-generics)
5. [Use Case 1: submit(Runnable)](#use-case-1-submitrunnable)
6. [Use Case 2: submit(Runnable, T)](#use-case-2-submitrunnable-t)
7. [Use Case 3: submit(Callable)](#use-case-3-submitcallable)
8. [Code Examples](#code-examples)
9. [Key Takeaways](#key-takeaways)

---

## 🎯 Introduction

### The Question from Previous Topic

In the Future topic, we saw:
```java
Future<?> future = executor.submit(() -> {
    System.out.println("Task");
});
```

**Questions:**
- ❓ Why is it `Future<?>` and not `Future<String>` or `Future<Integer>`?
- ❓ Can tasks return values?
- ❓ How do we get results from async tasks?

**Answer:** This is where **Callable** comes in!

---

## 🔄 Runnable vs Callable

### Key Difference

| Aspect | Runnable | Callable |
|--------|----------|----------|
| **Return Type** | `void` (no return) | Returns a value (generic `V`) |
| **Method** | `void run()` | `V call() throws Exception` |
| **Exception Handling** | Cannot throw checked exceptions | Can throw checked exceptions |
| **Purpose** | Tasks without return values | Tasks that compute and return results |

### Visual Comparison
```java
// Runnable - No return value
Runnable task = () -> {
    System.out.println("Do something");
    // No return statement
};

// Callable - Returns a value
Callable<String> task = () -> {
    System.out.println("Do something");
    return "Result"; // Returns value
};
```

---

## 🎨 Three Flavors of submit()

ThreadPoolExecutor provides **three overloaded versions** of `submit()`:

### Overview
```java
// Flavor 1: Runnable only
Future<?> submit(Runnable task)

// Flavor 2: Runnable + Result object
<T> Future<T> submit(Runnable task, T result)

// Flavor 3: Callable
<T> Future<T> submit(Callable<T> task)
```

### Which Flavor is Used When?
```java
// This uses Flavor 1 (Runnable)
executor.submit(() -> {
    System.out.println("Task");
});

// This uses Flavor 3 (Callable)
executor.submit(() -> {
    System.out.println("Task");
    return 45; // Returns Integer
});
```

**Decision Logic:**
- 🔹 If task has **no return statement** → Uses `submit(Runnable)`
- 🔹 If task has **return statement** → Uses `submit(Callable<T>)`

---

## 🔍 Understanding Future Generics

### Why `Future<?>`?
```java
Future<?> future = executor.submit(() -> {
    // No return value
});
```

**Explanation:**
- `?` is a **wildcard** - means "I don't know what type"
- Runnable returns `void` (nothing)
- Internally, result is set to `null`
- Since it's unknown/null, we use wildcard `?`

### How to Catch the Result?
```java
Object result = future.get(); // Must catch as Object

if (result == null) {
    System.out.println("No result - was Runnable");
}
```

**Why Object?**
- `Object` is the parent of all classes
- `?` (wildcard) can be anything → only `Object` can hold it
- For Runnable tasks, `result` will always be `null`

---

## 📋 Use Case 1: submit(Runnable)

### Signature
```java
Future<?> submit(Runnable task)
```

### Characteristics
- ✅ Task doesn't return any value
- ✅ Returns `Future<?>` (wildcard)
- ✅ Calling `future.get()` returns `null`

### Example
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

// Submit Runnable task
Future<?> future = executor.submit(() -> {
    System.out.println("Task 1 with Runnable - No return");
});

// Get result
Object result = future.get(); // Returns null

System.out.println(result == null); // true
```

### Internal Behavior
```
FutureTask {
    Runnable task: () -> { print("Task") }
    State: RUNNING → COMPLETED
    Result: null (hardcoded for Runnable)
}
```

**Key Point:**
> For Runnable tasks, the result is **always hardcoded to null** internally

---

## 📋 Use Case 2: submit(Runnable, T)

### Signature
```java
<T> Future<T> submit(Runnable task, T result)
```

### Characteristics
- ✅ Task still doesn't return a value (it's Runnable)
- ✅ You provide a **result object** yourself
- ✅ That same object is returned when task completes
- ✅ Useful for **shared/common objects**

### The Problem It Solves

**Scenario:** You want a Runnable task to produce output, but Runnable can't return values.

**Solution:** Use a shared object!

---

### Example: Using Common Object
```java
// Step 1: Create a common object
List<Integer> output = new ArrayList<>(); // Empty list

// Step 2: Create custom Runnable that uses this object
class MyRunnable implements Runnable {
    private List<Integer> list;
    
    public MyRunnable(List<Integer> list) {
        this.list = list; // Same reference as 'output'
    }
    
    @Override
    public void run() {
        // Update the shared list
        list.add(300);
    }
}

// Step 3: Submit with the result object
Future<List<Integer>> future = executor.submit(
    new MyRunnable(output),  // Pass the list to Runnable
    output                    // Provide as result object
);

// Step 4: Get result
List<Integer> result = future.get(); // Waits for task to complete

// Both are same!
System.out.println(output.get(0));  // 300
System.out.println(result.get(0));  // 300
```

### How It Works
```
Time 0:
output = []  (empty list)
       ↓
    passed to MyRunnable
       ↓
MyRunnable.list = [] (same reference)

During execution:
MyRunnable adds 300 to list
       ↓
output = [300]

After task completes:
future.get() returns → output (the same object)
result = [300]
```

### Visual Flow
```
Main Thread                          Thread-1
    |                                    |
    | output = []                        |
    |                                    |
    | submit(runnable, output) --------->| 
    |                                    | runnable.run()
    |                                    | list.add(300)
    |                                    | output = [300]
    |                                    |
    | future.get() <---------------------| returns output
    |                                    |
    | result = [300]                     |
    | output = [300]  (same reference!)  |
```

### Two Ways to Access Result
```java
// Method 1: Access original object directly
System.out.println(output.get(0)); // 300

// Method 2: Access via future.get()
List<Integer> result = future.get();
System.out.println(result.get(0)); // 300
```

**Both work because they reference the same object!**

---

### When to Use This?

**Use Case:** When you need to:
1. Use Runnable (not Callable)
2. Get some output/result
3. Work with a shared object

**Common Scenario:**
- Updating a shared collection
- Modifying a shared state object
- Working with mutable objects

---

## 📋 Use Case 3: submit(Callable)

### Signature
```java
<T> Future<T> submit(Callable<T> task)
```

### Characteristics
- ✅ Task **returns a value** directly
- ✅ Most **clean and straightforward** approach
- ✅ No need for workarounds with shared objects
- ✅ Type-safe with generics

### What is Callable?
```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

**Key Points:**
- Functional interface (can use lambda)
- Generic return type `V`
- Can throw checked exceptions

---

### Example: Basic Callable
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

// Submit Callable task
Future<List<Integer>> future = executor.submit(() -> {
    // Create and populate list
    List<Integer> output = new ArrayList<>();
    output.add(300);
    
    // Return directly
    return output;
});

// Get result
try {
    List<Integer> result = future.get();
    System.out.println(result.get(0)); // 300
} catch (Exception e) {
    e.printStackTrace();
}
```

### Why This is Better
```java
// ❌ Use Case 2 - Workaround with shared object
List<Integer> output = new ArrayList<>();
Future<List<Integer>> future = executor.submit(
    new MyRunnable(output), 
    output
);

// ✅ Use Case 3 - Clean with Callable
Future<List<Integer>> future = executor.submit(() -> {
    List<Integer> output = new ArrayList<>();
    output.add(300);
    return output;
});
```

**Advantages:**
- 🎯 More readable
- 🎯 No need for shared objects
- 🎯 No need for custom Runnable classes
- 🎯 Type-safe return values

---

## 💻 Code Examples

### Example 1: All Three Flavors Together
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    1, 1, 1, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(10)
);

// Flavor 1: Runnable (no return)
Future<?> future1 = executor.submit(() -> {
    System.out.println("Task 1: Runnable");
});
Object result1 = future1.get(); // null

// Flavor 2: Runnable + Result object
List<Integer> sharedList = new ArrayList<>();
Future<List<Integer>> future2 = executor.submit(() -> {
    sharedList.add(100);
}, sharedList);
List<Integer> result2 = future2.get(); // [100]

// Flavor 3: Callable (returns value)
Future<String> future3 = executor.submit(() -> {
    return "Hello from Callable";
});
String result3 = future3.get(); // "Hello from Callable"

executor.shutdown();
```

---

### Example 2: Callable with Different Return Types
```java
// Returns Integer
Future<Integer> futureInt = executor.submit(() -> {
    return 42;
});
Integer num = futureInt.get(); // 42

// Returns String
Future<String> futureStr = executor.submit(() -> {
    return "Result";
});
String str = futureStr.get(); // "Result"

// Returns Custom Object
Future<User> futureUser = executor.submit(() -> {
    return new User("John", 30);
});
User user = futureUser.get(); // User object
```

---

### Example 3: Callable with Exception Handling
```java
Future<Integer> future = executor.submit(() -> {
    if (someCondition) {
        throw new Exception("Something went wrong");
    }
    return 100;
});

try {
    Integer result = future.get();
    System.out.println("Result: " + result);
} catch (ExecutionException e) {
    System.out.println("Task threw exception: " + e.getCause());
} catch (InterruptedException e) {
    System.out.println("Thread interrupted");
}
```

**Key Point:**
> Callable can throw checked exceptions - they're wrapped in `ExecutionException`

---

## 🎓 Key Takeaways

### ✅ Summary

1. **Two Interfaces for Tasks**
```java
   Runnable → void run()           // No return value
   Callable → V call()             // Returns value
```

2. **Three Submit Flavors**
```java
   submit(Runnable)              → Future<?>
   submit(Runnable, T result)    → Future<T>
   submit(Callable<T>)           → Future<T>
```

3. **When to Use Which?**
    - No return value needed → `Runnable` (Flavor 1)
    - Need shared object pattern → `Runnable + Result` (Flavor 2)
    - Need clean return value → `Callable` (Flavor 3) ✅ **Recommended**

4. **Future Generic Types**
```java
   Future<?>           // Wildcard - unknown type (Runnable)
   Future<String>      // Specific type (Callable<String>)
   Future<Integer>     // Specific type (Callable<Integer>)
```

5. **Best Practice**
    - Prefer `Callable` over `Runnable` when you need return values
    - Use `Runnable` only for fire-and-forget tasks
    - Flavor 2 is mostly a workaround - Flavor 3 is cleaner

---

### 📊 Decision Table

| Need Return Value? | Can Throw Exceptions? | Use |
|-------------------|----------------------|-----|
| ❌ No | ❌ No | `Runnable` |
| ✅ Yes | ❌ No | `Callable` |
| ✅ Yes | ✅ Yes | `Callable` ✅ |
| ❌ No | ✅ Yes | `Callable` |

---

### ⚠️ Important Notes

> **Wildcard `<?>`**: Means "I don't know the type" - can only be caught as `Object`

> **Runnable Result**: Always `null` internally for `submit(Runnable)`

> **Shared Object Pattern**: Flavor 2 is useful but Flavor 3 (Callable) is cleaner

> **Exception Handling**: Callable can throw checked exceptions, Runnable cannot

---

### 🔗 Comparison Chart
```
submit(Runnable)
    ↓
Future<?>
    ↓
future.get() → null

submit(Runnable, T)
    ↓
Future<T>
    ↓
future.get() → T (the object you passed)

submit(Callable<T>)
    ↓
Future<T>
    ↓
future.get() → T (returned by call())
```

---

## 📝 Quick Reference
```java
// Flavor 1: Simple task, no return
Future<?> f1 = executor.submit(() -> {
    System.out.println("Task");
});

// Flavor 2: Runnable with shared object
List<Integer> list = new ArrayList<>();
Future<List<Integer>> f2 = executor.submit(() -> {
    list.add(100);
}, list);

// Flavor 3: Callable with return (RECOMMENDED)
Future<String> f3 = executor.submit(() -> {
    return "Result";
});
```

---

*Next: [CompletableFuture - Basics & supplyAsync() →](#)*