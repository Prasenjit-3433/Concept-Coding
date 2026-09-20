# Sorting DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Same discipline as the last four sheets. Sorting is even more over-subscribed than Hashing — nearly every hard problem in your other sheets *uses* a sort as a first step, but that doesn't make it a sorting problem. The bar for belonging here: the **sorting algorithm itself** (its mechanism, or a modification of its mechanism) has to be the thing being tested — not "sort, then do something else clever" where the something-else is the real lesson.

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 75. Sort Colors | Two Pointers sheet, Pattern 3 (Dutch National Flag) |
| LC 41. First Missing Positive | Arrays sheet, Pattern 3 (Cyclic Sort) |
| LC 220. Contains Duplicate III | BST/Ordered Set sheet, Pattern 8 (Ordered Set) |
| LC 148. Sort List | Linked List sheet, Pattern 4 (Merge Sort on a linked list) |
| LC 179. Largest Number | Greedy sheet, Pattern 1 (custom comparator, exchange-argument proof) |
| LC 347. Top K Frequent Elements | Heap sheet, Pattern 2 (Top K) — bucket-sort alternative discussed here in Pattern 2 |
| LC 215. Kth Largest Element in an Array | Heap sheet, Pattern 2 (min-heap of size k) — quickselect's full treatment lives here instead |

Two of these deserve a word on *why* they're deferred rather than dropped: LC 179 and LC 75 are genuinely sorting-*adjacent* — a custom comparator and a three-way partition, respectively — but their entire pedagogical weight (the exchange-argument proof, the Dutch-flag three-pointer mechanic) is already built out in detail in sheets you consider done. Re-deriving them here would dilute, not reinforce.

That leaves a tight, genuinely sorting-specific set: the non-comparison-based sorts (counting, bucket, radix) and the two comparison-based sorts whose *modified* form solves problems you can't otherwise touch efficiently (merge sort for counting, quickselect for order statistics).

---

## The Core Mental Model — Before Any Pattern

Every general-purpose comparison sort is bounded by `O(n log n)` — that's an information-theoretic floor, not an implementation detail, and it's *why* the non-comparison sorts exist at all.

**The one-line test for which family you're in:**
> Does the problem give you a **bounded, known range of values** (or a small alphabet, or values tightly tied to array length)? → Non-comparison sort (Counting / Bucket / Radix) can beat `O(n log n)`.
> Do you need a **custom notion of order**, not numeric comparison? → Custom Comparator.
> Do you need to **count cross-index relationships** (inversions, smaller-after-self, range sums) rather than just produce a sorted array? → Modified Merge Sort.
> Do you need only the **kth order statistic**, not a fully sorted array? → Quickselect — don't pay for a full sort when you only need one position.

This last point is the single most valuable interview instinct in this entire sheet: **sorting the whole array to answer a "kth" question is correct but is doing more work than the question asked for.** Every pattern below either exploits a structural shortcut past `O(n log n)`, or exploits the fact that you don't need the *whole* sorted array to answer what's actually being asked.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Counting Sort & Rank Mapping | small/bounded value range, "relative sort", "custom order string maps to rank" |
| Bucket Sort | "maximum gap between sorted elements", frequency-bucketed output, pigeonhole placement |
| Custom Comparator Sorting | sort by a derived rule, not raw value — "rank teams", "custom sort string" |
| QuickSort / Quickselect | "kth largest/smallest", full sort not required, only one order statistic needed |
| Modified Merge Sort for Cross-Index Counting | "count of smaller/larger after self", "count inversions", "count range sums", "count pairs satisfying an inequality" |

---

## Pattern 1: Counting Sort & Rank Mapping

**Identify:** Values live in a small, known, bounded range (or map to one via a given ordering rule), and you need either a fully sorted output or a query "how many elements are ≤ this value" — both of which counting sort answers in `O(n + k)`, beating any comparison sort's `O(n log n)` floor.

**Theory — the core mechanism:** Build a count array indexed by value (size = range of possible values). A single pass over the input increments counts; a single pass over the count array (optionally converted to a running prefix sum) reconstructs sorted order or answers rank queries directly. The entire benefit comes from replacing "compare pairs of elements" with "look up a value's position by its own identity" — which only works because the range is small enough to index directly.

