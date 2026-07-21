# Floyd-Warshall Algorithm

Solution: https://www.youtube.com/watch?v=YbY8cVwWAvw&ab_channel=takeUforward
Status: Done

# 🌐 **Floyd-Warshall Algorithm**

---

### What is Floyd-Warshall — and Why Is It Different From Everything Before?

Dijkstra and Bellman-Ford are both **single-source** shortest path algorithms — you're given one starting node, and you find the shortest distance from *that one node* to every other node.

Floyd-Warshall solves a fundamentally different question. Imagine a graph with 5 nodes: 0, 1, 2, 3, 4. Someone can ask you *"what's the shortest path from 4 to 1?"* Then someone else asks *"what's the shortest path from 1 to 3?"* You need an answer for **every possible source, to every possible destination** — not just one fixed source.

```
┌────────────────────────────────────────────────────────────────────┐
│  Floyd-Warshall = **MULTI-SOURCE** shortest path algorithm         │
│                                                                    │
│  Given: a graph with n nodes                                       │
│  Find:  shortest distance between EVERY pair of nodes (i, j)       │
│                                                                    │
│  Bonus: it also detects negative cycles, same as Bellman-Ford      │
└────────────────────────────────────────────────────────────────────┘
```

The **shortest path** definition itself doesn't change — it's still "the path between two nodes that has the least total edge weight." What changes is the scope: you want this answer for *all pairs at once*, not one source at a time.

---

## The Core Idea — `"Go Via Every Vertex"`

### Building the Intuition With a Concrete Example

Take this graph:

![image.png](Floyd-Warshall%20Algorithm/image.png)

Suppose you want the shortest distance from **node 0 to node 1**. What are your options?

**Option 1 — go directly.** There's a direct edge `0 → 1` costing **6**.

**Option 2 — go via node 2.** That means: first reach 2, then go from 2 to 1.

```
0 → 2 → 1  =  2 + 3  =  5
```

**Option 3 — go via node 3.**

```
0 → 3 → 1  =  4 + 1  =  5
```

**Option 4 — go via node 4.** Here's the subtlety: there's no *direct* edge `0 → 4`. But suppose someone already computed, in an earlier stage, that the shortest way to get from 0 to 4 is **3** (via some other route, say through 2). You don't need to re-derive that — you just *use* it:

```
0 → 4 → 1  =  3 + 1  =  4
```

Comparing all four options — 6, 5, 5, 4 — the minimum is **4**, via node 4.

![image.png](Floyd-Warshall%20Algorithm/image%201.png)

```
┌────────────────────────────────────────────────────────────────────┐
│  The strategy: for every pair (i, j), try routing THROUGH          │
│  every other vertex k, one at a time, and keep whichever           │
│  route gives the smallest total cost.                              │
│                                                                    │
│  dist(i, j) = minimum over every possible via-vertex k of:         │
│               dist(i, k) + dist(k, j)                              │
│               ...compared against the direct dist(i, j)            │
└────────────────────────────────────────────────────────────────────┘
```

### Why This Smells Like Dynamic Programming

Notice something important in Option 4 above: the cost of `0 → 4` (which is 3) was **not** a direct edge — it was itself something computed earlier, by routing through node 2, and that result was *reused* without recomputation.

```
┌────────────────────────────────────────────────────────────────────┐
│  This is the DP flavor of Floyd-Warshall:                          │
│                                                                    │
│  You don't need to know HOW dist(0,4) = 3 was derived.             │
│  You just trust that it was already computed and stored,           │
│  and you build on top of it.                                       │
│                                                                    │
│  "Using something already precomputed to build the answer"         │
│  is the exact signature of a DP-style algorithm.                   │
└────────────────────────────────────────────────────────────────────┘
```

You don't need to *know* dynamic programming as a topic to follow this — just recognize the pattern: earlier results feed into later ones.

---

## Setting Up — The Adjacency Matrix

Unlike Dijkstra and Bellman-Ford (which used adjacency lists / flat edge lists), Floyd-Warshall is naturally expressed using an **adjacency matrix**, because you need direct O(1) lookup of "what's the current best distance between any i and j" at every step.

