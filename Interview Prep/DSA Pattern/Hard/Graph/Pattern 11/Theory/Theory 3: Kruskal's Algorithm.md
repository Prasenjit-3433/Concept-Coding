# Kruskal's Algorithm — Minimum Spanning Tree

---

## A Quick Prerequisite Check

Everything in this note assumes the full Disjoint Set theory from the previous five parts — `findUParent` with path compression, and either `unionByRank` or `unionBySize`. Striver is explicit about this in the video: without DSU fully understood first, Kruskal's makes no sense. We'll reuse the exact `DisjointSet` class built in Part 4/5, unmodified.

---

## The Example Graph

Paste into csacademy.com (undirected, weighted):

```
1 2 2
2 6 7
6 3 8
3 4 5
4 5 9
5 1 4
1 4 1
2 4 3
2 3 3
```

6 nodes (1 to 6), 9 edges.

---

## Stage 1: Identification

**Step 1 — Which topic?**

Undirected weighted graph, find the cheapest way to connect everything → **Graph**.

**Step 2 — Which pattern?**

Same destination as Prim's — Minimum Spanning Tree, **Pattern 12**. But this time the mechanism is completely different: instead of growing outward from a single starting node, we're going to look at the **entire graph's edges up front**, sorted globally by weight.

**Step 3 — Which key concept?**

**`Sort All Edges by Weight, Then Greedily Accept Any Edge That Doesn't Create a Cycle — Using DSU to Detect Cycles Instantly`**

The single sentence that captures the whole algorithm:

> *Look at the cheapest edge in the entire graph first. If it connects two nodes that are currently in different components, take it. If it would connect two nodes already in the same component, discard it — it would only create a cycle, and cycles are useless in a tree.*

Disjoint Set is the tool that answers "are these two nodes already in the same component?" in near-constant time, which is exactly what makes this greedy strategy practical at scale.

---

## Stage 2: Intuition Building

### The Core Question

> *"If I'm allowed to pick edges in any order I like, and my goal is the cheapest possible way to connect everyone — what's the smartest order to consider edges in?"*

The answer, once you say it out loud, feels almost obvious: **consider the cheapest edges first.** If an edge is cheap and it helps connect two previously-separate parts of the graph, there's no reason to ever skip it in favor of something more expensive — using it can only help keep your total sum low.

The one thing you must guard against: **never accept an edge that connects two nodes already in the same component.** Adding such an edge doesn't improve connectivity at all — every node on both ends is already reachable from the other — it only adds unnecessary weight and, since a tree can only have `n-1` edges total, would actually make the structure invalid (a cycle, not a tree).

```
┌───────────────────────────────────────────────────────────────────┐
│  KRUSKAL'S — the entire algorithm in one breath                   │
│                                                                   │
│  1. Sort every edge in the graph by weight, ascending             │
│  2. Walk through them in that order                               │
│  3. For each edge (u, v, wt):                                     │
│       → if u and v are in DIFFERENT components: take the edge,    │
│         add wt to the sum, and MERGE their components             │
│       → if u and v are ALREADY in the same component: discard     │
│         it — it would only create a cycle                         │
│  4. Stop once you've taken n-1 edges (or just finish the list —   │
│     either way works, since nothing else will ever be accepted    │
│     past that point)                                              │
└───────────────────────────────────────────────────────────────────┘
```

### Why This Is a Genuinely Different Strategy From Prim's

Prim's grows **one connected tree outward**, node by node, always looking at edges *leaving the current tree*. Kruskal's doesn't grow a single tree at all — it looks at the graph's edges **globally**, and lets *multiple small disconnected pieces merge into each other* over time, wherever the cheapest available edge happens to connect them. Prim's never has more than one "current tree" in progress. Kruskal's can have many small fragments merging in parallel, in no particular geographic order — purely driven by weight.

