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
2. What is the monotone predicate — and is it even boolean, or am I minimizing a distance to a target instead?
3. Am I finding lower bound or upper bound?

**A note on ternary search:** For a strictly unimodal function (one peak, no plateaus — LC 162, or the row-max step inside LC 1901), ternary search is a legitimate alternative to "move toward the higher neighbor." It's rarely the expected answer over plain binary search in an interview, but know it exists so it isn't a surprise if an interviewer brings it up.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Standard Binary Search | sorted array, find target or its boundary position, derived monotonic formula on index, unknown array size |
| Binary Search on Semi-Sorted Space | rotated, has a peak (1D or 2D), semi-sorted — not uniformly sorted |
| Binary Search on Matrix | 2D sorted structure, find element or count, per-row search |
| Binary Search on Answer | "minimum days/speed/capacity", "minimize the maximum sum of a partition", feasibility via simulation, counting, or subsequence match |
| Minimax / Maximin via Binary Search | "maximize the minimum distance/gap", "minimize the maximum gap", placement with a spacing constraint |
| Kth Element — Count ≤ Mid | kth smallest across sorted structures — binary search on value domain |
| Kth Element — Partition | kth/median across two sorted arrays — binary search on split point |
| Binary Search on Events | versioned data, timestamps, "latest state at time t" |

---

## How to tell Binary Search on Answer apart from Minimax in an unseen problem

Both patterns binary search on an answer domain with a feasibility function — the outer shell looks identical. The difference is entirely inside the feasibility function.

**Binary Search on Answer:** Feasibility simulates a sequential process — iterate the array, greedily accumulate (days, loads, trips, pages, sum), check if the constraint is met. No placement decision. You are asking: *"given this capacity/speed/max-partition-sum, can I finish the task?"*

**Minimax / Maximin:** Feasibility involves a placement decision with a gap or load constraint. You are asking: *"can I place all elements — or add new ones — such that no two are closer than mid, or no gap exceeds mid?"* The greedy inside is about **where to place**, not how much to accumulate.

**The one-line test:**
> Feasibility iterates and **accumulates** → Pattern 4 (Binary Search on Answer).
> Feasibility iterates and **places / assigns / checks gaps** → Pattern 5 (Minimax).

