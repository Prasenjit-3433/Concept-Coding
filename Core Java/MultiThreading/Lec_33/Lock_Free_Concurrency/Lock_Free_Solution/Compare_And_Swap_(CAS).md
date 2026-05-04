# Compare-and-Swap (CAS) Operation

### What is CAS?

**Compare-and-Swap (CAS)** is a **low-level atomic operation** supported by modern CPUs. It's the CPU-level equivalent of optimistic concurrency control.

**Key Point:** CAS is inspired by (or inspires) optimistic locking, but it operates at the **CPU level**, not the database level.

---

### CAS Characteristics

1. **CPU-Level Operation:** Directly supported by modern processors
2. **Atomic:** Guaranteed to be atomic across all CPU cores (single, indivisible unit)
3. **Lock-Free:** No locks are involved
4. **Thread-Safe:** Multiple threads can safely use it without explicit synchronization

**Important:** Even if your CPU has 1, 2, 4, or more cores, and multiple threads are running in parallel, CAS remains atomic.

---

### CAS Operation: Three Parameters

CAS takes **three parameters**:

1. **Memory Location (M)** - Where the variable is stored
2. **Expected Value (E)** - What we expect the current value to be
3. **New Value (N)** - What we want to update it to

---

### How CAS Works: Three Steps
```
CAS(memory, expected, newValue)
```

**Step 1: Load/Read**
- Read the current value from the memory location

**Step 2: Compare**
- Compare the value in memory with the expected value
- If they match → proceed to Step 3
- If they don't match → operation fails (someone else changed it)

**Step 3: Swap/Update**
- If comparison succeeded, update memory with the new value
- Return success

---

### Visual Example
```
Memory M1: x = 10

CAS(M1, expected=10, newValue=12)

Step 1: Load from M1 → reads 10
Step 2: Compare: memory(10) == expected(10)? YES ✓
Step 3: Update M1 to 12

Result: x = 12 (Success!)
```

---

### CAS in Action: Scenario 1 (Success)
```
Initial State:
Memory M1: x = 10

Thread 1 calls: CAS(M1, 10, 12)

┌─────────────────────────────────┐
│ Step 1: Read from M1            │
│ Value read = 10                 │
└─────────────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Step 2: Compare                         │
│ Memory value (10) == Expected (10)?  │
│ YES ✓                                │
└──────────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 3: Update                     │
│ M1 = 12                            │
│ Return: SUCCESS                    │  
└─────────────────────────────────┘ 

Final State:
Memory M1: x = 12
```

---

### CAS in Action: Scenario 2 (Failure - Value Changed)
```
Initial State:
Memory M1: x = 10

Thread 1 reads x = 10, expects to change 10 → 13

Meanwhile, another thread changes M1 from 10 → 15

Thread 1 calls: CAS(M1, 10, 13)

┌─────────────────────────────────┐
│ Step 1: Read from M1            │
│ Value read = 15 (changed!)      │
└─────────────────────────────────┘
           ↓
┌───────────────────────────────────────┐
│ Step 2: Compare                       |
│ Memory value (15) == Expected (10)?   │
│ NO ✗                                  │
└───────────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 3: Fail                    │
│ Do NOT update                   │
│ Return: FAILURE                 │
└─────────────────────────────────┘

Final State:
Memory M1: x = 15 (unchanged by Thread 1)
Thread 1 must retry with new expected value
```

---

### Comparison: Optimistic Locking vs CAS

| Aspect | Optimistic Locking | CAS |
|--------|-------------------|-----|
| **Level** | Database level | CPU level |
| **Mechanism** | Row versioning | Memory comparison |
| **Version tracking** | Explicit (row_version column) | Can be implicit or explicit |
| **Operation** | SQL UPDATE with WHERE clause | Native CPU instruction |
| **Lock-free** | Yes | Yes |
| **Retry on conflict** | Yes | Yes |

---

### Why is it Called "Compare-and-Swap"?

1. **Compare:** Check if memory value matches expected value
2. **Swap:** If matched, swap (update) memory with new value

**Alternative names:**
- Compare-and-Set
- Compare-and-Exchange (CMPXCHG in x86 assembly)

---

### CAS Atomicity Guarantee
```
Multi-core CPU:
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  Core 1  │  │  Core 2  │  │  Core 3  │  │  Core 4  │
│ Thread 1 │  │ Thread 2 │  │ Thread 3 │  │ Thread 4 │
└──────────┘  └──────────┘  └──────────┘  └──────────┘
      ↓             ↓             ↓             ↓
      └─────────────┴─────────────┴─────────────┘
                       ↓
            ┌──────────────────────┐
            │    CAS Operation     │
            │   (ATOMIC - Only     │
            │   one thread at a    │
            │   time can execute)  │
            └──────────────────────┘
```

**Guarantee:** Even with multiple cores and threads, CAS executes as a single, indivisible operation.

---

### Key Takeaways

