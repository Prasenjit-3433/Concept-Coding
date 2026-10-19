# Lecture 1: Introduction, Complexity Analysis & Asymptotic Notations

Date: August 10, 2026
Status: Done

## Quick Recap Before We Begin

This is the first lecture of the course, so there is nothing to recall yet. Instead, here is where we're headed. Later units cover arrays, linked lists, stacks and queues, and every one of them comes with a cost: this operation is O(1), that one is O(n), this structure wastes memory, that one doesn't. This lecture builds the **vocabulary and tools** to read, write, and compare those costs. Without it, "this is faster" is just an opinion. With it, it's a measurable claim.

We'll cover, in order: the basic terminology of data, what a data structure and an algorithm are, how to measure **time** and **space**, the three asymptotic notations (**Big O, Omega, Theta**), the **time-space trade-off**, and a first look at the **basic data structures** the rest of the course builds.

---

## 1. Basic Concepts and Notations

Before data *structures*, we need to agree on what "data" itself means. These terms are used loosely in everyday speech but precisely in this course.

| Term | Meaning | Example |
| --- | --- | --- |
| **Data** | raw, unprocessed values | `45`, `"Riya"`, `'A'` |
| **Information** | data that has been processed to carry meaning | "Riya scored 45 out of 50" |
| **Data item / field** | the smallest named unit of data | `name`, `rollNo`, `marks` |
| **Record** | a collection of related fields about one entity | one student's `{rollNo, name, marks}` |
| **File** | a collection of related records | the whole class's results sheet |
| **Entity** | a thing about which data is stored | a student, a book, a bank account |
| **Attribute** | a property of an entity | a student's name, a book's price |

**Real-life analogy:** Think of a **school register**. A single cell (the number `45`) is a *data item*. One student's full row (roll number, name, marks) is a *record*. The entire register is the *file*. The raw number `45` is just data. Reading "Riya scored 45/50, top of the class" turns it into information.

```
FILE (the whole register)
┌────────┬──────────┬───────┐
│ rollNo │   name   │ marks │   ← field names (attributes)
├────────┼──────────┼───────┤
│   1    │  Riya    │  45   │   ← RECORD 1
├────────┼──────────┼───────┤
│   2    │  Arjun   │  38   │   ← RECORD 2
├────────┼──────────┼───────┤
│   3    │  Meera   │  41   │   ← RECORD 3
└────────┴──────────┴───────┘
              ↑
        one cell = one data item
```

---

## 2. What is a Data Structure?

A **data structure** is a way of **organizing, storing, and managing data in memory** so that it can be used efficiently, meaning the operations we care about (searching, inserting, deleting, sorting) are fast and use reasonable memory.

**Real-life analogy:** Consider the same pile of 500 books stored three ways. Thrown in a heap on the floor, adding a book is instant, but finding one means checking every book. Arranged alphabetically on a shelf, finding is fast, but inserting a new book means shifting many others. Kept in a library catalog with numbered slots, finding is fast *and* you can add books without moving the rest, at the cost of maintaining the catalog. Same data, three organizations, three different cost profiles. **That is the whole point of studying data structures: the choice of organization decides which operations are cheap and which are expensive.**

### Classification of Data Structures

```
                         Data Structures
                               │
              ┌────────────────┴────────────────┐
         Primitive                         Non-Primitive
      (built into language)             (built from primitives)
      int, float, char, bool                     │
                                  ┌──────────────┴──────────────┐
                              Linear                       Non-Linear
                       (elements in a sequence)      (elements in a hierarchy
                                  │                      or network)
                  ┌───────────────┼──────────────┐                 │
               Array        Linked List     Stack / Queue     ┌────┴────┐
                                                            Tree       Graph
```

| Classification | Meaning | Examples |
| --- | --- | --- |
| **Primitive** | basic types provided directly by the language | `int`, `float`, `char`, `bool` |
| **Non-primitive** | complex types constructed from primitives | arrays, lists, trees, graphs |
| **Linear** | each element has one predecessor and one successor, in a single sequence | array, linked list, stack, queue |
| **Non-linear** | an element can connect to several others, with no single sequence | tree, graph |
| **Static** | size fixed at creation | plain array |
| **Dynamic** | size can grow/shrink at runtime | linked list, `vector` |

