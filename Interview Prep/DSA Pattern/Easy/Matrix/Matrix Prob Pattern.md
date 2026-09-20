# Matrix DSA Patterns

*"Problems are infinite, but patterns are finite!"*

---

## Step 1 — What's Actually New Here

Matrix is the last of this six-topic batch, and it has a specific contamination risk: grid problems that are secretly **graph traversal** (flood fill, BFS/DFS on a grid) already belong entirely to the Graph sheet's "Grid DFS / Flood Fill" and "Multi-Source BFS" patterns — nothing in the raw Matrix lists actually overlaps with those, which is worth confirming explicitly rather than assuming. What *does* overlap is prefix-sum and binary-search machinery already built out elsewhere.

**Already fully covered elsewhere — cross-reference, don't re-teach:**

| Problem | Primary Home |
|---|---|
| LC 1314. Matrix Block Sum | Prefix Sum sheet, Pattern 4 (2D Prefix Sum) |
| LC 2536. Increment Submatrices by One | Prefix Sum sheet, Pattern 4 (2D Difference Array) |
| LC 304 / LC 74. Range Sum Query 2D / Search a 2D Matrix | Prefix Sum sheet (Pattern 4) / Binary Search sheet (Pattern 3) |
| LC 240. Search a 2D Matrix II | Binary Search sheet, Pattern 3 (staircase search) |
| LC 37. Sudoku Solver | Recursion & Backtracking sheet, Pattern 6 (Constraint Satisfaction) |
| LC 200 / LC 130 / any flood-fill grid problem | Graph sheet, Pattern 2 (Grid DFS / Flood Fill) — none of the raw Matrix lists actually contained these, confirmed on inspection |

**Dropped as too trivial to carry a lesson:** LC 1672 (Richest Customer Wealth) is a sum-each-row-take-max one-liner with no transferable technique beyond "iterate a 2D array" — including it would pad the sheet without adding to the concept dictionary, consistent with your instruction to drop "garbage" problems unless they're famous or frequently asked (this one is neither).

What's left is genuinely matrix-specific: traversal-order simulation, diagonal indexing as a grouping trick, in-place geometric transformation, row/column aggregate precomputation, grid-state simulation, and a couple of global-invariant arguments that only make sense on a 2D grid.

---

## The Core Mental Model — Before Any Pattern

A matrix is just a 2D array, but four ideas recur constantly enough to be named as the topic's own raw toolkit:

1. **Traversal order is itself the problem** — spiral, diagonal, and boustrophedon (zigzag) orders require careful boundary/direction bookkeeping, not cleverness.
2. **`row - col` is constant along a diagonal; `row + col` is constant along an anti-diagonal.** This single fact turns "group cells by which diagonal they're on" from a 2D problem into a 1D bucketing problem — one of the highest-leverage identities in this entire sheet.
3. **In-place transformation almost always decomposes into two simpler, well-known operations** (rotate = transpose + reverse), or uses part of the matrix itself as scratch space (first row/column as a marker) to avoid allocating `O(mn)` extra memory.
4. **Row and column aggregates (sums, counts, maxes) computed once up front turn an `O(mn)` per-query brute-force into an `O(1)` per-cell lookup** — the 2D analogue of the single-pass tracking instinct from the Arrays sheet.

---

## Pattern Identification Quick Reference

| Pattern | Trigger |
|---|---|
| Traversal Order Simulation | "spiral order", "diagonal order", direction-changing boundary walk |
| Diagonal Indexing as a Grouping Key | "sort the matrix diagonally", diagonal sum, cells sharing `r-c` or `r+c` |
| In-Place Geometric Transformation | "rotate image", "transpose", "set matrix zeroes", O(1) extra space required |
| Row/Column Aggregate Precomputation | "difference between ones and zeros", "equal row/column pairs", "lucky numbers", per-row/column property compared across the grid |
| Grid Simulation with Encoded State | "game of life", "candy crush", "rotate the box" (gravity), ball/light falling through a grid |
| Constraint Validation & Greedy Construction | "valid sudoku", "queens that can attack the king", "construct a matrix given row/column sums" |
| Shift/Overlap Enumeration | "image overlap", count matches after translating one grid onto another |
| Global Parity/Invariant Argument | "maximum matrix sum" via sign flips, "minimum operations to make uni-value" |

