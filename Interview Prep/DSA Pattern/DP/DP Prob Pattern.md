# DP DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| 1-D DP | Single array/sequence, current state depends on previous few states |
| Kadane's / Subarray DP | Maximize/minimize over contiguous subarrays |
| 2D / Grid DP | 2D table, path through matrix, square submatrix counting |
| 0/1 Knapsack | Each item used at most once, pick or skip decision |
| Unbounded Knapsack | Items can be reused unlimited times |
| LIS | Subsequence with ordering/custom constraint; O(n log n) expected |
| LCS | Two sequences, find longest matching subsequence |
| String DP | Character-by-character matching with wildcards or segmentation |
| Interval DP | Solve on range [i,j], combine sub-ranges; "partition optimally" |
| Bitmask DP | N ≤ 20, subsets of items, assign all tasks / visit all nodes |
| Tree / Graph DP | DP on rooted tree, propagate state from children to parent |
| State Machine DP | Finite states with allowed transitions; mode-switching problems |
| Digit DP | Count integers in [1,N] satisfying a digit-level property |
| Probability DP | Expected value or probability over random events across steps |
| Game Theory DP | Two players, alternating turns, both play optimally — who wins? |

---

## PHASE 1 — Foundation

---

### Pattern 1: 1-D DP (Linear)

**Identify:** Single sequence. Each state `dp[i]` depends only on a few previous states. No two-sequence matching, no grid.

**Sliding Window DP — Special Note:**
> Some 1-D DP problems have recurrences where `dp[i]` depends on the **maximum or minimum of a sliding window** of previous states. A naive scan is O(nk). The fix is a **monotonic deque** maintaining the running max/min in O(1) per step, making the whole DP O(n). Trigger: "at most k steps", "jump at most k", "window of size k" inside a DP context. Canonical problems: LC 1696, LC 1425.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 509. Fibonacci Number | Base template: `dp[i] = dp[i-1] + dp[i-2]` |
| 2 | LC 70. Climbing Stairs | Same recurrence as Fibonacci, different framing |
| 3 | LC 746. Min Cost Climbing Stairs | Add cost; choose min of two transitions |
| 4 | LC 198. House Robber | Skip-or-take with non-adjacency constraint |
| 5 | LC 213. House Robber II | Circular array — run House Robber twice on subarrays |
| 6 | LC 740. Delete and Earn | Reduce to House Robber after frequency bucketing |
| 7 | LC 343. Integer Break | `dp[i]` = max product split; builds multiplicative intuition |
| 8 | LC 646. Maximum Length of Pair Chain | Greedy OR DP — important duality lesson |
| 9 | LC 1262. Greatest Sum Divisible by Three | Remainder-state DP — extends 1D with modular states |
| 10 | LC 2218. Maximum Value of K Coins From Piles | Prefix sum + 1D DP combination |
| 11 | LC 2713. Maximum Strictly Increasing Cells in a Matrix | Sort by value + maintain row/col max — 1D DP on implicit DAG |
| 12 | LC 1696. Jump Game VI | **Sliding Window DP** — deque maintains max of previous k states |
| 13 | LC 1425. Constrained Subsequence Sum | **Sliding Window DP** — Kadane's variant with window constraint |

---

### Pattern 2: Kadane's / Subarray DP

**Identify:** Contiguous subarray, maximize or minimize some aggregate. Local vs global optimum structure.

**DP + Data Structure Note:**
> LC 1499 below looks like a sliding window problem but requires a deque for the max of a linear function — a preview of the Convex Hull Trick idea, without needing CHT itself. Good to recognize this transition.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 53. Maximum Subarray | Kadane's pure template |
| 2 | LC 152. Maximum Product Subarray | Track both max and min simultaneously (sign flip) |
| 3 | LC 1186. Maximum Subarray Sum with One Deletion | State: ended normally vs used deletion |
| 4 | LC 1499. Max Value of Equation | Sliding window deque on DP-like linear recurrence |
| 5 | LC 873. Length of Longest Fibonacci Subsequence | `dp[i][j]` = length ending at `(arr[i], arr[j])` |

