# Arrays DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Fraz has no dedicated Arrays list; Algomaster's list is short precisely because most "array problems" are already the primary content of other sheets you've built. Before inventing patterns, the honest first move is to sort every problem into "already owned elsewhere" vs. "genuinely belongs here."

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 283. Move Zeroes | Two Pointers sheet, Pattern 3 |
| LC 27. Remove Element | Two Pointers sheet, Pattern 3 |
| LC 26 / LC 80. Remove Duplicates from Sorted Array (I/II) | Two Pointers sheet, Pattern 3 |
| LC 189. Rotate Array | Two Pointers sheet, Pattern 8 |
| LC 31. Next Permutation | Two Pointers sheet, Pattern 8 |
| LC 238. Product of Array Except Self | Prefix Sum sheet, Pattern 5 |
| LC 121 / LC 122. Best Time to Buy and Sell Stock (I/II) | DP sheet, Pattern 13 (State Machine DP) |
| LC 268. Missing Number | Bit Manipulation sheet, Pattern 2 (XOR cancellation) |
| LC 287. Find the Duplicate Number | Two Pointers sheet, Pattern 9 (Floyd's cycle detection) |
| LC 1470. Shuffle the Array | Pure simulation, no technique to extract — drop |

**Deliberately deferred, not dropped:**

| Problem | Correct Home |
|---|---|
| LC 274 / LC 275. H-Index (I/II) | Sorting sheet — the mechanism is counting-sort-as-answer-bound, not an array-scanning technique |

That leaves a small, genuinely unclaimed set of raw techniques — exactly the "100s of problems, 10-12 real patterns" filtering you asked for, except here the filtering happens *across sheets*, not just within this list.

---

## The Core Mental Model — Before Any Pattern

Once the two-pointer/prefix-sum/DP-flavored problems are stripped out, what's left in "Arrays" as its own topic is: **single-pass state tracking without extra data structures**, plus a handful of named algorithms (Boyer-Moore, Cyclic Sort, Fisher-Yates) that are foundational enough to know cold rather than re-derive. Every pattern below answers one of two questions: *what is the minimum state I need to carry forward while scanning once?* or *is there a provably-correct named trick that replaces an O(n) space or O(n log n) time solution with something leaner?*

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Single-Pass State Tracking | "max consecutive", "third maximum", "missing ranges" — track a few running variables |
| Boyer-Moore Voting | "majority element", "appears more than n/k times" |
| Cyclic Sort / Index-as-Hash | values in range `[1,n]`, O(1) space, "missing", "duplicate", "first missing positive" |
| Run-Length Counting | "count subarrays of consecutive same value", "count zero-filled subarrays" |
| Track K-Smallest-Seen | "increasing triplet/k-length subsequence", O(1) space, no need to reconstruct |
| Rearrangement via Virtual Indexing | "wiggle sort", interleave two groups into alternating positions in-place |
| Fisher-Yates Random Shuffle | "shuffle array", "uniformly random permutation" |

---

## Pattern 1: Single-Pass State Tracking

**Identify:** The answer is derivable by scanning once and updating a small, fixed number of running variables — a current streak, top-k seen values, or the last "expected" position. No sorting, no extra structures beyond a few scalars.

**Why this deserves to be a named pattern, not "just coding":** The failure mode isn't algorithmic, it's discipline — people reach for a sort (`O(n log n)`) or a hashmap when 2–3 tracked variables solve it in `O(n)`/`O(1)`. Recognizing "I only need to remember the last k things I've seen" is the actual skill.

**LC 414 (Third Maximum Number) — the anchor for "top-k tracking":** Maintain exactly three variables (`first, second, third`), updating them in careful order on each new element (check greater-than conditions from largest to smallest, and skip duplicates of already-tracked values). This generalizes directly to Pattern 5's "track k smallest seen" — same instinct, different comparison direction and different k.

**LC 163 (Missing Ranges) — tracking the "expected next value":** Walk the array (conceptually including virtual bounds `lower-1` and `upper+1`), and every time `nums[i] - prev > 1`, the gap between them is a missing range. The entire problem is "track what I expected to see next, and report the gap when reality doesn't match."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 485. Max Consecutive Ones | Running streak counter, reset on 0 — simplest possible single-pass tracker |
| 2 | LC 414. Third Maximum Number | Track top-3 distinct values with careful update order and duplicate-skip |
| 3 | LC 163. Missing Ranges | Track expected-next value; gap whenever actual jumps ahead of expected |

---

## Pattern 2: Boyer-Moore Voting Algorithm

**Identify:** Find the element that appears **more than `n/2`** (or more than `n/k`) times, in O(1) space, O(n) time — a hashmap-frequency-count would work but violates the space constraint, and that's exactly the tell.

**The core idea — why cancellation works:** Maintain a `candidate` and a `count`. If `count == 0`, the current element becomes the new candidate. If the current element matches the candidate, increment `count`; otherwise decrement it. Because the majority element occurs more than every other element combined, it can never be fully "cancelled out" by the time the array ends — whatever survives as the final candidate must be it. This is a genuine exchange-argument proof, not a heuristic: pair up every majority occurrence with a non-majority occurrence and they cancel; since majority occurrences outnumber the rest, at least one is left unpaired.

**LC 229 (Majority Element II) — generalizing from one candidate to two:** At most `⌊n/3⌋`-threshold means at most **two** elements can qualify (three elements each appearing >n/3 times would exceed n). Track two candidates and two counts simultaneously, with the same cancellation logic extended to two slots. A final verification pass is required here (unlike LC 169) because with two candidates, there's no guarantee either genuinely crosses the n/3 threshold — only that if a majority element exists, it must be one of the two survivors.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 169. Majority Element | Boyer-Moore Voting — single candidate + count, cancellation proof guarantees correctness, no verification pass needed |
| 2 | LC 229. Majority Element II | Two candidates + two counts — at most 2 elements can exceed n/3; requires a verification pass afterward |

---

## Pattern 3: Cyclic Sort / Index-as-Hash

**Identify:** Values are constrained to a range tightly related to the array's length (typically `[1, n]` or `[0, n-1]`), you need O(1) extra space, and you're asked for a missing, duplicate, or out-of-place value. Two distinct mechanisms live under this same setup — don't conflate them.

**Sub-technique A — Swap to home index (LC 41):** Place each value `v` (where `1 <= v <= n`) at index `v-1` by repeatedly swapping, ignoring values outside that range or already correctly placed. After this pass, scan left to right — the first index `i` where `nums[i] != i+1` gives the answer `i+1`. Use this when you need to know the *specific missing/first-missing* value and the array's final arrangement doesn't matter.

**Sub-technique B — Negation marking (LC 448, LC 442):** Instead of moving values around, visit index `v-1` for each value `v` present in the array and **flip the sign** of whatever lives there (`nums[abs(v)-1] = -abs(nums[abs(v)-1])`). This marks "the value `abs(v)` has been seen" without moving anything. A second pass then reads meaning off the sign: for missing numbers, any index still positive means its `index+1` was never seen; for duplicates, any index you're about to negate that's *already* negative means you've seen that value twice.

**Why these are different enough to name separately:** Sub-technique A physically rearranges the array into a target permutation — useful when you need the *value* at a specific position afterward. Sub-technique B never moves elements, only their sign — useful when you only need a *seen/unseen* bit per value and want to preserve relative array structure otherwise. Reaching for the wrong one under interview pressure costs real time.

**Why LC 442 isn't just "LC 448 with an extra check":** Both scan and flip signs identically. LC 448's read-out pass asks "which indices stayed positive" (missing); LC 442's asks "which index did I try to flip that was already negative" (duplicate) — checked *during* the marking pass itself, not after. Doing both back-to-back is the fastest way to see they're the same marking mechanism with two different questions asked of the result.

**Why LC 41 comes first in the learning order:** Swap-to-home is the more intuitive mechanism — you can visualize the array "sorting itself." Negation marking will feel like a natural lighter-weight alternative once the value/index relationship from LC 41 is already internalized, rather than a brand-new idea.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 41. First Missing Positive | Swap-to-home-index — place each value at its target position, then scan for first mismatch |
| 2 | LC 448. Find All Numbers Disappeared in an Array | Negation marking — positive-valued indices at the end reveal missing numbers |
| 3 | LC 442. Find All Duplicates in an Array | Same negation marking — a value found already-negative during marking is the duplicate |
| — | LC 268. Missing Number *(cross-ref: Bit Manipulation sheet, Pattern 2)* | XOR cancellation — different mechanism, same value-range setup |
| — | LC 287. Find the Duplicate Number *(cross-ref: Two Pointers sheet, Pattern 9)* | Floyd's cycle detection on the array read as an implicit graph |

---

## Pattern 4: Run-Length / Contiguous Segment Counting

**Identify:** You're counting subarrays defined by a contiguous run of identical or qualifying values — not by sum or product, but by "how long does this run go on." The technique: scan once, measure the length of each maximal run, and convert each run length into a subarray count via the triangular-number formula.

**LC 2348 (Number of Zero-Filled Subarrays) — the anchor:** A contiguous run of `k` zeros contains exactly `k*(k+1)/2` all-zero subarrays (every one of the `k` starting points paired with every valid ending point within the run). Track the current zero-run length as you scan; every time it extends by one, add the new count of subarrays that end at this position (`currentRunLength`, not the whole formula recomputed) — an O(1)-per-step running accumulation rather than recomputing the formula per run.

**Why this is worth isolating as its own pattern:** It looks superficially like Pattern 1 (a running counter), but the *arithmetic* being accumulated — triangular numbers from run lengths — recurs constantly across string/array counting problems (count of substrings with all same character, count of binary substrings, etc. — the Two Pointers sheet's LC 696 uses the same core insight in a paired-runs form). Naming it explicitly here means recognizing "count subarrays from a run length" as a single reusable formula, not re-deriving it each time.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 2348. Number of Zero-Filled Subarrays | Running run-length counter; add `currentRunLength` to total at each step — triangular-number accumulation |

---

## Pattern 5: Track K-Smallest-Seen-So-Far

**Identify:** You need to detect whether an increasing (or otherwise ordered) subsequence of a small, fixed length `k` exists, in O(1) space and O(n) time — too small and structurally simple to justify LIS's full `O(n log n)` machinery, but the same underlying idea in miniature.

**LC 334 (Increasing Triplet Subsequence) — the anchor:** Maintain `first` and `second` — the smallest and second-smallest values seen so far that could each anchor a longer increasing run. Any element strictly greater than both means a valid increasing triplet exists (the current element extends past `second`, which itself extended past `first`). Update `first` whenever a smaller candidate for it appears, and `second` whenever a value sits between `first` and the current `second` — this is functionally a length-3 truncation of the LIS `tails[]` array used in the DP sheet's O(n log n) LIS technique, just unrolled into two named variables instead of a general array.

**Why this connects to, but doesn't replace, the DP sheet's LIS pattern:** If `k` were a parameter rather than a fixed small constant, this becomes exactly the general `tails[]` LIS technique — this pattern is the specific, k-fixed-and-small edge case of that general idea, worth calling out on its own because it appears at exactly this fixed size often enough to be recognized instantly rather than re-derived from the general LIS machinery each time.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 334. Increasing Triplet Subsequence | Track smallest and second-smallest seen so far — length-3 truncation of the general LIS `tails[]` idea |

---

## Pattern 6: Rearrangement via Virtual Indexing

**Identify:** You need to rearrange an array into a specific alternating or interleaved order (e.g., "wiggle" — every even-indexed element compared against its neighbors in one direction, every odd-indexed element the other) with O(1) extra space. The naive approach needs a second array to hold the rearranged result; the virtual indexing trick lets you write in-place by remapping *where* a logical position lands in the physical array.

**LC 280 (Wiggle Sort I) — the gentle entry point:** No extra machinery needed. A single pass comparing each adjacent pair and swapping when the required inequality (`<=` at even `i`, `>=` at odd `i`) is violated is enough — a greedy local-fix, not a full sort.

**LC 324 (Wiggle Sort II) — the anchor, and the actual new idea:** The stricter *strict* inequality version (`arr[even] < arr[odd]`) can't be solved with LC 280's local-swap trick alone, because a greedy adjacent fix can cascade and break earlier positions. Instead: find the median (via quickselect, O(n) average), then conceptually place the larger half of values at odd indices and the smaller half at even indices, both read in *decreasing* order from the median outward (this ordering prevents equal elements from ending up adjacent). The virtual index mapping `newIndex = (1 + 2*i) % (n | 1)` walks through indices in exactly the order `[1, 3, 5, ..., 0, 2, 4, ...]` needed to place values directly into their final position in one O(n) pass — no second array required.

**Why the mapping formula is the transferable idea, not the wiggle-sort story itself:** `(1 + 2*i) % (n | 1)` is a general technique for writing an "odd-indices-first-then-even-indices" traversal order without allocating a temp array. Any problem asking you to interleave two logical groups into alternating physical positions in-place can reuse this exact remapping instinct.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 280. Wiggle Sort | Single-pass adjacent swap — local greedy fix, non-strict inequality |
| 2 | LC 324. Wiggle Sort II | Median (quickselect) + virtual index mapping `(1+2i) % (n\|1)` — strict inequality, no extra array |

---

## Pattern 7: Fisher-Yates Random Shuffle

**Identify:** You need to produce a uniformly random permutation of an array, in-place, with every permutation equally likely. This is a named, provably-correct algorithm — not something to freehand under interview pressure, since naive approaches (e.g., swapping each element with a *fully* random index including ones already placed, or sorting by a random key) either bias the distribution or cost more than necessary.

**LC 384 (Shuffle an Array) — the anchor, and the whole pattern:** Walk the array from the last index down to the first. At each index `i`, swap `arr[i]` with `arr[r]`, where `r` is a uniformly random index in `[0, i]` (inclusive of `i` itself — a no-op swap is a valid outcome). The proof of uniformity is inductive: after placing the last element (n possible choices, each equally likely), the remaining `n-1` elements are still in a uniformly random relative order for the same argument to apply one step smaller — this is the exact reasoning to state out loud in an interview, not just the code.

**The most common bug, worth naming explicitly:** choosing `r` from the *entire* array (`[0, n-1]`) instead of `[0, i]` at every step. This still looks plausible and still runs, but it does **not** produce a uniform distribution — some permutations become more likely than others. Recognizing why the shrinking range `[0, i]` is load-bearing, not incidental, is the actual interview signal.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 384. Shuffle an Array | Fisher-Yates — swap each position with a uniform random index in the *shrinking* remaining range |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Single-Pass State Tracking | 3 | A handful of running variables replace a second pass or sort |
| Boyer-Moore Voting | 2 | Cancellation proof — survivor must be the majority if one exists |
| Cyclic Sort / Index-as-Hash | 3 (+2 cross-ref) | Array-as-its-own-hashmap — swap-to-home vs. negation-marking |
| Run-Length / Contiguous Segment Counting | 1 | Triangular-number accumulation from run length |
| Track K-Smallest-Seen | 1 | O(1)-space truncated LIS for small fixed k |
| Rearrangement via Virtual Indexing | 2 | In-place interleaving via index remapping, no temp array |
| Fisher-Yates Random Shuffle | 1 | Provably uniform in-place permutation |
| **Total** | **13 new + 7 cross-referenced** | |

---

## How to Use This Sheet

**This sheet is intentionally small — that's the correct outcome, not an oversight.** Algomaster's raw list ran to ~19 problems; more than half already belong to Two Pointers, Prefix Sum, or DP sheets you've already built with full rigor there, and H-Index is deliberately deferred to Sorting. What's left is the raw, undiluted "Arrays" core: cancellation proofs (Boyer-Moore), array-as-hashmap (Cyclic Sort, two flavors), lightweight single-pass tracking, and two named algorithms (virtual indexing, Fisher-Yates) that don't fit anywhere else in your reference. Padding this sheet with re-explanations of Two Pointers/Prefix Sum content would violate your own stated goal — distinct raw concepts, not topic-shaped repetition.

**Pattern 2 (Boyer-Moore) is the highest-value pattern here for interviews** — it's a genuinely different proof technique (cancellation/exchange argument) from anything in your other sheets, and it recurs as a sub-step inside harder problems (e.g., some Bit Manipulation "appears k times" variants use analogous counting logic).

**Pattern 3 (Cyclic Sort) is worth drilling most heavily** — do it in the order shown (LC 41 → LC 448 → LC 442). LC 41 is one of the most consistently asked "hard" array problems at FAANG, specifically because most candidates reach for a HashSet and get asked for the O(1)-space follow-up on the spot; LC 448/442 then show the lighter negation-marking alternative to the same value/index relationship.

**Patterns 6 and 7 are small but non-negotiable to know cold.** Fisher-Yates in particular is a classic "explain why your algorithm is correct" interview question, not just a coding one — the proof (not just the code) is what's being tested.