---

## Pattern 1: Traversal Order Simulation

**Identify:** The entire problem *is* the order in which you visit cells — spiral inward, diagonal zigzag — with no aggregation or transformation layered on top. There's no clever algorithmic insight to discover; the skill is maintaining boundary variables correctly and turning at the right moment, similar in spirit to the Strings sheet's "Simulation & Careful Parsing" pattern, just in 2D.

**LC 54 (Spiral Matrix) — the anchor:** Maintain four boundaries (`top, bottom, left, right`), walk right along the top row, down the right column, left along the bottom row, up the left column, shrinking each boundary inward after its pass — and check `top <= bottom` / `left <= right` before *each* of the four legs individually (not just once per outer loop iteration), since a non-square matrix can exhaust one dimension before the other mid-loop.

**LC 59 (Spiral Matrix II) — the same walk, generating instead of reading:** Identical boundary-shrinking mechanic, but filling in increasing integers as you go instead of reading existing values — confirms the traversal logic is the whole skill, independent of whether you're producing or consuming.

**LC 498 (Diagonal Traverse) — direction alternates by diagonal, not by cell:** Group cells by `r + c` (anti-diagonal index — see Pattern 2 for why this grouping works at all); traverse each anti-diagonal group in an order that alternates between "top-to-bottom" and "bottom-to-top" depending on whether the diagonal's index is even or odd. This is the first problem where Pattern 2's `r+c` identity and Pattern 1's "careful direction bookkeeping" combine — worth noticing the overlap rather than treating diagonal traversal as unrelated to diagonal grouping.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 54. Spiral Matrix | Four shrinking boundaries, check each of the four legs independently before walking it |
| 2 | LC 59. Spiral Matrix II | Same boundary-shrinking walk, generating values instead of reading them |
| 3 | LC 498. Diagonal Traverse | Group by `r+c`, alternate direction per diagonal based on parity |

---

## Pattern 2: Diagonal Indexing as a Grouping Key

**Identify:** The problem cares about cells that lie on the *same diagonal* — sorting within a diagonal, summing a diagonal, or identifying which diagonal a cell belongs to. The identity that makes this tractable: **every cell `(r, c)` on the same top-left-to-bottom-right diagonal shares the same value of `r - c`**; every cell on the same anti-diagonal (top-right-to-bottom-left) shares the same value of `r + c`. This turns "which cells are on my diagonal" from a geometric question into a single arithmetic bucket key.

**LC 1329 (Sort the Matrix Diagonally) — the anchor:** Group every cell into a hashmap keyed by `r - c` (a min-heap or a simple list works as the bucket), collect all values on each diagonal, sort them, then write them back in order along that same diagonal. The entire problem is "recognize `r-c` as the grouping key" — once that clicks, it's an ordinary group-then-sort-then-scatter operation.

**LC 1572 (Matrix Diagonal Sum) — the same identity, applied without a hashmap:** The main diagonal is exactly where `r == c`; the anti-diagonal is exactly where `r + c == n - 1`. Sum both directly by iterating `r` from `0` to `n-1` and reading `matrix[r][r]` and `matrix[r][n-1-r]` — with the one edge case worth naming explicitly: for odd `n`, the center cell satisfies *both* conditions simultaneously, so it must be added only once, not twice.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1329. Sort the Matrix Diagonally | `r - c` as a hashmap bucket key — group, sort each diagonal, scatter back |
| 2 | LC 1572. Matrix Diagonal Sum | `r == c` and `r + c == n-1` read directly — center cell counted once for odd `n` |

---

## Pattern 3: In-Place Geometric Transformation

**Identify:** You need to transform the matrix's layout (rotate, transpose, zero out rows/columns) with `O(1)` extra space. The recurring trick: a complex transformation almost always **decomposes into two or three simpler, well-known operations performed in sequence**, or borrows a small unused part of the matrix itself (its first row/column) as scratch space instead of allocating a new structure.

**LC 867 (Transpose Matrix) — the building block:** Swap `matrix[i][j]` with `matrix[j][i]` for every `i < j`. Simple on its own, but this exact operation is the first half of the next problem — worth doing standalone first specifically so its role as a sub-step is recognized immediately afterward.