This course (Units I-III) focuses on the **linear** structures: arrays, linked lists, stacks, queues, and deques.

### Operations Performed on Data Structures

Regardless of which structure we use, we keep coming back to the same handful of operations:

| Operation | What it does |
| --- | --- |
| **Traversal** | visit every element exactly once |
| **Searching** | find the location of a given value |
| **Insertion** | add a new element |
| **Deletion** | remove an existing element |
| **Sorting** | arrange elements in a defined order |
| **Merging** | combine two (usually sorted) collections into one |

⚠️ These six verbs are the syllabus in disguise. Unit I (arrays) does them on contiguous memory, Unit II on pointer-linked nodes, Unit III under restricted access. **Each structure is judged by how cheaply it performs these same operations**, which is exactly what complexity analysis measures.

### Abstract Data Type (ADT)

An **ADT** describes **what** a structure does (its operations and their behavior) without saying **how** it is implemented.

**Real-life analogy:** The steering wheel, pedals, and gear lever are the *interface* of a car. You can drive without knowing whether the engine is petrol or electric. A **stack** is an ADT: it promises `push`, `pop`, `top`, `size` with LIFO behavior. Whether it's built on an array or a linked list is an implementation detail. We'll see exactly this in Unit III, where one ADT gets two different implementations.

---

## 3. What is an Algorithm?

An **algorithm** is a finite, step-by-step procedure that takes some input and produces a desired output.

**Real-life analogy:** A **recipe**. It lists ingredients (input), precise steps in order, and ends with a finished dish (output). A vague recipe like "cook until it feels right" isn't an algorithm.

A valid algorithm must satisfy:

| Property | Meaning |
| --- | --- |
| **Input** | zero or more well-defined inputs |
| **Output** | at least one result is produced |
| **Definiteness** | every step is clear and unambiguous |
| **Finiteness** | it terminates after a finite number of steps |
| **Effectiveness** | every step is basic enough to actually be carried out |

⚠️ **Finiteness is the one students forget.** A loop like `while (true) { ... }` with no exit is a *procedure*, but not an *algorithm*.

**Data structures and algorithms are two halves of one idea:** the data structure decides *how the data sits in memory*, and the algorithm decides *what we do with it*. The best algorithm on a badly chosen structure is still slow.

---

## 4. Why Analyze Algorithms? Why Not Just Run Them and Time Them?

Suppose two students write a search function. The obvious test is to run both and see which finishes first. That is unreliable:

- Results depend on the **machine** (a laptop vs. a server).
- They depend on the **language and compiler**.
- They depend on **what else the computer is doing** at that moment.
- They depend on the **particular input** chosen for the test.

**Real-life analogy:** Comparing two routes to the airport by driving each once. One day there's a jam, the other day there isn't. What you really want is a measure of *how the route's length scales*, independent of today's traffic.

So we need a **machine-independent** measure. Instead of seconds, we count **how many basic steps the algorithm performs as a function of the input size `n`**. That function is the algorithm's **time complexity**, and what matters most is how it **grows** as `n` grows.

---

## 5. Time Complexity

**Time complexity** is the number of basic operations an algorithm performs, expressed as a function of the input size `n`.

### Counting Steps

Let's count the steps of a simple function that sums an array:

```cpp
int sumArray(int arr[], int n) {
    int sum = 0;                   // 1 step
    for (int i = 0; i < n; i++) {  // i=0: 1 step | i<n checks: n+1 | i++: n
        sum += arr[i];              // n steps
    }
    return sum;                     // 1 step
}
```

```
Total steps ≈ 1 + 1 + (n + 1) + n + n + 1
            = 3n + 4
```

So `T(n) = 3n + 4`. Is the exact `+4` or the `3` important? Not really. If `n` is a million, `3n + 4` is 3,000,004, and the `+4` is invisible. The `3` depends on how we choose to count steps. What survives on every machine and in every language is that **the work grows linearly with `n`**. That is the central idea behind asymptotic notation (Section 8): keep the *shape* of the growth, throw away the noise.

