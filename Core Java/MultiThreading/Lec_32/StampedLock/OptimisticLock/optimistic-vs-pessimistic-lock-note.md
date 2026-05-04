# Optimistic Locking

## Pessimistic vs Optimistic Locking

### Pessimistic Locking
- **Lock IS acquired** on the resource
- Examples: synchronized, ReentrantLock, ReadWriteLock, Semaphore
- Thread must wait if resource is locked

### Optimistic Locking
- **NO lock is acquired** on the resource
- Named "optimistic" because it optimistically assumes no conflicts
- Uses version checking instead of locks

---

## How Optimistic Locking Works

### Database Example

Consider a database table:

| ID  | Name | Type    | Row_Version |
|-----|------|---------|-------------|
| 123 | SJ   | Student | 1           |
| 456 | Ram  | Student | 1           |

**Row_Version Rules:**
- New row inserted → `row_version = 1`
- Every update → `row_version` increments

---

## Scenario: Two Threads Accessing Same Row

### Setup
- **Thread 1** and **Thread 2** both want to access row with ID = 123
- Both threads want to update the same row simultaneously

### Timeline

#### Time T1: Both Threads Read
```
Thread 1 reads row 123:
- Data: ID=123, Name=SJ, Type=Student
- Notes: row_version = 1

Thread 2 reads row 123:
- Data: ID=123, Name=SJ, Type=Student  
- Notes: row_version = 1
```

#### Time T2: Both Perform Operations
```
Thread 1: Updates Type to "Teacher" (in memory, not DB yet)
Thread 2: Updates Type to "Ex-Student" (in memory, not DB yet)
```

---

## Two Scenarios

### Scenario 1: Thread 2 Updates First

#### Time T3: Thread 2 Writes to DB
```sql
UPDATE table
SET type = 'Ex-Student'
WHERE id = 123 AND row_version = 1;
```

**Result:**
- ✅ row_version is still 1, so update succeeds
- Row becomes: `ID=123, Type=Ex-Student, row_version=2`
- row_version automatically incremented to **2**

#### Time T4: Thread 1 Tries to Write to DB
```sql
UPDATE table
SET type = 'Teacher'
WHERE id = 123 AND row_version = 1;
```

**Result:**
- ❌ **Update FAILS!** row_version is now 2, not 1
- Thread 1's operation is **rolled back**

#### Thread 1 Must Retry
1. Read the row again
2. Note new row_version = 2
3. Perform operation again
4. Try update with new version:
```sql
   UPDATE table
   SET type = 'Teacher'
   WHERE id = 123 AND row_version = 2;
```
5. ✅ Now succeeds, row_version becomes 3

---

## Key Points of Optimistic Locking

### How It Works
1. **Read**: Save the current version number
2. **Modify**: Perform operations locally
3. **Write**: Update ONLY if version hasn't changed
4. **Validation**: Check version before committing
5. **Retry**: If version changed, rollback and retry

### No Actual Lock
- No thread blocks another thread
- Both can read simultaneously
- Conflict detected during write (not during read)

### Version-Based Conflict Resolution
```
IF (current_row_version == my_saved_version) THEN
    UPDATE and INCREMENT version
ELSE
    ROLLBACK and RETRY
END IF
```

---

## Optimistic vs Pessimistic

| Aspect | Pessimistic | Optimistic |
|--------|-------------|------------|
| Lock Acquired? | ✅ Yes | ❌ No |
| Blocking? | ✅ Threads wait | ❌ Threads proceed |
| Conflict Detection | At lock time | At update time |
| Best For | High contention | Low contention |
| Mechanism | Lock/Unlock | Version checking |

---

## When to Use Optimistic Locking

### Good Use Cases ✅
- **Low contention** (conflicts are rare)
- Read-heavy applications
- Short transactions
- When lock overhead is expensive

### Bad Use Cases ❌
- **High contention** (many conflicts expected)
- Long-running transactions
- When retry cost is high

---

## Summary

1. **Optimistic Locking = No Lock**
2. Uses **version numbers** to detect conflicts
3. Thread saves version when reading
4. Before updating, checks if version changed
5. If version changed → rollback and retry
6. If version same → update and increment version
7. Better performance when conflicts are rare
8. More retries when conflicts are frequent

---