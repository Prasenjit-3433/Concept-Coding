# Semaphore

## Overview

Semaphore is a lock that allows **multiple threads** to acquire the lock simultaneously. You can **control how many threads** can enter the critical section at the same time.

---

## Key Difference from Other Locks

| Lock Type | Threads Allowed in Critical Section |
|-----------|-------------------------------------|
| synchronized | 1 thread only |
| ReentrantLock | 1 thread only |
| ReadWriteLock | Multiple for read, 1 for write |
| **Semaphore** | **N threads (configurable)** |

---

## Creating Semaphore
```java
import java.util.concurrent.Semaphore;

// Create semaphore with 2 permits
Semaphore semaphore = new Semaphore(2);
```

**Permits = Number of threads that can acquire lock simultaneously**

---

## Semaphore Methods

### 1. acquire()
```java
semaphore.acquire();  // Acquire the lock (acquire one permit)
```
- Decrements available permits by 1
- If no permits available, thread **waits**

### 2. release()
```java
semaphore.release();  // Release the lock (release one permit)
```
- Increments available permits by 1
- Wakes up waiting threads

---

## Code Example
```java
import java.util.concurrent.Semaphore;

class SharedResource {
    
    public void producer(Semaphore lock) {
        try {
            lock.acquire();  // Acquire lock
            System.out.println("Lock acquired by " + Thread.currentThread().getName());
            
            // Critical section - doing some work
            Thread.sleep(4000);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.release();  // Release lock
            System.out.println("Lock released by " + Thread.currentThread().getName());
        }
    }
}
```

---

## Complete Example with 4 Threads
```java
public static void main(String[] args) {
    // Create semaphore with 2 permits
    Semaphore semaphore = new Semaphore(2);
    
    SharedResource resource = new SharedResource();
    
    // Create 4 threads
    Thread t1 = new Thread(() -> resource.producer(semaphore), "Thread-0");
    Thread t2 = new Thread(() -> resource.producer(semaphore), "Thread-1");
    Thread t3 = new Thread(() -> resource.producer(semaphore), "Thread-2");
    Thread t4 = new Thread(() -> resource.producer(semaphore), "Thread-3");
    
    t1.start();
    t2.start();
    t3.start();
    t4.start();
}
```

---

## Execution Flow

### Initial State
```
Semaphore permits: 2
Waiting threads: []
```

### Step 1: Thread-0 and Thread-1 arrive
```
Thread-0: acquire() → SUCCESS (permits: 2 → 1)
Thread-1: acquire() → SUCCESS (permits: 1 → 0)

Working: [Thread-0, Thread-1]
Waiting: [Thread-2, Thread-3]
```

### Step 2: Thread-0 releases after 4 seconds
```
Thread-0: release() → (permits: 0 → 1)
Thread-2: acquire() → SUCCESS (permits: 1 → 0)

Working: [Thread-1, Thread-2]
Waiting: [Thread-3]
```

### Step 3: Thread-1 releases
```
Thread-1: release() → (permits: 0 → 1)
Thread-3: acquire() → SUCCESS (permits: 1 → 0)

Working: [Thread-2, Thread-3]
Waiting: []
```

### Step 4: All complete
```
Thread-2: release() → (permits: 0 → 1)
Thread-3: release() → (permits: 1 → 2)

Working: []
Permits available: 2
```

---

## Output
```
Lock acquired by Thread-0
Lock acquired by Thread-1
(both wait 4 seconds)
Lock released by Thread-0
Lock acquired by Thread-2
Lock released by Thread-1
Lock acquired by Thread-3
(Thread-2 and Thread-3 wait 4 seconds)
Lock released by Thread-2
Lock released by Thread-3
```

---

## How It Works Internally
```
Semaphore(2)
├── Available Permits: 2
├── acquire() → Permits--
└── release() → Permits++

When Permits = 0:
└── New threads WAIT until permits > 0
```

---

## Real-World Use Cases

### 1. Printer Management
```java
// You have 2 printers
Semaphore printerSemaphore = new Semaphore(2);

// Maximum 2 threads can print simultaneously
public void print(Semaphore printer) {
    printer.acquire();
    try {
        // Use printer
    } finally {
        printer.release();
    }
}
```

**Scenario:**
- 10 users want to print
- Only 2 printers available
- Maximum 2 can print at a time
- Others wait until a printer is free

---

### 2. Database Connection Pool
```java
// Connection pool has 5 connections
Semaphore connectionPool = new Semaphore(5);

public void executeQuery(Semaphore pool) {
    pool.acquire();  // Get a connection
    try {
        // Execute database query
    } finally {
        pool.release();  // Return connection to pool
    }
}
```

**Scenario:**
- Database has 5 connections
- 20 threads want to query database
- Maximum 5 threads can query simultaneously
- Others wait for a connection to be released

---

### 3. Restaurant Tables
```java
// Restaurant has 3 tables
Semaphore tables = new Semaphore(3);

public void dineIn(Semaphore tables) {
    tables.acquire();  // Get a table
    try {
        // Customer eats
    } finally {
        tables.release();  // Leave table
    }
}
```

**Scenario:**
- Restaurant has 3 tables
- 10 customers arrive
- Maximum 3 can sit simultaneously
- Others wait for a table

---

### 4. Parking Lot
```java
// Parking lot has 10 spots
Semaphore parkingSpots = new Semaphore(10);

public void park(Semaphore spots) {
    spots.acquire();  // Park car
    try {
        // Car is parked
    } finally {
        spots.release();  // Car leaves
    }
}
```

---

## When to Use Semaphore

### Use Semaphore When:
- ✅ You have **limited resources** (printers, connections, tables, etc.)
- ✅ You want to **control the number** of concurrent accesses
- ✅ You need **more than 1 thread** but **not unlimited threads**

### Don't Use Semaphore When:
- ❌ Only **1 thread** should access → Use ReentrantLock instead
- ❌ **Unlimited threads** can access → No lock needed
- ❌ Need **read/write differentiation** → Use ReadWriteLock instead

---

## Comparison with Other Locks
```java
// Only 1 thread at a time
ReentrantLock lock = new ReentrantLock();

// Multiple readers, 1 writer
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// Exactly N threads at a time
Semaphore semaphore = new Semaphore(N);
```

---

## Important Notes

1. **Permits must match resources**: If you have 5 database connections, use `Semaphore(5)`
2. **Always release in finally block**: Ensures permit is returned even if exception occurs
3. **Fair vs Unfair**:
```java
   Semaphore fairSemaphore = new Semaphore(2, true);  // Fair - FIFO order
   Semaphore unfairSemaphore = new Semaphore(2);      // Unfair - no order guarantee
```

---

## Summary

1. **Semaphore allows N threads** to acquire lock simultaneously
2. **Permits control** how many threads can enter
3. **acquire()** decrements permits, **release()** increments permits
4. When permits = 0, threads **wait**
5. Perfect for **limited resource management**
6. Examples: Connection pools, printer queues, parking lots, restaurant tables
7. **acquire() must be paired with release()** (use finally block)

---