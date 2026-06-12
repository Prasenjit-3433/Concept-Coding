# Binary Search DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Binary search is not just about sorted arrays. It is about **monotonicity** — any time you can say "if X is feasible, then X+1 is also feasible (or infeasible)," you can binary search on X.

**The universal template — always use this, stop debating lo/hi/mid:**
```
lo = minimum_possible_answer
hi = maximum_possible_answer

while lo < hi:
    mid = lo + (hi - lo) // 2
    if feasible(mid):
        hi = mid        # mid might be the answer, don't exclude it
    else:
        lo = mid + 1    # mid is not the answer, exclude it

return lo
```
This is the **lower bound template** — finds the smallest value where `feasible()` is true. For upper bound (largest valid), flip: use `lo = mid` and `hi = mid - 1`, with `while lo < hi` starting from the right.

**The three questions to answer before coding:**
1. What is the search space?
2. What is the monotone predicate?
3. Am I finding lower bound or upper bound?

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Standard Binary Search | sorted array, find target or its boundary position |
| Binary Search on Semi-Sorted Space | rotated, has a peak, semi-sorted — not uniformly sorted |
| Binary Search on Matrix | 2D sorted structure, find element or count |
| Binary Search on Answer | "minimum days/speed/capacity", feasibility via simulation |
| Minimax / Maximin via Binary Search | "minimize the maximum", "maximize the minimum", placement with gap/load constraint |
| Finding Kth Element | kth smallest/largest across sorted structures — count ≤ mid or partition |
| Binary Search on Events | versioned data, timestamps, "latest state at time t" |

---

## How to tell Binary Search on Answer apart from Minimax in an unseen problem

This is the most common source of confusion in this entire sheet. Both patterns use binary search on an answer domain with a feasibility function — the outer shell looks identical. The difference is in the feasibility function itself.

**Binary Search on Answer:** The feasibility check simulates a sequential process — iterate the array once, greedily accumulate (days, loads, trips), and check if the constraint is met. There is no placement decision. You are asking: *"given this capacity/speed, can I finish the task?"*

**Minimax / Maximin:** The feasibility check involves a placement decision with a gap or load constraint. You are asking: *"can I place all elements such that no two are closer than mid?"* or *"can I assign all tasks such that no worker exceeds mid load?"* The greedy inside the feasibility check is about **where to place**, not how much to accumulate.

**The one-line test:**
> If your feasibility function iterates and accumulates → Pattern 4 (Binary Search on Answer).
> If your feasibility function iterates and places / assigns → Pattern 5 (Minimax).

---

## Pattern 1: Standard Binary Search

**Identify:** The array is sorted. You are searching for a target, its first/last occurrence, or a boundary position. The predicate is simple comparison.

**Two boundary variants — know both cold:**
- **Lower bound:** First position where `arr[i] >= target` → `hi = mid` when condition met
- **Upper bound:** First position where `arr[i] > target` → last valid position is `upper_bound - 1`

These two alone solve LC 34, LC 35, LC 744, and a large fraction of "search in sorted array" problems.

**Bonus problem for Indian interview context:** LC 2300 (Successful Pairs of Spells and Potions) — sort one array, then for each element in the other, binary search for the threshold position. This is the standard lower bound technique applied in a two-array context. It looks harder than it is because the setup requires sorting a different array than the one you iterate over.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 704. Binary Search | Pure template — find exact target |
| 2 | LC 35. Search Insert Position | Lower bound — first position ≥ target |
| 3 | LC 374. Guess Number Higher or Lower | Binary search on implicit sorted range |
| 4 | LC 278. First Bad Version | Lower bound on boolean predicate — first `true` |
| 5 | LC 367. Valid Perfect Square | Binary search on answer space `[1, num]` |
| 6 | LC 744. Find Smallest Letter Greater Than Target | Upper bound on circular sorted array |
| 7 | LC 34. Find First and Last Position of Element | Lower + upper bound combined — two binary searches |
| 8 | LC 2300. Successful Pairs of Spells and Potions | Sort potions, lower bound per spell — two-array boundary search |

---

## Pattern 2: Binary Search on Semi-Sorted Space

**Identify:** The array is not uniformly sorted — it is rotated, has a peak, or is a mountain. Standard binary search breaks because the "discard half" logic relies on uniform order, which no longer holds globally. The key insight in each variant is finding a **local structural property** that still lets you discard one half confidently.

**Rotated array — key insight:** One half is always uniformly sorted. Identify which half, then check if the target falls inside it.
```
if arr[lo] <= arr[mid]:   # left half is sorted
    if arr[lo] <= target < arr[mid]:
        hi = mid - 1
    else:
        lo = mid + 1
else:                      # right half is sorted
    if arr[mid] < target <= arr[hi]:
        lo = mid + 1
    else:
        hi = mid - 1
```

