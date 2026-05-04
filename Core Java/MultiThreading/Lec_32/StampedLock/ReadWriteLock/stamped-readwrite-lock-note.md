# StampedLock - Read/Write Lock Functionality

## Overview

StampedLock provides **two functionalities**:
1. **Read/Write Lock capability** (similar to ReadWriteLock)
2. **Optimistic Locking** (unique feature)

We'll first cover the Read/Write Lock functionality.

---

## Creating StampedLock Object
```java
import java.util.concurrent.locks.StampedLock;

StampedLock lock = new StampedLock();
```

---

## Read Lock Implementation

### Code Example
```java
class SharedResource {
    
    public void producer(StampedLock lock) {
        long stamp = 0;  // Variable to store stamp
        
        try {
            stamp = lock.readLock();  // Acquire read lock, returns a stamp
            System.out.println("Read lock acquired by " + Thread.currentThread().getName());
            
            // Perform read operations
            // Do some work...
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlockRead(stamp);  // Unlock by passing the stamp
            System.out.println("Read lock released by " + Thread.currentThread().getName());
        }
    }
}
```

### Key Differences from ReadWriteLock

1. **lock.readLock()** returns a **stamp** (long value)
2. **unlock** requires passing the **stamp**: `lock.unlockRead(stamp)`

---

## Write Lock Implementation

### Code Example
```java
class SharedResource {
    
    public void consumer(StampedLock lock) {
        long stamp = 0;  // Variable to store stamp
        
        try {
            stamp = lock.writeLock();  // Acquire write lock, returns a stamp
            System.out.println("Write lock acquired by " + Thread.currentThread().getName());
            
            // Perform write operations
            // Do some work...
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlockWrite(stamp);  // Unlock by passing the stamp
            System.out.println("Write lock released by " + Thread.currentThread().getName());
        }
    }
}
```

---

## Why Does StampedLock Use Stamps?

### Question: Why acquire lock and get stamp? Why pass stamp during unlock?

**Answer**: StampedLock supports **Optimistic Locking** (covered in next topic)

- In optimistic locking, you need to track the **state/version** of the lock
- The **stamp represents the state** of the lock at the time of acquisition
- Similar to **row version** in database optimistic locking
- Though stamps are not heavily used in read/write operations, they are **crucial for optimistic locking**

### Analogy
Think of stamp as a "version number":
- When you read, you note down the version
- When unlocking, you verify with that version
- This becomes important for validating lock state in optimistic scenarios

---

## Comparison with ReadWriteLock

| Feature | ReadWriteLock | StampedLock |
|---------|---------------|-------------|
| Read Lock Syntax | `lock.readLock().lock()` | `stamp = lock.readLock()` |
| Read Unlock Syntax | `lock.readLock().unlock()` | `lock.unlockRead(stamp)` |
| Write Lock Syntax | `lock.writeLock().lock()` | `stamp = lock.writeLock()` |
| Write Unlock Syntax | `lock.writeLock().unlock()` | `lock.unlockWrite(stamp)` |
| Returns Stamp? | ❌ No | ✅ Yes |
| Optimistic Locking | ❌ Not supported | ✅ Supported |

---

## Complete Example
```java
public static void main(String[] args) {
    StampedLock lock = new StampedLock();
    SharedResource resource = new SharedResource();
    
    Thread t1 = new Thread(() -> resource.producer(lock));  // Read lock
    Thread t2 = new Thread(() -> resource.consumer(lock));  // Write lock
    
    t1.start();
    t2.start();
}
```

### Behavior
- Multiple threads can acquire read lock simultaneously
- Only one thread can acquire write lock
- Write lock waits until all read locks are released
- **Same behavior as ReadWriteLock**

---

## Key Points

1. StampedLock provides **Read/Write lock functionality** like ReadWriteLock
2. Main difference: Returns a **stamp** value during lock acquisition
3. Stamp must be **passed during unlock**
4. Stamp represents the **state of the lock**
5. This stamp mechanism is primarily useful for **optimistic locking** (next topic)
6. For basic read/write operations, stamp doesn't add much value
7. The real power of stamps is seen in **optimistic locking scenarios**

---