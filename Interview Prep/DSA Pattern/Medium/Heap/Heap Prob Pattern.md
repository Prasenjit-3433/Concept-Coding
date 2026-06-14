Here is the updated sheet with exactly those two changes incorporated:

---

# Heap DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

A heap gives you **O(log n) insert and O(1) peek at the extreme** — the minimum or maximum. Every heap pattern is exploiting exactly this: you have a dynamic collection, and at each step you need the best (smallest or largest) element right now, without sorting the entire collection each time.

**The two questions to ask before reaching for a heap:**
1. Do I need the running minimum or maximum of a dynamically changing set? → Heap.
2. Do I need the kth order statistic from a stream? → Heap of size k.

If neither applies, reconsider — heaps are frequently misused when a sort + scan or two-pointer would be cleaner.

> **Recognition trigger:** If the problem repeatedly asks for the current best candidate while elements are continuously entering and leaving consideration, think Heap.

**Language mapping:**
- Java → `PriorityQueue` (min-heap by default; wrap with `Collections.reverseOrder()` for max-heap)
- Python → `heapq` (min-heap only; negate values for max-heap)
- C++ → `priority_queue` (max-heap by default; use `greater<T>` for min-heap)

**Lazy deletion — the pattern you must know:**
When you need to remove an arbitrary element from a heap (not just the top), the clean solution is lazy deletion: mark the element as invalid, push a "stale" marker, and skip it when it surfaces at the top. This is the technique behind Sliding Window Median and The Skyline Problem. Know it before touching those problems.

---

## Pattern Identification Quick Reference

> If the problem repeatedly asks for the current best candidate while elements are continuously entering and leaving consideration, think Heap.

| Pattern | Trigger |
|---|---|
| Standard Heap | "simulate", "greedy with priority", "reorganize by frequency", "minimum cost to combine", "two moving frontiers" |
| Top K Elements | "kth largest/smallest", "k closest", "k most frequent", "top k" from a stream or static array |
| K-Way Merge | "merge k sorted", "kth smallest pair/product", "smallest range across k lists" |
| Two Heaps | "median of stream", "balance two groups", "profit maximization with capital constraint" |
| Scheduler / Event-Driven Heap | "schedule tasks", "meeting rooms", "events", "skyline", "intervals with queries" |

---

## Pattern 1: Standard Heap Problems

**Identify:** The problem requires maintaining a dynamic collection and repeatedly extracting the minimum or maximum to drive a greedy decision. No k-size constraint, no two-group balance, no merging of sorted sequences. The heap is the whole algorithm.

**The Huffman-style pattern (LC 1167, LC 1046):** Repeatedly extract the two smallest (or largest), combine them, and re-insert. This greedy works because combining the smallest two elements first always minimizes the total cost — a classic exchange argument proof.

**Frequency-based greedy (LC 1405, LC 767):** Build a max-heap on frequencies. At each step, pick the most frequent element that is valid to place (not same as the last placed). Re-insert with decremented frequency. The heap ensures you always use the most urgent element first, preventing any character from becoming unplaceable.

**Sub-pattern: Sort + Heap Invariant (LC 857, LC 2542, LC 2462)**
Sort by one variable to fix an ordering, then sweep while maintaining a heap on a second variable. The heap does not just find the best element — it maintains an invariant (k smallest qualities, k highest multipliers, cheapest frontier candidates) that updates as you advance through the sorted order. This trick is the most common source of hard-level heap problems at FAANG interviews. Recognition signal: the problem has two competing variables and asks you to optimize one while constraining the other.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1046. Last Stone Weight | Max-heap — repeatedly smash two largest, reinsert remainder |
| 2 | LC 1167. Minimum Cost to Connect Sticks | Min-heap — Huffman coding pattern; combine two smallest, add cost, reinsert |
| 3 | LC 767. Reorganize String | Max-heap on freq — always place most frequent valid char; return "" if impossible |
| 4 | LC 1405. Longest Happy String | Max-heap on freq — same greedy as LC 767, but pick top-2 when top is blocked |
| 5 | LC 2208. Minimum Operations to Halve Array Sum | Max-heap — always halve the largest; count operations until sum ≤ target |
| 6 | LC 2530. Maximal Score After Applying K Operations | Max-heap — take max, add to score, reinsert ceil(val/3); repeat k times |
| 7 | LC 857. Minimum Cost to Hire K Workers | Sort by wage/quality ratio + max-heap — maintain k-size quality sum; update min cost at each step |
| 8 | LC 2542. Maximum Subsequence Score | Sort descending by one array + min-heap — maintain k-size subset of highest multipliers |
| 9 | LC 2462. Total Cost to Hire K Workers | Two min-heaps from both ends of array — simulate k hiring rounds from two moving frontiers |

