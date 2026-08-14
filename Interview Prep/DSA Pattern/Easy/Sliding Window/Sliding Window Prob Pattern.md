# Sliding Window DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

A sliding window is a **contiguous range `[left, right]`** that you grow and shrink instead of re-scanning from scratch. Every sliding window pattern is exploiting the same fact: when the window moves by one step, you don't need to recompute anything — you just add the new element and remove the old one.

**Two things are always in play, and keeping them mentally separate prevents most bugs:**
- **Window state** — what you're maintaining as elements enter/leave: a sum, a frequency map, a distinct count, a cost total.
- **Answer state** — what you're actually optimizing or counting: max length, min length, number of valid windows, a boolean. These update at *different* points in the loop depending on the pattern (right after a shrink, inside a shrink, etc.) — that's what each pattern below specifies.

**The one question that decides everything:** *when does the window shrink, and what does that shrinking accomplish?*

- **Fixed-size:** the window never changes size. You add one, remove one, every step. (Pattern 1, 3)
- **Shrink on violation:** you keep expanding right for free. You only shrink left when the window becomes *invalid*. The answer is measured right after each shrink. (Pattern 2)
- **Shrink while still valid:** you expand right until the window *becomes* valid, then you shrink left as far as it stays valid, recording the minimum each step. (Pattern 5)
- **Not two-pointer at all:** some problems that sound like sliding window break the underlying assumption the template depends on. (see "Monotonicity" below, and Pattern 8)

---

## Monotonicity — Why Sliding Window Works (Read This Before Pattern 1)

Every shrink/expand template in this sheet relies on one silent assumption: **moving the window boundary changes validity in a predictable direction.** Specifically — if the window is invalid, shrinking it (removing from the left) should only ever move it *toward* valid, never away. That property is monotonicity, and it is the actual reason the two-pointer technique is allowed to work at all. It is not automatic. Check for it before writing any window code, not after debugging a wrong answer.

**Monotonicity usually holds when:**
- The constraint is a **count or frequency ceiling** — distinct characters ≤ k, zeros ≤ k. Removing an element can only reduce a count, never increase it, so shrinking always moves toward validity.
- The constraint is a **sum or product with non-negative values**. Removing a non-negative element from the sum can only decrease it (or leave it unchanged) — never increase it. This is why LC 209 and LC 1658 work: their constraints explicitly guarantee positive numbers.
- The constraint is a **running cost with non-negative per-element cost**. Same reasoning as sum — this is the entire justification behind Pattern 2's budget/cost problems.

**Monotonicity breaks when:**
- **Negative numbers are allowed in a sum-based constraint.** Removing a negative number from the left can *increase* the sum, so shrinking might move you further from valid, not closer. LC 862 (Shortest Subarray with Sum at Least K) is the canonical example — the array allows negative numbers, so the standard shrink-on-violation template silently gives wrong answers. The correct tool is prefix sums plus a monotonic deque (fully covered in the Queue sheet, Pattern 4) — not a plain two-pointer window.
- **The constraint is "at least k" instead of "at most k."** Removing an element from the left doesn't reliably restore or break an "at least" condition, because the deficit could be anywhere in the window, not concentrated at the edges. This is Pattern 8's entire lesson (LC 395).
- **The match must preserve order (subsequence, not subset).** LC 727 looks like a minimum-window problem but the ordering constraint means a simple two-pointer shrink can silently skip the correct answer — see Pattern 5's writeup on why it needs backward contraction instead.

**The one-line test before you write any window code:**
> *If I remove the leftmost element right now, does the window definitely move toward — or stay at — validity? If you can't answer yes with certainty, sliding window is the wrong tool until you can.*

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Fixed-Size Window | "subarray/substring of size k", "window of length k" |
| Variable Window — Shrink on Violation | "longest substring with at most...", "no more than k distinct/zeros/replacements/cost" |
| Anagram & Frequency-Match Windows | "find all anagrams", "permutation of s1 in s2", frequency comparison over a fixed span |
| Counting via At-Most Difference | "exactly k distinct", "exactly k odd numbers", "value in range [L,R]" — count, not longest |
| Minimum Window Covering | "minimum window substring/subarray", "smallest subarray with sum ≥ target" |
| Complement / Reframing Trick | Problem asks to *remove* or *exclude* elements to satisfy a condition on the rest |
| Sort + Sliding Window | Window validity depends on values being close together, not their positions |
| Non-Standard Invariant ("At Least K") | "at least k repeating characters" — standard shrink logic does not apply |

