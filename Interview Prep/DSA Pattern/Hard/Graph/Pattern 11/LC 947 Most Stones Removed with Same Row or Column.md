# LC 947. Most Stones Removed with Same Row or Column

Key Concept: Treating Rows & Cols as DSU Nodes — Not the grid cells
Problem: https://leetcode.com/problems/most-stones-removed-with-same-row-or-column/description/
Solution: https://www.youtube.com/watch?v=OwMNX8SPavM&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=53&ab_channel=takeUforward
Status: Done

# Stage 1: Identification

---

**Step 1 — Which topic?**

You're given stones at `(row, col)` positions on a plane. A stone can be removed if — even after other removals — it *still* shares a row or column with some other stone that hasn't been removed yet. You need the maximum number of stones removable. The moment you see "these things are removable *because* they share a row or column with something else," you should think: rows and columns are creating implicit connections between stones. Topic: **Graph**.

**Step 2 — Which pattern?**

Here's the reframing that unlocks the whole problem, exactly the way Striver walks it: forget "removal" for a second and just ask — *which stones can be grouped together because they're reachable from each other via shared rows/columns, directly or through a chain of other stones?* That's **connected components**. And once you can identify which stones belong to the same component, the answer falls out of a clean formula (derived below) — no removal simulation needed at all.

> "connect things sharing a property", "check if same group", "merge dynamically as you scan" → **Disjoint Set Union (Pattern 11)**
> 

**Step 3 — Which key concept?**

**`Treating Rows & Cols as DSU Nodes — Not the grid cells`**

This is the single most important **twist** in this problem, and it's easy to get backwards on a first read. The intuitive-but-wrong instinct is *"union stone A with stone B if they share a row or column."* That works, but it's needlessly complicated — you'd need to group stones by row, group stones by column, then union within each group. The elegant move Striver makes: **treat every row as a node, and every column as a node** (in the *same* DSU structure, using an index-shift trick to avoid collisions). A stone `(r, c)` doesn't need to be a node at all — it's simply the *evidence* that tells you "union row `r` with column `c`." Two stones end up in the same component automatically the moment their rows and columns chain together, with zero explicit stone-to-stone comparisons.

# Stage 2: Intuition Building

---

### The Core Question — Building the Formula From Scratch

Before any data structure, work out **what the answer even depends on**, the way Striver does with pure counting logic.

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image.png)

> *"Within one connected group of stones, how many can I actually remove?"*
> 

Think about a component with, say, 4 stones, all reachable from each other via shared rows/columns. You can keep removing stones one at a time **as long as the stone you're removing still has some other un-removed stone sharing its row or column**. Once you're down to the *very last* stone in that component, it has nobody left to share a row or column with — it's now isolated, and the removal rule says it *cannot* be removed.

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%201.png)

```
┌──────────────────────────────────────────────────────────────┐
│  Inside ANY connected component of size X:                         │
│    → You can remove X − 1 stones                                   │
│    → Exactly ONE stone must always remain — the last one           │
│      standing has nothing left to share a row/column with          │
└──────────────────────────────────────────────────────────────┘
```

This holds for a component of size 1 too — a stone with no row/column overlap with anyone else forms its own component of size 1, and `1 − 1 = 0` — correctly, you can't remove an isolated stone at all.

### Summing Across All Components

Suppose the stones split into components of sizes `X₁, X₂, X₃, ...`. Since every stone belongs to exactly one component:

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%202.png)

```
X₁ + X₂ + X₃ + ... = n     (n = total number of stones)
```

The total number of removable stones is the sum of removable stones per component:

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%203.png)

```
(X₁ − 1) + (X�2 − 1) + (X₃ − 1) + ...
```

Regroup this — pull all the `X` terms together, and all the `−1` terms together:

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%204.png)

```
= (X₁ + X₂ + X₃ + ...) − (1 + 1 + 1 + ... )
= n − (number of components)
```

The `(1 + 1 + 1 + ...)` sum has exactly one `1` per component — so it equals the **total number of components**.