---

## Pattern 2: Top K Elements

**Identify:** The problem asks for the kth largest, kth smallest, k closest, or k most frequent elements from a static array or a stream. The core trick: maintain a heap of exactly size k. For kth largest, use a min-heap of size k — the top is the kth largest because everything below it has been evicted.

**The size-k heap invariant:**
- kth largest → min-heap of size k (evict when size > k; top = kth largest)
- kth smallest → max-heap of size k (evict when size > k; top = kth smallest)
- k closest to origin → max-heap of size k on distance (evict farthest; everything remaining is closer)

**Quickselect alternative:** For static arrays, average O(n) vs heap's O(n log k). Interviewers may ask for both. Know quickselect for LC 215 — it is the follow-up question in ~60% of cases.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 703. Kth Largest Element in a Stream | Min-heap of size k — maintain invariant on each insert; top = answer |
| 2 | LC 215. Kth Largest Element in an Array | Min-heap of size k OR quickselect O(n) average — know both |
| 3 | LC 973. K Closest Points to Origin | Max-heap of size k on distance — evict farthest; remainder is k closest |
| 4 | LC 347. Top K Frequent Elements | Frequency map + min-heap of size k on frequency — O(n log k) |

---

## Pattern 3: K-Way Merge

**Identify:** You have k sorted sequences (lists, arrays, or virtual sorted structures like sorted pairs or products) and need to merge them or find the kth element across them. The heap holds one "current candidate" from each sequence. When you extract the minimum, you push the next element from the same sequence.

**The template — memorize this:**
```
heap = []
# Initialize: push (value, sequence_index, element_index) for each sequence
for i in range(k):
    heappush(heap, (sequences[i][0], i, 0))

for _ in range(target):
    val, seq_i, elem_i = heappop(heap)
    if elem_i + 1 < len(sequences[seq_i]):
        heappush(heap, (sequences[seq_i][elem_i + 1], seq_i, elem_i + 1))
```

**Virtual sequences (LC 373, LC 786):** When sequences are implicit (all pairs (a[i], b[j])), only initialize the heap with the first element of each virtual sequence (all pairs with j=0). When you extract (i, j), push (i, j+1) — this lazily generates the sequence on demand.

**Smallest range (LC 632):** The heap always holds exactly one element from each of the k lists. The window is `[heap_min, current_max]`. Each pop shrinks the window from the bottom; the current max is tracked explicitly. This problem showcases that K-Way Merge is not just about finding the kth element — it is about maintaining simultaneous pointers across k sequences.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Merge K Sorted Arrays | Pure K-Way Merge template — heap holds (value, array_index, element_index) |
| 2 | LC 23. Merge K Sorted Lists *(cross-ref from LL sheet)* | Same template on linked list nodes — see Linked List Pattern 4 for full treatment |
| 3 | LC 373. Find K Pairs with Smallest Sums | Virtual K-Way Merge — initialize with (a[i], b[0]) for all i; push (a[i], b[j+1]) on extract |
| 4 | LC 786. Kth Smallest Prime Fraction | Virtual sorted sequences — heap on (p/q, p_idx, q_idx); same enumeration pattern |
| 5 | LC 632. Smallest Range Covering Elements from K Lists | K-Way Merge + range tracking — window = [heap_min, current_max]; minimize window as you advance |

---

## Pattern 4: Two Heaps

**Identify:** The problem requires maintaining a partition of a dynamic stream into two groups — typically a lower half and an upper half — and querying the boundary between them. A max-heap holds the lower half (its top is the largest of the small elements), a min-heap holds the upper half (its top is the smallest of the large elements). The median lives at this boundary.

**Balance invariant:**
```
After each insert:
    if val <= max_heap.top(): push to max_heap
    else: push to min_heap
    
    Rebalance so |len(max_heap) - len(min_heap)| <= 1
    
Median:
    if sizes equal: (max_heap.top() + min_heap.top()) / 2
    else: top of the larger heap
```

