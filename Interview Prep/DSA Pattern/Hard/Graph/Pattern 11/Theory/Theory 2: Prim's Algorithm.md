# Prim's Algorithm — Minimum Spanning Tree

---

## Quick Recap — What We're Building Toward

From the previous video: MST = the spanning tree (n nodes, n−1 edges, everyone connected) with the **smallest possible sum of edge weights**. Prim's Algorithm is the first of two classic algorithms that actually *compute* this — no brute-force enumeration of every possible spanning tree needed.

---

## The Example Graph

Paste into csacademy.com (undirected, weighted):

```
0 1 2
0 2 1
2 1 1
2 4 2
2 3 2
3 4 1
```

5 nodes (0 to 4), 6 edges. We'll build the MST starting from node **0**.

---

## Stage 1: Identification

**Step 1 — Which topic?**

Undirected weighted graph, connect-everything-cheaply framing → **Graph**.

**Step 2 — Which pattern?**

"Find the minimum total weight to connect all nodes" is the exact definition of MST — **Pattern 12 (Minimum Spanning Tree)**.

**Step 3 — Which key concept?**

**`Greedy Growth via Priority Queue — Prim's Algorithm`**

The core idea, stated plainly before any mechanics: *start from any single node, and repeatedly grab the cheapest edge that extends your current tree to a brand-new node.* Never take an edge that lands you back on a node you already have — that would create a cycle, and a tree can't have cycles.

---

## Stage 2: Intuition Building

### The Core Question

> *"I'm building a tree one node at a time. At each step, what is the single safest, cheapest move I can make?"*

Answer: **look at every edge going out from the tree you've built so far, and take the cheapest one that leads to a node you don't already have.** That's it. That's the entire algorithm. Repeat until every node is included.

This is a **greedy** strategy — at no point do you look ahead or reconsider a past decision. You trust that always taking the locally cheapest available option leads to the globally cheapest tree. (Striver doesn't prove this in the video — it's a known greedy-exchange argument — but the mechanical process is what matters for implementation.)

---

### What You Need Before Writing Any Code

Three pieces of state, and each one earns its place for a specific reason:

**1. A Min-Heap (Priority Queue)** — storing `(weight, node, parent)` triples.

Why min-heap? Because at every step you need "the cheapest available edge right now" — and that's exactly what a min-heap gives you for free, popped in sorted order, without you manually scanning for the minimum every time.

Why store `parent` alongside `weight` and `node`? Because if you ever need to report *which edges* make up the MST (not just the total weight), you need to remember which node you came from. If the problem only asks for the **sum**, you can drop `parent` entirely — Striver explicitly makes this distinction in the video.

**2. A `visited[]` array** — marks which nodes are already locked into the MST.

Why do you need this at all, if the heap already gives you the cheapest edge? Because the heap can contain **multiple entries pointing to the same node** — e.g., node 2 might be reachable both from node 0 (weight 1) and later discovered again from node 1 (weight 1) as a duplicate/worse path. Once a node is already part of the tree, any *later* entry for that same node in the heap is stale and must be ignored — that's exactly the stale-entry situation we saw in Dijkstra, and it's handled the same way here.

**3. A running `sum`** (and optionally an `mst` list of edges) — accumulates the answer as you go.

---

### The Initial Configuration

You can start from **any node** — it doesn't matter which one; the algorithm converges to the same MST weight regardless of starting point. Striver starts at node 0.

```
minHeap = [(weight=0, node=0, parent=-1)]
visited = [F, F, F, F, F]
sum = 0
mst = []
```

**Why `parent = -1` for the very first entry?** It's a signal, not a real edge. When you eventually pop this entry, `parent == -1` tells you *"this is the starting node — there is no actual edge here, don't add anything to the sum or the MST list."* Every other entry that gets pushed later will have a real parent.

---

### The Core Loop — What Happens at Each Pop

```
1. Pop (weight, node, parent) — the globally cheapest untaken edge right now

2. If node is already visited → do nothing, discard this entry, move to the next pop
   (this is a stale entry — the node is already in the tree via a cheaper or
   equal-cost edge found earlier)

3. Otherwise:
   → mark node as visited
   → if parent != -1: this edge is genuinely part of the MST
       → add weight to sum
       → (optionally) record the edge (parent, node) in the mst list

4. Now that node is locked in, look at ALL its neighbours:
   → for each neighbour that is NOT yet visited:
       push (edgeWeight, neighbour, node) into the min-heap
   → (no relaxation condition needed here, unlike Dijkstra — every edge
      from a newly-visited node is a fresh candidate, pushed unconditionally)
```

**A subtle but important point Striver makes explicitly**: you push a node into the heap *multiple times* if it's reachable from multiple already-visited nodes — you do **not** try to avoid duplicate pushes. The `visited[]` check at pop-time is what cleans this up cheaply. Trying to prevent duplicates at push-time would need extra bookkeeping (like Dijkstra's Set-based version) for zero real benefit here.