```
┌──────────────────────────────────────────────────────────────┐
│  THE ENTIRE PROBLEM COLLAPSES TO ONE FORMULA:                      │
│                                                                    │
│      answer = n − (number of connected components)                 │
│                                                                    │
│  Once you can count connected components correctly, you            │
│  are completely done. No simulation of removal order               │
│  needed — the order never even affects the answer.                 │
└──────────────────────────────────────────────────────────────┘
```

This is a hugely important realization: the actual removal *process* the problem describes is a red herring for computing the answer. All that matters is the **grouping**.

---

### Why `Rows` and `Columns` Become the DSU Nodes (Not the Stones)

Now the implementation question: *how* do you compute *"number of connected components,"* given that connectivity here is defined through shared rows and columns?

You could union stone-to-stone, but there's a cleaner reframing:

> *"A stone doesn't need to be a graph node at all. What actually needs to be 'the same component' is the ROW it sits on and the COLUMN it sits on — because those are the two things that get shared across multiple stones."*
> 

Treat every distinct row index as a DSU node, and every distinct column index as a *separate* DSU node. For every stone `(r, c)`, simply **union row `r` with column `c`**. That's the entire connectivity-building step — one union call per stone, nothing more.

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%205.png)

```
┌──────────────────────────────────────────────────────────────┐
│  For every stone (r, c):  union(rowNode(r), colNode(c))            │
│                                                                    │
│  Why this automatically chains correctly:                          │
│  If stone A is at (0, 2) and stone B is at (3, 2), both            │
│  union their row with column-node(2). Column-node(2) becomes       │
│  the shared bridge — row 0 and row 3 end up in the SAME            │
│  DSU component, even though no stone directly said                 │
│  "union row 0 with row 3." The column silently did it.             │
└──────────────────────────────────────────────────────────────┘
```

### The Index-Shift Trick — Rows and Columns in One DSU

DSU nodes are just integers 0 to N-1. Rows are naturally numbered `0` to `maxRow`. But columns are *also* numbered starting from `0` — if you don't do something about it, row `2` and column `2` would collide as the exact same DSU node, which is wrong; they represent completely different things.

The fix: **shift every column index up, past the last possible row index**, so rows and columns occupy two disjoint integer ranges inside one DSU array:

```
colNode(c) = c + (maxRow + 1)
```

```
┌──────────────────────────────────────────────────────────────┐
│  If there are 5 possible rows (0 to 4):                            │
│    Row nodes occupy IDs      0, 1, 2, 3, 4                         │
│    Column nodes occupy IDs   5, 6, 7, 8, ...   (shifted by 5)      │
│                                                                    │
│  This is the exact same "flatten two different things into         │
│  one integer space" idea as Accounts Merge (email → index)         │
│  and Number of Islands II (row,col → single index) — just          │
│  a different combination rule for this specific problem.           │
└──────────────────────────────────────────────────────────────┘
```

---

### Walking Through a Concrete Example

Stones: `(0,0), (0,2), (1,3), (3,0), (3,2), (4,3)` — 6 stones total.

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%206.png)

`maxRow = 4`, `maxCol = 3` → row nodes are `0–4`, column nodes are shifted by `maxRow + 1 = 5`, so column `c` becomes node `c + 5`:

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%205.png)

```
column 0 → node 5
column 1 → node 6
column 2 → node 7
column 3 → node 8
```

**Initial DSU:** every node 0–8 is its own parent.

**Process each stone, union(row, colNode):**

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%207.png)

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%208.png)

```
Stone (0,0): union(0, 5)
   size[0]=1, size[5]=1 → equal → attach 5 under 0
   parent[5]=0, size[0]=2
```

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%209.png)

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%2010.png)

```
Stone (0,2): union(0, 7)
   findU(0)=0 (size 2), findU(7)=7 (size 1)
   size[0] ≥ size[7] → attach 7 under 0
   parent[7]=0, size[0]=3
```

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%2011.png)

