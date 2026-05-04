# Java Executors Utility Class - Thread Pool Types

## 📚 Overview

The `Executors` class is a **utility class** in the `java.util.concurrent` package that provides **factory methods** to create different types of thread pool executors. Instead of manually creating custom thread pools, we can use these pre-built executors for common use cases.

---

## Custom vs Pre-built Thread Pools

### Custom Thread Pool Executor
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>()
);
```
- You manually configure all parameters (core pool size, max pool size, keep-alive time, queue)
- Gives full control over thread pool behavior
- More verbose and requires understanding of all parameters
- Use when you have specific requirements

### Pre-built Executors
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
```
- Factory methods provide ready-made thread pools
- Less configuration required
- Easier to use but less flexible
- Use when your requirements match standard patterns

---

## 1. Fixed Thread Pool Executor

### Overview
Creates a thread pool with a **fixed number of threads**. The minimum and maximum pool sizes are the same.

### Creation
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
executor.submit(runnableTask);
```

### Configuration

| Property | Value |
|----------|-------|
| **Min Pool Size** | Same as the number you provide |
| **Max Pool Size** | Same as the number you provide |
| **Queue Size** | Unbounded (uses LinkedBlockingQueue) |
| **Thread Alive When Idle** | Yes - threads stay alive even when idle |

### Characteristics

- **Fixed Size**: Both min and max pool size are the same (e.g., 5 threads)
- **Unbounded Queue**: Can grow indefinitely (until memory issues)
- **Persistent Threads**: Threads remain alive even when there are no tasks
- **No Dynamic Scaling**: Cannot create more threads beyond the fixed limit

### When to Use ✅

- When you have **exact information** about how many async tasks are needed
- When you want to **limit resource usage** to a specific number of threads
- When workload is **predictable and stable**

### Disadvantages ❌

- **Not good for heavy workloads**: Leads to limited concurrency
- **Cannot scale up**: Even if system has capacity, cannot create more threads
- **Queue can grow large**: With unbounded queue, tasks pile up during high load

### Example Scenario
```
You know your application needs exactly 5 concurrent operations at any time.
Setting: newFixedThreadPool(5)
Result: Always 5 threads, no more, no less
```

---

## 2. Cached Thread Pool Executor

### Overview
Creates a thread pool that **dynamically creates new threads as needed**. Threads are created on-demand and removed when idle.

### Creation
```java
ExecutorService executor = Executors.newCachedThreadPool();
executor.submit(runnableTask);
```

### Configuration

| Property | Value |
|----------|-------|
| **Min Pool Size** | 0 (starts with zero threads) |
| **Max Pool Size** | Integer.MAX_VALUE (virtually unlimited) |
| **Queue Size** | 0 (no queue, or synchronous queue) |
| **Thread Alive When Idle** | 60 seconds |

### Characteristics

- **Dynamic Creation**: Creates threads as requests come in
- **No Queue**: Queue size is effectively zero - tasks are immediately assigned to threads
- **Short-lived Threads**: Idle threads are terminated after 60 seconds
- **Unlimited Threads**: Can create as many threads as needed (up to Integer.MAX_VALUE)

### How It Works
```
Request 1 arrives → Create Thread 1 → Assign work
Request 2 arrives → Create Thread 2 → Assign work
...
Request 100 arrives → Create Thread 100 → Assign work

After 60 seconds of idleness:
Thread 1 (idle for 60s) → Terminated
Thread 2 (idle for 60s) → Terminated
```

### When to Use ✅

- **Short-lived tasks**: Tasks that complete quickly
- **Burst of tasks**: Handling sudden spikes in workload
- **Unpredictable workload**: When you don't know how many concurrent tasks you'll have

### Disadvantages ❌

- **Memory issues**: Too many long-running tasks can create excessive threads
- **Resource exhaustion**: Rapid task submission can overwhelm the system
- **Not suitable for long-running tasks**: Threads won't terminate, consuming memory

### Example Scenario
```
Handling HTTP requests that complete in milliseconds:
- 100 requests arrive suddenly → 100 threads created
- Requests complete quickly → Threads become idle
- After 60 seconds → Idle threads are cleaned up
```

---

## 3. Single Thread Executor

### Overview
Creates an executor with **just one worker thread**. All tasks are executed sequentially by this single thread.

### Creation
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(runnableTask);
```

### Configuration

| Property | Value |
|----------|-------|
| **Min Pool Size** | 1 |
| **Max Pool Size** | 1 |
| **Queue Size** | Unbounded (LinkedBlockingQueue) |
| **Thread Alive When Idle** | Yes - thread stays alive even when queue is empty |

### Characteristics

- **Single Thread**: Only one thread processes all tasks
- **Sequential Execution**: Tasks are executed one after another
- **Unbounded Queue**: Can hold unlimited tasks (until memory issues)
- **Persistent Thread**: The single thread remains alive even when idle
- **No Concurrency**: Tasks never run in parallel

### How It Works
```
Thread Pool: [Thread-1]
Queue: [Task-1] [Task-2] [Task-3] [Task-4] ...

Execution:
Thread-1 executes Task-1 (completed)
Thread-1 executes Task-2 (completed)
Thread-1 executes Task-3 (completed)
Thread-1 executes Task-4 (completed)
...
```

### When to Use ✅

- **Sequential processing required**: When tasks must be executed in order
- **Shared resource access**: When tasks access a shared resource that cannot handle concurrent access
- **Simple background processing**: Single background worker for non-critical tasks

### Disadvantages ❌

- **No concurrency at all**: Only one task runs at a time
- **Slow processing**: All tasks wait in queue for their turn
- **Bottleneck**: Can become a performance bottleneck with many tasks

### Example Scenario
```
Logging system where logs must be written in order:
- Task 1: Write log entry A
- Task 2: Write log entry B  
- Task 3: Write log entry C

All executed sequentially by the single thread to maintain order.
```

---

## Comparison Table

| Feature | Fixed Thread Pool | Cached Thread Pool | Single Thread Executor |
|---------|------------------|-------------------|----------------------|
| **Min Threads** | Fixed number (e.g., 5) | 0 | 1 |
| **Max Threads** | Fixed number (e.g., 5) | Integer.MAX_VALUE | 1 |
| **Queue** | Unbounded | No queue (size 0) | Unbounded |
| **Thread Lifetime** | Always alive | 60s when idle | Always alive |
| **Best For** | Predictable workload | Short-lived burst tasks | Sequential processing |
| **Concurrency** | Limited (fixed) | High (dynamic) | None |

---

## Key Takeaways

1. **Fixed Thread Pool**: Use when you know exactly how many threads you need
2. **Cached Thread Pool**: Use for short-lived tasks with unpredictable bursts
3. **Single Thread Executor**: Use when tasks must execute sequentially

Choose based on your workload characteristics and concurrency requirements!

---

**Next: Work Stealing Pool & Fork Join Pool** →