**LC 48 (Rotate Image) — the anchor, decomposition as the whole insight:** Rotating a matrix 90° clockwise is provably equivalent to **transposing it, then reversing each row**. Neither step alone rotates the matrix; the composition of exactly these two well-understood O(1)-space operations does. This is the clearest example in this sheet of "a seemingly novel transformation is actually two known operations chained" — the skill worth generalizing is looking for this kind of decomposition before inventing a bespoke in-place algorithm from scratch.

**LC 73 (Set Matrix Zeroes) — scratch space borrowed from the matrix itself:** If any cell `matrix[i][j]` is zero, its entire row and column must become zero. Naively, you'd need a separate boolean array recording which rows/columns to zero — but the **first row and first column of the matrix itself** can store exactly that information (using `matrix[i][0]` and `matrix[0][j]` as marker flags), since those cells' original values are no longer needed once you've recorded whether row `i` or column `j` should be zeroed. One separate boolean is needed only for whether the first row/column *themselves* originally contained a zero (since they're now doing double duty as both data and markers) — this is the one edge case that breaks a naive implementation.

**LC 1861 (Rotating the Box) — gravity as a two-pointer sweep per row:** Simulating one item falling at a time is `O(n²)` per row in the worst case (a chain of stones falling one after another). Instead, treat each row independently with a "next empty landing spot" pointer, sweeping right to left: when a stone is found, drop it to the current landing-spot pointer and decrement that pointer; when an obstacle is found, reset the landing-spot pointer to just left of the obstacle. This is the same "read/write pointer" instinct as the Two Pointers sheet's in-place compaction pattern, applied per-row inside a 2D grid.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 867. Transpose Matrix | `matrix[i][j] ↔ matrix[j][i]` — the building block for rotation |
| 2 | LC 48. Rotate Image | Decomposition — 90° rotation = transpose, then reverse each row |
| 3 | LC 73. Set Matrix Zeroes | First row/column as marker storage — O(1) space, one extra flag for the first row/column's own original state |
| 4 | LC 1861. Rotating the Box | Per-row landing-spot two-pointer sweep — avoids simulating each stone's fall individually |

---

## Pattern 4: Row/Column Aggregate Precomputation

**Identify:** A query about a cell, row, or column depends on comparing it against a **property of every other row and column** — total ones, maximum value, count of something. Computing this fresh per query is `O(mn)` each time; precomputing per-row and per-column aggregates once, up front, turns every subsequent lookup into `O(1)`.

**LC 807 (Max Increase to Keep City Skyline) — the anchor:** For each cell, the maximum it can be raised to without changing any skyline view is `min(rowMax[i], colMax[j]) - grid[i][j]`. Precompute `rowMax[]` and `colMax[]` in one pass each, then a single final pass over every cell computes the total increase — the "skyline" framing is a disguise; the actual content is "precompute row and column maxes, then look them up."

**LC 2482 (Difference Between Ones and Zeros in Row and Column) — same precomputation, subtraction instead of min:** Precompute `onesRow[i], zerosRow[i], onesCol[j], zerosCol[j]` in two initial passes, then for every cell the answer is `(onesRow[i] + onesCol[j]) - (zerosRow[i] + zerosCol[j])` — direct arithmetic on precomputed aggregates, no per-cell recomputation.

**LC 2352 (Equal Row and Column Pairs) — aggregates as hashable keys, not just numbers:** Instead of a single aggregate number, each **entire row** (as a tuple/string key) is compared against each **entire column** for exact equality. Hash every row (as a string or tuple) into a frequency map, then for every column, check how many rows share its exact sequence — turning an `O(n³)` brute-force pairwise comparison into `O(n²)` hashing plus lookup.

**LC 1380 (Lucky Numbers in a Matrix) — aggregates as a search filter, not arithmetic:** A lucky number is the minimum in its row **and** the maximum in its column. Precompute the row-minimum for every row and the column-maximum for every column in two passes, then scan for any value that appears in both precomputed sets — turning a per-cell double-check into two clean precomputation passes plus a lookup.