```
Stone (1,3): union(1, 8)
   findU(1)=1 (size 1), findU(8)=8 (size 1) → equal → attach 8 under 1
   parent[8]=1, size[1]=2
```

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%2012.png)

```
Stone (3,0): union(3, 5)
   findU(3)=3 (size 1), findU(5)=0 (size 3, since parent[5]=0)
   size[3] < size[0] → attach 3 under 0
   parent[3]=0, size[0]=4
```

```
Stone (3,2): union(3, 7)
   findU(3)=0 (via parent[3]=0), findU(7)=0 (already under 0)
   SAME root → already connected, no union needed
   (this is exactly the "no need" moment in the transcript)
```

![image.png](LC%20947%20Most%20Stones%20Removed%20with%20Same%20Row%20or%20Column/image%2013.png)

```
Stone (4,3): union(4, 8)
   findU(4)=4 (size 1), findU(8)=1 (size 2, since parent[8]=1)
   size[4] < size[1] → attach 4 under 1
   parent[4]=1, size[1]=3
```

**Resolve every stone-touched node to its ultimate parent:**

```
Stone-touched nodes: rows {0,1,3,4}, columns {5,7,8}  (column 1/node 6 untouched — no stone in column 1)

findUParent(0) = 0
findUParent(1) = 1
findUParent(3) = 0
findUParent(4) = 1
findUParent(5) = 0
findUParent(7) = 0
findUParent(8) = 1
```

**Unique ultimate parents:** `{0, 1}` → exactly **2 components**.

```
answer = n − components = 6 − 2 = 4
```

Matches Striver's worked example exactly.

---

### Why You Only Count Ultimate Parents of *Stone-Touched* Nodes

One detail worth flagging explicitly: the DSU array technically contains a node for *every possible row and every possible column up to the max*, even ones with no stone at all sitting on them (like row 2 or column 1 in the example above — completely untouched). If you counted unique ultimate parents across the *entire* array, you'd wrongly count these empty rows/columns as extra "components" that don't correspond to any actual stone.

The fix is simple: only look at the ultimate parent of nodes that **actually correspond to a stone** — i.e., iterate over the stones themselves (or a set built while processing them), not over the full DSU array.

```
┌──────────────────────────────────────────────────────────────┐
│  Counting components = counting DISTINCT ultimate parents,         │
│  but ONLY among row/column nodes that at least one stone           │
│  actually touches. Empty rows and empty columns are DSU            │
│  array slots that exist for indexing convenience — they are        │
│  never valid components on their own.                              │
└──────────────────────────────────────────────────────────────┘
```

---

### The Key Insight to Carry

```
┌────────────────────────────────────────────────────────────────┐
│  MOST STONES REMOVED — the pattern                                   │
│                                                                      │
│  Step 1 — the math: answer = n − (number of components)              │
│           (removal simulation is a red herring; only grouping        │
│            matters, order never changes the answer)                  │
│                                                                      │
│  Step 2 — the DSU trick: ROWS and COLUMNS are the nodes,             │
│           NOT the stones. For every stone (r, c):                    │
│               union(row r, column c)                                 │
│           shared rows/columns act as bridges connecting              │
│           stones indirectly, with zero stone-to-stone unions         │
│                                                                      │
│  Step 3 — index collision fix: shift column indices by               │
│           (maxRow + 1) so rows and columns occupy disjoint           │
│           integer ranges inside one DSU array                        │
│                                                                      │
│  Step 4 — count components ONLY among stone-touched nodes,           │
│           not the entire DSU array (avoids counting empty            │
│           rows/columns as phantom components)                        │
└────────────────────────────────────────────────────────────────┘
```

# Stage 3: Coding

---

### Brute Force

> "For every pair of stones, check if they share a row or column — build an explicit stone-to-stone adjacency structure this way. Then run DFS/BFS across that adjacency structure to count connected components. Answer = n − components."
> 
- Compare every stone against every other stone → `O(n²)` pair checks
- For each connected component found via traversal, its size contributes `(size − 1)` removable stones
- Correct, but the `O(n²)` pairwise comparison is wasteful — you're explicitly discovering "shares a row" and "shares a column" as two separate relations, when a shared row or column is really just *one* underlying bridge between stones, not something that needs pairwise verification at all
- No code needed here — establishes the baseline only, and the DSU approach below removes the `O(n²)` factor entirely by never comparing stones to each other directly