### Best, Average, and Worst Case

The same algorithm can behave differently on different inputs of the *same size*. Consider **linear search** for a value `key` in an array of `n` elements:

```cpp
int linearSearch(int arr[], int n, int key) {
    for (int i = 0; i < n; i++) {
        if (arr[i] == key) return i;   // found
    }
    return -1;                          // not found
}
```

```
arr = [ 7 | 3 | 9 | 5 | 2 ]      n = 5

Best case:   key = 7  → found at index 0        → 1 comparison
Worst case:  key = 2  (last) or key absent      → 5 comparisons  (n)
Average:     key equally likely at any position → about (n+1)/2 comparisons
```

| Case | Meaning | Linear search |
| --- | --- | --- |
| **Best case** | the input that makes the algorithm do the *least* work | O(1) |
| **Average case** | expected work over all possible inputs | O(n) |
| **Worst case** | the input that makes the algorithm do the *most* work | O(n) |

**Real-life analogy:** Looking for your house keys. Best case, they're in the first pocket you check. Worst case, they're in the last place you look, or lost entirely, so you check everywhere.

⚠️ **Why the worst case gets the most attention:** it's a **guarantee**. "This will never take longer than X" is a promise you can build a system on, while the best case is an optimistic accident. Unless a question says otherwise, "time complexity" in exams and interviews means the **worst case**.

---

## 6. Space Complexity

**Space complexity** is the amount of memory an algorithm needs, as a function of input size `n`.

It has two parts:

```
Space complexity = Input space + Auxiliary space
                      │                │
          memory taken by the     extra memory the algorithm
          input itself            needs while running
                                  (temporary variables, extra
                                   arrays, recursion stack)
```

**Auxiliary space** is usually what people mean when they compare algorithms, since the input has to exist regardless.

```cpp
// O(1) auxiliary space: only a fixed number of variables
int sumArray(int arr[], int n) {
    int sum = 0;
    for (int i = 0; i < n; i++) sum += arr[i];
    return sum;
}

// O(n) auxiliary space: builds a whole new array of size n
void copyArray(int arr[], int n) {
    int* copy = new int[n];
    for (int i = 0; i < n; i++) copy[i] = arr[i];
    delete[] copy;
}
```

### The Hidden Cost: Recursion Uses Space Too

Every recursive call sits on the **call stack** until it returns:

```cpp
int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}
```

```
factorial(4)
  └─ factorial(3)
       └─ factorial(2)
            └─ factorial(1)      ← 4 frames alive at the deepest point
                                     (call stack depth = n)

Time:  O(n)  (n calls, constant work each)
Space: O(n)  (n stack frames waiting to return)
```

⚠️ **A loop-based factorial uses O(1) space; the recursive one uses O(n).** Same time, different space. This is easy to miss, because the recursive version *looks* like it has no extra variables.

---

## 7. Growth Rates: Seeing Why "Shape" Matters

Here is how common growth functions compare as `n` increases:

| `n` | `1` | `log n` | `n` | `n log n` | `n²` | `n³` | `2ⁿ` |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 10 | 1 | ~3 | 10 | ~33 | 100 | 1,000 | 1,024 |
| 100 | 1 | ~7 | 100 | ~664 | 10,000 | 1,000,000 | ~10³⁰ |
| 1000 | 1 | ~10 | 1000 | ~10,000 | 1,000,000 | 10⁹ | ~10³⁰¹ |

![time_complexity.jpg](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/time_complexity.jpg)

The ordering from best to worst, which you should memorize:

```
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ) < O(n!)
constant  logarithmic  linear  linearithmic  quadratic  cubic  exponential  factorial
```

| Class | Typical example |
| --- | --- |
| **O(1)** | accessing `arr[i]`, push on a stack |
| **O(log n)** | binary search (halving the problem each step) |
| **O(n)** | traversing an array or linked list |
| **O(n log n)** | efficient sorting (merge sort) |
| **O(n²)** | simple sorts (bubble, selection, insertion), nested loops |
| **O(2ⁿ)** | naive recursive Fibonacci, trying all subsets |

