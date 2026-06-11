# DP DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## How to Identify the Pattern in an Unseen Problem

This is the most important skill. Before thinking about recurrence, ask these questions in order:

**Step 1 — What is the input structure?**
- Single array or sequence → start with 1D DP or Kadane's
- Two sequences → LCS family
- A matrix or grid → DP on Grid
- A tree → DP on Tree
- A directed graph with no cycles → DP on DAG
- A small set of N items where you need all subsets → DP on Bitmask
- A string with matching or segmentation → String DP

**Step 2 — What are you optimizing or counting?**
- Max/min over contiguous subarray → Kadane's
- Count or optimize over subsequence with ordering constraint → LIS family
- Count ways to reach a target weight/sum, each item used once → 0/1 Knapsack
- Same but items reusable → Unbounded Knapsack
- Optimize over a range [i, j] by trying all split points → Interval DP
- Count integers satisfying a digit-level property → Digit DP
- Expected value or probability over steps → Probability DP
- Two players, both optimal → Game DP

**Step 3 — What are the states switching between?**
- A small fixed set of modes (held/not held, buy/sell/rest) → State Machine DP

**The one universal rule:**
> If a problem asks "how many ways" or "what is the best", and brute force would explore overlapping subproblems — that's DP. The data structure of the input tells you *which* DP pattern.

---

## Pattern Identification Quick Reference

| Pattern | The clearest trigger |
|---|---|
| 1D DP | Single sequence, current answer depends on a few previous answers |
| Kadane's | Contiguous subarray, maximize or minimize some aggregate |
| DP on Grid | 2D matrix, paths, square submatrices |
| 0/1 Knapsack | Each item used at most once, pick or skip, hit a target |
| Unbounded Knapsack | Items reusable unlimited times |
| LIS | Subsequence with ordering or custom constraint |
| LCS | Two sequences, longest matching subsequence |
| String DP | One or two strings, character matching with wildcards or segmentation |
| Interval DP | Optimize over range [i,j], try all split points |
| DP on Bitmask | N ≤ 20, state = which subset of items processed |
| DP on Tree | Input is a tree, state at each node depends on its children |
| DP on DAG | Directed graph, no cycles, dependencies flow one way |
| State Machine DP | Fixed small set of modes with defined transitions |
| Digit DP | Count integers in [1,N] satisfying a digit-level property |
| Probability DP | Expected value or probability across discrete random steps |
| Game DP | Two players, alternating turns, both play optimally |

---

## PHASE 1 — Foundation

---

### Pattern 1: 1D DP (Linear)

**How to identify:**
> The input is a single sequence — array, string, or number line. You are asked for the best or count of something, and the answer at position `i` only depends on a few positions before it. No two sequences, no grid, no tree.
>
> The simplest signal: "given an array, at each step you can do X or Y — what is the best outcome?"

**Sliding Window DP — when it appears inside 1D DP:**
> Sometimes `dp[i]` depends on the best of a *window* of previous states, not just `dp[i-1]` or `dp[i-2]`. Naively scanning that window is O(nk). The fix is a monotonic deque that maintains the running max/min in O(1) per step. Trigger: "at most k steps", "jump at most k", "window of size k" inside a DP recurrence. Canonical problems: LC 1696, LC 1425.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 509. Fibonacci Number | Base template: `dp[i] = dp[i-1] + dp[i-2]` |
| 2 | LC 70. Climbing Stairs | Same recurrence as Fibonacci, different framing |
| 3 | LC 746. Min Cost Climbing Stairs | Add cost, choose min of two transitions |
| 4 | LC 198. House Robber | Skip-or-take with non-adjacency constraint |
| 5 | LC 213. House Robber II | Circular array — run House Robber twice on subarrays |
| 6 | LC 740. Delete and Earn | Reduce to House Robber after frequency bucketing |
| 7 | LC 343. Integer Break | `dp[i]` = max product split — builds multiplicative intuition |
| 8 | LC 646. Maximum Length of Pair Chain | Greedy OR DP — important duality lesson |
| 9 | LC 1262. Greatest Sum Divisible by Three | Remainder-state DP — extends 1D with modular states |
| 10 | LC 2713. Maximum Strictly Increasing Cells in a Matrix | Sort by value + maintain row/col max — 1D DP on implicit DAG |
| 11 | LC 1696. Jump Game VI | Sliding Window DP — deque maintains max of previous k states |
| 12 | LC 1425. Constrained Subsequence Sum | Sliding Window DP — Kadane's variant with window constraint |

