# Meet-in-the-Middle DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Note on Source

Same as the LCA sheet — no Fraz/Algomaster raw aggregation exists for this topic, so this is built from standard competitive programming and interview coverage rather than filtered from a collected list. Flag anything here that doesn't match what you've seen elsewhere so it can be corrected.

---

## The Core Mental Model — Before Any Pattern

Meet-in-the-Middle is not really a new algorithm — it is a way of surviving an exponential search space that is too big to brute-force whole, but small enough to brute-force **half**.

**Why this exists at all:** A lot of problems have a brute-force solution that is O(2^n), because every element has a binary choice — include or exclude. When `n` is around 40, `2^n` is far too large to enumerate directly (roughly 10^12). But `2^(n/2)` at `n = 40` is only about a million — completely fine. The trick: split the input into two halves of size `n/2` each, enumerate every possible outcome for each half separately (roughly `2^(n/2)` outcomes per half), and then **combine** the two halves cleverly — usually via sorting plus binary search, or a hashmap — rather than trying every pairing directly, which would bring back the O(2^n) cost through the back door.

**The one-line test for recognizing this pattern:**
> Does the constraint say something like `n <= 40`, and does the brute force look like "try every subset"? That specific range — too big for `2^n`, small enough for `2^(n/2)` — is the single strongest signal in this entire topic.

Contrast this with the Recursion & Backtracking sheet's `n <= 20-25` trigger for plain backtracking with pruning — meet-in-the-middle exists specifically because that `n` roughly doubles once you split the exponent in half.

**The general recipe, stated once so every pattern below can refer back to it:**
1. Split the `n` elements into two halves, `A` and `B`, each of size `n/2`.
2. Enumerate all `2^(n/2)` possible subset outcomes of `A`, and all `2^(n/2)` possible subset outcomes of `B`, independently.
3. Sort one of the two resulting lists (usually whichever side makes the query direction convenient).
4. For each outcome from the other half, binary search or hashmap-lookup against the sorted/indexed list to find a matching or best-fitting counterpart.

The split-and-enumerate step (1–2) is almost always identical across problems in this topic. The **combine** step (3–4) is where problems genuinely differ — pattern recognition here is really about recognizing *what kind of combine step* a given problem needs.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Subset Sum Feasibility & Closest Sum | `n` up to ~40, "does a subset sum to exactly target", "closest subset sum to target", capacity/budget framing |
| Counting Pairs Across Two Halves | "count the number of subsets", "count pairs from two halves summing to X" — counting, not just existence |
| Maximize / Minimize a Combined Value | "maximize XOR of two halves", "partition into two groups minimizing difference", `n` too large for direct DP |
| Meet-in-the-Middle on Graphs / Paths | "shortest path with an exponential state space", state space itself splits naturally into two independent halves |

---

## Pattern 1: Subset Sum Feasibility & Closest Sum

**Identify:** The classic anchor for the entire topic. `n` is too large for `2^n` subset enumeration (roughly 30–40), but the underlying question is still "does some subset achieve this sum" or "what subset sum is closest to a target." The 0/1 Knapsack DP from your DP sheet does not scale here, because the target or capacity itself can be astronomically large, ruling out the usual O(n × capacity) DP table.

**Why this is the gentlest entry point:** There is no cleverness in the combine step yet — just sort one half and binary search from the other, the plainest possible version of the general recipe above.

**The anchor mechanic:** Split into `A` and `B`. Generate every subset sum of `A`, generate every subset sum of `B`, sort `B`'s sums. For each sum in `A`, binary search for `target - sum` in `B`'s sorted list — either for an exact match (feasibility), or the closest value using the Binary Search sheet's lower-bound machinery directly (floor/ceil around the needed complement).

**Why sorting only one side is enough:** You never need to sort both halves — only the side you're querying *into*. The side you're iterating *from* stays in whatever order enumeration produced it, since each of its values is looked up independently.

**CSES: Meet in the Middle — the textbook statement of this exact problem:** Given `n <= ~40` elements, count (or just decide feasibility of) subsets summing to exactly `X`. This is deliberately the least-disguised version of the pattern — do it first, before any problem adds a story on top.

**Subset Sum Closest to Zero / Target — the "closest," not "exact," variant:** Once both halves' sums are generated and one is sorted, for every sum in the unsorted half, a lower-bound binary search against the sorted half finds the tightest bracketing pair (the value just below and just above the ideal complement) — check both neighbors, since the closest overall sum could come from either side of the exact complement.

**Partition into Two Subsets with Given Sum Constraints — direct application to a Knapsack-shaped problem, but at a scale plain Knapsack DP cannot reach:** When capacity itself is huge (not bounded by a small `n`), meet-in-the-middle is the only tool between brute force and infeasibility — the same recognition test as the Core Mental Model's "n too big for 2^n, small enough for 2^(n/2)."

