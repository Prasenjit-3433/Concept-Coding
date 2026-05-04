# ReadWriteLock

## Prerequisites: Understanding Shared Lock vs Exclusive Lock

Before understanding ReadWriteLock, you need to know about **Shared Lock** and **Exclusive Lock**.

### Shared Lock (Read Lock)
- Multiple threads CAN acquire shared lock simultaneously
- Threads can only **READ** the resource
- **Cannot WRITE/modify** the resource
- Also known as **Read Lock**

### Exclusive Lock (Write Lock)
- Only ONE thread can acquire exclusive lock
- Thread can both **READ and WRITE** the resource
- Also known as **Write Lock**

## Lock Acquisition Rules

### Rule 1: When Thread 1 has Shared Lock
- ✅ Thread 2 CAN acquire Shared Lock (read together)
- ❌ Thread 2 CANNOT acquire Exclusive Lock

### Rule 2: When Thread 1 has Exclusive Lock
- ❌ Thread 2 CANNOT acquire Shared Lock
- ❌ Thread 2 CANNOT acquire Exclusive Lock
- **No other lock can be taken until exclusive lock is released**

### Rule 3: To Acquire Exclusive Lock
- Resource must have **NO lock** at all (neither shared nor exclusive)

## Summary Table

| Current Lock State | Can Acquire Shared Lock? | Can Acquire Exclusive Lock? |
|-------------------|-------------------------|----------------------------|
| No Lock           | ✅ Yes                   | ✅ Yes                      |
| Shared Lock       | ✅ Yes (multiple)        | ❌ No                       |
| Exclusive Lock    | ❌ No                    | ❌ No                       |

---

## ReadWriteLock Implementation

### Concept
- **Read Lock** = Shared Lock (multiple threads can read)
- **Write Lock** = Exclusive Lock (only one thread can write)

### Creating ReadWriteLock Object
```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// ReadWriteLock is an interface
// ReentrantReadWriteLock is its implementation class
ReadWriteLock lock = new ReentrantReadWriteLock();
```

## Code Example

### Producer Method (Read Lock)
```java
class SharedResource {
    
    public void read(ReadWriteLock lock) {
        try {
            lock.readLock().lock();  // Acquire read lock (shared lock)
            System.out.println("Read lock acquired by " + Thread.currentThread().getName());
            
            // Should only READ values here, NOT modify
            // Do some work...
            Thread.sleep(8000);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();  // Release read lock
            System.out.println("Read lock released by " + Thread.currentThread().getName());
        }
    }
}
```

### Consumer Method (Write Lock)
```java
class SharedResource {
    
    public void write(ReadWriteLock lock) {
        try {
            lock.writeLock().lock();  // Acquire write lock (exclusive lock)
            System.out.println("Write lock acquired by " + Thread.currentThread().getName());
            
            // Can READ and WRITE values here
            // Do some work...
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();  // Release write lock
            System.out.println("Write lock released by " + Thread.currentThread().getName());
        }
    }
}
```

## Example Execution
```java
public static void main(String[] args) {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    SharedResource resource = new SharedResource();
    
    // Thread 1 - Read lock
    Thread t1 = new Thread(() -> resource.read(lock));
    
    // Thread 2 - Read lock
    Thread t2 = new Thread(() -> resource.read(lock));
    
    // Thread 3 - Write lock (different object but same lock)
    SharedResource resource2 = new SharedResource();
    Thread t3 = new Thread(() -> resource2.write(lock));
    
    t1.start();
    t2.start();
    t3.start();
}
```

### Execution Flow

1. **Thread 1** acquires **read lock** → waits 8 seconds
2. **Thread 2** acquires **read lock** (allowed, both reading simultaneously)
3. **Thread 3** tries to acquire **write lock** → BLOCKED (shared locks are active)
4. After Thread 1 and Thread 2 release read locks
5. **Thread 3** acquires **write lock** and proceeds

### Output
```
Read lock acquired by Thread-0
Read lock acquired by Thread-1
(both wait 8 seconds)
Read lock released by Thread-0
Read lock released by Thread-1
Write lock acquired by Thread-2
(Thread 2 completes work)
Write lock released by Thread-2
```

## When to Use ReadWriteLock

### Ideal Use Case
When your application has:
- **High READ operations** (e.g., 1000 reads)
- **Low WRITE operations** (e.g., 10 writes)

### Benefits
- Multiple threads can read simultaneously (no waiting)
- Better performance for read-heavy applications
- Threads don't block each other for read operations

### Example Scenarios
- Database applications with frequent queries but rare updates
- Configuration data that's read often but modified rarely
- Caching systems with high read-to-write ratio

## Key Points to Remember

1. **Read Lock = Shared Lock** - Multiple threads allowed
2. **Write Lock = Exclusive Lock** - Only one thread allowed
3. With read lock, you should **only read**, not write
4. Write lock allows both read and write operations
5. Write lock waits until all read locks are released
6. Best for **read-heavy, write-light** scenarios

---