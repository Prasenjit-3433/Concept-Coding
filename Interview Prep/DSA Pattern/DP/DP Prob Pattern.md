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
| LIS | Find longest subsequence with strictly increasing (or custom order) property |
| LCS | Two sequences, find longest matching subsequence |
| String DP | Two strings, character-by-character matching/transformation |
| Interval DP | Solve on range [i,j], combine sub-ranges; "partition this array optimally" |
| Bitmask DP | Small N (≤20), subsets of items, "assign all tasks / visit all nodes" |
| Tree / Graph DP | DP on rooted tree, state propagated from children to parent |
| State Machine DP | Finite set of states with allowed transitions; stock problems |
| Digit DP | Count numbers in [L, R] satisfying a digit-level property |
| Probability DP | Expected value or probability over random events across steps |

---

## PHASE 1 — Foundation

---

### Pattern 1: 1-D DP (Linear)

**Identify:** Single sequence. Each state `dp[i]` depends only on a few previous states. No two-sequence matching, no grid.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 509. Fibonacci Number | Base template: dp[i] = dp[i-1] + dp[i-2] |
| 2 | LC 70. Climbing Stairs | Same recurrence as Fibonacci, different framing |
| 3 | LC 746. Min Cost Climbing Stairs | Add cost; choose min of two transitions |
| 4 | LC 198. House Robber | Skip-or-take with non-adjacency constraint |
| 5 | LC 213. House Robber II | Circular array — run House Robber twice on subarrays |
| 6 | LC 740. Delete and Earn | Reduce to House Robber after frequency bucketing |
| 7 | LC 322. Coin Change | Unbounded-style but introduced here as 1D for intuition |
| 8 | LC 343. Integer Break | dp[i] = max product split; builds multiplicative intuition |
| 9 | LC 646. Maximum Length of Pair Chain | Greedy OR DP — good duality lesson |
| 10 | LC 1262. Greatest Sum Divisible by Three | Remainder-state DP — extends 1D with modular states |
| 11 | LC 2218. Maximum Value of K Coins From Piles | Prefix sum + 1D DP combination |
| 12 | LC 2713. Maximum Strictly Increasing Cells in a Matrix | Sorting + 1D DP on implicit DAG |

---

### Pattern 2: Kadane's / Subarray DP

**Identify:** Contiguous subarray, maximize or minimize some aggregate. Local vs global optimum structure.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 53. Maximum Subarray | Kadane's pure template |
| 2 | LC 152. Maximum Product Subarray | Track both max and min (sign flip) |
| 3 | LC 1186. Maximum Subarray Sum with One Deletion | State: ended normally vs used deletion |
| 4 | LC 873. Length of Longest Fibonacci Subsequence | dp[i][j] = length ending at (arr[i], arr[j]) |
| 5 | LC 1027. Longest Arithmetic Subsequence | dp[i][diff] — Kadane extended with difference state |

---

### Pattern 3: 2D / Grid DP

**Identify:** 2D table, path through a matrix, square/rectangular submatrix, or Pascal's-style construction.

**Note on Cumulative Sum DP:** Problems like Maximal Square and Count Square Submatrices use a DP table that implicitly encodes cumulative structure. This is NOT prefix-sum — it is a genuine DP recurrence. These are placed here, not in a separate category.

| # | Problem | Key Concept |
|---|---|---|
| 1 | Pascal's Triangle (Theory) | 2D DP base: dp[i][j] = dp[i-1][j-1] + dp[i-1][j] |
| 2 | LC 62. Unique Paths | Count paths in grid — standard 2D DP |
| 3 | LC 63. Unique Paths II | Same + obstacle handling |
| 4 | LC 64. Minimum Path Sum | Minimize cost from top-left to bottom-right |
| 5 | LC 120. Triangle | Variable-width grid, bottom-up DP |
| 6 | LC 931. Minimum Falling Path Sum | Generalized triangle on square matrix |
| 7 | LC 1289. Minimum Falling Path Sum II | Falling path avoiding same column — O(n²) with running min optimization |
| 8 | LC 221. Maximal Square | dp[i][j] = side length of largest square ending here |
| 9 | LC 1277. Count Square Submatrices with All Ones | Same recurrence as Maximal Square, count all |
| 10 | LC 85. Maximal Rectangle | Histogram DP extended to 2D — hardest in this tier |
| 11 | LC 1314. Matrix Block Sum | True prefix sum (not DP) — contrast to above |
| 12 | LC 1444. Number of Ways of Cutting a Pizza | 2D DP + prefix sum — combination |
| 13 | LC 1301. Number of Paths with Max Score | 2D DP tracking both max score and count simultaneously |
| 14 | LC 174. Dungeon Game | Reverse 2D DP — must solve bottom-up from destination |
| 15 | LC 741. Cherry Pickup | Two-agent simultaneous traversal — reduce to dp[t][r1][r2] |
| 16 | LC 1463. Cherry Pickup II | Two agents top-down — cleaner formulation of same idea |

---

## PHASE 2 — Classical DP

---

### Pattern 4: 0/1 Knapsack

