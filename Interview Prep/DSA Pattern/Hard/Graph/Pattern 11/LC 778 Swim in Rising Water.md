# LC 778. Swim in Rising Water

Key Concept: Kruskal-Style DSU on grid cell value grid[i][j]
Problem: https://leetcode.com/problems/swim-in-rising-water/description/
Solution: https://www.youtube.com/watch?v=9WYhuzn8hd8
Status: Done

# Stage 1: Identification

---

### **Step 1 — Which topic?**

You're given an `n × n` grid of elevations. Water rises over time, and at time `t`, you may swim between two adjacent cells only if *both* have elevation `≤ t`. You need the minimum time to travel from `(0,0)` to `(n-1,n-1)`. Movement between adjacent grid cells is always an implicit graph — each cell is a node, each valid move is an edge. Topic: **Graph**.

### **Step 2 — Which pattern?**

Strip away the "water rising" story and look at what's actually being asked:

> *"What is the minimum threshold `t` such that a path exists from start to end using only cells with elevation `≤ t`?"*
> 

This is **not** a shortest-path-by-sum problem. It's a **`minimax path`** problem — you're not minimizing total elevation crossed, you're minimizing the *single worst cell* you're ever forced to step on along the best possible route. We've seen this exact flavor before in **LC 1631 (Path With Minimum Effort)**, solved with Dijkstra using a `max()` relaxation instead of **`+`**.

But there's a second, equally valid way to see this same minimax structure — and it's the one this note focuses on:

> "minimum bottleneck to connect two fixed points", "connectivity threshold", "grid cells as nodes" → **Disjoint Set Union (Pattern 11)**
> 

### **Step 3 — Which key concept?**

**`Kruskal-Style DSU —`** 

- **`Activate Cells in Ascending Elevation Order,`**
- **`Union With Active Neighbors, Detect First Connection`**

Here is the one-sentence version of the entire algorithm:

> *Sort every cell by its elevation. "Turn on" cells one at a time, from lowest to highest. Every time you turn a cell on, union it with any already-turned-on neighbor. The instant `(0,0)` and `(n-1,n-1)` land in the same component, the elevation of the cell you just turned on is the answer.*
> 

This is structurally **identical** to Kruskal's Algorithm — except instead of sorting *edges* and asking "would this edge create a cycle?", we sort *cells* (nodes) and ask "does turning this cell on finally connect start and end?" It's the same greedy-ascending-DSU skeleton, aimed at a different question.

---

# Stage 2: Intuition Building

### The Core Question

Before any code, before even thinking about DSU, ask the most basic question:

> *"If I imagine water rising very slowly, second by second — at literally what instant does a swimmable path from start to end first exist?"*
> 

Think about what happens as `t` increases from 0 upward. At each moment, a cell is **"usable"** if its elevation is `≤ t`. As `t` grows, cells only ever get **added** to the usable set — a cell that was swimmable at `t=5` never becomes unswimmable at `t=6`. Connectivity between any two cells, once achieved, can **never be lost** as `t` increases further.

```
┌──────────────────────────────────────────────────────────────────────┐
│  **MONOTONICITY** — the property that makes everything work          │
│                                                                      │
│  *"Is (0,0) connected to (n-1,n-1) using cells ≤ t?"*                │
│  is a question whose answer flips from NO to YES exactly             │
│  once, as t increases from 0 to the maximum elevation.               │
│                                                                      │
│  It never flips back. This is exactly what makes binary              │
│  search on t (the alternate approach) valid — and it's               │
│  exactly what lets DSU process cells once, in one direction,         │
│  with no need to ever "undo" a union.                                │
└──────────────────────────────────────────────────────────────────────┘
```

Because of this one-way monotonicity, there's no need to binary-search or repeatedly re-run BFS. You can just **walk forward through time once**, in the exact order cells become usable, and stop the moment the answer reveals itself.

---

### Why "Sorted Cell Activation" Instead of "Sorted Edges"

In Kruskal's, you sort **edges** by weight and ask "does this edge connect two different components?" Here, it's more natural to sort **cells** (the nodes themselves) by elevation, because a cell's "activation" is really shorthand for *all four of its potential edges becoming available simultaneously* — an edge between two grid cells only matters once **both** endpoints are swimmable, and the later of the two elevations is what actually gates that edge.