---

### Pattern 3: 2D / Grid DP

**Identify:** 2D table, path through a matrix, square/rectangular submatrix, or Pascal's-style construction.

**Note on Cumulative Sum DP:**
> Problems like Maximal Square use a DP recurrence that encodes cumulative structure — this is NOT a prefix sum, it is genuine DP. LC 1314 is included as a **contrast problem**: it IS pure prefix sum with no DP, placed here so you learn to distinguish the two.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Pascal's Triangle (Theory) | 2D DP base: `dp[i][j] = dp[i-1][j-1] + dp[i-1][j]` |
| 2 | LC 62. Unique Paths | Count paths in grid — standard 2D DP |
| 3 | LC 63. Unique Paths II | Same + obstacle handling |
| 4 | LC 64. Minimum Path Sum | Minimize cost from top-left to bottom-right |
| 5 | LC 120. Triangle | Variable-width grid, bottom-up DP |
| 6 | LC 931. Minimum Falling Path Sum | Generalized triangle on square matrix |
| 7 | LC 1289. Minimum Falling Path Sum II | Avoid same column — O(n²) with running min optimization |
| 8 | LC 221. Maximal Square | `dp[i][j]` = side length of largest square ending here |
| 9 | LC 1277. Count Square Submatrices with All Ones | Same recurrence as Maximal Square, count all |
| 10 | LC 85. Maximal Rectangle | Histogram DP extended to 2D — hardest in this tier |
| 11 | LC 1314. Matrix Block Sum | **Contrast problem** — pure prefix sum, NOT DP; learn the boundary |
| 12 | LC 1444. Number of Ways of Cutting a Pizza | 2D DP + prefix sum combination |
| 13 | LC 1301. Number of Paths with Max Score | 2D DP tracking both max score and count simultaneously |
| 14 | LC 174. Dungeon Game | Reverse 2D DP — must solve bottom-up from destination |
| 15 | LC 329. Longest Increasing Path in a Matrix | Topo sort on implicit DAG in matrix — 2D DP meets graph |
| 16 | LC 741. Cherry Pickup | Two-agent simultaneous traversal — reduce to `dp[t][r1][r2]` |
| 17 | LC 1463. Cherry Pickup II | Two agents top-down — cleaner formulation of same idea |

---

## PHASE 2 — Classical DP

---

### Pattern 4: 0/1 Knapsack

**Identify:** Each item used **at most once**. Binary pick-or-skip per item. Total capacity/target constraint.

**Core recurrence:** `dp[i][w] = max(dp[i-1][w], dp[i-1][w - wt[i]] + val[i])`

**Critical loop direction:** When space-optimizing to 1D, iterate weight **backward** (from target down to item weight) — this prevents reusing the same item.

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP D — Knapsack 1 | Pure 0/1 Knapsack template |
| 2 | LC 416. Partition Equal Subset Sum | Knapsack reframed as subset sum to target |
| 3 | LC 494. Target Sum | Count ways to assign +/- signs; reduce to subset sum count |
| 4 | LC 474. Ones and Zeroes | 2D knapsack — capacity in two dimensions |
| 5 | LC 1049. Last Stone Weight II | Minimize difference of two groups — subset sum variant |
| 6 | LC 879. Profitable Schemes | 3D knapsack — members × profit ≥ target |
| 7 | LC 638. Shopping Offers | Multi-item bundles — generalized knapsack |

---

### Pattern 5: Unbounded Knapsack

**Identify:** Items can be reused **unlimited times**.

**Core recurrence:** `dp[w] = max(dp[w], dp[w - wt[i]] + val[i])` — iterate items outer, weight inner **forward**.

