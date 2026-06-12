# Recursion & Backtracking DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## The Core Mental Model — Before Any Pattern

**Recursion** = break a problem into smaller identical subproblems, trust the base case.

**Backtracking** = recursion + an explicit **undo step**. The three-step loop:
```
for each choice:
    make the choice       ← explore
    recurse
    undo the choice       ← this is what separates backtracking from plain recursion
```

**Pruning** = cutting branches that cannot possibly lead to a valid answer **before** recursing into them. This is not optional on hard problems — it is what makes the difference between TLE and AC. Every hard backtracking problem has at least one pruning condition. Find it before you code.

**When to use Backtracking vs DP:**
If the problem asks for **all** valid solutions (enumerate, collect, print) → Backtracking. If it asks for the **best** or **count** with overlapping subproblems → DP. Some problems sit at the boundary — see the note at the end of this sheet.

**Scope of this sheet:** Pure recursion mechanics and backtracking. Problems that primarily teach Graph theory (Eulerian path, SCC) or DP recurrences belong to those sheets. A problem stays here only if the dominant lesson is the backtracking technique itself.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Pure Recursion | Structural decomposition — no choices to undo |
| Subsets | "all subsets", "power set", include or exclude each element |
| Combinations | "choose k from n", "sum to target", start index prevents revisiting |
| Permutations | "all arrangements", order matters, visited\[\] tracks used elements |
| Partitioning & Parsing | "split string into valid parts", "generate parentheses", validity check on each segment |
| Constraint Satisfaction | "place on board", "fill grid", "no two conflict" — place → validate → undo |
| Advanced Backtracking & Pruning | Bucket filling, expression building, multi-constraint hard problems |

---

## Pattern 1: Pure Recursion (No Undo Step)

**Identify:** The problem has a natural recursive decomposition but there is no "choice + undo" loop. You recurse, combine results, return. No backtracking state to manage.

**Why this is its own pattern:** Jumping to backtracking before mastering clean recursion is the single most common cause of confusion. These problems build the recursion muscle — trust the base case, trust the return value — before the undo step is added.

| # | Problem | Key Concept |
|---|---|---|
| 1 | GFG: Tower of Hanoi | Classic decomposition — move n-1 to peg B, move nth to C, move n-1 from B to C |
| 2 | LC 50. Pow(x, n) | Fast exponentiation — halve the problem each call; handle negative n carefully |
| 3 | LC 394. Decode String | Recursion on nested brackets — process until matching `]`, multiply and continue |
| 4 | LC 21. Merge Two Sorted Lists | Recursive merge — smaller head recurses on the rest |
| 5 | GFG: Sort a Stack | Recursive insert-in-sorted-position — no loop, no extra structure |
| 6 | LC 273. Integer to English Words | Recursive chunking by thousands — structural decomposition, tricky edge cases |

---

## Pattern 2: Subsets

**Identify:** Generate all subsets (power set) of a given set. Every element has exactly two choices — include or exclude. No ordering constraint. Result count is always 2^n.

**Two equivalent templates:**

Include/exclude at each index — recurse twice at every index, once with and once without the current element.

Loop with start index — iterate from `start` to end, add element, recurse with `start = i+1`, remove element. This template generalizes naturally to Combinations.

**Handling duplicates:** Sort the input first. In the loop template, skip `nums[i] == nums[i-1]` when `i > start` to prune duplicate branches at the same level.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 78. Subsets | Pure subset template — no duplicates; implement both include/exclude and loop variants |
| 2 | LC 90. Subsets II | Sort + skip duplicate at same recursion level |

Only two problems here by design. The pattern is fully captured. Mastery means implementing both templates cleanly and explaining why the duplicate skip works.

---

## Pattern 3: Combinations

**Identify:** Choose elements that satisfy a constraint — exactly k elements, or elements summing to a target. Order does NOT matter. Always pass a `start` index to prevent revisiting earlier elements.

**Reuse vs No-reuse — the critical distinction:**
- No reuse: recurse with `start = i+1`
- Reuse allowed (LC 39): recurse with `start = i`

**Pruning:** Sort the input. Break the loop as soon as `nums[i] > remaining`. This single pruning condition often reduces runtime by orders of magnitude.