So instead of building an explicit edge list of `max(grid[u], grid[v])` values and sorting `O(4·n²)` edges, we get the identical result more cleanly by sorting the `n²` **cells** themselves and unioning each newly-activated cell with whichever of its neighbors are *already* active:

```
┌──────────────────────────────────────────────────────────────────────┐
│  Turning ON cell C, and unioning it with already-ON neighbors,       │
│  is EXACTLY equivalent to processing every edge incident to C        │
│  whose weight equals max(grid[C], grid[neighbor]) — because          │
│  by definition, if the neighbor is already ON, its elevation         │
│  is ≤ grid[C], so grid[C] IS the max of that edge. Any edge          │
│  to a still-OFF neighbor isn't ready yet — its weight would be       │
│  the (still unprocessed) neighbor's higher elevation instead.        │
└──────────────────────────────────────────────────────────────────────┘
```

This is the same "cells as nodes" idea from **Number of Islands II**, but the *ordering* driving activation is completely different: there, cells arrived in a given operator sequence; here, **we choose the order ourselves** — strictly ascending by elevation — because that's the order in which the water actually makes them usable.

---

### Walking Through a Concrete Example

```
grid =
0  5  8
6  1  7
3  4  2
```

Start = `(0,0)`, elevation `0`. End = `(2,2)`, elevation `2`.

**Step 1 — Sort every cell by elevation:**

```
(0,0)=0, (1,1)=1, (2,2)=2, (2,0)=3, (2,1)=4, (0,1)=5, (1,0)=6, (1,2)=7, (0,2)=8
```

**Step 2 — Activate cells one by one, in the increasing order of elevation, & unioning with already-active neighbors:**

```
Activate (0,0)=0: no active neighbors → own component {(0,0)}
Activate (1,1)=1: no active neighbors → own component {(1,1)}
Activate (2,2)=2: no active neighbors → own component {(2,2)}
Activate (2,0)=3: no active neighbors → own component {(2,0)}

Activate (2,1)=4:
  neighbor (2,0)=3 is already ACTIVE → union → {(2,0),(2,1)}
  neighbor (2,2)=2 is already ACTIVE → union → {(2,0),(2,1),(2,2)}
  neighbor (1,1)=1 is already ACTIVE → union → {(1,1),(2,0),(2,1),(2,2)}
  Check: find(0,0) vs find(2,2) → (0,0) is still alone → **NOT connected yet**

Activate (0,1)=5:
  neighbor (0,0)=0 is already ACTIVE → union → {(0,0),(0,1)}
  neighbor (0,2) not yet active → skip
  neighbor (1,1)=1 is already ACTIVE → union → merges with the **BIG group**
                                        {(0,0),(0,1),(1,1),(2,0),(2,1),(2,2)}
  Check: find(0,0) vs find(2,2) → SAME ROOT NOW → **CONNECTED!**
```

**Answer: 5.**

Verify by eye: the path `(0,0)=0 → (0,1)=5 → (1,1)=1 → (2,1)=4 → (2,2)=2` has a maximum elevation of exactly `5`, and no path exists using only elevations `≤ 4` — confirmed by the trace above, where `(0,0)` was still completely isolated right after activating `(2,1)=4`.