**Real-life analogy:** Finding a name in a phone book of 1,000,000 entries. **O(n)**: start at page one and read every name (up to a million checks). **O(log n)**: open the middle, decide which half the name is in, and repeat. About **20 checks** instead of a million. Same book, same goal, and the choice of *method* is the difference between lunchtime and next week.

⚠️ **Why a faster computer doesn't rescue a bad algorithm:** if `n²` is too slow, a computer 100x faster only lets you handle roughly 10x more input. If `2ⁿ` is too slow, a computer 1000x faster adds about 10 to `n`. Growth rate beats hardware, which is why we analyze growth rate in the first place.

---

## 8. Asymptotic Notations: Big O, Omega, Theta

We now formalize "keep the shape, drop the noise". **Asymptotic notation** describes how a function behaves **for large `n`**, ignoring constant factors and lower-order terms. There are three notations, each answering a different question:

| Notation | Reads as | Gives | Analogy |
| --- | --- | --- | --- |
| **Big O**: `O(g(n))` | "at most" | an **upper bound** | "the delivery will take **at most** 5 days" |
| **Omega**: `Ω(g(n))` | "at least" | a **lower bound** | "it will take **at least** 2 days" |
| **Theta**: `Θ(g(n))` | "exactly about" | a **tight bound** (both) | "it will take **about** 3 days, neither much less nor much more" |

### 8.1 Big O Notation (Upper Bound)

**Definition:** `f(n) = O(g(n))` if there exist positive constants `c` and `n₀` such that

```
f(n) ≤ c · g(n)    for all n ≥ n₀
```

In words: beyond some point `n₀`, `f(n)` never exceeds a constant multiple of `g(n)`.

![upper_lower.webp](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/upper_lower.webp)

**Worked example:** Show that `f(n) = 3n + 2` is `O(n)`.

```
We need c and n₀ such that   3n + 2 ≤ c·n   for all n ≥ n₀.

Try c = 4:
    3n + 2 ≤ 4n
         2 ≤ n          ← true whenever n ≥ 2

So with c = 4 and n₀ = 2, the inequality holds.   ∴  3n + 2 = O(n)  ✅
```

**Counter-example:** Is `n² = O(n)`? We'd need `n² ≤ c·n`, which simplifies to `n ≤ c`. But `c` is a *fixed constant* and `n` grows without limit, so this fails for large `n`. **No such `c` exists, so `n² ≠ O(n)`.**

### 8.2 Omega Notation (Lower Bound)

**Definition:** `f(n) = Ω(g(n))` if there exist positive constants `c` and `n₀` such that

```
f(n) ≥ c · g(n)    for all n ≥ n₀
```

**Worked example:** Show `3n + 2 = Ω(n)`.

```
We need   3n + 2 ≥ c·n   for all n ≥ n₀.

Try c = 3:
    3n + 2 ≥ 3n     ← always true (since 2 > 0), for all n ≥ 1

So with c = 3 and n₀ = 1:   3n + 2 = Ω(n)  ✅
```

### 8.3 Theta Notation (Tight Bound)

**Definition:** `f(n) = Θ(g(n))` if there exist positive constants `c₁`, `c₂`, and `n₀` such that

```
c₁ · g(n) ≤ f(n) ≤ c₂ · g(n)    for all n ≥ n₀
```

`f(n)` is **sandwiched** between two scaled copies of `g(n)`. Equivalently: `f(n) = Θ(g(n))` exactly when `f(n)` is **both** `O(g(n))` **and** `Ω(g(n))`.

```
 steps
   ▲
   │                       c₂·g(n)    ← upper fence
   │                     ╱
   │                   ╱  ╱ f(n)      ← f(n) lives
   │                 ╱  ╱ ╱              between the fences
   │               ╱ ╱ ╱
   │             ╱╱ ╱╱
   │           ╱╱ ╱╱  c₁·g(n)         ← lower fence
   │ ──────╱──┼──────────────────────────▶ n
   │          n₀
```

**Worked example:** `3n + 2 = Θ(n)`, because we showed `3n ≤ 3n + 2 ≤ 4n` for all `n ≥ 2`. That gives `c₁ = 3`, `c₂ = 4`, `n₀ = 2`. ✅

