# Condition - Inter-Thread Communication

## The Problem

### With Synchronized (Monitor Lock)
We had inter-thread communication using:
- `wait()` - Thread waits on monitor lock
- `notify()` - Wake up one waiting thread
- `notifyAll()` - Wake up all waiting threads

**But**: `wait()` and `notify()` work ONLY with **synchronized (monitor lock)**

---

### With Custom Locks (ReentrantLock, ReadWriteLock, StampedLock, Semaphore)
- We are **NOT using monitor lock**
- We are using **custom locks**
- **Problem**: `wait()` and `notify()` **CANNOT be used** with custom locks

**Solution**: Use **Condition** interface

---

## Condition Interface

### What is Condition?
- Provides inter-thread communication for **custom locks**
- Works with ReentrantLock, ReadWriteLock, etc.
- **NOT** for synchronized blocks

### Methods Comparison

| Monitor Lock (synchronized) | Condition (Custom Locks) |
|----------------------------|--------------------------|
| `wait()` | `await()` |
| `notify()` | `signal()` |
| `notifyAll()` | `signalAll()` |

**Functionality is EXACTLY the same, only names are different**

---

## Creating Condition Object
```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

// Step 1: Create lock
ReentrantLock lock = new ReentrantLock();

// Step 2: Create condition on that lock
Condition condition = lock.newCondition();
```

**Key Point**: Condition is created **from a lock object**

---

## Producer-Consumer Problem with Condition

### Shared Resource Class
```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class SharedResource {
    private boolean isAvailable = false;
    
    public void producer(ReentrantLock lock, Condition condition) {
        try {
            lock.lock();  // Acquire lock
            
            // If item already available, wait for consumer
            if (isAvailable) {
                System.out.println("Producer waiting...");

                // starts waiting on "CONDITION" object, not on "LOCK" obj (because a lock can have multiple condition obj)
                condition.await();  // Wait on this condition (like wait())
            }
            
            // Produce item
            isAvailable = true;
            System.out.println("Produced by " + Thread.currentThread().getName());
            
            // Signal consumer that item is available (wakes up those threads which are waiting on the SAME condition obj)
            condition.signal();  // Wake up waiting thread (like notify())
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();  // Release lock
        }
    }
    
    public void consumer(ReentrantLock lock, Condition condition) {
        try {
            lock.lock();  // Acquire lock
            
            // If item not available, wait for producer
            if (!isAvailable) {
                System.out.println("Consumer waiting...");

                // starts waiting on "CONDITION" object, not on "LOCK" obj (because a lock can have multiple condition obj)
                condition.await();  // Wait on this condition (like wait())
            }
            
            // Consume item
            isAvailable = false;
            System.out.println("Consumed by " + Thread.currentThread().getName());
            
            // Signal producer that space is available (wakes up those threads which are waiting on the SAME condition obj)
            condition.signal();  // Wake up waiting thread (like notify())
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();  // Release lock
        }
    }
}
```

---

## Main Method
```java
public static void main(String[] args) {
    SharedResource resource = new SharedResource();
    
    // Create lock and condition
    ReentrantLock lock = new ReentrantLock();
    Condition condition = lock.newCondition();
    
    // Producer thread
    Thread producer = new Thread(() -> {
        for (int i = 0; i < 5; i++) {
            resource.producer(lock, condition);
        }
    }, "Producer");
    
    // Consumer thread
    Thread consumer = new Thread(() -> {
        for (int i = 0; i < 5; i++) {
            resource.consumer(lock, condition);
        }
    }, "Consumer");
    
    producer.start();
    consumer.start();
}
```

---

## Execution Flow

### Scenario 1: Producer Runs First
```
1. Producer acquires lock
2. isAvailable = false, so produce
3. isAvailable = true
4. signal() - but no one is waiting
5. Producer releases lock

6. Consumer acquires lock
7. isAvailable = true, so consume
8. isAvailable = false
9. signal() - wakes up producer (if waiting)
10. Consumer releases lock
```