**The one critical difference from 0/1:**
> In 0/1 Knapsack, iterate weight **backward** to prevent reuse. In Unbounded, iterate weight **forward** to allow reuse. This single direction flip is the entire difference — and the most common silent bug source in interviews. Your solution compiles and runs but produces wrong answers.

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP E — Knapsack 2 | Unbounded Knapsack pure template |
| 2 | LC 322. Coin Change | Minimum coins to reach amount — classic unbounded |
| 3 | LC 518. Coin Change II | Count number of ways — loop order matters critically |
| 4 | LC 279. Perfect Squares | Coins = perfect squares; minimize count |
| 5 | LC 983. Minimum Cost for Tickets | Day-indexed unbounded with 3 ticket durations |
| 6 | GFG: Rod Cutting | Maximize value; piece = item, total length = capacity |

---

### Pattern 6: Longest Increasing Subsequence (LIS)

**Identify:** Subsequence (not subarray) with ordering or custom constraint. Always know both O(n²) and O(n log n).

**O(n log n) is non-negotiable:**
> Maintain a `tails[]` array. For each element, binary search for its correct insertion position. `tails[i]` = smallest tail element of all increasing subsequences of length `i+1`. Length of `tails` at the end = LIS length. If n = 10⁵, O(n²) gets TLE — this approach is required at FAANG.

**Hashmap as DP table — Special Note:**
> LC 1027 below cannot use a 2D array because the second dimension (`diff`) is an arbitrary integer. Instead, use a **hashmap per index**: `dp[i]` is a map from `diff → length`. This is a general technique — whenever your DP state has a variable unbounded second dimension, a hashmap replaces the array. Other problems using the same idea: LC 873 (Fibonacci subsequence), LC 3 (LCS variants).

**DP + Data Structure Note:**
> LC 2407 requires LIS where the previous valid element must satisfy a range condition. Binary search alone is insufficient — you need a **segment tree with range max query** to efficiently find the best previous state.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 300. Longest Increasing Subsequence | Core template — both O(n²) and O(n log n) |
| 2 | GFG: Print LIS | Reconstruct the actual subsequence via backtracking |
| 3 | LC 673. Number of Longest Increasing Subsequences | Count — maintain both length and count arrays |
| 4 | LC 368. Largest Divisible Subset | LIS with divisibility instead of strict ordering |
| 5 | LC 1048. Longest String Chain | LIS where predecessor = remove one character |
| 6 | LC 1027. Longest Arithmetic Subsequence | LIS with fixed difference constraint — hashmap as DP table |
| 7 | LC 354. Russian Doll Envelopes | 2D LIS — sort by width, LIS on height |
| 8 | LC 1691. Maximum Height by Stacking Cuboids | 3D LIS — sort + 3-condition LIS |
| 9 | LC 2407. Longest Increasing Subsequence II | LIS + segment tree range max — DP + DS combo |

---

### Pattern 7: Longest Common Subsequence (LCS)

**Identify:** Two sequences, find longest matching subsequence. Foundation for edit distance, diff tools, string transformations.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1143. Longest Common Subsequence | Core LCS template |
| 2 | GFG: Print All LCS Sequences | Reconstruct all LCS strings via backtracking |
| 3 | LC 516. Longest Palindromic Subsequence | `LCS(s, reverse(s))` |
| 4 | LC 72. Edit Distance | LCS + insert/delete/replace costs |
| 5 | LC 1092. Shortest Common Supersequence | LCS then reconstruct merged string |
| 6 | LC 1312. Minimum Insertion Steps to Make String Palindrome | `len(s) - LPS(s)` |
| 7 | LC 583. Delete Operation for Two Strings | `m + n - 2*LCS` |

---

### Pattern 8: String DP

**Identify:** One or two strings, character-by-character matching with wildcards, patterns, or segmentation. Extends LCS but adds matching logic.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 139. Word Break | `dp[i]` = can `s[0..i]` be segmented — 1D string DP |
| 2 | LC 91. Decode Ways | Count decodings — tricky base cases |
| 3 | LC 115. Distinct Subsequences | Count ways `t` appears as subsequence in `s` |
| 4 | LC 97. Interleaving String | `dp[i][j]` = can `s1[0..i]` and `s2[0..j]` interleave to form `s3` |
| 5 | LC 44. Wildcard Matching | `?` and `*` matching — careful `*` transition |
| 6 | LC 10. Regular Expression Matching | `.` and `*` — hardest string DP; `*` can zero-out previous char |
| 7 | LC 1143. Minimum Deletions to Make String Balanced | 1D string scan with running state |
| 8 | LC 140. Word Break II | Backtrack all valid segmentations using memo |

