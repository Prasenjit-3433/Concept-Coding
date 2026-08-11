# Number of Islands - II (or Online Queries)

Key Concept: DSU on Grid Cells
Problem: https://www.geeksforgeeks.org/problems/number-of-islands/1?utm_source=youtube&utm_medium=collab_striver_ytdescription&utm_campaign=number-of-islands
Solution: https://www.youtube.com/watch?v=Rn6B-Q4SNyA&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=51&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

---

**Step 1 — Which topic?**

You're given an `n × m` grid, initially all water, and a sequence of "turn this cell into land" operations. After *each* operation, you need to know how many islands currently exist. Land cells connect into a single island when they share an edge. The moment you see "count connected groups after each incremental change" — this is a **graph connectivity problem**, framed on a grid. Topic: **Graph**.

**Step 2 — Which pattern?**

Here's the detail that changes everything about this problem compared to the "Number of Islands" you already know (LC 200): in that problem, the *entire grid* is handed to you upfront, fixed, and you run one DFS/BFS pass to count components once. Here, the grid **doesn't exist yet** — it's built one cell at a time, live, and you must report the correct island count **after every single addition**, not just at the end.

> "connect components as they're added", "dynamic graph", "online queries" → **Disjoint Set Union (Pattern 11)**
> 

This is exactly the "dynamic graph" motivation from the DSU theory notes (Part 1) — the same reasoning that made DFS-per-query too slow for the "does 1 and 4 belong to the same component?" scenario. Running a fresh DFS/BFS over the whole grid after every operation would cost `O(operations × n × m)` — completely impractical. DSU answers "are these two cells already connected?" and "merge them" in near-constant time, which is exactly what a query-after-every-update problem demands.

**Step 3 — Which key concept?**

**`DSU on Grid Cells` — Encode (row, col) as a Single Integer Node, Track a Running Island Count**

Two new pieces layer on top of everything you already know about DSU:

1. **Grid-to-node encoding.** DSU only understands integers. A grid cell is `(row, col)` — two numbers, not one. You need a formula that converts `(row, col)` into a single unique integer, so it can be used as a DSU node index — exactly the same "bridge" idea from Accounts Merge (there it was a HashMap bridging strings to integers; here it's arithmetic bridging coordinates to integers).
2. **A running counter, not a final count.** Every previous DSU problem asked one question at the very end ("how many components total?", "are these two connected?"). Here you maintain a live counter that goes up by 1 every time you place a brand-new island, and down by 1 every time that placement causes a merge with an *already-existing, different* island — and you report this counter's value after **every single operation**.

# Stage 2: Intuition Building

---

### The Core Question

Before touching any code, ask:

> *"When I drop a new piece of land onto the grid, what happens to the island count?"*
> 

Think about it in two stages, exactly the way Striver frames it:

**Stage A — assume the new cell is completely alone.** The moment you place land at `(row, col)`, if it turned out to have zero land neighbors, it would be a brand-new island all by itself. So the very first thing every placement does, unconditionally, is: **`count += 1`** — because in the worst case (fully isolated), that's the correct answer.

**Stage B — now check reality.** Look at all 4 neighbors (up, down, left, right). For each one that is *already land* and belongs to a *different* island than the current cell:

- Merging them means what were two separate islands become one.
- That's a **reduction of exactly 1** to the count — because two "islands" collapsed into one.

So the update rule per new placement is beautifully simple:

```
┌────────────────────────────────────────────────────────────────┐
│  count += 1                       (assume: brand new island)         │
│                                                                      │
│  for each of the 4 neighbours that is land:                          │
│      if neighbour's ultimate parent != current cell's parent:        │
│          union them                                                  │
│          count -= 1        (two islands just became one)             │
│      else:                                                           │
│          do nothing        (already the same island — a new          │
│                              edge inside an existing island          │
│                              changes nothing about the COUNT)        │
└────────────────────────────────────────────────────────────────┘
```

This is exactly why a single placement can reduce the count by **more than 1** — if a new cell has land on *both* its left and its right, and those two sides currently belong to two genuinely different islands, this one placement bridges them, and you pay two separate `-1` reductions in the same step. This is the single most important behavior to internalize before writing any code.

---

### Why You Must Re-Check the Root *After Each Union*, Not Before All of Them

This is a subtlety that's easy to get wrong. Suppose a new cell has three land neighbors, and two of those neighbors already belong to the *same* island as each other, while the third is a genuinely separate island. If you computed "which neighbors are in a different component" **all up front**, before doing any merging, you might double-count — or under-count.

The safe way: process neighbors **one at a time**, and for each one, ask *"is this neighbor's current ultimate parent different from my current ultimate parent, right now, at this exact moment?"* — because your own ultimate parent might have *just changed* two lines ago, from merging with a previous neighbor. You'll see this exact scenario play out in the trace below.

---

### Building the Node Encoding Formula

A grid cell doesn't come with a ready-made integer ID — you build one. Here's the derivation, the same way Striver reasons about it:

> *"If my grid has `m` columns, how many cells does one full row consume before I even reach row `r`?"*
> 

Every row has exactly `m` cells. So by the time you reach row `r` (0-indexed), rows `0` through `r-1` have already consumed `r × m` cells. The cell at `(r, c)` is then simply `c` positions further into row `r`:

```
nodeId(row, col) = row × m + col
```

```
┌────────────────────────────────────────────────────────────────┐
│  Why "row × m", not "row × n"?                                       │
│                                                                      │
│  m = number of COLUMNS = how many cells are in ONE row.              │
│  So "row × m" correctly counts "how many full rows have              │
│  already been consumed" in terms of cell count.                      │
│                                                                      │
│  This is exactly the same flattening trick used any time a           │
│  2D structure needs to become 1D for an array-based data             │
│  structure — DSU here, but the identical idea shows up               │
│  whenever you flatten a grid into a single index.                    │
└────────────────────────────────────────────────────────────────┘
```

With `n = 4` rows and `m = 5` columns (Striver's dimensions), cell `(2, 1)` becomes `2×5 + 1 = 11` — matching exactly what the transcript derives by hand.

---

### Why a `visited[]` Array Is Still Needed

DSU's `parent[]` array technically already tells you "has this cell been unioned with something," but there's a subtler question: *"has this cell been turned into land at all?"* A cell that's still water has never been assigned a meaningful parent relationship yet, and if the same `(row, col)` shows up twice in the operator list, placing land there a second time must do **nothing** — no new island, no merge, just report the current count unchanged. A dedicated `visited[]` (or equivalently, a `land[]` boolean grid) is the cleanest way to catch this duplicate case before doing any DSU work at all.

```
┌────────────────────────────────────────────────────────────────┐
│  If the current operator's cell is ALREADY visited:                  │
│      → do nothing at all — not even look at neighbours               │
│      → just record the CURRENT count as this operation's             │
│        answer (unchanged from before)                                │
└────────────────────────────────────────────────────────────────┘
```

---

### The Delta-Array Technique for 4-Directional Neighbors

Same technique as LC 1091 and LC 1631 — generate neighbors without hand-writing four separate `if` blocks:

```
dr = {-1, 1, 0, 0}
dc = {0, 0, -1, 1}
```

Each index gives one of the four directions: up, down, left, right.

---

### Walking Through a Clean, Verifiable Example

Grid: `n = 3` rows, `m = 3` columns (node id = `row × 3 + col`, ids `0` to `8`).

Operators, in order: `(0,0)`, `(0,2)`, `(0,1)`, `(0,0)`, `(1,1)`, `(2,2)`, `(1,2)`

```
INITIAL STATE — every cell is water, count = 0
```

**Operation 1 — `(0,0)`**

```
Not visited → mark visited, count += 1 → count = 1
Neighbours of (0,0): up=invalid, down=(1,0)=water, left=invalid, right=(0,1)=water
No land neighbours → nothing to merge
```

**Answer: 1**

**Operation 2 — `(0,2)`**

```
Not visited → mark visited, count += 1 → count = 2
Neighbours: up=invalid, down=(1,2)=water, left=(0,1)=water, right=invalid
No land neighbours → nothing to merge
```

**Answer: 2**

**Operation 3 — `(0,1)`** — the bridge

```
Not visited → mark visited, count += 1 → count = 3
Neighbours: up=invalid, down=(1,1)=water, left=(0,0)=LAND, right=(0,2)=LAND

Check left (0,0): findUParent(0,1) = itself, findUParent(0,0) = 0 → DIFFERENT
   → union them, count -= 1 → count = 2

Check right (0,2): findUParent(0,1) = [now merged with (0,0)'s root],
                    findUParent(0,2) = itself → STILL DIFFERENT
   → union them, count -= 1 → count = 1
```

**Answer: 1** — one placement, two merges, exactly the "bridge two separate islands" behavior from the transcript's `(1,0)` step.

**Operation 4 — `(0,0)`** — duplicate

```
ALREADY visited → skip everything → report current count unchanged
```

**Answer: 1**

**Operation 5 — `(1,1)`**

```
Not visited → mark visited, count += 1 → count = 2
Neighbours: up=(0,1)=LAND (part of the big merged island), down=(2,1)=water,
            left=(1,0)=water, right=(1,2)=water

Check up (0,1): different root → union, count -= 1 → count = 1
```

**Answer: 1**

**Operation 6 — `(2,2)`**

```
Not visited → mark visited, count += 1 → count = 2
Neighbours: up=(1,2)=water, down=invalid, left=(2,1)=water, right=invalid
No land neighbours → nothing to merge
```

**Answer: 2**

**Operation 7 — `(1,2)`** — three land neighbors, one already-merged

```
Not visited → mark visited, count += 1 → count = 3
Neighbours: up=(0,2)=LAND (big island), down=(2,2)=LAND (its own separate island),
            left=(1,1)=LAND (big island — same root as up)

Check up (0,2): different root (big island) → union, count -= 1 → count = 2
Check down (2,2): different root (its own island) → union, count -= 1 → count = 1
Check left (1,1): findUParent NOW equals the big island's root, because the
                   "up" merge one line ago already folded (1,1) into it
                   → SAME root → do nothing, no decrement
```

**Answer: 1**

This last operation is the exact scenario flagged above: if you had captured "is this different?" for all three neighbors *before* doing any merging, you'd have wrongly decremented three times instead of two. Checking fresh, one neighbor at a time, is what keeps this correct.

**Final answer array:** `[1, 2, 1, 1, 1, 2, 1]`

---

### The Key Insight to Carry

```
┌────────────────────────────────────────────────────────────────┐
│  NUMBER OF ISLANDS II — the pattern                                  │
│                                                                      │
│  Nodes for DSU   : flattened grid cells, id = row × m + col          │
│  visited[]       : catches duplicate operators — skip entirely       │
│  Per placement   :                                                   │
│      count += 1                        (assume brand new)            │
│      for each of 4 neighbours that is land:                          │
│          if different root: union, count -= 1                        │
│          (check root FRESH each time — an earlier merge in           │
│           this same step can silently make a later neighbour         │
│           already-connected)                                         │
│      record count as this operation's answer                         │
│                                                                      │
│  Why DSU specifically: the grid is DYNAMIC — built one cell          │
│  at a time — and you need a correct connectivity answer              │
│  after EVERY step, not just at the end. Same motivation as           │
│  DSU Part 1's "dynamic graph" argument, applied to a grid.           │
└────────────────────────────────────────────────────────────────┘
```

# Stage 3: Code

---

### The Complete Mental Workflow

```
1. Total cells = n × m. Initialize DisjointSet(n × m).
2. visited[] boolean array, size n × m, all false — tracks "is this cell land yet?"
3. count = 0, answer list to collect results

4. For each operator (row, col) in operators:
   a. cellId = row × m + col
   b. If visited[cellId] is already true:
        → append current count to answer, continue to next operator
   c. Otherwise:
        → mark visited[cellId] = true
        → count += 1                       (assume brand new island)
        → for each of the 4 directions (dr[]/dc[]):
              newRow = row + dr[i], newCol = col + dc[i]
              if in bounds AND visited[newRow][newCol] is true (it's land):
                  adjId = newRow × m + newCol
                  if findUParent(cellId) != findUParent(adjId):
                      union(cellId, adjId)
                      count -= 1
        → append count to answer

5. Return answer
```

---

### Full Java Implementation

```java
class DisjointSet {
    private List<Integer> parent = new ArrayList<>();
    private List<Integer> rank = new ArrayList<>();
    
    public DisjointSet(int n) {
        for(int i=0; i<=n; i++) {
            parent.add(i);
            rank.add(0);
        }
    }
    
    public int findUltParent(int node) {
        // base
        if (node == parent.get(node)) return node;
        
        int ultParent = findUltParent(parent.get(node));
        parent.set(node, ultParent);
        return parent.get(node);
    }
    
    public void unionByRank(int u, int v) {
        int ultParentU = findUltParent(u);
        int ultParentV = findUltParent(v);
        
        if (findUltParent(u) == findUltParent(v)) return;
        
        if (rank.get(ultParentU) < rank.get(ultParentV)) {
            parent.set(ultParentU, ultParentV);
        } else if (rank.get(ultParentV) < rank.get(ultParentU)){
            parent.set(ultParentV, ultParentU);
        } else {
            parent.set(ultParentU, ultParentV);
            rank.set(ultParentV, rank.get(ultParentV) + 1);
        }
    }
}

class Solution {
    
    private boolean isLand(int i, int j, boolean[][] visited) {
        int m = visited.length, n = visited[0].length;
        
        return (0 <= i && i < m) &&
        (0 <= j && j < n) &&
        (visited[i][j]);
    }

    public List<Integer> numOfIslands(int rows, int cols, int[][] operators) {
        boolean[][] visited = new boolean[rows][cols];
        
        DisjointSet dsu = new DisjointSet((rows * cols)-1);
        int count = 0;
        int[][] dir = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        List<Integer> ans = new ArrayList<>();
        
        
        for(int[] cell : operators) {
            int currX = cell[0];
            int currY = cell[1];
            int currNode = currX * cols + currY;
            
            // already marked the cell as land
            if (visited[currX][currY]) {
                ans.add(count);
                continue;
            }
            
            visited[currX][currY] = true;
            count++; 
            
            for(int i=0; i<4; i++) {
                int adjX = currX + dir[i][0];
                int adjY = currY + dir[i][1];
                int adjNode = adjX * cols + adjY;
                
                if (isLand(adjX, adjY, visited)) {
                    if (dsu.findUltParent(currNode) != dsu.findUltParent(adjNode)) {
                        dsu.unionByRank(currNode, adjNode);
                        count--;
                    }
                }
            }
            
            ans.add(count);
        }
        
        return ans;
    }
}
```

---

### Dry Run Confirmation (Matches Part 1 Exactly)

```
n=3, m=3, node id = row*3+col

Op (0,0): not visited → count=1, no land neighbours → ans=1
Op (0,2): not visited → count=2, no land neighbours → ans=2
Op (0,1): not visited → count=3
   left (0,0)=id0: findUParent(1)=1, findUParent(0)=0 → DIFFERENT → union, count=2
   right(0,2)=id2: findUParent(1)=[merged root], findUParent(2)=2 → DIFFERENT → union, count=1
   → ans=1
Op (0,0): ALREADY VISITED → ans=1 (unchanged)
Op (1,1): not visited → count=2
   up (0,1)=id1: different root (big island) → union, count=1
   → ans=1
Op (2,2): not visited → count=2, no land neighbours → ans=2
Op (1,2): not visited → count=3
   up   (0,2)=id2: different root → union, count=2
   down (2,2)=id8: different root → union, count=1
   left (1,1)=id4: SAME root now (just merged one line above) → no change
   → ans=1

FINAL: [1, 2, 1, 1, 1, 2, 1]  ✓ matches the Part 1 trace exactly
```

---

## Complexity Analysis

### Time Complexity — O(Q · 4α) ≈ O(Q)

Let `Q` = number of operators, and `4α` = the near-constant amortized cost per DSU operation (established in the DSU theory notes, Part 4).

```
For each of the Q operators:
  → 1 visited[] check                        → O(1)
  → up to 4 neighbour checks:
      each does 1-2 findUParent() calls       → O(4α) each
      possibly 1 unionBySize() call           → O(4α)

Per operator: O(4 × 4α) = O(4α)  (constant factor absorbed)

Total: O(Q · 4α) ≈ O(Q)
```

Compare this to the brute-force alternative — running a fresh DFS/BFS over the entire `n × m` grid after every single operator — which would cost `O(Q × n × m)`. For a grid with even a few hundred cells and a few hundred operators, that difference is the entire reason DSU is the correct tool here, not just a convenient one.

### Space Complexity — O(n · m)

```
DisjointSet (parent[] + size[])   → O(n·m)
visited[] array                    → O(n·m)
answer list                        → O(Q)
─────────────────────────────
Total: O(n·m + Q)
```

---

## Quick Revision Checklist

- [ ]  Why does `count += 1` happen *unconditionally* at the start of processing every new cell, before any neighbour is even checked?
- [ ]  Why must the root comparison for each neighbour be done *fresh*, rather than computed once before checking all 4 neighbours — walk through the exact scenario in the trace where this matters.
- [ ]  What is the derivation for `nodeId = row × m + col` — why `m` (columns) and not `n` (rows)?
- [ ]  Why is a separate `visited[]` array needed here, when DSU's `parent[]` already tracks component membership?
- [ ]  What is the time complexity difference between this DSU approach and re-running DFS/BFS after every operator — and why does that difference matter for this specific "online query" framing?
- [ ]  Can a single placement ever reduce the count by more than 2 in a standard 4-directional grid? Why or why not (think about the maximum number of *distinct* land neighbours a cell can have)?

---

That completes **Number of Islands II**. Ready for the next DSU problem whenever you are — LC 947 (Most Stones Removed), LC 924 (Minimize Malware Spread), or wherever your sequence goes next.