### Putting the Three Side by Side

|  | Big O | Omega | Theta |
| --- | --- | --- | --- |
| Symbol | `O(g(n))` | `Ω(g(n))` | `Θ(g(n))` |
| Bound | upper (≤) | lower (≥) | tight (both) |
| Meaning | grows **no faster** than `g(n)` | grows **no slower** than `g(n)` | grows **at the same rate** as `g(n)` |
| Constants | one `c` | one `c` | two: `c₁` and `c₂` |

⚠️ **A very common misconception: "Big O means worst case, Omega means best case."** That is *not* what the notations mean. The notations describe **bounds on a function**. The best/worst/average cases describe **which input** you're analyzing. You can apply any notation to any case. Linear search's *worst case* is `Θ(n)`, and its *best case* is `Θ(1)`. In practice, "O(...)" is used for worst-case guarantees, which is why the two ideas get blurred.

⚠️ **Loose but true:** since `3n + 2 = O(n)`, it's also technically `O(n²)` and `O(n³)`, because those are looser upper bounds. That's valid but useless. **Always state the tightest bound you can.**

### Simplification Rules

To read a complexity straight off a formula:

1. **Drop constant factors.** `5n` → `O(n)`
2. **Drop lower-order terms.** `n² + n + 1` → `O(n²)`, because for large `n` the `n²` term dominates completely.
3. **Different inputs, different variables.** Looping over an array of size `n` and then one of size `m` is `O(n + m)`, **not** `O(n)`.

```
Example:   T(n) = 4n³ + 10n² + 7n + 100

   Step 1: keep only the fastest-growing term  →  4n³
   Step 2: drop its constant                   →  n³
   Result: O(n³)
```

---

## 9. Reading Complexity Straight from Code

Rather than counting every step, we recognize **patterns**.

### Pattern 1: A single loop → O(n)

```cpp
for (int i = 0; i < n; i++) {
    // O(1) work
}
```

### Pattern 2: Consecutive loops → add, keep the biggest

```cpp
for (int i = 0; i < n; i++) { /* O(1) */ }   // O(n)
for (int j = 0; j < n; j++) { /* O(1) */ }   // O(n)
// total: O(n) + O(n) = O(2n) = O(n)
```

### Pattern 3: Nested loops → multiply

```cpp
for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) {
        // O(1) work
    }
}
// n iterations × n iterations = O(n²)
```

### Pattern 4: Dependent inner loop → sum a series

```cpp
for (int i = 0; i < n; i++) {
    for (int j = 0; j < i; j++) {
        // O(1) work
    }
}
```

```
i = 0 → inner runs 0 times
i = 1 → inner runs 1 time
i = 2 → inner runs 2 times
 ...
i = n-1 → inner runs n-1 times

Total = 0 + 1 + 2 + ... + (n-1) = n(n-1)/2   →   O(n²)
```

⚠️ The inner loop doesn't always run `n` times, but here the total is still quadratic. The `1/2` is a constant, so it gets dropped.

### Pattern 5: Variable multiplied/divided each step → O(log n)

```cpp
for (int i = 1; i < n; i = i * 2) {
    // O(1) work
}
```

```
n = 16:   i takes values  1 → 2 → 4 → 8 → (16 stops)
                          4 iterations = log₂(16)

Each step DOUBLES i, so we reach n after about log₂(n) steps.
```

**Real-life analogy:** Folding a sheet of paper in half repeatedly. A thousand-page stack needs only about **10 folds** to get down to a single page, because the problem halves each time.

### Pattern 6: Outer linear, inner logarithmic → O(n log n)

```cpp
for (int i = 0; i < n; i++) {
    for (int j = 1; j < n; j = j * 2) {
        // O(1) work
    }
}
// n × log n = O(n log n)
```

### Pattern 7: Loop until `i*i > n` → O(√n)

```cpp
for (int i = 1; i * i <= n; i++) {
    // O(1) work
}
// runs while i ≤ √n  →  O(√n)
```

### Quick Reference