```
┌──────────────────────────────────────────────────────────────────────┐
│  **WHY** the activating cell's OWN elevation is the correct answer   │
│                                                                      │
│  The instant start and end share a root, every cell that             │
│  contributed to that connection (including the one just              │
│  activated) has elevation ≤ the current cell's elevation —           │
│  because we process cells in strictly ascending order.               │
│                                                                      │
│  So t = (current cell's elevation) is SUFFICIENT to connect          │
│  them. And it's the SMALLEST sufficient t, because one instant       │
│  earlier — before this cell was activated — they were proven         │
│  NOT connected. No smaller t could possibly have worked.             │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 💡The Deeper Connection — Why This Is `*"Kruskal's for Minimax"*`

This isn't a coincidence or a trick — it's a direct consequence of a well-known property of Kruskal's Algorithm: **`in the Minimum Spanning Tree, the path between any two nodes has the smallest possible *"maximum edge weight"* among all paths between them in the original graph`.** This is called the **minimax path property** (sometimes the **"cycle property"**). Building the MST greedily — cheapest edges first, skip anything that creates a cycle — automatically constructs the bottleneck-optimal path between every pair of nodes as a side effect.

Our algorithm here is a lightweight, specialized version of exactly that: we don't need to build the *entire* MST or track cycles explicitly — we only care about **one specific pair** of nodes, `(0,0)` and `(n-1,n-1)`. So we can stop the instant *those two specific nodes* become connected, without needing to finish processing every remaining cell.

```
┌──────────────────────────────────────────────────────────────────────┐
│  **SWIM IN RISING WATER** — the pattern                              │
│                                                                      │
│  Question reframed: minimum t such that (0,0) and (n-1,n-1)          │
│  are connected using only cells with elevation ≤ t                   │
│                                                                      │
│  This is a **MINIMAX PATH problem** — same family as Path With       │
│  Minimum Effort (LC 1631), but solved here via DSU instead           │
│  of Dijkstra.                                                        │
│                                                                      │
│  Algorithm: sort all n² cells ascending by elevation → for           │
│  each cell, in that order, union it with any ALREADY-ACTIVE          │
│  neighbor → the elevation of the cell that FIRST causes              │
│  find(start) == find(end) is the answer                              │
│                                                                      │
│  Why correct: monotonicity (connectivity, once achieved,             │
│  never breaks as t grows) + the minimax/cycle property that          │
│  underlies Kruskal's Algorithm                                       │
└──────────────────────────────────────────────────────────────────────┘
```

# Stage 3: Coding

---

## Brute Force

> "Try every possible path from `(0,0)` to `(n-1,n-1)` via DFS/backtracking. For each complete path, compute its 'cost' as the maximum elevation encountered along the way. Return the minimum such cost across all paths."
> 
- Explore all 4 directions recursively, track the running max elevation, backtrack
- Exponential — paths can revisit combinatorially many routes even with a per-path visited set, worst case `O(4^(n²))`
- Correct but hopelessly impractical. Establishes the baseline only. No code needed.

---

## Better — Binary Search on the Answer + BFS/DFS Feasibility Check

Worth mentioning explicitly, since it's a very natural alternate approach given the monotonicity property established in Part 1:

> "Binary search on the candidate answer `t`. For a given `t`, run BFS/DFS from `(0,0)` using only cells with elevation `≤ t`, and check if `(n-1,n-1)` is reachable. Monotonicity guarantees this feasibility check flips from false to true exactly once as `t` increases — so binary search is valid."
> 
- Search space: `t` ranges from `0` to `n²-1` (the maximum possible elevation, since elevations are a permutation of `0` to `n²-1`)
- Each feasibility check costs `O(n²)` (a full grid BFS/DFS)
- Total: `O(n² log(n²))` = `O(n² log n)`

This is a legitimate, commonly-seen alternative — and so is Dijkstra with `max()`-based relaxation (identical in spirit to LC 1631, just on this specific start/end pair). We won't code either here, since **DSU is this problem's designated key concept** for Pattern 11 — but it's worth being able to name both alternatives out loud in an interview, since interviewers often ask "what's another way to solve this?"

---

## Optimal — Kruskal-Style DSU (Ascending Cell Activation)

**Mental workflow before writing a single line:**

```
1. n = grid size (n × n)

2. Flatten every cell (row, col) into a single DSU node id:
   nodeId = row * n + col
   → Initialize DisjointSet(n * n)

3. Collect all n² cells into a list, sort them ASCENDING by
   their grid elevation value

4. active[][] = boolean grid, all false
   (tracks which cells have been "turned on" so far)