1. **CAS is a CPU operation** - Low-level, hardware-supported
2. **Lock-free and atomic** - No locks needed, guaranteed thread-safe
3. **Three-step process** - Load → Compare → Swap
4. **Fails gracefully** - Returns false if value changed, allowing retry
5. **Foundation for atomic classes** - Java's `AtomicInteger`, `AtomicBoolean`, etc., use CAS internally
6. **Similar to optimistic locking** - Same concept, different levels (CPU vs DB)

---

## The ABA Problem

### What is the ABA Problem?

The **ABA problem** is a subtle issue that can occur with CAS operations when a value changes from A → B → A, making it appear unchanged even though it was modified.

---

### ABA Problem Example
```
Initial State:
Memory M1: x = 10 (Version 1)

Thread 1 reads: x = 10
Expected: 10
New Value: 13

--- Meanwhile, before Thread 1 executes CAS ---

Another thread changes:
10 → 12 (some operation)
12 → 10 (changes back!)

Now Thread 1 executes: CAS(M1, 10, 13)

┌─────────────────────────────────┐
│ Step 1: Read from M1            │
│ Value read = 10                 │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 2: Compare                 │
│ Memory value (10) == Expected (10)? │
│ YES ✓                           │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 3: Update                  │
│ M1 = 13                         │
│ Return: SUCCESS                 │
└─────────────────────────────────┘

Problem: Thread 1 thinks nothing changed, but the value 
actually went through 10 → 12 → 10!
```

---

### Why is This a Problem?

- Thread 1 expected the value to be the **original 10** (Version 1)
- But it's actually a **different 10** (Version 3) - the value was modified twice
- CAS succeeded because the values matched, but the **history is lost**

---

### Solution: Add Version/Timestamp

To solve the ABA problem, attach a **version number** or **timestamp** to the value:
```
Memory M1: 
Value: 10
Version: 1

Thread 1 reads:
Value: 10
Version: 1
Expected: (10, Version 1)

--- Meanwhile ---

Another thread changes:
10 (V1) → 12 (V2) → 10 (V3)

Now Thread 1 executes: CAS(M1, expected=(10, V1), new=(13, V4))

┌─────────────────────────────────┐
│ Step 1: Read from M1            │
│ Value: 10, Version: 3           │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 2: Compare                 │
│ Value matches: 10 == 10 ✓       │
│ Version matches: 3 == 1? NO ✗   │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Step 3: Fail                    │
│ Do NOT update                   │
│ Return: FAILURE                 │
└─────────────────────────────────┘

Result: CAS fails because version mismatch detected!
Thread 1 knows the value was modified and can retry.
```

---

### Version-Based CAS
```
CAS with version:
CAS(memory, expectedValue, expectedVersion, newValue, newVersion)

If (memory.value == expectedValue AND memory.version == expectedVersion):
    memory.value = newValue
    memory.version = newVersion
    return SUCCESS
else:
    return FAILURE
```

---

### Visual Timeline of ABA Problem
```
Time    Thread 1           Other Threads       Memory (Value, Version)
─────────────────────────────────────────────────────────────────────
T0      Read: (10, V1)                        (10, V1)
T1                         Change 10→12        (12, V2)
T2                         Change 12→10        (10, V3)
T3      CAS(10,V1 → 13,V4)                    
        ✗ FAILS                                (10, V3) - Unchanged
        Version mismatch!

Without version, CAS would succeed incorrectly!
```

---

### Real-World Analogy

Imagine you have $10 in your wallet (Version 1):

1. You check: "I have $10"
2. While you're not looking:
    - Someone takes $10 and puts $12 (now you have $12)
    - Then takes $12 and puts $10 back (now you have $10 again)
3. You look again: "Still $10, nothing changed!"

**Problem:** You think nothing happened, but your money was actually borrowed and returned!

**With version tracking:**
- Original: $10 (Version 1)
- After changes: $10 (Version 3)
- You detect: "Same amount, but different version - something happened!"

---

### How Java Handles This

Java provides `AtomicStampedReference` to solve the ABA problem:
```java
AtomicStampedReference<Integer> atomicRef = 
    new AtomicStampedReference<>(10, 1); // value=10, stamp=1

int[] stampHolder = new int[1];
Integer value = atomicRef.get(stampHolder);
int stamp = stampHolder[0];

// CAS with stamp checking
boolean success = atomicRef.compareAndSet(
    10,    // expected value
    13,    // new value
    1,     // expected stamp
    2      // new stamp
);
```

---

### Key Takeaways

1. **ABA Problem:** Value changes A → B → A, appearing unchanged
2. **Detection:** Add version/stamp to detect intermediate changes
3. **Solution:** Version-based CAS tracks value history
4. **Similarity to Optimistic Locking:** Both use versioning for conflict detection
5. **Java Support:** `AtomicStampedReference` solves ABA problem
6. **When it matters:** Critical in data structures like lock-free stacks/queues