| Code shape | Complexity |
| --- | --- |
| No loop, fixed number of steps | O(1) |
| `i++` up to `n` | O(n) |
| `i *= 2` or `i /= 2` up to `n` | O(log n) |
| `i++` up to `√n` | O(√n) |
| two nested `n`-loops | O(n²) |
| `n`-loop containing a doubling loop | O(n log n) |
| loops one after another | add them, keep the largest |

⚠️ **Don't just count loops. Ask how many times each one runs.** Two nested loops are not automatically `O(n²)`. `for i < n` containing `for j < 5` is just `O(5n) = O(n)`.

---

## 10. Time-Space Trade-off

In many problems, you can make an algorithm **faster by using more memory**, or **leaner by accepting more time**. You rarely get both for free. This is the **time-space trade-off**, and it will keep appearing in every unit of this course.

**Real-life analogy:** Preparing for an exam. You can **memorize a cheat sheet** of every formula (uses more space on a page, answers instantly during the exam), or **re-derive each formula** from first principles (no sheet needed, but each answer takes much longer). More storage buys speed. Less storage costs time.

### Example 1: Computing Fibonacci Numbers

```cpp
// Version A: pure recursion → fast to write, painfully slow to run
int fibSlow(int n) {
    if (n <= 1) return n;
    return fibSlow(n - 1) + fibSlow(n - 2);
}
// Time:  O(2ⁿ)   (recomputes the same values again and again)
// Space: O(n)    (recursion depth)
```

```
fibSlow(5) recomputes the same subproblems repeatedly:

                    fib(5)
                  ╱        ╲
             fib(4)         fib(3)
            ╱     ╲         ╱    ╲
        fib(3)   fib(2)  fib(2)  fib(1)
        ╱   ╲    ╱  ╲    ╱  ╲
    fib(2) fib(1) ...   ...
      ↑ fib(3) is computed TWICE, fib(2) THREE times.
        This waste explodes as n grows.
```

```cpp
// Version B: spend O(n) extra memory to remember answers
int fibMemo(int n, int memo[]) {      // memo[] pre-filled with -1
    if (n <= 1) return n;
    if (memo[n] != -1) return memo[n];            // already known → reuse!
    memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    return memo[n];
}
// Time:  O(n)    (each value computed once)
// Space: O(n)    (the memo array + recursion depth)
```

```cpp
// Version C: keep only the last two values
int fibLean(int n) {
    if (n <= 1) return n;
    int prev = 0, curr = 1;
    for (int i = 2; i <= n; i++) {
        int next = prev + curr;
        prev = curr;
        curr = next;
    }
    return curr;
}
// Time:  O(n)
// Space: O(1)    (only three variables)
```

| Version | Time | Space | Idea |
| --- | --- | --- | --- |
| A: plain recursion | O(2ⁿ) | O(n) | recompute everything |
| B: with memo | O(n) | O(n) | **spend memory** to avoid recomputation |
| C: two variables | O(n) | O(1) | remember only what's needed |

Going from A to B is the textbook trade-off: **extra memory bought an exponential speedup**. C shows that sometimes a smarter observation gives you both.

### Example 2: Lookup Table

To check whether a number from `0` to `9999` has been seen before, you could scan a list each time (**O(n)** time, small space), or keep a `bool seen[10000]` array and check `seen[x]` (**O(1)** time, but 10,000 slots reserved up front). That is memory spent to buy speed.

### Where This Trade-off Appears Later in the Course

| Unit | The trade-off |
| --- | --- |
| Arrays vs Linked Lists | arrays: fast access, fixed wasted space. Linked lists: flexible size, extra pointer per node, slow access |
| Singly vs Doubly Linked List | doubly spends an extra pointer per node to buy backward movement and O(1) deletion of a known node |
| Stack/Queue via array vs list | array: fixed capacity, no pointers. List: exact fit, pointer overhead |

**Every data structure in this course is a different answer to the same question: what are you willing to pay (memory, extra pointers, extra bookkeeping) to make the operations you care about cheap?**

---

## 11. Basic Data Structures: A First Look

Here is a quick tour of the structures you'll meet, each with a one-line analogy. Details come in the lectures that follow.