---

### Pattern 2: Kadane's / Subarray DP

**How to identify:**
> The word "subarray" or "contiguous" is usually present, or heavily implied. You are maximizing or minimizing something over every possible contiguous chunk of an array. The key structure: at each position, you decide whether to extend the previous subarray or start fresh.
>
> The clearest signal: "maximum sum subarray", "maximum product subarray". If the problem says "subsequence" instead of "subarray", it is probably LIS, not Kadane's.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 53. Maximum Subarray | Kadane's pure template |
| 2 | LC 152. Maximum Product Subarray | Track both max and min simultaneously — sign flip changes max to min |
| 3 | LC 1186. Maximum Subarray Sum with One Deletion | Two states: ended normally vs used the deletion |
| 4 | LC 873. Length of Longest Fibonacci Subsequence | `dp[i][j]` = length ending at pair `(arr[i], arr[j])` |
| 5 | LC 1499. Max Value of Equation | Sliding window deque on a DP-like linear recurrence |

---

### Pattern 3: DP on Grid

**How to identify:**
> The input is a 2D matrix. You are counting paths, minimizing path cost, or checking properties of submatrices (squares, rectangles). Movement is usually restricted (right/down only, or all four directions with memoization).
>
> The clearest signal: anything involving "paths from top-left to bottom-right", "minimum cost path", "largest square of 1s", "count all paths".

**Note on Cumulative Sum vs DP:**
> Problems like Maximal Square use a recurrence that encodes cumulative structure — this is genuine DP, not prefix sum. LC 1314 is included as a contrast problem: it is pure prefix sum with no DP. Learning to tell them apart is itself a tested skill.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 62. Unique Paths | Count paths in grid — standard 2D DP |
| 2 | LC 63. Unique Paths II | Same + obstacle handling |
| 3 | LC 64. Minimum Path Sum | Minimize cost from top-left to bottom-right |
| 4 | LC 120. Triangle | Variable-width grid, bottom-up DP |
| 5 | LC 931. Minimum Falling Path Sum | Generalized triangle on square matrix |
| 6 | LC 1289. Minimum Falling Path Sum II | Avoid same column — O(n²) with running min optimization |
| 7 | LC 221. Maximal Square | `dp[i][j]` = side length of largest square ending here |
| 8 | LC 1277. Count Square Submatrices with All Ones | Same recurrence as Maximal Square, count all |
| 9 | LC 85. Maximal Rectangle | Histogram DP extended to 2D — hardest in this tier |
| 10 | LC 1314. Matrix Block Sum | Contrast problem — pure prefix sum, NOT DP |
| 11 | LC 174. Dungeon Game | Reverse DP — solve bottom-up from destination |
| 12 | LC 1444. Number of Ways of Cutting a Pizza | 2D DP + prefix sum combined |
| 13 | LC 1301. Number of Paths with Max Score | 2D DP tracking both max score and count simultaneously |
| 14 | LC 741. Cherry Pickup | Two-agent simultaneous traversal — reduce to `dp[t][r1][r2]` |
| 15 | LC 1463. Cherry Pickup II | Two agents top-down — cleaner formulation of same idea |

---

## PHASE 2 — Classical DP

---

### Pattern 4: 0/1 Knapsack