**LC 2125 (Number of Laser Beams in a Bank) — a subtler row aggregate: only *consecutive non-empty* rows interact:** Count the number of devices (1s) in each row, but **skip empty rows entirely when pairing** — the number of beams between two devices only counts if they're in the *nearest* non-empty rows to each other, since a beam passing through an empty row still connects across it, but a beam is blocked by anything in a *populated* row between two devices. The total is the sum, over every pair of *consecutive non-empty* rows, of `count[i] * count[i+1]`. The insight worth flagging: this looks like ordinary row aggregation, but the pairing rule (consecutive non-empty, not literally adjacent) is the one place a naive per-adjacent-row aggregate would silently give the wrong answer.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 807. Max Increase to Keep City Skyline | Precompute row-max and col-max; answer per cell is `min(rowMax,colMax) - current` |
| 2 | LC 2482. Difference Between Ones and Zeros in Row and Column | Precompute four row/col count arrays; answer is direct arithmetic on lookups |
| 3 | LC 2352. Equal Row and Column Pairs | Hash entire rows as keys; count columns matching each row's exact sequence |
| 4 | LC 1380. Lucky Numbers in a Matrix | Precompute row-min and col-max sets; lucky number is in both |
| 5 | LC 2125. Number of Laser Beams in a Bank | Row device counts, paired only across *consecutive non-empty* rows — not literally adjacent rows |

---

## Pattern 5: Grid Simulation with Encoded or In-Place State

**Identify:** The matrix evolves through a rule applied simultaneously to every cell (cellular automaton, gravity, falling objects) or must stabilize through repeated passes, and the challenge is doing this **without a second full-size grid** to hold "the next state" while you're still reading "the current state" from the same array.

**LC 289 (Game of Life) — the anchor, in-place via bit-encoding:** Every cell's next state depends on its current neighbors, but if you overwrite a cell in-place before its neighbors have read *its* current value, you corrupt the simulation. The fix: **encode both the current and next state in the same cell using two bits** — e.g., use the low bit for the current state (needed by neighbors still being processed) and the high bit for the computed next state, then a final pass shifts every cell right by one bit to finalize. This bit-packing trick — "store two states in one cell, finalize with a cleanup pass" — is a genuinely reusable technique any time an in-place cellular-automaton update needs to avoid a second grid.

**LC 723 (Candy Crush) — repeated mark-then-drop until stable:** Two-phase loop: (1) scan for any run of 3+ identical adjacent candies (horizontally or vertically) and mark them (e.g., negate the value, so it's flagged without losing what it originally was, similar in spirit to the Arrays sheet's negation-marking trick); (2) apply gravity — compact each column's unmarked candies downward, filling the top with zeros — exactly the Pattern 3 read/write pointer instinct, per column. Repeat both phases until a full scan finds nothing left to mark. The "repeat until no more changes" structure, not either individual phase, is the actual content of this problem.

**LC 1706 (Where Will the Ball Fall) — simulate one ball per column, independently:** For each starting column, simulate the ball falling row by row: at each row, check whether the current cell and the cell it would move into form a valid "V" funnel (both walls slope the same direction) or a dead-end corner (walls form a "V" pointing the wrong way, or the ball would exit the grid). Each of the `n` balls takes `O(rows)` to simulate independently — the trick is recognizing there's no cross-ball interaction to worry about, so straightforward per-column simulation is already the intended and efficient solution, not something to over-optimize.

**LC 1260 (Shift 2D Grid) — flatten to 1D, shift, unflatten:** Rather than reasoning about wraparound in two dimensions directly, convert every cell's `(r, c)` to its flattened 1D index `r * cols + c`, apply the shift as simple modular arithmetic on that 1D index (`(index + k) % (rows * cols)`), then convert back to `(r, c)` for placement. The "flatten a 2D structure to reason about a 1D shift, then unflatten" trick generalizes beyond this specific problem whenever a 2D wraparound is being simulated.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 289. Game of Life | Two-bit in-place state encoding — current state in one bit, next state in another, finalize with a shift pass |
| 2 | LC 723. Candy Crush | Repeated mark (negate) → gravity-compact (per-column two-pointer) → repeat until stable |
| 3 | LC 1706. Where Will the Ball Fall | Independent per-column simulation — check V-funnel validity row by row |
| 4 | LC 1260. Shift 2D Grid | Flatten to 1D index, shift via modular arithmetic, unflatten |