### Building the Cost Matrix

Rule 1: **distance from any node to itself is always 0.**

```
cost[i][i] = 0   for every node i
```

Rule 2: for every directed edge `(u, v, wt)` in the graph, set `cost[u][v] = wt`. **Do not** set `cost[v][u]` — this is a directed graph, and only the given direction is valid.

Rule 3: everything else starts as **infinity** — meaning "unreachable, as of now."

### Handling Undirected Graphs

If you're given an undirected edge `(u, v, wt)`, split it into two directed edges with the **same weight**, going opposite ways:

```
u v wt
v u wt
```

This is the identical adaptation trick used for Bellman-Ford — Floyd-Warshall only operates cleanly when every edge has an explicit direction.

---

## The Formula

Let `i` and `j` be any two nodes, and let `k` be the vertex you are currently "trying as a via-point." The update rule is:

```
cost[i][j] = min( cost[i][j],  cost[i][k] + cost[k][j] )
```

Read this as: *"Is my current best-known distance from i to j still the best? Or does routing through k, using i-to-k's best-known distance plus k-to-j's best-known distance, beat it?"*

You do this for **every single via-vertex k**, one at a time, sweeping over **every pair (i, j)** for each k. Once you've tried every k from `0` to `n-1`, the matrix holds the true shortest distance between every pair.

![image.png](Floyd-Warshall%20Algorithm/image%202.png)

```
┌────────────────────────────────────────────────────────────────────┐
│  for k = 0 to n-1:            ← try every vertex as a via-point    │
│      for i = 0 to n-1:                                             │
│          for j = 0 to n-1:                                         │
│              cost[i][j] = min(cost[i][j], cost[i][k]+cost[k][j])   │
│                                                                    │
│  Three nested loops → O(n³)                                        │
└────────────────────────────────────────────────────────────────────┘
```

An important note on **why the row/column of k itself never changes** during the k-th pass: if the destination `j` equals `k`, then `cost[i][k] + cost[k][k] = cost[i][k] + 0 = cost[i][k]` — no improvement is possible over what's already there, so that entry is effectively just copied. Same logic when `i` equals `k`. This is why, when you trace through it, the row and column corresponding to the current via-vertex `k` always stay exactly as they were entering that pass.

---

## Walking Through the Algorithm — A Full Trace

Now let's build the complete matrix, step by step, using this graph (nodes 0, 1, 2, 3):

![image.png](Floyd-Warshall%20Algorithm/image%203.png)

### Initial Cost Matrix

![image.png](Floyd-Warshall%20Algorithm/image%204.png)

Diagonal = 0. Direct edges filled in. Everything else = ∞.

```
        j=0    j=1    j=2    j=3
i=0  [   0      2      ∞      ∞   ]
i=1  [   1      0      3      ∞   ]
i=2  [   ∞      ∞      0      ∞   ]
i=3  [   3      5      4      0   ]
```

Notice node 2 has no outgoing edges at all — its entire row (besides the diagonal) is infinity, and it will stay that way forever. No via-vertex can ever create an edge that doesn't exist.

---

### Pass 1 — Via Vertex k = 0

![image.png](Floyd-Warshall%20Algorithm/image%205.png)

For every `(i,j)`, check: `cost[i][j] = min(cost[i][j], cost[i][0] + cost[0][j])`

Row 0 and column 0 stay unchanged (as explained above — no improvement possible when i or j equals k).

```
i=1: cost[1][0] = 1
   j=1: min(0, 1+2=3) = 0        (no change)
   j=2: min(3, 1+∞)  = 3         (no change)
   j=3: min(∞, 1+∞)  = ∞         (no change)

i=2: cost[2][0] = ∞ → adding ∞ can't improve anything → row 2 unchanged

i=3: cost[3][0] = 3
   j=1: min(5, 3+2=5) = 5         (no change — tie, not strictly better)
   j=2: min(4, 3+∞)  = 4          (no change)
   j=3: min(0, 3+∞)  = 0          (no change)
```

**After via 0 — matrix unchanged:**

