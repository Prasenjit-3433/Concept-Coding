# StampedLock - Optimistic Locking Implementation

## Overview

StampedLock provides **optimistic locking** functionality in Java, similar to database row versioning.

---

## Key Concepts

### Stamp = Version Number
- In database: `row_version` tracks changes
- In StampedLock: **stamp** tracks lock state changes
- Stamp changes when write operations occur

### How It Works
1. **Try Optimistic Read**: Get current stamp (like noting row_version)
2. **Perform Operations**: Do your work without holding lock
3. **Validate**: Check if stamp is still valid (like checking if row_version changed)
4. **Result**:
    - Valid → Commit changes
    - Invalid → Rollback and retry

---

## Optimistic Read Implementation

### Code Example
```java
class SharedResource {
    int data = 10;
    
    public void producer(StampedLock lock) {
        // Step 1: Try optimistic read (NO lock acquired)
        long stamp = lock.tryOptimisticRead();
        System.out.println("Taken optimistic read");
        
        // Step 2: Perform operations
        data = 11;  // Trying to update value from 10 to 11
        
        // Simulating some work
        try {
            Thread.sleep(6000);  // Wait 6 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Step 3: Validate - Did anyone perform write operation meanwhile?
        if (lock.validate(stamp)) {
            // Valid: No write happened, safe to commit
            System.out.println("Updated successfully to 11");
        } else {
            // Invalid: Someone did write operation, rollback
            System.out.println("Rollback work - stamp changed");
            data = 10;  // Rollback to original value
        }
    }
}
```

---

## Write Lock Implementation
```java
class SharedResource {
    int data = 10;
    
    public void consumer(StampedLock lock) {
        long stamp = 0;
        
        try {
            stamp = lock.writeLock();  // Acquire write lock
            System.out.println("Write lock acquired by " + Thread.currentThread().getName());
            
            // Perform write operations
            data = 15;
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlockWrite(stamp);  // Release write lock
            System.out.println("Write lock released by " + Thread.currentThread().getName());
        }
    }
}
```

---

## Important Methods

### 1. tryOptimisticRead()
```java
long stamp = lock.tryOptimisticRead();
```
- Does **NOT acquire any lock**
- Returns current stamp (state of lock)
- Like noting down row_version before reading

### 2. validate(stamp)
```java
boolean isValid = lock.validate(stamp);
```
- Checks if stamp is still valid
- Returns `true` if no write happened since stamp was obtained
- Returns `false` if write operation occurred (stamp changed)

### 3. No Unlock Needed for Optimistic Read
- `tryOptimisticRead()` doesn't acquire lock
- So no `unlock()` needed
- Only validation is required

---

## Complete Example
```java
public static void main(String[] args) {
    StampedLock lock = new StampedLock();
    SharedResource resource = new SharedResource();
    
    // Thread 1: Optimistic read
    Thread t1 = new Thread(() -> resource.producer(lock));
    
    // Thread 2: Write lock
    Thread t2 = new Thread(() -> resource.consumer(lock));
    
    t1.start();
    Thread.sleep(1000);  // Small delay
    t2.start();
}
```

---

## Execution Flow - Scenario 1 (Write Happens)

### Timeline

**T1**: Thread 1 starts
```
- tryOptimisticRead() → stamp = 1
- Updates data = 11 (in memory)
- Sleeps for 6 seconds
```

**T2**: Thread 2 starts (after 1 second)
```
- Acquires writeLock() → stamp changes to 2
- Updates data = 15
- Releases writeLock()
```

**T3**: Thread 1 wakes up (after 6 seconds)
```
- Validates stamp (saved stamp = 1, current stamp = 2)
- validate(1) returns FALSE
- Rollback: data = 10
```

### Output
```
Taken optimistic read
Write lock acquired by Thread-1
Write lock released by Thread-1
Rollback work - stamp changed
```

---

## Execution Flow - Scenario 2 (No Write)

### Timeline

**T1**: Thread 1 starts (Thread 2 doesn't start)
```
- tryOptimisticRead() → stamp = 1
- Updates data = 11 (in memory)
- Sleeps for 6 seconds
- No other thread interferes
```

**T2**: Thread 1 validates
```
- validate(1) returns TRUE (stamp unchanged)
- Update successful: data = 11
```

### Output
```
Taken optimistic read
(waits 6 seconds)
Updated successfully to 11
```

---

## How Stamp Changes Internally

### Write Operations Change Stamp
```java
// When writeLock() is acquired
stamp = lock.writeLock();  // Stamp value changes internally

// When unlockWrite() is called
lock.unlockWrite(stamp);   // Stamp value increments/changes again
```

### Optimistic Read Uses Stamp
```java
long savedStamp = lock.tryOptimisticRead();  // Save current stamp

// ... do work ...

lock.validate(savedStamp);  // Check if stamp changed
```

---

## Key Differences: Optimistic vs Read/Write Lock

| Feature | Read/Write Lock | Optimistic Lock |
|---------|----------------|-----------------|
| Lock Acquired? | ✅ Yes (readLock) | ❌ No |
| Blocking? | ✅ Yes | ❌ No |
| Unlock Needed? | ✅ Yes | ❌ No |
| Validation Needed? | ❌ No | ✅ Yes (validate) |
| Performance | Medium | High (no blocking) |
| Use Case | Read-heavy | Low contention |

---

## Why Stamp in Read/Write Lock?

**Question**: Why do read/write lock methods also return stamp?

**Answer**:
- StampedLock supports **both** read/write locking AND optimistic locking
- Internally, it maintains stamp for all operations
- Read/write operations change stamp state
- This changed stamp is what optimistic read validates against
- **Stamp is primarily useful for optimistic locking validation**

---

## When to Use Optimistic Locking

### Good Scenarios ✅
- **Very high read operations**
- **Very low write operations**
- When blocking is expensive
- When contention is rare

### Example Use Cases
- Reading configuration data (rarely updated)
- Cache reads (infrequent updates)
- Read-heavy analytics

---

## Summary

1. **tryOptimisticRead()** - No lock, just saves stamp
2. **validate(stamp)** - Checks if write happened
3. **No unlock** needed for optimistic read
4. **Stamp tracks lock state** (like row_version)
5. **Write operations change stamp** internally
6. If validation fails → **rollback and retry**
7. If validation succeeds → **commit changes**
8. **Best performance** when writes are rare
9. StampedLock combines **both read/write lock + optimistic locking**

---