---

## Pattern 6: Constraint Validation & Greedy Construction

**Identify:** Either verify that a grid satisfies a set of positional constraints (rows, columns, boxes; attack lines), or **construct** a valid grid satisfying given row/column totals. The validation half reuses the "one hashset per constraint group" idea; the construction half is a direct greedy fill.

**LC 36 (Valid Sudoku) — the anchor for validation, and the lighter sibling of LC 37:** Maintain one hashset per row, one per column, and one per 3×3 box (indexed by `(r/3, c/3)`); for every filled cell, check membership in all three relevant sets before adding it — any collision means the board is invalid. This is deliberately simpler than the Recursion & Backtracking sheet's LC 37 (Sudoku Solver): LC 36 only *checks* a fully or partially filled board once, with no backtracking search over empty cells at all — worth doing before LC 37 specifically to isolate the constraint-set bookkeeping from the search logic layered on top of it there.

**LC 1222 (Queens That Can Attack the King) — direct 8-direction simulation on a small fixed board:** From the king's position, walk outward along each of the 8 directions (the same direction-vector list as any grid-DFS setup) one step at a time; the *first* queen encountered along a given direction is the one that can attack (any queens further away are blocked) — so each direction stops as soon as it finds one queen or exits the board. Because the board is a fixed 8×8, this direct simulation is both the simplest and the intended solution — no need for anything more sophisticated than careful direction bookkeeping.

**LC 1605 (Find Valid Matrix Given Row and Column Sums) — greedy cell-by-cell construction:** At each cell `(i, j)`, place `min(rowSum[i], colSum[j])` — the largest value that can't violate either constraint — then subtract that amount from both the row's and column's remaining budget, and move to the next cell. The greedy claim worth stating explicitly: placing anything less than the min at this cell can never help, since the remaining budget for whichever side you under-filled still has to be placed somewhere else in that same row/column eventually, and placing the max safe amount now never forecloses a later valid placement.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 36. Valid Sudoku | One hashset per row/column/box — pure validation, no search *(cross-ref: Recursion & Backtracking sheet Pattern 6 for the harder LC 37 Solver)* |
| 2 | LC 1222. Queens That Can Attack the King | 8-direction walk from the king, first queen per direction is the answer |
| 3 | LC 1605. Find Valid Matrix Given Row and Column Sums | Greedy — place `min(rowSum, colSum)` per cell, deduct from both budgets |

---

## Pattern 7: Shift/Overlap Enumeration

**Identify:** You need to find the best way to slide one grid over another (or over itself) to maximize some overlap count, and the search space of possible shifts, while finite, still needs a smarter approach than checking every cell alignment from scratch at every shift.

**LC 835 (Image Overlap) — the anchor:** Rather than trying every `(dx, dy)` shift and then re-scanning both entire grids to count overlaps at each shift (expensive), record the coordinates of every `1` in both grids first. Then, for every pair of a `1` in image A and a `1` in image B, compute the shift vector `(dx, dy) = (ax - bx, ay - by)` that would align them, and tally these shift vectors in a hashmap. The shift vector with the highest tally is the one that aligns the most `1`s simultaneously — converting an expensive "try every shift, rescan everything" search into a single pass over pairs of `1`s, counted via a hashmap.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 835. Image Overlap | Tally shift-vectors between every pair of `1`s across both grids via hashmap — best shift is the most frequent vector |

---

## Pattern 8: Global Parity/Invariant Argument

**Identify:** A grab-bag pairing, but both problems share a distinct flavor from everything above: the answer comes from reasoning about a **global property of the whole grid** (a parity count, a target value derived from all cells together) rather than any per-cell, per-row, or per-diagonal computation. Worth naming as its own small pattern specifically because the instinct — "step back and ask what invariant survives every allowed operation" — is different from every traversal/aggregation idea in this sheet.

