# Two Pointers DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Two pointers replaces an O(n²) nested-loop comparison with O(n) or O(n log n) by moving indices with intent — never blindly re-scanning. Every pattern below is one of four mechanics:

- **Opposite-direction (converge):** one pointer starts at each end, they move toward each other based on a comparison at each step.
- **Same-direction (read/write):** a slow pointer marks "the boundary of what's valid so far," a fast pointer scans ahead and decides what gets kept.
- **Same-direction (independent sequences):** two pointers, each walking its own separate sequence, advance based on a comparison between their current elements.
- **Different-speed (fast/slow):** one pointer moves twice as fast as the other — detects a cycle in an *implicit* sequence (an array read as a functional graph, or a number under repeated transformation).

**The one thing every pattern shares:** pointer movement must be justified by a **monotonic property** — moving a pointer must always move you *toward* the answer, never away from it. Same monotonicity requirement as the Sliding Window sheet, applied to point-relationships instead of window-aggregates.

---

## Two Pointers vs Sliding Window — the one-line test

> **Do your two indices always describe the bounds of one contiguous range, with a running aggregate over that range?** → Sliding Window sheet.
> **Do your pointers converge from opposite ends, read while another writes, walk two independent sequences, or move at different speeds?** → This sheet.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Opposite-Direction — Sorted Target Search | sorted array, "find pair/triplet/quadruplet summing to target", "count pairs/triples satisfying an inequality" |
| Opposite-Direction — Container/Boundary/Shape | "maximum area/water", two boundaries trap or bound something, "is this a valid mountain" |
| Same-Direction — In-place Compaction | "remove in-place", "move zeroes", "partition by pivot", O(1) extra space |
| Same-Direction — Two Independent Sequences | "merge two sorted", "add two number-strings", two arrays advancing independently |
| Opposite-Direction — Palindrome & String Comparison | "valid palindrome", "reverse string/words", "compare after backspaces" |
| Expand Around Center | "longest palindromic substring", "count palindromic substrings" |
| Same-Direction — Subsequence Matching, Parsing & Construction | "is subsequence", "construct result char by char", "compress string", "compare version numbers" |
| Array Reordering via Two Pointers | "rearrange in-place", "next permutation", "rotate array", "sort by parity" |
| Cycle Detection on Implicit Sequences | array read as a graph (`i → nums[i]`), repeated digit transformation, "find the duplicate" |

---

## Pattern 1: Opposite-Direction Pointers — Sorted Array Target Search

**Identify:** The array is sorted (or you sort it first), and you need to find — or count — pairs/triplets/quadruplets satisfying a target relationship. Two pointers converge: if the current combination is too small, advance the left pointer; if too large, retreat the right pointer.

**LC 1 vs LC 167 — the contrast that teaches the whole pattern:** Two Sum on an *unsorted* array needs a hashmap because there's no ordering to exploit. The moment the array is sorted, sortedness itself tells you which pointer to move.

**3Sum's outer-loop-plus-two-pointer template:** Fix one element via outer loop, run opposite-direction search on the rest. Skip duplicates at every level. 4Sum is the same with one more nested outer loop.

**The counting variant — a different mechanic worth calling out explicitly:** In LC 259 and LC 611, when the pointer condition holds, it holds for *every index between the pointers*, not just the current pair — so you add `(right - left)` to a running count instead of recording one answer and moving on. This mirrors the Sliding Window sheet's "count every sub-window ending here" trick (LC 713), just applied to a converging pair instead of an expanding window.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1. Two Sum | Contrast case — unsorted, hashmap required; establishes *why* sortedness matters |
| 2 | LC 167. Two Sum II — Input Array Is Sorted | Core template — converge pointers, move based on sum comparison |
| 3 | LC 15. 3Sum | Fix one element via outer loop + two-pointer search — skip duplicates at every level |
| 4 | LC 16. 3Sum Closest | Same skeleton as LC 15, track closest-to-target instead of exact match |
| 5 | LC 18. 4Sum | One more fixed outer loop on top of LC 15 |
| 6 | LC 259. 3Sum Smaller | Counting variant — when `sum < target`, all `(right - left)` pairs with this left qualify |
| 7 | LC 611. Valid Triangle Number | Sort + counting variant — for fixed largest side, count all valid pairs of smaller sides via converging pointers |