**Identify:** Each item used **at most once**. Binary pick-or-skip per item. Total capacity/target constraint.

**Core recurrence:** `dp[i][w] = max(dp[i-1][w], dp[i-1][w - wt[i]] + val[i])`

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP D — Knapsack 1 | Pure 0/1 Knapsack template |
| 2 | LC 416. Partition Equal Subset Sum | Knapsack reframed as subset sum to target |
| 3 | LC 494. Target Sum | Count ways to assign +/- signs; reduce to subset sum count |
| 4 | LC 474. Ones and Zeroes | 2D knapsack — capacity in two dimensions (0s and 1s) |
| 5 | LC 1049. Last Stone Weight II | Minimize difference of two groups — subset sum variant |
| 6 | LC 879. Profitable Schemes | 3D knapsack — members × profit ≥ target |
| 7 | LC 638. Shopping Offers | Multi-item bundles — generalized knapsack |

---

### Pattern 5: Unbounded Knapsack

**Identify:** Items can be reused **unlimited times**. Same item picked multiple times to fill target.

**Core recurrence:** `dp[w] = max(dp[w], dp[w - wt[i]] + val[i])` — iterate items in outer loop, weight in inner, no reset.

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP E — Knapsack 2 | Unbounded Knapsack pure template |
| 2 | LC 322. Coin Change | Minimum coins to reach amount — classic unbounded |
| 3 | LC 518. Coin Change II | Count number of ways — order of loops matters |
| 4 | LC 279. Perfect Squares | Coins = perfect squares; minimize count |
| 5 | LC 983. Minimum Cost for Tickets | Day-indexed unbounded with 3 ticket durations |
| 6 | GFG: Rod Cutting | Maximize value; rod piece = item, length = weight |

---

### Pattern 6: Longest Increasing Subsequence (LIS)

**Identify:** Subsequence (not subarray) with ordering constraint. O(n²) DP or O(n log n) patience sort.

**Note on O(n log n):** For FAANG interviews, always know the O(n log n) binary search approach (patience sorting). O(n²) alone is insufficient for large inputs.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 300. Longest Increasing Subsequence | Core template — both O(n²) and O(n log n) |
| 2 | GFG: Print LIS | Reconstruct the actual subsequence via backtracking |
| 3 | LC 673. Number of Longest Increasing Subsequences | Count — maintain both length and count arrays |
| 4 | LC 368. Largest Divisible Subset | LIS with divisibility instead of strict ordering |
| 5 | LC 1048. Longest String Chain | LIS where "predecessor" = remove one character |
| 6 | LC 1027. Longest Arithmetic Subsequence | LIS with fixed difference constraint |
| 7 | LC 354. Russian Doll Envelopes | 2D LIS — sort by width, LIS on height |
| 8 | LC 1691. Maximum Height by Stacking Cuboids | 3D LIS — sort + 3-condition LIS |

---

### Pattern 7: Longest Common Subsequence (LCS)

**Identify:** Two sequences, find longest matching subsequence (characters need not be contiguous). Foundation for edit distance, diff algorithms, string transformations.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1143. Longest Common Subsequence | Core LCS template |
| 2 | GFG: Print All LCS Sequences | Reconstruct all LCS strings via backtracking |
| 3 | LC 516. Longest Palindromic Subsequence | LCS(s, reverse(s)) |
| 4 | LC 72. Edit Distance | LCS + insert/delete/replace costs |
| 5 | LC 1092. Shortest Common Supersequence | LCS then reconstruct merged string |
| 6 | LC 1312. Minimum Insertion Steps to Make String Palindrome | len(s) - LPS(s) |
| 7 | LC 583. Delete Operation for Two Strings | m + n - 2*LCS |

---

### Pattern 8: String DP

**Identify:** One or two strings, character-by-character matching with wildcards, patterns, or segmentation. Extends LCS but adds matching logic.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 139. Word Break | dp[i] = can s[0..i] be segmented — 1D string DP |
| 2 | LC 91. Decode Ways | Count decodings — tricky base cases |
| 3 | LC 115. Distinct Subsequences | Count ways t appears as subsequence in s |
| 4 | LC 97. Interleaving String | dp[i][j] = can s1[0..i] and s2[0..j] interleave to form s3 |
| 5 | LC 44. Wildcard Matching | `?` and `*` matching — careful `*` transition |
| 6 | LC 10. Regular Expression Matching | `.` and `*` — hardest string DP; `*` can zero-out |
| 7 | LC 1143. Minimum Deletions to Make String Balanced | 1D string scan with state |
| 8 | LC 140. Word Break II | Backtrack all valid segmentations using memo |

---

## PHASE 3 — Advanced Patterns

---

### Pattern 9: Interval DP

**Identify:** Problem asks to optimally split/partition a range `[i, j]`. You try all split points `k` and combine sub-problems. Matrix Chain Multiplication is one instance — but burst balloons, palindrome partition, and strange printer are all the same pattern.