| # | Problem | Key Concept |
|---|---|---|
| 1 | CSES: Meet in the Middle | Anchor — split, enumerate all subset sums of each half, sort one side, binary search the other for exact match |
| 2 | Subset Sum Closest to a Given Target | Same split/enumerate, but lower-bound binary search brackets the ideal complement from both sides |
| 3 | Partition Array into Two Subsets with Sum Constraints (large capacity) | Same technique applied where capacity is too large for standard 0/1 Knapsack DP to run at all |

---

## Pattern 2: Counting Pairs Across Two Halves

**Identify:** The question shifts from "does a valid combination exist" to "**how many** valid combinations exist." Once both halves' outcomes are generated, this becomes a direct application of the Two Pointers sheet's counting variant (LC 259/611) or the Prefix Sum + HashMap sheet's frequency-counting idea — applied to two independently generated lists instead of one array.

**Why this needs a different combine step than Pattern 1:** Binary search alone tells you *whether* a match exists, or finds *one* matching position — it does not directly tell you *how many* elements satisfy a range condition around that position. Counting requires either a frequency hashmap (for exact-value matches) or counting the width of a valid range in the sorted array (for inequality-based matches, reusing the "count everything between these two pointers" trick).

**The core mechanic:** Generate every subset sum of `A` and every subset sum of `B`. Build a frequency map (or sorted array) of `B`'s sums. For each sum in `A`, the count of valid partners is either `freqMap[target - sum]` (exact-sum counting) or `upperBound(bound) - lowerBound(bound)` in `B`'s sorted array (range/inequality counting) — direct reuse of machinery you already have, just supplied with a meet-in-the-middle-generated list instead of a raw input array.

**4Sum II — the clean, undisguised version of this pattern:** Given four arrays, count quadruples `(i,j,k,l)` where `nums1[i] + nums2[j] + nums3[k] + nums4[l] == 0`. This is *already* pre-split into two natural halves — compute every pairwise sum of the first two arrays into a frequency map, then for every pairwise sum of the last two arrays, look up its negation. No enumeration step is even needed here since the "halves" are handed to you already split; it's the purest illustration of the combine step in isolation.

**Counting Subsets with Sum in a Range — combining Pattern 1's split with a range-counting combine step:** Same subset-sum generation as Pattern 1, but the answer is the total count of pairs `(sumA, sumB)` where `low <= sumA + sumB <= high`, which reduces to, for every `sumA`, counting how many `sumB` values fall in `[low - sumA, high - sumA]` — a direct two-binary-search range count on the sorted half, echoing the Prefix Sum sheet's "range counting via two boundary lookups" principle.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 454. 4Sum II | Pre-split into two natural halves — pairwise sums of the first two arrays hashed, negation looked up from the last two |
| 2 | Count Subsets with Sum in a Given Range | Same split/enumerate as Pattern 1, combine step is a range count via two binary searches per element |
| 3 | Count Pairs of Subsets with Sum Divisible by K | Same combine step, keyed on remainder mod K instead of raw sum — direct reuse of the Prefix Sum + HashMap modular trick |

---

## Pattern 3: Maximize / Minimize a Combined Value

**Identify:** Instead of a fixed target to match, the goal is to **optimize** something across the two halves — maximize a XOR, minimize the absolute difference between two group sums, or maximize a weighted combination. The combine step now needs a structure that supports an optimization query, not just an equality or range check.

**Minimize the absolute difference between two partition sums — the anchor for this pattern:** Generate every subset sum of `A` and every subset sum of `B`, sort one side. For each sum in the other half, a lower-bound binary search finds the closest achievable total on the sorted side — track the minimum absolute difference seen across every such lookup. This is structurally identical to Pattern 1's closest-sum mechanic, but the *goal* here is genuinely a minimization over the whole search, not just closeness to one fixed target.

**Maximum XOR of two halves — the combine step needs a bitwise trie, not a sorted array:** When the optimization is a XOR maximization rather than a numeric closeness, sorting doesn't help — XOR isn't monotonic with respect to numeric order. Build a **bitwise trie** (cross-ref: Bit Manipulation sheet, Pattern 5) over all of `B`'s subset values, then for each value in `A`, greedily walk the trie toward the opposite bit at every level, exactly as in LC 421 — meet-in-the-middle here supplies *what goes into* the trie; the trie-walk itself is unchanged from the Bit Manipulation sheet's existing technique.

**Why this pattern is really "meet-in-the-middle generates the candidates, a technique you already know optimizes over them":** Every problem here reuses a fully-built technique from another sheet (binary search closeness from Pattern 1, the bitwise trie from Bit Manipulation) — the only genuinely new content in this pattern is recognizing that the *candidate lists themselves* need meet-in-the-middle generation before that existing technique can be applied at all.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Partition Array to Minimize Absolute Difference of Two Subset Sums | Split/enumerate, then closest-value binary search on the sorted half — minimize over every lookup, not just one target |
| 2 | Maximum XOR of Two Subsets from Two Halves | Meet-in-the-middle generates both halves' subset values; combine step is a bitwise trie walk, cross-ref Bit Manipulation sheet Pattern 5 |

---

## Pattern 4: Meet-in-the-Middle on Graphs / Paths