**How to identify:**
> You have a list of items. Each item can be used **at most once** — you either take it or skip it. There is a capacity or target constraint. You want to maximize value, or check if a target sum is reachable, or count the number of ways.
>
> The clearest signal: "given these items, can you reach exactly target weight/sum?" or "maximize value given weight limit, each item used once."
>
> Grouped Knapsack variant: items come in groups, you pick some from each group. LC 2218 is this — "take 0 to x coins from pile i, combine with previous piles."

**Critical loop direction:**
> When space-optimizing to 1D, iterate the weight/target **backward** (from target down to item weight). This prevents using the same item twice. Iterating forward = accidentally treating it as unbounded knapsack. This silent bug produces wrong answers with no obvious error.

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP D — Knapsack 1 | Pure 0/1 Knapsack template |
| 2 | LC 416. Partition Equal Subset Sum | Knapsack reframed as "can you reach exactly target?" |
| 3 | LC 494. Target Sum | Count ways to assign +/- signs — reduces to subset sum count |
| 4 | LC 474. Ones and Zeroes | 2D knapsack — capacity in two dimensions simultaneously |
| 5 | LC 1049. Last Stone Weight II | Minimize difference of two groups — subset sum variant |
| 6 | LC 879. Profitable Schemes | 3D knapsack — members and profit both constrained |
| 7 | LC 638. Shopping Offers | Multi-item bundles — generalized knapsack |
| 8 | LC 2218. Maximum Value of K Coins From Piles | Grouped knapsack — take 0..x coins from each pile, k total |

---

### Pattern 5: Unbounded Knapsack

**How to identify:**
> Same as 0/1 Knapsack — items, capacity, target — but each item can be used **any number of times**. The problem often involves coins, denominations, or pieces that can repeat.
>
> The clearest signal: "you can use the same coin/item multiple times."

**The one critical difference from 0/1:**
> Iterate weight **forward** (from item weight up to target) to allow reuse of the same item. This single direction flip — backward for 0/1, forward for unbounded — is the most common silent bug in interviews. The code looks almost identical but produces wrong answers.

| # | Problem | Key Concept |
|---|---|---|
| 1 | AtCoder DP E — Knapsack 2 | Unbounded Knapsack pure template |
| 2 | LC 322. Coin Change | Minimum coins to reach amount |
| 3 | LC 518. Coin Change II | Count number of ways — loop order is critical |
| 4 | LC 279. Perfect Squares | Coins = perfect squares, minimize count |
| 5 | LC 983. Minimum Cost for Tickets | Day-indexed unbounded with 3 ticket durations |
| 6 | GFG: Rod Cutting | Maximize value — piece length = item, total length = capacity |

---

### Pattern 6: LIS (Longest Increasing Subsequence)

**How to identify:**
> You are looking for a subsequence (not subarray — elements can be skipped) where elements satisfy some ordering or custom constraint. The problem asks for the length, count, or reconstruction of this subsequence.
>
> The clearest signal: "longest subsequence where each element is strictly greater than the previous." Custom variants replace "greater than" with divisibility, string chain, or envelope containment — same pattern.

**O(n log n) is non-negotiable at FAANG:**
> Maintain a `tails[]` array. For each element, binary search for its insertion position. `tails[i]` = smallest tail of all increasing subsequences of length `i+1`. Length of `tails` = LIS length. If n = 10⁵, O(n²) gets TLE — you must know this approach.

**Hashmap as DP table:**
> LC 1027 cannot use a 2D array because the second dimension (arithmetic difference) is an arbitrary integer. Use a hashmap per index instead: `dp[i]` is a map from `diff → length`. General rule: whenever your DP state has a variable unbounded second dimension, replace the array with a hashmap.