```
        j=0    j=1    j=2    j=3
i=0  [   0      2      ∞      ∞   ]
i=1  [   1      0      3      ∞   ]
i=2  [   ∞      ∞      0      ∞   ]
i=3  [   3      5      4      0   ]
```

---

### Pass 2 — Via Vertex k = 1

![image.png](Floyd-Warshall%20Algorithm/image%206.png)

Row 1 and column 1 stay unchanged.

```
i=0: cost[0][1] = 2
   j=0: min(0, 2+1=3) = 0         (no change)
   j=2: min(∞, 2+3=5) = 5         ← IMPROVED! (was unreachable, now 5)
   j=3: min(∞, 2+∞)  = ∞          (no change)

i=2: cost[2][1] = ∞ → row 2 unchanged

i=3: cost[3][1] = 5
   j=0: min(3, 5+1=6) = 3         (no change)
   j=2: min(4, 5+3=8) = 4         (no change)
   j=3: min(0, 5+∞)  = 0          (no change)
```

**After via 1:**

```
        j=0    j=1    j=2    j=3
i=0  [   0      2      5      ∞   ]
i=1  [   1      0      3      ∞   ]
i=2  [   ∞      ∞      0      ∞   ]
i=3  [   3      5      4      0   ]
```

`cost[0][2]` just went from unreachable to **5** — this is the path `0 → 1 → 2` (cost `2+3=5`), discovered because node 1 was tried as a via-point.

---

### Pass 3 — Via Vertex k = 2

Row 2 and column 2 stay unchanged.

```
cost[2][j] is ∞ for every j (node 2 has no outgoing edges)
→ adding ∞ to anything can never improve a distance
→ NOTHING changes in this pass
```

**After via 2 — matrix unchanged:**

```
        j=0    j=1    j=2    j=3
i=0  [   0      2      5      ∞   ]
i=1  [   1      0      3      ∞   ]
i=2  [   ∞      ∞      0      ∞   ]
i=3  [   3      5      4      0   ]
```

---

### Pass 4 — Via Vertex k = 3

Row 3 and column 3 stay unchanged.

```
cost[i][3] is ∞ for i = 0, 1, 2 (nobody can reach node 3 — it only has
outgoing edges, never incoming)
→ adding ∞ can never improve anything for rows 0, 1, 2
→ NOTHING changes in this pass either
```

**Final matrix — after all 4 passes:**

```
        j=0    j=1    j=2    j=3
i=0  [   0      2      5      ∞   ]
i=1  [   1      0      3      ∞   ]
i=2  [   ∞      ∞      0      ∞   ]
i=3  [   3      5      4      0   ]
```

This is now the **shortest distance between every pair of nodes** in the graph. If someone asks "what's the shortest path from 3 to 0?" — you look up `cost[3][0] = 3`, done. No recomputation needed, ever, for any pair.

```
┌────────────────────────────────────────────────────────────────────┐
│  What just happened, structurally:                                 │
│                                                                    │
│  Pass via k=0 → tried routing everything through node 0            │
│  Pass via k=1 → tried routing everything through node 1            │
│                  (this is where 0→2 improved, via 0→1→2)           │
│  Pass via k=2 → node 2 has no outgoing edges → no improvement      │
│  Pass via k=3 → node 3 has no incoming edges → no improvement      │
│                                                                    │
│  After trying EVERY vertex as a via-point, the matrix is           │
│  guaranteed to hold the true shortest distance for every pair.     │
└────────────────────────────────────────────────────────────────────┘
```

![image.png](Floyd-Warshall%20Algorithm/image%207.png)

---

## Detecting Negative Cycles

![image.png](Floyd-Warshall%20Algorithm/image%208.png)

Think about what a negative cycle does structurally. Suppose there's a cycle `0 → 1 → 2 → 0` with weights `-2, -3, +2` — total cycle weight `-3`.

If Floyd-Warshall ever finds a route that goes all the way around this cycle and back to where it started, the cost of going from node 0 **to itself** would compute as something *less than 0* — a negative number, even though the "trivial" cost of staying at a node is always 0.