```
┌────────────────────────────────────────────────────────────────┐
│  PRIM'S CORE LOOP — one sentence each                          │
│                                                                │
│  Pop cheapest edge → if destination already in tree, skip it.  │
│  Otherwise, lock the destination in, add its weight to the     │
│  sum, and push every edge leading OUT of it for future rounds. │
│                                                                │
│  Unlike Dijkstra, there's no "relax if better" comparison —    │
│  every outgoing edge from a freshly visited node is pushed     │
│  unconditionally. The visited check at POP time is what        │
│  filters out everything that turns out to be unnecessary.      │
└────────────────────────────────────────────────────────────────┘
```

---

### Full Workflow Trace on Our Graph

Adjacency (from the edge list above):
```
0 → [(1,2), (2,1)]
1 → [(0,2), (2,1)]
2 → [(0,1), (1,1), (3,2), (4,2)]
3 → [(2,2), (4,1)]
4 → [(2,2), (3,1)]
```

```
INITIAL
────────────────────────────────────────────────
minHeap = [(0, 0, -1)]
visited = [F, F, F, F, F]
sum = 0

POP (0, node=0, parent=-1)
  parent == -1 → don't add to sum/MST, just mark visited
  visited[0] = T
  Push neighbours of 0: (2,1,0), (1,2,0)
────────────────────────────────────────────────
minHeap = [(1,2,0), (2,1,0)]

POP (1, node=2, parent=0)
  node 2 unvisited → LOCK IN
  sum += 1 → sum = 1;  MST edge: (0-2, wt 1)
  visited[2] = T
  Push neighbours of 2 (skip 0, already visited):
    (1,1,2), (2,3,2), (2,4,2)
────────────────────────────────────────────────
minHeap = [(1,1,2), (2,1,0), (2,3,2), (2,4,2)]

POP (1, node=1, parent=2)
  node 1 unvisited → LOCK IN
  sum += 1 → sum = 2;  MST edge: (2-1, wt 1)
  visited[1] = T
  Push neighbours of 1 (0 and 2 both already visited) → nothing new
────────────────────────────────────────────────
minHeap = [(2,1,0), (2,3,2), (2,4,2)]

POP (2, node=1, parent=0)
  node 1 ALREADY VISITED → STALE ENTRY, discard, do nothing
────────────────────────────────────────────────
minHeap = [(2,3,2), (2,4,2)]

POP (2, node=3, parent=2)
  node 3 unvisited → LOCK IN
  sum += 2 → sum = 4;  MST edge: (2-3, wt 2)
  visited[3] = T
  Push neighbours of 3 (skip 2, already visited):
    (1,4,3)
────────────────────────────────────────────────
minHeap = [(1,4,3), (2,4,2)]

POP (1, node=4, parent=3)
  node 4 unvisited → LOCK IN
  sum += 1 → sum = 5;  MST edge: (3-4, wt 1)
  visited[4] = T
  Push neighbours of 4 (2 and 3 both visited) → nothing new
────────────────────────────────────────────────
minHeap = [(2,4,2)]

POP (2, node=4, parent=2)
  node 4 ALREADY VISITED → STALE ENTRY, discard
────────────────────────────────────────────────
minHeap empty → DONE

FINAL: sum = 5
MST edges: (0-2, 1), (2-1, 1), (2-3, 2), (3-4, 1)
```

Verify: 5 nodes, 4 edges (`n-1` ✓), sum = `1+1+2+1 = 5`.

---

### The Greedy Intuition, Restated Plainly

```
┌─────────────────────────────────────────────────────────────────┐
│  Start anywhere — it becomes your "tree so far."                │
│                                                                 │
│  At every step: among ALL edges currently leading from your     │
│  tree to a node NOT yet in the tree, take the cheapest one.     │
│                                                                 │
│  Why does this never backfire? Because you never take an edge   │
│  to a node you already have — that edge would be wasted (create │
│  a cycle, contribute nothing to connectivity). You only ever    │
│  spend on edges that make genuine progress: one more new node   │
│  added to the tree, for the least possible price available      │
│  right now.                                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Stage 3: Coding

The video solves the GFG-style version of this problem, which only asks for the **sum of MST edge weights** — not the actual list of edges. Striver explicitly notes: if a problem asks for the edge list too, add the `parent`-tracking and edge-recording steps (shown in comments below).

### Java Implementation

```java
import java.util.*;

class Solution {

    // Pair: (weight, node) — parent is deliberately OMITTED here
    // because this version only needs the SUM, not the actual edges.
    // If your problem needs the MST's edge list, add a `parent` field
    // and record (parent, node) into a list whenever you lock a node in.
    static class Pair implements Comparable<Pair> {
        int weight;
        int node;

        Pair(int weight, int node) {
            this.weight = weight;
            this.node = node;
        }

        public int compareTo(Pair other) {
            return Integer.compare(this.weight, other.weight);
        }
    }