---

## Pattern 2: Opposite-Direction Pointers — Container / Boundary / Shape Validation

**Identify:** Two boundaries trap something between them (water, area) and you maximize what's trapped — or two boundaries define a shape (a single peak) and you validate it. The pointers converge or scan from both ends; at each step, the side that's currently the limiting factor is the one that moves.

**LC 11 — the exchange argument, stated explicitly:** Area = `width × min(height[left], height[right])`. Moving the taller pointer inward shrinks width while the min height can only stay the same or worsen — strictly a bad trade. This proof transfers directly to LC 42.

**LC 42 — O(1)-space alternative to precomputed max arrays:** Track running `leftMax`/`rightMax` while two pointers converge; process whichever side has the smaller running max. *(Cross-ref: Stack sheet Pattern 3 covers the monotonic-stack alternative — know both.)*

**LC 941 — shape validation, not maximization:** Climb from the left while strictly ascending, climb from the right while strictly ascending (walking backward). The array is a valid mountain only if both climbs meet at the same interior index — not at either end. Structurally different goal from LC 11/42, but the same "two boundaries advancing based on local comparison" instinct, which is why it belongs here rather than as an isolated one-off.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 11. Container With Most Water | Move the shorter wall inward — exchange argument proves it's always safe |
| 2 | LC 42. Trapping Rain Water *(cross-ref: Stack sheet Pattern 3)* | Running leftMax/rightMax, process the smaller side — O(1) space |
| 3 | LC 881. Boats to Save People | Sort + greedy pair — heaviest with lightest if they fit, else heaviest alone |
| 4 | LC 941. Valid Mountain Array | Climb from both ends — valid only if both climbs meet at the same interior peak |

---

## Pattern 3: Same-Direction Pointers — In-place Array Compaction

**Identify:** Modify an array in-place under O(1) extra space — remove elements, move elements to one side, partition by a pivot. A slow ("write") pointer marks the boundary of the valid region; a fast ("read") pointer scans ahead deciding what to bring in.

**The universal template:**
```
write = 0
for read in range(n):
    if arr[read] satisfies the keep-condition:
        arr[write] = arr[read]
        write += 1
return write   # new logical length
```

**LC 80's twist on LC 26:** Compare `arr[read]` to `arr[write-2]` instead of `arr[write-1]` — generalizes to "allow up to k duplicates."

**LC 75 (Sort Colors) — three pointers:** Dutch National Flag. `low` marks the 0-boundary, `high` marks the 2-boundary, `mid` scans. Swap and advance based on `arr[mid]`.

**LC 2161 — stable three-way partition:** Same Dutch-flag spirit, but order must be preserved within each group, which rules out the swap trick and requires building the result with two write pointers from opposite ends instead.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 27. Remove Element | Simplest read/write template — keep-condition is `arr[read] != val` |
| 2 | LC 26. Remove Duplicates from Sorted Array | Compare against `arr[write-1]` — allow zero duplicates |
| 3 | LC 80. Remove Duplicates from Sorted Array II | Compare against `arr[write-2]` — allow up to two duplicates |
| 4 | LC 283. Move Zeroes | Non-zero elements compact left, then fill remainder with zeroes |
| 5 | LC 75. Sort Colors | Dutch National Flag — three pointers, one-pass in-place partition |
| 6 | LC 2161. Partition Array According to Given Pivot | Three-way stable partition — order-preserving via two write pointers from both ends |

---

## Pattern 4: Same-Direction Pointers — Two Independent Sequences

**Identify:** You have two separate sequences, and each pointer walks its own independently. Three sub-flavors share this mechanic: **merging** two sorted sequences into one, **combining** two sequences with carry propagation (arithmetic), and **greedy searching** across two sequences for a best pairing.

