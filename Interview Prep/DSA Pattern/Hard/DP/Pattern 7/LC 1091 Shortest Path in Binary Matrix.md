# LC 1091. Shortest Path in Binary Matrix

Solution: https://www.youtube.com/watch?v=U5Mw4eyUmw4&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=36&ab_channel=takeUforward
Status: Done

# LC 1091. Shortest Path in Binary Matrix

---

Before we start — one important flag: **the transcript you gave me is Striver's "Shortest Distance in a Binary Maze" (GFG) lecture, not LC 1091 itself.** The core algorithmic idea is identical, but the problem specifics differ in three places. I'll call these out clearly so you don't mix them up when revising:

|  | GFG (transcript) | LC 1091 (this problem) |
| --- | --- | --- |
| Walkable cell value | `1` | `0` |
| Blocked cell value | `0` | `1` |
| Allowed moves | 4-directional | **8-directional** (includes diagonals) |
| Source / Destination | given arbitrary cells | always `(0,0)` → `(n-1,n-1)` |
| Answer measures | number of **steps** taken | number of **cells** visited (steps + 1) |

The **key concept is the same algorithm** — I'm just adapting the four specifics above. Let's build the note properly.

---

## Stage 1: Identification

**Step 1 — Which topic?**

You're given a 2D grid and asked to move between cells under a blocking constraint, to find the shortest path from one corner to the other. A grid where you move between adjacent cells is always an **implicit graph** — each cell is a node, each valid move is an edge. Topic: **Graph**.

**Step 2 — Which pattern?**

Every edge in this implicit graph costs the same — moving from one cell to an adjacent cell always costs exactly `+1`, regardless of direction. This is a **unit-weight graph**. The trigger:

> "shortest path", "minimum steps", **all edges cost the same** → Pattern 8 territory, but the *unit weight* detail matters — see Step 3.
> 

**Step 3 — Which key concept?**

**`Grid BFS via Dijkstra → Queue reduction`**

This is the exact insight from the transcript: Dijkstra's algorithm is *always correct* here, but the **priority queue is unnecessary**. Since every move costs `+1`, a plain `Queue` already dequeues nodes in increasing order of distance — the min-heap's only job (always giving you the minimum) is already done for free by FIFO ordering. So the "key concept" is really:

> Recognize a unit-weight shortest-path problem → skip Dijkstra's `O(log V)` overhead → use plain BFS instead.
> 

The grid twist: since there's no explicit adjacency list, you generate a cell's neighbours on the fly using **direction delta arrays**.

---

## Stage 2: Intuition Building

### The Core Question

> *"If every move costs the same, does the order I explore cells in matter for correctness?"*
> 

Think about what a priority queue buys you in Dijkstra: whenever weights vary, a node reached via a long chain of tiny edges might still be cheaper than a node reached via one huge edge. You need the heap to always surface the *globally* cheapest option next.

But here, every edge is `+1`. If you start at distance 0 and expand outward, **every neighbour you touch during the first expansion is at distance exactly 1. Every neighbour of those is at distance exactly 2.** There is no way to "skip ahead" — the frontier grows in perfectly synchronized rings. A plain FIFO queue naturally preserves this ring-by-ring order, because you always finish inserting all distance-`d` cells into the queue **before** you insert any distance-`(d+1)` cell (since distance-`(d+1)` cells only get discovered by popping distance-`d` cells first).

```
┌────────────────────────────────────────────────────────────────┐
│  Dijkstra's ONLY job is: "always process the minimum next."    │
│  A min-heap achieves that in general.                          │
│  A plain queue achieves the SAME result for free               │
│  whenever every edge weight is identical.                      │
│                                                                │
│  So: unit-weight shortest path = BFS, not Dijkstra.            │
│  (This is literally what the transcript proves by hand,        │
│   watching the queue naturally output 1,1,1,1 → 2,2,2,2 → ...) │
└────────────────────────────────────────────────────────────────┘
```

This is precisely why we already used plain BFS for the earlier "Shortest Path in Undirected Graph, Unit Weights" problem. LC 1091 is the same idea, just on an **implicit grid graph with 8 directions** instead of an explicit adjacency list.

---

### Generating Neighbours Without an Adjacency List

A grid cell `(row, col)` doesn't come with a pre-built neighbour list — you build it on the spot using **delta arrays**. For 8-directional movement (4 orthogonal + 4 diagonal), the deltas are:

```
dr = [-1, -1, -1,  0, 0,  1, 1, 1]
dc = [-1,  0,  1, -1, 1, -1, 0, 1]
```

Each index `i` gives one of the 8 neighbours: `(row + dr[i], col + dc[i])`. This covers up, down, left, right, and all four diagonals in one loop — no need to hand-write eight separate `if` blocks.

```
      (r-1,c-1) (r-1,c) (r-1,c+1)
      (r,  c-1)  (r,c)  (r,  c+1)
      (r+1,c-1) (r+1,c) (r+1,c+1)
```

---

### The Two Edge-Case Details LC 1091 Adds

1. **Start/end blocked check:** If `grid[0][0] == 1` or `grid[n-1][n-1] == 1`, no path can exist at all — return `1` immediately, before touching BFS.
2. **Answer = number of cells, not number of steps.** The problem defines path length as the count of visited cells in the path, including both source and destination. So if `dist[]` tracks *steps* the usual way (`dist[source] = 0`), the final answer is `dist[destination] + 1`. Equivalently, you can just initialize `dist[source] = 1` from the start and never add the `+1` at the end — cleaner, and what the code below does.

---

### The Key Insight to Carry

```
┌──────────────────────────────────────────────────────────────────┐
│  LC 1091 = BFS on an implicit 8-directional grid graph           │
│                                                                  │
│  Why BFS, not Dijkstra?                                          │
│    → Every move costs exactly +1 → queue self-sorts by distance  │
│    → Priority queue's O(log V) overhead buys nothing here        │
│                                                                  │
│  Grid-specific adaptation:                                       │
│    → No adjacency list — generate neighbours via dr[]/dc[]       │
│    → 0 = walkable, 1 = blocked (opposite of the GFG version!)    │
│    → Answer counts CELLS, not steps → dist[src] = 1, not 0       │
│    → If src or dest itself is blocked → return -1 immediately    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Stage 3: Coding

### Brute Force

> "Try every possible path from `(0,0)` to `(n-1,n-1)` via DFS, track the length of each valid path, return the minimum."
> 
- Explore all 8 directions recursively at every cell, backtrack, collect path lengths
- Exponential — in the worst case `O(8^(n²))` paths to explore
- Correct but completely impractical; establishes the baseline only. No code needed.

---

### Optimal — BFS (Dijkstra reduced to Queue)

**Mental workflow before writing a single line:**

```
1. Edge case: if grid[0][0] == 1 or grid[n-1][n-1] == 1 → return -1

2. dist[][] = 2D array, initialized to (int)1e9 for every cell
   dist[0][0] = 1   (this cell alone counts as path length 1)

3. Queue seeded with (0, 0)

4. BFS:
   → pop (row, col)
   → for each of the 8 directions (using dr[]/dc[]):
       newRow = row + dr[i], newCol = col + dc[i]
       if newRow, newCol in bounds
          AND grid[newRow][newCol] == 0
          AND dist[row][col] + 1 < dist[newRow][newCol]:
             dist[newRow][newCol] = dist[row][col] + 1
             push (newRow, newCol) into queue

5. After BFS:
   if dist[n-1][n-1] == (int)1e9 → return -1
   else → return dist[n-1][n-1]
```

```java
import java.util.*;

class Solution {
    public int shortestPathBinaryMatrix(int[][] grid) {
        int n = grid.length;

        // Edge case: source or destination itself is blocked
        if (grid[0][0] == 1 || grid[n - 1][n - 1] == 1) {
            return -1;
        }

        // Special case: 1x1 grid, source == destination
        if (n == 1) {
            return 1;
        }

        // dist[][] tracks the shortest CELL COUNT to reach each cell.
        // Using (int)1e9 as infinity — same reasoning as always:
        // safe from overflow when doing dist[curr] + 1.
        int[][] dist = new int[n][n];
        for (int[] row : dist) {
            Arrays.fill(row, (int) 1e9);
        }

        // The source cell itself counts as 1 cell in the path —
        // that's why we seed with 1, not 0. This avoids a
        // "+1" adjustment at the very end.
        dist[0][0] = 1;

        // Plain Queue — NOT a PriorityQueue.
        // WHY: every move costs exactly +1 (unit weight),
        // so the queue naturally dequeues cells in increasing
        // order of distance. A min-heap would give the same
        // result at extra O(log V) cost for nothing gained.
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{0, 0});