---

## PHASE 3 — Advanced Patterns

---

### Pattern 9: Interval DP

**Identify:** Optimize over a range `[i, j]`. Try all split points `k` and combine sub-problems. MCM is one instance — burst balloons, palindrome partitioning, and strange printer all share the same recurrence shape.

**Core recurrence:** `dp[i][j] = min/max over k in [i, j-1] of (dp[i][k] + dp[k+1][j] + cost(i, k, j))`

**Most common bug — iteration order:**
> Always iterate by **interval length** in the outer loop, not by `i`. Fill all intervals of length 2 before length 3, and so on. Iterating by `i` directly causes you to reference sub-problems that haven't been computed yet — wrong answers with no obvious error.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Matrix Chain Multiplication | Pure Interval DP template |
| 2 | LC 1039. Minimum Score Triangulation of Polygon | Interval DP on polygon diagonals |
| 3 | LC 312. Burst Balloons | Reverse thinking — last balloon popped in `[i,j]` |
| 4 | LC 1000. Minimum Cost to Merge Stones | Interval DP with grouping constraint |
| 5 | LC 1547. Minimum Cost to Cut a Stick | Interval DP with sorted cut points |
| 6 | LC 1278. Palindrome Partitioning III | Partition string into k palindromes — Interval DP + cost table |
| 7 | LC 1043. Partition Array for Maximum Sum | Fixed window partition — simpler interval DP |
| 8 | LC 471. Strange Printer | Print ranges optimally — non-obvious interval DP |

---

### Pattern 10: Bitmask DP

**Identify:** Small N (typically ≤ 20). State = subset of items processed. "Assign all tasks", "visit all nodes", "cover all requirements".

**Core structure:** `dp[mask]` or `dp[mask][node]` — iterate over all 2^N subsets.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1986. Minimum Number of Work Sessions | Bitmask DP — assign all tasks to sessions |
| 2 | LC 1723. Find Minimum Time to Finish All Jobs | Bitmask DP — distribute jobs among k workers |
| 3 | LC 1947. Maximum Compatibility Score Sum | Bitmask DP — bipartite matching via DP |
| 4 | LC 698. Partition to K Equal Sum Subsets | Bitmask DP — split into k equal groups |
| 5 | LC 847. Shortest Path Visiting All Nodes | BFS + Bitmask — visit all nodes shortest path |
| 6 | LC 1349. Maximum Students Taking Exam | Bitmask DP on rows — seat validity per row |
| 7 | LC 2305. Fair Distribution of Cookies | Bitmask DP — minimize max load across workers |
| 8 | CodeChef TSHIRTS | Classic bitmask assignment problem |

---

### Pattern 11: Tree / Graph DP

**Identify:** DP on a rooted tree. State at each node depends on children. Rerooting technique for "all roots" variants.

**Rerooting Note:**
> Standard tree DP roots once and answers for that root. Rerooting computes answers for **all possible roots in O(n)** using two DFS passes — down-pass builds child states, up-pass propagates parent contribution back down. Triggers: "sum of distances", "answer for every node as root".

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 337. House Robber III | Tree DP — rob or skip at each node, propagate pair of states |
| 2 | LC 968. Binary Tree Cameras | Tree DP — 3 states: covered, camera placed, not covered |
| 3 | LC 2646. Minimize the Total Price of the Trips | Tree DP with path frequency counting |
| 4 | LC 834. Sum of Distances in Tree | Rerooting DP — compute answer for all roots in O(n) |
| 5 | LC 2538. Difference Between Maximum and Minimum Price Sum | Rerooting DP — max path from every node |
| 6 | LC 2867. Count Valid Paths in a Tree | Tree DP with prime sieve |
| 7 | LC 3067. Count Pairs of Connectable Servers in a Weighted Tree | Tree DP — count valid pairs per edge |
| 8 | LC 2581. Count Number of Possible Root Nodes | Rerooting DP |