| Structure | What it is | Analogy | Where it appears |
| --- | --- | --- | --- |
| **Array** | elements stored in one contiguous block, accessed by index | a row of numbered parking slots | Unit I (next lecture) |
| **Linked List** | nodes scattered in memory, each pointing to the next | a treasure hunt, each clue pointing to the next | Unit II |
| **Stack** | access only at one end (LIFO) | a stack of plates | Unit III |
| **Queue** | add at the back, remove from the front (FIFO) | a ticket-counter line | Unit III |
| **Priority Queue** | always serves the highest-priority element | an emergency room | Unit III |
| **Deque** | add/remove at both ends | a line where people can join or leave at either end | Unit III |
| **Tree** | hierarchy of parent-child nodes | a family tree or company org chart | later courses |
| **Graph** | nodes connected by arbitrary links | a road map or social network | later courses |
| **Hash Table** | maps keys to slots for near-instant lookup | a locker room with numbered lockers | later courses |

#### First look at `Array`:

![array.png](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/array.png)

#### First look at `LinkedList`:

![Linked-list.webp](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/Linked-list.webp)

#### First look at `Stack`:

![stack.drawio2.png](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/stack.drawio2.png)

#### First look at `Queue`:

![Dequeue-Operation-in-Queue-1.webp](Lecture%201%20Introduction,%20Complexity%20Analysis%20&%20Asym/Dequeue-Operation-in-Queue-1.webp)

### Quick Complexity Preview

This table is a preview, not something to memorize yet. You'll derive every cell as the course progresses.

| Operation | Array | Linked List | Stack | Queue |
| --- | --- | --- | --- | --- |
| Access by index | O(1) | O(n) | n/a | n/a |
| Search by value | O(n) | O(n) | n/a | n/a |
| Insert at beginning | O(n) | O(1) | O(1) (push) | n/a |
| Insert at end | O(1)* | O(n) (O(1) with a tail pointer) | n/a | O(1) |
| Delete at beginning | O(n) | O(1) | O(1) (pop) | O(1) |
- Array insertion at the end is O(1) only if there's spare capacity.

⚠️ Notice there's **no row where one structure wins everywhere**. That is the whole reason we study several of them. There's no universally best data structure, only the best *fit* for the operations your problem needs most.

---

## 12. Quick Recap Table

| Concept | Meaning | Key point |
| --- | --- | --- |
| Data / Information | raw values vs. processed, meaningful values | information = data + context |
| Field → Record → File | smallest unit → group of fields → group of records | the school register |
| Data Structure | a way of organizing data in memory | choice decides which operations are cheap |
| Linear vs Non-linear | sequence vs. hierarchy/network | this course covers linear |
| ADT | *what* operations exist, not *how* they're built | stack = one ADT, two implementations |
| Algorithm | finite, step-by-step procedure with input and output | must terminate (finiteness) |
| Time complexity | number of basic steps as a function of `n` | machine-independent |
| Space complexity | input space + auxiliary space | recursion stack counts |
| Best / Average / Worst | which *input* is analyzed | worst case = the guarantee |
| **Big O** `O(g)` | upper bound, `f ≤ c·g` | "at most" |
| **Omega** `Ω(g)` | lower bound, `f ≥ c·g` | "at least" |
| **Theta** `Θ(g)` | tight bound, `c₁·g ≤ f ≤ c₂·g` | "exactly about" |
| Simplification | drop constants, drop lower-order terms | `4n³ + 10n² + 7` → `O(n³)` |
| Growth order | `1 < log n < n < n log n < n² < n³ < 2ⁿ < n!` | growth rate beats hardware |
| Loop patterns | single → `n`, nested → multiply, doubling → `log n` | count how often each loop *actually* runs |
| Time-space trade-off | spend memory to save time, or vice versa | Fibonacci: O(2ⁿ) → O(n) with a memo |

**The single biggest idea to carry out of this lecture:** we don't measure an algorithm in seconds, we measure **how its cost grows with input size**, and we describe that growth with Big O, Omega, and Theta. Every structure and algorithm from here on is judged by that yardstick, and every one of them is a trade-off between time and space.

---