        // 8-directional deltas: 4 orthogonal + 4 diagonal
        int[] dr = {-1, -1, -1,  0, 0,  1, 1, 1};
        int[] dc = {-1,  0,  1, -1, 1, -1, 0, 1};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int row = curr[0], col = curr[1];

            for (int i = 0; i < 8; i++) {
                int newRow = row + dr[i];
                int newCol = col + dc[i];

                // Bounds check
                if (newRow < 0 || newRow >= n || newCol < 0 || newCol >= n) {
                    continue;
                }

                // Must be walkable (0 = clear in LC 1091)
                if (grid[newRow][newCol] == 1) {
                    continue;
                }

                // Relaxation condition — identical spirit to Dijkstra,
                // just without the priority queue
                if (dist[row][col] + 1 < dist[newRow][newCol]) {
                    dist[newRow][newCol] = dist[row][col] + 1;
                    queue.add(new int[]{newRow, newCol});
                }
            }
        }

        int answer = dist[n - 1][n - 1];
        return answer == (int) 1e9 ? -1 : answer;
    }
}
```

---

### Workflow Trace on a Small Example

```
grid = [[0,0,0],
        [1,1,0],
        [1,1,0]]

n = 3, source = (0,0), destination = (2,2)

INITIAL STATE
────────────────────────────────────────────────
dist = [ [1, ∞, ∞],
         [∞, ∞, ∞],
         [∞, ∞, ∞] ]
Queue: [(0,0)]

POP (0,0), dist=1
  8 neighbours checked, valid + walkable ones:
    (0,1): 0-cell → dist=2, push
  (row-1 out of bounds, (1,0)/(1,1) blocked, (1,-1) out of bounds, etc.)
────────────────────────────────────────────────
dist = [ [1, 2, ∞],
         [∞, ∞, ∞],
         [∞, ∞, ∞] ]
Queue: [(0,1)]

POP (0,1), dist=2
  (0,0): already 1, discard
  (0,2): walkable → dist=3, push
  (1,0): blocked, skip
  (1,1): blocked, skip
  (1,2): walkable (diagonal move) → dist=3, push
────────────────────────────────────────────────
dist = [ [1, 2, 3],
         [∞, ∞, 3],
         [∞, ∞, ∞] ]
Queue: [(0,2), (1,2)]

POP (0,2), dist=3
  (1,2): already 3, not strictly less, discard
  (others blocked/out of bounds)

POP (1,2), dist=3
  (2,1): blocked, skip
  (2,2): walkable → dist=4, push
  (2,3): out of bounds
────────────────────────────────────────────────
dist = [ [1, 2, 3],
         [∞, ∞, 3],
         [∞, ∞, 4] ]
Queue: [(2,2)]

POP (2,2), dist=4 → destination popped
  no further improvement possible for anyone
Queue empty → DONE

dist[2][2] = 4 → return 4
```

Path: `(0,0) → (0,1) → (1,2) → (2,2)` — 4 cells, using one diagonal hop. Matches the expected LC 1091 answer for this grid.

---

## Complexity Analysis

### Time Complexity — O(n²)

Every cell is enqueued at most a constant number of times before its `dist` value stabilizes (in practice bounded by the number of successful relaxations, which is `O(V)` amortized for BFS-style unit-weight expansion — no `log V` factor since there's no heap). For each cell popped, we check exactly 8 neighbours — a constant. So:

```
Total cells = n²
Work per cell = O(8) = O(1)
─────────────────────────────
Total: O(n²)
```

Compare this to the Dijkstra-with-heap version, which would cost `O(n² log(n²))` = `O(n² · 2 log n)` = `O(n² log n)` for the exact same problem — strictly worse, for zero benefit, because the weights are uniform.

### Space Complexity — O(n²)

```
dist[][] array   → O(n²)
Queue            → O(n²) worst case (every cell pushed once)
─────────────────────────────
Total: O(n²)
```

---

## Quick Revision Checklist

- [ ]  Why does a plain `Queue` give the correct Dijkstra-equivalent answer here, and *only* here (not in general weighted graphs)?
- [ ]  What are the two "polarity flips" between this LC version and the GFG transcript version (walkable value, direction count)?
- [ ]  Why is `dist[src]` initialized to `1` instead of `0` in this problem specifically?
- [ ]  What are the two edge cases you must check *before* even starting BFS?
- [ ]  Why does the `dr[]`/`dc[]` delta-array technique scale cleanly from 4-directional to 8-directional movement without restructuring the algorithm?

---