**Lazy deletion for sliding window (LC 480):** When the window slides, the outgoing element may be deep inside one of the heaps. Mark it as deleted. When it surfaces at the top, skip it. This avoids the O(n) cost of arbitrary removal.

**IPO pattern (LC 502):** Two heaps with different semantics — one sorted by capital (min-heap: unlock projects as capital grows), one sorted by profit (max-heap: always pick the highest-profit available project). This shows the "two heaps for two dimensions" generalization beyond just median.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 295. Find Median from Data Stream | Canonical two-heap — max-heap lower half, min-heap upper half; balance after each insert |
| 2 | LC 502. IPO | Two heaps for two dimensions — min-heap by capital, max-heap by profit; unlock and pick greedily |
| 3 | LC 480. Sliding Window Median | Two heaps + lazy deletion — mark outgoing element stale, skip when it surfaces |
| 4 | LC 2402. Meeting Rooms III | Two heaps — available rooms (min-heap by room id) + occupied (min-heap by end time); free rooms at each meeting start |

---

## Pattern 5: Scheduler / Event-Driven Heap

**Identify:** Problems involving tasks, events, intervals, or meetings where the heap manages "what is active right now" as you sweep through time. Events are sorted by start time (or arrival time); the heap holds active events sorted by end time, priority, or some other ordering property. You process events in time order, using the heap to efficiently answer "what is the best choice right now?"

**The offline sweep pattern:** Sort queries or events by one dimension (start time, deadline, or query value). Sweep from left to right. As each event becomes relevant, push it into the heap. When it becomes irrelevant (its end is past the current point), pop it lazily.

**Why this is its own pattern:** The heap here is not operating on a static dataset or a pure stream — it is operating in coordination with a time sweep. The interaction between sorted events and the heap's priority ordering is the core technique. Problems in this pattern fail if you try to apply a greedy without the sweep coordination.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1353. Maximum Number of Events That Can Be Attended | Sort by start + min-heap of end times — each day attend the soonest-ending available event |
| 2 | LC 1834. Single-Threaded CPU | Sort by arrival + min-heap by processing time — idle CPU picks shortest available task |
| 3 | LC 1882. Process Tasks Using Servers | Two min-heaps — available (by weight, then index) + busy (by free time); assign greedily |
| 4 | LC 218. The Skyline Problem | Event sweep + max-heap with lazy deletion — track current tallest active building |
| 5 | LC 1851. Minimum Interval to Include Each Query | Offline sort queries + events + min-heap by interval length — sweep queries, add valid intervals, answer with heap top |
| 6 | LC 407. Trapping Rain Water II *(Advanced — Optional)* | Min-heap BFS from boundary inward — 3D extension of rain water trapping; heap replaces the two-pointer approach |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Standard Heap Problems | 9 | Push/pop to drive greedy; Huffman-style combining; frequency-based scheduling; Sort + Heap Invariant |
| Top K Elements | 4 | Size-k heap invariant; quickselect alternative for static arrays |
| K-Way Merge | 5 | One representative per sequence in heap; virtual sequence enumeration |
| Two Heaps | 4 | Partition stream into lower/upper halves; lazy deletion for sliding window |
| Scheduler / Event-Driven Heap | 6 | Time sweep + heap manages currently active events |
| **Total** | **~28 problems** | |

---

## How to Use This Sheet

**Pattern 1 first — the greedy intuition must be solid.** If you cannot explain why picking the smallest two sticks to combine first minimizes cost, the more complex patterns will feel arbitrary.

**Pattern 2 before Pattern 3.** The size-k heap invariant is the building block for K-Way Merge — once you internalize "maintain k elements in the heap," extending to k sequences is natural.

**Pattern 4 requires Pattern 2 as prerequisite.** The two-heap balance logic builds directly on the size-k heap concept. Median of stream without that foundation becomes pure memorization.

**Pattern 5 last.** Event-driven heap problems combine sorting, heap operations, and lazy deletion — three concepts that must each be individually solid first. LC 218 Skyline Problem is the hardest in this tier and should be attempted only after LC 295 and LC 1353 are clean.

**LC 857 and LC 2542 in Pattern 1 are the highest interview ROI.** Both appear at Google, Meta, and Amazon at the hard level. They combine sorting with a size-k heap in a non-obvious way — the key is recognizing that the heap maintains an invariant while you sweep, not that you blindly push all elements.

---