**Note on LC 17:** Letter Combinations is technically a Cartesian product (one character from each digit's set) rather than "choose k from n." The backtracking skeleton is identical, which is why it lives here, but recognize the distinction — it will not have a `start` index, just a digit index advancing by 1 each level.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 77. Combinations | Choose exactly k from n — pure template with start index |
| 2 | LC 39. Combination Sum | Reuse allowed — recurse with same `i`; prune when sum exceeds target |
| 3 | LC 40. Combination Sum II | No reuse, duplicates in input — sort + skip same value at same level |
| 4 | LC 216. Combination Sum III | Two simultaneous constraints — exactly k numbers, sum equals n, digits 1–9 only |
| 5 | LC 17. Letter Combinations of Phone Number | Cartesian product via backtracking — digit index advances each level, no start index |

---

## Pattern 4: Permutations

**Identify:** Generate all orderings of a set. Order matters — `[1,2]` and `[2,1]` are distinct. Every element appears exactly once per result. Uses a `visited[]` boolean array or swap-based approach instead of a `start` index.

**Two implementation approaches:**

visited[] array — iterate full array each call, skip visited elements. Cleaner for duplicates.

Swap-based — swap current position with each position from current onward, recurse, swap back. Slightly faster, harder to extend to Permutations II.

**Handling duplicates (LC 47):** Sort first. Skip `nums[i] == nums[i-1] && !visited[i-1]`. This condition is subtle — it ensures you only place a duplicate element after its identical predecessor has been placed in the current path, eliminating duplicate permutations.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 46. Permutations | Core permutation template — visited\[\] or swap-based; implement both |
| 2 | LC 47. Permutations II | Sort + skip duplicate at same level — visited\[\] approach is cleaner here |
| 3 | LC 526. Beautiful Arrangement | Permutation with divisibility constraint — prune early when condition fails |
| 4 | LC 60. Permutation Sequence | Factorial number system — find kth permutation directly without generating all |

---

## Pattern 5: Partitioning & Parsing

**Identify:** Split a string or sequence into parts where each part must pass a validity check. Iterate over all possible cut points, validate the current segment, recurse on the remainder. The `start` index advances to the position after the cut.

**Common validity checks:** Is it a palindrome? Is it a valid IP segment (0–255, no leading zeros)? Does it satisfy a numeric constraint?

**Generate Parentheses fits here:** At each step, choose to add `(` or `)`. The validity constraint — `open ≤ n` and `close ≤ open` — is checked before recursing, not after. This is pruning built into the choice logic.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 22. Generate Parentheses | Build valid string character by character — prune on open/close count constraints |
| 2 | LC 131. Palindrome Partitioning | Try every prefix that is a palindrome — recurse on remaining suffix |
| 3 | LC 93. Restore IP Addresses | Exactly 4 segments, each 0–255, no leading zeros — two hard constraints |
| 4 | LC 842. Split Array into Fibonacci Sequence | Each next number = sum of previous two — prune aggressively on value overflow |

---

## Pattern 6: Constraint Satisfaction

**Identify:** Place items on a board or fill a grid such that no two items conflict. The loop is: try a placement → validate → recurse → undo. The critical insight: **optimize validation first**, because it runs at every node in the search tree.

**N-Queens:** Track three sets — `cols`, `diag1` (row-col), `diag2` (row+col). Checking if a cell is under attack becomes O(1) set lookup instead of O(n) board scan.

**Sudoku:** Pre-build row, col, and box constraint sets. At each empty cell, only iterate digits absent from all three. The constraint sets eliminate invalid choices before you even place them.

**Grid path problems (Rat in a Maze, LC 980):** Mark the cell visited before recursing, unmark on return. This is the standard grid backtracking template.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 51. N-Queens | O(1) attack check via col/diagonal sets — classic constraint satisfaction |
| 2 | LC 52. N-Queens II | Same skeleton, only count — reinforces that enumerate vs count uses identical recursion |
| 3 | LC 37. Sudoku Solver | Row/col/box constraint sets — only try valid digits; first valid completion wins |
| 4 | GFG: Rat in a Maze | Grid path backtracking template — mark visited, recurse 4 directions, unmark |
| 5 | LC 980. Unique Paths III | Visit all non-obstacle cells exactly once — backtracking with visited state on grid |

---

## Pattern 7: Advanced Backtracking & Pruning

**Identify:** Hard problems that don't fit cleanly into a single earlier pattern — bucket filling, expression building, multi-constraint grid search. The unifying skill is **aggressive pruning** — without it, all these problems are TLE even with correct logic.

**Bucket filling (LC 698, LC 473):** Partition elements into k equal-sum groups. Key pruning rules: skip a bucket if it's identical to a previously tried empty bucket (symmetry pruning), skip if current element alone exceeds the target, return false immediately if element cannot fit anywhere. These three pruning conditions together make the difference between passing and TLE.

**Expression building (LC 282):** Insert +/−/× between digits to reach a target. The hard part is multiplication precedence — track `lastOperand` and undo it before applying multiplication: `currentVal - lastOperand + lastOperand * nextNum`. Prune when remaining digits cannot possibly bridge the gap to target.

**Word Search II:** The Trie is an optimization, not the lesson. The lesson is: prune on Trie prefix mismatch, remove matched words from the Trie to prevent duplicates. One DFS pass instead of one per word.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 698. Partition to K Equal Sum Subsets | Bucket filling — symmetry pruning + skip identical empty buckets |
| 2 | LC 473. Matchsticks to Square | Same bucket pattern — k=4 fixed, earlier pruning possible due to sorted input |
| 3 | LC 282. Expression Add Operators | Insert operators between digits — track lastOperand for multiplication precedence |
| 4 | LC 79. Word Search | Grid DFS + backtracking — mark cell visited in-place (`#`), restore on return |
| 5 | LC 212. Word Search II | Trie + grid DFS — prune on Trie prefix miss; delete matched words to avoid duplicates |
| 6 | LC 301. Remove Invalid Parentheses | Backtracking with min-removal count precomputed — prune when remaining chars cannot fix balance |
| 7 | LC 291. Word Pattern II | Pattern matching with bijection constraint — backtracking with two-way map validation |

---

## Note: Backtracking + Memoization (Boundary with DP)

Some problems use the backtracking/DFS skeleton but add memoization to avoid recomputing the same subproblem. These sit at the exact boundary between backtracking and DP.

Key examples: **LC 140 Word Break II** (covered in DP sheet — String DP), **LC 95 Unique Binary Search Trees II** (covered in BST sheet — BST + DP), **LC 241 Different Ways to Add Parentheses** (covered in DP sheet — Interval DP).

Since all three are already addressed in their primary sheets, no duplication is needed here. The concept to remember: when a backtracking solution has overlapping subproblems and you can define a unique state, memoize on that state. The DFS structure stays identical; you add a `memo` hashmap keyed on the current state.

---

## Final Summary

| Pattern | Problems | Core Mechanism |
|---|---|---|
| Pure Recursion | 6 | Structural decomposition, no undo step |
| Subsets | 2 | Include/exclude each element, 2^n results |
| Combinations | 5 | start index + reuse vs no-reuse distinction |
| Permutations | 4 | visited\[\] or swap; order matters |
| Partitioning & Parsing | 4 | Validate current segment, recurse on remainder |
| Constraint Satisfaction | 5 | Place → validate (O(1)) → undo |
| Advanced Backtracking & Pruning | 7 | Aggressive pruning is the entire lesson |
| **Total** | **~33 problems** | |

---

## How to Use This Sheet

**Do Pure Recursion first without exception.** If you cannot implement Tower of Hanoi and Decode String cleanly, you are not ready for backtracking.

**Subsets → Combinations → Permutations is the mandatory progression.** Each pattern changes exactly one thing from the previous. Skipping steps means you memorize solutions instead of understanding the structural shift.

**Constraint Satisfaction and Partitioning can run in parallel** once Patterns 1–4 are solid.

**Advanced Backtracking last.** LC 698 and LC 282 will feel approachable only after you have clean implementations of the earlier patterns. Attempting them first leads to brute-force solutions that miss all the pruning — which is the entire lesson.