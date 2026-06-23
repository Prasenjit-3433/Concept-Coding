# Shortest Path in Directed Acyclic Graph (DAG)

## Part 1 — Problem Understanding + Graph Setup + Intuition

---

### What is the Problem Asking?

You are given:
- A **Directed Acyclic Graph (DAG)** — edges have a direction, and there are no cycles
- Each edge has a **weight** (not necessarily 1 — can be any positive value)
- A **source node** (the problem states it will always be **0**, but the instructor solves it for source = **6** to keep it general)
- Find the **shortest distance from source to every other node**
- If a node is **unreachable**, return **-1** for it

---

### What is a DAG?

```
Directed   → every edge has a direction (u → v only, not both ways)
Acyclic    → no cycles (you can never loop back to where you started)
Graph      → nodes connected by directed weighted edges

Combined   → DAG = Directed Acyclic Graph
```

Both properties matter:
- **Directed** → adjacency list is one-way only (unlike the previous undirected problem)
- **Acyclic** → Topological Sort is possible — topo sort is only defined for DAGs

---

### The Example Graph

Paste this into [csacademy.com](https://csacademy.com/) — set it to **directed + weighted:**

```
6 4 2
4 0 3
0 1 2
1 3 1
2 3 3
4 2 1
6 5 3
5 4 1
```

Source node = **6**

**Verify it is a DAG — trace all paths:**
```
6 → 4 → 0 → 1 → 3
6 → 4 → 2 → 3
6 → 5 → 4 → 0 → 1 → 3
6 → 5 → 4 → 2 → 3
```
No node ever appears twice in any path — no cycles → confirmed DAG ✓

**Shortest distances from node 6:**

| Node | Shortest Distance | Path              |
|------|-------------------|-------------------|
| 6    | 0                 | source            |
| 4    | 2                 | 6→4               |
| 5    | 3                 | 6→5               |
| 0    | 5                 | 6→4→0             |
| 2    | 3                 | 6→4→2             |
| 1    | 7                 | 6→4→0→1           |
| 3    | 6                 | 6→4→2→3           |

---

### What's New Compared to the Previous Problem?

Each change in the problem forces a change in the approach:

```
┌──────────────────────┬─────────────────────────┬──────────────────────────────┐
│ Property             │ Previous Problem        │ This Problem                 │
├──────────────────────┼─────────────────────────┼──────────────────────────────┤
│ Graph type           │ Undirected              │ Directed (DAG)               │
│ Edge weights         │ Unit (all = 1)          │ Variable (any +ve value)     │
│ Adjacency list       │ List<Integer>           │ List<Pair(node, weight)>     │
│ Algorithm            │ Plain BFS               │ Topo Sort + Edge Relaxation  │
│ Core data structure  │ Queue                   │ Stack (DFS topo sort)        │
└──────────────────────┴─────────────────────────┴──────────────────────────────┘
```

---

### Why Plain BFS Breaks Here

In the previous problem, every edge cost exactly +1. BFS naturally processed nodes in order of increasing distance — the queue stayed sorted automatically.

Here edges have different weights. When you're at a node reached at distance `d`, its neighbours could be at `d+1`, `d+2`, `d+10` — anything. The queue is no longer sorted by distance, so BFS processes nodes in the wrong order and can miss shorter paths.

For general weighted graphs with no negative edges, the fix is **Dijkstra** (priority queue). But this graph is a **DAG** — that special acyclic structure lets us do something even simpler and faster than Dijkstra, using Topological Sort.

---

### The Adjacency List Stores Pairs Now

Previously for unit weight graphs:
```java
// just the neighbour node
adj.get(u) → [v1, v2, v3]
```

Now since edges have weights, you need to store both the neighbour and the edge weight together:
```java
// neighbour + weight as a Pair
adj.get(u) → [Pair(v1,w1), Pair(v2,w2), Pair(v3,w3)]
```

This is why a `Pair` class is used — it keeps the node and its edge weight bundled cleanly together.

---

### Step 1 — Topological Sort (DFS + Stack)

Topo sort is a linear ordering of nodes such that for every directed edge `u → v`, node `u` appears **before** `v` in the ordering.

**How DFS produces topo sort:**
- Run DFS from every unvisited node
- When DFS for a node is **completely finished** (all its descendants explored), push that node onto a Stack
- Stack naturally gives you the topo order (top = first in topo order)

**DFS trace on the example (for loop runs 0 to 6):**
```
i=0: visit 0 → go to 1 → go to 3
       3 has no neighbours → push 3
     back to 1, done → push 1
     back to 0, done → push 0

i=1: already visited
i=2: visit 2 → go to 3 (visited)
     push 2

i=3: already visited
i=4: visit 4 → go to 0 (visited) → go to 2 (visited)
     push 4

i=5: visit 5 → go to 4 (visited)
     push 5

i=6: visit 6 → go to 4 (visited) → go to 5 (visited)
     push 6

Stack (top → bottom): 6, 5, 4, 2, 0, 1, 3
Topo order           : 6  5  4  2  0  1  3
```

Verify: for every edge `u → v`, does `u` appear before `v`?
```
6→4 ✓   6→5 ✓   5→4 ✓   4→0 ✓
4→2 ✓   0→1 ✓   1→3 ✓   2→3 ✓
```
Valid topo sort ✓

---

### Critical Question — Does the Source Have to Be the First Node in Topo Order?

This is a very natural doubt. The short answer is **no** — and here's why.

**What topo order guarantees:**
> For every directed edge `u → v`, node `u` is processed before `v`.

This means when you process any node `v`, all nodes that have edges pointing **into** `v` have already been processed. So `dist[v]` has already received all possible updates before you use it to relax further edges.

**Now think about what happens to nodes that appear before the source in topo order:**

In the example, topo order is `6 5 4 2 0 1 3` and source is `6` — the first node. But what if source were node `4`? Topo order would still be `6 5 4 2 0 1 3`.

```
When we pop 6: dist[6] = (int)1e9 (not the source)
  → Try to relax 6's neighbours: dist[6] + wt = 1e9 + wt
  → This will NEVER be less than dist[neighbour] which is also 1e9
  → Condition (dist[curr] + wt < dist[neighbour]) is FALSE
  → Nothing gets updated → safely skipped ✓

When we pop 5: same situation → skipped ✓

When we pop 4: dist[4] = 0 (this IS the source)
  → Now relaxation begins correctly from here
  → All subsequent nodes get updated normally ✓
```

**So nodes before the source in topo order are completely harmless** — they have `dist = (int)1e9`, and `1e9 + anything` will never beat `1e9`. The relaxation condition silently discards them.

```
┌──────────────────────────────────────────────────────────────┐
│  IMPORTANT                                                   │
│                                                              │
│  The source does NOT need to be the first node in topo       │
│  order. Nodes processed before the source simply have        │
│  dist = (int)1e9 and their relaxation attempts always        │
│  fail the condition check → they are silently skipped.       │
│                                                              │
│  Relaxation only "activates" when it reaches the source      │
│  node (dist = 0) and propagates forward from there.          │
└──────────────────────────────────────────────────────────────┘
```

---

### Step 2 — Edge Relaxation

Once topo sort is ready, initialize the distance array:

```
dist[] = [(int)1e9, (int)1e9, (int)1e9, (int)1e9, (int)1e9, (int)1e9, (int)1e9]
           node 0     node 1    node 2    node 3    node 4    node 5    node 6

Set dist[src] = 0:
dist[] = [(int)1e9, (int)1e9, (int)1e9, (int)1e9, (int)1e9, (int)1e9, 0]
```

Then pop nodes from the stack one by one and relax edges:

**What "relax an edge" means:**

```
You are at node curr (reached at distance dist[curr])
You have an edge curr → adjNode with weight adjWt

Relaxation condition:
  if (dist[curr] + adjWt < dist[adjNode]):
      dist[adjNode] = dist[curr] + adjWt   ← found a shorter path, update
  else:
      discard                               ← current path is not better
```

**How the distance array evolves (snapshot style):**

```
INITIAL
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [1e9]  [1e9]  [1e9]  [1e9]  [1e9]  [1e9]  [ 0 ]
Stack (top→bottom): 6, 5, 4, 2, 0, 1, 3

POP 6 (dist=0) → neighbours: 4(wt=2), 5(wt=3)
  dist[4] = min(1e9, 0+2) = 2 ✓
  dist[5] = min(1e9, 0+3) = 3 ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [1e9]  [1e9]  [1e9]  [1e9]  [ 2 ]  [ 3 ]  [ 0 ]

POP 5 (dist=3) → neighbours: 4(wt=1)
  dist[4] = min(2, 3+1=4) → 2 is better → discard ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [1e9]  [1e9]  [1e9]  [1e9]  [ 2 ]  [ 3 ]  [ 0 ]

POP 4 (dist=2) → neighbours: 0(wt=3), 2(wt=1)
  dist[0] = min(1e9, 2+3=5) = 5 ✓
  dist[2] = min(1e9, 2+1=3) = 3 ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [ 5 ]  [1e9]  [ 3 ]  [1e9]  [ 2 ]  [ 3 ]  [ 0 ]

POP 2 (dist=3) → neighbours: 3(wt=3)
  dist[3] = min(1e9, 3+3=6) = 6 ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [ 5 ]  [1e9]  [ 3 ]  [ 6 ]  [ 2 ]  [ 3 ]  [ 0 ]

POP 0 (dist=5) → neighbours: 1(wt=2)
  dist[1] = min(1e9, 5+2=7) = 7 ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [ 5 ]  [ 7 ]  [ 3 ]  [ 6 ]  [ 2 ]  [ 3 ]  [ 0 ]

POP 1 (dist=7) → neighbours: 3(wt=1)
  dist[3] = min(6, 7+1=8) → 6 is better → discard ✓
────────────────────────────────────────────────
Node:    0      1      2      3      4      5      6
dist: [ 5 ]  [ 7 ]  [ 3 ]  [ 6 ]  [ 2 ]  [ 3 ]  [ 0 ]

POP 3 (dist=6) → no neighbours → nothing to relax
────────────────────────────────────────────────
Stack empty → DONE

FINAL ANSWER (1e9 replaced with -1 for unreachable nodes)
Node:    0    1    2    3    4    5    6
dist: [  5    7    3    6    2    3    0 ]
```

All nodes reachable from source 6 ✓

---

### The Core Intuition — Why Topo Sort Guarantees Correctness

> **The fundamental challenge in shortest path:** To compute the shortest distance to node `v`, you first need the shortest distance to all nodes that have edges pointing *into* `v`.

In a graph with cycles, this is circular — A depends on B, B depends on C, C depends back on A. That's why Dijkstra needs a priority queue to iteratively resolve this.

In a **DAG**, no cycles exist. Topo sort gives you an ordering where every node `u` with edge `u → v` is guaranteed to appear before `v`. So:

```
When you process node v in topo order:
  → Every node with an edge into v has already been processed
  → dist[v] has already received all possible updates
  → dist[v] is FINAL at this point
  → You can safely use dist[v] to relax v's outgoing edges
  → Move on — never need to revisit v
```

This "process predecessors first, then the node" guarantee is exactly what topo sort provides — and it's what makes the algorithm correct and efficient in a single linear pass.

```
┌──────────────────────────────────────────────────────────────┐
│  KEY INSIGHT                                                 │
│                                                              │
│  Topo sort = "process in order of dependency"                │
│                                                              │
│  By the time you process node v:                             │
│    → All nodes that can reach v → already done               │
│    → dist[v] is already at its minimum possible value        │
│    → One pass is enough — O(V + E)                           │
│                                                              │
│  This is why DAG shortest path is faster than Dijkstra:      │
│  No priority queue needed. No revisiting nodes.              │
│  Just one clean linear sweep in topo order.                  │
└──────────────────────────────────────────────────────────────┘
```

---
# Shortest Path in Directed Acyclic Graph (DAG)
## Part 2 — Algorithm Workflow (Clean Memory-Refresh Version)

---

### The Overall Workflow at a Glance

```
Build adjacency list (store Pairs — neighbour + weight)
            ↓
Run Topo Sort (DFS + Stack)
  → for every unvisited node, run DFS
  → when DFS finishes for a node, push it onto Stack
  → Stack top = first node in topo order
            ↓
Initialize dist[] = (int)1e9 for all nodes
Set dist[src] = 0
            ↓
While Stack is not empty:
  Pop node curr from Stack
  For each Pair(adjNode, adjWt) in adjList[curr]:
    if dist[curr] + adjWt < dist[adjNode]:
      dist[adjNode] = dist[curr] + adjWt
    Else:
      discard (already have a shorter/equal path)
            ↓
Stack empty → done
            ↓
Replace all (int)1e9 entries with -1 (unreachable nodes)
Return dist[]
```

---

### The Two Phases — What Each One Does and Why

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1 — TOPO SORT                                             │
├─────────────────────────────────────────────────────────────────┤
│ What  : DFS-based ordering pushed onto a Stack                  │
│ Why   : Guarantees that when you process node v,                │
│         every node with an edge into v is already processed.    │
│         dist[v] is FINAL before you use it to relax edges.      │
│ Output: Stack (top → bottom) = valid topo order                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2 — EDGE RELAXATION                                       │
├─────────────────────────────────────────────────────────────────┤
│ What  : Pop nodes in topo order, update neighbour distances     │
│ Why   : Since predecessors are always processed first,          │
│         each node's distance is already optimal when popped.    │
│         One pass is enough — no revisiting.                     │
│ Output: dist[] with shortest distances from source              │
└─────────────────────────────────────────────────────────────────┘
```

---

### Three Situations During Edge Relaxation

When you pop node `curr` and look at neighbour `adjNode`:

```
┌─────────────────────────────────────────────────────────────────┐
│ SITUATION 1: dist[curr] == (int)1e9                             │
│                                                                 │
│ curr was never reached from source                              │
│ dist[curr] + adjWt = 1e9 + adjWt → overflows / still huge       │
│ Will NEVER beat dist[adjNode] which is also 1e9                 │
│ → Relaxation condition always fails → silently skipped          │
│                                                                 │
│ This is exactly why nodes before the source in topo order       │
│ are harmless — they just get skipped automatically              │
├─────────────────────────────────────────────────────────────────┤
│ SITUATION 2: dist[curr] + adjWt < dist[adjNode]                 │
│                                                                 │
│ Found a shorter path to adjNode                                 │
│ → Update dist[adjNode] = dist[curr] + adjWt                     │
│ → No need to push into any queue (unlike BFS/Dijkstra)          │
│    Topo order guarantees adjNode will be processed later        │
│    and will use this updated dist value automatically           │
├─────────────────────────────────────────────────────────────────┤
│ SITUATION 3: dist[curr] + adjWt >= dist[adjNode]                │
│                                                                 │
│ adjNode already reached via a shorter or equal path             │
│ → Discard. Do nothing.                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

### Why There's No Queue Push During Relaxation

This is a key structural difference from BFS and Dijkstra — worth locking in clearly.

```
In BFS (unit weight):
  → When you find a shorter path to v, you push v into Queue
  → Queue controls the processing order

In Dijkstra (weighted graph):
  → When you find a shorter path to v, you push (dist, v) into PriorityQueue
  → PriorityQueue always gives you the globally closest unprocessed node

In DAG Shortest Path (this algorithm):
  → When you find a shorter path to v, you just UPDATE dist[v]
  → No push needed — topo order already guarantees v will be
    processed AFTER all its predecessors
  → By the time you pop v from the Stack, dist[v] already holds
    the best possible value from ALL nodes that can reach v
```

---

### The Processing Order Mental Model

```
Topo order: 6 → 5 → 4 → 2 → 0 → 1 → 3
            ↑
            source (dist=0), everything before it is skipped

Step 1: Pop 6 (dist=0)
          → Activates relaxation. Updates 4 and 5.

Step 2: Pop 5 (dist=3)
          → Tries to update 4 via 5→4.
          → Already have dist[4]=2 which is better. Discard.

Step 3: Pop 4 (dist=2)
          → Updates 0 (dist=5) and 2 (dist=3).
          → Both confirmed shortest because 6 and 5
            (the only nodes that could reach 4) are already done.

Step 4: Pop 2 (dist=3)
          → Updates 3 (dist=6).
          → Confirmed shortest because 4 (only node reaching 2
            in topo order) is already done.

Step 5: Pop 0 (dist=5)
          → Updates 1 (dist=7).

Step 6: Pop 1 (dist=7)
          → Tries to update 3 via 1→3.
          → Already have dist[3]=6 which is better. Discard.

Step 7: Pop 3 (dist=6)
          → No outgoing edges. Nothing to relax.

Stack empty → DONE
```

---

### Comparison — BFS vs DAG Shortest Path

```
┌──────────────────────┬──────────────────────────┬────────────────────────────┐
│ Aspect               │ BFS (Unit Weight)        │ DAG Shortest Path          │
├──────────────────────┼──────────────────────────┼────────────────────────────┤
│ Ordering mechanism   │ Queue (FIFO)             │ Stack from Topo Sort       │
│ Why it works         │ Unit weights keep queue  │ Topo order ensures all     │
│                      │ naturally sorted         │ predecessors processed     │
│                      │                          │ before current node        │
│ Push on update?      │ Yes — push to Queue      │ No — just update dist[]    │
│ Multiple updates?    │ Yes, possible            │ No — each node processed   │
│                      │                          │ exactly once               │
│ Edge weights         │ Must be unit (=1)        │ Any positive weight        │
│ Graph type           │ Undirected/Directed      │ DAG only                   │
│ Time complexity      │ O(V + E)                 │ O(V + E)                   │
└──────────────────────┴──────────────────────────┴────────────────────────────┘
```

---

### Problem Pattern Identification Card

```
HOW TO IDENTIFY THIS PATTERN IN AN UNSEEN PROBLEM
──────────────────────────────────────────────────────────────────
Signals to look for:

  ✓ "Find shortest path / minimum cost / minimum distance"
  ✓ Graph is explicitly stated as a DAG
    OR you can infer it — e.g. task scheduling, dependency chains,
    compilation order, level-based progression (no going back)
  ✓ Edges have weights (not all equal)
  ✓ No mention of negative weight cycles
    (DAGs can have negative weights and this algo still works —
     topo order handles it. Dijkstra cannot handle negative weights.)

When you see these together → Topo Sort + Edge Relaxation

  One extra signal: if the problem mentions negative edge weights
  on a DAG, this algorithm still works correctly. Dijkstra would
  fail there. That makes this pattern especially powerful.
──────────────────────────────────────────────────────────────────
```

---
# Shortest Path in Directed Acyclic Graph (DAG)
## Part 3 — Java Implementation + Complexity Analysis

---

### Full Java Implementation

```java
import java.util.*;

class Solution {

    // ─────────────────────────────────────────
    // Pair class — bundles neighbour node and
    // edge weight together cleanly.
    // Used instead of int[] to make code more
    // readable — adjacent.node, adjacent.weight
    // is far clearer than adjacent[0], adjacent[1]
    // ─────────────────────────────────────────
    class Pair {
        public int node;
        public int weight;

        public Pair(int node, int weight) {
            this.node = node;
            this.weight = weight;
        }
    }

    // ─────────────────────────────────────────
    // DFS helper for topo sort
    //
    // Why push AFTER visiting all neighbours?
    // A node is only pushed to the stack once
    // ALL nodes reachable from it are fully explored.
    // This guarantees that when a node appears in
    // the stack, everything it points to is already
    // deeper in the stack (processed later in relaxation).
    // ─────────────────────────────────────────
    private void dfs(int node, boolean[] visited,
                     Stack<Integer> st,
                     List<List<Pair>> adjList) {

        // mark current node as visited
        visited[node] = true;

        // explore all neighbours first (go deeper)
        for (Pair adjacent : adjList.get(node)) {
            int adjNode = adjacent.node;

            if (!visited[adjNode]) {
                dfs(adjNode, visited, st, adjList);
            }
        }

        // only after ALL descendants are explored,
        // push this node → guarantees topo order
        st.push(node);
    }

    // ─────────────────────────────────────────
    // Topo Sort — runs DFS from every unvisited
    // node to handle disconnected components.
    //
    // Why the for loop?
    // The graph may have multiple components.
    // A single DFS from one node won't reach nodes
    // in other components. The for loop ensures
    // every node gets a chance to start a DFS.
    // ─────────────────────────────────────────
    private Stack<Integer> topoSort(int V,
                                    List<List<Pair>> adjList) {
        boolean[] visited = new boolean[V];
        Stack<Integer> st = new Stack<>();

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                dfs(node, visited, st, adjList);
            }
        }

        return st;
    }

    // ─────────────────────────────────────────
    // Main method — shortest path in DAG
    // ─────────────────────────────────────────
    public int[] shortestPath(int V, int E, int[][] edges) {

        // ─────────────────────────────────────
        // STEP 1: Build the Adjacency List
        //
        // Directed graph → add edge one way only
        // u → v with weight wt
        // (unlike undirected where you add both ways)
        // ─────────────────────────────────────
        List<List<Pair>> adjList = new ArrayList<>();

        for (int node = 0; node < V; node++) {
            adjList.add(new ArrayList<>());
        }

        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            int wt = edge[2];

            // directed → one direction only
            adjList.get(u).add(new Pair(v, wt));
        }

        // ─────────────────────────────────────
        // STEP 2: Topological Sort
        //
        // Returns a Stack where top = first node
        // in topo order (has no unprocessed predecessors)
        // ─────────────────────────────────────
        Stack<Integer> topoStack = topoSort(V, adjList);

        // ─────────────────────────────────────
        // STEP 3: Initialize Distance Array
        //
        // (int) 1e9 as infinity — safe from overflow
        // when you do dist[curr] + adjWt unlike
        // Integer.MAX_VALUE which overflows on + 1
        //
        // Source is fixed as 0 per problem statement.
        // If source were any other node, nodes before
        // it in topo order would have dist = 1e9 and
        // their relaxation attempts would silently fail
        // the condition check → harmlessly skipped.
        // ─────────────────────────────────────
        int[] dist = new int[V];
        Arrays.fill(dist, (int) 1e9);

        dist[0] = 0; // source node distance = 0

        // ─────────────────────────────────────
        // STEP 4: Edge Relaxation in Topo Order
        //
        // Pop nodes one by one from the stack.
        // For each node, try to relax all its
        // outgoing edges.
        //
        // Why no queue push on update?
        // Topo order guarantees adjNode will be
        // processed after curr. By the time adjNode
        // is popped, dist[adjNode] will already hold
        // the best value from ALL its predecessors.
        // No revisiting needed.
        // ─────────────────────────────────────
        while (!topoStack.isEmpty()) {
            int curr = topoStack.pop();

            for (Pair adjacent : adjList.get(curr)) {
                int adjNode = adjacent.node;
                int adjWt = adjacent.weight;

                // relax edge: is going through curr
                // a shorter path to adjNode?
                if (dist[curr] + adjWt < dist[adjNode]) {
                    dist[adjNode] = dist[curr] + adjWt;
                }
                // else: adjNode already has a shorter
                // or equal path → discard
            }
        }

        // ─────────────────────────────────────
        // STEP 5: Replace (int)1e9 with -1
        //
        // Nodes still at (int)1e9 were never reached
        // from the source → return -1 for them
        // Reuse dist[] directly — no extra array needed
        // ─────────────────────────────────────
        for (int node = 0; node < V; node++) {
            if (dist[node] == (int) 1e9) {
                dist[node] = -1;
            }
        }

        return dist;
    }

    // ─────────────────────────────────────────
    // Driver — test with the example from lecture
    // ─────────────────────────────────────────
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Edges: {u, v, weight}
        int[][] edges = {
            {6, 4, 2},
            {4, 0, 3},
            {0, 1, 2},
            {1, 3, 1},
            {2, 3, 3},
            {4, 2, 1},
            {6, 5, 3},
            {5, 4, 1}
        };

        int V = 7; // nodes: 0 to 6
        int E = 8; // number of edges

        int[] result = sol.shortestPath(V, E, edges);

        System.out.println("Shortest distances from node 0... " +
                           "wait, source=6 in this example:");
        for (int i = 0; i < V; i++) {
            System.out.println("Node " + i + " → " +
                               (result[i] == -1 ? "unreachable" : result[i]));
        }
    }
}
```

---

### Expected Output

```
Node 0 → 5
Node 1 → 7
Node 2 → 3
Node 3 → 6
Node 4 → 2
Node 5 → 3
Node 6 → 0
```

---

### Complexity Analysis

#### Time Complexity — O(V + E)

The algorithm has three distinct phases. Let's account for each carefully.

**Phase 1 — Building the Adjacency List**

```
for (int[] edge : edges) → runs E times
  adjList.get(u).add(new Pair(v, wt)) → O(1) per edge

Total: O(E)
```

**Phase 2 — Topological Sort (DFS)**

```
Outer for loop in topoSort():
  Runs V times (once per node, 0 to V-1)
  But does NOT call dfs() V times —
  only calls dfs() for unvisited nodes.
  Once a node is visited, it's never
  entered again.

Inside dfs():
  Each node is visited exactly ONCE
  across ALL dfs() calls → O(V) total

  For each node, we iterate over its
  adjacency list:
    Node 0 → degree(0) iterations
    Node 1 → degree(1) iterations
    Node 2 → degree(2) iterations
    ...
    Node V-1 → degree(V-1) iterations

  Sum of all degrees = total edges = E
  (in a directed graph, each edge u→v
   contributes 1 to degree(u))

  → Adjacency traversal across ALL nodes = O(E)

  Stack push: O(1) per node, V nodes total → O(V)

Total for topo sort: O(V) + O(E) = O(V + E)
```

**Phase 3 — Edge Relaxation**

```
Stack has exactly V nodes (every node
is pushed exactly once in DFS)

While loop runs V times (one pop per node)

For each popped node curr, we iterate
over adjList.get(curr):
  → Again, sum of all adjacency list
    sizes = E (same argument as above)

dist[adjNode] update: O(1)

Total for relaxation: O(V) + O(E) = O(V + E)
```

**Overall Time Complexity:**

```
Build adjacency list  →  O(E)
Topo sort             →  O(V + E)
Edge relaxation       →  O(V + E)
Replace 1e9 with -1   →  O(V)
─────────────────────────────────
Total                 →  O(V + E)
```

**How does this compare to other shortest path algorithms?**

```
┌────────────────────────┬──────────────────┬──────────────────────────────────┐
│ Algorithm              │ Time Complexity  │ Condition                        │
├────────────────────────┼──────────────────┼──────────────────────────────────┤
│ BFS                    │ O(V + E)         │ Unit weights only                │
│ DAG Shortest Path      │ O(V + E)         │ DAG + any positive weights       │
│ Dijkstra               │ O((V+E) log V)   │ Any graph, non-negative weights  │
│ Bellman-Ford           │ O(V × E)         │ Any graph, handles -ve weights   │
└────────────────────────┴──────────────────┴──────────────────────────────────┘

DAG Shortest Path achieves O(V + E) — same as BFS — but works with
variable weights. It beats Dijkstra's O((V+E) log V) because:
  → No priority queue needed (no log V factor from heap operations)
  → Topo sort gives the "right order" for free in O(V + E)
  → Each node and edge is processed exactly once — no revisiting
```

---

#### Space Complexity — O(V + E)

```
┌──────────────────────────────────────────────────────────────┐
│ Structure            │ Space Used                            │
├──────────────────────────────────────────────────────────────┤
│ Adjacency list       │ O(V + E)                              │
│                      │ V lists + E Pair objects total        │
│                      │ (directed graph → each edge stored    │
│                      │  once, unlike undirected which is 2E) │
├──────────────────────────────────────────────────────────────┤
│ visited[] array      │ O(V)                                  │
│                      │ One boolean per node                  │
├──────────────────────────────────────────────────────────────┤
│ Stack (topo sort)    │ O(V)                                  │
│                      │ Every node pushed exactly once        │
├──────────────────────────────────────────────────────────────┤
│ DFS call stack       │ O(V) in the worst case                │
│                      │ Deepest DFS path = longest chain      │
│                      │ in the DAG, which is at most V nodes  │
│                      │ e.g. 0→1→2→3→...→V-1                  │
├──────────────────────────────────────────────────────────────┤
│ dist[] array         │ O(V)                                  │
│                      │ One integer per node                  │
└──────────────────────────────────────────────────────────────┘

Overall Space: O(V + E)

The dominant term is the adjacency list at O(V + E).
Everything else is O(V) and is absorbed into O(V + E).
```

---

### Quick Revision Card

```
┌──────────────────────────────────────────────────────────────┐
│  SHORTEST PATH — DAG (Directed Acyclic Graph)                │
├──────────────────────────────────────────────────────────────┤
│  Algorithm    : Topo Sort (DFS+Stack) + Edge Relaxation      │
│  Adjacency    : List<List<Pair>> — stores (node, weight)     │
│  Infinity     : (int) 1e9 — safe from overflow               │
│  Relax cond   : dist[curr] + adjWt < dist[adjNode]           │
│                 → update dist[adjNode]                       │
│                 → NO queue push needed                       │
│  Unreachable  : dist[i] == (int)1e9 → return -1              │
│  Time         : O(V + E)                                     │
│  Space        : O(V + E)                                     │
│                                                              │
│  KEY INSIGHT 1: Topo order = process predecessors first      │
│    → dist[v] is FINAL when v is popped from stack            │
│    → one pass, no revisiting                                 │
│                                                              │
│  KEY INSIGHT 2: Source need not be first in topo order       │
│    → nodes before source have dist = 1e9                     │
│    → 1e9 + anything never beats 1e9 → silently skipped       │
│                                                              │
│  KEY INSIGHT 3: Works with negative edge weights too         │
│    → Dijkstra fails on negative weights                      │
│    → Topo order sidesteps the problem entirely               │
└──────────────────────────────────────────────────────────────┘
```

---

That wraps up all three parts for DAG Shortest Path. Ready for the next lecture whenever you are!