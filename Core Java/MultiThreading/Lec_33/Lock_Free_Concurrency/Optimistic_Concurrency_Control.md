# Optimistic Concurrency Control

### What is Optimistic Concurrency Control?

Optimistic Concurrency Control is a **lock-free protocol** used primarily in databases. It assumes that conflicts between transactions are rare, so it allows multiple transactions to proceed without locking resources.

**Key Principle:** Instead of preventing conflicts (pessimistic), it detects conflicts and retries when they occur (optimistic).

---

### How It Works: Row Versioning
```
Database Table:
+------------+--------+-------------+
| Row Number | Name   | Row Version |
+------------+--------+-------------+
| 123        | Raj    | 1           |
+------------+--------+-------------+
```

---

### Step-by-Step Example with Two Threads

#### Initial State:
```
Row: 123 | Name: Raj | Version: 1
```

#### Step 1: Both Threads Read the Same Row

**Thread 1** reads:
- Row: 123
- Name: Raj
- Version: **1** (records this version)

**Thread 2** reads:
- Row: 123
- Name: Raj
- Version: **1** (records this version)

---

#### Step 2: Both Threads Modify the Data

**Thread 1** modifies:
- Changes "Raj" → "Raj K"

**Thread 2** modifies:
- Changes "Raj" → "Raja"

---

#### Step 3: Thread 1 Updates First (Success)

**Thread 1 executes:**
```sql
UPDATE table
SET name = 'Raj K', row_version = row_version + 1
WHERE row_number = 123 AND row_version = 1;
```

**Check:**
- Row number = 123? ✓
- Row version = 1? ✓

**Result:**
- Update successful
- New state: `123 | Raj K | Version: 2`

---

#### Step 4: Thread 2 Tries to Update (Fails)

**Thread 2 executes:**
```sql
UPDATE table
SET name = 'Raja', row_version = row_version + 1
WHERE row_number = 123 AND row_version = 1;
```

**Check:**
- Row number = 123? ✓
- Row version = 1? ✗ (Current version is 2, not 1)

**Result:**
- Update fails!
- Database detects that someone else modified the row

---

#### Step 5: Thread 2 Retries

1. **Thread 2 reads again:**
    - Row: 123
    - Name: Raj K (updated by Thread 1)
    - Version: **2**

2. **Thread 2 modifies:**
    - Changes "Raj K" → "Raja"

3. **Thread 2 updates:**
```sql
UPDATE table
SET name = 'Raja', row_version = row_version + 1
WHERE row_number = 123 AND row_version = 2;
```

**Check:**
- Row number = 123? ✓
- Row version = 2? ✓

**Result:**
- Update successful
- New state: `123 | Raja | Version: 3`

---

### Visual Flow Diagram
```
Thread 1                          Thread 2
   |                                 |
   |-- Read (Version: 1) ------------|-- Read (Version: 1)
   |                                 |
   |-- Modify: Raj → Raj K           |-- Modify: Raj → Raja
   |                                 |
   |-- Update (Version: 1)           |
   |   ✓ Success                     |
   |   Version: 1 → 2                |
   |                                 |
   |                                 |-- Update (Version: 1)
   |                                 |   ✗ Fail (Version is now 2)
   |                                 |
   |                                 |-- Re-read (Version: 2)
   |                                 |-- Modify: Raj K → Raja
   |                                 |-- Update (Version: 2)
   |                                 |   ✓ Success
   |                                 |   Version: 2 → 3
```

---

### Key Characteristics

1. **Lock-Free:** No locks are used; multiple threads can read simultaneously
2. **Version-Based:** Each row has a version number that increments with every update
3. **Conflict Detection:** Detects conflicts by checking version numbers during update
4. **Retry Mechanism:** Failed updates force the thread to re-read and retry
5. **Database Level:** This approach is typically used in database systems

---

### Why is it Called "Optimistic"?

- **Optimistic assumption:** Conflicts are rare, so don't lock upfront
- **Contrast with Pessimistic locking:** Lock the row immediately when reading (assuming conflicts will happen)

---

### Important Notes

- This is a **lock-free protocol** (similar to CAS, which we'll cover next)
- Works well when conflicts are infrequent
- If conflicts are frequent, performance may degrade due to retries
- The version number ensures data consistency without locks

---

**Next:** Compare-and-Swap (CAS) operation - the CPU-level equivalent of optimistic concurrency control!