**DP + Data Structure combo:**
> LC 2407 requires LIS where the previous valid element must satisfy a range condition. Binary search alone is not enough — you need a segment tree with range max query to find the best previous state efficiently.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 300. Longest Increasing Subsequence | Core template — know both O(n²) and O(n log n) |
| 2 | GFG: Print LIS | Reconstruct the actual subsequence via backtracking |
| 3 | LC 673. Number of Longest Increasing Subsequences | Maintain both length and count arrays simultaneously |
| 4 | LC 368. Largest Divisible Subset | LIS with divisibility instead of strict ordering |
| 5 | LC 1048. Longest String Chain | LIS where predecessor = remove one character |
| 6 | LC 1027. Longest Arithmetic Subsequence | LIS with fixed difference — hashmap as DP table |
| 7 | LC 354. Russian Doll Envelopes | 2D LIS — sort by width, apply LIS on height |
| 8 | LC 1691. Maximum Height by Stacking Cuboids | 3D LIS — sort + three-condition LIS |
| 9 | LC 2407. Longest Increasing Subsequence II | LIS + segment tree range max — DP + DS combo |

---

### Pattern 7: LCS (Longest Common Subsequence)

**How to identify:**
> Two sequences are given. You want the longest subsequence that appears in both, or you want to transform one string into another with minimum operations.
>
> The clearest signal: "two strings/arrays given", "how similar are they", "minimum deletions/insertions to make equal", "edit distance."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1143. Longest Common Subsequence | Core LCS template |
| 2 | GFG: Print All LCS Sequences | Reconstruct all LCS strings via backtracking |
| 3 | LC 516. Longest Palindromic Subsequence | `LCS(s, reverse(s))` |
| 4 | LC 72. Edit Distance | LCS extended with insert/delete/replace costs |
| 5 | LC 1092. Shortest Common Supersequence | LCS then reconstruct the merged string |
| 6 | LC 1312. Minimum Insertion Steps to Make String Palindrome | `len(s) - LPS(s)` |
| 7 | LC 583. Delete Operation for Two Strings | `m + n - 2 * LCS` |

---

### Pattern 8: String DP

**How to identify:**
> One or two strings, character-by-character processing, with matching rules more complex than plain equality — wildcards, regex, or segmentation into dictionary words.
>
> The clearest signal: "does string s match pattern p", "can s be split into dictionary words", "how many ways to decode this string."
>
> Distinction from LCS: LCS matches two sequences against each other with no special rules. String DP adds matching logic — `*` can match zero or more characters, `.` matches any character, a word boundary must align.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 139. Word Break | `dp[i]` = can `s[0..i]` be segmented — 1D string DP |
| 2 | LC 91. Decode Ways | Count decodings — tricky base cases with leading zeros |
| 3 | LC 115. Distinct Subsequences | Count ways `t` appears as subsequence in `s` |
| 4 | LC 97. Interleaving String | `dp[i][j]` = can `s1[0..i]` and `s2[0..j]` interleave to form `s3` |
| 5 | LC 44. Wildcard Matching | `?` and `*` matching — careful `*` transition |
| 6 | LC 10. Regular Expression Matching | `.` and `*` — hardest string DP; `*` can zero-out the previous char |
| 7 | LC 1653. Minimum Deletions to Make String Balanced | 1D string scan with running state |
| 8 | LC 140. Word Break II | Backtrack all valid segmentations using memoization |

---

## PHASE 3 — Advanced Patterns

---

### Pattern 9: Interval DP

**How to identify:**
> You are given a sequence, and you need to optimally partition or merge it. You try every possible split point within a range and combine the results of the two sub-ranges.
>
> The clearest signal: "partition this array/string optimally", "merge these elements with minimum cost", "burst balloons", "minimum score triangulation."
>
> The key mental image: you have a range [i, j]. You try every k between i and j as the "last operation" or "split point", and combine `dp[i][k]` and `dp[k+1][j]`.