---

## PHASE 4 — Specialized Patterns

---

### Pattern 12: State Machine DP

**Identify:** A fixed, small set of states with defined transitions. You are always in exactly one state. Stock problems are the canonical family — any "mode switching" problem fits.

**How to approach:**
> Draw the state diagram first — nodes are states, edges are allowed transitions with costs. Once the diagram is clear, the DP writes itself. Never code state machine DP before drawing the diagram.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 121. Best Time to Buy and Sell Stock | Trivial — one transaction, baseline |
| 2 | LC 122. Best Time to Buy and Sell Stock II | Unlimited transactions — greedy OR DP |
| 3 | LC 309. Best Time to Buy and Sell Stock with Cooldown | States: held, sold, rest |
| 4 | LC 714. Best Time to Buy and Sell Stock with Transaction Fee | States: held, not held |
| 5 | LC 123. Best Time to Buy and Sell Stock III | Exactly 2 transactions — 4 states |
| 6 | LC 188. Best Time to Buy and Sell Stock IV | At most k transactions — generalize III |
| 7 | LC 1955. Count Number of Special Subsequences | 3-state machine: 0s only → 0s+1s → all three |
| 8 | LC 2826. Sorting Three Groups | State machine on sorted group transitions |

---

### Pattern 13: Digit DP

**Identify:** "Count integers in [1, N] (or [L, R]) satisfying some digit-level property."

**Standard state template:** `dp[pos][tight][leading_zero][...custom state...]`

**The tight constraint explained:**
> `tight = true` means all digits placed so far exactly match the prefix of N — your next digit is bounded by the corresponding digit of N. When `tight = false`, you're free to place 0–9. This flag is the entire mechanism of digit DP. Everything else is just the custom state for whatever property you're counting.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 357. Count Numbers with Unique Digits | Digit DP intro — count without digit repeat |
| 2 | LC 233. Number of Digit One | Count 1s across all numbers — pure digit math |
| 3 | LC 788. Rotated Digits | Count valid rotatable numbers up to N |
| 4 | LC 902. Numbers At Most N Given Digit Set | Custom digit set — tight constraint core |
| 5 | LC 2376. Count Special Integers | No repeating digits up to N — full digit DP template |
| 6 | LC 1397. Find All Good Strings | Digit DP + KMP automaton — hardest combo |

---

### Pattern 14: Probability DP

**Identify:** Expected value or probability questions over random events across discrete steps. State encodes current position or remaining resource.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 688. Knight Probability in Chessboard | Probability forward DP — knight stays on board |
| 2 | LC 837. New 21 Game | Probability window DP — sliding window optimization |
| 3 | LC 808. Soup Servings | Expected value + early termination insight |
| 4 | LC 1230. Toss Strange Coins | Probability DP across coins with individual probabilities |
| 5 | LC 1227. Airplane Seat Assignment Probability | Math insight, no DP needed — contrast problem, teaches when NOT to use DP |

---

### Pattern 15: Game Theory DP

**Identify:** Two players alternate turns, both play optimally. "Who wins?" or "what is the optimal score?" Problems framed as "Alice and Bob", "first player wins/loses", "minimax".

**Core insight — Minimax:**
> At each state, the current player maximizes their own outcome knowing the opponent will minimize it. `dp[state]` = best outcome the current player can guarantee. If `dp[state] > 0` → current player wins; `< 0` → loses; `== 0` → draw.

**Level 1: Minimax DP** — required for FAANG.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 292. Nim Game | Base case — pure math insight, no DP; teaches "optimal play" mindset |
| 2 | LC 877. Stone Game | Minimax DP — or math trick; know both, interviewers test reasoning |
| 3 | LC 486. Predict the Winner | Classic minimax interval DP — pick from either end |
| 4 | LC 1406. Stone Game III | Minimax DP on array suffix — pick 1, 2, or 3 |
| 5 | LC 1140. Stone Game II | Minimax DP with expanding move range M |
| 6 | LC 464. Can I Win | Minimax + Bitmask — when simple math doesn't work |