**LC 1975 (Maximum Matrix Sum) — the anchor, a sign-parity invariant:** The allowed operation (flip the signs of any two *adjacent* cells) can move a negative sign anywhere in the grid, but it can never change the **parity of the total count of negative numbers** — flipping two signs at once changes that count by an even amount every time. So: if the number of negative values is even, every value can be made non-negative, and the answer is simply the sum of all absolute values. If it's odd, exactly one negative sign is unavoidable, and the optimal strategy is to leave it on the cell with the **smallest absolute value**, since that minimizes the one unavoidable loss — giving `sum(abs values) - 2 * min(abs value)`. No simulation of actual flips ever happens; the entire solution is reasoning about what the operation *cannot* change.

**LC 2033 (Minimum Operations to Make a Uni-Value Grid) — a feasibility invariant plus a median-minimization argument:** Since each operation adds or subtracts `x` from a single cell, every cell's value modulo `x` must already be identical across the whole grid, or making them all equal is outright impossible — a single global parity/modulus check up front. If feasible, the target value that minimizes total operations is the **median** of all flattened values (the standard "minimize sum of absolute differences" result — the same median-minimizing argument that underlies Binary Search sheet's Aggressive-Cows-adjacent minimax reasoning, though arrived at here by direct sorting rather than binary search). Flatten the grid, check the modulus condition once, sort, and sum absolute differences from the median, each divided by `x`.

| # | Problem | Key Concept |
|---|---|---|
| 1 | LC 1975. Maximum Matrix Sum | Sign-flip parity invariant — even negative count fixes everything, odd leaves exactly the smallest magnitude negative |
| 2 | LC 2033. Minimum Operations to Make a Uni-Value Grid | Modulus feasibility check + median-minimizes-total-absolute-distance argument |

---

## Final Summary

| Pattern | Problems (new) | Core Mechanism |
|---|---|---|
| Traversal Order Simulation | 3 | Careful boundary/direction bookkeeping — no aggregation, the order IS the answer |
| Diagonal Indexing as a Grouping Key | 2 | `r-c` / `r+c` collapse a 2D diagonal relationship into a 1D bucket key |
| In-Place Geometric Transformation | 4 | Decompose into known simpler ops, or borrow the matrix's own cells as scratch space |
| Row/Column Aggregate Precomputation | 5 | Precompute once per row/column, turn per-cell queries into O(1) lookups |
| Grid Simulation with Encoded State | 4 | Encode next-state in-place, or simulate independently per row/column |
| Constraint Validation & Greedy Construction | 3 | Hashset-per-constraint validation, or direct greedy min-fill construction |
| Shift/Overlap Enumeration | 1 | Hashmap-tallied shift vectors between two point sets, not brute-force shift trials |
| Global Parity/Invariant Argument | 2 | Reason about what an operation *cannot* change, not what it does |
| **Total** | **24 new** | |

---

## How to Use This Sheet

**Pattern 2's `r-c`/`r+c` identity is the single highest-leverage fact in this entire sheet — internalize it before anything else here.** It resurfaces inside Pattern 1 (LC 498's diagonal traversal direction) and would resurface in any future diagonal-related problem you encounter that isn't in this list — recognizing "cells sharing a diagonal" as "cells sharing a value of `r±c`" converts a geometric intuition into an arithmetic one instantly.

**Pattern 3 is where "look for a decomposition before inventing a bespoke algorithm" gets its clearest demonstration in this sheet.** LC 48's rotate-via-transpose-then-reverse is worth stating out loud as a general heuristic: before writing a custom O(1)-space in-place transformation from scratch, ask whether the target transformation is actually a composition of two operations you already know cold.

**Pattern 5's LC 289 bit-encoding trick generalizes beyond Game of Life** — any cellular-automaton-style simultaneous update on a grid, where every cell's new value depends on its neighbors' *old* values, faces the same in-place-corruption risk, and the two-states-in-one-cell trick is the general fix, not a one-off hack for this specific problem.

**Pattern 8 is small but conceptually the most different pattern in this sheet — treat it as a mindset check, not a mechanism to drill.** Both problems are solved by *not* simulating the allowed operations at all, and instead asking "what stays invariant no matter how many times I apply this operation?" This is the same category of insight as the Bit Manipulation sheet's XOR-cancellation reasoning (Pattern 2) — a global algebraic property replacing brute-force simulation — worth connecting the two if XOR cancellation is already solid.
