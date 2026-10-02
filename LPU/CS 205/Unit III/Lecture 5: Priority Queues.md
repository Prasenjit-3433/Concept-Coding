# Lecture 5: Priority Queues

## 1. The Problem a Priority Queue Solves

A plain queue (Class 1) only ever answers one question: "who's been waiting longest?" But a lot of real problems need a different question answered: **"who/what matters most right now?"** — and "most important" has nothing to do with arrival order.

**Real-life analogy:** Think of a **hospital emergency room**. Patients don't get treated in the order they walked in — a patient with a heart attack who just arrived gets seen before someone with a sprained ankle who's been waiting for an hour. The "queue" here is ordered by **severity (priority)**, not by **arrival time (FIFO)**.

```
**Plain Queue (FIFO)**:                  **Priority Queue (by severity)**:
front                   back           highest-priority served first
  │                       │                      │
  ▼                       ▼                      ▼
┌──────┬──────┬──────┬──────┐       ┌─────────────┐
│Ankle │ Flu  │Fever │Heart │       │Heart Attack │ ← served next,
│(1st) │(2nd) │(3rd) │(4th) │       ├─────────────┤   regardless of
└──────┴──────┴──────┴──────┘       │   Fever     │   when it arrived
  served in arrival order           ├─────────────┤
                                    │    Flu      │
                                    ├─────────────┤
                                    │   Ankle     │
                                    └─────────────┘
```

Other classic motivating examples:

- **OS task scheduling** — a high-priority system process should get CPU time before a low-priority background task, even if the background task was queued earlier.
- **Dijkstra's shortest path algorithm** — always expand the *closest* unvisited node next, not the one discovered first.
- **"Find the K largest/smallest elements"** problems — a PQ naturally keeps track of "the most extreme values seen so far" as you scan through data.

---

## 2. What Exactly Is a `Priority Queue`?

A **priority queue** is an abstract data structure that supports the same core operations as a regular queue (`push`, `pop`, `top`, `size`) — but `pop`/`top` always act on the element with the **highest priority**, not the one that arrived first.

"Highest priority" is just a convention — it could mean "largest value" or "smallest value," depending on what you need. That's where **min-heap** and **max-heap** come in.

### Max-Heap (Max-Priority Queue)

The element that comes out first is always the **largest** value currently in the structure.

```
Insert: 5, 1, 9, 3, 7

Conceptual *"always largest on top"* view:
┌───┐
│ 9 │  ← top() returns 9, pop() removes 9
└───┘
  (1, 3, 5, 7 are "inside," exact internal arrangement
   is a heap structure — covered when we reach Trees)
```

### Min-Heap (Min-Priority Queue)

The element that comes out first is always the **smallest** value currently in the structure.

```
Insert: 5, 1, 9, 3, 7

Conceptual *"always smallest on top"* view:
┌───┐
│ 1 │  ← top() returns 1, pop() removes 1
└───┘
  (3, 5, 7, 9 are "inside")
```

⚠️ **What you need to know right now vs. later:** for this class, treat a heap as a **black box** that always hands you the max (or min) in O(1) via `top()`, and can insert/remove in O(log n). *How* it maintains that guarantee internally (the actual heap data structure, stored as a special kind of binary tree) is a Trees-unit topic. Using a PQ correctly doesn't require knowing that internal mechanism — exactly like you don't need to know how `std::vector` resizes internally to use it correctly.

---

## 3. Priority Queue in C++ STL — Declaration and Core Operations

C++'s STL gives you `std::priority_queue`, which is a **max-heap by default**.

```cpp
#include <queue>
using namespace std;

priority_queue<int> maxHeap;         // **max-heap (default)** — largest on top
priority_queue<int, vector<int>, greater<int>> minHeap;   // min-heap — smallest on top
```

⚠️ **That second template argument, `vector<int>`,** is the *underlying container* the heap is built on top of — you'll almost always just copy this exact pattern rather than reasoning about it, since changing the underlying container is a rare, advanced need.

### Core Operations

| Operation | What it does | Time Complexity |
| --- | --- | --- |
| `pq.push(x)` | inserts `x`, heap re-adjusts to maintain max/min-on-top property | **O(log n)** |
| `pq.pop()` | removes the top element (max or min, depending on heap type) | **O(log n)** |
| `pq.top()` | returns the top element **without removing it** | **O(1)** |
| `pq.size()` | returns the number of elements | **O(1)** |
| `pq.empty()` | returns `true` if the PQ has no elements | **O(1)** |

