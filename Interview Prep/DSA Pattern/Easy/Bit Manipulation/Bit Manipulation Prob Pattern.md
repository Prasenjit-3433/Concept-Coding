# Bit Manipulation DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

Almost everything in this topic reduces to three building blocks, and every pattern below is one of them applied repeatedly:

```
Get bit i:    (n >> i) & 1
Set bit i:    n | (1 << i)
Clear bit i:  n & ~(1 << i)
Toggle bit i: n ^ (1 << i)
```

**The two properties that do 80% of the work in this entire sheet:**
- **XOR cancels itself:** `a ^ a = 0` and `a ^ 0 = a`. Anything that appears an *even* number of times vanishes under XOR, and order doesn't matter (XOR is commutative and associative). This single fact is the engine behind almost every "find the unique element" problem.
- **`n & (n-1)` clears the lowest set bit.** This is Brian Kernighan's algorithm — it turns "count the set bits" from an O(32)-per-number loop into a loop that runs exactly as many times as there are set bits, and it's the building block for several problems that ask "how many steps until this becomes 0."

**When to reach for bit manipulation at all:** the strongest signal is small, fixed-size input (≤ ~20–30 elements, or a fixed 32-bit integer range) combined with words like "unique," "missing," "appears once/twice/k times," "XOR," "subset," or "without using +/− operators."

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Bit Fundamentals | "power of two", "count set bits", "reverse bits", "steps to reduce to zero" |
| XOR Cancellation (Single Number family) | "appears once/twice/k times, all others appear k times", "find the missing/duplicate number" |
| Prefix XOR | "XOR of subarray", "decode XORed array/permutation", "triplets with equal XOR" |
| Bitmask as Set/State Encoding | "subset of characters", "which letters appear", "word puzzle", small alphabet |
| Bitwise Trie for XOR Maximization | "maximum XOR of two numbers", "maximum XOR pair", element from array |
| Range & Arithmetic Bit Tricks | "bitwise AND of a range", "sum without +", "reverse bits", "gray code" |

---

## Pattern 1: Bit Fundamentals

**Identify:** The problem asks you to directly manipulate or count bits of a single number — no array of numbers to combine, no cancellation trick. These are the primitives everything else in this sheet builds on, and they're worth drilling in isolation before combining them with XOR logic in later patterns.

**LC 191 vs LC 338 — same primitive, different framing:** Counting set bits in one number (LC 191) and counting set bits across a whole range `0..n` (LC 338) look different, but LC 338's DP recurrence is just `dp[i] = dp[i >> 1] + (i & 1)` — "the set-bit count of `i` equals the set-bit count of `i` with its last bit dropped, plus that last bit." Once LC 191 is automatic, LC 338 is a one-line recurrence, not a new problem.

**LC 1342's "steps to reduce to zero":** Even/odd check via `n & 1`, then either subtract 1 or right-shift. This is the most direct possible test of "do you reach for bit operations instead of arithmetic operations by default."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 191. Number of 1 Bits | Brian Kernighan's — `n & (n-1)` clears the lowest set bit; loop until 0 |
| 2 | LC 231. Power of Two | A power of two has exactly one set bit — `n > 0 && (n & (n-1)) == 0` |
| 3 | LC 1342. Number of Steps to Reduce a Number to Zero | `n & 1` for parity check, halve or decrement — direct primitive drilling |
| 4 | LC 338. Counting Bits | DP recurrence `dp[i] = dp[i>>1] + (i&1)` — reuses LC 191's insight, don't recompute per number |
| 5 | LC 461. Hamming Distance | XOR the two numbers, count set bits in the result (LC 191 as a subroutine) |
| 6 | LC 190. Reverse Bits | Shift-and-build — pull the lowest bit off one number, push it onto the top of another |

---

## Pattern 2: XOR Cancellation (Single Number Family)