**LC 1122 (Relative Sort Array) — the anchor, rank mapping as the whole solution:** You're not sorting by numeric value at all — you're sorting `arr1` so its elements appear in the *order dictated by* `arr2`. Build a hashmap from value → its rank in `arr2`, then sort `arr1` using that rank as the comparator key (with any value absent from `arr2` sorted normally, at the end, by its own numeric value). This is counting sort's core idea — precompute a value's position instead of comparing — applied through a hashmap instead of a raw array, because the "range" here is `arr2`'s own arbitrary order, not a numeric interval.

**LC 2191 (Sort the Jumbled Numbers) — the same rank-mapping idea, one layer of indirection deeper:** Each number must first be **transformed** (each digit remapped through a given permutation) before its rank can be determined, then the *original* numbers are sorted according to their transformed values. The insight worth naming explicitly: compute the transformed key for every element first, pair it with the original index, sort pairs by the transformed key, then read off original values in that order — never try to sort in place while simultaneously transforming, since that conflates the sort key with the thing being sorted.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Theory: Counting Sort | O(n+k) via a count array indexed by value — the mechanism every problem here specializes |
| 2 | LC 1122. Relative Sort Array | Rank map from a second array's order — comparator keyed on precomputed rank, not raw value |
| 3 | LC 2191. Sort the Jumbled Numbers | Transform-then-sort — compute a derived key per element before comparing, never sort the raw value directly |

---

## Pattern 2: Bucket Sort

**Identify:** You need to sort or query values by placing them into a fixed number of "buckets," where either (a) the range of possible values combined with pigeonhole reasoning guarantees a useful property about gaps between elements once sorted, or (b) you're bucketing by **frequency** rather than value, since frequency is bounded by array length regardless of how large the values themselves are.

**LC 164 (Maximum Gap) — the anchor, and the pigeonhole insight that makes it O(n):** Finding the maximum gap between consecutive elements in sorted order looks like it needs a full `O(n log n)` sort first. The bucket-sort trick avoids that: with `n` elements spanning a range `[min, max]`, distribute them into `n-1` buckets of size `(max-min)/(n-1)`, tracking only each bucket's local min and max (not every element in it). The critical pigeonhole fact: **the maximum gap can never occur between two elements inside the same bucket** — by construction, if two elements land in the same bucket, at least one other bucket must be empty, and the true maximum gap must span across that empty bucket. So the answer only ever needs comparing each bucket's max to the *next non-empty* bucket's min — never a full sort.

**LC 451 (Sort Characters by Frequency) — frequency as the bucket index:** Count each character's frequency, then place each character into a bucket indexed by *that frequency* (bucket index range is `[0, n]`, since frequency can never exceed the string's length regardless of alphabet size). Reading buckets from highest index to lowest, and emitting each character its frequency-many times, produces the frequency-sorted output in `O(n)` — no comparison sort needed because "sort by frequency" and "index an array by frequency" are the same operation once you notice frequency is bounded by `n`, not by the value range.

**LC 692 (Top K Frequent Words) — the same frequency-bucketing idea, plus a secondary tie-break:** Structurally identical to LC 451's bucketing, but when multiple words share a frequency, they must additionally be ordered lexicographically — so each bucket holds a small sorted (or heap-managed) group rather than a flat list. This is worth doing right after LC 451 specifically to see that "bucket by frequency" doesn't disappear just because a secondary ordering rule is layered on top — it just means each bucket needs slightly richer internal structure.

**Cross-reference — the heap alternative, not re-taught here:** LC 347 (Top K Frequent Elements) is the same frequency-bucketing idea *without* a tie-break requirement, and its full treatment (including the min-heap-of-size-k alternative) already lives in the Heap sheet, Pattern 2. Worth knowing both approaches exist for the *same* problem: bucket sort is `O(n)`, the heap approach is `O(n log k)` — bucket sort wins asymptotically here specifically because frequency is bounded by `n`, which is exactly this pattern's identifying signal.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 164. Maximum Gap | Pigeonhole bucketing — max gap never occurs within a bucket, only across an empty one |
| 2 | LC 451. Sort Characters by Frequency | Bucket index = frequency (bounded by n) — read buckets high-to-low |
| 3 | LC 692. Top K Frequent Words | Same frequency-bucketing + lexicographic tie-break within each bucket |
| — | LC 347. Top K Frequent Elements *(cross-ref: Heap sheet, Pattern 2)* | Same bucketing idea without a tie-break — heap alternative fully treated there |

---