⚠️ **Why `push`/`pop` cost O(log n), but a plain stack/queue's cost O(1):** every insertion or removal has to **re-sort just enough** of the structure to keep "the max (or min) is always on top" true — and because the heap is a balanced tree-like shape internally, that re-adjustment only ever needs to travel the height of the tree, which is `log n` for `n` elements. This is the one new complexity idea in this class — everything in Classes 1–4 was O(1) or O(n); this is the first O(log n) structure you've seen.

**Max-Heap walkthrough** — `push(5), push(1), push(9), push(3)`, then `pop()` twice:

```
push(5): heap effectively holds {5}         top() = 5
push(1): heap effectively holds {5, 1}       top() = 5   (1 is "buried")
push(9): heap effectively holds {5, 1, 9}    top() = 9   (9 now on top)
push(3): heap effectively holds {5, 1, 9, 3} top() = 9   (3 is "buried")

pop():   removes 9                           top() = 5
pop():   removes 5                           top() = 3
```

**Min-Heap walkthrough** — same pushes, but on `priority_queue<int, vector<int>, greater<int>>`:

```
push(5): heap effectively holds {5}          top() = 5
push(1): heap effectively holds {5, 1}       top() = 1   (1 now on top — smallest)
push(9): heap effectively holds {5, 1, 9}    top() = 1
push(3): heap effectively holds {5, 1, 9, 3} top() = 1

pop():   removes 1                           top() = 3
pop():   removes 3                           top() = 5
```