**Identify:** Every element in the input appears some fixed number of times except exactly one (or two), and the goal is to isolate the exception(s). The signal that separates this from ordinary hashmap counting: the problem constraints usually demand **O(1) extra space**, which a frequency map can't give you — XOR's self-cancelling property can.

**LC 136 as the anchor:** XOR every element together. Every value that appears twice cancels itself out (`a ^ a = 0`); whatever survives is the one that appeared once. This is the entire problem in one line, and every other problem in this pattern is a variation on making cancellation work under a harder constraint.

**LC 260's split-by-a-set-bit trick — the key idea for "two uniques instead of one":** XOR everything together first; the result is `unique1 ^ unique2` (nonzero, since the two are different). Pick any set bit in that result — it's a bit where `unique1` and `unique2` differ. Partition all numbers into two groups based on that bit, and XOR each group separately. Since every duplicated pair shares the same bit value there, duplicates stay together in one group and cancel out, while `unique1` and `unique2` land in different groups and survive.

**LC 137's bit-counting generalization — the key idea for "appears 3 times instead of 2":** Simple XOR only cancels pairs, so it fails when everything but one element appears **three** times. The fix: count the number of 1s at each bit position across *all* numbers. Any bit position whose count isn't divisible by 3 must have a contribution from the unique element — reconstruct it bit by bit. This generalizes to "appears k times" for any k.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 136. Single Number | Pure XOR cancellation — the anchor for the entire pattern |
| 2 | LC 268. Missing Number | XOR indices with values — the missing number is what survives |
| 3 | LC 645. Set Mismatch | XOR trick combined with sum trick to isolate both the duplicate and the missing value |
| 4 | LC 137. Single Number II | Bit-counting mod 3 — generalizes XOR cancellation beyond pairs |
| 5 | LC 260. Single Number III | Split into two groups via a differing set bit, then XOR each group independently |

---

## Pattern 3: Prefix XOR

**Identify:** The problem asks about the XOR of a **subarray** `[l, r]`, possibly across multiple queries, or asks you to reconstruct an original array from a XORed encoding of adjacent elements. This is the direct XOR-analogue of prefix sum: because XOR is invertible (`a ^ a = 0`), `XOR(l, r) = prefix[r] ^ prefix[l-1]` in exactly the same way `sum(l, r) = prefix[r] - prefix[l-1]` works for addition.

**Why this deserves to be its own pattern, not folded into Pattern 2:** Pattern 2 uses XOR to cancel *duplicates within a single pass over the whole array*. This pattern uses XOR to answer *range queries*, which requires the prefix-array construction step first — a genuinely different setup, even though the underlying cancellation property is the same.

**LC 1720 / LC 1734 — decoding as prefix-XOR in reverse:** If you're given `encoded[i] = arr[i] ^ arr[i+1]`, then `arr[i+1] = arr[i] ^ encoded[i]` — knowing just the *first* element of the original array lets you reconstruct the rest by walking forward and un-XORing one step at a time. LC 1734 adds a twist: the first element isn't given, but XORing all `encoded[odd indices]` together recovers it, since XORing a full permutation of `1..n` together is a computable fixed quantity.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1310. XOR Queries of a Subarray | Prefix XOR array — each query answered in O(1) via `prefix[r+1] ^ prefix[l]` |
| 2 | LC 1720. Decode XORed Array | Reverse the encoding — `arr[i+1] = arr[i] ^ encoded[i]`, walk forward from the known first element |
| 3 | LC 1734. Decode XORed Permutation | Same idea as LC 1720, but the first element must be derived from XOR-ing all of `1..n` and all odd-indexed encoded values |
| 4 | LC 1442. Count Triplets That Can Form Two Arrays of Equal XOR | Prefix XOR — `XOR(i,k) = 0` means `XOR(i,j-1) == XOR(j,k)`, reduces triplet counting to prefix XOR matching |
| 5 | LC 1863. Sum of All Subset XOR Totals | Every bit that's set in any element contributes to exactly half of all subsets — combinatorial shortcut, or brute-force backtracking for small n *(cross-ref: Recursion & Backtracking sheet, Pattern 2)* |