**Identify:** The exponential blow-up isn't from subset generation over an array — it's from a **state space** (a sequence of moves, a path with an exponential number of possible extensions) that happens to split naturally into two independent halves which can be searched separately and joined in the middle. This is the same underlying idea as Patterns 1–3, but the "two halves" are two ends of a search rather than two halves of an input array.

**Why this deserves separation from the array-based patterns above:** The recognition trigger is different — there's no explicit array to literally cut in half. Instead, the insight is noticing that a long sequence of moves/choices can be searched forward from the start and backward from the end simultaneously, and that the two searches only need to agree at some middle point. This is the same "search from both directions" instinct as the Two Pointers sheet's **Bidirectional BFS** note (Graph sheet, Pattern 3) — worth explicitly connecting the two, since bidirectional BFS is arguably meet-in-the-middle's graph-search sibling rather than an unrelated technique.

**LC 1755 (Closest Subsequence Sum) — array-flavored but belongs here as the bridge problem:** Structurally identical to Pattern 1/3's split-and-binary-search mechanic, included here specifically as the bridge between "meet in the middle on an array" and "meet in the middle on a search space," since the two subset-sum lists genuinely behave like two independent search halves being joined at a target value.

**Word Ladder-style shortest path with an exponential branching factor — the genuine graph-search version:** When BFS from a single source would explore a branching factor that makes full forward search infeasible, search forward from the source and backward from the destination simultaneously, stopping the moment the two frontiers meet. This is bidirectional BFS by name (Graph sheet's own cross-reference, via LC 127) — flagged here explicitly as the same underlying "split the exponential search in half" idea as every array-based problem above, not a separate algorithm to learn from scratch.

**Generating all reachable states within k moves, from both ends — the pattern's clearest "two independent halves of a search" example:** When a problem asks whether a target state is reachable within a move budget too large to search forward exhaustively, but exactly splittable (e.g. exactly half the moves from each direction), generate every state reachable in `k/2` moves forward from the start and every state reachable in `k/2` moves backward from the target, then check for any overlap — direct structural echo of Pattern 1's "generate both halves, then intersect."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1755. Closest Subsequence Sum | Bridge problem — same split/binary-search mechanic as Pattern 1/3, framed as two independently searched sum-halves |
| 2 | LC 127. Word Ladder *(cross-ref: Graph sheet, Pattern 3)* | Bidirectional BFS — meet-in-the-middle's graph-search sibling, search forward and backward simultaneously |
| 3 | Reachability Within k Moves via Forward/Backward State Generation | Generate reachable states from both ends independently, check for overlap — same "split, generate, intersect" shape as the array patterns |

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Subset Sum Feasibility & Closest Sum | 3 | Split, enumerate all subset sums per half, sort one side, binary search the other |
| Counting Pairs Across Two Halves | 3 | Same split/enumerate, combine step is a frequency map or range count instead of a single lookup |
| Maximize / Minimize a Combined Value | 2 | Meet-in-the-middle generates candidates; an existing technique (binary search closeness, bitwise trie) optimizes over them |
| Meet-in-the-Middle on Graphs / Paths | 3 | Same "split, generate, join in the middle" idea applied to a search space instead of an array |
| **Total** | **~11 problems** | |

---

## How to Use This Sheet

**Pattern 1 is mandatory first, and CSES: Meet in the Middle specifically should be the very first problem attempted.** It has no disguise, no extra story — it is the recipe from the Core Mental Model with nothing added. Every later pattern is this same recipe with a different combine step layered on top; if the split/enumerate/sort/binary-search skeleton isn't automatic from this one problem, every later problem will look like a new algorithm instead of a variation.

**Pattern 2 requires comfort with the Prefix Sum + HashMap sheet's counting idioms and the Two Pointers sheet's range-counting variant.** If `prefix[r] - target` lookups and "count everything between two pointers" aren't already reflexive from those sheets, Pattern 2 will feel like new material rather than "the same lookup, applied to a generated list instead of a given array."

**Pattern 3 explicitly borrows two techniques you should already have solid — closest-value binary search from Pattern 1, and the bitwise trie from Bit Manipulation sheet Pattern 5.** Don't attempt the XOR-maximization problem before LC 421's basic greedy trie-walk is completely automatic; meet-in-the-middle only supplies the candidate list, it doesn't change how the trie walk itself works.

**Pattern 4 is the one place in this sheet where the "split" isn't a literal array cut, and that's the actual skill being tested.** Do LC 1755 first specifically because it still *looks* like an array problem while already behaving like a search-space split — it's the bridge that makes the jump to genuine bidirectional graph search (LC 127) feel like a continuation of the same idea rather than an unrelated Graph-sheet technique reappearing out of nowhere.

**A broader note on this sheet's interview relevance, worth being honest about:** Meet-in-the-middle is a real and recurring competitive programming technique, but it shows up far less often in standard FAANG interview loops than Binary Search, DP, or Graph. Treat this sheet the same way as the LCA sheet — good to have internalized the recognition trigger (`n <= 40`, brute force looks like `2^n`) so you're not caught flat-footed if it comes up, but not something to over-invest drilling time in relative to your higher-frequency sheets.

---