```
┌────────────────────────────────────────────────────────────────┐
│  Every diagonal entry cost[i][i] should always be 0 —          │
│  the cost of going from a node to itself.                      │
│                                                                │
│  After running Floyd-Warshall, check every diagonal entry:     │
│                                                                │
│  if cost[i][i] < 0 for ANY node i  →  NEGATIVE CYCLE EXISTS    │
│                                                                │
│  Why: the only way a node-to-itself cost can drop below 0      │
│  is if some cycle reachable from i (and looping back to i)     │
│  has a total negative weight — Floyd-Warshall's minimization   │
│  would have discovered and applied that reduction.             │
└────────────────────────────────────────────────────────────────┘
```

If a negative cycle exists, "shortest path" stops being a meaningful concept for any pair that can reach that cycle — you could loop through it endlessly and keep reducing the cost forever. So detecting this is important before trusting any of the matrix's values.

---

## Complete Java Implementation

The LeetCode/GFG-style version of this problem gives you the matrix directly (with `-1` meaning "no edge," instead of infinity), and asks you to update it **in-place**.

```java
class Solution {
    public int[][] floydWarshall(int[][] edges, int V) {

        // ─────────────────────────────────────────
        // STEP 1: Build the adjacency matrix from
        // the given edge list.
        //
        // Default every cell to infinity ("unreachable"),
        // except the diagonal — cost of reaching yourself
        // is always 0, regardless of what edges exist.
        // ─────────────────────────────────────────
        int[][] matrix = new int[V][V];

        for (int i = 0; i < V; i++) {
            Arrays.fill(matrix[i], (int) 1e9);
            matrix[i][i] = 0;
        }

        // Populate direct edges. If the same edge (u,v)
        // appears more than once in the input, keep the
        // cheaper one — don't blindly overwrite, since a
        // worse duplicate could corrupt an already-correct
        // direct edge weight.
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            int wt = edge[2];

            matrix[u][v] = Math.min(matrix[u][v], wt);
            // NOTE: do NOT set matrix[v][u] here — this is a
            // directed edge. If the graph is meant to be
            // undirected, the caller is expected to supply
            // both (u,v,wt) and (v,u,wt) in `edges`, exactly
            // like the Bellman-Ford convention.
        }

        // ─────────────────────────────────────────
        // STEP 2: The core Floyd-Warshall triple loop
        //
        // k is outermost — each full pass over i and j
        // must see every via-vertex from 0 to k-1 already
        // fully folded into the matrix before k itself
        // is tried. That ordering is only guaranteed when
        // k drives the outermost loop.
        // ─────────────────────────────────────────
        for (int k = 0; k < V; k++) {
            for (int i = 0; i < V; i++) {

                // Small but real optimization: if i can't
                // even reach k, routing through k is pointless
                // for this entire row — skip early.
                if (matrix[i][k] == (int) 1e9) continue;

                for (int j = 0; j < V; j++) {
                    if (matrix[k][j] == (int) 1e9) continue;

                    if (matrix[i][k] + matrix[k][j] < matrix[i][j]) {
                        matrix[i][j] = matrix[i][k] + matrix[k][j];
                    }
                }
            }
        }

        // ─────────────────────────────────────────
        // STEP 3: Negative cycle check — MANDATORY here,
        // since the problem gives no guarantee against it.
        //
        // If cost[i][i] has dropped below 0 for ANY node,
        // some negative cycle reachable from (and looping
        // back to) i has corrupted the distances. The entire
        // matrix is untrustworthy at that point — not just
        // the affected row/column, because that negative
        // cycle could have been used as a via-path to
        // artificially shrink OTHER pairs' distances too.
        // ─────────────────────────────────────────
        for (int i = 0; i < V; i++) {
            if (matrix[i][i] < 0) {
                return null;   // signal: negative cycle exists,
                                // result is not meaningful
            }
        }

        // ─────────────────────────────────────────
        // STEP 4: Restore "unreachable" entries.
        // Any cell still at (int)1e9 was never connected
        // through any via-vertex → convert back to -1,
        // matching the standard "no path" convention.
        // ─────────────────────────────────────────
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (matrix[i][j] == (int) 1e9) {
                    matrix[i][j] = -1;
                }
            }
        }

        return matrix;
    }
}
```