---

## Pattern 1: Fixed-Size Window Fundamentals

**Identify:** The problem gives you an explicit window size `k` that never changes throughout the algorithm. You maintain a running aggregate (sum, count, frequency map) — add the incoming element, remove the outgoing element, one at a time as the window slides.

**The universal template:**
```
windowState = init with first k elements
answer = evaluate(windowState)

for right in range(k, n):
    add arr[right] to windowState
    remove arr[right - k] from windowState
    answer = combine(answer, evaluate(windowState))
```

**LC 1423 (Maximum Points from Cards) — the complement reframe:** You're picking `k` cards from either end of the array, but "picking from both ends" is awkward to slide directly. Flip it: the cards you *don't* pick form one **contiguous block of size `n - k`** in the middle. Minimizing that block's sum is a plain fixed-size window problem — total sum minus the minimum window of size `n-k` is the answer. Recognizing "pick from both ends" as "exclude a contiguous middle block" is the entire problem.

**LC 1052 (Grumpy Bookstore Owner) — base value + window boost:** Every minute already contributes a "base" satisfied-customer count regardless of what you do. Compute the base total first, then slide a window of size `k` looking for the maximum *additional* customers you can rescue — the window finds the improvement, not the whole answer.

**LC 2461 (Max Sum of Distinct Subarray of Length K) — fixed window + validity check:** Same fixed-window sliding, but you also maintain a frequency map inside the window to check "are all k elements distinct right now" before counting the sum. This is the bridge into Pattern 3 — fixed size, but the window can be *invalid*, not just numerically evaluated.

**General technique — Circular Sliding Window:** When the array wraps around (last element connects back to the first), the standard fix is to run the same scan twice over the array, indexing with `i % n`, rather than physically duplicating the array. The window logic itself never changes — only how you compute the index does. LC 2134 below is the clean example; the same trick generalizes to any circular-array window problem you'll see elsewhere.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 643. Maximum Average Subarray I | Base template — running sum, add/remove one element per slide |
| 2 | LC 1456. Maximum Number of Vowels in a Substring of Given Length | Running count instead of running sum — same mechanics |
| 3 | LC 1343. Number of Sub-arrays of Size K and Average ≥ Threshold | Running sum + count-if-condition-met, not max |
| 4 | LC 2461. Maximum Sum of Distinct Subarrays With Length K | Fixed window + frequency map validity check — bridge to Pattern 3 |
| 5 | LC 1052. Grumpy Bookstore Owner | Base value + best fixed-window boost — window finds the improvement, not the full answer |
| 6 | LC 1151. Minimum Swaps to Group All 1's Together | Window size = count of 1s; minimize zeros inside any window of that size |
| 7 | LC 2134. Minimum Swaps to Group All 1's Together II *(circular)* | Circular window — run twice, index `% n` |
| 8 | LC 1423. Maximum Points You Can Obtain from Cards | Complement reframe — minimize a size-`(n-k)` middle window instead of maximizing two edges |

---

## Pattern 2: Variable-Size Window — Shrink on Violation

**Identify:** You are maximizing a length (or counting valid windows) subject to an **upper-bound constraint** — "at most k distinct characters," "at most k zeros," "total cost within budget." The window only shrinks when it breaks that constraint. This relies on the monotonicity property discussed above — always confirm it holds before trusting this template. This is the most common sliding window shape in interviews — internalize it before anything else in this sheet.

**The universal template — memorize this exactly:**
```
left = 0
for right in range(n):
    add arr[right] to windowState

    while windowState violates the constraint:
        remove arr[left] from windowState
        left += 1

    answer = max(answer, right - left + 1)   # or accumulate (right - left + 1) for a count
```

**Why `right - left + 1` is always safe to record here, and not in Pattern 5:** By the time you exit the `while` loop, the window is guaranteed valid again — every window you record an answer for is a legal window. In Pattern 5 the update happens *inside* the shrink loop instead, for the opposite reason (see Pattern 5).

**LC 713 (Subarray Product Less Than K) — counting, not measuring length:** Once the window shrinks back to valid, *every* subarray ending at `right` and starting anywhere from `left` to `right` is also valid (a subarray of a valid window is valid). So the count contributed by this position is `right - left + 1`, not just 1. This "count all sub-windows ending here" trick recurs constantly — internalize it now.