**Peak finding — key insight:** Move toward the higher neighbor. `arr[mid] < arr[mid+1]` means peak is to the right; you can safely discard the left half.

**Nearly sorted array note (common in Indian interviews):** Each element is at most k positions away from its sorted position. Standard binary search breaks. Fix: check `mid-1`, `mid`, `mid+1` at each step. This still discards roughly half the space each iteration. Recognizing this as binary search (not linear scan) is the tested insight.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 153. Find Minimum in Rotated Sorted Array | Rotated — minimum is the inflection point |
| 2 | LC 33. Search in Rotated Sorted Array | Rotated — identify sorted half, decide side |
| 3 | LC 81. Search in Rotated Sorted Array II | Same + handle duplicates with `lo++` when `arr[lo]==arr[mid]` |
| 4 | LC 162. Find Peak Element | Peak finding — move toward higher neighbor |
| 5 | LC 852. Peak Index in a Mountain Array | Simpler peak — guaranteed mountain shape |
| 6 | LC 1095. Find in Mountain Array | Three-phase: find peak → search ascending half → search descending half |
| 7 | LC 540. Single Element in Sorted Array | Parity-based — single element breaks the even/odd index pattern |

---

## Pattern 3: Binary Search on Matrix

**Identify:** A 2D matrix with sorted structure. Three distinct techniques depending on the structure — identify which applies before writing a single line of code.

- **Fully sorted as 1D** (LC 74): rows concatenate in sorted order. Treat as flattened array. `row = mid // cols`, `col = mid % cols`. Full O(log mn) binary search applies.
- **Row and column sorted independently** (LC 240): binary search doesn't apply cleanly. Use staircase search from top-right corner — go left if too large, go down if too small. O(m+n).
- **Value domain search** (GFG Median): binary search on the answer value, count how many elements across all rows are ≤ mid using binary search per row. O(n log m · log(max−min)).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 74. Search a 2D Matrix | Flatten to 1D — full binary search applies |
| 2 | LC 240. Search a 2D Matrix II | Staircase from top-right — NOT standard binary search |
| 3 | GFG: Median in Row-Wise Sorted Matrix | Binary search on value domain — count ≤ mid across all rows |

---

## Pattern 4: Binary Search on Answer

**Identify:** You are not searching for a value in an array. You are searching for the **optimal value of the answer itself** — minimum speed, minimum capacity, fewest days. The answer space is a range of integers. The feasibility check **simulates a sequential process** — iterate the array, greedily accumulate, and verify the constraint is met. No placement decision involved.

**How to solve any problem in this pattern:**
1. Define `lo` and `hi` — the bounds of the answer space
2. Write `feasible(mid)` — simulate the process with answer = mid
3. Verify monotonicity — if mid works, mid+1 always works (or always fails)
4. Apply the lower bound template mechanically

**The feasibility function is the entire problem.** Once you have it, the binary search is 5 lines.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 69. Sqrt(x) | Simplest parametric — largest `k` where `k*k <= x` |
| 2 | LC 875. Koko Eating Bananas | feasible(k): can finish all piles in h hours at speed k? |
| 3 | LC 1011. Capacity to Ship Packages Within D Days | feasible(c): can ship all in d days at capacity c? |
| 4 | LC 1283. Find the Smallest Divisor | feasible(d): does sum of ceilings(arr[i]/d) ≤ threshold? |
| 5 | LC 2187. Minimum Time to Complete Trips | feasible(t): can all buses complete ≥ totalTrips by time t? |
| 6 | LC 1482. Minimum Number of Days to Make m Bouquets | feasible(d): can make m bouquets by day d? |
| 7 | LC 410. Split Array Largest Sum | feasible(m): can split into k parts each with sum ≤ m? |
| 8 | LC 2064. Maximum Value at a Given Index in Bounded Array | feasible(v): can build array with peak v satisfying sum ≤ maxSum? |

---

## Pattern 5: Minimax / Maximin via Binary Search

**Identify:** The problem asks to **minimize the maximum** value or **maximize the minimum** value after placing or assigning elements under a gap or load constraint. The feasibility check does NOT simulate accumulation — it makes placement decisions: greedily place each element and check if the required count fits within the constraint.

**How to distinguish from Pattern 4 in an unseen problem:**
- Read the feasibility check you would write. If it iterates and asks "can I place the next element here given the gap?" → this pattern.
- The words "minimize the maximum distance/gap/load" or "maximize the minimum distance/gap/force" in the problem statement are nearly definitive signals.
- The search space is inverted vs Pattern 4: you are often searching for the largest feasible minimum (use upper bound template) rather than the smallest feasible maximum.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1552. Magnetic Force Between Two Balls | Maximize minimum gap — greedily place balls, count placements ≥ mid gap |
| 2 | LC 2517. Maximum Tastiness of Candy Basket | Maximize minimum difference — same greedy placement pattern |
| 3 | LC 2528. Maximize the Minimum Powered City | Minimize maximum deficit — binary search on threshold + prefix sum + greedy |