## Pattern 3: Custom Comparator Sorting

**Identify:** The elements need to be ordered by a rule that isn't "compare the raw values" — a derived score, a positional-count table, or a domain-specific rank. The sorting *algorithm* here is a black box (any `O(n log n)` sort works); the actual skill is correctly deriving the comparator function itself.

**LC 791 (Custom Sort String) — the gentle entry point:** Characters in `s` must appear in the order dictated by `order`; anything not in `order` goes at the end in any position. Build a rank map from `order` (character → its index), then sort `s`'s characters using that rank as the comparator key — structurally the same rank-mapping idea as Pattern 1's LC 1122, just applied to characters instead of array elements. Worth noticing this overlap: "custom order via precomputed rank" is one idea that resurfaces as both a counting-sort-flavored technique (Pattern 1, when the range is numeric/bounded) and a plain comparator (here, when you'd rather just call the built-in sort with a custom key).

**LC 1366 (Rank Teams by Votes) — the anchor, and the harder derivation:** Each team's comparator key isn't a single number — it's the **entire vector of vote-counts per position** (how many first-place votes, how many second-place votes, etc.), compared lexicographically, with the team's own name as the final tiebreaker. Building this comparator requires first tallying, for every team, a full position-count array (a counting-sort-style precomputation feeding into a comparator, not into a bucket placement) — and *then* sorting teams using lexicographic comparison over those vectors. The transferable insight: when "the rule" is genuinely multi-criteria with a strict priority order (most first-place votes wins; ties broken by second-place votes; ties broken by name), the comparator itself needs to encode that entire priority chain, not just the most obvious single criterion.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 791. Custom Sort String | Rank map from a reference order, used directly as a sort comparator key |
| 2 | LC 1366. Rank Teams by Votes | Multi-criteria comparator — lexicographic comparison over a full per-team vote-count vector, name as final tiebreak |
| — | LC 179. Largest Number *(cross-ref: Greedy sheet, Pattern 1)* | Custom comparator (`a+b` vs `b+a`) — full exchange-argument proof already treated there |

---

## Pattern 4: QuickSort / Quickselect

**Identify:** You need the kth largest/smallest element — or a full array sort implemented from scratch, which interviewers do still ask for directly. If the question only needs *one* order statistic, sorting the entire array is provably more work than necessary; quickselect answers it in `O(n)` average time by only ever recursing into the *one* partition that could contain the answer, discarding the other partition entirely without examining it.

**LC 912 (Sort an Array) — the implementation anchor:** Asked directly often enough to be worth having both merge sort and quicksort implementations cold, not just conceptually understood. This is also the natural place to internalize quicksort's partition step precisely, since quickselect (below) reuses that exact partition step and *only* that step.

**Why quickselect beats a full sort for "kth" questions — the core argument:** After one partition step around a pivot, every element is provably on the correct side of where it would land in a fully sorted array — the pivot itself lands at its **final sorted position**. If that position happens to be the `k` you're looking for, you're done immediately; if not, you know with certainty which single side of the partition the answer lives in, and you never need to touch the other side again. Repeating this halves (on average) the remaining work each time, giving `O(n)` average time versus `O(n log n)` for a full sort — you are quite literally doing less work by design, not by luck.

**LC 215 (Kth Largest Element in an Array) — the anchor for quickselect itself:** Partition around a pivot exactly as in quicksort; compare the pivot's final position against `k`; recurse into only the relevant side. *(The min-heap-of-size-k alternative to this exact problem is fully treated in the Heap sheet, Pattern 2 — know both, since interviewers frequently ask for the tradeoff: heap gives `O(n log k)` worst-case guaranteed, quickselect gives `O(n)` average but `O(n²)` worst-case without a randomized pivot.)*

**LC 1985 (Find the Kth Largest Integer in the Array) — the same mechanism, comparator swapped out:** Identical quickselect skeleton to LC 215, except the input is an array of **numeric strings** that can exceed standard integer range, so the comparator must compare by string length first, then lexicographically for equal lengths, instead of raw numeric comparison. Worth doing immediately after LC 215 specifically to confirm that quickselect's partition logic is entirely comparator-agnostic — swapping the comparator is the only change needed.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 912. Sort an Array | Implement merge sort and quicksort from scratch — the partition step here is reused directly by quickselect |
| 2 | LC 215. Kth Largest Element in an Array | Quickselect — partition, compare pivot's final position to k, recurse into one side only |
| 3 | LC 1985. Find the Kth Largest Integer in the Array | Same quickselect skeleton, custom string-length-then-lexicographic comparator |
| — | LC 75. Sort Colors *(cross-ref: Two Pointers sheet, Pattern 3)* | A specialized single-pass partition (Dutch National Flag) — related in spirit, fully treated there |

---

## Pattern 5: Modified Merge Sort for Cross-Index Counting

**Identify:** The question asks you to **count relationships between pairs of elements** across the array — how many pairs are "out of order" (inversions), how many elements after position `i` are smaller than `nums[i]`, how many pairs satisfy a derived inequality, or how many range sums fall in a target interval. A brute-force nested loop is `O(n²)`; merge sort's divide-and-conquer structure, with one extra counting step added during the merge, answers all of these in `O(n log n)` — because merge sort already visits every cross-half pair implicitly while merging, and counting them costs nothing extra if done at the right moment.

**Theory — why the merge step is exactly where counting happens:** When merging two already-sorted halves, at the moment you're about to take an element from the right half (because it's smaller than the current left-half candidate), you know — for free, without any extra comparisons — that this right-half element is smaller than *every remaining element* in the left half, not just the one currently being compared. That single fact, multiplied across the whole merge, is what turns "count inversions" from an `O(n²)` pairwise check into an `O(n log n)` byproduct of sorting.