**Most common bug — iteration order:**
> Always iterate by **interval length** in the outer loop, not by `i`. Fill all length-2 intervals before length-3. If you iterate by `i` directly, you reference sub-problems not yet computed — wrong answers with no obvious error.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Matrix Chain Multiplication | Pure Interval DP template |
| 2 | LC 1039. Minimum Score Triangulation of Polygon | Interval DP on polygon diagonals |
| 3 | LC 312. Burst Balloons | Reverse thinking — which balloon is popped *last* in [i,j] |
| 4 | LC 1000. Minimum Cost to Merge Stones | Interval DP with grouping constraint |
| 5 | LC 1547. Minimum Cost to Cut a Stick | Interval DP with sorted cut points |
| 6 | LC 1278. Palindrome Partitioning III | Partition string into k palindromes — Interval DP + cost table |
| 7 | LC 1043. Partition Array for Maximum Sum | Fixed window partition — simpler interval DP |
| 8 | LC 471. Strange Printer | Print ranges optimally — non-obvious interval DP |

---

### Pattern 10: DP on Bitmask

**How to identify:**
> N is small — usually ≤ 20. You need to track which subset of items, tasks, or nodes have been processed. The state space is all 2^N possible subsets.
>
> The clearest signal: "assign all tasks", "visit all nodes", "every person must be matched", "cover all requirements" with a small N. The fact that N ≤ 20 is the giveaway — it tells you the intended solution is exponential in N.

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

### Pattern 11: DP on Tree

**How to identify:**
> The input is explicitly a tree — rooted, with parent-child relationships and no cycles. The answer at each node depends on combining answers from its children.
>
> The clearest signal: "given a tree, maximize/minimize/count something at each node using information from its subtree." You root the tree, run DFS postorder, and children feed results upward to the parent.
>
> Rerooting variant: when the problem asks for the answer at *every* node as if it were the root. Two DFS passes — down-pass builds child states, up-pass pushes parent contribution back down. Trigger: "sum of distances to all nodes", "answer for every possible root."

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 337. House Robber III | Classic 2-state tree DP — rob or skip, propagate pair of states |
| 2 | LC 968. Binary Tree Cameras | 3-state tree DP — covered / has camera / not covered |
| 3 | LC 2646. Minimize the Total Price of the Trips | Tree DP with path frequency counting |
| 4 | LC 834. Sum of Distances in Tree | Rerooting DP — compute answer for all roots in O(n) |
| 5 | LC 2538. Difference Between Maximum and Minimum Price Sum | Rerooting DP — max path from every node |
| 6 | LC 2867. Count Valid Paths in a Tree | Tree DP with prime sieve |
| 7 | LC 3067. Count Pairs of Connectable Servers in a Weighted Tree | Tree DP — count valid pairs per edge |
| 8 | LC 2581. Count Number of Possible Root Nodes | Rerooting DP |

---

### Pattern 12: DP on DAG

**How to identify:**
> The input is a directed graph with no cycles — or can be *treated* as one. Because there are no cycles, a valid ordering of nodes exists where all dependencies come before dependents. DP flows along that ordering.
>
> The clearest signal: "longest/shortest/count paths in a directed graph", "dependencies must be satisfied before proceeding", "matrix where you can only move to strictly larger values." If you find yourself thinking "I need topological order first", that is DP on DAG.
>
> Key distinction from Tree DP: a tree has one parent per node and a natural root. A DAG can have multiple parents per node and no single root — you need topological sort to establish the processing order, not just DFS postorder.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 329. Longest Increasing Path in a Matrix | Topological sort on implicit DAG in matrix |
| 2 | LC 2050. Parallel Courses III | Topological sort + DP for critical path length |
| 3 | LC 1857. Largest Color Value in a Directed Graph | Topological sort + frequency DP per color |
| 4 | LC 2328. Number of Increasing Paths in a Grid | Count paths — same implicit DAG as LC 329 |
| 5 | LC 1топологических. Longest Path in a DAG | Pure DAG DP template — topo sort then relax edges |