**LC 88's backward-merge trick:** Merging into `nums1` from the front would overwrite unread elements. Merge **from the back** instead — `nums1` has exactly enough trailing space to absorb both arrays.

**LC 415 / LC 67 — backward arithmetic combination:** Same backward-walking instinct as LC 88, but instead of picking the smaller element, you sum digits and propagate a carry. Both pointers start at the end of their respective strings and walk left, building the result in reverse. *(LC 989, Add to Array-Form of Integer, is the same technique applied to one array + one integer — optional, once 415/67 are solid.)*

**LC 1855 — greedy independent search:** Two pointers `i, j` both start at 0 and only move forward. Advance `j` looking for validity; advance `i` whenever the current pairing can never work for any larger `j`. This "advance whichever pointer can't possibly improve" instinct is the same exchange-argument reasoning as LC 11, just applied across two arrays instead of one.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 88. Merge Sorted Array | Merge backward from the end — avoids overwriting unread elements in `nums1` |
| 2 | LC 415. Add Strings | Backward pointers on two digit-strings + carry — same instinct as LC 88, arithmetic instead of comparison |
| 3 | LC 67. Add Binary | Identical technique to LC 415, base 2 instead of base 10 |
| 4 | LC 1855. Maximum Distance Between a Pair of Values | Two forward-only pointers — advance the one that can never improve given the other |
| — | LC 989. Add to Array-Form of Integer *(optional)* | Same technique as LC 415, array + integer instead of two strings |
| — | LC 21. Merge Two Sorted Lists *(cross-ref: LL sheet, Pattern 4)* | Same merge mechanic, linked list nodes instead of array indices |
| — | LC 986. Interval List Intersections *(cross-ref: Intervals sheet, Pattern 4)* | Same two-independent-pointers idea, applied to sorted interval lists |

---

## Pattern 5: Opposite-Direction Pointers on Strings — Palindrome & Comparison

