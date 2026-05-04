# Issue with Monitor Lock

## Problem Statement

When using `synchronized` keyword in Java, it places a **monitor lock** on an object. This creates limitations in certain scenarios.

## How Monitor Lock Works
```java
class SharedResource {
    public synchronized void producer() {
        // Critical section
        System.out.println("Lock acquired by " + Thread.currentThread().getName());
        // Doing some work...
        Thread.sleep(4000);
        System.out.println("Lock released by " + Thread.currentThread().getName());
    }
}
```

### Key Characteristic: Object-Dependent Locking

The monitor lock is placed on a specific object. This means:

- If Thread 1 calls `producer()` using `resource1` object → monitor lock is placed on `resource1`
- If Thread 2 calls `producer()` using `resource2` object → monitor lock is placed on `resource2`

### Example Scenario
```java
public static void main(String[] args) {
    SharedResource resource1 = new SharedResource();
    SharedResource resource2 = new SharedResource();
    
    Thread t1 = new Thread(() -> resource1.producer());
    Thread t2 = new Thread(() -> resource2.producer());
    
    t1.start();
    t2.start();
}
```

**Result**: Both threads enter the critical section simultaneously!
```
Lock acquired by Thread-0
Lock acquired by Thread-1
(after 4 seconds)
Lock released by Thread-0
Lock released by Thread-1
```

## The Problem

When different objects are used to invoke the synchronized method, the monitor lock doesn't prevent concurrent access. This is problematic when:

- Multiple threads create different object instances
- You need only ONE thread to access the critical section regardless of which object is used
- Real-world scenarios where threads work with different objects but access shared resources

## Why This Happens

Synchronized methods work only when all threads use the same object. If threads use different objects, each object gets its own monitor lock, defeating the purpose of synchronization.

## Solution Needed

A locking mechanism that:
- Does NOT depend on objects
- Ensures only one thread enters critical section
- Works across multiple object instances

This leads us to **Custom Locks** (ReentrantLock, ReadWriteLock, StampedLock, Semaphore).

---