5. Walk the sorted cell list, one cell at a time:
   → mark current cell as active
   → for each of its 4 neighbours (dr[]/dc[]):
        if neighbour is in bounds AND already active:
            union(currentNodeId, neighbourNodeId)
   → check: findUParent(startNode) == findUParent(endNode)?
        if YES → return grid[currentRow][currentCol] immediately
                 (this cell's elevation is the answer)

6. (Fallback — never actually reached, since the last cell
   activated is always the max-elevation cell, and by then
   the whole grid is one component)
```

---

### Full Java Implementation

```java
import java.util.*;

class DisjointSet {
    List<Integer> size = new ArrayList<>();
    List<Integer> parent = new ArrayList<>();

    public DisjointSet(int n) {
        for (int i = 0; i < n; i++) {
            size.add(1);
            parent.add(i);
        }
    }

    public int findUParent(int node) {
        if (node == parent.get(node)) {
            return node;
        }
        int ultimateParent = findUParent(parent.get(node));
        parent.set(node, ultimateParent);
        return parent.get(node);
    }

    public void unionBySize(int u, int v) {
        int ultimateParentU = findUParent(u);
        int ultimateParentV = findUParent(v);

        if (ultimateParentU == ultimateParentV) {
            return;
        }

        if (size.get(ultimateParentU) < size.get(ultimateParentV)) {
            parent.set(ultimateParentU, ultimateParentV);
            size.set(ultimateParentV,
                     size.get(ultimateParentV) + size.get(ultimateParentU));
        } else {
            parent.set(ultimateParentV, ultimateParentU);
            size.set(ultimateParentU,
                     size.get(ultimateParentU) + size.get(ultimateParentV));
        }
    }
}

class Solution {
    public int swimInWater(int[][] grid) {
        int n = grid.length;

        // ─────────────────────────────────────────
        // STEP 1: Flatten cells into DSU nodes.
        // nodeId(row, col) = row * n + col — the same
        // flattening trick used in Number of Islands II.
        // ─────────────────────────────────────────
        DisjointSet ds = new DisjointSet(n * n);

        // ─────────────────────────────────────────
        // STEP 2: Collect every cell, sort ASCENDING
        // by elevation. This is the order water makes
        // them usable — exactly the order we must
        // activate them in.
        //
        // Each entry: {elevation, row, col}
        // ─────────────────────────────────────────
        int[][] cells = new int[n * n][3];
        int idx = 0;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                cells[idx][0] = grid[r][c];
                cells[idx][1] = r;
                cells[idx][2] = c;
                idx++;
            }
        }

        Arrays.sort(cells, (a, b) -> a[0] - b[0]);

        // ─────────────────────────────────────────
        // STEP 3: active[][] — tracks which cells have
        // been turned on so far. A cell can only be
        // unioned with a neighbour that is ALREADY active,
        // never with one that hasn't been reached yet.
        // ─────────────────────────────────────────
        boolean[][] active = new boolean[n][n];

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        int startNode = 0;              // (0,0)  → 0*n + 0
        int endNode = (n - 1) * n + (n - 1); // (n-1,n-1)

        // ─────────────────────────────────────────
        // STEP 4: Activate cells in ascending order,
        // union with active neighbours, check connectivity
        // ─────────────────────────────────────────
        for (int[] cell : cells) {
            int elevation = cell[0];
            int row = cell[1];
            int col = cell[2];

            active[row][col] = true;
            int currentNode = row * n + col;

            for (int i = 0; i < 4; i++) {
                int newRow = row + dr[i];
                int newCol = col + dc[i];

                // Bounds check + must already be active —
                // this is the entire "edge is ready" condition
                // derived in Part 1: if the neighbour is active,
                // its elevation is ≤ current elevation, so THIS
                // cell's elevation is the max of that edge.
                if (newRow < 0 || newRow >= n || newCol < 0 || newCol >= n) {
                    continue;
                }
                if (!active[newRow][newCol]) {
                    continue;
                }

                int neighbourNode = newRow * n + newCol;
                ds.unionBySize(currentNode, neighbourNode);
            }

            // EARLY EXIT: the moment start and end share a root,
            // the elevation of the cell we JUST activated is
            // guaranteed to be the minimum sufficient t — because
            // one instant earlier (before this cell), they were
            // proven not connected (monotonicity, established
            // in Part 1).
            if (ds.findUParent(startNode) == ds.findUParent(endNode)) {
                return elevation;
            }
        }

        // Fallback — never actually reached. By the time the
        // highest-elevation cell is activated, the entire grid
        // is guaranteed to be one connected component, and the
        // early exit above will always have already fired.
        return -1;
    }
}
```

---

### Workflow Trace on the Example From Part 1

```
grid =
0  5  8
6  1  7
3  4  2

n = 3
startNode = 0  (cell (0,0))
endNode   = 8  (cell (2,2), since 2*3+2 = 8)

Sorted cells (elevation, row, col):
(0,0,0) (1,1,1) (2,2,2) (3,2,0) (4,2,1) (5,0,1) (6,1,0) (7,1,2) (8,0,2)

Activate (0,0,0): node=0. No active neighbours.
  find(0) vs find(8) → 0 != 4 (endNode not even active) → not connected

Activate (1,1,1): node=4. No active neighbours.
  find(0) vs find(4) → not connected

Activate (2,2,2): node=8. No active neighbours.
  find(0) vs find(8) → not connected