**Identify:** Checking whether a string reads the same forward/backward, or comparing two strings under a transformation. Pointers converge from both ends (or both strings' ends).

**LC 680's one-skip extension:** On first mismatch, try skipping *either* the left or right character once, then plain-check the remainder with LC 125's template.

**LC 917 — the mutation-instead-of-check variant:** Reverse only the alphabetic characters in place, skipping non-letters entirely — same "skip condition while converging" instinct as LC 125, but the pointers now swap instead of just compare, bridging naturally from LC 344.

**LC 844 — comparison scanned from the back:** Because `#` deletes the *previous* character, scan from the **back** of each string, skipping characters cancelled by pending backspaces — no stack needed.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 125. Valid Palindrome | Core template — skip non-alphanumeric, compare case-insensitively, converge |
| 2 | LC 680. Valid Palindrome II | On first mismatch, try skipping left OR right once, then plain-check the rest |
| 3 | LC 344. Reverse String | Simplest converging swap — foundational before any harder string two-pointer problem |
| 4 | LC 917. Reverse Only Letters | Converging swap + skip condition — bridges LC 344's swap and LC 125's skip logic |
| 5 | LC 151. Reverse Words in a String | Reverse whole string, then reverse each word back — two-pointer trim + reverse |
| 6 | LC 844. Backspace String Compare | Scan both strings from the back, skip characters cancelled by pending `#` |
| 7 | LC 408. Valid Word Abbreviation | Two pointers, one per string — abbreviation pointer consumes digit runs as jump distances |

---

## Pattern 6: Expand Around Center

**Identify:** Finding or counting palindromic substrings. Pointers start together (or one apart, for even-length centers) at a candidate center and expand **outward**, stopping the moment characters stop matching. Structurally an opposite-direction technique running in reverse.

**The core mechanic:** `2n - 1` possible centers — n single-character (odd-length) and `n-1` between-character (even-length). Run the same expand-outward helper from every candidate center.

**LC 5 vs LC 647:** Same expansion helper — LC 5 tracks the single best (longest) expansion, LC 647 sums every successful expansion.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 5. Longest Palindromic Substring | Expand outward from every center (odd + even) — track the longest match |
| 2 | LC 647. Palindromic Substrings | Same expansion helper — count every successful expansion |

---

## Pattern 7: Same-Direction Pointers — Subsequence Matching, Parsing & Construction

**Identify:** Checking if one sequence appears as a subsequence of another (order matters, contiguity doesn't), parsing two sequences segment-by-segment for comparison, or building a result character by character using two independent scanning positions.

**LC 392 — the anchor:** Advance the `s` pointer only on a match; always advance the `t` pointer. `s` is a subsequence if the `s` pointer reaches the end.

**LC 925 — matching + run-length, combined:** Extends LC 392's matching pointer with LC 696's run-length comparison — each character run in the typed name must be at least as long as the corresponding run in the actual name.

**LC 165 — segment-by-segment parsing:** Two pointers each walk their own version string, extracting and comparing one numeric segment (between dots) at a time, treating a missing trailing segment as `0`.

**LC 696 — run-length comparison:** For every pair of adjacent runs, the number of valid balanced substrings is `min(runLength1, runLength2)`.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 392. Is Subsequence | Advance `t` always, advance `s` only on match |
| 2 | LC 925. Long Pressed Name | Matching pointer + run-length comparison combined |
| 3 | LC 696. Count Binary Substrings | Run-length pointers — contribution per adjacent run pair is `min(run1, run2)` |
| 4 | LC 443. String Compression | Read/write pointers — read a full run, write character + count digits in place |
| 5 | LC 1768. Merge Strings Alternately | Two pointers, one per string, alternate appending — tail of longer string appended at end |
| 6 | LC 942. DI String Match | Two pointers (`low`, `high`) — `I` consumes `low++`, `D` consumes `high--` |
| 7 | LC 165. Compare Version Numbers | Two pointers parsing dot-separated segments — missing trailing segments treated as 0 |

---

## Pattern 8: Array Reordering via Two Pointers

**Identify:** Rearranging an array in place into a specific target order — sorted by absolute value, by parity, the next lexicographic permutation, or rotated by k.

**LC 977 — opposite-direction construction:** With negatives present, the largest squares live at *both* ends. Converge from the outside in, build the result **from the back** — mirrors LC 88's backward-merge trick.

**LC 905 before LC 922:** LC 905 is a plain single-condition partition (evens left, odds right, order doesn't matter) — the direct warmup for LC 922, which adds the constraint that evens and odds must land at even/odd *indices specifically*, requiring two pointers that each step by 2.

**LC 189 — the three-reversal trick:** Reverse whole array, then first k, then remaining n-k — each reversal is a converging two-pointer swap (same mechanic as LC 344), chained three times.

**LC 31 — hardest problem in this pattern:** Find the pivot from the right, find the swap target from the right, swap, reverse the suffix using LC 189's reversal. Three two-pointer sub-steps combined.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 977. Squares of a Sorted Array | Converge from outside in on absolute value, build result backward |
| 2 | LC 905. Sort Array By Parity | Single-condition partition — warmup for LC 922's stricter index constraint |
| 3 | LC 922. Sort Array By Parity II | Two pointers stepping by 2 — swap misplaced parity into the correct index |
| 4 | LC 189. Rotate Array | Three reversals — whole array, first k, remaining n-k |
| 5 | LC 31. Next Permutation | Find pivot, find swap target, swap, reverse suffix — three sub-steps combined |

---

## Pattern 9: Cycle Detection on Implicit Sequences

**Identify:** No explicit linked list, but an implicit "next" function — `index → nums[index]`, or a number transformed by a fixed rule — and you need to detect whether following it forever loops. The exact fast/slow mechanic from the Linked List sheet's Pattern 2, applied to a sequence that isn't a linked list.

**LC 287 — the anchor:** Treat `nums[i]` as a pointer from index `i` to index `nums[i]`. A duplicate value guarantees two indices point to the same place, guaranteeing a cycle. Run Floyd's exactly as in LC 142, then reset one pointer to start and advance both by one to find the entry point — the duplicate itself.

**LC 202:** The "next" function is "sum of squares of digits." Not-happy means this process cycles instead of reaching 1.

**LC 457 — the edge-case-heavy variant:** Same fast/slow mechanic, but the implicit "next" function (`i → i + nums[i]`, wrapping circularly) is only valid if every step in the cycle moves in the *same direction* (all positive or all negative) and the cycle has length greater than 1 — a single element pointing to itself is not a valid cycle. This forces you to actually reason about *why* Floyd's works, not just apply it mechanically.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 287. Find the Duplicate Number | Array as implicit graph (`i → nums[i]`) — Floyd's cycle detection, same as LL Pattern 2 |
| 2 | LC 202. Happy Number | Implicit sequence via digit-square-sum — cycle means "not happy," O(1) space over a hashset |
| 3 | LC 457. Circular Array Loop | Fast/slow with direction constraint — same-sign moves only, single-node self-loops excluded |
| — | LC 142. Linked List Cycle II *(cross-ref: LL sheet, Pattern 2)* | The original mechanism both problems above are adapting |

---

## Cross-References / Misclassifications Corrected

| Problem | Issue | Resolution |
|---|---|---|
| LC 2367. Count Subarrays with Fixed Bounds | Algomaster filed under "Opposite Direction" | Actually Sliding Window (three running positions over a contiguous range) |
| LC 5. Longest Palindromic Substring | Algomaster filed under "Same Direction" | Reclassified — expansion moves pointers *outward*, structurally opposite-direction |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Opposite-Direction — Sorted Target Search | 7 | Converge on comparison; counting variant adds range-of-indices contribution |
| Opposite-Direction — Container/Boundary/Shape | 4 | Move the limiting boundary; shape validation via dual climbs |
| Same-Direction — In-place Compaction | 6 | Read/write pointers; Dutch flag generalizes to three pointers |
| Same-Direction — Two Independent Sequences | 4 (+1 optional, +2 cross-ref) | Merge, backward arithmetic combine, or greedy independent search |
| Opposite-Direction — Palindrome & String Comparison | 7 | Converge on strings; backward scan through pending deletions |
| Expand Around Center | 2 | Expand outward from every center, odd and even |
| Subsequence Matching, Parsing & Construction | 7 | Independent-speed forward pointers; run-length, matching, or segment parsing |
| Array Reordering via Two Pointers | 5 | Construction, reversal-based rotation, permutation stepping |
| Cycle Detection on Implicit Sequences | 3 (+1 cross-ref) | Fast/slow on a functional graph, with a direction-constraint variant |
| **Total** | **~45 new + 4 cross-referenced/optional** | |

---

## How to Use This Sheet

**Pattern 1 first, always.** The LC 1 → LC 167 contrast is the reason two pointers works at all; the counting variant (LC 259/611) should come only after LC 15/16/18 are automatic.

**Patterns 2 and 3 run in parallel** once Pattern 1 is solid.

**Pattern 4's three sub-flavors should be done in the order listed** — merge (LC 88) establishes the backward-walk instinct that LC 415/67 reuse for arithmetic, and LC 1855 is the odd one out (forward, not backward) so do it last.

**Patterns 5 and 6 belong together, in that order.** Checking if a *given* string is a palindrome must be automatic before *finding* palindromic substrings by expansion.

**Pattern 8's LC 31 is the hardest problem in this sheet** — it combines LC 189's reversal with two independent right-to-left scans. Do it last, after LC 189 is completely automatic.

**Pattern 9 is a recognition drill, not a mechanics drill.** If Floyd's is solid from Linked Lists, the new skill is spotting the implicit "next" function — and LC 457 is where you learn the mechanic isn't a blind template, since it silently breaks without the direction constraint.

---