---

### Better

There isn't a distinct "better-but-not-optimal" tier for this problem the way some problems have a DP-before-greedy or O(n²)-before-O(n log n) progression. The moment you recognize "shared row/column = connectivity", the natural next step **is** DSU — there's no useful intermediate approach worth coding between brute-force pairwise comparison and the DSU solution. (A DFS/BFS-based union approach, where you explicitly build a stone-to-stone graph and traverse it, is a valid alternative to DSU — but it's the same `O(n²)` edge-building cost as brute force, just with the counting done via traversal instead of pairwise removal simulation. It doesn't earn its own tier.)

---

### Optimal — DSU With Rows and Columns as Nodes

**Mental workflow before writing a single line:**

```
1. Find maxRow and maxCol across all stones — needed to size the
   DSU array and to compute the column index shift

2. Initialize DisjointSet(maxRow + maxCol + 1)
   → row nodes occupy IDs 0 to maxRow
   → column nodes occupy IDs (maxRow+1) to (maxRow+maxCol+1),
     via the shift: colNode = col + maxRow + 1

3. For every stone (r, c):
   → union(r, c + maxRow + 1)
   → this is the ONLY connectivity-building step — no stone-to-stone
     comparison ever happens

4. Count connected components:
   → loop over every node in the DSU array
   → a node is a valid, stone-touched component root if:
       it is its own parent (findUParent(node) == node)
       AND its size > 1 (it was merged with something — proof
       at least one stone touched it)
   → count how many such roots exist

5. return n − count
```

### Full Java Implementation

```java
import java.util.*;

class DisjointSet {
    List<Integer> size = new ArrayList<>();
    List<Integer> parent = new ArrayList<>();

    public DisjointSet(int n) {
        for (int i = 0; i <= n; i++) {
            size.add(1);
            parent.add(i);
        }
    }

    public int findUltPar(int node) {
        if (node == parent.get(node)) return node;

        int ultPar = findUltPar(parent.get(node));
        parent.set(node, ultPar);
        return parent.get(node);
    }

    public void unionBySize(int u, int v) {
        int ultParU = findUltPar(u);
        int ultParV = findUltPar(v);

        if (ultParU == ultParV) return;

        if (size.get(ultParU) < size.get(ultParV)) {
            parent.set(ultParU, ultParV);
            size.set(ultParV, size.get(ultParV) + size.get(ultParU));
        } else {
            parent.set(ultParV, ultParU);
            size.set(ultParU, size.get(ultParU) + size.get(ultParV));
        }
    }
}

class Solution {
    public int removeStones(int[][] stones) {
        int n = stones.length;

        // ─────────────────────────────────────────
        // STEP 1: Find the grid's effective dimensions.
        // The problem never gives grid size directly —
        // the largest row/col actually used by a stone
        // IS the dimension we care about.
        // ─────────────────────────────────────────
        int maxRow = 0;
        int maxCol = 0;

        for (int[] stone : stones) {
            maxRow = Math.max(maxRow, stone[0]);
            maxCol = Math.max(maxCol, stone[1]);
        }

        // ─────────────────────────────────────────
        // STEP 2: Rows and columns BOTH become DSU nodes,
        // sharing one array via an index shift.
        // Row nodes: 0 .. maxRow
        // Column nodes: (maxRow+1) .. (maxRow+maxCol+1)
        // ─────────────────────────────────────────
        DisjointSet ds = new DisjointSet(maxRow + maxCol + 1);

        // ─────────────────────────────────────────
        // STEP 3: For every stone, union its row with its
        // (shifted) column. This is the ENTIRE connectivity
        // step — no stone is ever compared to another stone
        // directly. Shared rows/columns silently do the
        // bridging.
        // ─────────────────────────────────────────
        for (int[] stone : stones) {
            int rowNode = stone[0];
            int colNode = stone[1] + maxRow + 1; // index shift

            // if there's a stone at (i, j), it connects
            // the i-th row with the j-th column
            ds.unionBySize(rowNode, colNode);
        }

        // ─────────────────────────────────────────
        // STEP 4: Count components — but only ones that
        // actually contain a stone.
        //
        // WHY size > 1 correctly filters this:
        // an untouched row/column node NEVER gets passed
        // into unionBySize(), so it stays its own root with
        // size still at its initial value of 1. A node that
        // ANY stone touches is guaranteed to be merged with
        // at least one other node (its paired row or column),
        // so its component's size is always >= 2.
        // ─────────────────────────────────────────
        int count = 0;
        for (int node = 0; node <= maxRow + maxCol + 1; node++) {
            if (ds.findUltPar(node) == node && ds.size.get(node) > 1) {
                count++;
            }
        }

        // ─────────────────────────────────────────
        // STEP 5: Apply the formula derived in Stage 2:
        // answer = n - (number of connected components)
        // ─────────────────────────────────────────
        return n - count;
    }
}
```