---

## PHASE 4 — Specialized Patterns

---

### Pattern 13: State Machine DP

**How to identify:**
> There is a small, fixed set of modes or states you can be in at any point. You transition between these states at each step, and each transition has a cost or value. The question is what the best total value is after all steps.
>
> The clearest signal: stock buy/sell problems, "cooldown period", "transaction fee", "at most k transactions." More generally: any problem where you are always in one of a few named states and the rules of switching are fixed.
>
> How to approach: **draw the state diagram first** — circles are states, arrows are allowed transitions with their costs. Once the diagram is clear, the DP writes itself. Never code state machine DP before drawing the diagram.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 121. Best Time to Buy and Sell Stock | One transaction — baseline |
| 2 | LC 122. Best Time to Buy and Sell Stock II | Unlimited transactions — greedy or DP |
| 3 | LC 309. Best Time to Buy and Sell Stock with Cooldown | States: held / sold / rest |
| 4 | LC 714. Best Time to Buy and Sell Stock with Transaction Fee | States: held / not held |
| 5 | LC 123. Best Time to Buy and Sell Stock III | Exactly 2 transactions — 4 states |
| 6 | LC 188. Best Time to Buy and Sell Stock IV | At most k transactions — generalize III |
| 7 | LC 1955. Count Number of Special Subsequences | 3-state machine: 0s only → 0s+1s → all three |
| 8 | LC 2826. Sorting Three Groups | State machine on sorted group transitions |

---

### Pattern 14: Digit DP

**How to identify:**
> You are asked to count how many integers in a range [1, N] (or [L, R]) satisfy some property about their digits — no repeating digits, digit sum equals X, digits are non-decreasing, etc.
>
> The clearest signal: "count numbers up to N where [some digit property holds]." The range N can be huge (up to 10^18), so you cannot iterate — you must count by building the number digit by digit.

**The tight constraint — the core mechanism:**
> `tight = true` means every digit you have placed so far exactly matches the corresponding digit of N. Your next digit is bounded by N's next digit. When `tight = false`, you are free to place 0–9. This one flag is the entire engine of digit DP. Everything else is just the custom state for the property you are counting.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 357. Count Numbers with Unique Digits | Digit DP intro — count without repeating digits |
| 2 | LC 233. Number of Digit One | Count 1s across all numbers — pure digit math |
| 3 | LC 788. Rotated Digits | Count valid rotatable numbers up to N |
| 4 | LC 902. Numbers At Most N Given Digit Set | Custom digit set — tight constraint core |
| 5 | LC 2376. Count Special Integers | No repeating digits up to N — full digit DP template |
| 6 | LC 1397. Find All Good Strings | Digit DP + KMP automaton — hardest combo |

---

### Pattern 15: Probability DP

**How to identify:**
> The problem involves randomness — a dice roll, a random move, a random process — and asks for the probability of an outcome or the expected value after some number of steps.
>
> The clearest signal: "what is the probability that...", "what is the expected number of...", combined with discrete steps and a state that changes at each step.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 688. Knight Probability in Chessboard | Forward probability DP — knight stays on board |
| 2 | LC 837. New 21 Game | Probability window DP — sliding window optimization |
| 3 | LC 808. Soup Servings | Expected value + early termination insight |
| 4 | LC 1230. Toss Strange Coins | Probability DP across coins with individual probabilities |
| 5 | LC 1227. Airplane Seat Assignment Probability | Math insight, no DP needed — contrast problem, teaches when NOT to use DP |

---

### Pattern 16: Game DP