---

## Pattern 4: Bitmask as Set / State Encoding

**Identify:** The problem involves a **small, fixed alphabet or item count** (usually ≤ 26 letters, or a small fixed set of categories), and you need to represent "which of these are present" compactly and compare that presence cheaply. A bitmask turns "does this word contain any of these letters" into a single integer AND/OR operation instead of a set intersection.

**Why this is distinct from DP on Bitmask (already in your DP sheet, Pattern 10) and Subsets (Recursion & Backtracking sheet, Pattern 2):** Those patterns use a bitmask to track **which elements of a small array have been processed** — the mask evolves across recursive/DP states. This pattern uses a bitmask to encode **which characters are present in a string**, as a static fingerprint for fast comparison — there's no state transition, just an encoding trick.

**LC 187's rolling-hash-via-bitmask:** Encoding each 4-character DNA substring (alphabet size 4: A, C, G, T) as a 2-bit-per-character integer turns substring comparison into integer comparison in a hashmap — no string hashing needed.

**LC 1178's mask-subset-enumeration trick:** For each puzzle, only words whose letter-set is a *subset* of the puzzle's letters (and contains the puzzle's first letter) count. Precompute a frequency map of word-masks, then for each puzzle, enumerate all subsets of its own mask (via the standard `sub = (sub-1) & mask` subset-enumeration trick) and sum matching word counts — turns an O(words × puzzles) problem into something tractable.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 187. Repeated DNA Sequences | 2-bit-per-character bitmask encoding — substring becomes an integer key |
| 2 | LC 1178. Number of Valid Words for Each Puzzle | Bitmask per word + subset-enumeration over the puzzle's mask |

---

## Pattern 5: Bitwise Trie for XOR Maximization

**Identify:** The problem asks for the **maximum XOR** achievable between pairs of numbers (or between a query number and a fixed array), across many candidates. Brute-force pairwise XOR is O(n²); a **trie built on the binary representation of each number** (32 levels deep, 2 children per node: bit 0 or bit 1) answers each query in O(32) by greedily choosing, at each level, the child that *differs* from the query's current bit — because XOR is maximized by mismatched bits.

**Why this pattern was entirely missing from both source sheets, and why it matters:** This is a genuinely different data structure from anything else in this sheet — it's the Trie-Based Design pattern (from your DS Design sheet) applied to **binary digits instead of characters**. Once you see "trie over bit representations" as its own reusable idea, LC 421 and LC 1707 stop looking like standalone hard problems and become "build the trie, then greedy-walk it" — the exact same two-step shape every time.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 421. Maximum XOR of Two Numbers in an Array | Build a bitwise trie of all numbers, then for each number greedily walk toward the opposite bit at every level |
| 2 | LC 1707. Maximum XOR With an Element From Array | Same trie, but with an offline sort-by-limit sweep — only insert numbers ≤ each query's limit before answering it |
| 3 | LC 2932. Maximum Strong Pair XOR I *(optional, easier variant)* | Same trie idea under a simpler `|x - y| <= min(x, y)` pairing constraint |

---

## Pattern 6: Range & Arithmetic Bit Tricks

**Identify:** A grab-bag of problems that don't share one mechanism, but share the instinct of "replace an arithmetic or brute-force operation with a bit-level equivalent." Each one is a standalone trick worth knowing individually rather than a generalizable template — treat this pattern as a checklist, not a single recipe.

**LC 201 — common-prefix-of-a-range:** The bitwise AND of every number in `[m, n]` is just the common binary prefix shared by `m` and `n` — any bit that differs somewhere in the range gets zeroed out by at least one number in between. Right-shift both `m` and `n` together until they're equal, then shift back.

**LC 371 — addition without `+`:** `a ^ b` gives the sum ignoring carries; `(a & b) << 1` gives exactly the carries that were ignored. Repeat XOR-and-carry until there's no carry left — this is literally how a hardware adder works, one bit position at a time.

**LC 89 — Gray code's reflect-and-prefix trick:** The n-bit Gray code sequence is the (n-1)-bit sequence, followed by the (n-1)-bit sequence *reversed* with a leading 1 bit set. This recursive reflection guarantees adjacent entries differ by exactly one bit.

**LC 3097 — binary search on the answer, OR is monotonic once you fix a starting point:** "Shortest subarray with OR ≥ k" pairs a sliding window with the fact that OR-ing more elements can only add bits, never remove them — the window can shrink from the left safely once the OR condition is met, similar in spirit to the Binary Search sheet's Pattern 4 feasibility checks, but driven by OR-monotonicity instead of a numeric threshold.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 201. Bitwise AND of Numbers Range | Right-shift both bounds together until equal — finds the shared binary prefix |
| 2 | LC 371. Sum of Two Integers | `a^b` = sum without carry, `(a&b)<<1` = the carry — repeat until no carry remains |
| 3 | LC 89. Gray Code | Reflect-and-prefix construction — each step mirrors the previous sequence with a new leading bit |
| 4 | LC 3097. Shortest Subarray With OR at Least K II | Sliding window exploiting OR's monotonic-growth property |
| 5 | LC 1542. Find Longest Awesome Substring | Bitmask of digit parity (10 bits) + prefix-XOR-style hashmap — at most one digit allowed odd count for a palindrome-rearrangeable substring |
| 6 | LC 2939. Maximum Xor Product | Greedy bit-by-bit construction from MSB down — balance which number gets each differing bit to maximize the product |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Bit Fundamentals | 6 | get/set/clear/toggle bit, Brian Kernighan's algorithm |
| XOR Cancellation (Single Number family) | 5 | `a^a=0` cancellation, generalized via bit-counting mod k |
| Prefix XOR | 5 (1 cross-ref) | XOR-analogue of prefix sum — invertibility enables O(1) range queries |
| Bitmask as Set/State Encoding | 2 | Small alphabet → integer fingerprint for O(1) comparison |
| Bitwise Trie for XOR Maximization | 3 | Trie over binary digits — greedy opposite-bit walk |
| Range & Arithmetic Bit Tricks | 6 | Standalone tricks — common prefix, carry propagation, reflection, monotonic OR |
| **Total** | **~27 problems** | |

---

## How to Use This Sheet

**Pattern 1 is mandatory first, even though it feels almost too simple.** If `n & (n-1)` and `(n >> i) & 1` aren't automatic, every later pattern's explanation will feel like it's skipping a step, because it is.

**Pattern 2 is the highest-frequency pattern in this entire sheet.** Single Number variants show up constantly as easy/medium warm-up questions. LC 136 → LC 268 → LC 260 → LC 137 is the correct difficulty progression — each one adds exactly one new idea on top of the last.

**Pattern 3 requires Pattern 2's XOR fluency but is otherwise standalone.** If prefix *sum* is already second nature from your DP sheet, treat this pattern as "the same idea, XOR instead of addition" rather than something new.

**Pattern 4 has light prerequisites but real payoff for word/puzzle-style problems** — it's a small pattern, but it recurs disguised as a "hard" problem specifically because most people don't think to reach for a bitmask when the input is strings, not numbers.

**Pattern 5 is the one genuinely new data structure in this sheet — treat it with the same seriousness as your Trie-Based Design pattern in DS Design.** Do not attempt LC 1707 before LC 421's basic greedy-trie-walk is completely automatic.

**Pattern 6 last, as a checklist rather than a linear progression.** These problems don't build on each other — pick them off independently, and don't worry about "mastering" the pattern as a whole the way you would with Patterns 1–5.