This is exactly why Disjoint Set is the natural fit here and wasn't strictly needed for Prim's (Prim's just needed a `visited[]` array, since it only ever tracks "in my one tree or not"). Kruskal's needs to answer "are these two arbitrary nodes, possibly in two completely different fragments, already connected?" — and that's precisely the question DSU was built to answer instantly.

---

### Full Workflow Trace on Our Graph

**Step 1 — Sort all 9 edges by weight, ascending:**

```
1 4 1
1 2 2
2 4 3
2 3 3
5 1 4
3 4 5
2 6 7
6 3 8
4 5 9
```

**Step 2 — Initialize DSU with 6 nodes.** Every node starts as its own component.

**Step 3 — Walk the sorted list, one edge at a time:**

```
Edge (1, 4, wt=1):
  findUParent(1) = 1,  findUParent(4) = 4  → DIFFERENT
  → TAKE IT. sum = 1. union(1, 4).

Edge (1, 2, wt=2):
  findUParent(1) = 1,  findUParent(2) = 2  → DIFFERENT
  → TAKE IT. sum = 1+2 = 3. union(1, 2).

Edge (2, 4, wt=3):
  findUParent(2) = 1,  findUParent(4) = 1  → SAME COMPONENT
  → DISCARD. (both 2 and 4 already reachable from each other via 1)

Edge (2, 3, wt=3):
  findUParent(2) = 1,  findUParent(3) = 3  → DIFFERENT
  → TAKE IT. sum = 3+3 = 6. union(2, 3).

Edge (5, 1, wt=4):
  findUParent(5) = 5,  findUParent(1) = 1  → DIFFERENT
  → TAKE IT. sum = 6+4 = 10. union(5, 1).

Edge (3, 4, wt=5):
  findUParent(3) = 1,  findUParent(4) = 1  → SAME COMPONENT
  → DISCARD.

Edge (2, 6, wt=7):
  findUParent(2) = 1,  findUParent(6) = 6  → DIFFERENT
  → TAKE IT. sum = 10+7 = 17. union(2, 6).

Edge (6, 3, wt=8):
  findUParent(6) = 1,  findUParent(3) = 1  → SAME COMPONENT
  → DISCARD.

Edge (4, 5, wt=9):
  findUParent(4) = 1,  findUParent(5) = 1  → SAME COMPONENT
  → DISCARD.
```

**Result:** 5 edges taken (`1-4, 1-2, 2-3, 5-1, 2-6`), exactly `n-1 = 5` for 6 nodes.

```
MST weight = 1 + 2 + 3 + 4 + 7 = 17
```

Verify: 6 nodes, 5 edges, and every rejected edge was correctly identified as forming a cycle within an already-connected component.

---

### The Key Insight to Carry

```
┌──────────────────────────────────────────────────────────────────┐
│  Prim's:    grows ONE tree outward, edge by edge, always         │
│             looking at "what's cheap right at my current         │
│             frontier?" — needs a visited[] array.                │
│                                                                  │
│  Kruskal's: looks at ALL edges globally, sorted once, and        │
│             greedily accepts anything cheap that doesn't         │
│             create a cycle — needs Disjoint Set to answer        │
│             "would this edge create a cycle?" instantly.         │
│                                                                  │
│  Both are greedy. Both arrive at the same total MST weight       │
│  for a given graph (the MST weight is unique even when the       │
│  exact edge set achieving it might vary in case of ties).        │
│  They differ only in HOW they walk through the graph to get      │
│  there.                                                          │
└──────────────────────────────────────────────────────────────────┘
```

---

## Stage 3: Coding

The GFG-style version of this problem (which Striver solves) gives you an **adjacency list** as input, not a flat edge list — and since the graph is undirected, every edge appears **twice** in that adjacency list (once from each endpoint's perspective). We need to flatten this into a single sorted edge list first.

### Brute Force

> "Generate every possible subset of `n-1` edges from the graph, check which ones form a valid spanning tree (connected, acyclic), and take the minimum-sum one among those."

- Exponential — `C(m, n-1)` subsets to check in the worst case, and each check requires a connectivity verification
- Completely impractical. Establishes the baseline only. No code needed.

---

### Optimal — Sort Edges + DSU

**Mental workflow before writing a single line:**

```
1. Flatten the adjacency list into a flat list of (weight, u, v) triples
   → since it's undirected, each edge appears twice; this is fine —
     DSU will naturally reject the second occurrence as "same component"

2. Sort this flat list by weight, ascending

3. Initialize DisjointSet with V nodes

4. mstWeight = 0
   For each (weight, u, v) in the sorted list:
      if findUParent(u) != findUParent(v):
          mstWeight += weight
          union(u, v)          ← merge their components
      // else: discard, would create a cycle

5. Return mstWeight
```

### Full Java Implementation

```java
import java.util.*;

class Solution {

    // ─────────────────────────────────────────
    // Reused unmodified from the DSU theory notes (Part 4/5).
    // Path compression + Union by Size.
    // ─────────────────────────────────────────
    static class DisjointSet {
        List<Integer> size = new ArrayList<>();
        List<Integer> parent = new ArrayList<>();

        public DisjointSet(int n) {
            for (int i = 0; i <= n; i++) {
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

    // adj.get(u) contains int[]{v, weight} pairs — standard adjacency
    // list format, exactly like every Dijkstra/Prim's problem so far.
    static int spanningTree(int V, List<List<int[]>> adj) {

        // ─────────────────────────────────────────
        // STEP 1: Flatten the adjacency list into a flat
        // edge list of (weight, u, v) triples.
        //
        // WHY store weight FIRST in the array?
        // So that sorting this list by natural order (via a
        // Comparator on index 0) sorts by weight automatically —
        // exactly the same trick used for Dijkstra's Pair class.
        //
        // WHY is it fine that undirected edges appear twice?
        // Because when we later process the second occurrence
        // of the same edge, findUParent(u) and findUParent(v)
        // will already be equal (the first occurrence already
        // merged them) — so it gets discarded naturally. No
        // special deduplication logic needed anywhere.
        // ─────────────────────────────────────────
        List<int[]> edges = new ArrayList<>();

        for (int u = 0; u < V; u++) {
            for (int[] adjacent : adj.get(u)) {
                int v = adjacent[0];
                int wt = adjacent[1];
                edges.add(new int[]{wt, u, v});
            }
        }

        // ─────────────────────────────────────────
        // STEP 2: Sort all edges by weight, ascending
        // ─────────────────────────────────────────
        edges.sort((a, b) -> a[0] - b[0]);

        // ─────────────────────────────────────────
        // STEP 3: Initialize DSU
        // ─────────────────────────────────────────
        DisjointSet ds = new DisjointSet(V);

        // ─────────────────────────────────────────
        // STEP 4: Greedily walk the sorted edges
        // ─────────────────────────────────────────
        int mstWeight = 0;

        for (int[] edge : edges) {
            int wt = edge[0];
            int u = edge[1];
            int v = edge[2];

            // If u and v are in DIFFERENT components, taking
            // this edge is safe — it genuinely connects two
            // previously-separate parts of the graph.
            if (ds.findUParent(u) != ds.findUParent(v)) {
                mstWeight += wt;
                ds.unionBySize(u, v);
            }
            // else: u and v already connected — taking this
            // edge would only create a cycle. Discard, do nothing.
        }

        return mstWeight;
    }

    // ─────────────────────────────────────────
    // Driver — test with our example graph
    // (converted to 0-indexed: nodes 1-6 → 0-5)
    // ─────────────────────────────────────────
    public static void main(String[] args) {
        int V = 6;
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) adj.add(new ArrayList<>());

        int[][] edgeInput = {
            {0, 1, 2},   // 1-2, wt 2
            {1, 5, 7},   // 2-6, wt 7
            {5, 2, 8},   // 6-3, wt 8
            {2, 3, 5},   // 3-4, wt 5
            {3, 4, 9},   // 4-5, wt 9
            {4, 0, 4},   // 5-1, wt 4
            {0, 3, 1},   // 1-4, wt 1
            {1, 3, 3},   // 2-4, wt 3
            {1, 2, 3}    // 2-3, wt 3
        };

        for (int[] e : edgeInput) {
            int u = e[0], v = e[1], wt = e[2];
            adj.get(u).add(new int[]{v, wt});
            adj.get(v).add(new int[]{u, wt});   // undirected → both directions
        }

        System.out.println("MST weight: " + spanningTree(V, adj));
        // Expected: 17
    }
}
```

---

### Confirming the Trace Matches

```
Sorted flat edge list (0-indexed, matches the manual trace):
(1, 0, 3)  ← 1-4, wt 1
(2, 0, 1)  ← 1-2, wt 2
(3, 1, 3)  ← 2-4, wt 3
(3, 1, 2)  ← 2-3, wt 3
(4, 4, 0)  ← 5-1, wt 4
(5, 2, 3)  ← 3-4, wt 5
(7, 1, 5)  ← 2-6, wt 7
(8, 5, 2)  ← 6-3, wt 8
(9, 3, 4)  ← 4-5, wt 9

Processing:
(1,0,3): findUParent(0)=0, findUParent(3)=3 → DIFF → take, sum=1, union(0,3)
(2,0,1): findUParent(0)=0, findUParent(1)=1 → DIFF → take, sum=3, union(0,1)
(3,1,3): findUParent(1)=0, findUParent(3)=0 → SAME → discard
(3,1,2): findUParent(1)=0, findUParent(2)=2 → DIFF → take, sum=6, union(1,2)
(4,4,0): findUParent(4)=4, findUParent(0)=0 → DIFF → take, sum=10, union(4,0)
(5,2,3): findUParent(2)=0, findUParent(3)=0 → SAME → discard
(7,1,5): findUParent(1)=0, findUParent(5)=5 → DIFF → take, sum=17, union(1,5)
(8,5,2): findUParent(5)=0, findUParent(2)=0 → SAME → discard
(9,3,4): findUParent(3)=0, findUParent(4)=0 → SAME → discard

Final: mstWeight = 17
```

Matches the manual trace exactly. ✓

---

## Complexity Analysis

### Time Complexity — O(E log E + E · 4α) ≈ O(E log E)

Let's derive this carefully, the way Striver does.

```
Flattening adjacency list into edge list  → O(V + E)
Sorting the edge list                     → O(E log E)
Walking through all E edges:
  each iteration does 2 findUParent calls + possibly 1 union
  → O(4α) per edge, effectively O(1)
  → total across all edges: O(E · 4α)

Overall: O(V + E) + O(E log E) + O(E · 4α)
       = O(E log E)     [the sort dominates — 4α is practically constant]
```

Since `E ≤ V²`, `log E ≤ 2 log V`, so this is sometimes written as `O(E log V)` — same asymptotic family as Prim's, though the constant-factor behavior differs since Kruskal's sort is a one-time global cost rather than repeated heap operations.

### Space Complexity — O(V + E)

```
edges list (flattened)     → O(E)
DisjointSet (parent[]+size[]) → O(V)
─────────────────────────────
Total: O(V + E)
```

---

## Prim's vs Kruskal's — The Complete Comparison

```
┌───────────────────────┬────────────────────────────┬───────────────────────────┐
│ Aspect                │ Prim's                     │ Kruskal's                 │
├───────────────────────┼────────────────────────────┼───────────────────────────┤
│ Core strategy         │ Grow ONE tree outward,     │ Sort ALL edges globally,  │
│                       │ frontier by frontier       │ greedily accept non-cycle │
│                       │                            │ edges                     │
│ Data structure needed │ Priority Queue + visited[] │ Sort + Disjoint Set       │
│ Natural input format  │ Adjacency list             │ Flat edge list (or        │
│                       │                            │ adjacency list flattened) │
│ Cycle detection       │ Implicit (visited[] check) │ Explicit (DSU query)      │
│ Best suited for       │ Dense graphs               │ Sparse graphs (fewer      │
│                       │                            │ edges to sort)            │
│ Time Complexity       │ O(E log E)                 │ O(E log E)                │
│ Space Complexity      │ O(V + E)                   │ O(V + E)                  │
└───────────────────────┴────────────────────────────┴───────────────────────────┘
```

Both arrive at the exact same MST weight for any given graph — they're two different, equally valid greedy strategies for the same underlying problem. Which one an interviewer expects often just comes down to what input format they hand you.

---

## Quick Revision Checklist

- [ ] Why is it completely fine that each undirected edge appears twice in the flattened edge list — what specifically causes the second occurrence to be discarded automatically?
- [ ] What is the exact condition that determines whether an edge is accepted or discarded in Kruskal's — phrase it in terms of `findUParent`?
- [ ] Structurally, what is the core difference in *how* Prim's and Kruskal's explore the graph — one grows outward, the other does what?
- [ ] Why does Kruskal's need Disjoint Set specifically, when Prim's got away with a simple `visited[]` array?
- [ ] Why does the algorithm never need to explicitly check "have I taken `n-1` edges yet, should I stop early?" — what happens naturally if you just let it run through the entire sorted list?
- [ ] In the time complexity, which term dominates — the sort, or the DSU operations — and why?

---

That completes **Pattern 11 (DSU)** and its immediate application in **Pattern 12 (MST) — Kruskal's Algorithm**, alongside Prim's from earlier. Whenever you're ready, let me know what's next — whether that's the DSU problem set (LC 323, LC 261, LC 684, etc.) or moving on to Pattern 13 (Bridges & Articulation Points).