**LC 159 / LC 904 are the same problem wearing two costumes:** "At most 2 distinct characters" (LC 159) and "at most 2 fruit types" (LC 904) are structurally identical — a frequency map with a `size() > 2` violation check. LC 340 generalizes both to "at most k."

**LC 1493 (Longest Subarray of 1's After Deleting One Element) — k fixed at 1, not chosen:** Same shrink-on-violation shape as LC 1004, but `k` is implicitly 1 because you must delete exactly one element. The final answer is `(right - left + 1) - 1` — subtract one because the single zero remaining in every valid window represents the mandatory deletion, not a kept element.

**LC 1208 (Get Equal Substrings Within Budget) — the constraint is a running cost, not a count:** Convert each position into a per-character cost (`abs(s[i] - t[i])`), maintain a running total cost as the window slides, and shrink whenever `cost > budget`. Mechanically identical to every other problem in this pattern — the only new idea is that the thing you're bounding is an accumulated cost rather than a count or a distinct-element tally. This is one of the most reusable real-world shapes of variable sliding window: "total cost of the window ≤ budget."

**LC 2302 (Count Subarrays With Score Less Than K) — a synthesis of two ideas already in this pattern:** The constraint is `sum(window) * length(window) < k`. Shrink on violation exactly as usual, then apply LC 713's counting trick — every subarray ending at `right` within the now-valid window also satisfies the constraint, so add `right - left + 1` to the answer at each step. Nothing new mechanically; it's worth doing specifically because it forces you to combine the violation-shrink template with the suffix-counting trick in one problem.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 3. Longest Substring Without Repeating Characters | Base template — violation = duplicate character in window |
| 2 | LC 1493. Longest Subarray of 1's After Deleting One Element | Fixed k=1 zero allowed; subtract 1 for the mandatory deletion |
| 3 | LC 1004. Max Consecutive Ones III | Violation = zero-count in window exceeds k |
| 4 | LC 159. Longest Substring with At Most Two Distinct Characters | Violation = distinct-char count exceeds 2 |
| 5 | LC 904. Fruit Into Baskets | Identical structure to LC 159 — different domain, same technique |
| 6 | LC 340. Longest Substring with At Most K Distinct Characters | Generalizes LC 159/904 to arbitrary k |
| 7 | LC 424. Longest Repeating Character Replacement | Violation = `windowLength - maxFreqCharCount > k` |
| 8 | LC 1208. Get Equal Substrings Within Budget | Violation = running cost exceeds budget — cost window, not a count window |
| 9 | LC 713. Subarray Product Less Than K | Counting variant — every sub-window ending at `right` contributes `right-left+1` |
| 10 | LC 2302. Count Subarrays With Score Less Than K | Synthesis — violation-shrink + LC 713's suffix-counting trick, constraint = `sum × length` |
| 11 | LC 2401. Longest Nice Subarray *(optional)* | Violation = bitwise AND overlap between window elements, not a count — same template, different invalidity check |

---

## Pattern 3: Anagram & Frequency-Match Windows

**Identify:** The window size is fixed (equal to the length of a target string), but validity is decided by comparing the window's **character frequency map** against a target frequency map, not by a running number. Trigger words: "anagram," "permutation of," "same character counts."

**The core technique — maintain a match counter, don't compare full maps every step.** Comparing two full frequency maps at every slide is O(26) per step and wasteful. Instead, maintain a single integer `matched` = "how many characters currently have the exact right count in the window." The precise rule for updating it, spelled out because this is where implementations usually go wrong:

```
need[c] = required frequency of character c
have[c] = current frequency of character c in window

when have[c] increases and becomes exactly need[c]:
    matched += 1
when have[c] increases and becomes need[c] + 1 (was already matched, now overshot):
    matched -= 1

when have[c] decreases and becomes exactly need[c] - 1 (was matched, now short):
    matched -= 1
when have[c] decreases and it was above need[c] (still overshot, now less overshot):
    matched unchanged

window is valid exactly when matched == number of distinct characters in the target
```

The window is valid exactly when `matched` equals the number of distinct target characters.

**LC 438 vs LC 567 — same algorithm, different output:** Find All Anagrams (LC 438) collects every valid start index; Permutation in String (LC 567) just needs to know if *any* valid window exists. If you've built one, the other requires no new logic.

**LC 30 (Substring with Concatenation of All Words) — the hard extension:** Instead of matching single characters, you're matching whole **words** of fixed length. The window size is `numWords × wordLength`, and it slides one *word-length* at a time — you must try `wordLength` different starting offsets (0 through `wordLength - 1`) to cover every possible alignment. Miss this and you silently skip valid answers. The frequency-map matching logic is otherwise identical to LC 438.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 438. Find All Anagrams in a String | Frequency map + `matched` counter — collect every valid start index |
| 2 | LC 567. Permutation in String | Identical to LC 438 — return boolean instead of collecting indices |
| 3 | LC 30. Substring with Concatenation of All Words | Word-level frequency matching — window slides by word length, try every offset 0..wordLength-1 |

---

## Pattern 4: Counting via At-Most / Difference of Prefix-Style Counts

**Identify:** You need to **count** subarrays satisfying a precise constraint — exactly k distinct integers, exactly k odd numbers, or a value that must fall within a range `[L, R]`. Direct sliding-window counting only works cleanly for "at most" constraints (Pattern 2's counting variant). The general principle underneath every problem here:

> **F(exact target) = F(at most upper bound) − F(at most upper bound minus one)**
> **F(value in [L, R]) = F(at most R) − F(at most L − 1)**

Both are the same idea: count everything up to a boundary, count everything up to one boundary lower, subtract. You never write the exact/range counting logic directly — you write the "at most" helper once (an ordinary Pattern 2 counting window) and call it twice.

**LC 992 as the anchor:** "Subarrays with K Different Integers" — write `atMostKDistinct(nums, k)` once (identical shape to LC 340's window), call it with `k` and `k-1`, subtract.

**LC 1248 and LC 2537 are the same trick, different target property:** Nice Subarrays counts exactly-k-odd-numbers; Good Subarrays counts exactly-k-pairs. Recognizing these as the same subtraction — rather than three unrelated hard problems — is the entire value of grouping them here.

**LC 795 — the range-counting sibling, not the exact-k case:** "Number of Subarrays with Bounded Maximum" asks for subarrays whose maximum falls in `[left, right]`. This is the second line of the general principle above — `atMost(right) - atMost(left - 1)` — isolating a value range instead of a single exact value.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 992. Subarrays with K Different Integers | Anchor — `atMostKDistinct(k) - atMostKDistinct(k-1)` |
| 2 | LC 1248. Count Number of Nice Subarrays | Same trick — target property is "odd number count" instead of "distinct count" |
| 3 | LC 2537. Count the Number of Good Subarrays | Same trick — target property is "pair count" (`freq*(freq-1)/2` running total) |
| 4 | LC 795. Number of Subarrays with Bounded Maximum | Range-counting case — `atMost(right) - atMost(left-1)` isolates `[L,R]`, not a single value |

---

## Pattern 5: Minimum Window Covering

**Identify:** You need the **smallest** window satisfying a constraint — "contains all characters of t," "sum ≥ target." The window expands right until it *becomes* valid, then shrinks left *while it stays valid*, recording the minimum length at every shrink step — the mirror image of Pattern 2's shrink-on-violation. As with Pattern 2, this depends on monotonicity holding — LC 209 and LC 76 both rely on non-negative quantities (lengths/counts can't go negative; LC 209's sums are explicitly positive).

**The universal template:**
```
left = 0
for right in range(n):
    add arr[right] to windowState

    while windowState is valid:
        answer = min(answer, right - left + 1)
        remove arr[left] from windowState
        left += 1
```

**Why the update sits inside the `while` here, not after it (contrast with Pattern 2):** In Pattern 2, you want the *largest* valid window, so you record the answer right after restoring validity. Here you want the *smallest* valid window, so you must record the answer at every single point the window is still valid — including the instant right before it's about to become invalid.

**LC 209 as the gentle entry point:** "Minimum Size Subarray Sum" uses a plain running sum — the simplest possible version of this template, since validity is just `sum >= target`, no frequency map needed.

**LC 76 as the anchor:** "Minimum Window Substring" adds the frequency-map + `matched` counter machinery from Pattern 3 on top of the shrink-while-valid template from this pattern — it is literally the synthesis of Pattern 3's matching logic and Pattern 5's shrink direction. Do Pattern 3 before attempting LC 76.

**LC 727 — the exception that needs a different algorithm entirely:** "Minimum Window Subsequence" sounds identical to LC 76 but asks for a **subsequence** match (`t`'s characters must appear in `s` *in order*). The standard two-pointer shrink doesn't verify order — order is exactly the kind of constraint flagged in the Monotonicity section above as breaking the simple template. The actual technique: expand right until `t` is matched as a subsequence, then walk **backward** from that point to find the tightest starting point that still preserves the subsequence, then continue expanding from there. This backward-contraction step is the one genuinely new mechanic in this entire sheet.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 209. Minimum Size Subarray Sum | Gentle entry point — running sum, no frequency map |
| 2 | LC 76. Minimum Window Substring | Anchor — Pattern 3's frequency matching + Pattern 5's shrink-while-valid |
| 3 | LC 727. Minimum Window Subsequence | Different algorithm — order-preserving match requires backward contraction, not simple shrink |

---

## Pattern 6: Complement / Problem-Reframing Trick

**Identify:** The problem asks you to **remove, exclude, or discard** elements so that what remains satisfies some condition — rather than directly describing a window to keep. The signal: if your first instinct produces an awkward window (non-contiguous, or defined by what's *missing*), ask whether the complement of what's being removed is a clean, ordinary window problem instead.

**LC 1658 (Minimum Operations to Reduce X to Zero) — the anchor:** You remove elements only from the two ends, which sounds unlike a sliding window at all. Reframe: whatever you *don't* remove is one contiguous middle subarray, and its sum must equal `totalSum - x`. Since all values are positive (monotonicity holds — see the section above), this becomes an ordinary variable window search for a subarray with an exact target sum. The number of operations is `n - (length of the longest such subarray)`.

**Why this pattern exists separately from Patterns 1 and 2:** LC 1423 (Pattern 1) and LC 1658 (this pattern) use the *same* reframing insight — "solve the complement instead" — but land in different mechanical patterns once reframed (fixed window vs. variable window). Grouping the *insight* here, while keeping the mechanical execution cross-referenced back to Patterns 1/2, is what makes this transferable to a genuinely unseen problem: the recognition step is "can I complement this," not "which template does this look like."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1658. Minimum Operations to Reduce X to Zero | Reframe as "longest subarray with sum == totalSum − x" — variable window, exact target |
| — | LC 1423 *(cross-ref: Pattern 1)* | Same reframing instinct, lands as a fixed-size complement window instead |

---

## Pattern 7: Sort + Sliding Window

**Identify:** The window's validity depends on the **values being close to each other**, not on their original positions in the array. Sorting first turns "are these values close" into "is this a contiguous run in sorted order" — which a normal window can then slide across. This is the pattern most likely to be missed entirely, because after sorting, the problem no longer resembles the original array at all.

**Why this is its own pattern and not just Pattern 2 with a sort step tacked on:** In every earlier pattern, the window's order matches the problem's natural order (array index, string position). Here, you deliberately destroy that order first. The recognition skill is different: you have to notice that position doesn't matter — only value proximity does — before sorting even occurs to you as an option.

**LC 2779 (Maximum Beauty of an Array After Applying Operation) — the gentle entry point:** After sorting, two elements can be made equal if they're within `2k` of each other. The window is valid exactly when `nums[right] - nums[left] <= 2k` — the simplest possible version of "closeness after sorting."

**LC 1838 (Frequency of the Most Frequent Element) — the anchor:** After sorting, ask "for a window ending at index `right`, how many operations does it take to raise every element in the window up to `nums[right]`?" That cost is `nums[right] * windowLength - windowSum`. If the cost exceeds `k`, shrink from the left — an ordinary shrink-on-violation loop (Pattern 2), just running over sorted values instead of original positions.

**Cross-reference:** This same "sort, then maintain a size-k invariant while sweeping" idea powers several Heap sheet Pattern 1 problems (LC 857, LC 2542) — there the invariant is maintained with a heap instead of two pointers because the constraint isn't a simple contiguous-value window. If the sorted window can be tracked with two pointers, it belongs here; if it needs a priority ordering on a second variable, it belongs in the Heap sheet.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 2779. Maximum Beauty of an Array After Applying Operation | Gentle entry point — sort, then window where `nums[right]-nums[left] <= 2k` |
| 2 | LC 1838. Frequency of the Most Frequent Element | Sort, then window where `nums[right]*len - windowSum <= k` |

---

## Pattern 8: Non-Standard Invariant — "At Least K" (The Trap)

**Identify:** The problem says "at least k," not "at most k." This single word breaks monotonicity (see the section above) — removing an element from the left doesn't reliably move you toward or away from validity, because the deficit could be anywhere in the window. **Standard two-pointer sliding window does not work on this problem at all** — recognizing that is more valuable than any specific technique for solving it.

**LC 395 (Longest Substring with At Least K Repeating Characters) — the actual technique:** Two layers, neither of which is a simple two-pointer scan.
1. **Divide and conquer on the "blocking" character:** any character in the substring with a total count less than `k` can never be part of a valid answer — it's a hard wall. Split the string on every character whose frequency is below `k`, and recurse independently on each piece.
2. **Fixed-distinct-count sweep (an alternative, non-recursive technique):** since the alphabet is bounded (≤26 letters), fix "the substring uses exactly `t` distinct characters" for each `t` from 1 to 26, and run a *bounded* two-pointer scan under that fixed distinct-count constraint — this recovers monotonicity because `t` is now fixed, not "at least."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 395. Longest Substring with At Least K Repeating Characters | Standard shrink-on-violation does NOT apply — divide-and-conquer on blocking characters, or fixed-distinct-count sweep |

---

## Cross-References (Fully Covered Elsewhere — Don't Duplicate)

| Problem | Primary Home | Why It's Not Here |
|---|---|---|
| LC 239. Sliding Window Maximum | Queue sheet, Pattern 3 | Monotonic deque, not a two-pointer window |
| LC 1438. Longest Continuous Subarray with Abs Diff ≤ Limit | Queue sheet, Pattern 3 | Requires two monotonic deques (max + min), not plain two pointers |
| LC 2762. Continuous Subarrays | Queue sheet, Pattern 3 | Same two-deque mechanism as LC 1438 |
| LC 862. Shortest Subarray with Sum at Least K | Queue sheet, Pattern 4 | Negative numbers break sum monotonicity — needs prefix sum + monotonic deque, not a plain window |
| LC 220. Contains Duplicate III | BST/Ordered Set sheet, Pattern 8 | Window validity needs an ordered set (TreeSet), not a hashmap |
| LC 1696 / LC 1425 | DP sheet Pattern 1, Queue sheet Pattern 4 | Sliding Window DP — deque optimizes a DP recurrence, not a raw window |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Fixed-Size Window Fundamentals | 8 | Add one, remove one, every slide |
| Variable Window — Shrink on Violation | 11 | Expand freely, shrink only when invalid, measure after |
| Anagram & Frequency-Match Windows | 3 | Frequency map + `matched` counter, fixed size |
| Counting via At-Most Difference | 4 | `F(target) = F(upper) - F(upper-1)` and its range-counting form |
| Minimum Window Covering | 3 | Expand until valid, shrink while valid, measure inside the shrink |
| Complement / Reframing Trick | 1 (+1 cross-ref) | Solve what remains after removal, not the removal itself |
| Sort + Sliding Window | 2 | Sort first — window tracks value proximity, not position |
| Non-Standard Invariant ("At Least K") | 1 | The trap — standard template provably does not apply |
| **Total** | **~33 new + 6 cross-referenced** | |

---

## How to Use This Sheet

**Read the Monotonicity section before touching Pattern 1.** It's not optional theory — it's the reason any of the templates below are trustworthy. Every time you meet an unfamiliar sliding window problem, run the one-line monotonicity test before writing code, not after getting a wrong answer.

**Pattern 1 and Pattern 2 are the mandatory foundation, in that order.** Pattern 1's add-one-remove-one mechanics must be automatic before Pattern 2 adds the complexity of a conditional shrink. Say "am I shrinking because it broke, or because I'm looking for the smallest window" out loud every time until it's reflexive.

**Pattern 3 before Pattern 5.** LC 76 is the synthesis of both — attempting it before the frequency-map `matched`-counter transition rule is solid will make it feel like a brand-new hard problem instead of two familiar pieces combined.

**Pattern 4 requires Pattern 2's counting variant (LC 713) to be clean first.** The at-most subtraction principle is "write the Pattern 2 counting helper once, call it twice" — if that variant isn't automatic, Pattern 4 will feel like memorization instead of composition.

**Patterns 6 and 7 are the two places most people get stuck on genuinely unseen problems** — not because the two-pointer mechanics are hard, but because *recognizing* that a reframe or a sort is needed at all is the actual skill. When a problem doesn't visibly fit Patterns 1–5, ask: "would this become a clean window if I looked at the complement?" (Pattern 6) or "does closeness in value matter more than position?" (Pattern 7).

**Pattern 8 (LC 395), and the Cross-References table, should be done last, deliberately, as a lesson in when to stop forcing the template.** Walking into LC 395 or LC 862 expecting Pattern 2's logic to work, watching it fail, and understanding *why* via the Monotonicity section is more instructive than either solution on its own.