Activate (3,2,0): node=6. No active neighbours.
  find(0) vs find(8) → not connected

Activate (4,2,1): node=7.
  neighbour (2,0)=node6 ACTIVE → union(7,6)
  neighbour (2,2)=node8 ACTIVE → union(7,8)   [merges {6,7,8}]
  neighbour (1,1)=node4 ACTIVE → union(7,4)   [merges {4,6,7,8}]
  find(0) vs find(8) → 0 is alone, 8 is in {4,6,7,8} → NOT connected

Activate (5,0,1): node=1.
  neighbour (0,0)=node0 ACTIVE → union(1,0)   [merges {0,1}]
  neighbour (0,2) NOT active → skip
  neighbour (1,1)=node4 ACTIVE → union(1,4)
     find(1)'s root is now merged with find(4)'s root {4,6,7,8}
     → this fuses {0,1} with {4,6,7,8} → ALL ONE COMPONENT

  find(0) vs find(8) → SAME ROOT → CONNECTED!
  return elevation = 5
```

**Answer: 5** — matches the hand-trace and verification from Part 1 exactly.

---

## Complexity Analysis

### Time Complexity — O(n² log n)

Let's derive this carefully, the way we always do.

**Flattening + collecting cells:**

```
Building the cells[] array → O(n²)
```

**Sorting:**

```
n² cells, sorted by elevation → **O(n² log(n²))** = O(n² · 2 log n) = O(n² log n)
```

**Main activation loop:**

```
Outer loop → runs n² times (once per cell, worst case — early
             exit may cut this short, but worst case is the full grid)

Inner loop → checks exactly 4 neighbours per cell → O(4) = O(1)
             each check does at most 1 unionBySize() call
             → O(4α) ≈ O(1) amortized per neighbour (DSU near-constant time)

Plus 1 findUParent() comparison per cell → O(4α) ≈ O(1)

Total for activation loop: O(n²) · O(1) = O(n²)
```

```
Overall: O(n²) [build] + O(n² log n) [sort] + O(n²) [activate]
       = O(n² log n)
```

The sort dominates — same structural reasoning as Kruskal's Algorithm, where sorting the edges was the bottleneck over the near-constant-time DSU operations.

### Space Complexity — O(n²)

```
DisjointSet (parent[] + size[])   → O(n²)
cells[][] array                    → O(n²)
active[][] boolean grid            → O(n²)
─────────────────────────────
Total: O(n²)
```

---

## Comparing All Three Approaches

```
┌────────────────────────┬──────────────────────┬──────────────────────────────┐
│ Approach               │ Time Complexity      │ Core Idea                    │
├────────────────────────┼──────────────────────┼──────────────────────────────┤
│ Binary Search + BFS    │ O(n² log n)          │ Guess t, verify reachability │
│ Dijkstra (max-relax)   │ O(n² log n)          │ Minimax relaxation, like     │
│                        │                      │ LC 1631, single source/dest  │
│ DSU (this note)        │ O(n² log n)          │ Kruskal-style ascending      │
│                        │                      │ activation, minimax via      │
│                        │                      │ the cycle property           │
└────────────────────────┴──────────────────────┴──────────────────────────────┘
```

All three land at the same asymptotic complexity — which one an interviewer prefers often comes down to what topic they're probing. Since this note sits in **Pattern 11 (DSU)**, this is the version worth having cold, but being able to sketch the other two in one sentence each (as we did in Part 1 and the "Better" section) shows real command of the minimax-path family.

---

## Quick Revision Checklist

- [ ]  Why does "connectivity, once achieved, never breaks as `t` increases" matter — what specific step in the algorithm relies on it?
- [ ]  Why is activating a cell and unioning it with *already-active* neighbours equivalent to processing that cell's incident edges in a Kruskal's-style sorted-edge algorithm?
- [ ]  What is the minimax path property (cycle property) of Kruskal's Algorithm, and how does it justify why the DSU approach gives the correct answer here?
- [ ]  Why is it safe to `return` the moment `find(start) == find(end)`, rather than continuing to process the rest of the sorted cell list?
- [ ]  What are the two alternative approaches to this problem, and in one sentence each, how do they differ from the DSU approach?
- [ ]  Why does the time complexity come out to `O(n² log n)` — which single step dominates, and why?

---

That completes **LC 778. Swim in Rising Water**. Ready for the next Pattern 11 problem whenever you are.