### Scenario 2: Producer Tries to Produce Again
```
1. Producer acquires lock
2. isAvailable = true (already produced)
3. await() - Producer WAITS on lock
4. Lock is released (await releases lock)

5. Consumer acquires lock
6. isAvailable = true, so consume
7. isAvailable = false
8. signal() - wakes up Producer
9. Consumer releases lock

10. Producer wakes up and continues
11. Produces item
12. signal() and release lock
```

---

## Output Example
```
Produced by Producer
Consumed by Consumer
Produced by Producer
Consumed by Consumer
Producer waiting...
Consumed by Consumer
Produced by Producer
...
```

---

## How await() Works

### When thread calls condition.await():
1. **Releases the lock** (so other threads can acquire it)
2. **Thread goes to waiting state** on that condition
3. **Waits for signal()** from another thread
4. When signaled, **re-acquires the lock** and continues

### Similar to wait():
```java
// synchronized block
synchronized(obj) {
    obj.wait();  // Releases monitor lock and waits
}

// Custom lock
lock.lock();
condition.await();  // Releases custom lock and waits
```

---

## signal() vs signalAll()

### signal()
```java
condition.signal();
```
- Wakes up **ONE** arbitrary waiting thread
- Like `notify()`

### signalAll()
```java
condition.signalAll();
```
- Wakes up **ALL** waiting threads
- Like `notifyAll()`

---

## Key Differences: wait/notify vs await/signal

| Feature | wait/notify | await/signal |
|---------|-------------|--------------|
| **Works with** | synchronized (monitor lock) | Custom locks (ReentrantLock, etc.) |
| **Wait method** | `wait()` | `await()` |
| **Notify one** | `notify()` | `signal()` |
| **Notify all** | `notifyAll()` | `signalAll()` |
| **Where defined** | Object class | Condition interface |
| **How to get** | Every object has it | `lock.newCondition()` |

---

## Important Points

### 1. Must Hold Lock Before await/signal
```java
// WRONG - will throw IllegalMonitorStateException
condition.await();  // Lock not held!

// CORRECT
lock.lock();
try {
    condition.await();  // Lock held
} finally {
    lock.unlock();
}
```

### 2. await() Releases Lock
```java
lock.lock();
condition.await();  // Lock is RELEASED here
// When signaled, lock is RE-ACQUIRED here
lock.unlock();
```

### 3. Always Use in try-finally
```java
lock.lock();
try {
    // Work and await/signal
} finally {
    lock.unlock();  // Always release
}
```

---

## Multiple Conditions on Same Lock

You can create **multiple conditions** on the same lock:
```java
ReentrantLock lock = new ReentrantLock();
Condition notFull = lock.newCondition();   // For producer
Condition notEmpty = lock.newCondition();  // For consumer

// Producer waits on notFull
notFull.await();
notEmpty.signal();  // Signal consumer

// Consumer waits on notEmpty
notEmpty.await();
notFull.signal();  // Signal producer
```

**Advantage**: More precise control over which threads to wake up

---

## When to Use Condition

### Use Condition When:
- Using **custom locks** (ReentrantLock, ReadWriteLock, etc.)
- Need **inter-thread communication**
- Want threads to **wait and signal** each other
- Implementing **producer-consumer**, **bounded buffer**, etc.

### Use wait/notify When:
- Using **synchronized** blocks/methods
- Using **monitor locks**

---

## Summary

1. **Condition** provides inter-thread communication for **custom locks**
2. `await()` = `wait()` - Thread waits and releases lock
3. `signal()` = `notify()` - Wake up one waiting thread
4. `signalAll()` = `notifyAll()` - Wake up all waiting threads
5. **Cannot use wait/notify** with custom locks
6. **Cannot use await/signal** with synchronized
7. Condition is created from lock: `lock.newCondition()`
8. Thread must **hold the lock** before calling await/signal
9. `await()` **releases the lock** while waiting
10. Same functionality as wait/notify, just different API

---