**How to identify:**
> Two players take turns. Both play perfectly — neither makes a suboptimal move. The question is who wins, or what is the optimal score for the first player.
>
> The clearest signal: "Alice and Bob", "first player wins or loses", "both play optimally", "pick from either end."
>
> Core idea — Minimax: at each state, the current player picks the move that maximizes their own result, knowing the opponent will do the same. `dp[state]` = best outcome the current player can guarantee from this state.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 292. Nim Game | Pure math insight — no DP needed; builds "optimal play" intuition |
| 2 | LC 877. Stone Game | Minimax DP — or math trick; know both, interviewers test the reasoning |
| 3 | LC 486. Predict the Winner | Classic minimax — pick from either end |
| 4 | LC 1406. Stone Game III | Minimax DP on array suffix — pick 1, 2, or 3 stones |
| 5 | LC 1140. Stone Game II | Minimax DP with expanding move range M |
| 6 | LC 464. Can I Win | Minimax + Bitmask — when simple math does not work |

---

## PHASE 5 — Know About (No Deep Dive Needed)

---

> **DP Optimizations — Divide & Conquer DP, Convex Hull Trick, Knuth's:**
>
> These are speed optimizations applied *after* you have already found the correct DP recurrence. They are not pattern identifiers — they do not help you recognize what type of problem you are facing.
>
> - **Divide & Conquer DP:** When the optimal split point in interval DP is monotone, reduces O(n²) to O(n log n).
> - **Convex Hull Trick:** Optimizes transitions of the form `dp[i] = min(dp[j] + b[j] * a[i])` from O(n²) to O(n).
> - **Knuth's Optimization:** Special case of D&C DP for interval DP when the cost satisfies the quadrangle inequality. O(n³) → O(n²).
>
> All three are competitive programming territory. For EU FAANG, an O(n²) correct solution will receive full credit in almost every interview where one of these theoretically applies. Do not invest practice time here.

---

> **DP Traps — Problems That Look Like DP but Are Not:**
>
> Recognizing when *not* to use DP is itself tested. The tell: if there is a locally optimal choice that is also globally optimal — no future decision can ever make you regret the current one — it is greedy, not DP.
>
> | Problem | Lesson |
> |---|---|
> | LC 55. Jump Game | Looks like DP, is greedy |
> | LC 45. Jump Game II | Minimum jumps — greedy BFS |
> | LC 435. Non-overlapping Intervals | Maximize kept intervals — greedy by end time |
> | LC 11. Container With Most Water | Two pointers, not DP |

---

## Final Summary

| Phase | Pattern | Problems |
|---|---|---|
| Phase 1 — Foundation | 1D DP (+ Sliding Window DP) | 12 |
| | Kadane's / Subarray DP | 5 |
| | DP on Grid | 15 |
| Phase 2 — Classical | 0/1 Knapsack (+ Grouped Knapsack) | 8 |
| | Unbounded Knapsack | 6 |
| | LIS (+ Hashmap DP + DS Combo) | 9 |
| | LCS | 7 |
| | String DP | 8 |
| Phase 3 — Advanced | Interval DP | 8 |
| | DP on Bitmask | 8 |
| | DP on Tree | 8 |
| | DP on DAG | 5 |
| Phase 4 — Specialized | State Machine DP | 8 |
| | Digit DP | 6 |
| | Probability DP | 5 |
| | Game DP | 6 |
| Phase 5 — Know About | DP Optimizations | reference only |
| | DP Traps | 4 contrast problems |
| **Total** | **16 active patterns** | **~123 problems** |

---

## How to Use This Sheet

**Order strictly matters.** Do not jump to Phase 3 without Phase 2 solid. Interval DP without LCS and Knapsack fluency becomes pure memorization — you will solve the exact problem you practiced but fail any variant.

**Two-pass each pattern.** First pass: solve each problem, use hints or editorial if stuck, understand the recurrence fully. Second pass one to two weeks later: solve cold, no hints.

**After all 16 patterns:** Spend two to three weeks exclusively on LC Weekly Contest hard problems and problems you cannot immediately pattern-match. That final step — training on the unfamiliar — is what closes the remaining gap and eliminates interview surprise.