---

## Pattern 6: Finding Kth Element

**Identify:** Find the Kth smallest (or largest) element across one or more sorted structures. Two fundamentally different techniques — distinguish them before coding.

### Subtype A — Count ≤ mid (Value Domain Binary Search)
Binary search on the **value**, not the index. For a candidate answer `mid`, count how many elements across all structures are ≤ mid. If count ≥ k, answer is ≤ mid; otherwise answer is > mid. The counting function varies by structure but is always computable in better than O(n²).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 378. Kth Smallest Element in a Sorted Matrix | Count ≤ mid via O(n) staircase — binary search on value |
| 2 | LC 668. Kth Smallest Number in Multiplication Table | Count ≤ mid via row formula: `sum(min(mid//i, n))` |
| 3 | LC 719. Find K-th Smallest Pair Distance | Binary search on distance + sliding window count |
| 4 | LC 2040. Kth Smallest Product of Two Sorted Arrays | Binary search on value + count products ≤ mid with sign case handling |

### Subtype B — Partition Binary Search
Binary search on a **split point** (index), not a value. Verify correctness by comparing boundary elements. Does NOT count elements — partitions. This is a fundamentally different technique from Subtype A.

**LC 4 is the hardest problem in this entire sheet.** The insight: binary search on the partition point in the smaller array. The correct partition satisfies `maxLeft1 ≤ minRight2` and `maxLeft2 ≤ minRight1`. Do this only after all Subtype A problems are solid.

| # | Problem | Key Concept |
|---|---|---|
| 5 | LC 658. Find K Closest Elements | Binary search on window start — find best starting index for k-element window |
| 6 | LC 4. Median of Two Sorted Arrays | Partition binary search — split where combined left halves = combined right halves |

---

## Pattern 7: Binary Search on Events / Versioned Data

**Identify:** Data is stored at timestamps or version numbers, and you need to retrieve the most recent state at or before a given time. The stored data is implicitly sorted by time — apply upper bound on that sorted sequence.

**Why this is its own pattern:** It is primarily a design + retrieval pattern. The interview tests two skills simultaneously: (1) choosing the right storage structure — sorted list of `(timestamp, value)` pairs, and (2) applying the correct bound direction — upper bound, meaning last timestamp ≤ query. Getting the bound direction wrong is the most common mistake.

**LC 528 note:** Random Pick with Weight is included here because the technique is identical structurally — build a prefix sum array (implicitly sorted and monotonically increasing), then find the first prefix ≥ a random value via lower bound. It is not a versioned data problem but the binary-search-on-prefix-sum retrieval is the same skill.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 528. Random Pick with Weight | Build prefix sum array — lower bound for first prefix ≥ random value |
| 2 | LC 981. Time Based Key-Value Store | Store `(timestamp, value)` sorted — upper bound for latest timestamp ≤ query |
| 3 | LC 1146. Snapshot Array | Store `(snap_id, value)` per index — upper bound for latest snap_id ≤ query |

---

## Final Summary

| Pattern | Problems | Core Technique |
|---|---|---|
| Standard Binary Search | 8 | Lower / upper bound on sorted array |
| Binary Search on Semi-Sorted Space | 7 | Local structural property enables half-discard |
| Binary Search on Matrix | 3 | Flatten vs staircase vs value domain — three distinct cases |
| Binary Search on Answer | 8 | Feasibility via simulation — iterate and accumulate |
| Minimax / Maximin | 3 | Feasibility via placement — iterate and place greedily |
| Finding Kth Element | 6 | Count ≤ mid (value domain) vs partition (split point) |
| Binary Search on Events | 3 | Sorted timestamp storage + bound retrieval |
| **Total** | **~38 problems** | |

---

## How to Use This Sheet

**Pattern 1 → 2 is mandatory order.** The `lo < hi` invariant and bound direction must be automatic before touching semi-sorted arrays.

**Patterns 4 and 5 are the highest interview ROI.** Together they cover the majority of binary search questions at product companies. The critical skill is writing the feasibility function correctly and knowing which of the two patterns you are in. Use the one-line test from the intro section every time.

**Inside Pattern 6, complete all Subtype A before LC 4.** The count ≤ mid technique builds the intuition that binary search can operate on a value domain. LC 4's partition technique will feel approachable only after that foundation is solid.

**Pattern 7 is often asked as a system design mini-problem.** Knowing to store sorted `(timestamp, value)` pairs and retrieve with the correct bound is the complete answer.