    static int spanningTree(int V, List<List<int[]>> adj) {

        // ─────────────────────────────────────────
        // STEP 1: Min-heap seeded with the starting node.
        // Weight 0 because there's no real edge to reach
        // your own starting point.
        // ─────────────────────────────────────────
        PriorityQueue<Pair> minHeap = new PriorityQueue<>();
        minHeap.add(new Pair(0, 0));  // start from node 0

        // ─────────────────────────────────────────
        // STEP 2: visited[] — tracks which nodes are
        // already locked into the MST
        // ─────────────────────────────────────────
        boolean[] visited = new boolean[V];

        int sum = 0;

        // ─────────────────────────────────────────
        // STEP 3: Main loop
        // ─────────────────────────────────────────
        while (!minHeap.isEmpty()) {
            Pair curr = minHeap.poll();
            int wt = curr.weight;
            int node = curr.node;

            // STALE ENTRY — this node is already part of
            // the MST via a cheaper (or equal) edge found
            // earlier. Skip entirely — do not touch sum,
            // do not explore its neighbours again.
            if (visited[node]) {
                continue;
            }

            // Lock this node into the MST
            visited[node] = true;
            sum += wt;
            // If you needed the edge list: this is where you'd
            // record (parent, node, wt) into an MST list — but
            // only when parent != -1 (i.e., not the starting node).

            // Push every edge from this freshly-locked node.
            // No relaxation check here — every neighbour not
            // yet visited is a fresh, valid candidate.
            for (int[] adjacent : adj.get(node)) {
                int adjNode = adjacent[0];
                int adjWt = adjacent[1];

                if (!visited[adjNode]) {
                    minHeap.add(new Pair(adjWt, adjNode));
                }
            }
        }

        return sum;
    }

    // ─────────────────────────────────────────
    // Driver — test with the example graph
    // ─────────────────────────────────────────
    public static void main(String[] args) {
        int V = 5;
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < V; i++) adj.add(new ArrayList<>());

        int[][] edges = {
            {0, 1, 2},
            {0, 2, 1},
            {2, 1, 1},
            {2, 4, 2},
            {2, 3, 2},
            {3, 4, 1}
        };

        for (int[] e : edges) {
            int u = e[0], v = e[1], wt = e[2];
            adj.get(u).add(new int[]{v, wt});
            adj.get(v).add(new int[]{u, wt});   // undirected → both directions
        }

        System.out.println("MST weight: " + spanningTree(V, adj));
        // Expected output: 5
    }
}
```

### Extended Version — If You Need the Actual MST Edges

```java
// Only the differences from above are shown.

static class Pair implements Comparable<Pair> {
    int weight;
    int node;
    int parent;

    Pair(int weight, int node, int parent) {
        this.weight = weight;
        this.node = node;
        this.parent = parent;
    }

    public int compareTo(Pair other) {
        return Integer.compare(this.weight, other.weight);
    }
}

// ... inside the main loop, after visited[node] = true; sum += wt;

if (curr.parent != -1) {
    mstEdges.add(new int[]{curr.parent, node, wt});
    // This is the actual edge that got added to the MST
}

// Initial seed becomes: minHeap.add(new Pair(0, 0, -1));
// Every subsequent push becomes:
//   minHeap.add(new Pair(adjWt, adjNode, node));
//                                          ↑ current node becomes the parent
```

---

## Complexity Analysis

### Time Complexity — O(E log E)

Let's derive this the way Striver does, rather than just stating it.

The `while` loop processes entries out of the priority queue. In the worst case (e.g., a dense graph, or the "star + chain" adversarial case Striver draws — one node connected to everything), **every edge in the graph can end up pushed into the heap at some point**. So the heap can grow to size `O(E)`.

```
Outer while loop → runs up to O(E) times (bounded by total pushes)
Each pop           → O(log(heap size)) = O(log E)

Inner for loop (across ALL iterations combined, not per-iteration)
  → every edge is examined from exactly one direction when its
    source node is popped → total O(E) edge examinations
  → each successful push        → O(log E)

Total: O(E log E) [popping]  +  O(E log E) [pushing]  =  O(E log E)
```

Since `E ≤ V²` in a simple graph, `log E ≤ log(V²) = 2 log V`, so this is sometimes written as `O(E log V)` — same as Dijkstra, and for the same underlying reason (heap operations dominated by edge count).

### Space Complexity — O(V + E)

```
visited[] array   → O(V)
Priority Queue     → O(E) worst case (every edge can be pushed once)
Adjacency list      → O(V + E)
─────────────────────────────
Total: O(V + E)
```

---

## Quick Revision Checklist

- [ ] Why does the algorithm push `(weight, node, parent)` but only *use* `parent` if the problem asks for the actual MST edges, not just the sum?
- [ ] Why is `parent = -1` used for the starting node, and what does that signal during processing?
- [ ] Why do we check `visited[node]` **after popping**, rather than trying to avoid pushing duplicates in the first place?
- [ ] What is the key structural difference between Prim's relaxation step and Dijkstra's relaxation step — why is there no "is this better?" comparison here?
- [ ] Why can the priority queue size grow up to `O(E)` in the worst case — what graph shape causes this?
- [ ] Does the starting node matter for the final MST weight? Why or why not?

---

Next up per Striver's sequence: **Disjoint Set (Union-Find)** — the prerequisite for Kruskal's Algorithm. Send the transcript whenever you're ready.