**Inversion Count (GFG) — the anchor:** An inversion is a pair `(i, j)` with `i < j` but `arr[i] > arr[j]`. During the merge step, every time an element is taken from the right half ahead of a remaining left-half element, add the **count of all remaining left-half elements** (not just one) to the inversion total — this is the exact "free information" described above.

**LC 493 (Reverse Pairs) — the same skeleton, a shifted comparison condition:** Counts pairs where `arr[i] > 2 * arr[j]` (note the factor of 2) instead of a plain inversion. Because the comparison condition is different from the merge's own ordering condition, this requires a **separate counting pass** through both sorted halves *before* the normal merge step runs — you cannot piggyback the count directly onto the merge itself the way plain inversion counting does, since `2*arr[j]` isn't what the merge is comparing by. Recognizing when the target condition matches the merge's natural comparison (free counting, as in inversions) versus when it doesn't (a separate pass needed first, as here) is the actual transferable lesson.

**LC 315 (Count of Smaller Numbers After Self) — inversion counting with position tracking:** The same merge-sort inversion-counting mechanism, but since the *answer* is per-original-index (not a single total), the merge must carry original indices alongside values throughout, updating a per-index result array whenever the "free counting" moment occurs. *(Cross-reference: this exact problem is also solved by an augmented order-statistics BST in the BST/Ordered Set sheet, Pattern 2 — both are valid `O(n log n)` approaches; merge sort is often the cleaner one to code live, since no tree-balancing concerns exist.)*

**LC 327 (Count of Range Sum) — prefix sums feeding the same inversion-counting skeleton:** Counting range sums that fall within `[lower, upper]` reduces to counting, for every pair of prefix-sum indices `(i, j)`, whether `prefix[j] - prefix[i]` falls in that range — which is inversion counting generalized from a single inequality to a **range** of acceptable differences. During the merge step, for each left-half prefix sum, use two pointers sweeping through the right half to count how many right-half prefix sums fall within `[left + lower, left + upper]` — the same "free counting during merge" instinct as inversions, extended from one comparison to a two-pointer range count.

**LC 2426 (Number of Pairs Satisfying Inequality) — the same family, disguised by algebra:** The condition `nums1[i] - nums1[j] <= nums2[i] - nums2[j] + diff` rearranges into `(nums1[i] - nums2[i]) <= (nums1[j] - nums2[j]) + diff` — meaning if you first transform the input into a single array of differences (`nums1[k] - nums2[k]`), the problem becomes exactly LC 493's shifted-inversion-counting pattern on that transformed array. Recognizing that an algebraic rearrangement collapses a two-array problem into a single-array member of a pattern you already know is a real transferable skill, not busywork — this is the disguised-problem recognition this entire sheet series has been building toward.

