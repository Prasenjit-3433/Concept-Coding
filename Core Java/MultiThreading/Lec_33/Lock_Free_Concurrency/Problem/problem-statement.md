# Lock-Free Concurrency & Compare-and-Swap (CAS)

## Overview
This lecture covers advanced concurrency mechanisms in Java, focusing on lock-free approaches and atomic operations. It explains how concurrency can be achieved without traditional locking mechanisms, leading to better performance in specific use cases.

---

## The Problem: Non-Atomic Operations

### Single-Threaded Scenario (Works Fine)
```java
class SharedResource {
    private int counter = 0;

    public void increment() {
        counter++;  // Seems simple, but...
    }

    public int get() {
        return counter;
    }
}
```
```java
public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        for (int i = 0; i < 400; i++) {
            resource.increment();
        }

        System.out.println(resource.get());  // Output: 400 ✓
    }
}
```

**Result:** Works perfectly because only one thread is executing.

---

### The Hidden Problem: What Really Happens in `counter++`

The statement `counter++` is **NOT atomic**. It actually involves **3 separate operations**:
```
counter++;  // Expands to:

// Step 1: Load counter value from memory
int temp = counter;

// Step 2: Increment the value
temp = temp + 1;

// Step 3: Assign back to counter
counter = temp;
```

**Equivalent to:** `counter = counter + 1;`

**Key Point:** These three steps are **not executed as a single unit**, making it vulnerable to race conditions.

---

### Multi-Threaded Scenario (The Problem)
```java
class SharedResource {
    private int counter = 0;
    
    public void increment() {
        counter++;
    }
    
    public int get() {
        return counter;
    }
}
```

```java
public class Main {
    public static void main(String[] args) throws InterruptedException {
        SharedResource resource = new SharedResource();

        // Thread 1
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 200; i++) {
                resource.increment();
            }
        });

        // Thread 2
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 200; i++) {
                resource.increment();
            }
        });

        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (Exception e) {
            // exception handling
        }

        System.out.println(resource.get());  // Output: 371 (or some random value < 400) ✗
    }
}
```

**Expected Output:** 400 (200 + 200)  
**Actual Output:** 371 (or some other value less than 400)

---

### Why Does This Happen? (Race Condition Visualization)

Let's say `counter = 0` initially:

| Time | Thread 1 | Thread 2 | Counter Value |
|------|----------|----------|---------------|
| T1 | Load counter → reads **0** | Load counter → reads **0** | 0 |
| T2 | Increment → **0 + 1 = 1** | Increment → **0 + 1 = 1** | 0 |
| T3 | Assign back → counter = **1** | Assign back → counter = **1** | 1 |

**Problem:** Both threads read the same value (0), both increment it to 1, and both write 1 back. Instead of 2, counter becomes 1.

**Result:** Data loss! Two increments were performed, but only one was recorded.

---

### Key Takeaways

1. **`counter++` is NOT atomic** - It's actually 3 separate operations (load, increment, assign)
2. **Not thread-safe** - Multiple threads can interfere with each other
3. **Race condition** - Threads reading stale values and overwriting each other's changes
4. **Data loss** - Final result is less than expected

---

## Solutions (Coming Next)

There are **two ways** to fix this problem:

### 1. Lock-Based Solution
```java
class SharedResource {
    private int counter = 0;
    
    public synchronized void increment() {
        counter++;
    }
    
    public int get() {
        return counter;
    }
}
```
- Uses locks (synchronized keyword)
- Only one thread can execute at a time
- Safe but potentially slower

### 2. Lock-Free Solution
```java
import java.util.concurrent.atomic.AtomicInteger;

class SharedResource {
    private AtomicInteger counter = new AtomicInteger(0);

    public void increment() {
        counter.incrementAndGet();
    }

    public int get() {
        return counter.get();
    }
}
```
- Uses Compare-and-Swap (CAS) operation
- No locks involved
- Faster for specific use cases

---

**Next:** Let's explore the lock-free solution and how CAS operation works!