⚠️ **Important distinction from a sorted array:** a PQ does **not** keep *all* its elements in fully sorted order internally — it only guarantees the **top** element is correctly the max (or min) at any moment. This is *exactly* why `push`/`pop` can be O(log n) instead of O(n) (which is what you'd pay to keep a plain array fully sorted on every insertion) — the heap only does the minimum work needed to keep the *top* correct, not the whole collection.

---

## 4. Space Complexity

A `priority_queue<int>` storing `n` elements uses **O(n)** space — same as any other container holding `n` values, with the underlying `vector` growing dynamically as needed (no fixed-capacity limitation, unlike Class 1's array-based stack/queue).

---

## 5. A Quick Checkpoint Before Part 2

|  | Plain Queue (Class 1) | Priority Queue |
| --- | --- | --- |
| Ordering rule | FIFO (arrival order) | By priority (value-based, customizable) |
| `push` | O(1) | O(log n) |
| `pop` / `top` | O(1) | `pop`: O(log n), `top`: O(1) |
| Default C++ behavior | — | Max-heap |
| Min-heap in C++ | — | `priority_queue<int, vector<int>, greater<int>>` |

---

## 6. The Min-Heap-via-Max-Heap Trick (Negation)

Before reaching for custom comparators, there's a simpler trick worth knowing first, because it shows up constantly in competitive problems: if you only have a max-heap available (or find the `greater<int>` syntax easy to forget), you can simulate a min-heap by **negating every value** going in and out.

**The logic:** push `-x` instead of `x`. The max-heap will always put the *largest* negated value on top — which is the *smallest* original value. Negate again when reading it back out.

```cpp
priority_queue<int> maxHeap;   // plain max-heap

maxHeap.push(-5);
maxHeap.push(-1);
maxHeap.push(-9);
maxHeap.push(-3);

int smallest = -maxHeap.top();   // top() = -1 (largest negated value)
                                   // smallest = -(-1) = 1  ✅ correct, 1 was the true minimum
```

**Walkthrough — pushing `5, 1, 9, 3` as negated values:**

```
push(-5):  heap holds {-5}                top() = -5  → original: 5
push(-1):  heap holds {-5, -1}            top() = -1  → original: 1  (largest negated = smallest original)
push(-9):  heap holds {-5, -1, -9}        top() = -1  → original: 1
push(-3):  heap holds {-5, -1, -9, -3}    top() = -1  → original: 1

Reading out smallest-first: 1, 3, 5, 9  ✅ correctly ascending
```

⚠️ **Why this trick exists and when to actually use it:** it's purely a convenience for quick competitive-style code where you don't want to type out the `greater<int>` template — but it **only works cleanly for plain numeric types**. The moment you need to order pairs, strings, or custom objects by some non-numeric rule, negation stops making sense, and that's where real comparators (Sections 7–9) take over. Don't force the negation trick onto non-numeric data — use the right tool for the job.

---

## 7. Priority Queue of Pairs — Default Behavior

C++ pairs compare **lexicographically** by default: compare `.first` first; only if `.first` is equal, compare `.second`. This default behavior carries directly into a `priority_queue<pair<int,int>>`.

```cpp
priority_queue<pair<int,int>> pq;   // max-heap of pairs, default comparison
```

**Walkthrough** — push `{1,5}, {3,2}, {3,8}, {2,9}`:

```
push({1,5}): heap holds {(1,5)}                   top() = (1,5)
push({3,2}): heap holds {(1,5),(3,2)}             top() = (3,2)   — 3 > 1, so (3,2) wins
push({3,8}): heap holds {(1,5),(3,2),(3,8)}       top() = (3,8)   — .first tied at 3,
                                                                  compare .second: 8 > 2

push({2,9}): heap holds {(1,5),(3,2),(3,8),(2,9)}  top() = (3,8) — still the largest                                                                       first

pop(): removes (3,8)           top() = (3,2)  — the other .first=3 pair
pop(): removes (3,2)           top() = (2,9)
```

This default "compare `.first`, break ties with `.second`" behavior is exactly why pairs are a popular way to pack "priority + payload" together — e.g., `{distance, nodeId}` in Dijkstra's algorithm, where you want to prioritize by `distance` but still carry `nodeId` along for free.

---

## 8. Writing a Custom Comparator — Three Ways

Sometimes the default ordering (or even the negation trick) isn't enough — you need a genuinely custom rule, e.g., "order pairs by `.second`, not `.first`," or "order strings by length, not alphabetically." C++ gives you three ways to plug in custom logic.

### Method 1 — A Comparator Struct (most common, most explicit)

```cpp
struct CompareBySecond {
    bool operator()(const pair<int,int>& a, const pair<int,int>& b) {
        return a.second < b.second;
        // returning TRUE means "a has LOWER priority than b"
        // i.e., whichever pair makes this return true for the current top
        // gets pushed further down — so the element with the LARGEST
        // .second ends up on top (this is still a max-heap by .second)
    }
};

priority_queue<pair<int,int>, vector<pair<int,int>>, CompareBySecond> pq;
```

⚠️ **The single most confusing part of custom comparators, explained plainly:** `operator()` is answering the question *"should `a` be considered lower priority than `b`?"* — **not** "should `a` come before `b` in the output." Returning `true` means `a` sinks below `b`. This is the exact reverse of how you'd write a normal sorting comparator for `sort()`, and it is the single biggest source of bugs when people first write custom PQ comparators.

**Walkthrough — pushing `{1,5}, {3,2}, {3,8}`, ordered by `.second`:**

```
push({1,5}): heap holds {(1,5)}                top() = (1,5)   [.second = 5]
push({3,2}): CompareBySecond((3,2),(1,5))? → 2 < 5 → TRUE → (3,2) is lower priority, stays below
             heap holds {(1,5),(3,2)}           top() = (1,5)   [.second = 5, still highest]
push({3,8}): CompareBySecond((1,5),(3,8))? → 5 < 8 → TRUE → (1,5) is now lower priority
             heap holds {(1,5),(3,2),(3,8)}     top() = (3,8)   [.second = 8, now highest]

Correctly ordering purely by .second, ignoring .first entirely.
```

### Method 2 — A Lambda (concise, useful for one-off local use)

```cpp
auto cmp = [](const pair<int,int>& a, const pair<int,int>& b) {
    return a.second < b.second;   // same logic as Method 1
};

priority_queue<pair<int,int>, vector<pair<int,int>>, decltype(cmp)> pq(cmp);
```

⚠️ **`decltype(cmp)` and the trailing `(cmp)` are both required** — unlike a struct (where the type itself carries the logic), a lambda's type is anonymous/compiler-generated, so you must both name its type via `decltype` *and* pass an actual instance of it (`(cmp)`) to the constructor so the PQ has a comparator object to call. Forgetting the `(cmp)` argument is a common compile error.

### Method 3 — `greater<>`/`less<>` for Simple Reversal (seen already in Part 1)

This is really just a special case you've already seen: `greater<int>` is itself a built-in comparator struct, functionally identical in spirit to Method 1, just pre-written by the standard library for the simple "reverse the default ordering" case.

---

## 9. Priority Queue of Custom Objects

The same `operator()` struct approach (Method 1) extends directly to custom classes/structs — this is the realistic version of what you'd actually write for, say, a `Task` with a priority level and a name.

```cpp
struct Task {
    string name;
    int priority;
};

struct CompareTask {
    bool operator()(const Task& a, const Task& b) {
        return a.priority < b.priority;   // higher .priority value = higher actual priority
    }
};

priority_queue<Task, vector<Task>, CompareTask> taskQueue;

taskQueue.push({"Fix critical bug", 9});
taskQueue.push({"Update docs", 2});
taskQueue.push({"Security patch", 10});
taskQueue.push({"Refactor tests", 5});
```

**Walkthrough:**

```
push({"Fix critical bug", 9}):    top() = ("Fix critical bug", 9)
push({"Update docs", 2}):          CompareTask(("Update docs",2), ("Fix...",9))? 2<9 → TRUE, sinks
                                     top() = ("Fix critical bug", 9)
push({"Security patch", 10}):      CompareTask(("Fix...",9), ("Security...",10))? 9<10 → TRUE, "Fix..." sinks
                                     top() = ("Security patch", 10)
push({"Refactor tests", 5}):       sinks below everything with priority > 5
                                     top() = ("Security patch", 10)

pop() order: "Security patch"(10) → "Fix critical bug"(9) → "Refactor tests"(5) → "Update docs"(2)
```

This is the realistic shape of most exam/interview PQ problems: wrap whatever data you need to carry (`name`, in this case) alongside the field that actually determines priority (`priority`), and write one small `operator()` that compares on just that field.

---

## 10. Checkpoint Before Part 3

| Need | Approach |
| --- | --- |
| Max-heap of plain numbers | `priority_queue<int> pq;` |
| Min-heap of plain numbers | `priority_queue<int, vector<int>, greater<int>> pq;` or negate values |
| Pairs, default lexicographic order | `priority_queue<pair<int,int>> pq;` |
| Pairs/objects, custom field ordering | Comparator struct (Method 1) or lambda (Method 2) |
| Remember: comparator return value | `true` = "a is lower priority than b" = "a sinks" |

# 🎯Hands-On Coding

---

## `Exercise 1` — Warm-up: Max-Heap and Min-Heap Side by Side

**Task:** Push the same set of numbers into both a max-heap and a min-heap, then pop everything from each and print the order.

```cpp
#include <iostream>
#include <queue>
using namespace std;

int main() {
    priority_queue<int> maxHeap;
    priority_queue<int, vector<int>, greater<int>> minHeap;

    int values[] = {40, 10, 90, 30, 70};

    for (int v : values) {
        maxHeap.push(v);
        minHeap.push(v);
    }

    cout << "Max-heap pop order: ";
    while (!maxHeap.empty()) {
        cout << maxHeap.top() << " ";
        maxHeap.pop();
    }
    cout << endl;

    cout << "Min-heap pop order: ";
    while (!minHeap.empty()) {
        cout << minHeap.top() << " ";
        minHeap.pop();
    }
    cout << endl;

    return 0;
}
```

**Expected output:**

```
Max-heap pop order: 90 70 40 30 10
Min-heap pop order: 10 30 40 70 90
```

**What this exercise checks:** that you can correctly declare both heap types from memory (Part 1, Section 3) without looking anything up — this exact pair of declarations is the single most-reused piece of syntax in this entire lecture.

---

## `Exercise 2` — Running "Top 3 Highest Scores" From a Stream

**Task:** You receive exam scores one at a time (simulating a live stream). At every point, maintain and print the **3 highest scores seen so far**, using the min-heap-of-size-K pattern.

```cpp
#include <iostream>
#include <queue>
#include <vector>
using namespace std;

void printHeapContents(priority_queue<int, vector<int>, greater<int>> pq) {
    // pass by value — copying the heap so we don't destroy the real one just to print it
    vector<int> temp;
    while (!pq.empty()) {
        temp.push_back(pq.top());
        pq.pop();
    }
    for (int x : temp) cout << x << " ";
    cout << endl;
}

int main() {
    int K = 3;
    priority_queue<int, vector<int>, greater<int>> topK;   // min-heap of size K
    int stream[] = {55, 82, 40, 91, 67, 95, 30};

    for (int score : stream) {
        if (topK.size() < K) {
            topK.push(score);
        } else if (score > topK.top()) {
            topK.pop();
            topK.push(score);
        }
        cout << "After seeing " << score << ", top " << K << " so far: ";
        printHeapContents(topK);
    }

    return 0;
}
```

**Expected output:**

```
After seeing 55, top 3 so far: 55
After seeing 82, top 3 so far: 55 82
After seeing 40, top 3 so far: 40 55 82
After seeing 91, top 3 so far: 55 82 91
After seeing 67, top 3 so far: 67 82 91
After seeing 95, top 3 so far: 82 91 95
After seeing 30, top 3 so far: 82 91 95        ← 30 ignored, weaker than current top(82)
```

**What this exercise checks:** that you understand *why* a min-heap (not max-heap) is correct here — this is the exact "min-heap-for-largest-K inversion" idea, but now you're tracing it yourself instead of reading about it. Try modifying `K` to `2` or `4` by hand before running it, and predict the output first.

---

## `Exercise 3` — Priority Queue of Pairs: Employee Salaries

**Task:** Store `{salary, employeeId}` pairs and process employees from highest to lowest salary, using the **default** pair ordering (no custom comparator yet — this reinforces Part 2, Section 7).

```cpp
#include <iostream>
#include <queue>
using namespace std;

int main() {
    priority_queue<pair<int,int>> pq;   // {salary, employeeId}, default max-heap

    pq.push({55000, 101});
    pq.push({72000, 102});
    pq.push({61000, 103});
    pq.push({72000, 104});   // same salary as 102 — tie-break on employeeId

    cout << "Processing order (salary, employeeId):" << endl;
    while (!pq.empty()) {
        auto [salary, id] = pq.top();
        cout << "  Salary " << salary << ", Employee #" << id << endl;
        pq.pop();
    }

    return 0;
}
```

**Expected output:**

```
Processing order (salary, employeeId):
  Salary 72000, Employee #104
  Salary 72000, Employee #102
  Salary 61000, Employee #103
  Salary 55000, Employee #101
```

**What this exercise checks:** the tie-break behavior specifically — both employees 102 and 104 have salary 72000, and the pair's default ordering breaks the tie by the larger `employeeId` (104 before 102). Before running this, predict: which of the two `72000` entries comes out first, and why?

---

## `Exercise 4` — Custom Comparator: Order Pairs by Second Field

**Task:** Same `{salary, employeeId}` data as Exercise 3, but now you want to process by **employeeId order** instead, regardless of salary — forcing you to write a real comparator (Part 2, Section 8, Method 1).

```cpp
#include <iostream>
#include <queue>
using namespace std;

struct CompareById {
    bool operator()(const pair<int,int>& a, const pair<int,int>& b) {
        return a.second < b.second;   // smaller employeeId = HIGHER priority
                                        // careful: returning true sinks 'a' —
                                        // so here, true means a.second is SMALLER,
                                        // meaning... re-check this before running!
    }
};

int main() {
    priority_queue<pair<int,int>, vector<pair<int,int>>, CompareById> pq;

    pq.push({55000, 101});
    pq.push({72000, 102});
    pq.push({61000, 103});

    cout << "Processing order (salary, employeeId):" << endl;
    while (!pq.empty()) {
        auto [salary, id] = pq.top();
        cout << "  Salary " << salary << ", Employee #" << id << endl;
        pq.pop();
    }

    return 0;
}
```

⚠️ **Deliberately left in as a trap, same as a real exam question would:** trace `CompareById` by hand using the Part 2, Section 8 rule — "`true` means `a` is lower priority, sinks below `b`." With `a.second < b.second` returning `true` whenever `a`'s id is *smaller*, that means the **smaller** id gets treated as *lower* priority and sinks down — so this comparator actually produces **largest employeeId first**, not smallest. Before running the code, write down what you think the output will be, *then* run it and check whether your prediction matches. If you wanted smallest-id-first instead, which single character would you change?

---

## `Exercise 5` — Custom Objects: A Tiny Print-Job Scheduler

**Task:** Simulate an OS-style print queue where jobs have a priority level, and higher priority should print first — ties broken by shorter job name alphabetically.

```cpp
#include <iostream>
#include <queue>
#include <string>
using namespace std;

struct PrintJob {
    string fileName;
    int priority;
};

struct ComparePrintJob {
    bool operator()(const PrintJob& a, const PrintJob& b) {
        if (a.priority != b.priority) {
            return a.priority < b.priority;   // lower priority value sinks
        }
        return a.fileName > b.fileName;        // tie: alphabetically LATER name sinks
                                                 // (so alphabetically earlier prints first)
    }
};

int main() {
    priority_queue<PrintJob, vector<PrintJob>, ComparePrintJob> printQueue;

    printQueue.push({"report.pdf", 2});
    printQueue.push({"invoice.pdf", 5});
    printQueue.push({"memo.pdf", 5});
    printQueue.push({"poster.pdf", 1});

    cout << "Print order:" << endl;
    
    while (!printQueue.empty()) {
        PrintJob job = printQueue.top();
        cout << "  " << job.fileName << " (priority " << job.priority << ")" << endl;
        printQueue.pop();
    }

    return 0;
}
```

**Expected output:**

```
Print order:
  invoice.pdf (priority 5)
  memo.pdf (priority 5)
  report.pdf (priority 2)
  poster.pdf (priority 1)
```

**What this exercise checks:** a two-level comparator — first compare on `priority`, and **only** fall through to the `fileName` comparison when priorities are exactly equal. This `if (primary fields differ) {...} return secondary comparison` shape is the standard template for "sort by X, tie-break by Y" in almost any comparator you'll ever write, in a PQ or otherwise.

---

## `Exercise 6` — Combine Everything: Merge Pattern Using a Min-Heap of Pairs

**Task:** You have 3 separate sorted lists. Using a min-heap of `{value, listIndex}` pairs, merge them into one fully sorted sequence — without concatenating and sorting from scratch. This is the smallest possible version of the "merge K sorted lists" pattern.

```cpp
#include <iostream>
#include <queue>
#include <vector>
using namespace std;

int main() {
    vector<vector<int>> lists = {
        {1, 4, 7},
        {2, 5, 8},
        {3, 6}
    };

    // min-heap of {value, listIndex} — default pair ordering works perfectly here,
    // since we WANT to compare by value first (Exercise 3's default behavior)
    priority_queue<pair<int,int>, vector<pair<int,int>>, greater<pair<int,int>>> pq;
    vector<int> pos(lists.size(), 0);   // tracks how far we've consumed each list

    // seed the heap with the first element of every list
    for (int i = 0; i < lists.size(); i++) {
        pq.push({lists[i][0], i});
    }

    vector<int> merged;
    while (!pq.empty()) {
        auto [val, listIdx] = pq.top();
        pq.pop();
        merged.push_back(val);

        pos[listIdx]++;
        if (pos[listIdx] < lists[listIdx].size()) {
            pq.push({lists[listIdx][pos[listIdx]], listIdx});
        }
    }

    cout << "Merged result: ";
    for (int x : merged) cout << x << " ";
    cout << endl;

    return 0;
}
```

**Expected output:**

```
Merged result: 1 2 3 4 5 6 7 8
```

**What this exercise checks:** the "seed the heap, pop-and-replace" pattern that underlies every merge-K-lists style problem — each `pop` gives you the current overall-smallest candidate across *all* lists, and you immediately push that same list's *next* element back in, so the heap always has exactly one candidate per still-active list. Try tracing the heap's contents by hand after each pop, the same way we did in Parts 1–2 — this is the one exercise worth fully hand-tracing before trusting the output.

---

## 14. Full Recap — Lecture 5

| Topic | Key Takeaway |
| --- | --- |
| What problem a PQ solves | Access the current highest/lowest-priority element, where "priority" ≠ arrival order (unlike a plain FIFO queue) |
| Min-heap vs Max-heap | Max-heap (`priority_queue<int>`) = largest on top. Min-heap (`priority_queue<int,vector<int>,greater<int>>`) = smallest on top |
| Core operations & complexity | `push`/`pop`: O(log n). `top`/`size`/`empty`: O(1). Space: O(n) |
| Min-heap via negation | Push `-x`, read back `-top()` — quick trick for plain numeric types only |
| Pairs, default order | Lexicographic: compare `.first`, tie-break on `.second` (Exercise 3) |
| Custom comparators | `true` returned from `operator()` means "`a` is LOWER priority, sink it" — the opposite intuition from `sort()` (Exercise 4's deliberate trap) |
| Two-level comparators | Compare primary field; only fall through to secondary field on a tie (Exercise 5) |
| Min-heap-for-largest-K inversion | A min-heap surfaces the "weakest" of your kept top-K, making it the right structure for eviction decisions (Exercise 2) |
| Merge-K-lists pattern | Seed the heap with one candidate per source, pop-and-replace to always see the next-smallest (Exercise 6) |