**LC 1649 (Create Sorted Array through Instructions) — inversion counting run incrementally, not on the final array:** As each instruction is inserted one at a time, the cost is `min(count of already-inserted elements less than it, count already-inserted elements greater than it)` — which is exactly "count of smaller/greater elements so far," the same relationship LC 315 counts, but needed incrementally *during* construction rather than once at the end. A Binary Indexed Tree (Fenwick Tree) is the typical clean implementation for this online/incremental variant; the merge-sort approach from LC 315 answers the offline version efficiently but doesn't naturally extend to "count so far, one insertion at a time" the way a Fenwick Tree does. *(Flagged consistent with the BST and Binary Search sheets' existing Fenwick Tree references — full BIT treatment belongs in a separate advanced-structures sheet, not here.)*

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Count Inversions in an Array | Anchor — during merge, a right-half element taken early is smaller than ALL remaining left-half elements, free to count |
| 2 | LC 493. Reverse Pairs | Same skeleton, shifted comparison (`arr[i] > 2*arr[j]`) — needs a separate counting pass before the merge, not piggybacked on it |
| 3 | LC 315. Count of Smaller Numbers After Self | Same inversion counting, indices tracked alongside values for a per-position answer *(cross-ref: BST sheet, Pattern 2, alternate technique)* |
| 4 | LC 327. Count of Range Sum | Prefix sums + two-pointer range count during merge — inversion counting generalized to a range, not a single inequality |
| 5 | LC 2426. Number of Pairs Satisfying Inequality | Algebraic rearrangement collapses two arrays into one difference-array — reduces directly to LC 493's pattern |
| 6 | LC 1649. Create Sorted Array through Instructions | Same relationship as LC 315, needed incrementally — typically Fenwick Tree in practice, merge sort only for the offline version |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Counting Sort & Rank Mapping | 3 (incl. theory) | O(n+k) via bounded-range indexing, or rank-map when the "range" is an arbitrary given order |
| Bucket Sort | 3 (+1 cross-ref) | Pigeonhole placement (value buckets) or frequency-as-index (bounded by n regardless of value range) |
| Custom Comparator Sorting | 2 (+1 cross-ref) | Deriving the right comparator is the skill; the sort itself is a black box |
| QuickSort / Quickselect | 3 (+1 cross-ref) | Partition step guarantees pivot's final position — recurse into only one side for O(n) average |
| Modified Merge Sort for Cross-Index Counting | 6 | Free counting during merge — inversions, range sums, and their algebraic disguises |
| **Total** | **17 new + 3 cross-referenced** | |

---

## How to Use This Sheet

**Patterns 1 and 2 can run in parallel — both are non-comparison sorts, but the recognition trigger differs.** Counting sort/rank-mapping fires when there's a *given* or *derivable* ordering to index by (Pattern 1); bucket sort fires when pigeonhole reasoning about *gaps* or *frequency bounds* is what unlocks the speedup (Pattern 2). Confusing "the range is bounded" (which could justify either) with "the specific structural guarantee that makes skipping comparisons safe" (which differs between the two) is the trap — say out loud which guarantee you're relying on before coding either.

**Pattern 4 is the highest-frequency pattern in this sheet for interviews, and the one-line test at the top of this sheet is the whole recognition skill.** Every time a problem asks for a "kth" anything, the reflex should be "do I need the full sorted array, or just this one position" — defaulting to a full sort when quickselect applies is a correct-but-suboptimal answer that a strong interviewer will immediately follow up on.

**Pattern 5 should be done in the exact order shown, and only after Pattern 4's quicksort partition (LC 912) is completely solid** — the merge step, not the partition step, is what's being modified here, but you need to be fluent in divide-and-conquer array algorithms generally before layering a counting side-channel on top. Do the plain Inversion Count theory problem cold before touching LC 493 — the moment you see *why* free counting works for plain inversions, LC 493's need for a *separate* counting pass (because its condition doesn't match the merge's natural comparison) becomes a meaningful contrast rather than an arbitrary extra step. LC 2426 is deliberately placed last because recognizing the algebraic reduction to LC 493 is the actual test of whether the pattern has generalized in your head or is still tied to the literal "inversion" framing.

**The Fenwick Tree flag in LC 1649 is intentional, not a cop-out.** Consistent with how your BST and Binary Search sheets already flag Fenwick/Segment Trees as a known future gap, merge-sort-based counting is powerful but fundamentally *offline* — it needs the whole array up front. The moment a problem needs the same kind of count *incrementally*, that's the signal a Fenwick Tree is the intended tool, not a sign that merge sort has failed you.