**Level 2: Sprague-Grundy Theory** — know the concept, implement only if targeting Google.

> Every impartial two-player game reduces to a Nim heap. The Grundy value of a position is the MEX (minimum excludant — smallest non-negative integer not in the set) of Grundy values of all reachable next positions.
>
> `Grundy(pos) = MEX({ Grundy(next) for all reachable next positions })`
>
> For composite games (multiple independent sub-games), XOR all Grundy values — if XOR ≠ 0, first player wins; if XOR = 0, second player wins.
>
> **Interview reality:** Pure Sprague-Grundy is rare at standard FAANG but has appeared at Google. For other EU FAANG targets, understanding the concept is enough — you will not need to implement it from scratch under pressure.

---

## PHASE 5 — Patterns to Know About (No Deep-Dive Needed)

---

> **Special Note — DP Optimizations (D&C DP, CHT, Knuth's):**
>
> These are **speed optimizations** applied after you've already identified the correct DP recurrence. They are not pattern identifiers — they don't help you recognize what type of problem it is.
>
> - **Divide & Conquer DP:** When the optimal split point in an interval DP is monotone, reduces O(n²) to O(n log n). Competitive programming territory — essentially never at FAANG.
> - **Convex Hull Trick (CHT):** Optimizes transitions of the form `dp[i] = min(dp[j] + b[j]*a[i])` from O(n²) to O(n). Competitive programming only.
> - **Knuth's Optimization:** Special case of D&C DP for interval DP when cost satisfies the quadrangle inequality. O(n³) → O(n²). Competitive programming only.
>
> **Verdict for EU FAANG:** Do not invest practice time here. An O(n²) correct solution will be accepted or receive full credit in almost every FAANG interview where one of these optimizations theoretically applies.

---

> **Special Note — DP Traps (Problems That Look Like DP but Aren't):**
>
> Recognizing when **not** to use DP is itself tested at FAANG. The tell: if there is a locally optimal choice that is also globally optimal — no future decision can ever invalidate a current choice — it's greedy. DP is needed when a locally optimal choice genuinely depends on future states.
>
> | Problem | Lesson |
> |---|---|
> | LC 55. Jump Game | Looks like DP, is greedy — can you reach the end? |
> | LC 45. Jump Game II | Minimum jumps — greedy BFS, not DP |
> | LC 435. Non-overlapping Intervals | Maximize kept intervals — greedy by end time |
> | LC 11. Container With Most Water | Two pointers, not DP |

---

## Final Summary

| Phase | Pattern | Problems |
|---|---|---|
| Phase 1 — Foundation | 1-D DP (+ Sliding Window DP) | 13 |
| | Kadane's / Subarray DP | 5 |
| | 2D / Grid DP | 17 |
| Phase 2 — Classical | 0/1 Knapsack | 7 |
| | Unbounded Knapsack | 6 |
| | LIS (+ Hashmap DP + DS Combo) | 9 |
| | LCS | 7 |
| | String DP | 8 |
| Phase 3 — Advanced | Interval DP | 8 |
| | Bitmask DP | 8 |
| | Tree / Graph DP | 8 |
| Phase 4 — Specialized | State Machine DP | 8 |
| | Digit DP | 6 |
| | Probability DP | 5 |
| | Game Theory DP | 6 |
| Phase 5 — Know About | DP Optimizations | reference only |
| | DP Traps | 4 contrast problems |
| **Total** | **15 active patterns** | **~120 problems** |

---

## How to Use This Sheet

**Order strictly matters.** Do not jump to Phase 3 without Phase 2 being solid. Interval DP without LCS/Knapsack fluency is just memorization — you'll solve the exact problem you practiced but fail any variant.

**Two-pass each pattern.** First pass: solve each problem with hints/editorial if stuck, understand the recurrence fully. Second pass 1–2 weeks later: solve cold, no hints.

**After completing all 15 patterns:** Spend 2–3 weeks exclusively on LC Weekly Contest hard problems and problems you cannot immediately pattern-match. That final step — training on the unfamiliar — is what closes the remaining gap and eliminates interview surprise.