---

### Workflow Trace on the Example From Part 1

```
stones = [(0,0), (0,2), (1,3), (3,0), (3,2), (4,3)]
maxRow = 4, maxCol = 3
DisjointSet sized with n = maxRow+maxCol+1 = 8 → valid nodes 0..8

Column shift: colNode = col + 5
  col 0 → node 5, col 1 → node 6, col 2 → node 7, col 3 → node 8

UNION PHASE:
(0,0): union(0, 5)  → equal size → parent[5]=0, size[0]=2
(0,2): union(0, 7)  → size[0]=2 ≥ size[7]=1 → parent[7]=0, size[0]=3
(1,3): union(1, 8)  → equal size → parent[8]=1, size[1]=2
(3,0): union(3, 5)  → findU(5)=0(size3) > findU(3)=3(size1) → parent[3]=0, size[0]=4
(3,2): union(3, 7)  → findU(3)=0, findU(7)=0 → SAME ROOT → no-op
(4,3): union(4, 8)  → findU(8)=1(size2) > findU(4)=4(size1) → parent[4]=1, size[1]=3

COUNTING PHASE (node 0 to 8):
node 0: root, size=4 > 1 → count=1
node 1: root, size=3 > 1 → count=2
node 2: root, size=1     → skip (untouched row)
node 3, 4, 5: not roots
node 6: root, size=1     → skip (untouched column)
node 7, 8: not roots

count = 2
answer = n - count = 6 - 2 = 4   ✓
```

---

## Complexity Analysis

### Time Complexity — O(n + (maxRow + maxCol) · 4α)

```
Finding maxRow/maxCol            → O(n)

Union phase:
  n stones, each triggers 1 unionBySize() call
  → O(n · 4α) ≈ O(n)     (near-constant per DSU operation)

Counting phase:
  loops across the FULL node range (maxRow+maxCol+2 nodes),
  not just the stones — each iteration does 1 findUParent() call
  → O((maxRow + maxCol) · 4α) ≈ O(maxRow + maxCol)

Overall: O(n + maxRow + maxCol)
```

Since LeetCode's constraints keep coordinates bounded (`≤ 10⁴`) and stone count reasonable (`≤ 1000`), this comfortably passes — but note this is the one place this approach's cost is tied to the coordinate *range*, not just the stone *count*, unlike the union phase.

### Space Complexity — O(maxRow + maxCol)

```
DisjointSet (parent[] + size[])   → O(maxRow + maxCol)
─────────────────────────────
Total: O(maxRow + maxCol)
```

No separate tracking set is needed — the `size[]` array inside the DSU class does double duty as both the union-by-size mechanism *and* the "was this node ever touched by a stone" filter, which is what keeps this implementation lean compared to a version that maintains an explicit `HashSet` of touched row/column nodes alongside the union loop.