---

## Complexity Analysis

### Time Complexity — O(n³)

```
┌────────────────────────────────────────────────────────────────┐
│  Three nested loops, each running exactly n times:             │
│                                                                │
│  for k in 0..n-1:      → O(n)                                  │
│    for i in 0..n-1:    → O(n)                                  │
│      for j in 0..n-1:  → O(n)                                  │
│        O(1) work inside                                        │
│                                                                │
│  Total: O(n) × O(n) × O(n) = O(n³)                             │
└────────────────────────────────────────────────────────────────┘
```

### Space Complexity — O(n²)

Even though many implementations update the given matrix **in-place** (and technically use no *extra* allocated array), the space complexity is still counted as `O(n²)` — because that's the amount of space genuinely required to *hold* the problem's data and solve it. Whether the matrix was handed to you or you allocated it yourself doesn't change how much space the algorithm fundamentally needs.

```
Cost matrix → O(n²)
─────────────────────
Total: O(n²)
```

---

## Floyd-Warshall vs Running Dijkstra From Every Node

A natural question: instead of Floyd-Warshall, could you just run Dijkstra once from every single node, and get all-pairs shortest paths that way?

```
Dijkstra from every node:  n × O(E log V)  =  O(V · E log V)
Floyd-Warshall:             O(V³)
```

For sparse graphs, `V · E log V` can indeed be smaller than `V³`. So **yes, running Dijkstra from every node is a valid, sometimes faster, alternative** — but with one critical catch:

```
┌────────────────────────────────────────────────────────────────┐
│  Dijkstra-from-every-node FAILS the moment the graph has       │
│  negative edges — same fundamental limitation as always.       │
│                                                                │
│  Floyd-Warshall works correctly with negative edges (as long   │
│  as there's no negative cycle), AND it detects negative        │
│  cycles for free.                                              │
│                                                                │
│  So the choice isn't purely about speed — it's about whether   │
│  negative weights are possible in your graph at all.           │
└────────────────────────────────────────────────────────────────┘
```

Mentioning this tradeoff unprompted in an interview — not just reciting the O(n³) formula — is exactly the kind of detail that signals real understanding rather than memorization.

---

## The Complete Three-Algorithm Comparison

```
┌───────────────────┬────────────────────┬─────────────────────┬──────────────────────┐
│ **Aspect**        │ **Dijkstra**       │ **Bellman-Ford**    │ **Floyd-Warshall**   │
├───────────────────┼────────────────────┼─────────────────────┼──────────────────────┤
│ Source type       │ Single source      │ Single source       │ Multi-source         │
│ Negative edges    │ Fails              │ Works               │ Works                │
│ Negative cycles   │ Infinite loop      │ Detects them        │ Detects them         │
│ Graph type        │ Directed/Undirected│ Directed only       │ Directed/Undirected  │
│                   │ both work directly │ (convert undirected)│ (convert undirected) │
│ Core structure    │ Priority Queue     │ Flat edge list      │ Adjacency Matrix     │
│ Time Complexity   │ O(E log V)         │ O(V × E)            │ O(V³)                │
│ Space Complexity  │ O(V + E)           │ O(V + E)            │ O(V²)                │
└───────────────────┴────────────────────┴─────────────────────┴──────────────────────┘
```

---

## Quick Revision Checklist

- [ ]  Why is Floyd-Warshall called a *multi-source* algorithm, unlike Dijkstra and Bellman-Ford?
- [ ]  What is the exact update formula, and why is `k` (the via-vertex) the outermost loop?
- [ ]  Why does the row and column belonging to the current via-vertex `k` never change during that pass?
- [ ]  Why is this considered a DP-style algorithm — what specifically gets "reused" from earlier computation?
- [ ]  How do you detect a negative cycle after running Floyd-Warshall — what specific check on the matrix?
- [ ]  What tradeoff would you mention if an interviewer asks "why not just run Dijkstra from every node instead"?
- [ ]  Why is the space complexity O(n²) even when the update happens in-place on a given matrix?

---