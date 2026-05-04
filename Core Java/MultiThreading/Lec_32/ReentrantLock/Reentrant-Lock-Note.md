# ReentrantLock

## Overview

ReentrantLock is a custom lock that **does NOT depend on objects** like synchronized monitor lock. It provides explicit locking mechanism through lock objects.

## Four Types of Custom Locks

1. **ReentrantLock**
2. **ReadWriteLock**
3. **StampedLock**
4. **Semaphore**

## Key Difference from Synchronized

- **Synchronized**: Depends on object (monitor lock on object)
- **ReentrantLock**: Depends on lock object, NOT on class instances

## How ReentrantLock Works

### Basic Implementation
```java
import java.util.concurrent.locks.ReentrantLock;

class SharedResource {
    
    public void producer(ReentrantLock lock) {
        try {
            lock.lock();  // Acquire the lock
            System.out.println("Lock acquired by " + Thread.currentThread().getName());
            
            // Critical section - doing some work
            Thread.sleep(4000);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();  // Release the lock
            System.out.println("Lock released by " + Thread.currentThread().getName());
        }
    }
}
```

### Important Points

1. **lock.lock()** - Acquires the lock
2. **lock.unlock()** - Releases the lock
3. **unlock() must be in finally block** - Ensures lock is released even if exception occurs

## Example with Different Objects
```java
public static void main(String[] args) {
    ReentrantLock lock = new ReentrantLock();  // Create ONE lock object
    
    SharedResource resourceOne = new SharedResource();  // Different object 1
    SharedResource resourceTwo = new SharedResource();  // Different object 2
    
    // Both threads use different objects but SAME lock
    Thread t1 = new Thread(() -> resourceOne.producer(lock));
    Thread t2 = new Thread(() -> resourceTwo.producer(lock));
    
    t1.start();
    t2.start();
}
```

**Result**: Only one thread enters critical section at a time!
```
Lock acquired by Thread-0
(waits 4 seconds)
Lock released by Thread-0
Lock acquired by Thread-1
(waits 4 seconds)
Lock released by Thread-1
```

## How It Solves the Problem

- Even though we use **different objects** (resourceOne, resourceTwo)
- We pass the **same lock object** to both threads
- ReentrantLock doesn't care about object instances
- It only cares about the **lock object**
- If same lock is used, synchronization works correctly

## When to Use ReentrantLock

Use ReentrantLock when:
- Multiple threads create different objects
- You need to ensure only ONE thread accesses critical section
- Monitor lock (synchronized) won't work due to different object instances
- You need explicit control over lock acquisition and release

## Key Advantage

**Object-independent locking** - Works regardless of how many different objects you create, as long as the same lock object is passed.

---