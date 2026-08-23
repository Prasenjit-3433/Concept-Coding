# Prefix Sum DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

A prefix sum trades O(n) per-query recomputation for O(n) one-time preprocessing plus O(1) per query. The entire idea in one line:

```
prefix[i] = arr[0] + arr[1] + ... + arr[i-1]
sum(l, r) = prefix[r+1] - prefix[l]
```

**Why this works at all — the property every pattern below depends on:** the aggregate must be **invertible**. Addition is invertible (subtraction undoes it), which is why prefix *sum* works. XOR is invertible (`a ^ a = 0`), which is why prefix *XOR* works identically *(full treatment: Bit Manipulation sheet, Pattern 3 — don't duplicate it here)*. Multiplication is invertible too, with one landmine — division by zero — which is exactly why Pattern 5 below needs a workaround instead of a plain prefix-product array.

**What does NOT work with prefix sum, and why that matters:** min and max are **not invertible** — you cannot "subtract out" a minimum the way you subtract out a sum. Range-minimum-query problems need a Sparse Table or Segment Tree instead. If you ever catch yourself trying to build a "prefix min array" to answer range-min queries, that instinct is wrong — recognizing this boundary is as important as knowing the technique itself.

---

## Prefix Sum vs Difference Array — inverse operations of each other

These two techniques are mirror images, and confusing them is the most common conceptual error in this topic:

- **Prefix Sum:** the array is fixed; you answer many **range sum queries** in O(1) each after O(n) preprocessing.
- **Difference Array:** the array is being **updated** with many range-increment operations; you apply all updates in O(1) each, then take a **single prefix sum pass at the end** to materialize the final array in O(n).

> **One-line test:** Are you querying ranges of a fixed array? → Prefix Sum. Are you applying many range *updates* and only need the final result once? → Difference Array (Pattern 3).

## Prefix Sum vs Sliding Window — when prefix sum wins

Sliding Window (see that sheet's Monotonicity section) requires non-negative values for its shrink/expand logic to stay correct. The moment **negative numbers** enter a subarray-sum problem, sliding window silently breaks, and prefix sum (usually paired with a hashmap) becomes the correct tool — this is precisely why LC 560 needs a hashmap while LC 209 (all-positive) gets away with a plain two-pointer window.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| 1D Prefix Sum Fundamentals | "range sum query", "pivot index", "equal left/right sum", "split into parts with equal sum" |
| Prefix Sum + HashMap | "subarray sum equals k", "divisible by k", "equal 0s and 1s", "longest/count subarrays summing to X" |
| Difference Array — Range Update & Event Counting | "range increment", "bookings/passengers over ranges", "apply k operations to ranges then read final array" |
| 2D Prefix Sum | "range sum query 2D", "submatrix sum", "increment submatrix" |
| Product Prefix (Prefix Product) | "product except self", "product of last k numbers" |
| Prefix Sum + Binary Search | "split into three parts", "kth prefix ≥ value", "minimize difference between two halves" |

---

## Pattern 1: 1D Prefix Sum Fundamentals

**Identify:** You need to answer range-sum queries efficiently, or compare one part of the array's sum against another part's sum (pivot point, equal partition, balanced split). No hashmap, no updates — just precompute once, query in O(1).

**LC 724 as the anchor for the whole "balance point" family:** `leftSum == totalSum - leftSum - arr[i]` at the pivot. Once this single equation clicks, LC 1013 and LC 2270 stop looking like separate problems — they're the same equation applied to a three-way split and a two-way split respectively.

**LC 1013 vs LC 2270 — same instinct, different constraint shape:** LC 1013 asks whether the *entire* array can split into three equal-sum parts (compare running sum against `totalSum / 3`, twice). LC 2270 counts *how many* valid two-way split points exist where the left prefix sum equals the right suffix sum count-of-elements-condition — same running-total-comparison mechanic, just a different equality to check at each index.

**LC 1508 (optional, advanced) — prefix sum used to *generate* a new array, not just query one:** Build all `n(n+1)/2` subarray sums using prefix sums in O(n²), sort them, then answer a range query on *that* sorted list. Worth doing once fundamentals are automatic — it shows prefix sum as a preprocessing step feeding into an entirely different technique (sorting), not the final answer itself.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 303. Range Sum Query — Immutable | Core template — precompute once, answer each query via subtraction |
| 2 | LC 1480. Running Sum of 1d Array | Prefix sum array *is* the answer — simplest possible entry point |
| 3 | LC 1732. Find the Highest Altitude | Running sum as elevation tracking — prefix sum with no query layer at all |
| 4 | LC 724. Find Pivot Index | `leftSum == totalSum - leftSum - arr[i]` — anchor for the balance-point family |
| 5 | LC 1013. Partition Array Into Three Parts With Equal Sum | Same balance equation, applied twice for a three-way split |
| 6 | LC 2270. Number of Ways to Split Array | Same balance equation, counted across every valid split point |
| — | LC 1508. Range Sum of Sorted Subarray Sums *(optional, advanced)* | Prefix sum generates all subarray sums, then sort + range-query on the result |

---

## Pattern 2: Prefix Sum + HashMap (Subarray Sum Family)

**Identify:** You need to count or find subarrays whose sum satisfies an exact condition — equals k, is divisible by k, has equal counts of 0s and 1s. Because negative numbers or the "exact" (not "at most") condition break sliding window's monotonicity, you instead store **every prefix sum value seen so far** in a hashmap, and check whether `currentPrefix - target` has already occurred.

**The universal insight — restated precisely:** `sum(l+1, r) = prefix[r] - prefix[l]`. If you want this to equal `k`, you need `prefix[l] = prefix[r] - k`. So at each `r`, look up `prefix[r] - k` in a hashmap of prefix sums seen so far (with their counts, or their earliest index, depending on whether you're counting or finding length). This single lookup is the entire pattern — every problem below is a variation of what you store as the hashmap's value and what target you compute.

**LC 560 as the anchor:** HashMap of `prefixSum → count of times seen`. Initialize with `{0: 1}` to correctly count subarrays starting at index 0.

**LC 974 — the modular twist:** Instead of storing raw prefix sums, store `prefixSum % k`. Two prefixes with the same remainder mean the subarray between them is divisible by k — same lookup mechanic, different key transformation. Watch for negative remainders in languages where `%` can return negative; normalize with `((x % k) + k) % k`.

**LC 525 / LC 1524 — the same trick, a different encoding of the target property:** Treat `0` as `-1` (LC 525) or track parity directly (LC 1524) — both reduce "equal count of two categories" or "odd/even sum" to the identical prefix-sum-equality lookup as LC 560.

**LC 523 — the divisibility check plus two extra constraints:** Same `prefix % k` idea as LC 974, but requires the subarray to have length ≥ 2 and handles `k = 0` as a special case (checks for two consecutive zeros instead of the modular trick).

**LC 325 — storing the *earliest* index instead of a count:** When you need the *longest* subarray summing to k rather than the *count* of such subarrays, store `prefixSum → earliest index it occurred at` (only the first occurrence matters, since a later occurrence of the same prefix can only produce a shorter subarray).

**LC 1546 — greedy + the counting trick combined:** Find the maximum number of *non-overlapping* subarrays summing to target. Use the LC 560 hashmap trick to detect a valid ending point, but greedily cut immediately when found and reset the hashmap — this greedy-cut argument (cutting as early as possible never costs you a future valid subarray) is the one new idea layered on top of the base technique.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 560. Subarray Sum Equals K | Anchor — hashmap of `prefixSum → count`, look up `prefix[r] - k` |
| 2 | LC 325. Maximum Size Subarray Sum Equals k | Store `prefixSum → earliest index` instead of count — maximizes length |
| 3 | LC 1524. Number of Sub-arrays With Odd Sum | Parity of prefix sum + hashmap of even/odd counts |
| 4 | LC 974. Subarray Sums Divisible by K | Store `prefixSum % k` — same lookup, modular key |
| 5 | LC 523. Continuous Subarray Sum | Same modular trick + length ≥ 2 constraint + `k=0` edge case |
| 6 | LC 525. Contiguous Array | Treat 0 as -1 — reduces "equal 0s and 1s" to prefix-sum-equality lookup |
| 7 | LC 1546. Maximum Number of Non-Overlapping Subarrays With Sum Equals Target | LC 560's lookup + greedy immediate-cut-and-reset |

---

## Pattern 3: Difference Array — Range Update & Event Counting

**Identify:** You have many operations of the form "add a value to every element in range `[l, r]`," and you only need to read the final array once, after all updates are applied. Naively applying each update is O(n) per operation; a difference array makes each update O(1) and defers the O(n) cost to a single final prefix-sum pass.

**The universal template — memorize this exactly:**
```
diff = [0] * (n + 1)
for (l, r, val) in updates:
    diff[l] += val
    diff[r + 1] -= val

result = prefix_sum(diff)[:n]   # one pass, O(n) total regardless of update count
```

**Why this is the "inverse" of Pattern 1, made concrete:** Pattern 1 starts with a fixed array and derives a prefix sum to answer queries. Here you start with the *increments themselves* and derive the final array by prefix-summing the increments — the roles of "given" and "derived" are swapped.

**LC 1109 as the true anchor:** Each flight booking `[first, last, seats]` is a range-increment. Apply `diff[first] += seats`, `diff[last+1] -= seats` for every booking, then prefix-sum once at the end. This is the cleanest possible statement of the technique — no capacity check, no coordinate compression, nothing else layered on top.

**LC 1094 (Car Pooling) as a direct extension — cross-referenced, not re-taught here:** Identical difference-array mechanic to LC 1109, with one added check — after prefix-summing, verify no point in the array exceeds vehicle capacity. *(Full write-up: Intervals sheet, Pattern 5 — do LC 1109 here first, since it's the technique in its purest form, then recognize LC 1094 as "that, plus a capacity check.")*

**LC 2848 — the simplest possible version, no even prefix-sum needed:** Because ranges are small integers, mark `+1`/`-1` at boundaries directly on a small array and prefix-sum once — worth doing as the easiest on-ramp before LC 1109's slightly larger bookkeeping.

**LC 1893 — difference array answering a boolean coverage question:** Instead of summing values, mark coverage `+1`/`-1` per range, prefix-sum, and check that every queried point has count `≥ 1`. Same mechanism, boolean interpretation of the result instead of a numeric one.

**LC 2381 (Shifting Letters II) — signed difference array:** Each shift direction contributes `+1` or `-1` at the range boundaries instead of a flat value. After prefix-summing, the *net* value at each index (which can be negative) tells you the net shift direction and magnitude — same template, the "value" being accumulated is a direction rather than a quantity.

**LC 1589 — difference array used to build a *usage-frequency* array, then paired with greedy sorting:** Build a difference array marking how many query-ranges cover each index, prefix-sum to get a per-index "how often is this position summed over" frequency. Then sort both this frequency array and the input array in ascending order and pair the largest frequency with the largest value (rearrangement inequality) — the difference array here is feeding a completely different technique (greedy pairing), not the final answer itself.

**LC 1943 — difference array over a huge coordinate range:** When range boundaries can be up to `10^9`, you cannot allocate a real array. **Coordinate compression** first — collect every distinct boundary value that appears, sort and de-duplicate them, then run the difference-array logic over the *compressed* index space instead of raw coordinates. This is the one genuinely new idea in this pattern: difference array + compression is what makes it scale to problems where the domain is enormous but the number of *events* is small.

**LC 2251 (Number of Flowers in Full Bloom) — event counting without a materializable array at all:** Same "range coverage count at a query point" question as LC 1893, but times can be up to `10^9` with millions of queries — even coordinate compression plus a difference array is unnecessarily heavy. Instead: sort start times and end times **separately**, and for each query time `t`, binary search for `(number of flowers already started) - (number of flowers already ended)`. This is the natural escalation once you see that difference array + compression (LC 1943) is itself sometimes too much machinery — sorted arrays plus binary search answer the same question with less bookkeeping when you only need point queries, not the full materialized array.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1109. Corporate Flight Bookings | Anchor — pure difference array, `diff[l]+=v, diff[r+1]-=v`, prefix-sum once |
| 2 | LC 2848. Points That Intersect With Cars | Simplest version — small range, direct `+1/-1` marking |
| 3 | LC 370. Range Addition | Same template as LC 1109, framed as the textbook "apply k range updates" problem |
| 4 | LC 1893. Check if All the Integers in a Range Are Covered | Difference array → prefix sum → boolean coverage check, not a numeric answer |
| 5 | LC 2381. Shifting Letters II | Signed difference array — net direction/magnitude per index after prefix-summing |
| 6 | LC 1589. Maximum Sum Obtained of Any Permutation | Difference array builds a frequency array, then greedy pairing (rearrangement inequality) with sorted input |
| 7 | LC 1943. Describe the Painting | Difference array + coordinate compression — handles ranges over a huge domain |
| 8 | LC 2251. Number of Flowers in Full Bloom | Sorted starts/ends + binary search per query — escalation beyond difference array when domain is too large even for compression |
| — | LC 1094. Car Pooling *(cross-ref: Intervals sheet, Pattern 5)* | LC 1109's exact mechanic + a capacity check layered on top |
| — | LC 1854. Maximum Population Year *(cross-ref: Intervals sheet, Pattern 5)* | Same difference-array-over-time mechanic, framed as birth/death events |

---

## Pattern 4: 2D Prefix Sum

**Identify:** You need range-sum queries over rectangular regions of a 2D grid, or range-increment updates over submatrices. The 2D prefix sum generalizes Pattern 1's formula using inclusion-exclusion.

**The core formula — derive it once, never re-derive it under pressure:**
```
prefix[i][j] = grid[i-1][j-1] + prefix[i-1][j] + prefix[i][j-1] - prefix[i-1][j-1]

sum(r1,c1,r2,c2) = prefix[r2+1][c2+1] - prefix[r1][c2+1] - prefix[r2+1][c1] + prefix[r1][c1]
```
The `-prefix[i-1][j-1]` term exists because the region above and the region to the left both double-count the top-left overlapping rectangle — inclusion-exclusion, not a typo to memorize blindly.

**LC 304 as the anchor:** Build the 2D prefix sum once, answer every query in O(1) via the four-term formula above.

**LC 1314 as the direct application:** Given a query radius `k` per cell, the answer is a bounded-rectangle sum — literally LC 304's query formula called once per cell, with boundary clamping.

**LC 2536 — the 2D difference array, mirroring Pattern 3 in two dimensions:** Range-increment a submatrix by marking four corners: `diff[r1][c1] += v`, `diff[r1][c2+1] -= v`, `diff[r2+1][c1] -= v`, `diff[r2+1][c2+1] += v`. A single 2D prefix-sum pass at the end materializes every increment — exact 2D analogue of Pattern 3's 1D template.

**LC 1738 — 2D prefix XOR, not sum:** Because XOR is invertible exactly like sum, the identical four-term inclusion-exclusion formula works with `^` replacing `+` and `-`. Recognizing that the 2D prefix sum formula is really "any invertible binary operation over 2D inclusion-exclusion" — not sum-specific — is the transferable insight.

**LC 1074 — the hard-tier combination, 2D reduced to 1D:** Fix a pair of row boundaries `(top, bottom)`, collapse every column between them into a single 1D array of column sums, then run **Pattern 2's exact hashmap technique** on that collapsed 1D array to count subarrays (sub-rectangles) summing to target. This is the single clearest illustration in the whole sheet of "combine two already-known patterns" — 2D row-pair enumeration (same instinct as DP sheet's 2D Kadane's variants) + Pattern 2's subarray-sum hashmap, nothing new to learn mechanically.

**LC 363 (optional, advanced) — 2D + ordered set:** Same row-pair collapsing as LC 1074, but instead of finding an *exact* target sum, you need the largest sum **no larger than k**. A hashmap can't answer "largest value ≤ k" — that needs a TreeSet's `ceiling()` query on running prefix sums within the collapsed 1D array. Do this only after LC 1074 and the BST sheet's Ordered Set pattern are both solid.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 304. Range Sum Query 2D — Immutable | Anchor — inclusion-exclusion formula, O(1) query after O(mn) preprocessing |
| 2 | LC 1314. Matrix Block Sum | Direct application — bounded-radius rectangle sum per cell |
| 3 | LC 2536. Increment Submatrices by One | 2D difference array — four-corner marking, one final 2D prefix-sum pass |
| 4 | LC 1738. Find Kth Largest XOR Coordinate Value | Same formula, XOR replacing sum — inclusion-exclusion works for any invertible op |
| 5 | LC 1074. Number of Submatrices That Sum to Target | Row-pair collapse to 1D + Pattern 2's hashmap technique reused directly |
| — | LC 363. Max Sum of Rectangle No Larger Than K *(optional, advanced)* | Row-pair collapse + TreeSet `ceiling()` instead of hashmap exact match |

---

## Pattern 5: Product Prefix (Prefix Product)

**Identify:** You need a running product instead of a running sum — "product of array except self," "product of the last k numbers." Multiplication is invertible (division undoes it) in principle, but **zero breaks division** — you cannot divide by zero to "undo" it, which is why this pattern needs a real workaround rather than a direct copy of Pattern 1's formula.

**LC 238 — the anchor, and the workaround itself:** Build a `prefixProduct` array (product of everything to the left of `i`) and a `suffixProduct` array (product of everything to the right of `i`) *without ever dividing*. `answer[i] = prefixProduct[i] * suffixProduct[i]`. This sidesteps the zero problem entirely — no division ever happens, so a zero anywhere in the array is handled automatically and correctly, including the case of two or more zeros (which forces every answer to 0).

**LC 1352 — the streaming version of the same zero problem:** A running product breaks the instant a `0` is appended, because you can never divide it back out later when it slides out of the window. The fix: maintain a list of *prefix products since the last zero seen*. Appending a zero clears the list and restarts. A query for "product of the last k" is then a suffix-division within this zero-free list — or `0` outright, if fewer numbers have been appended since the last reset than `k`. *(Cross-reference: this exact problem and framing already exists in the DS Design sheet, Pattern 4 — same technique, listed there under Streaming Counters. Primary home is there; included here because the reasoning about *why* it belongs to the prefix-product family is the point of this pattern.)*

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 238. Product of Array Except Self | Prefix product × suffix product, no division — handles zeros automatically |
| 2 | LC 1352. Product of the Last K Numbers *(cross-ref: DS Design sheet, Pattern 4)* | Zero-reset list of prefix products — streaming version of the same zero problem |

---

## Pattern 6: Prefix Sum + Binary Search

**Identify:** The prefix sum array is, by construction, **monotonically non-decreasing** whenever all elements are non-negative — which means you can binary search over it. This pattern combines Pattern 1's array with the Binary Search sheet's lower/upper-bound template.

**Why monotonicity of the prefix array is the enabling fact here:** A binary search only works over a sequence that's sorted (or monotonic in the property you're searching). A prefix sum array of non-negative values is monotonic by definition — each step can only stay the same or increase — which is exactly the property Pattern 4 (Binary Search sheet) formalizes as a general binary-search precondition.

**LC 1712 — the anchor:** Split the array into three contiguous parts with `sum(part1) ≤ sum(part2) ≤ sum(part3)`. For each possible left boundary (or a fixed middle boundary), binary search the prefix sum array for the valid range of right boundaries satisfying both inequalities simultaneously — turns an O(n²) or O(n³) brute force into O(n log n).

**LC 1685 — prefix sum feeding a closed-form per-index formula, not a search:** For a sorted array, the sum of absolute differences at index `i` splits cleanly into "sum of everything smaller" and "sum of everything larger," both derivable from one prefix sum array in O(1) per index — included here because it's the same "prefix sum as the enabling precomputation for a per-element O(1) formula" instinct as the binary-search problems, just without an explicit binary search call.

**Cross-references worth knowing but not re-taught here:**
- **LC 528 (Random Pick with Weight)** — build a prefix sum array of weights, then lower-bound binary search for the first prefix ≥ a random value. *(Full treatment: Binary Search sheet, Pattern 8.)*
- **LC 2528 (Maximize the Minimum Powered City)** — binary search on the answer threshold, with a prefix-sum-powered feasibility check inside each iteration. *(Full treatment: Binary Search sheet, Pattern 5 — Minimax.)*

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1712. Ways to Split Array Into Three Subarrays | Prefix sum monotonicity enables binary search for valid split ranges |
| 2 | LC 1685. Sum of Absolute Differences in a Sorted Array | Prefix sum → closed-form per-index split into "smaller-sum" and "larger-sum" halves |
| — | LC 528. Random Pick with Weight *(cross-ref: Binary Search sheet, Pattern 8)* | Prefix sum of weights + lower bound on a random value |
| — | LC 2528. Maximize the Minimum Powered City *(cross-ref: Binary Search sheet, Pattern 5)* | Binary search on answer + prefix-sum feasibility check inside |

---

## Cross-References (Fully Covered Elsewhere — Don't Duplicate)

| Problem | Primary Home | Why It's Not Here |
|---|---|---|
| Prefix XOR family (LC 1310, 1720, 1734, 1442, 1863) | Bit Manipulation sheet, Pattern 3 | XOR-specific invertibility already fully treated there |
| LC 53. Maximum Subarray | DP sheet, Pattern 2 (Kadane's) | Solvable via "running sum minus minimum prefix seen" — Kadane's is the primary/cleaner framing |
| LC 209. Minimum Size Subarray Sum | Sliding Window sheet, Pattern 5 | All-positive values make sliding window strictly simpler than prefix sum + binary search here |
| LC 1094. Car Pooling, LC 1854. Maximum Population Year, LC 732. My Calendar III, LC 759. Employee Free Time | Intervals sheet, Pattern 5 | Same difference-array/sweep mechanism, already fully written up there |
| LC 729/731/732. My Calendar I/II/III | BST sheet, Pattern 8 | TreeMap-based sweep — different primary tool even though the underlying question is event counting |
| LC 528. Random Pick with Weight, LC 2528. Maximize the Minimum Powered City | Binary Search sheet, Patterns 5 & 8 | Binary search is the primary technique; prefix sum is a supporting precomputation |
| LC 1352. Product of the Last K Numbers | DS Design sheet, Pattern 4 | Listed there as a streaming-counter design problem; included here only for pattern-family reasoning |

**Out of scope for this sheet (flagged, not forgotten):** **LC 307 (Range Sum Query — Mutable)** requires point updates *interleaved* with range queries — plain prefix sum breaks because every update would force an O(n) rebuild. This needs a Fenwick Tree (Binary Indexed Tree) or Segment Tree instead, which — consistent with the BST sheet's existing flag on the same topic — belongs in a separate advanced/competitive-programming sheet, not here.

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| 1D Prefix Sum Fundamentals | 6 (+1 optional) | Precompute once, query via subtraction; balance-point equation family |
| Prefix Sum + HashMap | 7 | `prefix[r] - target` lookup — count, divisibility, parity, longest-length variants |
| Difference Array — Range Update & Event Counting | 8 (+2 cross-ref) | Mark boundaries, single final prefix-sum pass; compression for huge domains |
| 2D Prefix Sum | 5 (+1 optional) | Inclusion-exclusion formula; row-pair collapse combines with Pattern 2's hashmap |
| Product Prefix | 1 (+1 cross-ref) | Prefix × suffix product avoids division-by-zero entirely |
| Prefix Sum + Binary Search | 2 (+2 cross-ref) | Non-negative prefix sum is monotonic — binary-searchable |
| **Total** | **~29 new + 5 cross-referenced/optional** | |

---

## How to Use This Sheet

**Pattern 1 first, always.** The balance-point equation (`leftSum == totalSum - leftSum - arr[i]`) recurs in Patterns 1 and indirectly motivates Pattern 6 — get it completely automatic before anything else.

**Pattern 2 depends only on Pattern 1's core formula, not on hashmap fluency from elsewhere.** Do LC 560 until the `prefix[r] - target` lookup is reflexive — every other problem in this pattern is that one lookup with a different key transformation.

**Pattern 3 is standalone and can run in parallel with Pattern 2.** Say the inverse relationship out loud: "Pattern 1 derives sums from a fixed array; Pattern 3 derives a final array from many increments." LC 1109 before LC 1094 — get the pure mechanic clean before adding the capacity-check layer.

**Pattern 4 requires Pattern 1 and Pattern 2 both solid before attempting LC 1074.** The row-pair-collapse trick is 2D reasoning (dependent on nothing new) fused with Pattern 2's hashmap (which must already be automatic) — if either half is shaky, LC 1074 will feel like a brand-new hard problem instead of a combination of two known ones.

**Pattern 5 is small but conceptually important — don't skip the "why division breaks" reasoning.** LC 238's prefix×suffix trick generalizes to any problem where you'd instinctively reach for division to "undo" a running product.

**Pattern 6 last, and only after the Binary Search sheet's Pattern 1 (lower/upper bound) is solid.** The prefix array's monotonicity is what licenses the binary search — if that connection isn't obvious, revisit the Binary Search sheet before attempting LC 1712.

---