**The trap this catches in practice:** "Minimize the maximum sum when partitioning into k groups" (Split Array Largest Sum, Allocate Books, Painter's Partition) *sounds* like it belongs with Aggressive Cows because both say "minimize/maximize" — but partitioning accumulates a running sum and counts groups, while Aggressive Cows places cows and checks a literal distance. That's the accumulate-vs-place distinction, not the min-vs-max wording, that decides the pattern.

---

## How to tell Kth Element — Count ≤ Mid apart from Partition in an unseen problem

Both involve two sorted arrays or sorted structures and finding a specific order statistic. The difference is the technique used inside.

**Count ≤ Mid:** You pick a candidate value `mid`. You count how many elements across your structure(s) are ≤ mid. If count ≥ k, the answer is ≤ mid. You never look at individual array positions — only at the count. Works across matrices, multiplication tables, pair distances.

**Partition:** You pick a candidate split index in the smaller array. You derive the split index in the second array. You check four boundary values. No counting at all. Only works when you have exactly two sorted arrays and need a single order statistic — median, or a generalized kth element.

**One-line test:**
> Do you have one or more sorted structures and need the kth value via a counting function? → Pattern 6 (Count ≤ Mid).
> Do you have exactly two sorted arrays and need the median or a specific split? → Pattern 7 (Partition).

---

## Pattern 1: Standard Binary Search

**Identify:** The array is sorted. You are searching for a target, its first/last occurrence, or a boundary position. The predicate is simple comparison, a monotonic formula derived from the array's values, or a floor/ceil lookup. This is also where you handle the case where you don't even know the array's bounds yet.

**Two boundary variants — know both cold, drill them before anything else in this sheet:**
- **Lower bound:** First position where `arr[i] >= target` → use `hi = mid` when condition met
- **Upper bound:** First position where `arr[i] > target` → last valid position is `upper_bound - 1`

These two alone solve the majority of sorted-array problems, and every later pattern in this sheet is a variation on them applied to a different search space.

**Floor & Ceil in a Sorted Array — the value doesn't have to exist:** Floor is "largest element ≤ target," ceil is "smallest element ≥ target." Both are direct reads off the lower/upper bound machinery — ceil *is* lower bound; floor is one position before upper bound. This is a separately-named classic asked standalone often enough to drill in isolation, even though it's the same two templates you already know.

**Exponential (Unbounded) Binary Search — when you don't know `hi`:** If the array is unbounded or its size is unknown (a common framing: "search in a sorted array of infinite size"), you can't initialize `hi` the normal way. Fix: start `hi = 1`, double it repeatedly (`hi *= 2`) until `arr[hi] >= target` or you step out of bounds, then run ordinary binary search inside `[hi/2, hi]`. This is a genuinely different first step — bound-*finding* before bound-*searching* — and it's easy to blank on in an interview because every other problem in this sheet quietly assumes the bounds already exist.

**LC 1539 — the formula-driven variant:** Kth Missing Positive Number doesn't compare `arr[i]` to a target directly. Instead, `missing(i) = arr[i] - i - 1` is a monotonically non-decreasing function of `i` (as long as no duplicates), so you binary search for the first index where `missing(i) >= k`. Recognizing "a monotonic formula built from array values" as fair game for lower-bound search — not just raw comparison — is the generalizing insight this problem teaches.

**Indian interview context note:** LC 2300 (Successful Pairs of Spells and Potions) — sort one array, then for each element in the other, binary search for the threshold position. It looks harder than it is because the setup requires sorting a different array than the one you iterate over. The technique is pure lower bound.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Lower Bound & Upper Bound | The two templates everything else in this sheet builds on — implement both before touching problems |
| 2 | Floor and Ceil in a Sorted Array | Ceil = lower bound directly; floor = one position before upper bound |
| 3 | LC 704. Binary Search | Pure template — find exact target |
| 4 | LC 35. Search Insert Position | Lower bound — first position ≥ target |
| 5 | LC 374. Guess Number Higher or Lower | Binary search on implicit sorted range |
| 6 | LC 278. First Bad Version | Lower bound on boolean predicate — first `true` |
| 7 | LC 367. Valid Perfect Square | Binary search on answer space `[1, num]` |
| 8 | LC 744. Find Smallest Letter Greater Than Target | Upper bound on circular sorted array |
| 9 | LC 34. Find First and Last Position of Element | Lower + upper bound combined — two binary searches |
| 10 | Count Occurrences in a Sorted Array | Direct application — `upperBound(x) - lowerBound(x)` |
| 11 | LC 1539. Kth Missing Positive Number | Lower bound on a derived monotonic formula, not a raw comparison |
| 12 | LC 2300. Successful Pairs of Spells and Potions | Sort potions, lower bound per spell — two-array boundary search |
| 13 | Search in a Sorted Array of Unknown Size | Exponential search — double `hi` to find bounds, then standard binary search inside them |

---

## Pattern 2: Binary Search on Semi-Sorted Space

**Identify:** The array is not uniformly sorted — it is rotated, has a peak, or is a mountain (in 1D or 2D). Standard binary search breaks because the "discard half" logic relies on uniform order globally. The key insight in each variant: find a **local structural property** that still lets you confidently discard one half.

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

**"How many times has the array been rotated" — the free corollary of LC 153:** The rotation count *is* the index of the minimum element. Once you can find the minimum in a rotated sorted array, this problem requires zero new logic — it's the same function with a different return value. Do it immediately after LC 153, not as a separate exercise.

**Peak finding — key insight:** Move toward the higher neighbor. `arr[mid] < arr[mid+1]` means peak is to the right — safely discard the left half.

**LC 1901 — peak finding generalized to 2D:** Find a Peak Element II has no global sortedness at all — only the local guarantee that every element is greater than its immediate neighbors on the border. Binary search on **columns**: for the middle column, find the row with the maximum value, then compare that cell against its left and right neighbors. If it's smaller than the left neighbor, a peak is guaranteed to exist somewhere in the left half of columns; same logic to the right. This halves the column search space each time, exactly like LC 162 halves the array — the dimension changed, the discard argument didn't.

**Nearly sorted array note (common in Indian interviews):** Each element is at most k positions away from its sorted position. Fix: check `mid-1`, `mid`, `mid+1` at each step — still discards roughly half the space each iteration. Recognizing this as binary search (not linear scan) is the tested insight.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 153. Find Minimum in Rotated Sorted Array | Rotated — minimum is the inflection point |
| 2 | Find out how many times the array has been rotated | Free corollary of LC 153 — rotation count = index of the minimum |
| 3 | LC 33. Search in Rotated Sorted Array | Rotated — identify sorted half, decide side |
| 4 | LC 81. Search in Rotated Sorted Array II | Same + handle duplicates with `lo++` when `arr[lo]==arr[mid]` |
| 5 | LC 162. Find Peak Element | Peak finding — move toward higher neighbor |
| 6 | LC 852. Peak Index in a Mountain Array | Simpler peak — guaranteed mountain shape |
| 7 | LC 1095. Find in Mountain Array | Three-phase: find peak → search ascending half → search descending half |
| 8 | LC 540. Single Element in Sorted Array | Parity-based — single element breaks the even/odd index pattern |
| 9 | LC 1901. Find a Peak Element II | 2D peak finding — binary search on columns, find row-max, compare to neighbors to discard half the columns |

---

## Pattern 3: Binary Search on Matrix

**Identify:** A 2D matrix with sorted structure, and the technique is either flattening, per-row search, staircase traversal, or value-domain search. Four distinct techniques depending on the structure — identify which applies before writing a single line.

- **Fully sorted as 1D** (LC 74): rows concatenate in sorted order. Treat as flattened array. `row = mid // cols`, `col = mid % cols`. Full O(log mn).
- **Rows independently sorted, no cross-row order** (Find row with max 1s): binary search *within each row* for the first 1 (lower bound), track which row gives the smallest such index. O(n log m) — not staircase, because there's no relationship between rows to exploit for elimination.
- **Row and column sorted independently, one continuous surface** (LC 240): binary search does not apply cleanly across the whole matrix. Use staircase search from top-right — go left if too large, go down if too small. O(m+n).
- **Value domain search** (GFG Median): binary search on the answer value, count elements ≤ mid across all rows using binary search per row. O(n log m · log(max−min)).

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 74. Search a 2D Matrix | Flatten to 1D — full binary search applies |
| 2 | Find row with maximum number of 1s | Per-row lower-bound search for the first 1 — no cross-row elimination |
| 3 | LC 240. Search a 2D Matrix II | Staircase from top-right — NOT standard binary search |
| 4 | GFG: Median in Row-Wise Sorted Matrix | Binary search on value domain — count ≤ mid across all rows |

---

## Pattern 4: Binary Search on Answer

**Identify:** You are not searching for a value in an array. You are searching for the **optimal value of the answer itself** — minimum speed, minimum capacity, fewest days, the minimized maximum sum of a partition, or the value closest to a target. The answer space is a range of integers. The feasibility check simulates a process against that candidate answer — but *how* it verifies feasibility varies more than it first appears.

**How to solve any problem in this pattern:**
1. Define `lo` and `hi` — the bounds of the answer space
2. Write `feasible(mid)` — simulate the process with answer = mid
3. Verify monotonicity — if mid works, mid+1 always works (or always fails)
4. Apply the lower bound template mechanically

**The feasibility function is the entire problem — but it comes in more than one shape:**

- **Running-sum accumulation** (Koko, Ship Packages, Allocate Books, Split Array Largest Sum): iterate once, keep a running total, reset/count when a threshold is crossed. This is the default shape and most problems in this pattern use it.
- **Counting via inclusion-exclusion** (LC 1201): "how many numbers ≤ mid satisfy a divisibility condition" is not a single running sum — it requires counting multiples of a, b, c and subtracting overlaps via their LCM (`count(mid) = mid/a + mid/b + mid/c - mid/lcm(a,b) - mid/lcm(b,c) - mid/lcm(a,c) + mid/lcm(a,b,c)`). Same binary-search shell, a genuinely different counting function inside — this is where people fluent in the running-sum shape still get stuck, because the "accumulate" step is now a formula, not a loop.
- **Two-pointer / subsequence verification** (LC 1898): feasibility isn't a sum at all — it's "if I remove these `mid` characters, is `p` still a subsequence of `s`?", checked with a two-pointer scan. Recognizing that `feasible(mid)` can be *any* monotonic check, not just an accumulator, is the transferable lesson here.
- **Minimizing distance-to-target, not a boolean threshold** (LC 1300): there's no true/false feasibility at all. You're searching for the value of `mid` that makes a monotonic function (the clipped sum) land closest to a target — the exit condition is "closest," not "first true." This is the one problem in this pattern where the lower-bound template needs a small twist: track the best answer seen, don't just converge `lo` to `hi`.

**Allocate Books, Painter's Partition, and Split Array Largest Sum are the identical problem wearing three names.** All three ask: "partition a sequence into k contiguous groups, minimizing the maximum group sum." Feasibility accumulates a running sum, greedily starts a new group when the running sum would exceed `mid`, and checks whether the number of groups needed is ≤ k. Do all three back to back specifically to notice they're the same recurrence — that recognition is worth more than any one of them individually.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 69. Sqrt(x) | Simplest parametric — largest `k` where `k*k <= x` |
| 2 | Find the Nth Root of an Integer | Direct generalization of Sqrt(x) — largest `k` where `k^n <= x` |
| 3 | LC 875. Koko Eating Bananas | feasible(k): can finish all piles in h hours at speed k? |
| 4 | LC 1011. Capacity to Ship Packages Within D Days | feasible(c): can ship all in d days at capacity c? |
| 5 | LC 1283. Find the Smallest Divisor | feasible(d): does sum of ceilings(arr[i]/d) ≤ threshold? |
| 6 | LC 2187. Minimum Time to Complete Trips | feasible(t): can all buses complete ≥ totalTrips by time t? |
| 7 | LC 1482. Minimum Number of Days to Make m Bouquets | feasible(d): can make m bouquets by day d? |
| 8 | BS-18. Allocate Books / Book Allocation | feasible(mid): can allocate books to m students with max pages ≤ mid? — same recurrence as the next two |
| 9 | BS-19. Painter's Partition | Identical recurrence to Allocate Books — different story, same feasibility function |
| 10 | LC 410. Split Array Largest Sum | Identical recurrence again — feasible(m): can split into k parts each with sum ≤ m? |
| 11 | LC 2064. Maximum Value at a Given Index in Bounded Array | feasible(v): can build array with peak v satisfying sum ≤ maxSum? |
| 12 | LC 1201. Ugly Number III | Counting via inclusion-exclusion over LCMs — not a running sum, a formula |
| 13 | LC 1898. Maximum Number of Removable Characters | Feasibility verified by a two-pointer subsequence scan, not an accumulator |
| 14 | LC 1300. Sum of Mutated Array Closest to Target | No boolean feasibility — minimize distance to target, track best-seen instead of converging to a threshold |

---

## Pattern 5: Minimax / Maximin via Binary Search

**Identify:** The problem asks to **minimize the maximum** or **maximize the minimum** value after placing or assigning elements under a gap or load constraint. The feasibility check makes placement decisions — greedily place each element and check if the required count fits within the constraint, or check whether a candidate gap is achievable.

**How to identify in an unseen problem:**
- The words "minimize the maximum distance/gap/load" or "maximize the minimum distance/gap/force" are nearly definitive signals
- The feasibility function asks "can I place all N elements — or add new ones — such that every consecutive gap is ≥ mid (or ≤ mid)?" — a placement question, not an accumulation question
- The search space is often inverted vs Pattern 4: for maximize-the-minimum problems, you search for the **largest feasible minimum** (upper bound template) rather than the smallest feasible maximum

**Aggressive Cows — the anchor, and the problem that most often gets confused with Pattern 4:** Place k cows into stalls to maximize the minimum distance between any two. Feasibility greedily places a cow at the first stall, then places the next cow at the first available stall at least `mid` away, counting how many cows fit. If count ≥ k, `mid` is achievable. This is a **placement** decision at every step — nothing is being accumulated — which is exactly the tell that separates this from Split Array Largest Sum despite both sounding like "min/max under a constraint."

**LC 774 — the same idea on real numbers, not integers:** Minimize Max Distance Between Gas Stations adds `k` new stations to existing ones to minimize the largest gap between adjacent stations. Feasibility counts, for a candidate gap `mid`, how many stations each existing gap needs (`floor(gap / mid)`), summed across all gaps, checked against `k`. Because the answer isn't necessarily an integer, this requires binary search on real numbers with a precision threshold (`while hi - lo > 1e-6`) instead of the integer lower-bound template — the mechanism is identical, only the loop's stopping condition changes.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Aggressive Cows | Anchor — maximize minimum gap; greedily place, count how many fit at distance ≥ mid |
| 2 | LC 1552. Magnetic Force Between Two Balls | Same greedy placement pattern as Aggressive Cows, different story |
| 3 | LC 2517. Maximum Tastiness of Candy Basket | Maximize minimum difference — same greedy placement pattern |
| 4 | LC 774. Minimize Max Distance Between Gas Stations | Minimize maximum gap — real-number binary search variant, feasibility counts stations needed per gap |
| 5 | LC 2528. Maximize the Minimum Powered City | Minimize maximum deficit — binary search on threshold + prefix sum + greedy |

---

## Pattern 6: Kth Element — Count ≤ Mid

**Identify:** Find the Kth smallest value across one or more sorted structures — a matrix, a multiplication table, pair distances, products of two arrays. Binary search on the **value domain**. For each candidate value `mid`, count how many elements across all structures are ≤ mid. If count ≥ k, answer ≤ mid; otherwise answer > mid.

**The counting function is the hard part — it varies by structure:**
- Sorted matrix: O(n) staircase count
- Multiplication table row i: `min(mid // i, n)` elements ≤ mid, sum across all rows
- Pair distances: sliding window on sorted array
- Products of two sorted arrays: case split on positive/negative signs

**Distinction from Pattern 7:** Here you have one or more sorted structures and you are counting. In Pattern 7, you have exactly two sorted arrays and you are partitioning — no counting involved.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 378. Kth Smallest Element in a Sorted Matrix | Count ≤ mid via O(n) staircase — binary search on value |
| 2 | LC 668. Kth Smallest Number in Multiplication Table | Count ≤ mid via row formula: `sum(min(mid//i, n))` |
| 3 | LC 719. Find K-th Smallest Pair Distance | Binary search on distance + sliding window count |
| 4 | LC 2040. Kth Smallest Product of Two Sorted Arrays | Binary search on value + count products ≤ mid with sign case handling |

---

## Pattern 7: Kth Element — Partition Binary Search

**Identify:** Given **exactly two sorted arrays**, find the median or a specific order statistic. Binary search on a **split index** in the smaller array — find the partition where the combined left halves equal the combined right halves in size. No counting involved. Verification is purely a boundary element comparison.

**The core invariant:**
```
partition1 + partition2 = (m + n + 1) // 2

correct partition when:
    maxLeft1 <= minRight2  AND  maxLeft2 <= minRight1
```

**This is the hardest problem in this entire sheet.** Do it only after Pattern 6 is fully solid — Pattern 6 builds the intuition that binary search can operate on a value domain, which makes the partition approach feel less mysterious.

**Kth Element of Two Sorted Arrays — the direct generalization of median:** Once LC 4 (median) is solid, this asks the same partition question for an *arbitrary* k, not just the middle. The invariant generalizes to `partition1 + partition2 = k`, with the same four-boundary-comparison check. Doing this right after LC 4 turns "I memorized median of two sorted arrays" into "I understand partition binary search," which is the actual transferable skill.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 658. Find K Closest Elements | Binary search on window start — find best starting index for k-element window |
| 2 | LC 4. Median of Two Sorted Arrays | Partition binary search — split where combined left = combined right in size |
| 3 | Kth Element of Two Sorted Arrays | Generalizes LC 4's partition invariant to `partition1 + partition2 = k` for arbitrary k |

---

## Pattern 8: Binary Search on Events / Versioned Data

**Identify:** Data is stored at timestamps or version numbers. You need the most recent state at or before a given time. The stored data is implicitly sorted by time — apply upper bound on that sorted sequence.

**Why this is its own pattern:** It is primarily a design + retrieval pattern. The interview tests two skills simultaneously: (1) choosing the right storage structure — sorted list of `(timestamp, value)` pairs, and (2) applying the correct bound direction — **upper bound**, meaning last timestamp ≤ query. Getting the bound direction wrong is the most common mistake.

**LC 528 note:** Random Pick with Weight is included here because the technique is structurally identical — build a prefix sum array (implicitly sorted and monotonically increasing), then find the first prefix ≥ a random value via lower bound. The binary-search-on-prefix retrieval is the same skill as timestamp retrieval.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 528. Random Pick with Weight | Build prefix sum array — lower bound for first prefix ≥ random value |
| 2 | LC 981. Time Based Key-Value Store | Store `(timestamp, value)` sorted — upper bound for latest timestamp ≤ query |
| 3 | LC 1146. Snapshot Array | Store `(snap_id, value)` per index — upper bound for latest snap_id ≤ query |

---

## Out of Scope for This Sheet (Flagged, Not Forgotten)

**LC 1044. Longest Duplicate Substring** — binary search on substring length, verified with Rabin-Karp rolling hash to detect duplicates in O(n) per check. This is a genuine binary-search-on-answer problem, but the verification step depends on string hashing, which doesn't have its own sheet yet. Consistent with how the BST and Prefix Sum sheets flag Fenwick Tree/Segment Tree as out-of-scope rather than silently omitting them — revisit this once a String Hashing sheet exists.

---

## Final Summary

| Pattern | Problems | Core Technique |
|---|---|---|
| Standard Binary Search | 13 | Lower/upper bound, floor/ceil, derived monotonic formulas, and finding bounds when unknown |
| Binary Search on Semi-Sorted Space | 9 | Local structural property enables half-discard, in 1D or 2D |
| Binary Search on Matrix | 4 | Flatten vs per-row vs staircase vs value domain — four distinct cases |
| Binary Search on Answer | 14 | Feasibility via accumulation, inclusion-exclusion counting, subsequence match, or closest-to-target |
| Minimax / Maximin | 5 | Feasibility via placement — iterate and place greedily, integer or real-valued |
| Kth Element — Count ≤ Mid | 4 | Binary search on value domain, count across structures |
| Kth Element — Partition | 3 | Binary search on split index, boundary comparison |
| Binary Search on Events | 3 | Sorted timestamp storage + bound retrieval |
| **Total** | **~55 problems** | |

---

## How to Use This Sheet

**Pattern 1 → 2 is mandatory order.** The `lo < hi` invariant and bound direction must be automatic before touching semi-sorted arrays. Do the Lower Bound/Upper Bound theory item first — every later pattern quietly assumes it. Floor/Ceil and Exponential Search are small additions, but both are common enough as standalone interview questions that skipping them leaves a real gap.

**Patterns 4 and 5 are the highest interview ROI, and the ones most prone to the exact mix-up you caught.** Before starting either, say the one-line test out loud: "does my feasibility function accumulate, or does it place?" Do the three-name problem (Allocate Books / Painter's Partition / Split Array Largest Sum) back to back in Pattern 4 specifically to lock in that they're one recurrence, then do Aggressive Cows in Pattern 5 immediately after, specifically because it's the problem most likely to get dragged into Pattern 4 by the "minimize/maximize" wording alone.

**Within Pattern 4, treat the last three problems (LC 1201, LC 1898, LC 1300) as a second wave, done only after the running-sum shape is fully automatic.** They all still binary search on an answer, but each breaks a different assumption you'll have built from the first eleven problems — that feasibility is a running sum, that feasibility is boolean at all, and that you're always converging to a threshold rather than a closest value. Meeting all three assumptions broken at once, without having drilled the standard shape first, makes them feel unrelated to the pattern instead of a natural extension of it.

**Pattern 6 before Pattern 7 without exception.** Count ≤ Mid builds the intuition that binary search can operate on a value domain. LC 4's partition approach — and its generalization to Kth Element of Two Sorted Arrays — will feel approachable only after that foundation is solid.

**Pattern 8 is often asked as a system design mini-problem.** Knowing to store sorted `(timestamp, value)` pairs and retrieve with the correct bound is the complete answer.