**Core recurrence:** `dp[i][j] = min/max over k in [i, j-1] of (dp[i][k] + dp[k+1][j] + cost(i, k, j))`

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Matrix Chain Multiplication | Pure Interval DP template |
| 2 | LC 1039. Minimum Score Triangulation of Polygon | Interval DP on polygon diagonals |
| 3 | LC 312. Burst Balloons | Reverse thinking — last balloon popped in range [i,j] |
| 4 | LC 1000. Minimum Cost to Merge Stones | Interval DP with grouping constraint |
| 5 | LC 1547. Minimum Cost to Cut a Stick | Interval DP with sorted cut points |
| 6 | LC 516. Longest Palindromic Subsequence | Also solvable as Interval DP (expand palindrome) |
| 7 | LC 1278. Palindrome Partitioning III | Partition string into k palindromes — Interval DP + cost table |
| 8 | LC 1043. Partition Array for Maximum Sum | Fixed window partition — simpler interval DP |
| 9 | LC 471. Strange Printer | Print ranges optimally — non-obvious interval DP |

---

### Pattern 10: Bitmask DP

**Identify:** Small N (typically ≤ 20). State = subset of items already processed. "Assign all tasks", "visit all nodes", "cover all requirements".

**Core structure:** `dp[mask]` or `dp[mask][node]` — iterate over all subsets.

| # | Problem                                       | Key Concept |
|---|-----------------------------------------------|---|
| 1 | LC 1986. Minimum Number of Work Sessions      | Bitmask DP — assign all tasks to sessions |
| 2 | LC 1723. Find Minimum Time to Finish All Jobs | Bitmask DP — distribute jobs among k workers |
| 3 | LC 1947. Maximum Compatibility Score Sum      | Bitmask DP — bipartite matching via DP |
| 4 | LC 698. Partition to K Equal Sum Subsets      | Bitmask DP — split into k groups |
| 5 | LC 847. Shortest Path Visiting All Nodes      | BFS + Bitmask — visit all nodes |
| 6 | LC 1349. Maximum Students Taking Exam         | Bitmask DP on rows — seat validity per row |
| 7 | LC 2305. Fair Distribution of Cookies         | Bitmask DP — minimize max load |
| 8 | CodeChef: Little Elephant and T-Shirts        | Classic bitmask assignment problem |

---

### Pattern 11: Tree / Graph DP

**Identify:** DP on a rooted tree. State at each node depends on children states. Rerooting technique for "all roots" variants.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 337. House Robber III | Tree DP — rob or skip at each node, propagate states |
| 2 | LC 968. Binary Tree Cameras | Tree DP — 3 states: covered, camera, not covered |
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

**Identify:** A fixed set of states with defined allowed transitions. You're always in exactly one state. Stock buy/sell problems are the canonical family, but any "mode switching" problem fits.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 121. Best Time to Buy and Sell Stock | Trivial — one transaction, baseline |
| 2 | LC 122. Best Time to Buy and Sell Stock II | Unlimited transactions — greedy OR DP |
| 3 | LC 309. Best Time to Buy and Sell Stock with Cooldown | States: held, sold, rest |
| 4 | LC 714. Best Time to Buy and Sell Stock with Transaction Fee | States: held, not held |
| 5 | LC 123. Best Time to Buy and Sell Stock III | Exactly 2 transactions — 4 states |
| 6 | LC 188. Best Time to Buy and Sell Stock IV | At most k transactions — generalize III |
| 7 | LC 1955. Count Number of Special Subsequences | 3-state machine: 0s only → 0s+1s → 0s+1s+2s |
| 8 | LC 2826. Sorting Three Groups | State machine on sorted group transitions |

---

### Pattern 13: Digit DP

**Identify:** "Count integers in [1, N] (or [L, R]) satisfying some digit-level property." Standard parameters: position, tight constraint, leading zero flag.

**Template states:** `dp[pos][tight][leading_zero][...custom state...]`

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

**Identify:** Expected value or probability questions over random events across discrete steps. State usually encodes current position or remaining resource.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 688. Knight Probability in Chessboard | Probability forward DP — knight stays on board |
| 2 | LC 837. New 21 Game | Probability window DP — sliding window optimization |
| 3 | LC 808. Soup Servings | Expected value + early termination insight |
| 4 | LC 1227. Airplane Seat Assignment Probability | Math insight — no DP needed; good contrast problem |
| 5 | LC 1230. Toss Strange Coins | Probability DP across coins with individual probabilities |

---

## Summary

| Phase | Pattern | Problems |
|---|---|---|
| Phase 1 — Foundation | 1-D DP | 12 |
| | Kadane's / Subarray DP | 5 |
| | 2D / Grid DP | 16 |
| Phase 2 — Classical | 0/1 Knapsack | 7 |
| | Unbounded Knapsack | 6 |
| | LIS | 8 |
| | LCS | 7 |
| | String DP | 8 |
| Phase 3 — Advanced | Interval DP | 9 |
| | Bitmask DP | 8 |
| | Tree / Graph DP | 8 |
| Phase 4 — Specialized | State Machine DP | 8 |
| | Digit DP | 6 |
| | Probability DP | 5 |
| **Total** | **14 Patterns** | **~113 problems** |


